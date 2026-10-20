package app.andropaw.agent

import org.json.JSONArray
import org.json.JSONObject

/**
 * Termux-Ultra Agent 的对话模型（照搬其 AiTermuxModels 的结构），技能表换成
 * AndroPaw 自己的 guest 工具。UI/协议/对话逻辑与 Ki 对齐。
 */

/** 技能类型：就是本项目的全部 guest 工具 + 两类交互卡 */
enum class SkillType {
    GUEST_SHELL, TAKE_SCREENSHOT, TAP, DOUBLE_TAP, LONG_PRESS, SWIPE,
    INPUT_TEXT, KEY_ACTION, LAUNCH_APP, OPEN_DEEPLINK, WAIT_SECONDS,
    GET_SCREEN_STATE, VM_STATE, VM_POWER, VM_REBOOT_RECOVERY, VM_WIPE_DATA,
    VM_INSTALL_RECOVERY,
    /** 交互卡：危险操作二次确认 / 重复操作确认 */
    CONFIRM_DANGEROUS, CONFIRM_DUPLICATE;

    companion object {
        fun of(s: String?): SkillType? = s?.trim()?.uppercase()?.let { runCatching { valueOf(it) }.getOrNull() }
    }
}

enum class SkillStatus { RUNNING, COMPLETED, FAILED }

/** 技能卡片数据（Ki 的 SkillCardData 裁剪到本项目用到的字段） */
data class SkillCardData(
    val skillType: SkillType,
    val title: String,
    val description: String,
    val status: SkillStatus = SkillStatus.COMPLETED,
    val command: String? = null,
    val output: String? = null,
    /** 截图结果（base64，仅 take_screenshot） */
    val imageBase64: String? = null,
    val partialOutput: Boolean = false,
    // 危险操作确认
    val dangerousReason: String? = null,
    val dangerousAction: String? = null,
)

/** 聊天消息（Ki 的 ChatMessage，去掉本地模型准备中/原始响应等本项目用不到的字段） */
data class ChatMessage(
    val id: String = System.currentTimeMillis().toString() + "_" + java.lang.Long.toHexString((Math.random() * 1e9).toLong()),
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val skillCard: SkillCardData? = null,
    val errorMessage: String? = null,
    val isWarning: Boolean = false,
    val reasoningContent: String? = null,
    val reasoningDone: Boolean = false,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("role", role).put("content", content).put("timestamp", timestamp)
        .let { o -> skillCard?.let { o.put("skillCard", it.toJson()) } ?: o }
        .let { o -> errorMessage?.let { o.put("errorMessage", it) } ?: o }
        .put("isWarning", isWarning)
        .let { o -> reasoningContent?.let { o.put("reasoningContent", it) } ?: o }
        .put("reasoningDone", reasoningDone)

    companion object {
        fun from(o: JSONObject) = ChatMessage(
            id = o.optString("id"),
            role = o.optString("role"),
            content = o.optString("content"),
            timestamp = o.optLong("timestamp"),
            skillCard = o.optJSONObject("skillCard")?.let { skillCardFrom(it) },
            errorMessage = o.optString("errorMessage").ifEmpty { null },
            isWarning = o.optBoolean("isWarning"),
            reasoningContent = o.optString("reasoningContent").ifEmpty { null },
            reasoningDone = o.optBoolean("reasoningDone"),
        )
    }
}

/** 单条对话 */
data class AiConversation(
    val id: String,
    val title: String = "AgentPaw",
    val messages: List<ChatMessage> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

fun SkillCardData.toJson(): JSONObject = JSONObject()
    .put("skillType", skillType.name).put("title", title).put("description", description)
    .put("status", status.name)
    .let { o -> command?.let { o.put("command", it) } ?: o }
    .let { o -> output?.let { o.put("output", it) } ?: o }
    .let { o -> imageBase64?.let { o.put("imageBase64", it) } ?: o }
    .put("partialOutput", partialOutput)
    .let { o -> dangerousReason?.let { o.put("dangerousReason", it) } ?: o }
    .let { o -> dangerousAction?.let { o.put("dangerousAction", it) } ?: o }

fun skillCardFrom(o: JSONObject): SkillCardData = SkillCardData(
    skillType = SkillType.of(o.optString("skillType")) ?: SkillType.GUEST_SHELL,
    title = o.optString("title"),
    description = o.optString("description"),
    status = runCatching { SkillStatus.valueOf(o.optString("status")) }.getOrDefault(SkillStatus.COMPLETED),
    command = o.optString("command").ifEmpty { null },
    output = o.optString("output").ifEmpty { null },
    imageBase64 = o.optString("imageBase64").ifEmpty { null },
    partialOutput = o.optBoolean("partialOutput"),
    dangerousReason = o.optString("dangerousReason").ifEmpty { null },
    dangerousAction = o.optString("dangerousAction").ifEmpty { null },
)
