package com.chronie.homemoneylite.ui.charts

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.databinding.FragmentChartsBinding
import com.chronie.homemoneylite.databinding.ItemChartCategoryBinding
import com.chronie.homemoneylite.databinding.ItemChartWeekdayBinding
import com.chronie.homemoneylite.domain.model.TimeRange
import com.chronie.homemoneylite.ui.charts.view.CategoryBarChartView
import com.chronie.homemoneylite.ui.charts.view.LineChartView
import com.chronie.homemoneylite.ui.charts.view.WeekdayRadarChartView
import com.chronie.homemoneylite.ui.common.collectWithLifecycle
import com.chronie.homemoneylite.ui.components.showWheelDateRangePicker
import com.chronie.homemoneylite.ui.expense.ExpenseTypeLocalizer
import com.chronie.homemoneylite.ui.expense.formatDateByLocale
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

    private var selectedChart = ChartType.TREND
    private var lastSuccess: ChartsUiState.Success? = null
    private var currentStartStr: String = ""
    private var currentEndStr: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedChart = ChartType.values()[
            savedInstanceState?.getInt(KEY_SELECTED_CHART, ChartType.TREND.ordinal)
                ?: ChartType.TREND.ordinal
        ]
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
                renderTimeRangeCard(state)
                renderSummary(state)
                renderChartSection(state)
            }
        }
    }

    private fun renderTimeRangeCard(state: ChartsUiState.Success) {
        val localeTag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            resources.configuration.locales[0].toLanguageTag()
        } else {
            resources.configuration.locale.toLanguageTag()
        }
        binding.timeRangeTitle.text = getTimeRangeText(requireContext(), viewModel.selectedTimeRange.value)
        binding.timeRangeSubtitle.text =
            "${formatDateByLocale(currentStartStr, localeTag)} - ${formatDateByLocale(currentEndStr, localeTag)}"
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

    // ---------- 各图表卡片 ----------

    private fun buildTrendCard(state: ChartsUiState.Success) {
        val (card, inner) = buildChartCard(R.string.trend_chart)
        if (state.dailyData.isEmpty()) {
            inner.addView(noDataText())
        } else {
            val chart = LineChartView(requireContext())
            chart.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(300)
            )
            chart.setData(state.dailyData, currencyFormat)
            inner.addView(chart)
        }
        binding.chartSection.addView(card)
    }

    private fun buildCategoryCard(state: ChartsUiState.Success) {
        val (card, inner) = buildChartCard(R.string.category_breakdown)
        if (state.categoryData.isEmpty()) {
            inner.addView(noDataText())
        } else {
            val scroll = android.widget.HorizontalScrollView(requireContext())
            scroll.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            val widthDp = (state.categoryData.size * 64).coerceAtLeast(320)
            val chart = CategoryBarChartView(requireContext())
            chart.layoutParams = ViewGroup.LayoutParams(dp(widthDp), dp(320))
            chart.setData(state.categoryData, currencyFormat)
            scroll.addView(chart)
            inner.addView(scroll)

            val list = buildCategoryList(state.categoryData)
            inner.addView(list)
        }
        binding.chartSection.addView(card)
    }

    private fun buildWeekdayCard(state: ChartsUiState.Success) {
        val (card, inner) = buildChartCard(R.string.weekday_analysis)
        val hasData = state.weekdayData.any { it.amount > 0 }
        if (!hasData) {
            inner.addView(noDataText())
        } else {
            val radar = WeekdayRadarChartView(requireContext())
            radar.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(400)
            )
            radar.setData(state.weekdayData)
            radar.onWeekdayClick = { wd ->
                findNavController().navigate(
                    R.id.weekdayDetailFragment,
                    bundleOf(
                        "dayOfWeek" to wd.dayOfWeek,
                        "amount" to wd.amount.toFloat(),
                        "count" to wd.count,
                        "percentage" to wd.percentage,
                        "startDate" to currentStartStr,
                        "endDate" to currentEndStr
                    )
                )
            }
            inner.addView(radar)

            val list = buildWeekdayList(state.weekdayData)
            inner.addView(list)
        }
        binding.chartSection.addView(card)
    }

    private fun buildChartCard(titleRes: Int): Pair<com.google.android.material.card.MaterialCardView, ViewGroup> {
        val card = com.google.android.material.card.MaterialCardView(requireContext())
        card.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        card.radius = dp(12).toFloat()
        card.cardElevation = dp(2).toFloat()

        val inner = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).let { lp ->
            android.widget.LinearLayout(requireContext()).apply {
                layoutParams = lp
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(dp(16), dp(16), dp(16), dp(16))
            }
        }
        val title = TextView(requireContext())
        title.text = getString(titleRes)
        title.textSize = 16f
        title.setTypeface(null, android.graphics.Typeface.BOLD)
        title.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_primary))
        inner.addView(title)

        val spacer = View(requireContext())
        spacer.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(16))
        inner.addView(spacer)

        card.addView(inner)
        return card to inner
    }

    private fun buildCategoryList(data: List<CategoryChartData>): View {
        val container = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        data.forEach { category ->
            val itemBinding = ItemChartCategoryBinding.inflate(layoutInflater)
            itemBinding.catName.text = ExpenseTypeLocalizer.getLocalizedTypeName(requireContext(), category.type)
            itemBinding.catPct.text = String.format("%.1f%%", category.percentage)
            itemBinding.catProgress.setProgressCompat(category.percentage.toInt(), false)
            itemBinding.catDetail.text = "${currencyFormat.format(category.amount)} (${category.count} ${getString(R.string.records)})"
            container.addView(itemBinding.root)
            val spacer = View(requireContext())
            spacer.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(12))
            container.addView(spacer)
        }
        return container
    }

    private fun buildWeekdayList(data: List<WeekdayChartData>): View {
        val container = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
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
                container.addView(itemBinding.root)
            }
        }
        return container
    }

    private fun noDataText(): TextView {
        return TextView(requireContext()).apply {
            text = getString(R.string.no_data)
            textSize = 14f
            setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_secondary))
            setPadding(0, dp(32), 0, dp(32))
            gravity = android.view.Gravity.CENTER
        }
    }

    // ---------- 下拉菜单 ----------

    private fun showChartTypeMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.setGroupCheckable(0, true, true)
        ChartType.values().forEachIndexed { index, type ->
            val item = popup.menu.add(0, index, index, getString(type.stringRes))
            item.isChecked = selectedChart == type
        }
        popup.setOnMenuItemClickListener { item ->
            selectedChart = ChartType.values()[item.itemId]
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

    private enum class ChartType(val stringRes: Int) {
        TREND(R.string.trend_chart),
        CATEGORY(R.string.category_breakdown),
        WEEKDAY(R.string.weekday_analysis)
    }

    companion object {
        private const val KEY_SELECTED_CHART = "selected_chart"
    }
}
