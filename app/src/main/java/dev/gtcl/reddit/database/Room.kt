package dev.gtcl.reddit.database

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*
import dev.gtcl.reddit.SubscriptionType

@Dao
interface AccountDao{
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(account: DbAccount)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(accounts: List<DbAccount>)

    @Delete
    fun deleteUser(account: DbAccount)

    @Query("delete from user_table where name = :username")
    fun deleteUser(username: String)

    @Query("select * from user_table")
    fun getUsers(): LiveData<List<DbAccount>>
}

@Dao
interface ReadItemDao{
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(itemsRead: ItemRead)

    @Query("select * from read_listing")
    fun getAllLiveData(): LiveData<List<ItemRead>>

    @Query("select * from read_listing")
    suspend fun getAll(): List<ItemRead>
}

@Dao
interface SubscriptionDao{
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(list: List<Subscription>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(subscription: Subscription)

    @Query("delete from subscriptions where userId = :userId")
    fun deleteAllSubscriptions(userId: String)

    @Query("delete from subscriptions where userId = :userId and name = :name")
    fun deleteSubscription(userId: String, name: String)

    @Query("select * from subscriptions where userId = :userId and name = :name")
    suspend fun getSubscription(userId: String, name: String): Subscription?

    @Query("select * from subscriptions where userId = :userId and isFavorite = 1 order by name collate nocase asc")
    suspend fun getFavoriteSubscriptionsAlphabetically(userId: String): List<Subscription>

    @Query("select * from subscriptions where userId = :userId and isFavorite = 1 and type != :subscriptionType order by name collate nocase asc")
    suspend fun getFavoriteSubscriptionsAlphabeticallyExcluding(userId: String, subscriptionType: SubscriptionType): List<Subscription>

    @Query("select * from subscriptions where userId = :userId order by name collate nocase asc")
    suspend fun getSubscriptionsAlphabetically(userId: String): List<Subscription>

    @Query("select * from subscriptions where userId = :userId and type != :subscriptionType order by name collate nocase asc")
    suspend fun getSubscriptionsAlphabeticallyExcluding(userId: String, subscriptionType: SubscriptionType): List<Subscription>

    @Query("select * from subscriptions where userId = :userId and type = :subscriptionType order by name collate nocase asc")
    suspend fun getSubscriptionsAlphabetically(userId: String, subscriptionType: SubscriptionType): List<Subscription>

    @Query("update subscriptions set isFavorite = :isFavorite where userId = :userId and name = :name collate nocase")
    suspend fun updateSubscription(userId: String, name: String, isFavorite: Boolean)

}

@Database(entities = [DbAccount::class, ItemRead::class, Subscription::class], version = 1, exportSchema = false)
@TypeConverters(SubscriptionTypeConverter::class)
abstract class RedditDatabase: RoomDatabase(){
    abstract val accountDao: AccountDao
    abstract val readItemDao: ReadItemDao
    abstract val subscriptionDao: SubscriptionDao
}

private lateinit var INSTANCE: RedditDatabase
fun redditDatabase(context: Context): RedditDatabase {
    synchronized(RedditDatabase::class.java){
        if(!::INSTANCE.isInitialized){
            INSTANCE = Room.databaseBuilder(context.applicationContext, RedditDatabase::class.java, "local").build() // TODO: Rename
        }
    }
    return INSTANCE
}