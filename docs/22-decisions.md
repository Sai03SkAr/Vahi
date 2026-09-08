# 22 — Decision Log (ADRs)

> **This is the authoritative record of every decision made about Vahi.** Where any other document disagrees with this one, this one wins and the other document is a bug.
>
> Previously decisions lived in `11-forward-plan.md` Part 10 and `13-visual-design-system.md` Part 10. They have been **migrated here** and given full context. Those sections now point here.

**Status vocabulary**

| Status | Meaning |
|---|---|
| ✅ **Accepted** | Decided. Build to it. Do not re-litigate without new evidence. |
| 🔬 **Provisional** | Recommended and safe to build to, but a named piece of evidence could overturn it. The evidence is stated. |
| 🔲 **Open** | Genuinely undecided. Blocking work is listed. |
| ⛔ **Superseded** | Replaced by a later ADR, which is named. |

---

## Index

| # | Decision | Status |
|---|---|---|
| [D1](#d1--paged-documents-not-infinite-canvas) | Paged documents, not infinite canvas | ✅ Accepted |
| [D2](#d2--tablet-only-for-v1) | Tablet-only for v1 | ✅ Accepted |
| [D3](#d3--product-thesis-student-notebook-android-tablets-india) | Product thesis: student notebook, Android tablets, India | ✅ Accepted |
| [D4](#d4--brand-palette-not-material-you) | Brand palette, not Material You (`dynamicColor = false`) | ✅ Accepted · implemented |
| [D5](#d5--a-real-product-not-a-portfolio-prototype) | A real product, not a portfolio prototype | ✅ Accepted |
| [D6](#d6--ink-architecture-adopt-androidxink) | **Ink architecture: adopt `androidx.ink`** | 🔬 **Provisional — upgraded from Open** |
| [D7](#d7--schedule-expressed-in-engineering-days-not-calendar-dates) | Schedule in engineering-days, not calendar dates | ✅ Accepted |
| [D8](#d8--pdf-annotation-builds-on-androidxpdf--androidxink) | PDF annotation builds on `androidx.pdf` + `androidx.ink` | 🔬 Provisional |
| [D9](#d9--handwriting-recognition-uses-ml-kit-digital-ink-on-device) | Handwriting recognition uses ML Kit Digital Ink (on-device) | ✅ Accepted |
| [D10](#d10--no-auth-wall-account-is-optional-and-requested-in-context) | No auth wall — account is optional, requested in context | ✅ Accepted |
| [D11](#d11--drop-the-video-splash-and-the-media3-dependency) | Drop the video splash and the Media3 dependency | 🔬 Provisional |
| [D12](#d12--minsdk-stays-at-26-until-pdf-forces-28) | `minSdk` stays at 26 until PDF forces 28 | ✅ Accepted |
| [D13](#d13--android-auto-backup-is-disabled-for-notebook-data) | Android Auto Backup is disabled for notebook data | ✅ Accepted |
| [D14](#d14--single-gradle-module-until-a-named-trigger-fires) | Single Gradle module until a named trigger fires | ✅ Accepted |
| [D15](#d15--offline-first-the-local-database-is-the-source-of-truth) | Offline-first: the local database is the source of truth | ✅ Accepted |
| [D16](#d16--freemium-with-a-generous-free-tier-no-page-caps) | Freemium with a generous free tier, no page caps | 🔬 Provisional |
| [D17](#d17--stay-android-only-for-now-cross-platform-is-technically-feasible-but-not-free) | Stay Android-only for now — cross-platform is feasible but not free | 🔲 **Open — needs the owner's call** |
| [V1](#v1--app-theme-and-paper-theme-are-separate-settings) | App theme and Paper theme are separate settings | 🔬 Provisional |
| [V2](#v2--font-family-noto-sans--noto-sans-devanagari) | Font family: Noto Sans + Noto Sans Devanagari | 🔬 Provisional |
| [V3](#v3--lavender-stays-as-the-brand-accent) | Lavender stays as the brand accent | 🔬 Provisional |
| [V4](#v4--figma-is-the-visual-source-of-truth-the-docs-describe-behaviour) | Figma is the visual source of truth; the docs describe behaviour | 🔬 Provisional |
| [V5](#v5--empty-state-illustration-style) | Empty-state illustration style | 🔲 Open |

---

## D1 — Paged documents, not infinite canvas

**Status:** ✅ Accepted (Session 2, 2026-09-06) · confirmed with the project owner

**Decision.** A notebook is an ordered list of **pages**. Default page size is **A4, 595 × 842 points**. The document unit is the **PostScript point (1/72 inch)**. Page dimensions are stored **per page**, not globally.

**Why.**
- A4 is the paper size used in India; US Letter is not. This matters for printing and for matching imported material.
- Points are PDF's native unit. Choosing them makes PDF import/export a straight mapping rather than a conversion layer that accumulates rounding drift.
- Per-page dimensions are required because imported PDF pages have their own sizes and are frequently mixed within one document.

**Consequences.**
- `GridCanvasContent`'s 4000 dp surface is retired. Do not invest in it.
- `RuledCanvas` currently draws **vertical** lines, which contradicts its own description. It becomes horizontal rules at a spacing defined in points.
- Paper style becomes a property of a **page**, not of a notebook — a notebook can mix blank, ruled and imported pages.
- Infinite-canvas mode remains possible later as a special page type. It is not built for v1.

**Full specification:** [`17-data-spec.md`](17-data-spec.md) §2, [`16-canvas-spec.md`](16-canvas-spec.md) §2.

---

## D2 — Tablet-only for v1

**Status:** ✅ Accepted (Session 2) · partially implemented (Session 4)

**Decision.** v1 supports the **Expanded** window size class (≥ 840 dp wide) only. Phones get an honest "Vahi is designed for tablets" screen and no functional path forward.

**Why.** Building Compact layouts for Home, the canvas, the create modal and the tool rail is weeks of speculative layout work for a segment that is not the target user. A student annotating a textbook is not doing it on a 6-inch phone.

**Consequences.**
- No Compact layouts are built. `windowClass()` gates Auth today; it will gate the app shell in the target architecture.
- The Play Store listing must declare tablet as the target form factor and set expectations, or the app collects one-star reviews from phone users.
- Revisit after v1 ships, driven by install data — not before.

**What would overturn this:** evidence that a meaningful share of the target audience only owns a phone. That is a market question, not an engineering one.

---

## D3 — Product thesis: student notebook, Android tablets, India

**Status:** ✅ Accepted (Session 2) · confirmed with the project owner

**Decision.** Vahi is a **handwriting notebook for students on Android tablets in the Indian market**, where **imported study material** and **Indic-script handwriting recognition** are first-class.

**Why.** Two wedges the incumbents (GoodNotes, Notability, Samsung Notes, Nebo, Xodo, OneNote) have left open:

1. **Android tablets, properly.** The best handwriting apps are iPad-first; their Android builds are ports, and stylus latency on Android is where they are weakest.
2. **Indic-script handwriting.** On-device recognition of Devanagari, Bengali, Tamil, Telugu, Gujarati, Kannada, Malayalam, Odia and Gurmukhi is free and offline — and essentially nobody has bothered to build it.

**Consequences — and these are the ones that change the engineering.**

| Consequence | Effect on the plan |
|---|---|
| Target hardware is a **budget/mid-range tablet**, not an iPad Pro | Performance is a **requirement**, not polish. Viewport culling and render caching are promoted into the foundation, not the optimisation phase. See [`19-quality-bar.md`](19-quality-bar.md) §2. |
| **A pressure-sensitive stylus cannot be assumed** | Many tablets in this segment ship with no active stylus or a capacitive one with no pressure data. Finger drawing is a **first-class mode**, and palm rejection cannot be built on the assumption that a stylus is present. See [`16-canvas-spec.md`](16-canvas-spec.md) §4. |
| Connectivity is **intermittent** | Offline-first is a hard requirement, not a sequencing convenience. See [D15](#d15--offline-first-the-local-database-is-the-source-of-truth). |
| The market is **price-sensitive** | Per-user recurring cost must be near zero. This is why on-device ML ([D9](#d9--handwriting-recognition-uses-ml-kit-digital-ink-on-device)) is not just convenient but structural. |
| Recognition needs **ordered stroke input data**, not rendered paths | Another reason the stroke model has to be right before anything is built on it. |

**The governing principle that follows:** pick one thing to be unambiguously best at, and treat every other feature as table stakes to be built to "good enough" and no further. Building 20 features to mediocrity is this project's most likely failure mode.

---

## D4 — Brand palette, not Material You

**Status:** ✅ Accepted (Session 4, 2026-09-06) · **implemented**

**Decision.** `VahiTheme(dynamicColor = false)` by default.

**Why.** A notebook app benefits from a stable, recognisable identity. Both light and dark Vahi schemes were already hand-authored, and `dynamicColor = true` meant they were silently unused on Android 12+.

**Implemented in:** `ui/theme/Theme.kt`.

---

## D5 — A real product, not a portfolio prototype

**Status:** ✅ Accepted (Session 2) · confirmed with the project owner

**Decision.** Vahi is built to ship to the Play Store.

**Consequences.** Full foundations (F1–F5). Release hardening — minification, resource shrinking, a signing config, a privacy policy, a Play Data Safety declaration, crash reporting — is **real, mandatory work before launch**, not optional. See [`20-release-plan.md`](20-release-plan.md).

---

## D6 — Ink architecture: adopt `androidx.ink`

**Status:** 🔬 **Provisional** — *upgraded from 🔲 Open in Session 5 (2026-09-08) on verified evidence.*

### What changed

Sessions 2–4 treated this as the one open decision gating the whole foundation build, on the grounds that `androidx.ink` was "a relatively young API" whose release status had to be verified. **It has now been verified, and the picture is materially different from what the earlier plan assumed:**

| Earlier assumption (docs 05, 11) | Verified reality (checked 2026-09-08) |
|---|---|
| "Relatively young API — verify release status yourself" | **`androidx.ink` 1.0.0 went stable on 2025-12-17.** Latest alpha line is 1.1.0-alpha07 (2026-08-12). |
| "The authoring surface is View-based, so it needs `AndroidView` interop" | **False as of 1.0.0.** `androidx.ink:ink-authoring-compose` provides an `InProgressStrokes` **composable**. No Fragment or View interop is required. |
| "Serialisation format is defined" | Confirmed — `androidx.ink:ink-storage` is a shipped module. |
| "Gives you real stroke geometry with intersection queries" | Confirmed — `androidx.ink:ink-geometry` / `ink-geometry-compose`. |

### Decision

**Adopt `androidx.ink` 1.0.0 as the ink foundation**, unless the latency benchmark in [`21-backlog.md`](21-backlog.md) `VAHI-101` shows it performing *worse* than the current hand-rolled Compose canvas on target hardware.

Dependencies to add (pin to the stable line, not the alpha):

```
androidx.ink:ink-nativeloader:1.0.0
androidx.ink:ink-strokes:1.0.0
androidx.ink:ink-brush-compose:1.0.0
androidx.ink:ink-geometry-compose:1.0.0
androidx.ink:ink-rendering:1.0.0
androidx.ink:ink-authoring-compose:1.0.0
androidx.ink:ink-storage:1.0.0
```

### Why the recommendation is now strong rather than balanced

1. **It is the only realistic path to acceptable latency.** Compose's normal draw pipeline carries 2–3 frames of lag (input → composition → RenderThread → SurfaceFlinger → display). Serious stylus apps get under it with front-buffered rendering. *You cannot optimise your way to good ink feel inside a normal Compose `Canvas`.* Building front-buffered rendering yourself means reaching for `androidx.graphics:graphics-core` and `androidx.input:input-motionprediction` — at which point you have rebuilt the Ink API, worse.
2. **It solves three separate blocked problems at once.** Real stroke geometry unblocks accurate erasing and lasso select; `ink-storage` unblocks persistence; the brush model unblocks pressure/tilt/velocity response with graceful degradation when there is no stylus.
3. **It is the keystone for the PDF feature too.** `androidx.pdf` builds its annotation layer on `androidx.ink` (see [D8](#d8--pdf-annotation-builds-on-androidxpdf--androidxink)). Choosing a hand-rolled engine means writing the PDF annotation layer from scratch as well.
4. **It is the keystone for the differentiator.** ML Kit Digital Ink needs ordered input points with timing. `ink-strokes`' `StrokeInputBatch` is exactly that shape; a Compose `Path` is not.

**In short: three of the four things that make Vahi a product rather than a demo — good ink feel, PDF annotation, and Indic recognition — key off the same decision.**

### What is still Provisional, and the evidence that would settle it

The benchmark in `VAHI-101` must still run, because it answers questions no amount of reading answers:

| Question | How to answer it |
|---|---|
| Is latency actually acceptable **on a mid-range tablet**, not a flagship? | Measure. Target and method in [`19-quality-bar.md`](19-quality-bar.md) §2.1. |
| How much does the native library add to the APK? | Build with and without; compare per-ABI split sizes. Budget in [`19-quality-bar.md`](19-quality-bar.md) §2.4. |
| Does it behave with **finger input and no pressure data**? | Test explicitly with a finger. This is the common case for the target market — not an edge case. |
| Does `ink-authoring-compose` compose cleanly with a zoom/pan transform and a paged scroll? | Prototype one page inside a scrolling container with a transform applied. |

**If the benchmark says no**, the fallback is in [`16-canvas-spec.md`](16-canvas-spec.md) §8: keep Compose `Canvas` but fix the model properly — own point-list representation, consume `historical` samples, Bézier smoothing, pressure-modulated width, settled strokes cached into a `GraphicsLayer`. That recovers most of the *quality* and none of the *latency* win.

**Do not skip the measurement, and do not skip writing the numbers down.** But do not treat it as a coin flip either — the burden of proof now sits on *not* adopting.

**Sources:** [androidx.ink release notes](https://developer.android.com/jetpack/androidx/releases/ink) · [Ink API setup](https://developer.android.com/develop/ui/compose/touch-input/stylus-input/ink-api-setup) · [About the Ink API](https://developer.android.com/develop/ui/compose/touch-input/stylus-input/about-ink-api)

---

## D7 — Schedule expressed in engineering-days, not calendar dates

**Status:** ✅ Accepted (Session 5, 2026-09-08)

**Decision.** All estimates in this doc set are given in **engineering-days (ed)** — one uninterrupted day of one developer. No calendar dates, no sprint numbers, no ship date.

**Why.** Earlier drafts asked "solo or team, full-time or evenings?" and estimated in weeks under an assumption of ~1 full-time developer. That assumption is unverified, and a wrong assumption baked into a schedule produces a plan that is quietly wrong everywhere. Engineering-days are bandwidth-independent: multiply by your own throughput.

**Consequences.** [`21-backlog.md`](21-backlog.md) sizes every item in ed. To get a calendar estimate, apply your own days-per-week and a focus factor (0.6–0.7 is realistic for solo work with context switching).

---

## D8 — PDF annotation builds on `androidx.pdf` + `androidx.ink`

**Status:** 🔬 Provisional (Session 5, 2026-09-08)

**Decision.** Build PDF import and annotation on `androidx.pdf` rather than raw framework `PdfRenderer` or a third-party SDK — **provided** the Fragment-interop and `minSdk` costs below are acceptable when the work is scheduled.

**Verified facts (2026-09-08).**
- Latest release is **1.0.0-beta01** (2026-08-26). Not yet stable; most public APIs promoted from alpha, some still `@ExperimentalPdfApi`.
- It ships an annotation layer **built on `androidx.ink`** — pen, highlighter (free-form and snap-to-text), eraser — plus form filling and OCR.
- **`minSdk 28`** as of alpha18.
- The primary entry point is `PdfViewerFragment` / `EditablePdfViewerFragment` — **Fragment-based**, in an app that currently has zero Fragments.

**Why this and not the alternatives.**

| Option | Verdict |
|---|---|
| `androidx.pdf` | Annotation, form filling, text selection, OCR — all provided, and the annotation model is the same ink model used everywhere else. |
| Framework `PdfRenderer` | Renders pages to bitmaps and nothing else. Every annotation, text-selection and search feature is hand-built on top. Months of work. |
| Commercial SDK (Nutrient/PSPDFKit etc.) | Excellent, and priced for enterprises. Incompatible with a price-sensitive free tier. |

**The costs, stated honestly.**
1. **Beta, not stable.** Schedule PDF work such that a breaking change between beta and 1.0.0 stable is absorbable. Do not start it before the local data model is settled.
2. **`minSdk 28`.** See [D12](#d12--minsdk-stays-at-26-until-pdf-forces-28).
3. **Fragment interop.** Introduces `androidx.fragment` into a pure-Compose app. Contain it: one `AndroidFragment`/`FragmentContainerView` host inside a single Compose screen, and let nothing else in the codebase know Fragments exist.

**Evidence that would settle it:** a half-day spike (`VAHI-501`) that renders one imported PDF page, draws one annotation, and saves and reloads it. Do it when PDF work is scheduled, not now.

**Source:** [androidx.pdf release notes](https://developer.android.com/jetpack/androidx/releases/pdf)

---

## D9 — Handwriting recognition uses ML Kit Digital Ink (on-device)

**Status:** ✅ Accepted (Session 5, 2026-09-08)

**Decision.** Indic handwriting recognition uses **ML Kit Digital Ink Recognition**, running fully on-device.

**Verified facts (2026-09-08).** Recognition runs **entirely offline** — handwriting never leaves the device. Every script named in the thesis is supported, with BCP-47 model identifiers:

| Script | Languages / identifiers |
|---|---|
| Devanagari | Hindi (`hi`), Marathi (`mr`), Nepali (`ne`), Sanskrit (`sa-Deva-IN`) |
| Bengali | Bangla (`bn`), Assamese (`as`) |
| Tamil | `ta` · Telugu `te` · Gujarati `gu` · Kannada `kn` · Malayalam `ml` · Odia `or` |
| Gurmukhi | Punjabi (`pa`) |

Latin-transliteration variants (`hi-Latn`, `mr-Latn`, …) exist too — relevant, because Indian students routinely write Hindi in Latin script.

**Why this is structurally right, not just convenient.**
- **Zero marginal cost per user.** A cloud recognition API would put a per-user recurring cost on a free tier in a price-sensitive market. That is a business-model problem disguised as a technical choice.
- **Privacy is a feature, and it is honest.** "Your handwriting never leaves your tablet" is a true claim and a good one.
- **It works offline**, which [D15](#d15--offline-first-the-local-database-is-the-source-of-truth) requires.

**The one caveat that must be designed around.** Language packs are **downloaded on demand**, not bundled. First use of a language needs a network connection. So:
- Download models **explicitly and visibly**, from Settings → Languages, with size shown and a Wi-Fi-only default.
- Never block writing on a model download. Recognition is an enrichment pass over already-saved ink, never a step in the write path.
- Ship a clear empty state: "Hindi handwriting search needs a one-time 
download."

**Hard dependency.** Recognition consumes **ordered stroke input points with timestamps**. It cannot consume a rendered path. This is a third independent argument for [D6](#d6--ink-architecture-adopt-androidxink).

**Source:** [ML Kit Digital Ink Recognition](https://developers.google.com/ml-kit/vision/digital-ink-recognition)

---

## D10 — No auth wall; account is optional and requested in context

**Status:** ✅ Accepted (Session 5, 2026-09-08)

**Decision.** A first-time user goes **launch → library → writing**. No account, no sign-in screen, no skip link to hunt for. An account is requested only when the user asks for something that needs one (sync, backup, multi-device) or from Settings → Account.

**Why.** The current build shows a four-field form (username + email + password + confirm) under a button labelled "Login" before the user has seen a single thing the app does. For a free app competing against installed incumbents, that is the highest-drop-off screen it is possible to design — and it is gating **nothing**, because the app is offline-first and an account is not technically required for anything in v1.

**Consequences.**
- `AuthScreen` is **removed from the startup path**. It survives as a screen reachable from Settings → Account, rewritten per [`15-ux-blueprint.md`](15-ux-blueprint.md) §6.
- All local data is created against a local anonymous profile. Signing in later must **adopt** the existing local data, never replace it. This is a data-model requirement, not a UI one — see [`17-data-spec.md`](17-data-spec.md) §7.
- Sign in with Apple is dropped for v1 (there is no iOS app; a black button that does nothing costs credibility). Google Sign-In is the primary path when auth arrives in Track F.

---

## D11 — Drop the video splash and the Media3 dependency

**Status:** 🔬 Provisional (Session 5, 2026-09-08) — *this one is a product/brand call as much as a technical one, so it is flagged for the owner rather than assumed.*

**Decision.** Replace the ExoPlayer video splash with the **Android 12+ `SplashScreen` API** plus a short Compose brand animation, and **remove `androidx.media3` entirely**.

**Why.**

| Reason | Detail |
|---|---|
| **Time-to-ink** | The video is a forced wait on *every* launch. Time-to-ink is the metric this product lives on. See [`12-ux-plan.md`](12-ux-plan.md) Part 1. |
| **APK size** | `media3-exoplayer` + `media3-ui` is several MB of dependency, plus ~800 KB of `splash.mp4`, to display a logo. On a budget device in a price-sensitive market, install size is a real conversion factor. |
| **Cold start** | Constructing an `ExoPlayer`, preparing a media source and inflating a `PlayerView` is a lot of work on the critical path to first frame. |
| **Correctness** | It is the app's only source of dangling-resource risk and it already needed an error listener and a 6-second timeout bolted on (Session 4) to stop it stranding users. |

**Migration.** System splash (icon + brand background, no delay) → optional one-time animated wordmark on **first launch only**, skippable on tap → library. Repeat launches go straight to the library.

**What would overturn this:** the owner considering the video an essential brand asset. If so, the compromise is: keep it, first launch only, tap-to-skip, and accept the dependency. Say so and this becomes Accepted-with-video.

---

## D12 — `minSdk` stays at 26 until PDF forces 28

**Status:** ✅ Accepted (Session 5, 2026-09-08)

**Decision.** Keep `minSdk = 26` (Android 8.0) now. Raise to **28** (Android 9.0) at the point PDF import is built, not before.

**Why.** `androidx.pdf` requires `minSdk 28`. There is no reason to pay that cost before the feature that needs it exists — and no reason to fight it when it arrives, since Android 8.0-only devices are a vanishing share and are not plausible tablet-plus-stylus hardware.

**Consequence.** Do not write compatibility code for API 26–27 that PDF will make pointless. Do record the bump in the release notes when it happens — it removes devices from the Play Store eligibility list, and that is user-visible.

---

## D13 — Android Auto Backup is disabled for notebook data

**Status:** ✅ Accepted (Session 5, 2026-09-08)

**Decision.** Exclude the notebook database and ink files from Android Auto Backup and Device-to-Device transfer. Explicitly configure `backup_rules.xml` and `data_extraction_rules.xml` rather than leaving the templates in place. Keep `android:allowBackup="true"` for small preferences only.

**Why.** The manifest currently says `allowBackup="true"` with **template rules where every rule is commented out** — so once Room lands, the whole notebook database silently becomes eligible for Auto Backup. That looks like a free cloud-backup feature and is actually a hazard:

1. **The 25 MB quota.** Auto Backup silently stops backing up an app that exceeds it. A student's ink and imported PDFs will pass 25 MB quickly. Silent partial backup is worse than no backup, because the user believes they are covered.
2. **Restore is a time machine.** Restoring a device from an old backup would overwrite newer local notes with older ones and produce no conflict UI, because none exists.
3. **It pre-empts the real feature.** Proper backup/sync is Track F with a real conflict model. A half-working accidental version undermines it.

**Consequence.** Until Track F ships, "your notes live on this tablet" is the honest promise, and the app should offer **manual export** (`.vahi` archive / PDF) as the user-controlled backup path. See [`20-release-plan.md`](20-release-plan.md) §6.

---

## D14 — Single Gradle module until a named trigger fires

**Status:** ✅ Accepted (Session 5, 2026-09-08)

**Decision.** Stay on one `:app` module. Split into `:core:*` / `:feature:*` only when **both** of these are true:
1. A clean debug build exceeds **90 seconds** on the development machine, **and**
2. The data layer has landed and stabilised (so module boundaries are drawn along real seams rather than guessed ones).

**Why.** At ~2,800 lines, module splitting costs real time and buys nothing. Naming the trigger prevents both premature splitting and the opposite failure of never revisiting it.

**Consequence.** The target package structure in [`18-architecture-target.md`](18-architecture-target.md) is designed so that the split, when it comes, is a directory move rather than a redesign.

---

## D15 — Offline-first: the local database is the source of truth

**Status:** ✅ Accepted (Session 2, restated Session 5)

**Decision.** The on-device database is authoritative. Sync, when it exists, is a background reconciliation process. **No user-facing operation may ever block on the network.**

**Why.** Connectivity in the target market is intermittent, and this is a note-taking app, where losing work is unforgivable.

**Consequences.**
- Every entity carries the fields sync will need from day one — stable UUIDs, `updatedAt`, tombstones — even though nothing syncs yet. Retrofitting them means a migration over user data.
- No "Save" button, ever. Auto-save continuously; show a quiet "Saved" indicator. A save button implies work can be lost.
- Track F (accounts and sync) is deliberately last.

---

## D16 — Freemium with a generous free tier, no page caps

**Status:** 🔬 Provisional (Session 5, 2026-09-08)

**Decision.** Vahi Free is **unlimited notebooks, unlimited pages, unlimited local writing, all drawing tools, and on-device recognition for one language**. Vahi Plus adds cloud sync/backup, unlimited recognition languages, PDF export at high resolution, and advanced AI study features.

**Why.** The current UI has an "Upgrade" pill and an "Upgrade to Plus" row with no tier definition behind them — monetisation hooks with no monetisation design. The natural instinct is to cap notebooks or pages. **Do not.** For a note-taking app, capping the user's own content is the one thing that reliably destroys trust: the moment a student hits "you can't add another page to your Physics notes", they lose their work in practice and never come back. Charge for **conveniences that cost you money** — server storage, sync — not for the user's own writing.

**Consequences.**
- Remove the permanent "Upgrade" pill from the top bar. Surface upgrade contextually: when the user taps Sync/Backup, in Settings, and as one dismissible library card after they have real content. See [`12-ux-plan.md`](12-ux-plan.md) §4.6.
- No paywall ships in v1 — there is nothing to sell until sync exists. Ship v1 fully free, add Plus in Track F.
- Full tier table: [`14-product-spec.md`](14-product-spec.md) §6.

**What would overturn this:** the owner having a different monetisation model in mind (one-time purchase, ads, education licensing). Worth a deliberate answer before Track F.

---

## D17 — Stay Android-only for now — cross-platform is feasible, but not free

**Status:** 🔲 **Open** (Session 6, 2026-09-08) — genuinely the owner's call, researched but not decided.

**The question, as asked.** Should Vahi work on iOS as well as Android, and on phones and iPads as well as tablets?

**The recommendation: not yet — revisit after v1 ships on Android, not before.** Stated plainly because the evidence points one way, but this is flagged **Open** rather than **Accepted** because it is fundamentally a product/business call, not a technical one, and it directly contradicts two decisions ([D2](#d2--tablet-only-for-v1), [D3](#d3--product-thesis-student-notebook-android-tablets-india)) that were made deliberately and confirmed with the owner. Overriding those requires the owner's explicit say, not an engineer's inference.

### What's technically true, verified 2026-09-08

**The right path, if this is pursued, is Kotlin Multiplatform + Compose Multiplatform — not Flutter, not React Native, not separate native codebases.** Vahi is already 100% Kotlin + Jetpack Compose, and Compose Multiplatform for iOS has been stable and production-ready since Compose Multiplatform 1.8.0 (May 2025) — companies including Netflix and Cash App run it in production. Rewriting in a different framework would throw away everything built so far for no benefit; KMP is a *restructuring* of the existing codebase, not a rewrite.

**What ports cleanly, with real multiplatform support already confirmed:**

| Piece | Multiplatform status |
|---|---|
| `core/model/` (coordinate types, `PaperStyle`) | Already pure Kotlin, zero Android dependency — verified this session (its 24 tests need no Robolectric). Ports as-is. |
| `domain/` (`Notebook`, `NotebookRepository`) | Already pure Kotlin. Ports as-is. |
| `kotlinx.serialization` (the typed nav routes) | Already multiplatform by design. |
| **Room** | Has real KMP support since 2.7.0 via `androidx.sqlite` (a genuine cross-platform SQLite driver, not a shim) — confirmed working on iOS. Room 3.0 (alpha, 2026) extends this further. The `NotebookEntity`/`NotebookDao`/`VahiDatabase` built this session would very likely port with minimal change. |
| `navigation-compose` | Has a multiplatform build, but under a **different Maven coordinate** (`org.jetbrains.androidx.navigation`, not `androidx.navigation`) — a real but mechanical migration. iOS swipe-back needs extra platform-specific glue. |

**What needs real, separate work:**

| Piece | The problem |
|---|---|
| **Hilt** | Confirmed Android-only — it is Dagger-based and has no KMP path. The multiplatform-standard replacement is **Koin**. This is a genuine migration of everything built in this session's DI work, not a recompile. |
| **Media3/ExoPlayer** (splash video) | Android-only; iOS needs AVFoundation. Moot if [D11](#d11--drop-the-video-splash-and-the-media3-dependency) is accepted and the video splash is dropped anyway. |
| **Phone/Compact layouts** | Supporting phones and iPads properly means building the responsive layouts [D2](#d2--tablet-only-for-v1) explicitly deferred as "weeks of speculative layout work" — for every screen, not just one. |

**The one finding that actually matters most: the ink engine cannot be shared.**

`androidx.ink` — the library [D6](#d6--ink-architecture-adopt-androidxink) recommends adopting, specifically *because* it solves low-latency authoring, real stroke geometry, and the serialisation format all at once — is **confirmed Android-only**, with no iOS or KMP support and nothing announced. On iOS, the equivalent is Apple's own **PencilKit**, a completely different API with no code-sharing path. This means:

- The single most complex, highest-effort, most latency-critical part of the entire app — the part [D6](#d6--ink-architecture-adopt-androidxink) alone budgets at 1–2 weeks for Android *by itself* — would need a **second, fully separate implementation** for iOS, built on entirely different platform APIs.
- The `InkEngine` interface abstraction already designed in [`18-architecture-target.md`](18-architecture-target.md) §3.1 turns out to be exactly the right shape to contain this (a `PencilKitInkEngine` behind the same interface) — good foresight, even though it wasn't originally designed with iOS in mind. That does not make building the second engine cheap; it only stops it from being architecturally messy.

**Why this cuts against the product thesis, not just the engineering.** [D3](#d3--product-thesis-student-notebook-android-tablets-india) chose Android tablets specifically *because* "the best handwriting apps are iPad-first... being the app that feels best to write on, on Android, is a real, narrow, achievable claim" — i.e., iPad is already dominated by mature incumbents (GoodNotes, Notability), and that's precisely why the thesis pointed away from it. Two pieces of evidence found this session reinforce that this gap is real, not just competitive framing:

- **Latency:** Apple's PencilKit achieves a remarkably *consistent* 22–28 ms system latency across iPad hardware generations (±0.9 ms over long sessions) — a product of Apple's tight hardware/software integration. Real-world Android stylus testing (Samsung S Pen Ultra, Logitech Crayon) measured 41–69 ms with much higher variance, despite `androidx.ink` being theoretically capable of ~4 ms on ideal hardware. Entering the iPad market means matching a bar Apple's own stack is unusually good at holding — with a brand-new, unproven second ink engine.
- **Market shape:** in India specifically, Samsung leads Android tablets at ~31% share on affordability and variety (matching Vahi's price-sensitive target user), while Apple holds the premium segment where GoodNotes/Notability are already entrenched. Going to iPad means competing head-on in the segment the incumbents already own, not the one D3 identified as underserved.

### The honest cost, if pursued anyway

Not a fixed increment — closer to **roughly doubling** the remaining engineering effort, not a percentage on top of it: re-platforming the UI layer onto Compose Multiplatform, replacing Hilt with Koin, migrating the navigation dependency, building phone/iPad-responsive layouts for every screen (reversing D2), building and tuning a **second ink engine on PencilKit** from scratch, and maintaining double the QA surface (two platforms, two stylus ecosystems) for every feature from here on.

**What would make this the right call anyway:** if the actual goal is a broader cross-device note-taking product rather than the specific Android-tablet-and-Indic-recognition wedge D3 describes — that is a legitimate different thesis, just a different one, and it's the owner's to choose. It would also be far cheaper to decide *now*, before more Android-only foundation work (the ink engine itself, `VAHI-101`/`VAHI-102`) gets built, than after.

**What would settle this:** a direct answer from the owner to the tension above — is the goal still "best on Android tablets, with Indic recognition as the wedge no incumbent has bothered with" (D2/D3, stay Android-only, revisit iOS as a v2+ expansion once that's proven), or is it "a cross-device product from the start" (accept the roughly-doubled scope, start the KMP restructuring now, before sinking more effort into Android-only foundations)? Both are coherent; they are not the same product.

---

## V1 — App theme and Paper theme are separate settings

**Status:** 🔬 Provisional

**Decision.** Two independent settings, as GoodNotes does:

| Setting | Options |
|---|---|
| **App theme** | Light · Dark · Follow system |
| **Paper theme** | White · Cream · Dark |

When Paper is Dark, render dark paper with light ink **for display only**. Store ink colours unchanged; always export light.

**Why.** "Dark mode" means something unusual in a notebook app. White paper in a dark room is genuinely unpleasant — it is the brightest thing on screen. But inverting the paper changes what the user's notes *look like*, and exported or printed output must stay light. Separating the two settings resolves the conflict instead of picking a side.

**Consequence.** The document stays canonical; only presentation changes. Requires a display-time ink-colour mapping in the render path — cheap if designed in, invasive if bolted on. Specified in [`16-canvas-spec.md`](16-canvas-spec.md) §7.

---

## V2 — Font family: Noto Sans + Noto Sans Devanagari

**Status:** 🔬 Provisional

**Decision.** Bundle **Noto Sans** for Latin UI text and **Noto Sans Devanagari** for Devanagari, both as app assets rather than downloadable fonts. Add further Noto Indic faces as those languages ship.

**Why.** `FontFamily.Default` (Roboto) does not cover Indic scripts; Devanagari falls back to whatever each manufacturer ships, producing inconsistent rendering across devices — unacceptable when Hindi is a launch language. Noto exists precisely to solve this, the two faces are metrically compatible, and one family means no pairing work. Inter is a slightly better Latin face; it is not worth managing a second fallback chain for.

**Consequences.**
- Bundling costs APK size — budget in [`19-quality-bar.md`](19-quality-bar.md) §2.4. Ship only the weights actually used (Regular, Medium, SemiBold).
- **Verify glyph coverage before committing.** Render a test string in each target language and look at it.
- A distinctive display face for the wordmark only is still open, and is a brand question, not a typography one.

---

## V3 — Lavender stays as the brand accent

**Status:** 🔬 Provisional

**Decision.** Keep the existing lavender/purple family (`#7C4DFF`, `#B8B5FF`, `#C4BFFF`) as the brand accent, and keep the warm off-whites (`#F8F6F4`, `#FAF8F6`, `#F5F3F1`) as the surface family.

**Why.** The lavender reads as neither Google-blue nor GoodNotes-blue — it is genuinely distinctive in this category. The warm off-whites read as paper rather than as UI chrome, which is exactly right for a notebook app. Both instincts in the existing design are good; the problem was never the palette, it was that the palette is bypassed by hardcoded literals.

**Consequence.** The work is systematisation, not redesign — a two-layer token system per [`13-visual-design-system.md`](13-visual-design-system.md) §3.1. Flagged Provisional only because the accent propagates everywhere and deserves one deliberate confirmation.

---

## V4 — Figma is the visual source of truth; the docs describe behaviour

**Status:** 🔬 **Provisional** — *answered in Session 5 (2026-09-08). The file exists but has not yet been read.*

**The file.** The project owner has an existing Figma design, authored by them:

> **`vahi-v1`** — https://www.figma.com/design/JJAGOm1by1gHE6Nxe4yUhx/vahi-v1?node-id=0-1

**Decision.** When Figma and this doc set disagree about **how something looks**, Figma wins. When they disagree about **how something behaves**, the specs win ([`15-ux-blueprint.md`](15-ux-blueprint.md), [`16-canvas-spec.md`](16-canvas-spec.md)). Neither is subordinate overall; they own different questions.

| Question | Authority |
|---|---|
| Colour values, type sizes, spacing, corner radii, iconography, layout composition, illustration | **Figma** |
| Which screens exist, what states each has, what each control does, gesture behaviour, navigation, error handling, accessibility requirements | **The specs** |
| Anything the Figma does not cover (loading/empty/error states, tablet split-screen, handedness mirroring, dark mode, 200 % font scale, Hindi text expansion) | **The specs**, until Figma covers it |

**Why not simply "Figma wins".** A static design file cannot express a state machine, a gesture-arbitration rule, or what happens when storage is full — and those are most of what makes this app work. Making Figma authoritative over behaviour would mean the many undesigned states get invented ad hoc at implementation time, which is exactly the failure the specs exist to prevent.

**Status is Provisional because the file has not been read.** It is login-gated, so it could not be opened during Session 5. Until it is reconciled, [`13-visual-design-system.md`](13-visual-design-system.md) may propose values that contradict the actual design.

**To resolve (tracked as `VAHI-004`):** provide **either**
- PNG/PDF exports of the key frames (library, editor, create-notebook, settings), **or**
- a Figma *view* link with public access enabled, **or**
- the design tokens (colour styles, text styles, spacing) exported as a list.

Then [`13-visual-design-system.md`](13-visual-design-system.md) Parts 3–6 get rewritten against the real values, and [`15-ux-blueprint.md`](15-ux-blueprint.md) wireframes get annotated with which frames they correspond to.

**Blocks:** any significant visual build-out. Does **not** block foundation work (F1–F5), which is where implementation starts anyway — so this is not on the critical path today.

---

## V5 — Empty-state illustration style

**Status:** 🔲 **Open**

**Question.** What illustration style — and from what source — for empty states and onboarding?

**Options.** (a) Commission/draw a small custom set; (b) use a permissively-licensed open set; (c) skip illustration entirely and use large muted icons plus good copy.

**Recommendation:** **(c) for v1.** Well-set type and a single large muted glyph reads as deliberate and costs nothing. Half-hearted stock illustration reads as unfinished — which is the exact problem empty states are meant to fix.

**Blocks:** the empty-state work in `VAHI-208`. Not blocking anything earlier.

---

## Appendix — ADR template

Copy this block when adding a decision. Keep entries in the index table at the top in sync.

```markdown
## D<n> — <one-line decision, stated as a fact>

**Status:** ✅ Accepted | 🔬 Provisional | 🔲 Open | ⛔ Superseded by D<m> (Session <n>, YYYY-MM-DD)

**Decision.** What was decided, in the imperative or as a statement of fact.

**Why.** The reasoning. Include the alternatives considered and why they lost —
a decision without its rejected alternatives is not reusable.

**Consequences.** What this forces elsewhere. Link to the specs that implement it.

**What would overturn this.** The specific evidence that should reopen it.
If nothing could, say "nothing — this is a product commitment."
```

**Rules for this file**

1. **Never edit a decision's history.** To change a decision, mark the old one ⛔ Superseded and write a new one. The trail is the value.
2. **Every 🔬 Provisional must name its evidence.** "Provisional" without a named test is just an opinion with a hedge.
3. **Every 🔲 Open must name what it blocks.** An open question that blocks nothing is not a decision, it is a note.
4. Cross-reference from specs to here, never the reverse — specs move, decisions do not.
