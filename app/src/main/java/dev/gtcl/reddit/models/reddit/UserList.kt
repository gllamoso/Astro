package dev.gtcl.reddit.models.reddit

import android.os.Parcelable
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import dev.gtcl.reddit.Visibility
import dev.gtcl.reddit.models.reddit.listing.SubredditData
import kotlinx.android.parcel.Parcelize

data class UserList(val data: UserListData)
data class UserListData(val children: List<User>)
@Parcelize
data class User(
    val date: Long,
    @Json(name = "rel_id")
    val relId: String,
    val name: String,
    val id: String
): Parcelable

enum class UserType{
    FRIEND,
    BLOCKED
}

data class FriendRequest(val name: String){
    override fun toString(): String = Gson().toJson(this)
}

//@Parcelize
//data class MultiRedditUpdate(
//    @SerializedName("description_md") val description: String? = null,
//    @SerializedName("display_name") val displayName: String? = null,
//    @SerializedName("icon_img") val iconImg: String? = null,
//    @SerializedName("key_color") val keyColor: String? = null,
//    val subreddits: List<SubredditData>? = null,
//    val visibility: Visibility? = null
//): Parcelable {
//    override fun toString(): String = Gson().toJson(this)
//}