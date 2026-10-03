package com.coldclient.feature;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Detects "an attack just happened" by watching for the swing animation starting while an entity
 * is under the crosshair. Works for manual clicks and for other modules (Triggerbot).
 * Register this BEFORE the features that read it.
 */
public final class AttackWatcher {
    private static boolean lastSwinging;
    private static boolean attacked;
    private static Entity target;

    private AttackWatcher() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(AttackWatcher::tick);
    }

    /** True for exactly one tick after an attack on an entity. */
    public static boolean attacked() {
        return attacked;
    }

    public static Entity target() {
        return target;
    }

    private static void tick(Minecraft client) {
        attacked = false;
        target = null;
        if (client.player == null) {
            lastSwinging = false;
            return;
        }
        boolean swinging = client.player.swinging;
        if (swinging && !lastSwinging && client.hitResult instanceof EntityHitResult hit) {
            attacked = true;
            target = hit.getEntity();
        }
        lastSwinging = swinging;
    }
          }
