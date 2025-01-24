package com.example.horii_hht.syukka

import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.cipherlab.barcode.ReaderManager
import com.example.horii_hht.DB.AppDatabase
import com.example.horii_hht.DB.SyukkaDAO
import com.example.horii_hht.R
import com.example.horii_hht.setting.ScreenStateReceiver

class Syukka_B03 : AppCompatActivity(){
    private lateinit var filter : IntentFilter
    private var readerManager: ReaderManager? = null
    private  lateinit var db: AppDatabase
    lateinit var dao: SyukkaDAO
    private lateinit var screenReceiver: ScreenStateReceiver
    private val handler = Handler(Looper.getMainLooper()) // ハンドラ
    private val checkInterval: Long = 10000 // 10秒ごとにチェック

    private val checkRunnable = object : Runnable { // チェック用のRunnable
        override fun run() {
            resetDatabaseAndShowDialog() // データベースをリセットしてダイアログ表示
            handler.postDelayed(this, checkInterval) // 10秒後に再度実行
        }
    }

    private var source: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.syukka_b03)


    }
}