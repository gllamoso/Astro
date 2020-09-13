package dev.gtcl.astro.ui.fragments.media.list.item

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.astro.*
import dev.gtcl.astro.models.reddit.MediaURL
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

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

    fun setMedia(mediaURL: MediaURL, playWhenReady: Boolean) {
        coroutineScope.launch {
            _isLoading.postValue(true)
            if (mediaURL.mediaType == MediaType.VIDEO && !playWhenReady) {
                _mediaUrl.postValue(
                    MediaURL(
                        mediaURL.url,
                        MediaType.VIDEO_PREVIEW,
                        mediaURL.backupUrl,
                        mediaURL.thumbnail
                    )
                )
            } else{
                _mediaUrl.postValue(mediaURL)
            }
        }
    }

    fun setLoadingState(loading: Boolean) {
        _isLoading.value = loading
    }
}