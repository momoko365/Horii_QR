package com.example.horii_hht

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
//スクリーンのオンオフ状態を検知するレシーバーを作成する
class ScreenStateReceiver : BroadcastReceiver() {
    private var screenOffTime: Long = 0

    override fun onReceive(context: Context, intent: Intent) {
        val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        //SharedPreferencesから「lock_time」というキーで保存されている文字列を取得。もし値が存在しない場合はデフォルト値として「120」を返す
        val lockTime = sharedPreferences.getString("lock_time", "120")?.toLongOrNull() ?: 5
        val lockTimeMillis = lockTime * 60 * 1000

        when (intent.action) {
            Intent.ACTION_SCREEN_ON -> {
                val currentTime = System.currentTimeMillis()
                val elapsedTime = currentTime - screenOffTime
                if (elapsedTime >= lockTimeMillis) {
                    when (context) {
                        is Nyuka01_QRread -> context.resetDatabaseAndShowDialog()
                        is Nyuka02_KenpinStart -> context.resetDatabaseAndShowDialog()
                        is Nyuka03_Barread -> context.resetDatabaseAndShowDialog()
                        is Nyuka04_Num -> context.resetDatabaseAndShowDialog()
                    }
                } else {
                    when (context) {
                        is Nyuka01_QRread -> context.showWorkingDialog()
                        is Nyuka02_KenpinStart -> context.showWorkingDialog()
                        is Nyuka03_Barread -> context.showWorkingDialog()
                        is Nyuka04_Num -> context.showWorkingDialog()
                    }
                }
            }

            Intent.ACTION_SCREEN_OFF -> {
                screenOffTime = System.currentTimeMillis()
                Log.d("ScreenStateReceiver", "Screen OFF detected")
            }
        }
    }
}
