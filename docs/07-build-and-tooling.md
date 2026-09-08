# 07 — Build, Dependencies and Tooling

## Project structure

```
Vahi/
├── settings.gradle.kts        rootProject.name = "Vahi";  include(":app")
├── build.gradle.kts           root — declares plugins with `apply false`
├── gradle.properties          JVM args, AndroidX flags
├── gradle/
│   ├── libs.versions.toml     version catalog (single source of dependency truth)
│   └── wrapper/               Gradle 8.13 wrapper
├── gradlew / gradlew.bat      wrapper scripts
├── local.properties           SDK path — gitignored, machine-specific
├── .idea/                     Android Studio config (partially tracked)
└── app/                       the one and only module
```

Single-module. If the project grows, natural split points are `:core-ui` (theme + components), `:feature-canvas`, and `:data`.

## Gradle

| Setting | Value |
|---|---|
| Gradle wrapper | `8.13-bin` |
| Android Gradle Plugin | `8.13.2` |
| Kotlin | `2.0.21` |
| Build script language | Kotlin DSL (`.kts`) throughout |
| Dependency declaration | Version catalog (`gradle/libs.versions.toml`) |
| JVM args | `-Xmx2048m -Dfile.encoding=UTF-8` |
| `android.useAndroidX` | `true` |
| `android.nonTransitiveRClass` | `true` |
| `kotlin.code.style` | `official` |
| Parallel builds | commented out |

`settings.gradle.kts` uses `RepositoriesMode.FAIL_ON_PROJECT_REPOS` (centralised repository declaration) and filters the `google()` plugin repo by group regex — the standard modern template.

## Android configuration — `app/build.gradle.kts`

| Setting | Value | Note |
|---|---|---|
| `namespace` / `applicationId` | `com.vahi.app` | |
| `compileSdk` | `release(36)` | Android 16 — uses the newer block syntax |
| `minSdk` | `26` | Android 8.0 Oreo |
| `targetSdk` | `36` | |
| `versionCode` / `versionName` | `1` / `"1.0"` | never bumped |
| `sourceCompatibility` / `targetCompatibility` | Java 11 | |
| `kotlinOptions.jvmTarget` | `"11"` | |
| `buildFeatures.compose` | `true` | Compose compiler comes from the Kotlin 2.0 `kotlin-compose` plugin |
| `testInstrumentationRunner` | `androidx.test.runner.AndroidJUnitRunner` | |

### Build types

Only `release` is customised:
```kotlin
release {
    isMinifyEnabled = false
    proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
}
```

Notes:
- **`isMinifyEnabled = false`** — no code shrinking, obfuscation, or resource shrinking in release builds. Fine for a prototype; should be `true` (with `isShrinkResources = true`) before shipping.
- `proguard-rules.pro` is the untouched template — all rules commented out.
- No `debug` block, no signing config, no build flavours, no `buildConfigField`s.

### Deprecation to watch

`kotlinOptions { jvmTarget = "11" }` is deprecated in favour of the `compilerOptions { jvmTarget = JvmTarget.JVM_11 }` DSL on AGP 8.x + Kotlin 2.x. It still works but will warn.

## Dependencies

Every dependency is AndroidX except JUnit. The complete list:

### Version catalog

```toml
[versions]
agp = "8.13.2"
kotlin = "2.0.21"
coreKtx = "1.10.1"
junit = "4.13.2"
junitVersion = "1.1.5"
espressoCore = "3.5.1"
lifecycleRuntimeKtx = "2.6.1"
activityCompose = "1.8.0"
composeBom = "2024.09.00"
media3 = "1.2.0"
```

### Runtime

| Artifact | Version | Why |
|---|---|---|
| `androidx.core:core-ktx` | 1.10.1 | Kotlin extensions |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.6.1 | Lifecycle-aware coroutines |
| `androidx.activity:activity-compose` | 1.8.0 | `ComponentActivity.setContent`, `BackHandler`, `enableEdgeToEdge` |
| `androidx.compose:compose-bom` | 2024.09.00 | Aligns all Compose artifact versions |
| `androidx.compose.ui:ui` | via BOM | Core Compose |
| `androidx.compose.ui:ui-graphics` | via BOM | `Path`, `Color`, `DrawScope` |
| `androidx.compose.ui:ui-tooling-preview` | via BOM | `@Preview` annotation |
| `androidx.compose.material3:material3` | via BOM | Material 3 components + icons |
| `androidx.media3:media3-exoplayer` | 1.2.0 | Splash video playback |
| `androidx.media3:media3-ui` | 1.2.0 | `PlayerView`, `AspectRatioFrameLayout` |

### Test

| Artifact | Version | Scope |
|---|---|---|
| `junit:junit` | 4.13.2 | `testImplementation` |
| `androidx.test.ext:junit` | 1.1.5 | `androidTestImplementation` |
| `androidx.test.espresso:espresso-core` | 3.5.1 | `androidTestImplementation` |
| `androidx.compose.ui:ui-test-junit4` | via BOM | `androidTestImplementation` |
| `androidx.compose.ui:ui-tooling` | via BOM | `debugImplementation` |
| `androidx.compose.ui:ui-test-manifest` | via BOM | `debugImplementation` |

### Notable absences

| Not present | Consequence |
|---|---|
| `androidx.navigation:navigation-compose` | Hand-rolled navigation, no back stack |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | `viewModel()` is unavailable in composables |
| `org.jetbrains.kotlinx:kotlinx-coroutines-*` | Used by `BaseViewModel` but resolved only transitively |
| `androidx.room:*` or any DB | No persistence |
| Retrofit / Ktor / OkHttp | No networking |
| Hilt / Koin | No DI |
| `androidx.compose.material3:material3-window-size-class` | Breakpoints hand-rolled in `WindowInfo.kt` |
| Coil / Glide | No remote image loading |
| `androidx.core:core-splashscreen` | No system splash-screen API |

### Version-currency note

`composeBom = 2024.09.00`, `activityCompose = 1.8.0`, `lifecycleRuntimeKtx = 2.6.1`, and `coreKtx = 1.10.1` are noticeably older than `compileSdk 36` / AGP 8.13. Upgrading the BOM and the AndroidX libraries is a low-risk, worthwhile chore — some Compose APIs used here (`LocalConfiguration` for window size, `Modifier.blur`) have newer, better-supported equivalents in later releases.

## Assets and resources

| Path | Size / Type | Purpose |
|---|---|---|
| `app/src/main/assets/splash.mp4` | ~800 KB video | Splash animation, loaded as `asset:///splash.mp4` |
| `res/drawable/vahi_logo.png` | raster | Wordmark in the Home top bar |
| `res/drawable/ic_launcher_foreground_img.png` | raster | Adaptive-icon foreground |
| `res/drawable/ic_launcher_background.xml` | vector | Adaptive-icon background |
| `res/drawable/ic_launcher_foreground.xml` | vector | ⚠️ orphaned — the old default robot foreground |
| `res/mipmap-{h,m,xh,xxh,xxxh}dpi/ic_launcher[_round].png` | raster | Legacy launcher icons |
| `res/mipmap-anydpi/ic_launcher[_round].xml` | adaptive icon | API 26+ launcher icons |
| `res/values/strings.xml` | — | Exactly one string: `app_name = "Vahi"` |
| `res/values/colors.xml` | — | ⚠️ unused template colours |
| `res/values/themes.xml` | — | `Theme.Vahi` |
| `res/xml/backup_rules.xml` | — | ⚠️ template, all rules commented out |
| `res/xml/data_extraction_rules.xml` | — | ⚠️ template, all rules commented out |

> **Localisation:** every user-facing string in the app is hardcoded in Kotlin — "Username", "Login", "Continue with Google", "Create new notebook", "Select the style that fits your needs", and so on. `strings.xml` holds only the app name. Extracting strings is a prerequisite for any translation work and for accessibility tooling.

## Manifest

```xml
<application
    android:allowBackup="true"
    android:dataExtractionRules="@xml/data_extraction_rules"
    android:fullBackupContent="@xml/backup_rules"
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name"
    android:roundIcon="@mipmap/ic_launcher_round"
    android:supportsRtl="true"
    android:theme="@style/Theme.Vahi">
    <activity android:name=".MainActivity" android:exported="true"
              android:label="@string/app_name" android:theme="@style/Theme.Vahi">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>
</application>
```

- **Zero permissions requested.** (Import PDF will need storage/document access; sign-in will need `INTERNET`.)
- `supportsRtl="true"` is declared, but no layout uses `start`/`end`-aware reasoning beyond Compose's defaults, and the hardcoded English strings aren't RTL-tested.
- No `android:screenOrientation` lock and no `configChanges` — so every rotation destroys and recreates the Activity, resetting `AppRoot` to `SPLASH` and discarding all ink.

## Commands

> Verified during this documentation pass: `./gradlew :app:compileDebugKotlin` → **BUILD SUCCESSFUL**. The current working tree compiles cleanly.

```bash
./gradlew tasks                  # list available tasks
./gradlew assembleDebug          # build debug APK → app/build/outputs/apk/debug/
./gradlew assembleRelease        # build release APK (unsigned, unminified)
./gradlew installDebug           # install on a connected device
./gradlew test                   # JVM unit tests
./gradlew connectedAndroidTest   # instrumented tests (device/emulator required)
./gradlew lint                   # Android Lint
./gradlew clean                  # clear build outputs
```

## Testing

Two files, both untouched Android Studio templates:

- `app/src/test/java/com/vahi/app/ExampleUnitTest.kt` — asserts `2 + 2 == 4`.
- `app/src/androidTest/java/com/vahi/app/ExampleInstrumentedTest.kt` — asserts the package name is `com.vahi.app`.

**There are no tests of any Vahi code.** The infrastructure for Compose UI tests is already on the classpath (`ui-test-junit4`, `ui-test-manifest`), so adding them is a matter of writing them. `CanvasState` is the most valuable and easiest target — it is a plain class with no Android dependencies beyond `Path`/`Color`, and its undo/redo/erase semantics have real edge cases.

## Version control state

- One commit on `main`: `89bafd3 "update"`, with an `origin/main` remote configured.
- The working tree carries substantial uncommitted work: the entire `ui/components/drawing/` package, `CanvasScaffold.kt`, the icon replacement, and large rewrites of `AuthScreen`, `HomeScreen`, `SplashScreen`, and all three paper styles (≈ 660 insertions / 288 deletions across 25 files).
- `.gitignore` covers the standard set (`.gradle`, `/build`, `local.properties`, `.DS_Store`, `.idea/caches`, `.idea/workspace.xml`). `app/.gitignore` additionally ignores `/build`. No `.DS_Store` is tracked, though several exist on disk.
- Eleven `.idea/*.xml` files **are** tracked (`gradle.xml`, `misc.xml`, `compiler.xml`, `runConfigurations.xml`, `codeStyles/`, `deploymentTargetSelector.xml`, …), and two more (`deviceManager.xml`, `vcs.xml`) are untracked and would be picked up by a blanket `git add`. Decide deliberately which IDE files belong in version control.
