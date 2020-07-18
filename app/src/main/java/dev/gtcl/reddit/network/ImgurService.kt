package dev.gtcl.reddit.network

import android.util.Log
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.gtcl.reddit.models.imgur.ImgurResponse
import dev.gtcl.reddit.models.reddit.CommentsMoshiAdapter
import dev.gtcl.reddit.models.reddit.listing.*
import kotlinx.coroutines.Deferred
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

const val CLIENT_ID = "f6d367a7352ac18"
interface ImgurService {

    @Multipart
    @POST("/3/upload")
    fun uploadImage(
        @Header("Authorization") authorization: String = "Client-ID $CLIENT_ID",
        @Part body: MultipartBody.Part
    ): Deferred<ImgurResponse>

    companion object{
        private val URL = "https://api.imgur.com/".toHttpUrl()

        fun create(): ImgurService{
            val logger = HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger{
                override fun log(message: String) {
                    Log.d("API", message)
                }
            })
            logger.level = HttpLoggingInterceptor.Level.BASIC

            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .build()
                .create(ImgurService::class.java)
        }
    }

}

object ImgurApi{
    val retrofit : ImgurService by lazy{
        ImgurService.create()
    }
}