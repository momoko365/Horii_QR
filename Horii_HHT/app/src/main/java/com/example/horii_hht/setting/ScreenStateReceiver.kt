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

//スクリーンのオンオフ状態を検知するレシーバーを作成する
class ScreenStateReceiver : BroadcastReceiver() {
    //スクリーンがオフになった時刻を記録する変数
    private var screenOffTime: Long = 0

    override fun onReceive(context: Context, intent: Intent) {
        //SharedPreferencesから設定値を取得
        val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        //SharedPreferencesから「lock_time」というキーで保存されている文字列を取得。もし値が存在しない場合はデフォルト値として「120」を返す
        val lockTime = sharedPreferences.getString("lock_time", "120")?.toLongOrNull() ?: 5
        //lockTimeをミリ秒に変換
        val lockTimeMillis = lockTime * 60 * 1000

        //スクリーンのオンオフ状態に応じて処理を分岐
        when (intent.action) {
            //スクリーンがオンになった場合
            Intent.ACTION_SCREEN_ON -> {
                //現在時刻を取得
                val currentTime = System.currentTimeMillis()
                //スクリーンがオフになってからの経過時間を計算
                val elapsedTime = currentTime - screenOffTime
                //経過時間が設定値を超えている場合はデータベースをリセットしてダイアログを表示
                if (elapsedTime >= lockTimeMillis) {
                    when (context) {
                        is Nyuka01_QRread -> context.resetDatabaseAndShowDialog()
                        is Nyuka02_KenpinStart -> context.resetDatabaseAndShowDialog()
                        is Nyuka03_Barread -> context.resetDatabaseAndShowDialog()
                        is Nyuka04_Num -> context.resetDatabaseAndShowDialog()
                    }
                } else {
                    // 経過時間が設定値未満の場合作業中のダイアログを表示
                    when (context) {
                        is Nyuka01_QRread -> context.showWorkingDialog()
                        is Nyuka02_KenpinStart -> context.showWorkingDialog()
                        is Nyuka03_Barread -> context.showWorkingDialog()
                        is Nyuka04_Num -> context.showWorkingDialog()
                    }
                }
            }
            //スクリーンがオフになった場合
            Intent.ACTION_SCREEN_OFF -> {
                //スクリーンがオフになった時刻を記録
                screenOffTime = System.currentTimeMillis()
                Log.d("ScreenStateReceiver", "Screen OFF detected")
            }
        }
    }
}
