package com.spaceline.launcher.launch;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.spaceline.common.net.Build;
import com.spaceline.launcher.account.Account;
import com.spaceline.launcher.instance.Instance;
import com.spaceline.launcher.java.JavaRuntime;
import com.spaceline.launcher.version.adapter.VersionAdapter;

/**
 * Turns a resolved {@link GameInstallation} plus the chosen account, instance and
 * Java runtime into the exact process command line.
 *
 * <p>It assembles, in order: the Java binary, the adapter's recommended JVM
 * tuning, heap sizing and natives/classpath flags, the version JSON's own JVM
 * arguments, the main class, and finally the game arguments — substituting the
 * standard {@code ${...}} placeholders (player name, uuid, access token, asset
 * paths, …) along the way. OS-gated arguments are filtered via
 * {@link RuleEvaluator}.
 */
public final class LaunchCommandBuilder {

    private final VersionAdapter adapter;

    public LaunchCommandBuilder(VersionAdapter adapter) {
        this.adapter = adapter;
    }

    public List<String> build(GameInstallation installation, Account account, Instance instance,
                              JavaRuntime java, Path gameDir) {
        JsonObject versionJson = installation.versionJson();
        Map<String, String> vars = placeholders(installation, account, instance, gameDir);

        List<String> command = new ArrayList<>();
        command.add(java.executable().toString());

        // Heap + adapter-recommended GC tuning, then user JVM args.
        command.add("-Xms" + instance.minMemoryMb() + "M");
        command.add("-Xmx" + instance.maxMemoryMb() + "M");
        command.addAll(adapter.defaultJvmArgs());
        command.add("-Djava.library.path=" + installation.nativesDir());
        command.add("-Dminecraft.launcher.brand=spaceline");
        command.add("-Dminecraft.launcher.version=" + Build.VERSION);
        addUserJvmArgs(command, instance);

        // Version-JSON JVM args (classpath, natives placeholders, module flags).
        if (versionJson.has("arguments")) {
            command.addAll(resolveArgs(versionJson.getAsJsonObject("arguments"), "jvm", vars));
        } else {
            // Fallback for the unlikely legacy shape: supply classpath manually.
            command.add("-cp");
            command.add(vars.get("classpath"));
        }

        command.add(versionJson.get("mainClass").getAsString());

        // Game arguments.
        if (versionJson.has("arguments")) {
            command.addAll(resolveArgs(versionJson.getAsJsonObject("arguments"), "game", vars));
        } else if (versionJson.has("minecraftArguments")) {
            for (String token : versionJson.get("minecraftArguments").getAsString().split(" ")) {
                command.add(substitute(token, vars));
            }
        }

        // Window size from the instance.
        command.add("--width");
        command.add(Integer.toString(instance.windowWidth()));
        command.add("--height");
        command.add(Integer.toString(instance.windowHeight()));
        return command;
    }

    private void addUserJvmArgs(List<String> command, Instance instance) {
        String extra = instance.extraJvmArgs();
        if (extra != null && !extra.isBlank()) {
            for (String token : extra.trim().split("\\s+")) {
                command.add(token);
            }
        }
    }

    /** Resolves the {@code game}/{@code jvm} argument array, applying rules. */
    private List<String> resolveArgs(JsonObject arguments, String key, Map<String, String> vars) {
        List<String> result = new ArrayList<>();
        if (!arguments.has(key)) {
            return result;
        }
        for (JsonElement element : arguments.getAsJsonArray(key)) {
            if (element.isJsonPrimitive()) {
                result.add(substitute(element.getAsString(), vars));
            } else if (element.isJsonObject()) {
                JsonObject conditional = element.getAsJsonObject();
                JsonArray rules = conditional.has("rules")
                        ? conditional.getAsJsonArray("rules") : null;
                if (RuleEvaluator.applies(rules)) {
                    addValue(result, conditional.get("value"), vars);
                }
            }
        }
        return result;
    }

    private void addValue(List<String> out, JsonElement value, Map<String, String> vars) {
        if (value.isJsonArray()) {
            value.getAsJsonArray().forEach(v -> out.add(substitute(v.getAsString(), vars)));
        } else {
            out.add(substitute(value.getAsString(), vars));
        }
    }

    private Map<String, String> placeholders(GameInstallation installation, Account account,
                                             Instance instance, Path gameDir) {
        String classpath = installation.classpath().stream()
                .map(Path::toString)
                .collect(Collectors.joining(File.pathSeparator));

        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("auth_player_name", account.username());
        vars.put("version_name", installation.versionId());
        vars.put("game_directory", gameDir.toString());
        vars.put("assets_root", installation.assetsRoot().toString());
        vars.put("assets_index_name", installation.assetIndex());
        vars.put("auth_uuid", account.uuid().toString().replace("-", ""));
        vars.put("auth_access_token", account.accessToken());
        vars.put("clientid", "spaceline");
        vars.put("auth_xuid", "0");
        vars.put("user_type", account.isOnline() ? "msa" : "legacy");
        vars.put("version_type", "release");
        vars.put("natives_directory", installation.nativesDir().toString());
        vars.put("launcher_name", "spaceline");
        vars.put("launcher_version", Build.VERSION);
        vars.put("classpath", classpath);
        return vars;
    }

    /** Replaces all {@code ${var}} tokens in {@code template}. */
    public static String substitute(String template, Map<String, String> vars) {
        if (!template.contains("${")) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
