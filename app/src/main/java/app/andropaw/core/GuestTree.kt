package app.andropaw.core

import android.content.Context
import java.io.File

/**
 * The guest's own file system, written from the host.
 *
 * The guest runs on the host's kernel with its root at [VmPaths.root], so a guest path is a
 * host path with the firmware tree in front of it. That is how the management layer installs
 * anything — the superuser gate, the module store, an overlay — without a shell, without adb,
 * and whether the machine is booted or not.
 */
object GuestTree {
    /** "/data/adb/andropaw" → `<image>/root/data/adb/andropaw` */
    fun file(paths: VmPaths, guestPath: String): File =
        File(paths.root, guestPath.trimStart('/'))

    fun exists(paths: VmPaths, guestPath: String): Boolean = file(paths, guestPath).exists()

    fun writeText(paths: VmPaths, guestPath: String, text: String, executable: Boolean = false): File {
        val f = file(paths, guestPath)
        f.parentFile?.mkdirs()
        f.writeText(text)
        markExecutable(f, executable)
        return f
    }

    fun writeBytes(paths: VmPaths, guestPath: String, bytes: ByteArray, executable: Boolean = false): File {
        val f = file(paths, guestPath)
        f.parentFile?.mkdirs()
        f.writeBytes(bytes)
        markExecutable(f, executable)
        return f
    }

    /** Copies a host file into the guest tree, keeping its name. */
    fun install(paths: VmPaths, guestPath: String, source: File, executable: Boolean = false): File {
        val f = file(paths, guestPath)
        f.parentFile?.mkdirs()
        source.copyTo(f, overwrite = true)
        markExecutable(f, executable)
        return f
    }

    fun delete(paths: VmPaths, guestPath: String) {
        runCatching { file(paths, guestPath).delete() }
    }

    fun deleteTree(paths: VmPaths, guestPath: String) {
        runCatching { file(paths, guestPath).deleteRecursively() }
    }

    /** Size of a guest directory, for the module and machine summaries. */
    fun sizeOf(dir: File): Long = runCatching {
        dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }.getOrDefault(0L)

    /** A script from res/raw, byte for byte. */
    fun script(ctx: Context, resId: Int): ByteArray =
        ctx.resources.openRawResource(resId).use { it.readBytes() }

    private fun markExecutable(f: File, executable: Boolean) {
        if (!executable) return
        // The guest runs binaries out of this tree as the app's uid, so the execute bit has to
        // be on the host file itself; setExecutable can fail on a filesystem without it.
        runCatching { f.setExecutable(true, false) }
    }
}
