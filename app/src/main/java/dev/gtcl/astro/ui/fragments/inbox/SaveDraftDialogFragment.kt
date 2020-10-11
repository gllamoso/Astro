package dev.gtcl.astro.ui.fragments.inbox

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import dev.gtcl.astro.*
import dev.gtcl.astro.databinding.FragmentDialogSaveDraftBinding

class SaveDraftDialogFragment : DialogFragment() {
    private var binding: FragmentDialogSaveDraftBinding? = null

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
        binding = FragmentDialogSaveDraftBinding.inflate(inflater)

        binding?.fragmentDialogResubmitDialogButtons?.dialogPositiveButton?.setOnClickListener {
            saveDraft()
            dismiss()
        }

        binding?.fragmentDialogResubmitDialogButtons?.dialogNegativeButton?.setOnClickListener {
            clearSharedPreferenceDraft()
            dismiss()
        }

        return binding?.root
    }

    private fun saveDraft() {
        val arguments = requireArguments()
        val draft = arguments.get(DRAFT_KEY) as Draft
        val sharedPrefs =
            requireContext().getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putString(TO_KEY, draft.to)
            putString(SUBJECT_KEY, draft.subject)
            putString(MESSAGE_KEY, draft.message)
            commit()
        }
    }

    private fun clearSharedPreferenceDraft() {
        val sharedPrefs =
            requireContext().getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            remove(TO_KEY)
            remove(SUBJECT_KEY)
            remove(MESSAGE_KEY)
            commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        fun newInstance(draft: Draft): SaveDraftDialogFragment {
            return SaveDraftDialogFragment().apply {
                arguments = bundleOf(DRAFT_KEY to draft)
            }
        }
    }
}

data class Draft(
    val to: String?,
    val subject: String?,
    val message: String?
)