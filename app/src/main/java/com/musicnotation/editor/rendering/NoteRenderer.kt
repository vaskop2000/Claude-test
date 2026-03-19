package com.musicnotation.editor.rendering

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.musicnotation.editor.data.model.*

class NoteRenderer(private val rc: RenderContext) {

    /**
     * Returns Y coordinate for a pitch on a staff.
     * staffTopY is top of staff (first line).
     * staffPos = 0 means middle line.
     */
    fun pitchToY(pitch: Pitch, staffTopY: Float, clef: Clef): Float {
        val staffPos = pitch.staffPosition(clef)
        // Middle line = line index 2 (from top) => y = staffTopY + 2 * space
        val middleLineY = staffTopY + 2 * rc.space
        return middleLineY - staffPos * (rc.space / 2f)
    }

    fun drawNote(
        canvas: Canvas, note: NoteElement.Note, x: Float,
        staffTopY: Float, clef: Clef,
        isSelected: Boolean = false
    ) {
        val y = pitchToY(note.pitch, staffTopY, clef)
        drawNoteHead(canvas, x, y, note.duration, isSelected)
        if (note.duration.base.hasStem() && note.beamGroupId == null) {
            drawStem(canvas, x, y, note.duration, note.stemDirection, clef, staffTopY)
        }
        if (note.duration.base.hasFlag() && note.beamGroupId == null) {
            val stemUp = resolveStemUp(note.stemDirection, y, staffTopY)
            drawFlag(canvas, x, y, note.duration.base, stemUp)
        }
        drawLedgerLines(canvas, x, y, staffTopY)
        if (note.duration.dotted) drawDot(canvas, x, y)
        if (note.pitch.accidental != Accidental.NONE) {
            drawAccidental(canvas, x, y, note.pitch.accidental)
        }
        if (isSelected) drawSelection(canvas, x, y)
    }

    fun drawRest(canvas: Canvas, rest: NoteElement.Rest, x: Float, staffTopY: Float) {
        val restY = staffTopY + 2 * rc.space  // rests centered on middle of staff
        drawRestSymbol(canvas, x, restY, staffTopY, rest.duration)
        if (rest.duration.dotted) drawDot(canvas, x + rc.noteHeadW * 0.8f, restY - rc.space)
    }

    fun drawChord(
        canvas: Canvas, chord: NoteElement.Chord, x: Float,
        staffTopY: Float, clef: Clef, isSelected: Boolean = false
    ) {
        chord.pitches.forEach { pitch ->
            val y = pitchToY(pitch, staffTopY, clef)
            drawNoteHead(canvas, x, y, chord.duration, isSelected)
            drawLedgerLines(canvas, x, y, staffTopY)
            if (pitch.accidental != Accidental.NONE) drawAccidental(canvas, x, y, pitch.accidental)
        }

        // Draw single stem for chord
        if (chord.duration.base.hasStem() && chord.beamGroupId == null) {
            val stemUp = when (chord.stemDirection) {
                StemDirection.UP -> true
                StemDirection.DOWN -> false
                StemDirection.AUTO -> {
                    val lowestY = chord.pitches.maxOfOrNull { pitchToY(it, staffTopY, clef) } ?: return
                    val highestY = chord.pitches.minOfOrNull { pitchToY(it, staffTopY, clef) } ?: return
                    val middleY = staffTopY + 2 * rc.space
                    (lowestY + highestY) / 2 > middleY
                }
            }

            val stemNoteY = if (stemUp) {
                chord.pitches.minOfOrNull { pitchToY(it, staffTopY, clef) } ?: return
            } else {
                chord.pitches.maxOfOrNull { pitchToY(it, staffTopY, clef) } ?: return
            }

            if (stemUp) {
                canvas.drawLine(x + rc.noteHeadW * 0.45f, stemNoteY,
                    x + rc.noteHeadW * 0.45f, stemNoteY - rc.stemLength, rc.stemPaint)
            } else {
                canvas.drawLine(x - rc.noteHeadW * 0.45f, stemNoteY,
                    x - rc.noteHeadW * 0.45f, stemNoteY + rc.stemLength, rc.stemPaint)
            }
        }
        if (isSelected) drawSelection(canvas, x, staffTopY + 2 * rc.space)
    }

    private fun drawNoteHead(canvas: Canvas, x: Float, y: Float, duration: DurationValue, selected: Boolean) {
        val paint = if (selected) {
            Paint(rc.noteFilledPaint).apply { color = RenderConstants.COLOR_SELECTED }
        } else rc.noteFilledPaint

        val rx = rc.noteHeadW / 2
        val ry = rc.noteHeadH / 2
        val rect = RectF(x - rx, y - ry, x + rx, y + ry)

        when (duration.base) {
            Duration.WHOLE -> {
                // Open oval with hole
                canvas.drawOval(rect, rc.noteOutlinePaint)
                // Small inner hole
                val innerRect = RectF(x - rx * 0.45f, y - ry * 0.55f, x + rx * 0.45f, y + ry * 0.55f)
                canvas.drawOval(innerRect, Paint(rc.noteOutlinePaint).apply { color = RenderConstants.COLOR_BACKGROUND })
            }
            Duration.HALF -> {
                // Open oval
                canvas.drawOval(rect, paint)
                val innerRect = RectF(x - rx * 0.55f, y - ry * 0.55f, x + rx * 0.55f, y + ry * 0.55f)
                canvas.drawOval(innerRect, Paint(paint).apply { color = RenderConstants.COLOR_BACKGROUND })
                canvas.drawOval(rect, rc.noteOutlinePaint)
            }
            else -> {
                // Filled oval, slightly tilted
                canvas.save()
                canvas.rotate(-15f, x, y)
                canvas.drawOval(rect, paint)
                canvas.restore()
            }
        }
    }

    private fun drawStem(
        canvas: Canvas, x: Float, y: Float,
        duration: DurationValue, direction: StemDirection,
        clef: Clef, staffTopY: Float
    ) {
        val middleLineY = staffTopY + 2 * rc.space
        val stemUp = resolveStemUp(direction, y, staffTopY)

        if (stemUp) {
            val stemX = x + rc.noteHeadW * 0.45f
            canvas.drawLine(stemX, y, stemX, y - rc.stemLength, rc.stemPaint)
        } else {
            val stemX = x - rc.noteHeadW * 0.45f
            canvas.drawLine(stemX, y, stemX, y + rc.stemLength, rc.stemPaint)
        }
    }

    private fun drawFlag(canvas: Canvas, x: Float, y: Float, duration: Duration, stemUp: Boolean) {
        val flagCount = duration.flagCount()
        if (flagCount == 0) return

        val stemX = if (stemUp) x + rc.noteHeadW * 0.45f else x - rc.noteHeadW * 0.45f
        val stemTipY = if (stemUp) y - rc.stemLength else y + rc.stemLength

        // Draw flag as a curved unicode symbol
        val flagSymbol = if (stemUp) "𝅲" else "𝅳"
        val flagPaint = Paint(rc.musicSymbolPaint).apply {
            textSize = rc.space * 2.5f
            textAlign = if (stemUp) Paint.Align.LEFT else Paint.Align.RIGHT
        }

        for (i in 0 until flagCount) {
            val flagY = if (stemUp) stemTipY + i * rc.space * 0.8f + rc.space
            else stemTipY - i * rc.space * 0.8f - rc.space * 0.5f
            canvas.drawText("⌒", stemX, flagY, flagPaint)
        }
    }

    private fun drawRestSymbol(
        canvas: Canvas, x: Float, y: Float,
        staffTopY: Float, duration: DurationValue
    ) {
        val paint = rc.noteFilledPaint
        when (duration.base) {
            Duration.WHOLE -> {
                // Whole rest: filled rectangle hanging from 4th line
                val lineY = staffTopY + rc.space
                val w = rc.noteHeadW * 1.5f
                val h = rc.space * 0.55f
                canvas.drawRect(x - w / 2, lineY, x + w / 2, lineY + h, paint)
            }
            Duration.HALF -> {
                // Half rest: filled rectangle sitting on 3rd line
                val lineY = staffTopY + 2 * rc.space
                val w = rc.noteHeadW * 1.5f
                val h = rc.space * 0.55f
                canvas.drawRect(x - w / 2, lineY - h, x + w / 2, lineY, paint)
            }
            Duration.QUARTER -> {
                // Quarter rest: zigzag — simplified as text symbol
                canvas.drawText("𝄽", x, y + rc.space, Paint(rc.musicSymbolPaint).apply {
                    textSize = rc.staffHeight * 0.9f
                })
            }
            Duration.EIGHTH -> {
                canvas.drawText("𝄾", x, y + rc.space * 0.5f, Paint(rc.musicSymbolPaint).apply {
                    textSize = rc.staffHeight * 0.7f
                })
            }
            Duration.SIXTEENTH -> {
                canvas.drawText("𝄿", x, y + rc.space * 0.5f, Paint(rc.musicSymbolPaint).apply {
                    textSize = rc.staffHeight * 0.7f
                })
            }
            Duration.THIRTY_SECOND -> {
                canvas.drawText("𝅀", x, y + rc.space * 0.5f, Paint(rc.musicSymbolPaint).apply {
                    textSize = rc.staffHeight * 0.7f
                })
            }
        }
    }

    private fun drawLedgerLines(canvas: Canvas, x: Float, y: Float, staffTopY: Float) {
        val staffBottomY = staffTopY + rc.staffHeight
        val halfSpace = rc.space / 2f
        val lineWidth = rc.noteHeadW + rc.dp(RenderConstants.LEDGER_LINE_EXTRA * 10)

        // Above staff
        var lineY = staffTopY - rc.space
        while (y <= lineY + halfSpace) {
            canvas.drawLine(x - lineWidth / 2, lineY, x + lineWidth / 2, lineY, rc.ledgerPaint)
            lineY -= rc.space
        }

        // Below staff
        lineY = staffBottomY + rc.space
        while (y >= lineY - halfSpace) {
            canvas.drawLine(x - lineWidth / 2, lineY, x + lineWidth / 2, lineY, rc.ledgerPaint)
            lineY += rc.space
        }
    }

    private fun drawDot(canvas: Canvas, x: Float, y: Float) {
        val dotX = x + rc.noteHeadW * 0.7f + rc.dp(4f)
        val dotY = if (y % rc.space < rc.space / 2) y - rc.space / 4 else y
        canvas.drawCircle(dotX, dotY, rc.space * RenderConstants.DOT_RADIUS_RATIO * 2, rc.noteFilledPaint)
    }

    private fun drawAccidental(canvas: Canvas, x: Float, y: Float, accidental: Accidental) {
        val symbol = accidental.symbol
        val accX = x - rc.noteHeadW * 0.6f - rc.dp(4f)
        canvas.drawText(symbol, accX, y + rc.space * 0.4f, rc.accidentalPaint)
    }

    private fun drawSelection(canvas: Canvas, x: Float, y: Float) {
        canvas.drawCircle(x, y, rc.noteHeadW, rc.selectedPaint)
    }

    fun resolveStemUp(direction: StemDirection, noteY: Float, staffTopY: Float): Boolean {
        return when (direction) {
            StemDirection.UP -> true
            StemDirection.DOWN -> false
            StemDirection.AUTO -> noteY > staffTopY + 2 * rc.space  // below middle line -> stem up
        }
    }
}
