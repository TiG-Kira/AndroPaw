package app.aemu.core

import android.content.Context
import java.io.InputStream

/**
 * The engines' guest files (assets/engines/...) as the APK carries them: every kernel has its own copy of most of
 * them, 473 MB stored uncompressed, 110 MB of it different. The build (app/build.gradle.kts, packEngines) keeps each
 * content once, compressed, under assets/blobs/<sha1>, and writes assets/engines.idx: "path<TAB>sha1<TAB>size" per
 * file. Paths here are the old ones ("engines/kk/libGLES.so").
 */
object EngineAssets {
    private class Entry(val blob: String, val size: Long)

    @Volatile private var index: Map<String, Entry>? = null
    @Volatile private var dirs: Map<String, List<String>>? = null

    private fun load(ctx: Context): Map<String, Entry> {
        index?.let { return it }
        synchronized(this) {
            index?.let { return it }
            val m = HashMap<String, Entry>(4096)
            val d = HashMap<String, MutableList<String>>()
            ctx.assets.open("engines.idx").bufferedReader().useLines { lines ->
                for (l in lines) {
                    val p = l.split('\t')
                    if (p.size < 3) continue
                    m[p[0]] = Entry(p[1], p[2].toLong())
                    d.getOrPut(p[0].substringBeforeLast('/')) { ArrayList() }.add(p[0].substringAfterLast('/'))
                }
            }
            dirs = d
            index = m
            return m
        }
    }

    /** names in a directory, like AssetManager.list (empty when there is none) */
    fun list(ctx: Context, dir: String): Array<String> {
        load(ctx)
        return dirs?.get(dir.trimEnd('/'))?.toTypedArray() ?: emptyArray()
    }

    fun exists(ctx: Context, path: String): Boolean = load(ctx).containsKey(path)

    /** size in bytes, -1 when there is no such file */
    fun size(ctx: Context, path: String): Long = load(ctx)[path]?.size ?: -1L

    fun open(ctx: Context, path: String): InputStream {
        val e = load(ctx)[path] ?: throw java.io.FileNotFoundException(path)
        return ctx.assets.open("blobs/" + e.blob)
    }
}
