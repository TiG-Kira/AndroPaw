package app.andropaw.core

/**
 * Which superuser implementation a machine's generation is rooted with.
 *
 * The four are the ones the real world used for each generation, and each one leaves the
 * artefacts its generation expects to find: where `su` lives, which daemon runs, which tool
 * name a module or a root check calls. The privilege behind them is the same on all four —
 * see [RootManager] — but a machine of a given Android version must be recognisable as
 * rooted by the things that look for it.
 */
enum class RootBackend(val key: String, val label: String, val note: String) {
    /** 2.x–3.x: the age of a `su` binary in /system/xbin and `ro.secure=0`. */
    SUPERUSER("superuser", "Superuser", "2.x–3.x"),

    /** 4.x–7.x: SuperSU — `su`, `daemonsu` and `install-recovery.sh`. */
    SUPERSU("supersu", "SuperSU", "4.x–7.x"),

    /** 8.0–11: Magisk — magiskd, /data/adb/magisk, modules. */
    MAGISK("magisk", "Magisk", "8.0–11"),

    /** 12 and newer: KernelSU — ksud, /data/adb/ksu, modules. */
    KERNELSU("kernelsu", "KernelSU", "12+");

    companion object {
        fun forApi(api: Int): RootBackend = when {
            api >= 31 -> KERNELSU
            api >= 26 -> MAGISK
            api >= 14 -> SUPERSU
            else -> SUPERUSER
        }
    }
}
