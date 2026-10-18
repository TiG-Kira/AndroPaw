// aemu-omxstore: registers android.hardware.media.omx@1.0::IOmxStore/default with no OMX behind it.
// On 11 MediaCodecList is built from an OMX and a Codec2 part and is not built at all when the OMX part fails
// ("Cannot find an IOmxStore service"): every app asking for the codec list (WebView, Camera) then dies on a null
// list. A GSI has no vendor codec service, and the engine's 8.1 one cannot link. The firmware's own
// libstagefright_omx makes the store: with IOmx = null and a codec XML that names no codec it lists no OMX codec, which is what
// main_codecservice.cpp does when vendor.media.omx is 0. The software codecs come from Codec2 (media.swcodec).
// No AOSP headers: the C++ entry points are taken by their mangled names (libc++ of the platform, std::__1).
#include <dlfcn.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

struct str { unsigned char b[24]; };    // std::string, short form: length * 2, then the characters
struct vec { void *b, *e, *c; };        // std::vector, empty

int main(void) {
    void *base = dlopen("libhidlbase.so", RTLD_NOW);
    void *omx = dlopen("libstagefright_omx.so", RTLD_NOW);
    void *iface = dlopen("android.hardware.media.omx@1.0.so", RTLD_NOW);
    if (!base || !omx || !iface) { fprintf(stderr, "dlopen: %s\n", dlerror()); return 1; }
    void (*configure)(unsigned long, int) = dlsym(base, "_ZN7android8hardware22configureRpcThreadpoolEmb");
    void (*join)(void) = dlsym(base, "_ZN7android8hardware17joinRpcThreadpoolEv");
    void (*ctor)(void *, void **, const char *, const struct vec *, const struct vec *, const char *) = dlsym(omx,
        "_ZN7android8hardware5media3omx4V1_014implementation8OmxStoreC1ERKNS_2spINS3_4IOmxEEEPKcRKNSt3__16vectorINSD_12basic_stringIcNSD_11char_traitsIcEENSD_9allocatorIcEEEENSI_ISK_EEEESO_SC_");
    int (*reg)(void *, const struct str *) = dlsym(iface,
        "_ZN7android8hardware5media3omx4V1_09IOmxStore17registerAsServiceERKNSt3__112basic_stringIcNS5_11char_traitsIcEENS5_9allocatorIcEEEE");
    if (!configure || !join || !ctor || !reg) { fprintf(stderr, "no entry points\n"); return 1; }
    void *store = calloc(1, 4096);       // larger than OmxStore; freed by nobody, the process lives as long as it
    void *nullOmx = 0;
    // one codec list of our own that names no codec (with no file at all the store reports a parsing error)
    static struct str dir, file;
    dir.b[0] = 20 * 2; memcpy(dir.b + 1, "/system/etc/aemu-omx", 20);
    file.b[0] = 16 * 2; memcpy(file.b + 1, "media_codecs.xml", 16);
    struct vec dirs = { &dir, &dir + 1, &dir + 1 }, files = { &file, &file + 1, &file + 1 };
    ctor(store, &nullOmx, "vendor", &dirs, &files, "/data/misc/media/media_codecs_profiling_results.xml");
    struct str name; memset(&name, 0, sizeof name);
    name.b[0] = 7 * 2; memcpy(name.b + 1, "default", 7);
    configure(1, 1);
    int r = reg(store, &name);
    fprintf(stderr, "IOmxStore registered: %d\n", r);
    if (r != 0) return 1;
    join();
    return 0;
}
