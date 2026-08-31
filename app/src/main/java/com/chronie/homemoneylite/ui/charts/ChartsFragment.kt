package com.chronie.homemoneylite.ui.charts

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.databinding.FragmentChartsBinding
import com.chronie.homemoneylite.databinding.ItemChartCategoryBinding
import com.chronie.homemoneylite.databinding.ItemChartWeekdayBinding
import com.chronie.homemoneylite.domain.model.TimeRange
import com.chronie.homemoneylite.ui.common.collectWithLifecycle
import com.chronie.homemoneylite.ui.common.slideNavOptions
import com.chronie.homemoneylite.ui.components.showWheelDateRangePicker
import com.chronie.homemoneylite.ui.expense.ExpenseTypeLocalizer
import com.chronie.homemoneylite.ui.expense.formatDateByLocale
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@AndroidEntryPoint
class ChartsFragment : Fragment() {

    private var _binding: FragmentChartsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChartsViewModel by viewModels()

    private val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())

    private val shortDateFormatter = DateTimeFormatter.ofPattern("MM/dd", Locale.getDefault())

    private var selectedChart = ChartType.TREND
    private var lastSuccess: ChartsUiState.Success? = null
    private var currentStartStr: String = ""
    private var currentEndStr: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val savedOrdinal = savedInstanceState?.getInt(KEY_SELECTED_CHART, ChartType.TREND.ordinal)
            ?: ChartType.TREND.ordinal
        selectedChart = ChartType.entries.getOrElse(savedOrdinal) { ChartType.TREND }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChartsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.chartTypeButton.setOnClickListener { showChartTypeMenu(it) }
        binding.dateRangeButton.setOnClickListener { showTimeRangeMenu(it) }
        binding.retryButton.setOnClickListener { viewModel.refresh() }

        collectWithLifecycle(viewModel.uiState) { state -> renderState(state) }
        collectWithLifecycle(viewModel.selectedTimeRange) { /* 由 Success 重新渲染卡片标题 */ }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SELECTED_CHART, selectedChart.ordinal)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ---------- 状态渲染 ----------

    private fun renderState(state: ChartsUiState) {
        when (state) {
            is ChartsUiState.Loading -> {
                binding.scrollContent.visibility = View.GONE
                binding.errorContainer.visibility = View.GONE
                binding.progressBar.visibility = View.VISIBLE
            }
            is ChartsUiState.Error -> {
                binding.scrollContent.visibility = View.GONE
                binding.progressBar.visibility = View.GONE
                binding.errorContainer.visibility = View.VISIBLE
                binding.errorText.text = state.message
            }
            is ChartsUiState.Success -> {
                binding.progressBar.visibility = View.GONE
                binding.errorContainer.visibility = View.GONE
                binding.scrollContent.visibility = View.VISIBLE
                lastSuccess = state
                currentStartStr = state.startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                currentEndStr = state.endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                renderTimeRangeCard()
                renderSummary(state)
                renderChartSection(state)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun renderTimeRangeCard() {
        val localeTag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            resources.configuration.locales[0].toLanguageTag()
        } else {
            resources.configuration.locale.toLanguageTag()
        }
        binding.timeRangeTitle.text = getTimeRangeText(requireContext(), viewModel.selectedTimeRange.value)
        binding.timeRangeSubtitle.text =
            "${formatDateByLocale(currentStartStr)} - ${formatDateByLocale(currentEndStr)}"
    }

    private fun renderSummary(state: ChartsUiState.Success) {
        val s = state.statistics
        binding.statTotal.text = currencyFormat.format(s.totalAmount)
        binding.statCount.text = "${s.count}"
        binding.statAverage.text = currencyFormat.format(s.averageAmount)
        binding.statMedian.text = currencyFormat.format(s.medianAmount)
    }

    private fun renderChartSection(state: ChartsUiState.Success) {
        binding.chartSection.removeAllViews()
        when (selectedChart) {
            ChartType.TREND -> buildTrendCard(state)
            ChartType.CATEGORY -> buildCategoryCard(state)
            ChartType.WEEKDAY -> buildWeekdayCard(state)
        }
    }

    // ---------- 各图表卡片（MPAndroidChart 实现）----------

    private fun buildTrendCard(state: ChartsUiState.Success) {
        val (card, inner) = buildChartCard(R.string.trend_chart)
        if (state.dailyData.isEmpty()) {
            inner.addView(noDataText())
        } else {
            val chart = LineChart(requireContext()).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(300))
            }
            val primary = ContextCompat.getColor(requireContext(), R.color.brand_primary)
            val textColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
            val divider = ContextCompat.getColor(requireContext(), R.color.divider)

            val entries = state.dailyData.mapIndexed { i, d -> Entry(i.toFloat(), d.amount.toFloat()) }
            val dataSet = LineDataSet(entries, getString(R.string.daily_amount)).apply {
                color = primary
                lineWidth = 2f
                setDrawCircleHole(false)
                circleRadius = 3f
                setCircleColor(primary)
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawFilled(true)
                fillColor = primary
                fillAlpha = 40
                valueTextColor = textColor
                valueTextSize = 10f
            }
            chart.data = LineData(dataSet)
            chart.description.isEnabled = false
            chart.isScaleXEnabled = false
            chart.isScaleYEnabled = false
            chart.setPinchZoom(false)
            chart.setDrawGridBackground(false)
            chart.setExtraOffsets(8f, 8f, 8f, 8f)
            val legend = chart.legend
            legend.isEnabled = true
            legend.textSize = 11f
            legend.textColor = textColor

            val xAxis = chart.xAxis
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.textSize = 11f
            xAxis.textColor = textColor
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            xAxis.setLabelCount((state.dailyData.size).coerceAtMost(6), false)
            xAxis.valueFormatter = IndexAxisValueFormatter(
                state.dailyData.map { it.date.format(shortDateFormatter) }
            )

            val left = chart.axisLeft
            left.textSize = 11f
            left.textColor = textColor
            left.setDrawGridLines(true)
            left.gridColor = divider
            left.axisMinimum = 0f
            left.valueFormatter = currencyAxisFormatter()
            chart.axisRight.isEnabled = false

            inner.addView(chart)
        }
        binding.chartSection.addView(card)
    }

    private fun buildCategoryCard(state: ChartsUiState.Success) {
        val (card, inner) = buildChartCard(R.string.category_breakdown)
        if (state.categoryData.isEmpty()) {
            inner.addView(noDataText())
        } else {
            inner.addView(buildCategoryList(state.categoryData))
        }
        binding.chartSection.addView(card)
    }

    private fun buildWeekdayCard(state: ChartsUiState.Success) {
        val (card, inner) = buildChartCard(R.string.weekday_analysis)
        val hasData = state.weekdayData.any { it.amount > 0 }
        if (!hasData) {
            inner.addView(noDataText())
        } else {
            val chart = RadarChart(requireContext()).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(400))
            }
            val primary = ContextCompat.getColor(requireContext(), R.color.brand_primary)
            val textColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
            val divider = ContextCompat.getColor(requireContext(), R.color.divider)

            val entries = state.weekdayData.map { RadarEntry(it.amount.toFloat()) }
            val dataSet = RadarDataSet(entries, getString(R.string.weekday_analysis)).apply {
                color = primary
                fillColor = primary
                fillAlpha = 70
                lineWidth = 2f
                valueTextColor = textColor
                valueTextSize = 10f
            }
            chart.data = RadarData(dataSet)
            chart.description.isEnabled = false
            chart.legend.isEnabled = false
            chart.isRotationEnabled = false
            chart.webLineWidth = 1f
            chart.webColor = divider
            chart.webColorInner = divider
            chart.setExtraOffsets(8f, 8f, 8f, 8f)

            val xAxis = chart.xAxis
            xAxis.textSize = 12f
            xAxis.textColor = textColor
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String =
                    getWeekdayName(requireContext(), value.toInt())
            }

            val yAxis = chart.yAxis
            yAxis.textSize = 10f
            yAxis.textColor = textColor
            yAxis.axisMinimum = 0f

            chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    val idx = e?.x?.toInt() ?: return
                    val wd = state.weekdayData.getOrNull(idx) ?: return
                    navigateToWeekdayDetail(wd)
                }

                override fun onNothingSelected() {}
            })

            inner.addView(chart)

            val list = buildWeekdayList(state.weekdayData)
            inner.addView(list)
        }
        binding.chartSection.addView(card)
    }

    private fun buildChartCard(titleRes: Int): Pair<CardView, ViewGroup> {
        val card = CardView(requireContext())
        card.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        card.radius = dp(12).toFloat()
        card.cardElevation = dp(2).toFloat()

        val inner = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).let { lp ->
            LinearLayout(requireContext()).apply {
                layoutParams = lp
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(16), dp(16), dp(16))
            }
        }
        val title = TextView(requireContext())
        title.text = getString(titleRes)
        title.textSize = 16f
        title.setTypeface(null, Typeface.BOLD)
        title.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
        inner.addView(title)

        val spacer = View(requireContext())
        spacer.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(16))
        inner.addView(spacer)

        card.addView(inner)
        return card to inner
    }

    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun buildCategoryList(data: List<CategoryChartData>): View {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        data.forEach { category ->
            val itemBinding = ItemChartCategoryBinding.inflate(layoutInflater)
            itemBinding.catName.text = ExpenseTypeLocalizer.getLocalizedTypeName(requireContext(), category.type)
            itemBinding.catPct.text = String.format("%.1f%%", category.percentage)
            itemBinding.catProgress.progress = category.percentage.toInt()
            itemBinding.catDetail.text = "${currencyFormat.format(category.amount)} (${category.count} ${getString(R.string.records)})"
            container.addView(itemBinding.root)
            val spacer = View(requireContext())
            spacer.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(12))
            container.addView(spacer)
        }
        return container
    }

    @SuppressLint("DefaultLocale")
    private fun buildWeekdayList(data: List<WeekdayChartData>): View {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        data.forEach { wd ->
            if (wd.amount > 0) {
                val itemBinding = ItemChartWeekdayBinding.inflate(layoutInflater)
                itemBinding.wdName.text = getWeekdayName(requireContext(), wd.dayOfWeek)
                itemBinding.wdPct.text = String.format("%.1f%%", wd.percentage)
                itemBinding.wdAmount.text = currencyFormat.format(wd.amount)
                // 点击整行进入星期详情
                itemBinding.root.setOnClickListener { navigateToWeekdayDetail(wd) }
                itemBinding.root.isClickable = true
                container.addView(itemBinding.root)
            }
        }
        return container
    }

    private fun noDataText(): TextView {
        return TextView(requireContext()).apply {
            text = getString(R.string.no_data)
            textSize = 14f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            setPadding(0, dp(32), 0, dp(32))
            gravity = Gravity.CENTER
        }
    }

    // ---------- 下拉菜单 ----------

    private fun showChartTypeMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.setGroupCheckable(0, true, true)
        ChartType.entries.forEachIndexed { index, type ->
            val item = popup.menu.add(0, index, index, getString(type.stringRes))
            item.isChecked = selectedChart == type
        }
        popup.setOnMenuItemClickListener { item ->
            selectedChart = ChartType.entries.toTypedArray()[item.itemId]
            if (lastSuccess != null) renderChartSection(lastSuccess!!)
            true
        }
        popup.show()
    }

    private fun showTimeRangeMenu(anchor: View) {
        val ranges = listOf(
            TimeRange.THIS_WEEK, TimeRange.THIS_MONTH, TimeRange.LAST_MONTH,
            TimeRange.THIS_QUARTER, TimeRange.THIS_YEAR, TimeRange.CUSTOM
        )
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.setGroupCheckable(0, true, true)
        ranges.forEachIndexed { index, range ->
            val item = popup.menu.add(0, index, index, getTimeRangeText(requireContext(), range))
            item.isChecked = viewModel.selectedTimeRange.value == range
        }
        popup.setOnMenuItemClickListener { item ->
            val range = ranges[item.itemId]
            if (range == TimeRange.CUSTOM) showCustomRangePicker() else viewModel.selectTimeRange(range)
            true
        }
        popup.show()
    }

    private fun showCustomRangePicker() {
        val start = viewModel.customStartDate.value ?: LocalDate.now().minusMonths(1)
        val end = viewModel.customEndDate.value ?: LocalDate.now()
        showWheelDateRangePicker(
            requireContext(),
            initialStart = start,
            initialEnd = end,
            minDate = LocalDate.of(2000, 1, 1),
            maxDate = LocalDate.now()
        ) { s, e ->
            viewModel.setCustomDateRange(s, e)
        }
    }

    // ---------- 工具 ----------

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun currencyAxisFormatter(): ValueFormatter = object : ValueFormatter() {
        override fun getFormattedValue(value: Float): String = currencyFormat.format(value.toDouble())
    }

    private fun getTimeRangeText(context: android.content.Context, timeRange: TimeRange): String {
        return when (timeRange) {
            TimeRange.THIS_WEEK -> context.getString(R.string.this_week)
            TimeRange.THIS_MONTH -> context.getString(R.string.this_month)
            TimeRange.LAST_MONTH -> context.getString(R.string.last_month)
            TimeRange.THIS_QUARTER -> context.getString(R.string.this_quarter)
            TimeRange.THIS_YEAR -> context.getString(R.string.this_year)
            TimeRange.CUSTOM -> context.getString(R.string.custom_range)
        }
    }

    private fun getWeekdayName(context: android.content.Context, dayOfWeek: Int): String {
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

    private fun navigateToWeekdayDetail(wd: WeekdayChartData) {
        findNavController().navigate(
            R.id.weekdayDetailFragment,
            bundleOf(
                "dayOfWeek" to wd.dayOfWeek,
                "amount" to wd.amount.toFloat(),
                "count" to wd.count,
                "percentage" to wd.percentage,
                "startDate" to currentStartStr,
                "endDate" to currentEndStr
            ),
            slideNavOptions()
        )
    }

    private enum class ChartType(val stringRes: Int) {
        TREND(R.string.trend_chart),
        CATEGORY(R.string.category_breakdown),
        WEEKDAY(R.string.weekday_analysis)
    }

    companion object {
        private const val KEY_SELECTED_CHART = "selected_chart"
    }
}
