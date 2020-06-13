package dev.gtcl.reddit.ui.fragments

import androidx.lifecycle.*
import dev.gtcl.reddit.*

class ViewPagerVM(private val application: RedditApplication): AndroidViewModel(application){
    var isViewPagerSwipeEnabled = false
    var pages: ArrayList<PageType>? = null
}