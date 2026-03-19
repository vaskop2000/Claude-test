package com.musicnotation.editor.rendering

object RenderConstants {
    // Staff geometry (in pixels, scaled by density)
    const val STAFF_SPACE_DP = 10f        // distance between staff lines (1 space)
    const val STAFF_LINE_WIDTH_DP = 1.5f
    const val BAR_LINE_WIDTH_DP = 1.5f
    const val STEM_WIDTH_DP = 1.2f

    const val NOTE_HEAD_WIDTH_RATIO = 1.4f  // notehead width = space * ratio
    const val NOTE_HEAD_HEIGHT_RATIO = 1.0f
    const val STEM_LENGTH_SPACES = 3.5f     // stem length in staff spaces

    const val LEDGER_LINE_EXTRA = 0.8f      // extra width on each side of notehead
    const val DOT_RADIUS_RATIO = 0.2f       // dot radius relative to space
    const val DOT_OFFSET_X = 1.5f          // dot x offset in spaces after notehead

    // Layout
    const val CLEF_WIDTH_SPACES = 3.0f
    const val KEY_SIG_ACCIDENTAL_WIDTH_SPACES = 1.3f
    const val TIME_SIG_WIDTH_SPACES = 2.0f
    const val MEASURE_PADDING_SPACES = 0.5f
    const val NOTE_SPACING_MIN_SPACES = 2.5f   // min space between notes
    const val MEASURE_RIGHT_PAD_SPACES = 1.0f  // padding before bar line
    const val HEADER_GAP_DP = 4f               // gap between clef / key sig / time sig

    const val STAFF_LABEL_WIDTH_DP = 80f
    const val STAFF_MARGIN_TOP_DP = 20f
    const val STAFF_MARGIN_BOTTOM_DP = 20f
    const val SYSTEM_MARGIN_LEFT_DP = 12f

    // Beam
    const val BEAM_THICKNESS_RATIO = 0.4f   // beam thickness relative to space
    const val BEAM_SPACING_RATIO = 0.6f     // spacing between beams

    // Colors
    const val COLOR_INK = 0xFF1A1A1A.toInt()
    const val COLOR_STAFF_LINE = 0xFF333333.toInt()
    const val COLOR_SELECTED = 0xFF1976D2.toInt()
    const val COLOR_CURSOR = 0xFFFF5722.toInt()
    const val COLOR_BACKGROUND = 0xFFFAFAF8.toInt()
}
