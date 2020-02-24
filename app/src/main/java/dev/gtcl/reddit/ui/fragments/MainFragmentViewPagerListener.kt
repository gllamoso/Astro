package dev.gtcl.reddit.ui.fragments

interface MainFragmentViewPagerListener{
    fun enablePagerSwiping(enable: Boolean)
    fun navigateToPostList()
}