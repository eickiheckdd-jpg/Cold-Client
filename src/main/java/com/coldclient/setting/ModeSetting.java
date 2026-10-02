package com.coldclient.setting;

/** Single-choice dropdown. */
public final class ModeSetting extends Setting<String> {
    private final String[] options;
    private boolean open; // GUI state: is the dropdown expanded?

    public ModeSetting(String name, String description, String defaultValue, String... options) {
        super(name, description, defaultValue);
        if (options == null || options.length < 2) {
            throw new IllegalArgumentException("A mode setting needs at least two options");
        }
        this.options = options.clone();
        if (indexOf(defaultValue) < 0) {
            throw new IllegalArgumentException("Default value is not one of the options: " + defaultValue);
        }
    }

    public String[] options() {
        return options.clone();
    }

    public int optionCount() {
        return options.length;
    }

    public String option(int index) {
        return options[index];
    }

    public int index() {
        return indexOf(get());
    }

    public void select(int index) {
        if (index >= 0 && index < options.length) {
            set(options[index]);
        }
    }

    public boolean is(String option) {
        return get().equals(option);
    }

    public boolean open() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    @Override
    public void cycle() {
        select((index() + 1) % options.length);
    }

    private int indexOf(String value) {
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(value)) {
                return i;
            }
        }
        return -1;
    }
}