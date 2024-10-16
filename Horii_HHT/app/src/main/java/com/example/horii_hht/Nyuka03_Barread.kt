package com.example.horii_hht

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.KeyEvent
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cipherlab.barcode.GeneralString
import com.cipherlab.barcode.ReaderManager

class Nyuka03_Barread: AppCompatActivity() {
    private lateinit var filter: IntentFilter
    private var readerManager: ReaderManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.nyuka03)
        // ReaderManagerの初期化
        readerManager = ReaderManager.InitInstance(this)
        val scanBtn = findViewById<android.widget.Button>(R.id.button2)
        val itemBar = findViewById<android.widget.TextView>(R.id.caseNum)


        // ボタンを透明にする
        scanBtn.alpha = 0f
        scanBtn.setOnClickListener {
            readerManager?.SoftScanTrigger() // ソフトスキャントリガー
        }
        // スキャン結果を受け取るためのIntentフィルタ設定
        filter = IntentFilter()
        filter.addAction(GeneralString.Intent_SOFTTRIGGER_DATA)
        registerReceiver(scanDataReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        // BroadcastReceiverの解除
        unregisterReceiver(scanDataReceiver)
    }

    // スキャン結果を受け取るBroadcastReceiver
    private val scanDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == GeneralString.Intent_SOFTTRIGGER_DATA) {
                // スキャンされたデータを取得
                val scannedData = intent.getStringExtra(GeneralString.BcReaderData)
                if (scannedData != null) {
                    Toast.makeText(this@Nyuka03_Barread, "成功: $scannedData", Toast.LENGTH_SHORT).show()
                    // エディットテキストにデータを表示
                    val itemBar = findViewById<EditText>(R.id.caseNum)
                    itemBar.setText(scannedData)

                    // スキャンデータが13桁または14桁の数字かを確認
                    if (scannedData.matches(Regex("\\d{13,14}"))) {
                        // 次の画面へ
                        val nextIntent = Intent(this@Nyuka03_Barread, Nyuka04_Num::class.java)
                        startActivity(nextIntent)
                    } else {
                        Toast.makeText(this@Nyuka03_Barread, "スキャンデータが13桁または14桁の数字ではありません", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@Nyuka03_Barread, "スキャンデータが取得できませんでした", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this@Nyuka03_Barread, "意図しないアクション: ${intent.action}", Toast.LENGTH_SHORT).show()
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
            KeyEvent.KEYCODE_F1 -> {
                // Enterキーが押されたときの処理
                val nextIntent = Intent(this, Nyuka04_Num::class.java)
                startActivity(nextIntent)
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}