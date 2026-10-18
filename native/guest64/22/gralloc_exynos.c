/*
 * Kernel 22 (Android 5.1): gralloc.default.so for firmware of Samsung's Exynos phones (TouchWiz of the Galaxy S6).
 *
 * Samsung's libgui looks into every buffer that is queued: it takes the handle for the one of its own gralloc
 * (version 12, numFds + numInts = 37, the magic 0x3141592 at byte 0x18) and writes eight bytes at 0x90. A handle of
 * the default gralloc (one fd and eight ints, the magic at 0x10) fails that check, and the code goes on to write
 * through a null pointer: SurfaceFlinger dies on its first frame. The check is the same in the 32-bit library.
 *
 * So the buffers stay the default gralloc's (ashmem, the plain framebuffer), and only the handle the system sees is
 * laid out as Samsung expects. The firmware's own module stays next to this one as lib(64)/libgralloc_rom.so; its
 * handle is kept behind the one given out, and rebuilt from the fd, flags, size and offset in a process the buffer
 * comes to over binder. The tree fixer puts this file in place only where libgui has that check.
 */
#include <stdint.h>

extern void *dlopen(const char *path, int flags);
extern void *dlsym(void *h, const char *name);
extern void *calloc(unsigned long n, unsigned long size);
extern void free(void *p);
extern int getpid(void);

#define EXPORT __attribute__((visibility("default")))
#ifdef __LP64__
#define REAL "/system/lib64/libgralloc_rom.so"
#else
#define REAL "/system/lib/libgralloc_rom.so"
#endif

struct hw_module;
struct hw_device;
struct hw_module_methods { int (*open)(const struct hw_module *m, const char *id, struct hw_device **dev); };
struct hw_module {
    uint32_t tag;
    uint16_t module_api_version, hal_api_version;
    const char *id, *name, *author;
    struct hw_module_methods *methods;
    void *dso;
#ifdef __LP64__
    uint64_t reserved[32 - 7];
#else
    uint32_t reserved[32 - 7];
#endif
};
struct hw_device {
    uint32_t tag, version;
    struct hw_module *module;
#ifdef __LP64__
    uint64_t reserved[12];
#else
    uint32_t reserved[12];
#endif
    int (*close)(struct hw_device *dev);
};
struct gralloc_module {
    struct hw_module common;
    int (*registerBuffer)(const struct gralloc_module *m, const void *handle);
    int (*unregisterBuffer)(const struct gralloc_module *m, const void *handle);
    int (*lock)(const struct gralloc_module *m, const void *handle, int usage, int l, int t, int w, int h, void **vaddr);
    int (*unlock)(const struct gralloc_module *m, const void *handle);
    int (*perform)(const struct gralloc_module *m, int operation, ...);
    void *more[7];
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
struct fb_device {
    struct hw_device common;
    uint32_t flags, width, height;
    int stride, format;
    float xdpi, ydpi, fps;
    int minSwapInterval, maxSwapInterval, numFramebuffers;
    int reserved[7];
    int (*setSwapInterval)(struct fb_device *d, int interval);
    int (*setUpdateRect)(struct fb_device *d, int l, int t, int w, int h);
    int (*post)(struct fb_device *d, const void *handle);
    int (*compositionComplete)(struct fb_device *d);
    void (*dump)(struct fb_device *d, char *buf, int len);
    int (*enableScreen)(struct fb_device *d, int enable);
    void *reserved_proc[6];
};
struct wrap_fb {
    struct fb_device dev;
    struct fb_device *real;
};

#define HARDWARE_MODULE_TAG 0x484d4f44
#define MAGIC 0x3141592

/* the default gralloc's handle, the same bytes for 32- and 64-bit programs */
struct rom_handle {
    int version, numFds, numInts;
    int fd, magic, flags, size, offset;
    uint64_t base;
    int pid;
};

/* the handle given out: 40 ints. Named by byte offset where Samsung's code reads or writes. */
enum {
    W_VERSION = 0, W_NUMFDS, W_NUMINTS,
    W_FD,               /* 0x0c */
    W_FD1, W_FD2,       /* Exynos keeps two more fds here; ints here, -1 */
    W_MAGIC,            /* 0x18 */
    W_FLAGS, W_SIZE, W_OFFSET, W_FORMAT, W_WIDTH, W_HEIGHT, W_STRIDE, W_VSTRIDE,
    W_ROM = 28,         /* 0x70: the default gralloc's handle in this process (8 bytes) */
    W_ROM_PID = 30,     /* the process W_ROM is good in */
    W_REG_PID = 31,     /* the process that made W_ROM in registerBuffer and owes its release */
    W_SAMSUNG = 36,     /* 0x90: eight bytes libgui writes */
    W_INTS = 40
};

static const struct gralloc_module *real;

static int mine(const int *w) { return w && w[W_VERSION] == 12 && w[W_NUMFDS] + w[W_NUMINTS] == W_INTS - 3 && w[W_MAGIC] == MAGIC; }
static struct rom_handle *rom_of(const int *w) { return (struct rom_handle *)(uintptr_t)*(const uint64_t *)&w[W_ROM]; }
static void set_rom(int *w, struct rom_handle *r) { *(uint64_t *)&w[W_ROM] = (uintptr_t)r; }

static int w_register(const struct gralloc_module *m, const void *h) {
    (void)m;
    int *w = (int *)h;
    if (!real || !mine(w)) return -22;
    if (w[W_ROM_PID] == getpid() && rom_of(w)) return 0;      /* allocated or registered here already */
    struct rom_handle *r = calloc(1, sizeof *r);
    if (!r) return -12;
    r->version = 12; r->numFds = 1; r->numInts = 8;
    r->fd = w[W_FD]; r->magic = MAGIC; r->flags = w[W_FLAGS]; r->size = w[W_SIZE]; r->offset = w[W_OFFSET];
    int e = real->registerBuffer(real, r);
    if (e) { free(r); return e; }
    set_rom(w, r);
    w[W_ROM_PID] = w[W_REG_PID] = getpid();
    return 0;
}
static int w_unregister(const struct gralloc_module *m, const void *h) {
    (void)m;
    int *w = (int *)h;
    if (!real || !mine(w)) return -22;
    if (w[W_REG_PID] != getpid() || !rom_of(w)) return 0;
    struct rom_handle *r = rom_of(w);
    int e = real->unregisterBuffer(real, r);
    free(r);
    set_rom(w, 0);
    w[W_ROM_PID] = w[W_REG_PID] = 0;
    return e;
}
static int w_lock(const struct gralloc_module *m, const void *hnd, int usage, int l, int t, int w, int h, void **vaddr) {
    (void)m;
    if (!real || !mine(hnd) || !rom_of(hnd)) return -22;
    return real->lock(real, rom_of(hnd), usage, l, t, w, h, vaddr);
}
static int w_unlock(const struct gralloc_module *m, const void *h) {
    (void)m;
    if (!real || !mine(h) || !rom_of(h)) return -22;
    return real->unlock(real, rom_of(h));
}

static int d_alloc(struct alloc_device *d, int w, int h, int format, int usage, const void **handle, int *stride) {
    struct alloc_device *r = ((struct wrap_device *)d)->real;
    const void *rh = 0;
    int e = r->alloc(r, w, h, format, usage, &rh, stride);
    if (e) return e;
    const struct rom_handle *q = rh;
    int *o = calloc(W_INTS, sizeof(int));
    if (!o) { r->free(r, rh); return -12; }
    o[W_VERSION] = 12; o[W_NUMFDS] = 1; o[W_NUMINTS] = W_INTS - 4;
    o[W_FD] = q->fd; o[W_FD1] = o[W_FD2] = -1;
    o[W_MAGIC] = MAGIC; o[W_FLAGS] = q->flags; o[W_SIZE] = q->size; o[W_OFFSET] = q->offset;
    o[W_FORMAT] = format; o[W_WIDTH] = w; o[W_HEIGHT] = h; o[W_STRIDE] = stride ? *stride : w; o[W_VSTRIDE] = h;
    set_rom(o, (struct rom_handle *)q);
    o[W_ROM_PID] = getpid();
    *handle = o;
    return 0;
}
static int d_free(struct alloc_device *d, const void *handle) {
    struct alloc_device *r = ((struct wrap_device *)d)->real;
    int *w = (int *)handle;
    if (!mine(w)) return -22;
    int e = rom_of(w) && w[W_ROM_PID] == getpid() && !w[W_REG_PID] ? r->free(r, rom_of(w)) : 0;
    free(w);
    return e;
}
static int d_close(struct hw_device *d) {
    struct alloc_device *r = ((struct wrap_device *)d)->real;
    int e = r->common.close ? r->common.close(&r->common) : 0;
    free(d);
    return e;
}

/* the framebuffer device: the firmware's, which takes the buffer to show by the default gralloc's handle */
#define FB(d) (((struct wrap_fb *)(d))->real)
static int f_swap_interval(struct fb_device *d, int interval) { return FB(d)->setSwapInterval(FB(d), interval); }
static int f_update_rect(struct fb_device *d, int l, int t, int w, int h) { return FB(d)->setUpdateRect(FB(d), l, t, w, h); }
static int f_post(struct fb_device *d, const void *handle) {
    if (!mine(handle) || !rom_of(handle)) return -22;
    return FB(d)->post(FB(d), rom_of(handle));
}
static int f_composition_complete(struct fb_device *d) { return FB(d)->compositionComplete(FB(d)); }
static int f_enable_screen(struct fb_device *d, int enable) { return FB(d)->enableScreen(FB(d), enable); }
static int f_close(struct hw_device *d) {
    struct fb_device *r = FB(d);
    int e = r->common.close ? r->common.close(&r->common) : 0;
    free(d);
    return e;
}
static struct hw_device *wrap_fb(const struct hw_module *m, struct fb_device *r) {
    struct wrap_fb *f = calloc(1, sizeof *f);
    if (!f) return 0;
    f->dev = *r;
    f->real = r;
    f->dev.common.module = (struct hw_module *)m;
    f->dev.common.close = f_close;
    if (r->setSwapInterval) f->dev.setSwapInterval = f_swap_interval;
    if (r->setUpdateRect) f->dev.setUpdateRect = f_update_rect;
    f->dev.post = f_post;
    if (r->compositionComplete) f->dev.compositionComplete = f_composition_complete;
    f->dev.dump = 0;
    if (r->enableScreen) f->dev.enableScreen = f_enable_screen;
    return &f->dev.common;
}

static int w_open(const struct hw_module *m, const char *id, struct hw_device **out) {
    if (!real) return -22;
    struct hw_device *dev = 0;
    int e = real->common.methods->open(&real->common, id, &dev);
    if (e || !dev) return e ? e : -22;
    /* "gpu0" is the allocator, "fb0" the framebuffer */
    if (id[0] == 'f' && id[1] == 'b' && id[2] == '0' && !id[3]) {
        *out = wrap_fb(m, (struct fb_device *)dev);
        if (!*out) { if (dev->close) dev->close(dev); return -12; }
        return 0;
    }
    if (!(id[0] == 'g' && id[1] == 'p' && id[2] == 'u' && id[3] == '0' && !id[4])) { *out = dev; return 0; }
    struct wrap_device *wd = calloc(1, sizeof *wd);
    if (!wd) { if (dev->close) dev->close(dev); return -12; }
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
        .name = "AEmulator gralloc (default gralloc, Exynos handle layout)",
        .author = "AEmulator",
        .methods = &methods,
    },
    .registerBuffer = w_register,
    .unregisterBuffer = w_unregister,
    .lock = w_lock,
    .unlock = w_unlock,
};

__attribute__((constructor)) static void init(void) {
    void *h = dlopen(REAL, 2 /* RTLD_NOW */);
    real = h ? dlsym(h, "HMI") : 0;
    if (real) {
        HMI.common.module_api_version = real->common.module_api_version;
        HMI.common.hal_api_version = real->common.hal_api_version;
    }
}
