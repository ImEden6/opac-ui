# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - Unreleased

### Known Issues
- Party screen's background renders solid black instead of the normal dimmed/blurred world, but **only when Video Settings → Menu Background Blurriness is above 0**. Confirmed with `menuBackgroundBlurriness:0` the background renders correctly; confirmed black again as soon as blur is re-enabled (tested at `5`). This is **not** a Cloth Config bug — a from-scratch rebuild of `PartyScreen` on plain vanilla `ContainerObjectSelectionList` widgets (no Cloth Config anywhere in the class) reproduced the exact same black-with-blur/clean-without-blur behavior, so the earlier theory blaming Cloth Config's 1.21.x port (`shedaniel/cloth-config#267`) turned out to be a red herring — that upstream issue may still be real, but it isn't what's happening here. What *is* specific to `PartyScreen`: `ConfirmActionScreen` (this mod's other custom screen, e.g. the disband/kick confirmation dialogs) renders correctly under blur every time, with no changes needed. Both screens call vanilla's `Screen#renderBackground` the same way, so whatever's breaking `PartyScreen`'s interaction with `GameRenderer#processBlurEffect` is something structural to that screen specifically (candidates not yet ruled out: its `tick()` override doing per-tick work no other screen here does, or the sheer widget count) — not yet root-caused. Workaround: set blur to 0. Revisit before release.
