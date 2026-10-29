package app.andropaw.openharmony

import org.json.JSONObject

/**
 * Per-space settings: display presets, orientation, frame rate, render mode, and which phone
 * capabilities the space may use.
 */
data class OhSettings(
    val width: Int = 720,
    val height: Int = 1280,
    val density: Int = 240,
    /** 0 portrait, 1 landscape */
    val orientation: Int = ORI_PORTRAIT,
    /** 30 / 60 / 90 / 120 */
    val fps: Int = 60,
    /** 0 smart (GPU first, fall back to CPU), 1 GPU only, 2 CPU software rendering */
    val renderMode: Int = RENDER_SMART,
    /** guest RAM budget in MB, 0 = automatic */
    val ramMb: Int = 0,
    val shareCamera: Boolean = false,
    val shareMic: Boolean = false,
    val shareLocation: Boolean = false,
    val shareSensors: Boolean = false,
    val shareClipboard: Boolean = true,
    val shareAudio: Boolean = true,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("width", width).put("height", height).put("density", density)
        .put("orientation", orientation).put("fps", fps).put("renderMode", renderMode)
        .put("ramMb", ramMb)
        .put("shareCamera", shareCamera).put("shareMic", shareMic).put("shareLocation", shareLocation)
        .put("shareSensors", shareSensors).put("shareClipboard", shareClipboard).put("shareAudio", shareAudio)

    val landscape: Boolean get() = orientation == ORI_LANDSCAPE
    val displaySize: String get() = "${width}×${height}"

    companion object {
        const val ORI_PORTRAIT = 0
        const val ORI_LANDSCAPE = 1
        const val RENDER_SMART = 0
        const val RENDER_GPU = 1
        const val RENDER_CPU = 2
        val FPS_CHOICES = listOf(30, 60, 90, 120)

        data class Preset(val label: String, val width: Int, val height: Int, val density: Int)

        /** Display presets; the density class follows the chosen resolution. */
        val PRESETS = listOf(
            Preset("480p", 480, 854, 160),
            Preset("540p", 540, 960, 200),
            Preset("720p", 720, 1280, 240),
            Preset("1080p", 1080, 1920, 320),
        )

        fun fromJson(o: JSONObject?): OhSettings {
            if (o == null) return OhSettings()
            val d = OhSettings()
            return OhSettings(
                width = o.optInt("width", d.width),
                height = o.optInt("height", d.height),
                density = o.optInt("density", d.density),
                orientation = o.optInt("orientation", d.orientation),
                fps = o.optInt("fps", d.fps),
                renderMode = o.optInt("renderMode", d.renderMode),
                ramMb = o.optInt("ramMb", d.ramMb),
                shareCamera = o.optBoolean("shareCamera", d.shareCamera),
                shareMic = o.optBoolean("shareMic", d.shareMic),
                shareLocation = o.optBoolean("shareLocation", d.shareLocation),
                shareSensors = o.optBoolean("shareSensors", d.shareSensors),
                shareClipboard = o.optBoolean("shareClipboard", d.shareClipboard),
                shareAudio = o.optBoolean("shareAudio", d.shareAudio),
            )
        }
    }
}
