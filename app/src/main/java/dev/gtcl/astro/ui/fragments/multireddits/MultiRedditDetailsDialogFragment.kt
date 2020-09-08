package dev.gtcl.astro.ui.fragments.multireddits

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import dev.gtcl.astro.MULTI_KEY
import dev.gtcl.astro.R
import dev.gtcl.astro.Visibility
import dev.gtcl.astro.databinding.FragmentDialogMultiredditBinding
import dev.gtcl.astro.models.reddit.listing.MultiReddit
import dev.gtcl.astro.models.reddit.listing.MultiRedditUpdate

class MultiRedditDetailsDialogFragment : DialogFragment() {

    private var binding: FragmentDialogMultiredditBinding? = null

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
//        dialog?.window?.setBackgroundDrawableResource(android.R.color.black) // This makes the dialog full screen
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDialogMultiredditBinding.inflate(inflater)
        val multi = requireArguments().get(MULTI_KEY) as MultiReddit?
        binding?.multi = multi
        binding?.fragmentDialogMultiRedditDisplayNameText?.setText(multi?.displayName)
        binding?.fragmentDialogMultiRedditDescriptionText?.setText(multi?.description)
        setSpinner(multi)
        setOnClickListeners()
        binding?.executePendingBindings()
        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun setOnClickListeners() {
        binding?.fragmentDialogMultiRedditToolbar?.setNavigationOnClickListener {
            dismiss()
        }

        binding?.fragmentDialogMultiRedditDialogButtons?.dialogPositiveButton?.setOnClickListener {
            val displayName = binding?.fragmentDialogMultiRedditDisplayNameText?.text.toString()
            if (displayName.isEmpty() || displayName.length > 50) {
                binding?.fragmentDialogMultiRedditDisplayNameText?.error =
                    getString(R.string.invalid)
                return@setOnClickListener
            }
            val description = binding?.fragmentDialogMultiRedditDescriptionText?.text.toString()
            if (description.length > 500) {
                binding?.fragmentDialogMultiRedditDescriptionText?.error =
                    getString(R.string.invalid)
                return@setOnClickListener
            }
            val visibility =
                when (binding?.fragmentDialogMultiRedditVisibilitySpinner?.selectedItemPosition) {
                    0 -> Visibility.PRIVATE
                    1 -> Visibility.PUBLIC
                    2 -> Visibility.HIDDEN
                    else -> null
                }
            setFragmentResult(
                MULTI_KEY,
                bundleOf(
                    MULTI_KEY to MultiRedditUpdate(
                        displayName = displayName,
                        description = description,
                        visibility = visibility
                    )
                )
            )
            dismiss()
        }

        binding?.fragmentDialogMultiRedditDialogButtons?.dialogNegativeButton?.setOnClickListener {
            dismiss()
        }
    }

    private fun setSpinner(multi: MultiReddit?) {
        val strArray = arrayOf(
            resources.getString(R.string.private_label),
            resources.getString(R.string.public_label),
            resources.getString(R.string.hidden)
        )
        val arrayAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, strArray)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding?.fragmentDialogMultiRedditVisibilitySpinner?.adapter = arrayAdapter

        if (multi == null) {
            return
        }

        binding?.fragmentDialogMultiRedditVisibilitySpinner?.setSelection(
            when (multi.visibility) {
                Visibility.PRIVATE -> 0
                Visibility.PUBLIC -> 1
                Visibility.HIDDEN -> 2
            }
        )
    }

    companion object {
        fun newInstance(multi: MultiReddit?): MultiRedditDetailsDialogFragment {
            val arguments = bundleOf(MULTI_KEY to multi)
            return MultiRedditDetailsDialogFragment().apply {
                this.arguments = arguments
            }
        }
    }

}