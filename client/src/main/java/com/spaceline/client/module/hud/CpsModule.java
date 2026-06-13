package com.spaceline.client.module.hud;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import com.spaceline.client.engine.GameState;
import com.spaceline.client.event.MouseClickEvent;
import com.spaceline.client.hud.HudModule;
import com.spaceline.common.event.Subscribe;
import com.spaceline.common.module.setting.BooleanSetting;

/**
 * Displays clicks-per-second, computed over a rolling one-second window from
 * {@link MouseClickEvent}s. Left and right click rates can be shown separately.
 */
public final class CpsModule extends HudModule {

    private static final long WINDOW_MS = 1000;

    private final BooleanSetting showRight;
    private final Deque<Long> leftClicks = new ArrayDeque<>();
    private final Deque<Long> rightClicks = new ArrayDeque<>();

    public CpsModule() {
        super("cps", "CPS", "Shows your clicks per second");
        this.showRight = bool("right", "Right CPS", "Also show right-click CPS", false);
    }

    @Subscribe
    public void onClick(MouseClickEvent event) {
        long now = System.currentTimeMillis();
        if (event.button() == MouseClickEvent.Button.LEFT) {
            leftClicks.addLast(now);
        } else if (event.button() == MouseClickEvent.Button.RIGHT) {
            rightClicks.addLast(now);
        }
    }

    @Override
    protected List<String> lines(GameState game) {
        long now = System.currentTimeMillis();
        int left = countWithin(leftClicks, now);
        if (!showRight.get()) {
            return List.of(left + " CPS");
        }
        int right = countWithin(rightClicks, now);
        return List.of("L " + left + " CPS", "R " + right + " CPS");
    }

    private int countWithin(Deque<Long> clicks, long now) {
        while (!clicks.isEmpty() && now - clicks.peekFirst() > WINDOW_MS) {
            clicks.pollFirst();
        }
        return clicks.size();
    }
}
