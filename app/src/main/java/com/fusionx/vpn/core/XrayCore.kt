package com.fusionx.vpn.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray

class XrayCore(private val context: Context) {

    companion object {
        private const val TAG = "XrayCore"
    }

    private var controller: CoreController? = null
    private var statusCallback: ((Int, String) -> Unit)? = null

    fun initialize() {
        try {
            val assetsPath = context.filesDir.absolutePath
            copyAssetsIfNeeded(context, assetsPath)
            Libv2ray.initCoreEnv(assetsPath)
            Log.i(TAG, "Xray core initialized. Version: ${getVersion()}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Xray core", e)
        }
    }

    fun getVersion(): String {
        return try {
            Libv2ray.checkVersionX()
        } catch (e: Exception) {
            "unknown"
        }
    }

    fun start(configJson: String, onStatus: ((Int, String) -> Unit)? = null) {
        statusCallback = onStatus

        val handler = object : CoreCallbackHandler {
            override fun onEmitStatus(code: Long, msg: String) {
                Log.d(TAG, "Status: $code - $msg")
                statusCallback?.invoke(code.toInt(), msg)
            }

            override fun shutdown() {
                Log.d(TAG, "Core shutdown requested")
            }

            override fun startup(): Long {
                Log.d(TAG, "Core startup")
                return 0
            }

            override fun incRefnum(): Int = 0
        }

        try {
            controller = Libv2ray.newCoreController(handler)
            controller?.startLoop(configJson)
            Log.i(TAG, "Xray core started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Xray core", e)
            throw e
        }
    }

    fun stop() {
        try {
            controller?.stopLoop()
            controller = null
            Log.i(TAG, "Xray core stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop Xray core", e)
        }
    }

    fun isRunning(): Boolean {
        return try {
            controller?.isRunning ?: false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun measureDelay(url: String = "https://www.google.com/generate_204"): Long {
        return withContext(Dispatchers.IO) {
            try {
                controller?.measureDelay(url) ?: -1
            } catch (e: Exception) {
                -1
            }
        }
    }

    fun queryStats(tag: String, direction: String): Long {
        return try {
            controller?.queryStats(tag, direction) ?: 0
        } catch (e: Exception) {
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
