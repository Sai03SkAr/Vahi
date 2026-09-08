# 21 — Backlog

> **This is the execution spine.** Every piece of work in the plan, numbered, sized, dependency-ordered, with acceptance criteria.
>
> **During implementation, this file is updated in the same session as the code.** Status here plus the newest entry in [`PROJECT-LOG.md`](PROJECT-LOG.md) is how a cold session knows exactly where things stand.

**Status:** ⬜ Not started · 🟨 In progress · ✅ Done · ⏸️ Blocked · ❌ Dropped (with reason)
**Size:** engineering-days ([D7](22-decisions.md#d7--schedule-expressed-in-engineering-days-not-calendar-dates)). XS ≤ 0.5 · S ≤ 1 · M ≤ 3 · L ≤ 5 · XL > 5 (split it)

---

## Summary

| Epic | Theme | Items | Size | Status |
|---|---|---|---|---|
| **E0** | Stabilise & de-risk | 9 | ~7 ed | ⬜ |
| **E1** | Ink foundation (F1, F2, F5) | 10 | ~17 ed | ⬜ |
| **E2** | App foundation + UI rebuild (F3) | 13 | ~24 ed | ⬜ |
| **E3** | Persistence (F4) | 9 | ~16 ed | ⬜ |
| **E4** | v1.0 completion & release | 14 | ~24 ed | ⬜ |
| **E5** | v1.1 — PDF & organisation | 7 | ~19 ed | ⬜ |
| **E6** | v1.2 — Recognition & search | 6 | ~14 ed | ⬜ |
| **E7** | v2.0 — Accounts & sync | 7 | ~33 ed | ⬜ |

**To v1.0 release: ≈ 88 engineering-days.** Multiply by your own throughput and a focus factor of 0.6–0.7 for a calendar estimate.

### The critical path

```
VAHI-001 commit tree
   └─▶ VAHI-002 upgrade deps ──▶ VAHI-101 ink benchmark ──▶ VAHI-102 InkEngine
                    │                                              │
                    └──▶ VAHI-201 navigation ──┐                   │
                                               ▼                   ▼
                                      VAHI-110 coordinate model ───┤
                                               │                   │
                                               ▼                   ▼
                                      VAHI-301 Room schema ◀───────┘
                                               │
                                               ▼
                                      VAHI-305 ★ MILESTONE: nothing is lost
                                               │
                                               ▼
                                      E4 ──▶ v1.0
```

**The hard ordering rule, restated:** `VAHI-110` (coordinate model) **must** complete before `VAHI-301` (persistence). Strokes stored in screen pixels cannot be migrated — you will not know what device or zoom produced them. Do not relax this under schedule pressure.

---

## E0 — Stabilise and de-risk

*Start here. Everything survives every downstream decision, so there is zero rework risk.*

### VAHI-001 · Commit the working tree ✅ Done (Session 6) · XS
**Why:** the repo had **one commit** with hundreds of uncommitted lines on top — the entire drawing engine, the toolbar, all Session 4 fixes and this documentation set. A single mistaken `git checkout` cost all of it.
**Done:** committed in six logical chunks — Media3 dependency · launcher icons · ink engine + canvas host · Session 4 UX/bug fixes · IDE metadata · full documentation set. Each commit builds; history is readable.
**Deps:** none. **Blocked:** everything (now unblocked).

### VAHI-002 · Upgrade the dependency baseline ✅ Done (Session 6) · M
**Why:** Compose BOM `2024.09.00` and 2023-era AndroidX against `compileSdk 36`. Adding Room, Hilt, Navigation and Ink onto that baseline would produce confusing incompatibilities. Upgrade **before** adding, not after (R10).
**Done:** Kotlin 2.0.21→2.1.20, core-ktx 1.10.1→1.15.0, lifecycle 2.6.1→2.8.7, activity-compose 1.8.0→1.9.3, Compose BOM 2024.09.00→2025.01.01. AGP stays 8.13.2 deliberately — AGP 9 is a separate breaking-change migration, not required to unblock Room/Hilt/Navigation, and not something to bundle into a routine catalog refresh without on-device verification available. Migrated `kotlinOptions` → `compilerOptions`. Declared `lifecycle-viewmodel-compose`, `lifecycle-runtime-compose`, `kotlinx-coroutines-android` explicitly (previously only transitive). Added `kotlinx-coroutines-test` and Robolectric for JVM-testable Compose/graphics code.
**Accept:** `assembleDebug` and `testDebugUnitTest` pass — verified. **Deps:** VAHI-001.

### VAHI-003 · Fix the tool rail's shipped defects ✅ Done (Session 6) · S
**Why:** four real problems in code that already ships to the canvas — see [`09-project-status.md`](09-project-status.md) #13, #15, #16, #18.
**Done:**
- **Clear was unrecoverable (#13, the most severe fix in this pass).** `CanvasState.clearAll()` now snapshots the page first; `undoClear()` restores it. `CanvasScaffold.kt` shows a Snackbar with an "Undo" action. The snapshot is invalidated by any further mutation so stale ink can never silently overwrite newer work. 6 unit tests (Robolectric — `Stroke.path` is `android.graphics.Path`-backed, so a plain JVM test can't construct one; this is itself a useful finding, corrected in `19-quality-bar.md`'s test-target description).
- **Touch targets were 40 dp, not 48 (#15).** Removed the inner `.padding(4.dp)` that shrank `ToolbarGlyphToggle`'s clickable region.
- **No `contentDescription`; hardcoded colours (#16).** Every control now has a `contentDescription`; toggle-style controls use `Modifier.selectable(role = Role.RadioButton)` so TalkBack announces selection state. UI-chrome colours now reference `MaterialTheme.colorScheme.*`. The 5-swatch ink palette moved to named constants in `Color.kt` (deliberately theme-invariant — it's user-selected pigment, not chrome).
- **Width preview mixed px and dp (#18).** A `WidthPreset` type now separates the actual draw width (px) from an independently-chosen preview size (dp).
**Accept:** `grep -c '0x[0-9A-Fa-f]\{6,8\}' DrawingToolbar.kt` → 0; every control `.size(48.dp)` → `.selectable`/`.clickable` with nothing between; `testDebugUnitTest` and `assembleDebug` both pass. **Not yet verified on a real touchscreen or with TalkBack** — that needs a device (`VAHI-008`).
**Deps:** VAHI-001.

### VAHI-004 · Reconcile the docs with the Figma design ⬜ S ⏸️
**Why:** [V4](22-decisions.md#v4--figma-is-the-visual-source-of-truth-the-docs-describe-behaviour). The owner has [`vahi-v1`](https://www.figma.com/design/JJAGOm1by1gHE6Nxe4yUhx/vahi-v1?node-id=0-1); it is login-gated and has not been read. Until reconciled, [`13-visual-design-system.md`](13-visual-design-system.md) may propose values that contradict the real design.
**Blocked on:** frame exports, a public view link, or a token export from the owner.
**Do:** rewrite doc 13 Parts 3–6 against the real values; annotate the doc 15 wireframes with the frames they correspond to; note anything the Figma does not cover (states, dark mode, split-screen, 200 % scale, Hindi expansion) as spec-owned.
**Accept:** no contradiction between Figma and doc 13; doc 15 wireframes cross-referenced.

### VAHI-005 · Configure backup rules explicitly ✅ Done (Session 6) · XS
**Why:** [D13](22-decisions.md#d13--android-auto-backup-is-disabled-for-notebook-data). `allowBackup="true"` with template rules where every rule was commented out — the moment Room lands, the whole database would silently become Auto-Backup eligible, with a 25 MB silent-failure quota and a restore that can overwrite newer notes with older.
**Done:** excluded `database/vahi.db` and `file/notebooks/` in both `backup_rules.xml` (API 26-30) and `data_extraction_rules.xml`'s `<cloud-backup>` and `<device-transfer>` (API 31+). Verified: `assembleDebug` passes (valid XML, resources merge cleanly).
**Accept:** rules explicit; verified with `bmgr` that notebook data is not backed up.
**Deps:** none. **Do before** VAHI-301.

### VAHI-006 · Delete the phone dead-end properly ⬜ XS
**Why:** [D10](22-decisions.md#d10--no-auth-wall-account-is-optional-and-requested-in-context) removes the auth wall entirely, which makes `AuthFallbackLayout` moot on the startup path.
**Do:** move the tablet-only message to the app shell, not to Auth. Auth becomes a Settings-reachable screen only.
**Deps:** VAHI-201.

### VAHI-007 · First unit tests: command history ⬜ S
**Why:** the best test in the project — pure logic, no Android dependencies, real edge cases, and the area currently **known** to be wrong (erase and undo share one stack; drawing after erasing silently makes the erase permanent).
**Do:** write tests that fail against today's `CanvasState`, documenting the actual bugs. They become the specification for `VAHI-120`.
**Accept:** JVM tests run in < 1 s; each documents one real defect.
**Deps:** VAHI-002.

### VAHI-008 · Set up the reference devices ⬜ S
**Why:** every performance budget in [`19-quality-bar.md`](19-quality-bar.md) is stated for the T2 reference device, and no number is meaningful without one. Emulators cannot measure input latency at all.
**Do:** acquire or borrow a T1 and a T2 tablet; record exact models and Android versions in `PROJECT-LOG.md`; establish the latency measurement method (§2.1).
**Accept:** both devices run the current build; the method is documented and repeatable.
**Blocks:** VAHI-101.

### VAHI-009 · Baseline performance measurement ⬜ S
**Why:** you cannot claim an improvement without a before.
**Do:** measure the **current** implementation on T2 — ink latency, fps while drawing on an empty page and on a synthetically dense page, cold start, time-to-ink, APK size. Record all of it in `PROJECT-LOG.md`.
**Accept:** every §2 budget has a "before" figure with its device named.
**Deps:** VAHI-008.

---

## E1 — Ink foundation (F1, F2, F5)

### VAHI-101 · ★ Ink latency benchmark — the D6 gate ⬜ M
**Why:** [D6](22-decisions.md#d6--ink-architecture-adopt-androidxink). The recommendation to adopt `androidx.ink` 1.0.0 is strong but still **Provisional**, and this is the evidence that settles it. Latency is the product.
**Do:** build a throwaway prototype using `ink-authoring-compose`'s `InProgressStrokes`. Measure against the current implementation, **same device, same stylus, side by side**, on T2. Also record: APK delta from the native libraries, behaviour with a **finger and no pressure data**, and whether authoring composes cleanly inside a transformed, scrolling container.
**Accept:** numbers in `PROJECT-LOG.md`; D6 moved to ✅ Accepted or ⛔ Superseded; if superseded, the [`16-canvas-spec.md`](16-canvas-spec.md) §8 fallback is adopted explicitly.
**Deps:** VAHI-002, VAHI-008, VAHI-009. **Blocks:** VAHI-102.

### VAHI-102 · `InkEngine` interface + implementation ⬜ L
**Why:** the abstraction that makes the D6 fallback a swap rather than a rewrite. One file imports `androidx.ink`; nothing else in the codebase knows it exists.
**Accept:** [`16-canvas-spec.md`](16-canvas-spec.md) §3.1 interface implemented; `grep -rl "androidx.ink" app/src/main` returns exactly one file; unit tests cover serialise/deserialise round-trip.
**Deps:** VAHI-101.

### VAHI-110 · ★ Coordinate model and view transform ⬜ M
**Why:** the foundation most likely to be overlooked and the most expensive to retrofit. Strokes today are stored in screen pixels of whatever device drew them.
**Do:** implement [`16-canvas-spec.md`](16-canvas-spec.md) §2 — page/document/screen spaces, `ViewTransform`, the page strip, A4 in points.
**Accept:** pure JVM tests for every transform, including round-trips at multiple densities and zoom levels; no geometry anywhere in screen pixels.
**Deps:** VAHI-002. **Blocks:** VAHI-301 — *this is the hard ordering rule.*

### VAHI-111 · Pan and zoom ⬜ M
**Do:** two-finger pan + pinch about the centroid; clamped scale; double-tap fit-to-width. Retires the 4000 dp grid surface ([D1](22-decisions.md#d1--paged-documents-not-infinite-canvas)) and fixes bug #2 by construction.
**Accept:** [`16-canvas-spec.md`](16-canvas-spec.md) §11 "Transform" checklist passes.
**Deps:** VAHI-110.

### VAHI-112 · Pointer arbitration state machine ⬜ M
**Why:** the highest-severity gap in the current implementation. `event.changes.firstOrNull()` means a palm landing first draws instead of the stylus.
**Do:** implement [`16-canvas-spec.md`](16-canvas-spec.md) §4.2 exactly — IDLE/DRAWING/PANNING/TRANSFORMING, the 120 ms grace window, and mandatory cancellation on every exit path. `cancelStroke()` gets its first caller.
**Accept:** §11 "Palm and pointers" checklist passes on T2 with a real palm.
**Deps:** VAHI-102, VAHI-111.

### VAHI-113 · Palm rejection, tiered ⬜ M
**Do:** all four tiers from [`16-canvas-spec.md`](16-canvas-spec.md) §4.3 — stylus-present, `TOOL_TYPE_PALM`, size/stationarity heuristics, and the user override.
**Accept:** writing a full line with a naturally resting palm leaves **zero** palm marks on T2 and T3; the finger-draw toggle works.
**Deps:** VAHI-112.

### VAHI-114 · Brushes and graceful degradation ⬜ M
**Do:** ballpoint, fountain, pencil, highlighter per [`16-canvas-spec.md`](16-canvas-spec.md) §3.2–3.3. Highlighter draws **behind** ink and must not darken on overlap.
**Accept:** pressure modulates width on T3; a finger stroke on T1 looks deliberate, not broken; highlighter passes both z-order and non-accumulation checks.
**Deps:** VAHI-102.

### VAHI-115 · Rendering pipeline and viewport culling ⬜ L
**Why:** required, not optimisation ([D3](22-decisions.md#d3--product-thesis-student-notebook-android-tablets-india)). The current renderer repaints every settled stroke every frame.
**Do:** the layered pipeline in [`16-canvas-spec.md`](16-canvas-spec.md) §6, with per-page caching, LRU eviction and bounds-based culling.
**Accept:** dense-page and 100-page-scroll budgets from [`19-quality-bar.md`](19-quality-bar.md) §2.2 met on T2.
**Deps:** VAHI-110, VAHI-102.

### VAHI-120 · Command history (F5) ⬜ M
**Why:** one user action = one undo step. Must land **before** more tools exist — every new tool multiplies the broken interactions with the current shared-stack design.
**Do:** [`16-canvas-spec.md`](16-canvas-spec.md) §9. Notebook-scoped, depth 100, `ClearPage` undoable, one eraser drag = one command.
**Accept:** the `VAHI-007` tests now pass; §11 "Editing" checklist passes.
**Deps:** VAHI-102, VAHI-007.

### VAHI-121 · Geometry-accurate erase ⬜ S
**Why:** `strokeIntersectsRect` tests the axis-aligned **bounding box**, so touching anywhere inside a long diagonal stroke's box erases the whole thing — often far from any visible ink.
**Do:** use `ink-geometry` intersection queries (or `PathMeasure` sampling in the fallback). Add an eraser-size control and interpolate between samples so a fast drag does not skip strokes.
**Accept:** the eraser removes only what it visibly touches; a fast drag misses nothing.
**Deps:** VAHI-102, VAHI-120.

---

## E2 — App foundation and UI rebuild (F3)

### VAHI-201 · Navigation + typed routes 🟨 Partially done (Session 6) · M
**Why:** highest ratio of problems-solved to effort in the plan. Kills bug #1 (rotation) and bug #8 (blank-screen desync) at once, and deletes the `selectedNotebookType` side channel.
**Done:** `navigation-compose` with type-safe `kotlinx.serialization` routes (`Splash`, `Auth`, `Library`, `Editor(canvasType)`). Deleted `AppDestination.kt` and `AppRoot`'s hand-rolled `when`. Deleted `NotebookCanvasScreen.kt` — with `CanvasType` carried directly as the `Editor` route argument, its 1:1 `NotebookType`→`CanvasType` adapter had nothing left to do (also closes the "Duplicate enums" item in `09-project-status.md`'s code-quality table). Bug #8 (blank-screen desync) is fixed **by construction** — `Editor` cannot exist on the back stack without its `CanvasType`.
**Not done — this is the "M" part, deferred deliberately rather than bundled in:** the [`15-ux-blueprint.md`](15-ux-blueprint.md) §3 route table's full shape (routes keyed by `notebookId`, not `canvasType`) needs real notebook entities, i.e. `VAHI-301`+`VAHI-304`; doing it now would mean designing routes against data that doesn't exist yet. The full back-behaviour contract (§3, e.g. "commit the in-progress stroke before navigating") also isn't wired up — `CanvasScreen`'s `BackHandler` still calls `onBack()` immediately, so bug #17 (stroke discarded on back) is unchanged.
**Accept (partial):** rotation preserves the current destination and its arguments — verified by reading the `NavHost`/`rememberNavController` wiring; **not yet verified on a device** (`VAHI-008`). `CanvasState` itself is still `remember`-scoped, so ink is still lost on rotation (bug #1 unchanged) — this item only fixed the *navigation* half of that bug, not the *canvas state* half; the second half is `VAHI-110`/`VAHI-301`.
**Deps:** VAHI-002. **Verified:** `:app:assembleDebug`, `:app:testDebugUnitTest` both `BUILD SUCCESSFUL`.

### VAHI-202 · Hilt + `VahiApplication` ⬜ S
**Deps:** VAHI-002. **Blocks:** VAHI-301.

### VAHI-203 · Design-system foundations ⬜ M
**Do:** [`13-visual-design-system.md`](13-visual-design-system.md) §3.1 two-layer tokens · spacing constants · shape standardisation · bundled Noto ([V2](22-decisions.md#v2--font-family-noto-sans--noto-sans-devanagari)) · a `ui/designsystem/` component set.
**Accept:** dark mode works on every migrated screen; zero `Color(0xFF…)` in migrated files; Devanagari renders identically on T1/T2/T3.
**Deps:** VAHI-002. **Soft-blocked by:** VAHI-004.

### VAHI-204 · Replace every emoji with real icons ⬜ S
**Why:** `🐦 🔍 ⊞ ≡ ⋮⋮ 🧮 💻 📓 □ ↓ ← G` render differently per manufacturer, cannot be tinted, and screen readers announce them as "bird" and "abacus".
**Accept:** `grep` finds no emoji in `.kt`; every icon has a `contentDescription`.
**Deps:** VAHI-203.

### VAHI-205 · Kill the auth wall and the splash video ⬜ S
**Why:** [D10](22-decisions.md#d10--no-auth-wall-account-is-optional-and-requested-in-context), [D11](22-decisions.md#d11--drop-the-video-splash-and-the-media3-dependency). Biggest single time-to-ink win, and removes the largest dependency in the project.
**Do:** system splash → library. Remove `androidx.media3` and `splash.mp4`. First-launch-only brand animation, skippable.
**Accept:** warm start → library in < 2 s; `media3` gone; APK smaller.
**Deps:** VAHI-201. ⏸️ *Confirm the video question with the owner first ([`14-product-spec.md`](14-product-spec.md) Q1).*

### VAHI-206 · `LibraryViewModel` + split `HomeScreen.kt` ⬜ L
**Why:** 990 lines, 15 composables, two data classes. **Split it while introducing its ViewModel** — doing the two separately means refactoring the same 990 lines twice.
**Do:** feature package per [`18-architecture-target.md`](18-architecture-target.md) §2; one parameterised `VahiDropdownMenu` replacing the three near-identical menus; exhaustive `phase`.
**Accept:** no file over 300 lines; every composable stateless and previewable.
**Deps:** VAHI-201, VAHI-202, VAHI-301.

### VAHI-207 · Library rewrite ⬜ L
**Do:** [`15-ux-blueprint.md`](15-ux-blueprint.md) §4 — thumbnails, "Jump back in", real search field, working filters/sort/view-mode, `+ New` out of the grid, properly anchored card menu with real actions, colour as a 3 dp accent bar, "Edited 2h ago".
**Accept:** every visible control does something; card menus anchor to their own card.
**Deps:** VAHI-206.

### VAHI-208 · Empty, loading and error states ⬜ M
**Why:** the app currently has exactly one state per screen: success. An empty grid caused by a read error is indistinguishable from "you have no notes" — terrifying in a notes app.
**Do:** every state in [`15-ux-blueprint.md`](15-ux-blueprint.md) §4.4 and §5.8, each with a recovery action.
**Accept:** every async path shows loading, empty and error correctly. ⏸️ *Illustration style is [V5](22-decisions.md#v5--empty-state-illustration-style) — recommendation is to ship type + one muted glyph, no illustration.*
**Deps:** VAHI-207.

### VAHI-209 · Snackbar undo for destructive actions ⬜ S
**Why:** rule 5 — undo, not confirm. Faster for the 99 % of intentional deletes and genuinely safer for the 1 % of accidents.
**Accept:** delete notebook / delete page / clear page all execute immediately with a 5 s undo; no confirmation dialogs anywhere.
**Deps:** VAHI-207, VAHI-301.

### VAHI-210 · Editor chrome ⬜ L
**Do:** [`15-ux-blueprint.md`](15-ux-blueprint.md) §5.2–5.4 — document bar, tool rail with pen slots and tap-again-to-configure, auto-hide, handedness mirroring, keyboard shortcuts.
**Accept:** slot selection is one tap; rail auto-hides while writing; mirroring works; every control is TalkBack-reachable.
**Deps:** VAHI-203, VAHI-112, VAHI-401.

### VAHI-211 · Create-notebook rewrite ⬜ S
**Do:** [`15-ux-blueprint.md`](15-ux-blueprint.md) §4.5 — rendered paper previews, name field, colour, typed-enum dispatch, remembered default, Enter-to-create.
**Accept:** `+ New` then Enter creates and opens a notebook in two taps; no string-literal dispatch remains.
**Deps:** VAHI-207.

### VAHI-212 · Motion ⬜ S
**Do:** [`15-ux-blueprint.md`](15-ux-blueprint.md) §7.5, including the card → editor shared-element transition.
**Accept:** transitions present; the system "remove animations" setting is honoured.
**Deps:** VAHI-207, VAHI-210.

### VAHI-213 · Multi-window and configuration resilience ⬜ M
**Do:** [`15-ux-blueprint.md`](15-ux-blueprint.md) §7.1. **Not** via `android:configChanges` — via ViewModels and `SavedStateHandle`, so process death is covered too.
**Accept:** rotate mid-stroke, split-screen, free-form resize and process death all preserve state, position and zoom.
**Deps:** VAHI-201, VAHI-301.

---

## E3 — Persistence (F4)

> ⚠️ **Nothing in this epic may start before `VAHI-110` is complete.**

### VAHI-301 · Room schema, DAOs, migrations ⬜ L
**Do:** [`17-data-spec.md`](17-data-spec.md) §2 — entities, indexed bounding boxes, string-persisted enums, `exportSchema = true` with schemas committed, `fallbackToDestructiveMigration` banned in release.
**Accept:** instrumented DAO tests; a migration test harness exists even at version 1.
**Deps:** VAHI-110, VAHI-202, VAHI-005.

### VAHI-302 · `InkFileStore` and the `.ink` format ⬜ L
**Do:** [`17-data-spec.md`](17-data-spec.md) §3 — header with `formatVersion` and `engineId`, temp-file + fsync + atomic rename, debounced writes, compaction.
**Accept:** round-trip tests; a kill -9 mid-write never produces a truncated file.
**Deps:** VAHI-301, VAHI-102.

### VAHI-303 · Repository layer ⬜ M
**Do:** the interfaces in [`17-data-spec.md`](17-data-spec.md) §9, `Result`-returning, `Flow`-observing, with viewport-aware `loadStrokes`.
**Accept:** `domain/` has zero Android imports; no Room entity appears in any composable signature.
**Deps:** VAHI-301, VAHI-302.

### VAHI-304 · Notebook and page CRUD ⬜ M
**Accept:** creating a notebook actually creates one and the library reflects it; the hardcoded two-item list is gone; add/insert/delete/reorder pages all work and are undoable.
**Deps:** VAHI-303.

### VAHI-305 · ★★ Autosave — *the milestone that matters* ⬜ M
**Why:** *draw → close the app → reopen → the ink is still there.* **Everything before this is a demo; everything after it is a product.**
**Do:** [`16-canvas-spec.md`](16-canvas-spec.md) §10 — debounced writes, `NonCancellable` `ON_PAUSE` flush, "Saved ✓" indicator, persistent non-dismissible bar on write failure. No Save button, ever.
**Accept:** force-stop mid-session → reopen → everything is there, on the right page, at the right zoom. At most one uncommitted stroke lost.
**Deps:** VAHI-303, VAHI-302.

### VAHI-306 · Page thumbnails ⬜ M
**Do:** generate on page change via WorkManager; WebP; feeds library cards and the page drawer.
**Deps:** VAHI-305, VAHI-115.

### VAHI-307 · DataStore preferences ⬜ S
**Do:** Proto DataStore for theme, paper theme, handedness, finger-draw, default paper, pen slots, language. **No `SharedPreferences`.**
**Deps:** VAHI-202.

### VAHI-308 · Orphan sweep and compaction jobs ⬜ S
**Do:** [`17-data-spec.md`](17-data-spec.md) §4 and §1 — WorkManager, idle + charging constrained.
**Deps:** VAHI-302.

### VAHI-309 · Local profile identity ⬜ S
**Why:** [`17-data-spec.md`](17-data-spec.md) §7. Getting this wrong later means "sign in and your local notes disappear" — the single worst bug this app could ship, and the default outcome if identity is bolted on afterwards.
**Accept:** every notebook carries `ownerProfileId`; a documented adoption path exists for a future sign-in.
**Deps:** VAHI-301.

---

## E4 — v1.0 completion and release

### VAHI-401 · Page model in the editor ⬜ L
**Do:** vertical continuous scroll across the page strip, page indicator, page boundaries, "keep writing past the bottom".
**Deps:** VAHI-110, VAHI-304, VAHI-115.

### VAHI-402 · Page thumbnail drawer ⬜ M
**Do:** [`15-ux-blueprint.md`](15-ux-blueprint.md) §5.6 — jump, drag-reorder, per-page actions, all undoable.
**Deps:** VAHI-401, VAHI-306.

### VAHI-403 · Paper styles on the page model ⬜ S
**Do:** blank / **horizontal-ruled** / grid / dotted, generated for the visible rect only.
**Note:** `RuledCanvas` currently draws **vertical** lines, contradicting its own description ("Lined pages for writing"). The paged model resolves this naturally.
**Deps:** VAHI-401.

### VAHI-404 · Export to PDF ⬜ L
**Do:** whole notebook or page range; ink flattened; **always light paper regardless of paper theme** ([V1](22-decisions.md#v1--app-theme-and-paper-theme-are-separate-settings)); share sheet.
**Accept:** a 20-page export opens correctly in Drive and Adobe; geometry matches the screen.
**Deps:** VAHI-401.

### VAHI-405 · Export to PNG and `.vahi` ⬜ M
**Why:** with Auto Backup off ([D13](22-decisions.md#d13--android-auto-backup-is-disabled-for-notebook-data)), export **is** the v1 backup story. It is not optional.
**Accept:** `.vahi` round-trips losslessly, including through a version bump.
**Deps:** VAHI-404.

### VAHI-406 · Settings screen ⬜ L
**Do:** [`15-ux-blueprint.md`](15-ux-blueprint.md) §6, two-pane. Handedness, paper theme, finger-draw, storage-by-notebook, about.
**Deps:** VAHI-307, VAHI-201.

### VAHI-407 · Notebook properties + export sheet ⬜ M
**Deps:** VAHI-404, VAHI-207.

### VAHI-408 · Title search ⬜ S
**Deps:** VAHI-303.

### VAHI-409 · Onboarding and first-run ⬜ M
**Do:** [`15-ux-blueprint.md`](15-ux-blueprint.md) §5.7 — max two coach marks, the handedness prompt, "your notes live on this tablet" honesty.
**Deps:** VAHI-210, VAHI-406.

### VAHI-410 · String extraction + Hindi ⬜ M
**Why:** `strings.xml` has **one** entry; all UI copy is hardcoded Kotlin. The cost grows with every screen added, so this must be a rule from now on, not a cleanup later.
**Accept:** zero hardcoded UI strings; full Hindi UI; no clipping at 200 % font scale with Hindi (20–30 % longer than English).
**Deps:** VAHI-207, VAHI-210, VAHI-406.

### VAHI-411 · Accessibility pass ⬜ M
**Accept:** every A1–A11 requirement in [`19-quality-bar.md`](19-quality-bar.md) §3.1; the TalkBack script completes end to end.
**Deps:** VAHI-410.

### VAHI-412 · Performance pass ⬜ M
**Accept:** every [`19-quality-bar.md`](19-quality-bar.md) §2 budget met on T2 and usable on T1; results recorded with device names.
**Deps:** VAHI-115, VAHI-401.

### VAHI-413 · Release hardening ⬜ M
**Do:** [`20-release-plan.md`](20-release-plan.md) §3 — minify, shrink, ProGuard rules for Room/serialization/ink/Hilt, signing config, keystore backed up twice, AAB output.
**Accept:** the **release** build passes the full manual checklist on real hardware.
**Deps:** VAHI-412.

### VAHI-414 · Store readiness ⬜ M
**Do:** [`20-release-plan.md`](20-release-plan.md) §4, §7 — crash reporting with content-logging rules, privacy policy hosted, Data Safety form matching it, listing assets in EN + HI, tablet screenshots.
**Accept:** every pre-launch blocker in §9 is closed.
**Deps:** VAHI-413.

---

## E5 — v1.1: PDF and organisation

### VAHI-501 · `androidx.pdf` spike ⬜ S
**Do:** validate [D8](22-decisions.md#d8--pdf-annotation-builds-on-androidxpdf--androidxink) — render a page, draw one annotation, save, reload. Assess the Fragment-interop cost and the `minSdk 28` bump.
**Accept:** D8 → Accepted or Superseded; if superseded, the alternative is chosen explicitly.

### VAHI-502 · PDF import ⬜ L
**Do:** SAF picker (**no storage permission** — [`20-release-plan.md`](20-release-plan.md) §4.1), page-range preview, copy to `source.pdf`, create PDF-backed pages.
**Deps:** VAHI-501, VAHI-401.

### VAHI-503 · PDF annotation ⬜ L
**Accept:** annotations anchor to **PDF page coordinates**, so they stay correct at every zoom and survive export.
**Deps:** VAHI-502.

### VAHI-504 · PDF export with annotations ⬜ M · **Deps:** VAHI-503, VAHI-404
### VAHI-505 · Folders ⬜ L · **Deps:** VAHI-303
### VAHI-506 · Multi-select ⬜ M · **Deps:** VAHI-505
### VAHI-507 · Lasso select ⬜ L · **Deps:** VAHI-121

---

## E6 — v1.2: Recognition and search

### VAHI-601 · ML Kit integration ⬜ M
**Do:** [D9](22-decisions.md#d9--handwriting-recognition-uses-ml-kit-digital-ink-on-device). Feed `StrokeInputBatch` directly — recognition needs ordered points with timing, not rendered paths.
**Deps:** VAHI-102, VAHI-305.

### VAHI-602 · Language pack management ⬜ M
**Do:** explicit, visible downloads in Settings → Languages, size shown, Wi-Fi-only default. **Never block writing on a download** (R9). Adds the `INTERNET` permission — update the Data Safety form in the same release.
**Deps:** VAHI-601.

### VAHI-603 · Background recognition ⬜ M
**Do:** WorkManager, idle + charging; write to `pages.recognisedText`; never on the write path.
**Deps:** VAHI-601.

### VAHI-604 · ★ Content search ⬜ M
**Why:** *"search your handwritten Hindi notes and find them."* This is the differentiator.
**Do:** Room FTS over `recognisedText`; results show a page thumbnail with the match highlighted.
**Deps:** VAHI-603, VAHI-408.

### VAHI-605 · Search-in-notebook ⬜ S · **Deps:** VAHI-604
### VAHI-606 · Read page aloud ⬜ S
**Why:** the genuinely valuable accessibility feature for a handwriting app ([`19-quality-bar.md`](19-quality-bar.md) §3.2). **Deps:** VAHI-603

---

## E7 — v2.0: Accounts and sync

Sized coarsely; re-plan when it is actually next.

| ID | Item | Size |
|---|---|---|
| VAHI-701 | Backend selection and schema | M |
| VAHI-702 | Google Sign-In, with **local-data adoption** (VAHI-309) | M |
| VAHI-703 | Sync protocol + conflict resolution | XL |
| VAHI-704 | Multi-device continuity | L |
| VAHI-705 | Plus tier + billing ([D16](22-decisions.md#d16--freemium-with-a-generous-free-tier-no-page-caps)) | L |
| VAHI-706 | Account screen rewrite ([D10](22-decisions.md#d10--no-auth-wall-account-is-optional-and-requested-in-context)) | M |
| VAHI-707 | Server-side backup and restore | L |

---

## How to keep this file honest

1. **Update status in the same session as the code.** A backlog that lags reality is worse than none.
2. **Never delete an item.** Mark it ❌ Dropped with a reason. The trail is what stops the same idea being re-proposed in three months.
3. **Split anything over XL.** If an item cannot be sized, it is not understood yet — that is a signal, not a formality.
4. **New work gets an ID here first**, then gets built. Undocumented work is how a plan quietly becomes fiction.
5. **When acceptance criteria change, say so in `PROJECT-LOG.md`** — a moved goalpost that nobody recorded is indistinguishable from a missed one.
