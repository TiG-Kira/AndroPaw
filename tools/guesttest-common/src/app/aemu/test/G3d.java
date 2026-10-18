package app.aemu.test;

import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLES31;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.view.Surface;
import android.widget.FrameLayout;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

/**
 * 3D the way games and skins use the GPU (API 21+): draw-call storms, instancing, heavy fragment work, MSAA,
 * post-processing chains (bloom), shadow maps, huge mipmapped textures, ETC2, transform feedback, compute (ES 3.1),
 * float targets, MRT, texture arrays, async readback, uncapped swaps, a second context uploading textures, the
 * external-texture path of video and camera (SurfaceTexture), and a lost context coming back. Each scene: frames
 * (g3d.<scene>.fps ...), then GL errors and pixels read back against what the scene must have produced (.check).
 */
final class G3d {
    final Base m;
    final int es;
    final List<S> scenes = new ArrayList<S>();
    int at = -1;
    final long ms;
    GLSurfaceView view;

    abstract static class S {
        final String name; final int needEs; final int samples;
        S(String n, int es) { this(n, es, 0); }
        S(String n, int es, int samples) { name = n; needEs = es; this.samples = samples; }
        int w, h; long frames;
        abstract void init() throws Exception;
        abstract void draw(float t);
        /** after the last frame, on the GL thread: "ok ..." or "FAIL ..." */
        String check() { return center("non-black", new int[] { -1 }); }
        void pause(GLSurfaceView v) { }
        String center(String what, int[] want) {
            ByteBuffer b = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder());
            GLES20.glReadPixels(w / 2, h / 2, 1, 1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, b);
            int r = b.get(0) & 255, g = b.get(1) & 255, bl = b.get(2) & 255;
            if (want[0] == -1) return r + g + bl > 30 ? "ok center " + r + "," + g + "," + bl : "FAIL black center " + r + "," + g + "," + bl;
            boolean ok = Math.abs(r - want[0]) < 40 && Math.abs(g - want[1]) < 40 && Math.abs(bl - want[2]) < 40;
            return (ok ? "ok " : "FAIL ") + what + " center " + r + "," + g + "," + bl;
        }
    }

    G3d(Base m) {
        this.m = m;
        int v = ((android.app.ActivityManager) m.getSystemService(android.content.Context.ACTIVITY_SERVICE)).getDeviceConfigurationInfo().reqGlEsVersion;
        es = v >= 0x30001 ? 31 : v >= 0x30000 ? 30 : 20;
        ms = Math.max(1500, Math.min(3000, m.secs * 1000L / 2));
    }

    static void start(Base m) { G3d g = new G3d(m); m.line("g3d.es", g.es); g.add(); g.next(); }

    // ---- GL helpers ----
    static int shader(int type, String src) throws Exception {
        int s = GLES20.glCreateShader(type); GLES20.glShaderSource(s, src); GLES20.glCompileShader(s);
        int[] ok = new int[1]; GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, ok, 0);
        if (ok[0] == 0) throw new Exception("compile: " + GLES20.glGetShaderInfoLog(s));
        return s;
    }
    static int program(String vs, String fs) throws Exception {
        int p = GLES20.glCreateProgram();
        GLES20.glAttachShader(p, shader(GLES20.GL_VERTEX_SHADER, vs)); GLES20.glAttachShader(p, shader(GLES20.GL_FRAGMENT_SHADER, fs));
        GLES20.glBindAttribLocation(p, 0, "a_pos"); GLES20.glBindAttribLocation(p, 1, "a_nrm");
        GLES20.glLinkProgram(p);
        int[] ok = new int[1]; GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, ok, 0);
        if (ok[0] == 0) throw new Exception("link: " + GLES20.glGetProgramInfoLog(p));
        return p;
    }
    static FloatBuffer fb(float[] a) { FloatBuffer b = ByteBuffer.allocateDirect(a.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer(); b.put(a).position(0); return b; }
    static final FloatBuffer QUAD = fb(new float[] { -1, -1, 1, -1, -1, 1, 1, 1 });
    static void quad(int p) {
        int a = GLES20.glGetAttribLocation(p, "a_pos");
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glEnableVertexAttribArray(a); GLES20.glVertexAttribPointer(a, 2, GLES20.GL_FLOAT, false, 0, QUAD);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }
    /** a cube: 36 vertices, position + normal */
    static FloatBuffer cube() {
        float[] f = { 1,0,0, 0,1,0, 0,0,1, -1,0,0, 0,-1,0, 0,0,-1 };
        float[] out = new float[36 * 6]; int o = 0;
        for (int k = 0; k < 6; k++) {
            float nx = f[k * 3], ny = f[k * 3 + 1], nz = f[k * 3 + 2];
            float[][] q = new float[4][3];
            for (int i = 0; i < 4; i++) {
                float u = (i == 1 || i == 2) ? 1 : -1, v = (i >= 2) ? 1 : -1;
                if (nx != 0) q[i] = new float[] { nx, u, v }; else if (ny != 0) q[i] = new float[] { u, ny, v }; else q[i] = new float[] { u, v, nz };
            }
            int[] idx = { 0, 1, 2, 0, 2, 3 };
            for (int i : idx) { out[o++] = q[i][0] * 0.5f; out[o++] = q[i][1] * 0.5f; out[o++] = q[i][2] * 0.5f; out[o++] = nx; out[o++] = ny; out[o++] = nz; }
        }
        return fb(out);
    }
    static void bindCube(FloatBuffer c) {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        c.position(0); GLES20.glEnableVertexAttribArray(0); GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, 24, c);
        c.position(3); GLES20.glEnableVertexAttribArray(1); GLES20.glVertexAttribPointer(1, 3, GLES20.GL_FLOAT, false, 24, c);
        c.position(0);
    }
    static int tex(int w, int h, int fmt, int type, ByteBuffer data) {
        int[] t = new int[1]; GLES20.glGenTextures(1, t, 0); GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, fmt, w, h, 0, fmt, type, data);
        return t[0];
    }
    static int[] fbo(int w, int h) {
        int t = tex(w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        int[] f = new int[1]; GLES20.glGenFramebuffers(1, f, 0); GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, f[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, t, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        return new int[] { f[0], t };
    }

    static final String VS_LIT = "uniform mat4 u_mvp; attribute vec3 a_pos; attribute vec3 a_nrm; varying float v_l;\n" +
        "void main() { gl_Position = u_mvp * vec4(a_pos, 1.0); v_l = 0.35 + 0.65 * max(dot(a_nrm, normalize(vec3(0.4, 0.8, 0.6))), 0.0); }";
    static final String FS_LIT = "precision mediump float; uniform vec3 u_col; varying float v_l; void main() { gl_FragColor = vec4(u_col * v_l, 1.0); }";
    static final String VS_Q = "attribute vec2 a_pos; varying vec2 v_uv; void main() { v_uv = a_pos * 0.5 + 0.5; gl_Position = vec4(a_pos, 0.0, 1.0); }";

    static float[] proj(int w, int h) { float[] p = new float[16]; Matrix.perspectiveM(p, 0, 60, w / (float) h, 0.1f, 200); return p; }

    // ---- the scenes ----
    void add() {
        scenes.add(new S("draw_calls_1200", 20) {
            int p; FloatBuffer c; float[] pr;
            void init() throws Exception { p = program(VS_LIT, FS_LIT); c = cube(); pr = proj(w, h); GLES20.glEnable(GLES20.GL_DEPTH_TEST); }
            void draw(float t) {
                GLES20.glClearColor(0.05f, 0.05f, 0.1f, 1); GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
                GLES20.glUseProgram(p); bindCube(c);
                int um = GLES20.glGetUniformLocation(p, "u_mvp"), uc = GLES20.glGetUniformLocation(p, "u_col");
                float[] mm = new float[16], mvp = new float[16];
                for (int i = 0; i < 1200; i++) {
                    Matrix.setIdentityM(mm, 0); Matrix.translateM(mm, 0, (i % 40 - 20) * 1.2f, (i / 40 - 15) * 1.2f, -40);
                    Matrix.rotateM(mm, 0, t * 90 + i, 1, 1, 0);
                    Matrix.multiplyMM(mvp, 0, pr, 0, mm, 0);
                    GLES20.glUniformMatrix4fv(um, 1, false, mvp, 0); GLES20.glUniform3f(uc, (i % 7) / 7f + 0.2f, 0.6f, 1 - (i % 5) / 5f);
                    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
                }
            }
        });
        scenes.add(new S("instancing_20k", 30) {
            int p; FloatBuffer c; float[] pr;
            void init() throws Exception {
                p = program("#version 300 es\nuniform mat4 u_vp; uniform float u_t; layout(location=0) in vec3 a_pos; layout(location=1) in vec3 a_nrm; out float v_l; out vec3 v_c;\n" +
                    "void main() { float i = float(gl_InstanceID); vec3 o = vec3(mod(i, 200.0) - 100.0, floor(i / 200.0) - 50.0, -80.0 + 10.0 * sin(u_t + i * 0.01));\n" +
                    " gl_Position = u_vp * vec4(a_pos * 0.6 + o, 1.0); v_l = 0.4 + 0.6 * max(dot(a_nrm, vec3(0.5, 0.7, 0.5)), 0.0); v_c = vec3(fract(i * 0.013), 0.5, fract(i * 0.007)); }",
                    "#version 300 es\nprecision mediump float; in float v_l; in vec3 v_c; out vec4 o; void main() { o = vec4(v_c * v_l, 1.0); }");
                c = cube(); pr = proj(w, h); GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            }
            void draw(float t) {
                GLES20.glClearColor(0.02f, 0.02f, 0.05f, 1); GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
                GLES20.glUseProgram(p); bindCube(c);
                GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(p, "u_vp"), 1, false, pr, 0); GLES20.glUniform1f(GLES20.glGetUniformLocation(p, "u_t"), t);
                GLES30.glDrawArraysInstanced(GLES20.GL_TRIANGLES, 0, 36, 20000);
            }
        });
        scenes.add(new S("raymarch_heavy", 20) {
            int p;
            void init() throws Exception {
                p = program(VS_Q, "precision highp float; varying vec2 v_uv; uniform float u_t; uniform vec2 u_r;\n" +
                    "float map(vec3 q) { vec3 c = mod(q, 4.0) - 2.0; return length(c) - 0.8 + 0.1 * sin(q.x * 3.0 + u_t); }\n" +
                    "void main() { vec2 uv = (v_uv - 0.5) * vec2(u_r.x / u_r.y, 1.0); vec3 ro = vec3(0.0, 0.0, u_t * 2.0), rd = normalize(vec3(uv, 1.0)); float d = 0.0; int k = 0;\n" +
                    " for (int i = 0; i < 64; i++) { float s = map(ro + rd * d); if (s < 0.002) break; d += s; k = i; }\n" +
                    " vec3 pos = ro + rd * d; vec2 e = vec2(0.01, 0.0); vec3 n = normalize(vec3(map(pos + e.xyy) - map(pos - e.xyy), map(pos + e.yxy) - map(pos - e.yxy), map(pos + e.yyx) - map(pos - e.yyx)));\n" +
                    " float l = max(dot(n, normalize(vec3(0.5, 0.8, -0.3))), 0.0); gl_FragColor = vec4(vec3(0.2, 0.5, 0.9) * l + 0.1 + float(k) / 200.0, 1.0); }");
            }
            void draw(float t) {
                GLES20.glUseProgram(p); GLES20.glUniform1f(GLES20.glGetUniformLocation(p, "u_t"), t);
                GLES20.glUniform2f(GLES20.glGetUniformLocation(p, "u_r"), w, h); quad(p);
            }
        });
        scenes.add(new S("msaa_4x", 20, 4) {
            int p; FloatBuffer c; float[] pr;
            void init() throws Exception {
                p = program(VS_LIT, FS_LIT); c = cube(); pr = proj(w, h); GLES20.glEnable(GLES20.GL_DEPTH_TEST);
                int[] s = new int[1]; GLES20.glGetIntegerv(GLES20.GL_SAMPLES, s, 0); got = s[0];
            }
            int got;
            void draw(float t) {
                GLES20.glClearColor(0.9f, 0.9f, 0.95f, 1); GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
                GLES20.glUseProgram(p); bindCube(c);
                float[] mm = new float[16], mvp = new float[16];
                for (int i = 0; i < 60; i++) { Matrix.setIdentityM(mm, 0); Matrix.translateM(mm, 0, (i % 10 - 5) * 1.6f, (i / 10 - 3) * 1.6f, -14); Matrix.rotateM(mm, 0, t * 50 + i * 7, 0.3f, 1, 0.2f);
                    Matrix.multiplyMM(mvp, 0, pr, 0, mm, 0); GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(p, "u_mvp"), 1, false, mvp, 0);
                    GLES20.glUniform3f(GLES20.glGetUniformLocation(p, "u_col"), 0.9f, 0.3f, 0.2f); GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36); }
            }
            String check() { return (got >= 4 ? "ok samples " + got : "FAIL samples " + got) + " | " + center("", new int[] { -1 }); }
        });
        scenes.add(new S("bloom_postprocess", 20) {
            int scene, bright, blur, comp; int[] f0, f1, f2;
            void init() throws Exception {
                scene = program(VS_Q, "precision mediump float; varying vec2 v_uv; uniform float u_t; void main() { vec2 p = v_uv - 0.5; float d = 0.0;\n" +
                    " for (int i = 0; i < 6; i++) { vec2 c = 0.3 * vec2(sin(u_t + float(i)), cos(u_t * 1.3 + float(i) * 2.0)); d += 0.004 / dot(p - c, p - c); }\n" +
                    " gl_FragColor = vec4(d * vec3(1.0, 0.6, 0.3), 1.0); }");
                bright = program(VS_Q, "precision mediump float; varying vec2 v_uv; uniform sampler2D t; void main() { vec3 c = texture2D(t, v_uv).rgb; gl_FragColor = vec4(max(c - 0.7, 0.0) * 2.0, 1.0); }");
                blur = program(VS_Q, "precision mediump float; varying vec2 v_uv; uniform sampler2D t; uniform vec2 dir; void main() { vec4 s = vec4(0.0);\n" +
                    " for (int i = -6; i <= 6; i++) s += texture2D(t, v_uv + dir * float(i)) * (1.0 - abs(float(i)) / 7.0); gl_FragColor = s / 7.0; }");
                comp = program(VS_Q, "precision mediump float; varying vec2 v_uv; uniform sampler2D a; uniform sampler2D b; void main() { gl_FragColor = texture2D(a, v_uv) + texture2D(b, v_uv) * 1.5; }");
                f0 = fbo(w, h); f1 = fbo(w / 2, h / 2); f2 = fbo(w / 2, h / 2);
            }
            void pass(int prog, int[] target, int tw, int th, int src) {
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, target == null ? 0 : target[0]); GLES20.glViewport(0, 0, tw, th);
                GLES20.glUseProgram(prog); GLES20.glActiveTexture(GLES20.GL_TEXTURE0); GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, src);
                GLES20.glUniform1i(GLES20.glGetUniformLocation(prog, "t"), 0); quad(prog);
            }
            void draw(float t) {
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, f0[0]); GLES20.glViewport(0, 0, w, h);
                GLES20.glUseProgram(scene); GLES20.glUniform1f(GLES20.glGetUniformLocation(scene, "u_t"), t); quad(scene);
                pass(bright, f1, w / 2, h / 2, f0[1]);
                GLES20.glUseProgram(blur); GLES20.glUniform2f(GLES20.glGetUniformLocation(blur, "dir"), 2f / w, 0); pass(blur, f2, w / 2, h / 2, f1[1]);
                GLES20.glUseProgram(blur); GLES20.glUniform2f(GLES20.glGetUniformLocation(blur, "dir"), 0, 2f / h); pass(blur, f1, w / 2, h / 2, f2[1]);
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0); GLES20.glViewport(0, 0, w, h); GLES20.glUseProgram(comp);
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0); GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, f0[1]); GLES20.glUniform1i(GLES20.glGetUniformLocation(comp, "a"), 0);
                GLES20.glActiveTexture(GLES20.GL_TEXTURE1); GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, f1[1]); GLES20.glUniform1i(GLES20.glGetUniformLocation(comp, "b"), 1);
                quad(comp); GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            }
        });
        scenes.add(new S("shadow_map", 30) {
            int depthProg, litProg, fbo, depthTex; FloatBuffer c; float[] pr;
            void init() throws Exception {
                depthProg = program("#version 300 es\nuniform mat4 u_mvp; layout(location=0) in vec3 a_pos; void main() { gl_Position = u_mvp * vec4(a_pos, 1.0); }",
                    "#version 300 es\nprecision mediump float; out vec4 o; void main() { o = vec4(1.0); }");
                litProg = program("#version 300 es\nuniform mat4 u_mvp; uniform mat4 u_light; layout(location=0) in vec3 a_pos; layout(location=1) in vec3 a_nrm; out vec4 v_ls; out float v_l;\n" +
                    "void main() { gl_Position = u_mvp * vec4(a_pos, 1.0); v_ls = u_light * vec4(a_pos, 1.0); v_l = 0.4 + 0.6 * max(dot(a_nrm, vec3(0.0, 1.0, 0.0)), 0.0); }",
                    "#version 300 es\nprecision mediump float; precision mediump sampler2DShadow; uniform sampler2DShadow u_sm; in vec4 v_ls; in float v_l; out vec4 o;\n" +
                    "void main() { vec3 p = v_ls.xyz / v_ls.w * 0.5 + 0.5; float s = texture(u_sm, vec3(p.xy, p.z - 0.005)); o = vec4(vec3(0.8, 0.8, 0.7) * v_l * (0.4 + 0.6 * s), 1.0); }");
                int[] t = new int[1]; GLES20.glGenTextures(1, t, 0); depthTex = t[0]; GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, depthTex);
                GLES30.glTexStorage2D(GLES20.GL_TEXTURE_2D, 1, GLES30.GL_DEPTH_COMPONENT24, 1024, 1024);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES30.GL_TEXTURE_COMPARE_MODE, GLES30.GL_COMPARE_REF_TO_TEXTURE);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR); GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                int[] f = new int[1]; GLES20.glGenFramebuffers(1, f, 0); fbo = f[0]; GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);
                GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_TEXTURE_2D, depthTex, 0);
                GLES30.glDrawBuffers(1, new int[] { GLES20.GL_NONE }, 0);
                int st = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER); GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                if (st != GLES20.GL_FRAMEBUFFER_COMPLETE) throw new Exception("depth fbo 0x" + Integer.toHexString(st));
                c = cube(); pr = proj(w, h); GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            }
            void scene(int prog, float[] vp, float[] light, float t) {
                float[] mm = new float[16], mvp = new float[16], lm = new float[16];
                for (int i = 0; i < 26; i++) {
                    Matrix.setIdentityM(mm, 0);
                    if (i == 25) { Matrix.translateM(mm, 0, 0, -1.5f, 0); Matrix.scaleM(mm, 0, 20, 0.2f, 20); }
                    else { Matrix.translateM(mm, 0, (i % 5 - 2) * 2.2f, (float) Math.abs(Math.sin(t * 2 + i)) * 1.5f, (i / 5 - 2) * 2.2f); Matrix.rotateM(mm, 0, t * 60 + i * 20, 0, 1, 0); }
                    Matrix.multiplyMM(mvp, 0, vp, 0, mm, 0); GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(prog, "u_mvp"), 1, false, mvp, 0);
                    if (light != null) { Matrix.multiplyMM(lm, 0, light, 0, mm, 0); GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(prog, "u_light"), 1, false, lm, 0); }
                    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
                }
            }
            void draw(float t) {
                float[] lv = new float[16], lp = new float[16], lvp = new float[16], v = new float[16], vp = new float[16];
                Matrix.setLookAtM(lv, 0, 8, 14, 6, 0, 0, 0, 0, 1, 0); Matrix.orthoM(lp, 0, -12, 12, -12, 12, 1, 40); Matrix.multiplyMM(lvp, 0, lp, 0, lv, 0);
                Matrix.setLookAtM(v, 0, 0, 9, 16, 0, 0, 0, 0, 1, 0); Matrix.multiplyMM(vp, 0, pr, 0, v, 0);
                bindCube(c);
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo); GLES20.glViewport(0, 0, 1024, 1024); GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);
                GLES20.glUseProgram(depthProg); scene(depthProg, lvp, null, t);
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0); GLES20.glViewport(0, 0, w, h);
                GLES20.glClearColor(0.3f, 0.4f, 0.6f, 1); GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
                GLES20.glUseProgram(litProg); GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, depthTex);
                float[] bias = new float[16]; Matrix.setIdentityM(bias, 0); scene(litProg, vp, lvp, t);
            }
        });
        scenes.add(new S("texture_4096_mips", 20) {
            int p, t;
            void init() throws Exception {
                int[] mx = new int[1]; GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, mx, 0);
                int n = Math.min(4096, mx[0]);
                ByteBuffer b = ByteBuffer.allocateDirect(n * n * 4);
                for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) { boolean on = ((x >> 5) + (y >> 5)) % 2 == 0; b.put((byte) (on ? 255 : 30)).put((byte) (x * 255 / n)).put((byte) (y * 255 / n)).put((byte) 255); }
                b.position(0);
                t = tex(n, n, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, b);
                GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
                String ext = GLES20.glGetString(GLES20.GL_EXTENSIONS);
                if (ext != null && ext.contains("GL_EXT_texture_filter_anisotropic")) GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, 0x84FE, 8f);
                p = program(VS_Q, "precision mediump float; varying vec2 v_uv; uniform sampler2D t; uniform float u_t; void main() { vec2 uv = (v_uv - 0.5) / (0.6 + 0.5 * sin(u_t)) + 0.5;\n" +
                    " uv.y = 0.2 / (v_uv.y + 0.05) + u_t * 0.1; gl_FragColor = texture2D(t, uv * vec2(4.0, 1.0)); }");
                size = n;
            }
            int size;
            void draw(float tt) { GLES20.glUseProgram(p); GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t); GLES20.glUniform1f(GLES20.glGetUniformLocation(p, "u_t"), tt);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT); GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT); quad(p); }
            String check() { return "size " + size + " | " + super.check(); }
        });
        scenes.add(new S("etc2_compressed", 30) {
            int p, t;
            void init() throws Exception {
                int n = 1024, blocks = (n / 4) * (n / 4);
                ByteBuffer b = ByteBuffer.allocateDirect(blocks * 8);
                for (int i = 0; i < blocks; i++) { b.put((byte) 0xFF).put((byte) 0x00).put((byte) 0x00).put((byte) 0x00).putInt(0); }   // individual mode, red 15/15
                b.position(0);
                int[] tt = new int[1]; GLES20.glGenTextures(1, tt, 0); t = tt[0]; GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                GLES20.glCompressedTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES30.GL_COMPRESSED_RGB8_ETC2, n, n, 0, blocks * 8, b);
                int e = GLES20.glGetError(); if (e != 0) throw new Exception("compressed upload 0x" + Integer.toHexString(e));
                p = program(VS_Q, "precision mediump float; varying vec2 v_uv; uniform sampler2D t; void main() { gl_FragColor = texture2D(t, v_uv); }");
            }
            void draw(float tt) { GLES20.glUseProgram(p); GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t); quad(p); }
            String check() { return center("etc2 red", new int[] { 255, 2, 2 }); }
        });
        scenes.add(new S("transform_feedback_100k", 30) {
            int upd, rnd; int[] buf = new int[2]; int cur; final int N = 100000;
            void init() throws Exception {
                upd = GLES20.glCreateProgram();
                GLES20.glAttachShader(upd, shader(GLES20.GL_VERTEX_SHADER, "#version 300 es\nlayout(location=0) in vec4 a_p; out vec4 o_p; uniform float u_t;\n" +
                    "void main() { vec4 p = a_p; p.xy += vec2(sin(p.y * 5.0 + u_t), cos(p.x * 5.0 + u_t)) * 0.003; if (abs(p.x) > 1.0 || abs(p.y) > 1.0) p.xy *= 0.1; o_p = p; }"));
                GLES20.glAttachShader(upd, shader(GLES20.GL_FRAGMENT_SHADER, "#version 300 es\nprecision mediump float; out vec4 o; void main() { o = vec4(1.0); }"));
                GLES30.glTransformFeedbackVaryings(upd, new String[] { "o_p" }, GLES30.GL_INTERLEAVED_ATTRIBS);
                GLES20.glLinkProgram(upd);
                rnd = program("#version 300 es\nlayout(location=0) in vec4 a_p; void main() { gl_Position = vec4(a_p.xy, 0.0, 1.0); gl_PointSize = 2.0; }",
                    "#version 300 es\nprecision mediump float; out vec4 o; void main() { o = vec4(0.4, 0.9, 1.0, 1.0); }");
                float[] d = new float[N * 4]; java.util.Random r = new java.util.Random(3);
                for (int i = 0; i < N; i++) { d[i * 4] = r.nextFloat() * 2 - 1; d[i * 4 + 1] = r.nextFloat() * 2 - 1; d[i * 4 + 3] = 1; }
                GLES20.glGenBuffers(2, buf, 0);
                for (int b : buf) { GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, b); GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, N * 16, fb(d), GLES30.GL_DYNAMIC_COPY); }
            }
            void draw(float t) {
                GLES20.glClearColor(0, 0, 0, 1); GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                GLES20.glUseProgram(upd); GLES20.glUniform1f(GLES20.glGetUniformLocation(upd, "u_t"), t);
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buf[cur]); GLES20.glEnableVertexAttribArray(0); GLES20.glVertexAttribPointer(0, 4, GLES20.GL_FLOAT, false, 16, 0);
                GLES30.glBindBufferBase(GLES30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, buf[1 - cur]);
                GLES20.glEnable(GLES30.GL_RASTERIZER_DISCARD); GLES30.glBeginTransformFeedback(GLES20.GL_POINTS); GLES20.glDrawArrays(GLES20.GL_POINTS, 0, N);
                GLES30.glEndTransformFeedback(); GLES20.glDisable(GLES30.GL_RASTERIZER_DISCARD); GLES30.glBindBufferBase(GLES30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, 0);
                cur = 1 - cur;
                GLES20.glUseProgram(rnd); GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buf[cur]); GLES20.glVertexAttribPointer(0, 4, GLES20.GL_FLOAT, false, 16, 0);
                GLES20.glDrawArrays(GLES20.GL_POINTS, 0, N); GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
            }
            String check() {
                ByteBuffer px = ByteBuffer.allocateDirect(w * 4 * 9).order(ByteOrder.nativeOrder());
                GLES20.glReadPixels(0, h / 2 - 4, w, 9, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, px);
                int lit = 0; for (int i = 0; i < w * 9; i++) if ((px.get(i * 4 + 2) & 255) > 100) lit++;
                return (lit > 20 ? "ok " : "FAIL ") + "lit pixels in a band " + lit;
            }
        });
        scenes.add(new S("compute_es31", 31) {
            int cp, dp, ssbo; final int N = 1 << 16;
            void init() throws Exception {
                int s = shader(GLES31.GL_COMPUTE_SHADER, "#version 310 es\nlayout(local_size_x = 64) in; layout(std430, binding = 0) buffer B { uint v[]; };\n" +
                    "uniform uint u_k; void main() { uint i = gl_GlobalInvocationID.x; v[i] = i * 3u + u_k; }");
                cp = GLES20.glCreateProgram(); GLES20.glAttachShader(cp, s); GLES20.glLinkProgram(cp);
                int[] ok = new int[1]; GLES20.glGetProgramiv(cp, GLES20.GL_LINK_STATUS, ok, 0); if (ok[0] == 0) throw new Exception("link " + GLES20.glGetProgramInfoLog(cp));
                int[] b = new int[1]; GLES20.glGenBuffers(1, b, 0); ssbo = b[0];
                GLES20.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, ssbo); GLES20.glBufferData(GLES31.GL_SHADER_STORAGE_BUFFER, N * 4, null, GLES30.GL_DYNAMIC_READ);
                dp = program(VS_Q, "precision mediump float; varying vec2 v_uv; uniform float u_t; void main() { gl_FragColor = vec4(0.2, 0.5 + 0.5 * sin(u_t * 3.0 + v_uv.x * 10.0), 0.8, 1.0); }");
            }
            int k;
            void draw(float t) {
                k++;
                GLES20.glUseProgram(cp); GLES30.glUniform1ui(GLES20.glGetUniformLocation(cp, "u_k"), k);
                GLES30.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 0, ssbo); GLES31.glDispatchCompute(N / 64, 1, 1);
                GLES31.glMemoryBarrier(GLES31.GL_BUFFER_UPDATE_BARRIER_BIT);
                GLES20.glUseProgram(dp); GLES20.glUniform1f(GLES20.glGetUniformLocation(dp, "u_t"), t); quad(dp);
            }
            String check() {
                GLES20.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, ssbo);
                ByteBuffer b = (ByteBuffer) GLES30.glMapBufferRange(GLES31.GL_SHADER_STORAGE_BUFFER, 0, N * 4, GLES30.GL_MAP_READ_BIT);
                if (b == null) return "FAIL map 0x" + Integer.toHexString(GLES20.glGetError());
                IntBuffer ib = b.order(ByteOrder.nativeOrder()).asIntBuffer();
                int bad = 0; for (int i = 0; i < N; i += 997) if (ib.get(i) != i * 3 + k) bad++;
                GLES30.glUnmapBuffer(GLES31.GL_SHADER_STORAGE_BUFFER);
                return bad == 0 ? "ok values right (k " + k + ")" : "FAIL " + bad + " wrong values";
            }
        });
        scenes.add(new S("float_render_target", 30) {
            int p, f, t2; boolean half;
            void init() throws Exception {
                String ext = GLES20.glGetString(GLES20.GL_EXTENSIONS);
                half = ext != null && (ext.contains("GL_EXT_color_buffer_half_float") || ext.contains("GL_EXT_color_buffer_float"));
                if (!half) throw new Exception("n/a no float colour buffers");
                int[] tt = new int[1]; GLES20.glGenTextures(1, tt, 0); t2 = tt[0]; GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t2);
                GLES30.glTexStorage2D(GLES20.GL_TEXTURE_2D, 1, GLES30.GL_RGBA16F, 64, 64);
                int[] ff = new int[1]; GLES20.glGenFramebuffers(1, ff, 0); f = ff[0]; GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, f);
                GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, t2, 0);
                int st = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER); GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                if (st != GLES20.GL_FRAMEBUFFER_COMPLETE) throw new Exception("fp16 fbo 0x" + Integer.toHexString(st));
                p = program("#version 300 es\nlayout(location=0) in vec2 a_pos; void main() { gl_Position = vec4(a_pos, 0.0, 1.0); }",
                    "#version 300 es\nprecision highp float; out vec4 o; void main() { o = vec4(4.5, -2.0, 0.25, 1.0); }");
            }
            void draw(float t) {
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, f); GLES20.glViewport(0, 0, 64, 64); GLES20.glUseProgram(p); quad(p);
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0); GLES20.glViewport(0, 0, w, h);
                GLES20.glClearColor(0.2f, 0.6f, 0.3f, 1); GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            }
            String check() {
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, f);
                ByteBuffer b = ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder());
                GLES20.glReadPixels(5, 5, 1, 1, GLES20.GL_RGBA, GLES20.GL_FLOAT, b);
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                float r = b.getFloat(0), g = b.getFloat(4);
                return (Math.abs(r - 4.5f) < 0.05 && Math.abs(g + 2) < 0.05 ? "ok " : "FAIL ") + "read " + r + "," + g;
            }
        });
        scenes.add(new S("mrt_4_targets", 30) {
            int p, f; int[] tx = new int[4];
            void init() throws Exception {
                GLES20.glGenTextures(4, tx, 0);
                int[] ff = new int[1]; GLES20.glGenFramebuffers(1, ff, 0); f = ff[0]; GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, f);
                for (int i = 0; i < 4; i++) { GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tx[i]); GLES30.glTexStorage2D(GLES20.GL_TEXTURE_2D, 1, GLES30.GL_RGBA8, 32, 32);
                    GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0 + i, GLES20.GL_TEXTURE_2D, tx[i], 0); }
                GLES30.glDrawBuffers(4, new int[] { 0x8CE0, 0x8CE1, 0x8CE2, 0x8CE3 }, 0);
                int st = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER); GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                if (st != GLES20.GL_FRAMEBUFFER_COMPLETE) throw new Exception("mrt fbo 0x" + Integer.toHexString(st));
                p = program("#version 300 es\nlayout(location=0) in vec2 a_pos; void main() { gl_Position = vec4(a_pos, 0.0, 1.0); }",
                    "#version 300 es\nprecision mediump float; layout(location=0) out vec4 a; layout(location=1) out vec4 b; layout(location=2) out vec4 c; layout(location=3) out vec4 d;\n" +
                    "void main() { a = vec4(1,0,0,1); b = vec4(0,1,0,1); c = vec4(0,0,1,1); d = vec4(1,1,0,1); }");
            }
            void draw(float t) {
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, f); GLES20.glViewport(0, 0, 32, 32); GLES20.glUseProgram(p); quad(p);
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0); GLES20.glViewport(0, 0, w, h);
                GLES20.glClearColor(0.5f, 0.2f, 0.6f, 1); GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            }
            String check() {
                int[][] want = { { 255, 0, 0 }, { 0, 255, 0 }, { 0, 0, 255 }, { 255, 255, 0 } };
                StringBuilder s = new StringBuilder(); int bad = 0;
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, f);
                for (int i = 0; i < 4; i++) {
                    GLES30.glReadBuffer(0x8CE0 + i);
                    ByteBuffer b = ByteBuffer.allocateDirect(4); GLES20.glReadPixels(3, 3, 1, 1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, b);
                    int r = b.get(0) & 255, g = b.get(1) & 255, bl = b.get(2) & 255;
                    if (Math.abs(r - want[i][0]) > 10 || Math.abs(g - want[i][1]) > 10 || Math.abs(bl - want[i][2]) > 10) { bad++; s.append(i).append('=').append(r).append(',').append(g).append(',').append(bl).append(' '); }
                }
                GLES30.glReadBuffer(GLES20.GL_BACK); GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                return bad == 0 ? "ok 4 targets" : "FAIL " + s.toString().trim();
            }
        });
        scenes.add(new S("texture_array", 30) {
            int p, t;
            void init() throws Exception {
                int[] tt = new int[1]; GLES20.glGenTextures(1, tt, 0); t = tt[0]; GLES20.glBindTexture(GLES30.GL_TEXTURE_2D_ARRAY, t);
                GLES30.glTexStorage3D(GLES30.GL_TEXTURE_2D_ARRAY, 1, GLES30.GL_RGBA8, 64, 64, 8);
                for (int l = 0; l < 8; l++) {
                    ByteBuffer b = ByteBuffer.allocateDirect(64 * 64 * 4);
                    for (int i = 0; i < 64 * 64; i++) b.put((byte) (l * 32)).put((byte) (255 - l * 32)).put((byte) 128).put((byte) 255);
                    b.position(0); GLES30.glTexSubImage3D(GLES30.GL_TEXTURE_2D_ARRAY, 0, 0, 0, l, 64, 64, 1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, b);
                }
                GLES20.glTexParameteri(GLES30.GL_TEXTURE_2D_ARRAY, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
                GLES20.glTexParameteri(GLES30.GL_TEXTURE_2D_ARRAY, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
                p = program("#version 300 es\nlayout(location=0) in vec2 a_pos; out vec2 v; void main() { v = a_pos * 0.5 + 0.5; gl_Position = vec4(a_pos, 0.0, 1.0); }",
                    "#version 300 es\nprecision mediump float; precision mediump sampler2DArray; uniform sampler2DArray s; in vec2 v; out vec4 o; void main() { o = texture(s, vec3(v, floor(v.x * 8.0))); }");
            }
            void draw(float tt) { GLES20.glUseProgram(p); GLES20.glBindTexture(GLES30.GL_TEXTURE_2D_ARRAY, t); quad(p); }
            String check() { return center("layer 4", new int[] { 128, 127, 128 }); }
        });
        scenes.add(new S("pbo_async_readback", 30) {
            int[] pbo = new int[2]; int idx; int p; int seen;
            void init() throws Exception {
                // GLES30.glReadPixels(..., int offset) only exists in the framework from API 24
                if (android.os.Build.VERSION.SDK_INT < 24) throw new Exception("n/a (PBO readback binding needs API 24)");
                GLES20.glGenBuffers(2, pbo, 0);
                for (int b : pbo) { GLES20.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, b); GLES20.glBufferData(GLES30.GL_PIXEL_PACK_BUFFER, 256 * 256 * 4, null, GLES30.GL_STREAM_READ); }
                GLES20.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, 0);
                p = program(VS_Q, "precision mediump float; varying vec2 v_uv; uniform float u_t; void main() { gl_FragColor = vec4(v_uv, 0.5 + 0.5 * sin(u_t), 1.0); }");
            }
            void draw(float t) {
                GLES20.glUseProgram(p); GLES20.glUniform1f(GLES20.glGetUniformLocation(p, "u_t"), t); quad(p);
                GLES20.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, pbo[idx]); GLES30.glReadPixels(0, 0, 256, 256, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, 0);
                idx = 1 - idx; GLES20.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, pbo[idx]);
                ByteBuffer b = (ByteBuffer) GLES30.glMapBufferRange(GLES30.GL_PIXEL_PACK_BUFFER, 0, 256 * 256 * 4, GLES30.GL_MAP_READ_BIT);
                if (b != null) { if ((b.get(255 * 4) & 255) > 0) seen++; GLES30.glUnmapBuffer(GLES30.GL_PIXEL_PACK_BUFFER); }
                GLES20.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, 0);
            }
            String check() { return (seen > 3 ? "ok " : "FAIL ") + "frames read through PBO " + seen; }
        });
        scenes.add(new S("swap_interval_0", 20) {
            int p;
            void init() throws Exception {
                EGL14.eglSwapInterval(EGL14.eglGetCurrentDisplay(), 0);
                p = program(VS_Q, "precision mediump float; varying vec2 v_uv; uniform float u_t; void main() { gl_FragColor = vec4(fract(v_uv.x + u_t), v_uv.y, 0.4, 1.0); }");
            }
            void draw(float t) { GLES20.glUseProgram(p); GLES20.glUniform1f(GLES20.glGetUniformLocation(p, "u_t"), t); quad(p); }
        });
        scenes.add(new S("shared_context_upload", 20) {
            int p; final int[] shared = { 0 }; volatile int uploads; volatile String err; Thread th; volatile boolean stop;
            void init() throws Exception {
                final EGLDisplay d = EGL14.eglGetCurrentDisplay(); final EGLContext main = EGL14.eglGetCurrentContext();
                int[] tt = new int[1]; GLES20.glGenTextures(1, tt, 0); shared[0] = tt[0];
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, shared[0]); GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 256, 256, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
                GLES20.glFinish();
                th = new Thread(new Runnable() { public void run() {
                    try {
                        EGLConfig[] cf = new EGLConfig[1]; int[] n = new int[1];
                        EGL14.eglChooseConfig(d, new int[] { EGL14.EGL_RENDERABLE_TYPE, 4, EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT, EGL14.EGL_NONE }, 0, cf, 0, 1, n, 0);
                        EGLContext c = EGL14.eglCreateContext(d, cf[0], main, new int[] { 0x3098, 2, EGL14.EGL_NONE }, 0);
                        if (c == EGL14.EGL_NO_CONTEXT) { err = "shared context 0x" + Integer.toHexString(EGL14.eglGetError()); return; }
                        EGLSurface s = EGL14.eglCreatePbufferSurface(d, cf[0], new int[] { EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE }, 0);
                        EGL14.eglMakeCurrent(d, s, s, c);
                        ByteBuffer b = ByteBuffer.allocateDirect(256 * 256 * 4);
                        while (!stop) {
                            b.position(0); byte v = (byte) (uploads * 40); for (int i = 0; i < 256 * 256; i++) b.put((byte) 255).put(v).put((byte) 60).put((byte) 255); b.position(0);
                            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, shared[0]); GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, 256, 256, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, b);
                            GLES20.glFinish(); uploads++; SystemClock.sleep(50);
                        }
                        EGL14.eglMakeCurrent(d, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT); EGL14.eglDestroySurface(d, s); EGL14.eglDestroyContext(d, c);
                    } catch (Throwable t) { err = String.valueOf(t); }
                } }, "g3d-upload");
                th.start();
                p = program(VS_Q, "precision mediump float; varying vec2 v_uv; uniform sampler2D t; void main() { gl_FragColor = texture2D(t, v_uv); }");
            }
            void draw(float t) { GLES20.glUseProgram(p); GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, shared[0]); quad(p); }
            String check() {
                stop = true;
                if (err != null) return "FAIL " + err;
                ByteBuffer b = ByteBuffer.allocateDirect(4); GLES20.glReadPixels(w / 2, h / 2, 1, 1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, b);
                int r = b.get(0) & 255, bl = b.get(2) & 255;     // the other thread's texels: red 255, blue 60
                return (uploads > 3 && r > 200 && Math.abs(bl - 60) < 30 ? "ok " : "FAIL ") + "uploads " + uploads + " center " + r + "," + (b.get(1) & 255) + "," + bl;
            }
        });
        scenes.add(new S("surface_texture_external", 20) {
            int p, oes; SurfaceTexture st; Surface s; volatile int posted; Thread th; volatile boolean stop; final float[] mx = new float[16];
            void init() throws Exception {
                int[] tt = new int[1]; GLES20.glGenTextures(1, tt, 0); oes = tt[0];
                GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oes);
                GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                st = new SurfaceTexture(oes); st.setDefaultBufferSize(256, 256); s = new Surface(st);
                th = new Thread(new Runnable() { public void run() {
                    android.graphics.Paint pp = new android.graphics.Paint();
                    while (!stop) {
                        try {
                            Canvas c = android.os.Build.VERSION.SDK_INT >= 23 ? s.lockHardwareCanvas() : s.lockCanvas(null);
                            c.drawColor(0xff20c040); pp.setColor(0xffffffff); c.drawCircle(128 + (posted % 50), 128, 40, pp);
                            s.unlockCanvasAndPost(c); posted++;
                        } catch (Throwable e) { break; }
                        SystemClock.sleep(16);
                    }
                } }, "g3d-producer");
                th.start();
                p = program("attribute vec2 a_pos; uniform mat4 u_m; varying vec2 v_uv; void main() { v_uv = (u_m * vec4(a_pos * 0.5 + 0.5, 0.0, 1.0)).xy; gl_Position = vec4(a_pos, 0.0, 1.0); }",
                    "#extension GL_OES_EGL_image_external : require\nprecision mediump float; uniform samplerExternalOES t; varying vec2 v_uv; void main() { gl_FragColor = texture2D(t, v_uv); }");
            }
            void draw(float t) {
                try { st.updateTexImage(); st.getTransformMatrix(mx); } catch (Throwable e) { }
                GLES20.glUseProgram(p); GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(p, "u_m"), 1, false, mx, 0);
                GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oes); quad(p);
            }
            String check() {
                String c = center("", new int[] { -1 }); stop = true;
                ByteBuffer b = ByteBuffer.allocateDirect(4); GLES20.glReadPixels(w / 8, h / 8, 1, 1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, b);
                boolean green = (b.get(1) & 255) > 150 && (b.get(0) & 255) < 90;
                st.release(); s.release();
                return (green ? "ok " : "FAIL ") + "frames posted " + posted + " corner " + (b.get(0) & 255) + "," + (b.get(1) & 255) + "," + (b.get(2) & 255) + " | " + c;
            }
        });
        scenes.add(new S("context_loss_resume", 20) {
            int p; long before = -1;
            void init() throws Exception { p = program(VS_Q, "precision mediump float; varying vec2 v_uv; uniform float u_t; void main() { gl_FragColor = vec4(0.8, v_uv.y, 0.5 + 0.5 * sin(u_t), 1.0); }"); }
            void draw(float t) { GLES20.glUseProgram(p); GLES20.glUniform1f(GLES20.glGetUniformLocation(p, "u_t"), t); quad(p); }
            void pause(final GLSurfaceView v) { before = frames; v.onPause(); v.postDelayed(new Runnable() { public void run() { v.onResume(); } }, 300); }
            String check() { return (frames > before + 10 ? "ok " : "FAIL ") + "frames before pause " + before + " total " + frames + " | " + super.check(); }
        });
    }

    // ---- running ----
    void next() {
        if (view != null) { try { view.onPause(); } catch (Throwable t) { } }
        for (int i = m.root.getChildCount() - 1; i >= 0; i--) if (m.root.getChildAt(i) != m.status) m.root.removeViewAt(i);
        view = null;
        at++;
        if (at >= scenes.size()) { m.done(); return; }
        final S s = scenes.get(at);
        final String k = "g3d." + s.name;
        if (es < s.needEs) { m.line(k, "n/a (needs ES " + s.needEs / 10 + "." + s.needEs % 10 + ", device has " + es / 10 + "." + es % 10 + ")"); next(); return; }
        m.say("g3d " + (at + 1) + "/" + scenes.size() + ": " + s.name);
        final GLSurfaceView v = new GLSurfaceView(m);
        v.setEGLContextClientVersion(s.needEs >= 30 ? 3 : 2);
        if (s.samples > 0) v.setEGLConfigChooser(new GLSurfaceView.EGLConfigChooser() {
            public javax.microedition.khronos.egl.EGLConfig chooseConfig(javax.microedition.khronos.egl.EGL10 egl, javax.microedition.khronos.egl.EGLDisplay d) {
                int[] n = new int[1]; javax.microedition.khronos.egl.EGLConfig[] c = new javax.microedition.khronos.egl.EGLConfig[1];
                egl.eglChooseConfig(d, new int[] { 0x3024, 8, 0x3023, 8, 0x3022, 8, 0x3025, 16, 0x3032, 1, 0x3031, s.samples, 0x3040, 4, 0x3038 }, c, 1, n);
                if (n[0] == 0) egl.eglChooseConfig(d, new int[] { 0x3024, 8, 0x3023, 8, 0x3022, 8, 0x3025, 16, 0x3040, 4, 0x3038 }, c, 1, n);
                return c[0];
            }
        }); else v.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        final long[] ts = new long[4000]; final int[] n = { 0 }; final boolean[] failed = { false }, ended = { false };
        final long t0 = SystemClock.uptimeMillis();
        v.setRenderer(new GLSurfaceView.Renderer() {
            boolean ready;
            public void onSurfaceCreated(GL10 gl, javax.microedition.khronos.egl.EGLConfig c) { ready = false; }
            public void onSurfaceChanged(GL10 gl, int w, int h) {
                s.w = w; s.h = h; GLES20.glViewport(0, 0, w, h);
                try { s.init(); ready = true; } catch (Throwable e) {
                    String msg = String.valueOf(e.getMessage());
                    m.line(k, msg.startsWith("n/a") ? msg : "FAIL init " + e); failed[0] = true; finish();
                }
            }
            public void onDrawFrame(GL10 gl) {
                if (!ready || ended[0]) return;
                long now = SystemClock.uptimeMillis();
                float t = (now - t0) / 1000f;
                s.draw(t); s.frames++;
                if (now - t0 > 300 && n[0] < ts.length) ts[n[0]++] = System.nanoTime();
                if (now - t0 > ms + 300) {
                    ended[0] = true;
                    int e = GLES20.glGetError();
                    String c; try { c = s.check(); } catch (Throwable x) { c = "FAIL check " + x; }
                    m.frames(k, ts, n[0], m.hz());
                    m.line(k + ".gl_error", "0x" + Integer.toHexString(e));
                    m.line(k + ".check", e != 0 && c.startsWith("ok") ? "FAIL gl error 0x" + Integer.toHexString(e) + " " + c : c);
                    finish();
                }
            }
            void finish() { m.h.post(new Runnable() { public void run() { next(); } }); }
        });
        v.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        view = v;
        m.root.addView(v, 0, new FrameLayout.LayoutParams(-1, -1));
        v.postDelayed(new Runnable() { public void run() { if (view == v && !ended[0]) s.pause(v); } }, ms / 2);
    }
}
