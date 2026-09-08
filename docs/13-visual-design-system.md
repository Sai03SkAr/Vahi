# 13 — Visual Design System

> **Status: proposal, nothing implemented.** This document is how Vahi *looks*. Companions: [`12-ux-plan.md`](12-ux-plan.md) (principles) and [`15-ux-blueprint.md`](15-ux-blueprint.md) (screens and states).
>
> This is a plan for a design system, not a description of one. What exists today is in [`06-design-system.md`](06-design-system.md) — a good foundation that is almost entirely bypassed by the screens that use it.
>
> ### ⚠️ Pending reconciliation with the Figma design
>
> The project owner has an existing design file: **[`vahi-v1`](https://www.figma.com/design/JJAGOm1by1gHE6Nxe4yUhx/vahi-v1?node-id=0-1)**. It is login-gated and **has not been read**, so the specific values proposed in Parts 3–6 below may contradict it.
>
> Per [V4](22-decisions.md#v4--figma-is-the-visual-source-of-truth-the-docs-describe-behaviour): **Figma is authoritative for how things look** — colour values, type sizes, spacing, radii, iconography, layout. This document is authoritative for the *structure* of the system (two-layer tokens, semantic naming, contrast floors, the adoption sequence in Part 9) and for anything Figma does not cover — dark mode, states, 200 % font scale, Hindi text expansion.
>
> Reconciling the two is tracked as `VAHI-004`. Until then, treat Parts 3–6's **method** as settled and their **specific hex values and sizes** as placeholders.

---

## Part 1 — The problem to solve

The project already has a complete Material 3 colour scheme, a full 15-role typography scale, and a shape scale. **Almost none of it is used.**

| Asset | Status |
|---|---|
| `Color.kt` — ~40 named tokens, with a KDoc saying *"No hardcoded colors should be used in screens"* | Screens contain dozens of inline `Color(0xFF…)` literals |
| `Type.kt` — all 15 M3 roles explicitly defined | Screens pass raw `fontSize = 14.sp`. Only `AuthScreen` uses named styles |
| `Shape.kt` — 4/8/12/16/24 dp scale | Screens use `RoundedCornerShape(n.dp)` directly |
| Light **and** dark colour schemes | `dynamicColor = true` means neither is used on Android 12+; and screens hardcode white anyway, so **dark mode is effectively broken** |

So the work is not "design a system" — it's **make the existing system authoritative**, extend it where it's thin, and remove the ad-hoc values that compete with it.

---

## Part 2 — Design direction

### The principle: *the paper is the hero*

Vahi is a tool. On the canvas, the user's own handwriting is the content, and every pixel of app chrome competes with it. The visual language should therefore be:

1. **Quiet on the canvas, warm in the library.** Personality belongs in onboarding, empty states and the library. The canvas recedes.
2. **Warm neutrals, not clinical greys.** The splash already uses `#F8F6F4` / `#FAF8F6` / `#F5F3F1` — warm off-whites that read as paper. That instinct is right; extend it across the app instead of the cold `#F5F5F5` / `#F8F8F8` currently used elsewhere.
3. **One accent, used sparingly.** The existing lavender/purple (`#7C4DFF`, `#B8B5FF`, `#C4BFFF`) is distinctive — it reads neither as Google-blue nor as GoodNotes-blue. Keep it, and reserve it for genuinely primary actions.
4. **Generous space over dense chrome.** Tablets have room. Use it.
5. **Borders and tint over shadows.** Flat, paper-like, and cheaper to render on mid-range hardware.
6. **Motion that explains.** Transitions should show where things came from. No decoration for its own sake.

### What to keep from the current design

The lavender palette, the warm off-whites, the pill-shaped buttons, the 16 dp card radius, and the two-pane auth composition are all good instincts. This is not a redesign — it's systematisation plus the missing pieces.

---

## Part 3 — Colour

### 3.1 The structural fix: semantic tokens

Right now the app has *raw* colour values with names like `VahiUpgradeButtonBg`. That's a token named after **where it's used**, which doesn't survive a redesign — the moment the Upgrade button moves or changes, the name lies.

**Adopt a two-layer system:**

```
Layer 1 — Palette      raw values, named by what they are
                       VahiPurple500, VahiNeutral100, VahiWarmWhite …

Layer 2 — Semantic     named by role, mapped per theme
                       surface, surfaceRaised, textPrimary, textSecondary,
                       border, accent, accentMuted, paper, ink …
```

Screens reference **only Layer 2**. Dark mode then becomes a remapping of Layer 2, not an edit to every screen — which is exactly why dark mode is broken today.

### 3.2 Proposed semantic tokens

| Token | Role | Light | Dark |
|---|---|---|---|
| `paper` | The writing surface | `#FFFFFF` | `#1A1A1A` (see 3.4) |
| `surface` | App background | `#FAF8F6` | `#121212` |
| `surfaceRaised` | Cards, sheets, menus | `#FFFFFF` | `#1E1E1E` |
| `surfaceSunken` | Wells, search field | `#F5F3F1` | `#0E0E0E` |
| `textPrimary` | Headings, body | `#2A2A2A` | `#E8E8E8` |
| `textSecondary` | Captions, metadata | `#5F5F5F` | `#A8A8A8` |
| `textTertiary` | Placeholders | `#767676` ⚠️ *not `#999999`* | `#8A8A8A` |
| `border` | Dividers, outlines | `#E4E1DE` | `#2E2E2E` |
| `accent` | Primary actions | `#7C4DFF` | `#B39DFF` |
| `accentMuted` | Accent backgrounds | `#E8E6FF` | `#2A2440` |
| `rule` | Ruled-page lines | `#D0D8E0` | `#2A3138` |
| `gridDot` | Grid-page dots | `#D0D0D0` | `#303030` |
| `danger` | Destructive actions | `#D93025` | `#F28B82` |

### 3.3 Contrast — three current values fail WCAG AA

Checked against white:

| Value | Contrast | Verdict |
|---|---|---|
| `#333333` | ≈ 12.6 : 1 | ✅ Passes AAA |
| `#666666` | ≈ 5.7 : 1 | ✅ Passes AA |
| **`#999999`** | ≈ **2.85 : 1** | ❌ **Fails AA** (needs 4.5:1) — used for search placeholder |
| **`#AAAAAA`** | ≈ **2.3 : 1** | ❌ **Fails AA** — used for text-field placeholders |

`#767676` is the lightest grey that passes AA on white (≈ 4.54:1). Use it as the floor for any text.

### 3.4 Dark mode and the paper problem

This needs a deliberate decision, because "dark mode" means something unusual in a notebook app: **should the paper go dark too?**

- White paper in a dark room is genuinely unpleasant — it's the brightest thing on screen.
- But inverting the paper changes what the user's notes *look like*, and exported/printed output must stay light.

**Recommendation: separate "App theme" from "Paper theme"** in Settings, as GoodNotes does.

| Setting | Options |
|---|---|
| **App theme** | Light · Dark · Follow system |
| **Paper theme** | White · Cream · Dark |

When Paper is Dark, render dark paper with light ink **for display only**; store ink colours unchanged and always export light. This keeps the document canonical and the reading experience comfortable.

### 3.5 Resolve the `dynamicColor` question

`VahiTheme(dynamicColor = true)` currently means the hand-authored schemes are unused on Android 12+.

**Recommendation: `dynamicColor = false`.** A notebook app benefits from a stable, recognisable identity, and both schemes are already written. (Tracked as decision **D4** in doc 11.)

---

## Part 4 — Typography

### 4.1 The font must support Devanagari — this is a hard constraint

Given the confirmed target market, `FontFamily.Default` (Roboto) is not adequate: Devanagari and other Indic scripts will fall back to whatever the device provides, producing inconsistent rendering across manufacturers.

**Recommendation:**

| Role | Font | Why |
|---|---|---|
| **UI text** | **Inter** (or Roboto Flex) + **Noto Sans Devanagari** fallback | Excellent legibility at small sizes; Noto covers the Indic scripts consistently |
| **Alternative single-family option** | **Noto Sans** across the board | One family, broadest script coverage, less pairing work — the safer choice if you don't want to manage fallbacks |
| **Brand / wordmark** | A distinctive display face, used **only** for the logo and the occasional hero moment | Personality without hurting legibility |

Bundle the fonts rather than relying on downloadable fonts, so rendering is identical everywhere. Verify the Indic glyph coverage of whatever you choose before committing.

### 4.2 Actually use the scale

`VahiTypography` is complete and correct. The work is adoption:

```kotlin
// instead of
Text("Maths notes", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)

// use
Text("Maths notes", style = MaterialTheme.typography.titleMedium)
```

Mapping for the existing UI:

| Usage | Role |
|---|---|
| Brand wordmark | `displayMedium` |
| Screen titles | `headlineSmall` |
| Card titles, dialog headings | `titleMedium` |
| Body copy, menu items | `bodyMedium` |
| Card metadata ("Created 4 jan 2025") | `bodySmall` |
| Buttons, pills, chips | `labelLarge` |
| Captions, page indicators | `labelSmall` |

### 4.3 Font scaling

Layouts use fixed `dp` throughout. Test at **200% system font scale** — pills, buttons and card titles will clip. Containers need to grow rather than truncate.

---

## Part 5 — Iconography

### 5.1 Remove every emoji

Twelve emoji and text glyphs are currently doing the work of icons:

`🐦` `🔍` `⊞` `≡` `⋮⋮` `🧮` `💻` `📓` `□` `↓` `←` `G`

They render differently on every manufacturer's font, they can't be tinted or sized reliably, they read as placeholder work, and screen readers announce them by unicode name — "bird", "abacus" — which is meaningless in context.

**Recommendation: Material Symbols (Rounded), weight 400, optical size 24.** Free, exhaustive, tintable, and the rounded style matches the app's soft pill-and-card language.

| Current | Replace with |
|---|---|
| 🐦 avatar | User initials, or a real avatar image |
| 🔍 search | `search` |
| ⊞ grid view | `grid_view` |
| ≡ list view | `format_list_bulleted` |
| ⋮⋮ overflow | `more_vert` |
| 🧮 💻 📓 notebook icons | **Page thumbnail** (doc 12, §4.1), with a colour swatch fallback |
| □ ≡ ⊞ ↓ modal glyphs | Small illustrated previews of the actual paper styles |
| ← ↓ back | `arrow_back` — and **unify these**: Grid uses `←`, Blank/Ruled use `↓`, for no stated reason |
| ✓ "selected view" | This icon is semantically meaningless — remove it |
| G Google | The official Google `G` mark (required by their branding guidelines) |

### 5.2 The paper-style picker deserves real previews

The create modal currently shows `□ ≡ ⊞ ↓` in grey squares. Since paper style is the *only* decision at creation time, show a **small rendered preview of each paper style** instead. It's more informative and it's the most memorable screen in the create flow.

---

## Part 6 — Shape, spacing and elevation

### 6.1 Shape

The existing scale is sound. Standardise usage:

| Element | Radius | Token |
|---|---|---|
| Chips, small controls | 8 dp | `small` |
| Dropdowns, menus | 12 dp | `medium` |
| Cards, sheets | 16 dp | `large` |
| Modals, dialogs | 24 dp | `extraLarge` |
| Pills, buttons, text fields | Fully rounded | `CircleShape` |

This eliminates the current ad-hoc set (6/8/12/14/16/20/24/26/28 dp), which has no rationale behind the variation.

### 6.2 Spacing

Adopt a **4 dp base grid**: `4 · 8 · 12 · 16 · 24 · 32 · 48`. Define them as named constants (`Spacing.sm`, `Spacing.md`, …) so they stop being magic numbers.

### 6.3 Elevation

Prefer **borders and surface tint over drop shadows** — flatter, more paper-like, and cheaper to render on mid-range hardware (which doc 11 establishes as a real constraint). Reserve actual shadow for genuinely floating things: the canvas tool rail, modals, and menus, at 2–8 dp maximum.

---

## Part 7 — Motion

Currently there is **no motion at all** between destinations — screens hard-cut. Adding a small, consistent motion vocabulary is one of the cheapest ways to make an app feel finished.

| Transition | Treatment | Priority |
|---|---|---|
| **Notebook card → canvas** | **Shared-element transition** — the card expands into the page | High. This is the signature moment of the app and it's very achievable in Compose. |
| Splash → library | Cross-fade | High |
| Modal open/close | Scale + fade from the invoking element | Medium |
| Dropdown open/close | Scale from the anchor corner | Medium |
| Tool rail auto-hide | Fade to 20% over ~150 ms | Medium |
| Page turn | Slide with slight overshoot | Medium |
| Snackbar | Slide up from bottom | Low |

**Durations:** 150–200 ms for small elements, 300–400 ms for screen transitions. Use Material 3's emphasised easing rather than linear. Respect the system "remove animations" accessibility setting.

---

## Part 8 — Component specifications

New components the plan requires, specified enough to build from.

### 8.1 Canvas tool rail

```
┌─────┐
│ ✒ ● │  Pen slot 1 — selected (accent ring + filled dot)
│ ✒   │  Pen slot 2
│ ✒   │  Pen slot 3
│ ▨   │  Highlighter
│ ⌫   │  Eraser
│ ⬚   │  Lasso            (v1.1)
├─────┤
│ +   │  Add page
└─────┘
```

- Width **56 dp**; items **48 dp** (meets the minimum touch target)
- Floats 16 dp from the edge, vertically centred
- `surfaceRaised`, 16 dp radius, 4 dp elevation
- Selected state: accent ring + the slot's own colour as a filled dot
- Auto-hides to 20% opacity while drawing
- Mirrors to the opposite edge per the handedness setting

### 8.2 Pen slot configuration popover

Opens on tapping an already-selected slot:
- 8–10 colour swatches + a custom picker
- 4 width presets shown as actual stroke previews, not numbers
- Pen type (fountain / ballpoint / pencil) once brushes exist
- Live preview stroke at the top

### 8.3 Notebook card

```
┌────────────────────────┐
│ ┌────────────────────┐ │  Page-1 thumbnail, 16:10,
│ │                    │ │  clipped to 12 dp radius
│ │    [thumbnail]     │ │
│ └────────────────────┘ │
│ ▌Maths notes       ⋮  │  ← colour bar = notebook colour
│  Edited 2 hours ago    │
└────────────────────────┘
```

- `surfaceRaised`, 16 dp radius, 1 dp `border`
- Colour survives as a 3 dp accent bar rather than the whole card fill — thumbnails need a neutral background to read against
- Title `titleMedium`, metadata `bodySmall` in `textSecondary`
- **"Edited 2 hours ago" is more useful than "Created 4 jan 2025"** — recency is what people navigate by
- Overflow button: 24 dp icon in a **48 dp** touch target
- Long-press → multi-select mode

### 8.4 Empty state

Centred, vertically ~40% down: illustration or large muted icon · `titleMedium` headline · `bodyMedium` supporting line in `textSecondary` · one primary action · optionally one secondary text action.

### 8.5 Snackbar with undo

`surfaceRaised` on dark scrim-free background, 12 dp radius, bottom-centre, 5 s timeout, action label in `accent`. Used for all destructive actions (doc 12, §4.7).

---

## Part 9 — Implementation approach

The order matters, because doing this in the wrong sequence means touching every screen twice.

| # | Step | Notes |
|---|---|---|
| 1 | **Build the semantic token layer** in `Color.kt` | Two layers as per §3.1. Nothing else can be done cleanly first. |
| 2 | **Add spacing constants** | Trivial, unblocks consistency. |
| 3 | **Set `dynamicColor = false`** (D4) | Two lines. Makes the palette real. |
| 4 | **Bundle the font family** | Required before any typography migration. |
| 5 | **Migrate screens to semantic tokens + type scale** | **Do this screen-by-screen, as each screen is touched for other reasons.** A big-bang refactor conflicts with the Track A/C work in doc 11. |
| 6 | **Replace all emoji with Material Symbols** | Can be done independently and immediately. |
| 7 | **Fix contrast and touch-target failures** | Small, and it's an accessibility correctness issue, not a preference. |
| 8 | **Add motion** | After screens are stable, not before. |
| 9 | **Verify dark mode end-to-end** | Only meaningful once step 5 is complete. |

> **Sequencing note against doc 11:** steps 1–4, 6 and 7 fit inside Track A and are low-risk. Step 5 should ride along with the ViewModel refactor in Track C — refactoring `HomeScreen.kt` for colour tokens *and then again* for state architecture would be doing the same 982 lines twice.

---

## Part 10 — Open design questions

> **⛔ Migrated to [`22-decisions.md`](22-decisions.md) (Session 5, 2026-09-08)**, where each has full reasoning and a stated resolution path. Summary only:

| # | Question | Where it landed |
|---|---|---|
| **V1** | Dark paper, or white paper only? | 🔬 Separate App theme from Paper theme; inversion is display-only, export always light. [V1](22-decisions.md#v1--app-theme-and-paper-theme-are-separate-settings) |
| **V2** | Which font family? | 🔬 Noto Sans + Noto Sans Devanagari, bundled. [V2](22-decisions.md#v2--font-family-noto-sans--noto-sans-devanagari) |
| **V3** | Keep lavender as the accent? | 🔬 Yes. [V3](22-decisions.md#v3--lavender-stays-as-the-brand-accent) |
| **V4** | Is there an existing Figma? | ✅ **Answered: yes** — [`vahi-v1`](https://www.figma.com/design/JJAGOm1by1gHE6Nxe4yUhx/vahi-v1?node-id=0-1). Figma owns visuals, the specs own behaviour. Not yet read. [V4](22-decisions.md#v4--figma-is-the-visual-source-of-truth-the-docs-describe-behaviour) |
| **V5** | Illustration style? | 🔲 Open. Recommendation: none for v1 — type plus one large muted glyph. [V5](22-decisions.md#v5--empty-state-illustration-style) |
