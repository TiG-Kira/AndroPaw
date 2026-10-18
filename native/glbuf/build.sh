#!/bin/sh
# glbuf: buffered client reads, preloaded into libglserverd_kk (arm64, app's native libs)
NDK=${NDK:-$ANDROID_NDK_HOME}
BIN="$NDK/toolchains/llvm/prebuilt/windows-x86_64/bin"
[ -d "$BIN" ] || BIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
OUT=${1:-../../app/src/main/jniLibs/arm64-v8a/libglbuf.so}
"$BIN/clang" --target=aarch64-linux-android21 -O2 -fPIC -shared -fvisibility=hidden -Wall -Wl,-soname,libglbuf.so -o "$OUT" glbuf.c || exit 1
"$BIN/llvm-strip" "$OUT"
ls -la "$OUT"
