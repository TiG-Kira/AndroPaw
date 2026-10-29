package app.andropaw.openharmony

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * Registry of OpenHarmony spaces. Mirrors core/ImageStore so the surrounding UI code reads the
 * same, with one deliberate difference: space data lives in filesDir (persistent — a cleared
 * cache must never wipe a user's accounts and apps), while downloads and runtimes stay in the
 * cache under OhCache (re-downloadable, actively cleaned).
 *
 * Layout of one space:
 *   <spacesDir>/<id>/space.json   metadata (OhSpace)
 *   <spacesDir>/<id>/data/        per-space writable data (overlay onto the shared runtime rootfs)
 *   <spacesDir>/<id>/run/         host sockets, logs (same shape as VmPaths.bin)
 */
object OhSpaceStore {
    private fun spacesDir(ctx: Context): File = File(ctx.filesDir, "openharmony/spaces").apply { mkdirs() }

    private fun spaceDir(ctx: Context, id: String): File = File(spacesDir(ctx), id)
    private fun meta(ctx: Context, id: String): File = File(spaceDir(ctx, id), "space.json")
    fun dataDir(ctx: Context, id: String): File = File(spaceDir(ctx, id), "data").apply { mkdirs() }
    fun runDir(ctx: Context, id: String): File = File(spaceDir(ctx, id), "run").apply { mkdirs() }

    fun list(ctx: Context): List<OhSpace> =
        spacesDir(ctx).listFiles()?.mapNotNull { d ->
            val f = File(d, "space.json")
            if (!f.isFile) null else runCatching { OhSpace.fromJson(JSONObject(f.readText())) }.getOrNull()
        }?.sortedByDescending { it.createdAt } ?: emptyList()

    fun get(ctx: Context, id: String): OhSpace? {
        val f = meta(ctx, id)
        return if (f.isFile) runCatching { OhSpace.fromJson(JSONObject(f.readText())) }.getOrNull() else null
    }

    fun save(ctx: Context, space: OhSpace) {
        val d = File(spacesDir(ctx), space.id).apply { mkdirs() }
        val tmp = File(d, "space.json.tmp")
        tmp.writeText(space.toJson().toString(2))
        tmp.renameTo(File(d, "space.json"))
    }

    fun newId(ctx: Context): String {
        val abc = "abcdefghijkmnpqrstuvwxyz23456789"
        while (true) {
            val id = (1..6).map { abc.random() }.joinToString("")
            if (!File(spacesDir(ctx), id).exists()) return id
        }
    }

    /** A new space bound to an installed runtime: fresh data area and settings. */
    fun create(ctx: Context, name: String, runtime: OhRuntimeRepository.OhRuntime, settings: OhSettings = OhSettings()): OhSpace {
        val space = OhSpace(
            id = newId(ctx),
            name = name,
            runtimeVersion = runtime.version,
            ohVersion = runtime.ohVersion,
            settings = settings,
            createdAt = System.currentTimeMillis(),
        )
        save(ctx, space)
        dataDir(ctx, space.id)
        return space
    }

    @Synchronized
    fun update(ctx: Context, id: String, f: (OhSpace) -> OhSpace) {
        get(ctx, id)?.let { save(ctx, f(it)) }
    }

    /** Deletes the space directory tree without following symlinks out. */
    fun delete(ctx: Context, id: String): Boolean {
        val d = File(spacesDir(ctx), id)
        if (!d.exists()) return false
        ImageWipe.wipe(d)
        return !d.exists()
    }
}

/** Same no-symlink traversal as core/ImageStore.wipe, kept local to avoid a core dependency. */
private object ImageWipe {
    fun wipe(f: File) {
        val isLink = runCatching {
            android.system.OsConstants.S_ISLNK(android.system.Os.lstat(f.path).st_mode)
        }.getOrDefault(false)
        if (!isLink && f.isDirectory) { f.setWritable(true, true); f.listFiles()?.forEach { wipe(it) } }
        f.delete()
    }
}
