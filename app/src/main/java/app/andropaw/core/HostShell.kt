package app.andropaw.core

import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelFileDescriptor
import moe.shizuku.server.IRemoteProcess
import moe.shizuku.server.IShizukuService
import rikka.shizuku.Shizuku

/**
 * 宿主侧的高权限 shell 通道 —— shell (uid 2000)，不需要 root。
 *
 * 目前只有一处需要它：[PhantomKiller] 在模拟器运行期间临时抬高宿主 Android 12+ 的 phantom
 * 进程上限。设备上装了 Shizuku 并授权后，这里执行的就是和 `adb shell` 完全等价的命令。
 *
 * 没有 Shizuku 时返回 [State.NOT_INSTALLED] / [State.NOT_RUNNING]，调用方退回「让用户手动 adb」，
 * 所以这个类不可用时不会改变任何既有行为。
 */
object HostShell {
    const val PKG = "moe.shizuku.privileged.api"

    enum class State {
        /** Shizuku 服务在跑、本应用已授权：可以执行命令 */
        READY,
        /** Shizuku 装了、服务在跑，但本应用没被授权 */
        NEEDS_PERMISSION,
        /** Shizuku 装了但服务没起（重启后没重新通过无线调试 / root 启动） */
        NOT_RUNNING,
        /** 没装 Shizuku */
        NOT_INSTALLED,
    }

    data class Out(val code: Int, val text: String) {
        val ok: Boolean get() = code == 0

        /**
         * `settings get` / `device_config get` 用字面量 "null" 表示「这个键没有值」。
         * 还原时必须区分「原来是 0」和「原来根本没设置」，所以这里保留 null。
         */
        val value: String? get() = text.trim().takeIf { it.isNotEmpty() && !it.equals("null", true) }
    }

    private const val REQUEST_CODE = 0x5A1
    private const val DEFAULT_TIMEOUT_MS = 8_000L

    /** 「装着」是安装期的性质，缓存住（设置页每 1.5s 查一次状态，不想每次都走包管理器 IPC）。 */
    @Volatile private var installedOnce = false

    fun installed(ctx: Context): Boolean {
        if (installedOnce) return true
        val v = runCatching {
            ctx.packageManager.getPackageInfo(PKG, 0)
            true
        }.getOrDefault(false)
        if (v) installedOnce = true
        return v
    }

    /** Shizuku 服务在跑（不看授权）。 */
    fun running(): Boolean = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    fun granted(): Boolean = runCatching {
        running() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    fun state(ctx: Context): State = when {
        !installed(ctx) -> State.NOT_INSTALLED
        !running() -> State.NOT_RUNNING
        granted() -> State.READY
        else -> State.NEEDS_PERMISSION
    }

    fun usable(ctx: Context): Boolean = state(ctx) == State.READY

    /** 弹 Shizuku 的授权框（需要有 Activity 在前台）。返回是否真的发起了请求。 */
    fun requestPermission(): Boolean = runCatching {
        if (!running() || granted()) return false
        Shizuku.requestPermission(REQUEST_CODE)
        true
    }.getOrDefault(false)

    /** 执行一条 shell 命令；null = 通道不可用 / 超时。 */
    fun exec(cmd: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS): Out? {
        if (!granted()) return null
        return runCatching {
            // Shizuku 13 把 Shizuku.newProcess 私有化了（它自己的 ShizukuRemoteProcess 构造器也是
            // 包私有），所以直接走服务端的 AIDL：IShizukuService.newProcess 仍是公开方法，
            // 返回的 IRemoteProcess 就是一路 ParcelFileDescriptor。这条路径没有 deprecated API。
            val svc = IShizukuService.Stub.asInterface(Shizuku.getBinder()) ?: return null
            val rp: IRemoteProcess = svc.newProcess(arrayOf("/system/bin/sh", "-c", cmd), null, "/") ?: return null
            val code = await(rp, timeoutMs)
            if (code == null) {
                runCatching { rp.destroy() }
                return null
            }
            val out = runCatching { drain(rp.inputStream) }.getOrDefault("")
            val err = runCatching { drain(rp.errorStream) }.getOrDefault("")
            Out(code, (out + err).trim())
        }.getOrNull()
    }

    private fun drain(fd: ParcelFileDescriptor?): String =
        ParcelFileDescriptor.AutoCloseInputStream(fd).use { it.readBytes().toString(Charsets.UTF_8) }

    /**
     * `IRemoteProcess.waitFor()` 是同步 binder 调用，进程不退出就一直挂着 —— 用 alive() 轮询
     * 拿超时，Shizuku 服务半死不活时不会把开机线程一起拖住。
     */
    private fun await(rp: IRemoteProcess, timeoutMs: Long): Int? {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (!runCatching { rp.alive() }.getOrDefault(false)) {
                return runCatching { rp.exitValue() }.getOrNull()
            }
            Thread.sleep(40)
        }
        return null
    }
}
