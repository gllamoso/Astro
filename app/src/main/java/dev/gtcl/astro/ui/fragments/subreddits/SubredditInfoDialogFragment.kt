package dev.gtcl.astro.ui.fragments.subreddits

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import dev.gtcl.astro.AstroApplication
import dev.gtcl.astro.SUBREDDIT_KEY
import dev.gtcl.astro.ViewModelFactory
import dev.gtcl.astro.actions.LinkHandler
import dev.gtcl.astro.checkIfLoggedInBeforeExecuting
import dev.gtcl.astro.databinding.FragmentDialogSubredditInfoBinding
import dev.gtcl.astro.html.createHtmlViews
import dev.gtcl.astro.ui.activities.MainActivityVM

class SubredditInfoDialogFragment : DialogFragment(), LinkHandler {

    private val activityModel: MainActivityVM by activityViewModels()

    private val model: SubredditInfoVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as AstroApplication)
        ViewModelProvider(this, viewModelFactory).get(SubredditInfoVM::class.java)
    }

    private var binding: FragmentDialogSubredditInfoBinding? = null

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
        binding = FragmentDialogSubredditInfoBinding.inflate(inflater)
        binding?.lifecycleOwner = viewLifecycleOwner
        binding?.model = model

        val subredditName = requireArguments().getString(SUBREDDIT_KEY) ?: return null
        model.fetchSubreddit(subredditName)

        model.subreddit.observe(viewLifecycleOwner, {
            if (it != null) {
                binding?.fragmentDialogSubredditInfoTextLayout?.createHtmlViews(
                    it.parseDescription(),
                        null,
                    this
                )
            }
        })

        binding?.fragmentDialogRulesDialogButtons?.dialogNeutralButton?.setOnClickListener {
            checkIfLoggedInBeforeExecuting(requireContext()) {
                val sub = model.subreddit.value ?: return@checkIfLoggedInBeforeExecuting
                val subscribe = !(sub.userSubscribed ?: false)
                sub.userSubscribed = subscribe
                activityModel.subscribe(sub, subscribe)
                binding?.invalidateAll()
            }
        }

        binding?.fragmentDialogRulesDialogButtons?.dialogNegativeButton?.setOnClickListener {
            dismiss()
        }

        binding?.fragmentDialogRulesDialogButtons?.dialogPositiveButton?.setOnClickListener {
            val sub = model.subreddit.value ?: return@setOnClickListener
            activityModel.subredditSelected(sub)
            dismiss()
        }

        binding?.executePendingBindings()
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        fun newInstance(subredditName: String): SubredditInfoDialogFragment {
            return SubredditInfoDialogFragment().apply {
                arguments = bundleOf(SUBREDDIT_KEY to subredditName)
            }
        }
    }

    override fun handleLink(link: String) {
        activityModel.handleLink(link)
    }
}