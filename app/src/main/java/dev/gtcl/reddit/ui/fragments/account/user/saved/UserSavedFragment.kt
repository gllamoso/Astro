package dev.gtcl.reddit.ui.fragments.account.user.saved

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import dev.gtcl.reddit.databinding.FragmentSimpleRecyclerViewBinding
import dev.gtcl.reddit.ui.PostActions
import dev.gtcl.reddit.ui.fragments.account.user.UserFragment
import dev.gtcl.reddit.ui.fragments.home.listing.ListingAdapter

class UserSavedFragment : Fragment() {
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

        val adapter2 = ListingAdapter({ model.retrySaved() }, postActions)
        binding.list.adapter = adapter2

        model.savedPosts.observe(viewLifecycleOwner, Observer {
            adapter2.submitList(it)
        })

    }
}