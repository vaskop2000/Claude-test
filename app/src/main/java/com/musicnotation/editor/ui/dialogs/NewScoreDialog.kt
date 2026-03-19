package com.musicnotation.editor.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.musicnotation.editor.R
import com.musicnotation.editor.data.model.Clef
import com.musicnotation.editor.data.model.Instrument
import com.musicnotation.editor.data.model.InstrumentFamily

class NewScoreDialog : DialogFragment() {

    interface Listener {
        fun onScoreCreated(
            title: String,
            composer: String,
            measureCount: Int,
            staves: List<Pair<Instrument, Clef>>
        )
    }

    var listener: Listener? = null

    private val selectedStaves = mutableListOf<Pair<Instrument, Clef>>(
        Pair(Instrument.PIANO_TREBLE, Clef.TREBLE),
        Pair(Instrument.PIANO_BASS, Clef.BASS)
    )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_new_score, null)

        val etTitle = view.findViewById<EditText>(R.id.etTitle)
        val etComposer = view.findViewById<EditText>(R.id.etComposer)
        val etMeasures = view.findViewById<EditText>(R.id.etMeasureCount)
        val llStaves = view.findViewById<LinearLayout>(R.id.llStavesList)
        val btnAddStaff = view.findViewById<Button>(R.id.btnAddStaff)

        etMeasures.setText("4")

        // Build initial staves UI
        refreshStavesList(llStaves)

        btnAddStaff.setOnClickListener {
            showAddStaffDialog { instrument, clef ->
                selectedStaves.add(Pair(instrument, clef))
                refreshStavesList(llStaves)
            }
        }

        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.new_score)
            .setView(view)
            .setPositiveButton(R.string.create) { _, _ ->
                val title = etTitle.text.toString().ifBlank { getString(R.string.untitled) }
                val composer = etComposer.text.toString()
                val measures = etMeasures.text.toString().toIntOrNull()?.coerceIn(1, 99) ?: 4
                listener?.onScoreCreated(title, composer, measures, selectedStaves.toList())
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
    }

    private fun refreshStavesList(container: LinearLayout) {
        container.removeAllViews()
        selectedStaves.forEachIndexed { index, (instrument, clef) ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 4, 0, 4)
            }

            val tv = TextView(requireContext()).apply {
                text = "${index + 1}. ${instrument.displayName} (${clef.displayName})"
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

            val btnRemove = Button(requireContext()).apply {
                text = "✕"
                setOnClickListener {
                    if (selectedStaves.size > 1) {
                        selectedStaves.removeAt(index)
                        refreshStavesList(container)
                    }
                }
            }

            row.addView(tv)
            row.addView(btnRemove)
            container.addView(row)
        }
    }

    private fun showAddStaffDialog(callback: (Instrument, Clef) -> Unit) {
        val instruments = Instrument.values()
        val items = instruments.map { it.displayName }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.select_instrument)
            .setItems(items) { _, which ->
                val instrument = instruments[which]
                callback(instrument, instrument.defaultClef)
            }
            .show()
    }

    companion object {
        fun newInstance(): NewScoreDialog = NewScoreDialog()
    }
}
