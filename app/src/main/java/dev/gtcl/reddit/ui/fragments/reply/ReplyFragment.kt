package dev.gtcl.reddit.ui.fragments.reply

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dev.gtcl.reddit.*
import dev.gtcl.reddit.databinding.FragmentReplyBinding
import dev.gtcl.reddit.models.reddit.listing.Comment
import dev.gtcl.reddit.models.reddit.listing.Item
import dev.gtcl.reddit.models.reddit.listing.Message
import dev.gtcl.reddit.models.reddit.listing.Post
import dev.gtcl.reddit.ui.activities.MainActivityVM
import dev.gtcl.reddit.ui.fragments.create_post.CreatePostFragmentArgs

class ReplyFragment: Fragment() {

    private lateinit var binding: FragmentReplyBinding

    private val activityModel: MainActivityVM by activityViewModels()

    private val args: ReplyFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentReplyBinding.inflate(inflater)

        initItem()
        setListeners()

        binding.executePendingBindings()
        return binding.root
    }

    private fun initItem(){
        when(val item = args.parent){
            is Post -> {
                binding.replyToUser = item.author
                binding.replyToBody = item.title
            }
            is Comment -> {
                binding.replyToUser = item.author
                binding.replyToBody = item.body
            }
            is Message -> {
                binding.replyToUser = item.author
                binding.replyToBody = item.body
            }
            else -> {
                throw IllegalArgumentException("Unable to reply to the following item type: ${item.kind}")
            }
        }
    }

    private fun setListeners(){
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.toolbar.setOnMenuItemClickListener {
            val comment = binding.responseText.text.toString()
            activityModel.reply(args.parent, comment, args.position)
            findNavController().popBackStack()
            true
        }
    }
}