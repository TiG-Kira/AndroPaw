package app.aemu.test;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * What the three testers of AEmulator's guest firmwares share (A: Android 1.5-4.4, B: 5.0-8.1, C: 9 and later):
 * the run of tests one after another, the report (files/report.txt in the program's data directory, key=value
 * lines, "done=1" last, the same lines in the log under AEMU_TEST) and the tests that every Android can run.
 * Nothing here may need more than API 3: what is newer sits in classes of its own (Gl2Pattern, the programs'
 * Main), loaded only where the API is there.
 * A test that the Android at hand cannot run writes "<test>=n/a".
 * Extras: "only" - a comma separated list of tests to run; "secs" - seconds per timed test (default 5).
 */
public abstract class Base extends Activity {
    static final String TAG = "AEMU_TEST";
    static final int SDK = sdk();
    final Handler h = new Handler(Looper.getMainLooper());
    FrameLayout root;
    TextView status;
    FileOutputStream out;
    int secs = 5;
    String only = "";
    String[] tests;
    int idx = -1;
    String cur = "start";

    // the main thread's longest stall and the count of stalls over 100 ms, per test
    volatile long stallMax, stallCount, pingAt;
    volatile long testAt; volatile String stallWhere;
    volatile boolean alive = true;

    static int sdk() { try { return Integer.parseInt(Build.VERSION.SDK); } catch (Throwable t) { return 3; } }

    /** the names of this program's tests, in the order they run */
    abstract String[] tests();
    /** starts a test of the program's own; false when the name is not one of them */
    abstract boolean run(String name) throws Throwable;
    /** which of the three programs this is */
    abstract String program();

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (getIntent() != null) {
            secs = getIntent().getIntExtra("secs", 5);
            String o = getIntent().getStringExtra("only");
            if (o != null) only = "," + o + ",";
        }
        tests = tests();
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        status = new TextView(this);
        status.setTextColor(Color.WHITE);
        status.setTextSize(12);
        status.setBackgroundColor(0x80000000);
        setContentView(root);
        root.addView(status, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
        try { out = new FileOutputStream(new File(getFilesDir(), "report.txt")); } catch (Exception e) { Log.e(TAG, "report", e); }
        Thread w = new Thread(new Runnable() { public void run() { watch(); } }, "stall-watch");
        w.setDaemon(true);
        w.start();
        line("program", program());
        try { line("tester.installed", new java.util.Date(getPackageManager().getPackageInfo(getPackageName(), 0).lastUpdateTime)); } catch (Throwable t) { }
        h.postDelayed(new Runnable() { public void run() { next(); } }, 800);
    }

    @Override protected void onDestroy() { alive = false; super.onDestroy(); }

    void watch() {
        final Runnable pong = new Runnable() { public void run() { pingAt = 0; } };
        while (alive) {
            if (pingAt == 0) { pingAt = SystemClock.uptimeMillis(); h.post(pong); }
            SystemClock.sleep(20);
            long at = pingAt;
            if (at != 0) {
                long d = SystemClock.uptimeMillis() - at;
                if (d > stallMax) stallMax = d;
            } else continue;
            if (SystemClock.uptimeMillis() - at > 100) {       // one stall, however long: wait for its end
                if (stallWhere == null) {                         // where the main thread is during the first stall
                    StringBuilder b = new StringBuilder("+" + (at - testAt) + "ms");
                    StackTraceElement[] st = getMainLooper().getThread().getStackTrace();
                    for (int k = 0; k < st.length && k < 8; k++) b.append(" < ").append(st[k].getClassName().replaceAll(".*[.]", "")).append('.').append(st[k].getMethodName()).append(':').append(st[k].getLineNumber());
                    stallWhere = b.toString();
                }
                while (alive && pingAt != 0) { SystemClock.sleep(10); long d = SystemClock.uptimeMillis() - at; if (d > stallMax) stallMax = d; }
                stallCount++;
            }
        }
    }

    synchronized void line(String k, Object v) {
        String s = k + "=" + v;
        Log.i(TAG, s);
        try { if (out != null) { out.write((s + "\n").getBytes("UTF-8")); out.flush(); } } catch (Exception e) { }
    }

    void say(final String s) { h.post(new Runnable() { public void run() { status.setText(s); } }); }

    /** ends the current test (from any thread) and starts the next one */
    void done() {
        h.post(new Runnable() { public void run() {
            line(cur + ".stall_max_ms", stallMax);
            line(cur + ".stalls", stallCount);
            if (stallWhere != null) line(cur + ".stall_where", stallWhere);
            next();
        } });
    }

    void next() {
        for (int i = root.getChildCount() - 1; i >= 0; i--) if (root.getChildAt(i) != status) root.removeViewAt(i);
        do idx++; while (idx < tests.length && only.length() > 0 && only.indexOf("," + tests[idx] + ",") < 0);
        stallMax = 0; stallCount = 0; stallWhere = null; testAt = SystemClock.uptimeMillis();
        if (idx >= tests.length) {
            line("mem.native_kb", Debug.getNativeHeapAllocatedSize() >> 10);
            line("mem.java_kb", (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) >> 10);
            line("done", 1);
            say("done - report.txt");
            return;
        }
        cur = tests[idx];
        say("test " + (idx + 1) + "/" + tests.length + ": " + cur);
        line("test", cur);
        try {
            if (run(cur)) return;
            if (cur.equals("device")) device();
            else if (cur.equals("cpu")) bg(new Runnable() { public void run() { try { cpu(); } catch (Exception e) { line("cpu.error", e); } } });
            else if (cur.equals("disk")) bg(new Runnable() { public void run() { try { disk(); } catch (Exception e) { line("disk.error", e); } } });
            else if (cur.equals("net")) bg(new Runnable() { public void run() { net(); } });
            else if (cur.equals("audio")) bg(new Runnable() { public void run() { audio(); } });
            else if (cur.equals("sensors")) sensors();
            else if (cur.equals("canvas")) bg(new Runnable() { public void run() { canvas(); } });
            else if (cur.equals("pattern")) pattern();
            else if (cur.equals("ui")) ui();
            else if (cur.equals("gl1")) gl1();
            else if ((cur.equals("gl2") || cur.equals("gl3")) && SDK >= 8) Gl2.start(this, cur.equals("gl3"));
            else if (cur.equals("glcheck") && SDK >= 17) GlCheck.start(this);
            else if (cur.equals("camera1")) camera1();
            else if (cur.equals("webview")) webview();
            else { line(cur, "n/a"); done(); }
        } catch (Throwable t) {
            line(cur + ".error", t);
            done();
        }
    }

    void bg(final Runnable r) {
        new Thread(new Runnable() { public void run() {
            try { r.run(); } catch (Throwable t) { line(cur + ".error", t); }
            done();
        } }, "test-" + cur).start();
    }

    /** frame times in ms: count, average rate, percentiles, the longest, frames over two and over six periods */
    void frames(String k, long[] ns, int n, float hz) {
        if (n < 2) { line(k + ".frames", n); return; }
        double[] ms = new double[n - 1];
        for (int i = 1; i < n; i++) ms[i - 1] = (ns[i] - ns[i - 1]) / 1e6;
        double total = (ns[n - 1] - ns[0]) / 1e6, period = 1000.0 / (hz > 1 ? hz : 60);
        int jank = 0, freeze = 0;
        for (int i = 0; i < ms.length; i++) { if (ms[i] > period * 2) jank++; if (ms[i] > period * 6) freeze++; }
        Arrays.sort(ms);
        line(k + ".frames", n);
        line(k + ".fps", f1((n - 1) * 1000.0 / total));
        line(k + ".ms_p50", f1(ms[ms.length / 2]));
        line(k + ".ms_p95", f1(ms[(int) (ms.length * 0.95)]));
        line(k + ".ms_p99", f1(ms[(int) (ms.length * 0.99)]));
        line(k + ".ms_max", f1(ms[ms.length - 1]));
        line(k + ".jank", jank);
        line(k + ".freezes", freeze);
    }

    /** one decimal, with a point whatever the guest's language is */
    static String f1(double v) { long t = Math.round(v * 10); return (t / 10) + "." + Math.abs(t % 10); }

    float hz() { try { return getWindowManager().getDefaultDisplay().getRefreshRate(); } catch (Throwable t) { return 60; } }

    /** a static field of Build that older Androids lack */
    static String build(String field) {
        try { return String.valueOf(Build.class.getField(field).get(null)); } catch (Throwable t) { return "?"; }
    }

    static boolean hwAccel(View v) {
        try { return ((Boolean) View.class.getMethod("isHardwareAccelerated").invoke(v)).booleanValue(); } catch (Throwable t) { return false; }
    }

    void device() {
        line("device.sdk", SDK);
        line("device.release", Build.VERSION.RELEASE);
        line("device.model", build("MANUFACTURER") + " " + Build.MODEL);
        line("device.fingerprint", Build.FINGERPRINT);
        line("device.abis", SDK >= 21 ? Arrays.toString((Object[]) fieldOf("SUPPORTED_ABIS")) : build("CPU_ABI") + "," + build("CPU_ABI2"));
        line("device.cores", Runtime.getRuntime().availableProcessors());
        android.util.DisplayMetrics m = getResources().getDisplayMetrics();
        line("device.screen", m.widthPixels + "x" + m.heightPixels + " density " + m.density + " hz " + hz());
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        line("device.ram_free_mb", mi.availMem >> 20);
        line("device.gles", Integer.toHexString(am.getDeviceConfigurationInfo().reqGlEsVersion));
        line("device.hw_accel", hwAccel(root));
        line("device.heap_mb", Runtime.getRuntime().maxMemory() >> 20);
        done();
    }

    static Object fieldOf(String field) { try { return Build.class.getField(field).get(null); } catch (Throwable t) { return new Object[0]; } }

    static double spin(int n) {
        double x = 0; long a = 1;
        for (int i = 1; i <= n; i++) { a = a * 6364136223846793005L + 1442695040888963407L; x += Math.sqrt((a >>> 11) & 0xffff) / i; }
        return x;
    }

    void cpu() throws Exception {
        final int n = 20000000;
        spin(200000);                      // let the JIT see it
        long t = SystemClock.elapsedRealtime();
        spin(n);
        long one = SystemClock.elapsedRealtime() - t;
        line("cpu.single_ms", one);
        int c = Runtime.getRuntime().availableProcessors();
        Thread[] th = new Thread[c];
        t = SystemClock.elapsedRealtime();
        for (int i = 0; i < c; i++) { th[i] = new Thread(new Runnable() { public void run() { spin(n); } }); th[i].start(); }
        for (int i = 0; i < c; i++) th[i].join();
        long all = SystemClock.elapsedRealtime() - t;
        line("cpu.multi_ms", all);
        line("cpu.scaling", f1(one * (double) c / Math.max(1, all)));
        byte[] buf = new byte[4 << 20];
        t = SystemClock.elapsedRealtime();
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
        md.update(buf); md.digest();
        line("cpu.sha1_4mb_ms", SystemClock.elapsedRealtime() - t);
        byte[] to = new byte[buf.length];
        t = SystemClock.elapsedRealtime();
        for (int i = 0; i < 16; i++) System.arraycopy(buf, 0, to, 0, buf.length);
        line("cpu.memcpy_64mb_ms", SystemClock.elapsedRealtime() - t);
    }

    void disk() throws Exception {
        File f = new File(getFilesDir(), "disk.tmp");
        byte[] mb = new byte[1 << 20];
        for (int i = 0; i < mb.length; i++) mb[i] = (byte) (i * 31);
        long t = SystemClock.elapsedRealtime();
        FileOutputStream o = new FileOutputStream(f);
        for (int i = 0; i < 32; i++) o.write(mb);
        o.getFD().sync();
        o.close();
        long w = SystemClock.elapsedRealtime() - t;
        t = SystemClock.elapsedRealtime();
        FileInputStream in = new FileInputStream(f);
        long got = 0; int r, bad = 0;
        while ((r = in.read(mb)) > 0) { got += r; if (mb[0] != 0 || mb[1] != 31) bad++; }
        in.close();
        long rd = SystemClock.elapsedRealtime() - t;
        line("disk.write_mb_s", Math.round(32000.0 / Math.max(1, w)));
        line("disk.read_mb_s", Math.round(32000.0 / Math.max(1, rd)));
        line("disk.read_ok", got == 32L << 20);
        t = SystemClock.elapsedRealtime();
        for (int i = 0; i < 200; i++) { File s = new File(getFilesDir(), "s" + i); FileOutputStream so = new FileOutputStream(s); so.write(1); so.close(); s.delete(); }
        line("disk.small_files_200_ms", SystemClock.elapsedRealtime() - t);
        f.delete();
        File sd = android.os.Environment.getExternalStorageDirectory();
        line("disk.sd_state", android.os.Environment.getExternalStorageState());
        try {
            android.os.StatFs st = new android.os.StatFs(sd.getPath());
            line("disk.sd_free_mb", (long) st.getAvailableBlocks() * st.getBlockSize() >> 20);
        } catch (Throwable e) { line("disk.sd_error", e); }
    }

    void net() {
        try {
            long t = SystemClock.elapsedRealtime();
            InetAddress a = InetAddress.getByName("www.google.com");
            line("net.dns_ms", SystemClock.elapsedRealtime() - t);
            line("net.dns", a.getHostAddress());
        } catch (Throwable e) { line("net.dns_error", e); }
        String[] urls = { "https://www.google.com/generate_204", "http://connectivitycheck.gstatic.com/generate_204" };
        for (int i = 0; i < urls.length; i++) {
            String k = urls[i].startsWith("https") ? "net.https" : "net.http";
            try {
                long t = SystemClock.elapsedRealtime();
                HttpURLConnection c = (HttpURLConnection) new URL(urls[i]).openConnection();
                c.setConnectTimeout(8000); c.setReadTimeout(8000);
                line(k + "_code", c.getResponseCode());
                line(k + "_ms", SystemClock.elapsedRealtime() - t);
                c.disconnect();
            } catch (Throwable e) { line(k + "_error", e); }
        }
        try {       // a plain TCP connection, whatever the TLS of this Android can do
            long t = SystemClock.elapsedRealtime();
            java.net.Socket s = new java.net.Socket();
            s.connect(new java.net.InetSocketAddress("1.1.1.1", 80), 8000);
            line("net.tcp_ms", SystemClock.elapsedRealtime() - t);
            s.close();
        } catch (Throwable e) { line("net.tcp_error", e); }
    }

    /** a second of a 440 Hz tone: how long the output takes to accept it and how far the play head got */
    void audio() {
        int rate = 44100, min = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
        line("audio.min_buffer", min);
        AudioTrack t = new AudioTrack(AudioManager.STREAM_MUSIC, rate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(min, 8192), AudioTrack.MODE_STREAM);
        line("audio.state", t.getState());
        short[] s = new short[rate];
        for (int i = 0; i < s.length; i++) s[i] = (short) (Math.sin(i * 2 * Math.PI * 440 / rate) * 6000);
        t.play();
        long at = SystemClock.elapsedRealtime();
        int w = t.write(s, 0, s.length);
        line("audio.written", w);
        line("audio.write_ms", SystemClock.elapsedRealtime() - at);     // about 1000 less the buffer when the output drains in real time
        SystemClock.sleep(400);
        line("audio.head", t.getPlaybackHeadPosition());
        Object u = null;
        try { u = AudioTrack.class.getMethod("getUnderrunCount").invoke(t); } catch (Throwable e) { }
        line("audio.underruns", u == null ? "n/a" : u);
        t.stop(); t.release();
    }

    void sensors() {
        final SensorManager sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        line("sensors.count", sm.getSensorList(Sensor.TYPE_ALL).size());
        final Sensor acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (acc == null) { line("sensors.accelerometer", "none"); done(); return; }
        final int[] n = { 0 };
        final float[] last = new float[3];
        final SensorEventListener l = new SensorEventListener() {
            public void onSensorChanged(SensorEvent e) { n[0]++; System.arraycopy(e.values, 0, last, 0, 3); }
            public void onAccuracyChanged(Sensor s, int a) { }
        };
        sm.registerListener(l, acc, SensorManager.SENSOR_DELAY_GAME);
        h.postDelayed(new Runnable() { public void run() {
            sm.unregisterListener(l);
            line("sensors.accel_events_2s", n[0]);
            line("sensors.accel_last", Arrays.toString(last));
            done();
        } }, 2000);
    }

    // ---- pictures that can be checked ----

    /** the 16 colours of the test pattern, 4 x 4 cells, row by row from the top left; the host's check knows them too */
    static final int[] PATTERN = {
        0xffff0000, 0xff00ff00, 0xff0000ff, 0xffffff00,
        0xffff00ff, 0xff00ffff, 0xffffffff, 0xff808080,
        0xffff8000, 0xff8000ff, 0xff00ff80, 0xff804000,
        0xff0080ff, 0xffff0080, 0xff80ff00, 0xff404040,
    };

    static boolean near(int a, int b, int tol) {
        return Math.abs(Color.red(a) - Color.red(b)) <= tol && Math.abs(Color.green(a) - Color.green(b)) <= tol && Math.abs(Color.blue(a) - Color.blue(b)) <= tol;
    }

    static void drawPattern(Canvas c, int w, int h, Paint p) {
        p.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 16; i++) {
            p.setColor(PATTERN[i]);
            c.drawRect((i % 4) * w / 4f, (i / 4) * h / 4f, (i % 4 + 1) * w / 4f, (i / 4 + 1) * h / 4f, p);
        }
    }

    /** one check of the canvas test: the pixel at a place against the colour it must have */
    void want(String name, Bitmap b, int x, int y, int color, int tol, int[] bad) {
        int got = b.getPixel(x, y);
        if (!near(got, color, tol)) { bad[0]++; line("canvas.bad." + name, x + "," + y + " is " + Integer.toHexString(got) + ", must be " + Integer.toHexString(color)); }
    }

    /** the drawing calls of Canvas into a bitmap (the software renderer), each checked at control points */
    void canvas() {
        int[] bad = { 0 };
        Bitmap b = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint p = new Paint();
        c.drawColor(0xff102030);
        want("fill", b, 5, 5, 0xff102030, 0, bad);
        p.setColor(0xffff0000); c.drawRect(10, 10, 60, 60, p);
        want("rect", b, 30, 30, 0xffff0000, 0, bad); want("rect_edge", b, 61, 30, 0xff102030, 0, bad);
        p.setColor(0xff00ff00); p.setAntiAlias(true); c.drawCircle(100, 40, 25, p);
        want("circle", b, 100, 40, 0xff00ff00, 0, bad); want("circle_out", b, 100 + 24, 40 + 24, 0xff102030, 0, bad);
        p.setColor(0xff0000ff); c.drawRoundRect(new RectF(140, 10, 240, 70), 20, 20, p);
        want("round", b, 190, 40, 0xff0000ff, 0, bad); want("round_corner", b, 141, 11, 0xff102030, 0, bad);
        p.setColor(0xffffffff); p.setStrokeWidth(5); c.drawLine(10, 90, 240, 90, p);
        want("line", b, 120, 90, 0xffffffff, 0, bad);
        Path path = new Path(); path.moveTo(30, 110); path.lineTo(60, 170); path.lineTo(0, 170); path.close();
        p.setColor(0xffffff00); c.drawPath(path, p);
        want("path", b, 30, 150, 0xffffff00, 0, bad);
        p.setShader(new LinearGradient(80, 0, 240, 0, 0xff000000, 0xffffffff, Shader.TileMode.CLAMP));
        c.drawRect(80, 110, 240, 170, p);
        p.setShader(null);
        want("gradient_left", b, 82, 140, 0xff030303, 6, bad); want("gradient_mid", b, 160, 140, 0xff808080, 6, bad); want("gradient_right", b, 238, 140, 0xfffcfcfc, 6, bad);
        p.setColor(0x80ff0000); c.drawRect(0, 180, 256, 256, p);        // half red over the fill: 136,16,24
        want("alpha", b, 250, 250, 0xff881018, 3, bad);
        c.save(); c.clipRect(100, 190, 140, 230); c.drawColor(0xff00ffff); c.restore();
        want("clip_in", b, 120, 210, 0xff00ffff, 0, bad); want("clip_out", b, 150, 210, 0xff881018, 3, bad);
        c.save(); c.rotate(90, 200, 220); p.setColor(0xffff00ff); c.drawRect(190, 200, 210, 240, p); c.restore();   // becomes 180..220 x 210..230
        want("rotate", b, 183, 220, 0xffff00ff, 0, bad); want("rotate_out", b, 200, 203, 0xff881018, 3, bad);
        p.setColor(0xffffffff); p.setTextSize(40); c.drawText("Ag", 10, 240, p);
        int lit = 0;
        for (int y = 200; y < 250; y++) for (int x = 10; x < 70; x++) if (near(b.getPixel(x, y), 0xffffffff, 40)) lit++;
        line("canvas.text_pixels", lit);
        if (lit < 100 || lit > 2000) { bad[0]++; line("canvas.bad.text", lit); }
        line("canvas.text_width", f1(p.measureText("The quick brown fox")));
        Bitmap small = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888);
        Canvas sc = new Canvas(small);
        drawPattern(sc, 4, 4, new Paint());
        Bitmap big = Bitmap.createScaledBitmap(small, 64, 64, false);
        for (int i = 0; i < 16; i++) if (!near(big.getPixel((i % 4) * 16 + 8, (i / 4) * 16 + 8), PATTERN[i], 0)) { bad[0]++; line("canvas.bad.scale", i); }
        Bitmap s565 = big.copy(Bitmap.Config.RGB_565, false);
        if (s565 == null || !near(s565.getPixel(8, 8), PATTERN[0], 8)) { bad[0]++; line("canvas.bad.rgb565", s565 == null ? "null" : Integer.toHexString(s565.getPixel(8, 8))); }
        line("canvas.failed", bad[0]);
        line("canvas.ok", bad[0] == 0);
    }

    /**
     * The test pattern on the screen, drawn the ways programs draw: a View (the window's renderer), a GLES 1 surface
     * and, where there is one, a GLES 2 surface. Each stage stays for 3 s and is announced in the report
     * ("pattern.stage=<name> <uptime ms>"): the host takes a screenshot meanwhile and looks for the 16 colours
     * (gtp.sh). The GL stages also read their own picture back and compare.
     */
    void pattern() {
        final String[] stages = SDK >= 8 ? new String[] { "view", "text", "textlayer", "texthw", "gl1", "gl2" } : new String[] { "view", "text", "gl1" };
        final int[] at = { -1 };
        final Runnable[] step = new Runnable[1];
        step[0] = new Runnable() { public void run() {
            for (int i = root.getChildCount() - 1; i >= 0; i--) if (root.getChildAt(i) != status) root.removeViewAt(i);
            if (++at[0] >= stages.length) { done(); return; }
            String s = stages[at[0]];
            View v;
            if (s.equals("view")) {
                v = new View(Base.this) {
                    final Paint p = new Paint();
                    @Override protected void onDraw(Canvas c) { drawPattern(c, getWidth(), getHeight(), p); }
                };
            } else if (s.equals("texthw")) {
                // a launcher's app list: icons that are hardware bitmaps (26+), each with a dark label under it
                Bitmap icon = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888);
                drawPattern(new Canvas(icon), 48, 48, new Paint());
                Bitmap hw = SDK >= 26 ? Api21.hwBitmap(icon) : null;
                final Bitmap draw = hw != null ? hw : icon;
                line("pattern.texthw_hardware", hw != null);
                v = new View(Base.this) {
                    final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                    @Override protected void onDraw(Canvas c) {
                        c.drawColor(0xffffffff);
                        p.setTextSize(22); p.setColor(0x8a000000);
                        for (int r = 0; r < 4; r++) for (int k = 0; k < 4; k++) {
                            float x = 10 + k * 130, y = 10 + r * 110;
                            c.drawBitmap(draw, x + 30, y, null);
                            c.drawText("Calendar", x, y + 80, p);
                        }
                    }
                };
            } else if (s.startsWith("text")) {
                // what a launcher does: labels with a shadow first (the desktop), then the same typeface without one
                // in dark colours and several sizes (the app list). The host counts the dark pixels (gtp.py).
                v = new View(Base.this) {
                    final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                    @Override protected void onDraw(Canvas c) {
                        c.drawColor(0xffffffff);
                        p.setColor(0xffffffff); p.setTextSize(28); p.setShadowLayer(4, 0, 2, 0xc0000000);
                        c.drawText("Calendar Camera Clock Gallery", 10, 40, p);
                        p.clearShadowLayer();
                        float y = 80;
                        for (int sz = 12; sz <= 40 && y < getHeight(); sz += 4) {
                            p.setTextSize(sz); p.setColor(sz % 8 == 0 ? 0xff202124 : 0x8a000000);
                            c.drawText("Calendar Camera Clock Gallery", 10, y, p);
                            y += sz + 10;
                        }
                    }
                };
            } else {
                GLSurfaceView g = new GLSurfaceView(Base.this);
                if (s.equals("gl2")) Gl2Pattern.attach(Base.this, g); else g.setRenderer(new Gl1Pattern());
                v = g;
            }
            // (the upper five eighths: the status line and the system's bars stay clear of it)
            root.addView(v, 0, new FrameLayout.LayoutParams(-1, getWindowManager().getDefaultDisplay().getHeight() * 5 / 8, Gravity.TOP));
            if (s.equals("textlayer") && SDK >= 21) Api21.hwLayer(v);
            line("pattern.hw_accel." + s, hwAccel(v));
            h.postDelayed(new Runnable() { public void run() { line("pattern.stage", stages[at[0]] + " " + SystemClock.uptimeMillis()); } }, 700);
            h.postDelayed(step[0], 3700);
        } };
        step[0].run();
    }

    /** compares what a GL stage read back (RGBA, w x h, bottom row first) with the pattern */
    void checkRead(String stage, ByteBuffer px, int w, int h) {
        int bad = 0;
        for (int i = 0; i < 16; i++) {
            int x = (i % 4) * w / 4 + w / 8, y = h - 1 - ((i / 4) * h / 4 + h / 8), o = (y * w + x) * 4;
            int got = Color.rgb(px.get(o) & 0xff, px.get(o + 1) & 0xff, px.get(o + 2) & 0xff);
            if (!near(got, PATTERN[i], 10)) { bad++; line("pattern.bad." + stage, "cell " + i + " is " + Integer.toHexString(got) + ", must be " + Integer.toHexString(PATTERN[i])); }
        }
        line("pattern.read." + stage, bad == 0 ? "ok" : bad + " cells differ");
    }

    static FloatBuffer floats(float[] v) {
        FloatBuffer b = ByteBuffer.allocateDirect(v.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        b.put(v).position(0);
        return b;
    }

    /** the 16 cells as triangles: positions (x, y) and colours (r, g, b, a) in -1..1 */
    static float[][] cells() {
        float[] pos = new float[16 * 12], col = new float[16 * 24];
        for (int i = 0; i < 16; i++) {
            float x0 = (i % 4) / 2f - 1, x1 = x0 + 0.5f, y1 = 1 - (i / 4) / 2f, y0 = y1 - 0.5f;
            float[] q = { x0, y0, x1, y0, x0, y1, x1, y0, x1, y1, x0, y1 };
            System.arraycopy(q, 0, pos, i * 12, 12);
            for (int k = 0; k < 6; k++) {
                col[i * 24 + k * 4] = Color.red(PATTERN[i]) / 255f; col[i * 24 + k * 4 + 1] = Color.green(PATTERN[i]) / 255f;
                col[i * 24 + k * 4 + 2] = Color.blue(PATTERN[i]) / 255f; col[i * 24 + k * 4 + 3] = 1;
            }
        }
        return new float[][] { pos, col };
    }

    /** where a fixed-function colour goes wrong: a current colour without arrays, a white texture modulated by grey and
     *  by white; each reads the middle pixel back (expect ffffff, 808080, ffffff) */
    void probes(GL10 gl, int w, int h) {
        FloatBuffer quad = floats(new float[] { -1, -1, 1, -1, -1, 1, 1, 1 });
        FloatBuffer uv = floats(new float[] { 0, 0, 1, 0, 0, 1, 1, 1 });
        gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, quad);
        ByteBuffer px = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder());
        int[] tex = new int[1];
        gl.glGenTextures(1, tex, 0);
        gl.glBindTexture(GL10.GL_TEXTURE_2D, tex[0]);
        ByteBuffer white = ByteBuffer.allocateDirect(4 * 4 * 4);
        for (int i = 0; i < 64; i++) white.put((byte) 0xff);
        white.position(0);
        gl.glTexImage2D(GL10.GL_TEXTURE_2D, 0, GL10.GL_RGBA, 4, 4, 0, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, white);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);
        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, uv);
        String[] names = { "color4f_white", "texture_grey", "texture_white", "color4ub_white", "color4f_0999" };
        for (int k = 0; k < names.length; k++) {
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
            boolean t = k == 1 || k == 2;
            if (t) { gl.glEnable(GL10.GL_TEXTURE_2D); gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY); }
            if (k == 1) gl.glColor4f(0.5f, 0.5f, 0.5f, 1); else if (k == 3) ((javax.microedition.khronos.opengles.GL11) gl).glColor4ub((byte) 255, (byte) 255, (byte) 255, (byte) 255);
            else if (k == 4) gl.glColor4f(0.999f, 0.999f, 0.999f, 1); else gl.glColor4f(1, 1, 1, 1);
            gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
            if (t) { gl.glDisable(GL10.GL_TEXTURE_2D); gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY); }
            gl.glReadPixels(w / 2, h / 2, 1, 1, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, px);
            line("pattern.gl1_probe." + names[k], String.format("%02x%02x%02x", px.get(0) & 0xff, px.get(1) & 0xff, px.get(2) & 0xff));
        }
        gl.glDeleteTextures(1, tex, 0);
    }

    class Gl1Pattern implements GLSurfaceView.Renderer {
        int w, h, n;
        final float[][] c = cells();
        final FloatBuffer pos = floats(c[0]), col = floats(c[1]);
        public void onSurfaceCreated(GL10 gl, EGLConfig cfg) { line("pattern.gl1_renderer", gl.glGetString(GL10.GL_RENDERER)); }
        public void onSurfaceChanged(GL10 gl, int w, int h) { this.w = w; this.h = h; gl.glViewport(0, 0, w, h); }
        public void onDrawFrame(GL10 gl) {
            gl.glClearColor(0, 0, 0, 1);
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
            gl.glDisable(GL10.GL_DITHER);
            gl.glMatrixMode(GL10.GL_PROJECTION); gl.glLoadIdentity();
            gl.glMatrixMode(GL10.GL_MODELVIEW); gl.glLoadIdentity();
            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY); gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
            gl.glVertexPointer(2, GL10.GL_FLOAT, 0, pos); gl.glColorPointer(4, GL10.GL_FLOAT, 0, col);
            gl.glDrawArrays(GL10.GL_TRIANGLES, 0, 96);
            if (++n == 10) {
                ByteBuffer px = ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder());
                gl.glReadPixels(0, 0, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, px);
                checkRead("gl1", px, w, h);
                probes(gl, w, h);
                int e = gl.glGetError();
                if (e != 0) line("pattern.gl1_error", "0x" + Integer.toHexString(e));
            }
        }
    }

    // ---- smoothness ----

    /** a list scrolled by hand-made frames: text, rounded rectangles, alpha - what a launcher or Settings draws */
    void ui() {
        final long[] ts = new long[secs * 400];
        final int[] n = { 0 };
        final float hz = hz();
        final long end = SystemClock.uptimeMillis() + secs * 1000L;
        final boolean[] ended = { false };
        final View v = new View(this) {
            final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            final RectF r = new RectF();
            @Override protected void onDraw(Canvas c) {
                if (n[0] < ts.length) ts[n[0]++] = System.nanoTime();
                float t = SystemClock.uptimeMillis() / 1000f, rowH = getHeight() / 14f, off = (t * 900) % rowH;
                for (int i = -1; i < 16; i++) {
                    float y = i * rowH - off;
                    p.setColor(0xff202838 + ((i & 3) << 18));
                    r.set(16, y + 6, getWidth() - 16, y + rowH - 6);
                    c.drawRoundRect(r, 24, 24, p);
                    p.setColor(0xffe0e0ff); p.setTextSize(rowH * 0.32f);
                    c.drawText("Row " + i + " - the quick brown fox jumps", 40, y + rowH * 0.45f, p);
                    p.setColor(0x90ffffff); p.setTextSize(rowH * 0.22f);
                    c.drawText("secondary line of text, 0123456789", 40, y + rowH * 0.78f, p);
                    p.setColor(0x8040c0ff);
                    c.drawCircle(getWidth() - 80, y + rowH / 2, rowH * 0.3f, p);
                }
                if (SystemClock.uptimeMillis() < end) invalidate();
                else if (!ended[0]) {
                    ended[0] = true;
                    line("ui.hw_accel", hwAccel(this));
                    line("ui.clock", "draw");           // frames are counted where they are drawn
                    frames("ui", ts, n[0], hz);
                    done();
                }
            }
        };
        root.addView(v, 0, new FrameLayout.LayoutParams(-1, -1));
    }

    /** a lit, rotating, textured GLES 1 scene: fixed-function matrices, lighting, fog; timed like the others */
    void gl1() {
        final long[] ts = new long[secs * 400];
        final float hz = hz();
        GLSurfaceView sv = new GLSurfaceView(this);
        sv.setRenderer(new GLSurfaceView.Renderer() {
            int n, w, h; long t0; boolean ended;
            final FloatBuffer pos = floats(new float[] { -1, -1, 0, 1, -1, 0, -1, 1, 0, 1, 1, 0 });
            final FloatBuffer nor = floats(new float[] { 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1 });
            public void onSurfaceCreated(GL10 gl, EGLConfig c) {
                line("gl1.vendor", gl.glGetString(GL10.GL_VENDOR));
                line("gl1.renderer", gl.glGetString(GL10.GL_RENDERER));
                line("gl1.version", gl.glGetString(GL10.GL_VERSION));
                gl.glEnable(GL10.GL_DEPTH_TEST); gl.glEnable(GL10.GL_LIGHTING); gl.glEnable(GL10.GL_LIGHT0); gl.glEnable(GL10.GL_COLOR_MATERIAL);
                gl.glEnable(GL10.GL_FOG); gl.glFogf(GL10.GL_FOG_MODE, GL10.GL_LINEAR); gl.glFogf(GL10.GL_FOG_START, 4); gl.glFogf(GL10.GL_FOG_END, 14);
                gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, floats(new float[] { 0.3f, 0.5f, 1, 0 }));
            }
            public void onSurfaceChanged(GL10 gl, int w, int h) {
                this.w = w; this.h = h;
                gl.glViewport(0, 0, w, h);
                gl.glMatrixMode(GL10.GL_PROJECTION); gl.glLoadIdentity();
                float a = w / (float) Math.max(1, h);
                gl.glFrustumf(-a, a, -1, 1, 2, 20);
                gl.glMatrixMode(GL10.GL_MODELVIEW);
                line("gl1.surface", w + "x" + h);
            }
            public void onDrawFrame(GL10 gl) {
                long now = System.nanoTime();
                if (n == 0) t0 = now;
                if (n < ts.length) ts[n++] = now;
                float t = (now - t0) / 1e9f;
                gl.glClearColor(0.05f, 0.07f, 0.12f, 1);
                gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
                gl.glEnableClientState(GL10.GL_VERTEX_ARRAY); gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
                gl.glVertexPointer(3, GL10.GL_FLOAT, 0, pos); gl.glNormalPointer(GL10.GL_FLOAT, 0, nor);
                for (int i = 0; i < 100; i++) {
                    gl.glLoadIdentity();
                    gl.glTranslatef((i % 10 - 4.5f) * 0.9f, (i / 10 - 4.5f) * 0.9f, -8 - (i % 3));
                    gl.glRotatef(t * 90 + i * 7, 0.3f, 1, 0.2f);
                    gl.glScalef(0.35f, 0.35f, 0.35f);
                    gl.glColor4f((i % 5) / 4f, (i % 7) / 6f, 1 - (i % 4) / 4f, 1);
                    gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
                }
                if (!ended && now - t0 > secs * 1000000000L) {
                    ended = true;
                    ByteBuffer px = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder());
                    gl.glReadPixels(2, 2, 1, 1, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, px);
                    line("gl1.corner_pixel", (px.get(0) & 0xff) + "," + (px.get(1) & 0xff) + "," + (px.get(2) & 0xff));       // the clear colour: 13,18,31
                    int e = gl.glGetError();
                    line("gl1.gl_error", "0x" + Integer.toHexString(e));
                    frames("gl1", ts, n, hz);
                    done();
                }
            }
        });
        root.addView(sv, 0, new FrameLayout.LayoutParams(-1, -1));
    }

    // ---- camera, the first API: preview into a SurfaceView, frames to a callback ----

    @SuppressWarnings("deprecation")
    void camera1() {
        final SurfaceView sv = new SurfaceView(this);
        sv.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        sv.getHolder().addCallback(new SurfaceHolder.Callback() {
            public void surfaceCreated(final SurfaceHolder holder) {
                new Thread(new Runnable() { public void run() {
                    try { preview(holder); } catch (Throwable t) { line("camera1.error", t); }
                    done();
                } }, "test-camera1").start();
            }
            public void surfaceChanged(SurfaceHolder holder, int f, int w, int h) { }
            public void surfaceDestroyed(SurfaceHolder holder) { }
        });
        root.addView(sv, 0, new FrameLayout.LayoutParams(-1, getWindowManager().getDefaultDisplay().getHeight() * 5 / 8, Gravity.TOP));
    }

    @SuppressWarnings("deprecation")
    void preview(SurfaceHolder holder) throws Exception {
        long t = SystemClock.elapsedRealtime();
        android.hardware.Camera cam = android.hardware.Camera.open();
        if (cam == null) { line("camera1.error", "no camera"); return; }
        line("camera1.open_ms", SystemClock.elapsedRealtime() - t);
        try {
            android.hardware.Camera.Size sz = cam.getParameters().getPreviewSize();
            line("camera1.preview", sz.width + "x" + sz.height);
            final int[] n = { 0, 0 };                   // frames, frames that differ from the one before
            final long[] luma = { 0, 255, 0, -1 };      // mean, min, max of the last frame's Y plane; the sum before
            final int pixels = sz.width * sz.height;
            cam.setPreviewDisplay(holder);
            cam.setPreviewCallback(new android.hardware.Camera.PreviewCallback() {
                public void onPreviewFrame(byte[] d, android.hardware.Camera c) {
                    n[0]++;
                    long sum = 0; int lo = 255, hi = 0, m = Math.min(pixels, d.length);
                    for (int i = 0; i < m; i += 16) { int y = d[i] & 0xff; sum += y; if (y < lo) lo = y; if (y > hi) hi = y; }
                    if (sum != luma[3]) n[1]++;
                    luma[0] = sum / Math.max(1, m / 16); luma[1] = lo; luma[2] = hi; luma[3] = sum;
                }
            });
            cam.startPreview();
            line("camera1.stage", "preview " + SystemClock.uptimeMillis());
            SystemClock.sleep(3000);
            line("camera1.frames_3s", n[0]);
            line("camera1.changed_frames", n[1]);       // a sensor's noise makes every frame differ; 1 = a frozen picture
            line("camera1.luma_mean", luma[0]);
            line("camera1.luma_min", luma[1]);
            line("camera1.luma_max", luma[2]);          // min = max: a flat frame, nothing from a sensor
            cam.setPreviewCallback(null);
            final boolean[] shot = { false };
            final int[] size = { 0 };
            t = SystemClock.elapsedRealtime();
            cam.takePicture(null, null, new android.hardware.Camera.PictureCallback() {
                public void onPictureTaken(byte[] d, android.hardware.Camera c) {
                    size[0] = d == null ? 0 : d.length;
                    if (d != null && d.length > 3) size[0] = (d[0] & 0xff) == 0xff && (d[1] & 0xff) == 0xd8 ? d.length : -d.length;
                    synchronized (shot) { shot[0] = true; shot.notifyAll(); }
                }
            });
            synchronized (shot) { if (!shot[0]) shot.wait(6000); }
            line("camera1.jpeg_bytes", shot[0] ? String.valueOf(size[0]) : "no picture in 6 s");       // negative: not a JPEG
            line("camera1.jpeg_ms", SystemClock.elapsedRealtime() - t);
            cam.stopPreview();
        } finally {
            cam.release();
        }
    }

    // ---- WebView the way every Android has it: a page, script, a canvas animation; the answer comes by the title ----

    void webview() {
        final long t0 = SystemClock.elapsedRealtime();
        final boolean[] ended = { false };
        final Runnable end = new Runnable() { public void run() { if (!ended[0]) { ended[0] = true; done(); } } };
        final WebView w = new WebView(this);
        line("webview.created_ms", SystemClock.elapsedRealtime() - t0);
        w.getSettings().setJavaScriptEnabled(true);
        line("webview.ua", w.getSettings().getUserAgentString());
        w.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView v, String url) {
                if (url.startsWith("javascript:")) return;
                line("webview.loaded_ms", SystemClock.elapsedRealtime() - t0);
                h.postDelayed(new Runnable() { public void run() {
                    w.loadUrl("javascript:document.title='R:frames '+window.n+' sum '+window.s+' gl '+window.gl");
                    h.postDelayed(new Runnable() { public void run() {
                        String t = w.getTitle();
                        line("webview.js", t != null && t.startsWith("R:") ? t.substring(2) : "no answer (" + t + ")");
                        end.run();
                    } }, 500);
                } }, 3000);
            }
        });
        root.addView(w, 0, new FrameLayout.LayoutParams(-1, -1));
        // script: a sum (the engine runs), an animated canvas counted in frames (the page is drawn), a WebGL context
        w.loadDataWithBaseURL("http://aemu.test/", "<html><head><title>t</title></head><body style='background:#123;color:#fff'><h2>WebView</h2><canvas id=c width=300 height=300></canvas><script>"
            + "window.s=0;for(var i=0;i<1e6;i++)window.s+=i%7;window.n=0;var x=document.getElementById('c').getContext('2d');"
            + "var raf=window.requestAnimationFrame||window.webkitRequestAnimationFrame||function(f){setTimeout(f,16)};"
            + "function f(){window.n++;x.fillStyle='rgb('+(window.n*5%256)+',80,200)';x.fillRect(0,0,300,300);raf(f)}f();"
            + "try{var g=document.createElement('canvas').getContext('webgl');window.gl=g?g.getParameter(g.VERSION):'none'}catch(e){window.gl='error'}"
            + "</script></body></html>", "text/html", "utf-8", null);
        h.postDelayed(new Runnable() { public void run() { if (!ended[0]) { line("webview.error", "timeout 20 s"); end.run(); } } }, 20000);
    }
}
