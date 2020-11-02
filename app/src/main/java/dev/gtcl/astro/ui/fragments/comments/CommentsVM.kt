package dev.gtcl.astro.ui.fragments.comments

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.astro.*
import dev.gtcl.astro.models.reddit.listing.*
import dev.gtcl.astro.network.MoreComments
import dev.gtcl.astro.url.REDDIT_COMMENTS_REGEX
import dev.gtcl.astro.url.UrlType
import dev.gtcl.astro.url.getFirstGroup
import kotlinx.coroutines.launch
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

    private val _previewImg = MutableLiveData<String?>().apply { value = null }
    val previewImg: LiveData<String?>
        get() = _previewImg

    private val _previewType = MutableLiveData<UrlType?>()
    val previewType: LiveData<UrlType?>
        get() = _previewType

    private val _showPreviewIcon = MutableLiveData<Boolean>().apply { value = false }
    val showPreviewIcon: LiveData<Boolean>
        get() = _showPreviewIcon

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
            post.parseSelftext()
            _post.postValue(post)
            val preview = post.getPreviewImage() ?: post.getThumbnail(false) ?: ""
            _previewImg.postValue(preview)
            _previewType.postValue(post.urlType)
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
                    commentPage.post.parseSelftext()
                    _post.postValue(commentPage.post)
                    val preview = commentPage.post.getPreviewImage() ?: commentPage.post.getThumbnail(false) ?: ""
                    _previewImg.postValue(preview)
                    _previewType.postValue(commentPage.post.urlType)
                }
                if (!isFullContext) {
                    fullContextLink = (REDDIT_COMMENTS_REGEX.getFirstGroup(permalink)
                        ?: return@launch)
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
            if (_loading.value == true || position == -1) {
                if (position != -1) {
                    _notifyAt.value = position
                }
                return@launch
            }
            val moreItem = _comments.value?.get(position)
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
                    _comments.value?.removeAt(position)
                    _removeAt.postValue(position)
                }
                _moreComments.postValue(
                    MoreComments(
                        position,
                        comments
                    )
                )

                _comments.value?.addAll(position, comments)
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
        val itemInPosition = comments.value!![position]
        val depth = when (val currItem = comments.value!![position]) {
            is Comment -> currItem.depth ?: 0
            is More -> currItem.depth
            else -> 0
        }
        var i = position
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
        return if (i - 1 > position) {
            val listToHide = comments.value!!.subList(position + 1, i).toList()
            for (j in listToHide.indices) {
                comments.value!!.removeAt(position + 1)
            }
            hiddenItemsMap[itemInPosition.name] = listToHide
            listToHide.size
        } else {
            0
        }
    }

    fun unhideItems(position: Int): List<Item> {
        val itemInPosition = comments.value!![position]
        val hiddenItems = hiddenItemsMap[itemInPosition.name]
        return if (hiddenItems != null) {
            comments.value!!.addAll(position + 1, hiddenItems)
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

    fun showPreviewIcon(show: Boolean){
        _showPreviewIcon.value = show
    }
}