package dev.gtcl.reddit.ui.fragments

import androidx.core.widget.NestedScrollView

class ListingScrollListener(private val visibleThreshold: Int = 2000, private val loadMore: () -> Unit) : NestedScrollView.OnScrollChangeListener{

    private var isLoading = false
    fun finishedLoading(){
        isLoading = false
    }

    override fun onScrollChange(v: NestedScrollView?, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int) {
        if(scrollY > oldScrollY){
            if(v == null) {
                return
            }
            if ((scrollY + visibleThreshold >= (v.getChildAt(v.childCount - 1).measuredHeight - v.measuredHeight)) // gets recyclerview
                && v.getChildAt(v.childCount - 1) != null
                && !isLoading) {
                loadMore()
                isLoading = true
            }
        }
    }

}