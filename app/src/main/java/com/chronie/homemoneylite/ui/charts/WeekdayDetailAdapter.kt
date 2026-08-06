package com.chronie.homemoneylite.ui.charts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.databinding.ItemChartCategoryDetailBinding
import com.chronie.homemoneylite.ui.expense.ExpenseTypeLocalizer
import java.text.NumberFormat
import java.util.Locale

/**
 * 星期详情页「分类占比」列表适配器（item_chart_category_detail.xml）。
 */
class WeekdayDetailAdapter(
    private val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
) : ListAdapter<CategoryChartData, WeekdayDetailAdapter.ViewHolder>(DIFF) {

    class ViewHolder(val binding: ItemChartCategoryDetailBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChartCategoryDetailBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.itemName.text =
            ExpenseTypeLocalizer.getLocalizedTypeName(holder.itemView.context, item.type)
        holder.binding.itemPct.text = String.format("%.1f%%", item.percentage)
        holder.binding.itemProgress.progress = item.percentage.toInt()
        holder.binding.itemCount.text =
            "${item.count} ${holder.itemView.context.getString(R.string.records)}"
        holder.binding.itemAmount.text = currencyFormat.format(item.amount)
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<CategoryChartData>() {
            override fun areItemsTheSame(
                oldItem: CategoryChartData,
                newItem: CategoryChartData
            ): Boolean = oldItem.type == newItem.type

            override fun areContentsTheSame(
                oldItem: CategoryChartData,
                newItem: CategoryChartData
            ): Boolean = oldItem == newItem
        }
    }
}
