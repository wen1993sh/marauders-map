package com.marauders.map.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SharingStarted
import androidx.lifecycle.viewModelScope
import com.marauders.map.data.DeviceRepository
import com.marauders.map.data.local.SightingEntity
import com.marauders.map.model.ScannedDevice
import com.marauders.map.model.UiDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/**
 * 把 Repository 的实时设备流与“当前选中的设备”组合成界面状态。
 */
class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DeviceRepository(application, viewModelScope)

    /** 雷达与列表的实时数据源 */
    val devices: StateFlow<List<UiDevice>> = repository.devices

    private val _selectedMac = MutableStateFlow<String?>(null)

    /** 当前选中的设备（用于详情卡）；设备离开或取消选中时为 null */
    val selectedDevice: StateFlow<UiDevice?> = combine(devices, _selectedMac) { list, mac ->
        list.firstOrNull { it.mac == mac }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** 选中设备的近期出现时间线 */
    val recentSightings: StateFlow<List<SightingEntity>> = _selectedMac
        .flatMapLatest { mac ->
            if (mac == null) flowOf(emptyList()) else repository.recentSightings(mac)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 接收扫描器的去重设备列表 */
    fun ingest(list: List<ScannedDevice>) = repository.ingest(list)

    /** 选中 / 取消选中某设备 */
    fun select(mac: String?) {
        _selectedMac.value = mac
    }
}
