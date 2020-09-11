package dev.gtcl.astro.ui.fragments.view_pager

import androidx.lifecycle.*
import dev.gtcl.astro.AstroApplication
import dev.gtcl.astro.AstroViewModel

class ViewPagerVM(application: AstroApplication) : AstroViewModel(application) {
    var isViewPagerSwipeEnabled = false
    val pages = mutableListOf<ViewPagerPage>()

    private val _navigateToPreviousPage = MutableLiveData<Any?>()
    val navigateToPreviousPage: LiveData<Any?>
        get() = _navigateToPreviousPage

    private val _swipingEnabled = MutableLiveData<Boolean>().apply { value = false }
    val swipeEnabled: LiveData<Boolean>
        get() = _swipingEnabled

    private val _linkClicked = MutableLiveData<String?>()
    val linkClicked: LiveData<String?>
        get() = _linkClicked

    private val _syncViewPager = MutableLiveData<Any?>()
    val syncViewPager: LiveData<Any?>
        get() = _syncViewPager

    private val _newPostLink = MutableLiveData<String?>()
    val newPostLink: LiveData<String?>
        get() = _newPostLink

    fun navigateToPreviousPage() {
        _navigateToPreviousPage.value = Any()
    }

    fun navigateToPreviousPageObserved() {
        _navigateToPreviousPage.value = null
    }

    fun swipingEnabled(enabled: Boolean) {
        _swipingEnabled.value = enabled
    }

    fun linkClicked(link: String) {
        _linkClicked.value = link
    }

    fun newPost(link: String) {
        _newPostLink.value = link
    }

    fun newPostObserved() {
        _newPostLink.value = null
    }

    fun linkObserved() {
        _linkClicked.value = null
    }

    fun syncViewPager() {
        _syncViewPager.value = Any()
    }

    fun notifyViewPagerObserved() {
        _syncViewPager.value = null
    }
}