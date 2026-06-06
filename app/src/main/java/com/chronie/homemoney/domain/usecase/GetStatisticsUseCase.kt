package com.chronie.homemoney.domain.usecase

import com.chronie.homemoney.domain.model.ExpenseFilters
import com.chronie.homemoney.domain.model.ExpenseStatistics
import com.chronie.homemoney.domain.repository.ExpenseRepository
import javax.inject.Inject

/**
 * 获取支出统计数据用例
 */
class GetStatisticsUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    suspend operator fun invoke(filters: ExpenseFilters): Result<ExpenseStatistics> {
        return expenseRepository.getStatistics(filters)
    }
}
