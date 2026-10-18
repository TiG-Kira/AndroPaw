package app.aemu.test;

import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * The 3D tests. Plain: 300 lit, textured cubes, one draw call each, depth test, mipmapped texture, a blended layer.
 * Heavy: 1500 cubes into a 1024x1024 texture, then a full-screen pass whose fragment shader takes 24 samples of it
 * per pixel; with GLES 3, 2000 more cubes in one instanced call from a "#version 300 es" program.
 * Reports the driver's strings, frame times, the first GL error, shader logs, and the colour of the middle pixel
 * (a black frame from a renderer that "works" is a failure too).
 */
class Scene implements GLSurfaceView.Renderer {
    final Base m; final String k; final boolean heavy, es3; final int secs; final float hz;
    final long[] ts;
    int n, w, h, prog, post, inst, tex, fbo, fboTex, cubes;
    long end;
    boolean reported;
    int firstError;
    FloatBuffer cube, quad;
    final float[] proj = new float[16], view = new float[16], model = new float[16], mvp = new float[16], tmp = new float[16];

    Scene(Base m, String k, boolean heavy, boolean es3, int secs, float hz) {
        this.m = m; this.k = k; this.heavy = heavy; this.es3 = es3; this.secs = secs; this.hz = hz;
        ts = new long[secs * 1000];
        cubes = heavy ? 1500 : 300;
    }

    static final String VS = "uniform mat4 uMvp; uniform mat4 uModel; attribute vec3 aPos; attribute vec3 aNormal; attribute vec2 aUv;"
        + "varying vec2 vUv; varying float vLight;"
        + "void main(){ vUv=aUv; vec3 nrm=normalize((uModel*vec4(aNormal,0.0)).xyz); vLight=0.25+0.75*max(dot(nrm,normalize(vec3(0.4,0.8,0.5))),0.0); gl_Position=uMvp*vec4(aPos,1.0); }";
    static final String FS = "precision mediump float; uniform sampler2D uTex; uniform vec4 uTint; varying vec2 vUv; varying float vLight;"
        + "void main(){ vec4 c=texture2D(uTex,vUv)*uTint; gl_FragColor=vec4(c.rgb*vLight,c.a); }";
    static final String PVS = "attribute vec2 aPos; varying vec2 vUv; void main(){ vUv=aPos*0.5+0.5; gl_Position=vec4(aPos,0.0,1.0); }";
    static final String PFS = "precision mediump float; uniform sampler2D uTex; uniform float uTime; varying vec2 vUv;"
        + "void main(){ vec3 c=vec3(0.0); for(int i=0;i<24;i++){ float a=float(i)*0.2618+uTime; vec2 o=vec2(cos(a),sin(a))*(0.002+0.0006*float(i));"
        + " c+=texture2D(uTex,vUv+o).rgb; } c/=24.0; float v=smoothstep(0.9,0.2,length(vUv-0.5)); gl_FragColor=vec4(c*v+0.04*sin(uTime+vUv.xyx*40.0),1.0); }";
    static final String IVS = "#version 300 es\nuniform mat4 uVp; uniform float uTime; in vec3 aPos; in vec3 aNormal; out vec3 vCol;"
        + "void main(){ float i=float(gl_InstanceID); vec3 o=vec3(mod(i,50.0)-25.0, sin(i*0.37+uTime)*2.0-6.0, floor(i/50.0)-45.0)*0.6;"
        + " vCol=(0.5+0.5*aNormal)*(0.4+0.6*fract(i*0.618)); gl_Position=uVp*vec4(aPos*0.2+o,1.0); }";
    static final String IFS = "#version 300 es\nprecision mediump float; in vec3 vCol; out vec4 o; void main(){ o=vec4(vCol,1.0); }";

    int shader(int type, String src, String name) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, src);
        GLES20.glCompileShader(s);
        int[] ok = new int[1];
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, ok, 0);
        if (ok[0] == 0) m.line(k + ".shader_error." + name, GLES20.glGetShaderInfoLog(s).replace('\n', ' '));
        return s;
    }

    int program(String vs, String fs, String name) {
        int p = GLES20.glCreateProgram();
        GLES20.glAttachShader(p, shader(GLES20.GL_VERTEX_SHADER, vs, name + ".vs"));
        GLES20.glAttachShader(p, shader(GLES20.GL_FRAGMENT_SHADER, fs, name + ".fs"));
        GLES20.glBindAttribLocation(p, 0, "aPos");
        GLES20.glBindAttribLocation(p, 1, "aNormal");
        GLES20.glBindAttribLocation(p, 2, "aUv");
        GLES20.glLinkProgram(p);
        int[] ok = new int[1];
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, ok, 0);
        if (ok[0] == 0) { m.line(k + ".link_error." + name, GLES20.glGetProgramInfoLog(p).replace('\n', ' ')); return 0; }
        return p;
    }

    static FloatBuffer buf(float[] a) {
        FloatBuffer b = ByteBuffer.allocateDirect(a.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        b.put(a).position(0);
        return b;
    }

    /** 36 vertices: position, normal, uv */
    static float[] cubeData() {
        float[][] nrm = { {0,0,1}, {0,0,-1}, {1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0} };
        float[][] u = { {1,0,0}, {-1,0,0}, {0,0,-1}, {0,0,1}, {1,0,0}, {1,0,0} };
        float[][] v = { {0,1,0}, {0,1,0}, {0,1,0}, {0,1,0}, {0,0,-1}, {0,0,1} };
        int[][] corner = { {-1,-1}, {1,-1}, {1,1}, {-1,-1}, {1,1}, {-1,1} };
        float[] out = new float[36 * 8];
        int p = 0;
        for (int f = 0; f < 6; f++) for (int c = 0; c < 6; c++) {
            int a = corner[c][0], b = corner[c][1];
            for (int i = 0; i < 3; i++) out[p++] = (nrm[f][i] + a * u[f][i] + b * v[f][i]) * 0.5f;
            for (int i = 0; i < 3; i++) out[p++] = nrm[f][i];
            out[p++] = a * 0.5f + 0.5f; out[p++] = b * 0.5f + 0.5f;
        }
        return out;
    }

    void err(String where) {
        int e = GLES20.glGetError();
        if (e != 0 && firstError == 0) { firstError = e; m.line(k + ".gl_error", "0x" + Integer.toHexString(e) + " at " + where); }
    }

    @Override public void onSurfaceCreated(GL10 gl, EGLConfig cfg) {
        m.line(k + ".vendor", GLES20.glGetString(GLES20.GL_VENDOR));
        m.line(k + ".renderer", GLES20.glGetString(GLES20.GL_RENDERER));
        m.line(k + ".version", GLES20.glGetString(GLES20.GL_VERSION));
        String ext = GLES20.glGetString(GLES20.GL_EXTENSIONS);
        m.line(k + ".extensions", ext == null ? 0 : ext.trim().split(" ").length);
        int[] v = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, v, 0);
        m.line(k + ".max_texture", v[0]);
        prog = program(VS, FS, "scene");
        cube = buf(cubeData());
        quad = buf(new float[] { -1, -1, 1, -1, -1, 1, 1, 1 });
        // a 256x256 checker with mipmaps
        ByteBuffer px = ByteBuffer.allocateDirect(256 * 256 * 4);
        for (int y = 0; y < 256; y++) for (int x = 0; x < 256; x++) {
            boolean on = ((x >> 5) + (y >> 5)) % 2 == 0;
            px.put((byte) (on ? 255 : 60)).put((byte) (on ? 220 : 90)).put((byte) (on ? 180 : 160)).put((byte) 255);
        }
        px.position(0);
        int[] t = new int[1];
        GLES20.glGenTextures(1, t, 0);
        tex = t[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 256, 256, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, px);
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        err("texture");
        if (heavy) {
            post = program(PVS, PFS, "post");
            GLES20.glGenTextures(1, t, 0);
            fboTex = t[0];
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTex);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 1024, 1024, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            int[] rb = new int[1];
            GLES20.glGenRenderbuffers(1, rb, 0);
            GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, rb[0]);
            GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, 1024, 1024);
            GLES20.glGenFramebuffers(1, t, 0);
            fbo = t[0];
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, fboTex, 0);
            GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, rb[0]);
            int st = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
            m.line(k + ".fbo", st == GLES20.GL_FRAMEBUFFER_COMPLETE ? "complete" : "0x" + Integer.toHexString(st));
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            if (es3) inst = program(IVS, IFS, "instanced");
            m.line(k + ".es3", es3 ? (inst != 0 ? "instancing" : "program failed") : "no GLES 3 here");
            err("fbo");
        }
        end = SystemClock.uptimeMillis() + secs * 1000L;
    }

    @Override public void onSurfaceChanged(GL10 gl, int w, int h) { this.w = w; this.h = h; m.line(k + ".surface", w + "x" + h); }

    void drawCubes(int vw, int vh, float time) {
        GLES20.glViewport(0, 0, vw, vh);
        GLES20.glClearColor(0.05f, 0.07f, 0.12f, 1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        Matrix.perspectiveM(proj, 0, 60, vw / (float) vh, 0.1f, 100);
        Matrix.setLookAtM(view, 0, 0, 3, 14, 0, 0, 0, 0, 1, 0);
        GLES20.glUseProgram(prog);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex);
        int uMvp = GLES20.glGetUniformLocation(prog, "uMvp"), uModel = GLES20.glGetUniformLocation(prog, "uModel"), uTint = GLES20.glGetUniformLocation(prog, "uTint");
        cube.position(0); GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, 32, cube);
        cube.position(3); GLES20.glVertexAttribPointer(1, 3, GLES20.GL_FLOAT, false, 32, cube);
        cube.position(6); GLES20.glVertexAttribPointer(2, 2, GLES20.GL_FLOAT, false, 32, cube);
        for (int i = 0; i < 3; i++) GLES20.glEnableVertexAttribArray(i);
        int side = (int) Math.ceil(Math.sqrt(cubes));
        for (int i = 0; i < cubes; i++) {
            float x = (i % side - side / 2f) * (20f / side), z = (i / side - side / 2f) * (20f / side);
            Matrix.setIdentityM(model, 0);
            Matrix.translateM(model, 0, x, (float) Math.sin(time * 2 + i * 0.3f) * 0.8f, z);
            Matrix.rotateM(model, 0, time * 90 + i * 7, 0.3f, 1, 0.2f);
            Matrix.scaleM(model, 0, 14f / side, 14f / side, 14f / side);
            Matrix.multiplyMM(tmp, 0, view, 0, model, 0);
            Matrix.multiplyMM(mvp, 0, proj, 0, tmp, 0);
            GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0);
            GLES20.glUniformMatrix4fv(uModel, 1, false, model, 0);
            GLES20.glUniform4f(uTint, 0.6f + 0.4f * (i % 3) / 2f, 0.7f, 0.6f + 0.4f * (i % 5) / 4f, 1);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
        }
        // a translucent sheet over the scene: blending over the whole target
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, 0, 0, 4);
        Matrix.scaleM(model, 0, 30, 30, 0.01f);
        Matrix.multiplyMM(tmp, 0, view, 0, model, 0);
        Matrix.multiplyMM(mvp, 0, proj, 0, tmp, 0);
        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0);
        GLES20.glUniformMatrix4fv(uModel, 1, false, model, 0);
        GLES20.glUniform4f(uTint, 0.3f, 0.5f, 1, 0.25f);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
        GLES20.glDisable(GLES20.GL_BLEND);
        if (inst != 0) {
            GLES20.glUseProgram(inst);
            Matrix.multiplyMM(tmp, 0, proj, 0, view, 0);
            GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(inst, "uVp"), 1, false, tmp, 0);
            GLES20.glUniform1f(GLES20.glGetUniformLocation(inst, "uTime"), time);
            GLES30.glDrawArraysInstanced(GLES20.GL_TRIANGLES, 0, 36, 2000);
        }
    }

    @Override public void onDrawFrame(GL10 gl) {
        if (reported) return;
        if (n < ts.length) ts[n++] = System.nanoTime();
        float time = (SystemClock.uptimeMillis() % 100000) / 1000f;
        if (prog == 0) { finish(); return; }
        if (heavy && post != 0) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);
            drawCubes(1024, 1024, time);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            GLES20.glViewport(0, 0, w, h);
            GLES20.glDisable(GLES20.GL_DEPTH_TEST);
            GLES20.glUseProgram(post);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTex);
            GLES20.glUniform1f(GLES20.glGetUniformLocation(post, "uTime"), time);
            GLES20.glDisableVertexAttribArray(1); GLES20.glDisableVertexAttribArray(2);
            quad.position(0);
            GLES20.glVertexAttribPointer(0, 2, GLES20.GL_FLOAT, false, 8, quad);
            GLES20.glEnableVertexAttribArray(0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        } else {
            drawCubes(w, h, time);
        }
        err("frame " + n);
        if (SystemClock.uptimeMillis() >= end) finish();
    }

    void finish() {
        reported = true;
        ByteBuffer px = ByteBuffer.allocateDirect(4);
        GLES20.glReadPixels(w / 2, h / 2, 1, 1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, px);
        m.line(k + ".middle_pixel", (px.get(0) & 0xff) + "," + (px.get(1) & 0xff) + "," + (px.get(2) & 0xff));
        m.line(k + ".cubes", cubes + (inst != 0 ? "+2000 instanced" : ""));
        m.frames(k, ts, n, hz);
        m.done();
    }
}
