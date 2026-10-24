package app.andropaw.core

import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GuestLogTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun findsMatchesAcrossChunkBoundary() {
        val root = temp.newFolder()
        val log = File(root, "dev/log/main")
        checkNotNull(log.parentFile).mkdirs()
        log.writeText("x".repeat(65534) + "needle" + "y".repeat(65536), Charsets.ISO_8859_1)
        assertTrue(GuestLog.contains(root, "needle"))
        assertFalse(GuestLog.contains(root, "absent"))
        assertFalse(GuestLog.contains(root))
        assertTrue(GuestLog.contains(root, ""))
    }

    @Test fun missingLogIsSafe() {
        assertFalse(GuestLog.contains(temp.newFolder(), "needle"))
    }
}
