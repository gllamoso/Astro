package dev.gtcl.astro.ui.fragments.manage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.astro.AstroApplication
import dev.gtcl.astro.AstroViewModel
import dev.gtcl.astro.models.reddit.listing.Flair

class ManagePostVM(application: AstroApplication) : AstroViewModel(application) {

    private val _flair = MutableLiveData<Flair?>().apply { value = null }
    val flair: LiveData<Flair?>
        get() = _flair

    fun selectFlair(flair: Flair?) {
        _flair.value = flair
    }
}