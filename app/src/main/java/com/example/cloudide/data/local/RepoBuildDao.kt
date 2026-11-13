package com.example.cloudide.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RepoBuildDao {

    @Query("SELECT * FROM repo_builds WHERE fullName = :fullName")
    suspend fun get(fullName: String): RepoBuildEntity?

    @Query("SELECT * FROM repo_builds")
    suspend fun getAll(): List<RepoBuildEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RepoBuildEntity)

    @Query("DELETE FROM repo_builds")
    suspend fun clearAll()
}