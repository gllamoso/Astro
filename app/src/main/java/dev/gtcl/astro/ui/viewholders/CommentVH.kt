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
import dev.gtcl.astro.databinding.ItemCommentBinding
import dev.gtcl.astro.databinding.PopupCommentActionsBinding
import dev.gtcl.astro.html.createHtmlViews
import dev.gtcl.astro.models.reddit.listing.Comment
import dev.gtcl.astro.showAsDropdown
import me.saket.bettermovementmethod.BetterLinkMovementMethod

class CommentVH private constructor(private val binding: ItemCommentBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(
        comment: Comment,
        movementMethod: BetterLinkMovementMethod,
        commentActions: CommentActions,
        userId: String?,
        isLastItem: Boolean,
        itemClickListener: ItemClickListener
    ) {
        val isUser = (userId != null && comment.authorFullName == userId)
        binding.apply {
            showBottomDivider = isLastItem
            this.comment = comment
            this.isUser = isUser
            showTopDivider = (adapterPosition != 0 && comment.depth ?: 0 == 0)
            itemCommentBackground.apply {
                setOnClickListener {
                    itemClickListener.itemClicked(comment, adapterPosition)
                }
                setOnLongClickListener {
                    if(comment.isCollapsed){
                        false
                    } else {
                        itemClickListener.itemLongClicked(comment, adapterPosition)
                        true
                    }
                }
            }
            itemCommentBottomPanel.apply {
                layoutCommentBottomPanelSmallUpvoteButton.setOnClickListener {
                    commentActions.vote(
                        comment,
                        if (comment.likes == true) Vote.UNVOTE else Vote.UPVOTE
                    )
                    binding.invalidateAll()
                }

                layoutCommentBottomPanelSmallDownvoteButton.setOnClickListener {
                    commentActions.vote(
                        comment,
                        if (comment.likes == false) Vote.UNVOTE else Vote.DOWNVOTE
                    )
                    binding.invalidateAll()
                }

                layoutCommentBottomPanelSmallSaveButton.setOnClickListener {
                    commentActions.save(comment)
                    binding.invalidateAll()
                }


                layoutCommentBottomPanelSmallReplyButton.setOnClickListener {
                    commentActions.reply(comment, adapterPosition)
                }

                layoutCommentBottomPanelSmallMoreOptions.setOnClickListener {
                    showPopupWindow(comment, commentActions, isUser, it)
                }
            }
            itemCommentBodyMessageLayout.createHtmlViews(comment.parseBody(), null, movementMethod)
            executePendingBindings()
        }
    }

    private fun showPopupWindow(
        comment: Comment,
        commentActions: CommentActions,
        createdFromUser: Boolean,
        anchor: View
    ) {
        val inflater =
            anchor.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupBinding = PopupCommentActionsBinding.inflate(inflater)
        val popupWindow = PopupWindow()
        popupBinding.apply {
            this.comment = comment
            this.createdFromUser = createdFromUser
            this.inInbox = false
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
        fun create(parent: ViewGroup): CommentVH {
            return CommentVH(ItemCommentBinding.inflate(LayoutInflater.from(parent.context)))
        }
    }
}