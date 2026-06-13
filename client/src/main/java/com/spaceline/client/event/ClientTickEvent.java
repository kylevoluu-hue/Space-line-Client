package com.spaceline.client.event;

import com.spaceline.common.event.Event;

/**
 * Fired once per client tick (20 times/second). Modules that need periodic
 * updates independent of frame rate (timers, polling, state machines) subscribe
 * to this.
 */
public final class ClientTickEvent extends Event {

    private final long tick;

    public ClientTickEvent(long tick) {
        this.tick = tick;
    }

    /** A monotonically increasing tick counter since the client started. */
    public long tick() {
        return tick;
    }
}
