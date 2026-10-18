#!/bin/sh
# The GLES bridge: sh build.sh <kernel tag>...
# builds the guest driver libGLES_aemu.so into those kernels' guest files and the host renderer libaemugl.so
# into the application.
NDK=${NDK:-$ANDROID_NDK_HOME}
BIN="$NDK/toolchains/llvm/prebuilt/windows-x86_64/bin"
[ -x "$BIN/clang" ] || BIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
cd "$(dirname "$0")" || exit 1
python gen.py "$BIN/../sysroot/usr/include" || exit 1
A=../../app/src/main/assets/engines
W="-Wall -Wno-unused-function"
"$BIN/clang" --target=aarch64-linux-android29 -O2 -fPIC -shared $W -Wl,-soname,libaemugl.so \
  -o ../../app/src/main/jniLibs/arm64-v8a/libaemugl.so host.c -lEGL -lGLESv3 -llog -lnativewindow -landroid || exit 1
"$BIN/llvm-strip" ../../app/src/main/jniLibs/arm64-v8a/libaemugl.so
for TAG in "$@"; do
  # (API 21: thread-local storage through libc's keys, which every bionic has)
  "$BIN/clang" --target=aarch64-linux-android21 -O2 -fPIC -shared -fvisibility=hidden $W -Wl,-soname,libGLES_aemu.so \
    -o "$A/n64-$TAG/system#lib64#egl#libGLES_aemu.so" guest.c -llog -ldl || exit 1
  "$BIN/llvm-strip" "$A/n64-$TAG/system#lib64#egl#libGLES_aemu.so"
  ls -la "$A/n64-$TAG/system#lib64#egl#libGLES_aemu.so"
done
ls -la ../../app/src/main/jniLibs/arm64-v8a/libaemugl.so
