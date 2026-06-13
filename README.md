# Space~line Client

**Space~line** is an open-source, cross-platform **Minecraft launcher + Fabric
client** in the spirit of clients like Lunar and Feather. It pairs a desktop
launcher (accounts, version & Java management, mods, a Modrinth/CurseForge
browser, process control) with a modular Fabric client (90-ish toggleable
modules and a fully customizable HUD).

- 🟢 **Stable Minecraft only** — every officially released version from **1.21.x**
  onward. No snapshots, pre-releases or experimental builds, ever.
- 🧩 **Fabric** mod loader **and** plain vanilla launching.
- ☕ **Java 21–25** compatible.
- 🪟🍎🐧 **Windows, macOS, Linux**.
- 🧱 **Gradle multi-module**, event-driven, JSON-configured, MIT-licensed.

> **Status:** early foundation (`0.1.0`). The architecture, core systems and the
> full module catalogue are in place and tested; some game-side effects and the
> graphical launcher front-end are scaffolded for contribution. See
> [Roadmap](#roadmap).

---

## Repository layout

This is a Gradle multi-module project with strict launcher / client / common
separation:

```
space-line/
├── common/            Shared, Minecraft-agnostic logic (builds anywhere)
│   ├── event/           Annotation-driven EventBus
│   ├── config/          Versioned JSON config + migrations
│   ├── registry/        Generic id-keyed registry (plugin backbone)
│   ├── module/          Module + typed Setting framework, keybinds
│   ├── hud/             HUD layout model (anchors, profiles)
│   ├── ui/              Theme model + 5 built-in themes
│   ├── net/             Shared HTTP client with retries
│   └── util/            Platform detection, data paths, checksums
│
├── launcher/          Desktop launcher application (pure JVM)
│   ├── account/         Microsoft (device-code) + offline, multi-account
│   ├── version/         Manifest fetch, filtering, version ADAPTER system
│   ├── java/            Java 21–25 detection & selection
│   ├── instance/        Per-instance config & isolated game dirs
│   ├── mods/            Import, enable/disable, dependency & conflict checks
│   ├── content/         Resource packs & shaders (order, presets)
│   ├── browser/         Modrinth + CurseForge clients, one-click install
│   ├── launch/          Version install + Fabric merge + command builder
│   ├── process/         Play/Stop/Force-kill/Restart, crash detection, logs
│   └── skin/            Custom skins/capes + username skin loader
│
└── client/            The Fabric client
    ├── (engine)         Platform-agnostic engine: events, 85 modules, HUD
    │                    render — depends only on :common, fully unit-tested
    └── fabric/          Separate Loom build: binds the engine to Minecraft
                         (entrypoint, RenderContext/GameState, mixins)
```

### Why `client/fabric` is a separate build

The pure-JVM modules (`common`, `launcher` and the `client` **engine**) build and
test anywhere with Maven Central access. The Fabric mod needs
`maven.fabricmc.net` and the Minecraft libraries, which aren't reachable from
every CI sandbox — so it's a standalone
[composite build](client/fabric/README.md) that reuses the same `:common` and
`:client` code without duplication. The root `./gradlew build` always works; the
mod is built where the Fabric maven is available.

---

## Building & running

Requires a JDK in the **21–25** range. The Gradle wrapper is included.

```bash
# Build and test the launcher + client engine + shared code
./gradlew build

# Run the launcher's diagnostic CLI
./gradlew :launcher:run --args="status"
./gradlew :launcher:run --args="versions"   # lists installable stable releases
./gradlew :launcher:run --args="java"       # lists detected Java runtimes

# Build the Fabric client mod (needs maven.fabricmc.net)
cd client/fabric && ./gradlew build         # -> build/libs/spaceline-client-*.jar
```

---

## Key systems

### Version abstraction / adapter layer

Every behavioural difference between Minecraft version families lives behind a
single `VersionAdapter` (required Java major, argument format, JVM tuning,
supported loaders). The launch pipeline, Java selection and mod validation talk
to the adapter, never to hard-coded version checks. New families are added by
registering one adapter — see
[`version/adapter`](launcher/src/main/java/com/spaceline/launcher/version/adapter).
Only stable releases ≥ 1.21 are ever surfaced; the filter lives in one place
(`VersionRepository`).

### Module framework

Every client feature is a `Module` with: a stable id, a category, a keybind,
typed `Setting`s (boolean / number / enum / colour / string) that auto-serialize
to JSON, and `onEnable`/`onDisable` lifecycle hooks. Enabled modules are
auto-subscribed to the event bus; disabled ones cost nothing. The `ModuleManager`
groups them by category and persists everything as one versioned document.

### HUD system

Each HUD element stores an **anchor + offset** (resolution-independent), plus
scale, colour, opacity, layer order, visibility, font, shadow and background.
Layouts live in switchable **profiles** with snap-to-grid. The engine renders
through a `RenderContext` abstraction, so HUD modules are drawn — and unit-tested
— with no Minecraft on the classpath.

### Config system

Typed, versioned JSON with **migrations**: a `ConfigManager` stamps a schema
version, backs up the old file and runs ordered `ConfigMigration`s to upgrade it
before deserialization. Saves are atomic.

---

## Module catalogue

**85 modules** across the seven categories (HUD, Visual, Utility, Movement,
World, Social, Performance), including: Coordinates, FPS, CPS, Keystrokes, Ping,
Potion Effects, Armor Status, Scoreboard, Tablist, TPS, System Resources, Zoom,
FOV Changer, Brightness/Fullbright, Toggle Sprint, Nametags, Motion Blur,
Waypoints, Time/Weather Changer, Nick Hider, Hypixel Utilities, and many more.
The full list and wiring is generated into
[`ClientModules`](client/src/main/java/com/spaceline/client/engine/ClientModules.java).

## UI themes

Five built-in themes — **Dark, Light, Glass, Neon, Minimal** — plus user-uploaded
JSON themes, with colour / background (solid, image, animated, video) support.
See [`ui/ThemeManager`](common/src/main/java/com/spaceline/common/ui/ThemeManager.java).

---

## Roadmap

| Phase | Area | State |
|------:|------|-------|
| 1 | Core launcher skeleton | ✅ |
| 2 | Version + Java management | ✅ |
| 3 | Fabric integration (install + merge + launch) | ✅ |
| 4 | Mod system (install/enable/disable, validation) | ✅ |
| 5 | Client module framework + 85 modules | ✅ |
| 6 | HUD system (layout, profiles, render abstraction) | ✅ |
| 7 | UI themes + customization | ✅ (model) |
| 8 | Modrinth / CurseForge integration | ✅ |
| 9 | Skin + cape system | ✅ |
| 10 | Graphical launcher (Swing/FlatLaf) + MS login + `.exe` packaging | ✅ |
| 11 | Per-module in-game effects & launcher polish | 🚧 |

---

## Contributing

Contributions are welcome — this is meant to be forked and built on. See
[CONTRIBUTING.md](CONTRIBUTING.md). Adding a module is usually a single class;
adding support for a new Minecraft family is usually a single adapter.

## License

[MIT](LICENSE). Open source, fork-able, public.

> Not affiliated with Mojang, Microsoft, Modrinth, CurseForge or any existing
> client. "Minecraft" is a trademark of Mojang Synergies AB.
