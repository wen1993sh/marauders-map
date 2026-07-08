package com.marauders.map.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    /** 插入或更新设备档案 */
    @Upsert
    suspend fun upsertProfile(profile: DeviceProfileEntity)

    /** 取出所有档案（启动时预热内存缓存，保证跨重启的首见时间正确） */
    @Query("SELECT * FROM device_profiles")
    suspend fun allProfiles(): List<DeviceProfileEntity>

    /** 记录一次出现（冲突忽略，主键自增） */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun recordSighting(sighting: SightingEntity)

    /** 设备的档案流（详情卡用） */
    @Query("SELECT * FROM device_profiles WHERE mac = :mac")
    fun profile(mac: String): Flow<DeviceProfileEntity?>

    /** 设备最近的若干次出现（时间线，详情卡用） */
    @Query("SELECT * FROM sightings WHERE mac = :mac ORDER BY timestamp DESC LIMIT :limit")
    fun recentSightings(mac: String, limit: Int = 20): Flow<List<SightingEntity>>
}
