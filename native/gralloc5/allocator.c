/*
 * android.hardware.graphics.allocator-service.aemu of the native arm64 engine (Android 17): the AIDL IAllocator
 * (version 2) libui's Gralloc5 asks for, over the engine's gralloc module. See mapper.c for why; this is the half
 * that allocates and names the mapper library ("aemu" -> vendor/lib64/hw/mapper.aemu.so).
 * No AOSP headers and no generated AIDL code: the four calls are read and written with libbinder_ndk's parcel
 * functions, laid out as the NDK backend lays them out.
 */
#include <dlfcn.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include "../grallocwrap/note.h"

typedef struct AIBinder AIBinder;
typedef struct AIBinder_Class AIBinder_Class;
typedef struct AParcel AParcel;
typedef struct AStatus AStatus;
extern AIBinder_Class *AIBinder_Class_define(const char *descriptor, void *(*on_create)(void *), void (*on_destroy)(void *),
                                             int32_t (*on_transact)(AIBinder *, uint32_t, const AParcel *, AParcel *));
extern AIBinder *AIBinder_new(const AIBinder_Class *c, void *args);
extern void AIBinder_markVintfStability(AIBinder *b);
extern int32_t AServiceManager_addService(AIBinder *b, const char *instance);
extern bool ABinderProcess_setThreadPoolMaxThreadCount(uint32_t n);
extern void ABinderProcess_joinThreadPool(void);
extern int32_t AParcel_readInt32(const AParcel *p, int32_t *v);
extern int32_t AParcel_readInt64(const AParcel *p, int64_t *v);
extern int32_t AParcel_readByteArray(const AParcel *p, void *data, bool (*alloc)(void *data, int32_t len, int8_t **out));
extern int32_t AParcel_writeInt32(AParcel *p, int32_t v);
extern int32_t AParcel_writeBool(AParcel *p, bool v);
extern int32_t AParcel_writeString(AParcel *p, const char *s, int32_t len);
extern int32_t AParcel_writeInt32Array(AParcel *p, const int32_t *a, int32_t len);
extern int32_t AParcel_writeParcelFileDescriptor(AParcel *p, int fd);
extern int32_t AParcel_getDataPosition(const AParcel *p);
extern int32_t AParcel_setDataPosition(const AParcel *p, int32_t pos);
extern int32_t AParcel_writeStatusHeader(AParcel *p, const AStatus *s);
extern AStatus *AStatus_newOk(void);
extern AStatus *AStatus_fromServiceSpecificError(int32_t e);
extern void AStatus_delete(AStatus *s);

struct hw_module;
struct hw_device { uint32_t tag, version; struct hw_module *module; uint64_t reserved[12]; int (*close)(struct hw_device *dev); };
struct hw_module_methods { int (*open)(const struct hw_module *m, const char *id, struct hw_device **dev); };
struct hw_module { uint32_t tag; uint16_t module_api_version, hal_api_version; const char *id, *name, *author; struct hw_module_methods *methods; void *dso; uint64_t reserved[25]; };
struct alloc_device {
    struct hw_device common;
    int (*alloc)(struct alloc_device *d, int w, int h, int format, int usage, const void **handle, int *stride);
    int (*free)(struct alloc_device *d, const void *handle);
};
struct native_handle { int version, num_fds, num_ints, data[]; };

#define DESCRIPTOR "android.hardware.graphics.allocator.IAllocator"
enum { T_ALLOCATE = 1, T_ALLOCATE2, T_IS_SUPPORTED, T_SUFFIX, T_VERSION = 0xffffff, T_HASH = 0xfffffe };
enum { BAD_DESCRIPTOR = 0, NO_RESOURCES = 1, UNSUPPORTED = 2 };    /* AllocationError */

static struct alloc_device *dev;
static uint64_t next_id;

struct info { char name[128]; int32_t w, h, layers, format; int64_t usage; };

static bool name_room(void *data, int32_t len, int8_t **out) {
    if (len != 128) return false;
    *out = data;
    return true;
}

/* BufferDescriptorInfo as an argument: "not null", its size, then name[128], width, height, layerCount, format,
 * usage, reservedSize, additionalOptions (left unread) */
static int read_info(const AParcel *in, struct info *i) {
    int32_t there = 0, size = 0;
    if (AParcel_readInt32(in, &there) || !there) return -1;
    int32_t start = AParcel_getDataPosition(in);
    if (AParcel_readInt32(in, &size) || size < 4) return -1;
    memset(i, 0, sizeof *i);
    if (AParcel_readByteArray(in, i->name, name_room)) return -1;
    i->name[127] = 0;
    if (AParcel_readInt32(in, &i->w) || AParcel_readInt32(in, &i->h) || AParcel_readInt32(in, &i->layers) ||
        AParcel_readInt32(in, &i->format) || AParcel_readInt64(in, &i->usage)) return -1;
    AParcel_setDataPosition(in, start + size);
    return 0;
}

static bool supported(const struct info *i) {
    int f = i->format;
    return i->w > 0 && i->h > 0 && i->layers == 1 && ((f >= 1 && f <= 5) || f == 0x11 || f == 0x21 || f == 0x22 || f == 0x23 || f == 0x32315659);
}

static void ok(AParcel *out) { AStatus *s = AStatus_newOk(); AParcel_writeStatusHeader(out, s); AStatus_delete(s); }
static void fail(AParcel *out, int e) { AStatus *s = AStatus_fromServiceSpecificError(e); AParcel_writeStatusHeader(out, s); AStatus_delete(s); }

static void end_parcelable(AParcel *out, int32_t start) {
    int32_t end = AParcel_getDataPosition(out);
    AParcel_setDataPosition(out, start);
    AParcel_writeInt32(out, end - start);
    AParcel_setDataPosition(out, end);
}

/* AllocationResult: stride, then the handles, each a NativeHandle (its fds, its ints) */
static void allocate(const struct info *i, int32_t count, AParcel *out) {
    const void *h[16];
    int stride = 0, n = 0;
    if (count < 0 || count > 16 || !supported(i)) { fail(out, count < 0 || count > 16 ? BAD_DESCRIPTOR : UNSUPPORTED); return; }
    for (; n < count; n++) {
        if (dev->alloc(dev, i->w, i->h, i->format, (int)(uint32_t)i->usage, &h[n], &stride)) break;
        struct meta_note *m = meta_note_at(h[n]);
        if (m && m->magic == META_NOTE) {
            m->usage = (uint64_t)i->usage;
            m->id = __atomic_add_fetch(&next_id, 1, __ATOMIC_RELAXED);
            memcpy(m->name, i->name, sizeof m->name - 1);
        }
    }
    if (n < count) {
        while (n > 0) dev->free(dev, h[--n]);
        fail(out, NO_RESOURCES);
        return;
    }
    ok(out);
    AParcel_writeInt32(out, 1);
    int32_t start = AParcel_getDataPosition(out);
    AParcel_writeInt32(out, 0);
    AParcel_writeInt32(out, stride);
    AParcel_writeInt32(out, count);
    for (int k = 0; k < count; k++) {
        const struct native_handle *nh = h[k];
        AParcel_writeInt32(out, 1);
        int32_t hs = AParcel_getDataPosition(out);
        AParcel_writeInt32(out, 0);
        AParcel_writeInt32(out, nh->num_fds);
        for (int f = 0; f < nh->num_fds; f++) AParcel_writeParcelFileDescriptor(out, nh->data[f]);   /* a duplicate goes out */
        AParcel_writeInt32Array(out, nh->data + nh->num_fds, nh->num_ints);
        end_parcelable(out, hs);
    }
    end_parcelable(out, start);
    for (int k = 0; k < count; k++) dev->free(dev, h[k]);
}

static int32_t on_transact(AIBinder *b, uint32_t code, const AParcel *in, AParcel *out) {
    (void)b;
    struct info i;
    int32_t count = 0;
    switch (code) {
    case T_ALLOCATE:        /* the descriptor of mapper 4: there is none here */
        fail(out, UNSUPPORTED);
        return 0;
    case T_ALLOCATE2:
        if (read_info(in, &i) || AParcel_readInt32(in, &count)) return -22 /* BAD_VALUE */;
        allocate(&i, count, out);
        return 0;
    case T_IS_SUPPORTED:
        if (read_info(in, &i)) return -22;
        ok(out); AParcel_writeBool(out, supported(&i));
        return 0;
    case T_SUFFIX:
        ok(out); AParcel_writeString(out, "aemu", 4);
        return 0;
    case T_VERSION:
        ok(out); AParcel_writeInt32(out, 2);
        return 0;
    case T_HASH:
        ok(out); AParcel_writeString(out, "aemu", 4);
        return 0;
    }
    return -74 /* UNKNOWN_TRANSACTION */;
}

static void *on_create(void *args) { return args; }
static void on_destroy(void *data) { (void)data; }

int main(void) {
    void *so = dlopen("/vendor/lib64/hw/gralloc.default.so", RTLD_NOW);
    const struct hw_module *m = so ? dlsym(so, "HMI") : 0;
    struct hw_device *d = 0;
    if (!m || m->methods->open(m, "gpu0", &d) || !d) { fprintf(stderr, "gralloc: %s\n", m ? "no allocator" : dlerror()); return 1; }
    dev = (struct alloc_device *)d;
    ABinderProcess_setThreadPoolMaxThreadCount(4);
    AIBinder_Class *c = AIBinder_Class_define(DESCRIPTOR, on_create, on_destroy, on_transact);
    AIBinder *b = c ? AIBinder_new(c, 0) : 0;
    if (!b) { fprintf(stderr, "no binder object\n"); return 1; }
    AIBinder_markVintfStability(b);
    int32_t e = AServiceManager_addService(b, DESCRIPTOR "/default");
    fprintf(stderr, "allocator registered: %d\n", e);
    if (e) return 1;
    ABinderProcess_joinThreadPool();
    return 0;
}
