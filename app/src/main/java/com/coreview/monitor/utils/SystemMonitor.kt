package com.coreview.monitor.utils

import android.Manifest
import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.opengl.EGL14
import android.opengl.GLES20
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import com.coreview.monitor.data.*
import java.io.File
import java.io.RandomAccessFile
import java.util.Locale
import java.util.TimeZone
import kotlin.math.sqrt

class SystemMonitor(private val context: Context) {

    private var lastCpuStats = mutableMapOf<Int, Pair<Long, Long>>()

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
            
            val isOnline = readStringFromFile("/sys/devices/system/cpu/cpu$coreId/online")?.toIntOrNull() != 0
            val currentFreq = readLongFromFile("/sys/devices/system/cpu/cpu$coreId/cpufreq/scaling_cur_freq") ?: 0L
            val minFreq = readLongFromFile("/sys/devices/system/cpu/cpu$coreId/cpufreq/cpuinfo_min_freq") ?: 0L
            val maxFreq = readLongFromFile("/sys/devices/system/cpu/cpu$coreId/cpufreq/cpuinfo_max_freq") ?: 0L
            val usage = getCpuUsageImproved(coreId)
            
            cores.add(CpuCore(coreId, currentFreq, minFreq, maxFreq, usage, isOnline))
            coreId++
        }
        
        return cores
    }

    private fun getCpuUsageImproved(coreId: Int): Float {
        return try {
            val stat = File("/proc/stat").readLines()
            val coreLine = stat.find { it.startsWith("cpu$coreId ") } ?: return 0f
            val values = coreLine.trim().split(Regex("\\s+")).drop(1).mapNotNull { it.toLongOrNull() }
            
            if (values.size < 4) return 0f
            
            val idle = values.getOrNull(3) ?: 0L
            val iowait = values.getOrNull(4) ?: 0L
            val totalIdle = idle + iowait
            val total = values.sum()
            
            val lastStats = lastCpuStats[coreId]
            lastCpuStats[coreId] = Pair(total, totalIdle)
            
            if (lastStats != null) {
                val totalDiff = total - lastStats.first
                val idleDiff = totalIdle - lastStats.second
                
                if (totalDiff > 0) {
                    val usage = ((totalDiff - idleDiff).toFloat() / totalDiff.toFloat()) * 100f
                    return usage.coerceIn(0f, 100f)
                }
            }
            
            if (total > 0) {
                return ((total - totalIdle).toFloat() / total.toFloat() * 100f).coerceIn(0f, 100f)
            }
            
            return 0f
        } catch (e: Exception) {
            e.printStackTrace()
            return 0f
        }
    }

    private fun detectCoreConfiguration(cores: List<CpuCore>): List<CoreConfig> {
        val configs = mutableListOf<CoreConfig>()
        val freqGroups = cores.groupBy { it.maxFreq }.entries.sortedBy { it.key }
        
        freqGroups.forEachIndexed { index, entry ->
            val coreName = when {
                entry.key < 1500000 -> "Cortex-A55"
                entry.key < 2500000 -> "Cortex-A78"
                else -> "Cortex-X1"
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

    fun getGpuInfo(): GpuInfo {
        return try {
            val eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            EGL14.eglInitialize(eglDisplay, null, 0, null, 0)
            
            val vendor = GLES20.glGetString(GLES20.GL_VENDOR) ?: "Unknown"
            val renderer = GLES20.glGetString(GLES20.GL_RENDERER) ?: "Unknown"
            val version = GLES20.glGetString(GLES20.GL_VERSION) ?: "Unknown"
            val glVersion = GLES20.glGetString(GLES20.GL_VERSION) ?: "Unknown"
            val extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS)?.split(" ") ?: emptyList()
            
            val maxTextureSizeBuffer = IntArray(1)
            GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxTextureSizeBuffer, 0)
            
            GpuInfo(
                name = renderer,
                vendor = vendor,
                renderer = renderer,
                version = version,
                glVersion = glVersion,
                vulkanVersion = getVulkanVersion(),
                maxTextureSize = maxTextureSizeBuffer[0],
                extensions = extensions.take(20),
                driverVersion = "Unknown"
            )
        } catch (e: Exception) {
            GpuInfo(
                name = "Unable to detect",
                vendor = "Unknown",
                renderer = "Unknown",
                version = "Unknown",
                glVersion = "Unknown",
                vulkanVersion = "Unknown",
                maxTextureSize = 0,
                extensions = emptyList(),
                driverVersion = "Unknown"
            )
        }
    }

    private fun getVulkanVersion(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val pm = context.packageManager
                if (pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL)) {
                    "Vulkan 1.1+"
                } else {
                    "Not Supported"
                }
            } else {
                "Not Supported"
            }
        } catch (e: Exception) {
            "Unknown"
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
        val plugType = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        
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
        
        val plugTypeStr = when (plugType) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "Not Plugged"
        }
        
        val capacity = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val current = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val chargeCounter = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        
        return BatteryInfo(
            level = batteryLevel,
            temperature = temperature,
            voltage = voltage,
            health = healthStr,
            technology = technology,
            status = statusStr,
            capacity = capacity,
            current = current,
            chargeCounter = chargeCounter,
            plugType = plugTypeStr
        )
    }

    fun getDisplayInfo(): DisplayInfo {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        val display = windowManager.defaultDisplay
        display.getMetrics(displayMetrics)
        
        val widthPixels = displayMetrics.widthPixels
        val heightPixels = displayMetrics.heightPixels
        val density = displayMetrics.density
        val densityDpi = displayMetrics.densityDpi
        val refreshRate = display.refreshRate
        
        val widthInches = widthPixels / displayMetrics.xdpi
        val heightInches = heightPixels / displayMetrics.ydpi
        val diagonalInches = sqrt((widthInches * widthInches + heightInches * heightInches).toDouble()).toFloat()
        
        val brightness = try {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Exception) {
            0
        }
        
        val screenTimeout = try {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT)
        } catch (e: Exception) {
            0
        }
        
        val orientation = when (display.rotation) {
            0 -> "Portrait"
            1 -> "Landscape (90°)"
            2 -> "Portrait (180°)"
            3 -> "Landscape (270°)"
            else -> "Unknown"
        }
        
        val supportedModes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            display.supportedModes.map { it.refreshRate }
        } else {
            listOf(refreshRate)
        }
        
        val hdr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            display.isHdr
        } else {
            false
        }
        
        val wideColorGamut = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            display.isWideColorGamut
        } else {
            false
        }
        
        return DisplayInfo(
            resolution = "${widthPixels}x${heightPixels}",
            density = density,
            densityDpi = densityDpi,
            refreshRate = refreshRate,
            size = diagonalInches,
            brightness = brightness,
            screenTimeout = screenTimeout,
            orientation = orientation,
            hdr = hdr,
            wideColorGamut = wideColorGamut,
            supportedRefreshRates = supportedModes
        )
    }

    fun getMemoryInfo(): MemoryInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        val totalRam = memInfo.totalMem
        val availableRam = memInfo.availMem
        val usedRam = totalRam - availableRam
        val lowMemory = memInfo.lowMemory
        val threshold = memInfo.threshold
        
        val meminfoFile = File("/proc/meminfo")
        var totalZram = 0L
        var usedZram = 0L
        var totalSwap = 0L
        var usedSwap = 0L
        var cached = 0L
        var buffers = 0L
        
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
                    line.startsWith("Cached:") -> {
                        cached = line.split("\\s+".toRegex())[1].toLong() * 1024
                    }
                    line.startsWith("Buffers:") -> {
                        buffers = line.split("\\s+".toRegex())[1].toLong() * 1024
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
            usedSwap = usedSwap,
            lowMemory = lowMemory,
            threshold = threshold,
            cached = cached,
            buffers = buffers
        )
    }

    fun getStorageInfo(): StorageInfo {
        val internalPath = Environment.getDataDirectory()
        val internalStat = StatFs(internalPath.path)
        
        val internalBlockSize = internalStat.blockSizeLong
        val internalTotalBlocks = internalStat.blockCountLong
        val internalAvailableBlocks = internalStat.availableBlocksLong
        
        val internalTotal = internalTotalBlocks * internalBlockSize
        val internalAvailable = internalAvailableBlocks * internalBlockSize
        val internalUsed = internalTotal - internalAvailable
        
        var externalTotal = 0L
        var externalUsed = 0L
        var externalAvailable = 0L
        var hasExternalSD = false
        
        try {
            val externalPath = Environment.getExternalStorageDirectory()
            if (externalPath != null && Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                val externalStat = StatFs(externalPath.path)
                val externalBlockSize = externalStat.blockSizeLong
                val externalTotalBlocks = externalStat.blockCountLong
                val externalAvailableBlocks = externalStat.availableBlocksLong
                
                externalTotal = externalTotalBlocks * externalBlockSize
                externalAvailable = externalAvailableBlocks * externalBlockSize
                externalUsed = externalTotal - externalAvailable
                hasExternalSD = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        val storageType = detectStorageType()
        val isEncrypted = isStorageEncrypted()
        
        return StorageInfo(
            internalTotal = internalTotal,
            internalUsed = internalUsed,
            internalAvailable = internalAvailable,
            externalTotal = externalTotal,
            externalUsed = externalUsed,
            externalAvailable = externalAvailable,
            hasExternalSD = hasExternalSD,
            storageType = storageType,
            isEncrypted = isEncrypted
        )
    }

    private fun detectStorageType(): String {
        return try {
            val mountsFile = File("/proc/mounts")
            val mounts = mountsFile.readText()
            when {
                mounts.contains("f2fs") -> "F2FS"
                mounts.contains("ext4") -> "EXT4"
                mounts.contains("vfat") -> "FAT32"
                else -> "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun isStorageEncrypted(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val dm = context.getSystemService(Context.DEVICE_POLICY_SERVICE)
                dm != null
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun getBluetoothInfo(): BluetoothInfo {
        return try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val bluetoothAdapter = bluetoothManager?.adapter
            
            if (bluetoothAdapter == null) {
                return BluetoothInfo(
                    enabled = false,
                    name = "Not Available",
                    address = "N/A",
                    state = "Not Supported",
                    scanMode = "N/A",
                    bondedDevices = 0,
                    supportedProfiles = emptyList(),
                    bluetoothClass = "N/A",
                    leSupported = false,
                    le2MSupported = false,
                    leCodedSupported = false,
                    leExtendedAdvertisingSupported = false
                )
            }
            
            val enabled = bluetoothAdapter.isEnabled
            val name = if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                bluetoothAdapter.name ?: "Unknown"
            } else {
                "Permission Required"
            }
            val address = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    "Hidden for privacy"
                } else {
                    bluetoothAdapter.address ?: "Unknown"
                }
            } catch (e: Exception) {
                "Unknown"
            }
            
            val state = when (bluetoothAdapter.state) {
                BluetoothAdapter.STATE_ON -> "On"
                BluetoothAdapter.STATE_OFF -> "Off"
                BluetoothAdapter.STATE_TURNING_ON -> "Turning On"
                BluetoothAdapter.STATE_TURNING_OFF -> "Turning Off"
                else -> "Unknown"
            }
            
            val scanMode = when (bluetoothAdapter.scanMode) {
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE -> "Discoverable"
                BluetoothAdapter.SCAN_MODE_CONNECTABLE -> "Connectable"
                BluetoothAdapter.SCAN_MODE_NONE -> "None"
                else -> "Unknown"
            }
            
            val bondedDevices = try {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    bluetoothAdapter.bondedDevices?.size ?: 0
                } else {
                    0
                }
            } catch (e: Exception) {
                0
            }
            
            val leSupported = bluetoothAdapter.isMultipleAdvertisementSupported
            val le2MSupported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                bluetoothAdapter.isLe2MPhySupported
            } else {
                false
            }
            val leCodedSupported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                bluetoothAdapter.isLeCodedPhySupported
            } else {
                false
            }
            val leExtendedAdvertisingSupported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                bluetoothAdapter.isLeExtendedAdvertisingSupported
            } else {
                false
            }
            
            BluetoothInfo(
                enabled = enabled,
                name = name,
                address = address,
                state = state,
                scanMode = scanMode,
                bondedDevices = bondedDevices,
                supportedProfiles = listOf("A2DP", "HFP", "HSP", "AVRCP", "PBAP", "HID", "PAN"),
                bluetoothClass = "Class 2",
                leSupported = leSupported,
                le2MSupported = le2MSupported,
                leCodedSupported = leCodedSupported,
                leExtendedAdvertisingSupported = leExtendedAdvertisingSupported
            )
        } catch (e: Exception) {
            BluetoothInfo(
                enabled = false,
                name = "Error",
                address = "N/A",
                state = "Error",
                scanMode = "N/A",
                bondedDevices = 0,
                supportedProfiles = emptyList(),
                bluetoothClass = "N/A",
                leSupported = false,
                le2MSupported = false,
                leCodedSupported = false,
                leExtendedAdvertisingSupported = false
            )
        }
    }

    fun getAudioInfo(): AudioInfo {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        val outputDevices = mutableListOf<String>()
        val inputDevices = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_ALL)
            devices.forEach { device ->
                val deviceName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    device.productName.toString()
                } else {
                    device.type.toString()
                }
                
                if (device.isSink) {
                    outputDevices.add(deviceName)
                } else {
                    inputDevices.add(deviceName)
                }
            }
        }
        
        val musicActive = audioManager.isMusicActive
        val speakerphoneOn = audioManager.isSpeakerphoneOn
        val bluetoothScoOn = audioManager.isBluetoothScoOn
        val wiredHeadsetOn = audioManager.isWiredHeadsetOn
        val microphoneMute = audioManager.isMicrophoneMute
        
        val mode = when (audioManager.mode) {
            AudioManager.MODE_NORMAL -> "Normal"
            AudioManager.MODE_RINGTONE -> "Ringtone"
            AudioManager.MODE_IN_CALL -> "In Call"
            AudioManager.MODE_IN_COMMUNICATION -> "In Communication"
            else -> "Unknown"
        }
        
        val ringerMode = when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> "Silent"
            AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
            AudioManager.RINGER_MODE_NORMAL -> "Normal"
            else -> "Unknown"
        }
        
        val volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        
        return AudioInfo(
            outputDevices = outputDevices.ifEmpty { listOf("Built-in Speaker") },
            inputDevices = inputDevices.ifEmpty { listOf("Built-in Microphone") },
            musicActive = musicActive,
            speakerphoneOn = speakerphoneOn,
            bluetoothScoOn = bluetoothScoOn,
            wiredHeadsetOn = wiredHeadsetOn,
            microphoneMute = microphoneMute,
            mode = mode,
            ringerMode = ringerMode,
            volume = volume,
            maxVolume = maxVolume
        )
    }

    fun getDeviceInfo(): DeviceInfo {
        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            "Unknown"
        }
        
        val serialNumber = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                "Protected"
            } else {
                Build.SERIAL
            }
        } catch (e: Exception) {
            "Unknown"
        }
        
        return DeviceInfo(
            manufacturer = Build.MANUFACTURER,
            brand = Build.BRAND,
            model = Build.MODEL,
            device = Build.DEVICE,
            product = Build.PRODUCT,
            board = Build.BOARD,
            hardware = Build.HARDWARE,
            fingerprint = Build.FINGERPRINT,
            serialNumber = serialNumber,
            androidId = androidId,
            buildId = Build.ID,
            bootloader = Build.BOOTLOADER,
            radioVersion = Build.getRadioVersion(),
            abis = Build.SUPPORTED_ABIS.toList()
        )
    }

    fun getSystemInfo(): SystemInfo {
        val kernelVersion = System.getProperty("os.version") ?: "Unknown"
        val javaVm = System.getProperty("java.vm.name") ?: "Unknown"
        val javaVmVersion = System.getProperty("java.vm.version") ?: "Unknown"
        val uptime = SystemClock.elapsedRealtime()
        val timezone = TimeZone.getDefault().displayName
        val language = Locale.getDefault().displayName
        
        val securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Build.VERSION.SECURITY_PATCH
        } else {
            "N/A"
        }
        
        val rootAccess = checkRootAccess()
        val seLinuxStatus = getSELinuxStatus()
        
        return SystemInfo(
            osVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            securityPatch = securityPatch,
            buildType = Build.TYPE,
            buildTags = Build.TAGS,
            buildTime = Build.TIME,
            buildUser = Build.USER,
            buildHost = Build.HOST,
            kernelVersion = kernelVersion,
            javaVm = javaVm,
            javaVmVersion = javaVmVersion,
            uptime = uptime,
            timezone = timezone,
            language = language,
            rootAccess = rootAccess,
            seLinuxStatus = seLinuxStatus
        )
    }

    private fun checkRootAccess(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        
        return paths.any { File(it).exists() }
    }

    private fun getSELinuxStatus(): String {
        return try {
            val seLinuxFile = File("/sys/fs/selinux/enforce")
            if (seLinuxFile.exists()) {
                val status = seLinuxFile.readText().trim()
                when (status) {
                    "1" -> "Enforcing"
                    "0" -> "Permissive"
                    else -> "Unknown"
                }
            } else {
                "Disabled"
            }
        } catch (e: Exception) {
            "Unknown"
        }
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
