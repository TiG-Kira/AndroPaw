package app.aemu.core

import android.system.Os
import app.aemu.importer.BootImage
import java.io.File
import java.io.RandomAccessFile

/**
 * Recovery mode: the ramdisk of a recovery.img (stock, TWRP, OrangeFox, CWM…) unpacked into <image>/recovery.
 * The VM runs its /sbin/recovery under translator (or natively on arm64/aarch32) sharing the framebuffer and touch.
 */
object RecoveryImage {
    fun dir(paths: VmPaths) = File(paths.dir, "recovery")
    fun installed(paths: VmPaths) = File(dir(paths), "sbin/recovery").isFile
    fun fb(paths: VmPaths) = File(dir(paths), "dev/graphics/fb0")

    fun isElf64(paths: VmPaths): Boolean = File(dir(paths), "sbin/recovery").let { f ->
        f.isFile && runCatching {
            f.inputStream().use {
                val b = ByteArray(5)
                it.read(b) == 5 && b[0] == 0x7f.toByte() && b[1] == 'E'.code.toByte() && b[2] == 'L'.code.toByte() && b[3] == 'F'.code.toByte() && b[4] == 2.toByte()
            }
        }.getOrDefault(false)
    }

    /** Unpacks a recovery (or boot) image; false when it holds no /sbin/recovery. */
    fun install(paths: VmPaths, image: ByteArray, log: (String) -> Unit): Boolean {
        val rd = runCatching { BootImage.ramdisk(image) }.getOrNull()
        if (rd.isNullOrEmpty() || rd.none { it.name == "sbin/recovery" }) { log("recovery: image has no /sbin/recovery"); return false }
        val out = dir(paths)
        // never deleteRecursively: system/, data/, sdcard/ in it are links to the firmware's partitions and card
        ImageStore.wipe(out)
        out.mkdirs()
        val base = out.canonicalPath
        for (e in rd) {
            val n = e.name.trimStart('/')
            if (n.isEmpty() || n == ".") continue
            val f = File(out, n)
            if (!f.canonicalPath.startsWith(base)) continue
            when (e.mode and 0xF000) {
                0x4000 -> f.mkdirs()
                0xA000 -> { f.parentFile?.mkdirs(); runCatching { Os.symlink(String(e.data), f.path) } }
                0x8000 -> {
                    f.parentFile?.mkdirs(); f.writeBytes(e.data)
                    if (e.mode and 0x49 != 0) f.setExecutable(true, false)
                    f.setReadable(true, false)
                }
            }
        }
        val name = runCatching { File(out, "default.prop").readText() }.getOrDefault("")
            .let { Regex("twrp", RegexOption.IGNORE_CASE).containsMatchIn(it) }.let { if (it) "TWRP" else "" }
        log("recovery: installed ${rd.size} files ${name}".trim())
        return true
    }

    /** Runtime pieces the ramdisk lacks: device nodes qemu emulates, mount points, the shared framebuffer. */
    fun prepare(paths: VmPaths, sdcard: File?, settings: VmSettings? = null) {
        val r = dir(paths)
        for (d in listOf("dev/graphics", "dev/input", "tmp", "cache", "data", "system", "sdcard", "proc", "sys", "etc")) File(r, d).mkdirs()
        // no tty0: minui gives up on graphics when it opens one but KDSETMODE fails; without it, it skips the step
        File(r, "dev/tty0").delete()

        // Sizing framebuffer: TWRP and minui expect at least 2 pages of double buffering.
        val fb = fb(paths)
        val (w, h) = runCatching { File(paths.root, "dhd.fbgeom").readText().trim().split(Regex("\\s+")).map { it.toInt() } }
            .getOrNull()?.takeIf { it.size >= 2 }?.let { it[0] to it[1] } ?: ((settings?.width ?: 720) to (settings?.height ?: 1280))
        val minSize = (w.toLong() * h * 4 * 2).coerceAtLeast(paths.fb.length())
        runCatching {
            if (java.nio.file.Files.isSymbolicLink(fb.toPath())) fb.delete()
            fb.parentFile?.mkdirs()
            if (!fb.exists() || fb.length() < minSize) {
                RandomAccessFile(fb, "rw").use { it.setLength(minSize) }
            }
        }

        // dynamic recoveries (TWRP, OrangeFox) get librecshim.so preloaded: mount points look mounted, evdev ioctls answered
        runCatching {
            for (lib in listOf("librecshim.so", "libaemushim.so")) {
                EngineAssets.open(paths.ctx, "engines/common/$lib").use { i -> File(r, "sbin/$lib").outputStream().use { i.copyTo(it) } }
                File(r, "sbin/$lib").setReadable(true, false)
            }
        }
        File(r, "dev/aemu_power").let { if (!it.exists()) it.createNewFile() }
        runCatching { File(r, "dhd.fbgeom").writeText("$w $h\n") }

        // Virtual mount table for TWRP/OrangeFox: redirected from /proc/mounts and /proc/self/mounts
        val mounts = """
rootfs / rootfs rw,relatime 0 0
tmpfs /dev tmpfs rw,nosuid,relatime,mode=755 0 0
devpts /dev/pts devpts rw,relatime,mode=600 0 0
proc /proc proc rw,relatime 0 0
sysfs /sys sysfs rw,relatime 0 0
/dev/block/bootdevice/by-name/system /system ext4 rw,relatime 0 0
/dev/block/bootdevice/by-name/userdata /data ext4 rw,relatime 0 0
/dev/block/bootdevice/by-name/cache /cache ext4 rw,relatime 0 0
/dev/block/bootdevice/by-name/boot /boot emmc rw,relatime 0 0
/dev/block/bootdevice/by-name/recovery /recovery emmc rw,relatime 0 0
/data/media/0 /sdcard vfat rw,relatime 0 0
""".trimIndent()
        runCatching { File(r, "tmp/aemu_mounts").writeText(mounts + "\n") }

        // TWRP theme density & flags in default.prop
        runCatching {
            val defProp = File(r, "default.prop")
            val existing = if (defProp.isFile) defProp.readText() else ""
            val density = settings?.density?.takeIf { it > 0 } ?: 320
            val sb = java.lang.StringBuilder(existing)
            if (!existing.contains("ro.sf.lcd_density")) sb.append("\nro.sf.lcd_density=$density\n")
            if (!existing.contains("ro.twrp.boot")) sb.append("ro.twrp.boot=1\n")
            if (!existing.contains("ro.boot.recovery")) sb.append("ro.boot.recovery=1\n")
            if (!existing.contains("ro.product.model")) sb.append("ro.product.model=AEmulator Guest\n")
            defProp.writeText(sb.toString())
        }

        // the system's partitions and memory card, for recoveries that browse or flash files
        for ((name, target) in listOf("system" to File(paths.root, "system"), "data" to File(paths.root, "data"), "cache" to File(paths.root, "cache"))) {
            val link = File(r, name)
            if (!java.nio.file.Files.isSymbolicLink(link.toPath()) && (!link.exists() || (link.isDirectory && !hasFiles(link)))) {
                ImageStore.wipe(link); runCatching { Os.symlink(target.absolutePath, link.path) }
            }
        }
        if (sdcard != null) {
            val link = File(r, "sdcard")
            if (link.isDirectory && link.list().isNullOrEmpty()) { link.delete(); runCatching { Os.symlink(sdcard.absolutePath, link.path) } }
        }
    }

    /** a ramdisk mount point may hold empty folders (TWRP: system/bin); it still gets linked to the partition */
    private fun hasFiles(d: File): Boolean = d.listFiles().orEmpty().any {
        java.nio.file.Files.isSymbolicLink(it.toPath()) || !it.isDirectory || hasFiles(it)
    }
}
