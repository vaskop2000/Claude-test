package com.musicnotation.editor.data.model

data class Staff(
    val id: Int,
    val instrument: Instrument,
    var clef: Clef = Clef.TREBLE,
    var keySignature: KeySignature = KeySignature(0),
    var timeSignature: TimeSignature = TimeSignature.COMMON,
    val measures: MutableList<Measure> = mutableListOf()
) {
    val displayName: String get() = instrument.displayName

    fun addMeasure(): Measure {
        val measure = Measure(index = measures.size)
        measures.add(measure)
        return measure
    }

    fun removeMeasure(index: Int): Boolean {
        if (index < 0 || index >= measures.size) return false
        measures.removeAt(index)
        // Re-index remaining measures
        measures.forEachIndexed { i, m ->
            measures[i] = m.copy(index = i)
        }
        return true
    }

    fun getMeasure(index: Int): Measure? = measures.getOrNull(index)
}
