package dev.gtcl.reddit.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "read_posts")
data class ReadPost constructor(
    @PrimaryKey
    val name: String)

