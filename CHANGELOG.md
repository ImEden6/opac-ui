# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.5] - 2026-10-29

### Fixed
- Typing a name in the add box no longer crashes the game.
- Name suggestions in the add box show each player's skin instead of a default one.

## [1.0.4] - 2026-10-20

### Changed
- Now built against Open Parties and Claims 0.33.0 for Fabric, Forge and NeoForge.

### Fixed
- The mod now only loads on Minecraft 1.21.1. It used to accept any 1.21.x version and then crash on later ones such as 1.21.11, whose GUI and input code changed.

## [1.0.3] - 2026-09-29

### Fixed
- The party rename box now accepts up to 100 characters, matching the server limit. It was capped at 32, which also cut off longer existing names when the box loaded them.
- Party actions work again. Open Parties and Claims (OPAC) 0.31.6 renamed its command prefixes, and the mod now sends the new ones.

## [1.0.2] - 2026-09-29

### Changed
- Now built against the newest Open Parties and Claims 1.21.1 releases for Fabric, Forge and NeoForge.

## [1.0.1] - 2026-08-13

### Fixed
- The kick confirmation popup now keeps a small gap from the edge of the screen instead of potentially crowding right up against it on smaller windows.

## [1.0.0] - Release

### Known Issues
- Party screen's background renders solid black instead of the normal dimmed/blurred world, but **only when Video Settings → Menu Background Blurriness is above 0**. 
