package com.fusionx.vpn

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fusionx.vpn.service.FusionVpnService
import com.fusionx.vpn.ui.screens.AddServerScreen
import com.fusionx.vpn.ui.screens.HomeScreen
import com.fusionx.vpn.ui.screens.ServerListScreen
import com.fusionx.vpn.ui.screens.SettingsScreen
import com.fusionx.vpn.ui.theme.FusionXTheme
import com.fusionx.vpn.viewmodel.ConnectionState
import com.fusionx.vpn.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private var pendingProfileId: Long = -1

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpnService(pendingProfileId)
        } else {
            Toast.makeText(this, "VPN permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FusionXTheme {
                FusionXNavigation(
                    onConnectToggle = { viewModel ->
                        handleConnectToggle(viewModel)
                    }
                )
            }
        }

        // Handle deep links
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data?.toString() ?: return
        val schemes = listOf("vless://", "vmess://", "trojan://", "ss://", "ssr://", "hy2://", "hysteria2://", "tuic://", "wireguard://", "wg://")
        if (schemes.any { data.startsWith(it) }) {
            // Will be handled by ViewModel after navigation
            Toast.makeText(this, "Config link received", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleConnectToggle(viewModel: MainViewModel) {
        if (FusionVpnService.isRunning) {
            // Disconnect
            viewModel.setConnectionState(ConnectionState.DISCONNECTING)
            val intent = Intent(this, FusionVpnService::class.java).apply {
                action = FusionVpnService.ACTION_STOP
            }
            startService(intent)
            viewModel.setConnectionState(ConnectionState.DISCONNECTED)
        } else {
            // Connect
            val profile = viewModel.selectedProfile.value
            if (profile == null) {
                Toast.makeText(this, "Please select a server first", Toast.LENGTH_SHORT).show()
                return
            }

            pendingProfileId = profile.id
            viewModel.setConnectionState(ConnectionState.CONNECTING)

            val vpnIntent = VpnService.prepare(this)
            if (vpnIntent != null) {
                vpnPermissionLauncher.launch(vpnIntent)
            } else {
                startVpnService(profile.id)
            }
        }
    }

    private fun startVpnService(profileId: Long) {
        val intent = Intent(this, FusionVpnService::class.java).apply {
            action = FusionVpnService.ACTION_START
            putExtra(FusionVpnService.EXTRA_PROFILE_ID, profileId)
        }
        startForegroundService(intent)
    }
}

@Composable
fun FusionXNavigation(onConnectToggle: (MainViewModel) -> Unit) {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = Modifier.fillMaxSize()
    ) {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToServers = { navController.navigate("servers") },
                onNavigateToSettings = { navController.navigate("settings") },
                onConnectToggle = { onConnectToggle(viewModel) }
            )
        }
        composable("servers") {
            ServerListScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onAddServer = { navController.navigate("add_server") }
            )
        }
        composable("add_server") {
            AddServerScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
