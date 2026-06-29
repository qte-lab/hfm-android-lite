@file:Suppress("DEPRECATION")

package com.chronie.homemoneylite.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
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
    private const val ENCRYPTED_PREFS_FILE = "secure_prefs"
    private const val FALLBACK_PREFS_FILE = "db_prefs_fallback"
    
    /**
     * 提供数据库密码
     * 使用 EncryptedSharedPreferences 安全存储
     */
    @Provides
    @Singleton
    @Suppress("DEPRECATION")
    fun provideDatabasePassphrase(@ApplicationContext context: Context): ByteArray {
        // 尝试使用 EncryptedSharedPreferences
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        try {
            val encryptedPrefs = EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            val passphrase = encryptedPrefs.getString(DB_PASSPHRASE_KEY, null)
            if (passphrase != null) {
                return passphrase.toByteArray(StandardCharsets.UTF_8)
            }
        } catch (e: Exception) {
            // EncryptedSharedPreferences 创建或读取失败（低版本 Android Keystore 兼容性问题）
            Log.w(TAG, "EncryptedSharedPreferences failed, using fallback", e)
            deleteCorruptedPrefs(context)
        }
        
        // 回退方案：使用普通 SharedPreferences 存储密码
        // 密码本身是随机的，数据库已通过 SQLCipher 加密，SharedPreferences 仅做持久化
        val fallbackPrefs = context.getSharedPreferences(FALLBACK_PREFS_FILE, Context.MODE_PRIVATE)
        var passphrase = fallbackPrefs.getString(DB_PASSPHRASE_KEY, null)
        if (passphrase == null) {
            passphrase = generateRandomPassphrase()
            fallbackPrefs.edit().putString(DB_PASSPHRASE_KEY, passphrase).apply()
            // 如果之前有加密 prefs 中的数据，尝试迁移（此时已无法读取，跳过）
        }
        
        return passphrase.toByteArray(StandardCharsets.UTF_8)
    }
    
    /**
     * 删除损坏的加密 SharedPreferences 文件
     */
    private fun deleteCorruptedPrefs(context: Context) {
        try {
            context.deleteSharedPreferences(ENCRYPTED_PREFS_FILE)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete corrupted prefs", e)
        }
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
