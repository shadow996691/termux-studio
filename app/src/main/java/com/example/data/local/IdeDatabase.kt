package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BuildProfileEntity::class,
        CommandHistoryEntity::class,
        HeaderCacheEntity::class,
        CdnLibraryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class IdeDatabase : RoomDatabase() {
    abstract fun ideDao(): IdeDao

    companion object {
        @Volatile
        private var INSTANCE: IdeDatabase? = null

        fun getInstance(context: Context): IdeDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    IdeDatabase::class.java,
                    "termuxc_studio.db"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
