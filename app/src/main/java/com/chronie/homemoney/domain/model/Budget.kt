package com.chronie.homemoney.domain.model

/**
 * 预算领域模型
 */
data class Budget(
    val monthlyLimit: Double,
    val warningThreshold: Double = 0.8, // 默认80%时警告
    val isEnabled: Boolean = false
)

/**
 * 预算使用情况
 */
data class BudgetUsage(
    val monthlyLimit: Double,
    val currentSpending: Double,
    val remainingAmount: Double,
    val spendingPercentage: Double,
    val warningThreshold: Double,
    val isOverLimit: Boolean,
    val isNearLimit: Boolean,
    val dailyAverage: Double,
    val recommendedDaily: Double,
    val currentMonth: String
)

/**
 * 预算状态
 */
enum class BudgetStatus {
    NORMAL,      // 正常
    WARNING,     // 接近限额
    OVER_LIMIT   // 超出限额
}
