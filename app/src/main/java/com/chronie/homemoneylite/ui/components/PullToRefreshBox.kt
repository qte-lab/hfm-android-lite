package com.chronie.homemoneylite.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

/**
 * MD2 兼容的 PullToRefresh 包装组件（替代 material3 的 PullToRefreshBox）。
 * 基于 accompanist-swiperefresh 实现，内部自行管理刷新状态，因此调用方无需传递 state。
 *
 * @param isRefreshing 是否正在刷新
 * @param onRefresh 触发刷新时的回调
 * @param modifier 修饰符
 * @param content 内容（与 material3 版本一致，为 BoxScope 作用域）
 */
@Composable
fun PullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing),
        onRefresh = onRefresh,
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}
