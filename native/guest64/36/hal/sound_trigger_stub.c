// sound_trigger.primary.aemu.so: a sound trigger HAL module with nothing behind it. The firmware's own passthrough
// HIDL implementation loads it (ro.hardware.sound_trigger.primary=aemu) instead of the board's, which wants its DSP; the
// framework gets valid properties and "not supported" for the rest.
#include <stdint.h>
#include <string.h>

struct hw_module;
struct hw_device { uint32_t tag, version; struct hw_module *module; uint64_t reserved[12]; int (*close)(struct hw_device *); };
struct hw_methods { int (*open)(const struct hw_module *, const char *, struct hw_device **); };
struct hw_module {
    uint32_t tag; uint16_t module_api_version, hal_api_version;
    const char *id, *name, *author; struct hw_methods *methods; void *dso; uint64_t reserved[32 - 7];
};
struct properties {
    char implementor[80], description[80];
    unsigned version;
    struct { uint32_t a; uint16_t b, c, d; uint8_t node[6]; } uuid;
    unsigned max_sound_models, max_key_phrases, max_users, recognition_modes;
    _Bool capture_transition; unsigned max_buffer_ms; _Bool concurrent_capture, trigger_in_event; unsigned power_consumption_mw;
};
struct device {
    struct hw_device common;
    int (*get_properties)(const struct device *, struct properties *);
    long (*rest[11])();
};

static int get_properties(const struct device *d, struct properties *p) {
    memset(p, 0, sizeof *p);
    strcpy(p->implementor, "AEmulator"); strcpy(p->description, "Sound trigger stand-in");
    p->version = 0x0100;
    p->uuid.a = 0xed7a7d60; p->uuid.b = 0xc65e; p->uuid.c = 0x11e3; p->uuid.d = 0x9be4;
    memcpy(p->uuid.node, "\x00\x02\xa5\xd5\xc5\x1b", 6);
    p->max_sound_models = 1; p->max_key_phrases = 1; p->max_users = 1; p->recognition_modes = 1;
    p->concurrent_capture = 1;
    return 0;
}
// (device API 1.3, the only one the 2.3 HIDL implementation takes: the properties come with a header)
static struct { struct { uint32_t version; uint64_t size; } header; struct properties base; char supported_model_arch[64]; unsigned audio_capabilities; } g_ext;
static long get_properties_extended(const struct device *d) {
    memset(&g_ext, 0, sizeof g_ext);
    g_ext.header.version = 0x0103; g_ext.header.size = sizeof g_ext;
    get_properties(d, &g_ext.base);
    return (long)&g_ext;
}
static long unsupported() { return -38; /* ENOSYS */ }
static int dev_close(struct hw_device *d) { return 0; }

static struct device g_dev;
static int dev_open(const struct hw_module *m, const char *name, struct hw_device **out) {
    g_dev.common.tag = 0x48574454 /*HWDT*/; g_dev.common.version = 0x0103;
    g_dev.common.module = (struct hw_module *)m; g_dev.common.close = dev_close;
    g_dev.get_properties = get_properties;
    for (int i = 0; i < 11; i++) g_dev.rest[i] = unsupported;
    g_dev.rest[9] = (long (*)())get_properties_extended;
    *out = &g_dev.common;
    return 0;
}
static struct hw_methods g_methods = { dev_open };
__attribute__((visibility("default"))) struct hw_module HMI = {
    .tag = 0x48574d54 /*HWMT*/, .module_api_version = 0x0100, .hal_api_version = 0x0100,
    .id = "sound_trigger", .name = "AEmulator sound trigger stand-in", .author = "AEmulator", .methods = &g_methods,
};
