package dev.gtcl.reddit.network

import android.util.Log
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.gtcl.reddit.*
import dev.gtcl.reddit.listings.*
import dev.gtcl.reddit.listings.comments.Child
import dev.gtcl.reddit.listings.comments.CommentAdapter
import dev.gtcl.reddit.listings.comments.CommentPage
import dev.gtcl.reddit.listings.users.AccessToken
import kotlinx.coroutines.Deferred
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
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
        @Field("redirect_uri") redirectUri: String
    ) : Deferred<AccessToken>

    @FormUrlEncoded
    @POST("/api/v1/access_token")
    @Headers("User-Agent: Sample App")
    fun getAccessToken(
        @Header("Authorization") authorization: String,
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("refresh_token") refreshToken: String
    ): Deferred<AccessToken>

    @GET("/api/v1/me")
    fun getCurrentAccountInfo(
        @Header("Authorization") authorization: String
    ): Deferred<Account>

    @GET("/user/{user}/about/.json")
    fun getUserInfo(
        @Header("Authorization") authorization: String?,
        @Path("user") user: String
    ) : Deferred<AccountChild>

    @POST("/api/vote/")
    fun vote(
        @Header("Authorization") authorization: String,
        @Query("id") id: String,
        @Query("dir") dir: Int
    ): Call<Void>

    @POST("/api/save/")
    fun save(
        @Header("Authorization") authorization: String,
        @Query("id") id: String
    ): Call<Void>

    @POST("/api/unsave/")
    fun unsave(
        @Header("Authorization") authorization: String,
        @Query("id") id: String
    ): Call<Void>

    @POST("/api/hide")
    fun hide(
        @Header("Authorization") authorization: String,
        @Query("id") id: String
    ): Call<Void>

    @POST("/api/unhide")
    fun unhide(
        @Header("Authorization") authorization: String,
        @Query("id") id: String
    ): Call<Void>

//     ____   __   ____  ____  ____
//    (  _ \ /  \ / ___)(_  _)/ ___)
//     ) __/(  O )\___ \  )(  \___ \
//    (__)   \__/ (____/ (__) (____/

    @GET("/{sort}/.json")
    fun getPostFromFrontPage(
        @Header("Authorization") authorization: String?,
        @Path("sort") sort: PostSort,
        @Query("t") t: Time?,
        @Query("after") after: String? = null,
        @Query("limit") limit: Int
    ): Deferred<ListingResponse>

    @GET("/user/{user}/m/{multi}/{sort}.json")
    fun getPostFromMultiReddit(
        @Header("Authorization") authorization: String?,
        @Path("user") user: String,
        @Path("multi") multi: String,
        @Path("sort") sort: PostSort,
        @Query("t") t: Time?,
        @Query("after") after: String? = null,
        @Query("limit") limit: Int
    ): Deferred<ListingResponse>

    /**
     * categories: posts, saved, hidden, upvoted, downvoted, awards received, awards given
     */
    @GET("/user/{user}/{where}/.json")
    fun getPostsFromUser(
        @Header("Authorization") authorization: String?,
        @Path("user") user: String,
        @Path("where") where: ProfileInfo,
        @Query("after") after: String? = null,
        @Query("limit") limit: Int
    ): Deferred<ListingResponse>

    @GET("/user/{user}/.json")
    fun getUserOverview(
        @Header("Authorization") authorization: String?,
        @Path("user") user: String,
        @Query("after") after: String? = null,
        @Query("limit") limit: Int
    ): Deferred<ListingResponse>

    @GET("/r/{subreddit}/{sort}.json")
    fun getPostsFromSubreddit(
        @Header("Authorization") authorization: String?,
        @Path("subreddit") subreddit: String,
        @Path("sort") sort: PostSort,
        @Query("t") t: Time?,
        @Query("after") after: String? = null,
        @Query("limit") limit: Int
    ): Deferred<ListingResponse>

//     ____  _  _  ____  ____
//    / ___)/ )( \(  _ \/ ___)
//    \___ \) \/ ( ) _ (\___ \
//    (____/\____/(____/(____/

    @GET("/subreddits/{where}.json")
    fun getSubreddits(
        @Header("Authorization") authorization: String? = null,
        @Path("where") where: SubredditWhere,
        @Query("after") after: String? = null,
        @Query("limit") limit: Int? = null
    ): Deferred<ListingResponse>

    @GET("/subreddits/mine/{where}.json")
    fun getSubredditsOfMine(
        @Header("Authorization") authorization: String? = null,
        @Path("where") where: SubredditMineWhere,
        @Query("after") after: String? = null,
        @Query("limit") limit: Int? = 100,
        @Query("show") show: String? = "all"
    ): Deferred<ListingResponse>

    @GET("/subreddits/search.json")
    fun getSubredditsSearch(
        @Query("q") q: String,
        @Query("include_over_18") nsfw: String
    ): Deferred<ListingResponse>

    @GET("/api/search_reddit_names.json")
    fun getSubredditNameSearch(
        @Query("exact") exact: Boolean = false,
        @Query("include_over_18") nsfw: Boolean = true,
        @Query("include_unadvertisable") includeUnadvertisable: Boolean = true, // If set to False, subs that have "hide_ads" set to True or are on on the "anti_ads_subreddits" list will be filtered
        @Query("query") query: String
    ): Deferred<SubredditNamesResponse>

    @GET("/api/subreddit_autocomplete_v2.json")
    fun getSubredditNameSearch(
        @Header("Authorization") authorization: String? = null,
        @Query("include_over_18") nsfw: Boolean,
        @Query("include_profiles") includeProfiles: Boolean,
        @Query("limit") limit: Int = 5,
        @Query("query") query: String
    ): Deferred<ListingResponse>

    @POST("/api/subscribe")
    fun subscribeToSubreddit(
        @Header("Authorization") authorization: String? = null,
        @Query("action") action: SubscribeAction,
        @Query("sr_name") srName: String
    ): Call<Void>

//     ___  __   _  _  _  _  ____  __ _  ____  ____
//    / __)/  \ ( \/ )( \/ )(  __)(  ( \(_  _)/ ___)
//   ( (__(  O )/ \/ \/ \/ \ ) _) /    /  )(  \___ \
//    \___)\__/ \_)(_/\_)(_/(____)\_)__) (__) (____/

    @GET
    fun getPostAndComments(
        @Header("Authorization") authorization: String? = null,
        @Url permalink: String,
        @Query("sort") sort: CommentSort,
        @Query("limit") limit: Int
    ): Deferred<CommentPage>

    @GET
    fun getComments(
        @Header("Authorization") authorization: String? = null,
        @Url permalink: String,
        @Query("sort") sort: CommentSort
    ): Deferred<List<Item>>

    @GET("/api/morechildren/")
    fun getMoreComments(
        @Header("Authorization") authorization: String? = null,
        @Query("children") children: String,
        @Query("link_id") linkId: String,
        @Query("api_type") apiType: String = "json",
        @Query("limit_children") limitChildren: Boolean = false,
        @Query("sort") sort: CommentSort
    ): Deferred<List<Child>>

    // Awards
    @GET("/api/v1/user/{user}/trophies/.json")
    fun getAwards(
        @Header("Authorization") authorization: String? = null,
        @Path("user") user: String
    ): Deferred<TrophyListingResponse>

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
                .add(PolymorphicJsonAdapterFactory.of(ListingChild::class.java, "kind") // TODO: Finish
                    .withSubtype(CommentChild::class.java, "t1")
                    .withSubtype(PostChild::class.java, "t3")
                    .withSubtype(SubredditChild::class.java, "t5")
                    .withSubtype(MoreChild::class.java, "more"))
                .add(KotlinJsonAdapterFactory())
                .build()


            return Retrofit.Builder()
                .baseUrl(httpUrl)
                .client(client)
                .addConverterFactory(EnumConverterFactory())
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .build()
                .create(RedditApiService::class.java)
        }

    }
}

object RedditApi {
    val base : RedditApiService by lazy {
        RedditApiService.createWithNoAuth()
    }

    val oauth : RedditApiService by lazy {
        RedditApiService.createWithAuth()
    }
}