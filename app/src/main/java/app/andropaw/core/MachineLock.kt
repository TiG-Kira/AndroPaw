package app.andropaw.core

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Per-machine password protection — the headline feature of the upstream AEmulator Pro fork,
 * in a simplified form: one PBKDF2-derived verifier per machine, checked before the machine
 * window opens. No plaintext is stored and no key is kept in memory.
 *
 * This is a privacy / accidental-tap guard, not a disk-encryption scheme: the verifier lives
 * in the app's own private preferences, so a root user or a full device backup could remove it.
 * That is the same trust boundary upstream has.
 */
object MachineLock {
    private const val FILE = "machine_locks"
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16

    private fun sp(ctx: Context) = ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private fun key(id: String) = "lock_$id"

    fun isLocked(ctx: Context, id: String): Boolean = sp(ctx).contains(key(id))

    fun clear(ctx: Context, id: String) = sp(ctx).edit().remove(key(id)).apply()

    fun set(ctx: Context, id: String, password: CharArray) {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = derive(password, salt)
        sp(ctx).edit().putString(key(id), "${enc(salt)}:${enc(hash)}").apply()
    }

    /** Constant-time comparison; returns false for a machine with no verifier. */
    fun verify(ctx: Context, id: String, password: CharArray): Boolean {
        val stored = sp(ctx).getString(key(id), null) ?: return false
        val parts = stored.split(':')
        if (parts.size != 2) return false
        val expected = runCatching { Base64.decode(parts[1], Base64.NO_WRAP) }.getOrNull() ?: return false
        val salt = runCatching { Base64.decode(parts[0], Base64.NO_WRAP) }.getOrNull() ?: return false
        val actual = derive(password, salt)
        return MessageDigest.isEqual(expected, actual)
    }

    private fun derive(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun enc(b: ByteArray): String = Base64.encodeToString(b, Base64.NO_WRAP)
}
