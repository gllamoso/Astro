package dev.gtcl.reddit.network

import android.util.Log
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.gtcl.reddit.*
import dev.gtcl.reddit.models.reddit.*
import dev.gtcl.reddit.models.reddit.AccessToken
import dev.gtcl.reddit.models.reddit.listing.*
import kotlinx.coroutines.Deferred
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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
    ): Deferred<Response<Unit>>

    @POST("/api/save/")
    fun save(
        @Header("Authorization") authorization: String,
        @Query("id") id: String
    ): Deferred<Response<Unit>>

    @POST("/api/unsave/")
    fun unsave(
        @Header("Authorization") authorization: String,
        @Query("id") id: String
    ): Deferred<Response<Unit>>

    @POST("/api/hide")
    fun hide(
        @Header("Authorization") authorization: String,
        @Query("id") id: String
    ): Deferred<Response<Unit>>

    @POST("/api/unhide")
    fun unhide(
        @Header("Authorization") authorization: String,
        @Query("id") id: String
    ): Deferred<Response<Unit>>

//     _____          _
//    |  __ \        | |
//    | |__) |__  ___| |_ ___
//    |  ___/ _ \/ __| __/ __|
//    | |  | (_) \__ \ |_\__ \
//    |_|   \___/|___/\__|___/
//    http://patorjk.com/software/taag/#p=display&f=Big&t=Posts

    @GET("/{sort}/.json")
    fun getPostFromFrontPage(
        @Header("Authorization") authorization: String?,
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

    @GET("/r/{subreddit}/{sort}.json")
    fun getPostsFromSubreddit(
        @Header("Authorization") authorization: String?,
        @Path("subreddit") subreddit: String,
        @Path("sort") sort: PostSort,
        @Query("t") t: Time?,
        @Query("after") after: String? = null,
        @Query("limit") limit: Int
    ): Deferred<ListingResponse>

    @POST("/api/submit")
    fun submitPost(
        @Header("Authorization") authorization: String,
        @Query("sr") subreddit: String,
        @Query("kind") kind: PostType,
        @Query("title") title: String,
        @Query("text") text: String?,
        @Query("url") url: String?,
        @Query("nsfw") nsfw: Boolean,
        @Query("spoiler") spoiler: Boolean,
        @Query("flair_id") flairId: String?,
        @Query("flair_text") flairText: String?,
        @Query("resubmit") resubmit: Boolean,
        @Query("api_type") apiType: String = "json"
    ): Deferred<PostSubmittedResponse>

    @POST("/api/submit")
    fun submitPostForError(
        @Header("Authorization") authorization: String,
        @Query("sr") subreddit: String,
        @Query("kind") kind: PostType,
        @Query("title") title: String,
        @Query("text") text: String?,
        @Query("url") url: String?,
        @Query("nsfw") nsfw: Boolean,
        @Query("spoiler") spoiler: Boolean,
        @Query("flair_id") flairId: String?,
        @Query("flair_text") flairText: String?,
        @Query("api_type") apiType: String = "json"
    ): Deferred<ErrorResponse>

    @POST("/api/sendreplies")
    fun setSendReplies(
        @Header("Authorization") authorization: String,
        @Query("id") id: String,
        @Query("state") state: Boolean
    ): Deferred<Response<Unit>>

//      _____       _
//     / ____|     | |
//    | (___  _   _| |__  ___
//     \___ \| | | | '_ \/ __|
//     ____) | |_| | |_) \__ \
//    |_____/ \__,_|_.__/|___/

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

    @GET("/api/subreddit_autocomplete_v2.json")
    fun getSubredditNameSearch(
        @Header("Authorization") authorization: String? = null,
        @Query("include_over_18") nsfw: Boolean,
        @Query("include_profiles") includeProfiles: Boolean,
        @Query("limit") limit: Int = 5,
        @Query("query") query: String
    ): Deferred<ListingResponse>

    @POST("/api/subscribe")
    fun subscribe(
        @Header("Authorization") authorization: String? = null,
        @Query("action") action: SubscribeAction,
        @Query("sr_name") srName: String
    ): Deferred<Response<Unit>>

    @GET("/r/{displayName}/about")
    fun getSubredditInfo(
        @Header("Authorization") authorization: String? = null,
        @Path("displayName") displayName: String
    ): Deferred<SubredditChild>

    @GET("/r/{displayName}/about/rules.json")
    fun getSubredditRules(
        @Header("Authorization") authorization: String? = null,
        @Path("displayName") displayName: String
    ): Deferred<RulesResponse>

//     __  __       _ _   _        _____          _     _ _ _
//    |  \/  |     | | | (_)      |  __ \        | |   | (_) |
//    | \  / |_   _| | |_ _ ______| |__) |___  __| | __| |_| |_ ___
//    | |\/| | | | | | __| |______|  _  // _ \/ _` |/ _` | | __/ __|
//    | |  | | |_| | | |_| |      | | \ \  __/ (_| | (_| | | |_\__ \
//    |_|  |_|\__,_|_|\__|_|      |_|  \_\___|\__,_|\__,_|_|\__|___/
//

    @GET("/api/multi/mine")
    fun getMyMultiReddits(
        @Header("Authorization") authorization: String,
        @Query("expand_srs") expandSubs: Boolean = true
    ): Deferred<List<MultiRedditChild>>

    @GET("{multipath}{sort}.json")
    fun getMultiRedditListing(
        @Header("Authorization") authorization: String? = null,
        @Path("multipath", encoded = true) multipath: String,
        @Path("sort") sort: PostSort,
        @Query("t") t: Time?,
        @Query("after") after: String? = null,
        @Query("limit") limit: Int
    ): Deferred<ListingResponse>

    @GET("/api/multi/{multipath}.json")
    fun getMultiReddit(
        @Header("Authorization") authorization: String? = null,
        @Path("multipath", encoded = true) multipath: String,
        @Query("expand_srs") expandSubs: Boolean = true
    ): Deferred<MultiRedditChild>

    @DELETE("/api/multi/{multipath}.json")
    fun deleteMultiReddit(
        @Header("Authorization") authorization: String? = null,
        @Path("multipath", encoded = true) multipath: String
    ): Deferred<Response<Unit>>

    @DELETE("/api/multi/{multipath}/r/{srName}")
    fun deleteSubredditInMultiReddit(
        @Header("Authorization") authorization: String? = null,
        @Path("multipath", encoded = true) multipath: String,
        @Path("srName") srName: String
    ): Deferred<Response<Unit>>

    @PUT("/api/multi/{multipath}.json")
    fun updateMulti(
        @Header("Authorization") authorization: String? = null,
        @Path("multipath", encoded = true) multipath: String,
        @Query("model", encoded = true) model: MultiRedditUpdate,
        @Query("expand_srs") expandSubs: Boolean = true
    ): Deferred<MultiRedditChild>

    @POST("/api/multi/.json")
    fun createMulti(
        @Header("Authorization") authorization: String? = null,
        @Query("model", encoded = true) model: MultiRedditUpdate,
        @Query("expand_srs") expandSubs: Boolean = true
    ): Deferred<MultiRedditChild>

//      _____                                     _
//     / ____|                                   | |
//    | |     ___  _ __ ___  _ __ ___   ___ _ __ | |_ ___
//    | |    / _ \| '_ ` _ \| '_ ` _ \ / _ \ '_ \| __/ __|
//    | |___| (_) | | | | | | | | | | |  __/ | | | |_\__ \
//     \_____\___/|_| |_| |_|_| |_| |_|\___|_| |_|\__|___/

    @GET
    fun getPostAndComments(
        @Header("Authorization") authorization: String? = null,
        @Url permalink: String,
        @Query("sort") sort: CommentSort,
        @Query("limit") limit: Int
    ): Deferred<CommentPage>

    @GET("/api/morechildren/.json")
    fun getMoreComments(
        @Header("Authorization") authorization: String? = null,
        @Query("children") children: String,
        @Query("link_id") linkId: String,
        @Query("api_type") apiType: String = "json",
        @Query("limit_children") limitChildren: Boolean = false,
        @Query("sort") sort: CommentSort
    ): Deferred<MoreChildrenResponse>

    @POST("/api/comment/.json")
    fun addComment(
        @Header("Authorization") authorization: String,
        @Query("thing_id") parentName: String,
        @Query("text") body: String,
        @Query("api_type") apiType: String = "json"
    ): Deferred<MoreChildrenResponse>

//     __  __
//    |  \/  |
//    | \  / | ___  ___ ___  __ _  __ _  ___  ___
//    | |\/| |/ _ \/ __/ __|/ _` |/ _` |/ _ \/ __|
//    | |  | |  __/\__ \__ \ (_| | (_| |  __/\__ \
//    |_|  |_|\___||___/___/\__,_|\__, |\___||___/
//                                 __/ |
//                                |___/

    @GET("/message/{where}")
    fun getMessages(
        @Header("Authorization") authorization: String? = null,
        @Path("where") where: MessageWhere,
        @Query("after") after: String? = null,
        @Query("limit") limit: Int? = null
    ): Deferred<ListingResponse>

//                                  _
//        /\                       | |
//       /  \__      ____ _ _ __ __| |___
//      / /\ \ \ /\ / / _` | '__/ _` / __|
//     / ____ \ V  V / (_| | | | (_| \__ \
//    /_/    \_\_/\_/ \__,_|_|  \__,_|___/

    @GET("/api/v1/user/{user}/trophies/.json")
    fun getAwards(
        @Header("Authorization") authorization: String? = null,
        @Path("user") user: String
    ): Deferred<TrophyListingResponse>

//     ______ _       _
//    |  ____| |     (_)
//    | |__  | | __ _ _ _ __ ___
//    |  __| | |/ _` | | '__/ __|
//    | |    | | (_| | | |  \__ \
//    |_|    |_|\__,_|_|_|  |___/

    @GET("/r/{displayName}/api/link_flair.json")
    fun getFlairs(
        @Header("Authorization") authorization: String,
        @Path("displayName") srName: String
    ): Deferred<List<Flair>>

    companion object {
        private const val BASE_URL = "https://www.reddit.com/"
        private const val OAUTH_URL = "https://oauth.reddit.com/"
        fun createWithNoAuth(): RedditApiService =
            create(
                BASE_URL.toHttpUrl()
            )
        fun createWithAuth(): RedditApiService =
            create(
                OAUTH_URL.toHttpUrl()
            )
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
                .add(CommentsMoshiAdapter())
                .add(PolymorphicJsonAdapterFactory.of(ListingChild::class.java, "kind")
                    .withSubtype(CommentChild::class.java, "t1")
                    .withSubtype(PostChild::class.java, "t3")
                    .withSubtype(MessageChild::class.java, "t4")
                    .withSubtype(SubredditChild::class.java, "t5")
                    .withSubtype(MoreChild::class.java, "more"))
                .add(KotlinJsonAdapterFactory())
                .build()


            return Retrofit.Builder()
                .baseUrl(httpUrl)
                .client(client)
                .addConverterFactory(EnumConverterFactory)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .addConverterFactory(GsonConverterFactory.create())
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