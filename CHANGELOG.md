# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - Unreleased

### Investigated
- Looked at removing the party screen's background texture (as done on the 1.20.1 line via `ConfigBuilder.setTransparentBackground(true)`). Cloth Config's 1.21.1 build always draws vanilla's `INWORLD_MENU_BACKGROUND` texture for the full-screen background regardless of that setting; enabling it instead corrupted the frame to solid black, because it also triggers Cloth Config's manual `renderBlurredBackground` call (real `GameRenderer` PostChain processing + a render-target rebind) from inside a screen nested in `PartyScreen`, outside the sequence vanilla expects it to run in. Reverted; left as vanilla's native texture for now — a real fix would need a Mixin over Cloth Config's background rendering.
