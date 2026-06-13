# Space~line Fabric client

This is the Fabric mod that binds the platform-agnostic Space~line **client
engine** (`:client` in the root build) to Minecraft. It is a **separate Gradle
build** from the root project.

## Why it's separate

The mod applies the [Fabric Loom](https://github.com/FabricMC/fabric-loom)
plugin and downloads Minecraft + Fabric from `https://maven.fabricmc.net`. That
host is not reachable from every CI sandbox, so wiring it into the root build
would break `./gradlew build` in restricted environments. Keeping it standalone
means the rest of the project (launcher + common + client engine) always builds,
and this module builds wherever the Fabric maven is reachable.

It consumes `:common` and `:client` from the root build through a Gradle
**composite build** (`includeBuild('../..')` in `settings.gradle`), so there is
no code duplication — the same engine that is unit-tested in the root build is
shaded into the mod jar here.

## Building

```bash
cd client/fabric
./gradlew build       # produces build/libs/spaceline-client-<version>.jar
```

Run the client in a dev environment with:

```bash
./gradlew runClient
```

## What lives here

| File | Responsibility |
|------|----------------|
| `SpaceLineFabricClient` | Mod entrypoint; constructs the engine and bridges Fabric callbacks to its event bus |
| `FabricRenderContext`   | Implements the engine's `RenderContext` on Minecraft's `DrawContext` |
| `FabricGameState`       | Implements the engine's `GameState` from `MinecraftClient` |
| `KeyBindings`           | Registers the menu key and dispatches per-module toggle keys |
| `ModuleEffects`         | Applies option-driven effects (e.g. Fullbright gamma) |
| `mixin/GameRendererMixin` | Zoom + FOV Changer (adjusts computed FOV) |
| `mixin/MouseMixin`      | Feeds clicks to CPS / combo modules |
| `mixin/InGameHudMixin`  | Autohide HUD |

All game-agnostic logic — the modules, HUD layout, event system, config — lives
in `:client` and `:common`, not here.

## Minecraft version

Pinned in `gradle.properties` to the **1.21.x** stable line (Java 21). Update
`minecraft_version`, `yarn_mappings`, `loader_version` and `fabric_version`
together when moving to a newer stable release. Snapshots are not supported.
