#!/system/bin/sh
# AndroPaw superuser client.
#
# This emulator runs the guest on the host's kernel, so "root" is not a second boot image:
# it is the emulator's own privilege layer. When the ROOT switch is on, the machine is
# rooted — init's services start as root, /system is writable, the properties a rooted
# device carries are set — and this script is the door an app knocks on.
#
# The door answers through the host, not here: this script drops a request in
# /data/adb/andropaw/ask/<pid> and waits for /data/adb/andropaw/answer/<pid>. The host knows
# which package is calling (it reads the process tree), keeps the list the ROOT page shows,
# and answers "allow" for a rooted machine unless that app was denied there. Every tool used
# below exists in the toolbox of 2.3 as well — no tr, no grep, no cut.

APW=/data/adb/andropaw
ASK=$APW/ask
ANS=$APW/answer

if [ ! -f "$APW/enabled" ]; then
  echo "su: this emulator is not rooted (turn on ROOT in AndroPaw)" >&2
  exit 1
fi

# ------------------------------------------------------------------ who we pretend to be
# The version a root check reads out of `su -v` (and `-V`), per generation.
BACKEND=$(cat "$APW/backend" 2>/dev/null)
case "$BACKEND" in
  magisk)    VERSION_NAME="26.4:MAGISKSU"; VERSION_CODE=26400 ;;
  kernelsu)  VERSION_NAME="0.9.5:kernelsu"; VERSION_CODE=10905 ;;
  supersu)   VERSION_NAME="2.82:SUPERSU";  VERSION_CODE=282 ;;
  *)         VERSION_NAME="1.0.0:superuser"; VERSION_CODE=100 ;;
esac

# ------------------------------------------------------------------ what to run
CMD=""
SHELL_ONLY=1
SHELLBIN=/system/bin/sh
while [ $# -gt 0 ]; do
  case "$1" in
    -c)
      CMD=$2
      SHELL_ONLY=0
      shift 2
      ;;
    -s)
      SHELLBIN=$2
      shift 2
      ;;
    -v|--version)
      echo "$VERSION_NAME"
      exit 0
      ;;
    -V)
      echo "$VERSION_CODE"
      exit 0
      ;;
    --mount-master|--context)
      shift 2
      ;;
    --context=*)
      shift
      ;;
    -l|--login|-)
      shift
      ;;
    0|root|--root)
      shift
      ;;
    --)
      shift
      break
      ;;
    -*)
      shift
      ;;
    *)
      CMD="$1"
      shift
      SHELL_ONLY=0
      break
      ;;
  esac
done
if [ -n "$CMD" ]; then
  SHELL_ONLY=0
fi

# ------------------------------------------------------------------ ask the host
ME=$$
# The host looks at the ask directory a few times a second, so the answer is usually there in
# well under a tenth: poll quickly where the toolbox can sleep a fraction (3.x+), in whole
# seconds where it cannot.
NAP=0.02
TRIES=120
if ! sleep 0.02 2>/dev/null; then NAP=1; TRIES=8; fi

echo "$CMD" > "$ASK/$ME" 2>/dev/null

ANSWER=""
i=0
while [ $i -lt $TRIES ]; do
  if [ -f "$ANS/$ME" ]; then
    ANSWER=$(cat "$ANS/$ME" 2>/dev/null)
    break
  fi
  sleep $NAP
  i=$((i+1))
done
rm -f "$ANS/$ME" 2>/dev/null
rm -f "$ASK/$ME" 2>/dev/null

# No answer at all means nothing is watching (the machine was started outside AndroPaw, or
# the host is busy): a rooted machine grants, which is what the switch promised.
if [ -z "$ANSWER" ]; then
  ANSWER=allow
fi
if [ "$ANSWER" != "allow" ]; then
  echo "su: permission denied" >&2
  exit 1
fi

# ------------------------------------------------------------------ run
# AEMU64_ID is what the arm64 runner reads to report an id, DHD_UID is the 32-bit engine's
# spelling of the same thing: from here on everything this process starts answers `id` with
# 0 — that is the privilege the switch gives, the guest's own notion of root.
AEMU64_ID=0,0
DHD_UID=0
DHD_GID=0
export AEMU64_ID DHD_UID DHD_GID
if [ "$SHELL_ONLY" = "1" ]; then
  exec "$SHELLBIN"
fi
exec "$SHELLBIN" -c "$CMD"
