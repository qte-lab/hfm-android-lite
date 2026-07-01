@file:Suppress("DEPRECATION")

package com.chronie.homemoneylite.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.chronie.homemoneylite.data.local.AppDatabase
import com.chronie.homemoneylite.data.local.DatabaseMigrations
import com.chronie.homemoneylite.data.local.dao.ExpenseDao
import com.chronie.homemoneylite.data.local.dao.MemberDao
import com.chronie.homemoneylite.data.local.dao.SyncQueueDao
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
    
    private const val TAG = "DatabaseModule"
    private const val DB_PASSPHRASE_KEY = "db_passphrase"
    private const val PREFS_FILE = "db_prefs"
    
    /**
     * 提供数据库密码
     * 使用普通 SharedPreferences 存储，数据库本身已通过 SQLCipher 加密
     */
    @Provides
    @Singleton
    fun provideDatabasePassphrase(@ApplicationContext context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        var passphrase = prefs.getString(DB_PASSPHRASE_KEY, null)
        if (passphrase == null) {
            passphrase = generateRandomPassphrase()
            prefs.edit().putString(DB_PASSPHRASE_KEY, passphrase).apply()
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
        
        return try {
            val db = Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                AppDatabase.DATABASE_NAME
            )
                .openHelperFactory(factory)
                .addMigrations(*DatabaseMigrations.getAllMigrations())
                .fallbackToDestructiveMigration(true)
                .build()
            // 主动触发数据库打开，以便能捕获 SQLCipher 解密异常
            db.openHelper?.writableDatabase
            db
        } catch (e: Exception) {
            // 密码不匹配导致 SQLCipher 解密失败，删除数据库后重建
            Log.w(TAG, "Database open failed, recreating", e)
            context.deleteDatabase(AppDatabase.DATABASE_NAME)
            Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                AppDatabase.DATABASE_NAME
            )
                .openHelperFactory(factory)
                .addMigrations(*DatabaseMigrations.getAllMigrations())
                .fallbackToDestructiveMigration(true)
                .build()
        }
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
    fun provideBudgetDao(database: AppDatabase): com.chronie.homemoneylite.data.local.dao.BudgetDao {
        return database.budgetDao()
    }
}
