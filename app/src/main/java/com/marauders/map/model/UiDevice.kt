package com.marauders.map.model

/**
 * 界面实时使用的设备模型（由扫描结果 + 本地档案合并而来）。
 *
 * @param mac        MAC 地址（唯一键）
 * @param name       真名或魔法绰号
 * @param isNamed    true=有广播名；false=魔法绰号（背景氛围）
 * @param rssi       当前信号，单位 dBm（越接近 0 越强）
 * @param distance   RSSI 估算距离（米）
 * @param angle      雷达方位角（MAC 哈希，示意值，非真方向）
 * @param firstSeen  首次出现时间戳（来自本地档案，可空）
 * @param seenCount  累计出现次数（来自本地档案）
 * @param lastSeen   最近一次被探测到的时间戳
 */
data class UiDevice(
    val mac: String,
    val name: String,
    val isNamed: Boolean,
    val rssi: Int,
    val distance: Double,
    val angle: Float,
    val firstSeen: Long?,
    val seenCount: Int,
    val lastSeen: Long
)
