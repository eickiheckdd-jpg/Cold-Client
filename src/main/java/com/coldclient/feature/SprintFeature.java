package com.coldclient.feature;

import com.coldclient.gui.ClickGuiRenderer.Module;
import com.coldclient.module.ModuleRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public final class SprintFeature {
    /** Other modules (W-Tap) set this to briefly stop Sprint from re-sprinting. */
    public static boolean suppressed;

    private static boolean applied;

    private SprintFeature() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(SprintFeature::tick);
    }

    private static void tick(Minecraft client) {
        Module module = ModuleRegistry.get("Sprint");
        if (client.player == null || module == null) {
            return;
        }
        if (!module.enabled || suppressed) {
            if (applied) {
                client.options.keySprint.setDown(false);
                applied = false;
            }
            return;
        }
        boolean wanted = client.options.keyUp.isDown()
                && !client.player.isShiftKeyDown()
                && !client.player.isUsingItem()
                && client.gui.screen() == null;
        client.options.keySprint.setDown(wanted);
        applied = wanted;
    }
}
