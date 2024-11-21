package com.example.horii_hht

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
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

class Nyuka01_QRread : AppCompatActivity() {
//インテントフィルターを初期化
    private lateinit var filter: IntentFilter
    //ReaderManagerを初期化
    private var readerManager: ReaderManager? = null
    //データベースを初期化
    private lateinit var db: AppDatabase
    //DAOを初期化
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

        // BroadcastReceiverの登録（スキャンデータのブロードキャスト受信準備）
        registerReceiver(scanDataReceiver, filter)

        // データベースの初期化
        lifecycleScope.launch {
            db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "app_database"
            ).fallbackToDestructiveMigration().build()
            //データベース帯ジェクトの取得
            dao = db.itemDAO()
        }
    }

    // スキャン結果を受け取るBroadcastReceiver
    private val scanDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {


            when (intent.action) {
                GeneralString.Intent_PASS_TO_APP -> {
                    var scannedData = intent.getStringExtra(GeneralString.BcReaderData)
                    Log.d("ScanData", "Received raw data: $scannedData")

                    if (scannedData != null) {
                        var cleanedData = scannedData.replace("\n", "")
                        var dataParts = cleanedData.split(",")
                        var validDataParts = (dataParts.size / 7) * 7

                        // データ挿入用のリスト
                        val itemsToInsert = mutableListOf<Item>()

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

                            // 挿入するアイテムをリストに追加
                            itemsToInsert.add(item)
                        }

                        // データベースに一括挿入
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                dao.insert(itemsToInsert)
                                val allItem = dao.getItemAll()
                                Log.d("Database","Current items in database: $allItem")

                                // すべての処理が完了してから次の画面に遷移
                                runOnUiThread {
                                    val nextIntent = Intent(this@Nyuka01_QRread, Nyuka02_KenpinStart::class.java)
                                    startActivity(nextIntent)
                                    finish()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                runOnUiThread {
                                    AlertDialog.Builder(this@Nyuka01_QRread)
                                        .setTitle("エラー")
                                        .setMessage("不正なQRコードです。")
                                        .setPositiveButton("OK", null)
                                        .show()
                                }
                            }
                        }
                    } else {
                        if (!isFinishing && !isDestroyed) {
                            AlertDialog.Builder(this@Nyuka01_QRread)
                                .setTitle("エラー")
                                .setMessage("不正なQRコードです。")
                                .setPositiveButton("OK", null)
                                .show()
                        }
                    }
                }
                else -> {
                    if (!isFinishing && !isDestroyed) {
                        AlertDialog.Builder(this@Nyuka01_QRread)
                            .setTitle("エラー")
                            .setMessage("不正なQRコードです。")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
            }
        }
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            // F4キーが押されたときメインメニューに戻る処理
            KeyEvent.KEYCODE_F4 -> {
                val intent = Intent(this, Main_Menu::class.java)
                startActivity(intent)
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    // Activity破棄される時に呼び出されるライフサイクルメソッド
    override fun onDestroy() {
        super.onDestroy()
        // BroadcastReceiverの解除
        unregisterReceiver(scanDataReceiver)
        // ReaderManagerの解放
        readerManager?.Release()
        Log.d("ScanData", "Received raw data: ${intent.getStringExtra(GeneralString.BcReaderData)}")

    }
}
