package com.spaceline.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import com.spaceline.common.event.CancellableEvent;
import com.spaceline.common.event.Event;
import com.spaceline.common.event.EventBus;
import com.spaceline.common.event.EventPriority;
import com.spaceline.common.event.Subscribe;
import org.junit.jupiter.api.Test;

class EventBusTest {

    static final class Ping extends Event {
        int count;
    }

    static final class Action extends CancellableEvent {
    }

    @Test
    void dispatchesToListenerAndMutates() {
        EventBus bus = new EventBus();
        bus.register(new Object() {
            @Subscribe
            public void onPing(Ping ping) {
                ping.count++;
            }
        });
        Ping ping = bus.post(new Ping());
        assertEquals(1, ping.count);
    }

    @Test
    void honoursPriorityOrdering() {
        EventBus bus = new EventBus();
        List<String> order = new ArrayList<>();
        bus.register(new Object() {
            @Subscribe(priority = EventPriority.LOW)
            public void low(Ping p) {
                order.add("low");
            }

            @Subscribe(priority = EventPriority.HIGHEST)
            public void high(Ping p) {
                order.add("high");
            }
        });
        bus.post(new Ping());
        assertEquals(List.of("high", "low"), order);
    }

    @Test
    void skipsCancelledUnlessIgnoring() {
        EventBus bus = new EventBus();
        List<String> seen = new ArrayList<>();
        bus.register(new Object() {
            @Subscribe(priority = EventPriority.HIGHEST)
            public void cancel(Action a) {
                a.cancel();
            }

            @Subscribe(priority = EventPriority.LOW)
            public void afterDefault(Action a) {
                seen.add("default");
            }

            @Subscribe(priority = EventPriority.MONITOR, ignoreCancelled = true)
            public void monitor(Action a) {
                seen.add("monitor");
            }
        });
        Action action = bus.post(new Action());
        assertTrue(action.isCancelled());
        assertTrue(seen.contains("default"));
        assertFalse(seen.contains("monitor"));
    }

    @Test
    void unregisterStopsDelivery() {
        EventBus bus = new EventBus();
        int[] hits = {0};
        Object listener = new Object() {
            @Subscribe
            public void onPing(Ping p) {
                hits[0]++;
            }
        };
        bus.register(listener);
        bus.post(new Ping());
        bus.unregister(listener);
        bus.post(new Ping());
        assertEquals(1, hits[0]);
    }
}
