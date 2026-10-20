package app.andropaw.agent

import kotlinx.coroutines.delay
import org.json.JSONObject

/**
 * AgentPaw for AndroPaw 的工具集。
 *
 * 全部通过 [AgentVmBridge] 作用在模拟器内的 Android 系统上：
 * shell/输入类走 QEMU 通道，截图直读 fb0，机器管理走电源请求文件与宿主侧 LibraryModel。
 * 命名与参数风格对齐 AgentPaw 的 AndroidPhoneTools，模型迁移无成本。
 */

private fun jsonArgs(arguments: String): JSONObject =
    runCatching { JSONObject(arguments) }.getOrElse { JSONObject() }

private fun argStr(o: JSONObject, key: String, def: String = ""): String =
    o.optString(key).ifEmpty { def }

/** 在 guest 里执行任意 shell 命令——万能逃生舱，框架工具（am/pm/settings/input…）都能用。 */
class GuestShellTool(private val bridge: AgentVmBridge) : AgentTool {
    override val definition = ToolDefinition(
        name = "guest_shell",
        description = "Runs a shell command inside the emulated Android system (sh -c). Framework tools like am, pm, settings, input, dumpsys, cat, ls are available. Returns exit code and combined stdout/stderr.",
        parametersSchema = """
        {
          "type": "object",
          "properties": {
            "command": { "type": "string", "description": "The shell command to run inside the emulated Android" },
            "timeout_ms": { "type": "integer", "description": "Timeout in milliseconds (default 60000, max 600000)" }
          },
          "required": ["command"]
        }
        """.trimIndent(),
    )

    override suspend fun execute(arguments: String, context: AgentContext): String {
        val o = jsonArgs(arguments)
        val cmd = argStr(o, "command")
        if (cmd.isBlank()) return "Error: empty command"
        val timeout = o.optLong("timeout_ms", 60_000).coerceIn(1_000, 600_000)
        val (code, out) = bridge.guestShell(cmd, timeout)
        val body = if (out.length > 12_000) out.take(6_000) + "\n... (truncated) ...\n" + out.takeLast(6_000) else out
        return "exit=$code\n$body"
    }
}

/** 截图：直读 framebuffer，任何固件、任何渲染路径下都拿得到画面。 */
class ScreenshotTool(private val bridge: AgentVmBridge) : AgentTool {
    override val definition = ToolDefinition(
        name = "take_screenshot",
        description = "Takes a screenshot of the emulated Android screen (read straight from the emulator framebuffer). Returns the image plus the guest resolution.",
        parametersSchema = """
        {
          "type": "object",
          "properties": {
            "max_dim": { "type": "integer", "description": "Longest edge in pixels, default 1024. Lower saves tokens." }
          }
        }
        """.trimIndent(),
    )

    override suspend fun execute(arguments: String, context: AgentContext): String {
        val o = jsonArgs(arguments)
        val maxDim = o.optInt("max_dim", 1024).coerceIn(240, 1600)
        val b64 = bridge.screenshot(maxDim) ?: return """{"status":"error","message":"No frame available: the machine may still be booting or stopped."}"""
        return JSONObject().apply {
            put("status", "success")
            put("image_base64", b64)
        }.toString()
    }
}

/** 归一化坐标 → guest 像素 */
private fun px(v: Int, size: Int): Int = (v.coerceIn(0, 1000) * (size - 1) / 1000)

private class InputToolsBase(private val bridge: AgentVmBridge) {
    suspend fun guestSize(): Pair<Int, Int> {
        val img = app.andropaw.core.ImageStore.get(bridge.ctx, bridge.imageId ?: app.andropaw.core.VmSession.runningId(bridge.ctx) ?: return (1080 to 1920))
            ?: return (1080 to 1920)
        return img.settings.width to img.settings.height
    }
}

class TapTool(private val bridge: AgentVmBridge) : AgentTool {
    override val definition = ToolDefinition(
        name = "tap",
        description = "Taps the emulated screen at a point. Coordinates are normalized integers in [0,1000] (0,0 top-left).",
        parametersSchema = """
        {
          "type": "object",
          "properties": {
            "x": { "type": "integer", "description": "0..1000" },
            "y": { "type": "integer", "description": "0..1000" }
          },
          "required": ["x", "y"]
        }
        """.trimIndent(),
    )

    override suspend fun execute(arguments: String, context: AgentContext): String {
        val o = jsonArgs(arguments)
        val (w, h) = InputToolsBase(bridge).guestSize()
        val x = px(o.optInt("x", 500), w)
        val y = px(o.optInt("y", 500), h)
        val (code, out) = bridge.guestShell("input tap $x $y", 20_000)
        return if (code == 0) "tapped ($x, $y)" else "tap failed: $out"
    }
}

class DoubleTapTool(private val bridge: AgentVmBridge) : AgentTool {
    override val definition = ToolDefinition(
        name = "double_tap",
        description = "Double-taps the emulated screen at a normalized [0,1000] point.",
        parametersSchema = """
        {
          "type": "object",
          "properties": {
            "x": { "type": "integer" }, "y": { "type": "integer" }
          },
          "required": ["x", "y"]
        }
        """.trimIndent(),
    )

    override suspend fun execute(arguments: String, context: AgentContext): String {
        val o = jsonArgs(arguments)
        val (w, h) = InputToolsBase(bridge).guestSize()
        val x = px(o.optInt("x", 500), w)
        val y = px(o.optInt("y", 500), h)
        bridge.guestShell("input tap $x $y", 20_000)
        delay(80)
        val (code, out) = bridge.guestShell("input tap $x $y", 20_000)
        return if (code == 0) "double-tapped ($x, $y)" else "double-tap failed: $out"
    }
}

class LongPressTool(private val bridge: AgentVmBridge) : AgentTool {
    override val definition = ToolDefinition(
        name = "long_press",
        description = "Long-presses the emulated screen at a normalized [0,1000] point (about 600 ms).",
        parametersSchema = """
        {
          "type": "object",
          "properties": {
            "x": { "type": "integer" }, "y": { "type": "integer" }
          },
          "required": ["x", "y"]
        }
        """.trimIndent(),
    )

    override suspend fun execute(arguments: String, context: AgentContext): String {
        val o = jsonArgs(arguments)
        val (w, h) = InputToolsBase(bridge).guestSize()
        val x = px(o.optInt("x", 500), w)
        val y = px(o.optInt("y", 500), h)
        val (code, out) = bridge.guestShell("input swipe $x $y $x $y 600", 20_000)
        return if (code == 0) "long-pressed ($x, $y)" else "long-press failed: $out"
    }
}

class SwipeTool(private val bridge: AgentVmBridge) : AgentTool {
    override val definition = ToolDefinition(
        name = "swipe",
        description = "Swipes the emulated screen from one normalized [0,1000] point to another.",
        parametersSchema = """
        {
          "type": "object",
          "properties": {
            "x1": { "type": "integer" }, "y1": { "type": "integer" },
            "x2": { "type": "integer" }, "y2": { "type": "integer" },
            "duration_ms": { "type": "integer", "description": "default 300" }
          },
          "required": ["x1", "y1", "x2", "y2"]
        }
        """.trimIndent(),
    )

    override suspend fun execute(arguments: String, context: AgentContext): String {
        val o = jsonArgs(arguments)
        val (w, h) = InputToolsBase(bridge).guestSize()
        val x1 = px(o.optInt("x1", 500), w); val y1 = px(o.optInt("y1", 500), h)
        val x2 = px(o.optInt("x2", 500), w); val y2 = px(o.optInt("y2", 500), h)
        val ms = o.optInt("duration_ms", 300).coerceIn(50, 3000)
        val (code, out) = bridge.guestShell("input swipe $x1 $y1 $x2 $y2 $ms", 20_000)
        return if (code == 0) "swiped ($x1,$y1) → ($x2,$y2)" else "swipe failed: $out"
    }
}

class InputTextTool(private val bridge: AgentVmBridge) : AgentTool {
    override val definition = ToolDefinition(
        name = "input_text",
        description = "Types text into the focused field of the emulated Android.",
        parametersSchema = """
        {
          "type": "object",
          "properties": {
            "text": { "type": "string" }
          },
          "required": ["text"]
        }
        """.trimIndent(),
    )

    override suspend fun execute(arguments: String, context: AgentContext): String {
        val text = jsonArgs(arguments).optString("text")
        if (text.isEmpty()) return "Error: empty text"
        // guest sh 单引号转义；空格换成 %s（input text 的惯例）
        val safe = text.replace("'", "'\\''").replace(" ", "%s")
        val (code, out) = bridge.guestShell("input text '$safe'", 30_000)
        return if (code == 0) "typed ${text.length} chars" else "input failed: $out"
    }
}

class KeyActionTool(private val bridge: AgentVmBridge) : AgentTool {
    override val definition = ToolDefinition(
        name = "key_action",
        description = "Sends a hardware key to the emulated Android.",
        parametersSchema = """
        {
          "type": "object",
          "properties": {
            "key": { "type": "string", "enum": ["BACK", "HOME", "RECENTS", "ENTER", "POWER", "MENU", "VOLUME_UP", "VOLUME_DOWN"] }
          },
          "required": ["key"]
        }
        """.trimIndent(),
    )

    override suspend fun execute(arguments: String, context: AgentContext): String {
        val key = jsonArgs(arguments).optString("key", "BACK").uppercase()
        val code = when (key) {
            "BACK" -> 82; "HOME" -> 3; "RECENTS" -> 187; "ENTER" -> 66
            "POWER" -> 26; "MENU" -> 82; "VOLUME_UP" -> 24; "VOLUME_DOWN" -> 25
            else -> return "Error: unknown key '$key'"
        }
        val (c, out) = bridge.guestShell("input keyevent $code", 20_000)
        return if (c == 0) "sent $key" else "key failed: $out"
    }
}

class LaunchAppTool(private val bridge: AgentVmBridge) : AgentTool {
    override val definition = ToolDefinition(
        name = "launch_app",
        description = "Launches an app inside the emulated Android by package name.",
        parametersSchema = """
        {
          "type": "object",
          "properties": {
            "package": { "type": "string", "description": "e.g. com.android.settings" }
          },
          "required": ["package"]
        }
        """.trimIndent(),
    )

    override suspend fun execute(arguments: String, context: AgentContext): String {
        val pkg = jsonArgs(arguments).optString("package")
        if (pkg.isEmpty()) return "Error: empty package"
        val (c, out) = bridge.guestShell("monkey -p $pkg -c android.intent.category.LAUNCHER 1", 30_000)
        return if (c == 0) "launched $pkg" else "launch failed (not installed?): $out"
    }
}

class WaitTool : AgentTool {
    override val definition = ToolDefinition(
        name = "wait_seconds",
        description = "Waits while the emulated Android loads or animates.",
        parametersSchema = """
        {
          "type": "object",
          "properties": {
            "seconds": { "type": "integer", "description": "1..30" }
          },
          "required": ["seconds"]
        }
        """.trimIndent(),
    )

    override suspend fun execute(arguments: String, context: AgentContext): String {
        val s = jsonArgs(arguments).optInt("seconds", 2).coerceIn(1, 30)
        delay(s * 1000L)
        return "waited ${s}s"
    }
}

/** 机器状态速览：agent 开场先确认自己在操作哪台机器。 */
class MachineStateTool(private val bridge: AgentVmBridge) : AgentTool {
    override val definition = ToolDefinition(
        name = "vm_machine_state",
        description = "Reports which emulated machine the agent is attached to and whether it is running.",
        parametersSchema = """{ "type": "object", "properties": {} }""".trimIndent(),
    )

    override suspend fun execute(arguments: String, context: AgentContext): String {
        val name = bridge.targetName()
        return if (name == null) {
            "No machine is running. Ask the user to start a machine from the emulator page."
        } else {
            "Attached to machine \"$name\" (${if (bridge.isMachineRunning()) "running" else "stopped"})."
        }
    }
}

/** 机器电源：重启 / 关机（经 guest 电源请求文件，:vm 的 watchdog 执行）。 */
class VmPowerTool(private val bridge: AgentVmBridge) : AgentTool {
    override val definition = ToolDefinition(
        name = "vm_power",
        description = "Powers the emulated machine: reboot or shutdown. The machine restarts or stops as a whole. Only use when the user explicitly asks.",
        parametersSchema = """
        {
          "type": "object",
          "properties": {
            "action": { "type": "string", "enum": ["reboot", "shutdown"] }
          },
          "required": ["action"]
        }
        """.trimIndent(),
    )

    override suspend fun execute(arguments: String, context: AgentContext): String {
        val action = jsonArgs(arguments).optString("action", "reboot").lowercase()
        return when (action) {
            "reboot" -> bridge.powerRequest("reboot")
            "shutdown" -> bridge.powerRequest("shutdown")
            else -> "Error: action must be reboot or shutdown"
        }
    }
}

class VmRebootRecoveryTool(private val bridge: AgentVmBridge) : AgentTool {
    override val definition = ToolDefinition(
        name = "vm_reboot_recovery",
        description = "Reboots the emulated machine into its recovery (requires a recovery installed). Only use when the user explicitly asks.",
        parametersSchema = """{ "type": "object", "properties": {} }""".trimIndent(),
    )

    override suspend fun execute(arguments: String, context: AgentContext): String =
        bridge.powerRequest("reboot,recovery")
}

/** 擦数据：破坏性操作，仅当用户明确要求时调用。 */
class VmWipeDataTool(private val bridge: AgentVmBridge) : AgentTool {
    override val definition = ToolDefinition(
        name = "vm_wipe_data",
        description = "DESTRUCTIVE: factory-resets the emulated machine (wipes its data partition). The machine must be powered off first. Only use when the user explicitly asks for a wipe/factory reset.",
        parametersSchema = """{ "type": "object", "properties": {} }""".trimIndent(),
    )

    override suspend fun execute(arguments: String, context: AgentContext): String = bridge.wipeData()
}

class VmInstallRecoveryTool(private val bridge: AgentVmBridge) : AgentTool {
    override val definition = ToolDefinition(
        name = "vm_install_recovery",
        description = "Installs a recovery image into the emulated machine from a file (a host path or a path on the machine's shared sdcard). After installing, reboot to recovery.",
        parametersSchema = """
        {
          "type": "object",
          "properties": {
            "path": { "type": "string", "description": "Path to the recovery image file" }
          },
          "required": ["path"]
        }
        """.trimIndent(),
    )

    override suspend fun execute(arguments: String, context: AgentContext): String {
        val path = jsonArgs(arguments).optString("path")
        if (path.isBlank()) return "Error: empty path"
        return bridge.installRecovery(path)
    }
}

/** APAnP 的完整工具表。 */
fun apnpTools(bridge: AgentVmBridge): List<AgentTool> = listOf(
    MachineStateTool(bridge),
    ScreenshotTool(bridge),
    TapTool(bridge),
    DoubleTapTool(bridge),
    LongPressTool(bridge),
    SwipeTool(bridge),
    InputTextTool(bridge),
    KeyActionTool(bridge),
    LaunchAppTool(bridge),
    WaitTool(),
    GuestShellTool(bridge),
    VmPowerTool(bridge),
    VmRebootRecoveryTool(bridge),
    VmWipeDataTool(bridge),
    VmInstallRecoveryTool(bridge),
)
