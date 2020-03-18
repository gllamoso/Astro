package dev.gtcl.reddit.ui.fragments.account.saved

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import dev.gtcl.reddit.databinding.FragmentSimpleRecyclerViewBinding
import dev.gtcl.reddit.network.Post
import dev.gtcl.reddit.ui.fragments.account.UserFragment
import dev.gtcl.reddit.ui.fragments.posts.listing.ListingAdapter
import dev.gtcl.reddit.ui.fragments.posts.listing.PostViewClickListener

class UserSavedFragment : Fragment() {
    private lateinit var binding: FragmentSimpleRecyclerViewBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSimpleRecyclerViewBinding.inflate(inflater)
        setRecyclerViewAdapter()
        return binding.root
    }

    private fun setRecyclerViewAdapter(){
        val model = (requireParentFragment() as UserFragment).model

        val adapter = ListingAdapter({ Log.d("TAE", "retry callback") }, object : PostViewClickListener {
            override fun onPostClicked(post: Post?, position: Int) {
                TODO("Not yet implemented")
            }

            override fun onThumbnailClicked(post: Post) {
                TODO("Not yet implemented")
            }

        })
        binding.list.adapter = adapter

        model.savedPosts.observe(viewLifecycleOwner, Observer {
            adapter.submitList(it)
        })

    }
}