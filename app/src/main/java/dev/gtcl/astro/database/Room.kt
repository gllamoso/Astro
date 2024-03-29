package dev.gtcl.astro.database

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*
import dev.gtcl.astro.SubscriptionType

@Dao
interface AccountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(account: SavedAccount)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(accounts: List<SavedAccount>)

    @Delete
    fun deleteUser(account: SavedAccount)

    @Query("delete from user_table where name = :username")
    fun deleteUser(username: String)

    @Query("select * from user_table")
    fun getUsers(): LiveData<List<SavedAccount>>
}

@Dao
interface ReadItemDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(itemsRead: ItemRead)

    @Query("select * from read_listing")
    suspend fun getAll(): List<ItemRead>
}

@Dao
interface SubscriptionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(list: List<Subscription>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(subscription: Subscription)

    @Query("delete from subscriptions where userId = :userId")
    fun deleteAllSubscriptions(userId: String)

    @Query("delete from subscriptions where id = :subId")
    fun deleteSubscription(subId: String)

    @Query("select * from subscriptions where id = :subId")
    suspend fun getSubscription(subId: String): Subscription?

    @Query("select * from subscriptions where userId = :userId and name like :q and type != 'MULTIREDDIT' order by name collate nocase asc")
    suspend fun searchSubscriptionsExcludingMultiReddits(
        userId: String,
        q: String
    ): List<Subscription>

    @Query("select * from subscriptions where userId = :userId and isFavorite = 1 order by displayName collate nocase asc")
    suspend fun getFavoriteSubscriptionsAlphabetically(userId: String): List<Subscription>

    @Query("select * from subscriptions where userId = :userId and isFavorite = 1 and type = :type order by displayName collate nocase asc")
    suspend fun getFavoriteSubscriptionsAlphabetically(
        userId: String,
        type: SubscriptionType
    ): List<Subscription>

    @Query("select * from subscriptions where userId = :userId and isFavorite = 1 and type != :excluding order by displayName collate nocase asc")
    suspend fun getFavoriteSubscriptionsAlphabeticallyExcluding(
        userId: String,
        excluding: SubscriptionType
    ): List<Subscription>

    @Query("select * from subscriptions where userId = :userId and type = :type order by displayName collate nocase asc")
    suspend fun getSubscriptionsAlphabetically(
        userId: String,
        type: SubscriptionType
    ): List<Subscription>

    @Query("update subscriptions set isFavorite = :favorite where id = :subId collate nocase")
    suspend fun updateSubscription(subId: String, favorite: Boolean)

}

@Database(
    entities = [SavedAccount::class, ItemRead::class, Subscription::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(SubscriptionTypeConverter::class)
abstract class AstroDatabase : RoomDatabase() {
    abstract val accountDao: AccountDao
    abstract val readItemDao: ReadItemDao
    abstract val subscriptionDao: SubscriptionDao
}

private lateinit var INSTANCE: AstroDatabase
fun redditDatabase(context: Context): AstroDatabase {
    synchronized(AstroDatabase::class.java) {
        if (!::INSTANCE.isInitialized) {
            INSTANCE =
                Room.databaseBuilder(context.applicationContext, AstroDatabase::class.java, "local")
                    .build()
        }
    }
    return INSTANCE
}