package com.musicnotation.editor.data.model

enum class StemDirection {
    UP, DOWN, AUTO
}

sealed class NoteElement {
    abstract val id: Int
    abstract val duration: DurationValue
    abstract val stemDirection: StemDirection
    abstract val beamGroupId: Int?
    abstract val tupletId: Int?
    abstract val tickOffset: Int  // position in ticks within the measure

    data class Note(
        override val id: Int,
        val pitch: Pitch,
        override val duration: DurationValue,
        override val stemDirection: StemDirection = StemDirection.AUTO,
        override val beamGroupId: Int? = null,
        override val tupletId: Int? = null,
        override val tickOffset: Int = 0
    ) : NoteElement()

    data class Rest(
        override val id: Int,
        override val duration: DurationValue,
        override val stemDirection: StemDirection = StemDirection.AUTO,
        override val beamGroupId: Int? = null,
        override val tupletId: Int? = null,
        override val tickOffset: Int = 0
    ) : NoteElement()

    data class Chord(
        override val id: Int,
        val pitches: List<Pitch>,
        override val duration: DurationValue,
        override val stemDirection: StemDirection = StemDirection.AUTO,
        override val beamGroupId: Int? = null,
        override val tupletId: Int? = null,
        override val tickOffset: Int = 0
    ) : NoteElement() {
        val lowestPitch: Pitch? get() = pitches.minByOrNull { it.midiNote }
        val highestPitch: Pitch? get() = pitches.maxByOrNull { it.midiNote }
    }
}

private var _nextId = 1
fun nextElementId(): Int = _nextId++
