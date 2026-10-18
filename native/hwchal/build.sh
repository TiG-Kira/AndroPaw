#!/bin/sh
# hwcomposer of the native arm64 engine (see hwc.c): sh build.sh <kernel tag> builds it into that kernel's guest
# files as vendor/lib64/hw/hwcomposer.default.so. That kernel's gralloc has to be the engine's (native/grallocwrap).
TAG=${1:?usage: sh build.sh <kernel tag>}
NDK=${NDK:-$ANDROID_NDK_HOME}
BIN="$NDK/toolchains/llvm/prebuilt/windows-x86_64/bin"
[ -x "$BIN/clang" ] || BIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
OUT="../../app/src/main/assets/engines/n64-$TAG/vendor#lib64#hw#hwcomposer.default.so"
T=${TMPDIR:-${TEMP:-/tmp}}/aemu-hwc; mkdir -p "$T"
# the NDK has no libhardware to link against: a stand-in with the one entry used
echo "int hw_get_module(const char *id, const void **m) { (void)id; (void)m; return -1; }" > "$T/hw.c"
"$BIN/clang" --target=aarch64-linux-android26 -shared -nostdlib -Wl,-soname,libhardware.so -o "$T/libhardware.so" "$T/hw.c" || exit 1
"$BIN/clang" --target=aarch64-linux-android26 -O2 -fPIC -shared \
  -nostdlib -fno-builtin -fvisibility=hidden -fno-stack-protector \
  -Wl,-z,norelro -Wl,--build-id=none -Wl,--no-undefined -Wl,-soname,hwcomposer.default.so \
  -o "$OUT" hwc.c -L"$T" -lhardware -lc || exit 1
"$BIN/llvm-strip" "$OUT" 2>/dev/null || true
