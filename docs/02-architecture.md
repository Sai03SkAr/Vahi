# 02 — Architecture

## Shape of the app

Vahi is a **single-module, single-Activity, 100% Jetpack Compose** Android app.

```
MainActivity (the only Activity, the only Android component in the manifest)
  └── VahiTheme            ← Material 3 theming
        └── AppRoot        ← the entire navigation state machine
              └── one of: SplashScreen | AuthScreen | HomeScreen | NotebookCanvasScreen
```

There are no Fragments, no XML layouts, no Services, no BroadcastReceivers, no ContentProviders. `AndroidManifest.xml` declares exactly one `<activity>` and requests **no permissions at all**.

## Package layout

```
com.vahi.app
├── MainActivity.kt                     Entry point. setContent { VahiTheme { AppRoot() } }
│
├── ui/
│   ├── theme/                          Design system
│   │   ├── Color.kt                    All colour constants
│   │   ├── Type.kt                     Full Material 3 type scale
│   │   ├── Shape.kt                    Corner-radius scale
│   │   └── Theme.kt                    VahiTheme — assembles the MaterialTheme
│   │
│   ├── components/                     Reusable, screen-agnostic pieces
│   │   ├── WindowInfo.kt               windowClass() responsive breakpoints
│   │   └── drawing/                    The ink engine
│   │       ├── Stroke.kt               Data model for one pen mark
│   │       ├── CanvasState.kt          Stroke list, undo/redo, tool/colour/width
│   │       └── DrawingCanvas.kt        Composable: pointer capture + rendering
│   │
│   └── screens/                        One file (roughly) per destination
│       ├── AppDestination.kt           enum of the four destinations
│       ├── AppRoot.kt                  The navigation state machine
│       ├── SplashScreen.kt             Video splash
│       ├── AuthScreen.kt               Login / signup
│       ├── HomeScreen.kt               Notebook library + all its overlays
│       ├── NotebookCanvasScreen.kt     NotebookType enum + thin adapter to CanvasScreen
│       ├── CanvasScaffold.kt           CanvasType enum + CanvasScreen (layered canvas host)
│       ├── BlankCanvas.kt              Paper style: plain white
│       ├── RuledCanvas.kt              Paper style: vertical rules
│       ├── GridCanvas.kt               Paper style: scrollable dot grid
│       └── NotebookScreen.kt           ⚠️ dead code — not referenced anywhere
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

All state is Compose-local. There are three distinct patterns in play:

### 1. Hoisted screen state in `AppRoot`
```kotlin
var destination by remember { mutableStateOf(AppDestination.SPLASH) }
var selectedNotebookType by remember { mutableStateOf<NotebookType?>(null) }
```
Two variables define the entire app's position. Children never mutate them directly — they invoke callbacks (`onTimeout`, `onLoginSuccess`, `onNavigateToCanvas`, `onBack`) and `AppRoot` reassigns.

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
The codebase is consistent about this: state flows *down* as parameters, events flow *up* as lambdas. Every screen composable takes callbacks (`onBack: () -> Unit`, `onNavigateToCanvas: (NotebookType) -> Unit`) rather than reaching for a shared object. That convention is worth preserving as ViewModels get introduced.

## Navigation

There is **no navigation library**. `AppRoot.kt` is a `when` over `AppDestination`:

```kotlin
when (destination) {
    AppDestination.SPLASH -> SplashScreen(onTimeout = { destination = AppDestination.AUTH })
    AppDestination.AUTH   -> AuthScreen(onLoginSuccess = { destination = AppDestination.HOME })
    AppDestination.HOME   -> HomeScreen(onNavigateToCanvas = { type ->
                                 selectedNotebookType = type
                                 destination = AppDestination.NOTEBOOK_CANVAS
                             })
    AppDestination.NOTEBOOK_CANVAS -> selectedNotebookType?.let { type ->
                                 NotebookCanvasScreen(notebookType = type,
                                                      onBack = { destination = AppDestination.HOME })
                             }
}
```

Consequences of this design:

- **No back stack.** Back behaviour is hand-implemented per screen with `BackHandler`.
- **No argument passing mechanism.** `selectedNotebookType` is a manually-managed side channel — a second piece of navigation state that must be kept in sync with `destination`. If it's ever `null` while `destination == NOTEBOOK_CANVAS`, the `?.let` renders nothing and the user sees a blank screen.
- **No deep links, no state restoration.** Rotating the device on the Canvas screen returns you to Splash and drops all ink.
- **No transition animations** between destinations.

The two `BackHandler`s that exist:
- `HomeScreen` — enabled only while an overlay is open; closes the modal first, then dropdowns. When nothing is open there is no handler, so back exits the app.
- `CanvasScreen` — always enabled; calls `onBack()`, returning to Home.

Splash and Auth register no handler, so back exits the app from either.

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
