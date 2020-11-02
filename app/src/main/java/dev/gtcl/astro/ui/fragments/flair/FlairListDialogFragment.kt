package dev.gtcl.astro.ui.fragments.flair

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import dev.gtcl.astro.*
import dev.gtcl.astro.databinding.FragmentDialogFlairListBinding
import dev.gtcl.astro.models.reddit.listing.Flair

class FlairListDialogFragment : DialogFragment(), FlairListAdapter.FlairSelectionListener {

    private var binding: FragmentDialogFlairListBinding? = null

    private val model: FlairListVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as AstroApplication)
        ViewModelProvider(this, viewModelFactory).get(FlairListVM::class.java)
    }

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
        binding = FragmentDialogFlairListBinding.inflate(LayoutInflater.from(requireContext()))
        binding?.model = model
        binding?.lifecycleOwner = this
        val subredditName = requireArguments().getString(SUBREDDIT_KEY, "")
        model.fetchFlairs(subredditName)

        initList()
        initOtherObservers()

        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun initList() {
        val adapter = FlairListAdapter(this)
        binding?.fragmentDialogFlairSelectionRecyclerView?.adapter = adapter

        model.flairs.observe(this, {
            if (it != null) {
                adapter.submitList(it)
            }
        })
    }

    private fun initOtherObservers() {
        binding?.fragmentDialogFlairSelectionToolbar?.setNavigationOnClickListener {
            dismiss()
        }

        model.errorMessage.observe(this, {
            if (it != null) {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                model.errorMessageObserved()
            }
        })

        binding?.fragmentDialogFlairListDialogButtons?.dialogNegativeButton?.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                FLAIR_SELECTED_KEY,
                bundleOf(FLAIRS_KEY to null)
            )
            dismiss()
        }

        binding?.fragmentDialogFlairListDialogButtons?.dialogNeutralButton?.setOnClickListener {
            dismiss()
        }

    }

    override fun onSelect(flair: Flair) {
        parentFragmentManager.setFragmentResult(FLAIR_SELECTED_KEY, bundleOf(FLAIRS_KEY to flair))
        dismiss()
    }

    override fun onEdit(flair: Flair) {
        FlairEditDialogFragment.newInstance(flair).show(parentFragmentManager, null)
        dismiss()
    }

    companion object {
        fun newInstance(subredditName: String): FlairListDialogFragment {
            val fragment = FlairListDialogFragment()
            val args = bundleOf(SUBREDDIT_KEY to subredditName)
            fragment.arguments = args
            return fragment
        }
    }

}