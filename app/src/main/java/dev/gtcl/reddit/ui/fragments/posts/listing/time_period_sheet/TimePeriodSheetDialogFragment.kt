package dev.gtcl.reddit.ui.fragments.posts.listing.time_period_sheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.gtcl.reddit.Time
import dev.gtcl.reddit.databinding.FragmentDialogTimePeriodBinding

class TimePeriodSheetDialogFragment(private val onItemSelectedCallback: (Time) -> Unit) : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentDialogTimePeriodBinding.inflate(inflater)
        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            onItemSelectedCallback(when(checkedId){
                binding.hourRadioButton.id -> Time.HOUR
                binding.dayRadioButton.id -> Time.DAY
                binding.weekRadioButton.id -> Time.WEEK
                binding.monthRadioButton.id -> Time.MONTH
                binding.yearRadioButton.id -> Time.YEAR
                binding.allRadioButton.id -> Time.ALL
                else -> Time.WEEK
            })
            dismiss()
        }
        return binding.root
    }

    companion object {
        val TAG = TimePeriodSheetDialogFragment::class.qualifiedName
    }
}