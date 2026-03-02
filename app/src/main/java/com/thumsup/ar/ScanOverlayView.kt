package com.thumsup.ar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class ScanOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80000000")
        style = Paint.Style.FILL
    }

    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val frameStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 2f
    }

    private val frameRect = RectF()

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)

        val frameWidth = width * 0.72f
        val frameHeight = min(height * 0.36f, frameWidth)
        val left = (width - frameWidth) / 2f
        val top = height * 0.22f
        val right = left + frameWidth
        val bottom = top + frameHeight
        val radius = resources.displayMetrics.density * 18f

        frameRect.set(left, top, right, bottom)
        canvas.drawRoundRect(frameRect, radius, radius, clearPaint)
        canvas.drawRoundRect(frameRect, radius, radius, frameStrokePaint)
    }
}
