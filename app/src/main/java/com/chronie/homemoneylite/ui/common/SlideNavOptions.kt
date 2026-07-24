package com.chronie.homemoneylite.ui.common

import androidx.navigation.NavOptions
import com.chronie.homemoneylite.R

/**
 * 二级页面统一使用的水平滑入/滑出转场动画。
 *
 * 注意：仅设置 nav_graph 目的地上的 app:enterAnim 等属性，在通过
 * findNavController().navigate(resId) 不带 NavOptions 跳转时，部分 Navigation
 * 版本不会自动应用（表现为完全无动画）。因此这里集中返回一个 NavOptions，
 * 在每次 navigate() 调用时显式传入，确保动画稳定生效。
 */
fun slideNavOptions(): NavOptions = NavOptions.Builder()
    .setEnterAnim(R.anim.slide_in_right)
    .setExitAnim(R.anim.slide_out_left)
    .setPopEnterAnim(R.anim.slide_in_left)
    .setPopExitAnim(R.anim.slide_out_right)
    .build()
