package com.example.horii_hht

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.io.File

class ShutdownReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SHUTDOWN) {
            // ファイルを削除
            val filesDir = context.filesDir
            val tempFile = File(filesDir, "example.txt")
            if (tempFile.exists()) {
                tempFile.delete()
                Log.d("ShutdownReceiver", "ファイルが削除されました: ${tempFile.absolutePath}")
            }
        }
    }
}