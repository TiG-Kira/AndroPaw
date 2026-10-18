/*
 * libaemugl.so - the host side of the GLES bridge (see guest.c): the application's renderer for guest programs.
 *
 * GlHost.start(path) listens on a unix socket. Every drawing thread of a guest program is one connection and one
 * thread here; that thread makes the program's context current (a real GLES 3 context of the phone's GPU, on a
 * 1x1 pbuffer) and plays the command stream. A guest window is a framebuffer object of the context: the guest's
 * framebuffer 0 is bound as that object, and a swap reads it back and sends the pixels.
 *
 * Nothing here is specific to a GPU vendor: EGL 1.4 and GLES 3.0 only.
 */
#define _GNU_SOURCE
#define EGL_EGLEXT_PROTOTYPES
#define GL_GLEXT_PROTOTYPES
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <GLES3/gl32.h>
#include <GLES2/gl2ext.h>
#include <android/hardware_buffer.h>
#include <android/log.h>
#include <android/native_window_jni.h>
#include <errno.h>
#include <jni.h>
#include <pthread.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <time.h>
#include <unistd.h>
#include "common.h"

#define LOG(...) __android_log_print(ANDROID_LOG_INFO, "aemu-gl", __VA_ARGS__)

/* A guest window: a texture and a depth/stencil buffer; with a multisampled config (samples) the guest draws into
 * multisampled buffers (msfbo), resolved into the texture before it is read. */
struct surf { uint32_t key; int w, h, samples; GLuint fbo, col, rbo, msfbo, mscol, msds; };
struct hctx {
    uint32_t id;
    int pid, version, busy, doomed, fresh;
    EGLContext egl;
    EGLSurface pb;
    struct surf surfs[8];
    GLuint draw_fb, read_fb;        /* the guest's bindings; 0 is the current surface */
    /* Vertices (and indices) that arrive with a draw. Skia's particles: hundreds of small draws a frame, each with
     * five arrays - a buffer each, re-made for every draw, cost 60 ms a frame. With GL_EXT_buffer_storage they
     * are copied into a buffer mapped once for good (pring), one draw after the other, a fence per eighth of it
     * so that nothing a draw still reads is overwritten; without it, every draw's arrays are one new storage of
     * one buffer (ring). An unsynchronized map or a write into a buffer in use was slower still on Adreno. */
    GLuint ring, pring; uint8_t *pmap; long poff; GLsync pfence[8]; unsigned ring_gen;
    struct { long off; unsigned gen; uint32_t n; uint8_t *copy; } last[16];     /* where each array's bytes were put */
    /* An external texture of the guest is a 2D texture here. Per texture unit: what the guest has bound to each of
     * the two targets, and which of them the real 2D binding is now. */
    GLuint g2d[32], gext[32];
    /* textures that are a guest buffer (an EGL image bound to them), framebuffer objects with one attached, and
     * the objects a finished frame is copied into its buffer through */
    struct { GLuint tex; uint64_t id; } tb[64];
    struct { GLuint fbo; uint64_t id; } fbm[16];
    struct { uint64_t id; unsigned long serial; GLuint tex, fbo; } blit[6];
    uint8_t is_ext[32];
    int unit;
    uint32_t root;                  /* the id of the first context of its share group: programs and shaders are its */
    GLuint prog; int prog_ext;      /* the program in use, and whether one of its shaders sampled an external texture */
    char *ext; char **extv; int extn;
    struct hctx *next;
};
struct conn {
    int fd, pid, dead;
    struct hctx *ctx;
    struct surf *surf;
    uint8_t *in; size_t in_cap, in_a, in_b;
    uint8_t *rep; size_t rep_n, rep_cap;
    void *scr[16]; int nscr;
    uint8_t *px; size_t px_cap;
    unsigned long frames, unknown;
    /* the connection of the guest's composer: the context that puts finished frames on the phone's screen */
    EGLContext pctx;
    EGLSurface psurf;
    unsigned pgen;
    GLuint pprog;
    struct { uint64_t id; unsigned long serial, use; GLuint tex; } ptex[4];
};
static __thread struct conn *C;
static pthread_mutex_t g_lock = PTHREAD_MUTEX_INITIALIZER;
static struct hctx *g_ctx;
static uint32_t g_ids;
static EGLDisplay g_dpy;
static EGLConfig g_cfg;
/* the application's view the guest's screen is shown in (GlHost.surface), and how many frames went to it */
static pthread_mutex_t g_win_lock = PTHREAD_MUTEX_INITIALIZER;
static ANativeWindow *g_win;
static unsigned g_win_gen;
static volatile uint64_t g_presented;
static int g_listen = -1;
/* the GLES 3 minor version the guest is told: 1 when the phone's contexts are 3.1 or newer (every 3.1 entry point
 * is carried; 3.2's are not), else 0 */
static int g_minor;
static struct { int pid, n; } g_pids[64];

/* ---- the stream ---- */
static void wr_all(const void *p, size_t n) {
    const char *c = p;
    while (n && !C->dead) {
        ssize_t k = send(C->fd, c, n, MSG_NOSIGNAL);
        if (k <= 0) { if (k < 0 && errno == EINTR) continue; C->dead = 1; return; }
        c += k; n -= (size_t)k;
    }
}
static void reply(const void *p, uint32_t n) {
    if (C->rep_n + n > C->rep_cap) {
        size_t cap = (C->rep_n + n) * 2 + 4096;
        uint8_t *r = realloc(C->rep, cap);
        if (!r) { C->dead = 1; return; }
        C->rep = r; C->rep_cap = cap;
    }
    memcpy(C->rep + C->rep_n, p, n); C->rep_n += n;
}
static void reply32(uint32_t v) { reply(&v, 4); }
static void reply64(uint64_t v) { reply(&v, 8); }
static void reply_flush(void) { if (C->rep_n) { wr_all(C->rep, C->rep_n); C->rep_n = 0; } }
/* memory for what a call gives back: zeroed, with room to spare for a name whose length the tables got wrong */
static void *scratch(uint32_t n) {
    void *p = calloc(1, (size_t)n + 1024);
    if (C->nscr < 16) C->scr[C->nscr++] = p;
    return p;
}
static inline uint32_t rd32(const uint8_t **p) { uint32_t v; memcpy(&v, *p, 4); *p += 4; return v; }
static inline float rdf(const uint8_t **p) { float v; memcpy(&v, *p, 4); *p += 4; return v; }
static inline uint64_t rd64(const uint8_t **p) { uint64_t v; memcpy(&v, *p, 8); *p += 8; return v; }
static inline const void *rdblob(const uint8_t **p, uint32_t *n) {
    *n = rd32(p);
    if (*n == NIL) { *n = 0; return 0; }
    const void *r = *p;
    *p += (*n + 3u) & ~3u;
    return r;
}
static const void *rd_pixels(const uint8_t **p) {
    uint32_t kind = rd32(p), n;
    if (kind == 2) return (const void *)(uintptr_t)rd64(p);
    return kind == 1 ? rdblob(p, &n) : 0;
}

static uint32_t h_get_count(GLenum pname) {
    uint32_t name = 0, c = aemu_get_count0(pname, &name);
    if (!c) { GLint v = 0; glGetIntegerv(name, &v); c = v > 0 ? (uint32_t)v : 0; }
    return c;
}
#define aemu_get_count(pname) h_get_count(pname)

static void buf_mark(uint64_t id);
static uint64_t tb_find(struct hctx *x, GLuint tex) {
    for (int i = 0; tex && i < 64; i++) if (x->tb[i].tex == tex) return x->tb[i].id;
    return 0;
}
static void tb_set(struct hctx *x, GLuint tex, uint64_t id) {
    int spare = -1;
    for (int i = 0; i < 64; i++) {
        if (x->tb[i].tex == tex) { x->tb[i].id = id; if (!id) x->tb[i].tex = 0; return; }
        if (!x->tb[i].tex && spare < 0) spare = i;
    }
    if (id && spare >= 0) { x->tb[spare].tex = tex; x->tb[spare].id = id; }
}
static void fbm_set(struct hctx *x, GLuint fbo, uint64_t id) {
    int spare = -1;
    for (int i = 0; i < 16; i++) {
        if (x->fbm[i].fbo == fbo) { x->fbm[i].id = id; if (!id) x->fbm[i].fbo = 0; return; }
        if (!x->fbm[i].fbo && spare < 0) spare = i;
    }
    if (id && spare >= 0) { x->fbm[spare].fbo = fbo; x->fbm[spare].id = id; }
}

/* ---- the guest's framebuffer 0 ---- */
static GLuint surf_fb(const struct surf *s) { return s->msfbo ? s->msfbo : s->fbo; }
static GLuint real_fb(GLuint fb) { return fb ? fb : C->surf ? surf_fb(C->surf) : 0; }
/* a multisampled window's pixels into its texture (s->fbo); the bindings stay as they were */
static void resolve(struct surf *s) {
    if (!s || !s->msfbo) return;
    GLboolean scissor = glIsEnabled(GL_SCISSOR_TEST);
    GLint rd = 0, dr = 0;
    glGetIntegerv(GL_READ_FRAMEBUFFER_BINDING, &rd); glGetIntegerv(GL_DRAW_FRAMEBUFFER_BINDING, &dr);
    if (scissor) glDisable(GL_SCISSOR_TEST);
    glBindFramebuffer(GL_READ_FRAMEBUFFER, s->msfbo);
    glBindFramebuffer(GL_DRAW_FRAMEBUFFER, s->fbo);
    glBlitFramebuffer(0, 0, s->w, s->h, 0, 0, s->w, s->h, GL_COLOR_BUFFER_BIT, GL_NEAREST);
    glBindFramebuffer(GL_READ_FRAMEBUFFER, (GLuint)rd); glBindFramebuffer(GL_DRAW_FRAMEBUFFER, (GLuint)dr);
    if (scissor) glEnable(GL_SCISSOR_TEST);
}
/* A read of the guest's framebuffer 0 when it is multisampled (glReadPixels, glCopyTex*, a scaled blit): from the
 * resolved texture. Returns whether the read binding was moved (read_done puts it back). */
static int read_ms(void) {
    struct surf *s = C->surf;
    if (!C->ctx || C->ctx->read_fb || !s || !s->msfbo) return 0;
    resolve(s);
    glBindFramebuffer(GL_READ_FRAMEBUFFER, s->fbo);
    return 1;
}
static void read_done(int moved) { if (moved) glBindFramebuffer(GL_READ_FRAMEBUFFER, surf_fb(C->surf)); }
static void h_glCopyTexImage2D(GLenum t, GLint l, GLenum f, GLint x, GLint y, GLsizei w, GLsizei h, GLint b) { int m = read_ms(); glCopyTexImage2D(t, l, f, x, y, w, h, b); read_done(m); }
static void h_glCopyTexSubImage2D(GLenum t, GLint l, GLint xo, GLint yo, GLint x, GLint y, GLsizei w, GLsizei h) { int m = read_ms(); glCopyTexSubImage2D(t, l, xo, yo, x, y, w, h); read_done(m); }
static void h_glCopyTexSubImage3D(GLenum t, GLint l, GLint xo, GLint yo, GLint zo, GLint x, GLint y, GLsizei w, GLsizei h) { int m = read_ms(); glCopyTexSubImage3D(t, l, xo, yo, zo, x, y, w, h); read_done(m); }
static void h_glBlitFramebuffer(GLint a, GLint b, GLint c, GLint d, GLint e, GLint f, GLint g, GLint h, GLbitfield mask, GLenum filter) {
    /* (a multisampled source is resolved only 1:1: a scaled or moved blit of colour reads the resolved texture) */
    int m = (c - a != g - e || d - b != h - f || a != e || b != f) && mask == GL_COLOR_BUFFER_BIT ? read_ms() : 0;
    glBlitFramebuffer(a, b, c, d, e, f, g, h, mask, filter);
    read_done(m);
}
static void h_glBindFramebuffer(GLenum target, GLuint fb) {
    struct hctx *x = C->ctx;
    if (x) {
        if (target == GL_FRAMEBUFFER || target == GL_DRAW_FRAMEBUFFER) x->draw_fb = fb;
        if (target == GL_FRAMEBUFFER || target == GL_READ_FRAMEBUFFER) x->read_fb = fb;
    }
    glBindFramebuffer(target, real_fb(fb));
    /* drawing into a guest buffer is about to start: the host's copy becomes the newer one */
    if (x && fb && target != GL_READ_FRAMEBUFFER) for (int i = 0; i < 16; i++) if (x->fbm[i].fbo == fb) { buf_mark(x->fbm[i].id); break; }
}
static void h_glFramebufferTexture2D(GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level) {
    struct hctx *x = C->ctx;
    glFramebufferTexture2D(target, attachment, textarget, texture, level);
    if (!x || attachment != GL_COLOR_ATTACHMENT0) return;
    GLuint fb = target == GL_READ_FRAMEBUFFER ? x->read_fb : x->draw_fb;
    uint64_t id = tb_find(x, texture);
    if (fb) fbm_set(x, fb, id);
    if (id) buf_mark(id);
}
static void h_glDeleteFramebuffers(GLsizei n, const GLuint *ids) {
    struct hctx *x = C->ctx;
    glDeleteFramebuffers(n, ids);
    for (int i = 0; x && ids && i < n; i++) {
        if (!ids[i]) continue;
        fbm_set(x, ids[i], 0);
        if (x->draw_fb == ids[i]) { x->draw_fb = 0; glBindFramebuffer(GL_DRAW_FRAMEBUFFER, real_fb(0)); }
        if (x->read_fb == ids[i]) { x->read_fb = 0; glBindFramebuffer(GL_READ_FRAMEBUFFER, real_fb(0)); }
    }
}
static void ext_build(struct hctx *x);
static void h_glGetIntegerv(GLenum pname, GLint *data) {
    struct hctx *x = C->ctx;
    switch (pname) {
    case GL_DRAW_FRAMEBUFFER_BINDING: if (x) { *data = (GLint)x->draw_fb; return; } break;
    case GL_READ_FRAMEBUFFER_BINDING: if (x) { *data = (GLint)x->read_fb; return; } break;
    case GL_NUM_EXTENSIONS: if (x) { if (!x->ext) ext_build(x); *data = x->extn; return; } break;
    case GL_TEXTURE_BINDING_2D: if (x) { *data = (GLint)x->g2d[x->unit]; return; } break;
    case 0x8D67: if (x) { *data = (GLint)x->gext[x->unit]; return; } break;      /* GL_TEXTURE_BINDING_EXTERNAL_OES */
    /* no program binaries: a program loaded from one has no shader source here, and whether it samples an external
     * texture (fix_units) would not be known - Launcher3's icons of 14 went black from the second boot on, when hwui
     * took its programs from its shader cache. Programs are compiled from source each time instead. */
    case 0x87FE: *data = 0; return;     /* GL_NUM_PROGRAM_BINARY_FORMATS */
    case GL_MAJOR_VERSION: *data = 3; return;
    case GL_MINOR_VERSION: *data = g_minor; return;
    }
    glGetIntegerv(pname, data);
}
/* (the guest names the buffers of framebuffer 0 the way a window's are named) */
static void h_glReadBuffer(GLenum src) {
    glReadBuffer(src == GL_BACK && C->ctx && !C->ctx->read_fb ? GL_COLOR_ATTACHMENT0 : src);
}
static void h_glDrawBuffers(GLsizei n, const GLenum *bufs) {
    GLenum one = GL_COLOR_ATTACHMENT0;
    if (n == 1 && bufs && bufs[0] == GL_BACK && C->ctx && !C->ctx->draw_fb) bufs = &one;
    glDrawBuffers(n, bufs);
}
static void invalidate(GLenum target, GLsizei n, const GLenum *att, int sub, GLint x, GLint y, GLsizei w, GLsizei h) {
    GLenum t[8];
    struct hctx *c = C->ctx;
    int zero = c && !(target == GL_READ_FRAMEBUFFER ? c->read_fb : c->draw_fb);
    if (zero && att && n > 0 && n <= 8) {
        for (int i = 0; i < n; i++)
            t[i] = att[i] == GL_COLOR ? GL_COLOR_ATTACHMENT0 : att[i] == GL_DEPTH ? GL_DEPTH_ATTACHMENT : att[i] == GL_STENCIL ? GL_STENCIL_ATTACHMENT : att[i];
        att = t;
    }
    if (sub) glInvalidateSubFramebuffer(target, n, att, x, y, w, h); else glInvalidateFramebuffer(target, n, att);
}
static void h_glInvalidateFramebuffer(GLenum target, GLsizei n, const GLenum *att) { invalidate(target, n, att, 0, 0, 0, 0, 0); }
static void h_glInvalidateSubFramebuffer(GLenum target, GLsizei n, const GLenum *att, GLint x, GLint y, GLsizei w, GLsizei h) { invalidate(target, n, att, 1, x, y, w, h); }

/* ---- external textures ---- */
#define TEX_EXT 0x8D65
static void h_glActiveTexture(GLenum t) {
    if (C->ctx && t >= GL_TEXTURE0 && t < GL_TEXTURE0 + 32) C->ctx->unit = (int)(t - GL_TEXTURE0);
    glActiveTexture(t);
}
static void h_glBindTexture(GLenum target, GLuint name) {
    struct hctx *x = C->ctx;
    if (!x || (target != GL_TEXTURE_2D && target != TEX_EXT)) { glBindTexture(target, name); return; }
    int u = x->unit;
    if (target == GL_TEXTURE_2D) { x->g2d[u] = name; x->is_ext[u] = 0; glBindTexture(GL_TEXTURE_2D, name); return; }
    x->gext[u] = name;
    if (!name) { x->is_ext[u] = 0; glBindTexture(GL_TEXTURE_2D, x->g2d[u]); return; }
    x->is_ext[u] = 1;
    glBindTexture(GL_TEXTURE_2D, name);
    GLint f = 0;
    glGetTexParameteriv(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, &f);
    if (f == GL_NEAREST_MIPMAP_LINEAR) {    /* still a 2D texture's defaults: an external one starts like this */
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    }
}
/* the real target of a call on one of the two, with that one's texture bound */
static GLenum xt(GLenum target) {
    struct hctx *x = C->ctx;
    if (!x || (target != GL_TEXTURE_2D && target != TEX_EXT)) return target;
    int u = x->unit, ext = target == TEX_EXT;
    if (x->is_ext[u] != ext) { x->is_ext[u] = (uint8_t)ext; glBindTexture(GL_TEXTURE_2D, ext ? x->gext[u] : x->g2d[u]); }
    return GL_TEXTURE_2D;
}
static void h_glTexParameteri(GLenum t, GLenum p, GLint v) { glTexParameteri(xt(t), p, v); }
static void h_glTexParameterf(GLenum t, GLenum p, GLfloat v) { glTexParameterf(xt(t), p, v); }
static void h_glTexParameteriv(GLenum t, GLenum p, const GLint *v) { glTexParameteriv(xt(t), p, v); }
static void h_glTexParameterfv(GLenum t, GLenum p, const GLfloat *v) { glTexParameterfv(xt(t), p, v); }
static void h_glGetTexParameteriv(GLenum t, GLenum p, GLint *v) { glGetTexParameteriv(xt(t), p, v); }
static void h_glGetTexParameterfv(GLenum t, GLenum p, GLfloat *v) { glGetTexParameterfv(xt(t), p, v); }
static void h_glGenerateMipmap(GLenum t) { glGenerateMipmap(xt(t)); }
static void h_glDeleteTextures(GLsizei n, const GLuint *ids) {
    struct hctx *x = C->ctx;
    glDeleteTextures(n, ids);
    for (int i = 0; x && ids && i < n; i++) if (ids[i]) tb_set(x, ids[i], 0);
    for (int i = 0; x && ids && i < n; i++) for (int u = 0; ids[i] && u < 32; u++) {
        if (x->g2d[u] == ids[i]) x->g2d[u] = 0;
        if (x->gext[u] == ids[i]) { x->gext[u] = 0; x->is_ext[u] = 0; }
    }
}
static int trace_on;        /* files/gltrace present: every call is logged (debugging) */
/* a shader that samples an external texture samples a 2D one */
static void no_external(char *s) {
    for (char *p = s; (p = strstr(p, "samplerExternalOES")); ) memcpy(p, "sampler2D         ", 18);
    for (char *p = s; (p = strstr(p, "GL_OES_EGL_image_external")); ) {
        char *b = p, *e = strchr(p, '\n');
        while (b > s && b[-1] != '\n') b--;
        if (!e) e = p + strlen(p);
        memset(b, ' ', (size_t)(e - b));
        p = e;
    }
}

/* ---- the guest's graphic buffers ----
 * A buffer the guest binds to a texture (an EGL image) is a hardware buffer of the phone here, found by the
 * number the guest's gralloc gave it: every context that binds it gets the same storage, whatever program it
 * belongs to, as on a phone. The guest's memory is copied in when it was written since the last time. */
struct hbuf {
    uint64_t id, gen, frame;        /* gen: the guest's write count its pixels were last taken at; frame: the last frame drawn here */
    int w, h, gpu;                  /* gpu: drawn into since the guest last read it */
    AHardwareBuffer *ahb; EGLImageKHR img; EGLSyncKHR sync;
    unsigned long use, serial;
};
static pthread_cond_t g_cond = PTHREAD_COND_INITIALIZER;
static unsigned long g_serial;
static struct hbuf g_buf[512];
static struct { uint32_t id; EGLImageKHR img; } g_himg[256];     /* images of guest contexts' textures */
static uint32_t g_himg_ids;
static unsigned long g_use;
static void buf_drop(struct hbuf *b) {
    if (b->img != EGL_NO_IMAGE_KHR) eglDestroyImageKHR(g_dpy, b->img);
    if (b->sync != EGL_NO_SYNC_KHR) eglDestroySyncKHR(g_dpy, b->sync);
    if (b->ahb) AHardwareBuffer_release(b->ahb);
    memset(b, 0, sizeof(*b));
}
static struct hbuf *buf_get(uint64_t id, int w, int h, int make) {
    struct hbuf *b = 0, *spare = 0;
    pthread_mutex_lock(&g_lock);
    for (int i = 0; i < 512; i++) {
        if (g_buf[i].id == id) { b = &g_buf[i]; break; }
        if (!spare || (spare->id && (!g_buf[i].id || g_buf[i].use < spare->use))) spare = &g_buf[i];
    }
    if (b && (b->w != w || b->h != h)) { if (make) { buf_drop(b); spare = b; } b = 0; }
    if (!b && make && w > 0 && h > 0) {
        static const EGLint keep[] = { EGL_IMAGE_PRESERVED_KHR, EGL_TRUE, EGL_NONE };
        AHardwareBuffer_Desc d = { (uint32_t)w, (uint32_t)h, 1, AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM,
            AHARDWAREBUFFER_USAGE_GPU_SAMPLED_IMAGE | AHARDWAREBUFFER_USAGE_GPU_COLOR_OUTPUT |
            AHARDWAREBUFFER_USAGE_CPU_READ_RARELY | AHARDWAREBUFFER_USAGE_CPU_WRITE_RARELY, 0, 0, 0 };
        if (spare->id) buf_drop(spare);
        if (AHardwareBuffer_allocate(&d, &spare->ahb) == 0) {
            spare->img = eglCreateImageKHR(g_dpy, EGL_NO_CONTEXT, EGL_NATIVE_BUFFER_ANDROID, eglGetNativeClientBufferANDROID(spare->ahb), keep);
            if (spare->img != EGL_NO_IMAGE_KHR) { b = spare; b->id = id; b->w = w; b->h = h; b->serial = ++g_serial; }
            else { LOG("no EGL image of a %dx%d buffer: 0x%x", w, h, eglGetError()); buf_drop(spare); }
        } else LOG("the phone gave no %dx%d hardware buffer", w, h);
    }
    if (b) b->use = ++g_use;
    pthread_mutex_unlock(&g_lock);
    return b;
}
static void buf_mark(uint64_t id) {
    pthread_mutex_lock(&g_lock);
    for (int i = 0; i < 512; i++) if (g_buf[i].id == id) { g_buf[i].gpu = 1; break; }
    pthread_mutex_unlock(&g_lock);
}
static void buf_fill(struct hbuf *b, int w, int h, uint32_t stride, uint32_t format, const uint8_t *src, uint32_t n) {
    AHardwareBuffer_Desc d;
    uint8_t *va = 0;
    uint32_t bpp = format == 1 || format == 2 ? 4 : format == 3 ? 3 : format == 4 ? 2 : 0;
    if (!bpp || stride < (uint32_t)w || (uint64_t)stride * (uint32_t)h * bpp > n) return;
    AHardwareBuffer_describe(b->ahb, &d);
    if (AHardwareBuffer_lock(b->ahb, AHARDWAREBUFFER_USAGE_CPU_WRITE_RARELY, -1, 0, (void **)&va) != 0 || !va) return;
    for (int y = 0; y < h; y++) {
        const uint8_t *s = src + (size_t)y * stride * bpp;
        uint32_t *o = (uint32_t *)(va + (size_t)y * d.stride * 4);
        if (format == 1) memcpy(o, s, (size_t)w * 4);
        else if (format == 2) for (int x = 0; x < w; x++) { uint32_t v; memcpy(&v, s + x * 4, 4); o[x] = v | 0xff000000u; }
        else if (format == 3) for (int x = 0; x < w; x++) o[x] = s[x * 3] | (uint32_t)s[x * 3 + 1] << 8 | (uint32_t)s[x * 3 + 2] << 16 | 0xff000000u;
        else for (int x = 0; x < w; x++) {
            uint16_t v; memcpy(&v, s + x * 2, 2);
            uint32_t r = v >> 11, g = v >> 5 & 63, bl = v & 31;
            o[x] = (r << 3 | r >> 2) | (g << 2 | g >> 4) << 8 | (bl << 3 | bl >> 2) << 16 | 0xff000000u;
        }
    }
    AHardwareBuffer_unlock(b->ahb, 0);
}

/* ---- calls decoded by hand ---- */
#define NOT_SENT(name) static void hd_##name(const uint8_t *p, uint32_t len) { (void)p; (void)len; }
NOT_SENT(glGetString) NOT_SENT(glGetStringi) NOT_SENT(glMapBufferRange) NOT_SENT(glUnmapBuffer) NOT_SENT(glFlushMappedBufferRange)
NOT_SENT(glGetBufferPointerv) NOT_SENT(glGetVertexAttribPointerv) NOT_SENT(glGetUniformfv) NOT_SENT(glGetUniformiv)
NOT_SENT(glGetUniformuiv) NOT_SENT(glDrawRangeElements)
static void hd_glFlush(const uint8_t *p, uint32_t len) { (void)p; (void)len; glFlush(); }
static void hd_glFinish(const uint8_t *p, uint32_t len) { (void)p; (void)len; glFinish(); reply32(1); }
static void hd_glVertexAttribPointer(const uint8_t *p, uint32_t len) {
    GLuint index = rd32(&p); GLint size = (GLint)rd32(&p); GLenum type = rd32(&p); GLboolean norm = (GLboolean)rd32(&p); GLsizei stride = (GLsizei)rd32(&p);
    (void)len;
    glVertexAttribPointer(index, size, type, norm, stride, (const void *)(uintptr_t)rd64(&p));
}
static void hd_glVertexAttribIPointer(const uint8_t *p, uint32_t len) {
    GLuint index = rd32(&p); GLint size = (GLint)rd32(&p); GLenum type = rd32(&p); GLsizei stride = (GLsizei)rd32(&p);
    (void)len;
    glVertexAttribIPointer(index, size, type, stride, (const void *)(uintptr_t)rd64(&p));
}
static void fix_units(void);
static void hd_glDrawArrays(const uint8_t *p, uint32_t len) { GLenum m = rd32(&p); GLint f = (GLint)rd32(&p); (void)len; fix_units(); glDrawArrays(m, f, (GLsizei)rd32(&p)); }
static void hd_glDrawArraysInstanced(const uint8_t *p, uint32_t len) {
    GLenum m = rd32(&p); GLint f = (GLint)rd32(&p); GLsizei c = (GLsizei)rd32(&p);
    (void)len;
    fix_units();
    glDrawArraysInstanced(m, f, c, (GLsizei)rd32(&p));
}
static void hd_glDrawElements(const uint8_t *p, uint32_t len) {
    GLenum m = rd32(&p); GLsizei c = (GLsizei)rd32(&p); GLenum t = rd32(&p);
    (void)len;
    fix_units();
    glDrawElements(m, c, t, (const void *)(uintptr_t)rd64(&p));
}
static void hd_glDrawElementsInstanced(const uint8_t *p, uint32_t len) {
    GLenum m = rd32(&p); GLsizei c = (GLsizei)rd32(&p); GLenum t = rd32(&p); const void *o = (const void *)(uintptr_t)rd64(&p);
    (void)len;
    fix_units();
    glDrawElementsInstanced(m, c, t, o, (GLsizei)rd32(&p));
}
/* Shaders and programs that sampled an external texture before no_external() made it a 2D one. A texture unit has
 * one 2D binding here for the guest's two (2D and external): before a draw it is set to the one the program reads -
 * a program may draw after an external texture went to a unit whose 2D texture it still counts on (Skia's text
 * after the app icons of Launcher3, which are hardware bitmaps: the labels sampled the last icon). */
static struct { uint32_t root; GLuint name; uint8_t prog; } g_extobj[1024];
static int ext_has(uint32_t root, GLuint name, int prog) {
    int r = 0;
    pthread_mutex_lock(&g_lock);
    for (int i = 0; i < 1024; i++) if (g_extobj[i].name == name && g_extobj[i].root == root && g_extobj[i].prog == prog) { r = 1; break; }
    pthread_mutex_unlock(&g_lock);
    return r;
}
static void ext_mark(uint32_t root, GLuint name, int prog, int on) {
    pthread_mutex_lock(&g_lock);
    int spare = -1, at = -1;
    for (int i = 0; i < 1024; i++) {
        if (g_extobj[i].name == name && g_extobj[i].root == root && g_extobj[i].prog == prog) { at = i; break; }
        if (!g_extobj[i].name && spare < 0) spare = i;
    }
    if (!on && at >= 0) g_extobj[at].name = 0;
    if (on && at < 0 && spare >= 0) { g_extobj[spare].root = root; g_extobj[spare].name = name; g_extobj[spare].prog = (uint8_t)prog; }
    pthread_mutex_unlock(&g_lock);
}
static void h_glLinkProgram(GLuint program) {
    struct hctx *x = C->ctx;
    if (x) {
        GLuint sh[16]; GLsizei n = 0; int on = 0;
        glGetAttachedShaders(program, 16, &n, sh);
        for (GLsizei i = 0; i < n && !on; i++) on = ext_has(x->root, sh[i], 0);
        ext_mark(x->root, program, 1, on);
        if (x->prog == program) x->prog_ext = on;
    }
    glLinkProgram(program);
}
static void h_glUseProgram(GLuint program) {
    struct hctx *x = C->ctx;
    if (x) { x->prog = program; x->prog_ext = program && ext_has(x->root, program, 1); }
    glUseProgram(program);
}
static void fix_units(void) {
    struct hctx *x = C->ctx;
    static int off = -1;        /* DEBUG: files/glnofix switches this off (read once per run) */
    if (off < 0) off = access("/data/data/app.aemu/files/glnofix", F_OK) == 0;
    if (!x || off) return;
    int want = x->prog_ext, moved = 0;
    for (int u = 0; u < 32; u++) {
        if (want ? !(x->gext[u] && !x->is_ext[u]) : !x->is_ext[u]) continue;
        glActiveTexture(GL_TEXTURE0 + u); moved = 1;
        glBindTexture(GL_TEXTURE_2D, want ? x->gext[u] : x->g2d[u]);
        x->is_ext[u] = (uint8_t)want;
    }
    if (moved) glActiveTexture(GL_TEXTURE0 + x->unit);
}
static void hd_glShaderSource(const uint8_t *p, uint32_t len) {
    GLuint shader = rd32(&p); uint32_t n; const GLchar *s = rdblob(&p, &n);
    char *own = scratch(n + 1);
    (void)len;
    if (!s || !own) return;
    memcpy(own, s, n); own[n] = 0;
    if (C->ctx) ext_mark(C->ctx->root, shader, 0, strstr(own, "samplerExternalOES") != 0);
    no_external(own);
    s = own;
    glShaderSource(shader, 1, &s, 0);
}
/* a list of names as the guest sends it (names_of in guest.c) */
static int names_in(const char *b, uint32_t n, const GLchar **out, int max) {
    int k = 0;
    for (uint32_t i = 0; i < n && k < max; k++) { out[k] = b + i; i += (uint32_t)strnlen(b + i, n - i) + 1; }
    return k;
}
static void hd_glTransformFeedbackVaryings(const uint8_t *p, uint32_t len) {
    GLuint program = rd32(&p); GLsizei count = (GLsizei)rd32(&p); GLenum mode = rd32(&p); uint32_t n; const char *b = rdblob(&p, &n);
    const GLchar *v[64];
    (void)len;
    if (count > 64) count = 64;
    int k = b ? names_in(b, n, v, count) : 0;
    glTransformFeedbackVaryings(program, k, k ? v : 0, mode);
}
static void hd_glGetUniformIndices(const uint8_t *p, uint32_t len) {
    GLuint program = rd32(&p); GLsizei count = (GLsizei)rd32(&p); uint32_t n; const char *b = rdblob(&p, &n);
    (void)len;
    const GLchar **v = scratch((uint32_t)count * sizeof(char *));
    GLuint *out = scratch((uint32_t)count * 4);
    for (GLsizei i = 0; i < count; i++) out[i] = GL_INVALID_INDEX;
    int k = b ? names_in(b, n, v, count) : 0;
    if (k == count) glGetUniformIndices(program, count, v, out);
    reply(out, (uint32_t)count * 4);
}
static void hd_glCreateShaderProgramv(const uint8_t *p, uint32_t len) {
    GLenum type = rd32(&p); uint32_t n; const char *s = rdblob(&p, &n);
    char *own = scratch(n + 1);
    (void)len;
    if (s) memcpy(own, s, n);
    own[n] = 0;
    no_external(own);
    const GLchar *src = own;
    reply32(glCreateShaderProgramv(type, 1, &src));
}
static void hd_glTexImage2D(const uint8_t *p, uint32_t len) {
    GLenum target = rd32(&p); GLint level = (GLint)rd32(&p); GLint ifmt = (GLint)rd32(&p); GLsizei w = (GLsizei)rd32(&p); GLsizei h = (GLsizei)rd32(&p);
    GLint border = (GLint)rd32(&p); GLenum format = rd32(&p); GLenum type = rd32(&p);
    (void)len;
    glTexImage2D(xt(target), level, ifmt, w, h, border, format, type, rd_pixels(&p));
}
static void hd_glTexSubImage2D(const uint8_t *p, uint32_t len) {
    GLenum target = rd32(&p); GLint level = (GLint)rd32(&p); GLint x = (GLint)rd32(&p); GLint y = (GLint)rd32(&p); GLsizei w = (GLsizei)rd32(&p);
    GLsizei h = (GLsizei)rd32(&p); GLenum format = rd32(&p); GLenum type = rd32(&p);
    (void)len;
    glTexSubImage2D(xt(target), level, x, y, w, h, format, type, rd_pixels(&p));
    if (target == GL_TEXTURE_2D && C->ctx) { uint64_t id = tb_find(C->ctx, C->ctx->g2d[C->ctx->unit]); if (id) buf_mark(id); }
}
static void hd_glTexImage3D(const uint8_t *p, uint32_t len) {
    GLenum target = rd32(&p); GLint level = (GLint)rd32(&p); GLint ifmt = (GLint)rd32(&p); GLsizei w = (GLsizei)rd32(&p); GLsizei h = (GLsizei)rd32(&p);
    GLsizei d = (GLsizei)rd32(&p); GLint border = (GLint)rd32(&p); GLenum format = rd32(&p); GLenum type = rd32(&p);
    (void)len;
    glTexImage3D(target, level, ifmt, w, h, d, border, format, type, rd_pixels(&p));
}
static void hd_glTexSubImage3D(const uint8_t *p, uint32_t len) {
    GLenum target = rd32(&p); GLint level = (GLint)rd32(&p); GLint x = (GLint)rd32(&p); GLint y = (GLint)rd32(&p); GLint z = (GLint)rd32(&p);
    GLsizei w = (GLsizei)rd32(&p); GLsizei h = (GLsizei)rd32(&p); GLsizei d = (GLsizei)rd32(&p); GLenum format = rd32(&p); GLenum type = rd32(&p);
    (void)len;
    glTexSubImage3D(target, level, x, y, z, w, h, d, format, type, rd_pixels(&p));
}
static void hd_glCompressedTexImage2D(const uint8_t *p, uint32_t len) {
    GLenum target = rd32(&p); GLint level = (GLint)rd32(&p); GLenum ifmt = rd32(&p); GLsizei w = (GLsizei)rd32(&p); GLsizei h = (GLsizei)rd32(&p);
    GLint border = (GLint)rd32(&p); GLsizei size = (GLsizei)rd32(&p);
    (void)len;
    glCompressedTexImage2D(target, level, ifmt, w, h, border, size, rd_pixels(&p));
}
static void hd_glCompressedTexSubImage2D(const uint8_t *p, uint32_t len) {
    GLenum target = rd32(&p); GLint level = (GLint)rd32(&p); GLint x = (GLint)rd32(&p); GLint y = (GLint)rd32(&p); GLsizei w = (GLsizei)rd32(&p);
    GLsizei h = (GLsizei)rd32(&p); GLenum format = rd32(&p); GLsizei size = (GLsizei)rd32(&p);
    (void)len;
    glCompressedTexSubImage2D(target, level, x, y, w, h, format, size, rd_pixels(&p));
}
static void hd_glCompressedTexImage3D(const uint8_t *p, uint32_t len) {
    GLenum target = rd32(&p); GLint level = (GLint)rd32(&p); GLenum ifmt = rd32(&p); GLsizei w = (GLsizei)rd32(&p); GLsizei h = (GLsizei)rd32(&p);
    GLsizei d = (GLsizei)rd32(&p); GLint border = (GLint)rd32(&p); GLsizei size = (GLsizei)rd32(&p);
    (void)len;
    glCompressedTexImage3D(target, level, ifmt, w, h, d, border, size, rd_pixels(&p));
}
static void hd_glCompressedTexSubImage3D(const uint8_t *p, uint32_t len) {
    GLenum target = rd32(&p); GLint level = (GLint)rd32(&p); GLint x = (GLint)rd32(&p); GLint y = (GLint)rd32(&p); GLint z = (GLint)rd32(&p);
    GLsizei w = (GLsizei)rd32(&p); GLsizei h = (GLsizei)rd32(&p); GLsizei d = (GLsizei)rd32(&p); GLenum format = rd32(&p); GLsizei size = (GLsizei)rd32(&p);
    (void)len;
    glCompressedTexSubImage3D(target, level, x, y, z, w, h, d, format, size, rd_pixels(&p));
}
static void hd_glReadPixels(const uint8_t *p, uint32_t len) {
    GLint x = (GLint)rd32(&p); GLint y = (GLint)rd32(&p); GLsizei w = (GLsizei)rd32(&p); GLsizei h = (GLsizei)rd32(&p); GLenum format = rd32(&p); GLenum type = rd32(&p);
    uint32_t n = rd32(&p); uint64_t offset = rd64(&p);
    (void)len;
    if (n == NIL) { int m = read_ms(); glReadPixels(x, y, w, h, format, type, (void *)(uintptr_t)offset); read_done(m); return; }
    void *px = scratch(n);
    int m = read_ms();
    glReadPixels(x, y, w, h, format, type, px);
    read_done(m);
    reply(px, n);
}

/* ---- contexts and surfaces ---- */
static struct hctx *ctx_find(uint32_t id) {
    struct hctx *x;
    pthread_mutex_lock(&g_lock);
    for (x = g_ctx; x && x->id != id; x = x->next) ;
    pthread_mutex_unlock(&g_lock);
    return x;
}
static void ctx_destroy(struct hctx *x) {
    pthread_mutex_lock(&g_lock);
    for (struct hctx **pp = &g_ctx; *pp; pp = &(*pp)->next) if (*pp == x) { *pp = x->next; break; }
    pthread_mutex_unlock(&g_lock);
    eglDestroySurface(g_dpy, x->pb);
    eglDestroyContext(g_dpy, x->egl);
    for (int i = 0; i < 16; i++) free(x->last[i].copy);
    free(x->ext); free(x->extv); free(x);
}
static void ms_drop(struct surf *s) {
    if (s->msfbo) { glDeleteFramebuffers(1, &s->msfbo); glDeleteRenderbuffers(1, &s->mscol); glDeleteRenderbuffers(1, &s->msds); }
    s->msfbo = s->mscol = s->msds = 0;
}
static void surf_alloc(struct surf *s, int w, int h) {
    GLint rbo = 0;
    if (w < 1) w = 1;
    if (h < 1) h = 1;
    glGetIntegerv(GL_RENDERBUFFER_BINDING, &rbo);
    /* The window's colour is a renderbuffer, not a texture: programs bind texture names they never generated
     * (MIUI's camera binds its SurfaceTexture as 1), and a texture here could be one of them - the program's
     * glTexImage2D would then resize and overwrite the window itself. */
    if (!s->fbo) { glGenFramebuffers(1, &s->fbo); glGenRenderbuffers(1, &s->col); glGenRenderbuffers(1, &s->rbo); }
    glBindRenderbuffer(GL_RENDERBUFFER, s->col);
    glRenderbufferStorage(GL_RENDERBUFFER, GL_RGBA8, w, h);
    glBindRenderbuffer(GL_RENDERBUFFER, s->rbo);
    glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, w, h);
    glBindFramebuffer(GL_FRAMEBUFFER, s->fbo);
    glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, s->col);
    glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, s->rbo);
    if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) LOG("a %dx%d window's framebuffer is not complete", w, h);
    if (s->samples > 0) {
        GLint max = 0;
        glGetIntegerv(GL_MAX_SAMPLES, &max);
        int n = s->samples < max ? s->samples : max;
        if (n > 0) {
            if (!s->msfbo) { glGenFramebuffers(1, &s->msfbo); glGenRenderbuffers(1, &s->mscol); glGenRenderbuffers(1, &s->msds); }
            glBindRenderbuffer(GL_RENDERBUFFER, s->mscol);
            glRenderbufferStorageMultisample(GL_RENDERBUFFER, n, GL_RGBA8, w, h);
            glBindRenderbuffer(GL_RENDERBUFFER, s->msds);
            glRenderbufferStorageMultisample(GL_RENDERBUFFER, n, GL_DEPTH24_STENCIL8, w, h);
            glBindFramebuffer(GL_FRAMEBUFFER, s->msfbo);
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, s->mscol);
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, s->msds);
            if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
                LOG("a %dx%d window with %d samples is not complete: single-sampled", w, h, n);
                ms_drop(s);
            }
        }
    }
    s->w = w; s->h = h;
    glBindRenderbuffer(GL_RENDERBUFFER, (GLuint)rbo);
    struct hctx *x = C->ctx;
    glBindFramebuffer(GL_DRAW_FRAMEBUFFER, x->draw_fb ? x->draw_fb : surf_fb(C->surf ? C->surf : s));
    glBindFramebuffer(GL_READ_FRAMEBUFFER, x->read_fb ? x->read_fb : surf_fb(C->surf ? C->surf : s));
}
static void surf_drop(struct surf *s) {
    if (s->fbo) { glDeleteFramebuffers(1, &s->fbo); glDeleteRenderbuffers(1, &s->col); glDeleteRenderbuffers(1, &s->rbo); }
    ms_drop(s);
    memset(s, 0, sizeof(*s));
}
static void release_current(void) {
    struct hctx *x = C->ctx;
    if (!x) return;
    eglMakeCurrent(g_dpy, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    C->ctx = 0; C->surf = 0;
    x->busy = 0;
    if (x->doomed) ctx_destroy(x);
}
static uint32_t make_current(uint32_t id, uint32_t key, int w, int h, int samples) {
    struct hctx *x = ctx_find(id);
    if (!x) return 0;
    if (C->ctx != x) {
        if (x->busy) { LOG("context %u is current on another thread", id); return 0; }
        release_current();
        if (!eglMakeCurrent(g_dpy, x->pb, x->pb, x->egl)) { LOG("eglMakeCurrent: 0x%x", eglGetError()); return 0; }
        x->busy = 1; C->ctx = x;
    }
    C->surf = 0;
    if (key) {
        struct surf *s = 0, *spare = 0;
        for (int i = 0; i < 8; i++) { if (x->surfs[i].key == key) s = &x->surfs[i]; else if (!x->surfs[i].key && !spare) spare = &x->surfs[i]; }
        if (!s) {
            if (!spare) { spare = &x->surfs[0]; surf_drop(spare); }
            s = spare; s->key = key;
        }
        C->surf = s;
        if (!s->fbo || s->w != w || s->h != h || s->samples != samples) {
            if (s->samples != samples) ms_drop(s);
            s->samples = samples;
            surf_alloc(s, w, h);
        }
    }
    if (!x->draw_fb) glBindFramebuffer(GL_DRAW_FRAMEBUFFER, real_fb(0));
    if (!x->read_fb) glBindFramebuffer(GL_READ_FRAMEBUFFER, real_fb(0));
    if (x->fresh && key) { x->fresh = 0; glViewport(0, 0, w, h); glScissor(0, 0, w, h); }
    return 1;
}
/* contexts with a reset notification (GL_LOSE_CONTEXT_ON_RESET), when the phone's EGL can make them: WebGL of an
 * in-process GPU (Chromium's WebView) is allowed only with one. Every context gets the same, as sharing needs. */
static int g_robust = -1;
static uint32_t ctx_create(uint32_t share, int version) {
    static const EGLint ca[] = { EGL_CONTEXT_CLIENT_VERSION, 3, EGL_NONE }, pa[] = { EGL_WIDTH, 1, EGL_HEIGHT, 1, EGL_NONE };
    static const EGLint cr[] = { EGL_CONTEXT_CLIENT_VERSION, 3, 0x3138 /* RESET_NOTIFICATION_STRATEGY_EXT */, 0x31BF /* LOSE_CONTEXT_ON_RESET_EXT */, EGL_NONE };
    struct hctx *sh = share ? ctx_find(share) : 0, *x = calloc(1, sizeof(*x));
    if (!x || (share && !sh)) { free(x); return 0; }
    if (g_robust < 0) { const char *e = eglQueryString(g_dpy, EGL_EXTENSIONS); g_robust = e && strstr(e, "EGL_EXT_create_context_robustness") ? 1 : 0; }
    x->egl = g_robust ? eglCreateContext(g_dpy, g_cfg, sh ? sh->egl : EGL_NO_CONTEXT, cr) : EGL_NO_CONTEXT;
    if (x->egl == EGL_NO_CONTEXT && g_robust && !sh) { g_robust = 0; LOG("no context with a reset notification: 0x%x", eglGetError()); }
    if (x->egl == EGL_NO_CONTEXT) x->egl = eglCreateContext(g_dpy, g_cfg, sh ? sh->egl : EGL_NO_CONTEXT, ca);
    x->pb = x->egl != EGL_NO_CONTEXT ? eglCreatePbufferSurface(g_dpy, g_cfg, pa) : EGL_NO_SURFACE;
    if (x->egl == EGL_NO_CONTEXT || x->pb == EGL_NO_SURFACE) {
        LOG("no context from the phone's driver: 0x%x", eglGetError());
        if (x->egl != EGL_NO_CONTEXT) eglDestroyContext(g_dpy, x->egl);
        free(x);
        return 0;
    }
    x->version = version; x->pid = C->pid; x->fresh = 1;
    pthread_mutex_lock(&g_lock);
    x->id = ++g_ids;
    x->root = sh ? sh->root : x->id;
    x->next = g_ctx; g_ctx = x;
    pthread_mutex_unlock(&g_lock);
    return x->id;
}

/* what the guest may use of the phone's extensions: those whose entry points and enums the bridge carries */
static const char *const kExt[] = {
    "GL_OES_rgb8_rgba8", "GL_OES_depth24", "GL_OES_packed_depth_stencil", "GL_OES_depth_texture", "GL_OES_element_index_uint",
    "GL_OES_texture_npot", "GL_OES_standard_derivatives", "GL_OES_vertex_array_object", "GL_OES_texture_float",
    "GL_OES_texture_half_float", "GL_OES_texture_float_linear", "GL_OES_texture_half_float_linear",
    "GL_EXT_texture_filter_anisotropic", "GL_OES_compressed_ETC1_RGB8_texture", "GL_KHR_texture_compression_astc_ldr",
    "GL_EXT_texture_format_BGRA8888", "GL_EXT_color_buffer_float", "GL_EXT_color_buffer_half_float", "GL_OES_mapbuffer",
    "GL_EXT_map_buffer_range", "GL_EXT_shader_texture_lod", "GL_EXT_frag_depth",
    "GL_EXT_draw_buffers", "GL_EXT_instanced_arrays", "GL_EXT_draw_instanced", "GL_EXT_texture_storage", "GL_EXT_sRGB",
    "GL_OES_texture_3D", "GL_EXT_discard_framebuffer", "GL_EXT_blend_minmax", "GL_OES_vertex_half_float", "GL_EXT_texture_rg",
    "GL_OES_depth_texture_cube_map", "GL_OES_surfaceless_context", "GL_OES_fbo_render_mipmap", "GL_EXT_unpack_subimage",
    "GL_OES_texture_stencil8", "GL_EXT_texture_compression_s3tc", "GL_EXT_texture_compression_dxt1", "GL_OES_vertex_type_10_10_10_2",
    "GL_EXT_texture_type_2_10_10_10_REV", "GL_EXT_read_format_bgra", "GL_OES_stencil8", "GL_EXT_sRGB_write_control",
    "GL_EXT_texture_sRGB_decode", "GL_EXT_shadow_samplers", "GL_OES_texture_cube_map",
    "GL_EXT_robustness", "GL_KHR_robustness",     /* (contexts lose themselves on a reset when the phone can: g_robust) */
};
static void ext_build(struct hctx *x) {
    const char *all = (const char *)glGetString(GL_EXTENSIONS);
    size_t cap = 160;
    for (unsigned i = 0; i < sizeof(kExt) / sizeof(kExt[0]); i++) cap += strlen(kExt[i]) + 1;
    x->ext = calloc(1, cap);
    x->extv = calloc(sizeof(kExt) / sizeof(kExt[0]) + 5, sizeof(char *));
    for (unsigned i = 0; all && i < sizeof(kExt) / sizeof(kExt[0]); i++) {
        size_t n = strlen(kExt[i]);
        for (const char *f = strstr(all, kExt[i]); f; f = strstr(f + 1, kExt[i]))
            if ((f == all || f[-1] == ' ') && (f[n] == ' ' || !f[n])) { strcat(x->ext, kExt[i]); strcat(x->ext, " "); x->extv[x->extn++] = (char *)kExt[i]; break; }
    }
    static const char *const ours[] = { "GL_OES_EGL_image", "GL_OES_EGL_image_external", "GL_OES_EGL_image_external_essl3" };
    for (int i = 0; i < 3; i++) { strcat(x->ext, ours[i]); if (i < 2) strcat(x->ext, " "); x->extv[x->extn++] = (char *)ours[i]; }
    /* One name more by index than in the string. The guest's libEGL answers glGetStringi itself, from the string
     * with a name of its own put in front (GL_EXT_debug_marker), but leaves the count to the driver: with the
     * string's count a program that lists the extensions one by one (Skia) never reads the last of them. The
     * extra name is that very one, for a libEGL that passes the call through. */
    x->extv[x->extn++] = "GL_EXT_debug_marker";
}
static void get_string(GLenum name, uint32_t index) {
    struct hctx *x = C->ctx;
    char buf[256];
    const char *s = 0;
    if (x && !x->ext) ext_build(x);
    if (!x) s = 0;
    else if (index != NIL) s = name == GL_EXTENSIONS && (int)index < x->extn ? x->extv[index] : 0;
    else if (name == GL_EXTENSIONS) s = x->ext;
    else if (name == GL_VERSION) {
        const char *h = (const char *)glGetString(GL_VERSION);
        /* (the version of the context behind, whatever was asked for, as the phones' drivers answer: a program
         * that looks up its entry points once, by the first context, finds the same set in every other) */
        snprintf(buf, sizeof(buf), "OpenGL ES 3.%d (%.200s)", g_minor, h ? h : "");
        s = buf;
    } else if (name == GL_SHADING_LANGUAGE_VERSION) s = g_minor ? "OpenGL ES GLSL ES 3.10" : "OpenGL ES GLSL ES 3.00";
    else s = (const char *)glGetString(name);
    if (!s) { reply32(NIL); return; }
    uint32_t n = (uint32_t)strlen(s), pad = (4 - (n & 3)) & 3;
    reply32(n); reply(s, n); reply("\0\0\0", pad);
}

static void swap(int w, int h) {
    struct hctx *x = C->ctx;
    struct surf *s = C->surf;
    size_t need = (size_t)(w > 0 ? w : 0) * (size_t)(h > 0 ? h : 0) * 4;
    if (C->px_cap < need) { free(C->px); C->px = malloc(need); C->px_cap = C->px ? need : 0; }
    if (!C->px) { C->dead = 1; return; }
    if (!x || !s || s->w != w || s->h != h) memset(C->px, 0, need);
    else {
        GLint align = 4, row = 0, rows = 0, pix = 0, pbo = 0;
        glGetIntegerv(GL_PACK_ALIGNMENT, &align); glGetIntegerv(GL_PACK_ROW_LENGTH, &row);
        glGetIntegerv(GL_PACK_SKIP_ROWS, &rows); glGetIntegerv(GL_PACK_SKIP_PIXELS, &pix);
        glGetIntegerv(GL_PIXEL_PACK_BUFFER_BINDING, &pbo);
        if (pbo) glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
        if (row) glPixelStorei(GL_PACK_ROW_LENGTH, 0);
        if (rows) glPixelStorei(GL_PACK_SKIP_ROWS, 0);
        if (pix) glPixelStorei(GL_PACK_SKIP_PIXELS, 0);
        if (align != 4) glPixelStorei(GL_PACK_ALIGNMENT, 4);
        resolve(s);
        glBindFramebuffer(GL_READ_FRAMEBUFFER, s->fbo);
        glReadPixels(0, 0, w, h, GL_RGBA, GL_UNSIGNED_BYTE, C->px);
        glBindFramebuffer(GL_READ_FRAMEBUFFER, real_fb(x->read_fb));
        if (align != 4) glPixelStorei(GL_PACK_ALIGNMENT, align);
        if (pix) glPixelStorei(GL_PACK_SKIP_PIXELS, pix);
        if (rows) glPixelStorei(GL_PACK_SKIP_ROWS, rows);
        if (row) glPixelStorei(GL_PACK_ROW_LENGTH, row);
        if (pbo) glBindBuffer(GL_PIXEL_PACK_BUFFER, (GLuint)pbo);
    }
    reply_flush();
    wr_all(C->px, need);
    C->frames++;
}

/* The finished frame of a window whose buffer has a number: copied, the other way up, into the host's buffer of
 * that number, where the compositor's context finds it; the guest's memory gets it only if something reads it. */
static void swap_buf(int w, int h, uint64_t id, uint64_t gen, uint64_t frame) {
    struct hctx *x = C->ctx;
    struct surf *s = C->surf;
    struct hbuf *b = x && s && s->w == w && s->h == h ? buf_get(id, w, h, 1) : 0;
    if (b) {
        int k = -1, old = 0;
        for (int i = 0; i < 6; i++) {
            if (x->blit[i].id == id && x->blit[i].serial == b->serial) { k = i; break; }
            if (x->blit[i].serial < x->blit[old].serial) old = i;
        }
        if (k < 0) {
            k = old;
            if (x->blit[k].tex) { glDeleteFramebuffers(1, &x->blit[k].fbo); glDeleteTextures(1, &x->blit[k].tex); }
            glGenTextures(1, &x->blit[k].tex); glGenFramebuffers(1, &x->blit[k].fbo);
            glBindTexture(GL_TEXTURE_2D, x->blit[k].tex);
            glEGLImageTargetTexture2DOES(GL_TEXTURE_2D, b->img);
            glBindTexture(GL_TEXTURE_2D, x->is_ext[x->unit] ? x->gext[x->unit] : x->g2d[x->unit]);
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, x->blit[k].fbo);
            glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, x->blit[k].tex, 0);
            x->blit[k].id = id; x->blit[k].serial = b->serial;
        }
        resolve(s);
        GLboolean scissor = glIsEnabled(GL_SCISSOR_TEST);
        if (scissor) glDisable(GL_SCISSOR_TEST);
        glBindFramebuffer(GL_READ_FRAMEBUFFER, s->fbo);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, x->blit[k].fbo);
        glBlitFramebuffer(0, 0, w, h, 0, h, w, 0, GL_COLOR_BUFFER_BIT, GL_NEAREST);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, real_fb(x->draw_fb));
        glBindFramebuffer(GL_READ_FRAMEBUFFER, real_fb(x->read_fb));
        if (scissor) glEnable(GL_SCISSOR_TEST);
        EGLSyncKHR done = eglCreateSyncKHR(g_dpy, EGL_SYNC_FENCE_KHR, 0);
        glFlush();
        pthread_mutex_lock(&g_lock);
        if (b->id == id) {
            EGLSyncKHR was = b->sync;
            b->sync = done; done = was;
            b->gen = gen; b->gpu = 1;
            if (frame > b->frame) b->frame = frame;
        }
        pthread_cond_broadcast(&g_cond);
        pthread_mutex_unlock(&g_lock);
        if (done != EGL_NO_SYNC_KHR) eglDestroySyncKHR(g_dpy, done);
    } else {
        /* (no buffer for it: the reader must not wait for this frame) */
        pthread_mutex_lock(&g_lock);
        for (int i = 0; i < 512; i++) if (g_buf[i].id == id && frame > g_buf[i].frame) g_buf[i].frame = frame;
        pthread_cond_broadcast(&g_cond);
        pthread_mutex_unlock(&g_lock);
    }
    C->frames++;
}
/* The guest's gralloc is about to hand a buffer's memory to a program: the pixels, if the host's are newer.
 * A frame on its way here (sent on another connection) is waited for. */
static void buf_read(uint64_t id, uint64_t frame, int w, int h) {
    struct hbuf *b = 0;
    struct timespec until;
    size_t need = (size_t)(w > 0 ? w : 0) * (size_t)(h > 0 ? h : 0) * 4;
    int ok = 0;
    clock_gettime(CLOCK_REALTIME, &until);
    until.tv_nsec += 250 * 1000000L;
    if (until.tv_nsec >= 1000000000L) { until.tv_sec++; until.tv_nsec -= 1000000000L; }
    if (C->px_cap < need) { free(C->px); C->px = malloc(need); C->px_cap = C->px ? need : 0; }
    pthread_mutex_lock(&g_lock);
    for (;;) {
        b = 0;
        for (int i = 0; i < 512; i++) if (g_buf[i].id == id) { b = &g_buf[i]; break; }
        if (b && b->frame >= frame) break;
        if (pthread_cond_timedwait(&g_cond, &g_lock, &until) != 0) break;
    }
    if (b && b->gpu && b->w == w && b->h == h && C->px) {
        AHardwareBuffer_Desc d;
        uint8_t *va = 0;
        if (b->sync != EGL_NO_SYNC_KHR) eglClientWaitSyncKHR(g_dpy, b->sync, 0, 200 * 1000000LL);
        AHardwareBuffer_describe(b->ahb, &d);
        if (AHardwareBuffer_lock(b->ahb, AHARDWAREBUFFER_USAGE_CPU_READ_RARELY, -1, 0, (void **)&va) == 0 && va) {
            for (int y = 0; y < h; y++) memcpy(C->px + (size_t)y * w * 4, va + (size_t)y * d.stride * 4, (size_t)w * 4);
            AHardwareBuffer_unlock(b->ahb, 0);
            b->gpu = 0;
            ok = 1;
        }
    }
    pthread_mutex_unlock(&g_lock);
    reply32((uint32_t)ok);
    if (ok) { reply_flush(); wr_all(C->px, need); }
}

/* ---- the screen ----
 * The guest's composer (native/hwchal) names the buffer SurfaceFlinger composed the frame into; that buffer is a
 * hardware buffer here, and it is drawn into the application's view by the GPU: the pixels never reach memory.
 * -> 0 when the buffer is not known here (the composer shows it the old way, through the guest's memory). */
static void present_drop(void) {
    if (C->pctx == EGL_NO_CONTEXT) return;
    eglMakeCurrent(g_dpy, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    if (C->psurf != EGL_NO_SURFACE) eglDestroySurface(g_dpy, C->psurf);
    eglDestroyContext(g_dpy, C->pctx);
    C->pctx = EGL_NO_CONTEXT; C->psurf = EGL_NO_SURFACE; C->pprog = 0;
    memset(C->ptex, 0, sizeof(C->ptex));
}
static GLuint present_program(void) {
    static const char *vs = "attribute vec2 p; varying vec2 t; void main() { t = vec2(p.x * 0.5 + 0.5, 0.5 - p.y * 0.5); gl_Position = vec4(p, 0.0, 1.0); }";
    static const char *fs = "precision mediump float; uniform sampler2D s; varying vec2 t; void main() { gl_FragColor = vec4(texture2D(s, t).rgb, 1.0); }";
    GLuint v = glCreateShader(GL_VERTEX_SHADER), f = glCreateShader(GL_FRAGMENT_SHADER), pr = glCreateProgram();
    glShaderSource(v, 1, &vs, 0); glCompileShader(v);
    glShaderSource(f, 1, &fs, 0); glCompileShader(f);
    glAttachShader(pr, v); glAttachShader(pr, f);
    glBindAttribLocation(pr, 0, "p");
    glLinkProgram(pr);
    glUseProgram(pr);
    return pr;
}
static uint32_t present(uint64_t id, int w, int h) {
    static const EGLint ca[] = { EGL_CONTEXT_CLIENT_VERSION, 3, EGL_NONE };
    static const GLfloat quad[] = { -1, -1, 1, -1, -1, 1, 1, 1 };
    uint32_t known = 0;
    if (C->ctx) return 0;       /* (a drawing thread: not the composer) */
    pthread_mutex_lock(&g_win_lock);
    if (C->pctx == EGL_NO_CONTEXT) C->pctx = eglCreateContext(g_dpy, g_cfg, EGL_NO_CONTEXT, ca);
    if (C->pctx != EGL_NO_CONTEXT && (C->pgen != g_win_gen || C->psurf == EGL_NO_SURFACE)) {
        int fresh = C->pgen != g_win_gen;
        eglMakeCurrent(g_dpy, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        if (C->psurf != EGL_NO_SURFACE) eglDestroySurface(g_dpy, C->psurf);
        C->psurf = g_win && fresh ? eglCreateWindowSurface(g_dpy, g_cfg, g_win, 0) : EGL_NO_SURFACE;
        if (g_win && fresh && C->psurf == EGL_NO_SURFACE) LOG("no window surface for the screen: 0x%x", eglGetError());
        C->pgen = g_win_gen;
        if (C->psurf != EGL_NO_SURFACE) {
            if (eglMakeCurrent(g_dpy, C->psurf, C->psurf, C->pctx)) eglSwapInterval(g_dpy, 0);     /* the guest keeps its own pace */
            else { eglDestroySurface(g_dpy, C->psurf); C->psurf = EGL_NO_SURFACE; }
        }
    }
    /* (with no view to show it in, the frame is still taken: the guest is in the background) */
    int live = C->pctx != EGL_NO_CONTEXT && C->psurf != EGL_NO_SURFACE;
    if (C->pctx != EGL_NO_CONTEXT && !live && eglGetCurrentContext() != C->pctx) eglMakeCurrent(g_dpy, EGL_NO_SURFACE, EGL_NO_SURFACE, C->pctx);
    if (C->pctx != EGL_NO_CONTEXT && eglGetCurrentContext() == C->pctx) {
        static unsigned long use;
        GLuint tex = 0;
        int k = 0;
        if (!C->pprog) C->pprog = present_program();
        pthread_mutex_lock(&g_lock);
        for (int i = 0; i < 512; i++) if (g_buf[i].id == id && g_buf[i].w == w && g_buf[i].h == h) {
            struct hbuf *b = &g_buf[i];
            for (int j = 0; j < 4; j++) {
                if (C->ptex[j].id == id && C->ptex[j].serial == b->serial) { k = j; tex = C->ptex[j].tex; break; }
                if (C->ptex[j].use < C->ptex[k].use) k = j;
            }
            if (!tex) {
                if (!C->ptex[k].tex) glGenTextures(1, &C->ptex[k].tex);
                tex = C->ptex[k].tex;
                glBindTexture(GL_TEXTURE_2D, tex);
                glEGLImageTargetTexture2DOES(GL_TEXTURE_2D, b->img);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
                C->ptex[k].id = id; C->ptex[k].serial = b->serial;
            }
            C->ptex[k].use = ++use;
            b->use = ++g_use;
            known = 1;
            break;
        }
        pthread_mutex_unlock(&g_lock);
        if (known && live) {
            EGLint sw = 0, sh = 0;
            eglQuerySurface(g_dpy, C->psurf, EGL_WIDTH, &sw); eglQuerySurface(g_dpy, C->psurf, EGL_HEIGHT, &sh);
            glViewport(0, 0, sw, sh);
            glBindTexture(GL_TEXTURE_2D, tex);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0, 2, GL_FLOAT, GL_FALSE, 0, quad);
            glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
            if (!eglSwapBuffers(g_dpy, C->psurf)) {         /* the view is gone under us: a new one comes with its number */
                eglMakeCurrent(g_dpy, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
                eglDestroySurface(g_dpy, C->psurf); C->psurf = EGL_NO_SURFACE;
            }
        }
        if (known) g_presented++;
    }
    pthread_mutex_unlock(&g_win_lock);
    return known;
}


#define PRING (16L << 20)
#define PSEG (PRING / 8)
static void (*buffer_storage)(GLenum, GLsizeiptr, const void *, GLbitfield);
/* Room for n bytes of a draw: the offset in the buffer now bound to GL_ARRAY_BUFFER and where to write them;
 * *upload 1: the bytes are handed over with glBufferData once written (the plain way). */
static long ring_take(struct hctx *x, long n, uint8_t **to, int *upload) {
    static int has = -1;
    static __thread uint8_t *stage; static __thread long stage_cap;
    if (has < 0) {
        const char *e = (const char *)glGetString(GL_EXTENSIONS);
        buffer_storage = e && strstr(e, "GL_EXT_buffer_storage") ? (void *)eglGetProcAddress("glBufferStorageEXT") : 0;
        has = buffer_storage != 0;
    }
    x->ring_gen++;
    if (has && !x->pring && !x->pmap) {
        glGenBuffers(1, &x->pring);
        glBindBuffer(GL_ARRAY_BUFFER, x->pring);
        buffer_storage(GL_ARRAY_BUFFER, PRING, 0, GL_MAP_WRITE_BIT | 0x40 /* PERSISTENT */ | 0x80 /* COHERENT */);
        x->pmap = glMapBufferRange(GL_ARRAY_BUFFER, 0, PRING, GL_MAP_WRITE_BIT | 0x40 | 0x80);
        if (!x->pmap) { LOG("no persistent map of a stream buffer: 0x%x", glGetError()); glDeleteBuffers(1, &x->pring); x->pring = 0; x->pmap = (uint8_t *)1; }
    }
    if (x->pring && n <= PSEG) {
        long at = x->poff;
        if (at / PSEG != (at + n - 1) / PSEG || at + n > PRING) {     /* into the next eighth: done with this one */
            int cur = (int)(at / PSEG), next = (cur + 1) % 8;
            if (x->pfence[cur]) glDeleteSync(x->pfence[cur]);
            x->pfence[cur] = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
            at = (long)next * PSEG;
            if (x->pfence[next]) { glClientWaitSync(x->pfence[next], GL_SYNC_FLUSH_COMMANDS_BIT, 1000000000ull); glDeleteSync(x->pfence[next]); x->pfence[next] = 0; }
        }
        x->poff = at + ((n + 63) & ~63L);
        glBindBuffer(GL_ARRAY_BUFFER, x->pring);
        *to = x->pmap + at; *upload = 0;
        return at;
    }
    if (stage_cap < n) { free(stage); stage = malloc((size_t)n); stage_cap = stage ? n : 0; }
    if (!x->ring) glGenBuffers(1, &x->ring);
    glBindBuffer(GL_ARRAY_BUFFER, x->ring);
    *to = stage; *upload = 1;
    return stage ? 0 : -1;
}
static void draw_client(const uint8_t *p) {
    struct hctx *x = C->ctx;
    GLenum mode = rd32(&p); GLsizei count = (GLsizei)rd32(&p); GLenum type = rd32(&p);
    int instanced = (int)rd32(&p); GLsizei instances = (GLsizei)rd32(&p);
    uint32_t nattr = rd32(&p), ilen;
    const void *indices = rdblob(&p, &ilen);
    uint64_t ioff = indices ? 0 : rd64(&p);
    GLint array = 0;
    if (!x) return;
    struct { GLuint index; GLint size; GLenum type; uint32_t flags; GLsizei stride; const void *data; uint32_t n; long at; int reuse; } a[16];
    int na = 0;
    long total = indices ? ((ilen + 63) & ~63L) : 0;
    for (uint32_t i = 0; i < nattr; i++) {
        GLuint index = rd32(&p); GLint size = (GLint)rd32(&p); GLenum at = rd32(&p); uint32_t flags = rd32(&p); GLsizei stride = (GLsizei)rd32(&p);
        uint32_t div = rd32(&p), n;
        const void *data = rdblob(&p, &n);
        (void)div;
        if (trace_on) {
            uint32_t v[8] = { 0 };
            if (data) memcpy(v, data, n < sizeof(v) ? n : sizeof(v));
            __android_log_print(ANDROID_LOG_DEBUG, "aemu-gltrace", "  attr %u size %d type %x flags %u stride %d n %u%s %08x %08x %08x %08x %08x %08x %08x %08x",
                index, size, at, flags, stride, n, data ? "" : " (same)", v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7]);
        }
        if (index >= 16) continue;
        if (data) {     /* (kept: the guest sends nothing when the same bytes come again) */
            if (x->last[index].n < n || !x->last[index].copy) { free(x->last[index].copy); x->last[index].copy = malloc(n ? n : 1); }
            if (x->last[index].copy) memcpy(x->last[index].copy, data, n);
            x->last[index].n = x->last[index].copy ? n : 0;
            x->last[index].gen = 0;
        } else n = x->last[index].n;       /* (decided once the room is taken: the buffer may start anew) */
        a[na].reuse = !data; a[na].at = -1;
        a[na].index = index; a[na].size = size; a[na].type = at; a[na].flags = flags; a[na].stride = stride; a[na].data = data; a[na].n = n;
        total += (n + 63) & ~63L;
        na++;
    }
    glGetIntegerv(GL_ARRAY_BUFFER_BINDING, &array);
    uint8_t *m = 0; int upload = 0;
    long base = ring_take(x, total, &m, &upload);
    for (int i = 0; i < na; i++) if (a[i].reuse) {
        if (x->last[a[i].index].gen == x->ring_gen + 1) a[i].at = x->last[a[i].index].off;      /* still in the stream buffer */
        else a[i].data = x->last[a[i].index].copy;
    }
    if (base < 0) m = 0;
    long o = 0, iat = -1;
    if (indices) { iat = base + o; if (m) memcpy(m + o, indices, ilen); else if (base >= 0) glBufferSubData(GL_ARRAY_BUFFER, iat, ilen, indices); o += (ilen + 63) & ~63L; }
    for (int i = 0; i < na; i++) {
        if (!a[i].data) continue;
        a[i].at = base >= 0 ? base + o : -1;
        if (m) memcpy(m + o, a[i].data, a[i].n); else if (base >= 0) glBufferSubData(GL_ARRAY_BUFFER, a[i].at, a[i].n, a[i].data);
        o += (a[i].n + 63) & ~63L;
        x->last[a[i].index].off = a[i].at; x->last[a[i].index].gen = x->ring_gen + 1;
    }
    if (m && total && upload) glBufferData(GL_ARRAY_BUFFER, total, m, GL_STREAM_DRAW);
    if (base < 0) LOG("a draw with %ld bytes of vertices: too many", total);
    for (int i = 0; i < na; i++) {
        const void *off = (const void *)(uintptr_t)(a[i].at > 0 ? a[i].at : 0);
        if (a[i].flags & 2) glVertexAttribIPointer(a[i].index, a[i].size, a[i].type, a[i].stride, off);
        else glVertexAttribPointer(a[i].index, a[i].size, a[i].type, (GLboolean)(a[i].flags & 1), a[i].stride, off);
    }
    glBindBuffer(GL_ARRAY_BUFFER, (GLuint)array);
    fix_units();
    if (!type) {
        if (instanced) glDrawArraysInstanced(mode, 0, count, instances); else glDrawArrays(mode, 0, count);
        return;
    }
    const void *ip = (const void *)(uintptr_t)ioff;
    if (indices) { glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, upload ? x->ring : x->pring); ip = (const void *)(uintptr_t)(iat > 0 ? iat : 0); }
    if (instanced) glDrawElementsInstanced(mode, count, type, ip, instances); else glDrawElements(mode, count, type, ip);
    if (indices) glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
}

static void hd_other(unsigned op, const uint8_t *p, uint32_t len) {
    (void)len;
    switch (op) {
    case OP_HELLO: reply32(rd32(&p) == AEMU_GL_PROTOCOL ? AEMU_GL_PROTOCOL : 0); break;
    case OP_CTX_CREATE: { uint32_t share = rd32(&p); reply32(ctx_create(share, (int)rd32(&p))); break; }
    case OP_CTX_DESTROY: {
        struct hctx *x = ctx_find(rd32(&p));
        if (x && x->busy) x->doomed = 1; else if (x) ctx_destroy(x);
        break;
    }
    case OP_MAKE_CURRENT: {
        uint32_t id = rd32(&p), key = rd32(&p); int w = (int)rd32(&p), h = (int)rd32(&p), samples = len >= 20 ? (int)rd32(&p) : 0;
        reply32(make_current(id, key, w, h, samples));
        break;
    }
    case OP_RELEASE: release_current(); reply32(1); break;
    case OP_SURF_SIZE: { int w = (int)rd32(&p), h = (int)rd32(&p); if (C->ctx && C->surf) surf_alloc(C->surf, w, h); break; }
    case OP_SURF_DESTROY: {
        uint32_t key = rd32(&p);
        struct hctx *x = C->ctx;
        for (int i = 0; x && i < 8; i++) if (x->surfs[i].key == key) {
            if (C->surf == &x->surfs[i]) C->surf = 0;
            surf_drop(&x->surfs[i]);
        }
        break;
    }
    case OP_SWAP: { int w = (int)rd32(&p), h = (int)rd32(&p); swap(w, h); break; }
    case OP_DRAW_CLIENT: draw_client(p); break;
    case OP_MAP_READ: {
        GLenum target = rd32(&p); uint64_t off = rd64(&p); uint32_t n = rd32(&p);
        void *out = scratch(n), *m = C->ctx ? glMapBufferRange(target, (GLintptr)off, (GLsizeiptr)n, GL_MAP_READ_BIT) : 0;
        if (m) { memcpy(out, m, n); glUnmapBuffer(target); }
        reply(out, n);
        break;
    }
    case OP_MAP_WRITE: {
        GLenum target = rd32(&p); uint64_t off = rd64(&p); uint32_t n; const void *d = rdblob(&p, &n);
        if (C->ctx && d) glBufferSubData(target, (GLintptr)off, (GLsizeiptr)n, d);
        break;
    }
    case OP_GET_UNIFORM: {
        uint32_t kind = rd32(&p), v[16], n = 0; GLuint program = rd32(&p); GLint loc = (GLint)rd32(&p);
        for (int i = 0; i < 16; i++) v[i] = 0x7fc0ffe0u + (uint32_t)i;
        if (C->ctx) {
            if (kind == 0) glGetUniformfv(program, loc, (GLfloat *)v);
            else if (kind == 1) glGetUniformiv(program, loc, (GLint *)v);
            else glGetUniformuiv(program, loc, v);
        }
        for (int i = 0; i < 16; i++) if (v[i] != 0x7fc0ffe0u + (uint32_t)i) n = (uint32_t)i + 1;
        reply32(n); reply(v, sizeof(v));
        break;
    }
    case OP_GET_STRING: { GLenum name = rd32(&p); get_string(name, rd32(&p)); break; }
    case OP_IMAGE_TEX: {
        GLenum target = rd32(&p); GLsizei w = (GLsizei)rd32(&p), h = (GLsizei)rd32(&p); uint32_t n; const void *d = rdblob(&p, &n);
        GLint row = 0, pbo = 0;
        if (!C->ctx || (target != GL_TEXTURE_2D && target != TEX_EXT) || !d) break;
        xt(target);
        glGetIntegerv(GL_UNPACK_ROW_LENGTH, &row); glGetIntegerv(GL_PIXEL_UNPACK_BUFFER_BINDING, &pbo);
        if (pbo) glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
        if (row) glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, d);
        if (row) glPixelStorei(GL_UNPACK_ROW_LENGTH, row);
        if (pbo) glBindBuffer(GL_PIXEL_UNPACK_BUFFER, (GLuint)pbo);
        break;
    }
    case OP_PING: glFinish(); reply32(1); break;
    case OP_PRESENT: { uint64_t id = rd64(&p); int w = (int)rd32(&p), h = (int)rd32(&p); reply32(present(id, w, h)); break; }
    case OP_SWAP_BUF: {
        int w = (int)rd32(&p), h = (int)rd32(&p); uint64_t id = rd64(&p), gen = rd64(&p), frame = rd64(&p);
        swap_buf(w, h, id, gen, frame);
        break;
    }
    case OP_BUF_READ: {
        uint64_t id = rd64(&p), frame = rd64(&p); int w = (int)rd32(&p), h = (int)rd32(&p);
        buf_read(id, frame, w, h);
        break;
    }
    case OP_IMAGE_BIND: {
        GLenum target = rd32(&p); uint64_t id = rd64(&p), gen = rd64(&p); int w = (int)rd32(&p), h = (int)rd32(&p);
        struct hbuf *b = C->ctx && (target == GL_TEXTURE_2D || target == TEX_EXT) ? buf_get(id, w, h, 1) : 0;
        if (!b) { reply32(0); break; }
        xt(target);
        glEGLImageTargetTexture2DOES(GL_TEXTURE_2D, b->img);
        if (target == GL_TEXTURE_2D) tb_set(C->ctx, C->ctx->g2d[C->ctx->unit], id);
        if (!b->frame) b->frame = 1;
        reply32(b->gen != gen);
        break;
    }
    case OP_IMAGE_CREATE: {
        GLuint tex = rd32(&p); EGLint level = (EGLint)rd32(&p), at[] = { EGL_GL_TEXTURE_LEVEL_KHR, level, EGL_NONE };
        EGLImageKHR img = C->ctx ? eglCreateImageKHR(g_dpy, C->ctx->egl, EGL_GL_TEXTURE_2D_KHR, (EGLClientBuffer)(uintptr_t)tex, at) : EGL_NO_IMAGE_KHR;
        uint32_t id = 0;
        if (img == EGL_NO_IMAGE_KHR) LOG("no image of texture %u: 0x%x", tex, eglGetError());
        else {
            pthread_mutex_lock(&g_lock);
            for (int i = 0; i < 256; i++) if (!g_himg[i].id) { id = g_himg[i].id = ++g_himg_ids; g_himg[i].img = img; break; }
            pthread_mutex_unlock(&g_lock);
            if (!id) eglDestroyImageKHR(g_dpy, img);
        }
        reply32(id);
        break;
    }
    case OP_IMAGE_TARGET: {
        GLenum target = rd32(&p); uint32_t id = rd32(&p);
        EGLImageKHR img = EGL_NO_IMAGE_KHR;
        pthread_mutex_lock(&g_lock);
        for (int i = 0; i < 256; i++) if (g_himg[i].id == id) { img = g_himg[i].img; break; }
        pthread_mutex_unlock(&g_lock);
        if (C->ctx && img != EGL_NO_IMAGE_KHR && (target == GL_TEXTURE_2D || target == TEX_EXT)) { xt(target); glEGLImageTargetTexture2DOES(GL_TEXTURE_2D, img); }
        break;
    }
    case OP_IMAGE_DESTROY: {
        uint32_t id = rd32(&p);
        EGLImageKHR img = EGL_NO_IMAGE_KHR;
        pthread_mutex_lock(&g_lock);
        for (int i = 0; id && i < 256; i++) if (g_himg[i].id == id) { img = g_himg[i].img; g_himg[i].id = 0; break; }
        pthread_mutex_unlock(&g_lock);
        if (img != EGL_NO_IMAGE_KHR) eglDestroyImageKHR(g_dpy, img);
        break;
    }
    case OP_IMAGE_DATA: {
        uint64_t id = rd64(&p), gen = rd64(&p); int w = (int)rd32(&p), h = (int)rd32(&p); uint32_t stride = rd32(&p), format = rd32(&p), n;
        const uint8_t *d = rdblob(&p, &n);
        struct hbuf *b = d ? buf_get(id, w, h, 0) : 0;
        if (b) { buf_fill(b, w, h, stride, format, d, n); b->gen = gen; }
        break;
    }
    default:
        if (C->unknown++ < 10) LOG("a call the host does not know: %u", op);
    }
}

#include "h_api.h"

/* ---- connections ---- */
static int need(size_t n) {
    while (C->in_b - C->in_a < n) {
        if (C->in_a && (C->in_a == C->in_b || C->in_cap - C->in_a < n)) {
            memmove(C->in, C->in + C->in_a, C->in_b - C->in_a);
            C->in_b -= C->in_a; C->in_a = 0;
        }
        if (C->in_cap < n) {
            size_t cap = n + 65536;
            uint8_t *b = realloc(C->in, cap);
            if (!b) return 0;
            C->in = b; C->in_cap = cap;
        }
        ssize_t k = read(C->fd, C->in + C->in_b, C->in_cap - C->in_b);
        if (k <= 0) { if (k < 0 && errno == EINTR) continue; return 0; }
        C->in_b += (size_t)k;
    }
    return 1;
}
static void pid_count(int pid, int d) {
    int left = -1;
    pthread_mutex_lock(&g_lock);
    for (int i = 0; i < 64; i++) if (g_pids[i].n && g_pids[i].pid == pid) { left = g_pids[i].n += d; break; }
    if (left < 0 && d > 0) for (int i = 0; i < 64; i++) if (!g_pids[i].n) { g_pids[i].pid = pid; g_pids[i].n = 1; break; }
    pthread_mutex_unlock(&g_lock);
    if (left) return;
    /* the program's last drawing thread is gone: its contexts with it */
    for (;;) {
        struct hctx *x;
        pthread_mutex_lock(&g_lock);
        for (x = g_ctx; x && (x->pid != pid || x->busy); x = x->next) ;
        pthread_mutex_unlock(&g_lock);
        if (!x) break;
        ctx_destroy(x);
    }
}
static void *serve(void *arg) {
    struct conn c;
    memset(&c, 0, sizeof(c));
    c.fd = (int)(intptr_t)arg;
    C = &c;
    struct ucred cr; socklen_t cl = sizeof(cr);
    c.pid = getsockopt(c.fd, SOL_SOCKET, SO_PEERCRED, &cr, &cl) == 0 ? cr.pid : 0;
    int big = 4 << 20;
    setsockopt(c.fd, SOL_SOCKET, SO_SNDBUF, &big, sizeof(big));
    setsockopt(c.fd, SOL_SOCKET, SO_RCVBUF, &big, sizeof(big));
    c.in_cap = 512 * 1024; c.in = malloc(c.in_cap);
    pid_count(c.pid, 1);
    while (c.in && !c.dead && need(8)) {
        uint32_t op, len;
        memcpy(&op, c.in + c.in_a, 4); memcpy(&len, c.in + c.in_a + 4, 4);
        if (len > (256u << 20) || !need(8 + (size_t)len)) break;
        if (op < 0x4000 && !c.ctx) { if (c.unknown++ < 10) LOG("GL call %u with no context", op); }
        else {
            /* debugging: with files/gltrace present every call goes to the log (op, length, first words, the pid) */
            static unsigned trace_n = 0;
            if ((trace_n++ & 4095) == 0) trace_on = access("/data/data/app.aemu/files/gltrace", F_OK) == 0;
            if (trace_on) {
                uint32_t w[6] = { 0 };
                memcpy(w, c.in + c.in_a + 8, len < sizeof(w) ? len : sizeof(w));
                uint32_t nz = 0; const uint8_t *q = c.in + c.in_a + 8;
                for (uint32_t k = 0; k < len; k++) nz += q[k] != 0;
                __android_log_print(ANDROID_LOG_DEBUG, "aemu-gltrace", "%d %u %u %x %x %x %x %x %x nz=%u", c.pid, op, len, w[0], w[1], w[2], w[3], w[4], w[5], nz);
            }
            dispatch(op, c.in + c.in_a + 8, len);
            if (trace_on && c.ctx) {
                GLenum e = glGetError();
                if (e) __android_log_print(ANDROID_LOG_DEBUG, "aemu-gltrace", "  error 0x%x after op %u", e, op);
            }
        }
        c.in_a += 8 + (size_t)len;
        while (c.nscr) free(c.scr[--c.nscr]);
        reply_flush();
    }
    if (c.frames) LOG("a drawing thread of pid %d is gone after %lu frames", c.pid, c.frames);
    release_current();
    present_drop();
    eglReleaseThread();
    pid_count(c.pid, -1);
    close(c.fd);
    free(c.in); free(c.rep); free(c.px);
    return 0;
}
static void *acceptor(void *arg) {
    int ls = (int)(intptr_t)arg;
    for (;;) {
        int fd = accept4(ls, 0, 0, SOCK_CLOEXEC);
        if (fd < 0) { if (errno == EINTR) continue; break; }
        pthread_t t;
        pthread_attr_t a;
        pthread_attr_init(&a);
        pthread_attr_setdetachstate(&a, PTHREAD_CREATE_DETACHED);
        if (pthread_create(&t, &a, serve, (void *)(intptr_t)fd) != 0) close(fd);
        pthread_attr_destroy(&a);
    }
    return 0;
}

JNIEXPORT jstring JNICALL Java_app_aemu_core_GlHost_nativeStart(JNIEnv *env, jclass cls, jstring jpath) {
    static const EGLint want[] = { EGL_RENDERABLE_TYPE, 0x40 /* EGL_OPENGL_ES3_BIT */, EGL_SURFACE_TYPE, EGL_PBUFFER_BIT | EGL_WINDOW_BIT,
                                   EGL_RED_SIZE, 8, EGL_GREEN_SIZE, 8, EGL_BLUE_SIZE, 8, EGL_ALPHA_SIZE, 8, EGL_NONE };
    (void)cls;
    if (!g_dpy) {
        EGLint n = 0;
        EGLDisplay d = eglGetDisplay(EGL_DEFAULT_DISPLAY);
        if (d == EGL_NO_DISPLAY || !eglInitialize(d, 0, 0)) return (*env)->NewStringUTF(env, "no EGL display");
        if (!eglChooseConfig(d, want, &g_cfg, 1, &n) || n < 1) return (*env)->NewStringUTF(env, "the phone's GPU has no GLES 3 config");
        g_dpy = d;
    }
    const char *path = (*env)->GetStringUTFChars(env, jpath, 0);
    struct sockaddr_un a = { .sun_family = AF_UNIX };
    strncpy(a.sun_path, path, sizeof(a.sun_path) - 1);
    (*env)->ReleaseStringUTFChars(env, jpath, path);
    if (g_listen >= 0) { shutdown(g_listen, SHUT_RDWR); close(g_listen); g_listen = -1; }
    pthread_mutex_lock(&g_lock);
    for (int i = 0; i < 512; i++) if (g_buf[i].id) buf_drop(&g_buf[i]);     /* (a new guest numbers its buffers anew) */
    g_presented = 0;
    pthread_mutex_unlock(&g_lock);
    unlink(a.sun_path);
    int ls = socket(AF_UNIX, SOCK_STREAM | SOCK_CLOEXEC, 0);
    pthread_t t;
    if (ls < 0 || bind(ls, (struct sockaddr *)&a, sizeof(a)) < 0 || listen(ls, 32) < 0 || pthread_create(&t, 0, acceptor, (void *)(intptr_t)ls) != 0) {
        if (ls >= 0) close(ls);
        return (*env)->NewStringUTF(env, strerror(errno));
    }
    pthread_detach(t);
    g_listen = ls;
    /* what the renderer is: asked in a context of its own */
    static char info[200];
    if (!info[0]) {
        static const EGLint ca[] = { EGL_CONTEXT_CLIENT_VERSION, 3, EGL_NONE }, pa[] = { EGL_WIDTH, 1, EGL_HEIGHT, 1, EGL_NONE };
        EGLContext x = eglCreateContext(g_dpy, g_cfg, EGL_NO_CONTEXT, ca);
        EGLSurface s = eglCreatePbufferSurface(g_dpy, g_cfg, pa);
        if (x != EGL_NO_CONTEXT && eglMakeCurrent(g_dpy, s, s, x)) {
            GLint major = 0, minor = 0;
            glGetIntegerv(GL_MAJOR_VERSION, &major); glGetIntegerv(GL_MINOR_VERSION, &minor);
            g_minor = major > 3 || (major == 3 && minor >= 1) ? 1 : 0;
            /* (GuestVm reads "ES 3.<n> to the guest") */
            snprintf(info, sizeof(info), "ok %s, %s; ES 3.%d to the guest", (const char *)glGetString(GL_RENDERER), (const char *)glGetString(GL_VERSION), g_minor);
            eglMakeCurrent(g_dpy, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        } else snprintf(info, sizeof(info), "no GLES 3 context: 0x%x", eglGetError());
        if (s != EGL_NO_SURFACE) eglDestroySurface(g_dpy, s);
        if (x != EGL_NO_CONTEXT) eglDestroyContext(g_dpy, x);
        eglReleaseThread();
    }
    return (*env)->NewStringUTF(env, info);
}
JNIEXPORT void JNICALL Java_app_aemu_core_GlHost_nativeSurface(JNIEnv *env, jclass cls, jobject surface) {
    (void)cls;
    ANativeWindow *w = surface ? ANativeWindow_fromSurface(env, surface) : 0;
    pthread_mutex_lock(&g_win_lock);
    if (g_win) ANativeWindow_release(g_win);
    g_win = w; g_win_gen++;
    pthread_mutex_unlock(&g_win_lock);
}
JNIEXPORT jlong JNICALL Java_app_aemu_core_GlHost_nativePresented(JNIEnv *env, jclass cls) {
    (void)env; (void)cls;
    return (jlong)g_presented;
}
JNIEXPORT void JNICALL Java_app_aemu_core_GlHost_nativeStop(JNIEnv *env, jclass cls) {
    (void)env; (void)cls;
    if (g_listen >= 0) { shutdown(g_listen, SHUT_RDWR); close(g_listen); g_listen = -1; }
}
