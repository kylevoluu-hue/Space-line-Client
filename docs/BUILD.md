# Building Space~line Client — Step-by-step

This is the complete, beginner-friendly guide to building Space~line from source.
There are two buildable pieces:

1. **The launcher** (plus the shared code and the client engine) — builds
   anywhere with internet access to Maven Central.
2. **The Fabric client mod** — a separate build that additionally needs access to
   `maven.fabricmc.net`.

You do **not** need to install Gradle — a wrapper (`./gradlew`) is included.

---

## Step 1 — Install a JDK (Java 21–25)

Check whether you already have one:

```bash
java -version
```

You need a **64-bit JDK** (not just a JRE) with a major version between **21 and
25**. If you don't have one, install Temurin (Adoptium):

- **Windows**: download the JDK 21 `.msi` from <https://adoptium.net> and run it.
- **macOS**: `brew install --cask temurin@21`
- **Linux (Debian/Ubuntu)**: `sudo apt install openjdk-21-jdk`
- **Linux (Fedora)**: `sudo dnf install java-21-openjdk-devel`

Verify:

```bash
java -version    # should print 21.x, 22.x, ... up to 25.x
```

> If you have several JDKs installed, set `JAVA_HOME` to the 21–25 one, e.g.
> `export JAVA_HOME=/usr/lib/jvm/java-21-openjdk` (Linux/macOS).

---

## Step 2 — Get the source

```bash
git clone https://github.com/kylevoluu-hue/space-line-client.git
cd space-line-client
git checkout claude/spaceline-launcher-client-9k2717
```

(Once this branch is merged, you can skip the `checkout` and use `main`.)

---

## Step 3 — Build and test the launcher

From the repository root:

```bash
# Linux / macOS
./gradlew build

# Windows (PowerShell or cmd)
gradlew.bat build
```

The first run downloads Gradle and dependencies (a minute or two). A successful
build ends with:

```
BUILD SUCCESSFUL
```

This compiled and unit-tested three modules: `common` (shared code),
`launcher`, and the `client` engine.

> **Troubleshooting**
> - *"BUILD FAILED ... Could not resolve ..."* → no internet / blocked Maven
>   Central. Retry on a normal network.
> - *"invalid source release: 21"* → your JDK is older than 21. Re-check Step 1.

---

## Step 4 — Run the launcher

### Option A — run straight from Gradle (quickest)

```bash
./gradlew :launcher:run --args="status"
```

You should see a summary like:

```
Space~line Client
  Data directory : /home/you/.local/share/spaceline
  Accounts       : 0
  Instances      : 0
  Java runtimes  : 2 supported
  Version adapters: 2
```

Other commands:

```bash
./gradlew :launcher:run --args="java"               # list detected Java runtimes
./gradlew :launcher:run --args="versions"           # list installable stable releases (needs internet)
./gradlew :launcher:run --args="add-offline Steve"  # create an offline account
./gradlew :launcher:run --args="accounts"           # list accounts
./gradlew :launcher:run --args="help"               # all commands
```

> The current launcher is a **command-line tool**; the graphical front-end is the
> next milestone (see the README roadmap).

### Option B — build a standalone install (no Gradle needed afterwards)

```bash
./gradlew :launcher:installDist
```

This produces a self-contained app under `launcher/build/install/launcher/`.
Run it with the generated launch script:

```bash
# Linux / macOS
./launcher/build/install/launcher/bin/launcher status

# Windows
launcher\build\install\launcher\bin\launcher.bat status
```

To get a distributable archive instead:

```bash
./gradlew :launcher:distZip      # -> launcher/build/distributions/launcher-0.1.0.zip
```

Unzip it anywhere and run `bin/launcher` — only a JDK 21–25 is required on the
target machine.

---

## Step 5 — Where your data lives

The launcher stores accounts, instances, downloaded versions and caches under a
per-user directory:

| OS | Location |
|----|----------|
| Linux | `~/.local/share/spaceline` (or `$XDG_DATA_HOME/spaceline`) |
| macOS | `~/Library/Application Support/spaceline` |
| Windows | `%APPDATA%\.spaceline` |

Override it for testing:

```bash
./gradlew :launcher:run --args="status" -Dspaceline.dataDir=/tmp/spaceline-test
```

---

## Step 6 (optional) — Build the Fabric client mod

This is a **separate Gradle build** because it downloads Minecraft + Fabric from
`maven.fabricmc.net`.

```bash
cd client/fabric
./gradlew build
```

The mod jar appears at:

```
client/fabric/build/libs/spaceline-client-0.1.0.jar
```

Install it like any Fabric mod: put that jar **and** Fabric API into your
`.minecraft/mods/` folder on a **Fabric 1.21.4** profile.

To launch a development game instance directly (no manual install):

```bash
./gradlew runClient
```

> **Troubleshooting**
> - *"Could not resolve net.fabricmc..."* → your network blocks Fabric's maven.
>   This build only works where that host is reachable. The launcher build
>   (Steps 3–5) does not need it.

---

## Quick reference

```bash
# from repo root
./gradlew build                         # build + test launcher/common/client engine
./gradlew :launcher:run --args="status" # run the launcher CLI
./gradlew :launcher:installDist         # standalone launcher in launcher/build/install
./gradlew test                          # run all unit tests
./gradlew clean                         # remove build outputs

# from client/fabric
./gradlew build                         # build the Fabric mod jar
./gradlew runClient                     # launch a dev Minecraft client
```

That's it — you've built Space~line.
