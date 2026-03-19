package com.musicnotation.editor.rendering

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import com.musicnotation.editor.data.model.*

class MusicRenderer(context: Context) {

    val rc = RenderContext(context)

    private val staffRenderer = StaffRenderer(rc)
    private val clefRenderer = ClefRenderer(rc)
    private val keySigRenderer = KeySigRenderer(rc)
    private val timeSigRenderer = TimeSigRenderer(rc)
    private val noteRenderer = NoteRenderer(rc)
    private val beamRenderer = BeamRenderer(rc, noteRenderer)
    private val tupletRenderer = TupletRenderer(rc)
    private val layoutEngine = LayoutEngine(rc)

    private var lastLayout: LayoutEngine.ScoreLayout? = null

    /**
     * Main render entry point.
     * @param canvas Android canvas to draw on
     * @param score the score to render
     * @param selectedElementId the currently selected element ID (-1 = none)
     * @param labelWidth width of the staff label area on the left
     */
    fun render(
        canvas: Canvas,
        score: Score,
        selectedElementId: Int = -1,
        labelWidth: Float = rc.dp(RenderConstants.STAFF_LABEL_WIDTH_DP)
    ) {
        // Draw white background
        canvas.drawColor(RenderConstants.COLOR_BACKGROUND)

        val startX = labelWidth + rc.dp(RenderConstants.SYSTEM_MARGIN_LEFT_DP)
        val startY = rc.dp(RenderConstants.STAFF_MARGIN_TOP_DP) + rc.dp(40f) // top margin for title

        // Layout calculation
        val layout = layoutEngine.layout(score, startX, startY)
        lastLayout = layout

        // Draw title and composer
        drawScoreHeader(canvas, score, startX)

        // Draw staff labels
        score.staves.forEachIndexed { staffIdx, staff ->
            val staffLayout = layout.staves.getOrNull(staffIdx) ?: return@forEachIndexed
            drawStaffLabel(canvas, staff, labelWidth, staffLayout.topY)
        }

        // Draw system bracket if multiple staves
        if (score.staves.size > 1) {
            val firstStaffTop = layout.staves.firstOrNull()?.topY ?: return
            val lastStaffBottom = layout.staves.lastOrNull()?.let { it.topY + rc.staffHeight } ?: return
            staffRenderer.drawSystemBracket(canvas, startX - rc.dp(8f), firstStaffTop, lastStaffBottom)
        }

        // Draw each staff
        score.staves.forEachIndexed { staffIdx, staff ->
            val staffLayout = layout.staves.getOrNull(staffIdx) ?: return@forEachIndexed
            renderStaff(canvas, staff, staffLayout, score, selectedElementId)
        }
    }

    private fun renderStaff(
        canvas: Canvas,
        staff: Staff,
        staffLayout: LayoutEngine.StaffLayout,
        score: Score,
        selectedElementId: Int
    ) {
        val staffTopY = staffLayout.topY
        val totalWidth = staffLayout.measures.lastOrNull()?.let { it.startX + it.width } ?: return

        // Draw staff lines spanning all measures
        val firstMeasureX = staffLayout.measures.firstOrNull()?.startX ?: return
        staffRenderer.drawStaffLines(canvas, firstMeasureX, totalWidth, staffTopY)

        // Draw each measure
        staff.measures.forEachIndexed { measureIdx, measure ->
            val measureLayout = staffLayout.measures.getOrNull(measureIdx) ?: return@forEachIndexed
            renderMeasure(canvas, staff, measure, measureLayout, staffTopY, selectedElementId)
        }

        // Final bar line
        staffRenderer.drawFinalBarLine(canvas, totalWidth, staffTopY)
    }

    private fun renderMeasure(
        canvas: Canvas,
        staff: Staff,
        measure: Measure,
        measureLayout: LayoutEngine.MeasureLayout,
        staffTopY: Float,
        selectedElementId: Int
    ) {
        val x = measureLayout.startX

        // Draw bar line at measure start (except first measure)
        if (measureLayout.measureIndex > 0) {
            staffRenderer.drawBarLine(canvas, x, staffTopY)
        }

        // Draw header (clef, key sig, time sig) on first measure
        if (measureLayout.measureIndex == 0) {
            var headerX = x + rc.dp(RenderConstants.SYSTEM_MARGIN_LEFT_DP)
            clefRenderer.draw(canvas, staff.clef, headerX, staffTopY)
            headerX += rc.clefWidth + rc.dp(RenderConstants.HEADER_GAP_DP)

            keySigRenderer.draw(canvas, staff.keySignature, staff.clef, headerX, staffTopY)
            if (staff.keySignature.accidentalPitches.isNotEmpty()) {
                headerX += staff.keySignature.accidentalPitches.size * rc.keySigAccidentalWidth
                headerX += rc.dp(RenderConstants.HEADER_GAP_DP)
            }

            timeSigRenderer.draw(canvas, staff.timeSignature, headerX, staffTopY)
        }

        // Draw measure number above first staff only
        drawMeasureNumber(canvas, measure.index + 1, x, staffTopY)

        // Draw elements
        measure.elements.forEach { element ->
            val elemX = measureLayout.elementPositions[element.id] ?: return@forEach
            val isSelected = element.id == selectedElementId
            when (element) {
                is NoteElement.Note -> noteRenderer.drawNote(canvas, element, elemX, staffTopY, staff.clef, isSelected)
                is NoteElement.Rest -> noteRenderer.drawRest(canvas, element, elemX, staffTopY)
                is NoteElement.Chord -> noteRenderer.drawChord(canvas, element, elemX, staffTopY, staff.clef, isSelected)
            }
        }

        // Draw beams
        beamRenderer.drawBeams(canvas, measure, measureLayout.elementPositions, staffTopY, staff.clef)

        // Draw tuplets
        tupletRenderer.drawTuplets(canvas, measure, measureLayout.elementPositions, staffTopY)
    }

    private fun drawScoreHeader(canvas: Canvas, score: Score, startX: Float) {
        val titlePaint = Paint(rc.textPaint).apply {
            textSize = rc.space * 2.5f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(score.title, startX + 400f, rc.dp(30f), titlePaint)
        if (score.composer.isNotEmpty()) {
            val composerPaint = Paint(rc.smallTextPaint).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText(score.composer, startX + 800f, rc.dp(50f), composerPaint)
        }
    }

    private fun drawStaffLabel(canvas: Canvas, staff: Staff, labelWidth: Float, staffTopY: Float) {
        val y = staffTopY + rc.staffHeight / 2 + rc.dp(4f)
        canvas.drawText(staff.displayName, labelWidth - rc.dp(8f), y, rc.labelPaint)
    }

    private fun drawMeasureNumber(canvas: Canvas, number: Int, x: Float, staffTopY: Float) {
        if (number <= 1) return  // Don't draw number for first measure
        val paint = Paint(rc.smallTextPaint).apply { textSize = rc.space * 1.2f }
        canvas.drawText(number.toString(), x + rc.dp(2f), staffTopY - rc.dp(4f), paint)
    }

    /**
     * Hit-test: given a canvas coordinate, returns the nearest staff index.
     */
    fun hitTestStaff(y: Float): Int {
        val layout = lastLayout ?: return -1
        layout.staves.forEachIndexed { idx, staffLayout ->
            if (y >= staffLayout.topY - rc.space && y <= staffLayout.topY + rc.staffHeight + rc.space) {
                return idx
            }
        }
        return -1
    }

    /**
     * Hit-test: given a canvas coordinate, returns the measure index within a staff.
     */
    fun hitTestMeasure(x: Float, staffIndex: Int): Int {
        val layout = lastLayout ?: return -1
        val staffLayout = layout.staves.getOrNull(staffIndex) ?: return -1
        staffLayout.measures.forEachIndexed { idx, measureLayout ->
            if (x >= measureLayout.startX && x < measureLayout.startX + measureLayout.width) {
                return idx
            }
        }
        return -1
    }

    /**
     * Given a tap Y position within a staff, calculates the pitch.
     */
    fun yToPitch(y: Float, staffTopY: Float, clef: Clef): Pitch {
        val middleLineY = staffTopY + 2 * rc.space
        val halfSpaces = ((middleLineY - y) / (rc.space / 2f)).toInt()

        // halfSpaces to diatonic position
        val middleAbsolutePos = clef.middleLineAbsolutePos()
        val absolutePos = middleAbsolutePos + halfSpaces
        val octave = absolutePos / 7
        val stepIndex = ((absolutePos % 7) + 7) % 7
        val step = PitchStep.values()[stepIndex]
        return Pitch(step, octave)
    }

    fun getStaffTopY(staffIndex: Int): Float? {
        return lastLayout?.staves?.getOrNull(staffIndex)?.topY
    }

    fun getStaffClef(score: Score, staffIndex: Int): Clef {
        return score.staves.getOrNull(staffIndex)?.clef ?: Clef.TREBLE
    }

    fun getLayout(): LayoutEngine.ScoreLayout? = lastLayout
}
