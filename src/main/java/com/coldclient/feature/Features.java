package com.coldclient.feature;

import com.coldclient.hud.HudManager;
import com.coldclient.hud.Notifications;
import com.coldclient.module.FriendManager;
import com.coldclient.module.KeybindManager;
import net.minecraft.client.Minecraft;

/** Single place that wires every feature. Order matters: AttackWatcher must run before its readers. */
public final class Features {
    private Features() {}

    public static void registerAll() {
        FriendManager.load();
        HudManager.load();

        KeybindManager.register();
        TriggerbotFeature.register();
        AttackWatcher.register();
        SprintFeature.register();
        WTapFeature.register();
        JumpResetFeature.register();
        AutoTotemFeature.register();
        PearlCatchFeature.register();
        FullbrightFeature.register();
        Notifications.register();
        HudManager.register();
    }

    private static boolean reported;

    /** Prints the first render-hook error only, so a bug doesn't flood the log every frame. */
    public static void reportRenderError(Throwable t) {
        if (!reported) {
            reported = true;
            System.err.println("[Cold Client] Render hook error:");
            t.printStackTrace();
        }
    }

    /** Called once per rendered frame from the GUI render mixin. */
    public static void frame(Minecraft client) {
        AimAssistFeature.frame(client);
    }
}
