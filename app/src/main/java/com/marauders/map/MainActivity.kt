package com.marauders.map

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marauders.map.ble.BluetoothScanner
import com.marauders.map.ui.MaraudersMapScreen
import com.marauders.map.ui.MaraudersTheme
import com.marauders.map.ui.MapViewModel

class MainActivity : ComponentActivity() {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        getSystemService(BluetoothManager::class.java)?.adapter
    }

    private lateinit var scanner: BluetoothScanner

    /** 请求开启蓝牙的启动器 */
    private val enableBtLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) scanner.start()
    }

    /** 请求运行时权限的启动器 */
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.all { it }) proceedAfterPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scanner = BluetoothScanner(this, bluetoothAdapter)

        setContent {
            val viewModel: MapViewModel = viewModel()
            DisposableEffect(viewModel) {
                scanner.onUpdate = viewModel::ingest
                onDispose { scanner.onUpdate = null }
            }
            MaraudersTheme {
                MaraudersMapScreen(
                    viewModel = viewModel,
                    hasBluetooth = bluetoothAdapter != null,
                    scanner = scanner,
                    onStartScan = { ensureReadyAndScan() },
                    onStopScan = { scanner.stop() }
                )
            }
        }
    }

    /** 统一入口：先拿权限，再开蓝牙，最后扫描 */
    private fun ensureReadyAndScan() {
        val missing = neededPermissions().filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) proceedAfterPermission()
        else permissionLauncher.launch(missing.toTypedArray())
    }

    /** 权限就绪后：蓝牙没开就先开，再开始扫描 */
    private fun proceedAfterPermission() {
        val adapter = bluetoothAdapter ?: return
        if (!adapter.isEnabled) {
            enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        } else {
            scanner.start()
        }
    }

    private fun neededPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
}
