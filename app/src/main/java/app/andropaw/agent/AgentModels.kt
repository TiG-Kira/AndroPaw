package app.andropaw.agent

import org.json.JSONArray
import org.json.JSONObject

/**
 * AgentPaw for AndroPaw — 数据模型。
 *
 * 移植自合作项目 AgentPaw（com.paw.agent.core.model），去掉 kotlinx.serialization，
 * 持久化用 org.json 手写：AndroPaw 是单模块应用，不为此引入序列化插件。
 * 词汇表对齐 OpenAI chat-completions，任何 OpenAI 兼容端点都能原样收发。
 */

enum class MessageRole(val wire: String) {
    SYSTEM("system"), USER("user"), ASSISTANT("assistant"), TOOL("tool");

    companion object { fun from(s: String?) = entries.firstOrNull { it.wire == s } ?: USER }
}

enum class MessageStatus { COMPLETE, STREAMING, FAILED, CANCELLED;

    companion object { fun from(s: String?) = entries.firstOrNull { it.name == s } ?: COMPLETE }
}

data class Message(
    val id: String,
    val role: MessageRole,
    val content: String,
    val status: MessageStatus = MessageStatus.COMPLETE,
    val error: String? = null,
    val toolCalls: List<ToolCall> = emptyList(),
    val toolCallId: String? = null,
    /** base64 data URLs or http(s) URLs — 只有 user 消息携带截图 */
    val images: List<String> = emptyList(),
    val createdAt: Long = 0L,
) {
    val isUser: Boolean get() = role == MessageRole.USER
    val isAssistant: Boolean get() = role == MessageRole.ASSISTANT

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("role", role.wire); put("content", content)
        put("status", status.name)
        error?.let { put("error", it) }
        if (toolCalls.isNotEmpty()) put("toolCalls", JSONArray().apply { toolCalls.forEach { c -> put(c.toJson()) } })
        toolCallId?.let { put("toolCallId", it) }
        if (images.isNotEmpty()) put("images", JSONArray().apply { images.forEach { i -> put(i) } })
        put("createdAt", createdAt)
    }

    companion object {
        fun from(o: JSONObject) = Message(
            id = o.getString("id"),
            role = MessageRole.from(o.optString("role")),
            content = o.optString("content"),
            status = MessageStatus.from(o.optString("status")),
            error = o.optString("error").ifEmpty { null },
            toolCalls = o.optJSONArray("toolCalls")?.let { a -> (0 until a.length()).mapNotNull { i -> a.optJSONObject(i)?.let { ToolCall.from(it) } } } ?: emptyList(),
            toolCallId = o.optString("toolCallId").ifEmpty { null },
            images = o.optJSONArray("images")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList(),
            createdAt = o.optLong("createdAt"),
        )
    }
}

data class ToolCall(val id: String, val name: String, val arguments: String) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("name", name); put("arguments", arguments)
    }

    companion object {
        fun from(o: JSONObject) = ToolCall(o.getString("id"), o.getString("name"), o.optString("arguments"))
    }
}

data class ToolDefinition(val name: String, val description: String, val parametersSchema: String)

data class ToolResult(val toolCallId: String, val name: String, val content: String, val isError: Boolean = false)

data class Conversation(
    val id: String,
    val title: String = "",
    val messages: List<Message> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    val isEmpty: Boolean get() = messages.isEmpty()
    val isStreaming: Boolean get() = messages.lastOrNull()?.status == MessageStatus.STREAMING

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("title", title)
        put("messages", JSONArray().apply { messages.forEach { m -> put(m.toJson()) } })
        put("createdAt", createdAt); put("updatedAt", updatedAt)
    }

    companion object {
        fun from(o: JSONObject) = Conversation(
            id = o.getString("id"),
            title = o.optString("title"),
            messages = o.optJSONArray("messages")?.let { a -> (0 until a.length()).mapNotNull { i -> a.optJSONObject(i)?.let { Message.from(it) } } } ?: emptyList(),
            createdAt = o.optLong("createdAt"),
            updatedAt = o.optLong("updatedAt"),
        )
    }
}
