package dev.gtcl.reddit.ui.fragments.comments.reply

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dev.gtcl.reddit.databinding.FragmentReplyBinding
import dev.gtcl.reddit.models.reddit.listing.Comment
import dev.gtcl.reddit.models.reddit.listing.Message
import dev.gtcl.reddit.models.reddit.listing.Post
import dev.gtcl.reddit.ui.fragments.ViewPagerFragmentArgs

class ReplyFragment: Fragment() {

    private lateinit var binding: FragmentReplyBinding

    private val args: ReplyFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentReplyBinding.inflate(inflater)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.toolbar.setOnMenuItemClickListener {
            Log.d("TAE", "Menu Item Clicked: $it")
            true
        }

        initItem()

        binding.executePendingBindings()
        return binding.root
    }

    private fun initItem(){
        when(val item = args.item){
            is Post -> {
                binding.replyToUser = item.author
                binding.replyToBody = item.title
            }
            is Comment -> {
                Log.d("TAE", "Item Body: ${item.body}")
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
}