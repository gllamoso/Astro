package dev.gtcl.astro.ui.fragments.rules

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import dev.gtcl.astro.*
import dev.gtcl.astro.databinding.FragmentDialogRulesBinding
import dev.gtcl.astro.databinding.ItemRuleBinding
import io.noties.markwon.Markwon

class RulesDialogFragment: DialogFragment() {

    private val markwon: Markwon by lazy {
        createMarkwonInstance(requireContext()){}
    }

    private val model: RulesVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as AstroApplication)
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

        model.rules.observe(this, {
            if(!it.isNullOrEmpty()){
                binding.fragmentDialogRulesLinearLayout.removeAllViews()
                for(rule in it){
                    val ruleBinding = ItemRuleBinding.inflate(LayoutInflater.from(requireContext()))
                    ruleBinding.apply {
                        this.rule = rule
                        itemRuleDescription.text = markwon.toMarkdown(rule.description)
                        ruleBinding.invalidateAll()
                    }
                    binding.fragmentDialogRulesLinearLayout.addView(ruleBinding.root)
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