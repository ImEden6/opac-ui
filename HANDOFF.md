# Handoff: Party screen background on 1.21.1

## Goal

Fix the party screen's background on the `1.21.1/main` branch (this worktree,
`D:\projects\opac-ui-1.21`) so it matches what's already shipped on
`1.20.1/main` (the other worktree, `D:\projects\opac-ui`): no checkerboard/
custom texture behind the tabbed party UI, just the world dimmed.

On 1.20.1 this was a one-line fix: `ConfigBuilder.setTransparentBackground(true)`
in `buildClothScreen()`, committed as `c26dc98`. **That fix does not translate
to 1.21.1** — this is the whole story below.

## Current state (as of commit `09240c6`)

- `common/src/main/java/mervyn/opacui/client/gui/PartyScreen.java`'s
  `buildClothScreen()` has `.setTransparentBackground(false)` explicitly set
  (not omitted — the default on this Cloth Config version is `true`, see
  below). This is a deliberate, safer-but-still-broken choice, not a fix.
- **The background still renders solid black** behind the Cloth Config UI
  (buttons/text/list rows all draw fine on top). This is worse than the
  original checkerboard, and is an open, unfixed bug.
- Confirmed on **NeoForge** only, via this project's own `:neoforge:runClient`
  Gradle task. **Not yet confirmed on classic Forge** — see "Interrupted
  work" below.
- `CHANGELOG.md` on this branch documents this as a "Known Issues" entry
  under `[1.0.0] - Unreleased`.
- Working tree is clean, branch is up to date with `origin/1.21.1/main`.

## What didn't work (don't repeat these)

Tested exhaustively, all still produced the exact same solid-black result:

1. `.setTransparentBackground(true)` — no visual change from `false`.
2. Reordering `PartyScreen.render()` so its own fallback `g.fill(...)` only
   runs when `clothScreen == null` — harmless cleanup, irrelevant, since
   `clothScreen.render(...)` fully overpaints the screen regardless.
3. Theory: "blur is corrupting the frame" — **ruled out**. Decompiled
   `GameRenderer.processBlurEffect(float)` directly from the real 1.21.1
   client jar: it's a hard no-op unless
   `Options.getMenuBackgroundBlurriness() >= 1`, which defaults to `0`. Blur
   never actually runs under default settings.
4. Theory: "the render-target rebind (`bindWrite(false)`) in
   `renderBlurredBackground()` corrupts GL state" — **ruled out**. Explicitly
   set `.setTransparentBackground(false)` (which skips that whole method
   entirely, per Cloth Config's decompiled `ClothConfigScreen#render`
   bytecode) and it was **still black**. This proves the bug isn't in that
   code path at all.
5. Added temporary diagnostic logging (`LOGGER.warn` around
   `clothScreen.render(...)`, GL error checks via `GL11.glGetError()` before/
   after, try/catch(Throwable) around the call) — confirmed **zero GL errors,
   zero exceptions**, `mc.level != null` (in a real world), correct
   width/height. This logging has since been **fully removed** (see commit
   `09240c6`) — don't bother re-adding it verbatim, but the pattern (guard
   field + one-shot log in `render()`) is a fine starting point if resuming
   low-level GL investigation.

## What we know for sure

- The **only code path common** to both `transparentBackground=true` and
  `=false` is vanilla `Screen#renderMenuBackground(GuiGraphics)`, which blits
  `Screen.INWORLD_MENU_BACKGROUND` (`textures/gui/inworld_menu_background.png`).
  That's the prime suspect — confirmed via decompiling both Cloth Config's
  `ClothConfigScreen.class` and vanilla's `Screen.class` from the actual
  1.21.1 client jar (`minecraft-clientonly-1.21.1-...jar` in the Loom cache).
- The texture itself is fine: extracted and viewed it, it's a legitimate
  16×16 grayish tile, not corrupted/missing. So it's a rendering-correctness
  bug (likely blend-state or something else GL-side), not an asset problem.
- **Cloth Config's own docs** (shedaniel.gitbook.io/cloth-config) only list
  support up through "v11 (Stable) — Supports 1.20.x." No 1.21.x entry at
  all, despite the maintainer publishing 1.21.1 builds (we're on `v15.0.140`,
  curse.maven file IDs `5729125`/`5729126`/`5729127` for fabric/forge/
  neoforge — pinned in `gradle.properties`). Working theory: this newer
  build's background rendering assumes it owns the top-level
  `Minecraft.screen` and does real per-frame vanilla calls
  (`renderBlurredBackground`/`renderMenuBackground`) that may not behave
  correctly when invoked as a nested sub-screen the way `PartyScreen` does
  (`clothScreen.render(...)` called directly from `PartyScreen.render()`,
  bypassing `Screen#renderWithTooltip`, which is the method vanilla's own
  render loop actually calls). This is a plausible but **not conclusively
  proven** root cause — would need a GPU frame debugger (RenderDoc or
  similar) to pin down further, which wasn't available in this environment.

## Cloth Config usage scope (relevant if considering a rewrite)

Only 4 files use `me.shedaniel.clothconfig2` anywhere in this codebase:
- `common/src/main/java/mervyn/opacui/client/gui/PartyScreen.java`
- `common/src/main/java/mervyn/opacui/client/gui/entry/MemberEntry.java`
- `common/src/main/java/mervyn/opacui/client/gui/entry/InviteEntry.java`
- `common/src/main/java/mervyn/opacui/client/gui/entry/AllyEntry.java`

Usage is shallow: `ConfigBuilder`/`ConfigCategory` purely for tab scaffolding,
and `TooltipListEntry<Void>` purely as a scrollable-list-row base class with
fully custom `render()` bodies. **None** of Cloth Config's actual
config-editing features (toggles/sliders/real fields) are used — it's being
used as generic "tabbed list with custom rows" UI scaffolding, nothing more.

## YACL research (alternative considered)

- YACL (YetAnotherConfigLib) **does not support classic Forge past 1.20.1**
  (confirmed from the maintainer's own docs: "Forge (LexForge) is not and
  will not be supported past 1.20.1"). It does support Fabric 1.21.1 and
  NeoForge 1.21.1.
- This branch ships **three** loaders (`fabric`, `forge`, `neoforge`), so a
  straight YACL swap would drop Forge support for this MC version.
- Given how shallow the actual Cloth Config usage is (see above), the
  strongest option going forward is probably to **drop third-party config
  libraries entirely** and rebuild this small tabbed-list UI on vanilla's own
  `ObjectSelectionList` — zero dependency, no Forge-support gap, and immune
  to this whole class of "library assumes it owns the top-level Screen" bug.
  This was floated as a recommendation but **not yet decided**.
- User was presented three options: (a) vanilla-only rebuild [recommended],
  (b) YACL for Fabric/NeoForge + keep Cloth Config for Forge [dual-library],
  (c) YACL everywhere + drop Forge for 1.21.1. **User picked (b)**, but
  qualified: *"I want to test cloth config with forge first before anything
  else"* — i.e. before committing to keeping Cloth Config specifically for
  Forge, confirm whether classic Forge has the same black-screen bug NeoForge
  does. If Forge is unaffected, (b) makes sense. If Forge is *also* affected,
  (b) doesn't actually solve anything and needs rethinking (probably pushes
  toward the vanilla-only rebuild for all three loaders).

## Interrupted work — pick up here

We were mid-way through testing Cloth Config on **classic Forge** (1.21.1)
when this got derailed twice:

1. `:forge:runClient` (Gradle dev client, this worktree) hung with zero
   output and no `forge/run/logs/latest.log` created for 6+ minutes — looked
   dead (java process memory dropped from ~1GB to a few KB), so it was killed
   via `TaskStop`. Cause was never diagnosed; this may have just been a
   one-time cold-start cost since it was the very first time `:forge:runClient`
   was run on this specific worktree (Forge dev environments do more
   first-run setup than Fabric/NeoForge). Worth just retrying — it may work
   fine on a second attempt now that the Forge userdev/patched jar is likely
   cached.
2. User then tried manually dragging the built jar
   (`forge/build/libs/opacui-forge-1.21.1-1.0.0.jar`) into a real launcher
   (ATLauncher, instance "Minecraft1211withForge") to test outside Gradle.
   Hit an **unrelated** crash before ever reaching the party screen:
   `OpenPartiesAndClaims` (the party mod this UI integrates with) failed to
   save its own server config
   (`com.electronwill.nightconfig.core.io.WritingException` →
   `AccessDeniedException` on an atomic rename of
   `openpartiesandclaims-server.toml.new.tmp`). Nothing to do with `opacui`
   or this bug — `opacui-forge-1.21.1-1.0.0.jar` loaded fine (`DONE` in the
   crash report's mod list). Almost certainly a transient Windows file-lock
   issue (antivirus, OneDrive/cloud sync on that ATLauncher folder, or an
   indexer) on the user's machine, not a code bug. User needs to retry the
   launch and/or check whether that instance folder is under a synced/
   antivirus-scanned path.

**Next step**: get a clean run to the party screen on classic Forge (1.21.1)
— either retry `:forge:runClient` or retry the ATLauncher launch — and
observe whether the background is black there too, same as NeoForge. That
answer determines whether plan (b) above (keep Cloth Config for Forge) is
viable, or whether all three loaders need the same fix (pointing toward the
vanilla `ObjectSelectionList` rebuild instead).

No plan file was written for the YACL/rebuild decision yet (we were in
formal Plan Mode heading toward one, `luminous-kindling-quill.md`, but got
interrupted before Phase 4 — that file does not exist). Whoever picks this
up should re-run `EnterPlanMode` once the Forge answer is in, using this
document plus the "YACL research" and "Cloth Config usage scope" sections
above as the research base — no need to redo any of that investigation.
