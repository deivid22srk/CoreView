package com.coreview.monitor.data

data class CpuCore(
    val coreId: Int,
    val currentFreq: Long,
    val minFreq: Long,
    val maxFreq: Long,
    val usage: Float,
    val isOnline: Boolean
)

data class CpuInfo(
    val cores: List<CpuCore>,
    val chipsetName: String,
    val chipsetModel: String,
    val processNode: String,
    val coreCount: Int,
    val is64Bit: Boolean,
    val manufacturer: String,
    val hardware: String,
    val architecture: String,
    val abi: String,
    val supportedAbis: List<String>,
    val governor: String,
    val coreConfigs: List<CoreConfig>
)

data class CoreConfig(
    val name: String,
    val count: Int,
    val minFreq: Long,
    val maxFreq: Long
)

data class GpuInfo(
    val name: String,
    val vendor: String,
    val renderer: String,
    val version: String,
    val glVersion: String,
    val vulkanVersion: String,
    val maxTextureSize: Int,
    val extensions: List<String>,
    val driverVersion: String
)

data class BatteryInfo(
    val level: Int,
    val temperature: Float,
    val voltage: Int,
    val health: String,
    val technology: String,
    val status: String,
    val capacity: Int,
    val current: Int,
    val chargeCounter: Int,
    val plugType: String
)

data class DisplayInfo(
    val resolution: String,
    val density: Float,
    val densityDpi: Int,
    val refreshRate: Float,
    val size: Float,
    val brightness: Int,
    val screenTimeout: Int,
    val orientation: String,
    val hdr: Boolean,
    val wideColorGamut: Boolean,
    val supportedRefreshRates: List<Float>
)

data class MemoryInfo(
    val totalRam: Long,
    val availableRam: Long,
    val usedRam: Long,
    val totalZram: Long,
    val usedZram: Long,
    val totalSwap: Long,
    val usedSwap: Long,
    val lowMemory: Boolean,
    val threshold: Long,
    val cached: Long,
    val buffers: Long
)

data class StorageInfo(
    val internalTotal: Long,
    val internalUsed: Long,
    val internalAvailable: Long,
    val externalTotal: Long,
    val externalUsed: Long,
    val externalAvailable: Long,
    val hasExternalSD: Boolean,
    val storageType: String,
    val isEncrypted: Boolean
)

data class BluetoothInfo(
    val enabled: Boolean,
    val name: String,
    val address: String,
    val state: String,
    val scanMode: String,
    val bondedDevices: Int,
    val supportedProfiles: List<String>,
    val bluetoothClass: String,
    val leSupported: Boolean,
    val le2MSupported: Boolean,
    val leCodedSupported: Boolean,
    val leExtendedAdvertisingSupported: Boolean
)

data class AudioInfo(
    val outputDevices: List<String>,
    val inputDevices: List<String>,
    val musicActive: Boolean,
    val speakerphoneOn: Boolean,
    val bluetoothScoOn: Boolean,
    val wiredHeadsetOn: Boolean,
    val microphoneMute: Boolean,
    val mode: String,
    val ringerMode: String,
    val volume: Int,
    val maxVolume: Int
)

data class DeviceInfo(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val device: String,
    val product: String,
    val board: String,
    val hardware: String,
    val fingerprint: String,
    val serialNumber: String,
    val androidId: String,
    val buildId: String,
    val bootloader: String,
    val radioVersion: String,
    val abis: List<String>
)

data class SystemInfo(
    val osVersion: String,
    val apiLevel: Int,
    val securityPatch: String,
    val buildType: String,
    val buildTags: String,
    val buildTime: Long,
    val buildUser: String,
    val buildHost: String,
    val kernelVersion: String,
    val javaVm: String,
    val javaVmVersion: String,
    val uptime: Long,
    val timezone: String,
    val language: String,
    val rootAccess: Boolean,
    val seLinuxStatus: String
)
