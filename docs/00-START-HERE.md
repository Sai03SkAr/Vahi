# 00 — START HERE

**This is the entry point for anyone — human or AI assistant — picking up the Vahi project cold.**

Read this file completely, then the four documents marked **Required** in §6. That is enough to work productively. Do not re-derive anything already settled in [`22-decisions.md`](22-decisions.md).

---

## 1. What Vahi is, in 60 seconds

**Vahi** is a native Android digital notebook — handwriting-first, tablet-first, built entirely in Kotlin with Jetpack Compose. Target users: **students in India on Android tablets**. The intended differentiator: **Indic-script handwriting recognition**.

The app has a **complete UI shell** (system splash → library → canvas, auth wall removed as of Session 7), **working freehand pen drawing**, **a canvas tool rail**, and — as of Session 6 — **real navigation-compose routing** and a **fixed undo/erase/clear model**. It still has **no backend, no real authentication, and no ink persistence** (notebook metadata, app preferences and local profile identity all persist; strokes do not yet). Ink is lost the moment you leave the canvas or rotate the device — the navigation *position* now survives that, the canvas *content* does not.

- **Repo:** `/Users/saisalelkar/Desktop/Vahi` · branch `main` · remote `origin/main`
- **Package:** `com.vahi.app` · single Gradle module `:app`
- **Stack:** Kotlin 2.1.20 · Compose BOM 2025.01.01 · AGP 8.13.2 · Material 3 · `navigation-compose` · Room · Hilt · DataStore · minSdk 26 / target 36
- **Size:** 54 Kotlin source files, ~5,410 lines (incl. 10 test files, 68 tests). Largest: `HomeScreen.kt` at ~1,100 lines (`VAHI-206`'s still-pending split).
- **Build:** `./gradlew clean assembleDebug testDebugUnitTest` → **BUILD SUCCESSFUL, 68 tests, 0 failures** (verified Session 7 from cold Gradle *and* Kotlin daemons)
- **Design:** [`vahi-v1` on Figma](https://www.figma.com/design/JJAGOm1by1gHE6Nxe4yUhx/vahi-v1?node-id=0-1) — owner-authored, not yet reconciled with the docs ([V4](22-decisions.md#v4--figma-is-the-visual-source-of-truth-the-docs-describe-behaviour), `VAHI-004`)

---

## 2. Current phase

> ### IMPLEMENTATION IN PROGRESS · FOUNDATION WORK STARTED
>
> **Session 6 (2026-09-08) did the first real implementation pass, in two parts.** Part 1 worked the backlog's device-independent items in order: committed the working tree (`VAHI-001`), upgraded the dependency baseline (`VAHI-002`), configured backup rules (`VAHI-005`), fixed the tool rail's four shipped defects including the most severe bug in the project — an unrecoverable Clear button (`VAHI-003`) — replaced hand-rolled navigation with `navigation-compose` (`VAHI-201`, partial), added the coordinate-model types (`VAHI-110`, types only, not yet wired in), and fixed the undo/erase stack conflation (`VAHI-007`/`VAHI-120`). Part 2 (same day, extended pass) added a real persistence foundation: Room + Hilt + `LibraryViewModel` (`VAHI-202`, `VAHI-301`/`VAHI-303`/`VAHI-304` scoped to notebook metadata) — **notebooks are now genuinely created, listed, opened, pinned and deleted-with-undo**, not hardcoded. **7 of the 20 catalogued bugs are now fixed** (#5, #8, #13, #15, #16, #18, #19); bug #1 is half-fixed. Full detail: `PROJECT-LOG.md`'s two newest entries.
>
> **Session 7 (2026-09-08), two passes.** First pass implemented D10 (`VAHI-006`, `VAHI-205` partial): the auth wall off the startup path, the tablet-only fallback moved to the app shell, plus the two emoji that were genuine accessibility violations fixed (`VAHI-204`, partial). Second pass, after re-deriving backlog status from the file itself rather than trusting an earlier summary (E4–E7 — most of the v1.0 product — were still `⬜`, not "done"), picked up everything that was actually unblocked: **D11 implemented** after checking back with the owner — the `ExoPlayer`/`media3` video splash is deleted, replaced with the Android system `SplashScreen` API (`VAHI-205` now fully done); **`VAHI-307`** (DataStore preferences); **`VAHI-309`** (local profile identity — every notebook now carries `ownerProfileId`, caught and fixed a real get-or-create race in the same pass); **`VAHI-408`** (title search — the search bar was decorative with zero input; now real); **`VAHI-406`** (a real two-pane Settings screen, reached by real navigation, replacing the `SettingsOverlay` dropdown of four grey placeholder rows — Account and Vahi Plus deliberately not built, since neither auth nor billing exists). 62 tests, up from 51, all passing.
>
> **A verification pass on 2026-09-09 then found that the Settings screen shipped six controls that all silently did nothing** — every preference persisted, none was read back by anything. Two (`appTheme`, `defaultPaperStyle`) are now genuinely wired; three say "Saved, but not applied yet" on the control itself; one has no UI yet. The same pass fixed a `core/`-layer purity violation (`ViewTransform` imported Compose's `Density` for a single `Float`) and two live accessibility defects — a 28 dp unlabelled *delete* menu button, and a 44 dp **completely unlabelled** canvas back button. Detail in `PROJECT-LOG.md`'s newest entry; the ⚠️ note on `VAHI-406` in the backlog records why it happened.
>
> **The next action is `VAHI-008` → `VAHI-101`: get a real mid-range Android tablet and measure ink latency**, which settles decision D6. This is now the binding constraint — every device-independent item that could reasonably precede it is done. In parallel, on-device verification of everything shipped so far would be high-value and needs no further planning, just a device.
>
> **All-device support (phones, foldables, iPad, iPhone) is now planned and settled as a post-v1 phase** — [D17](22-decisions.md#d17--android-tablets-for-v1-all-devices-as-a-planned-post-v1-phase), planned in full in [`23-cross-platform-plan.md`](23-cross-platform-plan.md). **Nothing is started, and v1 scope is unchanged.** Two things from it matter to work happening *now*: (1) keep using window size classes rather than hardcoded dp — free, keeps the door open; (2) **`VAHI-102` gained a hard acceptance criterion — the `.ink` payload must be engine-neutral geometry, not `androidx.ink`'s native blob** — otherwise Android notes become permanently unreadable on a future iOS build, with no migration path.

### What Session 6 changed

1. **The most severe bug in the project is fixed.** The tool rail's Clear button destroyed a page with no way back, one tap below Undo/Redo. It now snapshots the page and offers an "Undo" snackbar. Three more shipped tool-rail defects fixed alongside it.
2. **All three of the app-foundation/persistence/ink foundations now have real down-payments, not just specs.** Navigation is genuinely `navigation-compose` (bug #8 fixed by construction). The coordinate-model *types* exist and are tested (24 tests, no Robolectric needed — confirms they're genuinely Android-free) but aren't wired into the canvas yet. Room + Hilt are real, with a working `NotebookRepository` and `LibraryViewModel` — `BaseViewModel`'s first subclass.
3. **The library is no longer a demo.** Create → persists → appears in the library → survives an app restart → can be reopened, pinned, deleted-with-undo. That loop did not exist before this session.
4. **Nothing was verified on a device**, same as every prior session — this environment still has no GUI. Every fix above is verified by compilation, 51 passing unit tests, and reading the code, not by touching a screen.

### Still true, and still the thing that most needs doing

**Nothing has ever been verified on a device.** `VAHI-008` — get a real tablet — unblocks more than any other single item: the D6 latency benchmark, on-device confirmation of everything fixed this session, and the two most severe *remaining* open bugs — #14 (a resting palm draws instead of the stylus) and #17 (an in-progress stroke is discarded on back) — both need a device to even reproduce, let alone fix with confidence.

---

## 3. The seven things that matter most

1. **The bug list is really a short list of missing foundations.** Twelve original bugs = 3 real bugs + 2 decisions + 7 symptoms of five absent architectural pieces. Don't fix them one by one. ([`11-forward-plan.md`](11-forward-plan.md) Part 3.)

2. **Hard ordering constraint: the coordinate model MUST land before persistence.** Strokes are stored as a mutable `Path` in *screen pixel* coordinates. Once persisted, they cannot be migrated — you will not know what device or zoom produced them. **This is the rule not to break under schedule pressure.** (`VAHI-110` before `VAHI-301`.)

3. **Ink feel is a go/no-go gate, and it must be measured.** Compose's normal draw pipeline carries 2–3 frames of latency and you cannot optimise past it inside a `Canvas`. Measure on a **mid-range** tablet, write the number down. (`VAHI-101`.)

4. **A resting palm currently draws.** `event.changes.firstOrNull()` selects the wrong pointer, and the palm usually lands before the stylus tip. This is not an edge case — it is what happens on every stroke of real handwriting. ([`09-project-status.md`](09-project-status.md) #14.)

5. **Target hardware changes the engineering.** Budget tablets make performance a *requirement*, not polish — and **you cannot assume a pressure-sensitive stylus is present.** Design for the finger case; treat stylus data as an enhancement.

6. **Time-to-ink is the key UX metric.** The forced video splash and the auth wall are both gone as of Session 7 — launch now goes straight to the library. Landing on a new blank page each time (rather than the last-edited one) is still unsolved. Target: ≤ 1 tap, ≤ 2 s. ([`12-ux-plan.md`](12-ux-plan.md) Part 1.)

7. **The repo has one commit and everything else is uncommitted.** A single mistaken `git checkout` costs the entire project. Fix this first. (`VAHI-001`.)

---

## 4. Settled decisions

**[`22-decisions.md`](22-decisions.md) is authoritative.** Summary only — go there for reasoning, alternatives and what would overturn each one.

| # | Decision | Status |
|---|---|---|
| D1 | Paged A4 documents, units in points | ✅ |
| D2 | Tablet-only for v1 | ✅ |
| D3 | Student notebook · Android tablets · India · Indic recognition | ✅ |
| D4 | Brand palette, not Material You | ✅ implemented |
| D5 | A real product to ship | ✅ |
| **D6** | **Adopt `androidx.ink` 1.0.0** | 🔬 pending the latency benchmark |
| D7 | Estimate in engineering-days, not calendar dates | ✅ |
| D8 | PDF annotation via `androidx.pdf` + `androidx.ink` | 🔬 pending a spike |
| D9 | On-device ML Kit Digital Ink for recognition | ✅ |
| D10 | No auth wall — account optional, requested in context | ✅ |
| D11 | Drop the video splash and the Media3 dependency | ✅ implemented (Session 7) |
| D12 | `minSdk` 26 until PDF forces 28 | ✅ |
| D13 | Android Auto Backup disabled for notebook data | ✅ |
| D14 | Single Gradle module until a named trigger fires | ✅ |
| D15 | Offline-first — local DB is the source of truth | ✅ |
| D16 | Freemium, generous free tier, **no page caps** | 🔬 owner's call |
| D17 | Android tablets for v1; all devices (phone/foldable/iPad/iPhone) as a planned post-v1 phase | ✅ |
| V1–V3 | Paper theme · Noto fonts · lavender accent | 🔬 |
| V4 | Figma owns visuals; the specs own behaviour | 🔬 not yet reconciled |
| V5 | Empty-state illustration style | 🔲 open |

**Needs the project owner, not an engineer:** D16 (monetisation shape), V4 (Figma access), V5 (illustration), and the second Indian language after Hindi.

---

## 5. Verified external facts

Checked 2026-09-08. Re-verify before relying on version numbers — these move.

| Library | Status | Why it matters |
|---|---|---|
| **`androidx.ink`** | **1.0.0 stable** (2025-12-17); 1.1.0-alpha07 latest | Compose-native low-latency authoring, real geometry, `ink-storage` serialisation. [D6](22-decisions.md#d6--ink-architecture-adopt-androidxink) |
| **`androidx.pdf`** | **1.0.0-beta01** (2026-08-26); `minSdk 28`; Fragment-based | Annotation layer built on `androidx.ink`. [D8](22-decisions.md#d8--pdf-annotation-builds-on-androidxpdf--androidxink) |
| **ML Kit Digital Ink** | Fully on-device; all nine target Indic scripts; packs downloaded on demand | Zero marginal cost, works offline. [D9](22-decisions.md#d9--handwriting-recognition-uses-ml-kit-digital-ink-on-device) |

Sources: [ink releases](https://developer.android.com/jetpack/androidx/releases/ink) · [ink setup](https://developer.android.com/develop/ui/compose/touch-input/stylus-input/ink-api-setup) · [pdf releases](https://developer.android.com/jetpack/androidx/releases/pdf) · [ML Kit](https://developers.google.com/ml-kit/vision/digital-ink-recognition)

---

## 6. Document map

**Docs 01–09 describe reality (as-built). Docs 10–22 describe intent (plans and specs).** Keep that split honest.

### Tier 1 — What exists today

| Doc | What it is | Priority |
|---|---|---|
| **`00-START-HERE.md`** | This file | **Required** |
| `01-overview.md` | What the project is; core vocabulary | Recommended |
| `02-architecture.md` | Packages, layering, state, navigation *as built* | When touching code |
| `03-app-flow.md` | End-to-end flow with diagrams | Recommended |
| `04-screens.md` | Screen-by-screen composable reference | Reference |
| `05-drawing-engine.md` | Stroke model, `CanvasState`, input, rendering | Before touching ink |
| `06-design-system.md` | Theme/colour/type/shape *as built* | Reference |
| `07-build-and-tooling.md` | Gradle, dependencies, assets, manifest | Reference |
| `08-file-reference.md` | Every file, annotated | Reference |
| **`09-project-status.md`** | What works, what's stubbed, **20 ranked bugs** | **Required** |

### Tier 2 — Strategy

| Doc | What it is | Priority |
|---|---|---|
| `10-roadmap.md` | A menu of implied work — superseded for ordering | Skip |
| **`11-forward-plan.md`** | **Why the plan is shaped this way.** The 5-foundations reframe, the wasted-work rule | **Required** (Parts 3 and 5) |
| `12-ux-plan.md` | UX principles: time-to-ink, auth wall, accessibility, localisation | Before UI work |
| `13-visual-design-system.md` | Visual: tokens, typography, icons, motion | Before UI work |
| **`14-product-spec.md`** | v1 scope in/out, personas, monetisation, exit criteria | **Required** |

### Tier 3 — Buildable specifications

| Doc | Read it before you… |
|---|---|
| `15-ux-blueprint.md` | …build any screen. Inventory, nav graph, wireframes, every state |
| `16-canvas-spec.md` | …touch the canvas. Coordinates, pointer state machine, palm rejection, rendering |
| `17-data-spec.md` | …write anything to disk. Schema, `.ink` format, migrations |
| `18-architecture-target.md` | …add a ViewModel, a dependency, or a package |
| `19-quality-bar.md` | …call anything done. Budgets, device matrix, a11y, tests, DoD |
| `20-release-plan.md` | …think about shipping |
| `23-cross-platform-plan.md` | …think about phones, foldables, iPad or iPhone. **Post-v1; nothing started.** Its Part 8 is the only part affecting work today. |

### Tier 4 — Execution

| Doc | What it is |
|---|---|
| **`21-backlog.md`** | **The execution spine.** ~94 numbered items, sized, dependency-ordered (E8/E9 are the post-v1 all-device epics) |
| **`22-decisions.md`** | **Authoritative decision log.** 22 ADRs |
| **`PROJECT-LOG.md`** | Append-only session record |

**Fastest useful path:** `00` → `09` → `21` → newest entry in `PROJECT-LOG.md`.
**Before writing code:** add `22` and whichever Tier 3 spec covers what you're touching.

---

## 7. Documentation maintenance protocol

**Standing instruction from the project owner: keep the documentation current as the project moves forward.**

### The rule

> **Any session that changes the project updates the docs in the same session, before reporting the work as done.** Documentation drift is what makes a project unresumable, and this doc set is the continuity mechanism.

### What to update, when

| If you… | Update |
|---|---|
| **Anything at all** | Append to `PROJECT-LOG.md`. **Non-negotiable.** |
| Start, finish or block a work item | `21-backlog.md` status |
| Make or overturn a decision | `22-decisions.md` (and never edit a superseded one — mark it ⛔ and write a new one) |
| Change phase or the next action | `00-START-HERE.md` §2 |
| Fix a bug from the list, or find a new one | `09-project-status.md` |
| Add, delete or rename a source file | `08-file-reference.md`; `02-architecture.md` if packages change |
| Change navigation or a screen flow | `03-app-flow.md`, `04-screens.md` |
| Touch the drawing engine | `05-drawing-engine.md` |
| Change theme, colours, typography, shapes | `06-design-system.md` (as-built) |
| Add/remove/upgrade a dependency or change Gradle | `07-build-and-tooling.md` |
| Measure anything | `PROJECT-LOG.md`, **with the device named** |
| Change what a spec requires | The relevant Tier 3 doc — never let code and spec disagree silently |

### Keep the split honest

When something from a Tier 2/3 plan gets **built**, move the description into the matching as-built doc (01–09) and mark it done in the plan and the backlog. **That migration is what stops the plan becoming fiction.**

### Conventions

- Mark unimplemented things ⚠️ or "proposal". Never describe planned work in the present tense.
- Absolute dates ("2026-09-08"), never "today".
- Verify claims against the code before writing them. This doc set's only value is that it is accurate.
- **When docs and code disagree, the code is right and the doc is a bug.**
- A measurement without its device is not a measurement.

---

## 8. Resuming in a new session

If a usage limit is hit or a session ends, open a fresh session in the same directory and paste this. It is written to be self-contained.

```
I'm continuing work on the Vahi project at /Users/saisalelkar/Desktop/Vahi.

Read these first, in order:
  docs/00-START-HERE.md
  docs/09-project-status.md
  docs/21-backlog.md
  docs/22-decisions.md
  docs/PROJECT-LOG.md   (the most recent entry)

Context so you don't re-derive it:
- Vahi is a handwriting notebook for students on Android tablets, Indian market.
  Kotlin + Jetpack Compose, single module, package com.vahi.app.
- The UI shell works end to end, pen drawing works, a canvas tool rail exists, and
  (Session 6) navigation is real navigation-compose with typed routes. There is
  still NO persistence, no backend, no ViewModels, no Hilt.
- Implementation is IN PROGRESS. Planning is complete; docs/22-decisions.md is the
  authoritative decision log — do not re-open anything marked Accepted.
- docs/21-backlog.md is the execution spine. Work items are VAHI-###. Several are
  already done or partially done (VAHI-001/002/003/005/110/120, VAHI-201 partial)
  — check status there before assuming an item hasn't started.
- The next action is VAHI-008 -> VAHI-101: get a real mid-range Android tablet and
  measure ink latency. This settles decision D6 and is now the binding blocker —
  everything device-independent that could precede it is done.
- HARD RULE: the coordinate model (VAHI-110 — types now exist in core/model/, but
  are NOT yet wired into the canvas) must be actually wired in BEFORE any
  persistence (VAHI-301). Strokes are currently still in screen pixels in
  practice and would be unmigratable if persisted as-is.
- androidx.ink 1.0.0 is STABLE and Compose-native. D6 recommends adopting it;
  the latency benchmark (VAHI-101) still has to run on a real mid-range tablet.
- Nothing has ever been verified on a device, in any session including the ones
  that wrote code. Everything is verified by compilation and unit tests only.
- There is a Figma design (docs/22-decisions.md V4) that has not been read yet.

Keep all documentation updated as we go, per docs/00-START-HERE.md section 7.
Confirm with me before starting major implementation work.
```
