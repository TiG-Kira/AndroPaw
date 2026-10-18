#!/bin/sh
# Camera HAL 3 of the native arm64 engine (see camera_hal3.c): sh build3.sh <kernel tag> [vendor tag table]
# builds it into that kernel's guest files as system/lib64/hw and vendor/lib64/hw camera.default.so.
TAG=${1:?usage: sh build3.sh <kernel tag> [camera_tags_NN.h]}
NDK=${NDK:-$ANDROID_NDK_HOME}
BIN="$NDK/toolchains/llvm/prebuilt/windows-x86_64/bin"
[ -x "$BIN/clang" ] || BIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
D="../../app/src/main/assets/engines/n64-$TAG"
OUT="$D/system#lib64#hw#camera.default.so"
"$BIN/clang" --target=aarch64-linux-android26 ${2:+-DCAM_VENDOR_TAGS="\"$2\""} -Os -fPIC -shared \
  -nostdlib -ffreestanding -fno-builtin -fvisibility=hidden -fno-stack-protector \
  -Wl,-z,norelro -Wl,--build-id=none -Wl,--no-undefined -Wl,-soname,camera.default.so \
  -o "$OUT" camera_hal3.c -ldl -lc || exit 1
"$BIN/llvm-strip" "$OUT" 2>/dev/null || true
cp "$OUT" "$D/vendor#lib64#hw#camera.default.so"
