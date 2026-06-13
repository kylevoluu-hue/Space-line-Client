package com.spaceline.common.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * A bounded numeric value rendered as a slider. The {@link #step()} controls
 * slider granularity; {@link #sanitize(Double)} clamps to {@code [min, max]}.
 */
public final class NumberSetting extends Setting<Double> {

    private final double min;
    private final double max;
    private final double step;

    public NumberSetting(String key, String displayName, String description,
                         double defaultValue, double min, double max, double step) {
        super(key, displayName, description, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }

    public double step() {
        return step;
    }

    public int asInt() {
        return (int) Math.round(get());
    }

    public float asFloat() {
        return get().floatValue();
    }

    @Override
    protected Double sanitize(Double candidate) {
        double v = candidate == null ? min : candidate;
        return Math.max(min, Math.min(max, v));
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(get());
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element != null && element.isJsonPrimitive()) {
            set(element.getAsDouble());
        }
    }
}
