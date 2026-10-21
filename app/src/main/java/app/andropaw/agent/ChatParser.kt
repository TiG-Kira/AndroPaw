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

        // 策略 2: 行业标准 <tool_call> XML；宽容处理没写闭合标签的块（写到回复末尾）
        val toolCallPattern = Regex("""<tool_call>([\s\S]*?)(?:</tool_call>|(?=<tool_call>)|$)""", RegexOption.DOT_MATCHES_ALL)
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

    /**
     * 回复清洗：模型（尤其 mimo/qwen 系）经常在技能块后面喷出一串孤儿闭合标签
     * （</invoke></parameter></antml:function_calls>…），甚至整段陷入闭合标签复读。
     * 这里把孤儿闭合标签删掉、把连续重复行截断，得到干净的可展示/可解析文本。
     */
    fun sanitizeReply(content: String): String {
        var s = content
        // 1) antml 系（Claude 端点串味的产物）整块移除
        s = Regex("""</?antml:[^>]*>""").replace(s, "")
        // 2) 孤儿闭合标签：不配对的 </invoke> </parameter> </function_calls> 连串删掉
        //    （成对的 </tool_call> 已被解析器消化，这里针对的是解析剩下的孤儿）
        // 2) 孤儿闭合标签：</invoke>/</function_calls> 没有对应开标签，一律删；
        //    </parameter> 只能删"多余的"——合法卡里成对的必须保留，
        //    否则参数块失去闭合、参数全丢（LAUNCH_APP 报 package is empty 就是这个）
        // 2) 只删我们没有开标签的孤儿闭合标签（</invoke>、</antml:...>）。
        //    </parameter> 一律不碰：合法卡里成对的必须保留，否则参数全丢
        //    （LAUNCH_APP 报 package is empty 就是把它误删造成的）
        s = Regex("""(?:\s*</(?:invoke|function_calls)>)+""").replace(s, " ")
        // 3) 连续重复行复读：同一行连出 3 次以上 → 保留一行并截断后面的复读
        val out = StringBuilder()
        var prev: String? = null
        var repeats = 0
        for (line in s.lines()) {
            if (line.isNotBlank() && line == prev) {
                repeats++
                if (repeats >= 3) continue
            } else { prev = line; repeats = 1 }
            out.append(line).append(10.toChar())
        }
        return out.toString().trim()
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
