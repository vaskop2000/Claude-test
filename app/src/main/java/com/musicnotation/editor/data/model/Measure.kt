package com.musicnotation.editor.data.model

data class Measure(
    val index: Int,
    val elements: MutableList<NoteElement> = mutableListOf(),
    val beamGroups: MutableList<BeamGroup> = mutableListOf(),
    val tuplets: MutableList<Tuplet> = mutableListOf(),
    var timeSignatureOverride: TimeSignature? = null  // null = use staff default
) {
    fun totalTicks(): Int = elements.sumOf { it.duration.ticks }

    fun isComplete(timeSignature: TimeSignature): Boolean =
        totalTicks() >= timeSignature.ticksPerMeasure

    fun remainingTicks(timeSignature: TimeSignature): Int =
        timeSignature.ticksPerMeasure - totalTicks()

    fun insertElement(element: NoteElement): Boolean {
        elements.add(element)
        recalculateOffsets()
        return true
    }

    fun removeElement(id: Int): Boolean {
        val removed = elements.removeIf { it.id == id }
        if (removed) recalculateOffsets()
        return removed
    }

    fun getBeamGroupById(id: Int): BeamGroup? = beamGroups.find { it.id == id }
    fun getTupletById(id: Int): Tuplet? = tuplets.find { it.id == id }

    private fun recalculateOffsets() {
        var currentTick = 0
        val recalculated = elements.map { element ->
            val offset = currentTick
            currentTick += element.duration.ticks
            when (element) {
                is NoteElement.Note -> element.copy(tickOffset = offset)
                is NoteElement.Rest -> element.copy(tickOffset = offset)
                is NoteElement.Chord -> element.copy(tickOffset = offset)
            }
        }
        elements.clear()
        elements.addAll(recalculated)
    }
}
