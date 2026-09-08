# 10 — Roadmap

> **Superseded by [`11-forward-plan.md`](11-forward-plan.md) for sequencing.** This document is a *menu* of the work the codebase implies — useful as a checklist of everything that could be done. Doc 11 is the reasoned plan: which of these actually matter, in what order, and which are wasted work if done too early. **Where the two disagree on ordering, follow doc 11.**

This is not a plan the project has committed to — it's the work the codebase *implies*, ordered so that each step unblocks the next. Nothing here has been agreed; treat it as a menu.

---

## Phase 0 — Stabilise what exists (days)

Small, self-contained fixes with no architectural consequences. Do these first because they make everything after them easier to evaluate.

| # | Task | Why |
|---|---|---|
| 0.1 | **Build a canvas toolbar** | Activates the eraser, undo, redo, clear, colour and width — all already implemented. Highest visible value per line of code in the project. |
| 0.2 | **Commit the working tree** | ~660 insertions sit unstaged. Split into logical commits (drawing engine / icon change / screen rewrites) before adding more. |
| 0.3 | **Apply window insets** | `enableEdgeToEdge()` is on but nothing consumes insets. Add `Modifier.safeDrawingPadding()` where appropriate. |
| 0.4 | **Add a splash timeout + error listener** | Removes the "stuck forever" failure mode. |
| 0.5 | **Fix dropdown anchoring** | Replace the px-as-dp padding with `Popup` + a `PopupPositionProvider`, or `DropdownMenu`. |
| 0.6 | **Delete dead code** | `NotebookScreen.kt` (after extracting its layout intent into a design note), `ic_launcher_foreground.xml`, `res/values/colors.xml`, unused colour constants. |
| 0.7 | **Extract strings to `strings.xml`** | Prerequisite for localisation and for meaningful content descriptions. |
| 0.8 | **Add content descriptions** | Accessibility is currently zero. |
| 0.9 | **Replace emoji icons with vector assets** | `Icons.*` or drawables. Emoji render inconsistently and are unreadable to screen readers. |

---

## Phase 1 — Make it a real app: state survival (1–2 weeks)

Nothing else matters until ink survives. This is the load-bearing phase.

| # | Task | Notes |
|---|---|---|
| 1.1 | **Introduce ViewModels** | `BaseViewModel<State>` is already written. Add `lifecycle-viewmodel-compose` and `kotlinx-coroutines-android` as *explicit* dependencies first — they're currently only transitive. |
| 1.2 | **Hoist `CanvasState` into a `CanvasViewModel`** | Survives rotation immediately. |
| 1.3 | **Adopt `androidx.navigation:navigation-compose`** | Gives you a back stack, typed arguments (replacing the `selectedNotebookType` side channel), and saved-state restoration. Removes bug #8 by construction. |
| 1.4 | **Design a serialisable stroke model** | The current `Stroke` holds only a `Path`, which can't be round-tripped. Add a point list (`List<Offset>` or a packed `FloatArray`) and derive the `Path` from it. |
| 1.5 | **Add Room + a repository** | Fill in `data/datasource`, `data/repository`, `domain/models`, `domain/usecases` — the packages already exist as markers. Entities: `Notebook`, `Page`, `Stroke`. |
| 1.6 | **Wire notebook CRUD** | Creating a notebook in the modal should actually persist a `Notebook` and open *that* notebook. Replace the hardcoded two-item list with a query. |
| 1.7 | **Implement card menu actions** | delete, edit (rename), pin — these become trivial once there's a repository. |
| 1.8 | **Add DI** | Hilt is the conventional choice. Worth doing at the same time as 1.5 rather than retrofitting. |

**Exit criterion for Phase 1:** create a notebook, draw in it, close the app, reopen it, and see your ink.

---

## Phase 2 — Make the canvas good (2–4 weeks)

| # | Task | Notes |
|---|---|---|
| 2.1 | **Pan and zoom** | A shared transform matrix applied to both the paper layer and the ink layer, with a mode toggle or two-finger gesture. Also fixes the unreachable-grid bug. |
| 2.2 | **Command-based undo stack** | `AddStroke` / `EraseStrokes(list)` actions instead of a shared stroke list. Fixes the erase/redo conflation. |
| 2.3 | **Geometry-accurate erasing** | `PathMeasure` sampling and point-distance testing instead of bounding-box overlap. |
| 2.4 | **Render performance** | Cache settled strokes into a `GraphicsLayer` / bitmap; live-render only the active stroke. Cull grid dots to the visible viewport. |
| 2.5 | **Stylus fidelity** | Consume `change.historical` samples, modulate width by `change.pressure`, add Bézier smoothing between samples. |
| 2.6 | **Palm rejection** | Filter on `PointerType.Stylus` when a stylus is present. |
| 2.7 | **Pages** | Page boundaries, page navigation, add/delete page, thumbnails. |
| 2.8 | **More tools** | Highlighter (translucent, width-modulated), lasso select, shape recognition, text boxes. |

---

## Phase 3 — Accounts and sync (timeline depends entirely on backend choice)

| # | Task |
|---|---|
| 3.1 | Pick a backend (Firebase is the fastest path given Google + Apple sign-in are already drawn) |
| 3.2 | Real authentication: validation, error states, loading states, sessions, logout |
| 3.3 | Google Sign-In and Sign in with Apple |
| 3.4 | Cloud sync with conflict resolution |
| 3.5 | Multi-device continuity |

> Requires adding the `INTERNET` permission — the manifest currently declares none.

---

## Phase 4 — The features the UI already promises

Each of these has a button or tab in the app today that does nothing.

| # | Feature | Existing UI hook |
|---|---|---|
| 4.1 | **Import PDF & annotate** | "Import PDF" modal option, "Import" toolbar button, "Imported Books" filter tab |
| 4.2 | **Search notebooks** | The decorative 280 dp search field |
| 4.3 | **Pinned notes** | The "Pinned notes" filter tab, the card menu "pin" item |
| 4.4 | **Locked notebooks** | The card menu "lock" item |
| 4.5 | **Sharing / export** | The card menu "share" item |
| 4.6 | **View modes & sorting** | The ✓ / ⊞ / ≡ toggles and the "Most recent" dropdown |
| 4.7 | **Subscription tier** | "Upgrade" pill, "Upgrade to Plus" profile item |
| 4.8 | **Settings, Profile, Account, Notifications, Help** | All five dropdown menu rows |

---

## Phase 5 — The stated long-term bet

**The AI / research side panel.** The only trace of it is a comment in `NotebookScreen.kt`:

```kotlin
// Right panel (AI / research later)
Surface(modifier = Modifier.width(320.dp).fillMaxHeight()) { }
```

Plausible directions given a handwriting-first app: handwriting recognition (ink → text), search across handwritten notes, summarisation, question-answering over notebook contents, or research lookup alongside the page. **This is undefined — it needs a product decision before any engineering.**

---

## Cross-cutting, ongoing

| Task | Notes |
|---|---|
| **Phone (Compact) layouts** | Decide first whether phones are in scope at all. If yes, `AuthFallbackLayout` is the blocker, then Home's fixed 3-column grid and 260 dp cards, then the 680 dp modal. |
| **Tests** | Start with `CanvasState` — it's a plain class with real edge cases (undo/redo/erase interaction) and no Android dependencies. Then Compose UI tests for navigation; the tooling is already on the classpath. |
| **Consolidate hardcoded colours into `Color.kt`** | The file's own KDoc already mandates this. |
| **Adopt the typography scale** | Replace raw `fontSize` with `MaterialTheme.typography.*`. |
| **Split `HomeScreen.kt`** | 982 lines, 15 composables. Extract the overlays; unify the three near-identical dropdown menus into one parameterised composable. |
| **Add `@Preview` functions** | `ui-tooling-preview` is already a dependency and there isn't a single preview. |
| **Upgrade dependencies** | Compose BOM, `activity-compose`, `lifecycle-*`, `core-ktx` are all well behind `compileSdk 36`. |
| **Release build hardening** | `isMinifyEnabled = true`, `isShrinkResources = true`, real ProGuard rules, a signing config. |
| **Decide on `dynamicColor`** | Brand consistency vs. Material You. Currently the brand palette is silently unused on Android 12+. |

---

## Suggested immediate next step

**Build the canvas toolbar (0.1), then move `CanvasState` into a ViewModel (1.2).**

Together those two changes turn the canvas from a demo into something usable: you get pen/eraser switching, colour, width, undo and redo, and your ink stops disappearing when the screen rotates. Everything else in Phase 1 builds on that foundation.
