package dev.gtcl.reddit.ui.fragments.dialog.imageviewer

import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.buildMediaSource
import dev.gtcl.reddit.models.gfycat.GfyItem
import dev.gtcl.reddit.models.reddit.UrlType
import dev.gtcl.reddit.repositories.GfycatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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

    var url: String? = null
    var backupVideoUrl: String? = null

    private val _urlType = MutableLiveData<UrlType>()
    val urlType: LiveData<UrlType>
        get() = _urlType

    fun setUrlType(urlType: UrlType){
        _urlType.value = urlType
    }

    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition = 0.toLong()
    var resized = false

    lateinit var gfyItem: GfyItem

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
                    uri = Uri.parse(backupVideoUrl)
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
                        if(uri.path != backupVideoUrl) {
                            mediaSource = buildMediaSource(application.baseContext, Uri.parse(backupVideoUrl))
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

    fun download(){
        // TODO: Delete
//        https://v.redd.it/r1m7ibntxjt41/HLS_224_v4.m3u8

//            val rc = FFmpeg.execute("-i https://v.redd.it/y0dvr5jj3ft41/HLSPlaylist.m3u8 -acodec copy -bsf:a aac_adtstoasc -vcodec copy /data/user/0/dev.gtcl.reddit/files/FunnyDad1.mp4")
//            when(rc){
//                Config.RETURN_CODE_SUCCESS -> Log.d("TAE", "Executed successfully")
//                Config.RETURN_CODE_CANCEL -> Log.d("TAE", "Cancelled by user.")
//                else -> Log.d("TAE", "Execution failed. RC = $rc")
//            }

//        val myUrl = "https://giant.gfycat.com/OffbeatRigidCivet.mp4" // WORKS!!!!
//        val request = DownloadManager.Request(Uri.parse(myUrl))
//        request.apply {
//            setTitle("Download")
//            setDescription("Your file is downloading...")
//            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
//            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
//            setDestinationInExternalFilesDir(this@MainActivity, Environment.DIRECTORY_DOWNLOADS, "test.mp4")
//        }
//        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
//        manager.enqueue(request)
    }

    override fun onCleared() {
        super.onCleared()
        _player.value?.let {
            it.release()
            Log.d("TAE", "Player released")
        }
        _player.value = null
    }
}