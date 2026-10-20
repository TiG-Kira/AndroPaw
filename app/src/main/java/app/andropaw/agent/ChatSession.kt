package app.andropaw.agent

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Termux-Ultra Agent 的会话宿主（对位其 AiTermuxViewModel 的对话子集）：
 * 多会话管理、流式收集、<tool_call> 卡片解析、幻觉/循环防护、技能执行与
 * [Skill Result] 回填。执行层全部走 [GuestSkills]（guest 工具）。
 */
object ChatSession {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var appCtx: Context
    private var bridge: AgentVmBridge? = null

    fun attach(ctx: Context, b: AgentVmBridge) {
        appCtx = ctx.applicationContext
        bridge = b
    }

    // ---- 状态 ----
    val conversations = mutableListOf<AiConversation>()
    val messages = mutableListOf<ChatMessage>()
    var activeId: String? = null
    var isLoading = false
    var isStreaming = false

    @Volatile private var cancelled = false
    private var generationJob: Job? = null

    private val listeners = mutableListOf<() -> Unit>()
    fun onChange(l: () -> Unit) { listeners.add(l) }
    private fun notifyChange() { listeners.forEach { runCatching { it() } } }

    // ---- 存储：filesDir/agent-chats/<id>.json（新目录，不与旧 conversations 混用） ----
    private fun dir(ctx: Context): File = File(ctx.filesDir, "agent-chats").apply { mkdirs() }

    private fun persist(ctx: Context) {
        val id = activeId ?: return
        runCatching {
            val conv = conversations.firstOrNull { it.id == id } ?: AiConversation(id = id)
            val updated = conv.copy(messages = messages.toList(), updatedAt = System.currentTimeMillis())
            File(dir(ctx), "$id.json").writeText(JSONObject().put("conversation", convToJson(updated)).toString())
            val i = conversations.indexOfFirst { it.id == id }
            if (i >= 0) conversations[i] = updated else conversations.add(updated)
        }
    }

    private fun convToJson(c: AiConversation): JSONObject = JSONObject()
        .put("id", c.id).put("title", c.title).put("createdAt", c.createdAt).put("updatedAt", c.updatedAt)
        .put("messages", JSONArray().apply { c.messages.forEach { put(it.toJson()) } })

    fun loadAll(ctx: Context) {
        conversations.clear()
        runCatching {
            dir(ctx).listFiles()?.filter { it.isFile && it.name.endsWith(".json") }?.forEach { f ->
                runCatching {
                    val c = JSONObject(f.readText()).optJSONObject("conversation") ?: return@runCatching
                    val msgs = c.optJSONArray("messages")?.let { a ->
                        (0 until a.length()).mapNotNull { i -> a.optJSONObject(i)?.let(ChatMessage::from) }
                    } ?: emptyList()
                    conversations.add(AiConversation(
                        id = c.optString("id"), title = c.optString("title", "AgentPaw"),
                        messages = msgs, createdAt = c.optLong("createdAt"), updatedAt = c.optLong("updatedAt"),
                    ))
                }
            }
        }
        conversations.sortByDescending { it.updatedAt }
        notifyChange()
    }

    // ---- 会话管理 ----
    fun newConversation(ctx: Context) {
        val c = AiConversation(id = "c" + System.currentTimeMillis())
        conversations.add(0, c)
        open(ctx, c.id)
        persist(ctx)
    }

    fun open(ctx: Context, id: String) {
        activeId = id
        // 旧文件可能存着重复 id：读入时统一补唯一，LazyColumn 的 key 见重复必炸
        val seen = HashSet<String>()
        messages.clear()
        messages.addAll((conversations.firstOrNull { it.id == id }?.messages ?: emptyList()).map { m ->
            if (seen.add(m.id)) m else m.copy(id = m.id + "#" + seen.size)
        })
        notifyChange()
    }

    fun renameConversation(ctx: Context, id: String, title: String) {
        val i = conversations.indexOfFirst { it.id == id }
        if (i >= 0) {
            conversations[i] = conversations[i].copy(title = title)
            persistTo(ctx, conversations[i])
        }
        notifyChange()
    }

    fun deleteConversation(ctx: Context, id: String) {
        conversations.removeAll { it.id == id }
        runCatching { File(dir(ctx), "$id.json").delete() }
        if (activeId == id) { activeId = null; messages.clear() }
        notifyChange()
    }

    fun deleteMessage(ctx: Context, messageId: String) {
        messages.removeAll { it.id == messageId }
        persist(ctx)
        notifyChange()
    }

    private fun persistTo(ctx: Context, c: AiConversation) {
        runCatching { File(dir(ctx), "${c.id}.json").writeText(JSONObject().put("conversation", convToJson(c)).toString()) }
    }

    // ---- 发送与循环 ----
    fun sendUserMessage(text: String) {
        val ctx = appCtx
        val b = bridge ?: return
        if (text.isBlank() || isLoading) return
        if (activeId == null) newConversation(ctx)

        cancelled = false
        isLoading = true
        synchronized(messages) { messages.add(ChatMessage(role = "user", content = text)) }
        persist(ctx)
        notifyChange()

        generationJob = scope.launch {
            try {
                processAiTurn(ctx, b, text)
            } finally {
                isLoading = false
                persist(ctx)
                notifyChange()
            }
        }
    }

    fun stopGeneration() {
        cancelled = true
        generationJob?.cancel()
    }

    /** 保留每条消息的会话历史条数（Ki 的 contextMessages） */
    private const val CONTEXT_MESSAGES = 12
    /** 每次发送的技能→结果→技能 最大轮数（Ki 的 maxRounds） */
    private const val MAX_ROUNDS = 10

    private suspend fun processAiTurn(ctx: Context, b: AgentVmBridge, userText: String) {
        var currentUserText = userText
        cancelled = false
        val config = AgentStore.config(ctx)
        if (!config.isUsable) {
            synchronized(messages) { messages.add(ChatMessage(role = "assistant", content = "⚠️ LLM is not configured yet — set it up in Settings → \"AgentPaw for AndroPaw\".", isWarning = true)) }
            notifyChange()
            return
        }
        // 技能执行历史：检测重复
        val executedHistory = mutableListOf<String>()
        val maxHistorySize = 10
        var lastSkillKey: String? = null
        var consecutiveSameSkill = 0
        val maxConsecutiveSameSkill = 3
        // 相似文本回复检测
        val recentAiReplies = mutableListOf<String>()
        var consecutiveSimilarReplies = 0
        val maxConsecutiveSimilarReplies = 3
        val client = OpenAiCompatibleClient()

        for (round in 1..MAX_ROUNDS) {
            if (cancelled) break

            val systemPrompt = config.systemPrompt + GuestSkills.protocolPrompt()
            val apiMsgs = JSONArray()
            apiMsgs.put(JSONObject().put("role", "system").put("content", systemPrompt))
            val historySnapshot = synchronized(messages) { messages.toList() }
            historySnapshot.dropLast(1).takeLast(CONTEXT_MESSAGES).forEach {
                if (it.role == "user" || it.role == "assistant") {
                    apiMsgs.put(JSONObject().put("role", it.role).put("content", it.content))
                }
            }
            apiMsgs.put(JSONObject().put("role", "user").put("content", currentUserText))

            // 空的 assistant 消息用于流式填充
            val streamMsgId = "stream_" + System.currentTimeMillis() + "_" + (Math.random() * 1e6).toInt()
            synchronized(messages) { messages.add(ChatMessage(id = streamMsgId, role = "assistant", content = "")) }
            notifyChange()

            isStreaming = true
            var replyText = ""
            var reasoningText = ""
            var wasCancelled = false
            try {
                client.complete(config, buildPlainRequest(config, apiMsgs)).collect { chunk ->
                    when (chunk) {
                        is LlmChunk.Reasoning -> {
                            reasoningText += chunk.text
                            updateStream(streamMsgId) { it.copy(reasoningContent = reasoningText, reasoningDone = false) }
                        }
                        is LlmChunk.Delta -> {
                            replyText += chunk.text
                            updateStream(streamMsgId) { it.copy(content = replyText) }
                        }
                        LlmChunk.Done -> Unit
                    }
                    notifyChange()
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                wasCancelled = true
            } catch (e: Exception) {
                synchronized(messages) {
                    val i = messages.indexOfFirst { it.id == streamMsgId }
                    if (i >= 0) messages[i] = messages[i].copy(
                        content = replyText.ifBlank { "⚠️ ${e.message}" },
                        errorMessage = if (replyText.isBlank()) e.message else null,
                        isWarning = replyText.isBlank(),
                    )
                }
                notifyChange()
                return
            } finally {
                isStreaming = false
            }
            if (wasCancelled || cancelled) {
                updateStream(streamMsgId) { it.copy(content = replyText + "\n\n（已停止）", reasoningDone = true) }
                notifyChange()
                return
            }

            // 只有思考没有正文
            if (replyText.isBlank() && reasoningText.isNotBlank()) {
                updateStream(streamMsgId) {
                    it.copy(content = "⚠️ AI 只进行了深度思考，未输出实际回复。", reasoningContent = reasoningText, reasoningDone = true, isWarning = true)
                }
                notifyChange()
                return
            }
            // 完全空输出
            if (replyText.isBlank()) {
                updateStream(streamMsgId) { it.copy(content = "⚠️ AI 未输出任何内容（检查模型/端点是否支持流式）", isWarning = true) }
                notifyChange()
                return
            }

            // 解析技能卡
            val cleanedReply = replyText.replace("[END_TURN]", "").trimEnd()
            val plainText = ChatParser.stripSkillBlocks(cleanedReply)
            val skills = ChatParser.parseSkillBlocks(cleanedReply)

            // 更新正文（剥掉卡片块）
            synchronized(messages) {
                val i = messages.indexOfFirst { it.id == streamMsgId }
                if (i >= 0) messages[i] = messages[i].copy(
                    content = plainText.ifBlank { cleanedReply },
                    reasoningContent = reasoningText.ifBlank { null },
                    reasoningDone = reasoningText.isNotBlank(),
                )
            }
            persist(ctx)
            notifyChange()

            // 重复执行历史记录键
            for (s in skills) executedHistory.add("${s.first}:${ChatParser.canonicalSkillKey(s.first, s.second)}")

            // 连续相同技能检测
            val firstKey = skills.firstOrNull()?.let { it.first + ":" + ChatParser.canonicalSkillKey(it.first, it.second) }
            if (skills.size == 1 && firstKey == lastSkillKey) consecutiveSameSkill++ else if (skills.isNotEmpty()) consecutiveSameSkill = 1
            if (consecutiveSameSkill >= maxConsecutiveSameSkill) {
                synchronized(messages) {
                    val i = messages.indexOfFirst { it.id == streamMsgId }
                    if (i >= 0) messages[i] = messages[i].copy(content = "⚠️ 检测到 AI 陷入循环（连续 $consecutiveSameSkill 次执行相同操作），已自动停止。", isWarning = true)
                }
                notifyChange()
                return
            }
            lastSkillKey = firstKey

            // 无技能 → 回复完成
            if (skills.isEmpty()) {
                // 相似文本回复检测
                val normalized = plainText.trim().lowercase().replace(Regex("\\s+"), " ")
                if (normalized.length > 20) {
                    val similar = recentAiReplies.any { prev -> similarity(normalized, prev) > 0.85 }
                    if (similar) consecutiveSimilarReplies++ else consecutiveSimilarReplies = 1
                    recentAiReplies.add(normalized)
                    if (recentAiReplies.size > 5) recentAiReplies.removeAt(0)
                    if (consecutiveSimilarReplies >= maxConsecutiveSimilarReplies) {
                        synchronized(messages) {
                            val i = messages.indexOfFirst { it.id == streamMsgId }
                            if (i >= 0) messages[i] = messages[i].copy(
                                content = plainText + "\n\n⚠️ 检测到 AI 陷入重复回答循环，已自动停止。", isWarning = true)
                        }
                        notifyChange()
                        return
                    }
                }
                return
            }

            // === 执行技能（顺序，危险卡中断等待用户） ===
            var needsUserInput = false
            val allResultTexts = mutableListOf<String>()

            for ((skillTypeStr, params) in skills) {
                if (cancelled) break
                val st = SkillType.of(skillTypeStr) ?: continue
                val dangerReason = GuestSkills.checkDangerous(st, params)
                if (dangerReason != null) {
                    synchronized(messages) {
                        messages.add(ChatMessage(
                            role = "assistant", content = "",
                            skillCard = SkillCardData(
                                skillType = SkillType.CONFIRM_DANGEROUS,
                                title = "危险操作，需要确认",
                                description = dangerReason,
                                status = SkillStatus.RUNNING,
                                dangerousReason = dangerReason,
                                dangerousAction = GuestSkills.dangerousActionDesc(st, params),
                            ),
                        ))
                    }
                    persist(ctx); notifyChange()
                    needsUserInput = true
                    break
                }

                val tempId = "skill_" + System.currentTimeMillis() + "_" + (Math.random() * 1e6).toInt()
                synchronized(messages) {
                    messages.add(ChatMessage(
                        id = tempId, role = "assistant", content = "",
                        skillCard = SkillCardData(st, "执行技能中…", skillTypeStr, SkillStatus.RUNNING),
                    ))
                }
                notifyChange()

                val result = GuestSkills.execute(b, st, params)
                synchronized(messages) {
                    val i = messages.indexOfLast { it.id == tempId }
                    if (i >= 0) {
                        val card = result.card ?: SkillCardData(
                            skillType = st,
                            title = if (result.success) "执行成功" else "执行失败",
                            description = result.message,
                            status = if (result.success) SkillStatus.COMPLETED else SkillStatus.FAILED,
                        )
                        messages[i] = messages[i].copy(content = if (result.success) "" else "⚠️ 执行出错", skillCard = card)
                        allResultTexts.add(buildSkillResultText(card, result.message))
                    }
                }
                persist(ctx)
                notifyChange()
                kotlinx.coroutines.delay(150)
            }

            if (needsUserInput || cancelled) return

            val allResults = allResultTexts.joinToString("\n")
            currentUserText = if (allResults.isNotBlank()) {
                "[技能执行完成，请根据以下执行结果继续回复用户：]\n$allResults"
            } else ""
        }
    }

    private fun buildSkillResultText(card: SkillCardData, defaultMsg: String): String {
        val status = if (card.status == SkillStatus.COMPLETED) "成功" else "失败"
        val output = if (!card.output.isNullOrBlank()) "\n${card.output}" else ""
        val desc = card.description.ifBlank { defaultMsg }
        return "[Skill Result] ${card.skillType.name} $status：$desc$output"
    }

    private fun updateStream(streamMsgId: String, edit: (ChatMessage) -> ChatMessage) {
        synchronized(messages) {
            val i = messages.indexOfFirst { it.id == streamMsgId }
            if (i >= 0) messages[i] = edit(messages[i])
        }
    }

    private fun similarity(a: String, b: String): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        if (a == b) return 1.0
        val wa = a.split(Regex("\\s+")).toSet()
        val wb = b.split(Regex("\\s+")).toSet()
        if (wa.isEmpty() || wb.isEmpty()) return 0.0
        val jaccard = (wa intersect wb).size.toDouble() / (wa union wb).size.toDouble()
        val shorter = if (a.length < b.length) a else b
        val longer = if (a.length < b.length) b else a
        return (jaccard + if (longer.contains(shorter)) 0.3 else 0.0).coerceAtMost(1.0)
    }
}

/** 不带 tools 的纯文本请求体（Ki 协议下技能卡走正文 XML，不需要 function calling） */
private fun buildPlainRequest(config: LlmConfig, apiMsgs: JSONArray): JSONObject = JSONObject()
    .put("model", config.model)
    .put("messages", apiMsgs)
    .put("temperature", config.temperature.toDouble())
    .put("stream", true)
    .put("max_tokens", config.maxTokens)
