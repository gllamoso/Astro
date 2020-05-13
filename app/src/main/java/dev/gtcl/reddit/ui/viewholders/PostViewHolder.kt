package dev.gtcl.reddit.ui.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.R
import dev.gtcl.reddit.Vote
import dev.gtcl.reddit.databinding.ItemPostBinding
import dev.gtcl.reddit.models.reddit.Post
import dev.gtcl.reddit.actions.PostActions
import dev.gtcl.reddit.databinding.LayoutPopupPostOptionsBinding

class PostViewHolder private constructor(private val binding:ItemPostBinding)
    : RecyclerView.ViewHolder(binding.root) {
    fun bind(post: Post?, postActions: PostActions, isRead: Boolean, hideAction: () -> Unit){
        binding.post = post
        binding.executePendingBindings()
        setIfRead(isRead)
        binding.rootLayout.setOnClickListener {
            setIfRead(true)
            postActions.postClicked(post!!)
        }

        binding.thumbnail.setOnClickListener{
            setIfRead(true)
            postActions.thumbnailClicked(post!!)
        }

        if(post != null){
            binding.moreOptions.setOnClickListener {
                showPopupWindow(post, postActions, it, hideAction)
            }
        }
    }

    private fun showPopupWindow(post: Post, postActions: PostActions, anchorView: View, hideAction: () -> Unit){
        val inflater = anchorView.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupBinding = LayoutPopupPostOptionsBinding.inflate(inflater)
        val popupWindow = PopupWindow(popupBinding.root, RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT, true)
        popupBinding.apply {
            this.post = post
            upvoteButton.root.setOnClickListener {
                postActions.vote(post, if(post.likes == true) Vote.UNVOTE else Vote.UPVOTE)
                post.likes = if(post.likes == true) null else true
                binding.invalidateAll()
                popupWindow.dismiss()
            }
            downvoteButton.root.setOnClickListener {
                postActions.vote(post, if(post.likes == false) Vote.UNVOTE else Vote.DOWNVOTE)
                post.likes = if(post.likes == false) null else false
                binding.invalidateAll()
                popupWindow.dismiss()
            }
            shareButton.root.setOnClickListener {
                postActions.share(post)
                popupWindow.dismiss()
            }
            profileButton.root.setOnClickListener {
                postActions.viewProfile(post)
                popupWindow.dismiss()
            }
            saveButton.root.setOnClickListener {
                postActions.save(post)
                post.saved = !post.saved
                binding.invalidateAll()
                popupWindow.dismiss()
            }
            hideButton.root.setOnClickListener {
                postActions.hide(post)
                post.hidden = !post.hidden
                hideAction()
                popupWindow.dismiss()
            }
            reportButton.root.setOnClickListener {
                postActions.report(post)
                popupWindow.dismiss()
            }
        }
        popupBinding.root.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        popupWindow.width = ViewGroup.LayoutParams.WRAP_CONTENT
        popupWindow.height = popupBinding.root.measuredHeight
        popupWindow.showAsDropDown(anchorView)
        popupBinding.executePendingBindings()
    }

    private fun setIfRead(isRead: Boolean){
        binding.title.setTextColor(ContextCompat.getColor(binding.root.context, if(isRead) android.R.color.darker_gray else R.color.textColor))
    }

    companion object {
        fun create(parent: ViewGroup): PostViewHolder {
            return PostViewHolder(
                ItemPostBinding.inflate(
                    LayoutInflater.from(parent.context)
                )
            )
        }
    }
}
