# Spaceline Client — Features & Full Tutorial

A description of **every** feature and a clear, step-by-step tutorial for using
it. Status is marked honestly:

- ✅ **Done** — built and usable now.
- 🟡 **Mostly done** — works, with a noted limitation.
- 🚧 **Planned** — designed, not yet built.

> **Update & launch:** `git pull` → `gradlew.bat build` → `gradlew.bat :launcher:run`.
> The launcher opens with a left **navigation rail**: *Play, Browse, Content,
> Skins, Appearance*, plus *Profiles* and *Credits* in the top-right.

---

## 1. Opening animation ✅
**Description.** On launch, an animated splash plays over an interactive particle
field: the black-hole logo fades/zooms in, the title **Spaceline** appears, and
the subheading *"Thank you for using Spaceline client. - kyluua"* fades in. Then
it fades out and the launcher opens.

**Tutorial.**
1. Start the launcher (`gradlew.bat :launcher:run` or the installed `Spaceline.exe`).
2. Watch the splash; move your mouse over it — the particles react.
3. It closes itself and the main window opens.

---

## 2. App icon with "Spaceline" wordmark ✅
**Description.** A custom black-hole icon with the **Spaceline** wordmark, used on
the `.exe`, installer, window/taskbar and the Fabric mod.

**Tutorial.** Automatic. To regenerate: `javac -d out scripts/IconGenerator.java && java -cp out IconGenerator`.

---

## 3. Multiple pages ✅
**Description.** The launcher is organized into pages via the left nav rail.

**Tutorial.** Click any rail button — **Play**, **Browse**, **Content**,
**Skins**, **Appearance** — to switch pages. **Profiles** and **Credits** open
from the top-right.

---

## 4. Mod / resource-pack / shader browser (Modrinth + CurseForge) ✅
**Description.** Search both Modrinth and CurseForge for **mods, shaders, resource
packs and modpacks**, filtered to your instance's Minecraft version + loader, and
install with one click (dependencies resolved, hashes verified). CurseForge
results turn on when an API key is set.

**Tutorial.**
1. On **Play**, select (or create) the instance you want to add content to.
2. Go to **Browse**.
3. Choose a type (Mod / Modpack / Resource Pack / Shader), type a search, press
   **Search**.
4. Select a result → **Install to selected instance**. It downloads into the
   correct folder (mods/resourcepacks/shaderpacks) with dependencies.
5. *(Optional, for CurseForge results):* set the `CURSEFORGE_API_KEY` environment
   variable before launching. Modrinth needs no key.

---

## 5. Mod Profiles (save / apply / export / import) ✅
**Description.** A profile bundles an instance's **mods + resource packs +
shaders**. Save the current instance as a profile, apply a profile to any
instance, **export to a `.zip`** (re-importable here or into other launchers via
its `mods/ resourcepacks/ shaderpacks/` folders), and import zips. Unlimited
profiles — only your disk limits them; nothing is force-installed.

**Tutorial.**
1. Top-right → **Profiles**.
2. **Save current instance as profile…** (select an instance first; name it).
3. **Apply to selected instance** to copy a profile onto an instance.
4. **Export profile (.zip)…** to get a shareable file; **Import profile (.zip)…**
   to add one.

---

## 6. Pre-built profiles ✅
**Description.** Built-in starters: **Pure Vanilla, FPS Booster, PvP, Legit
Performance**. They install the **newest compatible** mods from Modrinth for your
instance's version.

**Tutorial.** **Profiles** → pick a built-in (marked *built-in*) → **Apply to
selected instance**.

---

## 7. Bundled essentials: Sodium, Fabric API, FerriteCore ✅
**Description.** Creating a **Fabric** instance auto-installs the newest
compatible **Fabric API + Sodium + FerriteCore**. Vanilla instances get nothing
(your loader choice is respected).

**Tutorial.** **Play** → **New** → choose **Fabric** → create. The essentials are
pulled in automatically and stay up to date.

---

## 8. Toggleable mods, resource packs & shaders ✅
**Description.** Enable/disable and remove an instance's content. Mods toggle via
the `.disabled` convention, resource packs keep a load order, shaders track the
active pack.

**Tutorial.**
1. Select an instance on **Play**.
2. Go to **Content**. Use the **Mods / Resource Packs / Shaders** tabs.
3. Select an item → **Enable / Disable / Remove** (mods), **Enable / Disable /
   Move up / Move down** (packs), **Set active / Disable** (shaders).
4. **Import…** adds a file directly.

---

## 9. Skin loader / "stealer" with 3D 360° viewer ✅ (equip-on-account 🟡)
**Description.** Type any player's name to fetch their public skin, preview it on
a **rotatable 3D model** (drag to spin a full 360°, or auto-rotate), and save it
to a **named, favouritable library** with import/export.

**Tutorial.**
1. Go to **Skins**.
2. Type a player name → **Fetch skin**. The 3D model loads and the skin is saved
   to your library.
3. **Drag** the model to rotate it 360°, or tick **Auto-rotate**.
4. Library actions: **Favourite**, **Rename**, **Import** (your own PNG),
   **Export** (save a PNG), **Delete**. Click a saved skin to preview it.

> 🟡 Previewing, saving, importing and exporting work fully. *Applying* a skin to
> your actual Microsoft account (uploading it to Mojang) needs the authenticated
> profile API and is the remaining step.

---

## 10. Custom fonts + colour presets (solid / gradient / chrome-RGB) ✅
**Description.** Pick the UI font (Georgia, Arial, system fonts, or imported
ones) and size, and build an accent **text-colour style**: a **solid** colour,
a **gradient** between two colours, or an animated **chrome/RGB** cycle — with a
live preview.

**Tutorial.**
1. Go to **Appearance**.
2. **UI font** + **Font size** — applied to the whole launcher instantly.
3. **Title colour** → choose Solid / Gradient / Chrome, pick **Colour 1**/**Colour
   2**, and watch the **Preview** ("Spaceline") update live.

---

## 11. Customizable themes & backgrounds + imports ✅
**Description.** Five built-in themes (**Dark, Light, Glass, Neon, Minimal**),
selectable, plus **import your own** themes (`.json`), **fonts** (`.ttf`/`.otf`)
and **background images**.

**Tutorial.**
1. **Appearance** → **Theme** dropdown to switch theme.
2. **Import** row → **Import theme (.json)** / **Import font** / **Import
   background**. Imported fonts appear in the UI-font list; the theme applies
   immediately.

---

## 12. Toggleable interactive particles ✅
**Description.** An animated "constellation" particle field that reacts to your
mouse and never blocks buttons. Shown on the opening animation; toggle in
Appearance.

**Tutorial.** **Appearance** → tick/untick **Interactive particle background**.
(Always shown on the splash.)

---

## 13. Credits page ✅
**Description.** Credits **kyluua (@kyluua)** as creator, lists what the client is
built with, the MIT license, and **per-OS install instructions** (Windows /
macOS / Linux) with a GitHub link.

**Tutorial.** Top-right → **Credits**. Use **Open GitHub** or the in-page links.

---

## 14. Microsoft & offline accounts ✅
**Description.** Sign in with **Microsoft** for online play, or create an
**offline** account. Offline accounts use a local UUID and can only join
**cracked / offline-mode (LAN)** servers — they can't authenticate to online
servers.

**Tutorial.** Header → **Add account** → **Microsoft** (real sign-in via a code)
or **Offline** (name only). Switch the active account in the dropdown.

---

## 15. The toggleable client modules ✅
**Description.** All requested modules — *Animations, Armor Bar, Armor Status,
Attack Indicator, Autohide HUD, Auto Perspective, Auto Text, Backups, Block
Indicator, Block Overlay, Boss Bar, Brightness/Fullbright, Camera, Color
Saturation, Combo Display, Coordinates, CPS, Custom Crosshair, Cull Logs, Custom
Advancements, Custom Fog, Damage Indicator, Dark Mode, Death Info, Direction,
Drop Prevention, FOV Changer, FPS, Hearts, Hit Indicator, Horses, Hypixel
Utilities, Inventory, Item Counter, Item Despawn, Item Info, Item Physics, Jump
Reset, Keystrokes, Light Level Overlay, Loot Beams, Mob Overlay, Motion Blur,
Mousestrokes, Nametags, Nick Hider, Pack Display, Perspective, Ping, Player Model,
Playtime, Potion Effects, Reach Display, Reconnect, Saturation, Scoreboard,
Screenshot Tools, Keybind Search, Server Address, Shulker Tooltips, Snaplook,
Sound Filters, Speed Meter, Stopwatch, Subtitles, System Resources, Tablist, Team
Tracker, Tier Tagger, Time, Time Changer, Title Tweaker, TNT Timer, Toast Control,
Toggle Sprint, Tooltips, Totem Counter, TPS, UHC Overlay, UI Scaling, View Model,
Voice, Waypoints, Weather Changer, Zoom* — are registered, **toggleable,
keybind-able and persistent**, with HUD overlays that render in-game.

**Tutorial.**
1. Build the Fabric mod: `cd client/fabric && gradlew.bat build`.
2. Put `build/libs/spaceline-client-*.jar` + Fabric API into a Fabric instance's
   `mods` folder (or use **Browse** to install Fabric API).
3. In-game, press **Right Shift** to open the menu / HUD editor; toggle modules,
   set keybinds and settings.

> In-game *effects* wired so far: Zoom, FOV Changer, Fullbright, CPS, Autohide
> HUD, Toggle Sprint, and all HUD overlays. The rest are registered with
> persistent settings; their game effects are being added module by module.

---

## 16. Shareable installer (all platforms) ✅ Windows · 🟡 mac/Linux
**Description.** A native installer that bundles a Java runtime so recipients need
nothing installed.

**Tutorial.**
- **Windows:** `gradlew.bat :launcher:packageInstaller` → `Spaceline-1.0.0.exe`
  (needs free WiX Toolset). Or `:launcher:packageApp` for a folder app.
- **macOS / Linux:** `./gradlew :launcher:installDist` for a runnable bundle;
  `jpackage` builds a native `.dmg`/`.deb` on those platforms.

See `docs/BUILD.md` for the full build walkthrough.

---

## Status summary

| Feature | Status |
|---|---|
| Opening animation, icon, multi-page nav | ✅ |
| Modrinth/CurseForge browser (search + install) | ✅ |
| Mod profiles + pre-built + export/import zip | ✅ |
| Bundled Sodium / Fabric API / FerriteCore | ✅ |
| Toggle mods / resource packs / shaders | ✅ |
| Skin loader + 3D 360° viewer + library | ✅ (account-equip 🟡) |
| Custom fonts + colour presets (solid/gradient/chrome) | ✅ |
| Themes + background + font imports | ✅ |
| Toggleable interactive particles | ✅ |
| Credits page (per-OS install) | ✅ |
| Microsoft + offline accounts | ✅ |
| All 85 modules toggleable | ✅ (effects filling in) |
| Windows installer | ✅ (mac/Linux native 🟡) |
