package app.aemu.test;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** The test pattern through GLES 2 (API 8 and later; loaded only there): a shader program, vertex attributes. */
class Gl2Pattern implements GLSurfaceView.Renderer {
    final Base m;
    final FloatBuffer pos, col;
    int w, h, n, prog;

    Gl2Pattern(Base m) { this.m = m; float[][] c = Base.cells(); pos = Base.floats(c[0]); col = Base.floats(c[1]); }

    static void attach(Base m, GLSurfaceView v) {
        v.setEGLContextClientVersion(2);
        v.setRenderer(new Gl2Pattern(m));
    }

    static int shader(int type, String src) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, src);
        GLES20.glCompileShader(s);
        return s;
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig cfg) {
        m.line("pattern.gl2_renderer", GLES20.glGetString(GLES20.GL_RENDERER));
        m.line("pattern.gl2_version", GLES20.glGetString(GLES20.GL_VERSION));
        prog = GLES20.glCreateProgram();
        GLES20.glAttachShader(prog, shader(GLES20.GL_VERTEX_SHADER, "attribute vec2 p; attribute vec4 c; varying vec4 v; void main() { v = c; gl_Position = vec4(p, 0.0, 1.0); }"));
        GLES20.glAttachShader(prog, shader(GLES20.GL_FRAGMENT_SHADER, "precision mediump float; varying vec4 v; void main() { gl_FragColor = v; }"));
        GLES20.glBindAttribLocation(prog, 0, "p");
        GLES20.glBindAttribLocation(prog, 1, "c");
        GLES20.glLinkProgram(prog);
        int[] ok = new int[1];
        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, ok, 0);
        if (ok[0] == 0) m.line("pattern.gl2_link_error", GLES20.glGetProgramInfoLog(prog).replace('\n', ' '));
    }

    public void onSurfaceChanged(GL10 gl, int w, int h) { this.w = w; this.h = h; GLES20.glViewport(0, 0, w, h); }

    public void onDrawFrame(GL10 gl) {
        GLES20.glClearColor(0, 0, 0, 1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDisable(GLES20.GL_DITHER);
        GLES20.glUseProgram(prog);
        GLES20.glEnableVertexAttribArray(0);
        GLES20.glEnableVertexAttribArray(1);
        GLES20.glVertexAttribPointer(0, 2, GLES20.GL_FLOAT, false, 0, pos);
        GLES20.glVertexAttribPointer(1, 4, GLES20.GL_FLOAT, false, 0, col);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 96);
        if (++n == 10) {
            ByteBuffer px = ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder());
            GLES20.glReadPixels(0, 0, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, px);
            m.checkRead("gl2", px, w, h);
            int e = GLES20.glGetError();
            if (e != 0) m.line("pattern.gl2_error", "0x" + Integer.toHexString(e));
        }
    }
}
