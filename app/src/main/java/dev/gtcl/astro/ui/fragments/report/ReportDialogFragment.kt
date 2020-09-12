package dev.gtcl.astro.ui.fragments.report

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import dev.gtcl.astro.*
import dev.gtcl.astro.databinding.FragmentDialogReportBinding
import dev.gtcl.astro.models.reddit.listing.Comment
import dev.gtcl.astro.models.reddit.listing.Item
import dev.gtcl.astro.models.reddit.listing.Post
import dev.gtcl.astro.ui.activities.MainActivityVM
import dev.gtcl.astro.ui.fragments.rules.RulesDialogFragment

class ReportDialogFragment : DialogFragment() {

    private var binding: FragmentDialogReportBinding? = null

    private val model: ReportVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as AstroApplication)
        ViewModelProvider(this, viewModelFactory).get(ReportVM::class.java)
    }

    private val activityModel: MainActivityVM by activityViewModels()

    private var idToRule = HashMap<Int, RuleData>()

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
        binding = FragmentDialogReportBinding.inflate(inflater)
        binding?.model = model
        binding?.lifecycleOwner = this

        if (model.rules.value == null) {
            model.fetchRules((requireArguments().get(ITEM_KEY) as Item))
        }

        model.rules.observe(this, {
            if (it != null) {
                idToRule = HashMap()
                binding?.fragmentDialogReportRadioGroup?.removeAllViews()
                var otherButtonId = -1
                for (ruleData in it) {
                    val radioButton = RadioButton(requireContext()).apply {
                        text = ruleData.rule
                    }
                    binding?.fragmentDialogReportRadioGroup?.addView(radioButton)
                    idToRule[radioButton.id] = ruleData
                    if (ruleData.type == RuleType.OTHER) {
                        otherButtonId = radioButton.id
                    }
                }

                binding?.fragmentDialogReportRadioGroup?.setOnCheckedChangeListener { _, checkedId ->
                    if (checkedId == otherButtonId) {
                        model.otherRuleSelected()
                    } else {
                        model.otherRuleUnselected()
                    }
                }
            }
        })

        model.errorMessage.observe(this, {
            if (it != null) {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                model.errorMessageObserved()
            }
        })

        setOnClickListeners()

        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun setOnClickListeners() {

        binding?.fragmentDialogReportDialogButtons?.apply {
            dialogPositiveButton.setOnClickListener {
                val rule =
                    idToRule[binding?.fragmentDialogReportRadioGroup?.checkedRadioButtonId ?: 0]
                if (rule != null) {
                    val position = requireArguments().getInt(POSITION_KEY, -1)
                    val item = requireArguments().get(ITEM_KEY) as Item
                    if (rule.type == RuleType.OTHER) {
                        val otherReason =
                            binding?.fragmentDialogReportOtherRuleText?.text.toString()
                        if (!otherReason.isBlank()) {
                            activityModel.report(item.name, otherReason, RuleType.OTHER)
                            parentFragmentManager.setFragmentResult(
                                REPORT_KEY,
                                bundleOf(POSITION_KEY to position)
                            )
                        }
                    } else {
                        activityModel.report(item.name, rule.rule, rule.type)
                        parentFragmentManager.setFragmentResult(
                            REPORT_KEY,
                            bundleOf(POSITION_KEY to position)
                        )
                    }
                }
                dismiss()
            }
            dialogNegativeButton.setOnClickListener {
                dismiss()
            }
            dialogNeutralButton.setOnClickListener {
                val displayName = when (val item = requireArguments().get(ITEM_KEY) as Item) {
                    is Post -> item.subreddit
                    is Comment -> item.subreddit
                    else -> throw Exception("Invalid item type: $item")
                }
                RulesDialogFragment.newInstance(displayName).show(parentFragmentManager, null)
                dismiss()
            }
        }

    }

    companion object {
        fun newInstance(item: Item, position: Int = -1): ReportDialogFragment {
            return ReportDialogFragment().apply {
                arguments = bundleOf(ITEM_KEY to item, POSITION_KEY to position)
            }
        }
    }
}