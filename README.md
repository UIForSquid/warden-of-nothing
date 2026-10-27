# Warden of Nothing

A NeoForge mod for Minecraft **1.21.1** that turns the Warden into a boss that belongs to its Ancient City.

- Every Ancient City generated with the mod has its own Warden (1500 health, harder hits, a boss bar).
  The city's shriekers summon *that* Warden. It burrows when left alone, remembers its health, and heals
  underground.
- A City Warden stays near home: it gives up targets that leave its range and walks back or burrows.
  Wardens can't use portals or ride anything.
- Kill a city's last Warden and the city goes quiet: its shriekers stop summoning, and it drops a
  **Heart of the Warden**. Feed the Heart sculk-catalyst pulses until it's Alive, place it on sculk in the
  defeated city, and after ten minutes the city's Warden climbs back out.

Everything is tunable in the server config.

## Install

**Requirements:** Minecraft 1.21.1 and NeoForge **21.1.264 or newer**. The mod adds blocks and items, so
it must be installed on **both the server and every client**.

1. Download `wardenofnothing-neoforge-1.21.1-<version>.jar` from the
   [latest release](https://github.com/UIForSquid/warden-of-nothing/releases/latest).
2. Put the jar in your `mods` folder:
   - **Vanilla launcher:** install NeoForge 21.1.x with the
     [NeoForge installer](https://neoforged.net/), then open `%appdata%\.minecraft\mods` (Windows) or
     `~/.minecraft/mods` (macOS/Linux) and drop the jar in.
   - **CurseForge / Prism / Modrinth App / ATLauncher:** create or open a 1.21.1 NeoForge instance, open its
     folder, and drop the jar into its `mods` folder.
   - **Dedicated server:** drop the jar into the server's `mods` folder.
3. Start the game. "Warden of Nothing" appears in the Mods list.

The city features only apply to Ancient Cities that generate **after** the mod is installed. Cities that
already exist in a world are left to vanilla. Explore new chunks to find tracked cities.

## Configure

The settings live in `wardenofnothing-server.toml` in the game's (or server's) `config` folder, created
on first launch. Each setting has a comment with its default, its range and when a
change takes effect. In multiplayer the server's copy applies to everyone.

Other datapacks or mods can add their own structures to the `wardenofnothing:boss_cities` structure tag to
make them count as cities.

## For modpack makers

- Server and client both need the jar. The mod has no other dependencies.
- The mod lifts vanilla's attribute ceilings for max health (1024), attack damage (2048) and attack
  knockback (5) up to the config's maximums, so the boss defaults aren't cut down. This only raises the
  limits. It changes no other mob's stats.
- Commands and selectors: ops can make any Warden a City Warden by giving it the
  `wardenofnothing.assigned` tag while it stands in a city. City Wardens carry the
  `wardenofnothing.city_warden` tag.

## Build from source

Needs JDK 21.

```bash
./gradlew build              # the jar ends up in build/libs/
./gradlew runGameTestServer  # run the feature GameTests
./gradlew runClient          # dev client
```

Every push builds the jar on GitHub Actions. Open a run under **Actions** and download it from
**Artifacts**. Pushing a tag that matches `mod_version` in `gradle.properties` (for example `v0.1.0`)
publishes a GitHub Release with the jar attached.

## License

MIT, see [LICENSE](LICENSE). Built from the NeoForge MDK, whose template files are covered by
[TEMPLATE_LICENSE.txt](TEMPLATE_LICENSE.txt).
