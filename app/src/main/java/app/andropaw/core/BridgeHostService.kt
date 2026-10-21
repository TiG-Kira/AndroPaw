package app.andropaw.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.*
import java.io.File

/**
 * 独立进程（:bridge）里的前台服务，承载模拟器的桥进程：binderd / hwbinderd / vndbinderd / glserverd。
 *
 * 为什么单独一个进程：宿主 ROM 的后台清理按 uid 扫"非前台"进程，桥进程作为 :vm 的子进程
 * 一再被扫掉（guest 随之全崩）。让桥进程的父进程自己就是前台服务，它们才真正拥有前台身份。
 * zygote 这类 guest 二进制不适用（子进程不继承 FGS 身份），仍由 :vm 本地承载。
 *
 * 协议（Messenger）：what=1 spawn(Bundle: name/argv/env/cwd/log) → reply arg1=pid
 *                   what=2 killByName(name)
 *                   what=3 isAlive(name) → reply arg1=1/0
 *                   what=4 killAll
 */
class BridgeHostService : Service() {

    private val procs = HashMap<String, java.lang.Process>()

    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                1 -> {
                    val b = msg.data ?: return
                    val name = b.getString("name") ?: return
                    val argv = b.getStringArray("argv") ?: return
                    val env = b.getStringArray("env") ?: emptyArray()
                    val cwd = b.getString("cwd")
                    val logPath = b.getString("log")
                    synchronized(this@BridgeHostService) {
                        procs.remove(name)?.let { runCatching { it.destroyForcibly() } }
                        val pb = ProcessBuilder(*argv).directory(File(cwd ?: filesDir.path))
                        pb.environment().clear()
                        pb.environment().putAll(env.mapNotNull { e ->
                            val i = e.indexOf('=')
                            if (i > 0) e.substring(0, i) to e.substring(i + 1) else null
                        }.toMap())
                        if (!logPath.isNullOrBlank()) runCatching {
                            pb.redirectErrorStream(true)
                            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(File(logPath)))
                        }
                        val p = runCatching { pb.start() }.getOrNull()
                        val pid = p?.let { runCatching { java.lang.Process::class.java.getMethod("pid").invoke(it) as? Int }.getOrNull() } ?: -1
                        if (p != null) procs[name] = p
                        runCatching { msg.replyTo?.send(Message.obtain(null, 1, pid, 0)) }
                    }
                }
                2 -> synchronized(this@BridgeHostService) {
                    val name = msg.data?.getString("name") ?: return
                    procs.remove(name)?.let { runCatching { it.destroyForcibly() } }
                }
                3 -> synchronized(this@BridgeHostService) {
                    val name = msg.data?.getString("name") ?: return
                    val alive = procs[name]?.let { runCatching { it.exitValue(); false }.getOrDefault(true) } ?: false
                    runCatching { msg.replyTo?.send(Message.obtain(null, 3, if (alive) 1 else 0, 0)) }
                }
                4 -> synchronized(this@BridgeHostService) {
                    procs.values.forEach { runCatching { it.destroyForcibly() } }
                    procs.clear()
                }
            }
        }
    }

    private val messenger = Messenger(handler)

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel("bridge-host", "模拟器桥进程", NotificationManager.IMPORTANCE_LOW))
        }
        val n = Notification.Builder(this, "bridge-host")
            .setContentTitle("模拟器运行中").setContentText("正在承载 binder / GL 桥进程")
            .setSmallIcon(android.R.drawable.stat_notify_sync).setOngoing(true).build()
        // 不用带类型的 startForeground：specialUse 是 API 34 的类型常量，非 API 34 宿主上传了会抛
        // IllegalArgumentException 直接把 Service 打崩（表现为"bridge host unavailable"）。
        // API 24+ 一律用两参数版（合法且不崩）；失败的降级为普通常驻 service。
        // API 34+ 才认 specialUse 类型；低版本宿主必须用两参数版，否则抛 IllegalArgumentException
        // 直接把 Service 打崩（表现为 "bridge host unavailable"）。两级兜底保证 Service 一定起得来。
        var ok = runCatching {
            if (android.os.Build.VERSION.SDK_INT >= 34) startForeground(2, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            else startForeground(2, n)
        }.isSuccess
        if (!ok) ok = runCatching { startForeground(2, n) }.isSuccess
        if (!ok) runCatching { startService(android.content.Intent(this, BridgeHostService::class.java)) }
    }

    override fun onBind(intent: Intent?): IBinder = messenger.binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        synchronized(this) { procs.values.forEach { runCatching { it.destroyForcibly() } }; procs.clear() }
        super.onDestroy()
    }
}
