# 04 — Screen Reference

A composable-by-composable reference for every screen. For how the screens connect, see [`03-app-flow.md`](03-app-flow.md).

---

## AppRoot

**File:** `ui/screens/AppRoot.kt` · **Public API:** `@Composable fun AppRoot()`

✅ **Rewritten in Session 6** as a `navigation-compose` `NavHost` with type-safe routes. Owns a `NavController` (`rememberNavController()`), not raw `remember`ed state — the `NavController`'s own `SavedStateHandle` tracks the current destination and its arguments.

| Route | Type | Argument | Purpose |
|---|---|---|---|
| `Splash` | `@Serializable data object` | — | Start destination |
| `Auth` | `@Serializable data object` | — | |
| `Library` | `@Serializable data object` | — | Renders `HomeScreen` |
| `Editor` | `@Serializable data class` | `canvasType: CanvasType` | Renders `CanvasScreen`; the argument travels with the route, so it can never be null while this destination is active — the old `selectedNotebookType?.let { }` desync (bug #8) is now impossible by construction |

Renders one destination at a time via `composable<T> { }` blocks in the `NavHost`. Takes no parameters and exposes no callbacks — it is the top of the tree under `VahiTheme`. Full detail and the code: [`02-architecture.md`](02-architecture.md) §Navigation.

**Deleted in Session 6:** `AppDestination.kt` (the routes above replace the enum) and `NotebookCanvasScreen.kt` (see the `CanvasScaffold` section below).

---

## SplashScreen

**File:** `ui/screens/SplashScreen.kt` · **Public API:** `@Composable fun SplashScreen(onTimeout: () -> Unit)`

Annotated `@OptIn(UnstableApi::class)` because it touches Media3's `AspectRatioFrameLayout`.

| Element | Detail |
|---|---|
| Player | `ExoPlayer.Builder(context).build()` in `remember`, media `asset:///splash.mp4` |
| Config | `playWhenReady = true`, `volume = 0f`, `repeatMode = REPEAT_MODE_OFF`, `prepare()` |
| Advance | `Player.Listener.onPlaybackStateChanged` → `if (playbackState == Player.STATE_ENDED) onTimeout()` |
| Cleanup | `DisposableEffect(Unit) { onDispose { exoPlayer.release() } }` |
| View | `AndroidView { PlayerView(useController = false, resizeMode = RESIZE_MODE_FIT, transparent background) }` |
| Sizing | Player box at `fillMaxWidth(0.65f)`, padding `top 48 / start 24 / end 24 / bottom 24`, clipped to 16 dp radius |
| Background | `rememberInfiniteTransition` → 4000 ms `LinearEasing`, `RepeatMode.Reverse`, float 0→1, modulating alpha on a 3-stop vertical gradient `#F8F6F4 / #FAF8F6 / #F5F3F1` |

**Known issues:** no error listener (a load failure hangs the app on this screen); no timeout fallback; the `onTimeout` lambda is captured inside a `remember` block, so a changed callback identity would not be picked up (harmless here since `AppRoot` passes a stable lambda).

---

## AuthScreen

**File:** `ui/screens/AuthScreen.kt` · **Public API:** `@Composable fun AuthScreen(onLoginSuccess: () -> Unit = {})`

```
AuthScreen
├── windowClass() == Expanded → AuthTabletLayout(onLoginSuccess)
│     ├── Left  Box(weight 1f, white)                  → AuthCard(onLoginSuccess)
│     └── Right Box(weight 1f, VahiCardLavender @ 30%) → "Vahi" + "Your Digital Notebook"
└── else                      → AuthFallbackLayout()   → "Vahi works best on a tablet"
```

### `AuthCard` (private)

380 dp wide, 24 dp padding, 16 dp vertical spacing. Local state: `username`, `email`, `password`, `confirmPassword` — all `remember { mutableStateOf("") }`, **all unread**.

| Control | Detail |
|---|---|
| Username | `VahiStyledField(label = "Username", placeholder = "Username")` |
| E-mail | `VahiStyledField(label = "E-mail", placeholder = "example@mail.com")` |
| Password | `VahiStyledField(..., isPassword = true)` |
| Password confirmation | `VahiStyledField(..., isPassword = true)` |
| Login | `Button`, full width, 52 dp, 26 dp radius, `VahiAccentPurple`, `onClick = onLoginSuccess` |
| Divider | `HorizontalDivider`, 1 dp, `VahiInputBorder @ 50%` |
| Google | `OutlinedButton`, white, `#E0E0E0` border, "G" in `#4285F4` + "Continue with Google", `onClick = {}` |
| Apple | `Button`, `VahiAppleButtonBg` (black), empty glyph + "Continue with Apple", `onClick = {}` |

### `VahiStyledField` (private)

Reusable labelled input: a 14 sp medium `#333333` label above an `OutlinedTextField` — `singleLine`, 28 dp radius, `#AAAAAA` placeholder, white container in both states, `VahiInputBorder` / `VahiInputBorderFocused` borders, `VahiAccentPurple` cursor, and `PasswordVisualTransformation()` when `isPassword`.

### `AuthFallbackLayout` (private)

✅ **Rewritten in Session 4.** A centred "Vahi works best on a tablet" message using `MaterialTheme.typography.titleLarge`. **It still receives no callback, so phone users cannot reach Home** — that is intentional per decision [D2](22-decisions.md#d2--tablet-only-for-v1), not a bug. Under [D10](22-decisions.md#d10--no-auth-wall-account-is-optional-and-requested-in-context) the auth screen leaves the startup path entirely and this message moves to the app shell (`VAHI-006`).

---

## HomeScreen

**File:** `ui/screens/HomeScreen.kt` · **Public API:** `@Composable fun HomeScreen(onNavigateToCanvas: (CanvasType) -> Unit = {})` — the parameter type changed from `NotebookType` to `CanvasType` in Session 6 when the duplicate-enum adapter was removed.

### Data classes declared here

```kotlin
data class Notebook(val id: String, val title: String, val created: String, val color: Color)
data class CardMenuAnchor(val notebookId: String, val offset: Offset)
```

### State

| Name | Type | Role |
|---|---|---|
| `isCreateNotebookOpen` | `Boolean` | Full-screen modal visibility |
| `cardMenuAnchor` | `CardMenuAnchor?` | Which card's ⋮⋮ menu is open, and where |
| `isSettingsOpen` | `Boolean` | Settings dropdown visibility |
| `isProfileOpen` | `Boolean` | Profile dropdown visibility |
| `settingsButtonPosition` | `Offset` | Captured trigger position (px) |
| `profileButtonPosition` | `Offset` | Captured trigger position (px) |

Derived: `isAnyDropdownOpen = cardMenuAnchor != null || isSettingsOpen || isProfileOpen`.

Local helpers `openCardMenu` / `openSettings` / `openProfile` each null-out the other two before setting their own — dropdowns are mutually exclusive by construction. `closeDropdowns()` clears all three. `openCreateNotebook()` calls `closeDropdowns()` then opens the modal.

`BackHandler(enabled = isCreateNotebookOpen || isAnyDropdownOpen)` closes the modal if open, otherwise the dropdowns.

### Composables in this file

| Composable | Visibility | Purpose |
|---|---|---|
| `HomeScreen` | public | The three-layer host described above |
| `CreateNotebookOverlay` | public | Full-screen paper-style picker modal |
| `CreateNotebookOption` | public | One 120 dp option card in the modal |
| `CardContextMenu` | public | 180 dp per-notebook menu |
| `SettingsOverlay` | public | 260 dp settings menu |
| `ProfileOverlay` | public | 240 dp profile menu |
| `HomeContent` | private | Column: TopBar → Filters → Toolbar → Grid |
| `TopBar` | private | Logo, Upgrade, Settings, avatar |
| `FiltersRow` | private | All / Pinned notes / Imported Books |
| `ToolbarRow` | private | Search, view toggles, sort, Import |
| `NotebookGrid` | private | `LazyVerticalGrid(Fixed(3))` |
| `CreateNotebookCard` | private | The "+" tile |
| `NotebookCard` | private | A single notebook tile |

### Notes and issues

- The **search bar is decorative** — it is a `Surface` containing two `Text`s, not a `TextField`.
- Every filter tab, view toggle, sort control, and menu row is a no-op; menu rows call `onDismiss()`.
- The **Import** button and the **Import PDF** modal option are both no-ops.
- Dropdown anchoring mixes px and dp (see [`09-project-status.md`](09-project-status.md), issue #3).
- Modal option dispatch is by string literal (`"Blank"`, `"Ruled"`, …) rather than an enum.
- Cards are fixed at 260 × 200 dp inside a fixed 3-column grid — no responsive behaviour.
- Several icons are emoji/text glyphs standing in for real assets: 🐦, 🔍, ⊞, ≡, ⋮⋮, 🧮, 💻, 📓, □, ↓.

---

## NotebookCanvasScreen — 🗑️ deleted in Session 6

**File:** `ui/screens/NotebookCanvasScreen.kt` — **no longer exists.**

It declared `enum class NotebookType { BLANK, RULED, GRID }` and a thin adapter mapping it 1:1 to `CanvasType` before delegating to `CanvasScreen`. Doc 09's "Code quality observations" flagged this as a duplicate-enum smell whose only purpose was letting the navigation layer and the canvas layer avoid sharing a type.

Once `AppRoot` moved to typed `navigation-compose` routes (Session 6), that reason evaporated: the `Editor` route now carries a `canvasType: CanvasType` argument directly (`CanvasType` gained `@Serializable`), and `AppRoot` calls `CanvasScreen` straight from the `composable<Editor>` block. There was no longer anything for this file, or `NotebookType`, to do.

---

## CanvasScreen (CanvasScaffold.kt)

**File:** `ui/screens/CanvasScaffold.kt` · **Public API:** `enum class CanvasType`, `@Composable fun CanvasScreen(canvasType: CanvasType, onBack: () -> Unit)`

The layered host for all canvases:

```kotlin
val canvasState = remember { CanvasState() }
BackHandler { onBack() }

Box(Modifier.fillMaxSize()) {
    when (canvasType) { BLANK -> BlankCanvasContent(); RULED -> RuledCanvasContent(); GRID -> GridCanvasContent() }
    DrawingCanvas(canvasState, Modifier.fillMaxSize().zIndex(50f))
    BackArrowButton(onBack, isGrid = canvasType == CanvasType.GRID,
        Modifier.align(if (grid) TopStart else BottomStart).padding(if (grid) 16.dp else 24.dp).zIndex(100f))
}
```

### `BackArrowButton` (private)

Two visual variants, chosen by `isGrid`:

| Variant | Appearance | Placement |
|---|---|---|
| Grid | `←` 20 sp bold `#333333` in a 44 dp `#F5F5F5` circle | TopStart, 16 dp |
| Blank / Ruled | `↓` 22 sp normal `#333333`, no background, 8 dp inner padding | BottomStart, 24 dp |

The inconsistency is intentional in the code, but there is no comment explaining why the two paper styles get different back affordances — likely a design-iteration artifact worth unifying.

---

## Paper-style canvases

### `BlankCanvasContent` — `ui/screens/BlankCanvas.kt`
```kotlin
Box(Modifier.fillMaxSize().background(Color.White))
```
That's the whole file body. KDoc: *"pure white fullscreen background. No text, no watermarks, full screen area."*

### `RuledCanvasContent` — `ui/screens/RuledCanvas.kt`

| Constant | Value |
|---|---|
| `lineSpacingDp` | `32.dp` — comment notes this was doubled to halve the line count |
| `lineColor` | `#D0D8E0` |
| stroke width | `1f` (pixels) |

Draws `(size.width / lineSpacingPx).toInt()` **vertical** lines from `y = 0` to `y = size.height`, starting at `i = 1`. Uses `LocalDensity.current` to convert dp → px inside the `DrawScope`.

### `GridCanvasContent` — `ui/screens/GridCanvas.kt`

| Constant | Value |
|---|---|
| `dotSpacing` | `24f` (raw pixels, not dp) |
| `dotRadius` | `2f` |
| `dotColor` | `#D0D0D0` |
| `canvasSize` | `4000.dp` square |

Wrapped in `verticalScroll(rememberScrollState())` + `horizontalScroll(rememberScrollState())`. Draws a nested loop of `drawCircle` calls — at 4000 dp square with 24 px spacing this is on the order of tens of thousands of draw calls per frame, which is a real performance concern once ink is also being rendered.

⚠️ Scrolling is dead in practice because `DrawingCanvas` sits above it and consumes pointer input.

---

## NotebookScreen — 🗑️ deleted in Session 4

**File:** `ui/screens/NotebookScreen.kt` — **no longer exists.** It was never referenced from anywhere. Recorded here because it was the most explicit statement of long-term intent in the repo, and that intent survives as **P3, the AI/study panel**, in [`15-ux-blueprint.md`](15-ux-blueprint.md) §2 (v1.3).

It sketched the intended tablet workspace:

```
Column(fillMaxSize)
├── Surface(fillMaxWidth, height 64 dp)      // "Top toolbar (tools, page controls)" — empty
└── Row(fillMaxSize)
    ├── Surface(weight 1f, padding 16 dp)    // "Canvas area" — empty
    └── Surface(width 320 dp)                // "Right panel (AI / research later)" — empty
```

A toolbar, a canvas, and a persistent AI/research side panel. The toolbar shipped in Session 4 (`DrawingToolbar.kt`, on the left edge rather than the top — see [`12-ux-plan.md`](12-ux-plan.md) §3.1 for why); the side panel is planned for v1.3.
