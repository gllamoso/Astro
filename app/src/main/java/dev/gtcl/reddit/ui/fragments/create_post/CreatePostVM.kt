package dev.gtcl.reddit.ui.fragments.create_post

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.reddit.PostContent
import dev.gtcl.reddit.RedditApplication

class CreatePostVM(private val application: RedditApplication): AndroidViewModel(application){

    private val _fetchData = MutableLiveData<Boolean?>()
    val fetchData: LiveData<Boolean?>
        get() = _fetchData

    fun fetchData(){
        _fetchData.value = true
    }

    fun dataFetched(){
        _fetchData.value = null
    }

    private val _postContent = MutableLiveData<PostContent?>()
    val postContent: LiveData<PostContent?>
        get() = _postContent

    fun setPostContent(postContent: PostContent){
        _postContent.value = postContent
    }

    fun postContentObserved(){
        _postContent.value = null
    }

}