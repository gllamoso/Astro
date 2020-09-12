package dev.gtcl.astro.ui.fragments.create_post.resubmit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import dev.gtcl.astro.URL_KEY
import dev.gtcl.astro.databinding.FragmentDialogResubmitBinding

class ResubmitDialogFragment : DialogFragment() {

    private var binding: FragmentDialogResubmitBinding? = null

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
        binding = FragmentDialogResubmitBinding.inflate(inflater)

        binding?.fragmentDialogResubmitDialogButtons?.dialogPositiveButton?.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                URL_KEY,
                bundleOf(URL_KEY to requireArguments().getString(URL_KEY))
            )
            dismiss()
        }

        binding?.fragmentDialogResubmitDialogButtons?.dialogNegativeButton?.setOnClickListener {
            dismiss()
        }

        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        fun newInstance(url: String): ResubmitDialogFragment {
            return ResubmitDialogFragment().apply {
                arguments = bundleOf(URL_KEY to url)
            }
        }
    }
}