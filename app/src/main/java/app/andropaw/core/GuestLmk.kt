package app.andropaw.core

import android.net.LocalServerSocket
import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.system.Os
import android.system.OsConstants
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Low memory killer вместо ядерного. Без него фоновые процессы гостя не умирают никогда: на Xiaomi 15
 * у S3 4.3 висело 35 app_process по 75–300 МБ, телефон уходил в своп, а `pm` отвечал по две минуты.
 *
 * Важность процессов (oom_adj, шкала −17…15) сообщает ActivityManager гостя:
 *  - до 4.3 — записью в /proc/<pid>/oom_adj; хост её не пускает (SELinux), прослойка гостя
 *    (native/guestshim) перенаправляет запись в FIFO /dev/aemu_oom строкой «\n<pid> a <adj>»;
 *  - 4.4+ — пакетами в сокет /dev/socket/lmkd (int big-endian: 1 = PROCPRIO pid uid adj, 2 = PROCREMOVE pid).
 * Когда памяти мало, убиваем самый неважный и самый толстый процесс, как это делал бы lowmemorykiller.
 */
/**
 * [ramMb] > 0: guest RAM budget; background apps are killed when the guest's total RSS exceeds it.
 *
 * Besides memory, the host puts a hard ceiling on how many child processes an app may fork:
 * Android 12+'s "Phantom Process Killer" SIGKILLs the tree once the count passes 32, whatever
 * the free RAM is. A 5.0+ (N64) guest runs one host process per service and one per app, so on
 * a 12 GB phone with 5 GB free it can hit that ceiling during boot while the memory-based
 * reaper below never fires (it only looks at MemAvailable). [guardPhantomProcesses] is
 * therefore deliberately independent of [armed]: it runs during boot too, and only ever
 * reclaims ordinary cached apps, never a core service.
 */
class GuestLmk(
    private val root: File,
    private val lowRam: Boolean,
    private val log: (String) -> Unit,
    private val ramMb: Int = 0,
    /** AEmulator Pro: budget preset chosen on the settings page, applied when the machine boots. */
    private val warnLimit: Int = PHANTOM_WARN_LIMIT,
    private val safeLimit: Int = PHANTOM_SAFE_LIMIT,
) {
    private val adj = ConcurrentHashMap<Int, Int>()
    @Volatile private var running = false
    @Volatile var armed = false
    private var server: LocalServerSocket? = null
    private var bound: LocalSocket? = null
    private val fifo = File(root, "dev/aemu_oom")
    @Volatile var kills = 0
        private set
    /** Guest processes seen by the last scan — read by the zygote-exit diagnostics. */
    @Volatile var processCount = 0
        private set
    private var lastPhantomKillAt = 0L
    /** pids parked with SIGSTOP until the guest brings them forward or the budget needs them out */
    private val frozen = ConcurrentHashMap<Int, Long>()

    /**
     * Pids of *optional* fixed guest services (the boot plan marks them optional; framework roots
     * and HALs are never included). The old guard only reclaimed zygote-forked cached apps, so on
     * an N64 boot where ~40 native services are alive before the first app exists, its candidate
     * list was empty and all three budget presets were no-ops. Optional services are the second
     * tier: killing them lowers the host cgroup count for real, and the guest already has a
     * ctl.start path that brings one back when something asks for it.
     */
    @Volatile var swappablePids: () -> Set<Int> = { emptySet() }
    /** Notifies GuestVm that we killed one of its optional services, so the watchdog does not mistake the batch of 137s for a host cull. */
    @Volatile var onSwappedOut: (Int) -> Unit = {}

    fun start() {
        running = true
        runCatching {
            fifo.delete(); fifo.parentFile?.mkdirs()
            Os.mkfifo(fifo.absolutePath, "666".toInt(8))
            Os.chmod(fifo.absolutePath, "666".toInt(8))
            Thread({ readFifo() }, "lmk-fifo").apply { isDaemon = true; start() }
        }.onFailure { log("lmk: FIFO not created: $it") }
        runCatching { serveLmkd() }.onFailure { log("lmk: lmkd socket failed: $it") }
        Thread({ loop() }, "lmk").apply { isDaemon = true; start() }
    }

    fun stop() {
        running = false
        runCatching { server?.close() }; runCatching { bound?.close() }
        // разбудить читателя FIFO
        runCatching { File(fifo.absolutePath).appendText("\n") }
        // never leave a stopped process behind when the VM goes away
        frozen.keys.forEach { runCatching { Os.kill(it, OsConstants.SIGCONT) } }
        frozen.clear()
        adj.clear()
    }

    private fun readFifo() {
        // O_RDWR: открытие не ждёт писателя и чтение не получает EOF, когда писатели уходят
        val fd = Os.open(fifo.absolutePath, OsConstants.O_RDWR or OsConstants.O_CLOEXEC, 0)
        val buf = ByteArray(4096)
        val line = StringBuilder()
        FileInputStream(fd).use { input ->
            while (running) {
                val n = try { input.read(buf) } catch (_: Exception) { break }
                if (n <= 0) break
                for (i in 0 until n) {
                    val c = buf[i].toInt().toChar()
                    if (c == '\n') { parseLine(line.toString()); line.setLength(0) } else if (line.length < 64) line.append(c)
                }
                // значение приходит отдельным write() без «\n»: строку закрывает «\n» следующей записи
            }
        }
    }

    private fun parseLine(s: String) {
        val p = s.trim().split(' ').filter { it.isNotEmpty() }
        if (p.size < 3) return
        val pid = p[0].toIntOrNull() ?: return
        val v = p[2].toIntOrNull() ?: return
        noteAdj(pid, if (p[1] == "s") scoreToAdj(v) else v)
    }

    /**
     * 7.0+ sends oom_score_adj (−1000…999, cached from 900), 4.4–6.0 the old −17…15 scale (cached from 9).
     * Taking a raw 700 ("previous app") for a cached process killed services and visible apps every few seconds.
     */
    private fun lmkdAdj(v: Int): Int = when {
        v in -17..16 -> v
        v < 0 -> -17
        v >= 900 -> CACHED + (v - 900) / 12
        else -> CACHED - 1
    }

    private fun scoreToAdj(score: Int) = if (score >= 1000) 15 else score * 17 / 1000

    /** The framework roots are counted, but never frozen or swapped out: losing zygote means a boot loop. */
    private val FRAMEWORK_ROOTS = setOf(
        "zygote", "zygote64", "zygote_secondary", "system_server", "surfaceflinger",
        "servicemanager", "hwservicemanager", "vndservicemanager", "init",
        "app_process", "app_process32", "app_process64",
    )

    /**
     * The guest's ActivityManager reports a new importance on every state change. A parked pid
     * whose adj just dropped is being brought back to the foreground — swap it in immediately,
     * before anything tries to talk to a stopped process.
     */
    private fun noteAdj(pid: Int, value: Int) {
        adj[pid] = value
        if (value < CACHED && frozen.containsKey(pid)) {
            runCatching { Os.kill(pid, OsConstants.SIGCONT) }
            frozen.remove(pid)
            log("phantom guard: swapped pid $pid back in (adj $value)")
        }
    }

    private fun freeze(pid: Int): Boolean {
        if (frozen.containsKey(pid)) return false
        if (!runCatching { Os.kill(pid, OsConstants.SIGSTOP) }.isSuccess) return false
        frozen[pid] = System.currentTimeMillis()
        return true
    }

    /** Wake a parked pid; [swapOut] ends it instead — only that actually lowers the host's count. */
    private fun thaw(pid: Int, swapOut: Boolean = false): Boolean {
        if (frozen.remove(pid) == null) return false
        runCatching { Os.kill(pid, OsConstants.SIGCONT) }
        if (!swapOut) return true
        if (!runCatching { Os.kill(pid, OsConstants.SIGKILL) }.isSuccess) return false
        adj.remove(pid)
        kills++
        return true
    }

    private fun serveLmkd() {
        val sock = File(root, "dev/socket/lmkd")
        sock.parentFile?.mkdirs(); sock.delete()
        val ls = LocalSocket(LocalSocket.SOCKET_SEQPACKET)
        ls.bind(LocalSocketAddress(sock.absolutePath, LocalSocketAddress.Namespace.FILESYSTEM))
        runCatching { Os.chmod(sock.absolutePath, "666".toInt(8)) }
        bound = ls
        val s = LocalServerSocket(ls.fileDescriptor)
        server = s
        Thread({
            while (running) {
                val c = try { s.accept() } catch (_: Exception) { break }
                Thread({ lmkdClient(c) }, "lmkd-client").apply { isDaemon = true; start() }
            }
        }, "lmkd-accept").apply { isDaemon = true; start() }
    }

    private fun lmkdClient(c: LocalSocket) {
        val buf = ByteArray(256)
        try {
            val input = c.inputStream
            while (running) {
                val n = input.read(buf)
                if (n <= 0) break
                val bb = java.nio.ByteBuffer.wrap(buf, 0, n)
                if (n < 8) continue
                when (bb.int) {
                    1 -> if (n >= 16) { val pid = bb.int; bb.int; noteAdj(pid, lmkdAdj(bb.int)) }
                    2 -> adj.remove(bb.int)
                }
            }
        } catch (_: Exception) {
        } finally { runCatching { c.close() } }
    }

    // ---------------------------------------------------------------- убийца

    private fun memAvailableMb(): Long = runCatching {
        File("/proc/meminfo").useLines { l -> l.first { it.startsWith("MemAvailable:") } }
            .split(Regex("\\s+"))[1].toLong() / 1024
    }.getOrDefault(Long.MAX_VALUE)

    /** guest 里绝不能被 LMK 碰的核心进程（一旦被杀就是成批 code 137） */
    private val NEVER_KILL = setOf(
        "zygote", "zygote64", "servicemanager", "surfaceflinger", "keystore", "keystore2",
        "netd", "mediaserver", "bootanim", "drmserver", "installd", "installall", "install-recovery",
        "vold", "logd", "lmkd", "binderd", "hwbinderd", "vndbinderd", "glserverd", "fmradio",
        "rild", "debuggerd", "ueventd", "init", "cameraserver", "hwservicemanager", "audioserver",
    )

    private fun cmdlineOf(pid: Int): String? =
        runCatching { String(File("/proc/$pid/cmdline").readBytes(), Charsets.ISO_8859_1).substringBefore(0.toChar()).trim() }.getOrNull()

    /**
     * 可牺牲的只有"普通应用"：cmdline 是包名（含点）或 pkg:process 形式，且不在核心名单。
     * cmdline 为空（native 守护）、核心名单命中、宿主 helper（app.andropaw 前缀）一律不杀。
     */
    private fun isSacrificial(pid: Int): Boolean {
        val cmd = cmdlineOf(pid) ?: return false
        if (cmd.isEmpty()) return false
        val base = cmd.substringBefore(':')
        if (cmd in NEVER_KILL || base in NEVER_KILL) return false
        if (!GuestProcesses.appCandidate(cmd)) return false
        if (cmd.startsWith("app.andropaw")) return false
        // 系统关键应用：SystemUI/OOBE/设置被杀会让系统"能进桌面但没状态栏"或开箱流程反复重启
        for (keep in listOf("com.android.systemui", "com.google.android.setupwizard",
                "com.android.provision", "com.android.settings", "com.android.phone",
                "com.android.launcher", "android.process.acore")) {
            if (cmd.startsWith(keep)) return false
        }
        return true
    }

    private fun rssMb(pid: Int): Long = runCatching {
        File("/proc/$pid/statm").readText().split(' ')[1].toLong() * 4 / 1024
    }.getOrDefault(-1)

    /**
     * How many processes of this image the host can see, plus the pids of ordinary cached apps
     * that may be reclaimed. Native services carry the image root in argv; zygote children
     * rewrite their cmdline to a package name, so those are recognised by AEMU64_ROOT instead.
     * The env marker keeps the host's own apps out of the count.
     */
    private fun scanGuestProcesses(): Pair<Int, List<Int>> {
        val rootPath = root.absolutePath
        val marker = "AEMU64_ROOT=$rootPath"
        val canonical = runCatching { "AEMU64_ROOT=${root.canonicalPath}" }.getOrDefault(marker)
        var total = 0
        val sacrificial = ArrayList<Int>()
        File("/proc").listFiles()?.forEach { d ->
            val pid = d.name.toIntOrNull() ?: return@forEach
            val raw = runCatching { String(File(d, "cmdline").readBytes(), Charsets.ISO_8859_1) }.getOrNull()
                ?: return@forEach
            val argv0 = raw.substringBefore(0.toChar()).trim()
            // binderd/hwbinderd/vndbinderd are spawned directly with their socket under run/,
            // so they carry neither the image root nor AEMU64_ROOT. They still count against the
            // host ceiling, and were previously invisible to the budget.
            val native = raw.contains(rootPath) || raw.contains("libaemu64") || raw.contains("libqemu") ||
                raw.contains("libbinderd")
            val ours = if (native) true else {
                GuestProcesses.appCandidate(argv0) && runCatching {
                    String(File(d, "environ").readBytes(), Charsets.ISO_8859_1)
                        .split(0.toChar()).any { it == marker || it == canonical }
                }.getOrDefault(false)
            }
            if (!ours) return@forEach
            total++
            // Never a candidate, even if a recycled pid makes its cmdline look odd.
            if (argv0 in FRAMEWORK_ROOTS) return@forEach
            if (!native && isSacrificial(pid)) sacrificial += pid
        }
        return total to sacrificial
    }

    /**
     * Two-stage child-process budget, independent of [armed] because a boot that forks past 32
     * processes is killed in one batch whatever the free RAM is.
     *
     *  - soft stage (WARN): park the least useful cached apps with SIGSTOP. A stopped process
     *    still counts in the uid cgroup, but it stops burning background CPU — the host kills
     *    phantom processes for excessive CPU too.
     *  - hard stage (SAFE): swap them out for real. The guest framework keeps saved state, so a
     *    killed cached app is re-forked by the still-alive zygote when the user returns to it —
     *    exactly the foreground/background behaviour of a real device.
     *
     * Framework roots are never candidates: the whole point is that trimming background apps
     * must never take zygote down with it.
     */
    private fun guardPhantomProcesses(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastPhantomKillAt < PHANTOM_GUARD_GAP_MS) return false
        val (total, appCandidates) = scanGuestProcesses()
        processCount = total
        val services = runCatching { swappablePids() }.getOrDefault(emptySet())
            .filter { it > 1 && File("/proc/$it").exists() }
        // pids that disappeared on their own are no longer ours to wake
        frozen.keys.removeAll { !File("/proc/$it").exists() }
        // expire parked pids: a free slot means swap in, still over budget means swap out
        var resumed = 0
        var swapped = 0
        for ((pid, at) in frozen.toList()) {
            if (now - at < FREEZE_MAX_MS) continue
            if (total <= warnLimit) { if (thaw(pid)) resumed++ }
            else if (thaw(pid, swapOut = true)) swapped++
        }
        if (total <= warnLimit) {
            if (resumed > 0) log("phantom guard: $total processes — swapped $resumed app(s) back in")
            return resumed > 0
        }
        // least important first: highest adj, then the newest fork
        val apps = appCandidates.filter { !frozen.containsKey(it) }
            .sortedWith(compareByDescending<Int> { adj[it] ?: CACHED }.thenByDescending { it })
        if (total > safeLimit) {
            // Apps are cheapest to lose (zygote re-forks them on demand); optional services are
            // the last resort. Swap enough in one round to actually get back under the line —
            // a boot with ~40 services needs far more than the old batch of three.
            val parked = frozen.keys.sortedByDescending { adj[it] ?: CACHED }
            val queue = (parked + apps + services).distinct()
            val need = (total - warnLimit).coerceIn(1, SWAP_OUT_BATCH_MAX)
            var swappedServices = 0
            for (pid in queue.take(need)) {
                val wasService = pid in services
                if (thaw(pid, swapOut = true)) swapped++
                else if (runCatching { Os.kill(pid, OsConstants.SIGKILL) }.isSuccess) {
                    adj.remove(pid); swapped++; kills++
                    if (wasService) { swappedServices++; runCatching { onSwappedOut(pid) } }
                }
            }
            if (swappedServices > 0) log("phantom guard: swapped out $swappedServices optional guest service(s) — they come back via ctl.start when needed")
        } else {
            var parked = 0
            for (pid in apps.take(FREEZE_BATCH)) if (freeze(pid)) parked++
            if (parked > 0) log("phantom guard: $total processes (host ceiling $PHANTOM_PROCESS_LIMIT) — parked $parked cached app(s)")
            return parked > 0
        }
        lastPhantomKillAt = now
        if (swapped > 0) log("phantom guard: $total processes (host ceiling $PHANTOM_PROCESS_LIMIT) — swapped out $swapped process(es); zygote kept")
        return swapped > 0
    }

    private fun loop() {
        // RSS процессов гостя не складываем: страницы зиготы общие и считались бы в каждом процессе.
        // Мерило — свободная память телефона; с lowRam держим запас больше и фона меньше.
        val minHost = if (lowRam) 2500L else MIN_HOST_MB
        val maxCached = if (ramMb > 0) (ramMb / 128).coerceIn(2, MAX_CACHED) else if (lowRam) 4 else MAX_CACHED
        while (running) {
            Thread.sleep(if (processCount > safeLimit) 250L else if (processCount > warnLimit) 500L else 2000L)
            if (!running) continue
            adj.keys.removeAll { !File("/proc/$it").exists() }
            // Child-process ceiling is a host policy, not a memory condition: guard it during
            // boot as well, when the framework forks most of its processes.
            if (guardPhantomProcesses()) continue
            if (!armed) continue
            val cached = adj.filter { it.value >= CACHED }.keys.toMutableSet()
            if (cached.isEmpty()) continue
            val avail = memAvailableMb()
            // 不再用累加 RSS 判断预算：共享 zygote 页会被重复计算，5.0+ 上必然判定超预算
            val overBudget = false   // 累加 RSS 会重复计算共享页，5.0+ 必然误判
            // The count and the budget only matter when the phone runs short: with plenty free, killing a cached
            // service just makes the activity manager start it again (MIUI keeps 11-14 of them: a kill every 5 s,
            // providers gone under their clients, the CPU busy with restarts)
            val tight = avail < minHost * 2
            if (avail >= minHost && !(tight && (cached.size > maxCached || overBudget))) continue
            val sizes = cached.associateWith { rssMb(it) }
            // сначала самый неважный, среди равных — самый большой
            var victim = cached.maxWithOrNull(compareBy<Int> { adj[it] ?: 0 }.thenBy { sizes[it] ?: 0 }) ?: continue
            // 验明正身：adj 表可能被污染（pid 复用/上报错乱），把核心服务当 cached 杀
            // 曾造成 keystore/servicemanager/zygote 成批 code 137。只杀真正的普通应用。
            var guard = 0
            while (guard++ < 8 && !isSacrificial(victim)) {
                log("memory: refusing to kill non-app pid $victim (${cmdlineOf(victim)?.take(60) ?: "no cmdline"})")
                adj.remove(victim)
                cached.remove(victim)
                victim = cached.maxWithOrNull(compareBy<Int> { adj[it] ?: 0 }.thenBy { sizes[it] ?: 0 }) ?: break
            }
            if (victim == 0 || !isSacrificial(victim)) continue
            val a = adj[victim]
            val killed = runCatching { Os.kill(victim, OsConstants.SIGKILL) }.isSuccess
            adj.remove(victim)
            if (!killed) { log("memory: SIGKILL failed for pid $victim"); continue }
            kills++
            log("memory: killed background process $victim (adj $a, ${sizes[victim]} MB; cached ${cached.size}, host free $avail MB)")
            Thread.sleep(3000)
        }
    }

    companion object {
        /** HIDDEN_APP_MIN_ADJ (≤4.3) = CACHED_APP_MIN_ADJ (4.4+) = 9 */
        const val CACHED = 9
        const val MAX_CACHED = 10
        const val MIN_HOST_MB = 1500L
        /** Android 12+ ActivityManagerConstants.MAX_PHANTOM_PROCESSES (shared by all apps). */
        const val PHANTOM_PROCESS_LIMIT = 32
        /** Soft stage: start parking idle apps here. */
        const val PHANTOM_WARN_LIMIT = 20
        /** Hard stage: swap apps out for real, leaving room for other apps' children. */
        const val PHANTOM_SAFE_LIMIT = 24
        const val PHANTOM_GUARD_GAP_MS = 600L
        /** A parked app is swapped back in or out after this long, never left stopped forever. */
        const val FREEZE_MAX_MS = 8_000L
        const val FREEZE_BATCH = 2
        /** A boot can be ~40 processes over budget; three per round never catches up before the host culls. */
        const val SWAP_OUT_BATCH_MAX = 8
    }
}
