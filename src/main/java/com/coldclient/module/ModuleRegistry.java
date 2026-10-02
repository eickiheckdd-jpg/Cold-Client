package com.coldclient.module;

import com.coldclient.gui.ClickGuiRenderer.Module;
import com.coldclient.feature.TriggerbotFeature;
import com.coldclient.setting.BooleanSetting;
import com.coldclient.setting.KeybindSetting;
import com.coldclient.setting.ModeSetting;
import com.coldclient.setting.RangeSetting;
import com.coldclient.setting.SliderSetting;

public final class ModuleRegistry {
    private ModuleRegistry() {}

    // Config is intentionally not a normal category. It is a private view opened
    // by right-clicking a module card.
    public static final String[] CATEGORIES = {
            "Combat", "Visuals", "Misc", "Player", "Client", "Movement", "Hud"
    };

    public static final Module[] ALL = {
            new Module(
                    "Triggerbot",
                    "Attacks players under the crosshair",
                    "Combat",
                    new KeybindSetting(
                            "Keybind",
                            "Click, then press a key. Esc / Delete clears it"
                    ),
                    new ModeSetting(
                            "Attack Type",
                            "How attacks are timed around critical hits",
                            TriggerbotFeature.MODE_PRIORITIZE,
                            TriggerbotFeature.MODE_NORMAL,
                            TriggerbotFeature.MODE_PRIORITIZE,
                            TriggerbotFeature.MODE_ONLY_CRITS
                    ),
                    new BooleanSetting(
                            "Weapons Only",
                            "Only attack with a sword, axe, trident or mace",
                            false
                    ),
                    new BooleanSetting(
                            "Ground Check",
                            "Do not swing while rising in a jump",
                            false
                    ),
                    new BooleanSetting(
                            "Require Left Click",
                            "Only attack while Left Mouse Button is held",
                            false
                    ),
                    new BooleanSetting(
                            "Shield Check",
                            "Do not attack a target that is blocking",
                            true
                    ),
                    new BooleanSetting(
                            "Skip While Using Item",
                            "Do not trigger while using an item",
                            true
                    ),
                    new SliderSetting(
                            "Attack Cooldown",
                            "Attack only once your charge reaches this level",
                            0.0,
                            100.0,
                            100.0,
                            1.0,
                            "%"
                    ),
                    new RangeSetting(
                            "Pre-Attack Delay",
                            "Random wait after the crosshair lands on a target",
                            0.0,
                            300.0,
                            15.0,
                            35.0,
                            1.0,
                            " ms"
                    ),
                    new SliderSetting(
                            "Hit Chance",
                            "Chance to take each attack opportunity",
                            0.0,
                            100.0,
                            93.0,
                            1.0,
                            "%"
                    )
            )
    };

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