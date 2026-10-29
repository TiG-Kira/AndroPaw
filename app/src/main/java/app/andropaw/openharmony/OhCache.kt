package app.andropaw.openharmony

import android.content.Context
import android.os.StatFs
import java.io.File

/**
 * The OpenHarmony download cache — and nothing else.
 *
 * Only downloaded artifacts live here (under getExternalCacheDir/openharmony, falling back to
 * cacheDir): package archives, resumable *.part bodies and their *.meta.json. The OS may
 * reclaim the directory under pressure and "clear cache" wipes it, which is exactly right for
 * re-downloadable data.
 *
 * Emulator storage deliberately does NOT live here: installed runtimes stay in
 * filesDir/openharmony/runtimes (OhRuntimeRepository) and per-space data in
 * filesDir/openharmony/spaces (OhSpaceStore) — persistent, like filesDir/images for the
 * Android machines. A cleared cache can never wipe a user's spaces.
 */
object OhCache {
    fun root(ctx: Context): File = File(ctx.externalCacheDir ?: ctx.cacheDir, "openharmony").apply { mkdirs() }
    fun downloads(ctx: Context): File = File(root(ctx), "downloads").apply { mkdirs() }

    /** Total bytes held by the OpenHarmony download cache. */
    fun usedBytes(ctx: Context): Long = du(root(ctx))

    /** Free space on the volume the cache lives on. */
    fun deviceFreeBytes(ctx: Context): Long = runCatching {
        val st = StatFs(root(ctx).path)
        st.availableBlocksLong * st.blockSizeLong
    }.getOrDefault(0L)

    /** Directory size, links excluded (same walk as core/ImageStore.du). */
    fun du(f: File): Long {
        var n = 0L
        val st = ArrayDeque<File>().apply { add(f) }
        while (st.isNotEmpty()) {
            val x = st.removeLast()
            val isLink = runCatching { android.system.OsConstants.S_ISLNK(android.system.Os.lstat(x.path).st_mode) }.getOrDefault(false)
            if (isLink) continue
            if (x.isDirectory) x.listFiles()?.forEach { st.add(it) } else n += x.length()
        }
        return n
    }

    /** Remove unfinished downloads: *.part bodies and orphan *.meta.json files. Returns files removed. */
    fun clearPartialDownloads(ctx: Context): Int {
        var n = 0
        downloads(ctx).listFiles()?.forEach { f ->
            if (f.name.endsWith(".part") || f.name.endsWith(".meta.json")) { if (f.delete()) n++ }
        }
        return n
    }

    /** Drop every downloaded archive — installed runtimes and spaces are untouched. Returns bytes freed. */
    fun clearDownloads(ctx: Context): Long {
        var freed = 0L
        downloads(ctx).listFiles()?.forEach { f ->
            val sz = if (f.isDirectory) du(f) else f.length()
            if (f.deleteRecursively()) freed += sz
        }
        return freed
    }

    /**
     * Automatic cleanup when device storage runs low: unfinished downloads first, then whole
     * archives. Never touches runtimes or spaces — those live in filesDir on purpose.
     */
    fun autoClean(ctx: Context): Long {
        var freed = 0L
        clearPartialDownloads(ctx)
        if (deviceFreeBytes(ctx) < LOW_WATER_BYTES) {
            freed += clearDownloads(ctx)
        }
        return freed
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val group = ((Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()).coerceIn(0, units.size - 1)
        return String.format(java.util.Locale.US, "%.1f %s", bytes / Math.pow(1024.0, group.toDouble()), units[group])
    }

    /** Auto-clean kicks in below this much free space on the cache volume. */
    const val LOW_WATER_BYTES: Long = 512L shl 20
}
