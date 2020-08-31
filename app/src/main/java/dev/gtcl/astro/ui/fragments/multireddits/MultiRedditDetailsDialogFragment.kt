package dev.gtcl.astro.ui.fragments.multireddits

import android.app.AlertDialog
import android.app.Dialog
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

class MultiRedditDetailsDialogFragment: DialogFragment() {
    private lateinit var binding: FragmentDialogMultiredditBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context)
            .setPositiveButton(R.string.done, null)
            .setNegativeButton(R.string.cancel){_,_ ->}

        binding = FragmentDialogMultiredditBinding.inflate(LayoutInflater.from(context))
        builder.setView(binding.root)

        return builder.create()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val multi = requireArguments().get(MULTI_KEY) as MultiReddit?
        binding.multi = multi
        binding.fragmentDialogMultiRedditDisplayNameText.setText(multi?.displayName)
        binding.fragmentDialogMultiRedditDescriptionText.setText(multi?.description)
        setSpinner(multi)
        setOnClickListeners()
        binding.executePendingBindings()
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    private fun setOnClickListeners() {
        binding.fragmentDialogMultiRedditToolbar.setNavigationOnClickListener {
            dismiss()
        }

        dialog?.setOnShowListener {
            val button = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val displayName = binding.fragmentDialogMultiRedditDisplayNameText.text?.toString()
                if(displayName.isNullOrEmpty() || displayName.length > 50){
                    binding.fragmentDialogMultiRedditDisplayNameText.error = getString(R.string.invalid)
                    return@setOnClickListener
                }
                val description = binding.fragmentDialogMultiRedditDescriptionText.text?.toString()
                if(description?.length ?: 0 > 500){
                    binding.fragmentDialogMultiRedditDescriptionText.error = getString(R.string.invalid)
                    return@setOnClickListener
                }
                val visibility = when(binding.fragmentDialogMultiRedditVisibilitySpinner.selectedItemPosition){
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
    }

    private fun setSpinner(multi: MultiReddit?){
        val strArray = arrayOf(resources.getString(R.string.private_label), resources.getString(R.string.public_label), resources.getString(R.string.hidden))
        val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, strArray)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.fragmentDialogMultiRedditVisibilitySpinner.adapter = arrayAdapter

        if(multi == null){
            return
        }

        binding.fragmentDialogMultiRedditVisibilitySpinner.setSelection(when(multi.visibility){
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