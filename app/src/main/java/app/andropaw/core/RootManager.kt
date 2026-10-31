package app.andropaw.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * One app that asked this machine for root.
 *
 * The identity is the package name the request came from (the name the guest's process tree
 * carries), plus the id the request reported: on an arm64 guest every app has its own, on the
 * 32-bit engine they can share one, which is why the package is what the policy is keyed on.
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

/** The root switch plus everything that asked for it, per machine. */
data class RootState(
    val enabled: Boolean = false,
    val apps: List<RootApp> = emptyList(),
) {
    val allowed: List<RootApp> get() = apps.filter { it.granted }
    val pendingCount: Int get() = apps.count { it.pending }

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
 * Root for one machine: a switch, a policy, and the requests that asked for it.
 *
 * Root in this emulator is a gate rather than a uid change — the guest runs on the host's
 * kernel, so an app that is "allowed" is one whose `su` calls are answered. The gate is
 * /data/adb/andropaw/bin/su (a shell script), the policy is a plain text file it greps in the
 * guest, and the requests are lines it appends; the page and the in-machine window both read
 * the same two files, so they always agree.
 */
object RootManager {
    const val HOME = "/data/adb/andropaw"
    private const val SU = "$HOME/bin/su"
    private const val POLICY = "$HOME/policy"
    private const val REQUESTS = "$HOME/requests.log"
    private const val ENABLED = "$HOME/enabled"

    /** Where the guest finds `su` on its PATH, whichever generation it is. */
    private val SU_LINKS = listOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/data/adb/ksu/bin/su")

    fun stateFile(ctx: Context, imageId: String): File = File(VmPaths(ctx, imageId).dir, "root.json")

    fun state(ctx: Context, imageId: String): RootState =
        RootState.from(runCatching { stateFile(ctx, imageId).readText() }.getOrNull())

    private fun save(ctx: Context, imageId: String, st: RootState) {
        runCatching {
            val f = stateFile(ctx, imageId)
            f.parentFile?.mkdirs()
            f.writeText(st.toJson().toString())
        }
    }

    /**
     * The switch. Turning it on installs the gate and writes the policy; turning it off takes
     * `su` away again (the app list is kept, so switching back on restores the same answers).
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
        syncPolicy(ctx, img, next)
        return next
    }

    fun forget(ctx: Context, img: GuestImage, pkg: String): RootState {
        val st = state(ctx, img.id)
        val next = st.copy(apps = st.apps.filterNot { it.pkg == pkg })
        save(ctx, img.id, next)
        syncPolicy(ctx, img, next)
        return next
    }

    /**
     * Reads what the gate recorded since the last look and folds it into the state.
     *
     * Cheap enough to call from a polling loop: one small file, and it is truncated the moment
     * its lines have been merged, so a request is listed once.
     */
    fun collectRequests(ctx: Context, img: GuestImage): RootState {
        val paths = VmPaths(ctx, img.id)
        val f = GuestTree.file(paths, REQUESTS)
        if (!f.isFile) return state(ctx, img.id)
        val text = runCatching { f.readText() }.getOrNull().orEmpty()
        if (text.isBlank()) return state(ctx, img.id)
        runCatching { f.writeText("") }

        val st = state(ctx, img.id)
        val apps = st.apps.toMutableList()
        text.lineSequence().forEach { line ->
            val parts = line.trim().split(' ', limit = 4)
            if (parts.size < 2) return@forEach
            val at = parts[0].toLongOrNull() ?: 0L
            val pkg = parts[1]
            val uid = parts.getOrNull(2)?.toIntOrNull() ?: 0
            val cmd = parts.getOrNull(3).orEmpty()
            val i = apps.indexOfFirst { it.pkg == pkg }
            if (i >= 0) apps[i] = apps[i].copy(
                uid = if (uid > 0) uid else apps[i].uid,
                requestedAt = maxOf(at, apps[i].requestedAt),
                lastCommand = cmd.ifBlank { apps[i].lastCommand },
            ) else apps += RootApp(pkg = pkg, uid = uid, requestedAt = at, lastCommand = cmd)
        }
        val sorted = apps.sortedWith(compareByDescending<RootApp> { it.pending }.thenByDescending { it.requestedAt })
        val next = st.copy(apps = sorted)
        save(ctx, img.id, next)
        syncPolicy(ctx, img, next)
        return next
    }

    /**
     * Writes the gate, the compatibility tools and the policy into the guest tree.
     *
     * Called when the switch moves and on every boot: the firmware tree can be repaired or
     * replaced in between (a ROM import, a data reset), and a machine whose switch says "on"
     * must still have a `su` afterwards.
     */
    fun provision(ctx: Context, img: GuestImage, st: RootState = state(ctx, img.id)) {
        val paths = VmPaths(ctx, img.id)
        if (!File(paths.root, "system").isDirectory) return

        // The compatibility tools are always there: the plug-in page reports the backend it
        // found, and a module's own scripts call `magisk` / `ksud` / `xposed`.
        GuestTree.writeBytes(paths, "$HOME/bin/ksud", GuestTree.script(ctx, app.andropaw.R.raw.apw_ksud), executable = true)
        GuestTree.writeBytes(paths, "$HOME/bin/ksu", GuestTree.script(ctx, app.andropaw.R.raw.apw_ksud), executable = true)
        GuestTree.writeBytes(paths, "$HOME/bin/magisk", GuestTree.script(ctx, app.andropaw.R.raw.apw_magisk), executable = true)
        GuestTree.writeBytes(paths, "$HOME/bin/xposed", GuestTree.script(ctx, app.andropaw.R.raw.apw_xposed), executable = true)
        GuestTree.writeBytes(paths, "$HOME/bin/boot", GuestTree.script(ctx, app.andropaw.R.raw.apw_boot), executable = true)
        // the backends also look for their tools where they would really be
        GuestTree.writeBytes(paths, "/data/adb/ksu/bin/ksud", GuestTree.script(ctx, app.andropaw.R.raw.apw_ksud), executable = true)
        GuestTree.writeBytes(paths, "/data/adb/ksu/bin/ksu", GuestTree.script(ctx, app.andropaw.R.raw.apw_ksud), executable = true)
        GuestTree.writeBytes(paths, "/data/adb/magisk/magisk", GuestTree.script(ctx, app.andropaw.R.raw.apw_magisk), executable = true)

        if (st.enabled) {
            val su = GuestTree.script(ctx, app.andropaw.R.raw.apw_su)
            GuestTree.writeBytes(paths, SU, su, executable = true)
            SU_LINKS.forEach { link ->
                val parent = GuestTree.file(paths, link).parentFile
                if (parent?.isDirectory == true) GuestTree.writeBytes(paths, link, su, executable = true)
            }
            GuestTree.writeText(paths, ENABLED, "1\n")
            syncPolicy(ctx, img, st)
        } else {
            GuestTree.delete(paths, ENABLED)
            GuestTree.delete(paths, SU)
            SU_LINKS.forEach { GuestTree.delete(paths, it) }
            // an empty policy: nothing is allowed while the switch is off
            GuestTree.writeText(paths, POLICY, "")
        }
    }

    /** `<package> allow|deny`, one per line — the format the gate greps. */
    fun syncPolicy(ctx: Context, img: GuestImage, st: RootState) {
        val paths = VmPaths(ctx, img.id)
        if (!File(paths.root, "system").isDirectory) return
        val body = st.apps.filter { it.granted || it.denied }
            .joinToString("") { "${it.pkg} ${if (it.granted) "allow" else "deny"}\n" }
        GuestTree.writeText(paths, POLICY, body)
    }

    /** True when the machine currently has a usable gate (the switch is on and `su` is written). */
    fun active(ctx: Context, imageId: String): Boolean {
        val paths = VmPaths(ctx, imageId)
        return GuestTree.exists(paths, ENABLED) && GuestTree.exists(paths, SU)
    }
}
