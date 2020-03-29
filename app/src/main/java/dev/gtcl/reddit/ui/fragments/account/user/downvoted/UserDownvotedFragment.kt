package dev.gtcl.reddit.ui.fragments.account.user.downvoted

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import dev.gtcl.reddit.databinding.FragmentSimpleRecyclerViewBinding
import dev.gtcl.reddit.network.Post
import dev.gtcl.reddit.ui.PostActions
import dev.gtcl.reddit.ui.ViewPagerActions
import dev.gtcl.reddit.ui.fragments.account.user.UserFragment
import dev.gtcl.reddit.ui.fragments.posts.listing.ListingAdapter

class UserDownvotedFragment : Fragment() {
    private lateinit var binding: FragmentSimpleRecyclerViewBinding

    private lateinit var postActions: PostActions
    fun setPostActions(postActions: PostActions){
        this.postActions = postActions
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSimpleRecyclerViewBinding.inflate(inflater)
        setRecyclerViewAdapter()
        return binding.root
    }

    private fun setRecyclerViewAdapter(){
        val model = (requireParentFragment() as UserFragment).model

        val adapter = ListingAdapter({ model.retryDownvoted() }, postActions)
        binding.list.adapter = adapter

        model.downvotedPosts.observe(viewLifecycleOwner, Observer {
            adapter.submitList(it)
        })

    }
}