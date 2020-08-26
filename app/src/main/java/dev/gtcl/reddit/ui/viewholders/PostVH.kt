package dev.gtcl.reddit.ui.viewholders

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
import dev.gtcl.reddit.Vote
import dev.gtcl.reddit.actions.ItemClickListener
import dev.gtcl.reddit.databinding.ItemPostBinding
import dev.gtcl.reddit.actions.PostActions
import dev.gtcl.reddit.databinding.PopupPostOptionsBinding
import dev.gtcl.reddit.models.reddit.listing.Post
import dev.gtcl.reddit.showAsDropdown
import jp.wasabeef.glide.transformations.BlurTransformation

class PostVH private constructor(private val binding:ItemPostBinding)
    : RecyclerView.ViewHolder(binding.root) {

    fun bind(post: Post, postActions: PostActions, blurNsfw: Boolean, blurSpoiler: Boolean, userId: String?, itemClickListener: ItemClickListener) {
        binding.post = post

        binding.cardView.setOnClickListener{
            post.isRead = true
            binding.invalidateAll()
            itemClickListener.itemClicked(post, adapterPosition)
        }

        setThumbnail(post, blurNsfw, blurSpoiler, postActions)

        binding.moreOptions.setOnClickListener {
            showPopupWindow(post, postActions, (post.authorFullName == userId && userId != null), it)
        }

        binding.executePendingBindings()
    }

    private fun setThumbnail(post: Post, blurNsfw: Boolean, blurSpoiler: Boolean, postActions: PostActions){
        val thumbnailUrl = post.thumbnail
        if(thumbnailUrl != null && URLUtil.isValidUrl(thumbnailUrl) && Patterns.WEB_URL.matcher(thumbnailUrl).matches()){
            binding.frameLayout.visibility = View.VISIBLE
            binding.thumbnail.setOnClickListener{
                post.isRead = true
                binding.invalidateAll()
                postActions.thumbnailClicked(post, adapterPosition)
            }

            Glide.with(binding.root.context)
                .load(thumbnailUrl).apply {
                    if((post.spoiler && blurSpoiler) || (post.nsfw && blurNsfw)){
                        apply(RequestOptions.bitmapTransform(BlurTransformation()))
                    }
                }.into(binding.thumbnail)
        } else {
            binding.frameLayout.visibility = View.GONE
        }
    }

    private fun showPopupWindow(post: Post, postActions: PostActions, createdFromUser: Boolean, anchor: View){
        val inflater = anchor.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupBinding = PopupPostOptionsBinding.inflate(inflater)
        val popupWindow = PopupWindow()
        popupBinding.apply {
            this.post = post
            this.createdFromUser = createdFromUser
            if(createdFromUser){
                if(post.isSelf){
                    editButton.root.setOnClickListener {
                        postActions.edit(post, adapterPosition)
                        popupWindow.dismiss()
                    }
                }
                manageButton.root.setOnClickListener {
                    postActions.manage(post, adapterPosition)
                    popupWindow.dismiss()
                }
                deleteButton.root.setOnClickListener {
                    postActions.delete(post, adapterPosition)
                    popupWindow.dismiss()
                }
            }
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
                binding.invalidateAll()
                popupWindow.dismiss()
            }
            subredditButton.root.setOnClickListener {
                postActions.subredditSelected(post.subreddit)
                popupWindow.dismiss()
            }
            reportButton.root.setOnClickListener {
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
