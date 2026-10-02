package com.coldclient.module;

import com.coldclient.gui.ClickGuiRenderer.Module;
import com.coldclient.setting.BooleanSetting;
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
                    new BooleanSetting(
                            "Skip While Using Item",
                            "Do not trigger while using an item",
                            true
                    ),
                    new SliderSetting(
                            "Attack Delay",
                            "Extra delay between triggerbot attacks (milliseconds)",
                            0.0,
                            500.0,
                            0.0,
                            25.0
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