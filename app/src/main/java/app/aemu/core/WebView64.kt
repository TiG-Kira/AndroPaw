package app.aemu.core

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * The system WebView of a 64-bit firmware asks for the 32-bit ABI first (android:use32bitAbi="true"): its renderer
 * processes then come from a 32-bit WebView zygote. Up to 14 the engine switched WebView to one process ("cmd
 * webviewupdate disable-multiprocess") and no renderer was started at all; 15 has no such switch, and the 32-bit
 * child zygote, which runs under the translator, cannot be started (its SELinux calls are not there). With the
 * attribute off the package is 64-bit first and the renderers are native 64-bit processes.
 *
 * The attribute is one boolean in the binary AndroidManifest.xml. It is flipped in place: the entry is deflated
 * again into the room it had, and its checksum and size are corrected in the local header and in the central
 * directory. Nothing else in the file moves, so the libraries keep their alignment.
 * A package on a system partition is not checked against its signed digests.
 */
object WebView64 {
    private const val USE_32BIT_ABI = 0x01010515

    /** true when the manifest of [apk] was changed */
    fun prefer64(apk: File, log: (String) -> Unit): Boolean = runCatching {
        RandomAccessFile(apk, "rw").use { f ->
            // end of central directory: the last "PK\5\6"
            val tailLen = minOf(f.length(), 66_000L).toInt()
            val tail = ByteArray(tailLen).also { f.seek(f.length() - tailLen); f.readFully(it) }
            var e = tailLen - 22
            while (e >= 0 && !(tail[e] == 0x50.toByte() && tail[e + 1] == 0x4b.toByte() && tail[e + 2] == 5.toByte() && tail[e + 3] == 6.toByte())) e--
            if (e < 0) return@use false
            val eocd = ByteBuffer.wrap(tail).order(ByteOrder.LITTLE_ENDIAN)
            val cdSize = eocd.getInt(e + 12); val cdOff = eocd.getInt(e + 16).toLong() and 0xffffffffL
            val cd = ByteArray(cdSize).also { f.seek(cdOff); f.readFully(it) }
            val c = ByteBuffer.wrap(cd).order(ByteOrder.LITTLE_ENDIAN)
            var p = 0
            while (p + 46 <= cdSize && c.getInt(p) == 0x02014b50) {
                val nameLen = c.getShort(p + 28).toInt() and 0xffff
                val next = p + 46 + nameLen + (c.getShort(p + 30).toInt() and 0xffff) + (c.getShort(p + 32).toInt() and 0xffff)
                if (String(cd, p + 46, nameLen) != "AndroidManifest.xml") { p = next; continue }
                val flags = c.getShort(p + 8).toInt(); val method = c.getShort(p + 10).toInt()
                val csize = c.getInt(p + 20); val usize = c.getInt(p + 24)
                val lho = c.getInt(p + 42).toLong() and 0xffffffffL
                val lh = ByteArray(30).also { f.seek(lho); f.readFully(it) }
                val l = ByteBuffer.wrap(lh).order(ByteOrder.LITTLE_ENDIAN)
                val dataOff = lho + 30 + (l.getShort(26).toInt() and 0xffff) + (l.getShort(28).toInt() and 0xffff)
                val raw = ByteArray(csize).also { f.seek(dataOff); f.readFully(it) }
                val xml = when (method) {
                    0 -> raw
                    8 -> ByteArray(usize).also { out -> Inflater(true).run { setInput(raw); var n = 0; while (n < usize && !finished()) { val k = inflate(out, n, usize - n); if (k == 0 && needsInput()) break; n += k }; end() } }
                    else -> return@use false
                }
                if (!flip(xml)) return@use false
                // (sizes written after the data, "data descriptor": that record is dropped - the sizes go into the
                // local header instead - and its 16 bytes are room for the entry, which may deflate a little worse)
                var room = csize
                if (flags and 8 != 0) {
                    val sig = ByteArray(4).also { f.seek(dataOff + csize); f.readFully(it) }
                    room += if (ByteBuffer.wrap(sig).order(ByteOrder.LITTLE_ENDIAN).getInt() == 0x08074b50) 16 else 12
                }
                val packed = if (method == 0) xml else {
                    val d = Deflater(9, true); d.setInput(xml); d.finish()
                    val o = ByteArray(room + 64); var n = 0
                    while (!d.finished() && n < o.size) n += d.deflate(o, n, o.size - n)
                    val done = d.finished(); d.end()
                    if (!done || n > room) { log("webview.apk: the changed manifest does not fit its place"); return@use false }
                    o.copyOf(n)
                }
                val crc = CRC32().also { it.update(xml) }.value.toInt()
                f.seek(dataOff); f.write(packed); f.write(ByteArray(room - packed.size))
                l.putShort(6, (flags and 8.inv()).toShort()); l.putInt(14, crc); l.putInt(18, packed.size); l.putInt(22, usize)
                f.seek(lho); f.write(lh)
                c.putShort(p + 8, (flags and 8.inv()).toShort())
                c.putInt(p + 16, crc); c.putInt(p + 20, packed.size)
                f.seek(cdOff + p); f.write(cd, p, 46)
                log("${apk.name}: 64-bit first (use32bitAbi off)")
                return@use true
            }
            false
        }
    }.getOrElse { log("${apk.name}: ${it.message}"); false }

    /** android:use32bitAbi="true" -> "false" in a binary XML document; false when there is nothing to change */
    private fun flip(x: ByteArray): Boolean {
        val b = ByteBuffer.wrap(x).order(ByteOrder.LITTLE_ENDIAN)
        var off = 8; var nameIdx = -1; var changed = false
        while (off + 8 <= x.size) {
            val type = b.getShort(off).toInt() and 0xffff
            val size = b.getInt(off + 4)
            if (size < 8 || off + size > x.size) break
            if (type == 0x0180) {               // the resource map: attribute name index -> attribute id
                for (i in 0 until (size - 8) / 4) if (b.getInt(off + 8 + i * 4) == USE_32BIT_ABI) nameIdx = i
            } else if (type == 0x0102 && nameIdx >= 0) {    // a start tag: header 16, ns, name, attrStart, attrSize, attrCount
                val ext = off + 16
                val attrStart = b.getShort(ext + 8).toInt() and 0xffff
                val attrSize = b.getShort(ext + 10).toInt() and 0xffff
                val count = b.getShort(ext + 12).toInt() and 0xffff
                for (i in 0 until count) {
                    val a = ext + attrStart + i * attrSize      // ns, name, rawValue, typed value (size, 0, type, data)
                    if (a + 20 > off + size) break
                    if (b.getInt(a + 4) == nameIdx && x[a + 15] == 0x12.toByte() && b.getInt(a + 16) != 0) { b.putInt(a + 16, 0); changed = true }
                }
            }
            off += size
        }
        return changed
    }
}
