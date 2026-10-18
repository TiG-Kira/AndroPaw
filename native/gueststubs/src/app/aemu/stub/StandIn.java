package app.aemu.stub;

import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;

import java.lang.reflect.Method;

/**
 * Binder services a firmware's own programs call without checking that they exist, where the program that would
 * register them needs the phone's hardware (Samsung's SurfaceFlinger tells "Exynos.HWCService", which lives in the
 * Exynos display HAL, that the boot is over, and dies on a null pointer without it).
 *
 * app_process /system/bin app.aemu.stub.StandIn <service name>...
 *
 * Every call is answered "no exception" and an empty reply. A name somebody has registered is left alone.
 */
public final class StandIn {
    public static void main(String[] args) throws Exception {
        Looper.prepare();
        Class<?> sm = Class.forName("android.os.ServiceManager");
        Method check = sm.getMethod("checkService", String.class);
        Method add = sm.getMethod("addService", String.class, IBinder.class);
        for (final String name : args) {
            try {
                if (check.invoke(null, name) != null) continue;
                add.invoke(null, name, new Binder() {
                    @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
                        if (code == INTERFACE_TRANSACTION) { if (reply != null) reply.writeString(name); return true; }
                        if (reply != null) reply.writeNoException();
                        return true;
                    }
                });
                System.out.println("aemu-standin: " + name);
            } catch (Throwable t) { t.printStackTrace(System.out); }
        }
        System.out.flush();
        Looper.loop();
    }
}
