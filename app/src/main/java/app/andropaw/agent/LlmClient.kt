package app.andropaw.agent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.job
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * AgentPaw for AndroPaw — OpenAI 兼容 LLM 客户端。
 *
 * 移植自 AgentPaw 的 OpenAiCompatibleClient，换成 HttpURLConnection + org.json：
 * 不引 okhttp / kotlinx-serialization。SSE 流式 + 流式 tool_calls 片段按 index 聚合。
 */

enum class LlmProvider(
    val displayName: String,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val requiresApiKey: Boolean,
) {
    OPENAI("OpenAI", "https://api.openai.com/v1", "gpt-4o-mini", true),
    QWEN("Qwen / DashScope", "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-vl-max", true),
    DEEPSEEK("DeepSeek", "https://api.deepseek.com/v1", "deepseek-chat", true),
    GEMINI("Google Gemini (OpenAI compat)", "https://generativelanguage.googleapis.com/v1beta/openai", "gemini-2.0-flash", true),
    MOONSHOT("Moonshot / Kimi", "https://api.moonshot.cn/v1", "moonshot-v1-8k", true),
    OLLAMA("Ollama (local)", "http://127.0.0.1:11434/v1", "qwen2.5:7b", false),
    CUSTOM("Custom (OpenAI-compatible)", "", "", false),
    ;

    companion object { fun fromName(v: String?) = entries.firstOrNull { it.name == v } ?: CUSTOM }
}

data class LlmConfig(
    val provider: LlmProvider = LlmProvider.CUSTOM,
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
    val temperature: Float = 0.7f,
    val topP: Float = 1.0f,
    val maxTokens: Int = 2048,
    val maxToolRounds: Int = 12,
    val stream: Boolean = true,
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
) {
    val isUsable: Boolean
        get() = baseUrl.isNotBlank() && model.isNotBlank() && (!provider.requiresApiKey || apiKey.isNotBlank())

    companion object {
        const val DEFAULT_SYSTEM_PROMPT =
            "You are AgentPaw for AndroPaw, an autonomous AI assistant operating an Android system that runs inside the AndroPaw emulator on the user's phone.\n" +
            "You act ONLY inside the emulated Android system, through the emulator's own shell and input channels. You cannot touch the host phone.\n\n" +
            "Core Guidelines:\n" +
            "1. Observe first: call `take_screenshot` (or read state with `guest_shell`, e.g. `dumpsys window`) before acting, and after each action that changes the screen.\n" +
            "2. Screen coordinates are normalized integers in [0, 1000] (0,0 top-left, 1000,1000 bottom-right) for `tap`, `double_tap`, `long_press` and `swipe`.\n" +
            "3. `guest_shell` runs any command inside the emulated Android (`am`, `pm`, `settings`, `input`, `cat`, `ls`, …). Prefer dedicated tools when they exist.\n" +
            "4. `launch_app` opens an app by package name. `key_action` sends BACK/HOME/RECENTS/ENTER/POWER.\n" +
            "5. Machine tools (`vm_power`, `vm_wipe_data`, `vm_reboot_recovery`, `vm_install_recovery`) manage the emulator itself. Use them only when the user explicitly asks for machine-level actions; wiping data is destructive — confirm the intent from the conversation first.\n" +
            "6. Safety red line: NEVER type payment passwords, PINs, or confirm payments. Pause and ask the user to complete sensitive credentials themselves.\n" +
            "7. Be concise: briefly say what you are doing on each step and confirm clearly when the task is done."
    }
}

sealed interface LlmChunk {
    data class Delta(val text: String) : LlmChunk
    /** reasoning_content（深度思考模型的思考流） */
    data class Reasoning(val text: String) : LlmChunk
    data class ToolCalls(val calls: List<ToolCall>) : LlmChunk
    data object Done : LlmChunk
}

interface LlmClient {
    fun complete(config: LlmConfig, request: JSONObject): Flow<LlmChunk>
}

/** org.json 的 optString 在值为 JSON null 时返回 "null" 字符串——思考模型的 delta 每条都带 "content": null */
private fun JSONObject.realText(key: String): String? =
    if (!has(key) || isNull(key)) null else getString(key).takeIf { it.isNotEmpty() }

class LlmException(message: String) : Exception(message)

/** 把会话历史 + 工具表组装成 chat-completions 请求体。 */
internal fun buildRequest(config: LlmConfig, history: List<Message>, registry: ToolRegistry): JSONObject {
    val messages = JSONArray()
    if (config.systemPrompt.isNotBlank()) {
        messages.put(JSONObject().put("role", "system").put("content", config.systemPrompt))
    }
    // 只保留最近 2 条带图消息，防止 token 膨胀
    val keepImage = history.mapIndexedNotNull { i, m -> if (m.images.isNotEmpty()) i else null }.takeLast(2).toSet()
    history.forEachIndexed { index, m ->
        when (m.role) {
            MessageRole.SYSTEM -> messages.put(JSONObject().put("role", "system").put("content", m.content))
            MessageRole.USER -> {
                val sendImages = m.images.isNotEmpty() && index in keepImage
                if (!sendImages) {
                    val text = if (m.images.isNotEmpty()) "${m.content} (Previous screenshot omitted to preserve context)" else m.content
                    messages.put(JSONObject().put("role", "user").put("content", text))
                } else {
                    val parts = JSONArray()
                    if (m.content.isNotBlank()) parts.put(JSONObject().put("type", "text").put("text", m.content))
                    m.images.forEach { img ->
                        val url = when {
                            img.startsWith("data:") || img.startsWith("http://") || img.startsWith("https://") -> img
                            else -> "data:image/jpeg;base64,$img"
                        }
                        parts.put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", url)))
                    }
                    messages.put(JSONObject().put("role", "user").put("content", parts))
                }
            }
            MessageRole.ASSISTANT -> {
                val o = JSONObject().put("role", "assistant")
                if (m.content.isNotBlank()) o.put("content", m.content) else o.put("content", JSONObject.NULL)
                if (m.toolCalls.isNotEmpty()) {
                    o.put("tool_calls", JSONArray().apply {
                        m.toolCalls.forEach { c ->
                            put(JSONObject().apply {
                                put("id", c.id); put("type", "function")
                                put("function", JSONObject().put("name", c.name).put("arguments", c.arguments.ifBlank { "{}" }))
                            })
                        }
                    })
                }
                messages.put(o)
            }
            MessageRole.TOOL -> messages.put(JSONObject().put("role", "tool").put("content", m.content).put("tool_call_id", m.toolCallId ?: ""))
        }
    }
    val body = JSONObject()
        .put("model", config.model.ifBlank { "gpt-4o-mini" })
        .put("messages", messages)
        .put("stream", config.stream)
        .put("temperature", config.temperature.toDouble())
        .put("top_p", config.topP.toDouble())
        .put("max_tokens", config.maxTokens)
    if (!registry.isEmpty) {
        body.put("tools", JSONArray().apply {
            registry.definitions.forEach { d ->
                put(JSONObject().apply {
                    put("type", "function")
                    put("function", JSONObject().apply {
                        put("name", d.name); put("description", d.description)
                        put("parameters", runCatching { JSONObject(d.parametersSchema) }.getOrElse { JSONObject().put("type", "object") })
                    })
                })
            }
        })
    }
    return body
}

class OpenAiCompatibleClient : LlmClient {

    override fun complete(config: LlmConfig, request: JSONObject): Flow<LlmChunk> = flow {
        if (!config.isUsable) throw LlmException("Base URL, model and (where required) an API key must be set")
        if (config.baseUrl.startsWith("http://", true) && !isLocalOrPrivate(config.baseUrl)) {
            throw LlmException("Public APIs must use HTTPS. Plain HTTP is allowed only for local/private models.")
        }
        val url = URL(completionsUrl(config.baseUrl))
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 180_000
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", if (config.stream) "text/event-stream" else "application/json")
            if (config.apiKey.isNotBlank()) setRequestProperty("Authorization", "Bearer ${config.apiKey}")
        }
        val payload = request.apply {
            put("model", config.model.ifBlank { request.optString("model") })
            put("stream", config.stream)
            put("temperature", config.temperature.toDouble())
            put("top_p", config.topP.toDouble())
            put("max_tokens", config.maxTokens)
        }
        conn.outputStream.use { it.write(payload.toString().toByteArray()); it.flush() }

        // 用户取消时断开连接
        currentCoroutineContext().job.invokeOnCompletion { cause ->
            if (cause is kotlinx.coroutines.CancellationException) runCatching { conn.disconnect() }
        }

        val code = conn.responseCode
        if (code !in 200..299) {
            val err = runCatching { conn.errorStream?.bufferedReader()?.readText().orEmpty() }.getOrElse { "" }
            conn.disconnect()
            throw LlmException("HTTP $code: ${err.take(400)}")
        }

        if (!config.stream) {
            val text = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            val root = runCatching { JSONObject(text) }.getOrElse { throw LlmException("Malformed response: ${text.take(200)}") }
            root.optJSONObject("error")?.let { throw LlmException(it.optString("message", "API error")) }
            val msg = root.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")
                ?: throw LlmException("Empty response")
            (msg.realText("reasoning_content") ?: msg.realText("reasoning"))?.let { emit(LlmChunk.Reasoning(it)) }
            msg.realText("content")?.let { emit(LlmChunk.Delta(it)) }
            msg.optJSONArray("tool_calls")?.let { calls ->
                val list = (0 until calls.length()).mapNotNull { i ->
                    calls.optJSONObject(i)?.let { c ->
                        ToolCall(
                            c.optString("id").ifEmpty { "call_$i" },
                            c.optJSONObject("function")?.optString("name").orEmpty(),
                            c.optJSONObject("function")?.optString("arguments").orEmpty(),
                        )
                    }
                }
                if (list.isNotEmpty()) emit(LlmChunk.ToolCalls(list))
            }
            emit(LlmChunk.Done)
            return@flow
        }

        // ---- SSE ----
        val args = LinkedHashMap<Int, StringBuilder>()
        val names = LinkedHashMap<Int, String>()
        val ids = LinkedHashMap<Int, String>()
        val reader = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8))
        try {

            while (true) {
                val line = reader.readLine() ?: break
                if (!line.startsWith("data:")) continue
                val data = line.substring(5).trim()
                if (data == "[DONE]") break
                if (data.isEmpty()) continue
                val chunk = runCatching { JSONObject(data) }.getOrNull() ?: continue
                chunk.optJSONObject("error")?.let { throw LlmException(it.optString("message", "stream error")) }
                val choice = chunk.optJSONArray("choices")?.optJSONObject(0) ?: continue
                val delta = choice.optJSONObject("delta") ?: continue
                (delta.realText("reasoning_content") ?: delta.realText("reasoning"))?.let { emit(LlmChunk.Reasoning(it)) }
                delta.realText("content")?.let { emit(LlmChunk.Delta(it)) }
                delta.optJSONArray("tool_calls")?.let { calls ->
                    for (i in 0 until calls.length()) {
                        val c = calls.optJSONObject(i) ?: continue
                        val idx = c.optInt("index", i)
                        c.optString("id").takeIf { it.isNotEmpty() }?.let { ids[idx] = it }
                        c.optJSONObject("function")?.let { f ->
                            f.optString("name").takeIf { it.isNotEmpty() }?.let { names[idx] = it }
                            f.optString("arguments").let { a -> args.getOrPut(idx) { StringBuilder() }.append(a) }
                        }
                    }
                }
            }
        } finally {
            runCatching { reader.close() }
            conn.disconnect()
        }
        if (names.isNotEmpty()) {
            emit(LlmChunk.ToolCalls(names.keys.sorted().map { idx ->
                ToolCall(ids[idx] ?: "call_$idx", names[idx].orEmpty(), args[idx]?.toString().orEmpty())
            }))
        }
        emit(LlmChunk.Done)
    }.flowOn(Dispatchers.IO)

    private fun completionsUrl(base: String): String {
        val b = base.trimEnd('/')
        return if (b.endsWith("/chat/completions")) b else "$b/chat/completions"
    }

    private fun isLocalOrPrivate(base: String): Boolean = runCatching {
        val host = URL(base).host
        host == "localhost" || host == "127.0.0.1" || host == "::1" ||
            host.startsWith("10.") || host.startsWith("192.168.") ||
            (host.startsWith("172.") && host.split('.')[1].toIntOrNull()?.let { it in 16..31 } == true) ||
            InetAddress.getByName(host).isSiteLocalAddress
    }.getOrDefault(false)
}
