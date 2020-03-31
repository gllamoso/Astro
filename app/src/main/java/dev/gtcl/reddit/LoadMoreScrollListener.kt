package dev.gtcl.reddit

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LoadMoreScrollListener(private val mLayoutManager: GridLayoutManager, private val mOnLoadMoreListener: OnLoadMoreListener) : RecyclerView.OnScrollListener() {

    private val visibleThreshold = 2

    private var isLoading = false
    fun finishedLoading(){
        isLoading = false
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        if(dy <= 0) return

        val totalItemCount = mLayoutManager.itemCount
        val lastVisibleItem = mLayoutManager.findLastVisibleItemPosition()
        if(!isLoading && totalItemCount <= lastVisibleItem + visibleThreshold){
            mOnLoadMoreListener.loadMore()
            isLoading = true
        }
    }
}