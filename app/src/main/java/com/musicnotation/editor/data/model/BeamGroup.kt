package com.musicnotation.editor.data.model

data class BeamGroup(
    val id: Int,
    val elementIds: MutableList<Int> = mutableListOf(),
    val stemDirectionOverride: StemDirection = StemDirection.AUTO
)

private var _nextBeamId = 1
fun nextBeamGroupId(): Int = _nextBeamId++
