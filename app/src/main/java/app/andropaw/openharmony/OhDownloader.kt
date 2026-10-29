package app.andropaw.openharmony

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Downloads a runtime package into the app cache with HTTP resume, progress callbacks and
 * SHA-256 verification. The archive lands in OhCache.downloads(); the repository deletes it
 * after extraction so the cache never holds both the archive and the unpacked runtime.
 */
object OhDownloader {
    /** A published OpenHarmony runtime package (one GitHub release asset, or any direct URL). */
    data class Package(
        val version: String,
        val ohVersion: String,
        val url: String,
        val fileName: String,
        val sizeBytes: Long,
        val sha256: String = "",
    ) {
        fun toJson(): JSONObject = JSONObject()
            .put("version", version).put("ohVersion", ohVersion).put("url", url)
            .put("fileName", fileName).put("sizeBytes", sizeBytes).put("sha256", sha256)

        companion object {
            fun fromJson(o: JSONObject): Package = Package(
                version = o.optString("version", ""),
                ohVersion = o.optString("ohVersion", ""),
                url = o.getString("url"),
                fileName = o.optString("fileName", "oh-runtime.zip"),
                sizeBytes = o.optLong("sizeBytes", 0),
                sha256 = o.optString("sha256", ""),
            )
        }
    }

    sealed interface State {
        data object Idle : State
        data class Downloading(val progress: Float, val downloaded: Long, val total: Long) : State
        data object Verifying : State
        data class Done(val file: File) : State
        data class Failed(val message: String) : State
    }

    fun archiveFile(ctx: Context, pkg: Package): File = File(OhCache.downloads(ctx), pkg.fileName)
    private fun partFile(ctx: Context, pkg: Package): File = File(OhCache.downloads(ctx), pkg.fileName + ".part")
    private fun metaFile(ctx: Context, pkg: Package): File = File(OhCache.downloads(ctx), pkg.fileName + ".meta.json")

    /**
     * Download [pkg] into the cache. Resumes a previous *.part when the server honours ranges,
     * verifies size and SHA-256, then renames the part to the final archive name.
     */
    suspend fun download(ctx: Context, pkg: Package, onState: (State) -> Unit): File = withContext(Dispatchers.IO) {
        val part = partFile(ctx, pkg)
        val archive = archiveFile(ctx, pkg)
        try {
            // stale leftovers of other packages (same rule AppUpdateManager applies to update apks)
            OhCache.downloads(ctx).listFiles()?.forEach { f ->
                if (f != part && (f.name.endsWith(".part") || f.name.endsWith(".meta.json"))) f.delete()
            }

            if (archive.isFile && pkg.sizeBytes in 1..archive.length()) {
                onState(State.Verifying)
                if (sha256(archive) == pkg.sha256 || pkg.sha256.isBlank()) {
                    onState(State.Done(archive)); return@withContext archive
                }
                archive.delete()
            }

            metaFile(ctx, pkg).writeText(pkg.toJson().toString(2))

            var start = if (part.isFile) part.length() else 0L
            var redirects = 0
            var url = URL(pkg.url)
            val digest = MessageDigest.getInstance("SHA-256")
            val buf = ByteArray(64 * 1024)

            download@ while (true) {
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 15_000
                conn.readTimeout = 30_000
                conn.instanceFollowRedirects = false
                conn.setRequestProperty("User-Agent", "AndroPaw-OpenHarmony")
                if (start > 0) conn.setRequestProperty("Range", "bytes=$start-")
                val code = conn.responseCode
                when {
                    code in intArrayOf(HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_MOVED_TEMP, 307, 308) -> {
                        val loc = conn.getHeaderField("Location") ?: throw IllegalStateException("redirect without Location")
                        conn.disconnect()
                        if (++redirects > 5) throw IllegalStateException("too many redirects")
                        url = URL(URL(pkg.url), loc); continue@download
                    }
                    code == HttpURLConnection.HTTP_PARTIAL -> { /* resuming */ }
                    code == HttpURLConnection.HTTP_OK -> { start = 0L; part.delete() }
                    else -> throw IllegalStateException("HTTP $code: ${conn.responseMessage}")
                }

                val total = if (code == HttpURLConnection.HTTP_PARTIAL) {
                    val cr = conn.getHeaderField("Content-Range")?.substringAfter('/')?.toLongOrNull() ?: pkg.sizeBytes
                    start + (conn.contentLengthLong.takeIf { it > 0 } ?: 0L).coerceAtMost(cr - start)
                } else conn.contentLengthLong.takeIf { it > 0 } ?: pkg.sizeBytes

                conn.inputStream.use { input ->
                    FileOutputStream(part, start > 0).use { out ->
                        var downloaded = start
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            downloaded += n
                            if (total > 0) onState(State.Downloading((downloaded.toFloat() / total).coerceIn(0f, 1f), downloaded, total))
                        }
                        out.flush()
                    }
                }
                conn.disconnect()
                break
            }

            onState(State.Verifying)
            if (pkg.sizeBytes > 0 && part.length() != pkg.sizeBytes)
                throw IllegalStateException("size mismatch: got ${part.length()}, want ${pkg.sizeBytes}")
            if (pkg.sha256.isNotBlank() && sha256(part) != pkg.sha256)
                throw IllegalStateException("SHA-256 mismatch")
            if (!part.renameTo(archive)) {
                part.copyTo(archive, overwrite = true); part.delete()
            }
            metaFile(ctx, pkg).delete()
            onState(State.Done(archive))
            archive
        } catch (t: Throwable) {
            // keep the *.part for resume; drop the meta only on hard failures? keep it too — it holds the url
            onState(State.Failed(t.message ?: t.toString()))
            throw t
        }
    }

    /** Streaming SHA-256 of a file. */
    fun sha256(f: File): String {
        val d = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) { val n = input.read(buf); if (n < 0) break; d.update(buf, 0, n) }
        }
        return d.digest().joinToString("") { "%02x".format(it) }
    }

    /** Drop the archive after successful extraction (the runtime now lives unpacked). */
    fun discardArchive(ctx: Context, pkg: Package) {
        archiveFile(ctx, pkg).delete()
        partFile(ctx, pkg).delete()
        metaFile(ctx, pkg).delete()
    }
}
