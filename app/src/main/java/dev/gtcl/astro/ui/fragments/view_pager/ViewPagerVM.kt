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

    private val _syncViewPager = MutableLiveData<Any?>()
    val syncViewPager: LiveData<Any?>
        get() = _syncViewPager

    fun navigateToPreviousPage() {
        _navigateToPreviousPage.value = Any()
    }

    fun navigateToPreviousPageObserved() {
        _navigateToPreviousPage.value = null
    }

    fun swipingEnabled(enabled: Boolean) {
        _swipingEnabled.value = enabled
    }

    fun syncViewPager() {
        _syncViewPager.value = Any()
    }

    fun notifyViewPagerObserved() {
        _syncViewPager.value = null
    }
}