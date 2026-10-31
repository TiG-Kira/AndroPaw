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
    /**
     * Before the guest starts: the gate, the tools, and every enabled module's `system/` files
     * are in the tree.
     *
     * The overlay belongs here rather than after the boot: a module replaces framework files
     * that the first processes read while they start, which is exactly why Magisk applies its
     * modules in post-fs-data.
     */
    fun prepare(ctx: Context, img: GuestImage, log: (String) -> Unit) {
        if (!runCatching { java.io.File(VmPaths(ctx, img.id).root, "system").isDirectory }.getOrDefault(false)) return
        runCatching { RootManager.provision(ctx, img) }
            .onFailure { log("root: ${it.message}") }
        runCatching { PluginManager.refreshXposed(ctx, img) }
            .onFailure { log("plug-ins: ${it.message}") }
        runCatching { PluginManager.applyOverlays(ctx, img, log) }
            .onFailure { log("plug-in overlay: ${it.message}") }
        // the host half of `su`: a running machine always has someone answering its requests
        runCatching { RootManager.watch(ctx, img) }
            .onFailure { log("root requests: ${it.message}") }
    }

    /** Once the system is up: the modules' apks installed and their own boot scripts run. */
    fun onBoot(vm: GuestVm) {
        val ctx = vm.ctx
        val img = vm.img
        prepare(ctx, img, vm::log)
        runCatching { PluginManager.installPendingApks(vm, vm::log) }
            .onFailure { vm.log("plug-in apk: ${it.message}") }
        val hasScripts = runCatching {
            PluginManager.list(ctx, img).any { it.enabled && it.hasScripts }
        }.getOrDefault(false)
        // the root daemon of the generation starts from the same hook (apw_boot.sh), so a
        // rooted machine runs it even when it has no modules at all
        val rooted = runCatching { RootManager.state(ctx, img.id).enabled }.getOrDefault(false)
        if (hasScripts || rooted) {
            runCatching { vm.guestShell("/system/bin/sh /data/adb/andropaw/bin/boot", 180_000) }
                .onFailure { vm.log("plug-in scripts: ${it.message}") }
        }
    }

    /** The class path the Xposed generation's zygote has to load, or null (see PluginManager). */
    fun zygoteClasspath(ctx: Context, img: GuestImage): String? =
        runCatching { PluginManager.xposedClasspath(ctx, img) }.getOrNull()

    /** The properties the root layer wants in the guest's property area (empty when not rooted). */
    fun props(ctx: Context, img: GuestImage): Map<String, String> =
        runCatching { RootManager.props(ctx, img.id) }.getOrDefault(emptyMap())
}
