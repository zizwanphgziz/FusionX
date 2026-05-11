package com.fusionx.vpn

import android.app.Application
import android.util.Log

class FusionXApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.i("FusionX", "FusionX VPN Application started")

        // Initialize Xray core environment
        try {
            go.Seq.loadXray()
            go.Seq.init(this)
        } catch (e: Exception) {
            Log.w("FusionX", "Deferred core init: ${e.message}")
        }
    }
}
