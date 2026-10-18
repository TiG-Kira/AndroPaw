package app.aemu.core

/**
 * The native arm64 engine comes as one "kernel" per Android API level: its own runner and binder daemon (built from
 * native/guest64/<tag>), its own guest files (assets/engines/n64-<tag>) and its own rules for preparing the tree.
 * A fix made for one release stays in that release's kernel; the others keep their binaries and rules as they were
 * tested. A new release starts as a copy of the nearest one. The rules are written for vendor firmware (MIUI,
 * One UI, MediaTek and Qualcomm builds) as much as for AOSP: nothing here may assume a generic system image.
 */
enum class Kernel64(
    val tag: String,
    val apis: IntRange,
    /** Treble: HALs are separate services in /vendor/bin/hw, reached over hwbinder; vndbinder exists. */
    val treble: Boolean,
    /** where libEGL looks for the arm64 GLES implementation */
    val eglDir: String,
    /** qemu.gles value that makes libEGL pick SwiftShader */
    val gles: String,
    /** android.hardware.<name>.xml features whose HAL this kernel does not provide: the framework treats "feature
     *  declared, HAL missing" as fatal, so the declaration is set aside. Where the firmware's own HAL loads, the
     *  declaration has to stay (Android 6: "IR HAL present, but FEATURE_CONSUMER_IR is not set"). */
    val absentFeatures: List<String>,
    /** The guest sees a kernel with per-app traffic accounting (empty tables). 8.1 needs it to finish booting: its
     *  network policy service never reports "ready" with accounting off, and system_server waits 30 s for it. */
    val trafficAccounting: Boolean = false,
    /** /dev/qemu_pipe cannot be opened or created. The framework of an emulator build (ro.kernel.qemu=1) opens it
     *  read-write with O_CREAT for the host clipboard and takes whatever lands in that file for a message length. */
    val noQemuPipe: Boolean = false,
    /** vold is a binder service (9+): the storage service calls it without checking that it exists, so the engine's
     *  stand-in (VoldStub in aemu-stubs.jar) goes up before zygote. Up to 8.1 vold is a socket the engine answers. */
    val binderVold: Boolean = false,
    /** the emulated volume is announced again when the user starts: the storage service of 13+ drops a volume that
     *  shows up before the user's media provider is known ("does not support external storage") */
    val volumeAtUserStart: Boolean = false,
    /** the name in system/lib/egl under which 32-bit programs find libGLES_route (native/glroute): their own drawing
     *  goes to the phone's GPU through the 4.4 engine's bridge (served by glserverd), hwui stays on SwiftShader. They run in the translator, where a software renderer gives about
     *  one frame a second. null: this kernel has no bridge and they draw with its software renderer. */
    val gpuBridge32: String? = null,
    /** 32-bit programs get the open bridge (native/glbridge, armv7) under that name instead of glroute: hwui and
     *  Chromium draw on the phone's GPU too (the 4.4 engine's closed bridge has no EGLImage of a texture), with the
     *  engine's 32-bit SwiftShader behind it */
    val gpuBridge32Open: Boolean = false,
    /**
     * SurfaceFlinger is told to compose every layer itself (its own debug call 1008). 11: over the 8.1 composer
     * service its layers stay "device" ones, which the framebuffer composer never draws: a black screen.
     * Not for a kernel with the engine's own hwcomposer (native/hwchal: 33, 34), which draws such layers itself.
     */
    val clientComposition: Boolean = false,
    /**
     * WebView renders in the app's own process (the platform's switch, `cmd webviewupdate disable-multiprocess`,
     * set after every boot). The renderer processes come from webview_zygote, which dies here asking for the
     * SELinux context of its files (getfilecon: there is no SELinux for the guest): pages stay blank.
     */
    val webViewSingleProcess: Boolean = false,
    /** files the guest finds in /proc over the phone's own (path under /proc to content) */
    val procFiles: Map<String, String> = emptyMap(),
    /** The name libEGL knows the engine's GLES implementation by (lib<API>_<name>.so). 6.0 and 8.0+ take whatever
     *  sits in the directory; 7.x on an "emulator with a GPU" loads the name "emulation" and nothing else. */
    val eglName: String = "swiftshader",
    /** the firmware's libEGL may still pick its driver by the tags of egl.cfg (vendor builds of 5.x) */
    val eglCfgTags: Boolean = false,
    /** the 32-bit libc may read /proc/self/task/<tid>/maps for the main stack (Qualcomm builds of 5.1), which
     *  qemu-arm does not answer: the library is pointed at /proc/self/maps */
    val taskMapsIsSelfMaps: Boolean = false,
    /** The kernel ships a gralloc that gives out handles in the layout of Samsung's Exynos gralloc
     *  (libgralloc_exynos.so next to the system libraries): it goes in place of the firmware's default one where libgui checks every queued
     *  buffer for that layout (TouchWiz), and nowhere else. */
    val exynosHandles: Boolean = false,
    /** The kernel ships a gralloc that gives out handles in the layout of Qualcomm's gralloc (libgralloc_qcom.so next
     *  to the system libraries): it goes in place of the firmware's default one where the firmware's SurfaceFlinger is
     *  Qualcomm's, which reads "secure display" and "external only" out of every layer's handle and hides the layer,
     *  or all the others, by what a default handle has in that place (MIUI 8 on 7.0: a black screen). */
    val qcomHandles: Boolean = false,
    /** The infrared feature is declared only while an infrared HAL is in place: a firmware with a default one keeps
     *  it (MIUI), one that had only its board's (TouchWiz: consumerir.exynos5.so, set aside) loses the declaration. */
    val irFollowsHal: Boolean = false,
    /** The firmware's 32-bit AudioFlinger may be one with a 64-bit audio_format_t (TouchWiz): it gets the build of the
     *  audio HAL that returns formats that way. */
    val wideAudioFormat: Boolean = false,
    /** The settings are XML files (6.0+), written by the engine after the first boot: screen always on, device set
     *  up, setup wizards off. Per kernel, as each release's files are checked. */
    val xmlSettings: Boolean = false,
    /** The firmware's boot image is linked into /data/dalvik-cache where its ART wants a copy there (TouchWiz: it
     *  empties the cache at every start otherwise, and every app is compiled again on every boot). */
    val bootImageInCache: Boolean = false,
    /** Where the firmware declares no Ethernet service, the engine's network process registers an empty one: the
     *  guest's network is an Ethernet one, and a vendor's System UI may ask the service about it unchecked (TouchWiz). */
    val ethernetStandIn: Boolean = false,
    /** The engine's network process registers the guest's network with ConnectivityService (NetStub): without one
     *  the system reports no active network and browsers stay offline. The 32-bit engine takes it by API range (5.0-7.x). */
    val registeredNetwork: Boolean = false,
    /** The vendor side's minijail (8.1) takes one "@include" in a seccomp policy; the system's policy for the codec
     *  service has a second one, for coverage builds, which is dropped. */
    val oneIncludePolicy: Boolean = false,
    /** Binder services the engine registers empty stand-ins for (StandIn in aemu-stubs.jar), each with the file of the
     *  firmware that calls it unchecked: a firmware whose file does not name the service gets no stand-in. TouchWiz on
     *  Exynos: SurfaceFlinger reports the end of the boot to the display HAL's service and dies without it.
     *  ("file#text": the file is looked up for that text, not for the service's name.) */
    val standInServices: Map<String, String> = emptyMap(),
    /** Files the guest finds in /sys (path under /sys to content) where the firmware has none of its own there.
     *  TouchWiz on Exynos: its frequency service reads the board's CPU, GPU and memory bus frequency tables, hands
     *  out null for a missing one, and Samsung's own apps index the table unchecked (GameService dies at every boot). */
    val sysFiles: Map<String, String> = emptyMap(),
    /** The engine's name lookup service finds out which layout of the answer the firmware's libc reads: 5.1 changed
     *  it, and a vendor's 5.1 may still have the libc of 5.0 (TouchWiz), for which every lookup failed. */
    val dnsLayoutProbe: Boolean = false,
    /** properties this kernel sets over the firmware's own */
    val props: Map<String, String> = emptyMap(),
    /** properties this kernel sets where the firmware has none of its own (a generic system image over the engine's
     *  vendor side has no vendor properties; a vendor's firmware keeps what its vendor side declares) */
    val propDefaults: Map<String, String> = emptyMap(),
    /** Engine files (asset names) that a firmware with its own vendor side keeps its own of, where it has them: the
     *  engine's copy is for a generic system image, which has none. */
    val firmwareOwn: List<String> = emptyList(),
    /** Files (asset-style names) of a firmware with its own vendor side that are set aside with nothing in their
     *  place: what would be picked before the engine's own implementation of the same thing. */
    val firmwareAside: List<String> = emptyList(),
    /** netd of this release attaches its traffic programs to the cgroup v2 root and exits without one: the list of
     *  cgroup controllers init leaves in /dev/cgroup_info names it (an empty directory under /sys/fs/cgroup). */
    val cgroup2Root: Boolean = false,
    /** ...in the layout of 15: a controller's record carries max_activation_depth (60 bytes, not 56) */
    val cgroupRcDepth: Boolean = false,
    /** 16: a process's group is /sys/fs/cgroup/<one of these>/uid_N ("system" below the first app uid, "apps" from
     *  it on), and zygote does not make the parent: the empty directories are in the image */
    val cgroupUidParents: List<String> = emptyList(),
    /** 17: a flag that cannot be read from the aconfig storage is an error (AdvancedProtectionService does not
     *  start, system_server with it). The storage is /metadata/aconfig, which aconfigd fills at boot from the
     *  partitions' and modules' own files; the tree fixer does that here. */
    val aconfigStorage: Boolean = false,
    /** 17: the build check at boot (Build.isBuildConsistent) always compares the device with the framework
     *  compatibility matrix, the kernel policy version included. libvintf asks libselinux for it, and libselinux
     *  has no selinuxfs here, so the version reads as 0 and "There is an internal problem with your device" comes
     *  up at every boot. The tree fixer sets the version the matrix asks for to 0: nothing to compare with. */
    val kernelPolicyUnread: Boolean = false,
    /** 64-bit programs draw on the phone's GPU: libEGL is told to load the engine's bridge driver (libGLES_aemu.so,
     *  native/glbridge), which keeps SwiftShader for the compositor and the window toolkit; the application hosts
     *  the renderer (GlHost) */
    val gpuBridge64: Boolean = false,
    /** debug.aemu.gl when the bridge is up (guest.c policy): 3 - the compositor too, which needs the bridge's note in
     *  every gralloc buffer (AEMU_META gralloc); 1 - only programs' own contexts, frames read back on swap (7.1: the
     *  firmware's qcom gralloc has no note) */
    val gpuBridgeMode: String = "3",
    /** 13: the product and system_ext partitions are directories of the root (as mounted on a device) with links to
     *  them under /system, not the other way round: zygote lets system_server keep only the jars whose real path is
     *  the one in BOOTCLASSPATH (/system_ext/framework/...). */
    val partitionsAtRoot: Boolean = false,
    /** 13: the kernel answers for the size of a pinned eBPF map key and value (netd aborts on a mismatch); the tree
     *  fixer notes them from the firmware eBPF objects in dev/.aemu-bpf. */
    val bpfMapNotes: Boolean = false,
    /** where the firmware keeps its linker configuration generator (14: inside the runtime APEX) */
    val linkerConfigTool: String = "system/bin/linkerconfig",
    /** Qualcomm's libmedia opens the camcorder profiles under a name the vendor's init script would have picked by
     *  the chip, and that of 14 aborts when the file is not there: the missing names are linked to a file that is */
    val mediaProfilesFallback: Boolean = false,
    /** the vendor's and the boot image's build fingerprints read as the system's (see GuestVm) */
    val oneFingerprint: Boolean = false,
    /** dalvik.vm.heap* come from the vendor's build.prop where it has them (see GuestVm) */
    val vendorHeapProps: Boolean = false,
    /** init of this release makes the product's identity (ro.product.model and the like, ro.build.fingerprint) out
     *  of the per-partition properties where the firmware does not state it; the engine does it in init's place */
    val derivedIdentity: Boolean = false,
    /** Where the engine's copy of the VNDK its HAL services were built with goes (under the image root), over a
     *  firmware with its own vendor side and only the VNDK of its own release: those services are started with this
     *  directory as their library path, the firmware's own programs keep the firmware's VNDK. 11: nothing of 8.1's
     *  HALs links against VNDK 30, and the SDK emulator of 11 has no generic HALs to take instead. */
    val ownVndkDir: String? = null,
    /** Engine HAL services that are NOT built for that old VNDK: the directory of their own libraries, by the asset
     *  name of the program (15: the audio@7.0 service, built for VNDK 34, which the system image itself carries) */
    val ownLibDirs: Map<String, String> = emptyMap(),
    /** The pieces of the HAL manifest a firmware keeps next to it (vendor/etc/vintf/manifest/, 10+) are set aside
     *  with the manifest itself: they declare the board's HALs (Qualcomm's buffer mapper, composer and allocator),
     *  which the framework would then take before the engine's. */
    val manifestFragmentsAside: Boolean = false,
    /** The same for the fragments of the odm partition (odm/etc/vintf/manifest/): the board's own HALs again, and
     *  ColorOS keeps there a link into a /proc directory of its kernel, without which the whole manifest is void. */
    val odmManifestFragmentsAside: Boolean = false,
    /** servicemanager's request for a lazy AIDL service (ctl.interface_start aidl/<name>) starts the service that
     *  declares the interface; without it system_server waits for the service forever (ColorOS 14: cotaservice). */
    val lazyAidlServices: Boolean = false,
    /** 15+: the package manager asks apexd's binder service for the modules (no "flattened" mode any more); the
     *  engine answers with its own list (ApexStub in aemu-stubs.jar) and apexd is not started */
    val apexStandIn: Boolean = false,
    /** 15+: WebView cannot be switched to one process any more; its package is made 64-bit first, so that the
     *  renderer processes are native ones and not children of the translated 32-bit zygote (see WebView64) */
    val webView64: Boolean = false,
    /** 8.1-9: libEGL's loader sets qemu.gles itself (1 when ro.kernel.qemu.gles is 1, else 2 where /vendor/lib64/egl
     *  exists) and then loads the "emulation" driver for 1, SwiftShader for 2, never ro.hardware.egl's: with the GPU
     *  bridge up the flag is 1 and the bridge's driver is there under the emulator's name too (32-bit programs still
     *  read 0 through the runner) */
    val bridgeAsEmulation: Boolean = false,
    /** where that "emulation" driver is looked for, when not in eglDir (8.0's loader: only /system/lib64/egl) */
    val emulationDir: String? = null,
    /** 8.0's SystemUI asks for the navigation bar's view when the lock screen shows and dies on a device without one
     *  (fixed in 8.1): the guest's own bar stays even where the app's buttons replace it */
    val navBarRequired: Boolean = false,
    /** the policy's audio_policy_configuration.xml is replaced by the engine's single-output one (TreeFixer.audioPolicy) */
    val audioPolicyXml: Boolean = false,
    /** /proc/meminfo shows the image's RAM budget (the setting, or 4 GB), not the phone's memory */
    val ramBudget: Boolean = false,
    /** Debugging a guest that takes the phone's kernel down: aemu.log is synced to storage line by line, and
     *  <root>/dev/.aemu-stop-after (a number N) starts only the first N services of the plan */
    val syncLog: Boolean = false,
    /** ro.boot.hardware is set to the board family the firmware names its init.<hardware>.rc after (qcom, mt6xxx) */
    val bootHardwareProp: Boolean = false,
    /** ...but for the fragments named so: an AIDL HAL of the firmware that does run here registers only when it is
     *  declared (14: system_server waits for the health HAL, which is AIDL from this release on) */
    val manifestFragmentsKept: List<String> = emptyList(),
    /** The firmware's generator of the linker configuration (11+) is run as on a Treble device, whatever
     *  ro.treble.enabled says to the rest of the system. */
    val trebleLinkerConfig: Boolean = false,
    /** Size of the property area. A phone has one of 128 KB per SELinux context, the guest has one for all: a
     *  vendor's firmware of 11 (MIUI 12.5) fills that before the package manager is up. */
    val propAreaSize: Int = PropArea.SIZE,
    /** The own-vendor files (see [ownVendorAssets]) also go under a generic system image, which brings no vendor
     *  side at all: from 12 on the system has no VNDK the 8.1-built HALs of the base set could link against. */
    val ownVendorUnderGsi: Boolean = false,
) {
    // Android 5.0: starts as kernel 23
    A5_0("21", 21..21, registeredNetwork = true, eglCfgTags = true, trafficAccounting = true, treble = false, eglDir = "system/lib64/egl", gles = "1", absentFeatures = emptyList(),
        procFiles = mapOf("asound/cards" to "--- no soundcards ---\n")),
    // Android 5.1: starts as kernel 23. A board's own infrared HAL (consumerir.<board>.so) is set aside with the other
    // vendor HALs, and 5.1 takes "feature declared, HAL missing" for fatal.
    A5_1("22", 22..22, registeredNetwork = true, dnsLayoutProbe = true, exynosHandles = true, wideAudioFormat = true, bootImageInCache = true, ethernetStandIn = true, eglCfgTags = true, taskMapsIsSelfMaps = true, trafficAccounting = true, treble = false, eglDir = "system/lib64/egl", gles = "1",
        absentFeatures = listOf("consumerir"),
        standInServices = mapOf("Exynos.HWCService" to "system/lib64/libsurfaceflinger.so"),
        sysFiles = mapOf(
            "power/cpufreq_table" to "1500000 1400000 1300000 1200000 1100000 1000000 900000 800000 700000 600000 500000 400000\n",
            "devices/14ac0000.mali/dvfs_table" to "772 700 600 544 420 350 266\n",
            "devices/platform/exynos7-devfreq-mif/devfreq/exynos7-devfreq-mif/available_frequencies" to
                "1552000 1456000 1264000 1068000 828000 676000 543000 416000 348000 276000\n",
            "devices/platform/exynos5-devfreq-mif/devfreq/exynos5-devfreq-mif/available_frequencies" to
                "825000 728000 633000 543000 413000 275000 206000 165000 138000\n"),
        procFiles = mapOf("asound/cards" to "--- no soundcards ---\n")),
    // Android 6.0
    A6("23", 23..23, gpuBridge32 = "libGLES.so", gpuBridge32Open = true, gpuBridge64 = true, gpuBridgeMode = "3", registeredNetwork = true, exynosHandles = true, irFollowsHal = true, wideAudioFormat = true, xmlSettings = true, bootImageInCache = true, ethernetStandIn = true, treble = false, eglDir = "system/lib64/egl", gles = "1", absentFeatures = emptyList(),
        // Qualcomm's boot animation (MIUI 9) reads the list of sound cards and takes a missing file for an open one
        // (it dies at once and the boot goes on without an animation); the phone's own list is not readable
        procFiles = mapOf("asound/cards" to "--- no soundcards ---\n"),
        // The storage the engine's vold announces is an emulated one. A firmware that kept the SDK emulator's
        // init.goldfish.rc (TouchWiz) declares the primary storage physical there, and its storage service then
        // never mounts the emulated volume: no /sdcard for the apps.
        props = mapOf("ro.vold.primary_physical" to "0")),
    // Android 7.0: starts as kernel 23
    A7_0("24", 24..24, registeredNetwork = true, qcomHandles = true, irFollowsHal = true, xmlSettings = true, treble = false, eglDir = "system/lib64/egl", gles = "1", absentFeatures = emptyList(),
        eglName = "emulation",
        procFiles = mapOf("asound/cards" to "--- no soundcards ---\n")),
    // Android 7.1: starts as kernel 23
    A7_1("25", 25..25, gpuBridge64 = true, gpuBridgeMode = "1", bridgeAsEmulation = true, registeredNetwork = true, qcomHandles = true, irFollowsHal = true, xmlSettings = true, treble = false, eglDir = "system/lib64/egl", gles = "1", absentFeatures = emptyList(),
        eglName = "emulation",
        procFiles = mapOf("asound/cards" to "--- no soundcards ---\n"),
        // The first boot runs dex2oat over every app that came without compiled code (Google's apps on Xperia), one
        // by one, and the 32-bit ones through the translator: minutes on the boot screen. Verification is left to
        // ART at run time.
        props = mapOf("pm.dexopt.first-boot" to "verify-at-runtime", "pm.dexopt.boot" to "verify-at-runtime")),
    // Android 8.0: starts as kernel 27
    A8_0("26", 26..26, gpuBridge64 = true, bridgeAsEmulation = true, emulationDir = "system/lib64/egl", navBarRequired = true, audioPolicyXml = true, webViewSingleProcess = true, registeredNetwork = true, treble = true, eglDir = "vendor/lib64/egl", gles = "2",
        absentFeatures = listOf("consumerir", "fingerprint", "nfc", "nfc.hce", "nfc.hcef"), trafficAccounting = true, noQemuPipe = true,
        // 8.0's Skia pipeline of hwui is experimental and stops system_server with "GL errors!" (SkiaOpenGLPipeline):
        // 8.0 draws with its OpenGL pipeline
        props = mapOf("ro.treble.enabled" to "false", "debug.hwui.renderer" to "opengl"),
        // The engine's codec service is the 64-bit one of the emulator image, whose codec library a phone's system of
        // 8.0 has only as a 32-bit build: a firmware with its own vendor side keeps its own (32-bit) service.
        firmwareOwn = listOf("vendor#bin#hw#android.hardware.media.omx@1.0-service", "vendor#etc#init#android.hardware.media.omx@1.0-service.rc",
            "vendor#lib#libmediacodecservice.so")),
    // Android 8.1
    A8_1("27", 27..27, gpuBridge64 = true, bridgeAsEmulation = true, qcomHandles = true, webViewSingleProcess = true, registeredNetwork = true, treble = true, eglDir = "vendor/lib64/egl", gles = "2",
        // MIUI's system_server reads the panel colour from the touch controller's lockdown bytes before anything else
        // and dies closing the reader it never opened when the file is not there
        procFiles = mapOf("tp_lockdown_info" to "4636320131320200\n"),
        absentFeatures = listOf("consumerir", "fingerprint", "nfc", "nfc.hce", "nfc.hcef"), trafficAccounting = true, noQemuPipe = true,
        // The framework calls a Treble device consistent only after checking the running kernel (its SELinux policy
        // version, release and build options) against its certification list. There is no SELinux for the guest and
        // the kernel is the phone's, so that check cannot be passed: the device is declared as not certified, which
        // is all this property means to 8.x (the linker and HIDL do not read it).
        // Qualcomm's Wi-Fi service sets a property whose name the old protocol (31 characters) cannot carry, and
        // system_server dies in its constructor.
        // A vendor build's first boot compiles every app that came without compiled code (Google's ones on MIUI):
        // five minutes on the boot screen. They are only unpacked; ART verifies at run time.
        props = mapOf("ro.treble.enabled" to "false", "ro.property_service.version" to "2",
            "pm.dexopt.first-boot" to "extract", "pm.dexopt.boot" to "extract")),
    // Android 9 over the vendor side of 8.1, which is what a generic system image of 9 is made for: the HALs are the
    // ones of kernel 27 (copied, so that the two can part), and the system finds its copy of their libraries by the
    // VNDK version the vendor side declares.
    A9("28", 28..28, gpuBridge64 = true, webView64 = true, bridgeAsEmulation = true, qcomHandles = true, webViewSingleProcess = true, registeredNetwork = true, treble = true, eglDir = "vendor/lib64/egl", gles = "2",
        // (MIUI's system_server and the touch controller's lockdown bytes: as on 8.1)
        procFiles = mapOf("tp_lockdown_info" to "4636320131320200\n"),
        absentFeatures = listOf("consumerir", "fingerprint", "nfc", "nfc.hce", "nfc.hcef"), trafficAccounting = true, noQemuPipe = true, binderVold = true,
        // (property protocol 2 and unpack-only first boot: a vendor's firmware needs them as on 8.1)
        props = mapOf("ro.treble.enabled" to "false", "ro.property_service.version" to "2",
            "pm.dexopt.first-boot" to "extract", "pm.dexopt.boot" to "extract"),
        // A vendor's firmware of 9 brings its own vendor side and the VNDK of its own release: its linker
        // configuration is the one for that version, and with another one named there is no configuration at all
        // (every program in one namespace, the vendor's services without the VNDK libraries they were built with).
        propDefaults = mapOf("ro.vndk.version" to "27"),
        // The codec service of a phone is a 32-bit program and so are the software codecs in its VNDK: the firmware's
        // own one is the one built for them (the engine's is 8.1's, which does not link against the libraries of 9).
        firmwareOwn = listOf("vendor#bin#hw#android.hardware.media.omx@1.0-service", "vendor#etc#init#android.hardware.media.omx@1.0-service.rc",
            "vendor#lib#libavservices_minijail_vendor.so", "vendor#lib#libmediacodecservice.so", "vendor#lib#libminijail_vendor.so",
            // (and the buffer mapper for 32-bit programs: the engine has that one built for 8.1 only)
            "vendor#lib#hw#android.hardware.graphics.mapper@2.0-impl.so")),
    // Android 10: starts as kernel 28 (the 8.1 vendor side under a generic system image)
    A10("29", 29..29, gpuBridge64 = true, webView64 = true, qcomHandles = true, derivedIdentity = true, oneIncludePolicy = true, webViewSingleProcess = true, registeredNetwork = true, treble = true, eglDir = "vendor/lib64/egl", gles = "2",
        absentFeatures = listOf("consumerir", "fingerprint", "nfc", "nfc.hce", "nfc.hcef", "bluetooth", "bluetooth_le"), trafficAccounting = true, noQemuPipe = true, binderVold = true,
        // netd of 10 waits for the eBPF loader, which has nothing to load into the phone's kernel from here: the
        // programs are declared loaded, and the device as one that shipped with 8.1 (as its vendor side says), for
        // which netd keeps to the traffic accounting of before eBPF, the one the engine provides.
        // A debuggable build's runtime connects every process to adbd's JDWP socket, which here is the phone's own adbd
        // (it answers, and the thread spins): no JDWP.
        props = mapOf("ro.treble.enabled" to "false", "bpf.progs_loaded" to "1", "ro.product.first_api_level" to "27",
            "dalvik.vm.jdwp-provider" to "none",
            // the Bluetooth app of 10 sets a property whose name the old protocol (31 characters) cannot carry
            "ro.property_service.version" to "2",
            // (unpack-only first boot: a vendor's firmware needs it as on 8.1)
            "pm.dexopt.first-boot" to "extract", "pm.dexopt.boot" to "extract",
            // libEGL of 10 looks for any driver only where no driver is named: a firmware that names its GPU's one
            // (set aside here) gets the engine's named instead
            "ro.hardware.egl" to "swiftshader",
            // no FRP partition here: with the firmware's path set, TestHarnessModeService reads it and aborts the boot
            "ro.frp.pst" to ""),
        // (MIUI's system_server and the touch controller's lockdown bytes: as on 8.1)
        procFiles = mapOf("tp_lockdown_info" to "4636320131320200\n"),
        // (a vendor's firmware of 10 has the VNDK of its own release only: as on 9)
        propDefaults = mapOf("ro.vndk.version" to "29"), ownVendorUnderGsi = true,
        // (a GSI of 10 has no health service, and system_server's BatteryService waits for one: the goldfish one of 11,
        // n64-29-gsi, with its HIDL libraries of its own)
        ownLibDirs = mapOf("vendor#bin#hw#android.hardware.health@2.0-service.goldfish" to "vendor/lib64/aemu-health"),
        // (the firmware's own 32-bit codec service and buffer mapper: as on 9)
        firmwareOwn = listOf("vendor#bin#hw#android.hardware.media.omx@1.0-service", "vendor#etc#init#android.hardware.media.omx@1.0-service.rc",
            "vendor#lib#libavservices_minijail_vendor.so", "vendor#lib#libmediacodecservice.so", "vendor#lib#libminijail_vendor.so",
            "vendor#lib#hw#android.hardware.graphics.mapper@2.0-impl.so"),
        // The audio service registers the newest implementation it finds and the framework asks for the newest one
        // declared: the firmware's 5.0 one (it crashes over the engine's audio module) is out of the way of the 4.0
        // one the engine brings.
        firmwareAside = listOf("vendor#lib64#hw#android.hardware.audio@5.0-impl.so", "vendor#lib64#hw#android.hardware.audio.effect@5.0-impl.so",
            // (the passthrough lookup takes any "-impl*.so": Qualcomm's one waits for the secure world forever)
            "vendor#lib64#hw#android.hardware.keymaster@3.0-impl-qti.so")),
    // Android 11: starts as kernel 29
    A11("30", 30..30, webView64 = true, derivedIdentity = true, clientComposition = true, gpuBridge64 = true, oneIncludePolicy = true, webViewSingleProcess = true, registeredNetwork = true, treble = true, eglDir = "vendor/lib64/egl", gles = "2",
        absentFeatures = listOf("consumerir", "fingerprint", "nfc", "nfc.hce", "nfc.hcef", "bluetooth", "bluetooth_le"), trafficAccounting = true, noQemuPipe = true, binderVold = true,
        // APEX packages are unpacked by the engine into plain directories: "not updatable" is the mode in which apexd
        // and the package manager take them that way instead of mounting package files
        props = mapOf("ro.treble.enabled" to "false", "bpf.progs_loaded" to "1", "ro.product.first_api_level" to "27",
            "ro.apex.updatable" to "false", "dalvik.vm.jdwp-provider" to "none", "ro.property_service.version" to "2",
            // zygote of 11 hides the other apps' data from an app with mounts over /data, which are not made here
            // (the symlinks it then creates collide with the real directories): the platform's own switch for it
            "persist.zygote.app_data_isolation" to "false",
            // (unpack-only first boot, the engine's GLES driver by name, no FRP partition: a vendor's firmware needs
            // them as on 10)
            "pm.dexopt.first-boot" to "extract", "pm.dexopt.boot" to "extract", "ro.hardware.egl" to "swiftshader", "ro.frp.pst" to "",
            // an encoder's input surface comes from Codec2 itself: the OMX store here has no IOmx behind it and
            // CCodec::CreateOmxInputSurface dereferences it (every app recording video from a surface died)
            "debug.stagefright.c2inputsurface" to "1"),
        // (MIUI's system_server and the touch controller's lockdown bytes: as on 8.1)
        procFiles = mapOf("tp_lockdown_info" to "4636320131320200\n"),
        // (a vendor's firmware of 11 has the VNDK of its own release only: as on 9)
        propDefaults = mapOf("ro.vndk.version" to "28", "ro.vendor.build.security_patch" to "2020-08-05"), ownVendorUnderGsi = true,
        // (the generic system image's health service is built against a newer libc++ than the engine's VNDK 27 copy)
        ownLibDirs = mapOf("vendor#bin#hw#android.hardware.health@2.0-service.goldfish" to "vendor/lib64/aemu-health"),
        // MIUI's display feature service reads the panel's name first and, with no such file, skips the rest of its
        // constructor (its sensor manager stays null and system_server dies on the first screen effect)
        sysFiles = mapOf("android_lcd/lcd_name" to "aemu\n"),
        ownVndkDir = "vendor/lib64/aemu-vndk", manifestFragmentsAside = true, trebleLinkerConfig = true, propAreaSize = 512 * 1024,
        // (the firmware's own 32-bit codec service: as on 9)
        firmwareOwn = listOf("vendor#bin#hw#android.hardware.media.omx@1.0-service", "vendor#etc#init#android.hardware.media.omx@1.0-service.rc",
            "vendor#lib#libavservices_minijail_vendor.so", "vendor#lib#libmediacodecservice.so", "vendor#lib#libminijail_vendor.so"),
        // (the passthrough lookup takes any "-impl*.so": Qualcomm's ones wait for the secure world forever)
        firmwareAside = listOf("vendor#lib64#hw#android.hardware.keymaster@3.0-impl-qti.so", "vendor#lib64#hw#android.hardware.gatekeeper@1.0-impl-qti.so")),
    // Android 12: starts as kernel 30
    A12("31", 31..31, derivedIdentity = true, gpuBridge64 = true, oneIncludePolicy = true, webViewSingleProcess = true, registeredNetwork = true, treble = true, eglDir = "vendor/lib64/egl", gles = "2",
        absentFeatures = listOf("consumerir", "fingerprint", "nfc", "nfc.hce", "nfc.hcef", "bluetooth", "bluetooth_le"), trafficAccounting = true, noQemuPipe = true, binderVold = true,
        // (12 has no VNDK 27 any more: the 8.1 vendor side links against the oldest one there is)
        props = mapOf("ro.treble.enabled" to "false", "bpf.progs_loaded" to "1", "ro.product.first_api_level" to "27", "debug.stagefright.c2inputsurface" to "1",
            "ro.apex.updatable" to "false", "dalvik.vm.jdwp-provider" to "none", "ro.property_service.version" to "2",
            "persist.zygote.app_data_isolation" to "false",
            // (a vendor's firmware: as on 11)
            "pm.dexopt.first-boot" to "extract", "pm.dexopt.boot" to "extract", "ro.hardware.egl" to "swiftshader", "ro.frp.pst" to "",
            // SurfaceFlinger of 12 composes with Skia by default, which draws nothing over SwiftShader: the GLES
            // composer of the releases before is still in it
            "debug.renderengine.backend" to "gles"),
        procFiles = mapOf("tp_lockdown_info" to "4636320131320200\n"),
        propDefaults = mapOf("ro.vndk.version" to "32", "ro.vendor.build.security_patch" to "2022-07-05"), ownVendorUnderGsi = true,
        sysFiles = mapOf("android_lcd/lcd_name" to "aemu\n"),
        ownVndkDir = "vendor/lib64/aemu-vndk", manifestFragmentsAside = true, trebleLinkerConfig = true, propAreaSize = 512 * 1024, cgroup2Root = true, oneFingerprint = true, vendorHeapProps = true,
        firmwareOwn = listOf("vendor#bin#hw#android.hardware.media.omx@1.0-service", "vendor#etc#init#android.hardware.media.omx@1.0-service.rc",
            "vendor#lib#libavservices_minijail_vendor.so", "vendor#lib#libmediacodecservice.so", "vendor#lib#libminijail_vendor.so"),
        firmwareAside = listOf("vendor#lib64#hw#android.hardware.keymaster@3.0-impl-qti.so", "vendor#lib64#hw#android.hardware.gatekeeper@1.0-impl-qti.so")),
    // Android 13: a copy of the kernel of 12 (its own from here on)
    A13("33", 33..33, volumeAtUserStart = true, derivedIdentity = true, oneIncludePolicy = true, webViewSingleProcess = true, registeredNetwork = true, treble = true, eglDir = "vendor/lib64/egl", gles = "2",
        absentFeatures = listOf("consumerir", "fingerprint", "nfc", "nfc.hce", "nfc.hcef", "bluetooth", "bluetooth_le"), trafficAccounting = true, noQemuPipe = true, binderVold = true,
        // (12 has no VNDK 27 any more: the 8.1 vendor side links against the oldest one there is)
        props = mapOf("ro.treble.enabled" to "false", "bpf.progs_loaded" to "1", "ro.product.first_api_level" to "27", "debug.stagefright.c2inputsurface" to "1",
            "ro.apex.updatable" to "false", "dalvik.vm.jdwp-provider" to "none", "ro.property_service.version" to "2",
            "persist.zygote.app_data_isolation" to "false",
            // (a vendor's firmware: as on 11)
            "pm.dexopt.first-boot" to "extract", "pm.dexopt.boot" to "extract", "ro.hardware.egl" to "swiftshader", "ro.frp.pst" to "",
            // SurfaceFlinger of 12 composes with Skia by default, which draws nothing over SwiftShader: the GLES
            // composer of the releases before is still in it
            "debug.renderengine.backend" to "gles"),
        procFiles = mapOf("tp_lockdown_info" to "4636320131320200\n"),
        propDefaults = mapOf("ro.vndk.version" to "28", "ro.vendor.build.security_patch" to "2023-08-05"), ownVendorUnderGsi = true, gpuBridge64 = true,
        sysFiles = mapOf("android_lcd/lcd_name" to "aemu\n"),
        ownVndkDir = "vendor/lib64/aemu-vndk", manifestFragmentsAside = true, trebleLinkerConfig = true, propAreaSize = 512 * 1024, cgroup2Root = true, partitionsAtRoot = true, bpfMapNotes = true, oneFingerprint = true, vendorHeapProps = true,
        firmwareOwn = listOf("vendor#bin#hw#android.hardware.media.omx@1.0-service", "vendor#etc#init#android.hardware.media.omx@1.0-service.rc",
            "vendor#lib#libavservices_minijail_vendor.so", "vendor#lib#libmediacodecservice.so", "vendor#lib#libminijail_vendor.so"),
        firmwareAside = listOf("vendor#lib64#hw#android.hardware.keymaster@3.0-impl-qti.so", "vendor#lib64#hw#android.hardware.gatekeeper@1.0-impl-qti.so")),
    // Android 14: a copy of the kernel of 13 (its own from here on)
    A14("34", 34..34, volumeAtUserStart = true, derivedIdentity = true, oneIncludePolicy = true, webViewSingleProcess = true, registeredNetwork = true, treble = true, eglDir = "vendor/lib64/egl", gles = "2",
        absentFeatures = listOf("consumerir", "fingerprint", "nfc", "nfc.hce", "nfc.hcef", "bluetooth", "bluetooth_le"), trafficAccounting = true, noQemuPipe = true, binderVold = true,
        // (12 has no VNDK 27 any more: the 8.1 vendor side links against the oldest one there is)
        props = mapOf("ro.treble.enabled" to "false", "bpf.progs_loaded" to "1", "ro.product.first_api_level" to "27", "debug.stagefright.c2inputsurface" to "1",
            "ro.apex.updatable" to "false", "dalvik.vm.jdwp-provider" to "none", "ro.property_service.version" to "2",
            "persist.zygote.app_data_isolation" to "false",
            // (a vendor's firmware: as on 11)
            "pm.dexopt.first-boot" to "extract", "pm.dexopt.boot" to "extract", "ro.hardware.egl" to "swiftshader", "ro.frp.pst" to "",
            // SurfaceFlinger of 12 composes with Skia by default, which draws nothing over SwiftShader: the GLES
            // composer of the releases before is still in it
            "debug.renderengine.backend" to "gles",
            // the sound trigger HAL takes the engine's stand-in module, not the board's (which wants its DSP)
            "ro.hardware.sound_trigger.primary" to "aemu",
            // app compilation shares the phone's cores with the guest's UI, which SwiftShader draws on them too
            "dalvik.vm.dex2oat-threads" to "2", "dalvik.vm.background-dex2oat-threads" to "1", "dalvik.vm.boot-dex2oat-threads" to "2"),
        procFiles = mapOf("tp_lockdown_info" to "4636320131320200\n"),
        // (a generic system image of 14 carries the VNDK of 29..34 only, and its linkerconfig aborts on a version it
        // has no package of; the engine's HAL services find their own VNDK 28 copy by ownVndkDir)
        // (the software KeyMint inside keystore2 waits for the vendor's patch level property to exist, for ever over
        // a generic system image: no keystore service, system_server dies in LockSettingsService)
        propDefaults = mapOf("ro.vndk.version" to "34", "ro.vendor.build.security_patch" to "2023-12-05"),
        sysFiles = mapOf("android_lcd/lcd_name" to "aemu\n"),
        ownVndkDir = "vendor/lib64/aemu-vndk", manifestFragmentsAside = true, trebleLinkerConfig = true, propAreaSize = 512 * 1024, cgroup2Root = true, partitionsAtRoot = true, bpfMapNotes = true, linkerConfigTool = "apex/com.android.runtime/bin/linkerconfig", mediaProfilesFallback = true, manifestFragmentsKept = listOf("health", "aemu-"), oneFingerprint = true, vendorHeapProps = true, ownVendorUnderGsi = true, odmManifestFragmentsAside = true, lazyAidlServices = true, ramBudget = true, gpuBridge64 = true, bootHardwareProp = true,
        firmwareOwn = listOf("vendor#bin#hw#android.hardware.media.omx@1.0-service", "vendor#etc#init#android.hardware.media.omx@1.0-service.rc",
            "vendor#lib#libavservices_minijail_vendor.so", "vendor#lib#libmediacodecservice.so", "vendor#lib#libminijail_vendor.so",
            // (the empty libhidltransport and libhwbinder of 10+, which the engine's HALs of 9 still name: a vendor side
            // has them, a generic system image only inside its VNDK 29 package, where the sphal namespace does not look)
            "vendor#lib64#libhidltransport.so", "vendor#lib64#libhwbinder.so", "vendor#lib#libhidltransport.so", "vendor#lib#libhwbinder.so"),
        firmwareAside = listOf("vendor#lib64#hw#android.hardware.keymaster@3.0-impl-qti.so", "vendor#lib64#hw#android.hardware.gatekeeper@1.0-impl-qti.so")),
    // Android 15: a copy of the kernel of 14 (its own from here on)
    A15("35", 35..35, volumeAtUserStart = true, derivedIdentity = true, oneIncludePolicy = true, webViewSingleProcess = true, registeredNetwork = true, treble = true, eglDir = "vendor/lib64/egl", gles = "2",
        absentFeatures = listOf("consumerir", "fingerprint", "nfc", "nfc.hce", "nfc.hcef", "bluetooth", "bluetooth_le"), trafficAccounting = true, noQemuPipe = true, binderVold = true,
        // (12 has no VNDK 27 any more: the 8.1 vendor side links against the oldest one there is)
        props = mapOf("ro.treble.enabled" to "false", "bpf.progs_loaded" to "1", "ro.product.first_api_level" to "27", "debug.stagefright.c2inputsurface" to "1",
            "ro.apex.updatable" to "false", "dalvik.vm.jdwp-provider" to "none", "ro.property_service.version" to "2",
            "persist.zygote.app_data_isolation" to "false",
            // (a vendor's firmware: as on 11)
            "pm.dexopt.first-boot" to "extract", "pm.dexopt.boot" to "extract", "ro.hardware.egl" to "swiftshader", "ro.frp.pst" to "",
            // SurfaceFlinger of 12 composes with Skia by default, which draws nothing over SwiftShader: the GLES
            // composer of the releases before is still in it
            "debug.renderengine.backend" to "gles",
            // (15 has the Skia one only; the drawings it makes at start to fill its shader cache crash SwiftShader)
            "service.sf.prime_shader_cache" to "0",
            // (a generic image asks init to copy its second slot's preopt files and waits 100 s for an answer)
            "ro.cp_system_other_odex" to "0",
            // (netd of 15 waits for init to report that the network BPF loader has finished)
            "init.svc.mdnsd_netbpfload" to "stopped",
            // the sound trigger HAL takes the engine's stand-in module, not the board's (which wants its DSP)
            "ro.hardware.sound_trigger.primary" to "aemu",
            // app compilation shares the phone's cores with the guest's UI, which SwiftShader draws on them too
            "dalvik.vm.dex2oat-threads" to "2", "dalvik.vm.background-dex2oat-threads" to "1", "dalvik.vm.boot-dex2oat-threads" to "2"),
        procFiles = mapOf("tp_lockdown_info" to "4636320131320200\n",
            // (system_server of 15 asks the kernel configuration whether stacks are vmapped and aborts without one)
            "config.gz" to "CONFIG_ARM64=y\nCONFIG_64BIT=y\nCONFIG_VMAP_STACK=y\n"),
        // (a generic system image of 14 carries the VNDK of 29..34 only, and its linkerconfig aborts on a version it
        // has no package of; the engine's HAL services find their own VNDK 28 copy by ownVndkDir)
        // (the software KeyMint inside keystore2 waits for the vendor's patch level property to exist, for ever over
        // a generic system image: no keystore service, system_server dies in LockSettingsService)
        propDefaults = mapOf("ro.vndk.version" to "34", "ro.vendor.build.security_patch" to "2023-12-05"),
        sysFiles = mapOf("android_lcd/lcd_name" to "aemu\n",
            // (netd of 15 also looks for the mark the network BPF loader leaves when it is done)
            "fs/bpf/netd_shared/mainline_done" to ""),
        ownVndkDir = "vendor/lib64/aemu-vndk", manifestFragmentsAside = true, trebleLinkerConfig = true, propAreaSize = 512 * 1024, cgroup2Root = true, partitionsAtRoot = true, bpfMapNotes = true, linkerConfigTool = "apex/com.android.runtime/bin/linkerconfig", mediaProfilesFallback = true, manifestFragmentsKept = listOf("health", "aemu-"), oneFingerprint = true, vendorHeapProps = true, ownVendorUnderGsi = true, odmManifestFragmentsAside = true, lazyAidlServices = true, ramBudget = true, gpuBridge64 = true, syncLog = true, cgroupRcDepth = true, apexStandIn = true, webView64 = true, ownLibDirs = mapOf("vendor#bin#hw#android.hardware.audio.service" to "vendor/lib64/aemu-audio"), bootHardwareProp = true,
        firmwareOwn = listOf("vendor#bin#hw#android.hardware.media.omx@1.0-service", "vendor#etc#init#android.hardware.media.omx@1.0-service.rc",
            "vendor#lib#libavservices_minijail_vendor.so", "vendor#lib#libmediacodecservice.so", "vendor#lib#libminijail_vendor.so",
            // (the empty libhidltransport and libhwbinder of 10+, which the engine's HALs of 9 still name: a vendor side
            // has them, a generic system image only inside its VNDK 29 package, where the sphal namespace does not look)
            "vendor#lib64#libhidltransport.so", "vendor#lib64#libhwbinder.so", "vendor#lib#libhidltransport.so", "vendor#lib#libhwbinder.so"),
        firmwareAside = listOf("vendor#lib64#hw#android.hardware.keymaster@3.0-impl-qti.so", "vendor#lib64#hw#android.hardware.gatekeeper@1.0-impl-qti.so")),
    // Android 16: a copy of the kernel of 15 (its own from here on)
    A16("36", 36..36, volumeAtUserStart = true, derivedIdentity = true, oneIncludePolicy = true, webViewSingleProcess = true, registeredNetwork = true, treble = true, eglDir = "vendor/lib64/egl", gles = "2",
        absentFeatures = listOf("consumerir", "fingerprint", "nfc", "nfc.hce", "nfc.hcef", "bluetooth", "bluetooth_le"), trafficAccounting = true, noQemuPipe = true, binderVold = true,
        // (12 has no VNDK 27 any more: the 8.1 vendor side links against the oldest one there is)
        props = mapOf("ro.treble.enabled" to "false", "bpf.progs_loaded" to "1", "ro.product.first_api_level" to "27", "debug.stagefright.c2inputsurface" to "1",
            "ro.apex.updatable" to "false", "dalvik.vm.jdwp-provider" to "none", "ro.property_service.version" to "2",
            "persist.zygote.app_data_isolation" to "false",
            // (a vendor's firmware: as on 11)
            "pm.dexopt.first-boot" to "extract", "pm.dexopt.boot" to "extract", "ro.hardware.egl" to "swiftshader", "ro.frp.pst" to "",
            // SurfaceFlinger of 12 composes with Skia by default, which draws nothing over SwiftShader: the GLES
            // composer of the releases before is still in it
            "debug.renderengine.backend" to "gles",
            // (15 has the Skia one only; the drawings it makes at start to fill its shader cache crash SwiftShader)
            "service.sf.prime_shader_cache" to "0",
            // (a generic image asks init to copy its second slot's preopt files and waits 100 s for an answer)
            "ro.cp_system_other_odex" to "0",
            // (netd of 15 waits for init to report that the network BPF loader has finished)
            "init.svc.mdnsd_netbpfload" to "stopped",
            // the sound trigger HAL takes the engine's stand-in module, not the board's (which wants its DSP)
            "ro.hardware.sound_trigger.primary" to "aemu",
            // app compilation shares the phone's cores with the guest's UI, which SwiftShader draws on them too
            "dalvik.vm.dex2oat-threads" to "2", "dalvik.vm.background-dex2oat-threads" to "1", "dalvik.vm.boot-dex2oat-threads" to "2"),
        procFiles = mapOf("tp_lockdown_info" to "4636320131320200\n",
            // (system_server of 15 asks the kernel configuration whether stacks are vmapped and aborts without one)
            "config.gz" to "CONFIG_ARM64=y\nCONFIG_64BIT=y\nCONFIG_VMAP_STACK=y\n"),
        // (a generic system image of 14 carries the VNDK of 29..34 only, and its linkerconfig aborts on a version it
        // has no package of; the engine's HAL services find their own VNDK 28 copy by ownVndkDir)
        // (the software KeyMint inside keystore2 waits for the vendor's patch level property to exist, for ever over
        // a generic system image: no keystore service, system_server dies in LockSettingsService)
        propDefaults = mapOf("ro.vndk.version" to "34", "ro.vendor.build.security_patch" to "2023-12-05"),
        sysFiles = mapOf("android_lcd/lcd_name" to "aemu\n",
            // (netd of 15 also looks for the mark the network BPF loader leaves when it is done)
            "fs/bpf/netd_shared/mainline_done" to ""),
        ownVndkDir = "vendor/lib64/aemu-vndk", manifestFragmentsAside = true, trebleLinkerConfig = true, propAreaSize = 512 * 1024, cgroup2Root = true, partitionsAtRoot = true, bpfMapNotes = true, linkerConfigTool = "apex/com.android.runtime/bin/linkerconfig", mediaProfilesFallback = true, manifestFragmentsKept = listOf("health", "aemu-"), oneFingerprint = true, vendorHeapProps = true, ownVendorUnderGsi = true, odmManifestFragmentsAside = true, lazyAidlServices = true, ramBudget = true, syncLog = true, cgroupRcDepth = true, standInServices = mapOf("android.hardware.power.IPower/default" to "system/lib64/libpowermanager.so#android.hardware.power-V"), gpuBridge64 = true, cgroupUidParents = listOf("system", "apps"), apexStandIn = true, webView64 = true, ownLibDirs = mapOf("vendor#bin#hw#android.hardware.audio.service" to "vendor/lib64/aemu-audio"), bootHardwareProp = true,
        firmwareOwn = listOf("vendor#bin#hw#android.hardware.media.omx@1.0-service", "vendor#etc#init#android.hardware.media.omx@1.0-service.rc",
            "vendor#lib#libavservices_minijail_vendor.so", "vendor#lib#libmediacodecservice.so", "vendor#lib#libminijail_vendor.so",
            // (the empty libhidltransport and libhwbinder of 10+, which the engine's HALs of 9 still name: a vendor side
            // has them, a generic system image only inside its VNDK 29 package, where the sphal namespace does not look)
            "vendor#lib64#libhidltransport.so", "vendor#lib64#libhwbinder.so", "vendor#lib#libhidltransport.so", "vendor#lib#libhwbinder.so"),
        firmwareAside = listOf("vendor#lib64#hw#android.hardware.keymaster@3.0-impl-qti.so", "vendor#lib64#hw#android.hardware.gatekeeper@1.0-impl-qti.so")),
    // Android 17: a copy of the kernel of 16 (its own from here on)
    A17("37", 37..37, volumeAtUserStart = true, derivedIdentity = true, oneIncludePolicy = true, webViewSingleProcess = true, registeredNetwork = true, treble = true, eglDir = "vendor/lib64/egl", gles = "2",
        absentFeatures = listOf("consumerir", "fingerprint", "nfc", "nfc.hce", "nfc.hcef", "bluetooth", "bluetooth_le"), trafficAccounting = true, noQemuPipe = true, binderVold = true,
        // (12 has no VNDK 27 any more: the 8.1 vendor side links against the oldest one there is)
        // (17: HintManagerService parses ro.vendor.api_level without a default; init derives it on a device, and the
        // engine's vendor side is the one of 8.1)
        props = mapOf("ro.treble.enabled" to "false", "bpf.progs_loaded" to "1", "ro.product.first_api_level" to "27", "debug.stagefright.c2inputsurface" to "1", "ro.vendor.api_level" to "27",
            "ro.apex.updatable" to "false", "dalvik.vm.jdwp-provider" to "none", "ro.property_service.version" to "2",
            "persist.zygote.app_data_isolation" to "false",
            // (a vendor's firmware: as on 11)
            "pm.dexopt.first-boot" to "extract", "pm.dexopt.boot" to "extract", "ro.hardware.egl" to "swiftshader", "ro.frp.pst" to "",
            // SurfaceFlinger of 12 composes with Skia by default, which draws nothing over SwiftShader: the GLES
            // composer of the releases before is still in it
            "debug.renderengine.backend" to "gles",
            // (15 has the Skia one only; the drawings it makes at start to fill its shader cache crash SwiftShader)
            "service.sf.prime_shader_cache" to "0",
            // (a generic image asks init to copy its second slot's preopt files and waits 100 s for an answer)
            "ro.cp_system_other_odex" to "0",
            // (netd of 15 waits for init to report that the network BPF loader has finished)
            "init.svc.mdnsd_netbpfload" to "stopped",
            // the sound trigger HAL takes the engine's stand-in module, not the board's (which wants its DSP)
            "ro.hardware.sound_trigger.primary" to "aemu",
            // app compilation shares the phone's cores with the guest's UI, which SwiftShader draws on them too
            "dalvik.vm.dex2oat-threads" to "2", "dalvik.vm.background-dex2oat-threads" to "1", "dalvik.vm.boot-dex2oat-threads" to "2"),
        procFiles = mapOf("tp_lockdown_info" to "4636320131320200\n",
            // (system_server of 15 asks the kernel configuration whether stacks are vmapped and aborts without one)
            "config.gz" to "CONFIG_ARM64=y\nCONFIG_64BIT=y\nCONFIG_VMAP_STACK=y\n"),
        // (a generic system image of 14 carries the VNDK of 29..34 only, and its linkerconfig aborts on a version it
        // has no package of; the engine's HAL services find their own VNDK 28 copy by ownVndkDir)
        // (the software KeyMint inside keystore2 waits for the vendor's patch level property to exist, for ever over
        // a generic system image: no keystore service, system_server dies in LockSettingsService)
        propDefaults = mapOf("ro.vndk.version" to "34", "ro.vendor.build.security_patch" to "2023-12-05"),
        sysFiles = mapOf("android_lcd/lcd_name" to "aemu\n",
            // (netd of 15 also looks for the mark the network BPF loader leaves when it is done)
            "fs/bpf/netd_shared/mainline_done" to ""),
        ownVndkDir = "vendor/lib64/aemu-vndk", manifestFragmentsAside = true, trebleLinkerConfig = true, propAreaSize = 512 * 1024, cgroup2Root = true, partitionsAtRoot = true, bpfMapNotes = true, linkerConfigTool = "apex/com.android.runtime/bin/linkerconfig", mediaProfilesFallback = true, manifestFragmentsKept = listOf("health", "aemu-"), oneFingerprint = true, vendorHeapProps = true, ownVendorUnderGsi = true, odmManifestFragmentsAside = true, lazyAidlServices = true, ramBudget = true, syncLog = true, cgroupRcDepth = true, standInServices = mapOf("android.hardware.power.IPower/default" to "system/lib64/libpowermanager.so#android.hardware.power-V"), aconfigStorage = true, kernelPolicyUnread = true, gpuBridge64 = true, cgroupUidParents = listOf("system", "apps"), apexStandIn = true, webView64 = true, ownLibDirs = mapOf("vendor#bin#hw#android.hardware.audio.service" to "vendor/lib64/aemu-audio"), bootHardwareProp = true,
        firmwareOwn = listOf("vendor#bin#hw#android.hardware.media.omx@1.0-service", "vendor#etc#init#android.hardware.media.omx@1.0-service.rc",
            "vendor#lib#libavservices_minijail_vendor.so", "vendor#lib#libmediacodecservice.so", "vendor#lib#libminijail_vendor.so",
            // (the empty libhidltransport and libhwbinder of 10+, which the engine's HALs of 9 still name: a vendor side
            // has them, a generic system image only inside its VNDK 29 package, where the sphal namespace does not look)
            "vendor#lib64#libhidltransport.so", "vendor#lib64#libhwbinder.so", "vendor#lib#libhidltransport.so", "vendor#lib#libhwbinder.so"),
        firmwareAside = listOf("vendor#lib64#hw#android.hardware.keymaster@3.0-impl-qti.so", "vendor#lib64#hw#android.hardware.gatekeeper@1.0-impl-qti.so"));

    val runner: String get() = "libaemu64_$tag.so"
    val binderd: String get() = "libbinderd64_$tag.so"
    /** the engine's libEGL implementation, under the image root */
    val eglLib: String get() = "$eglDir/libEGL_swiftshader.so"
    /** every guest file some kernel ships, under its asset name */
    fun isOwnSuffix(name: String) = name.endsWith("_swiftshader.so") || name.endsWith("_$eglName.so") || name == "libGLES_aemu.so" ||
        name == "libGLESv1_CM_ss.so"     // SwiftShader's GLES 1 behind its colour fix (native/gles1fix)
    /** guest files, later ones win: SwiftShader (a prebuilt nobody rebuilds), then this release's own. The HALs are
     *  a copy per kernel: a HAL rebuilt for one release reaches no other. */
    val assets: List<String> get() = listOf("engines/n64-gl", "engines/n64-$tag")
    /** Guest files that go over those where the firmware brought the vendor side of its own release (a vendor's
     *  firmware, not a generic system image): HALs built for that release's VNDK, which is the one such a firmware
     *  has. 9: the 8.1 audio and keymaster services do not link against VNDK 28. A kernel may have none. */
    val ownVendorAssets: String get() = "engines/n64-$tag-own"
    /** Guest files only a generic system image gets (it has no vendor side: no health HAL, which BatteryService of
     *  14 waits for). A kernel may have none. */
    val gsiAssets: String get() = "engines/n64-$tag-gsi"

    companion object {
        /** A release nobody has brought up yet runs on the nearest older kernel. */
        fun forApi(api: Int): Kernel64 = entries.firstOrNull { api in it.apis } ?: entries.lastOrNull { api > it.apis.last } ?: A6
    }
}
