# 12 — UX Plan: Making Vahi Easy and Fast to Use

> **Status: proposal.** This document holds the UX **principles and priorities** — why time-to-ink matters, why the auth wall must go, what to fix first.
>
> **The three UX documents and what each owns:**
> - **`12-ux-plan.md`** *(this)* — principles and priorities. *Why.*
> - [`13-visual-design-system.md`](13-visual-design-system.md) — colour, type, icons, motion. *How it looks.*
> - [`15-ux-blueprint.md`](15-ux-blueprint.md) — screen inventory, nav graph, wireframes, every state. **Read that one when you are about to build a screen.**
>
> Context settled in [`22-decisions.md`](22-decisions.md): a real product · paged A4 notebooks · tablet-only v1 · students on Android tablets, Indian market. Several recommendations here are now formal decisions — the auth wall (D10), the splash (D11), the Upgrade pill (D16).
>
> **Session 5 note:** Part 7's accessibility list has grown. The tool rail shipped in Session 4 has 40 dp touch targets and no content descriptions — see [`09-project-status.md`](09-project-status.md) #15 and #16.

---

## Part 1 — The single number that matters: time-to-ink

For a note-taking app, the most important usability metric is **how long from tapping the icon to making a mark**. A student in a lecture has already missed a sentence by the time your splash finishes.

### Where Vahi is today

| Step | Cost |
|---|---|
| 1. Launch → splash video | Forced wait, full video length, **every launch** |
| 2. Auth screen → tap **Login** | 1 tap — **every launch**, because no session persists |
| 3. Home → tap **Create new notebook** | 1 tap |
| 4. Modal → tap **Blank** | 1 tap |
| 5. Canvas | Finally writing |

**3 taps + a forced video, on every single launch.** And it lands you on a *new* notebook every time — there is no way to get back to what you were writing yesterday.

### Where it should be

**Target: writing within 2 seconds and at most 1 tap, on the second and every subsequent launch.**

Four changes get there, and each is small:

| # | Change | Effect |
|---|---|---|
| 1 | **Splash only on first launch.** Use the Android 12+ system splash for cold start; show the video as a one-time brand moment. Make it tap-to-skip regardless. | Removes the forced wait |
| 2 | **Persist the session.** Once signed in (or skipped), never show auth again. | Removes 1 tap and a screen |
| 3 | **No auth wall at all** — see Part 2. Let people in and write immediately; ask for an account only when they want sync. | Removes the screen entirely for new users too |
| 4 | **"Continue where you left off"** as the primary action on Home, plus a persistent quick-note action. | Removes 2 taps for the common case |

> **Design rule to adopt:** *every* feature proposal gets checked against time-to-ink. If it adds a step between launch and writing, it needs to justify itself.

---

## Part 2 — Remove the auth wall

Right now, a first-time user must face a login form with **four fields** before they can see a single thing the app does. For a free app competing against installed incumbents, that is the highest-drop-off screen you could design.

### The problem, concretely

- The form asks for username, e-mail, password **and** password confirmation — that's a *registration* form — but the button says **Login**. Users cannot tell what's being asked of them.
- There's no way to skip, and no way to just look around.
- The app is offline-first by design (see doc 11), so **an account is not technically required for anything in v1.**

### Recommended flow

```
First launch
  → brand moment (video, skippable)
  → straight into an empty library with a warm empty state
  → "Create your first notebook"  |  "Import a PDF"
  → writing

Account is requested later, contextually:
  → when the user taps Sync / Backup
  → when they hit a free-tier limit
  → from Settings → Account
  → via a gentle, dismissible prompt after N notebooks ("Back up your notes?")
```

### If an auth screen is shown, fix these

1. **Separate sign-in from sign-up.** Two clear modes, or a progressive flow (enter email → we tell you which it is). Never four ambiguous fields under one "Login" button.
2. **Social first.** Google Sign-In is one tap on Android and should be the primary action. Email/password is the fallback, collapsed behind "Use email instead".
3. **Drop Apple sign-in for v1** unless there is an iOS app. It's a black button that does nothing and costs credibility.
4. **Real validation with inline errors** — email format, password rules, password/confirmation match — shown as the user types, not on submit.
5. **Loading and error states.** Currently the button is instant and infallible. Real auth is neither.
6. **"Continue without an account"** must be visible, not hidden.

---

## Part 3 — The canvas: the most important UI in the product

The canvas currently has **one control: a back arrow.** Everything else — eraser, undo, redo, colour, width — exists in code and is unreachable. This is the single biggest UI gap in the project, and it's where design effort pays back most.

### 3.1 Where the toolbar goes — and why it's not obvious

On a tablet held in landscape, a **right-handed writer's palm covers the bottom-right quadrant**. Most tablet apps put controls along the top, which is far from the hand and requires a full reach across the screen.

```
┌─────────────────────────────────────────────┐
│  top edge — far from hand, safe but distant │
│                                             │
│ L                                         R │
│ e                                         i │
│ f          C A N V A S                    g │
│ t                                         h │
│                                           t │
│                              ▒▒▒▒▒▒▒▒▒▒▒▒▒▒ │ ← palm/hand
│  bottom edge                 ▒▒ OCCLUDED ▒▒ │   occlusion zone
└─────────────────────────────────────────────┘        (right-handed)
```

**Recommendation: a floating vertical rail on the left edge for a right-handed user, mirrored to the right for a left-handed user, with a handedness setting.** Left edge is close to the non-writing hand, never occluded, and leaves maximum vertical canvas.

Keep a minimal top bar for *document*-level actions (back, notebook title, page indicator, undo/redo, overflow) — those are used rarely, so distance is fine.

```
┌─────────────────────────────────────────────┐
│ ←  Maths notes            3/12   ↶ ↷    ⋯   │  ← document bar (rare actions)
├───┬─────────────────────────────────────────┤
│ ✒ │                                         │
│ ✒ │                                         │
│ ✒ │            P A G E                      │  ← tool rail (constant actions)
│ ▨ │                                         │     floats over the page,
│ ⌫ │                                         │     auto-hides while writing
│ ⬚ │                                         │
│ + │                                         │
└───┴─────────────────────────────────────────┘
```

### 3.2 Pen slots — the pattern that makes it fast

Do **not** build "select pen → open colour picker → choose colour → close → write". That's four interactions for something done constantly.

Instead: **3–5 preconfigured pen slots, always visible.**

- **Tap** a slot → select that pen instantly
- **Tap the already-selected slot** (or long-press) → open its configuration (colour, width, type)
- Slots persist across sessions

This is how Notability and Samsung Notes work, and it collapses the common case to a single tap. It is the highest-value interaction design decision on the canvas.

### 3.3 Tool set for v1

| Tool | Priority | Notes |
|---|---|---|
| **Pen** | v1 | Via pen slots. Colour + width. |
| **Highlighter** | v1 | Translucent, wide, draws *behind* ink. Students highlight constantly. |
| **Eraser** | v1 | Stroke-erase by default; area-erase as an option. Already implemented, just unreachable. |
| **Undo / redo** | v1 | Already implemented, unreachable. Put in the document bar *and* on gestures. |
| **Lasso select** | v1.1 | Move, resize, delete, change colour of a selection. Needs stroke geometry (F1). |
| **Pan/zoom** | v1 | Gesture-based, no tool needed. |
| **Add page** | v1 | |
| **Text box** | v2 | |
| **Shapes / straight lines** | v2 | Shape recognition is a nice "wow" moment. |
| **Image insert** | v2 | Photo of a whiteboard is a real student use case. |
| **Laser pointer** | v3 | Presentation-only. |

### 3.4 Auto-hide

When the stylus or finger starts drawing, **fade the tool rail to ~20% opacity** (or hide it). Restore on lift after a short delay, or on tap. This maximises canvas area and is what every premium note app does. It's cheap to build and immediately makes the app feel considered.

### 3.5 Gestures — adopt the conventions, don't invent

Users arrive from GoodNotes, Notability, Samsung Notes and OneNote. Matching their muscle memory is free familiarity.

| Gesture | Action | Note |
|---|---|---|
| One finger / stylus | Draw | |
| **Two fingers** | Pan and zoom | Non-negotiable. Must-have. |
| **Two-finger tap** | Undo | Industry convention |
| **Three-finger tap** | Redo | Industry convention |
| Stylus button + draw | Erase | Where hardware supports it (S Pen) |
| Long-press on a stroke | Select | Once lasso exists |
| Double-tap empty canvas | Fit to width | |

> **Important, given the target hardware:** many tablets in this segment ship with no active stylus, so **finger-drawing must be a first-class mode**, and palm rejection cannot assume a stylus is present. Offer an explicit "draw with finger" toggle for users who want two-finger-pan without accidental marks.

### 3.6 Page navigation

Given the paged A4 model and PDF annotation as a core use case:

- **Vertical continuous scroll** between pages, not horizontal page-flip. It matches how students already read PDFs, and it makes "keep writing past the bottom of the page" natural.
- A **page thumbnail drawer** (slide in from the side) for jumping around a long document.
- Page indicator in the document bar ("3 / 12"), tappable to open the drawer.
- **Add page** at the end of the scroll and in the drawer.

### 3.7 First-run on the canvas

A blank white screen with no instructions is intimidating. On the very first canvas open only:

- A faint "Start writing" hint in the centre that fades on first touch
- Two coach marks maximum: the pen slots, and "two fingers to zoom"
- Dismissible, never shown again

Resist a full tutorial. Let them write.

---

## Part 4 — The library (Home)

The bones are good. What's missing is what makes it *usable* once a student has more than two notebooks — which will happen in week one.

### 4.1 Page thumbnails on cards

Cards currently show a solid colour and an emoji. Real note apps show a **thumbnail of the first page**, because that's how people actually recognise their notebooks — by what's written on them, not by a name they half-remember.

Keep the colour as a fallback and as a border/accent, but make the thumbnail the primary visual. This is the single biggest improvement to library scannability.

### 4.2 Continue where you left off

Add a row above the grid:

```
Jump back in
┌──────────────┐  ┌──────────────┐
│ [thumbnail]  │  │ [thumbnail]  │
│ Maths notes  │  │ Physics      │
│ 2 hours ago  │  │ Yesterday    │
└──────────────┘  └──────────────┘
```

Cheap to build, and it removes 2 taps from the single most common journey.

### 4.3 Make the existing controls real

Every one of these is currently a no-op that a user will tap and lose trust over. Either make them work or **remove them until they do** — a control that does nothing is worse than an absent control.

| Control | What it must do |
|---|---|
| **Search** | It is currently a `Text`, not a `TextField`. Make it a real field. Title search first; handwritten-content search once recognition lands. |
| **All / Pinned / Imported Books** | Real filters backed by the repository. |
| **View toggles (✓ / ⊞ / ≡)** | Grid vs list. The ✓ icon is meaningless — replace with recognisable grid/list icons. |
| **Sort ("Most recent")** | Recent / Name / Created / Manual. |
| **Import** | Opens the document picker. |
| **Upgrade** | See 4.6. |

### 4.4 Organisation — students will need it

By the end of a semester a student has 15–30 notebooks. A flat grid fails. Needed by v1.1:

- **Folders** (by subject/semester) or **tags** — pick one, don't build both
- **Recommendation: folders.** Students think in subjects, folders match the mental model of physical notebooks, and they're simpler to build and explain than tags.
- **Multi-select mode** (long-press a card) for bulk move/delete/pin

### 4.5 Empty states

Every list needs one. Currently the library shows only a "Create" tile — functional but cold.

| State | Content |
|---|---|
| **No notebooks (first run)** | Warm illustration, "Your notebooks will live here", primary **Create notebook**, secondary **Import a PDF** |
| **No pinned** | "Pin a notebook to keep it here" + how |
| **No imported** | "Import a PDF to annotate it" + import action |
| **No search results** | "No notebooks match *'xyz'*" + clear-search action |

### 4.6 Move the Upgrade prompt

A permanent dark **Upgrade** pill in the top bar, next to a second pill and an avatar, makes the header noisy and pushes monetisation at people who haven't yet got value from the app.

**Recommendation:** remove it from the top bar. Surface upgrade prompts *contextually* — when a limit is hit, in Settings, and as a single dismissible card in the library after the user has real content. This converts better and annoys less.

### 4.7 Destructive actions: undo, not confirm

Deleting a notebook should **not** open a "Are you sure?" dialog. It should delete immediately and show a snackbar with **Undo** for ~5 seconds.

Faster for the 99% of intentional deletes, and *safer* for the 1% of accidents — a confirmation dialog trains people to tap through it, whereas undo actually recovers the mistake.

---

## Part 5 — Screens that don't exist yet but must

| Screen | Why | Priority |
|---|---|---|
| **Settings** | Currently a 4-item dropdown. Needs a real screen: Account · Appearance (theme, default paper) · **Handedness** · Stylus & gestures · Backup & sync · Language · Storage · About | v1 |
| **Notebook properties** | Rename, cover colour, paper style, page size, export, duplicate | v1 |
| **Page thumbnail drawer** | Navigate long documents | v1 |
| **Search results** | Dedicated, with filters | v1.1 |
| **Import flow** | Pick file → preview → confirm page range → open | v1.1 |
| **Onboarding (2–3 screens max)** | Only if genuinely needed. Prefer in-context coach marks. | v1.1 |
| **Account / profile** | Only once accounts exist | Track F |

> The Settings and Profile *dropdowns* should be reduced to a couple of genuine shortcuts plus "Settings →" that opens the real screen. Menus of 5 grey placeholder squares read as unfinished.

---

## Part 6 — Feedback, states and trust

A note app lives or dies on whether people trust it with their work.

### 6.1 Never show a Save button

Auto-save continuously. Show a quiet "Saved" / "Saving…" indicator in the document bar. A save button implies work can be lost.

### 6.2 Every async thing needs three states

Currently the app has exactly one state per screen: success. Needed everywhere: **loading · empty · error**, each with a recovery action.

### 6.3 Motion and haptics as feedback

- Subtle haptic tick on tool change and page turn
- Snackbar for every completed action that isn't visually obvious
- Sound off by default

### 6.4 Where things currently give no feedback at all

Every menu row (delete, edit, share, pin, lock, and all of Settings and Profile) silently closes the menu. To a user this is indistinguishable from a bug. Until they work, they should be visibly disabled or absent.

---

## Part 7 — Accessibility (currently close to zero)

This is not optional polish; it's also mostly cheap.

| Issue | Current state | Fix |
|---|---|---|
| **Content descriptions** | Almost everything passes `contentDescription = null` or is a bare `Text` in a clickable `Box` | Label every interactive element |
| **Touch target size** | Card menu button **28 dp**, view toggles **36 dp** — both below the 48 dp minimum | Enlarge the touch target (visual size can stay small) |
| **Colour contrast** | `#999999` on white ≈ **2.85:1** and `#AAAAAA` ≈ **2.3:1** — both **fail WCAG AA** (needs 4.5:1). `#666666` ≈ 5.7:1 and `#333333` ≈ 12.6:1 both pass. | Darken placeholder/secondary text to at least `#767676` |
| **Emoji as icons** | 🐦 🔍 ⊞ ≡ ⋮⋮ 🧮 💻 📓 □ ↓ ← — screen readers announce these as their unicode names, which is nonsense in context | Replace with real icons + labels (see doc 13) |
| **Font scaling** | Fixed `dp` layouts will clip when users scale text up | Test at 200% font scale; let containers grow |
| **Dark mode** | Screens hardcode white, so dark mode is effectively broken | See doc 13 |
| **TalkBack order** | Untested | Test the four main flows |

---

## Part 8 — Localisation (a market requirement, not a nicety)

Given the confirmed target market:

1. **Every string is currently hardcoded in Kotlin.** `strings.xml` contains exactly one entry. Nothing can be translated until this is fixed — and the cost grows with every screen added, so start now.
2. **Ship English + Hindi for v1**, with the architecture ready for more Indic languages.
3. **The UI font must support Devanagari** and other Indic scripts — see doc 13. `FontFamily.Default` will fall back inconsistently.
4. **Test layouts with longer strings.** Hindi is often 20–30% longer than English; fixed-width pills and buttons will break.
5. RTL is not needed for Indic scripts (all LTR), so `supportsRtl` can stay as-is without RTL testing effort.

---

## Part 9 — What to fix first (UX priority order)

Ordered by user impact per unit of effort. Items 1–4 are all buildable during Track A of doc 11.

| # | Change | Effort | Impact |
|---|---|---|---|
| 1 | **Canvas toolbar with pen slots** | M | **Transformational** — the app has no tools today |
| 2 | **Fix time-to-ink** (skip splash, persist session, continue-where-you-left-off) | S | Very high — affects every single launch |
| 3 | **Remove the auth wall** / let users in without an account | S | Very high — removes the biggest drop-off point |
| 4 | **Replace emoji with real icons; fix contrast and touch targets** | S | High — this is most of what makes it look unfinished |
| 5 | **Two-finger pan/zoom + gesture conventions** | M | High — currently impossible to navigate a page |
| 6 | **Page thumbnails on library cards** | M | High — library scannability |
| 7 | **Empty states everywhere** | S | Medium-high — first impression |
| 8 | **Make search real; make filters/sort work** | M | Medium — needs the repository first |
| 9 | **Real Settings screen + handedness option** | M | Medium |
| 10 | **Undo-based deletes, auto-save indicator** | S | Medium — trust |
| 11 | **String extraction + Hindi** | M | Market requirement |
| 12 | **Folders / organisation** | L | Grows with usage; v1.1 |

> Items 1, 2, 3, 4, 7 and 10 do **not** depend on the data layer and can start immediately. Items 6, 8, 9 and 12 need the repository from doc 11's Track C.
