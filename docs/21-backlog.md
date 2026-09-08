# 21 — Backlog

> **This is the execution spine.** Every piece of work in the plan, numbered, sized, dependency-ordered, with acceptance criteria.
>
> **During implementation, this file is updated in the same session as the code.** Status here plus the newest entry in [`PROJECT-LOG.md`](PROJECT-LOG.md) is how a cold session knows exactly where things stand.

**Status:** ⬜ Not started · 🟨 In progress · ✅ Done · ⏸️ Blocked · ❌ Dropped (with reason)
**Size:** engineering-days ([D7](22-decisions.md#d7--schedule-expressed-in-engineering-days-not-calendar-dates)). XS ≤ 0.5 · S ≤ 1 · M ≤ 3 · L ≤ 5 · XL > 5 (split it)

---

## Summary

> **Updated Session 7, extended pass (2026-09-08).** `VAHI-006`, `VAHI-205` (both halves — the owner confirmed dropping the video splash), `VAHI-307`, `VAHI-309`, `VAHI-408`, and `VAHI-406` (Settings screen, once its `VAHI-307` dependency landed) all done. `VAHI-204` partial (the two glyphs that were genuine accessibility violations — no `contentDescription` at all — fixed; the rest are visual-only and already screen-reader-safe via `semantics`, deferred to avoid redoing icon work once Figma iconography (`VAHI-004`) is read). E1/E2/E3 all now have real, working down-payments (navigation, coordinate types, command history, Room+Hilt+a real ViewModel) — sized at "items done" not "epic done," since every remaining item in those epics is still real, substantial work. Persistence's *metadata* half is closed — notebooks, preferences, and profile identity are all real; ink is not, and cannot be until `VAHI-101`/`VAHI-110` (wired) land.

| Epic | Theme | Items | Size | Status |
|---|---|---|---|---|
| **E0** | Stabilise & de-risk | 9 | ~7 ed | 🟨 8/9 done — only `VAHI-004` left, blocked on the owner (Figma export) |
| **E1** | Ink foundation (F1, F2, F5) | 10 | ~17 ed | 🟨 2/10 started — `VAHI-110` (types only, not wired), `VAHI-120` (scoped fix) |
| **E2** | App foundation + UI rebuild (F3) | 13 | ~24 ed | 🟨 6/13 started — `VAHI-201` (partial), `VAHI-202`/`VAHI-205` (done), `VAHI-204` (partial), `VAHI-206`/`VAHI-207` (partial) |
| **E3** | Persistence (F4) | 9 | ~16 ed | 🟨 5/9 started — `VAHI-307`/`VAHI-309` (done), `VAHI-301`/`VAHI-303`/`VAHI-304` (notebook scope done, page/ink scope untouched, correctly blocked on `VAHI-110` wired + `VAHI-102`) |
| **E4** | v1.0 completion & release | 14 | ~24 ed | 🟨 2/14 done — `VAHI-406`, `VAHI-408` |
| **E5** | v1.1 — PDF & organisation | 7 | ~19 ed | ⬜ |
| **E6** | v1.2 — Recognition & search | 6 | ~14 ed | ⬜ |
| **E7** | v2.0 — Accounts & sync | 7 | ~33 ed | ⬜ |

**To v1.0 release: ≈ 88 engineering-days** at the estimates above; Session 6 (both passes) closed roughly 9–10 ed of that. Multiply by your own throughput and a focus factor of 0.6–0.7 for a calendar estimate.

**A milestone worth naming: the app now has its first real, persisted, end-to-end user-visible loop** — create a notebook, see it in the library, close and reopen the app, it's still there, open it, delete it, undo the delete. That loop did not exist at the start of this session. It is *not* the doc 11 "nothing is lost" milestone (`VAHI-305`) — that one is specifically about ink, and ink is still not persisted — but it is real progress of the same kind.

### The critical path

```
VAHI-001 ✅ commit tree
   └─▶ VAHI-002 ✅ upgrade deps ──▶ VAHI-101 ⬜ ink benchmark ──▶ VAHI-102 ⬜ InkEngine
                    │                (needs a real tablet)              │
                    └──▶ VAHI-201 🟨 navigation ──┐                     │
                          (nav done; canvas        ▼                     ▼
                           state still remember-  VAHI-110 🟨 coordinate model
                           scoped, ink still lost   (types done, not yet wired
                           on rotation)              into the canvas)   │
                                               │                   │
                                               ▼                   ▼
                                      VAHI-301 ⬜ Room schema ◀───────┘
                                               │
                                               ▼
                                      VAHI-305 ⬜ ★ MILESTONE: nothing is lost
                                               │
                                               ▼
                                      E4 ──▶ v1.0
```

**The hard ordering rule, restated:** `VAHI-110` (coordinate model) **must** complete before `VAHI-301` (persistence). Strokes stored in screen pixels cannot be migrated — you will not know what device or zoom produced them. Do not relax this under schedule pressure. **This rule now has teeth** — the types exist — but `VAHI-301` still correctly waits, because the types aren't wired into the canvas yet and the ink-engine decision (`VAHI-101`) hasn't been made.

**The next real blocker is `VAHI-101`: it needs a physical mid-range Android tablet.** Everything device-independent that could reasonably be done ahead of it — dependency baseline, navigation, coordinate math, command history, the worst shipped bugs — is now done. What's left either needs a device (`VAHI-008`→`VAHI-101`, on-device verification of everything in this session) or needs the ink-engine/data-layer decision that benchmark settles.

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

### VAHI-006 · Delete the phone dead-end properly ✅ Done (Session 7) · XS
**Why:** [D10](22-decisions.md#d10--no-auth-wall-account-is-optional-and-requested-in-context) removes the auth wall entirely, which makes `AuthFallbackLayout` moot on the startup path.
**Done:** the tablet-only message moved to the app shell (`AppRoot.kt`'s new `TabletOnlyFallback()`), checked once via `windowClass()` above the `NavHost` — so it now protects *every* destination (Splash included), not just the one screen that used to check it. `AuthFallbackLayout` deleted from `AuthScreen.kt`. Auth itself has no size branch left; it only ever renders once the shell has already confirmed Expanded.
**Deps:** VAHI-201. **Verified:** `:app:assembleDebug`, `:app:testDebugUnitTest` both `BUILD SUCCESSFUL`.

### VAHI-007 · First unit tests: command history ✅ Done, folded into VAHI-120 (Session 6) · S
**Why:** the best test in the project — pure logic, real edge cases, and the area that was **known** to be wrong (erase and undo shared one stack; drawing after erasing silently made the erase permanent).
**What happened:** rather than writing failing tests against the old `CanvasState` and fixing it as a separate later pass, `VAHI-120`'s scoped fix (see below) was small enough to do in one pass — tests were written *and* made to pass in the same session. This entry is kept only so `VAHI-120`'s dependency graph stays accurate.
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
**⚠️ Added acceptance criterion (Session 6, [D17](22-decisions.md#d17--android-tablets-for-v1-all-devices-as-a-planned-post-v1-phase)): the `.ink` payload must be engine-neutral geometry** — ordered page-space points with pressure/tilt/timestamp — **not `androidx.ink`'s native `ink-storage` blob.** If the native format is used for speed, store the neutral geometry alongside it and treat the neutral form as canonical.
> **Why this belongs here and not in the cross-platform epic:** a native-blob payload cannot be read by a future iOS `PencilKitInkEngine`, and there is no migration path once user notes exist in it — the geometry is locked inside an Android-only format. **Same class of irreversible mistake as the R1 screen-pixel rule.** Costs ~1 ed here; unfixable later. The `.ink` header already carries an `engineId` field ([`17-data-spec.md`](17-data-spec.md) §3) anticipating multiple engines — that only pays off if the payload is portable too.
**Deps:** VAHI-101.

### VAHI-110 · ★ Coordinate model and view transform 🟨 Core types done (Session 6) · M
**Why:** the foundation most likely to be overlooked and the most expensive to retrofit. Strokes today are stored in screen pixels of whatever device drew them.
**Done:** `core/model/` — `ScreenPoint`, `DocumentPoint`, `DocumentOffset`, `PagePoint`, `PageSize` (§2's three spaces as **distinct types**, not a shared `Offset` distinguished only by naming — the same class of unit-mixing bug as #18, now caught by the compiler); `ViewTransform` (scale + pan, `documentToScreen`/`screenToDocument` via the real `androidx.compose.ui.unit.Density`, not a raw `Float`); `PageLayout` (the page-strip model — precomputed offsets, `pageAt`/`toDocumentPoint`, pages centred by width, `PAGE_GAP_PT = 24`). A4 in points is `PageSize.A4_PORTRAIT`.
**Accept:** 24 pure-JVM tests (no Robolectric needed — confirms these types have no Android/native dependency), including exact round-trips at non-trivial scale/pan/density, gap/boundary cases in `pageAt`, and centring math for mixed page widths. `assembleDebug` + `testDebugUnitTest` both pass.
**Not done — this item is not closed yet:** nothing in the app **uses** this model yet. `CanvasState`/`DrawingCanvas`/`CanvasScaffold` still work entirely in raw screen pixels; pointer input is not yet converted through `screenToDocument`, and no page strip is rendered. That wiring is real, separate work — folding it in here would mean touching the ink engine before `VAHI-101`/`VAHI-102` (the `androidx.ink` decision) is settled, which is exactly the ordering mistake [D6](22-decisions.md#d6--ink-architecture-adopt-androidxink) warns against. **What this item *does* unblock right now:** Room can be designed and built against page-space geometry ([`17-data-spec.md`](17-data-spec.md) R1) without waiting on the ink engine — see `VAHI-301`.
**Deps:** VAHI-002. **Blocks:** VAHI-301 — *this is the hard ordering rule, and it now technically holds: the types exist.*

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

### VAHI-120 · Command history — scoped fix for the current stroke model 🟨 Done for today's `CanvasState` (Session 6) · M
**Why:** one user action = one undo step. The shared-stack design meant `redo()` could un-erase a stroke, and drawing after an erase silently made it permanent (bug #5).
**Done:** a private `StrokeCommand` sealed type (`Add`, `Erase`) with separate `undoStack`/`redoStack` in `CanvasState`, replacing the single stroke-list `_undoneStrokes`. New `beginErase()`/`finishErase()` gesture lifecycle in `CanvasState`, called from `DrawingCanvas.kt`'s pointer loop — batches an entire erase drag (potentially many `eraseAtPosition` calls, one per pointer sample) into **one** undo step, not one per sample. `Clear` deliberately stays on its own separate mechanism (`VAHI-003`'s `lastClearedSnapshot`/`undoClear()`), not folded into this stack — see the KDoc on `CanvasState.lastClearedSnapshot` for the specific correctness reason (a stale snackbar could otherwise undo the wrong action). History is per-`CanvasState` (i.e. per canvas session, not per-notebook — there's no notebook concept yet) and is cleared by `clearAll()`, matching the original design's intent.
**Accept:** 11 tests in `CanvasStateUndoRedoTest.kt` (Robolectric) — redo never un-erases; undo/redo of an erase; one-drag-one-step; an empty erase drag doesn't consume an undo slot; the redo stack is correctly invalidated by new ink. `assembleDebug` + `testDebugUnitTest` both pass.
**Not done — this is the "full model" gap, deliberately deferred:** [`16-canvas-spec.md`](16-canvas-spec.md) §9's real target (`AddStroke`/`EraseStrokes`/`TransformStrokes`/`ClearPage` all as one unified stack, **notebook-scoped** history with a depth cap, `ClearPage` genuinely undoable via the same `undo()` a user already knows) needs the `FinishedStroke`/`PageState` types that only exist once the `androidx.ink` decision (D6) is settled and `VAHI-102` lands. Building that now would mean designing against types that don't exist yet — the same ordering mistake D6 warns against. §11 "Editing" checklist in doc 16 is **not** fully met by this pass (e.g. no cross-page history, no depth cap) and shouldn't be treated as closed.
**Deps:** VAHI-002 (uses only what's already in the app). **Full version blocked on:** VAHI-102.

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

### VAHI-202 · Hilt + `VahiApplication` ✅ Done (Session 6) · S
**Done:** `VahiApplication` (`@HiltAndroidApp`), `MainActivity` (`@AndroidEntryPoint`), `di/DatabaseModule.kt` (provides `VahiDatabase`/`NotebookDao`), `di/RepositoryModule.kt` (`@Binds NotebookRepository`). Built alongside a real repository rather than as empty scaffolding — see the note in `CLAUDE.md` about not adding DI with nothing to inject.
**Deps:** VAHI-002. **Blocked:** VAHI-301 (now unblocked, metadata scope).

### VAHI-203 · Design-system foundations ⬜ M
**Do:** [`13-visual-design-system.md`](13-visual-design-system.md) §3.1 two-layer tokens · spacing constants · shape standardisation · bundled Noto ([V2](22-decisions.md#v2--font-family-noto-sans--noto-sans-devanagari)) · a `ui/designsystem/` component set.
**Accept:** dark mode works on every migrated screen; zero `Color(0xFF…)` in migrated files; Devanagari renders identically on T1/T2/T3.
**Deps:** VAHI-002. **Soft-blocked by:** VAHI-004.

### VAHI-204 · Replace every emoji with real icons 🟨 2/12 fixed (Session 7) · S
**Why:** `🐦 🔍 ⊞ ≡ ⋮⋮ 🧮 💻 📓 □ ↓ ← G` render differently per manufacturer, cannot be tinted, and screen readers announce them as "bird" and "abacus".
**Done:** the two that were genuine, unmitigated accessibility violations — `HomeScreen.kt`'s profile avatar (`🐦`, zero semantics) and search-bar icon (`🔍`, zero semantics) — replaced with `Icons.Default.Person`/`Icons.Default.Search` (already in use elsewhere in this codebase, so no new dependency) plus a real `contentDescription`. The profile button's touch target was also bumped 44dp → 48dp while in there.
**Not done, deliberately:** the other ten (`⊞ ≡ ⋮⋮ 🧮 💻 📓 □ ↓ ← G` and the drawing toolbar's `✒ ⌫ ↶ ↷`) are visual-only — they already carry correct `contentDescription`/`semantics` (verified by reading `ToolbarGlyphToggle`, which sets `semantics { contentDescription = label }`), so a screen reader already announces "Pen tool," not the glyph. Swapping them now risks doing the work twice: [decision V4](22-decisions.md#v4--figma-is-the-visual-source-of-truth-the-docs-describe-behaviour) makes iconography a Figma-owned concern, `VAHI-004` (reading the Figma) is still blocked on the owner, and several of these glyphs (`⊞ ≡ □`, the paper-style icons) don't exist in the core Material icon set — fixing them means either adding `material-icons-extended` or hand-drawn vectors, a real decision better made once actual iconography exists to match. The Google "G" / Apple glyphs are brand wordmarks, not icons, and belong to `AuthScreen`, which is currently off the nav path entirely (`VAHI-006`).
**Accept (unchanged):** `grep` finds no emoji in `.kt`; every icon has a `contentDescription`.
**Deps:** VAHI-203.

### VAHI-205 · Kill the auth wall and the splash video ✅ Done (Session 7, both halves) · S
**Why:** [D10](22-decisions.md#d10--no-auth-wall-account-is-optional-and-requested-in-context), [D11](22-decisions.md#d11--drop-the-video-splash-and-the-media3-dependency). Biggest single time-to-ink win, and removes the largest dependency in the project.
**Done — the D10 half:** `AppRoot.kt`'s `Splash` destination now navigates to `Library` directly on timeout, never to `Auth`. `Auth` stays declared in the nav graph (real UI work, not thrown away) but is unreached by anything until `VAHI-406` gives it a Settings entry point.
**Done — the D11 half, after checking back with the owner:** `SplashScreen.kt` (the `ExoPlayer`/`splash.mp4` Compose screen) is **deleted**. Replaced with the Android 12+ `SplashScreen` API (`androidx.core:core-splashscreen`, backported to minSdk 26) — a system window shown for the bare instant of cold start, dismissed automatically on first frame, no artificial delay. `Theme.Vahi.Starting` (extends `Theme.SplashScreen`) set on the activity in the manifest; `installSplashScreen()` called in `MainActivity.onCreate()` before `super.onCreate()`. `androidx.media3` (both artifacts) removed from `build.gradle.kts`/`libs.versions.toml`; `splash.mp4` and the now-empty `assets/` directory deleted. The "first-launch-only animated wordmark, skippable" embellishment from D11's original migration note was **not** built — it's onboarding-flavoured polish that belongs with `VAHI-409` (not started) rather than a one-off mechanism built ahead of it.
**Accept:** warm start → library, no forced wait ✅; `media3` gone ✅; APK smaller ✅ (a multi-MB player library plus an ~800 KB video, gone).
**Deps:** VAHI-201. **Verified:** `:app:assembleDebug`, `:app:testDebugUnitTest` both `BUILD SUCCESSFUL`, 51→62 tests across this session unaffected by the removal (nothing tested the video path).

### VAHI-206 · `LibraryViewModel` + split `HomeScreen.kt` 🟨 ViewModel done, split not done (Session 6) · L
**Why:** 990 lines, 15 composables, two data classes. **Split it while introducing its ViewModel** — doing the two separately means refactoring the same 990 lines twice.
**Done:** `LibraryViewModel` exists (`ui/feature/library/`) — `BaseViewModel`'s first real subclass — backed by `NotebookRepository`, collected via `hiltViewModel()` directly inside `HomeScreen`. `HomeScreen.kt` now shows real, persisted notebooks.
**Not done:** the file split. `HomeScreen.kt` is still one file (~870 lines, similar order of magnitude), and `HomeScreen` calls `hiltViewModel()` itself rather than through a separate stateless-composable + thin-Route wrapper (doc 18 §4's fuller pattern). This was a deliberate proportionality call, documented in `LibraryViewModel.kt`'s KDoc — restructuring the whole file was more than this pass's scope warranted alongside standing up Room+Hilt+the ViewModel for the first time. **Revisit this item, don't mark it done.**
**Deps:** VAHI-201, VAHI-202, VAHI-301.

### VAHI-207 · Library rewrite 🟨 Partially done (Session 6) · L
**Why:** every control was a no-op against two hardcoded notebooks.
**Done:** create persists and opens the new notebook; the grid shows real data; tapping an existing card opens it (a capability that **did not exist at all** before, on any hardcoded data); delete is soft with an "Undo" snackbar (rule 5, doc 15); pin/unpin works and "Pinned notes" filters for real; "Edited Xh ago" relative time replaces the creation-date string; the notebook icon is keyed off paper style instead of a title-string heuristic tied to sample data that no longer exists; card menu items pair a real action per item instead of all five dismissing identically.
**Not done:** thumbnails (doc 15 §4.1 — needs page rendering, which needs the ink engine decision), "Jump back in" row, real search, sort selector (though results already come back sorted by `updatedAt DESC`, so the existing "Most recent" label is at least honest), grid/list view toggle, colour-as-accent-bar (cards still use full-card fill), empty states (doc 15 §4.4 — still just the "+ New" tile).
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

> ⚠️ **Stroke/ink geometry may not be persisted before `VAHI-110` is wired into the canvas** (types existing isn't enough — see `VAHI-110`'s own status). **Notebook *metadata* has no such constraint** — it carries no coordinate-space data at all, so it was safe to build ahead of the ink decision. That's what Session 6 did; see `VAHI-301` below for the exact scope split.

### VAHI-301 · Room schema, DAOs, migrations 🟨 Notebook metadata done; Page/Stroke not started · L
**Do:** [`17-data-spec.md`](17-data-spec.md) §2 — entities, indexed bounding boxes, string-persisted enums, `fallbackToDestructiveMigration` banned in release.
**Done (Session 6), notebook metadata only:** `NotebookEntity` (id, title, colorArgb, paperStyle, timestamps, `isPinned`, `deletedAt` tombstone), `Converters` (enum-by-name, not ordinal), `NotebookDao`, `VahiDatabase`. 9 Robolectric tests against a real in-memory database (`RoomNotebookRepositoryTest.kt`) — ordering, the tombstone filter, restore, pin/unpin, ARGB round-trip.
**Deliberately not done:** `PageEntity`, `StrokeIndexEntity` — these carry page-space coordinates and are exactly what the hard rule protects; they wait on `VAHI-110` actually being wired into the canvas and the `androidx.ink` decision (`VAHI-101`/D6).
**Also not done, and flagged rather than silently skipped:** `exportSchema = true`. Room 2.8.4's schema re-validation throws a reproducible `AbstractMethodError` against this project's kotlinx.serialization toolchain when re-reading an already-exported schema JSON — confirmed with a fresh Kotlin daemon and with `kotlinx-serialization-core` forced to both 1.7.3 and 1.8.1, so it's a genuine upstream incompatibility, not a resolvable local version clash. Zero functional cost at schema v1 with no migrations yet; see the comment in `app/build.gradle.kts`. **Re-enable before shipping any real schema change** (docs/17-data-spec.md R4) — either Room will have a fix, or re-litigate the Room version then.
**Accept (metadata scope):** DAO tests against a real database — done. Migration test harness — not needed yet at v1 with no migrations.
**Deps:** VAHI-002, VAHI-202, VAHI-005 — done. **Full scope still deps on:** VAHI-110 (wired), VAHI-101/102.

### VAHI-302 · `InkFileStore` and the `.ink` format ⬜ L
**Do:** [`17-data-spec.md`](17-data-spec.md) §3 — header with `formatVersion` and `engineId`, temp-file + fsync + atomic rename, debounced writes, compaction.
**Accept:** round-trip tests; a kill -9 mid-write never produces a truncated file.
**Deps:** VAHI-301 (full scope), VAHI-102.

### VAHI-303 · Repository layer 🟨 NotebookRepository done; ink-related repositories not started · M
**Do:** the interfaces in [`17-data-spec.md`](17-data-spec.md) §9, `Result`-returning, `Flow`-observing, with viewport-aware `loadStrokes`.
**Done:** `domain/repository/NotebookRepository.kt` (interface) + `data/repository/RoomNotebookRepository.kt` (impl) — trimmed from doc 17 §9's full contract (no folders, no duplicate, no purge scheduling; add when the UI that needs them exists). `domain/` has zero Android imports — verified by `grep`, after a self-audit caught that it originally carried `androidx.compose.ui.graphics.Color` (a UI type in the domain layer, contradicting the very KDoc on `NotebookRepository`). Fixed by moving to a raw ARGB `Int`, which also removed a pointless `Color`↔`Int` round-trip since the Room column was already an int; `HomeScreen` converts at the UI boundary. No Room entity appears in any composable signature — `HomeScreen.kt` only imports `domain.model.Notebook`.
**Not done:** `PageRepository`, `InkRepository` — wait on `VAHI-301`'s full scope and `VAHI-302`.
**Deps:** VAHI-301 (metadata scope) — done for `NotebookRepository`. Full scope: VAHI-301 (full), VAHI-302.

### VAHI-304 · Notebook and page CRUD 🟨 Notebook CRUD done; page CRUD not started (no page concept exists yet) · M
**Done:** creating a notebook actually creates one and the library reflects it — the hardcoded two-item list is gone. Delete (soft, undoable), pin/unpin also work. This closes the exact gap doc 09's original audit called out: *"Creating a notebook does not create anything — it navigates straight to a blank canvas. The new notebook never appears in the library."* That sentence is no longer true.
**Not done:** page-level CRUD (add/insert/delete/reorder). There is currently no multi-page concept in the UI at all — one notebook still means one canvas, exactly as before. Real pages are `VAHI-401`, gated on the paged model actually landing in the editor.
**Deps:** VAHI-303 (metadata scope) — done for notebooks.

### VAHI-305 · ★★ Autosave — *the milestone that matters* ⬜ M
**Why:** *draw → close the app → reopen → the ink is still there.* **Everything before this is a demo; everything after it is a product.**
**Do:** [`16-canvas-spec.md`](16-canvas-spec.md) §10 — debounced writes, `NonCancellable` `ON_PAUSE` flush, "Saved ✓" indicator, persistent non-dismissible bar on write failure. No Save button, ever.
**Accept:** force-stop mid-session → reopen → everything is there, on the right page, at the right zoom. At most one uncommitted stroke lost.
**Deps:** VAHI-303, VAHI-302.

### VAHI-306 · Page thumbnails ⬜ M
**Do:** generate on page change via WorkManager; WebP; feeds library cards and the page drawer.
**Deps:** VAHI-305, VAHI-115.

### VAHI-307 · DataStore preferences ✅ Done (Session 7) · S
**Do:** ~~Proto DataStore~~ for theme, paper theme, handedness, finger-draw, default paper, ~~pen slots~~, language. **No `SharedPreferences`.**
**Built differently from the "Do" line, deliberately:** plain `datastore-preferences`, not Proto DataStore — matches docs/18-architecture-target.md §5's artifact table (which lists `datastore-preferences`), not this file's own prose. Proto DataStore's advantage is enforced schema evolution across a `.proto` file; not worth a protobuf compiler plugin for six scalar settings, and it's still typed/atomic/`Flow`-observable, which is what docs/17-data-spec.md §2.4 actually requires. **Pen slots excluded**: docs/17-data-spec.md §2.4 defines `PenSlotEntity` as a **Room** table, contradicting this line — the concrete entity definition wins.
**Done:** `domain/model/UserPreferences.kt` (`AppTheme`/`PaperTheme`/`Handedness` enums, `fingerDrawEnabled` defaulting `true` per CLAUDE.md's finger-first guidance), `domain/repository/PreferencesRepository.kt`, `data/preferences/DataStorePreferencesRepository.kt` (takes `DataStore<Preferences>` directly rather than `Context` — makes it a plain-JVM test, no Robolectric), `di/PreferencesModule.kt`. Not wired into any UI yet — that's `VAHI-406`'s job, this is the persistence layer only.
**8 new tests**, all plain JUnit (no Android framework needed).
**Deps:** VAHI-202. **Verified:** `:app:assembleDebug`, `:app:testDebugUnitTest` both `BUILD SUCCESSFUL`.

### VAHI-308 · Orphan sweep and compaction jobs ⬜ S
**Do:** [`17-data-spec.md`](17-data-spec.md) §4 and §1 — WorkManager, idle + charging constrained.
**Deps:** VAHI-302.

### VAHI-309 · Local profile identity ✅ Done (Session 7) · S
**Why:** [`17-data-spec.md`](17-data-spec.md) §7. Getting this wrong later means "sign in and your local notes disappear" — the single worst bug this app could ship, and the default outcome if identity is bolted on afterwards.
**Done:** `ProfileEntity`/`ProfileDao` (Room), `domain/repository/ProfileRepository.kt`, `RoomProfileRepository.kt`. `NotebookEntity` gains `ownerProfileId`, stamped by `RoomNotebookRepository.create()` via `ProfileRepository.getOrCreateLocalProfileId()`. Not surfaced on the domain `Notebook` — nothing above the repository needs to reason about profile ownership with only one profile in existence. The get-or-create race (two callers both finding no profile) is closed with `@Transaction` on a default `ProfileDao` method, not `OnConflictStrategy.IGNORE` as first drafted — IGNORE only helps when two writers collide on the *same* key, and two independently-generated UUIDs never do; caught before committing, fixed, and covered by a concurrent-callers test.
**Accept:** every notebook carries `ownerProfileId` ✅; the adoption path for a future sign-in is documented on `ProfileRepository`'s KDoc — not implemented, since sign-in doesn't exist yet (`VAHI-702`, v2.0).
**Deps:** VAHI-301. **Verified:** `:app:assembleDebug`, `:app:testDebugUnitTest` both `BUILD SUCCESSFUL`.

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

### VAHI-406 · Settings screen ✅ Done (Session 7) · L
**Do:** [`15-ux-blueprint.md`](15-ux-blueprint.md) §6, two-pane. Handedness, paper theme, finger-draw, storage-by-notebook, about.
**Done:** real navigation destination (`Settings` route in `AppRoot.kt`), replacing `SettingsOverlay`'s four grey placeholder rows and its dropdown-anchoring logic entirely (`isSettingsOpen`/`settingsButtonPosition` deleted from `HomeScreen.kt`). Two-pane: `SettingsSectionList` (left) + detail pane (right) — Appearance (app theme, paper theme, default paper style), Writing (handedness, finger-draw with a live description), Storage (real on-disk DB size + notebook count — see the note below), About (version from `BuildConfig`, newly enabled via `buildFeatures.buildConfig = true`). `SettingsViewModel` combines `PreferencesRepository.observe()` with `NotebookRepository.observeAll()`; the one-time DB-file-size read is dispatched onto `Dispatchers.IO` explicitly — `File.length()` is blocking I/O and a ViewModel's `init` block runs on whichever thread first calls `hiltViewModel()` (Compose's main thread).
**Scoped down from the full docs/15-ux-blueprint.md §6 table, deliberately:** Account and Vahi Plus sections are **not built** — neither auth nor billing exists (both v2.0), and building either as a no-op placeholder is exactly the trap CLAUDE.md's failure-mode table warns about ("wiring up the ~20 no-op buttons... work you throw away twice"). "Storage by notebook" is a **total** figure, not a per-notebook breakdown — with ink not yet persisted, every notebook is just a metadata row, so a per-notebook split would be close to meaningless; the screen says so explicitly rather than implying a precision it doesn't have.
**Deps:** VAHI-307, VAHI-201. **Verified:** `:app:assembleDebug`, `:app:testDebugUnitTest` both `BUILD SUCCESSFUL`.

### VAHI-407 · Notebook properties + export sheet ⬜ M
**Deps:** VAHI-404, VAHI-207.

### VAHI-408 · Title search ✅ Done (Session 7) · S
**Done:** the search bar in `ToolbarRow` was decorative (a static "Search notebooks..." `Text`, no input at all) — replaced with a real `BasicTextField`, filtered client-side against the already-loaded notebook list (`HomeScreen()`'s `searchQuery` state), same pattern as the existing `showPinnedOnly` filter right next to it. No ViewModel/repository change needed — the dataset is small and already fully loaded, so a server-side or ViewModel-level query wouldn't buy anything.
**Deps:** VAHI-303. **Verified:** `:app:assembleDebug`, `:app:testDebugUnitTest` both `BUILD SUCCESSFUL`.

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

## E8 — v1.5: All Android devices ("Axis A")

Full plan: [`23-cross-platform-plan.md`](23-cross-platform-plan.md) Part 5. **Gated on v1 shipping** — do not start earlier. Entirely within the current Android codebase: no KMP, no new build system, no second language. Re-scopes [D2](22-decisions.md#d2--tablet-only-for-v1) per [D17](22-decisions.md#d17--android-tablets-for-v1-all-devices-as-a-planned-post-v1-phase).

| ID | Item | Size |
|---|---|---|
| VAHI-801 | Window-size-class infrastructure + adaptive scaffolding (builds on the existing `WindowInfo.kt`) | M |
| VAHI-802 | Library adaptive layout — `GridCells.Adaptive`, carousel recents | M |
| VAHI-803 | Editor adaptive layout — tool rail becomes a **bottom bar** on Compact (the left-rail hand-occlusion logic in doc 12 §3.1 assumed a landscape tablet and is wrong on a phone) | M |
| VAHI-804 | Overlays → bottom sheets on Compact (retires the popover-anchoring bug class entirely) | S |
| VAHI-805 | Settings single-pane drill-down on Compact | S |
| VAHI-806 | Per-size-class canvas zoom defaults and min-scale clamps (doc 16 §2.2) | S |
| VAHI-807 | Foldable support — fold seam, state across the fold configuration change | M |
| VAHI-808 | Cross-size test pass (phone/tablet/foldable × portrait/landscape) | M |

**≈ 14 ed.** Cheaper than iOS and reaches more of the target user — most students with a tablet also own a phone.

---

## E9 — v2.0+: iOS via Kotlin Multiplatform ("Axis B")

Full plan: [`23-cross-platform-plan.md`](23-cross-platform-plan.md) Part 6. **Gated on E8 completing** — doing this first means building iPhone layouts twice. Also gated on phone usage data justifying the spend, and (realistically) on sync existing, since "works on iPad" without sync means a second, unrelated set of notes.

| ID | Item | Size |
|---|---|---|
| VAHI-901 | KMP module restructuring (`commonMain`/`androidMain`/`iosMain`) + build setup | M |
| VAHI-902 | Hilt → **Koin** (Hilt has no KMP path) | M |
| VAHI-903 | Navigation coordinate swap + iOS back-swipe | M |
| VAHI-904 | Room → KMP driver (`androidx.sqlite`) | M |
| VAHI-905 | ★ **`PencilKitInkEngine`** — the second ink engine, behind the existing `InkEngine` interface | XL |
| VAHI-906 | Engine-neutral `.ink` cross-engine round-trip compatibility + tests | M |
| VAHI-907 | ML Kit Digital Ink iOS binding (cinterop) — **the API does exist on iOS**; this is binding work, not reinvention | M |
| VAHI-908 | PDF via PDFKit (if v1.1 shipped) | L |
| VAHI-909 | iOS platform conventions — safe areas, Dynamic Type, VoiceOver, share sheet, file picker | M |
| VAHI-910 | iOS build, signing, App Store Connect, TestFlight | M |
| VAHI-911 | QA matrix expansion (ongoing thereafter, not one-off) | L |

**≈ 45 ed**, plus a standing **+30–40% on all subsequent feature work** — two ink engines, two stores, two QA surfaces, forever.

> ⚠️ **`VAHI-905` is the risk item, and `VAHI-102` (Android ink engine) carries a prerequisite for it:** the `.ink` payload must be **engine-neutral geometry**, not `androidx.ink`'s native blob. Get that wrong and Android-written notes are permanently unreadable on iOS with no migration path — the same class of one-way door as the R1 coordinate rule. See [`23-cross-platform-plan.md`](23-cross-platform-plan.md) Part 4 and X3.

---

## How to keep this file honest

1. **Update status in the same session as the code.** A backlog that lags reality is worse than none.
2. **Never delete an item.** Mark it ❌ Dropped with a reason. The trail is what stops the same idea being re-proposed in three months.
3. **Split anything over XL.** If an item cannot be sized, it is not understood yet — that is a signal, not a formality.
4. **New work gets an ID here first**, then gets built. Undocumented work is how a plan quietly becomes fiction.
5. **When acceptance criteria change, say so in `PROJECT-LOG.md`** — a moved goalpost that nobody recorded is indistinguishable from a missed one.
