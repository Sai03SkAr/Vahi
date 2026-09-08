# 15 — UX Blueprint

> **Status: proposal.** The buildable layer beneath [`12-ux-plan.md`](12-ux-plan.md).
>
> **The split between the three UX documents:**
> - `12-ux-plan.md` — *principles and priorities.* Why time-to-ink matters, why the auth wall must go, what to fix first.
> - `13-visual-design-system.md` — *how it looks.* Colour tokens, type, icons, motion, component visuals.
> - **`15-ux-blueprint.md`** *(this)* — *what exists and how it behaves.* Every screen, every state, every route, every gesture, wireframed.
>
> If you are about to build a screen, this is the document that tells you what to build.
>
> **Figma:** [`vahi-v1`](https://www.figma.com/design/JJAGOm1by1gHE6Nxe4yUhx/vahi-v1?node-id=0-1) is the visual source of truth ([V4](22-decisions.md#v4--figma-is-the-visual-source-of-truth-the-docs-describe-behaviour)). It has **not been read yet** (login-gated), so the wireframes below express *structure, hierarchy and behaviour*, not final visual design. Where Figma specifies a layout, follow Figma; where it is silent — states, dark mode, split-screen, handedness, 200 % font scale — follow this document. Reconciliation is `VAHI-004`.

---

## Part 1 — Six rules that resolve every UI argument

When two designs seem equally good, these break the tie. They are ordered: an earlier rule beats a later one.

1. **Time-to-ink beats everything.** Any change that adds a step between launch and writing must justify itself against this. Nothing has yet.
2. **The paper is the hero.** On the canvas, the user's handwriting is the content. Every pixel of chrome competes with it. Chrome recedes; it does not decorate.
3. **A control that does nothing is worse than an absent control.** Ship it working, ship it visibly disabled with a reason, or do not ship it. Twenty silent no-ops is the fastest way to lose a user's trust, and it is what the app does today.
4. **Never block the writing hand.** Placement follows hand occlusion, not convention. See §7.2.
5. **Undo, not confirm.** Destructive actions execute immediately and offer undo. Confirmation dialogs train people to tap through them; undo actually recovers the mistake.
6. **Frequency decides prominence.** A tool used 200 times a session gets one tap and permanent visibility. A tool used twice a month can live two levels deep.

---

## Part 2 — Screen inventory

Complete list of destinations for v1. **S** = screen (full destination) · **O** = overlay (sheet/dialog/popover) · **P** = panel (persistent region).

| ID | Name | Type | Release | Replaces / status |
|---|---|---|---|---|
| **S0** | Launch (system splash) | S | v1 | Replaces `SplashScreen` ([D11](22-decisions.md#d11--drop-the-video-splash-and-the-media3-dependency)) |
| **S1** | Library | S | v1 | Rewrite of `HomeScreen` |
| **S2** | Editor (canvas) | S | v1 | Rewrite of `CanvasScreen` |
| **S3** | Settings | S | v1 | Replaces the 4-item Settings dropdown |
| **S4** | Settings → sub-pages | S | v1 | New: Appearance · Writing · Language · Storage · About |
| **S5** | Notebook properties | O | v1 | New |
| **S6** | Export | O | v1 | New |
| **S7** | Search results | S | v1 (titles) / v1.2 (content) | Currently a decorative `Text` |
| **S8** | Account | S | v2 | Rewrite of `AuthScreen`, reachable only from Settings |
| **O1** | Create notebook | O | v1 | Rewrite of `CreateNotebookOverlay` |
| **O2** | Notebook card menu | O | v1 | Rewrite of `CardContextMenu` — must anchor properly |
| **O3** | Pen configuration popover | O | v1 | New |
| **O4** | Page thumbnail drawer | P | v1 | New |
| **O5** | Page actions menu | O | v1 | New |
| **O6** | Snackbar host | O | v1 | New — required by rule 5 |
| **O7** | First-run coach marks | O | v1 | New |
| **P1** | Editor document bar | P | v1 | Extends the lone back arrow |
| **P2** | Editor tool rail | P | v1 | Evolution of `DrawingToolbar` (Session 4) |
| **S9** | Import PDF flow | S | v1.1 | New |
| **P3** | AI / study panel | P | v1.3 | The `// Right panel (AI / research later)` intent |

**Deleted from the product:** the Auth screen as a startup gate ([D10](22-decisions.md#d10--no-auth-wall-account-is-optional-and-requested-in-context)); the video splash ([D11](22-decisions.md#d11--drop-the-video-splash-and-the-media3-dependency)); the "Upgrade" pill in the top bar ([D16](22-decisions.md#d16--freemium-with-a-generous-free-tier-no-page-caps)); the meaningless ✓ view toggle.

---

## Part 3 — Navigation graph

```
                        ┌────────────────────┐
      cold start ──────▶│  S0  Launch        │  system splash, no delay
                        └─────────┬──────────┘
                                  │  first launch only: one-time brand animation (skippable)
                                  ▼
   ┌──────────────────────────────────────────────────────────┐
   │  S1  LIBRARY                            ← start dest.    │
   │                                                          │
   │  O1 Create notebook ─┐                                   │
   │  O2 Card menu ───────┼── overlays, no route              │
   │  O6 Snackbar ────────┘                                   │
   └───┬────────┬─────────┬──────────┬────────────────────────┘
       │        │         │          │
       │        │         │          └──▶ S7 Search results ──┐
       │        │         └──▶ S3 Settings ──▶ S4 sub-pages   │
       │        │                       └──▶ S8 Account (v2)  │
       │        └──▶ S5 Notebook properties ──▶ S6 Export      │
       │                                                       │
       ▼                                                       │
   ┌──────────────────────────────────────────┐               │
   │  S2  EDITOR            (notebookId,      │◀──────────────┘
   │                         pageIndex?)      │  open a result
   │                                          │
   │  P1 document bar · P2 tool rail          │
   │  O3 pen config · O4 page drawer          │
   │  O5 page actions · O6 snackbar           │
   │  P3 AI panel (v1.3)                      │
   └──────────────────────────────────────────┘
```

### Route table

Typed routes, `navigation-compose` with type-safe args. No enum, no side-channel state variable.

| Route | Arguments | Deep link (v1.1) | Notes |
|---|---|---|---|
| `library` | `folderId: String?` | `vahi://library` | Start destination |
| `editor` | `notebookId: String`, `pageIndex: Int = 0` | `vahi://notebook/{id}` | Replaces `selectedNotebookType` entirely |
| `search` | `query: String = ""` | — | |
| `settings` | — | — | |
| `settings/{section}` | `section: SettingsSection` | — | |
| `account` | — | — | v2 |
| `import` | `uri: String` | `ACTION_VIEW` on `application/pdf` | v1.1 |

**Three things this fixes by construction:**
- Bug #8 (blank screen on nav desync) — the argument is part of the route, so it cannot be null while the destination is active.
- Bug #1 (rotation) — `SavedStateHandle` restores the back stack and the arguments.
- The missing back stack — Library → Settings → Appearance → back → back lands correctly, with no hand-written `BackHandler`.

### Back-behaviour contract

| At | System back does |
|---|---|
| Library, nothing open | Exits the app |
| Library, overlay open | Closes the overlay (topmost first) |
| Editor, nothing open | **Commits the in-progress stroke**, saves, returns to Library |
| Editor, drawer/popover open | Closes it; stays in the editor |
| Editor, active selection | Clears the selection; stays in the editor |
| Settings sub-page | Up one level |

> The editor's back must **finish and persist the current stroke before navigating**. Today it calls `onBack()` immediately and a stroke in progress is silently discarded.

---

## Part 4 — S1 Library

### 4.1 Wireframe

```
┌───────────────────────────────────────────────────────────────────────────┐
│  [Vahi]                                          [🔍 Search]   [⚙]  (A)  │  top bar
├───────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  Jump back in                                                             │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐                       │
│  │ ▩ thumbnail  │ │ ▩ thumbnail  │ │ ▩ thumbnail  │   ← recents, max 3    │
│  │ Maths notes  │ │ Physics      │ │ Chem lab     │     removes 2 taps    │
│  │ 2 hours ago  │ │ Yesterday    │ │ 3 days ago   │     from J2           │
│  └──────────────┘ └──────────────┘ └──────────────┘                       │
│                                                                           │
│  ─────────────────────────────────────────────────────────────────────    │
│                                                                           │
│  All   Pinned   Imported          [⊞ ≣]   [Recent ▾]      [+ New]  [↥]    │
│                                    view    sort           create  import  │
│                                                                           │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐      │
│  │ ▩            │ │ ▩            │ │ ▩            │ │ ▩            │      │
│  │  thumbnail ⋮ │ │  thumbnail ⋮ │ │  thumbnail ⋮ │ │  thumbnail ⋮ │      │
│  │              │ │              │ │              │ │              │      │
│  ├──────────────┤ ├──────────────┤ ├──────────────┤ ├──────────────┤      │
│  │▌Maths notes  │ │▌Physics      │ │▌Chem lab     │ │▌Python       │      │
│  │ Edited 2h ago│ │ Edited 1d ago│ │ Edited 3d ago│ │ Edited 1w ago│      │
│  └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘      │
│                                                                           │
└───────────────────────────────────────────────────────────────────────────┘
```

### 4.2 What changed from today, and why

| Change | Reason |
|---|---|
| **"Upgrade" pill removed** | Pushes monetisation at users who have not yet got value. [D16](22-decisions.md#d16--freemium-with-a-generous-free-tier-no-page-caps) |
| **"Jump back in" row added** | Removes 2 taps from J2, the second-most-frequent job |
| **Page-1 thumbnails replace colour+emoji** | People recognise notebooks by *what is written in them*. Biggest single win for scannability. |
| **Colour becomes a 3 dp accent bar** | Thumbnails need a neutral background to read against |
| **"Edited 2h ago" replaces "Created 4 jan 2025"** | Recency is what people navigate by |
| **"Create new notebook" moves out of the grid into a `+ New` button** | A grid tile that is not a notebook breaks the grid's meaning and pushes real content down |
| **Search becomes a real `TextField`** | It is currently a `Text`. Users will tap it and nothing happens. |
| **✓ view toggle deleted** | Semantically meaningless; only grid/list remain |
| **Card menu anchors to the card** | Today every menu opens at the right screen edge regardless of which card it belongs to |
| **Menu items get icons and real actions** | Today: five grey squares that silently dismiss |

### 4.3 Card menu (O2)

```
┌─────────────────────────┐
│  ✎  Rename              │
│  ⧉  Duplicate           │
│  📌 Pin to top          │
│  🎨 Change colour       │
│  ↥  Export…             │
│  ⓘ  Properties          │
│ ───────────────────────  │
│  🗑  Delete             │   danger colour
└─────────────────────────┘
```

Must be a real `DropdownMenu` / `Popup` with a `PopupPositionProvider` anchored to the ⋮ button — not a hand-computed padding from a captured offset. **Long-press a card** enters multi-select (v1.1).

> `share` and `lock` are dropped from v1. Share is covered by Export → system share sheet. Lock implies encryption and a credential story; a fake lock is worse than no lock.

### 4.4 Empty and error states

| Condition | Presentation |
|---|---|
| **No notebooks (first run)** | Centred, ~40 % down: large muted notebook glyph · "Your notebooks will live here" · body: "Create one and start writing — nothing needs setting up." · primary **Create notebook** · secondary **Import a PDF** (v1.1) |
| **No pinned** | "Nothing pinned yet" · "Pin a notebook from its ⋮ menu to keep it here." |
| **No imported** | "No imported documents" · **Import a PDF** action |
| **No search results** | "No notebooks match *'oscillations'*" · **Clear search** · (v1.2) "Searching handwriting too" hint |
| **Loading** | Skeleton cards in the grid — never a blank screen, never a spinner over an empty page |
| **Storage read error** | "Couldn't open your library" · **Retry** · **Report a problem** — and never a silent empty grid, which is indistinguishable from "you have no notes" and is terrifying in a notes app |

### 4.5 Create notebook (O1)

Today: four grey squares with glyphs `□ ≡ ⊞ ↓` and a string-literal dispatch. Replace with **rendered previews of the actual paper**, because paper style is the only decision being made here.

```
┌──────────────────────────────────────────────────────────────┐
│   New notebook                                          ✕    │
│                                                              │
│   Name  [ Untitled notebook                              ]   │   ← autofocus,
│                                                              │      preselected
│   Paper                                                      │
│   ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐             │
│   │        │  │ ────── │  │· · · · │  │   PDF  │             │
│   │        │  │ ────── │  │· · · · │  │   ↥    │             │
│   │        │  │ ────── │  │· · · · │  │        │             │
│   └────────┘  └────────┘  └────────┘  └────────┘             │
│     Blank       Ruled        Grid       Import   (v1.1)      │
│                                                              │
│   Colour   ● ● ● ● ● ●                                       │
│                                                              │
│                              [ Cancel ]  [ Create & write ]  │
└──────────────────────────────────────────────────────────────┘
```

- **Enter creates.** Escape cancels. Both already partially work — keep them.
- **Defaults are pre-filled** so a user who taps `+ New` then Enter is writing in two taps.
- Dispatch on a **typed enum**, not on the strings `"Blank"`/`"Ruled"`/`"Grid"`.
- Remember the last-used paper style and preselect it.

---

## Part 5 — S2 Editor

The most important screen in the product. Full interaction and rendering behaviour is in [`16-canvas-spec.md`](16-canvas-spec.md); this is the layout and chrome.

### 5.1 Wireframe (right-handed default)

```
┌───────────────────────────────────────────────────────────────────────────┐
│ ←   Maths notes ▾                    3 / 12        ↶  ↷   Saved ✓     ⋯   │ P1 document bar
├─────┬─────────────────────────────────────────────────────────────────────┤
│ ✒ ● │                                                                     │
│ ✒   │        ┌───────────────────────────────────────────┐                │
│ ✒   │        │                                           │                │
│ ▨   │        │                                           │                │
│ ⌫   │        │              P A G E   3                  │                │
│ ⬚   │        │              (A4 portrait)                │                │
│ ✋  │        │                                           │                │
│ ─── │        │                                           │                │
│ ⊞   │        └───────────────────────────────────────────┘                │
│     │        ─ ─ ─ ─ ─ ─ page break ─ ─ ─ ─ ─ ─ ─ ─ ─ ─                   │
│  P2 │        ┌───────────────────────────────────────────┐                │
│     │        │              P A G E   4                  │                │
└─────┴────────┴───────────────────────────────────────────┴────────────────┘
      ▲                                                              ▲
      tool rail — floats, auto-hides while writing        palm zone: keep clear
```

### 5.2 P1 — Document bar

Rare, document-level actions. Distance from the hand is fine here.

| Element | Behaviour |
|---|---|
| **←** Back | Commits the stroke, saves, returns to Library |
| **Title ▾** | Tap → inline rename. Chevron → notebook properties (S5) |
| **n / N** | Page indicator. Tap → opens the page drawer (O4) |
| **↶ ↷** | Undo / redo. Disabled states must reflect reality. Also on gestures (§5.5) |
| **Saved ✓ / Saving…** | Quiet auto-save indicator. **There is never a Save button.** |
| **⋯** | Overflow: Export · Page setup · Search in notebook (v1.2) · Settings |

Auto-hides with the tool rail while writing. Reappears on lift or tap.

### 5.3 P2 — Tool rail

Evolution of the shipped `DrawingToolbar`. Same left-edge placement — that part was right.

```
┌─────┐
│ ✒ ● │  Pen slot 1  ← selected: accent ring + the slot's own ink colour
│ ✒   │  Pen slot 2
│ ✒   │  Pen slot 3
│ ▨   │  Highlighter
│ ⌫   │  Eraser
│ ⬚   │  Lasso        (v1.1)
│ ✋  │  Hand / pan   (finger-draw users toggle this; see 16-canvas-spec §4.4)
├─────┤
│ ⊞   │  Pages — opens the thumbnail drawer
└─────┘
```

**Rules**
- Width 56 dp; each item a **true 48 dp touch target**. (The shipped toolbar applies `.padding(4.dp)` *inside* `.size(48.dp)` before `.clickable`, yielding a 40 dp target — see [`09-project-status.md`](09-project-status.md) bug #15.)
- Floats 16 dp from the edge, vertically centred, over the page.
- **Tap a slot** → select. **Tap the already-selected slot** → open its config popover (O3). One tap for the common case; two for the rare one.
- Slots persist across sessions, per user, not per notebook.
- Fades to 20 % opacity ~150 ms after a stroke starts; restores ~400 ms after the last lift, or instantly on tap.
- **Mirrors to the right edge** when handedness = left.
- Every item has a `contentDescription` and a long-press tooltip.

### 5.4 O3 — Pen configuration popover

Opens beside the slot, never covering it.

```
┌────────────────────────────────────┐
│   ╭──────────────────────────╮     │  live preview stroke, drawn with
│   │  ~~~~~~~~~~~~~~~~~~~~~~  │     │  the current settings
│   ╰──────────────────────────╯     │
│                                    │
│   Colour                           │
│   ● ● ● ● ● ● ● ● ● ●   [＋]      │  10 presets + custom picker
│                                    │
│   Width                            │
│   ·   ‧   •   ●                    │  4 presets shown as actual strokes,
│                                    │  never as numbers
│   Type                             │
│   ( Ballpoint )  Fountain  Pencil  │  once brushes exist
└────────────────────────────────────┘
```

### 5.5 Gestures

Adopt the conventions; do not invent. Users arrive with muscle memory from GoodNotes, Notability and Samsung Notes, and matching it is free familiarity. Conflict-resolution rules are in [`16-canvas-spec.md`](16-canvas-spec.md) §5.

| Gesture | Action | Priority |
|---|---|---|
| One stylus / one finger (draw mode) | Draw | v1 |
| **Two fingers** | Pan **and** pinch-zoom | v1 — non-negotiable |
| **Two-finger tap** | Undo | v1 — industry convention |
| **Three-finger tap** | Redo | v1 |
| Stylus barrel button + drag | Erase | v1 where hardware reports it |
| Double-tap empty canvas | Fit page to width | v1 |
| Long-press a stroke | Select it | v1.1 (needs lasso) |
| Two-finger swipe from right edge | Open page drawer | v1.1 |

### 5.6 O4 — Page thumbnail drawer

Slides in from the trailing edge, 280 dp, over the canvas, dismissed by tap-outside or swipe.

```
┌──────────────┐
│  Pages   12  │
│ ┌──────────┐ │
│ │  ▩   1   │ │
│ ├──────────┤ │
│ │  ▩   2   │ │
│ ├──────────┤ │
│ │ ▩▩▩  3 ●│ │  ← current page, accent border
│ ├──────────┤ │
│ │  ▩   4   │ │
│ └──────────┘ │
│  [ + Add ]   │
└──────────────┘
```

Tap → jump. Long-press → reorder by drag. ⋮ per thumbnail → Insert before/after · Duplicate · Delete (with undo).

### 5.7 First run on the canvas (O7)

A blank white screen with no instructions is intimidating. On the **very first** canvas open only:

- A faint centred "Start writing" that fades on first touch.
- **Two** coach marks, maximum: the pen slots, and "two fingers to zoom".
- Dismissible; never shown again; a "Show tips again" entry in Settings → About.

Resist a full tutorial. Let them write.

### 5.8 Editor states

| Condition | Presentation |
|---|---|
| Loading a notebook | Page frame with a skeleton fill; tool rail disabled ~150 ms. Under that, show nothing rather than a flash. |
| Empty page | Just paper. No placeholder text after first run. |
| Saving | "Saving…" in the document bar. Never a modal, never a blocker. |
| Save failed | Persistent (non-dismissible) amber bar: "Couldn't save — your work is safe on this device. Retry". This is the one place an error must not auto-dismiss. |
| Storage full | Blocking dialog before writing continues: "Your tablet is out of space. Free up space or export and delete an old notebook." with an action to Storage settings. |
| Notebook missing / corrupt | "This notebook couldn't be opened." · **Back to library** · **Report a problem**. Never a blank canvas. |

---

## Part 6 — S3/S4 Settings, and S8 Account

Today Settings is a four-row dropdown of grey placeholder squares. It becomes a real screen — a list on the left, detail on the right (tablet two-pane).

| Section | Contents |
|---|---|
| **Appearance** | App theme (Light/Dark/System) · Paper theme (White/Cream/Dark) · Default paper style · Default page size |
| **Writing** | **Handedness (left/right)** · Draw with finger (on/off) · Palm rejection · Two-finger-tap undo · Pressure sensitivity · Default pen slots |
| **Language** | App language (English/हिन्दी) · Handwriting recognition languages + downloads (v1.2) |
| **Storage** | Space used, by notebook · Export all · Clear cache |
| **Account** | Not signed in → "Vahi works fully offline. Sign in to sync." (v2) |
| **Vahi Plus** | The single upgrade surface |
| **About** | Version · what's new · privacy policy · licences · show tips again · report a problem |

**S8 Account**, when it arrives, is reached only from here — never on startup ([D10](22-decisions.md#d10--no-auth-wall-account-is-optional-and-requested-in-context)) — and fixes the current screen's specific faults: separate sign-in from sign-up, Google first, drop Apple, inline validation as the user types, real loading and error states, and a visible "Continue without an account".

---

## Part 7 — Cross-cutting behaviour

### 7.1 Rotation, resize and multi-window

Android tablets are used in split-screen and free-form windows constantly, and **every configuration change is currently catastrophic** — rotating returns to Splash and drops all ink.

| Situation | Required behaviour |
|---|---|
| Rotate anywhere | State survives, position survives, in-progress stroke survives |
| Rotate **mid-stroke** | The stroke completes and persists. Do not drop it, do not corrupt it. |
| Split-screen (narrow) | Editor: tool rail collapses to a compact strip; document bar keeps back/undo/redo only. Library: 2-column grid. **Do not show the "designed for tablets" wall** — the device is a tablet; the window is just small. |
| Free-form resize | Re-layout without losing zoom or scroll position |
| Dark-mode toggle | Theme swaps; nothing else changes |

**Do not fix this with `android:configChanges`.** Fix it with ViewModels and `SavedStateHandle`, so process death is covered too. `configChanges` hides the symptom and leaves the app broken after a low-memory kill — the case that actually loses a student's notes.

### 7.2 Handedness and hand occlusion

```
Right-handed, landscape:                Left-handed, landscape:
┌──────────────────────────┐            ┌──────────────────────────┐
│                          │            │                          │
│ [rail]      canvas       │            │       canvas      [rail] │
│                          │            │                          │
│              ░░░░░░░░░░░ │            │ ░░░░░░░░░░░              │
│              ░ palm  ░░░ │            │ ░░░ palm  ░              │
└──────────────────────────┘            └──────────────────────────┘
```

The rail lives on the **opposite side from the writing hand**. Default right-handed; setting in Settings → Writing; **prompt once** on first canvas open ("Which hand do you write with?") because getting this wrong makes the app feel broken and users rarely go hunting in settings.

### 7.3 Keyboard and stylus shortcuts

Many Android tablets are used with a keyboard case. Supporting shortcuts is cheap and makes the app feel serious.

| Key | Action | Screen |
|---|---|---|
| `Ctrl+N` | New notebook | Library |
| `Ctrl+F` | Focus search | Library / Editor |
| `Ctrl+Z` / `Ctrl+Shift+Z` | Undo / redo | Editor |
| `Ctrl+E` | Export | Editor |
| `1`–`5` | Select tool rail slot | Editor |
| `[` / `]` | Previous / next page | Editor |
| `Space` (hold) | Temporary pan mode | Editor |
| `Esc` | Close topmost overlay | Everywhere |
| `Enter` | Confirm primary action | Any dialog |

Stylus hardware: honour the barrel button (erase) and, where reported, the eraser tip.

### 7.4 Feedback and trust

- **Snackbar with Undo for every destructive action** — delete notebook, delete page, clear page — 5 s, bottom-centre. This replaces confirmation dialogs entirely (rule 5).
- **`clearAll()` must become undoable before it ships to users.** The shipped toolbar has a one-tap Clear that wipes both the stroke list *and* the redo stack, sitting immediately below Undo. See [`09-project-status.md`](09-project-status.md) bug #13.
- Subtle haptic tick on tool change and page turn. Sound off by default.
- Every async operation has **loading / empty / error** with a recovery action. The app currently has exactly one state per screen: success.

### 7.5 Motion

| Transition | Treatment | Priority |
|---|---|---|
| Notebook card → editor | **Shared-element**: the card expands into the page | High — the signature moment, and very achievable in Compose |
| Launch → library | Cross-fade | High |
| Overlay open/close | Scale + fade from the invoking element | Medium |
| Tool rail auto-hide | Fade to 20 % over ~150 ms | Medium |
| Page turn | Slide with slight overshoot | Medium |
| Snackbar | Slide up | Low |

150–200 ms for small elements; 300–400 ms for screen transitions; Material 3 emphasised easing. **Respect the system "remove animations" setting** — this is an accessibility requirement, not a nicety.

### 7.6 Microcopy

| Principle | Instead of | Write |
|---|---|---|
| Say what happened, not what failed | "Error 500" | "Couldn't save. Your work is safe on this device." |
| Name the object | "Item deleted" | "'Maths notes' deleted" |
| Offer the next step | "No results" | "No notebooks match 'oscillations'. Clear search" |
| Never blame the user | "Invalid input" | "That name's already taken — try another" |
| Plain words | "Authenticate", "Sync conflict detected" | "Sign in", "This note changed on two devices" |
| Reassure about data | *(silence)* | "Your handwriting never leaves your tablet" |

Every string goes in `strings.xml` from the first line written. `strings.xml` currently has **one** entry and all UI copy is hardcoded Kotlin — the cost of fixing that grows with every screen added, so it must be a rule now, not a cleanup later. Hindi runs 20–30 % longer than English: **no fixed-width pills or buttons.**

---

## Part 8 — Build order for the UI

Ordered by user impact per unit of effort. Items marked ⚡ do not depend on the data layer and can be built at any time.

| # | Work | Effort | Impact | Depends on |
|---|---|---|---|---|
| 1 | Tool rail: real 48 dp targets, content descriptions, semantic colours, **undoable clear** | S ⚡ | Fixes a data-loss bug and an a11y failure | — |
| 2 | Kill the auth wall + splash video; land in the library | S ⚡ | Highest — affects every launch | — |
| 3 | Navigation library + typed routes | M | Kills rotation loss and blank-screen desync | — |
| 4 | Two-finger pan/zoom + gesture set | M | High — the page is currently unnavigable | Coordinate model |
| 5 | Real icons; contrast and touch-target fixes | S ⚡ | High — most of what makes it look unfinished | — |
| 6 | Library rewrite: thumbnails, recents, real controls | L | High | Repository |
| 7 | Pen slots + config popover | M ⚡ | High — the constant interaction | — |
| 8 | Empty / loading / error states everywhere | M ⚡ | Trust | Partly repository |
| 9 | Page drawer + page management | M | Needed once notebooks are real | Pages |
| 10 | Settings screen + handedness | M | Medium | Preferences |
| 11 | Snackbar undo for all destructive actions | S | Trust | Repository |
| 12 | String extraction + Hindi | M ⚡ | Market requirement | — |
| 13 | Motion + shared-element transition | S | Polish that reads as quality | Screens stable |
| 14 | Folders + multi-select | L | Grows with usage | Repository (v1.1) |

Item-level tracking is in [`21-backlog.md`](21-backlog.md).
