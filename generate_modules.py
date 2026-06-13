#!/usr/bin/env python3
"""One-off generator for the Space~line client module catalogue.

It emits a well-formed Java class per module into the correct category package.
Hand-written flagship modules (Coordinates, FPS, CPS, Zoom, ...) are excluded
here. The output classes are pure Java (no Minecraft imports) so the whole
catalogue compiles and is unit-tested in the root build; the Fabric layer reads
each module's enabled state + settings to apply the real in-game effect.
"""
import os

BASE = "client/src/main/java/com/spaceline/client/module"
PKG = "com.spaceline.client.module"

# Already hand-written; do not regenerate.
SKIP = {"coordinates", "fps", "cps", "system_resources", "zoom", "fullbright",
        "fov_changer", "toggle_sprint"}

# (display, id, category, kind, hud_lines_java_or_None, settings)
# kind: "hud" -> extends HudModule ; "toggle" -> extends ToggleModule
# hud_lines_java: a Java expression returning List<String>, with `game` in scope.
# settings: list of (type, key, name, desc, args) generated in the constructor.
HUD = "HUD"
VIS = "VISUAL"
UTL = "UTILITY"
MOV = "MOVEMENT"
WRL = "WORLD"
SOC = "SOCIAL"
PRF = "PERFORMANCE"

M = [
    # --- HUD display modules (extend HudModule) ---
    ("Armor Bar", "armor_bar", HUD, "hud", None, []),
    ("Armor Status", "armor_status", HUD, "hud", None, []),
    ("Attack Indicator", "attack_indicator", HUD, "hud", None, []),
    ("Boss Bar", "boss_bar", HUD, "hud", None, []),
    ("Combo Display", "combo_display", HUD, "hud", None, []),
    ("Custom Crosshair", "custom_crosshair", HUD, "hud", None,
        [("number", "size", "Size", "Crosshair size", "4, 1, 16, 1")]),
    ("Damage Indicator", "damage_indicator", HUD, "hud", None, []),
    ("Death Info", "death_info", HUD, "hud", None, []),
    ("Direction", "direction", HUD, "real", 'List.of(game.facing())', []),
    ("Hearts", "hearts", HUD, "real", 'List.of("HP: " + (int) game.health() + " / " + (int) game.maxHealth())', []),
    ("Hit Indicator", "hit_indicator", HUD, "hud", None, []),
    ("Item Counter", "item_counter", HUD, "hud", None, []),
    ("Keystrokes", "keystrokes", HUD, "hud", None,
        [("bool", "mouse_buttons", "Mouse buttons", "Show LMB/RMB keys", "true")]),
    ("Mousestrokes", "mousestrokes", HUD, "hud", None, []),
    ("Pack Display", "pack_display", HUD, "hud", None, []),
    ("Ping", "ping", HUD, "real", 'game.pingMillis() < 0 ? List.of("Ping: --") : List.of("Ping: " + game.pingMillis() + "ms")', []),
    ("Playtime", "playtime", HUD, "real", 'List.of("Playtime: " + formatSession())', []),
    ("Potion Effects", "potion_effects", HUD, "real", 'game.potionEffects().isEmpty() ? List.of() : game.potionEffects()', []),
    ("Reach Display", "reach_display", HUD, "hud", None, []),
    ("Saturation", "saturation", HUD, "real", 'List.of(String.format("Saturation: %.1f", game.saturation()))', []),
    ("Scoreboard", "scoreboard", HUD, "hud", None, []),
    ("Server Address", "server_address", HUD, "real", 'List.of(game.serverAddress())', []),
    ("Speed Meter", "speed_meter", HUD, "hud", None, []),
    ("Stopwatch", "stopwatch", HUD, "hud", None, []),
    ("Tablist", "tablist", HUD, "hud", None, []),
    ("Tier Tagger", "tier_tagger", HUD, "hud", None, []),
    ("Time", "time", HUD, "real", 'List.of(new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()))', []),
    ("TNT Timer", "tnt_timer", HUD, "hud", None, []),
    ("Totem Counter", "totem_counter", HUD, "hud", None, []),
    ("TPS", "tps", HUD, "real", 'List.of(String.format("TPS: %.1f", game.tps()))', []),
    ("UHC Overlay", "uhc_overlay", HUD, "hud", None, []),

    # --- Visual modules ---
    ("Animations", "animations", VIS, "toggle", None, []),
    ("Block Overlay", "block_overlay", VIS, "toggle", None, []),
    ("Block Indicator", "block_indicator", VIS, "toggle", None, []),
    ("Color Saturation", "color_saturation", VIS, "toggle", None,
        [("number", "saturation", "Saturation", "World colour saturation", "1.0, 0.0, 3.0, 0.05")]),
    ("Custom Advancements", "custom_advancements", VIS, "toggle", None, []),
    ("Custom Fog", "custom_fog", VIS, "toggle", None,
        [("number", "density", "Density", "Fog density", "1.0, 0.0, 2.0, 0.05")]),
    ("Dark Mode", "dark_mode", VIS, "toggle", None, []),
    ("Item Info", "item_info", VIS, "toggle", None, []),
    ("Item Physics", "item_physics", VIS, "toggle", None, []),
    ("Light Level Overlay", "light_level_overlay", WRL, "toggle", None, []),
    ("Loot Beams", "loot_beams", VIS, "toggle", None, []),
    ("Mob Overlay", "mob_overlay", VIS, "toggle", None, []),
    ("Motion Blur", "motion_blur", VIS, "toggle", None,
        [("number", "strength", "Strength", "Blur amount", "0.5, 0.0, 1.0, 0.05")]),
    ("Nametags", "nametags", VIS, "toggle", None,
        [("number", "scale", "Scale", "Nametag scale", "1.0, 0.5, 3.0, 0.1")]),
    ("Player Model", "player_model", VIS, "toggle", None, []),
    ("Shulker Tooltips", "shulker_tooltips", VIS, "toggle", None, []),
    ("Subtitles", "subtitles", VIS, "toggle", None, []),
    ("Title Tweaker", "title_tweaker", VIS, "toggle", None, []),
    ("Toast Control", "toast_control", VIS, "toggle", None, []),
    ("Tooltips", "tooltips", VIS, "toggle", None, []),
    ("View Model", "view_model", VIS, "toggle", None, []),

    # --- Utility modules ---
    ("Auto Text", "auto_text", UTL, "toggle", None, []),
    ("Backups", "backups", UTL, "toggle", None,
        [("number", "interval", "Interval (min)", "Auto-backup interval", "10, 1, 120, 1")]),
    ("Camera", "camera", UTL, "toggle", None, []),
    ("Cull Logs", "cull_logs", PRF, "toggle", None, []),
    ("Drop Prevention", "drop_prevention", UTL, "toggle", None, []),
    ("Inventory", "inventory", UTL, "toggle", None, []),
    ("Item Despawn", "item_despawn", UTL, "toggle", None, []),
    ("Keybind Search", "keybind_search", UTL, "toggle", None, []),
    ("Perspective", "perspective", UTL, "toggle", None, []),
    ("Auto Perspective", "auto_perspective", UTL, "toggle", None, []),
    ("Reconnect", "reconnect", UTL, "toggle", None, []),
    ("Screenshot Tools", "screenshot_tools", UTL, "toggle", None,
        [("bool", "copy_clipboard", "Copy to clipboard", "Copy screenshots automatically", "true")]),
    ("Snaplook", "snaplook", UTL, "toggle", None, []),
    ("Sound Filters", "sound_filters", UTL, "toggle", None, []),
    ("UI Scaling", "ui_scaling", UTL, "toggle", None,
        [("number", "scale", "GUI scale", "Custom GUI scale", "1.0, 0.5, 4.0, 0.25")]),
    ("Autohide HUD", "autohide_hud", UTL, "toggle", None, []),

    # --- Movement modules ---
    ("Jump Reset", "jump_reset", MOV, "toggle", None, []),

    # --- World modules ---
    ("Time Changer", "time_changer", WRL, "toggle", None,
        [("number", "time", "Time", "Client-side world time", "6000, 0, 24000, 100")]),
    ("Weather Changer", "weather_changer", WRL, "toggle", None, []),
    ("Waypoints", "waypoints", WRL, "toggle", None, []),
    ("Horses", "horses", WRL, "toggle", None, []),

    # --- Social modules ---
    ("Hypixel Utilities", "hypixel_utilities", SOC, "toggle", None, []),
    ("Team Tracker", "team_tracker", SOC, "toggle", None, []),
    ("Nick Hider", "nick_hider", SOC, "toggle", None,
        [("string", "alias", "Alias", "Name to display instead", '"You"')]),
    ("Voice", "voice", SOC, "toggle", None, []),
]

HEADER = "// Generated by generate_modules.py — part of the Space~line client module catalogue.\n"

def class_name(display, mid):
    # Derive a CamelCase class name from the id.
    parts = mid.split("_")
    return "".join(p.capitalize() for p in parts) + "Module"

def pkg_dir(cat):
    return {
        HUD: "hud", VIS: "visual", UTL: "utility", MOV: "movement",
        WRL: "world", SOC: "social", PRF: "performance"
    }[cat]

def setting_field(stype, key, name, desc, args):
    jtype = {"bool": "BooleanSetting", "number": "NumberSetting",
             "string": "StringSetting", "color": "ColorSetting"}[stype]
    return jtype, key, name, desc, args

def render(display, mid, cat, kind, lines, settings):
    cn = class_name(display, mid)
    sub = pkg_dir(cat)
    pkg = f"{PKG}.{sub}"
    imports = []
    body_settings = []
    setting_imports = set()
    for (stype, key, name, desc, args) in settings:
        jtype, k, n, d, a = setting_field(stype, key, name, desc, args)
        setting_imports.add(jtype)
        field = key.replace("-", "_")
        body_settings.append(
            f'        register(new {jtype}("{k}", "{n}", "{d}"{("," + " " + a) if a else ""}));')

    si = "".join(f"import com.spaceline.common.module.setting.{t};\n" for t in sorted(setting_imports))

    if kind in ("hud", "real"):
        base = "HudModule"
        base_import = "import com.spaceline.client.hud.HudModule;\n"
        if kind == "real" and lines:
            lines_body = f"        return {lines};"
            extra = ""
            if "formatSession" in lines:
                extra = SESSION_HELPER
        else:
            # Placeholder line: the Fabric layer fills the live value in.
            lines_body = (f'        if (!game.inGame()) {{\n'
                          f'            return List.of();\n'
                          f'        }}\n'
                          f'        return List.of("{display}");')
            extra = ""
        ctor = "\n".join(body_settings)
        ctor_block = (f'\n{ctor}\n' if ctor else "")
        return f"""package {pkg};
{HEADER}
import java.util.List;

import com.spaceline.client.engine.GameState;
{base_import}{si}
/**
 * {display} HUD element. Live values are supplied by the Fabric layer through
 * {{@link GameState}}; this declaration owns the id, category and settings.
 */
public final class {cn} extends {base} {{

    public {cn}() {{
        super("{mid}", "{display}", "{display} overlay");{ctor_block}    }}

    @Override
    protected List<String> lines(GameState game) {{
{lines_body}
    }}{extra}
}}
"""
    else:
        base = "ToggleModule"
        ctor = "\n".join(body_settings)
        ctor_block = (f'\n{ctor}\n' if ctor else "")
        return f"""package {pkg};
{HEADER}
import com.spaceline.client.module.ToggleModule;
import com.spaceline.common.module.ModuleCategory;
{si}
/**
 * {display}. A toggleable {sub} feature; the Fabric layer applies the in-game
 * effect based on this module's enabled state and settings.
 */
public final class {cn} extends {base} {{

    public {cn}() {{
        super("{mid}", "{display}", ModuleCategory.{cat}, "{display}");{ctor_block}    }}
}}
"""

SESSION_HELPER = """

    private final long sessionStart = System.currentTimeMillis();

    private String formatSession() {
        long seconds = (System.currentTimeMillis() - sessionStart) / 1000;
        return String.format("%d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }"""

generated = []
for (display, mid, cat, kind, lines, settings) in M:
    if mid in SKIP:
        continue
    sub = pkg_dir(cat)
    d = os.path.join(BASE, sub)
    os.makedirs(d, exist_ok=True)
    cn = class_name(display, mid)
    with open(os.path.join(d, cn + ".java"), "w") as f:
        f.write(render(display, mid, cat, kind, lines, settings))
    generated.append((cn, sub, mid))

print(f"Generated {len(generated)} module classes")
for cn, sub, mid in generated:
    print(f"  {sub}/{cn} ({mid})")
