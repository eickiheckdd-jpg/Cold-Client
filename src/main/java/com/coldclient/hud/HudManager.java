package com.coldclient.hud;

import com.coldclient.gui.ClickGuiRenderer.Module;
import com.coldclient.module.ModuleRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class HudManager {
    public static final HudElement MODULE_LIST = new HudElement("Module List", 0.99f, 0.03f, true);
    public static final HudElement WATERMARK = new HudElement("Watermark", 0.01f, 0.02f, false);
    public static final HudElement NOTIFICATIONS = new HudElement("Notifications", 0.99f, 0.80f, true);
    public static final HudElement[] ALL = {MODULE_LIST, WATERMARK, NOTIFICATIONS};

    private HudManager() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(HudManager::tick);
    }

    /** The "HUD Editor" module acts as a button: turning it on opens the editor and switches it back off. */
    private static void tick(Minecraft client) {
        Module module = ModuleRegistry.get("HUD Editor");
        if (module != null && module.enabled) {
            module.enabled = false;
            if (client.player != null && !(client.gui.screen() instanceof HudEditorScreen)) {
                openEditor();
            }
        }
    }

    public static void openEditor() {
        Minecraft.getInstance().gui.setScreen(new HudEditorScreen());
    }

    public static void resetAll() {
        for (HudElement element : ALL) {
            element.reset();
        }
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("coldclient-hud.properties");
    }

    public static void save() {
        Properties props = new Properties();
        for (HudElement e : ALL) {
            props.setProperty(e.moduleName + ".x", Float.toString(e.anchorX));
            props.setProperty(e.moduleName + ".y", Float.toString(e.anchorY));
            props.setProperty(e.moduleName + ".right", Boolean.toString(e.alignRight));
        }
        try (OutputStream out = Files.newOutputStream(file())) {
            props.store(out, "Cold Client HUD positions");
        } catch (IOException e) {
            System.err.println("[Cold Client] Could not save HUD positions: " + e.getMessage());
        }
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) {
            return;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(f)) {
            props.load(in);
            for (HudElement e : ALL) {
                String x = props.getProperty(e.moduleName + ".x");
                String y = props.getProperty(e.moduleName + ".y");
                String right = props.getProperty(e.moduleName + ".right");
                if (x != null && y != null && right != null) {
                    e.anchorX = Float.parseFloat(x);
                    e.anchorY = Float.parseFloat(y);
                    e.alignRight = Boolean.parseBoolean(right);
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("[Cold Client] Could not read HUD positions: " + e.getMessage());
        }
    }
}
