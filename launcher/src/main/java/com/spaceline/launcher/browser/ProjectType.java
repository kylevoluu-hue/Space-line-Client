package com.spaceline.launcher.browser;

/** The kinds of content the mod browsers can search for. */
public enum ProjectType {
    MOD("mod"),
    MODPACK("modpack"),
    RESOURCE_PACK("resourcepack"),
    SHADER("shader");

    private final String modrinthFacet;

    ProjectType(String modrinthFacet) {
        this.modrinthFacet = modrinthFacet;
    }

    /** The value Modrinth expects in its {@code project_type} facet. */
    public String modrinthFacet() {
        return modrinthFacet;
    }

    /** The CurseForge numeric class id for this content type. */
    public int curseForgeClassId() {
        return switch (this) {
            case MOD -> 6;
            case MODPACK -> 4471;
            case RESOURCE_PACK -> 12;
            case SHADER -> 6552;
        };
    }
}
