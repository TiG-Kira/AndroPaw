package app.aemu.test;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Test "glcheck" (API 17 and later): EGL and GLES 2 one feature at a time, off the screen (a 64 x 64 pbuffer), each
 * drawing read back and compared with the colour it must give. One line per check: glcheck.<name>=ok or what was
 * read; glcheck.failed is the count.
 */
class GlCheck {
    final Base m;
    int failed, prog, uColor, uTex, uMode;
    EGLDisplay dpy;
    EGLConfig cfg;
    final FloatBuffer quad = Base.floats(new float[] { -1, -1, 1, -1, -1, 1, 1, 1 });

    GlCheck(Base m) { this.m = m; }

    static void start(final Base m) {
        m.bg(new Runnable() { public void run() { new GlCheck(m).run(); } });
    }

    void ok(String name, boolean good, String got) {
        if (!good) failed++;
        m.line("glcheck." + name, good ? "ok" : "FAIL " + got);
    }

    /** the pixel at x, y (0, 0 is the bottom left) as rrggbbaa */
    static String px(int x, int y) {
        ByteBuffer b = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder());
        GLES20.glReadPixels(x, y, 1, 1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, b);
        return String.format("%02x%02x%02x%02x", b.get(0) & 0xff, b.get(1) & 0xff, b.get(2) & 0xff, b.get(3) & 0xff);
    }

    /** each channel of the pixel within tol of r, g, b */
    void pixel(String name, int x, int y, int r, int g, int b, int tol) {
        String p = px(x, y);
        int pr = Integer.parseInt(p.substring(0, 2), 16), pg = Integer.parseInt(p.substring(2, 4), 16), pb = Integer.parseInt(p.substring(4, 6), 16);
        ok(name, Math.abs(pr - r) <= tol && Math.abs(pg - g) <= tol && Math.abs(pb - b) <= tol, p + " at " + x + "," + y + ", must be " + String.format("%02x%02x%02x", r, g, b));
    }

    static int shader(int type, String src) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, src);
        GLES20.glCompileShader(s);
        return s;
    }

    int program(String vs, String fs, String name) {
        int p = GLES20.glCreateProgram();
        GLES20.glAttachShader(p, shader(GLES20.GL_VERTEX_SHADER, vs));
        GLES20.glAttachShader(p, shader(GLES20.GL_FRAGMENT_SHADER, fs));
        GLES20.glBindAttribLocation(p, 0, "p");
        GLES20.glLinkProgram(p);
        int[] st = new int[1];
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, st, 0);
        if (st[0] == 0) { ok("link_" + name, false, GLES20.glGetProgramInfoLog(p).replace('\n', ' ')); return 0; }
        return p;
    }

    /** the whole surface (or the viewport) with the plain colour (mode 0), the 2D texture (1) or the cube map's +X (2) */
    void draw(int mode, float r, float g, float b, float a, float z) {
        GLES20.glUseProgram(prog);
        GLES20.glUniform4f(uColor, r, g, b, a);
        GLES20.glUniform1i(uMode, mode);
        GLES20.glUniform1f(GLES20.glGetUniformLocation(prog, "z"), z);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glEnableVertexAttribArray(0);
        GLES20.glVertexAttribPointer(0, 2, GLES20.GL_FLOAT, false, 0, quad);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }

    static ByteBuffer bytes(int... v) {
        ByteBuffer b = ByteBuffer.allocateDirect(v.length).order(ByteOrder.nativeOrder());
        for (int i = 0; i < v.length; i++) b.put((byte) v[i]);
        b.position(0);
        return b;
    }

    int texture(int target) {
        int[] t = new int[1];
        GLES20.glGenTextures(1, t, 0);
        GLES20.glBindTexture(target, t[0]);
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        return t[0];
    }

    void clear() {
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST); GLES20.glDisable(GLES20.GL_BLEND); GLES20.glDisable(GLES20.GL_DEPTH_TEST); GLES20.glDisable(GLES20.GL_STENCIL_TEST);
        GLES20.glClearColor(0, 0, 0, 1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_STENCIL_BUFFER_BIT);
    }

    void run() {
        dpy = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        int[] ver = new int[2];
        if (!EGL14.eglInitialize(dpy, ver, 0, ver, 1)) { ok("egl_init", false, "0x" + Integer.toHexString(EGL14.eglGetError())); return; }
        m.line("glcheck.egl_version", ver[0] + "." + ver[1]);
        int[] n = new int[1];
        EGL14.eglGetConfigs(dpy, null, 0, 0, n, 0);
        m.line("glcheck.egl_configs", n[0]);
        EGLConfig[] cs = new EGLConfig[1];
        int[] want = { EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT, EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8, EGL14.EGL_ALPHA_SIZE, 8, EGL14.EGL_DEPTH_SIZE, 16, EGL14.EGL_STENCIL_SIZE, 8, EGL14.EGL_NONE };
        if (!EGL14.eglChooseConfig(dpy, want, 0, cs, 0, 1, n, 0) || n[0] < 1) { ok("egl_config_8888_d16_s8", false, "none, 0x" + Integer.toHexString(EGL14.eglGetError())); return; }
        ok("egl_config_8888_d16_s8", true, "");
        cfg = cs[0];
        int[] w565 = { EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, EGL14.EGL_RED_SIZE, 5, EGL14.EGL_GREEN_SIZE, 6, EGL14.EGL_BLUE_SIZE, 5, EGL14.EGL_NONE };
        EGLConfig[] c2 = new EGLConfig[1];
        ok("egl_config_565", EGL14.eglChooseConfig(dpy, w565, 0, c2, 0, 1, n, 0) && n[0] >= 1, "none");
        int[] pb = { EGL14.EGL_WIDTH, 64, EGL14.EGL_HEIGHT, 64, EGL14.EGL_NONE }, ca = { EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE };
        final EGLSurface surf = EGL14.eglCreatePbufferSurface(dpy, cfg, pb, 0);
        final EGLContext ctx = EGL14.eglCreateContext(dpy, cfg, EGL14.EGL_NO_CONTEXT, ca, 0);
        if (surf == EGL14.EGL_NO_SURFACE || ctx == EGL14.EGL_NO_CONTEXT || !EGL14.eglMakeCurrent(dpy, surf, surf, ctx)) { ok("egl_pbuffer_context", false, "0x" + Integer.toHexString(EGL14.eglGetError())); return; }
        ok("egl_pbuffer_context", true, "");
        int[] q = new int[1];
        EGL14.eglQuerySurface(dpy, surf, EGL14.EGL_WIDTH, q, 0);
        ok("egl_query_surface", q[0] == 64, "width " + q[0]);
        // a wrong call must say so and must not break the context
        EGL14.eglGetError();
        boolean bad = EGL14.eglMakeCurrent(dpy, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, ctx);
        int e = EGL14.eglGetError();
        EGL14.eglMakeCurrent(dpy, surf, surf, ctx);
        m.line("glcheck.egl_surfaceless", bad ? "accepted" : "refused 0x" + Integer.toHexString(e));
        try { gles(); } catch (Throwable t) { ok("exception", false, t.toString()); }
        shared(ctx, ca);
        EGL14.eglMakeCurrent(dpy, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
        ok("egl_destroy", EGL14.eglDestroySurface(dpy, surf) && EGL14.eglDestroyContext(dpy, ctx), "0x" + Integer.toHexString(EGL14.eglGetError()));
        m.line("glcheck.failed", failed);
    }

    void gles() {
        m.line("glcheck.renderer", GLES20.glGetString(GLES20.GL_RENDERER));
        m.line("glcheck.version", GLES20.glGetString(GLES20.GL_VERSION));
        m.line("glcheck.glsl", GLES20.glGetString(GLES20.GL_SHADING_LANGUAGE_VERSION));
        String ext = " " + GLES20.glGetString(GLES20.GL_EXTENSIONS) + " ";
        GLES20.glViewport(0, 0, 64, 64);
        GLES20.glDisable(GLES20.GL_DITHER);

        GLES20.glClearColor(1, 0, 0, 1); GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        pixel("clear", 10, 10, 255, 0, 0, 1);

        prog = program("attribute vec2 p; uniform float z; varying vec2 t; void main() { t = p * 0.5 + 0.5; gl_Position = vec4(p, z, 1.0); }",
            "precision mediump float; uniform vec4 color; uniform int mode; uniform sampler2D tex; uniform samplerCube cube; varying vec2 t;"
            + " void main() { if (mode == 1) gl_FragColor = texture2D(tex, t); else if (mode == 2) gl_FragColor = textureCube(cube, vec3(1.0, t * 2.0 - 1.0)); else gl_FragColor = color; }", "main");
        if (prog == 0) return;
        uColor = GLES20.glGetUniformLocation(prog, "color"); uMode = GLES20.glGetUniformLocation(prog, "mode");
        GLES20.glUseProgram(prog);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(prog, "tex"), 0);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(prog, "cube"), 1);

        clear(); draw(0, 0, 1, 0, 1, 0);
        pixel("draw_uniform_color", 32, 32, 0, 255, 0, 1);

        clear(); GLES20.glEnable(GLES20.GL_SCISSOR_TEST); GLES20.glScissor(0, 0, 32, 64); draw(0, 0, 0, 1, 1, 0);
        pixel("scissor_in", 10, 30, 0, 0, 255, 1); pixel("scissor_out", 50, 30, 0, 0, 0, 1);

        clear(); GLES20.glViewport(32, 32, 32, 32); draw(0, 1, 1, 0, 1, 0); GLES20.glViewport(0, 0, 64, 64);
        pixel("viewport_in", 48, 48, 255, 255, 0, 1); pixel("viewport_out", 16, 16, 0, 0, 0, 1);

        clear(); GLES20.glEnable(GLES20.GL_BLEND); GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA); draw(0, 1, 1, 1, 0.5f, 0);
        pixel("blend_half", 32, 32, 128, 128, 128, 3);

        clear(); GLES20.glEnable(GLES20.GL_DEPTH_TEST); GLES20.glDepthFunc(GLES20.GL_LESS);
        draw(0, 1, 0, 0, 1, -0.5f); draw(0, 0, 0, 1, 1, 0.5f);
        pixel("depth_test", 32, 32, 255, 0, 0, 1);

        clear(); GLES20.glEnable(GLES20.GL_STENCIL_TEST); GLES20.glEnable(GLES20.GL_SCISSOR_TEST); GLES20.glScissor(0, 0, 32, 64);
        GLES20.glStencilFunc(GLES20.GL_ALWAYS, 1, 0xff); GLES20.glStencilOp(GLES20.GL_KEEP, GLES20.GL_KEEP, GLES20.GL_REPLACE);
        GLES20.glColorMask(false, false, false, false); draw(0, 1, 1, 1, 1, 0); GLES20.glColorMask(true, true, true, true);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
        GLES20.glStencilFunc(GLES20.GL_EQUAL, 1, 0xff); GLES20.glStencilOp(GLES20.GL_KEEP, GLES20.GL_KEEP, GLES20.GL_KEEP);
        draw(0, 1, 0, 1, 1, 0);
        pixel("stencil_in", 10, 30, 255, 0, 255, 1); pixel("stencil_out", 50, 30, 0, 0, 0, 1);

        // textures: 2 x 2, the bottom row first - red, green / blue, white
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
        int t = texture(GLES20.GL_TEXTURE_2D);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 2, 2, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
            bytes(255, 0, 0, 255, 0, 255, 0, 255, 0, 0, 255, 255, 255, 255, 255, 255));
        clear(); draw(1, 0, 0, 0, 0, 0);
        pixel("tex_rgba_00", 16, 16, 255, 0, 0, 1); pixel("tex_rgba_10", 48, 16, 0, 255, 0, 1); pixel("tex_rgba_01", 16, 48, 0, 0, 255, 1); pixel("tex_rgba_11", 48, 48, 255, 255, 255, 1);
        GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 1, 1, 1, 1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bytes(255, 128, 0, 255));
        clear(); draw(1, 0, 0, 0, 0, 0);
        pixel("tex_sub_image", 48, 48, 255, 128, 0, 2);

        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, 1, 1, 0, GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, bytes(10, 200, 90));
        clear(); draw(1, 0, 0, 0, 0, 0); pixel("tex_rgb888", 32, 32, 10, 200, 90, 1);
        ShortBuffer s565 = ByteBuffer.allocateDirect(2).order(ByteOrder.nativeOrder()).asShortBuffer(); s565.put((short) 0xf800).position(0);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, 1, 1, 0, GLES20.GL_RGB, GLES20.GL_UNSIGNED_SHORT_5_6_5, s565);
        clear(); draw(1, 0, 0, 0, 0, 0); pixel("tex_rgb565", 32, 32, 255, 0, 0, 1);
        ShortBuffer s4444 = ByteBuffer.allocateDirect(2).order(ByteOrder.nativeOrder()).asShortBuffer(); s4444.put((short) 0x0f0f).position(0);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 1, 1, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_SHORT_4_4_4_4, s4444);
        clear(); draw(1, 0, 0, 0, 0, 0); pixel("tex_rgba4444", 32, 32, 0, 255, 0, 1);
        ShortBuffer s5551 = ByteBuffer.allocateDirect(2).order(ByteOrder.nativeOrder()).asShortBuffer(); s5551.put((short) 0x003f).position(0);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 1, 1, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_SHORT_5_5_5_1, s5551);
        clear(); draw(1, 0, 0, 0, 0, 0); pixel("tex_rgba5551", 32, 32, 0, 0, 255, 1);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, 1, 1, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, bytes(77));
        clear(); draw(1, 0, 0, 0, 0, 0); pixel("tex_luminance", 32, 32, 77, 77, 77, 1);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE_ALPHA, 1, 1, 0, GLES20.GL_LUMINANCE_ALPHA, GLES20.GL_UNSIGNED_BYTE, bytes(200, 255));
        clear(); draw(1, 0, 0, 0, 0, 0); pixel("tex_luminance_alpha", 32, 32, 200, 200, 200, 1);

        // a texture whose sides are not powers of two (3 x 5, all one colour), row alignment 1
        ByteBuffer np = ByteBuffer.allocateDirect(3 * 5 * 3).order(ByteOrder.nativeOrder());
        for (int i = 0; i < 15; i++) { np.put((byte) 20); np.put((byte) 40); np.put((byte) 250); }
        np.position(0);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, 3, 5, 0, GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, np);
        clear(); draw(1, 0, 0, 0, 0, 0); pixel("tex_npot", 60, 60, 20, 40, 250, 1);

        // mip levels made by the driver: a 4 x 4 texture drawn into 2 x 2 pixels with a mip filter
        ByteBuffer mp = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder());
        for (int i = 0; i < 16; i++) { mp.put((byte) 0); mp.put((byte) 255); mp.put((byte) 255); mp.put((byte) 255); }
        mp.position(0);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 4, 4, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mp);
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
        clear(); GLES20.glViewport(0, 0, 2, 2); draw(1, 0, 0, 0, 0, 0); GLES20.glViewport(0, 0, 64, 64);
        pixel("tex_mipmap", 0, 0, 0, 255, 255, 2);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);

        // a cube map: +X yellow, the other faces black
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        texture(GLES20.GL_TEXTURE_CUBE_MAP);
        for (int f = 0; f < 6; f++) GLES20.glTexImage2D(GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_X + f, 0, GLES20.GL_RGBA, 1, 1, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
            f == 0 ? bytes(255, 255, 0, 255) : bytes(0, 0, 0, 255));
        clear(); draw(2, 0, 0, 0, 0, 0); pixel("tex_cube_map", 32, 32, 255, 255, 0, 1);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        // the framebuffer copied into a texture
        clear(); draw(0, 0.2f, 0.4f, 0.6f, 1, 0);
        GLES20.glCopyTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, 0, 0, 16, 16, 0);
        clear(); draw(1, 0, 0, 0, 0, 0); pixel("copy_tex_image", 32, 32, 51, 102, 153, 2);

        // drawing into a texture through a framebuffer object with depth and stencil, then using that texture
        int[] fb = new int[1], rb = new int[2];
        int ft = texture(GLES20.GL_TEXTURE_2D);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 32, 32, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glGenFramebuffers(1, fb, 0); GLES20.glGenRenderbuffers(2, rb, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fb[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, ft, 0);
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, rb[0]);
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, 32, 32);
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, rb[0]);
        int st = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        ok("fbo_depth16_complete", st == GLES20.GL_FRAMEBUFFER_COMPLETE, "0x" + Integer.toHexString(st));
        GLES20.glViewport(0, 0, 32, 32);
        clear(); GLES20.glEnable(GLES20.GL_DEPTH_TEST); draw(0, 0, 1, 0, 1, -0.5f); draw(0, 1, 0, 0, 1, 0.5f);
        pixel("fbo_draw_depth", 16, 16, 0, 255, 0, 1);
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, rb[1]);
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_STENCIL_INDEX8, 32, 32);
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_STENCIL_ATTACHMENT, GLES20.GL_RENDERBUFFER, rb[1]);
        st = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        m.line("glcheck.fbo_depth16_stencil8", st == GLES20.GL_FRAMEBUFFER_COMPLETE ? "complete" : "0x" + Integer.toHexString(st) + " (separate buffers are optional)");
        if (ext.indexOf(" GL_OES_packed_depth_stencil ") >= 0) {
            GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_STENCIL_ATTACHMENT, GLES20.GL_RENDERBUFFER, 0);
            GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, rb[0]);
            GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, 0x88F0 /* DEPTH24_STENCIL8_OES */, 32, 32);
            GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, rb[0]);
            GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_STENCIL_ATTACHMENT, GLES20.GL_RENDERBUFFER, rb[0]);
            st = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
            ok("fbo_packed_depth_stencil", st == GLES20.GL_FRAMEBUFFER_COMPLETE, "0x" + Integer.toHexString(st));
        } else m.line("glcheck.fbo_packed_depth_stencil", "n/a");
        clear(); draw(0, 0, 1, 1, 1, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glViewport(0, 0, 64, 64);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ft);
        clear(); draw(1, 0, 0, 0, 0, 0); pixel("fbo_texture_used", 32, 32, 0, 255, 255, 1);
        GLES20.glDeleteFramebuffers(1, fb, 0); GLES20.glDeleteRenderbuffers(2, rb, 0);

        // buffers: positions from a vertex buffer, indices from an index buffer
        int[] bo = new int[2];
        GLES20.glGenBuffers(2, bo, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bo[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 32, quad, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, bo[1]);
        ShortBuffer ix = ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder()).asShortBuffer();
        ix.put(new short[] { 0, 1, 2, 2, 1, 3 }).position(0);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, 12, ix, GLES20.GL_STATIC_DRAW);
        clear();
        GLES20.glUseProgram(prog); GLES20.glUniform4f(uColor, 1, 0.5f, 0, 1); GLES20.glUniform1i(uMode, 0); GLES20.glUniform1f(GLES20.glGetUniformLocation(prog, "z"), 0);
        GLES20.glEnableVertexAttribArray(0);
        GLES20.glVertexAttribPointer(0, 2, GLES20.GL_FLOAT, false, 0, 0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, 0);
        pixel("vbo_ibo_draw_elements", 32, 32, 255, 128, 0, 2);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0); GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        GLES20.glDeleteBuffers(2, bo, 0);

        // shader language: a loop, an array of uniforms, discard
        int p2 = program("attribute vec2 p; varying vec2 t; void main() { t = p * 0.5 + 0.5; gl_Position = vec4(p, 0.0, 1.0); }",
            "precision mediump float; uniform vec3 c[3]; varying vec2 t; void main() { if (t.x > 0.5) discard; vec3 s = vec3(0.0); for (int i = 0; i < 3; i++) s += c[i]; gl_FragColor = vec4(s, 1.0); }", "loop");
        if (p2 != 0) {
            clear(); GLES20.glUseProgram(p2);
            GLES20.glUniform3fv(GLES20.glGetUniformLocation(p2, "c"), 3, new float[] { 0.25f, 0, 0, 0.25f, 0.5f, 0, 0, 0, 1 }, 0);
            GLES20.glEnableVertexAttribArray(0); GLES20.glVertexAttribPointer(0, 2, GLES20.GL_FLOAT, false, 0, quad);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            pixel("shader_loop_uniform_array", 10, 30, 128, 128, 255, 3); pixel("shader_discard", 50, 30, 0, 0, 0, 1);
        }
        int bs = shader(GLES20.GL_FRAGMENT_SHADER, "this is not a shader");
        int[] cst = new int[1];
        GLES20.glGetShaderiv(bs, GLES20.GL_COMPILE_STATUS, cst, 0);
        ok("shader_error_reported", cst[0] == 0, "compiled");
        int[] range = new int[2], prec = new int[1];
        GLES20.glGetShaderPrecisionFormat(GLES20.GL_FRAGMENT_SHADER, GLES20.GL_HIGH_FLOAT, range, 0, prec, 0);
        m.line("glcheck.highp_float_fragment", prec[0] > 0 ? "precision " + prec[0] : "none");

        // errors: a wrong enum is reported once and nothing else is
        while (GLES20.glGetError() != 0) { }
        GLES20.glEnable(0x1234);
        int e1 = GLES20.glGetError(), e2 = GLES20.glGetError();
        ok("error_invalid_enum", e1 == GLES20.GL_INVALID_ENUM && e2 == 0, "0x" + Integer.toHexString(e1) + ", then 0x" + Integer.toHexString(e2));
        int[] v = new int[2];
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, v, 0);
        ok("max_texture_size", v[0] >= 2048, String.valueOf(v[0]));
        GLES20.glGetIntegerv(GLES20.GL_MAX_VIEWPORT_DIMS, v, 0);
        ok("max_viewport", v[0] >= 64 && v[1] >= 64, v[0] + "x" + v[1]);
        // every extension the driver names: the list must be a clean one (no empty or doubled names)
        String[] names = ext.trim().split(" +");
        java.util.HashSet<String> seen = new java.util.HashSet<String>();
        int doubled = 0;
        for (int i = 0; i < names.length; i++) if (!seen.add(names[i])) doubled++;
        m.line("glcheck.extensions", names.length);
        ok("extensions_unique", doubled == 0, doubled + " names twice");
    }

    /** a second context sharing with the first, made current on another thread: it sees the first one's texture */
    void shared(final EGLContext first, final int[] ca) {
        final int tex = texture(GLES20.GL_TEXTURE_2D);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 1, 1, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bytes(90, 180, 30, 255));
        GLES20.glFinish();
        final String[] got = { "thread did not finish" };
        Thread th = new Thread(new Runnable() { public void run() {
            EGLContext c2 = EGL14.eglCreateContext(dpy, cfg, first, ca, 0);
            EGLSurface s2 = EGL14.eglCreatePbufferSurface(dpy, cfg, new int[] { EGL14.EGL_WIDTH, 8, EGL14.EGL_HEIGHT, 8, EGL14.EGL_NONE }, 0);
            if (c2 == EGL14.EGL_NO_CONTEXT || !EGL14.eglMakeCurrent(dpy, s2, s2, c2)) { got[0] = "no shared context: 0x" + Integer.toHexString(EGL14.eglGetError()); return; }
            GlCheck g = new GlCheck(m);
            g.prog = g.program("attribute vec2 p; varying vec2 t; void main() { t = p * 0.5 + 0.5; gl_Position = vec4(p, 0.0, 1.0); }",
                "precision mediump float; uniform sampler2D tex; varying vec2 t; void main() { gl_FragColor = texture2D(tex, t); }", "shared");
            GLES20.glViewport(0, 0, 8, 8);
            GLES20.glUseProgram(g.prog);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex);
            GLES20.glEnableVertexAttribArray(0); GLES20.glVertexAttribPointer(0, 2, GLES20.GL_FLOAT, false, 0, g.quad);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            got[0] = px(4, 4);
            EGL14.eglMakeCurrent(dpy, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroySurface(dpy, s2); EGL14.eglDestroyContext(dpy, c2);
            EGL14.eglReleaseThread();
        } }, "glcheck-shared");
        th.start();
        try { th.join(10000); } catch (InterruptedException e) { }
        ok("egl_shared_context_other_thread", got[0].startsWith("5ab41e"), got[0] + ", must be 5ab41e");
    }
}
