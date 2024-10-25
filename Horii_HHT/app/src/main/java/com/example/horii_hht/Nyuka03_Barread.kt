package com.example.horii_hht

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.KeyEvent
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.cipherlab.barcode.GeneralString
import com.cipherlab.barcode.ReaderManager
import com.example.horii_hht.DB.AppDatabase
import com.example.horii_hht.DB.ItemDAO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Nyuka03_Barread: AppCompatActivity() {
    private lateinit var filter: IntentFilter
    private var readerManager: ReaderManager? = null
    private lateinit var db: AppDatabase
    private lateinit var dao: ItemDAO
    private var scannedData: String? = null // クラスメンバに移動

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.nyuka03)
        // ReaderManagerの初期化
        readerManager = ReaderManager.InitInstance(this)
        val scanBtn = findViewById<android.widget.Button>(R.id.button2)
        //商品総数テキスト
        val itemAll = findViewById<android.widget.TextView>(R.id.itemAll)
        //商品総数済み数テキスト
        val real_itemAll = findViewById<android.widget.TextView>(R.id.real_itemAll)
        //商品点数テキスト
        val itemNum = findViewById<android.widget.TextView>(R.id.itemNum)
        //商品点数済み数テキスト
        val real_itemNum = findViewById<android.widget.TextView>(R.id.real_itemNum)
        //検品番号テキスト
        val kenpinNo = findViewById<android.widget.TextView>(R.id.kenpinNo)
//DBの中身チェックするためだけのテキスト
        val dbtest =findViewById<TextView>(R.id.dbtest)

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
            val getCountOfZumiItems = dao.getCountOfZumiItems()
            val getTotalZumiCount = dao.getTotalZumiCount()
            //DB内全データ取得
//            val getall = dao.getItemAll()

            launch (Dispatchers.Main){
                real_itemNum.text = getCountOfZumiItems.toString()
                itemNum.text = distinctItemCount.toString()
                real_itemAll.text = getTotalZumiCount.toString()
                itemAll.text = totalSuryo.toString()
                kenpinNo.text = kenpinNoValue
                //DB内全データ表示
//                dbtest.text = getall.toString()
            }
        }

        // ボタンを透明にする
        scanBtn.alpha = 0f
        scanBtn.setOnClickListener {
            readerManager?.SoftScanTrigger() // ソフトスキャントリガー
        }
        // スキャン結果を受け取るためのIntentフィルタ設定
        filter = IntentFilter()
        filter.addAction(GeneralString.Intent_PASS_TO_APP)
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
            if (intent.action == GeneralString.Intent_PASS_TO_APP) {
                // スキャンされたデータを取得
                val data = intent.getStringExtra(GeneralString.BcReaderData)
                if (data != null) {
                    // 改行文字を削除
                    scannedData = data

                    // エディットテキストにデータを表示
                    val barcodedata = findViewById<EditText>(R.id.barcode)
//                    itemBar.setText("") // リセット
                    barcodedata.setText(scannedData)
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
                // F1キーが押されたときの処理
                if (scannedData != null && scannedData!!.matches(Regex("\\d{13,14}"))) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        // データベース検索: 13桁ならJAN、14桁ならITFをクエリに使用
                        val items = if (scannedData!!.length == 13) {
                            dao.getItemByCode(jan = scannedData!!, itf = "")
                        } else {
                            dao.getItemByCode(jan = "", itf = scannedData!!)
                        }

                        // メインスレッドでUI更新や画面遷移を行う
                        withContext(Dispatchers.Main) {
                            if (items.isNotEmpty()) {
                                // 次の画面へ遷移
                                val nextIntent = Intent(this@Nyuka03_Barread, Nyuka04_Num::class.java)
                                // スキャンデータを次の画面に渡す
                                nextIntent.putExtra("SCANNED_DATA", scannedData)
                                startActivity(nextIntent)
                            } else {
                                Toast.makeText(this@Nyuka03_Barread, "商品がデータベースに存在しません", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(this@Nyuka03_Barread, "コードの形式が正しくないか、スキャンデータが存在しません", Toast.LENGTH_SHORT).show()
                }
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}