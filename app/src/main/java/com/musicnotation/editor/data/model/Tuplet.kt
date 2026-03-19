package com.musicnotation.editor.data.model

enum class TupletRatio(
    val actual: Int,    // how many notes are played
    val normal: Int,    // in the time of how many
    val displayName: String
) {
    TRIPLET(3, 2, "3"),
    QUINTUPLET(5, 4, "5"),
    SEXTUPLET(6, 4, "6"),
    SEPTUPLET(7, 4, "7"),
    DUPLET(2, 3, "2"),
    QUADRUPLET(4, 3, "4");

    val scaleFactor: Float get() = normal.toFloat() / actual.toFloat()
}

data class Tuplet(
    val id: Int,
    val ratio: TupletRatio,
    val elementIds: MutableList<Int> = mutableListOf(),
    val showBracket: Boolean = true,
    val showNumber: Boolean = true
) {
    fun scaledTicks(baseTicks: Int): Int = (baseTicks * ratio.scaleFactor).toInt()
}

private var _nextTupletId = 1
fun nextTupletId(): Int = _nextTupletId++
