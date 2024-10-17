package com.example.horii_hht

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cipherlab.barcode.GeneralString
import com.cipherlab.barcode.ReaderManager
import com.cipherlab.barcodebase.ReaderCallback

class Nyuka01_QRread : AppCompatActivity() {
    //インテントの取得
    private lateinit var filter: IntentFilter
    private var readerManager: ReaderManager? = null
//ドライバおよびサービスと通信するために使用するクラス
    private var mReaderManager: ReaderManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.nyuka01)

        // ReaderManagerの初期化
//        readerManager = ReaderManager.InitInstance(this)


        val scanBtn = findViewById<android.widget.Button>(R.id.button)

        // UIでボタンを見せない
        // ボタンを透明にする
        scanBtn.alpha = 0f
        scanBtn.setOnClickListener {
            readerManager = ReaderManager.InitInstance(this)
            // スキャン結果を受け取るためのIntentフィルタ設定
          filter = IntentFilter().apply {
                addAction(com.cipherlab.barcode.GeneralString.Intent_SOFTTRIGGER_DATA)
                addAction(com.cipherlab.barcode.GeneralString.Intent_PASS_TO_APP)
                addAction(com.cipherlab.barcode.GeneralString.Intent_READERSERVICE_CONNECTED)
                registerReceiver(scanDataReceiver, filter)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // BroadcastReceiverの解除
        unregisterReceiver(scanDataReceiver)
            mReaderManager?.Release()
    }


    // スキャン結果を受け取るBroadcastReceiver
    private val scanDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == GeneralString.Intent_SOFTTRIGGER_DATA) {
                // スキャンされたデータを取得
                val scannedData = intent.getStringExtra(GeneralString.BcReaderData)
                Log.d("Nyuka01_QRread", "Scanned Data: $scannedData")
                if (scannedData != null) {
                    Toast.makeText(this@Nyuka01_QRread, "成功: $scannedData", Toast.LENGTH_SHORT).show()
                    // データを次のアクティビティに渡して画面遷移
                    val nextIntent = Intent(this@Nyuka01_QRread, Nyuka02_KenpinStart::class.java)
                    nextIntent.putExtra("scannedData", scannedData)
                    startActivity(nextIntent)
                } else {
                    Toast.makeText(this@Nyuka01_QRread, "スキャンデータが取得できませんでした", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this@Nyuka01_QRread, "意図しないアクション: ${intent.action}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_F4 -> {
                // F4キーが押されたときの処理
                val intent = Intent(this, Main_Menu::class.java)
                startActivity(intent)
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}