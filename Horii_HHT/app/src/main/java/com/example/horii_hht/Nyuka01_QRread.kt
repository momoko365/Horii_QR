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
import androidx.appcompat.app.AlertDialog
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
//インテントを受信したときの処理を定義
        override fun onReceive(context: Context, intent: Intent) {
            // インテントのアクションが GeneralString.Intent_PASS_TO_APP かどうかを確認
            when (intent.action) {
                GeneralString.Intent_PASS_TO_APP -> { // ハードウェアスキャンのデータ
                    // スキャンデータを取得
                    var scannedData = intent.getStringExtra(GeneralString.BcReaderData)

                    if (scannedData != null) {
                        // 取得したスキャンデータがnullでない場合
                        // 改行文字を削除
                        var cleanedData = scannedData.replace("\n", "")

                        // QRコードデータをカンマ区切りでパース
                        var dataParts = cleanedData.split(",")
                        var validDataParts = (dataParts.size / 8)*8  // 8の倍数の要素数を取得
// 8の倍数の要素ごとにデータを処理
                        for (i in 0 until  validDataParts step 8) {
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
                                try {
                                    dao.insert(item)
                                    scannedData = null
                                    dataParts = emptyList()
                                    cleanedData = ""

                                    // 次の画面に遷移
                                    val nextIntent = Intent(this@Nyuka01_QRread, Nyuka02_KenpinStart::class.java)
                                    startActivity(nextIntent)
                                    finish()
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
                        }
                    } else {
                        // データがnullの場合エラーダイアログを表示
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
                    // 意図しないアクションの場合エラーダイアログを表示
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
    }
}
