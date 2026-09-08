package com.chronie.homemoneylite.ui.expense

import android.annotation.SuppressLint
import android.content.Context
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.core.common.AppLanguageManager
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

/**
 * 日期格式化工具（国际化）。
 *
 * 所有展示用日期都不再硬编码「年月日」，而是按当前生效语言取 ICU 骨架：
 * - 中文：2026年9月8日 / 2026年9月 / 2026/09/08
 * - 英文：Sep 8, 2026 / Sep 2026 / 09/08/2026
 *
 * 语言统一取自 AppLanguageManager.resolveLocale()（跟随系统或应用内选择），
 * 而不是 Locale.getDefault()，保证应用内切语言后立即生效。
 */

private fun parseLocalDate(dateString: String): LocalDate? =
    try {
        LocalDate.parse(dateString)
    } catch (_: Exception) {
        null
    }

private fun LocalDate.toDate(): Date =
    Date(atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())

private fun appLocale(context: Context): Locale = AppLanguageManager.resolveLocale(context)

@SuppressLint("SimpleDateFormat")
private fun formatWithSkeleton(date: LocalDate, locale: Locale, skeleton: String): String {
    val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, skeleton)
    return SimpleDateFormat(pattern, locale).format(date.toDate())
}

/** 短日期：年月日，顺序与分隔符随语言（中文 2026/09/08，英文 09/08/2026） */
@SuppressLint("DefaultLocale")
fun formatDateShort(dateString: String, locale: Locale = Locale.getDefault()): String {
    val date = parseLocalDate(dateString) ?: return dateString
    return formatWithSkeleton(date, locale, "yyyyMMdd")
}

/** 月/日短格式，用于图表 X 轴 */
fun formatMonthDay(context: Context, dateString: String): String {
    val date = parseLocalDate(dateString) ?: return dateString
    return formatWithSkeleton(date, appLocale(context), "MMdd")
}

/** 完整日期：中文 2026年9月8日，英文 Sep 8, 2026 */
fun formatDateByLocale(context: Context, dateString: String): String {
    val date = parseLocalDate(dateString) ?: return dateString
    return DateFormat.getDateInstance(DateFormat.MEDIUM, appLocale(context)).format(date.toDate())
}

/** 年月标签：中文 2026年9月，英文 Sep 2026 */
fun formatMonthLabelByLocale(context: Context, dateString: String): String {
    val date = parseLocalDate(dateString) ?: return dateString
    return formatWithSkeleton(date, appLocale(context), "yyyyMMM")
}

/** 日期时间（含时分）：中文 2026年9月8日 15:34，英文 Sep 8, 2026, 3:34 PM */
fun formatDateTimeByLocale(context: Context, epochMillis: Long): String {
    val locale = appLocale(context)
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
        .format(Date(epochMillis))
}

@SuppressLint("DefaultLocale")
fun formatRelativeDate(dateString: String, context: Context, locale: String? = null): String {
    val date = parseLocalDate(dateString) ?: return dateString
    val today = LocalDate.now()
    val daysBetween = ChronoUnit.DAYS.between(date, today)

    val weekdayString = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> context.getString(R.string.monday)
        DayOfWeek.TUESDAY -> context.getString(R.string.tuesday)
        DayOfWeek.WEDNESDAY -> context.getString(R.string.wednesday)
        DayOfWeek.THURSDAY -> context.getString(R.string.thursday)
        DayOfWeek.FRIDAY -> context.getString(R.string.friday)
        DayOfWeek.SATURDAY -> context.getString(R.string.saturday)
        DayOfWeek.SUNDAY -> context.getString(R.string.sunday)
    }

    val formattedDate = if (locale != null) formatDateShort(dateString, appLocale(context)) else dateString

    val label = when (daysBetween) {
        0L -> context.getString(R.string.date_today)
        1L -> context.getString(R.string.date_yesterday)
        in 2..6 -> context.getString(R.string.date_days_ago, daysBetween)
        else -> formattedDate
    }
    // 括号形态也随语言：中文「今天（星期一）」，英文 "Today (Monday)"
    return context.getString(R.string.date_with_weekday, label, weekdayString)
}
