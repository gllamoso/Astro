package dev.gtcl.reddit.ui.fragments

import androidx.lifecycle.*
import dev.gtcl.reddit.*

class ViewPagerVM(private val application: RedditApplication): AndroidViewModel(application){
    var isViewPagerSwipeEnabled = false
    var pages: ArrayList<ViewPagerPage>? = null


    private val _newPage = MutableLiveData<ViewPagerPage?>()
    val newPage: LiveData<ViewPagerPage?>
        get() = _newPage

    fun newPage(page: ViewPagerPage){
        _newPage.value = page
    }

    fun newPageObserved(){
        _newPage.value = null
    }
}