package com.marauders.map.data

import android.app.Application
import androidx.room.Room
import com.marauders.map.data.local.AppDatabase
import com.marauders.map.data.local.DeviceProfileEntity
import com.marauders.map.data.local.SightingEntity
import com.marauders.map.model.ScannedDevice
import com.marauders.map.model.UiDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 数据中枢：扫描为实时源、Room 做旁路日志。
 *
 * - [devices] 由内存实时表驱动（每帧刷新、不读库），保证雷达流畅；
 * - 落库在 IO 协程中节流进行：每个设备最多每 [SIGHTING_MIN_INTERVAL_MS] 写一条 Sighting，
 *   并同步更新其档案（首次见 / 出现次数 / 最近见）。
 */
class DeviceRepository(
    application: Application,
    private val scope: CoroutineScope
) {
    private val dao = Room.databaseBuilder(
        application, AppDatabase::class.java, "marauders.db"
    ).build().deviceDao()

    private val _devices = MutableStateFlow<List<UiDevice>>(emptyList())
    val devices: StateFlow<List<UiDevice>> = _devices.asStateFlow()

    /** 内存中的档案缓存，避免每帧读库；同时作为 UiDevice 的 firstSeen/seenCount 来源 */
    private data class ProfileInfo(
        var firstSeen: Long,
        var seenCount: Int,
        /** 本次会话刚创建（首次出现），需在落库时写一次档案 */
        var firstSeenThisSession: Boolean = false
    )
    private val profileCache = mutableMapOf<String, ProfileInfo>()
    private val lastSightingAt = mutableMapOf<String, Long>()

    init {
        // 预热缓存：跨重启也要让“首次见”保持准确
        scope.launch(Dispatchers.IO) {
            dao.allProfiles().forEach { p ->
                profileCache[p.mac] = ProfileInfo(firstSeen = p.firstSeen, seenCount = p.seenCount)
            }
        }
    }

    /** 消费一次扫描器的去重设备列表 */
    fun ingest(list: List<ScannedDevice>) {
        val now = System.currentTimeMillis()
        val ui = list.map { s ->
            val isNamed = s.name != UNKNOWN_NAME
            val displayName = if (isNamed) s.name else NameProvider.nameFor(s.address)
            val info = profileCache.getOrPut(s.address) {
                ProfileInfo(firstSeen = now, seenCount = 0, firstSeenThisSession = true)
            }
            UiDevice(
                mac = s.address,
                name = displayName,
                isNamed = isNamed,
                rssi = s.rssi,
                distance = s.distance,
                angle = s.angle,
                firstSeen = info.firstSeen,
                seenCount = info.seenCount,
                lastSeen = now
            )
        }
        _devices.value = ui
        scope.launch(Dispatchers.IO) { persist(ui, now) }
    }

    /** 节流落库：首次出现立即写档案；之后每 [SIGHTING_MIN_INTERVAL_MS] 记录一次 Sighting 并同步档案 */
    private suspend fun persist(list: List<UiDevice>, now: Long) {
        for (d in list) {
            val info = profileCache[d.mac] ?: continue
            val last = lastSightingAt[d.mac] ?: 0
            val shouldRecord = now - last >= SIGHTING_MIN_INTERVAL_MS
            // 首次出现 或 达到记录间隔 时落库
            if (!info.firstSeenThisSession && !shouldRecord) continue

            if (shouldRecord) {
                dao.recordSighting(
                    SightingEntity(mac = d.mac, timestamp = now, rssi = d.rssi, distance = d.distance)
                )
                info.seenCount += 1
                lastSightingAt[d.mac] = now
            }
            dao.upsertProfile(
                DeviceProfileEntity(
                    mac = d.mac,
                    displayName = d.name,
                    isNamed = d.isNamed,
                    firstSeen = info.firstSeen,
                    lastSeen = now,
                    seenCount = info.seenCount
                )
            )
            info.firstSeenThisSession = false
        }
    }

    /** 设备最近的若干次出现（时间线，详情卡用） */
    fun recentSightings(mac: String): Flow<List<SightingEntity>> = dao.recentSightings(mac, 20)
}
