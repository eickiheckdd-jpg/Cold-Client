package com.coldclient.setting;

/** A button in the config panel that runs an action when clicked. */
public final class ActionSetting extends Setting<Boolean> {
    private final String buttonText;
    private final Runnable action;

    public ActionSetting(String name, String description, String buttonText, Runnable action) {
        super(name, description, Boolean.FALSE);
        this.buttonText = buttonText;
        this.action = action;
    }

    public String buttonText() {
        return buttonText;
    }

    public void run() {
        action.run();
    }

    @Override
    public void cycle() {
        run();
    }
}