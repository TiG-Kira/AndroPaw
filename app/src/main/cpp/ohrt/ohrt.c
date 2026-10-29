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
#include <linux/fcntl.h>
#include <poll.h>
#include <pthread.h>
#include <signal.h>
#include <stdarg.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/un.h>
#include <sys/wait.h>
#include <time.h>
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

/* --------------------------------------------------- display bridge (Ohspace::Ui) */

/*
 * Protocol recovered from the runtime's libcomposer.z.so (docs/oh-protocol.md):
 * the guest LISTENS on <rootfs>/ohspace/run/display.sock (SOCK_SEQPACKET), the
 * host connects, sends a 24-byte HELLO and receives a 64-byte CONFIG carrying
 * two fds over SCM_RIGHTS — a sealed memfd holding a triple-buffered frame
 * ring, and the eventfd the guest writes after every Publish.
 */

#define OH_KIND_HELLO  1
#define OH_KIND_CONFIG 2
#define OH_SLOT_STATE_BIT_R (1ULL << 62)
#define OH_SLOT_STATE_BIT_W (1ULL << 63)

struct oh_disp_cfg {
    uint32_t w, h, dpi, fps, orientation;
    uint64_t session;
};

/* Write const.ohspace.display.* into the guest's parameter area. Both sides
 * (this process here, the guest's GetParameter there) read the same file, so
 * the session id and the frame geometry stay in lockstep. Never overwrites a
 * parameter file the runtime package ships on its own. */
static int write_display_params(const char *rootfs, const struct oh_disp_cfg *c) {
    char path[300], dir[300];
    snprintf(dir, sizeof(dir), "%s/system/etc/param", rootfs);
    snprintf(path, sizeof(path), "%s/ohspace.para", dir);
    struct stat st;
    if (stat(path, &st) == 0) return 0;
    mkdirs_deep(dir);
    FILE *f = fopen(path, "w");
    if (!f) return -1;
    fprintf(f, "# AndroPaw display bridge parameters\n");
    fprintf(f, "const.ohspace.display.width=%u\n", c->w);
    fprintf(f, "const.ohspace.display.height=%u\n", c->h);
    fprintf(f, "const.ohspace.display.dpi=%u\n", c->dpi);
    fprintf(f, "const.ohspace.display.fps=%u\n", c->fps);
    fprintf(f, "const.ohspace.display.orientation=%u\n", c->orientation);
    fprintf(f, "const.ohspace.display.session=%llu\n", (unsigned long long)c->session);
    fprintf(f, "const.ohspace.display.host_pid=%d\n", (int)getpid());
    fclose(f);
    return 0;
}

static int read_display_params(const char *rootfs, struct oh_disp_cfg *c) {
    char path[300];
    snprintf(path, sizeof(path), "%s/system/etc/param/ohspace.para", rootfs);
    FILE *f = fopen(path, "r");
    if (!f) return -1;
    char line[256];
    while (fgets(line, sizeof(line), f)) {
        char *nl = strpbrk(line, "\r\n");
        if (nl) *nl = 0;
        char *eq = strchr(line, '=');
        if (!eq) continue;
        *eq = 0;
        const char *key = line, *val = eq + 1;
        while (*val == ' ') val++;
        if      (!strcmp(key, "const.ohspace.display.width"))       c->w = (uint32_t)strtoul(val, NULL, 0);
        else if (!strcmp(key, "const.ohspace.display.height"))      c->h = (uint32_t)strtoul(val, NULL, 0);
        else if (!strcmp(key, "const.ohspace.display.dpi"))         c->dpi = (uint32_t)strtoul(val, NULL, 0);
        else if (!strcmp(key, "const.ohspace.display.fps"))         c->fps = (uint32_t)strtoul(val, NULL, 0);
        else if (!strcmp(key, "const.ohspace.display.orientation")) c->orientation = (uint32_t)strtoul(val, NULL, 0);
        else if (!strcmp(key, "const.ohspace.display.session"))     c->session = strtoull(val, NULL, 0);
    }
    fclose(f);
    return c->session ? 0 : -1;
}

static void blit_frame(const uint8_t *src, uint32_t stride, uint32_t w, uint32_t h,
                       uint8_t *dst, int dst_pitch, int dw, int dh) {
    if (dw == (int)w && dh == (int)h) {
        for (uint32_t r = 0; r < h; r++)
            memcpy(dst + (size_t)r * (size_t)dst_pitch, src + (size_t)r * stride,
                   (size_t)w * 4);
        return;
    }
    /* nearest-neighbour scale: the guest frame and the window rarely agree */
    for (int y = 0; y < dh; y++) {
        uint32_t sy = (uint32_t)(((uint64_t)y * h) / (uint32_t)dh);
        if (sy >= h) sy = h - 1;
        const uint8_t *srow = src + (size_t)sy * stride;
        uint32_t *d = (uint32_t *)(dst + (size_t)y * (size_t)dst_pitch);
        for (int x = 0; x < dw; x++) {
            uint32_t sx = (uint32_t)(((uint64_t)x * w) / (uint32_t)dw);
            if (sx >= w) sx = w - 1;
            memcpy(&d[x], srow + (size_t)sx * 4, 4);
        }
    }
}

/* FrameRing::Acquire + CopyLatest, per docs/oh-protocol.md §6/§7: the metadata
 * page holds latest=(seq<<2)|slot and per-slot descriptors (state, pts). A
 * reader CASes state seq→seq|bit62, copies, then releases the slot back to 0.
 * Requires a strictly increasing seq so a slow reader never repaints old data. */
static int ring_acquire_copy(uint8_t *map, uint32_t page, uint64_t ring,
                             uint32_t stride, uint32_t w, uint32_t h,
                             uint64_t *last_seq,
                             uint8_t *dst, int dst_pitch, int dw, int dh) {
    uint64_t latest = __atomic_load_n((uint64_t *)map, __ATOMIC_ACQUIRE);
    uint64_t seq = latest >> 2, slot = latest & 3;
    if (seq == 0 || slot > 2 || seq <= *last_seq) return 0;
    uint64_t *state = (uint64_t *)(map + 0x40 + 64 * slot);
    uint64_t cur = __atomic_load_n(state, __ATOMIC_ACQUIRE);
    if (cur != seq) return 0;
    uint64_t want = seq | OH_SLOT_STATE_BIT_R;
    if (!__atomic_compare_exchange_n(state, &cur, want, 0,
                                     __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE)) return 0;
    blit_frame(map + page + ring * slot, stride, w, h, dst, dst_pitch, dw, dh);
    *last_seq = seq;
    cur = seq;
    __atomic_compare_exchange_n(state, &cur, 0, 0, __ATOMIC_RELEASE, __ATOMIC_RELAXED);
    return 1;
}

static volatile int g_disp_run = 0;
static int g_disp_sock = -1;
static pthread_t g_disp_thread;
static int g_disp_up = 0;

static void *display_thread_fn(void *arg) {
    char *rootfs = arg;
    struct oh_disp_cfg cfg = { 720, 1280, 655, 60, 0, 0 };
    if (read_display_params(rootfs, &cfg) != 0) {
        rlog("display bridge: no const.ohspace.display.session in %s — bridge idle", rootfs);
        free(rootfs);
        return NULL;
    }
    char path[300];
    snprintf(path, sizeof(path), "%s/ohspace/run/display.sock", rootfs);
    free(rootfs);
    if (strlen(path) >= 108) {
        rlog("display bridge: socket path too long (%zu bytes, unix max 108): %s",
             strlen(path), path);
        return NULL;
    }

    /* the guest's OhspaceDisplayOutput::Start() listens here once init is up */
    int s = -1;
    while (!g_stopping && g_disp_run) {
        s = socket(AF_UNIX, SOCK_SEQPACKET | SOCK_CLOEXEC, 0);
        if (s < 0) break;
        struct sockaddr_un addr;
        memset(&addr, 0, sizeof(addr));
        addr.sun_family = AF_UNIX;
        strncpy(addr.sun_path, path, sizeof(addr.sun_path) - 1);
        if (connect(s, (struct sockaddr *)&addr, sizeof(addr)) == 0) break;
        close(s);
        s = -1;
        usleep(500 * 1000);
    }
    if (s < 0) {
        if (!g_stopping) rlog("display bridge: never connected to %s", path);
        return NULL;
    }
    pthread_mutex_lock(&g_lock);
    g_disp_sock = s;
    pthread_mutex_unlock(&g_lock);
    rlog("display bridge: connected to %s", path);

    /* HELLO: {kind=1, len=24, session, pad8=0} */
    uint8_t hello[24];
    memset(hello, 0, sizeof(hello));
    uint32_t k32 = OH_KIND_HELLO, l32 = (uint32_t)sizeof(hello);
    memcpy(hello + 0, &k32, 4);
    memcpy(hello + 4, &l32, 4);
    memcpy(hello + 8, &cfg.session, 8);
    if (send(s, hello, sizeof(hello), MSG_NOSIGNAL) != (ssize_t)sizeof(hello)) {
        rlog("display bridge: HELLO failed: %s", strerror(errno));
        goto out;
    }

    /* CONFIG: one SEQPACKET packet, fds via SCM_RIGHTS */
    uint8_t buf[512];
    struct iovec iov = { buf, sizeof(buf) };
    union { struct cmsghdr h; char c[256]; } u;
    struct msghdr mh;
    memset(&mh, 0, sizeof(mh));
    mh.msg_iov = &iov;
    mh.msg_iovlen = 1;
    mh.msg_control = u.c;
    mh.msg_controllen = sizeof(u.c);
    ssize_t n = recvmsg(s, &mh, 0);
    if (n < 64) {
        rlog("display bridge: CONFIG too short (%zd bytes): %s", n,
             n < 0 ? strerror(errno) : "peer closed");
        goto out;
    }
    uint32_t rkind, rlen;
    uint64_t rsession;
    memcpy(&rkind, buf + 0, 4);
    memcpy(&rlen, buf + 4, 4);
    memcpy(&rsession, buf + 8, 8);
    if (rkind != OH_KIND_CONFIG || rsession != cfg.session) {
        rlog("display bridge: unexpected CONFIG (kind=%u session=%llu, want session=%llu)",
             rkind, (unsigned long long)rsession, (unsigned long long)cfg.session);
        goto out;
    }

    int ring_fd = -1, wake_fd = -1;
    for (struct cmsghdr *cm = CMSG_FIRSTHDR(&mh); cm; cm = CMSG_NXTHDR(&mh, cm)) {
        if (cm->cmsg_level != SOL_SOCKET || cm->cmsg_type != SCM_RIGHTS) continue;
        int *fds = (int *)CMSG_DATA(cm);
        int nf = (int)((cm->cmsg_len - CMSG_LEN(0)) / sizeof(int));
        for (int i = 0; i < nf; i++) {
            struct stat fst;
            /* the ring is a regular sealed memfd; the wake fd is anonymous */
            if (fstat(fds[i], &fst) == 0 && S_ISREG(fst.st_mode)) {
                if (ring_fd < 0) ring_fd = fds[i]; else close(fds[i]);
            } else {
                if (wake_fd < 0) wake_fd = fds[i]; else close(fds[i]);
            }
        }
    }
    if (ring_fd < 0 || wake_fd < 0) {
        rlog("display bridge: CONFIG carried no usable fds (ring=%d wake=%d)", ring_fd, wake_fd);
        if (ring_fd >= 0) close(ring_fd);
        if (wake_fd >= 0) close(wake_fd);
        goto out;
    }

    /* FrameInfo starts right behind the 16-byte header */
    uint32_t stride, page;
    uint64_t ring;
    memcpy(&stride, buf + 16 + 0x20, 4);
    memcpy(&page, buf + 16 + 0x24, 4);
    memcpy(&ring, buf + 16 + 0x28, 8);
    if (page < 256 || (page & (page - 1)) || ring == 0 || stride < 4 * cfg.w) {
        rlog("display bridge: bad FrameInfo (stride=%u page=%u ring=%llu)",
             stride, page, (unsigned long long)ring);
        close(ring_fd);
        close(wake_fd);
        goto out;
    }
    size_t total = (size_t)3 * (size_t)ring + page;
    struct stat rst;
    if (fstat(ring_fd, &rst) != 0 || (size_t)rst.st_size < total) {
        rlog("display bridge: memfd too small (%lld < %zu)", (long long)rst.st_size, total);
        close(ring_fd);
        close(wake_fd);
        goto out;
    }
#ifdef F_GET_SEALS
    long seals = fcntl(ring_fd, F_GET_SEALS);
    if (seals >= 0 && seals != (F_SEAL_SEAL | F_SEAL_SHRINK | F_SEAL_GROW)) {
        rlog("display bridge: unexpected memfd seals %#lx — accepting anyway", seals);
    }
#endif
    uint8_t *map = mmap(NULL, total, PROT_READ | PROT_WRITE, MAP_SHARED, ring_fd, 0);
    close(ring_fd);
    if (map == MAP_FAILED) {
        rlog("display bridge: mmap ring failed: %s", strerror(errno));
        close(wake_fd);
        goto out;
    }

    pthread_mutex_lock(&g_lock);
    ANativeWindow *win = g_window;
    pthread_mutex_unlock(&g_lock);
    if (win) ANativeWindow_setBuffersGeometry(win, 0, 0, WINDOW_FORMAT_RGBA_8888);
    rlog("display bridge: %ux%u @%ufps, ring %llu bytes, stride %u — streaming",
         cfg.w, cfg.h, cfg.fps, (unsigned long long)ring, stride);

    uint64_t last_seq = 0;
    while (!g_stopping && g_disp_run) {
        struct pollfd pfd = { wake_fd, POLLIN, 0 };
        int pr = poll(&pfd, 1, 1000);
        if (pr > 0 && (pfd.revents & POLLIN)) {
            uint64_t v;
            (void)read(wake_fd, &v, sizeof(v));
        }
        if (pr < 0 && errno == EINTR) continue;
        pthread_mutex_lock(&g_lock);
        ANativeWindow *w2 = g_window;
        if (w2) ANativeWindow_acquire(w2);
        pthread_mutex_unlock(&g_lock);
        if (!w2) continue;
        ANativeWindow_Buffer b;
        if (ANativeWindow_lock(w2, &b, NULL) == 0) {
            ring_acquire_copy(map, page, ring, stride, cfg.w, cfg.h, &last_seq,
                              (uint8_t *)b.bits, (int)b.stride * 4, b.width, b.height);
            ANativeWindow_unlockAndPost(w2);
        }
        ANativeWindow_release(w2);
    }
    rlog("display bridge: closed");
    munmap(map, total);
    close(wake_fd);

out:
    pthread_mutex_lock(&g_lock);
    if (g_disp_sock == s) g_disp_sock = -1;
    pthread_mutex_unlock(&g_lock);
    close(s);
    return NULL;
}

static void display_start(const char *rootfs) {
    if (g_disp_up) return;
    g_disp_run = 1;
    char *arg = strdup(rootfs);
    if (!arg) { g_disp_run = 0; return; }
    if (pthread_create(&g_disp_thread, NULL, display_thread_fn, arg) != 0) {
        free(arg);
        g_disp_run = 0;
        return;
    }
    g_disp_up = 1;
}

static void display_stop(void) {
    if (!g_disp_up) return;
    g_disp_run = 0;
    pthread_mutex_lock(&g_lock);
    if (g_disp_sock >= 0) shutdown(g_disp_sock, SHUT_RDWR);
    pthread_mutex_unlock(&g_lock);
    pthread_join(g_disp_thread, NULL);
    g_disp_up = 0;
}

/* ---------------------------------------------------- input bridge (touch) */

/*
 * The guest MMI server (OhspaceInputAdapter in libmmi-server.z.so) also
 * LISTENS: /ohspace/run/input.sock, same Ohspace::Ui SEQPACKET transport.
 * HELLO (24B kind 1) makes it open its uinput devices; afterwards the host
 * streams 216-byte TouchMessages. ValidTouch's pointer-id bitmap check makes
 * count>1 impossible (ids must be unique yet all equal activeId), so every
 * message carries exactly one pointer. Coordinates are normalised [0,1];
 * action uses the Android MotionEvent enum.
 */

struct oh_touch_msg {
    uint32_t kind, len;        /* 5, 216 */
    uint64_t session;
    uint64_t event_time;       /* +0x10, must be nonzero */
    uint64_t down_time;        /* +0x18, must be ≥ time */
    uint64_t time;             /* +0x20 */
    uint32_t action;           /* +0x28, 0..6 (4 invalid) */
    uint32_t active_id;        /* +0x2c */
    uint32_t count;            /* +0x30, ≤ 10 */
    uint32_t reserved;
    struct { uint32_t id; float x, y, pressure; } p[10];   /* +0x38 */
};

static pthread_mutex_t g_input_lock = PTHREAD_MUTEX_INITIALIZER;
static int g_input_sock = -1;
static volatile int g_input_run = 0;
static volatile int g_input_ready = 0;
static uint64_t g_input_session = 0;
static pthread_t g_input_thread;

static void *input_thread_fn(void *arg) {
    char *rootfs = arg;
    struct oh_disp_cfg cfg = { 0, 0, 0, 0, 0, 0 };
    if (read_display_params(rootfs, &cfg) != 0) {
        rlog("input bridge: no session parameter — touch disabled");
        free(rootfs);
        return NULL;
    }
    char path[300];
    snprintf(path, sizeof(path), "%s/ohspace/run/input.sock", rootfs);
    free(rootfs);
    if (strlen(path) >= 108) {
        rlog("input bridge: socket path too long (%zu bytes)", strlen(path));
        return NULL;
    }

    int s = -1;
    while (!g_stopping && g_input_run) {
        s = socket(AF_UNIX, SOCK_SEQPACKET | SOCK_CLOEXEC, 0);
        if (s < 0) break;
        struct sockaddr_un addr;
        memset(&addr, 0, sizeof(addr));
        addr.sun_family = AF_UNIX;
        strncpy(addr.sun_path, path, sizeof(addr.sun_path) - 1);
        if (connect(s, (struct sockaddr *)&addr, sizeof(addr)) == 0) break;
        close(s);
        s = -1;
        usleep(500 * 1000);
    }
    if (s < 0) return NULL;

    uint8_t hello[24];
    memset(hello, 0, sizeof(hello));
    uint32_t k32 = OH_KIND_HELLO, l32 = (uint32_t)sizeof(hello);
    memcpy(hello + 0, &k32, 4);
    memcpy(hello + 4, &l32, 4);
    memcpy(hello + 8, &cfg.session, 8);
    if (send(s, hello, sizeof(hello), MSG_NOSIGNAL) != (ssize_t)sizeof(hello)) {
        rlog("input bridge: HELLO failed: %s", strerror(errno));
        close(s);
        return NULL;
    }
    pthread_mutex_lock(&g_input_lock);
    g_input_sock = s;
    g_input_session = cfg.session;
    g_input_ready = 1;
    pthread_mutex_unlock(&g_input_lock);
    rlog("input bridge: connected to %s — touch enabled", path);

    while (!g_stopping && g_input_run) usleep(200 * 1000);
    pthread_mutex_lock(&g_input_lock);
    g_input_ready = 0;
    if (g_input_sock == s) g_input_sock = -1;
    pthread_mutex_unlock(&g_input_lock);
    close(s);
    return NULL;
}

static void input_start(const char *rootfs) {
    if (g_input_run) return;
    g_input_run = 1;
    char *arg = strdup(rootfs);
    if (!arg) { g_input_run = 0; return; }
    if (pthread_create(&g_input_thread, NULL, input_thread_fn, arg) != 0) {
        free(arg);
        g_input_run = 0;
    }
}

static void input_stop(void) {
    if (!g_input_run) return;
    g_input_run = 0;
    pthread_join(g_input_thread, NULL);
    g_input_ready = 0;
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

/* Pick the guest entry binary, preferring a real init. */
static int find_exec(const char *rootfs, const char *exec, char *out, size_t outsz) {
    if (exec && *exec) {
        if (exec[0] == '/') snprintf(out, outsz, "%s%s", rootfs, exec);
        else snprintf(out, outsz, "%s/%s", rootfs, exec);
        return is_file(out) ? 0 : -1;
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

/* The guest path that corresponds to a host path inside the rootfs ("/system/bin/init"). */
static void guest_path_of(const char *rootfs, const char *exec_abs, char *out, size_t outsz) {
    size_t rl = strlen(rootfs);
    if (rl && !strncmp(exec_abs, rootfs, rl) && exec_abs[rl] == '/')
        snprintf(out, outsz, "%s", exec_abs + rl);
    else
        snprintf(out, outsz, "%s", exec_abs);
}

/*
 * Launch the guest through AndroPaw's own guest runner (libaemu64_<api>.so) — the same
 * launcher the Android machines use.
 *
 * Running the guest binary directly is not enough: an OpenHarmony userland is full of
 * absolute paths (/system/lib64, /data/service, /dev/unix/socket, /ohspace/run) and it opens
 * them from every library, not only through libc entry points we could interpose. The runner
 * loads the ELF itself, installs a seccomp filter and answers SIGSYS by rewriting *syscall*
 * path arguments under AEMU64_ROOT, so the guest gets its own /system and /data even though
 * it runs as an app process. It also answers mount/umount2/chroot/unshare/capset/setgroups
 * with success — the calls seccomp kills in an app, and the ones OpenHarmony's init makes in
 * its first job.
 */
static int spawn_guest_virt(const char *rootfs, const char *runner, const char *exec_guest,
                            const char *host_sock, const char *data_dir) {
    char tmpdir[300];
    snprintf(tmpdir, sizeof(tmpdir), "%s/tmp", data_dir);
    mkdirs_deep(tmpdir);

    static char e_root[600], e_api[600], e_tmp[400], e_ld[900];
    char *envp[16];
    int e = 0;
    snprintf(e_root, sizeof(e_root), "AEMU64_ROOT=%s", rootfs);
    snprintf(e_api, sizeof(e_api), "OHRT_HOST_API=%s", host_sock);
    snprintf(e_tmp, sizeof(e_tmp), "TMPDIR=%s", tmpdir);
    /* the guest's own files are owned by the app's uid; the guest must see them as root's */
    snprintf(e_ld, sizeof(e_ld), "LD_LIBRARY_PATH=/system/lib64:/system/lib");
    envp[e++] = e_root;
    envp[e++] = e_api;
    envp[e++] = e_tmp;
    envp[e++] = e_ld;
    envp[e++] = (char *)"AEMU64_ID=0,0";
    envp[e++] = (char *)"PATH=/system/bin:/bin:/sbin:/usr/bin";
    envp[e++] = (char *)"HOME=/root";
    envp[e] = NULL;

    /* aemu64 <argv0> <guest program> [args…]: the first argument after the runner is what the
     * guest sees as argv[0], the second is what gets loaded. */
    char *argv[4];
    int a = 0;
    argv[a++] = (char *)runner;
    argv[a++] = (char *)exec_guest;
    argv[a++] = (char *)exec_guest;
    argv[a] = NULL;

    pid_t pid = fork();
    if (pid < 0) return -1;
    if (pid == 0) {
        setsid();
        sigset_t mask;
        sigemptyset(&mask);
        sigprocmask(SIG_SETMASK, &mask, NULL);
        chdir(data_dir);
        execve(runner, argv, envp);
        LOGE("execve %s failed: %s", runner, strerror(errno));
        _exit(127);
    }
    return (int)pid;
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
    char loader_dir[256] = "", exec_rel[128] = "", host_sock[300] = "", runner[256] = "";
    jgetstr(json, "rootfs", rootfs, sizeof(rootfs));
    jgetstr(json, "dataDir", data_dir, sizeof(data_dir));
    jgetstr(json, "runDir", run_dir, sizeof(run_dir));
    /* the host-side guest launcher: libaemu64_<api>.so, resolved by OhEngine */
    jgetstr(json, "runner", runner, sizeof(runner));
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
    char exec_abs[400], loader_abs[400], exec_guest[400];
    if (find_exec(rootfs, exec_rel[0] ? exec_rel : NULL, exec_abs, sizeof(exec_abs)) != 0) {
        rlog("boot failed: no guest init found under %s (looked for /sbin/init, /bin/init, /system/bin/init)",
             rootfs);
        pthread_mutex_unlock(&g_lock);
        (*env)->ReleaseStringUTFChars(env, cfg, json);
        return -3;
    }
    guest_path_of(rootfs, exec_abs, exec_guest, sizeof(exec_guest));
    int have_loader = find_loader(rootfs, loader_dir[0] ? loader_dir : NULL,
                                  loader_abs, sizeof(loader_abs)) == 0;
    if (!have_loader && !(runner[0] && is_file(runner))) {
        rlog("boot failed: no musl loader (ld-musl-aarch64.so.1) and no guest runner to launch with");
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

    /* 2.5 display bridge parameters: prefer the attached surface's geometry,
     * within the limits the guest's ValidConfig enforces (w,h ∈ [320,2560],
     * w*h ≤ 4M, dpi < 120 or ≥ 641, fps ∈ {30,60,90,110,120}). */
    struct oh_disp_cfg dcfg = { 720, 1280, 655, 60, 0, 0 };
    if (g_window) {
        int ww = ANativeWindow_getWidth(g_window);
        int wh = ANativeWindow_getHeight(g_window);
        if (ww >= 320 && ww <= 2560 && wh >= 320 && wh <= 2560 &&
            (long long)ww * wh <= 4LL * 1024 * 1024) {
            dcfg.w = (uint32_t)ww;
            dcfg.h = (uint32_t)wh;
            dcfg.orientation = (wh >= ww) ? 0 : 1;
        }
    }
    dcfg.session = ((uint64_t)time(NULL) * 1000003u + (uint64_t)getpid()) | 1;
    if (write_display_params(rootfs, &dcfg) == 0)
        rlog("display parameters: %ux%u @%ufps session %llu",
             dcfg.w, dcfg.h, dcfg.fps, (unsigned long long)dcfg.session);
    else
        rlog("display parameters: could not write ohspace.para (bridge may stay idle)");

    /* 3. spawn the guest */
    g_stopping = 0;
    const char *sock = host_sock[0] ? host_sock : "/dev/null";
    int pid = -1;
    if (runner[0] && is_file(runner)) {
        /* the guest keeps its own path names: /system, /data and /dev are its own directories */
        pid = spawn_guest_virt(rootfs, runner, exec_guest, sock, data_dir);
        if (pid > 0) rlog("guest pid %d: %s via runner %s (guest root /)", pid, exec_guest, runner);
        else rlog("runner %s could not take %s, falling back to the musl loader", runner, exec_guest);
    }
    if (pid <= 0 && have_loader) {
        pid = spawn_guest(rootfs, loader_abs, exec_abs, sock, data_dir);
        if (pid > 0) rlog("guest pid %d: %s (loader %s)", pid, exec_abs, loader_abs);
    }
    if (pid <= 0) {
        rlog("boot failed: fork/exec error");
        api_stop();
        pthread_mutex_unlock(&g_lock);
        (*env)->ReleaseStringUTFChars(env, cfg, json);
        return -5;
    }
    g_child = pid;
    /* 4. bridges: connect to the guest's display.sock / input.sock once they listen */
    display_start(rootfs);
    input_start(rootfs);
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
    display_stop();
    input_stop();
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

static inline float clamp01(float v) {
    if (!(v >= 0.0f)) return 0.0f;   /* also catches NaN */
    return v > 1.0f ? 1.0f : v;
}

static jint oh_touch(JNIEnv *env, jclass cls, jint action, jfloat x, jfloat y,
                     jint pointer_id) {
    (void)env;
    (void)cls;
    pthread_mutex_lock(&g_input_lock);
    int ok = g_input_ready && g_input_sock >= 0 && !g_stopping;
    if (ok) {
        struct oh_touch_msg m;
        memset(&m, 0, sizeof(m));
        uint64_t now_us = 0;
        struct timespec ts;
        clock_gettime(CLOCK_MONOTONIC, &ts);
        now_us = (uint64_t)ts.tv_sec * 1000000u + (uint64_t)ts.tv_nsec / 1000u;
        uint32_t act = (uint32_t)action;
        if (act > 6 || act == 4) act = 3;   /* OUTSIDE never reaches the guest */
        m.kind = 5;
        m.len = (uint32_t)sizeof(m);
        m.session = g_input_session;
        m.event_time = now_us;
        m.down_time = now_us;
        m.time = now_us;
        m.action = act;
        m.active_id = (uint32_t)pointer_id;
        m.count = 1;
        m.p[0].id = (uint32_t)pointer_id;
        m.p[0].x = clamp01(x);
        m.p[0].y = clamp01(y);
        m.p[0].pressure = 1.0f;
        ok = send(g_input_sock, &m, sizeof(m), MSG_NOSIGNAL) == (ssize_t)sizeof(m);
    }
    pthread_mutex_unlock(&g_input_lock);
    return ok ? 0 : -1;
}

static const JNINativeMethod kMethods[] = {
    { "ohStart",   "(Ljava/lang/String;)I",                  (void *)oh_start },
    { "ohSurface", "(Landroid/view/Surface;)I",              (void *)oh_surface },
    { "ohTouch",   "(IFFI)I",                                (void *)oh_touch },
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
