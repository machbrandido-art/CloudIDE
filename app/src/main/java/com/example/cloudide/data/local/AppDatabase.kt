package com.example.cloudide.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [RepoBuildEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun repoBuildDao(): RepoBuildDao
}