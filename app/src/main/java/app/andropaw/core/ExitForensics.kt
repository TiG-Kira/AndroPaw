package app.andropaw.core

import java.io.File

/**
 * Rolling pre-mortem sampler for the two seconds before a framework process dies.
 *
 * There is no reliable way for an ordinary app to learn *who* sent a SIGKILL: the siginfo
 * returned by waitid() for CLD_KILLED carries the dying child's pid in si_pid and SIGKILL in
 * si_status, never the sender. Catching the sender needs ptrace on the killer or kernel audit,
 * neither of which is available to a non-root app. So we collect the circumstantial evidence the
 * host actually lets us read, at 250 ms, and classify the kill from it:
 *
 *  - process count near the global phantom ceiling with plenty of free RAM -> Phantom Process Killer
 *  - very low MemAvailable                                                -> host LMK / kernel OOM
 *  - plannedRestart / requestedKill set                                   -> our own kill
 *
 * Binder queue depth is deliberately NOT faked here: it lives in the native binderd epoll loop
 * and needs a native counter to be meaningful. Everything in [Sample] is read from /proc or
 * from GuestLmk's own counters.
 */
class ExitForensics(private val probe: () -> Sample) {

    data class Sample(
        val at: Long,
        val guestProcesses: Int,
        val zygoteAdj: Int?,
        val zygoteRssMb: Long,
        val availMb: Long,
        val swapMb: Long,
        val lmkKills: Int,
        val frozen: Int,
        val bootDone: Boolean,
        val planned: Boolean,
    )

    private val samples = ArrayDeque<Sample>()
    @Volatile private var running = false
    @Volatile private var thread: Thread? = null

    fun start() {
        if (running) return
        running = true
        synchronized(samples) { samples.clear() }
        thread = Thread({
            while (running) {
                runCatching {
                    val s = probe()
                    synchronized(samples) {
                        samples.addLast(s)
                        while (samples.size > MAX_SAMPLES) samples.removeFirst()
                    }
                }
                Thread.sleep(SAMPLE_MS)
            }
        }, "vm-forensics").apply { isDaemon = true; start() }
    }

    fun stop() {
        running = false
        thread?.interrupt()
        thread = null
    }

    /** Human-readable pre-mortem plus a verdict; called from logZygoteExit(). */
    fun dump(name: String, code: Int): String {
        val now = System.currentTimeMillis()
        val window = synchronized(samples) { samples.toList() }
            .filter { now - it.at <= WINDOW_MS }
        val sb = StringBuilder()
        sb.append("zygote-forensics: $name code=$code, ${window.size} samples in the last ${WINDOW_MS / 1000}s (250ms apart)\n")
        for (s in window) {
            sb.append("  t=-%.2fs procs=%d/${GuestLmk.PHANTOM_PROCESS_LIMIT} zAdj=%s zRss=%dMB avail=%dMB swap=%dMB lmkKills=%d frozen=%d boot=%s planned=%s\n"
                .format((now - s.at) / 1000.0, s.guestProcesses, s.zygoteAdj?.toString() ?: "?",
                    s.zygoteRssMb, s.availMb, s.swapMb, s.lmkKills, s.frozen, s.bootDone, s.planned))
        }
        val peak = window.maxOfOrNull { it.guestProcesses } ?: 0
        val lowAvail = window.minOfOrNull { it.availMb } ?: Long.MAX_VALUE
        val verdict = when {
            window.isEmpty() -> "采样窗口内无数据（VM 刚启动或采样线程未就绪）"
            code != 137 -> "非 SIGKILL，不是宿主进程查杀（code=$code）"
            window.last().planned -> "这是 AndroPaw 自己发起的计划内重启，不是外部查杀"
            peak >= GuestLmk.PHANTOM_PROCESS_LIMIT - 2 && lowAvail > 1000 ->
                "峰值 $peak 个进程已逼近宿主全局上限 ${GuestLmk.PHANTOM_PROCESS_LIMIT}，且最低可用内存仍有 ${lowAvail}MB " +
                    "→ 宿主 Phantom Process Killer（按 uid cgroup 成批 SIGKILL），与 OOM 无关"
            lowAvail < 400 -> "可用内存最低 ${lowAvail}MB → 宿主 LMK / 内核 OOM killer"
            else -> "证据不足：进程峰值 $peak、最低可用内存 ${lowAvail}MB；需结合 guest logcat 判断"
        }
        sb.append("zygote-forensics verdict: $verdict")
        return sb.toString()
    }

    companion object {
        const val SAMPLE_MS = 250L
        const val WINDOW_MS = 3_000L
        const val MAX_SAMPLES = 48   // 12 s of history
    }
}
