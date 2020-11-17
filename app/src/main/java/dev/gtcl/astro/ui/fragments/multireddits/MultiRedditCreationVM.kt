package dev.gtcl.astro.ui.fragments.multireddits

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.astro.AstroApplication
import dev.gtcl.astro.AstroViewModel
import dev.gtcl.astro.R
import dev.gtcl.astro.getErrorMessage
import dev.gtcl.astro.models.reddit.listing.MultiReddit
import dev.gtcl.astro.models.reddit.listing.MultiRedditUpdate
import kotlinx.coroutines.launch
import retrofit2.HttpException

class MultiRedditCreationVM(private val application: AstroApplication) :
    AstroViewModel(application) {

    private val _isLoading = MutableLiveData<Boolean>().apply { value = false }
    val isLoading: LiveData<Boolean>
        get() = _isLoading

    private val _newMulti = MutableLiveData<MultiReddit?>()
    val newMulti: LiveData<MultiReddit?>
        get() = _newMulti

    private val _title = MutableLiveData<String>()
    val title: LiveData<String>
        get() = _title

    private val _isCopying = MutableLiveData<Boolean>()
    val isCopying: LiveData<Boolean>
        get() = _isCopying

    fun setModel(multiReddit: MultiReddit?, isCopying: Boolean) {
        _title.value =
            multiReddit?.displayName ?: application.getString(R.string.create_custom_feed)
        _isCopying.value = isCopying
    }

    fun newMultiObserved() {
        _newMulti.value = null
    }

    fun copyMulti(
        path: String,
        displayName: String,
        descriptionMd: String?
    ) {
        coroutineScope.launch {
            try {
                _isLoading.postValue(true)
                val multi =
                    subredditRepository.copyMulti(path, displayName, descriptionMd).await().data
                subredditRepository.insertMultiReddit(multi)
                _newMulti.postValue(multi)
            } catch (e: Exception) {
                _errorMessage.postValue(e.getErrorMessage(application))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun createMulti(model: MultiRedditUpdate) {
        coroutineScope.launch {
            try {
                _isLoading.postValue(true)
                val multi = subredditRepository.createMulti(model).await().data
                subredditRepository.insertMultiReddit(multi)
                _newMulti.postValue(multi)
            } catch (e: Exception) {
                _errorMessage.postValue(
                    if (e is HttpException && e.code() == 409) {
                        application.getString(R.string.conflict_error)
                    } else {
                        e.getErrorMessage(application)
                    }
                )
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun updateMultiReddit(multipath: String, model: MultiRedditUpdate) {
        coroutineScope.launch {
            try {
                _isLoading.postValue(true)
                val multi = subredditRepository.updateMulti(multipath, model).await().data
                subredditRepository.insertMultiReddit(multi)
                _newMulti.postValue(multi)
            } catch (e: Exception) {
                _errorMessage.postValue(e.getErrorMessage(application))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}