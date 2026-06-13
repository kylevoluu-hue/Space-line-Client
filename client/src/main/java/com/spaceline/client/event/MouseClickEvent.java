package com.spaceline.client.event;

import com.spaceline.common.event.Event;

/**
 * Fired on a mouse button press. Feeds click-tracking modules such as CPS and
 * the combo display.
 */
public final class MouseClickEvent extends Event {

    public enum Button { LEFT, RIGHT, MIDDLE }

    private final Button button;

    public MouseClickEvent(Button button) {
        this.button = button;
    }

    public Button button() {
        return button;
    }
}
