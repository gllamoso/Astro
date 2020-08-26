package dev.gtcl.reddit.ui.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.Vote
import dev.gtcl.reddit.actions.CommentActions
import dev.gtcl.reddit.actions.ItemClickListener
import dev.gtcl.reddit.databinding.ItemCommentBinding
import dev.gtcl.reddit.databinding.PopupCommentOptionsBinding
import dev.gtcl.reddit.models.reddit.listing.Comment
import dev.gtcl.reddit.showAsDropdown
import io.noties.markwon.Markwon

class CommentVH private constructor(private val binding: ItemCommentBinding): RecyclerView.ViewHolder(binding.root) {
    fun bind(comment: Comment, markwon: Markwon, commentActions: CommentActions, userId: String?, itemClickListener: ItemClickListener){
        binding.comment = comment
        binding.constraintLayout.setOnClickListener {
            itemClickListener.itemClicked(comment, adapterPosition)
        }
        binding.moreOptions.setOnClickListener {
            showPopupWindow(comment, commentActions, (userId != null && comment.authorFullName == userId), it)
        }
        markwon.setMarkdown(binding.bodyMessage, comment.body)
        binding.executePendingBindings()
    }

    private fun showPopupWindow(comment: Comment, commentActions: CommentActions, createdFromUser: Boolean, anchor: View){
        val inflater = anchor.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupBinding = PopupCommentOptionsBinding.inflate(inflater)
        val popupWindow = PopupWindow()
        popupBinding.apply {
            this.comment = comment
            this.createdFromUser = createdFromUser
            if(createdFromUser){
                editButton.root.setOnClickListener {
                    commentActions.edit(comment, adapterPosition)
                    popupWindow.dismiss()
                }
                deleteButton.root.setOnClickListener {
                    commentActions.delete(comment, adapterPosition)
                    popupWindow.dismiss()
                }
            }
            upvoteButton.root.setOnClickListener {
                commentActions.vote(comment, if(comment.likes == true) Vote.UNVOTE else Vote.UPVOTE)
                comment.likes = if(comment.likes == true) {
                    null
                } else {
                    true
                }
                binding.invalidateAll()
                popupWindow.dismiss()
            }
            downvoteButton.root.setOnClickListener {
                commentActions.vote(comment, if(comment.likes == false) Vote.UNVOTE else Vote.DOWNVOTE)
                comment.likes = if(comment.likes == false) {
                    null
                } else {
                    false
                }
                binding.invalidateAll()
                popupWindow.dismiss()
            }
            replyButton.root.setOnClickListener {
                commentActions.reply(comment, adapterPosition)
                popupWindow.dismiss()
            }
            saveButton.root.setOnClickListener {
                comment.saved = comment.saved != true
                commentActions.save(comment)
                binding.invalidateAll()
                popupWindow.dismiss()
            }
            profileButton.root.setOnClickListener {
                commentActions.viewProfile(comment)
                popupWindow.dismiss()
            }
            shareButton.root.setOnClickListener {
                commentActions.share(comment)
                popupWindow.dismiss()
            }
            reportButton.root.setOnClickListener {
                commentActions.report(comment, adapterPosition)
                popupWindow.dismiss()
            }
            executePendingBindings()
            root.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
        }

        popupWindow.showAsDropdown(anchor, popupBinding.root, ViewGroup.LayoutParams.WRAP_CONTENT, popupBinding.root.measuredHeight)
    }

    companion object{
        fun create(parent: ViewGroup): CommentVH {
            return CommentVH(ItemCommentBinding.inflate(LayoutInflater.from(parent.context)))
        }
    }
}