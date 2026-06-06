package com.chronie.homemoney.ui.budget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chronie.homemoney.R
import com.chronie.homemoney.domain.model.Budget
import com.chronie.homemoney.ui.components.ExpressiveSwitch
import com.chronie.homemoney.ui.components.CircularIconButton

/**
 * 预算设置对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSettingsDialog(
    context: android.content.Context,
    currentBudget: Budget?,
    onDismiss: () -> Unit,
    onSave: (monthlyLimit: Double, warningThreshold: Double, isEnabled: Boolean) -> Unit
) {
    var monthlyLimit by remember { 
        mutableStateOf(currentBudget?.monthlyLimit?.toString() ?: "")
    }
    var warningThreshold by remember { 
        mutableStateOf(((currentBudget?.warningThreshold ?: 0.8) * 100).toString())
    }
    var isEnabled by remember { 
        mutableStateOf(currentBudget?.isEnabled ?: false)
    }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(context.getString(R.string.budget_settings_title))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 启用开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = context.getString(R.string.budget_enable_feature),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    ExpressiveSwitch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it }
                    )
                }
                
                // 月度预算输入
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularIconButton(
                        onClick = {
                            val current = monthlyLimit.toDoubleOrNull() ?: 0.0
                            monthlyLimit = (current - 1000.0).coerceAtLeast(0.0).toString()
                            showError = false
                        },
                        enabled = isEnabled
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null)
                    }
                    OutlinedTextField(
                        value = monthlyLimit,
                        onValueChange = { 
                            monthlyLimit = it
                            showError = false
                        },
                        label = { Text(context.getString(R.string.budget_monthly_limit)) },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        enabled = isEnabled,
                        isError = showError
                    )
                    CircularIconButton(
                        onClick = {
                            val current = monthlyLimit.toDoubleOrNull() ?: 0.0
                            monthlyLimit = (current + 1000.0).toString()
                            showError = false
                        },
                        enabled = isEnabled
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                }
                
                // 警告阈值输入
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularIconButton(
                        onClick = {
                            val current = warningThreshold.toDoubleOrNull() ?: 80.0
                            warningThreshold = (current - 10.0).coerceAtLeast(0.0).toString()
                            showError = false
                        },
                        enabled = isEnabled
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null)
                    }
                    OutlinedTextField(
                        value = warningThreshold,
                        onValueChange = { 
                            warningThreshold = it
                            showError = false
                        },
                        label = { Text(context.getString(R.string.budget_warning_threshold)) },
                        placeholder = { Text("80") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        enabled = isEnabled,
                        isError = showError,
                        supportingText = {
                            Text(context.getString(R.string.budget_warning_threshold_hint))
                        }
                    )
                    CircularIconButton(
                        onClick = {
                            val current = warningThreshold.toDoubleOrNull() ?: 80.0
                            warningThreshold = (current + 10.0).coerceAtMost(100.0).toString()
                            showError = false
                        },
                        enabled = isEnabled
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                }
                
                // 错误提示
                if (showError) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // 验证输入
                    val limit = monthlyLimit.toDoubleOrNull()
                    val threshold = warningThreshold.toDoubleOrNull()
                    
                    when {
                        isEnabled && (limit == null || limit <= 0) -> {
                            showError = true
                            errorMessage = context.getString(R.string.budget_error_invalid_limit)
                        }
                        isEnabled && (threshold == null || threshold < 0 || threshold > 100) -> {
                            showError = true
                            errorMessage = context.getString(R.string.budget_error_invalid_threshold)
                        }
                        else -> {
                            onSave(
                                limit ?: 0.0,
                                (threshold ?: 80.0) / 100,
                                isEnabled
                            )
                        }
                    }
                }
            ) {
                Text(context.getString(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(context.getString(R.string.common_cancel))
            }
        }
    )
}
