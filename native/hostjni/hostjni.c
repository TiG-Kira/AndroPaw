// Хост-помощник (arm64): ослабляет fdsan в процессе :vm.
// Закрытый libglbridge делает freopen() поверх fd 2, которым на Android 10+ владеет unique_fd,
// и fdsan роняет всё приложение. Переводим fdsan в режим «предупредить один раз».
#include <jni.h>
#include <dlfcn.h>
#include <sys/resource.h>

#ifndef ANDROID_FDSAN_ERROR_LEVEL_DISABLED
enum android_fdsan_error_level {
    ANDROID_FDSAN_ERROR_LEVEL_DISABLED = 0,
    ANDROID_FDSAN_ERROR_LEVEL_WARN_ONCE,
    ANDROID_FDSAN_ERROR_LEVEL_WARN_ALWAYS,
    ANDROID_FDSAN_ERROR_LEVEL_FATAL,
};
#endif

JNIEXPORT void JNICALL Java_app_aemu_core_HostNative_relaxFdsan(JNIEnv *env, jclass cls) {
    (void)env; (void)cls;
    enum android_fdsan_error_level (*set_level)(enum android_fdsan_error_level) =
        (enum android_fdsan_error_level (*)(enum android_fdsan_error_level))dlsym(RTLD_DEFAULT, "android_fdsan_set_error_level");
    if (set_level) {
        set_level(ANDROID_FDSAN_ERROR_LEVEL_DISABLED);
    }
}

// Гостевые процессы наследуют RLIMIT_STACK процесса :vm (у приложения он огромный). bionic 6.0 считает
// низ стека главного потока как «верх [stack] минус RLIMIT_STACK»; с лимитом ~2 ГБ ART получает
// нелепые границы стека и падает. 8 МБ — как у обычного процесса Android.
#include <sys/resource.h>
JNIEXPORT void JNICALL Java_app_aemu_core_HostNative_limitStack(JNIEnv *env, jclass cls) {
    (void)env; (void)cls;
    struct rlimit r;
    if (getrlimit(RLIMIT_STACK, &r) == 0 && (r.rlim_cur == RLIM_INFINITY || r.rlim_cur > 8u << 20)) {
        r.rlim_cur = 8u << 20;
        setrlimit(RLIMIT_STACK, &r);
    }
}
