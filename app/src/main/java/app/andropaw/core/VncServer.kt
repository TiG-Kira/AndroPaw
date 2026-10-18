package app.aemu.core

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * The guest's screen and input over VNC (RFB 3.3–3.8), for any standard viewer.
 *
 * Frames come from [frames] as ARGB pixels of the guest's own resolution; a viewer gets them in the "Raw" encoding,
 * the one every viewer has, as the band of rows that changed since its last update. The first mouse button is the
 * finger; keys arrive as X11 keysyms and go to the guest as Linux key codes.
 *
 * With a password (VNC authentication, at most 8 characters count) the server listens on every interface; without
 * one only on the loopback address, so that an open session is reachable through `adb forward` and not from the
 * network.
 */
class VncServer(
    private val port: Int,
    private val password: String,
    private val width: Int,
    private val height: Int,
    private val name: String,
    private val frames: FrameSource,
    private val input: InputService,
    private val log: (String) -> Unit,
) {
    fun interface FrameSource {
        /** Fills [into] (width*height ARGB pixels) with the current frame; false when there is none to show. */
        fun grab(into: IntArray): Boolean
    }

    @Volatile private var server: ServerSocket? = null
    private val clients = java.util.concurrent.CopyOnWriteArrayList<Socket>()
    private val grabLock = Any()

    val running: Boolean get() = server != null

    fun start(): Boolean {
        if (server != null) return true
        return runCatching {
            val bind = if (password.isEmpty()) InetAddress.getLoopbackAddress() else null
            val s = ServerSocket(port, 4, bind)
            server = s
            Thread({ accept(s) }, "vnc-accept").apply { isDaemon = true }.start()
            log("vnc: listening on ${if (bind == null) "all interfaces" else "127.0.0.1"}, port $port")
        }.onFailure { log("vnc: cannot listen on port $port: ${it.message}") }.isSuccess
    }

    fun stop() {
        val s = server ?: return
        server = null
        runCatching { s.close() }
        clients.forEach { runCatching { it.close() } }
        clients.clear()
        log("vnc: stopped")
    }

    private fun accept(s: ServerSocket) {
        while (server === s) {
            val c = runCatching { s.accept() }.getOrNull() ?: break
            clients.add(c)
            Thread({
                runCatching { serve(c) }.onFailure { e -> if (server != null) log("vnc: ${c.inetAddress.hostAddress}: ${e.message ?: e}") }
                runCatching { c.close() }
                clients.remove(c)
            }, "vnc-client").apply { isDaemon = true }.start()
        }
    }

    /** the pixel format a viewer asked for */
    private class Format(var bpp: Int = 32, var bigEndian: Boolean = false, var rMax: Int = 255, var gMax: Int = 255, var bMax: Int = 255,
                         var rShift: Int = 16, var gShift: Int = 8, var bShift: Int = 0)

    private fun serve(c: Socket) {
        c.tcpNoDelay = true
        val inp = DataInputStream(c.getInputStream().buffered())
        val out = DataOutputStream(c.getOutputStream().buffered(1 shl 16))
        out.write("RFB 003.008\n".toByteArray()); out.flush()
        val ver = ByteArray(12); inp.readFully(ver)
        val minor = String(ver, 8, 3).toIntOrNull() ?: 3
        val type = if (password.isEmpty()) 1 else 2
        if (minor >= 7) {
            out.writeByte(1); out.writeByte(type); out.flush()
            if (inp.readUnsignedByte() != type) return
        } else {
            out.writeInt(type); out.flush()
        }
        if (type == 2) {
            val challenge = ByteArray(16).also { SecureRandom().nextBytes(it) }
            out.write(challenge); out.flush()
            val answer = ByteArray(16); inp.readFully(answer)
            if (!answer.contentEquals(vncDes(challenge, password))) {
                out.writeInt(1)
                if (minor >= 8) { val m = "wrong password".toByteArray(); out.writeInt(m.size); out.write(m) }
                out.flush()
                log("vnc: ${c.inetAddress.hostAddress}: wrong password")
                return
            }
            out.writeInt(0)
        } else if (minor >= 8) out.writeInt(0)
        out.flush()
        inp.readUnsignedByte()                                  // ClientInit: shared or not, all the same here
        val fmt = Format()
        out.writeShort(width); out.writeShort(height)
        out.writeByte(32); out.writeByte(24); out.writeByte(0); out.writeByte(1)
        out.writeShort(255); out.writeShort(255); out.writeShort(255)
        out.writeByte(16); out.writeByte(8); out.writeByte(0)
        out.write(ByteArray(3))
        val nm = name.toByteArray()
        out.writeInt(nm.size); out.write(nm); out.flush()
        log("vnc: ${c.inetAddress.hostAddress} connected")

        val wanted = Object()
        var want = 0                                           // 0 nothing asked, 1 changes only, 2 the whole screen
        var open = true
        val sender = Thread({
            val cur = IntArray(width * height)
            val sent = IntArray(width * height)
            var haveSent = false
            runCatching {
                while (open) {
                    val full: Boolean
                    synchronized(wanted) {
                        while (want == 0 && open) wanted.wait(500)
                        full = want == 2 || !haveSent
                    }
                    if (!open) break
                    val ok = synchronized(grabLock) { frames.grab(cur) }
                    if (!ok) { Thread.sleep(100); continue }
                    var y0 = 0
                    var y1 = height - 1
                    if (!full) {
                        while (y0 < height && rowSame(cur, sent, y0)) y0++
                        if (y0 == height) { Thread.sleep(40); continue }        // nothing new: the request stays open
                        while (y1 > y0 && rowSame(cur, sent, y1)) y1--
                    }
                    val rows = y1 - y0 + 1
                    synchronized(out) {
                        out.writeByte(0); out.writeByte(0); out.writeShort(1)
                        out.writeShort(0); out.writeShort(y0); out.writeShort(width); out.writeShort(rows)
                        out.writeInt(0)                                         // Raw
                        out.write(encode(cur, y0 * width, rows * width, fmt))
                        out.flush()
                    }
                    System.arraycopy(cur, y0 * width, sent, y0 * width, rows * width)
                    haveSent = true
                    synchronized(wanted) { want = 0 }
                    Thread.sleep(30)
                }
            }
            runCatching { c.close() }
        }, "vnc-send").apply { isDaemon = true; start() }

        var buttonDown = false
        var shiftHeld = false
        try {
            while (true) {
                when (inp.readUnsignedByte()) {
                    0 -> {                                  // SetPixelFormat
                        inp.skipBytes(3)
                        val bpp = inp.readUnsignedByte(); inp.readUnsignedByte()
                        val be = inp.readUnsignedByte() != 0; inp.readUnsignedByte()
                        val rm = inp.readUnsignedShort(); val gm = inp.readUnsignedShort(); val bm = inp.readUnsignedShort()
                        val rs = inp.readUnsignedByte(); val gs = inp.readUnsignedByte(); val bs = inp.readUnsignedByte()
                        inp.skipBytes(3)
                        synchronized(out) { fmt.bpp = bpp; fmt.bigEndian = be; fmt.rMax = rm; fmt.gMax = gm; fmt.bMax = bm; fmt.rShift = rs; fmt.gShift = gs; fmt.bShift = bs }
                        synchronized(wanted) { want = 2; wanted.notifyAll() }
                    }
                    2 -> { inp.skipBytes(1); val n = inp.readUnsignedShort(); inp.skipBytes(4 * n) }     // SetEncodings: Raw it is
                    3 -> {                                  // FramebufferUpdateRequest
                        val incremental = inp.readUnsignedByte() != 0
                        inp.skipBytes(8)
                        synchronized(wanted) { want = maxOf(want, if (incremental) 1 else 2); wanted.notifyAll() }
                    }
                    4 -> {                                  // KeyEvent
                        val down = inp.readUnsignedByte() != 0
                        inp.skipBytes(2)
                        val sym = inp.readInt()
                        if (sym == 0xffe1 || sym == 0xffe2) { shiftHeld = down; input.key(KEY_LEFTSHIFT, down) }
                        else keyOf(sym)?.let { (code, shift) ->
                            // a character that needs Shift on the guest's keyboard, typed without it on the viewer's
                            if (shift && !shiftHeld && down) input.key(KEY_LEFTSHIFT, true)
                            input.key(code, down)
                            if (shift && !shiftHeld && !down) input.key(KEY_LEFTSHIFT, false)
                        }
                    }
                    5 -> {                                  // PointerEvent
                        val mask = inp.readUnsignedByte()
                        val x = inp.readUnsignedShort().coerceIn(0, width - 1)
                        val y = inp.readUnsignedShort().coerceIn(0, height - 1)
                        val down = mask and 1 != 0
                        if (down) input.touch(listOf(InputService.P(0, x, y)))
                        else if (buttonDown) input.touch(emptyList())
                        buttonDown = down
                    }
                    6 -> { inp.skipBytes(3); val n = inp.readInt(); if (n < 0 || n > (1 shl 20)) return; inp.skipBytes(n) }  // ClientCutText
                    else -> return
                }
            }
        } finally {
            open = false
            synchronized(wanted) { wanted.notifyAll() }
            if (buttonDown) input.touch(emptyList())
            log("vnc: ${c.inetAddress.hostAddress} disconnected")
        }
    }

    private fun rowSame(a: IntArray, b: IntArray, y: Int): Boolean {
        val o = y * width
        for (i in o until o + width) if (a[i] != b[i]) return false
        return true
    }

    private fun encode(px: IntArray, from: Int, count: Int, f: Format): ByteArray {
        val bytes = f.bpp / 8
        val out = ByteArray(count * bytes)
        var o = 0
        for (i in from until from + count) {
            val p = px[i]
            val r = (p shr 16 and 0xff) * f.rMax / 255
            val g = (p shr 8 and 0xff) * f.gMax / 255
            val b = (p and 0xff) * f.bMax / 255
            val v = (r shl f.rShift) or (g shl f.gShift) or (b shl f.bShift)
            when (bytes) {
                4 -> if (f.bigEndian) { out[o] = (v ushr 24).toByte(); out[o + 1] = (v ushr 16).toByte(); out[o + 2] = (v ushr 8).toByte(); out[o + 3] = v.toByte() }
                     else { out[o] = v.toByte(); out[o + 1] = (v ushr 8).toByte(); out[o + 2] = (v ushr 16).toByte(); out[o + 3] = (v ushr 24).toByte() }
                2 -> if (f.bigEndian) { out[o] = (v ushr 8).toByte(); out[o + 1] = v.toByte() } else { out[o] = v.toByte(); out[o + 1] = (v ushr 8).toByte() }
                else -> out[o] = v.toByte()
            }
            o += bytes
        }
        return out
    }

    companion object {
        const val DEFAULT_PORT = 5900
        private const val KEY_LEFTSHIFT = 42

        /** VNC authentication: the challenge encrypted with DES under the password, each key byte bit-reversed */
        private fun vncDes(challenge: ByteArray, password: String): ByteArray {
            val key = ByteArray(8)
            val pw = password.toByteArray(Charsets.ISO_8859_1)
            for (i in 0 until 8) {
                val b = if (i < pw.size) pw[i].toInt() and 0xff else 0
                key[i] = (Integer.reverse(b) ushr 24).toByte()
            }
            val des = Cipher.getInstance("DES/ECB/NoPadding")
            des.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "DES"))
            return des.doFinal(challenge)
        }

        private const val ROW1 = "qwertyuiop"   // Linux key codes 16..25
        private const val ROW2 = "asdfghjkl"    // 30..38
        private const val ROW3 = "zxcvbnm"      // 44..50
        private val PLAIN = mapOf('1' to 2, '2' to 3, '3' to 4, '4' to 5, '5' to 6, '6' to 7, '7' to 8, '8' to 9, '9' to 10, '0' to 11,
            '-' to 12, '=' to 13, '[' to 26, ']' to 27, ';' to 39, '\'' to 40, '`' to 41, '\\' to 43, ',' to 51, '.' to 52, '/' to 53, ' ' to 57)
        private val SHIFTED = mapOf('!' to 2, '@' to 3, '#' to 4, '$' to 5, '%' to 6, '^' to 7, '&' to 8, '*' to 9, '(' to 10, ')' to 11,
            '_' to 12, '+' to 13, '{' to 26, '}' to 27, ':' to 39, '"' to 40, '~' to 41, '|' to 43, '<' to 51, '>' to 52, '?' to 53)

        /** X11 keysym to (Linux key code, needs Shift) */
        private fun keyOf(sym: Int): Pair<Int, Boolean>? {
            when (sym) {
                0xff08 -> return 14 to false            // BackSpace
                0xff09 -> return 15 to false            // Tab
                0xff0d, 0xff8d -> return 28 to false    // Return, keypad Enter
                0xff1b -> return InputService.KEY_BACK to false     // Escape: Back
                0xff50 -> return InputService.KEY_HOME to false     // Home
                0xff51 -> return 105 to false           // Left
                0xff52 -> return 103 to false           // Up
                0xff53 -> return 106 to false           // Right
                0xff54 -> return 108 to false           // Down
                0xff55 -> return InputService.KEY_VOLUMEUP to false     // Page Up
                0xff56 -> return InputService.KEY_VOLUMEDOWN to false   // Page Down
                0xff57 -> return InputService.KEY_POWER to false        // End
                0xffff -> return 111 to false           // Delete
                0xffbe -> return InputService.KEY_MENU to false         // F1
                0xffbf -> return InputService.KEY_APPSELECT to false    // F2: recent apps
                0xffc0 -> return InputService.KEY_SEARCH to false       // F3
            }
            if (sym !in 0x20..0x7e) return null
            val ch = sym.toChar()
            val lower = ch.lowercaseChar()
            ROW1.indexOf(lower).takeIf { it >= 0 }?.let { return (16 + it) to ch.isUpperCase() }
            ROW2.indexOf(lower).takeIf { it >= 0 }?.let { return (30 + it) to ch.isUpperCase() }
            ROW3.indexOf(lower).takeIf { it >= 0 }?.let { return (44 + it) to ch.isUpperCase() }
            PLAIN[ch]?.let { return it to false }
            SHIFTED[ch]?.let { return it to true }
            return null
        }
    }
}
