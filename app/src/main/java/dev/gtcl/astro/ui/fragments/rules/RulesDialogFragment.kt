package dev.gtcl.astro.ui.fragments.rules

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import dev.gtcl.astro.AstroApplication
import dev.gtcl.astro.SUBREDDIT_KEY
import dev.gtcl.astro.ViewModelFactory
import dev.gtcl.astro.actions.LinkHandler
import dev.gtcl.astro.databinding.FragmentDialogRulesBinding
import dev.gtcl.astro.databinding.ItemRuleBinding
import dev.gtcl.astro.html.createHtmlViews
import dev.gtcl.astro.models.reddit.listing.SubredditListing
import dev.gtcl.astro.ui.activities.MainActivityVM
import dev.gtcl.astro.ui.fragments.view_pager.AccountPage
import dev.gtcl.astro.ui.fragments.view_pager.ListingPage
import dev.gtcl.astro.ui.fragments.view_pager.ViewPagerFragmentDirections
import dev.gtcl.astro.url.*

class RulesDialogFragment : DialogFragment(), LinkHandler {

    private val activityModel: MainActivityVM by activityViewModels()

    private val model: RulesVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as AstroApplication)
        ViewModelProvider(this, viewModelFactory).get(RulesVM::class.java)
    }

    private var binding: FragmentDialogRulesBinding? = null

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
        binding = FragmentDialogRulesBinding.inflate(inflater)
        binding?.model = model
        binding?.lifecycleOwner = this

        if (model.rules.value == null) {
            val displayName = requireArguments().getString(SUBREDDIT_KEY) ?: return null
            model.fetchRules(displayName)
        }

        model.rules.observe(this, {
            if (!it.isNullOrEmpty()) {
                binding?.fragmentDialogRulesLinearLayout?.removeAllViews()
                for (rule in it) {
                    val ruleBinding = ItemRuleBinding.inflate(LayoutInflater.from(requireContext()))
                    ruleBinding.apply {
                        this.rule = rule
                        itemRuleDescriptionLayout.createHtmlViews(
                            rule.parseDescription(),
                                null,
                            this@RulesDialogFragment
                        )
                        ruleBinding.invalidateAll()
                    }
                    binding?.fragmentDialogRulesLinearLayout?.addView(ruleBinding.root)
                }
            }
        })

        model.errorMessage.observe(this, {
            if (it != null) {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                model.errorMessageObserved()
            }
        })

        binding?.fragmentDialogRulesDialogButtons?.dialogPositiveButton?.setOnClickListener {
            dismiss()
        }

        return binding?.root
    }

    override fun handleLink(url: URL) {
        when(val urlType = url.urlType ?: url.url.getUrlType()){
            UrlType.USER -> {
                val user = REDDIT_USER_REGEX.getFirstGroup(url.url)
                findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentSelf(AccountPage(user)))
                dismiss()
            }
            UrlType.SUBREDDIT -> {
                val subreddit = SUBREDDIT_REGEX.getFirstGroup(url.url) ?: return
                findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentSelf(ListingPage(SubredditListing(subreddit))))
                dismiss()
            }
            else -> {
                activityModel.handleLink(URL(url.url, urlType))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        fun newInstance(subDisplayName: String): RulesDialogFragment {
            return RulesDialogFragment().apply {
                arguments = bundleOf(SUBREDDIT_KEY to subDisplayName)
            }
        }
    }
}