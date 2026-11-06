package app.andropaw.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import app.andropaw.R
import java.io.File

/**
 * One-shot experiment — NOT part of the emulator. It answers a single question before any
 * "host an AMS component process" architecture is attempted:
 *
 *   If a Service process (an AMS ProcessRecord, therefore not a phantom process) replaces its
 *   own image with Os.execve(), does the host AMS kill it when the ART binder connection dies?
 *
 * The child is a shell that appends a heartbeat to filesDir/probe.log every two seconds. Let it
 * run for five minutes and open "experiment result" in Settings:
 *   - a continuous heartbeat means the pid survived execve and AMS did not reap it, so the
 *     "borrow an AMS process" idea (a pool of :gN Service slots execve'd into guest processes)
 *     is worth building;
 *   - a heartbeat that stops after a line or two, or an "execve FAILED" line, means AMS reaped
 *     it (or SELinux/seccomp refused the exec) and that whole direction is closed.
 *
 * Nothing here changes emulator behaviour. The Service is started only from the Settings page.
 */
class HostProcessProbe : Service() {

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, flags: Int, id: Int): Int {
        val dir = i?.getStringExtra(EXTRA_DIR) ?: return START_NOT_STICKY
        val log = File(dir, "probe.log")
        val pid = android.os.Process.myPid()
        runCatching { File(dir, "probe.pid").writeText(pid.toString()) }
        runCatching {
            log.appendText("before execve pid=$pid fds=${File("/proc/self/fd").listFiles()?.size ?: -1}\n")
        }

        // A foreground service keeps the oom adj out of the experiment: if the process dies,
        // it is because of execve / the AMS binder relationship, not the low-memory killer.
        runCatching {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "execve probe", NotificationManager.IMPORTANCE_LOW))
            startForeground(NOTE_ID, Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_vm)
                .setContentTitle("execve probe")
                .setContentText("pid $pid")
                .setOngoing(true)
                .build())
        }

        val script = "i=0; while :; do i=\$((i+1)); echo \"\$i \$(date +%s)\" >> '$dir/probe.log'; sleep 2; done"
        val argv = arrayOf("/system/bin/sh", "sh", "-c", script)
        val env = arrayOf("PATH=/system/bin:/system/xbin:/vendor/bin")
        try {
            android.system.Os.execve(argv[0], argv, env)
        } catch (e: Exception) {
            runCatching { log.appendText("execve FAILED: ${e.message}\n") }
        }
        // execve only returns on failure
        stopSelf()
        return START_NOT_STICKY
    }

    companion object {
        private const val EXTRA_DIR = "dir"
        private const val CHANNEL = "probe"
        private const val NOTE_ID = 7

        /** Start a fresh run; clears the previous heartbeat so the result is unambiguous. */
        fun start(ctx: Context) {
            val dir = ctx.filesDir
            runCatching {
                File(dir, "probe.log").writeText("")
                File(dir, "probe.pid").delete()
            }
            runCatching {
                ctx.startForegroundService(Intent(ctx, HostProcessProbe::class.java)
                    .putExtra(EXTRA_DIR, dir.absolutePath))
            }
        }

        /** Snapshot of the run: pid, whether it is alive, its oom adj and the last heartbeats. */
        fun report(ctx: Context): String {
            val dir = ctx.filesDir
            val lines = runCatching { File(dir, "probe.log").readLines() }.getOrDefault(emptyList())
            val pid = runCatching { File(dir, "probe.pid").readText().trim() }.getOrNull()
            val alive = pid?.let { File("/proc/$it").exists() } ?: false
            val adj = pid?.let {
                runCatching { File("/proc/$it/oom_score_adj").readText().trim() }.getOrDefault("?")
            } ?: "?"
            return buildString {
                appendLine("pid: ${pid ?: "none"}   alive: $alive   adj: $adj")
                appendLine("log lines: ${lines.size}   (one heartbeat every 2 s)")
                appendLine("--- last 20 lines ---")
                lines.takeLast(20).forEach { appendLine(it) }
                if (lines.isEmpty()) appendLine("(no heartbeat yet — wait a few seconds)")
            }.trimEnd()
        }
    }
}
