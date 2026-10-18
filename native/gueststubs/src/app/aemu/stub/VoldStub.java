package app.aemu.stub;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * The "vold" binder service for Android 9+.
 *
 * Up to 8.1 the storage service talks to vold over a socket (the engine answers it: VoldStub in Stubs.kt). From 9 on
 * it is a binder interface, android.os.IVold, and StorageManagerService calls it without checking that it is there
 * (fstrim on a null reference takes system_server down). The real vold mounts block devices and is of no use in the
 * guest, so this service plays its part the way the socket one does: one emulated volume over /data/media that
 * mounts on request; every other call succeeds and does nothing.
 *
 * Call numbers differ between releases, so they are read from the firmware's own IVold$Stub (TRANSACTION_ fields),
 * and the listener is called through the firmware's own proxy class.
 *
 * Started by the engine before zygote: app_process -Djava.class.path=/system/framework/aemu-stubs.jar /system/bin
 * app.aemu.stub.VoldStub
 */
public class VoldStub extends Binder {
    private static final String DESCRIPTOR = "android.os.IVold";
    private static final int FIRST_CALL = 1, LAST_CALL = 0x00ffffff;
    /** VolumeInfo: TYPE_EMULATED; STATE_UNMOUNTED, CHECKING, MOUNTED, EJECTING */
    private static final int TYPE_EMULATED = 2, UNMOUNTED = 0, CHECKING = 1, MOUNTED = 2, EJECTING = 5;

    private final HashMap<Integer, String> names = new HashMap<Integer, String>();
    private Object listener;
    /** What the listener is told about a volume goes out one call's worth at a time. The system answers the
     *  announcement of the volume with "mount" at once, on another thread here: without this the "unmounted" that
     *  follows the announcement could be sent after that mount's "mounted", and the volume stayed unmounted. */
    private final Object order = new Object();
    private static boolean atUserStart;

    VoldStub() throws Exception {
        for (Field f : Class.forName("android.os.IVold$Stub").getDeclaredFields()) {
            if (!f.getName().startsWith("TRANSACTION_")) continue;
            f.setAccessible(true);
            names.put(f.getInt(null), f.getName().substring(12));
        }
    }

    /** listener.<name>(args): the first method of that name, whatever the release added to its signature */
    private void tell(String name, Object... args) {
        Object l = listener;
        if (l == null) return;
        try {
            for (Method m : l.getClass().getMethods()) {
                if (!m.getName().equals(name)) continue;
                Class<?>[] p = m.getParameterTypes();
                Object[] a = new Object[p.length];
                for (int i = 0; i < p.length; i++) a[i] = i < args.length ? args[i] : (p[i] == int.class ? (Object) 0 : p[i] == String.class ? "" : null);
                m.invoke(l, a);
                return;
            }
        } catch (Throwable t) {
            System.out.println("aemu-vold: " + name + ": " + t);
        }
    }

    @Override
    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
        if (code < FIRST_CALL || code > LAST_CALL) {
            try { return super.onTransact(code, data, reply, flags); } catch (Exception e) { return false; }
        }
        String name = names.get(code);
        if (name == null) name = "";
        int answer = 0;
        try {
            data.enforceInterface(DESCRIPTOR);
            if (name.equals("setListener")) {
                IBinder b = data.readStrongBinder();
                listener = b == null ? null : Class.forName("android.os.IVoldListener$Stub").getMethod("asInterface", IBinder.class).invoke(null, b);
            } else if (name.equals("reset")) {
                synchronized (order) {
                    tell("onVolumeCreated", "emulated", TYPE_EMULATED, "", "");
                    tell("onVolumeStateChanged", "emulated", UNMOUNTED);
                }
            } else if (name.equals("onUserStarted") && atUserStart) {
                // 13+: the volume announced at reset was dropped, the user's media provider was not known yet
                synchronized (order) {
                    tell("onVolumeCreated", "emulated", TYPE_EMULATED, "", "");
                    tell("onVolumeStateChanged", "emulated", UNMOUNTED);
                }
            } else if (name.equals("mount")) {
                String id = data.readString();
                synchronized (order) {
                    tell("onVolumeStateChanged", id, CHECKING);
                    tell("onVolumeInternalPathChanged", id, "/data/media");
                    tell("onVolumePathChanged", id, "/storage/emulated");
                    tell("onVolumeStateChanged", id, MOUNTED);
                }
            } else if (name.equals("unmount")) {
                String id = data.readString();
                synchronized (order) {
                    tell("onVolumeStateChanged", id, EJECTING);
                    tell("onVolumeStateChanged", id, UNMOUNTED);
                }
            } else if (name.equals("mkdirs")) {
                String path = data.readString();
                if (path != null) new File(path).mkdirs();
            } else if (name.equals("fdeComplete")) {
                answer = 1;     // ENCRYPTION_STATE_NONE
            }
        } catch (Throwable t) {
            System.out.println("aemu-vold: " + name + ": " + t);
        }
        if (reply != null) {
            reply.writeNoException();
            // enough for any answer: boolean/int, an empty string, a null object
            reply.writeInt(answer);
            reply.writeInt(0);
        }
        return true;
    }

    @Override
    public String getInterfaceDescriptor() { return DESCRIPTOR; }

    public static void main(String[] args) throws Exception {
        atUserStart = args.length > 0 && args[0].equals("userstart");
        Class<?> sm = Class.forName("android.os.ServiceManager");
        if (sm.getMethod("checkService", String.class).invoke(null, "vold") != null) {
            System.out.println("aemu-vold: the firmware's own vold is running");
            return;
        }
        sm.getMethod("addService", String.class, IBinder.class).invoke(null, "vold", new VoldStub());
        System.out.println("aemu-vold: registered");
        Object lock = new Object();
        synchronized (lock) { while (true) lock.wait(); }
    }
}
