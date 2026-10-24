package app.andropaw.core

/** Process handles, rather than service names, identify one generation of a guest service. */
internal object GuestProcesses {
    private val appName = Regex("[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z][A-Za-z0-9_]*)+(?::[A-Za-z0-9_.]+)?")

    fun appCandidate(cmdline: String): Boolean = appName.matches(cmdline.substringBefore('\u0000'))

    fun pid(process: Process): Int? {
        val publicPid = runCatching {
            (Process::class.java.getMethod("pid").invoke(process) as? Number)?.toLong()
        }.getOrNull()
        if (publicPid != null && publicPid in 1..Int.MAX_VALUE.toLong()) return publicPid.toInt()
        var type: Class<*>? = process.javaClass
        while (type != null) {
            val current = type
            val value = runCatching {
                current.getDeclaredField("pid").let { field ->
                    field.isAccessible = true
                    (field.get(process) as? Number)?.toLong()
                }
            }.getOrNull()
            if (value != null && value in 1..Int.MAX_VALUE.toLong()) return value.toInt()
            type = current.superclass
        }
        return null
    }

    /** Consume each exit once; never remove a replacement that is already alive. */
    fun takeExited(processes: MutableMap<String, Process>, name: String): Process? = synchronized(processes) {
        val process = processes[name] ?: return@synchronized null
        if (process.isAlive) return@synchronized null
        processes.remove(name)
        process
    }
}
