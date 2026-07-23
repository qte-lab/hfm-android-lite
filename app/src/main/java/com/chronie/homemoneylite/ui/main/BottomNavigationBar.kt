package com.chronie.homemoneylite.ui.main

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.ui.theme.*

data class TabItemData(
    val icon: ImageVector,
    val label: String,
    val index: Int
)

@Composable
fun BottomNavigationBar(
    context: Context,
    selectedTab: Int,
    onTabChange: (Int) -> Unit
) {
    val navigationItems = listOf(
        TabItemData(
            icon = Icons.Default.Home,
            label = context.getString(R.string.expense_list_title),
            index = 0
        ),
        TabItemData(
            icon = Icons.Default.InsertChart,
            label = context.getString(R.string.charts_title),
            index = 1
        ),
        TabItemData(
            icon = Icons.Default.Settings,
            label = context.getString(R.string.settings),
            index = 2
        )
    )

    BottomNavigation(
        modifier = Modifier
            .fillMaxWidth(),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 4.dp,
        contentColor = MaterialTheme.colors.onSurface
    ) {
        navigationItems.forEach { item ->
            BottomNavigationItem(
                selected = selectedTab == item.index,
                onClick = { onTabChange(item.index) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(text = item.label)
                },
                alwaysShowLabel = true,
                selectedContentColor = MaterialTheme.colors.primary,
                unselectedContentColor = MaterialTheme.colors.onSurfaceVariant
            )
        }
    }
}
