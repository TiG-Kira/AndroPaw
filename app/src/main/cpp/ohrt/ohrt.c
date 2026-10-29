/*
 * AndroPaw built-in OpenHarmony runner (libohrt.so).
 *
 * Approach: run a musl-based guest OS natively on the Android kernel — no CPU
 * emulation. The guest userland is dynamically linked against musl, so the
 * runner spawns the guest init through the musl dynamic loader shipped inside
 * the runtime package, hands it a host-bridge unix socket, and supervises it.
 *
 * Contract (mirrors the OhEngine.kt JSON config):
 *   OHRT_ROOT      guest rootfs
 *   OHRT_EXEC      guest entry (relative to rootfs), default /sbin/init
 *   OHRT_LOADER    directory holding ld-musl-aarch64.so.1
 *   OHRT_HOST_API  unix socket the guest agent connects back to
 *
 * Host API protocol: newline-terminated text commands
 *   ping            -> pong
 *   state           -> running|stopping
 *   log <msg>       -> appended to runDir/runner.log
 *   anything else   -> err unknown-command
 */
#include <jni.h>
#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <pthread.h>
#include <signal.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/un.h>
#include <sys/wait.h>
#include <unistd.h>

#define TAG "ohrt"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

/* ------------------------------------------------------------------ state */

static pthread_mutex_t g_lock = PTHREAD_MUTEX_INITIALIZER;
static pid_t g_child = 0;              /* guest process group */
static volatile int g_stopping = 0;
static int g_api_fd = -1;              /* host api listen socket */
static pthread_t g_api_thread;
static int g_api_up = 0;
static char g_run_dir[256] = "";
static char g_log_path[320] = "";
static ANativeWindow *g_window = NULL;
static char g_version[64] = "ohrt 0.1 built-in";

/* write() wrapper that survives partial writes for our small payloads */
static void wstr(int fd, const char *s) {
    size_t n = strlen(s);
    while (n > 0) {
        ssize_t w = write(fd, s, n);
        if (w <= 0) return;
        s += w;
        n -= (size_t)w;
    }
}

static void rlog(const char *fmt, ...) {
    char msg[512];
    va_list ap;
    va_start(ap, fmt);
    vsnprintf(msg, sizeof(msg), fmt, ap);
    va_end(ap);
    LOGI("%s", msg);
    if (g_log_path[0]) {
        int fd = open(g_log_path, O_WRONLY | O_CREAT | O_APPEND, 0644);
        if (fd >= 0) {
            time_t t = time(NULL);
            struct tm tmbuf;
            char ts[32], line[600];
            localtime_r(&t, &tmbuf);
            strftime(ts, sizeof(ts), "%m-%d %H:%M:%S", &tmbuf);
            int len = snprintf(line, sizeof(line), "[%s] %s\n", ts, msg);
            if (len > 0) wstr(fd, line);
            close(fd);
        }
    }
}

/* ------------------------------------------------- minimal JSON utilities */
/* Enough for the machine-generated compact config OhEngine sends: object
 * lookup by key, flat string values, plus one nesting level for "env". */

static const char *jskip(const char *p) {
    while (*p == ' ' || *p == '\t' || *p == '\n' || *p == '\r' || *p == ',')
        p++;
    return p;
}

/* Reads a JSON string starting at p (must point at '"'). Returns pointer just
 * past the closing quote, or NULL on error. out gets an unescaped copy. */
static const char *jstr(const char *p, char *out, size_t outsz) {
    if (*p != '"') return NULL;
    p++;
    size_t o = 0;
    while (*p && *p != '"') {
        char c = *p;
        if (c == '\\' && p[1]) {
            p++;
            switch (*p) {
                case 'n': c = '\n'; break;
                case 't': c = '\t'; break;
                case 'r': c = '\r'; break;
                case 'b': c = '\b'; break;
                case 'f': c = '\f'; break;
                case 'u': { /* \uXXXX: pass through for ASCII, '?' otherwise */
                    unsigned v = 0;
                    for (int i = 1; i <= 4 && p[i]; i++) {
                        char h = p[i];
                        v <<= 4;
                        if (h >= '0' && h <= '9') v |= (unsigned)(h - '0');
                        else if (h >= 'a' && h <= 'f') v |= (unsigned)(h - 'a' + 10);
                        else if (h >= 'A' && h <= 'F') v |= (unsigned)(h - 'A' + 10);
                        else return NULL;
                    }
                    p += 4;
                    c = (v < 128) ? (char)v : '?';
                    break;
                }
                default: c = *p; break;
            }
        }
        if (o + 1 < outsz) out[o++] = c;
        p++;
    }
    if (*p != '"') return NULL;
    out[o] = 0;
    return p + 1;
}

/* Find "key" inside the object at obj (must point at '{'). On success *vs and *vl
 * point at the raw value (string including quotes, or object/array/number). */
static const char *jfind(const char *obj, const char *key, size_t klen,
                         const char **vs, size_t *vl) {
    const char *p = jskip(obj + 1);
    while (*p && *p != '}') {
        char kbuf[128];
        p = jstr(p, kbuf, sizeof(kbuf));
        if (!p) return NULL;
        p = jskip(p);
        if (*p != ':') return NULL;
        p = jskip(p + 1);
        const char *vstart = p;
        size_t vlen = 0;
        if (*p == '"') {
            char dummy[512];
            p = jstr(p, dummy, sizeof(dummy));
            if (!p) return NULL;
            vlen = (size_t)(p - vstart);
        } else if (*p == '{' || *p == '[') {
            char open = *p, close = (*p == '{') ? '}' : ']';
            int depth = 0;
            while (*p) {
                if (*p == '"') { /* skip strings wholesale */
                    char dummy[512];
                    p = jstr(p, dummy, sizeof(dummy));
                    if (!p) return NULL;
                    continue;
                }
                if (*p == open) depth++;
                else if (*p == close) { depth--; p++; if (!depth) break; continue; }
                p++;
            }
            if (depth) return NULL;
            vlen = (size_t)(p - vstart);
        } else { /* number / true / false / null */
            while (*p && *p != ',' && *p != '}' && *p != ']') p++;
            vlen = (size_t)(p - vstart);
        }
        if (strlen(kbuf) == klen && !memcmp(kbuf, key, klen)) {
            *vs = vstart;
            *vl = vlen;
            return vstart;
        }
        p = jskip(p);
    }
    return NULL;
}

/* Extract a string value for key inside obj; returns 0 on success. */
static int jgetstr(const char *obj, const char *key, char *out, size_t outsz) {
    const char *vs, *p;
    size_t vl;
    if (!jfind(obj, key, strlen(key), &vs, &vl)) return -1;
    if (vl < 2 || vs[0] != '"') return -1;
    p = jstr(vs, out, outsz);
    return p ? 0 : -1;
}

/* ------------------------------------------------------------ fs helpers */

static int is_dir(const char *path) {
    struct stat st;
    return stat(path, &st) == 0 && S_ISDIR(st.st_mode);
}

static int is_file(const char *path) {
    struct stat st;
    return stat(path, &st) == 0 && S_ISREG(st.st_mode);
}

static void mkdirs(const char *path) {
    char tmp[256];
    snprintf(tmp, sizeof(tmp), "%s", path);
    for (char *p = tmp + 1; *p; p++) {
        if (*p == '/') {
            *p = 0;
            mkdir(tmp, 0755);
            *p = '/';
        }
    }
    mkdir(tmp, 0755);
}

static int mkdirs_deep(const char *path) {
    if (!path || !*path) return -1;
    mkdirs(path);
    return is_dir(path) ? 0 : -1;
}

/* ------------------------------------------------------- host api socket */

static void *api_thread_fn(void *arg) {
    (void)arg;
    char buf[1024];
    while (!g_stopping) {
        int c = accept(g_api_fd, NULL, NULL);
        if (c < 0) {
            if (errno == EINTR) continue;
            if (g_stopping) break;
            continue;
        }
        /* One request/response per line, then the peer decides to keep going. */
        while (!g_stopping) {
            ssize_t n = read(c, buf, sizeof(buf) - 1);
            if (n <= 0) break;
            buf[n] = 0;
            /* trim trailing newline / spaces */
            while (n > 0 && (buf[n - 1] == '\n' || buf[n - 1] == '\r' || buf[n - 1] == ' '))
                buf[--n] = 0;
            if (!n) continue;
            if (!strcmp(buf, "ping")) {
                wstr(c, "pong\n");
            } else if (!strcmp(buf, "state")) {
                char st[32];
                snprintf(st, sizeof(st), "%s\n", g_stopping ? "stopping" : "running");
                wstr(c, st);
            } else if (!strncmp(buf, "log ", 4)) {
                rlog("guest: %s", buf + 4);
                wstr(c, "ok\n");
            } else {
                wstr(c, "err unknown-command\n");
            }
        }
        close(c);
    }
    return NULL;
}

static int api_start(const char *sock_path) {
    unlink(sock_path);
    int fd = socket(AF_UNIX, SOCK_STREAM, 0);
    if (fd < 0) return -1;
    struct sockaddr_un addr;
    memset(&addr, 0, sizeof(addr));
    addr.sun_family = AF_UNIX;
    if (strlen(sock_path) >= sizeof(addr.sun_path)) { close(fd); return -1; }
    strncpy(addr.sun_path, sock_path, sizeof(addr.sun_path) - 1);
    if (bind(fd, (struct sockaddr *)&addr, sizeof(addr)) < 0) { close(fd); return -1; }
    chmod(sock_path, 0666); /* the guest uid (if any) must be able to connect */
    if (listen(fd, 4) < 0) { close(fd); unlink(sock_path); return -1; }
    g_api_fd = fd;
    if (pthread_create(&g_api_thread, NULL, api_thread_fn, NULL) != 0) {
        close(fd);
        unlink(sock_path);
        return -1;
    }
    g_api_up = 1;
    return 0;
}

static void api_stop(void) {
    if (!g_api_up) return;
    g_stopping = 1;
    shutdown(g_api_fd, SHUT_RDWR);
    close(g_api_fd);
    g_api_fd = -1;
    pthread_join(g_api_thread, NULL);
    if (g_run_dir[0]) {
        char p[300];
        snprintf(p, sizeof(p), "%s/host.sock", g_run_dir);
        unlink(p);
    }
    g_api_up = 0;
}

/* ------------------------------------------------------------- guest boot */

/* Locate the musl dynamic loader inside the runtime package. */
static int find_loader(const char *rootfs, const char *loader_dir,
                       char *out, size_t outsz) {
    static const char *cand[] = {
        "lib/ld-musl-aarch64.so.1",
        "lib/ld-musl-arm64.so.1",
        "system/lib/ld-musl-aarch64.so.1",
        "system/lib64/ld-musl-aarch64.so.1",
    };
    if (loader_dir && *loader_dir) {
        snprintf(out, outsz, "%s/ld-musl-aarch64.so.1", loader_dir);
        if (is_file(out)) return 0;
    }
    for (size_t i = 0; i < sizeof(cand) / sizeof(cand[0]); i++) {
        snprintf(out, outsz, "%s/%s", rootfs, cand[i]);
        if (is_file(out)) return 0;
    }
    return -1;
}

/* Pick the guest entry binary, preferring a real init. A configured path that is
 * absent must not abort the boot: fall through to the standard locations. */
static int find_exec(const char *rootfs, const char *exec, char *out, size_t outsz) {
    if (exec && *exec) {
        if (exec[0] == '/') snprintf(out, outsz, "%s%s", rootfs, exec);
        else snprintf(out, outsz, "%s/%s", rootfs, exec);
        if (is_file(out)) return 0;
    }
    static const char *cand[] = {
        "/sbin/init", "/bin/init", "/system/bin/init",
    };
    for (size_t i = 0; i < sizeof(cand) / sizeof(cand[0]); i++) {
        snprintf(out, outsz, "%s%s", rootfs, cand[i]);
        if (is_file(out)) return 0;
    }
    return -1;
}

static int spawn_guest(const char *rootfs, const char *loader, const char *exec,
                       const char *host_sock, const char *data_dir) {
    char libpath[768];
    snprintf(libpath, sizeof(libpath), "%s/lib:%s/system/lib64:%s/system/lib:%s",
             loader ? loader : "", rootfs, rootfs, rootfs);
    char tmpdir[300];
    snprintf(tmpdir, sizeof(tmpdir), "%s/tmp", data_dir);
    mkdirs_deep(tmpdir);

    char *envp[16];
    int e = 0;
    char e_root[600], e_exec[128], e_loader[600], e_api[600], e_lp[900], e_tmp[400];
    snprintf(e_root, sizeof(e_root), "OHRT_ROOT=%s", rootfs);
    snprintf(e_exec, sizeof(e_exec), "OHRT_EXEC=%s", exec);
    snprintf(e_loader, sizeof(e_loader), "OHRT_LOADER=%s", loader ? loader : "");
    snprintf(e_api, sizeof(e_api), "OHRT_HOST_API=%s", host_sock);
    snprintf(e_lp, sizeof(e_lp), "LD_LIBRARY_PATH=%s", libpath);
    snprintf(e_tmp, sizeof(e_tmp), "TMPDIR=%s", tmpdir);
    envp[e++] = e_root;
    envp[e++] = e_exec;
    envp[e++] = e_loader;
    envp[e++] = e_api;
    envp[e++] = e_lp;
    envp[e++] = e_tmp;
    envp[e++] = (char *)"PATH=/system/bin:/bin:/sbin:/usr/bin";
    envp[e++] = (char *)"HOME=/root";
    envp[e] = NULL;

    /* argv: the loader itself drives the guest binary */
    char *argv[5];
    int a = 0;
    argv[a++] = (char *)loader;
    argv[a++] = (char *)"--library-path";
    argv[a++] = libpath;
    argv[a++] = (char *)exec;
    argv[a] = NULL;

    pid_t pid = fork();
    if (pid < 0) return -1;
    if (pid == 0) {
        setsid();
        sigset_t mask;
        sigemptyset(&mask);
        sigprocmask(SIG_SETMASK, &mask, NULL);
        chdir(data_dir);
        execve(loader, argv, envp);
        LOGE("execve %s failed: %s", loader, strerror(errno));
        _exit(127);
    }
    return (int)pid;
}

/* ------------------------------------------------------------------- JNI */

static jint oh_start(JNIEnv *env, jclass cls, jstring cfg) {
    (void)cls;
    if (cfg == NULL) return -1;
    const char *json = (*env)->GetStringUTFChars(env, cfg, NULL);
    if (!json) return -1;

    char rootfs[256] = "", data_dir[256] = "", run_dir[256] = "";
    char loader_dir[256] = "", exec_rel[128] = "", host_sock[300] = "";
    jgetstr(json, "rootfs", rootfs, sizeof(rootfs));
    jgetstr(json, "dataDir", data_dir, sizeof(data_dir));
    jgetstr(json, "runDir", run_dir, sizeof(run_dir));
    /* nested env.* (one level) */
    const char *vs;
    size_t vl;
    if (jfind(json, "env", 3, &vs, &vl)) {
        char *envobj = malloc(vl + 1);
        memcpy(envobj, vs, vl);
        envobj[vl] = 0;
        jgetstr(envobj, "OHRT_EXEC", exec_rel, sizeof(exec_rel));
        jgetstr(envobj, "OHRT_LOADER", loader_dir, sizeof(loader_dir));
        jgetstr(envobj, "OHRT_HOST_API", host_sock, sizeof(host_sock));
        free(envobj);
    }

    pthread_mutex_lock(&g_lock);
    snprintf(g_run_dir, sizeof(g_run_dir), "%s", run_dir);
    if (run_dir[0]) {
        mkdirs_deep(run_dir);
        snprintf(g_log_path, sizeof(g_log_path), "%s/runner.log", run_dir);
    }

    /* 1. validate the runtime package layout */
    if (!rootfs[0] || !is_dir(rootfs)) {
        rlog("boot failed: rootfs missing (%s)", rootfs);
        pthread_mutex_unlock(&g_lock);
        (*env)->ReleaseStringUTFChars(env, cfg, json);
        return -2;
    }
    char exec_abs[400], loader_abs[400];
    if (find_exec(rootfs, exec_rel[0] ? exec_rel : NULL, exec_abs, sizeof(exec_abs)) != 0) {
        rlog("boot failed: no guest init found under %s (looked for /sbin/init, /bin/init, /system/bin/init)",
             rootfs);
        pthread_mutex_unlock(&g_lock);
        (*env)->ReleaseStringUTFChars(env, cfg, json);
        return -3;
    }
    if (find_loader(rootfs, loader_dir[0] ? loader_dir : NULL, loader_abs, sizeof(loader_abs)) != 0) {
        rlog("boot failed: no musl loader (ld-musl-aarch64.so.1) in the runtime package");
        pthread_mutex_unlock(&g_lock);
        (*env)->ReleaseStringUTFChars(env, cfg, json);
        return -4;
    }

    /* 2. host api bridge */
    if (!host_sock[0] && run_dir[0]) snprintf(host_sock, sizeof(host_sock), "%s/host.sock", run_dir);
    if (host_sock[0] && api_start(host_sock) == 0) {
        rlog("host api listening on %s", host_sock);
    } else {
        rlog("host api unavailable (continuing without bridge)");
        host_sock[0] = 0;
    }

    /* 3. spawn the guest */
    g_stopping = 0;
    int pid = spawn_guest(rootfs, loader_abs, exec_abs,
                          host_sock[0] ? host_sock : "/dev/null", data_dir);
    if (pid <= 0) {
        rlog("boot failed: fork/exec error");
        api_stop();
        pthread_mutex_unlock(&g_lock);
        (*env)->ReleaseStringUTFChars(env, cfg, json);
        return -5;
    }
    g_child = pid;
    rlog("guest pid %d: %s (loader %s)", pid, exec_abs, loader_abs);
    pthread_mutex_unlock(&g_lock);
    (*env)->ReleaseStringUTFChars(env, cfg, json);
    return 0;
}

static jint oh_surface(JNIEnv *env, jclass cls, jobject surface) {
    (void)cls;
    pthread_mutex_lock(&g_lock);
    if (g_window) {
        ANativeWindow_release(g_window);
        g_window = NULL;
    }
    if (surface) {
        g_window = ANativeWindow_fromSurface(env, surface);
        if (g_window) {
            rlog("surface attached %dx%d", ANativeWindow_getWidth(g_window),
                 ANativeWindow_getHeight(g_window));
        }
    }
    pthread_mutex_unlock(&g_lock);
    return g_window ? 0 : (surface ? -1 : 0);
}

static jint oh_stop(JNIEnv *env, jclass cls) {
    (void)env;
    (void)cls;
    pthread_mutex_lock(&g_lock);
    pid_t pid = g_child;
    g_child = 0;
    if (pid > 0) {
        rlog("stopping guest group %d", pid);
        kill(-pid, SIGTERM);
        kill(pid, SIGTERM);
    }
    pthread_mutex_unlock(&g_lock);
    api_stop();
    if (pid > 0) {
        /* give it a moment, then force */
        for (int i = 0; i < 10; i++) {
            if (waitpid(pid, NULL, WNOHANG) == pid) { pid = 0; break; }
            usleep(100 * 1000);
        }
        if (pid > 0) {
            kill(-pid, SIGKILL);
            kill(pid, SIGKILL);
            waitpid(pid, NULL, 0);
        }
    }
    pthread_mutex_lock(&g_lock);
    if (g_window) {
        ANativeWindow_release(g_window);
        g_window = NULL;
    }
    pthread_mutex_unlock(&g_lock);
    rlog("stopped");
    return 0;
}

static jstring oh_version(JNIEnv *env, jclass cls) {
    (void)cls;
    return (*env)->NewStringUTF(env, g_version);
}

static const JNINativeMethod kMethods[] = {
    { "ohStart",   "(Ljava/lang/String;)I",                  (void *)oh_start },
    { "ohSurface", "(Landroid/view/Surface;)I",              (void *)oh_surface },
    { "ohStop",    "()I",                                    (void *)oh_stop },
    { "ohVersion", "()Ljava/lang/String;",                  (void *)oh_version },
};

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    (void)reserved;
    JNIEnv *env;
    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_6) != JNI_OK) return JNI_ERR;
    jclass cls = (*env)->FindClass(env, "app/andropaw/openharmony/OhEngine$OhNative");
    if (!cls) {
        LOGE("OhNative class not found");
        return JNI_ERR;
    }
    if ((*env)->RegisterNatives(env, cls, kMethods,
                                sizeof(kMethods) / sizeof(kMethods[0])) != JNI_OK) {
        return JNI_ERR;
    }
    LOGI("ohrt runner ready");
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved) {
    (void)vm;
    (void)reserved;
    oh_stop(NULL, NULL);
}
