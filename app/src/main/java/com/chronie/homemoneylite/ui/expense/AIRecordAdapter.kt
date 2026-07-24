package com.chronie.homemoneylite.ui.expense

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.databinding.ItemAiRecordBinding
import com.chronie.homemoneylite.domain.model.AIExpenseRecord

class AIRecordAdapter(
    private val onEdit: (AIExpenseRecord) -> Unit,
    private val onDelete: (AIExpenseRecord) -> Unit
) : ListAdapter<AIExpenseRecord, AIRecordAdapter.RecordViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<AIExpenseRecord>() {
            override fun areItemsTheSame(
                oldItem: AIExpenseRecord,
                newItem: AIExpenseRecord
            ): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: AIExpenseRecord,
                newItem: AIExpenseRecord
            ): Boolean = oldItem == newItem
        }
    }

    class RecordViewHolder(
        private val binding: ItemAiRecordBinding,
        private val onEdit: (AIExpenseRecord) -> Unit,
        private val onDelete: (AIExpenseRecord) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var current: AIExpenseRecord? = null

        init {
            binding.editButton.setOnClickListener { current?.let(onEdit) }
            binding.deleteButton.setOnClickListener { current?.let(onDelete) }
        }

        fun bind(record: AIExpenseRecord) {
            current = record
            val context = binding.root.context
            binding.typeText.text =
                ExpenseTypeLocalizer.getLocalizedName(context, record.type)
            binding.amountText.text = context.getString(
                R.string.currency_format,
                context.getString(R.string.currency_symbol),
                record.amount
            )
            binding.dateText.text = record.date

            if (record.remark.isNotBlank()) {
                binding.remarkText.visibility = ViewGroup.VISIBLE
                binding.remarkText.text = record.remark
            } else {
                binding.remarkText.visibility = ViewGroup.GONE
            }
            binding.editedBadge.visibility =
                if (record.isEdited) ViewGroup.VISIBLE else ViewGroup.GONE

            // 无效记录使用浅红背景；有效记录透明（露出卡片默认 colorSurface）
            binding.recordCard.setCardBackgroundColor(
                if (record.isValid) {
                    androidx.core.content.ContextCompat.getColor(
                        context,
                        android.R.color.transparent
                    )
                } else {
                    androidx.core.content.ContextCompat.getColor(
                        context,
                        R.color.ai_record_invalid_bg
                    )
                }
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val binding = ItemAiRecordBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RecordViewHolder(binding, onEdit, onDelete)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
