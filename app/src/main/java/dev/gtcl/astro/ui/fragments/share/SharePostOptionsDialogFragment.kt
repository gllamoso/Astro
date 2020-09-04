package dev.gtcl.astro.ui.fragments.share

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import dev.gtcl.astro.POST_KEY
import dev.gtcl.astro.R
import dev.gtcl.astro.databinding.FragmentDialogSharePostOptionsBinding
import dev.gtcl.astro.models.reddit.listing.Post
import dev.gtcl.astro.ui.fragments.create_post.CreatePostDialogFragment

class SharePostOptionsDialogFragment : DialogFragment(){

    private lateinit var binding: FragmentDialogSharePostOptionsBinding

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
//        dialog?.window?.setBackgroundDrawableResource(android.R.color.black) // This makes the dialog full screen
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentDialogSharePostOptionsBinding.inflate(inflater)
        val post = requireArguments().getParcelable<Post>(POST_KEY)!!
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
            binding.fragmentDialogSharePostOptionsLink.root.setOnClickListener {
                shareIntent.putExtra(Intent.EXTRA_TEXT, post.url)
                startActivity(Intent.createChooser(shareIntent, null))
                dismiss()
            }
        }

        binding.fragmentDialogSharePostOptionsComments.root.setOnClickListener {
            shareIntent.putExtra(Intent.EXTRA_TEXT, post.permalinkWithRedditDomain)
            startActivity(Intent.createChooser(shareIntent, null))
            dismiss()
        }

        if(post.isCrosspostable){
            binding.fragmentDialogSharePostOptionsCrosspost.root.setOnClickListener {
                CreatePostDialogFragment.newInstance(post).show(parentFragmentManager, null)
                dismiss()
            }
        }

        binding.fragmentDialogSharePostOptionsTitleAndLink.root.setOnClickListener {
            shareIntent.putExtra(Intent.EXTRA_TEXT, "${post.titleFormatted} - ${post.permalinkWithRedditDomain}")
            startActivity(Intent.createChooser(shareIntent, null))
            dismiss()
        }

        binding.fragmentDialogSharePostOptionsShortLink.root.setOnClickListener {
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