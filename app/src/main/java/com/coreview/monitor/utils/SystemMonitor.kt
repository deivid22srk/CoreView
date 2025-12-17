package com.coreview.monitor.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager
import com.coreview.monitor.data.BatteryInfo
import com.coreview.monitor.data.CpuCore
import com.coreview.monitor.data.DisplayInfo
import com.coreview.monitor.data.MemoryInfo
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.sqrt

class SystemMonitor(private val context: Context) {

    fun getCpuCores(): List<CpuCore> {
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
}
