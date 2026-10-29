# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-10-29

### Added
- A Claims tab that lists each party member's claim and forceload counts, with your own limits on your row. Hover a row to see how many claims they have in each dimension.
- With Xaero's World Map installed, each Claims row gets a map button that opens the World Map on that member's nearest claim. If all their claims are in another dimension, the button is greyed out and says which one has the most.

## [1.0.2] - 2026-10-20

### Changed
- Now built against Open Parties and Claims 0.33.0 for Fabric, Forge and NeoForge.

## [1.0.1] - 2026-09-29

### Changed
- Now built against the newest Open Parties and Claims 26.2 releases for Fabric, Forge and NeoForge.

### Fixed
- The party rename box now accepts up to 100 characters, matching the server limit. It was capped at 32, which also cut off longer existing names when the box loaded them.

## [1.0.0]

### Changed
- Ported to Minecraft 26.2.

### Fixed
- Party commands (create, invite, kick, rank, transfer, leave, disband,
  allies) stopped working after the update — Open Parties and Claims
  changed its command names for this version and opac-ui hadn't caught up.
- Development builds for Forge and NeoForge were failing to set up during
  testing. Doesn't affect the released mod.
