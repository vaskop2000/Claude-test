package com.musicnotation.editor.rendering

import android.graphics.Canvas

class StaffRenderer(private val rc: RenderContext) {

    fun drawStaffLines(canvas: Canvas, leftX: Float, rightX: Float, topY: Float) {
        for (i in 0..4) {
            val y = topY + i * rc.space
            canvas.drawLine(leftX, y, rightX, y, rc.staffLinePaint)
        }
    }

    fun drawBarLine(canvas: Canvas, x: Float, topY: Float) {
        canvas.drawLine(x, topY, x, topY + rc.staffHeight, rc.barLinePaint)
    }

    fun drawDoubleBarLine(canvas: Canvas, x: Float, topY: Float) {
        val offset = rc.dp(3f)
        canvas.drawLine(x, topY, x, topY + rc.staffHeight, rc.barLinePaint)
        canvas.drawLine(x + offset, topY, x + offset, topY + rc.staffHeight, rc.barLinePaint)
    }

    fun drawFinalBarLine(canvas: Canvas, x: Float, topY: Float) {
        val thinX = x - rc.dp(4f)
        // Thin line
        canvas.drawLine(thinX, topY, thinX, topY + rc.staffHeight, rc.barLinePaint)
        // Thick line
        val thickPaint = rc.barLinePaint.apply { strokeWidth = rc.dp(4f) }
        canvas.drawLine(x, topY, x, topY + rc.staffHeight, thickPaint)
        rc.barLinePaint.strokeWidth = rc.dp(RenderConstants.BAR_LINE_WIDTH_DP)
    }

    fun drawBrace(canvas: Canvas, x: Float, topY: Float, bottomY: Float) {
        // Draw a simple bracket
        val paint = rc.barLinePaint
        canvas.drawLine(x, topY, x, bottomY, paint)
    }

    fun drawSystemBracket(canvas: Canvas, x: Float, topY: Float, bottomY: Float) {
        rc.barLinePaint.strokeWidth = rc.dp(3f)
        canvas.drawLine(x, topY, x, bottomY, rc.barLinePaint)
        rc.barLinePaint.strokeWidth = rc.dp(RenderConstants.BAR_LINE_WIDTH_DP)
    }
}
