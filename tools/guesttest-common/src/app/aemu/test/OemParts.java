package app.aemu.test;

import android.app.Service;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;

/** The tester's own components for the vendor-firmware test (Oem2): a service, a job and a provider, each only signals. */
final class OemParts {
    static final Object lock = new Object();
    static volatile String serviceStarted, jobStarted;

    static void signal() { synchronized (lock) { lock.notifyAll(); } }

    public static class Svc extends Service {
        @Override public int onStartCommand(Intent i, int f, int id) {
            serviceStarted = i == null ? "null" : String.valueOf(i.getAction());
            if (i != null && "fg".equals(i.getAction()) && android.os.Build.VERSION.SDK_INT >= 26) {
                try {
                    Object nm = getSystemService(NOTIFICATION_SERVICE);
                    Class<?> ch = Class.forName("android.app.NotificationChannel");
                    Object chan = ch.getConstructor(String.class, CharSequence.class, int.class).newInstance("fg", "fg", 2);
                    nm.getClass().getMethod("createNotificationChannel", ch).invoke(nm, chan);
                    android.app.Notification n = android.app.Notification.Builder.class.getConstructor(android.content.Context.class, String.class)
                        .newInstance(this, "fg").setSmallIcon(android.R.drawable.stat_notify_sync).setContentTitle("aemu fg").build();
                    startForeground(8, n);
                    stopForeground(true);
                } catch (Throwable t) { serviceStarted = "fg error " + t; }
            }
            signal();
            stopSelf();
            return START_NOT_STICKY;
        }
        @Override public IBinder onBind(Intent i) { return new Binder(); }
    }

    public static class Job extends JobService {
        @Override public boolean onStartJob(JobParameters p) { jobStarted = "id " + p.getJobId(); signal(); return false; }
        @Override public boolean onStopJob(JobParameters p) { return false; }
    }

    public static class Prov extends ContentProvider {
        final ContentValues last = new ContentValues();
        @Override public boolean onCreate() { return true; }
        @Override public Cursor query(Uri u, String[] p, String s, String[] a, String o) {
            MatrixCursor c = new MatrixCursor(new String[] { "v" }); c.addRow(new Object[] { last.getAsString("v") }); return c;
        }
        @Override public String getType(Uri u) { return "vnd.android.cursor.item/aemu"; }
        @Override public Uri insert(Uri u, ContentValues v) { last.clear(); last.putAll(v); return u; }
        @Override public int delete(Uri u, String s, String[] a) { return 0; }
        @Override public int update(Uri u, ContentValues v, String s, String[] a) { return 0; }
    }
}
