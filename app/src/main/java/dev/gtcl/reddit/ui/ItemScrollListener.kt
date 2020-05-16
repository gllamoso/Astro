package dev.gtcl.reddit.ui

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ItemScrollListener(private val visibleThreshold: Int = 15, private val mLayoutManager: GridLayoutManager, private val loadMore: () -> Unit) : RecyclerView.OnScrollListener() {

    private var isLoading = false
    fun finishedLoading(){
        isLoading = false
    }

    private var hasReachedLastItem = false
    fun lastItemReached(){
        hasReachedLastItem = true
    }

    fun reset(){
        hasReachedLastItem = false
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        if(dy <= 0 || hasReachedLastItem) return

        val totalItemCount = mLayoutManager.itemCount
        val lastVisibleItem = mLayoutManager.findLastVisibleItemPosition()
        if(!isLoading && totalItemCount <= lastVisibleItem + visibleThreshold){
            loadMore()
            isLoading = true
        }
    }
}