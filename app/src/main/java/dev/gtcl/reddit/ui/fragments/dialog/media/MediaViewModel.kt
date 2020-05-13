package dev.gtcl.reddit.ui.fragments.dialog.media

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
//import com.arthenica.mobileffmpeg.Config
//import com.arthenica.mobileffmpeg.FFmpeg
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import dev.gtcl.reddit.R
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.URL_KEY
import dev.gtcl.reddit.buildMediaSource
import dev.gtcl.reddit.download.DownloadIntentService
import dev.gtcl.reddit.download.DownloadService
import dev.gtcl.reddit.download.HlsDownloader
import dev.gtcl.reddit.models.gfycat.GfyItem
import dev.gtcl.reddit.models.reddit.Post
import dev.gtcl.reddit.models.reddit.UrlType
import dev.gtcl.reddit.repositories.GfycatRepository
import kotlinx.coroutines.*
import java.util.*

class MediaViewModel(private val application: RedditApplication): AndroidViewModel(application){

    private val gfycatRepository = GfycatRepository.getInstance()

    // Scopes
    private var viewModelJob = Job()
    private var coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean>
        get() = _loading

    fun setLoading(loading: Boolean){
        _loading.value = loading
    }

    var initialized = false
    var url: String? = null
    var post: Post? = null

    private val _urlType = MutableLiveData<UrlType>()
    val urlType: LiveData<UrlType>
        get() = _urlType

    fun setUrlType(urlType: UrlType){
        _urlType.value = urlType
    }

    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition = 0.toLong()

    private lateinit var gfyItem: GfyItem

    private val _player = MutableLiveData<SimpleExoPlayer>()
    val player: LiveData<SimpleExoPlayer>
        get() = _player

    fun initializePlayer(){
        if(player.value != null) return
        coroutineScope.launch {
            val trackSelector = DefaultTrackSelector()
            trackSelector.setParameters(trackSelector.buildUponParameters().setMaxVideoSizeSd())
            var uri = Uri.parse(url)
            if(urlType.value == UrlType.GFYCAT) {
                try {
                    gfyItem = gfycatRepository.getGfycatInfo(
                        url!!.replace("http[s]?://gfycat.com/".toRegex(), "")
                    ).await().gfyItem
                    uri = Uri.parse(gfyItem.mobileUrl)
                }
                catch (e: Exception){
                    Log.d("TAE", "Exception found: $e")
                    if(post != null) {
                        uri = Uri.parse(post!!.videoUrl)
                    } else {
                        return@launch
                    }
                }
            }
            var mediaSource = buildMediaSource(application.baseContext, uri)
            val player = ExoPlayerFactory.newSimpleInstance(application.baseContext, trackSelector)
            player!!.apply {
                repeatMode = Player.REPEAT_MODE_ONE
                playWhenReady = this@MediaViewModel.playWhenReady
                seekTo(currentWindow, playbackPosition)
                prepare(mediaSource, false, false)
                addListener(object: Player.EventListener{
                    override fun onPlayerError(error: ExoPlaybackException?) {
                        Log.d("TAE", "Error: ${error.toString()}")
                        if(uri.path != post?.videoUrl && post != null) {
                            mediaSource = buildMediaSource(application.baseContext, Uri.parse(post!!.videoUrl))
                            prepare(mediaSource, false, false)
                        }
                    }
                })
            }
            _player.value = player
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

    @SuppressLint("Recycle")
    fun download(){
        val downloadUrl: String =
            if(::gfyItem.isInitialized) {
                gfyItem.mp4Url
            } else {
                url!!
            }

        val serviceIntent = Intent(application.applicationContext, DownloadIntentService::class.java)
        serviceIntent.putExtra(URL_KEY, downloadUrl)
        DownloadIntentService.enqueueWork(application.applicationContext, serviceIntent)
        Toast.makeText(application, application.getText(R.string.downloading), Toast.LENGTH_SHORT).show()
    }

    val shareUrl: String
        get() {
            return when{
                ::gfyItem.isInitialized -> gfyItem.mp4Url
                post != null -> post!!.url!!
                else -> url!!
            }
        }

    override fun onCleared() {
        super.onCleared()
        _player.value?.let {
            it.release()
        }
        _player.value = null
    }
}