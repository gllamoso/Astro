package dev.gtcl.astro.ui.fragments.manage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.gtcl.astro.models.reddit.listing.Flair

class ManagePostVM: ViewModel(){

    private val _flair = MutableLiveData<Flair?>().apply { value = null }
    val flair: LiveData<Flair?>
        get() = _flair

    fun selectFlair(flair: Flair?){
        _flair.value = flair
    }
}