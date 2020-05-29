package dev.gtcl.reddit.ui.fragments.media

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.reddit.R
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.URL_KEY
import dev.gtcl.reddit.download.DownloadIntentService
import dev.gtcl.reddit.models.reddit.Post
import dev.gtcl.reddit.models.reddit.UrlType

class MediaDialogVM(private val application: RedditApplication) : AndroidViewModel(application){

    var initialized = false

    lateinit var url: String

    private val _urlType = MutableLiveData<UrlType>()
    val urlType: LiveData<UrlType>
        get() = _urlType

    fun setUrlType(urlType: UrlType){
        _urlType.value = urlType
    }

    private val _post = MutableLiveData<Post?>()
    val post: LiveData<Post?>
        get() = _post

    fun setPost(post: Post?){
        _post.value = post
    }

    private val _showUi = MutableLiveData<Boolean>()
    val showUi: LiveData<Boolean>
        get() = _showUi

    fun setShowUi(show: Boolean){
        _showUi.value = show
    }

    fun alternateShowUi(){
        _showUi.value = !_showUi.value!!
    }

    fun download(){
        val serviceIntent = Intent(application.applicationContext, DownloadIntentService::class.java)
        serviceIntent.putExtra(URL_KEY, url)
        DownloadIntentService.enqueueWork(application.applicationContext, serviceIntent)
        Toast.makeText(application, application.getText(R.string.downloading), Toast.LENGTH_SHORT).show()
    }
}