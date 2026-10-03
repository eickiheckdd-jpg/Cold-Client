package com.coldclient.gui;

import com.coldclient.ColdClient;
import com.coldclient.module.KeyNames;
import com.coldclient.render.GlStateGuard;
import com.coldclient.render.NanoVGRenderer;
import com.coldclient.render.Ui;
import com.coldclient.setting.BooleanSetting;
import com.coldclient.setting.KeybindSetting;
import com.coldclient.setting.ModeSetting;
import com.coldclient.setting.RangeSetting;
import com.coldclient.setting.Setting;
import com.coldclient.setting.SliderSetting;
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
    private static final float SETTING_H = 66.0f;
    private static final float SETTING_GAP = 10.0f;
    private static final float SETTINGS_TOP = 64.0f; // where the first setting row starts (base units)
    private static final float OPT_H = 26.0f;        // one dropdown option row
    private static final float OPT_PAD = 8.0f;       // bottom padding of an open dropdown
    private static final float THUMB_R = 6.0f;

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
        private final List<Setting<?>> settings;

        // Renderer-owned animation values.
        float anim;
        float hover;

        public Module(String name, String description, String category, Setting<?>... settings) {
            this.name = name;
            this.description = description;
            this.category = category;
            this.settings = List.of(settings == null ? new Setting<?>[0] : settings);
        }

        public List<Setting<?>> settings() {
            return settings;
        }

        /** Find a setting by its developer-facing name. */
        public Setting<?> setting(String name) {
            for (Setting<?> setting : settings) {
                if (setting.name().equals(name)) {
                    return setting;
                }
            }
            return null;
        }
    }

    /** Call when the screen is opened so the intro animation restarts. */
    public static void open() {
        openProgress = 0.0f;
        pillIndex = -1.0f;
        scrollAnim = 0.0f;
        lastNanos = 0L;
        lastDrawnFrame = -1;
        drag = null;
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

        if (!screen.isConfiguring()) {
            pillIndex = pillIndex < 0.0f
                    ? screen.selectedCategory()
                    : approach(pillIndex, screen.selectedCategory(), dt, 16.0f);
            scrollAnim = approach(scrollAnim, screen.scroll(), dt, 18.0f);
        }

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

        // ---- Search bar / config header -----------------------------------
        if (screen.isConfiguring()) {
            panel(L.searchX, L.searchY, L.searchW, L.searchH, SEARCH_R * sc, sc, hair);
            Ui.text("CONFIGURATION", L.searchX + 18 * sc, L.searchY + L.searchH * 0.5f,
                    12.5f * sc, true, TEXT, 1.0f, Ui.ALIGN_LEFT_MIDDLE, 1.0f * sc);
            Module module = screen.configuredModule();
            if (module != null) {
                Ui.text(module.name, L.searchX + L.searchW - 18 * sc, L.searchY + L.searchH * 0.5f,
                        11.5f * sc, false, TEXT_DIM, 1.0f, Ui.ALIGN_RIGHT_MIDDLE);
            }
        } else {
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
        }

        // ---- Sidebar --------------------------------------------------------
        panel(L.sideX, L.sideY, L.sideW, L.sideH, PANEL_R * sc, sc, hair);

        if (screen.isConfiguring()) {
            Ui.text("CONFIG", L.sideX + 20 * sc, L.sideY + 22 * sc, 9.0f * sc, true,
                    TEXT_FAINT, 1.0f, Ui.ALIGN_LEFT_MIDDLE, 1.2f * sc);

            float backY = L.sideY + CAT_TOP * sc;
            boolean backHover = inside(mx, my, L.sideX + 10 * sc, backY, L.sideW - 20 * sc, CAT_H * sc);
            Ui.rect(L.sideX + 10 * sc, backY, L.sideW - 20 * sc, CAT_H * sc, 9 * sc,
                    0xFFFFFF, backHover ? 0.08f : 0.03f);
            Ui.text("←", L.sideX + 29 * sc, backY + CAT_H * sc * 0.5f, 15 * sc, false,
                    TEXT_DIM, 1.0f, Ui.ALIGN_CENTER_MIDDLE);
            Ui.text("Back", L.sideX + 48 * sc, backY + CAT_H * sc * 0.5f, 13 * sc, false,
                    backHover ? TEXT : TEXT_DIM, 1.0f, Ui.ALIGN_LEFT_MIDDLE);

            Module module = screen.configuredModule();
            if (module != null) {
                Ui.text(Ui.fit(module.name, 11.5f * sc, true, (L.sideW - 40 * sc)),
                        L.sideX + 20 * sc, L.sideY + 104 * sc, 11.5f * sc, true,
                        TEXT, 1.0f, Ui.ALIGN_LEFT_MIDDLE);
                Ui.text("Settings", L.sideX + 20 * sc, L.sideY + 128 * sc,
                        10.5f * sc, false, TEXT_FAINT, 1.0f, Ui.ALIGN_LEFT_MIDDLE);
            }
        } else {
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

                float sel = Math.max(0.0f, 1.0f - Math.abs(pillIndex - i));

                if (categoryHover[i] > 0.01f) {
                    Ui.rect(pillX, itemY, pillW, pillH, 9 * sc, 0xFFFFFF, 0.06f * categoryHover[i] * (1.0f - sel));
                }

                int labelColor = Ui.mix(TEXT_DIM, 0xFFFFFF, Math.max(sel, categoryHover[i] * 0.5f));
                Ui.icon(i, L.sideX + 29 * sc, itemY + pillH * 0.5f, 15 * sc, labelColor, 1.0f);
                Ui.text(categories[i], L.sideX + 48 * sc, itemY + pillH * 0.5f, 13.0f * sc, sel > 0.5f,
                        labelColor, 1.0f, Ui.ALIGN_LEFT_MIDDLE);
            }
        }

        Ui.text(ColdClient.NAME + " " + ColdClient.VERSION, L.sideX + 20 * sc, L.sideY + L.sideH - 18 * sc,
                9.5f * sc, false, 0x4E5468, 1.0f, Ui.ALIGN_LEFT_MIDDLE);

        // ---- Content --------------------------------------------------------
        panel(L.contentX, L.contentY, L.contentW, L.contentH, PANEL_R * sc, sc, hair);

        if (screen.isConfiguring()) {
            renderSettings(ctx, screen, L, sc, hair, mx, my);
        } else {
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
        }

        nvgRestore(ctx);
        NanoVGRenderer.endFrame();
    }

    private static void renderSettings(long ctx, ClickGuiScreen screen, Layout L, float sc, float hair, float mx, float my) {
        Module module = screen.configuredModule();
        if (module == null) {
            Ui.text("No module selected", L.contentX + L.contentW * 0.5f, L.contentY + L.contentH * 0.5f,
                    13.0f * sc, false, TEXT_FAINT, 1.0f, Ui.ALIGN_CENTER_MIDDLE);
            return;
        }

        Ui.text("SETTINGS", L.contentX + 20 * sc, L.contentY + 22 * sc,
                9.0f * sc, true, TEXT_FAINT, 1.0f, Ui.ALIGN_LEFT_MIDDLE, 1.2f * sc);
        Ui.text(Ui.fit(module.description, 11.0f * sc, false, L.contentW - 40 * sc),
                L.contentX + 20 * sc, L.contentY + 46 * sc,
                11.0f * sc, false, TEXT_DIM, 1.0f, Ui.ALIGN_LEFT_MIDDLE);

        List<Setting<?>> settings = module.settings();
        if (settings.isEmpty()) {
            Ui.text("This module has no settings yet.", L.contentX + L.contentW * 0.5f,
                    L.contentY + L.contentH * 0.5f, 12.0f * sc, false,
                    TEXT_FAINT, 1.0f, Ui.ALIGN_CENTER_MIDDLE);
            return;
        }

        float x = L.contentX + PAD * sc;
        float y = L.contentY + (SETTINGS_TOP - screen.settingScroll()) * sc;
        float w = L.contentW - 2 * PAD * sc;
        float[] track = trackGeometry(L);
        float trackX = track[0];
        float trackW = track[1];
        float topH = SETTING_H * sc;
        boolean mouseInViewport = inside(mx, my, L.vpX, L.vpY, L.vpW, L.vpH);

        nvgScissor(ctx, L.vpX, L.vpY, L.vpW, L.vpH);
        for (Setting<?> setting : settings) {
            float rowH = settingHeight(setting) * sc;
            if (y + rowH < L.vpY || y > L.vpY + L.vpH) {
                y += rowH + SETTING_GAP * sc;
                continue;
            }

            boolean hovered = mouseInViewport && inside(mx, my, x, y, w, topH);
            Ui.rect(x, y, w, rowH, CARD_R * sc, 0xFFFFFF, hovered ? 0.055f : 0.035f);
            Ui.outline(x, y, w, rowH, CARD_R * sc, hair,
                    hovered ? 0xFFFFFF : 0x8E94A8, hovered ? 0.15f : 0.07f);

            float leftMax = w - ((setting instanceof KeybindSetting || setting instanceof ModeSetting) ? 200 : 170) * sc;
            Ui.text(Ui.fit(setting.name(), 13.0f * sc, true, leftMax),
                    x + 14 * sc, y + 20 * sc, 13.0f * sc, true, TEXT, 1.0f, Ui.ALIGN_LEFT_MIDDLE);
            if (!setting.description().isEmpty()) {
                Ui.text(Ui.fit(setting.description(), 10.0f * sc, false, leftMax),
                        x + 14 * sc, y + 43 * sc, 10.0f * sc, false, TEXT_FAINT, 1.0f, Ui.ALIGN_LEFT_MIDDLE);
            }

            float midY = y + topH * 0.5f;

            if (setting instanceof BooleanSetting bool) {
                float toggleW = 34 * sc;
                float toggleH = 18 * sc;
                float toggleX = x + w - 18 * sc - toggleW;
                Ui.toggle(toggleX, midY - toggleH * 0.5f,
                        toggleW, toggleH, bool.value() ? 1.0f : 0.0f, ACCENT);
            } else if (setting instanceof SliderSetting slider) {
                float fraction = (float) ((slider.value() - slider.min()) / (slider.max() - slider.min()));
                drawTrack(trackX, midY, trackW, 0.0f, fraction, sc);
                Ui.circle(trackX + trackW * fraction, midY, THUMB_R * sc, 0xFFFFFF, 0.98f);
                Ui.text(formatSliderValue(slider), x + w - 18 * sc, y + 16 * sc,
                        10.0f * sc, true, TEXT_DIM, 1.0f, Ui.ALIGN_RIGHT_MIDDLE);
            } else if (setting instanceof RangeSetting range) {
                float lo = (float) range.lowFraction();
                float hi = (float) range.highFraction();
                drawTrack(trackX, midY, trackW, lo, hi, sc);
                Ui.circle(trackX + trackW * lo, midY, THUMB_R * sc, 0xFFFFFF, 0.98f);
                Ui.circle(trackX + trackW * hi, midY, THUMB_R * sc, 0xFFFFFF, 0.98f);
                Ui.text(formatRangeValue(range), x + w - 18 * sc, y + 16 * sc,
                        10.0f * sc, true, TEXT_DIM, 1.0f, Ui.ALIGN_RIGHT_MIDDLE);
            } else if (setting instanceof KeybindSetting bind) {
                float boxW = 132 * sc;
                float boxH = 26 * sc;
                float boxX = x + w - 18 * sc - boxW;
                float boxY = midY - boxH * 0.5f;
                boolean boxHover = mouseInViewport && inside(mx, my, boxX, boxY, boxW, boxH);
                boolean listening = bind.listening();

                Ui.rect(boxX, boxY, boxW, boxH, 7 * sc, listening ? ACCENT : 0xFFFFFF,
                        listening ? 0.16f : (boxHover ? 0.09f : 0.05f));
                Ui.outline(boxX, boxY, boxW, boxH, 7 * sc, hair, listening ? ACCENT : 0xFFFFFF,
                        listening ? 0.80f : (boxHover ? 0.20f : 0.12f));
                String label = listening ? "[Press a Key...]" : KeyNames.of(bind.key());
                Ui.text(Ui.fit(label, 11.0f * sc, true, boxW - 14 * sc),
                        boxX + boxW * 0.5f, boxY + boxH * 0.5f, 11.0f * sc, true,
                        (listening || bind.isBound()) ? TEXT : TEXT_DIM, 1.0f, Ui.ALIGN_CENTER_MIDDLE);
            } else if (setting instanceof ModeSetting mode) {
                float boxW = 150 * sc;
                float boxH = 26 * sc;
                float boxX = x + w - 18 * sc - boxW;
                float boxY = midY - boxH * 0.5f;
                boolean boxHover = mouseInViewport && inside(mx, my, boxX, boxY, boxW, boxH);

                Ui.rect(boxX, boxY, boxW, boxH, 7 * sc, mode.open() ? ACCENT : 0xFFFFFF,
                        mode.open() ? 0.14f : (boxHover ? 0.09f : 0.05f));
                Ui.outline(boxX, boxY, boxW, boxH, 7 * sc, hair, mode.open() ? ACCENT : 0xFFFFFF,
                        mode.open() ? 0.70f : (boxHover ? 0.20f : 0.12f));
                Ui.text(Ui.fit(mode.get(), 11.0f * sc, true, boxW - 38 * sc),
                        boxX + 12 * sc, boxY + boxH * 0.5f, 11.0f * sc, true, TEXT, 1.0f, Ui.ALIGN_LEFT_MIDDLE);
                Ui.text(mode.open() ? "^" : "v", boxX + boxW - 14 * sc, boxY + boxH * 0.5f,
                        10.5f * sc, true, TEXT_DIM, 1.0f, Ui.ALIGN_CENTER_MIDDLE);

                if (mode.open()) {
                    for (int i = 0; i < mode.optionCount(); i++) {
                        float rx = x + 12 * sc;
                        float ry = y + topH + i * OPT_H * sc;
                        float rw = w - 24 * sc;
                        float rh = (OPT_H - 3) * sc;
                        boolean selected = i == mode.index();
                        boolean optHover = mouseInViewport && inside(mx, my, rx, ry, rw, rh);

                        Ui.rect(rx, ry, rw, rh, 7 * sc, selected ? ACCENT : 0xFFFFFF,
                                selected ? 0.18f : (optHover ? 0.08f : 0.03f));
                        Ui.text(mode.option(i), rx + 12 * sc, ry + rh * 0.5f, 11.0f * sc, selected,
                                selected ? TEXT : TEXT_DIM, 1.0f, Ui.ALIGN_LEFT_MIDDLE);
                        if (selected) {
                            Ui.circle(rx + rw - 14 * sc, ry + rh * 0.5f, 3.2f * sc, ACCENT, 1.0f);
                        }
                    }
                }
            }

            y += rowH + SETTING_GAP * sc;
        }
        nvgResetScissor(ctx);
    }

    /** Track with a filled segment from fromFraction to toFraction (sliders use 0..value). */
    private static void drawTrack(float trackX, float midY, float trackW, float fromFraction, float toFraction, float sc) {
        Ui.rect(trackX, midY - 3 * sc, trackW, 6 * sc, 3 * sc, 0x3A3F52, 1.0f);
        float fillW = trackW * (toFraction - fromFraction);
        if (fillW > 0.5f) {
            Ui.gradient(trackX + trackW * fromFraction, midY - 3 * sc, fillW, 6 * sc,
                    3 * sc, ACCENT, 1.0f, ACCENT_DEEP, 1.0f, false);
        }
    }

    private static String formatNumber(double value, double step) {
        if (Math.abs(step - Math.rint(step)) < 0.000001) {
            return Long.toString(Math.round(value));
        }
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private static String formatSliderValue(SliderSetting slider) {
        return formatNumber(slider.value(), slider.step()) + slider.suffix();
    }

    private static String formatRangeValue(RangeSetting range) {
        return formatNumber(range.low(), range.step()) + " - " + formatNumber(range.high(), range.step())
                + range.suffix();
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

    public static int
hitCategory(ClickGuiScreen screen, float mx, float my) {
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
        if (screen.isConfiguring()) {
            return -1;
        }
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

    public static boolean hitConfigBack(ClickGuiScreen screen, float mx, float my) {
        if (!screen.isConfiguring()) {
            return false;
        }
        Layout L = layout(screen);
        float y = L.sideY + CAT_TOP * L.scale;
        return inside(mx, my, L.sideX + 10 * L.scale, y,
                L.sideW - 20 * L.scale, CAT_H * L.scale);
    }

    /** Maximum scroll in base units. */
    public static float maxScroll(ClickGuiScreen screen) {
        if (screen.isConfiguring()) {
            return 0.0f;
        }
        int rows = (visibleModules(screen).size() + 1) / 2;
        float contentHeight = rows * (CARD_H + CARD_GAP) - CARD_GAP;
        return Math.max(0.0f, contentHeight - (MAIN_H - 2 * PAD));
    }

    // ---- Settings input -----------------------------------------------------

    /** Which slider/range thumb is being dragged. */
    private static final class Drag {
        final Setting<?> setting;
        int thumb;      // range only: 0 = low, 1 = high, -1 = stacked, decide on first movement
        float stackX;   // range only: x of the stacked thumbs while undecided

        Drag(Setting<?> setting) {
            this.setting = setting;
        }
    }

    private static Drag drag;

    /** Height of one setting row in base units (an open dropdown grows). */
    private static float settingHeight(Setting<?> setting) {
        if (setting instanceof ModeSetting mode && mode.open()) {
            return SETTING_H + mode.optionCount() * OPT_H + OPT_PAD;
        }
        return SETTING_H;
    }

    /** x and width of the slider track, shared by drawing, press and drag. */
    private static float[] trackGeometry(Layout L) {
        float sc = L.scale;
        float x = L.contentX + PAD * sc;
        float w = L.contentW - 2 * PAD * sc;
        float trackW = Math.min(210 * sc, w * 0.36f);
        return new float[]{x + w - 18 * sc - trackW, trackW};
    }

    /**
     * Handles a left press in the settings view using the same geometry as renderSettings().
     * Toggles/dropdowns/keybind boxes act immediately; sliders start a drag that
     * {@link #dragSetting} continues. Returns true if the press hit a setting row.
     */
    public static boolean pressSetting(ClickGuiScreen screen, float mx, float my) {
        drag = null;
        Module module = screen.configuredModule();
        if (module == null) {
            return false;
        }

        Layout L = layout(screen);
        float sc = L.scale;
        if (!inside(mx, my, L.vpX, L.vpY, L.vpW, L.vpH)) {
            clearListening(module, null);
            return false;
        }

        float x = L.contentX + PAD * sc;
        float w = L.contentW - 2 * PAD * sc;
        float[] track = trackGeometry(L);
        float topH = SETTING_H * sc;
        float y = L.contentY + (SETTINGS_TOP - screen.settingScroll()) * sc;

        for (Setting<?> setting : module.settings()) {
            float rowH = settingHeight(setting) * sc;
            if (inside(mx, my, x, y, w, rowH)) {
                clearListening(module, setting);
                float midY = y + topH * 0.5f;

                if (setting instanceof BooleanSetting bool) {
                    bool.toggle();
                } else if (setting instanceof SliderSetting slider) {
                    // Generous start zone around the track; once started, the drag follows the
                    // mouse anywhere on screen.
                    if (inTrackZone(mx, track, sc)) {
                        drag = new Drag(slider);
                        slider.setFromFraction((mx - track[0]) / track[1]);
                    }
                } else if (setting instanceof RangeSetting range) {
                    if (inTrackZone(mx, track, sc)) {
                        beginRangeDrag(range, mx, track, sc);
                    }
                } else if (setting instanceof KeybindSetting bind) {
                    float boxW = 132 * sc;
                    float boxH = 26 * sc;
                    float boxX = x + w - 18 * sc - boxW;
                    if (inside(mx, my, boxX, midY - boxH * 0.5f, boxW, boxH)) {
                        bind.setListening(!bind.listening());
                    }
                } else if (setting instanceof ModeSetting mode) {
                    if (my < y + topH) {
                        float boxW = 150 * sc;
                        float boxH = 26 * sc;
                        float boxX = x + w - 18 * sc - boxW;
                        if (inside(mx, my, boxX, midY - boxH * 0.5f, boxW, boxH)) {
                            mode.setOpen(!mode.open());
                        }
                    } else if (mode.open()) {
                        int index = (int) Math.floor((my - (y + topH)) / (OPT_H * sc));
                        if (index >= 0 && index < mode.optionCount()) {
                            mode.select(index);
                            mode.setOpen(false);
                        }
                    }
                }
                return true;
            }
            y += rowH + SETTING_GAP * sc;
        }

        clearListening(module, null);
        return false;
    }

    private static boolean inTrackZone(float mx, float[] track, float sc) {
        return mx >= track[0] - 14 * sc && mx <= track[0] + track[1] + 14 * sc;
    }

    private static void beginRangeDrag(RangeSetting range, float mx, float[] track, float sc) {
        float lx = track[0] + (float) range.lowFraction() * track[1];
        float hx = track[0] + (float) range.highFraction() * track[1];
        Drag d = new Drag(range);

        if (range.low() == range.high()) {
            // Thumbs are stacked.
            if (Math.abs(mx - lx) > 2 * THUMB_R * sc) {
                d.thumb = mx > lx ? 1 : 0;              // clicked away from the stack: nearest side
            } else if (range.high() >= range.max()) {
                d.thumb = 0;                             // pinned at the right end: only low can move
            } else if (range.low() <= range.min()) {
                d.thumb = 1;                             // pinned at the left end: only high can move
            } else {
                d.thumb = -1;                            // pick by drag direction
                d.stackX = lx;
            }
        } else {
            d.thumb = Math.abs(mx - lx) <= Math.abs(mx - hx) ? 0 : 1; // nearest thumb
        }

        drag = d;
        if (d.thumb >= 0) {
            applyRangeDrag(range, d.thumb, mx, track);
        }
    }

    private static void applyRangeDrag(RangeSetting range, int thumb, float mx, float[] track) {
        double fraction = (mx - track[0]) / track[1];
        if (thumb == 0) {
            range.setLowFromFraction(fraction);
        } else {
            range.setHighFromFraction(fraction);
        }
    }

    /** Continues a slider/range drag. Works wherever the mouse is, not only over the track. */
    public static boolean dragSetting(ClickGuiScreen screen, float mx, float my) {
        if (drag == null) {
            return false;
        }
        float[] track = trackGeometry(layout(screen));

        if (drag.setting instanceof SliderSetting slider) {
            slider.setFromFraction((mx - track[0]) / track[1]);
        } else if (drag.setting instanceof RangeSetting range) {
            if (drag.thumb < 0) {
                if (Math.abs(mx - drag.stackX) < 1.0f) {
                    return true; // no direction yet
                }
                drag.thumb = mx > drag.stackX ? 1 : 0;
            }
            applyRangeDrag(range, drag.thumb, mx, track);
        }
        return true;
    }

    public static void endDrag() {
        drag = null;
    }

    public static boolean isDragging() {
        return drag != null;
    }

    private static void clearListening(Module module, Setting<?> except) {
        for (Setting<?> setting : module.settings()) {
            if (setting != except && setting instanceof KeybindSetting bind) {
                bind.setListening(false);
            }
        }
    }

    /** Resets transient UI state (bind listening, open dropdowns, drags) of a module's settings. */
    public static void resetConfigInteraction(Module module) {
        drag = null;
        if (module == null) {
            return;
        }
        for (Setting<?> setting : module.settings()) {
            if (setting instanceof KeybindSetting bind) {
                bind.setListening(false);
            } else if (setting instanceof ModeSetting mode) {
                mode.setOpen(false);
            }
        }
    }

    /** Maximum settings scroll in base units. */
    public static float maxSettingScroll(ClickGuiScreen screen) {
        Module module = screen.configuredModule();
        if (module == null || module.settings().isEmpty()) {
            return 0.0f;
        }
        float contentHeight = SETTINGS_TOP + Math.max(0, module.settings().size() - 1) * SETTING_GAP;
        for (Setting<?> setting : module.settings()) {
            contentHeight += settingHeight(setting);
        }
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