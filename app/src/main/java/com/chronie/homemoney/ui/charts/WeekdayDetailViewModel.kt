package com.chronie.homemoney.ui.charts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronie.homemoney.domain.model.ExpenseFilters
import com.chronie.homemoney.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class WeekdayDetailViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<WeekdayDetailUiState>(WeekdayDetailUiState.Loading)
    val uiState: StateFlow<WeekdayDetailUiState> = _uiState.asStateFlow()
    
    private val dayOfWeek: Int = savedStateHandle.get<Int>("dayOfWeek") ?: 0
    private val startDate: LocalDate = savedStateHandle.get<String>("startDate")?.let { LocalDate.parse(it) } ?: LocalDate.now().minusMonths(1)
    private val endDate: LocalDate = savedStateHandle.get<String>("endDate")?.let { LocalDate.parse(it) } ?: LocalDate.now()
    
    init {
        loadWeekdayDetail()
    }
    
    fun refresh() {
        loadWeekdayDetail()
    }
    
    private fun loadWeekdayDetail() {
        viewModelScope.launch {
            _uiState.value = WeekdayDetailUiState.Loading
            
            try {
                val filters = ExpenseFilters(
                    startDate = startDate,
                    endDate = endDate
                )
                
                val expensesResult = expenseRepository.getExpensesList(
                    page = 1,
                    limit = 100000,
                    filters = filters
                )
                
                if (expensesResult.isSuccess) {
                    val expenses = expensesResult.getOrNull()!!
                    
                    val dayExpenses = expenses.filter { expense ->
                        try {
                            val expenseDate = java.time.LocalDate.parse(expense.date)
                            val expenseDayOfWeek = expenseDate.dayOfWeek.value % 7
                            expenseDayOfWeek == dayOfWeek
                        } catch (e: Exception) {
                            false
                        }
                    }
                    
                    val totalAmount = dayExpenses.sumOf { it.amount }
                    
                    val categoryBreakdown = if (dayExpenses.isNotEmpty()) {
                        val expensesByType = dayExpenses.groupBy { it.type }
                        expensesByType.map { (type, typeExpenses) ->
                            val typeAmount = typeExpenses.sumOf { it.amount }
                            CategoryChartData(
                                type = type.name,
                                amount = typeAmount,
                                count = typeExpenses.size,
                                percentage = if (totalAmount > 0) (typeAmount / totalAmount * 100).toFloat() else 0f
                            )
                        }.sortedByDescending { it.amount }
                    } else {
                        emptyList()
                    }
                    
                    _uiState.value = WeekdayDetailUiState.Success(
                        categoryBreakdown = categoryBreakdown
                    )
                } else {
                    _uiState.value = WeekdayDetailUiState.Error(
                        expensesResult.exceptionOrNull()?.message ?: "Failed to load data"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = WeekdayDetailUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed class WeekdayDetailUiState {
    object Loading : WeekdayDetailUiState()
    data class Success(
        val categoryBreakdown: List<CategoryChartData>
    ) : WeekdayDetailUiState()
    data class Error(val message: String) : WeekdayDetailUiState()
}