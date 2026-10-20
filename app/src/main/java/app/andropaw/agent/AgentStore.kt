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



    // ------------------------------------------------------------------ LLM 设置

    fun config(ctx: Context): LlmConfig {
        val p = prefs(ctx)
        val provider = runCatching { LlmProvider.valueOf(p.getString("provider", "CUSTOM") ?: "CUSTOM") }.getOrDefault(LlmProvider.CUSTOM)
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
