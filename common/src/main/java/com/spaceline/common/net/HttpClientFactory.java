package com.spaceline.common.net;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Shared {@link HttpClient} configuration. A single client (with its connection
 * pool) is reused across the launcher's network subsystems — version metadata,
 * downloads and the Modrinth/CurseForge browsers — to avoid leaking threads and
 * sockets.
 */
public final class HttpClientFactory {

    private static final HttpClient SHARED = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .version(HttpClient.Version.HTTP_2)
            .build();

    private HttpClientFactory() {
    }

    public static HttpClient shared() {
        return SHARED;
    }

    /** The User-Agent every Space~line request must send (API etiquette). */
    public static String userAgent() {
        return "SpaceLineClient/" + Build.VERSION + " (+https://github.com/kylevoluu-hue/space-line-client)";
    }
}
