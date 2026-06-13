package com.spaceline.common.net;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thin, synchronous convenience layer over {@link java.net.http.HttpClient} for
 * the JSON REST calls and file downloads the launcher performs. Keeps retry,
 * timeout and User-Agent policy in one place.
 */
public final class Http {

    private static final Logger LOG = LoggerFactory.getLogger(Http.class);
    private static final int MAX_ATTEMPTS = 4;

    private Http() {
    }

    /** GETs {@code url} and parses the body as JSON, with bounded retries. */
    public static JsonElement getJson(String url, Map<String, String> headers) throws IOException {
        HttpRequest.Builder builder = baseRequest(url).GET();
        headers.forEach(builder::header);
        HttpResponse<String> response = sendWithRetry(builder.build(),
                HttpResponse.BodyHandlers.ofString());
        ensureSuccess(url, response.statusCode(), response.body());
        return JsonParser.parseString(response.body());
    }

    public static JsonElement getJson(String url) throws IOException {
        return getJson(url, Map.of());
    }

    /** POSTs a body and parses the JSON response. */
    public static JsonElement postJson(String url, String body, Map<String, String> headers) throws IOException {
        HttpRequest.Builder builder = baseRequest(url)
                .POST(HttpRequest.BodyPublishers.ofString(body));
        // Only default to JSON when the caller hasn't specified a content type;
        // HttpRequest.Builder#header APPENDS, so setting it unconditionally would
        // send two Content-Type values (e.g. json + form-urlencoded) and the
        // server would reject the request.
        boolean callerSetContentType = headers.keySet().stream()
                .anyMatch(k -> k.equalsIgnoreCase("Content-Type"));
        if (!callerSetContentType) {
            builder.header("Content-Type", "application/json");
        }
        headers.forEach(builder::header);
        HttpResponse<String> response = sendWithRetry(builder.build(),
                HttpResponse.BodyHandlers.ofString());
        ensureSuccess(url, response.statusCode(), response.body());
        return JsonParser.parseString(response.body());
    }

    /**
     * Downloads {@code url} to {@code destination} atomically (temp file then
     * move). Parent directories are created as needed.
     */
    public static void download(String url, Path destination) throws IOException {
        Files.createDirectories(destination.getParent());
        Path tmp = destination.resolveSibling(destination.getFileName() + ".part");
        HttpResponse<Path> response = sendWithRetry(baseRequest(url).GET().build(),
                HttpResponse.BodyHandlers.ofFile(tmp));
        ensureSuccess(url, response.statusCode());
        Files.move(tmp, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    private static HttpRequest.Builder baseRequest(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(5))
                .header("User-Agent", HttpClientFactory.userAgent());
    }

    private static <T> HttpResponse<T> sendWithRetry(HttpRequest request,
                                                     HttpResponse.BodyHandler<T> handler) throws IOException {
        IOException last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return HttpClientFactory.shared().send(request, handler);
            } catch (IOException e) {
                last = e;
                long backoff = (long) Math.pow(2, attempt) * 250L;
                LOG.warn("Request {} failed (attempt {}/{}), retrying in {}ms",
                        request.uri(), attempt, MAX_ATTEMPTS, backoff, e);
                sleep(backoff);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted during HTTP request to " + request.uri(), e);
            }
        }
        throw new IOException("Exhausted retries for " + request.uri(), last);
    }

    private static void ensureSuccess(String url, int status) throws IOException {
        ensureSuccess(url, status, null);
    }

    private static void ensureSuccess(String url, int status, String body) throws IOException {
        if (status < 200 || status >= 300) {
            String detail = "";
            if (body != null && !body.isBlank()) {
                String trimmed = body.strip();
                detail = ": " + (trimmed.length() > 300 ? trimmed.substring(0, 300) + "…" : trimmed);
            }
            throw new IOException("HTTP " + status + " from " + url + detail);
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
