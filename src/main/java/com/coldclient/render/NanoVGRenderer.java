package com.coldclient.render;

import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL3;
import org.lwjgl.opengl.GL;

import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVGGL3.*;
import static org.lwjgl.system.MemoryStack.stackPush;

/**
 * Central NanoVG renderer for Cold Client.
 *
 * <p>The renderer is intentionally independent from Minecraft classes.
 * Minecraft should call {@link #initialize()} once an OpenGL context is active,
 * then surround GUI drawing with {@link #beginFrame(float, float, float)} and
 * {@link #endFrame()}.</p>
 *
 * <p>Uses the NanoVG API provided by the LWJGL runtime bundled by Amethyst.</p>
 */
public final class NanoVGRenderer {

    private static final int DEFAULT_FLAGS =
            NVG_ANTIALIAS | NVG_STENCIL_STROKES;

    private static long context = 0L;

    private static boolean initialized = false;
    private static boolean frameActive = false;

    private static float scaleX = 1.0f;
    private static float scaleY = 1.0f;

    private NanoVGRenderer() {
    }

    public static boolean initialize() {
        if (initialized) {
            return true;
        }

        try {
            GL.getCapabilities();
        } catch (IllegalStateException exception) {
            System.err.println(
                    "[Cold Client] NanoVG initialization failed: "
                            + "no current OpenGL context."
            );
            return false;
        }

        long createdContext = NanoVGGL3.nvgCreate(DEFAULT_FLAGS);

        if (createdContext == 0L) {
            System.err.println(
                    "[Cold Client] NanoVG initialization failed: "
                            + "nvgCreate returned a null context."
            );
            return false;
        }

        context = createdContext;
        initialized = true;

        return true;
    }

    public static boolean isInitialized() {
        return initialized && context != 0L;
    }

    public static boolean isFrameActive() {
        return frameActive;
    }

    public static void beginFrame(
            float width,
            float height,
            float devicePixelRatio
    ) {
        ensureInitialized();

        if (frameActive) {
            throw new IllegalStateException(
                    "NanoVG frame is already active."
            );
        }

        if (width <= 0.0f || height <= 0.0f) {
            return;
        }

        float ratio = devicePixelRatio;

        if (!Float.isFinite(ratio) || ratio <= 0.0f) {
            ratio = 1.0f;
        }

        scaleX = 1.0f;
        scaleY = 1.0f;

        NanoVG.nvgBeginFrame(
                context,
                width,
                height,
                ratio
        );

        frameActive = true;
    }

    public static void endFrame() {
        if (!frameActive) {
            return;
        }

        NanoVG.nvgEndFrame(context);
        frameActive = false;
    }

    public static void cancelFrame() {
        if (!frameActive) {
            return;
        }

        NanoVG.nvgCancelFrame(context);
        frameActive = false;
    }

    public static void destroy() {
        if (context == 0L) {
            initialized = false;
            frameActive = false;
            return;
        }

        if (frameActive) {
            cancelFrame();
        }

        NanoVGGL3.nvgDelete(context);

        context = 0L;
        initialized = false;
        frameActive = false;

        scaleX = 1.0f;
        scaleY = 1.0f;
    }

    public static long getContext() {
        ensureInitialized();
        return context;
    }

    public static void setScaleX(float scale) {
        scaleX = sanitizeScale(scale);
    }

    public static void setScaleY(float scale) {
        scaleY = sanitizeScale(scale);
    }

    public static void setScale(float scale) {
        float sanitized = sanitizeScale(scale);

        scaleX = sanitized;
        scaleY = sanitized;
    }

    public static float getScaleX() {
        return scaleX;
    }

    public static float getScaleY() {
        return scaleY;
    }

    // -------------------------------------------------------------------------
    // Shapes
    // -------------------------------------------------------------------------

    public static void drawRect(
            float x,
            float y,
            float width,
            float height,
            float r,
            float g,
            float b,
            float a
    ) {
        requireFrame();

        NanoVG.nvgBeginPath(context);

        NanoVG.nvgRect(
                context,
                sx(x),
                sy(y),
                sw(width),
                sh(height)
        );

        fillColor(r, g, b, a);
        NanoVG.nvgFill(context);
    }

    public static void drawRoundedRect(
            float x,
            float y,
            float width,
            float height,
            float radius,
            float r,
            float g,
            float b,
            float a
    ) {
        requireFrame();

        NanoVG.nvgBeginPath(context);

        NanoVG.nvgRoundedRect(
                context,
                sx(x),
                sy(y),
                sw(width),
                sh(height),
                sr(radius)
        );

        fillColor(r, g, b, a);
        NanoVG.nvgFill(context);
    }

    public static void drawRoundedRectVarying(
            float x,
            float y,
            float width,
            float height,
            float topLeft,
            float topRight,
            float bottomRight,
            float bottomLeft,
            float r,
            float g,
            float b,
            float a
    ) {
        requireFrame();

        NanoVG.nvgBeginPath(context);

        NanoVG.nvgRoundedRectVarying(
                context,
                sx(x),
                sy(y),
                sw(width),
                sh(height),
                sr(topLeft),
                sr(topRight),
                sr(bottomRight),
                sr(bottomLeft)
        );

        fillColor(r, g, b, a);
        NanoVG.nvgFill(context);
    }

    public static void drawCircle(
            float centerX,
            float centerY,
            float radius,
            float r,
            float g,
            float b,
            float a
    ) {
        requireFrame();

        NanoVG.nvgBeginPath(context);

        NanoVG.nvgCircle(
                context,
                sx(centerX),
                sy(centerY),
                sr(radius)
        );

        fillColor(r, g, b, a);
        NanoVG.nvgFill(context);
    }

    public static void drawEllipse(
            float centerX,
            float centerY,
            float radiusX,
            float radiusY,
            float r,
            float g,
            float b,
            float a
    ) {
        requireFrame();

        NanoVG.nvgBeginPath(context);

        NanoVG.nvgEllipse(
                context,
                sx(centerX),
                sy(centerY),
                sw(radiusX),
                sh(radiusY)
        );

        fillColor(r, g, b, a);
        NanoVG.nvgFill(context);
    }

    public static void drawLine(
            float x1,
            float y1,
            float x2,
            float y2,
            float strokeWidth,
            float r,
            float g,
            float b,
            float a
    ) {
        requireFrame();

        NanoVG.nvgBeginPath(context);

        NanoVG.nvgMoveTo(
                context,
                sx(x1),
                sy(y1)
        );

        NanoVG.nvgLineTo(
                context,
                sx(x2),
                sy(y2)
        );

        NanoVG.nvgStrokeWidth(
                context,
                Math.max(0.1f, sr(strokeWidth))
        );

        NanoVG.nvgLineCap(
                context,
                NVG_ROUND
        );

        fillColor(r, g, b, a);
        NanoVG.nvgStroke(context);
    }

    public static void drawRoundedOutline(
            float x,
            float y,
            float width,
            float height,
            float radius,
            float strokeWidth,
            float r,
            float g,
            float b,
            float a
    ) {
        requireFrame();

        NanoVG.nvgBeginPath(context);

        NanoVG.nvgRoundedRect(
                context,
                sx(x),
                sy(y),
                sw(width),
                sh(height),
                sr(radius)
        );

        NanoVG.nvgStrokeWidth(
                context,
                Math.max(0.1f, sr(strokeWidth))
        );

        fillColor(r, g, b, a);
        NanoVG.nvgStroke(context);
    }

    // -------------------------------------------------------------------------
    // Gradients
    // -------------------------------------------------------------------------

    public static void drawLinearGradient(
            float x,
            float y,
            float width,
            float height,
            float startR,
            float startG,
            float startB,
            float startA,
            float endR,
            float endG,
            float endB,
            float endA,
            float angleRadians
    ) {
        requireFrame();

        try (var stack = stackPush()) {
            NVGColor inner = NVGColor.malloc(stack);
            NVGColor outer = NVGColor.malloc(stack);

            NanoVG.nvgRGBAf(
                    clamp01(startR),
                    clamp01(startG),
                    clamp01(startB),
                    clamp01(startA),
                    inner
            );

            NanoVG.nvgRGBAf(
                    clamp01(endR),
                    clamp01(endG),
                    clamp01(endB),
                    clamp01(endA),
                    outer
            );

            float centerX = sx(x + width * 0.5f);
            float centerY = sy(y + height * 0.5f);

            float halfWidth = Math.abs(sw(width) * 0.5f);
            float halfHeight = Math.abs(sh(height) * 0.5f);

            float dx = (float) Math.cos(angleRadians);
            float dy = (float) Math.sin(angleRadians);

            float extent = Math.max(halfWidth, halfHeight);

            float startX = centerX - dx * extent;
            float startY = centerY - dy * extent;

            float endX = centerX + dx * extent;
            float endY = centerY + dy * extent;

            NVGPaint paint = NanoVG.nvgLinearGradient(
                    context,
                    startX,
                    startY,
                    endX,
                    endY,
                    inner,
                    outer,
                    NVGPaint.malloc(stack)
            );

            NanoVG.nvgBeginPath(context);

            NanoVG.nvgRect(
                    context,
                    sx(x),
                    sy(y),
                    sw(width),
                    sh(height)
            );

            NanoVG.nvgFillPaint(context, paint);
            NanoVG.nvgFill(context);
        }
    }

    public static void drawShadowedRoundedRect(
            float x,
            float y,
            float width,
            float height,
            float radius,
            float shadowSize,
            float shadowAlpha,
            float r,
            float g,
            float b,
            float a
    ) {
        requireFrame();

        try (var stack = stackPush()) {
            NVGColor transparent = NVGColor.malloc(stack);
            NVGColor shadow = NVGColor.malloc(stack);

            NanoVG.nvgRGBAf(
                    0.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    transparent
            );

            NanoVG.nvgRGBAf(
                    0.0f,
                    0.0f,
                    0.0f,
                    clamp01(shadowAlpha),
                    shadow
            );

            float expandedX = x - shadowSize;
            float expandedY = y - shadowSize;
            float expandedWidth = width + shadowSize * 2.0f;
            float expandedHeight = height + shadowSize * 2.0f;

            NVGPaint paint = NanoVG.nvgBoxGradient(
                    context,
                    sx(expandedX),
                    sy(expandedY),
                    sw(expandedWidth),
                    sh(expandedHeight),
                    sr(radius + shadowSize),
                    sr(shadowSize),
                    shadow,
                    transparent,
                    NVGPaint.malloc(stack)
            );

            NanoVG.nvgBeginPath(context);

            NanoVG.nvgRoundedRect(
                    context,
                    sx(expandedX),
                    sy(expandedY),
                    sw(expandedWidth),
                    sh(expandedHeight),
                    sr(radius + shadowSize)
            );

            NanoVG.nvgFillPaint(context, paint);
            NanoVG.nvgFill(context);
        }

        drawRoundedRect(
                x,
                y,
                width,
                height,
                radius,
                r,
                g,
                b,
                a
        );
    }

    // -------------------------------------------------------------------------
    // Clipping
    // -------------------------------------------------------------------------

    public static void scissor(
            float x,
            float y,
            float width,
            float height
    ) {
        requireFrame();

        NanoVG.nvgScissor(
                context,
                sx(x),
                sy(y),
                sw(width),
                sh(height)
        );
    }

    public static void resetScissor() {
        requireFrame();
        NanoVG.nvgResetScissor(context);
    }

    // -------------------------------------------------------------------------
    // Transformations
    // -------------------------------------------------------------------------

    public static void save() {
        requireFrame();
        NanoVG.nvgSave(context);
    }

    public static void restore() {
        requireFrame();
        NanoVG.nvgRestore(context);
    }

    public static void resetTransform() {
        requireFrame();
        NanoVG.nvgResetTransform(context);
    }

    public static void translate(float x, float y) {
        requireFrame();

        NanoVG.nvgTranslate(
                context,
                sx(x),
                sy(y)
        );
    }

    public static void rotate(float radians) {
        requireFrame();
        NanoVG.nvgRotate(context, radians);
    }

    public static void scale(float x, float y) {
        requireFrame();

        NanoVG.nvgScale(
                context,
                x,
                y
        );
    }

    // -------------------------------------------------------------------------
    // Text
    // -------------------------------------------------------------------------

    public static void drawText(
            int fontId,
            String text,
            float x,
            float y,
            float fontSize,
            float r,
            float g,
            float b,
            float a
    ) {
        requireFrame();

        if (fontId < 0 || text == null || text.isEmpty()) {
            return;
        }

        NanoVG.nvgFontSize(
                context,
                sr(fontSize)
        );

        NanoVG.nvgFontFaceId(
                context,
                fontId
        );

        NanoVG.nvgTextAlign(
                context,
                NVG_ALIGN_LEFT | NVG_ALIGN_BASELINE
        );

        fillColor(r, g, b, a);

        NanoVG.nvgText(
                context,
                sx(x),
                sy(y),
                text
        );
    }

    public static void setFont(int fontId) {
        requireFrame();

        if (fontId < 0) {
            return;
        }

        NanoVG.nvgFontFaceId(
                context,
                fontId
        );
    }

    public static void setFontSize(float size) {
        requireFrame();

        NanoVG.nvgFontSize(
                context,
                sr(size)
        );
    }

    // -------------------------------------------------------------------------
    // Color
    // -------------------------------------------------------------------------

    private static void fillColor(
            float r,
            float g,
            float b,
            float a
    ) {
        try (var stack = stackPush()) {
            NVGColor color = NVGColor.malloc(stack);

            NanoVG.nvgRGBAf(
                    clamp01(r),
                    clamp01(g),
                    clamp01(b),
                    clamp01(a),
                    color
            );

            NanoVG.nvgFillColor(
                    context,
                    color
            );
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private static void ensureInitialized() {
        if (!isInitialized()) {
            throw new IllegalStateException(
                    "NanoVGRenderer has not been initialized."
            );
        }
    }

    private static void requireFrame() {
        ensureInitialized();

        if (!frameActive) {
            throw new IllegalStateException(
                    "NanoVG drawing attempted outside beginFrame/endFrame."
            );
        }
    }

    private static float sanitizeScale(float scale) {
        if (!Float.isFinite(scale) || scale <= 0.0f) {
            return 1.0f;
        }

        return scale;
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) {
            return 0.0f;
        }

        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static float sx(float value) {
        return value * scaleX;
    }

    private static float sy(float value) {
        return value * scaleY;
    }

    private static float sw(float value) {
        return value * scaleX;
    }

    private static float sh(float value) {
        return value * scaleY;
    }

    private static float sr(float value) {
        return value * Math.min(scaleX, scaleY);
    }
}
