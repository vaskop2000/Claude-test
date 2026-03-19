package com.musicnotation.editor.rendering

import android.graphics.Canvas
import com.musicnotation.editor.data.model.TimeSignature

class TimeSigRenderer(private val rc: RenderContext) {

    fun draw(canvas: Canvas, timeSig: TimeSignature, x: Float, staffTopY: Float) {
        val centerX = x + rc.timeSigWidth / 2
        val upperY = staffTopY + rc.space * 1.5f       // top number baseline
        val lowerY = staffTopY + rc.space * 3.5f       // bottom number baseline

        canvas.drawText(timeSig.numerator.toString(), centerX, upperY, rc.timeSigPaint)
        canvas.drawText(timeSig.denominator.toString(), centerX, lowerY, rc.timeSigPaint)
    }
}
