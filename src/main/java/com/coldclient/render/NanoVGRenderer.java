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
 * <p>LWJGL/NanoVG version target: 3.3.3.</p>
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

    /**
     * Initializes NanoVG.
     *
     * <p>This must be called from the Minecraft render thread after an
     * OpenGL context has been created.</p>
     *
     * @return true when initialization succeeded
     */
    public static boolean initialize() {
        if (initialized) {
            return true;
        }

        /*
         * NanoVGGL3 requires an active OpenGL context.
         * Minecraft/LWJGL is responsible for creating the capabilities.
         */
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

    /**
     * Returns whether NanoVG has been initialized successfully.
     */
    public static boolean isInitialized() {
        return initialized && context != 0L;
    }

    /**
     * Returns whether a NanoVG frame is currently active.
     */
    public static boolean isFrameActive() {
        return frameActive;
    }

    /**
     * Starts a NanoVG frame.
     *
     * @param width logical/window width
     * @param height logical/window height
     * @param devicePixelRatio framebuffer pixel ratio
     */
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

    /**
     * Ends the current NanoVG frame.
     */
    public static void endFrame() {
        if (!frameActive) {
            return;
        }

        NanoVG.nvgEndFrame(context);
        frameActive = false;
    }

    /**
     * Cancels the current frame without presenting its NanoVG draw commands.
     */
    public static void cancelFrame() {
        if (!frameActive) {
            return;
        }

        NanoVG.nvgCancelFrame(context);
        frameActive = false;
    }

    /**
     * Releases the NanoVG context.
     *
     * <p>Call this when the OpenGL context is being destroyed or the client
     * is shutting down.</p>
     */
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

    /**
     * Gets the raw NanoVG context handle.
     */
    public static long getContext() {
        ensureInitialized();
        return context;
    }

    /**
     * Sets the logical X scaling used by helper drawing methods.
     */
    public static void setScaleX(float scale) {
        scaleX = sanitizeScale(scale);
    }

    /**
     * Sets the logical Y scaling used by helper drawing methods.
     */
    public static void setScaleY(float scale) {
        scaleY = sanitizeScale(scale);
    }

    /**
     * Sets both logical X and Y scaling values.
     */
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

    /**
     * Draws a solid rectangle.
     */
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

    /**
     * Draws a solid rounded rectangle.
     */
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

    /**
     * Draws a rounded rectangle where every corner can have its own radius.
     */
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

    /**
     * Draws a circle.
     */
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

    /**
     * Draws an ellipse.
     */
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

    /**
     * Draws a line with a rounded cap.
     */
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

    /**
     * Draws a stroked rounded rectangle.
     */
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

    /**
     * Draws a simple linear gradient rectangle.
     */
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
            NVGColor inner = NVGColor.mallocStack(stack);
            NVGColor outer = NVGColor.mallocStack(stack);

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

            /*
             * NanoVG's linear gradient constructor takes start/end positions
             * and two colors.
             */
            NVGPaint paint = NanoVG.nvgLinearGradient(
                    context,
                    startX,
                    startY,
                    endX,
                    endY,
                    inner,
                    outer,
                    NVGPaint.mallocStack(stack)
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

    /**
     * Draws a rounded rectangle with a box-shadow-like gradient.
     */
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
            NVGColor transparent = NVGColor.mallocStack(stack);
            NVGColor shadow = NVGColor.mallocStack(stack);

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
                    NVGPaint.mallocStack(stack)
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

    /**
     * Sets the active clipping/scissor rectangle.
     */
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

    /**
     * Resets NanoVG clipping.
     */
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

    /**
     * Draws text using a previously loaded NanoVG font.
     *
     * @param fontId font returned by nvgCreateFont / nvgCreateFontMem
     */
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

    /**
     * Sets the current NanoVG font by ID.
     */
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

    /**
     * Sets the current font size.
     */
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
            NVGColor color = NVGColor.mallocStack(stack);

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

            /*
             * NanoVG copies the color into its internal state, so the
             * stack-allocated color does not need to remain alive afterwards.
             */
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
