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
 *
 * Everything the guest does reaches the UI through [nativeEvent]: log lines, the display and
 * input bridges coming up, the first frame, and — the one that used to be invisible — the guest
 * process dying. Without it a space that never booted looked exactly like one that was only
 * slow: a black screen either way.
 */
object OhEngine {
    enum class State { STOPPED, PREPARING, BOOTING, RUNNING, FAILED }

    /** Events libohrt reports back (see the type constants in ohrt.c). */
    private const val EV_LOG = 1
    private const val EV_DISPLAY_UP = 2
    private const val EV_FIRST_FRAME = 3
    private const val EV_GUEST_EXIT = 4
    private const val EV_INPUT_UP = 5
    private const val EV_GPU_UP = 6

    /** Input kinds the guest's MMI server understands on input.sock (docs/oh-protocol.md §10). */
    const val KEY_BACK = 6
    const val KEY_HOME = 7

    class OhException(message: String) : Exception(message)

    /** The native entry every OpenHarmony runtime package must implement. */
    private class OhNative {
        external fun ohStart(configJson: String): Int
        external fun ohSurface(surface: Surface?): Int
        external fun ohTouch(action: Int, x: Float, y: Float, pointerId: Int): Int
        external fun ohKey(kind: Int): Int
        external fun ohFrames(): Long
        external fun ohStop(): Int
        external fun ohVersion(): String
    }

    @Volatile var state = State.STOPPED
        private set
    @Volatile var failure: String? = null
        private set
    @Volatile var runningSpaceId: String? = null
        private set
    /** when this run started, for the boot counter on the operating page */
    @Volatile var bootAt = 0L
        private set

    private val native = OhNative()
    private var libLoaded = false
    private val listeners = CopyOnWriteArrayList<(State) -> Unit>()
    private val logListeners = CopyOnWriteArrayList<(String) -> Unit>()
    private val lines = ArrayList<String>()
    private val linesLock = Any()
    /** the surface the page has handed over; kept so a boot that starts later still gets one */
    @Volatile var pendingSurface: Surface? = null
        private set

    fun onState(l: (State) -> Unit): () -> Unit { listeners.add(l); return { listeners.remove(l) } }
    fun onLog(l: (String) -> Unit): () -> Unit { logListeners.add(l); return { logListeners.remove(l) } }

    /** the app context of the running space, for the home page's "running" marker */
    @Volatile private var hostCtx: android.content.Context? = null
    @Volatile private var beatThread: Thread? = null

    private fun setState(s: State) {
        state = s
        publish(s)
        listeners.forEach { runCatching { it(s) } }
    }

    /**
     * The home page watches one file for "which machine is up" (core/VmSession). The runner's
     * own state changes are what it is told, and a beat every few seconds keeps it from
     * expiring — without one the marker goes stale and the card stops showing as running.
     */
    private fun publish(s: State) {
        val c = hostCtx ?: return
        val id = runningSpaceId
        runCatching {
            if (id == null || s == State.STOPPED) app.andropaw.core.VmSession.clear(c)
            else app.andropaw.core.VmSession.publish(c, id, s.name)
        }
    }

    private fun startBeat() {
        if (beatThread != null) return
        beatThread = Thread({
            while (state != State.STOPPED) {
                hostCtx?.let { runCatching { app.andropaw.core.VmSession.beat(it) } }
                runCatching { Thread.sleep(3000) }
            }
            beatThread = null
        }, "oh-beat").apply { isDaemon = true; start() }
    }

    /** A line from anywhere in this run: the Java side, or the runner through [nativeEvent]. */
    private fun addLine(s: String) {
        synchronized(linesLock) {
            lines.add(s)
            if (lines.size > 400) lines.subList(0, lines.size - 400).clear()
        }
        logListeners.forEach { runCatching { it(s) } }
    }

    fun log(s: String) = addLine("${stamp()} $s")

    private fun stamp(): String {
        val t = java.util.Calendar.getInstance()
        return String.format(java.util.Locale.US, "%02d:%02d:%02d", t.get(java.util.Calendar.HOUR_OF_DAY),
            t.get(java.util.Calendar.MINUTE), t.get(java.util.Calendar.SECOND))
    }

    fun lines(): List<String> = synchronized(linesLock) { lines.toList() }

    /**
     * Called by libohrt (any thread). Log lines land in the panel the operating page shows; the
     * rest move the state machine, which is how a guest that died on the first job stops looking
     * like a guest that is merely still booting.
     */
    @JvmStatic
    fun nativeEvent(type: Int, arg: Long, msg: String?) {
        when (type) {
            EV_LOG -> addLine(msg ?: return)
            EV_DISPLAY_UP -> {
                addLine("${stamp()} display: bridge connected — the guest's composer is publishing frames")
                if (state == State.BOOTING || state == State.PREPARING) setState(State.RUNNING)
            }
            EV_FIRST_FRAME -> {
                addLine("${stamp()} display: first frame on screen")
                if (state != State.RUNNING) setState(State.RUNNING)
            }
            EV_GUEST_EXIT -> {
                val what = if (arg > 0 && arg and 0x7f != 0L) "killed by signal ${arg and 0x7f}"
                    else "exited with code ${(arg shr 8) and 0xff}"
                failure = "the OpenHarmony guest ${what}"
                addLine("${stamp()} ✖ guest ${what} — the space stopped")
                setState(State.FAILED)
            }
            EV_INPUT_UP -> addLine("${stamp()} input: bridge connected — touch and keys enabled")
            EV_GPU_UP -> addLine(if (arg != 0L) "${stamp()} gpu: pass-through up"
                else "${stamp()} gpu: pass-through unavailable, the guest falls back to software")
        }
    }

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
        bootAt = System.currentTimeMillis()
        hostCtx = ctx
        setState(State.PREPARING)
        startBeat()
        if (surface != null) pendingSurface = surface
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
                    // OpenHarmony's init lives at /system/bin/init (there is no /sbin)
                    .put("OHRT_EXEC", "/system/bin/init")
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
                // GPU pass-through (docs/oh-protocol.md §11): the vendor rutabaga
                // renderer takes the window density; geometry/fps follow the
                // display bridge parameters computed natively.
                .put("gpu", JSONObject()
                    .put("enabled", true)
                    .put("dpi", ctx.resources.configuration.densityDpi))

            runningSpaceId = space.id
            setState(State.BOOTING)
            log("boot: runtime ${runtimeDir.name}, openharmony $ohVersion, space ${space.id}")
            log("launcher: ${runner?.name ?: "musl loader (no libaemu64 — the guest will see the host's paths)"}")
            pendingSurface?.let { native.ohSurface(it) }
            val rc = native.ohStart(cfg.toString())
            if (rc != 0) {
                // the runner writes its own diagnosis to runner.log; surface the tail so the
                // failure dialog says "no guest init found under …" instead of a bare rc=-3
                val tail = runCatching {
                    File(OhSpaceStore.runDir(ctx, space.id), "runner.log")
                        .readLines().takeLast(5).joinToString("\n")
                }.getOrDefault("")
                throw OhException(
                    "runner rejected the config (rc=$rc)" +
                        if (tail.isBlank()) "" else "\n$tail",
                )
            }
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
        pendingSurface = surface
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
        if (state == State.RUNNING || state == State.BOOTING) runCatching { native.ohTouch(action, x, y, pointerId) }
    }

    /**
     * A navigation key. The guest's MMI server synthesises a full press (down+up) per message
     * and knows exactly two of them — back and home — so there is nothing else to send.
     */
    fun key(kind: Int): Boolean =
        runCatching { native.ohKey(kind) == 0 }.getOrDefault(false)

    /** Frames the display bridge has put on the screen; 0 means the screen is still empty. */
    fun frames(): Long = runCatching { native.ohFrames() }.getOrDefault(0L)

    @Synchronized
    fun stop() {
        if (state == State.STOPPED) return
        runCatching { native.ohStop() }
        runningSpaceId = null
        bootAt = 0
        hostCtx = null
        setState(State.STOPPED)
    }
}
