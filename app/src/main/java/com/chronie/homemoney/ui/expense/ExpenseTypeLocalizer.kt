package com.chronie.homemoney.ui.expense

import android.content.Context
import com.chronie.homemoney.R
import com.chronie.homemoney.domain.model.ExpenseType

/**
 * 支出类型本地化工具
 */
object ExpenseTypeLocalizer {
    
    fun getLocalizedName(context: Context, type: ExpenseType): String {
        val resourceId = when (type) {
            ExpenseType.DAILY_GOODS -> R.string.expense_type_daily_goods
            ExpenseType.LUXURY -> R.string.expense_type_luxury
            ExpenseType.COMMUNICATION -> R.string.expense_type_communication
            ExpenseType.FOOD -> R.string.expense_type_food
            ExpenseType.SNACKS -> R.string.expense_type_snacks
            ExpenseType.COLD_DRINKS -> R.string.expense_type_cold_drinks
            ExpenseType.CONVENIENCE_FOOD -> R.string.expense_type_convenience_food
            ExpenseType.TEXTILES -> R.string.expense_type_textiles
            ExpenseType.BEVERAGES -> R.string.expense_type_beverages
            ExpenseType.CONDIMENTS -> R.string.expense_type_condiments
            ExpenseType.TRANSPORTATION -> R.string.expense_type_transportation
            ExpenseType.DINING -> R.string.expense_type_dining
            ExpenseType.MEDICAL -> R.string.expense_type_medical
            ExpenseType.FRUITS -> R.string.expense_type_fruits
            ExpenseType.OTHER -> R.string.expense_type_other
            ExpenseType.SEAFOOD -> R.string.expense_type_seafood
            ExpenseType.DAIRY -> R.string.expense_type_dairy
            ExpenseType.GIFTS -> R.string.expense_type_gifts
            ExpenseType.TRAVEL -> R.string.expense_type_travel
            ExpenseType.GOVERNMENT -> R.string.expense_type_government
            ExpenseType.UTILITIES -> R.string.expense_type_utilities
            ExpenseType.BEAUTY -> R.string.expense_type_beauty
            ExpenseType.BEAN_PRODUCTS -> R.string.expense_type_bean_products
            ExpenseType.COSMETICS -> R.string.expense_type_cosmetics
            ExpenseType.ELECTRONICS -> R.string.expense_type_electronics
            ExpenseType.HOUSEHOLD_APPLIANCES -> R.string.expense_type_household_appliances
            ExpenseType.HARDWARE -> R.string.expense_type_hardware
            ExpenseType.CLOTHING -> R.string.expense_type_clothing
        }
        return context.getString(resourceId)
    }
    
    /**
     * 根据类型名称字符串获取本地化名称
     */
    fun getLocalizedTypeName(context: Context, typeName: String): String {
        return try {
            val type = ExpenseType.valueOf(typeName)
            getLocalizedName(context, type)
        } catch (e: IllegalArgumentException) {
            typeName // 如果无法解析，返回原始名称
        }
    }
}
