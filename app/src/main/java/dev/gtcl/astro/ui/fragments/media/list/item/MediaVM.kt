package dev.gtcl.astro.ui.fragments.media.list.item

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import dev.gtcl.astro.MediaType
import dev.gtcl.astro.AstroApplication
import dev.gtcl.astro.AstroViewModel
import dev.gtcl.astro.buildMediaSource
import dev.gtcl.astro.models.reddit.MediaURL
import kotlinx.coroutines.launch

class MediaVM(private val application: AstroApplication): AstroViewModel(application){

    private val _mediaUrl = MutableLiveData<MediaURL>()
    val mediaURL: LiveData<MediaURL>
        get() = _mediaUrl

    private val _isLoading = MutableLiveData<Boolean>().apply { value = true }
    val isLoading: LiveData<Boolean>
        get() = _isLoading

    private val _player = MutableLiveData<SimpleExoPlayer?>()
    val player: LiveData<SimpleExoPlayer?>
        get() = _player

    private var _initialized = false
    val initialized: Boolean
        get() = _initialized

    fun setMedia(mediaURL: MediaURL){
        coroutineScope.launch {
            _isLoading.value = true
            _mediaUrl.value = mediaURL
            if(mediaURL.mediaType == MediaType.GFYCAT || mediaURL.mediaType == MediaType.VIDEO){
                val trackSelector = DefaultTrackSelector()
                trackSelector.setParameters(trackSelector.buildUponParameters().setMaxVideoSizeSd())
                var videoUri: Uri = Uri.parse(mediaURL.url)
                if(mediaURL.mediaType == MediaType.GFYCAT || mediaURL.mediaType == MediaType.REDGIFS){
                    try{
                        val videoUrl = if(mediaURL.mediaType == MediaType.GFYCAT) {
                            gfycatRepository
                                .getGfycatInfo(mediaURL.url.replace("http[s]?://gfycat.com/".toRegex(), ""))
                                .await()
                                .gfyItem
                                .mobileUrl
                        } else {
                            gfycatRepository.getGfycatInfoFromRedgifs(
                                mediaURL.url.replace("http[s]?://(www\\.)?redgifs.com/watch/".toRegex(), ""))
                                .await()
                                .gfyItem
                                .mobileUrl
                        }
                        videoUri = Uri.parse(videoUrl)
                    } catch (e: Exception){
                        if(mediaURL.backupUrl != null){
                            videoUri = Uri.parse(mediaURL.backupUrl)
                        } else {
                            _errorMessage.value = e.toString()
                        }
                    }
                }
                var mediaSource = buildMediaSource(application.baseContext, videoUri)
                val player = ExoPlayerFactory.newSimpleInstance(application.baseContext, trackSelector)
                player!!.apply {
                    repeatMode = Player.REPEAT_MODE_ONE
                    playWhenReady = false
                    seekTo(0, 0)
                    prepare(mediaSource, false, false)
                    addListener(object: Player.EventListener{
                        override fun onPlayerError(error: ExoPlaybackException?) {
                            Log.e("Media", "Exception: $error")
                            if(videoUri.path != mediaURL.backupUrl && mediaURL.backupUrl != null) {
                                mediaSource = buildMediaSource(application.baseContext, Uri.parse(mediaURL.backupUrl))
                                prepare(mediaSource, false, false)
                            }
                        }
                    })
                }
                _player.value = player
            }
            _initialized = true
        }
    }

    fun setLoadingState(loading: Boolean){
        _isLoading.value = loading
    }

    override fun onCleared() {
        super.onCleared()
        _player.value?.release()
        _player.value = null
    }
}