package dev.gtcl.reddit.ui.fragments.media.list.item

import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerControlView
import dev.gtcl.reddit.MediaType
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.buildMediaSource
import dev.gtcl.reddit.models.reddit.MediaURL
import dev.gtcl.reddit.repositories.GfycatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class MediaVM(private val application: RedditApplication): AndroidViewModel(application){

    private val gfycatRepository = GfycatRepository.getInstance()

    // Scopes
    private var viewModelJob = Job()
    private var coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _mediaUrl = MutableLiveData<MediaURL>()
    val mediaURL: LiveData<MediaURL>
        get() = _mediaUrl

    private val _isLoading = MutableLiveData<Boolean>().apply { value = true }
    val isLoading: LiveData<Boolean>
        get() = _isLoading

    private val _player = MutableLiveData<SimpleExoPlayer?>()
    val player: LiveData<SimpleExoPlayer?>
        get() = _player

    private val _playerControllerView = MutableLiveData<WeakReference<PlayerControlView>?>()
    val playerControllerView: LiveData<WeakReference<PlayerControlView>?>
        get() = _playerControllerView

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?>
        get() = _errorMessage

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
                if(mediaURL.mediaType == MediaType.GFYCAT){
                    try{
                        val videoUrl = gfycatRepository
                            .getGfycatInfo(mediaURL.url.replace("http[s]?://gfycat.com/".toRegex(), ""))
                            .await()
                            .gfyItem
                            .mp4Url
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
                    playWhenReady = true
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

    fun pausePlayer(){
        _player.value?.let{0
            it.playWhenReady = false
        }
    }

    fun resumePlayer(){
        _player.value?.let {0
            it.playWhenReady = true
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