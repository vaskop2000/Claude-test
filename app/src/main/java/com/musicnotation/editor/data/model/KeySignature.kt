package com.musicnotation.editor.data.model

enum class KeyMode(val displayName: String) {
    MAJOR("Мажор"),
    MINOR("Минор")
}

data class KeySignature(
    val fifths: Int,    // negative = flats, positive = sharps, 0 = C major
    val mode: KeyMode = KeyMode.MAJOR
) {
    val displayName: String get() {
        return when {
            fifths == 0 -> "До мажор / Ля минор"
            fifths > 0 -> "$fifths♯ ${getMajorKey(fifths)} / ${getMinorKey(fifths)}"
            else -> "${-fifths}♭ ${getMajorKey(fifths)} / ${getMinorKey(fifths)}"
        }
    }

    val accidentalPitches: List<Pair<PitchStep, Accidental>> get() {
        return if (fifths > 0) {
            SHARP_ORDER.take(fifths).map { Pair(it, Accidental.SHARP) }
        } else {
            FLAT_ORDER.take(-fifths).map { Pair(it, Accidental.FLAT) }
        }
    }

    companion object {
        val SHARP_ORDER = listOf(PitchStep.F, PitchStep.C, PitchStep.G, PitchStep.D,
            PitchStep.A, PitchStep.E, PitchStep.B)
        val FLAT_ORDER = listOf(PitchStep.B, PitchStep.E, PitchStep.A, PitchStep.D,
            PitchStep.G, PitchStep.C, PitchStep.F)

        fun getMajorKey(fifths: Int): String = when (fifths) {
            0 -> "До"; 1 -> "Соль"; 2 -> "Ре"; 3 -> "Ля"; 4 -> "Ми"
            5 -> "Си"; 6 -> "Фа♯"; 7 -> "До♯"
            -1 -> "Фа"; -2 -> "Си♭"; -3 -> "Ми♭"; -4 -> "Ля♭"
            -5 -> "Ре♭"; -6 -> "Соль♭"; -7 -> "До♭"
            else -> "?"
        }

        fun getMinorKey(fifths: Int): String = when (fifths) {
            0 -> "Ля"; 1 -> "Ми"; 2 -> "Си"; 3 -> "Фа♯"; 4 -> "До♯"
            5 -> "Соль♯"; 6 -> "Ре♯"; 7 -> "Ля♯"
            -1 -> "Ре"; -2 -> "Соль"; -3 -> "До"; -4 -> "Фа"
            -5 -> "Си♭"; -6 -> "Ми♭"; -7 -> "Ля♭"
            else -> "?"
        }
    }
}
