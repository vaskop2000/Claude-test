package com.musicnotation.editor.rendering

import android.graphics.Canvas
import com.musicnotation.editor.data.model.Clef
import com.musicnotation.editor.data.model.KeySignature
import com.musicnotation.editor.data.model.PitchStep

class KeySigRenderer(private val rc: RenderContext) {

    fun draw(canvas: Canvas, keySig: KeySignature, clef: Clef, x: Float, staffTopY: Float) {
        if (keySig.fifths == 0) return
        val accidentals = keySig.accidentalPitches
        var currentX = x

        accidentals.forEach { (step, accidental) ->
            val staffPos = getAccidentalStaffPosition(step, clef, keySig.fifths > 0)
            val y = staffTopY + rc.staffHeight / 2 - staffPos * rc.space / 2
            val symbol = accidental.symbol

            canvas.drawText(symbol, currentX + rc.keySigAccidentalWidth / 2,
                y + rc.space * 1.2f, rc.accidentalPaint)
            currentX += rc.keySigAccidentalWidth
        }
    }

    /**
     * Returns staff position of the accidental sign for a given pitch step.
     * Positive = above middle line, negative = below.
     */
    private fun getAccidentalStaffPosition(step: PitchStep, clef: Clef, sharps: Boolean): Int {
        // Octave-adjusted positions for conventional sharp/flat placement
        return when (clef) {
            Clef.TREBLE -> TREBLE_POSITIONS[step] ?: 0
            Clef.BASS -> BASS_POSITIONS[step] ?: 0
            Clef.ALTO -> ALTO_POSITIONS[step] ?: 0
            Clef.TENOR -> TENOR_POSITIONS[step] ?: 0
        }
    }

    companion object {
        // Staff position offsets (in half-spaces from middle line) for each clef
        private val TREBLE_POSITIONS = mapOf(
            PitchStep.F to 4, PitchStep.C to 1, PitchStep.G to 5,
            PitchStep.D to 2, PitchStep.A to -1, PitchStep.E to 3, PitchStep.B to 0
        )
        private val BASS_POSITIONS = mapOf(
            PitchStep.F to 2, PitchStep.C to -1, PitchStep.G to 3,
            PitchStep.D to 0, PitchStep.A to -3, PitchStep.E to 1, PitchStep.B to -2
        )
        private val ALTO_POSITIONS = mapOf(
            PitchStep.F to 3, PitchStep.C to 0, PitchStep.G to 4,
            PitchStep.D to 1, PitchStep.A to -2, PitchStep.E to 2, PitchStep.B to -1
        )
        private val TENOR_POSITIONS = mapOf(
            PitchStep.F to 2, PitchStep.C to -1, PitchStep.G to 3,
            PitchStep.D to 0, PitchStep.A to -3, PitchStep.E to 1, PitchStep.B to -2
        )
    }
}
