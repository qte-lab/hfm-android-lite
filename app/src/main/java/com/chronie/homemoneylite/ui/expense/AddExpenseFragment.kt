package com.chronie.homemoneylite.ui.expense

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.databinding.FragmentAddExpenseBinding
import com.chronie.homemoneylite.domain.model.ExpenseType
import com.chronie.homemoneylite.ui.common.collectWithLifecycle
import com.chronie.homemoneylite.ui.components.showWheelDatePicker
import android.widget.Toast
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class AddExpenseFragment : Fragment() {

    private var _binding: FragmentAddExpenseBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddExpenseViewModel by viewModels()

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private var lastShownSaveError: String? = null

    private val expenseId: String?
        get() = arguments?.getString("expenseId")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isEdit = expenseId != null
        binding.toolbar.title = getString(
            if (isEdit) R.string.edit_expense_title else R.string.add_expense_title
        )
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.toolbar.inflateMenu(R.menu.menu_add_expense)
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_save) {
                onSave()
                true
            } else {
                false
            }
        }

        setupTypeInput()
        setupTextInputs()
        binding.dateCard.setOnClickListener { showDatePicker() }

        if (isEdit) {
            viewModel.loadExpenseForEdit(expenseId!!)
        }

        collectWithLifecycle(viewModel.uiState) { state ->
            // 分类
            val typeName = state.selectedType?.let {
                ExpenseTypeLocalizer.getLocalizedName(requireContext(), it)
            }
            if (typeName != null && binding.typeInput.text.toString() != typeName) {
                binding.typeInput.setText(typeName)
            }

            // 金额
            if (binding.amountInput.text.toString() != state.amount) {
                binding.amountInput.setText(state.amount)
            }

            // 日期
            binding.dateText.text = state.selectedDate.format(dateFormatter)
            binding.dateError.visibility =
                if (state.dateError != null) View.VISIBLE else View.GONE
            if (state.dateError != null) {
                binding.dateError.setText(R.string.add_expense_validation_date_required)
            }

            // 备注
            if (binding.remarkInput.text.toString() != state.remark) {
                binding.remarkInput.setText(state.remark)
            }

            // 保存中：禁用保存按钮
            binding.toolbar.menu.findItem(R.id.action_save)?.isEnabled = !state.isSaving

            // 保存错误：仅展示一次
            if (state.saveError != null && state.saveError != lastShownSaveError) {
                lastShownSaveError = state.saveError
                Toast.makeText(
                    requireContext(),
                    getString(R.string.add_expense_save_failed, state.saveError),
                    Toast.LENGTH_LONG
                ).show()
            } else if (state.saveError == null) {
                lastShownSaveError = null
            }
        }
    }

    private fun setupTypeInput() {
        val adapter = ExpenseTypeAdapter(requireContext(), ExpenseType.values().toList())
        binding.typeInput.setAdapter(adapter)
        binding.typeInput.threshold = 1
        binding.typeInput.setOnClickListener { binding.typeInput.showDropDown() }
        binding.typeInput.setOnItemClickListener { _, _, position, _ ->
            val type = adapter.getItem(position)
            val name = ExpenseTypeLocalizer.getLocalizedName(requireContext(), type)
            viewModel.setType(type)
            // AutoCompleteTextView 默认会用 convertToString(enum) 覆盖文本，
            // 这里在事件后修正为本地化名称，避免闪烁显示枚举名。
            binding.typeInput.post { binding.typeInput.setText(name, false) }
        }
    }

    private fun setupTextInputs() {
        binding.amountInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.setAmount(s?.toString().orEmpty())
            }
        })
        binding.remarkInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.setRemark(s?.toString().orEmpty())
            }
        })
    }

    private fun showDatePicker() {
        val initial = try {
            LocalDate.parse(binding.dateText.text.toString(), dateFormatter)
        } catch (e: Exception) {
            LocalDate.now()
        }
        showWheelDatePicker(
            requireContext(),
            initial = initial,
            minDate = LocalDate.of(2000, 1, 1),
            maxDate = LocalDate.now()
        ) { date ->
            viewModel.setDate(date)
        }
    }

    private fun onSave() {
        viewModel.saveExpense(
            onSuccess = { findNavController().popBackStack() },
            onError = { /* 错误通过 Toast 展示 */ }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
