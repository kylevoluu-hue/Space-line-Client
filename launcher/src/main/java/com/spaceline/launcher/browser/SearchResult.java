package com.spaceline.launcher.browser;

/**
 * A provider-agnostic search hit, normalised from either Modrinth or CurseForge
 * so the UI and install flow don't care where a project came from.
 *
 * @param provider    the source ("modrinth" / "curseforge")
 * @param projectId   the provider's project id/slug used for follow-up calls
 * @param title       display name
 * @param author      author/owner display name
 * @param description short summary
 * @param downloads   total download count, for sorting/credibility
 * @param iconUrl     icon image url, may be empty
 * @param type        the normalised content type
 */
public record SearchResult(
        String provider,
        String projectId,
        String title,
        String author,
        String description,
        long downloads,
        String iconUrl,
        ProjectType type) {
}
