package com.coldclient.module;

import com.coldclient.gui.ClickGuiRenderer.Module;
import com.coldclient.setting.KeybindSetting;
import com.coldclient.setting.Setting;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Toggles modules when their {@link KeybindSetting} key is pressed in-game.
 * Uses GLFW directly (via the current context) so it doesn't depend on Minecraft input API names.
 */
public final class KeybindManager {
    private KeybindManager() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(KeybindManager::tick);
    }

    private static void tick(Minecraft client) {
        long window = GLFW.glfwGetCurrentContext();
        if (window == 0L) {
            return;
        }

        // Any screen open (GUI, chat, inventory...) means keys are for typing, not toggling.
        boolean inGame = client.player != null && client.gui.screen() == null;

        for (Module module : ModuleRegistry.ALL) {
            for (Setting<?> setting : module.settings()) {
                if (!(setting instanceof KeybindSetting bind) || !bind.isBound()) {
                    continue;
                }
                boolean down = GLFW.glfwGetKey(window, bind.key()) == GLFW.GLFW_PRESS;
                // Always track the state so a key still held when a screen closes doesn't fire.
                if (down && !bind.wasDown() && inGame) {
                    module.enabled = !module.enabled;
                }
                bind.setWasDown(down);
            }
        }
    }
}