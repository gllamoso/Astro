package dev.gtcl.astro.ui.fragments.create_post

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.moshi.JsonDataException
import dev.gtcl.astro.*
import dev.gtcl.astro.models.reddit.NewPostData
import dev.gtcl.astro.models.reddit.listing.Flair
import dev.gtcl.astro.models.reddit.listing.Post
import dev.gtcl.astro.repositories.ImgurRepository
import dev.gtcl.astro.repositories.reddit.MiscRepository
import dev.gtcl.astro.repositories.reddit.SubredditRepository
import kotlinx.coroutines.*
import retrofit2.HttpException
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URI
import java.net.URL

class CreatePostVM(private val application: AstroApplication): AstroViewModel(application){

    private val _fetchInput = MutableLiveData<Boolean?>()
    val fetchInput: LiveData<Boolean?>
        get() = _fetchInput

    private val _subredditSuggestions = MutableLiveData<List<String>>()
    val subredditSuggestions: LiveData<List<String>>
        get() = _subredditSuggestions

    private val _subredditValid = MutableLiveData<Boolean>()
    val subredditValid: LiveData<Boolean>
        get() = _subredditValid

    private val _flair = MutableLiveData<Flair?>().apply { value = null }
    val flair: LiveData<Flair?>
        get() = _flair

    private val _urlResubmit = MutableLiveData<URL?>()
    val urlResubmit: LiveData<URL?>
        get() = _urlResubmit

    private val _newPostData = MutableLiveData<NewPostData?>()
    val newPostData: LiveData<NewPostData?>
        get() = _newPostData

    private val _postContent = MutableLiveData<PostContent?>()
    val postContent: LiveData<PostContent?>
        get() = _postContent

    private val _loading = MutableLiveData<Boolean>().apply { value = false }
    val loading: LiveData<Boolean>
        get() = _loading

    fun fetchData(){
        _fetchInput.value = true
    }

    fun dataFetched(){
        _fetchInput.value = null
    }

    fun setPostContent(postContent: PostContent){
        _postContent.value = postContent
    }

    fun postContentObserved(){
        _postContent.value = null
    }

    fun newPostObserved(){
        _newPostData.value = null
    }

    fun searchSubreddits(q: String){
        coroutineScope.launch {
            _subredditSuggestions.value = subredditRepository.searchMySubscriptionsExcludingMultireddits(q).map { it.name }
        }
    }

    fun validateSubreddit(displayName: String){
        coroutineScope.launch {
            try {
                subredditRepository.getSubreddit(displayName).await()
                _subredditValid.value = true
            } catch (e: Exception){
                _subredditValid.value = false
            }
        }
    }

    fun selectFlair(flair: Flair?){
        _flair.value = flair
    }

    fun submitTextPost(
        subreddit: String,
        title: String,
        text: String,
        notifications: Boolean,
        nsfw: Boolean,
        spoiler: Boolean
    ){
        coroutineScope.launch {
            try {
                _loading.value = true
                val newPostResponse = subredditRepository.submitTextPost(
                    subreddit,
                    title,
                    text,
                    nsfw,
                    spoiler,
                    _flair.value
                ).await()

                if(!notifications){
                    val sendNotificationsResponse = miscRepository.sendRepliesToInbox(newPostResponse.json.data.name, notifications).await()
                    if(!sendNotificationsResponse.isSuccessful){
                        throw HttpException(sendNotificationsResponse)
                    }
                }

                _newPostData.value = newPostResponse.json.data
            } catch (e: Exception){
                _errorMessage.value = e.getErrorMessage(application)
            } finally {
                _loading.value = false
            }
        }
    }

    fun submitPhotoPost(
        subreddit: String,
        title: String,
        photo: Uri,
        notifications: Boolean,
        nsfw: Boolean,
        spoiler: Boolean
    ){
        coroutineScope.launch {
            val file = createFile(application,photo) ?: return@launch
            try{
                _loading.value = true
                val imgurResponse = imgurRepository.uploadImage(file).await()
                val newPostResponse = subredditRepository.submitUrlPost(
                    subreddit,
                    title,
                    imgurResponse.data.link,
                    nsfw,
                    spoiler,
                    _flair.value,
                    true
                ).await()

                if(!notifications){
                    val sendNotificationsResponse = miscRepository.sendRepliesToInbox(newPostResponse.json.data.name, notifications).await()
                    if(!sendNotificationsResponse.isSuccessful){
                        throw HttpException(sendNotificationsResponse)
                    }
                }

                _newPostData.value = newPostResponse.json.data
            } catch (e: Exception){
                _errorMessage.value = e.getErrorMessage(application)
            } finally {
                file.delete()
                _loading.value = false
            }
        }
    }

    fun submitUrlPost(
        subreddit: String,
        title: String,
        url: URL,
        notifications: Boolean,
        nsfw: Boolean,
        spoiler: Boolean,
        resubmit: Boolean = false
        ){
        coroutineScope.launch {
            try {
                _loading.value = true
                val newPostResponse = subredditRepository.submitUrlPost(
                    subreddit,
                    title,
                    url.toString(),
                    nsfw,
                    spoiler,
                    _flair.value,
                    resubmit
                ).await()

                if(!notifications){
                    val sendNotificationsResponse = miscRepository.sendRepliesToInbox(newPostResponse.json.data.name, notifications).await()
                    if(!sendNotificationsResponse.isSuccessful){
                        throw HttpException(sendNotificationsResponse)
                    }
                }

                _newPostData.value = newPostResponse.json.data
            } catch (e: Exception){
                if(e is JsonDataException && e.localizedMessage?.startsWith("Required value 'data' missing") == true){
                    try{
                        val errorResponse = subredditRepository.submitUrlPostForErrors(
                            subreddit,
                            title,
                            url.toString(),
                            nsfw,
                            spoiler,
                            _flair.value
                        ).await()
                        val errorMessage = errorResponse.json.errors[0][1]
                        if(errorMessage == "that link has already been submitted"){
                            _urlResubmit.value = url
                        } else {
                            _errorMessage.value = errorMessage[0].toUpperCase() + errorMessage.substring(1)
                        }
                    } catch (e2: Exception){
                        _errorMessage.value = application.getString(R.string.unable_fetch_error)
                    }
                } else {
                    _errorMessage.value = e.getErrorMessage(application)
                }
            } finally {
                _loading.value = false
            }
        }
    }

    fun submitCrosspost(subreddit: String,
                        title: String,
                        notifications: Boolean,
                        nsfw: Boolean,
                        spoiler: Boolean,
                        crossPost: Post){

        coroutineScope.launch {
            try {
                _loading.value = true
                val newPostResponse = subredditRepository.submitCrosspost(
                    subreddit,
                    title,
                    nsfw,
                    spoiler,
                    _flair.value,
                    crossPost
                ).await()

                if(!notifications){
                    val sendNotificationsResponse = miscRepository.sendRepliesToInbox(newPostResponse.json.data.name, notifications).await()
                    if(!sendNotificationsResponse.isSuccessful){
                        throw HttpException(sendNotificationsResponse)
                    }
                }

                _newPostData.value = newPostResponse.json.data
            } catch (e: Exception){
                _errorMessage.value = e.getErrorMessage(application)
            } finally {
                _loading.value = false
            }
        }

    }

    fun urlResubmitObserved(){
        _urlResubmit.value = null
    }

    companion object{
        fun createFile(context: Context, uri: Uri): File?{
            val folder = context.getExternalFilesDirs(Environment.DIRECTORY_DOWNLOADS)[0] ?: return null
            val file = File.createTempFile("upload", ".jpg", folder)
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream.use { input ->
                val outputStream = FileOutputStream(file)
                outputStream.use {output ->
                    val buffer = ByteArray(4 * 1024)
                    while(true){
                        val byteCount = input?.read(buffer)
                        if(byteCount ?: -1 < 0) break
                        output.write(buffer, 0, byteCount!!)
                    }
                    output.flush()
                }
            }
            return file
        }
    }
}