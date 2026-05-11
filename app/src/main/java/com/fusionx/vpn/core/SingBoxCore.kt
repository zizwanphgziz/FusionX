package com.fusionx.vpn.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SingBoxCore(private val context: Context) {

    companion object {
        private const val TAG = "SingBoxCore"
        private var singboxLoaded = false

        @Synchronized
        fun loadSingBox(): Boolean {
            if (singboxLoaded) return true
            return try {
                System.loadLibrary("singboxjni")
                singboxLoaded = true
                Log.i(TAG, "Loaded libsingboxjni.so")
                true
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "Failed to load libsingboxjni.so: ${e.message}")
                false
            }
        }
    }

    private var boxInstance: libcore.BoxInstance? = null

    fun initialize(): Boolean {
        try {
            if (!loadSingBox()) {
                Log.e(TAG, "Failed to load sing-box native library")
                return false
            }
            libcore.Libcore.ensureInitialized()
            val basePath = context.filesDir.absolutePath + "/sing-box"
            val tempPath = context.cacheDir.absolutePath + "/sing-box"
            java.io.File(basePath).mkdirs()
            java.io.File(tempPath).mkdirs()
            copyAssetsIfNeeded(context, basePath)
            libcore.Libcore.initCore(basePath, tempPath, 0, "")
            Log.i(TAG, "sing-box core initialized")
            return true
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to initialize sing-box core", t)
            return false
        }
    }

    fun start(configJson: String) {
        try {
            boxInstance = libcore.Libcore.newSingBoxInstance(configJson)
            boxInstance?.preStart()
            boxInstance?.start()
            boxInstance?.setAsMain()
            Log.i(TAG, "sing-box core started")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to start sing-box", t)
            throw RuntimeException("sing-box start failed", t)
        }
    }

    fun stop() {
        try {
            boxInstance?.close()
            boxInstance = null
            Log.i(TAG, "sing-box core stopped")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to stop sing-box", t)
        }
    }

    fun isRunning(): Boolean = boxInstance != null

    fun queryStats(tag: String, direction: String): Long {
        return try {
            boxInstance?.queryStats(tag, direction) ?: 0
        } catch (t: Throwable) {
            0
        }
    }

    suspend fun selectOutbound(tag: String) {
        withContext(Dispatchers.IO) {
            try {
                boxInstance?.selectOutbound(tag)
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to select outbound: $tag", t)
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
