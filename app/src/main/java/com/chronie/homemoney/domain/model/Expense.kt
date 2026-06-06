package com.chronie.homemoney.domain.model

data class Expense(
    val id: String,
    val type: ExpenseType,
    val remark: String?,
    val amount: Double,
    val date: String,
    val version: Int = 1,
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val isSynced: Boolean = false
)

enum class ExpenseType(val displayNameKey: String) {
    DAILY_GOODS("expense_type_daily_goods"),
    LUXURY("expense_type_luxury"),
    COMMUNICATION("expense_type_communication"),
    FOOD("expense_type_food"),
    SNACKS("expense_type_snacks"),
    COLD_DRINKS("expense_type_cold_drinks"),
    CONVENIENCE_FOOD("expense_type_convenience_food"),
    TEXTILES("expense_type_textiles"),
    BEVERAGES("expense_type_beverages"),
    CONDIMENTS("expense_type_condiments"),
    TRANSPORTATION("expense_type_transportation"),
    DINING("expense_type_dining"),
    MEDICAL("expense_type_medical"),
    FRUITS("expense_type_fruits"),
    OTHER("expense_type_other"),
    SEAFOOD("expense_type_seafood"),
    DAIRY("expense_type_dairy"),
    GIFTS("expense_type_gifts"),
    TRAVEL("expense_type_travel"),
    GOVERNMENT("expense_type_government"),
    UTILITIES("expense_type_utilities"),
    BEAUTY("expense_type_beauty"),
    BEAN_PRODUCTS("expense_type_bean_products"),
    COSMETICS("expense_type_cosmetics"),
    ELECTRONICS("expense_type_electronics"),
    HOUSEHOLD_APPLIANCES("expense_type_household_appliances"),
    HARDWARE("expense_type_hardware"),
    CLOTHING("expense_type_clothing");

    companion object {
        fun fromString(value: String): ExpenseType {
            return when (value) {
                "日常用品" -> DAILY_GOODS
                "奢侈品" -> LUXURY
                "通讯费用" -> COMMUNICATION
                "食品" -> FOOD
                "零食糖果" -> SNACKS
                "冷饮" -> COLD_DRINKS
                "方便食品" -> CONVENIENCE_FOOD
                "纺织品" -> TEXTILES
                "饮品" -> BEVERAGES
                "调味品" -> CONDIMENTS
                "交通出行" -> TRANSPORTATION
                "餐饮" -> DINING
                "医疗费用" -> MEDICAL
                "水果" -> FRUITS
                "其他" -> OTHER
                "水产品" -> SEAFOOD
                "乳制品" -> DAIRY
                "礼物人情" -> GIFTS
                "旅行度假" -> TRAVEL
                "政务" -> GOVERNMENT
                "水电煤气" -> UTILITIES
                "美容美发" -> BEAUTY
                "豆制品" -> BEAN_PRODUCTS
                "个护美妆" -> COSMETICS
                "电子产品" -> ELECTRONICS
                "家用电器" -> HOUSEHOLD_APPLIANCES
                "五金" -> HARDWARE
                "服装" -> CLOTHING
                else -> OTHER
            }
        }
    }
}
