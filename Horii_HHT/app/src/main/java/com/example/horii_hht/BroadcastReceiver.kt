package com.example.horii_hht

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// 端末起動を検知するためのBroadcastReceiver
class BootBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
       Intent(context, Start_Day::class.java).apply {
           this.flags = Intent.FLAG_ACTIVITY_NEW_TASK
           context.startActivity(this)
       }
    }
}
