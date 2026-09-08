# CLAUDE.md — operating guide for AI sessions on Vahi

Read this before doing anything. Then read `docs/00-START-HERE.md`.

---

## What this project is

**Vahi** — a native Android handwriting notebook for students on Android tablets, Indian market. Kotlin + Jetpack Compose, single module `:app`, package `com.vahi.app`.

Working UI shell + freehand drawing + a canvas tool rail. **No persistence, no backend, no ViewModels, no navigation library.** Planning is complete; implementation has not started.

---

## Non-negotiable rules

1. **Plan before implementing. Wait for an explicit go-ahead before writing application code.** The project owner plans first, deliberately. Proposing code is fine; writing it unasked is not.

2. **Update the documentation in the same session as the change**, before reporting work as done. The protocol is `docs/00-START-HERE.md` §7. `docs/PROJECT-LOG.md` gets an entry **every session, without exception** — it is the continuity mechanism when a session ends abruptly.

3. **The coordinate model must land before any persistence.** `VAHI-110` before `VAHI-301`. Strokes are currently stored in screen pixels; persisting them produces permanently unmigratable data. This is the rule that gets broken under schedule pressure. Do not break it.

4. **Never commit unless asked.** The repo has one commit and a large uncommitted tree — this is a known risk (`VAHI-001`), not licence to commit on your own initiative.

5. **When docs and code disagree, the code is right and the doc is a bug.** Verify claims against the source before writing them down. The doc set's only value is accuracy.

6. **Never state a measurement without its device.** "60 fps" is meaningless; "58 fps on the T2 reference tablet, Android 14" is a measurement.

---

## Where things are

| Need | File |
|---|---|
| Orientation, current phase, next action | `docs/00-START-HERE.md` |
| What's broken (20 ranked bugs) | `docs/09-project-status.md` |
| The numbered work items | `docs/21-backlog.md` |
| Why a decision was made | `docs/22-decisions.md` — **authoritative** |
| Building a screen | `docs/15-ux-blueprint.md` |
| Touching the canvas | `docs/16-canvas-spec.md` |
| Writing to disk | `docs/17-data-spec.md` |
| Adding a ViewModel / dependency / package | `docs/18-architecture-target.md` |
| Calling something done | `docs/19-quality-bar.md` §5 |
| Session history | `docs/PROJECT-LOG.md` |

Docs `01`–`09` describe **reality**. Docs `10`–`22` describe **intent**. Do not blur them: when a plan gets built, move the description into the as-built doc and mark it done in the backlog.

---

## Commands

```bash
./gradlew :app:compileDebugKotlin   # fast check
./gradlew :app:assembleDebug        # full build — run this before claiming success
./gradlew :app:testDebugUnitTest    # JVM unit tests
./gradlew installDebug              # needs a connected device
```

`assembleDebug` is the honest check — `compileDebugKotlin` misses resource-merging failures, which is how deleted drawables and colours bite.

---

## Conventions

**Kotlin / Compose**
- State flows *down* as parameters; events flow *up* as lambdas. The existing code is consistent about this — preserve it as ViewModels arrive.
- Screen composables are stateless and previewable. The ViewModel is collected in a thin `Route` wrapper.
- Screen state models loading/content/empty/error as **one exhaustive `phase`**, never as separate booleans.
- One-shot effects (navigation, snackbars) go through a channel, not through state — they must not replay on rotation.

**Things that are currently violated everywhere and must not get worse**
- **No hardcoded colours.** `Color.kt`'s own KDoc mandates this and dozens of inline `Color(0xFF…)` literals ignore it. Any file you touch, fix as you go.
- **No hardcoded strings.** `strings.xml` has exactly one entry. The cost of fixing this grows with every screen.
- **No emoji as icons.** Twelve are in use. They render differently per manufacturer and screen readers announce them as "bird" and "abacus".
- **`contentDescription` on every interactive element.** Accessibility is currently close to zero.
- **48 dp minimum touch targets.** Several existing controls are 28–40 dp. Watch modifier order: `.size(48.dp).padding(4.dp).clickable{}` gives a **40 dp** target.

**Definition of done** — `docs/19-quality-bar.md` §5. Abbreviated: builds clean · tested · no hardcoded colours or strings · a11y labels · states handled · **run on a real device** · docs updated · backlog status updated.

---

## Context that saves you re-deriving it

- **`androidx.ink` 1.0.0 is stable** (released 2025-12-17) and **Compose-native** — `ink-authoring-compose` provides an `InProgressStrokes` composable, so no `AndroidView` interop is needed. Older notes in `docs/05` and `docs/11` claimed the authoring surface was View-based; they have been corrected, but the wrong claim may still be quoted somewhere.
- **`androidx.pdf` builds its annotation layer on `androidx.ink`**, so the two decisions are coupled.
- **ML Kit Digital Ink runs fully on-device** and covers all nine target Indic scripts.
- **Nothing has ever been run on a device.** Every session so far has had no GUI. Bugs #13–#20 were found by code review and have not been reproduced. Say so rather than implying verification you did not do.
- **`BaseViewModel` compiles only through transitive dependencies** — `lifecycle-viewmodel` and `kotlinx-coroutines` are not declared.
- **The Figma design exists** and has not been read: https://www.figma.com/design/JJAGOm1by1gHE6Nxe4yUhx/vahi-v1?node-id=0-1

---

## Failure modes specific to this project

| Trap | Why it's a trap |
|---|---|
| Fixing the 20 bugs one at a time | Most are symptoms of five missing foundations. `docs/11` Part 3. |
| Adding `android:configChanges` to fix rotation | Hides the symptom, leaves the app broken after a process kill — the case that actually loses notes. |
| Wiring up the ~20 no-op buttons | They all need a repository. Doing it against the hardcoded 2-item list is work you throw away twice. |
| Optimising the grid canvas | It is being replaced by the page model. `docs/22` D1. |
| Splitting `HomeScreen.kt` before its ViewModel | You would refactor the same 990 lines twice. |
| Adding Room, Hilt or Navigation onto the current dependency baseline | Compose BOM 2024.09.00 and 2023-era AndroidX against `compileSdk 36`. Upgrade first (`VAHI-002`). |
| Building phone layouts | Tablet-only for v1 by decision (D2). |
| Assuming a pressure-sensitive stylus | The target market often has none. Design for the finger; treat stylus data as an enhancement. |
