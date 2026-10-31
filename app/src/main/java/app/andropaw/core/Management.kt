package app.andropaw.core

import android.content.Context

/**
 * The host-side half of root and plug-ins, hooked into the machine's own boot.
 *
 * Called twice per boot: [prepare] before the first guest process starts (so `su` and the
 * Xposed class path exist from the very first app), and [onBoot] once the system is up (so
 * overlays are in place and the modules' own scripts can run). Both are best-effort: a
 * management failure must never keep a machine from booting.
 */
object Management {
    /** Before the guest starts: the gate and the tools are in the tree. */
    fun prepare(ctx: Context, img: GuestImage, log: (String) -> Unit) {
        if (!runCatching { java.io.File(VmPaths(ctx, img.id).root, "system").isDirectory }.getOrDefault(false)) return
        runCatching { RootManager.provision(ctx, img) }
            .onFailure { log("root: ${it.message}") }
        runCatching { PluginManager.refreshXposed(ctx, img) }
            .onFailure { log("plug-ins: ${it.message}") }
    }

    /** Once the system is up: overlay files in place, apks installed, module scripts run. */
    fun onBoot(vm: GuestVm) {
        val ctx = vm.ctx
        val img = vm.img
        prepare(ctx, img, vm::log)
        runCatching { PluginManager.applyOverlays(ctx, img, vm::log) }
            .onFailure { vm.log("plug-in overlay: ${it.message}") }
        runCatching { PluginManager.installPendingApks(vm, vm::log) }
            .onFailure { vm.log("plug-in apk: ${it.message}") }
        val hasScripts = runCatching {
            PluginManager.list(ctx, img).any { it.enabled && it.hasScripts }
        }.getOrDefault(false)
        if (hasScripts) {
            runCatching { vm.guestShell("/system/bin/sh /data/adb/andropaw/bin/boot", 180_000) }
                .onFailure { vm.log("plug-in scripts: ${it.message}") }
        }
    }

    /** The class path the Xposed generation's zygote has to load, or null (see PluginManager). */
    fun zygoteClasspath(ctx: Context, img: GuestImage): String? =
        runCatching { PluginManager.xposedClasspath(ctx, img) }.getOrNull()
}
