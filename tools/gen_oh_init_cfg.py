#!/usr/bin/env python3
"""Embed the runtime's container job list as a C string.

    tools/gen_oh_init_cfg.py app/src/main/cpp/ohrt/oh_init_cfg.h <rootfs>/system/etc/ohspace/init.cfg

The runtime package's /system/etc/init.cfg only imports /system/etc/ohspace/init.cfg; the
runner falls back to this embedded copy when a package does not ship that file. Re-run this
after updating the runtime so the fallback matches what the package would have used.
"""
import sys

# Source of truth: the container job list the runtime package ships at
# /system/etc/ohspace/init.cfg (extracted from the package by the caller).
cfg = open(sys.argv[2], encoding='utf-8').read()
lines = cfg.rstrip('\n').split('\n')

out = []
out.append('/*')
out.append(' * The container job list, as a fallback for a package that lost its own.')
out.append(' *')
out.append(' * /system/etc/init.cfg is one import line — {"import": ["/system/etc/ohspace/init.cfg"]}')
out.append(' * — and everything the boot does lives in that imported file: which services are started,')
out.append(' * in which order, and what the pre-init jobs create. When the imported file (or a runtime')
out.append(' * package that dropped it) is missing, init still loads the 122 service files under')
out.append(' * /system/etc/init but starts none of them: no hilogd, so every process in the guest')
out.append(' * prints "HiLogAdapter_init: Can\'t connect to server. Errno: 111" on stderr instead of')
out.append(' * logging; no samgr, no param_watcher, no framework. That is worth a fallback copy.')
out.append(' *')
out.append(' * The ohspace_ipcd lines are filtered out at write time when the daemon is not in the')
out.append(' * package, so the boot never waits ten seconds for a socket nobody will create.')
out.append(' */')
out.append('static const char OH_INIT_CFG[] =')
for i, l in enumerate(lines):
    esc = l.replace('\\', '\\\\').replace('"', '\\"')
    out.append('    "%s\\n"' % esc)
out[-1] += ';'

open(sys.argv[1], 'w').write('\n'.join(out) + '\n')
print('source bytes', len(cfg), '-> C lines', len(out))
