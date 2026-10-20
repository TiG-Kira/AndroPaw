package app.andropaw.core

import android.content.Context
import java.io.File

/**
 * Which machine the :vm process is running right now, published for the app's home page.
 *
 * The VM lives in its own process ([:vm]), so its state is not visible to the main one. This file
 * in the shared files dir is the bridge: the VM writes "imageId\npid\nstate\nbeat" on every state
 * change and on every watchdog tick, and clears it when the machine stops. The home page reads it
 * to show the "running" badge and to resume the machine instead of starting a second one.
 *
 * Liveness is decided by the heartbeat, not by /proc: on many builds a process may not read
 * another process's /proc entry even of the same app (hidepid), which made every marker look dead.
 * A :vm process killed by the system stops beating, so its marker expires on its own; the pid is
 * kept as a second opinion for the builds where /proc is readable.
 */
object VmSession {
    /** the :vm process beats every 3s (its watchdog tick); older than this counts as dead */
    private const val BEAT_MAX_AGE_MS = 15_000L

    private fun file(ctx: Context) = File(ctx.filesDir, "vm-session")

    fun publish(ctx: Context, imageId: String, state: String) = write(ctx, imageId, state)

    /** Heartbeat: the same contents with a fresh timestamp (the VM's watchdog tick calls this). */
    fun beat(ctx: Context) {
        val s = read(ctx) ?: return
        if (s.state == "STOPPED") return
        write(ctx, s.imageId, s.state)
    }

    fun clear(ctx: Context) { runCatching { file(ctx).delete() } }

    private fun write(ctx: Context, imageId: String, state: String) {
        runCatching {
            file(ctx).writeText("$imageId\n${android.os.Process.myPid()}\n$state\n${System.currentTimeMillis()}\n")
        }
    }

    data class Session(val imageId: String, val pid: Int, val state: String, val beatAt: Long) {
        val fresh: Boolean get() = System.currentTimeMillis() - beatAt < BEAT_MAX_AGE_MS
        /** a beat older than [BEAT_MAX_AGE_MS] means the :vm process is gone (killed or crashed) */
        val alive: Boolean get() = fresh ||
            (pid > 0 && runCatching { File("/proc/$pid").exists() }.getOrDefault(false))
        /** a machine the user can return to (booting counts: the screen comes up in a moment) */
        val resumable: Boolean get() = alive && (state == "RUNNING" || state == "BOOTING" || state == "PREPARING")
    }

    private fun read(ctx: Context): Session? {
        val f = file(ctx)
        if (!f.isFile) return null
        val parts = runCatching { f.readText().split('\n') }.getOrNull() ?: return null
        if (parts.size < 3) { runCatching { f.delete() }; return null }
        val imageId = parts[0].trim()
        if (imageId.isEmpty()) { runCatching { f.delete() }; return null }
        return Session(
            imageId,
            parts[1].trim().toIntOrNull() ?: -1,
            parts[2].trim(),
            parts.getOrNull(3)?.trim()?.toLongOrNull() ?: 0L,
        )
    }

    fun current(ctx: Context): Session? {
        val s = read(ctx) ?: return null
        if (!s.alive) { runCatching { file(ctx).delete() }; return null }
        return s
    }

    /** the image id of the running machine, or null */
    fun runningId(ctx: Context): String? = current(ctx)?.takeIf { it.resumable }?.imageId
}
