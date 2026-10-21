package app.andropaw.agent

import android.content.Context
import android.graphics.Bitmap
import app.andropaw.core.GuestImage
import app.andropaw.core.GuestRunner
import app.andropaw.core.ImageStore
import app.andropaw.core.RecoveryImage
import app.andropaw.core.Sdcard
import app.andropaw.core.VmPaths
import app.andropaw.core.VmSession
import app.andropaw.ui.LibraryModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.Base64

/**
 * AgentPaw for AndroPaw 的执行通道桥接层。
 *
 * 与 AgentPaw 的根本差异在这里：APAnP 不管控手机宿主端，所有能力只作用于模拟器内的
 * Android 系统。通道有两条，都不需要任何宿主端权限：
 *
 *  1. **QEMU 通道**（[guestShell]）——和 :vm 进程里 GuestVm 完全相同的机制：直接以
 *     qemu-user 启动 guest 里的 /system/bin/sh。am/pm/settings/input 等框架工具经
 *     aemu-stubs 的 Run 包装后能正常返回。
 *  2. **fb0 直读**（[screenshot]）——framebuffer 是普通文件，任何进程都能映射读取，
 *     不依赖 guest 的截屏能力（老固件没有 screencap，GPU 桥接帧也拿不到）。
 *
 * 电源/恢复等机器级操作经 guest 的 /dev/aemu_power 请求文件交给 :vm 的 watchdog，
 * 数据擦除等宿主侧操作直接走 [LibraryModel]。
 */
class AgentVmBridge(
    val ctx: Context,
    private val model: LibraryModel?,
) {
    /** 目标机器：默认当前运行中的那台 */
    @Volatile
    var imageId: String? = null

    /**
     * :vm 进程内的捷径：把 guest 命令直接交给活着的 GuestVm（它的 runner 已热身、
     * 工具包装已就绪），不再另起一次 qemu-user。主进程不设它，走自建的 runner。
     */
    @Volatile
    var inlineShell: ((cmd: String, timeoutMs: Long) -> Pair<Int, String>)? = null

    private fun image(): GuestImage? {
        val id = imageId ?: VmSession.runningId(ctx) ?: return null
        return ImageStore.get(ctx, id)
    }

    fun targetName(): String? = image()?.name

    /** 目标机器的 guest 分辨率（归一化坐标换算用） */
    suspend fun guestSize(): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val img = image() ?: return@withContext (1080 to 1920)
        img.settings.width to img.settings.height
    }

    fun isMachineRunning(): Boolean = VmSession.runningId(ctx) != null

    // ------------------------------------------------------------------ guest shell

    private fun toolWrappers(paths: VmPaths): String {
        val stubs = "/system/framework/aemu-stubs.jar"
        if (!File(paths.root, stubs.removePrefix("/")).isFile) return ""
        val sb = StringBuilder()
        File(paths.root, "system/bin").listFiles()?.forEach { f ->
            if (!f.isFile || f.length() > 8_000 || !f.name.matches(Regex("[a-z][a-z0-9_]*"))) return@forEach
            val text = runCatching { f.readText(Charsets.ISO_8859_1) }.getOrNull() ?: return@forEach
            val cls = Regex("app_process\\S*\\s+\\S+\\s+([A-Za-z_][\\w.$]*\\.[A-Z][\\w$]*)").find(text)?.groupValues?.get(1) ?: return@forEach
            val jar = Regex("framework/([\\w.-]+\\.jar)").find(text)?.groupValues?.get(1) ?: return@forEach
            sb.append("${f.name}() { CLASSPATH=/system/framework/$jar:$stubs app_process /system/bin app.andropaw.stub.Run $cls \"\$@\"; }; ")
        }
        return sb.toString()
    }

    /**
     * 在模拟器内的 Android 里执行一条 shell 命令，返回 (退出码, 合并输出)。
     * 超时强制杀死；命令经 qemu-user 单次进程执行，与 :vm 里跑 guest 服务同一机制。
     */
    suspend fun guestShell(cmd: String, timeoutMs: Long = 60_000): Pair<Int, String> = withContext(Dispatchers.IO) {
        inlineShell?.let { return@withContext it(cmd, timeoutMs) }
        val img = image() ?: return@withContext (-1 to "No target machine: start a machine first")
        val paths = VmPaths(ctx, img.id)
        if (!File(paths.root, "system/bin/sh").isFile) {
            return@withContext (-1 to "Firmware tree is not ready")
        }
        val prefix = "export PATH=/system/bin:/system/xbin:\$PATH; ${toolWrappers(paths)}"
        val runner = GuestRunner(paths, img)
        runner.run(listOf("/system/bin/sh", "-c", prefix + cmd), timeoutMs)
    }

    // ------------------------------------------------------------------ screenshot

    /**
     * 读 fb0 拿当前画面，缩放到最长边 [maxDim] 后编码成 JPEG base64。
     * 返回 null 表示还没有画面（机器刚起/已停）。
     */
    suspend fun screenshot(maxDim: Int = 1024, quality: Int = 80): String? = withContext(Dispatchers.IO) {
        val img = image() ?: return@withContext null
        val paths = VmPaths(ctx, img.id)
        val f = paths.fb
        if (!f.isFile) return@withContext null
        val w = img.settings.width
        val h = img.settings.height
        // 帧缓冲每像素字节数不猜：拿文件长度反推（QEMU 引擎的 fb 尺寸/格式随固件变，
        // 固定按 settings 的 w*h 会永远 miss 导致截图拿不到）
        val len = f.length()
        val candidates = mutableListOf<Int>()
        for (bpp in listOf(2, 4)) {
            val frame = w.toLong() * h * bpp
            if (len >= frame) candidates.add(bpp)
        }
        if (candidates.isEmpty() && len >= w.toLong() * h) candidates.add(2) // 兜底：长度不齐也试一把
        for (bytes in candidates) {
            val pixels = w.toLong() * h * bytes
            if (f.length() < pixels) continue
            val bmp = runCatching {
                RandomAccessFile(f, "r").use { raf ->
                    val span = if (f.length() >= pixels + 8) pixels + 8 else pixels
                    val buf: MappedByteBuffer = raf.channel.map(FileChannel.MapMode.READ_ONLY, 0, span)
                    val b = Bitmap.createBitmap(w, h, if (bytes == 2) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888)
                    buf.position(0)
                    if (bytes == 2) {
                        buf.limit(pixels.toInt())
                        b.copyPixelsFromBuffer(buf)
                    } else {
                        val n = w * h
                        val px = IntArray(n)
                        buf.order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(px, 0, n)
                        for (k in 0 until n) px[k] = px[k] or -0x1000000
                        b.setPixels(px, 0, w, 0, 0, w, h)
                    }
                    b
                }
            }.getOrNull() ?: continue
            val scale = maxDim.toFloat() / maxOf(w, h)
            val out = if (scale < 1f) Bitmap.createScaledBitmap(bmp, (w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1), true) else bmp
            val baos = ByteArrayOutputStream()
            out.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            if (out !== bmp) out.recycle()
            bmp.recycle()
            return@withContext Base64.getEncoder().encodeToString(baos.toByteArray())
        }
        null
    }

    // ------------------------------------------------------------------ 机器管理

    /**
     * 电源/恢复请求：写 guest 的 /dev/aemu_power，:vm 的 watchdog 每 3 秒收取并执行。
     * 值："reboot" / "reboot,recovery" / "shutdown"。
     */
    suspend fun powerRequest(value: String, waitStopMs: Long = 30_000): String = withContext(Dispatchers.IO) {
        val img = image() ?: return@withContext "No target machine"
        val paths = VmPaths(ctx, img.id)
        val f = File(paths.root, "dev/aemu_power")
        runCatching { f.writeText(value); f.setWritable(true, false) }
            .onFailure { return@withContext "Cannot reach the machine: ${it.message}" }
        // 等机器真的停下（会话标记消失），后续动作（如擦数据）才安全
        val until = System.currentTimeMillis() + waitStopMs
        while (System.currentTimeMillis() < until) {
            if (VmSession.current(ctx) == null) return@withContext "ok"
            Thread.sleep(500)
        }
        "request sent (${value}); the machine is still shutting down"
    }

    /** 擦除机器数据（宿主侧直接删数据目录；机器必须先停） */
    suspend fun wipeData(): String = withContext(Dispatchers.IO) {
        val img = image() ?: return@withContext "No target machine"
        if (VmSession.current(ctx) != null) return@withContext "The machine is still running — power it off first"
        runCatching {
            app.andropaw.core.ImageStore.resetData(ctx, img.id)
            model?.refresh()
            "Data wiped for ${img.name}"
        }.getOrElse { "Wipe failed: ${it.message}" }
    }

    /**
     * 安装 Recovery：从 [sourcePath]（宿主路径或 sdcard  guest 路径）读镜像并写入机器。
     * 安装后重启即可进入。
     */
    suspend fun installRecovery(sourcePath: String): String = withContext(Dispatchers.IO) {
        val img = image() ?: return@withContext "No target machine"
        val paths = VmPaths(ctx, img.id)
        val file = resolveHostFile(sourcePath, paths) ?: return@withContext "Recovery image not found: $sourcePath"
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return@withContext "Cannot read $sourcePath"
        val ok = runCatching { RecoveryImage.install(paths, bytes) { } }.getOrDefault(false)
        if (ok) "Recovery installed for ${img.name}; reboot to recovery to use it" else "Recovery install failed (not a valid recovery image?)"
    }

    /** 把 sdcard 的 guest 路径映射回宿主文件 */
    private fun resolveHostFile(path: String, paths: VmPaths): File? {
        val direct = File(path)
        if (direct.isFile) return direct
        val sd = Sdcard.hostDir(paths) ?: return null
        val img = image() ?: return null
        for (pre in Sdcard.guestPaths(img)) {
            val g = "/$pre"
            if (path == g) return sd
            if (path.startsWith("$g/")) return File(sd, path.removePrefix("$g/"))
        }
        return null
    }
}
