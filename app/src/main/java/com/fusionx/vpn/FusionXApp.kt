package com.fusionx.vpn

import android.app.Application
import android.util.Log

class FusionXApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.i("FusionX", "FusionX VPN Application started")
        // Native cores are initialized lazily when VPN connects
    }
}
