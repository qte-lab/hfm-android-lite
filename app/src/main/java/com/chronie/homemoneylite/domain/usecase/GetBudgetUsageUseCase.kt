package com.chronie.homemoneylite.domain.usecase

import com.chronie.homemoneylite.domain.model.BudgetUsage
import com.chronie.homemoneylite.domain.repository.BudgetRepository
import javax.inject.Inject

/**
 * 获取预算使用情况用例
 */
class GetBudgetUsageUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository
) {
    suspend operator fun invoke(): BudgetUsage? {
        return budgetRepository.getCurrentMonthUsage()
    }
}
