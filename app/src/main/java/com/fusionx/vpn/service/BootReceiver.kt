package com.fusionx.vpn.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.fusionx.vpn.data.PrefsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        CoroutineScope(Dispatchers.IO).launch {
            val prefs = PrefsManager(context)
            val autoConnect = prefs.autoConnect.first()
            val profileId = prefs.selectedProfileId.first()

            if (autoConnect && profileId >= 0) {
                Log.i("FusionX", "Auto-connecting on boot to profile $profileId")
                val serviceIntent = Intent(context, FusionVpnService::class.java).apply {
                    action = FusionVpnService.ACTION_START
                    putExtra(FusionVpnService.EXTRA_PROFILE_ID, profileId)
                }
                context.startForegroundService(serviceIntent)
            }
        }
    }
}
