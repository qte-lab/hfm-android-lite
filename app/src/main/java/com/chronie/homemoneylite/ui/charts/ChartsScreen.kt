package com.chronie.homemoneylite.ui.charts

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.domain.model.TimeRange
import com.chronie.homemoneylite.ui.expense.ExpenseTypeLocalizer
import com.chronie.homemoneylite.ui.expense.formatDateByLocale
import com.chronie.homemoneylite.ui.components.ExpressiveLinearProgressIndicator
import com.chronie.homemoneylite.ui.components.ExpressiveLoadingIndicator
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(
    context: Context,
    viewModel: ChartsViewModel = hiltViewModel(),
    onNavigateToWeekdayDetail: (dayOfWeek: Int, amount: Double, count: Int, percentage: Float, startDate: String, endDate: String) -> Unit = { _, _, _, _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedTimeRange by viewModel.selectedTimeRange.collectAsState()   
    var showTimeRangeDialog by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部工具栏
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
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
                    
                    IconButton(onClick = { showTimeRangeDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Select time range"
                        )
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
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.refresh() }) {
                            Text(context.getString(R.string.retry))
                        }
                    }
                }
            }
        }
        
        if (showTimeRangeDialog) {
            TimeRangeDialog(
                context = context,
                selectedTimeRange = selectedTimeRange,
                onDismiss = { showTimeRangeDialog = false },
                onTimeRangeSelected = { timeRange ->
                    viewModel.selectTimeRange(timeRange)
                    showTimeRangeDialog = false
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
        
        // 趋势折线图
        HybridTrendLineChartCard(context, state.dailyData, currencyFormat)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 分类占比
        CategoryBreakdownCard(context, state.categoryData, currencyFormat)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 星期分析雷达图
        HybridWeekdayRadarChartCard(
            context = context,
            weekdayData = state.weekdayData,
            currencyFormat = currencyFormat,
            startDate = state.startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
            endDate = state.endDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
            onNavigateToWeekdayDetail = onNavigateToWeekdayDetail
        )
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HybridTrendLineChartCard(
    context: Context,
    dailyData: List<DailyChartData>,
    currencyFormat: NumberFormat
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkTheme = isSystemInDarkTheme()
    val htmlContent = remember(dailyData, currencyFormat, colorScheme, isDarkTheme) {
        buildLineChartHtml(
            data = dailyData,
            currencyFormat = currencyFormat,
            backgroundColor = colorScheme.background,
            surfaceColor = colorScheme.surface,
            textColor = colorScheme.onSurface,
            primaryColor = colorScheme.primary,
            secondaryTextColor = colorScheme.onSurfaceVariant,
            borderColor = colorScheme.outlineVariant,
            isDarkTheme = isDarkTheme
        )
    }

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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            } else {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    factory = { webContext ->
                        WebView(webContext).apply {
                            setBackgroundColor(colorScheme.background.toArgb())
                            settings.javaScriptEnabled = false
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    return false
                                }

                                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                    return false
                                }
                            }
                            loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
                        }
                    },
                    update = { webView ->
                        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                categoryData.forEach { category ->
                    CategoryItem(context, category, currencyFormat)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

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
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "${currencyFormat.format(category.amount)} (${category.count} ${context.getString(R.string.records)})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HybridWeekdayRadarChartCard(
    context: Context,
    weekdayData: List<WeekdayChartData>,
    currencyFormat: NumberFormat,
    startDate: String,
    endDate: String,
    onNavigateToWeekdayDetail: (dayOfWeek: Int, amount: Double, count: Int, percentage: Float, startDate: String, endDate: String) -> Unit = { _, _, _, _, _, _ -> }
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkTheme = isSystemInDarkTheme()
    val htmlContent = remember(weekdayData, currencyFormat, colorScheme, isDarkTheme, startDate, endDate) {
        buildRadarChartHtml(
            weekdayData = weekdayData,
            currencyFormat = currencyFormat,
            backgroundColor = colorScheme.background,
            surfaceColor = colorScheme.surface,
            textColor = colorScheme.onSurface,
            primaryColor = colorScheme.primary,
            secondaryTextColor = colorScheme.onSurfaceVariant,
            borderColor = colorScheme.outlineVariant,
            isDarkTheme = isDarkTheme,
            context = context
        )
    }

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
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    factory = { webContext ->
                        WebView(webContext).apply {
                            setBackgroundColor(colorScheme.background.toArgb())
                            settings.javaScriptEnabled = false
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    return handleRadarUrl(
                                        view = view,
                                        url = url,
                                        weekdayData = weekdayData,
                                        startDate = startDate,
                                        endDate = endDate,
                                        onNavigateToWeekdayDetail = onNavigateToWeekdayDetail
                                    )
                                }
                            }
                            loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
                        }
                    },
                    update = { webView ->
                        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

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

@OptIn(ExperimentalMaterial3Api::class)
private fun handleRadarUrl(
    view: WebView?,
    url: String,
    weekdayData: List<WeekdayChartData>,
    startDate: String,
    endDate: String,
    onNavigateToWeekdayDetail: (dayOfWeek: Int, amount: Double, count: Int, percentage: Float, startDate: String, endDate: String) -> Unit
): Boolean {
    if (!url.startsWith("weekday://")) {
        return false
    }

    val dayOfWeek = url.removePrefix("weekday://").toIntOrNull() ?: return true
    val data = weekdayData.getOrNull(dayOfWeek) ?: return true
    onNavigateToWeekdayDetail(
        data.dayOfWeek,
        data.amount,
        data.count,
        data.percentage,
        startDate,
        endDate
    )
    return true
}

private fun buildLineChartHtml(
    data: List<DailyChartData>,
    currencyFormat: NumberFormat,
    backgroundColor: androidx.compose.ui.graphics.Color,
    surfaceColor: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    primaryColor: androidx.compose.ui.graphics.Color,
    secondaryTextColor: androidx.compose.ui.graphics.Color,
    borderColor: androidx.compose.ui.graphics.Color,
    isDarkTheme: Boolean
): String {
    if (data.isEmpty()) {
        return """
            <!DOCTYPE html>
            <html><body style="margin:0;padding:0;background:${colorToHex(backgroundColor)};color:${colorToHex(textColor)};font-family:system-ui;display:flex;align-items:center;justify-content:center;height:100%">No data</body></html>
        """.trimIndent()
    }

    val maxAmount = data.maxOfOrNull { it.amount } ?: 0.0
    if (maxAmount <= 0.0) {
        return """
            <!DOCTYPE html>
            <html><body style="margin:0;padding:0;background:${colorToHex(backgroundColor)};color:${colorToHex(textColor)};font-family:system-ui;display:flex;align-items:center;justify-content:center;height:100%">No data</body></html>
        """.trimIndent()
    }

    val width = 640
    val height = 320
    val paddingLeft = 70
    val paddingRight = 24
    val paddingTop = 24
    val paddingBottom = 56
    val chartWidth = width - paddingLeft - paddingRight
    val chartHeight = height - paddingTop - paddingBottom

    val points = data.mapIndexed { index, item ->
        val x = paddingLeft + (index.toFloat() / (data.size - 1).coerceAtLeast(1)) * chartWidth
        val y = height - paddingBottom - (item.amount / maxAmount).toFloat() * chartHeight
        Pair(x.toInt(), y.toInt())
    }

    val linePoints = points.joinToString(" ") { (x, y) -> "$x,$y" }
    val yTicks = (0..4).joinToString("") { step ->
        val y = paddingTop + (chartHeight / 4) * step
        val amount = maxAmount * (1 - step.toFloat() / 4)
        val label = escapeHtml(currencyFormat.format(amount))
        """<line x1="$paddingLeft" y1="$y" x2="${width - paddingRight}" y2="$y" stroke="${colorToHex(borderColor)}" stroke-width="1" stroke-dasharray="4 4" />""" +
                """<text x="${paddingLeft - 10}" y="${y + 4}" fill="${colorToHex(secondaryTextColor)}" text-anchor="end" font-size="12">$label</text>"""
    }

    val xLabels = data.mapIndexed { index, item ->
        val shouldShow = index == 0 || index == data.size - 1 || index % ((data.size / 6).coerceAtLeast(1)) == 0
        if (!shouldShow) return@mapIndexed ""
        val (x, _) = points[index]
        val label = "${item.date.monthValue}/${item.date.dayOfMonth}"
        """<text x="$x" y="${height - paddingBottom + 20}" fill="${colorToHex(secondaryTextColor)}" text-anchor="middle" font-size="12">${escapeHtml(label)}</text>"""
    }.joinToString("")

    val valueLabels = points.mapIndexed { index, (x, y) ->
        val item = data[index]
        if (item.amount <= 0) return@mapIndexed ""
        val label = escapeHtml(String.format(Locale.US, "%.0f", item.amount))
        """<circle cx="$x" cy="$y" r="4" fill="${colorToHex(primaryColor)}" />""" +
                """<text x="$x" y="${y - 10}" fill="${colorToHex(primaryColor)}" text-anchor="middle" font-size="12">$label</text>"""
    }.joinToString("")

    val bgHex = colorToHex(backgroundColor)
    val surfaceHex = colorToHex(surfaceColor)
    val textHex = colorToHex(textColor)
    val primaryHex = colorToHex(primaryColor)
    val borderHex = colorToHex(borderColor)
    val shadow = if (isDarkTheme) "rgba(255,255,255,0.08)" else "rgba(0,0,0,0.12)"

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1" />
            <style>
                body { margin: 0; padding: 0; background: $bgHex; color: $textHex; font-family: system-ui, sans-serif; }
                .wrap { padding: 8px; }
                svg { width: 100%; height: auto; display: block; }
            </style>
        </head>
        <body>
            <div class="wrap">
                <svg viewBox="0 0 $width $height" xmlns="http://www.w3.org/2000/svg">
                    <rect x="0" y="0" width="$width" height="$height" rx="16" fill="$surfaceHex" stroke="$borderHex" />
                    $yTicks
                    <line x1="$paddingLeft" y1="$paddingTop" x2="$paddingLeft" y2="${height - paddingBottom}" stroke="$borderHex" stroke-width="1.4" />
                    <line x1="$paddingLeft" y1="${height - paddingBottom}" x2="${width - paddingRight}" y2="${height - paddingBottom}" stroke="$borderHex" stroke-width="1.4" />
                    <polyline fill="none" stroke="$primaryHex" stroke-width="3" points="$linePoints" />
                    $valueLabels
                    $xLabels
                </svg>
            </div>
        </body>
        </html>
    """.trimIndent()
}

private fun buildRadarChartHtml(
    weekdayData: List<WeekdayChartData>,
    currencyFormat: NumberFormat,
    backgroundColor: androidx.compose.ui.graphics.Color,
    surfaceColor: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    primaryColor: androidx.compose.ui.graphics.Color,
    secondaryTextColor: androidx.compose.ui.graphics.Color,
    borderColor: androidx.compose.ui.graphics.Color,
    isDarkTheme: Boolean,
    context: Context
): String {
    if (weekdayData.isEmpty()) {
        return """
            <!DOCTYPE html>
            <html><body style="margin:0;padding:0;background:${colorToHex(backgroundColor)};color:${colorToHex(textColor)};font-family:system-ui;display:flex;align-items:center;justify-content:center;height:100%">No data</body></html>
        """.trimIndent()
    }

    val width = 640
    val height = 420
    val centerX = width / 2
    val centerY = height / 2
    val radius = 150
    val vertices = 7
    val angleStep = 2 * Math.PI / vertices
    val startAngle = -Math.PI / 2
    val maxAmount = weekdayData.maxOfOrNull { it.amount } ?: 0.0

    val labels = listOf(
        context.getString(R.string.sunday_short),
        context.getString(R.string.monday_short),
        context.getString(R.string.tuesday_short),
        context.getString(R.string.wednesday_short),
        context.getString(R.string.thursday_short),
        context.getString(R.string.friday_short),
        context.getString(R.string.saturday_short)
    )

    val points = (0 until vertices).map { index ->
        val data = weekdayData.getOrNull(index)
        val normalizedValue = if (data != null && maxAmount > 0) (data.amount / maxAmount).toFloat() else 0f
        val angle = startAngle + angleStep * index
        val pointRadius = radius * normalizedValue
        val x = centerX + pointRadius * kotlin.math.cos(angle).toFloat()
        val y = centerY + pointRadius * kotlin.math.sin(angle).toFloat()
        Pair(x.toInt(), y.toInt())
    }

    val polygonPoints = points.joinToString(" ") { (x, y) -> "$x,$y" }
    val axisMarkup = (0 until vertices).joinToString("") { index ->
        val angle = startAngle + angleStep * index
        val endX = centerX + radius * kotlin.math.cos(angle).toFloat()
        val endY = centerY + radius * kotlin.math.sin(angle).toFloat()
        val labelRadius = radius + 48
        val labelX = centerX + labelRadius * kotlin.math.cos(angle).toFloat()
        val labelY = centerY + labelRadius * kotlin.math.sin(angle).toFloat()
        val label = escapeHtml(labels[index])
        """<line x1="$centerX" y1="$centerY" x2="$endX" y2="$endY" stroke="${colorToHex(borderColor)}" stroke-width="1" />""" +
                """<a href="weekday://$index"><text x="$labelX" y="$labelY" fill="${colorToHex(textColor)}" text-anchor="middle" font-size="14">$label</text></a>"""
    }

    val gridMarkup = (1..5).joinToString("") { level ->
        val levelRadius = radius * level / 5
        val levelAmount = maxAmount * level / 5
        val label = escapeHtml(currencyFormat.format(levelAmount))
        "<circle cx="$centerX" cy="$centerY" r="$levelRadius" fill="none" stroke="${colorToHex(borderColor)}" stroke-width="1" stroke-dasharray=\"4 4\" />" +
                "<text x="${centerX + levelRadius + 10}" y="${centerY + 4}" fill="${colorToHex(secondaryTextColor)}" font-size=\"12\">$label</text>"
    }

    val pointMarkup = points.mapIndexed { index, (x, y) ->
        val value = weekdayData.getOrNull(index)?.amount ?: 0.0
        val valueLabel = escapeHtml(String.format(Locale.US, "%.0f", value))
        "<circle cx="$x" cy="$y" r="5" fill="${colorToHex(primaryColor)}" />" +
                "<circle cx="$x" cy="$y" r="2" fill="${colorToHex(backgroundColor)}" />" +
                "<text x="$x" y="${y - 12}" fill="${colorToHex(primaryColor)}" text-anchor=\"middle\" font-size=\"11\">$valueLabel</text>"
    }.joinToString("")

    val bgHex = colorToHex(backgroundColor)
    val surfaceHex = colorToHex(surfaceColor)
    val textHex = colorToHex(textColor)
    val primaryHex = colorToHex(primaryColor)
    val borderHex = colorToHex(borderColor)

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1" />
            <style>
                body { margin: 0; padding: 0; background: $bgHex; color: $textHex; font-family: system-ui, sans-serif; }
                .wrap { padding: 8px; }
                svg { width: 100%; height: auto; display: block; }
                a { cursor: pointer; }
            </style>
        </head>
        <body>
            <div class="wrap">
                <svg viewBox="0 0 $width $height" xmlns="http://www.w3.org/2000/svg">
                    <rect x="0" y="0" width="$width" height="$height" rx="16" fill="$surfaceHex" stroke="$borderHex" />
                    $gridMarkup
                    $axisMarkup
                    <polygon points="$polygonPoints" fill="$primaryHex" fill-opacity="0.18" stroke="$primaryHex" stroke-width="3" />
                    $pointMarkup
                </svg>
            </div>
        </body>
        </html>
    """.trimIndent()
}

private fun escapeHtml(value: String): String {
    return buildString {
        value.forEach { ch ->
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(ch)
            }
        }
    }
}

private fun colorToHex(color: androidx.compose.ui.graphics.Color): String {
    return String.format("#%06X", (color.toArgb() and 0x00FFFFFF))
}

@Composable
private fun TimeRangeDialog(
    context: Context,
    selectedTimeRange: TimeRange,
    onDismiss: () -> Unit,
    onTimeRangeSelected: (TimeRange) -> Unit
) {
    val viewModel = hiltViewModel<ChartsViewModel>()
    val customStartDate by viewModel.customStartDate.collectAsState()
    val customEndDate by viewModel.customEndDate.collectAsState()
    
    var showCustomRangeBottomSheet by remember { mutableStateOf(false) }
    
    val sheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = context.getString(R.string.select_time_range),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            var expanded by remember { mutableStateOf(false) }
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = getTimeRangeText(context, selectedTimeRange),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(context.getString(R.string.select_time_range)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    listOf(
                        TimeRange.THIS_WEEK,
                        TimeRange.THIS_MONTH,
                        TimeRange.LAST_MONTH,
                        TimeRange.THIS_QUARTER,
                        TimeRange.THIS_YEAR,
                        TimeRange.CUSTOM
                    ).forEach { timeRange ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedTimeRange == timeRange,
                                        onClick = {
                                            if (timeRange == TimeRange.CUSTOM) {
                                                showCustomRangeBottomSheet = true
                                            } else {
                                                onTimeRangeSelected(timeRange)
                                            }
                                            expanded = false
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(getTimeRangeText(context, timeRange))
                                }
                            },
                            onClick = {
                                if (timeRange == TimeRange.CUSTOM) {
                                    showCustomRangeBottomSheet = true
                                } else {
                                    onTimeRangeSelected(timeRange)
                                }
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (selectedTimeRange == TimeRange.CUSTOM && customStartDate != null && customEndDate != null) {
                val start = customStartDate
                val end = customEndDate
                if (start != null && end != null) {
                    val startDateString = start.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val endDateString = end.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    Text(
                        text = "${context.getString(R.string.expense_list_filter_start_date)} ${formatDateByLocale(startDateString, context.resources.configuration.locale.toLanguageTag())} ${context.getString(R.string.expense_list_filter_end_date)} ${formatDateByLocale(endDateString, context.resources.configuration.locale.toLanguageTag())}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    if (showCustomRangeBottomSheet) {
        CustomRangeBottomSheet(
            context = context,
            initialStartDate = customStartDate ?: LocalDate.now().minusMonths(1),
            initialEndDate = customEndDate ?: LocalDate.now(),
            onDismiss = { showCustomRangeBottomSheet = false },
            onConfirm = { startDate, endDate ->
                viewModel.setCustomDateRange(startDate, endDate)
                onTimeRangeSelected(TimeRange.CUSTOM)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomRangeBottomSheet(
    context: Context,
    initialStartDate: LocalDate,
    initialEndDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()
    
    var startDate by remember { mutableStateOf(initialStartDate) }
    var endDate by remember { mutableStateOf(initialEndDate) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = context.getString(R.string.custom_range),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            OutlinedTextField(
                value = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                onValueChange = {},
                readOnly = true,
                label = { Text(context.getString(R.string.expense_list_filter_start_date)) },
                trailingIcon = {
                    IconButton(onClick = { showStartDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = context.getString(R.string.expense_list_filter_start_date)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                onValueChange = {},
                readOnly = true,
                label = { Text(context.getString(R.string.expense_list_filter_end_date)) },
                trailingIcon = {
                    IconButton(onClick = { showEndDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = context.getString(R.string.expense_list_filter_end_date)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(context.getString(R.string.cancel))
                }
                Button(
                    onClick = {
                        onConfirm(startDate, endDate)
                        coroutineScope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !startDate.isAfter(endDate)
                ) {
                    Text(context.getString(R.string.confirm))
                }
            }
        }
    }
    
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            startDate = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        showStartDatePicker = false
                    }
                ) {
                    Text(context.getString(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text(context.getString(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = endDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            endDate = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        showEndDatePicker = false
                    }
                ) {
                    Text(context.getString(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text(context.getString(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun TimeRangeOption(
    context: Context,
    timeRange: TimeRange,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = getTimeRangeText(context, timeRange),
            style = MaterialTheme.typography.bodyLarge
        )
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
