#!/bin/sh
# Gralloc 5 of the native arm64 engine (Android 17, see mapper.c): sh build.sh <kernel tag> builds the stable-C
# mapper and the AIDL allocator service into that kernel's guest files, and that kernel's gralloc with the note
# the mapper reads (native/grallocwrap, AEMU_META).
TAG=${1:?usage: sh build.sh <kernel tag>}
NDK=${NDK:-$ANDROID_NDK_HOME}
BIN="$NDK/toolchains/llvm/prebuilt/windows-x86_64/bin"
[ -x "$BIN/clang" ] || BIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
cd "$(dirname "$0")" || exit 1
D="../../app/src/main/assets/engines/n64-$TAG"
(cd ../grallocwrap && DEFS=-DAEMU_META sh build.sh "$TAG") || exit 1
OUT="$D/vendor#lib64#hw#mapper.aemu.so"
"$BIN/clang" --target=aarch64-linux-android29 -Os -fPIC -shared \
  -nostdlib -ffreestanding -fno-builtin -fvisibility=hidden -fno-stack-protector \
  -Wl,-z,norelro -Wl,--build-id=none -Wl,--no-undefined -Wl,-soname,mapper.aemu.so \
  -o "$OUT" mapper.c -ldl -lc || exit 1
T=${TMPDIR:-${TEMP:-/tmp}}/aemu-gralloc5; mkdir -p "$T"
# the NDK's libbinder_ndk has no process and service manager entries (platform only): a stand-in to link against
cat > "$T/b.c" <<'EOF'
void AIBinder_Class_define(void) {} void AIBinder_new(void) {} void AIBinder_markVintfStability(void) {}
void AServiceManager_addService(void) {} void ABinderProcess_setThreadPoolMaxThreadCount(void) {} void ABinderProcess_joinThreadPool(void) {}
void AParcel_readInt32(void) {} void AParcel_readInt64(void) {} void AParcel_readByteArray(void) {} void AParcel_writeInt32(void) {}
void AParcel_writeBool(void) {} void AParcel_writeString(void) {} void AParcel_writeInt32Array(void) {} void AParcel_writeParcelFileDescriptor(void) {}
void AParcel_getDataPosition(void) {} void AParcel_setDataPosition(void) {} void AParcel_writeStatusHeader(void) {}
void AStatus_newOk(void) {} void AStatus_fromServiceSpecificError(void) {} void AStatus_delete(void) {}
EOF
"$BIN/clang" --target=aarch64-linux-android29 -shared -nostdlib -Wl,-soname,libbinder_ndk.so -o "$T/libbinder_ndk.so" "$T/b.c" || exit 1
SVC="$D/vendor#bin#hw#android.hardware.graphics.allocator-service.aemu"
"$BIN/clang" --target=aarch64-linux-android29 -O2 -Wall -o "$SVC" allocator.c -L"$T" -lbinder_ndk -ldl || exit 1
"$BIN/llvm-strip" "$OUT" "$SVC" 2>/dev/null || true
ls -la "$OUT" "$SVC" "$D/vendor#lib64#hw#gralloc.default.so"
