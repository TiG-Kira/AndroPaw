package app.andropaw

import android.app.Application
import app.andropaw.core.Keeper
import rikka.shizuku.ShizukuProvider

class AndroPawApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Keeper.ensureChannel(this)
        // The machine lives in `:vm`, so Shizuku's binder has to be reachable from more than the
        // main process. Must run before any Shizuku call (see core/HostShell.kt).
        runCatching { ShizukuProvider.enableMultiProcessSupport(true) }
    }
}
