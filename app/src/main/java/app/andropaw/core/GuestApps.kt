package app.andropaw.core

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import java.io.File

/**
 * What an app inside a guest looks like from the host: its label and its icon.
 *
 * The apks are plain files in the machine's tree, so `PackageManager` can read them without
 * the guest running — which is what the ROOT page needs: it lists apps that asked for root
 * while the machine may well be switched off. Both are best-effort: a package whose apk cannot
 * be found or parsed still gets a row, drawn from its package name.
 */
object GuestApps {
    data class Info(val label: String, val icon: Bitmap?)

    private val cache = HashMap<String, Info>()
    private var indexAt = 0L
    private var index: Map<String, File> = emptyMap()

    fun info(ctx: Context, paths: VmPaths, pkg: String): Info {
        cache[pkg]?.let { return it }
        val apk = apkIndex(paths)[pkg]
        val info = apk?.let { read(ctx, it, pkg) } ?: Info(fallbackLabel(pkg), null)
        synchronized(cache) { cache[pkg] = info }
        return info
    }

    fun label(ctx: Context, paths: VmPaths, pkg: String): String = info(ctx, paths, pkg).label

    fun clear() {
        synchronized(cache) { cache.clear() }
        indexAt = 0L
        index = emptyMap()
    }

    /** "com.example.player" → "Player", for a package whose apk is gone. */
    private fun fallbackLabel(pkg: String): String =
        pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }.ifBlank { pkg }

    private fun read(ctx: Context, apk: File, pkg: String): Info? = runCatching {
        val pm = ctx.packageManager
        val pi = pm.getPackageArchiveInfo(apk.absolutePath, 0) ?: return@runCatching null
        val ai = pi.applicationInfo ?: return@runCatching null
        // loadLabel / loadIcon read from these, and getPackageArchiveInfo leaves them unset
        ai.sourceDir = apk.absolutePath
        ai.publicSourceDir = apk.absolutePath
        val label = runCatching { pm.getApplicationLabel(ai).toString() }.getOrNull()
            ?.takeIf { it.isNotBlank() } ?: fallbackLabel(pkg)
        val icon = runCatching { ai.loadIcon(pm) }.getOrNull()?.let(::toBitmap)
        Info(label, icon)
    }.getOrNull()

    private fun toBitmap(d: Drawable): Bitmap? = runCatching {
        if (d is BitmapDrawable) return@runCatching d.bitmap
        val w = d.intrinsicWidth.takeIf { it > 0 } ?: 108
        val h = d.intrinsicHeight.takeIf { it > 0 } ?: 108
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        d.setBounds(0, 0, w, h)
        d.draw(c)
        bmp
    }.getOrNull()

    /** package name → the apk that carries it; rescanned at most once a minute. */
    private fun apkIndex(paths: VmPaths): Map<String, File> {
        val now = System.currentTimeMillis()
        if (now - indexAt < 60_000 && index.isNotEmpty()) return index
        val map = LinkedHashMap<String, File>()
        val dirs = listOf("data/app", "system/app", "system/priv-app", "product/app", "vendor/app")
        for (d in dirs) {
            val base = File(paths.root, d)
            if (!base.isDirectory) continue
            // 2.x–7.x keep "<pkg>-1.apk" flat, 8.0+ keep "<pkg>-<rand>/base.apk" one level down
            base.walkTopDown().maxDepth(3).forEach { f ->
                if (!f.isFile || !f.name.endsWith(".apk")) return@forEach
                val guess = when {
                    f.name == "base.apk" -> f.parentFile?.name?.substringBeforeLast('-')
                    else -> f.name.removeSuffix(".apk").substringBefore('-')
                } ?: return@forEach
                if (guess.contains('.')) map.putIfAbsent(guess, f)
            }
        }
        index = map
        indexAt = now
        return map
    }
}
