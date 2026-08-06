package com.chronie.homemoneylite.ui.expense

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.LocaleListCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.databinding.ItemExpenseBinding
import com.chronie.homemoneylite.databinding.ItemExpenseDateHeaderBinding
import com.chronie.homemoneylite.databinding.ItemExpenseLoadMoreBinding
import com.chronie.homemoneylite.domain.model.Expense

/**
 * 支出列表 Adapter（对应 Compose 的按日期分组列表）。
 * 多 viewType：日期组头 / 明细行 / 加载更多。使用 ListAdapter + DiffUtil + 稳定 id。
 */
class ExpenseListAdapter(
    private val context: Context
) : ListAdapter<ExpenseListAdapter.ListItem, RecyclerView.ViewHolder>(DIFF) {

    var onItemClick: ((Expense) -> Unit)? = null
    var onLoadMoreClick: (() -> Unit)? = null

    private var isLoadMoreLoading = false

    private val locale: String
        get() = LocaleListCompat.getDefault().get(0)?.toLanguageTag() ?: "zh-CN"

    sealed class ListItem {
        data class DateHeader(
            val date: String,
            val count: Int,
            val totalAmount: Double
        ) : ListItem()

        data class ExpenseItem(val expense: Expense) : ListItem()
        object LoadMore : ListItem()
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_EXPENSE = 1
        private const val TYPE_LOAD_MORE = 2

        private val DIFF = object : DiffUtil.ItemCallback<ListItem>() {
            override fun areItemsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
                return when (oldItem) {
                    is ListItem.DateHeader if newItem is ListItem.DateHeader ->
                        oldItem.date == newItem.date

                    is ListItem.ExpenseItem if newItem is ListItem.ExpenseItem ->
                        oldItem.expense.id == newItem.expense.id

                    is ListItem.LoadMore if newItem is ListItem.LoadMore -> true
                    else -> false
                }
            }

            override fun areContentsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
                return when (oldItem) {
                    is ListItem.DateHeader if newItem is ListItem.DateHeader ->
                        oldItem.count == newItem.count && oldItem.totalAmount == newItem.totalAmount

                    is ListItem.ExpenseItem if newItem is ListItem.ExpenseItem -> {
                        val a = oldItem.expense
                        val b = newItem.expense
                        a.id == b.id && a.amount == b.amount && a.date == b.date &&
                                a.type == b.type && a.remark == b.remark
                    }

                    else -> true
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ListItem.DateHeader -> TYPE_HEADER
            is ListItem.ExpenseItem -> TYPE_EXPENSE
            is ListItem.LoadMore -> TYPE_LOAD_MORE
        }
    }

    override fun getItemId(position: Int): Long {
        return when (val item = getItem(position)) {
            is ListItem.DateHeader -> ("h:${item.date}").hashCode().toLong()
            is ListItem.ExpenseItem -> ("e:${item.expense.id}").hashCode().toLong()
            is ListItem.LoadMore -> "load_more".hashCode().toLong()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(
                ItemExpenseDateHeaderBinding.inflate(inflater, parent, false)
            )
            TYPE_LOAD_MORE -> LoadMoreViewHolder(
                ItemExpenseLoadMoreBinding.inflate(inflater, parent, false)
            )
            else -> ExpenseViewHolder(
                ItemExpenseBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ListItem.DateHeader -> (holder as HeaderViewHolder).bind(item)
            is ListItem.ExpenseItem -> (holder as ExpenseViewHolder).bind(item.expense)
            is ListItem.LoadMore -> (holder as LoadMoreViewHolder).bindLoading(isLoadMoreLoading)
        }
    }

    private fun currency(value: Double): String {
        return context.getString(
            R.string.currency_format,
            context.getString(R.string.currency_symbol),
            value
        )
    }

    inner class HeaderViewHolder(
        private val binding: ItemExpenseDateHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ListItem.DateHeader) {
            binding.headerDate.text = formatRelativeDate(item.date, context, locale)
            binding.headerCount.text =
                context.getString(R.string.expense_stats_count) + ": " + item.count
            binding.headerTotal.text = "-" + currency(item.totalAmount)
        }
    }

    inner class ExpenseViewHolder(
        private val binding: ItemExpenseBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(expense: Expense) {
            binding.expenseType.text = ExpenseTypeLocalizer.getLocalizedName(context, expense.type)
            if (!expense.remark.isNullOrBlank()) {
                binding.expenseRemark.text = expense.remark
                binding.expenseRemark.visibility = android.view.View.VISIBLE
            } else {
                binding.expenseRemark.visibility = android.view.View.GONE
            }
            binding.expenseDate.text = formatDateByLocale(expense.date)
            binding.expenseAmount.text = "-" + currency(expense.amount)

            binding.root.setOnClickListener { onItemClick?.invoke(expense) }
        }
    }

    inner class LoadMoreViewHolder(
        private val binding: ItemExpenseLoadMoreBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.loadMoreButton.setOnClickListener { onLoadMoreClick?.invoke() }
        }

        fun bindLoading(isLoading: Boolean) {
            binding.loadMoreProgress.visibility =
                if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
            binding.loadMoreButton.visibility =
                if (isLoading) android.view.View.GONE else android.view.View.VISIBLE
        }
    }

    fun bindLoadMoreState(isLoading: Boolean) {
        if (isLoadMoreLoading == isLoading) return
        isLoadMoreLoading = isLoading
        val last = itemCount - 1
        if (last >= 0 && getItem(last) is ListItem.LoadMore) {
            // onBindViewHolder 会按 isLoadMoreLoading 重新绑定 LoadMore 项
            notifyItemChanged(last)
        }
    }
}
