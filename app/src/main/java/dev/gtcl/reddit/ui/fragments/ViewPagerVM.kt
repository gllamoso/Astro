package dev.gtcl.reddit.ui.fragments

import androidx.lifecycle.*
import dev.gtcl.reddit.*

class ViewPagerVM(private val application: RedditApplication): AndroidViewModel(application){
    var isViewPagerSwipeEnabled = false
    var pages: ArrayList<ViewPagerPage>? = null

    private val _continueThread = MutableLiveData<String?>()

    val continueThread: LiveData<String?>
        get() = _continueThread

    fun continueThreadObserved(){
        _continueThread.value = null
    }

    fun continueThread(url: String){
        _continueThread.value = url
    }
}