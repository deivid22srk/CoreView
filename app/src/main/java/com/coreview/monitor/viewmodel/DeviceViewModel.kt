package com.coreview.monitor.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coreview.monitor.data.*
import com.coreview.monitor.utils.SystemMonitor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DeviceViewModel(application: Application) : AndroidViewModel(application) {
    
    private val systemMonitor = SystemMonitor(application)
    
    private val _cpuInfo = MutableStateFlow<CpuInfo?>(null)
    val cpuInfo: StateFlow<CpuInfo?> = _cpuInfo
    
    private val _gpuInfo = MutableStateFlow<GpuInfo?>(null)
    val gpuInfo: StateFlow<GpuInfo?> = _gpuInfo
    
    private val _batteryInfo = MutableStateFlow<BatteryInfo?>(null)
    val batteryInfo: StateFlow<BatteryInfo?> = _batteryInfo
    
    private val _displayInfo = MutableStateFlow<DisplayInfo?>(null)
    val displayInfo: StateFlow<DisplayInfo?> = _displayInfo
    
    private val _memoryInfo = MutableStateFlow<MemoryInfo?>(null)
    val memoryInfo: StateFlow<MemoryInfo?> = _memoryInfo
    
    private val _storageInfo = MutableStateFlow<StorageInfo?>(null)
    val storageInfo: StateFlow<StorageInfo?> = _storageInfo
    
    private val _bluetoothInfo = MutableStateFlow<BluetoothInfo?>(null)
    val bluetoothInfo: StateFlow<BluetoothInfo?> = _bluetoothInfo
    
    private val _audioInfo = MutableStateFlow<AudioInfo?>(null)
    val audioInfo: StateFlow<AudioInfo?> = _audioInfo
    
    private val _deviceInfo = MutableStateFlow<DeviceInfo?>(null)
    val deviceInfo: StateFlow<DeviceInfo?> = _deviceInfo
    
    private val _systemInfo = MutableStateFlow<SystemInfo?>(null)
    val systemInfo: StateFlow<SystemInfo?> = _systemInfo
    
    init {
        startMonitoring()
        loadStaticInfo()
    }
    
    private fun loadStaticInfo() {
        viewModelScope.launch {
            _gpuInfo.value = systemMonitor.getGpuInfo()
            _deviceInfo.value = systemMonitor.getDeviceInfo()
            _systemInfo.value = systemMonitor.getSystemInfo()
        }
    }
    
    private fun startMonitoring() {
        viewModelScope.launch {
            while (true) {
                _cpuInfo.value = systemMonitor.getCpuInfo()
                _batteryInfo.value = systemMonitor.getBatteryInfo()
                _displayInfo.value = systemMonitor.getDisplayInfo()
                _memoryInfo.value = systemMonitor.getMemoryInfo()
                _storageInfo.value = systemMonitor.getStorageInfo()
                _bluetoothInfo.value = systemMonitor.getBluetoothInfo()
                _audioInfo.value = systemMonitor.getAudioInfo()
                delay(1000)
            }
        }
    }
}
