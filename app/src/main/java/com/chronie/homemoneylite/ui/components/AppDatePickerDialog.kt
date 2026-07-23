package com.chronie.homemoneylite.ui.components

import android.view.ContextThemeWrapper
import android.widget.DatePicker
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.chronie.homemoneylite.R
import java.util.Calendar

/**
 * MD2 兼容的日期选择对话框（替代 material3 的 DatePicker/DatePickerDialog）。
 * 基于原生 android.widget.DatePicker，无额外依赖。
 *
 * @param initialDateMillis 初始选中时间（毫秒）
 * @param onDateSelected 用户点击“确定”后回调，返回本地零点毫秒值
 * @param onDismiss 关闭（取消/点击外部）回调
 */
@Composable
fun AppDatePickerDialog(
    initialDateMillis: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val calendar = Calendar.getInstance().apply { timeInMillis = initialDateMillis }
    var year by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var month by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var day by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }

    val isDark = isSystemInDarkTheme()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val result = Calendar.getInstance().apply {
                    set(year, month, day, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                onDateSelected(result)
            }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        text = {
            Column(Modifier.padding(top = 8.dp)) {
                AndroidView(
                    factory = { context ->
                        val pickerContext = ContextThemeWrapper(
                            context,
                            if (isDark) R.style.DatePickerDialogDark else R.style.DatePickerDialogLight
                        )
                        DatePicker(pickerContext).apply {
                            init(year, month, day) { _, y, m, d ->
                                year = y
                                month = m
                                day = d
                            }
                        }
                    },
                    update = { picker ->
                        picker.updateDate(year, month, day)
                    }
                )
            }
        }
    )
}
