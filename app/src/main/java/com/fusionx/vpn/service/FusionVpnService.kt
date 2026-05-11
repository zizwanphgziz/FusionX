package com.fusionx.vpn.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import com.fusionx.vpn.R
import com.fusionx.vpn.core.SingBoxConfigBuilder
import com.fusionx.vpn.core.SingBoxCore
import com.fusionx.vpn.core.XrayConfigBuilder
import com.fusionx.vpn.core.XrayCore
import com.fusionx.vpn.data.AppDatabase
import com.fusionx.vpn.model.CoreType
import com.fusionx.vpn.model.ServerProfile
import com.v2ray.ang.service.TProxyService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class FusionVpnService : VpnService() {

    companion object {
        const val TAG = "FusionVPN"
        const val CHANNEL_ID = "fusionx_vpn"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.fusionx.vpn.START"
        const val ACTION_STOP = "com.fusionx.vpn.STOP"
        const val EXTRA_PROFILE_ID = "profile_id"

        private const val SOCKS_PORT = 10808
        private const val HTTP_PORT = 10809

        var isRunning = false
            private set
        var currentProfile: ServerProfile? = null
            private set
        var uploadBytes: Long = 0
            private set
        var downloadBytes: Long = 0
            private set
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var xrayCore: XrayCore? = null
    private var singBoxCore: SingBoxCore? = null
    private var vpnInterface: ParcelFileDescriptor? = null
    private var statsJob: Job? = null
    private var activeCoreType: CoreType? = null
    private var tun2socksRunning = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopVpn()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, createNotification("Connecting..."))
                val profileId = intent.getLongExtra(EXTRA_PROFILE_ID, -1)
                if (profileId >= 0) {
                    serviceScope.launch { startVpn(profileId) }
                }
            }
        }
        return START_STICKY
    }

    private suspend fun startVpn(profileId: Long) {
        try {
            val db = AppDatabase.getInstance(applicationContext)
            val profile = db.serverProfileDao().getProfileById(profileId) ?: return

            currentProfile = profile
            activeCoreType = profile.coreType

            // Start the appropriate core FIRST (local SOCKS5 proxy)
            when (profile.coreType) {
                CoreType.XRAY -> startXrayCore(profile)
                CoreType.SING_BOX -> startSingBoxCore(profile)
            }

            // Establish VPN tunnel AFTER core is running
            val builder = Builder()
                .setSession("FusionX")
                .setMtu(1500)
                .addAddress("10.1.10.1", 30)
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")
                .addRoute("0.0.0.0", 0)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setMetered(false)
            }

            try {
                builder.addDisallowedApplication(packageName)
            } catch (e: Exception) {
                Log.w(TAG, "Could not exclude self from VPN", e)
            }

            vpnInterface = builder.establish()

            if (vpnInterface == null) {
                Log.e(TAG, "Failed to establish VPN interface")
                stopVpn()
                return
            }

            // Start tun2socks bridge (TUN fd -> local SOCKS5 proxy)
            startTun2Socks(vpnInterface!!.fd)

            isRunning = true
            uploadBytes = 0
            downloadBytes = 0

            startStatsPolling()
            updateNotification("Connected: ${profile.displayName}")
            Log.i(TAG, "VPN started with ${profile.coreType} core")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to start VPN", t)
            stopVpn()
        }
    }

    private fun startXrayCore(profile: ServerProfile) {
        val core = XrayCore(applicationContext)
        if (!core.initialize()) {
            throw RuntimeException("Xray core init failed")
        }
        val config = XrayConfigBuilder.buildConfig(profile, SOCKS_PORT, HTTP_PORT)
        core.start(config) { code, msg ->
            Log.d(TAG, "Xray status: $code - $msg")
        }
        xrayCore = core
    }

    private fun startSingBoxCore(profile: ServerProfile) {
        val core = SingBoxCore(applicationContext)
        if (!core.initialize()) {
            throw RuntimeException("sing-box core init failed")
        }
        val config = SingBoxConfigBuilder.buildConfig(profile, tunEnabled = false)
        core.start(config)
        singBoxCore = core
    }

    private fun startTun2Socks(tunFd: Int) {
        if (!TProxyService.isLoaded()) {
            Log.w(TAG, "tun2socks library not available, skipping")
            return
        }

        val configFile = File(applicationContext.filesDir, "tun2socks.yml")
        val configContent = """
tunnel:
  mtu: 1500

socks5:
  port: $SOCKS_PORT
  address: 127.0.0.1
  udp: 'udp'

misc:
  task-stack-size: 81920
  connect-timeout: 5000
  read-write-timeout: 60000
  log-file: /dev/null
  log-level: warn
  pid-file: /dev/null
  limit-nofile: 65535
""".trimIndent()
        FileOutputStream(configFile).use {
            it.write(configContent.toByteArray())
        }

        Thread({
            try {
                Log.i(TAG, "Starting tun2socks bridge fd=$tunFd -> socks5://127.0.0.1:$SOCKS_PORT")
                tun2socksRunning = true
                TProxyService.TProxyStartService(configFile.absolutePath, tunFd)
                Log.i(TAG, "tun2socks exited")
            } catch (t: Throwable) {
                Log.e(TAG, "tun2socks failed", t)
            } finally {
                tun2socksRunning = false
            }
        }, "tun2socks").apply {
            isDaemon = true
            start()
        }
    }

    private fun stopTun2Socks() {
        if (tun2socksRunning && TProxyService.isLoaded()) {
            try {
                TProxyService.TProxyStopService()
            } catch (t: Throwable) {
                Log.w(TAG, "Error stopping tun2socks", t)
            }
        }
        tun2socksRunning = false
    }

    private fun startStatsPolling() {
        statsJob = serviceScope.launch {
            while (isActive) {
                delay(1000)
                when (activeCoreType) {
                    CoreType.XRAY -> {
                        xrayCore?.let {
                            uploadBytes = it.getUploadBytes()
                            downloadBytes = it.getDownloadBytes()
                        }
                    }
                    CoreType.SING_BOX -> {
                        singBoxCore?.let {
                            uploadBytes = it.queryStats("proxy", "uplink")
                            downloadBytes = it.queryStats("proxy", "downlink")
                        }
                    }
                    null -> {}
                }
            }
        }
    }

    fun stopVpn() {
        statsJob?.cancel()
        statsJob = null

        stopTun2Socks()

        try {
            xrayCore?.stop()
            xrayCore = null
        } catch (t: Throwable) {
            Log.e(TAG, "Error stopping Xray core", t)
        }

        try {
            singBoxCore?.stop()
            singBoxCore = null
        } catch (t: Throwable) {
            Log.e(TAG, "Error stopping sing-box core", t)
        }

        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (t: Throwable) {
            Log.e(TAG, "Error closing VPN interface", t)
        }

        isRunning = false
        currentProfile = null
        activeCoreType = null
        uploadBytes = 0
        downloadBytes = 0

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.i(TAG, "VPN stopped")
    }

    override fun onDestroy() {
        stopVpn()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "FusionX VPN",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "FusionX VPN connection status"
            setShowBadge(false)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun createNotification(text: String): Notification {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("FusionX VPN")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_vpn_key)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, createNotification(text))
    }
}
