package com.marauders.map.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.marauders.map.model.ScannedDevice

/**
 * 蓝牙低功耗（BLE）扫描器。
 *
 * 扫描结果通过 [onUpdate] 回调以“去重后的设备列表”形式抛出，
 * 上层（Compose）把它存进 state 即可驱动雷达刷新。
 */
class BluetoothScanner(
    private val context: Context,
    private val adapter: BluetoothAdapter?
) {
    private val leScanner = adapter?.bluetoothLeScanner

    /** 稳定的设备表：key = MAC 地址 */
    private val devices = linkedMapOf<String, ScannedDevice>()

    /** 扫描结果回调（每次有设备出现/刷新时触发） */
    var onUpdate: ((List<ScannedDevice>) -> Unit)? = null

    var scanning: Boolean = false
        private set

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val address = device.address ?: return
            val name = device.name ?: "未知设备"
            val rssi = result.rssi
            val existing = devices[address]
            devices[address] = ScannedDevice(
                address = address,
                name = name,
                rssi = rssi,
                distance = rssiToDistance(rssi),
                lastSeen = System.currentTimeMillis(),
                // 方位角用 MAC 做稳定哈希，保证同一个设备总在同一个方向
                angle = existing?.angle ?: angleFromAddress(address)
            )
            onUpdate?.invoke(devices.values.sortedBy { it.distance })
        }

        override fun onScanFailed(errorCode: Int) {
            scanning = false
        }
    }

    /** 开始扫描（会先做权限校验，避免崩溃） */
    fun start() {
        val le = leScanner ?: return
        if (!hasPermission()) return
        if (!scanning) {
            le.startScan(scanSettings, callback)
            scanning = true
        }
    }

    /** 停止扫描 */
    fun stop() {
        leScanner?.stopScan(callback)
        scanning = false
    }

    private fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        /** 1 米处的参考 RSSI（实测值，可按设备校准） */
        private const val MEASURED_POWER = -59
        /** 环境衰减系数（2.0 空旷 ~ 3.5 复杂室内） */
        private const val PATH_LOSS = 2.2

        /** 由 RSSI 估算距离（米）：d = 10^((MeasuredPower - RSSI) / (10 * n)) */
        fun rssiToDistance(rssi: Int): Double {
            val ratio = (MEASURED_POWER - rssi) / (10.0 * PATH_LOSS)
            return 10.0.pow(ratio)
        }

        /** 由 MAC 地址生成稳定的 0~360 方位角 */
        fun angleFromAddress(address: String): Float {
            var h = 7
            address.forEach { h = (h * 31 + it.code).absoluteValue }
            return (h % 360).toFloat()
        }
    }
}
