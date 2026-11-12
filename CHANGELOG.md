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
- With menu background blur on, the party screen and confirm dialog no longer blur their own lists and text ([#5](https://github.com/ImEden6/opac-ui/issues/5)).

## [1.1.0] - 2026-10-29

### Added
- A Claims tab that lists each party member's claim and forceload counts, with your own limits on your row. Hover a row to see how many claims they have in each dimension.
- With Xaero's World Map installed, each Claims row gets a map button that opens the World Map on that member's nearest claim. If all their claims are in another dimension, the button is greyed out and says which one has the most.

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
