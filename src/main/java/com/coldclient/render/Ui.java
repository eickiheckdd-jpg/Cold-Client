package com.coldclient.render;

import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.system.MemoryStack.stackPush;

/**
 * Small NanoVG drawing toolkit used by the ClickGUI.
 * Everything here must be called between NanoVGRenderer.beginFrame/endFrame.
 */
public final class Ui {
    private Ui() {}

    public static final int ICON_COMBAT = 0;
    public static final int ICON_MOVEMENT = 1;
    public static final int ICON_VISUALS = 2;
    public static final int ICON_PLAYER = 3;
    public static final int ICON_CLIENT = 4;
    public static final int ICON_SEARCH = 5;

    public static final int ALIGN_LEFT_MIDDLE = NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE;
    public static final int ALIGN_CENTER_MIDDLE = NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE;
    public static final int ALIGN_RIGHT_MIDDLE = NVG_ALIGN_RIGHT | NVG_ALIGN_MIDDLE;

    private static final String FONT_REGULAR = "/assets/coldclient/fonts/Outfit-Regular.ttf";
    private static final String FONT_BOLD = "/assets/coldclient/fonts/Outfit-Bold.ttf";

    private static int fontRegular = -1;
    private static int fontBold = -1;
    private static boolean fontsTried;

    // NanoVG keeps pointers into these buffers, so they must never be freed.
    private static ByteBuffer regularData;
    private static ByteBuffer boldData;

    private static long ctx() {
        return NanoVGRenderer.getContext();
    }

    // -------------------------------------------------------------------------
    // Fonts
    // -------------------------------------------------------------------------

    public static void ensureFonts() {
        if (fontsTried) {
            return;
        }
        fontsTried = true;

        long c = ctx();
        regularData = loadResource(FONT_REGULAR);
        boldData = loadResource(FONT_BOLD);

        if (regularData != null) {
            fontRegular = nvgCreateFontMem(c, "cold-regular", regularData, false);
        }
        if (boldData != null) {
            fontBold = nvgCreateFontMem(c, "cold-bold", boldData, false);
        }
        if (fontBold < 0) fontBold = fontRegular;
        if (fontRegular < 0) fontRegular = fontBold;

        if (fontRegular < 0) {
            System.err.println("[Cold Client] Could not load UI fonts from " + FONT_REGULAR);
        }
    }

    public static boolean hasFont() {
        return fontRegular >= 0;
    }

    private static ByteBuffer loadResource(String path) {
        try (InputStream in = Ui.class.getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            byte[] bytes = in.readAllBytes();
            ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes);
            buffer.flip();
            return buffer;
        } catch (IOException exception) {
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Colors
    // -------------------------------------------------------------------------

    private static float clamp01(float v) {
        return v < 0f ? 0f : Math.min(v, 1f);
    }

    private static void rgba(int rgb, float a, NVGColor out) {
        nvgRGBAf(
                ((rgb >> 16) & 0xFF) / 255f,
                ((rgb >> 8) & 0xFF) / 255f,
                (rgb & 0xFF) / 255f,
                clamp01(a),
                out
        );
    }

    public static int mix(int a, int b, float t) {
        t = clamp01(t);
        int r = Math.round(((a >> 16) & 0xFF) * (1f - t) + ((b >> 16) & 0xFF) * t);
        int g = Math.round(((a >> 8) & 0xFF) * (1f - t) + ((b >> 8) & 0xFF) * t);
        int bl = Math.round((a & 0xFF) * (1f - t) + (b & 0xFF) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static void setFill(int rgb, float a) {
        try (MemoryStack stack = stackPush()) {
            NVGColor color = NVGColor.malloc(stack);
            rgba(rgb, a, color);
            nvgFillColor(ctx(), color);
        }
    }

    private static void setStroke(int rgb, float a) {
        try (MemoryStack stack = stackPush()) {
            NVGColor color = NVGColor.malloc(stack);
            rgba(rgb, a, color);
            nvgStrokeColor(ctx(), color);
        }
    }

    // -------------------------------------------------------------------------
    // Shapes
    // -------------------------------------------------------------------------

    public static void rect(float x, float y, float w, float h, float radius, int rgb, float alpha) {
        long c = ctx();
        nvgBeginPath(c);
        nvgRoundedRect(c, x, y, w, h, radius);
        setFill(rgb, alpha);
        nvgFill(c);
    }

    public static void circle(float cx, float cy, float radius, int rgb, float alpha) {
        long c = ctx();
        nvgBeginPath(c);
        nvgCircle(c, cx, cy, radius);
        setFill(rgb, alpha);
        nvgFill(c);
    }

    /** Stroke drawn fully inside the given rectangle. */
    public static void outline(float x, float y, float w, float h, float radius,
                               float strokeWidth, int rgb, float alpha) {
        long c = ctx();
        float half = strokeWidth * 0.5f;
        nvgBeginPath(c);
        nvgRoundedRect(c, x + half, y + half, w - strokeWidth, h - strokeWidth, Math.max(0f, radius - half));
        nvgStrokeWidth(c, strokeWidth);
        setStroke(rgb, alpha);
        nvgStroke(c);
    }

    public static void gradient(float x, float y, float w, float h, float radius,
                                int rgb1, float a1, int rgb2, float a2, boolean vertical) {
        long c = ctx();
        try (MemoryStack stack = stackPush()) {
            NVGColor c1 = NVGColor.malloc(stack);
            NVGColor c2 = NVGColor.malloc(stack);
            rgba(rgb1, a1, c1);
            rgba(rgb2, a2, c2);

            NVGPaint paint = NVGPaint.malloc(stack);
            if (vertical) {
                nvgLinearGradient(c, x, y, x, y + h, c1, c2, paint);
            } else {
                nvgLinearGradient(c, x, y, x + w, y, c1, c2, paint);
            }

            nvgBeginPath(c);
            nvgRoundedRect(c, x, y, w, h, radius);
            nvgFillPaint(c, paint);
            nvgFill(c);
        }
    }

    /** Soft shadow / glow around a rounded rect. The area under the rect itself is left empty. */
    public static void shadow(float x, float y, float w, float h, float radius,
                              float blur, float offsetY, int rgb, float alpha) {
        long c = ctx();
        try (MemoryStack stack = stackPush()) {
            NVGColor inner = NVGColor.malloc(stack);
            NVGColor outer = NVGColor.malloc(stack);
            rgba(rgb, alpha, inner);
            rgba(rgb, 0f, outer);

            NVGPaint paint = NVGPaint.malloc(stack);
            nvgBoxGradient(c, x, y + offsetY, w, h, radius, blur, inner, outer, paint);

            float e = blur * 1.5f;
            nvgBeginPath(c);
            nvgRect(c, x - e, y - e + offsetY, w + e * 2f, h + e * 2f);
            nvgRoundedRect(c, x, y, w, h, radius);
            nvgPathWinding(c, NVG_HOLE);
            nvgFillPaint(c, paint);
            nvgFill(c);
        }
    }

    /** iOS-style toggle. t = 0 (off) .. 1 (on). */
    public static void toggle(float x, float y, float w, float h, float t, int onRgb) {
        rect(x, y, w, h, h * 0.5f, mix(0x3A3F52, onRgb, t), 1f);

        float pad = h * 0.15f;
        float knobR = (h - pad * 2f) * 0.5f;
        float knobX = x + pad + knobR + t * (w - pad * 2f - knobR * 2f);
        circle(knobX, y + h * 0.5f, knobR, 0xFFFFFF, 0.96f);
    }

    // -------------------------------------------------------------------------
    // Text
    // -------------------------------------------------------------------------

    public static void text(String s, float x, float y, float size, boolean bold,
                            int rgb, float alpha, int align) {
        text(s, x, y, size, bold, rgb, alpha, align, 0f);
    }

    public static void text(String s, float x, float y, float size, boolean bold,
                            int rgb, float alpha, int align, float letterSpacing) {
        if (s == null || s.isEmpty() || !hasFont()) {
            return;
        }
        long c = ctx();
        nvgFontFaceId(c, bold ? fontBold : fontRegular);
        nvgFontSize(c, size);
        nvgTextAlign(c, align);
        nvgTextLetterSpacing(c, letterSpacing);
        setFill(rgb, alpha);
        nvgText(c, x, y, s);
        nvgTextLetterSpacing(c, 0f);
    }

    public static float textWidth(String s, float size, boolean bold) {
        if (s == null || s.isEmpty() || !hasFont()) {
            return 0f;
        }
        long c = ctx();
        nvgFontFaceId(c, bold ? fontBold : fontRegular);
        nvgFontSize(c, size);
        nvgTextLetterSpacing(c, 0f);
        return nvgTextBounds(c, 0f, 0f, s, (FloatBuffer) null);
    }

    public static String fit(String s, float size, boolean bold, float maxWidth) {
        if (textWidth(s, size, bold) <= maxWidth) {
            return s;
        }
        for (int n = s.length() - 1; n > 0; n--) {
            String t = s.substring(0, n).stripTrailing() + "...";
            if (textWidth(t, size, bold) <= maxWidth) {
                return t;
            }
        }
        return "...";
    }

    // -------------------------------------------------------------------------
    // Vector icons (stroke only, centered on cx/cy, `size` = full width)
    // -------------------------------------------------------------------------

    public static void icon(int type, float cx, float cy, float size, int rgb, float alpha) {
        long c = ctx();
        float r = size * 0.5f;

        setStroke(rgb, alpha);
        nvgStrokeWidth(c, Math.max(0.5f, size * 0.11f));
        nvgLineCap(c, NVG_ROUND);
        nvgLineJoin(c, NVG_ROUND);
        nvgBeginPath(c);

        switch (type) {
            case ICON_COMBAT -> {
                // Crosshair.
                nvgCircle(c, cx, cy, r * 0.58f);
                nvgMoveTo(c, cx, cy - r);
                nvgLineTo(c, cx, cy - r * 0.58f);
                nvgMoveTo(c, cx, cy + r);
                nvgLineTo(c, cx, cy + r * 0.58f);
                nvgMoveTo(c, cx - r, cy);
                nvgLineTo(c, cx - r * 0.58f, cy);
                nvgMoveTo(c, cx + r, cy);
                nvgLineTo(c, cx + r * 0.58f, cy);
            }
            case ICON_MOVEMENT -> {
                // Lightning bolt.
                nvgMoveTo(c, cx + r * 0.25f, cy - r);
                nvgLineTo(c, cx - r * 0.55f, cy + r * 0.10f);
                nvgLineTo(c, cx - r * 0.02f, cy + r * 0.10f);
                nvgLineTo(c, cx - r * 0.25f, cy + r);
                nvgLineTo(c, cx + r * 0.55f, cy - r * 0.15f);
                nvgLineTo(c, cx + r * 0.02f, cy - r * 0.15f);
                nvgClosePath(c);
            }
            case ICON_VISUALS -> {
                // Eye.
                nvgMoveTo(c, cx - r, cy);
                nvgBezierTo(c, cx - r * 0.5f, cy - r * 0.8f, cx + r * 0.5f, cy - r * 0.8f, cx + r, cy);
                nvgBezierTo(c, cx + r * 0.5f, cy + r * 0.8f, cx - r * 0.5f, cy + r * 0.8f, cx - r, cy);
                nvgCircle(c, cx, cy, r * 0.28f);
            }
            case ICON_PLAYER -> {
                // Head + shoulders.
                nvgCircle(c, cx, cy - r * 0.38f, r * 0.36f);
                nvgMoveTo(c, cx - r * 0.85f, cy + r * 0.95f);
                nvgBezierTo(c, cx - r * 0.85f, cy + r * 0.30f, cx + r * 0.85f, cy + r * 0.30f, cx + r * 0.85f, cy + r * 0.95f);
            }
            case ICON_CLIENT -> {
                // Gear.
                nvgCircle(c, cx, cy, r * 0.56f);
                nvgCircle(c, cx, cy, r * 0.22f);
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * 0.25 * i;
                    float dx = (float) Math.cos(a);
                    float dy = (float) Math.sin(a);
                    nvgMoveTo(c, cx + dx * r * 0.56f, cy + dy * r * 0.56f);
                    nvgLineTo(c, cx + dx * r * 0.95f, cy + dy * r * 0.95f);
                }
            }
            case ICON_SEARCH -> {
                nvgCircle(c, cx - r * 0.12f, cy - r * 0.12f, r * 0.58f);
                nvgMoveTo(c, cx + r * 0.32f, cy + r * 0.32f);
                nvgLineTo(c, cx + r * 0.92f, cy + r * 0.92f);
            }
            default -> {
            }
        }

        nvgStroke(c);
    }
}
