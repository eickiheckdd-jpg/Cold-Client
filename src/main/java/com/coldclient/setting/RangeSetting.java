package com.coldclient.setting;

/** Dual-thumb range slider: a [low, high] pair, always low <= high. */
public final class RangeSetting extends Setting<double[]> {
    private final double min;
    private final double max;
    private final double step;
    private final String suffix;

    public RangeSetting(String name, String description, double min, double max,
                        double defaultLow, double defaultHigh, double step, String suffix) {
        super(name, description, new double[]{defaultLow, defaultHigh});
        if (!(max > min)) {
            throw new IllegalArgumentException("Range max must be greater than min");
        }
        if (!(step > 0.0)) {
            throw new IllegalArgumentException("Range step must be greater than zero");
        }
        this.min = min;
        this.max = max;
        this.step = step;
        this.suffix = suffix == null ? "" : suffix;
        double lo = snap(defaultLow);
        double hi = snap(defaultHigh);
        get()[0] = Math.min(lo, hi);
        get()[1] = Math.max(lo, hi);
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

    public String suffix() {
        return suffix;
    }

    public double low() {
        return get()[0];
    }

    public double high() {
        return get()[1];
    }

    /** Moves the low thumb; it can never pass the high thumb. */
    public void setLowFromFraction(double fraction) {
        get()[0] = Math.min(snap(fromFraction(fraction)), high());
    }

    /** Moves the high thumb; it can never pass the low thumb. */
    public void setHighFromFraction(double fraction) {
        get()[1] = Math.max(snap(fromFraction(fraction)), low());
    }

    public double lowFraction() {
        return (low() - min) / (max - min);
    }

    public double highFraction() {
        return (high() - min) / (max - min);
    }

    private double fromFraction(double fraction) {
        fraction = Math.max(0.0, Math.min(1.0, fraction));
        return min + fraction * (max - min);
    }

    private double snap(double value) {
        double stepped = Math.round((value - min) / step) * step + min;
        return Math.max(min, Math.min(max, stepped));
    }
}