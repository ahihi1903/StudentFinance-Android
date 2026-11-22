package vn.studentfinance.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import vn.studentfinance.R

class SpendingChart(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    val palette = listOf(R.color.teal, R.color.sky, R.color.ochre, R.color.violet, R.color.red)
    var slices: List<Slice> = emptyList()
        set(value) { field = value; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val diameter = minOf(width, height).toFloat()
        paint.strokeWidth = diameter * 0.18f
        val radius = (diameter - paint.strokeWidth) / 2f
        val x = width / 2f
        val y = height / 2f
        paint.color = ContextCompat.getColor(context, R.color.line)
        canvas.drawCircle(x, y, radius, paint)
        var start = -90f
        slices.forEachIndexed { i, slice ->
            paint.color = ContextCompat.getColor(context, palette[i % palette.size])
            val sweep = slice.millionths * (360f / 1_000_000)
            canvas.drawArc(x-radius, y-radius, x+radius, y+radius, start, sweep, false, paint)
            start += sweep
        }
    }
}
