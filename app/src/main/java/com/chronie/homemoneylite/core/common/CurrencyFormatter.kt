package com.chronie.homemoneylite.core.common

import android.content.Context
import com.chronie.homemoneylite.R
import java.text.NumberFormat
import java.util.Locale

/**
 * 金额格式化：货币符号**始终**取 `R.string.currency_symbol`（¥），不随系统/应用语言本地化，
 * 避免英文等语言下被 NumberFormat.getCurrencyInstance() 换成 $、€ 等符号。
 *
 * 数字部分（千分位分组、小数位）仍按当前语言习惯输出，例如：
 * - 中文 / 英文：¥1,234.56
 * - 部分欧陆语言：¥1.234,56
 */
object CurrencyFormatter {

    /** 固定货币符号，默认 ¥ */
    fun symbol(context: Context): String = context.getString(R.string.currency_symbol)

    /** 数字部分格式化器（不含货币符号），千分位/小数位随语言习惯 */
    @JvmOverloads
    fun numberFormat(locale: Locale = Locale.getDefault(), decimals: Boolean = true): NumberFormat =
        NumberFormat.getNumberInstance(locale).apply {
            val digits = if (decimals) 2 else 0
            minimumFractionDigits = digits
            maximumFractionDigits = digits
        }

    /** 格式化为「¥ + 数字」，decimals=false 时省略小数；语言取应用当前生效语言 */
    @JvmOverloads
    fun format(context: Context, value: Double, decimals: Boolean = true): String =
        symbol(context) + numberFormat(AppLanguageManager.resolveLocale(context), decimals).format(value)

    /** 需要多次格式化时先取一次 lambda，避免反复读取资源 */
    fun formatter(context: Context, decimals: Boolean = true): (Double) -> String {
        val prefix = symbol(context)
        val nf = numberFormat(AppLanguageManager.resolveLocale(context), decimals)
        return { value -> prefix + nf.format(value) }
    }
}
