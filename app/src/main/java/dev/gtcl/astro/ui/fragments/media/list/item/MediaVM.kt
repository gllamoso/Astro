package dev.gtcl.astro.ui.fragments.media.list.item

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import dev.gtcl.astro.*
import dev.gtcl.astro.models.reddit.MediaURL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class MediaVM(private val application: AstroApplication) : AstroViewModel(application) {

    private val _mediaUrl = MutableLiveData<MediaURL>()
    val mediaURL: LiveData<MediaURL>
        get() = _mediaUrl

    private val _isLoading = MutableLiveData<Boolean>().apply { value = true }
    val isLoading: LiveData<Boolean>
        get() = _isLoading

    private val _player = MutableLiveData<SimpleExoPlayer?>()
    val player: LiveData<SimpleExoPlayer?>
        get() = _player

    var initialized = false

    fun setMedia(mediaURL: MediaURL, playWhenReady: Boolean) {
        coroutineScope.launch {
            _isLoading.postValue(true)
            _mediaUrl.postValue(mediaURL)
            if (mediaURL.mediaType == MediaType.GFYCAT || mediaURL.mediaType == MediaType.VIDEO) {
                val trackSelector = DefaultTrackSelector()
                trackSelector.setParameters(trackSelector.buildUponParameters().setMaxVideoSizeSd())
                var videoUri: Uri = Uri.parse(mediaURL.url)
                if (mediaURL.mediaType == MediaType.GFYCAT || mediaURL.mediaType == MediaType.REDGIFS) {
                    try {
                        val videoUrl = if (mediaURL.mediaType == MediaType.GFYCAT) {
                            val id = GFYCAT_REGEX.getIdFromUrl(mediaURL.url)!!
                            gfycatRepository
                                .getGfycatInfo(id)
                                .await()
                                .gfyItem
                                .mobileUrl
                        } else {
                            val id = REDGIFS_REGEX.getIdFromUrl(mediaURL.url)!!
                            gfycatRepository.getGfycatInfoFromRedgifs(id)
                                .await()
                                .gfyItem
                                .mobileUrl
                        }
                        videoUri = Uri.parse(videoUrl)
                    } catch (e: Exception) {
                        if (mediaURL.backupUrl != null) {
                            videoUri = Uri.parse(mediaURL.backupUrl)
                        } else {
                            _errorMessage.postValue(e.getErrorMessage(application))
                        }
                    }
                }
                var mediaSource = buildMediaSource(application.baseContext, videoUri)
                withContext(Dispatchers.Main) {
                    val player =
                        ExoPlayerFactory.newSimpleInstance(application.baseContext, trackSelector)
                    (player ?: return@withContext).apply {
                        repeatMode = Player.REPEAT_MODE_ONE
                        this.playWhenReady = playWhenReady
                        seekTo(0, 0)
                        prepare(mediaSource, false, false)
                        addListener(object : Player.EventListener {
                            override fun onPlayerError(error: ExoPlaybackException?) {
                                Timber.tag("Media").d("Exception $error")
                                if (videoUri.path != mediaURL.backupUrl && mediaURL.backupUrl != null) {
                                    mediaSource = buildMediaSource(
                                        application.baseContext,
                                        Uri.parse(mediaURL.backupUrl)
                                    )
                                    prepare(mediaSource, false, false)
                                }
                            }
                        })
                    }
                    _player.value = player
                }
            }
            initialized = true
        }
    }

    fun setLoadingState(loading: Boolean) {
        _isLoading.value = loading
    }

    fun pausePlayer() {
        _player.value?.playWhenReady = false
    }

    fun releasePlayer() {
        _player.value?.release()
        _player.value = null
    }

    override fun onCleared() {
        super.onCleared()
        _player.value?.release()
        _player.value = null
    }
}