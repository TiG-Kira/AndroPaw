package app.aemu.core

/**
 * The renderer of guest programs on the phone's GPU (native/glbridge/host.c): the guest's GLES driver connects to
 * the socket, one connection per drawing thread, and its commands are played in real GLES 3 contexts here.
 */
object GlHost {
    private val ok: Boolean = try { System.loadLibrary("aemugl"); true } catch (_: Throwable) { false }

    /** "ok <renderer>, <version>" or why not */
    fun start(socket: String): String = if (ok) runCatching { nativeStart(socket) }.getOrElse { it.toString() } else "libaemugl.so does not load"
    fun stop() { if (ok) runCatching { nativeStop() } }

    /** the view finished frames are drawn into by the GPU (null: none, frames are dropped) */
    fun surface(s: android.view.Surface?) { if (ok) runCatching { nativeSurface(s) } }
    /** how many frames went to the screen that way since start() */
    fun presented(): Long = if (ok) runCatching { nativePresented() }.getOrDefault(0L) else 0L

    @JvmStatic private external fun nativeStart(socket: String): String
    @JvmStatic private external fun nativeStop()
    @JvmStatic private external fun nativeSurface(s: android.view.Surface?)
    @JvmStatic private external fun nativePresented(): Long
}
