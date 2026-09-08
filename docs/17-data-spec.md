# 17 — Data Specification

> **Status: proposal.** The schema, the on-disk format, and the rules that govern them.
>
> Depends on: [D1](22-decisions.md#d1--paged-documents-not-infinite-canvas) (paged A4, points) · [D6](22-decisions.md#d6--ink-architecture-adopt-androidxink) (`ink-storage`) · [D13](22-decisions.md#d13--android-auto-backup-is-disabled-for-notebook-data) (no auto-backup) · [D15](22-decisions.md#d15--offline-first-the-local-database-is-the-source-of-truth) (local is source of truth).

---

## Part 0 — Five rules, decided once

Every design choice below follows from these. They are cheap now and expensive-to-impossible later.

| # | Rule | If you break it |
|---|---|---|
| **R1** | **Nothing is written until the coordinate model exists.** All geometry is stored in page-space points. | Data is permanently ambiguous — you cannot know what device or zoom produced it. Unmigratable. |
| **R2** | **Every ID is a client-generated UUID**, never an autoincrement integer. | Sync collides on day one. Merging two devices' notebooks becomes impossible. |
| **R3** | **Strokes are immutable and append-only. Erase writes a tombstone.** | Undo/redo and sync conflict-resolution both become intractable. Mutable strokes make sync nearly impossible. |
| **R4** | **Every serialised blob carries a format version.** | You will change the format. Without a version you must discard user data to do it. |
| **R5** | **Every row carries `updatedAt` and `deletedAt` from v1**, even though nothing syncs yet. | Adding them later means a migration across every user's whole corpus. |

R1 is the one that gets broken under schedule pressure. Do not break R1.

---

## Part 1 — Storage layout

Two stores, split by what they are good at.

```
/data/data/com.vahi.app/
├── databases/
│   └── vahi.db                    Room — metadata, indexes, small blobs
│
└── files/
    └── notebooks/
        └── <notebookId>/
            ├── pages/
            │   └── <pageId>.ink   Serialised stroke data for one page
            ├── thumbs/
            │   └── <pageId>.webp  128×180 page thumbnail
            └── source.pdf         Imported original (v1.1), never modified
```

**Why split.** Room is excellent for querying metadata and terrible as a blob store — large `BLOB` columns bloat the database file, slow every query that touches the table, and make the whole DB a single corruption blast radius. Ink lives in per-page files; the database stores paths, bounding boxes and counts.

**Consequences to honour:**
- The database and the files must be kept consistent. Deleting a notebook row **must** delete its directory — do it in a repository method, never in a DAO, and never rely on a foreign-key cascade to remove files.
- A missing `.ink` file is a recoverable error, not a crash: show the page as empty with a "this page couldn't be loaded" marker and log it. Losing one page must never take down the notebook.
- On startup, an orphan-sweep job removes directories with no matching row. Cheap, and it stops storage leaking after a crash mid-delete.

---

## Part 2 — Entities

Room entities. Kotlin types shown; SQL types are the obvious mapping.

### 2.1 `Notebook`

```kotlin
@Entity(
    tableName = "notebooks",
    indices = [Index("folderId"), Index("updatedAt"), Index("isPinned"), Index("deletedAt")]
)
data class NotebookEntity(
    @PrimaryKey val id: String,              // UUID (R2)
    val title: String,
    val folderId: String?,                   // null = library root (v1.1)
    val coverColor: Int,                     // ARGB
    val sourceType: SourceType,              // BLANK | PDF
    val defaultPaperStyle: PaperStyle,       // paper for newly added pages
    val pageCount: Int,                      // denormalised — avoids COUNT() per card
    val thumbnailPageId: String?,            // usually page 1
    val isPinned: Boolean = false,
    val createdAt: Long,                     // epoch ms, UTC
    val updatedAt: Long,                     // R5
    val lastOpenedAt: Long?,                 // drives "Jump back in"
    val deletedAt: Long? = null,             // R5 — soft delete for undo + sync
    val schemaVersion: Int = 1
)
```

`lastOpenedAt` is separate from `updatedAt` on purpose: opening a notebook to read is not editing it, and conflating them makes "Recently edited" lie.

### 2.2 `Page`

```kotlin
@Entity(
    tableName = "pages",
    foreignKeys = [ForeignKey(NotebookEntity::class, ["id"], ["notebookId"], onDelete = CASCADE)],
    indices = [Index("notebookId", "orderIndex"), Index("deletedAt")]
)
data class PageEntity(
    @PrimaryKey val id: String,
    val notebookId: String,
    val orderIndex: Double,                  // sparse ordering — see below
    val widthPt: Float = 595f,               // A4 default (D1)
    val heightPt: Float = 842f,
    val paperStyle: PaperStyle,              // per-page override
    val pdfPageIndex: Int? = null,           // v1.1: which page of source.pdf
    val strokeCount: Int,                    // for culling decisions and empty-page detection
    val contentBoundsLeft: Float?,           // union of stroke bounds; null = empty page
    val contentBoundsTop: Float?,
    val contentBoundsRight: Float?,
    val contentBoundsBottom: Float?,
    val recognisedText: String? = null,      // v1.2 — ML Kit output, for search
    val recognisedAt: Long? = null,
    val recognitionLang: String? = null,     // BCP-47, e.g. "hi"
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)
```

**`orderIndex` is a `Double`, deliberately.** Inserting a page between index 3 and 4 becomes `orderIndex = 3.5` — one row written instead of renumbering every following page. Renormalise to integers in a background job when the gap between neighbours drops below `1e-6`.

### 2.3 `Stroke`

**Strokes are not one row per stroke.** A page holds thousands; individual rows would make loading a page thousands of queries and the database enormous.

Instead: **one `.ink` file per page** holds all its stroke geometry, and the database keeps only what must be queryable.

```kotlin
@Entity(
    tableName = "stroke_index",
    foreignKeys = [ForeignKey(PageEntity::class, ["id"], ["pageId"], onDelete = CASCADE)],
    indices = [Index("pageId"), Index("pageId", "createdAt"), Index("deletedAt")]
)
data class StrokeIndexEntity(
    @PrimaryKey val id: String,              // UUID, matches the id inside the .ink file
    val pageId: String,
    val byteOffset: Int,                     // offset into the page's .ink file
    val byteLength: Int,
    val boundsLeft: Float,                   // page-space points — R1
    val boundsTop: Float,
    val boundsRight: Float,
    val boundsBottom: Float,
    val brushFamily: String,
    val colorArgb: Int,
    val widthPt: Float,
    val layer: InkLayer,                     // HIGHLIGHT | INK  — drives z-order
    val createdAt: Long,
    val deletedAt: Long? = null              // R3: erase = tombstone
)
```

**Why the bounding box lives in indexed columns.** It is what makes viewport culling and hit-testing a **query** instead of a full scan of every stroke on the page. Cheap to add now; painful and migration-requiring to add later. Same reasoning for `layer` — z-ordering must not require deserialising geometry.

### 2.4 Supporting entities

```kotlin
@Entity("folders")            // v1.1
data class FolderEntity(
    @PrimaryKey val id: String, val name: String, val parentId: String?,
    val orderIndex: Double, val createdAt: Long, val updatedAt: Long, val deletedAt: Long? = null
)

@Entity("pen_slots")          // tool rail configuration, global not per-notebook
data class PenSlotEntity(
    @PrimaryKey val slotIndex: Int,          // 0..4
    val brushFamily: String, val colorArgb: Int, val widthPt: Float, val opacity: Float
)
```

Simple app preferences (theme, paper theme, handedness, finger-draw, default paper, language) live in **DataStore** (the plain `datastore-preferences` artifact, not Proto DataStore — see the note on `VAHI-307` in [`21-backlog.md`](21-backlog.md) for why), not Room and not `SharedPreferences` — typed, atomic, and observable as a `Flow`. There is no `SharedPreferences` in the project today; do not introduce one.

### 2.5 Enums — persisted as strings, never ordinals

```kotlin
enum class PaperStyle { BLANK, RULED, GRID, DOTTED, PDF }
enum class SourceType { BLANK, PDF }
enum class InkLayer   { HIGHLIGHT, INK }
```

Room's default is the ordinal. **Override it with a string converter.** An ordinal breaks silently and catastrophically the day someone inserts a value in the middle of an enum, and every existing row's meaning shifts.

This also collapses the current duplicate `NotebookType` / `CanvasType` enums into one `PaperStyle`, and removes the string-literal dispatch (`"Blank"` / `"Ruled"` / …) in the create-notebook modal.

---

## Part 3 — The `.ink` file format

One file per page. Written whole; never partially updated in place.

```
┌─────────────────────────────────────────────────────────┐
│ HEADER (32 bytes)                                       │
│   magic          4B   "VAHI"                            │
│   formatVersion  2B   uint16, currently 1     ← R4      │
│   flags          2B   bit0 = compressed                 │
│   pageWidthPt    4B   float32                           │
│   pageHeightPt   4B   float32                           │
│   strokeCount    4B   uint32                            │
│   engineId       4B   "AXIN" (androidx.ink) | "VHRL"    │
│   engineVersion  4B   uint32                            │
│   reserved       4B                                     │
├─────────────────────────────────────────────────────────┤
│ STROKE RECORDS ×  strokeCount                           │
│   id            16B   UUID                              │
│   payloadLength  4B   uint32                            │
│   payload        …    engine-defined bytes              │
└─────────────────────────────────────────────────────────┘
```

**Design points**

- **`formatVersion` in the header (R4).** You will change this format. Reading code branches on it; writing code always writes the current version. A migration job upgrades files lazily, on open — never in a blocking startup pass over the user's whole library.
- **`engineId` is why the fallback stays viable.** If [D6](22-decisions.md#d6--ink-architecture-adopt-androidxink) is overturned, or if a future `androidx.ink` release changes its serialisation, files declare which engine wrote them and the reader dispatches. Without this field a change of ink engine is a data-loss event.
- **Payload is engine-defined.** With `androidx.ink`, it is whatever `ink-storage` produces. With the fallback (§16-canvas-spec §8), it is a packed `FloatArray` of `(x, y, pressure, tMs)` in page points.
- **Byte offsets in `stroke_index` are into this file**, so a single stroke can be read without deserialising the page. Rewriting the file invalidates them, so the rewrite and the index update happen in **one transaction**.
- **Compression:** zstd or deflate over the stroke-records region when the file exceeds 64 KB. Ink data compresses very well. Storage efficiency matters on the target hardware and it reinforces R4 — flag it in the header, do not infer it.

### 3.1 Write strategy

Naive rewrite-on-every-stroke is O(page size) per stroke and will stutter on a dense page.

```
1. Strokes accumulate in memory for the active page.
2. Debounced 500 ms after a commit, or immediately on ON_PAUSE / page change:
     a. Write to <pageId>.ink.tmp
     b. fsync
     c. Atomically rename over <pageId>.ink
     d. Update stroke_index offsets in the SAME Room transaction
3. If the page has > 500 strokes, append new strokes to the tail and
   rewrite fully only when tombstones exceed 20 % of records (compaction).
```

**Write to a temp file and rename.** A crash mid-write must never leave a truncated `.ink` — that is a whole page of a student's notes.

---

## Part 4 — Tombstones and compaction (R3)

Erase does not remove a stroke. It sets `deletedAt`.

| Why | Effect |
|---|---|
| **Undo** | Un-erase is clearing a timestamp, not resurrecting lost bytes |
| **Sync** (Track F) | A device that was offline can be told "this was deleted" — which is impossible if the row simply vanished |
| **Crash safety** | An erase interrupted mid-write cannot lose the original |

**Compaction** runs when a page's tombstones exceed 20 % of its strokes **and** the notebook has not been opened for 7 days: tombstoned strokes are dropped, the file is rewritten, index rows are deleted. Post-sync, retain tombstones for a sync-horizon window (30 days) before compacting, or offline devices will resurrect deleted strokes.

---

## Part 5 — Queries the schema must serve

Sanity-check on the indexes. Each of these must be a single indexed query, not a scan.

| Screen / feature | Query |
|---|---|
| Library grid | `SELECT * FROM notebooks WHERE deletedAt IS NULL ORDER BY updatedAt DESC` |
| "Jump back in" | `… WHERE deletedAt IS NULL AND lastOpenedAt IS NOT NULL ORDER BY lastOpenedAt DESC LIMIT 3` |
| Pinned filter | `… WHERE isPinned = 1 AND deletedAt IS NULL` |
| Title search | `… WHERE title LIKE :q AND deletedAt IS NULL` |
| Content search (v1.2) | FTS4/5 virtual table over `pages.recognisedText` |
| Page strip for the editor | `SELECT * FROM pages WHERE notebookId = :id AND deletedAt IS NULL ORDER BY orderIndex` |
| **Viewport culling** | `SELECT id, byteOffset, byteLength FROM stroke_index WHERE pageId = :p AND deletedAt IS NULL AND boundsRight >= :l AND boundsLeft <= :r AND boundsBottom >= :t AND boundsTop <= :b` |
| Eraser hit-test | Same bounds query with the eraser rect, then exact geometry on the candidates |
| Storage screen | `SELECT notebookId, SUM(byteLength) FROM stroke_index GROUP BY notebookId` |

All read queries return `Flow` so the UI is reactive by construction; DAO writes are `suspend`.

---

## Part 6 — Migration policy

Two independent versioned things: the **Room schema** and the **`.ink` format**. Do not couple them.

**Room**
- `exportSchema = true`, and the generated JSON schemas are **committed to git**. This is what makes migration tests possible.
- Every schema change ships a `Migration` and a test that opens a real database at version N and migrates it to N+1.
- **`fallbackToDestructiveMigration()` is banned in release builds.** In a notes app it means "silently delete the user's work". Allowed in debug only, behind a build-type check.

**`.ink` format**
- Reader supports **all** historical versions. Writer emits only the current one.
- Upgrade lazily, on page open, then write back. Never a blocking startup migration over the whole library.
- Before any format change, verify a round-trip test corpus of real pages at every prior version.

---

## Part 7 — Identity and the future account ([D10](22-decisions.md#d10--no-auth-wall-account-is-optional-and-requested-in-context))

v1 has no accounts, but the schema must not make adding them a migration over every row.

```kotlin
@Entity("profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,       // UUID; a local anonymous profile from first launch
    val remoteUserId: String? = null, // filled when the user signs in later
    val displayName: String? = null,
    val createdAt: Long
)
```

Notebooks carry `ownerProfileId` from v1, pointing at the local anonymous profile. Signing in later **adopts** the existing local data by setting `remoteUserId` on that same profile — it never creates a second profile and never migrates rows between profiles.

**Get this right the first time.** The alternative — "sign in and your local notes disappear" — is the single worst bug this app could ship, and it is the default outcome if identity is bolted on later.

---

## Part 8 — Backup, export and storage ([D13](22-decisions.md#d13--android-auto-backup-is-disabled-for-notebook-data))

### 8.1 Auto Backup is off for notebook data

The manifest currently declares `allowBackup="true"` with template rules where **every rule is commented out**. The moment Room lands, the whole database silently becomes eligible for Auto Backup — which looks like a free feature and is actually a hazard: a 25 MB quota that silently stops backing up when exceeded, and a restore path that can overwrite newer notes with older ones with no conflict UI.

```xml
<!-- res/xml/backup_rules.xml -->
<full-backup-content>
    <include domain="sharedpref" path="." />
    <exclude domain="database" path="vahi.db" />
    <exclude domain="file"     path="notebooks/" />
</full-backup-content>
```

`data_extraction_rules.xml` mirrors this for both `<cloud-backup>` and `<device-transfer>`.

### 8.2 Export is the user-controlled backup path

| Format | Contents | Use |
|---|---|---|
| **PDF** | Rendered pages, ink flattened, always light paper | Sharing, printing, submitting |
| **PNG** | One page, 2× resolution | Quick share |
| **`.vahi`** | Zip: `manifest.json` + `.ink` files + `source.pdf` | Real backup — round-trips losslessly |

`.vahi` archives must be **importable by a future version**, so the manifest carries the same `formatVersion` discipline (R4).

### 8.3 Storage budget

| Item | Typical |
|---|---|
| One handwritten A4 page | 20–80 KB (`.ink`, compressed) |
| One page thumbnail | 5–15 KB (WebP) |
| A 100-page notebook | 3–10 MB |
| An imported PDF | Original size, stored once, never modified |

Surface real numbers in Settings → Storage, per notebook, and offer "export and delete" for a notebook the user has finished with. On a nearly-full budget tablet — Aditi's situation in [`14-product-spec.md`](14-product-spec.md) §2 — this is a feature, not a diagnostic.

---

## Part 9 — Repository contracts

The layer boundary. `domain/` depends on these interfaces; `data/` implements them; the UI never sees Room or a `File` ([`18-architecture-target.md`](18-architecture-target.md) §3).

```kotlin
interface NotebookRepository {
    fun observeAll(filter: LibraryFilter, sort: LibrarySort): Flow<List<Notebook>>
    fun observeRecent(limit: Int = 3): Flow<List<Notebook>>
    suspend fun create(title: String, paper: PaperStyle, color: Int): Result<NotebookId>
    suspend fun rename(id: NotebookId, title: String): Result<Unit>
    suspend fun setPinned(id: NotebookId, pinned: Boolean): Result<Unit>
    suspend fun duplicate(id: NotebookId): Result<NotebookId>
    suspend fun delete(id: NotebookId): Result<Unit>          // soft delete
    suspend fun restore(id: NotebookId): Result<Unit>          // powers snackbar undo
    suspend fun purgeDeletedOlderThan(cutoff: Long)
}

interface PageRepository {
    fun observePages(notebookId: NotebookId): Flow<List<Page>>
    suspend fun addPage(notebookId: NotebookId, afterIndex: Int?, paper: PaperStyle): Result<PageId>
    suspend fun deletePage(id: PageId): Result<Unit>
    suspend fun reorder(id: PageId, toIndex: Int): Result<Unit>
}

interface InkRepository {
    suspend fun loadStrokes(pageId: PageId, viewport: PageRect?): Result<List<FinishedStroke>>
    suspend fun appendStroke(pageId: PageId, stroke: FinishedStroke): Result<Unit>
    suspend fun tombstoneStrokes(pageId: PageId, ids: List<StrokeId>): Result<Unit>
    suspend fun restoreStrokes(pageId: PageId, ids: List<StrokeId>): Result<Unit>
    suspend fun flush(pageId: PageId): Result<Unit>            // called on ON_PAUSE
}
```

**Notes**
- `loadStrokes` takes an optional viewport so culling is a repository concern, not a UI one.
- `Result<T>` everywhere. Storage fails — disk full, permission lost, file corrupt — and the UI needs to say something honest ([`15-ux-blueprint.md`](15-ux-blueprint.md) §5.8), not crash or silently no-op.
- `restore` exists specifically so snackbar-undo is a repository call rather than UI state juggling.

---

## Part 10 — Acceptance checklist

- [ ] Every geometry value stored is page-space points (R1)
- [ ] Every ID is a client-generated UUID (R2)
- [ ] Erase writes a tombstone; nothing is hard-deleted on the user's behalf (R3)
- [ ] `.ink` header carries `formatVersion` and `engineId` (R4)
- [ ] Every entity has `createdAt`, `updatedAt`, `deletedAt` (R5)
- [ ] Bounding boxes are indexed columns and culling is a single query
- [ ] Enums persist as strings, not ordinals
- [ ] Room schemas exported and committed; a migration test exists per version
- [ ] `fallbackToDestructiveMigration` absent from release builds
- [ ] `.ink` writes are temp-file + fsync + atomic rename
- [ ] Deleting a notebook removes both rows and files; an orphan sweep runs at startup
- [ ] Auto Backup excludes the database and `notebooks/`
- [ ] A missing/corrupt `.ink` degrades to one empty page, never a crash
- [ ] Force-stop mid-stroke → reopen → at most the one uncommitted stroke is missing
