package com.coreview.monitor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coreview.monitor.data.*

@Composable
fun GpuDetailCard(gpuInfo: GpuInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFE91E63),
                                    Color(0xFF9C27B0)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Column {
                    Text(
                        text = "GPU",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = gpuInfo.vendor,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            
            DetailInfoRow(label = "Name", value = gpuInfo.name)
            DetailInfoRow(label = "Vendor", value = gpuInfo.vendor)
            DetailInfoRow(label = "Renderer", value = gpuInfo.renderer)
            DetailInfoRow(label = "OpenGL Version", value = gpuInfo.glVersion)
            DetailInfoRow(label = "Vulkan Support", value = gpuInfo.vulkanVersion)
            DetailInfoRow(label = "Max Texture Size", value = "${gpuInfo.maxTextureSize}px")
            DetailInfoRow(label = "Driver Version", value = gpuInfo.driverVersion)
            
            if (gpuInfo.extensions.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                
                Text(
                    text = "Extensions (${gpuInfo.extensions.size} total)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    gpuInfo.extensions.take(10).forEach { ext ->
                        Text(
                            text = "  • $ext",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StorageDetailCard(storageInfo: StorageInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF00BCD4),
                                    Color(0xFF009688)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SdCard,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Column {
                    Text(
                        text = "Storage",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatBytes(storageInfo.internalTotal),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            
            Text(
                text = "Internal Storage",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            MemoryProgressBar(
                label = "Used",
                used = storageInfo.internalUsed,
                total = storageInfo.internalTotal,
                color = Color(0xFF00BCD4)
            )
            
            DetailInfoRow(label = "Total", value = formatBytes(storageInfo.internalTotal))
            DetailInfoRow(label = "Used", value = formatBytes(storageInfo.internalUsed))
            DetailInfoRow(label = "Available", value = formatBytes(storageInfo.internalAvailable))
            
            if (storageInfo.hasExternalSD) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                
                Text(
                    text = "External Storage",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                MemoryProgressBar(
                    label = "Used",
                    used = storageInfo.externalUsed,
                    total = storageInfo.externalTotal,
                    color = Color(0xFF009688)
                )
                
                DetailInfoRow(label = "Total", value = formatBytes(storageInfo.externalTotal))
                DetailInfoRow(label = "Used", value = formatBytes(storageInfo.externalUsed))
                DetailInfoRow(label = "Available", value = formatBytes(storageInfo.externalAvailable))
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            
            DetailInfoRow(label = "File System", value = storageInfo.storageType)
            DetailInfoRow(label = "Encryption", value = if (storageInfo.isEncrypted) "Enabled" else "Disabled")
        }
    }
}

@Composable
fun BluetoothDetailCard(bluetoothInfo: BluetoothInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (bluetoothInfo.enabled) {
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF2196F3),
                                        Color(0xFF1976D2)
                                    )
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF757575),
                                        Color(0xFF616161)
                                    )
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Column {
                    Text(
                        text = "Bluetooth",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = bluetoothInfo.state,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (bluetoothInfo.enabled) Color(0xFF4CAF50) else Color(0xFFFF5722)
                    )
                }
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            
            DetailInfoRow(label = "Device Name", value = bluetoothInfo.name)
            DetailInfoRow(label = "Address", value = bluetoothInfo.address)
            DetailInfoRow(label = "State", value = bluetoothInfo.state)
            DetailInfoRow(label = "Scan Mode", value = bluetoothInfo.scanMode)
            DetailInfoRow(label = "Bonded Devices", value = "${bluetoothInfo.bondedDevices}")
            DetailInfoRow(label = "Class", value = bluetoothInfo.bluetoothClass)
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            
            Text(
                text = "Supported Features",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            FeatureRow("BLE Support", bluetoothInfo.leSupported)
            FeatureRow("BLE 2M PHY", bluetoothInfo.le2MSupported)
            FeatureRow("BLE Coded PHY", bluetoothInfo.leCodedSupported)
            FeatureRow("Extended Advertising", bluetoothInfo.leExtendedAdvertisingSupported)
            
            if (bluetoothInfo.supportedProfiles.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                
                Text(
                    text = "Supported Profiles",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    bluetoothInfo.supportedProfiles.forEach { profile ->
                        Text(
                            text = "  • $profile",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AudioDetailCard(audioInfo: AudioInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFF5722),
                                    Color(0xFFE91E63)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Column {
                    Text(
                        text = "Audio",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = audioInfo.mode,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            
            DetailInfoRow(label = "Mode", value = audioInfo.mode)
            DetailInfoRow(label = "Ringer Mode", value = audioInfo.ringerMode)
            DetailInfoRow(label = "Volume", value = "${audioInfo.volume} / ${audioInfo.maxVolume}")
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            
            Text(
                text = "Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            FeatureRow("Music Active", audioInfo.musicActive)
            FeatureRow("Speakerphone", audioInfo.speakerphoneOn)
            FeatureRow("Bluetooth SCO", audioInfo.bluetoothScoOn)
            FeatureRow("Wired Headset", audioInfo.wiredHeadsetOn)
            FeatureRow("Microphone Muted", audioInfo.microphoneMute)
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            
            if (audioInfo.outputDevices.isNotEmpty()) {
                Text(
                    text = "Output Devices",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    audioInfo.outputDevices.forEach { device ->
                        Text(
                            text = "  • $device",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            if (audioInfo.inputDevices.isNotEmpty()) {
                Text(
                    text = "Input Devices",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    audioInfo.inputDevices.forEach { device ->
                        Text(
                            text = "  • $device",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceInfoCard(deviceInfo: DeviceInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF4CAF50),
                                    Color(0xFF8BC34A)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Column {
                    Text(
                        text = "Device Info",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = deviceInfo.brand,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            
            DetailInfoRow(label = "Manufacturer", value = deviceInfo.manufacturer)
            DetailInfoRow(label = "Brand", value = deviceInfo.brand)
            DetailInfoRow(label = "Model", value = deviceInfo.model)
            DetailInfoRow(label = "Device", value = deviceInfo.device)
            DetailInfoRow(label = "Product", value = deviceInfo.product)
            DetailInfoRow(label = "Board", value = deviceInfo.board)
            DetailInfoRow(label = "Hardware", value = deviceInfo.hardware)
            DetailInfoRow(label = "Build ID", value = deviceInfo.buildId)
            DetailInfoRow(label = "Bootloader", value = deviceInfo.bootloader)
            DetailInfoRow(label = "Radio Version", value = deviceInfo.radioVersion)
            DetailInfoRow(label = "Serial Number", value = deviceInfo.serialNumber)
            DetailInfoRow(label = "Android ID", value = deviceInfo.androidId)
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            
            Text(
                text = "Supported ABIs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                deviceInfo.abis.forEach { abi ->
                    Text(
                        text = "  • $abi",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            
            Text(
                text = "Fingerprint",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = deviceInfo.fingerprint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun SystemInfoCard(systemInfo: SystemInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFF9800),
                                    Color(0xFFFF5722)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Column {
                    Text(
                        text = "System Info",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Android ${systemInfo.osVersion}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            
            DetailInfoRow(label = "OS Version", value = "Android ${systemInfo.osVersion}")
            DetailInfoRow(label = "API Level", value = "${systemInfo.apiLevel}")
            DetailInfoRow(label = "Security Patch", value = systemInfo.securityPatch)
            DetailInfoRow(label = "Build Type", value = systemInfo.buildType)
            DetailInfoRow(label = "Build Tags", value = systemInfo.buildTags)
            DetailInfoRow(label = "Build User", value = systemInfo.buildUser)
            DetailInfoRow(label = "Build Host", value = systemInfo.buildHost)
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            
            DetailInfoRow(label = "Kernel Version", value = systemInfo.kernelVersion)
            DetailInfoRow(label = "Java VM", value = systemInfo.javaVm)
            DetailInfoRow(label = "Java VM Version", value = systemInfo.javaVmVersion)
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            
            val uptimeHours = systemInfo.uptime / (1000 * 60 * 60)
            val uptimeMinutes = (systemInfo.uptime % (1000 * 60 * 60)) / (1000 * 60)
            DetailInfoRow(label = "Uptime", value = "${uptimeHours}h ${uptimeMinutes}m")
            DetailInfoRow(label = "Timezone", value = systemInfo.timezone)
            DetailInfoRow(label = "Language", value = systemInfo.language)
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            
            Text(
                text = "Security",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            FeatureRow("Root Access", systemInfo.rootAccess)
            DetailInfoRow(label = "SELinux Status", value = systemInfo.seLinuxStatus)
        }
    }
}

@Composable
fun FeatureRow(label: String, enabled: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (enabled) Color(0xFF4CAF50) else Color(0xFFFF5722))
            )
            Text(
                text = if (enabled) "Yes" else "No",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) Color(0xFF4CAF50) else Color(0xFFFF5722)
            )
        }
    }
}
