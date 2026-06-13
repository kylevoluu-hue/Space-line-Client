package com.spaceline.launcher.account;

import java.time.Instant;
import java.util.UUID;

/**
 * A launcher account — either a Microsoft account or a local offline profile.
 *
 * <p>The fields mirror what a launch needs to supply to the game: the player's
 * UUID and name, plus (for Microsoft accounts) the Minecraft access token and
 * its refresh token. Tokens are mutable because they are refreshed in place; the
 * identity fields are effectively final once created.
 */
public final class Account {

    private final AuthType type;
    private final UUID uuid;
    private String username;

    // Microsoft-only fields. Null/blank for offline accounts.
    private String minecraftAccessToken;
    private String microsoftRefreshToken;
    private Instant accessTokenExpiry;

    private Account(AuthType type, UUID uuid, String username) {
        this.type = type;
        this.uuid = uuid;
        this.username = username;
    }

    public static Account offline(String username) {
        return new Account(AuthType.OFFLINE, OfflineIdentity.uuidFor(username), username);
    }

    public static Account microsoft(UUID uuid, String username, String accessToken,
                                    String refreshToken, Instant expiry) {
        Account account = new Account(AuthType.MICROSOFT, uuid, username);
        account.minecraftAccessToken = accessToken;
        account.microsoftRefreshToken = refreshToken;
        account.accessTokenExpiry = expiry;
        return account;
    }

    public AuthType type() {
        return type;
    }

    public UUID uuid() {
        return uuid;
    }

    public String username() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isOnline() {
        return type == AuthType.MICROSOFT;
    }

    /** The access token to pass to the game, or a placeholder for offline play. */
    public String accessToken() {
        return isOnline() ? minecraftAccessToken : "0";
    }

    public String refreshToken() {
        return microsoftRefreshToken;
    }

    public void updateTokens(String accessToken, String refreshToken, Instant expiry) {
        this.minecraftAccessToken = accessToken;
        this.microsoftRefreshToken = refreshToken;
        this.accessTokenExpiry = expiry;
    }

    /** Whether the Minecraft access token has expired (with a safety margin). */
    public boolean needsRefresh() {
        if (!isOnline()) {
            return false;
        }
        return accessTokenExpiry == null
                || Instant.now().isAfter(accessTokenExpiry.minusSeconds(60));
    }

    public Instant accessTokenExpiry() {
        return accessTokenExpiry;
    }
}
