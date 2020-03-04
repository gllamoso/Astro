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
            PostSort.HOT -> binding.hotRadiobutton.id
            PostSort.NEW -> binding.newRadiobutton.id
            PostSort.TOP -> binding.topRadiobutton.id
            PostSort.CONTROVERSIAL -> binding.controversialRadiobutton.id
            PostSort.RISING -> binding.risingRadiobutton.id
        })

        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            onItemSelected(when(checkedId){
                binding.hotRadiobutton.id -> PostSort.HOT
                binding.newRadiobutton.id -> PostSort.NEW
                binding.topRadiobutton.id -> PostSort.TOP
                binding.controversialRadiobutton.id -> PostSort.CONTROVERSIAL
                binding.risingRadiobutton.id -> PostSort.RISING
                else -> PostSort.HOT
            })
            dismiss()
        }

        return binding.root
    }

    companion object {
        val TAG = SortSheetDialogFragment::class.qualifiedName
    }
}