#!/system/bin/sh
# KernelSU-compatible control tool for this machine.
#
# The modules themselves live where KernelSU keeps them (/data/adb/modules/<id>, module.prop,
# disable / remove markers), so anything that reads that layout — the AndroPaw plug-in page
# included — sees the same state. What is missing is the kernel side: there is no overlayfs
# and no magiskd here, so the host applies a module's system/ files itself and runs its
# scripts at boot (see core/PluginManager).

MODS=/data/adb/modules
VERSION=12000

case "$1" in
  --version|-v|version)
    echo "$VERSION"
    exit 0
    ;;
  module|modules)
    shift
    case "$1" in
      list|ls)
        for d in "$MODS"/*/; do
          [ -d "$d" ] || continue
          id=$(basename "$d")
          if [ -f "$d/remove" ]; then
            echo "$id removed"
          elif [ -f "$d/disable" ]; then
            echo "$id disabled"
          else
            echo "$id enabled"
          fi
        done
        ;;
      install)
        zip=$2
        if [ -z "$zip" ] || [ ! -f "$zip" ]; then
          echo "ksud: no such file: $zip" >&2
          exit 1
        fi
        tmp=/data/local/tmp/ksud-install
        rm -rf "$tmp"
        mkdir -p "$tmp"
        if ! unzip -o -q "$zip" -d "$tmp" 2>/dev/null; then
          echo "ksud: unzip is not available on this system; install the module from AndroPaw instead" >&2
          exit 1
        fi
        id=$(grep -m1 '^id=' "$tmp/module.prop" 2>/dev/null | cut -d= -f2)
        if [ -z "$id" ]; then
          echo "ksud: module.prop has no id" >&2
          exit 1
        fi
        rm -rf "$MODS/$id"
        mkdir -p "$MODS/$id"
        cp -r "$tmp"/* "$MODS/$id"/ 2>/dev/null
        rm -rf "$tmp"
        echo "installed $id"
        ;;
      uninstall|remove)
        id=$2
        if [ -z "$id" ] || [ ! -d "$MODS/$id" ]; then
          echo "ksud: no such module: $id" >&2
          exit 1
        fi
        touch "$MODS/$id/remove"
        echo "marked $id for removal"
        ;;
      enable)
        rm -f "$MODS/$2/disable"
        ;;
      disable)
        touch "$MODS/$2/disable"
        ;;
      *)
        echo "usage: ksud module list|install <zip>|uninstall <id>|enable <id>|disable <id>" >&2
        exit 2
        ;;
    esac
    ;;
  *)
    echo "KernelSU-compatible ksud (AndroPaw) version $VERSION" >&2
    echo "usage: ksud module ..." >&2
    exit 2
    ;;
esac
