package dev.gtcl.astro.ui.fragments.create_post

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import dev.gtcl.astro.ui.fragments.create_post.type.CreatePostImageFragment
import dev.gtcl.astro.ui.fragments.create_post.type.CreatePostLinkFragment
import dev.gtcl.astro.ui.fragments.create_post.type.CreatePostTextFragment

class CreatePostStateAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
    FragmentStateAdapter(fragmentManager, lifecycle) {

    private val fragments =
        listOf(CreatePostTextFragment(), CreatePostImageFragment(), CreatePostLinkFragment())

    override fun getItemCount() = fragments.size

    override fun createFragment(position: Int) = fragments[position]
}