package dev.gtcl.reddit.ui.fragments.misc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.gtcl.reddit.POST_SORT_KEY
import dev.gtcl.reddit.PostSort
import dev.gtcl.reddit.TIME_KEY
import dev.gtcl.reddit.Time
import dev.gtcl.reddit.actions.SortActions
import dev.gtcl.reddit.databinding.FragmentDialogTimePeriodBinding

class TimeDialogFragment : BottomSheetDialogFragment() {

    private var sortActions: SortActions? = null

    fun setActions(sortActions: SortActions?){
        this.sortActions = sortActions
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentDialogTimePeriodBinding.inflate(inflater)

        val arguments = requireArguments()
        val sort = arguments.get(POST_SORT_KEY) as PostSort
        val time = arguments.get(TIME_KEY) as Time?

        binding.time = time

        binding.hourItem.root.setOnClickListener {
            sortActions?.sortSelected(sort, Time.HOUR)
            dismiss()
        }

        binding.dayItem.root.setOnClickListener {
            sortActions?.sortSelected(sort, Time.DAY)
            dismiss()
        }

        binding.weekItem.root.setOnClickListener {
            sortActions?.sortSelected(sort, Time.WEEK)
            dismiss()
        }

        binding.monthItem.root.setOnClickListener {
            sortActions?.sortSelected(sort, Time.MONTH)
            dismiss()
        }

        binding.yearItem.root.setOnClickListener {
            sortActions?.sortSelected(sort, Time.YEAR)
            dismiss()
        }

        binding.allItem.root.setOnClickListener {
            sortActions?.sortSelected(sort, Time.ALL)
            dismiss()
        }

        binding.executePendingBindings()
        return binding.root
    }

    companion object {
        fun newInstance(sort: PostSort, time: Time?): TimeDialogFragment{
            val fragment = TimeDialogFragment()
            val args = bundleOf(POST_SORT_KEY to sort, TIME_KEY to time)
            fragment.arguments = args
            return fragment
        }
    }
}