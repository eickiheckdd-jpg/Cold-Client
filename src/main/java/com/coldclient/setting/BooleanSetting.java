package com.coldclient.setting;

public final class BooleanSetting extends Setting<Boolean> {
    public BooleanSetting(String name, String description, boolean defaultValue) {
        super(name, description, defaultValue);
    }

    public boolean value() {
        return get();
    }

    public void toggle() {
        set(!value());
    }

    @Override
    public void cycle() {
        toggle();
    }
}