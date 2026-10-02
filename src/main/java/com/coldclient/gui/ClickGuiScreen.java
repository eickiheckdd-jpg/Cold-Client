package com.coldclient.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public final class ClickGuiScreen extends Screen {
    private static final String[] CATEGORIES = {
            "Visuals", "Misc", "Player", "Client", "Movement", "Configs", "Hud"
    };

    private static final ClickGuiRenderer.Module[] MODULES = {
            // No example modules — add real ones here
    };

    private int selectedCategory = 0;
    private String search = "";
    private boolean searchFocused;
    private float scroll;
    private int mouseX;
    private int mouseY;

    public ClickGuiScreen() {
        super(Component.literal("Cold Client"));
    }

    public int selectedCategory() {
        return selectedCategory;
    }

    public String selectedCategoryName() {
        return CATEGORIES[selectedCategory];
    }

    public String search() {
        return search;
    }

    public boolean searchFocused() {
        return searchFocused;
    }

    public float scroll() {
        return scroll;
    }

    public int mouseX() {
        return mouseX;
    }

    public int mouseY() {
        return mouseY;
    }

    public String[] categories() {
        return CATEGORIES;
    }

    public ClickGuiRenderer.Module[] modules() {
        return MODULES;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Deliberately do not call super: NanoVG supplies the transparent background/panels.
        this.mouseX = mouseX;
        this.mouseY = mouseY;

        ClickGuiRenderer.extractText(this, graphics);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        mouseX = (int) event.x();
        mouseY = (int) event.y();

        if (ClickGuiRenderer.hitSearch(this, mouseX, mouseY)) {
            searchFocused = true;
            return true;
        }

        if (!ClickGuiRenderer.hitSearch(this, mouseX, mouseY)) {
            searchFocused = false;
        }

        int category = ClickGuiRenderer.hitCategory(this, mouseX, mouseY);
        if (category >= 0) {
            selectedCategory = category;
            scroll = 0;
            return true;
        }

        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int module = ClickGuiRenderer.hitModule(this, mouseX, mouseY);
            if (module >= 0) {
                MODULES[module].enabled = !MODULES[module].enabled;
                return true;
            }
        }

        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        scroll -= (float) scrollY * 32.0f;
        scroll = Math.max(0.0f, Math.min(scroll, ClickGuiRenderer.maxScroll(this)));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();

        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            onClose();
            return true;
        }

        if (searchFocused) {
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!search.isEmpty()) {
                    search = search.substring(0, search.length() - 1);
                }
                return true;
            }

            if (key == GLFW.GLFW_KEY_A && (event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
                search = "";
                return true;
            }

            if (key == GLFW.GLFW_KEY_DELETE) {
                search = "";
                return true;
            }
        }

        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!searchFocused) {
            return true;
        }

        int codepoint = event.codepoint();
        if (Character.isISOControl(codepoint)) {
            return true;
        }

        String value = new String(Character.toChars(codepoint));
        if (value.length() == 1 && search.length() < 48) {
            search += value;
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        super.onClose();
        if (minecraft != null) {
            minecraft.gui.setScreen(null);
        }
    }

    public boolean matches(ClickGuiRenderer.Module module) {
        if (!module.category.equals(selectedCategoryName())) {
            return false;
        }

        if (search.isBlank()) {
            return true;
        }

        String q = search.toLowerCase(Locale.ROOT);
        return module.name.toLowerCase(Locale.ROOT).contains(q)
                || module.description.toLowerCase(Locale.ROOT).contains(q);
    }
}