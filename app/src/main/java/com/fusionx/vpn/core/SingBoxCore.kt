package com.fusionx.vpn.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import libcore.BoxInstance
import libcore.Libcore

class SingBoxCore(private val context: Context) {

    companion object {
        private const val TAG = "SingBoxCore"
    }

    private var boxInstance: BoxInstance? = null

    fun initialize() {
        try {
            Libcore.ensureInitialized()
            val basePath = context.filesDir.absolutePath + "/sing-box"
            val tempPath = context.cacheDir.absolutePath + "/sing-box"
            java.io.File(basePath).mkdirs()
            java.io.File(tempPath).mkdirs()
            copyAssetsIfNeeded(context, basePath)
            Libcore.initCore(basePath, tempPath, 0, "")
            Log.i(TAG, "sing-box core initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize sing-box core", e)
        }
    }

    fun start(configJson: String) {
        try {
            boxInstance = Libcore.newSingBoxInstance(configJson)
            boxInstance?.preStart()
            boxInstance?.start()
            boxInstance?.setAsMain()
            Log.i(TAG, "sing-box core started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start sing-box", e)
            throw e
        }
    }

    fun stop() {
        try {
            boxInstance?.close()
            boxInstance = null
            Log.i(TAG, "sing-box core stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop sing-box", e)
        }
    }

    fun isRunning(): Boolean = boxInstance != null

    fun queryStats(tag: String, direction: String): Long {
        return try {
            boxInstance?.queryStats(tag, direction) ?: 0
        } catch (e: Exception) {
            0
        }
    }

    suspend fun selectOutbound(tag: String) {
        withContext(Dispatchers.IO) {
            try {
                boxInstance?.selectOutbound(tag)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to select outbound: $tag", e)
            }
        }
    }

    private fun copyAssetsIfNeeded(context: Context, targetDir: String) {
        val assets = listOf(
            "sing-box/geoip.db.xz" to "geoip.db.xz",
            "sing-box/geosite.db.xz" to "geosite.db.xz"
        )
        for ((src, dst) in assets) {
            val target = java.io.File(targetDir, dst)
            if (!target.exists()) {
                try {
                    context.assets.open(src).use { input ->
                        target.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to copy asset: $src", e)
                }
            }
        }
    }
}
