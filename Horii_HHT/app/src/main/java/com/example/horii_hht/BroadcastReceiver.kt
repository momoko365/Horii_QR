package com.example.horii_hht

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// 端末起動を検知するためのBroadcastReceiver
class BootBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Start_Dayアクティビティを起動するためのIntentを作成
       Intent(context, Start_Day::class.java).apply {
           // アクティビティを起動するフラグを設定
           this.flags = Intent.FLAG_ACTIVITY_NEW_TASK
           // 作成したIntentを使用してアクティビティを起動
           context.startActivity(this)
       }
    }
}
