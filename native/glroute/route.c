/*
 * libGLES_route: the EGL/GLES driver of 32-bit programs on the native arm64 engine.
 *
 * Such a program runs in the translator. Its own drawing (a game's GL thread) goes to the phone's GPU through the
 * 4.4 engine's bridge (libGLES_split.so in front of libGLES_bridge.so), where a software renderer gives about a
 * frame a second. The window toolkit's drawing (hwui's RenderThread, Chromium's GPU thread) stays on SwiftShader:
 * the bridge does not carry hwui's stream correctly, and that drawing is light.
 *
 * The renderer is chosen per thread, at the thread's first call, and each has a display of its own behind the one
 * display libEGL sees. Depends on libc and libdl only.
 */
#include "names.h"

extern void *dlopen(const char *name, int flags);
extern void *dlsym(void *handle, const char *name);
extern int prctl(int option, ...);
extern int pthread_key_create(int *key, void (*destructor)(void *));
extern void *pthread_getspecific(int key);
extern int pthread_setspecific(int key, const void *value);

#define N (sizeof(kNames) / sizeof(kNames[0]))
#define SELF "/system/lib/egl/libGLES.so"        /* the name TreeFixer gives this file (Kernel64.gpuBridge32) */
enum { GPU, SOFT };
static const char *const kLibs[2][3] = {
    { "/system/lib/egl/libGLES_split.so", 0, 0 },
    { "/system/lib/egl/libEGL_swiftshader.so", "/system/lib/egl/libGLESv2_swiftshader.so",
      "/system/lib/egl/libGLESv1_CM_swiftshader.so" },
};
static void *g_lib[2][3];
static void *g_fn[2][N];
static void *g_dpy[2];
static volatile int g_up[2];
static int g_key;
static volatile int g_keyed;       /* 0 none, 1 being made, 2 there (a key itself may be any value, negative too) */

/* threads (by the start of their name) that draw with the toolkit: hwui's own, and Chromium's (a WebView or a browser
 * hands its textures over to hwui's context, so both ends have to be the same renderer) */
static const char *const kSoft[] = { "RenderThread", "Chrome_", "Compositor", "CrGpu", "GpuThread" };

static int zero_fn(void) { return 0; }

static int backend(void) {
    if (g_keyed != 2) {
        if (!__sync_bool_compare_and_swap(&g_keyed, 0, 1)) return GPU;      /* another thread is at it right now */
        if (pthread_key_create(&g_key, 0)) { g_keyed = 0; return GPU; }
        g_keyed = 2;
    }
    long v = (long)pthread_getspecific(g_key);
    if (!v) {
        char n[17] = {0};
        prctl(16 /* PR_GET_NAME */, n, 0, 0, 0);
        v = 1 + GPU;
        for (unsigned k = 0; k < sizeof kSoft / sizeof kSoft[0]; k++) {
            unsigned i = 0;
            while (kSoft[k][i] && n[i] == kSoft[k][i]) i++;
            if (!kSoft[k][i]) v = 1 + SOFT;
        }
        pthread_setspecific(g_key, (void *)v);
    }
    return (int)v - 1;
}

static void *lookup(int be, unsigned idx) {
    void *f = g_fn[be][idx];
    if (f) return f;
    for (int i = 0; i < 3 && !f && kLibs[be][i]; i++) {
        if (!g_lib[be][i]) g_lib[be][i] = dlopen(kLibs[be][i], 0);
        if (g_lib[be][i]) f = dlsym(g_lib[be][i], kNames[idx]);
    }
    if (!f) f = (void *)zero_fn;       /* an entry this renderer does not have */
    g_fn[be][idx] = f;
    return f;
}

__attribute__((visibility("hidden"))) void *aemu_route_resolve(unsigned idx) { return lookup(backend(), idx); }

/* the calling thread's renderer's display, brought up on first use */
__attribute__((visibility("hidden"))) void *aemu_route_dpy(void) {
    int be = backend();
    if (!g_dpy[be]) g_dpy[be] = ((void *(*)(void *))lookup(be, IDX_eglGetDisplay))(0);
    if (!g_up[be]) {
        g_up[be] = 1;
        ((unsigned (*)(void *, int *, int *))lookup(be, IDX_eglInitialize))(g_dpy[be], 0, 0);
    }
    return g_dpy[be];
}

#define EXPORT __attribute__((visibility("default")))
#define THE_DISPLAY ((void *)0x52)

EXPORT void *eglGetDisplay(void *native) { (void)native; return THE_DISPLAY; }

EXPORT unsigned eglInitialize(void *dpy, int *major, int *minor) {
    (void)dpy;
    if (!aemu_route_dpy()) return 0;
    if (major) *major = 1;
    if (minor) *minor = 4;
    return 1;
}

/* libEGL ends its one display; the renderers' displays live as long as the process */
EXPORT unsigned eglTerminate(void *dpy) { (void)dpy; return 1; }

EXPORT void *eglGetCurrentDisplay(void) {
    return ((void *(*)(void))lookup(backend(), IDX_eglGetCurrentDisplay))() ? THE_DISPLAY : 0;
}

/* an entry asked for by name is this library's own one, so that it routes like the rest */
EXPORT void *eglGetProcAddress(const char *name) {
    static void *self;
    if (!self) self = dlopen(SELF, 0);
    return self ? dlsym(self, name) : 0;
}
