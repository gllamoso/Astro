package dev.gtcl.reddit.ui.viewholders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.R
import dev.gtcl.reddit.Vote
import dev.gtcl.reddit.databinding.ItemPostBinding
import dev.gtcl.reddit.listings.Post
import dev.gtcl.reddit.ui.MenuItem
import dev.gtcl.reddit.ui.OPTIONS_SIZE
import dev.gtcl.reddit.ui.PostActions
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
            postActions.thumbnailClicked(post!!)
        }


        binding.moreOptions.apply {
            val optionsAdapter = PostOptionsAdapter(binding.root.context, post!!)
            adapter = optionsAdapter
            setSelection(OPTIONS_SIZE - 1)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                override fun onNothingSelected(parent: AdapterView<*>?) {}

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    when(position){
                        MenuItem.UPVOTE.position -> {
                            binding.post?.let {
                                postActions.vote(it, if(it.likes == true) Vote.UNVOTE else Vote.UPVOTE)
                                it.likes = if(it.likes == true) null else true
                                binding.invalidateAll()
                            }
                        }
                        MenuItem.DOWNVOTE.position -> {
                            binding.post?.let {
                                postActions.vote(it, if(it.likes == false) Vote.UNVOTE else Vote.DOWNVOTE)
                                it.likes = if(it.likes == false) null else false
                                binding.invalidateAll()
                            }
                        }
                        MenuItem.SHARE.position -> postActions.share(post)
                        MenuItem.PROFILE.position -> postActions.viewProfile(post)
                        MenuItem.AWARD.position -> postActions.award(post)
                        MenuItem.SAVE.position -> {
                            binding.post?.let {
                                postActions.save(it)
                                it.saved = !it.saved
                                binding.invalidateAll()
                            }
                        }
                        MenuItem.HIDE.position -> {
                            binding.post?.let{
                                postActions.hide(post)
                                it.hidden = !it.hidden
                                hide()
                            }
                        }
                        MenuItem.REPORT.position -> postActions.report(post)
                    }
                    this@apply.setSelection(OPTIONS_SIZE - 1)
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