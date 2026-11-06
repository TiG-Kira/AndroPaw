package app.andropaw.core

import android.content.Context
import android.os.Build
import android.provider.Settings

/** Host facts the engine and the settings page need. */
object HostInfo {
    /** GPU driver (adreno, mali, powervr…) from ro.hardware.egl. */
    fun gpu(): String = runCatching {
        val c = Class.forName("android.os.SystemProperties")
        (c.getMethod("get", String::class.java).invoke(null, "ro.hardware.egl") as String).lowercase()
    }.getOrDefault("")

    /**
     * Android 12+ Phantom Process Killer: the host SIGKILLs an app's forked children in a batch
     * once the global count exceeds 32 (ActivityManagerConstants.MAX_PHANTOM_PROCESSES), whatever
     * the free RAM is. It identifies them through the uid cgroup accounting, not the /proc parent
     * chain — reparenting or daemonising does not escape it (and makes them easier to kill).
     * This setting is exactly what the developer option 「停止限制子进程」 toggles.
     */
    enum class PhantomState { NOT_APPLICABLE, DISABLED, ACTIVE, UNKNOWN }

    fun phantomState(ctx: Context): PhantomState {
        if (Build.VERSION.SDK_INT < 31) return PhantomState.NOT_APPLICABLE
        val v = runCatching {
            Settings.Global.getString(ctx.contentResolver, "settings_enable_monitor_phantom_procs")
        }.getOrNull()?.lowercase()
        return when (v) {
            "false", "0" -> PhantomState.DISABLED
            "true", "1" -> PhantomState.ACTIVE
            // unset: the platform feature flag default is monitoring enabled
            else -> PhantomState.UNKNOWN
        }
    }

    /** Android 14+ silently resets the child-process toggle when developer options are turned off. */
    fun developerOptionsEnabled(ctx: Context): Boolean = runCatching {
        Settings.Global.getInt(ctx.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0
    }.getOrDefault(false)

    /** The only host-side change that actually raises the ceiling (copied by the settings button). */
    const val ADB_RAISE_LIMIT =
        "adb shell device_config put activity_manager max_phantom_processes 2147483647\n" +
            "adb shell device_config set_sync_disabled_for_tests persistent"

    /** Host-version specific steps, shown verbatim on the settings page. */
    fun phantomGuide(): String {
        // 不用电脑的那条路放最前面：adb 对大多数用户来说不可行，Shizuku 才是能落地的方案。
        val shizuku =
            "【最省事：不用电脑】在本页把「Shizuku」授权，再打开「运行期间暂停宿主进程杀手」：\n" +
                "  开机前自动把 max_phantom_processes 改成 2147483647，最后一台模拟器停止后写回你原来的值。\n" +
                "  Shizuku 在设备重启后会停止，需要重新用无线调试启动一次。\n" +
                "  这是唯一能做到「根治」的路径 —— 上限抬走之后就不存在整批 SIGKILL 了。\n\n"
        val steps = when {
            Build.VERSION.SDK_INT >= 34 ->
                "【有电脑时】用 adb 执行（可点下方按钮复制）：\n" +
                    "   adb shell device_config put activity_manager max_phantom_processes 2147483647\n" +
                    "   adb shell device_config set_sync_disabled_for_tests persistent\n\n" +
                    "「停用子进程限制」只是关掉监控开关，并不提高 32 这个全局上限；HyperOS / ColorOS / " +
                    "Android 16+ 还常常直接忽略该开关。所以只开开关仍被杀属于正常现象。\n\n" +
                    "【同时做】设置 → 开发者选项 → 停用子进程限制 / Disable child process restrictions，" +
                    "并保持开发者选项总开关开启（关掉会自动复位）。"
            Build.VERSION.SDK_INT >= 32 ->
                "【有电脑时】用 adb 执行（可点下方按钮复制）：\n" +
                    "   adb shell settings put global settings_enable_monitor_phantom_procs false\n" +
                    "   adb shell device_config put activity_manager max_phantom_processes 2147483647\n" +
                    "   adb shell device_config set_sync_disabled_for_tests persistent\n" +
                    "开关值可能被 Google Play 服务的 device_config 同步覆盖，最后一条用于阻止覆盖。"
            else ->
                "【有电脑时】用 adb 执行（可点下方按钮复制）：\n" +
                    "   adb shell device_config put activity_manager max_phantom_processes 2147483647\n" +
                    "   adb shell device_config set_sync_disabled_for_tests persistent"
        }
        return shizuku + steps +
            "\n\n【都不做时的兜底】开机前会按宿主预算（29 个常驻进程）反推精简档位，" +
            "把诊断 / 可选服务改成按需启动，第一次开机就选对档位而不是失败两次再收敛；" +
            "仍超出预算时开机日志会写明实际数字，并最多自动重试 6 次（逐级更精简 + 退避）。" +
            "当常驻服务本身就超过 32 个时，软件侧再也压不下去 —— 那时只有上面的 Shizuku 或 adb 能解决。" +
            "\n\n另外请把 AndroPaw 的省电策略设为「无限制」，并在最近任务里锁定该应用。"
    }
}
