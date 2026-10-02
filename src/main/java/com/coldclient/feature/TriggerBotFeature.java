package com.coldclient.feature;

import com.coldclient.gui.ClickGuiRenderer.Module;
import com.coldclient.module.ModuleRegistry;
import com.coldclient.setting.BooleanSetting;
import com.coldclient.setting.SliderSetting;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

public final class TriggerbotFeature {
    private static long nextAttackNanos;

    private TriggerbotFeature() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(TriggerbotFeature::tick);
    }

    private static void tick(Minecraft client) {
        Module module = ModuleRegistry.get("Triggerbot");
        if (module == null || !module.enabled) {
            nextAttackNanos = 0L;
            return;
        }

        if (client.player == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }

        LocalPlayer player = client.player;
        BooleanSetting skipWhileUsingItem = (BooleanSetting) module.setting("Skip While Using Item");
        SliderSetting attackDelay = (SliderSetting) module.setting("Attack Delay");

        if (player.isSpectator() || (skipWhileUsingItem.value() && player.isUsingItem())) {
            return;
        }

        if (player.getAttackStrengthScale(0.0F) < 1.0F) {
            return;
        }

        long now = System.nanoTime();
        if (now < nextAttackNanos) {
            return;
        }

        if (!(client.hitResult instanceof EntityHitResult entityHitResult)) {
            return;
        }

        Entity target = entityHitResult.getEntity();
        if (!(target instanceof Player targetPlayer)) {
            return;
        }

        if (targetPlayer == player || !targetPlayer.isAlive() || targetPlayer.isSpectator()) {
            return;
        }

        MultiPlayerGameMode gameMode = client.gameMode;
        gameMode.attack(player, targetPlayer);

        nextAttackNanos = now + Math.round(attackDelay.value() * 1_000_000.0);
    }
}