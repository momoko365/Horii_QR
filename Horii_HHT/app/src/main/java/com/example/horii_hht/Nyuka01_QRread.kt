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
import com.cipherlab.barcodebase.ReaderCallback
import com.example.horii_hht.DB.AppDatabase
import com.example.horii_hht.DB.AppDatabase_Impl
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


        // インテントフィルタの初期化（ハードウェアスキャンをサポート）
        filter = IntentFilter().apply {
            addAction(GeneralString.Intent_PASS_TO_APP) // ハードウェアスキャン用
        }

        // BroadcastReceiverの登録
        registerReceiver(scanDataReceiver, filter)



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

    // Activity破棄される時に呼び出されるライフサイクルメソッド
    override fun onDestroy() {
        super.onDestroy()
        // BroadcastReceiverの解除
        unregisterReceiver(scanDataReceiver)
        // ReaderManagerの解放
        readerManager?.Release()
    }

    // スキャン結果を受け取るBroadcastReceiver
    private val scanDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                GeneralString.Intent_PASS_TO_APP -> { // ハードウェアスキャンのデータ
                    val scannedData = intent.getStringExtra(GeneralString.BcReaderData)
                    Log.d("Nyuka01_QRread", "Scanned Data: $scannedData")
                    if (scannedData != null) {
                        if (!isFinishing && !isDestroyed) {
                            Toast.makeText(this@Nyuka01_QRread, "成功: $scannedData", Toast.LENGTH_SHORT).show()
                        }

                        // 改行文字を削除
                        val cleanedData = scannedData.replace("\n", "")

                        // QRコードデータをパース
                        val dataParts = cleanedData.split(",")
                        val validDataParts = dataParts.size / 8 * 8 // 8の倍数の要素数を取得

                        for (i in 0 until validDataParts step 8) {
                            val item = Item(
                                id = 0, // autoGenerateなので0を設定
                                kenpinNo = dataParts[i],
                                itemCD = dataParts[i + 1],
                                itemName = dataParts[i + 2],
                                suryo = dataParts[i + 3].toInt(),
                                in_q = dataParts[i + 4].toInt(),
                                case_q = dataParts[i + 5].toInt(),
                                JAN = dataParts[i + 6],
                                ITF = dataParts[i + 7],
                                zumi = 0
                            )

                            // データベースにインサート
                            lifecycleScope.launch(Dispatchers.IO) {
                                dao.insert(item)
                                val nextIntent = Intent(this@Nyuka01_QRread, Nyuka02_KenpinStart::class.java)
                                startActivity(nextIntent)

                            }
                        }
                    } else {
                        if (!isFinishing && !isDestroyed) {
                            Toast.makeText(this@Nyuka01_QRread, "スキャンデータが取得できませんでした", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                else -> {
                    if (!isFinishing && !isDestroyed) {
                        Toast.makeText(this@Nyuka01_QRread, "意図しないアクション: ${intent.action}", Toast.LENGTH_SHORT).show()
                    }
                }
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
