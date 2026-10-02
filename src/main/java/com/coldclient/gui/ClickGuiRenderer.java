package com.coldclient.gui;

import com.coldclient.ColdClient;
import com.coldclient.render.GlStateGuard;
import com.coldclient.render.NanoVGRenderer;
import com.coldclient.render.Ui;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.nanovg.NanoVG.*;

public final class ClickGuiRenderer {
    private ClickGuiRenderer() {}

    // ---- Layout (in "base" units, scaled to fit the screen) -----------------
    private static final float BASE_W = 700.0f;
    private static final float SEARCH_H = 38.0f;
    private static final float GAP = 10.0f;
    private static final float MAIN_H = 272.0f;
    private static final float BASE_H = SEARCH_H + GAP + MAIN_H;

    private static final float SIDE_W = 172.0f;
    private static final float PAD = 12.0f;
    private static final float CARD_H = 54.0f;
    private static final float CARD_GAP = 10.0f;

    private static final float PANEL_R = 14.0f;
    private static final float SEARCH_R = 11.0f;
    private static final float CARD_R = 10.0f;

    private static final float CAT_TOP = 40.0f;
    private static final float CAT_PITCH = 38.0f;
    private static final float CAT_H = 34.0f;

    // ---- Colors -------------------------------------------------------------
    private static final int ACCENT = 0x9B4DE8;
    private static final int ACCENT_DEEP = 0x6D2BD0;
    private static final int PANEL = 0x0E1018;
    private static final int TEXT = 0xF2F3F8;
    private static final int TEXT_DIM = 0xA4A9BB;
    private static final int TEXT_FAINT = 0x6B7185;

    // ---- Animation state ----------------------------------------------------
    private static float openProgress;
    private static float pillIndex = -1.0f;
    private static float scrollAnim;
    private static long lastNanos;
    private static int lastDrawnFrame = -1;
    private static final float[] categoryHover = new float[16];

    public static final class Module {
        public final String name;
        public final String description;
        public final String category;
        public boolean enabled;

        // Renderer-owned animation values.
        float anim;
        float hover;

        public Module(String name, String description, String category) {
            this.name = name;
            this.description = description;
            this.category = category;
        }
    }

    /** Call when the screen is opened so the intro animation restarts. */
    public static void open() {
        openProgress = 0.0f;
        pillIndex = -1.0f;
        scrollAnim = 0.0f;
        lastNanos = 0L;
        lastDrawnFrame = -1;
    }

    // =========================================================================
    // Rendering
    // =========================================================================

    public static void render(ClickGuiScreen screen) {
        // GuiRenderer.render can run more than once per frame; only draw once.
        if (screen.frameId() == lastDrawnFrame) {
            return;
        }
        lastDrawnFrame = screen.frameId();

        GlStateGuard.save();
        try {
            GlStateGuard.prepareForNanoVG();
            drainGlErrors();
            renderInternal(screen);
            reportGlError();
        } finally {
            NanoVGRenderer.cancelFrame(); // no-op unless a frame was left open by an exception
            GlStateGuard.restore();
        }
    }

    // Diagnostics: report the first few GL errors raised while the ClickGUI draws.
    private static final int MAX_GL_ERROR_LOGS = 5;
    private static int glErrorLogs;

    private static void drainGlErrors() {
        if (glErrorLogs >= MAX_GL_ERROR_LOGS) {
            return;
        }
        for (int i = 0; i < 16 && GL11.glGetError() != GL11.GL_NO_ERROR; i++) {
            // Discard errors that were already pending from Minecraft's own rendering.
        }
    }

    private static void reportGlError() {
        if (glErrorLogs >= MAX_GL_ERROR_LOGS) {
            return;
        }
        int error = GL11.glGetError();
        if (error != GL11.GL_NO_ERROR) {
            glErrorLogs++;
            System.err.println("[Cold Client] OpenGL error 0x" + Integer.toHexString(error)
                    + " while drawing the ClickGUI");
        }
    }

    private static void renderInternal(ClickGuiScreen screen) {
        if (!NanoVGRenderer.initialize()) {
            return;
        }
        Ui.ensureFonts();

        float dt = step();
        openProgress = approach(openProgress, 1.0f, dt, 14.0f);
        float e = easeOutCubic(Math.min(1.0f, openProgress));

        Layout L = layout(screen);
        float sc = L.scale;

        pillIndex = pillIndex < 0.0f
                ? screen.selectedCategory()
                : approach(pillIndex, screen.selectedCategory(), dt, 16.0f);
        scrollAnim = approach(scrollAnim, screen.scroll(), dt, 18.0f);

        NanoVGRenderer.beginFrame(screen.width, screen.height, pixelRatio(screen));
        if (!NanoVGRenderer.isFrameActive()) {
            return;
        }

        long ctx = NanoVGRenderer.getContext();
        nvgGlobalAlpha(ctx, e);

        float hair = Math.max(0.45f, 0.7f * sc);
        float mx = screen.mouseX();
        float my = screen.mouseY();

        // World dimmer.
        Ui.rect(0, 0, screen.width, screen.height, 0, 0x04050A, 0.50f);

        // Subtle pop-in scale around the screen center.
        float pop = 0.965f + 0.035f * e;
        nvgSave(ctx);
        nvgTranslate(ctx, screen.width * 0.5f, screen.height * 0.5f);
        nvgScale(ctx, pop, pop);
        nvgTranslate(ctx, -screen.width * 0.5f, -screen.height * 0.5f);

        // ---- Search bar -----------------------------------------------------
        panel(L.searchX, L.searchY, L.searchW, L.searchH, SEARCH_R * sc, sc, hair);
        if (screen.searchFocused()) {
            Ui.outline(L.searchX, L.searchY, L.searchW, L.searchH, SEARCH_R * sc, hair, ACCENT, 0.65f);
        }

        float searchMid = L.searchY + L.searchH * 0.5f;
        float textX = L.searchX + 38 * sc;
        float searchFont = 12.5f * sc;
        String query = screen.search();

        Ui.icon(Ui.ICON_SEARCH, L.searchX + 20 * sc, searchMid, 13 * sc, TEXT_FAINT, 1.0f);
        if (query.isEmpty()) {
            Ui.text("Search for any module or feature", textX, searchMid, searchFont, false,
                    TEXT_FAINT, 1.0f, Ui.ALIGN_LEFT_MIDDLE);
        } else {
            Ui.text(query, textX, searchMid, searchFont, false, TEXT, 1.0f, Ui.ALIGN_LEFT_MIDDLE);
        }
        if (screen.searchFocused() && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            float caretX = textX + (query.isEmpty() ? 0.0f : Ui.textWidth(query, searchFont, false)) + 1.5f * sc;
            Ui.rect(caretX, searchMid - 7 * sc, 1.2f * sc, 14 * sc, 0, TEXT, 0.9f);
        }

        // ---- Sidebar --------------------------------------------------------
        panel(L.sideX, L.sideY, L.sideW, L.sideH, PANEL_R * sc, sc, hair);

        Ui.text("MODULES", L.sideX + 20 * sc, L.sideY + 22 * sc, 9.0f * sc, true,
                TEXT_FAINT, 1.0f, Ui.ALIGN_LEFT_MIDDLE, 1.2f * sc);

        // Animated selection pill.
        float pillX = L.sideX + 10 * sc;
        float pillW = L.sideW - 20 * sc;
        float pillH = CAT_H * sc;
        float pillY = L.sideY + CAT_TOP * sc + pillIndex * CAT_PITCH * sc;
        Ui.shadow(pillX, pillY, pillW, pillH, 9 * sc, 14 * sc, 3 * sc, ACCENT, 0.40f);
        Ui.gradient(pillX, pillY, pillW, pillH, 9 * sc, ACCENT, 1.0f, ACCENT_DEEP, 1.0f, false);

        String[] categories = screen.categories();
        for (int i = 0; i < categories.length; i++) {
            float itemY = L.sideY + (CAT_TOP + i * CAT_PITCH) * sc;

            boolean hovered = inside(mx, my, pillX, itemY, pillW, pillH);
            categoryHover[i] = approach(categoryHover[i], hovered ? 1.0f : 0.0f, dt, 18.0f);

            // How "selected" this item looks, following the moving pill.
            float sel = Math.max(0.0f, 1.0f - Math.abs(pillIndex - i));

            if (categoryHover[i] > 0.01f) {
                Ui.rect(pillX, itemY, pillW, pillH, 9 * sc, 0xFFFFFF, 0.06f * categoryHover[i] * (1.0f - sel));
            }

            int labelColor = Ui.mix(TEXT_DIM, 0xFFFFFF, Math.max(sel, categoryHover[i] * 0.5f));
            Ui.icon(i, L.sideX + 29 * sc, itemY + pillH * 0.5f, 15 * sc, labelColor, 1.0f);
            Ui.text(categories[i], L.sideX + 48 * sc, itemY + pillH * 0.5f, 13.0f * sc, sel > 0.5f,
                    labelColor, 1.0f, Ui.ALIGN_LEFT_MIDDLE);
        }

        Ui.text(ColdClient.NAME + " " + ColdClient.VERSION, L.sideX + 20 * sc, L.sideY + L.sideH - 18 * sc,
                9.5f * sc, false, 0x4E5468, 1.0f, Ui.ALIGN_LEFT_MIDDLE);

        // ---- Content --------------------------------------------------------
        panel(L.contentX, L.contentY, L.contentW, L.contentH, PANEL_R * sc, sc, hair);

        List<Integer> visible = visibleModules(screen);

        if (visible.isEmpty()) {
            Ui.text("No modules found", L.contentX + L.contentW * 0.5f, L.contentY + L.contentH * 0.5f,
                    13.0f * sc, false, TEXT_FAINT, 1.0f, Ui.ALIGN_CENTER_MIDDLE);
        }

        nvgScissor(ctx, L.vpX, L.vpY, L.vpW, L.vpH);

        float yOffset = -scrollAnim * sc;
        boolean mouseInViewport = inside(mx, my, L.vpX, L.vpY, L.vpW, L.vpH);

        for (int n = 0; n < visible.size(); n++) {
            int row = n / 2;
            int col = n % 2;
            Module module = screen.modules()[visible.get(n)];

            float x = L.cardsX + col * (L.colW + L.gap);
            float y = L.cardsY + row * (L.cardH + L.gap) + yOffset;

            if (y + L.cardH < L.vpY || y > L.vpY + L.vpH) {
                continue;
            }

            boolean hovered = mouseInViewport && inside(mx, my, x, y, L.colW, L.cardH);
            module.hover = approach(module.hover, hovered ? 1.0f : 0.0f, dt, 18.0f);
            module.anim = approach(module.anim, module.enabled ? 1.0f : 0.0f, dt, 16.0f);

            float r = CARD_R * sc;

            Ui.rect(x, y, L.colW, L.cardH, r, 0xFFFFFF, 0.035f + 0.03f * module.hover);
            if (module.anim > 0.01f) {
                Ui.gradient(x, y, L.colW, L.cardH, r,
                        ACCENT, 0.22f * module.anim, ACCENT_DEEP, 0.04f * module.anim, false);
            }
            Ui.outline(x, y, L.colW, L.cardH, r, hair,
                    Ui.mix(0xFFFFFF, ACCENT, module.anim),
                    0.07f + 0.07f * module.hover + 0.48f * module.anim);

            float toggleW = 30 * sc;
            float toggleH = 16 * sc;
            float toggleX = x + L.colW - 14 * sc - toggleW;
            Ui.toggle(toggleX, y + (L.cardH - toggleH) * 0.5f, toggleW, toggleH, module.anim, ACCENT);

            float textMax = toggleX - (x + 14 * sc) - 10 * sc;
            float nameSize = 13.5f * sc;
            float descSize = 10.5f * sc;

            Ui.text(Ui.fit(module.name, nameSize, true, textMax), x + 14 * sc, y + L.cardH * 0.36f,
                    nameSize, true, Ui.mix(0xE4E6EE, 0xFFFFFF, module.anim), 1.0f, Ui.ALIGN_LEFT_MIDDLE);
            Ui.text(Ui.fit(module.description, descSize, false, textMax), x + 14 * sc, y + L.cardH * 0.70f,
                    descSize, false, TEXT_FAINT, 1.0f, Ui.ALIGN_LEFT_MIDDLE);
        }

        nvgResetScissor(ctx);

        // Scrollbar.
        float max = maxScroll(screen);
        if (max > 0.0f) {
            float trackX = L.contentX + L.contentW - 6 * sc;
            float trackY = L.vpY + 6 * sc;
            float trackH = L.vpH - 12 * sc;
            float viewUnits = MAIN_H - 2 * PAD;
            float thumbH = Math.max(24 * sc, trackH * viewUnits / (viewUnits + max));
            float thumbY = trackY + (trackH - thumbH) * Math.min(1.0f, scrollAnim / max);
            Ui.rect(trackX, thumbY, 2.5f * sc, thumbH, 1.25f * sc, 0xFFFFFF, 0.18f);
        }

        nvgRestore(ctx);
        NanoVGRenderer.endFrame();
    }

    /** Glassy dark panel with a soft shadow and hairline border. */
    private static void panel(float x, float y, float w, float h, float r, float sc, float hair) {
        Ui.shadow(x, y, w, h, r, 22 * sc, 6 * sc, 0x000000, 0.45f);
        Ui.rect(x, y, w, h, r, PANEL, 0.80f);
        Ui.outline(x, y, w, h, r, hair, 0xFFFFFF, 0.07f);
    }

    // =========================================================================
    // Hit testing (used by ClickGuiScreen)
    // =========================================================================

    public static boolean hitSearch(ClickGuiScreen screen, float mx, float my) {
        Layout L = layout(screen);
        return inside(mx, my, L.searchX, L.searchY, L.searchW, L.searchH);
    }

    public static int hitCategory(ClickGuiScreen screen, float mx, float my) {
        Layout L = layout(screen);
        for (int i = 0; i < screen.categories().length; i++) {
            if (inside(mx, my, L.sideX + 10 * L.scale, L.sideY + (CAT_TOP + i * CAT_PITCH) * L.scale,
                    L.sideW - 20 * L.scale, CAT_H * L.scale)) {
                return i;
            }
        }
        return -1;
    }

    public static int hitModule(ClickGuiScreen screen, float mx, float my) {
        Layout L = layout(screen);
        if (!inside(mx, my, L.vpX, L.vpY, L.vpW, L.vpH)) {
            return -1;
        }

        List<Integer> visible = visibleModules(screen);
        float yOffset = -scrollAnim * L.scale;
        for (int n = 0; n < visible.size(); n++) {
            float x = L.cardsX + (n % 2) * (L.colW + L.gap);
            float y = L.cardsY + (n / 2) * (L.cardH + L.gap) + yOffset;
            if (inside(mx, my, x, y, L.colW, L.cardH)) {
                return visible.get(n);
            }
        }
        return -1;
    }

    /** Maximum scroll in base units. */
    public static float maxScroll(ClickGuiScreen screen) {
        int rows = (visibleModules(screen).size() + 1) / 2;
        float contentHeight = rows * (CARD_H + CARD_GAP) - CARD_GAP;
        return Math.max(0.0f, contentHeight - (MAIN_H - 2 * PAD));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static List<Integer> visibleModules(ClickGuiScreen screen) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < screen.modules().length; i++) {
            if (screen.matches(screen.modules()[i])) {
                result.add(i);
            }
        }
        return result;
    }

    private record Layout(
            float scale,
            float searchX, float searchY, float searchW, float searchH,
            float sideX, float sideY, float sideW, float sideH,
            float contentX, float contentY, float contentW, float contentH,
            float cardsX, float cardsY, float colW, float cardH, float gap,
            float vpX, float vpY, float vpW, float vpH
    ) {}

    private static Layout layout(ClickGuiScreen s) {
        float sc = Math.min(s.width * 0.94f / BASE_W, s.height * 0.90f / BASE_H);
        sc = Math.max(0.25f, Math.min(sc, 2.2f));

        float totalW = BASE_W * sc;
        float totalH = BASE_H * sc;
        float x0 = (s.width - totalW) * 0.5f;
        float y0 = (s.height - totalH) * 0.5f;

        float mainY = y0 + (SEARCH_H + GAP) * sc;
        float contentX = x0 + (SIDE_W + GAP) * sc;
        float contentW = (BASE_W - SIDE_W - GAP) * sc;
        float colW = ((BASE_W - SIDE_W - GAP - 2 * PAD) - CARD_GAP) * 0.5f * sc;

        return new Layout(
                sc,
                x0, y0, totalW, SEARCH_H * sc,
                x0, mainY, SIDE_W * sc, MAIN_H * sc,
                contentX, mainY, contentW, MAIN_H * sc,
                contentX + PAD * sc, mainY + PAD * sc, colW, CARD_H * sc, CARD_GAP * sc,
                contentX + 2 * sc, mainY + 2 * sc, contentW - 4 * sc, MAIN_H * sc - 4 * sc
        );
    }

    /** Framebuffer pixels per GUI unit, so NanoVG text is rasterized crisply. */
    private static float pixelRatio(ClickGuiScreen screen) {
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        if (viewport[2] <= 0 || screen.width <= 0) {
            return 1.0f;
        }
        float ratio = viewport[2] / (float) screen.width;
        return Math.max(1.0f, Math.min(ratio, 8.0f));
    }

    private static float step() {
        long now = System.nanoTime();
        float dt = lastNanos == 0L ? 0.016f : Math.min(0.1f, (now - lastNanos) / 1_000_000_000.0f);
        lastNanos = now;
        return dt;
    }

    private static float approach(float current, float target, float dt, float speed) {
        return current + (target - current) * (1.0f - (float) Math.exp(-dt * speed));
    }

    private static boolean inside(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static float easeOutCubic(float x) {
        float inv = 1.0f - x;
        return 1.0f - inv * inv * inv;
    }
}