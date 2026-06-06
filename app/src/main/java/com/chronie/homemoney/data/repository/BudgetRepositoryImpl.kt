package com.chronie.homemoney.data.repository

import com.chronie.homemoney.data.local.dao.BudgetDao
import com.chronie.homemoney.data.local.dao.ExpenseDao
import com.chronie.homemoney.data.local.entity.BudgetEntity
import com.chronie.homemoney.domain.model.Budget
import com.chronie.homemoney.domain.model.BudgetUsage
import com.chronie.homemoney.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 预算仓库实现
 */
@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao,
    private val expenseDao: ExpenseDao
) : BudgetRepository {
    
    override fun getBudget(): Flow<Budget?> {
        return budgetDao.getBudget().map { entity ->
            entity?.let {
                Budget(
                    monthlyLimit = it.monthlyLimit,
                    warningThreshold = it.warningThreshold,
                    isEnabled = it.isEnabled
                )
            }
        }
    }
    
    override suspend fun getBudgetOnce(): Budget? {
        return budgetDao.getBudgetOnce()?.let {
            Budget(
                monthlyLimit = it.monthlyLimit,
                warningThreshold = it.warningThreshold,
                isEnabled = it.isEnabled
            )
        }
    }
    
    override suspend fun saveBudget(budget: Budget) {
        val entity = BudgetEntity(
            id = 1,
            monthlyLimit = budget.monthlyLimit,
            warningThreshold = budget.warningThreshold,
            isEnabled = budget.isEnabled,
            updatedAt = System.currentTimeMillis()
        )
        budgetDao.insertBudget(entity)
    }
    
    override suspend fun getCurrentMonthUsage(): BudgetUsage? {
        return try {
            android.util.Log.d("BudgetRepository", "Getting current month usage...")
            
            val budget = getBudgetOnce()
            if (budget == null) {
                android.util.Log.d("BudgetRepository", "No budget found")
                return null
            }
            
            if (!budget.isEnabled) {
                android.util.Log.d("BudgetRepository", "Budget is not enabled")
                return null
            }
            
            // 获取当月的起始和结束日期字符串
            val now = java.time.LocalDate.now()
            val yearMonth = java.time.YearMonth.from(now)
            val startOfMonth = yearMonth.atDay(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val endOfMonth = yearMonth.atEndOfMonth().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            
            android.util.Log.d("BudgetRepository", "Querying expenses from $startOfMonth to $endOfMonth")
            
            // 查询当月支出总额
            val currentSpending: Double = expenseDao.getTotalAmountByDateRange(startOfMonth, endOfMonth) ?: 0.0
            
            android.util.Log.d("BudgetRepository", "Current spending: $currentSpending")
        val remainingAmount: Double = budget.monthlyLimit - currentSpending
        val spendingPercentage = if (budget.monthlyLimit > 0) {
            (currentSpending / budget.monthlyLimit) * 100
        } else {
            0.0
        }
        
        val isOverLimit = currentSpending > budget.monthlyLimit
        val isNearLimit = spendingPercentage >= (budget.warningThreshold * 100)
        
        // 计算日均消费
        val currentDay = now.dayOfMonth
        val dailyAverage = if (currentDay > 0) currentSpending / currentDay else 0.0
        
        // 计算建议日均消费
        val daysInMonth = yearMonth.lengthOfMonth()
        val remainingDays = daysInMonth - currentDay
        val recommendedDaily = if (remainingDays > 0) remainingAmount / remainingDays else 0.0
        
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM")
            val currentMonth = now.format(formatter)
            
            val usage = BudgetUsage(
                monthlyLimit = budget.monthlyLimit,
                currentSpending = currentSpending,
                remainingAmount = remainingAmount,
                spendingPercentage = spendingPercentage,
                warningThreshold = budget.warningThreshold,
                isOverLimit = isOverLimit,
                isNearLimit = isNearLimit,
                dailyAverage = dailyAverage,
                recommendedDaily = recommendedDaily,
                currentMonth = currentMonth
            )
            
            android.util.Log.d("BudgetRepository", "Budget usage calculated successfully: $usage")
            usage
        } catch (e: Exception) {
            android.util.Log.e("BudgetRepository", "Error calculating budget usage", e)
            null
        }
    }
    
    override suspend fun toggleBudgetEnabled(enabled: Boolean) {
        val current = budgetDao.getBudgetOnce()
        if (current != null) {
            budgetDao.updateBudget(current.copy(isEnabled = enabled))
        } else {
            // 如果没有预算设置，创建一个默认的
            val defaultBudget = BudgetEntity(
                id = 1,
                monthlyLimit = 0.0,
                warningThreshold = 0.8,
                isEnabled = enabled,
                updatedAt = System.currentTimeMillis()
            )
            budgetDao.insertBudget(defaultBudget)
        }
    }
}
