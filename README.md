# CookieCamera

A smooth in-game cinematic cutscene plugin for **Paper, Purpur, Leaf, and Folia (Minecraft 1.21 – 1.26.3)** powered by PacketEvents.

> **Original Code Credit:**
> Updated fork/build based on **AnkiCamera** by AnkiTeam:
> https://modrinth.com/plugin/ankicamera

## Features

- **Minecraft 1.21 – 1.26.3** compatible (single Java 21 release JAR tested against 1.21.4 and 26.3 APIs).
- **Paper, Purpur, Leaf, and Folia** support (`folia-supported: true`).
- **Smooth Camera Paths**: `CATMULL_ROM`, `BSPLINE`, `HERMITE`, and `LINEAR` interpolation with configurable easing, tension, and smoothing.
- **In-Game Hotbar Editor**: Build and preview scenes directly in-game with `/cc edit <scene>`, movement recording (`/cc rec start`), and particle path previews (`/cc path`).
- **Point Actions**: Attach `TITLE`, `ACTIONBAR`, `SOUND`, `MSG`, `CMD`, `PLAYERCMD`, and `SCRIPT` actions to individual points.
- **Trigger System**: Automatically start scenes on `REGION_ENTER`, `RIGHT_CLICK_BLOCK`, or `COMMAND` with cooldowns, one-time flags, and permission checks.
- **1.21.6 – 1.26.3 Double-Shift Skip**: Supports both legacy `ENTITY_ACTION` and modern `PLAYER_INPUT` sneak packets.
- **Import / Export & Localization**: Portable scene files (`exports/` and `imports/`) with built-in English (`en_US`) and Vietnamese (`vi_VN`) language files.

## Requirements

- **Server**: Paper, Purpur, Leaf, or Folia **1.21 – 1.26.3**
- **Java**: Java 21+ (Java 25 for 26.1–26.3)
- **Dependency**: [PacketEvents](https://modrinth.com/plugin/packetevents) (`2.11.2+` for 1.21.x, `2.14.0+` for 26.1–26.3)

## Main Commands

Primary command: `/cookiecamera` (alias: `/cc`)

- `/cc help [page]` — Show built-in help
- `/cc reload` — Reload config, language files, scenes, triggers, and scripts
- `/cc new <id>` / `/cc del <id>` / `/cc list [page]` — Manage scenes
- `/cc edit <id>` / `/cc exit` — Enter or leave in-game hotbar edit mode
- `/cc add` / `/cc insert <idx>` / `/cc rm <idx>` / `/cc set <idx> <param> [val]` — Manage camera points
- `/cc rec start` / `/cc rec stop` — Auto-record camera points while moving
- `/cc play <id> [player|all]` / `/cc pause` / `/cc stop [player|all]` — Control playback
- `/cc action <idx> add|list|rm ...` — Manage point actions
- `/cc trigger ...` — Create and configure scene triggers
- `/cc export <id>` / `/cc import <file>` — Export or import scene files

## Permissions

- `cookiecamera.admin` — Full editing, trigger, and reload commands (default `op`)
- `cookiecamera.play` — Play scenes for yourself (default `op`)
- `cookiecamera.play.other` — Play scenes for other players (default `op`)

## Building from Source

Requires **Java 21+** (Java 25 for the `verify-26.3` profile) and **Apache Maven 3.9+**.

```bash
git clone https://github.com/KhoaSuperT1/CookieCamera.git
cd CookieCamera
mvn package
```

Output JAR: `target/CookieCamera-1.1.0.jar`
