package com.chronie.homemoney.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.chronie.homemoney.data.local.dao.ExpenseDao
import com.chronie.homemoney.data.local.dao.SyncQueueDao
import com.chronie.homemoney.data.local.entity.ExpenseEntity
import com.chronie.homemoney.data.mapper.ExpenseMapper
import com.chronie.homemoney.data.remote.api.ExpenseApi
import com.chronie.homemoney.domain.model.Expense
import com.chronie.homemoney.domain.model.ExpenseFilters
import com.chronie.homemoney.domain.model.ExpenseStatistics
import com.chronie.homemoney.domain.model.SortOption
import com.chronie.homemoney.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val expenseApi: ExpenseApi
) : ExpenseRepository {
    
    override fun getExpenses(
        page: Int,
        limit: Int,
        filters: ExpenseFilters
    ): Flow<PagingData<Expense>> {
        return Pager<Int, Expense>(
            config = PagingConfig(
                pageSize = limit,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                throw NotImplementedError("PagingSource not implemented yet")
            }
        ).flow
    }
    
    override suspend fun getExpensesList(
        page: Int,
        limit: Int,
        filters: ExpenseFilters
    ): Result<List<Expense>> {
        return try {
            val networkTimeoutMillis = 3000L
            
            val expenses = try {
                withTimeout(networkTimeoutMillis) {
                    val response = expenseApi.getExpenses(
                        page = page,
                        limit = limit,
                        keyword = filters.keyword,
                        type = filters.type?.let { getChineseTypeName(it) },
                        month = filters.month,
                        minAmount = filters.minAmount,
                        maxAmount = filters.maxAmount,
                        sort = getSortString(filters.sortBy)
                    )
                    
                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!
                        var expenses = apiResponse.data.map { ExpenseMapper.toDomain(it) }
                        
                        if (filters.startDate != null) {
                            expenses = expenses.filter { LocalDate.parse(it.date) >= filters.startDate }
                        }
                        if (filters.endDate != null) {
                            expenses = expenses.filter { LocalDate.parse(it.date) <= filters.endDate }
                        }
                        
                        expenses = when (filters.sortBy) {
                            SortOption.DATE_ASC -> expenses.sortedBy { it.date }
                            SortOption.DATE_DESC -> expenses.sortedByDescending { it.date }
                            SortOption.AMOUNT_ASC -> expenses.sortedBy { it.amount }
                            SortOption.AMOUNT_DESC -> expenses.sortedByDescending { it.amount }
                        }
                        
                        expenses.forEach { expense ->
                            expenseDao.upsertExpense(ExpenseMapper.toEntity(expense.copy(isSynced = true)))
                        }
                        
                        Result.success(expenses)
                    } else {
                        Result.failure(Exception("Server returned error: ${response.code()}"))
                    }
                }
            } catch (timeoutException: kotlinx.coroutines.TimeoutCancellationException) {
                null
            } catch (networkError: Exception) {
                null
            }
            
            if (expenses != null) {
                return expenses
            }
            
            val allExpenses = expenseDao.getAllExpenses().first()
            var filteredExpenses = allExpenses.map { ExpenseMapper.toDomain(it) }
            
            if (filters.keyword != null) {
                filteredExpenses = filteredExpenses.filter { expense ->
                    expense.remark?.contains(filters.keyword, ignoreCase = true) == true ||
                    getChineseTypeName(expense.type).contains(filters.keyword, ignoreCase = true)
                }
            }
            
            if (filters.type != null) {
                filteredExpenses = filteredExpenses.filter { it.type == filters.type }
            }
            
            if (filters.minAmount != null) {
                filteredExpenses = filteredExpenses.filter { it.amount >= filters.minAmount }
            }
            
            if (filters.maxAmount != null) {
                filteredExpenses = filteredExpenses.filter { it.amount <= filters.maxAmount }
            }
            
            if (filters.startDate != null) {
                filteredExpenses = filteredExpenses.filter { 
                    LocalDate.parse(it.date) >= filters.startDate 
                }
            }
            
            if (filters.endDate != null) {
                filteredExpenses = filteredExpenses.filter { 
                    LocalDate.parse(it.date) <= filters.endDate 
                }
            }
            
            filteredExpenses = when (filters.sortBy) {
                SortOption.DATE_ASC -> filteredExpenses.sortedBy { it.date }
                SortOption.DATE_DESC -> filteredExpenses.sortedByDescending { it.date }
                SortOption.AMOUNT_ASC -> filteredExpenses.sortedBy { it.amount }
                SortOption.AMOUNT_DESC -> filteredExpenses.sortedByDescending { it.amount }
            }
            
            val startIndex = (page - 1) * limit
            val endIndex = minOf(startIndex + limit, filteredExpenses.size)
            val localExpenses = if (startIndex < filteredExpenses.size) {
                filteredExpenses.subList(startIndex, endIndex)
            } else {
                emptyList()
            }
            
            Result.success(localExpenses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getExpenseById(id: String): Result<Expense> {
        return try {
            val entity = expenseDao.getExpenseById(id)
            if (entity != null) {
                Result.success(ExpenseMapper.toDomain(entity))
            } else {
                Result.failure(Exception("Expense not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun addExpense(expense: Expense): Result<Expense> {
        return try {
            val id = expense.id.ifEmpty { UUID.randomUUID().toString() }
            val now = System.currentTimeMillis()
            
            val newExpense = expense.copy(
                id = id,
                version = 1,
                updatedAt = now,
                isSynced = false
            )
            
            val entity = ExpenseMapper.toEntity(newExpense)
            expenseDao.insertExpense(entity)
            
            Result.success(newExpense)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateExpense(expense: Expense): Result<Expense> {
        return try {
            val existing = expenseDao.getExpenseById(expense.id)
            val newVersion = (existing?.version ?: 0) + 1
            val now = System.currentTimeMillis()
            
            val updatedExpense = expense.copy(
                version = newVersion,
                updatedAt = now,
                isSynced = false
            )
            
            val entity = ExpenseMapper.toEntity(updatedExpense)
            expenseDao.updateExpense(entity)
            
            Result.success(updatedExpense)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteExpense(id: String): Result<Unit> {
        return try {
            val entity = expenseDao.getExpenseById(id)
            if (entity != null) {
                val now = System.currentTimeMillis()
                val deletedEntity = entity.copy(
                    deletedAt = now,
                    updatedAt = now,
                    version = entity.version + 1,
                    isSynced = false
                )
                expenseDao.updateExpense(deletedEntity)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getStatistics(filters: ExpenseFilters): Result<ExpenseStatistics> {
        return try {
            val allExpenses = expenseDao.getAllExpenses().first()
            var expenses = allExpenses.map { ExpenseMapper.toDomain(it) }
            
            if (filters.keyword != null) {
                expenses = expenses.filter { expense ->
                    expense.remark?.contains(filters.keyword, ignoreCase = true) == true ||
                    getChineseTypeName(expense.type).contains(filters.keyword, ignoreCase = true)
                }
            }
            
            if (filters.type != null) {
                expenses = expenses.filter { it.type == filters.type }
            }
            
            if (filters.minAmount != null) {
                expenses = expenses.filter { it.amount >= filters.minAmount }
            }
            
            if (filters.maxAmount != null) {
                expenses = expenses.filter { it.amount <= filters.maxAmount }
            }
            
            if (filters.startDate != null) {
                expenses = expenses.filter { expense ->
                    try {
                        val expenseDate = LocalDate.parse(expense.date)
                        expenseDate >= filters.startDate
                    } catch (e: Exception) {
                        false
                    }
                }
            }
            
            if (filters.endDate != null) {
                expenses = expenses.filter { expense ->
                    try {
                        val expenseDate = LocalDate.parse(expense.date)
                        expenseDate <= filters.endDate
                    } catch (e: Exception) {
                        false
                    }
                }
            }
            
            if (expenses.isEmpty()) {
                return Result.success(
                    ExpenseStatistics(
                        count = 0,
                        totalAmount = 0.0,
                        averageAmount = 0.0,
                        medianAmount = 0.0,
                        minAmount = 0.0,
                        maxAmount = 0.0
                    )
                )
            }
            
            val amounts = expenses.map { it.amount }.sorted()
            val total = amounts.sum()
            val average = total / amounts.size
            val median = if (amounts.size % 2 == 0) {
                (amounts[amounts.size / 2 - 1] + amounts[amounts.size / 2]) / 2
            } else {
                amounts[amounts.size / 2]
            }
            
            Result.success(
                ExpenseStatistics(
                    count = expenses.size,
                    totalAmount = total,
                    averageAmount = average,
                    medianAmount = median,
                    minAmount = amounts.first(),
                    maxAmount = amounts.last()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun syncWithServer(): Result<Unit> {
        return Result.success(Unit)
    }
    
    private fun getChineseTypeName(type: com.chronie.homemoney.domain.model.ExpenseType): String {
        return when (type) {
            com.chronie.homemoney.domain.model.ExpenseType.DAILY_GOODS -> "日常用品"
            com.chronie.homemoney.domain.model.ExpenseType.LUXURY -> "奢侈品"
            com.chronie.homemoney.domain.model.ExpenseType.COMMUNICATION -> "通讯费用"
            com.chronie.homemoney.domain.model.ExpenseType.FOOD -> "食品"
            com.chronie.homemoney.domain.model.ExpenseType.SNACKS -> "零食糖果"
            com.chronie.homemoney.domain.model.ExpenseType.COLD_DRINKS -> "冷饮"
            com.chronie.homemoney.domain.model.ExpenseType.CONVENIENCE_FOOD -> "方便食品"
            com.chronie.homemoney.domain.model.ExpenseType.TEXTILES -> "纺织品"
            com.chronie.homemoney.domain.model.ExpenseType.BEVERAGES -> "饮品"
            com.chronie.homemoney.domain.model.ExpenseType.CONDIMENTS -> "调味品"
            com.chronie.homemoney.domain.model.ExpenseType.TRANSPORTATION -> "交通出行"
            com.chronie.homemoney.domain.model.ExpenseType.DINING -> "餐饮"
            com.chronie.homemoney.domain.model.ExpenseType.MEDICAL -> "医疗费用"
            com.chronie.homemoney.domain.model.ExpenseType.FRUITS -> "水果"
            com.chronie.homemoney.domain.model.ExpenseType.OTHER -> "其他"
            com.chronie.homemoney.domain.model.ExpenseType.SEAFOOD -> "水产品"
            com.chronie.homemoney.domain.model.ExpenseType.DAIRY -> "乳制品"
            com.chronie.homemoney.domain.model.ExpenseType.GIFTS -> "礼物人情"
            com.chronie.homemoney.domain.model.ExpenseType.TRAVEL -> "旅行度假"
            com.chronie.homemoney.domain.model.ExpenseType.GOVERNMENT -> "政务"
            com.chronie.homemoney.domain.model.ExpenseType.UTILITIES -> "水电煤气"
            com.chronie.homemoney.domain.model.ExpenseType.BEAUTY -> "美容美发"
            com.chronie.homemoney.domain.model.ExpenseType.BEAN_PRODUCTS -> "豆制品"
            com.chronie.homemoney.domain.model.ExpenseType.COSMETICS -> "个护美妆"
            com.chronie.homemoney.domain.model.ExpenseType.ELECTRONICS -> "电子产品"
            com.chronie.homemoney.domain.model.ExpenseType.HOUSEHOLD_APPLIANCES -> "家用电器"
            com.chronie.homemoney.domain.model.ExpenseType.HARDWARE -> "五金"
            com.chronie.homemoney.domain.model.ExpenseType.CLOTHING -> "服装"
        }
    }
    
    private fun getSortString(sortOption: SortOption): String {
        return when (sortOption) {
            SortOption.DATE_ASC -> "dateAsc"
            SortOption.DATE_DESC -> "dateDesc"
            SortOption.AMOUNT_ASC -> "amountAsc"
            SortOption.AMOUNT_DESC -> "amountDesc"
        }
    }
}
