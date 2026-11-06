package app.andropaw.core

import app.andropaw.R

/**
 * AEmulator Pro — simplified feature registry.
 *
 * Upstream AEmulator Pro (github.com/jntm233-jntm/AEmulator-Pro) is a GPL-3.0 community fork of
 * uxazu/AEmulator, not a paid tier. Its "Pro" differences are app-level refinements: teal theme,
 * reworked collapsible settings, Android 16 live-update notifications, fast hand-off reboot,
 * per-machine PBKDF2 password, background firmware import, in-app licence/notice pages and a
 * per-machine setup-wizard skip.
 *
 * AndroPaw keeps the no-paywall spirit. This registry lists those features honestly:
 * [State.BUILT_IN] means the behaviour exists here, [State.PLANNED] means it needs real engine
 * or UI work and is deliberately not faked with a settings row. Add capabilities here; the
 * settings page renders whatever is registered.
 */
object ProFeatures {

    enum class State { BUILT_IN, PLANNED }

    /**
     * Child-process budget for Android 12+'s Phantom Process Killer. A lower threshold keeps the
     * guest further from the host's global 32-process ceiling, at the cost of swapping cached
     * apps out more often. Read once when a machine boots.
     */
    enum class GuardLevel(val key: String, val warn: Int, val safe: Int) {
        CONSERVATIVE("conservative", 24, 28),
        STANDARD("standard", 20, 24),
        AGGRESSIVE("aggressive", 14, 18),
        ;

        companion object {
            fun of(key: String?): GuardLevel = entries.firstOrNull { it.key == key } ?: STANDARD
        }
    }

    /** One capability shown on the Pro panel. [id] is stable; never reorder the registry. */
    data class Feature(val id: String, val titleRes: Int, val summaryRes: Int, val state: State)

    const val ID_FAST_REBOOT = "fast_reboot"
    const val ID_PROCESS_GUARD = "process_guard"
    const val ID_HOST_POLICY = "host_policy"
    const val ID_ZYGOTE_DIAG = "zygote_diag"
    const val ID_LICENCE_NOTICE = "licence_notice"
    const val ID_MACHINE_PASSWORD = "machine_password"
    const val ID_BACKGROUND_IMPORT = "background_import"
    const val ID_LIVE_UPDATES = "live_updates"
    const val ID_WIZARD_SKIP = "wizard_skip"
    const val ID_COLLAPSIBLE_SETTINGS = "collapsible_settings"

    /** The simplified Pro set: what this build has, and what still needs engine/UI work. */
    val ALL: List<Feature> = listOf(
        Feature(ID_FAST_REBOOT, R.string.pro_feature_reboot, R.string.pro_feature_reboot_sub, State.BUILT_IN),
        Feature(ID_PROCESS_GUARD, R.string.pro_feature_guard, R.string.pro_feature_guard_sub, State.BUILT_IN),
        Feature(ID_HOST_POLICY, R.string.pro_feature_host, R.string.pro_feature_host_sub, State.BUILT_IN),
        Feature(ID_ZYGOTE_DIAG, R.string.pro_feature_diag, R.string.pro_feature_diag_sub, State.BUILT_IN),
        Feature(ID_LICENCE_NOTICE, R.string.pro_feature_licence, R.string.pro_feature_licence_sub, State.BUILT_IN),
        Feature(ID_MACHINE_PASSWORD, R.string.pro_feature_password, R.string.pro_feature_password_sub, State.BUILT_IN),
        Feature(ID_BACKGROUND_IMPORT, R.string.pro_feature_import, R.string.pro_feature_import_sub, State.PLANNED),
        Feature(ID_LIVE_UPDATES, R.string.pro_feature_live, R.string.pro_feature_live_sub, State.PLANNED),
        Feature(ID_WIZARD_SKIP, R.string.pro_feature_wizard, R.string.pro_feature_wizard_sub, State.PLANNED),
        Feature(ID_COLLAPSIBLE_SETTINGS, R.string.pro_feature_collapse, R.string.pro_feature_collapse_sub, State.PLANNED),
    )
}
