package com.coldclient.setting;

public final class SliderSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double step;

    public SliderSetting(String name, String description, double min, double max, double defaultValue, double step) {
        super(name, description, clamp(defaultValue, min, max));
        if (!(max > min)) {
            throw new IllegalArgumentException("Slider max must be greater than min");
        }
        if (!(step > 0.0)) {
            throw new IllegalArgumentException("Slider step must be greater than zero");
        }
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }

    public double step() {
        return step;
    }

    public double value() {
        return get();
    }

    public void setFromFraction(double fraction) {
        fraction = Math.max(0.0, Math.min(1.0, fraction));
        double raw = min + fraction * (max - min);
        set(snap(raw));
    }

    private double snap(double value) {
        double stepped = Math.round((value - min) / step) * step + min;
        return clamp(stepped, min, max);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}