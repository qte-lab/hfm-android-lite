package com.chronie.homemoney.di

import android.content.Context
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.chronie.homemoney.data.local.AppDatabase
import com.chronie.homemoney.data.local.DatabaseMigrations
import com.chronie.homemoney.data.local.dao.ExpenseDao
import com.chronie.homemoney.data.local.dao.MemberDao
import com.chronie.homemoney.data.local.dao.SyncQueueDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.nio.charset.StandardCharsets
import javax.inject.Singleton

/**
 * 数据库依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    init {
        System.loadLibrary("sqlcipher")
    }
    
    private const val DB_PASSPHRASE_KEY = "db_passphrase"
    private const val ENCRYPTED_PREFS_FILE = "secure_prefs"
    
    /**
     * 提供数据库密码
     * 使用 EncryptedSharedPreferences 安全存储
     */
    @Provides
    @Singleton
    fun provideDatabasePassphrase(@ApplicationContext context: Context): ByteArray {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        
        // 获取或生成密码
        var passphrase = sharedPreferences.getString(DB_PASSPHRASE_KEY, null)
        if (passphrase == null) {
            // 生成随机密码
            passphrase = generateRandomPassphrase()
            sharedPreferences.edit().putString(DB_PASSPHRASE_KEY, passphrase).apply()
        }
        
        return passphrase.toByteArray(StandardCharsets.UTF_8)
    }
    
    /**
     * 生成随机密码
     */
    private fun generateRandomPassphrase(): String {
        val charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()"
        return (1..32)
            .map { charset.random() }
            .joinToString("")
    }
    
    /**
     * 提供 AppDatabase 实例
     */
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        passphrase: ByteArray
    ): AppDatabase {
        val factory = SupportOpenHelperFactory(passphrase)
        
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .openHelperFactory(factory)
            .addMigrations(*DatabaseMigrations.getAllMigrations())
            .fallbackToDestructiveMigration()
            .build()
    }
    
    /**
     * 提供 ExpenseDao
     */
    @Provides
    fun provideExpenseDao(database: AppDatabase): ExpenseDao {
        return database.expenseDao()
    }
    
    /**
     * 提供 MemberDao
     */
    @Provides
    fun provideMemberDao(database: AppDatabase): MemberDao {
        return database.memberDao()
    }
    
    /**
     * 提供 SyncQueueDao
     */
    @Provides
    fun provideSyncQueueDao(database: AppDatabase): SyncQueueDao {
        return database.syncQueueDao()
    }
    
    /**
     * 提供 BudgetDao
     */
    @Provides
    fun provideBudgetDao(database: AppDatabase): com.chronie.homemoney.data.local.dao.BudgetDao {
        return database.budgetDao()
    }
}
