package app.andropaw.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class ReleaseInfo(
    val versionName: String,
    val tagName: String,
    val title: String,
    val body: String,
    val downloadUrl: String,
    val fileName: String,
    val sizeBytes: Long
)

sealed interface UpdateState {
    object Idle : UpdateState
    object Checking : UpdateState
    data class Available(val release: ReleaseInfo) : UpdateState
    data class UpToDate(val currentVersion: String, val latestTag: String) : UpdateState
    data class Downloading(val release: ReleaseInfo, val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : UpdateState
    data class ReadyToInstall(val release: ReleaseInfo, val file: File) : UpdateState
    data class Error(val message: String) : UpdateState
}

object AppUpdateManager {
    /** 更新源指向 AndroPaw 自己的仓库（TiG-Kira/AndroPaw），tag 规范 v0.0.0.x。 */
    const val GITHUB_REPO_URL = "https://github.com/TiG-Kira/AndroPaw"
    private const val RELEASES_API = "https://api.github.com/repos/TiG-Kira/AndroPaw/releases"

    /**
     * Запрос к GitHub API для получения информации о последнем релизе с APK-файлом.
     */
    suspend fun checkLatestRelease(context: android.content.Context? = null): Result<ReleaseInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(RELEASES_API)
            val conn = url.openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.setRequestProperty("Accept", "application/vnd.github+json")
                conn.setRequestProperty("User-Agent", "AndroPaw-App")

                if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("HTTP ${conn.responseCode}: ${conn.responseMessage}")
                }

                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(text)
                if (array.length() == 0) return@runCatching null

                for (i in 0 until array.length()) {
                    val rel = array.getJSONObject(i)
                    if (rel.optBoolean("draft", false) || rel.optBoolean("prerelease", false)) continue

                    val tagName = rel.optString("tag_name", "").trim()
                    val versionName = tagName.removePrefix("v").removePrefix("V")
                    val title = rel.optString("name", tagName)
                    val body = rel.optString("body", "")

                    val assets = rel.optJSONArray("assets") ?: continue
                    // 并存包（app.andropaw.0003）必须拿 0003 的包，标准包拿标准包——装错包名安装直接失败
                    val wantV0003 = context?.packageName?.endsWith("0003") == true
                    val wantAsset = if (wantV0003) "app.andropaw.0003.apk" else "app.andropaw.apk"
                    for (j in 0 until assets.length()) {
                        val asset = assets.getJSONObject(j)
                        val assetName = asset.optString("name", "")
                        if (assetName.equals(wantAsset, ignoreCase = true)) {
                            val downloadUrl = asset.getString("browser_download_url")
                            val sizeBytes = asset.optLong("size", 0L)
                            return@runCatching ReleaseInfo(
                                versionName = versionName,
                                tagName = tagName,
                                title = title,
                                body = body,
                                downloadUrl = downloadUrl,
                                fileName = assetName,
                                sizeBytes = sizeBytes
                            )
                        }
                    }
                }
                null
            } finally { conn.disconnect() }
        }
    }

    /**
     * Сравнение версий (семантическое сравнение сегментов чисел).
     * Возвращает true, если latestTag новее чем currentVersion.
     *
     * 逐段数值比较，段数不敏感：同时兼容旧的 0.0.0.x 四段 tag 和 0.1.0 起的
     * semver 三段 tag（约定：v0.1.0 发布后版本号固定为三段 semver）。
     */
    fun isNewerVersion(latestTag: String, currentVersion: String): Boolean {
        val cleanLatest = latestTag.trim().removePrefix("v").removePrefix("V")
        val cleanCurrent = currentVersion.trim().removePrefix("v").removePrefix("V")

        if (cleanLatest.equals(cleanCurrent, ignoreCase = true)) return false

        val latestParts = cleanLatest.split('.').map { segment ->
            segment.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
        }
        val currentParts = cleanCurrent.split('.').map { segment ->
            segment.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
        }

        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }

        return false
    }

    /**
     * Скачивание APK с поддержкой редиректов (GitHub перенаправляет на AWS S3) и отслеживанием прогресса.
     */
    suspend fun downloadApk(
        context: Context,
        release: ReleaseInfo,
        onProgress: (Float, Long, Long) -> Unit
    ): File = withContext(Dispatchers.IO) {
        // App-private storage prevents other apps from replacing a downloaded installer.
        val updatesDir = File(context.cacheDir, "updates")
        check(updatesDir.isDirectory || updatesDir.mkdirs()) { "Cannot create update directory" }
        if (release.sizeBytes > 0 && updatesDir.usableSpace - (16L shl 20) < release.sizeBytes) {
            throw IOException("Not enough storage for update: ${formatBytes(release.sizeBytes)} required")
        }
        val partial = File.createTempFile("andropaw-", ".part", updatesDir)
        val targetFile = File(updatesDir, partial.nameWithoutExtension + ".apk")
        var connection: HttpURLConnection? = null
        try {
            var url = URL(release.downloadUrl)
            var redirects = 0
            while (true) {
                require(url.protocol == "https") { "Update download requires HTTPS" }
                currentCoroutineContext().ensureActive()
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "AndroPaw-App")
                    setRequestProperty("Accept-Encoding", "identity")
                }
                connection = conn
                val code = conn.responseCode
                if (code in listOf(301, 302, 303, 307, 308)) {
                    val location = conn.getHeaderField("Location")
                        ?: throw IOException("Redirect without Location")
                    if (++redirects > 5) throw IOException("Too many redirects")
                    url = URL(url, location)
                    conn.disconnect()
                    continue
                }
                if (code != HttpURLConnection.HTTP_OK) throw IOException("HTTP $code: ${conn.responseMessage}")
                break
            }
            val conn = checkNotNull(connection)
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: release.sizeBytes
            var downloaded = 0L
            var lastProgress = 0L
            conn.inputStream.use { input ->
                FileOutputStream(partial).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        downloaded += count
                        if ((total > 0 && downloaded > total) ||
                            (release.sizeBytes > 0 && downloaded > release.sizeBytes)) {
                            throw IOException("Update size exceeds expected length")
                        }
                        output.write(buffer, 0, count)
                        val now = System.nanoTime()
                        if (now - lastProgress >= 100_000_000L) {
                            onProgress(if (total > 0) (downloaded.toFloat() / total).coerceIn(0f, 1f) else 0f, downloaded, total)
                            lastProgress = now
                        }
                    }
                    output.fd.sync()
                }
            }
            currentCoroutineContext().ensureActive()
            if (downloaded == 0L || (total > 0 && downloaded != total) ||
                (release.sizeBytes > 0 && downloaded != release.sizeBytes)) {
                throw IOException("Incomplete update: $downloaded / $total bytes")
            }
            val archive = context.packageManager.getPackageArchiveInfo(partial.path, 0)
                ?: throw IOException("Downloaded file is not a valid APK")
            if (archive.packageName != context.packageName) throw IOException("Update package does not match this app")
            if (!partial.renameTo(targetFile)) throw IOException("Cannot finalize update")
            updatesDir.listFiles()?.filter { it != targetFile && it.extension == "apk" }?.forEach { it.delete() }
            onProgress(1f, downloaded, total)
            targetFile
        } finally {
            connection?.disconnect()
            partial.delete()
        }
    }

    /**
     * Запуск установки загруженного APK через системный PackageInstaller.
     */
    fun installApk(context: Context, file: File): Boolean {
        if (!file.exists()) return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val permIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(permIntent)
                return false
            }
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
        return true
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val group = digitGroups.coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, group.toDouble())
        return String.format(Locale.US, "%.1f %s", value, units[group])
    }
}
