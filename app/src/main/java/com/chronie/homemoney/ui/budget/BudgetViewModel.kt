package com.chronie.homemoney.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronie.homemoney.domain.model.Budget
import com.chronie.homemoney.domain.model.BudgetStatus
import com.chronie.homemoney.domain.model.BudgetUsage
import com.chronie.homemoney.domain.usecase.GetBudgetUseCase
import com.chronie.homemoney.domain.usecase.GetBudgetUsageUseCase
import com.chronie.homemoney.domain.usecase.SaveBudgetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 预算管理ViewModel
 */
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val getBudgetUseCase: GetBudgetUseCase,
    private val saveBudgetUseCase: SaveBudgetUseCase,
    private val getBudgetUsageUseCase: GetBudgetUsageUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()
    
    init {
        loadBudget()
    }
    
    private fun loadBudget() {
        viewModelScope.launch {
            try {
                getBudgetUseCase().collect { budget ->
                    _uiState.update { it.copy(budget = budget, error = null) }
                    if (budget?.isEnabled == true) {
                        loadBudgetUsage()
                    } else {
                        // 清除预算使用情况
                        _uiState.update { it.copy(budgetUsage = null) }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("BudgetViewModel", "Error loading budget", e)
                _uiState.update { it.copy(error = e.message ?: "Unknown error") }
            }
        }
    }
    
    fun loadBudgetUsage() {
        viewModelScope.launch {
            try {
                android.util.Log.d("BudgetViewModel", "Loading budget usage...")
                val usage = getBudgetUsageUseCase()
                android.util.Log.d("BudgetViewModel", "Budget usage loaded: $usage")
                _uiState.update { it.copy(budgetUsage = usage, error = null) }
            } catch (e: Exception) {
                android.util.Log.e("BudgetViewModel", "Error loading budget usage", e)
                _uiState.update { it.copy(error = e.message ?: "Failed to load budget usage", budgetUsage = null) }
            }
        }
    }
    
    fun saveBudget(monthlyLimit: Double, warningThreshold: Double, isEnabled: Boolean) {
        viewModelScope.launch {
            try {
                val budget = Budget(
                    monthlyLimit = monthlyLimit,
                    warningThreshold = warningThreshold,
                    isEnabled = isEnabled
                )
                saveBudgetUseCase(budget)
                _uiState.update { it.copy(error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
    
    fun toggleBudgetEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val currentBudget = _uiState.value.budget
                val budget = Budget(
                    monthlyLimit = currentBudget?.monthlyLimit ?: 0.0,
                    warningThreshold = currentBudget?.warningThreshold ?: 0.8,
                    isEnabled = enabled
                )
                saveBudgetUseCase(budget)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
    
    fun getBudgetStatus(): BudgetStatus {
        val usage = _uiState.value.budgetUsage ?: return BudgetStatus.NORMAL
        return when {
            usage.isOverLimit -> BudgetStatus.OVER_LIMIT
            usage.isNearLimit -> BudgetStatus.WARNING
            else -> BudgetStatus.NORMAL
        }
    }
    
    fun refresh() {
        loadBudgetUsage()
    }
}

/**
 * 预算UI状态
 */
data class BudgetUiState(
    val budget: Budget? = null,
    val budgetUsage: BudgetUsage? = null,
    val error: String? = null
)
