package app.andropaw.agent

import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/** LLM 配置（provider 预设 + 自定义端点） */
data class LlmConfig(
    val provider: LlmProvider = LlmProvider.OPENAI,
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
    val temperature: Float = 0.7f,
    val topP: Float = 1.0f,
    val maxTokens: Int = 2048,
    val maxToolRounds: Int = 10,
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
) {
    val isUsable: Boolean
        get() = baseUrl.isNotBlank() && model.isNotBlank() && (!provider.requiresApiKey || apiKey.isNotBlank())

    companion object {
        const val DEFAULT_SYSTEM_PROMPT =
            "You are AgentPaw for AndroPaw, an autonomous AI assistant operating an Android system that runs inside the AndroPaw emulator on the user's phone.\n" +
            "You act ONLY inside the emulated Android system, through the emulator's own shell and input channels. You cannot touch the host phone.\n\n" +
            "Core Guidelines:\n" +
            "1. Observe first: emit a `TAKE_SCREENSHOT` skill card (or read state with `GUEST_SHELL`, e.g. `dumpsys window`) before acting, and after each action that changes the screen.\n" +
            "2. All screen coordinates are normalized integers in [0, 1000] — (0,0) is top-left, (1000,1000) is bottom-right.\n" +
            "3. `GUEST_SHELL` runs any command inside the emulated Android (`am`, `pm`, `settings`, `input`, `cat`, `ls`, …). Prefer dedicated skills when they exist.\n" +
            "4. `LAUNCH_APP` opens an app by package name. `KEY_ACTION` sends BACK/HOME/RECENTS/ENTER/POWER.\n" +
            "5. Machine skills (`VM_POWER`, `VM_WIPE_DATA`, `VM_REBOOT_RECOVERY`, `VM_INSTALL_RECOVERY`) manage the emulator itself. Use them only when the user explicitly asks for machine-level actions; wiping data is destructive — the runtime asks the user to confirm automatically.\n" +
            "6. Safety red line: NEVER type payment passwords, PINs, or confirm payments. Pause and ask the user to complete sensitive credentials themselves.\n" +
            "7. Be concise: briefly say what you are doing on each step and confirm clearly when the task is done."
    }
}

enum class LlmProvider(val displayName: String, val defaultBaseUrl: String, val defaultModel: String, val requiresApiKey: Boolean) {
    OPENAI("OpenAI", "https://api.openai.com/v1", "gpt-4o-mini", true),
    DEEPSEEK("DeepSeek", "https://api.deepseek.com/v1", "deepseek-chat", true),
    QWEN("Qwen", "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-plus", true),
    GEMINI("Gemini", "https://generativelanguage.googleapis.com/v1beta/openai", "gemini-1.5-flash", true),
    MOONSHOT("Moonshot", "https://api.moonshot.cn/v1", "moonshot-v1-8k", true),
    OLLAMA("Ollama", "http://127.0.0.1:11434/v1", "llama3.1", false),
    CUSTOM("Custom", "", "", false);
}

sealed interface LlmChunk {
    data class Delta(val text: String) : LlmChunk
    /** reasoning_content（深度思考模型的思考流） */
    data class Reasoning(val text: String) : LlmChunk
    data object Done : LlmChunk
}

interface LlmClient {
    fun complete(config: LlmConfig, request: JSONObject): Flow<LlmChunk>
}

/** org.json 的 optString 在值为 JSON null 时返回 "null" 字符串——思考模型的 delta 每条都带 "content": null */
private fun JSONObject.realText(key: String): String? =
    if (!has(key) || isNull(key)) null else getString(key).takeIf { it.isNotEmpty() }

class LlmException(message: String) : Exception(message)

/** OpenAI 兼容端点的 SSE 流式客户端（HttpURLConnection + org.json，不引新依赖） */
class OpenAiCompatibleClient : LlmClient {

    override fun complete(config: LlmConfig, request: JSONObject): Flow<LlmChunk> = flow {
        val url = completionsUrl(config.baseUrl)
        val conn = openConnection(url, config.apiKey)
        try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "text/event-stream")
            conn.doOutput = true
            conn.outputStream.use { it.write(request.toString().toByteArray()) }

            val code = conn.responseCode
            if (code !in 200..299) {
                val err = runCatching { conn.errorStream?.bufferedReader()?.readText().orEmpty() }.getOrDefault("")
                throw LlmException("HTTP $code: ${err.take(300)}")
            }

            val ctype = conn.contentType.orEmpty()
            if (!ctype.contains("event-stream")) {
                // 非流式响应：一次性读出
                val text = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                val root = runCatching { JSONObject(text) }.getOrElse { throw LlmException("Malformed response: ${text.take(200)}") }
                root.optJSONObject("error")?.let { throw LlmException(it.optString("message", "API error")) }
                val msg = root.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")
                    ?: throw LlmException("Empty response")
                (msg.realText("reasoning_content") ?: msg.realText("reasoning"))?.let { emit(LlmChunk.Reasoning(it)) }
                msg.realText("content")?.let { emit(LlmChunk.Delta(it)) }
                emit(LlmChunk.Done)
                return@flow
            }

            // SSE：reasoning_content/reasoning 与 content 分轨发出
            val reader = BufferedReader(InputStreamReader(conn.inputStream))
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
                }
            } finally {
                runCatching { reader.close() }
                conn.disconnect()
            }
            emit(LlmChunk.Done)
        } finally {
            runCatching { conn.disconnect() }
        }
    }.flowOn(Dispatchers.IO)

    private fun completionsUrl(base: String): String {
        val b = base.trimEnd('/')
        return if (b.endsWith("/chat/completions")) b else "$b/chat/completions"
    }

    private fun openConnection(url: String, apiKey: String): HttpURLConnection {
        val u = URL(url)
        val c = u.openConnection() as HttpURLConnection
        if (apiKey.isNotBlank()) c.setRequestProperty("Authorization", "Bearer $apiKey")
        c.connectTimeout = 20_000
        c.readTimeout = 300_000
        return c
    }
}
