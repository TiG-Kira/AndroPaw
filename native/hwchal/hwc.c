/*
 * hwcomposer.default.so of the native arm64 engine: the screen is put together here, not by SurfaceFlinger's GLES.
 *
 * GLES in the guest is a software renderer, and composing a home screen with it (wallpaper, launcher, bars: four
 * blended full-screen quads) took three to four cores. A layer that is a plain copy or blend of an RGBA buffer
 * (no scaling, no rotation) is taken as an overlay and blended into the frame with NEON; anything else, and every
 * layer under it, stays with SurfaceFlinger and arrives composed in the framebuffer target, which is the bottom
 * of the frame then. The frame goes to /dev/graphics/fb0, the file the app's screen view shows.
 *
 * With the GLES bridge drawing for SurfaceFlinger too (native/glbridge, debug.aemu.gl=3) none of that is done: every
 * layer is left to SurfaceFlinger, whose frame is then a buffer of the phone's GPU, and the host is told that
 * buffer's number and shows it itself (present()). No pixel of the frame is touched in the guest.
 *
 * A buffer's size and format come from the note the engine's gralloc (native/grallocwrap) leaves in the buffer's
 * last bytes: the default gralloc's handle has no room for them. HWC 1.2 (plane alpha, integer crops), which the
 * 8.1 composer service drives through its HWC2-on-1 adapter. Depends on libc and libhardware.
 */
#include <stddef.h>
#include <stdint.h>
#include <arm_neon.h>
#include "../grallocwrap/note.h"
#include "../glbridge/common.h"

extern int open(const char *path, int flags, ...);
extern int close(int fd);
extern int ioctl(int fd, unsigned long req, ...);
extern void *mmap(void *a, size_t n, int prot, int flags, int fd, long off);
extern long lseek(int fd, long off, int whence);
extern void *malloc(size_t n);
extern void *memcpy(void *d, const void *s, size_t n);
extern void *memset(void *d, int c, size_t n);
extern int pthread_create(long *t, const void *attr, void *(*fn)(void *), void *arg);
struct timespec64 { long tv_sec, tv_nsec; };
extern int clock_gettime(int clk, struct timespec64 *ts);
extern int clock_nanosleep(int clk, int flags, const struct timespec64 *req, struct timespec64 *rem);
extern int hw_get_module(const char *id, const void **module);
extern int socket(int domain, int type, int proto);
extern int connect(int fd, const void *addr, unsigned len);
extern long read(int fd, void *buf, unsigned long n);
extern long write(int fd, const void *buf, unsigned long n);
extern int __system_property_get(const char *name, char *value);

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
    uint64_t reserved[25];
};
struct hw_device {
    uint32_t tag, version;
    struct hw_module *module;
    uint64_t reserved[12];
    int (*close)(struct hw_device *dev);
};
struct gralloc_module {
    struct hw_module common;
    int (*registerBuffer)(const struct gralloc_module *m, const void *handle);
    int (*unregisterBuffer)(const struct gralloc_module *m, const void *handle);
    int (*lock)(const struct gralloc_module *m, const void *handle, int usage, int l, int t, int w, int h, void **vaddr);
    int (*unlock)(const struct gralloc_module *m, const void *handle);
};

struct rect { int l, t, r, b; };
struct region { size_t n; const struct rect *rects; };
struct layer {
    int32_t type;
    uint32_t hints, flags;
    const void *handle;
    uint32_t transform;
    int32_t blending;
    struct rect crop, frame;
    struct region visible;
    int acquire_fence, release_fence;
    uint8_t plane_alpha, pad[3];
    struct region damage;
    uint8_t reserved[8];
};
struct contents {
    int retire_fence;
    void *dpy, *sur;
    uint32_t flags;
    size_t n;
    struct layer layers[];
};
struct procs {
    void (*invalidate)(const struct procs *p);
    void (*vsync)(const struct procs *p, int disp, int64_t ts);
    void (*hotplug)(const struct procs *p, int disp, int connected);
};
struct hwc {
    struct hw_device common;
    int (*prepare)(struct hwc *d, size_t n, struct contents **c);
    int (*set)(struct hwc *d, size_t n, struct contents **c);
    int (*event_control)(struct hwc *d, int disp, int event, int on);
    int (*blank)(struct hwc *d, int disp, int blank);
    int (*query)(struct hwc *d, int what, int *value);
    void (*register_procs)(struct hwc *d, const struct procs *p);
    void (*dump)(struct hwc *d, char *buf, int len);
    int (*get_configs)(struct hwc *d, int disp, uint32_t *configs, size_t *n);
    int (*get_attributes)(struct hwc *d, int disp, uint32_t config, const uint32_t *attrs, int32_t *values);
    void *reserved_proc[7];

    const struct procs *procs;
    volatile int vsync_on;
    const struct gralloc_module *gralloc;
    int w, h;
    uint32_t *fb, *back;
    uint32_t *frames;           /* after the screen page: a count of frames, so the viewer shows each one at once */
    int direct, gl;             /* SurfaceFlinger draws on the phone's GPU; the connection to the renderer there */
};

enum { L_FRAMEBUFFER = 0, L_OVERLAY = 1, L_BACKGROUND = 2, L_TARGET = 3 };
#define SKIP_LAYER 1u
#define BLEND_NONE 0x100
#define BLEND_PREMULT 0x105
#define BLEND_COVERAGE 0x405
#define FMT_RGBA 1
#define FMT_RGBX 2
#define VSYNC_NS 16666666L

static void *base_of(struct hwc *d, const void *handle) {
    void *va = 0;
    if (!handle || !d->gralloc) return 0;
    if (d->gralloc->lock(d->gralloc, handle, 3 /* SW_READ_OFTEN */, 0, 0, 1, 1, &va)) return 0;
    d->gralloc->unlock(d->gralloc, handle);
    return va;
}

/* a layer this module draws: a 1:1 copy or blend of a 32-bit buffer it knows the shape of */
static const struct rgb_note *plain(struct hwc *d, const struct layer *l) {
    if ((l->type != L_FRAMEBUFFER && l->type != L_OVERLAY) || (l->flags & SKIP_LAYER) || l->transform) return 0;
    if (l->blending != BLEND_NONE && l->blending != BLEND_PREMULT && l->blending != BLEND_COVERAGE) return 0;
    int w = l->frame.r - l->frame.l, h = l->frame.b - l->frame.t;
    if (w <= 0 || h <= 0 || l->crop.r - l->crop.l != w || l->crop.b - l->crop.t != h) return 0;
    if (!base_of(d, l->handle)) return 0;
    const struct rgb_note *n = rgb_note_of(l->handle);
    if (!n || (n->format != FMT_RGBA && n->format != FMT_RGBX)) return 0;
    if (l->crop.l < 0 || l->crop.t < 0 || l->crop.r > n->w || l->crop.b > n->h) return 0;
    return n;
}

/* The frame SurfaceFlinger composed, shown by the host from its own copy of the buffer. 0: the host has no such
 * buffer (drawn by the software renderer, or no renderer there at all). */
static int present(struct hwc *d, const void *handle) {
    struct meta_note *n = meta_note_at(handle);
    if (!n || n->magic != META_NOTE || !n->id || n->w <= 0 || n->h <= 0) return 0;
    struct { uint32_t op, len; uint64_t id; uint32_t w, h; } __attribute__((packed)) q = { OP_PRESENT, 16, n->id, (uint32_t)n->w, (uint32_t)n->h };
    uint32_t ok = 0;
    for (int tries = 0; tries < 2; tries++) {
        if (d->gl < 0) {
            struct { uint16_t family; char path[108]; } a;
            static const char P[] = AEMU_GL_SOCKET;
            for (unsigned i = 0; i < sizeof(a.path); i++) a.path[i] = i < sizeof(P) ? P[i] : 0;
            a.family = 1;
            int fd = socket(1, 1 | 02000000 /* SOCK_STREAM | SOCK_CLOEXEC */, 0);
            if (fd >= 0 && connect(fd, &a, sizeof(a)) < 0) { close(fd); fd = -1; }
            if (fd < 0) { d->direct = 0; return 0; }       /* (no renderer on the host: the old way from now on) */
            d->gl = fd;
        }
        if (write(d->gl, &q, sizeof(q)) == (long)sizeof(q) && read(d->gl, &ok, 4) == 4) return (int)ok;
        close(d->gl); d->gl = -1;
    }
    return 0;
}

static int hwc_prepare(struct hwc *d, size_t nd, struct contents **cs) {
    struct contents *c = nd && cs ? cs[0] : 0;
    if (!c) return 0;
    if (d->direct) {
        for (size_t i = 0; i < c->n; i++) if (c->layers[i].type != L_TARGET) c->layers[i].type = L_FRAMEBUFFER;
        return 0;
    }
    long cut = -1;      /* the topmost layer left to SurfaceFlinger: what is under it goes there too */
    for (size_t i = 0; i < c->n; i++) if (c->layers[i].type != L_TARGET && !plain(d, &c->layers[i])) cut = (long)i;
    for (size_t i = 0; i < c->n; i++) {
        struct layer *l = &c->layers[i];
        if (l->type == L_TARGET) continue;
        l->type = (long)i > cut ? L_OVERLAY : L_FRAMEBUFFER;
    }
    return 0;
}

/* dst = src (times alpha) over dst; src is premultiplied unless cov, and opaque whatever its alpha byte if solid */
static void blend_row(uint8_t *dst, const uint8_t *src, int n, unsigned alpha, int solid, int cov) {
    if (solid && alpha == 255) { memcpy(dst, src, (size_t)n * 4); return; }
    const uint8x8_t pa = vdup_n_u8((uint8_t)alpha), full = vdup_n_u8(255);
#define MUL255(a, b) ({ uint16x8_t t_ = vmull_u8(a, b); vrshrn_n_u16(vrsraq_n_u16(t_, t_, 8), 8); })
    int i = 0;
    for (; i + 8 <= n; i += 8) {
        uint8x8x4_t s = vld4_u8(src + i * 4), o = vld4_u8(dst + i * 4);
        if (solid) s.val[3] = full;
        if (cov) for (int k = 0; k < 3; k++) s.val[k] = MUL255(s.val[k], s.val[3]);
        if (alpha != 255) for (int k = 0; k < 4; k++) s.val[k] = MUL255(s.val[k], pa);
        uint8x8_t inv = vmvn_u8(s.val[3]);
        for (int k = 0; k < 3; k++) o.val[k] = vqadd_u8(s.val[k], MUL255(o.val[k], inv));
        o.val[3] = full;
        vst4_u8(dst + i * 4, o);
    }
#undef MUL255
    for (; i < n; i++) {
        unsigned s[4] = { src[i * 4], src[i * 4 + 1], src[i * 4 + 2], solid ? 255u : src[i * 4 + 3] };
        if (cov) for (int k = 0; k < 3; k++) s[k] = (s[k] * s[3] + 127) / 255;
        if (alpha != 255) for (int k = 0; k < 4; k++) s[k] = (s[k] * alpha + 127) / 255;
        for (int k = 0; k < 3; k++) {
            unsigned v = s[k] + (dst[i * 4 + k] * (255 - s[3]) + 127) / 255;
            dst[i * 4 + k] = (uint8_t)(v > 255 ? 255 : v);
        }
        dst[i * 4 + 3] = 255;
    }
}

static void draw(struct hwc *d, const struct layer *l, const struct rgb_note *n) {
    const uint8_t *base = base_of(d, l->handle);
    int x0 = l->frame.l, y0 = l->frame.t, x1 = l->frame.r, y1 = l->frame.b;
    int sx = l->crop.l, sy = l->crop.t;
    if (x0 < 0) { sx -= x0; x0 = 0; }
    if (y0 < 0) { sy -= y0; y0 = 0; }
    if (x1 > d->w) x1 = d->w;
    if (y1 > d->h) y1 = d->h;
    if (!base || x0 >= x1 || y0 >= y1) return;
    int solid = n->format == FMT_RGBX || l->blending == BLEND_NONE;
    for (int y = y0; y < y1; y++)
        blend_row((uint8_t *)(d->back + (size_t)y * d->w + x0), base + ((size_t)(sy + y - y0) * n->stride + sx) * 4,
                  x1 - x0, l->plane_alpha, solid, l->blending == BLEND_COVERAGE);
}

static int hwc_set(struct hwc *d, size_t nd, struct contents **cs) {
    struct contents *c = nd && cs ? cs[0] : 0;
    if (!c) return 0;
    const struct layer *target = 0;
    int theirs = 0, ours = 0;
    for (size_t i = 0; i < c->n; i++) {
        struct layer *l = &c->layers[i];
        if (l->acquire_fence >= 0) { close(l->acquire_fence); l->acquire_fence = -1; }    /* drawing here is finished when queued */
        l->release_fence = -1;
        if (l->type == L_TARGET) target = l; else if (l->type == L_OVERLAY) ours++; else theirs++;
    }
    c->retire_fence = -1;
    if (d->direct && target && target->handle && present(d, target->handle)) return 0;
    if (!d->fb) return 0;
    size_t bytes = (size_t)d->w * d->h * 4;
    const void *under = target && (theirs || !ours) ? base_of(d, target->handle) : 0;
    if (under) memcpy(d->back, under, bytes); else memset(d->back, 0, bytes);
    for (size_t i = 0; i < c->n; i++) {
        const struct layer *l = &c->layers[i];
        const struct rgb_note *n = l->type == L_OVERLAY ? rgb_note_of(l->handle) : 0;
        if (n) draw(d, l, n);
    }
    memcpy(d->fb, d->back, bytes);
    if (d->frames) *(volatile uint32_t *)d->frames = *d->frames + 1;      /* this thread is the only writer */
    return 0;
}

static int hwc_event_control(struct hwc *d, int disp, int event, int on) {
    if (disp || event) return -22;
    d->vsync_on = on;
    return 0;
}
static int hwc_blank(struct hwc *d, int disp, int blank) { (void)d; (void)disp; (void)blank; return 0; }
static int hwc_query(struct hwc *d, int what, int *value) {
    (void)d;
    switch (what) {
    case 0: *value = 0; return 0;                   /* no background layer */
    case 1: *value = (int)VSYNC_NS; return 0;
    case 2: *value = 1; return 0;                   /* the primary display only */
    }
    return -22;
}
static void hwc_register_procs(struct hwc *d, const struct procs *p) { d->procs = p; }
static void hwc_dump(struct hwc *d, char *buf, int len) { (void)d; if (buf && len > 0) buf[0] = 0; }
static int hwc_get_configs(struct hwc *d, int disp, uint32_t *configs, size_t *n) {
    (void)d;
    if (disp) return -22;
    if (*n) { configs[0] = 0; *n = 1; }
    return 0;
}
static int hwc_get_attributes(struct hwc *d, int disp, uint32_t config, const uint32_t *attrs, int32_t *values) {
    if (disp || config) return -22;
    for (int i = 0; attrs[i]; i++) switch (attrs[i]) {
    case 1: values[i] = (int32_t)VSYNC_NS; break;
    case 2: values[i] = d->w; break;
    case 3: values[i] = d->h; break;
    case 4: case 5: values[i] = 160000; break;      /* as the plain framebuffer reports; the density is a property */
    default: values[i] = 0;
    }
    return 0;
}

static void *vsync_thread(void *arg) {
    struct hwc *d = arg;
    struct timespec64 t;
    clock_gettime(1 /* MONOTONIC */, &t);
    for (;;) {
        t.tv_nsec += VSYNC_NS;
        if (t.tv_nsec >= 1000000000L) { t.tv_nsec -= 1000000000L; t.tv_sec++; }
        clock_nanosleep(1, 1 /* TIMER_ABSTIME */, &t, 0);
        const struct procs *p = d->procs;
        if (d->vsync_on && p && p->vsync) p->vsync(p, 0, t.tv_sec * 1000000000LL + t.tv_nsec);
    }
    return 0;
}

static int hwc_close(struct hw_device *dev) { (void)dev; return 0; }

static int hwc_open(const struct hw_module *m, const char *id, struct hw_device **out) {
    static struct hwc dev;
    static int up;
    const char *want = "composer";
    for (int i = 0; ; i++) { if (id[i] != want[i]) return -22; if (!id[i]) break; }
    if (!up) {
        struct hwc *d = &dev;
        d->common.tag = 0x48574454;                 /* HARDWARE_DEVICE_TAG */
        d->common.version = 0x01020001;             /* HWC 1.2 */
        d->common.module = (struct hw_module *)m;
        d->common.close = hwc_close;
        d->prepare = hwc_prepare; d->set = hwc_set; d->event_control = hwc_event_control; d->blank = hwc_blank;
        d->query = hwc_query; d->register_procs = hwc_register_procs; d->dump = hwc_dump;
        d->get_configs = hwc_get_configs; d->get_attributes = hwc_get_attributes;
        const void *g = 0;
        if (hw_get_module("gralloc", &g) == 0) d->gralloc = g;
        char mode[92] = {0};
        d->gl = -1;
        d->direct = __system_property_get("debug.aemu.gl", mode) > 0 && mode[0] == '3';
        int fd = open("/dev/graphics/fb0", 2 /* O_RDWR */);
        uint32_t var[40] = {0};
        if (fd < 0 || ioctl(fd, 0x4600 /* FBIOGET_VSCREENINFO */, var) || !var[0] || !var[1]) return -19;
        d->w = (int)var[0]; d->h = (int)var[1];
        size_t bytes = (size_t)d->w * d->h * 4;
        int counted = lseek(fd, 0, 2 /* SEEK_END */) >= (long)bytes + 8;       /* the file is two pages long */
        void *fb = mmap(0, bytes + (counted ? 8 : 0), 3, 1 /* MAP_SHARED */, fd, 0);
        close(fd);
        d->back = malloc(bytes);
        if (fb == (void *)-1 || !d->back) return -12;
        d->fb = fb;
        if (counted) d->frames = (uint32_t *)((char *)fb + bytes);
        long t;
        pthread_create(&t, 0, vsync_thread, d);
        up = 1;
    }
    *out = &dev.common;
    return 0;
}

static struct hw_module_methods methods = { hwc_open };

EXPORT struct hw_module HMI = {
    .tag = 0x484d4f44,
    .module_api_version = 1,
    .hal_api_version = 0,
    .id = "hwcomposer",
    .name = "AEmulator hwcomposer (NEON overlays over the framebuffer file)",
    .author = "AEmulator",
    .methods = &methods,
};
