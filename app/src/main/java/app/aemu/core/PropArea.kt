package app.aemu.core

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Область системных свойств Android bionic.
 * Поддерживает два формата:
 * - v1 (PROP / 0x45434f76): плоский список со смещениями TOC, используется в Android 1.0–7.1.
 * - v2 (PROP / 0xfc6ed0ab): гибридный Trie/Binary-Tree формат bionic libc, используется в Android 8.0+ (Oreo+).
 */
abstract class PropArea(protected val b: ByteBuffer, protected val chan: FileChannel?) : AutoCloseable {

    abstract val count: Int
    abstract fun get(name: String): String?
    abstract fun put(name: String, value: String): Boolean
    abstract fun all(): List<Pair<String, String>>
    abstract fun room(): Int

    override fun close() {
        (b as? MappedByteBuffer)?.force()
        runCatching { chan?.close() }
    }

    companion object {
        const val SIZE = 131072
        const val MAGIC = 0x504f5250
        const val VERSION_V1 = 0x45434f76
        const val VERSION_V2 = 0xfc6ed0ab.toInt()

        /** Создаёт пустую область в файле: v2 для Oreo+ (API 26+), v1 для более старых. */
        fun create(f: File, api: Int = 19): PropArea {
            f.parentFile?.mkdirs()
            RandomAccessFile(f, "rw").use { it.setLength(0); it.setLength(SIZE.toLong()) }
            val ch = RandomAccessFile(f, "rw").channel
            val m = ch.map(FileChannel.MapMode.READ_WRITE, 0, f.length()).apply { order(ByteOrder.LITTLE_ENDIAN) }
            return if (api >= 26) {
                // v2 header (128 байт): bytes_used_, serial_, magic, version, reserved[28]
                m.putInt(0, 24) // bytes_used_: размер корневого узла prop_bt (20 байт + '\0' выровненные = 24)
                m.putInt(4, 0)
                m.putInt(8, MAGIC)
                m.putInt(12, VERSION_V2)
                for (i in 16 until 128) m.put(i, 0)
                // root node at data offset 0 (file offset 128): namelen=0, prop=0, left=0, right=0, children=0, name="\0"
                for (i in 0 until 24) m.put(128 + i, 0)
                PropAreaV2(m, ch)
            } else {
                m.putInt(0, 0)
                m.putInt(4, 0)
                m.putInt(8, MAGIC)
                m.putInt(12, VERSION_V1)
                PropAreaV1(m, ch)
            }
        }

        /** Отображает существующую область (v1 или v2). */
        fun open(f: File): PropArea? {
            if (!f.isFile || f.length() < 128) return null
            val ch = RandomAccessFile(f, "rw").channel
            val m = ch.map(FileChannel.MapMode.READ_WRITE, 0, f.length()).apply { order(ByteOrder.LITTLE_ENDIAN) }
            val magic = m.getInt(8)
            val version = m.getInt(12)
            if (magic != MAGIC && !(magic == 0 && m.getInt(0) == 0)) {
                runCatching { ch.close() }
                return null
            }
            return when (version) {
                VERSION_V2 -> PropAreaV2(m, ch)
                VERSION_V1, 0 -> PropAreaV1(m, ch)
                else -> {
                    if (m.getInt(0) > 1000 && m.getInt(0) < SIZE) PropAreaV2(m, ch) else PropAreaV1(m, ch)
                }
            }
        }

        /** Разбор build.prop/default.prop: ключ=значение, комментарии, import не поддерживаем. */
        fun parseProps(text: String): LinkedHashMap<String, String> {
            val out = LinkedHashMap<String, String>()
            for (raw in text.lineSequence()) {
                val ln = raw.trim()
                if (ln.isEmpty() || ln.startsWith("#") || ln.startsWith("import ")) continue
                val i = ln.indexOf('=')
                if (i <= 0) continue
                out[ln.substring(0, i).trim()] = ln.substring(i + 1).trim()
            }
            return out
        }
    }
}

/** Формат bionic v1 (Android 1.0–7.1). */
class PropAreaV1(b: ByteBuffer, chan: FileChannel?) : PropArea(b, chan) {

    override val count: Int get() = b.getInt(0)

    override fun get(name: String): String? {
        val off = find(name) ?: return null
        val serial = b.getInt(off + NAME_MAX)
        val len = (serial ushr 24).coerceIn(0, VALUE_MAX - 1)
        val v = ByteArray(len)
        for (i in 0 until len) v[i] = b.get(off + NAME_MAX + 4 + i)
        return String(v, Charsets.UTF_8)
    }

    override fun all(): List<Pair<String, String>> {
        val out = ArrayList<Pair<String, String>>()
        for (i in 0 until count) {
            val e = b.getInt(TOC + i * 4)
            val off = e and 0xffffff
            val nl = e ushr 24
            val nb = ByteArray(nl) { b.get(off + it) }
            val n = String(nb, Charsets.UTF_8)
            out.add(n to (get(n) ?: ""))
        }
        return out
    }

    @Synchronized
    override fun put(name: String, value: String): Boolean {
        val nb = name.toByteArray(Charsets.UTF_8)
        var vb = value.toByteArray(Charsets.UTF_8)
        if (nb.isEmpty() || nb.size >= NAME_MAX) return false
        if (vb.size >= VALUE_MAX) vb = vb.copyOf(VALUE_MAX - 1)
        val off = find(name)
        if (off != null) {
            val s0 = b.getInt(off + NAME_MAX)
            b.putInt(off + NAME_MAX, s0 or 1) // нечётный — «пишу»
            for (i in vb.indices) b.put(off + NAME_MAX + 4 + i, vb[i])
            b.put(off + NAME_MAX + 4 + vb.size, 0)
            b.putInt(off + NAME_MAX, (vb.size shl 24) or (((s0 or 1) + 1) and 0xffffff))
            b.putInt(4, b.getInt(4) + 1)
            return true
        }
        val n = count
        val at = nextInfo(n)
        if ((n + 1) * 4 + TOC > firstInfo(n)) return false
        if (at + INFO > b.capacity()) return false
        for (i in 0 until INFO) b.put(at + i, 0)
        for (i in nb.indices) b.put(at + i, nb[i])
        for (i in vb.indices) b.put(at + NAME_MAX + 4 + i, vb[i])
        b.putInt(at + NAME_MAX, vb.size shl 24)
        b.putInt(TOC + n * 4, (nb.size shl 24) or at)
        b.putInt(0, n + 1)
        b.putInt(4, b.getInt(4) + 1)
        return true
    }

    override fun room(): Int {
        val n = count
        val byToc = (firstInfo(n) - TOC) / 4 - n
        val byTail = (b.capacity() - nextInfo(n)) / INFO
        return maxOf(0, minOf(byToc, byTail))
    }

    private fun find(name: String): Int? {
        val nb = name.toByteArray(Charsets.UTF_8)
        for (i in 0 until count) {
            val e = b.getInt(TOC + i * 4)
            if ((e ushr 24) != nb.size) continue
            val off = e and 0xffffff
            var same = true
            for (j in nb.indices) if (b.get(off + j) != nb[j]) { same = false; break }
            if (same && b.get(off + nb.size) == 0.toByte()) return off
        }
        return null
    }

    private fun firstInfo(n: Int): Int {
        if (n == 0) return INFO_START
        var m = Int.MAX_VALUE
        for (i in 0 until n) m = minOf(m, b.getInt(TOC + i * 4) and 0xffffff)
        return m
    }

    private fun nextInfo(n: Int): Int {
        if (n == 0) return firstInfo(0)
        var m = 0
        for (i in 0 until n) m = maxOf(m, b.getInt(TOC + i * 4) and 0xffffff)
        return m + INFO
    }

    companion object {
        private const val TOC = 32
        private const val INFO = 128
        private const val NAME_MAX = 32
        private const val VALUE_MAX = 92
        private const val INFO_START = 4096
    }
}

/** Формат bionic v2 Trie (Android 8.0+ Oreo+). */
class PropAreaV2(b: ByteBuffer, chan: FileChannel?) : PropArea(b, chan) {

    private val dataSize = maxOf(SIZE, b.capacity()) - DATA_OFFSET

    override val count: Int get() = all().size

    override fun room(): Int {
        val used = b.getInt(0)
        return maxOf(0, (dataSize - used) / (BT_MIN_SIZE + INFO_MIN_SIZE))
    }

    override fun get(name: String): String? {
        val propOff = findProperty(name, alloc = false)
        if (propOff <= 0) return null
        val pos = DATA_OFFSET + propOff
        val serial = b.getInt(pos)
        val len = (serial ushr 24).coerceIn(0, VALUE_MAX - 1)
        val v = ByteArray(len)
        for (i in 0 until len) v[i] = b.get(pos + 4 + i)
        return String(v, Charsets.UTF_8)
    }

    @Synchronized
    override fun put(name: String, value: String): Boolean {
        val nb = name.toByteArray(Charsets.UTF_8)
        var vb = value.toByteArray(Charsets.UTF_8)
        if (nb.isEmpty() || nb.size >= NAME_MAX_V2) return false
        if (vb.size >= VALUE_MAX) vb = vb.copyOf(VALUE_MAX - 1)

        val propOff = findProperty(name, alloc = true)
        if (propOff <= 0) return false

        val pos = DATA_OFFSET + propOff
        val s0 = b.getInt(pos)
        b.putInt(pos, s0 or 1) // dirty bit
        for (i in vb.indices) b.put(pos + 4 + i, vb[i])
        b.put(pos + 4 + vb.size, 0)
        b.putInt(pos, (vb.size shl 24) or (((s0 or 1) + 1) and 0xffffff))
        b.putInt(4, b.getInt(4) + 1) // header serial_++
        return true
    }

    override fun all(): List<Pair<String, String>> {
        val out = ArrayList<Pair<String, String>>()
        val rootChildren = b.getInt(DATA_OFFSET + 16)
        if (rootChildren > 0) {
            collect(rootChildren, out)
        }
        return out
    }

    private fun collect(btOff: Int, out: MutableList<Pair<String, String>>) {
        if (btOff <= 0 || btOff + 20 > dataSize) return
        val pos = DATA_OFFSET + btOff
        val propOff = b.getInt(pos + 4)
        if (propOff > 0 && propOff + 96 <= dataSize) {
            val pPos = DATA_OFFSET + propOff
            val serial = b.getInt(pPos)
            val vlen = (serial ushr 24).coerceIn(0, VALUE_MAX - 1)
            val vb = ByteArray(vlen) { b.get(pPos + 4 + it) }
            val valStr = String(vb, Charsets.UTF_8)

            var nlen = 0
            while (pPos + 96 + nlen < b.capacity() && b.get(pPos + 96 + nlen) != 0.toByte()) nlen++
            val nb = ByteArray(nlen) { b.get(pPos + 96 + it) }
            val nameStr = String(nb, Charsets.UTF_8)
            out.add(nameStr to valStr)
        }
        val left = b.getInt(pos + 8)
        if (left > 0) collect(left, out)
        val right = b.getInt(pos + 12)
        if (right > 0) collect(right, out)
        val children = b.getInt(pos + 16)
        if (children > 0) collect(children, out)
    }

    private fun allocateObj(size: Int): Int {
        val aligned = (size + 3) and 3.inv()
        val used = b.getInt(0)
        if (used + aligned > dataSize) return -1
        b.putInt(0, used + aligned)
        val start = DATA_OFFSET + used
        for (i in 0 until aligned) b.put(start + i, 0)
        return used
    }

    private fun newPropBt(name: ByteArray): Int {
        val off = allocateObj(20 + name.size + 1)
        if (off < 0) return -1
        val pos = DATA_OFFSET + off
        b.putInt(pos, name.size)
        for (i in name.indices) b.put(pos + 20 + i, name[i])
        b.put(pos + 20 + name.size, 0)
        return off
    }

    private fun newPropInfo(name: ByteArray, value: ByteArray): Int {
        val off = allocateObj(96 + name.size + 1)
        if (off < 0) return -1
        val pos = DATA_OFFSET + off
        b.putInt(pos, value.size shl 24)
        for (i in value.indices) b.put(pos + 4 + i, value[i])
        b.put(pos + 4 + value.size, 0)
        for (i in name.indices) b.put(pos + 96 + i, name[i])
        b.put(pos + 96 + name.size, 0)
        return off
    }

    private fun cmpPropName(n1: ByteArray, n2: ByteArray): Int {
        if (n1.size != n2.size) return n1.size.compareTo(n2.size)
        for (i in n1.indices) {
            val b1 = n1[i].toInt() and 0xff
            val b2 = n2[i].toInt() and 0xff
            if (b1 != b2) return b1.compareTo(b2)
        }
        return 0
    }

    private fun findPropBt(rootBt: Int, seg: ByteArray, alloc: Boolean): Int {
        var cur = rootBt
        while (cur > 0 && cur + 20 <= dataSize) {
            val pos = DATA_OFFSET + cur
            val namelen = b.getInt(pos)
            val curName = ByteArray(namelen) { b.get(pos + 20 + it) }
            val cmp = cmpPropName(seg, curName)
            if (cmp == 0) return cur
            if (cmp < 0) {
                val left = b.getInt(pos + 8)
                if (left != 0) {
                    cur = left
                } else if (alloc) {
                    val newBt = newPropBt(seg)
                    if (newBt < 0) return -1
                    b.putInt(pos + 8, newBt)
                    return newBt
                } else return -1
            } else {
                val right = b.getInt(pos + 12)
                if (right != 0) {
                    cur = right
                } else if (alloc) {
                    val newBt = newPropBt(seg)
                    if (newBt < 0) return -1
                    b.putInt(pos + 12, newBt)
                    return newBt
                } else return -1
            }
        }
        return -1
    }

    private fun findProperty(name: String, alloc: Boolean): Int {
        val parts = name.split('.')
        var curBt = 0 // root_node at data offset 0
        for (segStr in parts) {
            val seg = segStr.toByteArray(Charsets.UTF_8)
            if (seg.isEmpty()) return -1
            val pos = DATA_OFFSET + curBt
            var children = b.getInt(pos + 16)
            if (children == 0) {
                if (!alloc) return -1
                val newBt = newPropBt(seg)
                if (newBt < 0) return -1
                b.putInt(pos + 16, newBt)
                children = newBt
            }
            val nextBt = findPropBt(children, seg, alloc)
            if (nextBt < 0) return -1
            curBt = nextBt
        }
        val pos = DATA_OFFSET + curBt
        val prop = b.getInt(pos + 4)
        if (prop != 0) return prop
        if (!alloc) return -1
        val nb = name.toByteArray(Charsets.UTF_8)
        val newInfo = newPropInfo(nb, ByteArray(0))
        if (newInfo < 0) return -1
        b.putInt(pos + 4, newInfo)
        return newInfo
    }

    companion object {
        private const val DATA_OFFSET = 128
        private const val VALUE_MAX = 92
        private const val NAME_MAX_V2 = 512
        private const val BT_MIN_SIZE = 24
        private const val INFO_MIN_SIZE = 128
    }
}
