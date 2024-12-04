package com.example.horii_hht.setting

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import com.example.horii_hht.nyuka.Nyuka01_QRread
import com.example.horii_hht.nyuka.Nyuka02_KenpinStart
import com.example.horii_hht.nyuka.Nyuka03_Barread
import com.example.horii_hht.nyuka.Nyuka04_Num
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

//スクリーンのオンオフ状態を検知するレシーバーを作成する

class ScreenStateReceiver : BroadcastReceiver() {
    private var screenOffTime: Long = 0

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_ON -> {
                val currentTime = System.currentTimeMillis()
                val elapsedTime = currentTime - screenOffTime

                GlobalScope.launch(Dispatchers.IO) {
                    val dao = (context as? Nyuka04_Num)?.dao
                    val maxTimes = dao?.getMaxTimes()
                    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
                    val maxKenpinDate = maxTimes?.maxKenpinTime?.takeIf { it.isNotEmpty() }
                        ?.let { dateFormat.parse(it) }
                    val maxQRDate = maxTimes?.maxQRTime?.takeIf { it.isNotEmpty() }
                        ?.let { dateFormat.parse(it) }
                    val maxDate = when {
                        maxKenpinDate != null && maxQRDate != null -> maxOf(
                            maxKenpinDate,
                            maxQRDate
                        )

                        maxKenpinDate != null -> maxKenpinDate
                        maxQRDate != null -> maxQRDate
                        else -> null
                    }

                    if (maxDate != null) {
                        val sharedPreferences: SharedPreferences =
                            PreferenceManager.getDefaultSharedPreferences(context)
                        val lockTimeMinutes =
                            sharedPreferences.getString("lock_time", "5")?.toLongOrNull() ?: 5
                        val lockTimeMillis = lockTimeMinutes * 60 * 1000

                        if (currentTime - maxDate.time < lockTimeMillis) {
                            launch(Dispatchers.Main) {
                                when (context) {
                                    is Nyuka01_QRread -> context.showWorkingDialog()
                                    is Nyuka02_KenpinStart -> context.showWorkingDialog()
                                    is Nyuka03_Barread -> context.showWorkingDialog()
                                    is Nyuka04_Num -> context.showWorkingDialog()
                                }
                            }
                        }
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