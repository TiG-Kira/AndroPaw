// Open-source ashmem shim for AEmulator
// Implements Android libcutils ashmem interface using memfd_create / /dev/ashmem fallback

#define _GNU_SOURCE
#include <sys/syscall.h>
#include <sys/mman.h>
#include <sys/ioctl.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <stddef.h>

#define ASHMEM_NAME_LEN 256
#define __ASHMEMIOC 0x77
#define ASHMEM_SET_NAME _IOW(__ASHMEMIOC, 1, char[ASHMEM_NAME_LEN])
#define ASHMEM_GET_NAME _IOR(__ASHMEMIOC, 2, char[ASHMEM_NAME_LEN])
#define ASHMEM_SET_SIZE _IOW(__ASHMEMIOC, 3, size_t)
#define ASHMEM_GET_SIZE _IO(__ASHMEMIOC, 4)
#define ASHMEM_SET_PROT_MASK _IOW(__ASHMEMIOC, 5, unsigned long)
#define ASHMEM_GET_PROT_MASK _IO(__ASHMEMIOC, 6)
#define ASHMEM_PIN _IOW(__ASHMEMIOC, 7, struct ashmem_pin)
#define ASHMEM_UNPIN _IOW(__ASHMEMIOC, 8, struct ashmem_pin)

#define ASHMEM_NOT_PURGED 0
#define ASHMEM_WAS_PURGED 1

#ifndef __NR_memfd_create
#if defined(__aarch64__)
#define __NR_memfd_create 279
#elif defined(__arm__)
#define __NR_memfd_create 385
#endif
#endif

__attribute__((visibility("default")))
int ashmem_create_region(const char *name, size_t size) {
    int fd = (int)syscall(__NR_memfd_create, name ? name : "ashmem", 1 /* MFD_CLOEXEC */);
    if (fd < 0) {
        fd = open("/dev/ashmem", O_RDWR | O_CLOEXEC);
        if (fd >= 0) {
            if (name) ioctl(fd, ASHMEM_SET_NAME, name);
            ioctl(fd, ASHMEM_SET_SIZE, size);
            return fd;
        }
        return -1;
    }
    if (ftruncate(fd, size) < 0) {
        close(fd);
        return -1;
    }
    return fd;
}

__attribute__((visibility("default")))
int ashmem_set_prot_region(int fd, int prot) {
    (void)fd; (void)prot;
    return 0;
}

__attribute__((visibility("default")))
int ashmem_get_size_region(int fd) {
    struct stat st;
    if (fstat(fd, &st) < 0) return -1;
    return (int)st.st_size;
}

__attribute__((visibility("default")))
int ashmem_pin_region(int fd, size_t offset, size_t len) {
    (void)fd; (void)offset; (void)len;
    return ASHMEM_NOT_PURGED;
}

__attribute__((visibility("default")))
int ashmem_unpin_region(int fd, size_t offset, size_t len) {
    (void)fd; (void)offset; (void)len;
    return 0;
}
