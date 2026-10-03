package com.coldclient.feature;

import com.coldclient.gui.ClickGuiRenderer.Module;
import com.coldclient.module.Cfg;
import com.coldclient.module.ModuleRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** After you hit someone, briefly drops sprint and then restores it so the next hit gets sprint knockback. */
public final class WTapFeature {
    private static long startAt;
    private static long endAt;
    private static boolean active;

    private WTapFeature() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(WTapFeature::tick);
    }

    private static long ns(long millis) {
        return millis * 1_000_000L;
    }

    private static void tick(Minecraft client) {
        Module module = ModuleRegistry.get("W-Tap");
        if (module == null || !module.enabled || client.player == null) {
            if (active) {
                finish(client);
            }
            startAt = 0L;
            return;
        }

        LocalPlayer player = client.player;
        long now = System.nanoTime();

        if (AttackWatcher.attacked() && startAt == 0L && !active) {
            boolean ignoreFriends = Cfg.bool(module, "Ignore Friends");
            if (Targets.validPlayer(player, AttackWatcher.target(), ignoreFriends)
                    && player.isSprinting()
                    && Cfg.chance(Cfg.num(module, "Chance"))) {
                startAt = now + ns(Cfg.randomMillis(module, "Delay"));
                endAt = startAt + ns(Cfg.randomMillis(module, "Release Time"));
            }
        }

        if (startAt != 0L && !active && now >= startAt) {
            active = true;
            SprintFeature.suppressed = true;
            player.setSprinting(false);
            client.options.keySprint.setDown(false);
        }

        if (active && now >= endAt) {
            finish(client);
        }
    }

    private static void finish(Minecraft client) {
        active = false;
        startAt = 0L;
        SprintFeature.suppressed = false;
        if (client.player != null && client.options.keyUp.isDown() && !client.player.isShiftKeyDown()) {
            client.player.setSprinting(true);
        }
    }
}
