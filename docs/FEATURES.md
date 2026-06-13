# Spaceline Client — Features & Tutorials

A full description of each feature and a step-by-step guide to using it. Status
is marked honestly:

- ✅ **Done** — built, working, usable now.
- 🟡 **Backend ready** — the engine/logic works; the in-launcher UI page is still
  being built (usable from code / partially wired).
- 🚧 **Planned** — designed and on the roadmap, not yet implemented.

> Rebuild after pulling: `git pull` → `gradlew.bat build`. Open the launcher with
> `gradlew.bat :launcher:run`.

---

## 1. Opening animation ✅

**What it is.** When you launch the app, an animated splash appears over an
interactive particle field: the black-hole logo fades and zooms in, the title
**Spaceline** appears, and the subheading *"Thank you for using Spaceline
client. - kyluua"* fades in beneath it. After a short hold it fades out and the
main window opens.

**How to use it.** Just start the launcher (`gradlew.bat :launcher:run`, or the
installed `Spaceline.exe`). It plays automatically. Move your mouse over it — the
background particles react.

---

## 2. App icon with "Spaceline" wordmark ✅

**What it is.** A custom black-hole icon (glowing cyan/blue/magenta accretion
ring on a dark rounded square) with the **Spaceline** wordmark. It's used for the
`.exe`, the installer, the taskbar/window, and the Fabric mod.

**How to use it.** It's automatic — build the app/installer and the icon is
embedded. To regenerate or tweak it: edit `scripts/IconGenerator.java`, then
`javac -d out scripts/IconGenerator.java && java -cp out IconGenerator`.

---

## 3. Credits page ✅

**What it is.** A Credits window crediting **kyluua (@kyluua)** as the creator,
listing what the client is built with, the MIT license, and **per-OS install
instructions** (Windows / macOS / Linux) plus a GitHub link.

**How to use it.** In the launcher header, click **Credits**.

---

## 4. Mod Profiles (save / apply / export / import) ✅

**What it is.** A profile is a saved bundle of an instance's **mods, resource
packs and shaders**. You can snapshot the current instance, apply a profile to
any instance, and **export a profile to a portable `.zip`** that re-imports here
(or drops its `mods/ resourcepacks/ shaderpacks/` folders into other launchers).
Profiles are **unlimited** (only your disk space limits them).

**How to use it.**
1. Launcher header → **Profiles**.
2. **Save current instance as profile…** — select an instance first, then name it;
   its current mods/packs/shaders are captured.
3. **Apply to selected instance** — copies a profile's files onto the selected
   instance.
4. **Export profile (.zip)…** — pick a save location; share that zip.
5. **Import profile (.zip)…** — pick a profile zip to add it.

---

## 5. Pre-built profiles ✅

**What it is.** Starter profiles included out of the box:
- **Pure Vanilla** — no mods.
- **FPS Booster** — Fabric API + Sodium + Lithium + FerriteCore.
- **PvP** — performance + Iris for competitive play.
- **Legit Performance** — server-safe performance mods only.

Pre-built profiles install the **newest compatible builds** from Modrinth for
your instance's exact Minecraft version (never stale jars).

**How to use it.** **Profiles** → pick a built-in → **Apply to selected
instance**. It downloads the latest matching mods into that instance.

---

## 6. Bundled essentials: Sodium, Fabric API, FerriteCore ✅

**What it is.** When you create a **Fabric** instance, the launcher automatically
installs the newest compatible **Fabric API, Sodium and FerriteCore** for that
version. (Vanilla instances get nothing — your choice of loader is respected.)

**How to use it.** Just create a Fabric instance (**New** → choose Fabric). The
essentials are pulled in during creation. They always track the latest compatible
release.

---

## 7. Interactive particles ✅ (splash) / 🟡 (main menu)

**What it is.** An animated "constellation" particle field — drifting dots linked
by faint lines that **react to your mouse** (gentle repulsion) and never block
clicks. Currently shown on the **opening animation**; making it the full
main-menu background with a toggle is wired in code and being finished.

**How to use it.** It plays on the splash automatically. The `ParticlePanel`
component is reusable and density/colour-configurable.

---

## 8. Themes (built-in + import) ✅ / customization 🟡

**What it is.** Five built-in themes — **Dark, Light, Glass, Neon, Minimal** —
plus support for **user-uploaded JSON themes** with colour and background (solid /
image / animated / video) fields. A **Light/Dark toggle** is in the header now.

**How to use it.**
- Toggle **Light/Dark** in the header.
- Add a custom theme: drop a `theme.json` into the `themes` folder under your data
  directory (`%APPDATA%\.spaceline\themes` on Windows), or it's loaded on startup.

🚧 A full in-launcher theme editor (colour pickers, background uploader) is on the
roadmap.

---

## 9. Mod / resource-pack / shader browser (Modrinth + CurseForge) 🟡

**What it is.** Clients for **Modrinth** and **CurseForge** with search, version
filtering (matches your Minecraft version + loader), one-click install and
**transitive dependency resolution** with hash verification. CurseForge uses an
API key when you provide one (`CURSEFORGE_API_KEY`); Modrinth needs none.

**Status.** The backend (`ModBrowser`, `ModrinthClient`, `CurseForgeClient`) is
done and used by profiles/essentials. The dedicated **in-launcher search page**
(browse + click to install into an instance) is the next UI piece.

**How to use it (today).** It powers pre-built profiles and bundled essentials. To
enable CurseForge results, set the `CURSEFORGE_API_KEY` environment variable.

---

## 10. Skin loader / stealer 🟡

**What it is.** Type a player's name to **fetch and equip their public skin**,
with import/export and saved/favourite/namable skins, and a 360° 3D model preview.

**Status.** The lookup backend is done — `SkinManager.publicSkinUrl(username)`
resolves any player's public skin via Mojang, and custom skin/cape import works.
The **skins page** (name box, saved/favourites, **3D 360° viewer**) is the next UI
piece.

---

## 11. Custom fonts & font colour presets 🚧

**What it is.** Choose UI/HUD fonts (Georgia, Arial, …) and a **text colour
style**: solid presets (red, yellow, …), **gradients**, a **chrome/RGB** cycle, or
a custom palette. Importable custom fonts.

**Status.** Planned. The data directory already reserves a `fonts/` folder for
imports; the style model + pickers are next.

---

## 12. Offline play behaviour ✅

**What it is.** Offline accounts get a local, name-derived UUID and can only join
**cracked / offline-mode (LAN) servers** — they can't authenticate to online
servers. Use a **Microsoft** account for online play.

**How to use it.** Header → **Add account → Offline** (name only) vs.
**Microsoft** (real sign-in).

---

## 13. The toggleable client modules ✅

All requested modules are registered, **toggleable, keybind-able, persistent**,
and the HUD ones render in-game: *Animations, Armor Bar, Armor Status, Attack
Indicator, Autohide HUD, Auto Perspective, Auto Text, Backups, Block Indicator,
Block Overlay, Boss Bar, Brightness/Fullbright, Camera, Color Saturation, Combo
Display, Coordinates, CPS, Custom Crosshair, Cull Logs, Custom Advancements,
Custom Fog, Damage Indicator, Dark Mode, Death Info, Direction, Drop Prevention,
FOV Changer, FPS, Hearts, Hit Indicator, Horses, Hypixel Utilities, Inventory,
Item Counter, Item Despawn, Item Info, Item Physics, Jump Reset, Keystrokes, Light
Level Overlay, Loot Beams, Mob Overlay, Motion Blur, Mousestrokes, Nametags, Nick
Hider, Pack Display, Perspective, Ping, Player Model, Playtime, Potion Effects,
Reach Display, Reconnect, Saturation, Scoreboard, Screenshot Tools, Keybind
Search, Server Address, Shulker Tooltips, Snaplook, Sound Filters, Speed Meter,
Stopwatch, Subtitles, System Resources, Tablist, Team Tracker, Tier Tagger, Time,
Time Changer, Title Tweaker, TNT Timer, Toast Control, Toggle Sprint, Tooltips,
Totem Counter, TPS, UHC Overlay, UI Scaling, View Model, Voice, Waypoints, Weather
Changer, Zoom.*

In-game effects wired so far: Zoom, FOV Changer, Fullbright, CPS tracking, Autohide
HUD, Toggle Sprint, and all HUD overlays. The rest are registered and their
settings persist; their game effects are being filled in module by module.

**How to use it.** Build the Fabric mod (`cd client/fabric && gradlew.bat build`),
put the jar + Fabric API in a Fabric instance's `mods` folder, and in-game press
**Right Shift** to open the menu / HUD editor (default).

---

## 14. Sharable installer (all platforms) ✅ (Windows) / 🟡 (mac/Linux)

**Windows:** `gradlew.bat :launcher:packageInstaller` → a single
`Spaceline-1.0.0.exe` (needs the free WiX Toolset). See `docs/BUILD.md`.

**macOS / Linux:** `./gradlew :launcher:installDist` produces a runnable bundle;
`jpackage` can build a native `.dmg`/`.deb` on those platforms (same pattern as
the Windows task).

---

## Roadmap summary

| Feature | Status |
|---|---|
| Opening animation, icon, credits | ✅ |
| Mod profiles + pre-built + export/import | ✅ |
| Bundled essentials (Sodium/Fabric API/FerriteCore) | ✅ |
| Interactive particles | ✅ splash · 🟡 main menu |
| Themes built-in + JSON import | ✅ · editor 🚧 |
| Modrinth/CurseForge browser | 🟡 backend done, page next |
| Skin loader + 3D viewer | 🟡 lookup done, page next |
| Custom fonts + colour presets | 🚧 |
| All 85 modules toggleable | ✅ · effects filling in |
| Windows installer | ✅ · mac/Linux native 🟡 |
