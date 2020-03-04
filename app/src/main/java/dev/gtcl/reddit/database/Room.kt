package dev.gtcl.reddit.database

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*
import kotlinx.coroutines.Deferred

@Dao
interface UserDao{
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(databaseUser: DatabaseUser)

    @Delete
    fun deleteUser(databaseUser: DatabaseUser)

    @Query("delete from user_table where name = :username")
    fun deleteUser(username: String)

    @Query("select * from user_table")
    fun getUsers(): LiveData<List<DatabaseUser>>
}

@Dao
interface ReadPostDao{
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(readPost: ReadPost)

    @Query("select * from read_posts")
    fun getAll(): LiveData<List<ReadPost>>
}

@Database(entities = [DatabaseUser::class, ReadPost::class], version = 1)
abstract class RedditDatabase: RoomDatabase(){
    abstract val userDao: UserDao
    abstract val readPostDao: ReadPostDao
}

private lateinit var INSTANCE: RedditDatabase
fun redditDatabase(context: Context): RedditDatabase {
    synchronized(RedditDatabase::class.java){
        if(!::INSTANCE.isInitialized){
            INSTANCE = Room.databaseBuilder(context.applicationContext,
                RedditDatabase::class.java,
                "users").build()
        }
    }
    return INSTANCE
}