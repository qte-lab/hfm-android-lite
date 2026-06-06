package com.chronie.homemoneylite.domain.usecase

import com.chronie.homemoneylite.domain.model.Budget
import com.chronie.homemoneylite.domain.repository.BudgetRepository
import javax.inject.Inject

/**
 * 保存预算设置用例
 */
class SaveBudgetUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository
) {
    suspend operator fun invoke(budget: Budget) {
        budgetRepository.saveBudget(budget)
    }
}
