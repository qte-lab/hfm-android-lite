package com.chronie.homemoneylite.ui.expense

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import com.chronie.homemoneylite.domain.model.ExpenseType

/**
 * 支出分类下拉适配器：支持按本地化名称或枚举名搜索过滤。
 * 等价于 Compose 版 ExpenseTypeDropdown 的搜索功能。
 */
class ExpenseTypeAdapter(
    private val context: Context,
    private val allTypes: List<ExpenseType>
) : BaseAdapter(), Filterable {

    private var filtered: List<ExpenseType> = allTypes

    override fun getCount(): Int = filtered.size
    override fun getItem(position: Int): ExpenseType = filtered[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView
            ?: LayoutInflater.from(context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
        (view as TextView).text =
            ExpenseTypeLocalizer.getLocalizedName(context, getItem(position))
        return view
    }

    override fun getFilter(): Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val query = constraint?.toString()?.trim().orEmpty()
            val result = if (query.isBlank()) {
                allTypes
            } else {
                allTypes.filter { type ->
                    ExpenseTypeLocalizer.getLocalizedName(context, type)
                        .contains(query, ignoreCase = true) ||
                        type.name.contains(query, ignoreCase = true)
                }
            }
            return FilterResults().apply {
                values = result
                count = result.size
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            filtered = (results.values as? List<ExpenseType>) ?: emptyList()
            notifyDataSetChanged()
        }
    }
}
