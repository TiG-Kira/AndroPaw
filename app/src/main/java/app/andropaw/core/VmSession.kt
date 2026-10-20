package app.andropaw.core

import android.content.Context
import java.io.File

/**
 * Which machine the :vm process is running right now, published for the app's home page.
 *
 * The VM lives in its own process ([:vm]), so its state is not visible to the main one. This file
 * in the shared files dir is the bridge: the VM writes "imageId\npid\nstate" on every state change
 * and clears it when the machine stops. The home page reads it to show the "running" badge and to
 * resume the machine instead of starting a second one.
 *
 * The pid makes a stale marker (a :vm process the system killed while in the background)
 * self-healing: the home page treats it as no session at all.
 */
object VmSession {
    private fun file(ctx: Context) = File(ctx.filesDir, "vm-session")

    fun publish(ctx: Context, imageId: String, state: String) {
        runCatching { file(ctx).writeText("$imageId\n${android.os.Process.myPid()}\n$state\n") }
    }

    fun clear(ctx: Context) { runCatching { file(ctx).delete() } }

    data class Session(val imageId: String, val pid: Int, val state: String) {
        val alive: Boolean get() = File("/proc/$pid").exists()
        /** a machine the user can return to (booting counts: the screen comes up in a moment) */
        val resumable: Boolean get() = alive && (state == "RUNNING" || state == "BOOTING" || state == "PREPARING")
    }

    fun current(ctx: Context): Session? {
        val f = file(ctx)
        if (!f.isFile) return null
        val parts = runCatching { f.readText().split('\n') }.getOrNull() ?: return null
        if (parts.size < 3) { runCatching { f.delete() }; return null }
        val s = Session(parts[0].trim(), parts[1].trim().toIntOrNull() ?: -1, parts[2].trim())
        if (!s.alive) { runCatching { f.delete() }; return null }
        return s
    }

    /** the image id of the running machine, or null */
    fun runningId(ctx: Context): String? = current(ctx)?.takeIf { it.resumable }?.imageId
}
