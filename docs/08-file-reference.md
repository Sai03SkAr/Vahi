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

### `AppDestination.kt` — 🗑️ deleted in Session 6
Was `enum class AppDestination { SPLASH, AUTH, HOME, NOTEBOOK_CANVAS }`. Replaced by the typed `@Serializable` route objects/classes declared directly in `AppRoot.kt` — see below.

### `AppRoot.kt` ✅ **rewritten (Session 6)** · ~75 lines
A `navigation-compose` `NavHost` with type-safe routes: `Splash`, `Auth`, `Library` (`data object`s) and `Editor(canvasType: CanvasType)` (`data class`). Each is a `composable<T> { }` block wiring one screen's callback to a `navController.navigate(...)`/`popBackStack()` call. Replaces the former hand-rolled `when` over `AppDestination` and the `selectedNotebookType` side channel (docs/09 bug #8, now impossible by construction — `Editor` cannot exist on the back stack without its `CanvasType`). Full detail: [`02-architecture.md`](02-architecture.md) §Navigation.

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

### `NotebookCanvasScreen.kt` — 🗑️ deleted in Session 6
Declared `enum class NotebookType { BLANK, RULED, GRID }` and a thin adapter mapping it 1:1 to `CanvasType` before delegating to `CanvasScreen`. Once `AppRoot` carried `CanvasType` directly as a typed route argument, the adapter and its duplicate enum had nothing left to do.

### `CanvasScaffold.kt` ✅ · ~165 lines
`@Serializable enum class CanvasType { BLANK, RULED, GRID }` (gained `@Serializable` in Session 6 — it's now carried directly as a `navigation-compose` route argument, see `AppRoot.kt`); `CanvasScreen(canvasType, onBack)` — creates `remember { CanvasState() }`, registers an always-on `BackHandler`, and stacks paper (z 0) / `DrawingCanvas` (z 50) / `DrawingToolbar` (z 90) / `BackArrowButton` (z 100) / `SnackbarHost` (z 110, added Session 6 — surfaces "Undo" after Clear). Private `BackArrowButton` renders `←`-in-circle at TopStart for Grid, bare `↓` at BottomStart otherwise.

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

### `drawing/CanvasState.kt` ✅ · ~250 lines — **substantially rewritten in Session 6**
`enum class ToolType { PEN, ERASER }`, a private `StrokeCommand` sealed type (`Add`, `Erase`), and `class CanvasState`. Holds `strokes`, `currentStroke`, `activeTool`, `strokeColor`, `strokeWidth`. The old public `undoneStrokes: List<Stroke>` getter is gone — nothing outside this file ever read it, and it has no clean equivalent once redo is command-based rather than stroke-based.

Methods: `startStroke`, `continueStroke`, `finishStroke` (now pushes an `Add` command), `cancelStroke` ⚠️(still uncalled from anywhere — no gesture maps to it yet), `eraseAtPosition` (now accumulates into the current erase gesture rather than pushing its own command every call), `undo`/`redo` (now interpret `StrokeCommand`s rather than moving raw strokes between two lists — fixes bug #5, "undo and erase shared one stack"), `canUndo`/`canRedo`, `clearAll` (unchanged from Session 4 — undoable via its own separate `canUndoClear()`/`undoClear()`, not folded into the `Add`/`Erase` stack; see the KDoc on `lastClearedSnapshot` for why). **New in Session 6:** `beginErase()`/`finishErase()` — a gesture lifecycle mirroring the pen's start/continue/finish, so one erase drag across several strokes is one undo step, called from `DrawingCanvas.kt`.

Covered by 17 tests across `app/src/test/.../drawing/CanvasStateTest.kt` (6, `clearAll`/`undoClear`, `VAHI-003`) and `CanvasStateUndoRedoTest.kt` (11, the command-history fix, `VAHI-007`/`VAHI-120`) — both Robolectric, since `Stroke.path` is `android.graphics.Path`-backed.

### `drawing/DrawingCanvas.kt` ✅ · ~105 lines
`DrawingCanvas(canvasState, modifier)` — a transparent `Canvas` with a raw `awaitPointerEvent` loop keyed on `activeTool`, dispatching to pen or eraser behaviour, and a draw phase that repaints every stroke plus the in-progress one with round caps and joins. **Session 6:** the ERASER branch now tracks a local `eraseGestureActive` flag and calls `canvasState.beginErase()`/`finishErase()` around a drag, so a whole erase gesture becomes one undo step (`VAHI-120`) instead of one per pointer sample. `event.changes.firstOrNull()` — the source of bug #14 (palm rejection) — is unchanged; that's `VAHI-112`/`VAHI-113`, not touched this session.

### `drawing/DrawingToolbar.kt` ✅ **new (Session 4)**, fixed (Session 6) · ~250 lines
> Session 5 found four defects here — the Clear button was unrecoverable (#13), touch targets were 40 dp not 48 dp (#15), no content descriptions and every colour hardcoded so the rail was invisible on dark paper (#16), and the width preview rendered a pixel value as dp (#18). **All four fixed in Session 6** (`VAHI-003`) — see `09-project-status.md` for detail. Clear now takes an `onClear` callback rather than calling `CanvasState.clearAll()` directly, so `CanvasScaffold.kt` can offer an "Undo" snackbar.

`DrawingToolbar(canvasState, modifier)` — a floating vertical tool rail wired directly to `CanvasState`. Pen/eraser toggle, 5 fixed colour swatches, 3 fixed width presets, and undo/redo/clear (disabled state reflects `canUndo()`/`canRedo()`/`strokes.isNotEmpty()`). Uses text glyphs for pen/eraser/undo/redo (`✒ ⌫ ↶ ↷`, consistent with the app's existing icon convention) and `Icons.Default.Delete` for clear. Wired into `CanvasScaffold.kt` at `Alignment.CenterStart`. See `12-ux-plan.md` §3.1–3.2 and `13-visual-design-system.md` §8.1 for the design rationale; the fuller pen-slot-configuration-popover spec (§8.2) is not yet built.

---

## `ui/theme/`

| File | Status | Contents |
|---|---|---|
| `Color.kt` | ✅ | ~41 colour constants: Material scheme tokens (light + dark), 7 Home tokens, 3 Auth/input tokens, 1 accent, `VahiTextPlaceholder` (`#767676`, Session 4, fixes two WCAG-AA contrast failures), and 5 new ink-palette constants (`VahiInk*`, Session 6 — the tool rail's pen swatches, moved out of inline hex literals as part of `VAHI-003`) |
| `Type.kt` | ✅ | `VahiTypography` — all 15 Material 3 roles explicitly defined, `FontFamily.Default` throughout |
| `Shape.kt` | ✅ | `VahiShapes` — 4/8/12/16/24 dp |
| `Theme.kt` | ✅ | `VahiDarkColorScheme`, `VahiLightColorScheme`, `VahiTheme(darkTheme, dynamicColor = false, content)` — default changed from `true` to `false` in Session 4 (decision D4) |

---

## `core/model/` ✅ **new (Session 6)**

The first package built directly onto the target shape in [`18-architecture-target.md`](18-architecture-target.md) §2 — pure Kotlin, zero Android-framework dependency (verified: its 24 tests run as plain JVM tests, no Robolectric needed, unlike `CanvasState`/`Stroke`). Implements [`16-canvas-spec.md`](16-canvas-spec.md) §2. Not yet used by any screen — see `VAHI-110` in `21-backlog.md` for what's left.

| File | Contents |
|---|---|
| `CoordinateSpaces.kt` | `ScreenPoint`, `DocumentPoint` (+`DocumentOffset`), `PagePoint`, `PageSize` (incl. `A4_PORTRAIT`) — the three coordinate spaces from doc 16 §2 as **distinct types**, not a shared `Offset` distinguished only by convention |
| `ViewTransform.kt` | `scale` + `pan`; `documentToScreen`/`screenToDocument` via the real `androidx.compose.ui.unit.Density`; `MAX_VIEW_SCALE`, `MIN_VIEW_SCALE_PAGE_FRACTION` |
| `PageLayout.kt` | The vertical page-strip model — precomputed page offsets (`O(1)` per lookup, not `O(n)`), `pageAt`/`toDocumentPoint`, width-centring for mixed page sizes, `PAGE_GAP_PT = 24` |

---

## `viewmodel/`

### `BaseViewModel.kt` ⚠️ · 47 lines — **written, never used**
`abstract class BaseViewModel<State>(initialState: State) : ViewModel()` exposing a read-only `StateFlow<State>` over a private `MutableStateFlow`, with `protected fun updateState(reducer)` and `protected fun setState(newState)`. Plus two empty marker interfaces, `UiState` and `UiEvent`. No subclass exists anywhere.

---

## `domain/` and `data/` — ✅ real, as of Session 6 (notebook metadata scope)

The four empty `package-info.kt` markers from earlier sessions are gone — replaced with real content, and renamed to match the singular naming in [`18-architecture-target.md`](18-architecture-target.md) §2 (`domain/models` → `domain/model`, `domain/usecases` → `domain/usecase`, the latter still empty; `data/datasource` deleted, unused).

| File | Contents |
|---|---|
| `domain/model/Notebook.kt` | The domain model — `id`, `title`, `colorArgb` (raw `Int`, not a Compose `Color` — this layer carries no UI-framework dependency), `paperStyle`, timestamps, `isPinned`. Trimmed from doc 17 §2.1's full target (no page/stroke fields — see the KDoc for why). |
| `domain/repository/NotebookRepository.kt` | Interface: `observeAll`/`observePinned` (`Flow`), `create`/`setPinned`/`delete`/`restore` (`Result`-returning, suspend). |
| `data/database/entity/NotebookEntity.kt` | Room row — `paperStyle` persisted as its enum *name* via a converter, never an ordinal. |
| `data/database/Converters.kt` | `PaperStyle` ↔ `String`. |
| `data/database/dao/NotebookDao.kt` | Flow-returning queries; the tombstone (`deletedAt`) filter lives here. |
| `data/database/VahiDatabase.kt` | `@Database(version = 1, exportSchema = false)` — see the note on why `exportSchema` is off for now, in `app/build.gradle.kts` and `21-backlog.md` `VAHI-301`. |
| `data/repository/RoomNotebookRepository.kt` | The only class that knows notebooks are backed by Room. `Color` ↔ ARGB `Int` mapping lives here. |
| `di/DatabaseModule.kt`, `di/RepositoryModule.kt` | Hilt: provides `VahiDatabase`/`NotebookDao`; binds `NotebookRepository` → `RoomNotebookRepository`. |
| `ui/feature/library/LibraryViewModel.kt` | `BaseViewModel`'s first real subclass. `LibraryUiState` lives in the same file. |
| `VahiApplication.kt` (root) | `@HiltAndroidApp` — Hilt's entry point. |

**Still empty:** `domain/usecase/` — no use case has real logic beyond a single repository call yet, and per doc 18 §3, ceremony-only use cases aren't worth writing.

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
| `app/src/test/.../drawing/CanvasStateTest.kt` | ✅ **new (Session 6)** | 6 tests, Robolectric — `clearAll()`/`undoClear()` (`VAHI-003`) |
| `app/src/test/.../drawing/CanvasStateUndoRedoTest.kt` | ✅ **new (Session 6)** | 11 tests, Robolectric — the command-history fix (`VAHI-007`/`VAHI-120`) |
| `app/src/test/.../core/model/CoordinateSpacesTest.kt` | ✅ **new (Session 6)** | 5 tests, plain JVM |
| `app/src/test/.../core/model/ViewTransformTest.kt` | ✅ **new (Session 6)** | 7 tests, plain JVM, incl. exact round-trips |
| `app/src/test/.../core/model/PageLayoutTest.kt` | ✅ **new (Session 6)** | 12 tests, plain JVM, incl. gap/boundary cases |

41 tests beyond the two templates, all passing. **Still nothing covers:** pointer arbitration/palm rejection (bug #14 — no code exists yet to test; that's `VAHI-112`/`VAHI-113`), any Compose UI/navigation smoke test (`ui-test-junit4` is on the classpath, unused).

---

## Quick counts

Re-counted at the very end of Session 6 (2026-09-08) via `find` and `wc -l`, after both the navigation-rewrite pass and the extended Room+Hilt+persistence pass.

| Metric | Value |
|---|---|
| Kotlin source files (main) | 34 |
| Kotlin test files | 8 (adds `RoomNotebookRepositoryTest.kt` to the Session 6 total) |
| Lines of Kotlin (main + test) | 4,371 |
| Largest file | `HomeScreen.kt`, ~870 lines — still not split; see `VAHI-206`'s honest status |
| Enums | 3 (`PaperStyle` — renamed from `CanvasType`, moved to `core/model`, `@Serializable` — `ToolType`, `WindowClass`) |
| Data classes | 10 (`CardMenuAnchor`, `Stroke`; `core/model`'s 6; `domain/model/Notebook`, `LibraryUiState`) |
| Non-composable classes | 6 (`CanvasState`, `BaseViewModel`, `LibraryViewModel`, `RoomNotebookRepository`, `Converters`, `VahiDatabase`) |
| Hilt-managed classes | 4 (`VahiApplication`, `DatabaseModule`, `RepositoryModule`, `LibraryViewModel`) |
| Files that are dead or empty | 1 (`domain/usecase/` has no content yet — no use case has real logic beyond a single repository call). `BaseViewModel.kt` no longer counts — it has a real subclass now. |
