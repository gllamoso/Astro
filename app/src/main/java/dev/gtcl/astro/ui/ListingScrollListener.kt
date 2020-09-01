package dev.gtcl.astro.ui

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ListingScrollListener(private val visibleThreshold: Int = 15, private val layoutManager: GridLayoutManager, private val loadMore: () -> Unit) : RecyclerView.OnScrollListener() {

    private var isLoading = false
    fun finishedLoading(){
        isLoading = false
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        if(dy <= 0 || isLoading) return

        val totalItemCount = layoutManager.itemCount
        val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
        if(totalItemCount <= lastVisibleItem + visibleThreshold){
            loadMore()
            isLoading = true
        }
    }


}