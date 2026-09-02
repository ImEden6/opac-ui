# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0]

### Changed
- Ported to Minecraft 26.2.

### Fixed
- Party commands (create, invite, kick, rank, transfer, leave, disband,
  allies) stopped working after the update — Open Parties and Claims
  changed its command names for this version and opac-ui hadn't caught up.
- Development builds for Forge and NeoForge were failing to set up during
  testing. Doesn't affect the released mod.
