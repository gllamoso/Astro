package dev.gtcl.astro.ui.fragments

import androidx.lifecycle.*

class ViewPagerVM: ViewModel(){
    var isViewPagerSwipeEnabled = false
    var pages: MutableList<ViewPagerPage> = mutableListOf()

    private val _navigateToPreviousPage = MutableLiveData<Any?>()
    val navigateToPreviousPage: LiveData<Any?>
        get() = _navigateToPreviousPage

    private val _swipingEnabled = MutableLiveData<Boolean>().apply { value = false }
    val swipeEnabled: LiveData<Boolean>
        get() = _swipingEnabled

    private val _linkClicked = MutableLiveData<String?>()
    val linkClicked: LiveData<String?>
        get() = _linkClicked

    private val _notifyViewPager = MutableLiveData<Any?>()
    val notifyViewPager: LiveData<Any?>
        get() = _notifyViewPager

    private val _newPostLink = MutableLiveData<String?>()
    val newPostLink: LiveData<String?>
        get() = _newPostLink

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

    fun newPost(link: String){
        _newPostLink.value = link
    }

    fun newPostObserved(){
        _newPostLink.value = null
    }

    fun linkObserved(){
        _linkClicked.value = null
    }

    fun notifyViewPager(){
        _notifyViewPager.value = Any()
    }

    fun notifyViewPagerObserved(){
        _notifyViewPager.value = null
    }
}