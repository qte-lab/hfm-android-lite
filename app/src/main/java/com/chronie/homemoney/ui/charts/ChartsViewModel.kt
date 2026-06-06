package com.chronie.homemoney.ui.charts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronie.homemoney.domain.model.Expense
import com.chronie.homemoney.domain.model.ExpenseFilters
import com.chronie.homemoney.domain.model.ExpenseStatistics
import com.chronie.homemoney.domain.model.TimeRange
import com.chronie.homemoney.domain.repository.ExpenseRepository
import com.chronie.homemoney.domain.usecase.GetStatisticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

/**
 * 图表界面 ViewModel
 */
@HiltViewModel
class ChartsViewModel @Inject constructor(
    private val getStatisticsUseCase: GetStatisticsUseCase,
    private val expenseRepository: ExpenseRepository,
    val checkLoginStatusUseCase: com.chronie.homemoney.domain.usecase.CheckLoginStatusUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<ChartsUiState>(ChartsUiState.Loading)
    val uiState: StateFlow<ChartsUiState> = _uiState.asStateFlow()
    
    private val _selectedTimeRange = MutableStateFlow(TimeRange.THIS_MONTH)
    val selectedTimeRange: StateFlow<TimeRange> = _selectedTimeRange.asStateFlow()
    
    private val _customStartDate = MutableStateFlow<LocalDate?>(null)
    val customStartDate: StateFlow<LocalDate?> = _customStartDate.asStateFlow()
    
    private val _customEndDate = MutableStateFlow<LocalDate?>(null)
    val customEndDate: StateFlow<LocalDate?> = _customEndDate.asStateFlow()
    
    init {
        loadStatistics()
    }
    
    fun selectTimeRange(timeRange: TimeRange) {
        android.util.Log.d("ChartsViewModel", "Time range changed to: $timeRange")
        _selectedTimeRange.value = timeRange
        loadStatistics()
    }
    
    fun setCustomDateRange(startDate: LocalDate, endDate: LocalDate) {
        _customStartDate.value = startDate
        _customEndDate.value = endDate
        _selectedTimeRange.value = TimeRange.CUSTOM
        loadStatistics()
    }
    
    fun setCustomStartDate(startDate: LocalDate) {
        _customStartDate.value = startDate
    }
    
    fun setCustomEndDate(endDate: LocalDate) {
        _customEndDate.value = endDate
    }
    
    fun refresh() {
        loadStatistics()
    }
    
    private fun loadStatistics() {
        viewModelScope.launch {
            _uiState.value = ChartsUiState.Loading
            
            try {
                val (startDate, endDate) = getDateRange()
                val filters = ExpenseFilters(
                    startDate = startDate,
                    endDate = endDate
                )
                
                // 获取统计数据
                val statisticsResult = getStatisticsUseCase(filters)
                
                if (statisticsResult.isSuccess) {
                    val statistics = statisticsResult.getOrNull()!!
                    
                    // 获取详细的支出列表用于生成图表数据
                    val expensesResult = expenseRepository.getExpensesList(
                        page = 1,
                        limit = 10000, // 获取所有数据
                        filters = filters
                    )
                    
                    if (expensesResult.isSuccess) {
                        val expenses = expensesResult.getOrNull()!!
                        
                        // 生成每日数据
                        val dailyData = generateDailyData(expenses, startDate, endDate)
                        
                        // 生成分类数据
                        val categoryData = generateCategoryData(expenses)
                        
                        // 生成星期数据
                        val weekdayData = generateWeekdayData(expenses)
                        
                        android.util.Log.d("ChartsViewModel", "Loaded data: expenses=${expenses.size}, dailyData=${dailyData.size}, categoryData=${categoryData.size}, weekdayData=${weekdayData.size}, stats=${statistics.totalAmount}")
                        
                        _uiState.value = ChartsUiState.Success(
                            statistics = statistics,
                            dailyData = dailyData,
                            categoryData = categoryData,
                            weekdayData = weekdayData,
                            startDate = startDate,
                            endDate = endDate
                        )
                    } else {
                        _uiState.value = ChartsUiState.Error(
                            expensesResult.exceptionOrNull()?.message ?: "Failed to load expenses"
                        )
                    }
                } else {
                    _uiState.value = ChartsUiState.Error(
                        statisticsResult.exceptionOrNull()?.message ?: "Failed to load statistics"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ChartsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    private fun getDateRange(): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        
        val range = when (_selectedTimeRange.value) {
            TimeRange.THIS_WEEK -> {
                val startOfWeek = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                val endOfWeek = today.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY))
                Pair(startOfWeek, endOfWeek)
            }
            TimeRange.THIS_MONTH -> {
                val startOfMonth = today.with(TemporalAdjusters.firstDayOfMonth())
                val endOfMonth = today.with(TemporalAdjusters.lastDayOfMonth())
                Pair(startOfMonth, endOfMonth)
            }
            TimeRange.LAST_MONTH -> {
                val lastMonth = today.minusMonths(1)
                val startOfLastMonth = lastMonth.with(TemporalAdjusters.firstDayOfMonth())
                val endOfLastMonth = lastMonth.with(TemporalAdjusters.lastDayOfMonth())
                Pair(startOfLastMonth, endOfLastMonth)
            }
            TimeRange.THIS_QUARTER -> {
                val currentMonth = today.monthValue
                val quarterStartMonth = ((currentMonth - 1) / 3) * 3 + 1
                val startOfQuarter = today.withMonth(quarterStartMonth).with(TemporalAdjusters.firstDayOfMonth())
                val endOfQuarter = startOfQuarter.plusMonths(2).with(TemporalAdjusters.lastDayOfMonth())
                Pair(startOfQuarter, endOfQuarter)
            }
            TimeRange.THIS_YEAR -> {
                val startOfYear = today.with(TemporalAdjusters.firstDayOfYear())
                val endOfYear = today.with(TemporalAdjusters.lastDayOfYear())
                Pair(startOfYear, endOfYear)
            }
            TimeRange.CUSTOM -> {
                val start = _customStartDate.value ?: today.minusMonths(1)
                val end = _customEndDate.value ?: today
                Pair(start, end)
            }
        }
        
        android.util.Log.d("ChartsViewModel", "Date range: ${range.first} to ${range.second}")
        return range
    }
    
    private fun generateDailyData(
        expenses: List<Expense>,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailyChartData> {
        // 按日期分组
        val expensesByDate = expenses.groupBy { LocalDate.parse(it.date) }
        
        // 生成日期范围内的所有日期
        val dailyData = mutableListOf<DailyChartData>()
        var currentDate = startDate
        
        while (!currentDate.isAfter(endDate)) {
            val dayExpenses = expensesByDate[currentDate] ?: emptyList()
            val totalAmount = dayExpenses.sumOf { it.amount }
            
            dailyData.add(
                DailyChartData(
                    date = currentDate,
                    amount = totalAmount,
                    count = dayExpenses.size
                )
            )
            
            currentDate = currentDate.plusDays(1)
        }
        
        return dailyData
    }
    
    private fun generateCategoryData(expenses: List<Expense>): List<CategoryChartData> {
        if (expenses.isEmpty()) return emptyList()
        
        // 按类型分组
        val expensesByType = expenses.groupBy { it.type }
        val totalAmount = expenses.sumOf { it.amount }
        
        return expensesByType.map { (type, typeExpenses) ->
            val typeAmount = typeExpenses.sumOf { it.amount }
            CategoryChartData(
                type = type.name,
                amount = typeAmount,
                count = typeExpenses.size,
                percentage = if (totalAmount > 0) (typeAmount / totalAmount * 100).toFloat() else 0f
            )
        }.sortedByDescending { it.amount }
    }
    
    private fun generateWeekdayData(expenses: List<Expense>): List<WeekdayChartData> {
        if (expenses.isEmpty()) {
            // 返回7天的空数据（周日到周六）
            return (0..6).map { dayOfWeek ->
                WeekdayChartData(
                    dayOfWeek = dayOfWeek,
                    amount = 0.0,
                    count = 0,
                    percentage = 0f,
                    categoryBreakdown = emptyList()
                )
            }
        }
        
        // 按星期几分组（0=周日, 1=周一, ..., 6=周六）
        val expensesByWeekday = expenses.groupBy { expense ->
            val dayOfWeek = LocalDate.parse(expense.date).dayOfWeek.value % 7 // 转换为0-6，周日为0
            dayOfWeek
        }
        
        val totalAmount = expenses.sumOf { it.amount }
        
        // 生成7天的数据（周日到周六）
        return (0..6).map { dayOfWeek ->
            val dayExpenses = expensesByWeekday[dayOfWeek] ?: emptyList()
            val dayAmount = dayExpenses.sumOf { it.amount }
            
            // 生成该星期的类型占比
            val categoryBreakdown = if (dayExpenses.isNotEmpty()) {
                val expensesByType = dayExpenses.groupBy { it.type }
                expensesByType.map { (type, typeExpenses) ->
                    val typeAmount = typeExpenses.sumOf { it.amount }
                    CategoryChartData(
                        type = type.name,
                        amount = typeAmount,
                        count = typeExpenses.size,
                        percentage = if (dayAmount > 0) (typeAmount / dayAmount * 100).toFloat() else 0f
                    )
                }.sortedByDescending { it.amount }
            } else {
                emptyList()
            }
            
            WeekdayChartData(
                dayOfWeek = dayOfWeek,
                amount = dayAmount,
                count = dayExpenses.size,
                percentage = if (totalAmount > 0) (dayAmount / totalAmount * 100).toFloat() else 0f,
                categoryBreakdown = categoryBreakdown
            )
        }
    }
}

/**
 * 图表界面状态
 */
sealed class ChartsUiState {
    object Loading : ChartsUiState()
    data class Success(
        val statistics: ExpenseStatistics,
        val dailyData: List<DailyChartData>,
        val categoryData: List<CategoryChartData>,
        val weekdayData: List<WeekdayChartData>,
        val startDate: LocalDate,
        val endDate: LocalDate
    ) : ChartsUiState()
    data class Error(val message: String) : ChartsUiState()
}

/**
 * 每日图表数据
 */
data class DailyChartData(
    val date: LocalDate,
    val amount: Double,
    val count: Int
)

/**
 * 分类图表数据
 */
data class CategoryChartData(
    val type: String,
    val amount: Double,
    val count: Int,
    val percentage: Float
)

/**
 * 星期图表数据
 */
data class WeekdayChartData(
    val dayOfWeek: Int, // 0=周日, 1=周一, ..., 6=周六
    val amount: Double,
    val count: Int,
    val percentage: Float,
    val categoryBreakdown: List<CategoryChartData> // 该星期的类型占比
)
