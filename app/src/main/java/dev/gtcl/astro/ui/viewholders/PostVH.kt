package dev.gtcl.astro.ui.viewholders

import android.content.Context
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.PopupWindow
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import dev.gtcl.astro.GlideApp
import dev.gtcl.astro.Vote
import dev.gtcl.astro.actions.ItemClickListener
import dev.gtcl.astro.actions.PostActions
import dev.gtcl.astro.databinding.ItemPostBinding
import dev.gtcl.astro.databinding.PopupPostActionsBinding
import dev.gtcl.astro.models.reddit.listing.Post
import dev.gtcl.astro.showAsDropdown
import jp.wasabeef.glide.transformations.BlurTransformation

class PostVH private constructor(private val binding: ItemPostBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(
        post: Post,
        postActions: PostActions,
        blurNsfw: Boolean,
        username: String?,
        itemClickListener: ItemClickListener
    ) {

        binding.apply {
            this.post = post

            itemPostCardView.apply {
                setOnClickListener {
                    post.isRead = true
                    binding.invalidateAll()
                    itemClickListener.clicked(post, adapterPosition)
                }
                setOnLongClickListener {
                    itemClickListener.longClicked(post, adapterPosition)
                    true
                }
            }

            setThumbnail(post, blurNsfw, postActions)

            itemPostBottomPanel.apply {
                layoutPostBottomPanelUpvoteButton.setOnClickListener {
                    postActions.vote(post, if (post.likes == true) Vote.UNVOTE else Vote.UPVOTE)
                    binding.invalidateAll()
                }

                layoutPostBottomPanelDownvoteButton.setOnClickListener {
                    postActions.vote(post, if (post.likes == false) Vote.UNVOTE else Vote.DOWNVOTE)
                    binding.invalidateAll()
                }

                layoutPostBottomPanelSaveButton.setOnClickListener {
                    postActions.save(post)
                    binding.invalidateAll()
                }

                layoutPostBottomPanelShareButton.setOnClickListener {
                    postActions.share(post)
                }

                layoutPostBottomPanelMoreOptions.setOnClickListener {
                    showPopupWindow(post, postActions, (post.author == username), it)
                }
            }

            executePendingBindings()
            binding.itemPostBottomPanel.invalidateAll()

            // Force TextView to redraw and remeasure to prevent unintentional bottom padding
            itemPostTitle.invalidate()
            itemPostTitle.forceLayout()
        }

    }

    private fun setThumbnail(post: Post, blurNsfw: Boolean, postActions: PostActions) {
        val thumbnailUrl = post.thumbnailFormatted
        if (thumbnailUrl != null && URLUtil.isValidUrl(thumbnailUrl) && Patterns.WEB_URL.matcher(
                thumbnailUrl
            ).matches()
        ) {
            binding.itemPostThumbnailBackground.visibility = View.VISIBLE
            binding.itemPostThumbnail.setOnClickListener {
                post.isRead = true
                binding.invalidateAll()
                postActions.thumbnailClicked(post, adapterPosition)
            }

            GlideApp.with(binding.root.context)
                .load(thumbnailUrl).apply {
                    if ((post.nsfw && blurNsfw)) {
                        apply(RequestOptions.bitmapTransform(BlurTransformation()))
                    }
                }
                .thumbnail(0.5F)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(binding.itemPostThumbnail)
        } else {
            binding.itemPostThumbnailBackground.visibility = View.GONE
        }
    }

    private fun showPopupWindow(
        post: Post,
        postActions: PostActions,
        createdFromUser: Boolean,
        anchor: View
    ) {
        val inflater =
            anchor.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupBinding = PopupPostActionsBinding.inflate(inflater)
        val popupWindow = PopupWindow()
        popupBinding.apply {
            this.post = post
            this.createdFromUser = createdFromUser
            if (createdFromUser) {
                if (post.isSelf) {
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
            popupPostActionsProfile.root.setOnClickListener {
                postActions.viewProfile(post)
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

        popupWindow.showAsDropdown(
            anchor,
            popupBinding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            popupBinding.root.measuredHeight
        )
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
