# 08 — File Reference

Every file in the project, what it holds, and its status.

Legend: ✅ live and used · ⚠️ present but unused / incomplete · 📄 config or asset

---

## Root

| File | Status | Contents |
|---|---|---|
| `README.md` | ✅ | Project entry point (added by this documentation pass) |
| `settings.gradle.kts` | 📄 | `rootProject.name = "Vahi"`, `include(":app")`, centralised repositories with `FAIL_ON_PROJECT_REPOS` |
| `build.gradle.kts` | 📄 | Root build file — declares the three plugins with `apply false` |
| `gradle.properties` | 📄 | `-Xmx2048m`, `android.useAndroidX=true`, `kotlin.code.style=official`, `android.nonTransitiveRClass=true` |
| `gradle/libs.versions.toml` | 📄 | Version catalog: 10 version refs, 16 library aliases, 3 plugin aliases |
| `gradle/wrapper/gradle-wrapper.properties` | 📄 | Gradle `8.13-bin` |
| `gradle/wrapper/gradle-wrapper.jar` | 📄 | Wrapper binary |
| `gradlew`, `gradlew.bat` | 📄 | Wrapper launch scripts |
| `local.properties` | 📄 | Machine-local SDK path — gitignored |
| `.gitignore` | 📄 | Standard Android ignores |
| `.idea/*` | 📄 | Android Studio config; 11 files tracked, 2 untracked |

---

## `app/` module config

| File | Status | Contents |
|---|---|---|
| `app/build.gradle.kts` | 📄 | 65 lines — SDK levels, Java 11, `compose = true`, 15 dependency declarations |
| `app/proguard-rules.pro` | ⚠️ | Untouched template; every rule commented out. Irrelevant while `isMinifyEnabled = false` |
| `app/.gitignore` | 📄 | `/build` |
| `app/src/main/AndroidManifest.xml` | ✅ | One `<activity>`, zero permissions, `Theme.Vahi`, `supportsRtl` |

---

## Application entry

### `app/src/main/java/com/vahi/app/MainActivity.kt` ✅ · 25 lines
The only Android component. `ComponentActivity` → `enableEdgeToEdge()` → `setContent { VahiTheme { AppRoot() } }`. Deliberately logic-free; its KDoc states all UI logic is delegated to `AppRoot`.

---

## `ui/screens/` — destinations

### `AppDestination.kt` ✅ · 8 lines
`enum class AppDestination { SPLASH, AUTH, HOME, NOTEBOOK_CANVAS }`

### `AppRoot.kt` ✅ · 55 lines
The navigation state machine. Owns `destination` and `selectedNotebookType`; a `when` renders exactly one destination and wires each screen's callback back to a state assignment.

### `SplashScreen.kt` ✅ · 122 lines
`SplashScreen(onTimeout)`. ExoPlayer plays `asset:///splash.mp4` muted and once, inside a `PlayerView` via `AndroidView` at 65 % screen width; a `Player.Listener` fires `onTimeout()` on `STATE_ENDED`; `DisposableEffect` releases the player. Background is an infinitely animating 4 s reversing off-white gradient. `@OptIn(UnstableApi::class)`.

### `AuthScreen.kt` ✅ · 275 lines
| Composable | Vis. | Role |
|---|---|---|
| `AuthScreen(onLoginSuccess)` | public | Branches on `windowClass()` |
| `AuthTabletLayout` | private | 50/50 `Row`: form left, brand right |
| `AuthCard` | private | 380 dp form — 4 fields, Login, divider, Google, Apple |
| `VahiStyledField` | private | Labelled 28 dp-radius `OutlinedTextField` with lavender borders |
| `AuthFallbackLayout` | private | "Vahi works best on a tablet" (rewritten Session 4). Still no callback — intentional per D2 |

Field values are held in state but never validated or read.

### `HomeScreen.kt` ✅ · 982 lines — the largest file
| Declaration | Vis. | Role |
|---|---|---|
| `data class Notebook` | public | `id`, `title`, `created`, `color` |
| `data class CardMenuAnchor` | public | `notebookId`, `offset` |
| `HomeScreen(onNavigateToCanvas)` | public | 3-layer host: content (blurrable) / dropdowns / modal |
| `CreateNotebookOverlay` | public | 680 dp modal, 2×2 style picker, Esc + scrim + back dismissal |
| `CreateNotebookOption` | public | One 120 dp option card |
| `CardContextMenu` | public | 180 dp — delete, edit, share, pin, lock |
| `SettingsOverlay` | public | 260 dp — Profile, Preferences, Help, Logout |
| `ProfileOverlay` | public | 240 dp — Profile, Account, Notifications, Upgrade to Plus, Logout |
| `HomeContent` | private | TopBar → Filters → Toolbar → Grid |
| `TopBar` | private | Logo · Upgrade · Settings · avatar; captures trigger positions |
| `FiltersRow` | private | All / Pinned notes / Imported Books — no-ops |
| `ToolbarRow` | private | Search (decorative) · view toggles · sort · Import |
| `NotebookGrid` | private | `LazyVerticalGrid(Fixed(3))`; **hardcoded 2-notebook list** |
| `CreateNotebookCard` | private | 260×200 dp "+" tile |
| `NotebookCard` | private | 260×200 dp notebook tile with ⋮⋮ menu |

### `NotebookCanvasScreen.kt` ✅ · 32 lines
`enum class NotebookType { BLANK, RULED, GRID }` and a thin adapter mapping it 1:1 to `CanvasType` before delegating to `CanvasScreen`.

### `CanvasScaffold.kt` ✅ · 129 lines
`enum class CanvasType { BLANK, RULED, GRID }`; `CanvasScreen(canvasType, onBack)` — creates `remember { CanvasState() }`, registers an always-on `BackHandler`, and stacks paper (z 0) / `DrawingCanvas` (z 50) / `BackArrowButton` (z 100). Private `BackArrowButton` renders `←`-in-circle at TopStart for Grid, bare `↓` at BottomStart otherwise.

### `BlankCanvas.kt` ✅ · 21 lines
`BlankCanvasContent()` — a white full-size `Box`.

### `RuledCanvas.kt` ✅ · 45 lines
`RuledCanvasContent()` — white background plus a `Canvas` drawing **vertical** 1 px `#D0D8E0` lines every 32 dp.

### `GridCanvas.kt` ✅ · 56 lines
`GridCanvasContent()` — white background inside `verticalScroll` + `horizontalScroll` (⚠️ never receives gestures) containing a 4000 dp square `Canvas` of 2 px `#D0D0D0` dots at 24 px spacing.

### `NotebookScreen.kt` — 🗑️ **deleted (Session 4)**
Was dead code, never referenced. Sketched the intended tablet workspace: 64 dp top toolbar, flexible canvas area, 320 dp right panel commented `// Right panel (AI / research later)`. That intent is preserved in `11-forward-plan.md` Track E rather than left as an orphaned file.

---

## `ui/components/`

### `WindowInfo.kt` ✅ · 38 lines
`enum class WindowClass { Compact, Medium, Expanded }` and `@Composable fun windowClass()` derived from `LocalConfiguration.current.screenWidthDp` at the 600 / 840 dp Material breakpoints. Used by `AuthScreen` and `NotebookScreen` only.

### `drawing/Stroke.kt` ✅ · 20 lines
`data class Stroke(id: UUID, path: Path, color: Color = Black, strokeWidth: Float = 4f)`.

### `drawing/CanvasState.kt` ✅ · 162 lines
`enum class ToolType { PEN, ERASER }` and `class CanvasState`. Holds `strokes`, `undoneStrokes`, `currentStroke`, `activeTool`, `strokeColor`, `strokeWidth`. Methods: `startStroke`, `continueStroke`, `finishStroke`, `cancelStroke` ⚠️(uncalled), `eraseAtPosition`, private `strokeIntersectsRect`, `undo` ⚠️(uncalled), `redo` ⚠️(uncalled), `canUndo`, `canRedo`, `clearAll` ⚠️(uncalled).

### `drawing/DrawingCanvas.kt` ✅ · 94 lines
`DrawingCanvas(canvasState, modifier)` — a transparent `Canvas` with a raw `awaitPointerEvent` loop keyed on `activeTool`, dispatching to pen or eraser behaviour, and a draw phase that repaints every stroke plus the in-progress one with round caps and joins.

### `drawing/DrawingToolbar.kt` ✅ **new (Session 4)** · 241 lines
> ⚠️ **Session 5 found four defects here:** the Clear button is unrecoverable (#13), touch targets are 40 dp not 48 dp (#15), there are no content descriptions and every colour is hardcoded so the rail is invisible on dark paper (#16), and the width preview renders a pixel value as dp (#18). Fix tracked as `VAHI-003`.

`DrawingToolbar(canvasState, modifier)` — a floating vertical tool rail wired directly to `CanvasState`. Pen/eraser toggle, 5 fixed colour swatches, 3 fixed width presets, and undo/redo/clear (disabled state reflects `canUndo()`/`canRedo()`/`strokes.isNotEmpty()`). Uses text glyphs for pen/eraser/undo/redo (`✒ ⌫ ↶ ↷`, consistent with the app's existing icon convention) and `Icons.Default.Delete` for clear. Wired into `CanvasScaffold.kt` at `Alignment.CenterStart`. See `12-ux-plan.md` §3.1–3.2 and `13-visual-design-system.md` §8.1 for the design rationale; the fuller pen-slot-configuration-popover spec (§8.2) is not yet built.

---

## `ui/theme/`

| File | Status | Contents |
|---|---|---|
| `Color.kt` | ✅ | ~36 colour constants (4 unused ones removed in Session 4): Material scheme tokens (light + dark), 7 Home tokens, 3 Auth/input tokens, 1 accent, plus a new `VahiTextPlaceholder` (`#767676`) added in Session 4 to fix two WCAG-AA contrast failures |
| `Type.kt` | ✅ | `VahiTypography` — all 15 Material 3 roles explicitly defined, `FontFamily.Default` throughout |
| `Shape.kt` | ✅ | `VahiShapes` — 4/8/12/16/24 dp |
| `Theme.kt` | ✅ | `VahiDarkColorScheme`, `VahiLightColorScheme`, `VahiTheme(darkTheme, dynamicColor = false, content)` — default changed from `true` to `false` in Session 4 (decision D4) |

---

## `viewmodel/`

### `BaseViewModel.kt` ⚠️ · 47 lines — **written, never used**
`abstract class BaseViewModel<State>(initialState: State) : ViewModel()` exposing a read-only `StateFlow<State>` over a private `MutableStateFlow`, with `protected fun updateState(reducer)` and `protected fun setState(newState)`. Plus two empty marker interfaces, `UiState` and `UiEvent`. No subclass exists anywhere.

---

## `domain/` and `data/` — ⚠️ empty skeletons

Four files, each three lines (two comments + a `package` declaration):

| File | Declared intent |
|---|---|
| `domain/models/package-info.kt` | "will contain domain model entities" |
| `domain/usecases/package-info.kt` | "will contain use case implementations" |
| `data/repository/package-info.kt` | "will contain repository implementations" |
| `data/datasource/package-info.kt` | "will contain data source implementations (local/remote)" |

---

## Resources

| Path | Status | Contents |
|---|---|---|
| `res/values/strings.xml` | ⚠️ | One string: `app_name = "Vahi"`. All other UI text is hardcoded in Kotlin |
| `res/values/colors.xml` | 🗑️ **deleted (Session 4)** | Was unused Android Studio template colours |
| `res/values/themes.xml` | ✅ | `Theme.Vahi` → `android:Theme.Material.Light.NoActionBar` |
| `res/xml/backup_rules.xml` | ⚠️ | Template, all rules commented out |
| `res/xml/data_extraction_rules.xml` | ⚠️ | Template, all rules commented out |
| `res/drawable/vahi_logo.png` | ✅ | Wordmark used in the Home top bar |
| `res/drawable/ic_launcher_foreground_img.png` | ✅ | Adaptive-icon foreground |
| `res/drawable/ic_launcher_background.xml` | ✅ | Adaptive-icon background vector |
| `res/drawable/ic_launcher_foreground.xml` | 🗑️ **deleted (Session 4)** | Was an orphaned default-robot vector, no longer referenced |
| `res/mipmap-anydpi/ic_launcher.xml`, `ic_launcher_round.xml` | ✅ | Adaptive icons (background + foreground, **no monochrome layer**) |
| `res/mipmap-{h,m,xh,xxh,xxxh}dpi/ic_launcher[_round].png` | ✅ | Legacy launcher rasters |
| `assets/splash.mp4` | ✅ | ~800 KB splash video |

---

## Tests

| File | Status | Contents |
|---|---|---|
| `app/src/test/java/com/vahi/app/ExampleUnitTest.kt` | ⚠️ | Template — `assertEquals(4, 2 + 2)` |
| `app/src/androidTest/java/com/vahi/app/ExampleInstrumentedTest.kt` | ⚠️ | Template — asserts the package name |

No test covers any Vahi code.

---

## Quick counts

Re-counted in Session 5 (2026-09-08) via `find` and `wc -l`.

| Metric | Value |
|---|---|
| Kotlin source files (main) | 25 (`NotebookScreen.kt` deleted, `DrawingToolbar.kt` added — net unchanged) |
| Kotlin test files | 2 |
| Lines of Kotlin (main + test) | 2,791 |
| Largest file | `HomeScreen.kt`, 990 lines |
| `@Composable` functions | 37 (verified via `grep -c`) |
| Enums | 5 (`AppDestination`, `NotebookType`, `CanvasType`, `ToolType`, `WindowClass`) |
| Data classes | 3 (`Notebook`, `CardMenuAnchor`, `Stroke`) |
| Non-composable classes | 2 (`CanvasState`, `BaseViewModel`) |
| Files that are dead or empty | 5 (`BaseViewModel.kt`, 4 × `package-info.kt`) — `NotebookScreen.kt` deleted in Session 4 |
