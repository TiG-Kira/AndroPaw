/* written by gen.py */
typedef unsigned int GLenum, GLuint, GLbitfield;
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

typedef void * GLDEBUGPROCKHR;
typedef void * GLeglImageOES;
typedef void * GLeglClientBufferEXT;
typedef void * GLVULKANPROCNV;
typedef int64_t GLint64EXT;
typedef int64_t GLuint64EXT;
typedef int GLdouble;

enum {
    IDX_glActiveTexture,
    IDX_glAttachShader,
    IDX_glBindAttribLocation,
    IDX_glBindBuffer,
    IDX_glBindFramebuffer,
    IDX_glBindRenderbuffer,
    IDX_glBindTexture,
    IDX_glBlendColor,
    IDX_glBlendEquation,
    IDX_glBlendEquationSeparate,
    IDX_glBlendFunc,
    IDX_glBlendFuncSeparate,
    IDX_glBufferData,
    IDX_glBufferSubData,
    IDX_glCheckFramebufferStatus,
    IDX_glClear,
    IDX_glClearColor,
    IDX_glClearDepthf,
    IDX_glClearStencil,
    IDX_glColorMask,
    IDX_glCompileShader,
    IDX_glCompressedTexImage2D,
    IDX_glCompressedTexSubImage2D,
    IDX_glCopyTexImage2D,
    IDX_glCopyTexSubImage2D,
    IDX_glCreateProgram,
    IDX_glCreateShader,
    IDX_glCullFace,
    IDX_glDeleteBuffers,
    IDX_glDeleteFramebuffers,
    IDX_glDeleteProgram,
    IDX_glDeleteRenderbuffers,
    IDX_glDeleteShader,
    IDX_glDeleteTextures,
    IDX_glDepthFunc,
    IDX_glDepthMask,
    IDX_glDepthRangef,
    IDX_glDetachShader,
    IDX_glDisable,
    IDX_glDisableVertexAttribArray,
    IDX_glDrawArrays,
    IDX_glDrawElements,
    IDX_glEnable,
    IDX_glEnableVertexAttribArray,
    IDX_glFinish,
    IDX_glFlush,
    IDX_glFramebufferRenderbuffer,
    IDX_glFramebufferTexture2D,
    IDX_glFrontFace,
    IDX_glGenBuffers,
    IDX_glGenerateMipmap,
    IDX_glGenFramebuffers,
    IDX_glGenRenderbuffers,
    IDX_glGenTextures,
    IDX_glGetActiveAttrib,
    IDX_glGetActiveUniform,
    IDX_glGetAttachedShaders,
    IDX_glGetAttribLocation,
    IDX_glGetBooleanv,
    IDX_glGetBufferParameteriv,
    IDX_glGetError,
    IDX_glGetFloatv,
    IDX_glGetFramebufferAttachmentParameteriv,
    IDX_glGetIntegerv,
    IDX_glGetProgramiv,
    IDX_glGetProgramInfoLog,
    IDX_glGetRenderbufferParameteriv,
    IDX_glGetShaderiv,
    IDX_glGetShaderInfoLog,
    IDX_glGetShaderPrecisionFormat,
    IDX_glGetShaderSource,
    IDX_glGetString,
    IDX_glGetTexParameterfv,
    IDX_glGetTexParameteriv,
    IDX_glGetUniformfv,
    IDX_glGetUniformiv,
    IDX_glGetUniformLocation,
    IDX_glGetVertexAttribfv,
    IDX_glGetVertexAttribiv,
    IDX_glGetVertexAttribPointerv,
    IDX_glHint,
    IDX_glIsBuffer,
    IDX_glIsEnabled,
    IDX_glIsFramebuffer,
    IDX_glIsProgram,
    IDX_glIsRenderbuffer,
    IDX_glIsShader,
    IDX_glIsTexture,
    IDX_glLineWidth,
    IDX_glLinkProgram,
    IDX_glPixelStorei,
    IDX_glPolygonOffset,
    IDX_glReadPixels,
    IDX_glReleaseShaderCompiler,
    IDX_glRenderbufferStorage,
    IDX_glSampleCoverage,
    IDX_glScissor,
    IDX_glShaderBinary,
    IDX_glShaderSource,
    IDX_glStencilFunc,
    IDX_glStencilFuncSeparate,
    IDX_glStencilMask,
    IDX_glStencilMaskSeparate,
    IDX_glStencilOp,
    IDX_glStencilOpSeparate,
    IDX_glTexImage2D,
    IDX_glTexParameterf,
    IDX_glTexParameterfv,
    IDX_glTexParameteri,
    IDX_glTexParameteriv,
    IDX_glTexSubImage2D,
    IDX_glUniform1f,
    IDX_glUniform1fv,
    IDX_glUniform1i,
    IDX_glUniform1iv,
    IDX_glUniform2f,
    IDX_glUniform2fv,
    IDX_glUniform2i,
    IDX_glUniform2iv,
    IDX_glUniform3f,
    IDX_glUniform3fv,
    IDX_glUniform3i,
    IDX_glUniform3iv,
    IDX_glUniform4f,
    IDX_glUniform4fv,
    IDX_glUniform4i,
    IDX_glUniform4iv,
    IDX_glUniformMatrix2fv,
    IDX_glUniformMatrix3fv,
    IDX_glUniformMatrix4fv,
    IDX_glUseProgram,
    IDX_glValidateProgram,
    IDX_glVertexAttrib1f,
    IDX_glVertexAttrib1fv,
    IDX_glVertexAttrib2f,
    IDX_glVertexAttrib2fv,
    IDX_glVertexAttrib3f,
    IDX_glVertexAttrib3fv,
    IDX_glVertexAttrib4f,
    IDX_glVertexAttrib4fv,
    IDX_glVertexAttribPointer,
    IDX_glViewport,
    IDX_glReadBuffer,
    IDX_glDrawRangeElements,
    IDX_glTexImage3D,
    IDX_glTexSubImage3D,
    IDX_glCopyTexSubImage3D,
    IDX_glCompressedTexImage3D,
    IDX_glCompressedTexSubImage3D,
    IDX_glGenQueries,
    IDX_glDeleteQueries,
    IDX_glIsQuery,
    IDX_glBeginQuery,
    IDX_glEndQuery,
    IDX_glGetQueryiv,
    IDX_glGetQueryObjectuiv,
    IDX_glUnmapBuffer,
    IDX_glGetBufferPointerv,
    IDX_glDrawBuffers,
    IDX_glUniformMatrix2x3fv,
    IDX_glUniformMatrix3x2fv,
    IDX_glUniformMatrix2x4fv,
    IDX_glUniformMatrix4x2fv,
    IDX_glUniformMatrix3x4fv,
    IDX_glUniformMatrix4x3fv,
    IDX_glBlitFramebuffer,
    IDX_glRenderbufferStorageMultisample,
    IDX_glFramebufferTextureLayer,
    IDX_glMapBufferRange,
    IDX_glFlushMappedBufferRange,
    IDX_glBindVertexArray,
    IDX_glDeleteVertexArrays,
    IDX_glGenVertexArrays,
    IDX_glIsVertexArray,
    IDX_glGetIntegeri_v,
    IDX_glBeginTransformFeedback,
    IDX_glEndTransformFeedback,
    IDX_glBindBufferRange,
    IDX_glBindBufferBase,
    IDX_glTransformFeedbackVaryings,
    IDX_glGetTransformFeedbackVarying,
    IDX_glVertexAttribIPointer,
    IDX_glGetVertexAttribIiv,
    IDX_glGetVertexAttribIuiv,
    IDX_glVertexAttribI4i,
    IDX_glVertexAttribI4ui,
    IDX_glVertexAttribI4iv,
    IDX_glVertexAttribI4uiv,
    IDX_glGetUniformuiv,
    IDX_glGetFragDataLocation,
    IDX_glUniform1ui,
    IDX_glUniform2ui,
    IDX_glUniform3ui,
    IDX_glUniform4ui,
    IDX_glUniform1uiv,
    IDX_glUniform2uiv,
    IDX_glUniform3uiv,
    IDX_glUniform4uiv,
    IDX_glClearBufferiv,
    IDX_glClearBufferuiv,
    IDX_glClearBufferfv,
    IDX_glClearBufferfi,
    IDX_glGetStringi,
    IDX_glCopyBufferSubData,
    IDX_glGetUniformIndices,
    IDX_glGetActiveUniformsiv,
    IDX_glGetUniformBlockIndex,
    IDX_glGetActiveUniformBlockiv,
    IDX_glGetActiveUniformBlockName,
    IDX_glUniformBlockBinding,
    IDX_glDrawArraysInstanced,
    IDX_glDrawElementsInstanced,
    IDX_glFenceSync,
    IDX_glIsSync,
    IDX_glDeleteSync,
    IDX_glClientWaitSync,
    IDX_glWaitSync,
    IDX_glGetInteger64v,
    IDX_glGetSynciv,
    IDX_glGetInteger64i_v,
    IDX_glGetBufferParameteri64v,
    IDX_glGenSamplers,
    IDX_glDeleteSamplers,
    IDX_glIsSampler,
    IDX_glBindSampler,
    IDX_glSamplerParameteri,
    IDX_glSamplerParameteriv,
    IDX_glSamplerParameterf,
    IDX_glSamplerParameterfv,
    IDX_glGetSamplerParameteriv,
    IDX_glGetSamplerParameterfv,
    IDX_glVertexAttribDivisor,
    IDX_glBindTransformFeedback,
    IDX_glDeleteTransformFeedbacks,
    IDX_glGenTransformFeedbacks,
    IDX_glIsTransformFeedback,
    IDX_glPauseTransformFeedback,
    IDX_glResumeTransformFeedback,
    IDX_glGetProgramBinary,
    IDX_glProgramBinary,
    IDX_glProgramParameteri,
    IDX_glInvalidateFramebuffer,
    IDX_glInvalidateSubFramebuffer,
    IDX_glTexStorage2D,
    IDX_glTexStorage3D,
    IDX_glGetInternalformativ,
    IDX_glDispatchCompute,
    IDX_glDispatchComputeIndirect,
    IDX_glDrawArraysIndirect,
    IDX_glDrawElementsIndirect,
    IDX_glFramebufferParameteri,
    IDX_glGetFramebufferParameteriv,
    IDX_glGetProgramInterfaceiv,
    IDX_glGetProgramResourceIndex,
    IDX_glGetProgramResourceName,
    IDX_glGetProgramResourceiv,
    IDX_glGetProgramResourceLocation,
    IDX_glUseProgramStages,
    IDX_glActiveShaderProgram,
    IDX_glCreateShaderProgramv,
    IDX_glBindProgramPipeline,
    IDX_glDeleteProgramPipelines,
    IDX_glGenProgramPipelines,
    IDX_glIsProgramPipeline,
    IDX_glGetProgramPipelineiv,
    IDX_glProgramUniform1i,
    IDX_glProgramUniform2i,
    IDX_glProgramUniform3i,
    IDX_glProgramUniform4i,
    IDX_glProgramUniform1ui,
    IDX_glProgramUniform2ui,
    IDX_glProgramUniform3ui,
    IDX_glProgramUniform4ui,
    IDX_glProgramUniform1f,
    IDX_glProgramUniform2f,
    IDX_glProgramUniform3f,
    IDX_glProgramUniform4f,
    IDX_glProgramUniform1iv,
    IDX_glProgramUniform2iv,
    IDX_glProgramUniform3iv,
    IDX_glProgramUniform4iv,
    IDX_glProgramUniform1uiv,
    IDX_glProgramUniform2uiv,
    IDX_glProgramUniform3uiv,
    IDX_glProgramUniform4uiv,
    IDX_glProgramUniform1fv,
    IDX_glProgramUniform2fv,
    IDX_glProgramUniform3fv,
    IDX_glProgramUniform4fv,
    IDX_glProgramUniformMatrix2fv,
    IDX_glProgramUniformMatrix3fv,
    IDX_glProgramUniformMatrix4fv,
    IDX_glProgramUniformMatrix2x3fv,
    IDX_glProgramUniformMatrix3x2fv,
    IDX_glProgramUniformMatrix2x4fv,
    IDX_glProgramUniformMatrix4x2fv,
    IDX_glProgramUniformMatrix3x4fv,
    IDX_glProgramUniformMatrix4x3fv,
    IDX_glValidateProgramPipeline,
    IDX_glGetProgramPipelineInfoLog,
    IDX_glBindImageTexture,
    IDX_glGetBooleani_v,
    IDX_glMemoryBarrier,
    IDX_glMemoryBarrierByRegion,
    IDX_glTexStorage2DMultisample,
    IDX_glGetMultisamplefv,
    IDX_glSampleMaski,
    IDX_glGetTexLevelParameteriv,
    IDX_glGetTexLevelParameterfv,
    IDX_glBindVertexBuffer,
    IDX_glVertexAttribFormat,
    IDX_glVertexAttribIFormat,
    IDX_glVertexAttribBinding,
    IDX_glVertexBindingDivisor,
    IDX_glBlendBarrierKHR,
    IDX_glDebugMessageControlKHR,
    IDX_glDebugMessageInsertKHR,
    IDX_glDebugMessageCallbackKHR,
    IDX_glGetDebugMessageLogKHR,
    IDX_glPushDebugGroupKHR,
    IDX_glPopDebugGroupKHR,
    IDX_glObjectLabelKHR,
    IDX_glGetObjectLabelKHR,
    IDX_glObjectPtrLabelKHR,
    IDX_glGetObjectPtrLabelKHR,
    IDX_glGetPointervKHR,
    IDX_glMaxShaderCompilerThreadsKHR,
    IDX_glGetGraphicsResetStatusKHR,
    IDX_glReadnPixelsKHR,
    IDX_glGetnUniformfvKHR,
    IDX_glGetnUniformivKHR,
    IDX_glGetnUniformuivKHR,
    IDX_glEGLImageTargetTexture2DOES,
    IDX_glEGLImageTargetRenderbufferStorageOES,
    IDX_glCopyImageSubDataOES,
    IDX_glEnableiOES,
    IDX_glDisableiOES,
    IDX_glBlendEquationiOES,
    IDX_glBlendEquationSeparateiOES,
    IDX_glBlendFunciOES,
    IDX_glBlendFuncSeparateiOES,
    IDX_glColorMaskiOES,
    IDX_glIsEnablediOES,
    IDX_glDrawElementsBaseVertexOES,
    IDX_glDrawRangeElementsBaseVertexOES,
    IDX_glDrawElementsInstancedBaseVertexOES,
    IDX_glMultiDrawElementsBaseVertexEXT,
    IDX_glFramebufferTextureOES,
    IDX_glGetProgramBinaryOES,
    IDX_glProgramBinaryOES,
    IDX_glMapBufferOES,
    IDX_glUnmapBufferOES,
    IDX_glGetBufferPointervOES,
    IDX_glPrimitiveBoundingBoxOES,
    IDX_glMinSampleShadingOES,
    IDX_glPatchParameteriOES,
    IDX_glTexImage3DOES,
    IDX_glTexSubImage3DOES,
    IDX_glCopyTexSubImage3DOES,
    IDX_glCompressedTexImage3DOES,
    IDX_glCompressedTexSubImage3DOES,
    IDX_glFramebufferTexture3DOES,
    IDX_glTexParameterIivOES,
    IDX_glTexParameterIuivOES,
    IDX_glGetTexParameterIivOES,
    IDX_glGetTexParameterIuivOES,
    IDX_glSamplerParameterIivOES,
    IDX_glSamplerParameterIuivOES,
    IDX_glGetSamplerParameterIivOES,
    IDX_glGetSamplerParameterIuivOES,
    IDX_glTexBufferOES,
    IDX_glTexBufferRangeOES,
    IDX_glTexStorage3DMultisampleOES,
    IDX_glTextureViewOES,
    IDX_glBindVertexArrayOES,
    IDX_glDeleteVertexArraysOES,
    IDX_glGenVertexArraysOES,
    IDX_glIsVertexArrayOES,
    IDX_glViewportArrayvOES,
    IDX_glViewportIndexedfOES,
    IDX_glViewportIndexedfvOES,
    IDX_glScissorArrayvOES,
    IDX_glScissorIndexedOES,
    IDX_glScissorIndexedvOES,
    IDX_glDepthRangeArrayfvOES,
    IDX_glDepthRangeIndexedfOES,
    IDX_glGetFloati_vOES,
    IDX_glGetPerfMonitorGroupsAMD,
    IDX_glGetPerfMonitorCountersAMD,
    IDX_glGetPerfMonitorGroupStringAMD,
    IDX_glGetPerfMonitorCounterStringAMD,
    IDX_glGetPerfMonitorCounterInfoAMD,
    IDX_glGenPerfMonitorsAMD,
    IDX_glDeletePerfMonitorsAMD,
    IDX_glSelectPerfMonitorCountersAMD,
    IDX_glBeginPerfMonitorAMD,
    IDX_glEndPerfMonitorAMD,
    IDX_glGetPerfMonitorCounterDataAMD,
    IDX_glBlitFramebufferANGLE,
    IDX_glRenderbufferStorageMultisampleANGLE,
    IDX_glDrawArraysInstancedANGLE,
    IDX_glDrawElementsInstancedANGLE,
    IDX_glVertexAttribDivisorANGLE,
    IDX_glGetTranslatedShaderSourceANGLE,
    IDX_glCopyTextureLevelsAPPLE,
    IDX_glRenderbufferStorageMultisampleAPPLE,
    IDX_glResolveMultisampleFramebufferAPPLE,
    IDX_glFenceSyncAPPLE,
    IDX_glIsSyncAPPLE,
    IDX_glDeleteSyncAPPLE,
    IDX_glClientWaitSyncAPPLE,
    IDX_glWaitSyncAPPLE,
    IDX_glGetInteger64vAPPLE,
    IDX_glGetSyncivAPPLE,
    IDX_glEGLImageTargetTexStorageEXT,
    IDX_glEGLImageTargetTextureStorageEXT,
    IDX_glDrawArraysInstancedBaseInstanceEXT,
    IDX_glDrawElementsInstancedBaseInstanceEXT,
    IDX_glDrawElementsInstancedBaseVertexBaseInstanceEXT,
    IDX_glBindFragDataLocationIndexedEXT,
    IDX_glBindFragDataLocationEXT,
    IDX_glGetProgramResourceLocationIndexEXT,
    IDX_glGetFragDataIndexEXT,
    IDX_glBufferStorageEXT,
    IDX_glClearTexImageEXT,
    IDX_glClearTexSubImageEXT,
    IDX_glClipControlEXT,
    IDX_glCopyImageSubDataEXT,
    IDX_glLabelObjectEXT,
    IDX_glGetObjectLabelEXT,
    IDX_glInsertEventMarkerEXT,
    IDX_glPushGroupMarkerEXT,
    IDX_glPopGroupMarkerEXT,
    IDX_glDiscardFramebufferEXT,
    IDX_glGenQueriesEXT,
    IDX_glDeleteQueriesEXT,
    IDX_glIsQueryEXT,
    IDX_glBeginQueryEXT,
    IDX_glEndQueryEXT,
    IDX_glQueryCounterEXT,
    IDX_glGetQueryivEXT,
    IDX_glGetQueryObjectivEXT,
    IDX_glGetQueryObjectuivEXT,
    IDX_glGetQueryObjecti64vEXT,
    IDX_glGetQueryObjectui64vEXT,
    IDX_glDrawBuffersEXT,
    IDX_glEnableiEXT,
    IDX_glDisableiEXT,
    IDX_glBlendEquationiEXT,
    IDX_glBlendEquationSeparateiEXT,
    IDX_glBlendFunciEXT,
    IDX_glBlendFuncSeparateiEXT,
    IDX_glColorMaskiEXT,
    IDX_glIsEnablediEXT,
    IDX_glDrawElementsBaseVertexEXT,
    IDX_glDrawRangeElementsBaseVertexEXT,
    IDX_glDrawElementsInstancedBaseVertexEXT,
    IDX_glDrawArraysInstancedEXT,
    IDX_glDrawElementsInstancedEXT,
    IDX_glDrawTransformFeedbackEXT,
    IDX_glDrawTransformFeedbackInstancedEXT,
    IDX_glBufferStorageExternalEXT,
    IDX_glNamedBufferStorageExternalEXT,
    IDX_glFramebufferTextureEXT,
    IDX_glVertexAttribDivisorEXT,
    IDX_glMapBufferRangeEXT,
    IDX_glFlushMappedBufferRangeEXT,
    IDX_glGetUnsignedBytevEXT,
    IDX_glGetUnsignedBytei_vEXT,
    IDX_glDeleteMemoryObjectsEXT,
    IDX_glIsMemoryObjectEXT,
    IDX_glCreateMemoryObjectsEXT,
    IDX_glMemoryObjectParameterivEXT,
    IDX_glGetMemoryObjectParameterivEXT,
    IDX_glTexStorageMem2DEXT,
    IDX_glTexStorageMem2DMultisampleEXT,
    IDX_glTexStorageMem3DEXT,
    IDX_glTexStorageMem3DMultisampleEXT,
    IDX_glBufferStorageMemEXT,
    IDX_glTextureStorageMem2DEXT,
    IDX_glTextureStorageMem2DMultisampleEXT,
    IDX_glTextureStorageMem3DEXT,
    IDX_glTextureStorageMem3DMultisampleEXT,
    IDX_glNamedBufferStorageMemEXT,
    IDX_glImportMemoryFdEXT,
    IDX_glImportMemoryWin32HandleEXT,
    IDX_glImportMemoryWin32NameEXT,
    IDX_glMultiDrawArraysEXT,
    IDX_glMultiDrawElementsEXT,
    IDX_glMultiDrawArraysIndirectEXT,
    IDX_glMultiDrawElementsIndirectEXT,
    IDX_glRenderbufferStorageMultisampleEXT,
    IDX_glFramebufferTexture2DMultisampleEXT,
    IDX_glReadBufferIndexedEXT,
    IDX_glDrawBuffersIndexedEXT,
    IDX_glGetIntegeri_vEXT,
    IDX_glPolygonOffsetClampEXT,
    IDX_glPrimitiveBoundingBoxEXT,
    IDX_glRasterSamplesEXT,
    IDX_glGetGraphicsResetStatusEXT,
    IDX_glReadnPixelsEXT,
    IDX_glGetnUniformfvEXT,
    IDX_glGetnUniformivEXT,
    IDX_glGenSemaphoresEXT,
    IDX_glDeleteSemaphoresEXT,
    IDX_glIsSemaphoreEXT,
    IDX_glSemaphoreParameterui64vEXT,
    IDX_glGetSemaphoreParameterui64vEXT,
    IDX_glWaitSemaphoreEXT,
    IDX_glSignalSemaphoreEXT,
    IDX_glImportSemaphoreFdEXT,
    IDX_glImportSemaphoreWin32HandleEXT,
    IDX_glImportSemaphoreWin32NameEXT,
    IDX_glActiveShaderProgramEXT,
    IDX_glBindProgramPipelineEXT,
    IDX_glCreateShaderProgramvEXT,
    IDX_glDeleteProgramPipelinesEXT,
    IDX_glGenProgramPipelinesEXT,
    IDX_glGetProgramPipelineInfoLogEXT,
    IDX_glGetProgramPipelineivEXT,
    IDX_glIsProgramPipelineEXT,
    IDX_glProgramParameteriEXT,
    IDX_glProgramUniform1fEXT,
    IDX_glProgramUniform1fvEXT,
    IDX_glProgramUniform1iEXT,
    IDX_glProgramUniform1ivEXT,
    IDX_glProgramUniform2fEXT,
    IDX_glProgramUniform2fvEXT,
    IDX_glProgramUniform2iEXT,
    IDX_glProgramUniform2ivEXT,
    IDX_glProgramUniform3fEXT,
    IDX_glProgramUniform3fvEXT,
    IDX_glProgramUniform3iEXT,
    IDX_glProgramUniform3ivEXT,
    IDX_glProgramUniform4fEXT,
    IDX_glProgramUniform4fvEXT,
    IDX_glProgramUniform4iEXT,
    IDX_glProgramUniform4ivEXT,
    IDX_glProgramUniformMatrix2fvEXT,
    IDX_glProgramUniformMatrix3fvEXT,
    IDX_glProgramUniformMatrix4fvEXT,
    IDX_glUseProgramStagesEXT,
    IDX_glValidateProgramPipelineEXT,
    IDX_glProgramUniform1uiEXT,
    IDX_glProgramUniform2uiEXT,
    IDX_glProgramUniform3uiEXT,
    IDX_glProgramUniform4uiEXT,
    IDX_glProgramUniform1uivEXT,
    IDX_glProgramUniform2uivEXT,
    IDX_glProgramUniform3uivEXT,
    IDX_glProgramUniform4uivEXT,
    IDX_glProgramUniformMatrix2x3fvEXT,
    IDX_glProgramUniformMatrix3x2fvEXT,
    IDX_glProgramUniformMatrix2x4fvEXT,
    IDX_glProgramUniformMatrix4x2fvEXT,
    IDX_glProgramUniformMatrix3x4fvEXT,
    IDX_glProgramUniformMatrix4x3fvEXT,
    IDX_glFramebufferFetchBarrierEXT,
    IDX_glFramebufferPixelLocalStorageSizeEXT,
    IDX_glGetFramebufferPixelLocalStorageSizeEXT,
    IDX_glClearPixelLocalStorageuiEXT,
    IDX_glTexPageCommitmentEXT,
    IDX_glPatchParameteriEXT,
    IDX_glTexParameterIivEXT,
    IDX_glTexParameterIuivEXT,
    IDX_glGetTexParameterIivEXT,
    IDX_glGetTexParameterIuivEXT,
    IDX_glSamplerParameterIivEXT,
    IDX_glSamplerParameterIuivEXT,
    IDX_glGetSamplerParameterIivEXT,
    IDX_glGetSamplerParameterIuivEXT,
    IDX_glTexBufferEXT,
    IDX_glTexBufferRangeEXT,
    IDX_glTexStorage1DEXT,
    IDX_glTexStorage2DEXT,
    IDX_glTexStorage3DEXT,
    IDX_glTextureStorage1DEXT,
    IDX_glTextureStorage2DEXT,
    IDX_glTextureStorage3DEXT,
    IDX_glTextureViewEXT,
    IDX_glAcquireKeyedMutexWin32EXT,
    IDX_glReleaseKeyedMutexWin32EXT,
    IDX_glWindowRectanglesEXT,
    IDX_glGetTextureHandleIMG,
    IDX_glGetTextureSamplerHandleIMG,
    IDX_glUniformHandleui64IMG,
    IDX_glUniformHandleui64vIMG,
    IDX_glProgramUniformHandleui64IMG,
    IDX_glProgramUniformHandleui64vIMG,
    IDX_glFramebufferTexture2DDownsampleIMG,
    IDX_glFramebufferTextureLayerDownsampleIMG,
    IDX_glRenderbufferStorageMultisampleIMG,
    IDX_glFramebufferTexture2DMultisampleIMG,
    IDX_glApplyFramebufferAttachmentCMAAINTEL,
    IDX_glBeginPerfQueryINTEL,
    IDX_glCreatePerfQueryINTEL,
    IDX_glDeletePerfQueryINTEL,
    IDX_glEndPerfQueryINTEL,
    IDX_glGetFirstPerfQueryIdINTEL,
    IDX_glGetNextPerfQueryIdINTEL,
    IDX_glGetPerfCounterInfoINTEL,
    IDX_glGetPerfQueryDataINTEL,
    IDX_glGetPerfQueryIdByNameINTEL,
    IDX_glGetPerfQueryInfoINTEL,
    IDX_glGetTextureHandleNV,
    IDX_glGetTextureSamplerHandleNV,
    IDX_glMakeTextureHandleResidentNV,
    IDX_glMakeTextureHandleNonResidentNV,
    IDX_glGetImageHandleNV,
    IDX_glMakeImageHandleResidentNV,
    IDX_glMakeImageHandleNonResidentNV,
    IDX_glUniformHandleui64NV,
    IDX_glUniformHandleui64vNV,
    IDX_glProgramUniformHandleui64NV,
    IDX_glProgramUniformHandleui64vNV,
    IDX_glIsTextureHandleResidentNV,
    IDX_glIsImageHandleResidentNV,
    IDX_glBlendParameteriNV,
    IDX_glBlendBarrierNV,
    IDX_glViewportPositionWScaleNV,
    IDX_glBeginConditionalRenderNV,
    IDX_glEndConditionalRenderNV,
    IDX_glSubpixelPrecisionBiasNV,
    IDX_glConservativeRasterParameteriNV,
    IDX_glCopyBufferSubDataNV,
    IDX_glCoverageMaskNV,
    IDX_glCoverageOperationNV,
    IDX_glDrawBuffersNV,
    IDX_glDrawArraysInstancedNV,
    IDX_glDrawElementsInstancedNV,
    IDX_glDrawVkImageNV,
    IDX_glGetVkProcAddrNV,
    IDX_glWaitVkSemaphoreNV,
    IDX_glSignalVkSemaphoreNV,
    IDX_glSignalVkFenceNV,
    IDX_glDeleteFencesNV,
    IDX_glGenFencesNV,
    IDX_glIsFenceNV,
    IDX_glTestFenceNV,
    IDX_glGetFenceivNV,
    IDX_glFinishFenceNV,
    IDX_glSetFenceNV,
    IDX_glFragmentCoverageColorNV,
    IDX_glBlitFramebufferNV,
    IDX_glCoverageModulationTableNV,
    IDX_glGetCoverageModulationTableNV,
    IDX_glCoverageModulationNV,
    IDX_glRenderbufferStorageMultisampleNV,
    IDX_glUniform1i64NV,
    IDX_glUniform2i64NV,
    IDX_glUniform3i64NV,
    IDX_glUniform4i64NV,
    IDX_glUniform1i64vNV,
    IDX_glUniform2i64vNV,
    IDX_glUniform3i64vNV,
    IDX_glUniform4i64vNV,
    IDX_glUniform1ui64NV,
    IDX_glUniform2ui64NV,
    IDX_glUniform3ui64NV,
    IDX_glUniform4ui64NV,
    IDX_glUniform1ui64vNV,
    IDX_glUniform2ui64vNV,
    IDX_glUniform3ui64vNV,
    IDX_glUniform4ui64vNV,
    IDX_glGetUniformi64vNV,
    IDX_glProgramUniform1i64NV,
    IDX_glProgramUniform2i64NV,
    IDX_glProgramUniform3i64NV,
    IDX_glProgramUniform4i64NV,
    IDX_glProgramUniform1i64vNV,
    IDX_glProgramUniform2i64vNV,
    IDX_glProgramUniform3i64vNV,
    IDX_glProgramUniform4i64vNV,
    IDX_glProgramUniform1ui64NV,
    IDX_glProgramUniform2ui64NV,
    IDX_glProgramUniform3ui64NV,
    IDX_glProgramUniform4ui64NV,
    IDX_glProgramUniform1ui64vNV,
    IDX_glProgramUniform2ui64vNV,
    IDX_glProgramUniform3ui64vNV,
    IDX_glProgramUniform4ui64vNV,
    IDX_glVertexAttribDivisorNV,
    IDX_glGetInternalformatSampleivNV,
    IDX_glUniformMatrix2x3fvNV,
    IDX_glUniformMatrix3x2fvNV,
    IDX_glUniformMatrix2x4fvNV,
    IDX_glUniformMatrix4x2fvNV,
    IDX_glUniformMatrix3x4fvNV,
    IDX_glUniformMatrix4x3fvNV,
    IDX_glGenPathsNV,
    IDX_glDeletePathsNV,
    IDX_glIsPathNV,
    IDX_glPathCommandsNV,
    IDX_glPathCoordsNV,
    IDX_glPathSubCommandsNV,
    IDX_glPathSubCoordsNV,
    IDX_glPathStringNV,
    IDX_glPathGlyphsNV,
    IDX_glPathGlyphRangeNV,
    IDX_glWeightPathsNV,
    IDX_glCopyPathNV,
    IDX_glInterpolatePathsNV,
    IDX_glTransformPathNV,
    IDX_glPathParameterivNV,
    IDX_glPathParameteriNV,
    IDX_glPathParameterfvNV,
    IDX_glPathParameterfNV,
    IDX_glPathDashArrayNV,
    IDX_glPathStencilFuncNV,
    IDX_glPathStencilDepthOffsetNV,
    IDX_glStencilFillPathNV,
    IDX_glStencilStrokePathNV,
    IDX_glStencilFillPathInstancedNV,
    IDX_glStencilStrokePathInstancedNV,
    IDX_glPathCoverDepthFuncNV,
    IDX_glCoverFillPathNV,
    IDX_glCoverStrokePathNV,
    IDX_glCoverFillPathInstancedNV,
    IDX_glCoverStrokePathInstancedNV,
    IDX_glGetPathParameterivNV,
    IDX_glGetPathParameterfvNV,
    IDX_glGetPathCommandsNV,
    IDX_glGetPathCoordsNV,
    IDX_glGetPathDashArrayNV,
    IDX_glGetPathMetricsNV,
    IDX_glGetPathMetricRangeNV,
    IDX_glGetPathSpacingNV,
    IDX_glIsPointInFillPathNV,
    IDX_glIsPointInStrokePathNV,
    IDX_glGetPathLengthNV,
    IDX_glPointAlongPathNV,
    IDX_glMatrixLoad3x2fNV,
    IDX_glMatrixLoad3x3fNV,
    IDX_glMatrixLoadTranspose3x3fNV,
    IDX_glMatrixMult3x2fNV,
    IDX_glMatrixMult3x3fNV,
    IDX_glMatrixMultTranspose3x3fNV,
    IDX_glStencilThenCoverFillPathNV,
    IDX_glStencilThenCoverStrokePathNV,
    IDX_glStencilThenCoverFillPathInstancedNV,
    IDX_glStencilThenCoverStrokePathInstancedNV,
    IDX_glPathGlyphIndexArrayNV,
    IDX_glPathMemoryGlyphIndexArrayNV,
    IDX_glProgramPathFragmentInputGenNV,
    IDX_glGetProgramResourcefvNV,
    IDX_glMatrixFrustumEXT,
    IDX_glMatrixLoadIdentityEXT,
    IDX_glMatrixLoadTransposefEXT,
    IDX_glMatrixLoadTransposedEXT,
    IDX_glMatrixLoadfEXT,
    IDX_glMatrixLoaddEXT,
    IDX_glMatrixMultTransposefEXT,
    IDX_glMatrixMultTransposedEXT,
    IDX_glMatrixMultfEXT,
    IDX_glMatrixMultdEXT,
    IDX_glMatrixOrthoEXT,
    IDX_glMatrixPopEXT,
    IDX_glMatrixPushEXT,
    IDX_glMatrixRotatefEXT,
    IDX_glMatrixRotatedEXT,
    IDX_glMatrixScalefEXT,
    IDX_glMatrixScaledEXT,
    IDX_glMatrixTranslatefEXT,
    IDX_glMatrixTranslatedEXT,
    IDX_glPolygonModeNV,
    IDX_glReadBufferNV,
    IDX_glFramebufferSampleLocationsfvNV,
    IDX_glNamedFramebufferSampleLocationsfvNV,
    IDX_glResolveDepthValuesNV,
    IDX_glViewportArrayvNV,
    IDX_glViewportIndexedfNV,
    IDX_glViewportIndexedfvNV,
    IDX_glScissorArrayvNV,
    IDX_glScissorIndexedNV,
    IDX_glScissorIndexedvNV,
    IDX_glDepthRangeArrayfvNV,
    IDX_glDepthRangeIndexedfNV,
    IDX_glGetFloati_vNV,
    IDX_glEnableiNV,
    IDX_glDisableiNV,
    IDX_glIsEnablediNV,
    IDX_glViewportSwizzleNV,
    IDX_glFramebufferTextureMultiviewOVR,
    IDX_glFramebufferTextureMultisampleMultiviewOVR,
    IDX_glAlphaFuncQCOM,
    IDX_glGetDriverControlsQCOM,
    IDX_glGetDriverControlStringQCOM,
    IDX_glEnableDriverControlQCOM,
    IDX_glDisableDriverControlQCOM,
    IDX_glExtGetTexturesQCOM,
    IDX_glExtGetBuffersQCOM,
    IDX_glExtGetRenderbuffersQCOM,
    IDX_glExtGetFramebuffersQCOM,
    IDX_glExtGetTexLevelParameterivQCOM,
    IDX_glExtTexObjectStateOverrideiQCOM,
    IDX_glExtGetTexSubImageQCOM,
    IDX_glExtGetBufferPointervQCOM,
    IDX_glExtGetShadersQCOM,
    IDX_glExtGetProgramsQCOM,
    IDX_glExtIsProgramBinaryQCOM,
    IDX_glExtGetProgramBinarySourceQCOM,
    IDX_glFramebufferFoveationConfigQCOM,
    IDX_glFramebufferFoveationParametersQCOM,
    IDX_glFramebufferFetchBarrierQCOM,
    IDX_glTextureFoveationParametersQCOM,
    IDX_glStartTilingQCOM,
    IDX_glEndTilingQCOM,
    IDX_glAlphaFunc,
    IDX_glClipPlanef,
    IDX_glColor4f,
    IDX_glFogf,
    IDX_glFogfv,
    IDX_glFrustumf,
    IDX_glGetClipPlanef,
    IDX_glGetLightfv,
    IDX_glGetMaterialfv,
    IDX_glGetTexEnvfv,
    IDX_glLightModelf,
    IDX_glLightModelfv,
    IDX_glLightf,
    IDX_glLightfv,
    IDX_glLoadMatrixf,
    IDX_glMaterialf,
    IDX_glMaterialfv,
    IDX_glMultMatrixf,
    IDX_glMultiTexCoord4f,
    IDX_glNormal3f,
    IDX_glOrthof,
    IDX_glPointParameterf,
    IDX_glPointParameterfv,
    IDX_glPointSize,
    IDX_glRotatef,
    IDX_glScalef,
    IDX_glTexEnvf,
    IDX_glTexEnvfv,
    IDX_glTranslatef,
    IDX_glAlphaFuncx,
    IDX_glClearColorx,
    IDX_glClearDepthx,
    IDX_glClientActiveTexture,
    IDX_glClipPlanex,
    IDX_glColor4ub,
    IDX_glColor4x,
    IDX_glColorPointer,
    IDX_glDepthRangex,
    IDX_glDisableClientState,
    IDX_glEnableClientState,
    IDX_glFogx,
    IDX_glFogxv,
    IDX_glFrustumx,
    IDX_glGetClipPlanex,
    IDX_glGetFixedv,
    IDX_glGetLightxv,
    IDX_glGetMaterialxv,
    IDX_glGetPointerv,
    IDX_glGetTexEnviv,
    IDX_glGetTexEnvxv,
    IDX_glGetTexParameterxv,
    IDX_glLightModelx,
    IDX_glLightModelxv,
    IDX_glLightx,
    IDX_glLightxv,
    IDX_glLineWidthx,
    IDX_glLoadIdentity,
    IDX_glLoadMatrixx,
    IDX_glLogicOp,
    IDX_glMaterialx,
    IDX_glMaterialxv,
    IDX_glMatrixMode,
    IDX_glMultMatrixx,
    IDX_glMultiTexCoord4x,
    IDX_glNormal3x,
    IDX_glNormalPointer,
    IDX_glOrthox,
    IDX_glPointParameterx,
    IDX_glPointParameterxv,
    IDX_glPointSizex,
    IDX_glPolygonOffsetx,
    IDX_glPopMatrix,
    IDX_glPushMatrix,
    IDX_glRotatex,
    IDX_glSampleCoveragex,
    IDX_glScalex,
    IDX_glShadeModel,
    IDX_glTexCoordPointer,
    IDX_glTexEnvi,
    IDX_glTexEnvx,
    IDX_glTexEnviv,
    IDX_glTexEnvxv,
    IDX_glTexParameterx,
    IDX_glTexParameterxv,
    IDX_glTranslatex,
    IDX_glVertexPointer,
    IDX_glPointSizePointerOES,
    IDX_glBlendEquationSeparateOES,
    IDX_glBlendFuncSeparateOES,
    IDX_glBlendEquationOES,
    IDX_glDrawTexsOES,
    IDX_glDrawTexiOES,
    IDX_glDrawTexxOES,
    IDX_glDrawTexsvOES,
    IDX_glDrawTexivOES,
    IDX_glDrawTexxvOES,
    IDX_glDrawTexfOES,
    IDX_glDrawTexfvOES,
    IDX_glAlphaFuncxOES,
    IDX_glClearColorxOES,
    IDX_glClearDepthxOES,
    IDX_glClipPlanexOES,
    IDX_glColor4xOES,
    IDX_glDepthRangexOES,
    IDX_glFogxOES,
    IDX_glFogxvOES,
    IDX_glFrustumxOES,
    IDX_glGetClipPlanexOES,
    IDX_glGetFixedvOES,
    IDX_glGetTexEnvxvOES,
    IDX_glGetTexParameterxvOES,
    IDX_glLightModelxOES,
    IDX_glLightModelxvOES,
    IDX_glLightxOES,
    IDX_glLightxvOES,
    IDX_glLineWidthxOES,
    IDX_glLoadMatrixxOES,
    IDX_glMaterialxOES,
    IDX_glMaterialxvOES,
    IDX_glMultMatrixxOES,
    IDX_glMultiTexCoord4xOES,
    IDX_glNormal3xOES,
    IDX_glOrthoxOES,
    IDX_glPointParameterxvOES,
    IDX_glPointSizexOES,
    IDX_glPolygonOffsetxOES,
    IDX_glRotatexOES,
    IDX_glScalexOES,
    IDX_glTexEnvxOES,
    IDX_glTexEnvxvOES,
    IDX_glTexParameterxOES,
    IDX_glTexParameterxvOES,
    IDX_glTranslatexOES,
    IDX_glGetLightxvOES,
    IDX_glGetMaterialxvOES,
    IDX_glPointParameterxOES,
    IDX_glSampleCoveragexOES,
    IDX_glGetTexGenxvOES,
    IDX_glTexGenxOES,
    IDX_glTexGenxvOES,
    IDX_glIsRenderbufferOES,
    IDX_glBindRenderbufferOES,
    IDX_glDeleteRenderbuffersOES,
    IDX_glGenRenderbuffersOES,
    IDX_glRenderbufferStorageOES,
    IDX_glGetRenderbufferParameterivOES,
    IDX_glIsFramebufferOES,
    IDX_glBindFramebufferOES,
    IDX_glDeleteFramebuffersOES,
    IDX_glGenFramebuffersOES,
    IDX_glCheckFramebufferStatusOES,
    IDX_glFramebufferRenderbufferOES,
    IDX_glFramebufferTexture2DOES,
    IDX_glGetFramebufferAttachmentParameterivOES,
    IDX_glGenerateMipmapOES,
    IDX_glCurrentPaletteMatrixOES,
    IDX_glLoadPaletteFromModelViewMatrixOES,
    IDX_glMatrixIndexPointerOES,
    IDX_glWeightPointerOES,
    IDX_glQueryMatrixxOES,
    IDX_glClearDepthfOES,
    IDX_glClipPlanefOES,
    IDX_glDepthRangefOES,
    IDX_glFrustumfOES,
    IDX_glGetClipPlanefOES,
    IDX_glOrthofOES,
    IDX_glTexGenfOES,
    IDX_glTexGenfvOES,
    IDX_glTexGeniOES,
    IDX_glTexGenivOES,
    IDX_glGetTexGenfvOES,
    IDX_glGetTexGenivOES,
    IDX_glClipPlanefIMG,
    IDX_glClipPlanexIMG,
    N_FUNCS
};
#define N_CORE 314
static const char *const kNames[N_FUNCS] = {
    "glActiveTexture",
    "glAttachShader",
    "glBindAttribLocation",
    "glBindBuffer",
    "glBindFramebuffer",
    "glBindRenderbuffer",
    "glBindTexture",
    "glBlendColor",
    "glBlendEquation",
    "glBlendEquationSeparate",
    "glBlendFunc",
    "glBlendFuncSeparate",
    "glBufferData",
    "glBufferSubData",
    "glCheckFramebufferStatus",
    "glClear",
    "glClearColor",
    "glClearDepthf",
    "glClearStencil",
    "glColorMask",
    "glCompileShader",
    "glCompressedTexImage2D",
    "glCompressedTexSubImage2D",
    "glCopyTexImage2D",
    "glCopyTexSubImage2D",
    "glCreateProgram",
    "glCreateShader",
    "glCullFace",
    "glDeleteBuffers",
    "glDeleteFramebuffers",
    "glDeleteProgram",
    "glDeleteRenderbuffers",
    "glDeleteShader",
    "glDeleteTextures",
    "glDepthFunc",
    "glDepthMask",
    "glDepthRangef",
    "glDetachShader",
    "glDisable",
    "glDisableVertexAttribArray",
    "glDrawArrays",
    "glDrawElements",
    "glEnable",
    "glEnableVertexAttribArray",
    "glFinish",
    "glFlush",
    "glFramebufferRenderbuffer",
    "glFramebufferTexture2D",
    "glFrontFace",
    "glGenBuffers",
    "glGenerateMipmap",
    "glGenFramebuffers",
    "glGenRenderbuffers",
    "glGenTextures",
    "glGetActiveAttrib",
    "glGetActiveUniform",
    "glGetAttachedShaders",
    "glGetAttribLocation",
    "glGetBooleanv",
    "glGetBufferParameteriv",
    "glGetError",
    "glGetFloatv",
    "glGetFramebufferAttachmentParameteriv",
    "glGetIntegerv",
    "glGetProgramiv",
    "glGetProgramInfoLog",
    "glGetRenderbufferParameteriv",
    "glGetShaderiv",
    "glGetShaderInfoLog",
    "glGetShaderPrecisionFormat",
    "glGetShaderSource",
    "glGetString",
    "glGetTexParameterfv",
    "glGetTexParameteriv",
    "glGetUniformfv",
    "glGetUniformiv",
    "glGetUniformLocation",
    "glGetVertexAttribfv",
    "glGetVertexAttribiv",
    "glGetVertexAttribPointerv",
    "glHint",
    "glIsBuffer",
    "glIsEnabled",
    "glIsFramebuffer",
    "glIsProgram",
    "glIsRenderbuffer",
    "glIsShader",
    "glIsTexture",
    "glLineWidth",
    "glLinkProgram",
    "glPixelStorei",
    "glPolygonOffset",
    "glReadPixels",
    "glReleaseShaderCompiler",
    "glRenderbufferStorage",
    "glSampleCoverage",
    "glScissor",
    "glShaderBinary",
    "glShaderSource",
    "glStencilFunc",
    "glStencilFuncSeparate",
    "glStencilMask",
    "glStencilMaskSeparate",
    "glStencilOp",
    "glStencilOpSeparate",
    "glTexImage2D",
    "glTexParameterf",
    "glTexParameterfv",
    "glTexParameteri",
    "glTexParameteriv",
    "glTexSubImage2D",
    "glUniform1f",
    "glUniform1fv",
    "glUniform1i",
    "glUniform1iv",
    "glUniform2f",
    "glUniform2fv",
    "glUniform2i",
    "glUniform2iv",
    "glUniform3f",
    "glUniform3fv",
    "glUniform3i",
    "glUniform3iv",
    "glUniform4f",
    "glUniform4fv",
    "glUniform4i",
    "glUniform4iv",
    "glUniformMatrix2fv",
    "glUniformMatrix3fv",
    "glUniformMatrix4fv",
    "glUseProgram",
    "glValidateProgram",
    "glVertexAttrib1f",
    "glVertexAttrib1fv",
    "glVertexAttrib2f",
    "glVertexAttrib2fv",
    "glVertexAttrib3f",
    "glVertexAttrib3fv",
    "glVertexAttrib4f",
    "glVertexAttrib4fv",
    "glVertexAttribPointer",
    "glViewport",
    "glReadBuffer",
    "glDrawRangeElements",
    "glTexImage3D",
    "glTexSubImage3D",
    "glCopyTexSubImage3D",
    "glCompressedTexImage3D",
    "glCompressedTexSubImage3D",
    "glGenQueries",
    "glDeleteQueries",
    "glIsQuery",
    "glBeginQuery",
    "glEndQuery",
    "glGetQueryiv",
    "glGetQueryObjectuiv",
    "glUnmapBuffer",
    "glGetBufferPointerv",
    "glDrawBuffers",
    "glUniformMatrix2x3fv",
    "glUniformMatrix3x2fv",
    "glUniformMatrix2x4fv",
    "glUniformMatrix4x2fv",
    "glUniformMatrix3x4fv",
    "glUniformMatrix4x3fv",
    "glBlitFramebuffer",
    "glRenderbufferStorageMultisample",
    "glFramebufferTextureLayer",
    "glMapBufferRange",
    "glFlushMappedBufferRange",
    "glBindVertexArray",
    "glDeleteVertexArrays",
    "glGenVertexArrays",
    "glIsVertexArray",
    "glGetIntegeri_v",
    "glBeginTransformFeedback",
    "glEndTransformFeedback",
    "glBindBufferRange",
    "glBindBufferBase",
    "glTransformFeedbackVaryings",
    "glGetTransformFeedbackVarying",
    "glVertexAttribIPointer",
    "glGetVertexAttribIiv",
    "glGetVertexAttribIuiv",
    "glVertexAttribI4i",
    "glVertexAttribI4ui",
    "glVertexAttribI4iv",
    "glVertexAttribI4uiv",
    "glGetUniformuiv",
    "glGetFragDataLocation",
    "glUniform1ui",
    "glUniform2ui",
    "glUniform3ui",
    "glUniform4ui",
    "glUniform1uiv",
    "glUniform2uiv",
    "glUniform3uiv",
    "glUniform4uiv",
    "glClearBufferiv",
    "glClearBufferuiv",
    "glClearBufferfv",
    "glClearBufferfi",
    "glGetStringi",
    "glCopyBufferSubData",
    "glGetUniformIndices",
    "glGetActiveUniformsiv",
    "glGetUniformBlockIndex",
    "glGetActiveUniformBlockiv",
    "glGetActiveUniformBlockName",
    "glUniformBlockBinding",
    "glDrawArraysInstanced",
    "glDrawElementsInstanced",
    "glFenceSync",
    "glIsSync",
    "glDeleteSync",
    "glClientWaitSync",
    "glWaitSync",
    "glGetInteger64v",
    "glGetSynciv",
    "glGetInteger64i_v",
    "glGetBufferParameteri64v",
    "glGenSamplers",
    "glDeleteSamplers",
    "glIsSampler",
    "glBindSampler",
    "glSamplerParameteri",
    "glSamplerParameteriv",
    "glSamplerParameterf",
    "glSamplerParameterfv",
    "glGetSamplerParameteriv",
    "glGetSamplerParameterfv",
    "glVertexAttribDivisor",
    "glBindTransformFeedback",
    "glDeleteTransformFeedbacks",
    "glGenTransformFeedbacks",
    "glIsTransformFeedback",
    "glPauseTransformFeedback",
    "glResumeTransformFeedback",
    "glGetProgramBinary",
    "glProgramBinary",
    "glProgramParameteri",
    "glInvalidateFramebuffer",
    "glInvalidateSubFramebuffer",
    "glTexStorage2D",
    "glTexStorage3D",
    "glGetInternalformativ",
    "glDispatchCompute",
    "glDispatchComputeIndirect",
    "glDrawArraysIndirect",
    "glDrawElementsIndirect",
    "glFramebufferParameteri",
    "glGetFramebufferParameteriv",
    "glGetProgramInterfaceiv",
    "glGetProgramResourceIndex",
    "glGetProgramResourceName",
    "glGetProgramResourceiv",
    "glGetProgramResourceLocation",
    "glUseProgramStages",
    "glActiveShaderProgram",
    "glCreateShaderProgramv",
    "glBindProgramPipeline",
    "glDeleteProgramPipelines",
    "glGenProgramPipelines",
    "glIsProgramPipeline",
    "glGetProgramPipelineiv",
    "glProgramUniform1i",
    "glProgramUniform2i",
    "glProgramUniform3i",
    "glProgramUniform4i",
    "glProgramUniform1ui",
    "glProgramUniform2ui",
    "glProgramUniform3ui",
    "glProgramUniform4ui",
    "glProgramUniform1f",
    "glProgramUniform2f",
    "glProgramUniform3f",
    "glProgramUniform4f",
    "glProgramUniform1iv",
    "glProgramUniform2iv",
    "glProgramUniform3iv",
    "glProgramUniform4iv",
    "glProgramUniform1uiv",
    "glProgramUniform2uiv",
    "glProgramUniform3uiv",
    "glProgramUniform4uiv",
    "glProgramUniform1fv",
    "glProgramUniform2fv",
    "glProgramUniform3fv",
    "glProgramUniform4fv",
    "glProgramUniformMatrix2fv",
    "glProgramUniformMatrix3fv",
    "glProgramUniformMatrix4fv",
    "glProgramUniformMatrix2x3fv",
    "glProgramUniformMatrix3x2fv",
    "glProgramUniformMatrix2x4fv",
    "glProgramUniformMatrix4x2fv",
    "glProgramUniformMatrix3x4fv",
    "glProgramUniformMatrix4x3fv",
    "glValidateProgramPipeline",
    "glGetProgramPipelineInfoLog",
    "glBindImageTexture",
    "glGetBooleani_v",
    "glMemoryBarrier",
    "glMemoryBarrierByRegion",
    "glTexStorage2DMultisample",
    "glGetMultisamplefv",
    "glSampleMaski",
    "glGetTexLevelParameteriv",
    "glGetTexLevelParameterfv",
    "glBindVertexBuffer",
    "glVertexAttribFormat",
    "glVertexAttribIFormat",
    "glVertexAttribBinding",
    "glVertexBindingDivisor",
    "glBlendBarrierKHR",
    "glDebugMessageControlKHR",
    "glDebugMessageInsertKHR",
    "glDebugMessageCallbackKHR",
    "glGetDebugMessageLogKHR",
    "glPushDebugGroupKHR",
    "glPopDebugGroupKHR",
    "glObjectLabelKHR",
    "glGetObjectLabelKHR",
    "glObjectPtrLabelKHR",
    "glGetObjectPtrLabelKHR",
    "glGetPointervKHR",
    "glMaxShaderCompilerThreadsKHR",
    "glGetGraphicsResetStatusKHR",
    "glReadnPixelsKHR",
    "glGetnUniformfvKHR",
    "glGetnUniformivKHR",
    "glGetnUniformuivKHR",
    "glEGLImageTargetTexture2DOES",
    "glEGLImageTargetRenderbufferStorageOES",
    "glCopyImageSubDataOES",
    "glEnableiOES",
    "glDisableiOES",
    "glBlendEquationiOES",
    "glBlendEquationSeparateiOES",
    "glBlendFunciOES",
    "glBlendFuncSeparateiOES",
    "glColorMaskiOES",
    "glIsEnablediOES",
    "glDrawElementsBaseVertexOES",
    "glDrawRangeElementsBaseVertexOES",
    "glDrawElementsInstancedBaseVertexOES",
    "glMultiDrawElementsBaseVertexEXT",
    "glFramebufferTextureOES",
    "glGetProgramBinaryOES",
    "glProgramBinaryOES",
    "glMapBufferOES",
    "glUnmapBufferOES",
    "glGetBufferPointervOES",
    "glPrimitiveBoundingBoxOES",
    "glMinSampleShadingOES",
    "glPatchParameteriOES",
    "glTexImage3DOES",
    "glTexSubImage3DOES",
    "glCopyTexSubImage3DOES",
    "glCompressedTexImage3DOES",
    "glCompressedTexSubImage3DOES",
    "glFramebufferTexture3DOES",
    "glTexParameterIivOES",
    "glTexParameterIuivOES",
    "glGetTexParameterIivOES",
    "glGetTexParameterIuivOES",
    "glSamplerParameterIivOES",
    "glSamplerParameterIuivOES",
    "glGetSamplerParameterIivOES",
    "glGetSamplerParameterIuivOES",
    "glTexBufferOES",
    "glTexBufferRangeOES",
    "glTexStorage3DMultisampleOES",
    "glTextureViewOES",
    "glBindVertexArrayOES",
    "glDeleteVertexArraysOES",
    "glGenVertexArraysOES",
    "glIsVertexArrayOES",
    "glViewportArrayvOES",
    "glViewportIndexedfOES",
    "glViewportIndexedfvOES",
    "glScissorArrayvOES",
    "glScissorIndexedOES",
    "glScissorIndexedvOES",
    "glDepthRangeArrayfvOES",
    "glDepthRangeIndexedfOES",
    "glGetFloati_vOES",
    "glGetPerfMonitorGroupsAMD",
    "glGetPerfMonitorCountersAMD",
    "glGetPerfMonitorGroupStringAMD",
    "glGetPerfMonitorCounterStringAMD",
    "glGetPerfMonitorCounterInfoAMD",
    "glGenPerfMonitorsAMD",
    "glDeletePerfMonitorsAMD",
    "glSelectPerfMonitorCountersAMD",
    "glBeginPerfMonitorAMD",
    "glEndPerfMonitorAMD",
    "glGetPerfMonitorCounterDataAMD",
    "glBlitFramebufferANGLE",
    "glRenderbufferStorageMultisampleANGLE",
    "glDrawArraysInstancedANGLE",
    "glDrawElementsInstancedANGLE",
    "glVertexAttribDivisorANGLE",
    "glGetTranslatedShaderSourceANGLE",
    "glCopyTextureLevelsAPPLE",
    "glRenderbufferStorageMultisampleAPPLE",
    "glResolveMultisampleFramebufferAPPLE",
    "glFenceSyncAPPLE",
    "glIsSyncAPPLE",
    "glDeleteSyncAPPLE",
    "glClientWaitSyncAPPLE",
    "glWaitSyncAPPLE",
    "glGetInteger64vAPPLE",
    "glGetSyncivAPPLE",
    "glEGLImageTargetTexStorageEXT",
    "glEGLImageTargetTextureStorageEXT",
    "glDrawArraysInstancedBaseInstanceEXT",
    "glDrawElementsInstancedBaseInstanceEXT",
    "glDrawElementsInstancedBaseVertexBaseInstanceEXT",
    "glBindFragDataLocationIndexedEXT",
    "glBindFragDataLocationEXT",
    "glGetProgramResourceLocationIndexEXT",
    "glGetFragDataIndexEXT",
    "glBufferStorageEXT",
    "glClearTexImageEXT",
    "glClearTexSubImageEXT",
    "glClipControlEXT",
    "glCopyImageSubDataEXT",
    "glLabelObjectEXT",
    "glGetObjectLabelEXT",
    "glInsertEventMarkerEXT",
    "glPushGroupMarkerEXT",
    "glPopGroupMarkerEXT",
    "glDiscardFramebufferEXT",
    "glGenQueriesEXT",
    "glDeleteQueriesEXT",
    "glIsQueryEXT",
    "glBeginQueryEXT",
    "glEndQueryEXT",
    "glQueryCounterEXT",
    "glGetQueryivEXT",
    "glGetQueryObjectivEXT",
    "glGetQueryObjectuivEXT",
    "glGetQueryObjecti64vEXT",
    "glGetQueryObjectui64vEXT",
    "glDrawBuffersEXT",
    "glEnableiEXT",
    "glDisableiEXT",
    "glBlendEquationiEXT",
    "glBlendEquationSeparateiEXT",
    "glBlendFunciEXT",
    "glBlendFuncSeparateiEXT",
    "glColorMaskiEXT",
    "glIsEnablediEXT",
    "glDrawElementsBaseVertexEXT",
    "glDrawRangeElementsBaseVertexEXT",
    "glDrawElementsInstancedBaseVertexEXT",
    "glDrawArraysInstancedEXT",
    "glDrawElementsInstancedEXT",
    "glDrawTransformFeedbackEXT",
    "glDrawTransformFeedbackInstancedEXT",
    "glBufferStorageExternalEXT",
    "glNamedBufferStorageExternalEXT",
    "glFramebufferTextureEXT",
    "glVertexAttribDivisorEXT",
    "glMapBufferRangeEXT",
    "glFlushMappedBufferRangeEXT",
    "glGetUnsignedBytevEXT",
    "glGetUnsignedBytei_vEXT",
    "glDeleteMemoryObjectsEXT",
    "glIsMemoryObjectEXT",
    "glCreateMemoryObjectsEXT",
    "glMemoryObjectParameterivEXT",
    "glGetMemoryObjectParameterivEXT",
    "glTexStorageMem2DEXT",
    "glTexStorageMem2DMultisampleEXT",
    "glTexStorageMem3DEXT",
    "glTexStorageMem3DMultisampleEXT",
    "glBufferStorageMemEXT",
    "glTextureStorageMem2DEXT",
    "glTextureStorageMem2DMultisampleEXT",
    "glTextureStorageMem3DEXT",
    "glTextureStorageMem3DMultisampleEXT",
    "glNamedBufferStorageMemEXT",
    "glImportMemoryFdEXT",
    "glImportMemoryWin32HandleEXT",
    "glImportMemoryWin32NameEXT",
    "glMultiDrawArraysEXT",
    "glMultiDrawElementsEXT",
    "glMultiDrawArraysIndirectEXT",
    "glMultiDrawElementsIndirectEXT",
    "glRenderbufferStorageMultisampleEXT",
    "glFramebufferTexture2DMultisampleEXT",
    "glReadBufferIndexedEXT",
    "glDrawBuffersIndexedEXT",
    "glGetIntegeri_vEXT",
    "glPolygonOffsetClampEXT",
    "glPrimitiveBoundingBoxEXT",
    "glRasterSamplesEXT",
    "glGetGraphicsResetStatusEXT",
    "glReadnPixelsEXT",
    "glGetnUniformfvEXT",
    "glGetnUniformivEXT",
    "glGenSemaphoresEXT",
    "glDeleteSemaphoresEXT",
    "glIsSemaphoreEXT",
    "glSemaphoreParameterui64vEXT",
    "glGetSemaphoreParameterui64vEXT",
    "glWaitSemaphoreEXT",
    "glSignalSemaphoreEXT",
    "glImportSemaphoreFdEXT",
    "glImportSemaphoreWin32HandleEXT",
    "glImportSemaphoreWin32NameEXT",
    "glActiveShaderProgramEXT",
    "glBindProgramPipelineEXT",
    "glCreateShaderProgramvEXT",
    "glDeleteProgramPipelinesEXT",
    "glGenProgramPipelinesEXT",
    "glGetProgramPipelineInfoLogEXT",
    "glGetProgramPipelineivEXT",
    "glIsProgramPipelineEXT",
    "glProgramParameteriEXT",
    "glProgramUniform1fEXT",
    "glProgramUniform1fvEXT",
    "glProgramUniform1iEXT",
    "glProgramUniform1ivEXT",
    "glProgramUniform2fEXT",
    "glProgramUniform2fvEXT",
    "glProgramUniform2iEXT",
    "glProgramUniform2ivEXT",
    "glProgramUniform3fEXT",
    "glProgramUniform3fvEXT",
    "glProgramUniform3iEXT",
    "glProgramUniform3ivEXT",
    "glProgramUniform4fEXT",
    "glProgramUniform4fvEXT",
    "glProgramUniform4iEXT",
    "glProgramUniform4ivEXT",
    "glProgramUniformMatrix2fvEXT",
    "glProgramUniformMatrix3fvEXT",
    "glProgramUniformMatrix4fvEXT",
    "glUseProgramStagesEXT",
    "glValidateProgramPipelineEXT",
    "glProgramUniform1uiEXT",
    "glProgramUniform2uiEXT",
    "glProgramUniform3uiEXT",
    "glProgramUniform4uiEXT",
    "glProgramUniform1uivEXT",
    "glProgramUniform2uivEXT",
    "glProgramUniform3uivEXT",
    "glProgramUniform4uivEXT",
    "glProgramUniformMatrix2x3fvEXT",
    "glProgramUniformMatrix3x2fvEXT",
    "glProgramUniformMatrix2x4fvEXT",
    "glProgramUniformMatrix4x2fvEXT",
    "glProgramUniformMatrix3x4fvEXT",
    "glProgramUniformMatrix4x3fvEXT",
    "glFramebufferFetchBarrierEXT",
    "glFramebufferPixelLocalStorageSizeEXT",
    "glGetFramebufferPixelLocalStorageSizeEXT",
    "glClearPixelLocalStorageuiEXT",
    "glTexPageCommitmentEXT",
    "glPatchParameteriEXT",
    "glTexParameterIivEXT",
    "glTexParameterIuivEXT",
    "glGetTexParameterIivEXT",
    "glGetTexParameterIuivEXT",
    "glSamplerParameterIivEXT",
    "glSamplerParameterIuivEXT",
    "glGetSamplerParameterIivEXT",
    "glGetSamplerParameterIuivEXT",
    "glTexBufferEXT",
    "glTexBufferRangeEXT",
    "glTexStorage1DEXT",
    "glTexStorage2DEXT",
    "glTexStorage3DEXT",
    "glTextureStorage1DEXT",
    "glTextureStorage2DEXT",
    "glTextureStorage3DEXT",
    "glTextureViewEXT",
    "glAcquireKeyedMutexWin32EXT",
    "glReleaseKeyedMutexWin32EXT",
    "glWindowRectanglesEXT",
    "glGetTextureHandleIMG",
    "glGetTextureSamplerHandleIMG",
    "glUniformHandleui64IMG",
    "glUniformHandleui64vIMG",
    "glProgramUniformHandleui64IMG",
    "glProgramUniformHandleui64vIMG",
    "glFramebufferTexture2DDownsampleIMG",
    "glFramebufferTextureLayerDownsampleIMG",
    "glRenderbufferStorageMultisampleIMG",
    "glFramebufferTexture2DMultisampleIMG",
    "glApplyFramebufferAttachmentCMAAINTEL",
    "glBeginPerfQueryINTEL",
    "glCreatePerfQueryINTEL",
    "glDeletePerfQueryINTEL",
    "glEndPerfQueryINTEL",
    "glGetFirstPerfQueryIdINTEL",
    "glGetNextPerfQueryIdINTEL",
    "glGetPerfCounterInfoINTEL",
    "glGetPerfQueryDataINTEL",
    "glGetPerfQueryIdByNameINTEL",
    "glGetPerfQueryInfoINTEL",
    "glGetTextureHandleNV",
    "glGetTextureSamplerHandleNV",
    "glMakeTextureHandleResidentNV",
    "glMakeTextureHandleNonResidentNV",
    "glGetImageHandleNV",
    "glMakeImageHandleResidentNV",
    "glMakeImageHandleNonResidentNV",
    "glUniformHandleui64NV",
    "glUniformHandleui64vNV",
    "glProgramUniformHandleui64NV",
    "glProgramUniformHandleui64vNV",
    "glIsTextureHandleResidentNV",
    "glIsImageHandleResidentNV",
    "glBlendParameteriNV",
    "glBlendBarrierNV",
    "glViewportPositionWScaleNV",
    "glBeginConditionalRenderNV",
    "glEndConditionalRenderNV",
    "glSubpixelPrecisionBiasNV",
    "glConservativeRasterParameteriNV",
    "glCopyBufferSubDataNV",
    "glCoverageMaskNV",
    "glCoverageOperationNV",
    "glDrawBuffersNV",
    "glDrawArraysInstancedNV",
    "glDrawElementsInstancedNV",
    "glDrawVkImageNV",
    "glGetVkProcAddrNV",
    "glWaitVkSemaphoreNV",
    "glSignalVkSemaphoreNV",
    "glSignalVkFenceNV",
    "glDeleteFencesNV",
    "glGenFencesNV",
    "glIsFenceNV",
    "glTestFenceNV",
    "glGetFenceivNV",
    "glFinishFenceNV",
    "glSetFenceNV",
    "glFragmentCoverageColorNV",
    "glBlitFramebufferNV",
    "glCoverageModulationTableNV",
    "glGetCoverageModulationTableNV",
    "glCoverageModulationNV",
    "glRenderbufferStorageMultisampleNV",
    "glUniform1i64NV",
    "glUniform2i64NV",
    "glUniform3i64NV",
    "glUniform4i64NV",
    "glUniform1i64vNV",
    "glUniform2i64vNV",
    "glUniform3i64vNV",
    "glUniform4i64vNV",
    "glUniform1ui64NV",
    "glUniform2ui64NV",
    "glUniform3ui64NV",
    "glUniform4ui64NV",
    "glUniform1ui64vNV",
    "glUniform2ui64vNV",
    "glUniform3ui64vNV",
    "glUniform4ui64vNV",
    "glGetUniformi64vNV",
    "glProgramUniform1i64NV",
    "glProgramUniform2i64NV",
    "glProgramUniform3i64NV",
    "glProgramUniform4i64NV",
    "glProgramUniform1i64vNV",
    "glProgramUniform2i64vNV",
    "glProgramUniform3i64vNV",
    "glProgramUniform4i64vNV",
    "glProgramUniform1ui64NV",
    "glProgramUniform2ui64NV",
    "glProgramUniform3ui64NV",
    "glProgramUniform4ui64NV",
    "glProgramUniform1ui64vNV",
    "glProgramUniform2ui64vNV",
    "glProgramUniform3ui64vNV",
    "glProgramUniform4ui64vNV",
    "glVertexAttribDivisorNV",
    "glGetInternalformatSampleivNV",
    "glUniformMatrix2x3fvNV",
    "glUniformMatrix3x2fvNV",
    "glUniformMatrix2x4fvNV",
    "glUniformMatrix4x2fvNV",
    "glUniformMatrix3x4fvNV",
    "glUniformMatrix4x3fvNV",
    "glGenPathsNV",
    "glDeletePathsNV",
    "glIsPathNV",
    "glPathCommandsNV",
    "glPathCoordsNV",
    "glPathSubCommandsNV",
    "glPathSubCoordsNV",
    "glPathStringNV",
    "glPathGlyphsNV",
    "glPathGlyphRangeNV",
    "glWeightPathsNV",
    "glCopyPathNV",
    "glInterpolatePathsNV",
    "glTransformPathNV",
    "glPathParameterivNV",
    "glPathParameteriNV",
    "glPathParameterfvNV",
    "glPathParameterfNV",
    "glPathDashArrayNV",
    "glPathStencilFuncNV",
    "glPathStencilDepthOffsetNV",
    "glStencilFillPathNV",
    "glStencilStrokePathNV",
    "glStencilFillPathInstancedNV",
    "glStencilStrokePathInstancedNV",
    "glPathCoverDepthFuncNV",
    "glCoverFillPathNV",
    "glCoverStrokePathNV",
    "glCoverFillPathInstancedNV",
    "glCoverStrokePathInstancedNV",
    "glGetPathParameterivNV",
    "glGetPathParameterfvNV",
    "glGetPathCommandsNV",
    "glGetPathCoordsNV",
    "glGetPathDashArrayNV",
    "glGetPathMetricsNV",
    "glGetPathMetricRangeNV",
    "glGetPathSpacingNV",
    "glIsPointInFillPathNV",
    "glIsPointInStrokePathNV",
    "glGetPathLengthNV",
    "glPointAlongPathNV",
    "glMatrixLoad3x2fNV",
    "glMatrixLoad3x3fNV",
    "glMatrixLoadTranspose3x3fNV",
    "glMatrixMult3x2fNV",
    "glMatrixMult3x3fNV",
    "glMatrixMultTranspose3x3fNV",
    "glStencilThenCoverFillPathNV",
    "glStencilThenCoverStrokePathNV",
    "glStencilThenCoverFillPathInstancedNV",
    "glStencilThenCoverStrokePathInstancedNV",
    "glPathGlyphIndexArrayNV",
    "glPathMemoryGlyphIndexArrayNV",
    "glProgramPathFragmentInputGenNV",
    "glGetProgramResourcefvNV",
    "glMatrixFrustumEXT",
    "glMatrixLoadIdentityEXT",
    "glMatrixLoadTransposefEXT",
    "glMatrixLoadTransposedEXT",
    "glMatrixLoadfEXT",
    "glMatrixLoaddEXT",
    "glMatrixMultTransposefEXT",
    "glMatrixMultTransposedEXT",
    "glMatrixMultfEXT",
    "glMatrixMultdEXT",
    "glMatrixOrthoEXT",
    "glMatrixPopEXT",
    "glMatrixPushEXT",
    "glMatrixRotatefEXT",
    "glMatrixRotatedEXT",
    "glMatrixScalefEXT",
    "glMatrixScaledEXT",
    "glMatrixTranslatefEXT",
    "glMatrixTranslatedEXT",
    "glPolygonModeNV",
    "glReadBufferNV",
    "glFramebufferSampleLocationsfvNV",
    "glNamedFramebufferSampleLocationsfvNV",
    "glResolveDepthValuesNV",
    "glViewportArrayvNV",
    "glViewportIndexedfNV",
    "glViewportIndexedfvNV",
    "glScissorArrayvNV",
    "glScissorIndexedNV",
    "glScissorIndexedvNV",
    "glDepthRangeArrayfvNV",
    "glDepthRangeIndexedfNV",
    "glGetFloati_vNV",
    "glEnableiNV",
    "glDisableiNV",
    "glIsEnablediNV",
    "glViewportSwizzleNV",
    "glFramebufferTextureMultiviewOVR",
    "glFramebufferTextureMultisampleMultiviewOVR",
    "glAlphaFuncQCOM",
    "glGetDriverControlsQCOM",
    "glGetDriverControlStringQCOM",
    "glEnableDriverControlQCOM",
    "glDisableDriverControlQCOM",
    "glExtGetTexturesQCOM",
    "glExtGetBuffersQCOM",
    "glExtGetRenderbuffersQCOM",
    "glExtGetFramebuffersQCOM",
    "glExtGetTexLevelParameterivQCOM",
    "glExtTexObjectStateOverrideiQCOM",
    "glExtGetTexSubImageQCOM",
    "glExtGetBufferPointervQCOM",
    "glExtGetShadersQCOM",
    "glExtGetProgramsQCOM",
    "glExtIsProgramBinaryQCOM",
    "glExtGetProgramBinarySourceQCOM",
    "glFramebufferFoveationConfigQCOM",
    "glFramebufferFoveationParametersQCOM",
    "glFramebufferFetchBarrierQCOM",
    "glTextureFoveationParametersQCOM",
    "glStartTilingQCOM",
    "glEndTilingQCOM",
    "glAlphaFunc",
    "glClipPlanef",
    "glColor4f",
    "glFogf",
    "glFogfv",
    "glFrustumf",
    "glGetClipPlanef",
    "glGetLightfv",
    "glGetMaterialfv",
    "glGetTexEnvfv",
    "glLightModelf",
    "glLightModelfv",
    "glLightf",
    "glLightfv",
    "glLoadMatrixf",
    "glMaterialf",
    "glMaterialfv",
    "glMultMatrixf",
    "glMultiTexCoord4f",
    "glNormal3f",
    "glOrthof",
    "glPointParameterf",
    "glPointParameterfv",
    "glPointSize",
    "glRotatef",
    "glScalef",
    "glTexEnvf",
    "glTexEnvfv",
    "glTranslatef",
    "glAlphaFuncx",
    "glClearColorx",
    "glClearDepthx",
    "glClientActiveTexture",
    "glClipPlanex",
    "glColor4ub",
    "glColor4x",
    "glColorPointer",
    "glDepthRangex",
    "glDisableClientState",
    "glEnableClientState",
    "glFogx",
    "glFogxv",
    "glFrustumx",
    "glGetClipPlanex",
    "glGetFixedv",
    "glGetLightxv",
    "glGetMaterialxv",
    "glGetPointerv",
    "glGetTexEnviv",
    "glGetTexEnvxv",
    "glGetTexParameterxv",
    "glLightModelx",
    "glLightModelxv",
    "glLightx",
    "glLightxv",
    "glLineWidthx",
    "glLoadIdentity",
    "glLoadMatrixx",
    "glLogicOp",
    "glMaterialx",
    "glMaterialxv",
    "glMatrixMode",
    "glMultMatrixx",
    "glMultiTexCoord4x",
    "glNormal3x",
    "glNormalPointer",
    "glOrthox",
    "glPointParameterx",
    "glPointParameterxv",
    "glPointSizex",
    "glPolygonOffsetx",
    "glPopMatrix",
    "glPushMatrix",
    "glRotatex",
    "glSampleCoveragex",
    "glScalex",
    "glShadeModel",
    "glTexCoordPointer",
    "glTexEnvi",
    "glTexEnvx",
    "glTexEnviv",
    "glTexEnvxv",
    "glTexParameterx",
    "glTexParameterxv",
    "glTranslatex",
    "glVertexPointer",
    "glPointSizePointerOES",
    "glBlendEquationSeparateOES",
    "glBlendFuncSeparateOES",
    "glBlendEquationOES",
    "glDrawTexsOES",
    "glDrawTexiOES",
    "glDrawTexxOES",
    "glDrawTexsvOES",
    "glDrawTexivOES",
    "glDrawTexxvOES",
    "glDrawTexfOES",
    "glDrawTexfvOES",
    "glAlphaFuncxOES",
    "glClearColorxOES",
    "glClearDepthxOES",
    "glClipPlanexOES",
    "glColor4xOES",
    "glDepthRangexOES",
    "glFogxOES",
    "glFogxvOES",
    "glFrustumxOES",
    "glGetClipPlanexOES",
    "glGetFixedvOES",
    "glGetTexEnvxvOES",
    "glGetTexParameterxvOES",
    "glLightModelxOES",
    "glLightModelxvOES",
    "glLightxOES",
    "glLightxvOES",
    "glLineWidthxOES",
    "glLoadMatrixxOES",
    "glMaterialxOES",
    "glMaterialxvOES",
    "glMultMatrixxOES",
    "glMultiTexCoord4xOES",
    "glNormal3xOES",
    "glOrthoxOES",
    "glPointParameterxvOES",
    "glPointSizexOES",
    "glPolygonOffsetxOES",
    "glRotatexOES",
    "glScalexOES",
    "glTexEnvxOES",
    "glTexEnvxvOES",
    "glTexParameterxOES",
    "glTexParameterxvOES",
    "glTranslatexOES",
    "glGetLightxvOES",
    "glGetMaterialxvOES",
    "glPointParameterxOES",
    "glSampleCoveragexOES",
    "glGetTexGenxvOES",
    "glTexGenxOES",
    "glTexGenxvOES",
    "glIsRenderbufferOES",
    "glBindRenderbufferOES",
    "glDeleteRenderbuffersOES",
    "glGenRenderbuffersOES",
    "glRenderbufferStorageOES",
    "glGetRenderbufferParameterivOES",
    "glIsFramebufferOES",
    "glBindFramebufferOES",
    "glDeleteFramebuffersOES",
    "glGenFramebuffersOES",
    "glCheckFramebufferStatusOES",
    "glFramebufferRenderbufferOES",
    "glFramebufferTexture2DOES",
    "glGetFramebufferAttachmentParameterivOES",
    "glGenerateMipmapOES",
    "glCurrentPaletteMatrixOES",
    "glLoadPaletteFromModelViewMatrixOES",
    "glMatrixIndexPointerOES",
    "glWeightPointerOES",
    "glQueryMatrixxOES",
    "glClearDepthfOES",
    "glClipPlanefOES",
    "glDepthRangefOES",
    "glFrustumfOES",
    "glGetClipPlanefOES",
    "glOrthofOES",
    "glTexGenfOES",
    "glTexGenfvOES",
    "glTexGeniOES",
    "glTexGenivOES",
    "glGetTexGenfvOES",
    "glGetTexGenivOES",
    "glClipPlanefIMG",
    "glClipPlanexIMG",
};

EXPORT void glActiveTexture(GLenum texture) { ((void (*)(GLenum))TBL()[IDX_glActiveTexture])(texture); }
EXPORT void glAttachShader(GLuint program, GLuint shader) { ((void (*)(GLuint, GLuint))TBL()[IDX_glAttachShader])(program, shader); }
EXPORT void glBindAttribLocation(GLuint program, GLuint index, const GLchar * name) { ((void (*)(GLuint, GLuint, const GLchar *))TBL()[IDX_glBindAttribLocation])(program, index, name); }
EXPORT void glBindBuffer(GLenum target, GLuint buffer) { ((void (*)(GLenum, GLuint))TBL()[IDX_glBindBuffer])(target, buffer); }
EXPORT void glBindFramebuffer(GLenum target, GLuint framebuffer) { ((void (*)(GLenum, GLuint))TBL()[IDX_glBindFramebuffer])(target, framebuffer); }
EXPORT void glBindRenderbuffer(GLenum target, GLuint renderbuffer) { ((void (*)(GLenum, GLuint))TBL()[IDX_glBindRenderbuffer])(target, renderbuffer); }
EXPORT void glBindTexture(GLenum target, GLuint texture) { ((void (*)(GLenum, GLuint))TBL()[IDX_glBindTexture])(target, texture); }
EXPORT void glBlendColor(GLfloat red, GLfloat green, GLfloat blue, GLfloat alpha) { ((void (*)(GLfloat, GLfloat, GLfloat, GLfloat))TBL()[IDX_glBlendColor])(red, green, blue, alpha); }
EXPORT void glBlendEquation(GLenum mode) { ((void (*)(GLenum))TBL()[IDX_glBlendEquation])(mode); }
EXPORT void glBlendEquationSeparate(GLenum modeRGB, GLenum modeAlpha) { ((void (*)(GLenum, GLenum))TBL()[IDX_glBlendEquationSeparate])(modeRGB, modeAlpha); }
EXPORT void glBlendFunc(GLenum sfactor, GLenum dfactor) { ((void (*)(GLenum, GLenum))TBL()[IDX_glBlendFunc])(sfactor, dfactor); }
EXPORT void glBlendFuncSeparate(GLenum sfactorRGB, GLenum dfactorRGB, GLenum sfactorAlpha, GLenum dfactorAlpha) { ((void (*)(GLenum, GLenum, GLenum, GLenum))TBL()[IDX_glBlendFuncSeparate])(sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha); }
EXPORT void glBufferData(GLenum target, GLsizeiptr size, const void * data, GLenum usage) { ((void (*)(GLenum, GLsizeiptr, const void *, GLenum))TBL()[IDX_glBufferData])(target, size, data, usage); }
EXPORT void glBufferSubData(GLenum target, GLintptr offset, GLsizeiptr size, const void * data) { ((void (*)(GLenum, GLintptr, GLsizeiptr, const void *))TBL()[IDX_glBufferSubData])(target, offset, size, data); }
EXPORT GLenum glCheckFramebufferStatus(GLenum target) { return ((GLenum (*)(GLenum))TBL()[IDX_glCheckFramebufferStatus])(target); }
EXPORT void glClear(GLbitfield mask) { ((void (*)(GLbitfield))TBL()[IDX_glClear])(mask); }
EXPORT void glClearColor(GLfloat red, GLfloat green, GLfloat blue, GLfloat alpha) { ((void (*)(GLfloat, GLfloat, GLfloat, GLfloat))TBL()[IDX_glClearColor])(red, green, blue, alpha); }
EXPORT void glClearDepthf(GLfloat d) { ((void (*)(GLfloat))TBL()[IDX_glClearDepthf])(d); }
EXPORT void glClearStencil(GLint s) { ((void (*)(GLint))TBL()[IDX_glClearStencil])(s); }
EXPORT void glColorMask(GLboolean red, GLboolean green, GLboolean blue, GLboolean alpha) { ((void (*)(GLboolean, GLboolean, GLboolean, GLboolean))TBL()[IDX_glColorMask])(red, green, blue, alpha); }
EXPORT void glCompileShader(GLuint shader) { ((void (*)(GLuint))TBL()[IDX_glCompileShader])(shader); }
EXPORT void glCompressedTexImage2D(GLenum target, GLint level, GLenum internalformat, GLsizei width, GLsizei height, GLint border, GLsizei imageSize, const void * data) { ((void (*)(GLenum, GLint, GLenum, GLsizei, GLsizei, GLint, GLsizei, const void *))TBL()[IDX_glCompressedTexImage2D])(target, level, internalformat, width, height, border, imageSize, data); }
EXPORT void glCompressedTexSubImage2D(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLsizei imageSize, const void * data) { ((void (*)(GLenum, GLint, GLint, GLint, GLsizei, GLsizei, GLenum, GLsizei, const void *))TBL()[IDX_glCompressedTexSubImage2D])(target, level, xoffset, yoffset, width, height, format, imageSize, data); }
EXPORT void glCopyTexImage2D(GLenum target, GLint level, GLenum internalformat, GLint x, GLint y, GLsizei width, GLsizei height, GLint border) { ((void (*)(GLenum, GLint, GLenum, GLint, GLint, GLsizei, GLsizei, GLint))TBL()[IDX_glCopyTexImage2D])(target, level, internalformat, x, y, width, height, border); }
EXPORT void glCopyTexSubImage2D(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint x, GLint y, GLsizei width, GLsizei height) { ((void (*)(GLenum, GLint, GLint, GLint, GLint, GLint, GLsizei, GLsizei))TBL()[IDX_glCopyTexSubImage2D])(target, level, xoffset, yoffset, x, y, width, height); }
EXPORT GLuint glCreateProgram(void) { return ((GLuint (*)(void))TBL()[IDX_glCreateProgram])(); }
EXPORT GLuint glCreateShader(GLenum type) { return ((GLuint (*)(GLenum))TBL()[IDX_glCreateShader])(type); }
EXPORT void glCullFace(GLenum mode) { ((void (*)(GLenum))TBL()[IDX_glCullFace])(mode); }
EXPORT void glDeleteBuffers(GLsizei n, const GLuint * buffers) { ((void (*)(GLsizei, const GLuint *))TBL()[IDX_glDeleteBuffers])(n, buffers); }
EXPORT void glDeleteFramebuffers(GLsizei n, const GLuint * framebuffers) { ((void (*)(GLsizei, const GLuint *))TBL()[IDX_glDeleteFramebuffers])(n, framebuffers); }
EXPORT void glDeleteProgram(GLuint program) { ((void (*)(GLuint))TBL()[IDX_glDeleteProgram])(program); }
EXPORT void glDeleteRenderbuffers(GLsizei n, const GLuint * renderbuffers) { ((void (*)(GLsizei, const GLuint *))TBL()[IDX_glDeleteRenderbuffers])(n, renderbuffers); }
EXPORT void glDeleteShader(GLuint shader) { ((void (*)(GLuint))TBL()[IDX_glDeleteShader])(shader); }
EXPORT void glDeleteTextures(GLsizei n, const GLuint * textures) { ((void (*)(GLsizei, const GLuint *))TBL()[IDX_glDeleteTextures])(n, textures); }
EXPORT void glDepthFunc(GLenum func) { ((void (*)(GLenum))TBL()[IDX_glDepthFunc])(func); }
EXPORT void glDepthMask(GLboolean flag) { ((void (*)(GLboolean))TBL()[IDX_glDepthMask])(flag); }
EXPORT void glDepthRangef(GLfloat n, GLfloat f) { ((void (*)(GLfloat, GLfloat))TBL()[IDX_glDepthRangef])(n, f); }
EXPORT void glDetachShader(GLuint program, GLuint shader) { ((void (*)(GLuint, GLuint))TBL()[IDX_glDetachShader])(program, shader); }
EXPORT void glDisable(GLenum cap) { ((void (*)(GLenum))TBL()[IDX_glDisable])(cap); }
EXPORT void glDisableVertexAttribArray(GLuint index) { ((void (*)(GLuint))TBL()[IDX_glDisableVertexAttribArray])(index); }
EXPORT void glDrawArrays(GLenum mode, GLint first, GLsizei count) { ((void (*)(GLenum, GLint, GLsizei))TBL()[IDX_glDrawArrays])(mode, first, count); }
EXPORT void glDrawElements(GLenum mode, GLsizei count, GLenum type, const void * indices) { ((void (*)(GLenum, GLsizei, GLenum, const void *))TBL()[IDX_glDrawElements])(mode, count, type, indices); }
EXPORT void glEnable(GLenum cap) { ((void (*)(GLenum))TBL()[IDX_glEnable])(cap); }
EXPORT void glEnableVertexAttribArray(GLuint index) { ((void (*)(GLuint))TBL()[IDX_glEnableVertexAttribArray])(index); }
EXPORT void glFinish(void) { ((void (*)(void))TBL()[IDX_glFinish])(); }
EXPORT void glFlush(void) { ((void (*)(void))TBL()[IDX_glFlush])(); }
EXPORT void glFramebufferRenderbuffer(GLenum target, GLenum attachment, GLenum renderbuffertarget, GLuint renderbuffer) { ((void (*)(GLenum, GLenum, GLenum, GLuint))TBL()[IDX_glFramebufferRenderbuffer])(target, attachment, renderbuffertarget, renderbuffer); }
EXPORT void glFramebufferTexture2D(GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level) { ((void (*)(GLenum, GLenum, GLenum, GLuint, GLint))TBL()[IDX_glFramebufferTexture2D])(target, attachment, textarget, texture, level); }
EXPORT void glFrontFace(GLenum mode) { ((void (*)(GLenum))TBL()[IDX_glFrontFace])(mode); }
EXPORT void glGenBuffers(GLsizei n, GLuint * buffers) { ((void (*)(GLsizei, GLuint *))TBL()[IDX_glGenBuffers])(n, buffers); }
EXPORT void glGenerateMipmap(GLenum target) { ((void (*)(GLenum))TBL()[IDX_glGenerateMipmap])(target); }
EXPORT void glGenFramebuffers(GLsizei n, GLuint * framebuffers) { ((void (*)(GLsizei, GLuint *))TBL()[IDX_glGenFramebuffers])(n, framebuffers); }
EXPORT void glGenRenderbuffers(GLsizei n, GLuint * renderbuffers) { ((void (*)(GLsizei, GLuint *))TBL()[IDX_glGenRenderbuffers])(n, renderbuffers); }
EXPORT void glGenTextures(GLsizei n, GLuint * textures) { ((void (*)(GLsizei, GLuint *))TBL()[IDX_glGenTextures])(n, textures); }
EXPORT void glGetActiveAttrib(GLuint program, GLuint index, GLsizei bufSize, GLsizei * length, GLint * size, GLenum * type, GLchar * name) { ((void (*)(GLuint, GLuint, GLsizei, GLsizei *, GLint *, GLenum *, GLchar *))TBL()[IDX_glGetActiveAttrib])(program, index, bufSize, length, size, type, name); }
EXPORT void glGetActiveUniform(GLuint program, GLuint index, GLsizei bufSize, GLsizei * length, GLint * size, GLenum * type, GLchar * name) { ((void (*)(GLuint, GLuint, GLsizei, GLsizei *, GLint *, GLenum *, GLchar *))TBL()[IDX_glGetActiveUniform])(program, index, bufSize, length, size, type, name); }
EXPORT void glGetAttachedShaders(GLuint program, GLsizei maxCount, GLsizei * count, GLuint * shaders) { ((void (*)(GLuint, GLsizei, GLsizei *, GLuint *))TBL()[IDX_glGetAttachedShaders])(program, maxCount, count, shaders); }
EXPORT GLint glGetAttribLocation(GLuint program, const GLchar * name) { return ((GLint (*)(GLuint, const GLchar *))TBL()[IDX_glGetAttribLocation])(program, name); }
EXPORT void glGetBooleanv(GLenum pname, GLboolean * data) { ((void (*)(GLenum, GLboolean *))TBL()[IDX_glGetBooleanv])(pname, data); }
EXPORT void glGetBufferParameteriv(GLenum target, GLenum pname, GLint * params) { ((void (*)(GLenum, GLenum, GLint *))TBL()[IDX_glGetBufferParameteriv])(target, pname, params); }
EXPORT GLenum glGetError(void) { return ((GLenum (*)(void))TBL()[IDX_glGetError])(); }
EXPORT void glGetFloatv(GLenum pname, GLfloat * data) { ((void (*)(GLenum, GLfloat *))TBL()[IDX_glGetFloatv])(pname, data); }
EXPORT void glGetFramebufferAttachmentParameteriv(GLenum target, GLenum attachment, GLenum pname, GLint * params) { ((void (*)(GLenum, GLenum, GLenum, GLint *))TBL()[IDX_glGetFramebufferAttachmentParameteriv])(target, attachment, pname, params); }
EXPORT void glGetIntegerv(GLenum pname, GLint * data) { ((void (*)(GLenum, GLint *))TBL()[IDX_glGetIntegerv])(pname, data); }
EXPORT void glGetProgramiv(GLuint program, GLenum pname, GLint * params) { ((void (*)(GLuint, GLenum, GLint *))TBL()[IDX_glGetProgramiv])(program, pname, params); }
EXPORT void glGetProgramInfoLog(GLuint program, GLsizei bufSize, GLsizei * length, GLchar * infoLog) { ((void (*)(GLuint, GLsizei, GLsizei *, GLchar *))TBL()[IDX_glGetProgramInfoLog])(program, bufSize, length, infoLog); }
EXPORT void glGetRenderbufferParameteriv(GLenum target, GLenum pname, GLint * params) { ((void (*)(GLenum, GLenum, GLint *))TBL()[IDX_glGetRenderbufferParameteriv])(target, pname, params); }
EXPORT void glGetShaderiv(GLuint shader, GLenum pname, GLint * params) { ((void (*)(GLuint, GLenum, GLint *))TBL()[IDX_glGetShaderiv])(shader, pname, params); }
EXPORT void glGetShaderInfoLog(GLuint shader, GLsizei bufSize, GLsizei * length, GLchar * infoLog) { ((void (*)(GLuint, GLsizei, GLsizei *, GLchar *))TBL()[IDX_glGetShaderInfoLog])(shader, bufSize, length, infoLog); }
EXPORT void glGetShaderPrecisionFormat(GLenum shadertype, GLenum precisiontype, GLint * range, GLint * precision) { ((void (*)(GLenum, GLenum, GLint *, GLint *))TBL()[IDX_glGetShaderPrecisionFormat])(shadertype, precisiontype, range, precision); }
EXPORT void glGetShaderSource(GLuint shader, GLsizei bufSize, GLsizei * length, GLchar * source) { ((void (*)(GLuint, GLsizei, GLsizei *, GLchar *))TBL()[IDX_glGetShaderSource])(shader, bufSize, length, source); }
EXPORT const GLubyte * glGetString(GLenum name) { return ((const GLubyte * (*)(GLenum))TBL()[IDX_glGetString])(name); }
EXPORT void glGetTexParameterfv(GLenum target, GLenum pname, GLfloat * params) { ((void (*)(GLenum, GLenum, GLfloat *))TBL()[IDX_glGetTexParameterfv])(target, pname, params); }
EXPORT void glGetTexParameteriv(GLenum target, GLenum pname, GLint * params) { ((void (*)(GLenum, GLenum, GLint *))TBL()[IDX_glGetTexParameteriv])(target, pname, params); }
EXPORT void glGetUniformfv(GLuint program, GLint location, GLfloat * params) { ((void (*)(GLuint, GLint, GLfloat *))TBL()[IDX_glGetUniformfv])(program, location, params); }
EXPORT void glGetUniformiv(GLuint program, GLint location, GLint * params) { ((void (*)(GLuint, GLint, GLint *))TBL()[IDX_glGetUniformiv])(program, location, params); }
EXPORT GLint glGetUniformLocation(GLuint program, const GLchar * name) { return ((GLint (*)(GLuint, const GLchar *))TBL()[IDX_glGetUniformLocation])(program, name); }
EXPORT void glGetVertexAttribfv(GLuint index, GLenum pname, GLfloat * params) { ((void (*)(GLuint, GLenum, GLfloat *))TBL()[IDX_glGetVertexAttribfv])(index, pname, params); }
EXPORT void glGetVertexAttribiv(GLuint index, GLenum pname, GLint * params) { ((void (*)(GLuint, GLenum, GLint *))TBL()[IDX_glGetVertexAttribiv])(index, pname, params); }
EXPORT void glGetVertexAttribPointerv(GLuint index, GLenum pname, void ** pointer) { ((void (*)(GLuint, GLenum, void **))TBL()[IDX_glGetVertexAttribPointerv])(index, pname, pointer); }
EXPORT void glHint(GLenum target, GLenum mode) { ((void (*)(GLenum, GLenum))TBL()[IDX_glHint])(target, mode); }
EXPORT GLboolean glIsBuffer(GLuint buffer) { return ((GLboolean (*)(GLuint))TBL()[IDX_glIsBuffer])(buffer); }
EXPORT GLboolean glIsEnabled(GLenum cap) { return ((GLboolean (*)(GLenum))TBL()[IDX_glIsEnabled])(cap); }
EXPORT GLboolean glIsFramebuffer(GLuint framebuffer) { return ((GLboolean (*)(GLuint))TBL()[IDX_glIsFramebuffer])(framebuffer); }
EXPORT GLboolean glIsProgram(GLuint program) { return ((GLboolean (*)(GLuint))TBL()[IDX_glIsProgram])(program); }
EXPORT GLboolean glIsRenderbuffer(GLuint renderbuffer) { return ((GLboolean (*)(GLuint))TBL()[IDX_glIsRenderbuffer])(renderbuffer); }
EXPORT GLboolean glIsShader(GLuint shader) { return ((GLboolean (*)(GLuint))TBL()[IDX_glIsShader])(shader); }
EXPORT GLboolean glIsTexture(GLuint texture) { return ((GLboolean (*)(GLuint))TBL()[IDX_glIsTexture])(texture); }
EXPORT void glLineWidth(GLfloat width) { ((void (*)(GLfloat))TBL()[IDX_glLineWidth])(width); }
EXPORT void glLinkProgram(GLuint program) { ((void (*)(GLuint))TBL()[IDX_glLinkProgram])(program); }
EXPORT void glPixelStorei(GLenum pname, GLint param) { ((void (*)(GLenum, GLint))TBL()[IDX_glPixelStorei])(pname, param); }
EXPORT void glPolygonOffset(GLfloat factor, GLfloat units) { ((void (*)(GLfloat, GLfloat))TBL()[IDX_glPolygonOffset])(factor, units); }
EXPORT void glReadPixels(GLint x, GLint y, GLsizei width, GLsizei height, GLenum format, GLenum type, void * pixels) { ((void (*)(GLint, GLint, GLsizei, GLsizei, GLenum, GLenum, void *))TBL()[IDX_glReadPixels])(x, y, width, height, format, type, pixels); }
EXPORT void glReleaseShaderCompiler(void) { ((void (*)(void))TBL()[IDX_glReleaseShaderCompiler])(); }
EXPORT void glRenderbufferStorage(GLenum target, GLenum internalformat, GLsizei width, GLsizei height) { ((void (*)(GLenum, GLenum, GLsizei, GLsizei))TBL()[IDX_glRenderbufferStorage])(target, internalformat, width, height); }
EXPORT void glSampleCoverage(GLfloat value, GLboolean invert) { ((void (*)(GLfloat, GLboolean))TBL()[IDX_glSampleCoverage])(value, invert); }
EXPORT void glScissor(GLint x, GLint y, GLsizei width, GLsizei height) { ((void (*)(GLint, GLint, GLsizei, GLsizei))TBL()[IDX_glScissor])(x, y, width, height); }
EXPORT void glShaderBinary(GLsizei count, const GLuint * shaders, GLenum binaryformat, const void * binary, GLsizei length) { ((void (*)(GLsizei, const GLuint *, GLenum, const void *, GLsizei))TBL()[IDX_glShaderBinary])(count, shaders, binaryformat, binary, length); }
EXPORT void glShaderSource(GLuint shader, GLsizei count, const GLchar *const* string, const GLint * length) { ((void (*)(GLuint, GLsizei, const GLchar *const*, const GLint *))TBL()[IDX_glShaderSource])(shader, count, string, length); }
EXPORT void glStencilFunc(GLenum func, GLint ref, GLuint mask) { ((void (*)(GLenum, GLint, GLuint))TBL()[IDX_glStencilFunc])(func, ref, mask); }
EXPORT void glStencilFuncSeparate(GLenum face, GLenum func, GLint ref, GLuint mask) { ((void (*)(GLenum, GLenum, GLint, GLuint))TBL()[IDX_glStencilFuncSeparate])(face, func, ref, mask); }
EXPORT void glStencilMask(GLuint mask) { ((void (*)(GLuint))TBL()[IDX_glStencilMask])(mask); }
EXPORT void glStencilMaskSeparate(GLenum face, GLuint mask) { ((void (*)(GLenum, GLuint))TBL()[IDX_glStencilMaskSeparate])(face, mask); }
EXPORT void glStencilOp(GLenum fail, GLenum zfail, GLenum zpass) { ((void (*)(GLenum, GLenum, GLenum))TBL()[IDX_glStencilOp])(fail, zfail, zpass); }
EXPORT void glStencilOpSeparate(GLenum face, GLenum sfail, GLenum dpfail, GLenum dppass) { ((void (*)(GLenum, GLenum, GLenum, GLenum))TBL()[IDX_glStencilOpSeparate])(face, sfail, dpfail, dppass); }
EXPORT void glTexImage2D(GLenum target, GLint level, GLint internalformat, GLsizei width, GLsizei height, GLint border, GLenum format, GLenum type, const void * pixels) { ((void (*)(GLenum, GLint, GLint, GLsizei, GLsizei, GLint, GLenum, GLenum, const void *))TBL()[IDX_glTexImage2D])(target, level, internalformat, width, height, border, format, type, pixels); }
EXPORT void glTexParameterf(GLenum target, GLenum pname, GLfloat param) { ((void (*)(GLenum, GLenum, GLfloat))TBL()[IDX_glTexParameterf])(target, pname, param); }
EXPORT void glTexParameterfv(GLenum target, GLenum pname, const GLfloat * params) { ((void (*)(GLenum, GLenum, const GLfloat *))TBL()[IDX_glTexParameterfv])(target, pname, params); }
EXPORT void glTexParameteri(GLenum target, GLenum pname, GLint param) { ((void (*)(GLenum, GLenum, GLint))TBL()[IDX_glTexParameteri])(target, pname, param); }
EXPORT void glTexParameteriv(GLenum target, GLenum pname, const GLint * params) { ((void (*)(GLenum, GLenum, const GLint *))TBL()[IDX_glTexParameteriv])(target, pname, params); }
EXPORT void glTexSubImage2D(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLenum type, const void * pixels) { ((void (*)(GLenum, GLint, GLint, GLint, GLsizei, GLsizei, GLenum, GLenum, const void *))TBL()[IDX_glTexSubImage2D])(target, level, xoffset, yoffset, width, height, format, type, pixels); }
EXPORT void glUniform1f(GLint location, GLfloat v0) { ((void (*)(GLint, GLfloat))TBL()[IDX_glUniform1f])(location, v0); }
EXPORT void glUniform1fv(GLint location, GLsizei count, const GLfloat * value) { ((void (*)(GLint, GLsizei, const GLfloat *))TBL()[IDX_glUniform1fv])(location, count, value); }
EXPORT void glUniform1i(GLint location, GLint v0) { ((void (*)(GLint, GLint))TBL()[IDX_glUniform1i])(location, v0); }
EXPORT void glUniform1iv(GLint location, GLsizei count, const GLint * value) { ((void (*)(GLint, GLsizei, const GLint *))TBL()[IDX_glUniform1iv])(location, count, value); }
EXPORT void glUniform2f(GLint location, GLfloat v0, GLfloat v1) { ((void (*)(GLint, GLfloat, GLfloat))TBL()[IDX_glUniform2f])(location, v0, v1); }
EXPORT void glUniform2fv(GLint location, GLsizei count, const GLfloat * value) { ((void (*)(GLint, GLsizei, const GLfloat *))TBL()[IDX_glUniform2fv])(location, count, value); }
EXPORT void glUniform2i(GLint location, GLint v0, GLint v1) { ((void (*)(GLint, GLint, GLint))TBL()[IDX_glUniform2i])(location, v0, v1); }
EXPORT void glUniform2iv(GLint location, GLsizei count, const GLint * value) { ((void (*)(GLint, GLsizei, const GLint *))TBL()[IDX_glUniform2iv])(location, count, value); }
EXPORT void glUniform3f(GLint location, GLfloat v0, GLfloat v1, GLfloat v2) { ((void (*)(GLint, GLfloat, GLfloat, GLfloat))TBL()[IDX_glUniform3f])(location, v0, v1, v2); }
EXPORT void glUniform3fv(GLint location, GLsizei count, const GLfloat * value) { ((void (*)(GLint, GLsizei, const GLfloat *))TBL()[IDX_glUniform3fv])(location, count, value); }
EXPORT void glUniform3i(GLint location, GLint v0, GLint v1, GLint v2) { ((void (*)(GLint, GLint, GLint, GLint))TBL()[IDX_glUniform3i])(location, v0, v1, v2); }
EXPORT void glUniform3iv(GLint location, GLsizei count, const GLint * value) { ((void (*)(GLint, GLsizei, const GLint *))TBL()[IDX_glUniform3iv])(location, count, value); }
EXPORT void glUniform4f(GLint location, GLfloat v0, GLfloat v1, GLfloat v2, GLfloat v3) { ((void (*)(GLint, GLfloat, GLfloat, GLfloat, GLfloat))TBL()[IDX_glUniform4f])(location, v0, v1, v2, v3); }
EXPORT void glUniform4fv(GLint location, GLsizei count, const GLfloat * value) { ((void (*)(GLint, GLsizei, const GLfloat *))TBL()[IDX_glUniform4fv])(location, count, value); }
EXPORT void glUniform4i(GLint location, GLint v0, GLint v1, GLint v2, GLint v3) { ((void (*)(GLint, GLint, GLint, GLint, GLint))TBL()[IDX_glUniform4i])(location, v0, v1, v2, v3); }
EXPORT void glUniform4iv(GLint location, GLsizei count, const GLint * value) { ((void (*)(GLint, GLsizei, const GLint *))TBL()[IDX_glUniform4iv])(location, count, value); }
EXPORT void glUniformMatrix2fv(GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { ((void (*)(GLint, GLsizei, GLboolean, const GLfloat *))TBL()[IDX_glUniformMatrix2fv])(location, count, transpose, value); }
EXPORT void glUniformMatrix3fv(GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { ((void (*)(GLint, GLsizei, GLboolean, const GLfloat *))TBL()[IDX_glUniformMatrix3fv])(location, count, transpose, value); }
EXPORT void glUniformMatrix4fv(GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { ((void (*)(GLint, GLsizei, GLboolean, const GLfloat *))TBL()[IDX_glUniformMatrix4fv])(location, count, transpose, value); }
EXPORT void glUseProgram(GLuint program) { ((void (*)(GLuint))TBL()[IDX_glUseProgram])(program); }
EXPORT void glValidateProgram(GLuint program) { ((void (*)(GLuint))TBL()[IDX_glValidateProgram])(program); }
EXPORT void glVertexAttrib1f(GLuint index, GLfloat x) { ((void (*)(GLuint, GLfloat))TBL()[IDX_glVertexAttrib1f])(index, x); }
EXPORT void glVertexAttrib1fv(GLuint index, const GLfloat * v) { ((void (*)(GLuint, const GLfloat *))TBL()[IDX_glVertexAttrib1fv])(index, v); }
EXPORT void glVertexAttrib2f(GLuint index, GLfloat x, GLfloat y) { ((void (*)(GLuint, GLfloat, GLfloat))TBL()[IDX_glVertexAttrib2f])(index, x, y); }
EXPORT void glVertexAttrib2fv(GLuint index, const GLfloat * v) { ((void (*)(GLuint, const GLfloat *))TBL()[IDX_glVertexAttrib2fv])(index, v); }
EXPORT void glVertexAttrib3f(GLuint index, GLfloat x, GLfloat y, GLfloat z) { ((void (*)(GLuint, GLfloat, GLfloat, GLfloat))TBL()[IDX_glVertexAttrib3f])(index, x, y, z); }
EXPORT void glVertexAttrib3fv(GLuint index, const GLfloat * v) { ((void (*)(GLuint, const GLfloat *))TBL()[IDX_glVertexAttrib3fv])(index, v); }
EXPORT void glVertexAttrib4f(GLuint index, GLfloat x, GLfloat y, GLfloat z, GLfloat w) { ((void (*)(GLuint, GLfloat, GLfloat, GLfloat, GLfloat))TBL()[IDX_glVertexAttrib4f])(index, x, y, z, w); }
EXPORT void glVertexAttrib4fv(GLuint index, const GLfloat * v) { ((void (*)(GLuint, const GLfloat *))TBL()[IDX_glVertexAttrib4fv])(index, v); }
EXPORT void glVertexAttribPointer(GLuint index, GLint size, GLenum type, GLboolean normalized, GLsizei stride, const void * pointer) { ((void (*)(GLuint, GLint, GLenum, GLboolean, GLsizei, const void *))TBL()[IDX_glVertexAttribPointer])(index, size, type, normalized, stride, pointer); }
EXPORT void glViewport(GLint x, GLint y, GLsizei width, GLsizei height) { ((void (*)(GLint, GLint, GLsizei, GLsizei))TBL()[IDX_glViewport])(x, y, width, height); }
EXPORT void glReadBuffer(GLenum src) { ((void (*)(GLenum))TBL()[IDX_glReadBuffer])(src); }
EXPORT void glDrawRangeElements(GLenum mode, GLuint start, GLuint end, GLsizei count, GLenum type, const void * indices) { ((void (*)(GLenum, GLuint, GLuint, GLsizei, GLenum, const void *))TBL()[IDX_glDrawRangeElements])(mode, start, end, count, type, indices); }
EXPORT void glTexImage3D(GLenum target, GLint level, GLint internalformat, GLsizei width, GLsizei height, GLsizei depth, GLint border, GLenum format, GLenum type, const void * pixels) { ((void (*)(GLenum, GLint, GLint, GLsizei, GLsizei, GLsizei, GLint, GLenum, GLenum, const void *))TBL()[IDX_glTexImage3D])(target, level, internalformat, width, height, depth, border, format, type, pixels); }
EXPORT void glTexSubImage3D(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint zoffset, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, const void * pixels) { ((void (*)(GLenum, GLint, GLint, GLint, GLint, GLsizei, GLsizei, GLsizei, GLenum, GLenum, const void *))TBL()[IDX_glTexSubImage3D])(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, pixels); }
EXPORT void glCopyTexSubImage3D(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint zoffset, GLint x, GLint y, GLsizei width, GLsizei height) { ((void (*)(GLenum, GLint, GLint, GLint, GLint, GLint, GLint, GLsizei, GLsizei))TBL()[IDX_glCopyTexSubImage3D])(target, level, xoffset, yoffset, zoffset, x, y, width, height); }
EXPORT void glCompressedTexImage3D(GLenum target, GLint level, GLenum internalformat, GLsizei width, GLsizei height, GLsizei depth, GLint border, GLsizei imageSize, const void * data) { ((void (*)(GLenum, GLint, GLenum, GLsizei, GLsizei, GLsizei, GLint, GLsizei, const void *))TBL()[IDX_glCompressedTexImage3D])(target, level, internalformat, width, height, depth, border, imageSize, data); }
EXPORT void glCompressedTexSubImage3D(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint zoffset, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLsizei imageSize, const void * data) { ((void (*)(GLenum, GLint, GLint, GLint, GLint, GLsizei, GLsizei, GLsizei, GLenum, GLsizei, const void *))TBL()[IDX_glCompressedTexSubImage3D])(target, level, xoffset, yoffset, zoffset, width, height, depth, format, imageSize, data); }
EXPORT void glGenQueries(GLsizei n, GLuint * ids) { ((void (*)(GLsizei, GLuint *))TBL()[IDX_glGenQueries])(n, ids); }
EXPORT void glDeleteQueries(GLsizei n, const GLuint * ids) { ((void (*)(GLsizei, const GLuint *))TBL()[IDX_glDeleteQueries])(n, ids); }
EXPORT GLboolean glIsQuery(GLuint id) { return ((GLboolean (*)(GLuint))TBL()[IDX_glIsQuery])(id); }
EXPORT void glBeginQuery(GLenum target, GLuint id) { ((void (*)(GLenum, GLuint))TBL()[IDX_glBeginQuery])(target, id); }
EXPORT void glEndQuery(GLenum target) { ((void (*)(GLenum))TBL()[IDX_glEndQuery])(target); }
EXPORT void glGetQueryiv(GLenum target, GLenum pname, GLint * params) { ((void (*)(GLenum, GLenum, GLint *))TBL()[IDX_glGetQueryiv])(target, pname, params); }
EXPORT void glGetQueryObjectuiv(GLuint id, GLenum pname, GLuint * params) { ((void (*)(GLuint, GLenum, GLuint *))TBL()[IDX_glGetQueryObjectuiv])(id, pname, params); }
EXPORT GLboolean glUnmapBuffer(GLenum target) { return ((GLboolean (*)(GLenum))TBL()[IDX_glUnmapBuffer])(target); }
EXPORT void glGetBufferPointerv(GLenum target, GLenum pname, void ** params) { ((void (*)(GLenum, GLenum, void **))TBL()[IDX_glGetBufferPointerv])(target, pname, params); }
EXPORT void glDrawBuffers(GLsizei n, const GLenum * bufs) { ((void (*)(GLsizei, const GLenum *))TBL()[IDX_glDrawBuffers])(n, bufs); }
EXPORT void glUniformMatrix2x3fv(GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { ((void (*)(GLint, GLsizei, GLboolean, const GLfloat *))TBL()[IDX_glUniformMatrix2x3fv])(location, count, transpose, value); }
EXPORT void glUniformMatrix3x2fv(GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { ((void (*)(GLint, GLsizei, GLboolean, const GLfloat *))TBL()[IDX_glUniformMatrix3x2fv])(location, count, transpose, value); }
EXPORT void glUniformMatrix2x4fv(GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { ((void (*)(GLint, GLsizei, GLboolean, const GLfloat *))TBL()[IDX_glUniformMatrix2x4fv])(location, count, transpose, value); }
EXPORT void glUniformMatrix4x2fv(GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { ((void (*)(GLint, GLsizei, GLboolean, const GLfloat *))TBL()[IDX_glUniformMatrix4x2fv])(location, count, transpose, value); }
EXPORT void glUniformMatrix3x4fv(GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { ((void (*)(GLint, GLsizei, GLboolean, const GLfloat *))TBL()[IDX_glUniformMatrix3x4fv])(location, count, transpose, value); }
EXPORT void glUniformMatrix4x3fv(GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { ((void (*)(GLint, GLsizei, GLboolean, const GLfloat *))TBL()[IDX_glUniformMatrix4x3fv])(location, count, transpose, value); }
EXPORT void glBlitFramebuffer(GLint srcX0, GLint srcY0, GLint srcX1, GLint srcY1, GLint dstX0, GLint dstY0, GLint dstX1, GLint dstY1, GLbitfield mask, GLenum filter) { ((void (*)(GLint, GLint, GLint, GLint, GLint, GLint, GLint, GLint, GLbitfield, GLenum))TBL()[IDX_glBlitFramebuffer])(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter); }
EXPORT void glRenderbufferStorageMultisample(GLenum target, GLsizei samples, GLenum internalformat, GLsizei width, GLsizei height) { ((void (*)(GLenum, GLsizei, GLenum, GLsizei, GLsizei))TBL()[IDX_glRenderbufferStorageMultisample])(target, samples, internalformat, width, height); }
EXPORT void glFramebufferTextureLayer(GLenum target, GLenum attachment, GLuint texture, GLint level, GLint layer) { ((void (*)(GLenum, GLenum, GLuint, GLint, GLint))TBL()[IDX_glFramebufferTextureLayer])(target, attachment, texture, level, layer); }
EXPORT void * glMapBufferRange(GLenum target, GLintptr offset, GLsizeiptr length, GLbitfield access) { return ((void * (*)(GLenum, GLintptr, GLsizeiptr, GLbitfield))TBL()[IDX_glMapBufferRange])(target, offset, length, access); }
EXPORT void glFlushMappedBufferRange(GLenum target, GLintptr offset, GLsizeiptr length) { ((void (*)(GLenum, GLintptr, GLsizeiptr))TBL()[IDX_glFlushMappedBufferRange])(target, offset, length); }
EXPORT void glBindVertexArray(GLuint array) { ((void (*)(GLuint))TBL()[IDX_glBindVertexArray])(array); }
EXPORT void glDeleteVertexArrays(GLsizei n, const GLuint * arrays) { ((void (*)(GLsizei, const GLuint *))TBL()[IDX_glDeleteVertexArrays])(n, arrays); }
EXPORT void glGenVertexArrays(GLsizei n, GLuint * arrays) { ((void (*)(GLsizei, GLuint *))TBL()[IDX_glGenVertexArrays])(n, arrays); }
EXPORT GLboolean glIsVertexArray(GLuint array) { return ((GLboolean (*)(GLuint))TBL()[IDX_glIsVertexArray])(array); }
EXPORT void glGetIntegeri_v(GLenum target, GLuint index, GLint * data) { ((void (*)(GLenum, GLuint, GLint *))TBL()[IDX_glGetIntegeri_v])(target, index, data); }
EXPORT void glBeginTransformFeedback(GLenum primitiveMode) { ((void (*)(GLenum))TBL()[IDX_glBeginTransformFeedback])(primitiveMode); }
EXPORT void glEndTransformFeedback(void) { ((void (*)(void))TBL()[IDX_glEndTransformFeedback])(); }
EXPORT void glBindBufferRange(GLenum target, GLuint index, GLuint buffer, GLintptr offset, GLsizeiptr size) { ((void (*)(GLenum, GLuint, GLuint, GLintptr, GLsizeiptr))TBL()[IDX_glBindBufferRange])(target, index, buffer, offset, size); }
EXPORT void glBindBufferBase(GLenum target, GLuint index, GLuint buffer) { ((void (*)(GLenum, GLuint, GLuint))TBL()[IDX_glBindBufferBase])(target, index, buffer); }
EXPORT void glTransformFeedbackVaryings(GLuint program, GLsizei count, const GLchar *const* varyings, GLenum bufferMode) { ((void (*)(GLuint, GLsizei, const GLchar *const*, GLenum))TBL()[IDX_glTransformFeedbackVaryings])(program, count, varyings, bufferMode); }
EXPORT void glGetTransformFeedbackVarying(GLuint program, GLuint index, GLsizei bufSize, GLsizei * length, GLsizei * size, GLenum * type, GLchar * name) { ((void (*)(GLuint, GLuint, GLsizei, GLsizei *, GLsizei *, GLenum *, GLchar *))TBL()[IDX_glGetTransformFeedbackVarying])(program, index, bufSize, length, size, type, name); }
EXPORT void glVertexAttribIPointer(GLuint index, GLint size, GLenum type, GLsizei stride, const void * pointer) { ((void (*)(GLuint, GLint, GLenum, GLsizei, const void *))TBL()[IDX_glVertexAttribIPointer])(index, size, type, stride, pointer); }
EXPORT void glGetVertexAttribIiv(GLuint index, GLenum pname, GLint * params) { ((void (*)(GLuint, GLenum, GLint *))TBL()[IDX_glGetVertexAttribIiv])(index, pname, params); }
EXPORT void glGetVertexAttribIuiv(GLuint index, GLenum pname, GLuint * params) { ((void (*)(GLuint, GLenum, GLuint *))TBL()[IDX_glGetVertexAttribIuiv])(index, pname, params); }
EXPORT void glVertexAttribI4i(GLuint index, GLint x, GLint y, GLint z, GLint w) { ((void (*)(GLuint, GLint, GLint, GLint, GLint))TBL()[IDX_glVertexAttribI4i])(index, x, y, z, w); }
EXPORT void glVertexAttribI4ui(GLuint index, GLuint x, GLuint y, GLuint z, GLuint w) { ((void (*)(GLuint, GLuint, GLuint, GLuint, GLuint))TBL()[IDX_glVertexAttribI4ui])(index, x, y, z, w); }
EXPORT void glVertexAttribI4iv(GLuint index, const GLint * v) { ((void (*)(GLuint, const GLint *))TBL()[IDX_glVertexAttribI4iv])(index, v); }
EXPORT void glVertexAttribI4uiv(GLuint index, const GLuint * v) { ((void (*)(GLuint, const GLuint *))TBL()[IDX_glVertexAttribI4uiv])(index, v); }
EXPORT void glGetUniformuiv(GLuint program, GLint location, GLuint * params) { ((void (*)(GLuint, GLint, GLuint *))TBL()[IDX_glGetUniformuiv])(program, location, params); }
EXPORT GLint glGetFragDataLocation(GLuint program, const GLchar * name) { return ((GLint (*)(GLuint, const GLchar *))TBL()[IDX_glGetFragDataLocation])(program, name); }
EXPORT void glUniform1ui(GLint location, GLuint v0) { ((void (*)(GLint, GLuint))TBL()[IDX_glUniform1ui])(location, v0); }
EXPORT void glUniform2ui(GLint location, GLuint v0, GLuint v1) { ((void (*)(GLint, GLuint, GLuint))TBL()[IDX_glUniform2ui])(location, v0, v1); }
EXPORT void glUniform3ui(GLint location, GLuint v0, GLuint v1, GLuint v2) { ((void (*)(GLint, GLuint, GLuint, GLuint))TBL()[IDX_glUniform3ui])(location, v0, v1, v2); }
EXPORT void glUniform4ui(GLint location, GLuint v0, GLuint v1, GLuint v2, GLuint v3) { ((void (*)(GLint, GLuint, GLuint, GLuint, GLuint))TBL()[IDX_glUniform4ui])(location, v0, v1, v2, v3); }
EXPORT void glUniform1uiv(GLint location, GLsizei count, const GLuint * value) { ((void (*)(GLint, GLsizei, const GLuint *))TBL()[IDX_glUniform1uiv])(location, count, value); }
EXPORT void glUniform2uiv(GLint location, GLsizei count, const GLuint * value) { ((void (*)(GLint, GLsizei, const GLuint *))TBL()[IDX_glUniform2uiv])(location, count, value); }
EXPORT void glUniform3uiv(GLint location, GLsizei count, const GLuint * value) { ((void (*)(GLint, GLsizei, const GLuint *))TBL()[IDX_glUniform3uiv])(location, count, value); }
EXPORT void glUniform4uiv(GLint location, GLsizei count, const GLuint * value) { ((void (*)(GLint, GLsizei, const GLuint *))TBL()[IDX_glUniform4uiv])(location, count, value); }
EXPORT void glClearBufferiv(GLenum buffer, GLint drawbuffer, const GLint * value) { ((void (*)(GLenum, GLint, const GLint *))TBL()[IDX_glClearBufferiv])(buffer, drawbuffer, value); }
EXPORT void glClearBufferuiv(GLenum buffer, GLint drawbuffer, const GLuint * value) { ((void (*)(GLenum, GLint, const GLuint *))TBL()[IDX_glClearBufferuiv])(buffer, drawbuffer, value); }
EXPORT void glClearBufferfv(GLenum buffer, GLint drawbuffer, const GLfloat * value) { ((void (*)(GLenum, GLint, const GLfloat *))TBL()[IDX_glClearBufferfv])(buffer, drawbuffer, value); }
EXPORT void glClearBufferfi(GLenum buffer, GLint drawbuffer, GLfloat depth, GLint stencil) { ((void (*)(GLenum, GLint, GLfloat, GLint))TBL()[IDX_glClearBufferfi])(buffer, drawbuffer, depth, stencil); }
EXPORT const GLubyte * glGetStringi(GLenum name, GLuint index) { return ((const GLubyte * (*)(GLenum, GLuint))TBL()[IDX_glGetStringi])(name, index); }
EXPORT void glCopyBufferSubData(GLenum readTarget, GLenum writeTarget, GLintptr readOffset, GLintptr writeOffset, GLsizeiptr size) { ((void (*)(GLenum, GLenum, GLintptr, GLintptr, GLsizeiptr))TBL()[IDX_glCopyBufferSubData])(readTarget, writeTarget, readOffset, writeOffset, size); }
EXPORT void glGetUniformIndices(GLuint program, GLsizei uniformCount, const GLchar *const* uniformNames, GLuint * uniformIndices) { ((void (*)(GLuint, GLsizei, const GLchar *const*, GLuint *))TBL()[IDX_glGetUniformIndices])(program, uniformCount, uniformNames, uniformIndices); }
EXPORT void glGetActiveUniformsiv(GLuint program, GLsizei uniformCount, const GLuint * uniformIndices, GLenum pname, GLint * params) { ((void (*)(GLuint, GLsizei, const GLuint *, GLenum, GLint *))TBL()[IDX_glGetActiveUniformsiv])(program, uniformCount, uniformIndices, pname, params); }
EXPORT GLuint glGetUniformBlockIndex(GLuint program, const GLchar * uniformBlockName) { return ((GLuint (*)(GLuint, const GLchar *))TBL()[IDX_glGetUniformBlockIndex])(program, uniformBlockName); }
EXPORT void glGetActiveUniformBlockiv(GLuint program, GLuint uniformBlockIndex, GLenum pname, GLint * params) { ((void (*)(GLuint, GLuint, GLenum, GLint *))TBL()[IDX_glGetActiveUniformBlockiv])(program, uniformBlockIndex, pname, params); }
EXPORT void glGetActiveUniformBlockName(GLuint program, GLuint uniformBlockIndex, GLsizei bufSize, GLsizei * length, GLchar * uniformBlockName) { ((void (*)(GLuint, GLuint, GLsizei, GLsizei *, GLchar *))TBL()[IDX_glGetActiveUniformBlockName])(program, uniformBlockIndex, bufSize, length, uniformBlockName); }
EXPORT void glUniformBlockBinding(GLuint program, GLuint uniformBlockIndex, GLuint uniformBlockBinding) { ((void (*)(GLuint, GLuint, GLuint))TBL()[IDX_glUniformBlockBinding])(program, uniformBlockIndex, uniformBlockBinding); }
EXPORT void glDrawArraysInstanced(GLenum mode, GLint first, GLsizei count, GLsizei instancecount) { ((void (*)(GLenum, GLint, GLsizei, GLsizei))TBL()[IDX_glDrawArraysInstanced])(mode, first, count, instancecount); }
EXPORT void glDrawElementsInstanced(GLenum mode, GLsizei count, GLenum type, const void * indices, GLsizei instancecount) { ((void (*)(GLenum, GLsizei, GLenum, const void *, GLsizei))TBL()[IDX_glDrawElementsInstanced])(mode, count, type, indices, instancecount); }
EXPORT GLsync glFenceSync(GLenum condition, GLbitfield flags) { return ((GLsync (*)(GLenum, GLbitfield))TBL()[IDX_glFenceSync])(condition, flags); }
EXPORT GLboolean glIsSync(GLsync sync) { return ((GLboolean (*)(GLsync))TBL()[IDX_glIsSync])(sync); }
EXPORT void glDeleteSync(GLsync sync) { ((void (*)(GLsync))TBL()[IDX_glDeleteSync])(sync); }
EXPORT GLenum glClientWaitSync(GLsync sync, GLbitfield flags, GLuint64 timeout) { return ((GLenum (*)(GLsync, GLbitfield, GLuint64))TBL()[IDX_glClientWaitSync])(sync, flags, timeout); }
EXPORT void glWaitSync(GLsync sync, GLbitfield flags, GLuint64 timeout) { ((void (*)(GLsync, GLbitfield, GLuint64))TBL()[IDX_glWaitSync])(sync, flags, timeout); }
EXPORT void glGetInteger64v(GLenum pname, GLint64 * data) { ((void (*)(GLenum, GLint64 *))TBL()[IDX_glGetInteger64v])(pname, data); }
EXPORT void glGetSynciv(GLsync sync, GLenum pname, GLsizei bufSize, GLsizei * length, GLint * values) { ((void (*)(GLsync, GLenum, GLsizei, GLsizei *, GLint *))TBL()[IDX_glGetSynciv])(sync, pname, bufSize, length, values); }
EXPORT void glGetInteger64i_v(GLenum target, GLuint index, GLint64 * data) { ((void (*)(GLenum, GLuint, GLint64 *))TBL()[IDX_glGetInteger64i_v])(target, index, data); }
EXPORT void glGetBufferParameteri64v(GLenum target, GLenum pname, GLint64 * params) { ((void (*)(GLenum, GLenum, GLint64 *))TBL()[IDX_glGetBufferParameteri64v])(target, pname, params); }
EXPORT void glGenSamplers(GLsizei count, GLuint * samplers) { ((void (*)(GLsizei, GLuint *))TBL()[IDX_glGenSamplers])(count, samplers); }
EXPORT void glDeleteSamplers(GLsizei count, const GLuint * samplers) { ((void (*)(GLsizei, const GLuint *))TBL()[IDX_glDeleteSamplers])(count, samplers); }
EXPORT GLboolean glIsSampler(GLuint sampler) { return ((GLboolean (*)(GLuint))TBL()[IDX_glIsSampler])(sampler); }
EXPORT void glBindSampler(GLuint unit, GLuint sampler) { ((void (*)(GLuint, GLuint))TBL()[IDX_glBindSampler])(unit, sampler); }
EXPORT void glSamplerParameteri(GLuint sampler, GLenum pname, GLint param) { ((void (*)(GLuint, GLenum, GLint))TBL()[IDX_glSamplerParameteri])(sampler, pname, param); }
EXPORT void glSamplerParameteriv(GLuint sampler, GLenum pname, const GLint * param) { ((void (*)(GLuint, GLenum, const GLint *))TBL()[IDX_glSamplerParameteriv])(sampler, pname, param); }
EXPORT void glSamplerParameterf(GLuint sampler, GLenum pname, GLfloat param) { ((void (*)(GLuint, GLenum, GLfloat))TBL()[IDX_glSamplerParameterf])(sampler, pname, param); }
EXPORT void glSamplerParameterfv(GLuint sampler, GLenum pname, const GLfloat * param) { ((void (*)(GLuint, GLenum, const GLfloat *))TBL()[IDX_glSamplerParameterfv])(sampler, pname, param); }
EXPORT void glGetSamplerParameteriv(GLuint sampler, GLenum pname, GLint * params) { ((void (*)(GLuint, GLenum, GLint *))TBL()[IDX_glGetSamplerParameteriv])(sampler, pname, params); }
EXPORT void glGetSamplerParameterfv(GLuint sampler, GLenum pname, GLfloat * params) { ((void (*)(GLuint, GLenum, GLfloat *))TBL()[IDX_glGetSamplerParameterfv])(sampler, pname, params); }
EXPORT void glVertexAttribDivisor(GLuint index, GLuint divisor) { ((void (*)(GLuint, GLuint))TBL()[IDX_glVertexAttribDivisor])(index, divisor); }
EXPORT void glBindTransformFeedback(GLenum target, GLuint id) { ((void (*)(GLenum, GLuint))TBL()[IDX_glBindTransformFeedback])(target, id); }
EXPORT void glDeleteTransformFeedbacks(GLsizei n, const GLuint * ids) { ((void (*)(GLsizei, const GLuint *))TBL()[IDX_glDeleteTransformFeedbacks])(n, ids); }
EXPORT void glGenTransformFeedbacks(GLsizei n, GLuint * ids) { ((void (*)(GLsizei, GLuint *))TBL()[IDX_glGenTransformFeedbacks])(n, ids); }
EXPORT GLboolean glIsTransformFeedback(GLuint id) { return ((GLboolean (*)(GLuint))TBL()[IDX_glIsTransformFeedback])(id); }
EXPORT void glPauseTransformFeedback(void) { ((void (*)(void))TBL()[IDX_glPauseTransformFeedback])(); }
EXPORT void glResumeTransformFeedback(void) { ((void (*)(void))TBL()[IDX_glResumeTransformFeedback])(); }
EXPORT void glGetProgramBinary(GLuint program, GLsizei bufSize, GLsizei * length, GLenum * binaryFormat, void * binary) { ((void (*)(GLuint, GLsizei, GLsizei *, GLenum *, void *))TBL()[IDX_glGetProgramBinary])(program, bufSize, length, binaryFormat, binary); }
EXPORT void glProgramBinary(GLuint program, GLenum binaryFormat, const void * binary, GLsizei length) { ((void (*)(GLuint, GLenum, const void *, GLsizei))TBL()[IDX_glProgramBinary])(program, binaryFormat, binary, length); }
EXPORT void glProgramParameteri(GLuint program, GLenum pname, GLint value) { ((void (*)(GLuint, GLenum, GLint))TBL()[IDX_glProgramParameteri])(program, pname, value); }
EXPORT void glInvalidateFramebuffer(GLenum target, GLsizei numAttachments, const GLenum * attachments) { ((void (*)(GLenum, GLsizei, const GLenum *))TBL()[IDX_glInvalidateFramebuffer])(target, numAttachments, attachments); }
EXPORT void glInvalidateSubFramebuffer(GLenum target, GLsizei numAttachments, const GLenum * attachments, GLint x, GLint y, GLsizei width, GLsizei height) { ((void (*)(GLenum, GLsizei, const GLenum *, GLint, GLint, GLsizei, GLsizei))TBL()[IDX_glInvalidateSubFramebuffer])(target, numAttachments, attachments, x, y, width, height); }
EXPORT void glTexStorage2D(GLenum target, GLsizei levels, GLenum internalformat, GLsizei width, GLsizei height) { ((void (*)(GLenum, GLsizei, GLenum, GLsizei, GLsizei))TBL()[IDX_glTexStorage2D])(target, levels, internalformat, width, height); }
EXPORT void glTexStorage3D(GLenum target, GLsizei levels, GLenum internalformat, GLsizei width, GLsizei height, GLsizei depth) { ((void (*)(GLenum, GLsizei, GLenum, GLsizei, GLsizei, GLsizei))TBL()[IDX_glTexStorage3D])(target, levels, internalformat, width, height, depth); }
EXPORT void glGetInternalformativ(GLenum target, GLenum internalformat, GLenum pname, GLsizei bufSize, GLint * params) { ((void (*)(GLenum, GLenum, GLenum, GLsizei, GLint *))TBL()[IDX_glGetInternalformativ])(target, internalformat, pname, bufSize, params); }
EXPORT void glDispatchCompute(GLuint num_groups_x, GLuint num_groups_y, GLuint num_groups_z) { ((void (*)(GLuint, GLuint, GLuint))TBL()[IDX_glDispatchCompute])(num_groups_x, num_groups_y, num_groups_z); }
EXPORT void glDispatchComputeIndirect(GLintptr indirect) { ((void (*)(GLintptr))TBL()[IDX_glDispatchComputeIndirect])(indirect); }
EXPORT void glDrawArraysIndirect(GLenum mode, const void * indirect) { ((void (*)(GLenum, const void *))TBL()[IDX_glDrawArraysIndirect])(mode, indirect); }
EXPORT void glDrawElementsIndirect(GLenum mode, GLenum type, const void * indirect) { ((void (*)(GLenum, GLenum, const void *))TBL()[IDX_glDrawElementsIndirect])(mode, type, indirect); }
EXPORT void glFramebufferParameteri(GLenum target, GLenum pname, GLint param) { ((void (*)(GLenum, GLenum, GLint))TBL()[IDX_glFramebufferParameteri])(target, pname, param); }
EXPORT void glGetFramebufferParameteriv(GLenum target, GLenum pname, GLint * params) { ((void (*)(GLenum, GLenum, GLint *))TBL()[IDX_glGetFramebufferParameteriv])(target, pname, params); }
EXPORT void glGetProgramInterfaceiv(GLuint program, GLenum programInterface, GLenum pname, GLint * params) { ((void (*)(GLuint, GLenum, GLenum, GLint *))TBL()[IDX_glGetProgramInterfaceiv])(program, programInterface, pname, params); }
EXPORT GLuint glGetProgramResourceIndex(GLuint program, GLenum programInterface, const GLchar * name) { return ((GLuint (*)(GLuint, GLenum, const GLchar *))TBL()[IDX_glGetProgramResourceIndex])(program, programInterface, name); }
EXPORT void glGetProgramResourceName(GLuint program, GLenum programInterface, GLuint index, GLsizei bufSize, GLsizei * length, GLchar * name) { ((void (*)(GLuint, GLenum, GLuint, GLsizei, GLsizei *, GLchar *))TBL()[IDX_glGetProgramResourceName])(program, programInterface, index, bufSize, length, name); }
EXPORT void glGetProgramResourceiv(GLuint program, GLenum programInterface, GLuint index, GLsizei propCount, const GLenum * props, GLsizei bufSize, GLsizei * length, GLint * params) { ((void (*)(GLuint, GLenum, GLuint, GLsizei, const GLenum *, GLsizei, GLsizei *, GLint *))TBL()[IDX_glGetProgramResourceiv])(program, programInterface, index, propCount, props, bufSize, length, params); }
EXPORT GLint glGetProgramResourceLocation(GLuint program, GLenum programInterface, const GLchar * name) { return ((GLint (*)(GLuint, GLenum, const GLchar *))TBL()[IDX_glGetProgramResourceLocation])(program, programInterface, name); }
EXPORT void glUseProgramStages(GLuint pipeline, GLbitfield stages, GLuint program) { ((void (*)(GLuint, GLbitfield, GLuint))TBL()[IDX_glUseProgramStages])(pipeline, stages, program); }
EXPORT void glActiveShaderProgram(GLuint pipeline, GLuint program) { ((void (*)(GLuint, GLuint))TBL()[IDX_glActiveShaderProgram])(pipeline, program); }
EXPORT GLuint glCreateShaderProgramv(GLenum type, GLsizei count, const GLchar *const* strings) { return ((GLuint (*)(GLenum, GLsizei, const GLchar *const*))TBL()[IDX_glCreateShaderProgramv])(type, count, strings); }
EXPORT void glBindProgramPipeline(GLuint pipeline) { ((void (*)(GLuint))TBL()[IDX_glBindProgramPipeline])(pipeline); }
EXPORT void glDeleteProgramPipelines(GLsizei n, const GLuint * pipelines) { ((void (*)(GLsizei, const GLuint *))TBL()[IDX_glDeleteProgramPipelines])(n, pipelines); }
EXPORT void glGenProgramPipelines(GLsizei n, GLuint * pipelines) { ((void (*)(GLsizei, GLuint *))TBL()[IDX_glGenProgramPipelines])(n, pipelines); }
EXPORT GLboolean glIsProgramPipeline(GLuint pipeline) { return ((GLboolean (*)(GLuint))TBL()[IDX_glIsProgramPipeline])(pipeline); }
EXPORT void glGetProgramPipelineiv(GLuint pipeline, GLenum pname, GLint * params) { ((void (*)(GLuint, GLenum, GLint *))TBL()[IDX_glGetProgramPipelineiv])(pipeline, pname, params); }
EXPORT void glProgramUniform1i(GLuint program, GLint location, GLint v0) { ((void (*)(GLuint, GLint, GLint))TBL()[IDX_glProgramUniform1i])(program, location, v0); }
EXPORT void glProgramUniform2i(GLuint program, GLint location, GLint v0, GLint v1) { ((void (*)(GLuint, GLint, GLint, GLint))TBL()[IDX_glProgramUniform2i])(program, location, v0, v1); }
EXPORT void glProgramUniform3i(GLuint program, GLint location, GLint v0, GLint v1, GLint v2) { ((void (*)(GLuint, GLint, GLint, GLint, GLint))TBL()[IDX_glProgramUniform3i])(program, location, v0, v1, v2); }
EXPORT void glProgramUniform4i(GLuint program, GLint location, GLint v0, GLint v1, GLint v2, GLint v3) { ((void (*)(GLuint, GLint, GLint, GLint, GLint, GLint))TBL()[IDX_glProgramUniform4i])(program, location, v0, v1, v2, v3); }
EXPORT void glProgramUniform1ui(GLuint program, GLint location, GLuint v0) { ((void (*)(GLuint, GLint, GLuint))TBL()[IDX_glProgramUniform1ui])(program, location, v0); }
EXPORT void glProgramUniform2ui(GLuint program, GLint location, GLuint v0, GLuint v1) { ((void (*)(GLuint, GLint, GLuint, GLuint))TBL()[IDX_glProgramUniform2ui])(program, location, v0, v1); }
EXPORT void glProgramUniform3ui(GLuint program, GLint location, GLuint v0, GLuint v1, GLuint v2) { ((void (*)(GLuint, GLint, GLuint, GLuint, GLuint))TBL()[IDX_glProgramUniform3ui])(program, location, v0, v1, v2); }
EXPORT void glProgramUniform4ui(GLuint program, GLint location, GLuint v0, GLuint v1, GLuint v2, GLuint v3) { ((void (*)(GLuint, GLint, GLuint, GLuint, GLuint, GLuint))TBL()[IDX_glProgramUniform4ui])(program, location, v0, v1, v2, v3); }
EXPORT void glProgramUniform1f(GLuint program, GLint location, GLfloat v0) { ((void (*)(GLuint, GLint, GLfloat))TBL()[IDX_glProgramUniform1f])(program, location, v0); }
EXPORT void glProgramUniform2f(GLuint program, GLint location, GLfloat v0, GLfloat v1) { ((void (*)(GLuint, GLint, GLfloat, GLfloat))TBL()[IDX_glProgramUniform2f])(program, location, v0, v1); }
EXPORT void glProgramUniform3f(GLuint program, GLint location, GLfloat v0, GLfloat v1, GLfloat v2) { ((void (*)(GLuint, GLint, GLfloat, GLfloat, GLfloat))TBL()[IDX_glProgramUniform3f])(program, location, v0, v1, v2); }
EXPORT void glProgramUniform4f(GLuint program, GLint location, GLfloat v0, GLfloat v1, GLfloat v2, GLfloat v3) { ((void (*)(GLuint, GLint, GLfloat, GLfloat, GLfloat, GLfloat))TBL()[IDX_glProgramUniform4f])(program, location, v0, v1, v2, v3); }
EXPORT void glProgramUniform1iv(GLuint program, GLint location, GLsizei count, const GLint * value) { ((void (*)(GLuint, GLint, GLsizei, const GLint *))TBL()[IDX_glProgramUniform1iv])(program, location, count, value); }
EXPORT void glProgramUniform2iv(GLuint program, GLint location, GLsizei count, const GLint * value) { ((void (*)(GLuint, GLint, GLsizei, const GLint *))TBL()[IDX_glProgramUniform2iv])(program, location, count, value); }
EXPORT void glProgramUniform3iv(GLuint program, GLint location, GLsizei count, const GLint * value) { ((void (*)(GLuint, GLint, GLsizei, const GLint *))TBL()[IDX_glProgramUniform3iv])(program, location, count, value); }
EXPORT void glProgramUniform4iv(GLuint program, GLint location, GLsizei count, const GLint * value) { ((void (*)(GLuint, GLint, GLsizei, const GLint *))TBL()[IDX_glProgramUniform4iv])(program, location, count, value); }
EXPORT void glProgramUniform1uiv(GLuint program, GLint location, GLsizei count, const GLuint * value) { ((void (*)(GLuint, GLint, GLsizei, const GLuint *))TBL()[IDX_glProgramUniform1uiv])(program, location, count, value); }
EXPORT void glProgramUniform2uiv(GLuint program, GLint location, GLsizei count, const GLuint * value) { ((void (*)(GLuint, GLint, GLsizei, const GLuint *))TBL()[IDX_glProgramUniform2uiv])(program, location, count, value); }
EXPORT void glProgramUniform3uiv(GLuint program, GLint location, GLsizei count, const GLuint * value) { ((void (*)(GLuint, GLint, GLsizei, const GLuint *))TBL()[IDX_glProgramUniform3uiv])(program, location, count, value); }
EXPORT void glProgramUniform4uiv(GLuint program, GLint location, GLsizei count, const GLuint * value) { ((void (*)(GLuint, GLint, GLsizei, const GLuint *))TBL()[IDX_glProgramUniform4uiv])(program, location, count, value); }
EXPORT void glProgramUniform1fv(GLuint program, GLint location, GLsizei count, const GLfloat * value) { ((void (*)(GLuint, GLint, GLsizei, const GLfloat *))TBL()[IDX_glProgramUniform1fv])(program, location, count, value); }
EXPORT void glProgramUniform2fv(GLuint program, GLint location, GLsizei count, const GLfloat * value) { ((void (*)(GLuint, GLint, GLsizei, const GLfloat *))TBL()[IDX_glProgramUniform2fv])(program, location, count, value); }
EXPORT void glProgramUniform3fv(GLuint program, GLint location, GLsizei count, const GLfloat * value) { ((void (*)(GLuint, GLint, GLsizei, const GLfloat *))TBL()[IDX_glProgramUniform3fv])(program, location, count, value); }
EXPORT void glProgramUniform4fv(GLuint program, GLint location, GLsizei count, const GLfloat * value) { ((void (*)(GLuint, GLint, GLsizei, const GLfloat *))TBL()[IDX_glProgramUniform4fv])(program, location, count, value); }
EXPORT void glProgramUniformMatrix2fv(GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { ((void (*)(GLuint, GLint, GLsizei, GLboolean, const GLfloat *))TBL()[IDX_glProgramUniformMatrix2fv])(program, location, count, transpose, value); }
EXPORT void glProgramUniformMatrix3fv(GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { ((void (*)(GLuint, GLint, GLsizei, GLboolean, const GLfloat *))TBL()[IDX_glProgramUniformMatrix3fv])(program, location, count, transpose, value); }
EXPORT void glProgramUniformMatrix4fv(GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { ((void (*)(GLuint, GLint, GLsizei, GLboolean, const GLfloat *))TBL()[IDX_glProgramUniformMatrix4fv])(program, location, count, transpose, value); }
EXPORT void glProgramUniformMatrix2x3fv(GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { ((void (*)(GLuint, GLint, GLsizei, GLboolean, const GLfloat *))TBL()[IDX_glProgramUniformMatrix2x3fv])(program, location, count, transpose, value); }
EXPORT void glProgramUniformMatrix3x2fv(GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { ((void (*)(GLuint, GLint, GLsizei, GLboolean, const GLfloat *))TBL()[IDX_glProgramUniformMatrix3x2fv])(program, location, count, transpose, value); }
EXPORT void glProgramUniformMatrix2x4fv(GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { ((void (*)(GLuint, GLint, GLsizei, GLboolean, const GLfloat *))TBL()[IDX_glProgramUniformMatrix2x4fv])(program, location, count, transpose, value); }
EXPORT void glProgramUniformMatrix4x2fv(GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { ((void (*)(GLuint, GLint, GLsizei, GLboolean, const GLfloat *))TBL()[IDX_glProgramUniformMatrix4x2fv])(program, location, count, transpose, value); }
EXPORT void glProgramUniformMatrix3x4fv(GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { ((void (*)(GLuint, GLint, GLsizei, GLboolean, const GLfloat *))TBL()[IDX_glProgramUniformMatrix3x4fv])(program, location, count, transpose, value); }
EXPORT void glProgramUniformMatrix4x3fv(GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { ((void (*)(GLuint, GLint, GLsizei, GLboolean, const GLfloat *))TBL()[IDX_glProgramUniformMatrix4x3fv])(program, location, count, transpose, value); }
EXPORT void glValidateProgramPipeline(GLuint pipeline) { ((void (*)(GLuint))TBL()[IDX_glValidateProgramPipeline])(pipeline); }
EXPORT void glGetProgramPipelineInfoLog(GLuint pipeline, GLsizei bufSize, GLsizei * length, GLchar * infoLog) { ((void (*)(GLuint, GLsizei, GLsizei *, GLchar *))TBL()[IDX_glGetProgramPipelineInfoLog])(pipeline, bufSize, length, infoLog); }
EXPORT void glBindImageTexture(GLuint unit, GLuint texture, GLint level, GLboolean layered, GLint layer, GLenum access, GLenum format) { ((void (*)(GLuint, GLuint, GLint, GLboolean, GLint, GLenum, GLenum))TBL()[IDX_glBindImageTexture])(unit, texture, level, layered, layer, access, format); }
EXPORT void glGetBooleani_v(GLenum target, GLuint index, GLboolean * data) { ((void (*)(GLenum, GLuint, GLboolean *))TBL()[IDX_glGetBooleani_v])(target, index, data); }
EXPORT void glMemoryBarrier(GLbitfield barriers) { ((void (*)(GLbitfield))TBL()[IDX_glMemoryBarrier])(barriers); }
EXPORT void glMemoryBarrierByRegion(GLbitfield barriers) { ((void (*)(GLbitfield))TBL()[IDX_glMemoryBarrierByRegion])(barriers); }
EXPORT void glTexStorage2DMultisample(GLenum target, GLsizei samples, GLenum internalformat, GLsizei width, GLsizei height, GLboolean fixedsamplelocations) { ((void (*)(GLenum, GLsizei, GLenum, GLsizei, GLsizei, GLboolean))TBL()[IDX_glTexStorage2DMultisample])(target, samples, internalformat, width, height, fixedsamplelocations); }
EXPORT void glGetMultisamplefv(GLenum pname, GLuint index, GLfloat * val) { ((void (*)(GLenum, GLuint, GLfloat *))TBL()[IDX_glGetMultisamplefv])(pname, index, val); }
EXPORT void glSampleMaski(GLuint maskNumber, GLbitfield mask) { ((void (*)(GLuint, GLbitfield))TBL()[IDX_glSampleMaski])(maskNumber, mask); }
EXPORT void glGetTexLevelParameteriv(GLenum target, GLint level, GLenum pname, GLint * params) { ((void (*)(GLenum, GLint, GLenum, GLint *))TBL()[IDX_glGetTexLevelParameteriv])(target, level, pname, params); }
EXPORT void glGetTexLevelParameterfv(GLenum target, GLint level, GLenum pname, GLfloat * params) { ((void (*)(GLenum, GLint, GLenum, GLfloat *))TBL()[IDX_glGetTexLevelParameterfv])(target, level, pname, params); }
EXPORT void glBindVertexBuffer(GLuint bindingindex, GLuint buffer, GLintptr offset, GLsizei stride) { ((void (*)(GLuint, GLuint, GLintptr, GLsizei))TBL()[IDX_glBindVertexBuffer])(bindingindex, buffer, offset, stride); }
EXPORT void glVertexAttribFormat(GLuint attribindex, GLint size, GLenum type, GLboolean normalized, GLuint relativeoffset) { ((void (*)(GLuint, GLint, GLenum, GLboolean, GLuint))TBL()[IDX_glVertexAttribFormat])(attribindex, size, type, normalized, relativeoffset); }
EXPORT void glVertexAttribIFormat(GLuint attribindex, GLint size, GLenum type, GLuint relativeoffset) { ((void (*)(GLuint, GLint, GLenum, GLuint))TBL()[IDX_glVertexAttribIFormat])(attribindex, size, type, relativeoffset); }
EXPORT void glVertexAttribBinding(GLuint attribindex, GLuint bindingindex) { ((void (*)(GLuint, GLuint))TBL()[IDX_glVertexAttribBinding])(attribindex, bindingindex); }
EXPORT void glVertexBindingDivisor(GLuint bindingindex, GLuint divisor) { ((void (*)(GLuint, GLuint))TBL()[IDX_glVertexBindingDivisor])(bindingindex, divisor); }
EXPORT void glBlendBarrierKHR(void) { ((void (*)(void))TBL()[IDX_glBlendBarrierKHR])(); }
EXPORT void glDebugMessageControlKHR(GLenum source, GLenum type, GLenum severity, GLsizei count, const GLuint * ids, GLboolean enabled) { ((void (*)(GLenum, GLenum, GLenum, GLsizei, const GLuint *, GLboolean))TBL()[IDX_glDebugMessageControlKHR])(source, type, severity, count, ids, enabled); }
EXPORT void glDebugMessageInsertKHR(GLenum source, GLenum type, GLuint id, GLenum severity, GLsizei length, const GLchar * buf) { ((void (*)(GLenum, GLenum, GLuint, GLenum, GLsizei, const GLchar *))TBL()[IDX_glDebugMessageInsertKHR])(source, type, id, severity, length, buf); }
EXPORT void glDebugMessageCallbackKHR(GLDEBUGPROCKHR callback, const void * userParam) { ((void (*)(GLDEBUGPROCKHR, const void *))TBL()[IDX_glDebugMessageCallbackKHR])(callback, userParam); }
EXPORT GLuint glGetDebugMessageLogKHR(GLuint count, GLsizei bufSize, GLenum * sources, GLenum * types, GLuint * ids, GLenum * severities, GLsizei * lengths, GLchar * messageLog) { return ((GLuint (*)(GLuint, GLsizei, GLenum *, GLenum *, GLuint *, GLenum *, GLsizei *, GLchar *))TBL()[IDX_glGetDebugMessageLogKHR])(count, bufSize, sources, types, ids, severities, lengths, messageLog); }
EXPORT void glPushDebugGroupKHR(GLenum source, GLuint id, GLsizei length, const GLchar * message) { ((void (*)(GLenum, GLuint, GLsizei, const GLchar *))TBL()[IDX_glPushDebugGroupKHR])(source, id, length, message); }
EXPORT void glPopDebugGroupKHR(void) { ((void (*)(void))TBL()[IDX_glPopDebugGroupKHR])(); }
EXPORT void glObjectLabelKHR(GLenum identifier, GLuint name, GLsizei length, const GLchar * label) { ((void (*)(GLenum, GLuint, GLsizei, const GLchar *))TBL()[IDX_glObjectLabelKHR])(identifier, name, length, label); }
EXPORT void glGetObjectLabelKHR(GLenum identifier, GLuint name, GLsizei bufSize, GLsizei * length, GLchar * label) { ((void (*)(GLenum, GLuint, GLsizei, GLsizei *, GLchar *))TBL()[IDX_glGetObjectLabelKHR])(identifier, name, bufSize, length, label); }
EXPORT void glObjectPtrLabelKHR(const void * ptr, GLsizei length, const GLchar * label) { ((void (*)(const void *, GLsizei, const GLchar *))TBL()[IDX_glObjectPtrLabelKHR])(ptr, length, label); }
EXPORT void glGetObjectPtrLabelKHR(const void * ptr, GLsizei bufSize, GLsizei * length, GLchar * label) { ((void (*)(const void *, GLsizei, GLsizei *, GLchar *))TBL()[IDX_glGetObjectPtrLabelKHR])(ptr, bufSize, length, label); }
EXPORT void glGetPointervKHR(GLenum pname, void ** params) { ((void (*)(GLenum, void **))TBL()[IDX_glGetPointervKHR])(pname, params); }
EXPORT void glMaxShaderCompilerThreadsKHR(GLuint count) { ((void (*)(GLuint))TBL()[IDX_glMaxShaderCompilerThreadsKHR])(count); }
EXPORT GLenum glGetGraphicsResetStatusKHR(void) { return ((GLenum (*)(void))TBL()[IDX_glGetGraphicsResetStatusKHR])(); }
EXPORT void glReadnPixelsKHR(GLint x, GLint y, GLsizei width, GLsizei height, GLenum format, GLenum type, GLsizei bufSize, void * data) { ((void (*)(GLint, GLint, GLsizei, GLsizei, GLenum, GLenum, GLsizei, void *))TBL()[IDX_glReadnPixelsKHR])(x, y, width, height, format, type, bufSize, data); }
EXPORT void glGetnUniformfvKHR(GLuint program, GLint location, GLsizei bufSize, GLfloat * params) { ((void (*)(GLuint, GLint, GLsizei, GLfloat *))TBL()[IDX_glGetnUniformfvKHR])(program, location, bufSize, params); }
EXPORT void glGetnUniformivKHR(GLuint program, GLint location, GLsizei bufSize, GLint * params) { ((void (*)(GLuint, GLint, GLsizei, GLint *))TBL()[IDX_glGetnUniformivKHR])(program, location, bufSize, params); }
EXPORT void glGetnUniformuivKHR(GLuint program, GLint location, GLsizei bufSize, GLuint * params) { ((void (*)(GLuint, GLint, GLsizei, GLuint *))TBL()[IDX_glGetnUniformuivKHR])(program, location, bufSize, params); }
EXPORT void glEGLImageTargetTexture2DOES(GLenum target, GLeglImageOES image) { ((void (*)(GLenum, GLeglImageOES))TBL()[IDX_glEGLImageTargetTexture2DOES])(target, image); }
EXPORT void glEGLImageTargetRenderbufferStorageOES(GLenum target, GLeglImageOES image) { ((void (*)(GLenum, GLeglImageOES))TBL()[IDX_glEGLImageTargetRenderbufferStorageOES])(target, image); }
EXPORT void glCopyImageSubDataOES(GLuint srcName, GLenum srcTarget, GLint srcLevel, GLint srcX, GLint srcY, GLint srcZ, GLuint dstName, GLenum dstTarget, GLint dstLevel, GLint dstX, GLint dstY, GLint dstZ, GLsizei srcWidth, GLsizei srcHeight, GLsizei srcDepth) { ((void (*)(GLuint, GLenum, GLint, GLint, GLint, GLint, GLuint, GLenum, GLint, GLint, GLint, GLint, GLsizei, GLsizei, GLsizei))TBL()[IDX_glCopyImageSubDataOES])(srcName, srcTarget, srcLevel, srcX, srcY, srcZ, dstName, dstTarget, dstLevel, dstX, dstY, dstZ, srcWidth, srcHeight, srcDepth); }
EXPORT void glEnableiOES(GLenum target, GLuint index) { ((void (*)(GLenum, GLuint))TBL()[IDX_glEnableiOES])(target, index); }
EXPORT void glDisableiOES(GLenum target, GLuint index) { ((void (*)(GLenum, GLuint))TBL()[IDX_glDisableiOES])(target, index); }
EXPORT void glBlendEquationiOES(GLuint buf, GLenum mode) { ((void (*)(GLuint, GLenum))TBL()[IDX_glBlendEquationiOES])(buf, mode); }
EXPORT void glBlendEquationSeparateiOES(GLuint buf, GLenum modeRGB, GLenum modeAlpha) { ((void (*)(GLuint, GLenum, GLenum))TBL()[IDX_glBlendEquationSeparateiOES])(buf, modeRGB, modeAlpha); }
EXPORT void glBlendFunciOES(GLuint buf, GLenum src, GLenum dst) { ((void (*)(GLuint, GLenum, GLenum))TBL()[IDX_glBlendFunciOES])(buf, src, dst); }
EXPORT void glBlendFuncSeparateiOES(GLuint buf, GLenum srcRGB, GLenum dstRGB, GLenum srcAlpha, GLenum dstAlpha) { ((void (*)(GLuint, GLenum, GLenum, GLenum, GLenum))TBL()[IDX_glBlendFuncSeparateiOES])(buf, srcRGB, dstRGB, srcAlpha, dstAlpha); }
EXPORT void glColorMaskiOES(GLuint index, GLboolean r, GLboolean g, GLboolean b, GLboolean a) { ((void (*)(GLuint, GLboolean, GLboolean, GLboolean, GLboolean))TBL()[IDX_glColorMaskiOES])(index, r, g, b, a); }
EXPORT GLboolean glIsEnablediOES(GLenum target, GLuint index) { return ((GLboolean (*)(GLenum, GLuint))TBL()[IDX_glIsEnablediOES])(target, index); }
EXPORT void glDrawElementsBaseVertexOES(GLenum mode, GLsizei count, GLenum type, const void * indices, GLint basevertex) { ((void (*)(GLenum, GLsizei, GLenum, const void *, GLint))TBL()[IDX_glDrawElementsBaseVertexOES])(mode, count, type, indices, basevertex); }
EXPORT void glDrawRangeElementsBaseVertexOES(GLenum mode, GLuint start, GLuint end, GLsizei count, GLenum type, const void * indices, GLint basevertex) { ((void (*)(GLenum, GLuint, GLuint, GLsizei, GLenum, const void *, GLint))TBL()[IDX_glDrawRangeElementsBaseVertexOES])(mode, start, end, count, type, indices, basevertex); }
EXPORT void glDrawElementsInstancedBaseVertexOES(GLenum mode, GLsizei count, GLenum type, const void * indices, GLsizei instancecount, GLint basevertex) { ((void (*)(GLenum, GLsizei, GLenum, const void *, GLsizei, GLint))TBL()[IDX_glDrawElementsInstancedBaseVertexOES])(mode, count, type, indices, instancecount, basevertex); }
EXPORT void glMultiDrawElementsBaseVertexEXT(GLenum mode, const GLsizei * count, GLenum type, const void *const* indices, GLsizei primcount, const GLint * basevertex) { ((void (*)(GLenum, const GLsizei *, GLenum, const void *const*, GLsizei, const GLint *))TBL()[IDX_glMultiDrawElementsBaseVertexEXT])(mode, count, type, indices, primcount, basevertex); }
EXPORT void glFramebufferTextureOES(GLenum target, GLenum attachment, GLuint texture, GLint level) { ((void (*)(GLenum, GLenum, GLuint, GLint))TBL()[IDX_glFramebufferTextureOES])(target, attachment, texture, level); }
EXPORT void glGetProgramBinaryOES(GLuint program, GLsizei bufSize, GLsizei * length, GLenum * binaryFormat, void * binary) { ((void (*)(GLuint, GLsizei, GLsizei *, GLenum *, void *))TBL()[IDX_glGetProgramBinaryOES])(program, bufSize, length, binaryFormat, binary); }
EXPORT void glProgramBinaryOES(GLuint program, GLenum binaryFormat, const void * binary, GLint length) { ((void (*)(GLuint, GLenum, const void *, GLint))TBL()[IDX_glProgramBinaryOES])(program, binaryFormat, binary, length); }
EXPORT void * glMapBufferOES(GLenum target, GLenum access) { return ((void * (*)(GLenum, GLenum))TBL()[IDX_glMapBufferOES])(target, access); }
EXPORT GLboolean glUnmapBufferOES(GLenum target) { return ((GLboolean (*)(GLenum))TBL()[IDX_glUnmapBufferOES])(target); }
EXPORT void glGetBufferPointervOES(GLenum target, GLenum pname, void ** params) { ((void (*)(GLenum, GLenum, void **))TBL()[IDX_glGetBufferPointervOES])(target, pname, params); }
EXPORT void glPrimitiveBoundingBoxOES(GLfloat minX, GLfloat minY, GLfloat minZ, GLfloat minW, GLfloat maxX, GLfloat maxY, GLfloat maxZ, GLfloat maxW) { ((void (*)(GLfloat, GLfloat, GLfloat, GLfloat, GLfloat, GLfloat, GLfloat, GLfloat))TBL()[IDX_glPrimitiveBoundingBoxOES])(minX, minY, minZ, minW, maxX, maxY, maxZ, maxW); }
EXPORT void glMinSampleShadingOES(GLfloat value) { ((void (*)(GLfloat))TBL()[IDX_glMinSampleShadingOES])(value); }
EXPORT void glPatchParameteriOES(GLenum pname, GLint value) { ((void (*)(GLenum, GLint))TBL()[IDX_glPatchParameteriOES])(pname, value); }
EXPORT void glTexImage3DOES(GLenum target, GLint level, GLenum internalformat, GLsizei width, GLsizei height, GLsizei depth, GLint border, GLenum format, GLenum type, const void * pixels) { ((void (*)(GLenum, GLint, GLenum, GLsizei, GLsizei, GLsizei, GLint, GLenum, GLenum, const void *))TBL()[IDX_glTexImage3DOES])(target, level, internalformat, width, height, depth, border, format, type, pixels); }
EXPORT void glTexSubImage3DOES(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint zoffset, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, const void * pixels) { ((void (*)(GLenum, GLint, GLint, GLint, GLint, GLsizei, GLsizei, GLsizei, GLenum, GLenum, const void *))TBL()[IDX_glTexSubImage3DOES])(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, pixels); }
EXPORT void glCopyTexSubImage3DOES(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint zoffset, GLint x, GLint y, GLsizei width, GLsizei height) { ((void (*)(GLenum, GLint, GLint, GLint, GLint, GLint, GLint, GLsizei, GLsizei))TBL()[IDX_glCopyTexSubImage3DOES])(target, level, xoffset, yoffset, zoffset, x, y, width, height); }
EXPORT void glCompressedTexImage3DOES(GLenum target, GLint level, GLenum internalformat, GLsizei width, GLsizei height, GLsizei depth, GLint border, GLsizei imageSize, const void * data) { ((void (*)(GLenum, GLint, GLenum, GLsizei, GLsizei, GLsizei, GLint, GLsizei, const void *))TBL()[IDX_glCompressedTexImage3DOES])(target, level, internalformat, width, height, depth, border, imageSize, data); }
EXPORT void glCompressedTexSubImage3DOES(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint zoffset, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLsizei imageSize, const void * data) { ((void (*)(GLenum, GLint, GLint, GLint, GLint, GLsizei, GLsizei, GLsizei, GLenum, GLsizei, const void *))TBL()[IDX_glCompressedTexSubImage3DOES])(target, level, xoffset, yoffset, zoffset, width, height, depth, format, imageSize, data); }
EXPORT void glFramebufferTexture3DOES(GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level, GLint zoffset) { ((void (*)(GLenum, GLenum, GLenum, GLuint, GLint, GLint))TBL()[IDX_glFramebufferTexture3DOES])(target, attachment, textarget, texture, level, zoffset); }
EXPORT void glTexParameterIivOES(GLenum target, GLenum pname, const GLint * params) { ((void (*)(GLenum, GLenum, const GLint *))TBL()[IDX_glTexParameterIivOES])(target, pname, params); }
EXPORT void glTexParameterIuivOES(GLenum target, GLenum pname, const GLuint * params) { ((void (*)(GLenum, GLenum, const GLuint *))TBL()[IDX_glTexParameterIuivOES])(target, pname, params); }
EXPORT void glGetTexParameterIivOES(GLenum target, GLenum pname, GLint * params) { ((void (*)(GLenum, GLenum, GLint *))TBL()[IDX_glGetTexParameterIivOES])(target, pname, params); }
EXPORT void glGetTexParameterIuivOES(GLenum target, GLenum pname, GLuint * params) { ((void (*)(GLenum, GLenum, GLuint *))TBL()[IDX_glGetTexParameterIuivOES])(target, pname, params); }
EXPORT void glSamplerParameterIivOES(GLuint sampler, GLenum pname, const GLint * param) { ((void (*)(GLuint, GLenum, const GLint *))TBL()[IDX_glSamplerParameterIivOES])(sampler, pname, param); }
EXPORT void glSamplerParameterIuivOES(GLuint sampler, GLenum pname, const GLuint * param) { ((void (*)(GLuint, GLenum, const GLuint *))TBL()[IDX_glSamplerParameterIuivOES])(sampler, pname, param); }
EXPORT void glGetSamplerParameterIivOES(GLuint sampler, GLenum pname, GLint * params) { ((void (*)(GLuint, GLenum, GLint *))TBL()[IDX_glGetSamplerParameterIivOES])(sampler, pname, params); }
EXPORT void glGetSamplerParameterIuivOES(GLuint sampler, GLenum pname, GLuint * params) { ((void (*)(GLuint, GLenum, GLuint *))TBL()[IDX_glGetSamplerParameterIuivOES])(sampler, pname, params); }
EXPORT void glTexBufferOES(GLenum target, GLenum internalformat, GLuint buffer) { ((void (*)(GLenum, GLenum, GLuint))TBL()[IDX_glTexBufferOES])(target, internalformat, buffer); }
EXPORT void glTexBufferRangeOES(GLenum target, GLenum internalformat, GLuint buffer, GLintptr offset, GLsizeiptr size) { ((void (*)(GLenum, GLenum, GLuint, GLintptr, GLsizeiptr))TBL()[IDX_glTexBufferRangeOES])(target, internalformat, buffer, offset, size); }
EXPORT void glTexStorage3DMultisampleOES(GLenum target, GLsizei samples, GLenum internalformat, GLsizei width, GLsizei height, GLsizei depth, GLboolean fixedsamplelocations) { ((void (*)(GLenum, GLsizei, GLenum, GLsizei, GLsizei, GLsizei, GLboolean))TBL()[IDX_glTexStorage3DMultisampleOES])(target, samples, internalformat, width, height, depth, fixedsamplelocations); }
EXPORT void glTextureViewOES(GLuint texture, GLenum target, GLuint origtexture, GLenum internalformat, GLuint minlevel, GLuint numlevels, GLuint minlayer, GLuint numlayers) { ((void (*)(GLuint, GLenum, GLuint, GLenum, GLuint, GLuint, GLuint, GLuint))TBL()[IDX_glTextureViewOES])(texture, target, origtexture, internalformat, minlevel, numlevels, minlayer, numlayers); }
EXPORT void glBindVertexArrayOES(GLuint array) { ((void (*)(GLuint))TBL()[IDX_glBindVertexArrayOES])(array); }
EXPORT void glDeleteVertexArraysOES(GLsizei n, const GLuint * arrays) { ((void (*)(GLsizei, const GLuint *))TBL()[IDX_glDeleteVertexArraysOES])(n, arrays); }
EXPORT void glGenVertexArraysOES(GLsizei n, GLuint * arrays) { ((void (*)(GLsizei, GLuint *))TBL()[IDX_glGenVertexArraysOES])(n, arrays); }
EXPORT GLboolean glIsVertexArrayOES(GLuint array) { return ((GLboolean (*)(GLuint))TBL()[IDX_glIsVertexArrayOES])(array); }
EXPORT void glViewportArrayvOES(GLuint first, GLsizei count, const GLfloat * v) { ((void (*)(GLuint, GLsizei, const GLfloat *))TBL()[IDX_glViewportArrayvOES])(first, count, v); }
EXPORT void glViewportIndexedfOES(GLuint index, GLfloat x, GLfloat y, GLfloat w, GLfloat h) { ((void (*)(GLuint, GLfloat, GLfloat, GLfloat, GLfloat))TBL()[IDX_glViewportIndexedfOES])(index, x, y, w, h); }
EXPORT void glViewportIndexedfvOES(GLuint index, const GLfloat * v) { ((void (*)(GLuint, const GLfloat *))TBL()[IDX_glViewportIndexedfvOES])(index, v); }
EXPORT void glScissorArrayvOES(GLuint first, GLsizei count, const GLint * v) { ((void (*)(GLuint, GLsizei, const GLint *))TBL()[IDX_glScissorArrayvOES])(first, count, v); }
EXPORT void glScissorIndexedOES(GLuint index, GLint left, GLint bottom, GLsizei width, GLsizei height) { ((void (*)(GLuint, GLint, GLint, GLsizei, GLsizei))TBL()[IDX_glScissorIndexedOES])(index, left, bottom, width, height); }
EXPORT void glScissorIndexedvOES(GLuint index, const GLint * v) { ((void (*)(GLuint, const GLint *))TBL()[IDX_glScissorIndexedvOES])(index, v); }
EXPORT void glDepthRangeArrayfvOES(GLuint first, GLsizei count, const GLfloat * v) { ((void (*)(GLuint, GLsizei, const GLfloat *))TBL()[IDX_glDepthRangeArrayfvOES])(first, count, v); }
EXPORT void glDepthRangeIndexedfOES(GLuint index, GLfloat n, GLfloat f) { ((void (*)(GLuint, GLfloat, GLfloat))TBL()[IDX_glDepthRangeIndexedfOES])(index, n, f); }
EXPORT void glGetFloati_vOES(GLenum target, GLuint index, GLfloat * data) { ((void (*)(GLenum, GLuint, GLfloat *))TBL()[IDX_glGetFloati_vOES])(target, index, data); }
EXPORT void glGetPerfMonitorGroupsAMD(GLint * numGroups, GLsizei groupsSize, GLuint * groups) { ((void (*)(GLint *, GLsizei, GLuint *))TBL()[IDX_glGetPerfMonitorGroupsAMD])(numGroups, groupsSize, groups); }
EXPORT void glGetPerfMonitorCountersAMD(GLuint group, GLint * numCounters, GLint * maxActiveCounters, GLsizei counterSize, GLuint * counters) { ((void (*)(GLuint, GLint *, GLint *, GLsizei, GLuint *))TBL()[IDX_glGetPerfMonitorCountersAMD])(group, numCounters, maxActiveCounters, counterSize, counters); }
EXPORT void glGetPerfMonitorGroupStringAMD(GLuint group, GLsizei bufSize, GLsizei * length, GLchar * groupString) { ((void (*)(GLuint, GLsizei, GLsizei *, GLchar *))TBL()[IDX_glGetPerfMonitorGroupStringAMD])(group, bufSize, length, groupString); }
EXPORT void glGetPerfMonitorCounterStringAMD(GLuint group, GLuint counter, GLsizei bufSize, GLsizei * length, GLchar * counterString) { ((void (*)(GLuint, GLuint, GLsizei, GLsizei *, GLchar *))TBL()[IDX_glGetPerfMonitorCounterStringAMD])(group, counter, bufSize, length, counterString); }
EXPORT void glGetPerfMonitorCounterInfoAMD(GLuint group, GLuint counter, GLenum pname, void * data) { ((void (*)(GLuint, GLuint, GLenum, void *))TBL()[IDX_glGetPerfMonitorCounterInfoAMD])(group, counter, pname, data); }
EXPORT void glGenPerfMonitorsAMD(GLsizei n, GLuint * monitors) { ((void (*)(GLsizei, GLuint *))TBL()[IDX_glGenPerfMonitorsAMD])(n, monitors); }
EXPORT void glDeletePerfMonitorsAMD(GLsizei n, GLuint * monitors) { ((void (*)(GLsizei, GLuint *))TBL()[IDX_glDeletePerfMonitorsAMD])(n, monitors); }
EXPORT void glSelectPerfMonitorCountersAMD(GLuint monitor, GLboolean enable, GLuint group, GLint numCounters, GLuint * counterList) { ((void (*)(GLuint, GLboolean, GLuint, GLint, GLuint *))TBL()[IDX_glSelectPerfMonitorCountersAMD])(monitor, enable, group, numCounters, counterList); }
EXPORT void glBeginPerfMonitorAMD(GLuint monitor) { ((void (*)(GLuint))TBL()[IDX_glBeginPerfMonitorAMD])(monitor); }
EXPORT void glEndPerfMonitorAMD(GLuint monitor) { ((void (*)(GLuint))TBL()[IDX_glEndPerfMonitorAMD])(monitor); }
EXPORT void glGetPerfMonitorCounterDataAMD(GLuint monitor, GLenum pname, GLsizei dataSize, GLuint * data, GLint * bytesWritten) { ((void (*)(GLuint, GLenum, GLsizei, GLuint *, GLint *))TBL()[IDX_glGetPerfMonitorCounterDataAMD])(monitor, pname, dataSize, data, bytesWritten); }
EXPORT void glBlitFramebufferANGLE(GLint srcX0, GLint srcY0, GLint srcX1, GLint srcY1, GLint dstX0, GLint dstY0, GLint dstX1, GLint dstY1, GLbitfield mask, GLenum filter) { ((void (*)(GLint, GLint, GLint, GLint, GLint, GLint, GLint, GLint, GLbitfield, GLenum))TBL()[IDX_glBlitFramebufferANGLE])(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter); }
EXPORT void glRenderbufferStorageMultisampleANGLE(GLenum target, GLsizei samples, GLenum internalformat, GLsizei width, GLsizei height) { ((void (*)(GLenum, GLsizei, GLenum, GLsizei, GLsizei))TBL()[IDX_glRenderbufferStorageMultisampleANGLE])(target, samples, internalformat, width, height); }
EXPORT void glDrawArraysInstancedANGLE(GLenum mode, GLint first, GLsizei count, GLsizei primcount) { ((void (*)(GLenum, GLint, GLsizei, GLsizei))TBL()[IDX_glDrawArraysInstancedANGLE])(mode, first, count, primcount); }
EXPORT void glDrawElementsInstancedANGLE(GLenum mode, GLsizei count, GLenum type, const void * indices, GLsizei primcount) { ((void (*)(GLenum, GLsizei, GLenum, const void *, GLsizei))TBL()[IDX_glDrawElementsInstancedANGLE])(mode, count, type, indices, primcount); }
EXPORT void glVertexAttribDivisorANGLE(GLuint index, GLuint divisor) { ((void (*)(GLuint, GLuint))TBL()[IDX_glVertexAttribDivisorANGLE])(index, divisor); }
EXPORT void glGetTranslatedShaderSourceANGLE(GLuint shader, GLsizei bufsize, GLsizei * length, GLchar * source) { ((void (*)(GLuint, GLsizei, GLsizei *, GLchar *))TBL()[IDX_glGetTranslatedShaderSourceANGLE])(shader, bufsize, length, source); }
EXPORT void glCopyTextureLevelsAPPLE(GLuint destinationTexture, GLuint sourceTexture, GLint sourceBaseLevel, GLsizei sourceLevelCount) { ((void (*)(GLuint, GLuint, GLint, GLsizei))TBL()[IDX_glCopyTextureLevelsAPPLE])(destinationTexture, sourceTexture, sourceBaseLevel, sourceLevelCount); }
EXPORT void glRenderbufferStorageMultisampleAPPLE(GLenum target, GLsizei samples, GLenum internalformat, GLsizei width, GLsizei height) { ((void (*)(GLenum, GLsizei, GLenum, GLsizei, GLsizei))TBL()[IDX_glRenderbufferStorageMultisampleAPPLE])(target, samples, internalformat, width, height); }
EXPORT void glResolveMultisampleFramebufferAPPLE(void) { ((void (*)(void))TBL()[IDX_glResolveMultisampleFramebufferAPPLE])(); }
EXPORT GLsync glFenceSyncAPPLE(GLenum condition, GLbitfield flags) { return ((GLsync (*)(GLenum, GLbitfield))TBL()[IDX_glFenceSyncAPPLE])(condition, flags); }
EXPORT GLboolean glIsSyncAPPLE(GLsync sync) { return ((GLboolean (*)(GLsync))TBL()[IDX_glIsSyncAPPLE])(sync); }
EXPORT void glDeleteSyncAPPLE(GLsync sync) { ((void (*)(GLsync))TBL()[IDX_glDeleteSyncAPPLE])(sync); }
EXPORT GLenum glClientWaitSyncAPPLE(GLsync sync, GLbitfield flags, GLuint64 timeout) { return ((GLenum (*)(GLsync, GLbitfield, GLuint64))TBL()[IDX_glClientWaitSyncAPPLE])(sync, flags, timeout); }
EXPORT void glWaitSyncAPPLE(GLsync sync, GLbitfield flags, GLuint64 timeout) { ((void (*)(GLsync, GLbitfield, GLuint64))TBL()[IDX_glWaitSyncAPPLE])(sync, flags, timeout); }
EXPORT void glGetInteger64vAPPLE(GLenum pname, GLint64 * params) { ((void (*)(GLenum, GLint64 *))TBL()[IDX_glGetInteger64vAPPLE])(pname, params); }
EXPORT void glGetSyncivAPPLE(GLsync sync, GLenum pname, GLsizei bufSize, GLsizei * length, GLint * values) { ((void (*)(GLsync, GLenum, GLsizei, GLsizei *, GLint *))TBL()[IDX_glGetSyncivAPPLE])(sync, pname, bufSize, length, values); }
EXPORT void glEGLImageTargetTexStorageEXT(GLenum target, GLeglImageOES image, const GLint* attrib_list) { ((void (*)(GLenum, GLeglImageOES, const GLint*))TBL()[IDX_glEGLImageTargetTexStorageEXT])(target, image, attrib_list); }
EXPORT void glEGLImageTargetTextureStorageEXT(GLuint texture, GLeglImageOES image, const GLint* attrib_list) { ((void (*)(GLuint, GLeglImageOES, const GLint*))TBL()[IDX_glEGLImageTargetTextureStorageEXT])(texture, image, attrib_list); }
EXPORT void glDrawArraysInstancedBaseInstanceEXT(GLenum mode, GLint first, GLsizei count, GLsizei instancecount, GLuint baseinstance) { ((void (*)(GLenum, GLint, GLsizei, GLsizei, GLuint))TBL()[IDX_glDrawArraysInstancedBaseInstanceEXT])(mode, first, count, instancecount, baseinstance); }
EXPORT void glDrawElementsInstancedBaseInstanceEXT(GLenum mode, GLsizei count, GLenum type, const void * indices, GLsizei instancecount, GLuint baseinstance) { ((void (*)(GLenum, GLsizei, GLenum, const void *, GLsizei, GLuint))TBL()[IDX_glDrawElementsInstancedBaseInstanceEXT])(mode, count, type, indices, instancecount, baseinstance); }
EXPORT void glDrawElementsInstancedBaseVertexBaseInstanceEXT(GLenum mode, GLsizei count, GLenum type, const void * indices, GLsizei instancecount, GLint basevertex, GLuint baseinstance) { ((void (*)(GLenum, GLsizei, GLenum, const void *, GLsizei, GLint, GLuint))TBL()[IDX_glDrawElementsInstancedBaseVertexBaseInstanceEXT])(mode, count, type, indices, instancecount, basevertex, baseinstance); }
EXPORT void glBindFragDataLocationIndexedEXT(GLuint program, GLuint colorNumber, GLuint index, const GLchar * name) { ((void (*)(GLuint, GLuint, GLuint, const GLchar *))TBL()[IDX_glBindFragDataLocationIndexedEXT])(program, colorNumber, index, name); }
EXPORT void glBindFragDataLocationEXT(GLuint program, GLuint color, const GLchar * name) { ((void (*)(GLuint, GLuint, const GLchar *))TBL()[IDX_glBindFragDataLocationEXT])(program, color, name); }
EXPORT GLint glGetProgramResourceLocationIndexEXT(GLuint program, GLenum programInterface, const GLchar * name) { return ((GLint (*)(GLuint, GLenum, const GLchar *))TBL()[IDX_glGetProgramResourceLocationIndexEXT])(program, programInterface, name); }
EXPORT GLint glGetFragDataIndexEXT(GLuint program, const GLchar * name) { return ((GLint (*)(GLuint, const GLchar *))TBL()[IDX_glGetFragDataIndexEXT])(program, name); }
EXPORT void glBufferStorageEXT(GLenum target, GLsizeiptr size, const void * data, GLbitfield flags) { ((void (*)(GLenum, GLsizeiptr, const void *, GLbitfield))TBL()[IDX_glBufferStorageEXT])(target, size, data, flags); }
EXPORT void glClearTexImageEXT(GLuint texture, GLint level, GLenum format, GLenum type, const void * data) { ((void (*)(GLuint, GLint, GLenum, GLenum, const void *))TBL()[IDX_glClearTexImageEXT])(texture, level, format, type, data); }
EXPORT void glClearTexSubImageEXT(GLuint texture, GLint level, GLint xoffset, GLint yoffset, GLint zoffset, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, const void * data) { ((void (*)(GLuint, GLint, GLint, GLint, GLint, GLsizei, GLsizei, GLsizei, GLenum, GLenum, const void *))TBL()[IDX_glClearTexSubImageEXT])(texture, level, xoffset, yoffset, zoffset, width, height, depth, format, type, data); }
EXPORT void glClipControlEXT(GLenum origin, GLenum depth) { ((void (*)(GLenum, GLenum))TBL()[IDX_glClipControlEXT])(origin, depth); }
EXPORT void glCopyImageSubDataEXT(GLuint srcName, GLenum srcTarget, GLint srcLevel, GLint srcX, GLint srcY, GLint srcZ, GLuint dstName, GLenum dstTarget, GLint dstLevel, GLint dstX, GLint dstY, GLint dstZ, GLsizei srcWidth, GLsizei srcHeight, GLsizei srcDepth) { ((void (*)(GLuint, GLenum, GLint, GLint, GLint, GLint, GLuint, GLenum, GLint, GLint, GLint, GLint, GLsizei, GLsizei, GLsizei))TBL()[IDX_glCopyImageSubDataEXT])(srcName, srcTarget, srcLevel, srcX, srcY, srcZ, dstName, dstTarget, dstLevel, dstX, dstY, dstZ, srcWidth, srcHeight, srcDepth); }
EXPORT void glLabelObjectEXT(GLenum type, GLuint object, GLsizei length, const GLchar * label) { ((void (*)(GLenum, GLuint, GLsizei, const GLchar *))TBL()[IDX_glLabelObjectEXT])(type, object, length, label); }
EXPORT void glGetObjectLabelEXT(GLenum type, GLuint object, GLsizei bufSize, GLsizei * length, GLchar * label) { ((void (*)(GLenum, GLuint, GLsizei, GLsizei *, GLchar *))TBL()[IDX_glGetObjectLabelEXT])(type, object, bufSize, length, label); }
EXPORT void glInsertEventMarkerEXT(GLsizei length, const GLchar * marker) { ((void (*)(GLsizei, const GLchar *))TBL()[IDX_glInsertEventMarkerEXT])(length, marker); }
EXPORT void glPushGroupMarkerEXT(GLsizei length, const GLchar * marker) { ((void (*)(GLsizei, const GLchar *))TBL()[IDX_glPushGroupMarkerEXT])(length, marker); }
EXPORT void glPopGroupMarkerEXT(void) { ((void (*)(void))TBL()[IDX_glPopGroupMarkerEXT])(); }
EXPORT void glDiscardFramebufferEXT(GLenum target, GLsizei numAttachments, const GLenum * attachments) { ((void (*)(GLenum, GLsizei, const GLenum *))TBL()[IDX_glDiscardFramebufferEXT])(target, numAttachments, attachments); }
EXPORT void glGenQueriesEXT(GLsizei n, GLuint * ids) { ((void (*)(GLsizei, GLuint *))TBL()[IDX_glGenQueriesEXT])(n, ids); }
EXPORT void glDeleteQueriesEXT(GLsizei n, const GLuint * ids) { ((void (*)(GLsizei, const GLuint *))TBL()[IDX_glDeleteQueriesEXT])(n, ids); }
EXPORT GLboolean glIsQueryEXT(GLuint id) { return ((GLboolean (*)(GLuint))TBL()[IDX_glIsQueryEXT])(id); }
EXPORT void glBeginQueryEXT(GLenum target, GLuint id) { ((void (*)(GLenum, GLuint))TBL()[IDX_glBeginQueryEXT])(target, id); }
EXPORT void glEndQueryEXT(GLenum target) { ((void (*)(GLenum))TBL()[IDX_glEndQueryEXT])(target); }
EXPORT void glQueryCounterEXT(GLuint id, GLenum target) { ((void (*)(GLuint, GLenum))TBL()[IDX_glQueryCounterEXT])(id, target); }
EXPORT void glGetQueryivEXT(GLenum target, GLenum pname, GLint * params) { ((void (*)(GLenum, GLenum, GLint *))TBL()[IDX_glGetQueryivEXT])(target, pname, params); }
EXPORT void glGetQueryObjectivEXT(GLuint id, GLenum pname, GLint * params) { ((void (*)(GLuint, GLenum, GLint *))TBL()[IDX_glGetQueryObjectivEXT])(id, pname, params); }
EXPORT void glGetQueryObjectuivEXT(GLuint id, GLenum pname, GLuint * params) { ((void (*)(GLuint, GLenum, GLuint *))TBL()[IDX_glGetQueryObjectuivEXT])(id, pname, params); }
EXPORT void glGetQueryObjecti64vEXT(GLuint id, GLenum pname, GLint64 * params) { ((void (*)(GLuint, GLenum, GLint64 *))TBL()[IDX_glGetQueryObjecti64vEXT])(id, pname, params); }
EXPORT void glGetQueryObjectui64vEXT(GLuint id, GLenum pname, GLuint64 * params) { ((void (*)(GLuint, GLenum, GLuint64 *))TBL()[IDX_glGetQueryObjectui64vEXT])(id, pname, params); }
EXPORT void glDrawBuffersEXT(GLsizei n, const GLenum * bufs) { ((void (*)(GLsizei, const GLenum *))TBL()[IDX_glDrawBuffersEXT])(n, bufs); }
EXPORT void glEnableiEXT(GLenum target, GLuint index) { ((void (*)(GLenum, GLuint))TBL()[IDX_glEnableiEXT])(target, index); }
EXPORT void glDisableiEXT(GLenum target, GLuint index) { ((void (*)(GLenum, GLuint))TBL()[IDX_glDisableiEXT])(target, index); }
EXPORT void glBlendEquationiEXT(GLuint buf, GLenum mode) { ((void (*)(GLuint, GLenum))TBL()[IDX_glBlendEquationiEXT])(buf, mode); }
EXPORT void glBlendEquationSeparateiEXT(GLuint buf, GLenum modeRGB, GLenum modeAlpha) { ((void (*)(GLuint, GLenum, GLenum))TBL()[IDX_glBlendEquationSeparateiEXT])(buf, modeRGB, modeAlpha); }
EXPORT void glBlendFunciEXT(GLuint buf, GLenum src, GLenum dst) { ((void (*)(GLuint, GLenum, GLenum))TBL()[IDX_glBlendFunciEXT])(buf, src, dst); }
EXPORT void glBlendFuncSeparateiEXT(GLuint buf, GLenum srcRGB, GLenum dstRGB, GLenum srcAlpha, GLenum dstAlpha) { ((void (*)(GLuint, GLenum, GLenum, GLenum, GLenum))TBL()[IDX_glBlendFuncSeparateiEXT])(buf, srcRGB, dstRGB, srcAlpha, dstAlpha); }
EXPORT void glColorMaskiEXT(GLuint index, GLboolean r, GLboolean g, GLboolean b, GLboolean a) { ((void (*)(GLuint, GLboolean, GLboolean, GLboolean, GLboolean))TBL()[IDX_glColorMaskiEXT])(index, r, g, b, a); }
EXPORT GLboolean glIsEnablediEXT(GLenum target, GLuint index) { return ((GLboolean (*)(GLenum, GLuint))TBL()[IDX_glIsEnablediEXT])(target, index); }
EXPORT void glDrawElementsBaseVertexEXT(GLenum mode, GLsizei count, GLenum type, const void * indices, GLint basevertex) { ((void (*)(GLenum, GLsizei, GLenum, const void *, GLint))TBL()[IDX_glDrawElementsBaseVertexEXT])(mode, count, type, indices, basevertex); }
EXPORT void glDrawRangeElementsBaseVertexEXT(GLenum mode, GLuint start, GLuint end, GLsizei count, GLenum type, const void * indices, GLint basevertex) { ((void (*)(GLenum, GLuint, GLuint, GLsizei, GLenum, const void *, GLint))TBL()[IDX_glDrawRangeElementsBaseVertexEXT])(mode, start, end, count, type, indices, basevertex); }
EXPORT void glDrawElementsInstancedBaseVertexEXT(GLenum mode, GLsizei count, GLenum type, const void * indices, GLsizei instancecount, GLint basevertex) { ((void (*)(GLenum, GLsizei, GLenum, const void *, GLsizei, GLint))TBL()[IDX_glDrawElementsInstancedBaseVertexEXT])(mode, count, type, indices, instancecount, basevertex); }
EXPORT void glDrawArraysInstancedEXT(GLenum mode, GLint start, GLsizei count, GLsizei primcount) { ((void (*)(GLenum, GLint, GLsizei, GLsizei))TBL()[IDX_glDrawArraysInstancedEXT])(mode, start, count, primcount); }
EXPORT void glDrawElementsInstancedEXT(GLenum mode, GLsizei count, GLenum type, const void * indices, GLsizei primcount) { ((void (*)(GLenum, GLsizei, GLenum, const void *, GLsizei))TBL()[IDX_glDrawElementsInstancedEXT])(mode, count, type, indices, primcount); }
EXPORT void glDrawTransformFeedbackEXT(GLenum mode, GLuint id) { ((void (*)(GLenum, GLuint))TBL()[IDX_glDrawTransformFeedbackEXT])(mode, id); }
EXPORT void glDrawTransformFeedbackInstancedEXT(GLenum mode, GLuint id, GLsizei instancecount) { ((void (*)(GLenum, GLuint, GLsizei))TBL()[IDX_glDrawTransformFeedbackInstancedEXT])(mode, id, instancecount); }
EXPORT void glBufferStorageExternalEXT(GLenum target, GLintptr offset, GLsizeiptr size, GLeglClientBufferEXT clientBuffer, GLbitfield flags) { ((void (*)(GLenum, GLintptr, GLsizeiptr, GLeglClientBufferEXT, GLbitfield))TBL()[IDX_glBufferStorageExternalEXT])(target, offset, size, clientBuffer, flags); }
EXPORT void glNamedBufferStorageExternalEXT(GLuint buffer, GLintptr offset, GLsizeiptr size, GLeglClientBufferEXT clientBuffer, GLbitfield flags) { ((void (*)(GLuint, GLintptr, GLsizeiptr, GLeglClientBufferEXT, GLbitfield))TBL()[IDX_glNamedBufferStorageExternalEXT])(buffer, offset, size, clientBuffer, flags); }
EXPORT void glFramebufferTextureEXT(GLenum target, GLenum attachment, GLuint texture, GLint level) { ((void (*)(GLenum, GLenum, GLuint, GLint))TBL()[IDX_glFramebufferTextureEXT])(target, attachment, texture, level); }
EXPORT void glVertexAttribDivisorEXT(GLuint index, GLuint divisor) { ((void (*)(GLuint, GLuint))TBL()[IDX_glVertexAttribDivisorEXT])(index, divisor); }
EXPORT void * glMapBufferRangeEXT(GLenum target, GLintptr offset, GLsizeiptr length, GLbitfield access) { return ((void * (*)(GLenum, GLintptr, GLsizeiptr, GLbitfield))TBL()[IDX_glMapBufferRangeEXT])(target, offset, length, access); }
EXPORT void glFlushMappedBufferRangeEXT(GLenum target, GLintptr offset, GLsizeiptr length) { ((void (*)(GLenum, GLintptr, GLsizeiptr))TBL()[IDX_glFlushMappedBufferRangeEXT])(target, offset, length); }
EXPORT void glGetUnsignedBytevEXT(GLenum pname, GLubyte * data) { ((void (*)(GLenum, GLubyte *))TBL()[IDX_glGetUnsignedBytevEXT])(pname, data); }
EXPORT void glGetUnsignedBytei_vEXT(GLenum target, GLuint index, GLubyte * data) { ((void (*)(GLenum, GLuint, GLubyte *))TBL()[IDX_glGetUnsignedBytei_vEXT])(target, index, data); }
EXPORT void glDeleteMemoryObjectsEXT(GLsizei n, const GLuint * memoryObjects) { ((void (*)(GLsizei, const GLuint *))TBL()[IDX_glDeleteMemoryObjectsEXT])(n, memoryObjects); }
EXPORT GLboolean glIsMemoryObjectEXT(GLuint memoryObject) { return ((GLboolean (*)(GLuint))TBL()[IDX_glIsMemoryObjectEXT])(memoryObject); }
EXPORT void glCreateMemoryObjectsEXT(GLsizei n, GLuint * memoryObjects) { ((void (*)(GLsizei, GLuint *))TBL()[IDX_glCreateMemoryObjectsEXT])(n, memoryObjects); }
EXPORT void glMemoryObjectParameterivEXT(GLuint memoryObject, GLenum pname, const GLint * params) { ((void (*)(GLuint, GLenum, const GLint *))TBL()[IDX_glMemoryObjectParameterivEXT])(memoryObject, pname, params); }
EXPORT void glGetMemoryObjectParameterivEXT(GLuint memoryObject, GLenum pname, GLint * params) { ((void (*)(GLuint, GLenum, GLint *))TBL()[IDX_glGetMemoryObjectParameterivEXT])(memoryObject, pname, params); }
EXPORT void glTexStorageMem2DEXT(GLenum target, GLsizei levels, GLenum internalFormat, GLsizei width, GLsizei height, GLuint memory, GLuint64 offset) { ((void (*)(GLenum, GLsizei, GLenum, GLsizei, GLsizei, GLuint, GLuint64))TBL()[IDX_glTexStorageMem2DEXT])(target, levels, internalFormat, width, height, memory, offset); }
EXPORT void glTexStorageMem2DMultisampleEXT(GLenum target, GLsizei samples, GLenum internalFormat, GLsizei width, GLsizei height, GLboolean fixedSampleLocations, GLuint memory, GLuint64 offset) { ((void (*)(GLenum, GLsizei, GLenum, GLsizei, GLsizei, GLboolean, GLuint, GLuint64))TBL()[IDX_glTexStorageMem2DMultisampleEXT])(target, samples, internalFormat, width, height, fixedSampleLocations, memory, offset); }
EXPORT void glTexStorageMem3DEXT(GLenum target, GLsizei levels, GLenum internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLuint memory, GLuint64 offset) { ((void (*)(GLenum, GLsizei, GLenum, GLsizei, GLsizei, GLsizei, GLuint, GLuint64))TBL()[IDX_glTexStorageMem3DEXT])(target, levels, internalFormat, width, height, depth, memory, offset); }
EXPORT void glTexStorageMem3DMultisampleEXT(GLenum target, GLsizei samples, GLenum internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLboolean fixedSampleLocations, GLuint memory, GLuint64 offset) { ((void (*)(GLenum, GLsizei, GLenum, GLsizei, GLsizei, GLsizei, GLboolean, GLuint, GLuint64))TBL()[IDX_glTexStorageMem3DMultisampleEXT])(target, samples, internalFormat, width, height, depth, fixedSampleLocations, memory, offset); }
EXPORT void glBufferStorageMemEXT(GLenum target, GLsizeiptr size, GLuint memory, GLuint64 offset) { ((void (*)(GLenum, GLsizeiptr, GLuint, GLuint64))TBL()[IDX_glBufferStorageMemEXT])(target, size, memory, offset); }
EXPORT void glTextureStorageMem2DEXT(GLuint texture, GLsizei levels, GLenum internalFormat, GLsizei width, GLsizei height, GLuint memory, GLuint64 offset) { ((void (*)(GLuint, GLsizei, GLenum, GLsizei, GLsizei, GLuint, GLuint64))TBL()[IDX_glTextureStorageMem2DEXT])(texture, levels, internalFormat, width, height, memory, offset); }
EXPORT void glTextureStorageMem2DMultisampleEXT(GLuint texture, GLsizei samples, GLenum internalFormat, GLsizei width, GLsizei height, GLboolean fixedSampleLocations, GLuint memory, GLuint64 offset) { ((void (*)(GLuint, GLsizei, GLenum, GLsizei, GLsizei, GLboolean, GLuint, GLuint64))TBL()[IDX_glTextureStorageMem2DMultisampleEXT])(texture, samples, internalFormat, width, height, fixedSampleLocations, memory, offset); }
EXPORT void glTextureStorageMem3DEXT(GLuint texture, GLsizei levels, GLenum internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLuint memory, GLuint64 offset) { ((void (*)(GLuint, GLsizei, GLenum, GLsizei, GLsizei, GLsizei, GLuint, GLuint64))TBL()[IDX_glTextureStorageMem3DEXT])(texture, levels, internalFormat, width, height, depth, memory, offset); }
EXPORT void glTextureStorageMem3DMultisampleEXT(GLuint texture, GLsizei samples, GLenum internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLboolean fixedSampleLocations, GLuint memory, GLuint64 offset) { ((void (*)(GLuint, GLsizei, GLenum, GLsizei, GLsizei, GLsizei, GLboolean, GLuint, GLuint64))TBL()[IDX_glTextureStorageMem3DMultisampleEXT])(texture, samples, internalFormat, width, height, depth, fixedSampleLocations, memory, offset); }
EXPORT void glNamedBufferStorageMemEXT(GLuint buffer, GLsizeiptr size, GLuint memory, GLuint64 offset) { ((void (*)(GLuint, GLsizeiptr, GLuint, GLuint64))TBL()[IDX_glNamedBufferStorageMemEXT])(buffer, size, memory, offset); }
EXPORT void glImportMemoryFdEXT(GLuint memory, GLuint64 size, GLenum handleType, GLint fd) { ((void (*)(GLuint, GLuint64, GLenum, GLint))TBL()[IDX_glImportMemoryFdEXT])(memory, size, handleType, fd); }
EXPORT void glImportMemoryWin32HandleEXT(GLuint memory, GLuint64 size, GLenum handleType, void * handle) { ((void (*)(GLuint, GLuint64, GLenum, void *))TBL()[IDX_glImportMemoryWin32HandleEXT])(memory, size, handleType, handle); }
EXPORT void glImportMemoryWin32NameEXT(GLuint memory, GLuint64 size, GLenum handleType, const void * name) { ((void (*)(GLuint, GLuint64, GLenum, const void *))TBL()[IDX_glImportMemoryWin32NameEXT])(memory, size, handleType, name); }
EXPORT void glMultiDrawArraysEXT(GLenum mode, const GLint * first, const GLsizei * count, GLsizei primcount) { ((void (*)(GLenum, const GLint *, const GLsizei *, GLsizei))TBL()[IDX_glMultiDrawArraysEXT])(mode, first, count, primcount); }
EXPORT void glMultiDrawElementsEXT(GLenum mode, const GLsizei * count, GLenum type, const void *const* indices, GLsizei primcount) { ((void (*)(GLenum, const GLsizei *, GLenum, const void *const*, GLsizei))TBL()[IDX_glMultiDrawElementsEXT])(mode, count, type, indices, primcount); }
EXPORT void glMultiDrawArraysIndirectEXT(GLenum mode, const void * indirect, GLsizei drawcount, GLsizei stride) { ((void (*)(GLenum, const void *, GLsizei, GLsizei))TBL()[IDX_glMultiDrawArraysIndirectEXT])(mode, indirect, drawcount, stride); }
EXPORT void glMultiDrawElementsIndirectEXT(GLenum mode, GLenum type, const void * indirect, GLsizei drawcount, GLsizei stride) { ((void (*)(GLenum, GLenum, const void *, GLsizei, GLsizei))TBL()[IDX_glMultiDrawElementsIndirectEXT])(mode, type, indirect, drawcount, stride); }
EXPORT void glRenderbufferStorageMultisampleEXT(GLenum target, GLsizei samples, GLenum internalformat, GLsizei width, GLsizei height) { ((void (*)(GLenum, GLsizei, GLenum, GLsizei, GLsizei))TBL()[IDX_glRenderbufferStorageMultisampleEXT])(target, samples, internalformat, width, height); }
EXPORT void glFramebufferTexture2DMultisampleEXT(GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level, GLsizei samples) { ((void (*)(GLenum, GLenum, GLenum, GLuint, GLint, GLsizei))TBL()[IDX_glFramebufferTexture2DMultisampleEXT])(target, attachment, textarget, texture, level, samples); }
EXPORT void glReadBufferIndexedEXT(GLenum src, GLint index) { ((void (*)(GLenum, GLint))TBL()[IDX_glReadBufferIndexedEXT])(src, index); }
EXPORT void glDrawBuffersIndexedEXT(GLint n, const GLenum * location, const GLint * indices) { ((void (*)(GLint, const GLenum *, const GLint *))TBL()[IDX_glDrawBuffersIndexedEXT])(n, location, indices); }
EXPORT void glGetIntegeri_vEXT(GLenum target, GLuint index, GLint * data) { ((void (*)(GLenum, GLuint, GLint *))TBL()[IDX_glGetIntegeri_vEXT])(target, index, data); }
EXPORT void glPolygonOffsetClampEXT(GLfloat factor, GLfloat units, GLfloat clamp) { ((void (*)(GLfloat, GLfloat, GLfloat))TBL()[IDX_glPolygonOffsetClampEXT])(factor, units, clamp); }
EXPORT void glPrimitiveBoundingBoxEXT(GLfloat minX, GLfloat minY, GLfloat minZ, GLfloat minW, GLfloat maxX, GLfloat maxY, GLfloat maxZ, GLfloat maxW) { ((void (*)(GLfloat, GLfloat, GLfloat, GLfloat, GLfloat, GLfloat, GLfloat, GLfloat))TBL()[IDX_glPrimitiveBoundingBoxEXT])(minX, minY, minZ, minW, maxX, maxY, maxZ, maxW); }
EXPORT void glRasterSamplesEXT(GLuint samples, GLboolean fixedsamplelocations) { ((void (*)(GLuint, GLboolean))TBL()[IDX_glRasterSamplesEXT])(samples, fixedsamplelocations); }
EXPORT GLenum glGetGraphicsResetStatusEXT(void) { return ((GLenum (*)(void))TBL()[IDX_glGetGraphicsResetStatusEXT])(); }
EXPORT void glReadnPixelsEXT(GLint x, GLint y, GLsizei width, GLsizei height, GLenum format, GLenum type, GLsizei bufSize, void * data) { ((void (*)(GLint, GLint, GLsizei, GLsizei, GLenum, GLenum, GLsizei, void *))TBL()[IDX_glReadnPixelsEXT])(x, y, width, height, format, type, bufSize, data); }
EXPORT void glGetnUniformfvEXT(GLuint program, GLint location, GLsizei bufSize, GLfloat * params) { ((void (*)(GLuint, GLint, GLsizei, GLfloat *))TBL()[IDX_glGetnUniformfvEXT])(program, location, bufSize, params); }
EXPORT void glGetnUniformivEXT(GLuint program, GLint location, GLsizei bufSize, GLint * params) { ((void (*)(GLuint, GLint, GLsizei, GLint *))TBL()[IDX_glGetnUniformivEXT])(program, location, bufSize, params); }
EXPORT void glGenSemaphoresEXT(GLsizei n, GLuint * semaphores) { ((void (*)(GLsizei, GLuint *))TBL()[IDX_glGenSemaphoresEXT])(n, semaphores); }
EXPORT void glDeleteSemaphoresEXT(GLsizei n, const GLuint * semaphores) { ((void (*)(GLsizei, const GLuint *))TBL()[IDX_glDeleteSemaphoresEXT])(n, semaphores); }
EXPORT GLboolean glIsSemaphoreEXT(GLuint semaphore) { return ((GLboolean (*)(GLuint))TBL()[IDX_glIsSemaphoreEXT])(semaphore); }
EXPORT void glSemaphoreParameterui64vEXT(GLuint semaphore, GLenum pname, const GLuint64 * params) { ((void (*)(GLuint, GLenum, const GLuint64 *))TBL()[IDX_glSemaphoreParameterui64vEXT])(semaphore, pname, params); }
EXPORT void glGetSemaphoreParameterui64vEXT(GLuint semaphore, GLenum pname, GLuint64 * params) { ((void (*)(GLuint, GLenum, GLuint64 *))TBL()[IDX_glGetSemaphoreParameterui64vEXT])(semaphore, pname, params); }
EXPORT void glWaitSemaphoreEXT(GLuint semaphore, GLuint numBufferBarriers, const GLuint * buffers, GLuint numTextureBarriers, const GLuint * textures, const GLenum * srcLayouts) { ((void (*)(GLuint, GLuint, const GLuint *, GLuint, const GLuint *, const GLenum *))TBL()[IDX_glWaitSemaphoreEXT])(semaphore, numBufferBarriers, buffers, numTextureBarriers, textures, srcLayouts); }
EXPORT void glSignalSemaphoreEXT(GLuint semaphore, GLuint numBufferBarriers, const GLuint * buffers, GLuint numTextureBarriers, const GLuint * textures, const GLenum * dstLayouts) { ((void (*)(GLuint, GLuint, const GLuint *, GLuint, const GLuint *, const GLenum *))TBL()[IDX_glSignalSemaphoreEXT])(semaphore, numBufferBarriers, buffers, numTextureBarriers, textures, dstLayouts); }
EXPORT void glImportSemaphoreFdEXT(GLuint semaphore, GLenum handleType, GLint fd) { ((void (*)(GLuint, GLenum, GLint))TBL()[IDX_glImportSemaphoreFdEXT])(semaphore, handleType, fd); }
EXPORT void glImportSemaphoreWin32HandleEXT(GLuint semaphore, GLenum handleType, void * handle) { ((void (*)(GLuint, GLenum, void *))TBL()[IDX_glImportSemaphoreWin32HandleEXT])(semaphore, handleType, handle); }
EXPORT void glImportSemaphoreWin32NameEXT(GLuint semaphore, GLenum handleType, const void * name) { ((void (*)(GLuint, GLenum, const void *))TBL()[IDX_glImportSemaphoreWin32NameEXT])(semaphore, handleType, name); }
EXPORT void glActiveShaderProgramEXT(GLuint pipeline, GLuint program) { ((void (*)(GLuint, GLuint))TBL()[IDX_glActiveShaderProgramEXT])(pipeline, program); }
EXPORT void glBindProgramPipelineEXT(GLuint pipeline) { ((void (*)(GLuint))TBL()[IDX_glBindProgramPipelineEXT])(pipeline); }
EXPORT GLuint glCreateShaderProgramvEXT(GLenum type, GLsizei count, const GLchar ** strings) { return ((GLuint (*)(GLenum, GLsizei, const GLchar **))TBL()[IDX_glCreateShaderProgramvEXT])(type, count, strings); }
EXPORT void glDeleteProgramPipelinesEXT(GLsizei n, const GLuint * pipelines) { ((void (*)(GLsizei, const GLuint *))TBL()[IDX_glDeleteProgramPipelinesEXT])(n, pipelines); }
EXPORT void glGenProgramPipelinesEXT(GLsizei n, GLuint * pipelines) { ((void (*)(GLsizei, GLuint *))TBL()[IDX_glGenProgramPipelinesEXT])(n, pipelines); }
EXPORT void glGetProgramPipelineInfoLogEXT(GLuint pipeline, GLsizei bufSize, GLsizei * length, GLchar * infoLog) { ((void (*)(GLuint, GLsizei, GLsizei *, GLchar *))TBL()[IDX_glGetProgramPipelineInfoLogEXT])(pipeline, bufSize, length, infoLog); }
EXPORT void glGetProgramPipelineivEXT(GLuint pipeline, GLenum pname, GLint * params) { ((void (*)(GLuint, GLenum, GLint *))TBL()[IDX_glGetProgramPipelineivEXT])(pipeline, pname, params); }
EXPORT GLboolean glIsProgramPipelineEXT(GLuint pipeline) { return ((GLboolean (*)(GLuint))TBL()[IDX_glIsProgramPipelineEXT])(pipeline); }
EXPORT void glProgramParameteriEXT(GLuint program, GLenum pname, GLint value) { ((void (*)(GLuint, GLenum, GLint))TBL()[IDX_glProgramParameteriEXT])(program, pname, value); }
EXPORT void glProgramUniform1fEXT(GLuint program, GLint location, GLfloat v0) { ((void (*)(GLuint, GLint, GLfloat))TBL()[IDX_glProgramUniform1fEXT])(program, location, v0); }
EXPORT void glProgramUniform1fvEXT(GLuint program, GLint location, GLsizei count, const GLfloat * value) { ((void (*)(GLuint, GLint, GLsizei, const GLfloat *))TBL()[IDX_glProgramUniform1fvEXT])(program, location, count, value); }
EXPORT void glProgramUniform1iEXT(GLuint program, GLint location, GLint v0) { ((void (*)(GLuint, GLint, GLint))TBL()[IDX_glProgramUniform1iEXT])(program, location, v0); }
EXPORT void glProgramUniform1ivEXT(GLuint program, GLint location, GLsizei count, const GLint * value) { ((void (*)(GLuint, GLint, GLsizei, const GLint *))TBL()[IDX_glProgramUniform1ivEXT])(program, location, count, value); }
EXPORT void glProgramUniform2fEXT(GLuint program, GLint location, GLfloat v0, GLfloat v1) { ((void (*)(GLuint, GLint, GLfloat, GLfloat))TBL()[IDX_glProgramUniform2fEXT])(program, location, v0, v1); }
EXPORT void glProgramUniform2fvEXT(GLuint program, GLint location, GLsizei count, const GLfloat * value) { ((void (*)(GLuint, GLint, GLsizei, const GLfloat *))TBL()[IDX_glProgramUniform2fvEXT])(program, location, count, value); }
EXPORT void glProgramUniform2iEXT(GLuint program, GLint location, GLint v0, GLint v1) { ((void (*)(GLuint, GLint, GLint, GLint))TBL()[IDX_glProgramUniform2iEXT])(program, location, v0, v1); }
EXPORT void glProgramUniform2ivEXT(GLuint program, GLint location, GLsizei count, const GLint * value) { ((void (*)(GLuint, GLint, GLsizei, const GLint *))TBL()[IDX_glProgramUniform2ivEXT])(program, location, count, value); }
EXPORT void glProgramUniform3fEXT(GLuint program, GLint location, GLfloat v0, GLfloat v1, GLfloat v2) { ((void (*)(GLuint, GLint, GLfloat, GLfloat, GLfloat))TBL()[IDX_glProgramUniform3fEXT])(program, location, v0, v1, v2); }
EXPORT void glProgramUniform3fvEXT(GLuint program, GLint location, GLsizei count, const GLfloat * value) { ((void (*)(GLuint, GLint, GLsizei, const GLfloat *))TBL()[IDX_glProgramUniform3fvEXT])(program, location, count, value); }
EXPORT void glProgramUniform3iEXT(GLuint program, GLint location, GLint v0, GLint v1, GLint v2) { ((void (*)(GLuint, GLint, GLint, GLint, GLint))TBL()[IDX_glProgramUniform3iEXT])(program, location, v0, v1, v2); }
EXPORT void glProgramUniform3ivEXT(GLuint program, GLint location, GLsizei count, const GLint * value) { ((void (*)(GLuint, GLint, GLsizei, const GLint *))TBL()[IDX_glProgramUniform3ivEXT])(program, location, count, value); }
EXPORT void glProgramUniform4fEXT(GLuint program, GLint location, GLfloat v0, GLfloat v1, GLfloat v2, GLfloat v3) { ((void (*)(GLuint, GLint, GLfloat, GLfloat, GLfloat, GLfloat))TBL()[IDX_glProgramUniform4fEXT])(program, location, v0, v1, v2, v3); }
EXPORT void glProgramUniform4fvEXT(GLuint program, GLint location, GLsizei count, const GLfloat * value) { ((void (*)(GLuint, GLint, GLsizei, const GLfloat *))TBL()[IDX_glProgramUniform4fvEXT])(program, location, count, value); }
EXPORT void glProgramUniform4iEXT(GLuint program, GLint location, GLint v0, GLint v1, GLint v2, GLint v3) { ((void (*)(GLuint, GLint, GLint, GLint, GLint, GLint))TBL()[IDX_glProgramUniform4iEXT])(program, location, v0, v1, v2, v3); }
EXPORT void glProgramUniform4ivEXT(GLuint program, GLint location, GLsizei count, const GLint * value) { ((void (*)(GLuint, GLint, GLsizei, const GLint *))TBL()[IDX_glProgramUniform4ivEXT])(program, location, count, value); }
EXPORT void glProgramUniformMatrix2fvEXT(GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { ((void (*)(GLuint, GLint, GLsizei, GLboolean, const GLfloat *))TBL()[IDX_glProgramUniformMatrix2fvEXT])(program, location, count, transpose, value); }
EXPORT void glProgramUniformMatrix3fvEXT(GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { ((void (*)(GLuint, GLint, GLsizei, GLboolean, const GLfloat *))TBL()[IDX_glProgramUniformMatrix3fvEXT])(program, location, count, transpose, value); }
EXPORT void glProgramUniformMatrix4fvEXT(GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { ((void (*)(GLuint, GLint, GLsizei, GLboolean, const GLfloat *))TBL()[IDX_glProgramUniformMatrix4fvEXT])(program, location, count, transpose, value); }
EXPORT void glUseProgramStagesEXT(GLuint pipeline, GLbitfield stages, GLuint program) { ((void (*)(GLuint, GLbitfield, GLuint))TBL()[IDX_glUseProgramStagesEXT])(pipeline, stages, program); }
EXPORT void glValidateProgramPipelineEXT(GLuint pipeline) { ((void (*)(GLuint))TBL()[IDX_glValidateProgramPipelineEXT])(pipeline); }
EXPORT void glProgramUniform1uiEXT(GLuint program, GLint location, GLuint v0) { ((void (*)(GLuint, GLint, GLuint))TBL()[IDX_glProgramUniform1uiEXT])(program, location, v0); }
EXPORT void glProgramUniform2uiEXT(GLuint program, GLint location, GLuint v0, GLuint v1) { ((void (*)(GLuint, GLint, GLuint, GLuint))TBL()[IDX_glProgramUniform2uiEXT])(program, location, v0, v1); }
EXPORT void glProgramUniform3uiEXT(GLuint program, GLint location, GLuint v0, GLuint v1, GLuint v2) { ((void (*)(GLuint, GLint, GLuint, GLuint, GLuint))TBL()[IDX_glProgramUniform3uiEXT])(program, location, v0, v1, v2); }
EXPORT void glProgramUniform4uiEXT(GLuint program, GLint location, GLuint v0, GLuint v1, GLuint v2, GLuint v3) { ((void (*)(GLuint, GLint, GLuint, GLuint, GLuint, GLuint))TBL()[IDX_glProgramUniform4uiEXT])(program, location, v0, v1, v2, v3); }
EXPORT void glProgramUniform1uivEXT(GLuint program, GLint location, GLsizei count, const GLuint * value) { ((void (*)(GLuint, GLint, GLsizei, const GLuint *))TBL()[IDX_glProgramUniform1uivEXT])(program, location, count, value); }
EXPORT void glProgramUniform2uivEXT(GLuint program, GLint location, GLsizei count, const GLuint * value) { ((void (*)(GLuint, GLint, GLsizei, const GLuint *))TBL()[IDX_glProgramUniform2uivEXT])(program, location, count, value); }
EXPORT void glProgramUniform3uivEXT(GLuint program, GLint location, GLsizei count, const GLuint * value) { ((void (*)(GLuint, GLint, GLsizei, const GLuint *))TBL()[IDX_glProgramUniform3uivEXT])(program, location, count, value); }
EXPORT void glProgramUniform4uivEXT(GLuint program, GLint location, GLsizei count, const GLuint * value) { ((void (*)(GLuint, GLint, GLsizei, const GLuint *))TBL()[IDX_glProgramUniform4uivEXT])(program, location, count, value); }
EXPORT void glProgramUniformMatrix2x3fvEXT(GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { ((void (*)(GLuint, GLint, GLsizei, GLboolean, const GLfloat *))TBL()[IDX_glProgramUniformMatrix2x3fvEXT])(program, location, count, transpose, value); }
EXPORT void glProgramUniformMatrix3x2fvEXT(GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { ((void (*)(GLuint, GLint, GLsizei, GLboolean, const GLfloat *))TBL()[IDX_glProgramUniformMatrix3x2fvEXT])(program, location, count, transpose, value); }
EXPORT void glProgramUniformMatrix2x4fvEXT(GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { ((void (*)(GLuint, GLint, GLsizei, GLboolean, const GLfloat *))TBL()[IDX_glProgramUniformMatrix2x4fvEXT])(program, location, count, transpose, value); }
EXPORT void glProgramUniformMatrix4x2fvEXT(GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { ((void (*)(GLuint, GLint, GLsizei, GLboolean, const GLfloat *))TBL()[IDX_glProgramUniformMatrix4x2fvEXT])(program, location, count, transpose, value); }
EXPORT void glProgramUniformMatrix3x4fvEXT(GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { ((void (*)(GLuint, GLint, GLsizei, GLboolean, const GLfloat *))TBL()[IDX_glProgramUniformMatrix3x4fvEXT])(program, location, count, transpose, value); }
EXPORT void glProgramUniformMatrix4x3fvEXT(GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { ((void (*)(GLuint, GLint, GLsizei, GLboolean, const GLfloat *))TBL()[IDX_glProgramUniformMatrix4x3fvEXT])(program, location, count, transpose, value); }
EXPORT void glFramebufferFetchBarrierEXT(void) { ((void (*)(void))TBL()[IDX_glFramebufferFetchBarrierEXT])(); }
EXPORT void glFramebufferPixelLocalStorageSizeEXT(GLuint target, GLsizei size) { ((void (*)(GLuint, GLsizei))TBL()[IDX_glFramebufferPixelLocalStorageSizeEXT])(target, size); }
EXPORT GLsizei glGetFramebufferPixelLocalStorageSizeEXT(GLuint target) { return ((GLsizei (*)(GLuint))TBL()[IDX_glGetFramebufferPixelLocalStorageSizeEXT])(target); }
EXPORT void glClearPixelLocalStorageuiEXT(GLsizei offset, GLsizei n, const GLuint * values) { ((void (*)(GLsizei, GLsizei, const GLuint *))TBL()[IDX_glClearPixelLocalStorageuiEXT])(offset, n, values); }
EXPORT void glTexPageCommitmentEXT(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint zoffset, GLsizei width, GLsizei height, GLsizei depth, GLboolean commit) { ((void (*)(GLenum, GLint, GLint, GLint, GLint, GLsizei, GLsizei, GLsizei, GLboolean))TBL()[IDX_glTexPageCommitmentEXT])(target, level, xoffset, yoffset, zoffset, width, height, depth, commit); }
EXPORT void glPatchParameteriEXT(GLenum pname, GLint value) { ((void (*)(GLenum, GLint))TBL()[IDX_glPatchParameteriEXT])(pname, value); }
EXPORT void glTexParameterIivEXT(GLenum target, GLenum pname, const GLint * params) { ((void (*)(GLenum, GLenum, const GLint *))TBL()[IDX_glTexParameterIivEXT])(target, pname, params); }
EXPORT void glTexParameterIuivEXT(GLenum target, GLenum pname, const GLuint * params) { ((void (*)(GLenum, GLenum, const GLuint *))TBL()[IDX_glTexParameterIuivEXT])(target, pname, params); }
EXPORT void glGetTexParameterIivEXT(GLenum target, GLenum pname, GLint * params) { ((void (*)(GLenum, GLenum, GLint *))TBL()[IDX_glGetTexParameterIivEXT])(target, pname, params); }
EXPORT void glGetTexParameterIuivEXT(GLenum target, GLenum pname, GLuint * params) { ((void (*)(GLenum, GLenum, GLuint *))TBL()[IDX_glGetTexParameterIuivEXT])(target, pname, params); }
EXPORT void glSamplerParameterIivEXT(GLuint sampler, GLenum pname, const GLint * param) { ((void (*)(GLuint, GLenum, const GLint *))TBL()[IDX_glSamplerParameterIivEXT])(sampler, pname, param); }
EXPORT void glSamplerParameterIuivEXT(GLuint sampler, GLenum pname, const GLuint * param) { ((void (*)(GLuint, GLenum, const GLuint *))TBL()[IDX_glSamplerParameterIuivEXT])(sampler, pname, param); }
EXPORT void glGetSamplerParameterIivEXT(GLuint sampler, GLenum pname, GLint * params) { ((void (*)(GLuint, GLenum, GLint *))TBL()[IDX_glGetSamplerParameterIivEXT])(sampler, pname, params); }
EXPORT void glGetSamplerParameterIuivEXT(GLuint sampler, GLenum pname, GLuint * params) { ((void (*)(GLuint, GLenum, GLuint *))TBL()[IDX_glGetSamplerParameterIuivEXT])(sampler, pname, params); }
EXPORT void glTexBufferEXT(GLenum target, GLenum internalformat, GLuint buffer) { ((void (*)(GLenum, GLenum, GLuint))TBL()[IDX_glTexBufferEXT])(target, internalformat, buffer); }
EXPORT void glTexBufferRangeEXT(GLenum target, GLenum internalformat, GLuint buffer, GLintptr offset, GLsizeiptr size) { ((void (*)(GLenum, GLenum, GLuint, GLintptr, GLsizeiptr))TBL()[IDX_glTexBufferRangeEXT])(target, internalformat, buffer, offset, size); }
EXPORT void glTexStorage1DEXT(GLenum target, GLsizei levels, GLenum internalformat, GLsizei width) { ((void (*)(GLenum, GLsizei, GLenum, GLsizei))TBL()[IDX_glTexStorage1DEXT])(target, levels, internalformat, width); }
EXPORT void glTexStorage2DEXT(GLenum target, GLsizei levels, GLenum internalformat, GLsizei width, GLsizei height) { ((void (*)(GLenum, GLsizei, GLenum, GLsizei, GLsizei))TBL()[IDX_glTexStorage2DEXT])(target, levels, internalformat, width, height); }
EXPORT void glTexStorage3DEXT(GLenum target, GLsizei levels, GLenum internalformat, GLsizei width, GLsizei height, GLsizei depth) { ((void (*)(GLenum, GLsizei, GLenum, GLsizei, GLsizei, GLsizei))TBL()[IDX_glTexStorage3DEXT])(target, levels, internalformat, width, height, depth); }
EXPORT void glTextureStorage1DEXT(GLuint texture, GLenum target, GLsizei levels, GLenum internalformat, GLsizei width) { ((void (*)(GLuint, GLenum, GLsizei, GLenum, GLsizei))TBL()[IDX_glTextureStorage1DEXT])(texture, target, levels, internalformat, width); }
EXPORT void glTextureStorage2DEXT(GLuint texture, GLenum target, GLsizei levels, GLenum internalformat, GLsizei width, GLsizei height) { ((void (*)(GLuint, GLenum, GLsizei, GLenum, GLsizei, GLsizei))TBL()[IDX_glTextureStorage2DEXT])(texture, target, levels, internalformat, width, height); }
EXPORT void glTextureStorage3DEXT(GLuint texture, GLenum target, GLsizei levels, GLenum internalformat, GLsizei width, GLsizei height, GLsizei depth) { ((void (*)(GLuint, GLenum, GLsizei, GLenum, GLsizei, GLsizei, GLsizei))TBL()[IDX_glTextureStorage3DEXT])(texture, target, levels, internalformat, width, height, depth); }
EXPORT void glTextureViewEXT(GLuint texture, GLenum target, GLuint origtexture, GLenum internalformat, GLuint minlevel, GLuint numlevels, GLuint minlayer, GLuint numlayers) { ((void (*)(GLuint, GLenum, GLuint, GLenum, GLuint, GLuint, GLuint, GLuint))TBL()[IDX_glTextureViewEXT])(texture, target, origtexture, internalformat, minlevel, numlevels, minlayer, numlayers); }
EXPORT GLboolean glAcquireKeyedMutexWin32EXT(GLuint memory, GLuint64 key, GLuint timeout) { return ((GLboolean (*)(GLuint, GLuint64, GLuint))TBL()[IDX_glAcquireKeyedMutexWin32EXT])(memory, key, timeout); }
EXPORT GLboolean glReleaseKeyedMutexWin32EXT(GLuint memory, GLuint64 key) { return ((GLboolean (*)(GLuint, GLuint64))TBL()[IDX_glReleaseKeyedMutexWin32EXT])(memory, key); }
EXPORT void glWindowRectanglesEXT(GLenum mode, GLsizei count, const GLint * box) { ((void (*)(GLenum, GLsizei, const GLint *))TBL()[IDX_glWindowRectanglesEXT])(mode, count, box); }
EXPORT GLuint64 glGetTextureHandleIMG(GLuint texture) { return ((GLuint64 (*)(GLuint))TBL()[IDX_glGetTextureHandleIMG])(texture); }
EXPORT GLuint64 glGetTextureSamplerHandleIMG(GLuint texture, GLuint sampler) { return ((GLuint64 (*)(GLuint, GLuint))TBL()[IDX_glGetTextureSamplerHandleIMG])(texture, sampler); }
EXPORT void glUniformHandleui64IMG(GLint location, GLuint64 value) { ((void (*)(GLint, GLuint64))TBL()[IDX_glUniformHandleui64IMG])(location, value); }
EXPORT void glUniformHandleui64vIMG(GLint location, GLsizei count, const GLuint64 * value) { ((void (*)(GLint, GLsizei, const GLuint64 *))TBL()[IDX_glUniformHandleui64vIMG])(location, count, value); }
EXPORT void glProgramUniformHandleui64IMG(GLuint program, GLint location, GLuint64 value) { ((void (*)(GLuint, GLint, GLuint64))TBL()[IDX_glProgramUniformHandleui64IMG])(program, location, value); }
EXPORT void glProgramUniformHandleui64vIMG(GLuint program, GLint location, GLsizei count, const GLuint64 * values) { ((void (*)(GLuint, GLint, GLsizei, const GLuint64 *))TBL()[IDX_glProgramUniformHandleui64vIMG])(program, location, count, values); }
EXPORT void glFramebufferTexture2DDownsampleIMG(GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level, GLint xscale, GLint yscale) { ((void (*)(GLenum, GLenum, GLenum, GLuint, GLint, GLint, GLint))TBL()[IDX_glFramebufferTexture2DDownsampleIMG])(target, attachment, textarget, texture, level, xscale, yscale); }
EXPORT void glFramebufferTextureLayerDownsampleIMG(GLenum target, GLenum attachment, GLuint texture, GLint level, GLint layer, GLint xscale, GLint yscale) { ((void (*)(GLenum, GLenum, GLuint, GLint, GLint, GLint, GLint))TBL()[IDX_glFramebufferTextureLayerDownsampleIMG])(target, attachment, texture, level, layer, xscale, yscale); }
EXPORT void glRenderbufferStorageMultisampleIMG(GLenum target, GLsizei samples, GLenum internalformat, GLsizei width, GLsizei height) { ((void (*)(GLenum, GLsizei, GLenum, GLsizei, GLsizei))TBL()[IDX_glRenderbufferStorageMultisampleIMG])(target, samples, internalformat, width, height); }
EXPORT void glFramebufferTexture2DMultisampleIMG(GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level, GLsizei samples) { ((void (*)(GLenum, GLenum, GLenum, GLuint, GLint, GLsizei))TBL()[IDX_glFramebufferTexture2DMultisampleIMG])(target, attachment, textarget, texture, level, samples); }
EXPORT void glApplyFramebufferAttachmentCMAAINTEL(void) { ((void (*)(void))TBL()[IDX_glApplyFramebufferAttachmentCMAAINTEL])(); }
EXPORT void glBeginPerfQueryINTEL(GLuint queryHandle) { ((void (*)(GLuint))TBL()[IDX_glBeginPerfQueryINTEL])(queryHandle); }
EXPORT void glCreatePerfQueryINTEL(GLuint queryId, GLuint * queryHandle) { ((void (*)(GLuint, GLuint *))TBL()[IDX_glCreatePerfQueryINTEL])(queryId, queryHandle); }
EXPORT void glDeletePerfQueryINTEL(GLuint queryHandle) { ((void (*)(GLuint))TBL()[IDX_glDeletePerfQueryINTEL])(queryHandle); }
EXPORT void glEndPerfQueryINTEL(GLuint queryHandle) { ((void (*)(GLuint))TBL()[IDX_glEndPerfQueryINTEL])(queryHandle); }
EXPORT void glGetFirstPerfQueryIdINTEL(GLuint * queryId) { ((void (*)(GLuint *))TBL()[IDX_glGetFirstPerfQueryIdINTEL])(queryId); }
EXPORT void glGetNextPerfQueryIdINTEL(GLuint queryId, GLuint * nextQueryId) { ((void (*)(GLuint, GLuint *))TBL()[IDX_glGetNextPerfQueryIdINTEL])(queryId, nextQueryId); }
EXPORT void glGetPerfCounterInfoINTEL(GLuint queryId, GLuint counterId, GLuint counterNameLength, GLchar * counterName, GLuint counterDescLength, GLchar * counterDesc, GLuint * counterOffset, GLuint * counterDataSize, GLuint * counterTypeEnum, GLuint * counterDataTypeEnum, GLuint64 * rawCounterMaxValue) { ((void (*)(GLuint, GLuint, GLuint, GLchar *, GLuint, GLchar *, GLuint *, GLuint *, GLuint *, GLuint *, GLuint64 *))TBL()[IDX_glGetPerfCounterInfoINTEL])(queryId, counterId, counterNameLength, counterName, counterDescLength, counterDesc, counterOffset, counterDataSize, counterTypeEnum, counterDataTypeEnum, rawCounterMaxValue); }
EXPORT void glGetPerfQueryDataINTEL(GLuint queryHandle, GLuint flags, GLsizei dataSize, void * data, GLuint * bytesWritten) { ((void (*)(GLuint, GLuint, GLsizei, void *, GLuint *))TBL()[IDX_glGetPerfQueryDataINTEL])(queryHandle, flags, dataSize, data, bytesWritten); }
EXPORT void glGetPerfQueryIdByNameINTEL(GLchar * queryName, GLuint * queryId) { ((void (*)(GLchar *, GLuint *))TBL()[IDX_glGetPerfQueryIdByNameINTEL])(queryName, queryId); }
EXPORT void glGetPerfQueryInfoINTEL(GLuint queryId, GLuint queryNameLength, GLchar * queryName, GLuint * dataSize, GLuint * noCounters, GLuint * noInstances, GLuint * capsMask) { ((void (*)(GLuint, GLuint, GLchar *, GLuint *, GLuint *, GLuint *, GLuint *))TBL()[IDX_glGetPerfQueryInfoINTEL])(queryId, queryNameLength, queryName, dataSize, noCounters, noInstances, capsMask); }
EXPORT GLuint64 glGetTextureHandleNV(GLuint texture) { return ((GLuint64 (*)(GLuint))TBL()[IDX_glGetTextureHandleNV])(texture); }
EXPORT GLuint64 glGetTextureSamplerHandleNV(GLuint texture, GLuint sampler) { return ((GLuint64 (*)(GLuint, GLuint))TBL()[IDX_glGetTextureSamplerHandleNV])(texture, sampler); }
EXPORT void glMakeTextureHandleResidentNV(GLuint64 handle) { ((void (*)(GLuint64))TBL()[IDX_glMakeTextureHandleResidentNV])(handle); }
EXPORT void glMakeTextureHandleNonResidentNV(GLuint64 handle) { ((void (*)(GLuint64))TBL()[IDX_glMakeTextureHandleNonResidentNV])(handle); }
EXPORT GLuint64 glGetImageHandleNV(GLuint texture, GLint level, GLboolean layered, GLint layer, GLenum format) { return ((GLuint64 (*)(GLuint, GLint, GLboolean, GLint, GLenum))TBL()[IDX_glGetImageHandleNV])(texture, level, layered, layer, format); }
EXPORT void glMakeImageHandleResidentNV(GLuint64 handle, GLenum access) { ((void (*)(GLuint64, GLenum))TBL()[IDX_glMakeImageHandleResidentNV])(handle, access); }
EXPORT void glMakeImageHandleNonResidentNV(GLuint64 handle) { ((void (*)(GLuint64))TBL()[IDX_glMakeImageHandleNonResidentNV])(handle); }
EXPORT void glUniformHandleui64NV(GLint location, GLuint64 value) { ((void (*)(GLint, GLuint64))TBL()[IDX_glUniformHandleui64NV])(location, value); }
EXPORT void glUniformHandleui64vNV(GLint location, GLsizei count, const GLuint64 * value) { ((void (*)(GLint, GLsizei, const GLuint64 *))TBL()[IDX_glUniformHandleui64vNV])(location, count, value); }
EXPORT void glProgramUniformHandleui64NV(GLuint program, GLint location, GLuint64 value) { ((void (*)(GLuint, GLint, GLuint64))TBL()[IDX_glProgramUniformHandleui64NV])(program, location, value); }
EXPORT void glProgramUniformHandleui64vNV(GLuint program, GLint location, GLsizei count, const GLuint64 * values) { ((void (*)(GLuint, GLint, GLsizei, const GLuint64 *))TBL()[IDX_glProgramUniformHandleui64vNV])(program, location, count, values); }
EXPORT GLboolean glIsTextureHandleResidentNV(GLuint64 handle) { return ((GLboolean (*)(GLuint64))TBL()[IDX_glIsTextureHandleResidentNV])(handle); }
EXPORT GLboolean glIsImageHandleResidentNV(GLuint64 handle) { return ((GLboolean (*)(GLuint64))TBL()[IDX_glIsImageHandleResidentNV])(handle); }
EXPORT void glBlendParameteriNV(GLenum pname, GLint value) { ((void (*)(GLenum, GLint))TBL()[IDX_glBlendParameteriNV])(pname, value); }
EXPORT void glBlendBarrierNV(void) { ((void (*)(void))TBL()[IDX_glBlendBarrierNV])(); }
EXPORT void glViewportPositionWScaleNV(GLuint index, GLfloat xcoeff, GLfloat ycoeff) { ((void (*)(GLuint, GLfloat, GLfloat))TBL()[IDX_glViewportPositionWScaleNV])(index, xcoeff, ycoeff); }
EXPORT void glBeginConditionalRenderNV(GLuint id, GLenum mode) { ((void (*)(GLuint, GLenum))TBL()[IDX_glBeginConditionalRenderNV])(id, mode); }
EXPORT void glEndConditionalRenderNV(void) { ((void (*)(void))TBL()[IDX_glEndConditionalRenderNV])(); }
EXPORT void glSubpixelPrecisionBiasNV(GLuint xbits, GLuint ybits) { ((void (*)(GLuint, GLuint))TBL()[IDX_glSubpixelPrecisionBiasNV])(xbits, ybits); }
EXPORT void glConservativeRasterParameteriNV(GLenum pname, GLint param) { ((void (*)(GLenum, GLint))TBL()[IDX_glConservativeRasterParameteriNV])(pname, param); }
EXPORT void glCopyBufferSubDataNV(GLenum readTarget, GLenum writeTarget, GLintptr readOffset, GLintptr writeOffset, GLsizeiptr size) { ((void (*)(GLenum, GLenum, GLintptr, GLintptr, GLsizeiptr))TBL()[IDX_glCopyBufferSubDataNV])(readTarget, writeTarget, readOffset, writeOffset, size); }
EXPORT void glCoverageMaskNV(GLboolean mask) { ((void (*)(GLboolean))TBL()[IDX_glCoverageMaskNV])(mask); }
EXPORT void glCoverageOperationNV(GLenum operation) { ((void (*)(GLenum))TBL()[IDX_glCoverageOperationNV])(operation); }
EXPORT void glDrawBuffersNV(GLsizei n, const GLenum * bufs) { ((void (*)(GLsizei, const GLenum *))TBL()[IDX_glDrawBuffersNV])(n, bufs); }
EXPORT void glDrawArraysInstancedNV(GLenum mode, GLint first, GLsizei count, GLsizei primcount) { ((void (*)(GLenum, GLint, GLsizei, GLsizei))TBL()[IDX_glDrawArraysInstancedNV])(mode, first, count, primcount); }
EXPORT void glDrawElementsInstancedNV(GLenum mode, GLsizei count, GLenum type, const void * indices, GLsizei primcount) { ((void (*)(GLenum, GLsizei, GLenum, const void *, GLsizei))TBL()[IDX_glDrawElementsInstancedNV])(mode, count, type, indices, primcount); }
EXPORT void glDrawVkImageNV(GLuint64 vkImage, GLuint sampler, GLfloat x0, GLfloat y0, GLfloat x1, GLfloat y1, GLfloat z, GLfloat s0, GLfloat t0, GLfloat s1, GLfloat t1) { ((void (*)(GLuint64, GLuint, GLfloat, GLfloat, GLfloat, GLfloat, GLfloat, GLfloat, GLfloat, GLfloat, GLfloat))TBL()[IDX_glDrawVkImageNV])(vkImage, sampler, x0, y0, x1, y1, z, s0, t0, s1, t1); }
EXPORT GLVULKANPROCNV glGetVkProcAddrNV(const GLchar * name) { return ((GLVULKANPROCNV (*)(const GLchar *))TBL()[IDX_glGetVkProcAddrNV])(name); }
EXPORT void glWaitVkSemaphoreNV(GLuint64 vkSemaphore) { ((void (*)(GLuint64))TBL()[IDX_glWaitVkSemaphoreNV])(vkSemaphore); }
EXPORT void glSignalVkSemaphoreNV(GLuint64 vkSemaphore) { ((void (*)(GLuint64))TBL()[IDX_glSignalVkSemaphoreNV])(vkSemaphore); }
EXPORT void glSignalVkFenceNV(GLuint64 vkFence) { ((void (*)(GLuint64))TBL()[IDX_glSignalVkFenceNV])(vkFence); }
EXPORT void glDeleteFencesNV(GLsizei n, const GLuint * fences) { ((void (*)(GLsizei, const GLuint *))TBL()[IDX_glDeleteFencesNV])(n, fences); }
EXPORT void glGenFencesNV(GLsizei n, GLuint * fences) { ((void (*)(GLsizei, GLuint *))TBL()[IDX_glGenFencesNV])(n, fences); }
EXPORT GLboolean glIsFenceNV(GLuint fence) { return ((GLboolean (*)(GLuint))TBL()[IDX_glIsFenceNV])(fence); }
EXPORT GLboolean glTestFenceNV(GLuint fence) { return ((GLboolean (*)(GLuint))TBL()[IDX_glTestFenceNV])(fence); }
EXPORT void glGetFenceivNV(GLuint fence, GLenum pname, GLint * params) { ((void (*)(GLuint, GLenum, GLint *))TBL()[IDX_glGetFenceivNV])(fence, pname, params); }
EXPORT void glFinishFenceNV(GLuint fence) { ((void (*)(GLuint))TBL()[IDX_glFinishFenceNV])(fence); }
EXPORT void glSetFenceNV(GLuint fence, GLenum condition) { ((void (*)(GLuint, GLenum))TBL()[IDX_glSetFenceNV])(fence, condition); }
EXPORT void glFragmentCoverageColorNV(GLuint color) { ((void (*)(GLuint))TBL()[IDX_glFragmentCoverageColorNV])(color); }
EXPORT void glBlitFramebufferNV(GLint srcX0, GLint srcY0, GLint srcX1, GLint srcY1, GLint dstX0, GLint dstY0, GLint dstX1, GLint dstY1, GLbitfield mask, GLenum filter) { ((void (*)(GLint, GLint, GLint, GLint, GLint, GLint, GLint, GLint, GLbitfield, GLenum))TBL()[IDX_glBlitFramebufferNV])(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter); }
EXPORT void glCoverageModulationTableNV(GLsizei n, const GLfloat * v) { ((void (*)(GLsizei, const GLfloat *))TBL()[IDX_glCoverageModulationTableNV])(n, v); }
EXPORT void glGetCoverageModulationTableNV(GLsizei bufsize, GLfloat * v) { ((void (*)(GLsizei, GLfloat *))TBL()[IDX_glGetCoverageModulationTableNV])(bufsize, v); }
EXPORT void glCoverageModulationNV(GLenum components) { ((void (*)(GLenum))TBL()[IDX_glCoverageModulationNV])(components); }
EXPORT void glRenderbufferStorageMultisampleNV(GLenum target, GLsizei samples, GLenum internalformat, GLsizei width, GLsizei height) { ((void (*)(GLenum, GLsizei, GLenum, GLsizei, GLsizei))TBL()[IDX_glRenderbufferStorageMultisampleNV])(target, samples, internalformat, width, height); }
EXPORT void glUniform1i64NV(GLint location, GLint64EXT x) { ((void (*)(GLint, GLint64EXT))TBL()[IDX_glUniform1i64NV])(location, x); }
EXPORT void glUniform2i64NV(GLint location, GLint64EXT x, GLint64EXT y) { ((void (*)(GLint, GLint64EXT, GLint64EXT))TBL()[IDX_glUniform2i64NV])(location, x, y); }
EXPORT void glUniform3i64NV(GLint location, GLint64EXT x, GLint64EXT y, GLint64EXT z) { ((void (*)(GLint, GLint64EXT, GLint64EXT, GLint64EXT))TBL()[IDX_glUniform3i64NV])(location, x, y, z); }
EXPORT void glUniform4i64NV(GLint location, GLint64EXT x, GLint64EXT y, GLint64EXT z, GLint64EXT w) { ((void (*)(GLint, GLint64EXT, GLint64EXT, GLint64EXT, GLint64EXT))TBL()[IDX_glUniform4i64NV])(location, x, y, z, w); }
EXPORT void glUniform1i64vNV(GLint location, GLsizei count, const GLint64EXT * value) { ((void (*)(GLint, GLsizei, const GLint64EXT *))TBL()[IDX_glUniform1i64vNV])(location, count, value); }
EXPORT void glUniform2i64vNV(GLint location, GLsizei count, const GLint64EXT * value) { ((void (*)(GLint, GLsizei, const GLint64EXT *))TBL()[IDX_glUniform2i64vNV])(location, count, value); }
EXPORT void glUniform3i64vNV(GLint location, GLsizei count, const GLint64EXT * value) { ((void (*)(GLint, GLsizei, const GLint64EXT *))TBL()[IDX_glUniform3i64vNV])(location, count, value); }
EXPORT void glUniform4i64vNV(GLint location, GLsizei count, const GLint64EXT * value) { ((void (*)(GLint, GLsizei, const GLint64EXT *))TBL()[IDX_glUniform4i64vNV])(location, count, value); }
EXPORT void glUniform1ui64NV(GLint location, GLuint64EXT x) { ((void (*)(GLint, GLuint64EXT))TBL()[IDX_glUniform1ui64NV])(location, x); }
EXPORT void glUniform2ui64NV(GLint location, GLuint64EXT x, GLuint64EXT y) { ((void (*)(GLint, GLuint64EXT, GLuint64EXT))TBL()[IDX_glUniform2ui64NV])(location, x, y); }
EXPORT void glUniform3ui64NV(GLint location, GLuint64EXT x, GLuint64EXT y, GLuint64EXT z) { ((void (*)(GLint, GLuint64EXT, GLuint64EXT, GLuint64EXT))TBL()[IDX_glUniform3ui64NV])(location, x, y, z); }
EXPORT void glUniform4ui64NV(GLint location, GLuint64EXT x, GLuint64EXT y, GLuint64EXT z, GLuint64EXT w) { ((void (*)(GLint, GLuint64EXT, GLuint64EXT, GLuint64EXT, GLuint64EXT))TBL()[IDX_glUniform4ui64NV])(location, x, y, z, w); }
EXPORT void glUniform1ui64vNV(GLint location, GLsizei count, const GLuint64EXT * value) { ((void (*)(GLint, GLsizei, const GLuint64EXT *))TBL()[IDX_glUniform1ui64vNV])(location, count, value); }
EXPORT void glUniform2ui64vNV(GLint location, GLsizei count, const GLuint64EXT * value) { ((void (*)(GLint, GLsizei, const GLuint64EXT *))TBL()[IDX_glUniform2ui64vNV])(location, count, value); }
EXPORT void glUniform3ui64vNV(GLint location, GLsizei count, const GLuint64EXT * value) { ((void (*)(GLint, GLsizei, const GLuint64EXT *))TBL()[IDX_glUniform3ui64vNV])(location, count, value); }
EXPORT void glUniform4ui64vNV(GLint location, GLsizei count, const GLuint64EXT * value) { ((void (*)(GLint, GLsizei, const GLuint64EXT *))TBL()[IDX_glUniform4ui64vNV])(location, count, value); }
EXPORT void glGetUniformi64vNV(GLuint program, GLint location, GLint64EXT * params) { ((void (*)(GLuint, GLint, GLint64EXT *))TBL()[IDX_glGetUniformi64vNV])(program, location, params); }
EXPORT void glProgramUniform1i64NV(GLuint program, GLint location, GLint64EXT x) { ((void (*)(GLuint, GLint, GLint64EXT))TBL()[IDX_glProgramUniform1i64NV])(program, location, x); }
EXPORT void glProgramUniform2i64NV(GLuint program, GLint location, GLint64EXT x, GLint64EXT y) { ((void (*)(GLuint, GLint, GLint64EXT, GLint64EXT))TBL()[IDX_glProgramUniform2i64NV])(program, location, x, y); }
EXPORT void glProgramUniform3i64NV(GLuint program, GLint location, GLint64EXT x, GLint64EXT y, GLint64EXT z) { ((void (*)(GLuint, GLint, GLint64EXT, GLint64EXT, GLint64EXT))TBL()[IDX_glProgramUniform3i64NV])(program, location, x, y, z); }
EXPORT void glProgramUniform4i64NV(GLuint program, GLint location, GLint64EXT x, GLint64EXT y, GLint64EXT z, GLint64EXT w) { ((void (*)(GLuint, GLint, GLint64EXT, GLint64EXT, GLint64EXT, GLint64EXT))TBL()[IDX_glProgramUniform4i64NV])(program, location, x, y, z, w); }
EXPORT void glProgramUniform1i64vNV(GLuint program, GLint location, GLsizei count, const GLint64EXT * value) { ((void (*)(GLuint, GLint, GLsizei, const GLint64EXT *))TBL()[IDX_glProgramUniform1i64vNV])(program, location, count, value); }
EXPORT void glProgramUniform2i64vNV(GLuint program, GLint location, GLsizei count, const GLint64EXT * value) { ((void (*)(GLuint, GLint, GLsizei, const GLint64EXT *))TBL()[IDX_glProgramUniform2i64vNV])(program, location, count, value); }
EXPORT void glProgramUniform3i64vNV(GLuint program, GLint location, GLsizei count, const GLint64EXT * value) { ((void (*)(GLuint, GLint, GLsizei, const GLint64EXT *))TBL()[IDX_glProgramUniform3i64vNV])(program, location, count, value); }
EXPORT void glProgramUniform4i64vNV(GLuint program, GLint location, GLsizei count, const GLint64EXT * value) { ((void (*)(GLuint, GLint, GLsizei, const GLint64EXT *))TBL()[IDX_glProgramUniform4i64vNV])(program, location, count, value); }
EXPORT void glProgramUniform1ui64NV(GLuint program, GLint location, GLuint64EXT x) { ((void (*)(GLuint, GLint, GLuint64EXT))TBL()[IDX_glProgramUniform1ui64NV])(program, location, x); }
EXPORT void glProgramUniform2ui64NV(GLuint program, GLint location, GLuint64EXT x, GLuint64EXT y) { ((void (*)(GLuint, GLint, GLuint64EXT, GLuint64EXT))TBL()[IDX_glProgramUniform2ui64NV])(program, location, x, y); }
EXPORT void glProgramUniform3ui64NV(GLuint program, GLint location, GLuint64EXT x, GLuint64EXT y, GLuint64EXT z) { ((void (*)(GLuint, GLint, GLuint64EXT, GLuint64EXT, GLuint64EXT))TBL()[IDX_glProgramUniform3ui64NV])(program, location, x, y, z); }
EXPORT void glProgramUniform4ui64NV(GLuint program, GLint location, GLuint64EXT x, GLuint64EXT y, GLuint64EXT z, GLuint64EXT w) { ((void (*)(GLuint, GLint, GLuint64EXT, GLuint64EXT, GLuint64EXT, GLuint64EXT))TBL()[IDX_glProgramUniform4ui64NV])(program, location, x, y, z, w); }
EXPORT void glProgramUniform1ui64vNV(GLuint program, GLint location, GLsizei count, const GLuint64EXT * value) { ((void (*)(GLuint, GLint, GLsizei, const GLuint64EXT *))TBL()[IDX_glProgramUniform1ui64vNV])(program, location, count, value); }
EXPORT void glProgramUniform2ui64vNV(GLuint program, GLint location, GLsizei count, const GLuint64EXT * value) { ((void (*)(GLuint, GLint, GLsizei, const GLuint64EXT *))TBL()[IDX_glProgramUniform2ui64vNV])(program, location, count, value); }
EXPORT void glProgramUniform3ui64vNV(GLuint program, GLint location, GLsizei count, const GLuint64EXT * value) { ((void (*)(GLuint, GLint, GLsizei, const GLuint64EXT *))TBL()[IDX_glProgramUniform3ui64vNV])(program, location, count, value); }
EXPORT void glProgramUniform4ui64vNV(GLuint program, GLint location, GLsizei count, const GLuint64EXT * value) { ((void (*)(GLuint, GLint, GLsizei, const GLuint64EXT *))TBL()[IDX_glProgramUniform4ui64vNV])(program, location, count, value); }
EXPORT void glVertexAttribDivisorNV(GLuint index, GLuint divisor) { ((void (*)(GLuint, GLuint))TBL()[IDX_glVertexAttribDivisorNV])(index, divisor); }
EXPORT void glGetInternalformatSampleivNV(GLenum target, GLenum internalformat, GLsizei samples, GLenum pname, GLsizei bufSize, GLint * params) { ((void (*)(GLenum, GLenum, GLsizei, GLenum, GLsizei, GLint *))TBL()[IDX_glGetInternalformatSampleivNV])(target, internalformat, samples, pname, bufSize, params); }
EXPORT void glUniformMatrix2x3fvNV(GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { ((void (*)(GLint, GLsizei, GLboolean, const GLfloat *))TBL()[IDX_glUniformMatrix2x3fvNV])(location, count, transpose, value); }
EXPORT void glUniformMatrix3x2fvNV(GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { ((void (*)(GLint, GLsizei, GLboolean, const GLfloat *))TBL()[IDX_glUniformMatrix3x2fvNV])(location, count, transpose, value); }
EXPORT void glUniformMatrix2x4fvNV(GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { ((void (*)(GLint, GLsizei, GLboolean, const GLfloat *))TBL()[IDX_glUniformMatrix2x4fvNV])(location, count, transpose, value); }
EXPORT void glUniformMatrix4x2fvNV(GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { ((void (*)(GLint, GLsizei, GLboolean, const GLfloat *))TBL()[IDX_glUniformMatrix4x2fvNV])(location, count, transpose, value); }
EXPORT void glUniformMatrix3x4fvNV(GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { ((void (*)(GLint, GLsizei, GLboolean, const GLfloat *))TBL()[IDX_glUniformMatrix3x4fvNV])(location, count, transpose, value); }
EXPORT void glUniformMatrix4x3fvNV(GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { ((void (*)(GLint, GLsizei, GLboolean, const GLfloat *))TBL()[IDX_glUniformMatrix4x3fvNV])(location, count, transpose, value); }
EXPORT GLuint glGenPathsNV(GLsizei range) { return ((GLuint (*)(GLsizei))TBL()[IDX_glGenPathsNV])(range); }
EXPORT void glDeletePathsNV(GLuint path, GLsizei range) { ((void (*)(GLuint, GLsizei))TBL()[IDX_glDeletePathsNV])(path, range); }
EXPORT GLboolean glIsPathNV(GLuint path) { return ((GLboolean (*)(GLuint))TBL()[IDX_glIsPathNV])(path); }
EXPORT void glPathCommandsNV(GLuint path, GLsizei numCommands, const GLubyte * commands, GLsizei numCoords, GLenum coordType, const void * coords) { ((void (*)(GLuint, GLsizei, const GLubyte *, GLsizei, GLenum, const void *))TBL()[IDX_glPathCommandsNV])(path, numCommands, commands, numCoords, coordType, coords); }
EXPORT void glPathCoordsNV(GLuint path, GLsizei numCoords, GLenum coordType, const void * coords) { ((void (*)(GLuint, GLsizei, GLenum, const void *))TBL()[IDX_glPathCoordsNV])(path, numCoords, coordType, coords); }
EXPORT void glPathSubCommandsNV(GLuint path, GLsizei commandStart, GLsizei commandsToDelete, GLsizei numCommands, const GLubyte * commands, GLsizei numCoords, GLenum coordType, const void * coords) { ((void (*)(GLuint, GLsizei, GLsizei, GLsizei, const GLubyte *, GLsizei, GLenum, const void *))TBL()[IDX_glPathSubCommandsNV])(path, commandStart, commandsToDelete, numCommands, commands, numCoords, coordType, coords); }
EXPORT void glPathSubCoordsNV(GLuint path, GLsizei coordStart, GLsizei numCoords, GLenum coordType, const void * coords) { ((void (*)(GLuint, GLsizei, GLsizei, GLenum, const void *))TBL()[IDX_glPathSubCoordsNV])(path, coordStart, numCoords, coordType, coords); }
EXPORT void glPathStringNV(GLuint path, GLenum format, GLsizei length, const void * pathString) { ((void (*)(GLuint, GLenum, GLsizei, const void *))TBL()[IDX_glPathStringNV])(path, format, length, pathString); }
EXPORT void glPathGlyphsNV(GLuint firstPathName, GLenum fontTarget, const void * fontName, GLbitfield fontStyle, GLsizei numGlyphs, GLenum type, const void * charcodes, GLenum handleMissingGlyphs, GLuint pathParameterTemplate, GLfloat emScale) { ((void (*)(GLuint, GLenum, const void *, GLbitfield, GLsizei, GLenum, const void *, GLenum, GLuint, GLfloat))TBL()[IDX_glPathGlyphsNV])(firstPathName, fontTarget, fontName, fontStyle, numGlyphs, type, charcodes, handleMissingGlyphs, pathParameterTemplate, emScale); }
EXPORT void glPathGlyphRangeNV(GLuint firstPathName, GLenum fontTarget, const void * fontName, GLbitfield fontStyle, GLuint firstGlyph, GLsizei numGlyphs, GLenum handleMissingGlyphs, GLuint pathParameterTemplate, GLfloat emScale) { ((void (*)(GLuint, GLenum, const void *, GLbitfield, GLuint, GLsizei, GLenum, GLuint, GLfloat))TBL()[IDX_glPathGlyphRangeNV])(firstPathName, fontTarget, fontName, fontStyle, firstGlyph, numGlyphs, handleMissingGlyphs, pathParameterTemplate, emScale); }
EXPORT void glWeightPathsNV(GLuint resultPath, GLsizei numPaths, const GLuint * paths, const GLfloat * weights) { ((void (*)(GLuint, GLsizei, const GLuint *, const GLfloat *))TBL()[IDX_glWeightPathsNV])(resultPath, numPaths, paths, weights); }
EXPORT void glCopyPathNV(GLuint resultPath, GLuint srcPath) { ((void (*)(GLuint, GLuint))TBL()[IDX_glCopyPathNV])(resultPath, srcPath); }
EXPORT void glInterpolatePathsNV(GLuint resultPath, GLuint pathA, GLuint pathB, GLfloat weight) { ((void (*)(GLuint, GLuint, GLuint, GLfloat))TBL()[IDX_glInterpolatePathsNV])(resultPath, pathA, pathB, weight); }
EXPORT void glTransformPathNV(GLuint resultPath, GLuint srcPath, GLenum transformType, const GLfloat * transformValues) { ((void (*)(GLuint, GLuint, GLenum, const GLfloat *))TBL()[IDX_glTransformPathNV])(resultPath, srcPath, transformType, transformValues); }
EXPORT void glPathParameterivNV(GLuint path, GLenum pname, const GLint * value) { ((void (*)(GLuint, GLenum, const GLint *))TBL()[IDX_glPathParameterivNV])(path, pname, value); }
EXPORT void glPathParameteriNV(GLuint path, GLenum pname, GLint value) { ((void (*)(GLuint, GLenum, GLint))TBL()[IDX_glPathParameteriNV])(path, pname, value); }
EXPORT void glPathParameterfvNV(GLuint path, GLenum pname, const GLfloat * value) { ((void (*)(GLuint, GLenum, const GLfloat *))TBL()[IDX_glPathParameterfvNV])(path, pname, value); }
EXPORT void glPathParameterfNV(GLuint path, GLenum pname, GLfloat value) { ((void (*)(GLuint, GLenum, GLfloat))TBL()[IDX_glPathParameterfNV])(path, pname, value); }
EXPORT void glPathDashArrayNV(GLuint path, GLsizei dashCount, const GLfloat * dashArray) { ((void (*)(GLuint, GLsizei, const GLfloat *))TBL()[IDX_glPathDashArrayNV])(path, dashCount, dashArray); }
EXPORT void glPathStencilFuncNV(GLenum func, GLint ref, GLuint mask) { ((void (*)(GLenum, GLint, GLuint))TBL()[IDX_glPathStencilFuncNV])(func, ref, mask); }
EXPORT void glPathStencilDepthOffsetNV(GLfloat factor, GLfloat units) { ((void (*)(GLfloat, GLfloat))TBL()[IDX_glPathStencilDepthOffsetNV])(factor, units); }
EXPORT void glStencilFillPathNV(GLuint path, GLenum fillMode, GLuint mask) { ((void (*)(GLuint, GLenum, GLuint))TBL()[IDX_glStencilFillPathNV])(path, fillMode, mask); }
EXPORT void glStencilStrokePathNV(GLuint path, GLint reference, GLuint mask) { ((void (*)(GLuint, GLint, GLuint))TBL()[IDX_glStencilStrokePathNV])(path, reference, mask); }
EXPORT void glStencilFillPathInstancedNV(GLsizei numPaths, GLenum pathNameType, const void * paths, GLuint pathBase, GLenum fillMode, GLuint mask, GLenum transformType, const GLfloat * transformValues) { ((void (*)(GLsizei, GLenum, const void *, GLuint, GLenum, GLuint, GLenum, const GLfloat *))TBL()[IDX_glStencilFillPathInstancedNV])(numPaths, pathNameType, paths, pathBase, fillMode, mask, transformType, transformValues); }
EXPORT void glStencilStrokePathInstancedNV(GLsizei numPaths, GLenum pathNameType, const void * paths, GLuint pathBase, GLint reference, GLuint mask, GLenum transformType, const GLfloat * transformValues) { ((void (*)(GLsizei, GLenum, const void *, GLuint, GLint, GLuint, GLenum, const GLfloat *))TBL()[IDX_glStencilStrokePathInstancedNV])(numPaths, pathNameType, paths, pathBase, reference, mask, transformType, transformValues); }
EXPORT void glPathCoverDepthFuncNV(GLenum func) { ((void (*)(GLenum))TBL()[IDX_glPathCoverDepthFuncNV])(func); }
EXPORT void glCoverFillPathNV(GLuint path, GLenum coverMode) { ((void (*)(GLuint, GLenum))TBL()[IDX_glCoverFillPathNV])(path, coverMode); }
EXPORT void glCoverStrokePathNV(GLuint path, GLenum coverMode) { ((void (*)(GLuint, GLenum))TBL()[IDX_glCoverStrokePathNV])(path, coverMode); }
EXPORT void glCoverFillPathInstancedNV(GLsizei numPaths, GLenum pathNameType, const void * paths, GLuint pathBase, GLenum coverMode, GLenum transformType, const GLfloat * transformValues) { ((void (*)(GLsizei, GLenum, const void *, GLuint, GLenum, GLenum, const GLfloat *))TBL()[IDX_glCoverFillPathInstancedNV])(numPaths, pathNameType, paths, pathBase, coverMode, transformType, transformValues); }
EXPORT void glCoverStrokePathInstancedNV(GLsizei numPaths, GLenum pathNameType, const void * paths, GLuint pathBase, GLenum coverMode, GLenum transformType, const GLfloat * transformValues) { ((void (*)(GLsizei, GLenum, const void *, GLuint, GLenum, GLenum, const GLfloat *))TBL()[IDX_glCoverStrokePathInstancedNV])(numPaths, pathNameType, paths, pathBase, coverMode, transformType, transformValues); }
EXPORT void glGetPathParameterivNV(GLuint path, GLenum pname, GLint * value) { ((void (*)(GLuint, GLenum, GLint *))TBL()[IDX_glGetPathParameterivNV])(path, pname, value); }
EXPORT void glGetPathParameterfvNV(GLuint path, GLenum pname, GLfloat * value) { ((void (*)(GLuint, GLenum, GLfloat *))TBL()[IDX_glGetPathParameterfvNV])(path, pname, value); }
EXPORT void glGetPathCommandsNV(GLuint path, GLubyte * commands) { ((void (*)(GLuint, GLubyte *))TBL()[IDX_glGetPathCommandsNV])(path, commands); }
EXPORT void glGetPathCoordsNV(GLuint path, GLfloat * coords) { ((void (*)(GLuint, GLfloat *))TBL()[IDX_glGetPathCoordsNV])(path, coords); }
EXPORT void glGetPathDashArrayNV(GLuint path, GLfloat * dashArray) { ((void (*)(GLuint, GLfloat *))TBL()[IDX_glGetPathDashArrayNV])(path, dashArray); }
EXPORT void glGetPathMetricsNV(GLbitfield metricQueryMask, GLsizei numPaths, GLenum pathNameType, const void * paths, GLuint pathBase, GLsizei stride, GLfloat * metrics) { ((void (*)(GLbitfield, GLsizei, GLenum, const void *, GLuint, GLsizei, GLfloat *))TBL()[IDX_glGetPathMetricsNV])(metricQueryMask, numPaths, pathNameType, paths, pathBase, stride, metrics); }
EXPORT void glGetPathMetricRangeNV(GLbitfield metricQueryMask, GLuint firstPathName, GLsizei numPaths, GLsizei stride, GLfloat * metrics) { ((void (*)(GLbitfield, GLuint, GLsizei, GLsizei, GLfloat *))TBL()[IDX_glGetPathMetricRangeNV])(metricQueryMask, firstPathName, numPaths, stride, metrics); }
EXPORT void glGetPathSpacingNV(GLenum pathListMode, GLsizei numPaths, GLenum pathNameType, const void * paths, GLuint pathBase, GLfloat advanceScale, GLfloat kerningScale, GLenum transformType, GLfloat * returnedSpacing) { ((void (*)(GLenum, GLsizei, GLenum, const void *, GLuint, GLfloat, GLfloat, GLenum, GLfloat *))TBL()[IDX_glGetPathSpacingNV])(pathListMode, numPaths, pathNameType, paths, pathBase, advanceScale, kerningScale, transformType, returnedSpacing); }
EXPORT GLboolean glIsPointInFillPathNV(GLuint path, GLuint mask, GLfloat x, GLfloat y) { return ((GLboolean (*)(GLuint, GLuint, GLfloat, GLfloat))TBL()[IDX_glIsPointInFillPathNV])(path, mask, x, y); }
EXPORT GLboolean glIsPointInStrokePathNV(GLuint path, GLfloat x, GLfloat y) { return ((GLboolean (*)(GLuint, GLfloat, GLfloat))TBL()[IDX_glIsPointInStrokePathNV])(path, x, y); }
EXPORT GLfloat glGetPathLengthNV(GLuint path, GLsizei startSegment, GLsizei numSegments) { return ((GLfloat (*)(GLuint, GLsizei, GLsizei))TBL()[IDX_glGetPathLengthNV])(path, startSegment, numSegments); }
EXPORT GLboolean glPointAlongPathNV(GLuint path, GLsizei startSegment, GLsizei numSegments, GLfloat distance, GLfloat * x, GLfloat * y, GLfloat * tangentX, GLfloat * tangentY) { return ((GLboolean (*)(GLuint, GLsizei, GLsizei, GLfloat, GLfloat *, GLfloat *, GLfloat *, GLfloat *))TBL()[IDX_glPointAlongPathNV])(path, startSegment, numSegments, distance, x, y, tangentX, tangentY); }
EXPORT void glMatrixLoad3x2fNV(GLenum matrixMode, const GLfloat * m) { ((void (*)(GLenum, const GLfloat *))TBL()[IDX_glMatrixLoad3x2fNV])(matrixMode, m); }
EXPORT void glMatrixLoad3x3fNV(GLenum matrixMode, const GLfloat * m) { ((void (*)(GLenum, const GLfloat *))TBL()[IDX_glMatrixLoad3x3fNV])(matrixMode, m); }
EXPORT void glMatrixLoadTranspose3x3fNV(GLenum matrixMode, const GLfloat * m) { ((void (*)(GLenum, const GLfloat *))TBL()[IDX_glMatrixLoadTranspose3x3fNV])(matrixMode, m); }
EXPORT void glMatrixMult3x2fNV(GLenum matrixMode, const GLfloat * m) { ((void (*)(GLenum, const GLfloat *))TBL()[IDX_glMatrixMult3x2fNV])(matrixMode, m); }
EXPORT void glMatrixMult3x3fNV(GLenum matrixMode, const GLfloat * m) { ((void (*)(GLenum, const GLfloat *))TBL()[IDX_glMatrixMult3x3fNV])(matrixMode, m); }
EXPORT void glMatrixMultTranspose3x3fNV(GLenum matrixMode, const GLfloat * m) { ((void (*)(GLenum, const GLfloat *))TBL()[IDX_glMatrixMultTranspose3x3fNV])(matrixMode, m); }
EXPORT void glStencilThenCoverFillPathNV(GLuint path, GLenum fillMode, GLuint mask, GLenum coverMode) { ((void (*)(GLuint, GLenum, GLuint, GLenum))TBL()[IDX_glStencilThenCoverFillPathNV])(path, fillMode, mask, coverMode); }
EXPORT void glStencilThenCoverStrokePathNV(GLuint path, GLint reference, GLuint mask, GLenum coverMode) { ((void (*)(GLuint, GLint, GLuint, GLenum))TBL()[IDX_glStencilThenCoverStrokePathNV])(path, reference, mask, coverMode); }
EXPORT void glStencilThenCoverFillPathInstancedNV(GLsizei numPaths, GLenum pathNameType, const void * paths, GLuint pathBase, GLenum fillMode, GLuint mask, GLenum coverMode, GLenum transformType, const GLfloat * transformValues) { ((void (*)(GLsizei, GLenum, const void *, GLuint, GLenum, GLuint, GLenum, GLenum, const GLfloat *))TBL()[IDX_glStencilThenCoverFillPathInstancedNV])(numPaths, pathNameType, paths, pathBase, fillMode, mask, coverMode, transformType, transformValues); }
EXPORT void glStencilThenCoverStrokePathInstancedNV(GLsizei numPaths, GLenum pathNameType, const void * paths, GLuint pathBase, GLint reference, GLuint mask, GLenum coverMode, GLenum transformType, const GLfloat * transformValues) { ((void (*)(GLsizei, GLenum, const void *, GLuint, GLint, GLuint, GLenum, GLenum, const GLfloat *))TBL()[IDX_glStencilThenCoverStrokePathInstancedNV])(numPaths, pathNameType, paths, pathBase, reference, mask, coverMode, transformType, transformValues); }
EXPORT GLenum glPathGlyphIndexArrayNV(GLuint firstPathName, GLenum fontTarget, const void * fontName, GLbitfield fontStyle, GLuint firstGlyphIndex, GLsizei numGlyphs, GLuint pathParameterTemplate, GLfloat emScale) { return ((GLenum (*)(GLuint, GLenum, const void *, GLbitfield, GLuint, GLsizei, GLuint, GLfloat))TBL()[IDX_glPathGlyphIndexArrayNV])(firstPathName, fontTarget, fontName, fontStyle, firstGlyphIndex, numGlyphs, pathParameterTemplate, emScale); }
EXPORT GLenum glPathMemoryGlyphIndexArrayNV(GLuint firstPathName, GLenum fontTarget, GLsizeiptr fontSize, const void * fontData, GLsizei faceIndex, GLuint firstGlyphIndex, GLsizei numGlyphs, GLuint pathParameterTemplate, GLfloat emScale) { return ((GLenum (*)(GLuint, GLenum, GLsizeiptr, const void *, GLsizei, GLuint, GLsizei, GLuint, GLfloat))TBL()[IDX_glPathMemoryGlyphIndexArrayNV])(firstPathName, fontTarget, fontSize, fontData, faceIndex, firstGlyphIndex, numGlyphs, pathParameterTemplate, emScale); }
EXPORT void glProgramPathFragmentInputGenNV(GLuint program, GLint location, GLenum genMode, GLint components, const GLfloat * coeffs) { ((void (*)(GLuint, GLint, GLenum, GLint, const GLfloat *))TBL()[IDX_glProgramPathFragmentInputGenNV])(program, location, genMode, components, coeffs); }
EXPORT void glGetProgramResourcefvNV(GLuint program, GLenum programInterface, GLuint index, GLsizei propCount, const GLenum * props, GLsizei bufSize, GLsizei * length, GLfloat * params) { ((void (*)(GLuint, GLenum, GLuint, GLsizei, const GLenum *, GLsizei, GLsizei *, GLfloat *))TBL()[IDX_glGetProgramResourcefvNV])(program, programInterface, index, propCount, props, bufSize, length, params); }
EXPORT void glMatrixFrustumEXT(GLenum mode, GLdouble left, GLdouble right, GLdouble bottom, GLdouble top, GLdouble zNear, GLdouble zFar) { ((void (*)(GLenum, GLdouble, GLdouble, GLdouble, GLdouble, GLdouble, GLdouble))TBL()[IDX_glMatrixFrustumEXT])(mode, left, right, bottom, top, zNear, zFar); }
EXPORT void glMatrixLoadIdentityEXT(GLenum mode) { ((void (*)(GLenum))TBL()[IDX_glMatrixLoadIdentityEXT])(mode); }
EXPORT void glMatrixLoadTransposefEXT(GLenum mode, const GLfloat * m) { ((void (*)(GLenum, const GLfloat *))TBL()[IDX_glMatrixLoadTransposefEXT])(mode, m); }
EXPORT void glMatrixLoadTransposedEXT(GLenum mode, const GLdouble * m) { ((void (*)(GLenum, const GLdouble *))TBL()[IDX_glMatrixLoadTransposedEXT])(mode, m); }
EXPORT void glMatrixLoadfEXT(GLenum mode, const GLfloat * m) { ((void (*)(GLenum, const GLfloat *))TBL()[IDX_glMatrixLoadfEXT])(mode, m); }
EXPORT void glMatrixLoaddEXT(GLenum mode, const GLdouble * m) { ((void (*)(GLenum, const GLdouble *))TBL()[IDX_glMatrixLoaddEXT])(mode, m); }
EXPORT void glMatrixMultTransposefEXT(GLenum mode, const GLfloat * m) { ((void (*)(GLenum, const GLfloat *))TBL()[IDX_glMatrixMultTransposefEXT])(mode, m); }
EXPORT void glMatrixMultTransposedEXT(GLenum mode, const GLdouble * m) { ((void (*)(GLenum, const GLdouble *))TBL()[IDX_glMatrixMultTransposedEXT])(mode, m); }
EXPORT void glMatrixMultfEXT(GLenum mode, const GLfloat * m) { ((void (*)(GLenum, const GLfloat *))TBL()[IDX_glMatrixMultfEXT])(mode, m); }
EXPORT void glMatrixMultdEXT(GLenum mode, const GLdouble * m) { ((void (*)(GLenum, const GLdouble *))TBL()[IDX_glMatrixMultdEXT])(mode, m); }
EXPORT void glMatrixOrthoEXT(GLenum mode, GLdouble left, GLdouble right, GLdouble bottom, GLdouble top, GLdouble zNear, GLdouble zFar) { ((void (*)(GLenum, GLdouble, GLdouble, GLdouble, GLdouble, GLdouble, GLdouble))TBL()[IDX_glMatrixOrthoEXT])(mode, left, right, bottom, top, zNear, zFar); }
EXPORT void glMatrixPopEXT(GLenum mode) { ((void (*)(GLenum))TBL()[IDX_glMatrixPopEXT])(mode); }
EXPORT void glMatrixPushEXT(GLenum mode) { ((void (*)(GLenum))TBL()[IDX_glMatrixPushEXT])(mode); }
EXPORT void glMatrixRotatefEXT(GLenum mode, GLfloat angle, GLfloat x, GLfloat y, GLfloat z) { ((void (*)(GLenum, GLfloat, GLfloat, GLfloat, GLfloat))TBL()[IDX_glMatrixRotatefEXT])(mode, angle, x, y, z); }
EXPORT void glMatrixRotatedEXT(GLenum mode, GLdouble angle, GLdouble x, GLdouble y, GLdouble z) { ((void (*)(GLenum, GLdouble, GLdouble, GLdouble, GLdouble))TBL()[IDX_glMatrixRotatedEXT])(mode, angle, x, y, z); }
EXPORT void glMatrixScalefEXT(GLenum mode, GLfloat x, GLfloat y, GLfloat z) { ((void (*)(GLenum, GLfloat, GLfloat, GLfloat))TBL()[IDX_glMatrixScalefEXT])(mode, x, y, z); }
EXPORT void glMatrixScaledEXT(GLenum mode, GLdouble x, GLdouble y, GLdouble z) { ((void (*)(GLenum, GLdouble, GLdouble, GLdouble))TBL()[IDX_glMatrixScaledEXT])(mode, x, y, z); }
EXPORT void glMatrixTranslatefEXT(GLenum mode, GLfloat x, GLfloat y, GLfloat z) { ((void (*)(GLenum, GLfloat, GLfloat, GLfloat))TBL()[IDX_glMatrixTranslatefEXT])(mode, x, y, z); }
EXPORT void glMatrixTranslatedEXT(GLenum mode, GLdouble x, GLdouble y, GLdouble z) { ((void (*)(GLenum, GLdouble, GLdouble, GLdouble))TBL()[IDX_glMatrixTranslatedEXT])(mode, x, y, z); }
EXPORT void glPolygonModeNV(GLenum face, GLenum mode) { ((void (*)(GLenum, GLenum))TBL()[IDX_glPolygonModeNV])(face, mode); }
EXPORT void glReadBufferNV(GLenum mode) { ((void (*)(GLenum))TBL()[IDX_glReadBufferNV])(mode); }
EXPORT void glFramebufferSampleLocationsfvNV(GLenum target, GLuint start, GLsizei count, const GLfloat * v) { ((void (*)(GLenum, GLuint, GLsizei, const GLfloat *))TBL()[IDX_glFramebufferSampleLocationsfvNV])(target, start, count, v); }
EXPORT void glNamedFramebufferSampleLocationsfvNV(GLuint framebuffer, GLuint start, GLsizei count, const GLfloat * v) { ((void (*)(GLuint, GLuint, GLsizei, const GLfloat *))TBL()[IDX_glNamedFramebufferSampleLocationsfvNV])(framebuffer, start, count, v); }
EXPORT void glResolveDepthValuesNV(void) { ((void (*)(void))TBL()[IDX_glResolveDepthValuesNV])(); }
EXPORT void glViewportArrayvNV(GLuint first, GLsizei count, const GLfloat * v) { ((void (*)(GLuint, GLsizei, const GLfloat *))TBL()[IDX_glViewportArrayvNV])(first, count, v); }
EXPORT void glViewportIndexedfNV(GLuint index, GLfloat x, GLfloat y, GLfloat w, GLfloat h) { ((void (*)(GLuint, GLfloat, GLfloat, GLfloat, GLfloat))TBL()[IDX_glViewportIndexedfNV])(index, x, y, w, h); }
EXPORT void glViewportIndexedfvNV(GLuint index, const GLfloat * v) { ((void (*)(GLuint, const GLfloat *))TBL()[IDX_glViewportIndexedfvNV])(index, v); }
EXPORT void glScissorArrayvNV(GLuint first, GLsizei count, const GLint * v) { ((void (*)(GLuint, GLsizei, const GLint *))TBL()[IDX_glScissorArrayvNV])(first, count, v); }
EXPORT void glScissorIndexedNV(GLuint index, GLint left, GLint bottom, GLsizei width, GLsizei height) { ((void (*)(GLuint, GLint, GLint, GLsizei, GLsizei))TBL()[IDX_glScissorIndexedNV])(index, left, bottom, width, height); }
EXPORT void glScissorIndexedvNV(GLuint index, const GLint * v) { ((void (*)(GLuint, const GLint *))TBL()[IDX_glScissorIndexedvNV])(index, v); }
EXPORT void glDepthRangeArrayfvNV(GLuint first, GLsizei count, const GLfloat * v) { ((void (*)(GLuint, GLsizei, const GLfloat *))TBL()[IDX_glDepthRangeArrayfvNV])(first, count, v); }
EXPORT void glDepthRangeIndexedfNV(GLuint index, GLfloat n, GLfloat f) { ((void (*)(GLuint, GLfloat, GLfloat))TBL()[IDX_glDepthRangeIndexedfNV])(index, n, f); }
EXPORT void glGetFloati_vNV(GLenum target, GLuint index, GLfloat * data) { ((void (*)(GLenum, GLuint, GLfloat *))TBL()[IDX_glGetFloati_vNV])(target, index, data); }
EXPORT void glEnableiNV(GLenum target, GLuint index) { ((void (*)(GLenum, GLuint))TBL()[IDX_glEnableiNV])(target, index); }
EXPORT void glDisableiNV(GLenum target, GLuint index) { ((void (*)(GLenum, GLuint))TBL()[IDX_glDisableiNV])(target, index); }
EXPORT GLboolean glIsEnablediNV(GLenum target, GLuint index) { return ((GLboolean (*)(GLenum, GLuint))TBL()[IDX_glIsEnablediNV])(target, index); }
EXPORT void glViewportSwizzleNV(GLuint index, GLenum swizzlex, GLenum swizzley, GLenum swizzlez, GLenum swizzlew) { ((void (*)(GLuint, GLenum, GLenum, GLenum, GLenum))TBL()[IDX_glViewportSwizzleNV])(index, swizzlex, swizzley, swizzlez, swizzlew); }
EXPORT void glFramebufferTextureMultiviewOVR(GLenum target, GLenum attachment, GLuint texture, GLint level, GLint baseViewIndex, GLsizei numViews) { ((void (*)(GLenum, GLenum, GLuint, GLint, GLint, GLsizei))TBL()[IDX_glFramebufferTextureMultiviewOVR])(target, attachment, texture, level, baseViewIndex, numViews); }
EXPORT void glFramebufferTextureMultisampleMultiviewOVR(GLenum target, GLenum attachment, GLuint texture, GLint level, GLsizei samples, GLint baseViewIndex, GLsizei numViews) { ((void (*)(GLenum, GLenum, GLuint, GLint, GLsizei, GLint, GLsizei))TBL()[IDX_glFramebufferTextureMultisampleMultiviewOVR])(target, attachment, texture, level, samples, baseViewIndex, numViews); }
EXPORT void glAlphaFuncQCOM(GLenum func, GLclampf ref) { ((void (*)(GLenum, GLclampf))TBL()[IDX_glAlphaFuncQCOM])(func, ref); }
EXPORT void glGetDriverControlsQCOM(GLint * num, GLsizei size, GLuint * driverControls) { ((void (*)(GLint *, GLsizei, GLuint *))TBL()[IDX_glGetDriverControlsQCOM])(num, size, driverControls); }
EXPORT void glGetDriverControlStringQCOM(GLuint driverControl, GLsizei bufSize, GLsizei * length, GLchar * driverControlString) { ((void (*)(GLuint, GLsizei, GLsizei *, GLchar *))TBL()[IDX_glGetDriverControlStringQCOM])(driverControl, bufSize, length, driverControlString); }
EXPORT void glEnableDriverControlQCOM(GLuint driverControl) { ((void (*)(GLuint))TBL()[IDX_glEnableDriverControlQCOM])(driverControl); }
EXPORT void glDisableDriverControlQCOM(GLuint driverControl) { ((void (*)(GLuint))TBL()[IDX_glDisableDriverControlQCOM])(driverControl); }
EXPORT void glExtGetTexturesQCOM(GLuint * textures, GLint maxTextures, GLint * numTextures) { ((void (*)(GLuint *, GLint, GLint *))TBL()[IDX_glExtGetTexturesQCOM])(textures, maxTextures, numTextures); }
EXPORT void glExtGetBuffersQCOM(GLuint * buffers, GLint maxBuffers, GLint * numBuffers) { ((void (*)(GLuint *, GLint, GLint *))TBL()[IDX_glExtGetBuffersQCOM])(buffers, maxBuffers, numBuffers); }
EXPORT void glExtGetRenderbuffersQCOM(GLuint * renderbuffers, GLint maxRenderbuffers, GLint * numRenderbuffers) { ((void (*)(GLuint *, GLint, GLint *))TBL()[IDX_glExtGetRenderbuffersQCOM])(renderbuffers, maxRenderbuffers, numRenderbuffers); }
EXPORT void glExtGetFramebuffersQCOM(GLuint * framebuffers, GLint maxFramebuffers, GLint * numFramebuffers) { ((void (*)(GLuint *, GLint, GLint *))TBL()[IDX_glExtGetFramebuffersQCOM])(framebuffers, maxFramebuffers, numFramebuffers); }
EXPORT void glExtGetTexLevelParameterivQCOM(GLuint texture, GLenum face, GLint level, GLenum pname, GLint * params) { ((void (*)(GLuint, GLenum, GLint, GLenum, GLint *))TBL()[IDX_glExtGetTexLevelParameterivQCOM])(texture, face, level, pname, params); }
EXPORT void glExtTexObjectStateOverrideiQCOM(GLenum target, GLenum pname, GLint param) { ((void (*)(GLenum, GLenum, GLint))TBL()[IDX_glExtTexObjectStateOverrideiQCOM])(target, pname, param); }
EXPORT void glExtGetTexSubImageQCOM(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint zoffset, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, void * texels) { ((void (*)(GLenum, GLint, GLint, GLint, GLint, GLsizei, GLsizei, GLsizei, GLenum, GLenum, void *))TBL()[IDX_glExtGetTexSubImageQCOM])(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, texels); }
EXPORT void glExtGetBufferPointervQCOM(GLenum target, void ** params) { ((void (*)(GLenum, void **))TBL()[IDX_glExtGetBufferPointervQCOM])(target, params); }
EXPORT void glExtGetShadersQCOM(GLuint * shaders, GLint maxShaders, GLint * numShaders) { ((void (*)(GLuint *, GLint, GLint *))TBL()[IDX_glExtGetShadersQCOM])(shaders, maxShaders, numShaders); }
EXPORT void glExtGetProgramsQCOM(GLuint * programs, GLint maxPrograms, GLint * numPrograms) { ((void (*)(GLuint *, GLint, GLint *))TBL()[IDX_glExtGetProgramsQCOM])(programs, maxPrograms, numPrograms); }
EXPORT GLboolean glExtIsProgramBinaryQCOM(GLuint program) { return ((GLboolean (*)(GLuint))TBL()[IDX_glExtIsProgramBinaryQCOM])(program); }
EXPORT void glExtGetProgramBinarySourceQCOM(GLuint program, GLenum shadertype, GLchar * source, GLint * length) { ((void (*)(GLuint, GLenum, GLchar *, GLint *))TBL()[IDX_glExtGetProgramBinarySourceQCOM])(program, shadertype, source, length); }
EXPORT void glFramebufferFoveationConfigQCOM(GLuint framebuffer, GLuint numLayers, GLuint focalPointsPerLayer, GLuint requestedFeatures, GLuint * providedFeatures) { ((void (*)(GLuint, GLuint, GLuint, GLuint, GLuint *))TBL()[IDX_glFramebufferFoveationConfigQCOM])(framebuffer, numLayers, focalPointsPerLayer, requestedFeatures, providedFeatures); }
EXPORT void glFramebufferFoveationParametersQCOM(GLuint framebuffer, GLuint layer, GLuint focalPoint, GLfloat focalX, GLfloat focalY, GLfloat gainX, GLfloat gainY, GLfloat foveaArea) { ((void (*)(GLuint, GLuint, GLuint, GLfloat, GLfloat, GLfloat, GLfloat, GLfloat))TBL()[IDX_glFramebufferFoveationParametersQCOM])(framebuffer, layer, focalPoint, focalX, focalY, gainX, gainY, foveaArea); }
EXPORT void glFramebufferFetchBarrierQCOM(void) { ((void (*)(void))TBL()[IDX_glFramebufferFetchBarrierQCOM])(); }
EXPORT void glTextureFoveationParametersQCOM(GLuint texture, GLuint layer, GLuint focalPoint, GLfloat focalX, GLfloat focalY, GLfloat gainX, GLfloat gainY, GLfloat foveaArea) { ((void (*)(GLuint, GLuint, GLuint, GLfloat, GLfloat, GLfloat, GLfloat, GLfloat))TBL()[IDX_glTextureFoveationParametersQCOM])(texture, layer, focalPoint, focalX, focalY, gainX, gainY, foveaArea); }
EXPORT void glStartTilingQCOM(GLuint x, GLuint y, GLuint width, GLuint height, GLbitfield preserveMask) { ((void (*)(GLuint, GLuint, GLuint, GLuint, GLbitfield))TBL()[IDX_glStartTilingQCOM])(x, y, width, height, preserveMask); }
EXPORT void glEndTilingQCOM(GLbitfield preserveMask) { ((void (*)(GLbitfield))TBL()[IDX_glEndTilingQCOM])(preserveMask); }
EXPORT void glAlphaFunc(GLenum func, GLfloat ref) { ((void (*)(GLenum, GLfloat))TBL()[IDX_glAlphaFunc])(func, ref); }
EXPORT void glClipPlanef(GLenum p, const GLfloat * eqn) { ((void (*)(GLenum, const GLfloat *))TBL()[IDX_glClipPlanef])(p, eqn); }
EXPORT void glColor4f(GLfloat red, GLfloat green, GLfloat blue, GLfloat alpha) { ((void (*)(GLfloat, GLfloat, GLfloat, GLfloat))TBL()[IDX_glColor4f])(red, green, blue, alpha); }
EXPORT void glFogf(GLenum pname, GLfloat param) { ((void (*)(GLenum, GLfloat))TBL()[IDX_glFogf])(pname, param); }
EXPORT void glFogfv(GLenum pname, const GLfloat * params) { ((void (*)(GLenum, const GLfloat *))TBL()[IDX_glFogfv])(pname, params); }
EXPORT void glFrustumf(GLfloat l, GLfloat r, GLfloat b, GLfloat t, GLfloat n, GLfloat f) { ((void (*)(GLfloat, GLfloat, GLfloat, GLfloat, GLfloat, GLfloat))TBL()[IDX_glFrustumf])(l, r, b, t, n, f); }
EXPORT void glGetClipPlanef(GLenum plane, GLfloat * equation) { ((void (*)(GLenum, GLfloat *))TBL()[IDX_glGetClipPlanef])(plane, equation); }
EXPORT void glGetLightfv(GLenum light, GLenum pname, GLfloat * params) { ((void (*)(GLenum, GLenum, GLfloat *))TBL()[IDX_glGetLightfv])(light, pname, params); }
EXPORT void glGetMaterialfv(GLenum face, GLenum pname, GLfloat * params) { ((void (*)(GLenum, GLenum, GLfloat *))TBL()[IDX_glGetMaterialfv])(face, pname, params); }
EXPORT void glGetTexEnvfv(GLenum target, GLenum pname, GLfloat * params) { ((void (*)(GLenum, GLenum, GLfloat *))TBL()[IDX_glGetTexEnvfv])(target, pname, params); }
EXPORT void glLightModelf(GLenum pname, GLfloat param) { ((void (*)(GLenum, GLfloat))TBL()[IDX_glLightModelf])(pname, param); }
EXPORT void glLightModelfv(GLenum pname, const GLfloat * params) { ((void (*)(GLenum, const GLfloat *))TBL()[IDX_glLightModelfv])(pname, params); }
EXPORT void glLightf(GLenum light, GLenum pname, GLfloat param) { ((void (*)(GLenum, GLenum, GLfloat))TBL()[IDX_glLightf])(light, pname, param); }
EXPORT void glLightfv(GLenum light, GLenum pname, const GLfloat * params) { ((void (*)(GLenum, GLenum, const GLfloat *))TBL()[IDX_glLightfv])(light, pname, params); }
EXPORT void glLoadMatrixf(const GLfloat * m) { ((void (*)(const GLfloat *))TBL()[IDX_glLoadMatrixf])(m); }
EXPORT void glMaterialf(GLenum face, GLenum pname, GLfloat param) { ((void (*)(GLenum, GLenum, GLfloat))TBL()[IDX_glMaterialf])(face, pname, param); }
EXPORT void glMaterialfv(GLenum face, GLenum pname, const GLfloat * params) { ((void (*)(GLenum, GLenum, const GLfloat *))TBL()[IDX_glMaterialfv])(face, pname, params); }
EXPORT void glMultMatrixf(const GLfloat * m) { ((void (*)(const GLfloat *))TBL()[IDX_glMultMatrixf])(m); }
EXPORT void glMultiTexCoord4f(GLenum target, GLfloat s, GLfloat t, GLfloat r, GLfloat q) { ((void (*)(GLenum, GLfloat, GLfloat, GLfloat, GLfloat))TBL()[IDX_glMultiTexCoord4f])(target, s, t, r, q); }
EXPORT void glNormal3f(GLfloat nx, GLfloat ny, GLfloat nz) { ((void (*)(GLfloat, GLfloat, GLfloat))TBL()[IDX_glNormal3f])(nx, ny, nz); }
EXPORT void glOrthof(GLfloat l, GLfloat r, GLfloat b, GLfloat t, GLfloat n, GLfloat f) { ((void (*)(GLfloat, GLfloat, GLfloat, GLfloat, GLfloat, GLfloat))TBL()[IDX_glOrthof])(l, r, b, t, n, f); }
EXPORT void glPointParameterf(GLenum pname, GLfloat param) { ((void (*)(GLenum, GLfloat))TBL()[IDX_glPointParameterf])(pname, param); }
EXPORT void glPointParameterfv(GLenum pname, const GLfloat * params) { ((void (*)(GLenum, const GLfloat *))TBL()[IDX_glPointParameterfv])(pname, params); }
EXPORT void glPointSize(GLfloat size) { ((void (*)(GLfloat))TBL()[IDX_glPointSize])(size); }
EXPORT void glRotatef(GLfloat angle, GLfloat x, GLfloat y, GLfloat z) { ((void (*)(GLfloat, GLfloat, GLfloat, GLfloat))TBL()[IDX_glRotatef])(angle, x, y, z); }
EXPORT void glScalef(GLfloat x, GLfloat y, GLfloat z) { ((void (*)(GLfloat, GLfloat, GLfloat))TBL()[IDX_glScalef])(x, y, z); }
EXPORT void glTexEnvf(GLenum target, GLenum pname, GLfloat param) { ((void (*)(GLenum, GLenum, GLfloat))TBL()[IDX_glTexEnvf])(target, pname, param); }
EXPORT void glTexEnvfv(GLenum target, GLenum pname, const GLfloat * params) { ((void (*)(GLenum, GLenum, const GLfloat *))TBL()[IDX_glTexEnvfv])(target, pname, params); }
EXPORT void glTranslatef(GLfloat x, GLfloat y, GLfloat z) { ((void (*)(GLfloat, GLfloat, GLfloat))TBL()[IDX_glTranslatef])(x, y, z); }
EXPORT void glAlphaFuncx(GLenum func, GLfixed ref) { ((void (*)(GLenum, GLfixed))TBL()[IDX_glAlphaFuncx])(func, ref); }
EXPORT void glClearColorx(GLfixed red, GLfixed green, GLfixed blue, GLfixed alpha) { ((void (*)(GLfixed, GLfixed, GLfixed, GLfixed))TBL()[IDX_glClearColorx])(red, green, blue, alpha); }
EXPORT void glClearDepthx(GLfixed depth) { ((void (*)(GLfixed))TBL()[IDX_glClearDepthx])(depth); }
EXPORT void glClientActiveTexture(GLenum texture) { ((void (*)(GLenum))TBL()[IDX_glClientActiveTexture])(texture); }
EXPORT void glClipPlanex(GLenum plane, const GLfixed * equation) { ((void (*)(GLenum, const GLfixed *))TBL()[IDX_glClipPlanex])(plane, equation); }
EXPORT void glColor4ub(GLubyte red, GLubyte green, GLubyte blue, GLubyte alpha) { ((void (*)(GLubyte, GLubyte, GLubyte, GLubyte))TBL()[IDX_glColor4ub])(red, green, blue, alpha); }
EXPORT void glColor4x(GLfixed red, GLfixed green, GLfixed blue, GLfixed alpha) { ((void (*)(GLfixed, GLfixed, GLfixed, GLfixed))TBL()[IDX_glColor4x])(red, green, blue, alpha); }
EXPORT void glColorPointer(GLint size, GLenum type, GLsizei stride, const void * pointer) { ((void (*)(GLint, GLenum, GLsizei, const void *))TBL()[IDX_glColorPointer])(size, type, stride, pointer); }
EXPORT void glDepthRangex(GLfixed n, GLfixed f) { ((void (*)(GLfixed, GLfixed))TBL()[IDX_glDepthRangex])(n, f); }
EXPORT void glDisableClientState(GLenum array) { ((void (*)(GLenum))TBL()[IDX_glDisableClientState])(array); }
EXPORT void glEnableClientState(GLenum array) { ((void (*)(GLenum))TBL()[IDX_glEnableClientState])(array); }
EXPORT void glFogx(GLenum pname, GLfixed param) { ((void (*)(GLenum, GLfixed))TBL()[IDX_glFogx])(pname, param); }
EXPORT void glFogxv(GLenum pname, const GLfixed * param) { ((void (*)(GLenum, const GLfixed *))TBL()[IDX_glFogxv])(pname, param); }
EXPORT void glFrustumx(GLfixed l, GLfixed r, GLfixed b, GLfixed t, GLfixed n, GLfixed f) { ((void (*)(GLfixed, GLfixed, GLfixed, GLfixed, GLfixed, GLfixed))TBL()[IDX_glFrustumx])(l, r, b, t, n, f); }
EXPORT void glGetClipPlanex(GLenum plane, GLfixed * equation) { ((void (*)(GLenum, GLfixed *))TBL()[IDX_glGetClipPlanex])(plane, equation); }
EXPORT void glGetFixedv(GLenum pname, GLfixed * params) { ((void (*)(GLenum, GLfixed *))TBL()[IDX_glGetFixedv])(pname, params); }
EXPORT void glGetLightxv(GLenum light, GLenum pname, GLfixed * params) { ((void (*)(GLenum, GLenum, GLfixed *))TBL()[IDX_glGetLightxv])(light, pname, params); }
EXPORT void glGetMaterialxv(GLenum face, GLenum pname, GLfixed * params) { ((void (*)(GLenum, GLenum, GLfixed *))TBL()[IDX_glGetMaterialxv])(face, pname, params); }
EXPORT void glGetPointerv(GLenum pname, void ** params) { ((void (*)(GLenum, void **))TBL()[IDX_glGetPointerv])(pname, params); }
EXPORT void glGetTexEnviv(GLenum target, GLenum pname, GLint * params) { ((void (*)(GLenum, GLenum, GLint *))TBL()[IDX_glGetTexEnviv])(target, pname, params); }
EXPORT void glGetTexEnvxv(GLenum target, GLenum pname, GLfixed * params) { ((void (*)(GLenum, GLenum, GLfixed *))TBL()[IDX_glGetTexEnvxv])(target, pname, params); }
EXPORT void glGetTexParameterxv(GLenum target, GLenum pname, GLfixed * params) { ((void (*)(GLenum, GLenum, GLfixed *))TBL()[IDX_glGetTexParameterxv])(target, pname, params); }
EXPORT void glLightModelx(GLenum pname, GLfixed param) { ((void (*)(GLenum, GLfixed))TBL()[IDX_glLightModelx])(pname, param); }
EXPORT void glLightModelxv(GLenum pname, const GLfixed * param) { ((void (*)(GLenum, const GLfixed *))TBL()[IDX_glLightModelxv])(pname, param); }
EXPORT void glLightx(GLenum light, GLenum pname, GLfixed param) { ((void (*)(GLenum, GLenum, GLfixed))TBL()[IDX_glLightx])(light, pname, param); }
EXPORT void glLightxv(GLenum light, GLenum pname, const GLfixed * params) { ((void (*)(GLenum, GLenum, const GLfixed *))TBL()[IDX_glLightxv])(light, pname, params); }
EXPORT void glLineWidthx(GLfixed width) { ((void (*)(GLfixed))TBL()[IDX_glLineWidthx])(width); }
EXPORT void glLoadIdentity(void) { ((void (*)(void))TBL()[IDX_glLoadIdentity])(); }
EXPORT void glLoadMatrixx(const GLfixed * m) { ((void (*)(const GLfixed *))TBL()[IDX_glLoadMatrixx])(m); }
EXPORT void glLogicOp(GLenum opcode) { ((void (*)(GLenum))TBL()[IDX_glLogicOp])(opcode); }
EXPORT void glMaterialx(GLenum face, GLenum pname, GLfixed param) { ((void (*)(GLenum, GLenum, GLfixed))TBL()[IDX_glMaterialx])(face, pname, param); }
EXPORT void glMaterialxv(GLenum face, GLenum pname, const GLfixed * param) { ((void (*)(GLenum, GLenum, const GLfixed *))TBL()[IDX_glMaterialxv])(face, pname, param); }
EXPORT void glMatrixMode(GLenum mode) { ((void (*)(GLenum))TBL()[IDX_glMatrixMode])(mode); }
EXPORT void glMultMatrixx(const GLfixed * m) { ((void (*)(const GLfixed *))TBL()[IDX_glMultMatrixx])(m); }
EXPORT void glMultiTexCoord4x(GLenum texture, GLfixed s, GLfixed t, GLfixed r, GLfixed q) { ((void (*)(GLenum, GLfixed, GLfixed, GLfixed, GLfixed))TBL()[IDX_glMultiTexCoord4x])(texture, s, t, r, q); }
EXPORT void glNormal3x(GLfixed nx, GLfixed ny, GLfixed nz) { ((void (*)(GLfixed, GLfixed, GLfixed))TBL()[IDX_glNormal3x])(nx, ny, nz); }
EXPORT void glNormalPointer(GLenum type, GLsizei stride, const void * pointer) { ((void (*)(GLenum, GLsizei, const void *))TBL()[IDX_glNormalPointer])(type, stride, pointer); }
EXPORT void glOrthox(GLfixed l, GLfixed r, GLfixed b, GLfixed t, GLfixed n, GLfixed f) { ((void (*)(GLfixed, GLfixed, GLfixed, GLfixed, GLfixed, GLfixed))TBL()[IDX_glOrthox])(l, r, b, t, n, f); }
EXPORT void glPointParameterx(GLenum pname, GLfixed param) { ((void (*)(GLenum, GLfixed))TBL()[IDX_glPointParameterx])(pname, param); }
EXPORT void glPointParameterxv(GLenum pname, const GLfixed * params) { ((void (*)(GLenum, const GLfixed *))TBL()[IDX_glPointParameterxv])(pname, params); }
EXPORT void glPointSizex(GLfixed size) { ((void (*)(GLfixed))TBL()[IDX_glPointSizex])(size); }
EXPORT void glPolygonOffsetx(GLfixed factor, GLfixed units) { ((void (*)(GLfixed, GLfixed))TBL()[IDX_glPolygonOffsetx])(factor, units); }
EXPORT void glPopMatrix(void) { ((void (*)(void))TBL()[IDX_glPopMatrix])(); }
EXPORT void glPushMatrix(void) { ((void (*)(void))TBL()[IDX_glPushMatrix])(); }
EXPORT void glRotatex(GLfixed angle, GLfixed x, GLfixed y, GLfixed z) { ((void (*)(GLfixed, GLfixed, GLfixed, GLfixed))TBL()[IDX_glRotatex])(angle, x, y, z); }
EXPORT void glSampleCoveragex(GLclampx value, GLboolean invert) { ((void (*)(GLclampx, GLboolean))TBL()[IDX_glSampleCoveragex])(value, invert); }
EXPORT void glScalex(GLfixed x, GLfixed y, GLfixed z) { ((void (*)(GLfixed, GLfixed, GLfixed))TBL()[IDX_glScalex])(x, y, z); }
EXPORT void glShadeModel(GLenum mode) { ((void (*)(GLenum))TBL()[IDX_glShadeModel])(mode); }
EXPORT void glTexCoordPointer(GLint size, GLenum type, GLsizei stride, const void * pointer) { ((void (*)(GLint, GLenum, GLsizei, const void *))TBL()[IDX_glTexCoordPointer])(size, type, stride, pointer); }
EXPORT void glTexEnvi(GLenum target, GLenum pname, GLint param) { ((void (*)(GLenum, GLenum, GLint))TBL()[IDX_glTexEnvi])(target, pname, param); }
EXPORT void glTexEnvx(GLenum target, GLenum pname, GLfixed param) { ((void (*)(GLenum, GLenum, GLfixed))TBL()[IDX_glTexEnvx])(target, pname, param); }
EXPORT void glTexEnviv(GLenum target, GLenum pname, const GLint * params) { ((void (*)(GLenum, GLenum, const GLint *))TBL()[IDX_glTexEnviv])(target, pname, params); }
EXPORT void glTexEnvxv(GLenum target, GLenum pname, const GLfixed * params) { ((void (*)(GLenum, GLenum, const GLfixed *))TBL()[IDX_glTexEnvxv])(target, pname, params); }
EXPORT void glTexParameterx(GLenum target, GLenum pname, GLfixed param) { ((void (*)(GLenum, GLenum, GLfixed))TBL()[IDX_glTexParameterx])(target, pname, param); }
EXPORT void glTexParameterxv(GLenum target, GLenum pname, const GLfixed * params) { ((void (*)(GLenum, GLenum, const GLfixed *))TBL()[IDX_glTexParameterxv])(target, pname, params); }
EXPORT void glTranslatex(GLfixed x, GLfixed y, GLfixed z) { ((void (*)(GLfixed, GLfixed, GLfixed))TBL()[IDX_glTranslatex])(x, y, z); }
EXPORT void glVertexPointer(GLint size, GLenum type, GLsizei stride, const void * pointer) { ((void (*)(GLint, GLenum, GLsizei, const void *))TBL()[IDX_glVertexPointer])(size, type, stride, pointer); }
EXPORT void glPointSizePointerOES(GLenum type, GLsizei stride, const void * pointer) { ((void (*)(GLenum, GLsizei, const void *))TBL()[IDX_glPointSizePointerOES])(type, stride, pointer); }
EXPORT void glBlendEquationSeparateOES(GLenum modeRGB, GLenum modeAlpha) { ((void (*)(GLenum, GLenum))TBL()[IDX_glBlendEquationSeparateOES])(modeRGB, modeAlpha); }
EXPORT void glBlendFuncSeparateOES(GLenum srcRGB, GLenum dstRGB, GLenum srcAlpha, GLenum dstAlpha) { ((void (*)(GLenum, GLenum, GLenum, GLenum))TBL()[IDX_glBlendFuncSeparateOES])(srcRGB, dstRGB, srcAlpha, dstAlpha); }
EXPORT void glBlendEquationOES(GLenum mode) { ((void (*)(GLenum))TBL()[IDX_glBlendEquationOES])(mode); }
EXPORT void glDrawTexsOES(GLshort x, GLshort y, GLshort z, GLshort width, GLshort height) { ((void (*)(GLshort, GLshort, GLshort, GLshort, GLshort))TBL()[IDX_glDrawTexsOES])(x, y, z, width, height); }
EXPORT void glDrawTexiOES(GLint x, GLint y, GLint z, GLint width, GLint height) { ((void (*)(GLint, GLint, GLint, GLint, GLint))TBL()[IDX_glDrawTexiOES])(x, y, z, width, height); }
EXPORT void glDrawTexxOES(GLfixed x, GLfixed y, GLfixed z, GLfixed width, GLfixed height) { ((void (*)(GLfixed, GLfixed, GLfixed, GLfixed, GLfixed))TBL()[IDX_glDrawTexxOES])(x, y, z, width, height); }
EXPORT void glDrawTexsvOES(const GLshort * coords) { ((void (*)(const GLshort *))TBL()[IDX_glDrawTexsvOES])(coords); }
EXPORT void glDrawTexivOES(const GLint * coords) { ((void (*)(const GLint *))TBL()[IDX_glDrawTexivOES])(coords); }
EXPORT void glDrawTexxvOES(const GLfixed * coords) { ((void (*)(const GLfixed *))TBL()[IDX_glDrawTexxvOES])(coords); }
EXPORT void glDrawTexfOES(GLfloat x, GLfloat y, GLfloat z, GLfloat width, GLfloat height) { ((void (*)(GLfloat, GLfloat, GLfloat, GLfloat, GLfloat))TBL()[IDX_glDrawTexfOES])(x, y, z, width, height); }
EXPORT void glDrawTexfvOES(const GLfloat * coords) { ((void (*)(const GLfloat *))TBL()[IDX_glDrawTexfvOES])(coords); }
EXPORT void glAlphaFuncxOES(GLenum func, GLfixed ref) { ((void (*)(GLenum, GLfixed))TBL()[IDX_glAlphaFuncxOES])(func, ref); }
EXPORT void glClearColorxOES(GLfixed red, GLfixed green, GLfixed blue, GLfixed alpha) { ((void (*)(GLfixed, GLfixed, GLfixed, GLfixed))TBL()[IDX_glClearColorxOES])(red, green, blue, alpha); }
EXPORT void glClearDepthxOES(GLfixed depth) { ((void (*)(GLfixed))TBL()[IDX_glClearDepthxOES])(depth); }
EXPORT void glClipPlanexOES(GLenum plane, const GLfixed * equation) { ((void (*)(GLenum, const GLfixed *))TBL()[IDX_glClipPlanexOES])(plane, equation); }
EXPORT void glColor4xOES(GLfixed red, GLfixed green, GLfixed blue, GLfixed alpha) { ((void (*)(GLfixed, GLfixed, GLfixed, GLfixed))TBL()[IDX_glColor4xOES])(red, green, blue, alpha); }
EXPORT void glDepthRangexOES(GLfixed n, GLfixed f) { ((void (*)(GLfixed, GLfixed))TBL()[IDX_glDepthRangexOES])(n, f); }
EXPORT void glFogxOES(GLenum pname, GLfixed param) { ((void (*)(GLenum, GLfixed))TBL()[IDX_glFogxOES])(pname, param); }
EXPORT void glFogxvOES(GLenum pname, const GLfixed * param) { ((void (*)(GLenum, const GLfixed *))TBL()[IDX_glFogxvOES])(pname, param); }
EXPORT void glFrustumxOES(GLfixed l, GLfixed r, GLfixed b, GLfixed t, GLfixed n, GLfixed f) { ((void (*)(GLfixed, GLfixed, GLfixed, GLfixed, GLfixed, GLfixed))TBL()[IDX_glFrustumxOES])(l, r, b, t, n, f); }
EXPORT void glGetClipPlanexOES(GLenum plane, GLfixed * equation) { ((void (*)(GLenum, GLfixed *))TBL()[IDX_glGetClipPlanexOES])(plane, equation); }
EXPORT void glGetFixedvOES(GLenum pname, GLfixed * params) { ((void (*)(GLenum, GLfixed *))TBL()[IDX_glGetFixedvOES])(pname, params); }
EXPORT void glGetTexEnvxvOES(GLenum target, GLenum pname, GLfixed * params) { ((void (*)(GLenum, GLenum, GLfixed *))TBL()[IDX_glGetTexEnvxvOES])(target, pname, params); }
EXPORT void glGetTexParameterxvOES(GLenum target, GLenum pname, GLfixed * params) { ((void (*)(GLenum, GLenum, GLfixed *))TBL()[IDX_glGetTexParameterxvOES])(target, pname, params); }
EXPORT void glLightModelxOES(GLenum pname, GLfixed param) { ((void (*)(GLenum, GLfixed))TBL()[IDX_glLightModelxOES])(pname, param); }
EXPORT void glLightModelxvOES(GLenum pname, const GLfixed * param) { ((void (*)(GLenum, const GLfixed *))TBL()[IDX_glLightModelxvOES])(pname, param); }
EXPORT void glLightxOES(GLenum light, GLenum pname, GLfixed param) { ((void (*)(GLenum, GLenum, GLfixed))TBL()[IDX_glLightxOES])(light, pname, param); }
EXPORT void glLightxvOES(GLenum light, GLenum pname, const GLfixed * params) { ((void (*)(GLenum, GLenum, const GLfixed *))TBL()[IDX_glLightxvOES])(light, pname, params); }
EXPORT void glLineWidthxOES(GLfixed width) { ((void (*)(GLfixed))TBL()[IDX_glLineWidthxOES])(width); }
EXPORT void glLoadMatrixxOES(const GLfixed * m) { ((void (*)(const GLfixed *))TBL()[IDX_glLoadMatrixxOES])(m); }
EXPORT void glMaterialxOES(GLenum face, GLenum pname, GLfixed param) { ((void (*)(GLenum, GLenum, GLfixed))TBL()[IDX_glMaterialxOES])(face, pname, param); }
EXPORT void glMaterialxvOES(GLenum face, GLenum pname, const GLfixed * param) { ((void (*)(GLenum, GLenum, const GLfixed *))TBL()[IDX_glMaterialxvOES])(face, pname, param); }
EXPORT void glMultMatrixxOES(const GLfixed * m) { ((void (*)(const GLfixed *))TBL()[IDX_glMultMatrixxOES])(m); }
EXPORT void glMultiTexCoord4xOES(GLenum texture, GLfixed s, GLfixed t, GLfixed r, GLfixed q) { ((void (*)(GLenum, GLfixed, GLfixed, GLfixed, GLfixed))TBL()[IDX_glMultiTexCoord4xOES])(texture, s, t, r, q); }
EXPORT void glNormal3xOES(GLfixed nx, GLfixed ny, GLfixed nz) { ((void (*)(GLfixed, GLfixed, GLfixed))TBL()[IDX_glNormal3xOES])(nx, ny, nz); }
EXPORT void glOrthoxOES(GLfixed l, GLfixed r, GLfixed b, GLfixed t, GLfixed n, GLfixed f) { ((void (*)(GLfixed, GLfixed, GLfixed, GLfixed, GLfixed, GLfixed))TBL()[IDX_glOrthoxOES])(l, r, b, t, n, f); }
EXPORT void glPointParameterxvOES(GLenum pname, const GLfixed * params) { ((void (*)(GLenum, const GLfixed *))TBL()[IDX_glPointParameterxvOES])(pname, params); }
EXPORT void glPointSizexOES(GLfixed size) { ((void (*)(GLfixed))TBL()[IDX_glPointSizexOES])(size); }
EXPORT void glPolygonOffsetxOES(GLfixed factor, GLfixed units) { ((void (*)(GLfixed, GLfixed))TBL()[IDX_glPolygonOffsetxOES])(factor, units); }
EXPORT void glRotatexOES(GLfixed angle, GLfixed x, GLfixed y, GLfixed z) { ((void (*)(GLfixed, GLfixed, GLfixed, GLfixed))TBL()[IDX_glRotatexOES])(angle, x, y, z); }
EXPORT void glScalexOES(GLfixed x, GLfixed y, GLfixed z) { ((void (*)(GLfixed, GLfixed, GLfixed))TBL()[IDX_glScalexOES])(x, y, z); }
EXPORT void glTexEnvxOES(GLenum target, GLenum pname, GLfixed param) { ((void (*)(GLenum, GLenum, GLfixed))TBL()[IDX_glTexEnvxOES])(target, pname, param); }
EXPORT void glTexEnvxvOES(GLenum target, GLenum pname, const GLfixed * params) { ((void (*)(GLenum, GLenum, const GLfixed *))TBL()[IDX_glTexEnvxvOES])(target, pname, params); }
EXPORT void glTexParameterxOES(GLenum target, GLenum pname, GLfixed param) { ((void (*)(GLenum, GLenum, GLfixed))TBL()[IDX_glTexParameterxOES])(target, pname, param); }
EXPORT void glTexParameterxvOES(GLenum target, GLenum pname, const GLfixed * params) { ((void (*)(GLenum, GLenum, const GLfixed *))TBL()[IDX_glTexParameterxvOES])(target, pname, params); }
EXPORT void glTranslatexOES(GLfixed x, GLfixed y, GLfixed z) { ((void (*)(GLfixed, GLfixed, GLfixed))TBL()[IDX_glTranslatexOES])(x, y, z); }
EXPORT void glGetLightxvOES(GLenum light, GLenum pname, GLfixed * params) { ((void (*)(GLenum, GLenum, GLfixed *))TBL()[IDX_glGetLightxvOES])(light, pname, params); }
EXPORT void glGetMaterialxvOES(GLenum face, GLenum pname, GLfixed * params) { ((void (*)(GLenum, GLenum, GLfixed *))TBL()[IDX_glGetMaterialxvOES])(face, pname, params); }
EXPORT void glPointParameterxOES(GLenum pname, GLfixed param) { ((void (*)(GLenum, GLfixed))TBL()[IDX_glPointParameterxOES])(pname, param); }
EXPORT void glSampleCoveragexOES(GLclampx value, GLboolean invert) { ((void (*)(GLclampx, GLboolean))TBL()[IDX_glSampleCoveragexOES])(value, invert); }
EXPORT void glGetTexGenxvOES(GLenum coord, GLenum pname, GLfixed * params) { ((void (*)(GLenum, GLenum, GLfixed *))TBL()[IDX_glGetTexGenxvOES])(coord, pname, params); }
EXPORT void glTexGenxOES(GLenum coord, GLenum pname, GLfixed param) { ((void (*)(GLenum, GLenum, GLfixed))TBL()[IDX_glTexGenxOES])(coord, pname, param); }
EXPORT void glTexGenxvOES(GLenum coord, GLenum pname, const GLfixed * params) { ((void (*)(GLenum, GLenum, const GLfixed *))TBL()[IDX_glTexGenxvOES])(coord, pname, params); }
EXPORT GLboolean glIsRenderbufferOES(GLuint renderbuffer) { return ((GLboolean (*)(GLuint))TBL()[IDX_glIsRenderbufferOES])(renderbuffer); }
EXPORT void glBindRenderbufferOES(GLenum target, GLuint renderbuffer) { ((void (*)(GLenum, GLuint))TBL()[IDX_glBindRenderbufferOES])(target, renderbuffer); }
EXPORT void glDeleteRenderbuffersOES(GLsizei n, const GLuint * renderbuffers) { ((void (*)(GLsizei, const GLuint *))TBL()[IDX_glDeleteRenderbuffersOES])(n, renderbuffers); }
EXPORT void glGenRenderbuffersOES(GLsizei n, GLuint * renderbuffers) { ((void (*)(GLsizei, GLuint *))TBL()[IDX_glGenRenderbuffersOES])(n, renderbuffers); }
EXPORT void glRenderbufferStorageOES(GLenum target, GLenum internalformat, GLsizei width, GLsizei height) { ((void (*)(GLenum, GLenum, GLsizei, GLsizei))TBL()[IDX_glRenderbufferStorageOES])(target, internalformat, width, height); }
EXPORT void glGetRenderbufferParameterivOES(GLenum target, GLenum pname, GLint * params) { ((void (*)(GLenum, GLenum, GLint *))TBL()[IDX_glGetRenderbufferParameterivOES])(target, pname, params); }
EXPORT GLboolean glIsFramebufferOES(GLuint framebuffer) { return ((GLboolean (*)(GLuint))TBL()[IDX_glIsFramebufferOES])(framebuffer); }
EXPORT void glBindFramebufferOES(GLenum target, GLuint framebuffer) { ((void (*)(GLenum, GLuint))TBL()[IDX_glBindFramebufferOES])(target, framebuffer); }
EXPORT void glDeleteFramebuffersOES(GLsizei n, const GLuint * framebuffers) { ((void (*)(GLsizei, const GLuint *))TBL()[IDX_glDeleteFramebuffersOES])(n, framebuffers); }
EXPORT void glGenFramebuffersOES(GLsizei n, GLuint * framebuffers) { ((void (*)(GLsizei, GLuint *))TBL()[IDX_glGenFramebuffersOES])(n, framebuffers); }
EXPORT GLenum glCheckFramebufferStatusOES(GLenum target) { return ((GLenum (*)(GLenum))TBL()[IDX_glCheckFramebufferStatusOES])(target); }
EXPORT void glFramebufferRenderbufferOES(GLenum target, GLenum attachment, GLenum renderbuffertarget, GLuint renderbuffer) { ((void (*)(GLenum, GLenum, GLenum, GLuint))TBL()[IDX_glFramebufferRenderbufferOES])(target, attachment, renderbuffertarget, renderbuffer); }
EXPORT void glFramebufferTexture2DOES(GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level) { ((void (*)(GLenum, GLenum, GLenum, GLuint, GLint))TBL()[IDX_glFramebufferTexture2DOES])(target, attachment, textarget, texture, level); }
EXPORT void glGetFramebufferAttachmentParameterivOES(GLenum target, GLenum attachment, GLenum pname, GLint * params) { ((void (*)(GLenum, GLenum, GLenum, GLint *))TBL()[IDX_glGetFramebufferAttachmentParameterivOES])(target, attachment, pname, params); }
EXPORT void glGenerateMipmapOES(GLenum target) { ((void (*)(GLenum))TBL()[IDX_glGenerateMipmapOES])(target); }
EXPORT void glCurrentPaletteMatrixOES(GLuint matrixpaletteindex) { ((void (*)(GLuint))TBL()[IDX_glCurrentPaletteMatrixOES])(matrixpaletteindex); }
EXPORT void glLoadPaletteFromModelViewMatrixOES(void) { ((void (*)(void))TBL()[IDX_glLoadPaletteFromModelViewMatrixOES])(); }
EXPORT void glMatrixIndexPointerOES(GLint size, GLenum type, GLsizei stride, const void * pointer) { ((void (*)(GLint, GLenum, GLsizei, const void *))TBL()[IDX_glMatrixIndexPointerOES])(size, type, stride, pointer); }
EXPORT void glWeightPointerOES(GLint size, GLenum type, GLsizei stride, const void * pointer) { ((void (*)(GLint, GLenum, GLsizei, const void *))TBL()[IDX_glWeightPointerOES])(size, type, stride, pointer); }
EXPORT GLbitfield glQueryMatrixxOES(GLfixed * mantissa, GLint * exponent) { return ((GLbitfield (*)(GLfixed *, GLint *))TBL()[IDX_glQueryMatrixxOES])(mantissa, exponent); }
EXPORT void glClearDepthfOES(GLclampf depth) { ((void (*)(GLclampf))TBL()[IDX_glClearDepthfOES])(depth); }
EXPORT void glClipPlanefOES(GLenum plane, const GLfloat * equation) { ((void (*)(GLenum, const GLfloat *))TBL()[IDX_glClipPlanefOES])(plane, equation); }
EXPORT void glDepthRangefOES(GLclampf n, GLclampf f) { ((void (*)(GLclampf, GLclampf))TBL()[IDX_glDepthRangefOES])(n, f); }
EXPORT void glFrustumfOES(GLfloat l, GLfloat r, GLfloat b, GLfloat t, GLfloat n, GLfloat f) { ((void (*)(GLfloat, GLfloat, GLfloat, GLfloat, GLfloat, GLfloat))TBL()[IDX_glFrustumfOES])(l, r, b, t, n, f); }
EXPORT void glGetClipPlanefOES(GLenum plane, GLfloat * equation) { ((void (*)(GLenum, GLfloat *))TBL()[IDX_glGetClipPlanefOES])(plane, equation); }
EXPORT void glOrthofOES(GLfloat l, GLfloat r, GLfloat b, GLfloat t, GLfloat n, GLfloat f) { ((void (*)(GLfloat, GLfloat, GLfloat, GLfloat, GLfloat, GLfloat))TBL()[IDX_glOrthofOES])(l, r, b, t, n, f); }
EXPORT void glTexGenfOES(GLenum coord, GLenum pname, GLfloat param) { ((void (*)(GLenum, GLenum, GLfloat))TBL()[IDX_glTexGenfOES])(coord, pname, param); }
EXPORT void glTexGenfvOES(GLenum coord, GLenum pname, const GLfloat * params) { ((void (*)(GLenum, GLenum, const GLfloat *))TBL()[IDX_glTexGenfvOES])(coord, pname, params); }
EXPORT void glTexGeniOES(GLenum coord, GLenum pname, GLint param) { ((void (*)(GLenum, GLenum, GLint))TBL()[IDX_glTexGeniOES])(coord, pname, param); }
EXPORT void glTexGenivOES(GLenum coord, GLenum pname, const GLint * params) { ((void (*)(GLenum, GLenum, const GLint *))TBL()[IDX_glTexGenivOES])(coord, pname, params); }
EXPORT void glGetTexGenfvOES(GLenum coord, GLenum pname, GLfloat * params) { ((void (*)(GLenum, GLenum, GLfloat *))TBL()[IDX_glGetTexGenfvOES])(coord, pname, params); }
EXPORT void glGetTexGenivOES(GLenum coord, GLenum pname, GLint * params) { ((void (*)(GLenum, GLenum, GLint *))TBL()[IDX_glGetTexGenivOES])(coord, pname, params); }
EXPORT void glClipPlanefIMG(GLenum p, const GLfloat * eqn) { ((void (*)(GLenum, const GLfloat *))TBL()[IDX_glClipPlanefIMG])(p, eqn); }
EXPORT void glClipPlanexIMG(GLenum p, const GLfixed * eqn) { ((void (*)(GLenum, const GLfixed *))TBL()[IDX_glClipPlanexIMG])(p, eqn); }
static void k_glPixelStorei(GLenum pname, GLint param);
static void k_glBindVertexArray(GLuint array);
static void k_glDrawArraysIndirect(GLenum mode, const void * indirect);
static void k_glDrawElementsIndirect(GLenum mode, GLenum type, const void * indirect);
static void k_glGetVertexAttribfv(GLuint index, GLenum pname, GLfloat * params);
static void k_glGetVertexAttribiv(GLuint index, GLenum pname, GLint * params);
static void k_glGetVertexAttribIiv(GLuint index, GLenum pname, GLint * params);
static void k_glGetVertexAttribIuiv(GLuint index, GLenum pname, GLuint * params);
static void k_glDeleteBuffers(GLsizei n, const GLuint * buffers);
static void k_glDeleteVertexArrays(GLsizei n, const GLuint * arrays);
static void k_glVertexAttribDivisor(GLuint index, GLuint divisor);
static void k_glGetBooleanv(GLenum pname, GLboolean * data);
static void k_glGetFloatv(GLenum pname, GLfloat * data);
static void k_glGetIntegerv(GLenum pname, GLint * data);
static void k_glGetInteger64v(GLenum pname, GLint64 * data);
static void k_glBufferData(GLenum target, GLsizeiptr size, const void * data, GLenum usage);
static void k_glBufferSubData(GLenum target, GLintptr offset, GLsizeiptr size, const void * data);
static void k_glCopyBufferSubData(GLenum readTarget, GLenum writeTarget, GLintptr readOffset, GLintptr writeOffset, GLsizeiptr size);
static void k_glBindBufferBase(GLenum target, GLuint index, GLuint buffer);
static void k_glBindBufferRange(GLenum target, GLuint index, GLuint buffer, GLintptr offset, GLsizeiptr size);
static void k_glBeginTransformFeedback(GLenum primitiveMode);
static void k_glDispatchCompute(GLuint num_groups_x, GLuint num_groups_y, GLuint num_groups_z);
static void k_glDispatchComputeIndirect(GLintptr indirect);
static void k_glMemoryBarrier(GLbitfield barriers);
static void k_glMemoryBarrierByRegion(GLbitfield barriers);

static void *const kStubs[N_FUNCS] = {
    (void *)glActiveTexture,
    (void *)glAttachShader,
    (void *)glBindAttribLocation,
    (void *)glBindBuffer,
    (void *)glBindFramebuffer,
    (void *)glBindRenderbuffer,
    (void *)glBindTexture,
    (void *)glBlendColor,
    (void *)glBlendEquation,
    (void *)glBlendEquationSeparate,
    (void *)glBlendFunc,
    (void *)glBlendFuncSeparate,
    (void *)glBufferData,
    (void *)glBufferSubData,
    (void *)glCheckFramebufferStatus,
    (void *)glClear,
    (void *)glClearColor,
    (void *)glClearDepthf,
    (void *)glClearStencil,
    (void *)glColorMask,
    (void *)glCompileShader,
    (void *)glCompressedTexImage2D,
    (void *)glCompressedTexSubImage2D,
    (void *)glCopyTexImage2D,
    (void *)glCopyTexSubImage2D,
    (void *)glCreateProgram,
    (void *)glCreateShader,
    (void *)glCullFace,
    (void *)glDeleteBuffers,
    (void *)glDeleteFramebuffers,
    (void *)glDeleteProgram,
    (void *)glDeleteRenderbuffers,
    (void *)glDeleteShader,
    (void *)glDeleteTextures,
    (void *)glDepthFunc,
    (void *)glDepthMask,
    (void *)glDepthRangef,
    (void *)glDetachShader,
    (void *)glDisable,
    (void *)glDisableVertexAttribArray,
    (void *)glDrawArrays,
    (void *)glDrawElements,
    (void *)glEnable,
    (void *)glEnableVertexAttribArray,
    (void *)glFinish,
    (void *)glFlush,
    (void *)glFramebufferRenderbuffer,
    (void *)glFramebufferTexture2D,
    (void *)glFrontFace,
    (void *)glGenBuffers,
    (void *)glGenerateMipmap,
    (void *)glGenFramebuffers,
    (void *)glGenRenderbuffers,
    (void *)glGenTextures,
    (void *)glGetActiveAttrib,
    (void *)glGetActiveUniform,
    (void *)glGetAttachedShaders,
    (void *)glGetAttribLocation,
    (void *)glGetBooleanv,
    (void *)glGetBufferParameteriv,
    (void *)glGetError,
    (void *)glGetFloatv,
    (void *)glGetFramebufferAttachmentParameteriv,
    (void *)glGetIntegerv,
    (void *)glGetProgramiv,
    (void *)glGetProgramInfoLog,
    (void *)glGetRenderbufferParameteriv,
    (void *)glGetShaderiv,
    (void *)glGetShaderInfoLog,
    (void *)glGetShaderPrecisionFormat,
    (void *)glGetShaderSource,
    (void *)glGetString,
    (void *)glGetTexParameterfv,
    (void *)glGetTexParameteriv,
    (void *)glGetUniformfv,
    (void *)glGetUniformiv,
    (void *)glGetUniformLocation,
    (void *)glGetVertexAttribfv,
    (void *)glGetVertexAttribiv,
    (void *)glGetVertexAttribPointerv,
    (void *)glHint,
    (void *)glIsBuffer,
    (void *)glIsEnabled,
    (void *)glIsFramebuffer,
    (void *)glIsProgram,
    (void *)glIsRenderbuffer,
    (void *)glIsShader,
    (void *)glIsTexture,
    (void *)glLineWidth,
    (void *)glLinkProgram,
    (void *)glPixelStorei,
    (void *)glPolygonOffset,
    (void *)glReadPixels,
    (void *)glReleaseShaderCompiler,
    (void *)glRenderbufferStorage,
    (void *)glSampleCoverage,
    (void *)glScissor,
    (void *)glShaderBinary,
    (void *)glShaderSource,
    (void *)glStencilFunc,
    (void *)glStencilFuncSeparate,
    (void *)glStencilMask,
    (void *)glStencilMaskSeparate,
    (void *)glStencilOp,
    (void *)glStencilOpSeparate,
    (void *)glTexImage2D,
    (void *)glTexParameterf,
    (void *)glTexParameterfv,
    (void *)glTexParameteri,
    (void *)glTexParameteriv,
    (void *)glTexSubImage2D,
    (void *)glUniform1f,
    (void *)glUniform1fv,
    (void *)glUniform1i,
    (void *)glUniform1iv,
    (void *)glUniform2f,
    (void *)glUniform2fv,
    (void *)glUniform2i,
    (void *)glUniform2iv,
    (void *)glUniform3f,
    (void *)glUniform3fv,
    (void *)glUniform3i,
    (void *)glUniform3iv,
    (void *)glUniform4f,
    (void *)glUniform4fv,
    (void *)glUniform4i,
    (void *)glUniform4iv,
    (void *)glUniformMatrix2fv,
    (void *)glUniformMatrix3fv,
    (void *)glUniformMatrix4fv,
    (void *)glUseProgram,
    (void *)glValidateProgram,
    (void *)glVertexAttrib1f,
    (void *)glVertexAttrib1fv,
    (void *)glVertexAttrib2f,
    (void *)glVertexAttrib2fv,
    (void *)glVertexAttrib3f,
    (void *)glVertexAttrib3fv,
    (void *)glVertexAttrib4f,
    (void *)glVertexAttrib4fv,
    (void *)glVertexAttribPointer,
    (void *)glViewport,
    (void *)glReadBuffer,
    (void *)glDrawRangeElements,
    (void *)glTexImage3D,
    (void *)glTexSubImage3D,
    (void *)glCopyTexSubImage3D,
    (void *)glCompressedTexImage3D,
    (void *)glCompressedTexSubImage3D,
    (void *)glGenQueries,
    (void *)glDeleteQueries,
    (void *)glIsQuery,
    (void *)glBeginQuery,
    (void *)glEndQuery,
    (void *)glGetQueryiv,
    (void *)glGetQueryObjectuiv,
    (void *)glUnmapBuffer,
    (void *)glGetBufferPointerv,
    (void *)glDrawBuffers,
    (void *)glUniformMatrix2x3fv,
    (void *)glUniformMatrix3x2fv,
    (void *)glUniformMatrix2x4fv,
    (void *)glUniformMatrix4x2fv,
    (void *)glUniformMatrix3x4fv,
    (void *)glUniformMatrix4x3fv,
    (void *)glBlitFramebuffer,
    (void *)glRenderbufferStorageMultisample,
    (void *)glFramebufferTextureLayer,
    (void *)glMapBufferRange,
    (void *)glFlushMappedBufferRange,
    (void *)glBindVertexArray,
    (void *)glDeleteVertexArrays,
    (void *)glGenVertexArrays,
    (void *)glIsVertexArray,
    (void *)glGetIntegeri_v,
    (void *)glBeginTransformFeedback,
    (void *)glEndTransformFeedback,
    (void *)glBindBufferRange,
    (void *)glBindBufferBase,
    (void *)glTransformFeedbackVaryings,
    (void *)glGetTransformFeedbackVarying,
    (void *)glVertexAttribIPointer,
    (void *)glGetVertexAttribIiv,
    (void *)glGetVertexAttribIuiv,
    (void *)glVertexAttribI4i,
    (void *)glVertexAttribI4ui,
    (void *)glVertexAttribI4iv,
    (void *)glVertexAttribI4uiv,
    (void *)glGetUniformuiv,
    (void *)glGetFragDataLocation,
    (void *)glUniform1ui,
    (void *)glUniform2ui,
    (void *)glUniform3ui,
    (void *)glUniform4ui,
    (void *)glUniform1uiv,
    (void *)glUniform2uiv,
    (void *)glUniform3uiv,
    (void *)glUniform4uiv,
    (void *)glClearBufferiv,
    (void *)glClearBufferuiv,
    (void *)glClearBufferfv,
    (void *)glClearBufferfi,
    (void *)glGetStringi,
    (void *)glCopyBufferSubData,
    (void *)glGetUniformIndices,
    (void *)glGetActiveUniformsiv,
    (void *)glGetUniformBlockIndex,
    (void *)glGetActiveUniformBlockiv,
    (void *)glGetActiveUniformBlockName,
    (void *)glUniformBlockBinding,
    (void *)glDrawArraysInstanced,
    (void *)glDrawElementsInstanced,
    (void *)glFenceSync,
    (void *)glIsSync,
    (void *)glDeleteSync,
    (void *)glClientWaitSync,
    (void *)glWaitSync,
    (void *)glGetInteger64v,
    (void *)glGetSynciv,
    (void *)glGetInteger64i_v,
    (void *)glGetBufferParameteri64v,
    (void *)glGenSamplers,
    (void *)glDeleteSamplers,
    (void *)glIsSampler,
    (void *)glBindSampler,
    (void *)glSamplerParameteri,
    (void *)glSamplerParameteriv,
    (void *)glSamplerParameterf,
    (void *)glSamplerParameterfv,
    (void *)glGetSamplerParameteriv,
    (void *)glGetSamplerParameterfv,
    (void *)glVertexAttribDivisor,
    (void *)glBindTransformFeedback,
    (void *)glDeleteTransformFeedbacks,
    (void *)glGenTransformFeedbacks,
    (void *)glIsTransformFeedback,
    (void *)glPauseTransformFeedback,
    (void *)glResumeTransformFeedback,
    (void *)glGetProgramBinary,
    (void *)glProgramBinary,
    (void *)glProgramParameteri,
    (void *)glInvalidateFramebuffer,
    (void *)glInvalidateSubFramebuffer,
    (void *)glTexStorage2D,
    (void *)glTexStorage3D,
    (void *)glGetInternalformativ,
    (void *)glDispatchCompute,
    (void *)glDispatchComputeIndirect,
    (void *)glDrawArraysIndirect,
    (void *)glDrawElementsIndirect,
    (void *)glFramebufferParameteri,
    (void *)glGetFramebufferParameteriv,
    (void *)glGetProgramInterfaceiv,
    (void *)glGetProgramResourceIndex,
    (void *)glGetProgramResourceName,
    (void *)glGetProgramResourceiv,
    (void *)glGetProgramResourceLocation,
    (void *)glUseProgramStages,
    (void *)glActiveShaderProgram,
    (void *)glCreateShaderProgramv,
    (void *)glBindProgramPipeline,
    (void *)glDeleteProgramPipelines,
    (void *)glGenProgramPipelines,
    (void *)glIsProgramPipeline,
    (void *)glGetProgramPipelineiv,
    (void *)glProgramUniform1i,
    (void *)glProgramUniform2i,
    (void *)glProgramUniform3i,
    (void *)glProgramUniform4i,
    (void *)glProgramUniform1ui,
    (void *)glProgramUniform2ui,
    (void *)glProgramUniform3ui,
    (void *)glProgramUniform4ui,
    (void *)glProgramUniform1f,
    (void *)glProgramUniform2f,
    (void *)glProgramUniform3f,
    (void *)glProgramUniform4f,
    (void *)glProgramUniform1iv,
    (void *)glProgramUniform2iv,
    (void *)glProgramUniform3iv,
    (void *)glProgramUniform4iv,
    (void *)glProgramUniform1uiv,
    (void *)glProgramUniform2uiv,
    (void *)glProgramUniform3uiv,
    (void *)glProgramUniform4uiv,
    (void *)glProgramUniform1fv,
    (void *)glProgramUniform2fv,
    (void *)glProgramUniform3fv,
    (void *)glProgramUniform4fv,
    (void *)glProgramUniformMatrix2fv,
    (void *)glProgramUniformMatrix3fv,
    (void *)glProgramUniformMatrix4fv,
    (void *)glProgramUniformMatrix2x3fv,
    (void *)glProgramUniformMatrix3x2fv,
    (void *)glProgramUniformMatrix2x4fv,
    (void *)glProgramUniformMatrix4x2fv,
    (void *)glProgramUniformMatrix3x4fv,
    (void *)glProgramUniformMatrix4x3fv,
    (void *)glValidateProgramPipeline,
    (void *)glGetProgramPipelineInfoLog,
    (void *)glBindImageTexture,
    (void *)glGetBooleani_v,
    (void *)glMemoryBarrier,
    (void *)glMemoryBarrierByRegion,
    (void *)glTexStorage2DMultisample,
    (void *)glGetMultisamplefv,
    (void *)glSampleMaski,
    (void *)glGetTexLevelParameteriv,
    (void *)glGetTexLevelParameterfv,
    (void *)glBindVertexBuffer,
    (void *)glVertexAttribFormat,
    (void *)glVertexAttribIFormat,
    (void *)glVertexAttribBinding,
    (void *)glVertexBindingDivisor,
    (void *)glBlendBarrierKHR,
    (void *)glDebugMessageControlKHR,
    (void *)glDebugMessageInsertKHR,
    (void *)glDebugMessageCallbackKHR,
    (void *)glGetDebugMessageLogKHR,
    (void *)glPushDebugGroupKHR,
    (void *)glPopDebugGroupKHR,
    (void *)glObjectLabelKHR,
    (void *)glGetObjectLabelKHR,
    (void *)glObjectPtrLabelKHR,
    (void *)glGetObjectPtrLabelKHR,
    (void *)glGetPointervKHR,
    (void *)glMaxShaderCompilerThreadsKHR,
    (void *)glGetGraphicsResetStatusKHR,
    (void *)glReadnPixelsKHR,
    (void *)glGetnUniformfvKHR,
    (void *)glGetnUniformivKHR,
    (void *)glGetnUniformuivKHR,
    (void *)glEGLImageTargetTexture2DOES,
    (void *)glEGLImageTargetRenderbufferStorageOES,
    (void *)glCopyImageSubDataOES,
    (void *)glEnableiOES,
    (void *)glDisableiOES,
    (void *)glBlendEquationiOES,
    (void *)glBlendEquationSeparateiOES,
    (void *)glBlendFunciOES,
    (void *)glBlendFuncSeparateiOES,
    (void *)glColorMaskiOES,
    (void *)glIsEnablediOES,
    (void *)glDrawElementsBaseVertexOES,
    (void *)glDrawRangeElementsBaseVertexOES,
    (void *)glDrawElementsInstancedBaseVertexOES,
    (void *)glMultiDrawElementsBaseVertexEXT,
    (void *)glFramebufferTextureOES,
    (void *)glGetProgramBinaryOES,
    (void *)glProgramBinaryOES,
    (void *)glMapBufferOES,
    (void *)glUnmapBufferOES,
    (void *)glGetBufferPointervOES,
    (void *)glPrimitiveBoundingBoxOES,
    (void *)glMinSampleShadingOES,
    (void *)glPatchParameteriOES,
    (void *)glTexImage3DOES,
    (void *)glTexSubImage3DOES,
    (void *)glCopyTexSubImage3DOES,
    (void *)glCompressedTexImage3DOES,
    (void *)glCompressedTexSubImage3DOES,
    (void *)glFramebufferTexture3DOES,
    (void *)glTexParameterIivOES,
    (void *)glTexParameterIuivOES,
    (void *)glGetTexParameterIivOES,
    (void *)glGetTexParameterIuivOES,
    (void *)glSamplerParameterIivOES,
    (void *)glSamplerParameterIuivOES,
    (void *)glGetSamplerParameterIivOES,
    (void *)glGetSamplerParameterIuivOES,
    (void *)glTexBufferOES,
    (void *)glTexBufferRangeOES,
    (void *)glTexStorage3DMultisampleOES,
    (void *)glTextureViewOES,
    (void *)glBindVertexArrayOES,
    (void *)glDeleteVertexArraysOES,
    (void *)glGenVertexArraysOES,
    (void *)glIsVertexArrayOES,
    (void *)glViewportArrayvOES,
    (void *)glViewportIndexedfOES,
    (void *)glViewportIndexedfvOES,
    (void *)glScissorArrayvOES,
    (void *)glScissorIndexedOES,
    (void *)glScissorIndexedvOES,
    (void *)glDepthRangeArrayfvOES,
    (void *)glDepthRangeIndexedfOES,
    (void *)glGetFloati_vOES,
    (void *)glGetPerfMonitorGroupsAMD,
    (void *)glGetPerfMonitorCountersAMD,
    (void *)glGetPerfMonitorGroupStringAMD,
    (void *)glGetPerfMonitorCounterStringAMD,
    (void *)glGetPerfMonitorCounterInfoAMD,
    (void *)glGenPerfMonitorsAMD,
    (void *)glDeletePerfMonitorsAMD,
    (void *)glSelectPerfMonitorCountersAMD,
    (void *)glBeginPerfMonitorAMD,
    (void *)glEndPerfMonitorAMD,
    (void *)glGetPerfMonitorCounterDataAMD,
    (void *)glBlitFramebufferANGLE,
    (void *)glRenderbufferStorageMultisampleANGLE,
    (void *)glDrawArraysInstancedANGLE,
    (void *)glDrawElementsInstancedANGLE,
    (void *)glVertexAttribDivisorANGLE,
    (void *)glGetTranslatedShaderSourceANGLE,
    (void *)glCopyTextureLevelsAPPLE,
    (void *)glRenderbufferStorageMultisampleAPPLE,
    (void *)glResolveMultisampleFramebufferAPPLE,
    (void *)glFenceSyncAPPLE,
    (void *)glIsSyncAPPLE,
    (void *)glDeleteSyncAPPLE,
    (void *)glClientWaitSyncAPPLE,
    (void *)glWaitSyncAPPLE,
    (void *)glGetInteger64vAPPLE,
    (void *)glGetSyncivAPPLE,
    (void *)glEGLImageTargetTexStorageEXT,
    (void *)glEGLImageTargetTextureStorageEXT,
    (void *)glDrawArraysInstancedBaseInstanceEXT,
    (void *)glDrawElementsInstancedBaseInstanceEXT,
    (void *)glDrawElementsInstancedBaseVertexBaseInstanceEXT,
    (void *)glBindFragDataLocationIndexedEXT,
    (void *)glBindFragDataLocationEXT,
    (void *)glGetProgramResourceLocationIndexEXT,
    (void *)glGetFragDataIndexEXT,
    (void *)glBufferStorageEXT,
    (void *)glClearTexImageEXT,
    (void *)glClearTexSubImageEXT,
    (void *)glClipControlEXT,
    (void *)glCopyImageSubDataEXT,
    (void *)glLabelObjectEXT,
    (void *)glGetObjectLabelEXT,
    (void *)glInsertEventMarkerEXT,
    (void *)glPushGroupMarkerEXT,
    (void *)glPopGroupMarkerEXT,
    (void *)glDiscardFramebufferEXT,
    (void *)glGenQueriesEXT,
    (void *)glDeleteQueriesEXT,
    (void *)glIsQueryEXT,
    (void *)glBeginQueryEXT,
    (void *)glEndQueryEXT,
    (void *)glQueryCounterEXT,
    (void *)glGetQueryivEXT,
    (void *)glGetQueryObjectivEXT,
    (void *)glGetQueryObjectuivEXT,
    (void *)glGetQueryObjecti64vEXT,
    (void *)glGetQueryObjectui64vEXT,
    (void *)glDrawBuffersEXT,
    (void *)glEnableiEXT,
    (void *)glDisableiEXT,
    (void *)glBlendEquationiEXT,
    (void *)glBlendEquationSeparateiEXT,
    (void *)glBlendFunciEXT,
    (void *)glBlendFuncSeparateiEXT,
    (void *)glColorMaskiEXT,
    (void *)glIsEnablediEXT,
    (void *)glDrawElementsBaseVertexEXT,
    (void *)glDrawRangeElementsBaseVertexEXT,
    (void *)glDrawElementsInstancedBaseVertexEXT,
    (void *)glDrawArraysInstancedEXT,
    (void *)glDrawElementsInstancedEXT,
    (void *)glDrawTransformFeedbackEXT,
    (void *)glDrawTransformFeedbackInstancedEXT,
    (void *)glBufferStorageExternalEXT,
    (void *)glNamedBufferStorageExternalEXT,
    (void *)glFramebufferTextureEXT,
    (void *)glVertexAttribDivisorEXT,
    (void *)glMapBufferRangeEXT,
    (void *)glFlushMappedBufferRangeEXT,
    (void *)glGetUnsignedBytevEXT,
    (void *)glGetUnsignedBytei_vEXT,
    (void *)glDeleteMemoryObjectsEXT,
    (void *)glIsMemoryObjectEXT,
    (void *)glCreateMemoryObjectsEXT,
    (void *)glMemoryObjectParameterivEXT,
    (void *)glGetMemoryObjectParameterivEXT,
    (void *)glTexStorageMem2DEXT,
    (void *)glTexStorageMem2DMultisampleEXT,
    (void *)glTexStorageMem3DEXT,
    (void *)glTexStorageMem3DMultisampleEXT,
    (void *)glBufferStorageMemEXT,
    (void *)glTextureStorageMem2DEXT,
    (void *)glTextureStorageMem2DMultisampleEXT,
    (void *)glTextureStorageMem3DEXT,
    (void *)glTextureStorageMem3DMultisampleEXT,
    (void *)glNamedBufferStorageMemEXT,
    (void *)glImportMemoryFdEXT,
    (void *)glImportMemoryWin32HandleEXT,
    (void *)glImportMemoryWin32NameEXT,
    (void *)glMultiDrawArraysEXT,
    (void *)glMultiDrawElementsEXT,
    (void *)glMultiDrawArraysIndirectEXT,
    (void *)glMultiDrawElementsIndirectEXT,
    (void *)glRenderbufferStorageMultisampleEXT,
    (void *)glFramebufferTexture2DMultisampleEXT,
    (void *)glReadBufferIndexedEXT,
    (void *)glDrawBuffersIndexedEXT,
    (void *)glGetIntegeri_vEXT,
    (void *)glPolygonOffsetClampEXT,
    (void *)glPrimitiveBoundingBoxEXT,
    (void *)glRasterSamplesEXT,
    (void *)glGetGraphicsResetStatusEXT,
    (void *)glReadnPixelsEXT,
    (void *)glGetnUniformfvEXT,
    (void *)glGetnUniformivEXT,
    (void *)glGenSemaphoresEXT,
    (void *)glDeleteSemaphoresEXT,
    (void *)glIsSemaphoreEXT,
    (void *)glSemaphoreParameterui64vEXT,
    (void *)glGetSemaphoreParameterui64vEXT,
    (void *)glWaitSemaphoreEXT,
    (void *)glSignalSemaphoreEXT,
    (void *)glImportSemaphoreFdEXT,
    (void *)glImportSemaphoreWin32HandleEXT,
    (void *)glImportSemaphoreWin32NameEXT,
    (void *)glActiveShaderProgramEXT,
    (void *)glBindProgramPipelineEXT,
    (void *)glCreateShaderProgramvEXT,
    (void *)glDeleteProgramPipelinesEXT,
    (void *)glGenProgramPipelinesEXT,
    (void *)glGetProgramPipelineInfoLogEXT,
    (void *)glGetProgramPipelineivEXT,
    (void *)glIsProgramPipelineEXT,
    (void *)glProgramParameteriEXT,
    (void *)glProgramUniform1fEXT,
    (void *)glProgramUniform1fvEXT,
    (void *)glProgramUniform1iEXT,
    (void *)glProgramUniform1ivEXT,
    (void *)glProgramUniform2fEXT,
    (void *)glProgramUniform2fvEXT,
    (void *)glProgramUniform2iEXT,
    (void *)glProgramUniform2ivEXT,
    (void *)glProgramUniform3fEXT,
    (void *)glProgramUniform3fvEXT,
    (void *)glProgramUniform3iEXT,
    (void *)glProgramUniform3ivEXT,
    (void *)glProgramUniform4fEXT,
    (void *)glProgramUniform4fvEXT,
    (void *)glProgramUniform4iEXT,
    (void *)glProgramUniform4ivEXT,
    (void *)glProgramUniformMatrix2fvEXT,
    (void *)glProgramUniformMatrix3fvEXT,
    (void *)glProgramUniformMatrix4fvEXT,
    (void *)glUseProgramStagesEXT,
    (void *)glValidateProgramPipelineEXT,
    (void *)glProgramUniform1uiEXT,
    (void *)glProgramUniform2uiEXT,
    (void *)glProgramUniform3uiEXT,
    (void *)glProgramUniform4uiEXT,
    (void *)glProgramUniform1uivEXT,
    (void *)glProgramUniform2uivEXT,
    (void *)glProgramUniform3uivEXT,
    (void *)glProgramUniform4uivEXT,
    (void *)glProgramUniformMatrix2x3fvEXT,
    (void *)glProgramUniformMatrix3x2fvEXT,
    (void *)glProgramUniformMatrix2x4fvEXT,
    (void *)glProgramUniformMatrix4x2fvEXT,
    (void *)glProgramUniformMatrix3x4fvEXT,
    (void *)glProgramUniformMatrix4x3fvEXT,
    (void *)glFramebufferFetchBarrierEXT,
    (void *)glFramebufferPixelLocalStorageSizeEXT,
    (void *)glGetFramebufferPixelLocalStorageSizeEXT,
    (void *)glClearPixelLocalStorageuiEXT,
    (void *)glTexPageCommitmentEXT,
    (void *)glPatchParameteriEXT,
    (void *)glTexParameterIivEXT,
    (void *)glTexParameterIuivEXT,
    (void *)glGetTexParameterIivEXT,
    (void *)glGetTexParameterIuivEXT,
    (void *)glSamplerParameterIivEXT,
    (void *)glSamplerParameterIuivEXT,
    (void *)glGetSamplerParameterIivEXT,
    (void *)glGetSamplerParameterIuivEXT,
    (void *)glTexBufferEXT,
    (void *)glTexBufferRangeEXT,
    (void *)glTexStorage1DEXT,
    (void *)glTexStorage2DEXT,
    (void *)glTexStorage3DEXT,
    (void *)glTextureStorage1DEXT,
    (void *)glTextureStorage2DEXT,
    (void *)glTextureStorage3DEXT,
    (void *)glTextureViewEXT,
    (void *)glAcquireKeyedMutexWin32EXT,
    (void *)glReleaseKeyedMutexWin32EXT,
    (void *)glWindowRectanglesEXT,
    (void *)glGetTextureHandleIMG,
    (void *)glGetTextureSamplerHandleIMG,
    (void *)glUniformHandleui64IMG,
    (void *)glUniformHandleui64vIMG,
    (void *)glProgramUniformHandleui64IMG,
    (void *)glProgramUniformHandleui64vIMG,
    (void *)glFramebufferTexture2DDownsampleIMG,
    (void *)glFramebufferTextureLayerDownsampleIMG,
    (void *)glRenderbufferStorageMultisampleIMG,
    (void *)glFramebufferTexture2DMultisampleIMG,
    (void *)glApplyFramebufferAttachmentCMAAINTEL,
    (void *)glBeginPerfQueryINTEL,
    (void *)glCreatePerfQueryINTEL,
    (void *)glDeletePerfQueryINTEL,
    (void *)glEndPerfQueryINTEL,
    (void *)glGetFirstPerfQueryIdINTEL,
    (void *)glGetNextPerfQueryIdINTEL,
    (void *)glGetPerfCounterInfoINTEL,
    (void *)glGetPerfQueryDataINTEL,
    (void *)glGetPerfQueryIdByNameINTEL,
    (void *)glGetPerfQueryInfoINTEL,
    (void *)glGetTextureHandleNV,
    (void *)glGetTextureSamplerHandleNV,
    (void *)glMakeTextureHandleResidentNV,
    (void *)glMakeTextureHandleNonResidentNV,
    (void *)glGetImageHandleNV,
    (void *)glMakeImageHandleResidentNV,
    (void *)glMakeImageHandleNonResidentNV,
    (void *)glUniformHandleui64NV,
    (void *)glUniformHandleui64vNV,
    (void *)glProgramUniformHandleui64NV,
    (void *)glProgramUniformHandleui64vNV,
    (void *)glIsTextureHandleResidentNV,
    (void *)glIsImageHandleResidentNV,
    (void *)glBlendParameteriNV,
    (void *)glBlendBarrierNV,
    (void *)glViewportPositionWScaleNV,
    (void *)glBeginConditionalRenderNV,
    (void *)glEndConditionalRenderNV,
    (void *)glSubpixelPrecisionBiasNV,
    (void *)glConservativeRasterParameteriNV,
    (void *)glCopyBufferSubDataNV,
    (void *)glCoverageMaskNV,
    (void *)glCoverageOperationNV,
    (void *)glDrawBuffersNV,
    (void *)glDrawArraysInstancedNV,
    (void *)glDrawElementsInstancedNV,
    (void *)glDrawVkImageNV,
    (void *)glGetVkProcAddrNV,
    (void *)glWaitVkSemaphoreNV,
    (void *)glSignalVkSemaphoreNV,
    (void *)glSignalVkFenceNV,
    (void *)glDeleteFencesNV,
    (void *)glGenFencesNV,
    (void *)glIsFenceNV,
    (void *)glTestFenceNV,
    (void *)glGetFenceivNV,
    (void *)glFinishFenceNV,
    (void *)glSetFenceNV,
    (void *)glFragmentCoverageColorNV,
    (void *)glBlitFramebufferNV,
    (void *)glCoverageModulationTableNV,
    (void *)glGetCoverageModulationTableNV,
    (void *)glCoverageModulationNV,
    (void *)glRenderbufferStorageMultisampleNV,
    (void *)glUniform1i64NV,
    (void *)glUniform2i64NV,
    (void *)glUniform3i64NV,
    (void *)glUniform4i64NV,
    (void *)glUniform1i64vNV,
    (void *)glUniform2i64vNV,
    (void *)glUniform3i64vNV,
    (void *)glUniform4i64vNV,
    (void *)glUniform1ui64NV,
    (void *)glUniform2ui64NV,
    (void *)glUniform3ui64NV,
    (void *)glUniform4ui64NV,
    (void *)glUniform1ui64vNV,
    (void *)glUniform2ui64vNV,
    (void *)glUniform3ui64vNV,
    (void *)glUniform4ui64vNV,
    (void *)glGetUniformi64vNV,
    (void *)glProgramUniform1i64NV,
    (void *)glProgramUniform2i64NV,
    (void *)glProgramUniform3i64NV,
    (void *)glProgramUniform4i64NV,
    (void *)glProgramUniform1i64vNV,
    (void *)glProgramUniform2i64vNV,
    (void *)glProgramUniform3i64vNV,
    (void *)glProgramUniform4i64vNV,
    (void *)glProgramUniform1ui64NV,
    (void *)glProgramUniform2ui64NV,
    (void *)glProgramUniform3ui64NV,
    (void *)glProgramUniform4ui64NV,
    (void *)glProgramUniform1ui64vNV,
    (void *)glProgramUniform2ui64vNV,
    (void *)glProgramUniform3ui64vNV,
    (void *)glProgramUniform4ui64vNV,
    (void *)glVertexAttribDivisorNV,
    (void *)glGetInternalformatSampleivNV,
    (void *)glUniformMatrix2x3fvNV,
    (void *)glUniformMatrix3x2fvNV,
    (void *)glUniformMatrix2x4fvNV,
    (void *)glUniformMatrix4x2fvNV,
    (void *)glUniformMatrix3x4fvNV,
    (void *)glUniformMatrix4x3fvNV,
    (void *)glGenPathsNV,
    (void *)glDeletePathsNV,
    (void *)glIsPathNV,
    (void *)glPathCommandsNV,
    (void *)glPathCoordsNV,
    (void *)glPathSubCommandsNV,
    (void *)glPathSubCoordsNV,
    (void *)glPathStringNV,
    (void *)glPathGlyphsNV,
    (void *)glPathGlyphRangeNV,
    (void *)glWeightPathsNV,
    (void *)glCopyPathNV,
    (void *)glInterpolatePathsNV,
    (void *)glTransformPathNV,
    (void *)glPathParameterivNV,
    (void *)glPathParameteriNV,
    (void *)glPathParameterfvNV,
    (void *)glPathParameterfNV,
    (void *)glPathDashArrayNV,
    (void *)glPathStencilFuncNV,
    (void *)glPathStencilDepthOffsetNV,
    (void *)glStencilFillPathNV,
    (void *)glStencilStrokePathNV,
    (void *)glStencilFillPathInstancedNV,
    (void *)glStencilStrokePathInstancedNV,
    (void *)glPathCoverDepthFuncNV,
    (void *)glCoverFillPathNV,
    (void *)glCoverStrokePathNV,
    (void *)glCoverFillPathInstancedNV,
    (void *)glCoverStrokePathInstancedNV,
    (void *)glGetPathParameterivNV,
    (void *)glGetPathParameterfvNV,
    (void *)glGetPathCommandsNV,
    (void *)glGetPathCoordsNV,
    (void *)glGetPathDashArrayNV,
    (void *)glGetPathMetricsNV,
    (void *)glGetPathMetricRangeNV,
    (void *)glGetPathSpacingNV,
    (void *)glIsPointInFillPathNV,
    (void *)glIsPointInStrokePathNV,
    (void *)glGetPathLengthNV,
    (void *)glPointAlongPathNV,
    (void *)glMatrixLoad3x2fNV,
    (void *)glMatrixLoad3x3fNV,
    (void *)glMatrixLoadTranspose3x3fNV,
    (void *)glMatrixMult3x2fNV,
    (void *)glMatrixMult3x3fNV,
    (void *)glMatrixMultTranspose3x3fNV,
    (void *)glStencilThenCoverFillPathNV,
    (void *)glStencilThenCoverStrokePathNV,
    (void *)glStencilThenCoverFillPathInstancedNV,
    (void *)glStencilThenCoverStrokePathInstancedNV,
    (void *)glPathGlyphIndexArrayNV,
    (void *)glPathMemoryGlyphIndexArrayNV,
    (void *)glProgramPathFragmentInputGenNV,
    (void *)glGetProgramResourcefvNV,
    (void *)glMatrixFrustumEXT,
    (void *)glMatrixLoadIdentityEXT,
    (void *)glMatrixLoadTransposefEXT,
    (void *)glMatrixLoadTransposedEXT,
    (void *)glMatrixLoadfEXT,
    (void *)glMatrixLoaddEXT,
    (void *)glMatrixMultTransposefEXT,
    (void *)glMatrixMultTransposedEXT,
    (void *)glMatrixMultfEXT,
    (void *)glMatrixMultdEXT,
    (void *)glMatrixOrthoEXT,
    (void *)glMatrixPopEXT,
    (void *)glMatrixPushEXT,
    (void *)glMatrixRotatefEXT,
    (void *)glMatrixRotatedEXT,
    (void *)glMatrixScalefEXT,
    (void *)glMatrixScaledEXT,
    (void *)glMatrixTranslatefEXT,
    (void *)glMatrixTranslatedEXT,
    (void *)glPolygonModeNV,
    (void *)glReadBufferNV,
    (void *)glFramebufferSampleLocationsfvNV,
    (void *)glNamedFramebufferSampleLocationsfvNV,
    (void *)glResolveDepthValuesNV,
    (void *)glViewportArrayvNV,
    (void *)glViewportIndexedfNV,
    (void *)glViewportIndexedfvNV,
    (void *)glScissorArrayvNV,
    (void *)glScissorIndexedNV,
    (void *)glScissorIndexedvNV,
    (void *)glDepthRangeArrayfvNV,
    (void *)glDepthRangeIndexedfNV,
    (void *)glGetFloati_vNV,
    (void *)glEnableiNV,
    (void *)glDisableiNV,
    (void *)glIsEnablediNV,
    (void *)glViewportSwizzleNV,
    (void *)glFramebufferTextureMultiviewOVR,
    (void *)glFramebufferTextureMultisampleMultiviewOVR,
    (void *)glAlphaFuncQCOM,
    (void *)glGetDriverControlsQCOM,
    (void *)glGetDriverControlStringQCOM,
    (void *)glEnableDriverControlQCOM,
    (void *)glDisableDriverControlQCOM,
    (void *)glExtGetTexturesQCOM,
    (void *)glExtGetBuffersQCOM,
    (void *)glExtGetRenderbuffersQCOM,
    (void *)glExtGetFramebuffersQCOM,
    (void *)glExtGetTexLevelParameterivQCOM,
    (void *)glExtTexObjectStateOverrideiQCOM,
    (void *)glExtGetTexSubImageQCOM,
    (void *)glExtGetBufferPointervQCOM,
    (void *)glExtGetShadersQCOM,
    (void *)glExtGetProgramsQCOM,
    (void *)glExtIsProgramBinaryQCOM,
    (void *)glExtGetProgramBinarySourceQCOM,
    (void *)glFramebufferFoveationConfigQCOM,
    (void *)glFramebufferFoveationParametersQCOM,
    (void *)glFramebufferFetchBarrierQCOM,
    (void *)glTextureFoveationParametersQCOM,
    (void *)glStartTilingQCOM,
    (void *)glEndTilingQCOM,
    (void *)glAlphaFunc,
    (void *)glClipPlanef,
    (void *)glColor4f,
    (void *)glFogf,
    (void *)glFogfv,
    (void *)glFrustumf,
    (void *)glGetClipPlanef,
    (void *)glGetLightfv,
    (void *)glGetMaterialfv,
    (void *)glGetTexEnvfv,
    (void *)glLightModelf,
    (void *)glLightModelfv,
    (void *)glLightf,
    (void *)glLightfv,
    (void *)glLoadMatrixf,
    (void *)glMaterialf,
    (void *)glMaterialfv,
    (void *)glMultMatrixf,
    (void *)glMultiTexCoord4f,
    (void *)glNormal3f,
    (void *)glOrthof,
    (void *)glPointParameterf,
    (void *)glPointParameterfv,
    (void *)glPointSize,
    (void *)glRotatef,
    (void *)glScalef,
    (void *)glTexEnvf,
    (void *)glTexEnvfv,
    (void *)glTranslatef,
    (void *)glAlphaFuncx,
    (void *)glClearColorx,
    (void *)glClearDepthx,
    (void *)glClientActiveTexture,
    (void *)glClipPlanex,
    (void *)glColor4ub,
    (void *)glColor4x,
    (void *)glColorPointer,
    (void *)glDepthRangex,
    (void *)glDisableClientState,
    (void *)glEnableClientState,
    (void *)glFogx,
    (void *)glFogxv,
    (void *)glFrustumx,
    (void *)glGetClipPlanex,
    (void *)glGetFixedv,
    (void *)glGetLightxv,
    (void *)glGetMaterialxv,
    (void *)glGetPointerv,
    (void *)glGetTexEnviv,
    (void *)glGetTexEnvxv,
    (void *)glGetTexParameterxv,
    (void *)glLightModelx,
    (void *)glLightModelxv,
    (void *)glLightx,
    (void *)glLightxv,
    (void *)glLineWidthx,
    (void *)glLoadIdentity,
    (void *)glLoadMatrixx,
    (void *)glLogicOp,
    (void *)glMaterialx,
    (void *)glMaterialxv,
    (void *)glMatrixMode,
    (void *)glMultMatrixx,
    (void *)glMultiTexCoord4x,
    (void *)glNormal3x,
    (void *)glNormalPointer,
    (void *)glOrthox,
    (void *)glPointParameterx,
    (void *)glPointParameterxv,
    (void *)glPointSizex,
    (void *)glPolygonOffsetx,
    (void *)glPopMatrix,
    (void *)glPushMatrix,
    (void *)glRotatex,
    (void *)glSampleCoveragex,
    (void *)glScalex,
    (void *)glShadeModel,
    (void *)glTexCoordPointer,
    (void *)glTexEnvi,
    (void *)glTexEnvx,
    (void *)glTexEnviv,
    (void *)glTexEnvxv,
    (void *)glTexParameterx,
    (void *)glTexParameterxv,
    (void *)glTranslatex,
    (void *)glVertexPointer,
    (void *)glPointSizePointerOES,
    (void *)glBlendEquationSeparateOES,
    (void *)glBlendFuncSeparateOES,
    (void *)glBlendEquationOES,
    (void *)glDrawTexsOES,
    (void *)glDrawTexiOES,
    (void *)glDrawTexxOES,
    (void *)glDrawTexsvOES,
    (void *)glDrawTexivOES,
    (void *)glDrawTexxvOES,
    (void *)glDrawTexfOES,
    (void *)glDrawTexfvOES,
    (void *)glAlphaFuncxOES,
    (void *)glClearColorxOES,
    (void *)glClearDepthxOES,
    (void *)glClipPlanexOES,
    (void *)glColor4xOES,
    (void *)glDepthRangexOES,
    (void *)glFogxOES,
    (void *)glFogxvOES,
    (void *)glFrustumxOES,
    (void *)glGetClipPlanexOES,
    (void *)glGetFixedvOES,
    (void *)glGetTexEnvxvOES,
    (void *)glGetTexParameterxvOES,
    (void *)glLightModelxOES,
    (void *)glLightModelxvOES,
    (void *)glLightxOES,
    (void *)glLightxvOES,
    (void *)glLineWidthxOES,
    (void *)glLoadMatrixxOES,
    (void *)glMaterialxOES,
    (void *)glMaterialxvOES,
    (void *)glMultMatrixxOES,
    (void *)glMultiTexCoord4xOES,
    (void *)glNormal3xOES,
    (void *)glOrthoxOES,
    (void *)glPointParameterxvOES,
    (void *)glPointSizexOES,
    (void *)glPolygonOffsetxOES,
    (void *)glRotatexOES,
    (void *)glScalexOES,
    (void *)glTexEnvxOES,
    (void *)glTexEnvxvOES,
    (void *)glTexParameterxOES,
    (void *)glTexParameterxvOES,
    (void *)glTranslatexOES,
    (void *)glGetLightxvOES,
    (void *)glGetMaterialxvOES,
    (void *)glPointParameterxOES,
    (void *)glSampleCoveragexOES,
    (void *)glGetTexGenxvOES,
    (void *)glTexGenxOES,
    (void *)glTexGenxvOES,
    (void *)glIsRenderbufferOES,
    (void *)glBindRenderbufferOES,
    (void *)glDeleteRenderbuffersOES,
    (void *)glGenRenderbuffersOES,
    (void *)glRenderbufferStorageOES,
    (void *)glGetRenderbufferParameterivOES,
    (void *)glIsFramebufferOES,
    (void *)glBindFramebufferOES,
    (void *)glDeleteFramebuffersOES,
    (void *)glGenFramebuffersOES,
    (void *)glCheckFramebufferStatusOES,
    (void *)glFramebufferRenderbufferOES,
    (void *)glFramebufferTexture2DOES,
    (void *)glGetFramebufferAttachmentParameterivOES,
    (void *)glGenerateMipmapOES,
    (void *)glCurrentPaletteMatrixOES,
    (void *)glLoadPaletteFromModelViewMatrixOES,
    (void *)glMatrixIndexPointerOES,
    (void *)glWeightPointerOES,
    (void *)glQueryMatrixxOES,
    (void *)glClearDepthfOES,
    (void *)glClipPlanefOES,
    (void *)glDepthRangefOES,
    (void *)glFrustumfOES,
    (void *)glGetClipPlanefOES,
    (void *)glOrthofOES,
    (void *)glTexGenfOES,
    (void *)glTexGenfvOES,
    (void *)glTexGeniOES,
    (void *)glTexGenivOES,
    (void *)glGetTexGenfvOES,
    (void *)glGetTexGenivOES,
    (void *)glClipPlanefIMG,
    (void *)glClipPlanexIMG,
};

static void e_glActiveTexture(GLenum texture) { struct ts *T = TS(); begin(T, 0, 4, 4); put32(T, (uint32_t)texture); }
static void e_glAttachShader(GLuint program, GLuint shader) { struct ts *T = TS(); begin(T, 1, 8, 8); put32(T, (uint32_t)program); put32(T, (uint32_t)shader); }
static void e_glBindAttribLocation(GLuint program, GLuint index, const GLchar * name) { struct ts *T = TS(); uint32_t name_n = name ? (uint32_t)strlen(name) + 1 : NIL; begin(T, 2, 12, 12 + BLOB(name_n)); put32(T, (uint32_t)program); put32(T, (uint32_t)index); putblob(T, name, name_n); }
static void e_glBindBuffer(GLenum target, GLuint buffer);
static void e0_glBindBuffer(GLenum target, GLuint buffer) { struct ts *T = TS(); begin(T, 3, 8, 8); put32(T, (uint32_t)target); put32(T, (uint32_t)buffer); }
static void e_glBindFramebuffer(GLenum target, GLuint framebuffer) { struct ts *T = TS(); begin(T, 4, 8, 8); put32(T, (uint32_t)target); put32(T, (uint32_t)framebuffer); }
static void e_glBindRenderbuffer(GLenum target, GLuint renderbuffer) { struct ts *T = TS(); begin(T, 5, 8, 8); put32(T, (uint32_t)target); put32(T, (uint32_t)renderbuffer); }
static void e_glBindTexture(GLenum target, GLuint texture) { struct ts *T = TS(); begin(T, 6, 8, 8); put32(T, (uint32_t)target); put32(T, (uint32_t)texture); }
static void e_glBlendColor(GLfloat red, GLfloat green, GLfloat blue, GLfloat alpha) { struct ts *T = TS(); begin(T, 7, 16, 16); putf(T, red); putf(T, green); putf(T, blue); putf(T, alpha); }
static void e_glBlendEquation(GLenum mode) { struct ts *T = TS(); begin(T, 8, 4, 4); put32(T, (uint32_t)mode); }
static void e_glBlendEquationSeparate(GLenum modeRGB, GLenum modeAlpha) { struct ts *T = TS(); begin(T, 9, 8, 8); put32(T, (uint32_t)modeRGB); put32(T, (uint32_t)modeAlpha); }
static void e_glBlendFunc(GLenum sfactor, GLenum dfactor) { struct ts *T = TS(); begin(T, 10, 8, 8); put32(T, (uint32_t)sfactor); put32(T, (uint32_t)dfactor); }
static void e_glBlendFuncSeparate(GLenum sfactorRGB, GLenum dfactorRGB, GLenum sfactorAlpha, GLenum dfactorAlpha) { struct ts *T = TS(); begin(T, 11, 16, 16); put32(T, (uint32_t)sfactorRGB); put32(T, (uint32_t)dfactorRGB); put32(T, (uint32_t)sfactorAlpha); put32(T, (uint32_t)dfactorAlpha); }
static void e_glBufferData(GLenum target, GLsizeiptr size, const void * data, GLenum usage) { struct ts *T = TS(); k_glBufferData(target, size, data, usage); uint32_t data_n = data ? (uint32_t)((size) > 0 ? (size) : 0) : NIL; begin(T, 12, 20, 20 + BLOB(data_n)); put32(T, (uint32_t)target); put64(T, (uint64_t)size); putblob(T, data, data_n); put32(T, (uint32_t)usage); }
static void e_glBufferSubData(GLenum target, GLintptr offset, GLsizeiptr size, const void * data) { struct ts *T = TS(); k_glBufferSubData(target, offset, size, data); uint32_t data_n = data ? (uint32_t)((size) > 0 ? (size) : 0) : NIL; begin(T, 13, 24, 24 + BLOB(data_n)); put32(T, (uint32_t)target); put64(T, (uint64_t)offset); put64(T, (uint64_t)size); putblob(T, data, data_n); }
static GLenum e_glCheckFramebufferStatus(GLenum target) { struct ts *T = TS(); begin(T, 14, 4, 4); put32(T, (uint32_t)target); rsync(T); return (GLenum)get32v(T); }
static void e_glClear(GLbitfield mask) { struct ts *T = TS(); begin(T, 15, 4, 4); put32(T, (uint32_t)mask); }
static void e_glClearColor(GLfloat red, GLfloat green, GLfloat blue, GLfloat alpha) { struct ts *T = TS(); begin(T, 16, 16, 16); putf(T, red); putf(T, green); putf(T, blue); putf(T, alpha); }
static void e_glClearDepthf(GLfloat d) { struct ts *T = TS(); begin(T, 17, 4, 4); putf(T, d); }
static void e_glClearStencil(GLint s) { struct ts *T = TS(); begin(T, 18, 4, 4); put32(T, (uint32_t)s); }
static void e_glColorMask(GLboolean red, GLboolean green, GLboolean blue, GLboolean alpha) { struct ts *T = TS(); begin(T, 19, 16, 16); put32(T, (uint32_t)red); put32(T, (uint32_t)green); put32(T, (uint32_t)blue); put32(T, (uint32_t)alpha); }
static void e_glCompileShader(GLuint shader) { struct ts *T = TS(); begin(T, 20, 4, 4); put32(T, (uint32_t)shader); }
static void e_glCompressedTexImage2D(GLenum target, GLint level, GLenum internalformat, GLsizei width, GLsizei height, GLint border, GLsizei imageSize, const void * data);
static void e_glCompressedTexSubImage2D(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLsizei imageSize, const void * data);
static void e_glCopyTexImage2D(GLenum target, GLint level, GLenum internalformat, GLint x, GLint y, GLsizei width, GLsizei height, GLint border) { struct ts *T = TS(); begin(T, 23, 32, 32); put32(T, (uint32_t)target); put32(T, (uint32_t)level); put32(T, (uint32_t)internalformat); put32(T, (uint32_t)x); put32(T, (uint32_t)y); put32(T, (uint32_t)width); put32(T, (uint32_t)height); put32(T, (uint32_t)border); }
static void e_glCopyTexSubImage2D(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint x, GLint y, GLsizei width, GLsizei height) { struct ts *T = TS(); begin(T, 24, 32, 32); put32(T, (uint32_t)target); put32(T, (uint32_t)level); put32(T, (uint32_t)xoffset); put32(T, (uint32_t)yoffset); put32(T, (uint32_t)x); put32(T, (uint32_t)y); put32(T, (uint32_t)width); put32(T, (uint32_t)height); }
static GLuint e_glCreateProgram(void) { struct ts *T = TS(); begin(T, 25, 0, 0); rsync(T); return (GLuint)get32v(T); }
static GLuint e_glCreateShader(GLenum type) { struct ts *T = TS(); begin(T, 26, 4, 4); put32(T, (uint32_t)type); rsync(T); return (GLuint)get32v(T); }
static void e_glCullFace(GLenum mode) { struct ts *T = TS(); begin(T, 27, 4, 4); put32(T, (uint32_t)mode); }
static void e_glDeleteBuffers(GLsizei n, const GLuint * buffers) { struct ts *T = TS(); k_glDeleteBuffers(n, buffers); uint32_t buffers_n = buffers ? (uint32_t)((n * 4) > 0 ? (n * 4) : 0) : NIL; begin(T, 28, 8, 8 + BLOB(buffers_n)); put32(T, (uint32_t)n); putblob(T, buffers, buffers_n); }
static void e_glDeleteFramebuffers(GLsizei n, const GLuint * framebuffers) { struct ts *T = TS(); uint32_t framebuffers_n = framebuffers ? (uint32_t)((n * 4) > 0 ? (n * 4) : 0) : NIL; begin(T, 29, 8, 8 + BLOB(framebuffers_n)); put32(T, (uint32_t)n); putblob(T, framebuffers, framebuffers_n); }
static void e_glDeleteProgram(GLuint program) { struct ts *T = TS(); begin(T, 30, 4, 4); put32(T, (uint32_t)program); }
static void e_glDeleteRenderbuffers(GLsizei n, const GLuint * renderbuffers) { struct ts *T = TS(); uint32_t renderbuffers_n = renderbuffers ? (uint32_t)((n * 4) > 0 ? (n * 4) : 0) : NIL; begin(T, 31, 8, 8 + BLOB(renderbuffers_n)); put32(T, (uint32_t)n); putblob(T, renderbuffers, renderbuffers_n); }
static void e_glDeleteShader(GLuint shader) { struct ts *T = TS(); begin(T, 32, 4, 4); put32(T, (uint32_t)shader); }
static void e_glDeleteTextures(GLsizei n, const GLuint * textures) { struct ts *T = TS(); uint32_t textures_n = textures ? (uint32_t)((n * 4) > 0 ? (n * 4) : 0) : NIL; begin(T, 33, 8, 8 + BLOB(textures_n)); put32(T, (uint32_t)n); putblob(T, textures, textures_n); }
static void e_glDepthFunc(GLenum func) { struct ts *T = TS(); begin(T, 34, 4, 4); put32(T, (uint32_t)func); }
static void e_glDepthMask(GLboolean flag) { struct ts *T = TS(); begin(T, 35, 4, 4); put32(T, (uint32_t)flag); }
static void e_glDepthRangef(GLfloat n, GLfloat f) { struct ts *T = TS(); begin(T, 36, 8, 8); putf(T, n); putf(T, f); }
static void e_glDetachShader(GLuint program, GLuint shader) { struct ts *T = TS(); begin(T, 37, 8, 8); put32(T, (uint32_t)program); put32(T, (uint32_t)shader); }
static void e_glDisable(GLenum cap) { struct ts *T = TS(); begin(T, 38, 4, 4); put32(T, (uint32_t)cap); }
static void e_glDisableVertexAttribArray(GLuint index);
static void e0_glDisableVertexAttribArray(GLuint index) { struct ts *T = TS(); begin(T, 39, 4, 4); put32(T, (uint32_t)index); }
static void e_glDrawArrays(GLenum mode, GLint first, GLsizei count);
static void e_glDrawElements(GLenum mode, GLsizei count, GLenum type, const void * indices);
static void e_glEnable(GLenum cap) { struct ts *T = TS(); begin(T, 42, 4, 4); put32(T, (uint32_t)cap); }
static void e_glEnableVertexAttribArray(GLuint index);
static void e0_glEnableVertexAttribArray(GLuint index) { struct ts *T = TS(); begin(T, 43, 4, 4); put32(T, (uint32_t)index); }
static void e_glFinish(void);
static void e_glFlush(void);
static void e_glFramebufferRenderbuffer(GLenum target, GLenum attachment, GLenum renderbuffertarget, GLuint renderbuffer) { struct ts *T = TS(); begin(T, 46, 16, 16); put32(T, (uint32_t)target); put32(T, (uint32_t)attachment); put32(T, (uint32_t)renderbuffertarget); put32(T, (uint32_t)renderbuffer); }
static void e_glFramebufferTexture2D(GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level) { struct ts *T = TS(); begin(T, 47, 20, 20); put32(T, (uint32_t)target); put32(T, (uint32_t)attachment); put32(T, (uint32_t)textarget); put32(T, (uint32_t)texture); put32(T, (uint32_t)level); }
static void e_glFrontFace(GLenum mode) { struct ts *T = TS(); begin(T, 48, 4, 4); put32(T, (uint32_t)mode); }
static void e_glGenBuffers(GLsizei n, GLuint * buffers) { struct ts *T = TS(); begin(T, 49, 4, 4); put32(T, (uint32_t)n); rsync(T); get(T, buffers, (uint32_t)((n * 4) > 0 ? (n * 4) : 0)); }
static void e_glGenerateMipmap(GLenum target) { struct ts *T = TS(); begin(T, 50, 4, 4); put32(T, (uint32_t)target); }
static void e_glGenFramebuffers(GLsizei n, GLuint * framebuffers) { struct ts *T = TS(); begin(T, 51, 4, 4); put32(T, (uint32_t)n); rsync(T); get(T, framebuffers, (uint32_t)((n * 4) > 0 ? (n * 4) : 0)); }
static void e_glGenRenderbuffers(GLsizei n, GLuint * renderbuffers) { struct ts *T = TS(); begin(T, 52, 4, 4); put32(T, (uint32_t)n); rsync(T); get(T, renderbuffers, (uint32_t)((n * 4) > 0 ? (n * 4) : 0)); }
static void e_glGenTextures(GLsizei n, GLuint * textures) { struct ts *T = TS(); begin(T, 53, 4, 4); put32(T, (uint32_t)n); rsync(T); get(T, textures, (uint32_t)((n * 4) > 0 ? (n * 4) : 0)); }
static void e_glGetActiveAttrib(GLuint program, GLuint index, GLsizei bufSize, GLsizei * length, GLint * size, GLenum * type, GLchar * name) { struct ts *T = TS(); begin(T, 54, 12, 12); put32(T, (uint32_t)program); put32(T, (uint32_t)index); put32(T, (uint32_t)bufSize); rsync(T); get(T, length, (uint32_t)((4) > 0 ? (4) : 0)); get(T, size, (uint32_t)((4) > 0 ? (4) : 0)); get(T, type, (uint32_t)((4) > 0 ? (4) : 0)); get(T, name, (uint32_t)((bufSize) > 0 ? (bufSize) : 0)); }
static void e_glGetActiveUniform(GLuint program, GLuint index, GLsizei bufSize, GLsizei * length, GLint * size, GLenum * type, GLchar * name) { struct ts *T = TS(); begin(T, 55, 12, 12); put32(T, (uint32_t)program); put32(T, (uint32_t)index); put32(T, (uint32_t)bufSize); rsync(T); get(T, length, (uint32_t)((4) > 0 ? (4) : 0)); get(T, size, (uint32_t)((4) > 0 ? (4) : 0)); get(T, type, (uint32_t)((4) > 0 ? (4) : 0)); get(T, name, (uint32_t)((bufSize) > 0 ? (bufSize) : 0)); }
static void e_glGetAttachedShaders(GLuint program, GLsizei maxCount, GLsizei * count, GLuint * shaders) { struct ts *T = TS(); begin(T, 56, 8, 8); put32(T, (uint32_t)program); put32(T, (uint32_t)maxCount); rsync(T); get(T, count, (uint32_t)((4) > 0 ? (4) : 0)); get(T, shaders, (uint32_t)((maxCount * 4) > 0 ? (maxCount * 4) : 0)); }
static GLint e_glGetAttribLocation(GLuint program, const GLchar * name) { struct ts *T = TS(); uint32_t name_n = name ? (uint32_t)strlen(name) + 1 : NIL; begin(T, 57, 8, 8 + BLOB(name_n)); put32(T, (uint32_t)program); putblob(T, name, name_n); rsync(T); return (GLint)get32v(T); }
static void e_glGetBooleanv(GLenum pname, GLboolean * data) { struct ts *T = TS(); k_glGetBooleanv(pname, data); begin(T, 58, 4, 4); put32(T, (uint32_t)pname); rsync(T); get(T, data, (uint32_t)((aemu_get_count(pname)) > 0 ? (aemu_get_count(pname)) : 0)); }
static void e_glGetBufferParameteriv(GLenum target, GLenum pname, GLint * params) { struct ts *T = TS(); begin(T, 59, 8, 8); put32(T, (uint32_t)target); put32(T, (uint32_t)pname); rsync(T); get(T, params, (uint32_t)((4) > 0 ? (4) : 0)); }
static GLenum e_glGetError(void) { struct ts *T = TS(); begin(T, 60, 0, 0); rsync(T); return (GLenum)get32v(T); }
static void e_glGetFloatv(GLenum pname, GLfloat * data) { struct ts *T = TS(); k_glGetFloatv(pname, data); begin(T, 61, 4, 4); put32(T, (uint32_t)pname); rsync(T); get(T, data, (uint32_t)((aemu_get_count(pname) * 4) > 0 ? (aemu_get_count(pname) * 4) : 0)); }
static void e_glGetFramebufferAttachmentParameteriv(GLenum target, GLenum attachment, GLenum pname, GLint * params) { struct ts *T = TS(); begin(T, 62, 12, 12); put32(T, (uint32_t)target); put32(T, (uint32_t)attachment); put32(T, (uint32_t)pname); rsync(T); get(T, params, (uint32_t)((4) > 0 ? (4) : 0)); }
static void e_glGetIntegerv(GLenum pname, GLint * data) { struct ts *T = TS(); k_glGetIntegerv(pname, data); begin(T, 63, 4, 4); put32(T, (uint32_t)pname); rsync(T); get(T, data, (uint32_t)((aemu_get_count(pname) * 4) > 0 ? (aemu_get_count(pname) * 4) : 0)); }
static void e_glGetProgramiv(GLuint program, GLenum pname, GLint * params) { struct ts *T = TS(); begin(T, 64, 8, 8); put32(T, (uint32_t)program); put32(T, (uint32_t)pname); rsync(T); get(T, params, (uint32_t)((4) > 0 ? (4) : 0)); }
static void e_glGetProgramInfoLog(GLuint program, GLsizei bufSize, GLsizei * length, GLchar * infoLog) { struct ts *T = TS(); begin(T, 65, 8, 8); put32(T, (uint32_t)program); put32(T, (uint32_t)bufSize); rsync(T); get(T, length, (uint32_t)((4) > 0 ? (4) : 0)); get(T, infoLog, (uint32_t)((bufSize) > 0 ? (bufSize) : 0)); }
static void e_glGetRenderbufferParameteriv(GLenum target, GLenum pname, GLint * params) { struct ts *T = TS(); begin(T, 66, 8, 8); put32(T, (uint32_t)target); put32(T, (uint32_t)pname); rsync(T); get(T, params, (uint32_t)((4) > 0 ? (4) : 0)); }
static void e_glGetShaderiv(GLuint shader, GLenum pname, GLint * params) { struct ts *T = TS(); begin(T, 67, 8, 8); put32(T, (uint32_t)shader); put32(T, (uint32_t)pname); rsync(T); get(T, params, (uint32_t)((4) > 0 ? (4) : 0)); }
static void e_glGetShaderInfoLog(GLuint shader, GLsizei bufSize, GLsizei * length, GLchar * infoLog) { struct ts *T = TS(); begin(T, 68, 8, 8); put32(T, (uint32_t)shader); put32(T, (uint32_t)bufSize); rsync(T); get(T, length, (uint32_t)((4) > 0 ? (4) : 0)); get(T, infoLog, (uint32_t)((bufSize) > 0 ? (bufSize) : 0)); }
static void e_glGetShaderPrecisionFormat(GLenum shadertype, GLenum precisiontype, GLint * range, GLint * precision) { struct ts *T = TS(); begin(T, 69, 8, 8); put32(T, (uint32_t)shadertype); put32(T, (uint32_t)precisiontype); rsync(T); get(T, range, (uint32_t)((8) > 0 ? (8) : 0)); get(T, precision, (uint32_t)((4) > 0 ? (4) : 0)); }
static void e_glGetShaderSource(GLuint shader, GLsizei bufSize, GLsizei * length, GLchar * source) { struct ts *T = TS(); begin(T, 70, 8, 8); put32(T, (uint32_t)shader); put32(T, (uint32_t)bufSize); rsync(T); get(T, length, (uint32_t)((4) > 0 ? (4) : 0)); get(T, source, (uint32_t)((bufSize) > 0 ? (bufSize) : 0)); }
static const GLubyte * e_glGetString(GLenum name);
static void e_glGetTexParameterfv(GLenum target, GLenum pname, GLfloat * params) { struct ts *T = TS(); begin(T, 72, 8, 8); put32(T, (uint32_t)target); put32(T, (uint32_t)pname); rsync(T); get(T, params, (uint32_t)((4) > 0 ? (4) : 0)); }
static void e_glGetTexParameteriv(GLenum target, GLenum pname, GLint * params) { struct ts *T = TS(); begin(T, 73, 8, 8); put32(T, (uint32_t)target); put32(T, (uint32_t)pname); rsync(T); get(T, params, (uint32_t)((4) > 0 ? (4) : 0)); }
static void e_glGetUniformfv(GLuint program, GLint location, GLfloat * params);
static void e_glGetUniformiv(GLuint program, GLint location, GLint * params);
static GLint e_glGetUniformLocation(GLuint program, const GLchar * name) { struct ts *T = TS(); uint32_t name_n = name ? (uint32_t)strlen(name) + 1 : NIL; begin(T, 76, 8, 8 + BLOB(name_n)); put32(T, (uint32_t)program); putblob(T, name, name_n); rsync(T); return (GLint)get32v(T); }
static void e_glGetVertexAttribfv(GLuint index, GLenum pname, GLfloat * params) { struct ts *T = TS(); k_glGetVertexAttribfv(index, pname, params); begin(T, 77, 8, 8); put32(T, (uint32_t)index); put32(T, (uint32_t)pname); rsync(T); get(T, params, (uint32_t)((pname == 0x8626 ? 16 : 4) > 0 ? (pname == 0x8626 ? 16 : 4) : 0)); }
static void e_glGetVertexAttribiv(GLuint index, GLenum pname, GLint * params) { struct ts *T = TS(); k_glGetVertexAttribiv(index, pname, params); begin(T, 78, 8, 8); put32(T, (uint32_t)index); put32(T, (uint32_t)pname); rsync(T); get(T, params, (uint32_t)((pname == 0x8626 ? 16 : 4) > 0 ? (pname == 0x8626 ? 16 : 4) : 0)); }
static void e_glGetVertexAttribPointerv(GLuint index, GLenum pname, void ** pointer);
static void e_glHint(GLenum target, GLenum mode) { struct ts *T = TS(); begin(T, 80, 8, 8); put32(T, (uint32_t)target); put32(T, (uint32_t)mode); }
static GLboolean e_glIsBuffer(GLuint buffer) { struct ts *T = TS(); begin(T, 81, 4, 4); put32(T, (uint32_t)buffer); rsync(T); return (GLboolean)get32v(T); }
static GLboolean e_glIsEnabled(GLenum cap) { struct ts *T = TS(); begin(T, 82, 4, 4); put32(T, (uint32_t)cap); rsync(T); return (GLboolean)get32v(T); }
static GLboolean e_glIsFramebuffer(GLuint framebuffer) { struct ts *T = TS(); begin(T, 83, 4, 4); put32(T, (uint32_t)framebuffer); rsync(T); return (GLboolean)get32v(T); }
static GLboolean e_glIsProgram(GLuint program) { struct ts *T = TS(); begin(T, 84, 4, 4); put32(T, (uint32_t)program); rsync(T); return (GLboolean)get32v(T); }
static GLboolean e_glIsRenderbuffer(GLuint renderbuffer) { struct ts *T = TS(); begin(T, 85, 4, 4); put32(T, (uint32_t)renderbuffer); rsync(T); return (GLboolean)get32v(T); }
static GLboolean e_glIsShader(GLuint shader) { struct ts *T = TS(); begin(T, 86, 4, 4); put32(T, (uint32_t)shader); rsync(T); return (GLboolean)get32v(T); }
static GLboolean e_glIsTexture(GLuint texture) { struct ts *T = TS(); begin(T, 87, 4, 4); put32(T, (uint32_t)texture); rsync(T); return (GLboolean)get32v(T); }
static void e_glLineWidth(GLfloat width) { struct ts *T = TS(); begin(T, 88, 4, 4); putf(T, width); }
static void e_glLinkProgram(GLuint program) { struct ts *T = TS(); begin(T, 89, 4, 4); put32(T, (uint32_t)program); }
static void e_glPixelStorei(GLenum pname, GLint param) { struct ts *T = TS(); k_glPixelStorei(pname, param); begin(T, 90, 8, 8); put32(T, (uint32_t)pname); put32(T, (uint32_t)param); }
static void e_glPolygonOffset(GLfloat factor, GLfloat units) { struct ts *T = TS(); begin(T, 91, 8, 8); putf(T, factor); putf(T, units); }
static void e_glReadPixels(GLint x, GLint y, GLsizei width, GLsizei height, GLenum format, GLenum type, void * pixels);
static void e_glReleaseShaderCompiler(void) { struct ts *T = TS(); begin(T, 93, 0, 0); }
static void e_glRenderbufferStorage(GLenum target, GLenum internalformat, GLsizei width, GLsizei height) { struct ts *T = TS(); begin(T, 94, 16, 16); put32(T, (uint32_t)target); put32(T, (uint32_t)internalformat); put32(T, (uint32_t)width); put32(T, (uint32_t)height); }
static void e_glSampleCoverage(GLfloat value, GLboolean invert) { struct ts *T = TS(); begin(T, 95, 8, 8); putf(T, value); put32(T, (uint32_t)invert); }
static void e_glScissor(GLint x, GLint y, GLsizei width, GLsizei height) { struct ts *T = TS(); begin(T, 96, 16, 16); put32(T, (uint32_t)x); put32(T, (uint32_t)y); put32(T, (uint32_t)width); put32(T, (uint32_t)height); }
static void e_glShaderBinary(GLsizei count, const GLuint * shaders, GLenum binaryformat, const void * binary, GLsizei length) { struct ts *T = TS(); uint32_t shaders_n = shaders ? (uint32_t)((count * 4) > 0 ? (count * 4) : 0) : NIL; uint32_t binary_n = binary ? (uint32_t)((length) > 0 ? (length) : 0) : NIL; begin(T, 97, 20, 20 + BLOB(shaders_n) + BLOB(binary_n)); put32(T, (uint32_t)count); putblob(T, shaders, shaders_n); put32(T, (uint32_t)binaryformat); putblob(T, binary, binary_n); put32(T, (uint32_t)length); }
static void e_glShaderSource(GLuint shader, GLsizei count, const GLchar *const* string, const GLint * length);
static void e_glStencilFunc(GLenum func, GLint ref, GLuint mask) { struct ts *T = TS(); begin(T, 99, 12, 12); put32(T, (uint32_t)func); put32(T, (uint32_t)ref); put32(T, (uint32_t)mask); }
static void e_glStencilFuncSeparate(GLenum face, GLenum func, GLint ref, GLuint mask) { struct ts *T = TS(); begin(T, 100, 16, 16); put32(T, (uint32_t)face); put32(T, (uint32_t)func); put32(T, (uint32_t)ref); put32(T, (uint32_t)mask); }
static void e_glStencilMask(GLuint mask) { struct ts *T = TS(); begin(T, 101, 4, 4); put32(T, (uint32_t)mask); }
static void e_glStencilMaskSeparate(GLenum face, GLuint mask) { struct ts *T = TS(); begin(T, 102, 8, 8); put32(T, (uint32_t)face); put32(T, (uint32_t)mask); }
static void e_glStencilOp(GLenum fail, GLenum zfail, GLenum zpass) { struct ts *T = TS(); begin(T, 103, 12, 12); put32(T, (uint32_t)fail); put32(T, (uint32_t)zfail); put32(T, (uint32_t)zpass); }
static void e_glStencilOpSeparate(GLenum face, GLenum sfail, GLenum dpfail, GLenum dppass) { struct ts *T = TS(); begin(T, 104, 16, 16); put32(T, (uint32_t)face); put32(T, (uint32_t)sfail); put32(T, (uint32_t)dpfail); put32(T, (uint32_t)dppass); }
static void e_glTexImage2D(GLenum target, GLint level, GLint internalformat, GLsizei width, GLsizei height, GLint border, GLenum format, GLenum type, const void * pixels);
static void e_glTexParameterf(GLenum target, GLenum pname, GLfloat param) { struct ts *T = TS(); begin(T, 106, 12, 12); put32(T, (uint32_t)target); put32(T, (uint32_t)pname); putf(T, param); }
static void e_glTexParameterfv(GLenum target, GLenum pname, const GLfloat * params) { struct ts *T = TS(); uint32_t params_n = params ? (uint32_t)((4) > 0 ? (4) : 0) : NIL; begin(T, 107, 12, 12 + BLOB(params_n)); put32(T, (uint32_t)target); put32(T, (uint32_t)pname); putblob(T, params, params_n); }
static void e_glTexParameteri(GLenum target, GLenum pname, GLint param) { struct ts *T = TS(); begin(T, 108, 12, 12); put32(T, (uint32_t)target); put32(T, (uint32_t)pname); put32(T, (uint32_t)param); }
static void e_glTexParameteriv(GLenum target, GLenum pname, const GLint * params) { struct ts *T = TS(); uint32_t params_n = params ? (uint32_t)((4) > 0 ? (4) : 0) : NIL; begin(T, 109, 12, 12 + BLOB(params_n)); put32(T, (uint32_t)target); put32(T, (uint32_t)pname); putblob(T, params, params_n); }
static void e_glTexSubImage2D(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLenum type, const void * pixels);
static void e_glUniform1f(GLint location, GLfloat v0) { struct ts *T = TS(); begin(T, 111, 8, 8); put32(T, (uint32_t)location); putf(T, v0); }
static void e_glUniform1fv(GLint location, GLsizei count, const GLfloat * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 4) > 0 ? (count * 4) : 0) : NIL; begin(T, 112, 12, 12 + BLOB(value_n)); put32(T, (uint32_t)location); put32(T, (uint32_t)count); putblob(T, value, value_n); }
static void e_glUniform1i(GLint location, GLint v0) { struct ts *T = TS(); begin(T, 113, 8, 8); put32(T, (uint32_t)location); put32(T, (uint32_t)v0); }
static void e_glUniform1iv(GLint location, GLsizei count, const GLint * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 4) > 0 ? (count * 4) : 0) : NIL; begin(T, 114, 12, 12 + BLOB(value_n)); put32(T, (uint32_t)location); put32(T, (uint32_t)count); putblob(T, value, value_n); }
static void e_glUniform2f(GLint location, GLfloat v0, GLfloat v1) { struct ts *T = TS(); begin(T, 115, 12, 12); put32(T, (uint32_t)location); putf(T, v0); putf(T, v1); }
static void e_glUniform2fv(GLint location, GLsizei count, const GLfloat * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 8) > 0 ? (count * 8) : 0) : NIL; begin(T, 116, 12, 12 + BLOB(value_n)); put32(T, (uint32_t)location); put32(T, (uint32_t)count); putblob(T, value, value_n); }
static void e_glUniform2i(GLint location, GLint v0, GLint v1) { struct ts *T = TS(); begin(T, 117, 12, 12); put32(T, (uint32_t)location); put32(T, (uint32_t)v0); put32(T, (uint32_t)v1); }
static void e_glUniform2iv(GLint location, GLsizei count, const GLint * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 8) > 0 ? (count * 8) : 0) : NIL; begin(T, 118, 12, 12 + BLOB(value_n)); put32(T, (uint32_t)location); put32(T, (uint32_t)count); putblob(T, value, value_n); }
static void e_glUniform3f(GLint location, GLfloat v0, GLfloat v1, GLfloat v2) { struct ts *T = TS(); begin(T, 119, 16, 16); put32(T, (uint32_t)location); putf(T, v0); putf(T, v1); putf(T, v2); }
static void e_glUniform3fv(GLint location, GLsizei count, const GLfloat * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 12) > 0 ? (count * 12) : 0) : NIL; begin(T, 120, 12, 12 + BLOB(value_n)); put32(T, (uint32_t)location); put32(T, (uint32_t)count); putblob(T, value, value_n); }
static void e_glUniform3i(GLint location, GLint v0, GLint v1, GLint v2) { struct ts *T = TS(); begin(T, 121, 16, 16); put32(T, (uint32_t)location); put32(T, (uint32_t)v0); put32(T, (uint32_t)v1); put32(T, (uint32_t)v2); }
static void e_glUniform3iv(GLint location, GLsizei count, const GLint * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 12) > 0 ? (count * 12) : 0) : NIL; begin(T, 122, 12, 12 + BLOB(value_n)); put32(T, (uint32_t)location); put32(T, (uint32_t)count); putblob(T, value, value_n); }
static void e_glUniform4f(GLint location, GLfloat v0, GLfloat v1, GLfloat v2, GLfloat v3) { struct ts *T = TS(); begin(T, 123, 20, 20); put32(T, (uint32_t)location); putf(T, v0); putf(T, v1); putf(T, v2); putf(T, v3); }
static void e_glUniform4fv(GLint location, GLsizei count, const GLfloat * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 16) > 0 ? (count * 16) : 0) : NIL; begin(T, 124, 12, 12 + BLOB(value_n)); put32(T, (uint32_t)location); put32(T, (uint32_t)count); putblob(T, value, value_n); }
static void e_glUniform4i(GLint location, GLint v0, GLint v1, GLint v2, GLint v3) { struct ts *T = TS(); begin(T, 125, 20, 20); put32(T, (uint32_t)location); put32(T, (uint32_t)v0); put32(T, (uint32_t)v1); put32(T, (uint32_t)v2); put32(T, (uint32_t)v3); }
static void e_glUniform4iv(GLint location, GLsizei count, const GLint * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 16) > 0 ? (count * 16) : 0) : NIL; begin(T, 126, 12, 12 + BLOB(value_n)); put32(T, (uint32_t)location); put32(T, (uint32_t)count); putblob(T, value, value_n); }
static void e_glUniformMatrix2fv(GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 16) > 0 ? (count * 16) : 0) : NIL; begin(T, 127, 16, 16 + BLOB(value_n)); put32(T, (uint32_t)location); put32(T, (uint32_t)count); put32(T, (uint32_t)transpose); putblob(T, value, value_n); }
static void e_glUniformMatrix3fv(GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 36) > 0 ? (count * 36) : 0) : NIL; begin(T, 128, 16, 16 + BLOB(value_n)); put32(T, (uint32_t)location); put32(T, (uint32_t)count); put32(T, (uint32_t)transpose); putblob(T, value, value_n); }
static void e_glUniformMatrix4fv(GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 64) > 0 ? (count * 64) : 0) : NIL; begin(T, 129, 16, 16 + BLOB(value_n)); put32(T, (uint32_t)location); put32(T, (uint32_t)count); put32(T, (uint32_t)transpose); putblob(T, value, value_n); }
static void e_glUseProgram(GLuint program) { struct ts *T = TS(); begin(T, 130, 4, 4); put32(T, (uint32_t)program); }
static void e_glValidateProgram(GLuint program) { struct ts *T = TS(); begin(T, 131, 4, 4); put32(T, (uint32_t)program); }
static void e_glVertexAttrib1f(GLuint index, GLfloat x) { struct ts *T = TS(); begin(T, 132, 8, 8); put32(T, (uint32_t)index); putf(T, x); }
static void e_glVertexAttrib1fv(GLuint index, const GLfloat * v) { struct ts *T = TS(); uint32_t v_n = v ? (uint32_t)((4) > 0 ? (4) : 0) : NIL; begin(T, 133, 8, 8 + BLOB(v_n)); put32(T, (uint32_t)index); putblob(T, v, v_n); }
static void e_glVertexAttrib2f(GLuint index, GLfloat x, GLfloat y) { struct ts *T = TS(); begin(T, 134, 12, 12); put32(T, (uint32_t)index); putf(T, x); putf(T, y); }
static void e_glVertexAttrib2fv(GLuint index, const GLfloat * v) { struct ts *T = TS(); uint32_t v_n = v ? (uint32_t)((8) > 0 ? (8) : 0) : NIL; begin(T, 135, 8, 8 + BLOB(v_n)); put32(T, (uint32_t)index); putblob(T, v, v_n); }
static void e_glVertexAttrib3f(GLuint index, GLfloat x, GLfloat y, GLfloat z) { struct ts *T = TS(); begin(T, 136, 16, 16); put32(T, (uint32_t)index); putf(T, x); putf(T, y); putf(T, z); }
static void e_glVertexAttrib3fv(GLuint index, const GLfloat * v) { struct ts *T = TS(); uint32_t v_n = v ? (uint32_t)((12) > 0 ? (12) : 0) : NIL; begin(T, 137, 8, 8 + BLOB(v_n)); put32(T, (uint32_t)index); putblob(T, v, v_n); }
static void e_glVertexAttrib4f(GLuint index, GLfloat x, GLfloat y, GLfloat z, GLfloat w) { struct ts *T = TS(); begin(T, 138, 20, 20); put32(T, (uint32_t)index); putf(T, x); putf(T, y); putf(T, z); putf(T, w); }
static void e_glVertexAttrib4fv(GLuint index, const GLfloat * v) { struct ts *T = TS(); uint32_t v_n = v ? (uint32_t)((16) > 0 ? (16) : 0) : NIL; begin(T, 139, 8, 8 + BLOB(v_n)); put32(T, (uint32_t)index); putblob(T, v, v_n); }
static void e_glVertexAttribPointer(GLuint index, GLint size, GLenum type, GLboolean normalized, GLsizei stride, const void * pointer);
static void e_glViewport(GLint x, GLint y, GLsizei width, GLsizei height) { struct ts *T = TS(); begin(T, 141, 16, 16); put32(T, (uint32_t)x); put32(T, (uint32_t)y); put32(T, (uint32_t)width); put32(T, (uint32_t)height); }
static void e_glReadBuffer(GLenum src) { struct ts *T = TS(); begin(T, 142, 4, 4); put32(T, (uint32_t)src); }
static void e_glDrawRangeElements(GLenum mode, GLuint start, GLuint end, GLsizei count, GLenum type, const void * indices);
static void e_glTexImage3D(GLenum target, GLint level, GLint internalformat, GLsizei width, GLsizei height, GLsizei depth, GLint border, GLenum format, GLenum type, const void * pixels);
static void e_glTexSubImage3D(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint zoffset, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, const void * pixels);
static void e_glCopyTexSubImage3D(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint zoffset, GLint x, GLint y, GLsizei width, GLsizei height) { struct ts *T = TS(); begin(T, 146, 36, 36); put32(T, (uint32_t)target); put32(T, (uint32_t)level); put32(T, (uint32_t)xoffset); put32(T, (uint32_t)yoffset); put32(T, (uint32_t)zoffset); put32(T, (uint32_t)x); put32(T, (uint32_t)y); put32(T, (uint32_t)width); put32(T, (uint32_t)height); }
static void e_glCompressedTexImage3D(GLenum target, GLint level, GLenum internalformat, GLsizei width, GLsizei height, GLsizei depth, GLint border, GLsizei imageSize, const void * data);
static void e_glCompressedTexSubImage3D(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint zoffset, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLsizei imageSize, const void * data);
static void e_glGenQueries(GLsizei n, GLuint * ids) { struct ts *T = TS(); begin(T, 149, 4, 4); put32(T, (uint32_t)n); rsync(T); get(T, ids, (uint32_t)((n * 4) > 0 ? (n * 4) : 0)); }
static void e_glDeleteQueries(GLsizei n, const GLuint * ids) { struct ts *T = TS(); uint32_t ids_n = ids ? (uint32_t)((n * 4) > 0 ? (n * 4) : 0) : NIL; begin(T, 150, 8, 8 + BLOB(ids_n)); put32(T, (uint32_t)n); putblob(T, ids, ids_n); }
static GLboolean e_glIsQuery(GLuint id) { struct ts *T = TS(); begin(T, 151, 4, 4); put32(T, (uint32_t)id); rsync(T); return (GLboolean)get32v(T); }
static void e_glBeginQuery(GLenum target, GLuint id) { struct ts *T = TS(); begin(T, 152, 8, 8); put32(T, (uint32_t)target); put32(T, (uint32_t)id); }
static void e_glEndQuery(GLenum target) { struct ts *T = TS(); begin(T, 153, 4, 4); put32(T, (uint32_t)target); }
static void e_glGetQueryiv(GLenum target, GLenum pname, GLint * params) { struct ts *T = TS(); begin(T, 154, 8, 8); put32(T, (uint32_t)target); put32(T, (uint32_t)pname); rsync(T); get(T, params, (uint32_t)((4) > 0 ? (4) : 0)); }
static void e_glGetQueryObjectuiv(GLuint id, GLenum pname, GLuint * params) { struct ts *T = TS(); begin(T, 155, 8, 8); put32(T, (uint32_t)id); put32(T, (uint32_t)pname); rsync(T); get(T, params, (uint32_t)((4) > 0 ? (4) : 0)); }
static GLboolean e_glUnmapBuffer(GLenum target);
static void e_glGetBufferPointerv(GLenum target, GLenum pname, void ** params);
static void e_glDrawBuffers(GLsizei n, const GLenum * bufs) { struct ts *T = TS(); uint32_t bufs_n = bufs ? (uint32_t)((n * 4) > 0 ? (n * 4) : 0) : NIL; begin(T, 158, 8, 8 + BLOB(bufs_n)); put32(T, (uint32_t)n); putblob(T, bufs, bufs_n); }
static void e_glUniformMatrix2x3fv(GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 24) > 0 ? (count * 24) : 0) : NIL; begin(T, 159, 16, 16 + BLOB(value_n)); put32(T, (uint32_t)location); put32(T, (uint32_t)count); put32(T, (uint32_t)transpose); putblob(T, value, value_n); }
static void e_glUniformMatrix3x2fv(GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 24) > 0 ? (count * 24) : 0) : NIL; begin(T, 160, 16, 16 + BLOB(value_n)); put32(T, (uint32_t)location); put32(T, (uint32_t)count); put32(T, (uint32_t)transpose); putblob(T, value, value_n); }
static void e_glUniformMatrix2x4fv(GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 32) > 0 ? (count * 32) : 0) : NIL; begin(T, 161, 16, 16 + BLOB(value_n)); put32(T, (uint32_t)location); put32(T, (uint32_t)count); put32(T, (uint32_t)transpose); putblob(T, value, value_n); }
static void e_glUniformMatrix4x2fv(GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 32) > 0 ? (count * 32) : 0) : NIL; begin(T, 162, 16, 16 + BLOB(value_n)); put32(T, (uint32_t)location); put32(T, (uint32_t)count); put32(T, (uint32_t)transpose); putblob(T, value, value_n); }
static void e_glUniformMatrix3x4fv(GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 48) > 0 ? (count * 48) : 0) : NIL; begin(T, 163, 16, 16 + BLOB(value_n)); put32(T, (uint32_t)location); put32(T, (uint32_t)count); put32(T, (uint32_t)transpose); putblob(T, value, value_n); }
static void e_glUniformMatrix4x3fv(GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 48) > 0 ? (count * 48) : 0) : NIL; begin(T, 164, 16, 16 + BLOB(value_n)); put32(T, (uint32_t)location); put32(T, (uint32_t)count); put32(T, (uint32_t)transpose); putblob(T, value, value_n); }
static void e_glBlitFramebuffer(GLint srcX0, GLint srcY0, GLint srcX1, GLint srcY1, GLint dstX0, GLint dstY0, GLint dstX1, GLint dstY1, GLbitfield mask, GLenum filter) { struct ts *T = TS(); begin(T, 165, 40, 40); put32(T, (uint32_t)srcX0); put32(T, (uint32_t)srcY0); put32(T, (uint32_t)srcX1); put32(T, (uint32_t)srcY1); put32(T, (uint32_t)dstX0); put32(T, (uint32_t)dstY0); put32(T, (uint32_t)dstX1); put32(T, (uint32_t)dstY1); put32(T, (uint32_t)mask); put32(T, (uint32_t)filter); }
static void e_glRenderbufferStorageMultisample(GLenum target, GLsizei samples, GLenum internalformat, GLsizei width, GLsizei height) { struct ts *T = TS(); begin(T, 166, 20, 20); put32(T, (uint32_t)target); put32(T, (uint32_t)samples); put32(T, (uint32_t)internalformat); put32(T, (uint32_t)width); put32(T, (uint32_t)height); }
static void e_glFramebufferTextureLayer(GLenum target, GLenum attachment, GLuint texture, GLint level, GLint layer) { struct ts *T = TS(); begin(T, 167, 20, 20); put32(T, (uint32_t)target); put32(T, (uint32_t)attachment); put32(T, (uint32_t)texture); put32(T, (uint32_t)level); put32(T, (uint32_t)layer); }
static void * e_glMapBufferRange(GLenum target, GLintptr offset, GLsizeiptr length, GLbitfield access);
static void e_glFlushMappedBufferRange(GLenum target, GLintptr offset, GLsizeiptr length);
static void e_glBindVertexArray(GLuint array) { struct ts *T = TS(); k_glBindVertexArray(array); begin(T, 170, 4, 4); put32(T, (uint32_t)array); }
static void e_glDeleteVertexArrays(GLsizei n, const GLuint * arrays) { struct ts *T = TS(); k_glDeleteVertexArrays(n, arrays); uint32_t arrays_n = arrays ? (uint32_t)((n * 4) > 0 ? (n * 4) : 0) : NIL; begin(T, 171, 8, 8 + BLOB(arrays_n)); put32(T, (uint32_t)n); putblob(T, arrays, arrays_n); }
static void e_glGenVertexArrays(GLsizei n, GLuint * arrays) { struct ts *T = TS(); begin(T, 172, 4, 4); put32(T, (uint32_t)n); rsync(T); get(T, arrays, (uint32_t)((n * 4) > 0 ? (n * 4) : 0)); }
static GLboolean e_glIsVertexArray(GLuint array) { struct ts *T = TS(); begin(T, 173, 4, 4); put32(T, (uint32_t)array); rsync(T); return (GLboolean)get32v(T); }
static void e_glGetIntegeri_v(GLenum target, GLuint index, GLint * data) { struct ts *T = TS(); begin(T, 174, 8, 8); put32(T, (uint32_t)target); put32(T, (uint32_t)index); rsync(T); get(T, data, (uint32_t)((4) > 0 ? (4) : 0)); }
static void e_glBeginTransformFeedback(GLenum primitiveMode) { struct ts *T = TS(); k_glBeginTransformFeedback(primitiveMode); begin(T, 175, 4, 4); put32(T, (uint32_t)primitiveMode); }
static void e_glEndTransformFeedback(void) { struct ts *T = TS(); begin(T, 176, 0, 0); }
static void e_glBindBufferRange(GLenum target, GLuint index, GLuint buffer, GLintptr offset, GLsizeiptr size) { struct ts *T = TS(); k_glBindBufferRange(target, index, buffer, offset, size); begin(T, 177, 28, 28); put32(T, (uint32_t)target); put32(T, (uint32_t)index); put32(T, (uint32_t)buffer); put64(T, (uint64_t)offset); put64(T, (uint64_t)size); }
static void e_glBindBufferBase(GLenum target, GLuint index, GLuint buffer) { struct ts *T = TS(); k_glBindBufferBase(target, index, buffer); begin(T, 178, 12, 12); put32(T, (uint32_t)target); put32(T, (uint32_t)index); put32(T, (uint32_t)buffer); }
static void e_glTransformFeedbackVaryings(GLuint program, GLsizei count, const GLchar *const* varyings, GLenum bufferMode);
static void e_glGetTransformFeedbackVarying(GLuint program, GLuint index, GLsizei bufSize, GLsizei * length, GLsizei * size, GLenum * type, GLchar * name) { struct ts *T = TS(); begin(T, 180, 12, 12); put32(T, (uint32_t)program); put32(T, (uint32_t)index); put32(T, (uint32_t)bufSize); rsync(T); get(T, length, (uint32_t)((4) > 0 ? (4) : 0)); get(T, size, (uint32_t)((4) > 0 ? (4) : 0)); get(T, type, (uint32_t)((4) > 0 ? (4) : 0)); get(T, name, (uint32_t)((bufSize) > 0 ? (bufSize) : 0)); }
static void e_glVertexAttribIPointer(GLuint index, GLint size, GLenum type, GLsizei stride, const void * pointer);
static void e_glGetVertexAttribIiv(GLuint index, GLenum pname, GLint * params) { struct ts *T = TS(); k_glGetVertexAttribIiv(index, pname, params); begin(T, 182, 8, 8); put32(T, (uint32_t)index); put32(T, (uint32_t)pname); rsync(T); get(T, params, (uint32_t)((pname == 0x8626 ? 16 : 4) > 0 ? (pname == 0x8626 ? 16 : 4) : 0)); }
static void e_glGetVertexAttribIuiv(GLuint index, GLenum pname, GLuint * params) { struct ts *T = TS(); k_glGetVertexAttribIuiv(index, pname, params); begin(T, 183, 8, 8); put32(T, (uint32_t)index); put32(T, (uint32_t)pname); rsync(T); get(T, params, (uint32_t)((pname == 0x8626 ? 16 : 4) > 0 ? (pname == 0x8626 ? 16 : 4) : 0)); }
static void e_glVertexAttribI4i(GLuint index, GLint x, GLint y, GLint z, GLint w) { struct ts *T = TS(); begin(T, 184, 20, 20); put32(T, (uint32_t)index); put32(T, (uint32_t)x); put32(T, (uint32_t)y); put32(T, (uint32_t)z); put32(T, (uint32_t)w); }
static void e_glVertexAttribI4ui(GLuint index, GLuint x, GLuint y, GLuint z, GLuint w) { struct ts *T = TS(); begin(T, 185, 20, 20); put32(T, (uint32_t)index); put32(T, (uint32_t)x); put32(T, (uint32_t)y); put32(T, (uint32_t)z); put32(T, (uint32_t)w); }
static void e_glVertexAttribI4iv(GLuint index, const GLint * v) { struct ts *T = TS(); uint32_t v_n = v ? (uint32_t)((16) > 0 ? (16) : 0) : NIL; begin(T, 186, 8, 8 + BLOB(v_n)); put32(T, (uint32_t)index); putblob(T, v, v_n); }
static void e_glVertexAttribI4uiv(GLuint index, const GLuint * v) { struct ts *T = TS(); uint32_t v_n = v ? (uint32_t)((16) > 0 ? (16) : 0) : NIL; begin(T, 187, 8, 8 + BLOB(v_n)); put32(T, (uint32_t)index); putblob(T, v, v_n); }
static void e_glGetUniformuiv(GLuint program, GLint location, GLuint * params);
static GLint e_glGetFragDataLocation(GLuint program, const GLchar * name) { struct ts *T = TS(); uint32_t name_n = name ? (uint32_t)strlen(name) + 1 : NIL; begin(T, 189, 8, 8 + BLOB(name_n)); put32(T, (uint32_t)program); putblob(T, name, name_n); rsync(T); return (GLint)get32v(T); }
static void e_glUniform1ui(GLint location, GLuint v0) { struct ts *T = TS(); begin(T, 190, 8, 8); put32(T, (uint32_t)location); put32(T, (uint32_t)v0); }
static void e_glUniform2ui(GLint location, GLuint v0, GLuint v1) { struct ts *T = TS(); begin(T, 191, 12, 12); put32(T, (uint32_t)location); put32(T, (uint32_t)v0); put32(T, (uint32_t)v1); }
static void e_glUniform3ui(GLint location, GLuint v0, GLuint v1, GLuint v2) { struct ts *T = TS(); begin(T, 192, 16, 16); put32(T, (uint32_t)location); put32(T, (uint32_t)v0); put32(T, (uint32_t)v1); put32(T, (uint32_t)v2); }
static void e_glUniform4ui(GLint location, GLuint v0, GLuint v1, GLuint v2, GLuint v3) { struct ts *T = TS(); begin(T, 193, 20, 20); put32(T, (uint32_t)location); put32(T, (uint32_t)v0); put32(T, (uint32_t)v1); put32(T, (uint32_t)v2); put32(T, (uint32_t)v3); }
static void e_glUniform1uiv(GLint location, GLsizei count, const GLuint * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 4) > 0 ? (count * 4) : 0) : NIL; begin(T, 194, 12, 12 + BLOB(value_n)); put32(T, (uint32_t)location); put32(T, (uint32_t)count); putblob(T, value, value_n); }
static void e_glUniform2uiv(GLint location, GLsizei count, const GLuint * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 8) > 0 ? (count * 8) : 0) : NIL; begin(T, 195, 12, 12 + BLOB(value_n)); put32(T, (uint32_t)location); put32(T, (uint32_t)count); putblob(T, value, value_n); }
static void e_glUniform3uiv(GLint location, GLsizei count, const GLuint * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 12) > 0 ? (count * 12) : 0) : NIL; begin(T, 196, 12, 12 + BLOB(value_n)); put32(T, (uint32_t)location); put32(T, (uint32_t)count); putblob(T, value, value_n); }
static void e_glUniform4uiv(GLint location, GLsizei count, const GLuint * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 16) > 0 ? (count * 16) : 0) : NIL; begin(T, 197, 12, 12 + BLOB(value_n)); put32(T, (uint32_t)location); put32(T, (uint32_t)count); putblob(T, value, value_n); }
static void e_glClearBufferiv(GLenum buffer, GLint drawbuffer, const GLint * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((buffer == 0x1800 ? 16 : 4) > 0 ? (buffer == 0x1800 ? 16 : 4) : 0) : NIL; begin(T, 198, 12, 12 + BLOB(value_n)); put32(T, (uint32_t)buffer); put32(T, (uint32_t)drawbuffer); putblob(T, value, value_n); }
static void e_glClearBufferuiv(GLenum buffer, GLint drawbuffer, const GLuint * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((buffer == 0x1800 ? 16 : 4) > 0 ? (buffer == 0x1800 ? 16 : 4) : 0) : NIL; begin(T, 199, 12, 12 + BLOB(value_n)); put32(T, (uint32_t)buffer); put32(T, (uint32_t)drawbuffer); putblob(T, value, value_n); }
static void e_glClearBufferfv(GLenum buffer, GLint drawbuffer, const GLfloat * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((buffer == 0x1800 ? 16 : 4) > 0 ? (buffer == 0x1800 ? 16 : 4) : 0) : NIL; begin(T, 200, 12, 12 + BLOB(value_n)); put32(T, (uint32_t)buffer); put32(T, (uint32_t)drawbuffer); putblob(T, value, value_n); }
static void e_glClearBufferfi(GLenum buffer, GLint drawbuffer, GLfloat depth, GLint stencil) { struct ts *T = TS(); begin(T, 201, 16, 16); put32(T, (uint32_t)buffer); put32(T, (uint32_t)drawbuffer); putf(T, depth); put32(T, (uint32_t)stencil); }
static const GLubyte * e_glGetStringi(GLenum name, GLuint index);
static void e_glCopyBufferSubData(GLenum readTarget, GLenum writeTarget, GLintptr readOffset, GLintptr writeOffset, GLsizeiptr size) { struct ts *T = TS(); k_glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size); begin(T, 203, 32, 32); put32(T, (uint32_t)readTarget); put32(T, (uint32_t)writeTarget); put64(T, (uint64_t)readOffset); put64(T, (uint64_t)writeOffset); put64(T, (uint64_t)size); }
static void e_glGetUniformIndices(GLuint program, GLsizei uniformCount, const GLchar *const* uniformNames, GLuint * uniformIndices);
static void e_glGetActiveUniformsiv(GLuint program, GLsizei uniformCount, const GLuint * uniformIndices, GLenum pname, GLint * params) { struct ts *T = TS(); uint32_t uniformIndices_n = uniformIndices ? (uint32_t)((uniformCount * 4) > 0 ? (uniformCount * 4) : 0) : NIL; begin(T, 205, 16, 16 + BLOB(uniformIndices_n)); put32(T, (uint32_t)program); put32(T, (uint32_t)uniformCount); putblob(T, uniformIndices, uniformIndices_n); put32(T, (uint32_t)pname); rsync(T); get(T, params, (uint32_t)((uniformCount * 4) > 0 ? (uniformCount * 4) : 0)); }
static GLuint e_glGetUniformBlockIndex(GLuint program, const GLchar * uniformBlockName) { struct ts *T = TS(); uint32_t uniformBlockName_n = uniformBlockName ? (uint32_t)strlen(uniformBlockName) + 1 : NIL; begin(T, 206, 8, 8 + BLOB(uniformBlockName_n)); put32(T, (uint32_t)program); putblob(T, uniformBlockName, uniformBlockName_n); rsync(T); return (GLuint)get32v(T); }
static void e_glGetActiveUniformBlockiv(GLuint program, GLuint uniformBlockIndex, GLenum pname, GLint * params) { struct ts *T = TS(); begin(T, 207, 12, 12); put32(T, (uint32_t)program); put32(T, (uint32_t)uniformBlockIndex); put32(T, (uint32_t)pname); rsync(T); get(T, params, (uint32_t)((4) > 0 ? (4) : 0)); }
static void e_glGetActiveUniformBlockName(GLuint program, GLuint uniformBlockIndex, GLsizei bufSize, GLsizei * length, GLchar * uniformBlockName) { struct ts *T = TS(); begin(T, 208, 12, 12); put32(T, (uint32_t)program); put32(T, (uint32_t)uniformBlockIndex); put32(T, (uint32_t)bufSize); rsync(T); get(T, length, (uint32_t)((4) > 0 ? (4) : 0)); get(T, uniformBlockName, (uint32_t)((bufSize) > 0 ? (bufSize) : 0)); }
static void e_glUniformBlockBinding(GLuint program, GLuint uniformBlockIndex, GLuint uniformBlockBinding) { struct ts *T = TS(); begin(T, 209, 12, 12); put32(T, (uint32_t)program); put32(T, (uint32_t)uniformBlockIndex); put32(T, (uint32_t)uniformBlockBinding); }
static void e_glDrawArraysInstanced(GLenum mode, GLint first, GLsizei count, GLsizei instancecount);
static void e_glDrawElementsInstanced(GLenum mode, GLsizei count, GLenum type, const void * indices, GLsizei instancecount);
static GLsync e_glFenceSync(GLenum condition, GLbitfield flags) { struct ts *T = TS(); begin(T, 212, 8, 8); put32(T, (uint32_t)condition); put32(T, (uint32_t)flags); rsync(T); return (GLsync)get64v(T); }
static GLboolean e_glIsSync(GLsync sync) { struct ts *T = TS(); begin(T, 213, 8, 8); put64(T, (uint64_t)sync); rsync(T); return (GLboolean)get32v(T); }
static void e_glDeleteSync(GLsync sync) { struct ts *T = TS(); begin(T, 214, 8, 8); put64(T, (uint64_t)sync); }
static GLenum e_glClientWaitSync(GLsync sync, GLbitfield flags, GLuint64 timeout) { struct ts *T = TS(); begin(T, 215, 20, 20); put64(T, (uint64_t)sync); put32(T, (uint32_t)flags); put64(T, (uint64_t)timeout); rsync(T); return (GLenum)get32v(T); }
static void e_glWaitSync(GLsync sync, GLbitfield flags, GLuint64 timeout) { struct ts *T = TS(); begin(T, 216, 20, 20); put64(T, (uint64_t)sync); put32(T, (uint32_t)flags); put64(T, (uint64_t)timeout); }
static void e_glGetInteger64v(GLenum pname, GLint64 * data) { struct ts *T = TS(); k_glGetInteger64v(pname, data); begin(T, 217, 4, 4); put32(T, (uint32_t)pname); rsync(T); get(T, data, (uint32_t)((aemu_get_count(pname) * 8) > 0 ? (aemu_get_count(pname) * 8) : 0)); }
static void e_glGetSynciv(GLsync sync, GLenum pname, GLsizei bufSize, GLsizei * length, GLint * values) { struct ts *T = TS(); begin(T, 218, 16, 16); put64(T, (uint64_t)sync); put32(T, (uint32_t)pname); put32(T, (uint32_t)bufSize); rsync(T); get(T, length, (uint32_t)((4) > 0 ? (4) : 0)); get(T, values, (uint32_t)((bufSize * 4) > 0 ? (bufSize * 4) : 0)); }
static void e_glGetInteger64i_v(GLenum target, GLuint index, GLint64 * data) { struct ts *T = TS(); begin(T, 219, 8, 8); put32(T, (uint32_t)target); put32(T, (uint32_t)index); rsync(T); get(T, data, (uint32_t)((8) > 0 ? (8) : 0)); }
static void e_glGetBufferParameteri64v(GLenum target, GLenum pname, GLint64 * params) { struct ts *T = TS(); begin(T, 220, 8, 8); put32(T, (uint32_t)target); put32(T, (uint32_t)pname); rsync(T); get(T, params, (uint32_t)((8) > 0 ? (8) : 0)); }
static void e_glGenSamplers(GLsizei count, GLuint * samplers) { struct ts *T = TS(); begin(T, 221, 4, 4); put32(T, (uint32_t)count); rsync(T); get(T, samplers, (uint32_t)((count * 4) > 0 ? (count * 4) : 0)); }
static void e_glDeleteSamplers(GLsizei count, const GLuint * samplers) { struct ts *T = TS(); uint32_t samplers_n = samplers ? (uint32_t)((count * 4) > 0 ? (count * 4) : 0) : NIL; begin(T, 222, 8, 8 + BLOB(samplers_n)); put32(T, (uint32_t)count); putblob(T, samplers, samplers_n); }
static GLboolean e_glIsSampler(GLuint sampler) { struct ts *T = TS(); begin(T, 223, 4, 4); put32(T, (uint32_t)sampler); rsync(T); return (GLboolean)get32v(T); }
static void e_glBindSampler(GLuint unit, GLuint sampler) { struct ts *T = TS(); begin(T, 224, 8, 8); put32(T, (uint32_t)unit); put32(T, (uint32_t)sampler); }
static void e_glSamplerParameteri(GLuint sampler, GLenum pname, GLint param) { struct ts *T = TS(); begin(T, 225, 12, 12); put32(T, (uint32_t)sampler); put32(T, (uint32_t)pname); put32(T, (uint32_t)param); }
static void e_glSamplerParameteriv(GLuint sampler, GLenum pname, const GLint * param) { struct ts *T = TS(); uint32_t param_n = param ? (uint32_t)((4) > 0 ? (4) : 0) : NIL; begin(T, 226, 12, 12 + BLOB(param_n)); put32(T, (uint32_t)sampler); put32(T, (uint32_t)pname); putblob(T, param, param_n); }
static void e_glSamplerParameterf(GLuint sampler, GLenum pname, GLfloat param) { struct ts *T = TS(); begin(T, 227, 12, 12); put32(T, (uint32_t)sampler); put32(T, (uint32_t)pname); putf(T, param); }
static void e_glSamplerParameterfv(GLuint sampler, GLenum pname, const GLfloat * param) { struct ts *T = TS(); uint32_t param_n = param ? (uint32_t)((4) > 0 ? (4) : 0) : NIL; begin(T, 228, 12, 12 + BLOB(param_n)); put32(T, (uint32_t)sampler); put32(T, (uint32_t)pname); putblob(T, param, param_n); }
static void e_glGetSamplerParameteriv(GLuint sampler, GLenum pname, GLint * params) { struct ts *T = TS(); begin(T, 229, 8, 8); put32(T, (uint32_t)sampler); put32(T, (uint32_t)pname); rsync(T); get(T, params, (uint32_t)((4) > 0 ? (4) : 0)); }
static void e_glGetSamplerParameterfv(GLuint sampler, GLenum pname, GLfloat * params) { struct ts *T = TS(); begin(T, 230, 8, 8); put32(T, (uint32_t)sampler); put32(T, (uint32_t)pname); rsync(T); get(T, params, (uint32_t)((4) > 0 ? (4) : 0)); }
static void e_glVertexAttribDivisor(GLuint index, GLuint divisor) { struct ts *T = TS(); k_glVertexAttribDivisor(index, divisor); begin(T, 231, 8, 8); put32(T, (uint32_t)index); put32(T, (uint32_t)divisor); }
static void e_glBindTransformFeedback(GLenum target, GLuint id) { struct ts *T = TS(); begin(T, 232, 8, 8); put32(T, (uint32_t)target); put32(T, (uint32_t)id); }
static void e_glDeleteTransformFeedbacks(GLsizei n, const GLuint * ids) { struct ts *T = TS(); uint32_t ids_n = ids ? (uint32_t)((n * 4) > 0 ? (n * 4) : 0) : NIL; begin(T, 233, 8, 8 + BLOB(ids_n)); put32(T, (uint32_t)n); putblob(T, ids, ids_n); }
static void e_glGenTransformFeedbacks(GLsizei n, GLuint * ids) { struct ts *T = TS(); begin(T, 234, 4, 4); put32(T, (uint32_t)n); rsync(T); get(T, ids, (uint32_t)((n * 4) > 0 ? (n * 4) : 0)); }
static GLboolean e_glIsTransformFeedback(GLuint id) { struct ts *T = TS(); begin(T, 235, 4, 4); put32(T, (uint32_t)id); rsync(T); return (GLboolean)get32v(T); }
static void e_glPauseTransformFeedback(void) { struct ts *T = TS(); begin(T, 236, 0, 0); }
static void e_glResumeTransformFeedback(void) { struct ts *T = TS(); begin(T, 237, 0, 0); }
static void e_glGetProgramBinary(GLuint program, GLsizei bufSize, GLsizei * length, GLenum * binaryFormat, void * binary) { struct ts *T = TS(); begin(T, 238, 8, 8); put32(T, (uint32_t)program); put32(T, (uint32_t)bufSize); rsync(T); get(T, length, (uint32_t)((4) > 0 ? (4) : 0)); get(T, binaryFormat, (uint32_t)((4) > 0 ? (4) : 0)); get(T, binary, (uint32_t)((bufSize) > 0 ? (bufSize) : 0)); }
static void e_glProgramBinary(GLuint program, GLenum binaryFormat, const void * binary, GLsizei length) { struct ts *T = TS(); uint32_t binary_n = binary ? (uint32_t)((length) > 0 ? (length) : 0) : NIL; begin(T, 239, 16, 16 + BLOB(binary_n)); put32(T, (uint32_t)program); put32(T, (uint32_t)binaryFormat); putblob(T, binary, binary_n); put32(T, (uint32_t)length); }
static void e_glProgramParameteri(GLuint program, GLenum pname, GLint value) { struct ts *T = TS(); begin(T, 240, 12, 12); put32(T, (uint32_t)program); put32(T, (uint32_t)pname); put32(T, (uint32_t)value); }
static void e_glInvalidateFramebuffer(GLenum target, GLsizei numAttachments, const GLenum * attachments) { struct ts *T = TS(); uint32_t attachments_n = attachments ? (uint32_t)((numAttachments * 4) > 0 ? (numAttachments * 4) : 0) : NIL; begin(T, 241, 12, 12 + BLOB(attachments_n)); put32(T, (uint32_t)target); put32(T, (uint32_t)numAttachments); putblob(T, attachments, attachments_n); }
static void e_glInvalidateSubFramebuffer(GLenum target, GLsizei numAttachments, const GLenum * attachments, GLint x, GLint y, GLsizei width, GLsizei height) { struct ts *T = TS(); uint32_t attachments_n = attachments ? (uint32_t)((numAttachments * 4) > 0 ? (numAttachments * 4) : 0) : NIL; begin(T, 242, 28, 28 + BLOB(attachments_n)); put32(T, (uint32_t)target); put32(T, (uint32_t)numAttachments); putblob(T, attachments, attachments_n); put32(T, (uint32_t)x); put32(T, (uint32_t)y); put32(T, (uint32_t)width); put32(T, (uint32_t)height); }
static void e_glTexStorage2D(GLenum target, GLsizei levels, GLenum internalformat, GLsizei width, GLsizei height) { struct ts *T = TS(); begin(T, 243, 20, 20); put32(T, (uint32_t)target); put32(T, (uint32_t)levels); put32(T, (uint32_t)internalformat); put32(T, (uint32_t)width); put32(T, (uint32_t)height); }
static void e_glTexStorage3D(GLenum target, GLsizei levels, GLenum internalformat, GLsizei width, GLsizei height, GLsizei depth) { struct ts *T = TS(); begin(T, 244, 24, 24); put32(T, (uint32_t)target); put32(T, (uint32_t)levels); put32(T, (uint32_t)internalformat); put32(T, (uint32_t)width); put32(T, (uint32_t)height); put32(T, (uint32_t)depth); }
static void e_glGetInternalformativ(GLenum target, GLenum internalformat, GLenum pname, GLsizei bufSize, GLint * params) { struct ts *T = TS(); begin(T, 245, 16, 16); put32(T, (uint32_t)target); put32(T, (uint32_t)internalformat); put32(T, (uint32_t)pname); put32(T, (uint32_t)bufSize); rsync(T); get(T, params, (uint32_t)((bufSize * 4) > 0 ? (bufSize * 4) : 0)); }
static void e_glDispatchCompute(GLuint num_groups_x, GLuint num_groups_y, GLuint num_groups_z) { struct ts *T = TS(); k_glDispatchCompute(num_groups_x, num_groups_y, num_groups_z); begin(T, 246, 12, 12); put32(T, (uint32_t)num_groups_x); put32(T, (uint32_t)num_groups_y); put32(T, (uint32_t)num_groups_z); }
static void e_glDispatchComputeIndirect(GLintptr indirect) { struct ts *T = TS(); k_glDispatchComputeIndirect(indirect); begin(T, 247, 8, 8); put64(T, (uint64_t)indirect); }
static void e_glDrawArraysIndirect(GLenum mode, const void * indirect) { struct ts *T = TS(); k_glDrawArraysIndirect(mode, indirect); begin(T, 248, 12, 12); put32(T, (uint32_t)mode); put64(T, (uint64_t)(uintptr_t)indirect); }
static void e_glDrawElementsIndirect(GLenum mode, GLenum type, const void * indirect) { struct ts *T = TS(); k_glDrawElementsIndirect(mode, type, indirect); begin(T, 249, 16, 16); put32(T, (uint32_t)mode); put32(T, (uint32_t)type); put64(T, (uint64_t)(uintptr_t)indirect); }
static void e_glFramebufferParameteri(GLenum target, GLenum pname, GLint param) { struct ts *T = TS(); begin(T, 250, 12, 12); put32(T, (uint32_t)target); put32(T, (uint32_t)pname); put32(T, (uint32_t)param); }
static void e_glGetFramebufferParameteriv(GLenum target, GLenum pname, GLint * params) { struct ts *T = TS(); begin(T, 251, 8, 8); put32(T, (uint32_t)target); put32(T, (uint32_t)pname); rsync(T); get(T, params, (uint32_t)((4) > 0 ? (4) : 0)); }
static void e_glGetProgramInterfaceiv(GLuint program, GLenum programInterface, GLenum pname, GLint * params) { struct ts *T = TS(); begin(T, 252, 12, 12); put32(T, (uint32_t)program); put32(T, (uint32_t)programInterface); put32(T, (uint32_t)pname); rsync(T); get(T, params, (uint32_t)((4) > 0 ? (4) : 0)); }
static GLuint e_glGetProgramResourceIndex(GLuint program, GLenum programInterface, const GLchar * name) { struct ts *T = TS(); uint32_t name_n = name ? (uint32_t)strlen(name) + 1 : NIL; begin(T, 253, 12, 12 + BLOB(name_n)); put32(T, (uint32_t)program); put32(T, (uint32_t)programInterface); putblob(T, name, name_n); rsync(T); return (GLuint)get32v(T); }
static void e_glGetProgramResourceName(GLuint program, GLenum programInterface, GLuint index, GLsizei bufSize, GLsizei * length, GLchar * name) { struct ts *T = TS(); begin(T, 254, 16, 16); put32(T, (uint32_t)program); put32(T, (uint32_t)programInterface); put32(T, (uint32_t)index); put32(T, (uint32_t)bufSize); rsync(T); get(T, length, (uint32_t)((4) > 0 ? (4) : 0)); get(T, name, (uint32_t)((bufSize) > 0 ? (bufSize) : 0)); }
static void e_glGetProgramResourceiv(GLuint program, GLenum programInterface, GLuint index, GLsizei propCount, const GLenum * props, GLsizei bufSize, GLsizei * length, GLint * params) { struct ts *T = TS(); uint32_t props_n = props ? (uint32_t)((propCount * 4) > 0 ? (propCount * 4) : 0) : NIL; begin(T, 255, 24, 24 + BLOB(props_n)); put32(T, (uint32_t)program); put32(T, (uint32_t)programInterface); put32(T, (uint32_t)index); put32(T, (uint32_t)propCount); putblob(T, props, props_n); put32(T, (uint32_t)bufSize); rsync(T); get(T, length, (uint32_t)((4) > 0 ? (4) : 0)); get(T, params, (uint32_t)((bufSize * 4) > 0 ? (bufSize * 4) : 0)); }
static GLint e_glGetProgramResourceLocation(GLuint program, GLenum programInterface, const GLchar * name) { struct ts *T = TS(); uint32_t name_n = name ? (uint32_t)strlen(name) + 1 : NIL; begin(T, 256, 12, 12 + BLOB(name_n)); put32(T, (uint32_t)program); put32(T, (uint32_t)programInterface); putblob(T, name, name_n); rsync(T); return (GLint)get32v(T); }
static void e_glUseProgramStages(GLuint pipeline, GLbitfield stages, GLuint program) { struct ts *T = TS(); begin(T, 257, 12, 12); put32(T, (uint32_t)pipeline); put32(T, (uint32_t)stages); put32(T, (uint32_t)program); }
static void e_glActiveShaderProgram(GLuint pipeline, GLuint program) { struct ts *T = TS(); begin(T, 258, 8, 8); put32(T, (uint32_t)pipeline); put32(T, (uint32_t)program); }
static GLuint e_glCreateShaderProgramv(GLenum type, GLsizei count, const GLchar *const* strings);
static void e_glBindProgramPipeline(GLuint pipeline) { struct ts *T = TS(); begin(T, 260, 4, 4); put32(T, (uint32_t)pipeline); }
static void e_glDeleteProgramPipelines(GLsizei n, const GLuint * pipelines) { struct ts *T = TS(); uint32_t pipelines_n = pipelines ? (uint32_t)((n * 4) > 0 ? (n * 4) : 0) : NIL; begin(T, 261, 8, 8 + BLOB(pipelines_n)); put32(T, (uint32_t)n); putblob(T, pipelines, pipelines_n); }
static void e_glGenProgramPipelines(GLsizei n, GLuint * pipelines) { struct ts *T = TS(); begin(T, 262, 4, 4); put32(T, (uint32_t)n); rsync(T); get(T, pipelines, (uint32_t)((n * 4) > 0 ? (n * 4) : 0)); }
static GLboolean e_glIsProgramPipeline(GLuint pipeline) { struct ts *T = TS(); begin(T, 263, 4, 4); put32(T, (uint32_t)pipeline); rsync(T); return (GLboolean)get32v(T); }
static void e_glGetProgramPipelineiv(GLuint pipeline, GLenum pname, GLint * params) { struct ts *T = TS(); begin(T, 264, 8, 8); put32(T, (uint32_t)pipeline); put32(T, (uint32_t)pname); rsync(T); get(T, params, (uint32_t)((4) > 0 ? (4) : 0)); }
static void e_glProgramUniform1i(GLuint program, GLint location, GLint v0) { struct ts *T = TS(); begin(T, 265, 12, 12); put32(T, (uint32_t)program); put32(T, (uint32_t)location); put32(T, (uint32_t)v0); }
static void e_glProgramUniform2i(GLuint program, GLint location, GLint v0, GLint v1) { struct ts *T = TS(); begin(T, 266, 16, 16); put32(T, (uint32_t)program); put32(T, (uint32_t)location); put32(T, (uint32_t)v0); put32(T, (uint32_t)v1); }
static void e_glProgramUniform3i(GLuint program, GLint location, GLint v0, GLint v1, GLint v2) { struct ts *T = TS(); begin(T, 267, 20, 20); put32(T, (uint32_t)program); put32(T, (uint32_t)location); put32(T, (uint32_t)v0); put32(T, (uint32_t)v1); put32(T, (uint32_t)v2); }
static void e_glProgramUniform4i(GLuint program, GLint location, GLint v0, GLint v1, GLint v2, GLint v3) { struct ts *T = TS(); begin(T, 268, 24, 24); put32(T, (uint32_t)program); put32(T, (uint32_t)location); put32(T, (uint32_t)v0); put32(T, (uint32_t)v1); put32(T, (uint32_t)v2); put32(T, (uint32_t)v3); }
static void e_glProgramUniform1ui(GLuint program, GLint location, GLuint v0) { struct ts *T = TS(); begin(T, 269, 12, 12); put32(T, (uint32_t)program); put32(T, (uint32_t)location); put32(T, (uint32_t)v0); }
static void e_glProgramUniform2ui(GLuint program, GLint location, GLuint v0, GLuint v1) { struct ts *T = TS(); begin(T, 270, 16, 16); put32(T, (uint32_t)program); put32(T, (uint32_t)location); put32(T, (uint32_t)v0); put32(T, (uint32_t)v1); }
static void e_glProgramUniform3ui(GLuint program, GLint location, GLuint v0, GLuint v1, GLuint v2) { struct ts *T = TS(); begin(T, 271, 20, 20); put32(T, (uint32_t)program); put32(T, (uint32_t)location); put32(T, (uint32_t)v0); put32(T, (uint32_t)v1); put32(T, (uint32_t)v2); }
static void e_glProgramUniform4ui(GLuint program, GLint location, GLuint v0, GLuint v1, GLuint v2, GLuint v3) { struct ts *T = TS(); begin(T, 272, 24, 24); put32(T, (uint32_t)program); put32(T, (uint32_t)location); put32(T, (uint32_t)v0); put32(T, (uint32_t)v1); put32(T, (uint32_t)v2); put32(T, (uint32_t)v3); }
static void e_glProgramUniform1f(GLuint program, GLint location, GLfloat v0) { struct ts *T = TS(); begin(T, 273, 12, 12); put32(T, (uint32_t)program); put32(T, (uint32_t)location); putf(T, v0); }
static void e_glProgramUniform2f(GLuint program, GLint location, GLfloat v0, GLfloat v1) { struct ts *T = TS(); begin(T, 274, 16, 16); put32(T, (uint32_t)program); put32(T, (uint32_t)location); putf(T, v0); putf(T, v1); }
static void e_glProgramUniform3f(GLuint program, GLint location, GLfloat v0, GLfloat v1, GLfloat v2) { struct ts *T = TS(); begin(T, 275, 20, 20); put32(T, (uint32_t)program); put32(T, (uint32_t)location); putf(T, v0); putf(T, v1); putf(T, v2); }
static void e_glProgramUniform4f(GLuint program, GLint location, GLfloat v0, GLfloat v1, GLfloat v2, GLfloat v3) { struct ts *T = TS(); begin(T, 276, 24, 24); put32(T, (uint32_t)program); put32(T, (uint32_t)location); putf(T, v0); putf(T, v1); putf(T, v2); putf(T, v3); }
static void e_glProgramUniform1iv(GLuint program, GLint location, GLsizei count, const GLint * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 4) > 0 ? (count * 4) : 0) : NIL; begin(T, 277, 16, 16 + BLOB(value_n)); put32(T, (uint32_t)program); put32(T, (uint32_t)location); put32(T, (uint32_t)count); putblob(T, value, value_n); }
static void e_glProgramUniform2iv(GLuint program, GLint location, GLsizei count, const GLint * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 8) > 0 ? (count * 8) : 0) : NIL; begin(T, 278, 16, 16 + BLOB(value_n)); put32(T, (uint32_t)program); put32(T, (uint32_t)location); put32(T, (uint32_t)count); putblob(T, value, value_n); }
static void e_glProgramUniform3iv(GLuint program, GLint location, GLsizei count, const GLint * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 12) > 0 ? (count * 12) : 0) : NIL; begin(T, 279, 16, 16 + BLOB(value_n)); put32(T, (uint32_t)program); put32(T, (uint32_t)location); put32(T, (uint32_t)count); putblob(T, value, value_n); }
static void e_glProgramUniform4iv(GLuint program, GLint location, GLsizei count, const GLint * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 16) > 0 ? (count * 16) : 0) : NIL; begin(T, 280, 16, 16 + BLOB(value_n)); put32(T, (uint32_t)program); put32(T, (uint32_t)location); put32(T, (uint32_t)count); putblob(T, value, value_n); }
static void e_glProgramUniform1uiv(GLuint program, GLint location, GLsizei count, const GLuint * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 4) > 0 ? (count * 4) : 0) : NIL; begin(T, 281, 16, 16 + BLOB(value_n)); put32(T, (uint32_t)program); put32(T, (uint32_t)location); put32(T, (uint32_t)count); putblob(T, value, value_n); }
static void e_glProgramUniform2uiv(GLuint program, GLint location, GLsizei count, const GLuint * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 8) > 0 ? (count * 8) : 0) : NIL; begin(T, 282, 16, 16 + BLOB(value_n)); put32(T, (uint32_t)program); put32(T, (uint32_t)location); put32(T, (uint32_t)count); putblob(T, value, value_n); }
static void e_glProgramUniform3uiv(GLuint program, GLint location, GLsizei count, const GLuint * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 12) > 0 ? (count * 12) : 0) : NIL; begin(T, 283, 16, 16 + BLOB(value_n)); put32(T, (uint32_t)program); put32(T, (uint32_t)location); put32(T, (uint32_t)count); putblob(T, value, value_n); }
static void e_glProgramUniform4uiv(GLuint program, GLint location, GLsizei count, const GLuint * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 16) > 0 ? (count * 16) : 0) : NIL; begin(T, 284, 16, 16 + BLOB(value_n)); put32(T, (uint32_t)program); put32(T, (uint32_t)location); put32(T, (uint32_t)count); putblob(T, value, value_n); }
static void e_glProgramUniform1fv(GLuint program, GLint location, GLsizei count, const GLfloat * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 4) > 0 ? (count * 4) : 0) : NIL; begin(T, 285, 16, 16 + BLOB(value_n)); put32(T, (uint32_t)program); put32(T, (uint32_t)location); put32(T, (uint32_t)count); putblob(T, value, value_n); }
static void e_glProgramUniform2fv(GLuint program, GLint location, GLsizei count, const GLfloat * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 8) > 0 ? (count * 8) : 0) : NIL; begin(T, 286, 16, 16 + BLOB(value_n)); put32(T, (uint32_t)program); put32(T, (uint32_t)location); put32(T, (uint32_t)count); putblob(T, value, value_n); }
static void e_glProgramUniform3fv(GLuint program, GLint location, GLsizei count, const GLfloat * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 12) > 0 ? (count * 12) : 0) : NIL; begin(T, 287, 16, 16 + BLOB(value_n)); put32(T, (uint32_t)program); put32(T, (uint32_t)location); put32(T, (uint32_t)count); putblob(T, value, value_n); }
static void e_glProgramUniform4fv(GLuint program, GLint location, GLsizei count, const GLfloat * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 16) > 0 ? (count * 16) : 0) : NIL; begin(T, 288, 16, 16 + BLOB(value_n)); put32(T, (uint32_t)program); put32(T, (uint32_t)location); put32(T, (uint32_t)count); putblob(T, value, value_n); }
static void e_glProgramUniformMatrix2fv(GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 16) > 0 ? (count * 16) : 0) : NIL; begin(T, 289, 20, 20 + BLOB(value_n)); put32(T, (uint32_t)program); put32(T, (uint32_t)location); put32(T, (uint32_t)count); put32(T, (uint32_t)transpose); putblob(T, value, value_n); }
static void e_glProgramUniformMatrix3fv(GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 36) > 0 ? (count * 36) : 0) : NIL; begin(T, 290, 20, 20 + BLOB(value_n)); put32(T, (uint32_t)program); put32(T, (uint32_t)location); put32(T, (uint32_t)count); put32(T, (uint32_t)transpose); putblob(T, value, value_n); }
static void e_glProgramUniformMatrix4fv(GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 64) > 0 ? (count * 64) : 0) : NIL; begin(T, 291, 20, 20 + BLOB(value_n)); put32(T, (uint32_t)program); put32(T, (uint32_t)location); put32(T, (uint32_t)count); put32(T, (uint32_t)transpose); putblob(T, value, value_n); }
static void e_glProgramUniformMatrix2x3fv(GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 24) > 0 ? (count * 24) : 0) : NIL; begin(T, 292, 20, 20 + BLOB(value_n)); put32(T, (uint32_t)program); put32(T, (uint32_t)location); put32(T, (uint32_t)count); put32(T, (uint32_t)transpose); putblob(T, value, value_n); }
static void e_glProgramUniformMatrix3x2fv(GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 24) > 0 ? (count * 24) : 0) : NIL; begin(T, 293, 20, 20 + BLOB(value_n)); put32(T, (uint32_t)program); put32(T, (uint32_t)location); put32(T, (uint32_t)count); put32(T, (uint32_t)transpose); putblob(T, value, value_n); }
static void e_glProgramUniformMatrix2x4fv(GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 32) > 0 ? (count * 32) : 0) : NIL; begin(T, 294, 20, 20 + BLOB(value_n)); put32(T, (uint32_t)program); put32(T, (uint32_t)location); put32(T, (uint32_t)count); put32(T, (uint32_t)transpose); putblob(T, value, value_n); }
static void e_glProgramUniformMatrix4x2fv(GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 32) > 0 ? (count * 32) : 0) : NIL; begin(T, 295, 20, 20 + BLOB(value_n)); put32(T, (uint32_t)program); put32(T, (uint32_t)location); put32(T, (uint32_t)count); put32(T, (uint32_t)transpose); putblob(T, value, value_n); }
static void e_glProgramUniformMatrix3x4fv(GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 48) > 0 ? (count * 48) : 0) : NIL; begin(T, 296, 20, 20 + BLOB(value_n)); put32(T, (uint32_t)program); put32(T, (uint32_t)location); put32(T, (uint32_t)count); put32(T, (uint32_t)transpose); putblob(T, value, value_n); }
static void e_glProgramUniformMatrix4x3fv(GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat * value) { struct ts *T = TS(); uint32_t value_n = value ? (uint32_t)((count * 48) > 0 ? (count * 48) : 0) : NIL; begin(T, 297, 20, 20 + BLOB(value_n)); put32(T, (uint32_t)program); put32(T, (uint32_t)location); put32(T, (uint32_t)count); put32(T, (uint32_t)transpose); putblob(T, value, value_n); }
static void e_glValidateProgramPipeline(GLuint pipeline) { struct ts *T = TS(); begin(T, 298, 4, 4); put32(T, (uint32_t)pipeline); }
static void e_glGetProgramPipelineInfoLog(GLuint pipeline, GLsizei bufSize, GLsizei * length, GLchar * infoLog) { struct ts *T = TS(); begin(T, 299, 8, 8); put32(T, (uint32_t)pipeline); put32(T, (uint32_t)bufSize); rsync(T); get(T, length, (uint32_t)((4) > 0 ? (4) : 0)); get(T, infoLog, (uint32_t)((bufSize) > 0 ? (bufSize) : 0)); }
static void e_glBindImageTexture(GLuint unit, GLuint texture, GLint level, GLboolean layered, GLint layer, GLenum access, GLenum format) { struct ts *T = TS(); begin(T, 300, 28, 28); put32(T, (uint32_t)unit); put32(T, (uint32_t)texture); put32(T, (uint32_t)level); put32(T, (uint32_t)layered); put32(T, (uint32_t)layer); put32(T, (uint32_t)access); put32(T, (uint32_t)format); }
static void e_glGetBooleani_v(GLenum target, GLuint index, GLboolean * data) { struct ts *T = TS(); begin(T, 301, 8, 8); put32(T, (uint32_t)target); put32(T, (uint32_t)index); rsync(T); get(T, data, (uint32_t)((target == 0x0C23 ? 4 : 1) > 0 ? (target == 0x0C23 ? 4 : 1) : 0)); }
static void e_glMemoryBarrier(GLbitfield barriers) { struct ts *T = TS(); k_glMemoryBarrier(barriers); begin(T, 302, 4, 4); put32(T, (uint32_t)barriers); }
static void e_glMemoryBarrierByRegion(GLbitfield barriers) { struct ts *T = TS(); k_glMemoryBarrierByRegion(barriers); begin(T, 303, 4, 4); put32(T, (uint32_t)barriers); }
static void e_glTexStorage2DMultisample(GLenum target, GLsizei samples, GLenum internalformat, GLsizei width, GLsizei height, GLboolean fixedsamplelocations) { struct ts *T = TS(); begin(T, 304, 24, 24); put32(T, (uint32_t)target); put32(T, (uint32_t)samples); put32(T, (uint32_t)internalformat); put32(T, (uint32_t)width); put32(T, (uint32_t)height); put32(T, (uint32_t)fixedsamplelocations); }
static void e_glGetMultisamplefv(GLenum pname, GLuint index, GLfloat * val) { struct ts *T = TS(); begin(T, 305, 8, 8); put32(T, (uint32_t)pname); put32(T, (uint32_t)index); rsync(T); get(T, val, (uint32_t)((8) > 0 ? (8) : 0)); }
static void e_glSampleMaski(GLuint maskNumber, GLbitfield mask) { struct ts *T = TS(); begin(T, 306, 8, 8); put32(T, (uint32_t)maskNumber); put32(T, (uint32_t)mask); }
static void e_glGetTexLevelParameteriv(GLenum target, GLint level, GLenum pname, GLint * params) { struct ts *T = TS(); begin(T, 307, 12, 12); put32(T, (uint32_t)target); put32(T, (uint32_t)level); put32(T, (uint32_t)pname); rsync(T); get(T, params, (uint32_t)((4) > 0 ? (4) : 0)); }
static void e_glGetTexLevelParameterfv(GLenum target, GLint level, GLenum pname, GLfloat * params) { struct ts *T = TS(); begin(T, 308, 12, 12); put32(T, (uint32_t)target); put32(T, (uint32_t)level); put32(T, (uint32_t)pname); rsync(T); get(T, params, (uint32_t)((4) > 0 ? (4) : 0)); }
static void e_glBindVertexBuffer(GLuint bindingindex, GLuint buffer, GLintptr offset, GLsizei stride) { struct ts *T = TS(); begin(T, 309, 20, 20); put32(T, (uint32_t)bindingindex); put32(T, (uint32_t)buffer); put64(T, (uint64_t)offset); put32(T, (uint32_t)stride); }
static void e_glVertexAttribFormat(GLuint attribindex, GLint size, GLenum type, GLboolean normalized, GLuint relativeoffset) { struct ts *T = TS(); begin(T, 310, 20, 20); put32(T, (uint32_t)attribindex); put32(T, (uint32_t)size); put32(T, (uint32_t)type); put32(T, (uint32_t)normalized); put32(T, (uint32_t)relativeoffset); }
static void e_glVertexAttribIFormat(GLuint attribindex, GLint size, GLenum type, GLuint relativeoffset) { struct ts *T = TS(); begin(T, 311, 16, 16); put32(T, (uint32_t)attribindex); put32(T, (uint32_t)size); put32(T, (uint32_t)type); put32(T, (uint32_t)relativeoffset); }
static void e_glVertexAttribBinding(GLuint attribindex, GLuint bindingindex) { struct ts *T = TS(); begin(T, 312, 8, 8); put32(T, (uint32_t)attribindex); put32(T, (uint32_t)bindingindex); }
static void e_glVertexBindingDivisor(GLuint bindingindex, GLuint divisor) { struct ts *T = TS(); begin(T, 313, 8, 8); put32(T, (uint32_t)bindingindex); put32(T, (uint32_t)divisor); }
static void e_glEGLImageTargetTexture2DOES(GLenum target, GLeglImageOES image);
static void * e_glMapBufferOES(GLenum target, GLenum access);
static GLenum e_glGetGraphicsResetStatusEXT(void);
static GLenum e_glGetGraphicsResetStatusKHR(void);
static void e_glReadnPixelsEXT(GLint x, GLint y, GLsizei width, GLsizei height, GLenum format, GLenum type, GLsizei bufSize, void * data);
static void e_glReadnPixelsKHR(GLint x, GLint y, GLsizei width, GLsizei height, GLenum format, GLenum type, GLsizei bufSize, void * data);
static void e_glGetnUniformfvEXT(GLuint program, GLint location, GLsizei bufSize, GLfloat * params);
static void e_glGetnUniformivEXT(GLuint program, GLint location, GLsizei bufSize, GLint * params);
static void e_glGetnUniformfvKHR(GLuint program, GLint location, GLsizei bufSize, GLfloat * params);
static void e_glGetnUniformivKHR(GLuint program, GLint location, GLsizei bufSize, GLint * params);

static void *const kEnc[N_FUNCS] = {
    [IDX_glActiveTexture] = (void *)e_glActiveTexture,
    [IDX_glAttachShader] = (void *)e_glAttachShader,
    [IDX_glBindAttribLocation] = (void *)e_glBindAttribLocation,
    [IDX_glBindBuffer] = (void *)e_glBindBuffer,
    [IDX_glBindFramebuffer] = (void *)e_glBindFramebuffer,
    [IDX_glBindRenderbuffer] = (void *)e_glBindRenderbuffer,
    [IDX_glBindTexture] = (void *)e_glBindTexture,
    [IDX_glBlendColor] = (void *)e_glBlendColor,
    [IDX_glBlendEquation] = (void *)e_glBlendEquation,
    [IDX_glBlendEquationSeparate] = (void *)e_glBlendEquationSeparate,
    [IDX_glBlendFunc] = (void *)e_glBlendFunc,
    [IDX_glBlendFuncSeparate] = (void *)e_glBlendFuncSeparate,
    [IDX_glBufferData] = (void *)e_glBufferData,
    [IDX_glBufferSubData] = (void *)e_glBufferSubData,
    [IDX_glCheckFramebufferStatus] = (void *)e_glCheckFramebufferStatus,
    [IDX_glClear] = (void *)e_glClear,
    [IDX_glClearColor] = (void *)e_glClearColor,
    [IDX_glClearDepthf] = (void *)e_glClearDepthf,
    [IDX_glClearStencil] = (void *)e_glClearStencil,
    [IDX_glColorMask] = (void *)e_glColorMask,
    [IDX_glCompileShader] = (void *)e_glCompileShader,
    [IDX_glCompressedTexImage2D] = (void *)e_glCompressedTexImage2D,
    [IDX_glCompressedTexSubImage2D] = (void *)e_glCompressedTexSubImage2D,
    [IDX_glCopyTexImage2D] = (void *)e_glCopyTexImage2D,
    [IDX_glCopyTexSubImage2D] = (void *)e_glCopyTexSubImage2D,
    [IDX_glCreateProgram] = (void *)e_glCreateProgram,
    [IDX_glCreateShader] = (void *)e_glCreateShader,
    [IDX_glCullFace] = (void *)e_glCullFace,
    [IDX_glDeleteBuffers] = (void *)e_glDeleteBuffers,
    [IDX_glDeleteFramebuffers] = (void *)e_glDeleteFramebuffers,
    [IDX_glDeleteProgram] = (void *)e_glDeleteProgram,
    [IDX_glDeleteRenderbuffers] = (void *)e_glDeleteRenderbuffers,
    [IDX_glDeleteShader] = (void *)e_glDeleteShader,
    [IDX_glDeleteTextures] = (void *)e_glDeleteTextures,
    [IDX_glDepthFunc] = (void *)e_glDepthFunc,
    [IDX_glDepthMask] = (void *)e_glDepthMask,
    [IDX_glDepthRangef] = (void *)e_glDepthRangef,
    [IDX_glDetachShader] = (void *)e_glDetachShader,
    [IDX_glDisable] = (void *)e_glDisable,
    [IDX_glDisableVertexAttribArray] = (void *)e_glDisableVertexAttribArray,
    [IDX_glDrawArrays] = (void *)e_glDrawArrays,
    [IDX_glDrawElements] = (void *)e_glDrawElements,
    [IDX_glEnable] = (void *)e_glEnable,
    [IDX_glEnableVertexAttribArray] = (void *)e_glEnableVertexAttribArray,
    [IDX_glFinish] = (void *)e_glFinish,
    [IDX_glFlush] = (void *)e_glFlush,
    [IDX_glFramebufferRenderbuffer] = (void *)e_glFramebufferRenderbuffer,
    [IDX_glFramebufferTexture2D] = (void *)e_glFramebufferTexture2D,
    [IDX_glFrontFace] = (void *)e_glFrontFace,
    [IDX_glGenBuffers] = (void *)e_glGenBuffers,
    [IDX_glGenerateMipmap] = (void *)e_glGenerateMipmap,
    [IDX_glGenFramebuffers] = (void *)e_glGenFramebuffers,
    [IDX_glGenRenderbuffers] = (void *)e_glGenRenderbuffers,
    [IDX_glGenTextures] = (void *)e_glGenTextures,
    [IDX_glGetActiveAttrib] = (void *)e_glGetActiveAttrib,
    [IDX_glGetActiveUniform] = (void *)e_glGetActiveUniform,
    [IDX_glGetAttachedShaders] = (void *)e_glGetAttachedShaders,
    [IDX_glGetAttribLocation] = (void *)e_glGetAttribLocation,
    [IDX_glGetBooleanv] = (void *)e_glGetBooleanv,
    [IDX_glGetBufferParameteriv] = (void *)e_glGetBufferParameteriv,
    [IDX_glGetError] = (void *)e_glGetError,
    [IDX_glGetFloatv] = (void *)e_glGetFloatv,
    [IDX_glGetFramebufferAttachmentParameteriv] = (void *)e_glGetFramebufferAttachmentParameteriv,
    [IDX_glGetIntegerv] = (void *)e_glGetIntegerv,
    [IDX_glGetProgramiv] = (void *)e_glGetProgramiv,
    [IDX_glGetProgramInfoLog] = (void *)e_glGetProgramInfoLog,
    [IDX_glGetRenderbufferParameteriv] = (void *)e_glGetRenderbufferParameteriv,
    [IDX_glGetShaderiv] = (void *)e_glGetShaderiv,
    [IDX_glGetShaderInfoLog] = (void *)e_glGetShaderInfoLog,
    [IDX_glGetShaderPrecisionFormat] = (void *)e_glGetShaderPrecisionFormat,
    [IDX_glGetShaderSource] = (void *)e_glGetShaderSource,
    [IDX_glGetString] = (void *)e_glGetString,
    [IDX_glGetTexParameterfv] = (void *)e_glGetTexParameterfv,
    [IDX_glGetTexParameteriv] = (void *)e_glGetTexParameteriv,
    [IDX_glGetUniformfv] = (void *)e_glGetUniformfv,
    [IDX_glGetUniformiv] = (void *)e_glGetUniformiv,
    [IDX_glGetUniformLocation] = (void *)e_glGetUniformLocation,
    [IDX_glGetVertexAttribfv] = (void *)e_glGetVertexAttribfv,
    [IDX_glGetVertexAttribiv] = (void *)e_glGetVertexAttribiv,
    [IDX_glGetVertexAttribPointerv] = (void *)e_glGetVertexAttribPointerv,
    [IDX_glHint] = (void *)e_glHint,
    [IDX_glIsBuffer] = (void *)e_glIsBuffer,
    [IDX_glIsEnabled] = (void *)e_glIsEnabled,
    [IDX_glIsFramebuffer] = (void *)e_glIsFramebuffer,
    [IDX_glIsProgram] = (void *)e_glIsProgram,
    [IDX_glIsRenderbuffer] = (void *)e_glIsRenderbuffer,
    [IDX_glIsShader] = (void *)e_glIsShader,
    [IDX_glIsTexture] = (void *)e_glIsTexture,
    [IDX_glLineWidth] = (void *)e_glLineWidth,
    [IDX_glLinkProgram] = (void *)e_glLinkProgram,
    [IDX_glPixelStorei] = (void *)e_glPixelStorei,
    [IDX_glPolygonOffset] = (void *)e_glPolygonOffset,
    [IDX_glReadPixels] = (void *)e_glReadPixels,
    [IDX_glReleaseShaderCompiler] = (void *)e_glReleaseShaderCompiler,
    [IDX_glRenderbufferStorage] = (void *)e_glRenderbufferStorage,
    [IDX_glSampleCoverage] = (void *)e_glSampleCoverage,
    [IDX_glScissor] = (void *)e_glScissor,
    [IDX_glShaderBinary] = (void *)e_glShaderBinary,
    [IDX_glShaderSource] = (void *)e_glShaderSource,
    [IDX_glStencilFunc] = (void *)e_glStencilFunc,
    [IDX_glStencilFuncSeparate] = (void *)e_glStencilFuncSeparate,
    [IDX_glStencilMask] = (void *)e_glStencilMask,
    [IDX_glStencilMaskSeparate] = (void *)e_glStencilMaskSeparate,
    [IDX_glStencilOp] = (void *)e_glStencilOp,
    [IDX_glStencilOpSeparate] = (void *)e_glStencilOpSeparate,
    [IDX_glTexImage2D] = (void *)e_glTexImage2D,
    [IDX_glTexParameterf] = (void *)e_glTexParameterf,
    [IDX_glTexParameterfv] = (void *)e_glTexParameterfv,
    [IDX_glTexParameteri] = (void *)e_glTexParameteri,
    [IDX_glTexParameteriv] = (void *)e_glTexParameteriv,
    [IDX_glTexSubImage2D] = (void *)e_glTexSubImage2D,
    [IDX_glUniform1f] = (void *)e_glUniform1f,
    [IDX_glUniform1fv] = (void *)e_glUniform1fv,
    [IDX_glUniform1i] = (void *)e_glUniform1i,
    [IDX_glUniform1iv] = (void *)e_glUniform1iv,
    [IDX_glUniform2f] = (void *)e_glUniform2f,
    [IDX_glUniform2fv] = (void *)e_glUniform2fv,
    [IDX_glUniform2i] = (void *)e_glUniform2i,
    [IDX_glUniform2iv] = (void *)e_glUniform2iv,
    [IDX_glUniform3f] = (void *)e_glUniform3f,
    [IDX_glUniform3fv] = (void *)e_glUniform3fv,
    [IDX_glUniform3i] = (void *)e_glUniform3i,
    [IDX_glUniform3iv] = (void *)e_glUniform3iv,
    [IDX_glUniform4f] = (void *)e_glUniform4f,
    [IDX_glUniform4fv] = (void *)e_glUniform4fv,
    [IDX_glUniform4i] = (void *)e_glUniform4i,
    [IDX_glUniform4iv] = (void *)e_glUniform4iv,
    [IDX_glUniformMatrix2fv] = (void *)e_glUniformMatrix2fv,
    [IDX_glUniformMatrix3fv] = (void *)e_glUniformMatrix3fv,
    [IDX_glUniformMatrix4fv] = (void *)e_glUniformMatrix4fv,
    [IDX_glUseProgram] = (void *)e_glUseProgram,
    [IDX_glValidateProgram] = (void *)e_glValidateProgram,
    [IDX_glVertexAttrib1f] = (void *)e_glVertexAttrib1f,
    [IDX_glVertexAttrib1fv] = (void *)e_glVertexAttrib1fv,
    [IDX_glVertexAttrib2f] = (void *)e_glVertexAttrib2f,
    [IDX_glVertexAttrib2fv] = (void *)e_glVertexAttrib2fv,
    [IDX_glVertexAttrib3f] = (void *)e_glVertexAttrib3f,
    [IDX_glVertexAttrib3fv] = (void *)e_glVertexAttrib3fv,
    [IDX_glVertexAttrib4f] = (void *)e_glVertexAttrib4f,
    [IDX_glVertexAttrib4fv] = (void *)e_glVertexAttrib4fv,
    [IDX_glVertexAttribPointer] = (void *)e_glVertexAttribPointer,
    [IDX_glViewport] = (void *)e_glViewport,
    [IDX_glReadBuffer] = (void *)e_glReadBuffer,
    [IDX_glDrawRangeElements] = (void *)e_glDrawRangeElements,
    [IDX_glTexImage3D] = (void *)e_glTexImage3D,
    [IDX_glTexSubImage3D] = (void *)e_glTexSubImage3D,
    [IDX_glCopyTexSubImage3D] = (void *)e_glCopyTexSubImage3D,
    [IDX_glCompressedTexImage3D] = (void *)e_glCompressedTexImage3D,
    [IDX_glCompressedTexSubImage3D] = (void *)e_glCompressedTexSubImage3D,
    [IDX_glGenQueries] = (void *)e_glGenQueries,
    [IDX_glDeleteQueries] = (void *)e_glDeleteQueries,
    [IDX_glIsQuery] = (void *)e_glIsQuery,
    [IDX_glBeginQuery] = (void *)e_glBeginQuery,
    [IDX_glEndQuery] = (void *)e_glEndQuery,
    [IDX_glGetQueryiv] = (void *)e_glGetQueryiv,
    [IDX_glGetQueryObjectuiv] = (void *)e_glGetQueryObjectuiv,
    [IDX_glUnmapBuffer] = (void *)e_glUnmapBuffer,
    [IDX_glGetBufferPointerv] = (void *)e_glGetBufferPointerv,
    [IDX_glDrawBuffers] = (void *)e_glDrawBuffers,
    [IDX_glUniformMatrix2x3fv] = (void *)e_glUniformMatrix2x3fv,
    [IDX_glUniformMatrix3x2fv] = (void *)e_glUniformMatrix3x2fv,
    [IDX_glUniformMatrix2x4fv] = (void *)e_glUniformMatrix2x4fv,
    [IDX_glUniformMatrix4x2fv] = (void *)e_glUniformMatrix4x2fv,
    [IDX_glUniformMatrix3x4fv] = (void *)e_glUniformMatrix3x4fv,
    [IDX_glUniformMatrix4x3fv] = (void *)e_glUniformMatrix4x3fv,
    [IDX_glBlitFramebuffer] = (void *)e_glBlitFramebuffer,
    [IDX_glRenderbufferStorageMultisample] = (void *)e_glRenderbufferStorageMultisample,
    [IDX_glFramebufferTextureLayer] = (void *)e_glFramebufferTextureLayer,
    [IDX_glMapBufferRange] = (void *)e_glMapBufferRange,
    [IDX_glFlushMappedBufferRange] = (void *)e_glFlushMappedBufferRange,
    [IDX_glBindVertexArray] = (void *)e_glBindVertexArray,
    [IDX_glDeleteVertexArrays] = (void *)e_glDeleteVertexArrays,
    [IDX_glGenVertexArrays] = (void *)e_glGenVertexArrays,
    [IDX_glIsVertexArray] = (void *)e_glIsVertexArray,
    [IDX_glGetIntegeri_v] = (void *)e_glGetIntegeri_v,
    [IDX_glBeginTransformFeedback] = (void *)e_glBeginTransformFeedback,
    [IDX_glEndTransformFeedback] = (void *)e_glEndTransformFeedback,
    [IDX_glBindBufferRange] = (void *)e_glBindBufferRange,
    [IDX_glBindBufferBase] = (void *)e_glBindBufferBase,
    [IDX_glTransformFeedbackVaryings] = (void *)e_glTransformFeedbackVaryings,
    [IDX_glGetTransformFeedbackVarying] = (void *)e_glGetTransformFeedbackVarying,
    [IDX_glVertexAttribIPointer] = (void *)e_glVertexAttribIPointer,
    [IDX_glGetVertexAttribIiv] = (void *)e_glGetVertexAttribIiv,
    [IDX_glGetVertexAttribIuiv] = (void *)e_glGetVertexAttribIuiv,
    [IDX_glVertexAttribI4i] = (void *)e_glVertexAttribI4i,
    [IDX_glVertexAttribI4ui] = (void *)e_glVertexAttribI4ui,
    [IDX_glVertexAttribI4iv] = (void *)e_glVertexAttribI4iv,
    [IDX_glVertexAttribI4uiv] = (void *)e_glVertexAttribI4uiv,
    [IDX_glGetUniformuiv] = (void *)e_glGetUniformuiv,
    [IDX_glGetFragDataLocation] = (void *)e_glGetFragDataLocation,
    [IDX_glUniform1ui] = (void *)e_glUniform1ui,
    [IDX_glUniform2ui] = (void *)e_glUniform2ui,
    [IDX_glUniform3ui] = (void *)e_glUniform3ui,
    [IDX_glUniform4ui] = (void *)e_glUniform4ui,
    [IDX_glUniform1uiv] = (void *)e_glUniform1uiv,
    [IDX_glUniform2uiv] = (void *)e_glUniform2uiv,
    [IDX_glUniform3uiv] = (void *)e_glUniform3uiv,
    [IDX_glUniform4uiv] = (void *)e_glUniform4uiv,
    [IDX_glClearBufferiv] = (void *)e_glClearBufferiv,
    [IDX_glClearBufferuiv] = (void *)e_glClearBufferuiv,
    [IDX_glClearBufferfv] = (void *)e_glClearBufferfv,
    [IDX_glClearBufferfi] = (void *)e_glClearBufferfi,
    [IDX_glGetStringi] = (void *)e_glGetStringi,
    [IDX_glCopyBufferSubData] = (void *)e_glCopyBufferSubData,
    [IDX_glGetUniformIndices] = (void *)e_glGetUniformIndices,
    [IDX_glGetActiveUniformsiv] = (void *)e_glGetActiveUniformsiv,
    [IDX_glGetUniformBlockIndex] = (void *)e_glGetUniformBlockIndex,
    [IDX_glGetActiveUniformBlockiv] = (void *)e_glGetActiveUniformBlockiv,
    [IDX_glGetActiveUniformBlockName] = (void *)e_glGetActiveUniformBlockName,
    [IDX_glUniformBlockBinding] = (void *)e_glUniformBlockBinding,
    [IDX_glDrawArraysInstanced] = (void *)e_glDrawArraysInstanced,
    [IDX_glDrawElementsInstanced] = (void *)e_glDrawElementsInstanced,
    [IDX_glFenceSync] = (void *)e_glFenceSync,
    [IDX_glIsSync] = (void *)e_glIsSync,
    [IDX_glDeleteSync] = (void *)e_glDeleteSync,
    [IDX_glClientWaitSync] = (void *)e_glClientWaitSync,
    [IDX_glWaitSync] = (void *)e_glWaitSync,
    [IDX_glGetInteger64v] = (void *)e_glGetInteger64v,
    [IDX_glGetSynciv] = (void *)e_glGetSynciv,
    [IDX_glGetInteger64i_v] = (void *)e_glGetInteger64i_v,
    [IDX_glGetBufferParameteri64v] = (void *)e_glGetBufferParameteri64v,
    [IDX_glGenSamplers] = (void *)e_glGenSamplers,
    [IDX_glDeleteSamplers] = (void *)e_glDeleteSamplers,
    [IDX_glIsSampler] = (void *)e_glIsSampler,
    [IDX_glBindSampler] = (void *)e_glBindSampler,
    [IDX_glSamplerParameteri] = (void *)e_glSamplerParameteri,
    [IDX_glSamplerParameteriv] = (void *)e_glSamplerParameteriv,
    [IDX_glSamplerParameterf] = (void *)e_glSamplerParameterf,
    [IDX_glSamplerParameterfv] = (void *)e_glSamplerParameterfv,
    [IDX_glGetSamplerParameteriv] = (void *)e_glGetSamplerParameteriv,
    [IDX_glGetSamplerParameterfv] = (void *)e_glGetSamplerParameterfv,
    [IDX_glVertexAttribDivisor] = (void *)e_glVertexAttribDivisor,
    [IDX_glBindTransformFeedback] = (void *)e_glBindTransformFeedback,
    [IDX_glDeleteTransformFeedbacks] = (void *)e_glDeleteTransformFeedbacks,
    [IDX_glGenTransformFeedbacks] = (void *)e_glGenTransformFeedbacks,
    [IDX_glIsTransformFeedback] = (void *)e_glIsTransformFeedback,
    [IDX_glPauseTransformFeedback] = (void *)e_glPauseTransformFeedback,
    [IDX_glResumeTransformFeedback] = (void *)e_glResumeTransformFeedback,
    [IDX_glGetProgramBinary] = (void *)e_glGetProgramBinary,
    [IDX_glProgramBinary] = (void *)e_glProgramBinary,
    [IDX_glProgramParameteri] = (void *)e_glProgramParameteri,
    [IDX_glInvalidateFramebuffer] = (void *)e_glInvalidateFramebuffer,
    [IDX_glInvalidateSubFramebuffer] = (void *)e_glInvalidateSubFramebuffer,
    [IDX_glTexStorage2D] = (void *)e_glTexStorage2D,
    [IDX_glTexStorage3D] = (void *)e_glTexStorage3D,
    [IDX_glGetInternalformativ] = (void *)e_glGetInternalformativ,
    [IDX_glDispatchCompute] = (void *)e_glDispatchCompute,
    [IDX_glDispatchComputeIndirect] = (void *)e_glDispatchComputeIndirect,
    [IDX_glDrawArraysIndirect] = (void *)e_glDrawArraysIndirect,
    [IDX_glDrawElementsIndirect] = (void *)e_glDrawElementsIndirect,
    [IDX_glFramebufferParameteri] = (void *)e_glFramebufferParameteri,
    [IDX_glGetFramebufferParameteriv] = (void *)e_glGetFramebufferParameteriv,
    [IDX_glGetProgramInterfaceiv] = (void *)e_glGetProgramInterfaceiv,
    [IDX_glGetProgramResourceIndex] = (void *)e_glGetProgramResourceIndex,
    [IDX_glGetProgramResourceName] = (void *)e_glGetProgramResourceName,
    [IDX_glGetProgramResourceiv] = (void *)e_glGetProgramResourceiv,
    [IDX_glGetProgramResourceLocation] = (void *)e_glGetProgramResourceLocation,
    [IDX_glUseProgramStages] = (void *)e_glUseProgramStages,
    [IDX_glActiveShaderProgram] = (void *)e_glActiveShaderProgram,
    [IDX_glCreateShaderProgramv] = (void *)e_glCreateShaderProgramv,
    [IDX_glBindProgramPipeline] = (void *)e_glBindProgramPipeline,
    [IDX_glDeleteProgramPipelines] = (void *)e_glDeleteProgramPipelines,
    [IDX_glGenProgramPipelines] = (void *)e_glGenProgramPipelines,
    [IDX_glIsProgramPipeline] = (void *)e_glIsProgramPipeline,
    [IDX_glGetProgramPipelineiv] = (void *)e_glGetProgramPipelineiv,
    [IDX_glProgramUniform1i] = (void *)e_glProgramUniform1i,
    [IDX_glProgramUniform2i] = (void *)e_glProgramUniform2i,
    [IDX_glProgramUniform3i] = (void *)e_glProgramUniform3i,
    [IDX_glProgramUniform4i] = (void *)e_glProgramUniform4i,
    [IDX_glProgramUniform1ui] = (void *)e_glProgramUniform1ui,
    [IDX_glProgramUniform2ui] = (void *)e_glProgramUniform2ui,
    [IDX_glProgramUniform3ui] = (void *)e_glProgramUniform3ui,
    [IDX_glProgramUniform4ui] = (void *)e_glProgramUniform4ui,
    [IDX_glProgramUniform1f] = (void *)e_glProgramUniform1f,
    [IDX_glProgramUniform2f] = (void *)e_glProgramUniform2f,
    [IDX_glProgramUniform3f] = (void *)e_glProgramUniform3f,
    [IDX_glProgramUniform4f] = (void *)e_glProgramUniform4f,
    [IDX_glProgramUniform1iv] = (void *)e_glProgramUniform1iv,
    [IDX_glProgramUniform2iv] = (void *)e_glProgramUniform2iv,
    [IDX_glProgramUniform3iv] = (void *)e_glProgramUniform3iv,
    [IDX_glProgramUniform4iv] = (void *)e_glProgramUniform4iv,
    [IDX_glProgramUniform1uiv] = (void *)e_glProgramUniform1uiv,
    [IDX_glProgramUniform2uiv] = (void *)e_glProgramUniform2uiv,
    [IDX_glProgramUniform3uiv] = (void *)e_glProgramUniform3uiv,
    [IDX_glProgramUniform4uiv] = (void *)e_glProgramUniform4uiv,
    [IDX_glProgramUniform1fv] = (void *)e_glProgramUniform1fv,
    [IDX_glProgramUniform2fv] = (void *)e_glProgramUniform2fv,
    [IDX_glProgramUniform3fv] = (void *)e_glProgramUniform3fv,
    [IDX_glProgramUniform4fv] = (void *)e_glProgramUniform4fv,
    [IDX_glProgramUniformMatrix2fv] = (void *)e_glProgramUniformMatrix2fv,
    [IDX_glProgramUniformMatrix3fv] = (void *)e_glProgramUniformMatrix3fv,
    [IDX_glProgramUniformMatrix4fv] = (void *)e_glProgramUniformMatrix4fv,
    [IDX_glProgramUniformMatrix2x3fv] = (void *)e_glProgramUniformMatrix2x3fv,
    [IDX_glProgramUniformMatrix3x2fv] = (void *)e_glProgramUniformMatrix3x2fv,
    [IDX_glProgramUniformMatrix2x4fv] = (void *)e_glProgramUniformMatrix2x4fv,
    [IDX_glProgramUniformMatrix4x2fv] = (void *)e_glProgramUniformMatrix4x2fv,
    [IDX_glProgramUniformMatrix3x4fv] = (void *)e_glProgramUniformMatrix3x4fv,
    [IDX_glProgramUniformMatrix4x3fv] = (void *)e_glProgramUniformMatrix4x3fv,
    [IDX_glValidateProgramPipeline] = (void *)e_glValidateProgramPipeline,
    [IDX_glGetProgramPipelineInfoLog] = (void *)e_glGetProgramPipelineInfoLog,
    [IDX_glBindImageTexture] = (void *)e_glBindImageTexture,
    [IDX_glGetBooleani_v] = (void *)e_glGetBooleani_v,
    [IDX_glMemoryBarrier] = (void *)e_glMemoryBarrier,
    [IDX_glMemoryBarrierByRegion] = (void *)e_glMemoryBarrierByRegion,
    [IDX_glTexStorage2DMultisample] = (void *)e_glTexStorage2DMultisample,
    [IDX_glGetMultisamplefv] = (void *)e_glGetMultisamplefv,
    [IDX_glSampleMaski] = (void *)e_glSampleMaski,
    [IDX_glGetTexLevelParameteriv] = (void *)e_glGetTexLevelParameteriv,
    [IDX_glGetTexLevelParameterfv] = (void *)e_glGetTexLevelParameterfv,
    [IDX_glBindVertexBuffer] = (void *)e_glBindVertexBuffer,
    [IDX_glVertexAttribFormat] = (void *)e_glVertexAttribFormat,
    [IDX_glVertexAttribIFormat] = (void *)e_glVertexAttribIFormat,
    [IDX_glVertexAttribBinding] = (void *)e_glVertexAttribBinding,
    [IDX_glVertexBindingDivisor] = (void *)e_glVertexBindingDivisor,
    [IDX_glEGLImageTargetTexture2DOES] = (void *)e_glEGLImageTargetTexture2DOES,
    [IDX_glMapBufferOES] = (void *)e_glMapBufferOES,
    [IDX_glGetGraphicsResetStatusEXT] = (void *)e_glGetGraphicsResetStatusEXT,
    [IDX_glGetGraphicsResetStatusKHR] = (void *)e_glGetGraphicsResetStatusKHR,
    [IDX_glReadnPixelsEXT] = (void *)e_glReadnPixelsEXT,
    [IDX_glReadnPixelsKHR] = (void *)e_glReadnPixelsKHR,
    [IDX_glGetnUniformfvEXT] = (void *)e_glGetnUniformfvEXT,
    [IDX_glGetnUniformivEXT] = (void *)e_glGetnUniformivEXT,
    [IDX_glGetnUniformfvKHR] = (void *)e_glGetnUniformfvKHR,
    [IDX_glGetnUniformivKHR] = (void *)e_glGetnUniformivKHR,
    [IDX_glBindVertexArrayOES] = (void *)e_glBindVertexArray,
    [IDX_glDeleteVertexArraysOES] = (void *)e_glDeleteVertexArrays,
    [IDX_glGenVertexArraysOES] = (void *)e_glGenVertexArrays,
    [IDX_glIsVertexArrayOES] = (void *)e_glIsVertexArray,
    [IDX_glDrawArraysInstancedEXT] = (void *)e_glDrawArraysInstanced,
    [IDX_glDrawElementsInstancedEXT] = (void *)e_glDrawElementsInstanced,
    [IDX_glDrawArraysInstancedANGLE] = (void *)e_glDrawArraysInstanced,
    [IDX_glDrawElementsInstancedANGLE] = (void *)e_glDrawElementsInstanced,
    [IDX_glVertexAttribDivisorEXT] = (void *)e_glVertexAttribDivisor,
    [IDX_glVertexAttribDivisorANGLE] = (void *)e_glVertexAttribDivisor,
    [IDX_glDrawBuffersEXT] = (void *)e_glDrawBuffers,
    [IDX_glBlitFramebufferANGLE] = (void *)e_glBlitFramebuffer,
    [IDX_glRenderbufferStorageMultisampleANGLE] = (void *)e_glRenderbufferStorageMultisample,
    [IDX_glTexStorage2DEXT] = (void *)e_glTexStorage2D,
    [IDX_glMapBufferRangeEXT] = (void *)e_glMapBufferRange,
    [IDX_glFlushMappedBufferRangeEXT] = (void *)e_glFlushMappedBufferRange,
    [IDX_glUnmapBufferOES] = (void *)e_glUnmapBuffer,
    [IDX_glGetBufferPointervOES] = (void *)e_glGetBufferPointerv,
    [IDX_glDiscardFramebufferEXT] = (void *)e_glInvalidateFramebuffer,
    [IDX_glTexImage3DOES] = (void *)e_glTexImage3D,
    [IDX_glTexSubImage3DOES] = (void *)e_glTexSubImage3D,
    [IDX_glGetProgramBinaryOES] = (void *)e_glGetProgramBinary,
    [IDX_glProgramBinaryOES] = (void *)e_glProgramBinary,
};
