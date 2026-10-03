package com.coldclient.hud;

/**
 * A draggable HUD block. Position is stored as screen fractions so it survives window resizes.
 * anchorX is the RIGHT edge when alignRight is true, otherwise the LEFT edge; this keeps the block
 * growing away from the side it is docked to (and gives the module list its right/left alignment).
 */
public final class HudElement {
    public final String moduleName;
    public float anchorX;
    public float anchorY;
    public boolean alignRight;

    private final float defaultX;
    private final float defaultY;
    private final boolean defaultRight;

    /** Measured size (GUI units) from the most recent draw. */
    public float width = 80f;
    public float height = 20f;

    public HudElement(String moduleName, float x, float y, boolean alignRight) {
        this.moduleName = moduleName;
        this.anchorX = this.defaultX = x;
        this.anchorY = this.defaultY = y;
        this.alignRight = this.defaultRight = alignRight;
    }

    public float left(float screenW) {
        return alignRight ? anchorX * screenW - width : anchorX * screenW;
    }

    public float top(float screenH) {
        return anchorY * screenH;
    }

    /** Moves the block so its top-left corner is at (left, top), re-docking to the nearer side. */
    public void moveTo(float left, float top, float screenW, float screenH) {
        left = Math.max(0f, Math.min(left, screenW - width));
        top = Math.max(0f, Math.min(top, screenH - height));
        alignRight = left + width * 0.5f > screenW * 0.5f;
        anchorX = (alignRight ? left + width : left) / screenW;
        anchorY = top / screenH;
    }

    public void reset() {
        anchorX = defaultX;
        anchorY = defaultY;
        alignRight = defaultRight;
    }
}
