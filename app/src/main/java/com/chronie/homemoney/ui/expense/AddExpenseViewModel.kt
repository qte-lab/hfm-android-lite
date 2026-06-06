package com.chronie.homemoney.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronie.homemoney.domain.model.Expense
import com.chronie.homemoney.domain.model.ExpenseType
import com.chronie.homemoney.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val syncScheduler: com.chronie.homemoney.data.sync.SyncScheduler,
    val checkLoginStatusUseCase: com.chronie.homemoney.domain.usecase.CheckLoginStatusUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AddExpenseUiState())
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()
    
    fun setType(type: ExpenseType) {
        _uiState.update { it.copy(
            selectedType = type,
            typeError = null
        ) }
    }
    
    fun setAmount(amount: String) {
        _uiState.update { it.copy(
            amount = amount,
            amountError = null
        ) }
    }
    
    fun setDate(date: LocalDate) {
        _uiState.update { it.copy(
            selectedDate = date,
            dateError = null
        ) }
    }
    
    fun setRemark(remark: String) {
        _uiState.update { it.copy(remark = remark) }
    }
    
    fun loadExpenseForEdit(expenseId: String) {
        _uiState.update { it.copy(isSaving = true) }
        
        viewModelScope.launch {
            try {
                val expenseResult = expenseRepository.getExpenseById(expenseId)
                
                if (expenseResult.isSuccess) {
                    val expense = expenseResult.getOrThrow()
                    _uiState.update {
                        it.copy(
                            expenseId = expenseId,
                            selectedType = expense.type,
                            amount = expense.amount.toString(),
                            selectedDate = LocalDate.parse(expense.date),
                            remark = expense.remark ?: "",
                            isSaving = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saveError = expenseResult.exceptionOrNull()?.message ?: "Expense not found"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveError = e.message ?: "Unknown error"
                    )
                }
            }
        }
    }
    
    fun saveExpense(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (!validateForm()) {
            return
        }
        
        val state = _uiState.value
        
        _uiState.update { it.copy(isSaving = true, saveError = null) }
        
        viewModelScope.launch {
            try {
                val dateStr = state.selectedDate.toString()
                
                val expenseId = state.expenseId ?: UUID.randomUUID().toString()
                
                val expense = Expense(
                    id = expenseId,
                    type = state.selectedType!!,
                    amount = state.amount.toDouble(),
                    date = dateStr,
                    remark = state.remark.ifBlank { null },
                    isSynced = false
                )
                
                val result = if (state.expenseId != null) {
                    expenseRepository.updateExpense(expense)
                } else {
                    expenseRepository.addExpense(expense)
                }
                
                if (result.isSuccess) {
                    _uiState.update { it.copy(isSaving = false) }
                    
                    try {
                        syncScheduler.triggerImmediateSync()
                    } catch (e: Exception) {
                        android.util.Log.w("AddExpenseViewModel", "Failed to trigger sync after saving expense", e)
                    }
                    
                    onSuccess()
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    _uiState.update { it.copy(
                        isSaving = false,
                        saveError = error
                    ) }
                    onError(error)
                }
            } catch (e: Exception) {
                val error = e.message ?: "Unknown error"
                _uiState.update { it.copy(
                    isSaving = false,
                    saveError = error
                ) }
                onError(error)
            }
        }
    }
    
    private fun validateForm(): Boolean {
        val state = _uiState.value
        var isValid = true
        
        if (state.selectedType == null) {
            _uiState.update { it.copy(typeError = "TYPE_REQUIRED") }
            isValid = false
        }
        
        if (state.amount.isBlank()) {
            _uiState.update { it.copy(amountError = "AMOUNT_REQUIRED") }
            isValid = false
        } else {
            val amountValue = state.amount.toDoubleOrNull()
            if (amountValue == null || amountValue <= 0) {
                _uiState.update { it.copy(amountError = "AMOUNT_INVALID") }
                isValid = false
            }
        }
        
        return isValid
    }
    
    fun resetState() {
        _uiState.value = AddExpenseUiState()
    }
}

data class AddExpenseUiState(
    val expenseId: String? = null,
    val selectedType: ExpenseType? = null,
    val amount: String = "",
    val selectedDate: LocalDate = LocalDate.now(),
    val remark: String = "",
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val typeError: String? = null,
    val amountError: String? = null,
    val dateError: String? = null
)
