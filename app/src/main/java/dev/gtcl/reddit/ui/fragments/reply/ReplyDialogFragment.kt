package dev.gtcl.reddit.ui.fragments.reply

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import dev.gtcl.reddit.*
import dev.gtcl.reddit.databinding.FragmentReplyBinding
import dev.gtcl.reddit.models.reddit.listing.Comment
import dev.gtcl.reddit.models.reddit.listing.Item
import dev.gtcl.reddit.models.reddit.listing.Message
import dev.gtcl.reddit.models.reddit.listing.Post
import dev.gtcl.reddit.ui.fragments.listing.ListingVM

class ReplyDialogFragment: DialogFragment() {

    private lateinit var binding: FragmentReplyBinding

    private val model: ReplyVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(ReplyVM::class.java)
    }

    override fun onStart() {
        super.onStart()

        dialog?.let {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            it.window?.setLayout(width, height)
        }

//        dialog?.window?.setBackgroundDrawableResource(android.R.color.black) // This makes the dialog full screen
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentReplyBinding.inflate(inflater)

        val parent = requireArguments().get(ITEM_KEY) as Item
        val position = requireArguments().get(POSITION_KEY) as Int

        initParent(parent)
        setListeners(parent, position)

        binding.executePendingBindings()
        return binding.root
    }

    private fun initParent(parent: Item){
        when(parent){
            is Post -> {
                binding.replyToUser = parent.author
                binding.replyToBody = parent.title
            }
            is Comment -> {
                binding.replyToUser = parent.author
                binding.replyToBody = parent.body
            }
            is Message -> {
                binding.replyToUser = parent.author
                binding.replyToBody = parent.body
            }
            else -> {
                throw IllegalArgumentException("Unable to reply to the following item type: ${parent.kind}")
            }
        }
    }

    private fun setListeners(parent: Item, position: Int){
        binding.toolbar.setNavigationOnClickListener {
           dismiss()
        }

        binding.toolbar.setOnMenuItemClickListener {
            val comment = binding.responseText.text.toString()
            model.reply(parent, comment, position)
            true
        }

        model.newReply.observe(viewLifecycleOwner, Observer {
            if(it != null){
                parentFragmentManager.setFragmentResult(NEW_REPLY_KEY, bundleOf(NEW_REPLY_KEY to it))
                dismiss()
            }
        })
    }

    companion object{
        fun newInstance(parent: Item, position: Int): ReplyDialogFragment{
            return ReplyDialogFragment().apply {
                arguments = bundleOf(ITEM_KEY to parent, POSITION_KEY to position)
            }
        }
    }



}