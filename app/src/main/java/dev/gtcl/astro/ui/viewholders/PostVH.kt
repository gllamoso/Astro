package dev.gtcl.astro.ui.viewholders

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.PopupWindow
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import dev.gtcl.astro.GlideApp
import dev.gtcl.astro.R
import dev.gtcl.astro.Vote
import dev.gtcl.astro.actions.ItemClickListener
import dev.gtcl.astro.actions.PostActions
import dev.gtcl.astro.databinding.ItemPostBinding
import dev.gtcl.astro.databinding.PopupPostActionsBinding
import dev.gtcl.astro.html.toDp
import dev.gtcl.astro.models.reddit.listing.Post
import dev.gtcl.astro.showAsDropdown

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
                    itemClickListener.itemClicked(post, adapterPosition)
                    binding.invalidateAll()
                }
                setOnLongClickListener {
                    itemClickListener.itemLongClicked(post, adapterPosition)
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
            itemPostPostInfo1.invalidate()
            itemPostPostInfo1.forceLayout()
        }

    }

    private fun setThumbnail(post: Post, blurNsfw: Boolean, postActions: PostActions) {
        val thumbnailUrl = if(post.nsfw) {
            post.getThumbnail(blurNsfw)
        } else {
            post.getThumbnail(false)
        }

        val thumbnailIsValid = thumbnailUrl != null && Patterns.WEB_URL.matcher(thumbnailUrl).matches() && URLUtil.isValidUrl(thumbnailUrl)

        when{
            !thumbnailIsValid && post.isSelf -> binding.itemPostThumbnailBackground.visibility = View.GONE
            thumbnailIsValid && !post.spoiler -> {
                binding.itemPostThumbnailBackground.visibility = View.VISIBLE
                binding.itemPostThumbnail.apply {
                    setBackgroundColor(Color.TRANSPARENT)
                    setImageResource(android.R.color.transparent)
                    setOnClickListener {
                        postActions.thumbnailClicked(post, adapterPosition)
                        binding.invalidateAll()
                    }
                    setPadding(0, 0, 0, 0)

                    val thumbnailSize = 72.toDp(context)
                    GlideApp.with(context)
                            .load(thumbnailUrl)
                            .apply(RequestOptions().override(thumbnailSize, thumbnailSize))
                            .skipMemoryCache(true)
                            .centerCrop()
                            .addListener(object: RequestListener<Drawable> {
                                override fun onResourceReady(resource: Drawable?, model: Any?, target: com.bumptech.glide.request.target.Target<Drawable>?, dataSource: com.bumptech.glide.load.DataSource?, isFirstResource: Boolean): Boolean {
                                    if(resource is GifDrawable){
                                        setImageBitmap(resource.firstFrame)
                                        return true
                                    }
                                    return false
                                }

                                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                                    return false
                                }
                             })
                            .into(this)
                }
            }
            else -> {
                binding.itemPostThumbnailBackground.visibility = View.VISIBLE
                val resource = if(post.spoiler){
                    R.drawable.ic_error_outline_24
                } else {
                    R.drawable.ic_no_photo_24
                }
                binding.itemPostThumbnail.apply {
                    setBackgroundColor(Color.GRAY)
                    setImageResource(resource)
                    setOnClickListener {
                        postActions.thumbnailClicked(post, adapterPosition)
                        binding.invalidateAll()
                    }
                    val padding = 12.toDp(context)
                    setPadding(padding, padding, padding, padding)
                }

            }
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
