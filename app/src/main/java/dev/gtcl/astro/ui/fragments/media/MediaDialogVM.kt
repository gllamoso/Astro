package dev.gtcl.astro.ui.fragments.media

import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.astro.*
import dev.gtcl.astro.download.DownloadIntentService
import dev.gtcl.astro.models.reddit.MediaURL
import dev.gtcl.astro.models.reddit.listing.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class MediaDialogVM(private val application: AstroApplication) : AstroViewModel(application) {

    private val _mediaItems = MutableLiveData<List<MediaURL>>()
    val mediaItems: LiveData<List<MediaURL>>
        get() = _mediaItems

    private val _post = MutableLiveData<Post?>()
    val post: LiveData<Post?>
        get() = _post

    private val _itemPosition = MutableLiveData<Int>().apply { value = 0 }
    val itemPosition: LiveData<Int>
        get() = _itemPosition

    private val _isLoading = MutableLiveData<Boolean>().apply { value = false }
    val isLoading: LiveData<Boolean>
        get() = _isLoading

    private var _mediaInitialized = false
    val mediaInitialized: Boolean
        get() = _mediaInitialized

    fun setMedia(mediaURL: MediaURL) {
        coroutineScope.launch {
            try {
                _isLoading.postValue(true)
                _mediaItems.postValue(when (mediaURL.mediaType) {
                    MediaType.IMGUR_ALBUM -> {
                        val album = imgurRepository.getAlbumImages(mediaURL.imgurHash!!)
                            .await().data.images!!
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
                        val id = GFYCAT_REGEX.getIdFromUrl(mediaURL.url)!!
                        var videoUrl: String
                        try {
                            videoUrl = gfycatRepository.getGfycatInfo(id)
                                .await()
                                .gfyItem
                                .mobileUrl
                        } catch (e: Exception) {
                            if (e is HttpException) {
                                videoUrl = gfycatRepository.getGfycatInfoFromRedgifs(id)
                                    .await()
                                    .gfyItem
                                    .mobileUrl
                                listOf(MediaURL(videoUrl, MediaType.VIDEO, mediaURL.backupUrl))
                            } else {
                                throw Exception()
                            }
                        }
                        listOf(MediaURL(videoUrl, MediaType.VIDEO, mediaURL.backupUrl))
                    }
                    MediaType.REDGIFS -> {
                        val id = REDGIFS_REGEX.getIdFromUrl(mediaURL.url)!!
                        val videoUrl = gfycatRepository.getGfycatInfoFromRedgifs(id)
                            .await()
                            .gfyItem
                            .mobileUrl
                        listOf(MediaURL(videoUrl, MediaType.VIDEO, mediaURL.backupUrl))
                    }
                    else -> {
                        listOf(mediaURL)
                    }
                }
                )
                _mediaInitialized = true
                _isLoading.postValue(false)
            } catch (e: Exception) {
                _errorMessage.postValue(e.getErrorMessage(application))
            }

        }
    }

    fun setPost(post: Post?) {
        _post.value = post
    }

    fun setItemPosition(position: Int) {
        _itemPosition.value = position
    }

    fun downloadAlbum() {
        if (_mediaItems.value == null) {
            return
        }

        DownloadIntentService.enqueueWork(
            application.applicationContext,
            (_mediaItems.value ?: return).map { it.url })
    }

    fun downloadCurrentItem() {
        if (_mediaItems.value == null && _itemPosition.value == null) {
            return
        }

        coroutineScope.launch {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    application,
                    application.getText(R.string.downloading),
                    Toast.LENGTH_SHORT
                ).show()
            }
            val item = _mediaItems.value!![_itemPosition.value!!]
            var downloadUrl = item.url

            if (item.mediaType == MediaType.GFYCAT) {
                try {
                    downloadUrl = gfycatRepository.getGfycatInfo(
                        item.url.replace("http[s]?://gfycat.com/".toRegex(), "")
                    )
                        .await()
                        .gfyItem
                        .mp4Url
                } catch (e: Exception) {
                    if (e is HttpException && e.code() == 404 && item.backupUrl != null) {
                        downloadUrl = item.backupUrl
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                application,
                                application.getText(R.string.unable_to_download_file),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } else if (item.mediaType == MediaType.PICTURE) {
                downloadUrl = item.url.toValidImgUrl() ?: item.url
            }

            DownloadIntentService.enqueueWork(application.applicationContext, downloadUrl)
        }
    }

    fun setItems(list: List<MediaURL>) {
        _mediaItems.value = list
        _mediaInitialized = true
    }

    fun getCurrentMediaItem() = _mediaItems.value?.get(_itemPosition.value!!)
}