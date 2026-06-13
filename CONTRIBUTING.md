# Contributing to Space~line Client

Thanks for your interest! Space~line is MIT-licensed and built to be forked and
extended. This guide covers the conventions that keep the codebase coherent.

## Ground rules

- **Stable Minecraft only.** Never add support for snapshots, pre-releases or
  experimental builds. The supported floor is 1.21.
- **Strict module separation.** `common` must not depend on `launcher` or
  `client`. The `client` engine must not import Minecraft — only the
  `client/fabric` layer may. Keep launcher core logic headless (UI behind
  interfaces).
- **No version hard-coding.** Anything that differs between Minecraft families
  goes behind a `VersionAdapter`, not an `if (version.equals(...))`.
- **Handle errors.** No empty catch blocks that hide failures; log with context
  and degrade gracefully.

## Building

```bash
./gradlew build            # common + launcher + client engine (always works)
cd client/fabric && ./gradlew build   # the Fabric mod (needs maven.fabricmc.net)
```

Target Java 21 (the toolchain is pinned); the result must run on Java 21–25.

## Adding a client module

1. Create a class under `client/.../module/<category>/` extending `HudModule`
   (text overlay) or `ToggleModule` (feature toggle).
2. Give it a stable, lowercase `id`, a category, a description and any typed
   settings via `register(...)`.
3. Register it in
   `client/src/main/java/com/spaceline/client/engine/ClientModules.java`.
4. If it needs a game-side effect, wire it in `client/fabric` (a mixin or a tick
   handler that reads the module's state).
5. Add a unit test if it has non-trivial logic.

## Adding a Minecraft version family

Add a `VersionAdapter` (extend `AbstractVersionAdapter`) declaring the version
range, required Java major and supported loaders, then register it in
`VersionAdapterRegistry.withDefaults()` before the broader fallback.

## Style

- Match the surrounding code: clear names, Javadoc on public types explaining
  *why*, not just *what*.
- Prefer the standard library; new dependencies need a good reason.
- Keep methods small and the hot paths (tick/render) allocation-light.

## Tests

Unit tests live in each module's `src/test/java`. Run `./gradlew test`. New
behaviour-bearing code should come with tests; bug fixes should come with a
regression test.
