package com.musicnotation.editor.ui.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.musicnotation.editor.data.model.*
import com.musicnotation.editor.databinding.FragmentScoreEditorBinding
import com.musicnotation.editor.ui.dialogs.NewScoreDialog
import com.musicnotation.editor.ui.dialogs.StaffPropertiesDialog
import com.musicnotation.editor.ui.views.NoteInputToolbar

class ScoreEditorFragment : Fragment(),
    NewScoreDialog.Listener,
    StaffPropertiesDialog.Listener {

    private var _binding: FragmentScoreEditorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScoreEditorViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentScoreEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupScoreCanvas()
        setupToolbar()
        observeViewModel()

        // Show new score dialog on first launch
        if (savedInstanceState == null) {
            showNewScoreDialog()
        }
    }

    private fun setupScoreCanvas() {
        binding.scoreCanvasView.onStaffTapped = { staffIndex, measureIndex, tapX, tapY ->
            handleStaffTap(staffIndex, measureIndex, tapX, tapY)
        }
    }

    private fun setupToolbar() {
        val toolbar = binding.noteInputToolbar

        toolbar.onDurationSelected = { duration ->
            viewModel.setDuration(duration, toolbar.isDottedActive())
        }
        toolbar.onDotToggled = { dotted ->
            viewModel.toggleDot()
        }
        toolbar.onAccidentalSelected = { accidental ->
            viewModel.setAccidental(accidental)
        }
        toolbar.onRestToggled = { rest ->
            viewModel.toggleRestMode()
        }
        toolbar.onStemDirectionChanged = { direction ->
            viewModel.setStemDirection(direction)
            // Also apply to selected element
            viewModel.setStemDirectionForSelection(direction)
        }
        toolbar.onBeamToggled = { beaming ->
            if (beaming) viewModel.startBeamGroup() else viewModel.endBeamGroup()
        }
        toolbar.onTupletSelected = { ratio ->
            if (ratio != null) viewModel.startTuplet(ratio) else viewModel.endTuplet()
        }

        // Top action bar buttons
        binding.btnNewScore.setOnClickListener { showNewScoreDialog() }
        binding.btnAddMeasure.setOnClickListener { viewModel.addMeasure() }
        binding.btnRemoveMeasure.setOnClickListener {
            val score = viewModel.score.value ?: return@setOnClickListener
            val lastIndex = score.measureCount - 1
            if (lastIndex >= 1) viewModel.removeMeasure(lastIndex)
            else Toast.makeText(requireContext(), "Минимум один такт", Toast.LENGTH_SHORT).show()
        }
        binding.btnStaffProps.setOnClickListener { showStaffPropertiesDialog(0) }
    }

    private fun observeViewModel() {
        viewModel.score.observe(viewLifecycleOwner) { score ->
            binding.scoreCanvasView.setScore(score)
        }

        viewModel.inputState.observe(viewLifecycleOwner) { state ->
            binding.noteInputToolbar.updateFromInputState(state)
        }

        viewModel.selection.observe(viewLifecycleOwner) { selection ->
            binding.scoreCanvasView.setSelectedElement(selection.elementId)
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun handleStaffTap(staffIndex: Int, measureIndex: Int, tapX: Float, tapY: Float) {
        val score = viewModel.score.value ?: return
        val renderer = binding.scoreCanvasView.getRendererForHitTest()
        val staff = score.staves.getOrNull(staffIndex) ?: return
        val staffTopY = renderer.getStaffTopY(staffIndex) ?: return

        val inputState = viewModel.inputState.value ?: return

        if (inputState.isRestMode) {
            // Insert rest
            val fakePitch = Pitch(PitchStep.B, 4)
            viewModel.insertNote(staffIndex, measureIndex, fakePitch)
        } else {
            // Calculate pitch from tap Y position
            val pitch = renderer.yToPitch(tapY, staffTopY, staff.clef)
            val pitchWithAccidental = pitch.copy(accidental = inputState.accidental)
            viewModel.insertNote(staffIndex, measureIndex, pitchWithAccidental)
        }
    }

    private fun showNewScoreDialog() {
        val dialog = NewScoreDialog.newInstance()
        dialog.listener = this
        dialog.show(parentFragmentManager, "NewScoreDialog")
    }

    private fun showStaffPropertiesDialog(staffIndex: Int) {
        val score = viewModel.score.value ?: return
        val staff = score.staves.getOrNull(staffIndex) ?: return
        val dialog = StaffPropertiesDialog.newInstance(
            staffIndex, staff.clef, staff.keySignature, staff.timeSignature
        )
        dialog.listener = this
        dialog.show(parentFragmentManager, "StaffPropertiesDialog")
    }

    override fun onScoreCreated(
        title: String, composer: String, measureCount: Int,
        staves: List<Pair<Instrument, Clef>>
    ) {
        viewModel.createNewScore(title, composer, measureCount, staves)
    }

    override fun onStaffPropertiesUpdated(
        staffIndex: Int, clef: Clef,
        keySignature: KeySignature, timeSignature: TimeSignature
    ) {
        viewModel.updateStaffProperties(staffIndex, clef, keySignature, timeSignature)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Extension to check if dot button is active
fun NoteInputToolbar.isDottedActive(): Boolean {
    return try {
        val field = this.javaClass.getDeclaredField("isDotted")
        field.isAccessible = true
        field.getBoolean(this)
    } catch (e: Exception) {
        false
    }
}
