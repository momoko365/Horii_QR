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
import androidx.appcompat.app.AlertDialog
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
import kotlinx.coroutines.withContext

class Nyuka02_KenpinStart : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private lateinit var dao: ItemDAO
    private lateinit var filter: IntentFilter
    private var readerManager: ReaderManager? = null
    private var scannedData: String? = null

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
            )
                .fallbackToDestructiveMigration()
                .build()
            dao = db.itemDAO()

            // データベースからデータを取得
            val distinctItemCount = dao.getDistinctItemCount()
            val totalSuryo = dao.getTotalSuryo()
            val kenpinNoValue = dao.getKenpinNo()
            val check = dao.getItemCount()
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
            finish()
        }
    }

    // スキャン結果を受け取るBroadcastReceiver
    private val scanDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // インテントのアクションが GeneralString.Intent_PASS_TO_APP かどうかを確認
            if (intent.action == GeneralString.Intent_PASS_TO_APP) {
                val receivedData = intent.getStringExtra(GeneralString.BcReaderData)
                if (receivedData != null && receivedData != scannedData) {
                    scannedData = receivedData

                    // 改行文字を削除
                    val cleanedData = scannedData!!.replace("\n", "")
                    val dataParts = cleanedData.split(",")

                    // 8の倍数のデータだけ処理
                    val validDataParts = (dataParts.size / 8) * 8
                    if (validDataParts >= 8) {
                        for (i in 0 until validDataParts step 8) {
                            val item = Item(
                                id = 0,
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

                            // 非同期処理
                            lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    // データベースにインサート
                                    dao.insert(item)

                                    // データ取得もバックグラウンドで行う
                                    val distinctItemCount = dao.getDistinctItemCount() ?: 0
                                    val totalSuryo = dao.getTotalSuryo() ?: 0
                                    val kenpinNoValue = dao.getKenpinNo()

                                    // UIスレッドで更新
                                    withContext(Dispatchers.Main) {
                                        val itemNum = findViewById<TextView>(R.id.real_itemNum)
                                        val itemAll = findViewById<TextView>(R.id.real_itemAll)
                                        val kenpinNo = findViewById<TextView>(R.id.kenpinNo)
                                        itemNum.text = distinctItemCount.toString()
                                        itemAll.text = totalSuryo.toString()
                                        kenpinNo.text = kenpinNoValue ?: "N/A"
                                    }

                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Log.e("DatabaseError", "Error during insertion", e)  // 詳細なエラーメッセージをログに出力
                                    withContext(Dispatchers.Main) {
                                        AlertDialog.Builder(this@Nyuka02_KenpinStart)
                                            .setTitle("エラー")
                                            .setMessage("不正なQRコードです。")
                                            .setPositiveButton("OK", null)
                                            .show()
                                    }
                                }
                            }

                        }
                    }
                } else {
                    Toast.makeText(
                        this@Nyuka02_KenpinStart,
                        "スキャンデータが取得できませんでした",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_F7 -> {

                lifecycleScope.launch(Dispatchers.IO) {
                    scannedData = null
                    dao.deleteAllItems()
                    val distinctItemCount = dao.getDistinctItemCount() ?: 0
                    val totalSuryo = dao.getTotalSuryo() ?: 0
                    withContext(Dispatchers.Main) {
                        if (distinctItemCount == 0 && totalSuryo == 0) {
                            val itemNum = findViewById<TextView>(R.id.real_itemNum)
                            val itemAll = findViewById<TextView>(R.id.real_itemAll)
                            itemNum.text = distinctItemCount.toString()
                            itemAll.text = totalSuryo.toString()
                        } else {
                            Toast.makeText(this@Nyuka02_KenpinStart, "リセットに失敗しました。", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // BroadcastReceiverの解除
        unregisterReceiver(scanDataReceiver)
        // ReaderManagerの解放
        readerManager?.Release()
    }
}