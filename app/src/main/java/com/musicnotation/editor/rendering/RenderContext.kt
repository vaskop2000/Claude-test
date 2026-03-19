package com.musicnotation.editor.rendering

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface

/**
 * Holds scaled rendering parameters and shared Paint objects.
 */
class RenderContext(context: Context) {

    val density: Float = context.resources.displayMetrics.density

    // Scale dp to px
    fun dp(value: Float): Float = value * density

    val space: Float = dp(RenderConstants.STAFF_SPACE_DP)
    val staffHeight: Float = space * 4f   // 4 spaces = 5 lines
    val noteHeadW: Float = space * RenderConstants.NOTE_HEAD_WIDTH_RATIO
    val noteHeadH: Float = space * RenderConstants.NOTE_HEAD_HEIGHT_RATIO
    val stemLength: Float = space * RenderConstants.STEM_LENGTH_SPACES
    val beamThickness: Float = space * RenderConstants.BEAM_THICKNESS_RATIO
    val beamSpacing: Float = space * RenderConstants.BEAM_SPACING_RATIO
    val clefWidth: Float = space * RenderConstants.CLEF_WIDTH_SPACES
    val keySigAccidentalWidth: Float = space * RenderConstants.KEY_SIG_ACCIDENTAL_WIDTH_SPACES
    val timeSigWidth: Float = space * RenderConstants.TIME_SIG_WIDTH_SPACES
    val noteSpacingMin: Float = space * RenderConstants.NOTE_SPACING_MIN_SPACES

    val staffLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = RenderConstants.COLOR_STAFF_LINE
        strokeWidth = dp(RenderConstants.STAFF_LINE_WIDTH_DP)
        style = Paint.Style.STROKE
    }

    val barLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = RenderConstants.COLOR_INK
        strokeWidth = dp(RenderConstants.BAR_LINE_WIDTH_DP)
        style = Paint.Style.STROKE
    }

    val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = RenderConstants.COLOR_INK
        strokeWidth = dp(RenderConstants.STEM_WIDTH_DP)
        style = Paint.Style.STROKE
    }

    val noteFilledPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = RenderConstants.COLOR_INK
        style = Paint.Style.FILL
    }

    val noteOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = RenderConstants.COLOR_INK
        strokeWidth = dp(1.5f)
        style = Paint.Style.STROKE
    }

    val beamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = RenderConstants.COLOR_INK
        style = Paint.Style.FILL
    }

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = RenderConstants.COLOR_INK
        textSize = space * 2f
        typeface = Typeface.DEFAULT_BOLD
    }

    val smallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = RenderConstants.COLOR_INK
        textSize = space * 1.6f
    }

    val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = RenderConstants.COLOR_SELECTED
        style = Paint.Style.FILL
        alpha = 60
    }

    val musicSymbolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = RenderConstants.COLOR_INK
        textSize = space * 8f
        textAlign = Paint.Align.CENTER
    }

    val accidentalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = RenderConstants.COLOR_INK
        textSize = space * 3.5f
        textAlign = Paint.Align.CENTER
    }

    val timeSigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = RenderConstants.COLOR_INK
        textSize = space * 3.2f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    val tupletPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = RenderConstants.COLOR_INK
        textSize = space * 1.8f
        textAlign = Paint.Align.CENTER
    }

    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = RenderConstants.COLOR_INK
        textSize = space * 1.5f
        textAlign = Paint.Align.RIGHT
    }

    // ledger line
    val ledgerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = RenderConstants.COLOR_STAFF_LINE
        strokeWidth = dp(RenderConstants.STAFF_LINE_WIDTH_DP)
        style = Paint.Style.STROKE
    }
}
