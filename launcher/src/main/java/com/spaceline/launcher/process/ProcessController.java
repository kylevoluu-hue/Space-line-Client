package com.spaceline.launcher.process;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Owns every running {@link GameProcess} and implements the launcher's process
 * control surface: play (start), stop (graceful), force-kill, restart, crash
 * detection and the auto-restart policy.
 *
 * <p>One controller is shared across the whole launcher. It keys live processes
 * by instance id so an instance cannot be launched twice concurrently, and it
 * watches each process on a background thread to fire crash/exit callbacks.
 */
public final class ProcessController {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessController.class);
    private static final int LOG_CAPACITY = 5000;

    private final Map<String, GameProcess> running = new ConcurrentHashMap<>();
    private final CrashDetector crashDetector = new CrashDetector();

    /** Callback fired (off the watcher thread's hot path) when a process exits. */
    public interface ExitListener {
        void onExit(String instanceId, CrashDetector.Result result, boolean autoRestarting);
    }

    private volatile ExitListener exitListener = (id, result, restart) -> { };

    public void setExitListener(ExitListener listener) {
        this.exitListener = listener;
    }

    public boolean isRunning(String instanceId) {
        GameProcess process = running.get(instanceId);
        return process != null && process.isAlive();
    }

    public List<String> runningInstances() {
        return List.copyOf(running.keySet());
    }

    public java.util.Optional<GameProcess> process(String instanceId) {
        return java.util.Optional.ofNullable(running.get(instanceId));
    }

    /**
     * Starts a process from a fully-resolved command line.
     *
     * @param instanceId   the instance being launched
     * @param command      the complete process command (java + args)
     * @param workingDir   the instance game directory
     * @param environment  extra environment variables (may be empty)
     * @param autoRestart  whether to relaunch automatically on a crash
     */
    public synchronized GameProcess start(String instanceId, List<String> command, Path workingDir,
                                          Map<String, String> environment, boolean autoRestart) throws IOException {
        if (isRunning(instanceId)) {
            throw new IllegalStateException("Instance '" + instanceId + "' is already running");
        }
        LOG.info("Launching instance {}: {}", instanceId, String.join(" ", command));
        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(workingDir.toFile())
                .redirectErrorStream(true);
        builder.environment().putAll(environment);

        Process raw = builder.start();
        LogBuffer buffer = new LogBuffer(LOG_CAPACITY);
        GameProcess gameProcess = new GameProcess(instanceId, raw, buffer);
        running.put(instanceId, gameProcess);
        watch(gameProcess, command, workingDir, environment, autoRestart);
        return gameProcess;
    }

    /** Gracefully stops a running instance, escalating to force-kill on timeout. */
    public void stop(String instanceId, Duration graceful) {
        GameProcess process = running.get(instanceId);
        if (process == null) {
            return;
        }
        try {
            if (!process.stop(graceful)) {
                LOG.warn("Instance {} did not stop within {}; force killing", instanceId, graceful);
                process.forceKill();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.forceKill();
        }
    }

    public void forceKill(String instanceId) {
        GameProcess process = running.get(instanceId);
        if (process != null) {
            process.forceKill();
        }
    }

    /** Stops the instance (if running) and starts it again with the same command. */
    public void restart(String instanceId, List<String> command, Path workingDir,
                        Map<String, String> environment, boolean autoRestart) throws IOException {
        stop(instanceId, Duration.ofSeconds(10));
        start(instanceId, command, workingDir, environment, autoRestart);
    }

    private void watch(GameProcess process, List<String> command, Path workingDir,
                       Map<String, String> environment, boolean autoRestart) {
        Thread watcher = new Thread(() -> {
            int exitCode;
            try {
                exitCode = process.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            running.remove(process.instanceId());

            String recentLog = String.join("\n", process.logBuffer().snapshot());
            CrashDetector.Result result =
                    crashDetector.classify(exitCode, process.wasUserRequested(), recentLog);
            LOG.info("Instance {} exited: {} (code {})",
                    process.instanceId(), result.summary(), exitCode);

            boolean restarting = autoRestart && result.isCrash();
            exitListener.onExit(process.instanceId(), result, restarting);

            if (restarting) {
                attemptAutoRestart(process.instanceId(), command, workingDir, environment);
            }
        }, "spaceline-watch-" + process.instanceId());
        watcher.setDaemon(true);
        watcher.start();
    }

    private void attemptAutoRestart(String instanceId, List<String> command, Path workingDir,
                                    Map<String, String> environment) {
        try {
            LOG.info("Auto-restarting crashed instance {}", instanceId);
            // Auto-restart keeps auto-restart enabled so repeated crashes can be
            // observed; the caller's exit listener is responsible for backing off.
            start(instanceId, command, workingDir, environment, true);
        } catch (IOException e) {
            LOG.error("Failed to auto-restart instance {}", instanceId, e);
        }
    }

    /** Force-kills every running instance — used on launcher shutdown. */
    public void shutdownAll() {
        running.forEach((id, process) -> process.forceKill());
        running.clear();
    }
}
