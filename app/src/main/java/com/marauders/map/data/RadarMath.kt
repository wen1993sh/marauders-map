package com.marauders.map.data

import com.marauders.map.model.UiDevice
import kotlin.math.absoluteValue
import kotlin.math.pow

/** 设备无名时的占位名（用于判定是否需要给魔法绰号） */
const val UNKNOWN_NAME = "未知设备"

/** 1 米处的参考 RSSI（可按设备现场校准） */
const val MEASURED_POWER = -59

/** 环境衰减系数（2.0 空旷 ~ 3.5 复杂室内） */
const val PATH_LOSS = 2.2

/** 高亮 + 带标签的最近设备数 */
const val FOCUS_COUNT = 8

/** 雷达标绘最大量程（米），超出压到边缘 */
const val MAX_RANGE_M = 15.0

/** 同一设备两次 Sighting 记录之间的最小间隔（毫秒），避免每秒灌库 */
const val SIGHTING_MIN_INTERVAL_MS = 120_000L

/** 由 RSSI 估算距离（米）：d = 10^((MeasuredPower - RSSI) / (10 * n)) */
fun rssiToDistance(rssi: Int): Double {
    val ratio = (MEASURED_POWER - rssi) / (10.0 * PATH_LOSS)
    return 10.0.pow(ratio)
}

/** 由 MAC 地址生成稳定的 0~360 方位角（示意，非真方向） */
fun angleFromAddress(address: String): Float {
    var h = 7
    address.forEach { h = (h * 31 + it.code).absoluteValue }
    return (h % 360).toFloat()
}

/**
 * 选出按距离升序最近 [n] 个设备的 MAC 集合（用于雷达高亮）。
 * 设备不足 [n] 时返回全部。
 */
fun focusMacs(devices: List<UiDevice>, n: Int): Set<String> {
    return devices.sortedBy { it.distance }.take(n).map { it.mac }.toSet()
}
