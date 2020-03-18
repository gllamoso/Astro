package dev.gtcl.reddit.ui.fragments.posts

interface MainFragmentViewPagerListener{
    fun enablePagerSwiping(enable: Boolean)
    fun navigateToPostList()
}