# Vahi

**Vahi** is a native Android digital notebook app — a tablet-first, stylus-oriented note-taking surface built entirely in Kotlin with Jetpack Compose.

The name comes from *vahi* (वही) — the traditional Indian ledger/notebook.

> **Status: early prototype, fully planned.** The UI shell (splash → auth → home → canvas) is built and navigable, freehand drawing works, and the canvas has a tool rail. There is **no persistence, no backend, no real authentication, and no ViewModel wiring** — everything lives in in-memory Compose state and is lost when you leave a screen or rotate the device.
>
> Detailed planning is complete: 23 documents covering product scope, UX, the canvas, the data model, target architecture, quality budgets, release, a numbered backlog and a decision log. **Implementation has not started.** See [`docs/00-START-HERE.md`](docs/00-START-HERE.md).

**Design file:** [`vahi-v1` on Figma](https://www.figma.com/design/JJAGOm1by1gHE6Nxe4yUhx/vahi-v1?node-id=0-1) — the visual source of truth. Not yet reconciled with the docs ([`22-decisions.md` V4](docs/22-decisions.md)).

---

## At a glance

| | |
|---|---|
| **Platform** | Android (native) |
| **Language** | Kotlin 2.0.21 |
| **UI toolkit** | Jetpack Compose + Material 3 (Compose BOM 2024.09.00) |
| **Application ID** | `com.vahi.app` |
| **min / target / compile SDK** | 26 / 36 / 36 |
| **Build system** | Gradle 8.13 (Kotlin DSL) + AGP 8.13.2, version catalog |
| **Java target** | 11 |
| **Modules** | Single module: `:app` |
| **Architecture (intended)** | MVVM + Clean-ish layering (`ui` / `domain` / `data`) |
| **Architecture (actual)** | Single-activity Compose UI; state held in composables |
| **Third-party deps** | Media3 ExoPlayer (splash video) — nothing else beyond AndroidX |
| **Tests** | Only the two generated template tests |

## What it does today

1. **Splash** — plays a bundled video (`assets/splash.mp4`) via ExoPlayer over an animated gradient, then advances automatically when playback ends.
2. **Auth** — a two-pane tablet login/signup screen (form on the left, "Vahi — Your Digital Notebook" branding on the right). The Login button navigates onward unconditionally; there is no credential check.
3. **Home** — a notebook library: top bar (logo, Upgrade, Settings, profile avatar), filter tabs, a toolbar (search, view toggles, sort, Import), and a grid of notebook cards plus a "Create new notebook" tile. Settings / profile / per-card menus open as anchored dropdown overlays; "Create" opens a full-screen modal that blurs the content behind it. **Almost every control here is a no-op** — the notebook list is two hardcoded items.
4. **Canvas** — pick Blank, Ruled, or Grid and you land on a fullscreen page with a transparent drawing layer on top. Freehand pen strokes render live. A left-edge tool rail gives pen/eraser, five colours, three widths, undo/redo and clear. A back control returns you home — and everything you drew is discarded.

## Quick start

```bash
# from the repo root
./gradlew assembleDebug          # build the debug APK
./gradlew installDebug           # build + install on a connected device/emulator
./gradlew test                   # JVM unit tests
./gradlew connectedAndroidTest   # instrumented tests (device required)
```

Or open the project in Android Studio and hit Run.

> **Use a real tablet.** The Auth screen only renders its real layout at the `Expanded` window class (≥ 840 dp wide); on a phone it shows an honest "Vahi works best on a tablet" message with no way to continue (deliberate — decision D2). Home and Canvas are laid out for tablet dimensions too.
>
> **An emulator is not enough.** Nothing in this project has ever been verified on real hardware, and the two most serious known bugs — a resting palm drawing instead of the stylus, and an unrecoverable Clear button — can only be reproduced with a physical tablet and a stylus.

## Documentation

**New here — or resuming after a break? Start with [`docs/00-START-HERE.md`](docs/00-START-HERE.md).** It orients you in 60 seconds, lists the settled decisions, and contains a copy-pasteable prompt for resuming in a fresh session.

| Doc | What's in it |
|---|---|
| [`docs/00-START-HERE.md`](docs/00-START-HERE.md) | **Entry point.** Current phase, settled decisions, doc map, resume prompt, doc-maintenance protocol |
| [`docs/01-overview.md`](docs/01-overview.md) | What Vahi is, the product idea, core concepts and vocabulary |
| [`docs/02-architecture.md`](docs/02-architecture.md) | Package layout, layering, state management, patterns in use |
| [`docs/03-app-flow.md`](docs/03-app-flow.md) | Navigation model and end-to-end user flow, with diagrams |
| [`docs/04-screens.md`](docs/04-screens.md) | Screen-by-screen reference: composables, state, callbacks |
| [`docs/05-drawing-engine.md`](docs/05-drawing-engine.md) | The stroke model, `CanvasState`, input handling, rendering |
| [`docs/06-design-system.md`](docs/06-design-system.md) | Theme, colors, typography, shapes, responsive breakpoints |
| [`docs/07-build-and-tooling.md`](docs/07-build-and-tooling.md) | Gradle config, dependencies, SDK levels, assets, icons |
| [`docs/08-file-reference.md`](docs/08-file-reference.md) | Every source file and what it contains |
| [`docs/09-project-status.md`](docs/09-project-status.md) | What works, what's a stub, known bugs and gaps |
| [`docs/10-roadmap.md`](docs/10-roadmap.md) | A menu of implied next steps (superseded by 11 for ordering) |
| [`docs/11-forward-plan.md`](docs/11-forward-plan.md) | **Why the plan is shaped this way** — the five-foundations reframe, the wasted-work rule |
| [`docs/12-ux-plan.md`](docs/12-ux-plan.md) | UX principles — time-to-ink, the auth wall, accessibility, localisation |
| [`docs/13-visual-design-system.md`](docs/13-visual-design-system.md) | Visual design — colour tokens, typography, iconography, motion, components |

**Specifications — read before building the thing they cover**

| Doc | Read before you… |
|---|---|
| [`docs/14-product-spec.md`](docs/14-product-spec.md) | …argue about scope. v1 in/out, personas, monetisation, exit criteria |
| [`docs/15-ux-blueprint.md`](docs/15-ux-blueprint.md) | …build a screen. Inventory, nav graph, wireframes, every state |
| [`docs/16-canvas-spec.md`](docs/16-canvas-spec.md) | …touch the canvas. Coordinates, pointer state machine, palm rejection, rendering |
| [`docs/17-data-spec.md`](docs/17-data-spec.md) | …write anything to disk. Schema, `.ink` format, migrations |
| [`docs/18-architecture-target.md`](docs/18-architecture-target.md) | …add a ViewModel, a dependency, or a package |
| [`docs/19-quality-bar.md`](docs/19-quality-bar.md) | …call anything done. Budgets, device matrix, a11y, tests, definition of done |
| [`docs/20-release-plan.md`](docs/20-release-plan.md) | …think about shipping |

**Execution**

| Doc | What's in it |
|---|---|
| [`docs/21-backlog.md`](docs/21-backlog.md) | **The execution spine** — 75 numbered work items, sized, dependency-ordered |
| [`docs/22-decisions.md`](docs/22-decisions.md) | **Authoritative decision log** — 22 ADRs with reasoning and rejected alternatives |
| [`docs/PROJECT-LOG.md`](docs/PROJECT-LOG.md) | Append-only session-by-session record of what happened and why |

**Reading paths**

- *What exists today:* `01` → `03` → `09`
- *Why the plan looks like this:* `11` (Parts 3 and 5) → `14`
- *About to write code:* `22` → `21` → the relevant spec from `15`–`20`
- *Resuming cold:* `00` → `09` → `21` → newest entry in `PROJECT-LOG.md`

**Docs `01`–`09` describe reality (as-built). Docs `10`–`22` describe intent (plans and specs).** Keep that split honest — see [`docs/00-START-HERE.md`](docs/00-START-HERE.md) §7, which is the standing documentation-maintenance protocol.
