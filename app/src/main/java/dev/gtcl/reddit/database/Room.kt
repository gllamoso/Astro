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
    fun insert(itemsRead: ItemsRead)

    @Query("select * from read_listing")
    fun getAll(): LiveData<List<ItemsRead>>
}

@Dao
interface SubredditDao{
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(list: List<DbSubreddit>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(subreddit: DbSubreddit)

    @Query("select * from subs where userId = :userId order by displayName collate nocase asc")
    fun getSubscribedSubsLive(userId: String): LiveData<List<DbSubreddit>>

    @Query("delete from subs where userId = :userId")
    fun deleteSubscribedSubs(userId: String)

    @Query("select * from subs where userId = :userId and isFavorite = 0")
    fun getNonFavoriteSubsLive(userId: String): LiveData<List<DbSubreddit>>

    @Query("select * from subs where userId = :userId and isFavorite = 1")
    fun getFavoriteSubsLive(userId: String): LiveData<List<DbSubreddit>>

    @Query("select * from subs where userId = :userId and isFavorite = 1")
    suspend fun getFavoriteSubs(userId: String): List<DbSubreddit>

    @Query("update subs set isFavorite = :favorite where userId = :userId and displayName = :displayName")
    fun updateFavoriteSub(userId: String, displayName: String, favorite: Boolean)
}

@Database(entities = [DbAccount::class, ItemsRead::class, DbSubreddit::class], version = 1, exportSchema = false)
abstract class RedditDatabase: RoomDatabase(){
    abstract val accountDao: AccountDao
    abstract val readItemDao: ReadItemDao
    abstract val subredditDao: SubredditDao
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