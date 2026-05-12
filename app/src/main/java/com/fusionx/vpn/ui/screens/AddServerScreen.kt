package com.fusionx.vpn.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fusionx.vpn.ui.theme.CyanPrimary
import com.fusionx.vpn.ui.theme.DarkBackground
import com.fusionx.vpn.ui.theme.DarkCard
import com.fusionx.vpn.ui.theme.GreenConnected
import com.fusionx.vpn.ui.theme.TextSecondary
import com.fusionx.vpn.viewmodel.ImportResult
import com.fusionx.vpn.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServerScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Paste Config", "Subscription URL")
    var configInput by remember { mutableStateOf("") }
    var subscriptionUrl by remember { mutableStateOf("") }
    val importResult by viewModel.importResult.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(importResult) {
        when (val result = importResult) {
            is ImportResult.Success -> {
                snackbarHostState.showSnackbar("Imported ${result.count} server(s)")
                viewModel.clearImportResult()
                onNavigateBack()
            }
            is ImportResult.Error -> {
                snackbarHostState.showSnackbar("Error: ${result.message}")
                viewModel.clearImportResult()
            }
            null -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Server", color = CyanPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = CyanPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkCard,
                contentColor = CyanPrimary,
                indicator = { tabPositions ->
                    SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = CyanPrimary
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                color = if (selectedTab == index) CyanPrimary else TextSecondary
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> PasteConfigTab(
                    config = configInput,
                    onConfigChange = { configInput = it },
                    onImport = { viewModel.importConfig(configInput) }
                )
                1 -> SubscriptionTab(
                    url = subscriptionUrl,
                    onUrlChange = { subscriptionUrl = it },
                    onImport = { viewModel.importSubscription(subscriptionUrl) }
                )
            }
        }
    }
}

@Composable
private fun PasteConfigTab(
    config: String,
    onConfigChange: (String) -> Unit,
    onImport: () -> Unit
) {
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = CyanPrimary,
        unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        cursorColor = CyanPrimary,
        focusedLabelColor = CyanPrimary,
        unfocusedLabelColor = TextSecondary
    )

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "Paste your config links here",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            "Supports: vless://, vmess://, trojan://, ss://, ssr://, hy2://, tuic://, wg://",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary.copy(alpha = 0.7f)
        )
        Text(
            "Also supports base64-encoded configs and multi-line input",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = config,
            onValueChange = onConfigChange,
            label = { Text("Config / Share Link") },
            placeholder = { Text("vless://...\nvmess://...", color = TextSecondary.copy(alpha = 0.5f)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            colors = textFieldColors,
            shape = RoundedCornerShape(12.dp),
            maxLines = 20
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onImport,
            enabled = config.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Import", fontWeight = FontWeight.Bold, color = DarkBackground)
        }
    }
}

@Composable
private fun SubscriptionTab(
    url: String,
    onUrlChange: (String) -> Unit,
    onImport: () -> Unit
) {
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = CyanPrimary,
        unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        cursorColor = CyanPrimary,
        focusedLabelColor = CyanPrimary,
        unfocusedLabelColor = TextSecondary
    )

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "Enter your subscription URL",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            "The URL will be fetched and all configs imported automatically",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            label = { Text("Subscription URL") },
            placeholder = { Text("https://...", color = TextSecondary.copy(alpha = 0.5f)) },
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors,
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onImport,
            enabled = url.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Fetch & Import", fontWeight = FontWeight.Bold, color = DarkBackground)
        }
    }
}
