package dev.gtcl.reddit.ui

import android.util.Log
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LoadMoreScrollListener(private val mLayoutManager: GridLayoutManager, private val onLoadMore: () -> Unit) : RecyclerView.OnScrollListener() {

    private val visibleThreshold = 15

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
            onLoadMore()
            isLoading = true
        }
    }
}