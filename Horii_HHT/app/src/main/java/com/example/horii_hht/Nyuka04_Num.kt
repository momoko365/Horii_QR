package com.example.horii_hht

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
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

class Nyuka04_Num : AppCompatActivity() {
    private lateinit var filter: IntentFilter
//    private var readerManager: ReaderManager? = null
    private lateinit var db: AppDatabase
    private lateinit var dao: ItemDAO
    private var item: Item? = null // クラス変数として宣言

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.nyuka04)


        // Intentからデータを取得
        val scannedData = intent.getStringExtra("SCANNED_DATA")

        // ReaderManagerの初期化
//        readerManager = ReaderManager.InitInstance(this)

        // インテントフィルタの初期化（ハードウェアスキャンをサポート）
//        filter = IntentFilter().apply {
//            addAction(GeneralString.Intent_PASS_TO_APP) // ハードウェアスキャン用
//        }

        // BroadcastReceiverの登録
//        registerReceiver(scanDataReceiver, filter)

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
        //商品名テキスト
        val itemName = findViewById<android.widget.TextView>(R.id.itemname)
        //ケース数入力
        val caseNum = findViewById<android.widget.EditText>(R.id.barcode)
        //バラ数入力
        val baraNum = findViewById<android.widget.EditText>(R.id.baraNum)
        //次へボタン
        val nextbtn = findViewById<Button>(R.id.startbtn)

        nextbtn.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                item = scannedData?.let { it1 -> dao.getItemCode(jan = it1, itf = "") }
                // ケース数の入力処理
                val caseNumValue = caseNum.text.toString().toIntOrNull()
                if (caseNumValue != null && item != null) {
                    val totalCaseNum = caseNumValue * item!!.in_q + item!!.zumi
                    // `zumi` に登録する処理をここに追加
                    dao.update(item!!.copy(zumi = totalCaseNum))
                }

                // バラ数の入力処理
                val baraNumValue = baraNum.text.toString().toIntOrNull()
                if (baraNumValue != null) {
                    // `zumi` に登録する処理をここに追加
                    item?.let { dao.update(it.copy(zumi = baraNumValue + it.zumi)) }
                }

                // メインスレッドで画面遷移を行う
                launch(Dispatchers.Main) {
                    val intent = Intent(this@Nyuka04_Num, Nyuka03_Barread::class.java)
                    startActivity(intent)
                }
            }
        }

        // データベースの初期化
        lifecycleScope.launch(Dispatchers.IO) {
            db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "app_database"
            ).fallbackToDestructiveMigration().build()
            dao = db.itemDAO()
            // 商品名を取得
            val item = if (scannedData != null && scannedData.length == 13) {
                dao.getItemCode(jan = scannedData, itf = "")
            } else if (scannedData != null && scannedData.length == 14) {
                dao.getItemCode(jan = "", itf = scannedData)
            } else {
                null
            }
            // データベースからデータを取得
            val distinctItemCount = dao.getDistinctItemCount()
            val totalSuryo = dao.getTotalSuryo()
            val kenpinNoValue = dao.getKenpinNo()
            val getCountOfZumiItems = dao.getCountOfZumiItems()
            val getTotalZumiCount = dao.getTotalZumiCount()


            launch (Dispatchers.Main){
                real_itemNum.text = getCountOfZumiItems.toString()
                itemNum.text = distinctItemCount.toString()
                real_itemAll.text = getTotalZumiCount.toString()
                itemAll.text = totalSuryo.toString()
                kenpinNo.text = kenpinNoValue
                // 商品名をテキストビューに表示
                itemName.text = item?.itemName ?: "商品が見つかりません"

            }


        }

    }



}