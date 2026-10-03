package com.coldclient.feature;

import com.coldclient.gui.ClickGuiRenderer.Module;
import com.coldclient.hud.Notifications;
import com.coldclient.module.Cfg;
import com.coldclient.module.ModuleRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * One-shot combo: throws an ender pearl, then a wind charge after a random delay, then turns itself off.
 * Both items must be in the hotbar. Bind it to a key and tap.
 */
public final class PearlCatchFeature {
    private static int stage; // 0 = just enabled, 1 = waiting to throw pearl, 2 = waiting to throw wind, 3 = restoring
    private static long at;
    private static int origSlot;
    private static int pearlSlot;
    private static int windSlot;
    private static float origPitch;
    private static boolean pitchChanged;

    private PearlCatchFeature() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(PearlCatchFeature::tick);
    }

    private static long ns(long millis) {
        return millis * 1_000_000L;
    }

    private static void tick(Minecraft client) {
        Module module = ModuleRegistry.get("Pearl Catch");
        if (module == null || client.player == null || client.gameMode == null) {
            stage = 0;
            return;
        }
        LocalPlayer player = client.player;
        long now = System.nanoTime();

        if (!module.enabled) {
            if (stage > 0) {
                restore(player, module);
            }
            stage = 0;
            return;
        }

        switch (stage) {
            case 0 -> {
                if (client.gui.screen() != null) {
                    fail(module, "Close the menu first");
                    return;
                }
                pearlSlot = findInHotbar(player, Items.ENDER_PEARL);
                windSlot = findInHotbar(player, Items.WIND_CHARGE);
                if (pearlSlot < 0 || windSlot < 0) {
                    fail(module, "Put an ender pearl and a wind charge in your hotbar");
                    return;
                }
                origSlot = player.getInventory().getSelectedSlot();
                pitchChanged = false;
                stage = 1;
                at = now + ns(Cfg.randomMillis(module, "Pearl Delay"));
            }
            case 1 -> {
                if (now >= at) {
                    use(client, player, pearlSlot, false);
                    stage = 2;
                    at = now + ns(Cfg.randomMillis(module, "Wind Charge Delay"));
                }
            }
            case 2 -> {
                if (now >= at) {
                    use(client, player, windSlot, Cfg.mode(module, "Wind Charge Aim").equals("Straight Down"));
                    stage = 3;
                    at = now + ns(60L);
                }
            }
            case 3 -> {
                if (now >= at) {
                    restore(player, module);
                    stage = 0;
                    module.enabled = false; // one-shot: turn ourselves off
                }
            }
            default -> stage = 0;
        }
    }

    private static void use(Minecraft client, LocalPlayer player, int slot, boolean lookDown) {
        player.getInventory().setSelectedSlot(slot);
        if (lookDown) {
            origPitch = player.getXRot();
            pitchChanged = true;
            player.setXRot(90.0F);
        }
        client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        player.swing(InteractionHand.MAIN_HAND);
    }

    private static void restore(LocalPlayer player, Module module) {
        if (pitchChanged) {
            player.setXRot(origPitch);
            pitchChanged = false;
        }
        if (Cfg.bool(module, "Switch Back")) {
            player.getInventory().setSelectedSlot(origSlot);
        }
    }

    private static void fail(Module module, String message) {
        Notifications.push("Pearl Catch: " + message);
        module.enabled = false;
        stage = 0;
    }

    private static int findInHotbar(LocalPlayer player, Item item) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).is(item)) {
                return i;
            }
        }
        return -1;
    }
}
