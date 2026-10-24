package app.andropaw.core

import java.io.File
import java.io.IOException
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SafePathsTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun acceptsNestedFiles() {
        val root = temp.newFolder("root")
        assertEquals(File(root, "system/build.prop"), SafePaths.resolve(root, "system/build.prop"))
    }

    @Test fun rejectsTraversalAndSiblingPrefix() {
        val root = temp.newFolder("root")
        for (name in listOf("../root-other/file", "../../file", ".", root.absolutePath)) {
            assertThrows(IOException::class.java) { SafePaths.resolve(root, name) }
        }
        assertFalse(SafePaths.contains(root, File(root.parentFile, "root-other/file")))
    }

    @Test fun convertsGuestAbsoluteLinksAndRejectsEscapingLinks() {
        val root = temp.newFolder("root")
        val link = File(root, "sbin/sh")
        val target = SafePaths.linkTarget(root, link, "/system/bin/sh")
        assertEquals(File(root, "system/bin/sh").canonicalFile, File(link.parentFile, target).canonicalFile)
        assertEquals("..", SafePaths.linkTarget(root, link, ".."))
        assertThrows(IOException::class.java) { SafePaths.linkTarget(root, link, "../../outside") }
    }
}
