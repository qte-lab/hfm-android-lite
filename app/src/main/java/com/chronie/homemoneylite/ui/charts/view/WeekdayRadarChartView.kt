package com.chronie.homemoneylite.ui.charts.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.ui.charts.WeekdayChartData
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.PI

/**
 * 星期分布雷达图（原 Compose WeekdayRadarChart / WeekdayRadarChart.kt 行 100–293 机械移植）。
 *
 * 点击命中检测：原 Compose 用 pointerInput{detectTapGestures} + 记录 labelPositions。
 * 这里改为重写 onTouchEvent，ACTION_UP 时用同一套极坐标数学计算各星期标签圆心（px），
 * 命中半径 40dp，回调 onWeekdayClick。
 */
class WeekdayRadarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    // 命中检测：7 个星期标签圆心(px) + 半径(px)
    private val labelHit = Array(7) { FloatArray(3) }

    private var weekdayData: List<WeekdayChartData> = emptyList()

    var onWeekdayClick: ((WeekdayChartData) -> Unit)? = null

    init {
        isClickable = true
        isFocusable = true
    }

    fun setData(data: List<WeekdayChartData>) {
        weekdayData = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val data = weekdayData
        val maxAmount = data.maxOfOrNull { it.amount } ?: 1.0
        if (data.isEmpty() || maxAmount == 0.0) return

        val d = resources.displayMetrics.density
        val w = width / d
        val h = height / d

        val primary = ContextCompat.getColor(context, R.color.brand_primary)
        val text = ContextCompat.getColor(context, R.color.text_primary)
        val grid = ContextCompat.getColor(context, R.color.divider)

        val centerX = w / 2
        val centerY = h / 2
        // 留出边距，让雷达尽量占满图表区域（之前 -120f 会让雷达缩成很小一团）
        val radius = minOf(w, h) / 2 - 56f

        canvas.save()
        canvas.scale(d, d)

        paint.reset()
        paint.isAntiAlias = true

        // 同心圆网格 + 金额标注
        val levels = 5
        for (i in 1..levels) {
            val levelRadius = radius * i / levels
            val levelAmount = maxAmount * i / levels

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            paint.color = grid
            canvas.drawCircle(centerX, centerY, levelRadius, paint)

            val amountText = String.format("%.0f", levelAmount)
            paint.style = Paint.Style.FILL
            paint.color = ColorUtils.setAlphaComponent(grid, 204)
            paint.textAlign = Paint.Align.LEFT
            paint.textSize = 12f
            canvas.drawText(amountText, centerX + levelRadius + 6f, centerY + 4f, paint)
        }

        // 7 个顶点（星期日..星期六，从正上方顺时针）
        val vertices = 7
        val angleStep = 2 * PI / vertices
        val startAngle = -PI / 2

        // 中心到各顶点连线
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = grid
        for (i in 0 until vertices) {
            val angle = startAngle + angleStep * i
            val endX = centerX + radius * cos(angle).toFloat()
            val endY = centerY + radius * sin(angle).toFloat()
            canvas.drawLine(centerX, centerY, endX, endY, paint)
        }

        // 数据多边形
        path.reset()
        val points = ArrayList<Pair<Float, Float>>()
        for (i in 0 until vertices) {
            val item = data.getOrNull(i)
            val normalized = if (item != null && maxAmount > 0) (item.amount / maxAmount).toFloat() else 0f
            val angle = startAngle + angleStep * i
            val pointRadius = radius * normalized
            val x = centerX + pointRadius * cos(angle).toFloat()
            val y = centerY + pointRadius * sin(angle).toFloat()
            points.add(Pair(x, y))
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()

        paint.style = Paint.Style.FILL
        paint.color = ColorUtils.setAlphaComponent(primary, 76)
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f
        paint.color = primary
        canvas.drawPath(path, paint)

        // 数据点
        paint.style = Paint.Style.FILL
        points.forEach { (x, y) ->
            paint.color = primary
            canvas.drawCircle(x, y, 4f, paint)
            paint.color = Color.WHITE
            canvas.drawCircle(x, y, 2f, paint)
        }

        // 星期标签（可点击）
        val weekdayLabels = listOf(
            context.getString(R.string.sunday_short),
            context.getString(R.string.monday_short),
            context.getString(R.string.tuesday_short),
            context.getString(R.string.wednesday_short),
            context.getString(R.string.thursday_short),
            context.getString(R.string.friday_short),
            context.getString(R.string.saturday_short)
        )

        for (i in 0 until vertices) {
            val angle = startAngle + angleStep * i
            val labelRadius = radius + 30f
            val x = centerX + labelRadius * cos(angle).toFloat()
            val y = centerY + labelRadius * sin(angle).toFloat()

            // 记录命中区域（px）
            labelHit[i][0] = x * d
            labelHit[i][1] = y * d
            labelHit[i][2] = 40f * d

            // 背景圆圈（提示可点击，缩小避免喧宾夺主）
            paint.style = Paint.Style.FILL
            paint.color = ColorUtils.setAlphaComponent(primary, 26)
            canvas.drawCircle(x, y, 20f, paint)

            paint.style = Paint.Style.FILL
            paint.color = text
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 13f
            paint.isFakeBoldText = true
            canvas.drawText(weekdayLabels[i], x, y + 8f, paint)
            paint.isFakeBoldText = false
        }

        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            for (i in 0 until 7) {
                val lx = labelHit[i][0]
                val ly = labelHit[i][1]
                val r = labelHit[i][2]
                if (r > 0f) {
                    val dx = event.x - lx
                    val dy = event.y - ly
                    if (dx * dx + dy * dy <= r * r) {
                        weekdayData.getOrNull(i)?.let { onWeekdayClick?.invoke(it) }
                        return true
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }
}
