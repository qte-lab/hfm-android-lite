package com.chronie.homemoneylite.ui.charts.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.ui.charts.DailyChartData
import java.text.NumberFormat
import java.util.Locale

/**
 * 趋势折线图（原 Compose HighQualityLineChart / ChartsScreen 行 393–540 的机械移植）。
 *
 * 绘制坐标系：Compose DrawScope 以 dp 为单位，本 View 在 onDraw 中对 canvas 套用 density 缩放，
 * 使所有 dp 常量（paddingLeft=80f、paddingRight=40f、paddingTop=60f、paddingBottom=80f、textSize=13f 等）与原实现一致。
 * Paint/Path 全部为字段，onDraw 内不 new 对象。
 */
class LineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val pointList = ArrayList<Pair<Float, Float>>()

    private var dailyData: List<DailyChartData> = emptyList()
    private var currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())

    fun setData(data: List<DailyChartData>, format: NumberFormat) {
        dailyData = data
        currencyFormat = format
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val data = dailyData
        if (data.isEmpty()) return

        val maxAmount = data.maxOfOrNull { it.amount } ?: 0.0
        if (maxAmount == 0.0) return

        val d = resources.displayMetrics.density
        val w = width / d
        val h = height / d

        val primary = ContextCompat.getColor(context, R.color.brand_primary)
        val text = ContextCompat.getColor(context, R.color.text_primary)
        val grid = ContextCompat.getColor(context, R.color.divider)
        val axisColor = ColorUtils.setAlphaComponent(text, 128)

        val paddingLeft = 80f
        val paddingRight = 40f
        val paddingTop = 60f
        val paddingBottom = 80f
        val chartWidth = w - paddingLeft - paddingRight
        val chartHeight = h - paddingTop - paddingBottom

        canvas.save()
        canvas.scale(d, d)

        paint.reset()
        paint.isAntiAlias = true

        // Y 轴网格线 + 标签
        val ySteps = 5
        for (i in 0..ySteps) {
            val y = paddingTop + (chartHeight / ySteps) * i
            val amount = maxAmount * (1 - i.toFloat() / ySteps)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            paint.color = grid
            canvas.drawLine(paddingLeft, y, w - paddingRight, y, paint)

            val label = currencyFormat.format(amount)
            paint.style = Paint.Style.FILL
            paint.color = text
            paint.textAlign = Paint.Align.RIGHT
            paint.textSize = 13f
            canvas.drawText(label, paddingLeft - 10f, y + 6f, paint)
        }

        // 坐标轴
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = axisColor
        canvas.drawLine(paddingLeft, paddingTop, paddingLeft, h - paddingBottom, paint)
        canvas.drawLine(paddingLeft, h - paddingBottom, w - paddingRight, h - paddingBottom, paint)

        // 折线
        path.reset()
        pointList.clear()
        data.forEachIndexed { index, daily ->
            val x = paddingLeft + (index.toFloat() / (data.size - 1).coerceAtLeast(1)) * chartWidth
            val y = h - paddingBottom - (daily.amount.toFloat() / maxAmount.toFloat()) * chartHeight
            pointList.add(Pair(x, y))
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.color = primary
        canvas.drawPath(path, paint)

        // 数据点 + 非零数值标签
        data.forEachIndexed { index, daily ->
            val (x, y) = pointList[index]
            paint.style = Paint.Style.FILL
            paint.color = primary
            canvas.drawCircle(x, y, 6f, paint)
            paint.color = Color.WHITE
            canvas.drawCircle(x, y, 3f, paint)

            if (daily.amount > 0) {
                val valueLabel = String.format("%.0f", daily.amount)
                paint.color = primary
                paint.textAlign = Paint.Align.CENTER
                paint.textSize = 11f
                canvas.drawText(valueLabel, x, y - 16f, paint)
            }
        }

        // X 轴日期标签
        val xLabelStep = (data.size / 7).coerceAtLeast(1)
        paint.color = text
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 13f
        data.forEachIndexed { index, daily ->
            if (index % xLabelStep == 0 || index == data.size - 1) {
                val (x, _) = pointList[index]
                val dateLabel = "${daily.date.monthValue}/${daily.date.dayOfMonth}"
                canvas.drawText(dateLabel, x, h - paddingBottom + 40f, paint)
            }
        }

        canvas.restore()
    }
}
