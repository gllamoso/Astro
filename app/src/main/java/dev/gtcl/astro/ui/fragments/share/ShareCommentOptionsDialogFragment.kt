package dev.gtcl.astro.ui.fragments.share

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import dev.gtcl.astro.COMMENT_KEY
import dev.gtcl.astro.R
import dev.gtcl.astro.databinding.FragmentDialogShareCommentOptionsBinding
import dev.gtcl.astro.models.reddit.listing.Comment

class ShareCommentOptionsDialogFragment: DialogFragment() {

    private lateinit var binding: FragmentDialogShareCommentOptionsBinding

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
//        dialog?.window?.setBackgroundDrawableResource(android.R.color.black) // This makes the dialog full screen
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDialogShareCommentOptionsBinding.inflate(inflater)
        val comment = requireArguments().getParcelable<Comment>(COMMENT_KEY)!!
        initClickListeners(comment)
        return binding.root
    }

    private fun initClickListeners(comment: Comment){
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getText(R.string.share_subject_message))

        binding.fragmentDialogShareCommentsLink.root.setOnClickListener {
            shareIntent.putExtra(Intent.EXTRA_TEXT, comment.permalinkWithRedditDomain)
            startActivity(Intent.createChooser(shareIntent, null))
            dismiss()
        }

        binding.fragmentDialogShareCommentsText.root.setOnClickListener {
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