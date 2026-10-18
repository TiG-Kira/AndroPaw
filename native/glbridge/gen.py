#!/usr/bin/env python3
"""The GLES bridge's generated halves (see guest.c, host.c): python gen.py <ndk sysroot>/usr/include

g_api.h (guest): every GLES 1/2/3 entry point as an exported stub that jumps through the calling thread's table, and
the encoders of the GLES 2/3.0 core that put a call into the command stream.
h_api.h (host): the decoder of that stream.

A call is: uint32 opcode (the function's index), uint32 payload length, the payload - scalars as 4 or 8 bytes, a
block as uint32 length (0xffffffff: a null pointer) and its bytes padded to 4. A call that returns something is
answered at once: the "out" blocks in order (each of the size both sides compute from the scalars), then the return
value."""
import re, sys

INC = sys.argv[1]


def parse(path):
    out = []
    for m in re.finditer(r'GL_API(?:CALL)?\s+(.+?)\s*GL_APIENTRY\s+(\w+)\s*\((.*?)\);', open(INC + '/' + path).read(), re.S):
        ret, name, args = m.group(1).strip(), m.group(2), ' '.join(m.group(3).split())
        params = []
        if args != 'void':
            for a in args.split(','):
                mm = re.match(r'(.*?)(\w+)$', a.strip())
                if not mm or not mm.group(1).strip(): params = None; break     # (an array or a nameless parameter)
                params.append((mm.group(1).strip(), mm.group(2)))
        if params is None: print('skipped:', name, args); continue
        out.append((name, ret, params))
    return out


core = parse('GLES3/gl31.h')     # (GLES 3.0 and 3.1: the guest is told 3.1 when the phone has it, host.c es_minor)
seen = {f[0] for f in core}
extra = []
for h in ('GLES2/gl2ext.h', 'GLES/gl.h', 'GLES/glext.h'):
    for f in parse(h):
        if f[0] not in seen:
            seen.add(f[0]); extra.append(f)
funcs = core + extra
index = {f[0]: i for i, f in enumerate(funcs)}

KNOWN = '''typedef unsigned int GLenum, GLuint, GLbitfield;
typedef int GLint, GLsizei, GLfixed, GLclampx;
typedef float GLfloat, GLclampf;
typedef unsigned char GLboolean, GLubyte;
typedef char GLchar;
typedef signed char GLbyte;
typedef short GLshort;
typedef unsigned short GLushort, GLhalf;
typedef void GLvoid;
typedef signed long GLintptr, GLsizeiptr;
typedef int64_t GLint64;
typedef uint64_t GLuint64;
typedef struct __GLsync *GLsync;
'''
known = set(re.findall(r'GL\w+', KNOWN))
others = []
for f in funcs:
    for t in [f[1]] + [p[0] for p in f[2]]:
        for w in re.findall(r'GL\w+', t):
            if w not in known:
                known.add(w)
                others.append('typedef %s %s;' % ('int64_t' if '64' in w else 'signed long' if 'ptr' in w.lower() else
                                                  'void *' if re.search(r'PROC|egl|sync|Buffer', w) else 'int', w))

# ---- what the pointers of the core functions are ----
SZ = lambda e: '(uint32_t)((%s) > 0 ? (%s) : 0)' % (e, e)
SPEC = {
    'glBufferData': {'data': ('in', 'size', True)},
    'glBufferSubData': {'data': ('in', 'size')},
    'glTexParameterfv': {'params': ('in', '4')}, 'glTexParameteriv': {'params': ('in', '4')},
    'glSamplerParameterfv': {'param': ('in', '4')}, 'glSamplerParameteriv': {'param': ('in', '4')},
    'glDrawBuffers': {'bufs': ('in', 'n * 4')},
    'glInvalidateFramebuffer': {'attachments': ('in', 'numAttachments * 4')},
    'glInvalidateSubFramebuffer': {'attachments': ('in', 'numAttachments * 4')},
    'glClearBufferiv': {'value': ('in', 'buffer == 0x1800 ? 16 : 4')},
    'glClearBufferuiv': {'value': ('in', 'buffer == 0x1800 ? 16 : 4')},
    'glClearBufferfv': {'value': ('in', 'buffer == 0x1800 ? 16 : 4')},
    'glShaderBinary': {'shaders': ('in', 'count * 4'), 'binary': ('in', 'length')},
    'glProgramBinary': {'binary': ('in', 'length')},
    'glBindAttribLocation': {'name': ('str',)}, 'glGetAttribLocation': {'name': ('str',)},
    'glGetUniformLocation': {'name': ('str',)}, 'glGetFragDataLocation': {'name': ('str',)},
    'glGetUniformBlockIndex': {'uniformBlockName': ('str',)},
    'glGetBooleanv': {'data': ('out', 'aemu_get_count(pname)')},
    'glGetFloatv': {'data': ('out', 'aemu_get_count(pname) * 4')},
    'glGetIntegerv': {'data': ('out', 'aemu_get_count(pname) * 4')},
    'glGetInteger64v': {'data': ('out', 'aemu_get_count(pname) * 8')},
    'glGetBufferParameteriv': {'params': ('out', '4')}, 'glGetBufferParameteri64v': {'params': ('out', '8')},
    'glGetFramebufferAttachmentParameteriv': {'params': ('out', '4')},
    'glGetProgramiv': {'params': ('out', '4')}, 'glGetShaderiv': {'params': ('out', '4')},
    'glGetRenderbufferParameteriv': {'params': ('out', '4')},
    'glGetTexParameterfv': {'params': ('out', '4')}, 'glGetTexParameteriv': {'params': ('out', '4')},
    'glGetSamplerParameterfv': {'params': ('out', '4')}, 'glGetSamplerParameteriv': {'params': ('out', '4')},
    'glGetQueryiv': {'params': ('out', '4')}, 'glGetQueryObjectuiv': {'params': ('out', '4')},
    'glGetVertexAttribfv': {'params': ('out', 'pname == 0x8626 ? 16 : 4')},
    'glGetVertexAttribiv': {'params': ('out', 'pname == 0x8626 ? 16 : 4')},
    'glGetVertexAttribIiv': {'params': ('out', 'pname == 0x8626 ? 16 : 4')},
    'glGetVertexAttribIuiv': {'params': ('out', 'pname == 0x8626 ? 16 : 4')},
    'glGetIntegeri_v': {'data': ('out', '4')}, 'glGetInteger64i_v': {'data': ('out', '8')},
    'glGetActiveUniformBlockiv': {'params': ('out', '4')},
    'glGetProgramInfoLog': {'length': ('out', '4'), 'infoLog': ('out', 'bufSize')},
    'glGetShaderInfoLog': {'length': ('out', '4'), 'infoLog': ('out', 'bufSize')},
    'glGetShaderSource': {'length': ('out', '4'), 'source': ('out', 'bufSize')},
    'glGetActiveAttrib': {'length': ('out', '4'), 'size': ('out', '4'), 'type': ('out', '4'), 'name': ('out', 'bufSize')},
    'glGetActiveUniform': {'length': ('out', '4'), 'size': ('out', '4'), 'type': ('out', '4'), 'name': ('out', 'bufSize')},
    'glGetTransformFeedbackVarying': {'length': ('out', '4'), 'size': ('out', '4'), 'type': ('out', '4'), 'name': ('out', 'bufSize')},
    'glGetActiveUniformBlockName': {'length': ('out', '4'), 'uniformBlockName': ('out', 'bufSize')},
    'glGetProgramBinary': {'length': ('out', '4'), 'binaryFormat': ('out', '4'), 'binary': ('out', 'bufSize')},
    'glGetShaderPrecisionFormat': {'range': ('out', '8'), 'precision': ('out', '4')},
    'glGetAttachedShaders': {'count': ('out', '4'), 'shaders': ('out', 'maxCount * 4')},
    'glGetActiveUniformsiv': {'uniformIndices': ('in', 'uniformCount * 4'), 'params': ('out', 'uniformCount * 4')},
    'glGetInternalformativ': {'params': ('out', '{3} * 4')},
    'glGetSynciv': {'length': ('out', '4'), 'values': ('out', '{2} * 4')},
    # GLES 3.1 (an indirect draw reads its arguments from the bound GL_DRAW_INDIRECT_BUFFER: the pointer is an offset)
    'glDrawArraysIndirect': {'indirect': ('off',)}, 'glDrawElementsIndirect': {'indirect': ('off',)},
    'glGetFramebufferParameteriv': {'params': ('out', '4')}, 'glGetProgramInterfaceiv': {'params': ('out', '4')},
    'glGetProgramResourceIndex': {'name': ('str',)}, 'glGetProgramResourceLocation': {'name': ('str',)},
    'glGetProgramResourceName': {'length': ('out', '4'), 'name': ('out', 'bufSize')},
    'glGetProgramResourceiv': {'props': ('in', 'propCount * 4'), 'length': ('out', '4'), 'params': ('out', 'bufSize * 4')},
    'glGetProgramPipelineiv': {'params': ('out', '4')},
    'glGetProgramPipelineInfoLog': {'length': ('out', '4'), 'infoLog': ('out', 'bufSize')},
    'glGetBooleani_v': {'data': ('out', 'target == 0x0C23 ? 4 : 1')},
    'glGetMultisamplefv': {'val': ('out', '8')},
    'glGetTexLevelParameteriv': {'params': ('out', '4')}, 'glGetTexLevelParameterfv': {'params': ('out', '4')},
}
MAT = {'2': 4, '3': 9, '4': 16, '2x3': 6, '3x2': 6, '2x4': 8, '4x2': 8, '3x4': 12, '4x3': 12}
for name, ret, params in core:
    m = re.match(r'gl(?:Program)?Uniform([1-4])u?[fi]v$', name)
    if m: SPEC[name] = {params[-1][1]: ('in', 'count * %d' % (4 * int(m.group(1))))}
    m = re.match(r'gl(?:Program)?UniformMatrix(\w+)fv$', name)
    if m: SPEC[name] = {params[-1][1]: ('in', 'count * %d' % (4 * MAT[m.group(1)]))}
    m = re.match(r'glVertexAttrib([1-4])fv$', name)
    if m: SPEC[name] = {params[-1][1]: ('in', str(4 * int(m.group(1))))}
    if name in ('glVertexAttribI4iv', 'glVertexAttribI4uiv'): SPEC[name] = {params[-1][1]: ('in', '16')}
    if re.match(r'glDelete\w+s$', name) and len(params) == 2: SPEC[name] = {params[1][1]: ('in', params[0][1] + ' * 4')}
    if re.match(r'glGen\w+s$', name) and len(params) == 2: SPEC[name] = {params[1][1]: ('out', params[0][1] + ' * 4')}

# written by hand on both sides (g: e_<name> in guest.c, h: hd_<name> in host.c)
HAND = '''glVertexAttribPointer glVertexAttribIPointer glDrawArrays glDrawElements glDrawArraysInstanced
glDrawElementsInstanced glDrawRangeElements glShaderSource glGetString glGetStringi glTexImage2D glTexSubImage2D
glTexImage3D glTexSubImage3D glCompressedTexImage2D glCompressedTexSubImage2D glCompressedTexImage3D
glCompressedTexSubImage3D glReadPixels glMapBufferRange glUnmapBuffer glFlushMappedBufferRange glGetBufferPointerv
glGetVertexAttribPointerv glGetUniformfv glGetUniformiv glGetUniformuiv glFinish glFlush glTransformFeedbackVaryings
glGetUniformIndices glCreateShaderProgramv'''.split()
# the guest notes these before sending them (k_<name> in guest.c)
HOOK = '''glPixelStorei glBindVertexArray glDrawArraysIndirect glDrawElementsIndirect glGetVertexAttribfv
glGetVertexAttribiv glGetVertexAttribIiv glGetVertexAttribIuiv
glDeleteBuffers glDeleteVertexArrays glVertexAttribDivisor glGetBooleanv glGetFloatv glGetIntegerv glGetInteger64v
glBufferData glBufferSubData glCopyBufferSubData glBindBufferBase glBindBufferRange glBeginTransformFeedback glDispatchCompute
glDispatchComputeIndirect glMemoryBarrier glMemoryBarrierByRegion'''.split()
# sent by a hand-written e_<name> of guest.c (state the guest keeps and sends only when it changed) through the
# generated encoder, named e0_<name>
LAZY = '''glBindBuffer glEnableVertexAttribArray glDisableVertexAttribArray'''.split()
# the host calls its own function of the same signature (host.c)
HOSTFN = {n: 'h_' + n for n in 'glBindFramebuffer glGetIntegerv glDeleteFramebuffers glReadBuffer glDrawBuffers glInvalidateFramebuffer glInvalidateSubFramebuffer glActiveTexture glBindTexture glTexParameteri glTexParameterf glTexParameteriv glTexParameterfv glGetTexParameteriv glGetTexParameterfv glGenerateMipmap glDeleteTextures glFramebufferTexture2D glLinkProgram glUseProgram glCopyTexImage2D glCopyTexSubImage2D glCopyTexSubImage3D glBlitFramebuffer'.split()}
# entries of extensions that are core functions under another name, and hand-written extension entries
ALIAS = {
    'glBindVertexArrayOES': 'glBindVertexArray', 'glDeleteVertexArraysOES': 'glDeleteVertexArrays',
    'glGenVertexArraysOES': 'glGenVertexArrays', 'glIsVertexArrayOES': 'glIsVertexArray',
    'glDrawArraysInstancedEXT': 'glDrawArraysInstanced', 'glDrawElementsInstancedEXT': 'glDrawElementsInstanced',
    'glDrawArraysInstancedANGLE': 'glDrawArraysInstanced', 'glDrawElementsInstancedANGLE': 'glDrawElementsInstanced',
    'glVertexAttribDivisorEXT': 'glVertexAttribDivisor', 'glVertexAttribDivisorANGLE': 'glVertexAttribDivisor',
    'glDrawBuffersEXT': 'glDrawBuffers', 'glBlitFramebufferANGLE': 'glBlitFramebuffer',
    'glRenderbufferStorageMultisampleANGLE': 'glRenderbufferStorageMultisample',
    'glTexStorage2DEXT': 'glTexStorage2D', 'glMapBufferRangeEXT': 'glMapBufferRange',
    'glFlushMappedBufferRangeEXT': 'glFlushMappedBufferRange', 'glUnmapBufferOES': 'glUnmapBuffer',
    'glGetBufferPointervOES': 'glGetBufferPointerv', 'glDiscardFramebufferEXT': 'glInvalidateFramebuffer',
    'glTexImage3DOES': 'glTexImage3D', 'glTexSubImage3DOES': 'glTexSubImage3D',
    'glGetProgramBinaryOES': 'glGetProgramBinary', 'glProgramBinaryOES': 'glProgramBinary',
}
HAND_EXT = ['glEGLImageTargetTexture2DOES', 'glMapBufferOES', 'glGetGraphicsResetStatusEXT', 'glGetGraphicsResetStatusKHR',
            'glReadnPixelsEXT', 'glReadnPixelsKHR', 'glGetnUniformfvEXT', 'glGetnUniformivEXT', 'glGetnUniformfvKHR', 'glGetnUniformivKHR']


def kind(t):
    if '*' in t: return 'ptr'
    if t in ('GLfloat', 'GLclampf'): return 'f'
    if t in ('GLintptr', 'GLsizeiptr', 'GLint64', 'GLuint64', 'GLsync'): return 'q'
    return 'i'


def proto(f, prefix='', static=False):
    name, ret, params = f
    return '%s%s %s%s(%s)' % ('static ' if static else '', ret, prefix, name,
                              ', '.join('%s %s' % p for p in params) or 'void')


def fnptr(f):
    return '(%s (*)(%s))' % (f[1], ', '.join(p[0] for p in f[2]) or 'void')


def expr(e, params):
    return re.sub(r'\{(\d+)\}', lambda m: params[int(m.group(1))][1], e)


g = ['/* written by gen.py */', KNOWN] + others + ['', 'enum {'] + ['    IDX_%s,' % f[0] for f in funcs] + ['    N_FUNCS', '};',
     '#define N_CORE %d' % len(core), 'static const char *const kNames[N_FUNCS] = {'] + ['    "%s",' % f[0] for f in funcs] + ['};', '']
for f in funcs:
    call = '(%sTBL()[IDX_%s])(%s)' % (fnptr(f), f[0], ', '.join(p[1] for p in f[2]))
    g.append('EXPORT %s { %s%s; }' % (proto(f), '' if f[1] == 'void' else 'return ', call))
g += ['static void k_%s(%s);' % (n, ', '.join('%s %s' % p for p in funcs[index[n]][2])) for n in HOOK]
g += ['', 'static void *const kStubs[N_FUNCS] = {'] + ['    (void *)%s,' % f[0] for f in funcs] + ['};', '']

h = ['/* written by gen.py */', 'static void dispatch(unsigned op, const uint8_t *p, uint32_t len) {', '    (void)len;', '    switch (op) {']
unsupported = []
enc = {}
for f in core:
    name, ret, params = f
    spec = SPEC.get(name, {})
    if name in HAND:
        g.append(proto(f, 'e_', True) + ';')
        h.append('    case %d: hd_%s(p, len); break;' % (index[name], name))
        enc[name] = 'e_' + name
        continue
    if any(kind(t) == 'ptr' and n not in spec for t, n in params) or kind(ret) == 'ptr':
        unsupported.append(name)
        continue
    enc[name] = 'e_' + name
    sync = ret != 'void' or any(s[0] == 'out' for s in spec.values())
    fixed, pre, puts, tot, outs = 0, [], [], [], []
    hd, hargs, hrep = [], [], []
    for t, n in params:
        k = kind(t)
        if k == 'i':
            fixed += 4; puts.append('put32(T, (uint32_t)%s);' % n); hd.append('%s %s = (%s)rd32(&p);' % (t, n, t))
        elif k == 'f':
            fixed += 4; puts.append('putf(T, %s);' % n); hd.append('%s %s = rdf(&p);' % (t, n))
        elif k == 'q':
            fixed += 8; puts.append('put64(T, (uint64_t)%s);' % n); hd.append('%s %s = (%s)rd64(&p);' % (t, n, t))
        else:
            s = spec[n]
            if s[0] == 'off':
                fixed += 8; puts.append('put64(T, (uint64_t)(uintptr_t)%s);' % n); hd.append('%s %s = (%s)(uintptr_t)rd64(&p);' % (t, n, t))
            elif s[0] == 'str':
                fixed += 4
                pre.append('uint32_t %s_n = %s ? (uint32_t)strlen(%s) + 1 : NIL;' % (n, n, n))
                tot.append('BLOB(%s_n)' % n); puts.append('putblob(T, %s, %s_n);' % (n, n))
                hd.append('uint32_t %s_n; %s %s = (%s)rdblob(&p, &%s_n);' % (n, t, n, t, n))
            elif s[0] == 'in':
                fixed += 4
                pre.append('uint32_t %s_n = %s ? %s : NIL;' % (n, n, SZ(expr(s[1], params))))
                tot.append('BLOB(%s_n)' % n); puts.append('putblob(T, %s, %s_n);' % (n, n))
                hd.append('uint32_t %s_n; %s %s = (%s)rdblob(&p, &%s_n);' % (n, t, n, t, n))
            else:
                e = SZ(expr(s[1], params))
                outs.append('get(T, %s, %s);' % (n, e))
                hd.append('uint32_t %s_n = %s; %s %s = (%s)scratch(%s_n);' % (n, e, t, n, t, n))
                hrep.append('reply(%s, %s_n);' % (n, n))
        hargs.append(n)
    body = ['struct ts *T = TS();']
    if name in HOOK: body.append('k_%s(%s);' % (name, ', '.join(p[1] for p in params)))
    body += pre
    body.append('begin(T, %d, %d, %s);' % (index[name], fixed, ' + '.join(['%d' % fixed] + tot)))
    body += puts
    if sync:
        body.append('rsync(T);')
        body += outs
        if ret != 'void':
            body.append('return (%s)%s;' % (ret, 'get64v(T)' if kind(ret) == 'q' else 'get32v(T)'))
    if name in LAZY:
        g.append(proto(f, 'e_', True) + ';')
        g.append(proto(f, 'e0_', True) + ' { ' + ' '.join(body) + ' }')
    else:
        g.append(proto(f, 'e_', True) + ' { ' + ' '.join(body) + ' }')
    call = '%s(%s)' % (HOSTFN.get(name, name), ', '.join(hargs))
    if ret != 'void':
        call = '%s r = %s; ' % (ret, call)
        hrep.append('reply64((uint64_t)r);' if kind(ret) == 'q' else 'reply32((uint32_t)r);')
    else:
        call += ';'
    h.append('    case %d: { %s %s %s break; }' % (index[name], ' '.join(hd), call, ' '.join(hrep)))
h += ['    default: hd_other(op, p, len); break;', '    }', '}']
for n in HAND_EXT:
    g.append(proto(funcs[index[n]], 'e_', True) + ';')
    enc[n] = 'e_' + n
for a, b in ALIAS.items():
    if a in index and b in enc: enc[a] = enc[b]
g += ['', 'static void *const kEnc[N_FUNCS] = {'] + ['    [IDX_%s] = (void *)%s,' % (n, e) for n, e in enc.items()] + ['};']
open('g_api.h', 'w', newline='\n').write('\n'.join(g) + '\n')
open('h_api.h', 'w', newline='\n').write('\n'.join(h) + '\n')
print('%d entries, %d of the %d core ones encoded; not carried: %s' % (len(funcs), len([n for n in enc if index[n] < len(core)]), len(core), ' '.join(unsupported)))
