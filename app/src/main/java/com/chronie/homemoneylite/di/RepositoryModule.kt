package com.chronie.homemoneylite.di

import com.chronie.homemoneylite.data.local.dao.BudgetDao
import com.chronie.homemoneylite.data.local.dao.ExpenseDao
import com.chronie.homemoneylite.data.remote.api.ExpenseApi
import com.chronie.homemoneylite.data.remote.api.MemberApi
import com.chronie.homemoneylite.data.repository.BudgetRepositoryImpl
import com.chronie.homemoneylite.data.repository.ExpenseRepositoryImpl
import com.chronie.homemoneylite.data.sync.SyncManagerImpl
import com.chronie.homemoneylite.domain.repository.BudgetRepository
import com.chronie.homemoneylite.domain.repository.ExpenseRepository
import com.chronie.homemoneylite.domain.sync.SyncManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideExpenseRepository(
        expenseDao: ExpenseDao,
        expenseApi: ExpenseApi
    ): ExpenseRepository {
        return ExpenseRepositoryImpl(expenseDao, expenseApi)
    }
    
    @Provides
    @Singleton
    fun provideBudgetRepository(
        budgetDao: BudgetDao,
        expenseDao: ExpenseDao
    ): BudgetRepository {
        return BudgetRepositoryImpl(budgetDao, expenseDao)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncManagerModule {
    
    @Binds
    @Singleton
    abstract fun bindSyncManager(syncManagerImpl: SyncManagerImpl): SyncManager
}
