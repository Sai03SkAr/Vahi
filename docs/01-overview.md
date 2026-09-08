# 01 — Project Overview

## What Vahi is

Vahi is a **native Android digital notebook application**. It is designed for tablets with a stylus: you create notebooks of different paper styles (blank, ruled, grid), open them fullscreen, and write or draw on them by hand.

Think of the category as: GoodNotes / Notability / Samsung Notes / Xodo — a handwriting-first note app, as opposed to a typed-text note app like Notion or Keep.

The project is currently at the **UI prototype** stage. The entire visual shell exists and is clickable end to end, and freehand ink actually works, but nothing is saved and no server exists.

## The product idea, as expressed by the code

Nothing in this repo is a written spec, so the product intent has to be read out of the UI. Every item below corresponds to something concretely present in the source:

### 1. A notebook library as the home base
`HomeScreen.kt` builds a library dashboard: a grid of notebook cards, each with a title, a creation date, a colour, and a per-card overflow menu. The first tile in the grid is always **"Create new notebook"**. This establishes the core object of the app: the **Notebook**.

### 2. Notebooks have a paper style, chosen at creation
The create flow (`CreateNotebookOverlay`) offers four choices:

| Option | Subtitle in the UI | Implemented? |
|---|---|---|
| **Blank** | "Clean pages for free drawing" | Yes |
| **Ruled** | "Lined pages for writing" | Yes |
| **Grid** | "Grid pages for diagrams" | Yes |
| **Import PDF** | "Annotate existing documents" | No — no-op |

The presence of **Import PDF** signals an intended second content source: bringing in existing documents and annotating them, not just authoring blank pages. The Home screen's `"Imported Books"` filter tab is the other half of that idea.

### 3. Handwriting is the primary input
`ui/components/drawing/` contains a purpose-built ink layer — a `Stroke` model, a `CanvasState` with undo/redo and an eraser, and a `DrawingCanvas` that captures raw pointer events. This is not a text editor with an optional scribble mode; ink is the substrate.

### 4. Tablet-first, landscape, large canvas
- `WindowInfo.kt` defines Material window-size classes, and `AuthScreen` explicitly branches to a *tablet* layout for `Expanded` (≥ 840 dp) and a placeholder for everything else.
- `NotebookScreen.kt` (deleted in Session 4) sketched a tablet workspace: top toolbar, a flexible canvas area, and a fixed **320 dp right-hand panel** commented `// Right panel (AI / research later)`.
- `GridCanvasContent` allocates a 4000 dp × 4000 dp scrollable surface — an "infinite canvas" ambition, not a paper page. ⚠️ **Retired by decision [D1](22-decisions.md#d1--paged-documents-not-infinite-canvas):** the model is paged A4 in points. Do not invest further in this surface.

### 5. A freemium model is anticipated
Two independent surfaces point at paid tiers: an **"Upgrade"** pill in the Home top bar, and **"Upgrade to Plus"** in the profile dropdown.

### 6. Accounts, with social sign-in
`AuthScreen` renders username / e-mail / password / password-confirmation fields plus **Continue with Google** and **Continue with Apple** buttons. The Apple button implies a cross-platform or at least Apple-ID-aware identity story, even though the app is Android-only today.

### 7. AI / research assistance is on the horizon
The only direct evidence was the `// Right panel (AI / research later)` comment in `NotebookScreen.kt` (deleted in Session 4), but it was explicit: a side panel next to the canvas reserved for AI or research features. That intent is now a planned surface — **P3, the AI/study panel** in [`15-ux-blueprint.md`](15-ux-blueprint.md) §2, scheduled for v1.3 once handwriting recognition exists.

## Core vocabulary

These are the nouns the codebase actually uses. Use them consistently in future work.

| Term | Type | Where | Meaning |
|---|---|---|---|
| **Notebook** | `data class Notebook` | `HomeScreen.kt` | A library item: `id`, `title`, `created`, `color`. Currently a display-only model with hardcoded instances. |
| **NotebookType** | `enum` | `NotebookCanvasScreen.kt` | The paper style chosen at creation: `BLANK`, `RULED`, `GRID`. |
| **CanvasType** | `enum` | `CanvasScaffold.kt` | The paper style as consumed by the canvas layer: `BLANK`, `RULED`, `GRID`. A near-duplicate of `NotebookType`, mapped 1:1. |
| **AppDestination** | `enum` | `AppDestination.kt` | The four top-level screens: `SPLASH`, `AUTH`, `HOME`, `NOTEBOOK_CANVAS`. |
| **Stroke** | `data class` | `drawing/Stroke.kt` | One continuous pen mark: `id`, `path`, `color`, `strokeWidth`. |
| **CanvasState** | `class` | `drawing/CanvasState.kt` | Holds all strokes for one canvas session, plus the undo stack, active tool, colour and width. |
| **ToolType** | `enum` | `drawing/CanvasState.kt` | `PEN`, `ERASER`. |
| **WindowClass** | `enum` | `components/WindowInfo.kt` | `Compact` / `Medium` / `Expanded` responsive breakpoints. |
| **CardMenuAnchor** | `data class` | `HomeScreen.kt` | Which notebook card's overflow menu is open, and the screen position to anchor it to. |

## What is deliberately *not* here yet

So that no one goes looking for it:

- No database, no file I/O, no `SharedPreferences`, no network layer. Zero persistence of any kind.
- No dependency injection framework (no Hilt/Koin).
- No navigation library (no `androidx.navigation`); navigation is a hand-rolled `when` over an enum.
- No ViewModels are instantiated anywhere — `BaseViewModel` exists but is unused.
- No repositories, use cases, or domain models — those four packages contain only comment-only `package-info.kt` marker files.
- No meaningful tests — just the two Android Studio template tests.

See [`09-project-status.md`](09-project-status.md) for the full audit.
