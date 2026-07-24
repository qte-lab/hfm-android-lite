package com.chronie.homemoneylite.ui.expense

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.domain.model.ExpenseFilters
import com.chronie.homemoneylite.domain.model.ExpenseType
import com.chronie.homemoneylite.domain.model.SortOption
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 支出筛选对话框（传统 Dialog 版本，对应 Compose 的 ExpenseFilterDialog）。
 * “应用”时保留 currentFilters.sortBy（原实现行为）。
 */
class ExpenseFilterDialogFragment : DialogFragment() {

    var onApplyFilters: ((ExpenseFilters) -> Unit)? = null

    private var argKeyword: String? = null
    private var argSortBy: SortOption = SortOption.DATE_DESC

    private lateinit var editKeyword: TextInputEditText
    private lateinit var editMin: TextInputEditText
    private lateinit var editMax: TextInputEditText
    private lateinit var btnSelectType: MaterialButton
    private lateinit var btnStartDate: MaterialButton
    private lateinit var btnEndDate: MaterialButton

    private val selectedTypes = mutableSetOf<ExpenseType>()
    private var startDate: LocalDate? = null
    private var endDate: LocalDate? = null

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            argKeyword = it.getString(ARG_KEYWORD)
            val typeName = it.getString(ARG_TYPE)
            if (!typeName.isNullOrBlank()) {
                runCatching { selectedTypes.add(ExpenseType.valueOf(typeName)) }
            }
            val startEpoch = it.getLong(ARG_START_DATE, -1L)
            val endEpoch = it.getLong(ARG_END_DATE, -1L)
            if (startEpoch >= 0) startDate = LocalDate.ofEpochDay(startEpoch)
            if (endEpoch >= 0) endDate = LocalDate.ofEpochDay(endEpoch)
            argSortBy = runCatching { SortOption.valueOf(it.getString(ARG_SORT, SortOption.DATE_DESC.name)) }
                .getOrDefault(SortOption.DATE_DESC)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val context = requireContext()
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_expense_filter, null)

        editKeyword = view.findViewById(R.id.editKeyword)
        editMin = view.findViewById(R.id.editMinAmount)
        editMax = view.findViewById(R.id.editMaxAmount)
        btnSelectType = view.findViewById(R.id.btnSelectType)
        btnStartDate = view.findViewById(R.id.btnStartDate)
        btnEndDate = view.findViewById(R.id.btnEndDate)

        editKeyword.setText(argKeyword ?: "")
        editMin.setText(arguments?.getDouble(ARG_MIN, -1.0)?.let { if (it < 0) "" else it.toString() } ?: "")
        editMax.setText(arguments?.getDouble(ARG_MAX, -1.0)?.let { if (it < 0) "" else it.toString() } ?: "")

        updateTypeButton()
        updateDateButtons()

        view.findViewById<View>(R.id.btnClose).setOnClickListener { dismiss() }
        btnSelectType.setOnClickListener { showTypeSelector() }
        btnStartDate.setOnClickListener { showDatePicker(true) }
        btnEndDate.setOnClickListener { showDatePicker(false) }

        view.findViewById<View>(R.id.btnClear).setOnClickListener {
            editKeyword.setText("")
            selectedTypes.clear()
            editMin.setText("")
            editMax.setText("")
            startDate = null
            endDate = null
            updateTypeButton()
            updateDateButtons()
        }

        view.findViewById<View>(R.id.btnApply).setOnClickListener {
            val filters = ExpenseFilters(
                keyword = editKeyword.text.toString().ifBlank { null },
                type = selectedTypes.firstOrNull(),
                minAmount = editMin.text.toString().toDoubleOrNull(),
                maxAmount = editMax.text.toString().toDoubleOrNull(),
                startDate = startDate,
                endDate = endDate,
                sortBy = argSortBy
            )
            onApplyFilters?.invoke(filters)
            dismiss()
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setView(view)
            .create()
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun updateTypeButton() {
        btnSelectType.text = if (selectedTypes.isEmpty()) {
            getString(R.string.expense_list_filter_all_types)
        } else {
            getString(R.string.expense_list_filter_select_types) + " (${selectedTypes.size})"
        }
    }

    private fun updateDateButtons() {
        btnStartDate.text = startDate?.format(dateFormatter)
            ?: getString(R.string.expense_list_filter_start_date)
        btnEndDate.text = endDate?.format(dateFormatter)
            ?: getString(R.string.expense_list_filter_end_date)
    }

    private fun showTypeSelector() {
        val context = requireContext()
        val allTypes = ExpenseType.values()
        val names = allTypes.map { ExpenseTypeLocalizer.getLocalizedName(context, it) }.toTypedArray()
        val checked = allTypes.map { selectedTypes.contains(it) }.toBooleanArray()

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.expense_filter_type_selector_title)
            .setMultiChoiceItems(names, checked) { _, which, isChecked ->
                if (isChecked) selectedTypes.add(allTypes[which]) else selectedTypes.remove(allTypes[which])
            }
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.confirm) { _, _ -> updateTypeButton() }
            .show()
    }

    private fun showDatePicker(isStart: Boolean) {
        val context = requireContext()
        val initial = if (isStart) startDate else endDate
        val selection = initial
            ?.atStartOfDay(ZoneId.systemDefault())
            ?.toInstant()
            ?.toEpochMilli()
            ?: MaterialDatePicker.todayInUtcMilliseconds()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(if (isStart) R.string.expense_list_filter_start_date else R.string.expense_list_filter_end_date)
            .setSelection(selection)
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            if (millis == null) return@addOnPositiveButtonClickListener
            val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
            if (isStart) startDate = date else endDate = date
            updateDateButtons()
        }
        picker.show(childFragmentManager, if (isStart) "filter_start" else "filter_end")
    }

    companion object {
        private const val ARG_KEYWORD = "arg_keyword"
        private const val ARG_TYPE = "arg_type"
        private const val ARG_MIN = "arg_min"
        private const val ARG_MAX = "arg_max"
        private const val ARG_START_DATE = "arg_start_date"
        private const val ARG_END_DATE = "arg_end_date"
        private const val ARG_SORT = "arg_sort"

        fun newInstance(currentFilters: ExpenseFilters): ExpenseFilterDialogFragment {
            val fragment = ExpenseFilterDialogFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_KEYWORD, currentFilters.keyword)
                putString(ARG_TYPE, currentFilters.type?.name)
                putDouble(ARG_MIN, currentFilters.minAmount ?: -1.0)
                putDouble(ARG_MAX, currentFilters.maxAmount ?: -1.0)
                putLong(ARG_START_DATE, currentFilters.startDate?.toEpochDay() ?: -1L)
                putLong(ARG_END_DATE, currentFilters.endDate?.toEpochDay() ?: -1L)
                putString(ARG_SORT, currentFilters.sortBy.name)
            }
            return fragment
        }
    }
}
