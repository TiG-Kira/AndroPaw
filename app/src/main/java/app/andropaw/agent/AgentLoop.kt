package app.andropaw.agent

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * AgentPaw for AndroPaw — ReAct 驱动循环。
 *
 * 移植自 AgentPaw 的 com.paw.agent.core.agent.Agent：一次对话轮次 = 问模型 → 执行它请求的
 * 工具 → 把结果喂回去 → 再问，直到模型不再请求工具。不依赖任何 Android 类型，
 * 因此可以在后台线程里跑，也可以单测。
 */

/** Agent 工作时发出的事件，UI 直接渲染。 */
sealed interface AgentEvent {
    /** 助手消息正在流式生长，[message] 每次 delta 都是最新全量快照 */
    data class AssistantDelta(val message: Message) : AgentEvent
    /** 助手一轮结束并挂着待执行的工具调用 */
    data class AssistantTurn(val message: Message, val round: Int = 1) : AgentEvent
    /** 模型要求的某个工具即将执行 */
    data class ToolStarted(val call: ToolCall, val round: Int = 1) : AgentEvent
    /** 工具执行完毕（成败都有） */
    data class ToolFinished(val result: ToolResult) : AgentEvent
    /** 一轮正常结束 */
    data class Completed(val message: Message) : AgentEvent
    /** 一轮失败；[message].error 带原因 */
    data class Failed(val message: Message) : AgentEvent
    /** 用户点了停止 */
    data class Cancelled(val message: Message) : AgentEvent
}

/** 一个智能体能调用的能力。实现必须能在后台调度器上调用，并尊重 [AgentContext.isCancelled]。 */
interface AgentTool {
    val definition: ToolDefinition

    /**
     * 执行工具。
     * @param arguments 模型给出的原始 JSON 字符串
     * @return 作为工具结果回给模型的文本
     * @throws Exception 失败时 agent 会转成错误结果，模型可自行恢复而非整轮 abort
     */
    suspend fun execute(arguments: String, context: AgentContext): String
}

/** 每次运行的现场状态。 */
class AgentContext(
    val conversationId: String,
    val depth: Int = 0,
    val grantedTokens: Set<String> = emptySet(),
    private val cancelledCheck: () -> Boolean = { false },
) {
    fun isCancelled(): Boolean = cancelledCheck()
    fun canNest(limit: Int): Boolean = depth < limit
    fun isGranted(token: String): Boolean = grantedTokens.contains(token) || grantedTokens.contains("*")
}

/** 可用工具的注册表，注册顺序即发给 provider 的顺序。 */
class ToolRegistry(tools: List<AgentTool> = emptyList()) {
    private val toolsMap = java.util.concurrent.ConcurrentHashMap<String, AgentTool>().apply {
        tools.forEach { put(it.definition.name, it) }
    }

    val definitions: List<ToolDefinition> get() = toolsMap.values.map { it.definition }
    val isEmpty: Boolean get() = toolsMap.isEmpty()
    fun find(name: String): AgentTool? = toolsMap[name]
    fun register(tool: AgentTool) { toolsMap[tool.definition.name] = tool }
    fun unregister(name: String) { toolsMap.remove(name) }
    fun with(tool: AgentTool): ToolRegistry = ToolRegistry(toolsMap.values.toList() + tool)
}

class Agent(
    private val llmClient: LlmClient,
    private val toolRegistry: ToolRegistry = ToolRegistry(),
    private val maxToolRounds: Int = DEFAULT_MAX_TOOL_ROUNDS,
) {
    fun run(
        config: LlmConfig,
        history: List<Message>,
        isCancelled: () -> Boolean = { false },
        depth: Int = 0,
        enablePacing: Boolean = true,
    ): Flow<AgentEvent> = flow {
        val context = AgentContext("conversation", depth) { isCancelled() }
        var workingHistory = history
        var assistantId: String? = null
        var buffer: StringBuilder

        val effectiveMaxRounds: Int? = if (this@Agent.maxToolRounds != DEFAULT_MAX_TOOL_ROUNDS) {
            if (this@Agent.maxToolRounds <= 0) null else this@Agent.maxToolRounds.coerceAtMost(100)
        } else {
            if (config.maxToolRounds <= 0) null else config.maxToolRounds.coerceAtMost(100)
        }
        var round = 0
        while (true) {
            currentCoroutineContext().ensureActive()
            if (isCancelled()) {
                emit(AgentEvent.Cancelled(currentAssistant(StringBuilder(), assistantId, MessageStatus.CANCELLED)))
                return@flow
            }

            buffer = StringBuilder()
            val reasoning = StringBuilder()
            val pendingToolCalls = mutableListOf<ToolCall>()
            var failure: Exception? = null

            try {
                llmClient.complete(config, buildRequest(config, workingHistory, toolRegistry))
                    .collect { chunk ->
                        when (chunk) {
                            is LlmChunk.Delta -> {
                                buffer.append(chunk.text)
                                emit(AgentEvent.AssistantDelta(currentAssistant(buffer, assistantId, MessageStatus.STREAMING, reasoning = reasoning.toString())))
                            }
                            is LlmChunk.Reasoning -> {
                                reasoning.append(chunk.text)
                                emit(AgentEvent.AssistantDelta(currentAssistant(buffer, assistantId, MessageStatus.STREAMING, reasoning = reasoning.toString())))
                            }
                            is LlmChunk.ToolCalls -> pendingToolCalls += chunk.calls
                            is LlmChunk.Done -> Unit
                        }
                    }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                failure = e
            }

            if (failure != null) {
                emit(AgentEvent.Failed(currentAssistant(buffer, assistantId, MessageStatus.FAILED, failure.message ?: failure::class.simpleName, reasoning = reasoning.toString())))
                return@flow
            }

            if (pendingToolCalls.isEmpty()) {
                emit(AgentEvent.Completed(currentAssistant(buffer, assistantId, MessageStatus.COMPLETE, reasoning = reasoning.toString())))
                return@flow
            }

            if (effectiveMaxRounds != null && round >= effectiveMaxRounds) {
                emit(AgentEvent.Failed(currentAssistant(buffer, assistantId, MessageStatus.FAILED,
                    "Stopped after $effectiveMaxRounds tool rounds")))
                return@flow
            }

            val assistantMessage = currentAssistant(buffer, assistantId, MessageStatus.COMPLETE, reasoning = reasoning.toString()).copy(toolCalls = pendingToolCalls)
            workingHistory = workingHistory + assistantMessage
            emit(AgentEvent.AssistantTurn(assistantMessage, round + 1))

            for (call in pendingToolCalls) {
                currentCoroutineContext().ensureActive()
                if (isCancelled()) {
                    emit(AgentEvent.Cancelled(assistantMessage))
                    return@flow
                }
                emit(AgentEvent.ToolStarted(call, round + 1))
                val result = executeTool(call, context)
                emit(AgentEvent.ToolFinished(result))
                workingHistory = workingHistory + result.toMessages()

                if (enablePacing && !isCancelled() && !call.name.lowercase().contains("wait")) {
                    // 每步之间给 guest 一点稳定时间（移植 AdaptivePacingEngine 的保守子集）
                    val d = pacingDelay(call)
                    if (d > 0) delay(d)
                }
            }
            round++
        }
    }

    private fun pacingDelay(call: ToolCall): Long = when (call.name) {
        "take_screenshot" -> 350
        "tap", "double_tap", "long_press", "key_action" -> 250
        "launch_app" -> 1200
        "input_text" -> 200
        else -> 150
    }

    private suspend fun executeTool(call: ToolCall, context: AgentContext): ToolResult {
        val tool = toolRegistry.find(call.name)
            ?: return ToolResult(call.id, call.name, "Error: no tool named '${call.name}' is registered.", true)
        return runCatching { tool.execute(call.arguments, context) }
            .fold(
                onSuccess = { ToolResult(call.id, call.name, it) },
                onFailure = { e ->
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    ToolResult(call.id, call.name, "Error: ${e.message ?: e::class.simpleName}", true)
                },
            )
    }

    private fun currentAssistant(buffer: StringBuilder, id: String?, status: MessageStatus, error: String? = null, reasoning: String = ""): Message =
        Message(
            id = id ?: "streaming-assistant",
            role = MessageRole.ASSISTANT,
            content = buffer.toString(),
            status = status,
            error = error,
            reasoning = reasoning,
        )

    /** 工具结果转成回填给模型的消息；截图结果额外生成一条带图的 user 观察消息。 */
    private fun ToolResult.toMessages(): List<Message> {
        val imageBase64 = runCatching {
            val o = org.json.JSONObject(content)
            if (o.has("image_base64")) o.getString("image_base64") else null
        }.getOrNull()
        if (imageBase64 != null) {
            return listOf(
                Message("${toolCallId}_result", MessageRole.TOOL, "Screenshot taken successfully.", MessageStatus.COMPLETE, toolCallId = toolCallId),
                Message("${toolCallId}_obs", MessageRole.USER, "[Current Screen Observation]", MessageStatus.COMPLETE, images = listOf(imageBase64)),
            )
        }
        return listOf(
            Message("${toolCallId}_result", MessageRole.TOOL, content, if (isError) MessageStatus.FAILED else MessageStatus.COMPLETE, toolCallId = toolCallId),
        )
    }

    companion object {
        const val DEFAULT_MAX_TOOL_ROUNDS = 12
    }
}
