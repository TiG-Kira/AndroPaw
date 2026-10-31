package app.andropaw.core

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipFile

/** Which framework a machine's generation uses to load plug-ins. */
enum class PluginBackend(val key: String, val label: String, val note: String) {
    /** Android 4.x–7.x: modules hook the framework through Xposed. */
    XPOSED("xposed", "Xposed", "4.x–7.x"),

    /** Android 8.0–12: modules are Magisk modules. */
    MAGISK("magisk", "Magisk", "8.0–12"),

    /** Android 12L and newer: modules are KernelSU modules. */
    KERNELSU("kernelsu", "KernelSU", "12L+");

    companion object {
        fun forApi(api: Int): PluginBackend = when {
            api >= 32 -> KERNELSU
            api >= 26 -> MAGISK
            else -> XPOSED
        }
    }
}

/** One plug-in as the two pages show it. */
data class Plugin(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val enabled: Boolean,
    val backend: PluginBackend,
    /** Directory inside the guest, e.g. /data/adb/modules/battery_tweak */
    val dir: String,
    /** The apk the plug-in ships, if any, as a guest path. */
    val apk: String?,
    val sizeBytes: Long,
    val installedAt: Long,
    val hasScripts: Boolean,
    val hasAction: Boolean,
    val xposedEntry: String?,
    val pendingRemoval: Boolean,
    /** The apk is not in the guest's package list yet (installed while the machine was off). */
    val apkPending: Boolean,
) {
}

/**
 * The plug-in store of one machine.
 *
 * A plug-in is a directory in the guest's own module tree — `/data/adb/modules/<id>` for the
 * Magisk and KernelSU generations, `/data/adb/xposed/<id>` for the Xposed one — with the same
 * `module.prop` Magisk and KernelSU read, so anything that understands those layouts sees the
 * same state: `magisk --list`, `ksud module list`, our own pages.
 *
 * What the host adds on top is the part a real Magisk cannot do here: there is no overlayfs
 * and no magiskd in this emulator, so a module's `system/` files are copied into the guest
 * tree at boot (keeping a backup of anything they replace, restored on disable / uninstall)
 * and its `post-fs-data.sh` / `service.sh` are run through `/data/adb/andropaw/bin/boot`.
 */
object PluginManager {
    private const val MODULES = "/data/adb/modules"
    private const val XPOSED = "/data/adb/xposed"
    private const val OVERLAY_LIST = ".apw-overlay"
    private const val OVERLAY_BACKUP = ".apw-backup"
    private const val APK_INSTALLED = ".apw-apk-installed"

    fun backendFor(img: GuestImage): PluginBackend = PluginBackend.forApi(img.api)

    /** Where this machine keeps plug-ins of [backend]. */
    fun storePath(backend: PluginBackend): String =
        if (backend == PluginBackend.XPOSED) XPOSED else MODULES

    fun dirOf(ctx: Context, img: GuestImage, backend: PluginBackend, id: String): File =
        GuestTree.file(VmPaths(ctx, img.id), "${storePath(backend)}/$id")

    // ------------------------------------------------------------------ reading

    fun list(ctx: Context, img: GuestImage): List<Plugin> {
        val paths = VmPaths(ctx, img.id)
        val out = ArrayList<Plugin>()
        for (backend in PluginBackend.entries) {
            val base = GuestTree.file(paths, storePath(backend))
            if (!base.isDirectory) continue
            base.listFiles()?.filter { it.isDirectory }?.forEach { d ->
                runCatching { read(ctx, img, backend, d)?.let { out += it } }
            }
        }
        return out.sortedWith(compareByDescending<Plugin> { it.installedAt })
    }

    fun find(ctx: Context, img: GuestImage, id: String): Plugin? =
        list(ctx, img).firstOrNull { it.id == id }

    private fun read(ctx: Context, img: GuestImage, backend: PluginBackend, d: File): Plugin? {
        val paths = VmPaths(ctx, img.id)
        val prop = parseProps(File(d, "module.prop").takeIf { it.isFile }?.let { runCatching { it.readText() }.getOrNull() })
        val apw = runCatching { JSONObject(File(d, "apw.json").readText()) }.getOrNull()
        val id = prop["id"]?.takeIf { it.isNotBlank() } ?: d.name
        val apk = File(d, "module.apk").takeIf { it.isFile }
        val apkRel = apk?.let { "${storePath(backend)}/${d.name}/module.apk" }
        return Plugin(
            id = id,
            name = prop["name"]?.takeIf { it.isNotBlank() } ?: id,
            version = prop["version"] ?: "",
            author = prop["author"] ?: "",
            description = prop["description"] ?: "",
            enabled = !File(d, "disable").exists() && !File(d, "remove").exists(),
            backend = backend,
            dir = "${storePath(backend)}/${d.name}",
            apk = apkRel,
            sizeBytes = GuestTree.sizeOf(d),
            installedAt = apw?.optLong("installedAt", 0L)
                ?: runCatching { d.lastModified() }.getOrDefault(0L),
            hasScripts = File(d, "post-fs-data.sh").exists() || File(d, "service.sh").exists(),
            hasAction = File(d, "action.sh").exists(),
            xposedEntry = File(d, "xposed_init").takeIf { it.isFile }
                ?.let { runCatching { it.readText().trim() }.getOrNull() }
                ?: prop["entry"],
            pendingRemoval = File(d, "remove").exists(),
            apkPending = apk != null && !File(d, APK_INSTALLED).exists(),
        )
    }

    private fun parseProps(text: String?): Map<String, String> {
        if (text == null) return emptyMap()
        val map = LinkedHashMap<String, String>()
        text.lineSequence().forEach { line ->
            val t = line.trim()
            if (t.isEmpty() || t.startsWith("#") || !t.contains('=')) return@forEach
            map[t.substringBefore('=').trim()] = t.substringAfter('=').trim()
        }
        return map
    }

    // ------------------------------------------------------------------ installing

    /**
     * Installs [src] (a module zip, or an apk that is a plug-in) into the machine.
     *
     * The module is unpacked into the guest's own store, so it is there whether or not the
     * machine is running; an apk it carries is installed into the guest as well (immediately
     * when the machine is up, otherwise at the next boot).
     */
    fun install(ctx: Context, img: GuestImage, src: File, vm: GuestVm? = null): Plugin {
        val backend = backendFor(img)
        val meta = probe(ctx, src)
        val paths = VmPaths(ctx, img.id)
        val target = GuestTree.file(paths, "${storePath(backend)}/${meta.id}")
        target.deleteRecursively()
        target.mkdirs()

        var apkName: String? = null
        ZipFile(src).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val e = entries.nextElement()
                if (e.isDirectory) continue
                val name = e.name.replace('\\', '/')
                if (name.startsWith("META-INF/")) continue
                if (name.contains("..") || name.startsWith("/")) continue
                val out = File(target, name)
                out.parentFile?.mkdirs()
                zip.getInputStream(e).use { input -> out.outputStream().use { input.copyTo(it) } }
                if (name.endsWith(".apk") && (name == "module.apk" || !name.contains('/'))) apkName = name
            }
        }

        // an apk picked on its own is the plug-in: keep it as the module's own apk
        if (src.name.endsWith(".apk", true) && apkName == null) {
            src.copyTo(File(target, "module.apk"), overwrite = true)
            apkName = "module.apk"
        }
        val apk = apkName?.let { File(target, it) }
        val apkRel = apk?.let { "${storePath(backend)}/${meta.id}/${it.name}" }

        // module.prop: the firmware's own, or one built from what the apk says
        if (!File(target, "module.prop").isFile) {
            File(target, "module.prop").writeText(
                buildString {
                    appendLine("id=${meta.id}")
                    appendLine("name=${meta.name}")
                    appendLine("version=${meta.version}")
                    appendLine("versionCode=1")
                    appendLine("author=${meta.author}")
                    appendLine("description=${meta.description}")
                }
            )
        }
        meta.entry?.let { File(target, "xposed_init").writeText(it + "\n") }

        File(target, "apw.json").writeText(
            JSONObject()
                .put("installedAt", System.currentTimeMillis())
                .put("backend", backend.key)
                .put("apk", apk?.name ?: JSONObject.NULL)
                .toString()
        )

        // the Xposed generation keeps the list of apks the zygote has to load
        if (backend == PluginBackend.XPOSED) refreshXposed(ctx, img)

        if (apk != null && apkRel != null) {
            if (vm != null && vm.state == GuestVm.State.RUNNING) {
                val out = runCatching { vm.installApk("/$apkRel") }.getOrElse { "install failed: $it" }
                if (!out.contains("Failure")) File(target, APK_INSTALLED).writeText("1")
            }
        }
        return read(ctx, img, backend, target) ?: error("plug-in ${meta.id} could not be read back")
    }

    private data class Meta(
        val id: String,
        val name: String,
        val version: String,
        val author: String,
        val description: String,
        val entry: String?,
    )

    /**
     * What a module zip or an apk says about itself.
     *
     * A module carries `module.prop`; an apk picked on its own carries its package — which the
     * host reads with its own PackageManager, the same way the ROOT page reads labels.
     */
    private fun probe(ctx: Context, src: File): Meta {
        var props: String? = null
        var entry: String? = null
        var inner: File? = null
        ZipFile(src).use { zip ->
            zip.getEntry("module.prop")?.let { e ->
                props = zip.getInputStream(e).use { it.readBytes().toString(Charsets.UTF_8) }
            }
            zip.getEntry("assets/xposed_init")?.let { e ->
                entry = zip.getInputStream(e).use { it.readBytes().toString(Charsets.UTF_8) }
                    .lineSequence().map { l -> l.trim() }.firstOrNull { l -> l.isNotEmpty() }
            }
            zip.getEntry("module.apk")?.let { e ->
                val tmp = File.createTempFile("apw-probe", ".apk")
                zip.getInputStream(e).use { i -> tmp.outputStream().use { i.copyTo(it) } }
                inner = tmp
                ZipFile(tmp).use { z ->
                    z.getEntry("assets/xposed_init")?.let { x ->
                        entry = z.getInputStream(x).use { it.readBytes().toString(Charsets.UTF_8) }
                            .lineSequence().map { l -> l.trim() }.firstOrNull { l -> l.isNotEmpty() }
                    }
                }
            }
        }
        val map = parseProps(props)
        val apk = inner ?: src.takeIf { it.name.endsWith(".apk", true) }
        val pkg = apk?.let { packageOf(ctx, it) }
        runCatching { inner?.delete() }

        val id = map["id"]?.takeIf { it.isNotBlank() } ?: pkg?.first ?: "plugin"
        return Meta(
            id = id,
            name = map["name"]?.takeIf { it.isNotBlank() } ?: pkg?.second ?: id,
            version = map["version"] ?: pkg?.third.orEmpty(),
            author = map["author"].orEmpty(),
            description = map["description"].orEmpty(),
            entry = entry,
        )
    }

    /** package name, label and version of an apk file, as the host's PackageManager reads them. */
    private fun packageOf(ctx: Context, apk: File): Triple<String, String, String>? = runCatching {
        val pm = ctx.packageManager
        val pi = pm.getPackageArchiveInfo(apk.absolutePath, 0) ?: return@runCatching null
        val ai = pi.applicationInfo ?: return@runCatching null
        ai.sourceDir = apk.absolutePath
        ai.publicSourceDir = apk.absolutePath
        val label = runCatching { pm.getApplicationLabel(ai).toString() }.getOrNull()
            ?.takeIf { it.isNotBlank() } ?: pi.packageName
        Triple(pi.packageName, label, pi.versionName.orEmpty())
    }.getOrNull()

    // ------------------------------------------------------------------ state changes

    fun setEnabled(ctx: Context, img: GuestImage, plugin: Plugin, on: Boolean) {
        val d = GuestTree.file(VmPaths(ctx, img.id), plugin.dir)
        if (!d.isDirectory) return
        if (on) File(d, "disable").delete() else File(d, "disable").writeText("1\n")
        if (plugin.backend == PluginBackend.XPOSED) refreshXposed(ctx, img)
        // switching a module off puts back what its system/ overlay replaced
        if (!on) restoreOverlay(d, VmPaths(ctx, img.id).root)
    }

    fun uninstall(ctx: Context, img: GuestImage, plugin: Plugin) {
        val paths = VmPaths(ctx, img.id)
        val d = GuestTree.file(paths, plugin.dir)
        restoreOverlay(d, paths.root)
        d.deleteRecursively()
        if (plugin.backend == PluginBackend.XPOSED) refreshXposed(ctx, img)
        // a module's own app stays installed: uninstalling a plug-in is not uninstalling an app
    }

    /** The module's action script, when it has one (KernelSU's "操作" button). */
    fun runAction(vm: GuestVm, plugin: Plugin): String =
        vm.guestShell("/system/bin/sh ${plugin.dir}/action.sh", 120_000)

    /**
     * The same script from the home process: a one-shot shell in the machine's own tree (what
     * the operating page's terminal does), without touching the running machine's services.
     */
    fun runAction(ctx: Context, img: GuestImage, plugin: Plugin): String {
        val paths = VmPaths(ctx, img.id)
        val runner = GuestRunner(paths, img)
        return runner.run(
            listOf("/system/bin/sh", "-c", "export PATH=/system/bin:/system/xbin:\$PATH; /system/bin/sh ${plugin.dir}/action.sh"),
            120_000,
        ).second
    }

    // ------------------------------------------------------------------ boot work

    /**
     * Copies every enabled module's `system/` files into the guest tree.
     *
     * Anything already there is backed up first, in the module's own `.apw-backup/`, and the
     * list of what was copied is kept in `.apw-overlay`: that is what makes the overlay
     * reversible, which an unpacked copy otherwise never is.
     */
    fun applyOverlays(ctx: Context, img: GuestImage, log: (String) -> Unit) {
        val paths = VmPaths(ctx, img.id)
        list(ctx, img).filter { it.enabled }.forEach { p ->
            val d = GuestTree.file(paths, p.dir)
            val sys = File(d, "system")
            if (!sys.isDirectory) return@forEach
            val manifest = LinkedHashSet<String>()
            val manifestFile = File(d, OVERLAY_LIST)
            runCatching { manifestFile.readLines().filter { it.isNotBlank() }.forEach { manifest += it } }
            sys.walkTopDown().filter { it.isFile }.forEach { f ->
                val rel = "system/" + f.relativeTo(sys).path.replace('\\', '/')
                val dest = File(paths.root, rel)
                try {
                    if (dest.exists() && !dest.isDirectory) {
                        val bak = File(d, "$OVERLAY_BACKUP/${rel.removePrefix("system/")}")
                        if (!bak.exists()) {
                            bak.parentFile?.mkdirs()
                            dest.copyTo(bak, overwrite = true)
                        }
                    }
                    dest.parentFile?.mkdirs()
                    f.copyTo(dest, overwrite = true)
                    manifest += rel
                } catch (e: Exception) {
                    log("· ${p.id}: overlay $rel failed: ${e.message}")
                }
            }
            runCatching { manifestFile.writeText(manifest.joinToString("\n")) }
        }
    }

    /** Puts back everything a module's overlay replaced, and removes what it added. */
    fun restoreOverlay(d: File, root: File) {
        val manifestFile = File(d, OVERLAY_LIST)
        if (!manifestFile.isFile) return
        runCatching { manifestFile.readLines() }.getOrDefault(emptyList()).filter { it.isNotBlank() }.forEach { rel ->
            val dest = File(root, rel)
            val bak = File(d, "$OVERLAY_BACKUP/${rel.removePrefix("system/")}")
            runCatching {
                if (bak.isFile) {
                    dest.parentFile?.mkdirs()
                    bak.copyTo(dest, overwrite = true)
                    bak.delete()
                } else if (dest.isFile) {
                    dest.delete()
                }
            }
        }
        runCatching { manifestFile.delete() }
    }

    /** Installs the apks of modules that were added while the machine was off. */
    fun installPendingApks(vm: GuestVm, log: (String) -> Unit) {
        list(vm.ctx, vm.img).forEach { p ->
            val apk = p.apk ?: return@forEach
            if (!p.apkPending) return@forEach
            val d = GuestTree.file(VmPaths(vm.ctx, vm.img.id), p.dir)
            val out = runCatching { vm.installApk("/$apk") }.getOrElse { "install failed: $it" }
            if (out.contains("Failure")) log("· ${p.id}: apk install failed: ${out.trim()}") else {
                File(d, APK_INSTALLED).writeText("1")
                log("· ${p.id}: apk installed")
            }
        }
    }

    /** Rewrites /data/adb/xposed/modules.list and xposed.prop from the enabled modules' apks. */
    fun refreshXposed(ctx: Context, img: GuestImage) {
        val paths = VmPaths(ctx, img.id)
        val lines = list(ctx, img).filter { it.backend == PluginBackend.XPOSED && it.enabled }
            .mapNotNull { it.apk?.let { a -> "/$a" } }
        GuestTree.writeText(paths, "$XPOSED/modules.list", lines.joinToString("\n"))
        GuestTree.writeText(
            paths, "$XPOSED/xposed.prop",
            "version=89\narch=arm\nminApi=14\nmaxApi=25\n"
        )
    }

    /**
     * The class path the Xposed generation's zygote gets, or null when there is nothing to inject.
     *
     * Injection happens where the framework loads its own classes: the bridge and every enabled
     * module's apk are put in front of the zygote's class path, so an installed module's
     * `xposed_init` class is loadable in every process the guest starts afterwards. The bridge
     * itself is `bridge.jar` — the piece that turns those loadable classes into hooks.
     */
    fun xposedClasspath(ctx: Context, img: GuestImage): String? {
        if (backendFor(img) != PluginBackend.XPOSED) return null
        val paths = VmPaths(ctx, img.id)
        val bridge = GuestTree.file(paths, "$XPOSED/bridge.jar")
        val modules = list(ctx, img).filter { it.backend == PluginBackend.XPOSED && it.enabled && it.apk != null }
        if (!bridge.isFile || modules.isEmpty()) return null
        return (listOf("/$XPOSED/bridge.jar") + modules.mapNotNull { it.apk?.let { a -> "/$a" } }).joinToString(":")
    }

    /** True when the Xposed generation has a bridge but no enabled module on the class path. */
    fun xposedBridgePresent(ctx: Context, img: GuestImage): Boolean =
        GuestTree.file(VmPaths(ctx, img.id), "$XPOSED/bridge.jar").isFile
}
