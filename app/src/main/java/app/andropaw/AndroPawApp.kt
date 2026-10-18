package app.andropaw

import android.app.Application
import app.andropaw.core.Keeper

class AndroPawApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Keeper.ensureChannel(this)
    }
}
