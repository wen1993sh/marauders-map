package com.marauders.map.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 设备长期档案：首次见、最近见、累计出现次数 */
@Entity(tableName = "device_profiles")
data class DeviceProfileEntity(
    @PrimaryKey val mac: String,
    val displayName: String,
    val isNamed: Boolean,
    val firstSeen: Long,
    val lastSeen: Long,
    val seenCount: Int
)
