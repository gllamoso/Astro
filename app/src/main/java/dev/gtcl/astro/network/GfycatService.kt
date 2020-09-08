package dev.gtcl.astro.network

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.gtcl.astro.models.gfycat.GfycatResponse
import kotlinx.coroutines.Deferred
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import timber.log.Timber

interface GfycatService {

    @GET("v1/gfycats/{gfyid}")
    fun getGfycatInfo(
        @Path("gfyid") gfyid: String
    ): Deferred<GfycatResponse>

    companion object {
        private const val URL = "https://api.gfycat.com/"
        fun create(): GfycatService {
            val logger = HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
                override fun log(message: String) {
                    Timber.tag("API").d(message)
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
                .baseUrl(URL.toHttpUrl())
                .client(client)
                .addConverterFactory(EnumConverterFactory)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .build()
                .create(GfycatService::class.java)
        }
    }
}


object GfycatApi {
    val retrofit: GfycatService by lazy {
        GfycatService.create()
    }
}