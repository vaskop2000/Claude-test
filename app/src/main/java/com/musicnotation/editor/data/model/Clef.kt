package com.musicnotation.editor.data.model

enum class Clef(val displayName: String) {
    TREBLE("Скрипичный"),
    BASS("Басовый"),
    ALTO("Альтовый"),
    TENOR("Теноровый");

    // Which staff line the clef is anchored to (0 = bottom line, 4 = top line)
    fun anchorLine(): Int = when (this) {
        TREBLE -> 1   // G clef on second line from bottom
        BASS -> 3     // F clef on fourth line from bottom
        ALTO -> 2     // C clef on third line (middle)
        TENOR -> 3    // C clef on fourth line
    }

    // The pitch at the middle staff line
    fun middleLinePitch(): String = when (this) {
        TREBLE -> "B4"
        BASS -> "D3"
        ALTO -> "C4"
        TENOR -> "A3"
    }
}
