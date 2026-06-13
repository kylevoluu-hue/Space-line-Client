package com.spaceline.common.event;

/**
 * Implemented by events whose default behaviour can be suppressed by a listener.
 *
 * <p>The {@link EventBus} keeps dispatching a cancellable event to remaining
 * listeners even after it has been cancelled, unless a listener opts out via
 * {@link Subscribe#ignoreCancelled()}. This mirrors the behaviour modders expect
 * from Forge/Fabric-style buses and lets a high-priority listener veto an action
 * while lower-priority observers still see that it happened.
 */
public interface Cancellable {

    boolean isCancelled();

    void setCancelled(boolean cancelled);

    default void cancel() {
        setCancelled(true);
    }
}
