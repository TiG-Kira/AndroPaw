/*
 * libnativewindow.so for Android 5.x guests. The library itself appeared in 8.0; SwiftShader (engines/n64-gl) lists
 * it among its dependencies, so an empty one stands in. On 5.x it also carries what SwiftShader takes from a newer
 * libc: stdin/stdout/stderr as symbols (5.x has only the __sF array) and __gnu_strerror_r (6.0).
 */
extern char __sF[];                       /* FILE[3]; sizeof(FILE) is 152 on LP64 bionic */
extern int strerror_r(int err, char *buf, unsigned long len);   /* the POSIX one on 5.x */

#define EXPORT __attribute__((visibility("default")))

EXPORT void *stdin, *stdout, *stderr;
EXPORT void aemu_nativewindow_stub(void) {}
EXPORT char *__gnu_strerror_r(int err, char *buf, unsigned long len) { strerror_r(err, buf, len); return buf; }

__attribute__((constructor)) static void init(void) { stdin = __sF; stdout = __sF + 152; stderr = __sF + 304; }
