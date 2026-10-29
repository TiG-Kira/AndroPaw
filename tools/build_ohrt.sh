#!/usr/bin/env bash
# Builds the built-in OpenHarmony runner (app/src/main/jniLibs/arm64-v8a/libohrt.so)
# with the Android NDK. Usage: tools/build_ohrt.sh [ndk-version]
set -euo pipefail
cd "$(dirname "$0")/.."

SDK="${ANDROID_HOME:-$LOCALAPPDATA/Android/Sdk}"
NDK_VER="${1:-28.2.13676358}"
TC="$SDK/ndk/$NDK_VER/toolchains/llvm/prebuilt/windows-x86_64/bin"
CC="$TC/aarch64-linux-android24-clang"

SRC=app/src/main/cpp/ohrt/ohrt.c
OUT=app/src/main/jniLibs/arm64-v8a/libohrt.so

mkdir -p "$(dirname "$OUT")"
"$CC" -O2 -std=c11 -fPIC -shared -Wall -Wextra \
    -llog -landroid \
    "$SRC" -o "$OUT"
"$TC/llvm-strip" "$OUT"
echo "built $OUT ($(stat -c %s "$OUT") bytes)"
