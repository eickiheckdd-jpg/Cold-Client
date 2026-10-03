package com.coldclient.hud;

import com.coldclient.gui.ClickGuiRenderer.Module;
import com.coldclient.module.Cfg;
import com.coldclient.module.ModuleRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/** Small toast messages. Also announces module toggles. */
public final class Notifications {
    public static final class Toast {
        public final String text;
        public final long createdNanos;

        Toast(String text) {
            this.text = text;
            this.createdNanos = System.nanoTime();
        }
    }

    private static final int MAX_TOASTS = 5;
    private static final List<Toast> TOASTS = new ArrayList<>();
    private static boolean[] lastState;

    private Notifications() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(Notifications::tick);
    }

    public static void push(String text) {
        if (!Cfg.on("Notifications")) {
            return;
        }
        TOASTS.add(0, new Toast(text));
        while (TOASTS.size() > MAX_TOASTS) {
            TOASTS.remove(TOASTS.size() - 1);
        }
    }

    public static List<Toast> active() {
        return TOASTS;
    }

    public static boolean hasActive() {
        return !TOASTS.isEmpty();
    }

    public static float durationSeconds() {
        Module module = ModuleRegistry.get("Notifications");
        return module == null ? 3.0f : (float) Cfg.num(module, "Duration");
    }

    private static void tick(Minecraft client) {
        Module self = ModuleRegistry.get("Notifications");
        if (self == null) {
            return;
        }

        long now = System.nanoTime();
        long lifeNanos = (long) (durationSeconds() * 1_000_000_000.0);
        TOASTS.removeIf(t -> now - t.createdNanos > lifeNanos);

        Module[] all = ModuleRegistry.ALL;
        if (lastState == null || lastState.length != all.length) {
            lastState = new boolean[all.length];
            for (int i = 0; i < all.length; i++) {
                lastState[i] = all[i].enabled;
            }
            return;
        }
        boolean announce = self.enabled && Cfg.bool(self, "Toggle Alerts") && client.player != null;
        for (int i = 0; i < all.length; i++) {
            Module m = all[i];
            if (m.enabled != lastState[i]) {
                lastState[i] = m.enabled;
                boolean skip = m == self || m.name.equals("HUD Editor");
                if (announce && !skip) {
                    push(m.name + (m.enabled ? " enabled" : " disabled"));
                }
            }
        }
    }
}
