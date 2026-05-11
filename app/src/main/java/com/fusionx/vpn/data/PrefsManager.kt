package com.fusionx.vpn.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fusionx_prefs")

class PrefsManager(private val context: Context) {

    companion object {
        val KEY_SELECTED_PROFILE = longPreferencesKey("selected_profile_id")
        val KEY_WALLPAPER_URI = stringPreferencesKey("wallpaper_uri")
        val KEY_DNS_MODE = stringPreferencesKey("dns_mode")
        val KEY_CUSTOM_DNS = stringPreferencesKey("custom_dns")
        val KEY_ROUTING_MODE = stringPreferencesKey("routing_mode")
        val KEY_ENABLE_SNIFFING = booleanPreferencesKey("enable_sniffing")
        val KEY_ENABLE_MUX = booleanPreferencesKey("enable_mux")
        val KEY_MUX_CONCURRENCY = intPreferencesKey("mux_concurrency")
        val KEY_ENABLE_SPEED_DISPLAY = booleanPreferencesKey("enable_speed_display")
        val KEY_BYPASS_LAN = booleanPreferencesKey("bypass_lan")
        val KEY_PER_APP_PROXY = booleanPreferencesKey("per_app_proxy")
        val KEY_ALLOWED_APPS = stringPreferencesKey("allowed_apps")
        val KEY_SOCKS_PORT = intPreferencesKey("socks_port")
        val KEY_HTTP_PORT = intPreferencesKey("http_port")
        val KEY_AUTO_CONNECT = booleanPreferencesKey("auto_connect")
        val KEY_WALLPAPER_FADE = booleanPreferencesKey("wallpaper_fade")
    }

    val wallpaperUri: Flow<String> = context.dataStore.data.map { it[KEY_WALLPAPER_URI] ?: "" }
    val wallpaperFade: Flow<Boolean> = context.dataStore.data.map { it[KEY_WALLPAPER_FADE] ?: true }
    val selectedProfileId: Flow<Long> = context.dataStore.data.map { it[KEY_SELECTED_PROFILE] ?: -1 }
    val dnsMode: Flow<String> = context.dataStore.data.map { it[KEY_DNS_MODE] ?: "system" }
    val customDns: Flow<String> = context.dataStore.data.map { it[KEY_CUSTOM_DNS] ?: "1.1.1.1" }
    val routingMode: Flow<String> = context.dataStore.data.map { it[KEY_ROUTING_MODE] ?: "global" }
    val enableSniffing: Flow<Boolean> = context.dataStore.data.map { it[KEY_ENABLE_SNIFFING] ?: true }
    val enableMux: Flow<Boolean> = context.dataStore.data.map { it[KEY_ENABLE_MUX] ?: false }
    val muxConcurrency: Flow<Int> = context.dataStore.data.map { it[KEY_MUX_CONCURRENCY] ?: 8 }
    val enableSpeedDisplay: Flow<Boolean> = context.dataStore.data.map { it[KEY_ENABLE_SPEED_DISPLAY] ?: true }
    val bypassLan: Flow<Boolean> = context.dataStore.data.map { it[KEY_BYPASS_LAN] ?: true }
    val socksPort: Flow<Int> = context.dataStore.data.map { it[KEY_SOCKS_PORT] ?: 10808 }
    val httpPort: Flow<Int> = context.dataStore.data.map { it[KEY_HTTP_PORT] ?: 10809 }
    val autoConnect: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_CONNECT] ?: false }

    suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { it[key] = value }
    }
}
