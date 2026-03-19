package com.musicnotation.editor.rendering

import com.musicnotation.editor.data.model.*

/**
 * Computes X positions for all elements in a score.
 * Returns a map of (staffIndex, measureIndex, elementId) -> xPosition
 */
class LayoutEngine(private val rc: RenderContext) {

    data class MeasureLayout(
        val measureIndex: Int,
        val startX: Float,
        val width: Float,
        val headerWidth: Float,  // clef + key sig + time sig
        val elementPositions: Map<Int, Float>  // elementId -> x
    )

    data class StaffLayout(
        val staffIndex: Int,
        val topY: Float,
        val measures: List<MeasureLayout>
    )

    data class ScoreLayout(
        val staves: List<StaffLayout>,
        val totalWidth: Float,
        val totalHeight: Float
    )

    fun layout(score: Score, startX: Float, startY: Float): ScoreLayout {
        val staffLayouts = mutableListOf<StaffLayout>()

        // Calculate measure widths from the widest staff
        val measureWidths = calculateMeasureWidths(score)

        var currentX = startX
        var currentY = startY + rc.dp(RenderConstants.STAFF_MARGIN_TOP_DP)

        score.staves.forEachIndexed { staffIdx, staff ->
            val measureLayouts = mutableListOf<MeasureLayout>()
            currentX = startX

            staff.measures.forEachIndexed { measureIdx, measure ->
                val isFirst = measureIdx == 0
                val headerWidth = if (isFirst) {
                    calculateHeaderWidth(staff)
                } else {
                    rc.dp(4f) // small padding for bar line
                }

                val measureWidth = measureWidths[measureIdx]
                val elementPositions = layoutElements(
                    measure, currentX + headerWidth, measureWidth - headerWidth, staff.timeSignature
                )

                measureLayouts.add(MeasureLayout(
                    measureIndex = measureIdx,
                    startX = currentX,
                    width = measureWidth,
                    headerWidth = headerWidth,
                    elementPositions = elementPositions
                ))

                currentX += measureWidth
            }

            staffLayouts.add(StaffLayout(
                staffIndex = staffIdx,
                topY = currentY,
                measures = measureLayouts
            ))

            currentY += rc.staffHeight + rc.dp(RenderConstants.STAFF_MARGIN_BOTTOM_DP) +
                    rc.dp(RenderConstants.STAFF_MARGIN_TOP_DP)
        }

        return ScoreLayout(
            staves = staffLayouts,
            totalWidth = currentX,
            totalHeight = currentY
        )
    }

    private fun calculateHeaderWidth(staff: Staff): Float {
        var width = rc.dp(RenderConstants.SYSTEM_MARGIN_LEFT_DP)
        width += rc.clefWidth + rc.dp(RenderConstants.HEADER_GAP_DP)
        if (staff.keySignature.accidentalPitches.isNotEmpty()) {
            width += staff.keySignature.accidentalPitches.size * rc.keySigAccidentalWidth
            width += rc.dp(RenderConstants.HEADER_GAP_DP)
        }
        width += rc.timeSigWidth + rc.dp(RenderConstants.HEADER_GAP_DP)
        return width
    }

    private fun calculateMeasureWidths(score: Score): List<Float> {
        if (score.staves.isEmpty()) return emptyList()
        val measureCount = score.measureCount
        val widths = FloatArray(measureCount) { 0f }

        score.staves.forEach { staff ->
            staff.measures.forEachIndexed { i, measure ->
                val contentW = calculateMeasureContentWidth(measure, staff.timeSignature)
                // First measure must also accommodate the header (clef + key sig + time sig)
                val headerW = if (i == 0) calculateHeaderWidth(staff) else rc.dp(4f)
                val w = contentW + headerW
                if (w > widths[i]) widths[i] = w
            }
        }

        // Ensure minimum measure width
        val minWidth = rc.space * 12f
        return widths.map { maxOf(it, minWidth) }
    }

    private fun calculateMeasureContentWidth(measure: Measure, timeSignature: TimeSignature): Float {
        val totalTicks = timeSignature.ticksPerMeasure.toFloat()
        // Proportional width: one quarter-note worth of space per beat
        val quartersInMeasure = totalTicks / Duration.QUARTER.ticks
        val timeProportionalWidth = quartersInMeasure * rc.noteSpacingMin
        val noteCountWidth = measure.elements.size * rc.noteSpacingMin
        val rightPad = rc.noteHeadW + rc.space * RenderConstants.MEASURE_RIGHT_PAD_SPACES
        return maxOf(timeProportionalWidth, noteCountWidth) + rightPad
    }

    private fun layoutElements(
        measure: Measure,
        contentStartX: Float,
        availableWidth: Float,
        timeSignature: TimeSignature
    ): Map<Int, Float> {
        val positions = mutableMapOf<Int, Float>()
        if (measure.elements.isEmpty()) return positions

        // Reserve space for note head + padding before bar line so notes don't overlap it
        val rightPad = rc.noteHeadW + rc.space * RenderConstants.MEASURE_RIGHT_PAD_SPACES
        val usableWidth = maxOf(availableWidth - rightPad, rc.noteSpacingMin)

        val totalTicks = timeSignature.ticksPerMeasure.toFloat()
        measure.elements.forEach { element ->
            val xFraction = element.tickOffset / totalTicks
            val x = contentStartX + xFraction * usableWidth
            positions[element.id] = x
        }
        return positions
    }
}
