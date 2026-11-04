# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.0] - 2026-11-04

### Added
- Hover an online party member to see where they are: dimension, coordinates, and how far away they are if they're in your dimension. It uses the locations OPAC already shares with your party, the same ones Xaero's maps show.
- Your own row on the Members tab has a Sharing/Hidden button that turns OPAC's "share location with party" setting on or off, so you don't have to type the player-config command. It's greyed out if the server doesn't let players change it.

### Fixed
- Typing in the invite box no longer gets wiped when someone joins the party or a rank changes.
- The party screen no longer rebuilds every tab whenever any player on the server logs in or out. Online dots now update on their own.

## [1.1.0] - 2026-10-29

### Added
- A Claims tab that lists each party member's claim and forceload counts, with your own limits on your row. Hover a row to see how many claims they have in each dimension.
- With Xaero's World Map installed, each Claims row gets a map button that opens the World Map on that member's nearest claim. If all their claims are in another dimension, the button is greyed out and says which one has the most.

## [1.0.1] - 2026-10-20

### Changed
- Now built against Open Parties and Claims 0.33.0 for Fabric, Forge and NeoForge.

## [1.0.0] - 2026-10-07

### Changed
- Ported to Minecraft 26.3, built against Open Parties and Claims 0.32.8 for Fabric, Forge and NeoForge.

