package com.marauders.map.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DeviceProfileEntity::class, SightingEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
}
