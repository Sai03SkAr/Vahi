# 11 — Forward Plan: Strategy, Foundations and Sequencing

> **Status: strategy document. Still current; partially superseded in detail.**
>
> This remains the best single explanation of **why** the plan is shaped the way it is — especially Part 3 (the reframe) and Part 5 (the wasted-work rule). Read those two sections if you read nothing else.
>
> **What has moved on since it was written (Session 5, 2026-09-08):**
>
> | This document says | Now |
> |---|---|
> | Part 10 holds the decision log | **Moved to [`22-decisions.md`](22-decisions.md)**, expanded to 22 decisions with full context. That file is authoritative. |
> | D6 (ink architecture) is the one open decision gating everything | **Upgraded to a recommendation:** `androidx.ink` **1.0.0 went stable on 2025-12-17** and ships Compose-native low-latency authoring. See [D6](22-decisions.md#d6--ink-architecture-adopt-androidxink) and Part 4/F1 below. |
> | Part 6 tracks A–F define the sequence | **Superseded by [`21-backlog.md`](21-backlog.md)**, which turns them into numbered items with dependencies and acceptance criteria. |
> | Part 7 risk register | **Superseded by [`19-quality-bar.md`](19-quality-bar.md) §6**, re-scored against what Session 5 verified. |
> | Part 8 testing strategy | **Superseded by [`19-quality-bar.md`](19-quality-bar.md) §4.** |
> | "Track A is where to start" | Track A is **done** (Session 4). Start at `VAHI-001`. |
>
> Settled and unchanged: this is a **real product**, notebooks are **paged (A4)**, v1 is **tablet-only**, and the target is **students on Android tablets in the Indian market**.

---

## Part 1 — Where the project actually stands

One paragraph, so the rest has a baseline.

Vahi is a **tablet-first handwriting notebook with a complete UI shell, a working pen, and no memory**. The four-screen flow works end to end. Ink renders. Everything else — the library, auth, the eraser, undo, every menu item, PDF import — is either hardcoded, cosmetic, or implemented-but-unreachable. There is no data layer, no ViewModel, no backend, and no navigation library. 2,500 lines of Kotlin, one commit, ~660 lines uncommitted.

That is a **good** place to be. The expensive, ambiguous part (deciding what the app looks like) is largely done. What remains is well-understood engineering. The risk now is not "can we build it" — it's **building the right foundations in the right order**, because two or three of them are very expensive to retrofit.

---

## Part 2 — The product question (answer this before the engineering)

### The uncomfortable question

GoodNotes, Notability, Samsung Notes, Nebo, Xodo and OneNote all exist. Several have a decade of engineering and hundreds of features. **Vahi cannot win by being a slightly worse version of them.** Any plan that is just "build the remaining 20 buttons" ends with an app nobody has a reason to switch to.

So the highest-leverage question in this project is not technical. It's: **what is Vahi clearly best at?**

### What the code implies — ✅ **confirmed by the project owner**

I originally read this out of the codebase as a hypothesis. It has since been confirmed as broadly correct, so the rest of this document treats it as settled:

| Signal | In the code | Suggests |
|---|---|---|
| The name **Vahi** (वही) | `strings.xml`, package name | Indian market / Indic-language context |
| Sample notebooks: *"Maths notes"*, *"Python Problems"* | `HomeScreen.NotebookGrid` | The user is a **student**, not a professional |
| *"Import PDF — Annotate existing documents"* is a first-class create option | `CreateNotebookOverlay` | Textbooks, question papers, lecture slides |
| *"Imported Books"* filter tab | `FiltersRow` | Same — imported material is a core content type, not an afterthought |
| *"Upgrade"* + *"Upgrade to Plus"* | `TopBar`, `ProfileOverlay` | Freemium, price-sensitive |
| Android-native, tablet-first | Everything | Not chasing the iPad market |
| `// Right panel (AI / research later)` | `NotebookScreen.kt` | AI assistance beside the page is the intended long-term bet |

Put together, that reads as: **a handwriting notebook for students on Android tablets, where imported study material and AI assistance are first-class.**

If that's right, there are two genuinely defensible wedges that the incumbents have left open:

1. **Android tablets, properly.** The best handwriting apps are iPad-first; their Android builds are ports, and stylus latency on Android is where they're weakest. Being the app that *feels best to write on, on Android* is a real, narrow, achievable claim.
2. **Indic-script handwriting.** ML Kit's on-device Digital Ink Recognition covers Devanagari, Bengali, Tamil, Telugu, Gujarati, Kannada, Malayalam, Odia and Punjabi — free, offline. Searchable handwritten notes in Hindi or Marathi is something the incumbents have essentially not bothered to do. This aligns with the name and would be very hard for a competitor to bother matching.

Layered on top, a student-specific AI story writes itself: turn a page of handwritten notes into flashcards, check working on a maths problem, explain a highlighted passage from an imported textbook, generate practice questions.

### The governing principle

> **Pick one thing to be unambiguously best at, and treat every other feature as table stakes to be built to "good enough" and no further.**

The current UI advertises ~20 features. Building all of them to mediocrity is the most likely failure mode of this project. Building 8 of them well plus 1 that nobody else has is a product.

### What the target hardware implies — and it's not what you'd expect

This is the most important *engineering* consequence of the confirmed market, and it's easy to miss.

Price-sensitive Android tablets are not iPad Pros. That has three consequences that push work **earlier** in the plan, not later:

1. **Performance is a core requirement, not a polish item.** The current design repaints every stroke through the Compose pipeline on every frame, and the grid canvas issues tens of thousands of `drawCircle` calls with no viewport culling. On an iPad-class GPU you'd get away with that for a long time. On a budget tablet you will not. **Viewport culling and render caching move from Track D into the foundation**, not the optimisation phase.
2. **Storage efficiency matters.** Serialised stroke geometry should be compact from the first schema version — this reinforces the point in F4 about versioning the format and storing bounding boxes rather than re-deriving them.
3. **Do not assume a good stylus.** Many tablets in this segment ship with no active stylus, or a capacitive one with no pressure data. Samsung's S Pen is excellent; a generic capacitive stylus reports no pressure and no tilt, and plenty of users will draw with a finger. So: pressure-responsive brushes must **degrade gracefully** to a sensible constant width, and palm rejection cannot be built on the assumption that a stylus is present. Design for the finger case and treat stylus data as an enhancement.

Point 3 in particular cuts against the instinct to copy iPad-app design directly.

### Offline-first is not optional

Connectivity in the target market is intermittent. Combined with the fact that this is a note-taking app — where losing work is unforgivable — **the local database is the source of truth and sync is a background reconciliation process, never a dependency**. This is already the plan's shape (Track F is last), but it's now a hard requirement rather than a sequencing convenience.

---

## Part 3 — The reframe: 12 bugs are really 5 missing foundations

This is the most useful thing in this document.

[`09-project-status.md`](09-project-status.md) lists 12 bugs. Fixing them one at a time would be a mistake, because most are not independent defects — they are the *visible symptoms* of a small number of absent architectural pieces.

| Symptom (from doc 09) | Root cause | Genuine standalone bug? |
|---|---|---|
| #1 Rotation destroys everything | **F3** No state/nav architecture | No |
| #2 Grid canvas can't scroll | **F2** No view transform | No |
| #3 Dropdown px/dp anchoring | — | **Yes** |
| #4 Eraser deletes by bounding box | **F1** No stroke geometry | No |
| #5 Undo and erase share one stack | **F5** No command model | No |
| #6 Splash can hang forever | — | **Yes** |
| #7 Auth dead-ends on phones | Scope decision, not a bug | Decision |
| #8 Blank screen if nav desyncs | **F3** Hand-rolled navigation | No |
| #9 Edge-to-edge without insets | — | **Yes** |
| #10 Grid rendering cost | **F2** No viewport culling | No |
| #11 Per-motion-event allocation | **F1** Stroke model | No |
| #12 Dynamic colour overrides brand | Design decision | Decision |

**Three real bugs. Two decisions. Seven symptoms of five foundations.**

So the plan is not "fix 12 things." It is: fix 3 small things, make 2 decisions, and build 5 foundations. That is a much smaller and much more tractable piece of work than the bug list makes it look.

---

## Part 4 — The five foundations

For each: what it is, why it can't be deferred, and what it costs.

---

### F1 — Ink architecture and the stroke model

**The current design:** a `Stroke` holds a mutable Compose `Path`, points are appended with `lineTo`, and every stroke is repainted through the standard Compose draw pipeline on every frame.

**Why this is the highest-stakes decision in the project:**

1. **Latency.** Compose draws through the normal UI pipeline — input → composition → RenderThread → SurfaceFlinger → display. That's typically 2–3 frames of lag. Serious stylus apps target sub-30 ms perceived latency and get there with **front-buffered rendering**, drawing the in-progress stroke directly rather than through the frame pipeline. *You cannot optimise your way to good ink feel inside a normal Compose `Canvas`.* If writing feels laggy, no feature list saves the product.
2. **A `Path` is write-only geometry.** You cannot hit-test it accurately, partially erase it, select it with a lasso, or serialise it. Every one of those is a planned feature, and every one is blocked on replacing `Path` as the source of truth.
3. **Discarded input fidelity.** Android batches MotionEvents; the intermediate samples live in `historical`. The current loop reads `change.position` only and throws the rest away, so fast strokes are visibly faceted. Pressure and tilt are likewise ignored.

**The recommendation: adopt `androidx.ink`, and use the spike to validate rather than to decide.**

> **⚠️ Updated Session 5, 2026-09-08 — the caveats below were checked and two of them were wrong.**
>
> | Was assumed | Verified |
> |---|---|
> | "A relatively young API — verify the release status" | **1.0.0 went stable on 2025-12-17.** Latest alpha line is 1.1.0-alpha07. |
> | "The authoring surface is View-based, so it needs `AndroidView` interop" | **No longer true.** `androidx.ink:ink-authoring-compose` provides an `InProgressStrokes` **composable**. |
> | "It ships native libraries, so expect APK growth" | **Still true.** Budget it; measure per-ABI splits. |
>
> There is also a fourth argument that was not visible when this was written: **`androidx.pdf` builds its annotation layer on `androidx.ink`.** Choosing a hand-rolled engine means hand-writing the PDF annotation layer too. Together with recognition needing ordered stroke input, **three of the four things that make Vahi a product key off this one decision.**
>
> Full reasoning, dependency coordinates and the remaining evidence needed: [D6](22-decisions.md#d6--ink-architecture-adopt-androidxink).

The Ink API exists for exactly this problem and gives you, out of the box: low-latency authoring via front-buffered rendering, motion prediction, a pressure/tilt/velocity-responsive brush model, real stroke *geometry* (with intersection queries — which solves the eraser and unlocks lasso select), and a defined serialisation format (`ink-storage`). Hand-writing equivalents is months of work and you will not do it better.

**If the spike says no**, the fallback is to keep Compose `Canvas` but fix the model properly: your own point-list stroke representation, consume `historical` points, Bézier smoothing between samples, pressure-modulated width, and cache settled strokes into a `GraphicsLayer` so only the active stroke is live-rendered. That gets you most of the *quality* and none of the *latency* win. If you need the latency too, you'd be reaching for `androidx.graphics:graphics-core` (front-buffered rendering) and `androidx.input:input-motionprediction` yourself — at which point you've rebuilt the Ink API badly.

**Cost:** the spike is 2–4 days and produces a *decision plus a latency measurement*, not shipped code. The rewrite that follows is 1–2 weeks.

**Do not skip the measurement.** Run the current implementation and the spike side by side on real hardware with a real stylus, and write the numbers down. This is a go/no-go gate, and it's the only part of this plan I'd call non-negotiable.

---

### F2 — Document model and coordinate space

**This is the foundation most likely to be overlooked, and the most expensive to retrofit.**

Right now, strokes are stored in **screen pixels of whatever device drew them**. There is no document coordinate system and no view transform.

Everything below is blocked by that, and blocked *hard*:

| Blocked feature | Why |
|---|---|
| Zoom and pan | There is no transform to change |
| Grid canvas scrolling (bug #2) | Same |
| Export to PDF/PNG at any resolution | No device-independent geometry to rasterise |
| Opening a note on a different-sized screen | Ink would render at the wrong place and scale |
| PDF annotation | Annotations must anchor to *PDF page* coordinates, not screen coordinates |
| Sync across devices | Same problem, multiplied |
| Viewport culling (bug #10) | Nothing to cull against |

**What's needed** is unglamorous and small: a document coordinate space (page-relative, density-independent), a view transform (scale + translate, later rotate), input inverse-transformed from screen → document before it's stored, and rendering that applies the transform on the way out.

**Now that the paged model is confirmed (D1), this gets concrete:**

- **Page size: A4 (595 × 842 pt).** India uses A4, not US Letter — this matters for printing and for PDF export matching imported material. Store page dimensions per-page anyway, since imported PDF pages will have their own sizes.
- **Unit: points (1/72 inch).** Matches PDF's native unit, which makes PDF import/export a straight mapping instead of a conversion layer with rounding drift.
- **Consequence for the existing paper styles:** the Grid canvas's 4000 dp surface is **definitively replaced** by a page. Don't invest anything further in it.
- **Fix `RuledCanvas` while you're here:** it currently draws **vertical** lines, which contradicts its own description ("Lined pages for writing"). A ruled page for writing needs **horizontal** rules at a defined spacing in document units — which only becomes well-defined once pages exist. This is a genuine design bug that the paged model resolves naturally.

**Why it cannot be deferred:** once strokes are persisted in screen coordinates, **they cannot be migrated**. You will not know what device or zoom level produced them. The data is permanently ambiguous. This creates a hard ordering constraint:

> **F2 must land before any persistence. No exceptions.**

**Cost:** 3–5 days if done before persistence. Effectively a data-loss event if done after.

---

### F3 — State and navigation architecture

**Current:** `remember` everywhere, a hand-rolled `when` over an enum, a manually-managed `selectedNotebookType` side channel, no back stack.

**Fix:** adopt `androidx.navigation:navigation-compose` and move screen state into ViewModels. `BaseViewModel<State>` is already written and waiting for its first subclass.

This single change kills bug #1 (rotation), bug #8 (blank-screen desync), and gives you typed navigation arguments, a real back stack, and saved-state restoration. It's the highest ratio of problems-solved to effort in the whole plan.

**One dependency note:** `BaseViewModel` currently compiles only because `lifecycle-viewmodel` and `kotlinx-coroutines` arrive **transitively**. Declare them explicitly in the version catalog before building on them, or a future dependency bump will break the build in a confusing way.

**Cost:** 3–5 days. Independent of F1 and F2 — this can run in parallel.

---

### F4 — Persistence

Room, with a schema that anticipates what's coming rather than only what's needed now.

Sketch, to be refined against whatever F1 decides:

```
Notebook   id, title, paperStyle, createdAt, updatedAt, isPinned, isLocked, coverColor, sourceType (BLANK | PDF)
Page       id, notebookId, index, width, height, paperStyle override, pdfPageRef?
Stroke     id, pageId, createdAt, brush params, serialised geometry, bounding box
```

Three design points worth getting right the first time:

1. **Store the bounding box as indexed columns.** It's what makes viewport culling and hit-testing a query instead of a full scan. Cheap now, painful to add later.
2. **Make strokes append-only and immutable, with stable UUIDs.** Erase becomes a tombstone, not a delete. This is what makes undo/redo and eventual sync conflict-resolution tractable. Mutable strokes make sync nearly impossible.
3. **Version the stroke serialisation format from day one.** Include a schema version field. You will change this format, and you'll want to migrate rather than discard user data.

This is also the point where `domain/models`, `domain/usecases`, `data/repository` and `data/datasource` stop being empty marker files and start earning their keep — and where DI (Hilt) should land, because retrofitting DI later is tedious.

**Milestone this unlocks:** *draw → close the app → reopen → the ink is still there.* That is the single most important milestone in this project. Everything before it is a demo; everything after it is a product.

**Cost:** 1–2 weeks including notebook CRUD.

---

### F5 — Command/history model

**Current:** erased strokes get pushed onto the same list as undone strokes, so `redo()` un-erases them, and drawing after erasing silently clears the redo stack and makes the erase permanent.

**Fix:** an action stack (`AddStroke`, `EraseStrokes(list)`, `TransformStrokes`, …) rather than a stroke stack. One user action = one undo step.

Small in isolation — but it must land **before** more tools exist, because every new tool multiplies the number of broken interactions with the current shared-stack design. It's cheap now and gets steadily more expensive.

**Cost:** 2–3 days.

---

## Part 5 — The wasted-work rule (what NOT to build yet)

A plan that only says what to build is half a plan. These are things it would be natural to do next and that you should deliberately **not** do, with the reason:

| Don't build yet | Why |
|---|---|
| **Any persistence, before F1 + F2** | The single most important rule here. Saved strokes in screen coordinates cannot be migrated. |
| **The ~20 no-op buttons** (filters, sort, view modes, search, pin, lock, share, delete) | All of them need a repository to act on. Wiring them to the hardcoded 2-item list is work you throw away twice. |
| **More paper styles or drawing tools** | They all rest on F1. Wait for the ink foundation. |
| **Grid rendering optimisation** | The grid may be replaced entirely by the page model. Don't optimise something that may not survive. |
| **A cosmetic split of `HomeScreen.kt`** | It needs splitting — but do it *while* introducing its ViewModel, not before, or you'll refactor the same 982 lines twice. |
| **Phone (Compact) layouts** | Blocked on a scope decision. Building them speculatively could be weeks of wasted layout work. |
| **Backend, accounts, sync** | Highest cost, highest complexity, and it needs a stable local data model first. Deliberately last. |
| **Custom fonts, brand polish, animations** | Real, but they don't change whether the product works. Cheap to add at any time. |
| **Module splitting** (`:core`, `:feature:*`) | Premature at 2,500 lines. Revisit when the data layer lands *and* build times start hurting. |

---

## Part 6 — The sequenced plan

Four tracks. A and B start immediately and in parallel; C is gated on B; D and E follow.

### Track A — Stabilise (≈1 week, start now, zero risk of rework)

Everything here survives every downstream decision, which is why it's safe to do first.

1. **Commit the working tree** in logical chunks (drawing engine / icon change / screen rewrites). ~660 uncommitted lines is an unnecessary risk.
2. **Build the canvas toolbar.** This is the best value-per-line change available: it activates the eraser, undo, redo, clear, colour and width — all *already implemented and unreachable*. Pure UI wiring. Its bindings would change if F1 replaces `CanvasState`, but the UI itself survives.
3. **Fix the three real bugs:** dropdown anchoring (use `Popup` with a `PopupPositionProvider`), splash timeout + error listener, window insets.
4. **Delete dead code:** `NotebookScreen.kt` (extract its layout intent into a design note first — it's the clearest statement of long-term intent in the repo), the orphaned `ic_launcher_foreground.xml`, the unused `res/values/colors.xml`, four unused colour constants.
5. **Replace the phone dead end** (D2). Swap the debug string `"Auth (Mobile)"` for an honest "Vahi is designed for tablets" screen. About an hour, and it stops the app looking broken to anyone who launches it on a phone.

**Milestone M1 — "The canvas is usable."** You can switch tools, undo, and change colour and width.

### Track B — Decide (≈1 week, parallel with A, produces decisions not code)

D1, D2, D3 and D5 are now resolved (see Part 10), so this track has shrunk to the part that genuinely needs measurement:

1. **The ink spike** (F1). Prototype `androidx.ink`, measure latency against the current implementation on **real target hardware** — specifically a mid-range Android tablet, not a flagship, and ideally both with an active stylus and with a finger. Write the numbers down.
2. **Design the document model and coordinate space on paper** (F2) — half a day of thinking that saves weeks. A4 in points, per-page dimensions, view transform.
3. **Sketch the Room schema** (F4), including the stroke serialisation format and its version field.

**Output:** a short decision record, a latency measurement, and a data-model sketch. No shipped features. Resist the urge to skip this week; it's the cheapest week in the plan and it determines the cost of every week after it.

### Track C — The foundation (≈3–4 weeks, gated on B)

1. Navigation library + ViewModels (F3) — *can start before B resolves; it's ink-independent.*
2. Document model + coordinate space (F2).
3. New stroke model per the F1 decision.
4. Room + repository + DI (F4).
5. Notebook CRUD — creating a notebook actually creates one, and the library reflects reality.
6. Split `HomeScreen.kt` as part of introducing its ViewModel.

**Milestone M3 — "Nothing is lost."** Draw, close the app, rotate, reopen — the ink is there. *This is the milestone that turns a demo into a product.*

### Track D — Make the canvas genuinely good (≈3–4 weeks)

Pan/zoom (now trivial, given F2) · command-based undo (F5) · geometry-accurate erase · page navigation and thumbnails · **viewport culling and render caching** · stylus fidelity (historical points, pressure with graceful finger fallback, smoothing, palm rejection that doesn't assume a stylus).

> Given the target hardware, treat viewport culling and render caching as **required for M4**, not as optimisation to be done if there's time.

**Milestone M2 — "It feels good to write."** Latency measured and acceptable on a mid-range tablet; strokes are smooth and pressure-responsive where pressure exists.
**Milestone M4 — "It's a real notebook."** Pages, zoom, a library that reflects reality, and it stays smooth on target hardware.

### Track E — Product (now concrete, given the confirmed thesis)

In order:

1. **PDF import and annotation.** The most-signalled feature in the UI (a create option, a toolbar button, *and* a filter tab), and the one that makes the app useful to a student on day one — textbooks, question papers, lecture slides. The paged model and points-as-units decision (F2) were chosen specifically to make this a straight mapping. Needs a PDF renderer (`PdfRenderer` is in the framework; `androidx.pdf` is worth evaluating) and a document picker.
2. **Search across notebooks.** Titles first — cheap, and the search field already exists.
3. **The differentiator: handwriting recognition, Indic-first.** ML Kit Digital Ink Recognition runs on-device, is free, and covers Devanagari, Bengali, Tamil, Telugu, Gujarati, Kannada, Malayalam, Odia and Punjabi. On-device matters twice over here — it works offline, and it costs nothing per user, which is what makes it viable in a price-sensitive market. This turns search from "titles" into "search your handwritten notes", which is the feature the incumbents have essentially not bothered to build for these scripts.
   > Note the dependency: recognition needs **stroke input data** (ordered points with timing), not rendered paths. This is another reason F1's stroke model has to be right before anything else is built on it.
4. **Student AI features**, once recognised text exists: flashcards from a page of notes, practice questions, explain a highlighted passage from an imported textbook.

**Milestone M5 — "It does something nobody else does."** Search your handwritten Hindi notes and find them.

### Track F — Accounts and sync (deliberately last)

Real auth, sessions, Google/Apple sign-in, cloud sync with conflict resolution. Needs the `INTERNET` permission, which the manifest doesn't currently declare. Don't start this before M4.

**Milestone M6 — "It's multi-device."**

---

## Part 7 — Risk register

| # | Risk | Severity | Mitigation |
|---|---|---|---|
| R1 | **Ink doesn't feel good enough.** If writing lags, nothing else matters. | **Critical** | Measure in Track B, on real hardware, with a real stylus. Set an explicit target (<30 ms perceived). Treat it as a go/no-go gate. |
| R2 | **Persisting on the wrong stroke model** forces a rewrite or data loss. | **Critical** | The F2-before-F4 ordering constraint. Do not relax it under schedule pressure. |
| R3 | **Scope creep.** The UI advertises ~20 features and creates pressure to fill them all. | High | The wasted-work rule in Part 5. Accept a smaller, sharper v1. |
| R4 | **No differentiation.** Ships as a worse GoodNotes. | High | Answer Part 2 before Track E. |
| R5 | **Android stylus fragmentation.** S Pen, Xiaomi, Lenovo, generic capacitive, and plenty of finger-only users — all report different input data. | **High** (raised, given the confirmed market) | Define a small device test matrix early, weighted toward mid-range hardware. Design for the no-pressure/finger case and treat stylus data as an enhancement, not a baseline. |
| R6 | **`androidx.ink` maturity.** Younger API, less battle-tested. | Medium | That's what the spike is for. Keep the fallback path in mind. |
| R7 | **Performance on budget hardware.** The current render approach will not survive a full page of ink on a mid-range tablet. | **High** (raised) | Measure on target hardware, not a flagship or emulator. Culling and render caching promoted into M4. |
| R8 | **Bandwidth.** I don't know if this is one person or a team, full-time or evenings. | Unknown | Tell me, and I'll re-cut the timeline honestly. The week estimates assume roughly one focused full-time developer. |

---

## Part 8 — Testing strategy (proportionate, not exhaustive)

Don't write tests for everything. Write them where the bug cost is high and the test cost is low:

| Target | Value | Why |
|---|---|---|
| **Stroke/canvas state logic** | **High** | Pure logic, no Android deps, genuinely tricky edge cases (undo/redo/erase interaction). Start here — it's the best test in the project. |
| **Coordinate transform math** | **High** | Pure, easy, and a bug here corrupts user data silently. |
| **Repository + DAO** | High | Room has good test support; data-layer bugs are the expensive kind. |
| **Compose UI navigation smoke test** | Medium | Tooling (`ui-test-junit4`) is already on the classpath and unused. |
| **Screenshot tests** | Medium | Valuable for a design-heavy app, but heavy to set up. Defer to post-M4. |
| **Stylus feel** | — | **Cannot be automated.** Needs a written manual device checklist. Accept this. |

---

## Part 9 — Tech debt: dedicated work vs. boy-scout rule

Not all debt deserves a sprint.

**Do as you touch each file** (these compound linearly with every new screen, so paying as you go is cheapest):
- Extract hardcoded strings to `strings.xml` — currently *all* UI copy is hardcoded Kotlin
- Add content descriptions — accessibility is currently effectively zero
- Move inline `Color(0xFF…)` literals into `Color.kt` (its own KDoc already mandates this)
- Use `MaterialTheme.typography.*` instead of raw `fontSize`

**Worth dedicated time, but not yet:**
- Replacing emoji/text glyph icons (🐦 🔍 ⊞ ≡ ⋮⋮ □ ↓ ←) with real vector assets — they render inconsistently and are invisible to screen readers
- Unifying the three near-identical dropdown menus into one parameterised composable
- Collapsing the duplicate `NotebookType` / `CanvasType` enums
- Dependency upgrades (Compose BOM 2024.09.00 against `compileSdk 36` is a wide gap)
- `kotlinOptions` → `compilerOptions` DSL migration
- Adding `@Preview` functions (`ui-tooling-preview` is a declared dependency with zero previews)

**Before any public release:**
- `isMinifyEnabled = true`, `isShrinkResources = true`, real ProGuard rules, a signing config
- Decide the `dynamicColor` question (D4 below)

---

## Part 10 — Decision log

> **⛔ Superseded by [`22-decisions.md`](22-decisions.md) (Session 5, 2026-09-08).**
>
> That file is now the authoritative record: 22 decisions with full context, rejected alternatives, consequences, and — for anything still provisional — the specific evidence that would overturn it. The table below is retained only as a historical snapshot of what was settled by the end of Session 4. **Do not edit it; edit `22-decisions.md`.**
>
> Since this snapshot: **D4 resolved** (`dynamicColor = false`, implemented) · **D6 upgraded** from open to a strong recommendation on verified evidence · **D7 reframed** as engineering-days rather than a bandwidth question · **D8–D16 and V1–V5 added.**

### ✅ Resolved

| # | Decision | Resolution | Consequence |
|---|---|---|---|
| **D1** | Pages or infinite canvas? | **Paged.** A4 (595 × 842 pt), units in points, per-page dimensions stored. | Grid's 4000 dp surface is retired. `RuledCanvas` switches to horizontal rules. PDF import becomes a straight coordinate mapping. Paper style becomes a property of a page. Infinite-canvas mode stays possible later but is not built. |
| **D2** | Are phones in scope? | **Tablet-only v1**, but replace the dead end. | One hour of Track A work: an honest "designed for tablets" screen instead of the debug string. No Compact layouts built. Weeks of speculative layout work avoided. |
| **D3** | What is Vahi best at? | **Confirmed:** student notebook, Android tablets, Indian market. | Track E is now concrete: PDF annotation → search → **Indic handwriting recognition** as the differentiator → student AI features. Also raises R5 and R7 and promotes performance work into M4. |
| **D5** | Product or portfolio project? | **A real product to ship.** | Full foundations F1–F5. Release hardening (minify, shrink resources, signing config, version management) becomes real work before launch, not optional. |

### 🔲 Still open

| # | Decision | Recommendation | When it's needed |
|---|---|---|---|
| **D4** | Brand palette, or Material You? `VahiTheme(dynamicColor = true)` means the hand-authored Vahi schemes are silently unused on Android 12+. | **`dynamicColor = false`.** A notebook app benefits from a stable, recognisable identity, and both schemes are already written. Two-line change — just make it deliberately rather than by default. | Any time. Low stakes, but do it consciously. |
| **D6** | **Ink architecture: `androidx.ink` or hand-rolled?** | Decide from the spike's latency measurement, not from theory. | End of Track B. This is the one remaining decision that genuinely gates the foundation build. |
| **D7** | Bandwidth — solo or team, full-time or evenings? | — | Affects only the schedule, not the plan. Week estimates currently assume ~one focused full-time developer. |

---

## Summary in one page

**Settled:** a real product · paged A4 notebooks · tablet-only v1 · student notebook for Android tablets, Indian market, with Indic handwriting recognition as the intended differentiator.

- **Three real bugs, one open decision, five foundations** — not twelve bugs.
- **Foundations:** F1 ink architecture · F2 document/coordinate model · F3 state & navigation · F4 persistence · F5 command history.
- **Hard ordering constraint:** F1 and F2 must land before F4, or persisted data is unmigratable. This is the rule not to break under schedule pressure.
- **Start now, zero rework risk:** commit the tree, build the toolbar, fix three bugs, delete dead code, replace the phone dead end.
- **Spend one week measuring** — the ink spike on mid-range hardware is the last thing gating the foundation build (D6).
- **The milestone that matters:** *draw → close → reopen → the ink is still there.* Everything before it is a demo.
- **Don't build:** the 20 no-op buttons, more tools, or any backend — until the foundations are in.
- **The target hardware changes the engineering.** Budget tablets mean performance is a requirement not a polish item, and you cannot assume a pressure-sensitive stylus is present.
- **The differentiator has a dependency:** handwriting recognition needs ordered stroke *input* data, not rendered paths — one more reason to get F1's stroke model right first.
