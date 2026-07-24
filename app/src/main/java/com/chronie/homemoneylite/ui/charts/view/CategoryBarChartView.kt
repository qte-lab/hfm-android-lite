package com.chronie.homemoneylite.ui.charts.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.ui.charts.CategoryChartData
import com.chronie.homemoneylite.ui.expense.ExpenseTypeLocalizer
import java.text.NumberFormat
import java.util.Locale

/**
 * 分类柱状图（原 Compose CategoryBarChart / ChartsScreen 行 595–694 机械移植）。
 *
 * 与 LineChartView 相同：onDraw 内对 canvas 套 density 缩放，Paint/Path 为字段。
 */
class CategoryBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val seriesColors = intArrayOf(
        R.color.chart_series_1, R.color.chart_series_2, R.color.chart_series_3,
        R.color.chart_series_4, R.color.chart_series_5, R.color.chart_series_6,
        R.color.chart_series_7, R.color.chart_series_8, R.color.chart_series_9,
        R.color.chart_series_10
    )

    private var categoryData: List<CategoryChartData> = emptyList()
    private var currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())

    fun setData(data: List<CategoryChartData>, format: NumberFormat) {
        categoryData = data
        currencyFormat = format
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val data = categoryData
        if (data.isEmpty()) return

        val maxAmount = data.maxOfOrNull { it.amount } ?: 0.0
        if (maxAmount == 0.0) return

        val d = resources.displayMetrics.density
        val w = width / d
        val h = height / d

        val text = ContextCompat.getColor(context, R.color.text_primary)
        val grid = ContextCompat.getColor(context, R.color.divider)

        val paddingLeft = 24f
        val paddingRight = 24f
        val paddingTop = 50f
        val paddingBottom = 80f
        val chartWidth = w - paddingLeft - paddingRight
        val chartHeight = h - paddingTop - paddingBottom

        canvas.save()
        canvas.scale(d, d)

        paint.reset()
        paint.isAntiAlias = true

        // 水平网格线
        val ySteps = 4
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = grid
        for (i in 0..ySteps) {
            val y = paddingTop + (chartHeight / ySteps) * i
            canvas.drawLine(paddingLeft, y, w - paddingRight, y, paint)
        }

        val barCount = data.size
        val slotWidth = chartWidth / barCount
        val barWidth = (slotWidth * 0.6f).coerceAtMost(90f)

        paint.textAlign = Paint.Align.CENTER

        data.forEachIndexed { index, category ->
            val slotCenterX = paddingLeft + slotWidth * index + slotWidth / 2
            val barHeight = (category.amount / maxAmount).toFloat() * chartHeight
            val topY = paddingTop + (chartHeight - barHeight)
            val leftX = slotCenterX - barWidth / 2
            val barColor = ContextCompat.getColor(context, seriesColors[index % seriesColors.size])

            // 柱子
            if (barHeight > 0) {
                paint.style = Paint.Style.FILL
                paint.color = barColor
                canvas.drawRoundRect(leftX, topY, leftX + barWidth, topY + barHeight, 8f, 8f, paint)
            }

            // 顶部数值
            val valueText = String.format("%.0f", category.amount)
            paint.style = Paint.Style.FILL
            paint.color = barColor
            paint.textSize = 12f
            canvas.drawText(valueText, slotCenterX, topY - 12f, paint)

            // 类别名（超长截断）
            val name = ExpenseTypeLocalizer.getLocalizedTypeName(context, category.type)
            val shortName = if (name.length > 6) name.take(6) else name
            paint.color = text
            paint.textSize = 13f
            canvas.drawText(shortName, slotCenterX, h - paddingBottom + 38f, paint)

            // 占比
            val pctText = String.format("%.0f%%", category.percentage)
            paint.color = ColorUtils.setAlphaComponent(text, 179)
            paint.textSize = 11f
            canvas.drawText(pctText, slotCenterX, h - paddingBottom + 62f, paint)
        }

        canvas.restore()
    }
}
