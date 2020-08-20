package dev.gtcl.reddit.repositories

import androidx.annotation.MainThread
import dev.gtcl.reddit.models.imgur.ImgurResponse
import dev.gtcl.reddit.network.ImgurApi
import kotlinx.coroutines.Deferred
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class ImgurRepository private constructor(){

    @MainThread
    fun uploadImage(image: File): Deferred<ImgurResponse>{
        val requestFile = image.asRequestBody("multipart/form-data".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("image", image.name, requestFile)
        return ImgurApi.retrofit.uploadImage(body = body)
    }

    @MainThread
    fun getAlbumImages(albumHash: String): Deferred<ImgurResponse> = ImgurApi.retrofit.getAlbumImages(albumHash = albumHash)

    @MainThread
    fun getImage(imageHash: String): Deferred<ImgurResponse> = ImgurApi.retrofit.getImage(imageHash = imageHash)

    companion object{
        private lateinit var INSTANCE: ImgurRepository
        fun getInstance(): ImgurRepository{
            if(!Companion::INSTANCE.isInitialized){
                INSTANCE = ImgurRepository()
            }
            return INSTANCE
        }
    }
}