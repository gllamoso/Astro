package dev.gtcl.astro.ui.fragments.media

import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.astro.*
import dev.gtcl.astro.download.DownloadIntentService
import dev.gtcl.astro.models.reddit.MediaURL
import dev.gtcl.astro.models.reddit.listing.Post
import dev.gtcl.astro.url.GFYCAT_REGEX
import dev.gtcl.astro.url.MediaType
import dev.gtcl.astro.url.REDGIFS_REGEX
import dev.gtcl.astro.url.getFirstGroup
import kotlinx.coroutines.launch
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

    fun loadMedia(mediaURL: MediaURL) {
        coroutineScope.launch {
            try {
                _isLoading.postValue(true)
                _mediaItems.postValue(when (mediaURL.mediaType) {
                    MediaType.IMGUR_ALBUM -> {
                        val album =
                            imgurRepository.getAlbumImages(mediaURL.imgurHash ?: return@launch)
                                .await().data.images ?: return@launch
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
                        val imgurData =
                            imgurRepository.getImage(mediaURL.imgurHash ?: return@launch)
                                .await().data
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
                        val id = GFYCAT_REGEX.getFirstGroup(mediaURL.url) ?: return@launch
                        var videoUrl: String?
                        try {
                            videoUrl = gfycatRepository.getGfycatInfo(id)
                                .await()
                                .gfyItem
                                .mobileUrl
                        } catch (e: HttpException) {
                            videoUrl = try {
                                gfycatRepository.getGfycatInfoFromRedgifs(id)
                                    .await()
                                    .gfyItem
                                    .mobileUrl
                            } catch (e: Exception) {
                                mediaURL.backupUrl
                            }
                        } catch (e: Exception) {
                            videoUrl = mediaURL.backupUrl
                        }

                        if (videoUrl != null) {
                            val backupUrl = if (videoUrl != mediaURL.backupUrl) {
                                mediaURL.backupUrl
                            } else {
                                null
                            }
                            listOf(MediaURL(videoUrl, MediaType.VIDEO, backupUrl))
                        } else {
                            throw Exception()
                        }
                    }
                    MediaType.REDGIFS -> {
                        val id = REDGIFS_REGEX.getFirstGroup(mediaURL.url) ?: return@launch
                        val videoUrl = try {
                            gfycatRepository.getGfycatInfoFromRedgifs(id)
                                .await()
                                .gfyItem
                                .mobileUrl
                        } catch (e: Exception) {
                            mediaURL.backupUrl
                        }

                        if (videoUrl != null) {
                            val backupUrl = if (videoUrl != mediaURL.backupUrl) {
                                mediaURL.backupUrl
                            } else {
                                null
                            }
                            listOf(MediaURL(videoUrl, MediaType.VIDEO, backupUrl))
                        } else {
                            throw Exception()
                        }
                    }
                    else -> {
                        listOf(mediaURL)
                    }
                })
                _mediaInitialized = true
            } catch (e: Exception) {
                _mediaItems.postValue(listOf())
                _errorMessage.postValue(e.getErrorMessage(application))
            } finally {
                _isLoading.postValue(false)
            }

        }
    }

    fun fetchGallery(galleryId: String) {
        coroutineScope.launch {
            try {
                _isLoading.postValue(true)
                val fullname = if (galleryId.startsWith("t3_")) {
                    galleryId
                } else {
                    "t3_$galleryId"
                }
                val post =
                    listingRepository.getPostFromId(fullname).await().data.children[0].data as Post
                val mediaItems = post.galleryAsMediaItems ?: throw Exception()
                _mediaItems.postValue(mediaItems)
                _post.postValue(post)
            } catch (e: Exception) {
                _mediaItems.postValue(listOf())
                _errorMessage.postValue(e.getErrorMessage(application))
            } finally {
                _isLoading.postValue(false)
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
        if (_mediaItems.value?.isEmpty() != false || _itemPosition.value == null) {
            return
        }

        Toast.makeText(
            application,
            application.getText(R.string.downloading),
            Toast.LENGTH_SHORT
        ).show()

        val item = (_mediaItems.value ?: return)[_itemPosition.value ?: return]
        val downloadUrl = item.url.removeHtmlEntities()
        DownloadIntentService.enqueueWork(application.applicationContext, downloadUrl)
    }

    fun setItems(list: List<MediaURL>) {
        _mediaItems.value = list
        _mediaInitialized = true
    }

    fun getCurrentMediaItem() = _mediaItems.value?.get(
        _itemPosition.value ?: throw IllegalStateException("Unable to fetch current media item")
    )
}