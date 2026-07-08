package com.marauders.map.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 一次出现记录（时间线），写入受节流控制 */
@Entity(tableName = "sightings", indices = [Index("mac")])
data class SightingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mac: String,
    val timestamp: Long,
    val rssi: Int,
    val distance: Double
)
