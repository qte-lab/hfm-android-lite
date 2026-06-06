package com.chronie.homemoney.domain.model

/**
 * 支出统计数据模型
 */
data class ExpenseStatistics(
    val count: Int,
    val totalAmount: Double,
    val averageAmount: Double,
    val medianAmount: Double,
    val minAmount: Double = 0.0,
    val maxAmount: Double = 0.0
)

/**
 * 时间范围类型
 */
enum class TimeRange {
    THIS_WEEK,
    THIS_MONTH,
    LAST_MONTH,
    THIS_QUARTER,
    THIS_YEAR,
    CUSTOM
}
