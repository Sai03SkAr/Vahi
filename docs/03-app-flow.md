# 03 — Application Flow

## The whole app in one diagram

```mermaid
stateDiagram-v2
    [*] --> SPLASH : app launch

    SPLASH --> AUTH : ExoPlayer emits STATE_ENDED\n(onTimeout)
    AUTH --> HOME : Login button tapped\n(onLoginSuccess — no validation)
    HOME --> NOTEBOOK_CANVAS : paper style chosen in\nCreate Notebook modal\n(onNavigateToCanvas)
    NOTEBOOK_CANVAS --> HOME : back arrow or system back\n(onBack)

    SPLASH --> [*] : system back (exits app)
    AUTH --> [*] : system back (exits app)
    HOME --> [*] : system back with no overlay open
```

Four destinations, three forward transitions, one backward transition. That is the entire navigation graph.

## Startup sequence

```
Android launcher
  → MainActivity.onCreate()
      → enableEdgeToEdge()
      → setContent {
            VahiTheme {                 // Material 3: dynamic colour on Android 12+,
                AppRoot()               // otherwise Vahi light/dark scheme
            }
        }
  → AppRoot: destination = SPLASH  (initial value)
  → SplashScreen composes
```

`MainActivity` is intentionally trivial — 25 lines, no logic beyond hosting Compose. Its KDoc says so explicitly: *"This activity only hosts the Compose UI via setContent. All UI logic is delegated to AppRoot."*

## Step 1 — Splash

**File:** `ui/screens/SplashScreen.kt`

```mermaid
sequenceDiagram
    participant S as SplashScreen
    participant E as ExoPlayer
    participant R as AppRoot

    S->>E: remember { ExoPlayer.Builder(context).build() }
    S->>E: setMediaItem("asset:///splash.mp4")
    S->>E: volume = 0f, repeatMode = OFF, playWhenReady = true
    S->>E: prepare()
    S->>E: addListener(Player.Listener)
    Note over S: AndroidView { PlayerView(useController = false,<br/>RESIZE_MODE_FIT) } at 65% screen width
    Note over S: Background: infinite 4 s reversing<br/>vertical gradient animation
    E-->>S: onPlaybackStateChanged(STATE_ENDED)
    S->>R: onTimeout()
    R->>R: destination = AUTH
    Note over S: DisposableEffect onDispose → exoPlayer.release()
```

Key points:

- The transition is driven **entirely by video playback completing** — there is no timer fallback. The duration of `assets/splash.mp4` (800 KB) is the duration of the splash.
- Audio is muted (`volume = 0f`) and the video plays once (`REPEAT_MODE_OFF`).
- The player is correctly released in `DisposableEffect(Unit) { onDispose { ... } }`.
- The background is not static: `rememberInfiniteTransition` drives a 4-second `LinearEasing` reversing float, which modulates the alpha of a three-stop off-white vertical gradient (`#F8F6F4` → `#FAF8F6` → `#F5F3F1`).
- ⚠️ **Failure mode:** if the media fails to load, `STATE_ENDED` never fires and the app is stuck on the splash forever. There is no `onPlayerError` handling and no timeout.

## Step 2 — Auth

**File:** `ui/screens/AuthScreen.kt`

```mermaid
flowchart TD
    A[AuthScreen] --> B{windowClass}
    B -->|Expanded ≥ 840 dp| C[AuthTabletLayout]
    B -->|Compact / Medium| D["AuthFallbackLayout<br/>'Vahi works best on a tablet'<br/>⚠️ no onLoginSuccess — intentional (D2)"]

    C --> E["LEFT pane (weight 1f, white)<br/>AuthCard"]
    C --> F["RIGHT pane (weight 1f, lavender 30%)<br/>'Vahi' displayMedium bold purple<br/>'Your Digital Notebook'"]

    E --> G[Username field]
    E --> H[E-mail field]
    E --> I[Password field — masked]
    E --> J[Password confirmation — masked]
    E --> K["Login button (purple pill, 52 dp)"]
    E --> L[HorizontalDivider]
    E --> M["Continue with Google (outlined, no-op)"]
    E --> N["Continue with Apple (black, no-op)"]

    K -->|onClick = onLoginSuccess| O[destination = HOME]
```

Key points:

- The four text fields are backed by local `remember { mutableStateOf("") }` values. **Their contents are never read.** `onClick = onLoginSuccess` is passed the callback directly — no validation, no comparison of password vs. confirmation, no network call.
- One combined form serves both sign-up and sign-in — there is no mode toggle. Username + email + password + confirmation reads as *registration*, but the button says *Login*.
- Google and Apple buttons have `onClick = {}`.
- The Google logo is the letter `G` styled in `#4285F4`; the Apple button's icon slot contains an empty string.
- All fields are built by a shared private `VahiStyledField` composable: a label above a 28 dp-radius `OutlinedTextField` with lavender borders (`VahiInputBorder` → `VahiInputBorderFocused` on focus) and a purple cursor.
- ⚠️ **`AuthFallbackLayout` takes no parameters and provides no path forward.** On any device narrower than 840 dp the app is unusable past this point.

## Step 3 — Home (the notebook library)

**File:** `ui/screens/HomeScreen.kt` (982 lines — the largest file in the project)

### Layer structure

`HomeScreen` is a `Box` with three stacked layers:

```
zIndex 20   CreateNotebookOverlay      ← full-screen modal, when isCreateNotebookOpen
zIndex 10   Dropdown scrim + one of {CardContextMenu, SettingsOverlay, ProfileOverlay}
zIndex  0   HomeContent                ← blurred 8 dp while the modal is open
```

### HomeContent, top to bottom

| Section | Composable | Contents |
|---|---|---|
| Top bar | `TopBar` | `vahi_logo` image (48 dp tall) · spacer · **Upgrade** dark pill · **Settings** light pill with gear icon · 44 dp circular profile avatar (beige, 🐦 emoji) |
| Filters | `FiltersRow` | **All** (selected, pill background) · *Pinned notes* · *Imported Books* — all three currently no-ops |
| Toolbar | `ToolbarRow` | 280 dp search field ("Search notebooks…", non-functional — it's a `Text`, not a `TextField`) · spacer · three view-mode toggle boxes (✓ / ⊞ / ≡) · **Most recent** sort dropdown · blue **Import** pill |
| Grid | `NotebookGrid` | `LazyVerticalGrid(columns = Fixed(3))`, 24 dp gaps. First item is always `CreateNotebookCard`; then the notebook cards. |

### The notebook data

Hardcoded inside `NotebookGrid`:

```kotlin
val notebooks = listOf(
    Notebook("1", "Maths notes",     "Created 4 jan 2025",  Color(0xFFBABEFF)),
    Notebook("2", "Python Problems", "Created 23 dec 2025", Color(0xFFD3C3C3))
)
```

Each `NotebookCard` is 260 × 200 dp, 16 dp radius, filled with its colour, and shows: a 40 dp translucent icon tile (emoji picked by title keyword — 🧮 for "Maths", 💻 for "Python", 📓 otherwise), a `⋮⋮` overflow button top-right, and title + created date bottom-left.

`CreateNotebookCard` is the same size in `VahiCreateCardBg` grey with a 56 dp lavender circle containing a `+`.

### Overlay interaction model

```mermaid
flowchart LR
    subgraph triggers
      T1["Settings pill"] --> S1[openSettings]
      T2["Profile avatar"] --> S2[openProfile]
      T3["Card ⋮⋮ button"] --> S3[openCardMenu]
      T4["Create card /<br/>any create entry"] --> S4[openCreateNotebook]
    end

    S1 --> M["mutual exclusion:<br/>opening one closes the others"]
    S2 --> M
    S3 --> M
    S4 --> C1[closeDropdowns → isCreateNotebookOpen = true]

    M --> D["Layer 2 scrim (zIndex 10)<br/>tap anywhere → closeDropdowns()"]
    C1 --> E["Layer 3 modal (zIndex 20)<br/>black 40% scrim<br/>tap scrim or Esc → dismiss<br/>content behind blurred 8 dp"]

    BH["System back (BackHandler,<br/>enabled when anything is open)"] --> F{isCreateNotebookOpen?}
    F -->|yes| G[close modal]
    F -->|no| H[closeDropdowns]
```

The three dropdowns are anchored using positions captured via `onGloballyPositioned { it.positionInRoot() }` on the trigger, then applied as top padding on a `TopEnd`-aligned box.

⚠️ **Unit bug:** `positionInRoot()` returns **pixels**, but the value is used as **dp** — `padding(top = (settingsButtonPosition.y + 48).dp)`. On any device with density ≠ 1.0 the dropdowns are offset proportionally to screen density. Only the Y coordinate is used at all; X is discarded and everything is right-aligned, so the card menu appears at the screen edge regardless of which card it belongs to.

### Menu contents (all items currently just dismiss)

| Overlay | Width | Items |
|---|---|---|
| `CardContextMenu` | 180 dp | delete, edit, share, pin, lock |
| `SettingsOverlay` | 260 dp | Profile, Preferences, Help, Logout |
| `ProfileOverlay` | 240 dp | Profile, Account, Notifications, Upgrade to Plus, Logout |

Each row is 44 dp with a 20 dp grey square placeholder where an icon will go.

### The Create Notebook modal

`CreateNotebookOverlay` — a 680 dp-wide white `Surface`, 16 dp radius, 8 dp elevation, centred over a 40 %-black scrim, holding the heading *"Select the style that fits your needs"* and a 2 × 2 grid of `CreateNotebookOption` cards (120 dp tall, each with a glyph tile: □ ≡ ⊞ ↓).

Dismissal works three ways: tap the scrim, press **Escape** (via `onPreviewKeyEvent` on a `FocusRequester` that grabs focus in a `LaunchedEffect`), or system back.

Selection dispatch:

```kotlin
when (option) {
    "Blank"      -> onNavigateToCanvas(NotebookType.BLANK)
    "Ruled"      -> onNavigateToCanvas(NotebookType.RULED)
    "Grid"       -> onNavigateToCanvas(NotebookType.GRID)
    "Import PDF" -> { /* Import handled separately */ }   // ← no-op
}
```

Note that options are dispatched by **string literal**, not by an enum — a fragility worth fixing.

## Step 4 — Canvas

**Files:** `NotebookCanvasScreen.kt` → `CanvasScaffold.kt` → one of `BlankCanvas.kt` / `RuledCanvas.kt` / `GridCanvas.kt`, plus `components/drawing/`.

```mermaid
flowchart TD
    A["AppRoot: NOTEBOOK_CANVAS<br/>selectedNotebookType?.let"] --> B[NotebookCanvasScreen<br/>notebookType, onBack]
    B --> C["maps NotebookType → CanvasType<br/>(1:1)"]
    C --> D[CanvasScreen]

    D --> E["val canvasState = remember { CanvasState() }"]
    D --> F["BackHandler { onBack() } — always enabled"]

    D --> G["Layer 0 — paper"]
    G --> G1[BLANK → BlankCanvasContent]
    G --> G2[RULED → RuledCanvasContent]
    G --> G3[GRID → GridCanvasContent]

    D --> H["Layer 50 — DrawingCanvas(canvasState)"]
    D --> I["Layer 100 — BackArrowButton"]

    I --> I1["GRID: '←' in 44 dp grey circle,<br/>TopStart, 16 dp padding"]
    I --> I2["BLANK/RULED: '↓' bare glyph,<br/>BottomStart, 24 dp padding"]

    I --> J[onBack → destination = HOME]
```

### The three paper styles

| Style | Implementation |
|---|---|
| **Blank** | A white `Box` filling the screen. Nothing else. |
| **Ruled** | White background + a `Canvas` drawing **vertical** lines every 32 dp in `#D0D8E0` at 1 px width, full height. (Note: vertical, not the horizontal ruling the "Lined pages for writing" description implies.) |
| **Grid** | White background wrapped in `verticalScroll` + `horizontalScroll`, containing a **4000 dp × 4000 dp** `Canvas` that draws a dot lattice: 2 px-radius `#D0D0D0` circles spaced 24 px apart. |

⚠️ The grid's scroll modifiers cannot fire, because `DrawingCanvas` above it consumes the pointer events.

### Drawing

Once on any canvas, pointer input goes to `DrawingCanvas`, which starts / continues / finishes strokes in `CanvasState` and repaints on every change. `activeTool` defaults to `PEN`.

⚠️ **There is no toolbar on the canvas.** Nothing in the UI can change `activeTool`, `strokeColor`, or `strokeWidth`, and nothing calls `undo()`, `redo()`, or `clearAll()`. The eraser and the undo/redo stacks are fully implemented but unreachable. See [`05-drawing-engine.md`](05-drawing-engine.md).

## Callback contract summary

The complete set of cross-screen callbacks:

| Screen | Parameter | Signature | Fired when | AppRoot's response |
|---|---|---|---|---|
| `SplashScreen` | `onTimeout` | `() -> Unit` | ExoPlayer reaches `STATE_ENDED` | `destination = AUTH` |
| `AuthScreen` | `onLoginSuccess` | `() -> Unit` | Login button clicked | `destination = HOME` |
| `HomeScreen` | `onNavigateToCanvas` | `(NotebookType) -> Unit` | A paper style is chosen in the modal | store type; `destination = NOTEBOOK_CANVAS` |
| `NotebookCanvasScreen` | `onBack` | `() -> Unit` | Back arrow or system back | `destination = HOME` |
