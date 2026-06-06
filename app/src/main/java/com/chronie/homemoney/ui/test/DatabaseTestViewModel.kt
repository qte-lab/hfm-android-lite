package com.chronie.homemoney.ui.test

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronie.homemoney.R
import com.chronie.homemoney.data.local.dao.ExpenseDao
import com.chronie.homemoney.data.local.entity.ExpenseEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DatabaseTestViewModel @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val application: Application
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DatabaseTestUiState())
    val uiState: StateFlow<DatabaseTestUiState> = _uiState.asStateFlow()
    
    init {
        loadExpenses()
    }
    
    private fun loadExpenses() {
        viewModelScope.launch {
            try {
                expenseDao.getAllExpenses()
                    .catch { e ->
                        _uiState.update { it.copy(
                            message = application.getString(R.string.load_failed, e.message ?: ""),
                            isError = true
                        ) }
                    }
                    .collect { expenses ->
                        val count = expenses.size
                        val totalAmount = expenses.sumOf { it.amount }
                        _uiState.update { it.copy(
                            expenses = expenses,
                            expenseCount = count,
                            totalAmount = totalAmount,
                            message = null,
                            isError = false
                        ) }
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    message = application.getString(R.string.load_failed, e.message ?: ""),
                    isError = true
                ) }
            }
        }
    }
    
    fun addTestExpense() {
        viewModelScope.launch {
            try {
                val types = listOf("餐饮", "交通", "购物", "娱乐", "医疗", "其他")
                val remarks = listOf("早餐", "午餐", "晚餐", "打车", "地铁", "购物", "看电影", "买药")
                
                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val uuid = UUID.randomUUID().toString()
                
                val expense = ExpenseEntity(
                    id = uuid,
                    type = types.random(),
                    remark = remarks.random(),
                    amount = (10..200).random().toDouble(),
                    date = currentDate,
                    isSynced = false
                )
                
                expenseDao.insertExpense(expense)
                
                _uiState.update { it.copy(
                    message = application.getString(R.string.add_success),
                    isError = false
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    message = application.getString(R.string.add_failed, e.message ?: ""),
                    isError = true
                ) }
            }
        }
    }
    
    fun clearAllExpenses() {
        viewModelScope.launch {
            try {
                expenseDao.deleteAllExpenses()
                
                _uiState.update { it.copy(
                    message = application.getString(R.string.clear_success),
                    isError = false
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    message = application.getString(R.string.clear_failed, e.message ?: ""),
                    isError = true
                ) }
            }
        }
    }
    
    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}

data class DatabaseTestUiState(
    val expenses: List<ExpenseEntity> = emptyList(),
    val expenseCount: Int = 0,
    val totalAmount: Double = 0.0,
    val message: String? = null,
    val isError: Boolean = false
)
