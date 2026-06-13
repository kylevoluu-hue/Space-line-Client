package com.spaceline.launcher.browser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.spaceline.common.net.Http;
import com.spaceline.common.util.Checksums;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The unified mod-browser facade over all configured {@link ContentProvider}s.
 *
 * <p>Aggregates search results across Modrinth and CurseForge, and implements
 * one-click install with transitive dependency resolution and SHA-1 integrity
 * verification. Version filtering is enforced end-to-end: only files compatible
 * with the instance's Minecraft version and loader are ever downloaded.
 */
public final class ModBrowser {

    private static final Logger LOG = LoggerFactory.getLogger(ModBrowser.class);

    private final List<ContentProvider> providers;

    public ModBrowser(List<ContentProvider> providers) {
        this.providers = providers;
    }

    public static ModBrowser withDefaults() {
        return new ModBrowser(List.of(new ModrinthClient(), new CurseForgeClient()));
    }

    /** Searches every provider and concatenates the results. */
    public List<SearchResult> search(String query, ProjectType type, String minecraftVersion,
                                     String loaderId, int limitPerProvider) {
        List<SearchResult> all = new ArrayList<>();
        for (ContentProvider provider : providers) {
            try {
                all.addAll(provider.search(query, type, minecraftVersion, loaderId, limitPerProvider));
            } catch (IOException e) {
                LOG.warn("Search via {} failed: {}", provider.id(), e.getMessage());
            }
        }
        all.sort((a, b) -> Long.compare(b.downloads(), a.downloads()));
        return all;
    }

    /**
     * One-click install: resolves the best compatible version of {@code result}
     * plus its required dependencies and downloads them all into {@code targetDir},
     * verifying each file's hash. Returns the installed file paths.
     */
    public List<Path> install(SearchResult result, String minecraftVersion, String loaderId,
                              Path targetDir) throws IOException {
        return installById(result.provider(), result.projectId(), minecraftVersion, loaderId, targetDir);
    }

    /**
     * One-click install by provider + project id/slug (e.g. {@code "modrinth"},
     * {@code "sodium"}), resolving dependencies and verifying hashes. Used by the
     * bundled-essentials installer and pre-built profiles.
     */
    public List<Path> installById(String providerId, String projectId, String minecraftVersion,
                                  String loaderId, Path targetDir) throws IOException {
        ContentProvider provider = providerFor(providerId);
        List<Path> installed = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(projectId);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (!visited.add(current)) {
                continue;
            }
            Optional<ProjectVersion> best = provider.bestVersion(current, minecraftVersion, loaderId);
            if (best.isEmpty()) {
                LOG.warn("No compatible version of {} for {} {}; skipping",
                        current, minecraftVersion, loaderId);
                continue;
            }
            ProjectVersion version = best.get();
            installed.add(downloadVerified(version, targetDir));
            queue.addAll(version.dependencyProjects());
        }
        return installed;
    }

    private Path downloadVerified(ProjectVersion version, Path targetDir) throws IOException {
        Files.createDirectories(targetDir);
        Path target = targetDir.resolve(version.fileName());
        LOG.info("Downloading {} -> {}", version.name(), target.getFileName());
        Http.download(version.downloadUrl(), target);
        if (!Checksums.matches(target, version.sha1(), "SHA-1")) {
            Files.deleteIfExists(target);
            throw new IOException("Checksum mismatch for " + version.fileName() + "; download rejected");
        }
        return target;
    }

    private ContentProvider providerFor(String id) {
        return providers.stream().filter(p -> p.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown provider: " + id));
    }
}
