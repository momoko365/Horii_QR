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
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.cipherlab.barcode.GeneralString
import com.cipherlab.barcode.ReaderManager
import com.example.horii_hht.DB.AppDatabase
import com.example.horii_hht.DB.Item
import com.example.horii_hht.DB.ItemDAO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Nyuka01_QRread : AppCompatActivity() {
    private lateinit var filter: IntentFilter
    private var readerManager: ReaderManager? = null
    private lateinit var db: AppDatabase
    private lateinit var dao: ItemDAO

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

        // データベースの初期化
        lifecycleScope.launch {
            db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "app_database"
            ).fallbackToDestructiveMigration().build()
            dao = db.itemDAO()
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

                    // QRコードデータをパース
                    val dataParts = scannedData.split(",")
                    if (dataParts.size == 9) {
                        val item = Item(
                            id = 0, // autoGenerateなので0を設定
                            kenpinNo = dataParts[0],
                            itemCD = dataParts[1],
                            itemName = dataParts[2],
                            suryo = dataParts[3].toInt(),
                            in_q = dataParts[4].toInt(),
                            case_q = dataParts[5].toInt(),
                            JAN = dataParts[6],
                            ITF = dataParts[7]
                        )

                        // データベースにインサート
                        lifecycleScope.launch(Dispatchers.IO) {
                            dao.insert(item)
                        }

                        val nextIntent = Intent(this@Nyuka01_QRread, Nyuka02_KenpinStart::class.java)
                        nextIntent.putExtra("scannedData", scannedData)
                        startActivity(nextIntent)
                    } else {
                        Toast.makeText(this@Nyuka01_QRread, "QRコードの形式が正しくありません", Toast.LENGTH_SHORT).show()
                    }
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