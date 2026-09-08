# Project Log

Append-only record of what happened in each working session. **Newest entry at the top.**

Purpose: if a session ends abruptly or a usage limit is hit, the next session reads the top entry and knows exactly where things stand. See [`00-START-HERE.md`](00-START-HERE.md) §7 for the maintenance protocol.

**Entry format:** date · what was done · what changed · decisions made · state at end · next action.

---

## 2026-09-08 — Session 7: Implement D10 (kill the auth wall), partial emoji fix, doc accuracy sweep

**Requested:** "continue with the work, tell me if everything is done or not, if anything remains then complete it" — a fresh-context continuation after a long prior session. Also granted push permission for the private repo ("the vahi repo on my account is private so u can push there").

### First, an honest re-audit — the backlog was not actually "done"

A prior response in the same overall thread had implied the work was complete once persistence/Hilt/the self-audit fix landed. Re-reading `21-backlog.md` cold at the start of this session showed that framing was too generous: **E4 through E7 (14+7+6+7 = 34 items, the majority of the v1.0 product scope) are still entirely `⬜ Not started`** — export, settings, onboarding, accessibility pass, page model, autosave (the "milestone that matters"), and more. What *was* actually complete was the specific increment from the prior pass (Room/Hilt/ViewModel + the Color-in-domain fix), not the product. Worth recording as a process note: **re-derive backlog status from the file itself before reporting completion, don't trust a summary of a prior claim.**

### What's actually blocked vs. what wasn't

Most of the large remaining work (`VAHI-101` ink-latency benchmark, and everything downstream of it — `VAHI-102` `InkEngine`, `VAHI-110` wired, `VAHI-111`–`115`, page/stroke persistence, autosave) is genuinely blocked on physical Android tablet hardware, which this environment does not have and the project's own rules (`CLAUDE.md` rule 3, the ink-architecture failure-mode table) explicitly forbid working around by wiring the coordinate model or persistence ahead of that measurement. That blocker is real and unchanged.

But not everything was blocked. Found and did three small, genuinely unblocked, correctly-sequenced items:

**`VAHI-006` + `VAHI-205` (the D10 half) — done.** `AppRoot.kt`'s `Splash` destination now navigates straight to `Library` on timeout, never to `Auth` — implementing [D10](22-decisions.md#d10--no-auth-wall-account-is-optional-and-requested-in-context) (✅ Accepted since Session 5, never implemented until now). The tablet-only fallback moved from `AuthScreen.kt`'s `AuthFallbackLayout` to a new `TabletOnlyFallback()` at the app-shell level in `AppRoot.kt`, gated once via `windowClass()` above the whole `NavHost` — necessary, not cosmetic: `HomeScreen.kt` has no Compact fallback of its own, so simply removing Auth from the path without this move would have left phone users looking at raw tablet UI with no protective message at all, a regression from today's behaviour. `AuthScreen.kt` simplified to drop its now-redundant size branch; `Auth` stays declared in the nav graph, unreached, rather than deleted — real UI work, kept for `VAHI-406`'s future Settings entry point. **`D11`'s half (the video splash / `androidx.media3` removal) was deliberately not touched** — that item is explicitly owner-gated in the backlog, and unilaterally deciding a brand question wasn't in scope.

**`VAHI-204` — partial, 2 of 12 glyphs.** Read `ToolbarGlyphToggle` in `DrawingToolbar.kt` closely before touching anything, and found ten of the twelve catalogued emoji/glyphs (`⊞ ≡ ⋮⋮ 🧮 💻 📓 □ ↓ ← G` plus the drawing toolbar's `✒ ⌫ ↶ ↷`) already carry correct `semantics { contentDescription = label }` — a screen reader already announces "Pen tool," not the glyph. Only two — `HomeScreen.kt`'s profile avatar (`🐦`) and search-bar icon (`🔍`) — had zero semantics at all, a genuine, live accessibility bug. Fixed those two with `Icons.Default.Person`/`Icons.Default.Search` (already used elsewhere in the codebase, so no new dependency), added a real `contentDescription`, and bumped the profile button's touch target 44dp → 48dp while in there. **Deliberately left the other ten** — iconography is a Figma-owned concern per [V4](22-decisions.md#v4--figma-is-the-visual-source-of-truth-the-docs-describe-behaviour), `VAHI-004` (reading the Figma) is still owner-blocked, several of the remaining glyphs (`⊞ ≡ □`) don't exist in the core Material icon set and would need a new dependency or hand-drawn vectors, and doing that work twice once real iconography exists is waste this project's own docs warn against elsewhere.

**Docs:** `21-backlog.md` (`VAHI-006` done, `VAHI-204`/`VAHI-205` split/partial, epic summary counts corrected), `22-decisions.md` (D10 marked implemented), `00-START-HERE.md`, `README.md`, `09-project-status.md` (five stale lines corrected — the auth-flow description, the emoji bug entry, the phone-dead-end bug writeup, and a flatly wrong "Tests: None beyond templates" line sitting right next to what I was already editing — 51 tests have existed since Session 6).

### State at end

Build green (`:app:assembleDebug` + `:app:testDebugUnitTest`, `BUILD SUCCESSFUL`, 51 tests). Working tree was clean at session start (16 commits ahead of `origin/main`, nothing pushed) — this session's changes are additional commits on top.

### Next action

Unchanged and binding: `VAHI-008` → `VAHI-101` — a real mid-range Android tablet, needed to measure ink latency and settle D6. Everything sized-Epic (E4–E7, and E1's remaining ink items) either depends on that measurement or is real, uncommenced product work — there is no more "quick, unblocked" work of the kind done this session; what's left is genuinely large.

---

## 2026-09-08 — Session 6, planning pass: All-device expansion plan (D17 resolved)

**Requested:** plan how Vahi becomes available on all devices — phones, tablets, Android and iOS — as a phase *after* the current v1 work, explicitly **without implementing any of it now**. Also asked why the Hilt work hadn't been done.

**On the Hilt question:** it had been — committed in `78fb7cf` in the previous pass, as a complete injection graph (`@HiltAndroidApp` → `@AndroidEntryPoint` → `DatabaseModule`/`RepositoryModule` → `@HiltViewModel` → `hiltViewModel()` in `HomeScreen`). It was bundled with Room deliberately rather than done separately, since DI with nothing to inject is empty scaffolding. Nothing was needed from the owner. A stale entry in the working task list said otherwise and was corrected — worth noting as a process failure, not just a mix-up: the task list disagreed with the committed code and the code was right.

### Done — planning only, zero code changed

**New: [`23-cross-platform-plan.md`](23-cross-platform-plan.md)** — the full all-device plan. Its most useful contribution is a reframe: **"works everywhere" is two independent problems, not one.**

- **Axis A — form factor** (phone ⇄ tablet ⇄ foldable): pure responsive-layout work, entirely inside the current Android codebase, no KMP. **~14 ed.**
- **Axis B — platform** (Android ⇄ iOS): KMP restructuring, a second ink engine, DI migration, second store pipeline. **~45 ed**, plus a standing **+30–40% on all later feature work.**

They're independent, and **A must come first** — otherwise iPhone layouts get built twice. Axis A alone is a complete, defensible "works on all Android devices" outcome at ~25% of the cost, and likely reaches more of the target student (most tablet owners also own a phone).

**A correction to my own earlier analysis, recorded prominently.** The first draft of D17 (written earlier the same day) claimed iOS would *lose the product's differentiator*, on the assumption that ML Kit Digital Ink Recognition was Android-only. **That was wrong.** ML Kit Digital Ink runs on iOS too — fully offline, same 300+ languages and 25+ writing systems, every Indic script from D9, with an official Google iOS quickstart. Porting it is a cinterop binding (~3 ed), not a reinvention. The corrected picture: **the differentiator ports; the ink *engine* does not.** This made iOS look materially worse than it is, and the error is called out explicitly in D17 rather than quietly amended.

**Other findings verified this pass:** Compose Multiplatform is at **1.12.0** (Sept 2026), iOS stable since 1.8.0 with Netflix/Cash App in production · Room has genuine KMP support via `androidx.sqlite` · `navigation-compose` ports under a different Maven coordinate · **Hilt has no KMP path — Koin is the replacement** · `androidx.ink` remains Android-only, so iOS needs a second engine on PencilKit via CMP's first-class `UIKitView` interop.

**One finding that changes work happening now — the only one.** `VAHI-102` (the Android ink engine, not yet started) gained a hard acceptance criterion: **the `.ink` payload must be engine-neutral geometry, not `androidx.ink`'s native `ink-storage` blob.** If Android writes notes in an Android-only format, a future iOS engine cannot read them and there is **no migration path** — the geometry is locked in. Same class of irreversible, cheap-to-prevent mistake as the R1 screen-pixel-coordinates rule. Costs ~1 ed at `VAHI-102`; unfixable afterwards. The `.ink` header already carries an `engineId` field anticipating multiple engines — that foresight only pays off if the payload is portable too.

**Also decided:** do **not** use a single shared Compose `Canvas` ink engine across both platforms. CMP renders via Skia→Metal on iOS with measurable added latency, and a plain Compose `Canvas` already can't hit the latency bar on Android. Sharing there would sacrifice the core quality claim on both platforms to save integration work on one. Best native engine per platform, behind the existing `InkEngine` interface.

**Docs updated:** `22-decisions.md` (D17 rewritten and **Accepted**; D2 re-scoped as v1-only rather than permanent), `21-backlog.md` (new epics **E8** ~14 ed and **E9** ~45 ed, 19 new items; `VAHI-102` acceptance criterion), `14-product-spec.md` (roadmap gains v1.5 and v2.x), `19-quality-bar.md` (device matrix), `00-START-HERE.md`, `README.md`.

### State at end

**No code changed. Build untouched and still green** (51 tests). v1 scope is unchanged and remains the active work — this pass only ensured the expansion is planned rather than improvised, and that one decision inside v1's own scope (`VAHI-102`'s payload format) doesn't quietly foreclose it.

### Next action

Unchanged: `VAHI-008` → `VAHI-101` — a real mid-range Android tablet, to measure ink latency and settle D6. Everything in E8/E9 is gated on v1 shipping first and should not start earlier.

---

## 2026-09-08 — Session 6, extended pass: Persistence foundation (Room + Hilt) and the iOS/cross-platform question

**Requested:** "do whatever is left to complete the project, then do the Hilt part, tell me if I need to do anything, then tell me when done" — followed, mid-work, by a request to research and report on iOS and cross-device (phone/iPad/tablet) support.

### Done

**A real persistence foundation, scoped deliberately to notebook metadata only:**

- **`core/model/PaperStyle.kt`** — relocated `CanvasType` out of `ui/screens/CanvasScaffold.kt` (renamed to match the product's own vocabulary) so Room entities and the domain layer don't depend on a UI-layer type. Updated every call site (`HomeScreen.kt`, `AppRoot.kt`).
- **`domain/model/Notebook.kt`**, **`domain/repository/NotebookRepository.kt`** — the domain layer, trimmed from the full target schema in `17-data-spec.md` §2.1/§9 to exactly what the library screen needed. Renamed the pre-existing empty `domain/models`/`domain/usecases` marker packages to singular `domain/model`/`domain/usecase`, matching `18-architecture-target.md`'s target shape.
- **`data/database/`** (`NotebookEntity`, `Converters`, `NotebookDao`, `VahiDatabase`) and **`data/repository/RoomNotebookRepository.kt`** — Room, with the enum-persisted-as-name rule and soft-delete tombstones from `17-data-spec.md` followed from the first row written.
- **`di/DatabaseModule.kt`, `di/RepositoryModule.kt`, `VahiApplication.kt`** — Hilt, built *with* a real repository to inject rather than as empty scaffolding (`VAHI-202`).
- **`ui/feature/library/LibraryViewModel.kt`** — `BaseViewModel`'s first real subclass, after sitting unused since before Session 1.
- **`HomeScreen.kt` rewired** to real data: create persists and opens the new notebook; the grid shows real notebooks; **tapping an existing card now opens it — a capability that did not exist at all before**, on any data; delete is soft with an "Undo" snackbar; pin/unpin works and "Pinned notes" actually filters; "Edited Xh ago" relative time replaces the creation-date string.
- **`AppRoot.kt`** — the `Editor` route now carries `notebookId` alongside `canvasType`, threaded through to `CanvasScreen` for future use (ink still isn't persisted, so nothing reads it yet — documented as such rather than silently unused).

**9 new Robolectric tests** (`RoomNotebookRepositoryTest.kt`) against a real in-memory database — ordering, the tombstone filter, restore, pin/unpin, ARGB colour round-trip. **51 tests total, all passing.**

### A real build problem, worth recording in detail

Adding Room + KSP produced a reproducible `java.lang.AbstractMethodError` (`FieldBundle$$serializer` / `typeParametersSerializers()`) the moment KSP re-read an already-exported schema JSON — a genuine upstream incompatibility between Room 2.8.4's bundled schema-bundle classes and this toolchain's `kotlinx.serialization`, not a resolvable local version clash (confirmed by forcing `kotlinx-serialization-core` to both 1.7.3 and 1.8.1, and by killing a long-lived `KotlinCompileDaemon` process that survives `./gradlew --stop` — neither fixed it). It's deterministic on a full `clean` rebuild with a same-version schema file already present, and does **not** affect normal incremental builds. Disabled `exportSchema` for now (zero functional cost at schema v1 with no migrations to validate against); flagged clearly in `app/build.gradle.kts`, `VahiDatabase.kt`, and `21-backlog.md` `VAHI-301` to re-enable before any real schema v2 ships. Also hit, and worked around: `androidx.hilt:hilt-navigation-compose:1.4.0` requires compileSdk 37/AGP 9.1+ — pinned to 1.3.0, which doesn't.

**Verified stable, not just "passed once":** two independent `./gradlew clean` builds from freshly-killed Gradle *and* Kotlin daemons both succeeded.

### The iOS / cross-platform question — researched, not implemented

Mid-session the owner asked whether Vahi should also work on iOS and on phones/iPads, not just Android tablets. Researched thoroughly rather than answered from assumption; written up in full as **decision D17** in `22-decisions.md` (status: **Open** — genuinely the owner's call). Summary of what changes the answer:

- Kotlin Multiplatform + Compose Multiplatform (stable since 1.8.0, May 2025) is the right path *if* this is pursued — not Flutter, not a rewrite. Much of what was built this session (`core/model/`, `domain/`, Room via `androidx.sqlite`, `kotlinx.serialization`) already has real multiplatform support.
- Hilt does not — Koin is the multiplatform-standard replacement. A real migration, not a recompile.
- **The decisive finding: `androidx.ink` — the library D6 recommends adopting — is confirmed Android-only, with no iOS path.** The ink engine, the single most complex and differentiating part of the app, would need a **second, fully separate implementation** on Apple's PencilKit. The `InkEngine` interface already designed in `18-architecture-target.md` happens to be the right shape to contain this, but that doesn't make building a second engine cheap.
- This also directly reopens D2 (tablet-only) and D3 (Android-tablet, Indic-recognition thesis) — both confirmed decisions. Verified market/latency data (Apple PencilKit: consistent 22–28 ms; real-world Android stylus testing: 41–69 ms with high variance; India tablet share: Samsung ~31% on affordability, Apple owns the premium segment) reinforces *why* D3 pointed away from iPad in the first place — that's where GoodNotes and Notability already are.
- Honest cost estimate if pursued: roughly doubles remaining engineering effort, not a fixed increment on top of it.

No code changed as a result of this question — it was research and analysis only, delivered to the owner directly in conversation and recorded as D17.

### State at end

Build green, 51 tests passing, verified stable across repeated clean builds. 12 commits total across both Session 6 passes. The app now has its first real end-to-end persisted loop (create → survives restart → reopen → delete-with-undo) for notebooks; ink itself still does not persist, by design, pending the device-gated ink-engine decision.

### Next action

Unchanged: `VAHI-008` → `VAHI-101` (a real mid-range Android tablet, to measure ink latency and settle D6) is the binding blocker for further ink-engine work. Separately, and not blocking anything: **D17 needs the owner's answer** before any cross-platform restructuring should start — ideally before more Android-only ink-engine work is sunk, since that's the cheapest time to change course if the answer is "yes, cross-platform from the start."

---

## 2026-09-08 — Session 6: First implementation pass (VAHI-001–003, 005, 110, 120, 201-partial)

**Requested:** "start implementation, think logically and do everything properly." Explicit go-ahead to write code, following the plan and backlog from Session 5.

### Approach

Asked first whether to checkpoint the large uncommitted tree before touching any of the same files — the standing git-safety rule is never to commit without being asked. User chose "commit in logical chunks." Worked the backlog in dependency order, verifying `:app:assembleDebug` and `:app:testDebugUnitTest` after every change, and updating the affected as-built docs in the same commit as the code, per `00-START-HERE.md` §7. **No work in this session touched a real device** — everything was verified by compilation, unit tests, and reading the code; on-device behaviour (touch response, TalkBack, visual layout) remains unverified, as it has been every session so far.

### Done

**VAHI-001 — Committed the working tree**, six logical commits: Media3 dependency (had to land first — `SplashScreen.kt` needs it to compile) · launcher icon PNGs · the ink engine + canvas host · Session 4's UX/bug fixes · IDE metadata · the full documentation set. Repo went from one commit + a large uncommitted diff to a readable, buildable history.

**VAHI-002 — Upgraded the dependency baseline.** Kotlin 2.0.21→2.1.20, core-ktx 1.10.1→1.15.0, lifecycle 2.6.1→2.8.7, activity-compose 1.8.0→1.9.3, Compose BOM 2024.09.00→2025.01.01. AGP deliberately stays at 8.13.2 — AGP 9 is a separate breaking-change migration, not required to unblock Room/Hilt/Navigation, and not something to fold silently into a routine catalog refresh without on-device verification available. Migrated the deprecated `kotlinOptions` DSL to `compilerOptions`. Declared `lifecycle-viewmodel-compose`/`lifecycle-runtime-compose`/`kotlinx-coroutines-android` explicitly (`BaseViewModel` previously compiled only via transitive deps). Added Robolectric — needed almost immediately, see below.

**VAHI-005 — Configured backup rules explicitly**, excluding the future database and per-notebook ink files from Auto Backup on both API 26-30 and 31+, ahead of persistence landing (decision D13).

**VAHI-003 — Fixed four shipped tool-rail defects, including the most severe bug found this week.** `CanvasState.clearAll()` was destroying the redo stack alongside the strokes, making a mis-tap on a 48dp Clear button — sitting directly below Undo/Redo — unrecoverable data loss in a notes app (bug #13). Fixed with a snapshot-and-restore mechanism (`undoClear()`) and a Snackbar "Undo" action in `CanvasScaffold.kt`. Also fixed: 40dp touch targets that should have been 48dp (an inner `.padding(4.dp)` was shrinking the clickable region), zero content descriptions on any control, hardcoded hex colours throughout (now `MaterialTheme.colorScheme.*` plus five new named ink-palette constants in `Color.kt`), and a width-preview that reinterpreted a canvas-pixel value as a dp value. 6 new Robolectric tests. **This was the point Robolectric earned its place in the catalog** — `Stroke.path` is `android.graphics.Path`-backed, so `CanvasState` can't be tested on a plain JVM.

**VAHI-201 (partial) — Replaced hand-rolled navigation with `navigation-compose`.** `AppRoot.kt` was a `when` over an `AppDestination` enum with a manually-synchronised `selectedNotebookType` side channel — if that was ever null while the destination expected it, the screen silently rendered nothing (bug #8). Replaced with four type-safe, `kotlinx.serialization`-backed routes; the `Editor` route carries `canvasType: CanvasType` directly, so that failure mode has nowhere left to occur. This also removed the reason for `NotebookCanvasScreen.kt` to exist — it was a thin adapter whose only job was mapping an identical `NotebookType` enum onto `CanvasType` 1:1. Deleted both `AppDestination.kt` and `NotebookCanvasScreen.kt`. **Explicitly partial:** `CanvasState` is still `remember`-scoped inside `CanvasScreen`, so rotating the device on the canvas still drops every stroke — bug #1's harder half is unchanged and needs `VAHI-110`'s types actually wired in plus persistence.

**VAHI-110 (core types) — Added `core/model/`**, the coordinate model from `16-canvas-spec.md` §2: `ScreenPoint`/`DocumentPoint`/`PagePoint`/`PageSize` as **distinct types** (not a shared `Offset` distinguished only by naming — closes off the exact class of unit-mixing bug just fixed in VAHI-003), `ViewTransform` (scale + pan via the real `androidx.compose.ui.unit.Density`), and `PageLayout` (the page-strip model, precomputed offsets, width-centring for mixed page sizes). This is the first code built directly onto the target package shape in `18-architecture-target.md` §2. 24 unit tests, all plain JVM — no Robolectric needed, confirming these types have zero Android/native dependency, unlike `Stroke`. **Explicitly partial:** nothing in the app uses this model yet; wiring it into the canvas is separate, later work, deliberately not bundled in here since it would mean touching the ink engine before the `androidx.ink` decision (D6) is settled.

**VAHI-007/120 — Fixed the undo/erase stack conflation (bug #5).** Erased strokes were pushed onto the same list used for stroke-add redo, so `redo()` could un-erase a stroke instead of replaying whatever was actually undone, and drawing after an erase silently made it permanent. Replaced with a private `StrokeCommand` sealed type (`Add`, `Erase`) and separate undo/redo stacks, plus a `beginErase()`/`finishErase()` gesture lifecycle so an entire erase drag — many pointer samples — becomes one undo step, not one per sample. `Clear` deliberately stays on VAHI-003's separate mechanism rather than being folded into this stack (documented reason: a stale snackbar could otherwise undo the wrong action). 11 new tests.

**Documentation:** every doc touched by the above updated in the same commit as the code — `09-project-status.md` (bugs #5, #8, #13, #15, #16, #18, #19 all now marked fixed; bug #1 marked half-fixed with the remaining half explained), `08-file-reference.md`, `05-drawing-engine.md`, `02-architecture.md` (Navigation and State management sections rewritten), `01-overview.md`, `03-app-flow.md`, `04-screens.md`, `18-architecture-target.md`, `21-backlog.md` (7 items updated with honest "done vs. not done" scope notes, critical path diagram updated). Along the way, corrected several pieces of doc drift that **predated this session** — `03-app-flow.md` still claimed "there is no toolbar on the canvas," stale since Session 4; `08-file-reference.md` still described `undo`/`redo`/`clearAll` as "uncalled," also stale since Session 4.

### State at end

Build green throughout: `:app:assembleDebug` and `:app:testDebugUnitTest` (42 tests, all passing) succeeded after every commit. Working tree clean, 9 new commits on `main`. Bugs fixed this session: **#5, #8, #13, #15, #16, #18, #19** (7 of the 20 catalogued). Bug #1 (rotation) half-fixed. Bug #14 (palm rejection — still the most severe open issue) and bug #17 (stroke discarded on back) are untouched.

### Explicitly not done, and why

- **`VAHI-202` (Hilt)** — deliberately skipped. Nothing to inject yet (no repository exists); adding DI scaffolding with nothing using it would be exactly the kind of premature infrastructure the plan's own "wasted-work rule" warns against. Pick it up when `VAHI-301` (Room) lands.
- **On-device verification of anything in this session** — no GUI in this environment, as in every prior session. The tool-rail fixes, the navigation rewrite's rotation behaviour, and all four "fixed" bugs are verified by compilation, unit tests, and code reading only.
- **`VAHI-101` (ink latency benchmark)** — needs a physical mid-range Android tablet. This is now the most important blocker: everything device-independent that could reasonably precede it is done.

### Next action

Get a real mid-range Android tablet (`VAHI-008`) and run the ink latency benchmark (`VAHI-101`), which settles decision D6 (`androidx.ink` vs. the hand-rolled fallback) and unblocks `VAHI-102`. In parallel, on-device verification of everything shipped this session — particularly bug #13's fix (the Undo snackbar) and bug #16's fix (TalkBack actually reading the tool rail correctly) — would be high-value and needs no further planning, just a device.

---

## 2026-09-08 — Session 5: Detailed planning, spec build-out, source re-audit

**Requested:** plan everything in detail — read the existing documentation and enhance it, plan the UI and how to make everything user-friendly, find the flaws and solve them, refine everything, and produce proper documentation and files for all of it. Explicitly: implementation comes later, and documentation is to be kept updated as it proceeds. Mid-session the owner shared an existing Figma design.

### Done

**Nine new specification documents (~5,400 lines):**

| Doc | What it adds that didn't exist |
|---|---|
| `14-product-spec.md` | The missing product layer — v1 scope in/out with reasons, three concrete personas and how their needs conflict, jobs-to-be-done ranked by frequency, monetisation design, ten v1 exit criteria, competitive position |
| `15-ux-blueprint.md` | The buildable UX — 20-item screen inventory, navigation graph and route table, ASCII wireframes, per-screen loading/empty/error matrix, gesture set, keyboard shortcuts, multi-window behaviour, handedness, microcopy rules |
| `16-canvas-spec.md` | Coordinate spaces and transform math, the pointer arbitration **state machine**, four-tier palm rejection, brush degradation for no-stylus hardware, the layered render pipeline with culling, command model, autosave triggers |
| `17-data-spec.md` | Five data rules, Room entities with indexed bounding boxes, the versioned `.ink` binary format, tombstones and compaction, migration policy, local-profile identity for a future sign-in, repository contracts |
| `18-architecture-target.md` | Target package structure, layer contracts, state/event patterns (including why the editor is a deliberate exception), DI, threading policy, the dependency list, a 10-step migration order |
| `19-quality-bar.md` | The numbers — three reference device tiers, latency/fps/memory/size/battery budgets, 11 accessibility requirements, testing strategy, the definition of done, the re-scored risk register |
| `20-release-plan.md` | Build config, ProGuard needs, signing, permission timeline, privacy policy and Data Safety, crash reporting, Play listing, staged rollout, 10 pre-launch blockers (~6 ed) |
| `21-backlog.md` | **The execution spine** — 75 numbered items (`VAHI-###`) across 8 epics, sized in engineering-days, dependency-ordered, with acceptance criteria. ~88 ed to v1.0. |
| `22-decisions.md` | **Authoritative ADR log** — D1–D16 and V1–V5, each with reasoning, rejected alternatives, consequences and what would overturn it |

**Also:** `CLAUDE.md` at the repo root — operating guide for future AI sessions.

**Existing docs refreshed:** `00-START-HERE.md` (rewritten), `README.md`, `05-drawing-engine.md`, `08-file-reference.md`, `09-project-status.md`, `11-forward-plan.md`, `12-ux-plan.md`, `13-visual-design-system.md`.

### Key findings

**1. Three external dependencies verified — and the earlier plan was wrong about two things.**

| Earlier assumption | Verified 2026-09-08 |
|---|---|
| `androidx.ink` is "a relatively young API — verify status yourself" | **1.0.0 stable since 2025-12-17.** Latest alpha 1.1.0-alpha07. |
| "The authoring surface is View-based, so it needs `AndroidView` interop" | **False.** `ink-authoring-compose` provides an `InProgressStrokes` **composable**. |
| PDF annotation would need a renderer built from scratch | **`androidx.pdf` 1.0.0-beta01 builds its annotation layer on `androidx.ink`** — pen, highlighter, eraser, forms, OCR. `minSdk 28`, Fragment-based. |
| ML Kit Indic coverage assumed from recollection | **Confirmed:** fully on-device, all nine target scripts with BCP-47 identifiers, packs downloaded on demand. |

**Consequence: D6 upgraded from "the one open decision gating everything" to a strong recommendation.** Three of the four things that make Vahi a product — ink feel, PDF annotation, Indic recognition — key off the same choice. The latency benchmark still has to run (`VAHI-101`), but the burden of proof has moved to *not* adopting.

**2. Eight further defects found by re-reading the source (#13–#20), four of them in Session 4's own toolbar.**

- **#14 (critical) — a resting palm draws instead of the stylus.** `event.changes.firstOrNull()` takes the first pointer and ignores the rest. A palm typically lands *before* the stylus tip, so the palm becomes pointer 1. Earlier docs recorded "no palm rejection"; the truth is stronger — the current handling **actively selects the wrong pointer**. On a tablet this is not an edge case, it is every stroke.
- **#13 (high) — Clear is unrecoverable and one tap away.** `clearAll()` empties the redo stack alongside the stroke list, and `DrawingToolbar` wires it to a 48 dp button directly below Undo. A data-loss bug in shipped code, in a note-taking app.
- **#15/#16/#18** — the tool rail's touch targets are 40 dp not 48 (modifier ordering), it has no content descriptions and only hardcoded colours (so it will be invisible on dark paper), and the width preview renders a pixel value as dp.
- **#17** — leaving the editor mid-stroke silently discards the stroke.
- **#19 (latent-high)** — `allowBackup="true"` with template rules where every rule is commented out. Harmless today; the moment Room lands, the whole database becomes Auto-Backup eligible with a silent 25 MB quota and a restore that can overwrite newer notes with older.
- **#20** — the eraser skips strokes on a fast drag (no interpolation between samples).
- **#12 verified as fixed** in Session 4; the doc was stale.

**3. Structural gaps closed.** The prior doc set explained *why* and *what order*, but had no schema, no coordinate math, no gesture arbitration rules, no performance numbers, no monetisation design, no release process, and no work-item tracker. All of it would have been invented ad hoc at implementation time. That gap is now closed.

**4. `media3` is the largest dependency in the project and it plays one logo animation** — on the critical path to first frame, on every launch, in a market where install size is a conversion factor.

### Decisions made

- **D6 upgraded** — adopt `androidx.ink` 1.0.0, pending the benchmark
- **D7 reframed** — estimate in engineering-days, not calendar dates; the bandwidth question is sidestepped rather than guessed
- **D8–D16 added** — PDF stack · ML Kit · no auth wall · drop the splash video · `minSdk` timing · Auto Backup off · single module until a named trigger · offline-first restated · freemium with no page caps
- **V1–V3 restated** with reasoning; **V4 answered** (Figma exists); **V5 still open**
- **D4 verified** as implemented

### Open, and needing the project owner rather than an engineer

1. **Figma access** — the file is login-gated and could not be read. Needs frame exports, a public view link, or a token export (`VAHI-004`).
2. **D11** — keep the splash video as a brand asset, or drop it and Media3 with it?
3. **D16** — is freemium-with-sync the right monetisation shape?
4. **V5** — illustration style for empty states (recommendation: none for v1).
5. Which Indian language ships second, after Hindi.

### State at end

**No application code written or modified.** The build is untouched and still succeeds. Documentation went from 14 files to 23, plus `CLAUDE.md`. The working tree remains uncommitted on top of a single commit — still the highest-probability way to lose the project.

### Next action

`VAHI-001` — commit the working tree in logical chunks. Then `VAHI-002` (upgrade the dependency baseline) and `VAHI-008` → `VAHI-101` (get a real mid-range tablet and measure ink latency, which settles D6).

---

## 2026-09-06 — Session 4: Track A implementation (first code changes)

**Requested:** implement the minor/small items now (session budget was running low), defer major/foundational work to later. First session in which application code was written.

**Done — all of Track A from `11-forward-plan.md`, plus a few small items from doc 12/13:**
- **Built the canvas toolbar:** new file `ui/components/drawing/DrawingToolbar.kt` (241 lines) — a floating left-edge vertical rail wired directly to `CanvasState`. Pen/eraser toggle, 5 colour swatches, 3 width presets, undo/redo/clear (disabled states reflect `canUndo()`/`canRedo()`/`strokes.isNotEmpty()`). Wired into `CanvasScaffold.kt`. This activates six previously-unreachable `CanvasState` capabilities.
- **Fixed bug #3** (dropdown px/dp anchoring): `HomeScreen.kt` now converts captured pixel offsets to dp via `LocalDensity` before using them as padding.
- **Fixed bug #6** (splash could hang forever): `SplashScreen.kt` now has an `onPlayerError` listener and a 6-second fallback timeout, both guarded so `onTimeout()` fires exactly once.
- **Fixed bug #9** (missing window insets): `HomeScreen.kt`'s content and `CanvasScaffold.kt`'s back arrow + new toolbar now apply `windowInsetsPadding(WindowInsets.safeDrawing)`.
- **Fixed bug #7** (phone dead-end): `AuthFallbackLayout` now shows "Vahi works best on a tablet" instead of the debug string `"Auth (Mobile)"`. Still no functional path forward on phones — that's intentional per decision D2, not a bug.
- **Deleted dead code:** `NotebookScreen.kt`, `ic_launcher_foreground.xml`, `res/values/colors.xml`, and four unused colour constants (`VahiCardMauve`, `VahiGoogleButtonBg`, `VahiAccentBlue`, `VahiAccentTeal`) from `Color.kt`.
- **Resolved D4:** `VahiTheme`'s `dynamicColor` parameter now defaults to `false`.
- **Fixed two WCAG-AA contrast failures:** added `VahiTextPlaceholder` (`#767676`, passes AA) to `Color.kt`; replaced `#999999` (Home search placeholder) and `#AAAAAA` (Auth field placeholders), which measured ≈2.85:1 and ≈2.3:1 respectively — both below the 4.5:1 minimum.

**Verified before considering this done:**
- `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL
- `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL (full build, confirms resource merging is clean after the deletions)
- Grepped for dangling references to every deleted file/resource — none found

**Explicitly NOT done, and why:**
- **No on-device/emulator verification.** This environment has no GUI. The toolbar layout, insets behaviour, and dropdown positioning are compile-safe but visually unverified — flag this clearly to the user and recommend running on a real tablet or emulator before treating them as finished.
- **The working tree was not committed.** Committing requires explicit user request under this assistant's standing operating rules; it wasn't given this session. The repo still has one commit (`89bafd3`), with Session 4's changes layered on top of the pre-existing uncommitted diff from before Session 1.
- **Touch-target sizing (28dp/36dp → 48dp)** and **emoji → Material Symbols icon replacement** were scoped out as higher-risk-of-visual-regression without device testing, or larger than "minor." Confirmed via the `material-icons-core` jar (already on the classpath, no new dependency) that `Search`/`MoreVert`/`ArrowBack`/`Delete` are available but `Undo`/`Redo` are not — used text glyphs (`↶`/`↷`) for those instead, consistent with the app's existing icon conventions.
- **No general hardcoded-colour migration** — only the two contrast-failing values were touched. A full migration to semantic tokens is intentionally deferred to ride along with the Track C ViewModel refactor (per `13-visual-design-system.md` §9's own sequencing advice — doing it twice would be wasted work).

**State at end:** App compiles and builds successfully. Toolbar and bug fixes are code-complete but not visually verified. No foundational work (F1–F5) started.

**Next action:** Visually verify this session's changes on a tablet or emulator. Then, when ready for major work: Track B of `11-forward-plan.md` — the ink architecture spike (D6).

---

## 2026-09-06 — Session 3: UX and visual design planning, continuity system

**Requested:** plan the UI/UX — how to make it modern, attractive, user-friendly and easy to use — and set up documentation so work can resume smoothly on a different account if a usage limit is hit. Explicitly: no implementation.

**Done:**
- Wrote `docs/12-ux-plan.md` — UX plan
- Wrote `docs/13-visual-design-system.md` — visual design plan
- Wrote `docs/00-START-HERE.md` — cold-start entry point, settled decisions, resume prompt, doc maintenance protocol
- Created this log
- Updated `README.md` doc index

**Key findings:**
- **Time-to-ink is the critical UX metric and it's currently bad:** 3 taps plus a forced splash video on *every* launch, because no session persists and there's no "continue where you left off". Target: ≤1 tap, ≤2 seconds. Several recommendations follow from fixing just this.
- **The auth wall is the biggest drop-off risk.** Four ambiguous fields (username + email + password + confirm, under a button labelled "Login") before the user sees anything. The app is offline-first by design, so an account isn't technically needed in v1 — let people in and ask later.
- **Three colour values fail WCAG AA contrast:** `#999999` (≈2.85:1) and `#AAAAAA` (≈2.3:1) on white. `#666666` and `#333333` pass. `#767676` is the lightest grey that passes.
- **Hand-occlusion drives toolbar placement.** A right-handed writer's palm covers the bottom-right of a landscape tablet. Recommended a floating **left-edge vertical tool rail**, mirrored via a handedness setting — not the conventional top bar.
- **Pen slots** (3–5 always-visible preconfigured pens) collapse the common case to one tap, vs. four interactions for select→picker→choose→close.
- **The font is a hard constraint, not a preference.** `FontFamily.Default` won't render Devanagari consistently across manufacturers. Needs a bundled family with verified Indic coverage.
- **Dark mode has a product question inside it:** should the *paper* go dark? Recommended separating "App theme" from "Paper theme", storing ink colours unchanged and always exporting light.

**Decisions made:** none new. Added open visual-design questions V1–V5 to doc 13.

**State at end:** Planning complete across strategy, UX and visual design. **No application code written or modified.** The ~660 lines of uncommitted app code from before Session 1 are still uncommitted and untouched.

**Next action:** Awaiting the user's go-ahead to begin implementation. First work would be Track A of `11-forward-plan.md`.

---

## 2026-09-06 — Session 2: Strategy and forward planning

**Requested:** think independently about the project, plan how to take it forward, identify flaws to rectify. Explicitly: no implementation.

**Done:**
- Wrote `docs/11-forward-plan.md` — the reasoned plan
- Marked `docs/10-roadmap.md` as superseded for ordering
- Asked the user four decision questions; all four answered

**Key findings:**
- **Reframed the bug list:** the 12 catalogued bugs are really **3 genuine standalone bugs + 2 decisions + 7 symptoms of 5 missing foundations** (F1 ink architecture · F2 document/coordinate model · F3 state & navigation · F4 persistence · F5 command history).
- **Identified a hard ordering constraint:** strokes are stored as a mutable `Path` in *screen pixel* coordinates. Persisting that produces unmigratable data. **F1 and F2 must precede F4.**
- **Ink latency is a go/no-go gate.** Compose's normal draw pipeline carries 2–3 frames of latency; you cannot optimise past it inside a `Canvas`. Recommended spiking `androidx.ink` and *measuring*, rather than deciding from theory.
- **Inferred the product thesis** from the codebase (name, "Maths notes"/"Python Problems" samples, PDF import prominence, freemium hooks) — student notebook, Android tablets, Indian market. **Confirmed correct by the user.**
- **Target hardware changes the engineering:** budget tablets make performance a requirement rather than polish, and mean a pressure-sensitive stylus cannot be assumed.
- **The differentiator has a dependency:** Indic handwriting recognition needs ordered stroke *input* data, not rendered paths — another reason F1 must be right first.

**Decisions made:**
- **D1 — Paged**, A4 (595 × 842 pt), units in points
- **D2 — Tablet-only v1**, but fix the phone dead end
- **D3 — Product thesis confirmed:** students, Android tablets, Indian market
- **D5 — A real product to ship**, full foundations
- **D6 left open deliberately:** ink architecture, pending a latency measurement

**State at end:** No application code written or modified.

---

## 2026-09-06 — Session 1: Full project documentation

**Requested:** go through the whole project in detail and produce complete documentation covering what the project is, its existing ideas, its flow, and all related details.

**Done:**
- Read every source file, resource, and build file in the repo
- Wrote `README.md` and `docs/01` through `docs/10` (~1,880 lines)
- Verified `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL

**Key findings:**
- The app is a **UI prototype with a working ink engine and no data layer**. Full flow is navigable; pen drawing works; nothing persists.
- **Eraser, undo, redo, clear, colour and width are fully implemented but unreachable** — `activeTool` is never assigned anywhere and there is no canvas toolbar.
- **12 bugs catalogued and ranked**, including: rotation destroys all state; the Grid canvas cannot scroll because `DrawingCanvas` consumes its pointer events; dropdown anchoring uses pixels as dp; the eraser deletes by bounding box; undo and erase share one stack; the splash can hang forever on a media error.
- **Phones dead-end at the Auth screen** — `AuthFallbackLayout` receives no callback.
- Dead code identified: `NotebookScreen.kt`, `BaseViewModel.kt` (never subclassed), four empty `package-info.kt` markers.
- `BaseViewModel` compiles only via **transitive** dependencies — `lifecycle-viewmodel` and `kotlinx-coroutines` are not declared.

**State at end:** No application code written or modified. Repo had one commit (`89bafd3 "update"`) plus ~660 lines of uncommitted app code, which were left untouched.

---

## Baseline — before Session 1

- Single commit on `main`: `89bafd3 "update"`
- ~660 insertions / 288 deletions uncommitted across 25 files: the entire `ui/components/drawing/` package, `CanvasScaffold.kt`, the launcher-icon replacement, and rewrites of `AuthScreen`, `HomeScreen`, `SplashScreen` and all three paper styles
- No documentation of any kind existed
