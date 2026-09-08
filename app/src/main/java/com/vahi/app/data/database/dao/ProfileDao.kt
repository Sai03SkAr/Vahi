package com.vahi.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.vahi.app.data.database.entity.ProfileEntity
import java.util.UUID

@Dao
interface ProfileDao {

    /** There is only ever one row — see `ProfileEntity`'s KDoc. */
    @Query("SELECT * FROM profiles LIMIT 1")
    suspend fun getLocalProfile(): ProfileEntity?

    @Insert
    suspend fun insert(profile: ProfileEntity)

    /**
     * `@Transaction` on a default DAO method wraps the whole body in one database
     * transaction — under SQLite's single-writer lock, a second concurrent caller
     * blocks until the first commits, then finds the row [getLocalProfile] already
     * returned and never inserts a second one. A plain check-then-insert without
     * this would race: two callers can both see no profile and both insert their
     * own fresh UUID, since `OnConflictStrategy.IGNORE` only helps when two writers
     * collide on the *same* key — two independently-generated UUIDs never do.
     */
    @Transaction
    suspend fun getOrCreateLocalProfileId(): String {
        getLocalProfile()?.let { return it.id }
        val profile = ProfileEntity(id = UUID.randomUUID().toString(), createdAt = System.currentTimeMillis())
        insert(profile)
        return profile.id
    }
}
