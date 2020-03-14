package dev.gtcl.reddit.ui.fragments.posts.time_period_sheet

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
                binding.hourRadioButton.id -> Time.hour
                binding.dayRadioButton.id -> Time.day
                binding.weekRadioButton.id -> Time.week
                binding.monthRadioButton.id -> Time.month
                binding.yearRadioButton.id -> Time.year
                binding.allRadioButton.id -> Time.all
                else -> Time.week
            })
            dismiss()
        }
        return binding.root
    }

    companion object {
        val TAG = TimePeriodSheetDialogFragment::class.qualifiedName
    }
}