package app.andropaw

import android.content.Context
import android.content.res.Configuration
import app.andropaw.core.VmSettings
import java.util.Locale

/** Настройки самого приложения (не отдельной системы): язык, тема, умолчания для новых систем. */
object AppPrefs {
    private const val FILE = "app"

    /** Языки интерфейса: код BCP-47 → самоназвание. Пустой код — как в системе. */
    val LANGUAGES = listOf(
        "" to "",
        "en" to "English",
        "ru" to "Русский",
        "uk" to "Українська",
        "de" to "Deutsch",
        "fr" to "Français",
        "es" to "Español",
        "pt-BR" to "Português (Brasil)",
        "it" to "Italiano",
        "pl" to "Polski",
        "tr" to "Türkçe",
        "ar" to "العربية",
        "fa" to "فارسی",
        "hi" to "हिन्दी",
        "id" to "Bahasa Indonesia",
        "vi" to "Tiếng Việt",
        "zh-CN" to "简体中文",
        "ja" to "日本語",
        "ko" to "한국어",
    )

    const val THEME_SYSTEM = 0
    const val THEME_LIGHT = 1
    const val THEME_DARK = 2

    private fun sp(ctx: Context) = ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun prefs(ctx: Context) = sp(ctx)

    fun language(ctx: Context): String = sp(ctx).getString("lang", "") ?: ""
    fun setLanguage(ctx: Context, tag: String) = sp(ctx).edit().putString("lang", tag).apply()

    fun theme(ctx: Context): Int = sp(ctx).getInt("theme", THEME_SYSTEM)
    fun setTheme(ctx: Context, v: Int) = sp(ctx).edit().putInt("theme", v).apply()

    fun dynamicColor(ctx: Context): Boolean = sp(ctx).getBoolean("dynamic", false)
    fun setDynamicColor(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("dynamic", v).apply()

    fun autoCheckUpdates(ctx: Context): Boolean = sp(ctx).getBoolean("auto_check_updates", true)
    fun setAutoCheckUpdates(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("auto_check_updates", v).apply()

    /** The machine the plug-in page and the ROOT page were last pointed at. */
    fun pluginsTarget(ctx: Context): String = sp(ctx).getString("plugins_target", "") ?: ""
    fun setPluginsTarget(ctx: Context, id: String) = sp(ctx).edit().putString("plugins_target", id).apply()

    fun rootTarget(ctx: Context): String = sp(ctx).getString("root_target", "") ?: ""
    fun setRootTarget(ctx: Context, id: String) = sp(ctx).edit().putString("root_target", id).apply()

    fun lastUpdateCheck(ctx: Context): Long = sp(ctx).getLong("last_update_check", 0L)
    fun setLastUpdateCheck(ctx: Context, v: Long) = sp(ctx).edit().putLong("last_update_check", v).apply()

    /** AEmulator Pro: child-process budget preset, stored as a ProFeatures.GuardLevel key. */
    fun proGuardLevel(ctx: Context): String = sp(ctx).getString("pro_guard_level", "standard") ?: "standard"
    fun setProGuardLevel(ctx: Context, key: String) = sp(ctx).edit().putString("pro_guard_level", key).apply()

    /** When the host kills the guest tree, do one clean machine restart instead of a rebuild loop. */
    fun hostKillAutoRestart(ctx: Context): Boolean = sp(ctx).getBoolean("host_kill_auto_restart", true)
    fun setHostKillAutoRestart(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("host_kill_auto_restart", v).apply()

    /**
     * The five Java stand-ins (Bt/Net/Apex/StandIn/Vold) are separate app_process host processes.
     * They never actually loaded: GuestVm asked for app.andropaw.stub.* while aemu-stubs.jar only
     * contains app.aemu.stub.*, so every one of them aborted with 134 at boot. Default off keeps
     * behaviour identical to that (no regression) without wasting five process slots; turning it
     * on now uses the correct class names and restores the stand-ins for firmwares that need them.
     */
    fun guestStubsEnabled(ctx: Context): Boolean = sp(ctx).getBoolean("guest_java_stubs", false)
    fun setGuestStubsEnabled(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("guest_java_stubs", v).apply()

    /**
     * Slim boot: diagnostic/statistical services are marked on-demand instead of being forked at
     * boot. An Android 17 AOSP image starts ~41 host processes while the host caps all apps'
     * children at 32, so the guest is culled before any memory-based guard can help. These
     * services come back through ctl.start when the framework actually asks for them.
     */
    fun slimBoot(ctx: Context): Boolean = sp(ctx).getBoolean("slim_boot", true)
    fun setSlimBoot(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("slim_boot", v).apply()

    /**
     * Boot-time host kills are *retries*, not a fatal condition. Six independent attempts with a
     * leaner service plan each time: even a 30% per-attempt success rate compounds to ~88%.
     * The counter must live in preferences: GuestVm.stop() wipes every in-memory debounce flag
     * and a hand-off reboot replaces the whole :vm process.
     *
     * @return the attempt number (1..[MAX_BOOT_RETRIES]), or 0 when the budget is exhausted.
     */
    fun allowHostKillRestart(ctx: Context): Int {
        val p = sp(ctx)
        val now = System.currentTimeMillis()
        val start = p.getLong("host_kill_window", 0L)
        val count = p.getInt("host_kill_count", 0)
        if (start == 0L || now - start > 15 * 60_000L) {
            p.edit().putLong("host_kill_window", now).putInt("host_kill_count", 1).apply()
            return 1
        }
        if (count >= MAX_BOOT_RETRIES) return 0
        p.edit().putInt("host_kill_count", count + 1).apply()
        return count + 1
    }

    const val MAX_BOOT_RETRIES = 6

    /** Back off with jitter so retries do not re-collide with the host's periodic culling sweep. */
    fun bootRetryDelayMs(attempt: Int): Long = when (attempt) {
        1 -> 800L; 2 -> 1_600L; 3 -> 2_600L
        4 -> 4_000L; 5 -> 6_000L; else -> 8_000L
    } + (System.currentTimeMillis() % 500L)

    /** One-shot delay consumed by VmActivity.restartProcess(); manual reboots keep the 400 ms hand-off. */
    fun setBootDelayMs(ctx: Context, ms: Long) = sp(ctx).edit().putLong("next_boot_delay", ms).apply()
    fun takeBootDelayMs(ctx: Context): Long {
        val p = sp(ctx)
        val v = p.getLong("next_boot_delay", 400L)
        p.edit().remove("next_boot_delay").apply()
        return v
    }

    /**
     * Pause the host's phantom-process killer for as long as a machine runs: the cap is lifted
     * through Shizuku before boot and restored when the last machine stops (core/PhantomKiller.kt).
     * Off by default — it changes a host-wide setting, and it needs Shizuku to be running.
     */
    fun phantomPause(ctx: Context): Boolean = sp(ctx).getBoolean("host_phantom_pause", false)
    fun setPhantomPause(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("host_phantom_pause", v).apply()

    /** 0 = slim boot, 1 = leaner, 2 = minimum viable. Escalates on each failed attempt. */
    fun bootSlimLevel(ctx: Context): Int = sp(ctx).getInt("boot_slim_level", 0).coerceIn(0, 2)
    fun bumpBootSlimLevel(ctx: Context): Int {
        val next = (bootSlimLevel(ctx) + 1).coerceAtMost(2)
        sp(ctx).edit().putInt("boot_slim_level", next).apply()
        return next
    }

    /** A boot that actually completed resets the window and steps the lean level back down, so the machine converges on the lightest plan that works. */
    fun resetHostKillRestarts(ctx: Context) {
        val p = sp(ctx)
        p.edit().remove("host_kill_window").remove("host_kill_count")
            .putInt("boot_slim_level", (p.getInt("boot_slim_level", 0) - 1).coerceAtLeast(0)).apply()
    }

    /** Умолчания, которые получает только что импортированная система. */
    fun defaults(ctx: Context): VmSettings {
        val p = sp(ctx)
        val d = VmSettings()
        return d.copy(
            gpu = p.getBoolean("def_gpu", d.gpu),
            jit = p.getBoolean("def_jit", d.jit),
            netProxy = p.getBoolean("def_proxy", d.netProxy),
            showNavBar = p.getBoolean("def_nav", d.showNavBar),
            keepScreenOn = p.getBoolean("def_awake", d.keepScreenOn),
            showLed = p.getBoolean("def_led", d.showLed),
        )
    }

    fun setDefaults(ctx: Context, s: VmSettings) = sp(ctx).edit()
        .putBoolean("def_gpu", s.gpu).putBoolean("def_jit", s.jit).putBoolean("def_proxy", s.netProxy)
        .putBoolean("def_nav", s.showNavBar).putBoolean("def_awake", s.keepScreenOn).putBoolean("def_led", s.showLed).apply()

    /** Контекст с выбранным языком — для attachBaseContext каждой активности. */
    /**
     * Language and theme of the app applied to an activity's resources. The theme goes into the night bits of
     * uiMode: values-night (window background), isSystemInDarkTheme and enableEdgeToEdge's status bar icons then
     * follow the app's choice — before, a dark app theme on a light system kept a white window background and dark,
     * invisible status bar icons.
     */
    fun wrap(base: Context): Context {
        val tag = language(base)
        val night = when (theme(base)) {
            THEME_LIGHT -> Configuration.UI_MODE_NIGHT_NO
            THEME_DARK -> Configuration.UI_MODE_NIGHT_YES
            else -> -1
        }
        if (tag.isEmpty() && night < 0) return base
        val cfg = Configuration(base.resources.configuration)
        if (tag.isNotEmpty()) {
            val loc = Locale.forLanguageTag(tag)
            Locale.setDefault(loc)
            cfg.setLocale(loc)
            cfg.setLayoutDirection(loc)
        }
        if (night >= 0) cfg.uiMode = (cfg.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
        return base.createConfigurationContext(cfg)
    }
}
