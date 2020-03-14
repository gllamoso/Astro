package dev.gtcl.reddit.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "read_listing")
data class ReadListing constructor(
    @PrimaryKey
    val name: String)

