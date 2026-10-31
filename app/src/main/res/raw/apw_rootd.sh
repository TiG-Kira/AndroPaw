#!/system/bin/sh
# AndroPaw's root daemon: the resident half that every rooted generation has.
#
# On a real device magiskd, ksud or daemonsu is the process that answers `su`. Here the host
# answers it (apw_su.sh asks through /data/adb/andropaw), and this process is what a root
# check finds when it looks for the daemon that ought to be running: it is installed under
# each backend's own name (/data/adb/magisk/magiskd, /data/adb/ksu/bin/ksud, /system/xbin/
# daemonsu) and stays alive for as long as the machine does.
#
# Started from the boot hook (apw_boot.sh) in the background.

APW=/data/adb/andropaw
echo $$ > "$APW/rootd.pid" 2>/dev/null
while : ; do
  sleep 60
done
