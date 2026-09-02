# opac-ui

A client-side mod that adds an in-game party management GUI for [Open Parties and Claims](https://github.com/thexaero/open-parties-and-claims) using Cloth Config's tabbed-list UI.

## Requirements

- Minecraft 1.20.1 (Fabric)
- Fabric API
- Forge Config API Port (Fabric only - required by Open Parties and Claims' Fabric build)
- Open Parties and Claims (≥ 1.20.1)
- Cloth Config API (≥ 11.x)

## Usage

Press `P` (configurable in key bindings) to open the party manager.

- **Not in a party** - Create Party button
- **In a party** - Tabbed screen with three tabs:
  - **Members** - view ranked member list; promote/demote, kick, and transfer ownership (permission-dependent)
  - **Invite** - invite players by name; revoke pending invites
  - **Allies** - view allied parties; unally (permission-dependent)

## Branches

This mod is maintained as one branch per Minecraft version - see each
branch's own README and CHANGELOG on [GitHub](https://github.com/ImEden6/opac-ui)
for loader-specific setup notes.

opac-ui always requires [Open Parties and Claims](https://modrinth.com/mod/open-parties-and-claims)
- it's a client-side add-on, not a standalone mod. Exact versions per branch:

| Branch | Minecraft | Loader(s) | Open Parties and Claims | Cloth Config API | Fabric API | Forge Config API Port |
| --- | --- | --- | --- | --- | --- | --- |
| `1.20.1/main` | 1.20.1 | Fabric | ≥ 1.20.1 | ≥ 11.x | 0.92.11+1.20.1 | Fabric only |
| `1.21.1/main` | 1.21.1 | Fabric, Forge, NeoForge | ≥ 1.21.1 | ≥ 15.x | 0.105.0+1.21.1 (Fabric) | Fabric and Forge (not needed on NeoForge) |
| `26.1.2/main` | 26.1.2 | Fabric, Forge, NeoForge | ≥ 26.1.2 | not used | 0.145.4+26.1.2 (Fabric) | Fabric and Forge (not needed on NeoForge) |
| `26.2/main` | 26.2 | Fabric, Forge, NeoForge | ≥ 26.2 | not used | 0.159.0+26.2 (Fabric) | Fabric and Forge (not needed on NeoForge) |

Notes:
- Cloth Config was dropped starting with the `26.1.2/main` branch since it
  no longer ships a Forge build - the party screen there is built entirely
  with Minecraft's own UI components instead.
- `26.1.2/main` and `26.2/main` also need Java 25 (bundled with the game;
  only relevant when building from source).

## License

LGPL-3.0-only - see [LICENSE](LICENSE). Matches the Open Parties and Claims project.
