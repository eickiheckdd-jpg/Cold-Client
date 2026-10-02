package com.coldclient.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL33;

import java.nio.ByteBuffer;

/**
 * Saves/restores the GL state NanoVG changes so Minecraft's own
 * rendering isn't left in a corrupted state after the ClickGUI draws.
 */
public final class GlStateGuard {
    private GlStateGuard() {}

    private static boolean saved;

    // NanoVG's GL3 backend binds its own uniform buffer at binding point 0 and
    // textures/samplers on unit 0. Minecraft's GUI pipelines use these too.
    private static final int UBO_SLOTS = 8;
    private static final int TEX_UNITS = 4;
    private static final int[] uboBuffer = new int[UBO_SLOTS];
    private static final long[] uboStart = new long[UBO_SLOTS];
    private static final long[] uboSize = new long[UBO_SLOTS];
    private static int uniformBuffer;
    private static final int[] unitTexture = new int[TEX_UNITS];
    private static final int[] unitSampler = new int[TEX_UNITS];

    private static int program, vao, arrayBuffer, elementBuffer, activeTexture;
    private static int fbo, unpackAlign, unpackRowLength, unpackSkipPixels, unpackSkipRows, unpackBuffer;
    private static int blendSrcRgb, blendDstRgb, blendSrcA, blendDstA, blendEqRgb, blendEqA;
    private static int cullMode, frontFace, depthFunc, stencilMask;
    private static int stencilFunc, stencilRef, stencilValueMask;
    private static int stencilFail, stencilZFail, stencilZPass;
    private static boolean blend, cull, depthTest, stencilTest, scissor;
    private static boolean depthMask;
    private static final boolean[] colorMask = new boolean[4];
    private static final int[] viewport = new int[4];
    private static final int[] scissorBox = new int[4];

    public static void save() {
        program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        arrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        elementBuffer = GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING);
        activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        fbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);

        unpackBuffer = GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
        unpackAlign = GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
        unpackRowLength = GL11.glGetInteger(GL11.GL_UNPACK_ROW_LENGTH);
        unpackSkipPixels = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_PIXELS);
        unpackSkipRows = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_ROWS);

        blend = GL11.glIsEnabled(GL11.GL_BLEND);
        cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        stencilTest = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);
        scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);

        blendSrcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        blendDstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        blendSrcA = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        blendDstA = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        blendEqRgb = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB);
        blendEqA = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);

        cullMode = GL11.glGetInteger(GL11.GL_CULL_FACE_MODE);
        frontFace = GL11.glGetInteger(GL11.GL_FRONT_FACE);
        depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);

        stencilMask = GL11.glGetInteger(GL11.GL_STENCIL_WRITEMASK);
        stencilFunc = GL11.glGetInteger(GL11.GL_STENCIL_FUNC);
        stencilRef = GL11.glGetInteger(GL11.GL_STENCIL_REF);
        stencilValueMask = GL11.glGetInteger(GL11.GL_STENCIL_VALUE_MASK);
        stencilFail = GL11.glGetInteger(GL11.GL_STENCIL_FAIL);
        stencilZFail = GL11.glGetInteger(GL11.GL_STENCIL_PASS_DEPTH_FAIL);
        stencilZPass = GL11.glGetInteger(GL11.GL_STENCIL_PASS_DEPTH_PASS);

        ByteBuffer buf = ByteBuffer.allocateDirect(4);
        GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, buf);
        for (int i = 0; i < 4; i++) colorMask[i] = buf.get(i) != 0;

        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissorBox);

        uniformBuffer = GL11.glGetInteger(GL31.GL_UNIFORM_BUFFER_BINDING);
        for (int i = 0; i < UBO_SLOTS; i++) {
            uboBuffer[i] = GL30.glGetIntegeri(GL31.GL_UNIFORM_BUFFER_BINDING, i);
            uboStart[i] = GL32.glGetInteger64i(GL31.GL_UNIFORM_BUFFER_START, i);
            uboSize[i] = GL32.glGetInteger64i(GL31.GL_UNIFORM_BUFFER_SIZE, i);
        }
        for (int u = 0; u < TEX_UNITS; u++) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + u);
            unitTexture[u] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            unitSampler[u] = GL11.glGetInteger(GL33.GL_SAMPLER_BINDING);
        }
        GL13.glActiveTexture(activeTexture);

        saved = true;
    }

    /**
     * Puts the GL state NanoVG silently relies on into a known-good shape. Call right after
     * {@link #save()}; {@link #restore()} puts everything back afterwards.
     *
     * <p>The important part is the sampler: Minecraft binds sampler objects on texture unit 0
     * and never unbinds them. A bound sampler object overrides the texture's own filter
     * parameters, so NanoVG's font atlas (a single level texture) gets sampled with whatever
     * filter Minecraft used last. If that was a mipmapped filter the atlas is "incomplete",
     * the sampler returns 0 and every glyph comes out fully transparent, while flat shapes
     * (which never sample a texture) still render fine.</p>
     */
    public static void prepareForNanoVG() {
        // NanoVG binds its textures on whatever unit is active and then samples unit 0.
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL33.glBindSampler(0, 0);

        // Font atlas uploads must read from client memory with default pixel-store state.
        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
    }

    public static void restore() {
        if (!saved) return;
        saved = false;

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        GL11.glScissor(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3]);

        GL20.glUseProgram(program);
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, arrayBuffer);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, elementBuffer);

        for (int u = 0; u < TEX_UNITS; u++) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + u);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, unitTexture[u]);
            GL33.glBindSampler(u, unitSampler[u]);
        }
        GL13.glActiveTexture(activeTexture);

        for (int i = 0; i < UBO_SLOTS; i++) {
            if (uboBuffer[i] != 0 && uboSize[i] > 0) {
                GL30.glBindBufferRange(GL31.GL_UNIFORM_BUFFER, i, uboBuffer[i], uboStart[i], uboSize[i]);
            } else {
                GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, i, uboBuffer[i]);
            }
        }
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, uniformBuffer);

        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, unpackBuffer);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, unpackAlign);
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, unpackRowLength);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, unpackSkipPixels);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, unpackSkipRows);

        set(GL11.GL_BLEND, blend);
        set(GL11.GL_CULL_FACE, cull);
        set(GL11.GL_DEPTH_TEST, depthTest);
        set(GL11.GL_STENCIL_TEST, stencilTest);
        set(GL11.GL_SCISSOR_TEST, scissor);

        GL14.glBlendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcA, blendDstA);
        GL20.glBlendEquationSeparate(blendEqRgb, blendEqA);

        GL11.glCullFace(cullMode);
        GL11.glFrontFace(frontFace);
        GL11.glDepthFunc(depthFunc);
        GL11.glDepthMask(depthMask);

        GL11.glStencilMask(stencilMask);
        GL11.glStencilFunc(stencilFunc, stencilRef, stencilValueMask);
        GL11.glStencilOp(stencilFail, stencilZFail, stencilZPass);

        GL11.glColorMask(colorMask[0], colorMask[1], colorMask[2], colorMask[3]);
    }

    private static void set(int cap, boolean on) {
        if (on) GL11.glEnable(cap); else GL11.glDisable(cap);
    }
}