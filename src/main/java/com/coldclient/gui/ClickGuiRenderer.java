package com.coldclient.gui;

import com.coldclient.render.NanoVGRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class ClickGuiRenderer {
    private ClickGuiRenderer() {}

    private static final float BASE_W = 820.0f;
    private static final float BASE_H = 520.0f;
    private static final float SIDEBAR_W = 154.0f;

    private static final float PANEL_R = 14.0f;
    private static final float CARD_R = 9.0f;

    private static float openProgress = 0.0f;
    private static final float[] categoryHover = new float[7];
    private static final float[] moduleHover = new float[64];

    public static final class Module {
        public final String name;
        public final String description;
        public final String category;
        public boolean enabled;

        public Module(String name, String description, String category) {
            this.name = name;
            this.description = description;
            this.category = category;
        }
    }

    public static void render(ClickGuiScreen screen) {
        if (!NanoVGRenderer.initialize()) {
            return;
        }

        float target = 1.0f;
        openProgress += (target - openProgress) * 0.22f;
        if (openProgress < 0.001f) {
            return;
        }

        float width = screen.width;
        float height = screen.height;

        float scale = Math.min(width / BASE_W, height / BASE_H);
        scale = Math.max(0.72f, Math.min(scale, 1.55f));

        float panelW = BASE_W * scale;
        float panelH = BASE_H * scale;
        float panelX = (width - panelW) * 0.5f;
        float panelY = (height - panelH) * 0.5f;

        float alpha = easeOutCubic(openProgress);

        NanoVGRenderer.beginFrame(width, height, 1.0f);

        // World dimmer.
        rect(0, 0, width, height, 0, 0, 0, 0.34f * alpha);

        // Very subtle center glow.
        NanoVGRenderer.drawLinearGradient(
                panelX - 90, panelY - 70, panelW + 180, panelH + 140,
                0.45f, 0.10f, 0.65f, 0.045f * alpha,
                0.08f, 0.12f, 0.28f, 0.0f,
                0.0f
        );

        // Main shell.
        NanoVGRenderer.drawShadowedRoundedRect(
                panelX, panelY, panelW, panelH,
                PANEL_R * scale, 24.0f * scale, 0.30f * alpha,
                0.035f, 0.045f, 0.075f, 0.94f
        );

        NanoVGRenderer.drawRoundedOutline(
                panelX, panelY, panelW, panelH,
                PANEL_R * scale, 1.0f * scale,
                0.35f, 0.38f, 0.48f, 0.13f * alpha
        );

        // Sidebar.
        float sidebarX = panelX;
        float sidebarY = panelY;
        float sidebarW = SIDEBAR_W * scale;
        float sidebarH = panelH;

        NanoVGRenderer.drawRoundedRect(
                sidebarX, sidebarY, sidebarW, sidebarH,
                PANEL_R * scale,
                0.045f, 0.050f, 0.080f, 0.98f
        );

        // Search bar.
        float searchX = panelX + sidebarW + 22.0f * scale;
        float searchY = panelY + 20.0f * scale;
        float searchW = panelW - sidebarW - 42.0f * scale;
        float searchH = 38.0f * scale;

        NanoVGRenderer.drawShadowedRoundedRect(
                searchX, searchY, searchW, searchH,
                9.0f * scale, 9.0f * scale, 0.12f * alpha,
                0.018f, 0.024f, 0.045f, 0.94f
        );

        NanoVGRenderer.drawRoundedOutline(
                searchX, searchY, searchW, searchH,
                9.0f * scale, 1.0f * scale,
                0.34f, 0.37f, 0.48f, screen.searchFocused() ? 0.30f : 0.10f
        );

        // Sidebar category highlights.
        float categoryTop = panelY + 80.0f * scale;
        for (int i = 0; i < screen.categories().length; i++) {
            boolean selected = i == screen.selectedCategory();
            boolean hovered = inside(
                    screen.mouseX(), screen.mouseY(),
                    panelX + 10 * scale, categoryTop + i * 38 * scale,
                    sidebarW - 20 * scale, 32 * scale
            );

            categoryHover[i] += ((hovered ? 1.0f : 0.0f) - categoryHover[i]) * 0.18f;

            if (selected || categoryHover[i] > 0.01f) {
                float t = selected ? 1.0f : categoryHover[i];
                NanoVGRenderer.drawRoundedRect(
                        panelX + 10 * scale,
                        categoryTop + i * 38 * scale,
                        sidebarW - 20 * scale,
                        32 * scale,
                        8 * scale,
                        0.42f, 0.10f, 0.55f,
                        (selected ? 0.86f : 0.24f) * t
                );
            }
        }

        // Content cards.
        float contentX = searchX;
        float contentY = searchY + searchH + 18.0f * scale;
        float contentW = searchW;
        float cardGap = 10.0f * scale;
        float colW = (contentW - cardGap) * 0.5f;
        float cardH = 58.0f * scale;

        List<Integer> visible = visibleModules(screen);
        float yOffset = -screen.scroll() * scale;

        for (int row = 0; row * 2 < visible.size(); row++) {
            for (int col = 0; col < 2; col++) {
                int listIndex = row * 2 + col;
                if (listIndex >= visible.size()) continue;

                int moduleIndex = visible.get(listIndex);
                Module module = screen.modules()[moduleIndex];

                float x = contentX + col * (colW + cardGap);
                float y = contentY + row * (cardH + cardGap) + yOffset;

                if (y + cardH < contentY - 10 || y > panelY + panelH - 12) {
                    continue;
                }

                boolean hovered = inside(screen.mouseX(), screen.mouseY(), x, y, colW, cardH);
                moduleHover[moduleIndex] += ((hovered ? 1.0f : 0.0f) - moduleHover[moduleIndex]) * 0.20f;

                float hover = moduleHover[moduleIndex];

                float br = module.enabled ? 0.10f : 0.022f;
                float bg = module.enabled ? 0.035f : 0.028f;
                float bb = module.enabled ? 0.15f : 0.052f;
                float ba = 0.94f + hover * 0.04f;

                NanoVGRenderer.drawRoundedRect(
                        x, y, colW, cardH,
                        CARD_R * scale,
                        br, bg, bb, ba
                );

                if (module.enabled) {
                    NanoVGRenderer.drawLinearGradient(
                            x, y, colW, cardH,
                            0.55f, 0.12f, 0.75f, 0.16f,
                            0.12f, 0.16f, 0.45f, 0.02f,
                            0.0f
                    );
                }

                NanoVGRenderer.drawRoundedOutline(
                        x, y, colW, cardH,
                        CARD_R * scale, 1.0f * scale,
                        0.36f, 0.38f, 0.48f,
                        (0.09f + hover * 0.10f) * alpha
                );
            }
        }

        // Clip isn't necessary because cards are culled before drawing.
        NanoVGRenderer.endFrame();
    }

    public static void extractText(ClickGuiScreen screen, GuiGraphicsExtractor graphics) {
        float width = screen.width;
        float height = screen.height;

        float scale = Math.min(width / BASE_W, height / BASE_H);
        scale = Math.max(0.72f, Math.min(scale, 1.55f));

        float panelW = BASE_W * scale;
        float panelH = BASE_H * scale;
        float panelX = (width - panelW) * 0.5f;
        float panelY = (height - panelH) * 0.5f;

        float sidebarW = SIDEBAR_W * scale;
        float searchX = panelX + sidebarW + 22.0f * scale;
        float searchY = panelY + 20.0f * scale;
        float searchW = panelW - sidebarW - 42.0f * scale;
        float searchH = 38.0f * scale;

        // Search icon + search text.
        graphics.text(screen.getFont(), Component.literal("⌕"),
                (int) (searchX + 13 * scale), (int) (searchY + 9 * scale),
                0xFF8D93A8, false);

        String query = screen.search().isEmpty() ? "Search for any module or feature" : screen.search();
        int searchColor = screen.search().isEmpty() ? 0xFF6E7486 : 0xFFE9EAF0;
        graphics.text(screen.getFont(), Component.literal(query),
                (int) (searchX + 32 * scale), (int) (searchY + 11 * scale),
                searchColor, false);

        // Sidebar labels.
        float categoryTop = panelY + 80.0f * scale;
        for (int i = 0; i < screen.categories().length; i++) {
            int color = i == screen.selectedCategory() ? 0xFFF4F0F7 : 0xFFD1D3DC;
            graphics.text(
                    screen.getFont(),
                    Component.literal(screen.categories()[i]),
                    (int) (panelX + 43 * scale),
                    (int) (categoryTop + i * 38 * scale + 9 * scale),
                    color,
                    false
            );
        }

        // Tiny category markers; these stay readable with the vanilla font.
        for (int i = 0; i < screen.categories().length; i++) {
            graphics.text(
                    screen.getFont(),
                    Component.literal(i == screen.selectedCategory() ? "•" : "·"),
                    (int) (panelX + 19 * scale),
                    (int) (categoryTop + i * 38 * scale + 9 * scale),
                    i == screen.selectedCategory() ? 0xFFF0B4FF : 0xFF777D90,
                    false
            );
        }

        float contentX = searchX;
        float contentY = searchY + searchH + 18.0f * scale;
        float contentW = searchW;
        float cardGap = 10.0f * scale;
        float colW = (contentW - cardGap) * 0.5f;
        float cardH = 58.0f * scale;
        float yOffset = -screen.scroll() * scale;

        List<Integer> visible = visibleModules(screen);
        for (int row = 0; row * 2 < visible.size(); row++) {
            for (int col = 0; col < 2; col++) {
                int listIndex = row * 2 + col;
                if (listIndex >= visible.size()) continue;

                int moduleIndex = visible.get(listIndex);
                Module module = screen.modules()[moduleIndex];

                float x = contentX + col * (colW + cardGap);
                float y = contentY + row * (cardH + cardGap) + yOffset;

                if (y + cardH < contentY - 10 || y > panelY + panelH - 12) {
                    continue;
                }

                int titleColor = module.enabled ? 0xFFF3E9FA : 0xFFE4E5EA;
                int descColor = 0xFF777D8E;

                graphics.text(
                        screen.getFont(),
                        Component.literal(module.name),
                        (int) (x + 11 * scale),
                        (int) (y + 9 * scale),
                        titleColor,
                        false
                );

                graphics.text(
                        screen.getFont(),
                        Component.literal(module.description),
                        (int) (x + 11 * scale),
                        (int) (y + 29 * scale),
                        descColor,
                        false
                );

                if (module.enabled) {
                    graphics.text(
                            screen.getFont(),
                            Component.literal("ON"),
                            (int) (x + colW - 23 * scale),
                            (int) (y + 9 * scale),
                            0xFFE4B7FF,
                            false
                    );
                }
            }
        }
    }

    public static boolean hitSearch(ClickGuiScreen screen, int mx, int my) {
        Bounds b = bounds(screen);
        return inside(mx, my, b.searchX, b.searchY, b.searchW, b.searchH);
    }

    public static int hitCategory(ClickGuiScreen screen, int mx, int my) {
        Bounds b = bounds(screen);
        for (int i = 0; i < screen.categories().length; i++) {
            if (inside(mx, my, b.panelX + 10 * b.scale,
                    b.categoryTop + i * 38 * b.scale,
                    b.sidebarW - 20 * b.scale,
                    32 * b.scale)) {
                return i;
            }
        }
        return -1;
    }

    public static int hitModule(ClickGuiScreen screen, int mx, int my) {
        Bounds b = bounds(screen);
        List<Integer> visible = visibleModules(screen);

        float yOffset = -screen.scroll() * b.scale;
        for (int row = 0; row * 2 < visible.size(); row++) {
            for (int col = 0; col < 2; col++) {
                int listIndex = row * 2 + col;
                if (listIndex >= visible.size()) continue;

                int moduleIndex = visible.get(listIndex);

                float x = b.searchX + col * (b.colW + b.gap);
                float y = b.contentY + row * (b.cardH + b.gap) + yOffset;

                if (inside(mx, my, x, y, b.colW, b.cardH)) {
                    return moduleIndex;
                }
            }
        }
        return -1;
    }

    public static float maxScroll(ClickGuiScreen screen) {
        Bounds b = bounds(screen);
        int rows = (visibleModules(screen).size() + 1) / 2;
        float contentHeight = rows * (b.cardH + b.gap) - b.gap;
        float available = b.panelH - (b.contentY - b.panelY) - 12 * b.scale;
        return Math.max(0.0f, contentHeight - available) / b.scale;
    }

    private static List<Integer> visibleModules(ClickGuiScreen screen) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < screen.modules().length; i++) {
            if (screen.matches(screen.modules()[i])) {
                result.add(i);
            }
        }
        return result;
    }

    private static Bounds bounds(ClickGuiScreen screen) {
        float width = screen.width;
        float height = screen.height;

        float scale = Math.min(width / BASE_W, height / BASE_H);
        scale = Math.max(0.72f, Math.min(scale, 1.55f));

        float panelW = BASE_W * scale;
        float panelH = BASE_H * scale;
        float panelX = (width - panelW) * 0.5f;
        float panelY = (height - panelH) * 0.5f;

        float sidebarW = SIDEBAR_W * scale;
        float searchX = panelX + sidebarW + 22.0f * scale;
        float searchY = panelY + 20.0f * scale;
        float searchW = panelW - sidebarW - 42.0f * scale;
        float searchH = 38.0f * scale;

        float contentY = searchY + searchH + 18.0f * scale;
        float gap = 10.0f * scale;
        float colW = (searchW - gap) * 0.5f;
        float cardH = 58.0f * scale;

        return new Bounds(panelX, panelY, panelW, panelH, sidebarW, scale,
                searchX, searchY, searchW, searchH,
                panelY + 80.0f * scale, contentY, gap, colW, cardH);
    }

    private static boolean inside(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static float easeOutCubic(float x) {
        float inv = 1.0f - x;
        return 1.0f - inv * inv * inv;
    }

    private static void rect(float x, float y, float w, float h, float r, float g, float b, float a) {
        NanoVGRenderer.drawRoundedRect(x, y, w, h, 0, r, g, b, a);
    }

    private record Bounds(
            float panelX, float panelY, float panelW, float panelH,
            float sidebarW, float scale,
            float searchX, float searchY, float searchW, float searchH,
            float categoryTop, float contentY,
            float gap, float colW, float cardH
    ) {}
}