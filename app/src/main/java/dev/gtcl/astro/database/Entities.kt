package dev.gtcl.astro.database

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import dev.gtcl.astro.SubscriptionType
import kotlinx.android.parcel.Parcelize

//   _____                _   _____ _
//  |  __ \              | | |_   _| |
//  | |__) |___  __ _  __| |   | | | |_ ___ _ __ ___
//  |  _  // _ \/ _` |/ _` |   | | | __/ _ \ '_ ` _ \
//  | | \ \  __/ (_| | (_| |  _| |_| ||  __/ | | | | |
//  |_|  \_\___|\__,_|\__,_| |_____|\__\___|_| |_| |_|

@Entity(tableName = "read_listing")
data class ItemRead(
    @PrimaryKey
    val name: String
)

//                                     _
//      /\                            | |
//     /  \   ___ ___ ___  _   _ _ __ | |_ ___
//    / /\ \ / __/ __/ _ \| | | | '_ \| __/ __|
//   / ____ \ (_| (_| (_) | |_| | | | | |_\__ \
//  /_/    \_\___\___\___/ \__,_|_| |_|\__|___/
//


@Entity(tableName = "user_table")
@Parcelize
data class SavedAccount(
    @PrimaryKey
    val id: String,
    val name: String,
    val refreshToken: String?
) : Parcelable

//    _____       _                   _       _   _
//   / ____|     | |                 (_)     | | (_)
//  | (___  _   _| |__  ___  ___ _ __ _ _ __ | |_ _  ___  _ __  ___
//   \___ \| | | | '_ \/ __|/ __| '__| | '_ \| __| |/ _ \| '_ \/ __|
//   ____) | |_| | |_) \__ \ (__| |  | | |_) | |_| | (_) | | | \__ \
//  |_____/ \__,_|_.__/|___/\___|_|  |_| .__/ \__|_|\___/|_| |_|___/
//                                     | |
//                                     |_|

@Entity(tableName = "subscriptions")
@Parcelize
data class Subscription(
    @PrimaryKey
    val id: String, // {name}__{user ID}
    val name: String,
    val displayName: String,
    val userId: String,
    val icon: String?,
    val url: String,
    var isFavorite: Boolean,
    val type: SubscriptionType
) : Parcelable

class SubscriptionTypeConverter {

    @TypeConverter
    fun fromSubscriptionType(subscriptionType: SubscriptionType) = subscriptionType.name

    @TypeConverter
    fun toSubscriptionType(name: String): SubscriptionType {
        return when (name) {
            SubscriptionType.MULTIREDDIT.name -> SubscriptionType.MULTIREDDIT
            SubscriptionType.USER.name -> SubscriptionType.USER
            SubscriptionType.SUBREDDIT.name -> SubscriptionType.SUBREDDIT
            else -> throw IllegalArgumentException("Name not recognized as Subscription Type")
        }
    }
}