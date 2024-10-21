package com.example.horii_hht

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cipherlab.barcode.GeneralString
import com.cipherlab.barcode.ReaderManager

class Nyuka01_QRread : AppCompatActivity() {
    private lateinit var filter: IntentFilter
    private var readerManager: ReaderManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.nyuka01)

        // ReaderManagerの初期化
        readerManager = ReaderManager.InitInstance(this)

        // インテントフィルタの初期化
        filter = IntentFilter().apply {
            addAction(GeneralString.Intent_SOFTTRIGGER_DATA)
        }

        // BroadcastReceiverの登録
        registerReceiver(scanDataReceiver, filter)

        val scanBtn = findViewById<Button>(R.id.button)
        scanBtn.alpha = 0f
        scanBtn.setOnClickListener {
            readerManager?.SoftScanTrigger()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // BroadcastReceiverの解除
        unregisterReceiver(scanDataReceiver)
        readerManager?.Release()
    }

    // スキャン結果を受け取るBroadcastReceiver
    private val scanDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == GeneralString.Intent_SOFTTRIGGER_DATA) {
                val scannedData = intent.getStringExtra(GeneralString.BcReaderData)
                Log.d("Nyuka01_QRread", "Scanned Data: $scannedData")
                if (scannedData != null) {
                    Toast.makeText(this@Nyuka01_QRread, "成功: $scannedData", Toast.LENGTH_SHORT).show()
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
                val intent = Intent(this, Main_Menu::class.java)
                startActivity(intent)
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}