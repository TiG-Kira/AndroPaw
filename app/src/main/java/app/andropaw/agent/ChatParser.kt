package app.andropaw.agent

import org.json.JSONObject

/**
 * 从模型回复中解析技能块（照搬 Ki 的 SkillExecutor.parseSkillBlocks / stripSkillBlocks，
 * JSON 解析从 gson 换成 org.json）。支持三种写法：
 *  1. ```json 代码块包 {"skillType": ..., "params": {...}}
 *  2. 行业标准 <tool_call> XML（<tool_name> + <parameter name="...">）
 *  3. 内联 JSON 兜底
 */
object ChatParser {

    fun parseSkillBlocks(content: String): List<Pair<String, JSONObject>> {
        val results = mutableListOf<Pair<String, JSONObject>>()
        val seen = mutableSetOf<String>()

        fun add(type: String?, params: JSONObject?) {
            val raw = type?.trim()?.uppercase() ?: return
            if (SkillType.of(raw) == null) return
            val p = params ?: JSONObject()
            val key = canonicalSkillKey(raw, p)
            if (seen.add(key)) results.add(raw to p)
        }

        // 策略 1: ```skill / ```json 等代码块包裹的 JSON
        val blockPattern = Regex("""```[a-zA-Z]*\s*\r?\n([\s\S]*?)[\r\n]*```""", RegexOption.DOT_MATCHES_ALL)
        for (match in blockPattern.findAll(content)) {
            val jsonStr = match.groupValues.getOrNull(1)?.trim() ?: continue
            runCatching {
                val json = JSONObject(jsonStr)
                add(json.optString("skillType"), json.optJSONObject("params"))
            }
        }

        // 策略 2: 行业标准 <tool_call> XML
        val toolCallPattern = Regex("""<tool_call>([\s\S]*?)</tool_call>""", RegexOption.DOT_MATCHES_ALL)
        val namePattern = Regex("""<tool_name>\s*([\s\S]*?)\s*</tool_name>""", RegexOption.DOT_MATCHES_ALL)
        val paramPattern = Regex("""<parameter\s+name\s*=\s*["']([^"']+)["']\s*>([\s\S]*?)</parameter>""", RegexOption.DOT_MATCHES_ALL)
        for (match in toolCallPattern.findAll(content)) {
            val block = match.groupValues.getOrNull(1)?.trim() ?: continue
            val nameMatch = namePattern.find(block) ?: continue
            val params = JSONObject()
            paramPattern.findAll(block).forEach { pm ->
                params.put(pm.groupValues[1].trim(), pm.groupValues[2].trim())
            }
            add(nameMatch.groupValues[1], params)
        }

        // 策略 3: 内联 JSON 兜底
        if (results.isEmpty()) {
            val jsonPattern = Regex("""\{[^{}]*"skillType"[^{}]*\}""", RegexOption.DOT_MATCHES_ALL)
            for (match in jsonPattern.findAll(content)) {
                runCatching {
                    val json = JSONObject(match.value.trim())
                    add(json.optString("skillType"), json.optJSONObject("params"))
                }
            }
        }
        return results
    }

    /** 技能去重键：参数按 key 排序后拼接 */
    fun canonicalSkillKey(skillType: String, params: JSONObject): String {
        val sb = StringBuilder(skillType)
        params.keys().asSequence().sorted().forEach { key ->
            sb.append('|').append(key).append('=').append(params.optString(key))
        }
        return sb.toString()
    }

    /** 提取技能块以外的纯文本回复 */
    fun stripSkillBlocks(content: String): String {
        var result = content
        val blockPattern = Regex("""```[a-zA-Z]*\s*\r?\n([\s\S]*?)[\r\n]*```""", RegexOption.DOT_MATCHES_ALL)
        result = blockPattern.replace(result) { match ->
            val jsonStr = match.groupValues.getOrNull(1)?.trim() ?: return@replace match.value
            runCatching {
                val json = JSONObject(jsonStr)
                if (json.optString("skillType").isNotBlank()) "" else match.value
            }.getOrDefault(match.value)
        }
        result = Regex("""<tool_call>([\s\S]*?)</tool_call>""", RegexOption.DOT_MATCHES_ALL).replace(result, "").trim()
        result = Regex("""\{[^{}]*"skillType"[^{}]*\}""", RegexOption.DOT_MATCHES_ALL).replace(result, "").trim()
        return result.trim()
    }
}
