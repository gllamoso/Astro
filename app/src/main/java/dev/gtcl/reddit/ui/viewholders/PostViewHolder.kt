package dev.gtcl.reddit.ui.viewholders

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.R
import dev.gtcl.reddit.Vote
import dev.gtcl.reddit.databinding.ItemPostBinding
import dev.gtcl.reddit.models.reddit.Post
import dev.gtcl.reddit.ui.PostMenuItem
import dev.gtcl.reddit.ui.POST_OPTIONS_SIZE
import dev.gtcl.reddit.actions.PostActions
import dev.gtcl.reddit.ui.PostOptionsAdapter

class PostViewHolder private constructor(private val binding:ItemPostBinding)
    : RecyclerView.ViewHolder(binding.root) {
    fun bind(post: Post?, postActions: PostActions, isRead: Boolean, hide: () -> Unit){
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

        binding.moreOptions.apply {
            adapter = PostOptionsAdapter(binding.root.context, post!!)
            setSelection(POST_OPTIONS_SIZE - 1)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                override fun onNothingSelected(parent: AdapterView<*>?) {}

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    when(position){
                        PostMenuItem.UPVOTE.position -> {
                            binding.post?.let {
                                postActions.vote(it, if(it.likes == true) Vote.UNVOTE else Vote.UPVOTE)
                                it.likes = if(it.likes == true) null else true
                                binding.invalidateAll()
                            }
                        }
                        PostMenuItem.DOWNVOTE.position -> {
                            binding.post?.let {
                                postActions.vote(it, if(it.likes == false) Vote.UNVOTE else Vote.DOWNVOTE)
                                it.likes = if(it.likes == false) null else false
                                binding.invalidateAll()
                            }
                        }
                        PostMenuItem.SHARE.position -> postActions.share(post)
                        PostMenuItem.PROFILE.position -> postActions.viewProfile(post)
                        PostMenuItem.SAVE.position -> {
                            binding.post?.let {
                                postActions.save(it)
                                it.saved = !it.saved
                                binding.invalidateAll()
                            }
                        }
                        PostMenuItem.HIDE.position -> {
                            binding.post?.let{
                                postActions.hide(post)
                                it.hidden = !it.hidden
                                hide()
                            }
                        }
                        PostMenuItem.REPORT.position -> postActions.report(post)
                    }
                    this@apply.setSelection(POST_OPTIONS_SIZE - 1)
                }

            }
        }
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
