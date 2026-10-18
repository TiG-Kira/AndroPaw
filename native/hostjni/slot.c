// Open-source replacement for libdhdslot.so
// Launches background slot worker (binderd / glserverd) inside the :binder and :gl services

#include <jni.h>
#include <unistd.h>
#include <fcntl.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <sys/wait.h>
#include <android/log.h>

#define TAG "AEMU_SLOT"

JNIEXPORT jint JNICALL
Java_dev_lk_dhd_Slot_runNative(JNIEnv *env, jclass cls,
                               jstring jwhich, jobjectArray jargv,
                               jobjectArray jenv, jstring jlog) {
    (void)cls;

    if (jlog) {
        const char *log_path = (*env)->GetStringUTFChars(env, jlog, NULL);
        if (log_path) {
            int fd = open(log_path, O_WRONLY | O_CREAT | O_APPEND, 0644);
            if (fd >= 0) {
                dup2(fd, STDOUT_FILENO);
                dup2(fd, STDERR_FILENO);
                if (fd > STDERR_FILENO) close(fd);
            }
            (*env)->ReleaseStringUTFChars(env, jlog, log_path);
        }
    }

    if (jenv) {
        jsize env_len = (*env)->GetArrayLength(env, jenv);
        for (jsize i = 0; i + 1 < env_len; i += 2) {
            jstring k = (jstring)(*env)->GetObjectArrayElement(env, jenv, i);
            jstring v = (jstring)(*env)->GetObjectArrayElement(env, jenv, i + 1);
            if (k && v) {
                const char *ks = (*env)->GetStringUTFChars(env, k, NULL);
                const char *vs = (*env)->GetStringUTFChars(env, v, NULL);
                if (ks && vs) setenv(ks, vs, 1);
                if (ks) (*env)->ReleaseStringUTFChars(env, k, ks);
                if (vs) (*env)->ReleaseStringUTFChars(env, v, vs);
            }
        }
    }

    if (!jwhich) return -1;
    const char *which = (*env)->GetStringUTFChars(env, jwhich, NULL);
    if (!which) return -1;

    // Build argv
    int argc = jargv ? (*env)->GetArrayLength(env, jargv) : 0;
    char **argv = (char **)calloc(argc + 2, sizeof(char *));
    argv[0] = strdup(which);
    for (int i = 0; i < argc; i++) {
        jstring arg = (jstring)(*env)->GetObjectArrayElement(env, jargv, i);
        if (arg) {
            const char *str = (*env)->GetStringUTFChars(env, arg, NULL);
            argv[i + 1] = str ? strdup(str) : NULL;
            if (str) (*env)->ReleaseStringUTFChars(env, arg, str);
        }
    }
    argv[argc + 1] = NULL;

    __android_log_print(ANDROID_LOG_INFO, TAG, "Running slot process: %s", which);

    // Fork and execute
    pid_t pid = fork();
    if (pid == 0) {
        execvp(which, argv);
        _exit(127);
    }

    int status = 0;
    if (pid > 0) {
        waitpid(pid, &status, 0);
    }

    for (int i = 0; i <= argc; i++) {
        if (argv[i]) free(argv[i]);
    }
    free(argv);
    (*env)->ReleaseStringUTFChars(env, jwhich, which);

    return WIFEXITED(status) ? WEXITSTATUS(status) : -1;
}
