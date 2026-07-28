package com.chronie.homemoneylite.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.chronie.homemoneylite.data.local.dao.BudgetDao
import com.chronie.homemoneylite.data.local.dao.ExpenseDao
import com.chronie.homemoneylite.data.local.dao.MemberDao
import com.chronie.homemoneylite.data.local.dao.SyncQueueDao
import com.chronie.homemoneylite.data.local.entity.BudgetEntity
import com.chronie.homemoneylite.data.local.entity.ExpenseEntity
import com.chronie.homemoneylite.data.local.entity.MemberEntity
import com.chronie.homemoneylite.data.local.entity.SyncQueueEntity

@Database(
    entities = [
        ExpenseEntity::class,
        MemberEntity::class,
        SyncQueueEntity::class,
        BudgetEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun expenseDao(): ExpenseDao
    abstract fun memberDao(): MemberDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun budgetDao(): BudgetDao
    
    companion object {
        const val DATABASE_NAME = "homemoney.db"
    }
}
