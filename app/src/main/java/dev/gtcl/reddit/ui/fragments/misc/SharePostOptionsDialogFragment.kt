package dev.gtcl.reddit.ui.fragments.misc

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import dev.gtcl.reddit.POST_KEY
import dev.gtcl.reddit.R
import dev.gtcl.reddit.databinding.FragmentSharePostOptionsBinding
import dev.gtcl.reddit.models.reddit.listing.Post

class SharePostOptionsDialogFragment : DialogFragment(){

    private lateinit var binding: FragmentSharePostOptionsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSharePostOptionsBinding.inflate(inflater)
        val post = requireArguments().getParcelable(POST_KEY) as Post
        binding.post = post
        initClickListeners(post)
        binding.invalidateAll()
        return binding.root
    }

    private fun initClickListeners(post: Post){
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getText(R.string.share_subject_message))

        if(post.url != null){
            binding.link.root.setOnClickListener {
                shareIntent.putExtra(Intent.EXTRA_TEXT, post.url)
                startActivity(Intent.createChooser(shareIntent, null))
                dismiss()
            }
        }

        binding.comments.root.setOnClickListener {
            shareIntent.putExtra(Intent.EXTRA_TEXT, post.permalinkWithRedditDomain)
            startActivity(Intent.createChooser(shareIntent, null))
            dismiss()
        }

        binding.crosspost.root.setOnClickListener {
            TODO()
            dismiss()
        }

        binding.titleAndLink.root.setOnClickListener {
            shareIntent.putExtra(Intent.EXTRA_TEXT, "${post.titleFormatted} - ${post.permalinkWithRedditDomain}")
            startActivity(Intent.createChooser(shareIntent, null))
            dismiss()
        }

        binding.shortlink.root.setOnClickListener {
            shareIntent.putExtra(Intent.EXTRA_TEXT, post.shortLink)
            startActivity(Intent.createChooser(shareIntent, null))
            dismiss()
        }
    }

    companion object{
        fun newInstance(post: Post): SharePostOptionsDialogFragment{
            val fragment = SharePostOptionsDialogFragment()
            val args = bundleOf(POST_KEY to post)
            fragment.arguments = args
            return fragment
        }
    }
}