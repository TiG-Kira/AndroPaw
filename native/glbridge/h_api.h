/* written by gen.py */
static void dispatch(unsigned op, const uint8_t *p, uint32_t len) {
    (void)len;
    switch (op) {
    case 0: { GLenum texture = (GLenum)rd32(&p); h_glActiveTexture(texture);  break; }
    case 1: { GLuint program = (GLuint)rd32(&p); GLuint shader = (GLuint)rd32(&p); glAttachShader(program, shader);  break; }
    case 2: { GLuint program = (GLuint)rd32(&p); GLuint index = (GLuint)rd32(&p); uint32_t name_n; const GLchar * name = (const GLchar *)rdblob(&p, &name_n); glBindAttribLocation(program, index, name);  break; }
    case 3: { GLenum target = (GLenum)rd32(&p); GLuint buffer = (GLuint)rd32(&p); glBindBuffer(target, buffer);  break; }
    case 4: { GLenum target = (GLenum)rd32(&p); GLuint framebuffer = (GLuint)rd32(&p); h_glBindFramebuffer(target, framebuffer);  break; }
    case 5: { GLenum target = (GLenum)rd32(&p); GLuint renderbuffer = (GLuint)rd32(&p); glBindRenderbuffer(target, renderbuffer);  break; }
    case 6: { GLenum target = (GLenum)rd32(&p); GLuint texture = (GLuint)rd32(&p); h_glBindTexture(target, texture);  break; }
    case 7: { GLfloat red = rdf(&p); GLfloat green = rdf(&p); GLfloat blue = rdf(&p); GLfloat alpha = rdf(&p); glBlendColor(red, green, blue, alpha);  break; }
    case 8: { GLenum mode = (GLenum)rd32(&p); glBlendEquation(mode);  break; }
    case 9: { GLenum modeRGB = (GLenum)rd32(&p); GLenum modeAlpha = (GLenum)rd32(&p); glBlendEquationSeparate(modeRGB, modeAlpha);  break; }
    case 10: { GLenum sfactor = (GLenum)rd32(&p); GLenum dfactor = (GLenum)rd32(&p); glBlendFunc(sfactor, dfactor);  break; }
    case 11: { GLenum sfactorRGB = (GLenum)rd32(&p); GLenum dfactorRGB = (GLenum)rd32(&p); GLenum sfactorAlpha = (GLenum)rd32(&p); GLenum dfactorAlpha = (GLenum)rd32(&p); glBlendFuncSeparate(sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha);  break; }
    case 12: { GLenum target = (GLenum)rd32(&p); GLsizeiptr size = (GLsizeiptr)rd64(&p); uint32_t data_n; const void * data = (const void *)rdblob(&p, &data_n); GLenum usage = (GLenum)rd32(&p); glBufferData(target, size, data, usage);  break; }
    case 13: { GLenum target = (GLenum)rd32(&p); GLintptr offset = (GLintptr)rd64(&p); GLsizeiptr size = (GLsizeiptr)rd64(&p); uint32_t data_n; const void * data = (const void *)rdblob(&p, &data_n); glBufferSubData(target, offset, size, data);  break; }
    case 14: { GLenum target = (GLenum)rd32(&p); GLenum r = glCheckFramebufferStatus(target);  reply32((uint32_t)r); break; }
    case 15: { GLbitfield mask = (GLbitfield)rd32(&p); glClear(mask);  break; }
    case 16: { GLfloat red = rdf(&p); GLfloat green = rdf(&p); GLfloat blue = rdf(&p); GLfloat alpha = rdf(&p); glClearColor(red, green, blue, alpha);  break; }
    case 17: { GLfloat d = rdf(&p); glClearDepthf(d);  break; }
    case 18: { GLint s = (GLint)rd32(&p); glClearStencil(s);  break; }
    case 19: { GLboolean red = (GLboolean)rd32(&p); GLboolean green = (GLboolean)rd32(&p); GLboolean blue = (GLboolean)rd32(&p); GLboolean alpha = (GLboolean)rd32(&p); glColorMask(red, green, blue, alpha);  break; }
    case 20: { GLuint shader = (GLuint)rd32(&p); glCompileShader(shader);  break; }
    case 21: hd_glCompressedTexImage2D(p, len); break;
    case 22: hd_glCompressedTexSubImage2D(p, len); break;
    case 23: { GLenum target = (GLenum)rd32(&p); GLint level = (GLint)rd32(&p); GLenum internalformat = (GLenum)rd32(&p); GLint x = (GLint)rd32(&p); GLint y = (GLint)rd32(&p); GLsizei width = (GLsizei)rd32(&p); GLsizei height = (GLsizei)rd32(&p); GLint border = (GLint)rd32(&p); h_glCopyTexImage2D(target, level, internalformat, x, y, width, height, border);  break; }
    case 24: { GLenum target = (GLenum)rd32(&p); GLint level = (GLint)rd32(&p); GLint xoffset = (GLint)rd32(&p); GLint yoffset = (GLint)rd32(&p); GLint x = (GLint)rd32(&p); GLint y = (GLint)rd32(&p); GLsizei width = (GLsizei)rd32(&p); GLsizei height = (GLsizei)rd32(&p); h_glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height);  break; }
    case 25: {  GLuint r = glCreateProgram();  reply32((uint32_t)r); break; }
    case 26: { GLenum type = (GLenum)rd32(&p); GLuint r = glCreateShader(type);  reply32((uint32_t)r); break; }
    case 27: { GLenum mode = (GLenum)rd32(&p); glCullFace(mode);  break; }
    case 28: { GLsizei n = (GLsizei)rd32(&p); uint32_t buffers_n; const GLuint * buffers = (const GLuint *)rdblob(&p, &buffers_n); glDeleteBuffers(n, buffers);  break; }
    case 29: { GLsizei n = (GLsizei)rd32(&p); uint32_t framebuffers_n; const GLuint * framebuffers = (const GLuint *)rdblob(&p, &framebuffers_n); h_glDeleteFramebuffers(n, framebuffers);  break; }
    case 30: { GLuint program = (GLuint)rd32(&p); glDeleteProgram(program);  break; }
    case 31: { GLsizei n = (GLsizei)rd32(&p); uint32_t renderbuffers_n; const GLuint * renderbuffers = (const GLuint *)rdblob(&p, &renderbuffers_n); glDeleteRenderbuffers(n, renderbuffers);  break; }
    case 32: { GLuint shader = (GLuint)rd32(&p); glDeleteShader(shader);  break; }
    case 33: { GLsizei n = (GLsizei)rd32(&p); uint32_t textures_n; const GLuint * textures = (const GLuint *)rdblob(&p, &textures_n); h_glDeleteTextures(n, textures);  break; }
    case 34: { GLenum func = (GLenum)rd32(&p); glDepthFunc(func);  break; }
    case 35: { GLboolean flag = (GLboolean)rd32(&p); glDepthMask(flag);  break; }
    case 36: { GLfloat n = rdf(&p); GLfloat f = rdf(&p); glDepthRangef(n, f);  break; }
    case 37: { GLuint program = (GLuint)rd32(&p); GLuint shader = (GLuint)rd32(&p); glDetachShader(program, shader);  break; }
    case 38: { GLenum cap = (GLenum)rd32(&p); glDisable(cap);  break; }
    case 39: { GLuint index = (GLuint)rd32(&p); glDisableVertexAttribArray(index);  break; }
    case 40: hd_glDrawArrays(p, len); break;
    case 41: hd_glDrawElements(p, len); break;
    case 42: { GLenum cap = (GLenum)rd32(&p); glEnable(cap);  break; }
    case 43: { GLuint index = (GLuint)rd32(&p); glEnableVertexAttribArray(index);  break; }
    case 44: hd_glFinish(p, len); break;
    case 45: hd_glFlush(p, len); break;
    case 46: { GLenum target = (GLenum)rd32(&p); GLenum attachment = (GLenum)rd32(&p); GLenum renderbuffertarget = (GLenum)rd32(&p); GLuint renderbuffer = (GLuint)rd32(&p); glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer);  break; }
    case 47: { GLenum target = (GLenum)rd32(&p); GLenum attachment = (GLenum)rd32(&p); GLenum textarget = (GLenum)rd32(&p); GLuint texture = (GLuint)rd32(&p); GLint level = (GLint)rd32(&p); h_glFramebufferTexture2D(target, attachment, textarget, texture, level);  break; }
    case 48: { GLenum mode = (GLenum)rd32(&p); glFrontFace(mode);  break; }
    case 49: { GLsizei n = (GLsizei)rd32(&p); uint32_t buffers_n = (uint32_t)((n * 4) > 0 ? (n * 4) : 0); GLuint * buffers = (GLuint *)scratch(buffers_n); glGenBuffers(n, buffers); reply(buffers, buffers_n); break; }
    case 50: { GLenum target = (GLenum)rd32(&p); h_glGenerateMipmap(target);  break; }
    case 51: { GLsizei n = (GLsizei)rd32(&p); uint32_t framebuffers_n = (uint32_t)((n * 4) > 0 ? (n * 4) : 0); GLuint * framebuffers = (GLuint *)scratch(framebuffers_n); glGenFramebuffers(n, framebuffers); reply(framebuffers, framebuffers_n); break; }
    case 52: { GLsizei n = (GLsizei)rd32(&p); uint32_t renderbuffers_n = (uint32_t)((n * 4) > 0 ? (n * 4) : 0); GLuint * renderbuffers = (GLuint *)scratch(renderbuffers_n); glGenRenderbuffers(n, renderbuffers); reply(renderbuffers, renderbuffers_n); break; }
    case 53: { GLsizei n = (GLsizei)rd32(&p); uint32_t textures_n = (uint32_t)((n * 4) > 0 ? (n * 4) : 0); GLuint * textures = (GLuint *)scratch(textures_n); glGenTextures(n, textures); reply(textures, textures_n); break; }
    case 54: { GLuint program = (GLuint)rd32(&p); GLuint index = (GLuint)rd32(&p); GLsizei bufSize = (GLsizei)rd32(&p); uint32_t length_n = (uint32_t)((4) > 0 ? (4) : 0); GLsizei * length = (GLsizei *)scratch(length_n); uint32_t size_n = (uint32_t)((4) > 0 ? (4) : 0); GLint * size = (GLint *)scratch(size_n); uint32_t type_n = (uint32_t)((4) > 0 ? (4) : 0); GLenum * type = (GLenum *)scratch(type_n); uint32_t name_n = (uint32_t)((bufSize) > 0 ? (bufSize) : 0); GLchar * name = (GLchar *)scratch(name_n); glGetActiveAttrib(program, index, bufSize, length, size, type, name); reply(length, length_n); reply(size, size_n); reply(type, type_n); reply(name, name_n); break; }
    case 55: { GLuint program = (GLuint)rd32(&p); GLuint index = (GLuint)rd32(&p); GLsizei bufSize = (GLsizei)rd32(&p); uint32_t length_n = (uint32_t)((4) > 0 ? (4) : 0); GLsizei * length = (GLsizei *)scratch(length_n); uint32_t size_n = (uint32_t)((4) > 0 ? (4) : 0); GLint * size = (GLint *)scratch(size_n); uint32_t type_n = (uint32_t)((4) > 0 ? (4) : 0); GLenum * type = (GLenum *)scratch(type_n); uint32_t name_n = (uint32_t)((bufSize) > 0 ? (bufSize) : 0); GLchar * name = (GLchar *)scratch(name_n); glGetActiveUniform(program, index, bufSize, length, size, type, name); reply(length, length_n); reply(size, size_n); reply(type, type_n); reply(name, name_n); break; }
    case 56: { GLuint program = (GLuint)rd32(&p); GLsizei maxCount = (GLsizei)rd32(&p); uint32_t count_n = (uint32_t)((4) > 0 ? (4) : 0); GLsizei * count = (GLsizei *)scratch(count_n); uint32_t shaders_n = (uint32_t)((maxCount * 4) > 0 ? (maxCount * 4) : 0); GLuint * shaders = (GLuint *)scratch(shaders_n); glGetAttachedShaders(program, maxCount, count, shaders); reply(count, count_n); reply(shaders, shaders_n); break; }
    case 57: { GLuint program = (GLuint)rd32(&p); uint32_t name_n; const GLchar * name = (const GLchar *)rdblob(&p, &name_n); GLint r = glGetAttribLocation(program, name);  reply32((uint32_t)r); break; }
    case 58: { GLenum pname = (GLenum)rd32(&p); uint32_t data_n = (uint32_t)((aemu_get_count(pname)) > 0 ? (aemu_get_count(pname)) : 0); GLboolean * data = (GLboolean *)scratch(data_n); glGetBooleanv(pname, data); reply(data, data_n); break; }
    case 59: { GLenum target = (GLenum)rd32(&p); GLenum pname = (GLenum)rd32(&p); uint32_t params_n = (uint32_t)((4) > 0 ? (4) : 0); GLint * params = (GLint *)scratch(params_n); glGetBufferParameteriv(target, pname, params); reply(params, params_n); break; }
    case 60: {  GLenum r = glGetError();  reply32((uint32_t)r); break; }
    case 61: { GLenum pname = (GLenum)rd32(&p); uint32_t data_n = (uint32_t)((aemu_get_count(pname) * 4) > 0 ? (aemu_get_count(pname) * 4) : 0); GLfloat * data = (GLfloat *)scratch(data_n); glGetFloatv(pname, data); reply(data, data_n); break; }
    case 62: { GLenum target = (GLenum)rd32(&p); GLenum attachment = (GLenum)rd32(&p); GLenum pname = (GLenum)rd32(&p); uint32_t params_n = (uint32_t)((4) > 0 ? (4) : 0); GLint * params = (GLint *)scratch(params_n); glGetFramebufferAttachmentParameteriv(target, attachment, pname, params); reply(params, params_n); break; }
    case 63: { GLenum pname = (GLenum)rd32(&p); uint32_t data_n = (uint32_t)((aemu_get_count(pname) * 4) > 0 ? (aemu_get_count(pname) * 4) : 0); GLint * data = (GLint *)scratch(data_n); h_glGetIntegerv(pname, data); reply(data, data_n); break; }
    case 64: { GLuint program = (GLuint)rd32(&p); GLenum pname = (GLenum)rd32(&p); uint32_t params_n = (uint32_t)((4) > 0 ? (4) : 0); GLint * params = (GLint *)scratch(params_n); glGetProgramiv(program, pname, params); reply(params, params_n); break; }
    case 65: { GLuint program = (GLuint)rd32(&p); GLsizei bufSize = (GLsizei)rd32(&p); uint32_t length_n = (uint32_t)((4) > 0 ? (4) : 0); GLsizei * length = (GLsizei *)scratch(length_n); uint32_t infoLog_n = (uint32_t)((bufSize) > 0 ? (bufSize) : 0); GLchar * infoLog = (GLchar *)scratch(infoLog_n); glGetProgramInfoLog(program, bufSize, length, infoLog); reply(length, length_n); reply(infoLog, infoLog_n); break; }
    case 66: { GLenum target = (GLenum)rd32(&p); GLenum pname = (GLenum)rd32(&p); uint32_t params_n = (uint32_t)((4) > 0 ? (4) : 0); GLint * params = (GLint *)scratch(params_n); glGetRenderbufferParameteriv(target, pname, params); reply(params, params_n); break; }
    case 67: { GLuint shader = (GLuint)rd32(&p); GLenum pname = (GLenum)rd32(&p); uint32_t params_n = (uint32_t)((4) > 0 ? (4) : 0); GLint * params = (GLint *)scratch(params_n); glGetShaderiv(shader, pname, params); reply(params, params_n); break; }
    case 68: { GLuint shader = (GLuint)rd32(&p); GLsizei bufSize = (GLsizei)rd32(&p); uint32_t length_n = (uint32_t)((4) > 0 ? (4) : 0); GLsizei * length = (GLsizei *)scratch(length_n); uint32_t infoLog_n = (uint32_t)((bufSize) > 0 ? (bufSize) : 0); GLchar * infoLog = (GLchar *)scratch(infoLog_n); glGetShaderInfoLog(shader, bufSize, length, infoLog); reply(length, length_n); reply(infoLog, infoLog_n); break; }
    case 69: { GLenum shadertype = (GLenum)rd32(&p); GLenum precisiontype = (GLenum)rd32(&p); uint32_t range_n = (uint32_t)((8) > 0 ? (8) : 0); GLint * range = (GLint *)scratch(range_n); uint32_t precision_n = (uint32_t)((4) > 0 ? (4) : 0); GLint * precision = (GLint *)scratch(precision_n); glGetShaderPrecisionFormat(shadertype, precisiontype, range, precision); reply(range, range_n); reply(precision, precision_n); break; }
    case 70: { GLuint shader = (GLuint)rd32(&p); GLsizei bufSize = (GLsizei)rd32(&p); uint32_t length_n = (uint32_t)((4) > 0 ? (4) : 0); GLsizei * length = (GLsizei *)scratch(length_n); uint32_t source_n = (uint32_t)((bufSize) > 0 ? (bufSize) : 0); GLchar * source = (GLchar *)scratch(source_n); glGetShaderSource(shader, bufSize, length, source); reply(length, length_n); reply(source, source_n); break; }
    case 71: hd_glGetString(p, len); break;
    case 72: { GLenum target = (GLenum)rd32(&p); GLenum pname = (GLenum)rd32(&p); uint32_t params_n = (uint32_t)((4) > 0 ? (4) : 0); GLfloat * params = (GLfloat *)scratch(params_n); h_glGetTexParameterfv(target, pname, params); reply(params, params_n); break; }
    case 73: { GLenum target = (GLenum)rd32(&p); GLenum pname = (GLenum)rd32(&p); uint32_t params_n = (uint32_t)((4) > 0 ? (4) : 0); GLint * params = (GLint *)scratch(params_n); h_glGetTexParameteriv(target, pname, params); reply(params, params_n); break; }
    case 74: hd_glGetUniformfv(p, len); break;
    case 75: hd_glGetUniformiv(p, len); break;
    case 76: { GLuint program = (GLuint)rd32(&p); uint32_t name_n; const GLchar * name = (const GLchar *)rdblob(&p, &name_n); GLint r = glGetUniformLocation(program, name);  reply32((uint32_t)r); break; }
    case 77: { GLuint index = (GLuint)rd32(&p); GLenum pname = (GLenum)rd32(&p); uint32_t params_n = (uint32_t)((pname == 0x8626 ? 16 : 4) > 0 ? (pname == 0x8626 ? 16 : 4) : 0); GLfloat * params = (GLfloat *)scratch(params_n); glGetVertexAttribfv(index, pname, params); reply(params, params_n); break; }
    case 78: { GLuint index = (GLuint)rd32(&p); GLenum pname = (GLenum)rd32(&p); uint32_t params_n = (uint32_t)((pname == 0x8626 ? 16 : 4) > 0 ? (pname == 0x8626 ? 16 : 4) : 0); GLint * params = (GLint *)scratch(params_n); glGetVertexAttribiv(index, pname, params); reply(params, params_n); break; }
    case 79: hd_glGetVertexAttribPointerv(p, len); break;
    case 80: { GLenum target = (GLenum)rd32(&p); GLenum mode = (GLenum)rd32(&p); glHint(target, mode);  break; }
    case 81: { GLuint buffer = (GLuint)rd32(&p); GLboolean r = glIsBuffer(buffer);  reply32((uint32_t)r); break; }
    case 82: { GLenum cap = (GLenum)rd32(&p); GLboolean r = glIsEnabled(cap);  reply32((uint32_t)r); break; }
    case 83: { GLuint framebuffer = (GLuint)rd32(&p); GLboolean r = glIsFramebuffer(framebuffer);  reply32((uint32_t)r); break; }
    case 84: { GLuint program = (GLuint)rd32(&p); GLboolean r = glIsProgram(program);  reply32((uint32_t)r); break; }
    case 85: { GLuint renderbuffer = (GLuint)rd32(&p); GLboolean r = glIsRenderbuffer(renderbuffer);  reply32((uint32_t)r); break; }
    case 86: { GLuint shader = (GLuint)rd32(&p); GLboolean r = glIsShader(shader);  reply32((uint32_t)r); break; }
    case 87: { GLuint texture = (GLuint)rd32(&p); GLboolean r = glIsTexture(texture);  reply32((uint32_t)r); break; }
    case 88: { GLfloat width = rdf(&p); glLineWidth(width);  break; }
    case 89: { GLuint program = (GLuint)rd32(&p); h_glLinkProgram(program);  break; }
    case 90: { GLenum pname = (GLenum)rd32(&p); GLint param = (GLint)rd32(&p); glPixelStorei(pname, param);  break; }
    case 91: { GLfloat factor = rdf(&p); GLfloat units = rdf(&p); glPolygonOffset(factor, units);  break; }
    case 92: hd_glReadPixels(p, len); break;
    case 93: {  glReleaseShaderCompiler();  break; }
    case 94: { GLenum target = (GLenum)rd32(&p); GLenum internalformat = (GLenum)rd32(&p); GLsizei width = (GLsizei)rd32(&p); GLsizei height = (GLsizei)rd32(&p); glRenderbufferStorage(target, internalformat, width, height);  break; }
    case 95: { GLfloat value = rdf(&p); GLboolean invert = (GLboolean)rd32(&p); glSampleCoverage(value, invert);  break; }
    case 96: { GLint x = (GLint)rd32(&p); GLint y = (GLint)rd32(&p); GLsizei width = (GLsizei)rd32(&p); GLsizei height = (GLsizei)rd32(&p); glScissor(x, y, width, height);  break; }
    case 97: { GLsizei count = (GLsizei)rd32(&p); uint32_t shaders_n; const GLuint * shaders = (const GLuint *)rdblob(&p, &shaders_n); GLenum binaryformat = (GLenum)rd32(&p); uint32_t binary_n; const void * binary = (const void *)rdblob(&p, &binary_n); GLsizei length = (GLsizei)rd32(&p); glShaderBinary(count, shaders, binaryformat, binary, length);  break; }
    case 98: hd_glShaderSource(p, len); break;
    case 99: { GLenum func = (GLenum)rd32(&p); GLint ref = (GLint)rd32(&p); GLuint mask = (GLuint)rd32(&p); glStencilFunc(func, ref, mask);  break; }
    case 100: { GLenum face = (GLenum)rd32(&p); GLenum func = (GLenum)rd32(&p); GLint ref = (GLint)rd32(&p); GLuint mask = (GLuint)rd32(&p); glStencilFuncSeparate(face, func, ref, mask);  break; }
    case 101: { GLuint mask = (GLuint)rd32(&p); glStencilMask(mask);  break; }
    case 102: { GLenum face = (GLenum)rd32(&p); GLuint mask = (GLuint)rd32(&p); glStencilMaskSeparate(face, mask);  break; }
    case 103: { GLenum fail = (GLenum)rd32(&p); GLenum zfail = (GLenum)rd32(&p); GLenum zpass = (GLenum)rd32(&p); glStencilOp(fail, zfail, zpass);  break; }
    case 104: { GLenum face = (GLenum)rd32(&p); GLenum sfail = (GLenum)rd32(&p); GLenum dpfail = (GLenum)rd32(&p); GLenum dppass = (GLenum)rd32(&p); glStencilOpSeparate(face, sfail, dpfail, dppass);  break; }
    case 105: hd_glTexImage2D(p, len); break;
    case 106: { GLenum target = (GLenum)rd32(&p); GLenum pname = (GLenum)rd32(&p); GLfloat param = rdf(&p); h_glTexParameterf(target, pname, param);  break; }
    case 107: { GLenum target = (GLenum)rd32(&p); GLenum pname = (GLenum)rd32(&p); uint32_t params_n; const GLfloat * params = (const GLfloat *)rdblob(&p, &params_n); h_glTexParameterfv(target, pname, params);  break; }
    case 108: { GLenum target = (GLenum)rd32(&p); GLenum pname = (GLenum)rd32(&p); GLint param = (GLint)rd32(&p); h_glTexParameteri(target, pname, param);  break; }
    case 109: { GLenum target = (GLenum)rd32(&p); GLenum pname = (GLenum)rd32(&p); uint32_t params_n; const GLint * params = (const GLint *)rdblob(&p, &params_n); h_glTexParameteriv(target, pname, params);  break; }
    case 110: hd_glTexSubImage2D(p, len); break;
    case 111: { GLint location = (GLint)rd32(&p); GLfloat v0 = rdf(&p); glUniform1f(location, v0);  break; }
    case 112: { GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); uint32_t value_n; const GLfloat * value = (const GLfloat *)rdblob(&p, &value_n); glUniform1fv(location, count, value);  break; }
    case 113: { GLint location = (GLint)rd32(&p); GLint v0 = (GLint)rd32(&p); glUniform1i(location, v0);  break; }
    case 114: { GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); uint32_t value_n; const GLint * value = (const GLint *)rdblob(&p, &value_n); glUniform1iv(location, count, value);  break; }
    case 115: { GLint location = (GLint)rd32(&p); GLfloat v0 = rdf(&p); GLfloat v1 = rdf(&p); glUniform2f(location, v0, v1);  break; }
    case 116: { GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); uint32_t value_n; const GLfloat * value = (const GLfloat *)rdblob(&p, &value_n); glUniform2fv(location, count, value);  break; }
    case 117: { GLint location = (GLint)rd32(&p); GLint v0 = (GLint)rd32(&p); GLint v1 = (GLint)rd32(&p); glUniform2i(location, v0, v1);  break; }
    case 118: { GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); uint32_t value_n; const GLint * value = (const GLint *)rdblob(&p, &value_n); glUniform2iv(location, count, value);  break; }
    case 119: { GLint location = (GLint)rd32(&p); GLfloat v0 = rdf(&p); GLfloat v1 = rdf(&p); GLfloat v2 = rdf(&p); glUniform3f(location, v0, v1, v2);  break; }
    case 120: { GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); uint32_t value_n; const GLfloat * value = (const GLfloat *)rdblob(&p, &value_n); glUniform3fv(location, count, value);  break; }
    case 121: { GLint location = (GLint)rd32(&p); GLint v0 = (GLint)rd32(&p); GLint v1 = (GLint)rd32(&p); GLint v2 = (GLint)rd32(&p); glUniform3i(location, v0, v1, v2);  break; }
    case 122: { GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); uint32_t value_n; const GLint * value = (const GLint *)rdblob(&p, &value_n); glUniform3iv(location, count, value);  break; }
    case 123: { GLint location = (GLint)rd32(&p); GLfloat v0 = rdf(&p); GLfloat v1 = rdf(&p); GLfloat v2 = rdf(&p); GLfloat v3 = rdf(&p); glUniform4f(location, v0, v1, v2, v3);  break; }
    case 124: { GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); uint32_t value_n; const GLfloat * value = (const GLfloat *)rdblob(&p, &value_n); glUniform4fv(location, count, value);  break; }
    case 125: { GLint location = (GLint)rd32(&p); GLint v0 = (GLint)rd32(&p); GLint v1 = (GLint)rd32(&p); GLint v2 = (GLint)rd32(&p); GLint v3 = (GLint)rd32(&p); glUniform4i(location, v0, v1, v2, v3);  break; }
    case 126: { GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); uint32_t value_n; const GLint * value = (const GLint *)rdblob(&p, &value_n); glUniform4iv(location, count, value);  break; }
    case 127: { GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); GLboolean transpose = (GLboolean)rd32(&p); uint32_t value_n; const GLfloat * value = (const GLfloat *)rdblob(&p, &value_n); glUniformMatrix2fv(location, count, transpose, value);  break; }
    case 128: { GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); GLboolean transpose = (GLboolean)rd32(&p); uint32_t value_n; const GLfloat * value = (const GLfloat *)rdblob(&p, &value_n); glUniformMatrix3fv(location, count, transpose, value);  break; }
    case 129: { GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); GLboolean transpose = (GLboolean)rd32(&p); uint32_t value_n; const GLfloat * value = (const GLfloat *)rdblob(&p, &value_n); glUniformMatrix4fv(location, count, transpose, value);  break; }
    case 130: { GLuint program = (GLuint)rd32(&p); h_glUseProgram(program);  break; }
    case 131: { GLuint program = (GLuint)rd32(&p); glValidateProgram(program);  break; }
    case 132: { GLuint index = (GLuint)rd32(&p); GLfloat x = rdf(&p); glVertexAttrib1f(index, x);  break; }
    case 133: { GLuint index = (GLuint)rd32(&p); uint32_t v_n; const GLfloat * v = (const GLfloat *)rdblob(&p, &v_n); glVertexAttrib1fv(index, v);  break; }
    case 134: { GLuint index = (GLuint)rd32(&p); GLfloat x = rdf(&p); GLfloat y = rdf(&p); glVertexAttrib2f(index, x, y);  break; }
    case 135: { GLuint index = (GLuint)rd32(&p); uint32_t v_n; const GLfloat * v = (const GLfloat *)rdblob(&p, &v_n); glVertexAttrib2fv(index, v);  break; }
    case 136: { GLuint index = (GLuint)rd32(&p); GLfloat x = rdf(&p); GLfloat y = rdf(&p); GLfloat z = rdf(&p); glVertexAttrib3f(index, x, y, z);  break; }
    case 137: { GLuint index = (GLuint)rd32(&p); uint32_t v_n; const GLfloat * v = (const GLfloat *)rdblob(&p, &v_n); glVertexAttrib3fv(index, v);  break; }
    case 138: { GLuint index = (GLuint)rd32(&p); GLfloat x = rdf(&p); GLfloat y = rdf(&p); GLfloat z = rdf(&p); GLfloat w = rdf(&p); glVertexAttrib4f(index, x, y, z, w);  break; }
    case 139: { GLuint index = (GLuint)rd32(&p); uint32_t v_n; const GLfloat * v = (const GLfloat *)rdblob(&p, &v_n); glVertexAttrib4fv(index, v);  break; }
    case 140: hd_glVertexAttribPointer(p, len); break;
    case 141: { GLint x = (GLint)rd32(&p); GLint y = (GLint)rd32(&p); GLsizei width = (GLsizei)rd32(&p); GLsizei height = (GLsizei)rd32(&p); glViewport(x, y, width, height);  break; }
    case 142: { GLenum src = (GLenum)rd32(&p); h_glReadBuffer(src);  break; }
    case 143: hd_glDrawRangeElements(p, len); break;
    case 144: hd_glTexImage3D(p, len); break;
    case 145: hd_glTexSubImage3D(p, len); break;
    case 146: { GLenum target = (GLenum)rd32(&p); GLint level = (GLint)rd32(&p); GLint xoffset = (GLint)rd32(&p); GLint yoffset = (GLint)rd32(&p); GLint zoffset = (GLint)rd32(&p); GLint x = (GLint)rd32(&p); GLint y = (GLint)rd32(&p); GLsizei width = (GLsizei)rd32(&p); GLsizei height = (GLsizei)rd32(&p); h_glCopyTexSubImage3D(target, level, xoffset, yoffset, zoffset, x, y, width, height);  break; }
    case 147: hd_glCompressedTexImage3D(p, len); break;
    case 148: hd_glCompressedTexSubImage3D(p, len); break;
    case 149: { GLsizei n = (GLsizei)rd32(&p); uint32_t ids_n = (uint32_t)((n * 4) > 0 ? (n * 4) : 0); GLuint * ids = (GLuint *)scratch(ids_n); glGenQueries(n, ids); reply(ids, ids_n); break; }
    case 150: { GLsizei n = (GLsizei)rd32(&p); uint32_t ids_n; const GLuint * ids = (const GLuint *)rdblob(&p, &ids_n); glDeleteQueries(n, ids);  break; }
    case 151: { GLuint id = (GLuint)rd32(&p); GLboolean r = glIsQuery(id);  reply32((uint32_t)r); break; }
    case 152: { GLenum target = (GLenum)rd32(&p); GLuint id = (GLuint)rd32(&p); glBeginQuery(target, id);  break; }
    case 153: { GLenum target = (GLenum)rd32(&p); glEndQuery(target);  break; }
    case 154: { GLenum target = (GLenum)rd32(&p); GLenum pname = (GLenum)rd32(&p); uint32_t params_n = (uint32_t)((4) > 0 ? (4) : 0); GLint * params = (GLint *)scratch(params_n); glGetQueryiv(target, pname, params); reply(params, params_n); break; }
    case 155: { GLuint id = (GLuint)rd32(&p); GLenum pname = (GLenum)rd32(&p); uint32_t params_n = (uint32_t)((4) > 0 ? (4) : 0); GLuint * params = (GLuint *)scratch(params_n); glGetQueryObjectuiv(id, pname, params); reply(params, params_n); break; }
    case 156: hd_glUnmapBuffer(p, len); break;
    case 157: hd_glGetBufferPointerv(p, len); break;
    case 158: { GLsizei n = (GLsizei)rd32(&p); uint32_t bufs_n; const GLenum * bufs = (const GLenum *)rdblob(&p, &bufs_n); h_glDrawBuffers(n, bufs);  break; }
    case 159: { GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); GLboolean transpose = (GLboolean)rd32(&p); uint32_t value_n; const GLfloat * value = (const GLfloat *)rdblob(&p, &value_n); glUniformMatrix2x3fv(location, count, transpose, value);  break; }
    case 160: { GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); GLboolean transpose = (GLboolean)rd32(&p); uint32_t value_n; const GLfloat * value = (const GLfloat *)rdblob(&p, &value_n); glUniformMatrix3x2fv(location, count, transpose, value);  break; }
    case 161: { GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); GLboolean transpose = (GLboolean)rd32(&p); uint32_t value_n; const GLfloat * value = (const GLfloat *)rdblob(&p, &value_n); glUniformMatrix2x4fv(location, count, transpose, value);  break; }
    case 162: { GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); GLboolean transpose = (GLboolean)rd32(&p); uint32_t value_n; const GLfloat * value = (const GLfloat *)rdblob(&p, &value_n); glUniformMatrix4x2fv(location, count, transpose, value);  break; }
    case 163: { GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); GLboolean transpose = (GLboolean)rd32(&p); uint32_t value_n; const GLfloat * value = (const GLfloat *)rdblob(&p, &value_n); glUniformMatrix3x4fv(location, count, transpose, value);  break; }
    case 164: { GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); GLboolean transpose = (GLboolean)rd32(&p); uint32_t value_n; const GLfloat * value = (const GLfloat *)rdblob(&p, &value_n); glUniformMatrix4x3fv(location, count, transpose, value);  break; }
    case 165: { GLint srcX0 = (GLint)rd32(&p); GLint srcY0 = (GLint)rd32(&p); GLint srcX1 = (GLint)rd32(&p); GLint srcY1 = (GLint)rd32(&p); GLint dstX0 = (GLint)rd32(&p); GLint dstY0 = (GLint)rd32(&p); GLint dstX1 = (GLint)rd32(&p); GLint dstY1 = (GLint)rd32(&p); GLbitfield mask = (GLbitfield)rd32(&p); GLenum filter = (GLenum)rd32(&p); h_glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);  break; }
    case 166: { GLenum target = (GLenum)rd32(&p); GLsizei samples = (GLsizei)rd32(&p); GLenum internalformat = (GLenum)rd32(&p); GLsizei width = (GLsizei)rd32(&p); GLsizei height = (GLsizei)rd32(&p); glRenderbufferStorageMultisample(target, samples, internalformat, width, height);  break; }
    case 167: { GLenum target = (GLenum)rd32(&p); GLenum attachment = (GLenum)rd32(&p); GLuint texture = (GLuint)rd32(&p); GLint level = (GLint)rd32(&p); GLint layer = (GLint)rd32(&p); glFramebufferTextureLayer(target, attachment, texture, level, layer);  break; }
    case 168: hd_glMapBufferRange(p, len); break;
    case 169: hd_glFlushMappedBufferRange(p, len); break;
    case 170: { GLuint array = (GLuint)rd32(&p); glBindVertexArray(array);  break; }
    case 171: { GLsizei n = (GLsizei)rd32(&p); uint32_t arrays_n; const GLuint * arrays = (const GLuint *)rdblob(&p, &arrays_n); glDeleteVertexArrays(n, arrays);  break; }
    case 172: { GLsizei n = (GLsizei)rd32(&p); uint32_t arrays_n = (uint32_t)((n * 4) > 0 ? (n * 4) : 0); GLuint * arrays = (GLuint *)scratch(arrays_n); glGenVertexArrays(n, arrays); reply(arrays, arrays_n); break; }
    case 173: { GLuint array = (GLuint)rd32(&p); GLboolean r = glIsVertexArray(array);  reply32((uint32_t)r); break; }
    case 174: { GLenum target = (GLenum)rd32(&p); GLuint index = (GLuint)rd32(&p); uint32_t data_n = (uint32_t)((4) > 0 ? (4) : 0); GLint * data = (GLint *)scratch(data_n); glGetIntegeri_v(target, index, data); reply(data, data_n); break; }
    case 175: { GLenum primitiveMode = (GLenum)rd32(&p); glBeginTransformFeedback(primitiveMode);  break; }
    case 176: {  glEndTransformFeedback();  break; }
    case 177: { GLenum target = (GLenum)rd32(&p); GLuint index = (GLuint)rd32(&p); GLuint buffer = (GLuint)rd32(&p); GLintptr offset = (GLintptr)rd64(&p); GLsizeiptr size = (GLsizeiptr)rd64(&p); glBindBufferRange(target, index, buffer, offset, size);  break; }
    case 178: { GLenum target = (GLenum)rd32(&p); GLuint index = (GLuint)rd32(&p); GLuint buffer = (GLuint)rd32(&p); glBindBufferBase(target, index, buffer);  break; }
    case 179: hd_glTransformFeedbackVaryings(p, len); break;
    case 180: { GLuint program = (GLuint)rd32(&p); GLuint index = (GLuint)rd32(&p); GLsizei bufSize = (GLsizei)rd32(&p); uint32_t length_n = (uint32_t)((4) > 0 ? (4) : 0); GLsizei * length = (GLsizei *)scratch(length_n); uint32_t size_n = (uint32_t)((4) > 0 ? (4) : 0); GLsizei * size = (GLsizei *)scratch(size_n); uint32_t type_n = (uint32_t)((4) > 0 ? (4) : 0); GLenum * type = (GLenum *)scratch(type_n); uint32_t name_n = (uint32_t)((bufSize) > 0 ? (bufSize) : 0); GLchar * name = (GLchar *)scratch(name_n); glGetTransformFeedbackVarying(program, index, bufSize, length, size, type, name); reply(length, length_n); reply(size, size_n); reply(type, type_n); reply(name, name_n); break; }
    case 181: hd_glVertexAttribIPointer(p, len); break;
    case 182: { GLuint index = (GLuint)rd32(&p); GLenum pname = (GLenum)rd32(&p); uint32_t params_n = (uint32_t)((pname == 0x8626 ? 16 : 4) > 0 ? (pname == 0x8626 ? 16 : 4) : 0); GLint * params = (GLint *)scratch(params_n); glGetVertexAttribIiv(index, pname, params); reply(params, params_n); break; }
    case 183: { GLuint index = (GLuint)rd32(&p); GLenum pname = (GLenum)rd32(&p); uint32_t params_n = (uint32_t)((pname == 0x8626 ? 16 : 4) > 0 ? (pname == 0x8626 ? 16 : 4) : 0); GLuint * params = (GLuint *)scratch(params_n); glGetVertexAttribIuiv(index, pname, params); reply(params, params_n); break; }
    case 184: { GLuint index = (GLuint)rd32(&p); GLint x = (GLint)rd32(&p); GLint y = (GLint)rd32(&p); GLint z = (GLint)rd32(&p); GLint w = (GLint)rd32(&p); glVertexAttribI4i(index, x, y, z, w);  break; }
    case 185: { GLuint index = (GLuint)rd32(&p); GLuint x = (GLuint)rd32(&p); GLuint y = (GLuint)rd32(&p); GLuint z = (GLuint)rd32(&p); GLuint w = (GLuint)rd32(&p); glVertexAttribI4ui(index, x, y, z, w);  break; }
    case 186: { GLuint index = (GLuint)rd32(&p); uint32_t v_n; const GLint * v = (const GLint *)rdblob(&p, &v_n); glVertexAttribI4iv(index, v);  break; }
    case 187: { GLuint index = (GLuint)rd32(&p); uint32_t v_n; const GLuint * v = (const GLuint *)rdblob(&p, &v_n); glVertexAttribI4uiv(index, v);  break; }
    case 188: hd_glGetUniformuiv(p, len); break;
    case 189: { GLuint program = (GLuint)rd32(&p); uint32_t name_n; const GLchar * name = (const GLchar *)rdblob(&p, &name_n); GLint r = glGetFragDataLocation(program, name);  reply32((uint32_t)r); break; }
    case 190: { GLint location = (GLint)rd32(&p); GLuint v0 = (GLuint)rd32(&p); glUniform1ui(location, v0);  break; }
    case 191: { GLint location = (GLint)rd32(&p); GLuint v0 = (GLuint)rd32(&p); GLuint v1 = (GLuint)rd32(&p); glUniform2ui(location, v0, v1);  break; }
    case 192: { GLint location = (GLint)rd32(&p); GLuint v0 = (GLuint)rd32(&p); GLuint v1 = (GLuint)rd32(&p); GLuint v2 = (GLuint)rd32(&p); glUniform3ui(location, v0, v1, v2);  break; }
    case 193: { GLint location = (GLint)rd32(&p); GLuint v0 = (GLuint)rd32(&p); GLuint v1 = (GLuint)rd32(&p); GLuint v2 = (GLuint)rd32(&p); GLuint v3 = (GLuint)rd32(&p); glUniform4ui(location, v0, v1, v2, v3);  break; }
    case 194: { GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); uint32_t value_n; const GLuint * value = (const GLuint *)rdblob(&p, &value_n); glUniform1uiv(location, count, value);  break; }
    case 195: { GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); uint32_t value_n; const GLuint * value = (const GLuint *)rdblob(&p, &value_n); glUniform2uiv(location, count, value);  break; }
    case 196: { GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); uint32_t value_n; const GLuint * value = (const GLuint *)rdblob(&p, &value_n); glUniform3uiv(location, count, value);  break; }
    case 197: { GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); uint32_t value_n; const GLuint * value = (const GLuint *)rdblob(&p, &value_n); glUniform4uiv(location, count, value);  break; }
    case 198: { GLenum buffer = (GLenum)rd32(&p); GLint drawbuffer = (GLint)rd32(&p); uint32_t value_n; const GLint * value = (const GLint *)rdblob(&p, &value_n); glClearBufferiv(buffer, drawbuffer, value);  break; }
    case 199: { GLenum buffer = (GLenum)rd32(&p); GLint drawbuffer = (GLint)rd32(&p); uint32_t value_n; const GLuint * value = (const GLuint *)rdblob(&p, &value_n); glClearBufferuiv(buffer, drawbuffer, value);  break; }
    case 200: { GLenum buffer = (GLenum)rd32(&p); GLint drawbuffer = (GLint)rd32(&p); uint32_t value_n; const GLfloat * value = (const GLfloat *)rdblob(&p, &value_n); glClearBufferfv(buffer, drawbuffer, value);  break; }
    case 201: { GLenum buffer = (GLenum)rd32(&p); GLint drawbuffer = (GLint)rd32(&p); GLfloat depth = rdf(&p); GLint stencil = (GLint)rd32(&p); glClearBufferfi(buffer, drawbuffer, depth, stencil);  break; }
    case 202: hd_glGetStringi(p, len); break;
    case 203: { GLenum readTarget = (GLenum)rd32(&p); GLenum writeTarget = (GLenum)rd32(&p); GLintptr readOffset = (GLintptr)rd64(&p); GLintptr writeOffset = (GLintptr)rd64(&p); GLsizeiptr size = (GLsizeiptr)rd64(&p); glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size);  break; }
    case 204: hd_glGetUniformIndices(p, len); break;
    case 205: { GLuint program = (GLuint)rd32(&p); GLsizei uniformCount = (GLsizei)rd32(&p); uint32_t uniformIndices_n; const GLuint * uniformIndices = (const GLuint *)rdblob(&p, &uniformIndices_n); GLenum pname = (GLenum)rd32(&p); uint32_t params_n = (uint32_t)((uniformCount * 4) > 0 ? (uniformCount * 4) : 0); GLint * params = (GLint *)scratch(params_n); glGetActiveUniformsiv(program, uniformCount, uniformIndices, pname, params); reply(params, params_n); break; }
    case 206: { GLuint program = (GLuint)rd32(&p); uint32_t uniformBlockName_n; const GLchar * uniformBlockName = (const GLchar *)rdblob(&p, &uniformBlockName_n); GLuint r = glGetUniformBlockIndex(program, uniformBlockName);  reply32((uint32_t)r); break; }
    case 207: { GLuint program = (GLuint)rd32(&p); GLuint uniformBlockIndex = (GLuint)rd32(&p); GLenum pname = (GLenum)rd32(&p); uint32_t params_n = (uint32_t)((4) > 0 ? (4) : 0); GLint * params = (GLint *)scratch(params_n); glGetActiveUniformBlockiv(program, uniformBlockIndex, pname, params); reply(params, params_n); break; }
    case 208: { GLuint program = (GLuint)rd32(&p); GLuint uniformBlockIndex = (GLuint)rd32(&p); GLsizei bufSize = (GLsizei)rd32(&p); uint32_t length_n = (uint32_t)((4) > 0 ? (4) : 0); GLsizei * length = (GLsizei *)scratch(length_n); uint32_t uniformBlockName_n = (uint32_t)((bufSize) > 0 ? (bufSize) : 0); GLchar * uniformBlockName = (GLchar *)scratch(uniformBlockName_n); glGetActiveUniformBlockName(program, uniformBlockIndex, bufSize, length, uniformBlockName); reply(length, length_n); reply(uniformBlockName, uniformBlockName_n); break; }
    case 209: { GLuint program = (GLuint)rd32(&p); GLuint uniformBlockIndex = (GLuint)rd32(&p); GLuint uniformBlockBinding = (GLuint)rd32(&p); glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding);  break; }
    case 210: hd_glDrawArraysInstanced(p, len); break;
    case 211: hd_glDrawElementsInstanced(p, len); break;
    case 212: { GLenum condition = (GLenum)rd32(&p); GLbitfield flags = (GLbitfield)rd32(&p); GLsync r = glFenceSync(condition, flags);  reply64((uint64_t)r); break; }
    case 213: { GLsync sync = (GLsync)rd64(&p); GLboolean r = glIsSync(sync);  reply32((uint32_t)r); break; }
    case 214: { GLsync sync = (GLsync)rd64(&p); glDeleteSync(sync);  break; }
    case 215: { GLsync sync = (GLsync)rd64(&p); GLbitfield flags = (GLbitfield)rd32(&p); GLuint64 timeout = (GLuint64)rd64(&p); GLenum r = glClientWaitSync(sync, flags, timeout);  reply32((uint32_t)r); break; }
    case 216: { GLsync sync = (GLsync)rd64(&p); GLbitfield flags = (GLbitfield)rd32(&p); GLuint64 timeout = (GLuint64)rd64(&p); glWaitSync(sync, flags, timeout);  break; }
    case 217: { GLenum pname = (GLenum)rd32(&p); uint32_t data_n = (uint32_t)((aemu_get_count(pname) * 8) > 0 ? (aemu_get_count(pname) * 8) : 0); GLint64 * data = (GLint64 *)scratch(data_n); glGetInteger64v(pname, data); reply(data, data_n); break; }
    case 218: { GLsync sync = (GLsync)rd64(&p); GLenum pname = (GLenum)rd32(&p); GLsizei bufSize = (GLsizei)rd32(&p); uint32_t length_n = (uint32_t)((4) > 0 ? (4) : 0); GLsizei * length = (GLsizei *)scratch(length_n); uint32_t values_n = (uint32_t)((bufSize * 4) > 0 ? (bufSize * 4) : 0); GLint * values = (GLint *)scratch(values_n); glGetSynciv(sync, pname, bufSize, length, values); reply(length, length_n); reply(values, values_n); break; }
    case 219: { GLenum target = (GLenum)rd32(&p); GLuint index = (GLuint)rd32(&p); uint32_t data_n = (uint32_t)((8) > 0 ? (8) : 0); GLint64 * data = (GLint64 *)scratch(data_n); glGetInteger64i_v(target, index, data); reply(data, data_n); break; }
    case 220: { GLenum target = (GLenum)rd32(&p); GLenum pname = (GLenum)rd32(&p); uint32_t params_n = (uint32_t)((8) > 0 ? (8) : 0); GLint64 * params = (GLint64 *)scratch(params_n); glGetBufferParameteri64v(target, pname, params); reply(params, params_n); break; }
    case 221: { GLsizei count = (GLsizei)rd32(&p); uint32_t samplers_n = (uint32_t)((count * 4) > 0 ? (count * 4) : 0); GLuint * samplers = (GLuint *)scratch(samplers_n); glGenSamplers(count, samplers); reply(samplers, samplers_n); break; }
    case 222: { GLsizei count = (GLsizei)rd32(&p); uint32_t samplers_n; const GLuint * samplers = (const GLuint *)rdblob(&p, &samplers_n); glDeleteSamplers(count, samplers);  break; }
    case 223: { GLuint sampler = (GLuint)rd32(&p); GLboolean r = glIsSampler(sampler);  reply32((uint32_t)r); break; }
    case 224: { GLuint unit = (GLuint)rd32(&p); GLuint sampler = (GLuint)rd32(&p); glBindSampler(unit, sampler);  break; }
    case 225: { GLuint sampler = (GLuint)rd32(&p); GLenum pname = (GLenum)rd32(&p); GLint param = (GLint)rd32(&p); glSamplerParameteri(sampler, pname, param);  break; }
    case 226: { GLuint sampler = (GLuint)rd32(&p); GLenum pname = (GLenum)rd32(&p); uint32_t param_n; const GLint * param = (const GLint *)rdblob(&p, &param_n); glSamplerParameteriv(sampler, pname, param);  break; }
    case 227: { GLuint sampler = (GLuint)rd32(&p); GLenum pname = (GLenum)rd32(&p); GLfloat param = rdf(&p); glSamplerParameterf(sampler, pname, param);  break; }
    case 228: { GLuint sampler = (GLuint)rd32(&p); GLenum pname = (GLenum)rd32(&p); uint32_t param_n; const GLfloat * param = (const GLfloat *)rdblob(&p, &param_n); glSamplerParameterfv(sampler, pname, param);  break; }
    case 229: { GLuint sampler = (GLuint)rd32(&p); GLenum pname = (GLenum)rd32(&p); uint32_t params_n = (uint32_t)((4) > 0 ? (4) : 0); GLint * params = (GLint *)scratch(params_n); glGetSamplerParameteriv(sampler, pname, params); reply(params, params_n); break; }
    case 230: { GLuint sampler = (GLuint)rd32(&p); GLenum pname = (GLenum)rd32(&p); uint32_t params_n = (uint32_t)((4) > 0 ? (4) : 0); GLfloat * params = (GLfloat *)scratch(params_n); glGetSamplerParameterfv(sampler, pname, params); reply(params, params_n); break; }
    case 231: { GLuint index = (GLuint)rd32(&p); GLuint divisor = (GLuint)rd32(&p); glVertexAttribDivisor(index, divisor);  break; }
    case 232: { GLenum target = (GLenum)rd32(&p); GLuint id = (GLuint)rd32(&p); glBindTransformFeedback(target, id);  break; }
    case 233: { GLsizei n = (GLsizei)rd32(&p); uint32_t ids_n; const GLuint * ids = (const GLuint *)rdblob(&p, &ids_n); glDeleteTransformFeedbacks(n, ids);  break; }
    case 234: { GLsizei n = (GLsizei)rd32(&p); uint32_t ids_n = (uint32_t)((n * 4) > 0 ? (n * 4) : 0); GLuint * ids = (GLuint *)scratch(ids_n); glGenTransformFeedbacks(n, ids); reply(ids, ids_n); break; }
    case 235: { GLuint id = (GLuint)rd32(&p); GLboolean r = glIsTransformFeedback(id);  reply32((uint32_t)r); break; }
    case 236: {  glPauseTransformFeedback();  break; }
    case 237: {  glResumeTransformFeedback();  break; }
    case 238: { GLuint program = (GLuint)rd32(&p); GLsizei bufSize = (GLsizei)rd32(&p); uint32_t length_n = (uint32_t)((4) > 0 ? (4) : 0); GLsizei * length = (GLsizei *)scratch(length_n); uint32_t binaryFormat_n = (uint32_t)((4) > 0 ? (4) : 0); GLenum * binaryFormat = (GLenum *)scratch(binaryFormat_n); uint32_t binary_n = (uint32_t)((bufSize) > 0 ? (bufSize) : 0); void * binary = (void *)scratch(binary_n); glGetProgramBinary(program, bufSize, length, binaryFormat, binary); reply(length, length_n); reply(binaryFormat, binaryFormat_n); reply(binary, binary_n); break; }
    case 239: { GLuint program = (GLuint)rd32(&p); GLenum binaryFormat = (GLenum)rd32(&p); uint32_t binary_n; const void * binary = (const void *)rdblob(&p, &binary_n); GLsizei length = (GLsizei)rd32(&p); glProgramBinary(program, binaryFormat, binary, length);  break; }
    case 240: { GLuint program = (GLuint)rd32(&p); GLenum pname = (GLenum)rd32(&p); GLint value = (GLint)rd32(&p); glProgramParameteri(program, pname, value);  break; }
    case 241: { GLenum target = (GLenum)rd32(&p); GLsizei numAttachments = (GLsizei)rd32(&p); uint32_t attachments_n; const GLenum * attachments = (const GLenum *)rdblob(&p, &attachments_n); h_glInvalidateFramebuffer(target, numAttachments, attachments);  break; }
    case 242: { GLenum target = (GLenum)rd32(&p); GLsizei numAttachments = (GLsizei)rd32(&p); uint32_t attachments_n; const GLenum * attachments = (const GLenum *)rdblob(&p, &attachments_n); GLint x = (GLint)rd32(&p); GLint y = (GLint)rd32(&p); GLsizei width = (GLsizei)rd32(&p); GLsizei height = (GLsizei)rd32(&p); h_glInvalidateSubFramebuffer(target, numAttachments, attachments, x, y, width, height);  break; }
    case 243: { GLenum target = (GLenum)rd32(&p); GLsizei levels = (GLsizei)rd32(&p); GLenum internalformat = (GLenum)rd32(&p); GLsizei width = (GLsizei)rd32(&p); GLsizei height = (GLsizei)rd32(&p); glTexStorage2D(target, levels, internalformat, width, height);  break; }
    case 244: { GLenum target = (GLenum)rd32(&p); GLsizei levels = (GLsizei)rd32(&p); GLenum internalformat = (GLenum)rd32(&p); GLsizei width = (GLsizei)rd32(&p); GLsizei height = (GLsizei)rd32(&p); GLsizei depth = (GLsizei)rd32(&p); glTexStorage3D(target, levels, internalformat, width, height, depth);  break; }
    case 245: { GLenum target = (GLenum)rd32(&p); GLenum internalformat = (GLenum)rd32(&p); GLenum pname = (GLenum)rd32(&p); GLsizei bufSize = (GLsizei)rd32(&p); uint32_t params_n = (uint32_t)((bufSize * 4) > 0 ? (bufSize * 4) : 0); GLint * params = (GLint *)scratch(params_n); glGetInternalformativ(target, internalformat, pname, bufSize, params); reply(params, params_n); break; }
    case 246: { GLuint num_groups_x = (GLuint)rd32(&p); GLuint num_groups_y = (GLuint)rd32(&p); GLuint num_groups_z = (GLuint)rd32(&p); glDispatchCompute(num_groups_x, num_groups_y, num_groups_z);  break; }
    case 247: { GLintptr indirect = (GLintptr)rd64(&p); glDispatchComputeIndirect(indirect);  break; }
    case 248: { GLenum mode = (GLenum)rd32(&p); const void * indirect = (const void *)(uintptr_t)rd64(&p); glDrawArraysIndirect(mode, indirect);  break; }
    case 249: { GLenum mode = (GLenum)rd32(&p); GLenum type = (GLenum)rd32(&p); const void * indirect = (const void *)(uintptr_t)rd64(&p); glDrawElementsIndirect(mode, type, indirect);  break; }
    case 250: { GLenum target = (GLenum)rd32(&p); GLenum pname = (GLenum)rd32(&p); GLint param = (GLint)rd32(&p); glFramebufferParameteri(target, pname, param);  break; }
    case 251: { GLenum target = (GLenum)rd32(&p); GLenum pname = (GLenum)rd32(&p); uint32_t params_n = (uint32_t)((4) > 0 ? (4) : 0); GLint * params = (GLint *)scratch(params_n); glGetFramebufferParameteriv(target, pname, params); reply(params, params_n); break; }
    case 252: { GLuint program = (GLuint)rd32(&p); GLenum programInterface = (GLenum)rd32(&p); GLenum pname = (GLenum)rd32(&p); uint32_t params_n = (uint32_t)((4) > 0 ? (4) : 0); GLint * params = (GLint *)scratch(params_n); glGetProgramInterfaceiv(program, programInterface, pname, params); reply(params, params_n); break; }
    case 253: { GLuint program = (GLuint)rd32(&p); GLenum programInterface = (GLenum)rd32(&p); uint32_t name_n; const GLchar * name = (const GLchar *)rdblob(&p, &name_n); GLuint r = glGetProgramResourceIndex(program, programInterface, name);  reply32((uint32_t)r); break; }
    case 254: { GLuint program = (GLuint)rd32(&p); GLenum programInterface = (GLenum)rd32(&p); GLuint index = (GLuint)rd32(&p); GLsizei bufSize = (GLsizei)rd32(&p); uint32_t length_n = (uint32_t)((4) > 0 ? (4) : 0); GLsizei * length = (GLsizei *)scratch(length_n); uint32_t name_n = (uint32_t)((bufSize) > 0 ? (bufSize) : 0); GLchar * name = (GLchar *)scratch(name_n); glGetProgramResourceName(program, programInterface, index, bufSize, length, name); reply(length, length_n); reply(name, name_n); break; }
    case 255: { GLuint program = (GLuint)rd32(&p); GLenum programInterface = (GLenum)rd32(&p); GLuint index = (GLuint)rd32(&p); GLsizei propCount = (GLsizei)rd32(&p); uint32_t props_n; const GLenum * props = (const GLenum *)rdblob(&p, &props_n); GLsizei bufSize = (GLsizei)rd32(&p); uint32_t length_n = (uint32_t)((4) > 0 ? (4) : 0); GLsizei * length = (GLsizei *)scratch(length_n); uint32_t params_n = (uint32_t)((bufSize * 4) > 0 ? (bufSize * 4) : 0); GLint * params = (GLint *)scratch(params_n); glGetProgramResourceiv(program, programInterface, index, propCount, props, bufSize, length, params); reply(length, length_n); reply(params, params_n); break; }
    case 256: { GLuint program = (GLuint)rd32(&p); GLenum programInterface = (GLenum)rd32(&p); uint32_t name_n; const GLchar * name = (const GLchar *)rdblob(&p, &name_n); GLint r = glGetProgramResourceLocation(program, programInterface, name);  reply32((uint32_t)r); break; }
    case 257: { GLuint pipeline = (GLuint)rd32(&p); GLbitfield stages = (GLbitfield)rd32(&p); GLuint program = (GLuint)rd32(&p); glUseProgramStages(pipeline, stages, program);  break; }
    case 258: { GLuint pipeline = (GLuint)rd32(&p); GLuint program = (GLuint)rd32(&p); glActiveShaderProgram(pipeline, program);  break; }
    case 259: hd_glCreateShaderProgramv(p, len); break;
    case 260: { GLuint pipeline = (GLuint)rd32(&p); glBindProgramPipeline(pipeline);  break; }
    case 261: { GLsizei n = (GLsizei)rd32(&p); uint32_t pipelines_n; const GLuint * pipelines = (const GLuint *)rdblob(&p, &pipelines_n); glDeleteProgramPipelines(n, pipelines);  break; }
    case 262: { GLsizei n = (GLsizei)rd32(&p); uint32_t pipelines_n = (uint32_t)((n * 4) > 0 ? (n * 4) : 0); GLuint * pipelines = (GLuint *)scratch(pipelines_n); glGenProgramPipelines(n, pipelines); reply(pipelines, pipelines_n); break; }
    case 263: { GLuint pipeline = (GLuint)rd32(&p); GLboolean r = glIsProgramPipeline(pipeline);  reply32((uint32_t)r); break; }
    case 264: { GLuint pipeline = (GLuint)rd32(&p); GLenum pname = (GLenum)rd32(&p); uint32_t params_n = (uint32_t)((4) > 0 ? (4) : 0); GLint * params = (GLint *)scratch(params_n); glGetProgramPipelineiv(pipeline, pname, params); reply(params, params_n); break; }
    case 265: { GLuint program = (GLuint)rd32(&p); GLint location = (GLint)rd32(&p); GLint v0 = (GLint)rd32(&p); glProgramUniform1i(program, location, v0);  break; }
    case 266: { GLuint program = (GLuint)rd32(&p); GLint location = (GLint)rd32(&p); GLint v0 = (GLint)rd32(&p); GLint v1 = (GLint)rd32(&p); glProgramUniform2i(program, location, v0, v1);  break; }
    case 267: { GLuint program = (GLuint)rd32(&p); GLint location = (GLint)rd32(&p); GLint v0 = (GLint)rd32(&p); GLint v1 = (GLint)rd32(&p); GLint v2 = (GLint)rd32(&p); glProgramUniform3i(program, location, v0, v1, v2);  break; }
    case 268: { GLuint program = (GLuint)rd32(&p); GLint location = (GLint)rd32(&p); GLint v0 = (GLint)rd32(&p); GLint v1 = (GLint)rd32(&p); GLint v2 = (GLint)rd32(&p); GLint v3 = (GLint)rd32(&p); glProgramUniform4i(program, location, v0, v1, v2, v3);  break; }
    case 269: { GLuint program = (GLuint)rd32(&p); GLint location = (GLint)rd32(&p); GLuint v0 = (GLuint)rd32(&p); glProgramUniform1ui(program, location, v0);  break; }
    case 270: { GLuint program = (GLuint)rd32(&p); GLint location = (GLint)rd32(&p); GLuint v0 = (GLuint)rd32(&p); GLuint v1 = (GLuint)rd32(&p); glProgramUniform2ui(program, location, v0, v1);  break; }
    case 271: { GLuint program = (GLuint)rd32(&p); GLint location = (GLint)rd32(&p); GLuint v0 = (GLuint)rd32(&p); GLuint v1 = (GLuint)rd32(&p); GLuint v2 = (GLuint)rd32(&p); glProgramUniform3ui(program, location, v0, v1, v2);  break; }
    case 272: { GLuint program = (GLuint)rd32(&p); GLint location = (GLint)rd32(&p); GLuint v0 = (GLuint)rd32(&p); GLuint v1 = (GLuint)rd32(&p); GLuint v2 = (GLuint)rd32(&p); GLuint v3 = (GLuint)rd32(&p); glProgramUniform4ui(program, location, v0, v1, v2, v3);  break; }
    case 273: { GLuint program = (GLuint)rd32(&p); GLint location = (GLint)rd32(&p); GLfloat v0 = rdf(&p); glProgramUniform1f(program, location, v0);  break; }
    case 274: { GLuint program = (GLuint)rd32(&p); GLint location = (GLint)rd32(&p); GLfloat v0 = rdf(&p); GLfloat v1 = rdf(&p); glProgramUniform2f(program, location, v0, v1);  break; }
    case 275: { GLuint program = (GLuint)rd32(&p); GLint location = (GLint)rd32(&p); GLfloat v0 = rdf(&p); GLfloat v1 = rdf(&p); GLfloat v2 = rdf(&p); glProgramUniform3f(program, location, v0, v1, v2);  break; }
    case 276: { GLuint program = (GLuint)rd32(&p); GLint location = (GLint)rd32(&p); GLfloat v0 = rdf(&p); GLfloat v1 = rdf(&p); GLfloat v2 = rdf(&p); GLfloat v3 = rdf(&p); glProgramUniform4f(program, location, v0, v1, v2, v3);  break; }
    case 277: { GLuint program = (GLuint)rd32(&p); GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); uint32_t value_n; const GLint * value = (const GLint *)rdblob(&p, &value_n); glProgramUniform1iv(program, location, count, value);  break; }
    case 278: { GLuint program = (GLuint)rd32(&p); GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); uint32_t value_n; const GLint * value = (const GLint *)rdblob(&p, &value_n); glProgramUniform2iv(program, location, count, value);  break; }
    case 279: { GLuint program = (GLuint)rd32(&p); GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); uint32_t value_n; const GLint * value = (const GLint *)rdblob(&p, &value_n); glProgramUniform3iv(program, location, count, value);  break; }
    case 280: { GLuint program = (GLuint)rd32(&p); GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); uint32_t value_n; const GLint * value = (const GLint *)rdblob(&p, &value_n); glProgramUniform4iv(program, location, count, value);  break; }
    case 281: { GLuint program = (GLuint)rd32(&p); GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); uint32_t value_n; const GLuint * value = (const GLuint *)rdblob(&p, &value_n); glProgramUniform1uiv(program, location, count, value);  break; }
    case 282: { GLuint program = (GLuint)rd32(&p); GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); uint32_t value_n; const GLuint * value = (const GLuint *)rdblob(&p, &value_n); glProgramUniform2uiv(program, location, count, value);  break; }
    case 283: { GLuint program = (GLuint)rd32(&p); GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); uint32_t value_n; const GLuint * value = (const GLuint *)rdblob(&p, &value_n); glProgramUniform3uiv(program, location, count, value);  break; }
    case 284: { GLuint program = (GLuint)rd32(&p); GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); uint32_t value_n; const GLuint * value = (const GLuint *)rdblob(&p, &value_n); glProgramUniform4uiv(program, location, count, value);  break; }
    case 285: { GLuint program = (GLuint)rd32(&p); GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); uint32_t value_n; const GLfloat * value = (const GLfloat *)rdblob(&p, &value_n); glProgramUniform1fv(program, location, count, value);  break; }
    case 286: { GLuint program = (GLuint)rd32(&p); GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); uint32_t value_n; const GLfloat * value = (const GLfloat *)rdblob(&p, &value_n); glProgramUniform2fv(program, location, count, value);  break; }
    case 287: { GLuint program = (GLuint)rd32(&p); GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); uint32_t value_n; const GLfloat * value = (const GLfloat *)rdblob(&p, &value_n); glProgramUniform3fv(program, location, count, value);  break; }
    case 288: { GLuint program = (GLuint)rd32(&p); GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); uint32_t value_n; const GLfloat * value = (const GLfloat *)rdblob(&p, &value_n); glProgramUniform4fv(program, location, count, value);  break; }
    case 289: { GLuint program = (GLuint)rd32(&p); GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); GLboolean transpose = (GLboolean)rd32(&p); uint32_t value_n; const GLfloat * value = (const GLfloat *)rdblob(&p, &value_n); glProgramUniformMatrix2fv(program, location, count, transpose, value);  break; }
    case 290: { GLuint program = (GLuint)rd32(&p); GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); GLboolean transpose = (GLboolean)rd32(&p); uint32_t value_n; const GLfloat * value = (const GLfloat *)rdblob(&p, &value_n); glProgramUniformMatrix3fv(program, location, count, transpose, value);  break; }
    case 291: { GLuint program = (GLuint)rd32(&p); GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); GLboolean transpose = (GLboolean)rd32(&p); uint32_t value_n; const GLfloat * value = (const GLfloat *)rdblob(&p, &value_n); glProgramUniformMatrix4fv(program, location, count, transpose, value);  break; }
    case 292: { GLuint program = (GLuint)rd32(&p); GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); GLboolean transpose = (GLboolean)rd32(&p); uint32_t value_n; const GLfloat * value = (const GLfloat *)rdblob(&p, &value_n); glProgramUniformMatrix2x3fv(program, location, count, transpose, value);  break; }
    case 293: { GLuint program = (GLuint)rd32(&p); GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); GLboolean transpose = (GLboolean)rd32(&p); uint32_t value_n; const GLfloat * value = (const GLfloat *)rdblob(&p, &value_n); glProgramUniformMatrix3x2fv(program, location, count, transpose, value);  break; }
    case 294: { GLuint program = (GLuint)rd32(&p); GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); GLboolean transpose = (GLboolean)rd32(&p); uint32_t value_n; const GLfloat * value = (const GLfloat *)rdblob(&p, &value_n); glProgramUniformMatrix2x4fv(program, location, count, transpose, value);  break; }
    case 295: { GLuint program = (GLuint)rd32(&p); GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); GLboolean transpose = (GLboolean)rd32(&p); uint32_t value_n; const GLfloat * value = (const GLfloat *)rdblob(&p, &value_n); glProgramUniformMatrix4x2fv(program, location, count, transpose, value);  break; }
    case 296: { GLuint program = (GLuint)rd32(&p); GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); GLboolean transpose = (GLboolean)rd32(&p); uint32_t value_n; const GLfloat * value = (const GLfloat *)rdblob(&p, &value_n); glProgramUniformMatrix3x4fv(program, location, count, transpose, value);  break; }
    case 297: { GLuint program = (GLuint)rd32(&p); GLint location = (GLint)rd32(&p); GLsizei count = (GLsizei)rd32(&p); GLboolean transpose = (GLboolean)rd32(&p); uint32_t value_n; const GLfloat * value = (const GLfloat *)rdblob(&p, &value_n); glProgramUniformMatrix4x3fv(program, location, count, transpose, value);  break; }
    case 298: { GLuint pipeline = (GLuint)rd32(&p); glValidateProgramPipeline(pipeline);  break; }
    case 299: { GLuint pipeline = (GLuint)rd32(&p); GLsizei bufSize = (GLsizei)rd32(&p); uint32_t length_n = (uint32_t)((4) > 0 ? (4) : 0); GLsizei * length = (GLsizei *)scratch(length_n); uint32_t infoLog_n = (uint32_t)((bufSize) > 0 ? (bufSize) : 0); GLchar * infoLog = (GLchar *)scratch(infoLog_n); glGetProgramPipelineInfoLog(pipeline, bufSize, length, infoLog); reply(length, length_n); reply(infoLog, infoLog_n); break; }
    case 300: { GLuint unit = (GLuint)rd32(&p); GLuint texture = (GLuint)rd32(&p); GLint level = (GLint)rd32(&p); GLboolean layered = (GLboolean)rd32(&p); GLint layer = (GLint)rd32(&p); GLenum access = (GLenum)rd32(&p); GLenum format = (GLenum)rd32(&p); glBindImageTexture(unit, texture, level, layered, layer, access, format);  break; }
    case 301: { GLenum target = (GLenum)rd32(&p); GLuint index = (GLuint)rd32(&p); uint32_t data_n = (uint32_t)((target == 0x0C23 ? 4 : 1) > 0 ? (target == 0x0C23 ? 4 : 1) : 0); GLboolean * data = (GLboolean *)scratch(data_n); glGetBooleani_v(target, index, data); reply(data, data_n); break; }
    case 302: { GLbitfield barriers = (GLbitfield)rd32(&p); glMemoryBarrier(barriers);  break; }
    case 303: { GLbitfield barriers = (GLbitfield)rd32(&p); glMemoryBarrierByRegion(barriers);  break; }
    case 304: { GLenum target = (GLenum)rd32(&p); GLsizei samples = (GLsizei)rd32(&p); GLenum internalformat = (GLenum)rd32(&p); GLsizei width = (GLsizei)rd32(&p); GLsizei height = (GLsizei)rd32(&p); GLboolean fixedsamplelocations = (GLboolean)rd32(&p); glTexStorage2DMultisample(target, samples, internalformat, width, height, fixedsamplelocations);  break; }
    case 305: { GLenum pname = (GLenum)rd32(&p); GLuint index = (GLuint)rd32(&p); uint32_t val_n = (uint32_t)((8) > 0 ? (8) : 0); GLfloat * val = (GLfloat *)scratch(val_n); glGetMultisamplefv(pname, index, val); reply(val, val_n); break; }
    case 306: { GLuint maskNumber = (GLuint)rd32(&p); GLbitfield mask = (GLbitfield)rd32(&p); glSampleMaski(maskNumber, mask);  break; }
    case 307: { GLenum target = (GLenum)rd32(&p); GLint level = (GLint)rd32(&p); GLenum pname = (GLenum)rd32(&p); uint32_t params_n = (uint32_t)((4) > 0 ? (4) : 0); GLint * params = (GLint *)scratch(params_n); glGetTexLevelParameteriv(target, level, pname, params); reply(params, params_n); break; }
    case 308: { GLenum target = (GLenum)rd32(&p); GLint level = (GLint)rd32(&p); GLenum pname = (GLenum)rd32(&p); uint32_t params_n = (uint32_t)((4) > 0 ? (4) : 0); GLfloat * params = (GLfloat *)scratch(params_n); glGetTexLevelParameterfv(target, level, pname, params); reply(params, params_n); break; }
    case 309: { GLuint bindingindex = (GLuint)rd32(&p); GLuint buffer = (GLuint)rd32(&p); GLintptr offset = (GLintptr)rd64(&p); GLsizei stride = (GLsizei)rd32(&p); glBindVertexBuffer(bindingindex, buffer, offset, stride);  break; }
    case 310: { GLuint attribindex = (GLuint)rd32(&p); GLint size = (GLint)rd32(&p); GLenum type = (GLenum)rd32(&p); GLboolean normalized = (GLboolean)rd32(&p); GLuint relativeoffset = (GLuint)rd32(&p); glVertexAttribFormat(attribindex, size, type, normalized, relativeoffset);  break; }
    case 311: { GLuint attribindex = (GLuint)rd32(&p); GLint size = (GLint)rd32(&p); GLenum type = (GLenum)rd32(&p); GLuint relativeoffset = (GLuint)rd32(&p); glVertexAttribIFormat(attribindex, size, type, relativeoffset);  break; }
    case 312: { GLuint attribindex = (GLuint)rd32(&p); GLuint bindingindex = (GLuint)rd32(&p); glVertexAttribBinding(attribindex, bindingindex);  break; }
    case 313: { GLuint bindingindex = (GLuint)rd32(&p); GLuint divisor = (GLuint)rd32(&p); glVertexBindingDivisor(bindingindex, divisor);  break; }
    default: hd_other(op, p, len); break;
    }
}
