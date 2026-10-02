package com.coldclient.module;

import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/** Human-readable names for GLFW key codes (pure LWJGL, no Minecraft API). */
public final class KeyNames {
    private KeyNames() {}

    public static String of(int key) {
        if (key == GLFW.GLFW_KEY_UNKNOWN) {
            return "None";
        }
        if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) {
            return String.valueOf((char) key);
        }
        if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) {
            return String.valueOf((char) key);
        }
        if (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F25) {
            return "F" + (key - GLFW.GLFW_KEY_F1 + 1);
        }
        if (key >= GLFW.GLFW_KEY_KP_0 && key <= GLFW.GLFW_KEY_KP_9) {
            return "Numpad " + (key - GLFW.GLFW_KEY_KP_0);
        }
        switch (key) {
            case GLFW.GLFW_KEY_SPACE: return "Space";
            case GLFW.GLFW_KEY_TAB: return "Tab";
            case GLFW.GLFW_KEY_ENTER: return "Enter";
            case GLFW.GLFW_KEY_BACKSPACE: return "Backspace";
            case GLFW.GLFW_KEY_INSERT: return "Insert";
            case GLFW.GLFW_KEY_DELETE: return "Delete";
            case GLFW.GLFW_KEY_HOME: return "Home";
            case GLFW.GLFW_KEY_END: return "End";
            case GLFW.GLFW_KEY_PAGE_UP: return "Page Up";
            case GLFW.GLFW_KEY_PAGE_DOWN: return "Page Down";
            case GLFW.GLFW_KEY_UP: return "Up";
            case GLFW.GLFW_KEY_DOWN: return "Down";
            case GLFW.GLFW_KEY_LEFT: return "Left";
            case GLFW.GLFW_KEY_RIGHT: return "Right";
            case GLFW.GLFW_KEY_LEFT_SHIFT: return "Left Shift";
            case GLFW.GLFW_KEY_RIGHT_SHIFT: return "Right Shift";
            case GLFW.GLFW_KEY_LEFT_CONTROL: return "Left Ctrl";
            case GLFW.GLFW_KEY_RIGHT_CONTROL: return "Right Ctrl";
            case GLFW.GLFW_KEY_LEFT_ALT: return "Left Alt";
            case GLFW.GLFW_KEY_RIGHT_ALT: return "Right Alt";
            case GLFW.GLFW_KEY_CAPS_LOCK: return "Caps Lock";
            case GLFW.GLFW_KEY_GRAVE_ACCENT: return "`";
            case GLFW.GLFW_KEY_MINUS: return "-";
            case GLFW.GLFW_KEY_EQUAL: return "=";
            case GLFW.GLFW_KEY_LEFT_BRACKET: return "[";
            case GLFW.GLFW_KEY_RIGHT_BRACKET: return "]";
            case GLFW.GLFW_KEY_BACKSLASH: return "\\";
            case GLFW.GLFW_KEY_SEMICOLON: return ";";
            case GLFW.GLFW_KEY_APOSTROPHE: return "'";
            case GLFW.GLFW_KEY_COMMA: return ",";
            case GLFW.GLFW_KEY_PERIOD: return ".";
            case GLFW.GLFW_KEY_SLASH: return "/";
            default: break;
        }
        String name = GLFW.glfwGetKeyName(key, 0);
        if (name != null && !name.isEmpty()) {
            return name.toUpperCase(Locale.ROOT);
        }
        return "Key " + key;
    }
}