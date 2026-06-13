package com.spaceline.common.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an event listener. The method must be {@code public},
 * return {@code void} and accept exactly one parameter that is an {@link Event}
 * subtype. Register the owning object with {@link EventBus#register(Object)}.
 *
 * <pre>{@code
 * @Subscribe(priority = EventPriority.HIGH)
 * public void onTick(ClientTickEvent event) { ... }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subscribe {

    EventPriority priority() default EventPriority.NORMAL;

    /**
     * When {@code false} (the default) the listener is skipped if the event has
     * already been cancelled by a higher-priority listener.
     */
    boolean ignoreCancelled() default false;
}
