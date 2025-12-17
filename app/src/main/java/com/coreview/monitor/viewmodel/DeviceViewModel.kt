package com.coreview.monitor.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coreview.monitor.data.BatteryInfo
import com.coreview.monitor.data.CpuCore
import com.coreview.monitor.data.DisplayInfo
import com.coreview.monitor.data.MemoryInfo
import com.coreview.monitor.utils.SystemMonitor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DeviceViewModel(application: Application) : AndroidViewModel(application) {
    
    private val systemMonitor = SystemMonitor(application)
    
    private val _cpuCores = MutableStateFlow<List<CpuCore>>(emptyList())
    val cpuCores: StateFlow<List<CpuCore>> = _cpuCores
    
    private val _batteryInfo = MutableStateFlow<BatteryInfo?>(null)
    val batteryInfo: StateFlow<BatteryInfo?> = _batteryInfo
    
    private val _displayInfo = MutableStateFlow<DisplayInfo?>(null)
    val displayInfo: StateFlow<DisplayInfo?> = _displayInfo
    
    private val _memoryInfo = MutableStateFlow<MemoryInfo?>(null)
    val memoryInfo: StateFlow<MemoryInfo?> = _memoryInfo
    
    init {
        startMonitoring()
    }
    
    private fun startMonitoring() {
        viewModelScope.launch {
            while (true) {
                _cpuCores.value = systemMonitor.getCpuCores()
                _batteryInfo.value = systemMonitor.getBatteryInfo()
                _displayInfo.value = systemMonitor.getDisplayInfo()
                _memoryInfo.value = systemMonitor.getMemoryInfo()
                delay(1000)
            }
        }
    }
}
