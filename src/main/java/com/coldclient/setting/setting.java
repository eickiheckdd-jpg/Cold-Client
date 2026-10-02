package com.coldclient.setting;

/**
 * Reusable in-memory module setting.
 * GUI code only needs to know the concrete setting type; modules just declare settings.
 */
public abstract class Setting<T> {
    private final String name;
    private final String description;
    private T value;

    protected Setting(String name, String description, T defaultValue) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Setting name cannot be blank");
        }
        this.name = name;
        this.description = description == null ? "" : description;
        this.value = defaultValue;
    }

    public final String name() {
        return name;
    }

    public final String description() {
        return description;
    }

    public final T get() {
        return value;
    }

    public final void set(T value) {
        this.value = value;
    }

    /** Called by the generic GUI for click-to-change controls. */
    public void cycle() {
    }
}