package com.fusionx.vpn.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fusionx.vpn.model.ServerProfile
import com.fusionx.vpn.ui.theme.CyanPrimary
import com.fusionx.vpn.ui.theme.DarkBackground
import com.fusionx.vpn.ui.theme.DarkCard
import com.fusionx.vpn.ui.theme.DarkCardElevated
import com.fusionx.vpn.ui.theme.GreenConnected
import com.fusionx.vpn.ui.theme.PurplePrimary
import com.fusionx.vpn.ui.theme.RedDisconnected
import com.fusionx.vpn.ui.theme.TextSecondary
import com.fusionx.vpn.ui.theme.YellowConnecting
import com.fusionx.vpn.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerListScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onAddServer: () -> Unit
) {
    val profiles by viewModel.filteredProfiles.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val selectedProfile by viewModel.selectedProfile.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    var showSearch by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search servers...", color = TextSecondary) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                cursorColor = CyanPrimary,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("Servers", color = CyanPrimary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = CyanPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        showSearch = !showSearch
                        if (!showSearch) viewModel.setSearchQuery("")
                    }) {
                        Icon(
                            if (showSearch) Icons.Default.Close else Icons.Default.Search,
                            "Search",
                            tint = CyanPrimary
                        )
                    }
                    IconButton(onClick = { viewModel.testAllLatencies() }) {
                        Icon(Icons.Default.Speed, "Test All", tint = CyanPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddServer,
                containerColor = CyanPrimary,
                contentColor = DarkBackground
            ) {
                Icon(Icons.Default.Add, "Add Server")
            }
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Group filter chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedGroup == "All",
                        onClick = { viewModel.setSelectedGroup("All") },
                        label = { Text("All") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = CyanPrimary
                        )
                    )
                }
                items(groups) { group ->
                    FilterChip(
                        selected = selectedGroup == group,
                        onClick = { viewModel.setSelectedGroup(group) },
                        label = { Text(group) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = CyanPrimary
                        )
                    )
                }
            }

            // Server list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(profiles, key = { it.id }) { profile ->
                    ServerCard(
                        profile = profile,
                        isSelected = selectedProfile?.id == profile.id,
                        onSelect = { viewModel.selectProfile(profile) },
                        onDelete = { viewModel.deleteProfile(profile) },
                        onTestLatency = { viewModel.testLatency(profile) }
                    )
                }

                if (profiles.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "No servers yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Tap + to add servers",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerCard(
    profile: ServerProfile,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onTestLatency: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) DarkCardElevated else DarkCard
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Protocol badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when (profile.protocol.lowercase()) {
                            "vless" -> CyanPrimary.copy(alpha = 0.2f)
                            "vmess" -> PurplePrimary.copy(alpha = 0.2f)
                            "trojan" -> RedDisconnected.copy(alpha = 0.2f)
                            "hy2", "hysteria2" -> GreenConnected.copy(alpha = 0.2f)
                            else -> TextSecondary.copy(alpha = 0.2f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = profile.protocol.uppercase().take(2),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = when (profile.protocol.lowercase()) {
                        "vless" -> CyanPrimary
                        "vmess" -> PurplePrimary
                        "trojan" -> RedDisconnected
                        "hy2", "hysteria2" -> GreenConnected
                        else -> TextSecondary
                    }
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSelected) CyanPrimary else Color.White,
                        maxLines = 1
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.CheckCircle,
                            "Selected",
                            tint = GreenConnected,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = "${profile.address}:${profile.port} • ${profile.network}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1
                )
                if (profile.isStrx) {
                    Text(
                        text = "STRX",
                        style = MaterialTheme.typography.labelSmall,
                        color = PurplePrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Latency badge
            if (profile.latency > 0) {
                Text(
                    text = "${profile.latency}ms",
                    color = when {
                        profile.latency < 100 -> GreenConnected
                        profile.latency < 300 -> YellowConnecting
                        else -> RedDisconnected
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            IconButton(onClick = onTestLatency, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Speed, "Test", tint = TextSecondary, modifier = Modifier.size(18.dp))
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, "Delete", tint = RedDisconnected.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
            }
        }
    }
}
