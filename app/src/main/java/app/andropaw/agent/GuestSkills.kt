package app.andropaw.agent

import org.json.JSONObject

/**
 * AndroPaw 自己的技能执行器（对位 Ki 的 SkillExecutor）：所有技能都落在
 * [AgentVmBridge] 的 guest 通道里——QEMU 跑 guest 命令、fb0 直读截图、
 * aemu_power 机器请求——不触碰手机宿主端。
 */
object GuestSkills {

    data class Result(
        val success: Boolean,
        val message: String,
        val card: SkillCardData? = null,
    )

    /** 技能的显示名与参数表（协议文档与卡片标题共用） */
    val CATALOG: Map<SkillType, Pair<String, String>> = mapOf(
        SkillType.GUEST_SHELL to ("Guest Shell" to "command: the shell command to run inside the emulated Android"),
        SkillType.TAKE_SCREENSHOT to ("Take Screenshot" to "no parameters: captures the current screen"),
        SkillType.TAP to ("Tap" to "x, y: normalized coordinates in [0, 1000] (0,0 = top-left)"),
        SkillType.DOUBLE_TAP to ("Double Tap" to "x, y: normalized coordinates in [0, 1000]"),
        SkillType.LONG_PRESS to ("Long Press" to "x, y: normalized coordinates; duration_ms: optional, default 800"),
        SkillType.SWIPE to ("Swipe" to "x1, y1, x2, y2: normalized coordinates; duration_ms: optional, default 300"),
        SkillType.INPUT_TEXT to ("Input Text" to "text: the text to type into the focused field"),
        SkillType.KEY_ACTION to ("Key Action" to "key: BACK | HOME | RECENTS | ENTER | POWER | VOLUME_UP | VOLUME_DOWN | MENU"),
        SkillType.LAUNCH_APP to ("Launch App" to "package: the package name, e.g. com.android.settings"),
        SkillType.OPEN_DEEPLINK to ("Open Deep Link" to "uri: the deep-link URI to open"),
        SkillType.WAIT_SECONDS to ("Wait" to "seconds: how long to wait (max 30)"),
        SkillType.GET_SCREEN_STATE to ("Screen State" to "no parameters: dumps the focused window / display state"),
        SkillType.VM_STATE to ("Machine State" to "no parameters: reports boot state, Android version and model"),
        SkillType.VM_POWER to ("Machine Power" to "action: reboot | shutdown (requires user confirmation)"),
        SkillType.VM_REBOOT_RECOVERY to ("Reboot to Recovery" to "no parameters: reboots into the installed recovery"),
        SkillType.VM_WIPE_DATA to ("Wipe Data" to "confirm: must be the literal string yes (requires user confirmation)"),
        SkillType.VM_INSTALL_RECOVERY to ("Install Recovery" to "path: host or sdcard path of the recovery image (requires user confirmation)"),
    )

    /** 供系统提示词追加的调用协议段（保留我方提示词主体，只把调用方式换成 Ki 的 XML 协议） */
    fun protocolPrompt(): String = buildString {
        appendLine()
        appendLine("## Skill invocation format (<tool_call> XML)")
        appendLine("You control the emulated Android ONLY by emitting skill cards. You cannot execute anything yourself, cannot see files, and never receive results you did not obtain through a card.")
        appendLine("To call a skill, output exactly:")
        appendLine("```xml")
        appendLine("<tool_call>")
        appendLine("  <tool_name>SKILL_TYPE</tool_name>")
        appendLine("  <parameter name=\"param1\">value</parameter>")
        appendLine("  <parameter name=\"param2\">value</parameter>")
        appendLine("</tool_call>")
        appendLine("```")
        appendLine("The runtime parses your reply, executes the skill and sends the result back as a [Skill Result] message. Then continue: analyze the result, emit the next card, or answer the user.")
        appendLine("Rules:")
        appendLine("1. NEVER fabricate results. NEVER claim an action is done unless you received a [Skill Result] saying so.")
        appendLine("2. One card per action; several cards in one reply are allowed and run in order.")
        appendLine("3. All coordinates are normalized integers in [0, 1000], (0,0) is top-left.")
        appendLine("4. Machine-level skills (VM_POWER shutdown, VM_WIPE_DATA, VM_INSTALL_RECOVERY) raise a user confirmation card automatically; do not try to bypass it.")
        appendLine("5. Never type payment passwords or PINs; hand sensitive credentials to the user.")
        appendLine("6. When the task is done and no more skills are needed, reply in plain text only — that ends the turn.")
        appendLine()
        appendLine("Available skills:")
        for ((type, meta) in CATALOG.entries.sortedBy { it.key.name }) {
            appendLine("- ${type.name} — ${meta.first}: ${meta.second}")
        }
    }.trimEnd()

    /** 危险操作检测（对位 Ki 的 checkDangerous，简化到机器级操作） */
    fun checkDangerous(type: SkillType, params: JSONObject): String? = when (type) {
        SkillType.VM_WIPE_DATA ->
            if (params.optString("confirm") == "yes") "即将擦除这台模拟器的全部数据（应用、设置、文件），不可恢复"
            else null
        SkillType.VM_POWER ->
            if (params.optString("action") == "shutdown") "即将关闭这台模拟器，未保存的guest状态会丢失"
            else null
        SkillType.VM_INSTALL_RECOVERY -> "将向机器写入 Recovery 镜像（覆盖已有的 recovery 分区）"
        else -> null
    }

    fun dangerousActionDesc(type: SkillType, params: JSONObject): String = when (type) {
        SkillType.VM_WIPE_DATA -> "擦除模拟器数据（wipe data）"
        SkillType.VM_POWER -> "机器电源操作：${params.optString("action")}"
        SkillType.VM_INSTALL_RECOVERY -> "安装 Recovery：${params.optString("path")}"
        else -> type.name
    }

    /** 卡片标题：工具显示名 */
    fun title(type: SkillType): String = CATALOG[type]?.first ?: type.name

    /** 执行一个技能，返回执行结果（卡片状态已填好） */
    suspend fun execute(bridge: AgentVmBridge, type: SkillType, params: JSONObject): Result {
        fun p(key: String): String = params.optString(key).trim()
        fun fail(msg: String) = Result(false, msg)

        return runCatching {
            when (type) {
                SkillType.GUEST_SHELL -> {
                    val cmd = p("command")
                    if (cmd.isBlank()) return fail("command is empty")
                    val (code, out) = bridge.guestShell(cmd, 60_000)
                    val trimmed = out.trim().take(4000)
                    Result(success = code == 0, message = trimmed.ifBlank { "(no output, exit $code)" }).let {
                        it.copy(card = cardOf(type, params, it, output = it.message, command = cmd))
                    }
                }
                SkillType.TAKE_SCREENSHOT -> {
                    val b64 = bridge.screenshot(1024)
                        ?: return fail("No frame available: the machine may still be booting or stopped.")
                    val json = JSONObject().put("status", "ok").put("image_base64", b64)
                    Result(true, json.toString(), card = SkillCardData(
                        skillType = type, title = title(type),
                        description = "Screenshot captured (shown below)",
                        status = SkillStatus.COMPLETED, imageBase64 = b64,
                    ))
                }
                SkillType.TAP -> inputOk(bridge, "input tap ${pxy(bridge, p("x"), p("y"))}", type, params)
                SkillType.DOUBLE_TAP -> {
                    val xy = pxy(bridge, p("x"), p("y"))
                    inputOk(bridge, "input tap $xy; input tap $xy", type, params)
                }
                SkillType.LONG_PRESS -> {
                    val d = p("duration_ms").toIntOrNull()?.coerceIn(200, 5000) ?: 800
                    val xy = pxy(bridge, p("x"), p("y"))
                    inputOk(bridge, "input swipe $xy $xy $d", type, params)
                }
                SkillType.SWIPE -> {
                    val d = p("duration_ms").toIntOrNull()?.coerceIn(100, 3000) ?: 300
                    inputOk(bridge, "input swipe ${pxy(bridge, p("x1"), p("y1"))} ${pxy(bridge, p("x2"), p("y2"))} $d", type, params)
                }
                SkillType.INPUT_TEXT -> {
                    val text = p("text")
                    if (text.isBlank()) return fail("text is empty")
                    // input text 对空格/引号敏感：包一层 sh 引号转义
                    val escaped = text.replace("\\", "\\\\").replace(" ", "%s").replace("\"", "\\\"")
                    inputOk(bridge, "input text \"$escaped\"", type, params)
                }
                SkillType.KEY_ACTION -> {
                    val key = p("key").uppercase()
                    val allowed = mapOf(
                        "BACK" to "KEYCODE_BACK", "HOME" to "KEYCODE_HOME", "RECENTS" to "KEYCODE_APP_SWITCH",
                        "ENTER" to "KEYCODE_ENTER", "POWER" to "KEYCODE_POWER", "VOLUME_UP" to "KEYCODE_VOLUME_UP",
                        "VOLUME_DOWN" to "KEYCODE_VOLUME_DOWN", "MENU" to "KEYCODE_MENU",
                    )
                    val code = allowed[key] ?: return fail("unknown key: $key")
                    inputOk(bridge, "input keyevent $code", type, params)
                }
                SkillType.LAUNCH_APP -> {
                    val pkg = p("package")
                    if (pkg.isBlank()) return fail("package is empty")
                    shell(bridge, "monkey -p $pkg -c android.intent.category.LAUNCHER 1", type, params)
                }
                SkillType.OPEN_DEEPLINK -> {
                    val uri = p("uri")
                    if (uri.isBlank()) return fail("uri is empty")
                    shell(bridge, "am start -a android.intent.action.VIEW -d '$uri'", type, params)
                }
                SkillType.WAIT_SECONDS -> {
                    val s = (p("seconds").toDoubleOrNull() ?: 1.0).coerceIn(0.5, 30.0)
                    kotlinx.coroutines.delay((s * 1000).toLong())
                    Result(true, "waited ${s}s").let { it.copy(card = cardOf(type, params, it)) }
                }
                SkillType.GET_SCREEN_STATE -> {
                    val (_, out) = bridge.guestShell("dumpsys window windows 2>/dev/null | grep -E 'mCurrentFocus|mFocusedApp' ; dumpsys display | grep -E 'mScreenState|mDisplayWidth' | head -4", 20_000)
                    Result(true, out.trim().take(2000)).let { it.copy(card = cardOf(type, params, it, output = out.trim().take(2000))) }
                }
                SkillType.VM_STATE -> {
                    val (_, out) = bridge.guestShell("getprop ro.build.version.release; getprop ro.product.model; getprop sys.boot_completed", 20_000)
                    Result(true, "Android " + out.trim().replace('\n', ' ')).let {
                        it.copy(card = cardOf(type, params, it, output = it.message))
                    }
                }
                SkillType.VM_POWER -> {
                    val action = p("action")
                    val value = when (action) {
                        "reboot" -> "reboot"
                        "shutdown" -> "shutdown"
                        else -> return fail("unknown action: $action (use reboot | shutdown)")
                    }
                    val msg = bridge.powerRequest(value)
                    Result(true, msg).let { it.copy(card = cardOf(type, params, it)) }
                }
                SkillType.VM_REBOOT_RECOVERY -> {
                    val msg = bridge.powerRequest("reboot,recovery")
                    Result(true, msg).let { it.copy(card = cardOf(type, params, it)) }
                }
                SkillType.VM_WIPE_DATA -> {
                    if (p("confirm") != "yes") return fail("confirm must be the literal string \"yes\"")
                    Result(true, bridge.wipeData()).let { it.copy(card = cardOf(type, params, it)) }
                }
                SkillType.VM_INSTALL_RECOVERY -> {
                    val path = p("path")
                    if (path.isBlank()) return fail("path is empty")
                    Result(true, bridge.installRecovery(path)).let { it.copy(card = cardOf(type, params, it)) }
                }
                SkillType.CONFIRM_DANGEROUS, SkillType.CONFIRM_DUPLICATE ->
                    fail("interactive card, not executable")
            }
        }.getOrElse { e -> Result(false, "Execution error: ${e.message}") }
    }

    private suspend fun shell(bridge: AgentVmBridge, cmd: String, type: SkillType, params: JSONObject): Result {
        val (code, out) = bridge.guestShell(cmd, 30_000)
        val ok = code == 0
        return Result(ok, out.trim().take(2000).ifBlank { if (ok) "done" else "exit $code" })
            .let { it.copy(card = cardOf(type, params, it, command = cmd)) }
    }

    private suspend fun inputOk(bridge: AgentVmBridge, cmd: String, type: SkillType, params: JSONObject): Result {
        val (code, out) = bridge.guestShell(cmd, 20_000)
        val ok = code == 0
        return Result(ok, if (ok) "done" else out.trim().take(500).ifBlank { "exit $code" })
            .let { it.copy(card = cardOf(type, params, it, command = cmd)) }
    }

    /** 协议坐标是归一化 [0,1000]，guest 的 input 要真实像素：按机器分辨率换算成 "x y" */
    private suspend fun pxy(bridge: AgentVmBridge, x: String, y: String): String {
        val (w, h) = bridge.guestSize()
        fun px(v: String, axis: Int): Int {
            val n = (v.toIntOrNull() ?: 0).coerceIn(0, 1000)
            return n * axis / 1000
        }
        return "${px(x, w)} ${px(y, h)}"
    }

    private fun cardOf(type: SkillType, params: JSONObject, r: Result, output: String? = null, command: String? = null) = SkillCardData(
        skillType = type,
        title = title(type),
        description = r.message.take(300).ifBlank { if (r.success) "done" else "failed" },
        status = if (r.success) SkillStatus.COMPLETED else SkillStatus.FAILED,
        command = command,
        output = (output ?: r.message).take(4000).ifBlank { null },
    )
}
