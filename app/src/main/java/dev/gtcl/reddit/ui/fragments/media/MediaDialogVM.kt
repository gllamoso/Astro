package dev.gtcl.reddit.ui.fragments.media

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.reddit.MediaType
import dev.gtcl.reddit.R
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.download.DownloadIntentService
import dev.gtcl.reddit.getErrorMessage
import dev.gtcl.reddit.models.reddit.MediaURL
import dev.gtcl.reddit.models.reddit.listing.Post
import dev.gtcl.reddit.repositories.GfycatRepository
import dev.gtcl.reddit.repositories.ImgurRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.HttpException

class MediaDialogVM(private val application: RedditApplication): AndroidViewModel(application){

    private val imgurRepository = ImgurRepository.getInstance()
    private val gfycatRepository = GfycatRepository.getInstance()

    // Scopes
    private val viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _mediaItems = MutableLiveData<List<MediaURL>>()
    val mediaItems: LiveData<List<MediaURL>>
        get() = _mediaItems

    private val _post = MutableLiveData<Post?>()
    val post: LiveData<Post?>
        get() = _post

    private val _itemPosition = MutableLiveData<Int>().apply { value = 0 }
    val itemPosition : LiveData<Int>
        get() = _itemPosition

    private val _isLoading = MutableLiveData<Boolean>().apply { value = false }
    val isLoading: LiveData<Boolean>
        get() = _isLoading

    private var _mediaInitialized = false
    val mediaInitialized: Boolean
        get() = _mediaInitialized

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?>
        get() = _errorMessage

    fun setMedia(mediaURL: MediaURL){
        coroutineScope.launch {
            try{
                _isLoading.value = true
                _mediaItems.value = when(mediaURL.mediaType){
                    MediaType.IMGUR_ALBUM -> {
                        val album = imgurRepository.getAlbumImages(mediaURL.imgurHash!!).await().data.images!!
                        album.map {
                            val mediaType = when {
                                it.type.startsWith("video") -> {
                                    MediaType.VIDEO
                                }
                                it.type.startsWith("image/gif") -> {
                                    MediaType.GIF
                                }
                                else -> {
                                    MediaType.PICTURE
                                }
                            }
                            MediaURL(it.link, mediaType)
                        }
                    }
                    MediaType.IMGUR_PICTURE -> {
                        val imgurData = imgurRepository.getImage(mediaURL.imgurHash!!).await().data
                        val mediaType = when {
                            imgurData.type?.startsWith("video") ?: false -> {
                                MediaType.VIDEO
                            }
                            imgurData.type?.startsWith("image/gif") ?: false -> {
                                MediaType.GIF
                            }
                            else -> {
                                MediaType.PICTURE
                            }
                        }
                        listOf(MediaURL(imgurData.link, mediaType))
                    }
                    MediaType.GFYCAT -> {
                        val videoUrl = gfycatRepository.getGfycatInfo(
                            mediaURL.url.replace("http[s]?://gfycat.com/".toRegex(), ""))
                            .await()
                            .gfyItem
                            .mobileUrl
                        listOf(MediaURL(videoUrl, MediaType.VIDEO, mediaURL.backupUrl))
                    }
                    MediaType.REDGIFS -> {
                        val videoUrl = gfycatRepository.getGfycatInfoFromRedgifs(
                            mediaURL.url.replace("http[s]?://(www\\.)?redgifs.com/watch/".toRegex(), ""))
                            .await()
                            .gfyItem
                            .mobileUrl
                        listOf(MediaURL(videoUrl, MediaType.VIDEO, mediaURL.backupUrl))
                    }
                    else -> {
                        listOf(mediaURL)
                    }
                }
                _mediaInitialized = true
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = e.getErrorMessage(application)
            }

        }
    }

    fun errorMessageObserved(){
        _errorMessage.value = null
    }

    fun setPost(post: Post?){
        _post.value = post
    }

    fun setItemPosition(position: Int){
        _itemPosition.value = position
    }

    fun downloadAlbum(name: String){
        if(_mediaItems.value == null){
            return
        }

        DownloadIntentService.enqueueWork(application.applicationContext, _mediaItems.value!!.map { it.url }, name)
    }

    fun downloadCurrentItem(){
        if(_mediaItems.value == null && _itemPosition.value == null){
            return
        }

        coroutineScope.launch {
            Toast.makeText(application, application.getText(R.string.downloading), Toast.LENGTH_SHORT).show()
            val item = _mediaItems.value!![_itemPosition.value!!]
            var downloadUrl = item.url

            if(item.mediaType == MediaType.GFYCAT){
                try{
                    downloadUrl = gfycatRepository.getGfycatInfo(
                        item.url.replace("http[s]?://gfycat.com/".toRegex(), ""))
                        .await()
                        .gfyItem
                        .mp4Url
                } catch (e: Exception){
                    if(e is HttpException && e.code() == 404 && item.backupUrl != null){
                        downloadUrl = item.backupUrl
                    } else {
                        Toast.makeText(application, application.getText(R.string.unable_to_download_file), Toast.LENGTH_SHORT).show()
                    }
                }
            }

            DownloadIntentService.enqueueWork(application.applicationContext, downloadUrl)
        }
    }
}