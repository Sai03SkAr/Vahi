# 05 — The Drawing Engine

Everything under `com.vahi.app.ui.components.drawing`. Three files, ~280 lines total, and the most product-critical code in the repo.

```
Stroke.kt         data model — one pen mark
CanvasState.kt    state holder — stroke list, undo/redo, tool, colour, width
DrawingCanvas.kt  composable — pointer capture + rendering
```

---

## `Stroke`

```kotlin
data class Stroke(
    val id: UUID = UUID.randomUUID(),
    val path: Path,
    val color: Color = Color.Black,
    val strokeWidth: Float = 4f
)
```

A stroke is a Compose `Path` plus its paint properties, identified by a `UUID` for undo/redo bookkeeping.

**Design implications to be aware of:**

- The raw input points are **not retained** — only the accumulated `Path`. That means strokes cannot be re-smoothed, simplified, pressure-modulated after the fact, or serialised without first extracting geometry back out of the `Path`. Any persistence work will need a point-list representation alongside (or instead of) the `Path`.
- `Path` is **mutable**, which sits awkwardly inside a `data class`. Two `Stroke` copies share the same `Path` instance (see `continueStroke` below).

---

## `ToolType`

```kotlin
enum class ToolType { PEN, ERASER }
```

Only two tools. No highlighter, lasso/select, shape, text, or laser pointer.

---

## `CanvasState`

A plain class (not a ViewModel) holding Compose-observable state for one canvas session.

### Fields

| Field | Type | Exposure | Default |
|---|---|---|---|
| `_strokes` / `strokes` | `mutableStateListOf<Stroke>()` / `List<Stroke>` | private backing, read-only public | empty |
| `_undoneStrokes` / `undoneStrokes` | same pattern | private backing, read-only public | empty |
| `currentStroke` | `Stroke?` | public getter, `private set` | `null` |
| `activeTool` | `ToolType` | public read/write | `PEN` |
| `strokeColor` | `Color` | public read/write | `Color.Black` |
| `strokeWidth` | `Float` | public read/write | `4f` |

The encapsulation is clean: mutation happens only through methods, and the lists are exposed as immutable `List<Stroke>`.

### Methods

| Method | Behaviour |
|---|---|
| `startStroke(position: Offset)` | Creates a `Path` with `moveTo(x, y)` and assigns a fresh `Stroke` (new UUID, current colour/width) to `currentStroke`. |
| `continueStroke(position: Offset)` | `currentStroke.path.lineTo(x, y)`, then `currentStroke = stroke.copy(path = stroke.path)` — the copy exists purely to produce a new object identity so Compose recomposes. |
| `finishStroke()` | Appends `currentStroke` to `_strokes`, **clears the redo stack**, sets `currentStroke = null`. |
| `cancelStroke()` | Drops `currentStroke` without committing. *Currently never called.* |
| `eraseAtPosition(position, hitRadius = 20f)` | Builds a square `Rect` of side `2 × hitRadius` around the point; removes every stroke whose `path.getBounds()` overlaps it, pushing each onto `_undoneStrokes`. |
| `strokeIntersectsRect(stroke, rect)` | private — `stroke.path.getBounds().overlaps(rect)` |
| `undo()` | Moves the last stroke from `_strokes` to `_undoneStrokes`. |
| `redo()` | Moves the last stroke from `_undoneStrokes` back to `_strokes`. |
| `canUndo()` / `canRedo()` | Emptiness checks — intended to drive toolbar button enablement. |
| `clearAll()` | Empties both lists and nulls `currentStroke`. 🔴 **Unrecoverable** — see below. |

### Correctness notes

**🔴 `clearAll()` destroys the undo stack, and the toolbar wires it to a one-tap button.** Because it calls `_undoneStrokes.clear()` alongside `_strokes.clear()`, undo cannot recover the page. `DrawingToolbar` places that button directly below Undo and Redo. This is a data-loss bug in shipped code — [`09-project-status.md`](09-project-status.md) #13, fix tracked as `VAHI-003`.

**Eraser precision — bounding boxes, not geometry.** `strokeIntersectsRect` compares against the stroke's *axis-aligned bounding rectangle*. A long diagonal line has a bounding box covering a huge area of the page, so touching anywhere inside that box deletes the whole stroke — even far from the ink. This is the single biggest correctness gap in the engine.

**Eraser granularity — whole strokes only.** There is no partial/pixel erase. Touching a stroke removes all of it.

**Erase and undo share one stack.** Erased strokes are pushed onto `_undoneStrokes`, so `redo()` will resurrect them. Two problems follow:
1. Erasing several strokes in one drag pushes them individually, so each `redo()` restores one — order is by removal sequence, not by user action.
2. Because `finishStroke()` clears the redo stack, drawing after erasing silently discards the ability to un-erase.
A proper fix is a single command/action stack (`AddStroke`, `EraseStrokes(list)`) rather than a stroke stack.

**`continueStroke` allocates per motion event.** Every pointer move creates a new `Stroke` object. The `Path` is shared by reference, so it isn't a deep copy — but at ~120–240 Hz stylus sampling this is meaningful allocation churn. Bumping an integer version counter in the state holder would be cheaper.

**No smoothing.** Points are joined with `lineTo` — straight segments. Fast strokes will look faceted. Quadratic/cubic Bézier interpolation between sampled points is the standard remedy.

**No pressure, tilt, or velocity.** `strokeWidth` is constant per stroke. `PointerInputChange` carries pressure on stylus-capable devices; it is ignored.

---

## `DrawingCanvas`

```kotlin
@Composable
fun DrawingCanvas(canvasState: CanvasState, modifier: Modifier = Modifier)
```

A transparent `Canvas` that fills its parent, captures pointer input, and paints the ink.

### Input handling

```kotlin
.pointerInput(canvasState.activeTool) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull() ?: continue
            when (canvasState.activeTool) {
                ToolType.PEN -> {
                    if (change.pressed) {
                        if (canvasState.currentStroke == null) canvasState.startStroke(change.position)
                        else                                   canvasState.continueStroke(change.position)
                        change.consume()
                    } else if (canvasState.currentStroke != null) {
                        canvasState.finishStroke()
                    }
                }
                ToolType.ERASER -> {
                    if (change.pressed) { canvasState.eraseAtPosition(change.position); change.consume() }
                }
            }
        }
    }
}
```

This is raw `awaitPointerEvent` handling rather than `detectDragGestures` — the right call for ink, since gesture detectors add slop thresholds and latency that make handwriting feel laggy.

**Behavioural notes:**

- `pointerInput(canvasState.activeTool)` re-keys the handler when the tool changes, restarting the loop cleanly.
- 🔴 **`event.changes.firstOrNull()` takes only the first pointer — and this is worse than "no palm rejection".** It actively selects the wrong pointer. A palm typically lands *before* the stylus tip, so the palm becomes pointer 1 and **the palm draws while the stylus does nothing**. When the primary pointer lifts, the next event's first change is a different already-pressed pointer, so a **new stroke starts under the palm**. `change.type == PointerType.Stylus` is available and completely unused. See [`09-project-status.md`](09-project-status.md) #14 and the replacement state machine in [`16-canvas-spec.md`](16-canvas-spec.md) §4.
- Only *historical-free* positions are used — `change.historical` (the intermediate samples Android batches between frames) is discarded, which loses fidelity on fast strokes.
- Pen events are consumed while pressed; eraser events are consumed while pressed. **Nothing underneath the canvas can receive gestures** — this is why `GridCanvasContent`'s scroll modifiers never fire.

### Rendering

```kotlin
canvasState.strokes.forEach { stroke ->
    drawPath(stroke.path, stroke.color,
             style = Stroke(width = stroke.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
canvasState.currentStroke?.let { /* same drawPath */ }
```

Round caps and joins give smooth stroke ends and corners. **Every completed stroke is redrawn on every frame.** With no layer caching or dirty-rect optimisation, frame cost grows linearly with stroke count — a page with a few thousand strokes will drop frames. The standard fix is to rasterise settled strokes into a cached bitmap/`GraphicsLayer` and only live-render the active stroke.

---

## What the engine cannot do yet

| Missing | Notes |
|---|---|
| ~~**Any UI to control it**~~ | ✅ **Resolved in Session 4** — `DrawingToolbar.kt` wires `activeTool`, `strokeColor`, `strokeWidth`, `undo()`, `redo()` and `clearAll()`. **`cancelStroke()` still has no caller anywhere** — which is bug #14's other half: there is no path that abandons a stroke, so an interrupted or palm-started stroke is always committed. |
| **Persistence** | `CanvasState` is created with `remember`, not `rememberSaveable` and not ViewModel-scoped. Navigating back to Home, or simply rotating the device, destroys all strokes irrecoverably. |
| **Zoom / pan** | No transform matrix. The canvas is 1:1 with the screen. |
| **Pages** | One infinite surface, no page boundaries, no page navigation. |
| **Selection / lasso** | Not implemented. |
| **Shape recognition, text, images** | Not implemented. |
| **Export** | No PDF/PNG/SVG output. |

---

## Suggested build order, if you extend this next

> **Superseded by [`16-canvas-spec.md`](16-canvas-spec.md) and [`21-backlog.md`](21-backlog.md) as of Session 5.** Step 1 below shipped in Session 4. The rest is retained for context, but the current plan replaces this engine rather than extending it — [D6](22-decisions.md#d6--ink-architecture-adopt-androidxink) adopts `androidx.ink` 1.0.0, which supplies real stroke geometry, low-latency authoring and a serialisation format. Work the backlog order (`VAHI-101` → `VAHI-102` → `VAHI-110` → `VAHI-112`), not this list.

1. ~~**Toolbar**~~ — ✅ done in Session 4 (`DrawingToolbar.kt`), with four defects found in Session 5 (#13, #15, #16, #18).
2. **Command-based undo stack** — replace the shared `_undoneStrokes` list before more tools make the conflation worse. → `VAHI-120`
3. **Geometry-accurate erasing** — test distance to the eraser centre, not bounding-box overlap. → `VAHI-121`
4. **Persistence** — a serialisable stroke representation, then a repository/Room layer. **Must not precede the coordinate model.** → `VAHI-110`, then `VAHI-301`
5. **Rendering performance** — cache settled strokes; cull to the viewport. → `VAHI-115`
6. **Pointer arbitration and palm rejection** — *promoted to the top of the real priority order in Session 5*; bug #14 makes this the most severe defect in the engine. → `VAHI-112`, `VAHI-113`
7. **Stylus fidelity** — historical points, pressure-modulated width, Bézier smoothing. → `VAHI-114`
