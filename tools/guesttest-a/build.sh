#!/bin/sh
# Tester A of guest firmwares, Android 1.5 - 4.4 (see src/app/aemu/test/Main.java and ../guesttest-common):
# builds build/aemu-test-a.apk without Gradle. Needs a JDK (javac) and the Android SDK (aapt2, d8, zipalign,
# apksigner, android.jar); signed with the debug key, scheme 1 only (SHA-1 digests, what Android before 4.3 reads).
set -e
cd "$(dirname "$0")"
SDK=${ANDROID_HOME:-$LOCALAPPDATA/Android/Sdk}
JAR="$SDK/platforms/android-36/android.jar"
BT=$(ls -d "$SDK"/build-tools/*/ | sort -V | tail -1)
D8="${BT}d8"; [ -x "$D8" ] || D8="$D8.bat"
SIGN="${BT}apksigner"; [ -x "$SIGN" ] || SIGN="$SIGN.bat"
rm -rf build && mkdir -p build/classes
"${BT}aapt2" link -o build/base.apk --manifest AndroidManifest.xml -I "$JAR"
javac --release 8 -Xlint:-options -Xlint:-deprecation -cp "$JAR" -d build/classes $(find src ../guesttest-common/src -name '*.java')
python -c "import zipfile,os;z=zipfile.ZipFile('build/classes.jar','w');[z.write(os.path.join(r,f),os.path.relpath(os.path.join(r,f),'build/classes')) for r,_,fs in os.walk('build/classes') for f in fs];z.close()"
"$D8" --min-api 3 --lib "$JAR" --output build build/classes.jar
python -c "import zipfile;z=zipfile.ZipFile('build/base.apk','a',zipfile.ZIP_DEFLATED);z.write('build/classes.dex','classes.dex');z.close()"
"${BT}zipalign" -f 4 build/base.apk build/aligned.apk
"$SIGN" sign --ks "${KEYSTORE:-$(cygpath -m "$HOME" 2>/dev/null || echo "$HOME")/.android/debug.keystore}" --ks-pass pass:android --key-pass pass:android \
  --min-sdk-version 3 --v1-signing-enabled true --v2-signing-enabled false --v3-signing-enabled false --out build/aemu-test-a.apk build/aligned.apk
ls -la build/aemu-test-a.apk
