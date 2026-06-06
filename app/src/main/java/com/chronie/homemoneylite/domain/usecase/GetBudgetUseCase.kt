package com.chronie.homemoneylite.domain.usecase

import com.chronie.homemoneylite.domain.model.Budget
import com.chronie.homemoneylite.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取预算设置用例
 */
class GetBudgetUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository
) {
    operator fun invoke(): Flow<Budget?> {
        return budgetRepository.getBudget()
    }
}
