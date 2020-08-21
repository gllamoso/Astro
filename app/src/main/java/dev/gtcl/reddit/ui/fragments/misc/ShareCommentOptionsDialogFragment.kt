package dev.gtcl.reddit.ui.fragments.misc

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import dev.gtcl.reddit.COMMENT_KEY
import dev.gtcl.reddit.R
import dev.gtcl.reddit.databinding.FragmentShareCommentOptionsBinding
import dev.gtcl.reddit.models.reddit.listing.Comment
import dev.gtcl.reddit.models.reddit.listing.Post

class ShareCommentOptionsDialogFragment: DialogFragment() {

    private lateinit var binding: FragmentShareCommentOptionsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentShareCommentOptionsBinding.inflate(inflater)
        val comment = requireArguments().getParcelable(COMMENT_KEY) as Comment
        initClickListeners(comment)
        return binding.root
    }

    private fun initClickListeners(comment: Comment){
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getText(R.string.share_subject_message))

        binding.link.root.setOnClickListener {
            shareIntent.putExtra(Intent.EXTRA_TEXT, comment.permalinkWithRedditDomain)
            startActivity(Intent.createChooser(shareIntent, null))
            dismiss()
        }

        binding.text.root.setOnClickListener {
            shareIntent.putExtra(Intent.EXTRA_TEXT, comment.bodyFormatted)
            startActivity(Intent.createChooser(shareIntent, null))
            dismiss()
        }

    }

    companion object{
        fun newInstance(comment: Comment): ShareCommentOptionsDialogFragment{
            return ShareCommentOptionsDialogFragment().apply {
                arguments = bundleOf(COMMENT_KEY to comment)
            }
        }
    }
}