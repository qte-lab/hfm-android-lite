package com.chronie.homemoney.ui.main

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.chronie.homemoney.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// 数据类 for 导航项
data class TabItemData(
    val icon: @Composable () -> Unit,
    val label: String,
    val index: Int
)

@Composable
fun BottomNavigationBar(
    context: Context,
    selectedTab: Int,
    onTabChange: (Int) -> Unit
) {
    // 获取当前主题模式
    val isDarkTheme = isSystemInDarkTheme()

    // 导航栏背景色：根据主题模式设置半透明效果，使用更透明的背景
    val backgroundColor = if (isDarkTheme) Color(0x991E1E1E) else Color(0x99F5F5F5)
    
    // 未选中项颜色：浅色模式为黑色，深色模式为白色
    val unselectedColor = if (isDarkTheme) Color(0xFFAAAAAA) else Color(0xFF666666)
    
    // 选中项颜色：从主题中获取primary颜色
    val selectedColor = MaterialTheme.colorScheme.primary
    
    // 选中项背景色：使用 MaterialTheme 的 primaryContainer 并设置半透明
    val selectedBackgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    
    // 定义导航项
    val navigationItems = listOf(
        TabItemData(
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = context.getString(R.string.expense_list_title),
            index = 0
        ),
        TabItemData(
            icon = { Icon(Icons.Default.InsertChart, contentDescription = null) },
            label = context.getString(R.string.charts_title),
            index = 1
        ),
        TabItemData(
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = context.getString(R.string.settings),
            index = 2
        )
    )
    
    BottomNavigationBarImpl(
        selectedTabIndex = selectedTab,
        onTabSelected = onTabChange,
        items = navigationItems,
        backgroundColor = backgroundColor,
        selectedColor = selectedColor,
        unselectedColor = unselectedColor,
        selectedBackgroundColor = selectedBackgroundColor
    )
}

@Composable
private fun BottomNavigationBarImpl(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    items: List<TabItemData>,
    backgroundColor: Color,
    selectedColor: Color,
    unselectedColor: Color,
    selectedBackgroundColor: Color
) {
    val animationScope = rememberCoroutineScope()
    var currentIndex by remember { mutableIntStateOf(selectedTabIndex) }
    
    // 用于动画的背景位置，初始化为当前选中的 tab 索引
    val backgroundPosition = remember {
        Animatable(selectedTabIndex.toFloat())
    }
    
    // 监听选中索引变化
    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex != currentIndex) {
            currentIndex = selectedTabIndex
            backgroundPosition.snapTo(selectedTabIndex.toFloat())
        } else if (backgroundPosition.value != selectedTabIndex.toFloat()) {
            // 确保动画位置与选中索引一致（处理组件重建的情况）
            backgroundPosition.snapTo(selectedTabIndex.toFloat())
        }
    }
    
    // 自定义悬浮导航栏，使用圆角长矩形设计
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .windowInsetsPadding(WindowInsets.navigationBars),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier.height(64.dp)
        ) {
            val density = LocalDensity.current
            
            // 计算每个tab项的宽度（基于内容）
            val tabWidth = 80.dp
            val totalWidth = tabWidth * items.size + 16.dp
            val tabWidthPx = with(density) { tabWidth.toPx() }
            
            Box(
                modifier = Modifier
                    .width(totalWidth)
                    .height(64.dp)
                    .background(
                        color = backgroundColor,
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                // 选中项背景，在整个 Row 范围内移动
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .offset(
                            x = with(density) {
                                val animatedPosition = backgroundPosition.value
                                (animatedPosition * tabWidthPx).toDp()
                            },
                            y = 0.dp
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .width(tabWidth)
                            .fillMaxHeight()
                            .background(
                                color = selectedBackgroundColor,
                                shape = RoundedCornerShape(16.dp)
                            )
                    )
                }
                
                // 选项内容层
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEach { item ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            TabItem(
                                item = item,
                                isSelected = currentIndex == item.index,
                                selectedColor = selectedColor,
                                unselectedColor = unselectedColor,
                                onTabSelected = {
                                    animationScope.launch {
                                        backgroundPosition.animateTo(
                                            targetValue = item.index.toFloat(),
                                            animationSpec = spring(
                                                dampingRatio = 0.7f,
                                                stiffness = 300f
                                            )
                                        )
                                        currentIndex = item.index
                                        onTabSelected(item.index)
                                    }
                                }
                            )
                        }
                    }
                }
            }
            }
        }
    }

@Composable
private fun TabItem(
    item: TabItemData,
    isSelected: Boolean,
    selectedColor: Color,
    unselectedColor: Color,
    onTabSelected: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onTabSelected()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            modifier = Modifier
                .size(24.dp)
                .padding(bottom = 2.dp),
            imageVector = when (item.index) {
                0 -> Icons.Default.Home
                1 -> Icons.Default.InsertChart
                2 -> Icons.Default.Settings
                else -> Icons.Default.Home
            },
            contentDescription = item.label,
            tint = if (isSelected) selectedColor else unselectedColor
        )
        
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) selectedColor else unselectedColor
        )
    }
}