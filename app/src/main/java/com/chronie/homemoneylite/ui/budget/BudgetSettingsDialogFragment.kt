package com.chronie.homemoneylite.ui.budget

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.app.AlertDialog
import android.widget.EditText
import android.widget.Switch
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.domain.model.Budget

/**
 * 预算设置对话框（传统 Dialog 版本，对应 Compose 的 BudgetSettingsDialog）。
 * 校验逻辑与原实现一致：启用时 limit>0、threshold 在 0-100。
 */
class BudgetSettingsDialogFragment : DialogFragment() {

    var onSave: ((monthlyLimit: Double, warningThreshold: Double, isEnabled: Boolean) -> Unit)? = null

    private var initialLimit: Double = 0.0
    private var initialThreshold: Double = 0.8
    private var initialEnabled: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            initialLimit = it.getDouble(ARG_LIMIT, 0.0)
            initialThreshold = it.getDouble(ARG_THRESHOLD, 0.8)
            initialEnabled = it.getBoolean(ARG_ENABLED, false)
        }
    }

    @SuppressLint("UseGetLayoutInflater", "UseSwitchCompatOrMaterialCode")
    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val context = requireContext()
        // 强制用 AppTheme（原生 Holo 属性）inflate 对话框视图，
        // 避免 AlertDialog.Builder 套用框架默认对话框主题导致 ?android:attr/ 引用解析异常。
        val view = LayoutInflater.from(android.view.ContextThemeWrapper(context, R.style.AppTheme_NoActionBar))
            .inflate(R.layout.dialog_budget_settings, null)

        val switchEnabled = view.findViewById<Switch>(R.id.switchEnabled)
        val editLimit = view.findViewById<EditText>(R.id.editMonthlyLimit)
        val editThreshold = view.findViewById<EditText>(R.id.editWarningThreshold)
        val btnLimitMinus = view.findViewById<ImageButton>(R.id.btnLimitMinus)
        val btnLimitPlus = view.findViewById<ImageButton>(R.id.btnLimitPlus)
        val btnThresholdMinus = view.findViewById<ImageButton>(R.id.btnThresholdMinus)
        val btnThresholdPlus = view.findViewById<ImageButton>(R.id.btnThresholdPlus)
        val textError = view.findViewById<TextView>(R.id.textError)

        switchEnabled.isChecked = initialEnabled
        editLimit.setText(initialLimit.toString())
        editThreshold.setText((initialThreshold * 100).toString())

        fun syncEnabled() {
            val enabled = switchEnabled.isChecked
            editLimit.isEnabled = enabled
            editThreshold.isEnabled = enabled
            btnLimitMinus.isEnabled = enabled
            btnLimitPlus.isEnabled = enabled
            btnThresholdMinus.isEnabled = enabled
            btnThresholdPlus.isEnabled = enabled
        }
        syncEnabled()
        switchEnabled.setOnCheckedChangeListener { _, _ -> syncEnabled() }

        fun adjust(edit: EditText, delta: Double, min: Double, max: Double, curr: Double) {
            val next = (curr + delta).coerceIn(min, max)
            edit.setText(next.toString())
            textError.visibility = View.GONE
        }

        btnLimitMinus.setOnClickListener {
            adjust(editLimit, -1000.0, 0.0, Double.MAX_VALUE,
                editLimit.text.toString().toDoubleOrNull() ?: 0.0)
        }
        btnLimitPlus.setOnClickListener {
            adjust(editLimit, 1000.0, 0.0, Double.MAX_VALUE,
                editLimit.text.toString().toDoubleOrNull() ?: 0.0)
        }
        btnThresholdMinus.setOnClickListener {
            adjust(editThreshold, -10.0, 0.0, 100.0,
                editThreshold.text.toString().toDoubleOrNull() ?: 80.0)
        }
        btnThresholdPlus.setOnClickListener {
            adjust(editThreshold, 10.0, 0.0, 100.0,
                editThreshold.text.toString().toDoubleOrNull() ?: 80.0)
        }

        editLimit.doAfterTextChanged { textError.visibility = View.GONE }
        editThreshold.doAfterTextChanged { textError.visibility = View.GONE }

        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.budget_settings_title)
            .setView(view)
            .setPositiveButton(R.string.common_save, null)
            .setNegativeButton(R.string.common_cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val limit = editLimit.text.toString().toDoubleOrNull()
                val threshold = editThreshold.text.toString().toDoubleOrNull()
                val enabled = switchEnabled.isChecked
                when {
                    enabled && (limit == null || limit <= 0) -> {
                        textError.text = context.getString(R.string.budget_error_invalid_limit)
                        textError.visibility = View.VISIBLE
                    }
                    enabled && (threshold == null || threshold < 0 || threshold > 100) -> {
                        textError.text = context.getString(R.string.budget_error_invalid_threshold)
                        textError.visibility = View.VISIBLE
                    }
                    else -> {
                        onSave?.invoke(limit ?: 0.0, (threshold ?: 80.0) / 100, enabled)
                        dialog.dismiss()
                    }
                }
            }
        }

        return dialog
    }

    companion object {
        private const val ARG_LIMIT = "arg_limit"
        private const val ARG_THRESHOLD = "arg_threshold"
        private const val ARG_ENABLED = "arg_enabled"

        fun newInstance(budget: Budget?): BudgetSettingsDialogFragment {
            val fragment = BudgetSettingsDialogFragment()
            fragment.arguments = Bundle().apply {
                putDouble(ARG_LIMIT, budget?.monthlyLimit ?: 0.0)
                putDouble(ARG_THRESHOLD, budget?.warningThreshold ?: 0.8)
                putBoolean(ARG_ENABLED, budget?.isEnabled ?: false)
            }
            return fragment
        }
    }
}
