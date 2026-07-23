package com.chronie.homemoneylite.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import java.time.Instant
import java.time.ZoneId

/**
 * MD2 兼容的日期选择对话框（替代 material3 的 DatePicker/DatePickerDialog）。
 * 内部使用自定义滚轮（xxxx年 xx月 xx日 一行三列），无额外依赖。
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
    var date by remember {
        mutableStateOf(
            Instant.ofEpochMilli(initialDateMillis)
                .atZone(ZoneId.systemDefault()).toLocalDate()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val result = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
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
                WheelDatePicker(
                    date = date,
                    onDateChange = { date = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}
