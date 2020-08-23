package dev.gtcl.reddit.ui.fragments.rules

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import dev.gtcl.reddit.*
import dev.gtcl.reddit.databinding.FragmentDialogRulesBinding
import dev.gtcl.reddit.databinding.ItemRuleBinding
import io.noties.markwon.Markwon

class RulesDialogFragment: DialogFragment() {

    private val markwon: Markwon by lazy {
        createMarkwonInstance(requireContext()){}
    }

    private val model: RulesVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(RulesVM::class.java)
    }

    private lateinit var binding: FragmentDialogRulesBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context)
            .setPositiveButton(R.string.done){_,_ ->}

        binding = FragmentDialogRulesBinding.inflate(LayoutInflater.from(requireContext()))
        builder.setView(binding.root)

        return builder.create()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding.model = model
        binding.lifecycleOwner = this

        if(model.rules.value == null){
            val displayName = requireArguments().getString(SUBREDDIT_KEY)!!
            model.fetchRules(displayName)
        }

        model.rules.observe(this, Observer {
            if(!it.isNullOrEmpty()){
                binding.linearLayout.removeAllViews()
                for(rule in it){
                    val ruleBinding = ItemRuleBinding.inflate(LayoutInflater.from(requireContext()))
                    ruleBinding.apply {
                        this.rule = rule
                        ruleDescription.text = markwon.toMarkdown(rule.description)
                        ruleBinding.invalidateAll()
                    }
                    binding.linearLayout.addView(ruleBinding.root)
                }
            }
        })

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    companion object{
        fun newInstance(subDisplayName: String): RulesDialogFragment{
            return RulesDialogFragment().apply {
                arguments = bundleOf(SUBREDDIT_KEY to subDisplayName)
            }
        }
    }
}