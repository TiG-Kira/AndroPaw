/*
 * libGLES_aemu.so - the EGL/GLES driver of the guest that draws on the phone's GPU.
 *
 * A program's GLES 2/3 context becomes a context of the host application (host.c): every GL call is written into
 * a command stream, one unix socket per drawing thread, and played there by a thread that has the real context
 * current. eglSwapBuffers brings the finished frame back into the window's buffer, which goes on to the guest's
 * compositor as before.
 *
 * Everything else stays on SwiftShader, which this driver loads and stands in front of: EGL displays and configs
 * are SwiftShader's own, GLES 1 contexts, the compositor's and the window toolkit's contexts are created there and
 * their calls are passed straight through. Which side a context goes to is decided when it is created (policy()).
 * Each thread has a table of entry points: SwiftShader's GLES 2 or GLES 1 library, or the encoders.
 *
 * Built for every bionic from 5.0 on: libc, libdl and liblog only; libnativewindow is looked up at run time.
 */
#include <dlfcn.h>
#include <errno.h>
#include <poll.h>
#include <pthread.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <sys/prctl.h>
#include <sys/socket.h>
#include <sys/uio.h>
#include <sys/un.h>
#include <unistd.h>
#include <fcntl.h>
#include "common.h"
#include "../grallocwrap/note.h"

extern int __android_log_print(int prio, const char *tag, const char *fmt, ...);
extern int __system_property_get(const char *name, char *value);
#define LOG(...) __android_log_print(4, "aemu-gl", __VA_ARGS__)
#define EXPORT __attribute__((visibility("default")))
#define BLOB(n) ((n) == NIL ? 0u : (((n) + 3u) & ~3u))

struct ts;
static __thread void *const *t_tbl;
static __thread struct ts *t_ts;
static void *const *default_tbl(void);
#define TBL() (__builtin_expect(t_tbl != 0, 1) ? t_tbl : default_tbl())
#define TS() (t_ts)

/* ---- what the driver remembers of a bridged context's GL state ---- */
struct attr {
    int on, size, type, norm, integer, stride, div; unsigned buf; const void *ptr;
    const void *sent; uint32_t sent_n; uint64_t sent_sum;     /* what the host's copy of this array holds */
};
struct map { unsigned target, access; long offset, length; void *ptr; };
struct gstate {
    unsigned array, elem, vao, pack_buf, unpack_buf;
    int p_align, p_row, p_skip_rows, p_skip_pix;                     /* GL_PACK_* */
    int u_align, u_row, u_skip_rows, u_skip_pix, u_img_h, u_skip_img;  /* GL_UNPACK_* */
    struct attr attr[16];
    struct map maps[8];
    char *str[5];
    char **ext; int next;
    uint32_t count;         /* how many values the glGet* being sent gives */
    unsigned elem_vao; int elem_vao_known;          /* the element buffer of the bound vertex array, when it was bound here */
    struct { unsigned target, name; } bind[12];     /* the other targets' buffers */
    uint32_t root;          /* the host's id of the first context of the share group (buffers are the group's) */
    /* What the host's context has, so that only changes are sent: a program that draws thousands of small batches
     * (Skia) binds its buffers, sets its arrays up and enables and disables them around every draw - all of it
     * the same as the last time. Enabling the arrays of vertex array 0 waits for the next draw (host_on). */
    uint8_t host_on[16];
    unsigned host_array, host_elem; int host_elem_ok;
    struct hptr { int ok, size, type, norm, integer, stride; unsigned buf; const void *ptr; } hp[16];
};

/* ---- the stream ---- */
struct ectx;
struct esurf;
struct ts {
    int fd, dead;
    uint8_t *buf;
    uint32_t n, cap;
    struct ectx *ctx;
    struct esurf *draw, *read;
};

static void wr_all(struct ts *T, const void *p, size_t n) {
    const char *c = p;
    while (n && !T->dead) {
        ssize_t k = send(T->fd, c, n, MSG_NOSIGNAL);
        if (k <= 0) { if (k < 0 && errno == EINTR) continue; T->dead = 1; LOG("the host is gone (write, errno %d)", errno); return; }
        c += k; n -= (size_t)k;
    }
}
static void rd_all(struct ts *T, void *p, size_t n) {
    char *c = p;
    while (n && !T->dead) {
        ssize_t k = read(T->fd, c, n);
        if (k <= 0) { if (k < 0 && errno == EINTR) continue; T->dead = 1; LOG("the host is gone (read, errno %d)", errno); break; }
        c += k; n -= (size_t)k;
    }
    if (n) memset(c, 0, n);
}
static void flush(struct ts *T) { if (T->n) { wr_all(T, T->buf, T->n); T->n = 0; } }
static inline void room(struct ts *T, uint32_t n) { if (T->n + n > T->cap) flush(T); }
static inline void put32(struct ts *T, uint32_t v) { room(T, 4); memcpy(T->buf + T->n, &v, 4); T->n += 4; }
static inline void putf(struct ts *T, float v) { room(T, 4); memcpy(T->buf + T->n, &v, 4); T->n += 4; }
static inline void put64(struct ts *T, uint64_t v) { room(T, 8); memcpy(T->buf + T->n, &v, 8); T->n += 8; }
static inline void begin(struct ts *T, uint32_t op, uint32_t fixed, uint32_t total) {
    room(T, 8 + fixed);
    put32(T, op); put32(T, total);
}
static void putraw(struct ts *T, const void *p, uint32_t n) {
    static const char zero[4];
    uint32_t pad = (4 - (n & 3)) & 3;
    if (n + pad <= T->cap - T->n || n + pad <= T->cap / 4) {
        room(T, n + pad);
        memcpy(T->buf + T->n, p, n); memset(T->buf + T->n + n, 0, pad); T->n += n + pad;
    } else { flush(T); wr_all(T, p, n); wr_all(T, zero, pad); }
}
static void putblob(struct ts *T, const void *p, uint32_t n) {
    put32(T, n);
    if (n != NIL) putraw(T, p, n);
}
static inline void rsync(struct ts *T) { flush(T); }
static void get(struct ts *T, void *p, uint32_t n) {
    if (p) { rd_all(T, p, n); return; }
    char t[256];
    while (n) { uint32_t k = n < sizeof(t) ? n : sizeof(t); rd_all(T, t, k); n -= k; }
}
static inline uint32_t get32v(struct ts *T) { uint32_t v = 0; rd_all(T, &v, 4); return v; }
static inline uint64_t get64v(struct ts *T) { uint64_t v = 0; rd_all(T, &v, 8); return v; }

struct ectx {
    int bridged, version, current;
    void *ss;               /* SwiftShader's context */
    uint32_t host;          /* the host's context */
    void *dpy, *cfg;
    struct gstate g;
};
static struct gstate *G(void) { return &t_ts->ctx->g; }
#define aemu_get_count(pname) (G()->count)

#include "g_api.h"

/* ---- copies of index buffers ----
 * A draw with indices in a buffer and vertices in the program's memory needs the largest index to know how many
 * vertices go along. Reading the buffer back from the host makes the phone's GPU finish everything queued (Skia's
 * particles: a stall per draw, 4 frames a second), so the indices written while a buffer is bound as
 * GL_ELEMENT_ARRAY_BUFFER are kept here as well. A copy is dropped as soon as the buffer may change some other way
 * (a write through a target whose buffer is not known here, copies, transform feedback, compute). */
struct shadow { uint32_t root; unsigned name; long size; uint8_t *data; };
static struct shadow g_sh[64];
static pthread_mutex_t g_sh_lock = PTHREAD_MUTEX_INITIALIZER;
#define SHADOW_MAX (4L << 20)
static struct shadow *sh_find(uint32_t root, unsigned name) {
    for (int i = 0; name && i < 64; i++) if (g_sh[i].data && g_sh[i].root == root && g_sh[i].name == name) return &g_sh[i];
    return 0;
}
static void sh_drop(uint32_t root, unsigned name) {
    pthread_mutex_lock(&g_sh_lock);
    struct shadow *x = sh_find(root, name);
    if (x) { free(x->data); memset(x, 0, sizeof(*x)); }
    pthread_mutex_unlock(&g_sh_lock);
}
static void sh_drop_all(uint32_t root) {
    pthread_mutex_lock(&g_sh_lock);
    for (int i = 0; i < 64; i++) if (g_sh[i].data && g_sh[i].root == root) { free(g_sh[i].data); memset(&g_sh[i], 0, sizeof(g_sh[i])); }
    pthread_mutex_unlock(&g_sh_lock);
}
/* the buffer bound to a target; *known 0: not known here */
static unsigned bound_to(struct gstate *g, GLenum target, int *known) {
    *known = 1;
    if (target == 0x8892) return g->array;
    if (target == 0x8893) { if (!g->vao) return g->elem; *known = g->elem_vao_known; return g->elem_vao; }
    for (int i = 0; i < 12; i++) if (g->bind[i].target == target) return g->bind[i].name;
    return 0;
}
static void sh_write(GLenum target, long offset, const void *p, long size, int whole) {
    struct gstate *g = G();
    int known;
    unsigned name = bound_to(g, target, &known);
    if (!known) { sh_drop_all(g->root); return; }
    if (!name) return;
    pthread_mutex_lock(&g_sh_lock);
    struct shadow *x = sh_find(g->root, name);
    if (whole) {
        if (x) { free(x->data); memset(x, 0, sizeof(*x)); }
        if (target == 0x8893 && size > 0 && size <= SHADOW_MAX) {
            for (int i = 0; i < 64 && !x; i++) if (!g_sh[i].data) x = &g_sh[i];
            if (!x) { x = &g_sh[(name * 7u) & 63]; free(x->data); }
            x->data = p ? malloc((size_t)size) : calloc(1, (size_t)size);
            if (x->data) { if (p) memcpy(x->data, p, (size_t)size); x->root = g->root; x->name = name; x->size = size; }
            else memset(x, 0, sizeof(*x));
        }
    } else if (x) {
        if (p && offset >= 0 && size >= 0 && offset + size <= x->size) memcpy(x->data + offset, p, (size_t)size);
        else { free(x->data); memset(x, 0, sizeof(*x)); }
    }
    pthread_mutex_unlock(&g_sh_lock);
}
/* the largest of count indices at offset in the bound element buffer, from the copy; -1: no copy */
static long sh_max(struct gstate *g, long offset, int count, uint32_t isz) {
    long r = -1;
    pthread_mutex_lock(&g_sh_lock);
    struct shadow *x = sh_find(g->root, g->elem);
    if (x && offset >= 0 && offset + (long)count * isz <= x->size) {
        const uint8_t *ix = x->data + offset;
        uint32_t max = 0;
        for (int i = 0; i < count; i++) {
            uint32_t v = isz == 1 ? ix[i] : isz == 2 ? ((const uint16_t *)ix)[i] : ((const uint32_t *)ix)[i];
            if (v > max) max = v;
        }
        r = max;
    }
    pthread_mutex_unlock(&g_sh_lock);
    return r;
}
static void k_glBufferData(GLenum target, GLsizeiptr size, const void *data, GLenum usage) { (void)usage; sh_write(target, 0, data, size, 1); }
static void k_glBufferSubData(GLenum target, GLintptr offset, GLsizeiptr size, const void *data) { sh_write(target, offset, data, size, 0); }
static void k_glCopyBufferSubData(GLenum r, GLenum w, GLintptr ro, GLintptr wo, GLsizeiptr size) {
    struct gstate *g = G();
    int known;
    unsigned name = bound_to(g, w, &known);
    (void)r; (void)ro; (void)wo; (void)size;
    if (!known) sh_drop_all(g->root); else sh_drop(g->root, name);
}
static void bind_note(GLenum target, GLuint b) {
    struct gstate *g = G();
    int spare = -1;
    for (int i = 0; i < 12; i++) {
        if (g->bind[i].target == target) { g->bind[i].name = b; return; }
        if (!g->bind[i].target && spare < 0) spare = i;
    }
    if (spare >= 0) { g->bind[spare].target = target; g->bind[spare].name = b; }
}
static void k_glBindBufferBase(GLenum target, GLuint i, GLuint b) { (void)i; bind_note(target, b); }
static void k_glBindBufferRange(GLenum target, GLuint i, GLuint b, GLintptr o, GLsizeiptr n) { (void)i; (void)o; (void)n; bind_note(target, b); }
/* (the GPU may write buffers from here on) */
static void k_glBeginTransformFeedback(GLenum mode) { (void)mode; sh_drop_all(G()->root); }
static void k_glDispatchCompute(GLuint x, GLuint y, GLuint z) { (void)x; (void)y; (void)z; sh_drop_all(G()->root); }
static void k_glDispatchComputeIndirect(GLintptr o) { (void)o; sh_drop_all(G()->root); }
static void k_glMemoryBarrier(GLbitfield b) { (void)b; sh_drop_all(G()->root); }
static void k_glMemoryBarrierByRegion(GLbitfield b) { (void)b; sh_drop_all(G()->root); }

/* ---- notes taken before a call is sent ---- */
static void k_glBindBuffer(GLenum target, GLuint b) {
    struct gstate *g = G();
    switch (target) {
    case 0x8892: g->array = b; break;
    case 0x8893: if (!g->vao) g->elem = b; break;
    case 0x88EB: g->pack_buf = b; break;
    case 0x88EC: g->unpack_buf = b; break;
    }
    if (target == 0x8893 && g->vao) { g->elem_vao = b; g->elem_vao_known = 1; }
    if (target != 0x8892 && target != 0x8893) bind_note(target, b);
}
static void k_glPixelStorei(GLenum pname, GLint v) {
    struct gstate *g = G();
    switch (pname) {
    case 0x0D05: g->p_align = v; break;
    case 0x0D02: g->p_row = v; break;
    case 0x0D03: g->p_skip_rows = v; break;
    case 0x0D04: g->p_skip_pix = v; break;
    case 0x0CF5: g->u_align = v; break;
    case 0x0CF2: g->u_row = v; break;
    case 0x0CF3: g->u_skip_rows = v; break;
    case 0x0CF4: g->u_skip_pix = v; break;
    case 0x806E: g->u_img_h = v; break;
    case 0x806D: g->u_skip_img = v; break;
    }
}
static void k_glBindVertexArray(GLuint a) { struct gstate *g = G(); g->vao = a; g->elem_vao_known = 0; g->host_elem_ok = 0; }
static void k_glEnableVertexAttribArray(GLuint i) { struct gstate *g = G(); if (!g->vao && i < 16) g->attr[i].on = 1; }
static void k_glDisableVertexAttribArray(GLuint i) { struct gstate *g = G(); if (!g->vao && i < 16) g->attr[i].on = 0; }
static void k_glVertexAttribDivisor(GLuint i, GLuint d) { struct gstate *g = G(); if (!g->vao && i < 16) g->attr[i].div = (int)d; }
static void k_glDeleteBuffers(GLsizei n, const GLuint *b) {
    struct gstate *g = G();
    for (int i = 0; b && i < n; i++) {
        if (!b[i]) continue;
        sh_drop(g->root, b[i]);
        if (g->host_array == b[i]) g->host_array = 0;
        if (g->host_elem == b[i]) g->host_elem_ok = 0;
        for (int k = 0; k < 16; k++) if (g->hp[k].buf == b[i]) g->hp[k].ok = 0;
        if (g->array == b[i]) g->array = 0;
        if (g->elem == b[i]) g->elem = 0;
        if (g->pack_buf == b[i]) g->pack_buf = 0;
        if (g->unpack_buf == b[i]) g->unpack_buf = 0;
    }
}
static void k_glDeleteVertexArrays(GLsizei n, const GLuint *a) {
    struct gstate *g = G();
    for (int i = 0; a && i < n; i++) if (a[i] && g->vao == a[i]) g->vao = 0;
}
static void k_count(GLenum pname) {
    uint32_t name = 0, c = aemu_get_count0(pname, &name);
    if (!c) { GLint v = 0; e_glGetIntegerv(name, &v); c = v > 0 ? (uint32_t)v : 0; }
    G()->count = c;
}
static void k_glGetBooleanv(GLenum pname, GLboolean *d) { (void)d; k_count(pname); }
static void k_glGetFloatv(GLenum pname, GLfloat *d) { (void)d; k_count(pname); }
static void k_glGetIntegerv(GLenum pname, GLint *d) { (void)d; k_count(pname); }
static void k_glGetInteger64v(GLenum pname, GLint64 *d) { (void)d; k_count(pname); }

/* ---- state sent only when it changed ---- */
static void e_glBindBuffer(GLenum target, GLuint b) {
    struct gstate *g = G();
    k_glBindBuffer(target, b);
    if (target == 0x8892) { if (g->host_array == b) return; g->host_array = b; }
    if (target == 0x8893) {
        if (g->vao) g->host_elem_ok = 0;
        else { if (g->host_elem_ok && g->host_elem == b) return; g->host_elem = b; g->host_elem_ok = 1; }
    }
    e0_glBindBuffer(target, b);
}
static void e_glEnableVertexAttribArray(GLuint i) { struct gstate *g = G(); k_glEnableVertexAttribArray(i); if (g->vao || i >= 16) e0_glEnableVertexAttribArray(i); }
static void e_glDisableVertexAttribArray(GLuint i) { struct gstate *g = G(); k_glDisableVertexAttribArray(i); if (g->vao || i >= 16) e0_glDisableVertexAttribArray(i); }
/* before a draw (or a question about the arrays): the arrays of vertex array 0 enabled as the program left them */
static void arrays_sync(void) {
    struct gstate *g = G();
    if (g->vao) return;
    for (int i = 0; i < 16; i++) if (g->host_on[i] != (uint8_t)g->attr[i].on) {
        g->host_on[i] = (uint8_t)g->attr[i].on;
        if (g->attr[i].on) e0_glEnableVertexAttribArray((GLuint)i); else e0_glDisableVertexAttribArray((GLuint)i);
    }
}
static void k_glDrawArraysIndirect(GLenum m, const void *o) { (void)m; (void)o; arrays_sync(); }
static void k_glDrawElementsIndirect(GLenum m, GLenum t, const void *o) { (void)m; (void)t; (void)o; arrays_sync(); }
static void k_glGetVertexAttribfv(GLuint i, GLenum n, GLfloat *v) { (void)i; (void)n; (void)v; arrays_sync(); }
static void k_glGetVertexAttribiv(GLuint i, GLenum n, GLint *v) { (void)i; (void)n; (void)v; arrays_sync(); }
static void k_glGetVertexAttribIiv(GLuint i, GLenum n, GLint *v) { (void)i; (void)n; (void)v; arrays_sync(); }
static void k_glGetVertexAttribIuiv(GLuint i, GLenum n, GLuint *v) { (void)i; (void)n; (void)v; arrays_sync(); }

/* ---- calls encoded by hand ---- */
static void e_glFlush(void) { struct ts *T = TS(); begin(T, IDX_glFlush, 0, 0); flush(T); }
static void e_glFinish(void) { struct ts *T = TS(); begin(T, IDX_glFinish, 0, 0); rsync(T); get32v(T); }

static void e_glVertexAttribPointer(GLuint index, GLint size, GLenum type, GLboolean norm, GLsizei stride, const void *ptr) {
    struct ts *T = TS();
    struct gstate *g = G();
    if (!g->vao && index < 16) {
        struct attr *a = &g->attr[index];
        a->size = size; a->type = (int)type; a->norm = norm; a->integer = 0; a->stride = stride; a->buf = g->array; a->ptr = ptr;
        if (!g->array) return;      /* memory of the program: goes with each draw */
        struct hptr *h = &g->hp[index];
        if (h->ok && h->buf == g->array && h->size == size && h->type == (int)type && h->norm == norm && !h->integer && h->stride == stride && h->ptr == ptr) return;
        h->ok = 1; h->buf = g->array; h->size = size; h->type = (int)type; h->norm = norm; h->integer = 0; h->stride = stride; h->ptr = ptr;
    } else if (index < 16) g->hp[index].ok = 0;
    begin(T, IDX_glVertexAttribPointer, 28, 28);
    put32(T, index); put32(T, (uint32_t)size); put32(T, type); put32(T, norm); put32(T, (uint32_t)stride); put64(T, (uint64_t)(uintptr_t)ptr);
}
static void e_glVertexAttribIPointer(GLuint index, GLint size, GLenum type, GLsizei stride, const void *ptr) {
    struct ts *T = TS();
    struct gstate *g = G();
    if (!g->vao && index < 16) {
        struct attr *a = &g->attr[index];
        a->size = size; a->type = (int)type; a->norm = 0; a->integer = 1; a->stride = stride; a->buf = g->array; a->ptr = ptr;
        if (!g->array) return;
        struct hptr *h = &g->hp[index];
        if (h->ok && h->buf == g->array && h->size == size && h->type == (int)type && h->integer && h->stride == stride && h->ptr == ptr) return;
        h->ok = 1; h->buf = g->array; h->size = size; h->type = (int)type; h->norm = 0; h->integer = 1; h->stride = stride; h->ptr = ptr;
    } else if (index < 16) g->hp[index].ok = 0;
    begin(T, IDX_glVertexAttribIPointer, 24, 24);
    put32(T, index); put32(T, (uint32_t)size); put32(T, type); put32(T, (uint32_t)stride); put64(T, (uint64_t)(uintptr_t)ptr);
}
static void e_glGetVertexAttribPointerv(GLuint index, GLenum pname, void **pointer) {
    struct gstate *g = G();
    (void)pname;
    if (pointer) *pointer = (!g->vao && index < 16) ? (void *)g->attr[index].ptr : 0;
}

static int type_size(int type) {
    switch (type) {
    case 0x1400: case 0x1401: return 1;
    case 0x1402: case 0x1403: case 0x140B: case 0x8D61: return 2;
    }
    return 4;
}
static uint64_t sum_of(const void *p, uint32_t n) {
    const uint8_t *c = p;
    uint64_t h = 0x9e3779b97f4a7c15ull ^ n, w;
    for (; n >= 8; n -= 8, c += 8) { memcpy(&w, c, 8); h = (h ^ w) * 0xff51afd7ed558ccdull; h ^= h >> 29; }
    for (w = 0; n; n--) w = w << 8 | c[n - 1];
    h = (h ^ w) * 0xc4ceb9fe1a85ec53ull;
    return h ^ h >> 32;
}
static int client_arrays(const struct gstate *g) {
    if (g->vao) return 0;
    for (int i = 0; i < 16; i++) if (g->attr[i].on && !g->attr[i].buf && g->attr[i].ptr) return 1;
    return 0;
}
static void map_read(struct ts *T, GLenum target, long offset, long length, void *to) {
    begin(T, OP_MAP_READ, 16, 16);
    put32(T, target); put64(T, (uint64_t)offset); put32(T, (uint32_t)length);
    rsync(T);
    get(T, to, (uint32_t)length);
}
/* A draw that takes vertices (and maybe indices) from the program's memory: they travel with it.
 * type 0: glDrawArrays. Sent as: mode, count, type, instanced, instances, the number of arrays; the indices (a block,
 * or NIL and their offset in the bound buffer); for each array index, size, type, normalized | integer << 1, stride,
 * divisor and its bytes from the first vertex used. The host draws from vertex 0. */
static void e_draw(GLenum mode, GLint first, GLsizei count, GLenum type, const void *indices, int instanced, GLsizei instances) {
    struct ts *T = TS();
    struct gstate *g = G();
    if (count <= 0) return;
    uint32_t isz = type ? (type == 0x1401 ? 1u : type == 0x1403 ? 2u : 4u) : 0, ibytes = isz * (uint32_t)count;
    void *fetched = 0;
    int vfirst = first, vcount = count;
    if (type) {
        const void *ix = indices;
        long known = g->elem ? sh_max(g, (long)(uintptr_t)indices, count, isz) : -1;
        if (g->elem && known < 0) {      /* indices in a buffer, vertices in memory: the range of vertices is in the buffer */
            ix = fetched = malloc(ibytes);
            if (!fetched) return;
            map_read(T, 0x8893, (long)(uintptr_t)indices, (long)ibytes, fetched);
        }
        uint32_t max = known >= 0 ? (uint32_t)known : 0;
        for (int i = 0; known < 0 && i < count; i++) {
            uint32_t v = isz == 1 ? ((const uint8_t *)ix)[i] : isz == 2 ? ((const uint16_t *)ix)[i] : ((const uint32_t *)ix)[i];
            if (v > max) max = v;
        }
        vfirst = 0; vcount = (int)max + 1;
    }
    uint32_t len[16], nattr = 0, total = 24 + (type && !g->elem ? 4 + BLOB(ibytes) : 12);
    uint64_t sum[16];
    int same[16];
    for (int i = 0; i < 16; i++) {
        struct attr *a = &g->attr[i];
        len[i] = 0;
        if (!a->on || a->buf || !a->ptr) continue;
        int esz = (a->type == 0x8D9F || a->type == 0x8368) ? 4 : a->size * type_size(a->type);
        int st = a->stride ? a->stride : esz;
        int n = a->div ? ((instanced && instances > 0 ? instances : 1) + a->div - 1) / a->div : vcount;
        len[i] = (uint32_t)((n - 1) * st + esz);
        /* the same bytes as the last time (a mesh drawn again and again): the host still has them */
        const void *from = (const char *)a->ptr + (a->div ? 0 : (long)vfirst * st);
        sum[i] = sum_of(from, len[i]);
        same[i] = a->sent == from && a->sent_n == len[i] && a->sent_sum == sum[i];
        a->sent = from; a->sent_n = len[i]; a->sent_sum = sum[i];
        total += 28 + (same[i] ? 0 : BLOB(len[i]));
        nattr++;
    }
    arrays_sync();
    begin(T, OP_DRAW_CLIENT, 28, total);
    put32(T, mode); put32(T, (uint32_t)count); put32(T, type); put32(T, (uint32_t)instanced); put32(T, (uint32_t)instances);
    put32(T, nattr);
    if (type && !g->elem) putblob(T, indices, ibytes);
    else { put32(T, NIL); put64(T, (uint64_t)(uintptr_t)indices); }
    for (int i = 0; i < 16; i++) {
        struct attr *a = &g->attr[i];
        if (!len[i]) continue;
        int esz = (a->type == 0x8D9F || a->type == 0x8368) ? 4 : a->size * type_size(a->type);
        int st = a->stride ? a->stride : esz;
        g->hp[i].ok = 0;
        put32(T, (uint32_t)i); put32(T, (uint32_t)a->size); put32(T, (uint32_t)a->type);
        put32(T, (uint32_t)(a->norm | a->integer << 1)); put32(T, (uint32_t)st); put32(T, (uint32_t)a->div);
        if (same[i]) put32(T, NIL);
        else putblob(T, (const char *)a->ptr + (a->div ? 0 : (long)vfirst * st), len[i]);
    }
    free(fetched);
}
static void e_glDrawArrays(GLenum mode, GLint first, GLsizei count) {
    struct ts *T = TS();
    arrays_sync();
    if (client_arrays(G())) { e_draw(mode, first, count, 0, 0, 0, 0); return; }
    begin(T, IDX_glDrawArrays, 12, 12); put32(T, mode); put32(T, (uint32_t)first); put32(T, (uint32_t)count);
}
static void e_glDrawArraysInstanced(GLenum mode, GLint first, GLsizei count, GLsizei n) {
    struct ts *T = TS();
    arrays_sync();
    if (client_arrays(G())) { e_draw(mode, first, count, 0, 0, 1, n); return; }
    begin(T, IDX_glDrawArraysInstanced, 16, 16); put32(T, mode); put32(T, (uint32_t)first); put32(T, (uint32_t)count); put32(T, (uint32_t)n);
}
static void e_glDrawElements(GLenum mode, GLsizei count, GLenum type, const void *indices) {
    struct ts *T = TS();
    struct gstate *g = G();
    if (client_arrays(g) || (!g->vao && !g->elem)) { e_draw(mode, 0, count, type, indices, 0, 0); return; }
    arrays_sync();
    begin(T, IDX_glDrawElements, 20, 20); put32(T, mode); put32(T, (uint32_t)count); put32(T, type); put64(T, (uint64_t)(uintptr_t)indices);
}
static void e_glDrawElementsInstanced(GLenum mode, GLsizei count, GLenum type, const void *indices, GLsizei n) {
    struct ts *T = TS();
    struct gstate *g = G();
    if (client_arrays(g) || (!g->vao && !g->elem)) { e_draw(mode, 0, count, type, indices, 1, n); return; }
    arrays_sync();
    begin(T, IDX_glDrawElementsInstanced, 24, 24);
    put32(T, mode); put32(T, (uint32_t)count); put32(T, type); put64(T, (uint64_t)(uintptr_t)indices); put32(T, (uint32_t)n);
}
static void e_glDrawRangeElements(GLenum mode, GLuint start, GLuint end, GLsizei count, GLenum type, const void *indices) {
    (void)start; (void)end;
    e_glDrawElements(mode, count, type, indices);
}

static void e_glShaderSource(GLuint shader, GLsizei count, const GLchar *const *string, const GLint *length) {
    struct ts *T = TS();
    uint32_t total = 0;
    for (int i = 0; i < count; i++) if (string[i]) total += (length && length[i] >= 0) ? (uint32_t)length[i] : (uint32_t)strlen(string[i]);
    char *all = malloc(total + 1), *o = all;
    if (!all) return;
    for (int i = 0; i < count; i++) if (string[i]) {
        uint32_t n = (length && length[i] >= 0) ? (uint32_t)length[i] : (uint32_t)strlen(string[i]);
        memcpy(o, string[i], n); o += n;
    }
    begin(T, IDX_glShaderSource, 8, 8 + BLOB(total));
    put32(T, shader); putblob(T, all, total);
    free(all);
}

/* a list of names: one block, each name followed by its 0 */
static char *names_of(GLsizei count, const GLchar *const *names, uint32_t *n) {
    uint32_t total = 0;
    for (int i = 0; i < count; i++) total += (uint32_t)strlen(names[i] ? names[i] : "") + 1;
    char *all = malloc(total + 1), *o = all;
    if (!all) return 0;
    for (int i = 0; i < count; i++) { size_t k = strlen(names[i] ? names[i] : "") + 1; memcpy(o, names[i] ? names[i] : "", k); o += k; }
    *n = total;
    return all;
}
static void e_glTransformFeedbackVaryings(GLuint program, GLsizei count, const GLchar *const *varyings, GLenum mode) {
    struct ts *T = TS();
    uint32_t n = 0;
    char *all = count > 0 && varyings ? names_of(count, varyings, &n) : 0;
    begin(T, IDX_glTransformFeedbackVaryings, 16, 16 + BLOB(all ? n : NIL));
    put32(T, program); put32(T, (uint32_t)(all ? count : 0)); put32(T, mode); putblob(T, all, all ? n : NIL);
    free(all);
}
static void e_glGetUniformIndices(GLuint program, GLsizei count, const GLchar *const *names, GLuint *indices) {
    struct ts *T = TS();
    uint32_t n = 0;
    char *all = count > 0 && names ? names_of(count, names, &n) : 0;
    if (!all) return;
    begin(T, IDX_glGetUniformIndices, 12, 12 + BLOB(n));
    put32(T, program); put32(T, (uint32_t)count); putblob(T, all, n);
    free(all);
    rsync(T);
    get(T, indices, (uint32_t)count * 4);
}
static GLuint e_glCreateShaderProgramv(GLenum type, GLsizei count, const GLchar *const *strings) {
    struct ts *T = TS();
    uint32_t total = 0;
    for (int i = 0; strings && i < count; i++) if (strings[i]) total += (uint32_t)strlen(strings[i]);
    char *all = malloc(total + 1), *o = all;
    if (!all) return 0;
    for (int i = 0; strings && i < count; i++) if (strings[i]) { size_t k = strlen(strings[i]); memcpy(o, strings[i], k); o += k; }
    begin(T, IDX_glCreateShaderProgramv, 8, 8 + BLOB(total));
    put32(T, type); putblob(T, all, total);
    free(all);
    rsync(T);
    return get32v(T);
}

static char *host_string(struct ts *T, GLenum name, uint32_t index) {
    begin(T, OP_GET_STRING, 8, 8); put32(T, name); put32(T, index);
    rsync(T);
    uint32_t n = get32v(T);
    if (n == NIL) return 0;
    char *s = malloc(BLOB(n) + 1);
    if (!s) { get(T, 0, BLOB(n)); return 0; }
    get(T, s, BLOB(n)); s[n] = 0;
    return s;
}
static const GLubyte *e_glGetString(GLenum name) {
    struct gstate *g = G();
    int i = name >= 0x1F00 && name <= 0x1F03 ? (int)name - 0x1F00 : name == 0x8B8C ? 4 : -1;
    if (i < 0) return 0;
    if (!g->str[i]) g->str[i] = host_string(TS(), name, NIL);
    return (const GLubyte *)g->str[i];
}
static const GLubyte *e_glGetStringi(GLenum name, GLuint index) {
    struct gstate *g = G();
    if (name != 0x1F03) return 0;
    if (!g->ext) {
        GLint n = 0;
        e_glGetIntegerv(0x821D, &n);
        g->ext = calloc((size_t)(n > 0 ? n : 1), sizeof(char *));
        g->next = g->ext ? n : 0;
    }
    if ((int)index >= g->next) return 0;
    if (!g->ext[index]) g->ext[index] = host_string(TS(), name, index);
    return (const GLubyte *)g->ext[index];
}

/* bytes of w x h x d pixels of a format and type as the pixel store parameters lay them out */
static uint32_t pix_bytes(int w, int h, int d, GLenum format, GLenum type, int align, int row, int skip_rows, int skip_pix, int img_h, int skip_img) {
    int comps;
    switch (format) {
    case 0x1906: case 0x1909: case 0x1903: case 0x8D94: case 0x1902: case 0x1901: comps = 1; break;
    case 0x190A: case 0x8227: case 0x8228: case 0x84F9: comps = 2; break;
    case 0x1907: case 0x8D98: case 0x8C40: comps = 3; break;
    default: comps = 4;
    }
    int bpp;
    switch (type) {
    case 0x1400: case 0x1401: bpp = comps; break;
    case 0x8363: case 0x8033: case 0x8034: bpp = 2; break;
    case 0x1402: case 0x1403: case 0x140B: case 0x8D61: bpp = comps * 2; break;
    case 0x8368: case 0x8C3B: case 0x8C3E: case 0x84FA: bpp = 4; break;
    case 0x8DAD: bpp = 8; break;
    default: bpp = comps * 4;
    }
    if (w <= 0 || h <= 0 || d <= 0) return 0;
    if (align < 1) align = 4;
    long rowlen = row > 0 ? row : w;
    long stride = (rowlen * bpp + align - 1) / align * align;
    long ih = img_h > 0 ? img_h : h;
    long rows = (long)(d - 1 + skip_img) * ih + skip_rows + h;
    return (uint32_t)((rows - 1) * stride + (long)(skip_pix + w) * bpp);
}
static uint32_t unpack_bytes(int w, int h, int d, GLenum format, GLenum type) {
    struct gstate *g = G();
    return pix_bytes(w, h, d, format, type, g->u_align, g->u_row, g->u_skip_rows, g->u_skip_pix, g->u_img_h, g->u_skip_img);
}
/* a pointer to pixels: u32 kind - 0 none, 1 a block follows, 2 a u64 offset into the bound buffer follows */
static uint32_t pixels_len(const void *p, uint32_t n) { return 4 + (G()->unpack_buf ? 8 : p ? 4 + BLOB(n) : 0); }
static void put_pixels(struct ts *T, const void *p, uint32_t n) {
    if (G()->unpack_buf) { put32(T, 2); put64(T, (uint64_t)(uintptr_t)p); }
    else if (p) { put32(T, 1); putblob(T, p, n); }
    else put32(T, 0);
}
static void e_glTexImage2D(GLenum target, GLint level, GLint ifmt, GLsizei w, GLsizei h, GLint border, GLenum format, GLenum type, const void *pixels) {
    struct ts *T = TS();
    uint32_t n = unpack_bytes(w, h, 1, format, type);
    begin(T, IDX_glTexImage2D, 36, 32 + pixels_len(pixels, n));
    put32(T, target); put32(T, (uint32_t)level); put32(T, (uint32_t)ifmt); put32(T, (uint32_t)w); put32(T, (uint32_t)h);
    put32(T, (uint32_t)border); put32(T, format); put32(T, type);
    put_pixels(T, pixels, n);
}
static void e_glTexSubImage2D(GLenum target, GLint level, GLint x, GLint y, GLsizei w, GLsizei h, GLenum format, GLenum type, const void *pixels) {
    struct ts *T = TS();
    uint32_t n = unpack_bytes(w, h, 1, format, type);
    begin(T, IDX_glTexSubImage2D, 36, 32 + pixels_len(pixels, n));
    put32(T, target); put32(T, (uint32_t)level); put32(T, (uint32_t)x); put32(T, (uint32_t)y); put32(T, (uint32_t)w);
    put32(T, (uint32_t)h); put32(T, format); put32(T, type);
    put_pixels(T, pixels, n);
}
static void e_glTexImage3D(GLenum target, GLint level, GLint ifmt, GLsizei w, GLsizei h, GLsizei d, GLint border, GLenum format, GLenum type, const void *pixels) {
    struct ts *T = TS();
    uint32_t n = unpack_bytes(w, h, d, format, type);
    begin(T, IDX_glTexImage3D, 40, 36 + pixels_len(pixels, n));
    put32(T, target); put32(T, (uint32_t)level); put32(T, (uint32_t)ifmt); put32(T, (uint32_t)w); put32(T, (uint32_t)h);
    put32(T, (uint32_t)d); put32(T, (uint32_t)border); put32(T, format); put32(T, type);
    put_pixels(T, pixels, n);
}
static void e_glTexSubImage3D(GLenum target, GLint level, GLint x, GLint y, GLint z, GLsizei w, GLsizei h, GLsizei d, GLenum format, GLenum type, const void *pixels) {
    struct ts *T = TS();
    uint32_t n = unpack_bytes(w, h, d, format, type);
    begin(T, IDX_glTexSubImage3D, 44, 40 + pixels_len(pixels, n));
    put32(T, target); put32(T, (uint32_t)level); put32(T, (uint32_t)x); put32(T, (uint32_t)y); put32(T, (uint32_t)z);
    put32(T, (uint32_t)w); put32(T, (uint32_t)h); put32(T, (uint32_t)d); put32(T, format); put32(T, type);
    put_pixels(T, pixels, n);
}
static void e_glCompressedTexImage2D(GLenum target, GLint level, GLenum ifmt, GLsizei w, GLsizei h, GLint border, GLsizei size, const void *data) {
    struct ts *T = TS();
    uint32_t n = size > 0 ? (uint32_t)size : 0;
    begin(T, IDX_glCompressedTexImage2D, 32, 28 + pixels_len(data, n));
    put32(T, target); put32(T, (uint32_t)level); put32(T, ifmt); put32(T, (uint32_t)w); put32(T, (uint32_t)h);
    put32(T, (uint32_t)border); put32(T, n);
    put_pixels(T, data, n);
}
static void e_glCompressedTexSubImage2D(GLenum target, GLint level, GLint x, GLint y, GLsizei w, GLsizei h, GLenum format, GLsizei size, const void *data) {
    struct ts *T = TS();
    uint32_t n = size > 0 ? (uint32_t)size : 0;
    begin(T, IDX_glCompressedTexSubImage2D, 36, 32 + pixels_len(data, n));
    put32(T, target); put32(T, (uint32_t)level); put32(T, (uint32_t)x); put32(T, (uint32_t)y); put32(T, (uint32_t)w);
    put32(T, (uint32_t)h); put32(T, format); put32(T, n);
    put_pixels(T, data, n);
}
static void e_glCompressedTexImage3D(GLenum target, GLint level, GLenum ifmt, GLsizei w, GLsizei h, GLsizei d, GLint border, GLsizei size, const void *data) {
    struct ts *T = TS();
    uint32_t n = size > 0 ? (uint32_t)size : 0;
    begin(T, IDX_glCompressedTexImage3D, 36, 32 + pixels_len(data, n));
    put32(T, target); put32(T, (uint32_t)level); put32(T, ifmt); put32(T, (uint32_t)w); put32(T, (uint32_t)h);
    put32(T, (uint32_t)d); put32(T, (uint32_t)border); put32(T, n);
    put_pixels(T, data, n);
}
static void e_glCompressedTexSubImage3D(GLenum target, GLint level, GLint x, GLint y, GLint z, GLsizei w, GLsizei h, GLsizei d, GLenum format, GLsizei size, const void *data) {
    struct ts *T = TS();
    uint32_t n = size > 0 ? (uint32_t)size : 0;
    begin(T, IDX_glCompressedTexSubImage3D, 44, 40 + pixels_len(data, n));
    put32(T, target); put32(T, (uint32_t)level); put32(T, (uint32_t)x); put32(T, (uint32_t)y); put32(T, (uint32_t)z);
    put32(T, (uint32_t)w); put32(T, (uint32_t)h); put32(T, (uint32_t)d); put32(T, format); put32(T, n);
    put_pixels(T, data, n);
}
static void e_glReadPixels(GLint x, GLint y, GLsizei w, GLsizei h, GLenum format, GLenum type, void *pixels) {
    struct ts *T = TS();
    struct gstate *g = G();
    uint32_t n = g->pack_buf ? NIL : pix_bytes(w, h, 1, format, type, g->p_align, g->p_row, g->p_skip_rows, g->p_skip_pix, 0, 0);
    begin(T, IDX_glReadPixels, 36, 36);
    put32(T, (uint32_t)x); put32(T, (uint32_t)y); put32(T, (uint32_t)w); put32(T, (uint32_t)h); put32(T, format); put32(T, type);
    put32(T, n); put64(T, (uint64_t)(uintptr_t)pixels);
    if (n == NIL) { sh_drop(g->root, g->pack_buf); return; }      /* into the bound buffer */
    rsync(T);
    get(T, pixels, n);
}

static struct map *map_of(GLenum target, int make) {
    struct gstate *g = G();
    for (int i = 0; i < 8; i++) if (g->maps[i].ptr && g->maps[i].target == target) return &g->maps[i];
    if (make) for (int i = 0; i < 8; i++) if (!g->maps[i].ptr) return &g->maps[i];
    return 0;
}
static void map_write(struct ts *T, GLenum target, long offset, const void *p, long length) {
    if (length <= 0) return;
    sh_write(target, offset, p, length, 0);
    begin(T, OP_MAP_WRITE, 16, 16 + BLOB((uint32_t)length));
    put32(T, target); put64(T, (uint64_t)offset); putblob(T, p, (uint32_t)length);
}
/* A mapped buffer is memory of the guest: read from the host when it is mapped (unless the program gives the old
 * contents up), written back when it is unmapped or flushed. */
static void *e_glMapBufferRange(GLenum target, GLintptr offset, GLsizeiptr length, GLbitfield access) {
    struct ts *T = TS();
    struct map *m = map_of(target, 1);
    if (!m || m->ptr || length <= 0) return 0;
    void *p = malloc((size_t)length);
    if (!p) return 0;
    if ((access & 1) || !(access & (4 | 8))) map_read(T, target, offset, length, p);
    m->target = target; m->access = access; m->offset = offset; m->length = length; m->ptr = p;
    return p;
}
static GLboolean e_glUnmapBuffer(GLenum target) {
    struct ts *T = TS();
    struct map *m = map_of(target, 0);
    if (!m) return 0;
    if ((m->access & 2) && !(m->access & 0x10)) map_write(T, target, m->offset, m->ptr, m->length);
    free(m->ptr); m->ptr = 0;
    return 1;
}
static void e_glFlushMappedBufferRange(GLenum target, GLintptr offset, GLsizeiptr length) {
    struct map *m = map_of(target, 0);
    if (m && offset >= 0 && offset + length <= m->length) map_write(TS(), target, m->offset + offset, (char *)m->ptr + offset, length);
}
static void e_glGetBufferPointerv(GLenum target, GLenum pname, void **params) {
    struct map *m = map_of(target, 0);
    (void)pname;
    if (params) *params = m ? m->ptr : 0;
}
static void *e_glMapBufferOES(GLenum target, GLenum access) {
    GLint size = 0;
    (void)access;
    e_glGetBufferParameteriv(target, 0x8764, &size);
    return e_glMapBufferRange(target, 0, size, 2);
}

static void get_uniform(int kind, GLuint program, GLint location, void *params) {
    struct ts *T = TS();
    uint32_t v[16];
    begin(T, OP_GET_UNIFORM, 12, 12); put32(T, (uint32_t)kind); put32(T, program); put32(T, (uint32_t)location);
    rsync(T);
    uint32_t n = get32v(T);
    get(T, v, sizeof(v));
    if (params && n <= 16) memcpy(params, v, n * 4);
}
static void e_glGetUniformfv(GLuint program, GLint location, GLfloat *params) { get_uniform(0, program, location, params); }
static void e_glGetUniformiv(GLuint program, GLint location, GLint *params) { get_uniform(1, program, location, params); }
/* GL_EXT_robustness / GL_KHR_robustness (offered when the phone's driver has them and the host's contexts lose
 * themselves on a reset): Chromium's WebView enables WebGL only then. A reset is not passed on: no reset seen. */
static GLenum e_glGetGraphicsResetStatusEXT(void) { return 0; }
static GLenum e_glGetGraphicsResetStatusKHR(void) { return 0; }
static void e_glReadnPixelsEXT(GLint x, GLint y, GLsizei w, GLsizei h, GLenum format, GLenum type, GLsizei n, void *data) { (void)n; e_glReadPixels(x, y, w, h, format, type, data); }
static void e_glReadnPixelsKHR(GLint x, GLint y, GLsizei w, GLsizei h, GLenum format, GLenum type, GLsizei n, void *data) { (void)n; e_glReadPixels(x, y, w, h, format, type, data); }
static void e_glGetnUniformfvEXT(GLuint program, GLint location, GLsizei n, GLfloat *params) { (void)n; get_uniform(0, program, location, params); }
static void e_glGetnUniformivEXT(GLuint program, GLint location, GLsizei n, GLint *params) { (void)n; get_uniform(1, program, location, params); }
static void e_glGetnUniformfvKHR(GLuint program, GLint location, GLsizei n, GLfloat *params) { (void)n; get_uniform(0, program, location, params); }
static void e_glGetnUniformivKHR(GLuint program, GLint location, GLsizei n, GLint *params) { (void)n; get_uniform(1, program, location, params); }
static void e_glGetUniformuiv(GLuint program, GLint location, GLuint *params) { get_uniform(2, program, location, params); }

/* ---- the window system's types, as libnativewindow lays them out ---- */
struct native_base { int magic, version; void *reserved[4]; void (*incRef)(struct native_base *); void (*decRef)(struct native_base *); };
struct ANativeWindowBuffer {
    struct native_base common;
    int width, height, stride, format, usage_deprecated;
    uintptr_t layerCount;
    void *reserved[1];
    const void *handle;
    uint64_t usage;
};
struct ANativeWindow {
    struct native_base common;
    uint32_t flags;
    int minSwapInterval, maxSwapInterval;
    float xdpi, ydpi;
    intptr_t oem[4];
    int (*setSwapInterval)(struct ANativeWindow *, int);
    int (*dequeueBuffer_DEPRECATED)(struct ANativeWindow *, struct ANativeWindowBuffer **);
    int (*lockBuffer_DEPRECATED)(struct ANativeWindow *, struct ANativeWindowBuffer *);
    int (*queueBuffer_DEPRECATED)(struct ANativeWindow *, struct ANativeWindowBuffer *);
    int (*query)(const struct ANativeWindow *, int what, int *value);
    int (*perform)(struct ANativeWindow *, int op, ...);
    int (*cancelBuffer_DEPRECATED)(struct ANativeWindow *, struct ANativeWindowBuffer *);
    int (*dequeueBuffer)(struct ANativeWindow *, struct ANativeWindowBuffer **, int *fence);
    int (*queueBuffer)(struct ANativeWindow *, struct ANativeWindowBuffer *, int fence);
    int (*cancelBuffer)(struct ANativeWindow *, struct ANativeWindowBuffer *, int fence);
};
static int (*ahb_lock)(void *b, uint64_t usage, int fence, const void *rect, void **va);
static int (*ahb_unlock)(void *b, int *fence);
static void *(*anwb_ahb)(struct ANativeWindowBuffer *b);
/* before 8.0 there is no AHardwareBuffer API: the gralloc module locks the window buffer itself */
struct gr_module {
    uint32_t tag; uint16_t mver, hver; const char *id, *name, *author; void *methods, *dso;
#ifdef __LP64__
    uint64_t reserved[32 - 7];
#else
    uint32_t reserved[32 - 7];
#endif
    int (*registerBuffer)(const struct gr_module *, const void *);
    int (*unregisterBuffer)(const struct gr_module *, const void *);
    int (*lock)(const struct gr_module *, const void *, int usage, int l, int t, int w, int h, void **vaddr);
    int (*unlock)(const struct gr_module *, const void *);
};
static const struct gr_module *g_gr;
static int buf_api(void) {
    static int state;
    if (!state) {
        void *l = dlopen("libnativewindow.so", RTLD_NOW);
        ahb_lock = l ? dlsym(l, "AHardwareBuffer_lock") : 0;
        ahb_unlock = l ? dlsym(l, "AHardwareBuffer_unlock") : 0;
        anwb_ahb = l ? dlsym(l, "ANativeWindowBuffer_getHardwareBuffer") : 0;
        state = ahb_lock && ahb_unlock ? 1 : -1;
        if (state < 0) {
            void *hw = dlopen("libhardware.so", RTLD_NOW);
            int (*get)(const char *, const void **) = hw ? (int (*)(const char *, const void **))dlsym(hw, "hw_get_module") : 0;
            const void *m = 0;
            if (get && get("gralloc", &m) == 0 && m && ((const struct gr_module *)m)->lock) { g_gr = m; state = 2; }
        }
        if (state < 0) LOG("no AHardwareBuffer_lock nor gralloc module in this release: frames cannot be handed over");
    }
    return state > 0;
}
/* (an AHardwareBuffer is the ANativeWindowBuffer part of the same GraphicBuffer) */
static void *ahb_of(struct ANativeWindowBuffer *b) { return anwb_ahb ? anwb_ahb(b) : (void *)b; }
static void *buf_lock(struct ANativeWindowBuffer *b, int write) {
    void *va = 0;
    if (!buf_api()) return 0;
    if (g_gr) return g_gr->lock(g_gr, b->handle, write ? 0x30 : 0x3, 0, 0, b->width, b->height, &va) == 0 ? va : 0;
    if (ahb_lock(ahb_of(b), write ? 0x30 : 0x3, -1, 0, &va) != 0) return 0;
    return va;
}
static void buf_unlock(struct ANativeWindowBuffer *b) { if (g_gr) g_gr->unlock(g_gr, b->handle); else ahb_unlock(ahb_of(b), 0); }

/* ---- SwiftShader behind ---- */
#ifdef __LP64__
#define LIBDIR "lib64"
#else
#define LIBDIR "lib"
#endif
static struct { void *egl, *v2, *v1; void *t2[N_FUNCS], *t1[N_FUNCS], *enc[N_FUNCS]; volatile int up; } SS;
static pthread_mutex_t g_lock = PTHREAD_MUTEX_INITIALIZER;
static long nothing(void) { return 0; }
static long not_carried(void) {
    static int said;
    if (said < 20) { said++; LOG("a GL call the bridge does not carry yet"); }
    return 0;
}
static void *ss_open(const char *name) {
    static const char *const dirs[] = { "/vendor/" LIBDIR "/egl/", "/system/" LIBDIR "/egl/" };
    for (unsigned i = 0; i < 2; i++) {
        char p[128];
        strcpy(p, dirs[i]); strcat(p, name);
        void *h = dlopen(p, RTLD_NOW | RTLD_LOCAL);
        if (h) return h;
    }
    return 0;
}
static void ss_load(void) {
    pthread_mutex_lock(&g_lock);
    if (!SS.up) {
        SS.egl = ss_open("libEGL_swiftshader.so");
        SS.v2 = ss_open("libGLESv2_swiftshader.so");
        SS.v1 = ss_open("libGLESv1_CM_swiftshader.so");
#ifndef __LP64__
        /* the 32-bit engine (KK) has no SwiftShader: behind this driver stands its own one-library bridge (glsplit) */
        if (!SS.egl || !SS.v2) {
            void *k = ss_open("libGLES_split.so");
            if (k) SS.egl = SS.v2 = SS.v1 = k;
        }
#endif
        if (!SS.egl || !SS.v2) LOG("SwiftShader is not there: %s", dlerror());
        for (int i = 0; i < N_FUNCS; i++) {
            void *f = SS.v2 ? dlsym(SS.v2, kNames[i]) : 0;
            SS.t2[i] = f ? f : (void *)nothing;
            f = SS.v1 ? dlsym(SS.v1, kNames[i]) : 0;
            SS.t1[i] = f ? f : (void *)nothing;
            SS.enc[i] = kEnc[i] ? kEnc[i] : (void *)not_carried;
        }
        SS.up = 1;
    }
    pthread_mutex_unlock(&g_lock);
}
static void *const *default_tbl(void) { if (!SS.up) ss_load(); return SS.t2; }
static void *sse(const char *name) { if (!SS.up) ss_load(); return SS.egl ? dlsym(SS.egl, name) : 0; }

/* ---- EGL ---- */
typedef void *EGLDisplay, *EGLConfig, *EGLSurface, *EGLContext, *EGLImageKHR, *EGLSyncKHR, *EGLClientBuffer;
typedef int32_t EGLint;
typedef unsigned int EGLBoolean, EGLenum;
#define EGL_SUCCESS 0x3000
#define EGL_BAD_ALLOC 0x3003
#define EGL_BAD_CONTEXT 0x3006
#define EGL_BAD_MATCH 0x3009
#define EGL_BAD_SURFACE 0x300D
#define EGL_HEIGHT 0x3056
#define EGL_WIDTH 0x3057
#define EGL_NONE 0x3038
static __thread EGLint t_err;

#define FWD(ret, name, params, args) \
    EXPORT ret name params { static ret (*f) params; if (!f) f = sse(#name); return f ? f args : (ret)0; }
#define SSF(ret, name, params) static ret (*name) params; if (!name) name = sse("egl" #name)

FWD(EGLDisplay, eglGetDisplay, (void *native), (native))
FWD(EGLBoolean, eglInitialize, (EGLDisplay d, EGLint *major, EGLint *minor), (d, major, minor))
FWD(EGLBoolean, eglTerminate, (EGLDisplay d), (d))
FWD(EGLBoolean, eglGetConfigs, (EGLDisplay d, EGLConfig *c, EGLint size, EGLint *n), (d, c, size, n))
FWD(EGLBoolean, eglChooseConfig, (EGLDisplay d, const EGLint *a, EGLConfig *c, EGLint size, EGLint *n), (d, a, c, size, n))
FWD(EGLBoolean, eglGetConfigAttrib, (EGLDisplay d, EGLConfig c, EGLint a, EGLint *v), (d, c, a, v))
/* The extensions get EGL_EXT_create_context_robustness: contexts on the phone's GPU lose themselves on a reset (host.c,
 * g_robust), and ANGLE (Chromium's WebView from 9x on) turns WebGL on only for a context it asked that of. */
EXPORT const char *eglQueryString(EGLDisplay d, EGLint name) {
    static const char *(*f)(EGLDisplay, EGLint);
    static char *ext;
    if (!f) f = sse("eglQueryString");
    const char *s = f ? f(d, name) : 0;
    if (name != 0x3055 /* EGL_EXTENSIONS */ || !s) return s;
    /* and EGL_KHR_fence_sync (the sync objects below): the 4.4 engine's driver leaves it out, and WebView 44 (6.0)
     * creates a fence anyway, through a function it bound only for that extension - a call to null */
    static const char *const add[] = { "EGL_EXT_create_context_robustness", "EGL_KHR_fence_sync" };
    if (!ext) {
        size_t n = strlen(s);
        char *e = malloc(n + 64);
        if (!e) return s;
        memcpy(e, s, n); e[n] = 0;
        for (unsigned i = 0; i < sizeof add / sizeof add[0]; i++) {
            if (strstr(s, add[i])) continue;
            if (n && e[strlen(e) - 1] != ' ') strcat(e, " ");
            strcat(e, add[i]);
        }
        ext = e;
    }
    return ext;
}
FWD(EGLBoolean, eglBindAPI, (EGLenum api), (api))
FWD(EGLenum, eglQueryAPI, (void), ())
FWD(EGLBoolean, eglWaitNative, (EGLint engine), (engine))

enum { S_WINDOW, S_PBUFFER };
struct esurf {
    int kind, w, h, current, doomed, ready;
    void *dpy, *cfg;
    struct ANativeWindow *win;
    struct ANativeWindowBuffer *buf;    /* dequeued for the frame being drawn */
    void *ss; int ss_tried;
    uint32_t key;
    EGLint attribs[32];
    uint8_t *tmp; uint32_t tmp_n;
};
static volatile uint32_t g_keys;

/* (bionic has getprogname from 5.0; before it: the translator shows the guest's own argv in /proc/self/cmdline) */
extern const char *getprogname(void) __attribute__((weak));
static const char *prog_name(void) {
    static char name[128];
    const char *c = getprogname ? getprogname() : 0;
    if (c) return c;
    int fd = open("/proc/self/cmdline", O_RDONLY | O_CLOEXEC);
    if (fd >= 0) { ssize_t n = read(fd, name, sizeof(name) - 1); name[n > 0 ? n : 0] = 0; close(fd); }
    return name;
}

/* ---- which side a new context goes to ---- */
static int policy(int version) {
    static int mode = -1, process;      /* process: 1 a program that may draw on the GPU, 2 one that may not */
    if (mode < 0) {
        char v[96] = "";
        __system_property_get("debug.aemu.gl", v);
        /* off unless asked for: 1 - a program's own contexts, 2 - the window toolkit's as well */
        mode = v[0] >= '0' && v[0] <= '9' ? v[0] - '0' : 0;
    }
    if (!process) {
        /* (the name libc was started with: /proc/self/cmdline is the engine's runner) */
        const char *c = prog_name();
        /* (the compositor draws on the GPU in mode 3) */
        process = strstr(c, "bootanim") || (strstr(c, "surfaceflinger") && mode < 3) ? 2 : 1;
        LOG("program %s: %s", c, process == 2 ? "SwiftShader only" : "GPU contexts allowed");
    }
    if (!mode || version < 2 || process == 2) return 0;
    if (mode >= 2) return 1;
    /* the window toolkit and Chromium hand textures to each other, and their drawing is light: left where it was */
    static const char *const soft[] = { "RenderThread", "hwuiTask", "GrallocUpload", "Chrome_", "CrGpu", "Compositor", "GpuThread", "Viz" };
    char name[17] = "";
    prctl(PR_GET_NAME, name);
    for (unsigned i = 0; i < sizeof(soft) / sizeof(soft[0]); i++) if (!strncmp(name, soft[i], strlen(soft[i]))) return 0;
    return 1;
}

/* ---- the calling thread's connection ---- */
static pthread_key_t g_key;
static pthread_once_t g_key_once = PTHREAD_ONCE_INIT;
static void ts_free(void *p) {
    struct ts *T = p;
    if (T->fd >= 0) close(T->fd);
    free(T->buf); free(T);
}
static void key_make(void) { pthread_key_create(&g_key, ts_free); }
static struct ts *ts_get(void) {
    if (t_ts) return t_ts;
    struct ts *T = calloc(1, sizeof(*T));
    if (!T) return 0;
    T->cap = 256 * 1024;
    T->buf = malloc(T->cap);
    T->fd = socket(AF_UNIX, SOCK_STREAM | SOCK_CLOEXEC, 0);
    struct sockaddr_un a = { .sun_family = AF_UNIX };
    strcpy(a.sun_path, AEMU_GL_SOCKET);
    if (!T->buf || T->fd < 0 || connect(T->fd, (struct sockaddr *)&a, sizeof(a)) < 0) {
        LOG("no host renderer at " AEMU_GL_SOCKET " (errno %d)", errno);
        if (T->fd >= 0) close(T->fd);
        free(T->buf); free(T);
        return 0;
    }
    int big = 4 << 20;
    setsockopt(T->fd, SOL_SOCKET, SO_SNDBUF, &big, sizeof(big));
    setsockopt(T->fd, SOL_SOCKET, SO_RCVBUF, &big, sizeof(big));
    begin(T, OP_HELLO, 4, 4); put32(T, AEMU_GL_PROTOCOL);
    rsync(T);
    if (get32v(T) != AEMU_GL_PROTOCOL) { LOG("the host renderer speaks another protocol"); close(T->fd); free(T->buf); free(T); return 0; }
    pthread_once(&g_key_once, key_make);
    pthread_setspecific(g_key, T);
    t_ts = T;
    return T;
}

/* ---- surfaces ---- */
static int attr_of(const EGLint *a, EGLint name, EGLint def) {
    for (; a && *a != EGL_NONE; a += 2) if (*a == name) return a[1];
    return def;
}
static void copy_attribs(struct esurf *s, const EGLint *a) {
    int n = 0;
    for (; a && *a != EGL_NONE && n < 30; a += 2) { s->attribs[n++] = a[0]; s->attribs[n++] = a[1]; }
    s->attribs[n] = EGL_NONE;
}
EXPORT EGLSurface eglCreateWindowSurface(EGLDisplay d, EGLConfig c, void *win, const EGLint *attribs) {
    struct esurf *s = calloc(1, sizeof(*s));
    if (!s || !win) { free(s); t_err = EGL_BAD_ALLOC; return 0; }
    s->kind = S_WINDOW; s->dpy = d; s->cfg = c; s->win = win;
    s->key = __sync_add_and_fetch(&g_keys, 1);
    copy_attribs(s, attribs);
    s->win->common.incRef(&s->win->common);
    return s;
}
EXPORT EGLSurface eglCreatePbufferSurface(EGLDisplay d, EGLConfig c, const EGLint *attribs) {
    struct esurf *s = calloc(1, sizeof(*s));
    if (!s) { t_err = EGL_BAD_ALLOC; return 0; }
    s->kind = S_PBUFFER; s->dpy = d; s->cfg = c;
    s->w = attr_of(attribs, EGL_WIDTH, 1); s->h = attr_of(attribs, EGL_HEIGHT, 1);
    if (s->w < 1) s->w = 1;
    if (s->h < 1) s->h = 1;
    s->key = __sync_add_and_fetch(&g_keys, 1);
    copy_attribs(s, attribs);
    return s;
}
EXPORT EGLSurface eglCreatePixmapSurface(EGLDisplay d, EGLConfig c, void *pixmap, const EGLint *attribs) {
    (void)d; (void)c; (void)pixmap; (void)attribs;
    t_err = EGL_BAD_MATCH;
    return 0;
}
/* SwiftShader's surface for ours: made when a SwiftShader context first draws to it */
static void *ss_surface(struct esurf *s) {
    if (!s) return 0;
    if (!s->ss && !s->ss_tried) {
        s->ss_tried = 1;
        if (s->kind == S_WINDOW) {
            SSF(EGLSurface, CreateWindowSurface, (EGLDisplay, EGLConfig, void *, const EGLint *));
            if (s->buf) { s->win->cancelBuffer(s->win, s->buf, -1); s->buf = 0; }
            s->ss = CreateWindowSurface ? CreateWindowSurface(s->dpy, s->cfg, s->win, s->attribs) : 0;
        } else {
            SSF(EGLSurface, CreatePbufferSurface, (EGLDisplay, EGLConfig, const EGLint *));
            s->ss = CreatePbufferSurface ? CreatePbufferSurface(s->dpy, s->cfg, s->attribs) : 0;
        }
    }
    return s->ss;
}
static void surf_free(struct esurf *s) {
    if (s->ss) { SSF(EGLBoolean, DestroySurface, (EGLDisplay, EGLSurface)); if (DestroySurface) DestroySurface(s->dpy, s->ss); }
    if (s->win) {
        if (s->buf) s->win->cancelBuffer(s->win, s->buf, -1);
        s->win->common.decRef(&s->win->common);
    }
    free(s->tmp); free(s);
}
static void surf_unref(struct esurf *s) { if (s && --s->current <= 0 && s->doomed) surf_free(s); }
EXPORT EGLBoolean eglDestroySurface(EGLDisplay d, EGLSurface surface) {
    struct esurf *s = surface;
    (void)d;
    if (!s) { t_err = EGL_BAD_SURFACE; return 0; }
    struct ts *T = t_ts;
    if (T && T->ctx && T->ctx->bridged && s->ready) { begin(T, OP_SURF_DESTROY, 4, 4); put32(T, s->key); }
    if (s->current > 0) s->doomed = 1; else surf_free(s);
    return 1;
}
static void win_dequeue(struct esurf *s) {
    int fence = -1;
    if (s->buf) return;
    if (!s->ready) { s->win->perform(s->win, 0 /* NATIVE_WINDOW_SET_USAGE */, 0x933); s->ready = 1; }
    if (s->win->dequeueBuffer(s->win, &s->buf, &fence) != 0) { s->buf = 0; return; }
    if (fence >= 0) { struct pollfd p = { fence, POLLIN, 0 }; poll(&p, 1, 1000); close(fence); }
    s->w = s->buf->width; s->h = s->buf->height;
}
EXPORT EGLBoolean eglQuerySurface(EGLDisplay d, EGLSurface surface, EGLint attr, EGLint *value) {
    struct esurf *s = surface;
    if (!s || !value) { t_err = EGL_BAD_SURFACE; return 0; }
    if (s->ss) { SSF(EGLBoolean, QuerySurface, (EGLDisplay, EGLSurface, EGLint, EGLint *)); return QuerySurface(d, s->ss, attr, value); }
    switch (attr) {
    case EGL_WIDTH: case EGL_HEIGHT:
        if (s->kind == S_WINDOW && !s->buf) { int v = 0; s->win->query(s->win, attr == EGL_WIDTH ? 0 : 1, &v); *value = v; }
        else *value = attr == EGL_WIDTH ? s->w : s->h;
        return 1;
    case 0x3086: *value = 0x3084; return 1;     /* EGL_RENDER_BUFFER: EGL_BACK_BUFFER */
    case 0x3093: *value = 0x3095; return 1;     /* EGL_SWAP_BEHAVIOR: EGL_BUFFER_DESTROYED */
    case 0x3028: return eglGetConfigAttrib(d, s->cfg, 0x3028, value);   /* EGL_CONFIG_ID */
    }
    *value = 0;
    return 1;
}
EXPORT EGLBoolean eglSurfaceAttrib(EGLDisplay d, EGLSurface surface, EGLint attr, EGLint value) {
    struct esurf *s = surface;
    if (s && s->ss) { SSF(EGLBoolean, SurfaceAttrib, (EGLDisplay, EGLSurface, EGLint, EGLint)); return SurfaceAttrib(d, s->ss, attr, value); }
    return 1;
}
EXPORT EGLBoolean eglBindTexImage(EGLDisplay d, EGLSurface s, EGLint b) { (void)d; (void)s; (void)b; t_err = EGL_BAD_SURFACE; return 0; }
EXPORT EGLBoolean eglReleaseTexImage(EGLDisplay d, EGLSurface s, EGLint b) { (void)d; (void)s; (void)b; t_err = EGL_BAD_SURFACE; return 0; }
EXPORT EGLBoolean eglCopyBuffers(EGLDisplay d, EGLSurface s, void *target) { (void)d; (void)s; (void)target; t_err = EGL_BAD_SURFACE; return 0; }

/* ---- contexts ---- */
EXPORT EGLContext eglCreateContext(EGLDisplay d, EGLConfig cfg, EGLContext share, const EGLint *attribs) {
    struct ectx *sh = share, *c = calloc(1, sizeof(*c));
    if (!c) { t_err = EGL_BAD_ALLOC; return 0; }
    c->version = attr_of(attribs, 0x3098 /* EGL_CONTEXT_CLIENT_VERSION */, 1);
    c->dpy = d; c->cfg = cfg;
    c->g.p_align = c->g.u_align = 4;
    if (sh ? sh->bridged : policy(c->version)) {
        struct ts *T = ts_get();
        if (T) {
            begin(T, OP_CTX_CREATE, 8, 8); put32(T, sh ? sh->host : 0); put32(T, (uint32_t)c->version);
            rsync(T);
            c->host = get32v(T);
            c->g.root = sh ? sh->g.root : c->host;
        }
        if (c->host) {
            char name[17] = "";
            prctl(PR_GET_NAME, name);
            c->bridged = 1;
            LOG("a GLES %d context on the phone's GPU (%u), thread %s", c->version, c->host, name);
            return c;
        }
        if (sh) { free(c); t_err = EGL_BAD_ALLOC; return 0; }
        LOG("the host gave no context: SwiftShader");
    }
    SSF(EGLContext, CreateContext, (EGLDisplay, EGLConfig, EGLContext, const EGLint *));
    /* (SwiftShader may not know the robustness attributes the extension list offers: left out for it) */
    EGLint own[64]; int k = 0;
    for (int i = 0; attribs && attribs[i] != EGL_NONE && k < 60; i += 2)
        if (attribs[i] != 0x3138 /* RESET_NOTIFICATION_STRATEGY */ && attribs[i] != 0x30BF /* ROBUST_ACCESS */) { own[k++] = attribs[i]; own[k++] = attribs[i + 1]; }
    own[k] = EGL_NONE;
    c->ss = CreateContext ? CreateContext(d, cfg, sh ? sh->ss : 0, attribs ? own : 0) : 0;
    if (!c->ss) { free(c); return 0; }
    return c;
}
static void ctx_free(struct ectx *c) {
    if (c->bridged) {
        struct ts *T = ts_get();
        if (T) { begin(T, OP_CTX_DESTROY, 4, 4); put32(T, c->host); flush(T); }
        if (c->g.root == c->host) sh_drop_all(c->host);
        for (int i = 0; i < 5; i++) free(c->g.str[i]);
        for (int i = 0; i < c->g.next; i++) free(c->g.ext[i]);
        free(c->g.ext);
        for (int i = 0; i < 8; i++) free(c->g.maps[i].ptr);
    } else { SSF(EGLBoolean, DestroyContext, (EGLDisplay, EGLContext)); if (DestroyContext) DestroyContext(c->dpy, c->ss); }
    free(c);
}
EXPORT EGLBoolean eglDestroyContext(EGLDisplay d, EGLContext context) {
    struct ectx *c = context;
    (void)d;
    if (!c) { t_err = EGL_BAD_CONTEXT; return 0; }
    if (c->current > 0) c->current |= 0x10000;      /* freed when it is released */
    else ctx_free(c);
    return 1;
}
EXPORT EGLBoolean eglQueryContext(EGLDisplay d, EGLContext context, EGLint attr, EGLint *value) {
    struct ectx *c = context;
    if (!c || !value) { t_err = EGL_BAD_CONTEXT; return 0; }
    if (!c->bridged) { SSF(EGLBoolean, QueryContext, (EGLDisplay, EGLContext, EGLint, EGLint *)); return QueryContext(d, c->ss, attr, value); }
    switch (attr) {
    case 0x3098: *value = c->version; return 1;
    case 0x3097: *value = 0x30A0; return 1;     /* EGL_CONTEXT_CLIENT_TYPE: EGL_OPENGL_ES_API */
    case 0x3086: *value = 0x3084; return 1;
    case 0x3028: return eglGetConfigAttrib(d, c->cfg, 0x3028, value);
    }
    *value = 0;
    return 1;
}

static __thread struct ectx *t_ss_ctx;      /* the SwiftShader context current on this thread */
static __thread struct esurf *t_ss_draw, *t_ss_read;
static void ss_release(EGLDisplay d) {
    if (!t_ss_ctx) return;
    SSF(EGLBoolean, MakeCurrent, (EGLDisplay, EGLSurface, EGLSurface, EGLContext));
    if (MakeCurrent) MakeCurrent(d, 0, 0, 0);
    struct ectx *c = t_ss_ctx;
    t_ss_ctx = 0;
    surf_unref(t_ss_draw); if (t_ss_read != t_ss_draw) surf_unref(t_ss_read);
    t_ss_draw = t_ss_read = 0;
    if (--c->current == 0x10000) ctx_free(c);
}
static void bridge_release(void) {
    struct ts *T = t_ts;
    if (!T || !T->ctx) return;
    begin(T, OP_RELEASE, 0, 0);
    rsync(T); get32v(T);
    struct ectx *c = T->ctx;
    T->ctx = 0;
    surf_unref(T->draw); if (T->read != T->draw) surf_unref(T->read);
    T->draw = T->read = 0;
    if (--c->current == 0x10000) ctx_free(c);
}
EXPORT EGLBoolean eglMakeCurrent(EGLDisplay d, EGLSurface draw, EGLSurface read, EGLContext context) {
    struct ectx *c = context;
    struct esurf *sd = draw, *sr = read;
    if (!c) {
        bridge_release(); ss_release(d);
        t_tbl = 0;
        return 1;
    }
    if (c->bridged) {
        struct ts *T = ts_get();
        if (!T) { t_err = EGL_BAD_CONTEXT; return 0; }
        ss_release(d);
        if (T->ctx == c && T->draw == sd && T->read == sr) return 1;
        if (sd && sd->kind == S_WINDOW) {
            if (sd->ss) { t_err = EGL_BAD_MATCH; LOG("a window of a SwiftShader context given to a GPU context"); return 0; }
            win_dequeue(sd);
        }
        struct ectx *old = T->ctx;
        struct esurf *od = T->draw, *orr = T->read;
        EGLint samples = 0;
        if (sd && !eglGetConfigAttrib(d, sd->cfg, 0x3031 /* EGL_SAMPLES */, &samples)) samples = 0;
        begin(T, OP_MAKE_CURRENT, 20, 20);
        put32(T, c->host); put32(T, sd ? sd->key : 0); put32(T, sd ? (uint32_t)sd->w : 0); put32(T, sd ? (uint32_t)sd->h : 0);
        put32(T, (uint32_t)(samples > 0 ? samples : 0));
        rsync(T);
        if (!get32v(T)) { t_err = EGL_BAD_CONTEXT; LOG("the host did not make context %u current", c->host); return 0; }
        c->current++; if (sd) sd->current++; if (sr && sr != sd) sr->current++;
        T->ctx = c; T->draw = sd; T->read = sr;
        if (old) {
            surf_unref(od); if (orr != od) surf_unref(orr);
            if (--old->current == 0x10000) ctx_free(old);
        }
        t_tbl = SS.enc;
        return 1;
    }
    bridge_release();
    SSF(EGLBoolean, MakeCurrent, (EGLDisplay, EGLSurface, EGLSurface, EGLContext));
    void *a = ss_surface(sd), *b = sr == sd ? a : ss_surface(sr);
    if ((sd && !a) || (sr && !b)) { t_err = EGL_BAD_SURFACE; return 0; }
    if (!MakeCurrent || !MakeCurrent(d, a, b, c->ss)) return 0;
    c->current++; if (sd) sd->current++; if (sr && sr != sd) sr->current++;
    struct ectx *old = t_ss_ctx;
    struct esurf *od = t_ss_draw, *orr = t_ss_read;
    t_ss_ctx = c; t_ss_draw = sd; t_ss_read = sr;
    if (old) {
        surf_unref(od); if (orr != od) surf_unref(orr);
        if (--old->current == 0x10000) ctx_free(old);
    }
    t_tbl = c->version == 1 && SS.v1 ? SS.t1 : SS.t2;
    return 1;
}
EXPORT EGLContext eglGetCurrentContext(void) { return t_ts && t_ts->ctx ? (void *)t_ts->ctx : (void *)t_ss_ctx; }
EXPORT EGLSurface eglGetCurrentSurface(EGLint which) {
    if (t_ts && t_ts->ctx) return which == 0x305A ? t_ts->read : t_ts->draw;
    return which == 0x305A ? t_ss_read : t_ss_draw;
}
EXPORT EGLDisplay eglGetCurrentDisplay(void) {
    struct ectx *c = eglGetCurrentContext();
    return c ? c->dpy : 0;
}
EXPORT EGLint eglGetError(void) {
    if (t_err) { EGLint e = t_err; t_err = 0; return e; }
    SSF(EGLint, GetError, (void));
    return GetError ? GetError() : EGL_SUCCESS;
}
EXPORT EGLBoolean eglReleaseThread(void) {
    bridge_release();
    if (t_ss_ctx) ss_release(t_ss_ctx->dpy);
    t_tbl = 0;
    SSF(EGLBoolean, ReleaseThread, (void));
    return ReleaseThread ? ReleaseThread() : 1;
}
EXPORT EGLBoolean eglWaitGL(void) {
    if (t_ts && t_ts->ctx) { e_glFinish(); return 1; }
    SSF(EGLBoolean, WaitGL, (void));
    return WaitGL ? WaitGL() : 1;
}
EXPORT EGLBoolean eglWaitClient(void) { return eglWaitGL(); }

/* The frame of a bridged context: the host reads it back, bottom row first, and it is copied into the window's
 * buffer the other way up. */
static EGLBoolean bridge_swap(struct ts *T, struct esurf *s) {
    if (s->kind != S_WINDOW) return 1;
    win_dequeue(s);
    if (!s->buf) { t_err = EGL_BAD_SURFACE; return 0; }
    struct ANativeWindowBuffer *b = s->buf;
    uint32_t w = (uint32_t)b->width, h = (uint32_t)b->height, need = w * h * 4;
    /* A buffer of the engine's gralloc stays on the host: the frame is copied into the host's buffer of the same
     * number, and the note tells gralloc to fetch it if a program opens the memory (grallocwrap). */
    struct meta_note *note = meta_note_at(b->handle);
    /* (a framebuffer buffer - 6.0's compositor posting to fb0 - is read back: the screen shows fb0's memory) */
    if (note && note->magic == META_NOTE && note->id && !(note->usage & 0x1000 /* GRALLOC_USAGE_HW_FB */) &&
        (b->format == 1 || b->format == 2 || b->format == 4)) {
        uint64_t frame = ++note->pad[1];
        begin(T, OP_SWAP_BUF, 32, 32); put32(T, w); put32(T, h); put64(T, note->id); put64(T, note->pad[0]); put64(T, frame);
        flush(T);
        s->buf = 0;
        s->win->queueBuffer(s->win, b, -1);
        win_dequeue(s);
        if (s->buf && ((uint32_t)s->w != w || (uint32_t)s->h != h)) { begin(T, OP_SURF_SIZE, 8, 8); put32(T, (uint32_t)s->w); put32(T, (uint32_t)s->h); }
        return 1;
    }
    if (s->tmp_n < need) { free(s->tmp); s->tmp = malloc(need); s->tmp_n = s->tmp ? need : 0; }
    begin(T, OP_SWAP, 8, 8); put32(T, w); put32(T, h);
    rsync(T);
    get(T, s->tmp, need);
    uint8_t *va = s->tmp ? buf_lock(b, 1) : 0;
    if (va) {
        if (b->format == 4) {       /* RGB_565 */
            for (uint32_t y = 0; y < h; y++) {
                const uint8_t *src = s->tmp + (size_t)(h - 1 - y) * w * 4;
                uint16_t *dst = (uint16_t *)(va + (size_t)y * (uint32_t)b->stride * 2);
                for (uint32_t x = 0; x < w; x++, src += 4) dst[x] = (uint16_t)((src[0] >> 3) << 11 | (src[1] >> 2) << 5 | src[2] >> 3);
            }
        } else for (uint32_t y = 0; y < h; y++)
            memcpy(va + (size_t)y * (uint32_t)b->stride * 4, s->tmp + (size_t)(h - 1 - y) * w * 4, (size_t)w * 4);
        buf_unlock(b);
    }
    s->buf = 0;
    s->win->queueBuffer(s->win, b, -1);
    win_dequeue(s);
    if (s->buf && ((uint32_t)s->w != w || (uint32_t)s->h != h)) { begin(T, OP_SURF_SIZE, 8, 8); put32(T, (uint32_t)s->w); put32(T, (uint32_t)s->h); }
    return 1;
}
/* A bridged context's window is queued by this driver: the interval goes to the window itself (0: the program does
 * not wait for the display) */
EXPORT EGLBoolean eglSwapInterval(EGLDisplay d, EGLint interval) {
    struct ts *T = t_ts;
    if (T && T->ctx) {
        struct esurf *s = T->draw;
        if (s && s->kind == S_WINDOW && s->win->setSwapInterval) s->win->setSwapInterval(s->win, interval < 0 ? 0 : interval);
        return 1;
    }
    SSF(EGLBoolean, SwapInterval, (EGLDisplay, EGLint));
    return SwapInterval ? SwapInterval(d, interval) : 1;
}
EXPORT EGLBoolean eglSwapBuffers(EGLDisplay d, EGLSurface surface) {
    struct esurf *s = surface;
    if (!s) { t_err = EGL_BAD_SURFACE; return 0; }
    if (t_ts && t_ts->ctx && t_ts->draw == s) return bridge_swap(t_ts, s);
    if (!s->ss) return 1;
    SSF(EGLBoolean, SwapBuffers, (EGLDisplay, EGLSurface));
    return SwapBuffers(d, s->ss);
}
EXPORT EGLBoolean eglSwapBuffersWithDamageKHR(EGLDisplay d, EGLSurface s, EGLint *rects, EGLint n) { (void)rects; (void)n; return eglSwapBuffers(d, s); }
EXPORT EGLBoolean eglSetDamageRegionKHR(EGLDisplay d, EGLSurface s, EGLint *rects, EGLint n) { (void)d; (void)s; (void)rects; (void)n; return 1; }

/* ---- images: SwiftShader's own, and for each the buffer it was made of (a bridged context uploads its pixels) ---- */
static struct { void *img; struct ANativeWindowBuffer *buf; uint32_t host; } g_img[256];     /* host: an image of a bridged context's texture */
EXPORT EGLImageKHR eglCreateImageKHR(EGLDisplay d, EGLContext context, EGLenum target, EGLClientBuffer buffer, const EGLint *attribs) {
    struct ectx *c = context;
    SSF(EGLImageKHR, CreateImageKHR, (EGLDisplay, EGLContext, EGLenum, EGLClientBuffer, const EGLint *));
    if (c && c->bridged) {
        /* a bridged context's texture: the image is the host's, and only bridged contexts can use it */
        struct ts *T = t_ts;
        void *h = 0;
        uint32_t id = 0;
        if (target != 0x30B1 /* EGL_GL_TEXTURE_2D_KHR */ || !T || !T->ctx) { t_err = EGL_BAD_MATCH; return 0; }
        begin(T, OP_IMAGE_CREATE, 8, 8); put32(T, (uint32_t)(uintptr_t)buffer); put32(T, (uint32_t)attr_of(attribs, 0x30BC, 0));
        rsync(T);
        id = get32v(T);
        if (id && (h = malloc(8))) {
            pthread_mutex_lock(&g_lock);
            for (int i = 0; i < 256; i++) if (!g_img[i].img) { g_img[i].img = h; g_img[i].buf = 0; g_img[i].host = id; id = 0; break; }
            pthread_mutex_unlock(&g_lock);
        }
        if (!h || id) { free(h); t_err = EGL_BAD_ALLOC; return 0; }
        return h;
    }
    void *img = CreateImageKHR ? CreateImageKHR(d, c ? c->ss : 0, target, buffer, attribs) : 0;
    if (img && target == 0x3140 /* EGL_NATIVE_BUFFER_ANDROID */) {
        struct ANativeWindowBuffer *b = buffer;
        pthread_mutex_lock(&g_lock);
        for (int i = 0; i < 256; i++) if (!g_img[i].img) { g_img[i].img = img; g_img[i].buf = b; b->common.incRef(&b->common); break; }
        pthread_mutex_unlock(&g_lock);
    }
    return img;
}
EXPORT EGLBoolean eglDestroyImageKHR(EGLDisplay d, EGLImageKHR img) {
    struct ANativeWindowBuffer *b = 0;
    pthread_mutex_lock(&g_lock);
    uint32_t host = 0;
    for (int i = 0; i < 256; i++) if (g_img[i].img == img) { b = g_img[i].buf; host = g_img[i].host; g_img[i].img = 0; g_img[i].buf = 0; g_img[i].host = 0; break; }
    pthread_mutex_unlock(&g_lock);
    if (b) b->common.decRef(&b->common);
    if (host) {
        struct ts *T = ts_get();
        if (T) { begin(T, OP_IMAGE_DESTROY, 4, 4); put32(T, host); }
        free(img);
        return 1;
    }
    SSF(EGLBoolean, DestroyImageKHR, (EGLDisplay, EGLImageKHR));
    return DestroyImageKHR ? DestroyImageKHR(d, img) : 0;
}
static void e_glEGLImageTargetTexture2DOES(GLenum target, GLeglImageOES image) {
    struct ts *T = TS();
    struct ANativeWindowBuffer *b = 0;
    pthread_mutex_lock(&g_lock);
    uint32_t host = 0;
    for (int i = 0; i < 256; i++) if (g_img[i].img == image) { b = g_img[i].buf; host = g_img[i].host; break; }
    pthread_mutex_unlock(&g_lock);
    if (host) { begin(T, OP_IMAGE_TARGET, 8, 8); put32(T, target); put32(T, host); return; }
    if (!b) { not_carried(); return; }
    uint32_t w = (uint32_t)b->width, h = (uint32_t)b->height;
    /* A buffer of the engine's gralloc has a number and a count of writes (grallocwrap/note.h): the host keeps a
     * buffer of its own under that number, shared by every context that binds it, and the pixels go there only
     * when the guest's memory changed - what a GPU context drew into it stays. */
    struct meta_note *note = meta_note_at(b->handle);
    int bpp = b->format == 1 || b->format == 2 ? 4 : b->format == 3 ? 3 : b->format == 4 ? 2 : 0;
    if (note && note->magic == META_NOTE && note->id) {
        uint64_t count = note->pad[0];
        begin(T, OP_IMAGE_BIND, 28, 28);
        put32(T, target); put64(T, note->id); put64(T, count); put32(T, w); put32(T, h);
        rsync(T);
        uint32_t wanted = get32v(T);
        /* (it may be drawn into from now on: gralloc asks the host before the memory is read) */
        if (target == 0x0DE1 && !note->pad[1]) note->pad[1] = 1;
        if (!wanted || !bpp) return;
        uint8_t *px = buf_lock(b, 0);
        if (!px) return;
        uint32_t n = (uint32_t)b->stride * h * (uint32_t)bpp;
        begin(T, OP_IMAGE_DATA, 36, 36 + BLOB(n));
        put64(T, note->id); put64(T, count); put32(T, w); put32(T, h); put32(T, (uint32_t)b->stride); put32(T, (uint32_t)b->format);
        putblob(T, px, n);
        buf_unlock(b);
        return;
    }
    if (!bpp) { not_carried(); return; }
    uint8_t *va = buf_lock(b, 0), *tight = 0;
    if (!va) return;
    if (bpp != 4) {     /* RGB_565 / RGB_888 (a camera preview's window, say): widened to RGBA */
        if (!(tight = malloc((size_t)w * h * 4))) { buf_unlock(b); return; }
        for (uint32_t y = 0; y < h; y++) {
            const uint8_t *s = va + (size_t)y * (uint32_t)b->stride * bpp;
            uint8_t *d = tight + (size_t)y * w * 4;
            for (uint32_t x = 0; x < w; x++, d += 4) {
                if (bpp == 2) { uint16_t v = ((const uint16_t *)s)[x]; d[0] = (uint8_t)((v >> 11) << 3 | v >> 13); d[1] = (uint8_t)((v >> 5 & 63) << 2 | (v >> 9 & 3)); d[2] = (uint8_t)((v & 31) << 3 | (v >> 2 & 7)); }
                else { d[0] = s[x * 3]; d[1] = s[x * 3 + 1]; d[2] = s[x * 3 + 2]; }
                d[3] = 255;
            }
        }
    } else if ((uint32_t)b->stride != w && (tight = malloc((size_t)w * h * 4)))
        for (uint32_t y = 0; y < h; y++) memcpy(tight + (size_t)y * w * 4, va + (size_t)y * (uint32_t)b->stride * 4, (size_t)w * 4);
    if ((uint32_t)b->stride == w || tight) {
        begin(T, OP_IMAGE_TEX, 16, 16 + BLOB(w * h * 4));
        put32(T, target); put32(T, w); put32(T, h); putblob(T, tight ? tight : va, w * h * 4);
    }
    free(tight);
    buf_unlock(b);
}

/* ---- sync objects: SwiftShader's, or for a bridged context one that is signalled once the host caught up ---- */
#define BRIDGE_SYNC ((void *)(uintptr_t)0xAE5C0001)
EXPORT EGLSyncKHR eglCreateSyncKHR(EGLDisplay d, EGLenum type, const EGLint *attribs) {
    if (t_ts && t_ts->ctx) {        /* (what was drawn is finished when this returns) */
        struct ts *T = t_ts;
        begin(T, OP_PING, 0, 0); rsync(T); get32v(T);
        return BRIDGE_SYNC;
    }
    SSF(EGLSyncKHR, CreateSyncKHR, (EGLDisplay, EGLenum, const EGLint *));
    return CreateSyncKHR ? CreateSyncKHR(d, type, attribs) : 0;
}
EXPORT EGLBoolean eglDestroySyncKHR(EGLDisplay d, EGLSyncKHR s) {
    if (s == BRIDGE_SYNC) return 1;
    SSF(EGLBoolean, DestroySyncKHR, (EGLDisplay, EGLSyncKHR));
    return DestroySyncKHR ? DestroySyncKHR(d, s) : 0;
}
EXPORT EGLint eglClientWaitSyncKHR(EGLDisplay d, EGLSyncKHR s, EGLint flags, uint64_t timeout) {
    if (s == BRIDGE_SYNC) {
        struct ts *T = t_ts;
        if (T && T->ctx) { begin(T, OP_PING, 0, 0); rsync(T); get32v(T); }
        return 0x30F6;      /* EGL_CONDITION_SATISFIED_KHR */
    }
    SSF(EGLint, ClientWaitSyncKHR, (EGLDisplay, EGLSyncKHR, EGLint, uint64_t));
    return ClientWaitSyncKHR ? ClientWaitSyncKHR(d, s, flags, timeout) : 0;
}
EXPORT EGLBoolean eglGetSyncAttribKHR(EGLDisplay d, EGLSyncKHR s, EGLint attr, EGLint *value) {
    if (s == BRIDGE_SYNC) {
        if (value) *value = attr == 0x30F1 ? 0x30F2 /* SIGNALED */ : attr == 0x30F7 ? 0x30F9 /* SYNC_FENCE */ : 0x30F0;
        return 1;
    }
    SSF(EGLBoolean, GetSyncAttribKHR, (EGLDisplay, EGLSyncKHR, EGLint, EGLint *));
    return GetSyncAttribKHR ? GetSyncAttribKHR(d, s, attr, value) : 0;
}

EXPORT void *eglGetProcAddress(const char *name) {
    if (!name) return 0;
    if (name[0] == 'g' && name[1] == 'l') {
        for (int i = 0; i < N_FUNCS; i++) if (!strcmp(kNames[i], name)) return kStubs[i];
        return 0;
    }
    /* (a table, not dlsym on ourselves: dlopen(RTLD_NOLOAD) of this library finds nothing on 6.0, and Chromium then
     * called a null eglCreateSyncKHR) */
#define P(f) { #f, (void *)f }
    static const struct { const char *n; void *f; } egl[] = {
        P(eglCreateImageKHR), P(eglDestroyImageKHR), P(eglCreateSyncKHR), P(eglDestroySyncKHR), P(eglClientWaitSyncKHR),
        P(eglGetSyncAttribKHR), P(eglSetDamageRegionKHR), P(eglSwapBuffersWithDamageKHR),
    };
#undef P
    for (unsigned i = 0; i < sizeof egl / sizeof egl[0]; i++) if (!strcmp(egl[i].n, name)) return egl[i].f;
    return 0;
}
