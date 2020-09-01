package dev.gtcl.astro.ui.viewholders

import android.content.Context
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.PopupWindow
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import dev.gtcl.astro.Vote
import dev.gtcl.astro.actions.ItemClickListener
import dev.gtcl.astro.databinding.ItemPostBinding
import dev.gtcl.astro.actions.PostActions
import dev.gtcl.astro.databinding.PopupPostActionsBinding
import dev.gtcl.astro.models.reddit.listing.Post
import dev.gtcl.astro.showAsDropdown
import jp.wasabeef.glide.transformations.BlurTransformation

class PostVH private constructor(private val binding:ItemPostBinding)
    : RecyclerView.ViewHolder(binding.root) {

    fun bind(post: Post, postActions: PostActions, blurNsfw: Boolean, username: String?, itemClickListener: ItemClickListener) {
        binding.post = post

        binding.itemPostCardView.setOnClickListener{
            post.isRead = true
            binding.invalidateAll()
            itemClickListener.itemClicked(post, adapterPosition)
        }

        setThumbnail(post, blurNsfw, postActions)

        binding.itemPostMoreOptions.setOnClickListener {
            showPopupWindow(post, postActions, (post.author == username), it)
        }

        binding.executePendingBindings()
    }

    private fun setThumbnail(post: Post, blurNsfw: Boolean, postActions: PostActions){
        val thumbnailUrl = post.thumbnail
        if(thumbnailUrl != null && URLUtil.isValidUrl(thumbnailUrl) && Patterns.WEB_URL.matcher(thumbnailUrl).matches()){
            binding.itemPostThumbnailBackground.visibility = View.VISIBLE
            binding.itemPostThumbnail.setOnClickListener{
                post.isRead = true
                binding.invalidateAll()
                postActions.thumbnailClicked(post, adapterPosition)
            }

            Glide.with(binding.root.context)
                .load(thumbnailUrl).apply {
                    if((post.nsfw && blurNsfw)){
                        apply(RequestOptions.bitmapTransform(BlurTransformation()))
                    }
                }.into(binding.itemPostThumbnail)
        } else {
            binding.itemPostThumbnailBackground.visibility = View.GONE
        }
    }

    private fun showPopupWindow(post: Post, postActions: PostActions, createdFromUser: Boolean, anchor: View){
        val inflater = anchor.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupBinding = PopupPostActionsBinding.inflate(inflater)
        val popupWindow = PopupWindow()
        popupBinding.apply {
            this.post = post
            this.createdFromUser = createdFromUser
            if(createdFromUser){
                if(post.isSelf){
                    popupPostActionsEdit.root.setOnClickListener {
                        postActions.edit(post, adapterPosition)
                        popupWindow.dismiss()
                    }
                }
                popupPostActionsManage.root.setOnClickListener {
                    postActions.manage(post, adapterPosition)
                    popupWindow.dismiss()
                }
                popupPostActionsDelete.root.setOnClickListener {
                    postActions.delete(post, adapterPosition)
                    popupWindow.dismiss()
                }
            }
            popupPostActionsUpvote.root.setOnClickListener {
                postActions.vote(post, if(post.likes == true) Vote.UNVOTE else Vote.UPVOTE)
                binding.invalidateAll()
                popupWindow.dismiss()
            }
            popupPostActionsDownvote.root.setOnClickListener {
                postActions.vote(post, if(post.likes == false) Vote.UNVOTE else Vote.DOWNVOTE)
                binding.invalidateAll()
                popupWindow.dismiss()
            }
            popupPostActionsShare.root.setOnClickListener {
                postActions.share(post)
                popupWindow.dismiss()
            }
            popupPostActionsProfile.root.setOnClickListener {
                postActions.viewProfile(post)
                popupWindow.dismiss()
            }
            popupPostActionsSave.root.setOnClickListener {
                postActions.save(post)
                binding.invalidateAll()
                popupWindow.dismiss()
            }
            popupPostActionsHide.root.setOnClickListener {
                postActions.hide(post, adapterPosition)
                binding.invalidateAll()
                popupWindow.dismiss()
            }
            popupPostActionsSubreddit.root.setOnClickListener {
                postActions.subredditSelected(post.subreddit)
                popupWindow.dismiss()
            }
            popupPostActionsReport.root.setOnClickListener {
                postActions.report(post, adapterPosition)
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
