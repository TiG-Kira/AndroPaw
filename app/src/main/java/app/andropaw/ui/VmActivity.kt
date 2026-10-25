package app.andropaw.ui

import app.andropaw.R
import androidx.compose.ui.res.stringResource
import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CropSquare
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.andropaw.core.Engine
import app.andropaw.core.GuestLog
import app.andropaw.core.GuestVm
import app.andropaw.core.ImageStore
import app.andropaw.core.InputService
import kotlinx.coroutines.delay
import kotlin.concurrent.thread
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.glass.GlassDropdownPopup
import top.yukonga.miuix.kmp.glass.GlassPopupItem

/** Процесс :vm держит ровно одну машину. */
object VmHost {
    @Volatile var vm: GuestVm? = null
}

class VmActivity : ComponentActivity() {
    private lateinit var vm: GuestVm
    private lateinit var surfaceView: SurfaceView
    private lateinit var guest: GuestScreen
    /** the guest's screen size, for what takes frames off it (VNC, recording) */
    private var guestW = 0
    private var guestH = 0
    private var vnc: app.andropaw.core.VncServer? = null
    private var recorder: app.andropaw.core.ScreenRecorder? = null
    private val vncOn = androidx.compose.runtime.mutableStateOf(false)
    private val recOn = androidx.compose.runtime.mutableStateOf(false)
    private val grabThread by lazy { android.os.HandlerThread("frame-grab").apply { start() } }
    private var grabScratch: android.graphics.Bitmap? = null
    private lateinit var box: FrameLayout
    /**
     * The Surface is handed to the native bridge off the UI thread, and in order.
     *
     * Handing it over directly froze the app on every pause: the callback runs on the UI thread,
     * and a bridge whose window is going away at that very moment (its surface is being destroyed)
     * can block in the native call — the UI thread went with it and the machine could only be
     * brought back by restarting it. A single worker keeps the calls ordered and the UI free.
     */
    private val surfaceHand by lazy {
        java.util.concurrent.Executors.newSingleThreadExecutor { r -> Thread(r, "vm-surface").apply { isDaemon = true } }
    }
    private fun handSurface(s: android.view.Surface?) {
        if (!::vm.isInitialized) return
        surfaceHand.execute { runCatching { vm.surface = s } }
    }
    /** the guest screen's frame count as it was when this activity came back, for the check below */
    private var shownAtResume = -1L
    /** one-shot: after coming back, is the screen drawing at all? (a stuck bridge is logged, then re-bound) */
    private var resumeCheckPosted = false
    private val uiHandler by lazy { android.os.Handler(android.os.Looper.getMainLooper()) }
    private var state by mutableStateOf(GuestVm.State.STOPPED)
    /** the freeze supervisor's verdict, shown as a quiet banner over the guest screen */
    private val frozenState = androidx.compose.runtime.mutableStateOf(false)
    /** the guest's notification LED and where the guest screen starts (the LED sits just above it) */
    private var led by mutableStateOf<app.andropaw.core.Lights.Led?>(null)
    /** this system's setting "Notification light" (the settings sheet changes it while the system runs) */
    private var showLed by mutableStateOf(true)
    private var guestTopPx by mutableStateOf(0)
    private val massDeathNotice = mutableStateOf(false)
    private val logLines = mutableStateListOf<String>()

    @SuppressLint("ClickableViewAccessibility")
    override fun attachBaseContext(base: Context) = super.attachBaseContext(app.andropaw.AppPrefs.wrap(base))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // the overlay pads itself (status bar, navigation bar, keyboard): the terminal stays above the keyboard
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        val id = intent.getStringExtra(EXTRA_ID)
        val cur = VmHost.vm
        val img = ((if (id != null) ImageStore.get(this, id) else null) ?: cur?.img)
            ?.let { if (cur?.img?.id == it.id) it else app.andropaw.importer.Analyzer.refresh(this, it).effective() }
        if (img == null) { finish(); return }
        if (cur != null && cur.img.id != img.id && cur.state != GuestVm.State.STOPPED) {
            // в процессе уже живёт другая машина — её надо сначала остановить
            cur.stop()
            restartProcess(img.id)
            return
        }
        vm = if (cur != null && cur.img.id == img.id) cur
            else GuestVm(applicationContext, img).also { it.recoveryMode = intent.getBooleanExtra(EXTRA_RECOVERY, false); VmHost.vm = it }
        vm.onPower = { reboot, reason -> runOnUiThread { if (reboot) rebootVm(reason == "recovery") else stopVm() } }
        // 大量 137：只提示，不擅自恢复（用户决策）
        vm.onMassDeath = { runOnUiThread { massDeathNotice.value = true } }
        // 软重启会换掉 surfaceflinger：让显示视图重新绑定，避免画面停在旧帧
        vm.onScreenReset = {
            runCatching {
                guest.poke()
                if (::guest.isInitialized) guest.format = if (vm.engine == app.andropaw.core.Engine.N64) 1 else guest.format
            }
        }
        requestIgnoreBatteryOptimizationsOnce()
        vm.onFreeze = { f -> runOnUiThread { frozenState.value = f } }
        val s = vm.settings
        showLed = s.showLed
        if (s.keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val (gw, gh) = if (vm.engine == Engine.GB) 480 to 800 else s.width to s.height
        val root = FrameLayout(this).apply { setBackgroundColor(0xff000000.toInt()) }
        box = FrameLayout(this)
        surfaceView = SurfaceView(this)
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(h: SurfaceHolder) { handSurface(h.surface) }
            override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, ht: Int) { handSurface(h.surface) }
            override fun surfaceDestroyed(h: SurfaceHolder) { handSurface(null) }
        })
        guestW = gw; guestH = gh
        guest = GuestScreen(this, gw, gh).apply {
            input = vm.input
            fb = if (vm.recoveryMode) app.andropaw.core.RecoveryImage.fb(vm.paths) else vm.paths.fb
            if (vm.recoveryMode) pages = 2
        }
        // 5.0+ renders through the standalone glserverd into fb0 (see GuestVm.glUp), shown like the software path
        val useBridge = vm.engine == Engine.KK && s.gpu && !vm.recoveryMode && vm.img.api < 21
        // the arm64 engine's GLES bridge shows SurfaceFlinger's frames in the same view, straight from the GPU; until
        // the first of them (and with the software renderer) the screen is fb0 as before
        val direct = vm.engine == Engine.N64 && vm.kernel.gpuBridge64 && s.gpu && !vm.recoveryMode
        if (direct) { surfaceView.holder.setFixedSize(gw, gh); guest.direct = { app.andropaw.core.GlHost.presented() } }
        surfaceView.visibility = if (useBridge || direct) View.VISIBLE else View.GONE
        guest.passthrough = useBridge
        box.addView(surfaceView, FrameLayout.LayoutParams(-1, -1))
        box.addView(guest, FrameLayout.LayoutParams(-1, -1))
        root.addView(box, FrameLayout.LayoutParams(0, 0))
        // Overlay 里的 OverlayDialog / 下拉菜单依赖 Scaffold 提供的 LocalRootDialogStates 与
        // MiuixPopupHost：没有 Scaffold 宿主，状态挂进默认空表，窗口永远弹不出来。
        // containerColor 必须透明——这层 ComposeView 叠在虚拟机画面之上。
        val overlay = ComposeView(this).apply {
            setContent {
                ProvideNavigationEventOwner {
                    AndroPawTheme(forceDark = true) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = Color.Transparent,
                        ) { Overlay() }
                    }
                }
            }
        }
        root.addView(overlay, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)

        // экран гостя вписываем с сохранением пропорций над панелью кнопок
        val navDp = if (!s.showNavBar) 0 else if (s.navSize == 0) 46 else 64
        // the window draws edge to edge (for the overlay's keyboard padding): the guest screen keeps out of the
        // camera cutout and system bars; the keyboard is not counted, so the screen does not jump when it opens
        var safeTop = 0
        var safeBottom = 0
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root) { v, ins ->
            val bars = ins.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars() or androidx.core.view.WindowInsetsCompat.Type.displayCutout())
            if (bars.top != safeTop || bars.bottom != safeBottom) { safeTop = bars.top; safeBottom = bars.bottom; v.requestLayout() }
            ins
        }
        root.addOnLayoutChangeListener { v, l, t, r, b, _, _, _, _ ->
            val navPx = (navDp * resources.displayMetrics.density).toInt()
            val aw = r - l
            val ah = b - t - navPx - safeTop - safeBottom
            if (aw <= 0 || ah <= 0) return@addOnLayoutChangeListener
            val scale = minOf(aw.toFloat() / gw, ah.toFloat() / gh)
            val w = (gw * scale).toInt()
            val h = (gh * scale).toInt()
            val lp = box.layoutParams as FrameLayout.LayoutParams
            val top = safeTop + (ah - h) / 2
            if (guestTopPx != top) guestTopPx = top
            if (lp.width != w || lp.height != h || lp.topMargin != top) {
                lp.width = w; lp.height = h
                lp.gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
                lp.topMargin = top
                v.post { box.layoutParams = lp }
            }
        }

        vm.onFrame = { guest.rings++; guest.poke() }
        state = vm.state
        logLines.addAll(vm.lines().takeLast(200))
        vm.onState { st -> runOnUiThread { state = st } }
        vm.mic.onPermissionNeeded = { runOnUiThread { askMic.launch(android.Manifest.permission.RECORD_AUDIO) } }
        vm.camera.onPermissionNeeded = { runOnUiThread { askCamera.launch(android.Manifest.permission.CAMERA) } }
        led = vm.lights.current
        vm.lights.onChange { l -> runOnUiThread { led = l } }
        vm.onLog { line -> runOnUiThread { logLines.add(line); if (logLines.size > 400) logLines.removeRange(0, logLines.size - 400) } }
        guest.start()
        loadMenuOffset()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { vm.input.press(InputService.KEY_BACK) }
        })

        registerShell()
        if (vm.state == GuestVm.State.STOPPED || vm.state == GuestVm.State.FAILED) {
            thread(name = "vm-boot") { vm.boot() }
        }
    }

    /** Отладка: adb shell am broadcast -a app.andropaw.SHELL --es cmd "dumpsys power" → run/shell.out */
    private var shellRx: android.content.BroadcastReceiver? = null
    private fun registerShell() {
        if (!app.andropaw.BuildConfig.DEBUG) return
        val rx = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                // --es keys "116d 115 116u": raw guest key codes, d/u = down/up only, bare = press
                i.getStringExtra("keys")?.let { ks ->
                    thread {
                        for (k in ks.split(' ').filter { it.isNotBlank() }) {
                            val code = k.trimEnd('d', 'u').toIntOrNull() ?: continue
                            when { k.endsWith("d") -> vm.input.key(code, true); k.endsWith("u") -> vm.input.key(code, false)
                                else -> { vm.input.key(code, true); Thread.sleep(80); vm.input.key(code, false) } }
                            Thread.sleep(150)
                        }
                    }
                    return
                }
                i.getStringExtra("sms")?.let { sendSms("+15550001234", it); return }
                if (i.getBooleanExtra("shot", false)) { runOnUiThread { screenshot() }; return }
                // --ez frames true --es out <name>: the count of frames shown and the clock, for the testers
                if (i.getBooleanExtra("frames", false)) {
                    val name = i.getStringExtra("out")?.takeIf { it.matches(Regex("[A-Za-z0-9_.-]+")) } ?: "frames.out"
                    java.io.File(vm.paths.bin, name).writeText("shown=${guest.shown} direct=${guest.passthrough} ms=${android.os.SystemClock.uptimeMillis()}\n")
                    return
                }
                if (i.hasExtra("adb")) { if (i.getBooleanExtra("adb", false)) vm.adb.start() else vm.adb.stop(); return }
                val cmd = i.getStringExtra("cmd") ?: return
                // своё имя файла на каждую команду: медленная предыдущая команда не затрёт ответ
                val name = i.getStringExtra("out")?.takeIf { it.matches(Regex("[A-Za-z0-9_.-]+")) } ?: "shell.out"
                thread {
                    val out = runCatching { vm.guestShell(cmd, 120_000) }.getOrElse { it.toString() }
                    val tmp = java.io.File(vm.paths.bin, "$name.tmp")
                    tmp.writeText(out + "\n<<done>>\n"); tmp.renameTo(java.io.File(vm.paths.bin, name))
                }
            }
        }
        shellRx = rx
        val f = android.content.IntentFilter("app.andropaw.SHELL")
        if (android.os.Build.VERSION.SDK_INT >= 33) registerReceiver(rx, f, Context.RECEIVER_EXPORTED) else registerReceiver(rx, f)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val code = map(keyCode) ?: return super.onKeyDown(keyCode, event)
        if (event.repeatCount == 0) vm.input.key(code, true)
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        val code = map(keyCode) ?: return super.onKeyUp(keyCode, event)
        vm.input.key(code, false)
        return true
    }

    private fun map(k: Int): Int? = when (k) {
        KeyEvent.KEYCODE_VOLUME_UP -> InputService.KEY_VOLUMEUP
        KeyEvent.KEYCODE_VOLUME_DOWN -> InputService.KEY_VOLUMEDOWN
        KeyEvent.KEYCODE_MENU -> InputService.KEY_MENU
        KeyEvent.KEYCODE_SEARCH -> InputService.KEY_SEARCH
        else -> null
    }

    override fun onResume() {
        super.onResume()
        if (!::vm.isInitialized) return
        vm.sensors.resume(true)
        vm.setUiVisible(true)
        // coming back from the home page or from another app: the window's surface was destroyed
        // while we were away, so make sure the bridge holds the one that is on screen now (a
        // create that landed while the activity was stopping is easy to miss)
        runCatching {
            val s = surfaceView.holder.surface
            if (s != null && s.isValid) handSurface(s)
        }
        guest.poke()
        shownAtResume = guest.shown
        resumeCheckPosted = false
        postResumeCheck()
    }

    /**
     * A few seconds after the screen is back: has anything been drawn? If not, the bridge is stuck
     * (deadlock or a lost window) — rebind the surface once and write what was seen to the log, so
     * a frozen machine can be told apart from an idle one without restarting it.
     */
    private fun postResumeCheck() {
        if (resumeCheckPosted || !::vm.isInitialized) return
        resumeCheckPosted = true
        uiHandler.postDelayed({
            if (!::vm.isInitialized || isFinishing || isDestroyed) return@postDelayed
            val running = vm.state == GuestVm.State.RUNNING
            if (running && guest.shown == shownAtResume) {
                vm.log("screen: nothing drawn ${RESUME_CHECK_MS / 1000}s after returning — rebinding the surface")
                runCatching {
                    val s = surfaceView.holder.surface
                    if (s != null && s.isValid) handSurface(s) else handSurface(null)
                }
                guest.poke()
                uiHandler.postDelayed({
                    if (!::vm.isInitialized || isFinishing || isDestroyed) return@postDelayed
                    vm.log(
                        "screen: after rebinding — shown=${guest.shown} (was $shownAtResume) " +
                            "passthrough=${guest.passthrough} surface=${surfaceView.holder.surface?.isValid} " +
                            "frames=${if (vm.glInApp) dev.lk.m7sense.GlBridge.frames() else guest.rings}",
                    )
                }, RESUME_CHECK_MS)
            }
        }, RESUME_CHECK_MS)
    }

    override fun onPause() {
        if (::vm.isInitialized) {
            vm.sensors.resume(false); vm.setUiVisible(false)
            // 宿主切后台后 MIUI 的清理通常几秒内就到：先立刻刷一遍 oom pin 抢时间窗
            runCatching { vm.reprotectNow() }
        }
        super.onPause()
    }

    override fun onDestroy() {
        vnc?.stop(); vnc = null
        recorder?.let { r -> thread { r.stop() } }; recorder = null
        if (::guest.isInitialized) guest.stop()
        shellRx?.let { runCatching { unregisterReceiver(it) } }
        super.onDestroy()
    }

    /** 一次性的电池优化豁免请求：MIUI/ColorOS 的后台清理会扫掉模拟器的子进程，系统级豁免是最有效的一层 */
    private fun requestIgnoreBatteryOptimizationsOnce() {
        runCatching {
            val prefs = getSharedPreferences("vm", MODE_PRIVATE)
            if (prefs.getBoolean("asked_batt_opt", false)) return
            val pm = getSystemService(android.os.PowerManager::class.java)
            if (pm != null && pm.isIgnoringBatteryOptimizations(packageName)) return
            prefs.edit().putBoolean("asked_batt_opt", true).apply()
            startActivity(Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, android.net.Uri.parse("package:$packageName")))
        }
    }

    private fun stopVm() {
        thread {
            vm.stop()
            VmHost.vm = null
            runOnUiThread { finishAndRemoveTask() }
            Thread.sleep(300)
            // GL-мост нельзя поднять повторно в том же процессе — процесс :vm уходит вместе с машиной
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    private enum class Dlg { NONE, SMS, CALL, BATTERY, ADB, DEVICE }

    // position of the floating menu button, kept across launches
    private val uiPrefs by lazy { getSharedPreferences("vm_ui", MODE_PRIVATE) }
    private var menuOffset by mutableStateOf(Offset.Zero)
    private fun loadMenuOffset() { menuOffset = Offset(uiPrefs.getFloat("menu_x", 0f), uiPrefs.getFloat("menu_y", 0f)) }
    private fun saveMenuOffset() { uiPrefs.edit().putFloat("menu_x", menuOffset.x).putFloat("menu_y", menuOffset.y).apply() }

    private val askMic = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) vm.mic.permissionGranted() }

    private val askCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private fun toast(msg: String) = runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }

    private fun guestAsync(cmd: String, done: ((String) -> Unit)? = null) {
        thread { val out = runCatching { vm.guestShell(cmd, 60_000) }.getOrElse { it.toString() }; done?.invoke(out) }
    }

    /** Quote for the guest's /system/bin/sh. */
    private fun q(s: String) = "'" + s.replace("'", "'\\''") + "'"

    private fun pasteToGuest() {
        val text = getSystemService(ClipboardManager::class.java).primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
        if (text.isEmpty()) { toast(getString(R.string.m_clip_empty)); return }
        // Normalize line endings and truncate extreme paste payloads
        val lines = text.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        val safeLines = if (lines.size > 150) lines.take(150) else lines
        val script = buildString {
            for ((i, line) in safeLines.withIndex()) {
                if (line.isNotEmpty()) {
                    val escaped = line.replace(" ", "%s")
                    append("input text ").append(q(escaped)).append("; ")
                }
                if (i < safeLines.lastIndex) {
                    append("input keyevent 66; ")
                }
            }
        }
        guestAsync(script)
    }

    /**
     * The frame is taken on the host (the fb0 copy, or the GPU bridge's surface through PixelCopy): `screencap` does
     * not exist before 4.0 and cannot read the bridge's frames. The PNG lands in the guest's Pictures folder.
     */
    private fun screenshot() {
        val name = "aemu-" + java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.US).format(java.util.Date()) + ".png"
        val hostDir = app.andropaw.core.Sdcard.hostDir(vm.paths)?.let { java.io.File(it, "Pictures") }
        if (hostDir == null) { toast(getString(R.string.m_failed)); return }
        fun save(b: android.graphics.Bitmap) = thread {
            val ok = runCatching {
                hostDir.mkdirs()
                java.io.File(hostDir, name).outputStream().use { check(b.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)) }
            }.also { b.recycle() }.isSuccess
            toast(if (ok) getString(R.string.m_screenshot_saved, vm.img.sdcardPath.trimEnd('/') + "/Pictures/$name") else getString(R.string.m_failed))
            // the guest's media scanner picks the file up for its gallery
            if (ok) guestAsync("am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file://${vm.img.sdcardPath.trimEnd('/')}/Pictures/$name")
        }
        guest.snapshot()?.let { save(it); return }
        if (surfaceView.width <= 0 || surfaceView.height <= 0) { toast(getString(R.string.m_failed)); return }
        val b = android.graphics.Bitmap.createBitmap(surfaceView.width, surfaceView.height, android.graphics.Bitmap.Config.ARGB_8888)
        runCatching {
            android.view.PixelCopy.request(surfaceView, b, { r ->
                if (r == android.view.PixelCopy.SUCCESS) save(b) else {
                    b.recycle()
                    toast(getString(R.string.m_failed))
                }
            }, android.os.Handler(mainLooper))
        }.onFailure { b.recycle(); toast(getString(R.string.m_failed)) }
    }

    /**
     * The current frame at the guest's resolution: the fb0 copy, or the GPU bridge's surface through PixelCopy
     * (waited for here, so not to be called on the main thread).
     */
    private fun grabFrame(into: android.graphics.Bitmap): Boolean {
        val dst = android.graphics.Rect(0, 0, into.width, into.height)
        guest.snapshot()?.let { android.graphics.Canvas(into).drawBitmap(it, null, dst, null); it.recycle(); return true }
        val sw = surfaceView.width
        val sh = surfaceView.height
        if (sw <= 0 || sh <= 0 || !surfaceView.holder.surface.isValid) return false
        val tmp = grabScratch?.takeIf { it.width == sw && it.height == sh }
            ?: android.graphics.Bitmap.createBitmap(sw, sh, android.graphics.Bitmap.Config.ARGB_8888).also { grabScratch = it }
        val done = java.util.concurrent.CountDownLatch(1)
        var ok = false
        runCatching {
            android.view.PixelCopy.request(surfaceView, tmp, { r -> ok = r == android.view.PixelCopy.SUCCESS; done.countDown() },
                android.os.Handler(grabThread.looper))
        }.onFailure { return false }
        if (!done.await(500, java.util.concurrent.TimeUnit.MILLISECONDS) || !ok) return false
        android.graphics.Canvas(into).drawBitmap(tmp, null, dst, null)
        return true
    }

    /** VNC on/off. Each session gets a password of its own, shown here and in the log. */
    private fun toggleVnc() {
        vnc?.let { it.stop(); vnc = null; vncOn.value = false; return }
        if (guestW <= 0 || guestH <= 0) { toast(getString(R.string.m_failed)); return }
        val password = (1..8).map { "abcdefghijkmnpqrstuvwxyz23456789".random() }.joinToString("")
        val bmp = android.graphics.Bitmap.createBitmap(guestW, guestH, android.graphics.Bitmap.Config.ARGB_8888)
        val server = app.andropaw.core.VncServer(app.andropaw.core.VncServer.DEFAULT_PORT, password, guestW, guestH, vm.img.name,
            { px -> if (grabFrame(bmp)) { bmp.getPixels(px, 0, guestW, 0, 0, guestW, guestH); true } else false }, vm.input, vm::log)
        if (!server.start()) { toast(getString(R.string.m_failed)); return }
        vnc = server; vncOn.value = true
        val ip = runCatching {
            java.net.NetworkInterface.getNetworkInterfaces().asSequence().filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.asSequence() }.firstOrNull { it is java.net.Inet4Address }?.hostAddress
        }.getOrNull() ?: "127.0.0.1"
        val where = getString(R.string.m_vnc_started, "$ip:${app.andropaw.core.VncServer.DEFAULT_PORT}", password)
        vm.log("vnc: $where")
        runOnUiThread { Toast.makeText(this, where, Toast.LENGTH_LONG).show() }
    }

    /** Screen recording on/off: an MP4 in the Movies folder of the guest's card (the phone's shared folder). */
    private fun toggleRecording() {
        recorder?.let { r ->
            recorder = null; recOn.value = false
            thread {
                r.stop()
                toast(getString(R.string.m_screenshot_saved, vm.img.sdcardPath.trimEnd('/') + "/Movies/" + r.file.name))
                guestAsync("am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file://${vm.img.sdcardPath.trimEnd('/')}/Movies/${r.file.name}")
            }
            return
        }
        val dir = app.andropaw.core.Sdcard.hostDir(vm.paths)?.let { java.io.File(it, "Movies") }
        if (dir == null || guestW <= 0 || guestH <= 0) { toast(getString(R.string.m_failed)); return }
        val name = "aemu-" + java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.US).format(java.util.Date()) + ".mp4"
        val r = app.andropaw.core.ScreenRecorder(java.io.File(dir, name), guestW, guestH, ::grabFrame, vm::log)
        if (!r.start()) { toast(getString(R.string.m_failed)); return }
        recorder = r; recOn.value = true
    }

    // APKs picked on the host are streamed into unique shared-card files and installed in order.
    private val pickApks = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@registerForActivityResult
        val dir = app.andropaw.core.Sdcard.hostDir(vm.paths)
        if (dir == null) { toast(getString(R.string.m_failed)); return@registerForActivityResult }
        toast(getString(R.string.m_install_apk_start) + if (uris.size > 1) " (${uris.size})" else "")
        thread {
            var installed = 0
            val failures = ArrayList<String>()
            for ((index, uri) in uris.withIndex()) {
                val name = "aemu-install-${java.util.UUID.randomUUID()}.apk"
                val staged = java.io.File(dir, name)
                val result = runCatching {
                    val input = contentResolver.openInputStream(uri) ?: error("Cannot open selected APK")
                    input.use { i -> staged.outputStream().buffered(128 * 1024).use { o -> i.copyTo(o, 128 * 1024) } }
                    val guestPath = vm.img.sdcardPath.trimEnd('/') + "/" + name
                    vm.guestShell("pm install -r ${q(guestPath)}", 300_000)
                }
                val output = result.getOrNull()?.trim().orEmpty()
                if (result.isSuccess && output.lineSequence().lastOrNull { it.isNotBlank() } == "Success") installed++
                else failures += "${index + 1}: ${(result.exceptionOrNull()?.message ?: output).ifBlank { getString(R.string.m_failed) }.take(120)}"
                runCatching { staged.delete() }
            }
            if (installed > 0) runCatching { app.andropaw.core.TreeFixer(this@VmActivity, vm.paths, vm.img, vm::log).seedOwners() }
            val summary = if (failures.isEmpty()) "$installed/${uris.size} APK installed" else
                "$installed/${uris.size} APK installed; failed: ${failures.take(3).joinToString("; ")}"
            toast(summary.take(400))
        }
    }

    private fun sendSms(from: String, body: String) {
        // through the emulated modem: stored, notified and rung like a real SMS on every version
        if (vm.ril.sms(from, body)) { toast(getString(R.string.m_sms_sent)); return }
        // radio emulation off: the message is written straight into the SMS provider inbox (4.1+)
        val cmd = "content insert --uri content://sms/inbox --bind address:s:${q(from)} --bind body:s:${q(body)} " +
            "--bind read:i:0 --bind date:l:${System.currentTimeMillis()}"
        guestAsync(cmd) { out -> toast(if (out.isBlank()) getString(R.string.m_sms_sent) else out.trim().take(200)) }
    }

    @Composable
    private fun SmsDialog(onDone: () -> Unit) {
        var from by remember { mutableStateOf("+10000000000") }
        var body by remember { mutableStateOf("Hello from AndroPaw") }
        MiuixDialog(show = true, title = stringResource(R.string.m_sms), onDismissRequest = onDone) {
            TextField(from, { from = it }, label = stringResource(R.string.m_sms_from), singleLine = true)
            TextField(body, { body = it }, label = stringResource(R.string.m_sms_body))
            DialogButtonRow(listOf(
                DialogAction(stringResource(R.string.close), onClick = onDone),
                DialogAction(stringResource(R.string.m_send), onClick = { sendSms(from, body); onDone() }, primary = true),
            ))
        }
    }

    @Composable
    private fun DeviceDialog(onDone: () -> Unit) {
        var airplane by remember { mutableStateOf(false) }
        var awake by remember { mutableStateOf(false) }
        val ns = if (vm.img.api >= 17) "global" else "system"
        // the switches show what the guest has now
        LaunchedEffect(Unit) {
            guestAsync("settings get $ns airplane_mode_on; settings get $ns stay_on_while_plugged_in") { out ->
                val v = out.lines().map { it.trim() }.filter { it.isNotEmpty() }
                runOnUiThread { airplane = v.getOrNull(0) == "1"; awake = (v.getOrNull(1)?.toIntOrNull() ?: 0) != 0 }
            }
        }
        MiuixDialog(show = true, title = stringResource(R.string.m_device), onDismissRequest = onDone) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.m_airplane), Modifier.weight(1f))
                Switch(airplane, { v ->
                    airplane = v
                    guestAsync("settings put $ns airplane_mode_on ${if (v) 1 else 0}; " +
                        "am broadcast -a android.intent.action.AIRPLANE_MODE --ez state $v")
                })
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.m_stay_awake), Modifier.weight(1f))
                Switch(awake, { v ->
                    awake = v
                    guestAsync("settings put $ns stay_on_while_plugged_in ${if (v) 7 else 0}; svc power stayon ${if (v) "true" else "false"}")
                })
            }
            DialogButtonRow(listOf(DialogAction(stringResource(R.string.close), onClick = onDone, primary = true)))
        }
    }

    @Composable
    private fun AdbDialog(onDone: () -> Unit) {
        var on by remember { mutableStateOf(vm.adb.running) }
        MiuixDialog(show = true, title = stringResource(R.string.m_adb), onDismissRequest = onDone) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.m_adb_enable), Modifier.weight(1f))
                Switch(on, { v -> if (v) vm.adb.start() else vm.adb.stop(); on = vm.adb.running })
            }
            if (on) {
                val ips = app.andropaw.core.AdbServer.addresses()
                for (ip in ips.ifEmpty { listOf("127.0.0.1") })
                    Text("adb connect $ip:${vm.adb.port}", fontSize = 15.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            }
            Text(stringResource(R.string.m_adb_hint), fontSize = 12.sp)
            DialogButtonRow(listOf(DialogAction(stringResource(R.string.close), onClick = onDone, primary = true)))
        }
    }

    @Composable
    private fun CallDialog(onDone: () -> Unit) {
        var from by remember { mutableStateOf("+10000000000") }
        MiuixDialog(show = true, title = stringResource(R.string.m_call), onDismissRequest = onDone) {
            TextField(from, { from = it }, label = stringResource(R.string.m_sms_from), singleLine = true)
            DialogButtonRow(listOf(
                DialogAction(stringResource(R.string.close), onClick = onDone),
                DialogAction(stringResource(R.string.m_call_ring), onClick = {
                    if (!vm.ril.ring(from.trim())) toast(getString(R.string.m_failed))
                    onDone()
                }, primary = true),
            ))
        }
    }

    @Composable
    private fun BatteryDialog(onDone: () -> Unit) {
        var level by remember { mutableStateOf(80f) }
        var charging by remember { mutableStateOf(true) }
        // start from the guest's own battery state (sysfs or dumpsys)
        LaunchedEffect(Unit) {
            val cmd = "cat /sys/class/power_supply/battery/capacity 2>/dev/null; " +
                "cat /sys/class/power_supply/ac/online 2>/dev/null; " +
                "dumpsys battery 2>/dev/null"
            guestAsync(cmd) { out ->
                val lines = out.lines().map { it.trim() }
                val sysCap = lines.getOrNull(0)?.toFloatOrNull()
                val sysAc = lines.getOrNull(1)?.toIntOrNull()
                fun field(n: String) = out.lineSequence().map { it.trim() }.firstOrNull { it.startsWith("$n:") }?.substringAfter(':')?.trim()
                val l = sysCap ?: field("level")?.toFloatOrNull()
                val ac = if (sysAc != null) sysAc == 1 else (field("AC powered") == "true" || field("USB powered") == "true")
                runOnUiThread {
                    if (l != null) level = l.coerceIn(0f, 100f)
                    charging = ac
                }
            }
        }
        MiuixDialog(show = true, title = stringResource(R.string.m_battery), onDismissRequest = onDone) {
            Text("${level.toInt()}%", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Slider(level, { level = it }, valueRange = 0f..100f)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.m_charging), Modifier.weight(1f)); Switch(charging, { charging = it })
            }
            DialogButtonRow(listOf(
                DialogAction(stringResource(R.string.m_reset), onClick = {
                    guestAsync("dumpsys battery reset 2>/dev/null")
                    onDone()
                }),
                DialogAction(stringResource(R.string.m_apply), onClick = {
                    val lvl = level.toInt()
                    val ac = if (charging) 1 else 0
                    val cmd = "echo $lvl > /sys/class/power_supply/battery/capacity 2>/dev/null; " +
                        "echo $ac > /sys/class/power_supply/ac/online 2>/dev/null; " +
                        "dumpsys battery set level $lvl 2>/dev/null; " +
                        "dumpsys battery set ac $ac 2>/dev/null; " +
                        "dumpsys battery set usb 0 2>/dev/null"
                    guestAsync(cmd)
                    onDone()
                }, primary = true),
            ))
        }
    }

    private fun rebootVm(recovery: Boolean = false) {
        val id = vm.img.id
        thread {
            vm.stop()
            VmHost.vm = null
            // the GL bridge cannot come up twice in one process, so a reboot restarts the :vm process
            runOnUiThread { restartProcess(id, recovery) }
        }
    }

    private fun restartProcess(id: String, recovery: Boolean = false) {
        val i = Intent(this, VmActivity::class.java).putExtra(EXTRA_ID, id).putExtra(EXTRA_RECOVERY, recovery)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pi = android.app.PendingIntent.getActivity(this, 1, i, android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_CANCEL_CURRENT)
        (getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager).set(android.app.AlarmManager.RTC, System.currentTimeMillis() + 400, pi)
        finishAndRemoveTask()
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    // ------------------------------------------------------------------ интерфейс поверх экрана

    @Composable
    private fun Overlay() {
        var showLog by remember { mutableStateOf(false) }
        var showTerm by remember { mutableStateOf(false) }
        var dialog by remember { mutableStateOf(Dlg.NONE) }
        // recovery is "running" too, but has no Android behind am/pm/settings
        val running = state == GuestVm.State.RUNNING && !vm.recoveryMode
        var seconds by remember { mutableStateOf(0L) }
        var frames by remember { mutableStateOf(0L) }
        LaunchedEffect(Unit) { while (true) { if (vm.recoveryMode) guest.format = vm.recoveryFormat else if (vm.engine == app.andropaw.core.Engine.N64) guest.format = 1; seconds = vm.bootSeconds(); frames = if (vm.glInApp) dev.lk.m7sense.GlBridge.frames() else guest.rings; delay(500) } }
        // как только гость начал рисовать — карточку убираем, остаётся маленький индикатор
        val drawing = frames > 30
        // The "more" menu: the button and the panel it drops are hosted apart. GlassDropdownPopup lays
        // itself out on the box it is composed in, so hosting it inside the 40 dp button made that
        // button the whole page — the panel had nowhere to grow and stayed a small circle. The panel
        // is therefore hosted on the page below (Termux-Ultra's rule: the popup lives on the root box).
        var menuOpen by remember { mutableStateOf(false) }
        // the button, in root coordinates
        var menuIcon by remember { mutableStateOf(Rect.Zero) }
        // the page's origin, in root coordinates — the anchor is handed to the panel relative to it
        var menuPageOrigin by remember { mutableStateOf(Offset.Zero) }
        // AgentPaw floating chat: the entry on the operating page itself. Its shell rides the live
        // VM's own runner — no second qemu-user start.
        var agentOpen by remember { mutableStateOf(false) }
        val agentBridge = remember {
            app.andropaw.agent.AgentVmBridge(applicationContext, null).apply {
                imageId = vm.img.id
                inlineShell = { cmd, t -> vm.guestRunner.run(listOf("/system/bin/sh", "-c", vm.shellPrefix + cmd), t) }
            }
        }
        LaunchedEffect(Unit) { app.andropaw.agent.ChatSession.attach(applicationContext, agentBridge) }
        app.andropaw.ui.RebootNoticeHost()
        androidx.compose.foundation.layout.BoxWithConstraints(
            Modifier.fillMaxSize().onGloballyPositioned { menuPageOrigin = it.boundsInRoot().topLeft }
        ) {
            // the menu button stays on screen: 40 dp button + 8 dp padding, anchored at the top end
            val keep = with(androidx.compose.ui.platform.LocalDensity.current) { 56.dp.toPx() }
            val maxX = (constraints.maxWidth - keep).coerceAtLeast(0f)
            val maxY = (constraints.maxHeight - keep * 2).coerceAtLeast(0f)
            val rtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
            fun clamp(o: Offset) = Offset(if (rtl) o.x.coerceIn(0f, maxX) else o.x.coerceIn(-maxX, 0f), o.y.coerceIn(0f, maxY))
            LaunchedEffect(maxX, maxY) { menuOffset = clamp(menuOffset) }
            led?.takeIf { it.lit && showLed }?.let { LedIndicator(it, Modifier.align(Alignment.TopCenter)) }
            // карточка загрузки
            AnimatedVisibility(
                visible = state != GuestVm.State.RUNNING && !showLog && !(drawing && state == GuestVm.State.BOOTING),
                enter = fadeIn(), exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center),
            ) { BootCard(seconds) }

            if (drawing && state == GuestVm.State.BOOTING && !showLog && vm.screenUpAt == 0L) {
                Box(Modifier.align(Alignment.TopStart).displayCutoutPadding().statusBarsPadding().padding(8.dp)
                    .clip(CircleShape).background(MiuixTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f))) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(size = 24.dp, strokeWidth = 3.dp)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.vm_booting_short, seconds.toInt()), fontSize = 12.sp)
                    }
                }
            }

            // The freeze banner: quiet, non-modal, does not take the whole screen — the staged
            // recovery is already running underneath; this tells the user and offers the way out
            // that always works, a cold restart of the machine.
            AnimatedVisibility(
                visible = frozenState.value && !showLog && !showTerm,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier.align(Alignment.TopCenter).displayCutoutPadding().statusBarsPadding().padding(top = 8.dp),
            ) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(MiuixTheme.colorScheme.errorContainer.copy(alpha = 0.94f))
                        .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Rounded.Warning, null, Modifier.size(16.dp), tint = MiuixTheme.colorScheme.error)
                    Text(stringResource(R.string.vm_frozen_banner), fontSize = 12.sp, color = MiuixTheme.colorScheme.onErrorContainer)
                    Text(
                        stringResource(R.string.vm_frozen_restart),
                        fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.error,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { rebootVm() }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            // журнал
            AnimatedVisibility(visible = showLog, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
                LogPanel(onClose = { showLog = false })
            }
            AnimatedVisibility(visible = showTerm, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
                TerminalPanel(onClose = { showTerm = false })
            }

            // the menu's entries — hoisted: the button and the panel are hosted apart, and the panel
            // is laid out on this box (the page), see at the end of it
            val menuItems = buildList {
                add(GlassMenuItem(stringResource(R.string.agent_chat_title), Icons.Rounded.AutoAwesome, enabled = running) { agentOpen = true })
                add(GlassMenuItem(stringResource(R.string.log), Icons.Rounded.Terminal) { showLog = true })
                add(GlassMenuItem(stringResource(R.string.m_terminal), Icons.Rounded.Code, enabled = running) { showTerm = true })
                // back to the app's home page: the machine keeps running in the :vm process
                // (Keeper keeps it alive) and the home page shows it as running
                add(GlassMenuItem(stringResource(R.string.m_back_home), Icons.Rounded.Close) {
                    runCatching {
                        startActivity(Intent(this@VmActivity, app.andropaw.ui.MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
                    }
                })
                add(GlassMenuItem(stringResource(R.string.vol_up), Icons.Rounded.VolumeUp) { vm.input.press(InputService.KEY_VOLUMEUP) })
                add(GlassMenuItem(stringResource(R.string.vol_down), Icons.Rounded.VolumeDown) { vm.input.press(InputService.KEY_VOLUMEDOWN) })
                add(GlassMenuItem(stringResource(R.string.power), Icons.Rounded.PowerSettingsNew) { vm.input.press(InputService.KEY_POWER) })
                add(GlassMenuItem(stringResource(R.string.m_power_menu), Icons.Rounded.PowerSettingsNew) { vm.input.press(InputService.KEY_POWER, 1500) })
                // long-press Menu makes 2.x–4.x call InputMethodManager.toggleSoftInput: the firmware's own keyboard
                if (!vm.recoveryMode) {
                    add(GlassMenuItem(stringResource(R.string.m_keyboard), Icons.Rounded.Keyboard, enabled = running) {
                        // 5.0+ hides the soft keyboard while it sees hardware keys and has no long-Menu toggle
                        if (vm.img.api >= 21) guestAsync("settings put secure show_ime_with_hard_keyboard 1")
                        else vm.input.press(InputService.KEY_MENU, 1000)
                    })
                    add(GlassMenuItem(stringResource(R.string.m_paste), Icons.Rounded.ContentPaste, enabled = running) { pasteToGuest() })
                    add(GlassMenuItem(stringResource(R.string.m_sms), Icons.Rounded.Sms, enabled = running) { dialog = Dlg.SMS })
                    add(GlassMenuItem(stringResource(R.string.m_call), Icons.Rounded.Call, enabled = running) { dialog = Dlg.CALL })
                    add(GlassMenuItem(stringResource(R.string.m_battery), Icons.Rounded.BatteryStd, enabled = running) { dialog = Dlg.BATTERY })
                    add(GlassMenuItem(stringResource(R.string.m_install_apk), Icons.Rounded.InstallMobile, enabled = running) {
                        pickApks.launch(arrayOf("application/vnd.android.package-archive", "application/octet-stream", "*/*"))
                    })
                    add(GlassMenuItem(stringResource(R.string.m_device), Icons.Rounded.Tune, enabled = running) { dialog = Dlg.DEVICE })
                    add(GlassMenuItem(stringResource(R.string.m_adb), Icons.Rounded.Lan, enabled = running) { dialog = Dlg.ADB })
                    add(GlassMenuItem(stringResource(R.string.m_settings), Icons.Rounded.Settings, enabled = running) {
                        guestAsync("am start -a android.settings.SETTINGS")
                    })
                }
                add(GlassMenuItem(stringResource(R.string.m_screenshot), Icons.Rounded.Screenshot, enabled = state == GuestVm.State.RUNNING) { screenshot() })
                add(GlassMenuItem(
                    stringResource(if (recOn.value) R.string.m_record_stop else R.string.m_record),
                    Icons.Rounded.Videocam,
                    enabled = state == GuestVm.State.RUNNING || recOn.value,
                ) { toggleRecording() })
                add(GlassMenuItem(
                    stringResource(if (vncOn.value) R.string.m_vnc_stop else R.string.m_vnc),
                    Icons.Rounded.Cast,
                ) { toggleVnc() })
                add(GlassMenuItem(stringResource(R.string.m_reset_menu_pos), Icons.Rounded.RestartAlt) {
                    menuOffset = Offset.Zero; saveMenuOffset()
                })
                add(GlassMenuItem(stringResource(R.string.m_reboot), Icons.Rounded.RestartAlt) { rebootVm() })
                add(GlassMenuItem(
                    stringResource(if (vm.recoveryMode) R.string.m_reboot_system else R.string.m_reboot_recovery),
                    Icons.Rounded.RestartAlt,
                    enabled = vm.recoveryMode || app.andropaw.core.RecoveryImage.installed(vm.paths),
                ) { rebootVm(!vm.recoveryMode) })
                add(GlassMenuItem(stringResource(R.string.shutdown), Icons.Rounded.Close) { stopVm() })
            }


            when (dialog) {
                Dlg.SMS -> SmsDialog { dialog = Dlg.NONE }
                Dlg.CALL -> CallDialog { dialog = Dlg.NONE }
                Dlg.BATTERY -> BatteryDialog { dialog = Dlg.NONE }
                Dlg.ADB -> AdbDialog { dialog = Dlg.NONE }
                Dlg.DEVICE -> DeviceDialog { dialog = Dlg.NONE }
                Dlg.NONE -> {}
            }

            // 大量进程退出的询问条（工具栏上方）：等待自救 / 冷重启
            if (massDeathNotice.value) {
                Box(
                    Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                        .padding(bottom = if (vm.settings.navSize == 0) 56.dp else 76.dp, start = 12.dp, end = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MiuixTheme.colorScheme.surfaceContainerHighest)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Column {
                        Text("检测到模拟器进程成批退出", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Spacer(Modifier.height(2.dp))
                        Text("可以让 guest 的 init 自行恢复，或直接冷重启机器。",
                            fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(text = "等待自救", onClick = { massDeathNotice.value = false })
                            TextButton(text = "冷重启", onClick = { massDeathNotice.value = false; rebootVm() })
                        }
                    }
                }
            }

            // 统一浮动工具栏：菜单 + AgentPaw + （可选）Android 按键，位置与旧按键栏一致
            if (!showLog && !showTerm) {
                FloatingToolbar(
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                        .padding(bottom = if (vm.settings.navSize == 0) 3.dp else 6.dp),
                    showKeys = vm.settings.showNavBar,
                    agentEnabled = running,
                    onMenu = { menuOpen = true },
                    onMenuBounds = { r -> menuIcon = r },
                    onAgent = { agentOpen = true },
                )
            }

            if (agentOpen) {
                app.andropaw.agent.AgentChatOverlay(
                    bridge = agentBridge,
                    onDismiss = { agentOpen = false },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(end = 12.dp, bottom = 72.dp),
                )
            }

            // the panel itself, hosted on the page (this box): it gets the whole overlay as its page,
            // so it can take its real size and clamp to the screen, and the button above stays put —
            // its bounds are recorded in root coordinates, the panel speaks this box's own
            GlassDropdownPopup(
                show = menuOpen,
                onDismissRequest = { menuOpen = false },
                anchorBounds = menuIcon.translate(-menuPageOrigin.x, -menuPageOrigin.y),
                backdrop = null,
            ) {
                menuItems.forEach { item ->
                    GlassPopupItem(
                        text = item.text,
                        icon = item.icon,
                        enabled = item.enabled,
                        onClick = { menuOpen = false; item.onClick() },
                    )
                }
            }
        }
    }

    /**
     * 统一的浮动工具栏：菜单按钮 + AgentPaw 按钮，Android 按键栏开启时在后面接上按键。
     * 位置固定在旧的按键栏处（底部居中）；按键栏关闭时只剩菜单与 Agent 两个按钮。
     */
    @Composable
    private fun FloatingToolbar(
        modifier: Modifier,
        showKeys: Boolean,
        agentEnabled: Boolean,
        onMenu: () -> Unit,
        onMenuBounds: (androidx.compose.ui.geometry.Rect) -> Unit,
        onAgent: () -> Unit,
    ) {
        val compact = vm.settings.navSize == 0
        val keys = remember(showKeys) {
            if (!showKeys) emptyList()
            else vm.settings.navKeys.split(',').filter { it in app.andropaw.core.VmSettings.NAV_ALL }
        }
        val btn = if (compact) 36.dp else 48.dp
        Box(modifier.clip(CircleShape).background(MiuixTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f))) {
            Row(
                Modifier.padding(horizontal = if (compact) 6.dp else 10.dp, vertical = if (compact) 2.dp else 6.dp),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 菜单
                Box(
                    Modifier.size(btn).clip(CircleShape).clickable { onMenu() }
                        .onGloballyPositioned { onMenuBounds(it.boundsInRoot()) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.MoreVert, stringResource(R.string.menu),
                        Modifier.size(if (compact) 18.dp else 22.dp), tint = MiuixTheme.colorScheme.onSurface)
                }
                // AgentPaw
                Box(
                    Modifier.size(btn).clip(CircleShape).clickable(enabled = agentEnabled) { if (agentEnabled) onAgent() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.AutoAwesome, stringResource(R.string.agent_chat_title),
                        Modifier.size(if (compact) 18.dp else 22.dp),
                        tint = if (agentEnabled) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
                if (keys.isNotEmpty()) {
                    Box(Modifier.size(width = 1.dp, height = if (compact) 18.dp else 24.dp)
                        .background(MiuixTheme.colorScheme.onSurface.copy(alpha = 0.15f)))
                    for (k in keys) NavKey(k, btn)
                }
            }
        }
    }

    @Composable
    private fun NavKey(k: String, size: androidx.compose.ui.unit.Dp) {
        var pressed by remember { mutableStateOf(false) }
        val bg by androidx.compose.animation.animateColorAsState(
            if (pressed) MiuixTheme.colorScheme.primary.copy(alpha = 0.28f) else Color.Transparent, label = "key")
        val code = when (k) {
            "back" -> InputService.KEY_BACK; "home" -> InputService.KEY_HOME; "menu" -> InputService.KEY_MENU
            "search" -> InputService.KEY_SEARCH; "volup" -> InputService.KEY_VOLUMEUP; "voldown" -> InputService.KEY_VOLUMEDOWN
            "power" -> InputService.KEY_POWER; else -> 0
        }
        val step = with(androidx.compose.ui.platform.LocalDensity.current) { 18.dp.toPx() }
        Box(Modifier.size(size).background(bg, CircleShape).pointerInput(k) {
            awaitEachGesture {
                val down = awaitFirstDown()
                pressed = true
                val t0 = android.os.SystemClock.uptimeMillis()
                if (k == "home") vm.input.home(true)
                else if (code != 0) vm.input.key(code, true)
                // the D-pad button is a little trackball: sliding the finger sends arrows, a tap selects
                var acc = Offset.Zero
                var moved = false
                while (true) {
                    val e = awaitPointerEvent()
                    val c = e.changes.firstOrNull { it.id == down.id } ?: break
                    if (k == "dpad") {
                        acc += c.position - c.previousPosition
                        while (kotlin.math.abs(acc.x) >= step || kotlin.math.abs(acc.y) >= step) {
                            moved = true
                            if (kotlin.math.abs(acc.x) >= kotlin.math.abs(acc.y)) {
                                vm.input.press(if (acc.x > 0) InputService.KEY_RIGHT else InputService.KEY_LEFT, 30)
                                acc = Offset(acc.x - step * kotlin.math.sign(acc.x), acc.y)
                            } else {
                                vm.input.press(if (acc.y > 0) InputService.KEY_DOWN else InputService.KEY_UP, 30)
                                acc = Offset(acc.x, acc.y - step * kotlin.math.sign(acc.y))
                            }
                        }
                    }
                    c.consume()
                    if (!c.pressed) break
                }
                pressed = false
                if (k == "home") vm.input.home(false)
                else if (code != 0) vm.input.key(code, false)
                else if (k == "recents") vm.input.recents(vm.img.api, vm.img.skin)
                else if (k == "dpad" && !moved) vm.input.press(InputService.KEY_CENTER, if (android.os.SystemClock.uptimeMillis() - t0 > 500) 700 else 80)
            }
        }, contentAlignment = Alignment.Center) {
            val d = navKeyLabel(k)
            val m = Modifier.size(if (size < 40.dp) 20.dp else 24.dp)
            when (k) {
                "back" -> Icon(Icons.AutoMirrored.Rounded.ArrowBack, d, m)
                "home" -> Icon(Icons.Rounded.Circle, d, m)
                "recents" -> Icon(Icons.Rounded.CropSquare, d, m)
                "menu" -> Icon(Icons.Rounded.Menu, d, m)
                "search" -> Icon(Icons.Rounded.Search, d, m)
                "volup" -> Icon(Icons.Rounded.VolumeUp, d, m)
                "voldown" -> Icon(Icons.Rounded.VolumeDown, d, m)
                "power" -> Icon(Icons.Rounded.PowerSettingsNew, d, m)
                else -> Icon(Icons.Rounded.Gamepad, d, m)
            }
        }
    }

    /**
     * The guest's LED above its screen: Sony's illumination bar as a glowing strip, other phones' notification LED as
     * a dot. Blinks with the guest's on/off times.
     */
    @Composable
    private fun LedIndicator(l: app.andropaw.core.Lights.Led, modifier: Modifier) {
        var on by remember(l) { mutableStateOf(true) }
        LaunchedEffect(l) {
            if (l.onMs > 0 && l.offMs > 0) while (true) { on = true; delay(l.onMs.toLong()); on = false; delay(l.offMs.toLong()) }
        }
        val alpha by androidx.compose.animation.core.animateFloatAsState(if (on) 1f else 0.08f,
            androidx.compose.animation.core.tween(if (on) 120 else 260), label = "led")
        val c = Color(0xff000000.toInt() or (l.color and 0xffffff))
        val sony = vm.img.skin.contains("Xperia", true) || vm.img.brand.contains("Sony", true)
        val density = androidx.compose.ui.platform.LocalDensity.current
        val y = with(density) { (guestTopPx.toDp() - 10.dp).coerceAtLeast(4.dp) }
        if (sony) {
            Box(
                modifier.offset(y = y)
                    .width(160.dp)
                    .height(6.dp)
                    .alpha(alpha)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                c.copy(alpha = 0.4f),
                                c,
                                c.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    )
            )
        } else {
            Box(
                modifier.offset(y = y).size(14.dp)
                    .alpha(alpha)
                    .background(androidx.compose.ui.graphics.Brush.radialGradient(listOf(c, c.copy(alpha = 0.55f), Color.Transparent)), CircleShape)
            )
        }
    }

    @Composable
    private fun BootCard(seconds: Long) {
        Card(
            modifier = Modifier.widthIn(max = 360.dp).padding(24.dp),
            insideMargin = androidx.compose.foundation.layout.PaddingValues(24.dp),
        ) {
            Column(Modifier.padding(0.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (state == GuestVm.State.FAILED) {
                    Text(stringResource(R.string.failed_start), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text(vm.failure ?: "", fontSize = 14.sp, color = MiuixTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(text = stringResource(R.string.close), onClick = { stopVm() })
                        Button(onClick = { thread { vm.stop(); vm.boot() } }) { Text(stringResource(R.string.retry)) }
                    }
                } else {
                    CircularProgressIndicator(size = 64.dp)
                    Spacer(Modifier.height(16.dp))
                    Text(vm.img.name, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(vm.img.displayVersion, fontSize = 14.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    Spacer(Modifier.height(12.dp))
                    val phase = when (state) {
                        GuestVm.State.PREPARING -> stringResource(R.string.st_preparing)
                        GuestVm.State.BOOTING -> stringResource(R.string.st_booting, seconds.toInt())
                        GuestVm.State.STOPPING -> stringResource(R.string.st_stopping)
                        else -> "…"
                    }
                    Text(phase, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MiuixTheme.colorScheme.primary)
                    if (vm.img.bootCount == 0 && state == GuestVm.State.BOOTING) {
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.first_boot_hint),
                            fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(logLines.lastOrNull()?.substringAfter(' ') ?: "", style = Mono, maxLines = 2,
                        overflow = TextOverflow.Ellipsis, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                }
            }
        }
    }

    @Composable
    private fun LogPanel(onClose: () -> Unit) {
        var guestTab by remember { mutableStateOf(false) }
        val guestLines = remember { mutableStateListOf<String>() }
        LaunchedEffect(guestTab) {
            while (guestTab) {
                val recs = GuestLog.tail(vm.paths.root, 128 * 1024).takeLast(300)
                guestLines.clear(); guestLines.addAll(recs.map { "${it.prio} ${it.tag}: ${it.msg}" })
                delay(1500)
            }
        }
        val list = if (guestTab) guestLines else logLines
        Box(Modifier.fillMaxSize().background(Color(0xF0101410))) {
            Column(Modifier.statusBarsPadding().navigationBarsPadding().imePadding().padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.log), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    PanelIconButton(Icons.Rounded.ContentCopy, stringResource(R.string.log_copy)) { copyLog(list.toList()) }
                    PanelIconButton(Icons.Rounded.Download, stringResource(R.string.log_export)) {
                        pendingExport = list.toList(); exportLog.launch("aemu-${vm.img.id}-${if (guestTab) "logcat" else "host"}.log")
                    }
                    PanelIconButton(Icons.Rounded.Close, stringResource(R.string.close), filled = true) { onClose() }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ConnectedButtonSpace)) {
                    ConnectedToggleButton(
                        checked = !guestTab,
                        onCheckedChange = { guestTab = false },
                        shape = connectedButtonShapes(0, 1),
                        modifier = Modifier.weight(1f),
                    ) { checked -> TabLabel(stringResource(R.string.host), checked) }
                    ConnectedToggleButton(
                        checked = guestTab,
                        onCheckedChange = { guestTab = true },
                        shape = connectedButtonShapes(1, 1),
                        modifier = Modifier.weight(1f),
                    ) { checked -> TabLabel(stringResource(R.string.guest_log), checked) }
                }
                Spacer(Modifier.height(8.dp))
                val st = rememberLazyListState()
                LaunchedEffect(list.size) { if (list.isNotEmpty()) st.scrollToItem(list.size - 1) }
                SelectionContainer {
                    LazyColumn(state = st, modifier = Modifier.fillMaxSize()) {
                        items(list) { Text(it, style = Mono, color = Color(0xFFCFE8CF)) }
                    }
                }
            }
        }
    }

    // shell terminal into the guest: its lines survive closing and reopening the panel
    private val termLines = mutableStateListOf<String>()
    private val termHistory = ArrayList<String>()

    @Composable
    private fun TerminalPanel(onClose: () -> Unit) {
        var cmd by remember { mutableStateOf("") }
        var busy by remember { mutableStateOf(false) }
        var histPos by remember { mutableStateOf(-1) }
        fun run() {
            val c = cmd.trim()
            if (c.isEmpty() || busy) return
            cmd = ""; histPos = -1
            termHistory.remove(c); termHistory.add(c)
            if (c == "clear") { termLines.clear(); return }
            termLines.add("$ $c")
            busy = true
            thread {
                val out = runCatching { vm.guestShell(c, 180_000) }.getOrElse { it.toString() }
                runOnUiThread {
                    termLines.addAll(out.trimEnd('\n').lines())
                    while (termLines.size > 3000) termLines.removeAt(0)
                    busy = false
                }
            }
        }
        Box(Modifier.fillMaxSize().background(Color(0xF0101410))) {
            Column(Modifier.statusBarsPadding().navigationBarsPadding().imePadding().padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.m_terminal), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (busy) CircularProgressIndicator(Modifier.size(32.dp))
                    PanelIconButton(Icons.Rounded.ContentCopy, stringResource(R.string.log_copy)) { copyLog(termLines.toList()) }
                    PanelIconButton(Icons.Rounded.Close, stringResource(R.string.close), filled = true) { onClose() }
                }
                Spacer(Modifier.height(8.dp))
                val st = rememberLazyListState()
                LaunchedEffect(termLines.size) { if (termLines.isNotEmpty()) st.scrollToItem(termLines.size - 1) }
                SelectionContainer(Modifier.weight(1f)) {
                    LazyColumn(state = st, modifier = Modifier.fillMaxSize()) {
                        items(termLines) { Text(it, style = Mono, color = if (it.startsWith("$ ")) Color(0xFF9AD0FF) else Color(0xFFCFE8CF)) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PanelIconButton(Icons.Rounded.History, null) {
                        if (termHistory.isNotEmpty()) {
                            histPos = if (histPos <= 0) termHistory.size - 1 else histPos - 1
                            cmd = termHistory[histPos]
                        }
                    }
                    TextField(cmd, { cmd = it }, modifier = Modifier.weight(1f), singleLine = true, label = "",
                        textStyle = Mono,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Send,
                            autoCorrectEnabled = false, capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.None),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = { run() }))
                    Spacer(Modifier.width(8.dp))
                    PanelIconButton(Icons.AutoMirrored.Rounded.Send, stringResource(R.string.m_send), filled = true,
                        enabled = !busy && cmd.isNotBlank()) { run() }
                }
            }
        }
    }

    /** A small circular icon button for the log/terminal panels' toolbars. */
    @Composable
    private fun PanelIconButton(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        contentDescription: String?,
        filled: Boolean = false,
        enabled: Boolean = true,
        onClick: () -> Unit,
    ) {
        Box(
            Modifier
                .padding(horizontal = 4.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(if (filled) MiuixTheme.colorScheme.secondaryContainer else Color.Transparent)
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription)
        }
    }

    /** Tab caption for the log panel's connected toggle pair. */
    @Composable
    private fun TabLabel(label: String, checked: Boolean) {
        Text(
            label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 13.sp,
            color = if (checked) MiuixTheme.colorScheme.onPrimaryContainer else MiuixTheme.colorScheme.onSurface,
        )
    }

    private var pendingExport: List<String> = emptyList()
    private val exportLog = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri == null) return@registerForActivityResult
        val lines = pendingExport
        thread {
            val ok = runCatching { contentResolver.openOutputStream(uri)!!.bufferedWriter().use { w -> lines.forEach { w.write(it); w.write("\n") } } }.isSuccess
            runOnUiThread { Toast.makeText(this, if (ok) R.string.log_exported else R.string.log_export_failed, Toast.LENGTH_SHORT).show() }
        }
    }

    private fun copyLog(lines: List<String>) {
        val cm = getSystemService(ClipboardManager::class.java)
        cm.setPrimaryClip(ClipData.newPlainText("AndroPaw log", lines.joinToString("\n")))
        Toast.makeText(this, R.string.log_copied, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_ID = "id"
        const val EXTRA_RECOVERY = "recovery"
        /** how long after returning the screen is checked for frames (see [postResumeCheck]) */
        private const val RESUME_CHECK_MS = 8_000L
        fun start(ctx: Context, id: String, recovery: Boolean = false) {
            ctx.startActivity(Intent(ctx, VmActivity::class.java).putExtra(EXTRA_ID, id).putExtra(EXTRA_RECOVERY, recovery)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
