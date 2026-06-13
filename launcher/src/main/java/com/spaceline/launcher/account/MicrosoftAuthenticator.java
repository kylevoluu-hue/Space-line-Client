package com.spaceline.launcher.account;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import com.google.gson.JsonObject;
import com.spaceline.common.net.Http;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements Microsoft sign-in via the OAuth 2.0 <em>device code</em> flow and
 * the subsequent Xbox Live → XSTS → Minecraft token exchange.
 *
 * <p>The device code flow is the right fit for a desktop launcher: the user is
 * shown a short code and a URL to enter it at, and the launcher polls in the
 * background until they finish — no embedded browser or redirect server needed.
 *
 * <p>The Azure application (public client) id is supplied at construction. Ship
 * your own by registering an app at the Azure portal and enabling the
 * "Live SDK"/Xbox scopes; the well-known Minecraft client id also works for
 * personal use.
 */
public final class MicrosoftAuthenticator {

    private static final Logger LOG = LoggerFactory.getLogger(MicrosoftAuthenticator.class);

    // The legacy live.com device-code endpoints. Unlike the Azure AD v2 endpoints
    // (which require you to register your own app), these work with the
    // well-known public Minecraft client id, so sign-in works out of the box.
    private static final String DEVICE_CODE_URL = "https://login.live.com/oauth20_connect.srf";
    private static final String TOKEN_URL = "https://login.live.com/oauth20_token.srf";
    private static final String XBL_URL = "https://user.auth.xboxlive.com/user/authenticate";
    private static final String XSTS_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";
    private static final String MC_LOGIN_URL =
            "https://api.minecraftservices.com/authentication/login_with_xbox";
    private static final String MC_PROFILE_URL =
            "https://api.minecraftservices.com/minecraft/profile";
    private static final String SCOPE = "service::user.auth.xboxlive.com::MBI_SSL";

    private final String clientId;

    public MicrosoftAuthenticator(String clientId) {
        this.clientId = clientId;
    }

    /** The information the UI shows the user to begin device-code sign-in. */
    public record DeviceCodePrompt(String userCode, String verificationUri, String deviceCode,
                                   int intervalSeconds, int expiresInSeconds) {
    }

    /**
     * Step 1: request a device code. The returned prompt must be shown to the
     * user; then call {@link #pollForToken(DeviceCodePrompt, Consumer)}.
     */
    public DeviceCodePrompt requestDeviceCode() throws IOException {
        String body = form(Map.of(
                "client_id", clientId,
                "scope", SCOPE,
                "response_type", "device_code"));
        JsonObject response = Http.postJson(DEVICE_CODE_URL, body,
                Map.of("Content-Type", "application/x-www-form-urlencoded")).getAsJsonObject();
        return new DeviceCodePrompt(
                response.get("user_code").getAsString(),
                response.get("verification_uri").getAsString(),
                response.get("device_code").getAsString(),
                response.has("interval") ? response.get("interval").getAsInt() : 5,
                response.has("expires_in") ? response.get("expires_in").getAsInt() : 900);
    }

    /**
     * Step 2: poll until the user authorises (or the code expires), then run the
     * full token exchange and return a ready-to-use {@link Account}.
     *
     * @param status optional progress callback for the UI (may be {@code null})
     */
    public Account pollForToken(DeviceCodePrompt prompt, Consumer<String> status) throws IOException {
        long deadline = System.currentTimeMillis() + prompt.expiresInSeconds() * 1000L;
        report(status, "Waiting for you to sign in...");
        while (System.currentTimeMillis() < deadline) {
            sleep(prompt.intervalSeconds() * 1000L);
            JsonObject token = tryRedeem(prompt.deviceCode());
            if (token == null) {
                continue; // still pending
            }
            String msAccessToken = token.get("access_token").getAsString();
            String refreshToken = token.has("refresh_token")
                    ? token.get("refresh_token").getAsString() : "";
            return completeExchange(msAccessToken, refreshToken, status);
        }
        throw new IOException("Device code expired before sign-in completed");
    }

    /**
     * Refreshes a Microsoft account's tokens using its stored refresh token,
     * producing a fresh Minecraft access token. Called transparently before
     * launch when {@link Account#needsRefresh()} is true.
     */
    public Account refresh(Account account) throws IOException {
        String body = form(Map.of(
                "client_id", clientId,
                "grant_type", "refresh_token",
                "refresh_token", account.refreshToken()));
        JsonObject token = Http.postJson(TOKEN_URL, body,
                Map.of("Content-Type", "application/x-www-form-urlencoded")).getAsJsonObject();
        String msAccessToken = token.get("access_token").getAsString();
        String newRefresh = token.has("refresh_token")
                ? token.get("refresh_token").getAsString() : account.refreshToken();
        return completeExchange(msAccessToken, newRefresh, null);
    }

    // ------------------------------------------------------------------
    // Token exchange chain: MS -> XBL -> XSTS -> Minecraft -> profile.
    // ------------------------------------------------------------------

    private Account completeExchange(String msAccessToken, String refreshToken,
                                     Consumer<String> status) throws IOException {
        report(status, "Authenticating with Xbox Live...");
        JsonObject xbl = authenticateXbox(msAccessToken);
        String xblToken = xbl.get("Token").getAsString();
        String userHash = xbl.getAsJsonObject("DisplayClaims")
                .getAsJsonArray("xui").get(0).getAsJsonObject()
                .get("uhs").getAsString();

        report(status, "Authorising XSTS...");
        String xstsToken = authorizeXsts(xblToken).get("Token").getAsString();

        report(status, "Logging into Minecraft services...");
        JsonObject mc = loginWithXbox(userHash, xstsToken);
        String mcAccessToken = mc.get("access_token").getAsString();
        int expiresIn = mc.has("expires_in") ? mc.get("expires_in").getAsInt() : 86400;

        report(status, "Fetching profile...");
        JsonObject profile = fetchProfile(mcAccessToken);
        UUID uuid = parseUuid(profile.get("id").getAsString());
        String name = profile.get("name").getAsString();

        LOG.info("Signed in Microsoft account {} ({})", name, uuid);
        return Account.microsoft(uuid, name, mcAccessToken, refreshToken,
                Instant.now().plusSeconds(expiresIn));
    }

    private JsonObject tryRedeem(String deviceCode) throws IOException {
        String body = form(Map.of(
                "client_id", clientId,
                "grant_type", "urn:ietf:params:oauth:grant-type:device_code",
                "device_code", deviceCode));
        try {
            return Http.postJson(TOKEN_URL, body,
                    Map.of("Content-Type", "application/x-www-form-urlencoded")).getAsJsonObject();
        } catch (IOException e) {
            // authorization_pending / slow_down come back as HTTP 400; treat any
            // failure here as "keep polling" — a truly expired code ends the loop.
            return null;
        }
    }

    private JsonObject authenticateXbox(String msAccessToken) throws IOException {
        // For legacy live.com (MBI_SSL) tokens the RpsTicket is the raw access
        // token with no "d=" prefix (that prefix is only for Azure AD tokens).
        String payload = """
                {"Properties":{"AuthMethod":"RPS","SiteName":"user.auth.xboxlive.com",\
                "RpsTicket":"%s"},"RelyingParty":"http://auth.xboxlive.com",\
                "TokenType":"JWT"}""".formatted(msAccessToken);
        return Http.postJson(XBL_URL, payload, jsonHeaders()).getAsJsonObject();
    }

    private JsonObject authorizeXsts(String xblToken) throws IOException {
        String payload = """
                {"Properties":{"SandboxId":"RETAIL","UserTokens":["%s"]},\
                "RelyingParty":"rp://api.minecraftservices.com/","TokenType":"JWT"}"""
                .formatted(xblToken);
        return Http.postJson(XSTS_URL, payload, jsonHeaders()).getAsJsonObject();
    }

    private JsonObject loginWithXbox(String userHash, String xstsToken) throws IOException {
        String payload = "{\"identityToken\":\"XBL3.0 x=%s;%s\"}".formatted(userHash, xstsToken);
        return Http.postJson(MC_LOGIN_URL, payload, jsonHeaders()).getAsJsonObject();
    }

    private JsonObject fetchProfile(String mcAccessToken) throws IOException {
        return Http.getJson(MC_PROFILE_URL, Map.of("Authorization", "Bearer " + mcAccessToken))
                .getAsJsonObject();
    }

    private static Map<String, String> jsonHeaders() {
        return Map.of("Content-Type", "application/json", "Accept", "application/json");
    }

    private static String form(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        params.forEach((k, v) -> {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(URLEncoder.encode(k, StandardCharsets.UTF_8))
              .append('=')
              .append(URLEncoder.encode(v, StandardCharsets.UTF_8));
        });
        return sb.toString();
    }

    private static UUID parseUuid(String undashed) {
        String dashed = undashed.replaceFirst(
                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})",
                "$1-$2-$3-$4-$5");
        return UUID.fromString(dashed);
    }

    private static void report(Consumer<String> status, String message) {
        LOG.debug(message);
        if (status != null) {
            status.accept(message);
        }
    }

    private static void sleep(long millis) throws IOException {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Sign-in interrupted", e);
        }
    }
}
