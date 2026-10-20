package app.andropaw.agent

import android.content.Context
import java.io.File

/**
 * AgentPaw for AndroPaw 的本地存储：对话落盘（filesDir/agent/conversations）+ LLM 设置。
 */
object AgentStore {
    private fun dir(ctx: Context) = File(ctx.filesDir, "agent").apply { mkdirs() }
    private fun convDir(ctx: Context) = File(dir(ctx), "conversations").apply { mkdirs() }
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("agent", Context.MODE_PRIVATE)

    // ------------------------------------------------------------------ 对话

    fun list(ctx: Context): List<Conversation> =
        convDir(ctx).listFiles()?.mapNotNull { f ->
            runCatching { Conversation.from(org.json.JSONObject(f.readText())) }.getOrNull()
        }?.sortedByDescending { it.updatedAt } ?: emptyList()

    fun get(ctx: Context, id: String): Conversation? {
        val f = File(convDir(ctx), "$id.json")
        if (!f.isFile) return null
        return runCatching { Conversation.from(org.json.JSONObject(f.readText())) }.getOrNull()
    }

    fun save(ctx: Context, c: Conversation) {
        val tmp = File(convDir(ctx), "${c.id}.json.tmp")
        tmp.writeText(c.toJson().toString())
        tmp.renameTo(File(convDir(ctx), "${c.id}.json"))
    }

    fun delete(ctx: Context, id: String) {
        runCatching { File(convDir(ctx), "$id.json").delete() }
    }

    fun newConversation(ctx: Context): Conversation {
        val id = "c" + System.currentTimeMillis().toString(36) + (100..999).random().toString(36)
        val now = System.currentTimeMillis()
        return Conversation(id = id, createdAt = now, updatedAt = now)
    }

    /** 用首条用户消息起个标题 */
    fun titleFor(c: Conversation): String {
        if (c.title.isNotBlank()) return c.title
        val first = c.messages.firstOrNull { it.isUser }?.content?.trim().orEmpty()
        return if (first.isEmpty()) "New chat" else first.take(24).replace('\n', ' ')
    }

    // ------------------------------------------------------------------ LLM 设置

    fun config(ctx: Context): LlmConfig {
        val p = prefs(ctx)
        val provider = LlmProvider.fromName(p.getString("provider", "CUSTOM"))
        val base = p.getString("baseUrl", provider.defaultBaseUrl) ?: provider.defaultBaseUrl
        return LlmConfig(
            provider = provider,
            baseUrl = base,
            apiKey = p.getString("apiKey", "") ?: "",
            model = p.getString("model", provider.defaultModel) ?: provider.defaultModel,
            temperature = p.getFloat("temperature", 0.7f),
            topP = p.getFloat("topP", 1.0f),
            maxTokens = p.getInt("maxTokens", 2048),
            maxToolRounds = p.getInt("maxToolRounds", 12),
            systemPrompt = p.getString("systemPrompt", LlmConfig.DEFAULT_SYSTEM_PROMPT) ?: LlmConfig.DEFAULT_SYSTEM_PROMPT,
        )
    }

    fun saveConfig(ctx: Context, c: LlmConfig) {
        prefs(ctx).edit()
            .putString("provider", c.provider.name)
            .putString("baseUrl", c.baseUrl)
            .putString("apiKey", c.apiKey)
            .putString("model", c.model)
            .putFloat("temperature", c.temperature)
            .putFloat("topP", c.topP)
            .putInt("maxTokens", c.maxTokens)
            .putInt("maxToolRounds", c.maxToolRounds)
            .putString("systemPrompt", c.systemPrompt)
            .apply()
    }
}
