package com.chronie.homemoneylite.ui.charts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.databinding.FragmentWeekdayDetailBinding
import com.chronie.homemoneylite.ui.common.collectWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class WeekdayDetailFragment : Fragment() {

    private var _binding: FragmentWeekdayDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WeekdayDetailViewModel by viewModels()

    private val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
    private val adapter = WeekdayDetailAdapter(currencyFormat)

    // 说明：导航传入的 arguments 会由默认的 SavedStateViewModelFactory 自动填充进
    // WeekdayDetailViewModel 的 SavedStateHandle（dayOfWeek/startDate/endDate），无需手动写入。

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWeekdayDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dayOfWeek = requireArguments().getInt("dayOfWeek", 0)
        binding.titleText.text = getWeekdayName(requireContext(), dayOfWeek)
        binding.subtitleText.setText(R.string.expense_details)

        binding.headerTotal.text = currencyFormat.format(
            requireArguments().getFloat("amount", 0f).toDouble()
        )
        binding.headerCount.text = "${requireArguments().getInt("count", 0)} ${getString(R.string.records)}"
        binding.headerPct.text = String.format("%.1f%%", requireArguments().getFloat("percentage", 0f))

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.detailRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.detailRecyclerView.adapter = adapter

        collectWithLifecycle(viewModel.uiState) { state -> renderState(state) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun renderState(state: WeekdayDetailUiState) {
        when (state) {
            is WeekdayDetailUiState.Loading -> {
                binding.contentGroup.visibility = View.GONE
                binding.errorText.visibility = View.GONE
                binding.progressBar.visibility = View.VISIBLE
            }
            is WeekdayDetailUiState.Error -> {
                binding.progressBar.visibility = View.GONE
                binding.contentGroup.visibility = View.GONE
                binding.errorText.visibility = View.VISIBLE
                binding.errorText.text = state.message
            }
            is WeekdayDetailUiState.Success -> {
                binding.progressBar.visibility = View.GONE
                binding.errorText.visibility = View.GONE
                binding.contentGroup.visibility = View.VISIBLE
                if (state.categoryBreakdown.isEmpty()) {
                    binding.breakdownTitle.visibility = View.GONE
                    binding.detailRecyclerView.visibility = View.GONE
                    binding.noDataText.visibility = View.VISIBLE
                } else {
                    binding.noDataText.visibility = View.GONE
                    binding.breakdownTitle.visibility = View.VISIBLE
                    binding.detailRecyclerView.visibility = View.VISIBLE
                    adapter.submitList(state.categoryBreakdown)
                }
            }
        }
    }

    private fun getWeekdayName(context: android.content.Context, dayOfWeek: Int): String {
        return when (dayOfWeek) {
            0 -> context.getString(R.string.sunday)
            1 -> context.getString(R.string.monday)
            2 -> context.getString(R.string.tuesday)
            3 -> context.getString(R.string.wednesday)
            4 -> context.getString(R.string.thursday)
            5 -> context.getString(R.string.friday)
            6 -> context.getString(R.string.saturday)
            else -> ""
        }
    }
}
