package dev.gtcl.reddit.models.reddit

import android.os.Parcelable
import com.google.gson.Gson
import com.squareup.moshi.Json
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