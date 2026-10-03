package com.coldclient.feature;

import com.coldclient.gui.ClickGuiRenderer.Module;
import com.coldclient.module.ModuleRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Client-side night vision. Re-applied before it can run out so it never flickers. */
public final class FullbrightFeature {
    private static boolean applied;

    private FullbrightFeature() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(FullbrightFeature::tick);
    }

    private static void tick(Minecraft client) {
        Module module = ModuleRegistry.get("Fullbright");
        if (client.player == null || module == null) {
            applied = false;
            return;
        }
        if (module.enabled) {
            MobEffectInstance current = client.player.getEffect(MobEffects.NIGHT_VISION);
            if (current == null || current.getDuration() < 400) {
                client.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1200, 0, false, false, false));
            }
            applied = true;
        } else if (applied) {
            client.player.removeEffect(MobEffects.NIGHT_VISION);
            applied = false;
        }
    }
}
