package dev.gtcl.astro.network

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.gtcl.astro.models.imgur.ImgurResponse
import kotlinx.coroutines.Deferred
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import timber.log.Timber

const val CLIENT_ID = "f6d367a7352ac18"

interface ImgurService {

    @Multipart
    @POST("/3/upload")
    fun uploadImage(
        @Header("Authorization") authorization: String = "Client-ID $CLIENT_ID",
        @Part body: MultipartBody.Part
    ): Deferred<ImgurResponse>

    @GET("/3/album/{albumHash}")
    fun getAlbumImages(
        @Header("Authorization") authorization: String = "Client-ID $CLIENT_ID",
        @Path("albumHash") albumHash: String
    ): Deferred<ImgurResponse>

    @GET("/3/image/{imageHash}")
    fun getImage(
        @Header("Authorization") authorization: String = "Client-ID $CLIENT_ID",
        @Path("imageHash") imageHash: String
    ): Deferred<ImgurResponse>

    companion object {
        private val URL = "https://api.imgur.com/".toHttpUrl()

        fun create(): ImgurService {
            val logger = HttpLoggingInterceptor { message -> Timber.tag("API").d(message) }
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

object ImgurApi {
    val retrofit: ImgurService by lazy {
        ImgurService.create()
    }
}