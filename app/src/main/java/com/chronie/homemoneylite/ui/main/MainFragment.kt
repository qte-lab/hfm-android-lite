package com.chronie.homemoneylite.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.databinding.FragmentMainBinding
import com.chronie.homemoneylite.ui.charts.ChartsFragment
import com.chronie.homemoneylite.ui.expense.ExpenseListFragment
import com.chronie.homemoneylite.ui.settings.SettingsFragment
import dagger.hilt.android.AndroidEntryPoint

/**
 * 主框架：底部导航 + 三个 Tab 子 Fragment。
 * 子 Fragment 采用 add + show/hide 复用，切 Tab 不重建视图（低端机友好）。
 */
@AndroidEntryPoint
class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private var selectedTabId: Int = R.id.tab_expense

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 仅在有保存状态时恢复；否则保留当前选中的 Tab（避免从二级页返回时强制跳回“支出”）
        selectedTabId = savedInstanceState?.getInt(KEY_SELECTED_TAB) ?: selectedTabId

        binding.tabExpense.setOnClickListener { selectTab(R.id.tab_expense) }
        binding.tabCharts.setOnClickListener { selectTab(R.id.tab_charts) }
        binding.tabSettings.setOnClickListener { selectTab(R.id.tab_settings) }

        selectTab(selectedTabId)
    }

    private fun selectTab(tabId: Int) {
        selectedTabId = tabId
        updateTabVisual()
        showTab(tabId)
    }

    private fun updateTabVisual() {
        val ctx = requireContext()
        listOf(
            R.id.tab_expense to TabItem(binding.tabExpense, binding.tabExpenseText, binding.tabExpenseIndicator),
            R.id.tab_charts to TabItem(binding.tabCharts, binding.tabChartsText, binding.tabChartsIndicator),
            R.id.tab_settings to TabItem(binding.tabSettings, binding.tabSettingsText, binding.tabSettingsIndicator)
        ).forEach { (id, item) ->
            val selected = id == selectedTabId
            item.container.isSelected = selected
            item.textView.setTextColor(
                ContextCompat.getColor(
                    ctx,
                    if (selected) R.color.holo_blue else R.color.text_secondary
                )
            )
            item.indicator.visibility = if (selected) View.VISIBLE else View.GONE
        }
    }

    private data class TabItem(
        val container: View,
        val textView: TextView,
        val indicator: View
    )

    private fun showTab(tabId: Int) {
        val fm = childFragmentManager
        val tag = when (tabId) {
            R.id.tab_charts -> TAG_CHARTS
            R.id.tab_settings -> TAG_SETTINGS
            else -> TAG_EXPENSE
        }
        val transaction = fm.beginTransaction()
        var target = fm.findFragmentByTag(tag)
        if (target == null) {
            target = createFragment(tag)
            transaction.add(R.id.tab_container, target, tag)
        }
        fm.fragments.forEach { fragment ->
            if (fragment !== target && !fragment.isHidden) {
                transaction.hide(fragment)
                transaction.setMaxLifecycle(fragment, androidx.lifecycle.Lifecycle.State.STARTED)
            }
        }
        transaction.show(target)
        transaction.setMaxLifecycle(target, androidx.lifecycle.Lifecycle.State.RESUMED)
        transaction.commitNowAllowingStateLoss()
    }

    private fun createFragment(tag: String): Fragment = when (tag) {
        TAG_CHARTS -> ChartsFragment()
        TAG_SETTINGS -> SettingsFragment()
        else -> ExpenseListFragment()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SELECTED_TAB, selectedTabId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val KEY_SELECTED_TAB = "selected_tab"
        private const val TAG_EXPENSE = "tab_expense"
        private const val TAG_CHARTS = "tab_charts"
        private const val TAG_SETTINGS = "tab_settings"
    }
}
