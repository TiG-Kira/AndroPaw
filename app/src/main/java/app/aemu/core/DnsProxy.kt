package app.aemu.core

import android.net.LocalSocket
import android.system.Os
import java.io.ByteArrayOutputStream
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

/**
 * Host-side stand-in for netd's dnsproxyd (Android 5.0–7.x). Guest libc sends every getaddrinfo/gethostbyname
 * there; the real netd resolves through a per-network interface and marks that do not exist in the sandbox, so
 * every lookup ended in "unknown host". Here the phone resolves the name and the reply is written in the same
 * binary layout DnsProxyListener uses. Netd creates the socket file itself, so [ensure] takes the path over again
 * whenever netd (re)starts.
 */
class DnsProxy(private val paths: VmPaths, private val api: Int, private val probe: Boolean, private val log: (String) -> Unit) {
    /** Whether the firmware's libc takes struct addrinfo as one block (the layout of 5.0, which some vendors kept in
     *  5.1: TouchWiz) and not field by field. Nothing in the request tells, so with [probe] the first reply stops
     *  after "one more entry follows": a libc of the old layout takes that word for a block length, finds it too
     *  short and hangs up at once (that one lookup fails), a libc of the new layout waits for the rest. */
    @Volatile private var block: Boolean? = if (probe) null else false
    private val server = UnixServer(paths.socket("dnsproxyd"), "dnsproxy") { c -> serve(c) }
    private var inode = -1L
    private var failures = 0
    @Volatile var queries = 0L
        private set

    @Synchronized
    fun ensure() {
        val f = paths.socket("dnsproxyd")
        val ino = runCatching { Os.stat(f.absolutePath).st_ino }.getOrDefault(-2L)
        if (server.running && ino == inode) return
        server.stop()
        if (server.start(log)) inode = runCatching { Os.stat(f.absolutePath).st_ino }.getOrDefault(-1L)
    }

    fun stop() { server.stop(); inode = -1 }

    private fun serve(c: LocalSocket) {
        val req = ByteArrayOutputStream()
        val inp = c.inputStream
        while (req.size() < 1024) {
            val b = inp.read()
            if (b <= 0) break
            req.write(b)
        }
        val t = req.toString("UTF-8").trim().split(' ').filter { it.isNotEmpty() }
        if (t.isEmpty()) return
        queries++
        if (queries <= 12) log("dns: " + t.joinToString(" "))
        val sock = c.outputStream
        val out = ByteArrayOutputStream()
        val t0 = System.currentTimeMillis()
        try {
            when (t[0]) {
                "getaddrinfo" -> if (t.size >= 6) getaddrinfo(out, t) else fail(out)
                "gethostbyname" -> hostByName(out, t)
                else -> fail(out)
            }
        } catch (e: Throwable) {
            if (queries <= 40 || failures++ < 200) log("dns: " + t.joinToString(" ") + " failed: " + e)
            runCatching { fail(out) }
        }
        val bytes = out.toByteArray()
        var sent = 0
        try {
            if (block == null && t[0] == "getaddrinfo" && bytes.size > 12) {
                sock.write(bytes, 0, 8); sock.flush(); sent = 8
                c.soTimeout = 300
                val hungUp = try { inp.read() < 0 } catch (e: java.io.IOException) { false }
                block = hungUp
                log("dns: struct addrinfo goes " + if (hungUp) "as one block" else "field by field")
                if (hungUp) return
            }
            while (sent < bytes.size) { val n = minOf(4, bytes.size - sent); sock.write(bytes, sent, n); sock.flush(); sent += n; if (DEBUG_SLOW) Thread.sleep(20) }
        } catch (e: java.io.IOException) { log("dns: client closed after $sent of ${bytes.size} bytes") }
        if (queries <= 6) log("dns: reply " + out.toByteArray().take(96).joinToString("") { "%02x".format(it) })
        if (queries <= 40) log("dns: " + t.getOrElse(1) { "" } + " answered in " + (System.currentTimeMillis() - t0) + " ms")
    }

    private fun fail(o: java.io.OutputStream) {
        o.write(byteArrayOf('4'.code.toByte(), '0'.code.toByte(), '1'.code.toByte(), 0))
        o.write(be(4)); o.write(be(EAI_NODATA))
    }

    private fun be(v: Int) = byteArrayOf((v ushr 24).toByte(), (v ushr 16).toByte(), (v ushr 8).toByte(), v.toByte())

    private fun ok(o: java.io.OutputStream) = o.write(byteArrayOf('2'.code.toByte(), '2'.code.toByte(), '2'.code.toByte(), 0))

    private fun lenData(o: java.io.OutputStream, d: ByteArray) { o.write(be(d.size)); if (d.isNotEmpty()) o.write(d) }

    /** getaddrinfo host serv flags family socktype protocol [netid] */
    private fun getaddrinfo(o: java.io.OutputStream, t: List<String>) {
        val host = t[1].takeIf { it != "^" }
        val serv = t[2].takeIf { it != "^" }
        val flags = t[3].toIntOrNull() ?: 0
        val family = t[4].toIntOrNull() ?: 0
        val socktype = t[5].toIntOrNull() ?: 0
        val port = when (serv) {
            null -> 0
            "http" -> 80; "https" -> 443; "ftp" -> 21; "domain" -> 53
            else -> serv.toIntOrNull() ?: 0
        }
        val addrs: List<InetAddress> = when {
            host == null -> listOf(InetAddress.getByAddress(if (flags and AI_PASSIVE != 0) byteArrayOf(0, 0, 0, 0) else byteArrayOf(127, 0, 0, 1)))
            else -> {
                val all = InetAddress.getAllByName(host).toList()
                val v4 = all.filterIsInstance<Inet4Address>()
                when (family) {
                    AF_INET -> v4
                    AF_INET6 -> all.filterIsInstance<Inet6Address>()
                    else -> v4.ifEmpty { all }   // no IPv6 route inside the guest: prefer IPv4 for AF_UNSPEC
                }
            }
        }
        if (addrs.isEmpty()) { log("dns: no addresses for $host"); return fail(o) }
        if (queries <= 40) log("dns: $host -> " + addrs.joinToString { it.hostAddress ?: "?" })
        val types = if (socktype != 0) listOf(socktype) else listOf(1, 2, 3)
        ok(o)
        var first = true
        for (a in addrs) for (st in types) {
            val fam = if (a is Inet6Address) AF_INET6 else AF_INET
            val proto = when (st) { 1 -> 6; 2 -> 17; else -> 0 }
            val sa = sockaddr(a, port)
            val canon = if (first && flags and AI_CANONNAME != 0 && host != null) (host + "\u0000").toByteArray() else ByteArray(0)
            if (block == true) {
                // the 48 bytes of a 64-bit struct addrinfo; a 32-bit libc reads its 32 from the same block (the
                // integers come first in both, the pointers are the reader's to fill)
                val st64 = java.nio.ByteBuffer.allocate(48).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                st64.putInt(flags).putInt(fam).putInt(st).putInt(proto).putInt(sa.size)
                lenData(o, st64.array()); lenData(o, sa); lenData(o, canon)
            } else if (api <= 21) {
                // 4.x and 5.0: the 32-byte struct addrinfo of a 32-bit process as one blob (pointers zero), then address, then name
                val st32 = java.nio.ByteBuffer.allocate(32).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                st32.putInt(flags).putInt(fam).putInt(st).putInt(proto).putInt(sa.size)
                lenData(o, st32.array()); lenData(o, sa); lenData(o, canon)
            } else {
                o.write(be(1))
                o.write(be(flags)); o.write(be(fam)); o.write(be(st)); o.write(be(proto))
                lenData(o, sa); lenData(o, canon)
            }
            first = false
        }
        o.write(be(0))
    }

    private fun sockaddr(a: InetAddress, port: Int): ByteArray {
        val raw = a.address
        return if (a is Inet6Address) ByteArray(28).also {
            it[0] = AF_INET6.toByte(); it[2] = (port ushr 8).toByte(); it[3] = port.toByte()
            System.arraycopy(raw, 0, it, 8, 16)
        } else ByteArray(16).also {
            it[0] = AF_INET.toByte(); it[2] = (port ushr 8).toByte(); it[3] = port.toByte()
            System.arraycopy(raw, 0, it, 4, 4)
        }
    }

    /** gethostbyname name af netid (5.0 orders the arguments differently; the name is the non-numeric one) */
    private fun hostByName(o: java.io.OutputStream, t: List<String>) {
        val args = t.drop(1)
        val name = args.firstOrNull { !it.all { ch -> ch.isDigit() } } ?: args.firstOrNull() ?: return fail(o)
        val af = args.filter { it != name }.mapNotNull { it.toIntOrNull() }.firstOrNull { it == AF_INET || it == AF_INET6 } ?: AF_INET
        if (af != AF_INET) return fail(o)
        val v4 = InetAddress.getAllByName(name).filterIsInstance<Inet4Address>()
        if (v4.isEmpty()) return fail(o)
        ok(o)
        lenData(o, (name + "\u0000").toByteArray())
        lenData(o, ByteArray(0))                    // no aliases
        o.write(be(AF_INET)); o.write(be(4))
        for (a in v4) lenData(o, a.address.copyOf(16))
        lenData(o, ByteArray(0))
    }

    private companion object {
        const val DEBUG_SLOW = false
        const val AF_INET = 2
        const val AF_INET6 = 10
        const val AI_PASSIVE = 1
        const val AI_CANONNAME = 2
        const val EAI_NODATA = 7
    }
}
