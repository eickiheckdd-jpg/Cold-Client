package com.coldclient.module;

import com.coldclient.feature.TriggerbotFeature;
import com.coldclient.gui.ClickGuiRenderer.Module;
import com.coldclient.hud.HudManager;
import com.coldclient.render.Ui;
import com.coldclient.setting.ActionSetting;
import com.coldclient.setting.BooleanSetting;
import com.coldclient.setting.ColorSetting;
import com.coldclient.setting.KeybindSetting;
import com.coldclient.setting.ModeSetting;
import com.coldclient.setting.RangeSetting;
import com.coldclient.setting.SliderSetting;
import org.lwjgl.glfw.GLFW;

public final class ModuleRegistry {
    private ModuleRegistry() {}

    // "Friends" is a special tab (name list) with no modules; the GUI draws it itself.
    public static final String FRIENDS = "Friends";

    // Sidebar categories and the icon drawn for each (same index).
    public static final String[] CATEGORIES = {
            "Combat", "Movement", "Visuals", "Client", FRIENDS
    };
    public static final int[] CATEGORY_ICONS = {
            Ui.ICON_COMBAT, Ui.ICON_MOVEMENT, Ui.ICON_VISUALS, Ui.ICON_CLIENT, Ui.ICON_PLAYER
    };

    private static final String BIND_HELP = "Click, then press a key. Esc / Delete clears it";
    private static final int ACCENT = 0xB06BFF;

    public static final Module[] ALL = {
            // ------------------------------------------------------------------ Combat
            new Module(
                    "Triggerbot",
                    "Attacks players under the crosshair",
                    "Combat",
                    new KeybindSetting("Keybind", BIND_HELP),
                    new ModeSetting(
                            "Attack Type",
                            "How attacks are timed around critical hits",
                            TriggerbotFeature.MODE_PRIORITIZE,
                            TriggerbotFeature.MODE_NORMAL,
                            TriggerbotFeature.MODE_PRIORITIZE,
                            TriggerbotFeature.MODE_ONLY_CRITS
                    ),
                    new BooleanSetting("Weapons Only", "Only attack with a sword, axe, trident or mace", false),
                    new BooleanSetting("Ground Check", "Do not swing while rising in a jump", false),
                    new BooleanSetting("Require Left Click", "Only attack while Left Mouse Button is held", false),
                    new BooleanSetting("Shield Check", "Do not attack a target that is blocking", true),
                    new BooleanSetting("Ignore Friends", "Never attack players on your friends list", true),
                    new BooleanSetting("Skip While Using Item", "Do not trigger while using an item", true),
                    new SliderSetting("Attack Cooldown", "Attack only once your charge reaches this level",
                            0.0, 100.0, 100.0, 1.0, "%"),
                    new RangeSetting("Pre-Attack Delay", "Random wait after the crosshair lands on a target",
                            0.0, 300.0, 15.0, 35.0, 1.0, " ms"),
                    new SliderSetting("Hit Chance", "Chance to take each attack opportunity",
                            0.0, 100.0, 93.0, 1.0, "%")
            ),
            new Module(
                    "Aim Assist",
                    "Gently pulls your aim toward nearby players",
                    "Combat",
                    new KeybindSetting("Keybind", BIND_HELP),
                    new ModeSetting("Aim Type", "Smooth eases in, Linear is constant speed, Strict snaps",
                            "Smooth", "Smooth", "Linear", "Strict"),
                    new SliderSetting("Speed", "How fast the aim moves (ignored by Strict)",
                            1.0, 100.0, 40.0, 1.0, "%"),
                    new SliderSetting("FOV", "Only targets inside this cone around your crosshair",
                            5.0, 180.0, 70.0, 1.0, " deg"),
                    new SliderSetting("Range", "Maximum target distance",
                            2.0, 8.0, 4.5, 0.1, " blocks"),
                    new ModeSetting("Aim Point", "Where on the target to aim", "Chest", "Chest", "Head"),
                    new BooleanSetting("Vertical Aim", "Also adjust pitch, not just yaw", true),
                    new SliderSetting("Wobble", "Random drift so the aim is never perfect",
                            0.0, 3.0, 0.6, 0.1),
                    new BooleanSetting("Require Left Click", "Only assist while Left Mouse Button is held", true),
                    new BooleanSetting("Weapons Only", "Only assist with a sword, axe, trident or mace", false),
                    new BooleanSetting("Ignore Friends", "Never aim at players on your friends list", true)
            ),
            new Module(
                    "W-Tap",
                    "Resets sprint after each hit for extra knockback",
                    "Combat",
                    new KeybindSetting("Keybind", BIND_HELP),
                    new RangeSetting("Delay", "Wait after the hit before releasing sprint",
                            0.0, 300.0, 10.0, 40.0, 1.0, " ms"),
                    new RangeSetting("Release Time", "How long sprint stays released",
                            30.0, 300.0, 50.0, 90.0, 1.0, " ms"),
                    new SliderSetting("Chance", "Chance to W-Tap on each hit", 0.0, 100.0, 100.0, 1.0, "%"),
                    new BooleanSetting("Ignore Friends", "Do not W-Tap when hitting friends", true)
            ),
            new Module(
                    "Jump Reset",
                    "Jumps after you are hit to cut the knockback",
                    "Combat",
                    new KeybindSetting("Keybind", BIND_HELP),
                    new RangeSetting("Delay", "Wait after being hit before jumping",
                            0.0, 300.0, 0.0, 30.0, 1.0, " ms"),
                    new SliderSetting("Chance", "Chance to jump after each hit", 0.0, 100.0, 100.0, 1.0, "%"),
                    new BooleanSetting("Only When Moving", "Only jump while holding forward", true)
            ),
            new Module(
                    "Auto Totem",
                    "Opens your inventory and puts a totem in your offhand",
                    "Combat",
                    new KeybindSetting("Keybind", BIND_HELP),
                    new RangeSetting("Delay", "Human-like wait between each step",
                            0.0, 300.0, 80.0, 160.0, 1.0, " ms"),
                    new SliderSetting("Health Threshold", "Only swap at or below this many hearts (10 = always)",
                            1.0, 10.0, 10.0, 1.0, " hearts"),
                    new BooleanSetting("Close Inventory", "Close the inventory after swapping", true)
            ),
            new Module(
                    "Pearl Catch",
                    "Throws a pearl, then a wind charge, then turns itself off",
                    "Combat",
                    new KeybindSetting("Keybind", "Tap to run once. Esc / Delete clears it", GLFW.GLFW_KEY_G),
                    new RangeSetting("Pearl Delay", "Wait before throwing the pearl",
                            0.0, 300.0, 0.0, 40.0, 1.0, " ms"),
                    new RangeSetting("Wind Charge Delay", "Wait between the pearl and the wind charge",
                            0.0, 300.0, 40.0, 90.0, 1.0, " ms"),
                    new ModeSetting("Wind Charge Aim", "Where the wind charge is thrown",
                            "Look Direction", "Look Direction", "Straight Down"),
                    new BooleanSetting("Switch Back", "Return to your previous hotbar slot afterwards", true)
            ),

            // ---------------------------------------------------------------- Movement
            new Module(
                    "Sprint",
                    "Automatically sprints while you move forward",
                    "Movement",
                    new KeybindSetting("Keybind", BIND_HELP)
            ),

            // ----------------------------------------------------------------- Visuals
            new Module(
                    "ESP",
                    "Draws a box around players",
                    "Visuals",
                    new KeybindSetting("Keybind", BIND_HELP),
                    new ColorSetting("Box Color", "Color of the box", 0xFF3B3B),
                    new ColorSetting("Friend Color", "Box color for players on your friends list", 0x3BFF6B),
                    new BooleanSetting("Highlight Friends", "Use the friend color for friends", true),
                    new BooleanSetting("Fill", "Tint the inside of the box", true),
                    new SliderSetting("Fill Opacity", "How strong the tint is", 5.0, 60.0, 18.0, 1.0, "%"),
                    new SliderSetting("Line Width", "Thickness of the outline", 1.0, 4.0, 1.5, 0.5),
                    new SliderSetting("Max Distance", "Do not draw players farther than this",
                            8.0, 128.0, 64.0, 1.0, " blocks"),
                    new BooleanSetting("FOV Compensation", "Match the sprint / speed FOV change so boxes stay aligned", true)
            ),
            new Module(
                    "Fullbright",
                    "Removes darkness",
                    "Visuals",
                    new KeybindSetting("Keybind", BIND_HELP)
            ),

            // ------------------------------------------------------------------ Client
            new Module(
                    "Module List",
                    "Shows your enabled modules on screen",
                    "Client",
                    new KeybindSetting("Keybind", BIND_HELP),
                    new ColorSetting("Text Color", "Color of the module names", ACCENT),
                    new ModeSetting("Sort", "Order of the list", "Longest First",
                            "Longest First", "Shortest First", "Alphabetical"),
                    new BooleanSetting("Background", "Dark backing behind each name", true),
                    new BooleanSetting("Accent Bar", "Colored bar on the docked edge", true),
                    new SliderSetting("Size", "Text size", 60.0, 200.0, 100.0, 5.0, "%"),
                    new ActionSetting("Edit Position", "Drag it anywhere. It aligns to the side it is on",
                            "Edit", HudManager::openEditor)
            ),
            new Module(
                    "Watermark",
                    "Shows the client name on screen",
                    "Client",
                    new KeybindSetting("Keybind", BIND_HELP),
                    new ColorSetting("Text Color", "Color of the name", ACCENT),
                    new BooleanSetting("Show Version", "Show the version number", true),
                    new BooleanSetting("Background", "Dark backing behind the text", true),
                    new SliderSetting("Size", "Text size", 60.0, 200.0, 100.0, 5.0, "%"),
                    new ActionSetting("Edit Position", "Drag it anywhere on screen", "Edit", HudManager::openEditor)
            ),
            new Module(
                    "Notifications",
                    "Pop-up messages, like when you toggle a module",
                    "Client",
                    new KeybindSetting("Keybind", BIND_HELP),
                    new SliderSetting("Duration", "How long each message stays", 1.0, 8.0, 3.0, 0.5, " s"),
                    new BooleanSetting("Toggle Alerts", "Announce when a module is enabled or disabled", true),
                    new ActionSetting("Edit Position", "Drag it anywhere on screen", "Edit", HudManager::openEditor)
            ),
            new Module(
                    "HUD Editor",
                    "Drag the module list, watermark and notifications",
                    "Client",
                    new KeybindSetting("Keybind", BIND_HELP),
                    new ActionSetting("Open Editor", "Move every HUD block in one place", "Open", HudManager::openEditor)
            )
    };

    static {
        // Sensible defaults: the HUD is on from the start.
        get("Module List").enabled = true;
        get("Watermark").enabled = true;
        get("Notifications").enabled = true;
    }

    public static Module get(String name) {
        for (Module module : ALL) {
            if (module.name.equals(name)) {
                return module;
            }
        }
        return null;
    }

    public static boolean isEnabled(String name) {
        Module module = get(name);
        return module != null && module.enabled;
    }
}