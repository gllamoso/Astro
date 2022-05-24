package dev.gtcl.astro.ui.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.astro.Vote
import dev.gtcl.astro.actions.CommentActions
import dev.gtcl.astro.actions.ItemClickListener
import dev.gtcl.astro.databinding.ItemCommentDetailedBinding
import dev.gtcl.astro.databinding.PopupCommentActionsBinding
import dev.gtcl.astro.html.createHtmlViews
import dev.gtcl.astro.models.reddit.listing.Comment
import dev.gtcl.astro.showAsDropdown
import me.saket.bettermovementmethod.BetterLinkMovementMethod

class CommentDetailedVH private constructor(private val binding: ItemCommentDetailedBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(
        comment: Comment,
        movementMethod: BetterLinkMovementMethod,
        commentActions: CommentActions,
        username: String?,
        inInbox: Boolean,
        itemClickListener: ItemClickListener
    ) {
        val isUser = (username != null && comment.author == username)
        binding.apply {
            this.comment = comment
            this.isUser = isUser
            this.inInbox = inInbox

            itemCommentDetailedBackground.apply {
                setOnClickListener {
                    itemClickListener.itemClicked(comment, adapterPosition)
                }
                setOnLongClickListener {
                    itemClickListener.itemLongClicked(comment, adapterPosition)
                    true
                }
            }

            itemCommentDetailedBottomPanel.apply {
                layoutItemBottomPanelUpvoteButton.setOnClickListener {
                    commentActions.vote(
                        comment,
                        if (comment.likes == true) Vote.UNVOTE else Vote.UPVOTE
                    )
                    binding.invalidateAll()
                }

                layoutItemBottomPanelDownvoteButton.setOnClickListener {
                    commentActions.vote(
                        comment,
                        if (comment.likes == false) Vote.UNVOTE else Vote.DOWNVOTE
                    )
                    binding.invalidateAll()
                }

                layoutItemBottomPanelSaveButton.setOnClickListener {
                    commentActions.save(comment)
                    binding.invalidateAll()
                }

                layoutItemBottomPanelReplyButton.setOnClickListener {
                    commentActions.reply(comment, adapterPosition)
                }

                layoutItemBottomPanelMoreOptions.setOnClickListener {
                    showPopupWindow(comment, commentActions, isUser, inInbox, it)
                }
            }

            itemCommentDetailedBodyMessageLayout.createHtmlViews(
                comment.parseBody(),
                null,
                movementMethod
            )

            executePendingBindings()
        }

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
            popupCommentActionsShare.root.setOnClickListener {
                commentActions.share(comment)
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