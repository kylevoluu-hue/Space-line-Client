package com.spaceline.launcher.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A single running Minecraft process and its lifecycle controls.
 *
 * <p>Wraps a {@link Process}, pumps its combined stdout/stderr into a
 * {@link LogBuffer} on a dedicated daemon thread, and exposes graceful stop,
 * force-kill and crash-classification. A "user requested" flag distinguishes an
 * intentional stop from a crash so the {@link CrashDetector} and any auto-restart
 * policy behave correctly.
 */
public final class GameProcess {

    private static final Logger LOG = LoggerFactory.getLogger(GameProcess.class);

    private final String instanceId;
    private final Process process;
    private final LogBuffer logBuffer;
    private final Instant startedAt;
    private final AtomicBoolean userRequestedStop = new AtomicBoolean(false);
    private final Thread pumpThread;

    GameProcess(String instanceId, Process process, LogBuffer logBuffer) {
        this.instanceId = instanceId;
        this.process = process;
        this.logBuffer = logBuffer;
        this.startedAt = Instant.now();
        this.pumpThread = startLogPump();
    }

    private Thread startLogPump() {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logBuffer.append(line);
                }
            } catch (IOException e) {
                LOG.debug("Log pump for instance {} ended", instanceId, e);
            }
        }, "spaceline-log-pump-" + instanceId);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    public String instanceId() {
        return instanceId;
    }

    public boolean isAlive() {
        return process.isAlive();
    }

    public long pid() {
        return process.pid();
    }

    public Duration uptime() {
        return Duration.between(startedAt, Instant.now());
    }

    public LogBuffer logBuffer() {
        return logBuffer;
    }

    public boolean wasUserRequested() {
        return userRequestedStop.get();
    }

    /**
     * Asks the game to shut down gracefully (SIGTERM / destroy), waiting up to
     * {@code timeout} before reporting failure. Marks the stop as user-requested
     * so it is not treated as a crash.
     *
     * @return {@code true} if the process exited within the timeout
     */
    public boolean stop(Duration timeout) throws InterruptedException {
        userRequestedStop.set(true);
        LOG.info("Requesting graceful stop of instance {} (pid {})", instanceId, pid());
        process.destroy();
        return process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    /** Immediately and forcibly terminates the process (SIGKILL). */
    public void forceKill() {
        userRequestedStop.set(true);
        LOG.warn("Force killing instance {} (pid {})", instanceId, pid());
        process.destroyForcibly();
    }

    /** Blocks until the process exits and returns its exit code. */
    public int waitFor() throws InterruptedException {
        int code = process.waitFor();
        pumpThread.join(Duration.ofSeconds(2).toMillis());
        return code;
    }

    public int exitCode() {
        return process.exitValue();
    }
}
