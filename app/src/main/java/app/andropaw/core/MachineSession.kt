package app.andropaw.core

import android.view.Surface
import java.io.File

/**
 * What the operating page ([app.andropaw.ui.VmActivity]) drives: one machine, whatever is
 * underneath it.
 *
 * Two kinds live behind this interface:
 *  - an Android machine: [GuestVm], with its shell, its radio, its adb, its recovery;
 *  - an OpenHarmony space: the [app.andropaw.openharmony] runtime, with its display and
 *    input bridges but none of the Android-only plumbing.
 *
 * The page is written once against this interface and asks [Caps] which operations exist,
 * instead of keeping two parallel copies of itself.
 *
 * State and log shapes are GuestVm's own, so the page's comparisons and its log panel need
 * no translation.
 */
interface MachineSession {
    enum class Kind { ANDROID, OPENHARMONY }

    val kind: Kind

    /** Stable id of the machine (an image id, or an OpenHarmony space id). */
    val id: String
    val title: String
    val subtitle: String

    val state: GuestVm.State
    val failure: String?
    val bootSeconds: Long

    /** The guest screen's pixel size — layout, screenshots, recording and VNC all use it. */
    val screenW: Int
    val screenH: Int

    /** fb0 to render through [app.andropaw.ui.GuestScreen]; null when the frame comes from a surface. */
    val fbFile: File?
    /** The page needs a SurfaceView (GPU bridge, GL bridge, OpenHarmony display bridge). */
    val needsSurface: Boolean
    /** The surface is pinned to [screenW]×[screenH] instead of following the window. */
    val surfaceFixed: Boolean
    /** recovery's minui flips between two pan-visible pages. */
    val fbPages: Int

    val navBar: Boolean
    val navSize: Int
    val navKeys: String
    val showLed: Boolean
    val keepScreenOn: Boolean

    val input: MachineInput
    val caps: Caps

    /** The Android machine behind this session; null for an OpenHarmony space. */
    val android: GuestVm?

    fun boot()
    fun stop()
    fun attachSurface(s: Surface?)

    /** Frames that actually reached the screen; 0 means nothing has been drawn yet. */
    fun frames(): Long

    fun log(s: String)
    /** Host-side log lines (the newest last). */
    fun lines(): List<String>
    /** Guest-side log lines: logcat for Android, the runner's log for OpenHarmony. */
    fun guestLog(): List<String>
    fun onState(cb: (GuestVm.State) -> Unit)
    fun onLog(cb: (String) -> Unit)

    /** The page is in front of the user (sensors on, the host's memory pressure guards relaxed). */
    fun setUiVisible(v: Boolean)
    /** Re-apply the out-of-memory shield right away (see GuestVm.reprotectNow). */
    fun reprotectNow()
    /** Watchdog tick: the home page's "running" badge stays lit while this keeps coming. */
    fun beat()
    fun publishStopped()
}

/** Key and button injection, as the operating page's toolbar and menus use it. */
interface MachineInput {
    fun press(code: Int, holdMs: Long = 80)
    fun key(code: Int, down: Boolean)
    fun home(down: Boolean)
    fun recents(api: Int, skin: String)
}

/**
 * Which operations this machine offers. The page hides what is missing instead of offering a
 * control that cannot work — an OpenHarmony space has no shell, no radio and no adb, and
 * saying so in the menu is better than a toast that says "failed".
 */
data class Caps(
    val shell: Boolean = false,
    val adb: Boolean = false,
    val telephony: Boolean = false,
    val battery: Boolean = false,
    val device: Boolean = false,
    val installApk: Boolean = false,
    val recovery: Boolean = false,
    val vnc: Boolean = false,
    val screenshot: Boolean = true,
    val record: Boolean = true,
)

/**
 * An Android machine behind [MachineSession]: everything delegates to the [GuestVm] the page
 * used to hold directly.
 */
class AndroidSession(val vm: GuestVm) : MachineSession {
    override val kind = MachineSession.Kind.ANDROID
    override val id: String get() = vm.img.id
    override val title: String get() = vm.img.name
    override val subtitle: String get() = vm.img.displayVersion
    override val state get() = vm.state
    override val failure get() = vm.failure
    override val bootSeconds get() = vm.bootSeconds()
    override val screenW: Int get() = if (vm.engine == Engine.GB) 480 else vm.settings.width
    override val screenH: Int get() = if (vm.engine == Engine.GB) 800 else vm.settings.height
    override val fbFile: File? get() = if (vm.recoveryMode) RecoveryImage.fb(vm.paths) else vm.paths.fb
    override val needsSurface: Boolean
        get() = (vm.engine == Engine.KK && vm.settings.gpu && !vm.recoveryMode && vm.img.api < 21) ||
            (vm.engine == Engine.N64 && vm.kernel.gpuBridge64 && vm.settings.gpu && !vm.recoveryMode)
    override val surfaceFixed: Boolean get() = vm.engine == Engine.N64 && vm.kernel.gpuBridge64 &&
        vm.settings.gpu && !vm.recoveryMode
    override val fbPages: Int get() = if (vm.recoveryMode) 2 else 1
    override val navBar: Boolean get() = vm.settings.showNavBar
    override val navSize: Int get() = vm.settings.navSize
    override val navKeys: String get() = vm.settings.navKeys
    override val showLed: Boolean get() = vm.settings.showLed
    override val keepScreenOn: Boolean get() = vm.settings.keepScreenOn
    override val input: MachineInput get() = vm.input
    override val caps = Caps(
        shell = true, adb = true, telephony = true, battery = true, device = true,
        installApk = true, recovery = true, vnc = true,
    )
    override val android: GuestVm? get() = vm

    override fun boot() = vm.boot()
    override fun stop() = vm.stop()
    override fun attachSurface(s: Surface?) { vm.surface = s }
    override fun frames(): Long = if (vm.glInApp) dev.lk.m7sense.GlBridge.frames() else 0L
    override fun log(s: String) = vm.log(s)
    override fun lines(): List<String> = vm.lines()
    override fun guestLog(): List<String> =
        GuestLog.tail(vm.paths.root, 128 * 1024).takeLast(300).map { "${it.prio} ${it.tag}: ${it.msg}" }
    override fun onState(cb: (GuestVm.State) -> Unit) = vm.onState(cb)
    override fun onLog(cb: (String) -> Unit) = vm.onLog(cb)
    override fun setUiVisible(v: Boolean) = vm.setUiVisible(v)
    override fun reprotectNow() = vm.reprotectNow()
    override fun beat() { runCatching { VmSession.beat(vm.ctx) } }
    override fun publishStopped() = VmSession.clear(vm.ctx)
}
