package dev.gtcl.reddit.ui.fragments.comments

import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import dev.gtcl.reddit.*
import dev.gtcl.reddit.models.reddit.MediaURL
import dev.gtcl.reddit.models.reddit.MoreComments
import dev.gtcl.reddit.models.reddit.listing.*
import dev.gtcl.reddit.repositories.ListingRepository
import dev.gtcl.reddit.repositories.GfycatRepository
import dev.gtcl.reddit.repositories.ImgurRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

const val CHILDREN_PER_FETCH = 50
class CommentsVM(val application: RedditApplication): AndroidViewModel(application) {

    // Repos
    private val listingRepository = ListingRepository.getInstance(application)
    private val gfycatRepository = GfycatRepository.getInstance()
    private val imgurRepository = ImgurRepository.getInstance()

    // Scopes
    private var viewModelJob = Job()
    private var coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val hiddenItemsMap = HashMap<String, List<Item>>()

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

    private val _allCommentsFetched = MutableLiveData<Boolean>()
    val allCommentsFetched: LiveData<Boolean>
        get() = _allCommentsFetched

    private val _loading = MutableLiveData<Boolean>().apply { value = true }
    val loading: LiveData<Boolean>
        get() = _loading

    private val _commentSort = MutableLiveData<CommentSort>()
    val commentSort: LiveData<CommentSort>
        get() = _commentSort

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?>
        get() = _errorMessage

    private val _removeAt = MutableLiveData<Int?>().apply { value = null }
    val removeAt: LiveData<Int?>
        get() = _removeAt

    private val _mediaItems = MutableLiveData<List<MediaURL>?>().apply { value = null }
    val mediaItems: LiveData<List<MediaURL>?>
        get() = _mediaItems

    private val pageSize = 15

    var contentInitialized = false

    init {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(application)
        val defaultSort = sharedPref.getString("default_comment_sort", application.getString(R.string.order_best))
        val sortArray = application.resources.getStringArray(R.array.comment_sort_entries)
        _commentSort.value = when(sortArray.indexOf(defaultSort)){
            1 -> CommentSort.TOP
            2 -> CommentSort.NEW
            3 -> CommentSort.CONTROVERSIAL
            4 -> CommentSort.OLD
            5 -> CommentSort.QA
            6 -> CommentSort.RANDOM
            else -> CommentSort.BEST
        }
    }

    fun setPost(post: Post){
        _post.value = post
        fetchComments(post.permalink)
    }

    fun setCommentSort(sort: CommentSort){
        _commentSort.value = sort
    }

    fun fetchComments(permalink: String = post.value!!.permalink, refreshPost: Boolean = true){
        coroutineScope.launch {
            try{
                _loading.value = true
                val commentPage = listingRepository.getPostAndComments(permalink, _commentSort.value!!, pageSize * 3).await()
                if(refreshPost){
                    _post.value = commentPage.post
                }
                _allCommentsFetched.value = permalink.endsWith(commentPage.post.permalink)
                _comments.value = commentPage.comments.toMutableList()
                _commentsFetched = true
                _loading.value = false
            } catch (e: Exception){
                _errorMessage.value = e.getErrorMessage(application)
            }
        }
    }

    fun fetchMoreComments(position: Int){
        coroutineScope.launch {
            val positionOffset = position + if(allCommentsFetched.value == false) -1 else 0
            if(_loading.value == true || position == -1) {
                return@launch
            }
            val moreItem = _comments.value?.get(positionOffset)
            if(moreItem == null || moreItem !is More){
                throw IllegalArgumentException("Invalid more item: $moreItem")
            }
            val children = moreItem.pollChildrenAsValidString(CHILDREN_PER_FETCH)
            try{
                _loading.value = true
                val comments = listingRepository.getMoreComments(children, post.value!!.name, _commentSort.value!!).await().json.data.things.map { it.data }.filter { !(it is More && it.depth == 0) }
                if(moreItem.lastChildFetched){
                    _comments.value?.removeAt(positionOffset)
                    _removeAt.value = position
                }
                _moreComments.value = MoreComments(
                    position,
                    comments
                )

                _comments.value?.addAll(positionOffset, comments)
                _loading.value = false
            } catch (e: Exception){
                moreItem.undoChildrenPoll()
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

    fun addItems(position: Int, items: List<Item>){
        _comments.value?.addAll(position, items)
    }

    fun errorMessageObserved(){
        _errorMessage.value = null
    }

    fun hideItems(position: Int): Int{
        val positionOffset = position + if(allCommentsFetched.value == false) -1 else 0
        val itemInPosition = comments.value!![positionOffset]
        val depth = when(val currItem = comments.value!![positionOffset]){
            is Comment -> currItem.depth ?: 0
            is More -> currItem.depth
            else -> 0
        }
        var i = positionOffset
        while(++i < comments.value!!.size - 1){
            val currDepth = when(val currItem = comments.value!![i]){
                is Comment -> currItem.depth ?: 0
                is More -> currItem.depth
                else -> 0
            }
            if(currDepth <= depth){
                break
            }
        }
        return if(i - 1 > positionOffset){
            val listToHide = comments.value!!.subList(positionOffset + 1, i).toList()
            for(j in listToHide.indices){
                comments.value!!.removeAt(positionOffset + 1)
            }
            hiddenItemsMap[itemInPosition.name] = listToHide
            listToHide.size
        } else {
            0
        }
    }

    fun unhideItems(position: Int): List<Item>{
        val positionOffset = position + if(allCommentsFetched.value == false) -1 else 0
        val itemInPosition = comments.value!![positionOffset]
        val hiddenItems = hiddenItemsMap[itemInPosition.name]
        return if(hiddenItems != null){
            comments.value!!.addAll(positionOffset + 1, hiddenItems)
            hiddenItemsMap.remove(itemInPosition.name)
            hiddenItems
        } else {
            listOf()
        }
    }

    fun fetchMediaItems(post: Post) {

        coroutineScope.launch {
            try {
                _loading.value = true
                _mediaItems.value = when (val urlType = post.urlType) {
                    UrlType.IMAGE -> {
                        listOf(MediaURL(post.url!!, MediaType.PICTURE))
                    }
                    UrlType.GIF -> {
                        listOf(MediaURL(post.url!!, MediaType.GIF))
                    }
                    UrlType.HLS, UrlType.GIFV, UrlType.STANDARD_VIDEO, UrlType.REDDIT_VIDEO -> {
                        if(post.previewVideoUrl != null){
                            listOf(MediaURL(post.previewVideoUrl!!, MediaType.VIDEO))
                        } else {
                            val url = if(urlType == UrlType.GIFV) {
                                post.url!!.replace(".gifv", ".mp4")
                            } else {
                                post.url!!
                            }
                            listOf(MediaURL(url, MediaType.VIDEO))
                        }
                    }
                    UrlType.GFYCAT -> {
                        val videoUrl = gfycatRepository.getGfycatInfo(
                            post.url!!.replace("http[s]?://gfycat.com/".toRegex(), "")
                        )
                            .await()
                            .gfyItem
                            .mobileUrl
                        listOf(MediaURL(videoUrl, MediaType.VIDEO, post.previewVideoUrl))
                    }
                    UrlType.REDGIFS -> {
                        val videoUrl = gfycatRepository.getGfycatInfoFromRedgifs(
                            post.url!!.replace(
                                "http[s]?://(www\\.)?redgifs.com/watch/".toRegex(),
                                ""
                            )
                        )
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
                    else -> {
                        listOf()
                    }
                }

                _loading.value = false
            } catch (e: Exception) {
                _errorMessage.value = e.getErrorMessage(application)
            }
        }
    }
}