package com.coldclient.gui;

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
            "Combat", "Movement", "Visuals", "Player", "Client"
    };

    private static final ClickGuiRenderer.Module[] MODULES = {
            // Combat
            new ClickGuiRenderer.Module("Combo Counter", "Tracks your current hit streak", "Combat"),
            new ClickGuiRenderer.Module("Reach Display", "Shows distance to your target", "Combat"),
            new ClickGuiRenderer.Module("Hit Indicator", "Flashes when you land a hit", "Combat"),
            new ClickGuiRenderer.Module("Target HUD", "Health and armor of your target", "Combat"),
            new ClickGuiRenderer.Module("Armor Status", "Durability of your worn armor", "Combat"),
            new ClickGuiRenderer.Module("Cooldown Indicator", "Attack cooldown near the crosshair", "Combat"),

            // Movement
            new ClickGuiRenderer.Module("Sprint", "Keeps sprinting when possible", "Movement"),
            new ClickGuiRenderer.Module("Sneak Toggle", "Toggle crouch instead of holding", "Movement"),
            new ClickGuiRenderer.Module("Speed Display", "Shows your current movement speed", "Movement"),
            new ClickGuiRenderer.Module("Jump Assist", "Movement timing controls", "Movement"),
            new ClickGuiRenderer.Module("Velocity Display", "Client velocity readout", "Movement"),

            // Visuals
            new ClickGuiRenderer.Module("Fullbright", "Brightens dark areas", "Visuals"),
            new ClickGuiRenderer.Module("Entity Outline", "Highlights selected entities", "Visuals"),
            new ClickGuiRenderer.Module("Nametags", "Improves player name visibility", "Visuals"),
            new ClickGuiRenderer.Module("No Hurt Cam", "Removes the camera shake", "Visuals"),
            new ClickGuiRenderer.Module("Particles", "Controls particle rendering", "Visuals"),
            new ClickGuiRenderer.Module("Crosshair", "Customizes the in-game crosshair", "Visuals"),
            new ClickGuiRenderer.Module("Zoom", "Smooth scope-style zoom", "Visuals"),

            // Player
            new ClickGuiRenderer.Module("Camera", "Client camera controls", "Player"),
            new ClickGuiRenderer.Module("Inventory Move", "Move while a screen is open", "Player"),
            new ClickGuiRenderer.Module("Fast Place", "Reduces client-side placement delay", "Player"),
            new ClickGuiRenderer.Module("Chat Tweaks", "Small chat quality-of-life options", "Player"),
            new ClickGuiRenderer.Module("Auto GG", "Sends a configurable message", "Player"),
            new ClickGuiRenderer.Module("Coordinates", "Display player coordinates", "Player"),

            // Client
            new ClickGuiRenderer.Module("ClickGUI", "Open and configure Cold Client", "Client"),
            new ClickGuiRenderer.Module("Notifications", "Client notification settings", "Client"),
            new ClickGuiRenderer.Module("Performance", "Client rendering options", "Client"),
            new ClickGuiRenderer.Module("Theme", "Choose GUI appearance", "Client"),
            new ClickGuiRenderer.Module("Profiles", "Manage client profiles", "Client"),
            new ClickGuiRenderer.Module("Watermark", "Cold Client watermark", "Client"),
            new ClickGuiRenderer.Module("ArrayList", "Display active modules", "Client")
    };

    private int selectedCategory = 0;
    private String search = "";
    private boolean searchFocused;
    private float scroll;
    private int mouseX;
    private int mouseY;
    private int frameId;

    public ClickGuiScreen() {
        super(Component.literal("Cold Client"));
        ClickGuiRenderer.open();
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

    /** Increments once per extracted frame; lets the renderer skip duplicate render calls. */
    public int frameId() {
        return frameId;
    }

    public String[] categories() {
        return CATEGORIES;
    }

    public ClickGuiRenderer.Module[] modules() {
        return MODULES;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Deliberately do not call super: everything (panels AND text) is drawn by NanoVG
        // from GuiRendererMixin, so nothing needs to be queued here.
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.frameId++;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        mouseX = (int) event.x();
        mouseY = (int) event.y();

        searchFocused = ClickGuiRenderer.hitSearch(this, mouseX, mouseY);
        if (searchFocused) {
            return true;
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
        scroll -= (float) scrollY * 36.0f;
        clampScroll();
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
                    clampScroll();
                }
                return true;
            }

            if ((key == GLFW.GLFW_KEY_A && (event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0)
                    || key == GLFW.GLFW_KEY_DELETE) {
                search = "";
                clampScroll();
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
            scroll = 0;
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

    /** With an empty search the selected category is shown; otherwise every module is searched. */
    public boolean matches(ClickGuiRenderer.Module module) {
        if (search.isBlank()) {
            return module.category.equals(selectedCategoryName());
        }

        String q = search.toLowerCase(Locale.ROOT).trim();
        return module.name.toLowerCase(Locale.ROOT).contains(q)
                || module.description.toLowerCase(Locale.ROOT).contains(q);
    }

    private void clampScroll() {
        scroll = Math.max(0.0f, Math.min(scroll, ClickGuiRenderer.maxScroll(this)));
    }
}
