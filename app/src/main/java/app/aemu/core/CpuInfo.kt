package app.aemu.core

import android.os.Build
import java.io.File

object CpuInfo {
    /**
     * Checks if the host device CPU and kernel support 32-bit ARM (AArch32 EL0) execution.
     * On 64-bit-only devices (e.g. Snapdragon 8 Gen 3/4, 8 Elite, Dimensity 9300),
     * this will return false. On devices with AArch32 support, it returns true.
     */
    val hasAArch32: Boolean by lazy {
        Build.SUPPORTED_32_BIT_ABIS.isNotEmpty() || runCatching {
            val f = File("/proc/cpuinfo")
            if (f.canRead()) {
                val t = f.readText()
                t.contains("Features") && (t.contains("swp") || t.contains("half") || t.contains("thumb"))
            } else false
        }.getOrDefault(false)
    }

    /**
     * True if host is running on a 64-bit CPU.
     */
    val is64Bit: Boolean by lazy {
        Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()
    }
}
