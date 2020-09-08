package dev.gtcl.astro.ui.viewholders

import dev.gtcl.astro.databinding.ItemCommentDetailedBinding

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.astro.Vote
import dev.gtcl.astro.actions.CommentActions
import dev.gtcl.astro.actions.ItemClickListener
import dev.gtcl.astro.databinding.PopupCommentActionsBinding
import dev.gtcl.astro.models.reddit.listing.Comment
import dev.gtcl.astro.showAsDropdown
import io.noties.markwon.Markwon

class CommentDetailedVH private constructor(private val binding: ItemCommentDetailedBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(
        comment: Comment,
        markwon: Markwon?,
        commentActions: CommentActions,
        username: String?,
        inInbox: Boolean,
        itemClickListener: ItemClickListener
    ) {
        val isUser = (username != null && comment.author == username)
        binding.comment = comment
        binding.isUser = isUser
        binding.inInbox = inInbox
        binding.itemCommentDetailedBackground.setOnClickListener {
            itemClickListener.itemClicked(comment, adapterPosition)
        }
        binding.itemCommentDetailedMoreOptions.setOnClickListener {
            showPopupWindow(comment, commentActions, isUser, inInbox, it)
        }
        if (markwon != null) {
            markwon.setMarkdown(binding.itemCommentDetailedBodyMessage, comment.body)
        } else {
            binding.itemCommentDetailedBodyMessage.text = comment.bodyFormatted
        }
        binding.executePendingBindings()
    }

    private fun showPopupWindow(
        comment: Comment,
        commentActions: CommentActions,
        createdFromUser: Boolean,
        inInbox: Boolean,
        anchor: View
    ) {
        val inflater =
            anchor.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupBinding = PopupCommentActionsBinding.inflate(inflater)
        val popupWindow = PopupWindow()
        popupBinding.apply {
            this.comment = comment
            this.createdFromUser = createdFromUser
            this.inInbox = inInbox
            if (createdFromUser) {
                popupCommentActionsEdit.root.setOnClickListener {
                    commentActions.edit(comment, adapterPosition)
                    popupWindow.dismiss()
                }
                popupCommentActionsDelete.root.setOnClickListener {
                    commentActions.delete(comment, adapterPosition)
                    popupWindow.dismiss()
                }
            }
            popupCommentActionsUpvote.root.setOnClickListener {
                commentActions.vote(
                    comment,
                    if (comment.likes == true) Vote.UNVOTE else Vote.UPVOTE
                )
                binding.invalidateAll()
                popupWindow.dismiss()
            }
            popupCommentActionsDownvote.root.setOnClickListener {
                commentActions.vote(
                    comment,
                    if (comment.likes == false) Vote.UNVOTE else Vote.DOWNVOTE
                )
                binding.invalidateAll()
                popupWindow.dismiss()
            }
            popupCommentActionsReply.root.setOnClickListener {
                commentActions.reply(comment, adapterPosition)
                popupWindow.dismiss()
            }
            popupCommentActionsSave.root.setOnClickListener {
                commentActions.save(comment)
                binding.invalidateAll()
                popupWindow.dismiss()
            }
            if (inInbox) {
                popupCommentActionsMark.root.setOnClickListener {
                    commentActions.mark(comment)
                    binding.invalidateAll()
                    popupWindow.dismiss()
                }
                if (!createdFromUser) {
                    popupCommentActionsBlock.root.setOnClickListener {
                        commentActions.block(comment, adapterPosition)
                        popupWindow.dismiss()
                    }
                }
            }
            popupCommentActionsProfile.root.setOnClickListener {
                commentActions.viewProfile(comment)
                popupWindow.dismiss()
            }
            popupCommentActionsShare.root.setOnClickListener {
                commentActions.share(comment)
                popupWindow.dismiss()
            }
            popupCommentActionsReport.root.setOnClickListener {
                commentActions.report(comment, adapterPosition)
                popupWindow.dismiss()
            }
            executePendingBindings()
            root.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
        }

        popupWindow.showAsDropdown(
            anchor,
            popupBinding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            popupBinding.root.measuredHeight
        )
    }

    companion object {
        fun create(parent: ViewGroup) =
            CommentDetailedVH(ItemCommentDetailedBinding.inflate(LayoutInflater.from(parent.context)))
    }
}