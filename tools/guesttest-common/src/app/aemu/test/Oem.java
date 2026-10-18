package app.aemu.test;

import android.accounts.AccountManager;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.WallpaperManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.display.DisplayManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaDrm;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.StatFs;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.telephony.TelephonyManager;
import android.view.Display;
import android.view.accessibility.AccessibilityManager;
import android.view.inputmethod.InputMethodManager;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What vendor firmwares add or rewrite, checked without knowing which vendor it is (API 21+): every binder service
 * answers a ping, every content provider's process comes up, every permission file's library is there, the
 * standard intents have someone to handle them, and the platform services vendors reimplement (notifications,
 * clipboard, alarms, TTS, codecs, DRM, GPU...) work. Vendor, skin and SoC are only reported (oem.skin, oem.soc,
 * oem.gpu), nothing here depends on them. Lines: oem.<section>.<item>; failures are listed by name.
 */
final class Oem {
    final Base m;
    final Context c;
    final PackageManager pm;
    final long t0 = SystemClock.elapsedRealtime();
    static final long BUDGET_MS = 45000;      // the whole test; providers and services stop early when it is spent
    final ExecutorService pool = Executors.newCachedThreadPool();   // (a hung call keeps its thread)
    int apiOk, apiFail;

    Oem(Base m) { this.m = m; this.c = m; this.pm = m.getPackageManager(); }

    static void run(Base m) {
        Oem o = new Oem(m);
        try {
            o.props(); o.services(); o.packages(); o.providers(); o.permissionFiles(); o.features();
            o.settings(); o.intents(); o.apis();
            new Oem2(o).run();
        } finally { o.pool.shutdownNow(); }
        m.line("oem.api.ok", o.apiOk);
        m.line("oem.api.fail", o.apiFail);
        m.line("oem.ms", SystemClock.elapsedRealtime() - o.t0);
    }

    boolean late() { return SystemClock.elapsedRealtime() - t0 > BUDGET_MS; }

    /** runs f on the pool, gives up after ms (a vendor service or provider may never answer) */
    <T> T timed(Callable<T> f, long ms) throws Exception {
        Future<T> r = pool.submit(f);
        try { return r.get(ms, TimeUnit.MILLISECONDS); }
        catch (java.util.concurrent.TimeoutException e) { r.cancel(true); throw new Exception("timeout " + ms + " ms"); }
        catch (java.util.concurrent.ExecutionException e) { throw new Exception(String.valueOf(e.getCause())); }
    }

    static String join(List<String> l, int max) {
        StringBuilder b = new StringBuilder();
        for (String s : l) { if (b.length() > max) { b.append(" ..."); break; } if (b.length() > 0) b.append(' '); b.append(s); }
        return b.toString();
    }

    static String prop(String k) {
        try { return (String) Class.forName("android.os.SystemProperties").getMethod("get", String.class).invoke(null, k); }
        catch (Throwable t) { return ""; }
    }

    // ---- what the firmware says it is ----
    static final String[] PROPS = {
        "ro.miui.ui.version.name", "ro.mi.os.version.name", "ro.build.version.oneui", "ro.build.version.sem",
        "ro.build.version.opporom", "ro.build.version.oplusrom", "ro.oxygen.version", "ro.vivo.os.version",
        "ro.vivo.os.build.display.id", "ro.build.version.emui", "ro.build.version.magic", "ro.build.hw_emui_api_level",
        "ro.flyme.published", "ro.build.display.id", "ro.lenovo.series", "ro.asus.ui", "ro.nubia.version",
        "ro.board.platform", "ro.hardware", "ro.mediatek.platform", "ro.mediatek.version.release", "ro.soc.manufacturer",
        "ro.soc.model", "ro.hardware.egl", "ro.hardware.vulkan", "ro.hardware.gralloc", "ro.product.first_api_level",
        "ro.vndk.version", "ro.treble.enabled", "ro.build.characteristics" };

    void props() {
        for (String k : PROPS) { String v = prop(k); if (v != null && v.length() > 0) m.line("oem.prop." + k, v); }
        String skin = "aosp";
        if (prop("ro.mi.os.version.name").length() > 0) skin = "hyperos";
        else if (prop("ro.miui.ui.version.name").length() > 0) skin = "miui";
        else if (prop("ro.build.version.oneui").length() > 0) skin = "oneui";
        else if (prop("ro.build.version.oplusrom").length() > 0 || prop("ro.build.version.opporom").length() > 0) skin = "coloros";
        else if (prop("ro.vivo.os.version").length() > 0) skin = "originos/funtouch";
        else if (prop("ro.build.version.emui").length() > 0 || prop("ro.build.version.magic").length() > 0) skin = "emui/magicos";
        else if (prop("ro.flyme.published").length() > 0) skin = "flyme";
        m.line("oem.skin", skin);
        String p = (prop("ro.soc.manufacturer") + " " + prop("ro.board.platform") + " " + prop("ro.hardware") + " " + prop("ro.mediatek.platform")).toLowerCase();
        String soc = p.contains("mediatek") || p.matches(".*\\bmt\\d.*") ? "mediatek"
            : p.contains("qti") || p.contains("qcom") || p.matches(".*\\b(msm|sdm|sm|kona|lahaina|taro|kalama|pineapple|sun|lito|bengal|holi|trinket|atoll)\\w*.*") ? "qualcomm"
            : p.contains("exynos") || p.contains("universal") || p.contains("samsung") ? "exynos"
            : p.contains("kirin") || p.matches(".*\\bhi\\d.*") ? "hisilicon"
            : p.contains("unisoc") || p.matches(".*\\b(ums|sc)\\d.*") ? "unisoc"
            : p.contains("tensor") || p.contains("gs1") || p.contains("zuma") ? "tensor" : "other";
        m.line("oem.soc", soc + " (" + p.trim().replaceAll("\\s+", " ") + ")");
    }

    // ---- every binder service ----
    void services() {
        try {
            Class<?> sm = Class.forName("android.os.ServiceManager");
            String[] names = (String[]) sm.getMethod("listServices").invoke(null);
            final Method get = sm.getMethod("checkService", String.class);
            int alive = 0, missing = 0;
            List<String> dead = new ArrayList<String>();
            Map<String, Integer> prefix = new TreeMap<String, Integer>();
            for (final String n : names) {
                if (late()) { m.line("oem.service.stopped_at", n); break; }
                try {
                    String d = timed(new Callable<String>() { public String call() throws Exception {
                        IBinder b = (IBinder) get.invoke(null, n);
                        if (b == null) return null;
                        if (!b.pingBinder()) return "";
                        String s = b.getInterfaceDescriptor();
                        return s == null ? "?" : s;
                    } }, 1500);
                    if (d == null) { missing++; continue; }
                    if (d.length() == 0) { dead.add(n); continue; }
                    alive++;
                    String[] seg = d.split("\\.");
                    String pre = seg.length > 2 ? seg[0] + "." + seg[1] : d;
                    Integer was = prefix.get(pre); prefix.put(pre, was == null ? 1 : was + 1);
                } catch (Exception e) { dead.add(n + "(" + e.getMessage() + ")"); }
            }
            m.line("oem.service.count", names.length);
            m.line("oem.service.alive", alive);
            m.line("oem.service.null", missing);
            m.line("oem.service.dead", dead.size() + " " + join(dead, 1500));
            StringBuilder b = new StringBuilder();
            for (Map.Entry<String, Integer> e : prefix.entrySet()) b.append(e.getKey()).append(':').append(e.getValue()).append(' ');
            m.line("oem.service.interfaces", b.toString().trim());
        } catch (Throwable t) { m.line("oem.service.error", t); }
    }

    // ---- every package: its files, its components ----
    void packages() {
        try {
            List<PackageInfo> all = pm.getInstalledPackages(0);
            int sys = 0, act = 0, svc = 0, prov = 0, rcv = 0, launch = 0;
            List<String> broken = new ArrayList<String>(), noApk = new ArrayList<String>();
            for (PackageInfo p0 : all) {
                ApplicationInfo ai = p0.applicationInfo;
                if (ai != null && (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0) sys++;
                if (ai != null && ai.sourceDir != null && !new File(ai.sourceDir).exists()) noApk.add(p0.packageName);
                try {
                    PackageInfo p = pm.getPackageInfo(p0.packageName, PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES
                        | PackageManager.GET_PROVIDERS | PackageManager.GET_RECEIVERS | PackageManager.GET_DISABLED_COMPONENTS);
                    if (p.activities != null) act += p.activities.length;
                    if (p.services != null) svc += p.services.length;
                    if (p.providers != null) prov += p.providers.length;
                    if (p.receivers != null) rcv += p.receivers.length;
                } catch (Throwable t) { broken.add(p0.packageName); }
                if (pm.getLaunchIntentForPackage(p0.packageName) != null) launch++;
            }
            m.line("oem.pkg.count", all.size() + " system " + sys + " launchable " + launch);
            m.line("oem.pkg.components", "activities " + act + " services " + svc + " providers " + prov + " receivers " + rcv);
            m.line("oem.pkg.unreadable", broken.size() + " " + join(broken, 800));
            m.line("oem.pkg.apk_missing", noApk.size() + " " + join(noApk, 800));
        } catch (Throwable t) { m.line("oem.pkg.error", t); }
    }

    // ---- every content provider of the system: does its process come up and answer ----
    void providers() {
        try {
            List<ProviderInfo> l = pm.queryContentProviders(null, 0, 0);
            final ContentResolver cr = c.getContentResolver();
            int ok = 0, tried = 0;
            List<String> fail = new ArrayList<String>();
            for (ProviderInfo p : l) {
                if (p.authority == null || p.packageName.equals(c.getPackageName())) continue;
                if (late()) { m.line("oem.provider.stopped_at", p.authority); break; }
                final String auth = p.authority.split(";")[0];
                tried++;
                try {
                    timed(new Callable<Object>() { public Object call() throws Exception {
                        ContentProviderClient cl = cr.acquireUnstableContentProviderClient(auth);
                        if (cl == null) throw new Exception("null client");
                        try { cl.getType(Uri.parse("content://" + auth + "/")); } finally { cl.release(); }
                        return null;
                    } }, 3000);
                    ok++;
                } catch (Exception e) {
                    String s = e.getMessage() == null ? "" : e.getMessage();
                    // its process came up when it answers at all (an exception about the uri, or "not for you")
                    boolean down = s.startsWith("timeout") || s.contains("null client") || s.contains("DeadObject") || s.contains("died");
                    if (!down) ok++;
                    else fail.add(auth + "(" + s.replaceAll("\\s+", "_") + ")");
                }
            }
            m.line("oem.provider.count", l.size() + " tried " + tried + " ok " + ok);
            m.line("oem.provider.fail", fail.size() + " " + join(fail, 1500));
        } catch (Throwable t) { m.line("oem.provider.error", t); }
    }

    // ---- permission files: libraries and features a vendor declares, and whether the library is there ----
    void permissionFiles() {
        String[] dirs = { "/system/etc/permissions", "/vendor/etc/permissions", "/product/etc/permissions",
            "/system_ext/etc/permissions", "/odm/etc/permissions", "/system/product/etc/permissions" };
        Pattern lib = Pattern.compile("<library\\s+name=\"([^\"]+)\"\\s+file=\"([^\"]+)\"");
        int files = 0, libs = 0;
        List<String> missing = new ArrayList<String>();
        for (String d : dirs) {
            File[] fs = new File(d).listFiles();
            if (fs == null) continue;
            for (File f : fs) {
                if (!f.getName().endsWith(".xml")) continue;
                files++;
                try {
                    byte[] b = new byte[(int) Math.min(f.length(), 1 << 20)];
                    FileInputStream in = new FileInputStream(f); int n = in.read(b); in.close();
                    Matcher mt = lib.matcher(new String(b, 0, Math.max(n, 0), "UTF-8"));
                    while (mt.find()) { libs++; if (!new File(mt.group(2)).exists()) missing.add(mt.group(1) + "=" + mt.group(2)); }
                } catch (Throwable t) { missing.add(f.getName() + "(unreadable)"); }
            }
        }
        m.line("oem.permfiles", files + " libraries " + libs);
        m.line("oem.permfiles.missing_lib", missing.size() + " " + join(missing, 1200));
        try {
            String[] shared = pm.getSystemSharedLibraryNames();
            m.line("oem.shared_libs", shared == null ? 0 : shared.length);
        } catch (Throwable t) { m.line("oem.shared_libs.error", t); }
    }

    void features() {
        try {
            FeatureInfo[] fs = pm.getSystemAvailableFeatures();
            List<String> own = new ArrayList<String>();
            for (FeatureInfo f : fs) if (f.name != null && !f.name.startsWith("android.")) own.add(f.name);
            m.line("oem.feature.count", fs.length + " non-android " + own.size());
            m.line("oem.feature.non_android", join(own, 2000));
        } catch (Throwable t) { m.line("oem.feature.error", t); }
    }

    void settings() {
        for (String t : new String[] { "system", "secure", "global" }) {
            try {
                Cursor q = c.getContentResolver().query(Uri.parse("content://settings/" + t), null, null, null, null);
                m.line("oem.settings." + t, q == null ? "null" : String.valueOf(q.getCount()));
                if (q != null) q.close();
            } catch (Throwable e) { m.line("oem.settings." + t, "fail " + e); }
        }
    }

    // ---- the standard intents: a vendor's own apps have to answer them ----
    void intents() {
        Object[][] list = {
            { "settings", new Intent(Settings.ACTION_SETTINGS) }, { "wifi", new Intent(Settings.ACTION_WIFI_SETTINGS) },
            { "bluetooth", new Intent(Settings.ACTION_BLUETOOTH_SETTINGS) }, { "display", new Intent(Settings.ACTION_DISPLAY_SETTINGS) },
            { "sound", new Intent(Settings.ACTION_SOUND_SETTINGS) }, { "location", new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS) },
            { "security", new Intent(Settings.ACTION_SECURITY_SETTINGS) }, { "date", new Intent(Settings.ACTION_DATE_SETTINGS) },
            { "locale", new Intent(Settings.ACTION_LOCALE_SETTINGS) }, { "ime", new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS) },
            { "app_details", new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + c.getPackageName())) },
            { "apps", new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS) }, { "storage", new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS) },
            { "battery_saver", new Intent("android.settings.BATTERY_SAVER_SETTINGS") }, { "overlay", new Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION", Uri.parse("package:" + c.getPackageName())) },
            { "notif_settings", new Intent("android.settings.APP_NOTIFICATION_SETTINGS").putExtra("android.provider.extra.APP_PACKAGE", c.getPackageName()) },
            { "accessibility", new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS) }, { "nfc_settings", new Intent(Settings.ACTION_NFC_SETTINGS) },
            { "dial", new Intent(Intent.ACTION_DIAL, Uri.parse("tel:123")) }, { "sms", new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:123")) },
            { "mail", new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:a@b.c")) }, { "web", new Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")) },
            { "photo", new Intent(MediaStore.ACTION_IMAGE_CAPTURE) }, { "video", new Intent(MediaStore.ACTION_VIDEO_CAPTURE) },
            { "pick_image", new Intent(Intent.ACTION_GET_CONTENT).setType("image/*") }, { "pick_contact", new Intent(Intent.ACTION_PICK, Uri.parse("content://contacts/people")) },
            { "insert_contact", new Intent(Intent.ACTION_INSERT, Uri.parse("content://com.android.contacts/contacts")) },
            { "calendar_event", new Intent(Intent.ACTION_INSERT, Uri.parse("content://com.android.calendar/events")) },
            { "set_alarm", new Intent("android.intent.action.SET_ALARM") }, { "show_alarms", new Intent("android.intent.action.SHOW_ALARMS") },
            { "share_text", new Intent(Intent.ACTION_SEND).setType("text/plain") }, { "open_document", new Intent("android.intent.action.OPEN_DOCUMENT").addCategory(Intent.CATEGORY_OPENABLE).setType("*/*") },
            { "view_image", new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse("file:///sdcard/a.jpg"), "image/jpeg") },
            { "view_video", new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse("file:///sdcard/a.mp4"), "video/mp4") },
            { "view_audio", new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse("file:///sdcard/a.mp3"), "audio/mpeg") },
            { "view_pdf", new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse("file:///sdcard/a.pdf"), "application/pdf") },
            { "map", new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=x")) }, { "home", new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME) },
            { "search", new Intent(Intent.ACTION_WEB_SEARCH) }, { "voice", new Intent("android.speech.action.RECOGNIZE_SPEECH") },
            { "app_calculator", Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALCULATOR) },
            { "app_browser", Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER) },
            { "app_calendar", Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR) },
            { "app_contacts", Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CONTACTS) },
            { "app_email", Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_EMAIL) },
            { "app_gallery", Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_GALLERY) },
            { "app_messaging", Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING) },
            { "app_music", Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC) },
            { "app_maps", Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MAPS) },
        };
        int ok = 0; List<String> none = new ArrayList<String>();
        for (Object[] e : list) {
            try { if (((Intent) e[1]).resolveActivity(pm) != null) ok++; else none.add((String) e[0]); }
            catch (Throwable t) { none.add(e[0] + "(" + t + ")"); }
        }
        m.line("oem.intent.resolved", ok + "/" + list.length);
        m.line("oem.intent.unresolved", join(none, 1200));
    }

    // ---- platform services vendors reimplement ----
    interface Check { String run() throws Throwable; }

    void api(String name, final Check k) {
        long t = SystemClock.elapsedRealtime();
        try {
            // (a vendor function that never returns is a failure too, not the end of the test)
            String r = timed(new Callable<String>() { public String call() throws Exception {
                try { return k.run(); } catch (Exception e) { throw e; } catch (Throwable e) { throw new Exception(e); }
            } }, 10000);
            apiOk++;
            m.line("oem.api." + name, "ok " + (r == null ? "" : r + " ") + (SystemClock.elapsedRealtime() - t) + "ms");
        } catch (Throwable e) {
            apiFail++;
            m.line("oem.api." + name, "FAIL " + e);
        }
    }

    Object svc(String n) { return c.getSystemService(n); }

    void apis() {
        final int sdk = Build.VERSION.SDK_INT;
        api("notification", new Check() { public String run() throws Throwable {
            NotificationManager nm = (NotificationManager) svc(Context.NOTIFICATION_SERVICE);
            Notification.Builder b;
            if (sdk >= 26) {
                Class<?> ch = Class.forName("android.app.NotificationChannel");
                Object chan = ch.getConstructor(String.class, CharSequence.class, int.class).newInstance("t", "test", 2);
                NotificationManager.class.getMethod("createNotificationChannel", ch).invoke(nm, chan);
                b = Notification.Builder.class.getConstructor(Context.class, String.class).newInstance(c, "t");
            } else b = new Notification.Builder(c);
            nm.notify(7, b.setSmallIcon(android.R.drawable.stat_notify_sync).setContentTitle("aemu test").build());
            nm.cancel(7);
            return null;
        } });
        api("vibrator", new Check() { public String run() throws Throwable {
            Vibrator v = (Vibrator) svc(Context.VIBRATOR_SERVICE); boolean has = v != null && v.hasVibrator();
            if (has) v.vibrate(20); return "has " + has;
        } });
        api("clipboard", new Check() { public String run() throws Throwable {
            final ClipboardManager[] cm = new ClipboardManager[1];
            final String[] got = new String[1];
            final Object lock = new Object();
            m.h.post(new Runnable() { public void run() {
                try {
                    cm[0] = (ClipboardManager) svc(Context.CLIPBOARD_SERVICE);
                    cm[0].setPrimaryClip(ClipData.newPlainText("t", "aemu-clip"));
                    ClipData d = cm[0].getPrimaryClip();
                    got[0] = d == null ? "null" : String.valueOf(d.getItemAt(0).getText());
                } catch (Throwable t) { got[0] = "error " + t; }
                synchronized (lock) { lock.notify(); }
            } });
            synchronized (lock) { if (got[0] == null) lock.wait(3000); }
            if (!"aemu-clip".equals(got[0])) throw new Exception("read back " + got[0]);
            return null;
        } });
        api("alarm", new Check() { public String run() throws Throwable {
            AlarmManager a = (AlarmManager) svc(Context.ALARM_SERVICE);
            PendingIntent pi = PendingIntent.getBroadcast(c, 0, new Intent("app.aemu.test.ALARM").setPackage(c.getPackageName()), sdk >= 23 ? 0x04000000 : 0);
            a.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 3600000, pi); a.cancel(pi);
            return null;
        } });
        api("power", new Check() { public String run() throws Throwable {
            PowerManager p = (PowerManager) svc(Context.POWER_SERVICE);
            return "interactive " + p.isInteractive() + " saver " + p.isPowerSaveMode();
        } });
        api("battery", new Check() { public String run() throws Throwable {
            BatteryManager b = (BatteryManager) svc(Context.BATTERY_SERVICE);
            Intent s = c.registerReceiver(null, new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            return "capacity " + b.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) + " level " + (s == null ? -1 : s.getIntExtra("level", -1));
        } });
        api("storage", new Check() { public String run() throws Throwable {
            StatFs f = new StatFs(c.getFilesDir().getPath());
            String vols = "";
            if (sdk >= 24) vols = " volumes " + ((List<?>) Class.forName("android.os.storage.StorageManager").getMethod("getStorageVolumes").invoke(svc(Context.STORAGE_SERVICE))).size();
            return "free_mb " + (f.getAvailableBytes() >> 20) + vols;
        } });
        api("keyguard", new Check() { public String run() throws Throwable { return "locked " + ((KeyguardManager) svc(Context.KEYGUARD_SERVICE)).isKeyguardLocked(); } });
        api("audio", new Check() { public String run() throws Throwable {
            AudioManager a = (AudioManager) svc(Context.AUDIO_SERVICE);
            String dev = sdk >= 23 ? " outputs " + a.getDevices(AudioManager.GET_DEVICES_OUTPUTS).length + " inputs " + a.getDevices(AudioManager.GET_DEVICES_INPUTS).length : "";
            return "music " + a.getStreamVolume(AudioManager.STREAM_MUSIC) + "/" + a.getStreamMaxVolume(AudioManager.STREAM_MUSIC) + " mode " + a.getMode() + dev;
        } });
        api("location", new Check() { public String run() throws Throwable {
            LocationManager l = (LocationManager) svc(Context.LOCATION_SERVICE); return "providers " + l.getAllProviders();
        } });
        api("accounts", new Check() { public String run() throws Throwable {
            return "authenticators " + AccountManager.get(c).getAuthenticatorTypes().length;
        } });
        api("telephony", new Check() { public String run() throws Throwable {
            TelephonyManager t = (TelephonyManager) svc(Context.TELEPHONY_SERVICE);
            return "phone_type " + t.getPhoneType() + " sim " + t.getSimState() + " operator '" + t.getNetworkOperatorName() + "'";
        } });
        api("connectivity", new Check() { public String run() throws Throwable {
            NetworkInfo n = ((ConnectivityManager) svc(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
            return n == null ? "no network" : n.getTypeName() + " " + n.getState();
        } });
        api("wifi", new Check() { public String run() throws Throwable {
            WifiManager w = (WifiManager) c.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            return w == null ? "none" : "enabled " + w.isWifiEnabled();
        } });
        api("bluetooth", new Check() { public String run() throws Throwable {
            BluetoothAdapter b = BluetoothAdapter.getDefaultAdapter(); return b == null ? "none" : "present";
        } });
        api("nfc", new Check() { public String run() throws Throwable { return NfcAdapter.getDefaultAdapter(c) == null ? "none" : "present"; } });
        api("ime", new Check() { public String run() throws Throwable {
            return "enabled " + ((InputMethodManager) svc(Context.INPUT_METHOD_SERVICE)).getEnabledInputMethodList().size();
        } });
        api("accessibility", new Check() { public String run() throws Throwable {
            return "installed " + ((AccessibilityManager) svc(Context.ACCESSIBILITY_SERVICE)).getInstalledAccessibilityServiceList().size();
        } });
        api("wallpaper", new Check() { public String run() throws Throwable {
            WallpaperManager w = WallpaperManager.getInstance(c); return "min_w " + w.getDesiredMinimumWidth() + " live " + (w.getWallpaperInfo() != null);
        } });
        api("display", new Check() { public String run() throws Throwable {
            Display[] ds = ((DisplayManager) svc(Context.DISPLAY_SERVICE)).getDisplays();
            String modes = "";
            if (sdk >= 23) { Object[] ms = (Object[]) Display.class.getMethod("getSupportedModes").invoke(ds[0]);
                StringBuilder b = new StringBuilder(); for (Object o : ms) b.append(Base.f1((Float) o.getClass().getMethod("getRefreshRate").invoke(o))).append(' ');
                modes = " rates " + b.toString().trim(); }
            return "displays " + ds.length + modes;
        } });
        api("camera2", new Check() { public String run() throws Throwable {
            CameraManager cm = (CameraManager) svc(Context.CAMERA_SERVICE);
            StringBuilder b = new StringBuilder();
            for (String id : cm.getCameraIdList()) {
                CameraCharacteristics ch = cm.getCameraCharacteristics(id);
                b.append(id).append(":facing").append(ch.get(CameraCharacteristics.LENS_FACING)).append(",level").append(ch.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)).append(' ');
            }
            return b.toString().trim();
        } });
        api("media_store", new Check() { public String run() throws Throwable {
            String t = c.getContentResolver().getType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI); return "type " + t;
        } });
        api("downloads", new Check() { public String run() throws Throwable {
            Object dm = svc(Context.DOWNLOAD_SERVICE); if (dm == null) throw new Exception("no service");
            Cursor q = ((android.app.DownloadManager) dm).query(new android.app.DownloadManager.Query()); int n = q.getCount(); q.close(); return "rows " + n;
        } });
        api("tts", new Check() { public String run() throws Throwable {
            final int[] st = { 99 }; final Object lock = new Object();
            final TextToSpeech[] tts = new TextToSpeech[1];
            m.h.post(new Runnable() { public void run() {
                tts[0] = new TextToSpeech(c, new TextToSpeech.OnInitListener() { public void onInit(int s) { synchronized (lock) { st[0] = s; lock.notify(); } } });
            } });
            // (without an engine onInit comes from inside the constructor, before tts[0] is set)
            long end = SystemClock.elapsedRealtime() + 6000;
            synchronized (lock) { while ((st[0] == 99 || tts[0] == null) && SystemClock.elapsedRealtime() < end) lock.wait(100); }
            int engines = tts[0] == null ? -1 : tts[0].getEngines().size();
            if (tts[0] != null) tts[0].shutdown();
            if (engines == 0) return "n/a (the firmware has no TTS engine)";
            if (st[0] != TextToSpeech.SUCCESS) throw new Exception("init status " + st[0] + " engines " + engines);
            return "engines " + engines;
        } });
        api("drm_widevine", new Check() { public String run() throws Throwable {
            UUID wv = UUID.fromString("edef8ba9-79d6-4ace-a3c8-27dcd51d21ed");
            if (!MediaDrm.isCryptoSchemeSupported(wv)) return "n/a (no Widevine plugin)";
            // what Play services do at first start: the plugin's identity, then a session (keybox / provisioning)
            MediaDrm d = new MediaDrm(wv);
            try {
                String r = "level " + d.getPropertyString("securityLevel") + " system " + d.getPropertyString("systemId");
                try { byte[] s = d.openSession(); d.closeSession(s); r += ", session ok"; }
                catch (android.media.NotProvisionedException e) { r += ", needs provisioning"; }
                return r;
            } finally { d.release(); }
        } });
        api("codecs", new Check() { public String run() throws Throwable {
            MediaCodecInfo[] all = new MediaCodecList(MediaCodecList.ALL_CODECS).getCodecInfos();
            int dec = 0, enc = 0, vendor = 0;
            Map<String, Integer> pre = new TreeMap<String, Integer>();
            StringBuilder hw = new StringBuilder();
            for (MediaCodecInfo i : all) {
                if (i.isEncoder()) enc++; else dec++;
                String n = i.getName();
                boolean sw = n.startsWith("OMX.google.") || n.startsWith("c2.android.") || n.startsWith("OMX.ffmpeg");
                if (!sw) vendor++;
                String p = n.indexOf('.', n.indexOf('.') + 1) > 0 ? n.substring(0, n.indexOf('.', n.indexOf('.') + 1)) : n;
                Integer w = pre.get(p); pre.put(p, w == null ? 1 : w + 1);
            }
            for (String mime : new String[] { "video/avc", "video/hevc", "video/x-vnd.on2.vp9", "video/av01", "audio/mp4a-latm", "audio/opus" }) {
                String best = "-";
                for (MediaCodecInfo i : all) if (!i.isEncoder()) for (String t : i.getSupportedTypes()) if (t.equalsIgnoreCase(mime)) { if (best.equals("-") || best.startsWith("OMX.google") || best.startsWith("c2.android")) best = i.getName(); }
                hw.append(mime.substring(mime.indexOf('/') + 1)).append('=').append(best).append(' ');
            }
            m.line("oem.codecs.families", pre.toString());
            m.line("oem.codecs.decoders", hw.toString().trim());
            return "decoders " + dec + " encoders " + enc + " vendor " + vendor;
        } });
        api("gpu", new Check() { public String run() throws Throwable {
            EGLDisplay d = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
            int[] v = new int[2]; EGL14.eglInitialize(d, v, 0, v, 1);
            EGLConfig[] cf = new EGLConfig[1]; int[] n = new int[1];
            EGL14.eglChooseConfig(d, new int[] { EGL14.EGL_RENDERABLE_TYPE, 4, EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT, EGL14.EGL_NONE }, 0, cf, 0, 1, n, 0);
            EGLContext ctx = EGL14.eglCreateContext(d, cf[0], EGL14.EGL_NO_CONTEXT, new int[] { 0x3098, 2, EGL14.EGL_NONE }, 0);
            EGLSurface s = EGL14.eglCreatePbufferSurface(d, cf[0], new int[] { EGL14.EGL_WIDTH, 4, EGL14.EGL_HEIGHT, 4, EGL14.EGL_NONE }, 0);
            EGL14.eglMakeCurrent(d, s, s, ctx);
            String vendor = GLES20.glGetString(GLES20.GL_VENDOR), r = GLES20.glGetString(GLES20.GL_RENDERER);
            String ext = GLES20.glGetString(GLES20.GL_EXTENSIONS);
            EGL14.eglMakeCurrent(d, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroySurface(d, s); EGL14.eglDestroyContext(d, ctx);
            String fam = r == null ? "?" : r.contains("Mali") ? "mali" : r.contains("Adreno") ? "adreno" : r.contains("PowerVR") ? "powervr"
                : r.contains("SwiftShader") || r.contains("llvmpipe") ? "software" : r.contains("Xclipse") ? "xclipse" : "other";
            m.line("oem.gpu", fam + " | " + vendor + " | " + r);
            m.line("oem.gpu.egl_ext", EGL14.eglQueryString(d, EGL14.EGL_EXTENSIONS));
            return "gl_extensions " + (ext == null ? 0 : ext.split(" ").length);
        } });
        api("vulkan", new Check() { public String run() throws Throwable {
            boolean lvl = pm.hasSystemFeature("android.hardware.vulkan.level"), ver = pm.hasSystemFeature("android.hardware.vulkan.version");
            return "level " + lvl + " version " + ver + " lib " + (new File("/system/lib64/libvulkan.so").exists() || new File("/system/lib/libvulkan.so").exists());
        } });
        api("webview_provider", new Check() { public String run() throws Throwable {
            if (sdk < 26) return "n/a";
            PackageInfo p = (PackageInfo) Class.forName("android.webkit.WebView").getMethod("getCurrentWebViewPackage").invoke(null);
            return p == null ? "none" : p.packageName + " " + p.versionName;
        } });
        api("running_processes", new Check() { public String run() throws Throwable {
            List<ActivityManager.RunningAppProcessInfo> l = ((ActivityManager) svc(Context.ACTIVITY_SERVICE)).getRunningAppProcesses();
            return "visible " + (l == null ? 0 : l.size());
        } });
    }
}
