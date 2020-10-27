package dev.gtcl.astro.ui.fragments.comments

import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.astro.*
import dev.gtcl.astro.download.DownloadIntentService
import dev.gtcl.astro.models.reddit.MediaURL
import dev.gtcl.astro.models.reddit.listing.*
import dev.gtcl.astro.network.MoreComments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import timber.log.Timber

const val CHILDREN_PER_FETCH = 50

class CommentsVM(val application: AstroApplication) : AstroViewModel(application) {

    var commentsExpanded: Boolean? = null

    private val hiddenItemsMap = HashMap<String, List<Item>>()

    private val _post = MutableLiveData<Post>()
    val post: LiveData<Post>
        get() = _post

    private val _comments = MutableLiveData<MutableList<Item>?>()
    val comments: LiveData<MutableList<Item>?>
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

    private val _mediaItemsFailed = MutableLiveData<Boolean>().apply { value = false }
    val mediaItemsFailed: LiveData<Boolean>
        get() = _mediaItemsFailed

    private val pageSize = 15

    private var fullContextLink: String? = null

    var contentInitialized = false

    var viewPagerInitialized = false

    val postCreatedFromUser: Boolean
        get() {
            val currentAccount = application.currentAccount ?: return false
            val post = _post.value ?: return false
            return currentAccount.fullId == post.authorFullName
        }

    init {
        val sharedPref = application.sharedPref
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
        coroutineScope.launch {
            post.parseSelfText()
            _post.postValue(post)
        }
    }

    fun setCommentSort(sort: CommentSort) {
        _commentSort.value = sort
    }

    fun fetchFullContext() {
        fetchComments(fullContextLink ?: return, isFullContext = true, refreshPost = false)
    }

    fun setAllCommentsFetched(fetched: Boolean){
        _allCommentsFetched.value = fetched
    }

    fun fetchComments(permalink: String, isFullContext: Boolean, refreshPost: Boolean) {
        val previousValueOfAllCommentsFetched = _allCommentsFetched.value ?: false
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
                    miscRepository.getPostAndComments(
                        link,
                        _commentSort.value ?: return@launch, pageSize * 3
                    )
                        .await()
                if (refreshPost) {
                    commentPage.post.parseSelfText()
                    _post.postValue(commentPage.post)
                }
                if (!isFullContext) {
                    fullContextLink = (VALID_REDDIT_COMMENTS_URL_REGEX.find(permalink)
                        ?: return@launch).value
                }
                if(_allCommentsFetched.value != isFullContext){
                    _allCommentsFetched.postValue(isFullContext)
                }
                commentPage.comments.parseAllText()
                _comments.postValue(commentPage.comments.toMutableList())
            } catch (e: Exception) {
                if(_comments.value == null){
                    _comments.postValue(mutableListOf())
                }
                _allCommentsFetched.postValue(previousValueOfAllCommentsFetched)
                _errorMessage.postValue(e.getErrorMessage(application))
                Timber.tag(this@CommentsVM::class.simpleName).d(e)
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
                val comments = miscRepository.getMoreComments(
                    children,
                    (post.value ?: return@launch).name,
                    _commentSort.value ?: return@launch
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

    fun clearComments() {
        _comments.value = null
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
                _mediaItemsFailed.postValue(false)
                _loading.postValue(true)
                _mediaItems.postValue(when (post.urlType) {
                    UrlType.IMAGE -> {
                        listOf(MediaURL(post.urlFormatted ?: return@launch, MediaType.PICTURE))
                    }
                    UrlType.GIF -> {
                        listOf(MediaURL(post.urlFormatted ?: return@launch, MediaType.GIF))
                    }
                    UrlType.HLS, UrlType.STANDARD_VIDEO, UrlType.REDDIT_VIDEO -> {
                        if (post.previewVideoUrl != null) {
                            val url = post.previewVideoUrl
                            listOf(
                                MediaURL(
                                    url ?: return@launch,
                                    MediaType.VIDEO,
                                    thumbnail = post.thumbnailFormatted
                                )
                            )
                        } else {
                            val url = post.urlFormatted ?: return@launch
                            listOf(
                                MediaURL(
                                    url,
                                    MediaType.VIDEO,
                                    thumbnail = post.thumbnailFormatted
                                )
                            )
                        }
                    }
                    UrlType.GIFV -> {
                        listOf(
                            MediaURL(
                                (post.urlFormatted ?: return@launch).replace(".gifv", ".mp4"),
                                MediaType.VIDEO,
                                backupUrl = post.previewVideoUrl,
                                thumbnail = post.thumbnailFormatted
                            )
                        )
                    }
                    UrlType.GFYCAT -> {
                        val id = GFYCAT_REGEX.getIdFromUrl(post.urlFormatted ?: return@launch)
                            ?: return@launch
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
                                        post.previewVideoUrl,
                                        post.thumbnailFormatted
                                    )
                                )
                            } else {
                                throw Exception()
                            }
                        }
                        listOf(
                            MediaURL(
                                videoUrl,
                                MediaType.VIDEO,
                                post.previewVideoUrl,
                                post.thumbnailFormatted
                            )
                        )
                    }
                    UrlType.REDGIFS -> {
                        val id = REDGIFS_REGEX.getIdFromUrl(post.urlFormatted ?: return@launch)
                            ?: return@launch
                        val videoUrl = gfycatRepository.getGfycatInfoFromRedgifs(id)
                            .await()
                            .gfyItem
                            .mobileUrl
                        listOf(
                            MediaURL(
                                videoUrl,
                                MediaType.VIDEO,
                                post.previewVideoUrl,
                                post.thumbnailFormatted
                            )
                        )
                    }
                    UrlType.IMGUR_ALBUM -> {
                        val album =
                            imgurRepository.getAlbumImages(
                                (post.urlFormatted ?: return@launch).getImgurHashFromUrl()
                                    ?: return@launch
                            )
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
                    UrlType.IMGUR_IMAGE -> {
                        val imgurData = imgurRepository.getImage(
                            (post.urlFormatted ?: return@launch).getImgurHashFromUrl()
                                ?: return@launch
                        )
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
                        if (post.galleryAsMediaItems?.size ?: 0 == 0) {
                            _mediaItemsFailed.postValue(true)
                            null
                        } else {
                            post.galleryAsMediaItems
                        }
                    }
                    else -> {
                        null
                    }
                }
                )
            } catch (e: Exception) {
                _mediaItemsFailed.postValue(true)
                Timber.tag(this::javaClass.name).d(e)
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
            (_mediaItems.value ?: return).map { it.url })
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
            val item = (_mediaItems.value ?: return@launch)[position]
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
            } else if (item.mediaType == MediaType.PICTURE) {
                downloadUrl = item.url.formatHtmlEntities()
            }

            DownloadIntentService.enqueueWork(application.applicationContext, downloadUrl)
        }
    }
}