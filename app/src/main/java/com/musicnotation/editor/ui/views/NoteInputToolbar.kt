package com.musicnotation.editor.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ToggleButton
import com.musicnotation.editor.R
import com.musicnotation.editor.data.model.*

class NoteInputToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {

    var onDurationSelected: ((Duration) -> Unit)? = null
    var onDotToggled: ((Boolean) -> Unit)? = null
    var onAccidentalSelected: ((Accidental) -> Unit)? = null
    var onRestToggled: ((Boolean) -> Unit)? = null
    var onStemDirectionChanged: ((StemDirection) -> Unit)? = null
    var onBeamToggled: ((Boolean) -> Unit)? = null
    var onTupletSelected: ((TupletRatio?) -> Unit)? = null

    private var currentDuration = Duration.QUARTER
    private var isDotted = false
    private var currentAccidental = Accidental.NONE
    private var isRestMode = false
    private var stemDirection = StemDirection.AUTO
    private var isBeaming = false
    private var activeTuplet: TupletRatio? = null

    init {
        orientation = HORIZONTAL
        LayoutInflater.from(context).inflate(R.layout.view_note_toolbar, this, true)
        setupButtons()
    }

    private fun setupButtons() {
        // Duration buttons
        setupDurationButton(R.id.btnWhole, Duration.WHOLE)
        setupDurationButton(R.id.btnHalf, Duration.HALF)
        setupDurationButton(R.id.btnQuarter, Duration.QUARTER)
        setupDurationButton(R.id.btnEighth, Duration.EIGHTH)
        setupDurationButton(R.id.btnSixteenth, Duration.SIXTEENTH)
        setupDurationButton(R.id.btnThirtySecond, Duration.THIRTY_SECOND)

        // Dot
        findViewById<ToggleButton>(R.id.btnDot).setOnCheckedChangeListener { _, checked ->
            isDotted = checked
            onDotToggled?.invoke(checked)
        }

        // Accidentals
        findViewById<Button>(R.id.btnSharp).setOnClickListener {
            val acc = if (currentAccidental == Accidental.SHARP) Accidental.NONE else Accidental.SHARP
            currentAccidental = acc
            onAccidentalSelected?.invoke(acc)
            updateAccidentalHighlights()
        }
        findViewById<Button>(R.id.btnFlat).setOnClickListener {
            val acc = if (currentAccidental == Accidental.FLAT) Accidental.NONE else Accidental.FLAT
            currentAccidental = acc
            onAccidentalSelected?.invoke(acc)
            updateAccidentalHighlights()
        }
        findViewById<Button>(R.id.btnNatural).setOnClickListener {
            val acc = if (currentAccidental == Accidental.NATURAL) Accidental.NONE else Accidental.NATURAL
            currentAccidental = acc
            onAccidentalSelected?.invoke(acc)
            updateAccidentalHighlights()
        }

        // Rest
        findViewById<ToggleButton>(R.id.btnRest).setOnCheckedChangeListener { _, checked ->
            isRestMode = checked
            onRestToggled?.invoke(checked)
        }

        // Stem direction
        findViewById<Button>(R.id.btnStemUp).setOnClickListener {
            val dir = if (stemDirection == StemDirection.UP) StemDirection.AUTO else StemDirection.UP
            stemDirection = dir
            onStemDirectionChanged?.invoke(dir)
            updateStemHighlights()
        }
        findViewById<Button>(R.id.btnStemDown).setOnClickListener {
            val dir = if (stemDirection == StemDirection.DOWN) StemDirection.AUTO else StemDirection.DOWN
            stemDirection = dir
            onStemDirectionChanged?.invoke(dir)
            updateStemHighlights()
        }

        // Beam
        findViewById<ToggleButton>(R.id.btnBeam).setOnCheckedChangeListener { _, checked ->
            isBeaming = checked
            onBeamToggled?.invoke(checked)
        }

        // Tuplets
        setupTupletButton(R.id.btnTriplet, TupletRatio.TRIPLET)
        setupTupletButton(R.id.btnQuintuplet, TupletRatio.QUINTUPLET)
    }

    private fun setupDurationButton(id: Int, duration: Duration) {
        findViewById<Button>(id).setOnClickListener {
            currentDuration = duration
            onDurationSelected?.invoke(duration)
            updateDurationHighlights()
        }
    }

    private fun setupTupletButton(id: Int, ratio: TupletRatio) {
        findViewById<ToggleButton>(id).setOnCheckedChangeListener { _, checked ->
            activeTuplet = if (checked) ratio else null
            onTupletSelected?.invoke(activeTuplet)
        }
    }

    private fun updateDurationHighlights() {
        val durationMap = mapOf(
            R.id.btnWhole to Duration.WHOLE,
            R.id.btnHalf to Duration.HALF,
            R.id.btnQuarter to Duration.QUARTER,
            R.id.btnEighth to Duration.EIGHTH,
            R.id.btnSixteenth to Duration.SIXTEENTH,
            R.id.btnThirtySecond to Duration.THIRTY_SECOND
        )
        durationMap.forEach { (id, dur) ->
            val btn = findViewById<Button>(id)
            btn.isSelected = dur == currentDuration
        }
    }

    private fun updateAccidentalHighlights() {
        findViewById<Button>(R.id.btnSharp).isSelected = currentAccidental == Accidental.SHARP
        findViewById<Button>(R.id.btnFlat).isSelected = currentAccidental == Accidental.FLAT
        findViewById<Button>(R.id.btnNatural).isSelected = currentAccidental == Accidental.NATURAL
    }

    private fun updateStemHighlights() {
        findViewById<Button>(R.id.btnStemUp).isSelected = stemDirection == StemDirection.UP
        findViewById<Button>(R.id.btnStemDown).isSelected = stemDirection == StemDirection.DOWN
    }

    fun updateFromInputState(state: com.musicnotation.editor.ui.editor.InputState) {
        currentDuration = state.duration.base
        isDotted = state.duration.dotted
        currentAccidental = state.accidental
        isRestMode = state.isRestMode
        stemDirection = state.stemDirection
        isBeaming = state.isBeaming

        updateDurationHighlights()
        updateAccidentalHighlights()
        updateStemHighlights()

        findViewById<ToggleButton>(R.id.btnDot).isChecked = isDotted
        findViewById<ToggleButton>(R.id.btnRest).isChecked = isRestMode
        findViewById<ToggleButton>(R.id.btnBeam).isChecked = isBeaming
    }
}
