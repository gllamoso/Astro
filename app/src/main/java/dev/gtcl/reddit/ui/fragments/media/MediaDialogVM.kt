package dev.gtcl.reddit.ui.fragments.media

import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.reddit.MediaType
import dev.gtcl.reddit.R
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.download.DownloadIntentService
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

    private val _showUi = MutableLiveData<Boolean>().apply { value = true }
    val showUi: LiveData<Boolean>
        get() = _showUi

    fun setMedia(mediaURL: MediaURL){
        coroutineScope.launch {
            if(mediaURL.mediaType == MediaType.IMGUR_ALBUM){
                val album = imgurRepository.getAlbumImages(mediaURL.imgurHash).await().data.images!!
                _mediaItems.value = album.map {
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
            } else {
                _mediaItems.value = listOf(mediaURL)
            }
        }
    }

    fun setPost(post: Post?){
        _post.value = post
    }

    fun setItemPosition(position: Int){
        _itemPosition.value = position
    }

    fun toggleUiVisibility(){
        _showUi.value = !(_showUi.value ?: false)
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