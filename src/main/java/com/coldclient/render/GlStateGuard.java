package com.coldclient.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

/**
 * Saves/restores the GL state NanoVG changes so Minecraft's own
 * rendering isn't left corrupted after the ClickGUI draws
 * (inventory, pause menu, tooltips, hotbar, mod menu icons, etc.).
 */
public final class GlStateGuard {
    private GlStateGuard() {}

    private static boolean saved;

    private static final int UBO_SLOTS = 8;
    private static final int TEX_UNITS = 8;

    private static final int[] uboBuffer = new int[UBO_SLOTS];
    private static int uniformBuffer;

    private static final int[] unitTexture2d = new int[TEX_UNITS];
    private static final int[] unitSampler = new int[TEX_UNITS];

    private static int program, vao, arrayBuffer, elementBuffer, activeTexture;
    private static int drawFbo, readFbo;
    private static int unpackAlign, unpackRowLength, unpackSkipPixels, unpackSkipRows, unpackBuffer;
    private static int packAlign, packBuffer;

    private static int blendSrcRgb, blendDstRgb, blendSrcA, blendDstA, blendEqRgb, blendEqA;
    private static int cullMode, frontFace, depthFunc, stencilMask;
    private static int stencilFunc, stencilRef, stencilValueMask;
    private static int stencilFail, stencilZFail, stencilZPass;
    private static int polygonMode;

    private static boolean blend, cull, depthTest, stencilTest, scissor;
    private static boolean depthMask;
    private static boolean colorMaskR, colorMaskG, colorMaskB, colorMaskA;
    private static final int[] viewport = new int[4];
    private static final int[] scissorBox = new int[4];

    public static void save() {
        program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        arrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        elementBuffer = GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING);
        activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);

        drawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        readFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);

        unpackBuffer = GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
        unpackAlign = GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
        unpackRowLength = GL11.glGetInteger(GL11.GL_UNPACK_ROW_LENGTH);
        unpackSkipPixels = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_PIXELS);
        unpackSkipRows = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_ROWS);

        packBuffer = GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING);
        packAlign = GL11.glGetInteger(GL11.GL_PACK_ALIGNMENT);

        uniformBuffer = GL11.glGetInteger(GL31.GL_UNIFORM_BUFFER_BINDING);
        for (int i = 0; i < UBO_SLOTS; i++) {
            // Indexed binding query — no 64-bit range APIs (not in this LWJGL binding)
            uboBuffer[i] = GL30.glGetIntegeri(GL31.GL_UNIFORM_BUFFER_BINDING, i);
        }

        for (int u = 0; u < TEX_UNITS; u++) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + u);
            unitTexture2d[u] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            unitSampler[u] = GL11.glGetInteger(GL33.GL_SAMPLER_BINDING);
        }
        GL13.glActiveTexture(activeTexture);

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

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer vp = stack.mallocInt(4);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, vp);
            vp.get(viewport);
            IntBuffer sc = stack.mallocInt(4);
            GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, sc);
            sc.get(scissorBox);
            IntBuffer pm = stack.mallocInt(2);
            GL11.glGetIntegerv(GL11.GL_POLYGON_MODE, pm);
            polygonMode = pm.get(0);
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            var bb = stack.malloc(4);
            GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, bb);
            colorMaskR = bb.get(0) != 0;
            colorMaskG = bb.get(1) != 0;
            colorMaskB = bb.get(2) != 0;
            colorMaskA = bb.get(3) != 0;
        }

        saved = true;
    }

    /** Clean state NanoVG expects before beginFrame. */
    public static void prepareForNanoVG() {
        // Leftover Minecraft sampler on unit 0 → purple/missing textures & invisible text
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL33.glBindSampler(0, 0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        for (int u = 1; u < TEX_UNITS; u++) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + u);
            GL33.glBindSampler(u, 0);
        }
        GL13.glActiveTexture(GL13.GL_TEXTURE0);

        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);

        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA
        );
        GL11.glColorMask(true, true, true, true);
        GL11.glDepthMask(false);
        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
    }

    public static void restore() {
        if (!saved) {
            return;
        }
        saved = false;

        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFbo);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFbo);

        GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        GL11.glScissor(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3]);

        GL20.glUseProgram(program);
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, arrayBuffer);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, elementBuffer);

        for (int u = 0; u < TEX_UNITS; u++) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + u);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, unitTexture2d[u]);
            GL33.glBindSampler(u, unitSampler[u]);
        }
        GL13.glActiveTexture(activeTexture);

        for (int i = 0; i < UBO_SLOTS; i++) {
            GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, i, uboBuffer[i]);
        }
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, uniformBuffer);

        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, unpackBuffer);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, unpackAlign);
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, unpackRowLength);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, unpackSkipPixels);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, unpackSkipRows);

        GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, packBuffer);
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, packAlign);

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

        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, polygonMode);

        GL11.glColorMask(colorMaskR, colorMaskG, colorMaskB, colorMaskA);
    }

    private static void set(int cap, boolean on) {
        if (on) {
            GL11.glEnable(cap);
        } else {
            GL11.glDisable(cap);
        }
    }
}