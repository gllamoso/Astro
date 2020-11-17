package dev.gtcl.astro.ui.fragments.media.list.item

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.astro.AstroApplication
import dev.gtcl.astro.AstroViewModel
import dev.gtcl.astro.models.reddit.MediaURL
import dev.gtcl.astro.url.MediaType

class MediaVM(application: AstroApplication) : AstroViewModel(application) {

    var playWhenReady = true
    var currentWindow = 0
    var playbackPosition = 0L

    private val _mediaUrl = MutableLiveData<MediaURL?>().apply { value = null }
    val mediaURL: LiveData<MediaURL?>
        get() = _mediaUrl

    private val _isLoading = MutableLiveData<Boolean>().apply { value = false }
    val isLoading: LiveData<Boolean>
        get() = _isLoading

    private val _hasFailed = MutableLiveData<Boolean>().apply { value = false }
    val hasFailed: LiveData<Boolean>
        get() = _hasFailed

    fun setMedia(mediaURL: MediaURL, playWhenReady: Boolean) {
        _isLoading.value = true
        _hasFailed.value = false
        if (mediaURL.mediaType == MediaType.VIDEO && !playWhenReady) {
            _mediaUrl.postValue(
                MediaURL(
                    mediaURL.url,
                    MediaType.VIDEO_PREVIEW,
                    mediaURL.backupUrl,
                    mediaURL.thumbnail
                )
            )
        } else {
            _mediaUrl.postValue(mediaURL)
        }
    }

    fun setLoadingState(loading: Boolean) {
        _isLoading.value = loading
    }

    fun hasFailed(failed: Boolean) {
        _hasFailed.value = failed
    }


}