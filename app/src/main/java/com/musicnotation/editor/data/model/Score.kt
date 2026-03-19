package com.musicnotation.editor.data.model

data class Score(
    val title: String = "Без названия",
    val composer: String = "",
    val staves: MutableList<Staff> = mutableListOf(),
    var measureCount: Int = 4
) {
    fun addStaff(instrument: Instrument, clef: Clef? = null): Staff {
        val staff = Staff(
            id = staves.size,
            instrument = instrument,
            clef = clef ?: instrument.defaultClef
        )
        // Fill with empty measures
        repeat(measureCount) { staff.addMeasure() }
        staves.add(staff)
        return staff
    }

    fun removeStaff(index: Int): Boolean {
        if (index < 0 || index >= staves.size) return false
        staves.removeAt(index)
        return true
    }

    fun addMeasureToAll() {
        measureCount++
        staves.forEach { it.addMeasure() }
    }

    fun removeMeasureFromAll(index: Int): Boolean {
        if (index < 0 || index >= measureCount) return false
        staves.forEach { it.removeMeasure(index) }
        measureCount--
        return true
    }

    companion object {
        fun createDefault(): Score {
            val score = Score(title = "Без названия", measureCount = 4)
            score.addStaff(Instrument.PIANO_TREBLE)
            score.addStaff(Instrument.PIANO_BASS)
            return score
        }
    }
}
