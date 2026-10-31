#!/system/bin/sh
# AndroPaw superuser client.
#
# A guest process asks for root by running su. This script looks the caller up in the policy
# the host keeps at /data/adb/andropaw/policy, records a first-time request in requests.log
# (the ROOT page lists those, KernelSU-style) and waits for the user's answer before it gives
# up. Root here is a gate, not a real uid change: this emulator runs the guest on the host's
# kernel, so what a "root" process may touch is decided by the gate, not by setuid.

APW=/data/adb/andropaw
POL=$APW/policy
REQ=$APW/requests.log

if [ ! -f "$APW/enabled" ]; then
  echo "su: root access is disabled on this machine (AndroPaw > ROOT)" >&2
  exit 1
fi

# ------------------------------------------------------------------ who is asking
# The caller is the process that ran su. Apps usually reach it through a shell
# (Runtime.exec("su -c ...") goes through /system/bin/sh), so a few levels are climbed while
# the process found is a shell rather than an app.
name_of() {
  n=""
  if [ -r "/proc/$1/cmdline" ]; then
    n=$(tr '\0' '\n' < "/proc/$1/cmdline" 2>/dev/null | head -n 1)
  fi
  if [ -z "$n" ]; then
    n=$(cat "/proc/$1/comm" 2>/dev/null)
  fi
  # the process name, not the path it was started from: an app's is its package
  n=${n##*/}
  echo "$n"
}

parent_of() {
  rest=$(sed 's/^.*) //' "/proc/$1/stat" 2>/dev/null)
  echo "$rest" | cut -d' ' -f2
}

PARENT=$PPID
CALLER=$(name_of "$PARENT")
CUID=$(id -u 2>/dev/null)
climb=0
while [ $climb -lt 3 ]; do
  case "$CALLER" in
    sh|ash|mksh|ksh|dash|su|toybox|busybox|"")
      next=$(parent_of "$PARENT")
      if [ -z "$next" ] || [ "$next" = "0" ] || [ "$next" = "1" ]; then
        break
      fi
      PARENT=$next
      CALLER=$(name_of "$PARENT")
      ;;
    *)
      break
      ;;
  esac
  climb=$((climb+1))
done
if [ -z "$CALLER" ]; then
  CALLER=unknown
fi

# ------------------------------------------------------------------ what to run
CMD=""
SHELL_ONLY=1
case "$1" in
  -c)
    CMD=$2
    SHELL_ONLY=0
    ;;
  -s|-|"")
    SHELL_ONLY=1
    ;;
  0|root)
    shift
    CMD="$*"
    SHELL_ONLY=0
    ;;
  *)
    CMD="$*"
    SHELL_ONLY=0
    ;;
esac
if [ -n "$CMD" ]; then
  SHELL_ONLY=0
fi

# ------------------------------------------------------------------ policy
decision() {
  if [ ! -f "$POL" ]; then
    return 0
  fi
  grep -m1 "^$CALLER " "$POL" 2>/dev/null | cut -d' ' -f2
}

state=$(decision)

if [ "$state" != "allow" ] && [ "$state" != "deny" ]; then
  # First request from this app: record it for the ROOT page, then give the user a moment to
  # answer instead of failing straight away.
  if ! grep -q "^$CALLER " "$REQ" 2>/dev/null; then
    printf '%s %s %s %s\n' "$(date +%s)" "$CALLER" "$CUID" "$CMD" >> "$REQ" 2>/dev/null
  fi
  n=0
  while [ $n -lt 300 ]; do
    state=$(decision)
    if [ "$state" = "allow" ]; then
      break
    fi
    sleep 0.2 2>/dev/null || sleep 1
    n=$((n+1))
  done
fi

if [ "$state" != "allow" ]; then
  echo "su: permission denied for $CALLER" >&2
  exit 1
fi

# ------------------------------------------------------------------ run
# AEMU64_ID is what the arm64 runner reads to report an id; DHD_UID is the 32-bit engine's
# spelling of the same thing. Neither turns a shell into a real uid 0 (there is no root to
# turn into), but both keep /proc/self/status and the common `id` checks consistent for the
# processes that care.
# (exported rather than passed through `env`: toolbox on 2.x does not ship one)
AEMU64_ID=0,0
DHD_UID=0
DHD_GID=0
export AEMU64_ID DHD_UID DHD_GID
if [ "$SHELL_ONLY" = "1" ]; then
  exec /system/bin/sh
fi
exec /system/bin/sh -c "$CMD"
