#!/usr/bin/env python3
"""Stubs of libGLES_route (see route.c): every EGL/GL entry jumps to the same entry of the renderer chosen for the
calling thread; EGL entries that take a display get that renderer's own display in its place."""
syms = [s.strip() for s in open('../glsplit/symbols.txt') if s.strip()]
SPECIAL = {'eglGetDisplay', 'eglInitialize', 'eglTerminate', 'eglGetCurrentDisplay', 'eglGetProcAddress'}
NO_DPY = SPECIAL | {'eglBindAPI', 'eglGetCurrentContext', 'eglGetCurrentSurface', 'eglGetError', 'eglGetSystemTimeFrequencyNV',
                    'eglGetSystemTimeNV', 'eglQueryAPI', 'eglReleaseThread', 'eglWaitClient', 'eglWaitGL', 'eglWaitNative'}
out = ['/* written by gen.py */', '    .syntax unified', '    .arm', '    .text']
for i, s in enumerate(syms):
    if s in SPECIAL:
        continue
    out += [f'    .global {s}', f'    .type {s},%function', '    .align 2', f'{s}:',
            '    push {r0-r3, r4, lr}', f'    movw r0, #{i}', '    bl aemu_route_resolve']
    if s.startswith('egl') and s not in NO_DPY:
        out += ['    mov r4, r0', '    bl aemu_route_dpy', '    str r0, [sp]', '    mov r12, r4']
    else:
        out += ['    mov r12, r0']
    out += ['    pop {r0-r3, r4, lr}', '    bx r12']
open('stubs.S', 'w').write('\n'.join(out) + '\n')
with open('names.h', 'w') as f:
    f.write('/* written by gen.py */\nstatic const char *const kNames[] = {\n')
    for s in syms: f.write(f'    "{s}",\n')
    f.write('};\n')
    for s in sorted(SPECIAL): f.write(f'#define IDX_{s} {syms.index(s)}\n')
