package com.chronie.homemoney.ui.budget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chronie.homemoney.R
import com.chronie.homemoney.domain.model.BudgetStatus
import com.chronie.homemoney.ui.components.ExpressiveLinearProgressIndicator
import com.chronie.homemoney.ui.components.ExpressiveLoadingIndicator
import com.chronie.homemoney.ui.expense.formatMonthLabelByLocale
import java.util.Locale

/**
 * 预算管理卡片
 * 显示在支出列表界面的标题栏下方
 */
@Composable
fun BudgetCard(
    context: android.content.Context,
    viewModel: BudgetViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    refreshTrigger: Int = 0
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    
    // 监听刷新触发器
    LaunchedEffect(refreshTrigger) {
        viewModel.refresh()
    }
    
    // 如果预算功能未启用，显示启用提示
    if (uiState.budget?.isEnabled != true) {
        BudgetEnablePrompt(
            context = context,
            onEnableClick = {
                showSettings = true
            },
            modifier = modifier
        )
    } else {
        // 显示预算使用情况
        val usage = uiState.budgetUsage
        if (usage != null) {
            BudgetUsageCard(
                context = context,
                usage = usage,
                onSettingsClick = { showSettings = true },
                modifier = modifier
            )
        } else {
            // 加载中状态
            Card(
                modifier = modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ExpressiveLoadingIndicator()
                }
            }
        }
    }
    
    // 预算设置对话框
    if (showSettings) {
        BudgetSettingsDialog(
            context = context,
            currentBudget = uiState.budget,
            onDismiss = { showSettings = false },
            onSave = { limit, threshold, enabled ->
                viewModel.saveBudget(limit, threshold, enabled)
                showSettings = false
            }
        )
    }
}

/**
 * 启用预算提示卡片
 */
@Composable
fun BudgetEnablePrompt(
    context: android.content.Context,
    onEnableClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalance,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            Text(
                text = context.getString(R.string.budget_enable_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = context.getString(R.string.budget_enable_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
            
            Button(
                onClick = onEnableClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(context.getString(R.string.budget_enable_button))
            }
        }
    }
}

/**
 * 预算使用情况卡片
 */
@Composable
fun BudgetUsageCard(
    context: android.content.Context,
    usage: com.chronie.homemoney.domain.model.BudgetUsage,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    val status = when {
        usage.isOverLimit -> BudgetStatus.OVER_LIMIT
        usage.isNearLimit -> BudgetStatus.WARNING
        else -> BudgetStatus.NORMAL
    }
    
    val progressColor = when (status) {
        BudgetStatus.OVER_LIMIT -> MaterialTheme.colorScheme.error
        BudgetStatus.WARNING -> MaterialTheme.colorScheme.tertiary
        BudgetStatus.NORMAL -> MaterialTheme.colorScheme.primary
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标题行（始终显示）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 收起状态下的简要信息
                if (!isExpanded) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatMonthLabelByLocale(usage.currentMonth + "-01", context.resources.configuration.locale.toLanguageTag()),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = context.getString(R.string.currency_format_no_decimal, context.getString(R.string.currency_symbol), usage.currentSpending) + "/" + context.getString(R.string.currency_format_no_decimal, context.getString(R.string.currency_symbol), usage.monthlyLimit),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = progressColor
                        )
                        Text(
                            text = "(${String.format(Locale.getDefault(), "%.0f", usage.spendingPercentage)}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = progressColor
                        )
                    }
                } else {
                    // 展开状态下的标题
                    Column {
                        Text(
                            text = context.getString(R.string.budget_monthly_progress),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatMonthLabelByLocale(usage.currentMonth + "-01", context.resources.configuration.locale.toLanguageTag()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
                
                // 展开/收起按钮和设置按钮
                Row {
                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) context.getString(R.string.budget_collapse) else context.getString(R.string.budget_expand)
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = context.getString(R.string.budget_settings)
                        )
                    }
                }
            }
            
            // 详细内容（可展开/收起）
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 金额信息
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = context.getString(R.string.currency_format, context.getString(R.string.currency_symbol), usage.currentSpending),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = progressColor
                        )
                        Text(
                            text = "/ " + context.getString(R.string.currency_format, context.getString(R.string.currency_symbol), usage.monthlyLimit),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "(${String.format(Locale.getDefault(), "%.0f", usage.spendingPercentage)}%)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = progressColor
                        )
                    }
                    
                    // 进度条
                    ExpressiveLinearProgressIndicator(
                        progress = (usage.spendingPercentage / 100).toFloat().coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = progressColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    
                    // 状态提示
                    when (status) {
                        BudgetStatus.OVER_LIMIT -> {
                            AlertCard(
                                context = context,
                                title = context.getString(R.string.budget_alert_over_title),
                                message = context.getString(
                                    R.string.budget_alert_over_message,
                                    context.getString(R.string.currency_format, context.getString(R.string.currency_symbol), usage.currentSpending - usage.monthlyLimit)
                                ),
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        BudgetStatus.WARNING -> {
                            AlertCard(
                                context = context,
                                title = context.getString(R.string.budget_alert_warning_title),
                                message = context.getString(
                                    R.string.budget_alert_warning_message,
                                    context.getString(R.string.currency_format, context.getString(R.string.currency_symbol), usage.remainingAmount),
                                    usage.spendingPercentage
                                ),
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        BudgetStatus.NORMAL -> {
                            AlertCard(
                                context = context,
                                title = context.getString(R.string.budget_alert_normal_title),
                                message = context.getString(
                                    R.string.budget_alert_normal_message,
                                    context.getString(R.string.currency_format, context.getString(R.string.currency_symbol), usage.remainingAmount),
                                    100 - usage.spendingPercentage
                                ),
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    
                    // 详细信息
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DetailItem(
                            context = context,
                            label = context.getString(R.string.budget_daily_average),
                            value = context.getString(R.string.currency_format, context.getString(R.string.currency_symbol), usage.dailyAverage)
                        )
                        
                        DetailItem(
                            context = context,
                            label = context.getString(R.string.budget_recommended_daily),
                            value = context.getString(R.string.currency_format, context.getString(R.string.currency_symbol), usage.recommendedDaily),
                            valueColor = if (usage.recommendedDaily <= 0) {
                                MaterialTheme.colorScheme.error
                            } else if (usage.recommendedDaily < usage.dailyAverage * 0.8) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 警告卡片
 */
@Composable
fun AlertCard(
    context: android.content.Context,
    title: String,
    message: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * 详细信息项
 */
@Composable
fun DetailItem(
    context: android.content.Context,
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onPrimaryContainer,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}