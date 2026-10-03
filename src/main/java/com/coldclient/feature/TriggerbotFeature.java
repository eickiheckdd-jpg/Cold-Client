package com.coldclient.feature;

import com.coldclient.gui.ClickGuiRenderer.Module;
import com.coldclient.module.FriendManager;
import com.coldclient.module.ModuleRegistry;
import com.coldclient.setting.BooleanSetting;
import com.coldclient.setting.ModeSetting;
import com.coldclient.setting.RangeSetting;
import com.coldclient.setting.SliderSetting;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.ThreadLocalRandom;

public final class TriggerbotFeature {
    public static final String MODE_NORMAL = "Normal";
    public static final String MODE_PRIORITIZE = "Prioritize Crits";
    public static final String MODE_ONLY_CRITS = "Only Crits";

    // Pre-attack delay state: the target we've been aiming at, since when, and the delay we rolled for it.
    private static Entity armedTarget;
    private static long armedAtNanos;
    private static long delayNanos;

    private TriggerbotFeature() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(TriggerbotFeature::tick);
    }

    private static void disarm() {
        armedTarget = null;
    }

    private static void tick(Minecraft client) {
        Module module = ModuleRegistry.get("Triggerbot");
        if (module == null || !module.enabled) {
            disarm();
            return;
        }

        if (client.player == null || client.gameMode == null || client.gui.screen() != null) {
            disarm();
            return;
        }

        LocalPlayer player = client.player;
        ModeSetting attackType = (ModeSetting) module.setting("Attack Type");
        BooleanSetting weaponsOnly = (BooleanSetting) module.setting("Weapons Only");
        BooleanSetting groundCheck = (BooleanSetting) module.setting("Ground Check");
        BooleanSetting requireLeftClick = (BooleanSetting) module.setting("Require Left Click");
        BooleanSetting shieldCheck = (BooleanSetting) module.setting("Shield Check");
        BooleanSetting ignoreFriends = (BooleanSetting) module.setting("Ignore Friends");
        BooleanSetting skipWhileUsingItem = (BooleanSetting) module.setting("Skip While Using Item");
        SliderSetting attackCooldown = (SliderSetting) module.setting("Attack Cooldown");
        RangeSetting preAttackDelay = (RangeSetting) module.setting("Pre-Attack Delay");
        SliderSetting hitChance = (SliderSetting) module.setting("Hit Chance");

        // ---- State filters (failing any of these also cancels the pending delay) ----
        if (player.isSpectator() || (skipWhileUsingItem.value() && player.isUsingItem())) {
            disarm();
            return;
        }
        if (requireLeftClick.value() && !isLeftMouseDown()) {
            disarm();
            return;
        }
        if (weaponsOnly.value() && !isWeapon(player.getMainHandItem())) {
            disarm();
            return;
        }

        // ---- Target under the crosshair ----
        if (!(client.hitResult instanceof EntityHitResult entityHitResult)) {
            disarm();
            return;
        }
        Entity target = entityHitResult.getEntity();
        if (!(target instanceof Player targetPlayer)
                || targetPlayer == player || !targetPlayer.isAlive() || targetPlayer.isSpectator()
                || (ignoreFriends.value() && FriendManager.isFriend(targetPlayer))) {
            disarm();
            return;
        }

        long now = System.nanoTime();

        // Crosshair just landed on a (new) target: roll the pre-attack delay.
        if (armedTarget != target) {
            armedTarget = target;
            armedAtNanos = now;
            delayNanos = rollDelayNanos(preAttackDelay);
        }

        // ---- Gates (these wait without resetting the delay) ----
        if (shieldCheck.value() && targetPlayer.isBlocking()) {
            return;
        }
        if (!motionAllowsAttack(player, attackType, groundCheck.value())) {
            return;
        }
        if (player.getAttackStrengthScale(0.0F) < attackCooldown.value() / 100.0) {
            return;
        }
        if (now - armedAtNanos < delayNanos) {
            return;
        }

        // ---- Hit chance: sometimes let the opportunity pass and wait a fresh delay ----
        if (ThreadLocalRandom.current().nextDouble() * 100.0 >= hitChance.value()) {
            armedAtNanos = now;
            delayNanos = rollDelayNanos(preAttackDelay);
            return;
        }

        MultiPlayerGameMode gameMode = client.gameMode;
        gameMode.attack(player, targetPlayer);
        player.swing(InteractionHand.MAIN_HAND);
        disarm(); // next target acquisition rolls a new delay
    }

    /**
     * Crit/motion rules.
     * - Only Crits: never on the ground, and only while falling.
     * - Prioritize Crits: on the ground behaves normally; in the air waits until falling.
     * - Normal: no crit logic.
     * - Ground Check (any mode): additionally never swing while rising in the air.
     */
    private static boolean motionAllowsAttack(LocalPlayer player, ModeSetting attackType, boolean groundCheck) {
        boolean onGround = player.onGround();
        boolean falling = player.getDeltaMovement().y() < 0.0;

        if (attackType.is(MODE_ONLY_CRITS)) {
            return !onGround && falling;
        }
        boolean airborneRising = !onGround && !falling;
        if ((attackType.is(MODE_PRIORITIZE) || groundCheck) && airborneRising) {
            return false;
        }
        return true;
    }

    private static boolean isWeapon(ItemStack stack) {
        return stack.is(ItemTags.SWORDS)
                || stack.is(ItemTags.AXES)
                || stack.is(Items.TRIDENT)
                || stack.is(Items.MACE);
    }

    private static boolean isLeftMouseDown() {
        long window = GLFW.glfwGetCurrentContext();
        return window != 0L && GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }

    private static long rollDelayNanos(RangeSetting range) {
        double lo = range.low();
        double hi = range.high();
        double ms = hi > lo ? lo + ThreadLocalRandom.current().nextDouble() * (hi - lo) : lo;
        return Math.round(ms * 1_000_000.0);
    }
}
