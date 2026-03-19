package com.musicnotation.editor.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.musicnotation.editor.R
import com.musicnotation.editor.data.model.*

class StaffPropertiesDialog : DialogFragment() {

    interface Listener {
        fun onStaffPropertiesUpdated(
            staffIndex: Int,
            clef: Clef,
            keySignature: KeySignature,
            timeSignature: TimeSignature
        )
    }

    var listener: Listener? = null
    private var staffIndex: Int = 0
    private var currentClef: Clef = Clef.TREBLE
    private var currentFifths: Int = 0
    private var currentTimeSig: TimeSignature = TimeSignature.COMMON

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_staff_properties, null)

        val spinnerClef = view.findViewById<Spinner>(R.id.spinnerClef)
        val spinnerKeySig = view.findViewById<Spinner>(R.id.spinnerKeySig)
        val spinnerTimeSig = view.findViewById<Spinner>(R.id.spinnerTimeSig)

        // Clef spinner
        val clefNames = Clef.values().map { it.displayName }
        spinnerClef.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, clefNames).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerClef.setSelection(Clef.values().indexOf(currentClef))
        spinnerClef.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                currentClef = Clef.values()[pos]
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        // Key signature spinner
        val keyOptions = (-7..7).map { fifths ->
            when {
                fifths == 0 -> "До мажор / Ля минор (0)"
                fifths > 0 -> "${KeySignature.getMajorKey(fifths)} мажор ($fifths♯)"
                else -> "${KeySignature.getMajorKey(fifths)} мажор (${-fifths}♭)"
            }
        }
        spinnerKeySig.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, keyOptions).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerKeySig.setSelection(currentFifths + 7)
        spinnerKeySig.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                currentFifths = pos - 7
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        // Time signature spinner
        val timeOptions = TimeSignature.PRESETS.map { it.displayName }
        spinnerTimeSig.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, timeOptions).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        val timeSigIndex = TimeSignature.PRESETS.indexOf(currentTimeSig).takeIf { it >= 0 } ?: 0
        spinnerTimeSig.setSelection(timeSigIndex)
        spinnerTimeSig.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                currentTimeSig = TimeSignature.PRESETS[pos]
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.staff_properties)
            .setView(view)
            .setPositiveButton(R.string.apply) { _, _ ->
                listener?.onStaffPropertiesUpdated(
                    staffIndex = staffIndex,
                    clef = currentClef,
                    keySignature = KeySignature(currentFifths),
                    timeSignature = currentTimeSig
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
    }

    companion object {
        fun newInstance(staffIndex: Int, clef: Clef, keySig: KeySignature, timeSig: TimeSignature): StaffPropertiesDialog {
            return StaffPropertiesDialog().also { d ->
                d.staffIndex = staffIndex
                d.currentClef = clef
                d.currentFifths = keySig.fifths
                d.currentTimeSig = timeSig
            }
        }
    }
}
