package com.vahi.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.vahi.app.data.database.dao.NotebookDao
import com.vahi.app.data.database.entity.NotebookEntity

/**
 * `exportSchema = false` for now — see the note in `app/build.gradle.kts` above the
 * (removed) `room.schemaLocation` arg: Room 2.8.4's schema re-validation throws a
 * genuine `AbstractMethodError` against this project's kotlinx.serialization
 * toolchain. Re-enable (`docs/17-data-spec.md` R4) when there's a real schema v2 to
 * migrate against. Every future schema change still ships a `Migration` and a test
 * that opens a real database at version N and migrates it forward;
 * `fallbackToDestructiveMigration()` is banned in release builds — in a notes app
 * it means "silently delete the user's work."
 */
@Database(entities = [NotebookEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class VahiDatabase : RoomDatabase() {
    abstract fun notebookDao(): NotebookDao

    companion object {
        const val NAME = "vahi.db"
    }
}
