package dev.gtcl.reddit.ui.fragments.media

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerControlView
import dev.gtcl.reddit.R
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.URL_KEY
import dev.gtcl.reddit.buildMediaSource
import dev.gtcl.reddit.download.DownloadIntentService
import dev.gtcl.reddit.models.gfycat.GfyItem
import dev.gtcl.reddit.models.reddit.listing.Post
import dev.gtcl.reddit.models.reddit.listing.UrlType
import dev.gtcl.reddit.repositories.GfycatRepository
import kotlinx.coroutines.*
import java.lang.ref.WeakReference

class MediaVM(private val application: RedditApplication): AndroidViewModel(application){

    private val gfycatRepository = GfycatRepository.getInstance()

    // Scopes
    private var viewModelJob = Job()
    private var coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _loading = MutableLiveData<Boolean>().apply { value = true }
    val loading: LiveData<Boolean>
        get() = _loading

    private val _showUi = MutableLiveData<Boolean>().apply { value = true }
    val showUi: LiveData<Boolean>
        get() = _showUi

    private var _initialized = false
    val initialized: Boolean
        get() = _initialized

    private val _url = MutableLiveData<String>()
    val url: LiveData<String>
        get() = _url

    private val _post = MutableLiveData<Post?>()
    val post: LiveData<Post?>
        get() = _post

    private val _urlType = MutableLiveData<UrlType>()
    val urlType: LiveData<UrlType>
        get() = _urlType

    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition = 0.toLong()

    private var gfyItem: GfyItem? = null

    private val _player = MutableLiveData<SimpleExoPlayer?>()
    val player: LiveData<SimpleExoPlayer?>
        get() = _player

    private val _playerControllerView = MutableLiveData<WeakReference<PlayerControlView>?>()
    val playerControllerView: LiveData<WeakReference<PlayerControlView>?>
        get() = _playerControllerView

    fun loadingStarted(){
        _loading.value = true
    }

    fun loadingFinished(){
        _loading.value = false
    }

    fun initialize(url: String, urlType: UrlType, post: Post?){
        _url.value = url
        _urlType.value = urlType
        _post.value = post
        _initialized = true
    }

    fun toggleUiVisibility(){
        _showUi.value = !(_showUi.value ?: false)
    }

    fun initializePlayer(){
        if(player.value != null) {
            return
        }
        if(url.value == null) {
            throw IllegalStateException("Url has not been initialized")
        }
        coroutineScope.launch {
            _loading.value = true
            val trackSelector = DefaultTrackSelector()
            trackSelector.setParameters(trackSelector.buildUponParameters().setMaxVideoSizeSd())
            var uri = Uri.parse(url.value)
            if(urlType.value == UrlType.GFYCAT) {
                try {
                    gfyItem = gfycatRepository.getGfycatInfo(
                        url.value!!.replace("http[s]?://gfycat.com/".toRegex(), ""))
                        .await()
                        .gfyItem
                    uri = Uri.parse(gfyItem!!.mobileUrl)
                }
                catch (e: Exception){
                    if(post.value != null) {
                        uri = Uri.parse(post.value!!.previewVideoUrl)
                    } else {
                        return@launch
                    }
                }
            }
            var mediaSource = buildMediaSource(application.baseContext, uri)
            val player = ExoPlayerFactory.newSimpleInstance(application.baseContext, trackSelector)
            player!!.apply {
                repeatMode = Player.REPEAT_MODE_ONE
                playWhenReady = this@MediaVM.playWhenReady
                seekTo(currentWindow, playbackPosition)
                prepare(mediaSource, false, false)
                addListener(object: Player.EventListener{
                    override fun onPlayerError(error: ExoPlaybackException?) {
                        Log.e("Media", "Exception: $error")
                        if(uri.path != post.value?.previewVideoUrl && post.value != null) {
                            mediaSource = buildMediaSource(application.baseContext, Uri.parse(post.value!!.previewVideoUrl))
                            prepare(mediaSource, false, false)
                        }
                    }
                })
            }
            _player.value = player
            _loading.value = false
        }
    }

    fun pausePlayer(){
        _player.value?.let{
            currentWindow = it.currentWindowIndex
            playbackPosition = it.currentPosition
            playWhenReady = false
            it.playWhenReady = false
        }
    }

    fun passPlayerControlView(playerReference: WeakReference<PlayerControlView>){
        _playerControllerView.value = playerReference
    }

    fun playerControlViewObserved(){
        _playerControllerView.value = null
    }

    fun download(){
        if(url.value == null || loading.value == true) {
            return
        }
        DownloadIntentService.enqueueWork(application.applicationContext, gfyItem?.mp4Url ?: url.value!!)
        Toast.makeText(application, application.getText(R.string.downloading), Toast.LENGTH_SHORT).show()
    }

    override fun onCleared() {
        super.onCleared()
        _player.value?.release()
        _player.value = null
    }
}