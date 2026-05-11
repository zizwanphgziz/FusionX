package com.fusionx.vpn.ui.screens

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.fusionx.vpn.R
import com.fusionx.vpn.ui.components.TrafficStats
import com.fusionx.vpn.ui.components.WatermarkOverlay
import com.fusionx.vpn.ui.theme.CyanDark
import com.fusionx.vpn.ui.theme.CyanPrimary
import com.fusionx.vpn.ui.theme.DarkBackground
import com.fusionx.vpn.ui.theme.DarkCard
import com.fusionx.vpn.ui.theme.GreenConnected
import com.fusionx.vpn.ui.theme.PurplePrimary
import com.fusionx.vpn.ui.theme.RedDisconnected
import com.fusionx.vpn.ui.theme.TextSecondary
import com.fusionx.vpn.ui.theme.YellowConnecting
import com.fusionx.vpn.viewmodel.ConnectionState
import com.fusionx.vpn.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToServers: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onConnectToggle: () -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val selectedProfile by viewModel.selectedProfile.collectAsState()
    val uploadSpeed by viewModel.uploadSpeed.collectAsState()
    val downloadSpeed by viewModel.downloadSpeed.collectAsState()
    val totalUpload by viewModel.totalUpload.collectAsState()
    val totalDownload by viewModel.totalDownload.collectAsState()
    val wallpaperUri by viewModel.wallpaperUri.collectAsState()
    val wallpaperFade by viewModel.wallpaperFade.collectAsState()

    val isConnected = connectionState == ConnectionState.CONNECTED
    val isConnecting = connectionState == ConnectionState.CONNECTING

    val buttonColor by animateColorAsState(
        targetValue = when (connectionState) {
            ConnectionState.CONNECTED -> GreenConnected
            ConnectionState.CONNECTING, ConnectionState.DISCONNECTING -> YellowConnecting
            ConnectionState.DISCONNECTED -> CyanPrimary
        },
        animationSpec = tween(500),
        label = "buttonColor"
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (isConnecting) 1.1f else 1f,
        animationSpec = tween(300),
        label = "buttonScale"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Custom wallpaper background — fills screen, respects fade setting
        if (wallpaperUri.isNotEmpty()) {
            Image(
                painter = rememberAsyncImagePainter(Uri.parse(wallpaperUri)),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = if (wallpaperFade) 0.4f else 1f
            )
        }

        // Gradient overlay (lighter when wallpaper is visible & not faded)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (wallpaperUri.isNotEmpty() && !wallpaperFade) {
                            listOf(
                                DarkBackground.copy(alpha = 0.5f),
                                DarkBackground.copy(alpha = 0.3f),
                                DarkBackground.copy(alpha = 0.5f)
                            )
                        } else {
                            listOf(
                                DarkBackground.copy(alpha = 0.9f),
                                DarkBackground.copy(alpha = 0.7f),
                                DarkBackground.copy(alpha = 0.9f)
                            )
                        }
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar with logo + title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.fusionx_logo),
                        contentDescription = "FusionX Logo",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "FusionX",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = CyanPrimary
                    )
                }
                Row {
                    IconButton(onClick = onNavigateToServers) {
                        Icon(Icons.AutoMirrored.Filled.List, "Servers", tint = TextSecondary)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Settings", tint = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.3f))

            // Status text
            Text(
                text = when (connectionState) {
                    ConnectionState.CONNECTED -> "PROTECTED"
                    ConnectionState.CONNECTING -> "CONNECTING..."
                    ConnectionState.DISCONNECTING -> "DISCONNECTING..."
                    ConnectionState.DISCONNECTED -> "NOT CONNECTED"
                },
                style = MaterialTheme.typography.titleMedium,
                color = buttonColor,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Connect button
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(buttonScale)
                    .shadow(
                        elevation = if (isConnected) 16.dp else 8.dp,
                        shape = CircleShape,
                        ambientColor = buttonColor.copy(alpha = 0.4f),
                        spotColor = buttonColor.copy(alpha = 0.4f)
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                buttonColor.copy(alpha = 0.2f),
                                DarkCard
                            )
                        )
                    )
                    .border(2.dp, buttonColor.copy(alpha = 0.6f), CircleShape)
                    .clickable(onClick = onConnectToggle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PowerSettingsNew,
                    contentDescription = "Connect",
                    tint = buttonColor,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Traffic stats
            if (isConnected) {
                TrafficStats(
                    uploadSpeed = uploadSpeed,
                    downloadSpeed = downloadSpeed,
                    totalUpload = totalUpload,
                    totalDownload = totalDownload
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Selected server card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToServers),
                colors = CardDefaults.cardColors(containerColor = DarkCard.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedProfile?.displayName ?: "No server selected",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (selectedProfile != null) CyanPrimary else TextSecondary
                        )
                        if (selectedProfile != null) {
                            Text(
                                text = "${selectedProfile?.protocol?.uppercase()} \u2022 ${selectedProfile?.address}:${selectedProfile?.port}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            if (selectedProfile?.isStrx == true) {
                                Text(
                                    text = "STRX PAYLOAD",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PurplePrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    if (selectedProfile?.latency != null && selectedProfile?.latency != -1L) {
                        Text(
                            text = "${selectedProfile?.latency}ms",
                            color = when {
                                (selectedProfile?.latency ?: 0) < 100 -> GreenConnected
                                (selectedProfile?.latency ?: 0) < 300 -> YellowConnecting
                                else -> RedDisconnected
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        // Watermark
        WatermarkOverlay()
    }
}
