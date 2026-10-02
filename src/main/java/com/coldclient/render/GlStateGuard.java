package com.coldclient.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Saves/restores the GL state NanoVG changes so Minecraft's world
 * rendering isn't left in a corrupted state after the ClickGUI draws.
 */
public final class GlStateGuard {
    private GlStateGuard() {}

    private static boolean saved;

    private static int program, vao, arrayBuffer, elementBuffer, activeTexture, texture2d;
    private static int fbo, unpackAlign, unpackRowLength, unpackSkipPixels, unpackSkipRows;
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
        texture2d = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        fbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);

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

        java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocateDirect(4);
        GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, buf);
        for (int i = 0; i < 4; i++) colorMask[i] = buf.get(i) != 0;

        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissorBox);

        saved = true;
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
        GL13.glActiveTexture(activeTexture);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture2d);

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
