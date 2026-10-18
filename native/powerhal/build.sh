#!/bin/sh
# power HAL of the native arm64 engine: sh build.sh <kernel tag> builds it into that kernel's guest files
TAG=${1:?usage: sh build.sh <kernel tag>}
NDK=${NDK:-$ANDROID_NDK_HOME}
BIN="$NDK/toolchains/llvm/prebuilt/windows-x86_64/bin"
[ -x "$BIN/clang" ] || BIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
OUT="../../app/src/main/assets/engines/n64-$TAG/vendor#lib64#hw#power.default.so"
"$BIN/clang" --target=aarch64-linux-android26 -Os -fPIC -shared \
  -nostdlib -ffreestanding -fno-builtin -fvisibility=hidden -fno-stack-protector \
  -Wl,-z,norelro -Wl,--build-id=none -Wl,--no-undefined -Wl,-soname,power.default.so \
  -o "$OUT" power_hal.c || exit 1
"$BIN/llvm-strip" "$OUT" 2>/dev/null || true
