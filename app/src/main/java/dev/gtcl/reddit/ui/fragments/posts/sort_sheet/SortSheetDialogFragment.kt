package dev.gtcl.reddit.ui.fragments.posts.sort_sheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

import dev.gtcl.reddit.PostSort
import dev.gtcl.reddit.databinding.FragmentDialogSortSheetBinding

// TODO: Remove arguments from constructor
class SortSheetDialogFragment(private val selectedValue: PostSort, private val onItemSelected: (PostSort) -> Unit): BottomSheetDialogFragment(){

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentDialogSortSheetBinding.inflate(inflater)
        binding.radioGroup.check(when(selectedValue){
            PostSort.best -> binding.bestRadiobutton.id
            PostSort.hot -> binding.hotRadiobutton.id
            PostSort.new -> binding.newRadiobutton.id
            PostSort.top -> binding.topRadiobutton.id
            PostSort.controversial -> binding.controversialRadiobutton.id
            PostSort.rising -> binding.risingRadiobutton.id
        })

        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            onItemSelected(when(checkedId){
                binding.bestRadiobutton.id -> PostSort.best
                binding.hotRadiobutton.id -> PostSort.hot
                binding.newRadiobutton.id -> PostSort.new
                binding.topRadiobutton.id -> PostSort.top
                binding.controversialRadiobutton.id -> PostSort.controversial
                binding.risingRadiobutton.id -> PostSort.rising
                else -> PostSort.hot
            })
            dismiss()
        }

        return binding.root
    }

    companion object {
        val TAG = SortSheetDialogFragment::class.qualifiedName
    }
}