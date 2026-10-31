#!/system/bin/sh
# Runs the enabled modules' own scripts once the guest has booted.
#
# The host calls this after the system is up (core/Management.onBoot). Everything a Magisk or
# KernelSU module would put in post-fs-data.sh / service.sh runs here, with the same two
# phases: post-fs-data scripts in order and to completion, service scripts in the background.
# Output goes to /data/adb/andropaw/boot.log.

APW=/data/adb/andropaw
LOG=$APW/boot.log
MODS=/data/adb/modules
XP=/data/adb/xposed

echo "=== boot $(date) ===" >> "$LOG" 2>/dev/null

run_phase() {
  phase=$1
  for d in "$MODS"/*/; do
    [ -d "$d" ] || continue
    [ -f "$d/remove" ] && continue
    [ -f "$d/disable" ] && continue
    [ -f "$d/$phase" ] || continue
    echo "-- $phase: $(basename "$d")" >> "$LOG" 2>/dev/null
    if [ "$phase" = "service.sh" ]; then
      sh "$d/$phase" >> "$LOG" 2>&1 &
    else
      sh "$d/$phase" >> "$LOG" 2>&1
    fi
  done
  for d in "$XP"/*/; do
    [ -d "$d" ] || continue
    [ -f "$d/disable" ] && continue
    [ -f "$d/$phase" ] || continue
    echo "-- $phase: xposed/$(basename "$d")" >> "$LOG" 2>/dev/null
    if [ "$phase" = "service.sh" ]; then
      sh "$d/$phase" >> "$LOG" 2>&1 &
    else
      sh "$d/$phase" >> "$LOG" 2>&1
    fi
  done
}

run_phase post-fs-data.sh
run_phase service.sh

echo "=== boot scripts done ===" >> "$LOG" 2>/dev/null
