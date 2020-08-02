package dev.gtcl.reddit.ui.fragments.comments

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import dev.gtcl.reddit.*
import dev.gtcl.reddit.download.DownloadIntentService
import dev.gtcl.reddit.models.gfycat.GfyItem
import dev.gtcl.reddit.models.reddit.MoreComments
import dev.gtcl.reddit.models.reddit.listing.*
import dev.gtcl.reddit.repositories.ListingRepository
import dev.gtcl.reddit.repositories.GfycatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

const val CHILDREN_PER_FETCH = 50
class CommentsVM(val application: RedditApplication): AndroidViewModel(application) {

    // Repos
    private val listingRepository = ListingRepository.getInstance(application)
    private val gfycatRepository = GfycatRepository.getInstance()

    // Scopes
    private var viewModelJob = Job()
    private var coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private var _commentsFetched = false
    val commentsFetched: Boolean
        get() = _commentsFetched

    private val _post = MutableLiveData<Post>()
    val post: LiveData<Post>
        get() = _post

    private val _comments = MutableLiveData<MutableList<Item>>()
    val comments: LiveData<MutableList<Item>>
        get() = _comments

    private val _moreComments = MutableLiveData<MoreComments?>()
    val moreComments: LiveData<MoreComments?>
        get() = _moreComments

    private val _loading = MutableLiveData<Boolean>().apply { value = true }
    val loading: LiveData<Boolean>
        get() = _loading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?>
        get() = _errorMessage

    private val _removeAt = MutableLiveData<Int?>().apply { value = null }
    val removeAt: LiveData<Int?>
        get() = _removeAt

    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition = 0.toLong()

    private var gfyItem: GfyItem? = null

    private val _player = MutableLiveData<SimpleExoPlayer?>()
    val player: LiveData<SimpleExoPlayer?>
        get() = _player

    private val pageSize = 15

    fun setPost(post: Post){
        _post.value = post
        fetchPostAndComments(post.permalink)
    }

    fun fetchPostAndComments(permalink: String = post.value!!.permalink){
        coroutineScope.launch {
            try{
                _loading.value = true
                val commentPage = listingRepository.getPostAndComments(permalink, CommentSort.BEST, pageSize * 3).await()
                _post.value = commentPage.post
                _comments.value = commentPage.comments.toMutableList()
                _commentsFetched = true
                _loading.value = false
            } catch (e: Exception){
                Log.e("Error", e.toString())
                _errorMessage.value = e.getErrorMessage(application)
            }
        }
    }

    fun fetchMoreComments(position: Int){
        if(_loading.value == true){
            return
        }

        val moreItem = _comments.value?.get(position)
        if(moreItem == null || moreItem !is More){
            throw IllegalArgumentException("Invalid more item: $moreItem")
        }
        val children = moreItem.pollChildrenAsValidString(CHILDREN_PER_FETCH)

        coroutineScope.launch {
            try{
                _loading.value = true

                val comments = listingRepository.getMoreComments(children, post.value!!.name, CommentSort.BEST).await().json.data.things.map { it.data }.filter { !(it is More && it.depth == 0) }
                if(moreItem.lastChildFetched()){
                    _comments.value?.removeAt(position)
                    _removeAt.value = position
                }
                _moreComments.value = MoreComments(
                    position,
                    comments
                )

                _comments.value?.addAll(position, comments)
                _loading.value = false
            } catch (e: Exception){
                _errorMessage.value = e.getErrorMessage(application)
            }
        }
    }

    fun removeAtObserved(){
        _removeAt.value = null
    }

    fun moreCommentsObserved(){
        _moreComments.value = null
    }

    fun initializePlayer(post: Post){
        if(player.value != null) {
            return
        }
        if(_post.value == null) {
            throw IllegalStateException("Post has not been initialized")
        }
        coroutineScope.launch {
            val trackSelector = DefaultTrackSelector()
            trackSelector.setParameters(trackSelector.buildUponParameters().setMaxVideoSizeSd())
            val urlType = (post.url ?: "").getUrlType()
            val url = when(urlType){
                UrlType.GIFV, UrlType.GFYCAT, UrlType.HLS -> post.url
                else -> post.previewVideoUrl
            }
            var uri = Uri.parse(url)
            if(urlType == UrlType.GFYCAT) {
                try {
                    gfyItem = gfycatRepository.getGfycatInfo(
                        url!!.replace("http[s]?://gfycat.com/".toRegex(), ""))
                        .await()
                        .gfyItem
                    uri = Uri.parse(gfyItem!!.mobileUrl)
                }
                catch (e: Exception){
                    uri = Uri.parse(post.previewVideoUrl)
                }
            }
            var mediaSource = buildMediaSource(application.baseContext, uri)
            val player = ExoPlayerFactory.newSimpleInstance(application.baseContext, trackSelector)
            player!!.apply {
                repeatMode = Player.REPEAT_MODE_ONE
                playWhenReady = this@CommentsVM.playWhenReady
                seekTo(currentWindow, playbackPosition)
                prepare(mediaSource, false, false)
                addListener(object: Player.EventListener{
                    override fun onPlayerError(error: ExoPlaybackException?) {
                        _errorMessage.value = application.getString(R.string.error_with_video_player)
                        if(uri.path != post.previewVideoUrl) {
                            mediaSource = buildMediaSource(application.baseContext, Uri.parse(post.previewVideoUrl))
                            prepare(mediaSource, false, false)
                        }
                    }
                })
            }
            _player.value = player
        }
    }

    fun loadingFinished(){
        _loading.value = false
    }

    fun addItems(position: Int, items: List<Item>){
        _comments.value?.addAll(position, items)
    }

    fun errorMessageObserved(){
        _errorMessage.value = null
    }

    fun download(){
        if(_post.value == null) {
            return
        }
        DownloadIntentService.enqueueWork(application.applicationContext, gfyItem?.mp4Url ?: _post.value!!.url!!)
        Toast.makeText(application, application.getText(R.string.downloading), Toast.LENGTH_SHORT).show()
    }

    fun pausePlayer(){
        _player.value?.let{
            currentWindow = it.currentWindowIndex
            playbackPosition = it.currentPosition
            playWhenReady = false
            it.playWhenReady = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        _player.value?.release()
        _player.value = null
    }
}