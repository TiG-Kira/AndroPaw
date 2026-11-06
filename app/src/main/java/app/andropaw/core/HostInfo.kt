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

    /** Host-version specific steps, shown verbatim on the settings page. */
    fun phantomGuide(): String {
        val steps = when {
            Build.VERSION.SDK_INT >= 34 ->
                "1. 设置 → 系统（或「其他设置」）→ 开发者选项，打开「停用子进程限制 / Disable child process restrictions」。\n" +
                    "2. 必须保持「开发者选项」总开关开启：关掉它，这个开关会自动复位，限制重新生效。\n" +
                    "3. 部分 ColorOS / OriginOS / OneUI 会重置该项；若反复失效，用电脑 adb 执行：\n" +
                    "   adb shell settings put global settings_enable_monitor_phantom_procs false"
            Build.VERSION.SDK_INT >= 32 ->
                "Android 12L / 13 没有可用的系统开关，用电脑 adb 执行一次：\n" +
                    "   adb shell settings put global settings_enable_monitor_phantom_procs false\n" +
                    "该值可能被 Google Play 服务的 device_config 同步覆盖；若失效，补一条：\n" +
                    "   adb shell device_config set_sync_disabled_for_tests persistent"
            else ->
                "Android 12 只能提高进程上限，用电脑 adb 执行：\n" +
                    "   adb shell device_config put activity_manager max_phantom_processes 2147483647\n" +
                    "   adb shell device_config set_sync_disabled_for_tests persistent"
        }
        return steps + "\n\n注意：部分 HyperOS / ColorOS / Android 16+ 版本不遵守该开关（厂商自行实现进程限制），" +
            "已关闭仍被杀属正常现象；此时请保持设置页「宿主杀死进程时自动重启」开启，" +
            "让模拟器走一次干净的整机重启，而不是在残缺的进程树上反复重建。" +
            "\n\n另外请把 AndroPaw 的省电策略设为「无限制」，并在最近任务里锁定该应用。"
    }
}
