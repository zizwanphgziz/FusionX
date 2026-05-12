package com.fusionx.vpn.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fusionx.vpn.core.ConfigDetector
import com.fusionx.vpn.data.AppDatabase
import com.fusionx.vpn.data.PrefsManager
import com.fusionx.vpn.model.ServerProfile
import com.fusionx.vpn.service.FusionVpnService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db.serverProfileDao()
    val prefs = PrefsManager(application)

    // All profiles
    val profiles: StateFlow<List<ServerProfile>> = dao.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Groups
    val groups: StateFlow<List<String>> = dao.getAllGroups()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Selected profile
    private val _selectedProfile = MutableStateFlow<ServerProfile?>(null)
    val selectedProfile: StateFlow<ServerProfile?> = _selectedProfile.asStateFlow()

    // Connection state
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Traffic stats
    private val _uploadSpeed = MutableStateFlow(0L)
    val uploadSpeed: StateFlow<Long> = _uploadSpeed.asStateFlow()
    private val _downloadSpeed = MutableStateFlow(0L)
    val downloadSpeed: StateFlow<Long> = _downloadSpeed.asStateFlow()
    private val _totalUpload = MutableStateFlow(0L)
    val totalUpload: StateFlow<Long> = _totalUpload.asStateFlow()
    private val _totalDownload = MutableStateFlow(0L)
    val totalDownload: StateFlow<Long> = _totalDownload.asStateFlow()

    // Wallpaper
    val wallpaperUri: StateFlow<String> = prefs.wallpaperUri
        .stateIn(viewModelScope, SharingStarted.Lazily, "")
    val wallpaperFade: StateFlow<Boolean> = prefs.wallpaperFade
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    // Import status
    private val _importResult = MutableStateFlow<ImportResult?>(null)
    val importResult: StateFlow<ImportResult?> = _importResult.asStateFlow()

    // Filter
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGroup = MutableStateFlow("All")
    val selectedGroup: StateFlow<String> = _selectedGroup.asStateFlow()

    val filteredProfiles: StateFlow<List<ServerProfile>> = combine(
        profiles, _searchQuery, _selectedGroup
    ) { allProfiles, query, group ->
        allProfiles.filter { profile ->
            val matchesQuery = query.isEmpty() ||
                    profile.displayName.contains(query, ignoreCase = true) ||
                    profile.address.contains(query, ignoreCase = true) ||
                    profile.protocol.contains(query, ignoreCase = true)
            val matchesGroup = group == "All" || profile.group == group
            matchesQuery && matchesGroup
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            prefs.selectedProfileId.collect { id ->
                if (id >= 0) {
                    _selectedProfile.value = dao.getProfileById(id)
                }
            }
        }
        startStatsPolling()
    }

    fun selectProfile(profile: ServerProfile) {
        viewModelScope.launch {
            dao.deselectAll()
            dao.selectProfile(profile.id)
            prefs.set(PrefsManager.KEY_SELECTED_PROFILE, profile.id)
            _selectedProfile.value = profile
        }
    }

    fun importConfig(input: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val profiles = ConfigDetector.detectAndParse(input)
                if (profiles.isNotEmpty()) {
                    dao.insertAll(profiles)
                    _importResult.value = ImportResult.Success(profiles.size)
                } else {
                    _importResult.value = ImportResult.Error("No valid configs found")
                }
            } catch (e: Exception) {
                _importResult.value = ImportResult.Error(e.message ?: "Import failed")
            }
        }
    }

    fun importSubscription(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val content = fetchUrl(url)
                val profiles = ConfigDetector.detectAndParse(content)
                    .map { it.copy(subscriptionUrl = url) }
                if (profiles.isNotEmpty()) {
                    dao.deleteBySubscription(url)
                    dao.insertAll(profiles)
                    _importResult.value = ImportResult.Success(profiles.size)
                } else {
                    _importResult.value = ImportResult.Error("No valid configs in subscription")
                }
            } catch (e: Exception) {
                _importResult.value = ImportResult.Error("Subscription fetch failed: ${e.message}")
            }
        }
    }

    fun deleteProfile(profile: ServerProfile) {
        viewModelScope.launch {
            dao.delete(profile)
            if (_selectedProfile.value?.id == profile.id) {
                _selectedProfile.value = null
            }
        }
    }

    fun addOrUpdateProfile(profile: ServerProfile) {
        viewModelScope.launch {
            dao.insert(profile)
        }
    }

    fun testLatency(profile: ServerProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            val latency = measureLatency(profile.address, profile.port)
            dao.updateLatency(profile.id, latency)
        }
    }

    fun testAllLatencies() {
        viewModelScope.launch(Dispatchers.IO) {
            profiles.value.forEach { profile ->
                val latency = measureLatency(profile.address, profile.port)
                dao.updateLatency(profile.id, latency)
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedGroup(group: String) {
        _selectedGroup.value = group
    }

    fun setWallpaper(uri: Uri?) {
        viewModelScope.launch {
            prefs.set(PrefsManager.KEY_WALLPAPER_URI, uri?.toString() ?: "")
        }
    }

    fun clearImportResult() {
        _importResult.value = null
    }

    fun setConnectionState(state: ConnectionState) {
        _connectionState.value = state
    }

    private fun startStatsPolling() {
        viewModelScope.launch {
            var prevUp = 0L
            var prevDown = 0L
            while (isActive) {
                delay(1000)
                if (FusionVpnService.isRunning) {
                    val curUp = FusionVpnService.uploadBytes
                    val curDown = FusionVpnService.downloadBytes
                    _uploadSpeed.value = curUp - prevUp
                    _downloadSpeed.value = curDown - prevDown
                    _totalUpload.value = curUp
                    _totalDownload.value = curDown
                    prevUp = curUp
                    prevDown = curDown
                } else {
                    _uploadSpeed.value = 0
                    _downloadSpeed.value = 0
                }
            }
        }
    }

    private fun measureLatency(host: String, port: Int): Long {
        return try {
            val start = System.currentTimeMillis()
            val socket = java.net.Socket()
            socket.connect(java.net.InetSocketAddress(host, port), 5000)
            val elapsed = System.currentTimeMillis() - start
            socket.close()
            elapsed
        } catch (e: Exception) {
            -1
        }
    }

    private fun fetchUrl(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        conn.setRequestProperty("User-Agent", "FusionX/1.0")
        return conn.inputStream.bufferedReader().readText()
    }
}

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING
}

sealed class ImportResult {
    data class Success(val count: Int) : ImportResult()
    data class Error(val message: String) : ImportResult()
}
