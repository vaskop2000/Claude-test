package com.musicnotation.editor.rendering

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.musicnotation.editor.data.model.Clef

class ClefRenderer(private val rc: RenderContext) {

    /**
     * Draws the clef symbol at the given position.
     * @param x left edge of clef
     * @param staffTopY top of the staff (line 0)
     */
    fun draw(canvas: Canvas, clef: Clef, x: Float, staffTopY: Float) {
        when (clef) {
            Clef.TREBLE -> drawTrebleClef(canvas, x, staffTopY)
            Clef.BASS -> drawBassClef(canvas, x, staffTopY)
            Clef.ALTO -> drawAltoClef(canvas, x, staffTopY)
            Clef.TENOR -> drawTenorClef(canvas, x, staffTopY)
        }
    }

    private fun drawTrebleClef(canvas: Canvas, x: Float, staffTopY: Float) {
        // Treble clef: G clef, anchor on second line from bottom (line index 1)
        // Use Unicode music symbol scaled to staff
        val paint = Paint(rc.musicSymbolPaint).apply {
            textSize = rc.staffHeight * 2.2f
        }
        val anchorY = staffTopY + 3 * rc.space  // second line from bottom
        canvas.drawText("𝄞", x + rc.clefWidth / 2, anchorY + rc.staffHeight * 0.7f, paint)
    }

    private fun drawBassClef(canvas: Canvas, x: Float, staffTopY: Float) {
        val paint = Paint(rc.musicSymbolPaint).apply {
            textSize = rc.staffHeight * 1.5f
        }
        val anchorY = staffTopY + 1 * rc.space  // fourth line from bottom
        canvas.drawText("𝄢", x + rc.clefWidth / 2, anchorY + rc.staffHeight * 0.75f, paint)
    }

    private fun drawAltoClef(canvas: Canvas, x: Float, staffTopY: Float) {
        drawCClef(canvas, x, staffTopY, anchorLine = 2) // middle line
    }

    private fun drawTenorClef(canvas: Canvas, x: Float, staffTopY: Float) {
        drawCClef(canvas, x, staffTopY, anchorLine = 3) // fourth line
    }

    private fun drawCClef(canvas: Canvas, x: Float, staffTopY: Float, anchorLine: Int) {
        // C clef drawn as two rectangles + vertical bar (simplified)
        val centerY = staffTopY + anchorLine * rc.space
        val halfH = rc.space
        val w = rc.clefWidth * 0.8f
        val barX = x + rc.dp(4f)

        // Vertical bar
        rc.noteFilledPaint.strokeWidth = rc.dp(2.5f)
        canvas.drawRect(barX, centerY - halfH, barX + rc.dp(3f), centerY + halfH, rc.noteFilledPaint)

        // Upper bracket
        drawCBracket(canvas, barX + rc.dp(3f), centerY - halfH, w - rc.dp(3f), halfH, true)
        // Lower bracket
        drawCBracket(canvas, barX + rc.dp(3f), centerY, w - rc.dp(3f), halfH, false)
    }

    private fun drawCBracket(canvas: Canvas, startX: Float, startY: Float,
                              width: Float, height: Float, upper: Boolean) {
        val path = Path()
        val sign = if (upper) -1f else 1f
        path.moveTo(startX, startY)
        path.lineTo(startX + width, startY)
        path.lineTo(startX + width, startY + sign * height * 0.5f)
        path.lineTo(startX + width * 0.3f, startY + sign * height)
        canvas.drawPath(path, rc.noteOutlinePaint)
    }
}
