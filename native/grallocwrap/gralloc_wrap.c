/*
 * gralloc.default.so of the native arm64 engine (Android 8.x): AOSP's default gralloc with YV12 buffers added.
 *
 * The default gralloc (ashmem buffers, the plain framebuffer) allocates RGB formats only, and the software video
 * path of the framework (SoftwareRenderer) asks the window for HAL_PIXEL_FORMAT_YV12: every frame of a video failed
 * in "createGraphicBuffer failed". The AOSP module stays as it is under the name vendor/lib64/libgralloc_aosp.so (outside hw/, where the tree fixer sets aside every gralloc but the default); this one forwards
 * everything to it and turns a YV12 request into an RGBA request of the same byte size. The handle of the default
 * gralloc carries no format (fd, size, base), so mapping and locking such a buffer needs nothing new: lock() returns
 * the base address, and the layout (Y plane, then V and U, strides aligned to 16) is the one the format defines.
 *
 * The camera's formats are added the same way: BLOB (a JPEG, w bytes), YCbCr_420_888 and NV21 (see lock_ycbcr),
 * IMPLEMENTATION_DEFINED as RGBA_8888.
 */
#include <stdint.h>
#include "note.h"
#ifdef AEMU_META
#include "../glbridge/common.h"
extern int socket(int domain, int type, int proto);
extern int connect(int fd, const void *addr, unsigned len);
extern long read(int fd, void *buf, unsigned long n);
extern long write(int fd, const void *buf, unsigned long n);
extern int close(int fd);
extern int getpid(void);
extern int pthread_mutex_lock(void *m);
extern int pthread_mutex_unlock(void *m);
#endif

extern void *dlopen(const char *path, int flags);
extern void *dlsym(void *h, const char *name);
extern void *malloc(unsigned long n);
extern void free(void *p);

#define EXPORT __attribute__((visibility("default")))

struct hw_module;
struct hw_device;
struct hw_module_methods { int (*open)(const struct hw_module *m, const char *id, struct hw_device **dev); };
struct hw_module {
    uint32_t tag;
    uint16_t module_api_version, hal_api_version;
    const char *id, *name, *author;
    struct hw_module_methods *methods;
    void *dso;
    unsigned long reserved[25];     /* (pointer-sized, as in AOSP: uint32_t on 32-bit) */
};
struct hw_device {
    uint32_t tag, version;
    struct hw_module *module;
    unsigned long reserved[12];
    int (*close)(struct hw_device *dev);
};
struct android_ycbcr { void *y, *cb, *cr; unsigned long ystride, cstride, chroma_step; uint32_t reserved[8]; };
struct gralloc_module {
    struct hw_module common;
    int (*registerBuffer)(const struct gralloc_module *m, const void *handle);
    int (*unregisterBuffer)(const struct gralloc_module *m, const void *handle);
    int (*lock)(const struct gralloc_module *m, const void *handle, int usage, int l, int t, int w, int h, void **vaddr);
    int (*unlock)(const struct gralloc_module *m, const void *handle);
    int (*perform)(const struct gralloc_module *m, int operation, ...);
    int (*lock_ycbcr)(const struct gralloc_module *m, const void *handle, int usage, int l, int t, int w, int h, struct android_ycbcr *out);
    void *more[6];      /* the async calls: absent in the default gralloc, absent here */
};
struct alloc_device {
    struct hw_device common;
    int (*alloc)(struct alloc_device *d, int w, int h, int format, int usage, const void **handle, int *stride);
    int (*free)(struct alloc_device *d, const void *handle);
    void (*dump)(struct alloc_device *d, char *buf, int len);
    void *reserved_proc[7];
};
struct wrap_device {
    struct alloc_device dev;
    struct alloc_device *real;
};

#define HARDWARE_MODULE_TAG 0x484d4f44
#define HAL_PIXEL_FORMAT_RGBA_8888 1
#define HAL_PIXEL_FORMAT_YV12 0x32315659
#define HAL_PIXEL_FORMAT_YCRCB_420_SP 0x11
#define HAL_PIXEL_FORMAT_BLOB 0x21
#define HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED 0x22
#define HAL_PIXEL_FORMAT_YCBCR_420_888 0x23

static const struct gralloc_module *real;

static int w_register(const struct gralloc_module *m, const void *h) { (void)m; return real ? real->registerBuffer(real, h) : -22; }
static int w_unregister(const struct gralloc_module *m, const void *h) { (void)m; return real ? real->unregisterBuffer(real, h) : -22; }
#ifdef AEMU_META
/* A buffer the GLES bridge draws into on the host (native/glbridge; the note's pad[1] is its frame number there):
 * before a program gets the memory, the host is asked for the pixels, and sends them if its own are newer. */
static int g_gl = -1, g_gl_pid;
static uint64_t g_gl_lock[8];           /* pthread_mutex_t, zero is its initial state */
static int rd_full(int fd, void *buf, unsigned long n) {
    char *p = buf;
    while (n) { long k = read(fd, p, n); if (k <= 0) return -1; p += k; n -= (unsigned long)k; }
    return 0;
}
static void gpu_fetch(struct meta_note *n, void *va) {
    struct { uint32_t op, len; uint64_t id, frame; uint32_t w, h; } __attribute__((packed)) q = { OP_BUF_READ, 24, n->id, n->pad[1], (uint32_t)n->w, (uint32_t)n->h };
    uint32_t ok = 0;
    if (n->w <= 0 || n->h <= 0 || n->stride < n->w) return;
    pthread_mutex_lock(g_gl_lock);
    if (g_gl >= 0 && g_gl_pid != getpid()) g_gl = -1;        /* (a forked child: the connection is the parent's) */
    if (g_gl < 0) {
        struct { uint16_t family; char path[108]; } a;
        static const char P[] = AEMU_GL_SOCKET;
        for (unsigned i = 0; i < sizeof(a.path); i++) a.path[i] = i < sizeof(P) ? P[i] : 0;
        a.family = 1;
        int fd = socket(1, 1 | 02000000 /* SOCK_STREAM | SOCK_CLOEXEC */, 0);
        if (fd >= 0 && connect(fd, &a, sizeof(a)) < 0) { close(fd); fd = -1; }
        g_gl = fd; g_gl_pid = getpid();
    }
    if (g_gl >= 0 && (write(g_gl, &q, sizeof(q)) != (long)sizeof(q) || rd_full(g_gl, &ok, 4) < 0)) { close(g_gl); g_gl = -1; ok = 0; }
    if (ok) {
        unsigned char row[4096 * 4];
        int bad = 0;
        for (int y = 0; y < n->h && !bad; y++) {
            unsigned char *dst = (unsigned char *)va + (unsigned long)y * (unsigned long)n->stride * (n->format == 4 ? 2 : 4);
            if ((n->format == 1 || n->format == 2) && 1) { bad = rd_full(g_gl, dst, (unsigned long)n->w * 4) < 0; continue; }
            for (int x = 0; x < n->w && !bad; ) {
                int k = n->w - x < 4096 ? n->w - x : 4096;
                bad = rd_full(g_gl, row, (unsigned long)k * 4) < 0;
                if (n->format == 4) for (int i = 0; i < k; i++)
                    ((uint16_t *)dst)[x + i] = (uint16_t)((row[i * 4] >> 3) << 11 | (row[i * 4 + 1] >> 2) << 5 | row[i * 4 + 2] >> 3);
                x += k;
            }
        }
        if (bad) { close(g_gl); g_gl = -1; }
    }
    pthread_mutex_unlock(g_gl_lock);
}
#endif
static int w_lock(const struct gralloc_module *m, const void *hnd, int usage, int l, int t, int w, int h, void **vaddr) {
    (void)m;
    int e = real ? real->lock(real, hnd, usage, l, t, w, h, vaddr) : -22;
#ifdef AEMU_META
    if (!e && vaddr && *vaddr) { struct meta_note *n = meta_note_at(hnd); if (n && n->magic == META_NOTE && n->pad[1]) gpu_fetch(n, *vaddr); }
    /* a count of the times the buffer was opened for writing: the GLES bridge (native/glbridge) sends the pixels to
     * the host only when it moved */
    if (!e && (usage & 0xf0)) { struct meta_note *n = meta_note_at(hnd); if (n && n->magic == META_NOTE) n->pad[0]++; }
#endif
    return e;
}
static int w_unlock(const struct gralloc_module *m, const void *h) { (void)m; return real ? real->unlock(real, h) : -22; }

/* A camera's YUV buffer is NV21 in one block (Y, then V and U interleaved, no row padding); its size and format go
 * into the last bytes of the block, which is how lock_ycbcr below knows the planes: the default gralloc's handle
 * (fd, magic, flags, size, offset, base) has no room for them. */
struct aosp_handle { int version, num_fds, num_ints, fd, magic, flags, size, offset; uint64_t base; };
struct yuv_note { uint32_t magic; int32_t w, h; uint32_t pad; };   /* pad: 0 NV21, else YV12 with pad = its Y stride */
#define YUV_NOTE 0x56594541u
static struct yuv_note *note_of(const void *handle) {
    const struct aosp_handle *p = handle;
    if (!p || !p->base || p->size < 4096) return 0;
    return (struct yuv_note *)(p->base + (uint64_t)p->size - sizeof(struct yuv_note));
}

/* DEBUG(ycbcr): why a lock_ycbcr failed, one line per failure in /data/misc/cameraserver/ycbcr.log */
extern int open(const char *p, int f, ...);
extern long write(int fd, const void *buf, unsigned long n);
extern int close(int fd);
static void dbg_hex(char **o, unsigned long v) { char t[17]; int n = 0; do { t[n++] = "0123456789abcdef"[v & 15]; v >>= 4; } while (v); while (n) *(*o)++ = t[--n]; *(*o)++ = ' '; }
static void dbg_ycbcr(const void *hnd, int e, const struct yuv_note *n) {
    char b[256], *o = b; const struct aosp_handle *p = hnd;
    dbg_hex(&o, (unsigned long)(unsigned)e);
    if (p) { dbg_hex(&o, (unsigned)p->version); dbg_hex(&o, (unsigned)p->num_fds); dbg_hex(&o, (unsigned)p->num_ints); dbg_hex(&o, (unsigned)p->magic);
             dbg_hex(&o, (unsigned)p->flags); dbg_hex(&o, (unsigned)p->size); dbg_hex(&o, (unsigned long)p->base); }
    if (n) dbg_hex(&o, n->magic);
    *o++ = 10;
    int fd = open("/data/misc/cameraserver/ycbcr.log", 02 | 0100 | 02000, 0666);
    if (fd >= 0) { write(fd, b, (unsigned long)(o - b)); close(fd); }
}
static int w_lock_ycbcr(const struct gralloc_module *m, const void *hnd, int usage, int l, int t, int w, int h, struct android_ycbcr *out) {
    (void)m;
    void *va = 0;
    if (!real || !out) return -22;
    int e = real->lock(real, hnd, usage, l, t, w, h, &va);
    if (e) { dbg_ycbcr(hnd, e, 0); return e; }
    struct yuv_note *n = note_of(hnd);
    if (!va || !n || n->magic != YUV_NOTE) { dbg_ycbcr(hnd, -22, n); real->unlock(real, hnd); return -22; }
    out->y = va;
    if (n->pad) {       /* YV12: Y, then V and U planes of half size, rows aligned to 16 */
        long ys = n->pad, cs = (ys / 2 + 15) & ~15;
        out->cr = (char *)va + ys * n->h;
        out->cb = (char *)out->cr + cs * ((n->h + 1) / 2);
        out->ystride = (unsigned long)ys;
        out->cstride = (unsigned long)cs;
        out->chroma_step = 1;
        return 0;
    }
    out->cr = (char *)va + (long)n->w * n->h;
    out->cb = (char *)out->cr + 1;
    out->ystride = (unsigned long)n->w;
    out->cstride = (unsigned long)n->w;
    out->chroma_step = 2;
    return 0;
}

/* AEMU_META (Android 17): every buffer gets room for the mapper's note as well (note.h) */
#ifdef AEMU_META
#define EXTRA ((long)META_ROOM)
#else
#define EXTRA 0L
#endif

static int alloc_any(struct alloc_device *r, int w, int h, int format, int usage, const void **handle, int *stride) {
    int unused = 0;
    if (format == HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED) format = HAL_PIXEL_FORMAT_RGBA_8888;
    if (format == HAL_PIXEL_FORMAT_BLOB) {      /* w bytes */
        if (w <= 0 || h != 1) return -22;
        int e = r->alloc(r, (int)((w + 3 + EXTRA) / 4), 1, HAL_PIXEL_FORMAT_RGBA_8888, usage, handle, &unused);
        if (e == 0) *stride = w;
        return e;
    }
    if (format == HAL_PIXEL_FORMAT_YCBCR_420_888 || format == HAL_PIXEL_FORMAT_YCRCB_420_SP) {
        if (w <= 0 || h <= 0 || w > 8192 || h > 8192) return -22;
        long size = (long)w * h + 2L * ((w + 1) / 2) * ((h + 1) / 2) + (long)sizeof(struct yuv_note) + EXTRA;
        int e = r->alloc(r, (int)((size + 3) / 4), 1, HAL_PIXEL_FORMAT_RGBA_8888, usage, handle, &unused);
        if (e) return e;
        struct yuv_note *n = note_of(*handle);
        if (n) { n->magic = YUV_NOTE; n->w = w; n->h = h; n->pad = 0; }
        *stride = w;
        return 0;
    }
    if (format >= 1 && format <= 5 && !(usage & 0x1000 /* HW_FB: the framebuffer's own pages */) && w > 0 && h > 0) {
        /* an RGB buffer: rows are added for the note the hwcomposer reads the buffer's shape from (note.h) */
        long bpr = ((long)w * (format == 4 ? 2 : format == 3 ? 3 : 4) + 3) & ~3L;
        int e = r->alloc(r, w, h + (int)(((EXTRA ? EXTRA : (long)sizeof(struct rgb_note)) + bpr - 1) / bpr), format, usage, handle, stride);
        struct rgb_note *n = e ? 0 : rgb_note_at(*handle);
        if (n) { n->magic = RGB_NOTE; n->w = w; n->h = h; n->stride = *stride; n->format = format; }
        return e;
    }
    if (format != HAL_PIXEL_FORMAT_YV12) return r->alloc(r, w, h, format, usage, handle, stride);
    if (w <= 0 || h <= 0 || w > 8192 || h > 8192) return -22;
    int ys = (w + 15) & ~15, cs = (ys / 2 + 15) & ~15;
    long size = (long)ys * h + 2L * cs * ((h + 1) / 2) + (long)sizeof(struct yuv_note) + EXTRA;
    int e = r->alloc(r, (int)((size + 3) / 4), 1, HAL_PIXEL_FORMAT_RGBA_8888, usage, handle, &unused);
    if (e) return e;
    /* (the note lets lock_ycbcr find the planes: CPU readers of a camera's YV12 previews lock it that way) */
    struct yuv_note *n = note_of(*handle);
    if (n) { n->magic = YUV_NOTE; n->w = w; n->h = h; n->pad = (uint32_t)ys; }
    *stride = ys;
    return 0;
}
static int d_alloc(struct alloc_device *d, int w, int h, int format, int usage, const void **handle, int *stride) {
    struct alloc_device *r = ((struct wrap_device *)d)->real;
#ifdef AEMU_META
    /* no buffer out of the framebuffer's own pages: those have no room for a note, and the hwcomposer reads a
     * plain buffer of the screen's size the same way */
    int fb = usage & 0x1000;
    usage &= ~0x1000;
    int known = (format >= 1 && format <= 5) || format == HAL_PIXEL_FORMAT_YV12 || format == HAL_PIXEL_FORMAT_BLOB ||
                format == HAL_PIXEL_FORMAT_YCRCB_420_SP || format == HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED ||
                format == HAL_PIXEL_FORMAT_YCBCR_420_888;
    if (!known) return -22;
    int e = alloc_any(r, w, h, format, usage, handle, stride);
    struct meta_note *m = e ? 0 : meta_note_at(*handle);
    if (m) {
        for (unsigned i = 0; i < sizeof *m; i++) ((char *)m)[i] = 0;
        m->magic = META_NOTE; m->w = w; m->h = h; m->format = format; m->stride = *stride; m->layers = 1;
        m->usage = (uint32_t)usage;
#ifdef NOTE_FB_USAGE   /* (6.0: the bridge reads the compositor's frame back into such a buffer, fb0 is the screen) */
        m->usage |= (uint32_t)fb;
#endif
        (void)fb;
        /* the buffer's number, by which the GLES bridge's host side knows it (one allocator process hands them out) */
        static uint64_t next_id;
        m->id = __atomic_add_fetch(&next_id, 1, __ATOMIC_RELAXED);
    }
    return e;
#else
    return alloc_any(r, w, h, format, usage, handle, stride);
#endif
}
static int d_free(struct alloc_device *d, const void *handle) { struct alloc_device *r = ((struct wrap_device *)d)->real; return r->free(r, handle); }
static int d_close(struct hw_device *d) {
    struct alloc_device *r = ((struct wrap_device *)d)->real;
    int e = r->common.close ? r->common.close(&r->common) : 0;
    free(d);
    return e;
}

static int w_open(const struct hw_module *m, const char *id, struct hw_device **out) {
    if (!real) return -22;
    struct hw_device *dev = 0;
    int e = real->common.methods->open(&real->common, id, &dev);
    if (e || !dev) return e ? e : -22;
    /* "gpu0" is the allocator; the framebuffer device ("fb0") is the AOSP one as it is */
    if (!(id[0] == 'g' && id[1] == 'p' && id[2] == 'u' && id[3] == '0' && !id[4])) { *out = dev; return 0; }
    struct wrap_device *wd = malloc(sizeof *wd);
    if (!wd) { *out = dev; return 0; }
    wd->dev = *(struct alloc_device *)dev;
    wd->real = (struct alloc_device *)dev;
    wd->dev.common.module = (struct hw_module *)m;
    wd->dev.common.close = d_close;
    wd->dev.alloc = d_alloc;
    wd->dev.free = d_free;
    wd->dev.dump = 0;
    *out = &wd->dev.common;
    return 0;
}

static struct hw_module_methods methods = { w_open };

EXPORT struct gralloc_module HMI = {
    .common = {
        .tag = HARDWARE_MODULE_TAG,
        .module_api_version = 1,
        .hal_api_version = 0,
        .id = "gralloc",
        .name = "AEmulator gralloc (AOSP default + YV12)",
        .author = "AEmulator",
        .methods = &methods,
    },
    .registerBuffer = w_register,
    .unregisterBuffer = w_unregister,
    .lock = w_lock,
    .unlock = w_unlock,
    .lock_ycbcr = w_lock_ycbcr,
};

__attribute__((constructor)) static void init(void) {
/* (6.0 has no vendor partition: REAL_DIR=/system) */
#ifndef REAL_DIR
#define REAL_DIR "/vendor"
#endif
#ifdef __LP64__
    void *h = dlopen(REAL_DIR "/lib64/libgralloc_aosp.so", 2 /* RTLD_NOW */);
#else       /* (32-bit programs - cameraserver of 11 - read the camera's YUV buffers through this one) */
    void *h = dlopen(REAL_DIR "/lib/libgralloc_aosp.so", 2 /* RTLD_NOW */);
#endif
    real = h ? dlsym(h, "HMI") : 0;
    if (real) {     /* the same API version as the module wrapped: the HIDL mapper picks its adapter by it */
        HMI.common.module_api_version = real->common.module_api_version;
        /* (0.2 at least: the gralloc 0 adapter of the HIDL mapper only calls lock_ycbcr from that version on - with
         * the default module's 0.1 a camera's YUV buffers could not be read, Camera1 previews and ImageReader got none) */
#ifndef __LP64__
        if (HMI.common.module_api_version < 2) HMI.common.module_api_version = 2;
#endif
        HMI.common.hal_api_version = real->common.hal_api_version;
    }
}
