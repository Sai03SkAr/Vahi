# 00 — START HERE

**This is the entry point for anyone — human or AI assistant — picking up the Vahi project cold.**

Read this file completely, then the four documents marked **Required** in §6. That is enough to work productively. Do not re-derive anything already settled in [`22-decisions.md`](22-decisions.md).

---

## 1. What Vahi is, in 60 seconds

**Vahi** is a native Android digital notebook — handwriting-first, tablet-first, built entirely in Kotlin with Jetpack Compose. Target users: **students in India on Android tablets**. The intended differentiator: **Indic-script handwriting recognition**.

The app has a **complete UI shell** (splash → auth → home → canvas), **working freehand pen drawing**, and **a canvas tool rail**. It has **no persistence, no backend, no real authentication, and no ViewModels**. Everything lives in in-memory Compose state and is lost the moment you leave a screen or rotate the device.

- **Repo:** `/Users/saisalelkar/Desktop/Vahi` · branch `main` · remote `origin/main`
- **Package:** `com.vahi.app` · single Gradle module `:app`
- **Stack:** Kotlin 2.0.21 · Compose BOM 2024.09.00 · Material 3 · minSdk 26 / target 36
- **Size:** 25 Kotlin source files, ~2,790 lines. Largest: `HomeScreen.kt` at 990 lines.
- **Build:** `./gradlew :app:assembleDebug` → **BUILD SUCCESSFUL** (verified Session 4)
- **Design:** [`vahi-v1` on Figma](https://www.figma.com/design/JJAGOm1by1gHE6Nxe4yUhx/vahi-v1?node-id=0-1) — owner-authored, not yet reconciled with the docs ([V4](22-decisions.md#v4--figma-is-the-visual-source-of-truth-the-docs-describe-behaviour), `VAHI-004`)

---

## 2. Current phase

> ### PLANNING COMPLETE · IMPLEMENTATION NOT STARTED
>
> **Track A (stabilise) shipped in Session 4.** Foundations F1–F5 — ink architecture, coordinate model, navigation/ViewModels, persistence, command history — are **not started**.
>
> **Session 5 (2026-09-08) completed the detailed plan**: nine new specification documents (14–22), verified the three external dependencies the strategy rests on, and re-audited the source, finding eight further defects.
>
> **The next action is [`VAHI-001`](21-backlog.md#vahi-001--commit-the-working-tree): commit the working tree.** Then `VAHI-002` (upgrade dependencies) and `VAHI-008`/`VAHI-101` (get a real tablet and measure ink latency).

### What Session 5 changed, in three bullets

1. **The ink decision got much easier.** `androidx.ink` **1.0.0 went stable on 2025-12-17**, ships **Compose-native** low-latency authoring (no `AndroidView` interop, contrary to the earlier assumption), and is what `androidx.pdf` builds *its* annotation layer on. Three of the four things that make Vahi a product — ink feel, PDF annotation, Indic recognition — key off this one choice. [D6](22-decisions.md#d6--ink-architecture-adopt-androidxink) moved from open to a strong recommendation. The latency benchmark still has to run.
2. **Two serious new bugs, both reachable today.** A resting palm draws instead of the stylus ([#14](09-project-status.md)), and the tool rail's Clear button destroys the page unrecoverably ([#13](09-project-status.md)). Neither was known before.
3. **The plan became executable.** [`21-backlog.md`](21-backlog.md) has 75 numbered items with sizes, dependencies and acceptance criteria — ~88 engineering-days to v1.0.

### Still true, and still the thing that most needs doing

**Nothing has ever been verified on a device.** Every session so far has run in an environment with no GUI. Session 4's toolbar, insets and dropdown fixes are compile-safe and visually unverified; Session 5's bugs #13–#20 were found by reading code, not by reproducing them. `VAHI-008` — get a real tablet — unblocks more than any other single item.

---

## 3. The seven things that matter most

1. **The bug list is really a short list of missing foundations.** Twelve original bugs = 3 real bugs + 2 decisions + 7 symptoms of five absent architectural pieces. Don't fix them one by one. ([`11-forward-plan.md`](11-forward-plan.md) Part 3.)

2. **Hard ordering constraint: the coordinate model MUST land before persistence.** Strokes are stored as a mutable `Path` in *screen pixel* coordinates. Once persisted, they cannot be migrated — you will not know what device or zoom produced them. **This is the rule not to break under schedule pressure.** (`VAHI-110` before `VAHI-301`.)

3. **Ink feel is a go/no-go gate, and it must be measured.** Compose's normal draw pipeline carries 2–3 frames of latency and you cannot optimise past it inside a `Canvas`. Measure on a **mid-range** tablet, write the number down. (`VAHI-101`.)

4. **A resting palm currently draws.** `event.changes.firstOrNull()` selects the wrong pointer, and the palm usually lands before the stylus tip. This is not an edge case — it is what happens on every stroke of real handwriting. ([`09-project-status.md`](09-project-status.md) #14.)

5. **Target hardware changes the engineering.** Budget tablets make performance a *requirement*, not polish — and **you cannot assume a pressure-sensitive stylus is present.** Design for the finger case; treat stylus data as an enhancement.

6. **Time-to-ink is the key UX metric.** Currently 3 taps plus a forced video on *every* launch, landing on a new blank page each time. Target: ≤ 1 tap, ≤ 2 s. ([`12-ux-plan.md`](12-ux-plan.md) Part 1.)

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
| D11 | Drop the video splash and the Media3 dependency | 🔬 owner's call |
| D12 | `minSdk` 26 until PDF forces 28 | ✅ |
| D13 | Android Auto Backup disabled for notebook data | ✅ |
| D14 | Single Gradle module until a named trigger fires | ✅ |
| D15 | Offline-first — local DB is the source of truth | ✅ |
| D16 | Freemium, generous free tier, **no page caps** | 🔬 owner's call |
| V1–V3 | Paper theme · Noto fonts · lavender accent | 🔬 |
| V4 | Figma owns visuals; the specs own behaviour | 🔬 not yet reconciled |
| V5 | Empty-state illustration style | 🔲 open |

**Needs the project owner, not an engineer:** D11 (splash video), D16 (monetisation shape), V4 (Figma access), V5 (illustration), and the second Indian language after Hindi.

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

### Tier 4 — Execution

| Doc | What it is |
|---|---|
| **`21-backlog.md`** | **The execution spine.** 75 numbered items, sized, dependency-ordered |
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
- The UI shell works end to end, pen drawing works, and a canvas tool rail exists.
  There is NO persistence, no backend, no ViewModels, no navigation library.
- Planning is COMPLETE. docs/22-decisions.md is the authoritative decision log —
  do not re-open anything marked Accepted.
- docs/21-backlog.md is the execution spine. Work items are VAHI-###.
  The next action is VAHI-001 unless the log says otherwise.
- HARD RULE: the coordinate model (VAHI-110) must land BEFORE any persistence
  (VAHI-301). Strokes are currently in screen pixels and would be unmigratable.
- androidx.ink 1.0.0 is STABLE and Compose-native. D6 recommends adopting it;
  the latency benchmark (VAHI-101) still has to run on a real mid-range tablet.
- Nothing has ever been verified on a device. Bugs #13-#20 were found by code
  review, not reproduced.
- There is a Figma design (docs/22-decisions.md V4) that has not been read yet.

Keep all documentation updated as we go, per docs/00-START-HERE.md section 7.
Confirm with me before starting major implementation work.
```
