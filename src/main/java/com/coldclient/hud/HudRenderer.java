package com.coldclient.hud;

import com.coldclient.ColdClient;
import com.coldclient.gui.ClickGuiRenderer.Module;
import com.coldclient.module.Cfg;
import com.coldclient.module.FriendManager;
import com.coldclient.module.ModuleRegistry;
import com.coldclient.render.GlStateGuard;
import com.coldclient.render.NanoVGRenderer;
import com.coldclient.render.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.lwjgl.nanovg.NanoVG.nvgGlobalAlpha;

/** Draws the in-game overlay (module list, watermark, notifications, ESP) and the HUD editor with NanoVG. */
public final class HudRenderer {
    private static final int ACCENT = 0x9B4DE8;
    private static final int BG = 0x0B0D14;
    private static final float TOAST_H = 24f;
    private static final float TOAST_GAP = 6f;
    private static final float TOAST_BOX_W = 190f;

    private static long lastFrame;

    private HudRenderer() {}

    // =========================================================================
    // In-game overlay
    // =========================================================================

    public static void render(Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        boolean list = Cfg.on("Module List");
        boolean mark = Cfg.on("Watermark");
        boolean toasts = Cfg.on("Notifications") && Notifications.hasActive();
        boolean esp = Cfg.on("ESP") && client.gui.screen() == null; // no boxes over menus
        if (!(list || mark || toasts || esp)) {
            return;
        }
        if (!claimFrame()) {
            return;
        }

        int w = client.getWindow().getGuiScaledWidth();
        int h = client.getWindow().getGuiScaledHeight();
        withFrame(w, h, () -> {
            if (esp) {
                drawEsp(client, w, h);
            }
            if (list) {
                drawModuleList(HudManager.MODULE_LIST, w, h, false);
            }
            if (mark) {
                drawWatermark(HudManager.WATERMARK, w, h);
            }
            if (toasts) {
                drawNotifications(HudManager.NOTIFICATIONS, w, h, false);
            }
        });
    }

    // =========================================================================
    // HUD editor
    // =========================================================================

    public static void renderEditor(HudEditorScreen screen) {
        if (!claimFrame()) {
            return;
        }
        int w = screen.width;
        int h = screen.height;
        withFrame(w, h, () -> {
            long ctx = NanoVGRenderer.getContext();
            Ui.rect(0, 0, w, h, 0, 0x04050A, 0.55f);

            Ui.text("HUD EDITOR", w * 0.5f, 28, 14f, true, 0xF2F3F8, 1.0f, Ui.ALIGN_CENTER_MIDDLE, 1.5f);
            Ui.text("Drag a block anywhere. The module list aligns to the side of the screen it is on.",
                    w * 0.5f, 48, 10.5f, false, 0xA4A9BB, 1.0f, Ui.ALIGN_CENTER_MIDDLE);

            for (HudElement e : HudManager.ALL) {
                boolean enabled = Cfg.on(e.moduleName);
                nvgGlobalAlpha(ctx, enabled ? 1.0f : 0.45f);
                switch (e.moduleName) {
                    case "Module List" -> drawModuleList(e, w, h, true);
                    case "Watermark" -> drawWatermark(e, w, h);
                    default -> drawNotifications(e, w, h, true);
                }
                nvgGlobalAlpha(ctx, 1.0f);

                float l = e.left(w);
                float t = e.top(h);
                boolean active = screen.dragging() == e;
                Ui.outline(l - 3, t - 3, e.width + 6, e.height + 6, 5, 1.2f, active ? 0xFFFFFF : ACCENT, active ? 1.0f : 0.8f);
                Ui.text(e.moduleName + (enabled ? "" : " (off)"), l - 3, t - 12, 9.5f, true,
                        active ? 0xFFFFFF : ACCENT, 1.0f, Ui.ALIGN_LEFT_MIDDLE);
            }

            float[] done = doneRect(w, h);
            float[] reset = resetRect(w, h);
            drawButton(done, "Done", ACCENT, screen.mouseX(), screen.mouseY());
            drawButton(reset, "Reset", 0x5A6075, screen.mouseX(), screen.mouseY());
        });
    }

    public static boolean hitDone(HudEditorScreen screen, float mx, float my) {
        return inside(doneRect(screen.width, screen.height), mx, my);
    }

    public static boolean hitReset(HudEditorScreen screen, float mx, float my) {
        return inside(resetRect(screen.width, screen.height), mx, my);
    }

    private static float[] doneRect(int w, int h) {
        return new float[]{w * 0.5f - 100, h - 62, 92, 30};
    }

    private static float[] resetRect(int w, int h) {
        return new float[]{w * 0.5f + 8, h - 62, 92, 30};
    }

    private static boolean inside(float[] r, float mx, float my) {
        return mx >= r[0] && mx <= r[0] + r[2] && my >= r[1] && my <= r[1] + r[3];
    }

    private static void drawButton(float[] r, String label, int color, float mx, float my) {
        boolean hover = inside(r, mx, my);
        Ui.rect(r[0], r[1], r[2], r[3], 8, color, hover ? 0.95f : 0.75f);
        Ui.text(label, r[0] + r[2] * 0.5f, r[1] + r[3] * 0.5f, 12f, true, 0xFFFFFF, 1.0f, Ui.ALIGN_CENTER_MIDDLE);
    }

    // =========================================================================
    // Module list
    // =========================================================================

    private static void drawModuleList(HudElement el, float w, float h, boolean preview) {
        Module m = ModuleRegistry.get("Module List");
        float scale = (float) Cfg.num(m, "Size") / 100f;
        float size = 9.5f * scale;
        float rowH = size + 7f * scale;
        float padX = 5f * scale;
        float bar = Cfg.bool(m, "Accent Bar") ? 2f * scale : 0f;
        boolean background = Cfg.bool(m, "Background");
        int color = Cfg.color(m, "Text Color");

        List<String> names = new ArrayList<>();
        for (Module other : ModuleRegistry.ALL) {
            if (other.enabled && !other.name.equals("HUD Editor")) {
                names.add(other.name);
            }
        }
        if (names.isEmpty() && preview) {
            names = new ArrayList<>(List.of("Triggerbot", "Aim Assist", "Sprint", "ESP"));
        }
        switch (Cfg.mode(m, "Sort")) {
            case "Shortest First" -> names.sort(Comparator.comparingDouble(n -> Ui.textWidth(n, size, false)));
            case "Alphabetical" -> names.sort(String.CASE_INSENSITIVE_ORDER);
            default -> names.sort(Comparator.comparingDouble((String n) -> Ui.textWidth(n, size, false)).reversed());
        }

        float maxW = 0f;
        float[] widths = new float[names.size()];
        for (int i = 0; i < widths.length; i++) {
            widths[i] = Ui.textWidth(names.get(i), size, false) + padX * 2f + bar;
            maxW = Math.max(maxW, widths[i]);
        }
        if (names.isEmpty()) {
            maxW = 60f;
        }
        el.width = maxW;
        el.height = Math.max(rowH, names.size() * rowH);

        float left = el.left(w);
        float top = el.top(h);
        for (int i = 0; i < names.size(); i++) {
            float rw = widths[i];
            float x = el.alignRight ? left + maxW - rw : left;
            float y = top + i * rowH;
            if (background) {
                Ui.rect(x, y, rw, rowH, 0, BG, 0.55f);
            }
            if (bar > 0f) {
                Ui.rect(el.alignRight ? x + rw - bar : x, y, bar, rowH, 0, color, 1.0f);
            }
            float textX = el.alignRight ? x + padX : x + bar + padX;
            Ui.text(names.get(i), textX, y + rowH * 0.5f, size, false, color, 1.0f, Ui.ALIGN_LEFT_MIDDLE);
        }
    }

    // =========================================================================
    // Watermark
    // =========================================================================

    private static void drawWatermark(HudElement el, float w, float h) {
        Module m = ModuleRegistry.get("Watermark");
        float scale = (float) Cfg.num(m, "Size") / 100f;
        float size = 12f * scale;
        float padX = 8f * scale;
        float boxH = size + 12f * scale;
        int color = Cfg.color(m, "Text Color");

        String name = ColdClient.NAME;
        String version = Cfg.bool(m, "Show Version") ? " " + ColdClient.VERSION : "";
        float nameW = Ui.textWidth(name, size, true);
        float versionW = Ui.textWidth(version, size * 0.8f, false);
        float boxW = nameW + versionW + padX * 2f;

        el.width = boxW;
        el.height = boxH;
        float x = el.left(w);
        float y = el.top(h);

        if (Cfg.bool(m, "Background")) {
            Ui.rect(x, y, boxW, boxH, 5f * scale, BG, 0.60f);
            Ui.rect(x + 4f * scale, y + boxH - 2f * scale, boxW - 8f * scale, 1.5f * scale, 0, color, 0.9f);
        }
        Ui.text(name, x + padX, y + boxH * 0.5f, size, true, color, 1.0f, Ui.ALIGN_LEFT_MIDDLE);
        if (!version.isEmpty()) {
            Ui.text(version, x + padX + nameW, y + boxH * 0.5f + 0.5f, size * 0.8f, false,
                    0xA4A9BB, 1.0f, Ui.ALIGN_LEFT_MIDDLE);
        }
    }

    // =========================================================================
    // Notifications
    // =========================================================================

    private static void drawNotifications(HudElement el, float w, float h, boolean preview) {
        long ctx = NanoVGRenderer.getContext();
        List<Notifications.Toast> toasts = Notifications.active();
        boolean sample = preview && toasts.isEmpty();

        int count = sample ? 3 : toasts.size();
        el.width = TOAST_BOX_W;
        el.height = Math.max(TOAST_H, count * (TOAST_H + TOAST_GAP) - TOAST_GAP);

        float left = el.left(w);
        float top = el.top(h);
        float life = Notifications.durationSeconds();
        long now = System.nanoTime();
        String[] samples = {"Triggerbot enabled", "Sprint enabled", "ESP disabled"};

        for (int i = 0; i < count; i++) {
            String text;
            float alpha = 1f;
            float slide = 1f;
            if (sample) {
                text = samples[i];
            } else {
                Notifications.Toast toast = toasts.get(i);
                text = toast.text;
                float age = (now - toast.createdNanos) / 1_000_000_000.0f;
                float in = Math.min(1f, age / 0.25f);
                slide = 1f - (1f - in) * (1f - in) * (1f - in);
                float remaining = life - age;
                alpha = remaining < 0.4f ? Math.max(0f, remaining / 0.4f) : 1f;
            }

            float size = 10f;
            float tw = Ui.textWidth(text, size, false) + 26f;
            float tx = el.alignRight ? left + TOAST_BOX_W - tw : left;
            tx += (el.alignRight ? 1f : -1f) * (1f - slide) * 40f;
            float ty = top + i * (TOAST_H + TOAST_GAP);

            nvgGlobalAlpha(ctx, alpha * (sample ? 1f : 1f));
            Ui.rect(tx, ty, tw, TOAST_H, 6f, BG, 0.80f);
            Ui.rect(tx, ty + 4f, 2.5f, TOAST_H - 8f, 1.25f, ACCENT, 1.0f);
            Ui.text(text, tx + 13f, ty + TOAST_H * 0.5f, size, false, 0xF2F3F8, 1.0f, Ui.ALIGN_LEFT_MIDDLE);
            nvgGlobalAlpha(ctx, 1.0f);
        }
    }

    // =========================================================================
    // ESP (2D boxes from a manual world-to-screen projection)
    // =========================================================================

    private static void drawEsp(Minecraft client, int w, int h) {
        Module m = ModuleRegistry.get("ESP");
        LocalPlayer self = client.player;
        float partial = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        Vec3 eye = self.getEyePosition(partial);
        double yaw = Math.toRadians(self.getViewYRot(partial));
        double pitch = Math.toRadians(self.getViewXRot(partial));
        double fx = -Math.sin(yaw) * Math.cos(pitch);
        double fy = -Math.sin(pitch);
        double fz = Math.cos(yaw) * Math.cos(pitch);
        double rx = -Math.cos(yaw);
        double rz = -Math.sin(yaw);
        double ux = -rz * fy;
        double uy = rz * fx - rx * fz;
        double uz = rx * fy;

        double fov = client.options.fov().get();
        if (Cfg.bool(m, "FOV Compensation")) {
            fov *= fovModifier(client, self);
        }
        double focal = (h * 0.5) / Math.tan(Math.toRadians(fov) * 0.5);

        double maxDist = Cfg.num(m, "Max Distance");
        boolean fill = Cfg.bool(m, "Fill");
        float fillAlpha = (float) Cfg.num(m, "Fill Opacity") / 100f;
        float line = (float) Cfg.num(m, "Line Width");
        int boxColor = Cfg.color(m, "Box Color");
        int friendColor = Cfg.color(m, "Friend Color");
        boolean highlightFriends = Cfg.bool(m, "Highlight Friends");

        for (Player p : client.level.players()) {
            if (p == self || !p.isAlive() || p.isSpectator()) {
                continue;
            }
            Vec3 pos = p.getPosition(partial);
            if (pos.distanceTo(eye) > maxDist) {
                continue;
            }

            double hw = p.getBbWidth() * 0.5;
            double height = p.getBbHeight();
            double minX = Double.MAX_VALUE;
            double minY = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE;
            double maxY = -Double.MAX_VALUE;
            boolean visible = true;

            for (int i = 0; i < 8; i++) {
                double dx = pos.x() + ((i & 1) == 0 ? -hw : hw) - eye.x();
                double dy = pos.y() + ((i & 2) == 0 ? 0.0 : height) - eye.y();
                double dz = pos.z() + ((i & 4) == 0 ? -hw : hw) - eye.z();
                double depth = dx * fx + dy * fy + dz * fz;
                if (depth < 0.1) {
                    visible = false; // a corner is behind the camera
                    break;
                }
                double sx = w * 0.5 + ((dx * rx + dz * rz) / depth) * focal;
                double sy = h * 0.5 - ((dx * ux + dy * uy + dz * uz) / depth) * focal;
                minX = Math.min(minX, sx);
                maxX = Math.max(maxX, sx);
                minY = Math.min(minY, sy);
                maxY = Math.max(maxY, sy);
            }
            if (!visible || maxX - minX > w * 4 || maxY - minY > h * 4) {
                continue;
            }

            int color = highlightFriends && FriendManager.isFriend(p) ? friendColor : boxColor;
            float bx = (float) minX;
            float by = (float) minY;
            float bw = (float) (maxX - minX);
            float bh = (float) (maxY - minY);
            if (fill) {
                Ui.rect(bx, by, bw, bh, 0, color, fillAlpha);
            }
            Ui.outline(bx, by, bw, bh, 0, line, color, 1.0f);
        }
    }

    /** Approximates vanilla's dynamic FOV (sprinting / speed effects) so boxes stay on the player. */
    private static double fovModifier(Minecraft client, LocalPlayer self) {
        double speed = self.getAttributeValue(Attributes.MOVEMENT_SPEED);
        double f = (speed / 0.1 + 1.0) / 2.0;
        double effectScale = client.options.fovEffectScale().get();
        return 1.0 + (f - 1.0) * effectScale;
    }

    // =========================================================================
    // Frame plumbing
    // =========================================================================

    /** GuiRenderer.render can run more than once per frame; draw once. */
    private static boolean claimFrame() {
        long now = System.nanoTime();
        if (now - lastFrame < 1_000_000L) {
            return false;
        }
        lastFrame = now;
        return true;
    }

    private static void withFrame(int w, int h, Runnable draw) {
        GlStateGuard.save();
        try {
            GlStateGuard.prepareForNanoVG();
            if (!NanoVGRenderer.initialize()) {
                return;
            }
            Ui.ensureFonts();
            NanoVGRenderer.beginFrame(w, h, pixelRatio(w));
            if (!NanoVGRenderer.isFrameActive()) {
                return;
            }
            draw.run();
            NanoVGRenderer.endFrame();
        } finally {
            NanoVGRenderer.cancelFrame();
            GlStateGuard.restore();
        }
    }

    private static float pixelRatio(int guiWidth) {
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        if (viewport[2] <= 0 || guiWidth <= 0) {
            return 1.0f;
        }
        return Math.max(1.0f, Math.min(viewport[2] / (float) guiWidth, 8.0f));
    }
}
