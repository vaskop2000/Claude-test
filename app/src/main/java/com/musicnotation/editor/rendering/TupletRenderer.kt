package com.musicnotation.editor.rendering

import android.graphics.Canvas
import android.graphics.Path
import com.musicnotation.editor.data.model.Measure
import com.musicnotation.editor.data.model.NoteElement
import com.musicnotation.editor.data.model.Tuplet

class TupletRenderer(private val rc: RenderContext) {

    fun drawTuplets(
        canvas: Canvas,
        measure: Measure,
        elementPositions: Map<Int, Float>,
        staffTopY: Float
    ) {
        measure.tuplets.forEach { tuplet ->
            drawTuplet(canvas, tuplet, measure, elementPositions, staffTopY)
        }
    }

    private fun drawTuplet(
        canvas: Canvas,
        tuplet: Tuplet,
        measure: Measure,
        elementPositions: Map<Int, Float>,
        staffTopY: Float
    ) {
        val elements = tuplet.elementIds.mapNotNull { id ->
            measure.elements.find { it.id == id }
        }
        if (elements.isEmpty()) return

        val startX = elementPositions[elements.first().id] ?: return
        val endX = elementPositions[elements.last().id] ?: return
        val bracketY = staffTopY - rc.space * 1.5f  // above the staff

        if (tuplet.showBracket) {
            drawBracket(canvas, startX, endX, bracketY)
        }

        if (tuplet.showNumber) {
            val centerX = (startX + endX) / 2f
            canvas.drawText(tuplet.ratio.displayName, centerX, bracketY - rc.dp(2f), rc.tupletPaint)
        }
    }

    private fun drawBracket(canvas: Canvas, startX: Float, endX: Float, y: Float) {
        val tickHeight = rc.dp(5f)
        val path = Path()
        path.moveTo(startX, y + tickHeight)
        path.lineTo(startX, y)
        path.lineTo(endX, y)
        path.lineTo(endX, y + tickHeight)
        canvas.drawPath(path, rc.noteOutlinePaint)
    }
}
