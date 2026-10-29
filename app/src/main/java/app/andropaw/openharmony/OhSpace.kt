package app.andropaw.openharmony

import org.json.JSONObject

/**
 * One OpenHarmony space: its own data directory, its own settings and boot counters —
 * the counterpart of core/GuestImage for the OpenHarmony runtime.
 */
data class OhSpace(
    val id: String,
    val name: String,
    /** runtime package version this space was created with */
    val runtimeVersion: String,
    /** OpenHarmony version the runtime reports (e.g. "7.0") */
    val ohVersion: String,
    val settings: OhSettings,
    val createdAt: Long,
    val lastBootMs: Long = 0,
    val bootCount: Int = 0,
    val sizeBytes: Long = 0,
) {
    val displayVersion: String get() = "OpenHarmony $ohVersion"

    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("name", name)
        .put("runtimeVersion", runtimeVersion).put("ohVersion", ohVersion)
        .put("settings", settings.toJson())
        .put("createdAt", createdAt).put("lastBootMs", lastBootMs)
        .put("bootCount", bootCount).put("sizeBytes", sizeBytes)

    companion object {
        fun fromJson(o: JSONObject): OhSpace = OhSpace(
            id = o.getString("id"),
            name = o.optString("name", "OpenHarmony"),
            runtimeVersion = o.optString("runtimeVersion", ""),
            ohVersion = o.optString("ohVersion", ""),
            settings = OhSettings.fromJson(o.optJSONObject("settings")),
            createdAt = o.optLong("createdAt", 0),
            lastBootMs = o.optLong("lastBootMs", 0),
            bootCount = o.optInt("bootCount", 0),
            sizeBytes = o.optLong("sizeBytes", 0),
        )
    }
}
