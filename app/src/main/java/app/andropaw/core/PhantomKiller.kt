package app.andropaw.core

import android.content.Context
import android.os.Build
import app.andropaw.AppPrefs
import java.io.File

/**
 * 宿主 Android 12+ 的 Phantom Process Killer。
 *
 * 任何应用 fork 出的子进程总数一旦超过全局上限（默认 32），system_server 会按父进程的 oom_adj
 * 从差到好**整批** SIGKILL。日志里就是一片 `exited with code 137`，接着 surfaceflinger 崩、
 * guest init 进 `(1/12)` 重启 —— 与内存无关，机器再空也会发生。
 *
 * 两条容易混淆的东西：
 *  - 开发者选项「停用子进程限制」= `settings_enable_monitor_phantom_procs`，只是把监控关掉，
 *    **不提高 32 这个上限**；HyperOS / ColorOS 还经常直接忽略它。
 *  - 真正有效的是官方给 Termux 的那条：`device_config put activity_manager max_phantom_processes`
 *    设成 `2147483647`。它需要 shell 或 root，所以普通应用改不了 —— 这就是 Shizuku 的用处。
 *
 * 本类在模拟器启动前通过 [HostShell] 写入，模拟器**完全停止后**写回原值，用户的机器不会被永久改坏。
 *
 * 崩溃安全：快照和「已生效」标记在写宿主**之前**就 commit 落盘。进程被 LMK 打死、用户强杀、
 * 甚至写了一半失败，下次启动 [recoverStale] 都能把宿主还原回原样。
 */
object PhantomKiller {
    private const val PREFS = "phantom_killer"
    private const val K_ARMED = "armed"
    private const val K_COUNT = "count"
    private const val K_MON = "monitor"
    private const val K_MAX = "max_procs"
    private const val K_SYNC = "sync"
    private const val K_AT = "armed_at"

    /** 官方文档给的「无上限」。 */
    const val UNLIMITED = "2147483647"

    /** 读回校验的阈值：低于这个数说明没写进去（写入被 ROM 挡住时读回的还是 32）。 */
    private const val SANE_MIN = 4096L

    private const val KEY_MONITOR = "settings_enable_monitor_phantom_procs"
    private const val KEY_MAX = "max_phantom_processes"
    private const val NS = "activity_manager"

    enum class Arm {
        /** 已写入，宿主 phantom 上限提到 2^31-1 */
        PAUSED,

        /** 上一轮的快照还在（同一次运行里的开机重试 / 第二台机器），只加了计数 */
        ALREADY,

        /** 设置里没开这个开关 */
        OFF,

        /** 宿主 < Android 12，没有这个东西 */
        NOT_APPLICABLE,

        /** Shizuku 没装 / 没跑 / 没授权 */
        NO_SHIZUKU,

        /** 命令执行了但读回不对（被 ROM 挡住），已尽力还原 */
        FAILED,
    }

    private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun supported(): Boolean = Build.VERSION.SDK_INT >= 31

    fun isArmed(ctx: Context): Boolean = prefs(ctx).getBoolean(K_ARMED, false)

    fun armedAt(ctx: Context): Long = prefs(ctx).getLong(K_AT, 0L)

    /**
     * 模拟器开机前调用：把宿主 phantom 上限抬到最大。
     *
     * 幂等 —— 同一次运行里重复调用（开机失败重试、第二台机器）只加计数，不会重复写宿主。
     */
    @Synchronized
    fun arm(ctx: Context): Arm {
        if (!supported()) return Arm.NOT_APPLICABLE
        if (!AppPrefs.phantomPause(ctx)) return Arm.OFF
        val p = prefs(ctx)
        if (p.getBoolean(K_ARMED, false)) {
            p.edit().putInt(K_COUNT, p.getInt(K_COUNT, 1) + 1).commit()
            return Arm.ALREADY
        }
        if (!HostShell.usable(ctx)) return Arm.NO_SHIZUKU

        // 1. 抄原值。抄不到就不要动宿主 —— 否则还原时无从下手，等于永久改坏。
        val mon = HostShell.exec("settings get global $KEY_MONITOR") ?: return Arm.FAILED
        val mx = HostShell.exec("device_config get $NS $KEY_MAX") ?: return Arm.FAILED
        val sync = HostShell.exec("device_config get_sync_disabled_for_tests")

        // 2. 先记账再动宿主：中途被打断时 recoverStale 还知道该还原成什么。
        p.edit()
            .putBoolean(K_ARMED, true)
            .putInt(K_COUNT, 1)
            .putString(K_MON, mon.value)
            .putString(K_MAX, mx.value)
            .putString(K_SYNC, sync?.value)
            .putLong(K_AT, System.currentTimeMillis())
            .commit()

        // 3. 写。set_sync_disabled_for_tests persistent 挡住 Google Play 服务把 device_config 同步回去。
        HostShell.exec("settings put global $KEY_MONITOR false")
        HostShell.exec("device_config put $NS $KEY_MAX $UNLIMITED")
        HostShell.exec("device_config set_sync_disabled_for_tests persistent")

        // 4. 读回校验 —— 写没写进去只有这一步说了算。
        val check = HostShell.exec("device_config get $NS $KEY_MAX")?.value?.toLongOrNull()
        if (check == null || check < SANE_MIN) {
            restore(ctx)
            p.edit().clear().commit()
            return Arm.FAILED
        }
        return Arm.PAUSED
    }

    /**
     * 模拟器停止后调用：计数归零时把宿主写回原值。
     *
     * @return 是否真的还原了（false = 还有别的机器在跑，或者 Shizuku 已经不可用、留给 [recoverStale]）
     */
    @Synchronized
    fun disarm(ctx: Context): Boolean {
        val p = prefs(ctx)
        if (!p.getBoolean(K_ARMED, false)) return false
        val left = p.getInt(K_COUNT, 1) - 1
        if (left > 0) {
            p.edit().putInt(K_COUNT, left).commit()
            return false
        }
        // 还原失败就把标记留着（计数归 0），下次启动 recoverStale 再试一次。
        if (!restore(ctx)) {
            p.edit().putInt(K_COUNT, 0).commit()
            return false
        }
        p.edit().clear().commit()
        return true
    }

    /**
     * 崩溃兜底：上次留下「已生效」标记，而现在没有任何 guest 进程在跑 —— 说明退出路径没走到
     * （进程被 LMK 打死 / 用户强杀 / 写了一半失败），还原宿主。
     *
     * 只在**没有**模拟器运行时动手，避免把正在运行的机器重新推回 32 的刀口下。
     */
    @Synchronized
    fun recoverStale(ctx: Context): Boolean {
        val p = prefs(ctx)
        if (!p.getBoolean(K_ARMED, false)) return false
        if (liveGuestProcesses() > 0) return false
        if (!restore(ctx)) return false
        p.edit().clear().commit()
        return true
    }

    /** 还原宿主原值。原值缺失的键用 delete 还原成「未设置」，而不是写一个假值。 */
    private fun restore(ctx: Context): Boolean {
        if (!HostShell.usable(ctx)) return false
        val p = prefs(ctx)
        val mon = p.getString(K_MON, null)
        val mx = p.getString(K_MAX, null)
        val sync = p.getString(K_SYNC, null)

        var ok = HostShell.exec(
            if (mon == null) "settings delete global $KEY_MONITOR" else "settings put global $KEY_MONITOR $mon"
        ) != null
        ok = (HostShell.exec(
            if (mx == null) "device_config delete $NS $KEY_MAX" else "device_config put $NS $KEY_MAX $mx"
        ) != null) && ok
        HostShell.exec("device_config set_sync_disabled_for_tests ${sync ?: "none"}")
        return ok
    }

    /** 手动还原（设置页按钮 / 退出没用上的兜底），无视计数。 */
    @Synchronized
    fun forceRestore(ctx: Context): Boolean {
        if (!restore(ctx)) return false
        prefs(ctx).edit().clear().commit()
        return true
    }

    /** 宿主当前的真实取值，给设置页显示用。max 只有 Shizuku 可用时才读得到。 */
    fun hostMax(ctx: Context): String? = HostShell.exec("device_config get $NS $KEY_MAX", 4_000)?.value

    /**
     * 还有多少 guest 进程活着。主进程用它判断「上标记是否过期」：
     * `:vm` 里的 guest 子进程与主进程同 uid，/proc/<pid>/cmdline 读得到。
     */
    fun liveGuestProcesses(): Int {
        val mine = android.os.Process.myPid()
        var n = 0
        File("/proc").listFiles()?.forEach { d ->
            val pid = d.name.toIntOrNull() ?: return@forEach
            if (pid == mine) return@forEach
            val cmd = runCatching { String(File(d, "cmdline").readBytes(), Charsets.ISO_8859_1) }.getOrNull() ?: return@forEach
            if (cmd.contains("libaemu64") || cmd.contains("libbinderd64") || cmd.contains("libglserverd")) n++
        }
        return n
    }
}
