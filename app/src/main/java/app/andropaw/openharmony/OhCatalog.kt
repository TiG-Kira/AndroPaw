package app.andropaw.openharmony

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/**
 * Where the OpenHarmony runtime packages come from.
 *
 * A package carries the OpenHarmony system (rootfs, guest helper HAP and the native runtime
 * libraries — loader/runner/GPU/hardware bridges), packed as zip, tar, tar.gz or tar.xz.
 * Publish packages as GitHub release assets matching [RUNTIME_ASSET] and point [REPO] at the
 * repository holding them.
 */
object OhCatalog {
    /** Repository whose GitHub Releases carry the OpenHarmony runtime packages. */
    const val REPO = "TiG-Kira/AndroPaw"

    private val RELEASES_API get() = "https://api.github.com/repos/$REPO/releases"

    /** Asset names that count as a runtime package (as opposed to the app's own update apks). */
    private val RUNTIME_ASSET = Regex("(?i)oh[-_]?runtime.*\\.(zip|tar\\.xz|tgz)$")

    /**
     * Newest published runtime package, or null when the repository has none.
     * The tag provides the version; the OH version may be given in the release body as
     * "ohVersion: 7.0" (falls back to the tag).
     */
    suspend fun latest(): OhDownloader.Package? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL(RELEASES_API).openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "AndroPaw-OpenHarmony")
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
            val text = conn.inputStream.bufferedReader().use { it.readText() }

            val array = JSONArray(text)
            for (i in 0 until array.length()) {
                val rel = array.getJSONObject(i)
                if (rel.optBoolean("draft", false)) continue
                val tag = rel.optString("tag_name", "").trim()
                if (tag.isEmpty()) continue
                val assets = rel.optJSONArray("assets") ?: continue
                for (j in 0 until assets.length()) {
                    val a = assets.getJSONObject(j)
                    val name = a.optString("name", "")
                    if (!RUNTIME_ASSET.containsMatchIn(name)) continue
                    val ohVersion = Regex("(?im)^ohVersion:\\s*(.+)$")
                        .find(rel.optString("body", ""))?.groupValues?.get(1)?.trim()
                        ?: tag.removePrefix("v")
                    return@runCatching OhDownloader.Package(
                        version = tag.removePrefix("v"),
                        ohVersion = ohVersion,
                        url = a.getString("browser_download_url"),
                        fileName = name,
                        sizeBytes = a.optLong("size", 0),
                        sha256 = Regex("(?im)^sha256:\\s*([0-9a-fA-F]{64})$")
                            .find(rel.optString("body", ""))?.groupValues?.get(1)?.lowercase() ?: "",
                    )
                }
            }
            null
        }.getOrNull()
    }

    /** A package the user pointed at directly (custom URL, known checksum). */
    fun manual(url: String, version: String, ohVersion: String, sha256: String = "", sizeBytes: Long = 0): OhDownloader.Package {
        val fileName = url.substringAfterLast('/').ifEmpty { "oh-runtime.zip" }
        return OhDownloader.Package(version, ohVersion, url, fileName, sizeBytes, sha256)
    }
}
