#!/bin/sh
# One kernel of the native arm64 engine per Android API level: sh build.sh <tag> builds only native/guest64/<tag>
# (21, 22 = Android 5.x, 23 = 6.0, 24, 25 = 7.x, 26, 27 = 8.x, 28 = 9, 29 = 10, 30 = 11, 31 = 12, 33 = 13, 34 = 14, 35 = 15, 36 = 16, 37 = 17), so the other releases'
# binaries stay exactly as they were tested.
# aemu64: runs the firmware's own arm64 programs natively. Static, no libc, linked at a fixed address the
# seccomp filter recognises (syscalls made from this image pass, the guest's path/ioctl/id calls trap into it).
TAG=$1
[ -d "$(dirname "$0")/$TAG" ] && [ -n "$TAG" ] || { echo "usage: sh build.sh <tag>   (one of: $(cd "$(dirname "$0")" && ls -d [0-9]* | tr '\n' ' '))"; exit 1; }
cd "$(dirname "$0")/$TAG" || exit 1
NDK=${NDK:-$ANDROID_NDK_HOME}
BIN="$NDK/toolchains/llvm/prebuilt/windows-x86_64/bin"
[ -x "$BIN/clang" ] || BIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
DIR=${2:-../../../app/src/main/jniLibs/arm64-v8a}
OUT="$DIR/libaemu64_$TAG.so"
"$BIN/clang" --target=aarch64-linux-android26 -O2 -static -nostdlib -ffreestanding -fno-builtin -fno-stack-protector -mno-outline-atomics \
  -fno-pic -no-pie -Wall -Wno-unused-function -Wl,--image-base=0x2000000000 -Wl,--build-id=none -Wl,-z,norelro \
  -o "$OUT" start.S run64.c || exit 1
"$BIN/llvm-strip" "$OUT" 2>/dev/null || true
# binderd64: the binder driver in user space for those programs (an ordinary host program)
"$BIN/clang" --target=aarch64-linux-android26 -O2 -Wall -o "$DIR/libbinderd64_$TAG.so" binderd.c || exit 1
"$BIN/llvm-strip" "$DIR/libbinderd64_$TAG.so" 2>/dev/null || true
# a kernel with its own libnativewindow.so for the guest (5.x: SwiftShader's dependency plus the newer libc symbols)
if [ -f nativewindow_stub.c ]; then
  NW="../../../app/src/main/assets/engines/n64-$TAG/system#lib64#libnativewindow.so"
  "$BIN/clang" --target=aarch64-linux-android21 -Os -fPIC -shared -nostdlib -ffreestanding -fno-builtin -fvisibility=hidden \
    -fno-stack-protector -Wl,--build-id=none -Wl,-soname,libnativewindow.so -o "$NW" nativewindow_stub.c -lc || exit 1
  "$BIN/llvm-strip" "$NW" 2>/dev/null || true
fi
# a kernel with its own front for SwiftShader's libEGL (5.1: what a vendor's SurfaceFlinger asks of its GPU driver)
if [ -f egl_wrap.c ]; then
  A="../../../app/src/main/assets/engines"
  cp "$A/n64-gl/system#lib64#egl#libEGL_swiftshader.so" "$A/n64-$TAG/system#lib64#egl#libreal_swiftshader.so" || exit 1
  "$BIN/clang" --target=aarch64-linux-android21 -Os -fPIC -shared -nostdlib -ffreestanding -fno-builtin -fvisibility=hidden \
    -fno-stack-protector -Wl,--build-id=none -Wl,-soname,libEGL_swiftshader.so \
    -o "$A/n64-$TAG/system#lib64#egl#libEGL_swiftshader.so" egl_wrap.c -ldl -lc || exit 1
  "$BIN/llvm-strip" "$A/n64-$TAG/system#lib64#egl#libEGL_swiftshader.so" 2>/dev/null || true
fi
# a kernel with its own front for SwiftShader's libGLESv2 (5.1: shaders a vendor wrote against its GPU driver)
if [ -f gles_wrap.c ]; then
  A="../../../app/src/main/assets/engines"
  cp "$A/n64-gl/system#lib64#egl#libGLESv2_swiftshader.so" "$A/n64-$TAG/system#lib64#egl#libGLESv2real_swiftshader.so" || exit 1
  "$BIN/clang" --target=aarch64-linux-android21 -Os -fPIC -shared -nostdlib -ffreestanding -fno-builtin -fvisibility=hidden \
    -fno-stack-protector -Wl,--build-id=none -Wl,-soname,libGLESv2_swiftshader.so \
    -o "$A/n64-$TAG/system#lib64#egl#libGLESv2_swiftshader.so" gles_wrap.c -ldl -lc || exit 1
  "$BIN/llvm-strip" "$A/n64-$TAG/system#lib64#egl#libGLESv2_swiftshader.so" 2>/dev/null || true
fi
# a kernel with a gralloc that gives out handles laid out as a vendor's libgui expects (6.0: Samsung Exynos); both
# word sizes, kept outside hw/ — the tree fixer puts it in place where the firmware needs it
if [ -f gralloc_exynos.c ]; then
  A="../../../app/src/main/assets/engines/n64-$TAG"
  F="-Os -fPIC -shared -nostdlib -ffreestanding -fno-builtin -fvisibility=hidden -fno-stack-protector -Wl,--build-id=none -Wl,--no-undefined -Wl,-soname,gralloc.default.so"
  "$BIN/clang" --target=aarch64-linux-android21 $F -o "$A/system#lib64#libgralloc_exynos.so" gralloc_exynos.c -ldl -lc || exit 1
  "$BIN/clang" --target=armv7a-linux-androideabi21 -mthumb $F -Wl,--hash-style=both -o "$A/system#lib#libgralloc_exynos.so" gralloc_exynos.c -ldl -lc || exit 1
  "$BIN/llvm-strip" "$A/system#lib64#libgralloc_exynos.so" "$A/system#lib#libgralloc_exynos.so" 2>/dev/null || true
fi
# a kernel with a gralloc that gives out handles laid out as Qualcomm's SurfaceFlinger reads them (7.0: MIUI 8); both
# word sizes, kept outside hw/ - the tree fixer puts it in place where the firmware needs it
if [ -f gralloc_qcom.c ]; then
  A="../../../app/src/main/assets/engines/n64-$TAG"
  F="-Os -fPIC -shared -nostdlib -ffreestanding -fno-builtin -fvisibility=hidden -fno-stack-protector -Wl,--build-id=none -Wl,--no-undefined -Wl,-soname,gralloc.default.so"
  # (a Treble kernel's gralloc is on the vendor side: the source names the module it wraps there)
  P=system; grep -q '"/vendor/lib64/libgralloc_rom.so"' gralloc_qcom.c && P=vendor
  "$BIN/clang" --target=aarch64-linux-android21 $F -o "$A/$P#lib64#libgralloc_qcom.so" gralloc_qcom.c -ldl -lc || exit 1
  "$BIN/clang" --target=armv7a-linux-androideabi21 -mthumb $F -Wl,--hash-style=both -o "$A/$P#lib#libgralloc_qcom.so" gralloc_qcom.c -ldl -lc || exit 1
  "$BIN/llvm-strip" "$A/$P#lib64#libgralloc_qcom.so" "$A/$P#lib#libgralloc_qcom.so" 2>/dev/null || true
fi
