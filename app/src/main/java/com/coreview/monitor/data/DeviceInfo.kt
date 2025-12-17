package com.coreview.monitor.data

data class CpuCore(
    val coreId: Int,
    val currentFreq: Long,
    val minFreq: Long,
    val maxFreq: Long,
    val usage: Float
)

data class BatteryInfo(
    val level: Int,
    val temperature: Float,
    val voltage: Int,
    val health: String,
    val technology: String,
    val status: String,
    val capacity: Int
)

data class DisplayInfo(
    val resolution: String,
    val density: Float,
    val refreshRate: Float,
    val size: Float,
    val brightness: Int
)

data class MemoryInfo(
    val totalRam: Long,
    val availableRam: Long,
    val usedRam: Long,
    val totalZram: Long,
    val usedZram: Long,
    val totalSwap: Long,
    val usedSwap: Long
)
