# opac-ui

A client-side mod that adds an in-game party management GUI for [Open Parties and Claims](https://github.com/thexaero/open-parties-and-claims) using Cloth Config's tabbed-list UI.

This branch targets **Minecraft 1.21.1** and ships for **Fabric, Forge, and NeoForge**.

> **⚠️ 1.21.1 disclaimer:** if Video Settings → **Menu Background Blurriness** is above 0, the party screen's background renders solid black instead of the normal dimmed/blurred world behind the tabbed UI. Set it to 0 as a workaround. This reproduces independent of Cloth Config (confirmed on a from-scratch vanilla-widgets rebuild too) and only affects `PartyScreen` specifically — other screens in this mod render correctly under blur. See [CHANGELOG.md](CHANGELOG.md) for details. The UI is otherwise fully functional.

## Requirements

- Minecraft 1.21.1 (Fabric, Forge, or NeoForge)
- Open Parties and Claims (≥ 1.21.1)
- Cloth Config API (v15.x)
- Fabric API — Fabric only
- Forge Config API Port — Fabric and Forge only (required by Open Parties and Claims on those loaders; not needed on NeoForge)

## Usage

Press `P` (configurable in key bindings) to open the party manager.

- **Not in a party** — Create Party button
- **In a party** — Tabbed screen with three tabs:
  - **Members** — view ranked member list; promote/demote, kick, and transfer ownership (permission-dependent)
  - **Invite** — invite players by name; revoke pending invites
  - **Allies** — view allied parties; unally (permission-dependent)

## License

LGPL-3.0-only — see [LICENSE](LICENSE). Matches the Open Parties and Claims project.
