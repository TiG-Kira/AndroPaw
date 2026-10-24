package app.andropaw.core

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.*
import org.junit.Test

class GuestProcessesTest {
    private class FakeProcess(private var alive: Boolean, private val id: Long = 12345) : Process() {
        override fun getOutputStream() = ByteArrayOutputStream()
        override fun getInputStream() = ByteArrayInputStream(byteArrayOf())
        override fun getErrorStream() = ByteArrayInputStream(byteArrayOf())
        override fun waitFor(): Int = 137
        override fun exitValue(): Int = if (alive) throw IllegalThreadStateException() else 137
        override fun destroy() { alive = false }
        override fun isAlive() = alive
        private val pid: Long = id
    }

    @Test fun readsLongPidWithoutDependingOnPublicApi() {
        assertEquals(12345, GuestProcesses.pid(FakeProcess(true)))
        assertNull(GuestProcesses.pid(FakeProcess(true, Long.MAX_VALUE)))
    }

    @Test fun exitIsConsumedOnlyOnce() {
        val dead = FakeProcess(false)
        val processes = mutableMapOf<String, Process>("zygote" to dead)
        assertSame(dead, GuestProcesses.takeExited(processes, "zygote"))
        assertNull(GuestProcesses.takeExited(processes, "zygote"))
    }

    @Test fun replacementSurvivesStaleExitNotification() {
        val processes = mutableMapOf<String, Process>("zygote" to FakeProcess(false))
        GuestProcesses.takeExited(processes, "zygote")
        val replacement = FakeProcess(true)
        processes["zygote"] = replacement
        assertNull(GuestProcesses.takeExited(processes, "zygote"))
        assertSame(replacement, processes["zygote"])
    }

    @Test fun interpreterPathsAndArgumentsAreNotAppIdentities() {
        assertFalse(GuestProcesses.appCandidate("/data/app/app.andropaw/lib/arm64/libaemu64.so\u0000/system/bin/app_process64"))
        assertFalse(GuestProcesses.appCandidate("zygote\u0000com.example.fake"))
        assertFalse(GuestProcesses.appCandidate("system_server"))
        assertTrue(GuestProcesses.appCandidate("com.example.app\u0000ignored"))
        assertTrue(GuestProcesses.appCandidate("com.example.app:worker"))
    }
}
