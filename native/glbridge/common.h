/* The GLES bridge: what the guest driver (guest.c) and the host renderer (host.c) share. The stream itself is
 * described in gen.py. */
#ifndef AEMU_GLBRIDGE_COMMON_H
#define AEMU_GLBRIDGE_COMMON_H
#include <stdint.h>

#define AEMU_GL_PROTOCOL 1
#define AEMU_GL_SOCKET "/dev/socket/aemu_gl"
#define NIL 0xffffffffu

/* Calls that are not GL entry points. "->" is what the host answers. */
enum {
    OP_HELLO = 0x4000,      /* u32 protocol -> u32 protocol (0: refused) */
    OP_CTX_CREATE,          /* u32 share, u32 version -> u32 context (0: failed) */
    OP_CTX_DESTROY,         /* u32 context */
    OP_MAKE_CURRENT,        /* u32 context, u32 surface key, u32 w, u32 h, u32 samples (of the surface's config) -> u32 ok */
    OP_RELEASE,             /* -> u32 ok */
    OP_SURF_SIZE,           /* u32 w, u32 h: the current surface */
    OP_SURF_DESTROY,        /* u32 surface key: of the current context */
    OP_SWAP,                /* u32 w, u32 h -> w*h*4 bytes, RGBA, bottom row first */
    OP_DRAW_CLIENT,         /* a draw with vertex arrays in the program's memory, see e_draw() */
    OP_MAP_READ,            /* u32 target, u64 offset, u32 length -> length bytes */
    OP_MAP_WRITE,           /* u32 target, u64 offset, block */
    OP_GET_UNIFORM,         /* u32 kind (0 float, 1 int, 2 uint), u32 program, u32 location -> u32 count, 16 values */
    OP_GET_STRING,          /* u32 name, u32 index (NIL: glGetString) -> u32 length, bytes padded to 4 */
    OP_IMAGE_TEX,           /* u32 target, u32 w, u32 h, block (RGBA, top row first) */
    OP_PING,                /* -> u32 */
    OP_IMAGE_BIND,          /* u32 target, u64 buffer id, u64 write count, u32 w, u32 h -> u32 1: send its pixels */
    OP_IMAGE_CREATE,        /* u32 texture, u32 level: an image of the current context's 2D texture -> u32 image (0: failed) */
    OP_IMAGE_TARGET,        /* u32 target, u32 image */
    OP_IMAGE_DESTROY,       /* u32 image */
    OP_SWAP_BUF,            /* u32 w, u32 h, u64 buffer id, u64 write count, u64 frame: the frame stays in the host's buffer */
    OP_BUF_READ,            /* u64 buffer id, u64 frame, u32 w, u32 h -> u32 1 and w*h*4 bytes (RGBA, top row first) when
                               the host's buffer is newer than the guest's memory, else u32 0. Sent by the guest's gralloc. */
    OP_IMAGE_DATA,          /* u64 buffer id, u64 write count, u32 w, u32 h, u32 stride, u32 format, block (top row first) */
    OP_PRESENT,             /* u64 buffer id, u32 w, u32 h: the composed frame, to the screen -> u32 1, or 0: not a
                               buffer the host has. Sent by the guest's hwcomposer. */
};

/* how many values glGet* gives for a name; 0: a list whose length is another name's value (each side asks for it) */
static inline uint32_t aemu_get_count0(uint32_t pname, uint32_t *count_name) {
    switch (pname) {
    case 0x0BA2: /* VIEWPORT */ case 0x0C10: /* SCISSOR_BOX */ case 0x0C22: /* COLOR_CLEAR_VALUE */
    case 0x0C23: /* COLOR_WRITEMASK */ case 0x8005: /* BLEND_COLOR */
        return 4;
    case 0x0B70: /* DEPTH_RANGE */ case 0x846D: /* ALIASED_POINT_SIZE_RANGE */ case 0x846E: /* ALIASED_LINE_WIDTH_RANGE */
    case 0x0D3A: /* MAX_VIEWPORT_DIMS */
        return 2;
    case 0x86A3: *count_name = 0x86A2; return 0;    /* COMPRESSED_TEXTURE_FORMATS */
    case 0x87FF: *count_name = 0x87FE; return 0;    /* PROGRAM_BINARY_FORMATS */
    case 0x8DF8: *count_name = 0x8DF9; return 0;    /* SHADER_BINARY_FORMATS */
    }
    return 1;
}
#endif
