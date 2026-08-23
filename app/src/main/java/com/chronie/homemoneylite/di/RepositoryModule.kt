package com.chronie.homemoneylite.di

import com.chronie.homemoneylite.data.local.dao.BudgetDao
import com.chronie.homemoneylite.data.local.dao.ExpenseDao
import com.chronie.homemoneylite.data.remote.api.ExpenseApi
import com.chronie.homemoneylite.data.remote.api.GoldPigCoinApi
import com.chronie.homemoneylite.data.remote.api.MemberApi
import com.chronie.homemoneylite.data.repository.BudgetRepositoryImpl
import com.chronie.homemoneylite.data.repository.ExpenseRepositoryImpl
import com.chronie.homemoneylite.data.repository.GoldPigCoinRepositoryImpl
import com.chronie.homemoneylite.data.sync.SyncManagerImpl
import com.chronie.homemoneylite.domain.model.GpcProduct
import com.chronie.homemoneylite.domain.model.GpcProductType
import com.chronie.homemoneylite.domain.repository.BudgetRepository
import com.chronie.homemoneylite.domain.repository.ExpenseRepository
import com.chronie.homemoneylite.domain.repository.GoldPigCoinRepository
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

    @Provides
    @Singleton
    fun provideGoldPigCoinRepository(
        api: GoldPigCoinApi
    ): GoldPigCoinRepository {
        return GoldPigCoinRepositoryImpl(api)
    }

    /**
     * 金猪币可购买商品目录（延长 EOL 订阅 / 新功能移植）。
     * 价格与提示文案集中在此维护，UI 直接消费。
     */
    @Provides
    @Singleton
    fun provideGpcProducts(): List<GpcProduct> = listOf(
        GpcProduct(
            type = GpcProductType.EOL_EXTEND,
            title = "延长 EOL 支持期",
            description = "延长本应用的停止服务（EOL）时间（按月订阅，单价随梯度浮动）。",
            amount = 350.0, // 仅作本地估算回退值；实际单价由 GPC 服务端按当月梯度决定
            customAmount = false
        ),
        GpcProduct(
            type = GpcProductType.FEATURE_PORT,
            title = "新功能移植",
            description = "将你所需的新功能移植到本应用，金额可自定义。",
            amount = 0.0,
            customAmount = true,
            minCustom = 500.0,
            maxCustom = 5000.0,
            customNotice = "每个新功能移植需要 500-5000 GPC 不等，下单前请先联系开发团队。"
        )
    )
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncManagerModule {
    
    @Binds
    @Singleton
    abstract fun bindSyncManager(syncManagerImpl: SyncManagerImpl): SyncManager
}
