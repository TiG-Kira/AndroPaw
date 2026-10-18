#!/bin/sh
# The GLES bridge for the 32-bit engine (KK, armv7): sh build32.sh -> assets engines/kk/libGLES_aemu.so
# (the host side is the same libaemugl.so as for build.sh)
NDK=${NDK:-$ANDROID_NDK_HOME}
BIN="$NDK/toolchains/llvm/prebuilt/windows-x86_64/bin"
[ -x "$BIN/clang" ] || BIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
cd "$(dirname "$0")" || exit 1
python gen.py "$BIN/../sysroot/usr/include" || exit 1
O=../../app/src/main/assets/engines/kk/libGLES_aemu.so
# (API 21 is the lowest the NDK has: what it uses beyond 4.4's bionic is weak - getprogname)
"$BIN/clang" --target=armv7a-linux-androideabi21 -O2 -fPIC -shared -fvisibility=hidden -Wall -Wno-unused-function \
  -Wl,-soname,libGLES_aemu.so -o $O guest.c -llog -ldl || exit 1
"$BIN/llvm-strip" $O
ls -la $O
