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
        //ケース数テキスト
        val caseAll = findViewById<TextView>(R.id.real_itemAll)
        //バラ数テキスト
        val baraAll = findViewById<TextView>(R.id.realbara)
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
            val totalCase = dao.getTotalCase()
            val totalBara = dao.getTotalBara()
            val kenpinNoValue = dao.getKenpinNo()
            val check = dao.getItemCount()
            // UIスレッドでテキストビューに値を設定
            launch(Dispatchers.Main) {
                itemNum.text = distinctItemCount.toString()
                caseAll.text = totalCase.toString()
                baraAll.text = totalBara.toString()
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
            if (intent.action == GeneralString.Intent_PASS_TO_APP) {
                val receivedData = intent.getStringExtra(GeneralString.BcReaderData) ?: return


                Log.d("ScanData", "Received raw data: $receivedData")

                if (receivedData != null && receivedData != scannedData) {
                    scannedData = receivedData

                    // 改行文字を削除
                    val cleanedData = scannedData!!.replace("\n", "")
                    val dataParts = cleanedData.split(",")

                    // データ挿入用のリスト
                    val itemsToInsert = mutableListOf<Item>()

                    // 7の倍数のデータだけ処理
                    val validDataParts = (dataParts.size / 7) * 7
                    var dataInserted = true // フラグを追加
                    Log.d("ScanData", "Received raw data: $scannedData")


                    // データ挿入が成功した場合にアクティビティを再起動
                    if (validDataParts >= 7) {
                        for (i in 0 until validDataParts step 7) {
                            val item = Item(
                                id = 0,
                                kenpinNo = dataParts[i],
                                itemCD = dataParts[i + 1],
                                itemName = dataParts[i + 2],
                                case_q = dataParts[i + 3].toInt(),
                                bara = dataParts[i + 4].toInt(),
                                JAN = dataParts[i + 5],
                                ITF = dataParts[i + 6],
                                casezumi = 0,
                                barazumi = 0
                            )
                            itemsToInsert.add(item)
                        }

                        // 非同期処理
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                // データベースにインサート
                                dao.insert(itemsToInsert)

                                val allItem = dao.getItemAll()
                                Log.d("Database", "Current items in database: $allItem")

                                // UIスレッドでアクティビティを再起動
                                withContext(Dispatchers.Main) {
                                    val intent = Intent(this@Nyuka02_KenpinStart, Nyuka02_KenpinStart::class.java)
                                    startActivity(intent)
                                    finish() // 現在のアクティビティを終了
                                }

                            } catch (e: Exception) {
                                e.printStackTrace()
                                Log.e("DatabaseError", "Error during insertion", e)
                                withContext(Dispatchers.Main) {
                                    AlertDialog.Builder(this@Nyuka02_KenpinStart)
                                        .setTitle("エラー")
                                        .setMessage("不正なQRコードです。")
                                        .setPositiveButton("OK", null)
                                        .show()
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
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_F7 -> {

                lifecycleScope.launch(Dispatchers.IO) {
                    scannedData = null
                    dao.deleteAllItems()
                    val distinctItemCount = dao.getDistinctItemCount() ?: 0
                    val totalSuryo = dao.getTotalCase() ?: 0
                    val totalBara = dao.getTotalBara() ?: 0
                    withContext(Dispatchers.Main) {
                        if (distinctItemCount == 0 && totalSuryo == 0 && totalBara == 0) {
                            val itemNum = findViewById<TextView>(R.id.real_itemNum)
                            val itemAll = findViewById<TextView>(R.id.real_itemAll)
                            itemNum.text = distinctItemCount.toString()
                            itemAll.text = totalSuryo.toString()
                        } else {
                            Toast.makeText(this@Nyuka02_KenpinStart, "リセットに失敗しました。", Toast.LENGTH_SHORT).show()
                        }
                    }
                    withContext(Dispatchers.Main) {
                        withContext(Dispatchers.IO) {
                          val allItem = dao.getItemAll()
                            Log.d("Database","Current items in database: $allItem")
                        }
                        val intent = Intent(this@Nyuka02_KenpinStart, Nyuka01_QRread::class.java)
                        startActivity(intent)
                        finish()
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