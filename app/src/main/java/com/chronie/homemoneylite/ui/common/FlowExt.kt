package com.chronie.homemoneylite.ui.common

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * 在 Fragment 中安全地收集 Flow，仅在 STARTED 及以上生命周期活跃。
 * Kotlin 1.6.10 / lifecycle-runtime-ktx 2.4.x 提供 repeatOnLifecycle。
 */
inline fun <T> Fragment.collectWithLifecycle(
    flow: Flow<T>,
    crossinline action: (T) -> Unit
) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect { action(it) }
        }
    }
}
