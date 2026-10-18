package app.aemu.stub;

import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * "apexservice" (Android 15+): the package manager takes the list of APEX modules from apexd over binder and scans
 * the apps inside them from that list (up to 14 it walked /apex itself when the modules were "flattened"). The real
 * apexd has nothing to report here: the modules are plain directories the engine unpacked, not mounted images.
 *
 * app_process /system/bin app.aemu.stub.ApexStub
 *
 * The modules come from /apex/.aemu-apexes, one per line: name, package file, preinstalled path, version (tabs).
 * The parcelable and the call numbers are taken from the firmware's own services.jar, so the layout is always the
 * one its package manager reads. Every other call is answered "no exception" (an empty list where one is read).
 */
public final class ApexStub {
    public static void main(String[] args) throws Exception {
        Looper.prepare();
        ClassLoader cl = new dalvik.system.PathClassLoader("/system/framework/services.jar", ApexStub.class.getClassLoader());
        final Class<?> info = cl.loadClass("android.apex.ApexInfo");
        Class<?> stub = cl.loadClass("android.apex.IApexService$Stub");
        final HashMap<Integer, String> calls = new HashMap<Integer, String>();
        for (Field f : stub.getDeclaredFields()) {
            if (!f.getName().startsWith("TRANSACTION_")) continue;
            f.setAccessible(true);
            calls.put(f.getInt(null), f.getName().substring(12));
        }
        final ArrayList<Object> list = new ArrayList<Object>();
        BufferedReader r = new BufferedReader(new FileReader("/apex/.aemu-apexes"));
        for (String line; (line = r.readLine()) != null; ) {
            String[] p = line.split("\t");
            if (p.length < 4) continue;
            Object o = info.newInstance();
            set(o, "moduleName", p[0]);
            set(o, "modulePath", p[1]);
            set(o, "preinstalledModulePath", p[2]);
            set(o, "versionCode", Long.parseLong(p[3]));
            set(o, "versionName", p[3]);
            set(o, "isFactory", true);
            set(o, "isActive", true);
            set(o, "hasClassPathJars", new File("/apex/" + p[0] + "/javalib").isDirectory());
            set(o, "activeApexChanged", false);
            list.add(o);
        }
        r.close();
        final Parcelable[] all = (Parcelable[]) Array.newInstance(info, list.size());
        list.toArray(all);
        final Field nameField = info.getField("moduleName");
        Class<?> sm = Class.forName("android.os.ServiceManager");
        Method add = sm.getMethod("addService", String.class, IBinder.class);
        add.invoke(null, "apexservice", new Binder() {
            @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
                if (code == INTERFACE_TRANSACTION) { if (reply != null) reply.writeString("android.apex.IApexService"); return true; }
                String call = calls.get(code);
                if (reply == null) return true;
                reply.writeNoException();
                if ("getActivePackages".equals(call) || "getAllPackages".equals(call)) {
                    reply.writeTypedArray(all, Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
                } else if ("getActivePackage".equals(call)) {
                    data.enforceInterface("android.apex.IApexService");
                    String name = data.readString();
                    Parcelable found = null;
                    try {
                        for (Parcelable p : all) if (name != null && name.equals(nameField.get(p))) found = p;
                    } catch (IllegalAccessException e) { /* a public field */ }
                    reply.writeTypedObject(found, Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
                }
                return true;
            }
        });
        System.out.println("aemu-apex: " + all.length + " modules");
        System.out.flush();
        Looper.loop();
    }

    /** sets a public field when this release's parcelable has it */
    private static void set(Object o, String field, Object value) {
        try { o.getClass().getField(field).set(o, value); } catch (Exception e) { /* not in this release */ }
    }
}
