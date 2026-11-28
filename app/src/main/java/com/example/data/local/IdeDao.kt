package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IdeDao {
    @Query("SELECT * FROM build_profiles WHERE projectKey = :projectKey LIMIT 1")
    fun observeBuildProfile(projectKey: String): Flow<BuildProfileEntity?>

    @Query("SELECT * FROM build_profiles WHERE projectKey = :projectKey LIMIT 1")
    suspend fun getBuildProfile(projectKey: String): BuildProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBuildProfile(profile: BuildProfileEntity)

    @Query("SELECT * FROM command_history ORDER BY timestamp DESC LIMIT 40")
    fun observeCommandHistory(): Flow<List<CommandHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommandHistory(entry: CommandHistoryEntity)

    @Query("DELETE FROM command_history")
    suspend fun clearCommandHistory()

    @Query("SELECT * FROM header_cache WHERE projectKey = :projectKey")
    fun observeHeaderCache(projectKey: String): Flow<List<HeaderCacheEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHeaderCache(entries: List<HeaderCacheEntity>)

    @Query("DELETE FROM header_cache WHERE projectKey = :projectKey")
    suspend fun clearHeaderCacheForProject(projectKey: String)

    @Query("SELECT * FROM cdn_libraries ORDER BY installedAt DESC")
    fun observeCdnLibraries(): Flow<List<CdnLibraryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCdnLibrary(lib: CdnLibraryEntity)

    @Query("UPDATE cdn_libraries SET isEnabled = :enabled WHERE name = :name")
    suspend fun setCdnLibraryEnabled(name: String, enabled: Boolean)

    @Query("DELETE FROM cdn_libraries WHERE name = :name")
    suspend fun deleteCdnLibrary(name: String)
}
