package com.chronie.homemoney.domain.model

import java.time.LocalDateTime

/**
 * AI 识别的支出记录
 */
data class AIExpenseRecord(
    val id: String = "",
    val type: ExpenseType,
    val amount: Double,
    val date: String,
    val remark: String,
    val isEdited: Boolean = false,
    val isValid: Boolean = true
) {
    /**
     * 转换为普通支出记录
     */
    fun toExpense(): Expense {
        return Expense(
            id = id,
            type = type,
            amount = amount,
            date = date,
            remark = remark,
            isSynced = false
        )
    }
}
