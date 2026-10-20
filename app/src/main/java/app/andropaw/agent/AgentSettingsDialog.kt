package app.andropaw.agent

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.andropaw.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * AgentPaw for AndroPaw 的 LLM 设置对话框（设置页 “AgentPaw for AndroPaw” 组进入）。
 * 任意 OpenAI 兼容端点：provider 预设 + 自定义 baseUrl/apiKey/model。
 */
@Composable
fun AgentSettingsDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val initial = remember { AgentStore.config(ctx) }
    var provider by remember { mutableStateOf(initial.provider) }
    var baseUrl by remember { mutableStateOf(initial.baseUrl) }
    var apiKey by remember { mutableStateOf(initial.apiKey) }
    var model by remember { mutableStateOf(initial.model) }
    var temperature by remember { mutableStateOf(initial.temperature) }
    var maxTokens by remember { mutableStateOf(initial.maxTokens.toFloat()) }
    var maxRounds by remember { mutableStateOf(initial.maxToolRounds.toFloat()) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun cfg() = LlmConfig(
        provider = provider,
        baseUrl = baseUrl.ifBlank { provider.defaultBaseUrl },
        apiKey = apiKey,
        model = model.ifBlank { provider.defaultModel },
        temperature = temperature,
        maxTokens = maxTokens.toInt(),
        maxToolRounds = maxRounds.toInt(),
        systemPrompt = initial.systemPrompt,
    )

    WindowDialog(
        show = true,
        title = stringResource(R.string.agent_settings_group),
        summary = stringResource(R.string.agent_settings_sub),
        onDismissRequest = onDismiss,
    ) {
        Column(
            Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // provider
            Text(stringResource(R.string.agent_provider), fontSize = 13.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            LlmProvider.entries.forEach { p ->
                BasicComponent(
                    title = p.displayName,
                    summary = p.defaultBaseUrl.ifBlank { stringResource(R.string.agent_custom_hint) },
                    startAction = { RadioButton(selected = provider == p, onClick = null) },
                    onClick = {
                        provider = p
                        if (baseUrl.isEmpty() || baseUrl == provider.defaultBaseUrl) baseUrl = p.defaultBaseUrl
                        if (model.isEmpty() || model == provider.defaultModel) model = p.defaultModel
                    },
                )
            }
            Spacer(Modifier.height(4.dp))
            TextField(value = baseUrl, onValueChange = { baseUrl = it }, label = stringResource(R.string.agent_base_url))
            TextField(
                value = apiKey, onValueChange = { apiKey = it },
                label = stringResource(R.string.agent_api_key),
                visualTransformation = PasswordVisualTransformation(),
            )
            TextField(value = model, onValueChange = { model = it }, label = stringResource(R.string.agent_model))

            Text(stringResource(R.string.agent_temperature, temperature), fontSize = 13.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            Slider(temperature, { temperature = it }, valueRange = 0f..1f)

            Text(stringResource(R.string.agent_max_tokens, maxTokens.toInt()), fontSize = 13.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            Slider(maxTokens, { maxTokens = it }, valueRange = 256f..8192f)

            Text(stringResource(R.string.agent_max_rounds, maxRounds.toInt()), fontSize = 13.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            Slider(maxRounds, { maxRounds = it }, valueRange = 1f..30f)

            testResult?.let {
                Text(it, fontSize = 12.sp,
                    color = if (it.startsWith("✓")) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.error)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(text = stringResource(R.string.agent_test), enabled = !testing, onClick = {
                    testing = true; testResult = null
                    scope.launch {
                        val c = cfg()
                        val r = withContext(Dispatchers.IO) {
                            runCatching {
                                val req = org.json.JSONObject().apply {
                                    put("model", c.model)
                                    put("messages", org.json.JSONArray().put(
                                        org.json.JSONObject().put("role", "user").put("content", "ping")))
                                    put("max_tokens", 8)
                                    put("stream", false)
                                }
                                var got = ""
                                OpenAiCompatibleClient().complete(c, req).collect { chunk ->
                                    if (chunk is LlmChunk.Delta) got += chunk.text
                                }
                                "✓ OK"
                            }.getOrElse { "✗ ${it.message}" }
                        }
                        testing = false; testResult = r
                    }
                })
                Spacer(Modifier.weight(1f))
                TextButton(text = stringResource(R.string.agent_reset_prompt), onClick = {
                    AgentStore.saveConfig(ctx, cfg().copy(systemPrompt = LlmConfig.DEFAULT_SYSTEM_PROMPT))
                    testResult = ctx.getString(R.string.agent_prompt_reset)
                })
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(text = stringResource(R.string.cancel), onClick = onDismiss)
                Spacer(Modifier.weight(1f))
                TextButton(text = stringResource(R.string.ok), onClick = {
                    AgentStore.saveConfig(ctx, cfg())
                    onDismiss()
                })
            }
        }
    }
}

/** 供其他地方复用：保存配置的紧凑入口 */
fun saveAgentConfig(ctx: Context, c: LlmConfig) = AgentStore.saveConfig(ctx, c)
