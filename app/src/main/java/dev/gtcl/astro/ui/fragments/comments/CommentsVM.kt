package dev.gtcl.astro.ui.fragments.comments

import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import dev.gtcl.astro.*
import dev.gtcl.astro.download.DownloadIntentService
import dev.gtcl.astro.models.reddit.MediaURL
import dev.gtcl.astro.models.reddit.listing.Comment
import dev.gtcl.astro.models.reddit.listing.Item
import dev.gtcl.astro.models.reddit.listing.More
import dev.gtcl.astro.models.reddit.listing.Post
import dev.gtcl.astro.network.MoreComments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import kotlin.collections.HashMap

const val CHILDREN_PER_FETCH = 50

class CommentsVM(val application: AstroApplication) : AstroViewModel(application) {

    var commentsExpanded: Boolean? = null

    private val hiddenItemsMap = HashMap<String, List<Item>>()

    private val _post = MutableLiveData<Post>()
    val post: LiveData<Post>
        get() = _post

    private val _comments = MutableLiveData<MutableList<Item>>()
    val comments: LiveData<MutableList<Item>>
        get() = _comments

    private val _moreComments = MutableLiveData<MoreComments?>()
    val moreComments: LiveData<MoreComments?>
        get() = _moreComments

    private val _allCommentsFetched = MutableLiveData<Boolean>().apply { value = false }
    val allCommentsFetched: LiveData<Boolean>
        get() = _allCommentsFetched

    private val _loading = MutableLiveData<Boolean>().apply { value = false }
    val loading: LiveData<Boolean>
        get() = _loading

    private val _commentSort = MutableLiveData<CommentSort>()
    val commentSort: LiveData<CommentSort>
        get() = _commentSort

    private val _removeAt = MutableLiveData<Int?>().apply { value = null }
    val removeAt: LiveData<Int?>
        get() = _removeAt

    private val _notifyAt = MutableLiveData<Int?>().apply { value = null }
    val notifyAt: LiveData<Int?>
        get() = _notifyAt

    private val _mediaItems = MutableLiveData<List<MediaURL>?>().apply { value = null }
    val mediaItems: LiveData<List<MediaURL>?>
        get() = _mediaItems

    private val _mediaItemsLoading = MutableLiveData<Boolean>()
    val mediaItemsLoading: LiveData<Boolean>
        get() = _mediaItemsLoading

    private val pageSize = 15

    private var fullContextLink: String? = null

    var contentInitialized = false

    var viewPagerInitialized = false

    init {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(application)
        val defaultSort =
            sharedPref.getString("default_comment_sort", application.getString(R.string.order_best))
        val sortArray = application.resources.getStringArray(R.array.comment_sort_entries)
        _commentSort.value = when (sortArray.indexOf(defaultSort)) {
            1 -> CommentSort.TOP
            2 -> CommentSort.NEW
            3 -> CommentSort.CONTROVERSIAL
            4 -> CommentSort.OLD
            5 -> CommentSort.QA
            6 -> CommentSort.RANDOM
            else -> CommentSort.BEST
        }
    }

    fun setPost(post: Post) {
        _post.value = post
    }

    fun setCommentSort(sort: CommentSort) {
        _commentSort.value = sort
    }

    fun fetchFullContext() {
        fetchComments(fullContextLink!!, isFullContext = true, refreshPost = false)
    }

    fun fetchComments(permalink: String, isFullContext: Boolean, refreshPost: Boolean) {
        coroutineScope.launch {
            try {
                _loading.postValue(true)
                val isLoggedIn = application.accessToken != null
                val link = if (isLoggedIn) {
                    permalink.replace("www.", "oauth.")
                } else {
                    permalink
                }
                val commentPage =
                    listingRepository.getPostAndComments(link, _commentSort.value!!, pageSize * 3)
                        .await()
                if (refreshPost) {
                    _post.postValue(commentPage.post)
                }
                _allCommentsFetched.postValue(isFullContext)
                if (!isFullContext) {
                    fullContextLink = VALID_REDDIT_COMMENTS_URL_REGEX.find(permalink)!!.value
                }
                _comments.postValue(commentPage.comments.toMutableList())
            } catch (e: Exception) {
                _comments.postValue(mutableListOf())
                _errorMessage.postValue(e.getErrorMessage(application))
            } finally {
                _loading.postValue(false)
            }
        }
    }

    fun fetchMoreComments(position: Int) {
        coroutineScope.launch {
            val positionOffset = position + if (allCommentsFetched.value == false) -1 else 0
            if (_loading.value == true || position == -1) {
                if (position != -1) {
                    _notifyAt.value = position
                }
                return@launch
            }
            val moreItem = _comments.value?.get(positionOffset)
            if (moreItem == null || moreItem !is More) {
                throw IllegalArgumentException("Invalid more item: $moreItem")
            }
            val children = moreItem.pollChildrenAsValidString(CHILDREN_PER_FETCH)
            try {
                _loading.postValue(true)
                val comments = listingRepository.getMoreComments(
                    children,
                    post.value!!.name,
                    _commentSort.value!!
                ).await().json.data.things.map { it.data }.filter { !(it is More && it.depth == 0) }
                if (moreItem.lastChildFetched) {
                    _comments.value?.removeAt(positionOffset)
                    _removeAt.postValue(position)
                }
                _moreComments.postValue(
                    MoreComments(
                        position,
                        comments
                    )
                )

                _comments.value?.addAll(positionOffset, comments)
            } catch (e: Exception) {
                moreItem.undoChildrenPoll()
                _notifyAt.postValue(position)
                _errorMessage.postValue(e.getErrorMessage(application))
            } finally {
                _loading.postValue(false)
            }
        }
    }

    fun removeAtObserved() {
        _removeAt.value = null
    }

    fun notifyAtObserved() {
        _notifyAt.value = null
    }

    fun moreCommentsObserved() {
        _moreComments.value = null
    }

    fun addItems(position: Int, items: List<Item>) {
        _comments.value?.addAll(position, items)
    }

    fun hideItems(position: Int): Int {
        val positionOffset = position + if (allCommentsFetched.value == false) -1 else 0
        val itemInPosition = comments.value!![positionOffset]
        val depth = when (val currItem = comments.value!![positionOffset]) {
            is Comment -> currItem.depth ?: 0
            is More -> currItem.depth
            else -> 0
        }
        var i = positionOffset
        while (i++ < comments.value!!.size - 1) {
            val currDepth = when (val currItem = comments.value!![i]) {
                is Comment -> currItem.depth ?: 0
                is More -> currItem.depth
                else -> 0
            }
            if (currDepth <= depth) {
                break
            }
        }
        return if (i - 1 > positionOffset) {
            val listToHide = comments.value!!.subList(positionOffset + 1, i).toList()
            for (j in listToHide.indices) {
                comments.value!!.removeAt(positionOffset + 1)
            }
            hiddenItemsMap[itemInPosition.name] = listToHide
            listToHide.size
        } else {
            0
        }
    }

    fun unhideItems(position: Int): List<Item> {
        val positionOffset = position + if (allCommentsFetched.value == false) -1 else 0
        val itemInPosition = comments.value!![positionOffset]
        val hiddenItems = hiddenItemsMap[itemInPosition.name]
        return if (hiddenItems != null) {
            comments.value!!.addAll(positionOffset + 1, hiddenItems)
            hiddenItemsMap.remove(itemInPosition.name)
            hiddenItems
        } else {
            listOf()
        }
    }

    fun removeCommentAt(position: Int) {
        comments.value?.removeAt(position)
    }

    fun setCommentAt(comment: Comment, position: Int) {
        _comments.value?.set(position, comment)
    }

    fun fetchMediaItems(post: Post) {
        coroutineScope.launch {
            try {
                _mediaItemsLoading.postValue(true)
                _loading.postValue(true)
                _mediaItems.postValue(when (val urlType = post.urlType) {
                    UrlType.IMAGE -> {
                        listOf(MediaURL(post.url!!, MediaType.PICTURE))
                    }
                    UrlType.GIF -> {
                        listOf(MediaURL(post.url!!, MediaType.GIF))
                    }
                    UrlType.HLS, UrlType.GIFV, UrlType.STANDARD_VIDEO, UrlType.REDDIT_VIDEO -> {
                        if (post.previewVideoUrl != null) {
                            listOf(MediaURL(post.previewVideoUrl!!, MediaType.VIDEO))
                        } else {
                            val url = when (urlType) {
                                UrlType.GIFV -> {
                                    post.url!!.replace(".gifv", ".mp4")
                                }
                                else -> {
                                    post.url!!
                                }
                            }
                            listOf(MediaURL(url, MediaType.VIDEO))
                        }
                    }
                    UrlType.GFYCAT -> {
                        val id = GFYCAT_REGEX.getIdFromUrl(post.url!!)!!
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
                                listOf(
                                    MediaURL(
                                        videoUrl,
                                        MediaType.VIDEO,
                                        post.previewVideoUrl
                                    )
                                )
                            } else {
                                throw Exception()
                            }
                        }
                        listOf(MediaURL(videoUrl, MediaType.VIDEO, post.previewVideoUrl))
                    }
                    UrlType.REDGIFS -> {
                        val id = REDGIFS_REGEX.getIdFromUrl(post.url!!)!!
                        val videoUrl = gfycatRepository.getGfycatInfoFromRedgifs(id)
                            .await()
                            .gfyItem
                            .mobileUrl
                        listOf(MediaURL(videoUrl, MediaType.VIDEO, post.previewVideoUrl))
                    }
                    UrlType.IMGUR_ALBUM -> {
                        val album =
                            imgurRepository.getAlbumImages(post.url!!.getImgurHashFromUrl()!!)
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
                    UrlType.IMGUR_IMAGE -> {
                        val imgurData = imgurRepository.getImage(post.url!!.getImgurHashFromUrl()!!)
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
                    UrlType.REDDIT_GALLERY -> {
                        post.galleryAsMediaItems!!
                    }
                    else -> {
                        listOf()
                    }
                }
                )
            } catch (e: Exception) {
                _errorMessage.postValue(e.getErrorMessage(application))
            } finally {
                _loading.postValue(false)
                _mediaItemsLoading.postValue(false)
            }
        }
    }

    fun downloadAlbum() {
        if (_mediaItems.value == null) {
            return
        }

        DownloadIntentService.enqueueWork(
            application.applicationContext,
            _mediaItems.value!!.map { it.url })
    }

    fun downloadItem(position: Int) {
        if (_mediaItems.value == null) {
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
            val item = _mediaItems.value!![position]
            var downloadUrl = item.url

            if (item.mediaType == MediaType.GFYCAT) {
                try {
                    downloadUrl = gfycatRepository.getGfycatInfo(
                        item.url.replace("http[s]?://gfycat.com/".toRegex(), "").split("-")[0]
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
            }

            DownloadIntentService.enqueueWork(application.applicationContext, downloadUrl)
        }
    }
}