package app.andropaw.openharmony

import android.content.Context
import android.view.Surface
import app.andropaw.core.Caps
import app.andropaw.core.GuestVm
import app.andropaw.core.InputService
import app.andropaw.core.MachineInput
import app.andropaw.core.MachineSession
import app.andropaw.core.VmSession
import java.io.File

/**
 * An OpenHarmony space behind the operating page's [MachineSession]: the same boot, stop, log
 * and input contract an Android machine offers, carried by [OhEngine] instead of core/GuestVm.
 *
 * What an OpenHarmony space has no use for is simply not offered — no guest shell, no radio,
 * no adb, no recovery — and [Caps] tells the page so, so the menu shows what actually works.
 *
 * The log is the part that matters most here: the runner's own log file is tail'd into the
 * page's guest-log tab, and every line libohrt writes comes back through OhEngine's callbacks,
 * so a space that never comes up says why instead of just staying black.
 */
class OhSession(
    private val ctx: Context,
    val space: OhSpace,
) : MachineSession {

    override val kind = MachineSession.Kind.OPENHARMONY
    override val id: String get() = space.id
    override val title: String get() = space.name
    override val subtitle: String get() = space.displayVersion

    override val state: GuestVm.State
        get() = when (OhEngine.state) {
            OhEngine.State.STOPPED -> GuestVm.State.STOPPED
            OhEngine.State.PREPARING -> GuestVm.State.PREPARING
            OhEngine.State.BOOTING -> GuestVm.State.BOOTING
            OhEngine.State.RUNNING -> GuestVm.State.RUNNING
            OhEngine.State.FAILED -> GuestVm.State.FAILED
        }
    override val failure: String? get() = OhEngine.failure
    override val bootSeconds: Long
        get() = OhEngine.bootAt.let { if (it == 0L) 0L else (System.currentTimeMillis() - it) / 1000 }

    override val screenW: Int get() = space.settings.width
    override val screenH: Int get() = space.settings.height

    override val fbFile: File? get() = null
    override val needsSurface: Boolean get() = true
    override val surfaceFixed: Boolean get() = false
    override val fbPages: Int get() = 1

    /** The guest draws its own navigation; the page's key bar keeps back and home for it. */
    override val navBar: Boolean get() = true
    override val navSize: Int get() = 0
    override val navKeys: String get() = "back,home"
    override val showLed: Boolean get() = false
    override val keepScreenOn: Boolean get() = true

    override val input: MachineInput get() = OhInput
    override val caps = Caps()   // shell/adb/radio/apk/recovery/vnc: none of them exist here
    override val android: GuestVm? get() = null

    /** the runner's log: everything libohrt and the guest wrote during this run */
    private fun runnerLog(): File = File(OhSpaceStore.runDir(ctx, space.id), "runner.log")

    init {
        OhEngine.onState { st ->
            if (st == OhEngine.State.STOPPED) VmSession.clear(ctx)
            else runCatching { VmSession.publish(ctx, space.id, state.name) }
        }
    }

    override fun boot() {
        val rt = OhRuntimeRepository.get(ctx, space.runtimeVersion)
        if (rt == null) {
            OhEngine.log("✖ runtime ${space.runtimeVersion} is gone — deploy it again from the settings page")
            OhEngine.stop()
            return
        }
        OhEngine.start(ctx, space, OhRuntimeRepository.dir(ctx, rt.version), OhEngine.pendingSurface)
    }

    override fun stop() {
        OhEngine.stop()
        VmSession.clear(ctx)
    }

    override fun attachSurface(s: Surface?) = OhEngine.surface(s)

    override fun frames(): Long = OhEngine.frames()

    override fun log(s: String) = OhEngine.log(s)

    override fun lines(): List<String> = OhEngine.lines()

    override fun guestLog(): List<String> = runCatching {
        runnerLog().readLines().takeLast(300)
    }.getOrDefault(emptyList())

    override fun onState(cb: (GuestVm.State) -> Unit) {
        OhEngine.onState { cb(state) }
    }

    override fun onLog(cb: (String) -> Unit) {
        OhEngine.onLog(cb)
    }

    override fun setUiVisible(v: Boolean) {
        // the space has no sensors to pause; the display bridge keeps its surface either way
    }

    override fun reprotectNow() {
        // out-of-memory shielding is an Android-machine concern (core/GuestVm.protectProcessTree)
    }

    override fun beat() { runCatching { VmSession.beat(ctx) } }

    override fun publishStopped() = VmSession.clear(ctx)

    /**
     * Keys for the guest's MMI server: it takes a whole press per message and knows back and
     * home only, so a down/up pair is one send and everything else is dropped.
     */
    private object OhInput : MachineInput {
        private fun send(code: Int) {
            val kind = when (code) {
                InputService.KEY_BACK -> OhEngine.KEY_BACK
                InputService.KEY_HOME -> OhEngine.KEY_HOME
                else -> return
            }
            OhEngine.key(kind)
        }

        override fun press(code: Int, holdMs: Long) = send(code)
        override fun key(code: Int, down: Boolean) { if (down) send(code) }
        override fun home(down: Boolean) { if (down) send(InputService.KEY_HOME) }
        override fun recents(api: Int, skin: String) { /* no recents key on the OH wire */ }
    }
}
