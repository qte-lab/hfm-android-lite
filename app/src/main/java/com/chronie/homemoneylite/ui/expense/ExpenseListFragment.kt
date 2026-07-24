package com.chronie.homemoneylite.ui.expense

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.LocaleListCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.databinding.FragmentExpenseListBinding
import com.chronie.homemoneylite.domain.model.Expense
import com.chronie.homemoneylite.domain.model.ExpenseFilters
import com.chronie.homemoneylite.domain.model.SortOption
import com.chronie.homemoneylite.ui.budget.BudgetSettingsDialogFragment
import com.chronie.homemoneylite.ui.budget.BudgetUiState
import com.chronie.homemoneylite.ui.budget.BudgetViewModel
import com.chronie.homemoneylite.ui.common.collectWithLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import androidx.appcompat.widget.PopupMenu

/**
 * 支出列表页（传统 View 版本，对应 Compose 的 ExpenseListScreen）。
 * 作为 MainFragment 的子 Fragment 存在；数据刷新在 onResume 触发（替代原 shouldRefresh 参数机制），
 * 并用 isHidden 守卫避免隐藏时无效刷新。
 */
@AndroidEntryPoint
class ExpenseListFragment : Fragment(R.layout.fragment_expense_list) {

    private var _binding: FragmentExpenseListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExpenseListViewModel by viewModels()
    private val budgetViewModel: BudgetViewModel by viewModels()

    private lateinit var adapter: ExpenseListAdapter

    private var latestFilters: ExpenseFilters = ExpenseFilters()
    private var latestBudget: BudgetUiState = BudgetUiState()
    private var lastLoadMoreTime = 0L

    private val locale: String
        get() = LocaleListCompat.getDefault().get(0)?.toLanguageTag() ?: "zh-CN"

    private lateinit var statCount: TextView
    private lateinit var statTotal: TextView
    private lateinit var statAverage: TextView
    private lateinit var statMedian: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpenseListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        statCount = binding.statsCard.statCount
        statTotal = binding.statsCard.statTotal
        statAverage = binding.statsCard.statAverage
        statMedian = binding.statsCard.statMedian

        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()

        binding.statusRetry.setOnClickListener { viewModel.refresh() }

        binding.budgetCardView.onSettingsRequested = { showBudgetSettings() }

        observeState()
        observeBudget()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        if (!isHidden) {
            viewModel.refresh()
            budgetViewModel.refresh()
        }
    }

    private fun setupToolbar() {
        binding.btnSort.setOnClickListener { showSortMenu() }
        binding.btnAdd.setOnClickListener { showAddMenu() }
        binding.btnMore.setOnClickListener { showMoreMenu() }
    }

    private fun setupRecyclerView() {
        adapter = ExpenseListAdapter(requireContext())
        adapter.setHasStableIds(true)
        adapter.onItemClick = { expense -> navigateEdit(expense.id) }
        adapter.onItemLongClick = { expense -> showActionDialog(expense) }
        adapter.onLoadMoreClick = { viewModel.loadMore() }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                val lm = rv.layoutManager as LinearLayoutManager
                val total = lm.itemCount
                val last = lm.findLastVisibleItemPosition()
                if (total > 0 && last >= total - 1) {
                    val now = System.currentTimeMillis()
                    val state = viewModel.uiState.value
                    if (now - lastLoadMoreTime > 1000 && state.hasMore && !state.isLoading) {
                        lastLoadMoreTime = now
                        viewModel.loadMore()
                    }
                }
            }
        })
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
            budgetViewModel.refresh()
        }
    }

    private fun observeState() {
        collectWithLifecycle(viewModel.uiState) { state ->
            latestFilters = state.filters

            val items = mutableListOf<ExpenseListAdapter.ListItem>()
            state.groupedExpenses.forEach { (date, expenses) ->
                items.add(
                    ExpenseListAdapter.ListItem.DateHeader(
                        date = date,
                        count = expenses.size,
                        totalAmount = expenses.sumOf { it.amount }
                    )
                )
                expenses.forEach { items.add(ExpenseListAdapter.ListItem.ExpenseItem(it)) }
            }
            if (state.hasMore) items.add(ExpenseListAdapter.ListItem.LoadMore)

            adapter.submitList(items)
            adapter.bindLoadMoreState(state.isLoading)

            binding.swipeRefresh.isRefreshing = state.isLoading && state.currentPage == 1

            updateStatus(state)
            bindStats(state)
        }
    }

    private fun observeBudget() {
        collectWithLifecycle(budgetViewModel.uiState) { state ->
            latestBudget = state
            binding.budgetCardView.bind(state)
        }
    }

    private fun updateStatus(state: ExpenseListUiState) {
        when {
            state.isLoading && state.expenses.isEmpty() -> {
                binding.statusContainer.visibility = View.VISIBLE
                binding.statusText.setText(R.string.common_loading)
                binding.statusDesc.visibility = View.GONE
                binding.statusRetry.visibility = View.GONE
            }
            state.error != null && state.expenses.isEmpty() -> {
                binding.statusContainer.visibility = View.VISIBLE
                binding.statusText.text = state.error ?: getString(R.string.common_error)
                binding.statusDesc.visibility = View.GONE
                binding.statusRetry.visibility = View.VISIBLE
            }
            state.expenses.isEmpty() -> {
                binding.statusContainer.visibility = View.VISIBLE
                binding.statusText.setText(R.string.expense_list_empty)
                binding.statusDesc.setText(R.string.expense_list_empty_description)
                binding.statusDesc.visibility = View.VISIBLE
                binding.statusRetry.visibility = View.GONE
            }
            else -> binding.statusContainer.visibility = View.GONE
        }
    }

    private fun bindStats(state: ExpenseListUiState) {
        val symbol = getString(R.string.currency_symbol)
        val stats = state.statistics
        val fmt = { value: Double -> getString(R.string.currency_format, symbol, value) }
        statCount.text = stats.count.toString()
        statTotal.text = fmt(stats.totalAmount)
        statAverage.text = fmt(stats.averageAmount)
        statMedian.text = fmt(stats.medianAmount)
    }

    private fun showSortMenu() {
        val popup = PopupMenu(requireContext(), binding.btnSort)
        SortOption.values().forEachIndexed { index, option ->
            val item = popup.menu.add(0, index, index, getSortOptionText(option))
            item.isCheckable = true
            item.isChecked = latestFilters.sortBy == option
        }
        popup.setOnMenuItemClickListener { item ->
            val option = SortOption.values()[item.itemId]
            viewModel.updateFilters(latestFilters.copy(sortBy = option))
            true
        }
        popup.show()
    }

    private fun showAddMenu() {
        val popup = PopupMenu(requireContext(), binding.btnAdd)
        popup.menu.add(0, 0, 0, getString(R.string.add_expense_title))
        popup.menu.add(0, 1, 1, getString(R.string.ai_expense_title))
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                0 -> navigateAdd()
                1 -> navigateAI()
            }
            true
        }
        popup.show()
    }

    private fun showMoreMenu() {
        val popup = PopupMenu(requireContext(), binding.btnMore)
        popup.menu.add(0, 0, 0, getString(R.string.common_filter))
        popup.menu.add(0, 1, 1, getString(R.string.expense_list_clear_filters))
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                0 -> openFilterDialog()
                1 -> viewModel.resetFilters()
            }
            true
        }
        popup.show()
    }

    private fun openFilterDialog() {
        val dialog = ExpenseFilterDialogFragment.newInstance(latestFilters)
        dialog.onApplyFilters = { filters -> viewModel.updateFilters(filters) }
        dialog.show(childFragmentManager, "expense_filter")
    }

    private fun showBudgetSettings() {
        val dialog = BudgetSettingsDialogFragment.newInstance(latestBudget.budget)
        dialog.onSave = { limit, threshold, enabled ->
            budgetViewModel.saveBudget(limit, threshold, enabled)
            budgetViewModel.refresh()
        }
        dialog.show(childFragmentManager, "budget_settings")
    }

    private fun showActionDialog(expense: Expense) {
        val ctx = requireContext()
        val typeName = ExpenseTypeLocalizer.getLocalizedName(ctx, expense.type)
        val dateStr = formatDateByLocale(expense.date, locale)
        val amountStr = "-" + getString(
            R.string.currency_format, getString(R.string.currency_symbol), expense.amount
        )
        val message = buildString {
            if (!expense.remark.isNullOrBlank()) append(expense.remark).append("\n")
            append(dateStr).append("\n").append(amountStr)
        }
        MaterialAlertDialogBuilder(ctx)
            .setTitle(typeName)
            .setMessage(message)
            .setPositiveButton(R.string.edit) { _, _ -> navigateEdit(expense.id) }
            .setNegativeButton(R.string.delete) { _, _ -> showDeleteConfirm1(expense) }
            .show()
    }

    private fun showDeleteConfirm1(expense: Expense) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_confirm_title)
            .setMessage(R.string.delete_confirm_message)
            .setPositiveButton(R.string.confirm) { _, _ -> showDeleteConfirm2(expense) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeleteConfirm2(expense: Expense) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_second_confirm_title)
            .setMessage(R.string.delete_second_confirm_message)
            .setPositiveButton(R.string.delete) { _, _ -> viewModel.deleteExpense(expense) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun navigateEdit(id: String) {
        findNavController().navigate(R.id.addExpenseFragment, bundleOf("expenseId" to id))
    }

    private fun navigateAdd() {
        findNavController().navigate(R.id.addExpenseFragment)
    }

    private fun navigateAI() {
        findNavController().navigate(R.id.aiExpenseFragment)
    }

    private fun getSortOptionText(option: SortOption): String = when (option) {
        SortOption.DATE_DESC -> getString(R.string.expense_list_sort_date_desc)
        SortOption.DATE_ASC -> getString(R.string.expense_list_sort_date_asc)
        SortOption.AMOUNT_DESC -> getString(R.string.expense_list_sort_amount_desc)
        SortOption.AMOUNT_ASC -> getString(R.string.expense_list_sort_amount_asc)
    }
}
