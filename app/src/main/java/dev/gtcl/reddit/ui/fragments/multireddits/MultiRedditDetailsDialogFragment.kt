package dev.gtcl.reddit.ui.fragments.multireddits

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import dev.gtcl.reddit.MULTI_KEY
import dev.gtcl.reddit.R
import dev.gtcl.reddit.Visibility
import dev.gtcl.reddit.databinding.FragmentDialogMultiredditBinding
import dev.gtcl.reddit.models.reddit.listing.MultiReddit
import dev.gtcl.reddit.models.reddit.listing.MultiRedditUpdate

class MultiRedditDetailsDialogFragment: DialogFragment() {
    private lateinit var binding: FragmentDialogMultiredditBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val multi = requireArguments().get(MULTI_KEY) as MultiReddit?
        binding = FragmentDialogMultiredditBinding.inflate(inflater)
        binding.multi = multi
        setSpinner(multi)
        binding.displayNameInput.setText(multi?.displayName)
        binding.descriptionInput.setText(multi?.description)
        binding.executePendingBindings()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.exitButton.setOnClickListener {
            dismiss()
        }
        binding.finishedButton.setOnClickListener {
            val displayName = binding.displayNameInput.text?.toString()
            if(displayName.isNullOrEmpty()){
                binding.displayNameInput.error = getString(R.string.display_name_error)
                return@setOnClickListener
            }
            val description = binding.descriptionInput.text?.toString()
            val visibility = when(binding.visibilitySpinner.selectedItemPosition){
                0 -> Visibility.PRIVATE
                1 -> Visibility.PUBLIC
                2 -> Visibility.HIDDEN
                else -> null
            }
            setFragmentResult(MULTI_KEY,
                bundleOf(MULTI_KEY to MultiRedditUpdate(displayName = displayName, description = description, visibility = visibility)))
            dismiss()
        }
    }

    private fun setSpinner(multi: MultiReddit?){
        val strArray = arrayOf(resources.getString(R.string.private_label), resources.getString(R.string.public_label), resources.getString(R.string.hidden))
        val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, strArray)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.visibilitySpinner.adapter = arrayAdapter

        if(multi == null){
            return
        }

        binding.visibilitySpinner.setSelection(when(multi.visibility){
            Visibility.PRIVATE -> 0
            Visibility.PUBLIC -> 1
            Visibility.HIDDEN -> 2
        })
    }

    companion object{
        fun newInstance(multi: MultiReddit?): MultiRedditDetailsDialogFragment{
            val arguments = bundleOf(MULTI_KEY to multi)
            return MultiRedditDetailsDialogFragment().apply {
                this.arguments = arguments
            }
        }
    }

}