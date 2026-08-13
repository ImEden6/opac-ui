# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.2] - 2026-08-13

### Fixed
- The kick confirmation popup now keeps a small gap from the edge of the screen instead of potentially crowding right up against it on smaller windows.

## [1.0.1] - 2026-08-03

### Fixed
- Party screen no longer shows Cloth Config's tiled checkerboard background; it now dims the world behind the UI instead (`ConfigBuilder.setTransparentBackground(true)`).
- Added missing `pack.mcmeta` to the Fabric and Forge modules, silencing a "failed to load a valid ResourcePackInfo" warning on dev client launch.
