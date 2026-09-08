# 09 — Project Status: What Works, What Doesn't

An honest audit as of the current working tree (one commit, `89bafd3 "update"`, plus substantial uncommitted work).

> **Last updated: Session 6, 2026-09-08 (implementation).** Session 5 (planning, same day) re-audited the source after Session 4's changes and found **eight further defects (#13–#20)**, four of them in code that Session 4 shipped, and verified bug #12 fixed. Session 6 then implemented Track A follow-up work: bugs **#13, #15, #16, #18, #19 fixed**, and bug **#8 fixed by construction** via a real navigation-compose rewrite (`VAHI-001`–`VAHI-003`, `VAHI-005`, `VAHI-201` — see `PROJECT-LOG.md`). Bug **#1 is half-fixed** — navigation position now survives rotation, canvas *content* still does not. The reframe in [`11-forward-plan.md`](11-forward-plan.md) Part 3 still holds — most of the remaining list is symptoms of missing foundations, chiefly **#14** (palm rejection — still critical, still unfixed) and persistence.

---

## Summary

Vahi is a **UI prototype with a working ink engine and no data layer**. You can walk the full flow — splash → auth → home → canvas — and draw on the page. Nothing you do persists past the moment, and no button that isn't part of that happy path does anything.

| Area | State |
|---|---|
| Visual design / layout | **Substantially complete** for tablet |
| Navigation | **Works.** Rewritten onto `navigation-compose` with a real back stack and type-safe routes (Session 6) |
| Splash | **Works**, now with error + timeout handling (Session 4) |
| Authentication | **Cosmetic only** — no validation, no backend |
| Notebook library | **Cosmetic only** — two hardcoded notebooks |
| Canvas + paper styles | **Works** |
| Freehand drawing (pen) | **Works** |
| Eraser / undo / redo / colour / width | **Works — toolbar added (Session 4).** Session 5 found four defects (#13, #15, #16, #18); all four fixed in Session 6. Still not on-device verified. |
| Palm rejection / multi-touch | **Actively wrong** — see bug #14. The palm draws instead of the stylus |
| Persistence | **Does not exist** |
| Networking / backend | **Does not exist** |
| ViewModels / domain / data layers | **Scaffolded, empty** |
| Phone support | **Does not exist by design (D2)** — Auth shows an honest message now instead of a debug string (Session 4) |
| Tests | **None** beyond templates |

---

## ✅ What genuinely works

1. **The full navigation loop.** Splash advances on video end; Auth advances on tap; Home opens a canvas; the canvas returns to Home. No crashes on the happy path.
2. **Video splash.** ExoPlayer, muted, plays once, releases correctly on dispose, with an animated gradient background.
3. **The Home layout.** Top bar, filters, toolbar, and the notebook grid all render.
4. **Overlay choreography.** Three mutually-exclusive dropdowns plus a full-screen modal, with an 8 dp blur applied to the content behind the modal, dismissal via scrim tap / Escape key / system back, and a `BackHandler` that closes the modal before the dropdowns.
5. **All three paper styles.** Blank, ruled (vertical rules every 32 dp), and dot grid all draw correctly.
6. **Freehand pen drawing.** Raw pointer capture, path accumulation, live rendering with round caps and joins. It feels responsive because it bypasses gesture detectors.
7. **The design system files.** Complete Material 3 colour schemes, a full 15-role type scale, and a shape scale — all correctly wired into `MaterialTheme`.
8. **Build hygiene.** Kotlin DSL, version catalog, centralised repositories, modern AGP/Kotlin. The build is clean and conventional.

---

## 🟡 Implemented but unreachable

**Update (Session 4): the canvas toolbar now exists** — `ui/components/drawing/DrawingToolbar.kt`, wired into `CanvasScaffold.kt`. Six of the eight items below are now reachable. Verified by compilation and a full `assembleDebug`; **not yet visually verified on-device.**

| Feature | Where | Status |
|---|---|---|
| **Eraser tool** | `CanvasState.eraseAtPosition`, `DrawingCanvas` ERASER branch | ✅ Reachable — toolbar pen/eraser toggle |
| **Undo / redo** | `CanvasState.undo/redo/canUndo/canRedo` | ✅ Reachable — toolbar buttons, disabled state reflects `canUndo()`/`canRedo()` |
| **Clear canvas** | `CanvasState.clearAll` | ✅ Reachable — toolbar Delete icon, disabled when no strokes |
| **Stroke colour** | `CanvasState.strokeColor` | ✅ Reachable — 5 fixed swatches on the toolbar (no custom colour picker yet) |
| **Stroke width** | `CanvasState.strokeWidth` | ✅ Reachable — 3 fixed presets (2f/4f/8f; no custom width picker yet) |
| **Cancel stroke** | `CanvasState.cancelStroke` | ⬜ Still no caller — there's no gesture (e.g. two-finger tap while drawing) mapped to it yet |
| **MVVM base class** | `BaseViewModel<State>` | ⬜ Still no subclass — waits on Track C (F3) |
| **Tablet workspace layout** | `NotebookScreen.kt` | 🗑️ Deleted (Session 4) — it was dead code; its intent (a persistent AI/research side panel) is preserved in `11-forward-plan.md` Track E |

**Deferred to a later pass, deliberately:** the toolbar exposes colour/width as direct swatches rather than the fuller "tap-again-to-configure a pen slot" popover from `13-visual-design-system.md` §8.2, and there's no handedness setting yet to mirror it to the right edge. Both are noted as follow-ups in `00-START-HERE.md` §2a.

---

## 🔴 Not implemented

### Data and persistence — the biggest gap
- No database, no file storage, no preferences, no serialisation.
- **Strokes vanish** the moment you leave the canvas or rotate the device (`remember`, not `rememberSaveable`, not ViewModel-scoped).
- **Notebooks are a hardcoded two-element list** inside `NotebookGrid`. Creating a notebook does not create anything — it navigates straight to a blank canvas. The new notebook never appears in the library.
- No `Notebook` ↔ canvas association at all: the canvas doesn't know which notebook it belongs to, only which paper style to draw.

### Authentication
- `onClick = onLoginSuccess` — the button ignores the four fields entirely.
- No validation (no email format check, no password/confirmation comparison, no length rules).
- No error states, no loading state, no session, no logout (the Logout menu items are no-ops).
- Google and Apple buttons: `onClick = {}`.
- No sign-in vs. sign-up mode distinction.

### Home screen features
Every one of these is a no-op: search, all three filter tabs, all three view-mode toggles, the sort dropdown, the Import button, Upgrade, and every row in all three dropdown menus (delete, edit, share, pin, lock, Profile, Preferences, Help, Logout, Account, Notifications, Upgrade to Plus).

### Import PDF
Selected in the modal, dispatched to `{ /* Import handled separately */ }`. No PDF rendering dependency, no file picker, no storage permission.

### Canvas features
A tool rail exists (Session 4). Still missing: zoom/pan, pages, selection, shapes, text, images, export, page thumbnails, palm rejection, pressure, smoothing.

### AI / research panel
Only the `// Right panel (AI / research later)` comment in dead code.

### Phone / Compact support
`AuthFallbackLayout` shows "Vahi works best on a tablet" (rewritten in Session 4) and takes no callback. **Phone users cannot get past the auth screen** — intentional per [D2](22-decisions.md#d2--tablet-only-for-v1). Home and canvas have no Compact layouts either.

---

## 🐛 Known bugs and correctness issues

Ordered by impact.

### 1. 🟡 PARTIALLY FIXED (Session 6) — Rotation still destroys ink

**Original bug:** no `configChanges`, no `rememberSaveable`, no ViewModel, no navigation library. Rotating the device recreated the Activity, reset `destination` to `SPLASH`, and dropped every stroke.

**What Session 6's navigation rewrite fixed:** `AppRoot` no longer holds `destination` as a bare `remember`ed variable — it's `navigation-compose`'s `NavController`, whose `SavedStateHandle` survives process death. **The app should no longer bounce back to Splash on rotation.** Not yet verified on a device (`VAHI-008`).

**What is still broken:** `CanvasState` is still created with `remember { CanvasState() }` inside `CanvasScreen` (`CanvasScaffold.kt`), not hoisted into a ViewModel. **Rotating the device on the canvas still drops every stroke** — the navigation *position* now survives, but the *canvas content* does not. This was always the harder half of the bug, and it needs the coordinate model (`VAHI-110`) before persistence can fix it properly, or a `SavedStateHandle`-backed ViewModel as an interim step. **Severity: still high** — the app remains unusable as a note-taking tool until this half lands too.

### 2. Grid canvas cannot scroll
`GridCanvasContent` applies `verticalScroll` + `horizontalScroll` to a 4000 dp surface, but `DrawingCanvas` sits above it at `zIndex(50f)` and consumes pointer events while pressed. The scroll modifiers never fire, so 96 % of the grid is unreachable. **Severity: high.** Fix requires an explicit pan/zoom gesture mode (a hand tool, two-finger pan, or a transform matrix shared between the paper and ink layers).

### 3. ✅ FIXED (Session 4) — Dropdown anchoring mixed pixels and dp
```kotlin
// settingsButtonPosition comes from coordinates.positionInRoot() — PIXELS
.padding(top = (settingsButtonPosition.y + 48).dp)   // used as DP
```
Same pattern affected `profileButtonPosition` and `cardMenuAnchor.offset.y`. The offset scaled with screen density, so menu placement drifted across devices.

**Fix applied:** `HomeScreen.kt` now captures `LocalDensity.current` once and converts each offset properly — `with(density) { anchor.offset.y.toDp() }` — before adding the fixed dp padding. Verified by compilation; **not yet visually verified on-device.**

**Still not fixed, and out of scope for this pass:** the X coordinate is still discarded and every overlay is still `Alignment.TopEnd`-aligned, so a card menu still opens at the right screen edge regardless of which card it belongs to. The proper long-term fix remains `Popup` with a `PopupPositionProvider`, or `DropdownMenu` — that's a larger, more structural change than the density-unit bug this pass corrected.

### 4. Eraser deletes by bounding box
`strokeIntersectsRect` tests `stroke.path.getBounds().overlaps(rect)`. A long diagonal stroke has a bounding box spanning a large region, so touching anywhere inside that box erases the whole stroke — potentially far from any visible ink. **Severity: medium.** Fix: walk the path with `PathMeasure` and test point-to-eraser distance.

### 5. ✅ FIXED (Session 6) — Undo and erase shared one stack

Erased strokes were pushed onto `_undoneStrokes` — the same list used for stroke-add redo — so `redo()` would un-erase a stroke instead of re-adding whatever had actually just been undone. `finishStroke()` cleared that list, so drawing after erasing silently made the erase permanent with no way back. Erasing N strokes in one drag also pushed N entries (one per pointer sample), not one.

**Fix applied (`VAHI-007`/`VAHI-120`):** `CanvasState` now has a private `StrokeCommand` sealed type (`Add`, `Erase`) and two stacks — `undoStack`/`redoStack` — replacing the single stroke-based `_undoneStrokes`. `undo()`/`redo()` interpret the command instead of blindly moving strokes between two lists, so a redo can never resurrect an erased stroke by accident. A new `beginErase()`/`finishErase()` gesture lifecycle (mirroring the pen's `startStroke`/`continueStroke`/`finishStroke`) batches everything removed during one erase drag — potentially many `eraseAtPosition` calls, one per pointer sample — into a **single** undo step; `DrawingCanvas.kt`'s pointer loop was updated to call them. This is a scoped fix for the *current* stroke model, not the full command model in [`16-canvas-spec.md`](16-canvas-spec.md) §9 (which assumes ink-engine types that don't exist until [D6](22-decisions.md#d6--ink-architecture-adopt-androidxink) is settled) — `Clear` deliberately stays on its own separate undo mechanism from `VAHI-003`, not folded into this stack; see the KDoc on `lastClearedSnapshot` for why unifying them would risk a stale snackbar undoing the wrong action.

11 new unit tests in `CanvasStateUndoRedoTest.kt` (Robolectric), covering: redo never un-erasing, undo/redo of an erase, one-drag-one-undo-step, an empty erase drag not consuming an undo slot, and the redo-stack being correctly invalidated by new ink after an undo. Verified: `:app:testDebugUnitTest` and `:app:assembleDebug` both `BUILD SUCCESSFUL`.

### 6. ✅ FIXED (Session 4) — Splash could hang forever
`onTimeout()` only fired on `Player.STATE_ENDED`. There was no `onPlayerError` handler and no timeout fallback, so a media failure left the user permanently on the splash screen.

**Fix applied:** `SplashScreen.kt` now has an `onPlayerError` listener and a 6-second `LaunchedEffect` fallback timer, both guarded by a `hasNavigated` flag so `onTimeout()` fires exactly once regardless of which path triggers it.

### 7. 🟡 PARTIALLY FIXED (Session 4) — Auth was a dead end on phones
Covered above. Decision D2 settled this as tablet-only for v1, so the "dead end" is intentional — but it looked like a bug (`Text("Auth (Mobile)")`, a debug string).

**Fix applied:** `AuthFallbackLayout` now shows an honest "Vahi works best on a tablet" message instead of the debug string. **It still provides no functional path forward on phones**, by design (D2) — building real Compact layouts is deferred until there's a reason.

### 8. ✅ FIXED (Session 6) — Blank screen if navigation state desynchronised
```kotlin
AppDestination.NOTEBOOK_CANVAS -> selectedNotebookType?.let { ... }
```
If `destination` was `NOTEBOOK_CANVAS` while `selectedNotebookType` was `null`, nothing rendered and the only escape was system back. Was unreachable through the UI at the time, but was a real class of bug.

**Fix applied:** `AppRoot.kt` rewritten onto `navigation-compose` with type-safe routes ([`02-architecture.md`](02-architecture.md) §Navigation, `VAHI-201`). The `Editor` route is `@Serializable data class Editor(val canvasType: CanvasType)` — the argument travels *with* the route on the back stack, so there is no separate side-channel variable that could desynchronise, and no `null` state for this bug to occupy. **Fixed by construction, not by validation** — the failure mode doesn't have anywhere to happen.

### 9. ✅ FIXED (Session 4) — Edge-to-edge without insets
`enableEdgeToEdge()` is called but no composable consumed window insets. Content could render under the status bar and gesture navigation bar — most visibly the Blank/Ruled back arrow at `BottomStart`.

**Fix applied:** `HomeScreen.kt`'s main content column and `CanvasScaffold.kt`'s back arrow and new toolbar all now apply `Modifier.windowInsetsPadding(WindowInsets.safeDrawing)`. **Not yet visually verified on-device** — this is a compile-time-safe change but insets behaviour can only be fully confirmed by running the app.

### 10. Grid rendering cost
A 4000 dp square canvas at 24 px dot spacing means tens of thousands of `drawCircle` calls per frame, with no culling to the visible viewport. Combined with re-rendering every stroke each frame, this will drop frames on a busy page. **Severity: low today** (few strokes), **high later**.

### 11. Per-motion-event allocation
`continueStroke` allocates a new `Stroke` on every pointer sample purely to trigger recomposition. At stylus sampling rates this is measurable GC pressure. **Severity: low.**

### 12. ✅ FIXED (Session 4) — Dynamic colour overrode the brand palette
`VahiTheme(dynamicColor = true)` meant Android 12+ devices derived `MaterialTheme.colorScheme` from wallpaper, and the hand-authored Vahi schemes never applied.

**Fix applied:** `Theme.kt` now defaults `dynamicColor = false` (decision [D4](22-decisions.md#d4--brand-palette-not-material-you)). *Verified in Session 5 by reading the source.*

---

## 🐛 Found in the Session 5 re-audit (2026-09-08)

Four of these (#13, #15, #16, #18) are in `DrawingToolbar.kt` — code Session 4 shipped without on-device verification. That is not a criticism of the work; it is exactly what "compiles but was never run" produces, and it is why [`19-quality-bar.md`](19-quality-bar.md) §5 makes "run it on a real device" part of the definition of done.

### 13. ✅ FIXED (Session 6) — Clear canvas was unrecoverable, and it was one tap away

```kotlin
fun clearAll() {
    _strokes.clear()
    _undoneStrokes.clear()     // ← the redo stack is destroyed too
    currentStroke = null
}
```

`DrawingToolbar` wired this to a 48 dp delete button sitting **directly below Undo and Redo**. One mis-tap destroyed every stroke on the page with **no way back** — undo could not recover it because the undo stack was cleared in the same call.

**Fix applied:** `CanvasState.clearAll()` now snapshots the page before clearing; a new `undoClear()` restores it. The snapshot is invalidated the moment any other mutating action happens (a new stroke, an erase, a second clear) so restoring stale ink can never silently discard newer work. `CanvasScaffold.kt` wires this to a Material3 `Snackbar` with an "Undo" action, shown whenever Clear is tapped. Covered by 6 unit tests in `CanvasStateTest.kt` (Robolectric — see the note in that file on why a plain JVM test can't construct a `Stroke`). Verified: `:app:testDebugUnitTest` and `:app:assembleDebug` both `BUILD SUCCESSFUL`.

**Still open:** this is a minimal, targeted fix (one level of undo, scoped to Clear only) — not the full command-history model. The proper long-term fix remains `ClearPage` as a first-class entry in a real undo/redo command stack ([`16-canvas-spec.md`](16-canvas-spec.md) §9), tracked as `VAHI-120`.

### 14. 🔴 A resting palm draws, and the stylus does not

```kotlin
val change = event.changes.firstOrNull() ?: continue
```

`DrawingCanvas` takes the **first pointer in the list** and ignores every other one. On a real tablet:

- **The palm usually lands before the stylus tip.** When it does, the palm becomes pointer 1 → **you draw with your palm and the stylus does nothing.**
- When the primary pointer lifts, the next event's first change is a *different* already-pressed pointer. Since `finishStroke()` nulled `currentStroke`, a **new stroke starts under the palm.**
- `change.type == PointerType.Stylus` is available and completely unused, so stylus-vs-finger discrimination is not merely imperfect — it is absent.

**Severity: critical.** This is not an edge case; it is what happens on essentially every stroke of real handwriting. `05-drawing-engine.md` noted "no palm rejection" as a missing feature; the re-audit's finding is stronger — **the current pointer handling actively selects the wrong pointer.**

**Fix:** the pointer arbitration state machine and tiered palm rejection in [`16-canvas-spec.md`](16-canvas-spec.md) §4. Tracked as `VAHI-112` / `VAHI-113`.

### 15. ✅ FIXED (Session 6) — Toolbar touch targets were 40 dp, not 48 dp

```kotlin
Modifier.size(48.dp).padding(4.dp).clip(CircleShape).background(…).clickable(…)
```

The `padding` was applied **inside** the 48 dp box and **before** `clickable`, so the actual touch target was 40 dp — below the 48 dp accessibility minimum. Affected `ToolbarGlyphToggle` (the pen/eraser toggle). Same class of failure as the still-open 28 dp card overflow button and 36 dp view toggles noted in [`12-ux-plan.md`](12-ux-plan.md) Part 7.

**Fix applied:** removed the inner `.padding(4.dp)` so `.selectable()` is applied directly to the full 48 dp box. Verified by reading the modifier chain: every control in `DrawingToolbar.kt` now goes `.size(48.dp)` → `.selectable()`/`.clickable()` with nothing in between. **Not yet verified with a real finger on a real touchscreen** — that requires a device.

### 16. ✅ FIXED (Session 6) — The toolbar had no content descriptions and used hardcoded colours

Only the clear button had a `contentDescription`. Pen, eraser, undo, redo, every colour swatch and every width preset were unlabelled `Text` glyphs inside clickable `Box`es — a screen reader would announce `✒`, `⌫`, `↶`, `↷` by unicode name, or nothing at all.

Every colour in the file was an inline `Color(0xFF…)` literal, including a hardcoded `Color.White` background — invisible on dark paper ([V1](22-decisions.md#v1--app-theme-and-paper-theme-are-separate-settings)) and wrong in dark mode.

**Fix applied:**
- Every control now carries a `contentDescription` via `Modifier.semantics { contentDescription = … }`, and toggle-style controls (pen/eraser, colour, width) use `Modifier.selectable(selected = …, role = Role.RadioButton)` rather than a bare `clickable`, so TalkBack announces selected state correctly rather than just a label.
- All UI-chrome colours now reference `MaterialTheme.colorScheme.*` (surface, primary, onSurface, onSurfaceVariant, outlineVariant), which already responds to light/dark via `VahiTheme` — not a full semantic-token migration ([`13-visual-design-system.md`](13-visual-design-system.md), still `VAHI-203`), but no longer hardcoded, and no longer invisible on a dark theme.
- The 5-swatch ink palette (the pen colours themselves, not UI chrome) moved to five named constants in `Color.kt` (`VahiInkBlack/Purple/Blue/Red/Green`) — these stay fixed across themes deliberately, since they're user-selected pigment content, not adaptive chrome.

`grep -c '0x[0-9A-Fa-f]\{6,8\}' DrawingToolbar.kt` → 0.

### 18. ✅ FIXED (Session 6) — The width preview rendered a pixel value as dp

```kotlin
Modifier.size(width = 22.dp, height = (width * 0.9f).dp)   // width is a stroke width in px
```

`strokeWidth` values (`2f`, `4f`, `8f`) are canvas **pixels**, reinterpreted as **dp** for the preview box height via an arbitrary `*0.9f` fudge factor. The three presets therefore did not visually match the strokes they produced, and the mismatch scaled with device density.

**Fix applied:** a `WidthPreset` type now pairs each stroke's actual canvas width (px, used for drawing) with an independently-chosen `previewHeight: Dp` (used only for the swatch), so the two units can never be conflated again. **Still not the full fix** — [`15-ux-blueprint.md`](15-ux-blueprint.md) §5.4 calls for showing widths as actual rendered strokes, which is the fuller pen-slot rework, `VAHI-210`.

### 17. Leaving the editor discards an in-progress stroke

`CanvasScreen`'s `BackHandler { onBack() }` fires immediately. If the user is mid-stroke — pen still down — `finishStroke()` is never called and the stroke is silently dropped. Same on navigation away by any other route.

**Severity: medium.** Small in isolation, but it is the class of bug that erodes trust in a notes app. **Fix:** commit the in-progress stroke, flush, then navigate ([`15-ux-blueprint.md`](15-ux-blueprint.md) §3, back-behaviour contract).

### 18. The width preview renders a pixel value as dp

```kotlin
Modifier.size(width = 22.dp, height = (width * 0.9f).dp)   // width is a stroke width in px
```

`strokeWidth` values (`2f`, `4f`, `8f`) are canvas **pixels**; the preview renders them as **dp**. The three presets therefore do not visually match the strokes they produce, and the mismatch scales with device density.

**Severity: low.** **Fix:** covered by the pen-slot rework (`VAHI-210`), which shows widths as actual rendered strokes rather than sized boxes.

### 19. ✅ FIXED (Session 6) — Android Auto Backup was silently enabled with template rules

`AndroidManifest.xml` declares `allowBackup="true"` and pointed at `backup_rules.xml` / `data_extraction_rules.xml` — both of which were **untouched templates with every rule commented out**.

Was harmless (there was no data). The moment Room landed it would have silently made the entire notebook database Auto-Backup eligible, with a 25 MB quota that fails silently when exceeded and a restore path that can overwrite newer notes with older ones with no conflict UI.

**Fix applied:** both files now explicitly exclude `database/vahi.db` and `file/notebooks/` — `backup_rules.xml` for API 26-30, `data_extraction_rules.xml`'s `<cloud-backup>` and `<device-transfer>` for API 31+ — per [D13](22-decisions.md#d13--android-auto-backup-is-disabled-for-notebook-data). Excluding paths that don't exist yet is harmless and means nobody has to remember this later, under schedule pressure, once there's real user data at stake.

### 20. The eraser skips strokes during a fast drag

`eraseAtPosition` is called once per pointer sample, testing a square of side `2 × hitRadius` around that single point. Between two samples of a fast drag, the gap can exceed the hit area entirely, so strokes crossing the middle survive.

**Severity: low-medium.** Compounds with bug #4 (bounding-box hit testing) to make the eraser feel arbitrary. **Fix:** interpolate a swept region between consecutive samples — `VAHI-121`.

---

## 🧹 Code quality observations

Not bugs, but things a reviewer would flag.

| Observation | Detail |
|---|---|
| **`HomeScreen.kt` is 990 lines** | It holds 15 composables plus two data classes. The overlays (`CardContextMenu`, `SettingsOverlay`, `ProfileOverlay`, `CreateNotebookOverlay`) are self-contained and belong in their own files or a `components/` subpackage. |
| **Three near-identical menu composables** | `CardContextMenu`, `SettingsOverlay`, and `ProfileOverlay` differ only in width and item list. One parameterised `VahiDropdownMenu(width, items, onItemClick)` would replace all three. |
| **Hardcoded colours in most screens** | Directly contradicts `Color.kt`'s own KDoc: *"No hardcoded colors should be used in screens."* Dozens of inline `Color(0xFF…)` literals remain across `HomeScreen.kt`, `AuthScreen.kt`, `CanvasScaffold.kt`'s `BackArrowButton`, and the paper-style screens. **Fixed so far:** the two WCAG-AA contrast failures (Session 4, `VahiTextPlaceholder`) and all of `DrawingToolbar.kt` (Session 6, part of `VAHI-003`). A general migration to semantic tokens is intentionally deferred to ride along with the ViewModel refactor per `13-visual-design-system.md` §9. |
| **Typography scale mostly bypassed** | Screens pass raw `fontSize = 14.sp` instead of `style = MaterialTheme.typography.*`. Only `AuthScreen` uses named styles. |
| ~~**Duplicate enums**~~ | ✅ **Fixed (Session 6).** `NotebookType` and `CanvasType` were identical, mapped 1:1. `NotebookType` and its adapter file (`NotebookCanvasScreen.kt`) are deleted; `CanvasType` alone survives, now `@Serializable` for use as a navigation route argument. |
| **String-literal dispatch** | The create-notebook modal routes on `"Blank"` / `"Ruled"` / `"Grid"` / `"Import PDF"` string comparisons. |
| **Emoji and text glyphs as icons** | 🐦 🔍 ⊞ ≡ ⋮⋮ 🧮 💻 📓 □ ↓ ← G. Render inconsistently across devices and fonts, and are inaccessible to screen readers. |
| **No content descriptions** | Almost every clickable element passes `contentDescription = null` or is a bare `Text` in a `clickable` `Box`. Accessibility is effectively zero. |
| **No string extraction** | All UI copy is hardcoded Kotlin. No localisation is possible. |
| **No `@Preview` functions** | Despite `ui-tooling-preview` being a declared dependency, not one preview exists. |
| **Dead code left in place** | ✅ Deleted in Session 4: `NotebookScreen.kt`, `ic_launcher_foreground.xml`, `res/values/colors.xml`, four unused colour constants (`VahiCardMauve`, `VahiGoogleButtonBg`, `VahiAccentBlue`, `VahiAccentTeal`). **Session 6 also deleted:** `AppDestination.kt` (replaced by typed nav routes) and `NotebookCanvasScreen.kt` (its `NotebookType` enum was an identical duplicate of `CanvasType`, mapped 1:1 purely to feed the old hand-rolled navigation layer). |
| **Deprecated Gradle DSL** | `kotlinOptions { jvmTarget = "11" }` should be `compilerOptions { jvmTarget = JvmTarget.JVM_11 }`. |
| **Stale dependency versions** | Compose BOM 2024.09.00 and AndroidX libs from 2023 against `compileSdk 36`. |
| **Release builds unminified** | `isMinifyEnabled = false`, no resource shrinking, no signing config. `proguard-rules.pro` is the untouched template. See [`20-release-plan.md`](20-release-plan.md). |
| **Single commit, large uncommitted diff** | ~660 insertions / 288 deletions across 25 files sitting unstaged, plus everything Session 4 and 5 added. **This is the highest-probability way to lose the project right now** — one mistaken `git checkout` costs all of it. `VAHI-001`. |
| **Media3 is the largest dependency, and it plays one logo** ✚ | `media3-exoplayer` + `media3-ui` plus an ~800 KB `splash.mp4`, on the critical path to first frame, for a brand animation shown on *every* launch. In a price-sensitive market where install size is a conversion factor, this is a poor trade. [D11](22-decisions.md#d11--drop-the-video-splash-and-the-media3-dependency). |
| **`Theme.Vahi` inherits a light-only platform theme** ✚ | `android:Theme.Material.Light.NoActionBar`. Dark-mode users get a white flash before Compose draws, and there is no Android 12 splash-screen theme configured. Cheap to fix alongside [D11](22-decisions.md#d11--drop-the-video-splash-and-the-media3-dependency). |
| **No `configChanges`, and that is correct** ✚ | Worth stating so nobody "fixes" rotation the wrong way: adding `android:configChanges` would hide the symptom while leaving the app broken after a low-memory process kill — the case that actually loses a student's notes. The real fix is ViewModels + `SavedStateHandle` (`VAHI-201`, `VAHI-213`). |

✚ = added in the Session 5 re-audit.

---

## What a reviewer should check first

If you're picking this project up cold, verify these yourself before planning anything. **The first two need a real tablet** — an emulator cannot show you them.

1. **Rest your palm on the screen and write with a stylus.** The palm draws; the stylus does not. (Bug #14 — the most important thing in this document, and still unfixed.)
2. **Start a stroke and press back without lifting.** The stroke is discarded. (Bug #17, still unfixed.)
3. Draw on a canvas, press back, re-enter — the ink is gone. (Bug: no persistence.)
4. Rotate the device **on the canvas screen** — the ink is gone, even though (as of Session 6) you should land back where you were rather than on Splash. (Bug #1, half-fixed — see its entry above.)
5. Try to scroll the Grid canvas. You can't. (Bug #2.)
6. ~~Tap the delete icon on the tool rail — everything is gone and undo can't bring it back.~~ **Fixed in Session 6** (bug #13) — Clear now shows an "Undo" snackbar. Worth re-checking that the snackbar actually restores the page correctly.
7. ~~Turn on TalkBack and try to use the tool rail — almost nothing is announced.~~ **Fixed in Session 6** (bug #16) — every control now has a `contentDescription`. Worth confirming TalkBack actually reads them as expected.

> **Nothing in this list has been verified on a real device by any session so far** — the working environment has had no GUI, only a JVM toolchain. Bugs #13/#15/#16/#18 were fixed and confirmed via unit tests (Robolectric) and a full `assembleDebug`/`testDebugUnitTest`, which verifies the code compiles and the logic is correct — it does **not** verify layout, touch response, or screen-reader behaviour on real hardware. Items 1–2 are still read out of the source, unreproduced. Getting a real tablet is `VAHI-008`.
