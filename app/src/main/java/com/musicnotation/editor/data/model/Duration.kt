package com.musicnotation.editor.data.model

enum class Duration(val ticks: Int, val displayName: String) {
    WHOLE(384, "Целая"),
    HALF(192, "Половинная"),
    QUARTER(96, "Четверть"),
    EIGHTH(48, "Восьмая"),
    SIXTEENTH(24, "Шестнадцатая"),
    THIRTY_SECOND(12, "Тридцать вторая");

    fun hasStem(): Boolean = this != WHOLE
    fun hasFlag(): Boolean = this == EIGHTH || this == SIXTEENTH || this == THIRTY_SECOND
    fun flagCount(): Int = when (this) {
        EIGHTH -> 1
        SIXTEENTH -> 2
        THIRTY_SECOND -> 3
        else -> 0
    }
}

data class DurationValue(
    val base: Duration,
    val dotted: Boolean = false
) {
    val ticks: Int get() = if (dotted) base.ticks * 3 / 2 else base.ticks
    val isBeamable: Boolean get() = base.hasFlag()
}
