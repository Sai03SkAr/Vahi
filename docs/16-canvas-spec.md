# 16 — Canvas Specification

> **Status: proposal.** The engineering specification for the editor surface — the most product-critical code in the project.
>
> Describes what **must be built**. What exists today is in [`05-drawing-engine.md`](05-drawing-engine.md).
>
> Depends on: [D1](22-decisions.md#d1--paged-documents-not-infinite-canvas) (paged A4 in points) · [D3](22-decisions.md#d3--product-thesis-student-notebook-android-tablets-india) (no stylus assumed) · [D6](22-decisions.md#d6--ink-architecture-adopt-androidxink) (`androidx.ink`) · [V1](22-decisions.md#v1--app-theme-and-paper-theme-are-separate-settings) (paper theme).

---

## Part 1 — The four things that must be true

Everything in this document serves one of these. If a change does not, it is not needed.

1. **Writing feels instant.** Latency is the product. No feature list survives laggy ink.
2. **A resting palm never draws.** On a tablet this is not an edge case — it is what happens on literally every stroke of real handwriting.
3. **Ink is data, not pixels.** Geometry that can be hit-tested, erased partially, selected, serialised, exported at any resolution, and fed to a recogniser.
4. **Nothing is ever lost.** Not on rotation, not on process death, not on a crash mid-stroke.

**Today the app satisfies none of them.** That is the honest baseline this spec is written against.

---

## Part 2 — Coordinate spaces

**This is the foundation most likely to be overlooked and the most expensive to retrofit.** Strokes today are stored in *screen pixels of whatever device drew them*, with no document space and no view transform. Persisting that produces permanently ambiguous data — you cannot know afterwards what device or zoom level produced it.

> **Hard rule: the coordinate model lands before any persistence. No exceptions, under any schedule pressure.**

### 2.1 The three spaces

| Space | Unit | Origin | Used for |
|---|---|---|---|
| **Screen** | Android px | Top-left of the viewport | Raw pointer input, rendering output |
| **Document** | **points (1/72 in)** | Top-left of page 1 | The scroll model — where pages sit in the continuous strip |
| **Page** | **points** | Top-left of *that page* | **The only space anything is ever stored in** |

Every stroke is stored in **page space**, on exactly one page. A stroke never spans pages.

### 2.2 The transforms

```
                    viewTransform                  pageOffset(i)
  screen px  ◀──────────────────────  document pt  ◀────────────  page pt
             ──────────────────────▶               ────────────▶
                    inverse                          translate
```

```kotlin
/** Scale + translate. Rotation is out of scope for v1 but the type allows it later. */
data class ViewTransform(
    val scale: Float = 1f,        // clamped to [minScale, maxScale]
    val offset: Offset = Offset.Zero  // document-space translation, in points
)

fun ViewTransform.documentToScreen(p: Offset, density: Float): Offset =
    Offset((p.x + offset.x) * scale * density, (p.y + offset.y) * scale * density)

fun ViewTransform.screenToDocument(p: Offset, density: Float): Offset =
    Offset(p.x / (scale * density) - offset.x, p.y / (scale * density) - offset.y)
```

**Rules**
- Input is converted **screen → document → page on the way in**, before it is stored. Never store screen coordinates, not even temporarily.
- Rendering applies the transform **on the way out**.
- `1 pt = 1/72 in`. Convert with device density; never hardcode a px-per-point ratio.
- Zoom limits: `minScale` = fit-page-to-viewport, `maxScale = 8.0`. Never allow a zoom that makes a page smaller than 30 % of the viewport width — users get lost.
- **Pinch zooms about the gesture centroid**, not the viewport centre. Anything else feels wrong immediately.

### 2.3 The page strip (document space)

Pages stack vertically with a gap. Per [`15-ux-blueprint.md`](15-ux-blueprint.md) §5.1, scroll is **vertical continuous** — matching how students already read PDFs, and making "keep writing past the bottom" natural.

```
pageOffset(i).y = Σ(height[j] for j < i) + PAGE_GAP_PT * i
pageOffset(i).x = (maxPageWidth - width[i]) / 2      // centre unequal page widths
PAGE_GAP_PT = 24f
```

Page widths differ once PDFs are imported ([D1](22-decisions.md#d1--paged-documents-not-infinite-canvas): dimensions are stored per page), so never assume a uniform page rectangle.

### 2.4 What this unblocks

Every one of these is blocked today, and blocked hard:

| Feature | Why it is blocked without this |
|---|---|
| Zoom and pan | There is no transform to change |
| Grid canvas scrolling (bug #2) | Same |
| Export to PDF/PNG at any resolution | No device-independent geometry to rasterise |
| Opening a note on a different-sized screen | Ink renders at the wrong place and scale |
| PDF annotation | Annotations must anchor to *PDF page* coordinates |
| Viewport culling (bug #10) | Nothing to cull against |
| Sync across devices | The same problem, multiplied |

---

## Part 3 — Ink model

### 3.1 With `androidx.ink` (the expected path — [D6](22-decisions.md#d6--ink-architecture-adopt-androidxink))

Map onto the library's own types rather than wrapping them in a parallel model:

| Vahi concept | `androidx.ink` type |
|---|---|
| In-progress stroke | `InProgressStroke` via the `InProgressStrokes` composable (`ink-authoring-compose`) |
| Finished stroke | `Stroke` (immutable) |
| Raw input | `StrokeInputBatch` — ordered points **with timestamps, pressure, tilt, orientation** |
| Brush | `Brush` (family, size, colour, epsilon) |
| Hit-testing / erase / lasso | `ink-geometry` intersection queries |
| Serialisation | `ink-storage` |

**Keep `StrokeInputBatch` for the life of the stroke.** It is what ML Kit Digital Ink consumes ([D9](22-decisions.md#d9--handwriting-recognition-uses-ml-kit-digital-ink-on-device)) and what allows re-rendering a stroke with a different brush later. Discarding raw input to save bytes is the mistake the current implementation makes and cannot be undone.

**Interface boundary — non-negotiable.** All library types stay behind a Vahi-owned interface:

```kotlin
interface InkEngine {
    fun beginStroke(pageId: PageId, input: PointerSample, brush: BrushSpec): StrokeHandle
    fun extendStroke(handle: StrokeHandle, inputs: List<PointerSample>)
    fun finishStroke(handle: StrokeHandle): FinishedStroke
    fun cancelStroke(handle: StrokeHandle)
    fun hitTest(pageId: PageId, at: PagePoint, radiusPt: Float): List<StrokeId>
    fun intersecting(pageId: PageId, region: PageRegion): List<StrokeId>
    fun serialise(stroke: FinishedStroke): ByteArray
    fun deserialise(bytes: ByteArray, formatVersion: Int): FinishedStroke
}
```

This costs about a day and it is what makes the fallback in §8 a swap rather than a rewrite. Given `androidx.ink` is a young stable API (1.0.0 shipped 2025-12-17), that insurance is cheap.

### 3.2 Brushes for v1

| Brush | Behaviour |
|---|---|
| **Ballpoint** | Constant width unless pressure exists; slight velocity taper |
| **Fountain** | Pressure- and velocity-responsive width |
| **Pencil** | Textured, tilt-responsive where tilt exists |
| **Highlighter** | Wide, translucent (α ≈ 0.35), **flat caps**, renders **behind** ink, and multiple passes over the same spot must **not** darken |

**The highlighter's z-order and non-accumulating alpha are a real design constraint, not a detail.** Students highlight constantly, and a highlighter that darkens on overlap or covers the text is unusable. Implement as a separate render layer composited under ink, drawn into an offscreen layer per stroke so alpha does not compound.

### 3.3 Graceful degradation — the target-hardware requirement

Per [D3](22-decisions.md#d3--product-thesis-student-notebook-android-tablets-india), a pressure-sensitive stylus **cannot be assumed**. Many tablets in this segment ship with no active stylus, or a capacitive one with no pressure data, and plenty of users will draw with a finger.

| Signal | If present | If absent |
|---|---|---|
| Pressure | Modulate width | **Constant width**, with a subtle velocity taper so strokes still look alive |
| Tilt | Pencil shading | Ignore |
| Stylus vs finger | Palm rejection (§4) | Heuristic rejection (§4.3) |
| Barrel button | Temporary eraser | Not offered |

**Design for the finger case and treat stylus data as an enhancement.** This is the opposite of the instinct you get from copying iPad app design, and getting it backwards would make the app feel broken to most of its intended users.

---

## Part 4 — Pointer arbitration and palm rejection

**This is the highest-severity gap in the current implementation** and the one most likely to make a real user abandon the app in the first minute.

### 4.1 What is broken today

`DrawingCanvas` does:

```kotlin
val change = event.changes.firstOrNull() ?: continue
```

It takes the **first pointer in the list** and ignores every other. Consequences on a real tablet:

- **The palm usually lands before the stylus tip.** When it does, the palm is pointer 1 → **you draw with your palm and the stylus does nothing.**
- When the primary pointer lifts, the next event's first change is a *different* pointer that is already pressed → since `currentStroke` was nulled by `finishStroke()`, a **new stroke starts under the palm**.
- There is no gesture-cancel path, so `cancelStroke()` is never called and an interrupted stroke is left dangling.
- Because pen events are consumed while pressed, **nothing underneath can ever receive a gesture** — which is why the grid canvas cannot scroll.

### 4.2 The required state machine

One canvas-wide state, not per-pointer flags. Everything below is a transition in it.

```
                 ┌──────────────────────────────────────────────┐
                 │                   IDLE                       │
                 └───┬──────────────┬──────────────┬────────────┘
    stylus down ─────┘              │              └───── 2 fingers down
    or finger down                  │ 1 finger down
    (draw mode)                     │ (pan mode / hand tool)
         ▼                          ▼                    ▼
    ┌──────────┐              ┌──────────┐        ┌──────────────┐
    │ DRAWING  │              │ PANNING  │        │ TRANSFORMING │
    │          │              │          │        │ (pan + zoom) │
    └────┬─────┘              └────┬─────┘        └──────┬───────┘
         │                         │                     │
         │ 2nd pointer arrives ────┼─────────────────────┘
         │   within GRACE_MS  ─────▶ cancel stroke, → TRANSFORMING
         │ 2nd pointer arrives after GRACE_MS ─▶ ignore it, keep drawing
         │ stylus down while finger drawing ──▶ cancel finger stroke, → DRAWING (stylus)
         │ up / cancel ───────────────────────▶ commit, → IDLE
         ▼
    ┌──────────────────────────────────────────────────────────┐
    │  every transition out of DRAWING either COMMITS or        │
    │  CANCELS the stroke. There is no third path.              │
    └──────────────────────────────────────────────────────────┘
```

`GRACE_MS = 120`. A second finger arriving within the grace window means the user meant to zoom and the first touch was the start of that gesture — cancel the stroke and remove the mark. Arriving later means they are already writing and a palm or stray finger has landed — ignore it.

### 4.3 Palm rejection, tiered

Tiered because the hardware is not uniform. Apply the first tier that is available.

**Tier 1 — a stylus is present (best).**
Once any `PointerType.Stylus` event is seen in this session, **only stylus pointers draw.** Finger touches become pan/zoom. Note that `pointerType` is available today and completely unused.

**Tier 2 — `TOOL_TYPE_PALM` reported (API 29+).**
Reject those pointers outright and cancel any stroke they started.

**Tier 3 — no stylus, finger drawing (heuristic).**
Reject a pointer if any of:
- **Contact size** exceeds `PALM_SIZE_THRESHOLD` (`getTouchMajor()` — a palm is much larger than a fingertip). Calibrate per device class; do not hardcode one number.
- It is the **later of two simultaneous down events** and is significantly larger.
- It is **stationary for > 400 ms while another pointer is moving** — the classic resting-palm signature.

**Tier 4 — user override.**
Settings → Writing → "Draw with finger". When off, finger touches only pan and zoom, and the tool rail shows a hand icon. This is the escape hatch for anyone the heuristics fail, and it must exist because they will fail for someone.

### 4.4 Stroke cancellation is mandatory

Every one of these **must** call `cancelStroke()` and leave no mark:

| Trigger | Why |
|---|---|
| Second pointer within `GRACE_MS` | It was a zoom, not a stroke |
| `PointerEventType.Exit` / gesture cancellation | System took the gesture |
| Palm detected retroactively | Tier 2/3 fired after the stroke began |
| Composable disposal mid-stroke | Do not leak a half-stroke into the document |
| Configuration change mid-stroke | **Commit** rather than cancel — the user's mark is real ([`15-ux-blueprint.md`](15-ux-blueprint.md) §7.1) |

`CanvasState.cancelStroke()` exists today and has **no caller anywhere**. That is precisely the bug.

---

## Part 5 — Gesture arbitration

Given the state machine, the conventional gesture set from [`15-ux-blueprint.md`](15-ux-blueprint.md) §5.5 resolves cleanly:

| Input | DRAWING | PANNING | TRANSFORMING | IDLE |
|---|---|---|---|---|
| 1 stylus | continue stroke | cancel pan → draw | ignore | begin stroke |
| 1 finger (draw mode, no stylus seen) | continue stroke | continue pan | ignore | begin stroke |
| 1 finger (stylus seen, or finger-draw off) | ignore | continue pan | ignore | begin pan |
| 2nd finger < 120 ms | **cancel stroke** → transform | → transform | — | → transform |
| 2nd finger > 120 ms | ignore (palm) | → transform | — | → transform |
| 3rd+ finger | ignore | ignore | ignore | ignore |
| 2-finger tap (< 200 ms, < 20 px travel) | — | — | — | **undo** |
| 3-finger tap | — | — | — | **redo** |
| Double-tap | — | — | — | fit page to width |
| Barrel button + move | erase | erase | — | erase |

**Rules**
- Taps are only recognised from IDLE, so they can never eat a drawing gesture.
- Two- and three-finger taps must be distinguished by *maximum simultaneous pointer count*, not by the count at lift-off.
- The canvas consumes only the events it actually uses. Page scrolling is driven by the same transform, so there is no competing scroll container to starve — which is what breaks the grid canvas today.

---

## Part 6 — Rendering

### 6.1 The pipeline

```
┌────────────────────────────────────────────────────────────┐
│  L4  Chrome — tool rail, document bar, drawer               │  Compose
├────────────────────────────────────────────────────────────┤
│  L3  Live stroke — front-buffered, low-latency              │  ink-authoring
├────────────────────────────────────────────────────────────┤
│  L2  Settled ink — cached per page, invalidated on change   │  GraphicsLayer / bitmap
├────────────────────────────────────────────────────────────┤
│  L1  Highlighter — separate layer, composited under ink     │
├────────────────────────────────────────────────────────────┤
│  L0  Paper — blank / rules / dots / PDF page bitmap         │  cached per (page, zoom)
└────────────────────────────────────────────────────────────┘
```

**The critical property: L3 never waits for L0–L2.** That separation is what low latency *is*. Redrawing every settled stroke every frame — what the app does today — makes frame cost grow linearly with stroke count and will drop frames on a full page.

### 6.2 Viewport culling — required, not an optimisation

Per [D3](22-decisions.md#d3--product-thesis-student-notebook-android-tablets-india), the target hardware is a budget tablet. On an iPad-class GPU you would get away with brute force for a long time. Here you will not.

- Only pages intersecting the viewport (plus one page of prefetch each way) are composed at all.
- Within a page, only strokes whose **bounding box** intersects the visible rect are drawn. The bounding box is stored as indexed columns ([`17-data-spec.md`](17-data-spec.md) §3) so this is a query, not a scan.
- Paper patterns are generated for the **visible rect only**. `GridCanvasContent` today issues tens of thousands of `drawCircle` calls across a 4000 dp surface with no culling — do not carry that pattern forward.

### 6.3 Cache invalidation

| Event | Invalidate |
|---|---|
| Stroke committed | That page's L2 tile only |
| Stroke erased | That page's L2 tile only |
| Undo / redo | Affected pages only |
| Zoom changed > 1.5× since the cache was rendered | Re-render at the new scale |
| Paper theme changed | All L0 and L2 (ink colours remap) |
| Page inserted / deleted | Recompute the page strip; L2 tiles survive |

### 6.4 Memory budget

Rasterised caches are the biggest memory risk on a 2 GB tablet. Cap the L2 cache at **N visible pages + 2**, evict LRU, and re-render on demand. Never hold a cache for every page in a 200-page notebook. Numbers in [`19-quality-bar.md`](19-quality-bar.md) §2.3.

---

## Part 7 — Paper themes and ink colour ([V1](22-decisions.md#v1--app-theme-and-paper-theme-are-separate-settings))

| Paper theme | Background | Ink presentation | Rules / dots |
|---|---|---|---|
| White | `#FFFFFF` | As stored | `rule` / `gridDot` |
| Cream | `#FBF7EF` | As stored | Warmed rule colour |
| Dark | `#1A1A1A` | **Display-time inversion** | Dark-mode rule tokens |

**Dark-paper inversion is a presentation mapping, never a data mutation.**

```kotlin
fun displayColor(stored: Color, paper: PaperTheme): Color = when (paper) {
    PaperTheme.DARK -> if (stored.luminance() < 0.35f) stored.lightenForDarkPaper() else stored
    else -> stored
}
```

Near-black ink becomes near-white on dark paper; a red highlight stays red. **Export and print always use the stored colours on white** — the document stays canonical, so what a student prints matches what they wrote.

---

## Part 8 — The fallback, if the benchmark rejects `androidx.ink`

Keep the Compose `Canvas`, but fix the model properly. This is a real fallback, not a formality — but it recovers most of the *quality* and none of the *latency* win.

1. **Own the stroke representation.** A packed `FloatArray` of `(x, y, pressure, tMs)` in page points. Never a `Path` as the source of truth — a `Path` is write-only geometry: you cannot hit-test it accurately, partially erase it, lasso-select it, or serialise it.
2. **Consume `change.historical`.** Android batches MotionEvents between frames; the current loop reads `change.position` only and throws the rest away, which is why fast strokes look faceted.
3. **Smooth with Catmull-Rom → cubic Bézier** between samples.
4. **Modulate width by pressure** where present, with the §3.3 fallback.
5. **Cache settled strokes into a `GraphicsLayer`**; live-render only the active stroke.
6. **Geometry-accurate erase** via `PathMeasure` sampling and point-distance testing, replacing the bounding-box test.
7. If latency is still unacceptable, the honest next step is `androidx.graphics:graphics-core` (front-buffered rendering) plus `androidx.input:input-motionprediction` — **at which point you have rebuilt the Ink API, worse.** That is the argument for adopting it.

---

## Part 9 — Command model (F5)

One user action = one undo step. The current design pushes erased strokes onto the same list as undone strokes, so `redo()` un-erases them; and `finishStroke()` clears that stack, so drawing after erasing silently makes the erase permanent.

```kotlin
sealed interface EditCommand {
    fun applyTo(page: PageState)
    fun revertFrom(page: PageState)
}

data class AddStroke(val stroke: FinishedStroke) : EditCommand
data class EraseStrokes(val strokes: List<FinishedStroke>) : EditCommand   // one drag = ONE command
data class TransformStrokes(val ids: List<StrokeId>, val delta: Transform) : EditCommand
data class ChangeStrokeStyle(val ids: List<StrokeId>, val from: BrushSpec, val to: BrushSpec) : EditCommand
data class ClearPage(val strokes: List<FinishedStroke>) : EditCommand      // undoable — see bug #13
data class AddPage(val page: PageId, val index: Int) : EditCommand
data class DeletePage(val page: PageSnapshot, val index: Int) : EditCommand
```

**Rules**
- History is **per notebook**, not per page — a student expects Ctrl+Z to undo their last action wherever it happened.
- Depth capped at **100** commands; older ones drop off. Bound the memory.
- History does **not** survive app restart in v1. That is a defensible limit; silently losing work is not.
- **`ClearPage` is a command like any other**, so a mis-tap on Clear is one undo away. Today `clearAll()` empties the stroke list *and* the redo stack, making it unrecoverable — from a 48 dp button sitting right below Undo ([`09-project-status.md`](09-project-status.md) bug #13).
- Erasing N strokes in one drag is **one** `EraseStrokes` command, not N.

**This must land before more tools exist**, because every new tool multiplies the broken interactions with the current shared-stack design. It is cheap now and gets steadily more expensive.

---

## Part 10 — Persistence and autosave

| Trigger | Action |
|---|---|
| Stroke committed | Enqueue a write; debounce 500 ms |
| Erase committed | Same |
| Page changed | Flush pending writes |
| `ON_PAUSE` | **Flush synchronously.** This is the one that saves a student's notes when the OS kills the app. |
| Navigating back | Commit in-progress stroke, flush, then navigate |
| Every 30 s while writing | Heartbeat flush |

- Writes are **append-only** where possible. Erase is a tombstone, not a delete ([`17-data-spec.md`](17-data-spec.md) §4) — that is what makes undo/redo and eventual sync tractable.
- No Save button ever. A quiet "Saved ✓" / "Saving…" indicator only ([`15-ux-blueprint.md`](15-ux-blueprint.md) §5.2).
- A write failure surfaces a persistent, non-dismissible bar with Retry. It is the one error in the app that must not auto-dismiss.

---

## Part 11 — Acceptance checklist

The canvas is done when every line passes on the **mid-range reference device** ([`19-quality-bar.md`](19-quality-bar.md) §1).

**Ink feel**
- [ ] Measured latency within budget, with the number written into `PROJECT-LOG.md`
- [ ] Fast diagonal strokes show no visible faceting
- [ ] Pressure varies width on a pressure-capable stylus
- [ ] Finger drawing looks deliberate, not broken, with no pressure data

**Palm and pointers**
- [ ] Palm down first, then stylus → **only the stylus draws**
- [ ] Palm lands mid-stroke → the stroke continues, the palm leaves nothing
- [ ] Two fingers during a stroke, < 120 ms → the stroke is removed, zoom begins
- [ ] Two fingers during a stroke, > 120 ms → the stroke continues
- [ ] Finger-draw off → a finger only pans

**Transform**
- [ ] Pinch zooms about the centroid
- [ ] Zoom clamped; no page smaller than 30 % viewport width
- [ ] Double-tap fits to width
- [ ] Ink stays registered to paper at every zoom level
- [ ] A note drawn on a 10" tablet opens correctly on a 12" one

**Editing**
- [ ] Eraser removes only what it visibly touches
- [ ] One eraser drag = one undo
- [ ] Clear page is undoable
- [ ] Undo/redo enabled states always reflect reality
- [ ] Highlighter draws behind ink and does not darken on overlap

**Durability**
- [ ] Rotate mid-stroke → the stroke persists
- [ ] Force-stop and reopen → everything is where it was, at the right zoom
- [ ] Airplane mode changes nothing

**Performance**
- [ ] A full page of dense ink holds the frame budget
- [ ] A 100-page notebook scrolls smoothly
- [ ] Memory stays within budget on a 2 GB device
