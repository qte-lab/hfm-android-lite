package com.chronie.homemoney.ui.charts

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chronie.homemoney.R
import com.chronie.homemoney.ui.components.ExpressiveLinearProgressIndicator
import com.chronie.homemoney.ui.expense.ExpenseTypeLocalizer
import java.text.NumberFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.PI

/**
 * 按星期几统计的雷达图卡片
 */
@Composable
fun WeekdayRadarChartCard(
    context: Context,
    weekdayData: List<WeekdayChartData>,
    currencyFormat: NumberFormat,
    startDate: String,
    endDate: String,
    onNavigateToWeekdayDetail: (dayOfWeek: Int, amount: Double, count: Int, percentage: Float, startDate: String, endDate: String) -> Unit = { _, _, _, _, _, _ -> }
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = context.getString(R.string.weekday_analysis),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (weekdayData.isEmpty() || weekdayData.all { it.amount == 0.0 }) {
                Text(
                    text = context.getString(R.string.no_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            } else {
                // 雷达图
                WeekdayRadarChart(
                    context = context,
                    weekdayData = weekdayData,
                    currencyFormat = currencyFormat,
                    onWeekdayClick = { weekday ->
                        onNavigateToWeekdayDetail(
                            weekday.dayOfWeek,
                            weekday.amount,
                            weekday.count,
                            weekday.percentage,
                            startDate,
                            endDate
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 详细数据列表
                weekdayData.forEach { data ->
                    if (data.amount > 0) {
                        WeekdayDataItem(context, data, currencyFormat)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

/**
 * 雷达图组件
 */
@Composable
private fun WeekdayRadarChart(
    context: Context,
    weekdayData: List<WeekdayChartData>,
    currencyFormat: NumberFormat,
    onWeekdayClick: (WeekdayChartData) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurface
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    
    // 存储标签位置用于点击检测
    val labelPositions = remember { mutableStateMapOf<Int, Pair<Offset, Float>>() }
    
    Canvas(modifier = modifier.pointerInput(Unit) {
        detectTapGestures { offset ->
            // 检测点击的是哪个星期标签
            labelPositions.forEach { (dayOfWeek, posAndRadius) ->
                val (labelPos, radius) = posAndRadius
                val distance = sqrt(
                    (offset.x - labelPos.x) * (offset.x - labelPos.x) +
                    (offset.y - labelPos.y) * (offset.y - labelPos.y)
                )
                if (distance <= radius) {
                    weekdayData.getOrNull(dayOfWeek)?.let { data ->
                        onWeekdayClick(data)
                    }
                }
            }
        }
    }) {
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height / 2
        val radius = minOf(width, height) / 2 - 120f
        
        // 获取最大值用于归一化
        val maxAmount = weekdayData.maxOfOrNull { it.amount } ?: 1.0
        if (maxAmount == 0.0) return@Canvas
        
        // 绘制同心圆网格（5层）并添加金额标注
        val levels = 5
        for (i in 1..levels) {
            val levelRadius = radius * i / levels
            val levelAmount = maxAmount * i / levels
            
            // 绘制圆圈
            drawCircle(
                color = gridColor,
                radius = levelRadius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1f)
            )
            
            // 在右侧添加金额标注
            val amountText = String.format("%.0f", levelAmount)
            val amountPaint = android.graphics.Paint().apply {
                textAlign = android.graphics.Paint.Align.LEFT
                textSize = 24f
                color = gridColor.copy(alpha = 0.8f).toArgb()
            }
            
            drawContext.canvas.nativeCanvas.drawText(
                amountText,
                centerX + levelRadius + 10f,
                centerY + 8f,
                amountPaint
            )
        }
        
        // 7个顶点（星期日到星期六）
        val vertices = 7
        val angleStep = 2 * PI / vertices
        
        // 从顶部开始（星期日），顺时针排列
        val startAngle = -PI / 2 // 从正上方开始
        
        // 绘制从中心到各顶点的线
        for (i in 0 until vertices) {
            val angle = startAngle + angleStep * i
            val endX = centerX + radius * cos(angle).toFloat()
            val endY = centerY + radius * sin(angle).toFloat()
            
            drawLine(
                color = gridColor,
                start = Offset(centerX, centerY),
                end = Offset(endX, endY),
                strokeWidth = 1f
            )
        }
        
        // 绘制数据多边形
        val dataPath = Path()
        val points = mutableListOf<Offset>()
        
        for (i in 0 until vertices) {
            val data = weekdayData.getOrNull(i)
            val normalizedValue = if (data != null && maxAmount > 0) {
                (data.amount / maxAmount).toFloat()
            } else {
                0f
            }
            
            val angle = startAngle + angleStep * i
            val pointRadius = radius * normalizedValue
            val x = centerX + pointRadius * cos(angle).toFloat()
            val y = centerY + pointRadius * sin(angle).toFloat()
            
            points.add(Offset(x, y))
            
            if (i == 0) {
                dataPath.moveTo(x, y)
            } else {
                dataPath.lineTo(x, y)
            }
        }
        dataPath.close()
        
        // 填充数据区域
        drawPath(
            path = dataPath,
            color = primaryColor.copy(alpha = 0.3f)
        )
        
        // 绘制数据边界线
        drawPath(
            path = dataPath,
            color = primaryColor,
            style = Stroke(width = 3f)
        )
        
        // 绘制数据点
        points.forEach { point ->
            drawCircle(
                color = primaryColor,
                radius = 6f,
                center = point
            )
            drawCircle(
                color = Color.White,
                radius = 3f,
                center = point
            )
        }
        
        // 绘制星期标签（可点击）
        val weekdayLabels = listOf(
            context.getString(R.string.sunday_short),
            context.getString(R.string.monday_short),
            context.getString(R.string.tuesday_short),
            context.getString(R.string.wednesday_short),
            context.getString(R.string.thursday_short),
            context.getString(R.string.friday_short),
            context.getString(R.string.saturday_short)
        )
        
        labelPositions.clear()
        
        for (i in 0 until vertices) {
            val angle = startAngle + angleStep * i
            val labelRadius = radius + 60f
            val x = centerX + labelRadius * cos(angle).toFloat()
            val y = centerY + labelRadius * sin(angle).toFloat()
            
            // 存储标签位置用于点击检测
            labelPositions[i] = Pair(Offset(x, y), 40f)
            
            // 绘制标签背景圆圈（提示可点击）
            drawCircle(
                color = primaryColor.copy(alpha = 0.1f),
                radius = 35f,
                center = Offset(x, y)
            )
            
            // 调整文本对齐方式
            val textPaint = android.graphics.Paint().apply {
                textSize = 36f
                color = textColor.toArgb()
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
            }
            
            drawContext.canvas.nativeCanvas.drawText(
                weekdayLabels[i],
                x,
                y + 12f, // 垂直居中调整
                textPaint
            )
        }
    }
}

/**
 * 星期数据项
 */
@Composable
private fun WeekdayDataItem(
    context: Context,
    data: WeekdayChartData,
    currencyFormat: NumberFormat
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = getWeekdayName(context, data.dayOfWeek),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = "${String.format("%.1f", data.percentage)}%",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(60.dp)
        )
        
        Text(
            text = currencyFormat.format(data.amount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(100.dp)
        )
    }
}

/**
 * 类型详细信息项
 */
@Composable
private fun CategoryDetailItem(
    context: Context,
    category: CategoryChartData,
    currencyFormat: NumberFormat
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = ExpenseTypeLocalizer.getLocalizedTypeName(context, category.type),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${String.format("%.1f", category.percentage)}%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(60.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        ExpressiveLinearProgressIndicator(
            progress = category.percentage / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${category.count} ${context.getString(R.string.records)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = currencyFormat.format(category.amount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * 获取星期名称
 */
private fun getWeekdayName(context: Context, dayOfWeek: Int): String {
    return when (dayOfWeek) {
        0 -> context.getString(R.string.sunday)
        1 -> context.getString(R.string.monday)
        2 -> context.getString(R.string.tuesday)
        3 -> context.getString(R.string.wednesday)
        4 -> context.getString(R.string.thursday)
        5 -> context.getString(R.string.friday)
        6 -> context.getString(R.string.saturday)
        else -> ""
    }
}
