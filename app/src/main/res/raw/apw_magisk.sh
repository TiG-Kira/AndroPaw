#!/system/bin/sh
# Magisk-compatible control tool for this machine.
#
# Same module layout as Magisk (/data/adb/modules/<id> with module.prop and the disable /
# remove markers), same command surface for the parts that make sense without a magiskd: the
# host does the mounting-equivalent work (copying a module's system/ files into the guest tree
# and restoring them on uninstall) and runs the scripts at boot.

MODS=/data/adb/modules
MAGISK=/data/adb/magisk
VERSION=27000
VERSTR=27.0-andropaw

case "$1" in
  -v|--version|-V)
    echo "$VERSTR"
    exit 0
    ;;
  -c|--path)
    echo "$MAGISK"
    ;;
  --list|-l)
    for d in "$MODS"/*/; do
      [ -d "$d" ] || continue
      id=$(basename "$d")
      state=enabled
      [ -f "$d/disable" ] && state=disabled
      [ -f "$d/remove" ] && state=remove
      echo "$id $state"
    done
    ;;
  --install-module)
    zip=$2
    if [ -z "$zip" ] || [ ! -f "$zip" ]; then
      echo "magisk: no such file: $zip" >&2
      exit 1
    fi
    tmp=/data/local/tmp/magisk-install
    rm -rf "$tmp"
    mkdir -p "$tmp"
    if ! unzip -o -q "$zip" -d "$tmp" 2>/dev/null; then
      echo "magisk: unzip is not available on this system; install the module from AndroPaw instead" >&2
      exit 1
    fi
    id=$(grep -m1 '^id=' "$tmp/module.prop" 2>/dev/null | cut -d= -f2)
    if [ -z "$id" ]; then
      echo "magisk: module.prop has no id" >&2
      exit 1
    fi
    rm -rf "$MODS/$id"
    mkdir -p "$MODS/$id"
    cp -r "$tmp"/* "$MODS/$id"/ 2>/dev/null
    rm -rf "$tmp"
    echo "installed $id"
    ;;
  --remove-module)
    id=$2
    if [ -z "$id" ] || [ ! -d "$MODS/$id" ]; then
      echo "magisk: no such module: $id" >&2
      exit 1
    fi
    touch "$MODS/$id/remove"
    echo "marked $id for removal"
    ;;
  --enable-module)
    rm -f "$MODS/$2/disable"
    ;;
  --disable-module)
    touch "$MODS/$2/disable"
    ;;
  *)
    echo "Magisk-compatible magisk (AndroPaw) $VERSTR" >&2
    echo "usage: magisk --list | --install-module <zip> | --remove-module <id> | --enable-module <id> | --disable-module <id>" >&2
    exit 2
    ;;
esac
