/* Kernel 22 (Android 5.1): the name libEGL loads as its driver. Everything goes on to SwiftShader's own libEGL, kept
 * next to it as libreal_swiftshader.so (in the same folder: it looks for its GLES libraries from where it lies; a name
 * libEGL's own search does not match), except the calls a vendor's SurfaceFlinger makes with things only
 * its GPU driver knows:
 *   eglCreateContext — Qualcomm and MediaTek builds ask for EGL_IMG_context_priority without checking that the driver
 *   has it; SwiftShader answers EGL_BAD_ATTRIBUTE and SurfaceFlinger aborts ("EGLContext creation failed").
 *   eglCreateWindowSurface — Samsung's SurfaceFlinger (TouchWiz on Exynos) passes attribute 0x304F of its Mali
 *   driver; SwiftShader answers EGL_BAD_ATTRIBUTE, the display gets no surface and SurfaceFlinger aborts on its
 *   first swap. */
#define EXPORT __attribute__((visibility("default")))
#define REAL "/system/lib64/egl/libreal_swiftshader.so"
#define GLES_DIR "/system/lib64/egl/"
#define EGL_NONE 0x3038
#define EGL_CONTEXT_PRIORITY_LEVEL_IMG 0x3100
#define EGL_MALI_WINDOW_ATTRIB 0x304F

extern void *dlopen(const char *, int);
extern void *dlsym(void *, const char *);

#define FORWARDS(F) \
    F(eglBindAPI) F(eglBindTexImage) F(eglChooseConfig) F(eglClientWaitSync) F(eglClientWaitSyncKHR) F(eglCopyBuffers) \
    F(eglCreateImage) F(eglCreateImageKHR) F(eglCreatePbufferFromClientBuffer) F(eglCreatePbufferSurface) \
    F(eglCreatePixmapSurface) F(eglCreatePlatformPixmapSurface) F(eglCreatePlatformPixmapSurfaceEXT) \
    F(eglCreatePlatformWindowSurface) F(eglCreatePlatformWindowSurfaceEXT) F(eglCreateSync) F(eglCreateSyncKHR) \
    F(eglDestroyContext) F(eglDestroyImage) F(eglDestroyImageKHR) F(eglDestroySurface) \
    F(eglDestroySync) F(eglDestroySyncKHR) F(eglGetConfigAttrib) F(eglGetConfigs) F(eglGetCurrentContext) \
    F(eglGetCurrentDisplay) F(eglGetCurrentSurface) F(eglGetDisplay) F(eglGetError) F(eglGetPlatformDisplay) \
    F(eglGetPlatformDisplayEXT) F(eglGetProcAddress) F(eglGetSyncAttrib) F(eglGetSyncAttribKHR) F(eglInitialize) \
    F(eglMakeCurrent) F(eglQueryAPI) F(eglQueryContext) F(eglQueryString) F(eglQuerySurface) F(eglReleaseTexImage) \
    F(eglReleaseThread) F(eglSurfaceAttrib) F(eglSwapBuffers) F(eglSwapInterval) F(eglTerminate) F(eglWaitClient) \
    F(eglWaitGL) F(eglWaitNative) F(eglWaitSync) F(libEGL_swiftshader)

/* one pointer and one jump per forwarded call: the arguments pass through untouched */
#define DECL(n) void *aemu_p_##n; \
    __asm__(".text\n.globl " #n "\n.type " #n ",%function\n" #n ":\n" \
            "  adrp x16, aemu_p_" #n "\n  ldr x16, [x16, :lo12:aemu_p_" #n "]\n  br x16\n");
FORWARDS(DECL)

static void *(*real_create_context)(void *, void *, void *, const int *);
static void *(*real_create_window_surface)(void *, void *, void *, const int *);

/* the attribute list without one attribute; `out` holds 16 pairs and the end mark */
static const int *without(const int *attrs, int drop, int *out) {
    int n = 0;
    if (!attrs) return attrs;
    for (; attrs[0] != EGL_NONE && n < 32; attrs += 2) {
        if (attrs[0] == drop) continue;
        out[n++] = attrs[0];
        out[n++] = attrs[1];
    }
    out[n] = EGL_NONE;
    return out;
}

EXPORT void *eglCreateContext(void *dpy, void *config, void *share, const int *attrs) {
    int mine[34];
    return real_create_context(dpy, config, share, without(attrs, EGL_CONTEXT_PRIORITY_LEVEL_IMG, mine));
}

EXPORT void *eglCreateWindowSurface(void *dpy, void *config, void *win, const int *attrs) {
    int mine[34];
    return real_create_window_surface(dpy, config, win, without(attrs, EGL_MALI_WINDOW_ATTRIB, mine));
}

__attribute__((constructor)) static void init(void) {
    void *h = dlopen(REAL, 2 /* RTLD_NOW */);
    if (!h) return;
    /* SwiftShader's libEGL looks for its GLES libraries by bare name, once, and keeps a failure for good. The
     * system's libEGL has usually loaded them from this directory by then; Samsung's (TouchWiz) asks the EGL driver
     * first, so they are loaded here ("EGLContext creation failed" with EGL_BAD_ALLOC otherwise). */
    dlopen(GLES_DIR "libGLESv2_swiftshader.so", 2);
    dlopen(GLES_DIR "libGLESv1_CM_swiftshader.so", 2);
#define LOAD(n) aemu_p_##n = dlsym(h, #n);
    FORWARDS(LOAD)
    real_create_context = (void *(*)(void *, void *, void *, const int *))dlsym(h, "eglCreateContext");
    real_create_window_surface = (void *(*)(void *, void *, void *, const int *))dlsym(h, "eglCreateWindowSurface");
}
