# opac-ui

A client-side mod that adds an in-game party management GUI for [Open Parties and Claims](https://github.com/thexaero/open-parties-and-claims).

This branch targets **Minecraft 26.1.2** and ships for **Fabric, Forge, and NeoForge**. The party screen no longer relies on the Cloth Config library - it's built with Minecraft's own built-in UI pieces instead, since Cloth Config doesn't support Forge at all on this version.

## Requirements

- Minecraft 26.1.2 (Fabric, Forge, or NeoForge)
- Java 25 (bundled with the game; only relevant if building from source)
- Open Parties and Claims (≥ 26.1.2)
- Fabric API - Fabric only
- Forge Config API Port - Fabric and Forge only (required by Open Parties and Claims on those loaders; not needed on NeoForge)

## Usage

Press `P` (configurable in key bindings) to open the party manager.

- **Not in a party** - Create Party button
- **In a party** - Tabbed screen with three tabs:
  - **Members** - view ranked member list; promote/demote, kick, and transfer ownership (permission-dependent)
  - **Invite** - invite players by name; revoke pending invites
  - **Allies** - view allied parties; unally (permission-dependent)

## License

LGPL-3.0-only - see [LICENSE](LICENSE). Matches the Open Parties and Claims project.
