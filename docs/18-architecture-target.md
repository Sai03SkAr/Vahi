# 18 — Target Architecture

> **Status: proposal.** Where the code is going. What it looks like today is in [`02-architecture.md`](02-architecture.md).
>
> Depends on: [D6](22-decisions.md#d6--ink-architecture-adopt-androidxink) · [D14](22-decisions.md#d14--single-gradle-module-until-a-named-trigger-fires) (single module) · [D15](22-decisions.md#d15--offline-first-the-local-database-is-the-source-of-truth) (offline-first).

---

## Part 1 — The gap, stated plainly

The package skeleton already declares MVVM over Clean-ish layering: `ui` → `domain` → `data`, with four `package-info.kt` markers staking out the layers. **None of it is wired up.** The real architecture today is:

```
Composable → remember { … } → in-memory state → discarded on exit
```

`BaseViewModel<State>` is well-formed and has **no subclass anywhere**. The four layer packages contain three-line comment files. Navigation is a `when` over an enum with a manually-managed side-channel argument.

This document is the target. The migration order is in [`21-backlog.md`](21-backlog.md).

> **One thing the current code gets right and must be preserved:** state flows *down* as parameters, events flow *up* as lambdas. Every screen takes callbacks rather than reaching for a shared object. Keep that convention as ViewModels arrive — a ViewModel injected three levels deep into a composable is a step backwards from what exists today.

---

## Part 2 — Target package structure

Single Gradle module ([D14](22-decisions.md#d14--single-gradle-module-until-a-named-trigger-fires)), organised so that a future module split is a directory move rather than a redesign.

```
com.vahi.app
├── VahiApplication.kt              @HiltAndroidApp
├── MainActivity.kt                 @AndroidEntryPoint, single activity
│
├── core/                           ── no dependencies on anything below ──
│   ├── model/                      Pure Kotlin: NotebookId, PageId, PagePoint,
│   │                               ViewTransform, PaperStyle, BrushSpec …
│   ├── result/                     Result<T>, AppError taxonomy
│   ├── time/                       Clock abstraction (testability)
│   └── util/
│
├── domain/                         ── depends on core only ──
│   ├── repository/                 INTERFACES only (17-data-spec §9)
│   ├── ink/                        InkEngine interface (16-canvas-spec §3.1)
│   ├── model/                      Notebook, Page, FinishedStroke, EditCommand
│   └── usecase/                    CreateNotebook, DeletePageWithUndo,
│                                   ExportNotebookToPdf, RecogniseePage …
│
├── data/                           ── implements domain interfaces ──
│   ├── database/
│   │   ├── VahiDatabase.kt
│   │   ├── dao/                    NotebookDao, PageDao, StrokeIndexDao
│   │   ├── entity/                 Room @Entity classes
│   │   └── migration/              Migration_1_2.kt …
│   ├── file/                       InkFileStore — .ink read/write/compact
│   ├── preferences/                DataStore (datastore-preferences)
│   ├── ink/                        AndroidxInkEngine — the ONLY file that
│   │                               imports androidx.ink
│   ├── pdf/                        v1.1
│   ├── recognition/                v1.2 — ML Kit
│   └── repository/                 Repository IMPLEMENTATIONS
│
├── ui/
│   ├── theme/                      Color · Type · Shape · Spacing · Theme
│   ├── designsystem/               VahiButton, VahiMenu, VahiEmptyState,
│   │                               VahiSnackbar, VahiTextField …
│   ├── navigation/                 VahiNavHost, Route definitions
│   ├── common/                     WindowInfo, modifiers, previews
│   └── feature/
│       ├── library/                LibraryScreen, LibraryViewModel,
│       │                           LibraryUiState, components/
│       ├── editor/                 EditorScreen, EditorViewModel, …
│       ├── settings/
│       ├── search/
│       └── onboarding/
│
└── di/                             Hilt modules
```

**Why feature-first under `ui/`.** `HomeScreen.kt` is 990 lines holding 15 composables and two data classes. Splitting by *type* (`screens/`, `components/`) puts a feature's pieces in different directories and makes it harder, not easier, to navigate. Splitting by *feature* keeps everything for the library together — and is exactly the shape a `:feature:library` module would take.

**The one rule that keeps this honest:** `ui/` never imports from `data/`, and `domain/` never imports Android. If a `domain` file needs `android.*`, the abstraction is in the wrong place.

---

## Part 3 — Layer contracts

```
┌─────────────────────────────────────────────────────────────┐
│  ui/feature/*                                               │
│    Composables — stateless, take UiState + (Event) -> Unit  │
│    ViewModels  — hold UiState, dispatch Events              │
└────────────────────────────┬────────────────────────────────┘
                             │ calls use cases / repository interfaces
┌────────────────────────────▼────────────────────────────────┐
│  domain/                                                    │
│    Interfaces + pure logic. No Android. No Room. No Compose. │
│    Unit-testable on the JVM with no instrumentation.         │
└────────────────────────────┬────────────────────────────────┘
                             │ implemented by
┌────────────────────────────▼────────────────────────────────┐
│  data/                                                      │
│    Room · files · DataStore · androidx.ink · ML Kit · PDF   │
└─────────────────────────────────────────────────────────────┘
```

| Rule | Rationale |
|---|---|
| `domain` has **zero** Android imports | Makes the interesting logic — coordinate math, command history, erase semantics — testable on the JVM in milliseconds. [`19-quality-bar.md`](19-quality-bar.md) §4 |
| `ui` depends on `domain` interfaces, never `data` classes | A Room entity must never reach a composable. Entities change for storage reasons; the UI should not. |
| Exactly **one** file imports `androidx.ink` | `data/ink/AndroidxInkEngine.kt`. Makes the [D6](22-decisions.md#d6--ink-architecture-adopt-androidxink) fallback a swap, not a rewrite. |
| Exactly **one** file imports `androidx.fragment` | v1.1, PDF only. Contain the interop ([D8](22-decisions.md#d8--pdf-annotation-builds-on-androidxpdf--androidxink)). |
| Use cases only when there is real logic | A use case that forwards one call to one repository is ceremony. Let the ViewModel call the repository directly. |

---

## Part 4 — State and events

One state object per screen, exposed as a `StateFlow`. One sealed event type in. `BaseViewModel<State>` — already written and waiting for its first subclass — is the base.

```kotlin
// ui/feature/library/LibraryUiState.kt
data class LibraryUiState(
    val phase: Phase = Phase.Loading,          // Loading | Content | Empty | Error
    val notebooks: List<NotebookCard> = emptyList(),
    val recents: List<NotebookCard> = emptyList(),
    val filter: LibraryFilter = LibraryFilter.All,
    val sort: LibrarySort = LibrarySort.RecentlyEdited,
    val viewMode: ViewMode = ViewMode.Grid,
    val searchQuery: String = "",
    val selection: Set<NotebookId> = emptySet(),
    val error: AppError? = null
) : UiState

sealed interface LibraryEvent : UiEvent {
    data object CreateNotebookClicked : LibraryEvent
    data class NotebookOpened(val id: NotebookId) : LibraryEvent
    data class NotebookDeleted(val id: NotebookId) : LibraryEvent
    data class DeleteUndone(val id: NotebookId) : LibraryEvent
    data class SearchChanged(val query: String) : LibraryEvent
    // …
}
```

**Rules**

1. **`phase` is an enum, not three booleans.** `isLoading && isEmpty && hasError` has eight combinations, six of which are nonsense and at least one of which will ship. Every screen models loading/content/empty/error as one exhaustive value — which is also how [`15-ux-blueprint.md`](15-ux-blueprint.md) §4.4 and §5.8 get built rather than forgotten.
2. **Composables are stateless.** `LibraryScreen(state, onEvent)`. The ViewModel is collected once in a thin `LibraryRoute` wrapper. This keeps every screen `@Preview`-able — `ui-tooling-preview` is a declared dependency with **zero** previews today.
3. **One-shot effects are a `Channel`, not state.** Navigation, snackbars and share sheets must not replay on rotation. A snackbar that reappears after every rotation is a bug users notice immediately.
4. **`SavedStateHandle` for anything that must survive process death** — current notebook, page index, scroll position, zoom.

### The editor is the exception worth naming

`EditorViewModel` must **not** hold every stroke in its `UiState`. A page has thousands of strokes and emitting a new immutable list per pointer sample would be catastrophic. Instead:

- `EditorUiState` holds document *metadata*: page list, current index, transform, tool selection, save status, undo/redo availability.
- Ink lives in an `InkSession` state holder scoped to the ViewModel — snapshot-backed, mutated in place, observed by the canvas directly.
- The active stroke never passes through `StateFlow` at all; it goes to the low-latency layer ([`16-canvas-spec.md`](16-canvas-spec.md) §6.1).

**This is a deliberate departure from the uniform pattern, and it is correct.** Ink is a real-time signal, not screen state. Forcing it through a `StateFlow` for architectural symmetry would cost the product its most important property.

---

## Part 5 — Navigation

`androidx.navigation:navigation-compose` with type-safe routes ([`15-ux-blueprint.md`](15-ux-blueprint.md) §3).

```kotlin
@Serializable data object Library
@Serializable data class Editor(val notebookId: String, val pageIndex: Int = 0)
@Serializable data class Search(val query: String = "")
@Serializable data object Settings
```

**What this deletes**

| Deleted | Replaced by |
|---|---|
| `AppDestination` enum | Route types |
| `AppRoot`'s `when` block | `NavHost` |
| `selectedNotebookType` side channel | A typed route argument |
| Both hand-written `BackHandler`s for navigation | The nav back stack |
| Bug #1 (rotation) | `SavedStateHandle` |
| Bug #8 (blank screen on desync) | Impossible by construction |

`BackHandler` survives only for genuinely local interception — closing an editor overlay, clearing a selection.

---

## Part 6 — Dependency injection

**Hilt.** Retrofitting DI later is tedious, and it should land **with** the data layer, not after.

```
di/
├── DatabaseModule.kt      @Provides VahiDatabase, DAOs
├── RepositoryModule.kt    @Binds interface → implementation
├── InkModule.kt           @Binds InkEngine → AndroidxInkEngine
├── DispatcherModule.kt    @IoDispatcher, @DefaultDispatcher qualifiers
└── PreferencesModule.kt
```

The dispatcher qualifiers matter more than they look: injecting dispatchers is what makes repository tests deterministic without `Thread.sleep`.

---

## Part 7 — Concurrency

| Work | Dispatcher | Scope |
|---|---|---|
| Room / DataStore reads | Room's own | `viewModelScope` |
| `.ink` file read/write | `@IoDispatcher` | `viewModelScope`, or `NonCancellable` on flush |
| Stroke geometry, culling, coordinate math | `@DefaultDispatcher` | `viewModelScope` |
| Live stroke rendering | **Main / render thread** | Not a coroutine |
| Thumbnail generation | `@DefaultDispatcher` | `WorkManager` |
| Compaction, orphan sweep, recognition | — | `WorkManager`, constrained to idle + charging |
| PDF page rasterisation | `@IoDispatcher` | `viewModelScope`, LRU-cached |

**The rule that prevents data loss:** the `ON_PAUSE` flush ([`16-canvas-spec.md`](16-canvas-spec.md) §10) runs in `withContext(NonCancellable)`. If it is cancelled with the scope, the app loses the user's last strokes precisely when the OS is about to kill it — the exact case the flush exists for.

---

## Part 8 — Errors

```kotlin
sealed interface AppError {
    data class Storage(val cause: StorageFailure) : AppError   // FULL, PERMISSION, CORRUPT, IO
    data class NotFound(val what: String) : AppError
    data class Ink(val cause: Throwable) : AppError
    data class Pdf(val cause: PdfFailure) : AppError           // v1.1
    data class Unknown(val cause: Throwable) : AppError
}
```

- Repositories return `Result<T>`; they do not throw across the layer boundary.
- Every `AppError` maps to a **user-facing message with a recovery action** ([`15-ux-blueprint.md`](15-ux-blueprint.md) §7.6). No raw exception text ever reaches the UI.
- **Storage errors are never swallowed.** Silence in a notes app reads as data loss.
- Non-fatal errors are logged to crash reporting with the *operation* as context, never with note content.

---

## Part 9 — Dependencies to add

Against the current catalog. Pin versions in `gradle/libs.versions.toml` — never inline.

| Group | Artifacts | For | When |
|---|---|---|---|
| Navigation | `navigation-compose` | F3 | Foundation |
| Lifecycle | `lifecycle-viewmodel-compose`, `lifecycle-runtime-compose` | F3 | Foundation |
| Coroutines | `kotlinx-coroutines-android` | Everywhere | Foundation |
| Serialization | `kotlinx-serialization-json` + plugin | Typed routes, `.vahi` manifest | Foundation |
| Room | `room-runtime`, `room-ktx`, `room-compiler` (KSP) | F4 | Foundation |
| Hilt | `hilt-android`, `hilt-compiler`, `hilt-navigation-compose` | DI | Foundation |
| DataStore | `datastore-preferences` | Settings | Foundation |
| **Ink** | the seven `androidx.ink:*` artifacts at **1.0.0** | F1 | Foundation |
| WorkManager | `work-runtime-ktx` | Background jobs | v1 |
| Testing | `mockk`, `turbine`, `room-testing`, `kotlinx-coroutines-test` | Tests | Foundation |
| PDF | `androidx.pdf:*` + `androidx.fragment` | v1.1 | Later |
| ML Kit | `com.google.mlkit:digital-ink-recognition` | v1.2 | Later |

**Two corrections to make first, before anything is added:**

1. **`BaseViewModel` currently compiles only through transitive dependencies.** It imports `androidx.lifecycle.ViewModel` and `kotlinx.coroutines.flow.*`, but neither `lifecycle-viewmodel-compose` nor `kotlinx-coroutines` is declared. Declare them explicitly, or a future dependency bump breaks the build in a confusing way.
2. **Migrate `kotlinOptions { jvmTarget = "11" }` → `compilerOptions { jvmTarget = JvmTarget.JVM_11 }`.** The old DSL is deprecated and will eventually fail the build.

**Also worth doing at the same time:** the Compose BOM is `2024.09.00` and core AndroidX libraries are from 2023, against `compileSdk 36`. That gap will produce confusing incompatibilities as soon as Room, Hilt and Navigation are added. Upgrade the catalog **before** adding to it, and verify with a full `assembleDebug`.

**And one to remove:** `androidx.media3` (ExoPlayer + UI), once the splash is reworked ([D11](22-decisions.md#d11--drop-the-video-splash-and-the-media3-dependency)). It is the largest dependency in the project and it exists to play one logo animation.

---

## Part 10 — Migration order

Order matters. Doing these in the wrong sequence means touching the same files twice.

| # | Step | Why here |
|---|---|---|
| 1 | ✅ **Done (Session 6)** — Upgrade the catalog; declare the transitive deps; fix the deprecated DSL | Do it before the tree gets bigger |
| 2 | Add Hilt + `VahiApplication` | Everything after this wants injection — still pending; nothing to inject yet, so deferred until a real repository exists (step 6) |
| 3 | 🟡 **Partially done (Session 6)** — Add Navigation; delete `AppDestination` / `AppRoot`'s `when` | Kills the *navigation-desync* half of rotation loss immediately. **Not yet done:** `CanvasState` is still `remember`-scoped, so the *canvas-content* half of rotation loss (dropped ink) is unchanged — that needs step 4 and 6/7, not step 3 alone. See `09-project-status.md` bug #1. |
| 4 | Introduce `core/model` + coordinate types | Nothing may be stored before this ([`17-data-spec.md`](17-data-spec.md) R1) |
| 5 | `InkEngine` interface + `AndroidxInkEngine` | Behind the interface from the first line |
| 6 | Room + DAOs + repositories + `InkFileStore` | Now safe: geometry has a coordinate space |
| 7 | `EditorViewModel` + `InkSession`; canvas onto the new model | The big one |
| 8 | `LibraryViewModel`; **split `HomeScreen.kt` while doing it** | Splitting it separately means refactoring the same 990 lines twice |
| 9 | Settings + DataStore | Unblocks handedness, paper theme |
| 10 | Design-system components + token migration | Ride along with each screen, not as a big-bang refactor |

**Steps 3 and 4 can run in parallel with 5** — navigation is ink-independent. Step 6 must not start before 4 finishes.

---

## Part 11 — Acceptance checklist

- [ ] `domain/` has zero Android imports (enforce with a lint rule or an import test)
- [ ] `androidx.ink` is imported in exactly one file
- [ ] No Room entity appears in any `@Composable` signature
- [ ] Every screen composable is stateless and previewable
- [ ] Every screen state has an exhaustive `phase`
- [ ] One-shot effects use a channel and do not replay on rotation
- [ ] Rotation and process death both restore: destination, arguments, page, scroll, zoom
- [ ] The `ON_PAUSE` flush is `NonCancellable`
- [ ] Repositories return `Result`; nothing throws across a layer boundary
- [ ] All versions live in the version catalog
- [ ] `./gradlew :app:assembleDebug` and `:app:testDebugUnitTest` both pass
