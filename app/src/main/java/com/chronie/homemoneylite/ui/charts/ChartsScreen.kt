package com.chronie.homemoneylite.ui.charts

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.domain.model.TimeRange
import com.chronie.homemoneylite.ui.expense.ExpenseTypeLocalizer
import com.chronie.homemoneylite.ui.expense.formatDateByLocale
import com.chronie.homemoneylite.ui.components.ExpressiveLinearProgressIndicator
import com.chronie.homemoneylite.ui.components.ExpressiveLoadingIndicator
import com.chronie.homemoneylite.ui.components.WheelDatePicker
import com.chronie.homemoneylite.ui.theme.*
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable

@Composable
fun ChartsScreen(
    context: Context,
    viewModel: ChartsViewModel = hiltViewModel(),
    onNavigateToWeekdayDetail: (dayOfWeek: Int, amount: Double, count: Int, percentage: Float, startDate: String, endDate: String) -> Unit = { _, _, _, _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedTimeRange by viewModel.selectedTimeRange.collectAsState()
    val customStartDate by viewModel.customStartDate.collectAsState()
    val customEndDate by viewModel.customEndDate.collectAsState()
    var timeRangeMenuExpanded by remember { mutableStateOf(false) }
    var showCustomRangeDialog by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部工具栏
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colors.surface,
                elevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = context.getString(R.string.charts_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    )
                    
                    Box {
                        IconButton(onClick = { timeRangeMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Select time range"
                            )
                        }
                        DropdownMenu(
                            expanded = timeRangeMenuExpanded,
                            onDismissRequest = { timeRangeMenuExpanded = false }
                        ) {
                            listOf(
                                TimeRange.THIS_WEEK,
                                TimeRange.THIS_MONTH,
                                TimeRange.LAST_MONTH,
                                TimeRange.THIS_QUARTER,
                                TimeRange.THIS_YEAR,
                                TimeRange.CUSTOM
                            ).forEach { range ->
                                DropdownMenuItem(
                                    onClick = {
                                        timeRangeMenuExpanded = false
                                        if (range == TimeRange.CUSTOM) {
                                            showCustomRangeDialog = true
                                        } else {
                                            viewModel.selectTimeRange(range)
                                        }
                                    }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = selectedTimeRange == range,
                                            onClick = null
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(getTimeRangeText(context, range))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // 内容区域
            when (val state = uiState) {
                is ChartsUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        ExpressiveLoadingIndicator()
                    }
                }
                is ChartsUiState.Success -> {
                    ChartsContent(
                        context = context,
                        state = state,
                        selectedTimeRange = selectedTimeRange,
                        onNavigateToWeekdayDetail = onNavigateToWeekdayDetail
                    )
                }
                is ChartsUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colors.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.refresh() }) {
                            Text(context.getString(R.string.retry))
                        }
                    }
                }
            }
        }
        
        if (showCustomRangeDialog) {
            CustomDateRangeDialog(
                context = context,
                initialStartDate = customStartDate ?: LocalDate.now().minusMonths(1),
                initialEndDate = customEndDate ?: LocalDate.now(),
                onDismiss = { showCustomRangeDialog = false },
                onConfirm = { start, end ->
                    viewModel.setCustomDateRange(start, end)
                    showCustomRangeDialog = false
                }
            )
        }
    }
}

@Composable
private fun ChartsContent(
    context: Context,
    state: ChartsUiState.Success,
    selectedTimeRange: TimeRange,
    onNavigateToWeekdayDetail: (dayOfWeek: Int, amount: Double, count: Int, percentage: Float, startDate: String, endDate: String) -> Unit = { _, _, _, _, _, _ -> }
) {
    val scrollState = rememberScrollState()
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }

    // 当前选中的图表（通过下拉菜单切换）
    var selectedChart by remember { mutableStateOf(ChartType.TREND) }
    
    // 调试日志
    LaunchedEffect(state) {
        android.util.Log.d("ChartsScreen", "UI updated: total=${state.statistics.totalAmount}, categories=${state.categoryData.size}, daily=${state.dailyData.size}")
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .padding(bottom = 80.dp)
    ) {
        // 时间范围显示
        TimeRangeCard(context, selectedTimeRange, state)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 统计摘要
        StatisticsSummaryCard(context, state.statistics, currencyFormat)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 图表类型选择（下拉菜单）
        ChartSelector(
            context = context,
            selectedChart = selectedChart,
            onChartSelected = { selectedChart = it }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 根据下拉选择只展示一张图表
        when (selectedChart) {
            ChartType.TREND -> TrendLineChartCard(context, state.dailyData, currencyFormat)
            ChartType.CATEGORY -> CategoryBreakdownCard(context, state.categoryData, currencyFormat)
            ChartType.WEEKDAY -> WeekdayRadarChartCard(
                context = context,
                weekdayData = state.weekdayData,
                currencyFormat = currencyFormat,
                startDate = state.startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                endDate = state.endDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                onNavigateToWeekdayDetail = onNavigateToWeekdayDetail
            )
        }
    }
}

/**
 * 图表类型枚举
 */
private enum class ChartType(val stringRes: Int) {
    TREND(R.string.trend_chart),
    CATEGORY(R.string.category_breakdown),
    WEEKDAY(R.string.weekday_analysis)
}

/**
 * 图表选择下拉菜单
 */
@Composable
private fun ChartSelector(
    context: Context,
    selectedChart: ChartType,
    onChartSelected: (ChartType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = context.getString(selectedChart.stringRes),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(context.getString(R.string.select_chart)) },
                    trailingIcon = {
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select chart"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    ChartType.values().forEach { chartType ->
                        DropdownMenuItem(
                            onClick = {
                                onChartSelected(chartType)
                                expanded = false
                            }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = selectedChart == chartType,
                                    onClick = {
                                        onChartSelected(chartType)
                                        expanded = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(context.getString(chartType.stringRes))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeRangeCard(
    context: Context,
    selectedTimeRange: TimeRange,
    state: ChartsUiState.Success
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = getTimeRangeText(context, selectedTimeRange),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${formatDateByLocale(state.startDate.format(DateTimeFormatter.ISO_LOCAL_DATE), context.resources.configuration.locale.toLanguageTag())} - ${formatDateByLocale(state.endDate.format(DateTimeFormatter.ISO_LOCAL_DATE), context.resources.configuration.locale.toLanguageTag())}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatisticsSummaryCard(
    context: Context,
    statistics: com.chronie.homemoneylite.domain.model.ExpenseStatistics,
    currencyFormat: NumberFormat
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = context.getString(R.string.statistics_summary),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatisticItem(
                    label = context.getString(R.string.total_amount),
                    value = currencyFormat.format(statistics.totalAmount)
                )
                StatisticItem(
                    label = context.getString(R.string.count),
                    value = "${statistics.count}"
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatisticItem(
                    label = context.getString(R.string.average_amount),
                    value = currencyFormat.format(statistics.averageAmount)
                )
                StatisticItem(
                    label = context.getString(R.string.median_amount),
                    value = currencyFormat.format(statistics.medianAmount)
                )
            }
        }
    }
}

@Composable
private fun StatisticItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colors.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TrendLineChartCard(
    context: Context,
    dailyData: List<DailyChartData>,
    currencyFormat: NumberFormat
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = context.getString(R.string.trend_chart),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (dailyData.isEmpty()) {
                Text(
                    text = context.getString(R.string.no_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            } else {
                HighQualityLineChart(
                    data = dailyData,
                    currencyFormat = currencyFormat,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                )
            }
        }
    }
}

@Composable
private fun HighQualityLineChart(
    data: List<DailyChartData>,
    currencyFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colors.primary
    val textColor = MaterialTheme.colors.onSurface
    val gridColor = MaterialTheme.colors.outlineVariant
    
    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas
        
        val maxAmount = data.maxOfOrNull { it.amount } ?: 0.0
        if (maxAmount == 0.0) return@Canvas
        
        val width = size.width
        val height = size.height
        val paddingLeft = 80f
        val paddingRight = 40f
        val paddingTop = 60f
        val paddingBottom = 80f
        
        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom
        
        val paint = android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = 28f
            color = textColor.toArgb()
        }
        
        // 绘制Y轴网格线和标签
        val ySteps = 5
        for (i in 0..ySteps) {
            val y = paddingTop + (chartHeight / ySteps) * i
            val amount = maxAmount * (1 - i.toFloat() / ySteps)
            
            // 网格线
            drawLine(
                color = gridColor,
                start = Offset(paddingLeft, y),
                end = Offset(width - paddingRight, y),
                strokeWidth = 1f
            )
            
            // Y轴标签
            val label = currencyFormat.format(amount)
            drawContext.canvas.nativeCanvas.drawText(
                label,
                paddingLeft - 10f,
                y + 10f,
                paint.apply { textAlign = android.graphics.Paint.Align.RIGHT }
            )
        }
        
        // 绘制坐标轴
        drawLine(
            color = textColor.copy(alpha = 0.5f),
            start = Offset(paddingLeft, paddingTop),
            end = Offset(paddingLeft, height - paddingBottom),
            strokeWidth = 2f
        )
        drawLine(
            color = textColor.copy(alpha = 0.5f),
            start = Offset(paddingLeft, height - paddingBottom),
            end = Offset(width - paddingRight, height - paddingBottom),
            strokeWidth = 2f
        )
        
        // 绘制折线
        val path = Path()
        val points = mutableListOf<Pair<Float, Float>>()
        
        data.forEachIndexed { index, dailyData ->
            val x = paddingLeft + (index.toFloat() / (data.size - 1).coerceAtLeast(1)) * chartWidth
            val y = height - paddingBottom - (dailyData.amount.toFloat() / maxAmount.toFloat()) * chartHeight
            
            points.add(Pair(x, y))
            
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        
        // 绘制折线
        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 4f)
        )
        
        // 绘制数据点和标签
        data.forEachIndexed { index, dailyData ->
            val (x, y) = points[index]
            
            // 数据点
            drawCircle(
                color = primaryColor,
                radius = 6f,
                center = Offset(x, y)
            )
            
            drawCircle(
                color = Color.White,
                radius = 3f,
                center = Offset(x, y)
            )
            
            // 显示所有非零数值标签
            if (dailyData.amount > 0) {
                val valueLabel = String.format("%.0f", dailyData.amount)
                drawContext.canvas.nativeCanvas.drawText(
                    valueLabel,
                    x,
                    y - 20f,
                    paint.apply {
                        color = primaryColor.toArgb()
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 22f
                    }
                )
            }
        }
        
        // X轴日期标签
        val xLabelStep = (data.size / 7).coerceAtLeast(1)
        data.forEachIndexed { index, dailyData ->
            if (index % xLabelStep == 0 || index == data.size - 1) {
                val (x, _) = points[index]
                val dateLabel = "${dailyData.date.monthValue}/${dailyData.date.dayOfMonth}"
                
                drawContext.canvas.nativeCanvas.drawText(
                    dateLabel,
                    x,
                    height - paddingBottom + 40f,
                    paint.apply {
                        color = textColor.toArgb()
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 26f
                    }
                )
            }
        }
    }
}

@Composable
private fun CategoryBreakdownCard(
    context: Context,
    categoryData: List<CategoryChartData>,
    currencyFormat: NumberFormat
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = context.getString(R.string.category_breakdown),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (categoryData.isEmpty()) {
                Text(
                    text = context.getString(R.string.no_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            } else {
                // 柱状图
                val minChartWidth = if (categoryData.size * 64 > 320) (categoryData.size * 64).dp else 320.dp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    CategoryBarChart(
                        context = context,
                        categoryData = categoryData,
                        currencyFormat = currencyFormat,
                        modifier = Modifier
                            .width(minChartWidth)
                            .height(320.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 详细数据列表
                categoryData.forEach { category ->
                    CategoryItem(context, category, currencyFormat)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun CategoryBarChart(
    context: Context,
    categoryData: List<CategoryChartData>,
    currencyFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    val textColor = MaterialTheme.colors.onSurface
    val gridColor = MaterialTheme.colors.outlineVariant

    Canvas(modifier = modifier) {
        if (categoryData.isEmpty()) return@Canvas
        val maxAmount = categoryData.maxOfOrNull { it.amount } ?: 0.0
        if (maxAmount == 0.0) return@Canvas

        val width = size.width
        val height = size.height
        val paddingLeft = 24f
        val paddingRight = 24f
        val paddingTop = 50f
        val paddingBottom = 80f
        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        // 水平网格线
        val ySteps = 4
        for (i in 0..ySteps) {
            val y = paddingTop + (chartHeight / ySteps) * i
            drawLine(
                color = gridColor,
                start = Offset(paddingLeft, y),
                end = Offset(width - paddingRight, y),
                strokeWidth = 1f
            )
        }

        val barCount = categoryData.size
        val slotWidth = chartWidth / barCount
        val barWidth = (slotWidth * 0.6f).coerceAtMost(90f)

        val labelPaint = android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
        }

        categoryData.forEachIndexed { index, category ->
            val slotCenterX = paddingLeft + slotWidth * index + slotWidth / 2
            val barHeight = (category.amount / maxAmount).toFloat() * chartHeight
            val topY = paddingTop + (chartHeight - barHeight)
            val leftX = slotCenterX - barWidth / 2
            val barColor = barChartPalette[index % barChartPalette.size]

            // 柱子
            if (barHeight > 0) {
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(leftX, topY),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(8f, 8f)
                )
            }

            // 顶部数值标签
            val valueText = String.format("%.0f", category.amount)
            drawContext.canvas.nativeCanvas.drawText(
                valueText,
                slotCenterX,
                topY - 14f,
                labelPaint.apply {
                    color = barColor.toArgb()
                    textSize = 24f
                }
            )

            // 类别名称（超长截断保护）
            val name = ExpenseTypeLocalizer.getLocalizedTypeName(context, category.type)
            val shortName = if (name.length > 6) name.take(6) else name
            drawContext.canvas.nativeCanvas.drawText(
                shortName,
                slotCenterX,
                height - paddingBottom + 40f,
                labelPaint.apply {
                    color = textColor.toArgb()
                    textSize = 26f
                }
            )

            // 占比标签
            val pctText = String.format("%.0f%%", category.percentage)
            drawContext.canvas.nativeCanvas.drawText(
                pctText,
                slotCenterX,
                height - paddingBottom + 66f,
                labelPaint.apply {
                    color = textColor.copy(alpha = 0.7f).toArgb()
                    textSize = 22f
                }
            )
        }
    }
}

private val barChartPalette = listOf(
    Color(0xFF4F8DFD),
    Color(0xFF54C8A8),
    Color(0xFFF4B400),
    Color(0xFFE0607E),
    Color(0xFF9B6DFF),
    Color(0xFF3FC1D8),
    Color(0xFFFF8A5B),
    Color(0xFF7CCF5A),
    Color(0xFF5C6BC0),
    Color(0xFFEC6F9C)
)

@Composable
private fun CategoryItem(
    context: Context,
    category: CategoryChartData,
    currencyFormat: NumberFormat
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ExpenseTypeLocalizer.getLocalizedTypeName(context, category.type),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${String.format("%.1f", category.percentage)}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        ExpressiveLinearProgressIndicator(
            progress = category.percentage / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = MaterialTheme.colors.primary,
            backgroundColor = MaterialTheme.colors.surfaceVariant
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "${currencyFormat.format(category.amount)} (${category.count} ${context.getString(R.string.records)})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colors.onSurfaceVariant
        )
    }
}

/**
 * 自定义时间范围弹窗：开始/结束日期可自由切换编辑，共用一个滚轮日期选择器。
 */
@Composable
private fun CustomDateRangeDialog(
    context: Context,
    initialStartDate: LocalDate,
    initialEndDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit
) {
    var startDate by remember { mutableStateOf(initialStartDate) }
    var endDate by remember { mutableStateOf(initialEndDate) }
    var editingStart by remember { mutableStateOf(true) }
    val isValid = !startDate.isAfter(endDate)
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy年M月d日") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(context.getString(R.string.custom_range)) },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DateFieldChip(
                        label = context.getString(R.string.expense_list_filter_start_date),
                        value = startDate.format(dateFormatter),
                        selected = editingStart,
                        onClick = { editingStart = true },
                        modifier = Modifier.weight(1f)
                    )
                    DateFieldChip(
                        label = context.getString(R.string.expense_list_filter_end_date),
                        value = endDate.format(dateFormatter),
                        selected = !editingStart,
                        onClick = { editingStart = false },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                WheelDatePicker(
                    date = if (editingStart) startDate else endDate,
                    onDateChange = {
                        if (editingStart) startDate = it else endDate = it
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (!isValid) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = context.getString(R.string.custom_range_invalid),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colors.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(startDate, endDate) },
                enabled = isValid
            ) {
                Text(context.getString(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(context.getString(R.string.cancel))
            }
        }
    )
}

/**
 * 开始/结束日期字段（可点击切换当前编辑对象）
 */
@Composable
private fun DateFieldChip(
    label: String,
    value: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colors.primary
            else MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
        ),
        color = if (selected) MaterialTheme.colors.primary.copy(alpha = 0.08f)
        else MaterialTheme.colors.surface
    ) {
        Column(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colors.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colors.primary
                else MaterialTheme.colors.onSurface
            )
        }
    }
}

private fun getTimeRangeText(context: Context, timeRange: TimeRange): String {
    return when (timeRange) {
        TimeRange.THIS_WEEK -> context.getString(R.string.this_week)
        TimeRange.THIS_MONTH -> context.getString(R.string.this_month)
        TimeRange.LAST_MONTH -> context.getString(R.string.last_month)
        TimeRange.THIS_QUARTER -> context.getString(R.string.this_quarter)
        TimeRange.THIS_YEAR -> context.getString(R.string.this_year)
        TimeRange.CUSTOM -> context.getString(R.string.custom_range)
    }
}
