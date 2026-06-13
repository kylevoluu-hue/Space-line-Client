package com.spaceline.launcher.error;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes self-contained error/crash reports to the crash-reports directory.
 *
 * <p>A report bundles the failing context, a captured exception (if any) and the
 * tail of the live log so a user can attach a single file when asking for help.
 * Reports are written locally only — nothing is transmitted anywhere — keeping
 * the launcher privacy-respecting by default.
 */
public final class ErrorReporter {

    private static final Logger LOG = LoggerFactory.getLogger(ErrorReporter.class);
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final Path crashReportsDir;

    public ErrorReporter(Path crashReportsDir) {
        this.crashReportsDir = crashReportsDir;
    }

    /**
     * Writes a report and returns its path.
     *
     * @param context  what failed (e.g. "Launch instance pvp")
     * @param error    the throwable, or {@code null}
     * @param logTail  recent log lines to include
     */
    public Path report(String context, Throwable error, List<String> logTail) {
        String fileName = "spaceline-error-" + LocalDateTime.now().format(STAMP) + ".txt";
        Path file = crashReportsDir.resolve(fileName);
        StringBuilder sb = new StringBuilder();
        sb.append("Space~line Client error report\n");
        sb.append("==============================\n");
        sb.append("Time:    ").append(LocalDateTime.now()).append('\n');
        sb.append("Context: ").append(context).append('\n');
        sb.append("OS:      ").append(System.getProperty("os.name"))
                .append(' ').append(System.getProperty("os.version")).append('\n');
        sb.append("Java:    ").append(System.getProperty("java.version"))
                .append(" (").append(System.getProperty("java.vendor")).append(")\n\n");

        if (error != null) {
            sb.append("Exception:\n").append(stackTrace(error)).append('\n');
        }
        if (logTail != null && !logTail.isEmpty()) {
            sb.append("Recent log:\n");
            logTail.forEach(line -> sb.append("  ").append(line).append('\n'));
        }

        try {
            Files.createDirectories(crashReportsDir);
            Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
            LOG.info("Wrote error report to {}", file);
        } catch (IOException e) {
            LOG.error("Failed to write error report", e);
        }
        return file;
    }

    private static String stackTrace(Throwable error) {
        StringWriter writer = new StringWriter();
        error.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}
