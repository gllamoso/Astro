package dev.gtcl.reddit.ui.fragments.flair

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.method.TextKeyListener.clear
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import dev.gtcl.reddit.*
import dev.gtcl.reddit.databinding.FragmentDialogFlairSelectionBinding
import dev.gtcl.reddit.models.reddit.listing.Flair

class FlairListDialogFragment : DialogFragment(), FlairListAdapter.FlairSelectionListener{

    private lateinit var binding: FragmentDialogFlairSelectionBinding

    private val model: FlairListVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(FlairListVM::class.java)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context)
            .setNegativeButton(R.string.clear){_,_ ->
                parentFragmentManager.setFragmentResult(FLAIR_SELECTED_KEY, bundleOf(FLAIRS_KEY to null))
            }
            .setNeutralButton(R.string.cancel){_,_ ->}

        binding = FragmentDialogFlairSelectionBinding.inflate(LayoutInflater.from(requireContext()))
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
        val subredditName = requireArguments().get(SUBREDDIT_KEY) as String
        model.fetchFlairs(subredditName)

        initList()
        initOtherObservers()
        binding.invalidateAll()

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    private fun initList(){
        val adapter = FlairListAdapter(this)
        binding.recyclerView.adapter = adapter

        model.flairs.observe(this, Observer {
            if(it != null){
                adapter.submitList(it)
            }
        })
    }

    private fun initOtherObservers(){
        binding.toolbar.setNavigationOnClickListener {
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

    companion object{
        fun newInstance(subredditName: String): FlairListDialogFragment {
            val fragment = FlairListDialogFragment()
            val args = bundleOf(SUBREDDIT_KEY to subredditName)
            fragment.arguments = args
            return fragment
        }
    }

}