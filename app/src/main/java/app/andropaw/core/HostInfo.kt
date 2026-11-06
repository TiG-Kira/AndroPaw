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
        val steps = when {
            Build.VERSION.SDK_INT >= 34 ->
                "【首选：真正提高上限】用电脑 adb 执行（可点下方按钮复制）：\n" +
                    "   adb shell device_config put activity_manager max_phantom_processes 2147483647\n" +
                    "   adb shell device_config set_sync_disabled_for_tests persistent\n\n" +
                    "「停用子进程限制」只是关掉监控开关，并不提高 32 这个全局上限；HyperOS / ColorOS / " +
                    "Android 16+ 还常常直接忽略该开关。所以只开开关仍被杀属于正常现象。\n\n" +
                    "【同时做】设置 → 开发者选项 → 停用子进程限制 / Disable child process restrictions，" +
                    "并保持开发者选项总开关开启（关掉会自动复位）。\n" +
                    "没有电脑时，Android 11+ 可用「无线调试」配合另一台设备或终端类应用执行上述命令。"
            Build.VERSION.SDK_INT >= 32 ->
                "Android 12L / 13 请同时执行（可点下方按钮复制）：\n" +
                    "   adb shell settings put global settings_enable_monitor_phantom_procs false\n" +
                    "   adb shell device_config put activity_manager max_phantom_processes 2147483647\n" +
                    "   adb shell device_config set_sync_disabled_for_tests persistent\n" +
                    "开关值可能被 Google Play 服务的 device_config 同步覆盖，最后一条用于阻止覆盖。"
            else ->
                "Android 12 只能提高进程上限，用电脑 adb 执行：\n" +
                    "   adb shell device_config put activity_manager max_phantom_processes 2147483647\n" +
                    "   adb shell device_config set_sync_disabled_for_tests persistent"
        }
        return steps + "\n\n【应用侧已做的缓解】精简启动（诊断/统计服务改为按需）、进程预算护栏（超限时换出可选服务）、" +
            "关闭 USAP 预 fork 进程池、压低 guest 自身的缓存应用上限。\n" +
            "但核心服务与 HAL 有二十多个是固件启动必需的，软件侧删不掉。若宿主上限仍是 32，" +
            "这些手段只能缓解、无法根治 —— 提高上限才是根治办法。开机日志里的 " +
            "guestProcesses=N/32 可以直接确认瓶颈。" +
            "\n\n另外请把 AndroPaw 的省电策略设为「无限制」，并在最近任务里锁定该应用。"
    }
}
