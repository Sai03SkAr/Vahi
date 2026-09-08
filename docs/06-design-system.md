# 06 — Design System

Everything under `com.vahi.app.ui.theme`, plus the responsive helper in `ui/components/WindowInfo.kt`.

---

## `VahiTheme` — `ui/theme/Theme.kt`

```kotlin
@Composable
fun VahiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
)
```

Resolves a colour scheme in this priority order:

1. **`dynamicColor && SDK ≥ 31`** → `dynamicLightColorScheme(context)` / `dynamicDarkColorScheme(context)` (Material You, derived from the user's wallpaper)
2. **`darkTheme`** → `VahiDarkColorScheme`
3. otherwise → `VahiLightColorScheme`

then applies `MaterialTheme(colorScheme, typography = VahiTypography, shapes = VahiShapes, content)`.

> ⚠️ **`dynamicColor` defaults to `true`.** On Android 12+ — which is most devices, and `minSdk` is 26 so the range is wide — the hand-authored Vahi schemes are **never used**. The brand palette in `Color.kt` is bypassed on the majority of installs. If Vahi wants a consistent brand identity, pass `dynamicColor = false` from `MainActivity`, or drop the dynamic branch entirely. This currently matters less than it should, because most screens hardcode their colours anyway (see below).

Both schemes are complete Material 3 mappings covering `primary`, `onPrimary`, `secondary`, `onSecondary`, `tertiary`, `onTertiary`, `background`, `onBackground`, `surface`, `onSurface`, `surfaceVariant`, `onSurfaceVariant`, `error`, `onError`, and `outline`.

---

## Colours — `ui/theme/Color.kt`

The file's own KDoc states the rule:

> *"Centralized color definitions for consistent theming across the app. No hardcoded colors should be used in screens - always reference these values."*

⚠️ **That rule is widely violated.** `HomeScreen.kt` alone contains dozens of inline `Color(0xFF…)` literals (`#333333`, `#666666`, `#E0E0E0`, `#F8F8F8`, `#E8D4A8`, `#7B68EE`, …), and `AuthScreen`, `CanvasScaffold`, `RuledCanvas`, `GridCanvas`, and `SplashScreen` all do the same. Consolidating these into `Color.kt` is a well-defined, low-risk cleanup.

### Material scheme tokens

| Token | Light | Dark |
|---|---|---|
| Primary | `#1A73E8` | `#8AB4F8` |
| On primary | `#FFFFFF` | `#003A6E` |
| Secondary | `#5F6368` | `#BDC1C6` |
| On secondary | `#FFFFFF` | `#303030` |
| Tertiary | `#1E8E3E` | `#81C995` |
| On tertiary | `#FFFFFF` | `#003A23` |
| Background | `#FFFBFE` | `#121212` |
| On background | `#1C1B1F` | `#E3E3E3` |
| Surface | `#FFFBFE` | `#1E1E1E` |
| On surface | `#1C1B1F` | `#E3E3E3` |
| Surface variant | `#E7E0EC` | `#2D2D2D` |
| On surface variant | `#49454F` | `#CAC4D0` |
| Error | `#D93025` | `#F28B82` |
| On error | `#FFFFFF` | `#601410` |
| Outline | `#79747E` | `#938F99` |

The base palette is recognisably Google/Material-derived (Google Blue `#1A73E8`, Google Green `#1E8E3E`, Google Red `#D93025`).

### Component tokens

**Home screen**

| Constant | Value | Used for |
|---|---|---|
| `VahiUpgradeButtonBg` | `#4A4A4A` | Dark "Upgrade" pill |
| `VahiSettingsButtonBg` | `#F5F5F5` | Light "Settings" pill |
| `VahiImportButtonBg` | `#1A73E8` | Blue "Import" pill |
| `VahiCardLavender` | `#C4BFFF` | Lavender notebook card / Auth right pane |
| `VahiCardMauve` | `#D3C3C3` | Mauve notebook card *(declared but unused — the card hardcodes `0xFFD3C3C3`)* |
| `VahiFilterSelected` | `#F0F0F0` | Selected filter pill |
| `VahiCreateCardBg` | `#F5F5F5` | "Create new notebook" tile |
| `VahiPlusCircleBg` | `#E8E6FF` | Lavender circle behind the `+` |

**Auth / inputs**

| Constant | Value | Used for |
|---|---|---|
| `VahiInputBorder` | `#B8B5FF` | Unfocused text-field border |
| `VahiInputBorderFocused` | `#8B85FF` | Focused text-field border |
| `VahiGoogleButtonBg` | `#FFFFFF` | *(declared but unused — the button hardcodes `Color.White`)* |
| `VahiAppleButtonBg` | `#000000` | Apple sign-in button |

**Accents**

| Constant | Value | Used for |
|---|---|---|
| `VahiAccentPurple` | `#7C4DFF` | Login button, "Vahi" wordmark, text cursor |
| `VahiAccentBlue` | `#2196F3` | *(declared but unused)* |
| `VahiAccentTeal` | `#00BFA5` | *(declared but unused)* |

### Ad-hoc colour families found inline

Recurring literals that should become named tokens:

| Hex | Where it appears | Suggested role |
|---|---|---|
| `#333333` | Everywhere — all primary text | `VahiTextPrimary` |
| `#666666` | Secondary text, glyphs | `VahiTextSecondary` |
| `#999999` / `#AAAAAA` | Placeholders | `VahiTextPlaceholder` |
| `#E0E0E0` / `#E8E8E8` | Borders, dividers | `VahiBorderSubtle` |
| `#F5F5F5` / `#F8F8F8` / `#F0F0F0` | Neutral fills | `VahiSurfaceMuted` |
| `#D0D8E0` | Ruled-canvas lines | `VahiRuleLine` |
| `#D0D0D0` | Grid-canvas dots | `VahiGridDot` |
| `#F8F6F4` / `#FAF8F6` / `#F5F3F1` | Splash gradient | `VahiSplashGradient*` |

### `res/values/colors.xml`

Contains only the Android Studio template colours (`purple_200`, `purple_500`, `purple_700`, `teal_200`, `teal_700`, `black`, `white`). **None of them are referenced by anything.** Safe to delete.

---

## Typography — `ui/theme/Type.kt`

`VahiTypography` is a **complete** Material 3 type scale — all 15 roles are explicitly defined rather than left to defaults:

| Group | Roles | Sizes |
|---|---|---|
| Display | Large / Medium / Small | 57 / 45 / 36 sp |
| Headline | Large / Medium / Small | 32 / 28 / 24 sp |
| Title | Large / Medium / Small | 22 / 16 / 14 sp (Medium weight) |
| Body | Large / Medium / Small | 16 / 14 / 12 sp |
| Label | Large / Medium / Small | 14 / 12 / 11 sp (Medium weight) |

Every role uses `FontFamily.Default` — the file notes *"Custom fonts can be added in future iterations."* Line heights and letter spacings follow the Material 3 reference values (e.g. `displayLarge` at `-0.25 sp` tracking, `bodyLarge` at `0.5 sp`).

⚠️ In practice screens mostly ignore this scale too, passing raw `fontSize = 14.sp` / `16.sp` / `18.sp` to `Text` instead of `style = MaterialTheme.typography.bodyMedium`. `AuthScreen` is the only screen that uses named styles (`displayMedium`, `bodyLarge`).

---

## Shapes — `ui/theme/Shape.kt`

| Role | Radius | Intended for |
|---|---|---|
| `extraSmall` | 4 dp | chips, small buttons |
| `small` | 8 dp | text fields, small cards |
| `medium` | 12 dp | cards, dialogs |
| `large` | 16 dp | modals, bottom sheets |
| `extraLarge` | 24 dp | expanded containers |

Screens again mostly use `RoundedCornerShape(n.dp)` directly. The de-facto radius vocabulary in use is: **6/8 dp** small controls, **12 dp** dropdowns, **14/16 dp** cards and modals, **20/24/26/28 dp** pills and text fields.

---

## Responsive design — `ui/components/WindowInfo.kt`

```kotlin
enum class WindowClass { Compact, Medium, Expanded }

@Composable
fun windowClass(): WindowClass {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    return when {
        screenWidthDp < 600 -> WindowClass.Compact    // phones, portrait
        screenWidthDp < 840 -> WindowClass.Medium     // phone landscape, small tablets
        else                -> WindowClass.Expanded   // tablets, desktop
    }
}
```

Standard Material breakpoints, hand-rolled rather than using `androidx.compose.material3.windowsizeclass` (which is not a declared dependency).

**Adoption:** used in `AuthScreen` and the unused `NotebookScreen`. Not used in `HomeScreen`, `CanvasScreen`, or any paper style.

**Fixed dimensions that assume a tablet:**

| Element | Size |
|---|---|
| Notebook card | 260 × 200 dp |
| Notebook grid | 3 fixed columns, 24 dp gaps |
| Create-notebook modal | 680 dp wide |
| Auth card | 380 dp wide |
| Auth panes | 50 / 50 split |
| Home page padding | 24 dp |
| Search field | 280 dp |
| Card menu / Settings / Profile | 180 / 260 / 240 dp |
| (Planned) right side panel | 320 dp |

At Compact width a 3-column grid of 260 dp cards needs ≥ 828 dp of content width — it simply will not fit.

---

## Android resource theming

`res/values/themes.xml`:

```xml
<style name="Theme.Vahi" parent="android:Theme.Material.Light.NoActionBar" />
```

Applied to both the `<application>` and the `<activity>`. Points to note:

- It inherits from the **framework** `android:Theme.Material.Light`, not from an AppCompat or Material Components theme. That's acceptable for a pure-Compose app with a `ComponentActivity`, but it means no XML-level Material 3 attributes are available, and it's hardcoded Light regardless of system dark mode — so the pre-Compose window background is always light even when `VahiTheme` resolves dark.
- There is no `values-night/themes.xml`.
- No splash-screen theme (`Theme.SplashScreen` / `windowSplashScreenBackground`) is configured; the video splash is a Compose screen, so there will be a brief default-themed window before it appears.

`MainActivity` calls `enableEdgeToEdge()`, which draws behind the system bars — but **no composable applies window insets** (`Modifier.systemBarsPadding()`, `WindowInsets.safeDrawing`, etc.). Content near the top and bottom edges can therefore sit under the status bar or gesture navigation bar. The Home top bar's 24 dp padding partially masks this; the Blank/Ruled back arrow at `BottomStart` is the most exposed.

---

## App icon

`res/mipmap-anydpi/ic_launcher.xml` and `ic_launcher_round.xml` are adaptive icons:

```xml
<adaptive-icon>
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground_img" />
</adaptive-icon>
```

- The foreground was switched from the generated vector (`ic_launcher_foreground.xml`, the default Android robot) to a raster `ic_launcher_foreground_img.png`. **The old vector is still in the repo, now unreferenced.**
- No `<monochrome>` layer is declared, so themed icons on Android 13+ fall back to a generic treatment.
- The legacy density-bucket `.webp` files were replaced with `.png` equivalents across hdpi → xxxhdpi.
- `res/drawable/vahi_logo.png` is the in-app wordmark, used in the Home top bar.
