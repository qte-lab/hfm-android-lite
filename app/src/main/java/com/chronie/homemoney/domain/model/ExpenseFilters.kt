package com.chronie.homemoney.domain.model

import java.time.LocalDate

/**
 * 支出筛选条件
 */
data class ExpenseFilters(
    val keyword: String? = null,
    val type: ExpenseType? = null,
    val month: String? = null,  // 格式: YYYY-MM
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val sortBy: SortOption = SortOption.DATE_DESC
)

/**
 * 排序选项
 */
enum class SortOption {
    DATE_ASC,
    DATE_DESC,
    AMOUNT_ASC,
    AMOUNT_DESC
}
