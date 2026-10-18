package app.aemu.test;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.media.ToneGenerator;
import android.net.Uri;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.SpeechRecognizer;
import android.view.HapticFeedbackConstants;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.text.Collator;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Callable;

/**
 * Second half of the vendor-firmware test: the tester itself uses what vendors rewrite in the framework - its own
 * windows, components, storage, keystore, fonts, media, codecs, gralloc buffers, GPU - and every use is one
 * oem.api.<name> line (ok with what it saw, or FAIL with the exception). Waits are short: a function that does not
 * answer in seconds is a failure the firmware's user would see too.
 */
final class Oem2 {
    final Oem o;
    final Base m;
    final Context c;
    final int sdk = Build.VERSION.SDK_INT;

    Oem2(Oem o) { this.o = o; this.m = o.m; this.c = o.c; }

    /** runs f on the main thread and waits for it (UI objects may only be made there) */
    <T> T main(final Callable<T> f, long ms) throws Throwable {
        final Object[] r = new Object[2]; final Object lock = new Object();
        m.h.post(new Runnable() { public void run() {
            try { r[0] = f.call(); } catch (Throwable t) { r[1] = t; }
            synchronized (lock) { r[1] = r[1] == null ? "done" : r[1]; lock.notifyAll(); }
        } });
        synchronized (lock) { long end = SystemClock.elapsedRealtime() + ms; while (r[1] == null && SystemClock.elapsedRealtime() < end) lock.wait(100); }
        if (r[1] == null) throw new Exception("main thread busy " + ms + " ms");
        if (r[1] instanceof Throwable) throw (Throwable) r[1];
        @SuppressWarnings("unchecked") T t = (T) r[0];
        return t;
    }

    static boolean waitFor(Callable<Boolean> cond, long ms) throws Exception {
        long end = SystemClock.elapsedRealtime() + ms;
        while (SystemClock.elapsedRealtime() < end) {
            if (cond.call()) return true;
            synchronized (OemParts.lock) { OemParts.lock.wait(100); }
        }
        return cond.call();
    }

    void api(String n, Oem.Check k) { o.api(n, k); }

    void run() {
        fp(); fpConv(); fpFxCheck(); ui(); components(); storage(); security(); text(); media(); graphics(); misc();
    }

    /** 64-bit literals as the guest's runtime sees them (a double constant once read as 0 here) */
    static volatile double sink;
    void fp() {
        api("fp_constants", new Oem.Check() { public String run() throws Throwable {
            double a = 0.6, b = 0.1, c = 1.1, d = 3.141592653589793;
            float f = 0.6f;
            long l = 0x3FE3333333333333L, l2 = 0x123456789ABCDEF0L;
            double built = Double.longBitsToDouble(Long.parseLong("3FE3333333333333", 16));
            StringBuilder s = new StringBuilder(); int bad = 0;
            if (Double.doubleToRawLongBits(a) != 0x3FE3333333333333L) { bad++; s.append("0.6=").append(Long.toHexString(Double.doubleToRawLongBits(a))).append(' '); }
            if (Double.doubleToRawLongBits(b) != 0x3FB999999999999AL) { bad++; s.append("0.1=").append(Long.toHexString(Double.doubleToRawLongBits(b))).append(' '); }
            if (Double.doubleToRawLongBits(c) != 0x3FF199999999999AL) { bad++; s.append("1.1=").append(Long.toHexString(Double.doubleToRawLongBits(c))).append(' '); }
            if (Double.doubleToRawLongBits(d) != 0x400921FB54442D18L) { bad++; s.append("pi=").append(Long.toHexString(Double.doubleToRawLongBits(d))).append(' '); }
            if (Float.floatToRawIntBits(f) != 0x3F19999A) { bad++; s.append("0.6f=").append(Integer.toHexString(Float.floatToRawIntBits(f))).append(' '); }
            if (l != Long.parseLong("3FE3333333333333", 16)) { bad++; s.append("long1=").append(Long.toHexString(l)).append(' '); }
            if (l2 != Long.parseLong("123456789ABCDEF0", 16)) { bad++; s.append("long2=").append(Long.toHexString(l2)).append(' '); }
            double x = 938.74; sink = x * built;
            if (Math.abs(sink - 563.244) > 0.01) { bad++; s.append("mul_runtime=").append(sink).append(' '); }
            sink = x * a;
            if (Math.abs(sink - 563.244) > 0.01) { bad++; s.append("mul_const=").append(sink).append(' '); }
            if (bad > 0) throw new Exception(s.toString().trim());
            return null;
        } });
        // the same in a hot loop: the JIT compiles it (on-stack replacement) and loads 0.6 from the literal pool by its code
        api("fp_constants_jit", new Oem.Check() { public String run() throws Throwable {
            int bad = 0; double first = 0; long t = SystemClock.elapsedRealtime();
            for (int i = 0; i < 3000000; i++) {
                double out = 938.74 + (i & 1) * 0.0;
                double lim = out * 0.6;
                if (lim < 563.2 || lim > 563.3) { if (bad++ == 0) first = lim; }
            }
            if (bad > 0) throw new Exception(bad + " of 3000000 wrong, first " + first);
            return "3M in " + (SystemClock.elapsedRealtime() - t) + "ms";
        } });
        // the shape that failed in Fx.check: a method compiled while in its loop, calling one that loops over
        // Bitmap.getPixel (JNI), then multiplying by a constant the compiler keeps in a register across the call
        api("fp_after_jni_calls", new Oem.Check() { public String run() throws Throwable {
            android.graphics.Bitmap b = android.graphics.Bitmap.createBitmap(400, 400, android.graphics.Bitmap.Config.ARGB_8888);
            b.eraseColor(0xff336699);
            for (int y = 0; y < 400; y += 2) for (int x = 0; x < 400; x += 3) b.setPixel(x, y, 0xffffffff);
            String r = fpShape(b);
            if (r != null) throw new Exception(r);
            return null;
        } });
    }

    /** conversions and arithmetic, each in a method run once (interpreted, cold) and then hot (compiled) */
    static String conv(long sum, long sq, int cnt, int i, long big) {
        StringBuilder s = new StringBuilder();
        double a = sum / (double) cnt;                         // long -> double, divide
        if (Math.abs(a - 2.5) > 1e-12) s.append("l2d_div=").append(a).append(' ');
        double v = sq / (double) cnt - a * a;
        if (Math.abs(v - 1.25) > 1e-12) s.append("var=").append(v).append(' ');
        double c = i;                                          // int -> double
        if (c != 7.0) s.append("i2d=").append(c).append(' ');
        double e = (double) big;                               // long -> double, big
        if (e != 1.0e15) s.append("l2d_big=").append(e).append(' ');
        long f = (long) (e * 0.5);                             // double -> long
        if (f != 500000000000000L) s.append("d2l=").append(f).append(' ');
        int g = (int) (c * 1.5);                               // double -> int
        if (g != 10) s.append("d2i=").append(g).append(' ');
        float h = (float) a;                                   // double -> float
        if (h != 2.5f) s.append("d2f=").append(h).append(' ');
        double k = (float) 0.1f;                               // float -> double
        if (Double.doubleToRawLongBits(k) != 0x3FB99999A0000000L) s.append("f2d=").append(Long.toHexString(Double.doubleToRawLongBits(k))).append(' ');
        double m = Math.sqrt(v * 4), n2 = Math.floor(-2.5), o = Math.pow(2, 10), p = Math.sin(0.0) + Math.cos(0.0);
        if (m != Math.sqrt(5.0) || n2 != -3.0 || o != 1024.0 || p != 1.0) s.append("math=").append(m).append(',').append(n2).append(',').append(o).append(',').append(p).append(' ');
        long q = sum * 1000003L % 97;
        if (q != (10L * 1000003L) % 97) s.append("lmod=").append(q).append(' ');
        return s.length() == 0 ? null : s.toString().trim();
    }

    void fpConv() {
        api("fp_conversions", new Oem.Check() { public String run() throws Throwable {
            String cold = conv(10, 30, 4, 7, 1000000000000000L);
            String hot = null;
            for (int r = 0; r < 200000 && hot == null; r++) hot = conv(10, 30, 4, 7, 1000000000000000L);
            if (cold != null || hot != null) throw new Exception("cold: " + cold + " | hot: " + hot);
            return null;
        } });
    }

    void fpFxCheck() {
        final android.graphics.Bitmap b = android.graphics.Bitmap.createBitmap(1200, 2400, android.graphics.Bitmap.Config.ARGB_8888);
        b.eraseColor(0xff336699);
        for (int y = 0; y < 2400; y += 2) for (int x = 0; x < 1200; x += 3) if (y < 700 || y > 1400 || x < 200 || x > 1000) b.setPixel(x, y, 0xffffffff);
        final android.graphics.RectF g = new android.graphics.RectF(200, 700, 1000, 1400);
        api("fp_fx_check_bg", new Oem.Check() { public String run() throws Throwable { String r = Fx.check(b, null, g, 0.6); if (!r.startsWith("ok")) throw new Exception(r); return r; } });
        api("fp_fx_check_main", new Oem.Check() { public String run() throws Throwable {
            String r = main(new Callable<String>() { public String call() { return Fx.check(b, null, g, 0.6); } }, 20000);
            if (!r.startsWith("ok")) throw new Exception(r); return r;
        } });
    }

    static String fpShape(android.graphics.Bitmap b) {
        long sum = 0; int n = 0;
        for (int y = 0; y < b.getHeight(); y += 2) for (int x = 0; x < b.getWidth(); x += 2) { sum += b.getPixel(x, y) & 255; n++; }   // OSR here
        String bad = null;
        for (int round = 0; round < 20 && bad == null; round++) {
            double in = pixelDetail(b, true), out = pixelDetail(b, false);
            double lim = out * 0.6;
            if (Math.abs(lim - out * 3 / 5) > 1e-6 * Math.abs(out) + 1e-9) bad = "round " + round + ": out " + out + " * 0.6 = " + lim + " (in " + in + ", sum " + sum / Math.max(1, n) + ")";
        }
        return bad;
    }

    static double pixelDetail(android.graphics.Bitmap b, boolean left) {
        long s = 0; int n = 0;
        for (int y = 2; y < b.getHeight() - 2; y += 3) for (int x = 2; x < b.getWidth() - 2; x += 3) {
            if ((x < b.getWidth() / 2) != left) continue;
            int c = b.getPixel(x, y), d = b.getPixel(x + 1, y);
            s += Math.abs(((c >> 8) & 255) - ((d >> 8) & 255)) + Math.abs(((c >> 16) & 255) - ((d >> 16) & 255)); n++;
        }
        return n == 0 ? 0 : s * 100.0 / n;
    }

    // ---- windows ----
    void ui() {
        api("toast", new Oem.Check() { public String run() throws Throwable {
            return main(new Callable<String>() { public String call() { Toast.makeText(c, "aemu", Toast.LENGTH_SHORT).show(); return null; } }, 3000);
        } });
        api("dialog", new Oem.Check() { public String run() throws Throwable {
            return main(new Callable<String>() { public String call() {
                AlertDialog d = new AlertDialog.Builder(m).setTitle("aemu").setMessage("test").setPositiveButton("OK", null).create();
                d.show(); boolean s = d.isShowing(); d.dismiss(); return "showing " + s;
            } }, 3000);
        } });
        api("popup", new Oem.Check() { public String run() throws Throwable {
            return main(new Callable<String>() { public String call() {
                TextView t = new TextView(c); t.setText("aemu popup");
                PopupWindow p = new PopupWindow(t, 300, 100); p.showAtLocation(m.root, android.view.Gravity.CENTER, 0, 0);
                boolean s = p.isShowing(); p.dismiss(); return "showing " + s;
            } }, 3000);
        } });
        api("haptic", new Oem.Check() { public String run() throws Throwable {
            return main(new Callable<String>() { public String call() { return "performed " + m.root.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); } }, 3000);
        } });
        api("window_insets", new Oem.Check() { public String run() throws Throwable {
            return main(new Callable<String>() { public String call() throws Exception {
                Object in = m.root.getRootWindowInsets();
                String cut = "";
                if (sdk >= 28 && in != null) cut = " cutout " + (in.getClass().getMethod("getDisplayCutout").invoke(in) != null);
                return "insets " + (in != null) + cut + (sdk >= 24 ? " multiwindow " + m.isInMultiWindowMode() : "");
            } }, 3000);
        } });
    }

    // ---- the tester's own components through the system ----
    void components() {
        api("broadcast", new Oem.Check() { public String run() throws Throwable {
            final String[] got = new String[1];
            BroadcastReceiver r = new BroadcastReceiver() { public void onReceive(Context x, Intent i) { got[0] = i.getStringExtra("v"); OemParts.signal(); } };
            c.registerReceiver(r, new IntentFilter("app.aemu.test.BC"));
            try {
                c.sendBroadcast(new Intent("app.aemu.test.BC").setPackage(c.getPackageName()).putExtra("v", "x"));
                if (!waitFor(new Callable<Boolean>() { public Boolean call() { return got[0] != null; } }, 4000)) throw new Exception("not delivered in 4 s");
            } finally { c.unregisterReceiver(r); }
            return null;
        } });
        api("service_start", new Oem.Check() { public String run() throws Throwable {
            OemParts.serviceStarted = null;
            c.startService(new Intent(c, OemParts.Svc.class).setAction("plain"));
            if (!waitFor(new Callable<Boolean>() { public Boolean call() { return OemParts.serviceStarted != null; } }, 5000)) throw new Exception("not started in 5 s");
            return null;
        } });
        api("service_bind", new Oem.Check() { public String run() throws Throwable {
            final IBinder[] b = new IBinder[1];
            ServiceConnection sc = new ServiceConnection() {
                public void onServiceConnected(ComponentName n, IBinder s) { b[0] = s; OemParts.signal(); }
                public void onServiceDisconnected(ComponentName n) { }
            };
            if (!c.bindService(new Intent(c, OemParts.Svc.class), sc, Context.BIND_AUTO_CREATE)) throw new Exception("bindService false");
            try { if (!waitFor(new Callable<Boolean>() { public Boolean call() { return b[0] != null; } }, 5000)) throw new Exception("not connected in 5 s"); }
            finally { c.unbindService(sc); }
            return "alive " + b[0].isBinderAlive();
        } });
        api("service_foreground", new Oem.Check() { public String run() throws Throwable {
            if (sdk < 26) return "n/a";
            OemParts.serviceStarted = null;
            Context.class.getMethod("startForegroundService", Intent.class).invoke(c, new Intent(c, OemParts.Svc.class).setAction("fg"));
            if (!waitFor(new Callable<Boolean>() { public Boolean call() { return OemParts.serviceStarted != null; } }, 5000)) throw new Exception("not started in 5 s");
            if (!"fg".equals(OemParts.serviceStarted)) throw new Exception(OemParts.serviceStarted);
            return null;
        } });
        api("job", new Oem.Check() { public String run() throws Throwable {
            OemParts.jobStarted = null;
            JobScheduler js = (JobScheduler) c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            int r = js.schedule(new JobInfo.Builder(77, new ComponentName(c, OemParts.Job.class)).setOverrideDeadline(500).build());
            if (r != JobScheduler.RESULT_SUCCESS) throw new Exception("schedule " + r);
            if (!waitFor(new Callable<Boolean>() { public Boolean call() { return OemParts.jobStarted != null; } }, 8000)) { js.cancel(77); throw new Exception("not run in 8 s"); }
            return null;
        } });
        api("alarm_fires", new Oem.Check() { public String run() throws Throwable {
            final boolean[] got = new boolean[1];
            BroadcastReceiver r = new BroadcastReceiver() { public void onReceive(Context x, Intent i) { got[0] = true; OemParts.signal(); } };
            c.registerReceiver(r, new IntentFilter("app.aemu.test.ALARM2"));
            AlarmManager a = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            PendingIntent pi = PendingIntent.getBroadcast(c, 1, new Intent("app.aemu.test.ALARM2").setPackage(c.getPackageName()), sdk >= 23 ? 0x04000000 : 0);
            long t = SystemClock.elapsedRealtime();
            try {
                try { a.setExact(AlarmManager.ELAPSED_REALTIME, t + 1000, pi); } catch (SecurityException e) { a.set(AlarmManager.ELAPSED_REALTIME, t + 1000, pi); }
                if (!waitFor(new Callable<Boolean>() { public Boolean call() { return got[0]; } }, 8000)) throw new Exception("not fired in 8 s");
            } finally { a.cancel(pi); c.unregisterReceiver(r); }
            return "after " + (SystemClock.elapsedRealtime() - t) + " ms";
        } });
        api("own_provider", new Oem.Check() { public String run() throws Throwable {
            Uri u = Uri.parse("content://app.aemu.test.oem/x");
            ContentValues v = new ContentValues(); v.put("v", "aemu-prov");
            c.getContentResolver().insert(u, v);
            Cursor q = c.getContentResolver().query(u, null, null, null, null);
            q.moveToFirst(); String s = q.getString(0); q.close();
            if (!"aemu-prov".equals(s)) throw new Exception("read back " + s);
            return null;
        } });
    }

    // ---- storage ----
    void storage() {
        api("prefs", new Oem.Check() { public String run() throws Throwable {
            SharedPreferences p = c.getSharedPreferences("oem", 0);
            if (!p.edit().putString("k", "v1").commit()) throw new Exception("commit false");
            return "read " + p.getString("k", null);
        } });
        api("sqlite", new Oem.Check() { public String run() throws Throwable {
            SQLiteDatabase db = c.openOrCreateDatabase("oem.db", 0, null);
            try {
                db.execSQL("create table if not exists t(a integer)"); db.execSQL("delete from t");
                db.beginTransaction(); for (int i = 0; i < 1000; i++) db.execSQL("insert into t values(" + i + ")"); db.setTransactionSuccessful(); db.endTransaction();
                Cursor q = db.rawQuery("select sum(a) from t", null); q.moveToFirst(); long s = q.getLong(0); q.close();
                if (s != 499500) throw new Exception("sum " + s);
            } finally { db.close(); }
            return null;
        } });
        api("files_internal", new Oem.Check() { public String run() throws Throwable { return rw(new File(c.getFilesDir(), "oem.bin")); } });
        api("files_external_app", new Oem.Check() { public String run() throws Throwable {
            File d = c.getExternalFilesDir(null); if (d == null) throw new Exception("no external app dir"); return rw(new File(d, "oem.bin"));
        } });
        api("media_store_insert", new Oem.Check() { public String run() throws Throwable {
            if (sdk < 29) return "n/a (needs storage permission before 10)";
            ContentValues v = new ContentValues();
            v.put(MediaStore.MediaColumns.DISPLAY_NAME, "aemu-oem.png"); v.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
            v.put("relative_path", Environment.DIRECTORY_PICTURES + "/aemu");
            Uri u = c.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
            if (u == null) throw new Exception("insert null");
            java.io.OutputStream out = c.getContentResolver().openOutputStream(u);
            android.graphics.Bitmap.createBitmap(8, 8, android.graphics.Bitmap.Config.ARGB_8888).compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out); out.close();
            int n = c.getContentResolver().delete(u, null, null);
            return "deleted " + n;
        } });
        api("download_manager", new Oem.Check() { public String run() throws Throwable {
            DownloadManager dm = (DownloadManager) c.getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Request r = new DownloadManager.Request(Uri.parse("https://www.google.com/robots.txt"));
            r.setDestinationInExternalFilesDir(c, null, "oem-dl.txt");
            final long id = dm.enqueue(r);
            final DownloadManager d2 = dm; final int[] st = { 0 };
            try {
                waitFor(new Callable<Boolean>() { public Boolean call() {
                    Cursor q = d2.query(new DownloadManager.Query().setFilterById(id));
                    if (q != null && q.moveToFirst()) st[0] = q.getInt(q.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    if (q != null) q.close();
                    return st[0] == DownloadManager.STATUS_SUCCESSFUL || st[0] == DownloadManager.STATUS_FAILED;
                } }, 15000);
            } finally { dm.remove(id); }
            if (st[0] != DownloadManager.STATUS_SUCCESSFUL) throw new Exception("status " + st[0]);
            return null;
        } });
    }

    static String rw(File f) throws Exception {
        byte[] b = new byte[1 << 20]; for (int i = 0; i < b.length; i++) b[i] = (byte) i;
        long t = System.nanoTime();
        FileOutputStream out = new FileOutputStream(f); out.write(b); out.getFD().sync(); out.close();
        byte[] r = new byte[b.length]; FileInputStream in = new FileInputStream(f); int n = 0; while (n < r.length) { int k = in.read(r, n, r.length - n); if (k < 0) break; n += k; } in.close();
        f.delete();
        if (n != b.length || r[12345] != b[12345]) throw new Exception("read back " + n);
        return "1MB " + (System.nanoTime() - t) / 1000000 + "ms";
    }

    // ---- keystore, wake locks ----
    void security() {
        api("wakelock", new Oem.Check() { public String run() throws Throwable {
            PowerManager.WakeLock w = ((PowerManager) c.getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "aemu:oem");
            w.acquire(); boolean h = w.isHeld(); w.release(); return "held " + h;
        } });
        api("keystore_ec", new Oem.Check() { public String run() throws Throwable {
            KeyPairGenerator g = KeyPairGenerator.getInstance("EC", "AndroidKeyStore");
            Class<?> sb = Class.forName("android.security.keystore.KeyGenParameterSpec$Builder");
            Object b = sb.getConstructor(String.class, int.class).newInstance("aemu-oem-ec", 4 | 8);
            sb.getMethod("setDigests", String[].class).invoke(b, (Object) new String[] { "SHA-256" });
            g.initialize((java.security.spec.AlgorithmParameterSpec) sb.getMethod("build").invoke(b));
            java.security.KeyPair kp = g.generateKeyPair();
            Signature s = Signature.getInstance("SHA256withECDSA"); s.initSign(kp.getPrivate()); s.update(new byte[] { 1, 2, 3 }); byte[] sig = s.sign();
            Signature v = Signature.getInstance("SHA256withECDSA"); v.initVerify(kp.getPublic()); v.update(new byte[] { 1, 2, 3 });
            boolean ok = v.verify(sig);
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore"); ks.load(null); ks.deleteEntry("aemu-oem-ec");
            if (!ok) throw new Exception("signature does not verify");
            return null;
        } });
        api("keystore_aes", new Oem.Check() { public String run() throws Throwable {
            javax.crypto.KeyGenerator g = javax.crypto.KeyGenerator.getInstance("AES", "AndroidKeyStore");
            Class<?> sb = Class.forName("android.security.keystore.KeyGenParameterSpec$Builder");
            Object b = sb.getConstructor(String.class, int.class).newInstance("aemu-oem-aes", 1 | 2);
            sb.getMethod("setBlockModes", String[].class).invoke(b, (Object) new String[] { "GCM" });
            sb.getMethod("setEncryptionPaddings", String[].class).invoke(b, (Object) new String[] { "NoPadding" });
            g.init((java.security.spec.AlgorithmParameterSpec) sb.getMethod("build").invoke(b));
            javax.crypto.SecretKey k = g.generateKey();
            javax.crypto.Cipher ci = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding"); ci.init(javax.crypto.Cipher.ENCRYPT_MODE, k);
            byte[] enc = ci.doFinal("aemu".getBytes("UTF-8"));
            javax.crypto.Cipher de = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding"); de.init(javax.crypto.Cipher.DECRYPT_MODE, k, new javax.crypto.spec.GCMParameterSpec(128, ci.getIV()));
            String back = new String(de.doFinal(enc), "UTF-8");
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore"); ks.load(null); ks.deleteEntry("aemu-oem-aes");
            if (!"aemu".equals(back)) throw new Exception("decrypted " + back);
            return null;
        } });
    }

    // ---- fonts and locale data (vendors ship their own fonts) ----
    void text() {
        api("fonts", new Oem.Check() { public String run() throws Throwable {
            Paint p = new Paint(); p.setTextSize(40);
            // (hasGlyph takes one grapheme: one letter of each script)
            String[][] s = { { "latin", "H" }, { "cyrillic", "Ж" }, { "cjk", "中" }, { "kana", "か" }, { "hangul", "한" },
                { "arabic", "ع" }, { "hebrew", "ע" }, { "devanagari", "ह" }, { "thai", "ภ" }, { "emoji", "😀" } };
            StringBuilder b = new StringBuilder(); int missing = 0;
            for (String[] e : s) {
                boolean has = sdk >= 23 ? (Boolean) Paint.class.getMethod("hasGlyph", String.class).invoke(p, e[1]) : p.measureText(e[1]) > 0;
                if (!has) { missing++; b.append(e[0]).append(' '); }
            }
            if (missing > 0) throw new Exception("no glyphs: " + b.toString().trim());
            return "all " + s.length + " scripts";
        } });
        api("icu", new Oem.Check() { public String run() throws Throwable {
            String d = DateFormat.getDateInstance(DateFormat.LONG, new Locale("ru")).format(new Date(0));
            String n = NumberFormat.getCurrencyInstance(Locale.JAPAN).format(1234.5);
            int cmp = Collator.getInstance(new Locale("de")).compare("ä", "b");
            return d + " | " + n + " | collate " + cmp + " | zones " + TimeZone.getAvailableIDs().length + " default " + TimeZone.getDefault().getID();
        } });
    }

    // ---- media: players, tones, the vendor's codecs ----
    File wav() throws Exception {
        File f = new File(c.getCacheDir(), "oem.wav"); int rate = 22050, n = rate / 4;
        ByteBuffer b = ByteBuffer.allocate(44 + n * 2).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        b.put("RIFF".getBytes()).putInt(36 + n * 2).put("WAVEfmt ".getBytes()).putInt(16).putShort((short) 1).putShort((short) 1)
            .putInt(rate).putInt(rate * 2).putShort((short) 2).putShort((short) 16).put("data".getBytes()).putInt(n * 2);
        for (int i = 0; i < n; i++) b.putShort((short) (Math.sin(i * 2 * Math.PI * 440 / rate) * 3000));
        FileOutputStream o = new FileOutputStream(f); o.write(b.array()); o.close();
        return f;
    }

    void media() {
        api("media_player", new Oem.Check() { public String run() throws Throwable {
            MediaPlayer p = new MediaPlayer();
            try {
                p.setDataSource(wav().getPath()); p.prepare(); int d = p.getDuration(); p.setVolume(0.05f, 0.05f); p.start();
                SystemClock.sleep(300); boolean playing = p.isPlaying(); int pos = p.getCurrentPosition();
                return "duration " + d + " playing " + playing + " pos " + pos;
            } finally { p.release(); }
        } });
        api("soundpool", new Oem.Check() { public String run() throws Throwable {
            final SoundPool sp = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
            final int[] st = { -1 };
            sp.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() { public void onLoadComplete(SoundPool s, int id, int status) { st[0] = status; OemParts.signal(); } });
            try {
                int id = sp.load(wav().getPath(), 1);
                if (!waitFor(new Callable<Boolean>() { public Boolean call() { return st[0] >= 0; } }, 4000)) throw new Exception("not loaded in 4 s");
                if (st[0] != 0) throw new Exception("load status " + st[0]);
                return "stream " + sp.play(id, 0.05f, 0.05f, 1, 0, 1f);
            } finally { sp.release(); }
        } });
        api("tone", new Oem.Check() { public String run() throws Throwable {
            ToneGenerator t = new ToneGenerator(AudioManager.STREAM_MUSIC, 5);
            try { return "started " + t.startTone(ToneGenerator.TONE_PROP_BEEP, 100); } finally { SystemClock.sleep(150); t.release(); }
        } });
        api("audio_record", new Oem.Check() { public String run() throws Throwable {
            if (sdk >= 23 && c.checkSelfPermission("android.permission.RECORD_AUDIO") != 0) return "n/a (no microphone permission)";
            int min = android.media.AudioRecord.getMinBufferSize(16000, 16, 2);
            android.media.AudioRecord r = new android.media.AudioRecord(1, 16000, 16, 2, Math.max(min, 8192));
            try {
                if (r.getState() != 1) throw new Exception("state " + r.getState());
                r.startRecording(); short[] b = new short[4000]; int n = r.read(b, 0, b.length); r.stop();
                return "read " + n;
            } finally { r.release(); }
        } });
        for (final String mime : new String[] { "video/avc", "video/hevc", "video/x-vnd.on2.vp9" }) {
            api("decoder_" + mime.substring(6), new Oem.Check() { public String run() throws Throwable { return codec(mime, false); } });
        }
        api("encoder_avc", new Oem.Check() { public String run() throws Throwable { return codec("video/avc", true); } });
    }

    /** the best codec for mime (a vendor's own one before the platform's software one): configured and started */
    String codec(String mime, boolean enc) throws Exception {
        String name = null;
        for (MediaCodecInfo i : new MediaCodecList(MediaCodecList.ALL_CODECS).getCodecInfos()) {
            if (i.isEncoder() != enc) continue;
            for (String t : i.getSupportedTypes()) if (t.equalsIgnoreCase(mime)) {
                boolean sw = i.getName().startsWith("OMX.google.") || i.getName().startsWith("c2.android.");
                if (name == null || (!sw && (name.startsWith("OMX.google.") || name.startsWith("c2.android.")))) name = i.getName();
            }
        }
        if (name == null) return "none";
        MediaCodec cd = MediaCodec.createByCodecName(name);
        try {
            MediaFormat f = MediaFormat.createVideoFormat(mime, 320, 240);
            ImageReader ir = null;
            if (enc) {
                f.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
                f.setInteger(MediaFormat.KEY_BIT_RATE, 500000); f.setInteger(MediaFormat.KEY_FRAME_RATE, 30); f.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
                cd.configure(f, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                cd.createInputSurface().release();
            } else {
                ir = ImageReader.newInstance(320, 240, PixelFormat.RGBA_8888, 2);
                try { cd.configure(f, ir.getSurface(), null, 0); } catch (Throwable t) { cd.configure(f, null, null, 0); }
            }
            long t = SystemClock.elapsedRealtime();
            cd.start(); cd.stop();
            if (ir != null) ir.close();
            return name + " start/stop " + (SystemClock.elapsedRealtime() - t) + "ms";
        } finally { cd.release(); }
    }

    // ---- graphics: gralloc buffers, the GPU through a window of a consumer ----
    void graphics() {
        api("hardware_buffer", new Oem.Check() { public String run() throws Throwable {
            if (sdk < 26) return "n/a";
            Class<?> hb = Class.forName("android.hardware.HardwareBuffer");
            Object b = hb.getMethod("create", int.class, int.class, int.class, int.class, long.class).invoke(null, 256, 256, 1, 1, 0x100L | 0x200L | 0x33L);
            String r = "w " + hb.getMethod("getWidth").invoke(b);
            hb.getMethod("close").invoke(b);
            return r;
        } });
        api("gpu_to_image_reader", new Oem.Check() { public String run() throws Throwable {
            ImageReader ir = ImageReader.newInstance(64, 64, PixelFormat.RGBA_8888, 2);
            EGLDisplay d = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
            int[] v = new int[2]; EGL14.eglInitialize(d, v, 0, v, 1);
            EGLConfig[] cf = new EGLConfig[1]; int[] n = new int[1];
            EGL14.eglChooseConfig(d, new int[] { EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8, EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, 4, EGL14.EGL_NONE }, 0, cf, 0, 1, n, 0);
            EGLContext ctx = EGL14.eglCreateContext(d, cf[0], EGL14.EGL_NO_CONTEXT, new int[] { 0x3098, 2, EGL14.EGL_NONE }, 0);
            EGLSurface s = EGL14.eglCreateWindowSurface(d, cf[0], ir.getSurface(), new int[] { EGL14.EGL_NONE }, 0);
            if (s == EGL14.EGL_NO_SURFACE) throw new Exception("window surface 0x" + Integer.toHexString(EGL14.eglGetError()));
            try {
                EGL14.eglMakeCurrent(d, s, s, ctx);
                GLES20.glClearColor(1, 0, 0, 1); GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                EGL14.eglSwapBuffers(d, s);
                Image im = null;
                for (int i = 0; i < 30 && im == null; i++) { im = ir.acquireLatestImage(); if (im == null) SystemClock.sleep(50); }
                if (im == null) throw new Exception("no image reached the reader");
                ByteBuffer px = im.getPlanes()[0].getBuffer();
                int r = px.get(0) & 0xff, g = px.get(1) & 0xff, b = px.get(2) & 0xff;
                im.close();
                if (r < 200 || g > 50 || b > 50) throw new Exception("pixel " + r + "," + g + "," + b + " (wanted red)");
                return "red arrived";
            } finally {
                EGL14.eglMakeCurrent(d, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
                EGL14.eglDestroySurface(d, s); EGL14.eglDestroyContext(d, ctx); ir.close();
            }
        } });
        api("gles3", new Oem.Check() { public String run() throws Throwable {
            EGLDisplay d = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
            int[] v = new int[2]; EGL14.eglInitialize(d, v, 0, v, 1);
            int[] n = new int[1]; EGL14.eglGetConfigs(d, new EGLConfig[256], 0, 256, n, 0); // (6.0 rejects a null array)
            EGLConfig[] cf = new EGLConfig[1]; int[] k = new int[1];
            EGL14.eglChooseConfig(d, new int[] { EGL14.EGL_RENDERABLE_TYPE, 0x40, EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT, EGL14.EGL_NONE }, 0, cf, 0, 1, k, 0);
            if (k[0] == 0) return "configs " + n[0] + ", no ES3 config";
            EGLContext ctx = EGL14.eglCreateContext(d, cf[0], EGL14.EGL_NO_CONTEXT, new int[] { 0x3098, 3, EGL14.EGL_NONE }, 0);
            if (ctx == EGL14.EGL_NO_CONTEXT) throw new Exception("ES3 context 0x" + Integer.toHexString(EGL14.eglGetError()));
            EGLSurface s = EGL14.eglCreatePbufferSurface(d, cf[0], new int[] { EGL14.EGL_WIDTH, 4, EGL14.EGL_HEIGHT, 4, EGL14.EGL_NONE }, 0);
            EGL14.eglMakeCurrent(d, s, s, ctx);
            String ver = GLES20.glGetString(GLES20.GL_VERSION);
            EGL14.eglMakeCurrent(d, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroySurface(d, s); EGL14.eglDestroyContext(d, ctx);
            return "configs " + n[0] + " | " + ver;
        } });
        api("renderscript", new Oem.Check() { public String run() throws Throwable {
            android.renderscript.RenderScript rs = android.renderscript.RenderScript.create(c);
            try {
                android.renderscript.Allocation a = android.renderscript.Allocation.createSized(rs, android.renderscript.Element.I32(rs), 16);
                int[] in = new int[16]; for (int i = 0; i < 16; i++) in[i] = i * 3;
                a.copyFrom(in); int[] out = new int[16]; a.copyTo(out); a.destroy();
                if (out[15] != 45) throw new Exception("copy " + out[15]);
                return null;
            } finally { rs.destroy(); }
        } });
    }

    // ---- launcher, permissions, radios, voice, standby ----
    void misc() {
        api("shortcuts", new Oem.Check() { public String run() throws Throwable {
            if (sdk < 25) return "n/a";
            Object sm = c.getSystemService(Class.forName("android.content.pm.ShortcutManager"));
            String r = "max " + sm.getClass().getMethod("getMaxShortcutCountPerActivity").invoke(sm);
            if (sdk >= 26) r += " pin " + sm.getClass().getMethod("isRequestPinShortcutSupported").invoke(sm);
            return r;
        } });
        api("app_widgets", new Oem.Check() { public String run() throws Throwable { return "providers " + AppWidgetManager.getInstance(c).getInstalledProviders().size(); } });
        api("app_ops", new Oem.Check() { public String run() throws Throwable {
            AppOpsManager a = (AppOpsManager) c.getSystemService(Context.APP_OPS_SERVICE);
            return "camera " + a.checkOpNoThrow("android:camera", android.os.Process.myUid(), c.getPackageName());
        } });
        api("sms_manager", new Oem.Check() { public String run() throws Throwable {
            return android.telephony.SmsManager.getDefault() == null ? "none" : "present";
        } });
        api("ble", new Oem.Check() { public String run() throws Throwable {
            android.bluetooth.BluetoothAdapter b = android.bluetooth.BluetoothAdapter.getDefaultAdapter();
            if (b == null) return "no adapter";
            return "le " + c.getPackageManager().hasSystemFeature("android.hardware.bluetooth_le") + " scanner " + (b.getBluetoothLeScanner() != null);
        } });
        api("biometric", new Oem.Check() { public String run() throws Throwable {
            if (sdk < 29) return "n/a";
            Object bm = c.getSystemService(Class.forName("android.hardware.biometrics.BiometricManager"));
            return "can_authenticate " + bm.getClass().getMethod("canAuthenticate").invoke(bm);
        } });
        api("speech", new Oem.Check() { public String run() throws Throwable { return "available " + SpeechRecognizer.isRecognitionAvailable(c); } });
        api("standby_bucket", new Oem.Check() { public String run() throws Throwable {
            if (sdk < 28) return "n/a";
            Object u = c.getSystemService(Class.forName("android.app.usage.UsageStatsManager"));
            return "bucket " + u.getClass().getMethod("getAppStandbyBucket").invoke(u);
        } });
        api("brightness", new Oem.Check() { public String run() throws Throwable {
            return "level " + Settings.System.getInt(c.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, -1)
                + " auto " + Settings.System.getInt(c.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, -1);
        } });
        api("memory_class", new Oem.Check() { public String run() throws Throwable {
            android.app.ActivityManager am = (android.app.ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
            return "class " + am.getMemoryClass() + " large " + am.getLargeMemoryClass() + " low_ram " + am.isLowRamDevice();
        } });
        api("network_validated", new Oem.Check() { public String run() throws Throwable {
            if (sdk < 23) return "n/a";
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
            Object net = cm.getClass().getMethod("getActiveNetwork").invoke(cm);
            if (net == null) throw new Exception("no active network");
            android.net.NetworkCapabilities nc = cm.getNetworkCapabilities((android.net.Network) net);
            boolean v = nc.hasCapability(16);
            if (!v) throw new Exception("not validated");
            return null;
        } });
        api("dns_many", new Oem.Check() { public String run() throws Throwable {
            StringBuilder b = new StringBuilder(); int bad = 0;
            for (String h : new String[] { "www.google.com", "www.wikipedia.org", "api.github.com", "cdn.jsdelivr.net", "ya.ru", "vk.com" }) {
                try { java.net.InetAddress.getByName(h); } catch (Throwable t) { bad++; b.append(h).append(' '); }
            }
            if (bad > 0) throw new Exception("unresolved " + b.toString().trim());
            return null;
        } });
    }
}
