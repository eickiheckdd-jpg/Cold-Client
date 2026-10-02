package com.coldclient.gui;

import com.coldclient.module.ModuleRegistry;
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


    private int selectedCategory = 0;
    private String search = "";
    private boolean searchFocused;
    private float scroll;
    private float settingScroll;
    private int configuredModuleIndex = -1;
    private int frameId;
    private int mouseX;
    private int mouseY;

    public ClickGuiScreen() {
        super(Component.literal("Cold Client"));
        ClickGuiRenderer.open();
    }

    public int selectedCategory() {
        return selectedCategory;
    }

    public String selectedCategoryName() {
        return ModuleRegistry.CATEGORIES[selectedCategory];
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

    public float settingScroll() {
        return settingScroll;
    }

    public int frameId() {
        return frameId;
    }

    public boolean isConfiguring() {
        return configuredModuleIndex >= 0 && configuredModuleIndex < ModuleRegistry.ALL.length;
    }

    public ClickGuiRenderer.Module configuredModule() {
        return isConfiguring() ? ModuleRegistry.ALL[configuredModuleIndex] : null;
    }

    public int configuredModuleIndex() {
        return configuredModuleIndex;
    }

    public int mouseX() {
        return mouseX;
    }

    public int mouseY() {
        return mouseY;
    }

    public String[] categories() {
        return ModuleRegistry.CATEGORIES;
    }

    public ClickGuiRenderer.Module[] modules() {
        return ModuleRegistry.ALL;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Deliberately do not call super: NanoVG supplies the transparent background/panels.
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.frameId++;

    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        mouseX = (int) event.x();
        mouseY = (int) event.y();
        int button = event.button();

        if (isConfiguring()) {
            if (ClickGuiRenderer.hitConfigBack(this, mouseX, mouseY)) {
                closeConfig();
                return true;
            }

            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                if (ClickGuiRenderer.applySettingClick(this, mouseX, mouseY)) {
                    return true;
                }
            }

            return true;
        }

        if (ClickGuiRenderer.hitSearch(this, mouseX, mouseY)) {
            searchFocused = true;
            return true;
        }

        searchFocused = false;

        int category = ClickGuiRenderer.hitCategory(this, mouseX, mouseY);
        if (category >= 0) {
            selectedCategory = category;
            scroll = 0;
            return true;
        }

        int module = ClickGuiRenderer.hitModule(this, mouseX, mouseY);
        if (module >= 0) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                ModuleRegistry.ALL[module].enabled = !ModuleRegistry.ALL[module].enabled;
                return true;
            }
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                openConfig(module);
                return true;
            }
        }

        return true;
    }

    private void openConfig(int moduleIndex) {
        if (moduleIndex < 0 || moduleIndex >= ModuleRegistry.ALL.length) {
            return;
        }
        configuredModuleIndex = moduleIndex;
        settingScroll = 0.0f;
        searchFocused = false;
    }

    private void closeConfig() {
        configuredModuleIndex = -1;
        settingScroll = 0.0f;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (isConfiguring()) {
            settingScroll -= (float) scrollY * 28.0f;
            settingScroll = Math.max(0.0f, Math.min(settingScroll, ClickGuiRenderer.maxSettingScroll(this)));
        } else {
            scroll -= (float) scrollY * 32.0f;
            scroll = Math.max(0.0f, Math.min(scroll, ClickGuiRenderer.maxScroll(this)));
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();

        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            onClose();
            return true;
        }

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (isConfiguring()) {
                closeConfig();
            } else {
                onClose();
            }
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