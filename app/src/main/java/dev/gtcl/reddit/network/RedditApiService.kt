package dev.gtcl.reddit.network

import android.util.Log
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.gtcl.reddit.users.AccessToken
import dev.gtcl.reddit.comments.CommentAdapter
import dev.gtcl.reddit.comments.Child
import dev.gtcl.reddit.comments.CommentItem
import dev.gtcl.reddit.comments.CommentPage
import dev.gtcl.reddit.posts.PostListingResponse
import dev.gtcl.reddit.subs.SubredditListingResponse
import dev.gtcl.reddit.users.User
import kotlinx.coroutines.Deferred
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

interface RedditApiService {

    @FormUrlEncoded
    @POST("/api/v1/access_token")
    @Headers("User-Agent: Sample App")
    fun postCode(
        @Header("Authorization") authorization: String,
        @Field("grant_type") grantType: String = "authorization_code",
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String) : Deferred<AccessToken>

    @FormUrlEncoded
    @POST("/api/v1/access_token")
    @Headers("User-Agent: Sample App")
    fun getAccessToken(
        @Header("Authorization") authorization: String,
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("refresh_token") refreshToken: String): Deferred<AccessToken>

    @GET("/api/v1/me")
    fun getCurrentUserInfo(
        @Header("Authorization") authorization: String): Deferred<User>

//     ____   __   ____  ____  ____
//    (  _ \ /  \ / ___)(_  _)/ ___)
//     ) __/(  O )\___ \  )(  \___ \
//    (__)   \__/ (____/ (__) (____/

    @GET("/.json")
    fun getFrontPage(
        @Header("Authorization") authorization: String?): Deferred<PostListingResponse>

    @GET("/r/{subreddit}/{sort}.json")
    fun getPostsFromSubreddit(
        @Path("subreddit") subreddit: String,
        @Path("sort") sort: String,
        @Query("t") t: String?,
        @Query("after") after: String? = null,
        @Query("limit") limit: Int): Deferred<PostListingResponse>

//     ____  _  _  ____  ____
//    / ___)/ )( \(  _ \/ ___)
//    \___ \) \/ ( ) _ (\___ \
//    (____/\____/(____/(____/

    @GET("/subreddits/{where}.json")
    fun getSubreddits(
        @Path("where") where: String,
        @Query("after") after: String? = null,
        @Query("limit") limit: Int? = null): Deferred<SubredditListingResponse>

    @GET("/subreddits/mine/{where}.json")
    fun getSubredditsOfMine(
        @Header("Authorization") authorization: String? = null,
        @Path("where") where: String,
        @Query("after") after: String? = null,
        @Query("limit") limit: Int? = 100,
        @Query("show") show: String? = "all"): Deferred<SubredditListingResponse>

    @GET("/subreddits/search.json")
    fun getSubredditsSearch(
        @Query("q") q: String,
        @Query("include_over_18") nsfw: String
    ): Deferred<SubredditListingResponse>

//     ___  __   _  _  _  _  ____  __ _  ____  ____
//    / __)/  \ ( \/ )( \/ )(  __)(  ( \(_  _)/ ___)
//   ( (__(  O )/ \/ \/ \/ \ ) _) /    /  )(  \___ \
//    \___)\__/ \_)(_/\_)(_/(____)\_)__) (__) (____/

    @GET
    fun getPostAndComments(
        @Header("Authorization") authorization: String? = null,
        @Url permalink: String,
        @Query("sort") sort: String,
        @Query("limit") limit: Int
    ): Deferred<CommentPage>

    @GET
    fun getComments(
        @Header("Authorization") authorization: String? = null,
        @Url permalink: String,
        @Query("sort") sort: String
    ): Deferred<List<CommentItem>>

    @GET("/api/morechildren/")
    fun getMoreComments(
        @Header("Authorization") authorization: String? = null,
        @Query("children") children: String,
        @Query("link_id") linkId: String,
        @Query("api_type") apiType: String = "json",
        @Query("limit_children") limitChildren: Boolean = false,
        @Query("sort") sort: String
    ): Deferred<List<Child>>


    companion object {
        private const val BASE_URL = "https://www.reddit.com/"
        private const val OAUTH_URL = "https://oauth.reddit.com/"
        fun createWithNoAuth(): RedditApiService = create(BASE_URL.toHttpUrl())
        fun createWithAuth(): RedditApiService = create(OAUTH_URL.toHttpUrl())
        fun create(httpUrl: HttpUrl): RedditApiService {
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
                .add(CommentAdapter())
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(httpUrl)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .build()
                .create(RedditApiService::class.java)
        }

    }
}

object RedditApi {
    val retrofitServiceWithNoAuth : RedditApiService by lazy {
        RedditApiService.createWithNoAuth()
    }

    val retrofitServiceWithAuth : RedditApiService by lazy {
        RedditApiService.createWithAuth()
    }
}