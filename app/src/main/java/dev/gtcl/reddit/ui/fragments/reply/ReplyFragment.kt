package dev.gtcl.reddit.ui.fragments.reply

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.ViewModelFactory
import dev.gtcl.reddit.databinding.FragmentReplyBinding
import dev.gtcl.reddit.models.reddit.listing.Comment
import dev.gtcl.reddit.models.reddit.listing.Message
import dev.gtcl.reddit.models.reddit.listing.Post

class ReplyFragment: Fragment() {

    private lateinit var binding: FragmentReplyBinding

    private val model: ReplyVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(ReplyVM::class.java)
    }

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
        val item = args.item
        when(item){
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
        model.setParentId(item.name)
    }

    private fun setListeners(){
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.toolbar.setOnMenuItemClickListener {
            val comment = binding.responseText.text.toString()
            model.addComment(comment)
            true
        }

        model.response.observe(viewLifecycleOwner, Observer {
            Log.d("TAE", "Item response: $it")
        })
    }
}