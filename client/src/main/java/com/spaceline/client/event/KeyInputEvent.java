package com.spaceline.client.event;

import com.spaceline.common.event.Event;

/**
 * Fired when a key is pressed. The engine routes these to module keybinds; a
 * module may also subscribe directly for richer input handling (e.g. hold-to-zoom).
 */
public final class KeyInputEvent extends Event {

    private final int keyCode;
    private final boolean pressed;

    public KeyInputEvent(int keyCode, boolean pressed) {
        this.keyCode = keyCode;
        this.pressed = pressed;
    }

    public int keyCode() {
        return keyCode;
    }

    /** True on key-down, false on key-up. */
    public boolean pressed() {
        return pressed;
    }
}
