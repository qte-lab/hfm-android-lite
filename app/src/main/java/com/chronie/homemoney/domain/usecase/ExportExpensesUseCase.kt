package com.chronie.homemoney.domain.usecase

import android.content.Context
import android.os.Environment
import com.chronie.homemoney.R
import com.chronie.homemoney.data.local.dao.ExpenseDao
import com.chronie.homemoney.data.local.entity.ExpenseEntity
import com.chronie.homemoney.data.mapper.ExpenseMapper
import com.chronie.homemoney.domain.model.Expense
import com.chronie.homemoney.domain.model.ExpenseType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import org.dhatim.fastexcel.Workbook
import org.dhatim.fastexcel.Worksheet
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.security.AccessControlException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Export expenses to Excel file from local database
 */
class ExportExpensesUseCase @Inject constructor(
    private val expenseDao: ExpenseDao,
    @param:ApplicationContext private val context: Context
) {
    
    suspend operator fun invoke(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): Result<String> {
        return try {
            android.util.Log.d("ExportExpensesUseCase", "Starting export with startDate=$startDate, endDate=$endDate")
            
            val expenses = if (startDate != null && endDate != null) {
                val startDateStr = startDate.toString()
                val endDateStr = endDate.toString()
                android.util.Log.d("ExportExpensesUseCase", "Querying expenses by date range: $startDateStr to $endDateStr")
                expenseDao.getExpensesByDateRangeSync(startDateStr, endDateStr)
                    .map { ExpenseMapper.toDomain(it) }
            } else {
                android.util.Log.d("ExportExpensesUseCase", "Querying all expenses")
                expenseDao.getAllExpenses().first()
                    .map { ExpenseMapper.toDomain(it) }
            }
            
            android.util.Log.d("ExportExpensesUseCase", "Found ${expenses.size} expenses to export")
            
            if (expenses.isEmpty()) {
                android.util.Log.w("ExportExpensesUseCase", "No expenses found")
                return Result.failure(Exception(context.getString(R.string.no_records_to_export)))
            }
            
            android.util.Log.d("ExportExpensesUseCase", "Validating expenses data")
            val validExpenses = expenses.filter { expense ->
                val isValid = expense.date.isNotBlank() && 
                              expense.amount >= 0 &&
                              !expense.type.name.isBlank()
                if (!isValid) {
                    android.util.Log.w("ExportExpensesUseCase", "Skipping invalid expense: id=${expense.id}, date=${expense.date}, amount=${expense.amount}")
                }
                isValid
            }
            
            android.util.Log.d("ExportExpensesUseCase", "Valid expenses count: ${validExpenses.size}")
            
            if (validExpenses.isEmpty()) {
                return Result.failure(Exception("没有有效的支出记录可以导出"))
            }
            
            android.util.Log.d("ExportExpensesUseCase", "Preparing Downloads directory")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                android.util.Log.d("ExportExpensesUseCase", "Creating Downloads directory")
                downloadsDir.mkdirs()
            }
            
            val timestamp = System.currentTimeMillis()
            val dateRange = if (startDate != null && endDate != null) {
                "_${startDate}_${endDate}"
            } else {
                ""
            }
            val filename = "${context.getString(R.string.export_filename_prefix)}${dateRange}_$timestamp.xlsx"
            val file = File(downloadsDir, filename)
            
            android.util.Log.d("ExportExpensesUseCase", "Creating Excel file: ${file.absolutePath}")
            
            FileOutputStream(file).use { outputStream ->
                Workbook(outputStream, "HomeMoney", "1.0").use { workbook ->
                    val sheet = workbook.newWorksheet(context.getString(R.string.expense_records))
                    
                    val headers = listOf(
                        context.getString(R.string.excel_header_date),
                        context.getString(R.string.excel_header_type),
                        context.getString(R.string.excel_header_amount),
                        context.getString(R.string.excel_header_remark)
                    )
                    
                    headers.forEachIndexed { index, header ->
                        sheet.value(0, index, header)
                        sheet.style(0, index).bold().set()
                    }
                    
                    validExpenses.forEachIndexed { index, expense ->
                        val row = index + 1
                        
                        if (index % 100 == 0) {
                            android.util.Log.d("ExportExpensesUseCase", "Writing row $row/${validExpenses.size}, id=${expense.id}")
                        }
                        
                        try {
                            val dateValue = expense.date.ifBlank { "" }
                            android.util.Log.v("ExportExpensesUseCase", "Row $row - Writing date: '$dateValue'")
                            sheet.value(row, 0, dateValue)
                            
                            val typeValue = getExpenseTypeName(expense.type)
                            android.util.Log.v("ExportExpensesUseCase", "Row $row - Writing type: '$typeValue'")
                            sheet.value(row, 1, typeValue)
                            
                            val amountValue = if (expense.amount.isNaN() || expense.amount.isInfinite()) 0.0 else expense.amount
                            android.util.Log.v("ExportExpensesUseCase", "Row $row - Writing amount: $amountValue")
                            sheet.value(row, 2, amountValue)
                            
                            val remarkValue = expense.remark?.ifBlank { "" } ?: ""
                            android.util.Log.v("ExportExpensesUseCase", "Row $row - Writing remark: '$remarkValue'")
                            sheet.value(row, 3, remarkValue)
                        } catch (e: Exception) {
                            android.util.Log.e("ExportExpensesUseCase", "Failed to write row $row: id=${expense.id}, date='${expense.date}', amount=${expense.amount}, type=${expense.type}", e)
                            throw e
                        }
                    }
                    
                    android.util.Log.d("ExportExpensesUseCase", "Setting column widths")
                    sheet.width(0, 15.0)
                    sheet.width(1, 12.0)
                    sheet.width(2, 10.0)
                    sheet.width(3, 20.0)
                    android.util.Log.d("ExportExpensesUseCase", "Column widths set successfully")
                }
            }
            
            android.util.Log.d("ExportExpensesUseCase", "Export completed successfully: ${file.absolutePath}")
            Result.success(file.absolutePath)
        } catch (e: Exception) {
            android.util.Log.e("ExportExpensesUseCase", "Export failed", e)
            val errorMessage = when (e) {
                is java.security.AccessControlException -> "存储权限被拒绝"
                is java.io.FileNotFoundException -> "找不到存储目录"
                is java.io.IOException -> "文件写入失败: ${e.message}"
                else -> e.message ?: "未知错误"
            }
            Result.failure(Exception(errorMessage))
        }
    }
    
    private fun getExpenseTypeName(type: ExpenseType): String {
        val resourceId = context.resources.getIdentifier(
            type.displayNameKey,
            "string",
            context.packageName
        )
        return if (resourceId != 0) {
            context.getString(resourceId)
        } else {
            type.name
        }
    }
}
