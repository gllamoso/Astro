package dev.gtcl.reddit.ui.fragments.create_post

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.moshi.JsonDataException
import dev.gtcl.reddit.PostContent
import dev.gtcl.reddit.R
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.models.reddit.listing.Flair
import dev.gtcl.reddit.models.reddit.listing.Rule
import dev.gtcl.reddit.repositories.ImgurRepository
import dev.gtcl.reddit.repositories.SubredditRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL


class CreatePostVM(private val application: RedditApplication): AndroidViewModel(application){

    // Repos
    private val imgurRepository = ImgurRepository.getInstance()
    private val subredditRepository = SubredditRepository.getInstance(application)

    // Scopes
    private val viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _fetchData = MutableLiveData<Boolean?>()
    val fetchData: LiveData<Boolean?>
        get() = _fetchData

    private val _subredditSuggestions = MutableLiveData<List<String>>()
    val subredditSuggestions: LiveData<List<String>>
        get() = _subredditSuggestions

    private val _subredditValid = MutableLiveData<Boolean>()
    val subredditValid: LiveData<Boolean>
        get() = _subredditValid

    private val _rules = MutableLiveData<String?>().apply { value = null }
    val rules: LiveData<String?>
        get() = _rules

    private val _flairs = MutableLiveData<List<Flair>?>().apply { value = null }
    val flairs: LiveData<List<Flair>?>
        get() = _flairs

    private val _flair = MutableLiveData<Flair?>().apply { value = null }
    val flair: LiveData<Flair?>
        get() = _flair

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String>
        get() = _errorMessage

    private val _urlResubmit = MutableLiveData<URL?>()
    val urlResubmit: LiveData<URL?>
        get() = _urlResubmit

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

    fun fetchRules(displayName: String){
        coroutineScope.launch {
            try{
                val rules = subredditRepository.getRules(displayName).await().rules
                val sb = StringBuilder()
                for(rule: Rule in rules){
                    sb.append("${rule.shortName}\n")
                    sb.append("${rule.description}\n\n")
                }
                _rules.value = if(sb.isEmpty()){
                    application.getString(R.string.no_rules_found)
                } else {
                    sb.toString()
                }
            } catch (e: Exception){
                _errorMessage.value = e.toString()
            }
        }
    }

    fun fetchFlairs(srName: String){
        coroutineScope.launch {
            try{
                _flairs.value = subredditRepository.getFlairs(srName).await()
            } catch (e: Exception){
                if(e is HttpException && e.code() == 403){
                    _flairs.value = listOf()
                } else {
                    _errorMessage.value = e.toString()
                }
            }
        }
    }

    fun flairsObserved(){
        _flairs.value = null
    }

    fun rulesObserved(){
        _rules.value = null
    }

    fun selectFlair(flair: Flair?){
        _flair.value = flair
    }

    fun submitTextPost(
        subreddit: String,
        title: String,
        text: String,
        nsfw: Boolean,
        spoiler: Boolean
    ){
        coroutineScope.launch {
            try {
                val test = subredditRepository.submitTextPost(
                    subreddit,
                    title,
                    text,
                    nsfw,
                    spoiler,
                    _flair.value
                ).await()
                Log.d("TAE", "Test: $test")
            } catch (e: Exception){
                _errorMessage.value = e.toString()
            }
        }
    }

    fun submitPhotoPost(
        subreddit: String,
        title: String,
        photo: Uri,
        nsfw: Boolean,
        spoiler: Boolean
    ){
        coroutineScope.launch {
            try{
                val imgurResponse = imgurRepository.uploadImage(createFile(application ,photo)).await()
                val test = subredditRepository.submitUrlPost(
                    subreddit,
                    title,
                    imgurResponse.data.link,
                    nsfw,
                    spoiler,
                    _flair.value,
                    true
                ).await()

                Log.d("TAE", "Test: $test")
            } catch (e: Exception){
                _errorMessage.value = e.toString()
            }
        }
    }

    fun submitUrlPost(
        subreddit: String,
        title: String,
        url: URL,
        nsfw: Boolean,
        spoiler: Boolean,
        resubmit: Boolean = false
        ){
        coroutineScope.launch {
            try {
                val test = subredditRepository.submitUrlPost(
                    subreddit,
                    title,
                    url.toString(),
                    nsfw,
                    spoiler,
                    _flair.value,
                    resubmit
                ).await()
                Log.d("TAE", "Test: $test")
            } catch (e: Exception){
                if(e is JsonDataException && e.localizedMessage.startsWith("Required value 'data' missing")){
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
                    _errorMessage.value = e.toString()
                }
            }
        }
    }

    fun urlResubmitObserved(){
        _urlResubmit.value = null
    }

    companion object{
        fun createFile(context: Context, uri: Uri): File{
            val storageDir = File("${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath}/${context.getText(R.string.app_name)}/upload").apply {
                deleteRecursively()
                mkdirs()
            }
            val file = File.createTempFile("upload", ".jpg", storageDir)
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