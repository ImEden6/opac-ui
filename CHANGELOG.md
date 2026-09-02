# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.3]

### Fixed
- Development builds for Forge were failing to set up during testing.
  Doesn't affect the released mod.

## [1.0.2] - 2026-08-13

### Fixed
- The kick confirmation popup now keeps a small gap from the edge of the screen instead of potentially crowding right up against it on smaller windows.

## [1.0.1] - 2026-08-06

### Fixed
- Buttons overlapping on party screen rows.

## [1.0.0] - Released

### Changed
- Ported to Minecraft 26.1.2.
- Dropped Cloth Config, the library the party screen's tabbed layout used
  to be built on. It doesn't support Forge at all on this version, so
  the screen was rebuilt using only Minecraft's own built-in UI pieces
  instead. Works the same as before across Fabric, Forge, and NeoForge.

### Fixed
- The black-background bug present on the 1.21.1 branch when Menu Background Blurriness is enabled does not reproduce here, confirmed with blur on.
