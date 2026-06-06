package com.chronie.homemoney.domain.usecase

import android.content.Context
import android.net.Uri
import com.chronie.homemoney.R
import com.chronie.homemoney.domain.model.Expense
import com.chronie.homemoney.domain.model.ExpenseType
import com.chronie.homemoney.domain.repository.ExpenseRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import org.dhatim.fastexcel.reader.ReadableWorkbook
import org.dhatim.fastexcel.reader.Sheet
import org.dhatim.fastexcel.reader.Row
import org.dhatim.fastexcel.reader.Cell

import java.util.*
import javax.inject.Inject

/**
 * 从 Excel 文件导入支出记录
 */
class ImportExpensesUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    @param:ApplicationContext private val context: Context
) {
    
    data class ImportResult(
        val successCount: Int,
        val failedCount: Int,
        val errors: List<String>
    )
    
    suspend operator fun invoke(uri: Uri): Result<ImportResult> {
        return try {
            val expenses = parseExcelFile(uri)
            
            if (expenses.isEmpty()) {
                return Result.failure(Exception("No valid records found in file"))
            }
            
            // 导入数据
            var successCount = 0
            var failedCount = 0
            val errors = mutableListOf<String>()
            
            expenses.forEachIndexed { index, expense ->
                val result = expenseRepository.addExpense(expense)
                if (result.isSuccess) {
                    successCount++
                } else {
                    failedCount++
                    errors.add("Row ${index + 2}: ${result.exceptionOrNull()?.message}")
                }
            }
            
            Result.success(ImportResult(successCount, failedCount, errors))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun parseExcelFile(uri: Uri): List<Expense> {
        val expenses = mutableListOf<Expense>()
        
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            ReadableWorkbook(inputStream).use { workbook ->
                val sheet = workbook.getFirstSheet()
                
                val rows = sheet.read()
                if (rows.isEmpty()) {
                    throw Exception("Empty file")
                }
                
                val headerRow = rows[0]
                val headers = mutableMapOf<String, Int>()
                
                headerRow.forEachIndexed { index, cell ->
                    val cellValue = cell.asString()
                    if (cellValue.isNotEmpty()) {
                        headers[cellValue.trim()] = index
                    }
                }
                
                val dateColIndex = findColumnIndex(headers, "date")
                val typeColIndex = findColumnIndex(headers, "type")
                val amountColIndex = findColumnIndex(headers, "amount")
                val remarkColIndex = findColumnIndex(headers, "remark")
                
                if (dateColIndex == -1 || typeColIndex == -1 || amountColIndex == -1) {
                    throw Exception(context.getString(R.string.import_validation_error, "Missing required columns"))
                }
                
                for (i in 1 until rows.size) {
                    val row = rows[i]
                    
                    try {
                        val dateCell = row.getCell(dateColIndex)
                        val dateStr = parseDateCell(dateCell) ?: continue
                        
                        val typeCell = row.getCell(typeColIndex)
                        val typeStr = typeCell?.asString() ?: continue
                        val expenseType = parseExpenseType(typeStr)
                        
                        val amountCell = row.getCell(amountColIndex)
                        val amount = parseAmountCell(amountCell) ?: continue
                        
                        if (amount <= 0) continue
                        
                        val remarkCell = row.getCell(remarkColIndex)
                        val remark = remarkCell?.asString()
                        
                        val expense = Expense(
                            id = UUID.randomUUID().toString(),
                            type = expenseType,
                            remark = remark,
                            amount = amount,
                            date = dateStr,
                            isSynced = false
                        )
                        
                        expenses.add(expense)
                    } catch (e: Exception) {
                        android.util.Log.w("ImportExpenses", "Failed to parse row ${i + 1}: ${e.message}")
                    }
                }
            }
        }
        
        return expenses
    }
    
    private fun parseDateCell(cell: Cell?): String? {
        if (cell == null) return null
        
        return try {
            val cellValue = cell.asString()
            val datePart = if (cellValue.contains('T') || cellValue.contains(' ')) {
                cellValue.substringBefore('T').substringBefore(' ')
            } else {
                cellValue
            }
            
            java.time.LocalDate.parse(datePart).toString()
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseAmountCell(cell: Cell?): Double? {
        if (cell == null) return null
        
        return try {
            cell.asNumber().toDouble()
        } catch (e: Exception) {
            cell.asString().toDoubleOrNull()
        }
    }
    
    private fun findColumnIndex(headers: Map<String, Int>, columnKey: String): Int {
        // 尝试匹配多语言标题
        val possibleHeaders = when (columnKey) {
            "date" -> listOf(
                context.getString(R.string.excel_header_date),
                "Date", "日期", "日期"
            )
            "type" -> listOf(
                context.getString(R.string.excel_header_type),
                "Type", "类型", "類型"
            )
            "amount" -> listOf(
                context.getString(R.string.excel_header_amount),
                "Amount", "金额", "金額"
            )
            "remark" -> listOf(
                context.getString(R.string.excel_header_remark),
                "Remark", "备注", "備註"
            )
            else -> emptyList()
        }
        
        for (header in possibleHeaders) {
            val index = headers[header]
            if (index != null) return index
        }
        
        return -1
    }
    
    private fun parseExpenseType(typeStr: String): ExpenseType {
        // 尝试匹配所有语言的类型名称
        return ExpenseType.values().find { type ->
            val resourceId = context.resources.getIdentifier(
                type.displayNameKey,
                "string",
                context.packageName
            )
            if (resourceId != 0) {
                context.getString(resourceId) == typeStr
            } else {
                false
            }
        } ?: ExpenseType.fromString(typeStr)
    }
}
