package app.aemu.test;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.SystemClock;
import android.view.Choreographer;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

/** Tests of testers B and C that need API 21: frames by the Choreographer, the camera into a texture, script results from WebView. */
class Api21 {
    /** draws the view through an offscreen hardware layer, half transparent, as a launcher's app list while it opens */
    static void hwLayer(View v) { v.setLayerType(View.LAYER_TYPE_HARDWARE, null); v.setAlpha(0.95f); }

    /** (26+) a bitmap kept in a graphics buffer, as a launcher's app icons; null before 26 */
    static android.graphics.Bitmap hwBitmap(android.graphics.Bitmap b) {
        return Build.VERSION.SDK_INT >= 26 ? b.copy(android.graphics.Bitmap.Config.HARDWARE, false) : null;
    }

    /** the scrolled list of Base.ui(), its frames counted at the display's pulse - what the screen can show */
    static void ui(final Base m) {
        final long[] ts = new long[m.secs * 400];
        final int[] n = { 0 };
        final float hz = m.hz();
        final View v = new View(m) {
            final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            final RectF r = new RectF();
            @Override protected void onDraw(Canvas c) {
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
                invalidate();
            }
        };
        m.line("ui.layer", v.getLayerType());
        m.root.addView(v, 0, new FrameLayout.LayoutParams(-1, -1));
        final long end = SystemClock.uptimeMillis() + m.secs * 1000L;
        Choreographer.getInstance().postFrameCallback(new Choreographer.FrameCallback() {
            public void doFrame(long t) {
                if (n[0] < ts.length) ts[n[0]++] = t;
                if (SystemClock.uptimeMillis() < end) Choreographer.getInstance().postFrameCallback(this);
                else { m.line("ui.hw_accel", v.isHardwareAccelerated()); m.line("ui.clock", "vsync"); m.frames("ui", ts, n[0], hz); m.done(); }
            }
        });
    }

    /** the first camera API with the preview going to a texture nobody shows: frames and their brightness */
    @SuppressWarnings("deprecation")
    static void camera(Base m) {
        int count = android.hardware.Camera.getNumberOfCameras();
        m.line("camera.count", count);
        if (count == 0) return;
        if (Build.VERSION.SDK_INT >= 23 && m.checkSelfPermission("android.permission.CAMERA") != 0) { m.line("camera.error", "no permission (pm grant app.aemu.test android.permission.CAMERA)"); return; }
        long t = SystemClock.elapsedRealtime();
        android.hardware.Camera cam = android.hardware.Camera.open(0);
        m.line("camera.open_ms", SystemClock.elapsedRealtime() - t);
        try {
            android.hardware.Camera.Size sz = cam.getParameters().getPreviewSize();
            m.line("camera.preview", sz.width + "x" + sz.height);
            final int[] n = { 0, 0 };
            final long[] luma = { 0, 255, 0, -1 };      // mean, min, max of the last frame's Y plane; the sum before
            final int pixels = sz.width * sz.height;
            cam.setPreviewTexture(new SurfaceTexture(10));
            cam.setPreviewCallback(new android.hardware.Camera.PreviewCallback() {
                public void onPreviewFrame(byte[] d, android.hardware.Camera c) {
                    n[0]++;
                    long sum = 0; int lo = 255, hi = 0, k = Math.min(pixels, d.length);
                    for (int i = 0; i < k; i += 16) { int y = d[i] & 0xff; sum += y; if (y < lo) lo = y; if (y > hi) hi = y; }
                    if (sum != luma[3]) n[1]++;
                    luma[0] = sum / Math.max(1, k / 16); luma[1] = lo; luma[2] = hi; luma[3] = sum;
                }
            });
            cam.startPreview();
            SystemClock.sleep(3000);
            m.line("camera.frames_3s", n[0]);
            m.line("camera.changed_frames", n[1]);      // a sensor's noise makes every frame differ; 1 = a frozen picture
            m.line("camera.luma_mean", luma[0]);
            m.line("camera.luma_min", luma[1]);
            m.line("camera.luma_max", luma[2]);         // min = max: a flat frame, nothing from a sensor
            cam.stopPreview();
        } catch (Throwable e) {
            m.line("camera.error", e);
        } finally {
            cam.release();
        }
    }

    static void webview(final Base m) {
        final long t0 = SystemClock.elapsedRealtime();
        final boolean[] ended = { false };
        final Runnable end = new Runnable() { public void run() { if (!ended[0]) { ended[0] = true; m.done(); } } };
        final WebView w = new WebView(m);
        m.line("webview.created_ms", SystemClock.elapsedRealtime() - t0);
        w.getSettings().setJavaScriptEnabled(true);
        m.line("webview.ua", w.getSettings().getUserAgentString());
        w.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView v, String url) {
                m.line("webview.loaded_ms", SystemClock.elapsedRealtime() - t0);
                m.h.postDelayed(new Runnable() { public void run() {
                    w.evaluateJavascript("JSON.stringify({frames:window.n,sum:window.s,gl:window.gl})", new android.webkit.ValueCallback<String>() {
                        public void onReceiveValue(String v) { m.line("webview.js", v); end.run(); }
                    });
                } }, 3000);
            }
        });
        m.root.addView(w, 0, new FrameLayout.LayoutParams(-1, -1));
        // script: a sum (the engine runs), an animated canvas counted in frames (the page is drawn), a WebGL context
        w.loadDataWithBaseURL("https://aemu.test/", "<html><body style='background:#123;color:#fff'><h2>WebView</h2><canvas id=c width=300 height=300></canvas><script>"
            + "window.s=0;for(var i=0;i<1e6;i++)window.s+=i%7;window.n=0;var x=document.getElementById('c').getContext('2d');"
            + "function f(){window.n++;x.fillStyle='hsl('+(window.n*5%360)+',80%,50%)';x.fillRect(0,0,300,300);requestAnimationFrame(f)}f();"
            + "try{var g=document.createElement('canvas').getContext('webgl');window.gl=g?g.getParameter(g.VERSION):'none'}catch(e){window.gl='error'}"
            + "</script></body></html>", "text/html", "utf-8", null);
        m.h.postDelayed(new Runnable() { public void run() { if (!ended[0]) { m.line("webview.error", "timeout 20 s"); end.run(); } } }, 20000);
    }
}
