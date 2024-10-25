package com.example.horii_hht

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Button
import android.widget.TextView
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

class Nyuka02_KenpinStart : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private lateinit var dao: ItemDAO
    private lateinit var filter: IntentFilter
    private var readerManager: ReaderManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.nyuka02)

        // ReaderManagerの初期化
        readerManager = ReaderManager.InitInstance(this)

        // インテントフィルタの初期化
        filter = IntentFilter().apply {
            addAction(GeneralString.Intent_PASS_TO_APP) // ハードウェアスキャン用
        }

        // BroadcastReceiverの登録
        registerReceiver(scanDataReceiver, filter)


        //商品点数テキスト
        val itemNum = findViewById<TextView>(R.id.real_itemNum)
        //商品総数テキスト
        val itemAll = findViewById<TextView>(R.id.real_itemAll)
        //検品番号テキスト
        val kenpinNo = findViewById<TextView>(R.id.kenpinNo)

        // データベースの初期化
        lifecycleScope.launch(Dispatchers.IO) {
            db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "app_database"
            ).fallbackToDestructiveMigration().build()
            dao = db.itemDAO()

            // データベースからデータを取得
            val distinctItemCount = dao.getDistinctItemCount()
            val totalSuryo = dao.getTotalSuryo()
            val kenpinNoValue = dao.getKenpinNo()

            // UIスレッドでテキストビューに値を設定
            launch(Dispatchers.Main) {
                itemNum.text = distinctItemCount.toString()
                itemAll.text = totalSuryo.toString()
                kenpinNo.text = kenpinNoValue ?: "N/A"
            }
        }

        val kenpinBtn = findViewById<Button>(R.id.startbtn)
        kenpinBtn.setOnClickListener {
            val intent = Intent(this, Nyuka03_Barread::class.java)
            startActivity(intent)
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
            if (intent.action == GeneralString.Intent_PASS_TO_APP) {
                val scannedData = intent.getStringExtra(GeneralString.BcReaderData)
                Log.d("Nyuka01_QRread", "Scanned Data: $scannedData")
                if (scannedData != null) {
                    if (!isFinishing) {
                        Toast.makeText(this@Nyuka02_KenpinStart, "成功: $scannedData", Toast.LENGTH_SHORT).show()
                    }

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
                            ITF = dataParts[7],
                            zumi = 0
                        )

                        // データベースにインサート
                        lifecycleScope.launch(Dispatchers.IO) {
                            dao.insert(item)
                        }

                    } else {
                        if (!isFinishing) {
                            Toast.makeText(this@Nyuka02_KenpinStart, "QRコードの形式が正しくありません", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    if (!isFinishing) {
                        Toast.makeText(this@Nyuka02_KenpinStart, "スキャンデータが取得できませんでした", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                if (!isFinishing) {
                    Toast.makeText(this@Nyuka02_KenpinStart, "意図しないアクション: ${intent.action}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_F7 -> {
                // F7キーが押されたときDBリセットする処理
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}