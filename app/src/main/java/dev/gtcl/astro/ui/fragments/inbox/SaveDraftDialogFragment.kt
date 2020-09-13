package dev.gtcl.astro.ui.fragments.inbox

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import dev.gtcl.astro.MESSAGE_KEY
import dev.gtcl.astro.PREFERENCES_KEY
import dev.gtcl.astro.SUBJECT_KEY
import dev.gtcl.astro.TO_KEY
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
        val to = arguments.getString(TO_KEY)
        val subject = arguments.getString(SUBJECT_KEY)
        val message = arguments.getString(MESSAGE_KEY)
        val sharedPrefs =
            requireContext().getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putString(TO_KEY, to)
            putString(SUBJECT_KEY, subject)
            putString(MESSAGE_KEY, message)
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
        fun newInstance(to: String?, subject: String?, message: String?): SaveDraftDialogFragment {
            return SaveDraftDialogFragment().apply {
                arguments = bundleOf(TO_KEY to to, SUBJECT_KEY to subject, MESSAGE_KEY to message)
            }
        }
    }
}