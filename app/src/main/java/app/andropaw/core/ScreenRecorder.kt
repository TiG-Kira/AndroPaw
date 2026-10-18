package app.aemu.core

import android.graphics.Bitmap
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.view.Surface
import java.io.File

/**
 * Records the guest's screen into an MP4 file (H.264, video only).
 *
 * Frames come from [frame] as a bitmap of the guest's resolution, [FPS] times a second, and are drawn onto the
 * encoder's input surface; the encoder's output goes straight into the file. The guest's sound is not recorded yet.
 */
class ScreenRecorder(
    private val out: File,
    width: Int,
    height: Int,
    private val frame: (Bitmap) -> Boolean,
    private val log: (String) -> Unit,
) {
    // H.264 wants even sizes
    private val w = width and 1.inv()
    private val h = height and 1.inv()
    @Volatile private var live = false
    private var worker: Thread? = null

    val running: Boolean get() = live
    val file: File get() = out

    fun start(): Boolean {
        if (live) return true
        return runCatching {
            out.parentFile?.mkdirs()
            val fmt = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, w, h).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, (w * h * 6).coerceIn(1_000_000, 12_000_000))
                setInteger(MediaFormat.KEY_FRAME_RATE, FPS)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
            val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            codec.configure(fmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val surface = codec.createInputSurface()
            codec.start()
            val muxer = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            live = true
            worker = Thread({ run(codec, surface, muxer) }, "screen-rec").apply { start() }
            log("recording: started, ${w}x$h -> ${out.name}")
        }.onFailure { live = false; log("recording: cannot start: ${it.message}") }.isSuccess
    }

    /** Stops and waits for the file to be finished. */
    fun stop() {
        if (!live) return
        live = false
        runCatching { worker?.join(5000) }
        worker = null
    }

    private fun run(codec: MediaCodec, surface: Surface, muxer: MediaMuxer) {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val dst = Rect(0, 0, w, h)
        val info = MediaCodec.BufferInfo()
        var track = -1
        var frames = 0
        fun drain(end: Boolean) {
            var idle = 0
            while (true) {
                val i = codec.dequeueOutputBuffer(info, if (end) 10_000 else 0)
                when {
                    i == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> { track = muxer.addTrack(codec.outputFormat); muxer.start() }
                    i >= 0 -> {
                        val buf = codec.getOutputBuffer(i)
                        if (buf != null && track >= 0 && info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                            muxer.writeSampleData(track, buf, info)
                        }
                        codec.releaseOutputBuffer(i, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                    }
                    // nothing ready: back to drawing; at the end, wait for the encoder's last buffers (3 s at most)
                    else -> if (!end || ++idle > 300) return
                }
            }
        }
        try {
            val step = 1_000_000_000L / FPS
            var next = System.nanoTime()
            while (live) {
                if (frame(bmp)) {
                    val c = surface.lockHardwareCanvas()
                    try { c.drawBitmap(bmp, null, dst, null) } finally { surface.unlockCanvasAndPost(c) }
                    frames++
                }
                drain(false)
                next += step
                val wait = (next - System.nanoTime()) / 1_000_000
                if (wait > 0) Thread.sleep(wait) else next = System.nanoTime()
            }
            codec.signalEndOfInputStream()
            drain(true)
        } catch (t: Throwable) {
            log("recording: ${t.message ?: t}")
        } finally {
            live = false
            runCatching { codec.stop() }; runCatching { codec.release() }
            runCatching { if (track >= 0) muxer.stop() }; runCatching { muxer.release() }
            runCatching { surface.release() }
            log("recording: finished, $frames frames, ${out.length() / 1024} KB")
        }
    }

    companion object { const val FPS = 30 }
}
