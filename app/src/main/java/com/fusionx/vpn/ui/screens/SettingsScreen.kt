package com.fusionx.vpn.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fusionx.vpn.data.PrefsManager
import com.fusionx.vpn.ui.theme.CyanPrimary
import com.fusionx.vpn.ui.theme.DarkBackground
import com.fusionx.vpn.ui.theme.DarkCard
import com.fusionx.vpn.ui.theme.PurplePrimary
import com.fusionx.vpn.ui.theme.TextSecondary
import com.fusionx.vpn.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val enableSniffing by viewModel.prefs.enableSniffing.collectAsState(initial = true)
    val enableMux by viewModel.prefs.enableMux.collectAsState(initial = false)
    val bypassLan by viewModel.prefs.bypassLan.collectAsState(initial = true)
    val enableSpeedDisplay by viewModel.prefs.enableSpeedDisplay.collectAsState(initial = true)
    val autoConnect by viewModel.prefs.autoConnect.collectAsState(initial = false)
    val wallpaperFade by viewModel.prefs.wallpaperFade.collectAsState(initial = true)
    val dnsMode by viewModel.prefs.dnsMode.collectAsState(initial = "system")
    val routingMode by viewModel.prefs.routingMode.collectAsState(initial = "global")

    val wallpaperPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.setWallpaper(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = CyanPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = CyanPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Appearance section
            SettingsSection("Appearance") {
                SettingsItem(
                    icon = Icons.Default.Image,
                    title = "Background Wallpaper",
                    subtitle = "Choose a custom background image",
                    onClick = { wallpaperPicker.launch("image/*") }
                )
                SettingsItem(
                    icon = Icons.Default.Image,
                    title = "Reset Wallpaper",
                    subtitle = "Remove custom background",
                    onClick = { viewModel.setWallpaper(null) }
                )
                SettingsToggle(
                    icon = Icons.Default.Image,
                    title = "Wallpaper Fade",
                    subtitle = if (wallpaperFade) "Faded background (subtle)" else "Normal background (full brightness)",
                    checked = wallpaperFade,
                    onCheckedChange = {
                        scope.launch { viewModel.prefs.set(PrefsManager.KEY_WALLPAPER_FADE, it) }
                    }
                )
            }

            // Network section
            SettingsSection("Network") {
                SettingsToggle(
                    icon = Icons.Default.Security,
                    title = "Traffic Sniffing",
                    subtitle = "Detect and route traffic by domain",
                    checked = enableSniffing,
                    onCheckedChange = {
                        scope.launch { viewModel.prefs.set(PrefsManager.KEY_ENABLE_SNIFFING, it) }
                    }
                )
                SettingsToggle(
                    icon = Icons.Default.Speed,
                    title = "Mux (Multiplexing)",
                    subtitle = "Multiplex connections to reduce latency",
                    checked = enableMux,
                    onCheckedChange = {
                        scope.launch { viewModel.prefs.set(PrefsManager.KEY_ENABLE_MUX, it) }
                    }
                )
                SettingsToggle(
                    icon = Icons.Default.Router,
                    title = "Bypass LAN",
                    subtitle = "Exclude local network from VPN",
                    checked = bypassLan,
                    onCheckedChange = {
                        scope.launch { viewModel.prefs.set(PrefsManager.KEY_BYPASS_LAN, it) }
                    }
                )
            }

            // DNS section
            SettingsSection("DNS") {
                SettingsItem(
                    icon = Icons.Default.Dns,
                    title = "DNS Mode",
                    subtitle = dnsMode.replaceFirstChar { it.uppercase() },
                    onClick = {
                        val next = when (dnsMode) {
                            "system" -> "cloudflare"
                            "cloudflare" -> "google"
                            "google" -> "custom"
                            else -> "system"
                        }
                        scope.launch { viewModel.prefs.set(PrefsManager.KEY_DNS_MODE, next) }
                    }
                )
            }

            // Routing section
            SettingsSection("Routing") {
                SettingsItem(
                    icon = Icons.Default.NetworkCheck,
                    title = "Routing Mode",
                    subtitle = routingMode.replaceFirstChar { it.uppercase() },
                    onClick = {
                        val next = when (routingMode) {
                            "global" -> "bypass_mainland"
                            "bypass_mainland" -> "bypass_lan"
                            else -> "global"
                        }
                        scope.launch { viewModel.prefs.set(PrefsManager.KEY_ROUTING_MODE, next) }
                    }
                )
            }

            // General section
            SettingsSection("General") {
                SettingsToggle(
                    icon = Icons.Default.Tune,
                    title = "Show Speed",
                    subtitle = "Display upload/download speed",
                    checked = enableSpeedDisplay,
                    onCheckedChange = {
                        scope.launch { viewModel.prefs.set(PrefsManager.KEY_ENABLE_SPEED_DISPLAY, it) }
                    }
                )
                SettingsToggle(
                    icon = Icons.Default.Tune,
                    title = "Auto Connect",
                    subtitle = "Reconnect VPN on device boot",
                    checked = autoConnect,
                    onCheckedChange = {
                        scope.launch { viewModel.prefs.set(PrefsManager.KEY_AUTO_CONNECT, it) }
                    }
                )
            }

            // About section
            SettingsSection("About") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "FusionX VPN",
                    subtitle = "Version 1.0.0 • MANVPN × FREEFLOW",
                    onClick = {}
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = PurplePrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = CyanPrimary, modifier = Modifier.padding(end = 16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}

@Composable
private fun SettingsToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = CyanPrimary, modifier = Modifier.padding(end = 16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CyanPrimary,
                checkedTrackColor = CyanPrimary.copy(alpha = 0.3f)
            )
        )
    }
}
