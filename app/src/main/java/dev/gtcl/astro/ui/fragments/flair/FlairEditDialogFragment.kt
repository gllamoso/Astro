package dev.gtcl.astro.ui.fragments.flair

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import dev.gtcl.astro.FLAIRS_KEY
import dev.gtcl.astro.FLAIR_SELECTED_KEY
import dev.gtcl.astro.R
import dev.gtcl.astro.databinding.FragmentDialogFlairEditBinding
import dev.gtcl.astro.models.reddit.listing.Flair

class FlairEditDialogFragment : DialogFragment(), TextWatcher {

    private var binding: FragmentDialogFlairEditBinding? = null

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
        binding = FragmentDialogFlairEditBinding.inflate(LayoutInflater.from(context))
        val flair = requireArguments().get(FLAIRS_KEY) as Flair
        initFlair(flair)
        initObservers()

        binding?.fragmentDialogFlairEditDialogButtons?.dialogPositiveButton?.setOnClickListener {
            val text = binding?.fragmentDialogFlairEditText?.text.toString()
            if (text.length in 1..64) {
                flair.text = text
                parentFragmentManager.setFragmentResult(
                    FLAIR_SELECTED_KEY,
                    bundleOf(FLAIRS_KEY to flair)
                )
                dismiss()
            } else {
                binding?.fragmentDialogFlairEditTextInputLayout?.error = getString(R.string.invalid)
            }
        }

        binding?.fragmentDialogFlairEditDialogButtons?.dialogNegativeButton?.setOnClickListener {
            dismiss()
        }

        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun initFlair(flair: Flair) {
        binding?.flair = flair
        binding?.executePendingBindings()
    }

    private fun initObservers() {
        binding?.fragmentDialogFlairEditToolbar?.setNavigationOnClickListener {
            dismiss()
        }

        binding?.fragmentDialogFlairEditText?.addTextChangedListener(this)
    }

    override fun afterTextChanged(s: Editable?) {}

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        binding?.fragmentDialogFlairEditTextInputLayout?.error = null
    }

    companion object {
        fun newInstance(flair: Flair): FlairEditDialogFragment {
            val fragment = FlairEditDialogFragment()
            val args = bundleOf(FLAIRS_KEY to flair)
            fragment.arguments = args
            return fragment
        }
    }

}