package app.andropaw.openharmony

import android.content.Context
import android.view.Surface
import org.json.JSONObject
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

/**
 * The stable Java side of the OpenHarmony boot contract.
 *
 * A runtime package ships the OpenHarmony rootfs plus, optionally, its own native runner. The
 * runner implements exactly one Java-visible entry point, [OhNative], and receives everything
 * else through a JSON config: paths, host bridge sockets (the same unix-socket transports
 * core/InputService, core/AudioOut and the GL bridge already serve for Android guests) and the
 * space settings.
 *
 * The runner library is resolved from the *runtime directory* first (a "lib" entry named by
 * engine.json), falling back to AndroPaw's built-in libohrt.so — so runtime packages can evolve
 * independently of AndroPaw releases, and the app boots OpenHarmony out of the box.
 */
object OhEngine {
    enum class State { STOPPED, PREPARING, BOOTING, RUNNING, FAILED }

    class OhException(message: String) : Exception(message)

    /** The native entry every OpenHarmony runtime package must implement. */
    private class OhNative {
        external fun ohStart(configJson: String): Int
        external fun ohSurface(surface: Surface?): Int
        external fun ohTouch(action: Int, x: Float, y: Float, pointerId: Int): Int
        external fun ohStop(): Int
        external fun ohVersion(): String
    }

    @Volatile var state = State.STOPPED
        private set
    @Volatile var failure: String? = null
        private set
    @Volatile var runningSpaceId: String? = null
        private set

    private val native = OhNative()
    private var libLoaded = false
    private val listeners = CopyOnWriteArrayList<(State) -> Unit>()
    private val logListeners = CopyOnWriteArrayList<(String) -> Unit>()

    fun onState(l: (State) -> Unit): () -> Unit { listeners.add(l); return { listeners.remove(l) } }
    fun onLog(l: (String) -> Unit): () -> Unit { logListeners.add(l); return { logListeners.remove(l) } }

    private fun setState(s: State) { state = s; listeners.forEach { runCatching { it(s) } } }
    private fun log(s: String) { logListeners.forEach { runCatching { it(s) } } }

    private fun loadRuntimeLib(runtimeDir: File, libName: String) {
        if (libLoaded) return
        val packaged = File(runtimeDir, "lib/$libName")
        if (packaged.isFile) {
            System.load(packaged.absolutePath)
        } else {
            // the built-in runner (app/src/main/cpp/ohrt): spawns the guest through the musl
            // loader inside the runtime package and serves the host-bridge socket
            System.loadLibrary("ohrt")
        }
        libLoaded = true
    }

    /**
     * Boot [space] against the runtime installed in [runtimeDir]. One space runs at a time:
     * starting a new one stops the old one.
     */
    @Synchronized
    fun start(
        ctx: Context,
        space: OhSpace,
        runtimeDir: File,
        surface: Surface?,
    ) {
        if (state == State.BOOTING || state == State.RUNNING || state == State.PREPARING) {
            if (runningSpaceId == space.id) return
            stop()
        }
        failure = null
        setState(State.PREPARING)
        try {
            val manifest = File(runtimeDir, "runtime.json")
            if (!manifest.isFile) throw OhException("runtime ${runtimeDir.name} is not installed")
            val engineJson = File(runtimeDir, "engine.json").takeIf { it.isFile }
                ?.let { runCatching { JSONObject(it.readText()) }.getOrNull() }
            val libName = engineJson?.optString("lib", "libohrt.so")?.removePrefix("lib")?.removeSuffix(".so")
                ?: "ohrt"

            loadRuntimeLib(runtimeDir, "lib$libName.so")
            val ohVersion = runCatching { native.ohVersion() }.getOrDefault(space.ohVersion)
            val runner = guestRunner(ctx)

            val cfg = JSONObject()
                .put("space", space.id)
                .put("rootfs", File(runtimeDir, "rootfs").absolutePath)
                .put("helper", File(runtimeDir, "helper").absolutePath)
                .put("dataDir", OhSpaceStore.dataDir(ctx, space.id).absolutePath)
                .put("runDir", OhSpaceStore.runDir(ctx, space.id).absolutePath)
                .put("ohVersion", ohVersion)
                .put("settings", space.settings.toJson())
                .put("runner", runner?.absolutePath ?: "")
                // the runner contract: the guest process is spawned through the musl loader
                // shipped in the runtime package, pointed at the host-bridge unix socket
                .put("env", JSONObject()
                    .put("OHRT_ROOT", File(runtimeDir, "rootfs").absolutePath)
                    .put("OHRT_EXEC", "/sbin/init")
                    .put("OHRT_LOADER", File(runtimeDir, "lib").absolutePath)
                    .put("OHRT_HOST_API", File(OhSpaceStore.runDir(ctx, space.id), "host.sock").absolutePath))
                // host bridges over the same unix-socket transports the Android guests use
                .put("sockets", JSONObject()
                    .put("input", "input")
                    .put("frame", "frame")
                    .put("audio", "eac")
                    .put("camera", "aemu_cam")
                    .put("clipboard", "oh_clipboard"))
                .put("share", JSONObject()
                    .put("camera", space.settings.shareCamera)
                    .put("mic", space.settings.shareMic)
                    .put("location", space.settings.shareLocation)
                    .put("sensors", space.settings.shareSensors)
                    .put("clipboard", space.settings.shareClipboard)
                    .put("audio", space.settings.shareAudio))

            runningSpaceId = space.id
            setState(State.BOOTING)
            log("boot: runtime ${runtimeDir.name}, openharmony $ohVersion, space ${space.id}")
            log("launcher: ${runner?.name ?: "musl loader (no libaemu64 — the guest will see the host's paths)"}")
            surface?.let { native.ohSurface(it) }
            val rc = native.ohStart(cfg.toString())
            if (rc != 0) throw OhException("runner rejected the config (rc=$rc)")
            setState(State.RUNNING)
            OhSpaceStore.update(ctx, space.id) {
                it.copy(lastBootMs = System.currentTimeMillis(), bootCount = it.bootCount + 1)
            }
        } catch (t: Throwable) {
            failure = t.message ?: t.toString()
            log("✖ boot aborted: $failure")
            setState(State.FAILED)
        }
    }

    /**
     * The host-side guest launcher shipped with AndroPaw: `libaemu64_<api>.so`, the same runner
     * the Android machines boot through. It loads the guest ELF, installs a seccomp filter and
     * rewrites the guest's absolute paths under the runtime's root, so an OpenHarmony userland
     * gets its own /system, /data and /dev while running as an ordinary app process — and its
     * mount/chroot/unshare calls, which seccomp would kill, report success instead.
     *
     * The newest one wins: they differ only in the syscall tables of the Android release they
     * were built for, and any of them is newer than what OpenHarmony needs.
     */
    private fun guestRunner(ctx: Context): File? =
        File(ctx.applicationInfo.nativeLibraryDir).listFiles()
            ?.filter { it.name.startsWith("libaemu64_") && it.name.endsWith(".so") && it.canExecute() }
            ?.maxByOrNull { it.name.removePrefix("libaemu64_").removeSuffix(".so").toIntOrNull() ?: 0 }

    /** Attach the display surface of the current run (called from the activity's SurfaceView). */
    @Synchronized
    fun surface(surface: Surface?) {
        if (state == State.RUNNING || state == State.BOOTING) {
            runCatching { native.ohSurface(surface) }
        }
    }

    /**
     * Inject a touch event into the guest's input channel. [action] uses the
     * Android MotionEvent constants (they are the guest wire enum verbatim);
     * [x]/[y] are normalised to the view size, [pointerId] the Android pointer id.
     */
    fun touch(action: Int, x: Float, y: Float, pointerId: Int) {
        if (state == State.RUNNING) runCatching { native.ohTouch(action, x, y, pointerId) }
    }

    @Synchronized
    fun stop() {
        if (state == State.STOPPED) return
        runCatching { native.ohStop() }
        runningSpaceId = null
        setState(State.STOPPED)
    }
}
