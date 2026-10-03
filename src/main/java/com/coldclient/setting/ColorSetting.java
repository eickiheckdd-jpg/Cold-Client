package com.coldclient.setting;

/** RGB color edited with an HSV picker. Value is 0xRRGGBB. */
public final class ColorSetting extends Setting<Integer> {
    private float hue;
    private float sat;
    private float val;
    private boolean open; // GUI state

    public ColorSetting(String name, String description, int rgb) {
        super(name, description, rgb & 0xFFFFFF);
        fromRgb(rgb & 0xFFFFFF);
    }

    public int rgb() {
        return get();
    }

    public float hue() { return hue; }
    public float sat() { return sat; }
    public float val() { return val; }

    public void setHue(float h) {
        hue = clamp(h);
        apply();
    }

    public void setSatVal(float s, float v) {
        sat = clamp(s);
        val = clamp(v);
        apply();
    }

    public boolean open() { return open; }
    public void setOpen(boolean open) { this.open = open; }

    @Override
    public void cycle() {
        open = !open;
    }

    /** Fully saturated, full-brightness color of a hue (0..1). */
    public static int hueColor(float h) {
        return hsvToRgb(h, 1.0f, 1.0f);
    }

    private void apply() {
        set(hsvToRgb(hue, sat, val));
    }

    private void fromRgb(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d = max - min;
        val = max;
        sat = max <= 0f ? 0f : d / max;
        float h;
        if (d <= 0f) {
            h = 0f;
        } else if (max == r) {
            h = ((g - b) / d) % 6f;
        } else if (max == g) {
            h = (b - r) / d + 2f;
        } else {
            h = (r - g) / d + 4f;
        }
        h /= 6f;
        hue = h < 0f ? h + 1f : h;
    }

    private static int hsvToRgb(float h, float s, float v) {
        h = clamp(h);
        float hh = (h >= 1f ? 0f : h) * 6f;
        int i = (int) Math.floor(hh);
        float f = hh - i;
        float p = v * (1f - s);
        float q = v * (1f - s * f);
        float t = v * (1f - s * (1f - f));
        float r, g, b;
        switch (i) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return (Math.round(r * 255f) << 16) | (Math.round(g * 255f) << 8) | Math.round(b * 255f);
    }

    private static float clamp(float v) {
        return v < 0f ? 0f : Math.min(v, 1f);
    }
}