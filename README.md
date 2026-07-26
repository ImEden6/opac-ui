# opac-ui

A Fabric 1.20.1 client-side mod that adds an in-game party management GUI for [Open Parties and Claims](https://github.com/thexaero/open-parties-and-claims) using Cloth Config's tabbed-list UI.

## Requirements

- Minecraft 1.20.1 (Fabric)
- Fabric API
- Open Parties and Claims (≥ 1.20.1)
- Cloth Config API (≥ 11.x)

## Usage

Press `P` (configurable in key bindings) to open the party manager.

- **Not in a party** — Create Party button
- **In a party** — Tabbed screen with three tabs:
  - **Members** — view ranked member list; promote/demote, kick, and transfer ownership (permission-dependent)
  - **Invite** — invite players by name; revoke pending invites
  - **Allies** — view allied parties; unally (permission-dependent)

## License

LGPL-3.0-only — see [LICENSE](LICENSE). Matches the Open Parties and Claims project.
