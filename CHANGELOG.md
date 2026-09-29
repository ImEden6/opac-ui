# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.4] - 2026-09-29

### Changed
- Party commands now use OPAC's renamed command prefixes: `/oparties` (was `/openpac-parties`) and `/opac` (was `/openpac`). Without this, every party action would fail with an unknown command on OPAC 0.31.6.

### Fixed
- The party rename box now accepts up to 100 characters, matching the server limit. It was capped at 32, which also cut off longer existing names when the box loaded them.

## [1.0.3] - 2026-09-18

### Fixed
- Fixed a rare crash (`ConcurrentModificationException`) when opening the party screen while a member's skin was still being fetched. The render thread was reading a player profile at the same moment the background skin download was writing to it.
- Members with no UUID no longer make the background skin fetch fail with an error. They now just show the default skin.

## [1.0.2] - 2026-08-13

### Fixed
- The kick confirmation popup now keeps a small gap from the edge of the screen instead of potentially crowding right up against it on smaller windows.

## [1.0.1] - 2026-08-03

### Fixed
- Party screen no longer shows Cloth Config's tiled checkerboard background; it now dims the world behind the UI instead (`ConfigBuilder.setTransparentBackground(true)`).
- Added missing `pack.mcmeta` to the Fabric and Forge modules, silencing a "failed to load a valid ResourcePackInfo" warning on dev client launch.
