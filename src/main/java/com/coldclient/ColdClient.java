package com.coldclient;

import com.coldclient.gui.ClickGuiScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class ColdClient implements ClientModInitializer {

    public static final String NAME = "Cold Client";
    public static final String VERSION = "1.0.0";
    public static final String MOD_ID = "coldclient";

    private static final KeyMapping OPEN_CLICK_GUI = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key.coldclient.open_click_gui",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_RIGHT_SHIFT,
                    KeyMapping.Category.MISC
            )
    );

    @Override
    public void onInitializeClient() {
        System.out.println(NAME + " " + VERSION + " initialized.");

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_CLICK_GUI.consumeClick()) {
                if (client.gui.screen() instanceof ClickGuiScreen) {
                    client.gui.setScreen(null);
                } else {
                    client.gui.setScreen(new ClickGuiScreen());
                }
            }
        });
    }
}