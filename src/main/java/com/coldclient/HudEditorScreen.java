package com.coldclient.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Drag the HUD blocks where you want them. Drawn by HudRenderer.renderEditor via the GUI render mixin. */
public final class HudEditorScreen extends Screen {
    private HudElement dragging;
    private float grabX;
    private float grabY;
    private int mouseX;
    private int mouseY;

    public HudEditorScreen() {
        super(Component.literal("HUD Editor"));
    }

    public HudElement dragging() {
        return dragging;
    }

    public int mouseX() {
        return mouseX;
    }

    public int mouseY() {
        return mouseY;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Deliberately do not call super: NanoVG draws everything.
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        float mx = (float) event.x();
        float my = (float) event.y();
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return true;
        }

        if (HudRenderer.hitDone(this, mx, my)) {
            onClose();
            return true;
        }
        if (HudRenderer.hitReset(this, mx, my)) {
            HudManager.resetAll();
            return true;
        }

        // Last drawn = on top, so test in reverse.
        for (int i = HudManager.ALL.length - 1; i >= 0; i--) {
            HudElement e = HudManager.ALL[i];
            float l = e.left(width);
            float t = e.top(height);
            if (mx >= l && mx <= l + e.width && my >= t && my <= t + e.height) {
                dragging = e;
                grabX = mx - l;
                grabY = my - t;
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging != null) {
            dragging.moveTo((float) event.x() - grabX, (float) event.y() - grabY, width, height);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = null;
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE || event.key() == GLFW.GLFW_KEY_ENTER) {
            onClose();
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        dragging = null;
        HudManager.save();
        super.onClose();
        if (minecraft != null) {
            minecraft.gui.setScreen(null);
        }
    }
          }
