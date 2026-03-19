package com.musicnotation.editor.rendering

import android.graphics.Canvas
import android.graphics.Path
import com.musicnotation.editor.data.model.*

class BeamRenderer(private val rc: RenderContext, private val noteRenderer: NoteRenderer) {

    /**
     * Draws beam groups for a measure.
     * elementPositions: elementId -> x center
     */
    fun drawBeams(
        canvas: Canvas,
        measure: Measure,
        elementPositions: Map<Int, Float>,
        staffTopY: Float,
        clef: Clef
    ) {
        measure.beamGroups.forEach { beamGroup ->
            drawBeamGroup(canvas, beamGroup, measure, elementPositions, staffTopY, clef)
        }

        // Auto-beam consecutive beamable notes that have no beam group
        val unbeamed = measure.elements.filter { it.beamGroupId == null && it.duration.isBeamable }
        if (unbeamed.size >= 2) {
            val autoGroup = BeamGroup(id = -1, elementIds = unbeamed.map { it.id }.toMutableList())
            drawBeamGroup(canvas, autoGroup, measure, elementPositions, staffTopY, clef)
        }
    }

    private fun drawBeamGroup(
        canvas: Canvas,
        beamGroup: BeamGroup,
        measure: Measure,
        elementPositions: Map<Int, Float>,
        staffTopY: Float,
        clef: Clef
    ) {
        val elements = beamGroup.elementIds.mapNotNull { id ->
            measure.elements.find { it.id == id }
        }.filter { it.duration.isBeamable }

        if (elements.size < 2) return

        // Determine stem direction for the group
        val firstPos = elementPositions[elements.first().id] ?: return
        val lastPos = elementPositions[elements.last().id] ?: return

        val stemUp = when (beamGroup.stemDirectionOverride) {
            StemDirection.UP -> true
            StemDirection.DOWN -> false
            StemDirection.AUTO -> {
                val avgY = elements.mapNotNull { el ->
                    when (el) {
                        is NoteElement.Note -> noteRenderer.pitchToY(el.pitch, staffTopY, clef)
                        is NoteElement.Chord -> el.pitches.map {
                            noteRenderer.pitchToY(it, staffTopY, clef)
                        }.average().toFloat()
                        else -> null
                    }
                }.average().toFloat()
                avgY > staffTopY + 2 * rc.space
            }
        }

        // Compute stem tip Y positions for first and last note
        val firstNoteY = getNoteY(elements.first(), elementPositions, staffTopY, clef)
        val lastNoteY = getNoteY(elements.last(), elementPositions, staffTopY, clef)

        val stemOffsetX = if (stemUp) rc.noteHeadW * 0.45f else -rc.noteHeadW * 0.45f
        val firstStemTipY: Float
        val lastStemTipY: Float

        if (stemUp) {
            firstStemTipY = firstNoteY - rc.stemLength
            lastStemTipY = lastNoteY - rc.stemLength
        } else {
            firstStemTipY = firstNoteY + rc.stemLength
            lastStemTipY = lastNoteY + rc.stemLength
        }

        // Determine max beam order
        val maxBeams = elements.maxOf { it.duration.base.flagCount() }

        // Draw stems for each note in the group
        elements.forEach { el ->
            val elX = elementPositions[el.id] ?: return@forEach
            val elY = getNoteY(el, elementPositions, staffTopY, clef)
            val fraction = if (lastPos > firstPos) (elX - firstPos) / (lastPos - firstPos) else 0f
            val beamY = firstStemTipY + fraction * (lastStemTipY - firstStemTipY)

            // Draw stem from notehead to beam
            canvas.drawLine(
                elX + stemOffsetX, elY,
                elX + stemOffsetX, beamY,
                rc.stemPaint
            )
        }

        // Draw beams (primary and secondary)
        for (beamLevel in 1..maxBeams) {
            drawBeamLevel(canvas, beamLevel, elements, elementPositions, firstPos, lastPos,
                firstStemTipY, lastStemTipY, stemOffsetX, stemUp)
        }
    }

    private fun drawBeamLevel(
        canvas: Canvas,
        level: Int,
        elements: List<NoteElement>,
        elementPositions: Map<Int, Float>,
        firstX: Float, lastX: Float,
        firstBeamY: Float, lastBeamY: Float,
        stemOffsetX: Float, stemUp: Boolean
    ) {
        // Find groups of consecutive notes with at least 'level' flags
        val eligible = elements.filter { it.duration.base.flagCount() >= level }
        if (eligible.size < 2) return

        val beamOffset = if (stemUp) {
            (level - 1) * (rc.beamThickness + rc.beamSpacing)
        } else {
            -(level - 1) * (rc.beamThickness + rc.beamSpacing)
        }

        // Draw beam from first eligible to last eligible (simplified)
        val startX = (elementPositions[eligible.first().id] ?: return) + stemOffsetX
        val endX = (elementPositions[eligible.last().id] ?: return) + stemOffsetX

        val fraction1 = if (lastX > firstX) (startX - stemOffsetX - firstX) / (lastX - firstX) else 0f
        val fraction2 = if (lastX > firstX) (endX - stemOffsetX - firstX) / (lastX - firstX) else 1f

        val startY = firstBeamY + fraction1 * (lastBeamY - firstBeamY) + beamOffset
        val endY = firstBeamY + fraction2 * (lastBeamY - firstBeamY) + beamOffset

        val path = Path()
        val t = rc.beamThickness
        path.moveTo(startX, startY)
        path.lineTo(endX, endY)
        path.lineTo(endX, endY + t)
        path.lineTo(startX, startY + t)
        path.close()
        canvas.drawPath(path, rc.beamPaint)
    }

    private fun getNoteY(element: NoteElement, positions: Map<Int, Float>,
                          staffTopY: Float, clef: Clef): Float {
        return when (element) {
            is NoteElement.Note -> noteRenderer.pitchToY(element.pitch, staffTopY, clef)
            is NoteElement.Chord -> element.pitches.map {
                noteRenderer.pitchToY(it, staffTopY, clef)
            }.average().toFloat()
            is NoteElement.Rest -> staffTopY + 2 * rc.space
        }
    }
}
