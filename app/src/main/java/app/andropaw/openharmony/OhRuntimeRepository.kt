package app.andropaw.openharmony

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.json.JSONObject
import org.tukaani.xz.XZInputStream
import java.io.File
import java.io.InputStream
import java.util.zip.GZIPInputStream
import java.util.zip.ZipFile

/**
 * The "download → install" one-stop for the OpenHarmony runtime:
 *
 *   OhCatalog (find the latest package)
 *     → OhDownloader (fetch into the download cache, resume, SHA-256)
 *     → extract under filesDir/openharmony/runtimes/<version>/  (zip-slip guarded)
 *     → register runtime.json
 *     → discard the archive (the cache never keeps what's installed)
 *
 * Package formats are plain zip, tar, tar.gz or tar.xz. The xz/tar machinery is the same
 * org.tukaani:xz + commons-compress the firmware importer already uses — no new dependencies.
 *
 * Storage split (deliberate): the download cache (OhCache) only ever holds re-downloadable
 * archives; the installed runtime — the actual "emulator storage" — lives in
 * filesDir/openharmony/runtimes next to the spaces, persistent and safe from cache clears,
 * exactly like filesDir/images for the Android machines.
 */
object OhRuntimeRepository {
    data class OhRuntime(
        val version: String,
        val ohVersion: String,
        val sizeBytes: Long,
        val installedAt: Long,
    )

    private const val RUNTIME_JSON = "runtime.json"

    private fun runtimesDir(ctx: Context): File =
        File(File(ctx.filesDir, "openharmony"), "runtimes").apply { mkdirs() }

    fun dir(ctx: Context, version: String): File = File(runtimesDir(ctx), version)
    fun isInstalled(ctx: Context, version: String): Boolean = File(dir(ctx, version), RUNTIME_JSON).isFile

    fun get(ctx: Context, version: String): OhRuntime? {
        val f = File(dir(ctx, version), RUNTIME_JSON)
        if (!f.isFile) return null
        return runCatching { fromJson(JSONObject(f.readText())) }.getOrNull()
    }

    fun installed(ctx: Context): List<OhRuntime> =
        runtimesDir(ctx).listFiles()?.mapNotNull { d ->
            get(ctx, d.name)?.takeIf { it.version == d.name }
        }?.sortedByDescending { it.installedAt } ?: emptyList()

    /**
     * Make sure [pkg] is usable: download if needed, verify, extract, register. Returns the
     * runtime directory (contains the OpenHarmony rootfs plus the native runner).
     */
    suspend fun ensureInstalled(
        ctx: Context,
        pkg: OhDownloader.Package,
        onState: (OhDownloader.State) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val d = dir(ctx, pkg.version)
        if (isInstalled(ctx, pkg.version)) {
            onState(OhDownloader.State.Done(d))
            return@withContext d
        }
        val archive = OhDownloader.download(ctx, pkg, onState)
        try {
            install(ctx, pkg, archive)
        } finally {
            // the unpacked runtime now exists: the archive is pure ballast, drop it either way
            OhDownloader.discardArchive(ctx, pkg)
        }
        onState(OhDownloader.State.Done(d))
        d
    }

    /** Extract a runtime package (zip / tar / tar.gz / tar.xz) and register it. */
    fun install(ctx: Context, pkg: OhDownloader.Package, archive: File): OhRuntime {
        val final = dir(ctx, pkg.version)
        val tmp = File(final.parentFile, final.name + ".installing")
        runCatching { tmp.deleteRecursively() }
        tmp.mkdirs()
        try {
            extract(archive, tmp)
            val info = OhRuntime(
                version = pkg.version,
                ohVersion = pkg.ohVersion,
                sizeBytes = OhCache.du(tmp),
                installedAt = System.currentTimeMillis(),
            )
            File(tmp, RUNTIME_JSON).writeText(info.toJson().toString(2))
            if (final.exists()) final.deleteRecursively()
            if (!tmp.renameTo(final)) {
                tmp.copyRecursively(final, overwrite = true)
                tmp.deleteRecursively()
            }
            return info
        } catch (t: Throwable) {
            tmp.deleteRecursively()
            throw t
        }
    }

    fun uninstall(ctx: Context, version: String): Boolean = dir(ctx, version).deleteRecursively()

    /** Uninstall every installed runtime (spaces keep their data, they just can't boot). */
    fun uninstallAll(ctx: Context): Long {
        var freed = 0L
        installed(ctx).forEach { rt ->
            val d = dir(ctx, rt.version)
            val sz = OhCache.du(d)
            if (d.deleteRecursively()) freed += sz
        }
        return freed
    }

    // ------------------------------------------------------------------ extraction

    private fun extract(archive: File, to: File) {
        val head = ByteArray(8)
        archive.inputStream().use { it.read(head) }
        when {
            head[0] == 'P'.code.toByte() && head[1] == 'K'.code.toByte() -> extractZip(archive, to)
            head[0] == 0xFD.toByte() && head[1] == '7'.code.toByte() -> extractTarStream(XZInputStream(archive.inputStream()), to)
            head[0] == 0x1F.toByte() && head[1] == 0x8B.toByte() -> extractTarStream(GZIPInputStream(archive.inputStream()), to)
            isTar(head) -> extractTarStream(archive.inputStream(), to)
            else -> error("unknown runtime package format: ${archive.name}")
        }
    }

    private fun isTar(h: ByteArray) = h.size > 262 && String(h, 257, 5, Charsets.ISO_8859_1) == "ustar"

    private fun extractZip(archive: File, to: File) {
        ZipFile(archive).use { z ->
            val entries = z.entries()
            while (entries.hasMoreElements()) {
                val e = entries.nextElement()
                if (e.isDirectory) continue
                writeFile(sink(e.name, to)) { out -> z.getInputStream(e).use { it.copyTo(out, 1 shl 16) } }
                runCatching { sink(e.name, to).setExecutable(isExecName(e.name), false) }
            }
        }
    }

    private fun extractTarStream(raw: InputStream, to: File) {
        TarArchiveInputStream(raw, 1 shl 16).use { t ->
            while (true) {
                val e = t.nextTarEntry ?: break
                if (e.isDirectory) continue
                val mode = e.mode
                writeFile(sink(e.name, to)) { out -> t.copyTo(out, 1 shl 16) }
                // keep the guest's exec bits: the runner and the loader must stay executable
                runCatching { sink(e.name, to).setExecutable(mode and 0b001_000_000 != 0, false) }
            }
        }
    }

    /** zip-slip guard: every entry must land inside the runtime dir. */
    private fun sink(name: String, to: File): File {
        val target = File(to, name)
        if (!target.canonicalPath.startsWith(to.canonicalPath + File.separator))
            throw SecurityException("bad entry in runtime package: $name")
        target.parentFile?.mkdirs()
        return target
    }

    private inline fun writeFile(target: File, block: (java.io.OutputStream) -> Unit) {
        target.outputStream().use { block(it) }
    }

    private fun isExecName(name: String) = name.startsWith("lib") || name.contains("/bin/")

    private fun fromJson(o: JSONObject): OhRuntime = OhRuntime(
        version = o.optString("version", ""),
        ohVersion = o.optString("ohVersion", ""),
        sizeBytes = o.optLong("sizeBytes", 0),
        installedAt = o.optLong("installedAt", 0),
    )

    private fun OhRuntime.toJson(): JSONObject = JSONObject()
        .put("version", version).put("ohVersion", ohVersion)
        .put("sizeBytes", sizeBytes).put("installedAt", installedAt)
}
