#!/bin/sh
# Сборка camera HAL эмулятора: ARMv7, только libc, классическая хэш-таблица для линкера bionic 2.3
NDK=${NDK:-$ANDROID_NDK_HOME}
BIN="$NDK/toolchains/llvm/prebuilt/windows-x86_64/bin"
[ -x "$BIN/clang" ] || BIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
OUT=${1:-../../app/src/main/assets/engines/common/camera.default.so}
mkdir -p "$(dirname "$OUT")"
"$BIN/clang" --target=armv7a-linux-androideabi21 -march=armv7-a -mthumb -Os -fPIC -shared \
  -nostdlib -ffreestanding -fno-builtin -fvisibility=hidden -fno-stack-protector \
  -Wl,--hash-style=sysv -Wl,-z,norelro -Wl,--build-id=none -Wl,--no-undefined -Wl,-soname,camera.default.so \
  -o "$OUT" camera_hal.c -ldl -lc
"$BIN/llvm-strip" "$OUT" 2>/dev/null || true
# 2.3: a HAL with no cameras for firmwares whose own one needs the modem (see camera_gb_none.c)
"$BIN/clang" --target=armv7a-linux-androideabi21 -march=armv7-a -mthumb -Os -fPIC -shared \
  -nostdlib -ffreestanding -fno-builtin -fvisibility=hidden -fno-stack-protector \
  -Wl,--hash-style=sysv -Wl,-z,norelro -Wl,--build-id=none -Wl,--no-undefined -Wl,-soname,libcamera.so \
  -o "$(dirname "$OUT")/libcamera.none.so" camera_gb_none.c
"$BIN/llvm-strip" "$(dirname "$OUT")/libcamera.none.so" 2>/dev/null || true
# native arm64 engine, Android 8.x (kernel 27): arm64, camera module API 2.4 for the HIDL camera provider
"$BIN/clang" --target=aarch64-linux-android26 -DCAM_MODULE_24 -Os -fPIC -shared \
  -nostdlib -ffreestanding -fno-builtin -fvisibility=hidden -fno-stack-protector \
  -Wl,-z,norelro -Wl,--build-id=none -Wl,--no-undefined -Wl,-soname,camera.default.so \
  -o "$(dirname "$OUT")/../n64-${TAG:-27}/system#lib64#hw#camera.default.so" camera_hal.c -ldl -lc
"$BIN/llvm-strip" "$(dirname "$OUT")/../n64-${TAG:-27}/system#lib64#hw#camera.default.so" 2>/dev/null || true
# native arm64 engine, Android 7.1 (kernel 25): the 32-bit camera service of a vendor firmware, camera module API 2.4
# for Camera.openLegacy
"$BIN/clang" --target=armv7a-linux-androideabi21 -march=armv7-a -mthumb -DCAM_MODULE_24 -Os -fPIC -shared   -nostdlib -ffreestanding -fno-builtin -fvisibility=hidden -fno-stack-protector   -Wl,--hash-style=sysv -Wl,-z,norelro -Wl,--build-id=none -Wl,--no-undefined -Wl,-soname,camera.default.so   -o "$(dirname "$OUT")/../n64-25/system#lib#hw#camera.default.so" camera_hal.c -ldl -lc
"$BIN/llvm-strip" "$(dirname "$OUT")/../n64-25/system#lib#hw#camera.default.so" 2>/dev/null || true
