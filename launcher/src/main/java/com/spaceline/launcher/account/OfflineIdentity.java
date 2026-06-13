package com.spaceline.launcher.account;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Derives a stable offline UUID from a username, matching the vanilla
 * "OfflinePlayer:" scheme so the same name always maps to the same world data
 * and is consistent with other launchers.
 */
public final class OfflineIdentity {

    private OfflineIdentity() {
    }

    public static UUID uuidFor(String username) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
    }
}
