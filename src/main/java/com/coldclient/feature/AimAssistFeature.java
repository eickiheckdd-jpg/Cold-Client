package com.coldclient.feature;

import com.coldclient.gui.ClickGuiRenderer.Module;
import com.coldclient.module.Cfg;
import com.coldclient.module.ModuleRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

/** Runs once per rendered frame (see Features.frame) so the aim moves smoothly instead of 20 times a second. */
public final class AimAssistFeature {
    private static long lastNanos;
    private static float wobbleYaw;
    private static float wobblePitch;

    private AimAssistFeature() {}

    public static void frame(Minecraft client) {
        long now = System.nanoTime();
        float dt = (now - lastNanos) / 1_000_000_000.0f;
        lastNanos = now;
        if (dt < 0.0005f || dt > 0.25f) {
            return;
        }

        Module module = ModuleRegistry.get("Aim Assist");
        if (module == null || !module.enabled || client.player == null || client.level == null
                || client.gui.screen() != null) {
            return;
        }
        LocalPlayer player = client.player;

        if (Cfg.bool(module, "Require Left Click") && !Targets.leftMouseDown()) {
            return;
        }
        if (Cfg.bool(module, "Weapons Only") && !Targets.isWeapon(player.getMainHandItem())) {
            return;
        }

        float partial = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        boolean ignoreFriends = Cfg.bool(module, "Ignore Friends");
        double range = Cfg.num(module, "Range");
        double fov = Cfg.num(module, "FOV");
        double aimRatio = Cfg.mode(module, "Aim Point").equals("Head") ? 0.90 : 0.65;

        Vec3 eye = player.getEyePosition(partial);
        float curYaw = player.getYRot();
        float curPitch = player.getXRot();

        Player best = null;
        double bestAngle = Double.MAX_VALUE;
        double bestYawDiff = 0.0;
        double bestPitchDiff = 0.0;

        for (Player other : client.level.players()) {
            if (!Targets.validPlayer(player, other, ignoreFriends)) {
                continue;
            }
            Vec3 pos = other.getPosition(partial);
            double dx = pos.x() - eye.x();
            double dy = pos.y() + other.getBbHeight() * aimRatio - eye.y();
            double dz = pos.z() - eye.z();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > range || dist < 0.3) {
                continue;
            }
            double yaw = Math.toDegrees(Math.atan2(-dx, dz));
            double pitch = -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
            double yawDiff = wrap(yaw - curYaw);
            double pitchDiff = pitch - curPitch;
            double angle = Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
            if (angle > fov * 0.5 || angle >= bestAngle) {
                continue;
            }
            if (!player.hasLineOfSight(other)) {
                continue;
            }
            best = other;
            bestAngle = angle;
            bestYawDiff = yawDiff;
            bestPitchDiff = pitchDiff;
        }

        if (best == null) {
            wobbleYaw = 0f;
            wobblePitch = 0f;
            return;
        }

        // Slowly drifting random offset so the aim never looks locked on perfectly.
        float wobble = (float) Cfg.num(module, "Wobble");
        if (wobble > 0f) {
            ThreadLocalRandom r = ThreadLocalRandom.current();
            wobbleYaw = clamp(wobbleYaw + (r.nextFloat() - 0.5f) * wobble * dt * 20f, -wobble, wobble);
            wobblePitch = clamp(wobblePitch + (r.nextFloat() - 0.5f) * wobble * dt * 20f, -wobble, wobble);
        }

        double yawDiff = bestYawDiff + wobbleYaw;
        double pitchDiff = Cfg.bool(module, "Vertical Aim") ? bestPitchDiff + wobblePitch : 0.0;

        if (Math.abs(yawDiff) < 0.15 && Math.abs(pitchDiff) < 0.15) {
            return;
        }

        double speed = Cfg.num(module, "Speed");
        double moveYaw;
        double movePitch;
        switch (Cfg.mode(module, "Aim Type")) {
            case "Strict" -> {
                moveYaw = yawDiff;
                movePitch = pitchDiff;
            }
            case "Linear" -> {
                double maxStep = speed * 5.0 * dt; // degrees this frame
                moveYaw = clamp(yawDiff, maxStep);
                movePitch = clamp(pitchDiff, maxStep);
            }
            default -> { // Smooth: exponential ease toward the target
                double factor = 1.0 - Math.exp(-speed * 0.2 * dt);
                moveYaw = yawDiff * factor;
                movePitch = pitchDiff * factor;
            }
        }

        // Entity.turn() multiplies by 0.15 internally.
        player.turn(moveYaw / 0.15, movePitch / 0.15);
    }

    private static double wrap(double degrees) {
        degrees %= 360.0;
        if (degrees >= 180.0) {
            degrees -= 360.0;
        }
        if (degrees < -180.0) {
            degrees += 360.0;
        }
        return degrees;
    }

    private static float clamp(float v, float limit) {
        return Math.max(-limit, Math.min(limit, v));
    }

    private static double clamp(double v, double limit) {
        return Math.max(-limit, Math.min(limit, v));
    }
}
