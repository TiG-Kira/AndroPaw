#!/bin/sh
# libGLES_route.so (ARMv7; libc and libdl only)
NDK=${NDK:-$ANDROID_NDK_HOME}
BIN="$NDK/toolchains/llvm/prebuilt/windows-x86_64/bin"
[ -d "$BIN" ] || BIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
OUT=${1:-../../app/src/main/assets/engines/kk/libGLES_route.so}
SYSROOT="$(dirname "$BIN")/sysroot"
python gen.py || exit 1
"$BIN/clang" --target=armv7a-linux-androideabi21 -march=armv7-a -Os -fPIC -shared -nostdlib \
  -fvisibility=hidden -fno-stack-protector -ffreestanding -fno-builtin -Wl,--hash-style=sysv -Wl,-z,norelro \
  -Wl,-soname,libGLES_route.so -Wl,--build-id=none -o "$OUT" route.c stubs.S \
  -L"$SYSROOT/usr/lib/arm-linux-androideabi/21" -ldl -lc
"$BIN/llvm-strip" "$OUT"
