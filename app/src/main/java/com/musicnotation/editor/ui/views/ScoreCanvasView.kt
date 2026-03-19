package com.musicnotation.editor.ui.views

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.musicnotation.editor.data.model.*
import com.musicnotation.editor.rendering.MusicRenderer

class ScoreCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val musicRenderer = MusicRenderer(context)
    private var score: Score? = null
    private var selectedElementId: Int = -1

    // Scroll state
    private var scrollX = 0f
    private var scrollY = 0f
    private var scaleFactor = 1.0f
    private val minScale = 0.5f
    private val maxScale = 3.0f

    // Callbacks
    var onStaffTapped: ((staffIndex: Int, measureIndex: Int, tapX: Float, tapY: Float) -> Unit)? = null

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            handleTap(e.x, e.y)
            return true
        }

        override fun onScroll(
            e1: MotionEvent?, e2: MotionEvent,
            distanceX: Float, distanceY: Float
        ): Boolean {
            scrollX += distanceX
            scrollY += distanceY
            scrollX = scrollX.coerceAtLeast(0f)
            scrollY = scrollY.coerceAtLeast(0f)
            invalidate()
            return true
        }
    })

    private val scaleGestureDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor *= detector.scaleFactor
                scaleFactor = scaleFactor.coerceIn(minScale, maxScale)
                invalidate()
                return true
            }
        })

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val currentScore = score ?: return

        canvas.save()
        canvas.translate(-scrollX, -scrollY)
        canvas.scale(scaleFactor, scaleFactor)
        musicRenderer.render(canvas, currentScore, selectedElementId)
        canvas.restore()
    }

    private fun handleTap(rawX: Float, rawY: Float) {
        val currentScore = score ?: return

        // Convert screen coordinates to score canvas coordinates
        val canvasX = (rawX + scrollX) / scaleFactor
        val canvasY = (rawY + scrollY) / scaleFactor

        val staffIndex = musicRenderer.hitTestStaff(canvasY)
        if (staffIndex < 0) return

        val measureIndex = musicRenderer.hitTestMeasure(canvasX, staffIndex)
        if (measureIndex < 0) return

        onStaffTapped?.invoke(staffIndex, measureIndex, canvasX, canvasY)
    }

    fun setScore(score: Score) {
        this.score = score
        invalidate()
    }

    fun setSelectedElement(elementId: Int) {
        selectedElementId = elementId
        invalidate()
    }

    fun getRendererForHitTest(): MusicRenderer = musicRenderer

    fun resetView() {
        scrollX = 0f
        scrollY = 0f
        scaleFactor = 1.0f
        invalidate()
    }

    override fun computeHorizontalScrollRange(): Int {
        return (musicRenderer.getLayout()?.totalWidth?.toInt() ?: width) + 200
    }
}
