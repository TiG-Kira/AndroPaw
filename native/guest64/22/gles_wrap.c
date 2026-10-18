/* Kernel 22 (Android 5.1): the name libEGL and SwiftShader's own libEGL load as the GLES 2 library. Everything goes on
 * to SwiftShader's libGLESv2, kept next to it as libGLESv2real_swiftshader.so (a name libEGL's own search does not
 * match), except:
 *   glShaderSource — SwiftShader's compiler takes `smooth` for a reserved word in GLSL ES 1.00 too, where it is an
 *   ordinary name (it became a keyword in 3.00); a vendor's shaders written against its GPU driver use it as one
 *   (TouchWiz: the water droplet effect of the lock screen has `float smooth`, its shader failed to compile and the
 *   effect drew black squares). In a shader that does not declare version 300 the name is spelled differently. */
#define EXPORT __attribute__((visibility("default")))
#define REAL "/system/lib64/egl/libGLESv2real_swiftshader.so"

extern void *dlopen(const char *, int);
extern void *dlsym(void *, const char *);
extern void *malloc(unsigned long);
extern void free(void *);

#define FORWARDS(F) \
    F(glActiveTexture) F(glAttachShader) F(glBeginQuery) F(glBeginQueryEXT) F(glBeginTransformFeedback) F(glBindAttribLocation) \
    F(glBindBuffer) F(glBindBufferBase) F(glBindBufferRange) F(glBindFramebuffer) F(glBindFramebufferOES) F(glBindRenderbuffer) \
    F(glBindRenderbufferOES) F(glBindSampler) F(glBindTexture) F(glBindTransformFeedback) F(glBindVertexArray) F(glBindVertexArrayOES) \
    F(glBlendColor) F(glBlendEquation) F(glBlendEquationSeparate) F(glBlendFunc) F(glBlendFuncSeparate) F(glBlitFramebuffer) \
    F(glBlitFramebufferANGLE) F(glBufferData) F(glBufferSubData) F(glCheckFramebufferStatus) F(glCheckFramebufferStatusOES) F(glClear) \
    F(glClearBufferfi) F(glClearBufferfv) F(glClearBufferiv) F(glClearBufferuiv) F(glClearColor) F(glClearDepthf) \
    F(glClearStencil) F(glClientWaitSync) F(glColorMask) F(glCompileShader) F(glCompressedTexImage2D) F(glCompressedTexImage3D) \
    F(glCompressedTexSubImage2D) F(glCompressedTexSubImage3D) F(glCopyBufferSubData) F(glCopyTexImage2D) F(glCopyTexSubImage2D) F(glCopyTexSubImage3D) \
    F(glCreateProgram) F(glCreateShader) F(glCullFace) F(glDeleteBuffers) F(glDeleteFencesNV) F(glDeleteFramebuffers) \
    F(glDeleteFramebuffersOES) F(glDeleteProgram) F(glDeleteQueries) F(glDeleteQueriesEXT) F(glDeleteRenderbuffers) F(glDeleteRenderbuffersOES) \
    F(glDeleteSamplers) F(glDeleteShader) F(glDeleteSync) F(glDeleteTextures) F(glDeleteTransformFeedbacks) F(glDeleteVertexArrays) \
    F(glDeleteVertexArraysOES) F(glDepthFunc) F(glDepthMask) F(glDepthRangef) F(glDetachShader) F(glDisable) \
    F(glDisableVertexAttribArray) F(glDrawArrays) F(glDrawArraysInstanced) F(glDrawBuffers) F(glDrawBuffersEXT) F(glDrawElements) \
    F(glDrawElementsInstanced) F(glDrawRangeElements) F(glEGLImageTargetRenderbufferStorageOES) F(glEGLImageTargetTexture2DOES) F(glEnable) F(glEnableVertexAttribArray) \
    F(glEndQuery) F(glEndQueryEXT) F(glEndTransformFeedback) F(glFenceSync) F(glFinish) F(glFinishFenceNV) \
    F(glFlush) F(glFlushMappedBufferRange) F(glFramebufferRenderbuffer) F(glFramebufferRenderbufferOES) F(glFramebufferTexture2D) F(glFramebufferTexture2DOES) \
    F(glFramebufferTextureLayer) F(glFrontFace) F(glGenBuffers) F(glGenFencesNV) F(glGenFramebuffers) F(glGenFramebuffersOES) \
    F(glGenQueries) F(glGenQueriesEXT) F(glGenRenderbuffers) F(glGenRenderbuffersOES) F(glGenSamplers) F(glGenTextures) \
    F(glGenTransformFeedbacks) F(glGenVertexArrays) F(glGenVertexArraysOES) F(glGenerateMipmap) F(glGenerateMipmapOES) F(glGetActiveAttrib) \
    F(glGetActiveUniform) F(glGetActiveUniformBlockName) F(glGetActiveUniformBlockiv) F(glGetActiveUniformsiv) F(glGetAttachedShaders) F(glGetAttribLocation) \
    F(glGetBooleanv) F(glGetBufferParameteri64v) F(glGetBufferParameteriv) F(glGetBufferPointerv) F(glGetError) F(glGetFenceivNV) \
    F(glGetFloatv) F(glGetFragDataLocation) F(glGetFramebufferAttachmentParameteriv) F(glGetFramebufferAttachmentParameterivOES) F(glGetGraphicsResetStatusEXT) F(glGetInteger64i_v) \
    F(glGetInteger64v) F(glGetIntegeri_v) F(glGetIntegerv) F(glGetInternalformativ) F(glGetProgramBinary) F(glGetProgramInfoLog) \
    F(glGetProgramiv) F(glGetQueryObjectuiv) F(glGetQueryObjectuivEXT) F(glGetQueryiv) F(glGetQueryivEXT) F(glGetRenderbufferParameteriv) \
    F(glGetRenderbufferParameterivOES) F(glGetSamplerParameterfv) F(glGetSamplerParameteriv) F(glGetShaderInfoLog) F(glGetShaderPrecisionFormat) F(glGetShaderSource) \
    F(glGetShaderiv) F(glGetString) F(glGetStringi) F(glGetSynciv) F(glGetTexParameterfv) F(glGetTexParameteriv) \
    F(glGetTransformFeedbackVarying) F(glGetUniformBlockIndex) F(glGetUniformIndices) F(glGetUniformLocation) F(glGetUniformfv) F(glGetUniformiv) \
    F(glGetUniformuiv) F(glGetVertexAttribIiv) F(glGetVertexAttribIuiv) F(glGetVertexAttribPointerv) F(glGetVertexAttribfv) F(glGetVertexAttribiv) \
    F(glGetnUniformfvEXT) F(glGetnUniformivEXT) F(glHint) F(glInvalidateFramebuffer) F(glInvalidateSubFramebuffer) F(glIsBuffer) \
    F(glIsEnabled) F(glIsFenceNV) F(glIsFramebuffer) F(glIsFramebufferOES) F(glIsProgram) F(glIsQuery) \
    F(glIsQueryEXT) F(glIsRenderbuffer) F(glIsRenderbufferOES) F(glIsSampler) F(glIsShader) F(glIsSync) \
    F(glIsTexture) F(glIsTransformFeedback) F(glIsVertexArray) F(glIsVertexArrayOES) F(glLineWidth) F(glLinkProgram) \
    F(glMapBufferRange) F(glPauseTransformFeedback) F(glPixelStorei) F(glPolygonOffset) F(glProgramBinary) F(glProgramParameteri) \
    F(glReadBuffer) F(glReadPixels) F(glReadnPixelsEXT) F(glReleaseShaderCompiler) F(glRenderbufferStorage) F(glRenderbufferStorageMultisample) \
    F(glRenderbufferStorageMultisampleANGLE) F(glRenderbufferStorageOES) F(glResumeTransformFeedback) F(glSampleCoverage) F(glSamplerParameterf) F(glSamplerParameterfv) \
    F(glSamplerParameteri) F(glSamplerParameteriv) F(glScissor) F(glSetFenceNV) F(glShaderBinary) F(glStencilFunc) \
    F(glStencilFuncSeparate) F(glStencilMask) F(glStencilMaskSeparate) F(glStencilOp) F(glStencilOpSeparate) F(glTestFenceNV) \
    F(glTexImage2D) F(glTexImage3D) F(glTexImage3DOES) F(glTexParameterf) F(glTexParameterfv) F(glTexParameteri) \
    F(glTexParameteriv) F(glTexStorage2D) F(glTexStorage3D) F(glTexSubImage2D) F(glTexSubImage3D) F(glTransformFeedbackVaryings) \
    F(glUniform1f) F(glUniform1fv) F(glUniform1i) F(glUniform1iv) F(glUniform1ui) F(glUniform1uiv) \
    F(glUniform2f) F(glUniform2fv) F(glUniform2i) F(glUniform2iv) F(glUniform2ui) F(glUniform2uiv) \
    F(glUniform3f) F(glUniform3fv) F(glUniform3i) F(glUniform3iv) F(glUniform3ui) F(glUniform3uiv) \
    F(glUniform4f) F(glUniform4fv) F(glUniform4i) F(glUniform4iv) F(glUniform4ui) F(glUniform4uiv) \
    F(glUniformBlockBinding) F(glUniformMatrix2fv) F(glUniformMatrix2x3fv) F(glUniformMatrix2x4fv) F(glUniformMatrix3fv) F(glUniformMatrix3x2fv) \
    F(glUniformMatrix3x4fv) F(glUniformMatrix4fv) F(glUniformMatrix4x2fv) F(glUniformMatrix4x3fv) F(glUnmapBuffer) F(glUseProgram) \
    F(glValidateProgram) F(glVertexAttrib1f) F(glVertexAttrib1fv) F(glVertexAttrib2f) F(glVertexAttrib2fv) F(glVertexAttrib3f) \
    F(glVertexAttrib3fv) F(glVertexAttrib4f) F(glVertexAttrib4fv) F(glVertexAttribDivisor) F(glVertexAttribI4i) F(glVertexAttribI4iv) \
    F(glVertexAttribI4ui) F(glVertexAttribI4uiv) F(glVertexAttribIPointer) F(glVertexAttribPointer) F(glViewport) F(glWaitSync) \
    F(libGLESv2_swiftshader)

/* one pointer and one jump per forwarded call: the arguments pass through untouched */
#define DECL(n) void *aemu_p_##n; \
    __asm__(".text\n.globl " #n "\n.type " #n ",%function\n" #n ":\n" \
            "  adrp x16, aemu_p_" #n "\n  ldr x16, [x16, :lo12:aemu_p_" #n "]\n  br x16\n");
FORWARDS(DECL)

static void (*real_shader_source)(unsigned, int, const char *const *, const int *);

static int word(char c) { return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_'; }

static int starts(const char *s, const char *e, const char *w) {
    for (; *w; s++, w++) if (s >= e || *s != *w) return 0;
    return 1;
}

EXPORT void glShaderSource(unsigned shader, int count, const char *const *strings, const int *lengths) {
    unsigned long total = 0;
    int i, found = 0, es3 = 0;
    char *all, *p, *e;
    if (count <= 0 || !strings) { real_shader_source(shader, count, strings, lengths); return; }
    for (i = 0; i < count; i++) {
        const char *s = strings[i];
        unsigned long n = 0;
        if (!s) continue;
        if (lengths && lengths[i] >= 0) n = (unsigned long)lengths[i]; else while (s[n]) n++;
        total += n;
    }
    all = malloc(total + 1);
    if (!all) { real_shader_source(shader, count, strings, lengths); return; }
    for (p = all, i = 0; i < count; i++) {
        const char *s = strings[i];
        unsigned long n = 0, k;
        if (!s) continue;
        if (lengths && lengths[i] >= 0) n = (unsigned long)lengths[i]; else while (s[n]) n++;
        for (k = 0; k < n; k++) *p++ = s[k];
    }
    *p = 0;
    e = p;
    for (p = all; p < e; p++) {
        if (*p == '#' && !es3) {
            char *q = p + 1;
            while (q < e && (*q == ' ' || *q == '\t')) q++;
            if (starts(q, e, "version")) { q += 7; while (q < e && (*q == ' ' || *q == '\t')) q++; if (starts(q, e, "300")) es3 = 1; }
        }
        if (*p == 's' && starts(p, e, "smooth") && (p == all || !word(p[-1])) && (p + 6 >= e || !word(p[6]))) { p[5] = 'H'; found = 1; }
    }
    if (found && !es3) {
        const char *one = all;
        real_shader_source(shader, 1, &one, 0);
    } else real_shader_source(shader, count, strings, lengths);
    free(all);
}

__attribute__((constructor)) static void init(void) {
    void *h = dlopen(REAL, 2 /* RTLD_NOW */);
    if (!h) return;
#define LOAD(n) aemu_p_##n = dlsym(h, #n);
    FORWARDS(LOAD)
    real_shader_source = (void (*)(unsigned, int, const char *const *, const int *))dlsym(h, "glShaderSource");
}
