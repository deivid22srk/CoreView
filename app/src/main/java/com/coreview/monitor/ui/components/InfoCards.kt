package com.coreview.monitor.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery0Bar
import androidx.compose.material.icons.filled.Battery1Bar
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.Battery3Bar
import androidx.compose.material.icons.filled.Battery4Bar
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.Battery6Bar
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coreview.monitor.data.BatteryInfo
import com.coreview.monitor.data.DisplayInfo
import com.coreview.monitor.data.MemoryInfo

@Composable
fun BatteryCard(batteryInfo: BatteryInfo) {
    val batteryIcon = when {
        batteryInfo.status == "Charging" -> Icons.Default.BatteryChargingFull
        batteryInfo.level >= 90 -> Icons.Default.BatteryFull
        batteryInfo.level >= 75 -> Icons.Default.Battery6Bar
        batteryInfo.level >= 60 -> Icons.Default.Battery5Bar
        batteryInfo.level >= 45 -> Icons.Default.Battery4Bar
        batteryInfo.level >= 30 -> Icons.Default.Battery3Bar
        batteryInfo.level >= 15 -> Icons.Default.Battery2Bar
        batteryInfo.level >= 5 -> Icons.Default.Battery1Bar
        else -> Icons.Default.Battery0Bar
    }
    
    val batteryColor = when {
        batteryInfo.level >= 50 -> Color(0xFF4CAF50)
        batteryInfo.level >= 20 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = batteryIcon,
                        contentDescription = null,
                        tint = batteryColor,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Battery",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
                
                Column {
                    InfoRow(label = "Level", value = "${batteryInfo.level}%")
                    InfoRow(label = "Temperature", value = "${batteryInfo.temperature}°C")
                    InfoRow(label = "Voltage", value = "${batteryInfo.voltage / 1000f}V")
                    InfoRow(label = "Health", value = batteryInfo.health)
                    InfoRow(label = "Technology", value = batteryInfo.technology)
                    InfoRow(label = "Status", value = batteryInfo.status)
                }
            }
            
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressBar(
                    progress = batteryInfo.level / 100f,
                    modifier = Modifier.fillMaxSize(),
                    color = batteryColor,
                    strokeWidth = 16f
                )
                Text(
                    text = "${batteryInfo.level}%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DisplayCard(displayInfo: DisplayInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "Display",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
            
            Column {
                InfoRow(label = "Resolution", value = displayInfo.resolution)
                InfoRow(label = "Density", value = "${displayInfo.density}x")
                InfoRow(label = "Refresh Rate", value = "${displayInfo.refreshRate}Hz")
                InfoRow(label = "Size", value = String.format("%.2f\"", displayInfo.size))
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Brightness",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${(displayInfo.brightness / 255f * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    val animatedBrightness by animateFloatAsState(
                        targetValue = displayInfo.brightness / 255f,
                        animationSpec = tween(500),
                        label = "brightness"
                    )
                    
                    LinearProgressIndicator(
                        progress = { animatedBrightness },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .height(8.dp),
                        color = Color(0xFFFFB74D),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        strokeCap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}

@Composable
fun MemoryCard(memoryInfo: MemoryInfo) {
    val ramUsagePercent = if (memoryInfo.totalRam > 0) {
        (memoryInfo.usedRam.toFloat() / memoryInfo.totalRam.toFloat())
    } else {
        0f
    }
    
    val zramUsagePercent = if (memoryInfo.totalZram > 0) {
        (memoryInfo.usedZram.toFloat() / memoryInfo.totalZram.toFloat())
    } else {
        0f
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "Memory",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
            
            Column {
                MemoryProgressRow(
                    label = "RAM",
                    used = memoryInfo.usedRam,
                    total = memoryInfo.totalRam,
                    color = Color(0xFF2196F3)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                if (memoryInfo.totalZram > 0) {
                    MemoryProgressRow(
                        label = "ZRAM",
                        used = memoryInfo.usedZram,
                        total = memoryInfo.totalZram,
                        color = Color(0xFF9C27B0)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                if (memoryInfo.totalSwap > 0) {
                    MemoryProgressRow(
                        label = "Swap",
                        used = memoryInfo.usedSwap,
                        total = memoryInfo.totalSwap,
                        color = Color(0xFFFF5722)
                    )
                }
            }
        }
    }
}

@Composable
fun MemoryProgressRow(label: String, used: Long, total: Long, color: Color) {
    val usagePercent = if (total > 0) (used.toFloat() / total.toFloat()) else 0f
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                text = "${formatBytes(used)} / ${formatBytes(total)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        val animatedProgress by animateFloatAsState(
            targetValue = usagePercent,
            animationSpec = tween(500),
            label = "memory_progress"
        )
        
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .height(8.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            strokeCap = StrokeCap.Round,
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun formatBytes(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    
    return when {
        gb >= 1 -> String.format("%.2f GB", gb)
        mb >= 1 -> String.format("%.2f MB", mb)
        kb >= 1 -> String.format("%.2f KB", kb)
        else -> "$bytes B"
    }
}
