package com.coldclient.feature;

import com.coldclient.gui.ClickGuiRenderer.Module;
import com.coldclient.module.Cfg;
import com.coldclient.module.ModuleRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;

/**
 * Keeps a totem in your offhand the way a player would: opens the inventory, waits a human delay,
 * swaps a totem into the offhand, waits again, then closes the inventory.
 */
public final class AutoTotemFeature {
    private enum Stage { IDLE, SWAP_WAIT, CLOSE_WAIT }

    private static Stage stage = Stage.IDLE;
    private static long at;
    private static long cooldownUntil;

    private AutoTotemFeature() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(AutoTotemFeature::tick);
    }

    private static long ns(long millis) {
        return millis * 1_000_000L;
    }

    private static void tick(Minecraft client) {
        Module module = ModuleRegistry.get("Auto Totem");
        if (module == null || !module.enabled || client.player == null || client.gameMode == null) {
            stage = Stage.IDLE;
            return;
        }

        LocalPlayer player = client.player;
        long now = System.nanoTime();

        switch (stage) {
            case IDLE -> {
                if (now < cooldownUntil || client.gui.screen() != null) {
                    return;
                }
                if (player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
                    return;
                }
                if (player.getHealth() > Cfg.num(module, "Health Threshold") * 2.0) {
                    return;
                }
                if (findTotem(player) < 0) {
                    return;
                }
                client.gui.setScreen(new InventoryScreen(player));
                stage = Stage.SWAP_WAIT;
                at = now + ns(Cfg.randomMillis(module, "Delay"));
            }
            case SWAP_WAIT -> {
                if (!(client.gui.screen() instanceof InventoryScreen)) {
                    stage = Stage.IDLE; // the player closed it
                    cooldownUntil = now + ns(1000L);
                    return;
                }
                if (now < at) {
                    return;
                }
                int slot = findTotem(player);
                if (slot >= 0) {
                    int menuSlot = slot < 9 ? 36 + slot : slot;
                    client.gameMode.handleInventoryMouseClick(
                            player.inventoryMenu.containerId, menuSlot, 40, ClickType.SWAP, player);
                }
                cooldownUntil = now + ns(1000L);
                if (Cfg.bool(module, "Close Inventory")) {
                    stage = Stage.CLOSE_WAIT;
                    at = now + ns(Cfg.randomMillis(module, "Delay"));
                } else {
                    stage = Stage.IDLE;
                }
            }
            case CLOSE_WAIT -> {
                if (now >= at) {
                    if (client.gui.screen() instanceof InventoryScreen) {
                        client.gui.setScreen(null);
                    }
                    stage = Stage.IDLE;
                }
            }
        }
    }

    /** Inventory index (0-8 hotbar, 9-35 main) of a totem, or -1. */
    private static int findTotem(LocalPlayer player) {
        for (int i = 0; i < 36; i++) {
            if (player.getInventory().getItem(i).is(Items.TOTEM_OF_UNDYING)) {
                return i;
            }
        }
        return -1;
    }
}
