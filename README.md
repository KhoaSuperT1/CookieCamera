<div align="center">

# CookieCamera

**In-game cinematic cutscenes, spline camera paths, triggers, and point scripts for Paper, Purpur, Leaf, and Folia.**

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21--1.26.3-16a34a?style=flat-square)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-21%20%7C%2025-ea580c?style=flat-square)](https://adoptium.net)
[![PacketEvents](https://img.shields.io/badge/PacketEvents-2.11%E2%80%932.14-2563eb?style=flat-square)](https://modrinth.com/plugin/packetevents)
[![Version](https://img.shields.io/badge/Version-1.1.0-334155?style=flat-square)](https://github.com/KhoaSuperT1/CookieCamera)

[Original Project (AnkiCamera)](https://modrinth.com/plugin/ankicamera) · [Support Discord](https://discord.gg/NZycRMWrSS)

</div>

---

> [!NOTE]
> **Origin & AI Notice:**
> CookieCamera is an updated fork/build based on [AnkiCamera by AnkiTeam](https://modrinth.com/plugin/ankicamera).
> Roughly **80%** of the source recovery, cleanup, and `1.21 – 1.26.3` update work was done with AI assistance. Please test your scenes and triggers on a staging server before running them in production.

## Overview

CookieCamera lets you build and preview cutscenes directly inside Minecraft using hotbar tools or movement recording, without asking players to install any client mods. Attach titles, sounds, commands, or custom scripts to individual points and trigger scenes by region, block interaction, or command.

### Changes in v1.1.0

- Verified compatibility for **Minecraft 1.21 – 1.26.3** (Java 21 on 1.21.x, Java 25 on 26.x).
- Updated PacketEvents integration for **PacketEvents 2.14.0** (while remaining compatible with `2.11.2+`).
- Fixed double-Shift cutscene skipping on **1.21.6 – 1.26.3** by tracking `Client.PLAYER_INPUT` rising edges alongside `Client.ENTITY_ACTION`.
- Switched script/trigger console command dispatch and point teleports to Folia-safe schedulers.
- Hardened `Registry.SOUNDS` lookups so custom or namespaced sound keys fall back cleanly.

## Features

- **Interpolation & Easing:** `CATMULL_ROM`, `BSPLINE`, `HERMITE`, and `LINEAR` paths with configurable speed, tension, and smoothing.
- **Hotbar Editor & Recording:** Edit scenes visually with `/cc edit <scene>`, auto-record points while flying (`/cc rec start`), and toggle particle path previews (`/cc path`).
- **Point Actions:** Attach `TITLE`, `ACTIONBAR`, `SOUND`, `MSG`, `CMD`, `PLAYERCMD`, or `SCRIPT` actions to any point.
- **Triggers:** Run scenes on `REGION_ENTER`, `RIGHT_CLICK_BLOCK`, or `COMMAND` with cooldowns, one-time flags, and permission checks.
- **Import / Export & Languages:** Portable scene files under `exports/` and `imports/`, plus built-in `en_US` and `vi_VN` translations.
- **Folia Support:** Uses entity, global, and async schedulers (`folia-supported: true`).

## Requirements

| Requirement | Version |
| :--- | :--- |
| Server Platform | Paper, Purpur, Leaf, or Folia (`1.21 – 1.26.3`) |
| Java Runtime | Java 21+ (Java 25 for `26.1 – 26.3`) |
| Required Plugin | [PacketEvents](https://modrinth.com/plugin/packetevents) (`2.11.2+` for 1.21.x, `2.14.0+` for 26.x) |

## Commands & Permissions

Primary command: `/cookiecamera` (alias: `/cc`)

| Command Group | Usage |
| :--- | :--- |
| General | `/cc help [page]`, `/cc reload` |
| Scenes | `/cc new <id>`, `/cc del <id>`, `/cc list [page]`, `/cc copy <src> <dst>`, `/cc info [id]`, `/cc reverse <id>`, `/cc smooth <id>` |
| Editor | `/cc edit <id>`, `/cc exit`, `/cc add`, `/cc insert <idx>`, `/cc rm <idx>`, `/cc clear [id]`, `/cc pt <idx>`, `/cc tp <idx>` |
| Properties | `/cc set <idx> <duration\|fov\|yaw\|pitch\|x\|y\|z\|ease\|pos> [val]`, `/cc setting <speed\|name\|markers\|mode\|tension\|smoothing> <val>` |
| Recording & Path | `/cc rec start \| stop`, `/cc path [on\|off]` |
| Playback | `/cc play <id> [player\|all]`, `/cc pause`, `/cc stop [player\|all]` |
| Actions & Scripts | `/cc action <idx> list \| rm <actionIdx> \| add <type> <value...>`, `/cc script <name>` |
| Triggers | `/cc trigger add\|del\|list\|info\|set\|setregion\|setblock\|fire ...` |
| Import / Export | `/cc export <id>`, `/cc import <file>` |

Permissions (default `op`; regular players can still watch cutscenes played by triggers or console):

- `cookiecamera.admin` — Scene editing, triggers, and reload commands
- `cookiecamera.play` — Play scenes for yourself
- `cookiecamera.play.other` — Play scenes for other players

## Building from Source

Requires **Java 21+** (Java 25 for the `verify-26.3` profile) and **Apache Maven 3.9+**.

```bash
git clone https://github.com/KhoaSuperT1/CookieCamera.git
cd CookieCamera
mvn package
```

Compiled artifact: `target/CookieCamera-1.1.0.jar`

## Support

- **Discord:** https://discord.gg/NZycRMWrSS

---

<div align="center"><sub>Made with love and care by <b>Java-Cookieee</b> &amp; AI.</sub></div>
