package com.coldclient.module;

import com.coldclient.gui.ClickGuiRenderer.Module;
import com.coldclient.setting.BooleanSetting;
import com.coldclient.setting.ColorSetting;
import com.coldclient.setting.ModeSetting;
import com.coldclient.setting.RangeSetting;
import com.coldclient.setting.SliderSetting;

import java.util.concurrent.ThreadLocalRandom;

/** Tiny typed accessors so feature code stays readable. Setting names must match ModuleRegistry. */
public final class Cfg {
    private Cfg() {}

    public static boolean on(String module) {
        Module m = ModuleRegistry.get(module);
        return m != null && m.enabled;
    }

    public static boolean bool(Module m, String name) {
        return ((BooleanSetting) m.setting(name)).value();
    }

    public static double num(Module m, String name) {
        return ((SliderSetting) m.setting(name)).value();
    }

    public static int color(Module m, String name) {
        return ((ColorSetting) m.setting(name)).rgb();
    }

    public static String mode(Module m, String name) {
        return ((ModeSetting) m.setting(name)).get();
    }

    public static RangeSetting range(Module m, String name) {
        return (RangeSetting) m.setting(name);
    }

    /** Random value inside a range setting, in milliseconds. */
    public static long randomMillis(Module m, String name) {
        RangeSetting r = range(m, name);
        double lo = r.low();
        double hi = r.high();
        double v = hi > lo ? lo + ThreadLocalRandom.current().nextDouble() * (hi - lo) : lo;
        return Math.round(v);
    }

    /** True with the given percentage chance (0..100). */
    public static boolean chance(double percent) {
        return ThreadLocalRandom.current().nextDouble() * 100.0 < percent;
    }
}