#!/system/bin/sh
# Xposed-compatible control tool for machines older than Android 8 (the Xposed era).
#
# Modules sit in /data/adb/xposed/<id>/ (the installed apk, its module.prop and the
# disable / remove markers). /data/adb/xposed/modules.list names the apks the wrapped
# app_process has to put on the boot classpath, and /data/adb/xposed/bridge.jar is the
# Xposed bridge itself: when it is present the host installs the wrapper, so every app
# process starts with the modules' hook entry points loadable.

XP=/data/adb/xposed
VERSION=89

case "$1" in
  -v|--version|version)
    echo "$VERSION"
    ;;
  list|ls)
    for d in "$XP"/*/; do
      [ -d "$d" ] || continue
      id=$(basename "$d")
      state=enabled
      [ -f "$d/disable" ] && state=disabled
      [ -f "$d/remove" ] && state=remove
      echo "$id $state"
    done
    ;;
  enable)
    rm -f "$XP/$2/disable"
    ;;
  disable)
    touch "$XP/$2/disable"
    ;;
  bridge)
    if [ -f "$XP/bridge.jar" ]; then
      echo "$XP/bridge.jar"
    else
      echo "no bridge" >&2
      exit 1
    fi
    ;;
  *)
    echo "Xposed-compatible xposed (AndroPaw) $VERSION" >&2
    echo "usage: xposed list | enable <id> | disable <id> | bridge" >&2
    exit 2
    ;;
esac
