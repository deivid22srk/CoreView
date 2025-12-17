package com.coreview.monitor.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager
import com.coreview.monitor.data.BatteryInfo
import com.coreview.monitor.data.CoreConfig
import com.coreview.monitor.data.CpuCore
import com.coreview.monitor.data.CpuInfo
import com.coreview.monitor.data.DisplayInfo
import com.coreview.monitor.data.MemoryInfo
import java.io.File
import kotlin.math.sqrt

class SystemMonitor(private val context: Context) {

    fun getCpuInfo(): CpuInfo {
        val cores = getCpuCores()
        val hardware = getHardwareInfo()
        val supportedAbis = Build.SUPPORTED_ABIS.toList()
        val governor = readStringFromFile("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor") ?: "Unknown"
        
        val coreConfigs = detectCoreConfiguration(cores)
        
        return CpuInfo(
            cores = cores,
            chipsetName = hardware["chipset_name"] ?: "Unknown",
            chipsetModel = hardware["chipset_model"] ?: Build.HARDWARE,
            processNode = hardware["process_node"] ?: "Unknown",
            coreCount = cores.size,
            is64Bit = Build.SUPPORTED_64_BIT_ABIS.isNotEmpty(),
            manufacturer = hardware["manufacturer"] ?: Build.MANUFACTURER,
            hardware = Build.HARDWARE,
            architecture = System.getProperty("os.arch") ?: "Unknown",
            abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown",
            supportedAbis = supportedAbis,
            governor = governor,
            coreConfigs = coreConfigs
        )
    }

    private fun getCpuCores(): List<CpuCore> {
        val cores = mutableListOf<CpuCore>()
        var coreId = 0
        
        while (true) {
            val cpuDir = File("/sys/devices/system/cpu/cpu$coreId")
            if (!cpuDir.exists()) break
            
            val currentFreq = readLongFromFile("/sys/devices/system/cpu/cpu$coreId/cpufreq/scaling_cur_freq") ?: 0L
            val minFreq = readLongFromFile("/sys/devices/system/cpu/cpu$coreId/cpufreq/cpuinfo_min_freq") ?: 0L
            val maxFreq = readLongFromFile("/sys/devices/system/cpu/cpu$coreId/cpufreq/cpuinfo_max_freq") ?: 0L
            val usage = getCpuUsage(coreId)
            
            cores.add(CpuCore(coreId, currentFreq, minFreq, maxFreq, usage))
            coreId++
        }
        
        return cores
    }

    private fun detectCoreConfiguration(cores: List<CpuCore>): List<CoreConfig> {
        val configs = mutableListOf<CoreConfig>()
        val freqGroups = cores.groupBy { it.maxFreq }
        
        freqGroups.entries.sortedBy { it.key }.forEachIndexed { index, entry ->
            val coreName = when (index) {
                0 -> "Cortex-A55"
                1 -> "Cortex-A78"
                else -> "Core"
            }
            
            configs.add(
                CoreConfig(
                    name = coreName,
                    count = entry.value.size,
                    minFreq = entry.value.firstOrNull()?.minFreq ?: 0L,
                    maxFreq = entry.key
                )
            )
        }
        
        return configs
    }

    private fun getHardwareInfo(): Map<String, String> {
        val info = mutableMapOf<String, String>()
        
        try {
            val cpuInfo = File("/proc/cpuinfo").readText()
            
            cpuInfo.lines().forEach { line ->
                when {
                    line.startsWith("Hardware") -> {
                        val hardware = line.substringAfter(":").trim()
                        info["hardware_raw"] = hardware
                        
                        when {
                            hardware.contains("Qualcomm", ignoreCase = true) -> {
                                info["manufacturer"] = "Qualcomm"
                                info["chipset_name"] = extractQualcommChipset(hardware)
                            }
                            hardware.contains("MediaTek", ignoreCase = true) || hardware.contains("MT", ignoreCase = true) -> {
                                info["manufacturer"] = "MediaTek"
                                info["chipset_name"] = "MediaTek $hardware"
                            }
                            hardware.contains("Exynos", ignoreCase = true) -> {
                                info["manufacturer"] = "Samsung"
                                info["chipset_name"] = "Samsung $hardware"
                            }
                        }
                    }
                    line.startsWith("Processor") -> {
                        info["processor"] = line.substringAfter(":").trim()
                    }
                }
            }
            
            if (Build.HARDWARE.contains("qcom", ignoreCase = true) || 
                Build.HARDWARE.contains("qualcomm", ignoreCase = true)) {
                info["manufacturer"] = "Qualcomm"
                if (!info.containsKey("chipset_name")) {
                    info["chipset_name"] = "Qualcomm Snapdragon"
                }
            }
            
            info["chipset_model"] = Build.HARDWARE
            info["process_node"] = estimateProcessNode()
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return info
    }

    private fun extractQualcommChipset(hardware: String): String {
        return when {
            hardware.contains("SM", ignoreCase = true) -> {
                val model = hardware.substringAfter("SM", "").take(4)
                "Qualcomm Snapdragon (SM$model)"
            }
            else -> "Qualcomm Snapdragon"
        }
    }

    private fun estimateProcessNode(): String {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> "4-7 nm"
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> "5-8 nm"
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> "7-10 nm"
            else -> "10+ nm"
        }
    }

    private fun getCpuUsage(coreId: Int): Float {
        return try {
            val stat = File("/proc/stat").readLines()
            val coreLine = stat.find { it.startsWith("cpu$coreId ") } ?: return 0f
            val values = coreLine.split("\\s+".toRegex()).drop(1).map { it.toLong() }
            
            val idle = values.getOrNull(3) ?: 0L
            val total = values.sum()
            
            if (total == 0L) 0f else ((total - idle).toFloat() / total * 100f)
        } catch (e: Exception) {
            0f
        }
    }

    fun getBatteryInfo(): BatteryInfo {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, intentFilter)
        
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val temperature = (batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1) / 10f
        val voltage = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
        val health = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val technology = batteryStatus?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        
        val batteryLevel = if (level != -1 && scale != -1) {
            (level.toFloat() / scale.toFloat() * 100f).toInt()
        } else {
            0
        }
        
        val healthStr = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }
        
        val statusStr = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            else -> "Unknown"
        }
        
        val capacity = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        
        return BatteryInfo(
            level = batteryLevel,
            temperature = temperature,
            voltage = voltage,
            health = healthStr,
            technology = technology,
            status = statusStr,
            capacity = capacity
        )
    }

    fun getDisplayInfo(): DisplayInfo {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        
        val widthPixels = displayMetrics.widthPixels
        val heightPixels = displayMetrics.heightPixels
        val density = displayMetrics.density
        val refreshRate = windowManager.defaultDisplay.refreshRate
        
        val widthInches = widthPixels / displayMetrics.xdpi
        val heightInches = heightPixels / displayMetrics.ydpi
        val diagonalInches = sqrt((widthInches * widthInches + heightInches * heightInches).toDouble()).toFloat()
        
        val brightness = try {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Exception) {
            0
        }
        
        return DisplayInfo(
            resolution = "${widthPixels}x${heightPixels}",
            density = density,
            refreshRate = refreshRate,
            size = diagonalInches,
            brightness = brightness
        )
    }

    fun getMemoryInfo(): MemoryInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        val totalRam = memInfo.totalMem
        val availableRam = memInfo.availMem
        val usedRam = totalRam - availableRam
        
        val meminfoFile = File("/proc/meminfo")
        var totalZram = 0L
        var usedZram = 0L
        var totalSwap = 0L
        var usedSwap = 0L
        
        try {
            val lines = meminfoFile.readLines()
            for (line in lines) {
                when {
                    line.startsWith("SwapTotal:") -> {
                        totalSwap = line.split("\\s+".toRegex())[1].toLong() * 1024
                    }
                    line.startsWith("SwapFree:") -> {
                        val swapFree = line.split("\\s+".toRegex())[1].toLong() * 1024
                        usedSwap = totalSwap - swapFree
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        val zramFile = File("/sys/block/zram0/mm_stat")
        if (zramFile.exists()) {
            try {
                val zramLine = zramFile.readText().trim()
                val values = zramLine.split("\\s+".toRegex())
                if (values.size >= 2) {
                    usedZram = values[0].toLong()
                    totalZram = values[1].toLong()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        return MemoryInfo(
            totalRam = totalRam,
            availableRam = availableRam,
            usedRam = usedRam,
            totalZram = totalZram,
            usedZram = usedZram,
            totalSwap = totalSwap,
            usedSwap = usedSwap
        )
    }

    private fun readLongFromFile(path: String): Long? {
        return try {
            File(path).readText().trim().toLong()
        } catch (e: Exception) {
            null
        }
    }

    private fun readStringFromFile(path: String): String? {
        return try {
            File(path).readText().trim()
        } catch (e: Exception) {
            null
        }
    }
}
