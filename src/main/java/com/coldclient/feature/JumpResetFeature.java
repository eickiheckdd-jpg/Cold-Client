package com.coldclient.feature;

import com.coldclient.gui.ClickGuiRenderer.Module;
import com.coldclient.module.Cfg;
import com.coldclient.module.ModuleRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Jumps shortly after you take a hit so the knockback is cut short. */
public final class JumpResetFeature {
    private static int lastHurt;
    private static long jumpAt;
    private static boolean jumpHeld;

    private JumpResetFeature() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(JumpResetFeature::tick);
    }

    private static void tick(Minecraft client) {
        if (jumpHeld) {
            client.options.keyJump.setDown(false);
            jumpHeld = false;
        }

        Module module = ModuleRegistry.get("Jump Reset");
        if (module == null || !module.enabled || client.player == null) {
            lastHurt = 0;
            jumpAt = 0L;
            return;
        }

        LocalPlayer player = client.player;
        long now = System.nanoTime();

        int hurt = player.hurtTime;
        if (hurt > lastHurt && jumpAt == 0L && Cfg.chance(Cfg.num(module, "Chance"))) {
            jumpAt = now + Cfg.randomMillis(module, "Delay") * 1_000_000L;
        }
        lastHurt = hurt;

        if (jumpAt != 0L && now >= jumpAt) {
            jumpAt = 0L;
            boolean moving = !Cfg.bool(module, "Only When Moving") || client.options.keyUp.isDown();
            if (player.onGround() && moving && client.gui.screen() == null) {
                client.options.keyJump.setDown(true);
                jumpHeld = true;
            }
        }
    }
}
