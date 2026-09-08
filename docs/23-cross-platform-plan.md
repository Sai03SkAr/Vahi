# 23 — Cross-Platform and All-Device Plan

> **Status: plan only. Nothing here is implemented, and nothing here should be started until v1 ships on Android tablets.** This document exists so the expansion is a considered move rather than an improvised one — and so that the work happening *now* doesn't quietly make it more expensive.
>
> Resolves [D17](22-decisions.md#d17--android-tablets-for-v1-all-devices-as-a-planned-post-v1-phase). Companion to [`14-product-spec.md`](14-product-spec.md) (what v1 is) and [`18-architecture-target.md`](18-architecture-target.md) (the layering this depends on).
>
> **Read Part 2 and Part 8 if you read nothing else.** Part 2 is the reframe that makes this tractable; Part 8 is the only part that affects work happening today.

---

## Part 1 — The goal, stated concretely

"Works on all devices" means these, specifically:

| Device class | Screen | Input | Currently |
|---|---|---|---|
| **Android tablet** | 840dp+ | Stylus (often none), finger | ✅ The v1 target |
| **Android phone** | < 600dp | Finger, rarely stylus | ❌ Honest "designed for tablets" wall ([D2](22-decisions.md#d2--tablet-only-for-v1)) |
| **Android foldable** | Both, mid-session | Finger, S Pen on some | ❌ Not considered |
| **iPad** | 1024pt+ | Apple Pencil, finger | ❌ Does not exist |
| **iPhone** | < 430pt | Finger, no stylus | ❌ Does not exist |
| **ChromeOS / desktop** | Large, mouse+keyboard | Mouse, trackpad | ❌ Out of scope; see Part 9 |

The end state: one codebase, five shipping targets, one account, notes that open identically everywhere.

---

## Part 2 — The reframe: this is two independent problems, not one

**This is the most useful thing in this document.** "Make it work everywhere" gets treated as a single project. It is not. It is two, they are independent, and they have wildly different costs:

```
                    ┌─────────────────────────────────────────┐
                    │  AXIS A — FORM FACTOR                   │
                    │  phone ⇄ tablet ⇄ foldable              │
                    │                                         │
                    │  Pure responsive-layout work.           │
                    │  100% inside the current Android        │
                    │  codebase. No KMP. No new language.     │
                    │  No new build system.                   │
                    │                                         │
                    │  ~12–14 ed                              │
                    └─────────────────────────────────────────┘
                                      ⊥  independent
                    ┌─────────────────────────────────────────┐
                    │  AXIS B — PLATFORM                      │
                    │  Android ⇄ iOS                          │
                    │                                         │
                    │  KMP restructuring, second ink engine,  │
                    │  DI migration, second app-store         │
                    │  pipeline, doubled QA surface.          │
                    │                                         │
                    │  ~44–50 ed                              │
                    └─────────────────────────────────────────┘
```

**Why this matters practically:**

1. **Axis A is ~25% of the cost and unlocks the larger market.** Android phones vastly outnumber Android tablets. A student who owns a tablet almost certainly also owns a phone. Supporting phones is cheaper than supporting iOS *and* likely reaches more of the target user.
2. **Axis A must happen first regardless**, because the responsive layouts it produces are what iPhone and iPad need too. Doing Axis B first would mean building iOS layouts that then get rebuilt for Android phones.
3. **You can stop after Axis A** and have a genuinely "works on all Android devices" product. That is a legitimate, complete outcome.

> **Sequencing rule: Axis A before Axis B, always.** Axis B without Axis A means building iPhone layouts twice.

---

## Part 3 — Dependency audit: what ports, what doesn't

Verified 2026-09-08. Re-verify before starting — these move.

### ✅ Already portable (built this way deliberately)

| Piece | Why it's fine |
|---|---|
| `core/model/` — coordinate spaces, `ViewTransform`, `PageLayout`, `PaperStyle` | Pure Kotlin, zero Android imports. **Proven**: its 24 tests run as plain JVM tests with no Robolectric. |
| `domain/` — `Notebook`, `NotebookRepository` | Pure Kotlin by design ([`18-architecture-target.md`](18-architecture-target.md) §3). |
| `CanvasState` command/history logic | Pure logic — *except* `Stroke.path` (see below). |
| `kotlinx.serialization` (typed nav routes) | Multiplatform by design. |
| Compose UI code generally | Compose Multiplatform **1.12.0** (Sept 2026) covers iOS, Android, desktop and web. iOS has been stable since 1.8.0 (May 2025); Netflix and Cash App run it in production. |

### 🔄 Ports with a real but mechanical migration

| Piece | The change |
|---|---|
| **Room** | Has genuine KMP support since 2.7.0 via the `androidx.sqlite` driver API (`BundledSQLiteDriver` works on iOS). Room 3.0 (2026) goes further. The entity/DAO/database built this session should port with modest change. |
| **`navigation-compose`** | Different Maven coordinate: `org.jetbrains.androidx.navigation` instead of `androidx.navigation`. Plus iOS back-swipe needs platform glue — CMP translates it, but native-feeling swipe-back wants extra work. |
| **DataStore** | Has multiplatform support; alternatively `multiplatform-settings`. Not yet used by Vahi, so this is a "choose correctly when you add it" note, not a migration. |

### ❌ Android-only — needs a genuine second implementation

| Piece | iOS replacement | Cost |
|---|---|---|
| **Hilt** (built this session) | **Koin** — the KMP standard. Dagger/Hilt has no KMP path. | ~3 ed. Mechanical but touches every injection site. |
| **`androidx.ink`** ⚠️ | **PencilKit** (`PKCanvasView`) via `UIKitView`/cinterop | **The big one — see Part 4.** |
| **`androidx.pdf`** (v1.1 plan) | **PDFKit** (`PDFView`) | ~5 ed. Both are "render + annotate a PDF"; neither shares code. |
| **Media3/ExoPlayer** (splash video) | AVFoundation | Moot if [D11](22-decisions.md#d11--drop-the-video-splash-and-the-media3-dependency) is accepted and the video is dropped. |

### ✅ The differentiator ports — correcting an earlier error

> **This corrects what [D17](22-decisions.md#d17--android-tablets-for-v1-all-devices-as-a-planned-post-v1-phase) originally said.** The first pass of that analysis assumed ML Kit Digital Ink Recognition was Android-only and concluded the Indic-recognition wedge would be lost on iOS. **That was wrong.**
>
> **ML Kit Digital Ink Recognition runs on iOS as well as Android** — fully offline, same 300+ languages and 25+ writing systems, including every Indic script in [D9](22-decisions.md#d9--handwriting-recognition-uses-ml-kit-digital-ink-on-device). Google ships an official iOS quickstart for it.
>
> **This materially improves the case for iOS.** The product's central differentiator — "search your handwritten Hindi notes" — is *not* Android-locked. It needs a Kotlin binding to the iOS SDK (~3 ed of cinterop work), not a reinvention. The remaining Android-only piece is the ink *engine*, not the ink *recognition*.

---

## Part 4 — The ink engine: the actual hard part

Everything else on this list is schedule. This is the part that carries risk.

### The problem

`androidx.ink` — the library [D6](22-decisions.md#d6--ink-architecture-adopt-androidxink) recommends, specifically because it solves low-latency authoring, real stroke geometry and serialisation together — has **no iOS build and none announced**. iOS's equivalent is Apple's **PencilKit**: a completely different API, with no shared code.

So iOS needs a **second ink engine**. Not a port — a second implementation of the single most latency-critical, most complex part of the product.

### Why not just use Compose Multiplatform's shared `Canvas` for both?

Tempting — one engine, shared everywhere. **Don't.** Compose Multiplatform renders through Skia→Metal on iOS, and Skia's Metal integration carries measurable latency versus native. The current Android implementation already proves the point: a plain Compose `Canvas` ink path cannot hit the latency bar even on Android ([`16-canvas-spec.md`](16-canvas-spec.md) §1). Shipping a shared-but-laggy engine would sacrifice the product's core quality claim on *both* platforms to save integration work on one.

**Use the best native engine on each platform.** This is the one place where "share everything" is the wrong instinct.

### The architecture that makes this survivable

The `InkEngine` interface in [`18-architecture-target.md`](18-architecture-target.md) §3.1 was designed to contain exactly one swap (androidx.ink vs. a hand-rolled fallback). It turns out to be the right shape for this too:

```
                  ┌──────────────────────────────┐
                  │  commonMain                  │
                  │  interface InkEngine         │  ← the app only ever sees this
                  │  + CanvasState, command      │
                  │    history, coordinate model │
                  └───────┬──────────────┬───────┘
                          │              │
              ┌───────────▼──────┐  ┌────▼─────────────────┐
              │ androidMain      │  │ iosMain              │
              │ AndroidxInkEngine│  │ PencilKitInkEngine   │
              │ (androidx.ink)   │  │ (PKCanvasView via    │
              │                  │  │  UIKitView/cinterop) │
              └──────────────────┘  └──────────────────────┘
```

Compose Multiplatform's `UIKitView` interop is first-class and documented, so embedding a `PKCanvasView` inside Compose is a supported pattern, not a hack.

**What iOS gets for free that Android fights for:** PencilKit provides palm rejection, pressure/tilt, and a tuned low-latency pipeline out of the box. Apple's measured system latency is a consistent 22–28 ms across iPad generations (±0.9 ms). Bug #14 — the palm-rejection problem that is currently the most severe open defect on Android — largely does not exist on iOS.

### ⚠️ The data-format trap — this one affects decisions being made *now*

**A user with an Android tablet and an iPhone must see the same notes.** That means stroke data written by `AndroidxInkEngine` has to be readable by `PencilKitInkEngine` and vice versa.

If the `.ink` file stores `androidx.ink`'s native serialised blob (via `ink-storage`), **iOS cannot read it** — and there is no migration path, because the geometry is locked inside an Android-only format. This is the same class of irreversible mistake as the R1 screen-pixel-coordinates rule in [`17-data-spec.md`](17-data-spec.md): cheap to prevent, impossible to undo.

**The rule to adopt when the ink engine lands (`VAHI-102`), whether or not iOS is ever built:**

> **The `.ink` payload must be engine-neutral geometry** — ordered points in page space with pressure/tilt/timestamp — **not the ink library's native blob.** If `ink-storage`'s format is used for speed, store the neutral geometry alongside it, and treat the neutral form as canonical.

The format already has the field to express this: `.ink`'s header carries an `engineId` ([`17-data-spec.md`](17-data-spec.md) §3), designed so a reader knows which engine wrote a file. That foresight only pays off if the *payload* is also portable. **Add this as an explicit acceptance criterion on `VAHI-102`.**

---

## Part 5 — Axis A: all Android devices (phones, foldables)

Entirely within the current codebase. Reverses [D2](22-decisions.md#d2--tablet-only-for-v1)'s v1 scoping — which was always framed as a v1 decision, not a permanent one.

### The layout model

Material 3 window size classes, which `ui/components/WindowInfo.kt` already partially implements:

| Class | Width | Devices | Library | Editor |
|---|---|---|---|---|
| **Compact** | < 600dp | Phones, portrait | 1–2 col grid or list | Tool rail → **bottom bar**; document bar collapses to back + title + overflow |
| **Medium** | 600–839dp | Small tablets, foldable inner, phone landscape | 2–3 col | Rail stays, narrower |
| **Expanded** | ≥ 840dp | Tablets (current target) | 3+ col | Current design |

### What actually changes

| Surface | Compact adaptation |
|---|---|
| **Library grid** | `GridCells.Adaptive(minSize)` instead of `Fixed(3)`; cards shrink; "Jump back in" becomes a horizontal carousel |
| **Editor tool rail** | Left rail is wrong on a phone — the hand-occlusion logic in [`12-ux-plan.md`](12-ux-plan.md) §3.1 assumed a landscape tablet. On a phone, a **bottom toolbar** is correct: reachable by thumb, doesn't steal precious width. |
| **Create-notebook modal** | 680dp fixed modal → **bottom sheet** |
| **Dropdowns/overlays** | Anchored popovers → bottom sheets (the anchoring bug class disappears entirely) |
| **Settings** | Two-pane list+detail → single-pane with drill-down navigation |
| **Canvas zoom defaults** | Fit-to-width on a phone means a *much* smaller page; default zoom and minimum-scale clamps need per-class values ([`16-canvas-spec.md`](16-canvas-spec.md) §2.2) |
| **Foldables** | Handle the fold seam; preserve state across the fold/unfold configuration change (already covered by the `SavedStateHandle` work in `VAHI-213`) |

### Sizing

| Item | ed |
|---|---|
| Window-size-class infrastructure + adaptive scaffolding | 2 |
| Library adaptive layout | 2 |
| Editor adaptive layout (bottom bar variant) | 3 |
| Overlays → sheets on Compact | 1 |
| Settings single-pane | 1 |
| Canvas zoom/scale per size class | 1 |
| Foldable handling | 2 |
| Cross-size testing | 2 |
| **Total** | **~14 ed** |

---

## Part 6 — Axis B: iOS via Kotlin Multiplatform

### Module restructuring

```
:composeApp
  ├── commonMain     core/, domain/, ui/ (Compose), CanvasState, command history
  ├── androidMain    AndroidxInkEngine, Android Room driver, MainActivity, Hilt→Koin
  └── iosMain        PencilKitInkEngine, iOS Room driver, ML Kit iOS binding
:iosApp              Xcode project, thin SwiftUI/UIKit host
```

The existing package layout ([`18-architecture-target.md`](18-architecture-target.md) §2) was designed so this is largely a **directory move**, not a redesign — `core/` and `domain/` go to `commonMain` unchanged.

### Sizing

| Item | ed | Notes |
|---|---|---|
| KMP module restructuring + build setup | 3 | |
| Hilt → Koin | 3 | Touches every injection site |
| Navigation coordinate swap + iOS back-swipe | 2 | |
| Room → KMP driver | 2 | Genuine support exists |
| **`PencilKitInkEngine`** | **12** | **The risk item.** Stroke bridging, pressure/tilt, serialisation compat |
| Engine-neutral `.ink` compat + round-trip tests | 3 | Cheaper if Part 4's rule was followed from the start |
| ML Kit Digital Ink iOS binding (cinterop) | 3 | The API exists on iOS; this is binding work |
| PDF via PDFKit (if v1.1 shipped) | 5 | |
| iOS platform conventions — safe areas, Dynamic Type, VoiceOver, share sheet, file picker | 4 | |
| iOS build, signing, App Store Connect, TestFlight | 3 | Elapsed time as much as effort |
| QA matrix expansion | 5 | Ongoing thereafter, not one-off |
| **Total** | **~45 ed** | |

### The ongoing cost nobody budgets

After the port, **every feature costs more forever**: two ink engines to keep in sync, two stylus ecosystems to test, two app-store review cycles, two crash dashboards. Budget roughly **+30–40% on all subsequent feature work**, not a one-time payment.

---

## Part 7 — Sequencing

```
   NOW ──────────────────────────────────────────────────────────────▶
   │
   │  v1.0  Android tablets          ◀── current work, unchanged
   │        (docs/14 §4 exit criteria)
   │
   ├─ GATE 1: v1 shipped, ink latency verified on real hardware,
   │          crash-free > 99.5%, no data-loss reports
   │
   │  v1.5  Axis A — all Android devices        ~14 ed
   │        phones, foldables, responsive everything
   │
   ├─ GATE 2: phone install/retention data justifies iOS;
   │          engine-neutral .ink format proven by round-trip tests
   │
   │  v2.0  Axis B — iOS                        ~45 ed
   │        iPad first, iPhone same release (Axis A did the layouts)
   │
   └─ v2.x  sync makes cross-device real (Track F — without sync,
            "works on iPad" means "a second, separate set of notes")
```

**Gate 1 exists because** shipping v1 well is worth more than shipping v1 and a half-working phone build. **Gate 2 exists because** iOS is the expensive one and should be justified by real usage data, not ambition.

> **A dependency worth naming: iOS without sync is half a feature.** "Works on my iPad too" that shows an empty, unrelated library is not what a user means. Cloud sync ([Track F](11-forward-plan.md)) is what makes multi-device *mean* something — so the honest iOS release either follows sync or ships alongside it.

---

## Part 8 — What to do NOW so this stays cheap

**This is the only part that touches work happening today.** None of it is speculative cross-platform work — every item is either free or already good practice.

| # | Rule | Cost now | Cost if skipped |
|---|---|---|---|
| **X1** | **`core/` and `domain/` stay free of Android imports.** Already true and already verified (their tests need no Robolectric — that's the proof, and it's worth keeping as the check). | 0 — already doing it | Every violation is a file to untangle later |
| **X2** | **Everything touching `androidx.ink` stays behind `InkEngine`.** Already the plan ([`18-architecture-target.md`](18-architecture-target.md) §3.1); `grep` should keep returning exactly one importing file. | 0 — already the plan | The iOS engine becomes a rewrite instead of an implementation |
| **X3** | ⚠️ **The `.ink` payload is engine-neutral geometry, not a library-native blob.** See Part 4. **Add to `VAHI-102`'s acceptance criteria.** | ~1 ed at `VAHI-102` | **Irreversible.** Android-written notes unreadable on iOS, with no migration path. |
| **X4** | **Don't scatter `android.*` imports through `ui/`.** Where a Compose-native or neutral API exists, prefer it. | ~0 | Death by a thousand small ports |
| **X5** | **When DataStore lands, pick the multiplatform-capable option.** | 0 | A small migration |
| **X6** | **Keep using window size classes rather than hardcoded dp**, even while only Expanded ships. `WindowInfo.kt` already exists. | ~0 | Axis A gets more expensive |
| **X7** | **Don't build phone layouts speculatively.** [D2](22-decisions.md#d2--tablet-only-for-v1) still holds for v1 — X6 keeps the door open without paying to walk through it. | — | Weeks of speculative layout work |

**X3 is the one that matters.** X1, X2, X4–X6 are hygiene that pays off mildly. X3 is a one-way door: get it wrong and cross-device notes become impossible without abandoning user data.

---

## Part 9 — Explicitly out of scope

| Not planned | Why |
|---|---|
| **Desktop (Windows/macOS/Linux)** | Compose Multiplatform supports it, but a handwriting app without a stylus is a viewer, not a notebook. Revisit only if a real "review my notes on a laptop" need appears — and then likely as a web viewer. |
| **Web** | CMP supports it, but **ML Kit does not run on web** — the differentiator would be missing, and stylus latency in a browser is unfixable. |
| **Wear / TV** | Nonsensical for the product. |
| **Android phones as a *primary* target** | Even after Axis A, the design centre stays tablet. Phones are supported, not optimised-for. |

---

## Part 10 — Risks

| # | Risk | Severity | Mitigation |
|---|---|---|---|
| C1 | **The `.ink` format locks to an Android-only blob before iOS is considered** | **Critical** | X3, enforced as a `VAHI-102` acceptance criterion |
| C2 | `PencilKitInkEngine` can't match `androidx.ink`'s feature set (brush parity, geometry queries) | High | Spike PencilKit *before* committing to Axis B; the 12 ed estimate assumes parity is achievable |
| C3 | Compose Multiplatform iOS regression on a Compose/Kotlin upgrade | Medium | Pin versions; CMP has been stable since 1.8.0 and is at 1.12.0 |
| C4 | Doubled QA surface silently slows all future work | Medium | Budget +30–40% on post-port features (Part 6) |
| C5 | Axis B started before Axis A → iPhone layouts built twice | Medium | The Part 2 sequencing rule |
| C6 | iOS ships without sync; users see an empty library and churn | High | The Part 7 dependency — sync before or with iOS |
| C7 | Effort roughly doubles and v1 quality suffers as attention splits | High | The gates. Ship v1 properly first. |

---

## Part 11 — Open questions for the project owner

None of these block current work.

| # | Question | Needed by |
|---|---|---|
| Q1 | Is Axis A (Android phones) enough, or is iOS a firm requirement? Axis A alone is a complete, defensible "works on all Android devices" outcome at ~25% of the cost. | Before v1.5 planning |
| Q2 | If iOS: iPad-only first, or iPhone+iPad together? (Axis A makes "together" nearly free.) | Before Axis B |
| Q3 | Does iOS wait for sync, or ship as standalone-per-device first? | Before Axis B |
| Q4 | Is there budget for an Apple Developer account, a test iPad, and a test iPhone? Axis B cannot be verified without real Apple hardware — the same constraint that currently blocks `VAHI-101` on Android. | Before Axis B |
