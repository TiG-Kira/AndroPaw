/*
 * libGLESv1_CM_swiftshader.so in front of the real one (libGLESv1_CM_ss.so, same directory).
 *
 * SwiftShader 4.1's fixed-function pipeline on arm64 packs a colour of exactly 1.0 without saturation: an untextured
 * primitive in white, pure red, yellow... comes out black (0.999 is drawn right, a texel of 1.0 too). Found by the
 * tester's pattern probes (pattern.gl1_probe.*). The colours that reach the pipeline are pulled down to 0.9995 here,
 * which still reads back as 255: the current colour and client-side colour arrays (copied for the draw). A colour
 * array in a buffer object and lighting that saturates at 1.0 are not covered.
 *
 * Everything else is not defined here: libEGL looks every entry point up with dlsym() on this library's handle, and
 * that search goes on into its DT_NEEDED, the real renderer.
 */
#include <dlfcn.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

typedef float GLfloat; typedef int GLint; typedef int GLsizei; typedef unsigned GLenum; typedef unsigned char GLubyte;
typedef int GLfixed;
#define GL_COLOR_ARRAY 0x8076
#define GL_UNSIGNED_BYTE 0x1401
#define GL_UNSIGNED_SHORT 0x1403
#define GL_UNSIGNED_INT 0x1405
#define GL_FLOAT 0x1406
#define GL_FIXED 0x140C
#define GL_ARRAY_BUFFER_BINDING 0x8894
#define GL_ELEMENT_ARRAY_BUFFER_BINDING 0x8895
#define EXPORT __attribute__((visibility("default")))

#define TOP 0.9995f
#define TOPX 65503

static void *real(const char *name) {
    static void *h;
    if (!h) h = dlopen("libGLESv1_CM_ss.so", RTLD_NOW);
    /* (6.0's linker has no $ORIGIN: the egl directory is not searched) */
    if (!h) h = dlopen("/system/lib64/egl/libGLESv1_CM_ss.so", RTLD_NOW);
    return h ? dlsym(h, name) : NULL;
}
#define REAL(ret, name, args) static ret (*r) args; if (!r) r = (ret (*) args) real(#name)

static float cf(float v) { return v > TOP ? TOP : v; }

static void get_int(GLenum e, GLint *v) {
    static void (*g)(GLenum, GLint *);
    if (!g) g = (void (*)(GLenum, GLint *)) real("glGetIntegerv");
    *v = 0;
    if (g) g(e, v);
}

/* client state of this thread's context */
static __thread int arr_on, arr_vbo;
static __thread GLint arr_size;
static __thread GLenum arr_type;
static __thread GLsizei arr_stride;
static __thread const void *arr_ptr;
static __thread float *tmp;
static __thread size_t tmp_n;

EXPORT void glColor4f(GLfloat a, GLfloat b, GLfloat c, GLfloat d) {
    REAL(void, glColor4f, (GLfloat, GLfloat, GLfloat, GLfloat));
    r(cf(a), cf(b), cf(c), cf(d));
}

EXPORT void glColor4x(GLfixed a, GLfixed b, GLfixed c, GLfixed d) {
    REAL(void, glColor4x, (GLfixed, GLfixed, GLfixed, GLfixed));
    r(a > TOPX ? TOPX : a, b > TOPX ? TOPX : b, c > TOPX ? TOPX : c, d > TOPX ? TOPX : d);
}

EXPORT void glColor4ub(GLubyte a, GLubyte b, GLubyte c, GLubyte d) {
    glColor4f(a / 255.f, b / 255.f, c / 255.f, d / 255.f);
}

EXPORT void glColorPointer(GLint size, GLenum type, GLsizei stride, const void *p) {
    REAL(void, glColorPointer, (GLint, GLenum, GLsizei, const void *));
    r(size, type, stride, p);
    GLint vbo;
    get_int(GL_ARRAY_BUFFER_BINDING, &vbo);
    arr_size = size; arr_type = type; arr_stride = stride; arr_ptr = p; arr_vbo = vbo != 0;
}

EXPORT void glEnableClientState(GLenum a) {
    REAL(void, glEnableClientState, (GLenum));
    if (a == GL_COLOR_ARRAY) arr_on = 1;
    r(a);
}

EXPORT void glDisableClientState(GLenum a) {
    REAL(void, glDisableClientState, (GLenum));
    if (a == GL_COLOR_ARRAY) arr_on = 0;
    r(a);
}

/* the colour array's vertices 0..n-1 as clamped floats; 0 when there is nothing to change */
static int swap_colors(GLint n) {
    if (!arr_on || arr_vbo || !arr_ptr || n <= 0 || arr_size != 4) return 0;
    size_t one = arr_type == GL_FLOAT || arr_type == GL_FIXED ? 4 : arr_type == GL_UNSIGNED_BYTE ? 1 : 0;
    if (!one) return 0;
    size_t stride = arr_stride ? (size_t) arr_stride : one * 4;
    if (tmp_n < (size_t) n * 4) {
        free(tmp);
        tmp_n = (size_t) n * 4 + 1024;
        tmp = malloc(tmp_n * sizeof(float));
        if (!tmp) { tmp_n = 0; return 0; }
    }
    const uint8_t *s = arr_ptr;
    for (GLint i = 0; i < n; i++, s += stride)
        for (int k = 0; k < 4; k++) {
            float v;
            if (arr_type == GL_FLOAT) memcpy(&v, s + 4 * k, 4);
            else if (arr_type == GL_FIXED) { int32_t x; memcpy(&x, s + 4 * k, 4); v = x / 65536.f; }
            else v = s[k] / 255.f;
            tmp[i * 4 + k] = cf(v);
        }
    REAL(void, glColorPointer, (GLint, GLenum, GLsizei, const void *));
    r(4, GL_FLOAT, 0, tmp);
    return 1;
}

static void restore_colors(void) {
    REAL(void, glColorPointer, (GLint, GLenum, GLsizei, const void *));
    r(arr_size, arr_type, arr_stride, arr_ptr);
}

EXPORT void glDrawArrays(GLenum mode, GLint first, GLsizei count) {
    REAL(void, glDrawArrays, (GLenum, GLint, GLsizei));
    int sw = count > 0 && first >= 0 && swap_colors(first + count);
    r(mode, first, count);
    if (sw) restore_colors();
}

EXPORT void glDrawElements(GLenum mode, GLsizei count, GLenum type, const void *idx) {
    REAL(void, glDrawElements, (GLenum, GLsizei, GLenum, const void *));
    int sw = 0;
    if (arr_on && !arr_vbo && idx && count > 0) {
        GLint eb;
        get_int(GL_ELEMENT_ARRAY_BUFFER_BINDING, &eb);
        if (!eb) {
            uint32_t mx = 0;
            for (GLsizei i = 0; i < count; i++) {
                uint32_t v = type == GL_UNSIGNED_BYTE ? ((const uint8_t *) idx)[i]
                    : type == GL_UNSIGNED_SHORT ? ((const uint16_t *) idx)[i] : ((const uint32_t *) idx)[i];
                if (v > mx) mx = v;
            }
            if (mx < (1u << 24)) sw = swap_colors((GLint) mx + 1);
        }
    }
    r(mode, count, type, idx);
    if (sw) restore_colors();
}
