package com.vahi.app.domain.model

import com.vahi.app.core.model.PaperStyle

/** docs/22-decisions.md V1 — app theme and paper theme are deliberately separate
 *  settings; a dark room and a canonical light document are not the same axis. */
enum class AppTheme { LIGHT, DARK, SYSTEM }

/** Presentation only — ink colours are stored unchanged and always export light.
 *  See docs/16-canvas-spec.md §7. */
enum class PaperTheme { WHITE, CREAM, DARK }

/** Drives tool-rail edge placement — see `DrawingToolbar`'s KDoc and
 *  docs/15-ux-blueprint.md §7.2. Default is RIGHT, matching the rail's existing
 *  default left-edge placement (away from a right hand's palm). */
enum class Handedness { LEFT, RIGHT }

/**
 * App-wide settings — never per-notebook data, so this is not a Room table.
 * docs/17-data-spec.md §2.4: "theme, paper theme, handedness, finger-draw,
 * default paper, language" live here. Pen slot colours are deliberately
 * excluded — docs/17-data-spec.md §2.4 defines `PenSlotEntity` as a Room table
 * (richer structured data: 5 slots × 4 fields each), which contradicts the one
 * line in `VAHI-307`'s backlog entry that lists them as a DataStore field.
 * The data-spec's concrete entity definition is treated as authoritative here.
 *
 * [fingerDrawEnabled] defaults `true`: CLAUDE.md's own guidance is "design for
 * the finger; treat stylus data as an enhancement" — the target market often
 * has no pressure-sensitive stylus at all.
 */
data class UserPreferences(
    val appTheme: AppTheme = AppTheme.SYSTEM,
    val paperTheme: PaperTheme = PaperTheme.WHITE,
    val handedness: Handedness = Handedness.RIGHT,
    val fingerDrawEnabled: Boolean = true,
    val defaultPaperStyle: PaperStyle = PaperStyle.BLANK,
    val languageTag: String = "en"
)
