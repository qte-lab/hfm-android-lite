package com.chronie.homemoneylite.ui.expense

import android.annotation.SuppressLint
import android.content.Context
import com.chronie.homemoneylite.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@SuppressLint("DefaultLocale")
fun formatDateShort(dateString: String): String {
    try {
        val date = LocalDate.parse(dateString)
        return "${date.year}/${String.format("%02d", date.monthValue)}/${String.format("%02d", date.dayOfMonth)}"
    } catch (_: Exception) {
        return dateString
    }
}

fun formatRelativeDate(dateString: String, context: Context, locale: String? = null): String {
    try {
        val date = LocalDate.parse(dateString)
        val today = LocalDate.now()
        val daysBetween = ChronoUnit.DAYS.between(date, today)

        val dayOfWeek = date.dayOfWeek
        val weekdayString = when (dayOfWeek) {
            DayOfWeek.MONDAY -> context.getString(R.string.monday)
            DayOfWeek.TUESDAY -> context.getString(R.string.tuesday)
            DayOfWeek.WEDNESDAY -> context.getString(R.string.wednesday)
            DayOfWeek.THURSDAY -> context.getString(R.string.thursday)
            DayOfWeek.FRIDAY -> context.getString(R.string.friday)
            DayOfWeek.SATURDAY -> context.getString(R.string.saturday)
            DayOfWeek.SUNDAY -> context.getString(R.string.sunday)
        }

        val formattedDate = if (locale != null) formatDateShort(dateString) else dateString

        return when (daysBetween) {
            0L -> "${context.getString(R.string.date_today)}（$weekdayString）"
            1L -> "${context.getString(R.string.date_yesterday)}（$weekdayString）"
            in 2..6 -> "${context.getString(R.string.date_days_ago, daysBetween)}（$weekdayString）"
            else -> "$formattedDate（$weekdayString）"
        }
    } catch (_: Exception) {
        return dateString
    }
}

fun formatDateByLocale(dateString: String): String {
    try {
        val date = LocalDate.parse(dateString)
        return "${date.year}年${date.monthValue}月${date.dayOfMonth}日"
    } catch (_: Exception) {
        return dateString
    }
}

fun formatMonthLabelByLocale(dateString: String): String {
    try {
        val date = LocalDate.parse(dateString)
        return "${date.year}年${date.monthValue}月"
    } catch (_: Exception) {
        return dateString
    }
}
