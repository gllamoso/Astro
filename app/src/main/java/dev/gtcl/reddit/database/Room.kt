package dev.gtcl.reddit.database

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*

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
interface SubredditDao{
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(list: List<DbSubreddit>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(subreddit: DbSubreddit)

    @Query("select * from subs where userId = :userId order by displayName collate nocase asc")
    suspend fun getSubscribedSubs(userId: String): List<DbSubreddit>

    @Query("select * from subs where userId = :userId and displayName = :displayName order by displayName collate nocase asc")
    suspend fun getSubscribedSubs(userId: String, displayName: String): List<DbSubreddit>

    @Query("select * from subs where userId = :userId order by displayName collate nocase asc")
    fun getSubscribedSubsLive(userId: String): LiveData<List<DbSubreddit>>

    @Query("delete from subs where userId = :userId")
    fun deleteSubscribedSubs(userId: String)

    @Query("delete from subs where userId = :userId and displayName = :displayName collate nocase")
    fun deleteSubreddit(userId: String, displayName: String)

    @Query("select * from subs where userId = :userId and isFavorite = 1 order by displayName collate nocase asc")
    fun getFavoriteSubsLive(userId: String): LiveData<List<DbSubreddit>>

    @Query("select * from subs where userId = :userId and isFavorite = 1")
    suspend fun getFavoriteSubs(userId: String): List<DbSubreddit>

    @Query("update subs set isFavorite = :favorite where userId = :userId and displayName = :displayName collate nocase")
    fun updateFavoriteSub(userId: String, displayName: String, favorite: Boolean)
}

@Dao
interface MultiRedditDao{
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(list: List<DbMultiReddit>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(multi: DbMultiReddit)

    @Query("select * from multis where userId = :userId order by name collate nocase asc")
    fun getMultiRedditsLive(userId: String): LiveData<List<DbMultiReddit>>

    @Query("select * from multis where userId = :userId order by name collate nocase asc")
    suspend fun getMultiReddits(userId: String): List<DbMultiReddit>

    @Query("delete from multis where userId = :userId")
    fun deleteSubscribedSubs(userId: String)
}

@Database(entities = [DbAccount::class, ItemRead::class, DbSubreddit::class, DbMultiReddit::class], version = 1, exportSchema = false)
abstract class RedditDatabase: RoomDatabase(){
    abstract val accountDao: AccountDao
    abstract val readItemDao: ReadItemDao
    abstract val subredditDao: SubredditDao
    abstract val multiRedditDao: MultiRedditDao
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