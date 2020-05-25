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
import dev.gtcl.reddit.databinding.FragmentDialogSortSheetBinding

class SortDialogFragment: BottomSheetDialogFragment(){

    private var sortActions: SortActions? = null

    fun setActions(sortActions: SortActions?){
        this.sortActions = sortActions
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentDialogSortSheetBinding.inflate(inflater)
        val sort = requireArguments().get(POST_SORT_KEY) as PostSort
        val time = requireArguments().get(TIME_KEY) as Time?
        binding.sort = sort

        binding.bestItem.root.setOnClickListener {
            sortActions?.sortSelected(PostSort.BEST, null)
            dismiss()
        }

        binding.hotItem.root.setOnClickListener {
            sortActions?.sortSelected(PostSort.HOT, null)
            dismiss()
        }

        binding.newItem.root.setOnClickListener {
            sortActions?.sortSelected(PostSort.NEW, null)
            dismiss()
        }

        binding.topItem.root.setOnClickListener {
            val timeSelected = if(sort == PostSort.TOP) {
                time
            } else {
                null
            }
            TimeDialogFragment.newInstance(PostSort.TOP, timeSelected).show(parentFragmentManager, null)
            dismiss()
        }

        binding.controversialItem.root.setOnClickListener {
            val timeSelected = if(sort == PostSort.CONTROVERSIAL) {
                time
            } else {
                null
            }
            TimeDialogFragment.newInstance(PostSort.CONTROVERSIAL, timeSelected).show(parentFragmentManager, null)
            dismiss()
        }

        binding.risingItem.root.setOnClickListener {
            sortActions?.sortSelected(PostSort.RISING, null)
            dismiss()
        }

        binding.executePendingBindings()
        return binding.root
    }

    companion object {
        fun newInstance(sort: PostSort, time: Time?): SortDialogFragment{
            val fragment = SortDialogFragment()
            val args = bundleOf(POST_SORT_KEY to sort, TIME_KEY to time)
            fragment.arguments = args
            return fragment
        }
    }
}