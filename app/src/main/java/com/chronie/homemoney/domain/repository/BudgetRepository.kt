package com.chronie.homemoney.domain.repository

import com.chronie.homemoney.domain.model.Budget
import com.chronie.homemoney.domain.model.BudgetUsage
import kotlinx.coroutines.flow.Flow

/**
 * 预算仓库接口
 */
interface BudgetRepository {
    
    /**
     * 获取预算设置
     */
    fun getBudget(): Flow<Budget?>
    
    /**
     * 获取预算设置（一次性）
     */
    suspend fun getBudgetOnce(): Budget?
    
    /**
     * 保存预算设置
     */
    suspend fun saveBudget(budget: Budget)
    
    /**
     * 获取当月预算使用情况
     */
    suspend fun getCurrentMonthUsage(): BudgetUsage?
    
    /**
     * 启用/禁用预算功能
     */
    suspend fun toggleBudgetEnabled(enabled: Boolean)
}
