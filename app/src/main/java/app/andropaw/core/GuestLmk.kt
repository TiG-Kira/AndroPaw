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
/** [ramMb] > 0: guest RAM budget; background apps are killed when the guest's total RSS exceeds it. */
class GuestLmk(private val root: File, private val lowRam: Boolean, private val log: (String) -> Unit, private val ramMb: Int = 0) {
    private val adj = ConcurrentHashMap<Int, Int>()
    @Volatile private var running = false
    @Volatile var armed = false
    private var server: LocalServerSocket? = null
    private var bound: LocalSocket? = null
    private val fifo = File(root, "dev/aemu_oom")
    @Volatile var kills = 0
    /** boot 完成后才允许杀进程（由 GuestVm 置位） */
        private set

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
        adj[pid] = if (p[1] == "s") scoreToAdj(v) else v
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
                    1 -> if (n >= 16) { val pid = bb.int; bb.int; adj[pid] = lmkdAdj(bb.int) }
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
        runCatching { String(File("/proc/$pid/cmdline").readBytes(), Charsets.ISO_8859_1).trim().trimEnd(0.toChar()) }.getOrNull()

    /**
     * 可牺牲的只有"普通应用"：cmdline 是包名（含点）或 pkg:process 形式，且不在核心名单。
     * cmdline 为空（native 守护）、核心名单命中、宿主 helper（app.andropaw 前缀）一律不杀。
     */
    private fun isSacrificial(pid: Int): Boolean {
        val cmd = cmdlineOf(pid) ?: return false
        if (cmd.isEmpty()) return false
        val base = cmd.substringBefore(':')
        if (cmd in NEVER_KILL || base in NEVER_KILL) return false
        if (!cmd.contains('.')) return false
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

    private fun loop() {
        // RSS процессов гостя не складываем: страницы зиготы общие и считались бы в каждом процессе.
        // Мерило — свободная память телефона; с lowRam держим запас больше и фона меньше.
        val minHost = if (lowRam) 2500L else MIN_HOST_MB
        val maxCached = if (ramMb > 0) (ramMb / 128).coerceIn(2, MAX_CACHED) else if (lowRam) 4 else MAX_CACHED
        while (running) {
            Thread.sleep(2000)
            adj.keys.removeAll { !File("/proc/$it").exists() }
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
            runCatching { Os.kill(victim, OsConstants.SIGKILL) }
            adj.remove(victim)
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
    }
}
