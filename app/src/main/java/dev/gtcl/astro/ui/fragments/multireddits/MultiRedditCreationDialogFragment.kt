package dev.gtcl.astro.ui.fragments.multireddits

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import dev.gtcl.astro.*
import dev.gtcl.astro.databinding.FragmentDialogMultiredditBinding
import dev.gtcl.astro.models.reddit.listing.MultiReddit
import dev.gtcl.astro.models.reddit.listing.MultiRedditUpdate
import dev.gtcl.astro.ui.activities.MainActivityVM

class MultiRedditCreationDialogFragment : DialogFragment() {

    private var binding: FragmentDialogMultiredditBinding? = null

    private val model: MultiRedditCreationVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as AstroApplication)
        ViewModelProvider(this, viewModelFactory).get(MultiRedditCreationVM::class.java)
    }

    private val activityModel: MainActivityVM by activityViewModels()

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
        binding?.model = model
        val multi = requireArguments().get(MULTI_KEY) as MultiReddit?
        val isCopying = requireArguments().getBoolean(COPY_KEY, false)
        model.setModel(multi, isCopying)
        binding?.apply {
            fragmentDialogMultiRedditDisplayNameText.setText(multi?.displayName)
            fragmentDialogMultiRedditDescriptionText.setText(multi?.description)
        }
        if (!isCopying) {
            setSpinner(multi)
        }
        setOnClickListeners(multi, isCopying)
        binding?.executePendingBindings()
        initObservers()
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun setOnClickListeners(multi: MultiReddit?, isCopying: Boolean) {
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
//            setFragmentResult(
//                MULTI_KEY,
//                bundleOf(
//                    MULTI_KEY to MultiRedditUpdate(
//                        displayName = displayName,
//                        description = description,
//                        visibility = visibility
//                    )
//                )
//            )
            val update = MultiRedditUpdate(
                displayName = displayName,
                description = description,
                visibility = visibility
            )
            when {
                multi == null -> {
                    model.createMulti(update)
                }
                isCopying -> {
                    model.copyMulti(
                        multi.pathFormatted,
                        displayName,
                        description
                    )
                }
                else -> {
                    model.updateMultiReddit(multi.pathFormatted, update)
                }
            }
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

    private fun initObservers(){
        model.newMulti.observe(viewLifecycleOwner, {
            if (it != null) {
                activityModel.newMultiReddit(it)
                model.newMultiObserved()
                dismiss()
            }
        })

        model.errorMessage.observe(viewLifecycleOwner, {
            if(it != null){
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                model.errorMessageObserved()
            }
        })
    }

    companion object {
        fun newInstance(
            multi: MultiReddit?,
            isCopying: Boolean = multi != null
        ): MultiRedditCreationDialogFragment {
            return MultiRedditCreationDialogFragment().apply {
                this.arguments = bundleOf(MULTI_KEY to multi, COPY_KEY to isCopying)
            }
        }
    }

}