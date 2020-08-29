package dev.gtcl.reddit.ui.fragments.flair

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import dev.gtcl.reddit.FLAIRS_KEY
import dev.gtcl.reddit.FLAIR_SELECTED_KEY
import dev.gtcl.reddit.R
import dev.gtcl.reddit.databinding.FragmentDialogFlairEditBinding
import dev.gtcl.reddit.models.reddit.listing.Flair

class FlairEditDialogFragment : DialogFragment(), TextWatcher {

    private lateinit var binding: FragmentDialogFlairEditBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context)
            .setPositiveButton(R.string.done, null)
            .setNegativeButton(R.string.cancel){_,_ ->}

        binding = FragmentDialogFlairEditBinding.inflate(LayoutInflater.from(context))
        builder.setView(binding.root)

        return builder.create()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val flair = requireArguments().get(FLAIRS_KEY) as Flair
        initFlair(flair)
        initObservers()

        dialog?.setOnShowListener {
            val button = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val text = binding.fragmentDialogFlairEditText.text.toString()
                if (text.length in 1..64) {
                    flair.text = text
                    parentFragmentManager.setFragmentResult(
                        FLAIR_SELECTED_KEY,
                        bundleOf(FLAIRS_KEY to flair)
                    )
                    dismiss()
                } else {
                    binding.fragmentDialogFlairEditTextInputLayout.error = getString(R.string.invalid)
                }
            }
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    private fun initFlair(flair: Flair) {
        binding.flair = flair
        binding.executePendingBindings()
    }

    private fun initObservers() {
        binding.fragmentDialogFlairEditToolbar.setNavigationOnClickListener {
            dismiss()
        }

        binding.fragmentDialogFlairEditText.addTextChangedListener(this)
    }

    override fun afterTextChanged(s: Editable?) {}

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        binding.fragmentDialogFlairEditTextInputLayout.error = null
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