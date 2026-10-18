package app.aemu.core

import android.system.Os
import app.aemu.importer.ChannelSource
import app.aemu.importer.ErofsImage
import app.aemu.importer.Ext4Reader
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.util.zip.ZipFile

/**
 * APEX packages as files (Android 11+ system images: /system/apex/<package>.apex).
 *
 * An .apex is a zip whose apex_payload.img is a file system image with the module's files; on a device apexd attaches
 * it to a loop device and mounts it at /apex/<module name>. Nothing can be mounted for the guest, so the payload is
 * unpacked into <root>/apex/<module name>, the same place a flattened package is moved to (TreeFixer.fixApex), and
 * the package file gives way to a link to that directory: the system then sees a flattened package, which is what
 * ro.apex.updatable=false tells apexd and the package manager to expect.
 */
object ApexUnpacker {
    /** Unpacks every *.apex of [sysApex]; returns the number of modules put in place. */
    fun unpackAll(root: File, sysApex: File, log: (String) -> Unit): Int {
        val apexDir = File(root, "apex").apply { mkdirs() }
        val parked = File(root, "system/.aemu-parked").apply { mkdirs() }
        var done = 0
        sysApex.listFiles()?.filter { it.isFile && (it.name.endsWith(".apex") || it.name.endsWith(".capex")) }?.sortedBy { it.name }?.forEach { pkg ->
            runCatching {
                val name = unpack(pkg, apexDir)
                if (name == null) {         // the module is there already (another package of it, or its flattened copy)
                    pkg.renameTo(File(parked, "${sysApex.relativeTo(root).path.replace('/', '#')}#${pkg.name}"))
                    return@runCatching
                }
                // the package file out of the way, a "flattened package" of the same base name in its place
                val flat = File(sysApex, pkg.name.removeSuffix(".apex").removeSuffix(".capex"))
                pkg.renameTo(File(parked, "${sysApex.relativeTo(root).path.replace('/', '#')}#${pkg.name}"))
                val up = "../".repeat(sysApex.relativeTo(root).path.count { it == '/' } + 1)
                if (!flat.exists()) Os.symlink("${up}apex/$name", flat.path)
                done++
            }.onFailure { log("apex ${pkg.name}: ${it.message}") }
        }
        return done
    }

    /** Unpacks one package into [apexDir]/<module name>; null when another package already provides that module. */
    private fun unpack(pkg: File, apexDir: File): String? {
        ZipFile(pkg).use { zip ->
            // a compressed package (12+, "<name>_compressed.apex" or .capex): the real one is inside, whole
            zip.getEntry("original_apex")?.let { inner ->
                val orig = File(apexDir, ".original.tmp")
                try {
                    zip.getInputStream(inner).use { i -> orig.outputStream().use { o -> i.copyTo(o, 1 shl 20) } }
                    return unpack(orig, apexDir)
                } finally {
                    orig.delete()
                }
            }
            val manifest = zip.getEntry("apex_manifest.pb")?.let { zip.getInputStream(it).use { s -> s.readBytes() } }
            val name = manifest?.let(::moduleName) ?: pkg.name.removeSuffix(".apex").removeSuffix(".debug").removeSuffix(".release")
            val home = File(apexDir, name)
            if (File(home, "apex_manifest.pb").isFile || File(home, "apex_manifest.json").isFile) return null
            val payload = zip.getEntry("apex_payload.img") ?: error("no apex_payload.img")
            val img = File(apexDir, ".payload.tmp")
            try {
                zip.getInputStream(payload).use { i -> img.outputStream().use { o -> i.copyTo(o, 1 shl 20) } }
                FileChannel.open(img.toPath(), StandardOpenOption.READ).use { ch ->
                    val src = ChannelSource(ch)
                    val links = ArrayList<Pair<String, File>>()
                    home.mkdirs()
                    // (17: the payload of com.android.virt is EROFS)
                    if (!Ext4Reader.probe(src)) (runCatching { ErofsImage(src) }.getOrNull() ?: error("payload is neither ext4 nor EROFS"))
                        .walk { path, isDir, link, mode, _, data ->
                            val f = File(home, path.trimStart('/'))
                            when {
                                isDir -> f.mkdirs()
                                link != null -> links.add(link to f)
                                data != null -> {
                                    f.parentFile?.mkdirs()
                                    data().use { i -> f.outputStream().use { o -> i.copyTo(o, 1 shl 20) } }
                                    f.setReadable(true, false)
                                    if (mode and 0x49 != 0) f.setExecutable(true, false)
                                }
                            }
                        }
                    else Ext4Reader(src).let { fs -> fs.walk { path, node ->
                        val f = File(home, path)
                        when {
                            node.isDir -> f.mkdirs()
                            node.isLink -> links.add(fs.linkTarget(node) to f)
                            node.isFile -> {
                                f.parentFile?.mkdirs()
                                f.outputStream().buffered(1 shl 20).use { fs.copy(node, it) }
                                f.setReadable(true, false)
                                if (node.perm and 0x49 != 0) f.setExecutable(true, false)
                            }
                        }
                    } }
                    for ((target, f) in links) runCatching {
                        f.parentFile?.mkdirs()
                        // a link to an absolute guest path: relative to the image root, where the host resolves it
                        val depth = f.relativeTo(apexDir.parentFile!!).path.count { it == '/' }
                        val t = if (target.startsWith("/")) "../".repeat(depth) + target.trimStart('/') else target
                        if (!f.exists()) Os.symlink(t, f.path)
                    }
                }
            } catch (t: Throwable) {
                ImageStore.wipe(home)       // a half-unpacked module must not pass for a complete one
                throw t
            } finally {
                img.delete()
            }
            if (manifest != null && !File(home, "apex_manifest.pb").isFile) File(home, "apex_manifest.pb").writeBytes(manifest)
            return name
        }
    }

    /** apex_manifest.pb: field 1 (a string) is the module name */
    private fun moduleName(pb: ByteArray): String? {
        if (pb.size < 3 || pb[0].toInt() != 0x0A) return null
        val len = pb[1].toInt() and 0xff
        if (len == 0 || len > 127 || 2 + len > pb.size) return null
        return String(pb, 2, len, Charsets.UTF_8).takeIf { n -> n.all { it.isLetterOrDigit() || it == '.' || it == '_' } }
    }
}
