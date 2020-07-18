package dev.gtcl.reddit.ui.fragments.create_post.flair

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import dev.gtcl.reddit.*
import dev.gtcl.reddit.databinding.FragmentFlairSelectionBinding
import dev.gtcl.reddit.models.reddit.listing.Flair

class FlairSelectionDialogFragment : DialogFragment(), FlairListAdapter.FlairSelectionListener{

    private lateinit var binding: FragmentFlairSelectionBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFlairSelectionBinding.inflate(inflater)
        initView()
        initObservers()
        binding.invalidateAll()
        return binding.root
    }

    private fun initView(){
        val flairs = requireArguments().get(FLAIRS_KEY) as List<Flair>
        val adapter = FlairListAdapter(this).apply {
            submitList(flairs)
        }
        binding.recyclerView.adapter = adapter

        val subredditName = requireArguments().get(SUBREDDIT_KEY) as String
        binding.subName = subredditName

        binding.invalidateAll()
    }

    private fun initObservers(){
        binding.toolbar.setOnMenuItemClickListener {
            if(it.itemId == R.id.close){
                onSelect(null)
                dismiss()
            }
            true
        }

        binding.finishedButton.setOnClickListener {
            onSelect(null)
            dismiss()
        }
    }

    override fun onSelect(flair: Flair?) {
        setFragmentResult(FLAIR_SELECTED_KEY, bundleOf(FLAIRS_KEY to flair))
        dismiss()
    }

    override fun onEdit(flair: Flair) {
        val fragment = FlairEditDialogFragment.newInstance(flair)
        fragment.show(parentFragmentManager, null)
        dismiss()
    }

    companion object{
        fun newInstance(flairs: List<Flair>, subredditName: String): FlairSelectionDialogFragment {
            val fragment = FlairSelectionDialogFragment()
            val args = bundleOf(FLAIRS_KEY to flairs, SUBREDDIT_KEY to subredditName)
            fragment.arguments = args
            return fragment
        }
    }

}