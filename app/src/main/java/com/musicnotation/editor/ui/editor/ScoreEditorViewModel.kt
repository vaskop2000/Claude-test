package com.musicnotation.editor.ui.editor

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.musicnotation.editor.data.model.*

data class InputState(
    val duration: DurationValue = DurationValue(Duration.QUARTER),
    val accidental: Accidental = Accidental.NONE,
    val isRestMode: Boolean = false,
    val stemDirection: StemDirection = StemDirection.AUTO,
    val isBeaming: Boolean = false,
    val activeBeamGroupId: Int? = null,
    val tupletRatio: TupletRatio? = null,
    val activeTupletId: Int? = null
)

data class SelectionState(
    val staffIndex: Int = -1,
    val measureIndex: Int = -1,
    val elementId: Int = -1
)

class ScoreEditorViewModel : ViewModel() {

    private val _score = MutableLiveData<Score>(Score.createDefault())
    val score: LiveData<Score> get() = _score

    private val _inputState = MutableLiveData<InputState>(InputState())
    val inputState: LiveData<InputState> get() = _inputState

    private val _selection = MutableLiveData<SelectionState>(SelectionState())
    val selection: LiveData<SelectionState> get() = _selection

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    // ---- Score Setup ----

    fun createNewScore(title: String, composer: String, initialMeasures: Int, staves: List<Pair<Instrument, Clef>>) {
        val score = Score(title = title, composer = composer, measureCount = initialMeasures)
        staves.forEach { (instrument, clef) -> score.addStaff(instrument, clef) }
        _score.value = score
    }

    fun updateScoreTitle(title: String) {
        _score.value = _score.value?.copy(title = title)
    }

    // ---- Staff Management ----

    fun addStaff(instrument: Instrument, clef: Clef? = null) {
        val score = _score.value ?: return
        score.addStaff(instrument, clef)
        _score.value = score
    }

    fun removeStaff(index: Int) {
        val score = _score.value ?: return
        score.removeStaff(index)
        _score.value = score
    }

    fun updateStaffProperties(staffIndex: Int, clef: Clef? = null,
                               keySignature: KeySignature? = null,
                               timeSignature: TimeSignature? = null) {
        val score = _score.value ?: return
        val staff = score.staves.getOrNull(staffIndex) ?: return
        clef?.let { staff.clef = it }
        keySignature?.let { staff.keySignature = it }
        timeSignature?.let { staff.timeSignature = it }
        _score.value = score
    }

    // ---- Measure Management ----

    fun addMeasure() {
        val score = _score.value ?: return
        score.addMeasureToAll()
        _score.value = score
    }

    fun removeMeasure(index: Int) {
        val score = _score.value ?: return
        if (!score.removeMeasureFromAll(index)) {
            _errorMessage.value = "Невозможно удалить такт"
        }
        _score.value = score
    }

    // ---- Note Input ----

    fun insertNote(staffIndex: Int, measureIndex: Int, pitch: Pitch) {
        val score = _score.value ?: return
        val staff = score.staves.getOrNull(staffIndex) ?: return
        val measure = staff.getMeasure(measureIndex) ?: return
        val inputState = _inputState.value ?: InputState()

        val element = if (inputState.isRestMode) {
            NoteElement.Rest(
                id = nextElementId(),
                duration = inputState.duration.copy(),
                stemDirection = inputState.stemDirection,
                beamGroupId = inputState.activeBeamGroupId,
                tupletId = inputState.activeTupletId
            )
        } else {
            NoteElement.Note(
                id = nextElementId(),
                pitch = pitch.copy(accidental = inputState.accidental),
                duration = inputState.duration.copy(),
                stemDirection = inputState.stemDirection,
                beamGroupId = inputState.activeBeamGroupId,
                tupletId = inputState.activeTupletId
            )
        }

        // Check if adding to beam group
        inputState.activeBeamGroupId?.let { bgId ->
            val bg = measure.beamGroups.find { it.id == bgId }
                ?: BeamGroup(id = bgId).also { measure.beamGroups.add(it) }
            bg.elementIds.add(element.id)
        }

        // Check if adding to tuplet
        inputState.activeTupletId?.let { tId ->
            val tuplet = measure.tuplets.find { it.id == tId }
                ?: Tuplet(id = tId, ratio = inputState.tupletRatio ?: TupletRatio.TRIPLET)
                    .also { measure.tuplets.add(it) }
            tuplet.elementIds.add(element.id)
        }

        measure.insertElement(element)
        _score.value = score
    }

    fun deleteElement(staffIndex: Int, measureIndex: Int, elementId: Int) {
        val score = _score.value ?: return
        val measure = score.staves.getOrNull(staffIndex)?.getMeasure(measureIndex) ?: return
        measure.removeElement(elementId)
        _score.value = score
    }

    // ---- Input State ----

    fun setDuration(duration: Duration, dotted: Boolean = false) {
        _inputState.value = _inputState.value?.copy(duration = DurationValue(duration, dotted))
    }

    fun toggleDot() {
        val current = _inputState.value ?: return
        _inputState.value = current.copy(
            duration = current.duration.copy(dotted = !current.duration.dotted)
        )
    }

    fun setAccidental(accidental: Accidental) {
        _inputState.value = _inputState.value?.copy(accidental = accidental)
    }

    fun toggleRestMode() {
        _inputState.value = _inputState.value?.let { it.copy(isRestMode = !it.isRestMode) }
    }

    fun setStemDirection(direction: StemDirection) {
        _inputState.value = _inputState.value?.copy(stemDirection = direction)
    }

    fun startBeamGroup() {
        val bgId = nextBeamGroupId()
        _inputState.value = _inputState.value?.copy(isBeaming = true, activeBeamGroupId = bgId)
    }

    fun endBeamGroup() {
        _inputState.value = _inputState.value?.copy(isBeaming = false, activeBeamGroupId = null)
    }

    fun startTuplet(ratio: TupletRatio) {
        val tId = nextTupletId()
        _inputState.value = _inputState.value?.copy(tupletRatio = ratio, activeTupletId = tId)
    }

    fun endTuplet() {
        _inputState.value = _inputState.value?.copy(tupletRatio = null, activeTupletId = null)
    }

    fun setStemDirectionForSelection(direction: StemDirection) {
        val sel = _selection.value ?: return
        if (sel.elementId == -1) return
        val score = _score.value ?: return
        val staff = score.staves.getOrNull(sel.staffIndex) ?: return
        val measure = staff.getMeasure(sel.measureIndex) ?: return
        val idx = measure.elements.indexOfFirst { it.id == sel.elementId }
        if (idx < 0) return
        val updated = when (val e = measure.elements[idx]) {
            is NoteElement.Note -> e.copy(stemDirection = direction)
            is NoteElement.Rest -> e.copy(stemDirection = direction)
            is NoteElement.Chord -> e.copy(stemDirection = direction)
        }
        measure.elements[idx] = updated
        _score.value = score
    }

    fun setSelection(staffIndex: Int, measureIndex: Int, elementId: Int) {
        _selection.value = SelectionState(staffIndex, measureIndex, elementId)
    }

    fun clearSelection() {
        _selection.value = SelectionState()
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
