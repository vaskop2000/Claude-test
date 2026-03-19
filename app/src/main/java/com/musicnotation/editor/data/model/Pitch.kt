package com.musicnotation.editor.data.model

enum class PitchStep(val displayName: String) {
    C("До"), D("Ре"), E("Ми"), F("Фа"), G("Соль"), A("Ля"), B("Си")
}

enum class Accidental(val semitones: Int, val symbol: String) {
    SHARP(1, "♯"),
    FLAT(-1, "♭"),
    NATURAL(0, "♮"),
    DOUBLE_SHARP(2, "𝄪"),
    DOUBLE_FLAT(-2, "𝄫"),
    NONE(0, "")
}

data class Pitch(
    val step: PitchStep,
    val octave: Int,          // octave number (4 = middle octave, C4 = middle C)
    val accidental: Accidental = Accidental.NONE
) {
    // Midi note number for comparison (C4 = 60)
    val midiNote: Int get() {
        val base = when (step) {
            PitchStep.C -> 0; PitchStep.D -> 2; PitchStep.E -> 4
            PitchStep.F -> 5; PitchStep.G -> 7; PitchStep.A -> 9; PitchStep.B -> 11
        }
        return (octave + 1) * 12 + base + accidental.semitones
    }

    // Staff line position: 0 = middle line (B4 treble), positive = higher
    // Used for rendering (each unit = half space)
    fun staffPosition(clef: Clef): Int {
        val diatonicPos = when (step) {
            PitchStep.C -> 0; PitchStep.D -> 1; PitchStep.E -> 2
            PitchStep.F -> 3; PitchStep.G -> 4; PitchStep.A -> 5; PitchStep.B -> 6
        }
        val absolutePos = octave * 7 + diatonicPos
        val clefMiddleLine = clef.middleLineAbsolutePos()
        return absolutePos - clefMiddleLine
    }

    override fun toString() = "${step.name}${accidental.symbol}$octave"
}

fun Clef.middleLineAbsolutePos(): Int = when (this) {
    Clef.TREBLE -> 34  // B4
    Clef.BASS -> 27    // D3
    Clef.ALTO -> 31    // C4
    Clef.TENOR -> 30   // A3
}
