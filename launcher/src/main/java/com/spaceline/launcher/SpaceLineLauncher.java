package com.spaceline.launcher;

import java.io.IOException;
import java.util.List;

import com.spaceline.launcher.java.JavaRuntime;
import com.spaceline.launcher.version.MinecraftVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the Space~line launcher.
 *
 * <p>The desktop GUI is a separate front-end module; this entry point boots the
 * {@link LauncherContext} (the headless application core) and supports a small
 * diagnostic CLI so the launcher can be smoke-tested and scripted without a
 * display — useful in CI and on headless servers. Run with {@code --help} to see
 * the available commands.
 */
public final class SpaceLineLauncher {

    private static final Logger LOG = LoggerFactory.getLogger(SpaceLineLauncher.class);

    public static void main(String[] args) {
        LOG.info("Space~line Client launcher starting");
        LauncherContext context = new LauncherContext();
        context.initialize();

        String command = args.length > 0 ? args[0] : "status";
        try {
            switch (command) {
                case "status" -> printStatus(context);
                case "versions" -> printVersions(context);
                case "java" -> printJava(context);
                case "accounts" -> printAccounts(context);
                case "add-offline" -> addOffline(context, args);
                case "--help", "help" -> printHelp();
                default -> {
                    System.err.println("Unknown command: " + command);
                    printHelp();
                    System.exit(2);
                }
            }
        } catch (IOException e) {
            LOG.error("Command '{}' failed", command, e);
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void printStatus(LauncherContext context) {
        System.out.println("Space~line Client");
        System.out.println("  Data directory : " + context.paths().root());
        System.out.println("  Accounts       : " + context.accounts().accounts().size());
        System.out.println("  Instances      : " + context.instances().list().size());
        System.out.println("  Java runtimes  : " + context.java().supported().size() + " supported");
        System.out.println("  Version adapters: " + context.adapters().all().size());
        System.out.println();
        System.out.println("Run 'help' for available commands.");
    }

    private static void printVersions(LauncherContext context) throws IOException {
        List<MinecraftVersion> versions = context.versions().supportedVersions();
        System.out.println("Supported stable releases (newest first):");
        versions.forEach(v -> System.out.println("  " + v.id() + "  (" + v.releaseTime() + ")"));
        System.out.println("Total: " + versions.size());
    }

    private static void printJava(LauncherContext context) {
        List<JavaRuntime> runtimes = context.java().detectAll();
        System.out.println("Detected Java runtimes:");
        for (JavaRuntime runtime : runtimes) {
            System.out.println("  " + (runtime.isSupported() ? "[ok]  " : "[skip]") + " " + runtime.describe());
        }
    }

    private static void printAccounts(LauncherContext context) {
        var accounts = context.accounts().accounts();
        if (accounts.isEmpty()) {
            System.out.println("No accounts. Add one with: add-offline <username>");
            return;
        }
        var active = context.accounts().active().orElse(null);
        accounts.forEach(account -> System.out.println(
                (account.equals(active) ? "* " : "  ")
                        + account.username() + " (" + account.type() + ")"));
    }

    private static void addOffline(LauncherContext context, String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: add-offline <username>");
            System.exit(2);
            return;
        }
        var account = context.accounts().addOffline(args[1]);
        System.out.println("Added offline account: " + account.username() + " (" + account.uuid() + ")");
    }

    private static void printHelp() {
        System.out.println("""
                Space~line Client launcher — diagnostic CLI

                Usage: spaceline <command> [args]

                Commands:
                  status              Show launcher data, account and instance summary (default)
                  versions            List installable stable Minecraft releases (>= 1.21)
                  java                List detected Java runtimes and which are supported
                  accounts            List configured accounts
                  add-offline <name>  Create an offline account
                  help                Show this help

                The full graphical launcher is provided by the desktop front-end.""");
    }

    private SpaceLineLauncher() {
    }
}
