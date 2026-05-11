package com.fusionx.vpn.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fusionx.vpn.ui.theme.CyanPrimary
import com.fusionx.vpn.ui.theme.PurplePrimary
import com.fusionx.vpn.ui.theme.TextSecondary

@Composable
fun TrafficStats(
    uploadSpeed: Long,
    downloadSpeed: Long,
    totalUpload: Long,
    totalDownload: Long,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.ArrowUpward,
                contentDescription = "Upload",
                tint = CyanPrimary
            )
            Text(
                text = formatSpeed(uploadSpeed),
                color = CyanPrimary
            )
            Text(
                text = formatBytes(totalUpload),
                color = TextSecondary,
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.ArrowDownward,
                contentDescription = "Download",
                tint = PurplePrimary
            )
            Text(
                text = formatSpeed(downloadSpeed),
                color = PurplePrimary
            )
            Text(
                text = formatBytes(totalDownload),
                color = TextSecondary,
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall
            )
        }
    }
}

fun formatSpeed(bytesPerSecond: Long): String {
    return when {
        bytesPerSecond < 1024 -> "$bytesPerSecond B/s"
        bytesPerSecond < 1024 * 1024 -> "${bytesPerSecond / 1024} KB/s"
        else -> String.format("%.1f MB/s", bytesPerSecond / (1024.0 * 1024.0))
    }
}

fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024L * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
