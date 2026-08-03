# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - Unreleased

### Known Issues
- Party screen's background renders solid black behind the Cloth Config UI (`buildClothScreen` in `PartyScreen`). Confirmed this is unrelated to `ConfigBuilder.setTransparentBackground` — it's black with the flag `true`, `false`, or unset, with no exceptions or GL errors logged either way. The one thing all three have in common is vanilla's `Screen#renderMenuBackground`, which Cloth Config's 1.21.1 build (v15.0.140, cloth-config-neoforge/-forge) always calls to paint the full-screen background. Cloth Config's own documented compatibility table only lists up through v11/1.20.x — 1.21.x + NeoForge builds exist and are published, but are past what's actually documented/tested, and our usage (a `ClothConfigScreen` built via `ConfigBuilder` and rendered nested inside another `Screen`, `PartyScreen`, rather than as the top-level `Minecraft.screen`) is exactly the kind of edge case likely to fall outside that testing. Not investigating further for now since this branch isn't released yet; revisit alongside either an older Cloth Config build or a Mixin over its background rendering.
