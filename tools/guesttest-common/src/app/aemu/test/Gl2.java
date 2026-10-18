package app.aemu.test;

import android.app.ActivityManager;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.widget.FrameLayout;

/** The timed GLES 2 / GLES 3 scenes (Scene) on a GLSurfaceView: API 8 and later, loaded only there. */
class Gl2 {
    static void start(Base m, boolean heavy) {
        ActivityManager am = (ActivityManager) m.getSystemService(Context.ACTIVITY_SERVICE);
        int ver = am.getDeviceConfigurationInfo().reqGlEsVersion;
        String k = heavy ? "gl3" : "gl2";
        if (ver < 0x20000) { m.line(k + ".skipped", "GLES " + Integer.toHexString(ver)); m.done(); return; }
        boolean es3 = heavy && ver >= 0x30000 && Base.SDK >= 18;
        Scene scene = new Scene(m, k, heavy, es3, m.secs, m.hz());
        GLSurfaceView sv = new GLSurfaceView(m);
        sv.setEGLContextClientVersion(es3 ? 3 : 2);
        sv.setRenderer(scene);
        m.root.addView(sv, 0, new FrameLayout.LayoutParams(-1, -1));
    }
}
