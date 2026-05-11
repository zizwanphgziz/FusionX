package com.fusionx.vpn.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class XrayCore(private val context: Context) {

    companion object {
        private const val TAG = "XrayCore"
    }

    private var controller: libv2ray.CoreController? = null
    private var statusCallback: ((Int, String) -> Unit)? = null

    fun initialize(): Boolean {
        try {
            if (!go.Seq.loadXray()) {
                Log.e(TAG, "Failed to load native library")
                return false
            }
            go.Seq.initIfLoaded(context)

            val assetsPath = context.filesDir.absolutePath
            val tempPath = context.cacheDir.absolutePath
            copyAssetsIfNeeded(context, assetsPath)
            libv2ray.Libv2ray.initCoreEnv(assetsPath, tempPath)
            Log.i(TAG, "Xray core initialized. Version: ${getVersion()}")
            return true
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to initialize Xray core", t)
            return false
        }
    }

    fun getVersion(): String {
        return try {
            libv2ray.Libv2ray.checkVersionX()
        } catch (t: Throwable) {
            "unknown"
        }
    }

    fun start(configJson: String, vpnFd: Int = 0, onStatus: ((Int, String) -> Unit)? = null) {
        statusCallback = onStatus

        val handler = object : libv2ray.CoreCallbackHandler {
            override fun onEmitStatus(code: Long, msg: String): Long {
                Log.d(TAG, "Status: $code - $msg")
                statusCallback?.invoke(code.toInt(), msg)
                return 0
            }

            override fun shutdown(): Long {
                Log.d(TAG, "Core shutdown requested")
                return 0
            }

            override fun startup(): Long {
                Log.d(TAG, "Core startup")
                return 0
            }
        }

        try {
            controller = libv2ray.Libv2ray.newCoreController(handler)
            controller?.startLoop(configJson, vpnFd)
            Log.i(TAG, "Xray core started")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to start Xray core", t)
            throw RuntimeException("Xray core start failed", t)
        }
    }

    fun stop() {
        try {
            controller?.stopLoop()
            controller = null
            Log.i(TAG, "Xray core stopped")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to stop Xray core", t)
        }
    }

    fun isRunning(): Boolean {
        return try {
            controller?.isRunning ?: false
        } catch (t: Throwable) {
            false
        }
    }

    suspend fun measureDelay(url: String = "https://www.google.com/generate_204"): Long {
        return withContext(Dispatchers.IO) {
            try {
                controller?.measureDelay(url) ?: -1
            } catch (t: Throwable) {
                -1
            }
        }
    }

    fun queryStats(tag: String, direction: String): Long {
        return try {
            controller?.queryStats(tag, direction) ?: 0
        } catch (t: Throwable) {
            0
        }
    }

    fun getUploadBytes(): Long = queryStats("proxy", "uplink")
    fun getDownloadBytes(): Long = queryStats("proxy", "downlink")

    private fun copyAssetsIfNeeded(context: Context, targetDir: String) {
        val assetsToCheck = listOf("geoip.dat", "geosite.dat")
        for (asset in assetsToCheck) {
            val target = java.io.File(targetDir, asset)
            if (!target.exists()) {
                try {
                    context.assets.open(asset).use { input ->
                        target.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to copy asset: $asset", e)
                }
            }
        }
    }
}
