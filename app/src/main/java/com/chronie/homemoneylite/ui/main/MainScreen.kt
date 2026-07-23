package com.chronie.homemoneylite.ui.main

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.ui.expense.ExpenseListScreen
import com.chronie.homemoneylite.ui.settings.SettingsScreen
import com.chronie.homemoneylite.ui.theme.*

@Composable
fun MainScreen(
    context: Context,
    shouldRefreshExpenses: Boolean = false,
    onRefreshHandled: () -> Unit = {},
    selectedTab: Int = 0,
    onTabChange: (Int) -> Unit = {},
    onNavigateToSettings: () -> Unit,
    onNavigateToAddExpense: () -> Unit = {},
    onNavigateToEditExpense: (expenseId: String) -> Unit = {},
    onNavigateToMoreFunctions: () -> Unit = {},
    onNavigateToWeekdayDetail: (dayOfWeek: Int, amount: Double, count: Int, percentage: Float, startDate: String, endDate: String) -> Unit = { _, _, _, _, _, _ -> },
    onNavigateToLanSync: () -> Unit = {}
) {

    // 原生界面（带底部 Tab 栏）
    Box(modifier = Modifier
        .fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            when (selectedTab) {
                0 -> {
                    // 支出记录界面
                    ExpenseListScreen(
                        context = context,
                        shouldRefresh = shouldRefreshExpenses,
                        onRefreshHandled = onRefreshHandled,
                        onNavigateToMoreFunctions = {},
                        onNavigateToAddExpense = onNavigateToAddExpense,
                        onNavigateToEditExpense = onNavigateToEditExpense
                    )
                }
                1 -> {
                    // 图表界面
                    com.chronie.homemoneylite.ui.charts.ChartsScreen(
                        context = context,
                        onNavigateToWeekdayDetail = onNavigateToWeekdayDetail
                    )
                }
                2 -> {
                    // 设置界面
                    SettingsScreen(
                        context = context,
                        onNavigateToLanSync = onNavigateToLanSync
                    )
                }
            }
        }

        // 悬浮导航栏
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            BottomNavigationBar(
                context = context,
                selectedTab = selectedTab,
                onTabChange = onTabChange
            )
        }
    }
}
