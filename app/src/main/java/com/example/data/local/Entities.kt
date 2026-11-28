package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "build_profiles")
data class BuildProfileEntity(
    @PrimaryKey val projectKey: String,
    val compilerName: String = "AUTO",
    val cStandard: String = "c17",
    val cppStandard: String = "c++20",
    val outputType: String = "EXECUTABLE",
    val outputName: String = "app_out",
    val includeDirs: String = ".",
    val libraryDirs: String = ".",
    val linkedLibs: String = "-lm -lpthread",
    val extraFlags: String = "-Wall -Wextra",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "command_history")
data class CommandHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val command: String,
    val workdir: String,
    val exitCode: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "header_cache")
data class HeaderCacheEntity(
    @PrimaryKey val headerKey: String,
    val projectKey: String,
    val headerPath: String,
    val symbolsJoined: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cdn_libraries")
data class CdnLibraryEntity(
    @PrimaryKey val name: String,
    val url: String,
    val installRelPath: String,
    val compilerFlags: String,
    val isEnabled: Boolean = true,
    val installedAt: Long = System.currentTimeMillis()
)
