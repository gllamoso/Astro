package dev.gtcl.reddit.ui.viewholders

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.RelativeLayout
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import dev.gtcl.reddit.Vote
import dev.gtcl.reddit.databinding.ItemPostBinding
import dev.gtcl.reddit.models.reddit.Post
import dev.gtcl.reddit.actions.PostActions
import dev.gtcl.reddit.databinding.LayoutPopupPostOptionsBinding

class PostVH private constructor(private val binding:ItemPostBinding)
    : RecyclerView.ViewHolder(binding.root) {
    fun bind(post: Post?, postActions: PostActions, hideAction: ((Int) -> Unit), postClicked: (Post) -> Unit){
        binding.post = post
        binding.executePendingBindings()
        binding.rootLayout.setOnClickListener {
            post?.isRead = true
            binding.invalidateAll()
            postClicked(post!!)
        }

        binding.thumbnail.setOnClickListener{
            post?.isRead = true
            binding.invalidateAll()
            postActions.thumbnailClicked(post!!)
        }

        if(post != null){
            binding.moreOptions.setOnClickListener {
                showPopupWindow(post, postActions, it, hideAction)
            }
        }
    }

    private fun showPopupWindow(post: Post, postActions: PostActions, anchorView: View, hideAction: ((Int) -> Unit)){
        val inflater = anchorView.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupBinding = LayoutPopupPostOptionsBinding.inflate(inflater)
        val popupWindow = PopupWindow(popupBinding.root, RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT, true)
        popupBinding.apply {
            this.post = post
            upvoteButton.root.setOnClickListener {
                postActions.vote(post, if(post.likes == true) Vote.UNVOTE else Vote.UPVOTE)
                post.likes = if(post.likes == true) {
                    null
                } else {
                    true
                }
                binding.invalidateAll()
                popupWindow.dismiss()
            }
            downvoteButton.root.setOnClickListener {
                postActions.vote(post, if(post.likes == false) Vote.UNVOTE else Vote.DOWNVOTE)
                post.likes = if(post.likes == false) {
                    null
                } else {
                    false
                }
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
                post.saved = !post.saved
                postActions.save(post)
                binding.invalidateAll()
                popupWindow.dismiss()
            }
            hideButton.root.setOnClickListener {
                post.hidden = !post.hidden
                postActions.hide(post, adapterPosition)
                hideAction(adapterPosition)
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

    companion object {
        fun create(parent: ViewGroup): PostVH {
            return PostVH(
                ItemPostBinding.inflate(
                    LayoutInflater.from(parent.context)
                )
            )
        }
    }
}
