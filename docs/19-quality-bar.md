# 19 — Quality Bar

> **Status: proposal.** Performance budgets, device matrix, accessibility requirements, testing strategy and the definition of done — the numbers a build is measured against.
>
> Earlier documents said "performance is a requirement, not polish" and "accessibility is currently close to zero". Both are true and neither is actionable without numbers. This document supplies them.

---

## Part 1 — The reference devices

**Budgets are meaningless without naming the hardware they apply to.** Per [D3](22-decisions.md#d3--product-thesis-student-notebook-android-tablets-india), the target is a price-sensitive Android tablet, not a flagship.

| Tier | Spec profile | Role |
|---|---|---|
| **T1 — Floor** | ~2 GB RAM, entry SoC, 60 Hz, **no active stylus** | The app must be *usable*. Sets the hard minimum. |
| **T2 — Reference** ⭐ | ~4 GB RAM, mid-range SoC, 60–90 Hz, capacitive or basic active stylus | **All budgets below are stated for T2.** This is Aditi's tablet. |
| **T3 — Good** | ~6–8 GB RAM, 90–120 Hz, pressure-sensitive stylus (S Pen class) | The app should feel excellent. |

**Rules**

1. **Never benchmark on an emulator.** Emulator input latency is meaningless and emulator GPU performance is unrelated to the target.
2. **Never benchmark on your development flagship alone.** It hides exactly the problems this section exists to catch.
3. Every number recorded in `PROJECT-LOG.md` must name the **device, Android version and tier**. A number without its device is not a measurement.

**Test matrix — minimum coverage before release** ([D3](22-decisions.md#d3--product-thesis-student-notebook-android-tablets-india) raises stylus fragmentation as a high risk):

| Axis | Must cover |
|---|---|
| Tier | T1, T2, T3 |
| Input | Finger-only · capacitive stylus (no pressure) · active stylus (pressure + barrel button) |
| OS | Oldest supported · current · newest |
| Screen | 10" · 12"+ · split-screen · free-form window |
| *(v1.5+)* Form factor | **Phone (Compact) · foldable folded/unfolded** — added when [E8](21-backlog.md) lands; see [`23-cross-platform-plan.md`](23-cross-platform-plan.md) Part 5 |
| *(v2.x)* Platform | **iPad · iPhone**, each with its own stylus story (Apple Pencil / none) — see that plan's Part 6. Note this roughly doubles every row above it, which is most of the standing +30–40% cost on post-port feature work. |
| Locale | English · हिन्दी |
| Font scale | 100 % · 200 % |
| Theme | Light · Dark · Dark app + white paper · Light app + dark paper |

---

## Part 2 — Performance budgets

All figures are for **T2**, the reference device.

### 2.1 Ink latency — the number the product lives on

| Metric | Budget | Stretch |
|---|---|---|
| Touch-to-pixel, active stroke | **≤ 40 ms** | ≤ 25 ms |
| Touch-to-pixel, T3 with active stylus | ≤ 30 ms | ≤ 20 ms |
| Regression tolerance | +5 ms in any release | — |

**Why 40 ms and not "as low as possible".** Below roughly 50 ms, ink starts to feel attached to the stylus tip rather than trailing it. Compose's normal draw pipeline carries 2–3 frames — around 33–50 ms at 60 Hz — *before* any of your own work, which is why the current implementation cannot reach this and why [D6](22-decisions.md#d6--ink-architecture-adopt-androidxink) matters.

**How to measure — pick one and stay with it:**
1. **High-speed camera** (240 fps phone camera is enough). Film the screen while drawing; count frames between physical contact and first ink. Crude, cheap, and unarguable.
2. **Android's `dumpsys gfxinfo` / latency traces.** More precise, more setup.
3. **A/B comparison.** Run the current build and the `androidx.ink` build side by side on the *same* device with the *same* stylus. Even without absolute numbers, the relative answer decides D6.

**This measurement is the single most important number in the project. Do not skip it, and write it down.**

### 2.2 Frame rate

| Scenario | Budget |
|---|---|
| Idle / static page | No dropped frames |
| Drawing on an empty page | ≥ 58 fps at 60 Hz |
| Drawing on a **dense page** (~2,000 strokes) | ≥ 55 fps |
| Scrolling a 100-page notebook | ≥ 55 fps, no visible page pop-in |
| Pinch-zoom on a dense page | ≥ 50 fps |
| Jank (frames > 32 ms) during a 60 s writing session | **< 1 %** |

> The current renderer repaints **every settled stroke every frame**, and the grid canvas issues tens of thousands of `drawCircle` calls with no viewport culling. On T2 that will fail these budgets on a busy page. Culling and render caching are therefore **foundation work, not optimisation** ([`16-canvas-spec.md`](16-canvas-spec.md) §6.2).

### 2.3 Memory

| Metric | Budget (T2) | Hard ceiling (T1) |
|---|---|---|
| Resident set, editing a typical notebook | ≤ 250 MB | ≤ 180 MB |
| Rasterised page-cache | ≤ 60 MB, LRU-evicted | ≤ 40 MB |
| Peak during PDF import (v1.1) | ≤ 350 MB | ≤ 220 MB |
| Leak after opening and closing 20 notebooks | **0 retained activities/VMs** | same |

### 2.4 Size and startup

| Metric | Budget | Notes |
|---|---|---|
| Download size (per-ABI split, AAB) | **≤ 25 MB** | Real conversion factor in a price-sensitive market |
| Install size | ≤ 60 MB | |
| Cold start → first frame | ≤ 800 ms | Today: ExoPlayer construction is on this path |
| **Time-to-ink, warm** | **≤ 2 s, ≤ 1 tap** | The headline UX metric ([`12-ux-plan.md`](12-ux-plan.md) Part 1) |
| Notebook open (100 pages) → first page interactive | ≤ 400 ms | Load the visible page first, the rest lazily |

**Size budget allocation.** `androidx.ink` ships native libraries and the bundled Noto fonts are not free. Both are worth their cost; `media3` is not ([D11](22-decisions.md#d11--drop-the-video-splash-and-the-media3-dependency)). Measure per-ABI splits, not the universal APK — the universal number will alarm you for no reason.

### 2.5 Battery and thermals

| Metric | Budget |
|---|---|
| 30 min continuous writing | ≤ 12 % battery on T2 |
| Sustained thermal throttling in 30 min | None |
| Background CPU when not in the editor | ~0 (WorkManager jobs are idle+charging constrained) |

---

## Part 3 — Accessibility requirements

Currently **close to zero**: almost every interactive element passes `contentDescription = null` or is a bare `Text` inside a clickable `Box`. This is mostly cheap to fix and it is a correctness issue, not a preference.

### 3.1 Required for v1 — non-negotiable

| # | Requirement | Verify by |
|---|---|---|
| A1 | Every interactive element has a meaningful `contentDescription` or `Modifier.semantics` label | TalkBack sweep of every screen |
| A2 | Every touch target ≥ **48 × 48 dp** (visual size may be smaller) | Layout Inspector. **Current failures:** card overflow 28 dp, view toggles 36 dp, tool-rail items effectively 40 dp ([`09-project-status.md`](09-project-status.md) #15) |
| A3 | All text ≥ **4.5:1** contrast on its background (3:1 for ≥ 18 pt) | Contrast checker on every token pair |
| A4 | Layout survives **200 % font scale** without clipping or overlap | Manual, every screen |
| A5 | TalkBack can complete: create → write → back → delete → undo | Manual script |
| A6 | Focus order matches visual order; no focus traps | TalkBack + keyboard |
| A7 | Full keyboard navigation with a visible focus indicator | Bluetooth keyboard |
| A8 | System "remove animations" is honoured | Developer options |
| A9 | Dark mode works end-to-end — **no hardcoded white** | Manual, every screen |
| A10 | No information conveyed by colour alone (selected tools need a shape/ring cue too) | Greyscale screenshot review |
| A11 | Icons are real vectors, not emoji | Grep for emoji in `.kt` |

**On A11.** Twelve emoji and text glyphs are currently doing the work of icons — `🐦 🔍 ⊞ ≡ ⋮⋮ 🧮 💻 📓 □ ↓ ← G`. They render differently on every manufacturer's font, cannot be tinted or sized reliably, and screen readers announce them by unicode name: "bird", "abacus". That is not a cosmetic problem.

### 3.2 Canvas accessibility — be honest about the limit

A handwriting canvas is inherently visual and **cannot be made fully accessible to a blind user**. Pretending otherwise produces worse outcomes than stating the limit. What is achievable and required:

- All **chrome** — tool rail, document bar, page drawer, menus — is fully TalkBack-navigable and keyboard-operable.
- The canvas exposes a single labelled region: "Drawing canvas, page 3 of 12, 47 strokes."
- Once recognition ships (v1.2), a page can be **read aloud** as recognised text. That is the genuinely valuable accessibility feature here, and it is a real one.
- Every canvas action is reachable without a gesture — undo, redo, tools, page navigation all have button equivalents. Gestures are shortcuts, never the only path.

### 3.3 Localisation ([`12-ux-plan.md`](12-ux-plan.md) Part 8)

| # | Requirement |
|---|---|
| L1 | **Zero hardcoded UI strings.** `strings.xml` currently has exactly one entry. |
| L2 | English + Hindi at v1 |
| L3 | Bundled font with **verified** Devanagari coverage ([V2](22-decisions.md#v2--font-family-noto-sans--noto-sans-devanagari)) — render a test string in each language and look at it |
| L4 | No fixed-width text containers — Hindi runs 20–30 % longer |
| L5 | Dates, numbers and pluralisation via platform formatting, never string concatenation |
| L6 | RTL not required (Indic scripts are LTR); `supportsRtl` stays as-is with no RTL testing effort |

---

## Part 4 — Testing strategy

Proportionate, not exhaustive. Test where the bug cost is high and the test cost is low.

| Target | Type | Value | Why |
|---|---|---|---|
| **Coordinate transforms** | Unit | **Highest** | Pure, trivial to test, and a bug here corrupts user data silently and irreversibly |
| **Command history** (undo/redo/erase interaction) | Unit | **Highest** | Pure logic with genuinely tricky edge cases — the exact area where the current implementation is wrong |
| **`.ink` serialisation round-trip** | Unit | **Highest** | Data loss if wrong. Include every historical format version. |
| **Pointer arbitration state machine** | Unit | High | Palm rejection logic is pure and its failure mode is severe |
| **Repository + DAO** | Instrumented | High | Room has good test support; data-layer bugs are the expensive kind |
| **Room migrations** | Instrumented | High | Open a real vN database, migrate, assert. Non-negotiable once users have data. |
| **ViewModel state reduction** | Unit (Turbine) | Medium | Cheap once ViewModels exist |
| **Navigation smoke test** | Compose UI | Medium | `ui-test-junit4` is already on the classpath and completely unused |
| **Screenshot tests** | Instrumented | Medium | Valuable for a design-heavy app; heavy to set up. Defer to post-v1. |
| **Ink feel, palm rejection, latency** | **Manual** | Critical | **Cannot be automated.** Accept this and write the checklist. |

**Coverage target:** ~70 % on `domain/` and `data/`. **No target on `ui/`** — chasing coverage in composables produces brittle tests that assert layout instead of behaviour.

**Start here.** The first test written in this project should be `CanvasState` / command-history logic: a plain class, no Android dependencies, real edge cases, and the area currently known to be wrong.

### 4.1 The manual checklist that cannot be automated

Run before every release, on T1 **and** T2, with **both** a finger and a stylus:

**Ink feel**
- [ ] Write a paragraph of normal handwriting. Does it feel attached to the tip?
- [ ] Fast diagonal strokes — any visible faceting?
- [ ] Slow curve — smooth, or polygonal?
- [ ] Pressure varies width (T3)
- [ ] Finger drawing looks deliberate, not broken

**Palm**
- [ ] Palm down first, then stylus → only the stylus draws
- [ ] Palm lands mid-stroke → stroke continues, palm leaves nothing
- [ ] Write a full line resting naturally → zero palm marks
- [ ] Finger-draw off → finger only pans

**Durability**
- [ ] Rotate mid-stroke → stroke persists
- [ ] Force-stop mid-session → reopen → everything is there
- [ ] Airplane mode → nothing changes
- [ ] Fill storage → clear error, no data loss

**Every release, every screen**
- [ ] TalkBack sweep
- [ ] 200 % font scale
- [ ] Dark mode
- [ ] Hindi
- [ ] Split-screen

---

## Part 5 — Definition of Done

A work item is **not** done until every line applies.

**Code**
- [ ] Builds clean: `./gradlew :app:assembleDebug` — no new warnings
- [ ] Unit tests pass; new logic has tests
- [ ] No hardcoded colours — semantic tokens only
- [ ] No hardcoded strings — `strings.xml` only
- [ ] No emoji as icons
- [ ] `contentDescription` on every interactive element
- [ ] Touch targets ≥ 48 dp
- [ ] Loading, empty and error states exist where the operation can be slow or fail
- [ ] `@Preview` for every new composable, light and dark

**Verification**
- [ ] Run on a real device — **not only the emulator**
- [ ] Rotation and process death both survived
- [ ] TalkBack reaches and describes everything new
- [ ] Dark mode checked
- [ ] Relevant performance budget re-measured if the change could affect it

**Documentation** — the standing rule from [`00-START-HERE.md`](00-START-HERE.md) §7
- [ ] `PROJECT-LOG.md` entry appended
- [ ] Affected as-built docs (01–09) updated
- [ ] `21-backlog.md` item status updated
- [ ] Any decision made recorded in `22-decisions.md`

**The rule behind the rule:** if a change is not documented in the same session it was made, it is not finished. Documentation drift is what makes a project unresumable, and this doc set is the continuity mechanism.

---

## Part 6 — Risk register

Carried forward from [`11-forward-plan.md`](11-forward-plan.md) Part 7, re-scored against what Session 5 verified.

| # | Risk | Severity | Change | Mitigation |
|---|---|---|---|---|
| R1 | Ink does not feel good enough | **Critical** | ↓ *lowered* | `androidx.ink` 1.0.0 is stable with Compose-native low-latency authoring ([D6](22-decisions.md#d6--ink-architecture-adopt-androidxink)). Still gated on the §2.1 measurement. |
| R2 | Persisting on the wrong stroke model | **Critical** | — | The R1 ordering rule in [`17-data-spec.md`](17-data-spec.md). Do not relax it under schedule pressure. |
| R3 | Scope creep — the UI advertises ~20 features | High | — | The explicit out-of-scope list in [`14-product-spec.md`](14-product-spec.md) §4 |
| R4 | No differentiation — ships as a worse GoodNotes | High | ↓ *lowered* | ML Kit covers all nine target scripts, on-device and free ([D9](22-decisions.md#d9--handwriting-recognition-uses-ml-kit-digital-ink-on-device)) |
| R5 | Android stylus fragmentation | **High** | — | The §1 device matrix. Design for the no-pressure/finger case; treat stylus data as enhancement. |
| R6 | `androidx.ink` maturity | Medium → **Low** | ↓ *lowered* | 1.0.0 stable since 2025-12-17. `InkEngine` interface keeps the fallback cheap. |
| R7 | Performance on budget hardware | **High** | — | The §2 budgets, measured on T2. Culling and caching promoted into the foundation. |
| R8 | **`androidx.pdf` is beta and forces `minSdk 28`** | Medium | ↑ *new* | Schedule PDF late; contain Fragment interop to one file ([D8](22-decisions.md#d8--pdf-annotation-builds-on-androidxpdf--androidxink)) |
| R9 | **ML Kit language packs need network on first use** | Medium | ↑ *new* | Explicit, visible download from Settings; never block writing on it ([D9](22-decisions.md#d9--handwriting-recognition-uses-ml-kit-digital-ink-on-device)) |
| R10 | **Stale dependency baseline** — Compose BOM 2024.09.00 and 2023-era AndroidX against `compileSdk 36` | Medium | ↑ *new* | Upgrade the catalog **before** adding Room/Hilt/Navigation, not after ([`18-architecture-target.md`](18-architecture-target.md) §9) |
| R11 | **Auto Backup silently enabled on notebook data** | Medium | ↑ *new* | Explicit exclusion rules before Room ships ([D13](22-decisions.md#d13--android-auto-backup-is-disabled-for-notebook-data)) |
| R12 | **The uncommitted working tree** — hundreds of lines, one commit in the repo | Medium | ↑ *new* | Commit in logical chunks before foundation work starts. A single bad `git checkout` currently costs everything. |
