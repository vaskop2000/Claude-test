package com.musicnotation.editor.data.model

data class TimeSignature(
    val numerator: Int,
    val denominator: Int
) {
    val ticksPerMeasure: Int get() {
        val denominatorDuration = when (denominator) {
            1 -> Duration.WHOLE
            2 -> Duration.HALF
            4 -> Duration.QUARTER
            8 -> Duration.EIGHTH
            16 -> Duration.SIXTEENTH
            else -> Duration.QUARTER
        }
        return numerator * denominatorDuration.ticks
    }

    val displayName: String get() = "$numerator/$denominator"

    companion object {
        val COMMON = TimeSignature(4, 4)
        val CUT = TimeSignature(2, 2)
        val THREE_FOUR = TimeSignature(3, 4)
        val SIX_EIGHT = TimeSignature(6, 8)
        val NINE_EIGHT = TimeSignature(9, 8)
        val TWELVE_EIGHT = TimeSignature(12, 8)
        val TWO_FOUR = TimeSignature(2, 4)
        val FIVE_FOUR = TimeSignature(5, 4)
        val SEVEN_EIGHT = TimeSignature(7, 8)

        val PRESETS = listOf(COMMON, CUT, THREE_FOUR, SIX_EIGHT, NINE_EIGHT,
            TWELVE_EIGHT, TWO_FOUR, FIVE_FOUR, SEVEN_EIGHT)
    }
}
