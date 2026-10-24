package app.andropaw.core

import java.io.File
import java.io.IOException

/** Canonical containment also checks existing parent symlinks and sibling-prefix escapes. */
object SafePaths {
    fun contains(root: File, file: File): Boolean =
        file.canonicalFile.toPath().startsWith(root.canonicalFile.toPath())

    fun resolve(root: File, name: String): File {
        val file = File(root, name)
        if (File(name).isAbsolute || !contains(root, file) || file.canonicalFile == root.canonicalFile) {
            throw IOException("Unsafe archive path: $name")
        }
        return file
    }

    /** Absolute guest links must be relative on the host, never point into the phone's root. */
    fun linkTarget(root: File, link: File, target: String): String {
        val parent = link.parentFile ?: throw IOException("Archive link has no parent")
        val relative = if (target.startsWith('/')) {
            parent.toPath().relativize(File(root, target.trimStart('/')).toPath()).toString()
        } else target
        if (File(relative).isAbsolute || !contains(root, File(parent, relative))) {
            throw IOException("Unsafe archive link: ${link.name} -> $target")
        }
        return relative
    }
}
