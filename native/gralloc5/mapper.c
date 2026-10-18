/*
 * mapper.aemu.so of the native arm64 engine (Android 17): the stable-C IMapper 5 over the engine's gralloc module.
 *
 * libui of Android 17 takes a mapper of version 4 or 5 only (the HIDL mappers 2 and 3 the engine served until 16 are
 * no longer tried), and aborts with "gralloc-mapper is missing". Buffers stay what they were: handles of AOSP's
 * default gralloc (fd, magic, flags, size, offset, base), allocated, mapped and locked by vendor/lib64/hw/
 * gralloc.default.so, so the engine's hwcomposer and camera read them as before. What IMapper 5 adds is questions
 * about a buffer (size, format, stride, planes, dataspace); the answers are in the note the engine's gralloc leaves
 * in front of the buffer's last 32 bytes (native/grallocwrap/note.h, built with AEMU_META).
 * No AOSP headers: the interface is declared here as hardware/interfaces/graphics/mapper/stable-c lays it out.
 */
#include <stddef.h>
#include <stdint.h>
#include "../grallocwrap/note.h"

extern void *dlopen(const char *path, int flags);
extern void *dlsym(void *h, const char *name);
extern void *malloc(size_t n);
extern void free(void *p);
extern void *memcpy(void *d, const void *s, size_t n);
extern size_t strlen(const char *s);
extern int strcmp(const char *a, const char *b);
extern int dup(int fd);
extern int close(int fd);
struct pollfd { int fd; short events, revents; };
extern int poll(struct pollfd *fds, unsigned long n, int timeout);

#define EXPORT __attribute__((visibility("default")))

struct hw_module { uint32_t tag; uint16_t module_api_version, hal_api_version; const char *id, *name, *author; void *methods, *dso; uint64_t reserved[25]; };
struct gralloc_module {
    struct hw_module common;
    int (*registerBuffer)(const struct gralloc_module *m, const void *handle);
    int (*unregisterBuffer)(const struct gralloc_module *m, const void *handle);
    int (*lock)(const struct gralloc_module *m, const void *handle, int usage, int l, int t, int w, int h, void **vaddr);
    int (*unlock)(const struct gralloc_module *m, const void *handle);
};
struct native_handle { int version, num_fds, num_ints, data[]; };

enum { E_NONE = 0, E_BAD_DESCRIPTOR = 1, E_BAD_BUFFER = 2, E_BAD_VALUE = 3, E_NO_RESOURCES = 5, E_UNSUPPORTED = 7 };
struct rect { int32_t left, top, right, bottom; };
struct metadata_type { const char *name; int64_t value; };

static const struct gralloc_module *gralloc;

static const struct meta_note *meta_of(const void *buffer) {
    const struct meta_note *m = meta_note_at(buffer);
    return m && m->magic == META_NOTE ? m : 0;
}

static int m_import(const struct native_handle *h, const void **out) {
    *out = 0;
    if (!gralloc || !h || h->num_fds < 1 || h->num_fds > 8 || h->num_ints < 0 || h->num_ints > 64) return E_BAD_BUFFER;
    size_t n = sizeof *h + sizeof(int) * (size_t)(h->num_fds + h->num_ints);
    struct native_handle *c = malloc(n);
    if (!c) return E_NO_RESOURCES;
    memcpy(c, h, n);
    for (int i = 0; i < h->num_fds; i++) c->data[i] = dup(h->data[i]);
    if (gralloc->registerBuffer(gralloc, c)) {
        for (int i = 0; i < c->num_fds; i++) close(c->data[i]);
        free(c);
        return E_BAD_BUFFER;
    }
    *out = c;
    return E_NONE;
}

static int m_free(const void *buffer) {
    struct native_handle *c = (struct native_handle *)buffer;
    if (!gralloc || !c) return E_BAD_BUFFER;
    gralloc->unregisterBuffer(gralloc, c);
    for (int i = 0; i < c->num_fds; i++) close(c->data[i]);
    free(c);
    return E_NONE;
}

static int m_transport_size(const void *buffer, uint32_t *fds, uint32_t *ints) {
    const struct native_handle *c = buffer;
    if (!c) return E_BAD_BUFFER;
    *fds = (uint32_t)c->num_fds; *ints = (uint32_t)c->num_ints;
    return E_NONE;
}

static int m_lock(const void *buffer, uint64_t usage, struct rect r, int fence, void **out) {
    if (fence >= 0) {       /* the fence is this side's to wait for and close */
        struct pollfd p = { fence, 1 /* POLLIN */, 0 };
        poll(&p, 1, 3000);
        close(fence);
    }
    *out = 0;
    if (!gralloc || !buffer) return E_BAD_BUFFER;
    if (gralloc->lock(gralloc, buffer, (int)(usage & 0xff), r.left, r.top, r.right - r.left, r.bottom - r.top, out) || !*out) return E_BAD_BUFFER;
    return E_NONE;
}

static int m_unlock(const void *buffer, int *fence) {
    *fence = -1;
    if (!gralloc || !buffer) return E_BAD_BUFFER;
    return gralloc->unlock(gralloc, buffer) ? E_BAD_BUFFER : E_NONE;
}

static int m_nop(const void *buffer) { return buffer ? E_NONE : E_BAD_BUFFER; }

/* A value as IMapperMetadataTypes.h writes it: the type's name and number, then the value; strings and lists are
 * led by a 64-bit count. Returns the size wanted, whether or not it fitted. */
#define STANDARD "android.hardware.graphics.common.StandardMetadataType"
struct wr { uint8_t b[1024]; size_t n; };
static void put(struct wr *w, const void *p, size_t n) { if (w->n + n <= sizeof w->b) memcpy(w->b + w->n, p, n); w->n += n; }
static void put64(struct wr *w, int64_t v) { put(w, &v, 8); }
static void put32(struct wr *w, int32_t v) { put(w, &v, 4); }
static void puts_(struct wr *w, const char *s) { size_t n = strlen(s); put64(w, (int64_t)n); put(w, s, n); }
static void put_ext(struct wr *w, const char *name, int64_t v) { puts_(w, name); put64(w, v); }

#define COMPONENT "android.hardware.graphics.common.PlaneLayoutComponentType"
enum { C_Y = 1, C_CB = 2, C_CR = 4, C_R = 1 << 10, C_G = 1 << 11, C_B = 1 << 12, C_RAW = 1 << 20, C_A = 1 << 30 };
static void plane_head(struct wr *w, int components) { put64(w, components); }
static void component(struct wr *w, int64_t type, int64_t off_bits, int64_t size_bits) { put_ext(w, COMPONENT, type); put64(w, off_bits); put64(w, size_bits); }
static void plane_tail(struct wr *w, int64_t off, int64_t inc_bits, int64_t stride, int64_t ws, int64_t hs, int64_t hsub, int64_t vsub) {
    put64(w, off); put64(w, inc_bits); put64(w, stride); put64(w, ws); put64(w, hs); put64(w, stride * hs); put64(w, hsub); put64(w, vsub);
}

/* the layouts gralloc_wrap.c gives the formats it allocates */
static int planes(struct wr *w, const struct meta_note *m) {
    int64_t W = m->w, H = m->h, cw = (W + 1) / 2, ch = (H + 1) / 2;
    switch (m->format) {
    case 1: case 2: case 5: case 0x22: {     /* RGBA, RGBX, BGRA; IMPLEMENTATION_DEFINED is RGBA here */
        int bgra = m->format == 5, alpha = m->format != 2;
        put64(w, 1); plane_head(w, alpha ? 4 : 3);
        component(w, bgra ? C_B : C_R, 0, 8); component(w, C_G, 8, 8); component(w, bgra ? C_R : C_B, 16, 8);
        if (alpha) component(w, C_A, 24, 8);
        plane_tail(w, 0, 32, (int64_t)m->stride * 4, W, H, 1, 1);
        return 1;
    }
    case 3:
        put64(w, 1); plane_head(w, 3);
        component(w, C_R, 0, 8); component(w, C_G, 8, 8); component(w, C_B, 16, 8);
        plane_tail(w, 0, 24, (int64_t)m->stride * 3, W, H, 1, 1);
        return 1;
    case 4:
        put64(w, 1); plane_head(w, 3);
        component(w, C_B, 0, 5); component(w, C_G, 5, 6); component(w, C_R, 11, 5);
        plane_tail(w, 0, 16, (int64_t)m->stride * 2, W, H, 1, 1);
        return 1;
    case 0x11: case 0x23:                   /* NV21 in one block: Y, then V and U interleaved, no row padding */
        put64(w, 2);
        plane_head(w, 1); component(w, C_Y, 0, 8); plane_tail(w, 0, 8, W, W, H, 1, 1);
        plane_head(w, 2); component(w, C_CR, 0, 8); component(w, C_CB, 8, 8); plane_tail(w, W * H, 16, W, cw, ch, 2, 2);
        return 2;
    case 0x32315659: {                      /* YV12: Y, V, U; strides aligned to 16 */
        int64_t ys = (W + 15) & ~15, cs = (ys / 2 + 15) & ~15;
        put64(w, 3);
        plane_head(w, 1); component(w, C_Y, 0, 8); plane_tail(w, 0, 8, ys, W, H, 1, 1);
        plane_head(w, 1); component(w, C_CR, 0, 8); plane_tail(w, ys * H, 8, cs, cw, ch, 2, 2);
        plane_head(w, 1); component(w, C_CB, 0, 8); plane_tail(w, ys * H + cs * ch, 8, cs, cw, ch, 2, 2);
        return 3;
    }
    case 0x21:                              /* BLOB: w bytes */
        put64(w, 1); plane_head(w, 1); component(w, C_RAW, 0, 8); plane_tail(w, 0, 8, W, W, 1, 1, 1);
        return 1;
    }
    return 0;
}

static int32_t m_get_standard(const void *buffer, int64_t type, void *dest, size_t dest_size) {
    const struct meta_note *m = meta_of(buffer);
    if (!m) return -E_BAD_BUFFER;
    struct wr w; w.n = 0;
    puts_(&w, STANDARD); put64(&w, type);
    switch (type) {
    case 1: put64(&w, (int64_t)m->id); break;
    case 2: { size_t n = 0; while (n < sizeof m->name && m->name[n]) n++; put64(&w, (int64_t)n); put(&w, m->name, n); break; }
    case 3: put64(&w, m->w); break;
    case 4: put64(&w, m->h); break;
    case 5: put64(&w, m->layers); break;
    case 6: put32(&w, m->format); break;
    case 7: put32(&w, 0); break;            /* no DRM fourcc, no modifier */
    case 8: put64(&w, 0); break;
    case 9: put64(&w, (int64_t)m->usage); break;
    case 10: put64(&w, ((const struct rgb_note_handle *)buffer)->size); break;
    case 11: put64(&w, 0); break;
    case 12: put_ext(&w, "android.hardware.graphics.common.Compression", 0); break;
    case 13: put_ext(&w, "android.hardware.graphics.common.Interlaced", 0); break;
    case 14: put_ext(&w, "android.hardware.graphics.common.ChromaSiting", 0); break;
    case 15: if (!planes(&w, m)) return -E_UNSUPPORTED; break;
    case 16: {                              /* the crop of every plane: all of it */
        struct wr t; t.n = 0;
        int n = planes(&t, m);
        if (!n) return -E_UNSUPPORTED;
        put64(&w, n);
        for (int i = 0; i < n; i++) { put32(&w, 0); put32(&w, 0); put32(&w, m->w); put32(&w, m->h); }
        break;
    }
    case 17: put32(&w, m->dataspace); break;
    case 18: put32(&w, m->blend); break;
    case 23: put32(&w, m->stride); break;
    default: return -E_UNSUPPORTED;
    }
    if (w.n > sizeof w.b) return -E_NO_RESOURCES;
    if (dest && dest_size >= w.n) memcpy(dest, w.b, w.n);
    return (int32_t)w.n;
}

static int32_t m_get(const void *buffer, struct metadata_type t, void *dest, size_t dest_size) {
    if (!t.name || strcmp(t.name, STANDARD)) return -E_UNSUPPORTED;
    return m_get_standard(buffer, t.value, dest, dest_size);
}

static int m_set_standard(const void *buffer, int64_t type, const void *data, size_t size) {
    struct meta_note *m = (struct meta_note *)meta_of(buffer);
    if (!m) return E_BAD_BUFFER;
    size_t head = 8 + sizeof STANDARD - 1 + 8;
    if (type != 17 && type != 18) return E_UNSUPPORTED;
    if (!data || size < head + 4) return E_BAD_VALUE;
    int32_t v; memcpy(&v, (const char *)data + head, 4);
    if (type == 17) m->dataspace = v; else m->blend = v;
    return E_NONE;
}

static int m_set(const void *buffer, struct metadata_type t, const void *data, size_t size) {
    if (!t.name || strcmp(t.name, STANDARD)) return E_UNSUPPORTED;
    return m_set_standard(buffer, t.value, data, size);
}

static int m_list(const void **list, size_t *n) { *list = 0; *n = 0; return E_NONE; }
static int m_dump(const void *buffer, void *cb, void *ctx) { (void)cb; (void)ctx; return buffer ? E_NONE : E_BAD_BUFFER; }
static int m_dump_all(void *begin, void *cb, void *ctx) { (void)begin; (void)cb; (void)ctx; return E_NONE; }
static int m_reserved(const void *buffer, void **region, uint64_t *size) { *region = 0; *size = 0; return buffer ? E_NONE : E_BAD_BUFFER; }
static int m_no_views(void) { return E_UNSUPPORTED; }

struct mapper {
    _Alignas(16) uint32_t version;
    struct {
        int (*importBuffer)(const struct native_handle *h, const void **out);
        int (*freeBuffer)(const void *buffer);
        int (*getTransportSize)(const void *buffer, uint32_t *fds, uint32_t *ints);
        int (*lock)(const void *buffer, uint64_t usage, struct rect r, int fence, void **out);
        int (*unlock)(const void *buffer, int *fence);
        int (*flushLockedBuffer)(const void *buffer);
        int (*rereadLockedBuffer)(const void *buffer);
        int32_t (*getMetadata)(const void *buffer, struct metadata_type t, void *dest, size_t size);
        int32_t (*getStandardMetadata)(const void *buffer, int64_t type, void *dest, size_t size);
        int (*setMetadata)(const void *buffer, struct metadata_type t, const void *data, size_t size);
        int (*setStandardMetadata)(const void *buffer, int64_t type, const void *data, size_t size);
        int (*listSupportedMetadataTypes)(const void **list, size_t *n);
        int (*dumpBuffer)(const void *buffer, void *cb, void *ctx);
        int (*dumpAllBuffers)(void *begin, void *cb, void *ctx);
        int (*getReservedRegion)(const void *buffer, void **region, uint64_t *size);
    } v5;
    struct { int (*getMultiViewInfo)(void), (*getBaseView)(void), (*importViewBuffer)(void); } v6;
};

static struct mapper mapper = {
    .version = 5,
    .v5 = { m_import, m_free, m_transport_size, m_lock, m_unlock, m_nop, m_nop, m_get, m_get_standard, m_set, m_set_standard,
            m_list, m_dump, m_dump_all, m_reserved },
    .v6 = { m_no_views, m_no_views, m_no_views },
};

EXPORT uint32_t ANDROID_HAL_STABLEC_VERSION = 5;

EXPORT int AIMapper_loadIMapper(struct mapper **out) {
    if (!gralloc) {
        void *h = dlopen("/vendor/lib64/hw/gralloc.default.so", 2 /* RTLD_NOW */);
        gralloc = h ? dlsym(h, "HMI") : 0;
    }
    if (!gralloc) { *out = 0; return E_NO_RESOURCES; }
    *out = &mapper;
    return E_NONE;
}
