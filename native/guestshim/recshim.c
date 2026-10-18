/*
 * librecshim.so — preloaded into a dynamic recovery (TWRP, OrangeFox…) running in recovery mode.
 *
 * 1. mount() is not possible inside an app sandbox. We return success (0).
 *    TWRP checks "is it mounted" by comparing st_dev of <mount point>/. and <mount point>/../. —
 *    equal, so it mounts recursively. For mount points in AEMU_MOUNTS ("/sdcard:/data:…")
 *    stat(), stat64(), lstat(), lstat64(), fstatat() report another device ID via XOR 0x5a5a.
 * 2. /proc/mounts and /proc/self/mounts are redirected to /tmp/aemu_mounts where the host
 *    sets up mounted entries for /system, /data, /cache, /sdcard.
 * 3. Linux evdev ioctl support: minui, TWRP and OrangeFox query input devices (/dev/input/event0)
 *    via ioctl(EVIOCGBIT, EVIOCGABS, EVIOCGPROP, EVIOCGNAME). When event0 is a FIFO pipe,
 *    standard ioctls fail with ENOTTY, causing recovery to ignore touch input.
 *    We intercept and answer all evdev ioctls with touchscreen geometry matching DHD_IN_W/H.
 * 4. reboot()/__reboot()/android_reboot() go to the host through /dev/aemu_power.
 */
#define EXPORT __attribute__((visibility("default")))

extern char **environ;
extern int *__errno(void);

__attribute__((naked, noinline)) static long sys3(long n, long a, long b, long c) {
    __asm__ volatile("push {r7}; mov r7, r0; mov r0, r1; mov r1, r2; mov r2, r3; svc #0; pop {r7}; bx lr");
}

__attribute__((naked, noinline)) static long sys4(long n, long a, long b, long c, long d) {
    __asm__ volatile(
        "push {r7}\n"
        "mov r7, r0\n"
        "mov r0, r1\n"
        "mov r1, r2\n"
        "mov r2, r3\n"
        "ldr r3, [sp, #4]\n"
        "svc #0\n"
        "pop {r7}\n"
        "bx lr\n"
    );
}

static int fail(long r) { *__errno() = (int)-r; return -1; }

static const char *env(const char *key) {
    for (char **e = environ; e && *e; e++) {
        const char *a = *e, *k = key;
        while (*k && *a == *k) { a++; k++; }
        if (!*k && *a == '=') return a + 1;
    }
    return 0;
}

static int get_env_int(const char *key, int def) {
    const char *v = env(key);
    if (!v || !*v) return def;
    int val = 0;
    while (*v >= '0' && *v <= '9') {
        val = val * 10 + (*v - '0');
        v++;
    }
    return val > 0 ? val : def;
}

/* p is exactly "<mount point>/." for one of the AEMU_MOUNTS entries */
static int mount_dot(const char *p) {
    const char *list = env("AEMU_MOUNTS");
    if (!list || !p) return 0;
    int n = 0;
    while (p[n]) n++;
    if (n < 3 || p[n - 1] != '.' || p[n - 2] != '/') return 0;
    int len = n - 2;
    const char *s = list;
    while (*s) {
        const char *e = s;
        while (*e && *e != ':') e++;
        if (e - s == len) {
            int i = 0;
            while (i < len && s[i] == p[i]) i++;
            if (i == len) return 1;
        }
        s = *e ? e + 1 : e;
    }
    return 0;
}

EXPORT int stat(const char *path, void *st) {
    long r = sys3(195 /* stat64 */, (long)path, (long)st, 0);
    if (r < 0) return fail(r);
    if (mount_dot(path)) *(unsigned long long *)st ^= 0x5a5aULL;   /* st_dev: "another filesystem" */
    return 0;
}
EXPORT int stat64(const char *path, void *st) { return stat(path, st); }

EXPORT int lstat(const char *path, void *st) {
    long r = sys3(196 /* lstat64 */, (long)path, (long)st, 0);
    if (r < 0) return fail(r);
    if (mount_dot(path)) *(unsigned long long *)st ^= 0x5a5aULL;
    return 0;
}
EXPORT int lstat64(const char *path, void *st) { return lstat(path, st); }

EXPORT int fstatat(int dirfd, const char *path, void *st, int flags) {
    long r = sys4(327 /* fstatat64 */, dirfd, (long)path, (long)st, flags);
    if (r < 0) return fail(r);
    if (mount_dot(path)) *(unsigned long long *)st ^= 0x5a5aULL;
    return 0;
}
EXPORT int fstatat64(int dirfd, const char *path, void *st, int flags) {
    return fstatat(dirfd, path, st, flags);
}

static int is_mounts_file(const char *path) {
    if (!path) return 0;
    if (path[0] == '/' && path[1] == 'p' && path[2] == 'r' && path[3] == 'o' && path[4] == 'c') {
        const char *p = path + 5;
        if (p[0] == '/' && p[1] == 'm' && p[2] == 'o' && p[3] == 'u' && p[4] == 'n' && p[5] == 't' && p[6] == 's' && p[7] == '\0') return 1;
        if (p[0] == '/' && p[1] == 's' && p[2] == 'e' && p[3] == 'l' && p[4] == 'f' &&
            p[5] == '/' && p[6] == 'm' && p[7] == 'o' && p[8] == 'u' && p[9] == 'n' && p[10] == 't' && p[11] == 's' && p[12] == '\0') return 1;
    }
    if (path[0] == '/' && path[1] == 'e' && path[2] == 't' && path[3] == 'c' && path[4] == '/' &&
        path[5] == 'm' && path[6] == 't' && path[7] == 'a' && path[8] == 'b' && path[9] == '\0') return 1;
    return 0;
}

static const char *redirect_path(const char *path) {
    if (is_mounts_file(path)) return "/tmp/aemu_mounts";
    return path;
}

EXPORT int open(const char *path, int flags, int mode) {
    long r = sys3(5 /* open */, (long)redirect_path(path), flags, mode);
    if (r < 0) return fail(r);
    return (int)r;
}
EXPORT int open64(const char *path, int flags, int mode) { return open(path, flags, mode); }

EXPORT int openat(int dirfd, const char *path, int flags, int mode) {
    long r = sys4(322 /* openat */, dirfd, (long)redirect_path(path), flags, mode);
    if (r < 0) return fail(r);
    return (int)r;
}
EXPORT int openat64(int dirfd, const char *path, int flags, int mode) { return openat(dirfd, path, flags, mode); }

/*
 * Evdev ioctls for touchscreen emulation in minui / TWRP / OrangeFox
 */
struct aemu_input_id {
    unsigned short bustype;
    unsigned short vendor;
    unsigned short product;
    unsigned short version;
};

struct aemu_input_absinfo {
    int value;
    int minimum;
    int maximum;
    int fuzz;
    int flat;
    int resolution;
};

EXPORT int ioctl(int fd, unsigned long req, void *arg) {
    unsigned int type = (req >> 8) & 0xFF;
    if (type == 'E') { // Evdev 'E' ioctl family
        unsigned int nr = req & 0xFF;
        unsigned int size = (req >> 16) & 0x3FFF;

        if (nr == 0x01) { // EVIOCGVERSION
            if (arg && size >= 4) { *(int *)arg = 0x010001; return 0; }
        } else if (nr == 0x02) { // EVIOCGID
            if (arg && size >= sizeof(struct aemu_input_id)) {
                struct aemu_input_id *id = (struct aemu_input_id *)arg;
                id->bustype = 0x18; // BUS_VIRTUAL
                id->vendor = 0x18d1;
                id->product = 0x0001;
                id->version = 1;
                return 0;
            }
        } else if (nr == 0x06) { // EVIOCGNAME
            const char *name = "AEmulator Touchscreen";
            if (arg && size > 0) {
                char *d = (char *)arg;
                int i = 0;
                while (i < (int)size - 1 && name[i]) { d[i] = name[i]; i++; }
                d[i] = '\0';
                return i;
            }
            return 0;
        } else if (nr == 0x07) { // EVIOCGPHYS
            const char *phys = "aemu/input0";
            if (arg && size > 0) {
                char *d = (char *)arg;
                int i = 0;
                while (i < (int)size - 1 && phys[i]) { d[i] = phys[i]; i++; }
                d[i] = '\0';
                return i;
            }
            return 0;
        } else if (nr == 0x08) { // EVIOCGUNIQ
            if (arg && size > 0) { *(char *)arg = '\0'; return 0; }
        } else if (nr == 0x09) { // EVIOCGPROP
            if (arg && size > 0) {
                unsigned char *p = (unsigned char *)arg;
                for (unsigned int i = 0; i < size; i++) p[i] = 0;
                p[0] = 0x01; // INPUT_PROP_DIRECT
                return 0;
            }
        } else if (nr >= 0x20 && nr <= 0x3F) { // EVIOCGBIT(ev, len)
            unsigned int ev = nr - 0x20;
            if (arg && size > 0) {
                unsigned char *b = (unsigned char *)arg;
                for (unsigned int i = 0; i < size; i++) b[i] = 0;
                if (ev == 0) {
                    // EV_SYN (0), EV_KEY (1), EV_ABS (3)
                    b[0] = (1 << 0) | (1 << 1) | (1 << 3);
                } else if (ev == 1) { // EV_KEY
                    // BTN_TOUCH = 330: byte 41, bit 2
                    // KEY_POWER = 116, VOLUMEDOWN = 114, VOLUMEUP = 115: byte 14, bits 4, 2, 3
                    if (size > 14) b[14] |= (1 << 2) | (1 << 3) | (1 << 4);
                    if (size > 41) b[41] |= (1 << 2);
                } else if (ev == 3) { // EV_ABS
                    // ABS_X = 0, ABS_Y = 1: byte 0, bits 0, 1
                    // ABS_PRESSURE = 24: byte 3, bit 0
                    // ABS_MT_SLOT = 47: byte 5, bit 7
                    // ABS_MT_TOUCH_MAJOR = 48: byte 6, bit 0
                    // ABS_MT_POSITION_X = 53: byte 6, bit 5
                    // ABS_MT_POSITION_Y = 54: byte 6, bit 6
                    // ABS_MT_TRACKING_ID = 57: byte 7, bit 1
                    b[0] |= (1 << 0) | (1 << 1);
                    if (size > 3) b[3] |= (1 << 0);
                    if (size > 5) b[5] |= (1 << 7);
                    if (size > 6) b[6] |= (1 << 0) | (1 << 5) | (1 << 6);
                    if (size > 7) b[7] |= (1 << 1);
                }
                return 0;
            }
        } else if (nr >= 0x40 && nr <= 0x7F) { // EVIOCGABS(abs)
            unsigned int abs = nr - 0x40;
            if (arg && size >= sizeof(struct aemu_input_absinfo)) {
                struct aemu_input_absinfo *info = (struct aemu_input_absinfo *)arg;
                for (unsigned int i = 0; i < sizeof(struct aemu_input_absinfo); i++) ((char *)info)[i] = 0;
                int w = get_env_int("DHD_IN_W", get_env_int("DHD_FB_W", 1080));
                int h = get_env_int("DHD_IN_H", get_env_int("DHD_FB_H", 1920));
                if (abs == 0 /* ABS_X */ || abs == 0x35 /* ABS_MT_POSITION_X */) {
                    info->minimum = 0;
                    info->maximum = w > 0 ? w - 1 : 1079;
                } else if (abs == 1 /* ABS_Y */ || abs == 0x36 /* ABS_MT_POSITION_Y */) {
                    info->minimum = 0;
                    info->maximum = h > 0 ? h - 1 : 1919;
                } else if (abs == 0x39 /* ABS_MT_TRACKING_ID */) {
                    info->minimum = 0;
                    info->maximum = 65535;
                } else if (abs == 0x2f /* ABS_MT_SLOT */) {
                    info->minimum = 0;
                    info->maximum = 9;
                } else if (abs == 0x30 /* ABS_MT_TOUCH_MAJOR */ || abs == 0x18 /* ABS_PRESSURE */) {
                    info->minimum = 0;
                    info->maximum = 255;
                }
                return 0;
            }
        }
        return 0;
    }
    long r = sys3(54 /* ioctl */, fd, req, (long)arg);
    if (r < 0) return fail(r);
    return (int)r;
}

static void power_request(const char *what, const char *arg) {
    char buf[96]; int n = 0;
    while (*what && n < 40) buf[n++] = *what++;
    if (arg && *arg) { buf[n++] = ','; while (*arg && n < 90) buf[n++] = *arg++; }
    long fd = sys3(5 /* open */, (long)"/dev/aemu_power", 0x241 /* O_WRONLY|O_CREAT|O_TRUNC */, 0666);
    if (fd >= 0) { sys3(4 /* write */, fd, (long)buf, n); sys3(6 /* close */, fd, 0, 0); }
}
EXPORT int reboot(int cmd) {
    power_request(cmd == 0x4321FEDC || cmd == (int)0xCDEF0123 ? "shutdown" : "reboot", 0);
    return 0;
}
EXPORT int __reboot(int m1, int m2, int cmd, void *arg) {
    (void)m1; (void)m2;
    if (cmd == (int)0xA1B2C3D4) power_request("reboot", (const char *)arg); else reboot(cmd);
    return 0;
}
EXPORT int android_reboot(int cmd, int flags, char *arg) {
    (void)flags;
    power_request(cmd == (int)0xDEAD0002 ? "shutdown" : "reboot", cmd == (int)0xDEAD0003 ? arg : 0);
    return 0;
}

/* the app sandbox kills the process on umount (seccomp); partitions are the host's folders anyway */
EXPORT int mount(const char *src, const char *target, const char *type, unsigned long flags, const void *data) {
    (void)src; (void)target; (void)type; (void)flags; (void)data; return 0;
}
EXPORT int umount(const char *target) { (void)target; return 0; }
EXPORT int umount2(const char *target, int flags) { (void)target; (void)flags; return 0; }
EXPORT int swapon(const char *path, int swapflags) { (void)path; (void)swapflags; return 0; }
EXPORT int swapoff(const char *path) { (void)path; return 0; }
