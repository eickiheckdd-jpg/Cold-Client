package com.coldclient.setting;

import org.lwjgl.glfw.GLFW;

/**
 * Keyboard bind for a module. Value is a GLFW key code, or {@link #NONE}.
 * Place one as the FIRST setting of a module so it renders at the top of the panel.
 */
public final class KeybindSetting extends Setting<Integer> {
    public static final int NONE = GLFW.GLFW_KEY_UNKNOWN; // -1

    private boolean listening; // GUI is waiting for the next key press
    private boolean wasDown;   // edge detection for the toggle handler

    public KeybindSetting(String name, String description) {
        super(name, description, NONE);
    }

    public int key() {
        return get();
    }

    public boolean isBound() {
        return key() != NONE;
    }

    public void bind(int key) {
        set(key);
        listening = false;
    }

    public void unbind() {
        set(NONE);
        listening = false;
    }

    public boolean listening() {
        return listening;
    }

    public void setListening(boolean listening) {
        this.listening = listening;
    }

    public boolean wasDown() {
        return wasDown;
    }

    public void setWasDown(boolean wasDown) {
        this.wasDown = wasDown;
    }

    @Override
    public void cycle() {
        listening = !listening;
    }
}