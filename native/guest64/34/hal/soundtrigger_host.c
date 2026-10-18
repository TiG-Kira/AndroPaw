// aemu-soundtrigger: registers the firmware's own passthrough sound trigger HAL (android.hardware.soundtrigger@2.x-impl.so)
// with hwservicemanager. ColorOS 14 on a Qualcomm board asks for that HAL in a loop on system_server's main thread
// until it is there (SoundTriggerModule.attachToHal under Build.isQcomPlatform()); the firmware hosts it inside its
// audio HAL service, which does not run here. The HAL finds no device and answers every call with an error, which
// the framework takes as "no sound trigger".
// No AOSP headers: the three C++ entry points are taken by their mangled names.
#include <dlfcn.h>
#include <stdio.h>
#include <string.h>

struct str { unsigned char b[24]; };    // libc++ std::string, short form: length * 2, then the characters

int main(void) {
    static const char *vers[] = { "2.3", "2.2", "2.1", "2.0" };
    static const char *ns[] = { "V2_3", "V2_2", "V2_1", "V2_0" };
    void *base = dlopen("libhidlbase.so", RTLD_NOW);
    if (!base) { fprintf(stderr, "libhidlbase: %s\n", dlerror()); return 1; }
    void (*configure)(unsigned long, int) = dlsym(base, "_ZN7android8hardware22configureRpcThreadpoolEmb");
    void (*join)(void) = dlsym(base, "_ZN7android8hardware17joinRpcThreadpoolEv");
    if (!configure || !join) { fprintf(stderr, "no thread pool entry points\n"); return 1; }
    for (int i = 0; i < 4; i++) {
        char path[128], sym[256];
        snprintf(path, sizeof path, "/vendor/lib64/hw/android.hardware.soundtrigger@%s-impl.so", vers[i]);
        void *impl = dlopen(path, RTLD_NOW);
        if (!impl) { fprintf(stderr, "%s: %s\n", path, dlerror()); continue; }
        void *(*fetch)(const char *) = dlsym(impl, "HIDL_FETCH_ISoundTriggerHw");
        snprintf(path, sizeof path, "android.hardware.soundtrigger@%s.so", vers[i]);
        void *iface = dlopen(path, RTLD_NOW);
        snprintf(sym, sizeof sym, "_ZN7android8hardware12soundtrigger4%s15ISoundTriggerHw17registerAsServiceERKNSt3__112basic_stringIcNS4_11char_traitsIcEENS4_9allocatorIcEEEE", ns[i]);
        int (*reg)(void *, const struct str *) = iface ? dlsym(iface, sym) : 0;
        if (!fetch || !reg) { fprintf(stderr, "%s: no entry points\n", vers[i]); continue; }
        void *hal = fetch("default");
        if (!hal) { fprintf(stderr, "%s: no HAL object\n", vers[i]); continue; }
        struct str name; memset(&name, 0, sizeof name);
        name.b[0] = 7 * 2; memcpy(name.b + 1, "default", 7);
        configure(2, 1);
        int r = reg(hal, &name);
        fprintf(stderr, "soundtrigger@%s registered: %d\n", vers[i], r);
        if (r != 0) continue;
        join();
        return 0;
    }
    return 1;
}
