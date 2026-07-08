package com.marauders.map.model

/**
 * 一个被扫描到的蓝牙设备（光点）。
 *
 * @param address   MAC 地址（唯一键）
 * @param name      设备名（可能为“未知设备”）
 * @param rssi      信号强度，单位 dBm（越接近 0 越强）
 * @param distance  由 RSSI 估算出的距离（米）
 * @param lastSeen  最近一次被探测到的时间戳
 * @param angle     在雷达上的方位角（0~360，示意值）
 */
data class ScannedDevice(
    val address: String,
    val name: String,
    val rssi: Int,
    val distance: Double,
    val lastSeen: Long,
    val angle: Float
)
