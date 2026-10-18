package app.aemu.core

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.system.Os
import android.system.OsConstants
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.RandomAccessFile

/**
 * Подгонка дерева прошивки под запуск без ядра и железа.
 *  - [sanitize] выполняется один раз после импорта: убирает вендорские драйверы, которые полезли бы
 *    в несуществующее железо (GPU, композитор, OMX, датчики, камера…). Файлы не удаляются —
 *    переносятся в /system/.aemu-parked, чтобы можно было вернуть.
 *  - [fixup] выполняется перед каждым запуском: кладёт файлы движка, создаёт узлы /dev и /sys,
 *    каталоги, которые создаёт init, восстанавливает владельцев и т.п.
 */
class TreeFixer(
    private val ctx: Context,
    private val paths: VmPaths,
    private val img: GuestImage,
    private val log: (String) -> Unit,
) {
    private val root get() = paths.root
    private val engine get() = img.engine
    private val kernel get() = Kernel64.forApi(img.api)

    // ---------------------------------------------------------------- однократно после импорта

    fun sanitize() {
        val parked = File(root, "system/.aemu-parked").apply { mkdirs() }
        var n = 0
        fun park(f: File) {
            if (!f.exists() && !isLink(f)) return
            val rel = f.relativeTo(root).path.replace('/', '#')
            val dst = File(parked, rel)
            dst.delete()
            if (f.renameTo(dst)) n++
        }
        // 1. HAL-модули: оставляем только нейтральные *.default.so и безвредные классы
        val keepClasses = setOf("audio_policy", "local_time", "power", "keystore", "consumerir", "vibrator", "memtrack")
        val parkClasses = setOf(
            "gralloc", "hwcomposer", "copybit", "overlay", "sensors", "camera", "gps", "nfc", "nfc_nci",
            "lights", "audio", "fm", "bluetooth", "hdmi", "tv", "display", "ir", "wlan", "gpu", "fingerprint",
        )
        for (dir in listOf("system/lib/hw", "vendor/lib/hw", "system/vendor/lib/hw")) {
            File(root, dir).listFiles()?.forEach { f ->
                val name = f.name
                if (!name.endsWith(".so")) return@forEach
                val cls = name.substringBefore('.')
                val variant = name.removeSuffix(".so").substringAfter('.', "")
                val isDefault = variant == "default" || variant == "goldfish"
                when {
                    // HIDL passthrough implementations (android.hidl.memory@1.0-impl.so: 32-bit audioserver maps its
                    // effect buffers through it) are not hardware modules
                    name.contains('@') && dir == "system/lib/hw" -> {}
                    // родной gralloc.default прошивки работает с fb0, который эмулирует qemu, — оставляем
                    cls == "gralloc" && isDefault -> {}
                    // звук даёт движок
                    cls == "gralloc" || (cls == "audio" && variant == "primary.default") -> park(f)
                    cls == "audio" && (variant.startsWith("a2dp") || variant.startsWith("usb") || variant.startsWith("r_submix")) -> {}
                    cls in keepClasses && isDefault -> {}
                    cls == "audio_policy" -> if (!isDefault) park(f)
                    cls in parkClasses -> park(f)
                    !isDefault && cls !in keepClasses -> park(f)
                    !isDefault -> park(f)
                }
            }
        }
        // 2. вендорские драйверы OpenGL: их место займёт GL-мост
        for (dir in listOf("system/lib/egl", "vendor/lib/egl", "system/vendor/lib/egl")) {
            File(root, dir).listFiles()?.forEach { f ->
                val nm = f.name
                val vendorGl = (nm.startsWith("libEGL_") || nm.startsWith("libGLESv1_CM_") || nm.startsWith("libGLESv2_") ||
                    nm.startsWith("libGLESv3_") || (nm.startsWith("libGLES_") && nm != "libGLES_android.so") ||
                    nm.startsWith("libq3dtools") || nm.startsWith("eglsubAndroid") || nm.startsWith("libRBEGL") || nm.startsWith("libRBGLES"))
                if (vendorGl) park(f)
            }
        }
        // 3. аппаратные кодеки OMX переполняют стек OMXMaster и роняют mediaserver
        for (rel in listOf("system/lib/libstagefrighthw.so", "vendor/lib/libstagefrighthw.so", "system/vendor/lib/libstagefrighthw.so")) park(File(root, rel))
        // 4. заслонки ядра: /dev/binder как каталог, группы планировщика
        File(root, "dev/binder").takeIf { it.isDirectory }?.let { wipe(it) }
        File(root, "dev/cpuctl").takeIf { it.exists() }?.let { wipe(it) }
        log("firmware prep: vendor modules set aside: $n")
    }

    // ---------------------------------------------------------------- перед каждым запуском

    /**
     * Precompiled code of 5.0+ system apps and of the framework (the odex files in oat/arm and framework/arm) is the only copy: the jars
     * and apks are stripped. installd's dexopt on a fresh /data replaced or unlinked those files inside /system
     * (a real /system is read-only), and the next boot died in system_server with "No original dex files found".
     * Taking the write bit away makes such writes fail harmlessly; ImageStore.wipe gives it back before deleting.
     */
    private fun protectOat() {
        val sys = File(root, "system")
        var n = 0
        fun lock(d: File) {
            d.listFiles()?.forEach { f -> if (f.isFile) { if (f.canWrite()) { f.setWritable(false, false); n++ } } else if (f.isDirectory) lock(f) }
            d.setWritable(false, false)
        }
        File(sys, "framework").listFiles()?.filter { it.isDirectory && (it.name == "oat" || it.name == "arm" || it.name == "arm64") }?.forEach { lock(it) }
        for (top in listOf("app", "priv-app")) File(sys, top).listFiles()?.forEach { app ->
            File(app, "oat").takeIf { it.isDirectory }?.let { lock(it) }
        }
        if (n > 0) log("oat files write-protected: $n")
    }

    fun fixup(owners: Boolean = true) {
        if (owners) seedOwners()
        installEngineFiles()
        protectOat()
        fixThemeXml()
        parkWhetstone()
        parkQcomServices()
        swapMtkAudioHal()
        eglConfig(img.settings.gpu)
        makeDataDirs()
        miuiSdkFiles()
        makeUserZeroLink()
        makeDevNodes()
        rootLinks()
        fixApex()
        bpfMapNotes()
        aconfigStorage()
        kernelPolicyUnread()
        makeSysNodes()
        makeProcMounts()
        resolvConf()
        caCerts()
        audioPolicy()
        qtaguid()
        if (engine == Engine.N64 && kernel.webView64) for (dir in listOf("product/app", "system/app", "system/product/app", "system_ext/app"))
            File(root, dir).listFiles().orEmpty().filter { it.isDirectory && it.name.contains("webview", true) }
                .flatMap { it.listFiles().orEmpty().filter { f -> f.isFile && f.name.endsWith(".apk") } }
                .forEach { WebView64.prefer64(it, log) }
        vendorChecks()
        scriptShebangs()
        samsungEfs()
        keyLayout()
        for (n in LOGS) File(root, "dev/log/$n").let { if (!it.isFile) { it.parentFile?.mkdirs(); it.createNewFile() } }
        makeFb()
    }

    /**
     * Скрипты am/pm/input/monkey… в старых прошивках начинаются с «# Script…» без #!. Ядро на такой
     * execve отвечает ENOEXEC и mksh запускает файл сам, а qemu вместо этого падает «Exec format error».
     */
    private fun scriptShebangs() {
        for (dir in listOf("system/bin", "system/xbin")) {
            val files = File(root, dir).listFiles() ?: continue
            for (f in files) {
                if (!f.isFile || f.length() > 64_000 || java.nio.file.Files.isSymbolicLink(f.toPath())) continue
                val head = runCatching { f.inputStream().use { s -> ByteArray(2).also { s.read(it) } } }.getOrNull() ?: continue
                if (head[0] != '#'.code.toByte() || head[1] == '!'.code.toByte()) continue
                runCatching { f.writeBytes("#!/system/bin/sh\n".toByteArray() + f.readBytes()) }
            }
        }
    }

    /**
     * Samsung: без /efs/FactoryApp/factorymode = ON прошивка считает себя заводской и поверх всего
     * рисует таблицу «PDA / CSC / RF Cal Date…», а часть служб работает в режиме заводского теста.
     */
    private fun samsungEfs() {
        val efs = File(root, "efs")
        if (!efs.isDirectory) return
        val dir = File(efs, "FactoryApp").apply { mkdirs() }
        for (n in listOf("factorymode", "keystr")) {
            val f = File(dir, n)
            if (runCatching { f.readText().trim() }.getOrNull() != "ON") runCatching { f.writeText("ON") }
        }
    }

    private fun qcomAudioFlinger(): Boolean = runCatching {
        val af = File(root, "system/lib/libaudioflinger.so")
        af.isFile && String(af.readBytes(), Charsets.ISO_8859_1).contains("setFmVolume")
    }.getOrDefault(false)

    /**
     * MediaTek 4.x: libaudioflinger loads the primary HAL from /system/lib/libaudio.primary.default.so
     * (AudioMTKHardware, which needs the MTK sound driver — silent here). Park it as libaudio.mtk.so and put
     * our HAL there; it forwards the DcRemove filter AudioFlinger links from that library to the original.
     */
    private fun swapMtkAudioHal() {
        if (engine != Engine.KK || img.api < 17) return
        val lib = File(root, "system/lib/libaudio.primary.default.so")
        val orig = File(root, "system/lib/libaudio.mtk.so")
        if (!orig.isFile) {
            if (!lib.isFile || !isMtkAudio(root)) return
            if (!lib.renameTo(orig)) return
        }
        val asset = "engines/kk/audio.primary.mtk.so"
        val size = runCatching { EngineAssets.size(ctx, asset) }.getOrDefault(-1L)
        if (lib.isFile && lib.length() == size && sameContent(asset, lib)) return
        runCatching {
            EngineAssets.open(ctx, asset).use { i -> lib.outputStream().use { o -> i.copyTo(o) } }
            lib.setReadable(true, false)
            log("audio: MediaTek HAL replaced with the emulator HAL")
        }.onFailure { log("audio: MediaTek HAL swap failed: ${it.message}") }
    }

    /**
     * MIUI 8 reads system/media/theme/theme_compatibility.xml while zygote preloads classes. Some builds ship it with
     * nested comments ("<!-- <!-- ...") that Android's strict XML parser rejects; ThemeCompatibility and MiuiResources
     * then fail to initialise in every process and the system never finishes booting (black screen). Double hyphens
     * inside comments are separated so the file parses.
     */
    private fun fixThemeXml() {
        val f = File(root, "system/media/theme/theme_compatibility.xml")
        if (!f.isFile || f.length() > 4_000_000) return
        runCatching {
            val s = f.readText()
            if (!s.contains("<!-- <!--")) return
            val out = StringBuilder(s.length)
            var i = 0
            while (true) {
                val a = s.indexOf("<!--", i)
                if (a < 0) { out.append(s, i, s.length); break }
                out.append(s, i, a + 4)
                val e = s.indexOf("-->", a + 4)
                if (e < 0) { out.append(s, a + 4, s.length); break }
                out.append(s.substring(a + 4, e).replace("--", "- -")).append("-->")
                i = e + 3
            }
            f.writeText(out.toString())
            log("theme_compatibility.xml: nested comments fixed")
        }
    }

    /**
     * MIUI 8: ActivityManagerService calls MIUI's Whetstone service (an app process) while holding its own lock, and that
     * process in turn waits for the same lock. The watchdog then restarts system_server over and over and the boot never
     * finishes. Whetstone only does background-power bookkeeping, so its package is set aside.
     */
    private fun parkWhetstone() {
        val apk = File(root, "system/app/Whetstone.apk")
        if (!apk.isFile || !File(root, "system/app/miuisystem.apk").isFile) return
        runCatching {
            val dir = File(root, "system/.aemu-parked").apply { mkdirs() }
            if (apk.renameTo(File(dir, "Whetstone.apk"))) {
                File(root, "data/dalvik-cache/system@app@Whetstone.apk@classes.dex").delete()
                log("MIUI: Whetstone package set aside (deadlock with the activity manager)")
            }
        }
    }

    /**
     * qemu names the input device "dhd-touch"; with no layout of that name the guest falls back to the firmware's
     * Generic.kl / qwerty.kl, where vendors map the codes differently (4.x Generic.kl: 102 = MOVE_HOME, 580 often
     * unmapped), so Home and Recents did nothing on many firmwares. A layout under the device's own name is looked up
     * first by every version from 2.3 on and gives the navigation bar the same codes everywhere.
     */
    private fun keyLayout() {
        val f = File(root, "system/usr/keylayout/dhd-touch.kl")
        val vf = File(root, "vendor/usr/keylayout/dhd-touch.kl")
        val text = buildString {
            append("# AEmulator: navigation bar and hardware keys of the emulator\n")
            append("key ${InputService.KEY_HOME} HOME WAKE\n")
            append("key 172 HOME WAKE\n")
            append("key ${InputService.KEY_BACK} BACK WAKE_DROPPED\n")
            append("key ${InputService.KEY_MENU} MENU WAKE_DROPPED\n")
            append("key ${InputService.KEY_SEARCH} SEARCH WAKE_DROPPED\n")
            append("key ${InputService.KEY_POWER} POWER WAKE\n")
            append("key ${InputService.KEY_VOLUMEDOWN} VOLUME_DOWN WAKE\n")
            append("key ${InputService.KEY_VOLUMEUP} VOLUME_UP WAKE\n")
            append("key 113 VOLUME_MUTE\n")
            append("key 212 CAMERA\n")
            append("key 103 DPAD_UP\nkey 108 DPAD_DOWN\nkey 105 DPAD_LEFT\nkey 106 DPAD_RIGHT\nkey 232 DPAD_CENTER\n")
            // APP_SWITCH exists from 3.0; older layouts reject unknown labels and drop the whole file
            if (img.api >= 11) {
                append("key ${InputService.KEY_APPSELECT} APP_SWITCH\n")
                append("key 254 APP_SWITCH\n")
                append("key 187 APP_SWITCH\n")
            }
        }.let {
            // 5.0 dropped the WAKE flags from the layout syntax: an unknown flag makes the guest reject the whole file
            if (img.api >= 21) it.replace(" WAKE_DROPPED", "").replace(" WAKE", "") else it
        }
        val targets = listOf(
            f, vf,
            File(root, "system/usr/keylayout/Vendor_18d1_Product_0001.kl"),
            File(root, "system/usr/keylayout/AEmulator_Touchscreen.kl")
        )
        for (target in targets) {
            if (target == vf && !File(root, "vendor/usr/keylayout").isDirectory) continue
            if (runCatching { target.readText() }.getOrNull() != text) {
                runCatching { target.parentFile?.mkdirs(); target.writeText(text); target.setReadable(true, false) }
                    .onFailure { log("key layout (${target.name}): ${it.message}") }
            }
        }
        // In Generic.kl and qwerty.kl: ensure HOME, BACK, MENU, and APP_SWITCH are properly mapped
        for (dir in listOf(File(root, "system/usr/keylayout"), File(root, "vendor/usr/keylayout"))) {
            if (!dir.isDirectory) continue
            for (name in listOf("Generic.kl", "qwerty.kl")) {
                val kl = File(dir, name)
                if (kl.isFile) runCatching {
                    var s = kl.readText()
                    var changed = false
                    if (s.contains("key 102   MOVE_HOME")) {
                        s = s.replace("key 102   MOVE_HOME", "key 102   HOME")
                        changed = true
                    } else if (s.contains("key 102 MOVE_HOME")) {
                        s = s.replace("key 102 MOVE_HOME", "key 102 HOME")
                        changed = true
                    }
                    if (!s.contains("APP_SWITCH") && img.api >= 11) {
                        s += "\n# AEmulator navigation recents keys\nkey 580 APP_SWITCH\nkey 187 APP_SWITCH\nkey 254 APP_SWITCH\n"
                        changed = true
                    }
                    if (changed) kl.writeText(s)
                }
            }
        }
    }

    /**
     * Qualcomm builds: QTI permissions, DPM, the location stack, the RIL message tunnel and Dirac audio bind to modem,
     * DSP and data-path daemons that do not exist here; their apps crash in a loop and drag system_server with them.
     */
    private fun parkQcomServices() {
        val parked = File(root, "system/.aemu-parked")
        var n = 0
        // 4.x keeps apps as bare app/Name.apk (+ .odex) instead of app/Name/
        for (rel in QCOM_PARK.flatMap { if (it.endsWith(".xml")) listOf(it) else listOf(it, "$it.apk", "$it.odex") }) {
            val f = File(root, rel)
            if (!f.exists() && !isLink(f)) continue
            parked.mkdirs()
            val dst = File(parked, rel.replace('/', '#'))
            if (dst.exists()) ImageStore.wipe(dst)
            if (f.renameTo(dst)) n++
        }
        if (n > 0) log("Qualcomm: vendor services set aside: $n")
    }

    /** Native arm64 engine: its HALs and libraries are arm64 files from assets/engines/n64-<kernel> (each kernel has its own copy), named "a#b#c" for a/b/c. */
    /** the firmware's libgui checks queued buffers for the handle of Samsung's Exynos gralloc */
    /** some 64-bit HAL of this class is in place (the board's own ones are set aside after import) */
    private fun hasHal(cls: String) = listOf("system/lib64/hw", "vendor/lib64/hw", "system/vendor/lib64/hw")
        .any { d -> File(root, d).list()?.any { it.startsWith("$cls.") && it.endsWith(".so") } == true }

    private fun contains(f: File, text: String): Boolean = runCatching {
        f.isFile && String(f.readBytes(), Charsets.ISO_8859_1).contains(text)
    }.getOrDefault(false)

    private fun exynosLibgui(lib: String): Boolean = runCatching {
        String(File(root, "system/$lib/libgui.so").readBytes(), Charsets.ISO_8859_1).contains("invalid gralloc handle (at %p)")
    }.getOrDefault(false)

    private fun installNative64() {
        // engines/n64-<kernel>: AOSP's own SurfaceFlinger for that release. The vendors' ones (MIUI's blur, Qualcomm's
        // ExSurfaceFlinger) need the GPU's GLES 2 and show no windows on the software renderer. For a Treble release
        // (8.0+) it is the vendor side: the graphics and keymaster HAL services over the plain framebuffer, and the
        // manifest that lists exactly them (a HAL that is listed but not running is waited for without end).
        // engines/n64-gl: SwiftShader, the same for every release.
        var copied = 0
        // the GPU bridge for 32-bit programs (native/glroute): libEGL takes it before the software renderer, and it
        // sends the toolkit's drawing on to that renderer; without the GPU option it is not there at all
        kernel.gpuBridge32?.let { name ->
            val on = img.settings.gpu && File(root, "system/bin/app_process32").isFile
            for ((asset, to) in listOf((if (kernel.gpuBridge32Open) "libGLES_aemu.so" else "libGLES_route.so") to name, "libGLES_split.so" to "libGLES_split.so", "libGLES.so" to "libGLES_bridge.so")) {
                val f = File(root, "system/lib/egl/$to")
                if (on) runCatching {
                    val len = EngineAssets.size(ctx, "engines/kk/$asset")
                    if (isLink(f) || !f.isFile || f.length() != len) {
                        f.delete()
                        EngineAssets.open(ctx, "engines/kk/$asset").use { i -> f.outputStream().use { o -> i.copyTo(o) } }
                        f.setReadable(true, false); f.setExecutable(true, false); copied++
                    }
                } else if (to != name || !isLink(f)) f.delete()
            }
        }
        // 10+ over the 8.1 vendor side: the codec service there is 8.1's, and its minijail takes one @include in a
        // policy; the system image's policy has a second one (code coverage, empty in a release build), and the
        // service gives up on it. That line goes.
        if (kernel.oneIncludePolicy) File(root, "system/etc/seccomp_policy/mediacodec.policy").let { f ->
            runCatching {
                val lines = f.readLines()
                // (11 also names a constant that minijail's table of 8.1 does not have)
                val kept = lines.filterNot { it.startsWith("@include") && "code_coverage" in it }.map { it.replace("MREMAP_MAYMOVE", "1") }
                if (kept != lines) f.writeText(kept.joinToString("\n", postfix = "\n"))
            }
        }
        // A system image with no vendor side of its own (a GSI): libselinux of 8.x opens the platform's and the
        // vendor's context files together once the platform's one is there, and hwservicemanager aborts without
        // the second. The vendor adds no rules here.
        if (kernel.treble) for (n in listOf("hwservice", "file", "property", "seapp", "service")) {
            val vendor = File(root, "vendor/etc/selinux/nonplat_${n}_contexts")
            if (File(root, "system/etc/selinux/plat_${n}_contexts").isFile && !vendor.exists() &&
                !File(root, "vendor/etc/selinux/vendor_${n}_contexts").exists()) runCatching {
                vendor.parentFile?.mkdirs()
                vendor.writeText("")
                vendor.setReadable(true, false)
            }
        }
        // (a firmware with its own vendor side: vendor/build.prop is its file, the engine ships none)
        val ownVendor = kernel.treble && File(root, "vendor/build.prop").isFile
        val layers = ArrayList<String>(kernel.assets)
        if (ownVendor || kernel.treble && kernel.ownVendorUnderGsi) layers += kernel.ownVendorAssets
        if (kernel.treble && !ownVendor) layers += kernel.gsiAssets
        val files = LinkedHashMap<String, String>()         // file name to the asset it comes from: later layers win
        for (dir in layers) for (n in runCatching { EngineAssets.list(ctx, dir) }.getOrNull().orEmpty()) files[n] = "$dir/$n"
        // ...and what such a firmware keeps its own of comes back from where an earlier start put it aside
        if (ownVendor) for (n in kernel.firmwareOwn) {
            val parked = File(root, "system/.aemu-parked/$n")
            val own = File(root, n.replace('#', '/'))
            if (parked.isFile) { own.delete(); if (parked.renameTo(own)) log("${own.name}: the firmware's own file is back") }
            if (own.isFile) files.remove(n)
        }
        if (ownVendor) for (n in kernel.firmwareAside) {
            val own = File(root, n.replace('#', '/'))
            val parked = File(root, "system/.aemu-parked/$n")
            if (own.isFile && !parked.exists()) { parked.parentFile?.mkdirs(); if (own.renameTo(parked)) log("${own.name}: the firmware's own file is set aside") }
        }
        if (ownVendor && kernel.manifestFragmentsAside && "vendor#etc#vintf#manifest.xml" in files) {
            for (f in File(root, "vendor/etc/vintf/manifest").listFiles().orEmpty()) {
                val parked = File(root, "system/.aemu-parked/vendor#etc#vintf#manifest#${f.name}")
                if (kernel.manifestFragmentsKept.any { it in f.name }) continue
                if (f.isFile && !parked.exists()) { parked.parentFile?.mkdirs(); if (f.renameTo(parked)) log("${f.name}: the firmware's own file is set aside") }
            }
        }
        // the odm partition adds fragments of its own for the board's HALs, none of which run here; a declared HAL
        // that never registers is waited for, and one unreadable fragment voids the whole device manifest (ColorOS
        // links network_manifest.xml into /proc/oplusManifest, which only its kernel has)
        if (ownVendor && kernel.odmManifestFragmentsAside && "vendor#etc#vintf#manifest.xml" in files) {
            for (f in File(root, "odm/etc/vintf/manifest").listFiles().orEmpty()) {
                val parked = File(root, "system/.aemu-parked/odm#etc#vintf#manifest#${f.name}")
                val link = java.nio.file.Files.isSymbolicLink(f.toPath())
                if ((f.isFile || link) && !parked.exists()) { parked.parentFile?.mkdirs(); if (f.renameTo(parked)) log("odm ${f.name}: the firmware's own file is set aside") }
            }
        }
        // (a fragment this kernel keeps, set aside by an earlier build of the app, goes back)
        if (ownVendor) for (parked in File(root, "system/.aemu-parked").listFiles().orEmpty()) {
            val name = parked.name.removePrefix("vendor#etc#vintf#manifest#")
            if (name != parked.name && kernel.manifestFragmentsKept.any { it in name }) {
                val own = File(root, "vendor/etc/vintf/manifest/$name")
                if (!own.exists() && parked.renameTo(own)) log("$name: the firmware's own file is back")
            }
        }
        // a file this kernel no longer ships (the kernels part ways: 7.x keeps the firmware's own SurfaceFlinger)
        // goes back from where it was kept aside
        val shipped = files.keys
        val anyKernel = Kernel64.entries.flatMap { k -> listOf("engines/n64-${k.tag}", k.ownVendorAssets).flatMap { runCatching { EngineAssets.list(ctx, it) }.getOrNull().orEmpty().asList() } }.toSet()
        for (parked in File(root, "system/.aemu-parked").listFiles().orEmpty()) {
            if (!parked.isFile || parked.name !in anyKernel || parked.name in shipped) continue
            val dst = File(root, parked.name.replace('#', '/'))
            dst.delete()
            if (parked.renameTo(dst)) log("${dst.name}: the firmware's own file is back")
        }
        // Samsung's libgui (see the gralloc rule below) is not the library AOSP's SurfaceFlinger was built against:
        // the consumer side of the buffer queue is laid out differently and the first queued frame crashes in it.
        // Such firmware keeps its own SurfaceFlinger.
        val ownSf = kernel.exynosHandles && exynosLibgui("lib64")
        // The sound trigger service of the engine only hosts the firmware's own implementation; where there is none (a
        // generic system image) it exits at once, and its manifest entry makes system_server wait for it forever.
        val soundTriggerImpl = File(root, "vendor/lib64/hw").list().orEmpty().any { it.startsWith("android.hardware.soundtrigger@") && it.endsWith("-impl.so") }
        for ((n, asset) in files) {
            if (!soundTriggerImpl && "aemu-soundtrigger" in n) { File(root, n.replace('#', '/')).delete(); continue }
            if (ownSf && (n == "system#bin#surfaceflinger" || n == "system#lib64#libsurfaceflinger.so")) {
                val parked = File(root, "system/.aemu-parked/$n")
                val own = File(root, n.replace('#', '/'))
                if (parked.isFile) { own.delete(); if (parked.renameTo(own)) log("${own.name}: the firmware's own file is back") }
                continue
            }
            // (the 32-bit driver goes next to the 64-bit one: a Treble firmware's libEGL loads drivers in the vendor
            // namespace of the linker, which does not reach /system/lib/egl - "couldn't find an OpenGL ES
            // implementation" in every 32-bit app of MIUI 10)
            val eglDir32 = kernel.eglDir.replace("/lib64/", "/lib/")
            val dst = File(root, n.replace('#', '/').let {
                when {
                    it.startsWith("system/lib64/egl/") -> kernel.eglDir + it.removePrefix("system/lib64/egl")
                    it.startsWith("system/lib/egl/") -> eglDir32 + it.removePrefix("system/lib/egl")
                    else -> it
                }
            })
            val size = runCatching { EngineAssets.size(ctx, asset) }.getOrDefault(-1L)
            if (dst.isFile && size >= 0 && dst.length() == size && sameContent(asset, dst)) continue
            runCatching {
                dst.parentFile?.mkdirs()
                if (isLink(dst)) Os.remove(dst.absolutePath)
                else if (dst.isFile) {          // the firmware's own file is kept aside
                    val parked = File(root, "system/.aemu-parked/$n")
                    parked.parentFile?.mkdirs()
                    if (!parked.exists()) dst.renameTo(parked)
                }
                EngineAssets.open(ctx, asset).use { i -> dst.outputStream().use { o -> i.copyTo(o) } }
                dst.setReadable(true, false); dst.setExecutable(true, false)
                copied++
            }.onFailure { log("$n: ${it.message}") }
        }
        // SwiftShader's libEGL finds its GLES libraries by their own names, so the name this release's libEGL asks
        // for is a link to them
        if (kernel.eglName != "swiftshader") listOf(kernel.eglDir, "system/lib/egl").flatMap { File(root, it).listFiles().orEmpty().toList() }.filter { it.name.endsWith("_swiftshader.so") }.forEach { f ->
            val link = File(f.parentFile, f.name.replace("_swiftshader.so", "_${kernel.eglName}.so"))
            if (!isLink(link)) { link.delete(); runCatching { Os.symlink(f.name, link.path) } }
        }
        // 8.0's 32-bit libEGL looks for the "emulation" driver in system/lib/egl only, where the engine's 32-bit
        // SwiftShader is not (it goes with the other EGL drivers to vendor/lib/egl): links there, under SwiftShader's
        // own names too, by which its libEGL opens its GLES libraries
        if (kernel.emulationDir != null) File(root, "vendor/lib/egl").listFiles().orEmpty().filter { it.name.endsWith("_swiftshader.so") }.forEach { f ->
            for (n in listOf(f.name, f.name.replace("_swiftshader.so", "_emulation.so"))) {
                val link = File(root, "system/lib/egl/$n")
                link.parentFile?.mkdirs()
                if (!isLink(link)) { link.delete(); runCatching { Os.symlink("../../../vendor/lib/egl/${f.name}", link.path) } }
            }
        }
        // A vendor libEGL that still reads egl.cfg (MediaTek on 5.x: "0 1 mtk") asks for the driver by the tag written
        // there, whatever is in the folder: the file names SwiftShader instead (a link under the vendor's tag would
        // load a second copy of each library on 5.x, and SwiftShader's EGL and GLES would not see each other).
        // 32-bit programs find no driver of that name and stay on the software renderer of the first line.
        if (kernel.eglCfgTags) {
            val cfg = File(root, "system/lib/egl/egl.cfg")
            val rom = File(root, "system/lib/egl/egl.cfg.rom")
            if (cfg.isFile) runCatching {
                val lines = cfg.readLines()
                val out = lines.map { l ->
                    val t = l.trim().split(Regex("\\s+"))
                    if (t.size == 3 && !l.trim().startsWith("#") && t[2] != "android") "${t[0]} ${t[1]} swiftshader" else l
                }
                if (out != lines) {
                    if (!rom.isFile) cfg.copyTo(rom)
                    cfg.writeText(out.joinToString("\n", postfix = "\n"))
                    log("egl.cfg: the vendor's driver tag replaced with SwiftShader")
                }
            }
        }
        // Samsung's ART empties /data/dalvik-cache at every start when the cache holds no copy of the boot image
        // ("PruneDalvikCache : File Not Exist"): every app is then compiled again on every boot, minutes of
        // "Optimising app N of M". A phone has that copy from relocating the image at first boot; here the image
        // stays where it was built for (-Xnorelocate), so the firmware's own files are linked into the cache.
        if (kernel.bootImageInCache) for ((lib, isa) in listOf("lib64" to "arm64", "lib" to "arm")) runCatching {
            if (!contains(File(root, "system/$lib/libart.so"), "PruneDalvikCache : File Not Exist")) return@runCatching
            for (ext in listOf("art", "oat")) {
                val src = File(root, "system/framework/$isa/boot.$ext")
                val dst = File(root, "data/dalvik-cache/$isa/system@framework@boot.$ext")
                if (!src.isFile || (dst.isFile && dst.length() == src.length())) continue
                dst.parentFile?.mkdirs()
                dst.delete()
                runCatching { Os.link(src.absolutePath, dst.absolutePath) }.onFailure { src.copyTo(dst, overwrite = true) }
                dst.setReadable(true, false)
                log("dalvik-cache/$isa: boot.$ext of the firmware linked in")
            }
        }
        // Samsung's libgui on Exynos (TouchWiz) takes every queued buffer for one of its own gralloc and writes into
        // the handle; with the default gralloc's handle it writes through a null pointer and SurfaceFlinger dies on
        // the first frame. Where libgui has that check, the kernel's gralloc of the same buffers and Samsung's
        // handle layout goes in front of the firmware's default one (kept next to it as libgralloc_rom.so).
        // Qualcomm's SurfaceFlinger reads its gralloc's flags out of every layer's handle: the same way, the kernel's
        // gralloc with Qualcomm's handle layout goes in front where the firmware's SurfaceFlinger is that one.
        val qcomSf = kernel.qcomHandles && runCatching {
            String(File(root, "system/lib64/libsurfaceflinger.so").readBytes(), Charsets.ISO_8859_1).contains("ExSurfaceFlinger")
        }.getOrDefault(false)
        for ((on, name) in listOf(kernel.exynosHandles to "exynos", kernel.qcomHandles to "qcom")) if (on) for (lib in listOf("lib64", "lib")) runCatching {
            // (a Treble release keeps its gralloc on the vendor side)
            val side = if (kernel.treble) "vendor" else "system"
            val wrap = File(root, "$side/$lib/libgralloc_$name.so")
            val hw = File(root, "$side/$lib/hw/gralloc.default.so")
            val rom = File(root, "$side/$lib/libgralloc_rom.so")
            val gui = File(root, "system/$lib/libgui.so")
            if (!wrap.isFile || !hw.isFile || !gui.isFile) return@runCatching
            val inPlace = hw.length() == wrap.length() && hw.readBytes().contentEquals(wrap.readBytes())
            val wanted = if (name == "exynos") exynosLibgui(lib) else qcomSf
            if (wanted && !inPlace) {
                if (!rom.isFile) hw.renameTo(rom)
                wrap.copyTo(hw, overwrite = true)
                hw.setReadable(true, false)
                log("$lib: gralloc with ${if (name == "exynos") "Samsung" else "Qualcomm"}'s handle layout in front of the firmware's")
            } else if (!wanted && inPlace && rom.isFile) {
                hw.delete(); rom.renameTo(hw)
            }
        }
        // Qualcomm's 32-bit libc of 5.1 looks for the main thread's stack in /proc/self/task/<tid>/maps, a file qemu-arm
        // does not describe to its guest: the program reads the translator's own stack, cut to 32 bits, and ART
        // aborts ("stack variable above the stack end") in patchoat and in the 32-bit zygote. /proc/self/maps is the
        // same list, and that one qemu-arm answers itself; the name in the library is shortened to it.
        if (kernel.taskMapsIsSelfMaps) {
            val libc = File(root, "system/lib/libc.so")
            if (libc.isFile) runCatching {
                val d = libc.readBytes()
                val a = "/proc/self/task/%d/maps".toByteArray() + 0
                val b = "/proc/self/maps".toByteArray()
                val at = (0..d.size - a.size).firstOrNull { i -> a.indices.all { d[i + it] == a[it] } }
                if (at != null) {
                    a.indices.forEach { d[at + it] = if (it < b.size) b[it] else 0 }
                    libc.writeBytes(d)
                    log("libc.so (32-bit): the main thread's stack is read from /proc/self/maps")
                }
            }
        }
        // mediaserver of a 6.0 arm64 firmware is a 32-bit program (it runs through qemu-arm): camera and sound
        // reach the phone through the same 32-bit HALs as on the other engines
        if (File(root, "system/bin/mediaserver").let { it.isFile && !isElf64(it) }) {
            // Samsung's audio_format_t is 64 bits wide (5.1: "%lld not supported", 6.0: "%llx not valid"): its AudioFlinger takes a format from the HAL in two registers
            val audioHal = if (kernel.wideAudioFormat && contains(File(root, "system/lib/libaudioflinger.so"), "HAL format %ll"))
                "engines/kk/audio.primary.fmt64.so" else "engines/kk/audio.primary.aosp.so"
            // a kernel may bring its own build of the 32-bit camera HAL (7.1: a newer module API for Camera.openLegacy)
            val cameraHal = kernel.assets.reversed().map { "$it/system#lib#hw#camera.default.so" }
                .firstOrNull { a -> runCatching { check(EngineAssets.exists(ctx, a)) }.isSuccess } ?: "engines/common/camera.default.so"
            for ((asset, to) in listOf(cameraHal to "system/lib/hw/camera.default.so",
                audioHal to "system/lib/hw/audio.primary.default.so")) {
                val dst = File(root, to)
                val size = runCatching { EngineAssets.size(ctx, asset) }.getOrDefault(-1L)
                if (dst.isFile && size >= 0 && dst.length() == size && sameContent(asset, dst)) continue
                runCatching {
                    dst.parentFile?.mkdirs()
                    val parked = File(root, "system/.aemu-parked/${to.replace('/', '#')}")
                    parked.parentFile?.mkdirs()
                    if (dst.isFile && !parked.exists()) dst.renameTo(parked)
                    EngineAssets.open(ctx, asset).use { i -> dst.outputStream().use { o -> i.copyTo(o) } }
                    dst.setReadable(true, false); copied++
                }.onFailure { log("$to: ${it.message}") }
            }
        }
        // the Java stubs (bluetooth manager, network, tool runner) are dex code: the same jar as for 32-bit guests
        runCatching {
            // (read whole: a jar is stored compressed in the package, and such an asset has no length to ask for)
            val jar = File(root, "system/framework/aemu-stubs.jar")
            val bytes = EngineAssets.open(ctx, "engines/kk/aemu-stubs.jar").use { it.readBytes() }
            if (!jar.isFile || !jar.readBytes().contentEquals(bytes)) {
                jar.writeBytes(bytes)
                jar.setReadable(true, false); copied++
            }
        }.onFailure { log("aemu-stubs.jar: ${it.message}") }
        if (copied > 0) log("engine n64 files updated: $copied")
        // qemu-arm (the firmware's 32-bit programs) asks for the lowest mappable address; the phone hides the real file
        runCatching { File(root, "proc/sys/vm").mkdirs(); File(root, "proc/sys/vm/mmap_min_addr").writeText("65536") }
        runCatching { File(root, "proc/.one/1").mkdirs(); File(root, "proc/.task1").mkdirs(); File(root, "proc/.task1/none").writeText("") }     // what a 32-bit program sees as /proc/self/task
        // the vendor's HALs drive hardware this phone does not have (ION, MDP, the SoC's audio DSP): with them
        // aside, the loader falls back to the *.default ones
        var parked = 0
        for (dir in listOf("system/lib64/hw", "system/vendor/lib64/hw", "system/lib/hw", "system/vendor/lib/hw")) {
            File(root, dir).listFiles()?.forEach { f ->
                val id = f.name.substringBefore('.')
                // (Qualcomm ships its location HAL as gps.default.so: no GPS at all is better than its modem worker)
                if (!f.isFile || (f.name.endsWith(".default.so") && id != "gps") || id !in VENDOR_HALS) return@forEach
                val dst = File(root, "system/.aemu-parked/${f.relativeTo(root).path.replace('/', '#')}")
                dst.parentFile?.mkdirs()
                if (dst.exists()) dst.delete()
                if (f.renameTo(dst)) parked++
            }
        }
        // ...and so does what declares that hardware to the framework: system_server takes "the feature is there but
        // its HAL is not" as fatal for the whole group of services it was starting (8.x: the window manager is in it)
        for (dir in listOf("system/etc/permissions", "vendor/etc/permissions")) for (hw in listOf("consumerir", "fingerprint", "nfc", "nfc.hce", "nfc.hcef")) {
            val f = File(root, "$dir/android.hardware.$hw.xml")
            val dst = File(root, "system/.aemu-parked/${f.relativeTo(root).path.replace('/', '#')}")
            // which ones is the kernel's call: where the firmware's own HAL loads, the declaration has to be there
            val absent = hw in kernel.absentFeatures || (hw == "consumerir" && kernel.irFollowsHal && !hasHal("consumerir"))
            if (!absent) { if (dst.isFile && !f.exists()) dst.renameTo(f); continue }
            if (!f.isFile) continue
            dst.parentFile?.mkdirs()
            if (dst.exists()) dst.delete()
            if (f.renameTo(dst)) parked++
        }
        // Bluetooth is declared in the core hardware list along with everything else, so it cannot be parked as a file:
        // it is declared unavailable instead. 10: with the feature there, system_server turns the adapter on and the
        // Bluetooth app dies in a loop on the missing HCI HAL; without it the engine's own manager stub answers.
        File(root, "system/etc/permissions/aemu-absent.xml").let { f ->
            val un = kernel.absentFeatures.filter { it.startsWith("bluetooth") }
            if (un.isEmpty()) f.delete()
            else f.writeText("<permissions>" + un.joinToString("") { "<unavailable-feature name=\"android.hardware.$it\" />" } + "</permissions>")
        }
        // Qualcomm location core loads its modem (QMI) back end by name and crashes inside it without a modem;
        // with the back end gone it keeps its built-in do-nothing one
        for (rel in listOf("system/lib64/libloc_api_v02.so", "system/vendor/lib64/libloc_api_v02.so", "system/lib/libloc_api_v02.so", "system/vendor/lib/libloc_api_v02.so")) {
            val f = File(root, rel)
            if (!f.isFile) continue
            val dst = File(root, "system/.aemu-parked/${rel.replace('/', '#')}")
            dst.parentFile?.mkdirs()
            if (f.renameTo(dst)) parked++
        }
        // the vendor's RenderScript driver goes straight to its GPU; without it libRS computes on the CPU
        for (dir in listOf("system/lib64", "system/vendor/lib64", "system/lib", "system/vendor/lib")) {
            File(root, dir).listFiles()?.filter { it.name.startsWith("libRSDriver_") }?.forEach { f ->
                val dst = File(root, "system/.aemu-parked/${f.relativeTo(root).path.replace('/', '#')}")
                dst.parentFile?.mkdirs()
                if (f.renameTo(dst)) parked++
            }
        }
        // GLES 1/2/3 comes from SwiftShader (engines/n64-<api>): libEGL scans egl/ for lib<API>_*.so, so the GPU
        // vendor's drivers step aside. The 32-bit side keeps the stock software renderer under the exact name libGLES.so.
        if (File(root, kernel.eglLib).isFile) {
            for (dir in listOf("system/lib64/egl", "system/vendor/lib64/egl", "system/vendor/lib/egl").filter { !kernel.treble || it != "system/lib64/egl" }) {
                File(root, dir).listFiles()?.forEach { f ->
                    if (!f.isFile || isLink(f) || kernel.isOwnSuffix(f.name) || f.name == "libGLES_android.so") return@forEach
                    val dst = File(root, "system/.aemu-parked/${f.relativeTo(root).path.replace('/', '#')}")
                    dst.parentFile?.mkdirs()
                    if (dst.exists()) dst.delete()
                    if (f.renameTo(dst)) parked++
                }
            }
            val egl32 = File(root, "system/lib/egl")
            // an image that ran on the 4.4 engine before has our GL shim under the renderer's name
            File(egl32, "libGLES_android.so.sw").let { sw ->
                if (sw.isFile) { File(egl32, "libGLES_android.so").delete(); sw.renameTo(File(egl32, "libGLES_android.so")) }
            }
            val bridge32 = img.settings.gpu && kernel.gpuBridge32 != null
            if (File(egl32, "libGLES_android.so").isFile && !bridge32) {
                for (n in listOf("libGLES_aemu.so", "libGLES_bridge.so", "libEGL_aemu.so", "libGLESv1_CM_aemu.so", "libGLESv2_aemu.so", "libGLES.so"))
                    File(egl32, n).let { if (isLink(it)) Os.remove(it.absolutePath) else it.delete() }
                // a kernel that ships SwiftShader for 32-bit programs too: libEGL takes libGLES.so before anything
                // else in the directory, so the software renderer does not get that name
                if (!File(egl32, "libGLESv2_swiftshader.so").isFile) runCatching { Os.symlink("libGLES_android.so", File(egl32, "libGLES.so").path) }
            }
        }
        if (parked > 0) log("vendor HALs set aside: $parked")
    }

    private val VENDOR_HALS = setOf("gralloc", "hwcomposer", "copybit", "lights", "sensors", "audio", "power", "memtrack",
        "camera", "gps", "fingerprint", "gxfingerprint", "consumerir", "bluetooth", "nfc", "vibrator", "keystore", "gatekeeper")

    private fun installEngineFiles() {
        val copies = when (engine) {
            Engine.KK -> listOf(
                "libashmemshim.so" to "system/lib/libashmemshim.so",
                "libGLES.so" to "system/lib/egl/libGLES_bridge.so",
                "audio.primary.default.so" to "system/lib/hw/audio.primary.default.so",
                "gralloc.default.so" to "system/lib/hw/gralloc.default.so",
                "aemu-stubs.jar" to "system/framework/aemu-stubs.jar",
            )
            Engine.GB -> listOf(
                "libashmemshim.so" to "system/lib/libashmemshim.so",
                "libGLES_dhd.so" to "system/lib/egl/libGLES_dhd.so",
                "gralloc.default.so" to "system/lib/hw/gralloc.default.so",
            )
            Engine.N64 -> return installNative64()
        }
        File(root, "system/bin/fbpaint").delete()
        // a static network debugging tool shipped by older versions; nothing runs it
        File(root, "system/bin/netprobe").delete()
        // Widevine L3 aborts on its first session under the translator (6.0: right after "Could not read
        // /data/mediadrm/IDM0/ay64.dat"); up to 6.0 it lives in mediaserver, which takes camera and audio down with it.
        // Without the plugin MediaDrm says Widevine is not supported and players fall back to clear content.
        if (engine == Engine.KK) for (d in listOf("system/vendor/lib/mediadrm", "vendor/lib/mediadrm")) {
            val wv = File(root, "$d/libwvdrmengine.so")
            if (wv.isFile && !isLink(wv)) {
                val parked = File(root, "system/.aemu-parked/${d.replace('/', '#')}#libwvdrmengine.so")
                parked.parentFile?.mkdirs(); parked.delete()
                if (wv.renameTo(parked)) log("Widevine plugin set aside (aborts under the translator)")
            }
        }
        // образы, подготовленные старой версией: вернуть родной gralloc.default из «парковки»
        val parkedGralloc = File(root, "system/.aemu-parked/system#lib#hw#gralloc.default.so")
        if (parkedGralloc.isFile) {
            val g = File(root, "system/lib/hw/gralloc.default.so")
            g.delete(); parkedGralloc.renameTo(g)
        }
        // gralloc.default, который выделяет память через ION (MediaTek и др.), без ядра не работает:
        // устройство выделения получается пустым и SurfaceFlinger падает — ставим gralloc движка
        val romGralloc = File(root, "system/lib/hw/gralloc.default.so")
        if (engine == Engine.KK && romGralloc.isFile && romGralloc.length() < 2_000_000) {
            val text = runCatching { String(romGralloc.readBytes(), Charsets.ISO_8859_1) }.getOrDefault("")
            if (text.contains("libion.so") || text.contains("ion_alloc")) {
                val parked = File(root, "system/.aemu-parked/system#lib#hw#gralloc.default.so.ion")
                parked.parentFile?.mkdirs()
                if (romGralloc.renameTo(parked)) log("firmware gralloc uses ION, replaced with engine gralloc")
            }
        }
        var copied = 0
        // 7.0+ libEGL on a qemu kernel (qemu.gles=1) only tries the "emulation" driver name
        val nougat = if (img.api >= 24 && copies.any { it.second == "system/lib/egl/libGLES_bridge.so" })
            listOf("libGLES_split.so" to "system/lib/egl/libGLES_emulation.so") else emptyList()
        // 7.0+ without the GPU bridge: libEGL would still find the bridge by scanning egl/, so it is not there at all
        val noBridge = img.api >= 24 && !img.settings.gpu
        if (noBridge) {
            for (n in listOf("libGLES_bridge.so", "libGLES_emulation.so")) File(root, "system/lib/egl/$n").delete()
            // a scan skips libGLES_android.so; in "vendor software renderer" mode (qemu.gles=2) libEGL asks for
            // libGLES_swiftshader.so, which here is the stock software renderer
            val sw = File(root, "system/lib/egl/libGLES_swiftshader.so")
            if (!sw.exists() && !isLink(sw) && File(root, "system/lib/egl/libGLES_android.so").isFile)
                runCatching { Os.symlink("libGLES_android.so", sw.path) }
        }
        for ((from, to) in (copies + nougat).filter { !noBridge || !it.second.startsWith("system/lib/egl/libGLES_") } +
            listOf("@libaemushim.so" to "system/lib/libaemushim.so", "@lights.default.so" to "system/lib/hw/lights.default.so",
                "@sensors.default.so" to "system/lib/hw/sensors.default.so",
                "@camera.default.so" to "system/lib/hw/camera.default.so")) {
            val dst = File(root, to)
            // gralloc движка — только если в прошивке своего нет
            if (from == "gralloc.default.so" && dst.isFile) continue
            // «@» — общий для всех движков файл
            // звуковой HAL стенда разложен под AudioFlinger HTC; остальным 4.2+ — вариант с раскладкой AOSP.
            // У Samsung свой audio_stream_out (лишние слоты) — с ним AOSP-вариант роняет mediaserver,
            // поэтому там остаётся исходный: выход не открывается, система работает без звука.
            // Samsung 4.3 AudioFlinger uses the KitKat slots (verified on I9300 XXUGNJ2: init_check 0x44,
            // open_output_stream 0x6c, stream write 0x40), so only 4.1–4.2 TouchWiz keeps the stand HAL
            val samsung = img.skin.contains("TouchWiz", true) || img.skin.contains("Samsung", true)
            val htcLike = img.skin.contains("HTC", true)
            val name = when {
                from != "audio.primary.default.so" || htcLike -> from
                // 4.0 has its own audio_hw_device layout; Qualcomm CAF builds add set_fm_volume/open_output_session
                img.api in 14..15 -> if (qcomAudioFlinger()) "audio.primary.ics-qcom.so" else "audio.primary.ics.so"
                img.api >= 16 -> "audio.primary.aosp.so"
                else -> from
            }
            val asset = if (name.startsWith("@")) "engines/common/${name.drop(1)}" else "engines/${engine.id}/$name"
            val size = runCatching { EngineAssets.size(ctx, asset) }.getOrDefault(-1L)
            // одинаковый размер ещё не значит тот же файл (правки движка на месте, варианты HAL) — сверяем CRC
            if (dst.isFile && size >= 0 && dst.length() == size && sameContent(asset, dst)) continue
            runCatching {
                dst.parentFile?.mkdirs()
                if (isLink(dst)) Os.remove(dst.absolutePath)
                EngineAssets.open(ctx, asset).use { i -> dst.outputStream().use { o -> i.copyTo(o) } }
                dst.setReadable(true, false)
                dst.setExecutable(true, false)
                copied++
            }.onFailure { log("${from.removePrefix("@")} missing from engine set: ${it.message}") }
        }
        // Replicate camera.default.so into vendor/lib/hw and 64-bit libraries for modern cameraserver and apps
        val cam32 = File(root, "system/lib/hw/camera.default.so")
        val vcam32 = File(root, "vendor/lib/hw/camera.default.so")
        if (cam32.isFile && File(root, "vendor/lib/hw").isDirectory && (!vcam32.exists() || vcam32.length() != cam32.length())) {
            runCatching { cam32.copyTo(vcam32, overwrite = true) }
        }
        if (File(root, "system/lib64").isDirectory) {
            val cam64 = File(root, "system/lib64/hw/camera.default.so")
            val vcam64 = File(root, "vendor/lib64/hw/camera.default.so")
            runCatching {
                // a kernel of the native engine may bring its own build of the HAL
                val asset64 = (if (engine == Engine.N64) kernel.assets.reversed() else emptyList()).map { "$it/system#lib64#hw#camera.default.so" }
                    .firstOrNull { a -> runCatching { check(EngineAssets.exists(ctx, a)) }.isSuccess } ?: "engines/common/camera.default64.so"
                val size64 = EngineAssets.size(ctx, asset64)
                if (!cam64.exists() || cam64.length() != size64) {
                    cam64.parentFile?.mkdirs()
                    EngineAssets.open(ctx, asset64).use { i -> cam64.outputStream().use { o -> i.copyTo(o) } }
                    cam64.setReadable(true, false)
                }
                if (File(root, "vendor/lib64/hw").isDirectory && (!vcam64.exists() || vcam64.length() != cam64.length())) {
                    cam64.copyTo(vcam64, overwrite = true)
                }
            }
        }
        linkerLibRange()
        // вендорские драйверы RenderScript (Adreno, Mali…) лезут в GPU; без них libRS берёт процессорный
        for (dir in listOf("system/lib", "vendor/lib", "system/vendor/lib")) {
            File(root, dir).listFiles()?.filter { it.name.startsWith("libRSDriver_") }?.forEach { f ->
                val parked = File(root, "system/.aemu-parked/${f.relativeTo(root).path.replace('/', '#')}")
                parked.parentFile?.mkdirs()
                if (f.renameTo(parked)) log("RenderScript: removed vendor driver ${f.name}")
            }
        }
        // netfilter в эмуляторе нет: iptables всегда падает, а netd 4.x (Samsung) считает это фатальным
        // для NetworkManagementService/ConnectivityService — ставим пустую программу, родную прячем
        val trueSize = runCatching { EngineAssets.size(ctx, "engines/common/aemu_true.so") }.getOrDefault(-1L)
        // MIUI: invoke-as runs FirewallService's iptables loop as root; under qemu that shell never sees its
        // children exit, so ConnectivityService (and the whole boot) hangs in CommandLineUtils.waitFor
        val fakes = listOf("bin/iptables", "bin/ip6tables") + if (img.skin.contains("MIUI", true)) listOf("xbin/invoke-as") else emptyList()
        for (rel in fakes) {
            val n = rel.substringAfter('/')
            val f = File(root, "system/$rel")
            if (!f.exists() && !isLink(f) || trueSize < 0 || (!isLink(f) && f.length() == trueSize)) continue
            runCatching {
                val parked = File(root, "system/.aemu-parked/system#${rel.replace('/', '#')}")
                parked.parentFile?.mkdirs()
                if (isLink(f)) Os.remove(f.absolutePath) else if (!parked.exists()) f.renameTo(parked) else f.delete()
                EngineAssets.open(ctx, "engines/common/aemu_true.so").use { i -> f.outputStream().use { o -> i.copyTo(o) } }
                f.setReadable(true, false); f.setExecutable(true, false)
                copied++
            }.onFailure { log("$n not replaced: ${it.message}") }
        }
        // камера 2.3: заглушка вместо вендорской libcamera.so, которая лезет в /dev/msm_camera
        val cam = File(root, "system/lib/libcamera.so")
        val stamp = File(root, "system/lib/libcamera.so.aemu")
        val parkedCam = File(root, "system/.aemu-parked/system#lib#libcamera.so")
        val htc = img.skin.contains("HTC", true)
        // заглушка собрана под интерфейс камеры HTC — у других производителей оставляем родную
        if (!htc && stamp.isFile && parkedCam.isFile) { cam.delete(); parkedCam.renameTo(cam); stamp.delete() }
        if (engine == Engine.GB && htc) {
            if (cam.isFile && !stamp.isFile) {
                runCatching {
                    File(root, "system/.aemu-parked").mkdirs()
                    cam.copyTo(File(root, "system/.aemu-parked/system#lib#libcamera.so"), overwrite = true)
                    EngineAssets.open(ctx, "engines/gb/libcamera.so").use { i -> cam.outputStream().use { o -> i.copyTo(o) } }
                    stamp.writeText("1")
                }
            }
        }
        // 2.3, Qualcomm: the camera HAL needs the modem (ONCRPC) and quits mediaserver without it — a HAL with no cameras
        if (img.api <= 10 && !htc && cam.isFile && !parkedCam.exists() && cam.length() > 4096) {
            val text = runCatching { String(cam.readBytes(), Charsets.ISO_8859_1) }.getOrDefault("")
            if (listOf("libcald", "liboemcamera", "oncrpc").any { text.contains(it) }) runCatching {
                File(root, "system/.aemu-parked").mkdirs()
                cam.copyTo(parkedCam, overwrite = true)
                EngineAssets.open(ctx, "engines/common/libcamera.none.so").use { i -> cam.outputStream().use { o -> i.copyTo(o) } }
                log("camera: the firmware's 2.3 HAL needs the modem, replaced with an empty one (stock kept)")
                copied++
            }
        }
        if (copied > 0) log("engine ${engine.id} files updated: $copied")
    }

    /**
     * The 2.x linker gives libraries that are not prelinked addresses from a fixed range of its own. AOSP's is
     * 0x80000000 - 0x90000000; Sony's 2.3 (Xperia 2011) uses 0x40000000 - 0x50000000, which is exactly where the
     * stand's qemu keeps the guest stack and its own mappings: nothing fits ("cannot map library
     * 'libashmemshim.so'. no vspace available", every service exits with 255). The range is a constant in the
     * linker — base, size, 1 MB step — and is moved to 0x90000000, which Sony's prelink map leaves free.
     */
    private fun linkerLibRange() {
        if (img.api > 10) return
        val f = File(root, "system/bin/linker")
        if (!f.isFile || f.length() > 4_000_000) return
        runCatching {
            val d = f.readBytes()
            val b = java.nio.ByteBuffer.wrap(d).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            var off = 0
            while (off + 12 <= d.size) {
                if (b.getInt(off) == 0x40000000 && b.getInt(off + 4) == 0x10000000 && b.getInt(off + 8) == 0x100000) {
                    b.putInt(off, 0x90000000.toInt())
                    f.writeBytes(d)
                    log("linker: range for non-prelinked libraries moved from 0x40000000 to 0x90000000")
                    break
                }
                off += 4
            }
        }
    }

    /** Направляет загрузчик EGL прошивки на GL-мост (или на программный растеризатор). */
    private fun eglConfig(gpu: Boolean) {
        val dir = File(root, "system/lib/egl").apply { mkdirs() }
        val cfg = File(dir, "egl.cfg")
        val cfgRom = File(dir, "egl.cfg.rom")
        if (!cfgRom.isFile && cfg.isFile) cfg.copyTo(cfgRom, overwrite = true)
        when (engine) {
            Engine.N64 -> {}
            Engine.GB -> cfg.writeText(if (gpu) "0 0 dhd\n" else "0 0 android\n")
            Engine.KK -> {
                // сам мост лежит как libGLES_bridge.so; все имена, которые ищут загрузчики EGL (libGLES.so у 4.4,
                // libGLES_<тег>.so по egl.cfg, раздельные libEGL_/libGLESv*_), занимает переходник libGLES_split:
                // он чинит загрузку текстур, сводит раздельные библиотеки в один мост и даёт SurfaceFlinger ES2
                val bridge = File(dir, "libGLES_bridge.so")
                val sw = File(dir, "libGLES_android.so")
                val swOff = File(dir, "libGLES_android.so.sw")
                val names = listOf("libGLES.so", "libGLES_aemu.so", "libGLES_android.so", "libEGL_aemu.so", "libGLESv1_CM_aemu.so", "libGLESv2_aemu.so")
                if (gpu && bridge.isFile) {
                    if (!swOff.isFile && sw.isFile && sw.length() != splitSize() && sw.length() != assetSize(OPEN_GL)) sw.renameTo(swOff)
                    // test switch: a file "glopen" in the image's directory puts the open bridge (native/glbridge,
                    // armv7) on those names; it stands in front of the shim, which keeps the compositor and hwui
                    val open = openGl32()
                    val asset = if (open) OPEN_GL else "engines/kk/libGLES_split.so"
                    val all = names + (if (img.api >= 24) listOf("libGLES_emulation.so") else emptyList())
                    runCatching {
                        for (n in all) installAsset(asset, File(dir, n))
                        val shim = File(dir, "libGLES_split.so")
                        if (open) installAsset("engines/kk/libGLES_split.so", shim) else shim.delete()
                    }.onFailure { log("GL: shim failed to install: ${it.message}") }
                    if (open) log("GL: open bridge in front of the 4.4 engine's one (glopen)")
                    // часть загрузчиков (Samsung 4.3) берут последнюю строку, остальные — строку с impl=1
                    cfg.writeText("0 0 android\n0 1 aemu\n")
                } else {
                    // программная отрисовка: убираем все наши имена, иначе загрузчик 4.4 всё равно найдёт libGLES.so
                    for (n in names) if (n != "libGLES_android.so") File(dir, n).delete()
                    if (swOff.isFile) { sw.delete(); swOff.renameTo(sw) }
                    cfg.writeText("0 0 android\n")
                }
            }
        }
    }

    private fun isElf64(f: File): Boolean = runCatching {
        f.inputStream().use { i -> val b = ByteArray(5); i.read(b) == 5 && b[4] == 2.toByte() }
    }.getOrDefault(false)

    private fun sameContent(asset: String, f: File): Boolean = runCatching {
        fun crc(i: java.io.InputStream): Long {
            val c = java.util.zip.CRC32(); val b = ByteArray(1 shl 16)
            while (true) { val n = i.read(b); if (n < 0) break; c.update(b, 0, n) }
            return c.value
        }
        EngineAssets.open(ctx, asset).use { crc(it) } == f.inputStream().use { crc(it) }
    }.getOrDefault(false)

    /** Пустые таблицы xt_qtaguid, на которые гостевая прослойка подменяет /proc/net/xt_qtaguid/… */
    /**
     * MediaTek DRVB: кодеки, DRM, камера и даже debuggerd сверяют «платформу» через демон drvbd, а тот
     * читает efuse через /dev/devmap. На эмуляторе проверка проваливается, каждый клиент 10 с ждёт демон,
     * а модуль DRM затем нарочно прыгает на 0xddeeaadd (drmserver падает, MediaPlayer зависает намертво).
     * Проверка отвечает на вызов подписанным ответом, который считает сама библиотека, — поэтому достаточно,
     * чтобы её внутренние проверки вернули «успех»: ответ получится честный.
     */
    private fun vendorChecks() = runCatching {
        val drvb = File(root, "system/lib/libmtk_drvb.so")
        if (drvb.isFile) {
            val n = ElfPatch.returnZero(drvb, setOf("mtk_drvb_basechk", "platform_init", "platform_advchk", "drvb_ext_input"))
            if (n > 0) log("MediaTek DRVB: platform check disabled ($n func.)")
        }
    }.onFailure { log("MediaTek DRVB: failed: ${it.message}") }

    private fun qtaguid() = runCatching {
        val d = File(root, "data/.aemu_qtaguid").apply { mkdirs() }
        d.setReadable(true, false); d.setExecutable(true, false)
        val files = mapOf(
            "stats" to "idx iface acct_tag_hex uid_tag_int cnt_set rx_bytes rx_packets tx_bytes tx_packets " +
                "rx_tcp_bytes rx_tcp_packets rx_udp_bytes rx_udp_packets rx_other_bytes rx_other_packets " +
                "tx_tcp_bytes tx_tcp_packets tx_udp_bytes tx_udp_packets tx_other_bytes tx_other_packets\n",
            "iface_stat_fmt" to "ifname total_skb_rx_bytes total_skb_rx_packets total_skb_tx_bytes total_skb_tx_packets\n",
            "iface_stat_all" to "",
        )
        for ((n, text) in files) File(d, n).apply { writeText(text); setReadable(true, false) }
        // the native arm64 engine shows the image's own /proc files to the guest: the same empty tables in place,
        // with the control file whose presence is what "the kernel does traffic accounting" means to the framework
        if (engine == Engine.N64 && kernel.trafficAccounting) {
            val p = File(root, "proc/net/xt_qtaguid").apply { mkdirs() }
            for ((n, text) in files + ("ctrl" to "")) File(p, n).writeText(text)
            File(root, "proc/net/xt_quota").mkdirs()           // netd writes its data-usage alert threshold here
            File(root, "proc/net/xt_quota/globalAlert").writeText("")
        }
        if (engine == Engine.N64) for ((n, text) in kernel.procFiles) File(root, "proc/$n").let {
            it.parentFile?.mkdirs()
            if (n.endsWith(".gz")) {            // (the kernel's configuration: the text, gzipped as the kernel gives it)
                val gz = java.io.ByteArrayOutputStream().also { o -> java.util.zip.GZIPOutputStream(o).use { z -> z.write(text.toByteArray()) } }.toByteArray()
                if (!it.isFile || !it.readBytes().contentEquals(gz)) it.writeBytes(gz)
            } else if (!it.isFile || it.readText() != text) it.writeText(text)
        }
    }

    private fun assetSize(a: String): Long = runCatching { EngineAssets.size(ctx, a) }.getOrDefault(-1L)
    private fun installAsset(a: String, f: File) {
        if (f.isFile && !isLink(f) && f.length() == assetSize(a)) return
        f.delete()
        EngineAssets.open(ctx, a).use { i -> f.outputStream().use { o -> i.copyTo(o) } }
        f.setReadable(true, false); f.setExecutable(true, false)
    }
    /** the open GLES bridge for the 32-bit engine is asked for (file "glopen" in the image's directory) */
    private fun openGl32(): Boolean = engine == Engine.KK && File(paths.dir, "glopen").isFile &&
        assetSize(OPEN_GL) > 0

    private fun splitSize(): Long = runCatching { EngineAssets.size(ctx, "engines/kk/libGLES_split.so") }.getOrDefault(-1L)

    private fun makeDataDirs() {
        var made = 0
        val want = (DATA_DIRS + img.dirs.map { it.trim().removePrefix("/") }).filter { it.isNotEmpty() && !it.startsWith("#") }
        for (d in want.distinct()) {
            // каталоги init в /dev, /sys, /proc, /acct не создаём — это подменяет qemu
            if (d.startsWith("proc") || d.startsWith("sys/") || d == "sys" || d.startsWith("acct")) continue
            val f = File(root, d)
            if (!f.isDirectory && f.mkdirs()) made++
        }
        if (made > 0) log("created init directories: $made")
    }

    /**
     * MIUI on Android 5: /system/framework/miuisdk.jar and miuisystem.jar are links into /data/miui, and both are
     * on the BOOTCLASSPATH. The phone's own init fills /data/miui from /system/app at boot; without the files
     * dex2oat skips the two jars and aborts while building the boot image ("Check failed: method != NULL
     * android.content.MiuiIntent.<init>"), zygote never starts.
     */
    private fun miuiSdkFiles() {
        for ((jar, apk) in listOf("miuisdk" to "system/app/miui/miui.apk", "miuisystem" to "system/app/miuisystem/miuisystem.apk")) {
            val link = File(root, "system/framework/$jar.jar")
            val target = runCatching { Os.readlink(link.absolutePath) }.getOrNull() ?: continue
            if (!target.endsWith("data/miui/$jar.apk")) continue
            val src = File(root, apk).takeIf { it.isFile } ?: File(root, "system/app/${apk.substringAfterLast('/')}").takeIf { it.isFile } ?: continue
            val dst = File(root, "data/miui/$jar.apk")
            if (dst.isFile && dst.length() == src.length()) continue
            runCatching {
                dst.parentFile?.mkdirs()
                src.copyTo(dst, overwrite = true)
                log("MIUI: /data/miui/$jar.apk restored from /$apk")
            }
        }
    }

    private fun makeUserZeroLink() {
        if (img.api < 14) return // ICS installd creates /data/user/0 too; an absolute link points at the host /data
        // 7.0+: vold prepares the device-encrypted per-user dirs; without it installd cannot create app DE storage
        if (img.api >= 24) for (d in listOf("data", "user_de/0", "misc_ce/0", "misc_de/0", "system_ce/0", "system_de/0", "media/0", "misc/profiles/cur/0", "misc/profiles/ref"))
            File(root, "data/$d").mkdirs()
        val user = File(root, "data/user").apply { mkdirs() }
        val zero = File(user, "0")
        val cur = runCatching { Os.readlink(zero.absolutePath) }.getOrNull()
        if (cur == "../data") return
        runCatching {
            if (cur != null || zero.exists()) { if (zero.isDirectory && !isLink(zero)) wipe(zero) else zero.delete() }
            Os.symlink("../data", zero.absolutePath)
        }
    }

    private fun makeDevNodes() {
        File(root, "dev/input/event0").let { if (!it.isFile) { it.parentFile?.mkdirs(); it.createNewFile() } }
        File(root, "dev/socket").mkdirs()
        File(root, "dev/graphics").mkdirs()
        // a directory in the file's place: open() fails with EISDIR whatever the flags
        if (engine == Engine.N64 && kernel.noQemuPipe) File(root, "dev/qemu_pipe").let { if (!it.isDirectory) { it.delete(); it.mkdirs() } }
        AudioOut(paths, log).makeFifo()
        File(root, "dev/binder").takeIf { it.isDirectory }?.let { wipe(it) }
        File(root, "dev/hwbinder").takeIf { it.isDirectory }?.let { wipe(it) }
        File(root, "dev/vndbinder").takeIf { it.isDirectory }?.let { wipe(it) }
        for (n in listOf("binder", "hwbinder", "vndbinder", "ashmem")) {
            File(root, "dev/$n").let { if (!it.exists()) { it.parentFile?.mkdirs(); it.createNewFile() } }
        }
        File(root, "dev/cpuctl").takeIf { it.exists() }?.let { wipe(it) }
    }

    /**
     * Android 10–17 (API 29+): core libraries and runtimes (Bionic libc/libdl/libm, ART, ICU) are packaged in APEX
     * modules. In non-root container emulation without loopback APEX mounts, the dynamic linker and ART fail immediately
     * unless APEX directories and symlinks are populated.
     */
    /**
     * Android 10+: the runtime (the linker, the C library, ART) and other system modules live in APEX packages, which
     * apexd mounts at /apex/<module name>. A "flattened" package is a plain directory /system/apex/<name>[.debug|...].
     * The directory itself is moved to /apex/<module name> and a link is left where it was: the linker allows a
     * library by its real path, which has to start with /apex/ as under a mount (through a link at /apex it came
     * out as /system/apex/... and nothing could load libc). Packages that are file system images (*.apex) are not
     * unpacked yet.
     */
    private fun fixApex() {
        // (system_ext: where a generic system image keeps the VNDK snapshots of older vendor releases, and flattened
        // copies of the packages it also ships as files)
        val sources = listOf("system/apex", "system/system_ext/apex", "system/product/apex").map { File(root, it) }.filter { it.isDirectory }
        if (sources.isEmpty()) return
        val apexDir = File(root, "apex").apply { mkdirs() }
        // an earlier version of the engine built com.android.runtime and com.android.art here out of links to /system/lib
        // (a real module, moved or unpacked here, has its manifest)
        for (n in listOf("com.android.art", "com.android.runtime")) File(apexDir, n).let {
            if (it.isDirectory && !isLink(it) && !File(it, "apex_manifest.json").exists() && !File(it, "apex_manifest.pb").exists()) wipe(it)
        }
        for (arch in listOf("lib", "lib64")) File(root, "system/$arch/bionic").let { if (isLink(it) && runCatching { Os.readlink(it.path) }.getOrNull() == ".") Os.remove(it.path) }
        apexDir.listFiles()?.filter { isLink(it) && runCatching { Os.readlink(it.path) }.getOrNull()?.startsWith("../system/apex/") == true }?.forEach { Os.remove(it.path) }
        var moved = 0
        var unpacked = 0
        for (dir in sources) {
            val up = "../".repeat(dir.relativeTo(root).path.count { it == '/' } + 1)
            // 11+: packages as files, each a zip with a file system image (see ApexUnpacker)
            if (dir.listFiles()?.any { it.isFile && (it.name.endsWith(".apex") || it.name.endsWith(".capex")) } == true) unpacked += ApexUnpacker.unpackAll(root, dir, log)
            dir.listFiles()?.filter { it.isDirectory && !isLink(it) }?.forEach { pkg ->
                val name = moduleName(pkg)
                val home = File(apexDir, name)
                runCatching {
                    if (home.exists() || isLink(home)) return@forEach      // a second package of the same module: the first one stays
                    if (!pkg.renameTo(home)) error("cannot move")
                    Os.symlink("${up}apex/$name", pkg.path); moved++
                }.onFailure { log("apex $name: ${it.message}") }
            }
        }
        // 12: a generic image's flattened copies share libraries with /system through relative links
        // (lib64/libbase.so -> ../../../../lib64/libbase.so), which a move to /apex leaves pointing nowhere: they
        // are made to point at the same file from the new place (also for a package moved by an earlier version)
        var relinked = 0
        for (dir in sources) dir.listFiles()?.filter { isLink(it) }?.forEach { was ->
            val home = File(apexDir, runCatching { Os.readlink(was.path) }.getOrNull()?.substringAfterLast('/') ?: return@forEach)
            if (!home.isDirectory || isLink(home)) return@forEach
            fun walk(d: File) {
                d.listFiles()?.forEach { f ->
                    if (!isLink(f)) { if (f.isDirectory) walk(f); return@forEach }
                    val t = runCatching { Os.readlink(f.path) }.getOrNull() ?: return@forEach
                    if (t.startsWith("/") || File(d, t).exists()) return@forEach
                    val old = File(File(was.parentFile, home.name + "/" + d.relativeTo(home).path), t).normalize()
                    if (!old.exists() || !old.path.startsWith(root.path + "/")) return@forEach
                    runCatching { Os.remove(f.path); Os.symlink(old.relativeTo(d).path, f.path); relinked++ }
                }
            }
            walk(home)
        }
        // 12+: a module may hold libraries for the other ones (lib64/<library>/<hash>/<library>), which reach them
        // through /apex/sharedlibs - links apexd makes when it mounts such a module
        var shared = 0
        apexDir.listFiles()?.filter { it.isDirectory && !isLink(it) && it.name != "sharedlibs" }?.forEach { mod ->
            for (lib in listOf("lib", "lib64")) File(mod, lib).listFiles()?.filter { it.isDirectory && !isLink(it) && it.name.endsWith(".so") }?.forEach { d ->
                d.listFiles()?.filter { it.isDirectory }?.forEach { hash ->
                    val link = File(apexDir, "sharedlibs/$lib/${d.name}/${hash.name}")
                    if (link.exists() || isLink(link)) return@forEach
                    link.parentFile?.mkdirs()
                    runCatching { Os.symlink("../../../${mod.name}/$lib/${d.name}/${hash.name}", link.path); shared++ }
                }
            }
        }
        if (shared > 0) log("APEX shared libraries linked: $shared")
        if (relinked > 0) log("APEX links to /system set right: $relinked")
        if (moved > 0) log("APEX modules put in place: $moved")
        if (unpacked > 0) log("APEX packages unpacked: $unpacked")
    }

    /** the module a flattened package directory holds: the name in its manifest (json up to 10, protobuf from 11) */
    private fun moduleName(pkg: File): String {
        runCatching { Regex("\"name\"\\s*:\\s*\"([^\"]+)\"").find(File(pkg, "apex_manifest.json").readText())?.groupValues?.get(1) }.getOrNull()?.let { return it }
        runCatching {
            val pb = File(pkg, "apex_manifest.pb").readBytes()
            if (pb.size > 2 && pb[0].toInt() == 0x0A && pb[1] in 1..127 && 2 + pb[1] <= pb.size) return String(pb, 2, pb[1].toInt(), Charsets.UTF_8)
        }
        return pkg.name.removeSuffix(".debug").removeSuffix(".release")
    }

    /**
     * A system image of 9+ carries the product (and later system_ext) partition inside as /system/product, and its
     * root directory links /product to it. The package manager scans /product/priv-app: without the link SystemUI,
     * the launcher and Settings do not exist for it and the boot never leaves the animation.
     */
    private fun rootLinks() {
        for (n in listOf("product", "system_ext")) {
            val link = File(root, n)
            if (engine == Engine.N64 && kernel.partitionsAtRoot) {
                val inSystem = File(root, "system/$n")
                if (isLink(inSystem) || !inSystem.isDirectory) continue
                if (isLink(link) || link.isDirectory && link.list().isNullOrEmpty()) link.delete()
                if (link.exists() || !inSystem.renameTo(link)) continue
                runCatching { Os.symlink("../$n", inSystem.path); log("/$n is a directory of the root, /system/$n links to it") }
                continue
            }
            if (!File(root, "system/$n").isDirectory || isLink(link)) continue
            // (a system-as-root image brings an empty directory there, the partition's mount point)
            if (link.isDirectory && link.list().isNullOrEmpty()) link.delete()
            if (link.exists()) continue
            runCatching { Os.symlink("system/$n", link.path); log("/$n -> /system/$n") }
        }
    }

    /**
     * Notes for the eBPF stand-in of the kernel (Kernel64.bpfMapNotes): one file per map of the firmware eBPF objects,
     * named as the map is pinned (map_<object>_<map>), holding its type, key size and value size. They are the first
     * three words of each definition in the "maps" section of the object; the symbols of that section name the maps.
     * 17: the section is ".android_maps", a definition carries the path it is pinned at (the note takes that name).
     * An object built for libbpf (*.bpf, 16+) has its maps in ".maps", described in BTF: see [btfMapNotes].
     */
    private fun bpfMapNotes() {
        if (engine != Engine.N64 || !kernel.bpfMapNotes) return
        val dir = File(root, "dev/.aemu-bpf")
        if (dir.isDirectory) return
        val tmp = File(root, "dev/.aemu-bpf.tmp").apply { deleteRecursively(); mkdirs() }
        val objects = (listOf(File(root, "system/etc/bpf")) + (File(root, "apex").listFiles()?.map { File(it, "etc/bpf") } ?: emptyList()))
            .flatMap { d -> d.walkTopDown().maxDepth(3).filter { it.isFile && (it.name.endsWith(".o") || it.name.endsWith(".bpf")) }.toList() }
        var n = 0
        for (o in objects) runCatching {
            val b = java.nio.ByteBuffer.wrap(o.readBytes()).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            if (b.getInt(0) != 0x464c457f || b.get(4).toInt() != 2) return@runCatching
            val shoff = b.getLong(0x28).toInt(); val shnum = b.getShort(0x3c).toInt(); val shstr = b.getShort(0x3e).toInt()
            fun sh(i: Int) = shoff + i * 64
            fun str(tab: Int, off: Int): String {
                var p = b.getLong(sh(tab) + 0x18).toInt() + off
                val sb = StringBuilder()
                while (b.get(p).toInt() != 0) sb.append(b.get(p++).toInt().toChar())
                return sb.toString()
            }
            if (o.name.endsWith(".bpf")) {
                val find = { name: String -> (0 until shnum).firstOrNull { str(shstr, b.getInt(sh(it))) == name } }
                val btf = find(".BTF") ?: return@runCatching
                val sec = find(".maps") ?: return@runCatching
                val symtab = (0 until shnum).firstOrNull { b.getInt(sh(it) + 4) == 2 } ?: return@runCatching
                val strtab = b.getInt(sh(symtab) + 0x28)
                val symOff = b.getLong(sh(symtab) + 0x18).toInt(); val symCount = (b.getLong(sh(symtab) + 0x20) / 24).toInt()
                val names = (0 until symCount).map { symOff + it * 24 }.filter { (b.getShort(it + 6).toInt() and 0xffff) == sec }.map { str(strtab, b.getInt(it)) }.toSet()
                val at = b.getLong(sh(btf) + 0x18).toInt(); val size = b.getLong(sh(btf) + 0x20).toInt()
                for ((name, note) in btfMapNotes(java.nio.ByteBuffer.wrap(b.array(), at, size).slice().order(java.nio.ByteOrder.LITTLE_ENDIAN), names)) {
                    val out = java.nio.ByteBuffer.allocate(12).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                    note.forEach { out.putInt(it) }
                    File(tmp, "map_${o.name.removeSuffix(".bpf")}_$name").writeBytes(out.array())
                    n++
                }
                return@runCatching
            }
            val maps = (0 until shnum).firstOrNull { str(shstr, b.getInt(sh(it))) == "maps" }
                ?: (0 until shnum).firstOrNull { str(shstr, b.getInt(sh(it))) == ".android_maps" } ?: return@runCatching
            val pinned = str(shstr, b.getInt(sh(maps))) == ".android_maps"
            val symtab = (0 until shnum).firstOrNull { b.getInt(sh(it) + 4) == 2 } ?: return@runCatching
            val strtab = b.getInt(sh(symtab) + 0x28)
            val symOff = b.getLong(sh(symtab) + 0x18).toInt(); val symCount = (b.getLong(sh(symtab) + 0x20) / 24).toInt()
            val mapsOff = b.getLong(sh(maps) + 0x18).toInt(); val mapsSize = b.getLong(sh(maps) + 0x20)
            val obj = o.name.removeSuffix(".o").substringBefore('@')
            for (i in 0 until symCount) {
                val s = symOff + i * 24
                if ((b.getShort(s + 6).toInt() and 0xffff) != maps) continue
                val name = str(strtab, b.getInt(s))
                val at = b.getLong(s + 8)
                if (name.isEmpty() || at + 12 > mapsSize) continue
                val note = java.nio.ByteBuffer.allocate(12).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                for (k in 0 until 3) note.putInt(b.getInt(mapsOff + at.toInt() + k * 4))
                var file = "map_${obj}_$name"
                if (pinned) {       // struct bpf_map_def: 12 words, create_location[n], pin_location[n], name_idx
                    val size = b.getLong(s + 16)
                    val n = ((size - 52) / 2).toInt()
                    if (n <= 0 || at + size > mapsSize) continue
                    val path = StringBuilder()
                    var q = mapsOff + at.toInt() + 48 + n
                    val end = q + n
                    while (q < end && b.get(q).toInt() != 0) path.append(b.get(q++).toInt().toChar())
                    if (path.isNotEmpty()) file = path.substring(path.lastIndexOf('/') + 1)
                }
                File(tmp, file).writeBytes(note.array())
                n++
            }
        }
        if (tmp.renameTo(dir)) log("eBPF: $n map notes from ${objects.size} objects")
    }

    /** The kernel policy version the framework compatibility matrices ask for becomes 0 (Kernel64.kernelPolicyUnread). */
    private fun kernelPolicyUnread() {
        if (engine != Engine.N64 || !kernel.kernelPolicyUnread) return
        val asked = Regex("<kernel-sepolicy-version>\\s*[1-9][0-9]*\\s*</kernel-sepolicy-version>")
        for (part in listOf("system", "system_ext", "product")) {
            for (f in File(root, "$part/etc/vintf").listFiles() ?: continue) {
                if (!f.isFile || !f.name.startsWith("compatibility_matrix") || !f.name.endsWith(".xml")) continue
                val text = runCatching { f.readText() }.getOrNull() ?: continue
                if (!asked.containsMatchIn(text)) continue
                runCatching { f.setWritable(true); f.writeText(asked.replace(text, "<kernel-sepolicy-version>0</kernel-sepolicy-version>")) }
                    .onSuccess { log("build check: ${f.name} no longer asks for a kernel policy version") }
            }
        }
    }

    /**
     * The aconfig flag storage (Kernel64.aconfigStorage), as aconfigd leaves it: /metadata/aconfig/maps/<container>.
     * package.map and .flag.map, /metadata/aconfig/boot/<container>.val and .info, copied from <partition>/etc/aconfig
     * and from each module's etc. A container is a partition or a module, by name.
     */
    private fun aconfigStorage() {
        if (engine != Engine.N64 || !kernel.aconfigStorage) return
        val maps = File(root, "metadata/aconfig/maps").apply { mkdirs() }
        val boot = File(root, "metadata/aconfig/boot").apply { mkdirs() }
        val sources = listOf("system", "system_ext", "product", "vendor").map { it to File(root, "$it/etc/aconfig") } +
            (File(root, "apex").listFiles()?.filter { it.isDirectory && it.name != "sharedlibs" }?.map { it.name to File(it, "etc") } ?: emptyList())
        var n = 0
        for ((container, dir) in sources) {
            val pkg = File(dir, "package.map"); val flag = File(dir, "flag.map"); val value = File(dir, "flag.val")
            if (!pkg.isFile || !flag.isFile || !value.isFile) continue
            val out = File(boot, "$container.val")
            if (out.isFile && out.length() == value.length() && out.lastModified() >= value.lastModified()) continue
            runCatching {
                pkg.copyTo(File(maps, "$container.package.map"), true)
                flag.copyTo(File(maps, "$container.flag.map"), true)
                File(dir, "flag.info").takeIf { it.isFile }?.copyTo(File(boot, "$container.info"), true)
                value.copyTo(out, true)
                for (f in listOf(File(maps, "$container.package.map"), File(maps, "$container.flag.map"), File(boot, "$container.info"), out)) f.setReadable(true, false)
                n++
            }.onFailure { log("aconfig $container: ${it.message}") }
        }
        if (n > 0) log("aconfig: the flag storage of $n containers")
    }

    /**
     * The maps of a libbpf object, from its BTF: a map is a variable of the given [names] whose type is a struct with
     * the members libbpf's macros make - "type", "key_size", "value_size" as pointers to an array of that many
     * elements, "key" and "value" as pointers to the type itself. Returns type, key size, value size per map.
     */
    private fun btfMapNotes(b: java.nio.ByteBuffer, names: Set<String>): Map<String, IntArray> {
        if (b.limit() < 24 || (b.getShort(0).toInt() and 0xffff) != 0xeb9f) return emptyMap()
        val hdr = b.getInt(4); val typeOff = hdr + b.getInt(8); val typeLen = b.getInt(12); val strOff = hdr + b.getInt(16)
        fun str(off: Int): String { val sb = StringBuilder(); var p = strOff + off; while (p < b.limit() && b.get(p).toInt() != 0) sb.append(b.get(p++).toInt().toChar()); return sb.toString() }
        val types = ArrayList<Int>().apply { add(-1) }      // id -> offset; id 0 is void
        var p = typeOff
        while (p + 12 <= typeOff + typeLen) {
            types.add(p)
            val info = b.getInt(p + 4); val vlen = info and 0xffff
            p += 12 + when ((info ushr 24) and 0x1f) { 1, 14, 17 -> 4; 3 -> 12; 4, 5, 15, 19 -> vlen * 12; 6, 13 -> vlen * 8; else -> 0 }
        }
        fun kind(id: Int) = if (id <= 0 || id >= types.size) 0 else (b.getInt(types[id] + 4) ushr 24) and 0x1f
        fun ref(id: Int) = b.getInt(types[id] + 8)
        fun sizeOf(id: Int, depth: Int = 0): Int = when (if (depth > 16) 0 else kind(id)) {
            1, 4, 5, 6, 16, 19 -> ref(id)
            2 -> 8
            3 -> sizeOf(b.getInt(types[id] + 12), depth + 1) * b.getInt(types[id] + 20)
            8, 9, 10, 11, 14, 18 -> sizeOf(ref(id), depth + 1)
            else -> 0
        }
        fun count(id: Int): Int = if (kind(id) == 2 && kind(ref(id)) == 3) b.getInt(types[ref(id)] + 20) else 0
        val out = HashMap<String, IntArray>()
        for (id in 1 until types.size) {
            if (kind(id) != 14) continue
            val name = str(b.getInt(types[id]))
            var t = ref(id)
            while (kind(t) in listOf(8, 9, 10, 11)) t = ref(t)
            if (name !in names || kind(t) != 4) continue
            val note = IntArray(3)
            for (m in 0 until (b.getInt(types[t] + 4) and 0xffff)) {
                val e = types[t] + 12 + m * 12
                val type = b.getInt(e + 4)
                when (str(b.getInt(e))) {
                    "type" -> note[0] = count(type)
                    "key_size" -> note[1] = count(type)
                    "value_size" -> note[2] = count(type)
                    "key" -> if (kind(type) == 2) note[1] = sizeOf(ref(type))
                    "value" -> if (kind(type) == 2) note[2] = sizeOf(ref(type))
                }
            }
            if (note[0] != 0) out[name] = note
        }
        return out
    }

    /** Version (field 2) of an apex_manifest.pb, or 0. */
    private fun apexVersion(f: File): Long {
        val d = runCatching { f.readBytes() }.getOrNull() ?: return 0
        var i = 0
        while (i < d.size) {
            val tag = d[i++].toInt() and 0xff
            var v = 0L; var s = 0
            fun varint(): Long { v = 0; s = 0; while (i < d.size) { val b = d[i++].toInt() and 0xff; v = v or ((b and 0x7f).toLong() shl s); s += 7; if (b < 0x80) break }; return v }
            when (tag and 7) {
                0 -> { val x = varint(); if (tag ushr 3 == 2) return x }
                2 -> { val n = varint().toInt(); i += n }
                else -> return 0
            }
        }
        return 0
    }

    /** True when the image's system library [lib] contains [marker] (a feature test on the firmware itself); no library: true. */
    private fun libKnows(lib: String, marker: String): Boolean {
        val f = listOf("system/lib64/$lib", "system/lib/$lib").map { File(root, it) }.firstOrNull { it.isFile } ?: return true
        val m = marker.toByteArray()
        val d = runCatching { f.readBytes() }.getOrNull() ?: return true
        outer@ for (i in 0..d.size - m.size) {
            for (j in m.indices) if (d[i + j] != m[j]) continue@outer
            return true
        }
        return false
    }

    private fun makeSysNodes() {
        val freq = File(root, "sys/devices/system/cpu/cpu0/cpufreq/stats/time_in_state")
        if (!freq.isFile) {
            freq.parentFile?.mkdirs()
            freq.writeText(listOf(384000, 594000, 810000, 1026000, 1242000, 1512000).joinToString("\n") { "$it 0" } + "\n")
        }
        val cpus = Runtime.getRuntime().availableProcessors().coerceIn(1, 8)
        val cpu = File(root, "sys/devices/system/cpu").apply { mkdirs() }
        for ((n, v) in listOf("present" to "0-${cpus - 1}", "possible" to "0-${cpus - 1}", "online" to "0-${cpus - 1}", "offline" to "")) {
            File(cpu, n).let { if (!it.isFile) it.writeText(v + "\n") }
        }
        // one directory per core: apps size their thread pools by counting cpuN here (MIUI File Manager of 13 saw one
        // core, and its only pool thread waited for the tasks it had queued behind itself)
        for (n in 0 until cpus) File(cpu, "cpu$n/online").let { if (!it.isFile) { it.parentFile?.mkdirs(); it.writeText("1\n") } }
        File(root, "sys/class/net/lo").mkdirs()
        if (engine == Engine.N64 && kernel.cgroup2Root) {
            File(root, "sys/fs/cgroup").mkdirs()
            for (d in kernel.cgroupUidParents) File(root, "sys/fs/cgroup/$d").mkdirs()
            // libprocessgroup's file: version 1, one controller {version 2, mounted, name[16], path[32]}
            // (15: the record gained max_activation_depth after the flags - 0, the engine has no controllers to switch on
            // in child groups - and netd leaves with "cgroup.rc file has invalid size, expected 68")
            // (early 15 builds, e.g. HyperOS 2, still have the 64-byte record: the reader of the file, libcgrouprc, knows the
            // MaxActivationDepth field only from the later ones on, so the record is sized by that, not by the API level)
            val depth = kernel.cgroupRcDepth && libKnows("libcgrouprc.so", "MaxActivationDepth")
            val b = java.nio.ByteBuffer.allocate(8 + (if (depth) 60 else 56)).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            b.putInt(1).putInt(1).putInt(2).putInt(1)
            if (depth) b.putInt(0)
            b.put("cgroup2".toByteArray().copyOf(16)).put("/sys/fs/cgroup".toByteArray().copyOf(32))
            File(root, "dev/cgroup_info").let { it.mkdirs(); File(it, "cgroup.rc").writeBytes(b.array()) }
        }
        // owner the connectivity module expects on clatd's directory (the runner answers its stat with it):
        // clat from the 2025 module on, root before
        if (engine == Engine.N64) {
            val v = apexVersion(File(root, "apex/com.android.tethering/apex_manifest.pb"))
            File(root, "dev").mkdirs(); File(root, "dev/.aemu-clat-uid").writeText(if (v >= 360000000L) "1029" else "0")
        }
        if (engine == Engine.N64) for ((n, text) in kernel.sysFiles) File(root, "sys/$n").let {
            if (!it.exists()) { it.parentFile?.mkdirs(); it.writeText(text) }
        }
        val base = File(root, "sys/class/power_supply")
        val bat = mapOf(
            "battery/type" to "Battery", "battery/status" to "Full", "battery/health" to "Good",
            "battery/present" to "1", "battery/capacity" to "100", "battery/batt_vol" to "4200",
            "battery/voltage_now" to "4200000", "battery/batt_temp" to "250", "battery/temp" to "250",
            "battery/technology" to "Li-ion", "ac/type" to "Mains", "ac/online" to "1",
            "usb/type" to "USB", "usb/online" to "0",
        )
        for ((rel, v) in bat) File(base, rel).let { if (!it.isFile) { it.parentFile?.mkdirs(); it.writeText(v + "\n") } }
        healthdRelativeSysfs(base)
        personalityNoop()
        parkNfc()
        mainStackMaps()
        val power = File(root, "sys/power").apply { mkdirs() }
        for (n in listOf("state", "wake_lock", "wake_unlock", "autosleep")) File(power, n).let { if (!it.isFile) it.createNewFile(); it.setWritable(true, false) }
        // узлы питания, которые открывает libhardware_legacy именно этой прошивки (Samsung: dvfslock_ctrl…):
        // если хоть один не откроется, библиотека считает экран выключенным и система не принимает касания
        runCatching {
            val lib = File(root, "system/lib/libhardware_legacy.so")
            if (lib.isFile) {
                val text = String(lib.readBytes(), Charsets.ISO_8859_1)
                for (m in Regex("/sys/(power|android_power)/[a-z_0-9]+").findAll(text)) {
                    val rel = m.value.removePrefix("/")
                    if (rel.contains("wait_for_fb")) continue // handled below
                    File(root, rel).let { f -> if (!f.exists()) { f.parentFile?.mkdirs(); f.createNewFile(); f.setWritable(true, false) } }
                }
            }
        }
        // SurfaceFlinger's DisplayEventThread loops sleep→wake: missing nodes make it spin at 100% CPU (EBADF).
        // wake answers at once; sleep is a FIFO with no writer, so the read blocks forever and the screen stays on.
        runCatching {
            File(power, "wait_for_fb_wake").let { if (!it.isFile) it.writeText("awake") }
            val sleep = File(power, "wait_for_fb_sleep")
            if (!sleep.exists()) android.system.Os.mkfifo(sleep.absolutePath, "666".toInt(8))
        }
        // libsuspend with early suspend (4.2/4.3, Samsung's 4.4) waits in autosuspend_enable() for that sleep event
        // under PowerManagerService's lock: the first screen timeout hung system_server. A state node it cannot open
        // for writing makes it fall back to autosleep, which does not wait
        runCatching {
            val lib = File(root, "system/lib/libsuspend.so")
            if (lib.isFile && contains(lib, "wait_for_fb_sleep")) File(power, "state").setWritable(false, false)
        }
        // подсветка экрана: LightsService пишет сюда яркость
        val bl = File(root, "sys/class/leds/lcd-backlight").apply { mkdirs() }
        File(bl, "brightness").let { if (!it.isFile) it.writeText("255\n") }
        File(bl, "max_brightness").let { if (!it.isFile) it.writeText("255\n") }
        // Samsung's flip cover sensor (Galaxy S4/S5...): unreadable means "cover closed" to TouchWiz, and it hides
        // the status bar, the lock screen and recents behind the S View cover screen
        if (img.brand.equals("Samsung", true) || img.skin.contains("TouchWiz", true)) {
            val key = File(root, "sys/class/sec/sec_key").apply { mkdirs() }
            File(key, "hall_detect").let { if (!it.isFile) it.writeText("OPEN\n") }
        }
    }

    /**
     * Своя таблица монтирования: прошивки (Samsung /efs, /preload…) читают /proc/mounts и, не найдя
     * раздел, пытаются смонтировать его сами — а seccomp телефона убивает за mount(). qemu отдаёт
     * гостю файлы из дерева поверх хостовых, поэтому root/proc/mounts видится как настоящий.
     */
    private fun makeProcMounts() {
        val mounts = LinkedHashMap<String, String>() // точка → строка
        fun add(dev: String, point: String, type: String, opts: String) { mounts.putIfAbsent(point, "$dev $point $type $opts 0 0") }
        add("rootfs", "/", "rootfs", "ro,relatime")
        add("tmpfs", "/dev", "tmpfs", "rw,nosuid,relatime,mode=755")
        add("devpts", "/dev/pts", "devpts", "rw,relatime,mode=600")
        add("proc", "/proc", "proc", "rw,relatime")
        add("sysfs", "/sys", "sysfs", "rw,relatime")
        add("none", "/acct", "cgroup", "rw,relatime,cpuacct")
        add("tmpfs", "/mnt/asec", "tmpfs", "rw,relatime,mode=755,gid=1000")
        add("tmpfs", "/mnt/obb", "tmpfs", "rw,relatime,mode=755,gid=1000")
        add("/dev/block/platform/aemu/by-name/system", "/system", "ext4", "ro,relatime")
        add("/dev/block/platform/aemu/by-name/userdata", "/data", "ext4", "rw,nosuid,nodev,noatime")
        add("/dev/block/platform/aemu/by-name/cache", "/cache", "ext4", "rw,nosuid,nodev,noatime")
        // точки монтирования из init*.rc и fstab прошивки
        val rcs = (root.listFiles()?.filter { it.isFile && (it.name.endsWith(".rc") || it.name.startsWith("fstab")) } ?: emptyList()) +
            listOfNotNull(File(root, "system/etc/vold.fstab").takeIf { it.isFile })
        for (f in rcs) runCatching {
            for (raw in f.readLines()) {
                val t = raw.trim().split(Regex("\\s+"))
                if (t.isEmpty() || t[0].startsWith("#")) continue
                when {
                    t[0] == "mount" && t.size >= 4 -> {
                        val point = t[3]
                        if (point.startsWith("/") && point != "/" && !point.startsWith("/proc") && !point.startsWith("/sys") && !point.startsWith("/dev"))
                            add(t[2].substringBefore('@').ifEmpty { "none" }, point, t[1], if (point == "/system") "ro,relatime" else "rw,relatime")
                    }
                    f.name.startsWith("fstab") && t.size >= 3 && t[0].startsWith("/dev") && t[1].startsWith("/") ->
                        add(t[0], t[1], t[2], "rw,relatime")
                }
            }
        }
        // карта памяти видна «смонтированной»
        add("/dev/block/vold/179:1", img.sdcardPath, "vfat", "rw,dirsync,nosuid,nodev,noexec,relatime,uid=1000,gid=1015,fmask=0702,dmask=0702")
        for (p in mounts.keys) if (p != "/" && !p.startsWith("/proc") && !p.startsWith("/sys") && !p.startsWith("/dev")) File(root, p.trimStart('/')).mkdirs()
        val f = File(root, "proc/mounts")
        f.parentFile?.mkdirs()
        val text = mounts.values.joinToString("\n", postfix = "\n")
        if (f.takeIf { it.isFile }?.readText() != text) f.writeText(text)
    }

    /** DNS fallback for native binaries, web engines and tools: /etc/resolv.conf and /system/etc/resolv.conf */
    /**
     * Old firmwares lack today's root certificates (ISRG, GlobalSign R46…): HTTPS sites failed with
     * CERT_AUTHORITY_INVALID (-202). The phone's own roots are added; the names are the same subject_hash_old.N.
     */
    private fun caCerts() = runCatching {
        val dst = File(root, "system/etc/security/cacerts")
        if (!dst.isDirectory) return@runCatching
        val src = listOf("/apex/com.android.conscrypt/cacerts", "/system/etc/security/cacerts").map(::File).firstOrNull { it.isDirectory } ?: return@runCatching
        val have = dst.listFiles().orEmpty().mapNotNull { runCatching { it.readText() }.getOrNull() }.toHashSet()
        var added = 0
        for (f in src.listFiles().orEmpty()) {
            val text = runCatching { f.readText() }.getOrNull() ?: continue
            if (text in have) continue
            val hash = f.name.substringBefore('.')
            val out = (0..9).map { File(dst, "$hash.$it") }.firstOrNull { !it.exists() } ?: continue
            out.writeText(text); out.setReadable(true, false); have += text; added++
        }
        if (added > 0) log("certificates: $added roots added from the phone")
    }

    private fun resolvConf() {
        val conf = "nameserver 8.8.8.8\nnameserver 1.1.1.1\nnameserver 9.9.9.9\n"
        runCatching {
            val f1 = File(root, "system/etc/resolv.conf")
            f1.parentFile?.mkdirs()
            if (!f1.exists() || f1.length() == 0L) f1.writeText(conf)
            val f2 = File(root, "etc/resolv.conf")
            f2.parentFile?.mkdirs()
            if (!f2.exists() || f2.length() == 0L) f2.writeText(conf)
        }
    }

    /**
     * Звук обслуживает наш HAL (один выход на динамик, 48 кГц), поэтому audio_policy.conf пишем
     * канонический: лишние выходы прошивок (hdmi с dynamic-параметрами, deep_buffer, a2dp, usb)
     * заставляют audio_policy спрашивать у HAL то, чего он не знает, и mediaserver падает.
     */
    private fun audioPolicy() {
        val cur = File(root, "system/etc/audio_policy.conf")
        val rom = File(root, "system/etc/audio_policy.conf.rom")
        if (!cur.isFile && !rom.isFile && img.api < 16) return
        runCatching {
            if (cur.isFile && !rom.isFile) cur.copyTo(rom)
            val text = """
                |# создано AEmulator: единственный модуль — звуковой HAL эмулятора
                |global_configuration {
                |  attached_output_devices AUDIO_DEVICE_OUT_SPEAKER
                |  default_output_device AUDIO_DEVICE_OUT_SPEAKER
                |  attached_input_devices AUDIO_DEVICE_IN_BUILTIN_MIC
                |}
                |
                |audio_hw_modules {
                |  primary {
                |    outputs {
                |      primary {
                |        sampling_rates 48000
                |        channel_masks AUDIO_CHANNEL_OUT_STEREO
                |        formats AUDIO_FORMAT_PCM_16_BIT
                |        devices AUDIO_DEVICE_OUT_SPEAKER
                |        flags AUDIO_OUTPUT_FLAG_PRIMARY
                |      }
                |    }
                |    inputs {
                |      primary {
                |        sampling_rates 8000|16000|44100|48000
                |        channel_masks AUDIO_CHANNEL_IN_MONO|AUDIO_CHANNEL_IN_STEREO
                |        formats AUDIO_FORMAT_PCM_16_BIT
                |        devices AUDIO_DEVICE_IN_BUILTIN_MIC
                |      }
                |    }
                |  }
                |}
                |""".trimMargin()
            if (cur.takeIf { it.isFile }?.readText() != text) cur.writeText(text)
        }.onFailure { log("audio: failed to write audio_policy.conf: ${it.message}") }
        // 8.0+ reads audio_policy_configuration.xml (odm, vendor, then system) and never the .conf: a phone's one
        // (deep buffer, offload, voice ports) makes the policy ask the engine's HAL for streams it cannot open, and
        // audioserver dies on the first sound. The same single output goes to vendor/etc, read before system/etc.
        val xml = File(root, "vendor/etc/audio_policy_configuration.xml")
        if (img.engine == Engine.N64 && kernel.audioPolicyXml && File(root, "system/etc/audio_policy_configuration.xml").isFile && File(root, "vendor/etc").isDirectory) runCatching {
            val own = File(root, "system/.aemu-parked/vendor#etc#audio_policy_configuration.xml")
            if (xml.isFile && !own.exists() && !xml.readText().contains("AEmulator")) { own.parentFile?.mkdirs(); xml.renameTo(own) }
            val text = """
                |<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                |<!-- AEmulator: the one module is the engine's audio HAL -->
                |<audioPolicyConfiguration version="1.0" xmlns:xi="http://www.w3.org/2001/XInclude">
                |    <globalConfiguration speaker_drc_enabled="false"/>
                |    <modules>
                |        <module name="primary" halVersion="2.0">
                |            <attachedDevices>
                |                <item>Speaker</item>
                |                <item>Built-In Mic</item>
                |            </attachedDevices>
                |            <defaultOutputDevice>Speaker</defaultOutputDevice>
                |            <mixPorts>
                |                <mixPort name="primary output" role="source" flags="AUDIO_OUTPUT_FLAG_PRIMARY">
                |                    <profile name="" format="AUDIO_FORMAT_PCM_16_BIT" samplingRates="48000" channelMasks="AUDIO_CHANNEL_OUT_STEREO"/>
                |                </mixPort>
                |                <mixPort name="primary input" role="sink">
                |                    <profile name="" format="AUDIO_FORMAT_PCM_16_BIT" samplingRates="8000,16000,44100,48000" channelMasks="AUDIO_CHANNEL_IN_MONO,AUDIO_CHANNEL_IN_STEREO"/>
                |                </mixPort>
                |            </mixPorts>
                |            <devicePorts>
                |                <devicePort tagName="Speaker" type="AUDIO_DEVICE_OUT_SPEAKER" role="sink"/>
                |                <devicePort tagName="Built-In Mic" type="AUDIO_DEVICE_IN_BUILTIN_MIC" role="source"/>
                |            </devicePorts>
                |            <routes>
                |                <route type="mix" sink="Speaker" sources="primary output"/>
                |                <route type="mix" sink="primary input" sources="Built-In Mic"/>
                |            </routes>
                |        </module>
                |    </modules>
                |    <xi:include href="/system/etc/audio_policy_volumes_drc.xml"/>
                |    <xi:include href="/system/etc/default_volume_tables.xml"/>
                |</audioPolicyConfiguration>
                |""".trimMargin()
            if (xml.takeIf { it.isFile }?.readText() != text) { xml.writeText(text); xml.setReadable(true, false) }
        }.onFailure { log("audio: failed to write audio_policy_configuration.xml: ${it.message}") }
        // AudioFlinger reads vendor/etc/audio_effects.conf before the system one. A vendor's list adds effects for its own
        // amplifier and DSP (Motorola's speaker protection crashed 7.1's audioserver on a null calibration, so AudioFlinger
        // was never published); the engine's HAL is a plain speaker, so the system's AOSP effects are used instead.
        val fx = File(root, "vendor/etc/audio_effects.conf")
        if (img.engine != Engine.N64 && fx.isFile && File(root, "system/etc/audio_effects.conf").isFile) runCatching {
            val own = File(root, "system/.aemu-parked/vendor#etc#audio_effects.conf")
            own.parentFile?.mkdirs()
            if (fx.renameTo(own)) log("audio: vendor audio_effects.conf parked, AOSP effects used")
        }
    }

    /** Кадровый буфер — обычный файл, qemu отвечает на ioctl FBIOGET_* от гостя. */
    private fun makeFb() {
        val s = img.settings
        val (w, h) = if (engine == Engine.GB) 480 to 800 else s.width to s.height
        // двойная буферизация: гость листает страницы через FBIOPAN_DISPLAY
        // qemu reports smem_len = two 32-bit pages (TWRP draws BGRA_8888); mmap past the file end would SIGBUS
        val need = w.toLong() * h * 4 * 2
        val f = paths.fb
        if (f.isFile && f.length() == need) return
        f.parentFile?.mkdirs()
        RandomAccessFile(f, "rw").use { it.setLength(need) }
        runCatching { File(root, "dhd.fbgeom").writeText("$w $h\n") }
        log("framebuffer: ${w}x$h")
    }

    /**
     * Владельцы файлов приложений в /data/data: ядро телефона не даёт менять uid, поэтому qemu
     * подставляет их по таблице dhd.owners (dev, ino → uid, gid). Берём uid из packages.xml.
     */
    fun seedOwners() {
        val stamp = File(root, "dhd.owners.seeded")
        val done = if (stamp.isFile) stamp.readLines().filter { it.isNotBlank() }.toMutableSet() else mutableSetOf()
        val pkgXml = File(root, "data/system/packages.xml")
        val dataDir = File(root, "data/data")
        if (!pkgXml.isFile || !dataDir.isDirectory) return
        runCatching {
            val text = pkgXml.readText()
            val sharedUsers = HashMap<String, Int>()
            val suRe = Regex("<shared-user name=\"([^\"]+)\"[^>]*?\\suserId=\"(\\d+)\"")
            for (m in suRe.findAll(text)) {
                sharedUsers[m.groupValues[1]] = m.groupValues[2].toInt()
            }
            val sb = StringBuilder()
            var rows = 0
            var pkgs = 0
            val pkgRe = Regex("<package name=\"([^\"]+)\"[^>]*?>")
            for (m in pkgRe.findAll(text)) {
                val tag = m.value
                val name = m.groupValues[1]
                val uid = Regex("\\suserId=\"(\\d+)\"").find(tag)?.groupValues?.get(1)?.toIntOrNull()
                    ?: Regex("\\ssharedUserId=\"([^\"]+)\"").find(tag)?.groupValues?.get(1)?.let { sharedUsers[it] }
                    ?: continue
                val d = File(dataDir, name)
                if (!d.isDirectory) continue
                val key = "$name $uid"
                if (key in done) continue
                done.add(key)
                pkgs++
                val todo = ArrayDeque<File>().apply { add(d) }
                while (todo.isNotEmpty()) {
                    val f = todo.removeFirst()
                    runCatching {
                        f.setReadable(true, false)
                        f.setWritable(true, false)
                        if (f.isDirectory) f.setExecutable(true, false)
                    }
                    val st = runCatching { Os.lstat(f.absolutePath) }.getOrNull() ?: continue
                    sb.append(String.format("%016x %016x %08x %08x\n", st.st_dev, st.st_ino, uid, uid))
                    rows++
                    if (!OsConstants.S_ISLNK(st.st_mode) && f.isDirectory) f.listFiles()?.forEach { todo.add(it) }
                }
            }
            if (rows > 0) {
                paths.owners.appendText(sb.toString())
                log("file owners restored: $pkgs packages, $rows entries")
            }
            stamp.writeText(done.joinToString("\n", postfix = "\n"))
        }.onFailure { log("owners: ${it.message}") }
    }

    /** Пропустить миграцию PRE_BOOT_COMPLETED (4.x): под эмуляцией она занимает минуты. */
    fun skipPreBoot(props: PropService) {
        if (img.api < 14) return
        val f = File(root, "data/system/called_pre_boots.dat")
        val rel = props.get("ro.build.version.release") ?: img.release
        val code = props.get("ro.build.version.codename") ?: "REL"
        val incr = props.get("ro.build.version.incremental") ?: ""
        val fresh = runCatching {
            DataInputStream(f.inputStream().buffered()).use { i ->
                i.readInt() == 10000 && i.readUTF() == rel && i.readUTF() == code && i.readUTF() == incr
            }
        }.getOrDefault(false)
        if (fresh) return
        runCatching {
            f.parentFile?.mkdirs()
            DataOutputStream(f.outputStream().buffered()).use { o ->
                o.writeInt(10000); o.writeUTF(rel); o.writeUTF(code); o.writeUTF(incr); o.writeInt(0)
            }
        }
    }

    /** Экран не гаснет, данные «включены»: прямо в базе настроек (есть после первой загрузки). */
    fun noScreenSleep() {
        val db = File(root, "data/data/com.android.providers.settings/databases/settings.db")
        if (!db.isFile && img.api >= 23 && kernel.xmlSettings) { xmlSettings(); disableSetupWizards(); return }
        if (!db.isFile) return
        runCatching {
            SQLiteDatabase.openDatabase(db.absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use { d ->
                fun put(table: String, name: String, value: String) {
                    val exists = runCatching {
                        d.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(table)).use { it.count > 0 }
                    }.getOrDefault(false)
                    if (!exists) return
                    val cv = android.content.ContentValues().apply { put("name", name); put("value", value) }
                    if (d.update(table, cv, "name=?", arrayOf(name)) == 0) d.insert(table, null, cv)
                }
                put("system", "screen_off_timeout", "2147483647")
                val t = if (img.api >= 17) "global" else "system"
                put(t, "stay_on_while_plugged_in", "7")
                put(if (img.api >= 17) "global" else "secure", "mobile_data", "1")
                put(if (img.api >= 17) "global" else "secure", "package_verifier_enable", "0")
                put(if (img.api >= 17) "global" else "secure", "install_non_market_apps", "1")
                put("secure", "install_non_market_apps", "1")
                if (img.api >= 17) put("global", "verifier_verify_adb_installs", "0")
                // мастер первого запуска считаем пройденным: иначе он остаётся «домашним экраном»,
                // и кнопка «Домой» открывает его (у Samsung — чёрный экран на минуты)
                put(if (img.api >= 17) "global" else "secure", "device_provisioned", "1")
                put("secure", "user_setup_complete", "1")
            }
        }.onFailure { log("settings: database failed to open: ${it.message}") }
        disableSetupWizards()
    }

    /**
     * 6.0 keeps the settings as XML files, one per table (data/system/users/0/settings_<table>.xml). The same values
     * as for the database: the screen stays on, and the device counts as set up, without which the setup wizard
     * stays the home screen.
     */
    private fun xmlSettings() {
        val values = listOf(
            Triple("system", "screen_off_timeout", "2147483647"),
            Triple("global", "stay_on_while_plugged_in", "7"),
            Triple("global", "package_verifier_enable", "0"),
            Triple("global", "device_provisioned", "1"),
            Triple("secure", "user_setup_complete", "1"),
            Triple("secure", "install_non_market_apps", "1"),
        )
        for ((table, list) in values.groupBy { it.first }) {
            val f = File(root, "data/system/users/0/settings_$table.xml")
            var xml = runCatching { f.readText() }.getOrNull() ?: continue
            if (!xml.contains("</settings>")) continue
            val before = xml
            for ((_, name, value) in list) {
                val m = Regex("(<setting id=\"[0-9]+\" name=\"${Regex.escape(name)}\" value=\")[^\"]*\"").find(xml)
                if (m != null) { xml = xml.replaceRange(m.range, m.groupValues[1] + value + "\""); continue }
                val id = (Regex("<setting id=\"([0-9]+)\"").findAll(xml).maxOfOrNull { it.groupValues[1].toInt() } ?: 0) + 1
                xml = xml.replace("</settings>", "  <setting id=\"$id\" name=\"$name\" value=\"$value\" package=\"android\" />\n</settings>")
            }
            if (xml != before) runCatching { f.writeText(xml) }.onFailure { log("settings_$table.xml: ${it.message}") }
        }
    }

    /** Отключает пакеты мастеров первого запуска в package-restrictions.xml (enabled="2" = выключен). */
    private fun disableSetupWizards() {
        val f = File(root, "data/system/users/0/package-restrictions.xml")
        if (!f.isFile) return
        // packages.xml, а не packages.list: в списке нет пакетов с общим системным uid (мастер Samsung)
        val pkgs = runCatching { File(root, "data/system/packages.xml").readText() }.getOrNull() ?: return
        val targets = SETUP_WIZARDS.filter { pkgs.contains("<package name=\"$it\"") }
        if (targets.isEmpty()) return
        var xml = runCatching { f.readText() }.getOrNull() ?: return
        var changed = false
        for (p in targets) {
            val open = Regex("<pkg name=\"${Regex.escape(p)}\"([^>]*?)(/?)>")
            val m = open.find(xml)
            if (m == null) {
                xml = xml.replace("</package-restrictions>", "    <pkg name=\"$p\" enabled=\"2\" />\n</package-restrictions>")
                changed = true
            } else if (!m.groupValues[1].contains("enabled=\"2\"")) {
                val attrs = m.groupValues[1].replace(Regex(" enabled=\"\\d\""), "")
                xml = xml.replaceRange(m.range, "<pkg name=\"$p\"$attrs enabled=\"2\"${m.groupValues[2]}>")
                changed = true
            }
        }
        if (changed) runCatching { f.writeText(xml); log("setup wizard disabled: ${targets.joinToString()}") }
    }

    /**
     * Static healthd (5.0+) checks the battery files with faccessat, which the stand's qemu does not map into the
     * guest root: it tests the phone's real /sys and finds no battery. Its path constant becomes the relative
     * "sys/class/power_supply" (resolved from the services' working directory, run/, where sys links to root/sys).
     */
    private fun healthdRelativeSysfs(base: File) {
        if (img.api < 21) return
        runCatching {
            val link = File(paths.bin, "sys")
            paths.bin.mkdirs()
            if (!isLink(link)) { wipe(link); Os.symlink(File(root, "sys").absolutePath, link.path) }
            val hd = File(root, "sbin/healthd").takeIf { it.isFile } ?: return
            val d = hd.readBytes()
            val from = "/sys/class/power_supply".toByteArray()
            val to = "sys/class/power_supply".toByteArray() + 0
            var n = 0
            var i = 0
            while (i <= d.size - from.size) {
                if (d[i] == from[0] && (0 until from.size).all { d[i + it] == from[it] }) { System.arraycopy(to, 0, d, i, to.size); n++; i += from.size } else i++
            }
            if (n > 0) { hd.writeBytes(d); log("healthd: battery sysfs path made relative ($n)") }
        }.onFailure { log("healthd patch failed: ${it.message}") }
        if (!base.isDirectory) log("no fake power_supply")
    }

    /**
     * bionic 6.0+ starts every 32-bit process with personality(PER_LINUX32) and aborts ("error setting PER_LINUX32
     * personality") when it fails — and it does on phones without AArch32 (Snapdragon 8 Elite…), qemu passes the
     * call to the host kernel. The personality syscall stub (mov ip,r7; mov r7,#136; swi 0) gets "mov r0,#0"
     * instead of the swi, in libc.so and in static binaries (healthd, ueventd…). Done once per image.
     */
    private fun personalityNoop() {
        if (img.api < 23) return
        val stamp = File(root, ".aemu-personality")
        if (stamp.isFile && runCatching { stamp.readText().trim().toInt() }.getOrDefault(0) > 0) return
        val files = listOf(File(root, "system/lib/libc.so")) +
            listOf("sbin", "system/bin", "system/xbin").flatMap { File(root, it).listFiles().orEmpty().toList() }
        var n = 0
        for (f in files) {
            if (!f.isFile || isLink(f) || f.length() > 8_000_000 || f.length() < 1024) continue
            runCatching {
                val d = f.readBytes()
                if (d[0] != 0x7f.toByte() || d[1] != 'E'.code.toByte()) return@runCatching
                val b = java.nio.ByteBuffer.wrap(d).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                var hit = false
                var i = 0
                while (i + 12 <= d.size) {
                    // mov ip, r7 ; mov r7, #136 (6.0) or ldr r7, [pc, #x] holding 136 (7.x) ; swi #0
                    if (b.getInt(i) == 0xe1a0c007.toInt() && b.getInt(i + 8) == 0xef000000.toInt()) {
                        val w = b.getInt(i + 4)
                        val nr = when {
                            w == 0xe3a07088.toInt() -> 136
                            w and 0xfffff000.toInt() == 0xe59f7000.toInt() ->
                                (i + 4 + 8 + (w and 0xfff)).takeIf { it + 4 <= d.size }?.let { b.getInt(it) } ?: -1
                            else -> -1
                        }
                        if (nr == 136) { b.putInt(i + 8, 0xe3a00000.toInt()); hit = true }   // swi #0 → mov r0, #0
                    }
                    i += 4
                }
                if (hit) { f.writeBytes(d); n++ }
            }
        }
        log("personality(PER_LINUX32) made a no-op in $n files")
        runCatching { stamp.writeText("$n\n") }
    }

    /**
     * bionic 6.0 finds the main thread's stack in /proc/self/task/<pid>/maps; qemu only emulates /proc/self/maps,
     * so the guest parses the host's 64-bit map and ART gets random stack bounds ("Check failed: &stack_variable >
     * stack_end", then SIGSEGV in zygote). The format string in libc.so becomes "/proc/self/maps".
     */
    /**
     * 5.0+: the NFC app's own watchdog aborts it while the vendor NFC stack initialises under qemu; that
     * persistent crash loops the whole system. There is no NFC to offer, so the app is moved out of the way.
     */
    private fun parkNfc() {
        // 7.0+ zygote: the shim shows qemu's /dhd.owners descriptor under this whitelisted name; after fork
        // zygote reopens it by that path, which has to lead back to the same table
        if (img.api >= 24) runCatching {
            val link = File(root, "system/framework/aemu-owners.jar")
            if (!isLink(link)) { link.delete(); Os.symlink(paths.owners.absolutePath, link.path) }
        }
        if (img.api < 21) return
        val parked = File(root, "system/.aemu-parked").apply { mkdirs() }
        for (dir in listOf("system/app", "system/priv-app")) File(root, dir).listFiles()?.filter { it.name.startsWith("Nfc") }?.forEach { f ->
            val dst = File(parked, f.relativeTo(root).path.replace('/', '#'))
            if (f.renameTo(dst)) log("parked ${f.name}")
        }
    }

    private fun mainStackMaps() {
        if (img.api < 23) return
        val libc = File(root, "system/lib/libc.so")
        runCatching {
            val d = libc.readBytes()
            val from = "/proc/self/task/%d/maps".toByteArray()
            val i = indexOf(d, from)
            if (i < 0) return
            val to = "/proc/self/maps".toByteArray()
            for (k in from.indices) d[i + k] = if (k < to.size) to[k] else 0
            libc.writeBytes(d)
            log("libc: main thread stack read from /proc/self/maps")
        }.onFailure { log("libc maps patch failed: ${it.message}") }
    }

    private fun indexOf(d: ByteArray, p: ByteArray): Int {
        var i = 0
        while (i <= d.size - p.size) {
            if (d[i] == p[0] && (1 until p.size).all { d[i + it] == p[it] }) return i
            i++
        }
        return -1
    }

    private fun isLink(f: File) = runCatching { OsConstants.S_ISLNK(Os.lstat(f.absolutePath).st_mode) }.getOrDefault(false)
    private fun wipe(f: File) = ImageStore.wipe(f)

    companion object {
        private const val OPEN_GL = "engines/kk/libGLES_aemu.so"
        /** Мастера первого запуска: Samsung, Google, AOSP, MIUI, HTC, Sony */
        val SETUP_WIZARDS = listOf(
            "com.sec.android.app.SecSetupWizard", "com.google.android.setupwizard", "com.android.provision",
            "com.miui.provision", "com.htc.setupwizard", "com.sonyericsson.setupwizard", "com.sonymobile.setupwizard",
            // вход в Google-аккаунт на 2.x–4.x больше не работает (старые протоколы входа Google закрыл):
            // его экран не рисуется, запрещает «Домой», и «Контакты» при первом запуске запирают в нём систему
            "com.google.android.gsf.login",
            // Samsung 4.4 (Galaxy S5): Quick connect dies without a Bluetooth address, the S View cover service
            // without its cover font; both come back with a "has stopped" dialog over every screen
            "com.samsung.android.sconnect", "com.sec.android.sviewcover",
            // Samsung 6.0 (Galaxy S6): the TV beacon service parses the Bluetooth address at start and dies on an empty one
            "com.samsung.android.beaconmanager",
        )
        /** the entries of [SETUP_WIZARDS] that take over the screen at first boot (the AOSP provisioner finishes by itself) */
        val FIRST_RUN_WIZARDS = listOf("com.sec.android.app.SecSetupWizard", "com.google.android.setupwizard", "com.miui.provision",
            "com.htc.setupwizard", "com.sonyericsson.setupwizard", "com.sonymobile.setupwizard")
        /** Прошивка MediaTek, где AudioFlinger связан с собственной звуковой библиотекой MTK (/dev/eac). */
        fun isMtkAudio(root: File): Boolean {
            if (File(root, "system/lib/libaudio.mtk.so").isFile) return true   // already swapped for our HAL
            val f = File(root, "system/lib/libaudio.primary.default.so")
            if (!f.isFile || f.length() > 20_000_000) return false
            return runCatching { String(f.readBytes(), Charsets.ISO_8859_1).contains("AudioMTKHardware") }.getOrDefault(false)
        }

        val QCOM_PARK = listOf(
            "system/etc/permissions/qti_permissions.xml",
            "system/etc/permissions/com.qti.dpmframework.xml",
            "system/priv-app/com.qualcomm.location",
            "system/priv-app/qcrilmsgtunnel",
            "system/priv-app/dpmserviceapp",
            "system/priv-app/DiracAudioControlService",
            "system/app/DiracManager",
        )
        private val LOGS = listOf("main", "system", "radio") // events — канал, его делает EventsSink
        private val DATA_DIRS = listOf(
            "data", "data/app", "data/app-private", "data/app-lib", "data/app-asec", "data/data", "data/dalvik-cache",
            "data/local", "data/local/tmp", "data/misc", "data/misc/keystore", "data/misc/wifi", "data/misc/zoneinfo",
            "data/property", "data/system", "data/anr", "data/backup", "data/lost+found", "data/drm", "data/media",
            "data/resource-cache", "data/security", "data/tombstones", "cache", "cache/download", "cache/lost+found",
            // what init.rc makes for the daemons (a system-only image comes without that file; installd leaves without misc/user)
            "data/misc/user", "data/misc/systemkeys", "data/misc/vpn", "data/misc/net", "data/misc/shared_relro", "data/misc/media", "data/misc/gatekeeper",
            "mnt", "mnt/asec", "mnt/obb", "mnt/secure", "mnt/secure/asec", "mnt/shell", "mnt/media_rw", "storage",
        )
    }
}
