#!/bin/sh
# The GLES 1 colour fix in front of SwiftShader (see gles1fix.c): sh build.sh
# The real renderer goes to the same egl directory as libGLESv1_CM_ss.so (taken from the asset once, when that name
# is not there yet), the wrapper takes its name. 64-bit: engines/n64-gl.
NDK=${NDK:-$ANDROID_NDK_HOME}
BIN="$NDK/toolchains/llvm/prebuilt/windows-x86_64/bin"
[ -x "$BIN/clang" ] || BIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
cd "$(dirname "$0")" || exit 1
A=../../app/src/main/assets/engines/n64-gl
R="$A/system#lib64#egl#libGLESv1_CM_ss.so"
W="$A/system#lib64#egl#libGLESv1_CM_swiftshader.so"
[ -f "$R" ] || { cp "$W" "$R" && python -c "
import sys; p=sys.argv[1]; b=open(p,'rb').read(); o=b'libGLESv1_CM_swiftshader.so'; n=b'libGLESv1_CM_ss.so'
assert b.count(o)==1; open(p,'wb').write(b.replace(o, n+bytes(len(o)-len(n))))" "$R"; } || exit 1
"$BIN/clang" --target=aarch64-linux-android21 -O2 -fPIC -shared -fvisibility=hidden -Wall \
  -Wl,-soname,libGLESv1_CM_swiftshader.so -Wl,-rpath,'$ORIGIN' -o "$W" gles1fix.c \
  -L"$(dirname "$R")" -l:"$(basename "$R")" -ldl || exit 1
"$BIN/llvm-strip" "$W"
ls -la "$W" "$R"
# Kernel 23 (6.0): its linker ignores DT_RUNPATH, so the real renderer is named by its full path (a stub with that
# soname is linked against; the symbols come from the real one at run time)
S=/system/lib64/egl/libGLESv1_CM_ss.so
T=$(cygpath -m "$(mktemp -d)" 2>/dev/null || mktemp -d)
echo 'void aemu_gles1_stub(void) {}' > "$T/s.c"
MSYS_NO_PATHCONV=1 "$BIN/clang" --target=aarch64-linux-android21 -shared -Wl,-soname,$S -o "$T/libss.so" "$T/s.c" || exit 1
W23=../../app/src/main/assets/engines/n64-23/system#lib64#egl#libGLESv1_CM_swiftshader.so
"$BIN/clang" --target=aarch64-linux-android21 -O2 -fPIC -shared -fvisibility=hidden -Wall \
  -Wl,-soname,libGLESv1_CM_swiftshader.so -Wl,--no-as-needed -o "$W23" gles1fix.c "$T/libss.so" -ldl || exit 1
"$BIN/llvm-strip" "$W23"; rm -rf "$T"
# 5.1 (kernel 22): the same linker without DT_RUNPATH, the same build. Not enough there: 5.x libEGL takes the
# first libGLESv1_CM_*.so of the egl directory, the real renderer (renaming it crashed apps; open, see PLAN.md)
cp "$W23" "../../app/src/main/assets/engines/n64-22/system#lib64#egl#libGLESv1_CM_swiftshader.so"
ls -la "$W23"
