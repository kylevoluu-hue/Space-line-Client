package com.spaceline.launcher;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import com.spaceline.launcher.account.Account;
import com.spaceline.launcher.instance.Instance;
import com.spaceline.launcher.instance.ModLoader;
import com.spaceline.launcher.java.JavaRuntime;
import com.spaceline.launcher.launch.GameLauncher;
import com.spaceline.launcher.process.GameProcess;
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
                case "create-instance" -> createInstance(context, args);
                case "instances" -> listInstances(context);
                case "launch" -> launch(context, args);
                case "stop" -> stop(context, args);
                case "--help", "help" -> printHelp();
                default -> {
                    System.err.println("Unknown command: " + command);
                    printHelp();
                    System.exit(2);
                }
            }
        } catch (Exception e) {
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

    private static void createInstance(LauncherContext context, String[] args) throws IOException {
        if (args.length < 4) {
            System.err.println("Usage: create-instance <name> <mcVersion> <vanilla|fabric>");
            System.exit(2);
            return;
        }
        String name = args[1];
        String mcVersion = args[2];
        ModLoader loader = ModLoader.fromId(args[3]);

        // Confirm the requested version is an installable stable release.
        boolean supported = context.versions().supportedVersions().stream()
                .anyMatch(v -> v.id().equals(mcVersion));
        if (!supported) {
            System.err.println("'" + mcVersion + "' is not an installable stable release (>= "
                    + MinecraftVersion.MINIMUM_SUPPORTED + "). Run 'versions' to see the list.");
            System.exit(1);
            return;
        }

        Instance instance = context.instances().create(name, mcVersion, loader);
        if (loader == ModLoader.FABRIC) {
            String loaderVersion = context.fabric().latestStableLoader();
            instance.setFabricLoaderVersion(loaderVersion);
            context.instances().save(instance);
            System.out.println("Resolved Fabric loader " + loaderVersion);
        }
        System.out.println("Created instance '" + instance.id() + "' (" + mcVersion + " " + loader.id() + ")");
        System.out.println("Launch it with: launch " + instance.id());
    }

    private static void listInstances(LauncherContext context) {
        List<Instance> instances = context.instances().list();
        if (instances.isEmpty()) {
            System.out.println("No instances. Create one with: create-instance <name> <mcVersion> <vanilla|fabric>");
            return;
        }
        System.out.println("Instances:");
        for (Instance instance : instances) {
            boolean running = context.processes().isRunning(instance.id());
            System.out.printf("  %-24s %s %-8s %s%n", instance.id(), instance.minecraftVersion(),
                    instance.loader().id(), running ? "[running]" : "");
        }
    }

    private static void launch(LauncherContext context, String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: launch <instanceId>");
            System.exit(2);
            return;
        }
        String instanceId = args[1];
        Instance instance = context.instances().get(instanceId).orElse(null);
        if (instance == null) {
            System.err.println("No instance '" + instanceId + "'. Run 'instances' to list them.");
            System.exit(1);
            return;
        }
        Account account = context.accounts().active().orElse(null);
        if (account == null) {
            System.err.println("No active account. Add one with: add-offline <username>");
            System.exit(1);
            return;
        }

        System.out.println("Preparing '" + instance.id() + "' — first launch downloads Minecraft and may take a while...");
        try {
            GameProcess process = context.launcher().launch(instance, account);
            System.out.println("Minecraft started (pid " + process.pid() + "). Streaming logs; press Ctrl+C to detach.");
            process.logBuffer().addListener(System.out::println);
            int exitCode = process.waitFor();
            System.out.println("Minecraft exited with code " + exitCode);
        } catch (GameLauncher.LaunchException e) {
            System.err.println("Launch failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void stop(LauncherContext context, String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: stop <instanceId>");
            System.exit(2);
            return;
        }
        context.processes().stop(args[1], Duration.ofSeconds(10));
        System.out.println("Requested stop of '" + args[1] + "'");
    }

    private static void printHelp() {
        System.out.println("""
                Space~line Client launcher - diagnostic CLI

                Usage: spaceline <command> [args]

                Commands:
                  status                                 Launcher data, account and instance summary (default)
                  versions                               List installable stable Minecraft releases (>= 1.21)
                  java                                   List detected Java runtimes
                  accounts                               List configured accounts
                  add-offline <name>                     Create an offline account
                  create-instance <name> <ver> <loader>  Create an instance (loader: vanilla|fabric)
                  instances                              List instances
                  launch <instanceId>                    Download (if needed) and start Minecraft
                  stop <instanceId>                      Gracefully stop a running instance
                  help                                   Show this help

                Example:
                  add-offline Kyle
                  create-instance MyPack 1.21.4 fabric
                  launch mypack""");
    }

    private SpaceLineLauncher() {
    }
}
