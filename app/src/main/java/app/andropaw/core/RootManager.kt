package app.andropaw.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * One app that asked this machine for root.
 *
 * The identity is the package name the request came from (the name the guest's process tree
 * carries). [uid] is the id the guest's own package list gives that package, for display.
 */
data class RootApp(
    val pkg: String,
    val uid: Int = 0,
    val granted: Boolean = false,
    val denied: Boolean = false,
    val requestedAt: Long = 0L,
    val lastCommand: String = "",
) {
    val pending: Boolean get() = !granted && !denied

    fun toJson(): JSONObject = JSONObject()
        .put("pkg", pkg).put("uid", uid).put("granted", granted).put("denied", denied)
        .put("requestedAt", requestedAt).put("lastCommand", lastCommand)

    companion object {
        fun from(o: JSONObject) = RootApp(
            pkg = o.optString("pkg"),
            uid = o.optInt("uid", 0),
            granted = o.optBoolean("granted", false),
            denied = o.optBoolean("denied", false),
            requestedAt = o.optLong("requestedAt", 0L),
            lastCommand = o.optString("lastCommand", ""),
        )
    }
}

/** The root switch plus every app that has asked for it, per machine. */
data class RootState(
    val enabled: Boolean = false,
    val apps: List<RootApp> = emptyList(),
) {
    val allowed: List<RootApp> get() = apps.filter { it.granted }
    val pendingCount: Int get() = apps.count { it.pending }
    val deniedCount: Int get() = apps.count { it.denied }

    fun toJson(): JSONObject = JSONObject().put("enabled", enabled).put("apps", JSONArray().apply {
        apps.forEach { put(it.toJson()) }
    })

    companion object {
        fun from(text: String?): RootState = runCatching {
            if (text.isNullOrBlank()) return@runCatching RootState()
            val o = JSONObject(text)
            val a = o.optJSONArray("apps") ?: JSONArray()
            RootState(
                enabled = o.optBoolean("enabled", false),
                apps = (0 until a.length()).mapNotNull { i -> a.optJSONObject(i)?.let(RootApp::from) },
            )
        }.getOrDefault(RootState())
    }
}

/**
 * Root for one machine: the switch, the privilege layer it turns on, and the apps that used it.
 *
 * This emulator runs the guest on the host's kernel and keeps the guest's file system in a
 * plain directory, so rooting a machine is neither a second boot image nor an exploit: it is
 * the emulator's own privilege layer, and the switch moves it.
 *
 * What the switch turns on, inside the guest:
 *  - every process init starts is root instead of a demoted one (services run as uid 0), and
 *    /system is writable — `AEMU_RO_SYSTEM` off, so `mount -o rw,remount /system` and an
 *    install into /system do what they say;
 *  - the properties a rooted, debuggable build carries (ro.secure=0, ro.debuggable=1,
 *    ro.build.tags=test-keys), so adbd comes up as root and a root check sees the device it
 *    is written for;
 *  - `su` on every path its generation looks at, answering through the host: whatever it
 *    starts reports uid 0 to everything inside the guest;
 *  - the generation's own pieces and daemon — Superuser for 2.x–3.x, SuperSU with
 *    `daemonsu` for 4.x–7.x, magiskd with /data/adb/magisk for 8.0–11, ksud with
 *    /data/adb/ksu for 12+ — so a module and a root check find what they look for.
 *
 * The switch is the policy: on means the machine is rooted, and an app's `su` is answered
 * "allow". The list below it is KernelSU's superuser page — the apps that used root, each
 * with a switch that takes it back from that one app.
 */
object RootManager {
    const val HOME = "/data/adb/andropaw"
    private const val SU = "$HOME/bin/su"
    private const val ROOTD = "$HOME/bin/rootd"
    private const val ASK = "$HOME/ask"
    private const val ANSWER = "$HOME/answer"
    private const val DENY = "$HOME/deny"
    private const val ENABLED = "$HOME/enabled"
    private const val BACKEND = "$HOME/backend"
    private const val OWNED = "$HOME/owned"

    fun backendOf(img: GuestImage): RootBackend = RootBackend.forApi(img.api)

    /** Where `su` goes for a machine of this generation: newest layout first, oldest last. */
    fun suPaths(api: Int): List<String> {
        val list = mutableListOf(SU, "/system/xbin/su", "/system/bin/su")
        if (api < 14) list += "/system/sbin/su"
        if (api >= 14) list += "/su/bin/su"            // SuperSU's systemless layout
        if (api >= 30) list += "/debug_ramdisk/su"     // Magisk on 11+
        list += "/sbin/su"                             // 8.x and older had /sbin on PATH
        return list
    }

    fun stateFile(ctx: Context, imageId: String): File = File(VmPaths(ctx, imageId).dir, "root.json")

    fun state(ctx: Context, imageId: String): RootState =
        RootState.from(runCatching { stateFile(ctx, imageId).readText() }.getOrNull())

    private fun save(ctx: Context, imageId: String, st: RootState) {
        runCatching {
            val f = stateFile(ctx, imageId)
            f.parentFile?.mkdirs()
            // two processes read this file (the machine's own and the page's): whole, then move
            val tmp = File(f.parentFile, "${f.name}.tmp")
            tmp.writeText(st.toJson().toString())
            if (!tmp.renameTo(f)) {
                f.writeText(st.toJson().toString())
                tmp.delete()
            }
        }
    }

    /**
     * The switch. On installs the whole layer, off takes it away again — `su` disappears,
     * /system goes back to read-only, the properties go back to the firmware's. The list of
     * apps is kept either way, so switching back on restores the same answers.
     */
    fun setEnabled(ctx: Context, img: GuestImage, on: Boolean): RootState {
        val st = state(ctx, img.id).copy(enabled = on)
        save(ctx, img.id, st)
        provision(ctx, img, st)
        return st
    }

    /** Answers one app: allow, deny, or forget (`null` puts it back to "asks again"). */
    fun decide(ctx: Context, img: GuestImage, pkg: String, allow: Boolean?): RootState {
        val st = state(ctx, img.id)
        val apps = st.apps.map { a ->
            if (a.pkg != pkg) a else when (allow) {
                true -> a.copy(granted = true, denied = false)
                false -> a.copy(granted = false, denied = true)
                null -> a.copy(granted = false, denied = false)
            }
        }
        val next = st.copy(apps = apps)
        save(ctx, img.id, next)
        // the marker a script (or a person with a shell) can read, so the guest's view of the
        // answer is the same as the page's
        val mark = GuestTree.file(VmPaths(ctx, img.id), "$DENY/$pkg")
        if (allow == false) {
            mark.parentFile?.mkdirs()
            runCatching { mark.writeText("1\n") }
        } else {
            mark.delete()
        }
        return next
    }

    fun forget(ctx: Context, img: GuestImage, pkg: String): RootState {
        val st = state(ctx, img.id)
        val next = st.copy(apps = st.apps.filterNot { it.pkg == pkg })
        save(ctx, img.id, next)
        GuestTree.delete(VmPaths(ctx, img.id), "$DENY/$pkg")
        return next
    }

    // ------------------------------------------------------------------ the gate's host half

    private data class Who(val pkg: String, val uid: Int)

    private val shells = setOf("sh", "ash", "mksh", "ksh", "dash", "su", "toybox", "busybox", "app_process")
    private val uids = ConcurrentHashMap<String, Int>()
    private val watched = ConcurrentHashMap<String, Pair<Context, GuestImage>>()

    @Volatile private var watcher: Thread? = null
    @Volatile private var lastSweep = 0L

    /**
     * Answers the `su` requests of a running machine.
     *
     * One thread for every machine is enough: it looks at the ask directories a few times a
     * second and does nothing at all while there is no request. Started from the boot path, so
     * a machine that is up always has someone behind its `su`.
     */
    fun watch(ctx: Context, img: GuestImage) {
        watched[img.id] = ctx to img
        if (watcher?.isAlive == true) return
        synchronized(this) {
            if (watcher?.isAlive == true) return
            watcher = Thread({
                while (!Thread.currentThread().isInterrupted) {
                    for ((_, pair) in watched) runCatching { pump(pair.first, pair.second) }
                    runCatching { Thread.sleep(60) }
                }
            }, "rootd-host").apply { isDaemon = true; start() }
        }
    }

    fun unwatch(imageId: String) {
        watched.remove(imageId)
    }

    /**
     * Answers every request waiting in the guest's ask directory.
     *
     * Safe to call from the page as well (and harmless while no machine is running): that is
     * how the list keeps moving when the page is in the app's own process and the machine in
     * another.
     */
    fun pump(ctx: Context, img: GuestImage): RootState {
        val paths = VmPaths(ctx, img.id)
        val answers = GuestTree.file(paths, ANSWER).apply { mkdirs() }
        val now = System.currentTimeMillis()
        // answers nobody collected (the asker gave up first): cleared now and then
        if (now - lastSweep > 60_000) {
            lastSweep = now
            answers.listFiles()?.forEach { if (now - it.lastModified() > 120_000) runCatching { it.delete() } }
        }
        val asks = GuestTree.file(paths, ASK).listFiles() ?: return state(ctx, img.id)
        if (asks.isEmpty()) return state(ctx, img.id)
        var st = state(ctx, img.id)
        var dirty = false
        for (f in asks) {
            val pid = f.name.toIntOrNull()
            if (pid == null) {
                f.delete()
                continue
            }
            val answer = File(answers, f.name)
            if (answer.exists()) {
                // the guest has not collected it yet; drop both if that process is long gone
                if (now - f.lastModified() > 60_000) {
                    f.delete()
                    answer.delete()
                }
                continue
            }
            // a machine that is not rooted has no `su`: refuse, and do not list the app
            if (!st.enabled) {
                runCatching { answer.writeText("deny\n") }
                f.delete()
                continue
            }
            val text = runCatching { f.readText() }.getOrDefault("")
            val who = whoOf(paths, img.id, pid, text) ?: run { f.delete(); continue }
            val cmd = commandOf(text)
            val apps = st.apps.toMutableList()
            val i = apps.indexOfFirst { it.pkg == who.pkg }
            val allow = if (i >= 0) !apps[i].denied else true
            if (i >= 0) {
                val a = apps[i]
                apps[i] = a.copy(
                    uid = if (who.uid > 0) who.uid else a.uid,
                    granted = allow, denied = !allow,
                    requestedAt = now,
                    lastCommand = cmd.ifBlank { a.lastCommand },
                )
            } else {
                apps += RootApp(who.pkg, who.uid, granted = allow, denied = !allow, requestedAt = now, lastCommand = cmd)
            }
            st = st.copy(apps = apps.sortedWith(order))
            dirty = true
            runCatching { answer.writeText(if (allow) "allow\n" else "deny\n") }
            f.delete()
        }
        if (dirty) save(ctx, img.id, st)
        return st
    }

    private val order = compareByDescending<RootApp> { it.pending }.thenByDescending { it.requestedAt }

    /**
     * The package behind a request.
     *
     * The guest says who it is: its own `su` walks up /proc — where the engine fakes the
     * process name an app sets — and writes the result as the ask file's first line. The host
     * cannot do that walk as well, because from outside the guest the same process still
     * carries the translator's command line; it only checks that the name looks like a package
     * and falls back to the process tree for anything else.
     */
    private fun whoOf(paths: VmPaths, imageId: String, pid: Int, text: String): Who? {
        val line = text.substringBefore('\n').removePrefix("who=")
        val cleaned = line
            .filter { it.isLetterOrDigit() || it == '.' || it == '_' || it == ':' || it == '-' }
            .substringBefore(':')
        if (cleaned.length >= 3 && cleaned.contains('.')) return Who(cleaned, uidOf(paths, imageId, cleaned))
        return callerOf(paths, imageId, pid)
    }

    /** Everything after "cmd=" — a command may hold newlines of its own. */
    private fun commandOf(text: String): String {
        val at = text.indexOf("cmd=")
        return if (at < 0) "" else text.substring(at + 4).trim()
    }

    /**
     * The package behind a request.
     *
     * The request comes from a `su` script, whose parent is a shell and whose grandparent is
     * the process that ran it — an app, usually, since apps go through
     * `Runtime.exec("su -c …")`. The walk climbs past the shells and takes the first real
     * process name, which for an app process is its package (the framework sets argv[0] to it).
     */
    private fun callerOf(paths: VmPaths, imageId: String, pid: Int): Who? {
        var p = pid
        var name: String? = null
        for (i in 0 until 6) {
            val argv0 = arg0(p) ?: return null
            val base = argv0.substringAfterLast('/')
            if (base.isEmpty() || base in shells) {
                p = parentOf(p) ?: return null
                continue
            }
            name = base
            break
        }
        if (name.isNullOrBlank()) return null
        // a secondary process of an app (com.foo:push) is the same app to the user
        val pkg = name.substringBefore(':')
        if (pkg.length < 3 || !pkg.contains('.')) return null
        return Who(pkg, uidOf(paths, imageId, pkg))
    }

    private fun arg0(pid: Int): String? {
        val raw = runCatching { File("/proc/$pid/cmdline").readBytes() }.getOrNull()
        if (raw != null) {
            val end = raw.indexOf(0)
            val text = when {
                end > 0 -> String(raw, 0, end)
                end == 0 -> ""
                else -> String(raw)
            }
            if (text.isNotEmpty()) return text
        }
        return runCatching { File("/proc/$pid/comm").readText().trim() }.getOrNull()
    }

    /** `/proc/<pid>/stat`: "pid (comm) state ppid …" — comm may hold spaces and brackets. */
    private fun parentOf(pid: Int): Int? = runCatching {
        val s = File("/proc/$pid/stat").readText()
        val i = s.lastIndexOf(')')
        if (i < 0) return@runCatching null
        s.substring(i + 2).split(' ').getOrNull(1)?.toIntOrNull()
    }.getOrNull()

    /** The id the guest's own package list gives a package (0 when it is not there yet). */
    private fun uidOf(paths: VmPaths, imageId: String, pkg: String): Int {
        val key = "$imageId/$pkg"
        uids[key]?.let { return it }
        var uid = runCatching {
            val list = GuestTree.file(paths, "data/system/packages.list")
            if (!list.isFile) return@runCatching 0
            list.useLines { lines -> lines.firstOrNull { it.startsWith("$pkg ") } }
                ?.split(' ')?.getOrNull(1)?.toIntOrNull() ?: 0
        }.getOrDefault(0)
        if (uid == 0) uid = runCatching {
            val xml = GuestTree.file(paths, "data/system/packages.xml")
            if (!xml.isFile) return@runCatching 0
            val text = xml.readText()
            val at = text.indexOf("<package name=\"$pkg\"")
            if (at < 0) return@runCatching 0
            Regex("userId=\"(\\d+)\"").find(text.substring(at, minOf(text.length, at + 800)))
                ?.groupValues?.get(1)?.toIntOrNull() ?: 0
        }.getOrDefault(0)
        if (uid > 0) uids[key] = uid
        return uid
    }

    // ------------------------------------------------------------------ what the switch writes

    /**
     * Writes (or removes) the layer for the current state.
     *
     * Called when the switch moves and on every boot: the firmware tree can be repaired or
     * replaced in between (a ROM import, a data reset), and a machine whose switch says "on"
     * must still have a `su` and a writable /system afterwards.
     */
    fun provision(ctx: Context, img: GuestImage, st: RootState = state(ctx, img.id)) {
        val paths = VmPaths(ctx, img.id)
        if (!File(paths.root, "system").isDirectory) return

        // the compatibility tools are always there: the plug-in page reports the backend it
        // found, and a module's own scripts call `magisk` / `ksud` / `xposed`
        tools(ctx, paths)

        if (!st.enabled) {
            removeGate(paths, img.api)
            return
        }

        val su = GuestTree.script(ctx, app.andropaw.R.raw.apw_su)
        for (path in suPaths(img.api)) {
            val f = GuestTree.file(paths, path)
            // only where this generation would really have it (its own tree has the directory)
            if (path.startsWith(HOME) || f.parentFile?.isDirectory == true) {
                GuestTree.writeBytes(paths, path, su, executable = true)
            }
        }
        GuestTree.writeText(paths, ENABLED, "1\n")
        GuestTree.writeText(paths, BACKEND, backendOf(img).key + "\n")
        listOf(ASK, ANSWER, DENY).forEach { GuestTree.file(paths, it).mkdirs() }
        // requests from an earlier run of this machine are stale: nobody is waiting for them
        GuestTree.file(paths, ASK).listFiles()?.forEach { runCatching { it.delete() } }
        GuestTree.file(paths, ANSWER).listFiles()?.forEach { runCatching { it.delete() } }
        guestFiles(ctx, paths, img)
    }

    /** The generation's own pieces: the daemon, its policy tool, the boot hook it is expected to have. */
    private fun guestFiles(ctx: Context, paths: VmPaths, img: GuestImage) {
        val rootd = GuestTree.script(ctx, app.andropaw.R.raw.apw_rootd)
        when (backendOf(img)) {
            // 2.x–3.x: a `su` in /system/xbin and ro.secure=0 was the whole of a root
            RootBackend.SUPERUSER -> putNew(paths, "/system/xbin/superuser", rootd)
            RootBackend.SUPERSU -> {
                putNew(paths, "/system/xbin/daemonsu", rootd)
                putNew(paths, "/system/xbin/supolicy", SUPOLICY)
                putNew(paths, "/system/etc/install-recovery.sh", INSTALL_RECOVERY)
            }
            RootBackend.MAGISK -> {
                putNew(paths, "/data/adb/magisk/magiskd", rootd)
                putNew(paths, "/data/adb/magisk/magiskpolicy", SUPOLICY)
                // 8.0–10 look in /sbin, 11+ in /debug_ramdisk
                if (File(paths.root, "sbin").isDirectory) putNew(paths, "/sbin/magiskd", rootd)
                if (img.api >= 30) putNew(paths, "/debug_ramdisk/magisk", GuestTree.script(ctx, app.andropaw.R.raw.apw_magisk))
            }
            // 12+: /data/adb/ksu is where modules live, `ksud daemon` the resident half (see apw_ksud.sh)
            RootBackend.KERNELSU -> {
                GuestTree.file(paths, "/data/adb/ksu/modules").mkdirs()
                putNew(paths, "/data/adb/ksu/ksu.conf", "{\"version\":\"1.0.0\"}\n".toByteArray())
            }
        }
    }

    /** A file the firmware did not have: never overwrite one it did. */
    private fun putNew(paths: VmPaths, guestPath: String, bytes: ByteArray) {
        val f = GuestTree.file(paths, guestPath)
        if (f.exists()) return
        GuestTree.writeBytes(paths, guestPath, bytes, executable = true)
        runCatching {
            val m = GuestTree.file(paths, ownedOf(guestPath))
            m.parentFile?.mkdirs()
            m.writeText(guestPath)
        }
    }

    private fun ownedOf(guestPath: String): String = "$OWNED/" + guestPath.trimStart('/').replace('/', '#')

    private fun dropOwned(paths: VmPaths, guestPath: String) {
        val mark = GuestTree.file(paths, ownedOf(guestPath))
        if (!mark.isFile) return
        GuestTree.delete(paths, guestPath)
        mark.delete()
    }

    private fun removeGate(paths: VmPaths, api: Int) {
        // `su` goes whatever was there before: the switch says this machine is not rooted
        suPaths(api).forEach { GuestTree.delete(paths, it) }
        GuestTree.delete(paths, ENABLED)
        GuestTree.delete(paths, BACKEND)
        GuestTree.delete(paths, "$HOME/rootd.pid")
        listOf(
            "/system/xbin/superuser", "/system/xbin/daemonsu", "/system/xbin/supolicy",
            "/system/etc/install-recovery.sh", "/data/adb/magisk/magiskd", "/data/adb/magisk/magiskpolicy",
            "/sbin/magiskd", "/debug_ramdisk/magisk", "/data/adb/ksu/ksu.conf",
        ).forEach { dropOwned(paths, it) }
        GuestTree.file(paths, ASK).listFiles()?.forEach { runCatching { it.delete() } }
        GuestTree.file(paths, ANSWER).listFiles()?.forEach { runCatching { it.delete() } }
        GuestTree.file(paths, DENY).listFiles()?.forEach { runCatching { it.delete() } }
    }

    private fun tools(ctx: Context, paths: VmPaths) {
        val ksud = GuestTree.script(ctx, app.andropaw.R.raw.apw_ksud)
        val magisk = GuestTree.script(ctx, app.andropaw.R.raw.apw_magisk)
        GuestTree.writeBytes(paths, "$HOME/bin/ksud", ksud, executable = true)
        GuestTree.writeBytes(paths, "$HOME/bin/ksu", ksud, executable = true)
        GuestTree.writeBytes(paths, "$HOME/bin/magisk", magisk, executable = true)
        GuestTree.writeBytes(paths, "$HOME/bin/xposed", GuestTree.script(ctx, app.andropaw.R.raw.apw_xposed), executable = true)
        GuestTree.writeBytes(paths, "$HOME/bin/boot", GuestTree.script(ctx, app.andropaw.R.raw.apw_boot), executable = true)
        GuestTree.writeBytes(paths, ROOTD, GuestTree.script(ctx, app.andropaw.R.raw.apw_rootd), executable = true)
        // the backends also look for their tools where they would really be
        GuestTree.writeBytes(paths, "/data/adb/ksu/bin/ksud", ksud, executable = true)
        GuestTree.writeBytes(paths, "/data/adb/ksu/bin/ksu", ksud, executable = true)
        GuestTree.writeBytes(paths, "/data/adb/magisk/magisk", magisk, executable = true)
    }

    /**
     * The properties the root layer sets.
     *
     * A rooted device is a device that booted with `ro.secure=0` and `ro.debuggable=1`: adbd
     * comes up as root, `adb root` works, and the checks a root-aware app runs see the device
     * they are written for. Merged into the property area at boot, so they are what the guest
     * reads from its first process onwards (run/props.extra still wins, as for every property).
     */
    fun props(ctx: Context, imageId: String): Map<String, String> {
        if (!state(ctx, imageId).enabled) return emptyMap()
        val m = LinkedHashMap<String, String>()
        m["ro.secure"] = "0"
        m["ro.adb.secure"] = "0"
        m["ro.debuggable"] = "1"
        m["ro.build.tags"] = "test-keys"
        m["service.adb.root"] = "1"
        return m
    }

    /** True when the machine currently has a usable gate (the switch is on and `su` is written). */
    fun active(ctx: Context, imageId: String): Boolean {
        val paths = VmPaths(ctx, imageId)
        return GuestTree.exists(paths, ENABLED) && GuestTree.exists(paths, "/system/xbin/su")
    }

    /** SuperSU's supolicy: there is no SELinux policy here to patch, so it takes its arguments and succeeds. */
    private val SUPOLICY: ByteArray = """
        #!/system/bin/sh
        # SuperSU's supolicy patches the SELinux policy of the running kernel. This emulator's
        # guest shares the host's kernel and loads no policy of its own, so there is nothing to
        # patch: the tool accepts what it is given and succeeds, which is what a module expects.
        exit 0
    """.trimIndent().plus("\n").toByteArray()

    /** SuperSU's boot hook. AndroPaw starts the same daemon from its own boot script. */
    private val INSTALL_RECOVERY: ByteArray = """
        #!/system/bin/sh
        # SuperSU starts daemonsu from here on 4.x-7.x. AndroPaw starts the same daemon from
        # /data/adb/andropaw/bin/boot once the framework is up, so nothing is left to do here.
        exit 0
    """.trimIndent().plus("\n").toByteArray()
}
