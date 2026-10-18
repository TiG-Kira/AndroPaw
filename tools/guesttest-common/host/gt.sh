#!/bin/sh
# gt.sh <id> [only] [secs]: the guest tester in the RUNNING image: picks the program by the image's API
# (A: up to 19, B: 21-27, C: 28 and later), installs, starts, waits for "done" (88 s at most; "gt.sh -w <id>"
# waits again), prints the report. Saved as gt-<id>.txt next to this script's work directory (W).
export MSYS_NO_PATHCONV=1
A="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe${ANDROID_SERIAL:+ -s $ANDROID_SERIAL}"
W=${W:-$PWD}
T=$(cd "$(dirname "$0")/../.." && pwd)
if [ "$1" = "-w" ]; then shift; R="files/images/$1/root/data/data/app.aemu.test/files/report.txt"; else
  I=$1
  R="files/images/$I/root/data/data/app.aemu.test/files/report.txt"
  API=$($A shell "run-as app.aemu grep '\"api\"' files/images/$I/image.json" | tr -cd '0-9')
  if [ "${API:-0}" -le 19 ]; then P=$T/guesttest-a/build/aemu-test-a.apk
  elif [ "$API" -le 27 ]; then P=$T/guesttest-b/build/aemu-test-b.apk
  else P=$T/guesttest/build/aemu-test.apk; fi
  [ -f "$P" ] || { echo "no $P - build it (sh build.sh in its directory)"; exit 1; }
  G=""; [ "$API" -ge 23 ] && G="-g"
  $A push $P /data/local/tmp/aemu-test.apk >/dev/null
  $A shell "run-as app.aemu sh -c 'mkdir -p files/images/$I/root/data/local/tmp; cp /data/local/tmp/aemu-test.apk files/images/$I/root/data/local/tmp/; chmod 644 files/images/$I/root/data/local/tmp/aemu-test.apk'"
  T=40 sh $W/gsh.sh $I "pm install -r $G /data/local/tmp/aemu-test.apk" | tail -1
  # (some vendors' installers ignore -g)
  [ "$API" -ge 23 ] && for p in CAMERA RECORD_AUDIO; do T=15 sh $W/gsh.sh $I "pm grant app.aemu.test android.permission.$p" >/dev/null; done
  $A shell "run-as app.aemu rm -f $R"
  E=""; [ -n "$2" ] && E="--es only $2"; [ -n "$3" ] && E="$E --ei secs $3"
  T=20 sh $W/gsh.sh $I "am start -S -n app.aemu.test/.Main $E" | tail -1
  [ -n "$NOWAIT" ] && exit 0
fi
n=0
until $A shell "run-as app.aemu cat $R 2>/dev/null" | grep -q "^done=1" || [ $n -ge 17 ]; do sleep 5; n=$((n+1)); done
$A shell "run-as app.aemu cat $R 2>/dev/null" | tr -d '\r' > $W/gt-$1.txt
cat $W/gt-$1.txt
grep -q "^done=1" $W/gt-$1.txt || echo "(not finished: last test $(grep '^test=' $W/gt-$1.txt | tail -1))"
