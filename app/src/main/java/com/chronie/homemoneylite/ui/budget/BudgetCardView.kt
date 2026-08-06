package com.chronie.homemoneylite.ui.budget

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.domain.model.BudgetStatus
import com.chronie.homemoneylite.domain.model.BudgetUsage
import com.chronie.homemoneylite.ui.expense.formatMonthLabelByLocale

/**
 * 预算卡（传统 View 版本），对应 Compose 的 BudgetCard / BudgetUsageCard。
 * 由 ExpenseListFragment 持有并调用 [bind] 渲染 BudgetUiState；
 * 展开/收起仅做 150ms alpha，避免低端机连续重布局卡顿。
 */
class BudgetCardView @JvmOverloads constructor(
    context: Context,
    attrs: android.util.AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {

    var onSettingsRequested: (() -> Unit)? = null

    private val locale: String
        get() = LocaleListCompat.getDefault().get(0)?.toLanguageTag() ?: "zh-CN"

    private val enablePrompt: LinearLayout
    private val usageCard: CardView
    private val btnEnable: Button
    private val btnExpand: ImageButton
    private val btnSettings: ImageButton
    private val collapsedGroup: LinearLayout
    private val expandedGroup: LinearLayout
    private val monthCollapsed: TextView
    private val summaryCollapsed: TextView
    private val monthExpanded: TextView
    private val loading: TextView
    private val detail: LinearLayout
    private val currentSpending: TextView
    private val limitText: TextView
    private val percentText: TextView
    private val progress: ProgressBar
    private val alert: LinearLayout
    private val alertTitle: TextView
    private val alertMessage: TextView
    private val dailyAverage: TextView
    private val recommendedDaily: TextView

    private var isExpanded = false

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_budget_card, this, true)

        enablePrompt = findViewById(R.id.budgetEnablePrompt)
        usageCard = findViewById(R.id.budgetUsageCard)
        btnEnable = findViewById(R.id.btnEnableBudget)
        btnExpand = findViewById(R.id.btnBudgetExpand)
        btnSettings = findViewById(R.id.btnBudgetSettings)
        collapsedGroup = findViewById(R.id.budgetCollapsedGroup)
        expandedGroup = findViewById(R.id.budgetExpandedGroup)
        monthCollapsed = findViewById(R.id.budgetMonthCollapsed)
        summaryCollapsed = findViewById(R.id.budgetSummaryCollapsed)
        monthExpanded = findViewById(R.id.budgetMonthExpanded)
        loading = findViewById(R.id.budgetLoading)
        detail = findViewById(R.id.budgetDetail)
        currentSpending = findViewById(R.id.budgetCurrentSpending)
        limitText = findViewById(R.id.budgetLimit)
        percentText = findViewById(R.id.budgetPercent)
        progress = findViewById(R.id.budgetProgress)
        alert = findViewById(R.id.budgetAlert)
        alertTitle = findViewById(R.id.budgetAlertTitle)
        alertMessage = findViewById(R.id.budgetAlertMessage)
        dailyAverage = findViewById(R.id.budgetDailyAverage)
        recommendedDaily = findViewById(R.id.budgetRecommendedDaily)

        btnEnable.setOnClickListener { onSettingsRequested?.invoke() }
        btnSettings.setOnClickListener { onSettingsRequested?.invoke() }
        btnExpand.setOnClickListener { setExpanded(!isExpanded) }
    }

    @SuppressLint("SetTextI18n")
    fun bind(state: BudgetUiState) {
        val budget = state.budget
        if (budget?.isEnabled != true) {
            enablePrompt.visibility = VISIBLE
            usageCard.visibility = GONE
            return
        }
        enablePrompt.visibility = GONE
        usageCard.visibility = VISIBLE

        val usage = state.budgetUsage
        if (usage == null) {
            loading.visibility = VISIBLE
            detail.visibility = GONE
            collapsedGroup.visibility = GONE
            expandedGroup.visibility = GONE
            return
        }
        loading.visibility = GONE
        collapsedGroup.visibility = if (isExpanded) GONE else VISIBLE
        expandedGroup.visibility = if (isExpanded) VISIBLE else GONE

        val status = when {
            usage.isOverLimit -> BudgetStatus.OVER_LIMIT
            usage.isNearLimit -> BudgetStatus.WARNING
            else -> BudgetStatus.NORMAL
        }

        val progressColor = when (status) {
            BudgetStatus.OVER_LIMIT -> ContextCompat.getColor(context, R.color.app_error)
            BudgetStatus.WARNING -> ContextCompat.getColor(context, R.color.budget_warning)
            BudgetStatus.NORMAL -> ContextCompat.getColor(context, R.color.brand_primary)
        }

        val symbol = context.getString(R.string.currency_symbol)
        val monthLabel = formatMonthLabelByLocale(usage.currentMonth + "-01")
        val pct = usage.spendingPercentage

        monthCollapsed.text = monthLabel
        summaryCollapsed.text = context.getString(
            R.string.currency_format_no_decimal, symbol, usage.currentSpending
        ) + "/" + context.getString(
            R.string.currency_format_no_decimal, symbol, usage.monthlyLimit
        ) + " (" + String.format(java.util.Locale.getDefault(), "%.0f", pct) + "%)"
        summaryCollapsed.setTextColor(progressColor)
        monthExpanded.text = monthLabel

        currentSpending.text = context.getString(
            R.string.currency_format, symbol, usage.currentSpending
        )
        currentSpending.setTextColor(progressColor)
        limitText.text = "/ " + context.getString(
            R.string.currency_format, symbol, usage.monthlyLimit
        )
        percentText.text = "(" + String.format(java.util.Locale.getDefault(), "%.0f", pct) + "%)"
        percentText.setTextColor(progressColor)

        progress.progress = pct.toInt().coerceIn(0, 100)
        progress.progressTintList = android.content.res.ColorStateList.valueOf(progressColor)
        progress.progressBackgroundTintList =
            android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, R.color.divider))

        bindAlert(status, usage)

        dailyAverage.text = context.getString(
            R.string.currency_format, symbol, usage.dailyAverage
        )
        recommendedDaily.text = context.getString(
            R.string.currency_format, symbol, usage.recommendedDaily
        )
        recommendedDaily.setTextColor(
            when {
                usage.recommendedDaily <= 0 -> ContextCompat.getColor(context, R.color.app_error)
                usage.recommendedDaily < usage.dailyAverage * 0.8 ->
                    ContextCompat.getColor(context, R.color.budget_warning)
                else -> ContextCompat.getColor(context, R.color.brand_primary)
            }
        )
    }

    private fun bindAlert(status: BudgetStatus, usage: BudgetUsage) {
        val alertBg = when (status) {
            BudgetStatus.OVER_LIMIT -> R.color.budget_alert_over_bg
            BudgetStatus.WARNING -> R.color.budget_alert_warning_bg
            BudgetStatus.NORMAL -> R.color.budget_alert_normal_bg
        }
        alert.setBackgroundColor(ContextCompat.getColor(context, alertBg))
        alert.visibility = VISIBLE

        val symbol = context.getString(R.string.currency_symbol)
        when (status) {
            BudgetStatus.OVER_LIMIT -> {
                alertTitle.text = context.getString(R.string.budget_alert_over_title)
                alertMessage.text = context.getString(
                    R.string.budget_alert_over_message,
                    context.getString(
                        R.string.currency_format, symbol, usage.currentSpending - usage.monthlyLimit
                    )
                )
            }
            BudgetStatus.WARNING -> {
                alertTitle.text = context.getString(R.string.budget_alert_warning_title)
                alertMessage.text = context.getString(
                    R.string.budget_alert_warning_message,
                    context.getString(R.string.currency_format, symbol, usage.remainingAmount),
                    usage.spendingPercentage
                )
            }
            BudgetStatus.NORMAL -> {
                alertTitle.text = context.getString(R.string.budget_alert_normal_title)
                alertMessage.text = context.getString(
                    R.string.budget_alert_normal_message,
                    context.getString(R.string.currency_format, symbol, usage.remainingAmount),
                    100 - usage.spendingPercentage
                )
            }
        }
    }

    private fun setExpanded(expanded: Boolean) {
        if (isExpanded == expanded) return
        isExpanded = expanded
        if (expanded) {
            detail.visibility = VISIBLE
            detail.alpha = 0f
            detail.animate().alpha(1f).setDuration(150).start()
            collapsedGroup.visibility = GONE
            expandedGroup.visibility = VISIBLE
            btnExpand.setImageResource(R.drawable.ic_exp_chevron_up)
            btnExpand.contentDescription = context.getString(R.string.budget_collapse)
        } else {
            detail.animate().alpha(0f).setDuration(150)
                .withEndAction { detail.visibility = GONE }.start()
            collapsedGroup.visibility = VISIBLE
            expandedGroup.visibility = GONE
            btnExpand.setImageResource(R.drawable.ic_exp_chevron_down)
            btnExpand.contentDescription = context.getString(R.string.budget_expand)
        }
    }
}
