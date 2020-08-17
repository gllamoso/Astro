package dev.gtcl.reddit.ui.fragments

import androidx.lifecycle.*
import dev.gtcl.reddit.*

class ViewPagerVM(private val application: RedditApplication): AndroidViewModel(application){
    var isViewPagerSwipeEnabled = false
    var pages: MutableList<ViewPagerPage> = mutableListOf()

    private val _newPage = MutableLiveData<ViewPagerPage?>()
    val newPage: LiveData<ViewPagerPage?>
        get() = _newPage

    private val _navigateToPreviousPage = MutableLiveData<Any?>()
    val navigateToPreviousPage: LiveData<Any?>
        get() = _navigateToPreviousPage

    private val _swipingEnabled = MutableLiveData<Boolean>().apply { value = false }
    val swipeEnabled: LiveData<Boolean>
        get() = _swipingEnabled

    fun newPage(page: ViewPagerPage){
        _newPage.value = page
        pages.add(page)
    }

    private val _linkClicked = MutableLiveData<String?>()
    val linkClicked: LiveData<String?>
        get() = _linkClicked

    fun newPageObserved(){
        _newPage.value = null
    }

    fun navigateToPreviousPage(){
        _navigateToPreviousPage.value = Any()
    }

    fun navigateToPreviousPageObserved(){
        _navigateToPreviousPage.value = null
    }

    fun swipingEnabled(enabled: Boolean){
        _swipingEnabled.value = enabled
    }

    fun linkClicked(link: String){
        _linkClicked.value = link
    }

    fun linkObserved(){
        _linkClicked.value = null
    }
}