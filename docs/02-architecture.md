# 02 — Architecture

## Shape of the app

Vahi is a **single-module, single-Activity, 100% Jetpack Compose** Android app.

```
MainActivity (the only Activity, the only Android component in the manifest)
  └── VahiTheme            ← Material 3 theming
        └── AppRoot        ← a navigation-compose NavHost (Session 6)
              └── one of: SplashScreen | AuthScreen | HomeScreen | CanvasScreen
```

There are no Fragments, no XML layouts, no Services, no BroadcastReceivers, no ContentProviders. `AndroidManifest.xml` declares exactly one `<activity>` and requests **no permissions at all**.

## Package layout

> Corrected in Session 6 — this tree had drifted from the code since Session 4 (it still listed `NotebookScreen.kt`, deleted that session, and omitted `DrawingToolbar.kt`, added that session). Verified against `find app/src/main/java -name '*.kt'` as of this edit.

```
com.vahi.app
├── MainActivity.kt                     Entry point. setContent { VahiTheme { AppRoot() } }
│
├── ui/
│   ├── theme/                          Design system
│   │   ├── Color.kt                    Colour constants + the 5-swatch ink palette
│   │   ├── Type.kt                     Full Material 3 type scale
│   │   ├── Shape.kt                    Corner-radius scale
│   │   └── Theme.kt                    VahiTheme — assembles the MaterialTheme
│   │
│   ├── components/                     Reusable, screen-agnostic pieces
│   │   ├── WindowInfo.kt               windowClass() responsive breakpoints
│   │   └── drawing/                    The ink engine
│   │       ├── Stroke.kt               Data model for one pen mark
│   │       ├── CanvasState.kt          Strokes, undo/redo, tool/colour/width, undo-clear
│   │       ├── DrawingCanvas.kt        Composable: pointer capture + rendering
│   │       └── DrawingToolbar.kt       Floating tool rail (Session 4; fixed Session 6)
│   │
│   └── screens/                        One file (roughly) per destination
│       ├── AppRoot.kt                  NavHost + typed routes (Session 6)
│       ├── SplashScreen.kt             Video splash
│       ├── AuthScreen.kt               Login / signup
│       ├── HomeScreen.kt               Notebook library + all its overlays
│       ├── CanvasScaffold.kt           CanvasType enum + CanvasScreen (layered canvas host)
│       ├── BlankCanvas.kt              Paper style: plain white
│       ├── RuledCanvas.kt              Paper style: vertical rules
│       └── GridCanvas.kt               Paper style: scrollable dot grid
│
├── viewmodel/
│   └── BaseViewModel.kt                ⚠️ written but never instantiated
│
├── domain/
│   ├── models/package-info.kt          ⚠️ empty marker file
│   └── usecases/package-info.kt        ⚠️ empty marker file
│
└── data/
    ├── repository/package-info.kt      ⚠️ empty marker file
    └── datasource/package-info.kt      ⚠️ empty marker file
```

**Deleted in Session 6:** `AppDestination.kt` (replaced by typed routes in `AppRoot.kt`) and `NotebookCanvasScreen.kt` (it declared a `NotebookType` enum whose only job was a 1:1 map to `CanvasType` before delegating to `CanvasScreen` — with real typed navigation there was no longer a reason for two identical enums or the adapter between them; `CanvasType` alone is now the route argument, see §Navigation below).

## Intended vs. actual architecture

The package skeleton declares an intent: **MVVM layered over a Clean-Architecture-style split**.

```
ui  →  domain (use cases, models)  →  data (repositories, data sources)
```

The four `package-info.kt` files are placeholders staking out those layers. Their contents are literally two comment lines and a `package` declaration each — for example:

```kotlin
// Package marker for data.repository
// This package will contain repository implementations
package com.vahi.app.data.repository
```

**As of today, none of that layering is wired up.** The real architecture is:

```
Composable  →  remember { ... }  →  local in-memory state  →  discarded on exit
```

`BaseViewModel<State>` is a well-formed generic MVVM base — `MutableStateFlow` behind a read-only `StateFlow`, with `updateState(reducer)` and `setState(newState)` — plus two marker interfaces, `UiState` and `UiEvent`. Nothing extends it. When the domain/data layers get built, this is the class to inherit from.

> **Dependency note:** `BaseViewModel` imports `androidx.lifecycle.ViewModel` and `kotlinx.coroutines.flow.*`, but neither `lifecycle-viewmodel-compose` nor `kotlinx-coroutines` is declared in `app/build.gradle.kts`. It compiles today only because those artifacts arrive transitively (via `activity-compose` / `lifecycle-runtime-ktx`). Declare them explicitly before relying on ViewModels.

## State management

There are three distinct patterns in play. Two are still Compose-local; navigation position is not, as of Session 6.

### 1. Navigation position, owned by `NavController` (Session 6)
```kotlin
val navController = rememberNavController()
NavHost(navController, startDestination = Splash) { /* … */ }
```
`AppRoot` no longer holds `destination`/`selectedNotebookType` as `remember`ed variables. `rememberNavController()` holds the back stack and, via its built-in `SavedStateHandle`, restores which destination is current — and any typed arguments it carries — across process death, not merely recomposition. Children never mutate navigation state directly; they invoke callbacks (`onTimeout`, `onLoginSuccess`, `onNavigateToCanvas`, `onBack`) and `AppRoot` calls `navController.navigate(...)` / `popBackStack()`. See **Navigation** below.

### 2. Screen-local UI state
`HomeScreen` owns six `remember { mutableStateOf(...) }` values for its overlays (modal open, which dropdown is open, two anchor positions). It also defines local helper functions — `openCardMenu`, `openSettings`, `openProfile`, `closeDropdowns`, `openCreateNotebook` — that enforce mutual exclusivity: opening any one dropdown closes the other two.

### 3. A plain state-holder class
`CanvasState` is not a ViewModel and not a data class. It's an ordinary class whose fields are Compose observables:

```kotlin
private val _strokes = mutableStateListOf<Stroke>()
var activeTool: ToolType by mutableStateOf(ToolType.PEN)
```

Created with `remember { CanvasState() }` inside `CanvasScreen`. This is the idiomatic Compose "state holder" pattern — and it's the right home for the ink model — but because it's `remember` and not `rememberSaveable`/ViewModel-scoped, **every stroke is destroyed on configuration change or on navigating away.**

### The unidirectional data flow convention
The codebase is consistent about this: state flows *down* as parameters, events flow *up* as lambdas. Every screen composable takes callbacks (`onBack: () -> Unit`, `onNavigateToCanvas: (CanvasType) -> Unit`) rather than reaching for a shared object. That convention held up well through the Session 6 navigation rewrite and is worth preserving as ViewModels get introduced.

## Navigation

**As of Session 6, this is `androidx.navigation:navigation-compose` with type-safe (`kotlinx.serialization`-backed) routes.** `AppRoot.kt` is a `NavHost`:

```kotlin
@Serializable private data object Splash
@Serializable private data object Auth
@Serializable private data object Library
@Serializable private data class Editor(val canvasType: CanvasType)

NavHost(navController, startDestination = Splash) {
    composable<Splash> { SplashScreen(onTimeout = {
        navController.navigate(Auth) { popUpTo(Splash) { inclusive = true } }
    }) }
    composable<Auth> { AuthScreen(onLoginSuccess = {
        navController.navigate(Library) { popUpTo(Auth) { inclusive = true } }
    }) }
    composable<Library> { HomeScreen(onNavigateToCanvas = { canvasType ->
        navController.navigate(Editor(canvasType))
    }) }
    composable<Editor> { backStackEntry ->
        val editor: Editor = backStackEntry.toRoute()
        CanvasScreen(canvasType = editor.canvasType, onBack = { navController.popBackStack() })
    }
}
```

What this replaced, and why it matters:

| Before (through Session 5) | Now |
|---|---|
| `AppDestination` enum + hand-rolled `when` | Typed `@Serializable` route objects/classes, one `composable<T>` block each |
| `selectedNotebookType` — a second piece of state manually kept in sync with `destination`; if it was ever `null` while `destination == NOTEBOOK_CANVAS`, the screen silently rendered nothing (was bug #8) | **Eliminated by construction.** `Editor` cannot exist on the back stack without its `canvasType` — there is no `null` state to desynchronise into. |
| `NotebookCanvasScreen.kt` — an adapter whose only job was mapping `NotebookType` → `CanvasType` 1:1 | Deleted. `CanvasType` alone is now `@Serializable` and carried directly as the `Editor` route's argument. |
| No back stack; back behaviour hand-implemented per screen | A real back stack via `NavHost`/`NavController` |
| No state restoration across process death | `rememberNavController()`'s `SavedStateHandle` integration restores the current destination and its arguments — not full screen state (see the `CanvasState` caveat below), but the navigation *position* survives a process kill, which it did not before |

**What this does *not* yet fix:** `CanvasState` is still created with `remember { CanvasState() }` inside `CanvasScreen` (`CanvasScaffold.kt`), not hoisted into a ViewModel. Rotating the device or navigating away and back still drops every stroke — bug #1 is unchanged. Fixing that is `VAHI-110` (coordinate model) and `VAHI-301` (persistence); see [`18-architecture-target.md`](18-architecture-target.md) §4 for why the editor's state deliberately does **not** move into a plain `StateFlow`-backed `UiState` the way other screens will.

Splash and Auth pop themselves off the back stack on the way through (`popUpTo(...) { inclusive = true }`), so pressing back from the library still exits the app — matching the pre-Session-6 behaviour, just now for the honest reason (nothing left on the stack) rather than by accident.

The two `BackHandler`s that exist are unchanged by this rewrite:
- `HomeScreen` — enabled only while an overlay is open; closes the modal first, then dropdowns. When nothing is open there is no handler, so system back falls through to the `NavController`, which exits the app (Library is the base of the stack).
- `CanvasScreen` — always enabled; calls `onBack()`, which now calls `navController.popBackStack()`, returning to Home.

## The canvas composition model

The canvas is the most structurally interesting part of the app. `CanvasScreen` stacks three explicit `zIndex` layers inside one `Box`:

```
zIndex 100  BackArrowButton            ← navigation control
zIndex  50  DrawingCanvas              ← transparent; captures pointers, renders ink
zIndex   0  Blank/Ruled/GridCanvasContent  ← the "paper"
```

This separation is deliberate and good: paper style and ink are fully decoupled, so a new paper style is a single new composable with no change to the drawing engine.

It does have one structural consequence worth knowing: because `DrawingCanvas` fills the screen and consumes pointer events, the `verticalScroll`/`horizontalScroll` modifiers on `GridCanvasContent` underneath never receive gestures. The 4000 dp grid is effectively fixed in place. See [`05-drawing-engine.md`](05-drawing-engine.md).

## Responsive strategy

`ui/components/WindowInfo.kt` provides a `@Composable fun windowClass(): WindowClass` derived from `LocalConfiguration.current.screenWidthDp`, with Material breakpoints (`< 600` Compact, `600–839` Medium, `≥ 840` Expanded).

It is used in exactly **two** places — `AuthScreen` and the unused `NotebookScreen`. `HomeScreen` and the canvas screens ignore it entirely and use fixed dimensions (260 × 200 dp cards, a 3-column grid, a 680 dp modal). The app is therefore tablet-only in practice.
