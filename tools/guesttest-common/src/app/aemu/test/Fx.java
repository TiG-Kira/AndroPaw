package app.aemu.test;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.view.Choreographer;
import android.view.TextureView;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The look of vendor skins, drawn by the tester (API 21+): frosted and liquid glass (HyperOS 3, OriginOS 6, iOS-26-like),
 * blur transitions, contour glow (ColorOS 16), islands that morph, springs, app-open zoom, smoke and particle dust,
 * elevation and coloured shadows, rounded clipping, colour filters, parallax depth, TextureView/SurfaceView mixes,
 * hardware bitmaps, variable fonts. Each effect is done the ways firmwares of every release do it: RenderEffect (12+),
 * AGSL RuntimeShader (13+), RenderScript blur (the pre-12 MIUI way). Every scene: frames at the display's pulse for
 * a while (fx.<scene>.fps/jank/...), then a PixelCopy of the window (8.0+) checked: not blank, moving, and, for the
 * blurs, less fine detail inside the glass than outside it (fx.<scene>.check). A scene a release cannot do is n/a.
 */
final class Fx {
    final Base m;
    final int sdk = Build.VERSION.SDK_INT;
    final List<Scene> scenes = new ArrayList<Scene>();
    int at = -1;
    long sceneMs;

    abstract static class Scene {
        final String name; final int minSdk;
        Scene(String name, int minSdk) { this.name = name; this.minSdk = minSdk; }
        abstract View make(Base m);
        /** the region that must come out blurred (null: no blur check) */
        RectF glass(View v) { return null; }
        /** inside detail must stay below this share of outside; refracting glass adds its own streaks, so it gets more room */
        double blurLimit() { return 0.6; }
        void end() { }
    }

    Fx(Base m) { this.m = m; sceneMs = Math.max(1500, Math.min(3000, m.secs * 1000L / 2)); }

    static void start(Base m) { Fx f = new Fx(m); f.add(); f.next(); }

    // ---- shared drawing: a busy moving backdrop (icons, text, stripes) like a home screen behind glass ----
    static void backdrop(Canvas c, int w, int h, float t, Paint p) {
        p.setShader(null); p.setMaskFilter(null);
        c.drawColor(0xff101830);
        float off = (t * 160) % 120;
        for (int i = -2; i < h / 60 + 3; i++) {
            p.setColor((i & 1) == 0 ? 0xff2a4a8a : 0xff8a2a4a);
            c.drawRect(0, i * 60 + off, w, i * 60 + off + 22, p);
        }
        p.setColor(0xffffffff); p.setTextSize(h / 40f);
        for (int r = 0; r < 12; r++) c.drawText("Glass test line " + r + " - 0123456789 ABCDEFG", 20 + (t * 40) % 60, 80 + r * h / 12f, p);
        for (int k = 0; k < 20; k++) {
            p.setColor(Color.HSVToColor(new float[] { (k * 37 + t * 60) % 360, 0.8f, 1 }));
            c.drawCircle((k % 5) * w / 5f + w / 10f, (k / 5) * h / 4f + h / 8f + (float) Math.sin(t * 2 + k) * 20, w / 16f, p);
        }
    }

    abstract static class Anim extends View {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final long t0 = SystemClock.uptimeMillis();
        Anim(Base m) { super(m); }
        float t() { return (SystemClock.uptimeMillis() - t0) / 1000f; }
        abstract void paint(Canvas c, float t);
        @Override protected void onDraw(Canvas c) { paint(c, t()); postInvalidateOnAnimation(); }
    }

    static class Backdrop extends Anim {
        Backdrop(Base m) { super(m); }
        void paint(Canvas c, float t) { backdrop(c, getWidth(), getHeight(), t, p); }
    }

    /** a glass panel: draws the same backdrop under itself (shifted to its place), the blur comes from outside */
    static class GlassContent extends Anim {
        GlassContent(Base m) { super(m); }
        void paint(Canvas c, float t) {
            View parent = (View) getParent();
            c.save(); c.translate(-getLeft(), -getTop());
            backdrop(c, parent.getWidth(), parent.getHeight(), t, p);
            c.restore();
        }
    }

    static FrameLayout.LayoutParams glassLp(int w, int h) { FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w, h); lp.leftMargin = w / 6; lp.topMargin = h; return lp; }

    // ---- the scenes ----
    void add() {
        scenes.add(new Scene("blur_rendereffect", 31) {
            View make(Base m) { return glassScene(m, 0); }
            RectF glass(View v) { return rectOf(((FrameLayout) v).getChildAt(1)); }
        });
        scenes.add(new Scene("liquid_glass_agsl", 33) {
            View make(Base m) { return glassScene(m, 1); }
            RectF glass(View v) { return rectOf(((FrameLayout) v).getChildAt(1)); }
            double blurLimit() { return 0.95; }
        });
        scenes.add(new Scene("blur_renderscript", 21) {
            View make(Base m) { return rsBlurScene(m); }
            RectF glass(View v) { View g = v; return new RectF(g.getWidth() * 0.15f, g.getHeight() * 0.3f, g.getWidth() * 0.85f, g.getHeight() * 0.6f); }
        });
        scenes.add(new Scene("blur_window_behind", 31) {
            android.app.Dialog d;
            View make(Base m) {
                View b = new Backdrop(m);
                d = new android.app.Dialog(m);
                android.widget.TextView tv = new android.widget.TextView(m); tv.setText("blur behind"); tv.setPadding(80, 80, 80, 80);
                d.setContentView(tv);
                android.view.Window w = d.getWindow();
                w.addFlags(0x4);    // FLAG_BLUR_BEHIND
                try {
                    w.getClass().getMethod("setBackgroundBlurRadius", int.class).invoke(w, 60);
                    w.getAttributes().getClass().getMethod("setBlurBehindRadius", int.class).invoke(w.getAttributes(), 60);
                    Object wm = m.getSystemService("window");
                    m.line("fx.blur_window_behind.cross_window_blur", wm.getClass().getMethod("isCrossWindowBlurEnabled").invoke(wm));
                } catch (Throwable t) { m.line("fx.blur_window_behind.api", t); }
                d.show();
                return b;
            }
            void end() { if (d != null) d.dismiss(); }
        });
        scenes.add(new Scene("contour_glow", 21) {
            View make(Base m) {
                return new Anim(m) {
                    { setLayerType(LAYER_TYPE_HARDWARE, null); }
                    void paint(Canvas c, float t) {
                        backdrop(c, getWidth(), getHeight(), t, p);
                        RectF r = new RectF(getWidth() * 0.1f, getHeight() * 0.35f, getWidth() * 0.9f, getHeight() * 0.55f);
                        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(18);
                        SweepGradient g = new SweepGradient(r.centerX(), r.centerY(), new int[] { 0xff00ffff, 0xffff00ff, 0xffffff00, 0xff00ffff }, null);
                        android.graphics.Matrix mx = new android.graphics.Matrix(); mx.setRotate(t * 120, r.centerX(), r.centerY()); g.setLocalMatrix(mx);
                        p.setShader(g); p.setMaskFilter(new BlurMaskFilter(24, BlurMaskFilter.Blur.NORMAL));
                        c.drawRoundRect(r, 60, 60, p);
                        p.setMaskFilter(null); p.setStrokeWidth(4); c.drawRoundRect(r, 60, 60, p);
                        p.setShader(null); p.setStyle(Paint.Style.FILL);
                    }
                };
            }
        });
        scenes.add(new Scene("island_morph", 21) {
            View make(Base m) {
                return new Anim(m) {
                    void paint(Canvas c, float t) {
                        backdrop(c, getWidth(), getHeight(), t, p);
                        float k = spring(t % 1.6f);         // collapsed pill -> expanded card, with overshoot
                        float w = 200 + k * (getWidth() * 0.8f - 200), h = 60 + k * 260, cx = getWidth() / 2f;
                        RectF r = new RectF(cx - w / 2, 40, cx + w / 2, 40 + h);
                        p.setColor(0xff000000); p.setShadowLayer(30, 0, 10, 0x80000000);
                        c.drawRoundRect(r, Math.min(h, 120) / 2, Math.min(h, 120) / 2, p);
                        p.clearShadowLayer(); p.setColor(0xffffffff); p.setAlpha((int) (255 * Math.max(0, Math.min(1, k))));
                        p.setTextSize(36); c.drawText("Island 12:34  ♪", r.left + 40, r.top + Math.min(h, 120) / 2 + 12, p); p.setAlpha(255);
                    }
                };
            }
        });
        scenes.add(new Scene("spring_icons", 21) {
            View make(Base m) {
                return new Anim(m) {
                    final float[] x = new float[30], v = new float[30];
                    long last = SystemClock.uptimeMillis();
                    void paint(Canvas c, float t) {
                        c.drawColor(0xff203040);
                        long now = SystemClock.uptimeMillis(); float dt = Math.min(0.05f, (now - last) / 1000f); last = now;
                        boolean open = (int) (t / 1.2f) % 2 == 0;
                        for (int i = 0; i < 30; i++) {          // spring k=170 d=14 (an "elastic" launcher folder)
                            float target = open ? 1 : 0; float a = 170 * (target - x[i]) - 14 * v[i];
                            v[i] += a * dt; x[i] += v[i] * dt;
                            float s = 0.3f + 0.7f * x[i], cx = (i % 5 + 0.5f) * getWidth() / 5f, cy = (i / 5 + 0.5f) * getHeight() / 6f;
                            p.setColor(Color.HSVToColor(new float[] { i * 12, 0.7f, 1 }));
                            float r = 60 * s; c.drawRoundRect(new RectF(cx - r, cy - r, cx + r, cy + r), r * 0.45f, r * 0.45f, p);
                        }
                    }
                };
            }
        });
        scenes.add(new Scene("app_open_zoom", 21) {
            View make(Base m) {
                FrameLayout f = new FrameLayout(m); f.addView(new Backdrop(m));
                final View card = new Anim(m) { void paint(Canvas c, float t) { c.drawColor(0xfff0f0f0); p.setColor(0xff3060ff); c.drawRect(0, 0, getWidth(), getHeight() / 6f, p);
                    p.setColor(0xff202020); p.setTextSize(40); for (int i = 0; i < 20; i++) c.drawText("App content row " + i, 30, getHeight() / 6f + 60 + i * 60, p); } };
                card.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                card.setClipToOutline(true);
                final float[] rad = { 80 };
                card.setOutlineProvider(new ViewOutlineProvider() { public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), rad[0]); } });
                f.addView(card);
                card.post(new Runnable() { public void run() {
                    float k = spring((SystemClock.uptimeMillis() % 1400) / 1000f);
                    card.setPivotX(card.getWidth() * 0.2f); card.setPivotY(card.getHeight() * 0.8f);
                    card.setScaleX(0.15f + 0.85f * k); card.setScaleY(0.15f + 0.85f * k);
                    rad[0] = 120 * (1 - k) + 40; card.invalidateOutline(); card.setAlpha(Math.min(1, 0.3f + k));
                    card.postOnAnimation(this);
                } });
                return f;
            }
        });
        scenes.add(new Scene("smoke_particles", 21) {
            View make(Base m) {
                return new Anim(m) {
                    final int n = 2500; final float[] px = new float[n], py = new float[n], life = new float[n];
                    final Random rnd = new Random(1);
                    void paint(Canvas c, float t) {
                        c.drawColor(0xff0a0a12);
                        p.setMaskFilter(null);
                        for (int i = 0; i < n; i++) {
                            if (life[i] <= 0) { px[i] = getWidth() / 2f + rnd.nextFloat() * 80 - 40; py[i] = getHeight() * 0.9f; life[i] = 1 + rnd.nextFloat() * 2; }
                            life[i] -= 0.016f;
                            px[i] += (float) (Math.sin(py[i] * 0.01f + t * 1.7f) * 3 + Math.cos(i + t)); py[i] -= 3 + (i % 7);
                            int a = (int) (40 * Math.max(0, Math.min(1, life[i])));
                            p.setColor(Color.argb(a, 200, 210, 230));
                            c.drawCircle(px[i], py[i], 18 + (1 - life[i] / 3) * 40, p);
                        }
                    }
                };
            }
        });
        scenes.add(new Scene("dust_delete", 21) {
            View make(Base m) {
                return new Anim(m) {
                    final int n = 6000; final float[] pts = new float[n * 2], vel = new float[n * 2]; final Random r = new Random(2);
                    void paint(Canvas c, float t) {
                        c.drawColor(0xff181818);
                        float ph = t % 1.5f;
                        if (ph < 0.02f || pts[0] == 0) for (int i = 0; i < n; i++) { pts[i * 2] = getWidth() * 0.3f + r.nextFloat() * getWidth() * 0.4f; pts[i * 2 + 1] = getHeight() * 0.4f + r.nextFloat() * getHeight() * 0.2f; vel[i * 2] = r.nextFloat() * 8 - 2; vel[i * 2 + 1] = -r.nextFloat() * 6; }
                        for (int i = 0; i < n; i++) { pts[i * 2] += vel[i * 2]; pts[i * 2 + 1] += vel[i * 2 + 1]; vel[i * 2 + 1] += 0.15f; }
                        p.setColor(0xffffc040); p.setStrokeWidth(4); c.drawPoints(pts, p);
                    }
                };
            }
        });
        scenes.add(new Scene("shadows_elevation", 21) {
            View make(Base m) {
                FrameLayout f = new FrameLayout(m); f.setBackgroundColor(0xffe8e8ee);
                final List<View> cards = new ArrayList<View>();
                for (int i = 0; i < 24; i++) {
                    View v = new View(m); v.setBackgroundColor(0xffffffff);
                    v.setOutlineProvider(new ViewOutlineProvider() { public void getOutline(View w, Outline o) { o.setRoundRect(0, 0, w.getWidth(), w.getHeight(), 30); } });
                    if (Build.VERSION.SDK_INT >= 28) { v.setOutlineSpotShadowColor(Color.HSVToColor(new float[] { i * 15, 1, 1 })); v.setOutlineAmbientShadowColor(0xff0000ff); }
                    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(220, 160); lp.leftMargin = 40 + (i % 4) * 250; lp.topMargin = 60 + (i / 4) * 210;
                    f.addView(v, lp); cards.add(v);
                }
                f.post(new Runnable() { public void run() {
                    float t = SystemClock.uptimeMillis() / 1000f;
                    for (int i = 0; i < cards.size(); i++) cards.get(i).setTranslationZ((float) (20 + 18 * Math.sin(t * 3 + i)));
                    cards.get(0).postOnAnimation(this);
                } });
                return f;
            }
        });
        scenes.add(new Scene("rounded_clip_scroll", 21) {
            View make(Base m) {
                final FrameLayout f = new FrameLayout(m); f.setBackgroundColor(0xff101010);
                final Bitmap b = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
                Canvas bc = new Canvas(b); Paint bp = new Paint();
                bp.setShader(new LinearGradient(0, 0, 256, 256, 0xffff4080, 0xff40a0ff, Shader.TileMode.CLAMP)); bc.drawRect(0, 0, 256, 256, bp);
                final List<View> rows = new ArrayList<View>();
                for (int i = 0; i < 30; i++) {
                    android.widget.ImageView iv = new android.widget.ImageView(m); iv.setImageBitmap(b); iv.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                    iv.setClipToOutline(true);
                    iv.setOutlineProvider(new ViewOutlineProvider() { public void getOutline(View w, Outline o) { o.setRoundRect(0, 0, w.getWidth(), w.getHeight(), 48); } });
                    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, 180); lp.leftMargin = lp.rightMargin = 30; lp.topMargin = i * 200;
                    f.addView(iv, lp); rows.add(iv);
                }
                f.post(new Runnable() { public void run() {
                    float y = -((SystemClock.uptimeMillis() / 2) % 3000);
                    for (View r : rows) r.setTranslationY(y);
                    f.postOnAnimation(this);
                } });
                return f;
            }
        });
        scenes.add(new Scene("color_filter_aod", 21) {
            View make(Base m) {
                return new Anim(m) {
                    final ColorMatrix cm = new ColorMatrix();
                    void paint(Canvas c, float t) {
                        cm.setSaturation((float) (0.5 + 0.5 * Math.sin(t * 2)));
                        int save = c.saveLayer(0, 0, getWidth(), getHeight(), null);
                        backdrop(c, getWidth(), getHeight(), t, p);
                        Paint q = new Paint(); q.setColorFilter(new ColorMatrixColorFilter(cm));
                        c.restoreToCount(save);
                        int s2 = c.saveLayer(0, 0, getWidth(), getHeight(), q);
                        backdrop(c, getWidth(), getHeight(), t, p);
                        c.restoreToCount(s2);
                    }
                };
            }
        });
        scenes.add(new Scene("parallax_depth", 21) {
            View make(Base m) {
                return new Anim(m) {
                    void paint(Canvas c, float t) {
                        c.drawColor(0xff0b1020);
                        for (int layer = 0; layer < 3; layer++) {
                            float sp = (layer + 1) * 60, sz = 40 + layer * 40;
                            p.setColor(new int[] { 0x6040a0ff, 0x9060ffa0, 0xffffffff }[layer]);
                            if (layer == 0) p.setMaskFilter(new BlurMaskFilter(20, BlurMaskFilter.Blur.NORMAL)); else p.setMaskFilter(null);
                            for (int i = 0; i < 12; i++) c.drawCircle(((i * 173 + t * sp) % (getWidth() + 200)) - 100, (i * 97 + layer * 300) % getHeight(), sz, p);
                        }
                        p.setMaskFilter(null);
                    }
                };
            }
        });
        scenes.add(new Scene("texture_surface_mix", 21) {
            View make(final Base m) {
                FrameLayout f = new FrameLayout(m);
                final TextureView tv = new TextureView(m);
                f.addView(tv, new FrameLayout.LayoutParams(-1, -1));
                final android.view.SurfaceView sv = new android.view.SurfaceView(m);
                sv.setZOrderMediaOverlay(true); sv.getHolder().setFormat(android.graphics.PixelFormat.TRANSLUCENT);
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(500, 500); lp.leftMargin = 100; lp.topMargin = 300;
                f.addView(sv, lp);
                final View over = new Anim(m) { void paint(Canvas c, float t) { p.setColor(0x80ff0000); c.drawCircle(getWidth() / 2f + (float) Math.sin(t * 3) * 200, getHeight() / 2f, 120, p); } };
                f.addView(over, new FrameLayout.LayoutParams(-1, -1));
                final Paint pp = new Paint();
                final Runnable[] r = new Runnable[1];
                r[0] = new Runnable() { public void run() {
                    float t = SystemClock.uptimeMillis() / 1000f;
                    if (tv.isAvailable()) { Canvas c = tv.lockCanvas(); if (c != null) { backdrop(c, c.getWidth(), c.getHeight(), t, pp); tv.unlockCanvasAndPost(c); } }
                    if (sv.getHolder().getSurface().isValid()) { Canvas c = sv.getHolder().lockCanvas(); if (c != null) { c.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR); pp.setColor(0xa000ff80); c.drawCircle(250, 250, 150 + (float) Math.sin(t * 4) * 80, pp); sv.getHolder().unlockCanvasAndPost(c); } }
                    if (tv.getParent() != null) tv.postOnAnimation(r[0]);
                } };
                tv.post(r[0]);
                return f;
            }
        });
        scenes.add(new Scene("hardware_bitmaps", 26) {
            View make(Base m) { return hwBitmapScene(m); }
        });
        scenes.add(new Scene("variable_font_text", 26) {
            View make(Base m) {
                return new Anim(m) {
                    void paint(Canvas c, float t) {
                        c.drawColor(0xfffafafa); p.setColor(0xff101010);
                        for (int i = 0; i < 14; i++) {
                            p.setTextSize(30 + i * 4);
                            try { Paint.class.getMethod("setFontVariationSettings", String.class).invoke(p, "'wght' " + (int) (100 + 800 * (0.5 + 0.5 * Math.sin(t * 2 + i * 0.3)))); } catch (Throwable e) { }
                            p.setShadowLayer(8, 0, 4, 0x60000000);
                            c.drawText("Шрифт Font 字体 " + i, 20 + (t * 90 + i * 30) % 200, 80 + i * (getHeight() / 15f), p);
                        }
                        p.clearShadowLayer();
                    }
                };
            }
        });
    }

    /** spring 0 -> 1 with overshoot, as skins animate islands and app windows */
    static float spring(float t) { return (float) (1 - Math.exp(-6 * t) * Math.cos(9 * t)); }

    static RectF rectOf(View v) { return new RectF(v.getX(), v.getY(), v.getX() + v.getWidth(), v.getY() + v.getHeight()); }

    /** mode 0: RenderEffect blur; mode 1: AGSL liquid glass (refraction, chromatic edge, specular rim, frosted) */
    View glassScene(Base m, int mode) {
        FrameLayout f = new FrameLayout(m);
        f.addView(new Backdrop(m));
        final View glass = new GlassContent(m);
        DisplayFix.size(m);
        int w = DisplayFix.w * 2 / 3, h = DisplayFix.h / 4;
        f.addView(glass, glassLp(w, h));
        try {
            if (mode == 0) Modern.blur(glass, 40);
            else Modern.liquidGlass(glass, w, h);
        } catch (Throwable t) { m.line("fx.glass.error", t); }
        glass.post(new Runnable() { public void run() {
            float t = SystemClock.uptimeMillis() / 1000f;
            glass.setTranslationX((float) Math.sin(t * 1.3) * 60); glass.setTranslationY((float) Math.cos(t) * 120);
            glass.postOnAnimation(this);
        } });
        return f;
    }

    /** the pre-12 way (MIUI's own blur before RenderEffect): a small copy of what is under the panel, blurred by RenderScript */
    View rsBlurScene(final Base m) {
        return new Anim(m) {
            android.renderscript.RenderScript rs; android.renderscript.ScriptIntrinsicBlur blur;
            Bitmap small, out; Canvas sc;
            void paint(Canvas c, float t) {
                int w = getWidth(), h = getHeight();
                backdrop(c, w, h, t, p);
                RectF g = new RectF(w * 0.15f, h * 0.3f, w * 0.85f, h * 0.6f);
                if (rs == null) {
                    rs = android.renderscript.RenderScript.create(getContext());
                    blur = android.renderscript.ScriptIntrinsicBlur.create(rs, android.renderscript.Element.U8_4(rs));
                    small = Bitmap.createBitmap((int) (g.width() / 4), (int) (g.height() / 4), Bitmap.Config.ARGB_8888);
                    out = Bitmap.createBitmap(small.getWidth(), small.getHeight(), Bitmap.Config.ARGB_8888); sc = new Canvas(small);
                }
                sc.save(); sc.scale(0.25f, 0.25f); sc.translate(-g.left, -g.top); backdrop(sc, w, h, t, new Paint()); sc.restore();
                android.renderscript.Allocation a = android.renderscript.Allocation.createFromBitmap(rs, small), b = android.renderscript.Allocation.createFromBitmap(rs, out);
                blur.setRadius(12); blur.setInput(a); blur.forEach(b); b.copyTo(out); a.destroy(); b.destroy();
                c.save(); Path clip = new Path(); clip.addRoundRect(g, 60, 60, Path.Direction.CW); c.clipPath(clip);
                c.drawBitmap(out, null, g, new Paint(Paint.FILTER_BITMAP_FLAG)); p.setColor(0x30ffffff); c.drawRect(g, p); c.restore();
            }
        };
    }

    @TargetApi(26)
    View hwBitmapScene(Base m) {
        Bitmap s = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(s); Paint p = new Paint(); backdrop(c, 512, 512, 0, p);
        final Bitmap hw = s.copy(Bitmap.Config.HARDWARE, false);
        m.line("fx.hardware_bitmaps.config", hw == null ? "null" : String.valueOf(hw.getConfig()));
        return new Anim(m) {
            void paint(Canvas c, float t) {
                c.drawColor(0xff000000);
                for (int i = 0; i < 9; i++) { c.save(); c.translate((i % 3) * getWidth() / 3f + 150, (i / 3) * getHeight() / 3f + 200); c.rotate(t * 40 * (i + 1));
                    if (hw != null) c.drawBitmap(hw, -128, -128, null); c.restore(); }
            }
        };
    }

    // ---- running ----
    void next() {
        if (at >= 0) { Scene s = scenes.get(at); try { s.end(); } catch (Throwable t) { } }
        for (int i = m.root.getChildCount() - 1; i >= 0; i--) if (m.root.getChildAt(i) != m.status) m.root.removeViewAt(i);
        at++;
        if (at >= scenes.size()) { m.done(); return; }
        final Scene s = scenes.get(at);
        final String k = "fx." + s.name;
        if (sdk < s.minSdk) { m.line(k, "n/a (needs API " + s.minSdk + ")"); next(); return; }
        final View v;
        try { v = s.make(m); } catch (Throwable t) { m.line(k, "FAIL " + t); next(); return; }
        m.root.addView(v, 0, new FrameLayout.LayoutParams(-1, -1));
        m.say("fx " + (at + 1) + "/" + scenes.size() + ": " + s.name);
        final long[] ts = new long[2000]; final int[] n = { 0 };
        final long warm = SystemClock.uptimeMillis() + 400, end = warm + sceneMs;
        final Bitmap[] first = new Bitmap[1];
        Choreographer.getInstance().postFrameCallback(new Choreographer.FrameCallback() {
            public void doFrame(long t) {
                long now = SystemClock.uptimeMillis();
                if (now >= warm && n[0] < ts.length) ts[n[0]++] = t;
                if (n[0] == 2) snap(new Done() { public void got(Bitmap b) { first[0] = b; } });
                if (now < end) { Choreographer.getInstance().postFrameCallback(this); return; }
                m.frames(k, ts, n[0], m.hz());
                snap(new Done() { public void got(Bitmap b) {
                    RectF g = s.glass(v);
                    if (g != null) { int[] loc = new int[2]; v.getLocationInWindow(loc); g.offset(loc[0], loc[1]); }   // the copy is of the whole window
                    m.line(k + ".check", check(b, first[0], g, s.blurLimit()));
                    m.h.post(new Runnable() { public void run() { next(); } });
                } });
            }
        });
    }

    interface Done { void got(Bitmap b); }

    /** the window as the compositor shows it (8.0+); before that the check is skipped */
    void snap(final Done d) {
        if (sdk < 26) { d.got(null); return; }
        try { Modern.pixelCopy(m, d); } catch (Throwable t) { d.got(null); }
    }

    /** not blank, changed since the scene began, and blurred where the glass is */
    static String check(Bitmap b, Bitmap first, RectF glass, double limit) {
        if (b == null) return "unverified (no PixelCopy before 8.0)";
        int w = b.getWidth(), h = b.getHeight();
        long sum = 0, sq = 0; int cnt = 0;
        for (int y = 0; y < h; y += 7) for (int x = 0; x < w; x += 7) { int c = b.getPixel(x, y); int l = (Color.red(c) + Color.green(c) + Color.blue(c)) / 3; sum += l; sq += l * l; cnt++; }
        double mean = sum / (double) cnt, var = sq / (double) cnt - mean * mean;
        if (var < 20) return "FAIL blank (variance " + (int) var + ")";
        String moved = "";
        if (first != null && first.getWidth() == w) {
            int diff = 0, all = 0;
            for (int y = 0; y < h; y += 11) for (int x = 0; x < w; x += 11) { all++; if (Math.abs(first.getPixel(x, y) - b.getPixel(x, y)) > 0) diff++; }
            if (diff * 100 < all) return "FAIL frozen (" + diff + "/" + all + " samples changed)";
            moved = " moved " + diff * 100 / all + "%";
        }
        String blur = "";
        if (glass != null) {
            double in = detail(b, glass, true), out = detail(b, glass, false);
            if (in > out * limit) return "FAIL not blurred (detail inside " + (int) in + " outside " + (int) out + " limit " + (int) (out * limit) + ")";
            blur = " blur detail " + (int) in + "/" + (int) out;
        }
        return "ok var " + (int) var + moved + blur;
    }

    /** mean neighbour difference (fine detail) inside or outside r */
    static double detail(Bitmap b, RectF r, boolean inside) {
        long s = 0; int n = 0;
        RectF in = new RectF(r.left + 20, r.top + 20, r.right - 20, r.bottom - 20);
        for (int y = 2; y < b.getHeight() - 2; y += 3) for (int x = 2; x < b.getWidth() - 2; x += 3) {
            if (in.contains(x, y) != inside) continue;
            if (!inside && r.contains(x, y)) continue;
            int c = b.getPixel(x, y), d = b.getPixel(x + 1, y);
            s += Math.abs(Color.green(c) - Color.green(d)) + Math.abs(Color.red(c) - Color.red(d)); n++;
        }
        return n == 0 ? 0 : s * 100.0 / n;
    }

    static final class DisplayFix {
        static int w, h;
        static void size(Base m) { android.util.DisplayMetrics d = m.getResources().getDisplayMetrics(); w = d.widthPixels; h = d.heightPixels; }
    }

    /** the calls of newer releases, apart so that older ones never verify them */
    static final class Modern {
        @TargetApi(31)
        static void blur(View v, float r) {
            v.setRenderEffect(android.graphics.RenderEffect.createBlurEffect(r, r, Shader.TileMode.CLAMP));
        }

        static final String GLASS =
            "uniform shader content; uniform float2 size; uniform float time;\n" +
            "float sdRound(float2 p, float2 b, float r) { float2 q = abs(p) - b + r; return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r; }\n" +
            "half4 main(float2 xy) {\n" +
            "  float2 c = size * 0.5; float2 p = xy - c;\n" +
            "  float d = sdRound(p, c - 4.0, min(c.x, c.y) * 0.8);\n" +
            "  if (d > 0.0) return half4(0);\n" +
            "  float edge = clamp(-d / 40.0, 0.0, 1.0);\n" +
            "  float2 n = normalize(p + 0.001) * (1.0 - edge);\n" +              // lens: bend most at the rim
            "  float2 off = n * 38.0;\n" +
            "  half4 acc = half4(0);\n" +
            "  for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) acc += content.eval(xy - off + float2(i, j) * 5.0);\n" +   // frosted
            "  acc /= 25.0;\n" +
            "  half r = content.eval(xy - off * 1.08).r; half b = content.eval(xy - off * 0.92).b;\n" +      // chromatic edge
            "  half4 col = half4(mix(acc.r, r, 0.35), acc.g, mix(acc.b, b, 0.35), 1.0);\n" +
            "  float spec = pow(clamp(1.0 - abs(d + 6.0) / 6.0, 0.0, 1.0), 2.0) * (0.6 + 0.4 * sin(time * 2.0 + p.x * 0.02));\n" +   // specular rim
            "  return col * 0.9 + half4(0.12) + half4(spec);\n" +
            "}\n";

        @TargetApi(33)
        static void liquidGlass(final View v, int w, int h) {
            final android.graphics.RuntimeShader s = new android.graphics.RuntimeShader(GLASS);
            s.setFloatUniform("size", w, h);
            v.post(new Runnable() { public void run() {
                s.setFloatUniform("time", SystemClock.uptimeMillis() / 1000f);
                v.setRenderEffect(android.graphics.RenderEffect.createRuntimeShaderEffect(s, "content"));
                if (v.getParent() != null) v.postOnAnimation(this);
            } });
        }

        @TargetApi(26)
        static void pixelCopy(final Base m, final Done d) {
            View r = m.getWindow().getDecorView();
            final Bitmap b = Bitmap.createBitmap(r.getWidth(), r.getHeight(), Bitmap.Config.ARGB_8888);
            android.view.PixelCopy.request(m.getWindow(), b, new android.view.PixelCopy.OnPixelCopyFinishedListener() {
                public void onPixelCopyFinished(int res) { d.got(res == android.view.PixelCopy.SUCCESS ? b : null); }
            }, new Handler(android.os.Looper.getMainLooper()));
        }
    }
}
