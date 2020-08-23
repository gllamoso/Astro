package dev.gtcl.reddit.ui.fragments.report

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import dev.gtcl.reddit.*
import dev.gtcl.reddit.databinding.FragmentDialogReportBinding
import dev.gtcl.reddit.models.reddit.Rule
import dev.gtcl.reddit.models.reddit.listing.Comment
import dev.gtcl.reddit.models.reddit.listing.Item
import dev.gtcl.reddit.models.reddit.listing.Post
import dev.gtcl.reddit.ui.activities.MainActivityVM
import dev.gtcl.reddit.ui.fragments.rules.RulesDialogFragment

class ReportDialogFragment: DialogFragment() {

    private lateinit var binding: FragmentDialogReportBinding

    private val model: ReportVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(ReportVM::class.java)
    }

    private val activityModel: MainActivityVM by activityViewModels()

    private var idToRule = HashMap<Int, RuleData>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder = AlertDialog.Builder(requireContext())
            .setPositiveButton(R.string.report){ _, _ ->
                val rule = idToRule[binding.radioGroup.checkedRadioButtonId]
                if(rule != null){
                    val position = requireArguments().getInt(POSITION_KEY, -1)
                    val item = requireArguments().get(ITEM_KEY) as Item
                    if(rule.type == RuleType.OTHER){
                        val otherReason = binding.otherRuleText.text.toString()
                        if(!otherReason.isBlank()){
                            activityModel.report(item.name, otherReason, RuleType.OTHER)
                            parentFragmentManager.setFragmentResult(REPORT_KEY, bundleOf(POSITION_KEY to position))
                        }
                    } else {
                        activityModel.report(item.name, rule.rule, rule.type)
                        parentFragmentManager.setFragmentResult(REPORT_KEY, bundleOf(POSITION_KEY to position))
                    }
                }
            }
            .setNegativeButton(R.string.cancel){ _, _ -> }
            .setNeutralButton(R.string.rules){ _, _ ->
                val displayName = when(val item = requireArguments().get(ITEM_KEY) as Item){
                    is Post -> item.subreddit
                    is Comment -> item.subreddit
                    else -> throw Exception("Invalid item type: $item")
                }
                RulesDialogFragment.newInstance(displayName).show(parentFragmentManager, null)
            }

        binding = FragmentDialogReportBinding.inflate(LayoutInflater.from(requireContext()))
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
            model.fetchRules((requireArguments().get(ITEM_KEY) as Item))
        }

        model.rules.observe(this, Observer {
            if(it != null){
                idToRule = HashMap()
                binding.radioGroup.removeAllViews()
                var otherButtonId = -1
                for(ruleData in it){
                    val radioButton = RadioButton(requireContext()).apply {
                        text = ruleData.rule
                    }
                    binding.radioGroup.addView(radioButton)
                    idToRule[radioButton.id] = ruleData
                    if(ruleData.type == RuleType.OTHER){
                        otherButtonId = radioButton.id
                    }
                }

                binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
                    if(checkedId == otherButtonId){
                        model.otherRuleSelected()
                    } else {
                        model.otherRuleUnselected()
                    }
                }
            }
        })

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    companion object{
        fun newInstance(item: Item, position: Int = -1): ReportDialogFragment{
            return ReportDialogFragment().apply {
                arguments = bundleOf(ITEM_KEY to item, POSITION_KEY to position)
            }
        }
    }
}