package app.aemu.core

import android.system.Os
import android.system.OsConstants
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Guest LED emulation:
 * 1. Lights HAL (/dev/aemu_lights FIFO): written by lights.default.so
 *    "<light> <argb hex> <flash mode> <on ms> <off ms>"
 * 2. Samsung SVC LED (/sys/class/sec/led/led_pattern, led_blink)
 * 3. Linux evdev / sysfs RGB LEDs:
 *    /sys/class/leds/{red,green,blue,amber,white}/brightness, delay_on, delay_off
 * 4. Sony illumination bar / LEDs:
 *    /sys/class/leds/rgb/rgb_test, /sys/class/leds/so34-led/brightness
 *
 * If no real LED signal is requested by the guest (brightness 0 / color 0), current is null
 * and NO dummy light is drawn.
 */
class Lights(private val root: File, private val log: (String) -> Unit) {
    data class Led(val color: Int, val onMs: Int, val offMs: Int) {
        val lit get() = (color and 0xffffff) != 0
    }

    private val fifo = File(root, "dev/aemu_lights")
    private val samsung = listOf("led_pattern", "led_blink").map { File(root, "sys/class/sec/led/$it") }

    private val sysfsPaths = listOf(
        "red/brightness", "red/delay_on", "red/delay_off",
        "green/brightness", "green/delay_on", "green/delay_off",
        "blue/brightness", "blue/delay_on", "blue/delay_off",
        "amber/brightness", "white/brightness",
        "so34-led/brightness", "rgb/rgb_test",
        "led:rgb_red/brightness", "led:rgb_green/brightness", "led:rgb_blue/brightness"
    ).map { File(root, "sys/class/leds/$it") }

    @Volatile private var running = false
    private val state = HashMap<String, Led>()
    private val listeners = CopyOnWriteArrayList<(Led?) -> Unit>()
    @Volatile var current: Led? = null
        private set

    @Volatile private var sysfsR = 0
    @Volatile private var sysfsG = 0
    @Volatile private var sysfsB = 0
    @Volatile private var sysfsOn = 0
    @Volatile private var sysfsOff = 0

    fun onChange(l: (Led?) -> Unit) { listeners.add(l) }

    fun start() {
        running = true
        runCatching {
            mkfifo(fifo)
            Thread({ readHal() }, "lights-hal").apply { isDaemon = true; start() }
        }.onFailure { log("lights: FIFO not created: $it") }

        for (f in samsung) runCatching {
            mkfifo(f)
            Thread({ readSamsung(f) }, "lights-${f.name}").apply { isDaemon = true; start() }
        }
        runCatching { File(root, "sys/class/sec/led/led_lowpower").let { if (!it.exists()) { it.parentFile?.mkdirs(); it.writeText("0") } } }

        for (f in sysfsPaths) runCatching {
            mkfifo(f)
            Thread({ readSysfs(f) }, "lights-sysfs-${f.parentFile?.name ?: ""}-${f.name}").apply { isDaemon = true; start() }
        }
    }

    fun stop() {
        running = false
        // wake readers
        for (f in sysfsPaths + samsung + fifo) runCatching { File(f.absolutePath).appendText("0\n") }
    }

    private fun mkfifo(f: File) {
        f.delete(); f.parentFile?.mkdirs()
        Os.mkfifo(f.absolutePath, "666".toInt(8))
        Os.chmod(f.absolutePath, "666".toInt(8))
    }

    private fun readHal() {
        val fd = Os.open(fifo.absolutePath, OsConstants.O_RDWR or OsConstants.O_CLOEXEC, 0)
        FileInputStream(fd).bufferedReader().use { r ->
            while (running) {
                val line = runCatching { r.readLine() }.getOrNull() ?: break
                val p = line.trim().split(' ')
                if (p.size < 5) continue
                val col = p[1].toLongOrNull(16)?.toInt() ?: continue
                val onMs = p[3].toIntOrNull() ?: 0
                val offMs = p[4].toIntOrNull() ?: 0
                publish(p[0], Led(col, onMs, offMs))
            }
        }
    }

    private fun readSamsung(f: File) {
        val fd = Os.open(f.absolutePath, OsConstants.O_RDWR or OsConstants.O_CLOEXEC, 0)
        val buf = ByteArray(128)
        FileInputStream(fd).use { s ->
            while (running) {
                val n = runCatching { s.read(buf) }.getOrDefault(-1)
                if (n <= 0) break
                val t = String(buf, 0, n).trim().lines().lastOrNull()?.trim() ?: continue
                if (f.name == "led_pattern") {
                    val id = t.toIntOrNull() ?: 0
                    val led = when (id) {
                        1 -> Led(0xff0000, 0, 0)
                        2 -> Led(0xff0000, 500, 500)
                        3 -> Led(0x0000ff, 500, 5000)
                        4 -> Led(0xff0000, 500, 5000)
                        5 -> Led(0x00ff00, 0, 0)
                        6 -> Led(0x00ffff, 1000, 1000)
                        else -> Led(0, 0, 0)
                    }
                    if (id == 0) { publish("battery", led); publish("notifications", led) }
                    else publish(if (id == 3 || id == 6) "notifications" else "battery", led)
                } else {
                    val p = t.split(' ').filter { it.isNotEmpty() }
                    publish("notifications", Led(p[0].removePrefix("0x").toLongOrNull(16)?.toInt() ?: 0,
                        p.getOrNull(1)?.toIntOrNull() ?: 0, p.getOrNull(2)?.toIntOrNull() ?: 0))
                }
            }
        }
    }

    private fun readSysfs(f: File) {
        val fd = Os.open(f.absolutePath, OsConstants.O_RDWR or OsConstants.O_CLOEXEC, 0)
        val buf = ByteArray(128)
        val name = f.name
        val parent = f.parentFile?.name ?: ""
        FileInputStream(fd).use { s ->
            while (running) {
                val n = runCatching { s.read(buf) }.getOrDefault(-1)
                if (n <= 0) break
                val t = String(buf, 0, n).trim().lines().lastOrNull()?.trim() ?: continue
                val v = t.toIntOrNull() ?: 0
                when {
                    name == "brightness" && (parent == "red" || parent == "led:rgb_red") -> sysfsR = v.coerceIn(0, 255)
                    name == "brightness" && (parent == "green" || parent == "led:rgb_green") -> sysfsG = v.coerceIn(0, 255)
                    name == "brightness" && (parent == "blue" || parent == "led:rgb_blue" || parent == "so34-led") -> sysfsB = v.coerceIn(0, 255)
                    name == "brightness" && parent == "amber" -> { sysfsR = v.coerceIn(0, 255); sysfsG = (v * 3 / 4).coerceIn(0, 255); sysfsB = 0 }
                    name == "brightness" && parent == "white" -> { sysfsR = v.coerceIn(0, 255); sysfsG = sysfsR; sysfsB = sysfsR }
                    name == "delay_on" -> sysfsOn = v.coerceAtLeast(0)
                    name == "delay_off" -> sysfsOff = v.coerceAtLeast(0)
                    name == "rgb_test" -> {
                        // Sony rgb_test accepts "R G B" or "#RRGGBB"
                        val parts = t.split(Regex("[\\s,]+"))
                        if (parts.size >= 3) {
                            sysfsR = parts[0].toIntOrNull()?.coerceIn(0, 255) ?: 0
                            sysfsG = parts[1].toIntOrNull()?.coerceIn(0, 255) ?: 0
                            sysfsB = parts[2].toIntOrNull()?.coerceIn(0, 255) ?: 0
                        }
                    }
                }
                val rgb = ((sysfsR and 0xff) shl 16) or ((sysfsG and 0xff) shl 8) or (sysfsB and 0xff)
                publish("notifications", Led(rgb, sysfsOn, sysfsOff))
            }
        }
    }

    /** The LED shows the most urgent request. If none or brightness is 0, current is null. */
    private fun publish(name: String, led: Led) {
        val shown = synchronized(state) {
            state[name] = led
            listOf("attention", "notifications", "battery").firstNotNullOfOrNull { n -> state[n]?.takeIf { it.lit } }
        }
        if (shown != current) {
            current = shown
            listeners.forEach { runCatching { it(shown) } }
        }
    }
}
