package com.chronie.homemoney.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chronie.homemoney.data.local.dao.ExpenseDao
import com.chronie.homemoney.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * 数据库基本操作测试
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {
    
    private lateinit var database: AppDatabase
    private lateinit var expenseDao: ExpenseDao
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).build()
        expenseDao = database.expenseDao()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun testInsertAndGetExpense() = runBlocking {
        // 创建测试数据
        val expense = ExpenseEntity(
            id = UUID.randomUUID().toString(),
            type = "餐饮",
            remark = "午餐",
            amount = 45.0,
            date = "2024-01-01",
            isSynced = false
        )
        
        // 插入数据
        expenseDao.insertExpense(expense)
        
        // 查询数据
        val result = expenseDao.getExpenseById(expense.id)
        
        // 验证
        assertNotNull(result)
        assertEquals(expense.id, result?.id)
        assertEquals(expense.type, result?.type)
        result?.amount?.let { assertEquals(expense.amount, it, 0.01) }
    }
    
    @Test
    fun testGetAllExpenses() = runBlocking {
        // 插入多条数据
        val expenses = listOf(
            ExpenseEntity(
                id = UUID.randomUUID().toString(),
                type = "餐饮",
                remark = "早餐",
                amount = 15.0,
                date = "2024-01-01",
                isSynced = false
            ),
            ExpenseEntity(
                id = UUID.randomUUID().toString(),
                type = "交通",
                remark = "打车",
                amount = 32.0,
                date = "2024-01-01",
                isSynced = false
            )
        )
        
        expenseDao.insertExpenses(expenses)
        
        // 查询所有数据
        val result = expenseDao.getAllExpenses().first()
        
        // 验证
        assertEquals(2, result.size)
    }
    
    @Test
    fun testUpdateExpense() = runBlocking {
        // 插入数据
        val expense = ExpenseEntity(
            id = UUID.randomUUID().toString(),
            type = "餐饮",
            remark = "午餐",
            amount = 45.0,
            date = "2024-01-01",
            isSynced = false
        )
        expenseDao.insertExpense(expense)
        
        // 更新数据
        val updatedExpense = expense.copy(
            amount = 50.0,
            remark = "午餐（更新）"
        )
        expenseDao.updateExpense(updatedExpense)
        
        // 查询验证
        val result = expenseDao.getExpenseById(expense.id)
        result?.amount?.let { assertEquals(50.0, it, 0.01) }
        assertEquals("午餐（更新）", result?.remark)
    }
    
    @Test
    fun testDeleteExpense() = runBlocking {
        // 插入数据
        val expense = ExpenseEntity(
            id = UUID.randomUUID().toString(),
            type = "餐饮",
            remark = "午餐",
            amount = 45.0,
            date = "2024-01-01",
            isSynced = false
        )
        expenseDao.insertExpense(expense)
        
        // 删除数据
        expenseDao.deleteExpenseById(expense.id)
        
        // 验证
        val result = expenseDao.getExpenseById(expense.id)
        assertNull(result)
    }
    
    @Test
    fun testGetUnsyncedExpenses() = runBlocking {
        // 插入已同步和未同步的数据
        val syncedExpense = ExpenseEntity(
            id = UUID.randomUUID().toString(),
            type = "餐饮",
            remark = "已同步",
            amount = 45.0,
            date = "2024-01-01",
            isSynced = true
        )
        
        val unsyncedExpense = ExpenseEntity(
            id = UUID.randomUUID().toString(),
            type = "交通",
            remark = "未同步",
            amount = 32.0,
            date = "2024-01-01",
            isSynced = false
        )
        
        expenseDao.insertExpenses(listOf(syncedExpense, unsyncedExpense))
        
        // 查询未同步的数据
        val result = expenseDao.getUnsyncedExpenses()
        
        // 验证
        assertEquals(1, result.size)
        assertEquals(unsyncedExpense.id, result[0].id)
    }
}
