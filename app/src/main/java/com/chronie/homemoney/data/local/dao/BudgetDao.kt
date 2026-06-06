package com.chronie.homemoney.data.local.dao

import androidx.room.*
import com.chronie.homemoney.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

/**
 * 预算数据访问对象
 */
@Dao
interface BudgetDao {
    
    @Query("SELECT * FROM budgets WHERE id = 1")
    fun getBudget(): Flow<BudgetEntity?>
    
    @Query("SELECT * FROM budgets WHERE id = 1")
    suspend fun getBudgetOnce(): BudgetEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)
    
    @Update
    suspend fun updateBudget(budget: BudgetEntity)
    
    @Query("DELETE FROM budgets")
    suspend fun deleteAll()
}
