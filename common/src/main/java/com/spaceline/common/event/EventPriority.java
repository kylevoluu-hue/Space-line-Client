package com.spaceline.common.event;

/**
 * Listener ordering. Higher priority listeners run first, giving them the chance
 * to mutate or cancel an event before observers further down the chain see it.
 */
public enum EventPriority {
    HIGHEST(0),
    HIGH(1),
    NORMAL(2),
    LOW(3),
    LOWEST(4),
    MONITOR(5); // MONITOR runs last and should never mutate the event.

    private final int order;

    EventPriority(int order) {
        this.order = order;
    }

    public int order() {
        return order;
    }
}
