package dev.gtcl.reddit.ui.fragments.create_post.flair

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import dev.gtcl.reddit.FLAIRS_KEY
import dev.gtcl.reddit.FLAIR_SELECTED_KEY
import dev.gtcl.reddit.R
import dev.gtcl.reddit.databinding.FragmentFlairTextBinding
import dev.gtcl.reddit.models.reddit.listing.Flair

class FlairEditDialogFragment : DialogFragment(), TextWatcher{

    private lateinit var binding: FragmentFlairTextBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFlairTextBinding.inflate(inflater)
        val flair = requireArguments().get(FLAIRS_KEY) as Flair
        initFlair(flair)
        initObservers(flair)
        return binding.root
    }

    private fun initFlair(flair: Flair){
        binding.flair = flair
        binding.executePendingBindings()
    }

    private fun initObservers(flair: Flair){
        binding.toolbar.setOnMenuItemClickListener {
            if(it.itemId == R.id.close){
                setFragmentResult(FLAIR_SELECTED_KEY, bundleOf(FLAIRS_KEY to null))
                dismiss()
            }
            true
        }

        binding.finishedButton.setOnClickListener {
            val text = binding.text.text.toString()
            if(text.length in 1..64){
                flair.text = text
                setFragmentResult(FLAIR_SELECTED_KEY, bundleOf(FLAIRS_KEY to flair))
                dismiss()
            } else {
                binding.textInputLayout.error = getString(R.string.invalid)
            }
        }

        binding.text.addTextChangedListener(this)
    }

    override fun afterTextChanged(s: Editable?) {}

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        binding.textInputLayout.error = null
    }

    companion object{
        fun newInstance(flair: Flair): FlairEditDialogFragment {
            val fragment = FlairEditDialogFragment()
            val args = bundleOf(FLAIRS_KEY to flair)
            fragment.arguments = args
            return fragment
        }
    }

}