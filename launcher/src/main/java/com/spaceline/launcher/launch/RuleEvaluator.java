package com.spaceline.launcher.launch;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.spaceline.common.util.Platform;

/**
 * Evaluates the {@code rules} arrays that appear on libraries and arguments in
 * Mojang/Fabric version JSON. A rule has an {@code action} ("allow"/"disallow")
 * and optional {@code os}/{@code features} conditions; the net result decides
 * whether the item applies on this machine.
 *
 * <p>Feature flags (demo mode, custom resolution, …) are treated as off here,
 * which is correct for a normal launch; the launcher supplies window size via the
 * standard width/height arguments instead.
 */
public final class RuleEvaluator {

    private RuleEvaluator() {
    }

    /** @return whether an item guarded by {@code rules} applies on this OS. */
    public static boolean applies(JsonArray rules) {
        if (rules == null || rules.isEmpty()) {
            return true;
        }
        boolean allowed = false;
        for (JsonElement element : rules) {
            JsonObject rule = element.getAsJsonObject();
            boolean matches = matchesConditions(rule);
            if (matches) {
                allowed = "allow".equals(rule.get("action").getAsString());
            }
        }
        return allowed;
    }

    private static boolean matchesConditions(JsonObject rule) {
        if (rule.has("features")) {
            // No optional features are enabled for a standard launch.
            return false;
        }
        if (rule.has("os")) {
            JsonObject os = rule.getAsJsonObject("os");
            if (os.has("name") && !os.get("name").getAsString().equals(Platform.current().mojangName())) {
                return false;
            }
            if (os.has("arch")) {
                String arch = System.getProperty("os.arch", "");
                if (!arch.contains(os.get("arch").getAsString())) {
                    return false;
                }
            }
        }
        return true;
    }
}
