package com.example.horii_hht.syukka

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.cipherlab.barcode.GeneralString
import com.cipherlab.barcode.ReaderManager
import com.example.horii_hht.CustomDialog
import com.example.horii_hht.DB.AppDatabase
import com.example.horii_hht.DB.SyukkaDAO
import com.example.horii_hht.DB.SyukkaItem
import com.example.horii_hht.Main_Menu
import com.example.horii_hht.R
import com.example.horii_hht.nyuka.Nyuka02_KenpinStart
import com.example.horii_hht.setting.ScreenStateReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Syukka_B02 : AppCompatActivity(){
    private lateinit var filter : IntentFilter
    private var readerManager: ReaderManager? = null
    private  lateinit var db: AppDatabase
    lateinit var dao: SyukkaDAO
    private lateinit var screenReceiver: ScreenStateReceiver


    private var source: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.syukka_c02)

        val backbtn = findViewById<Button>(R.id.back)

        backbtn.setOnClickListener {
            val intent = Intent(this, Main_Menu::class.java)
            startActivity(intent)
            finish()
            true
        }

        source = intent.getStringExtra("source")

        screenReceiver = ScreenStateReceiver()  // ScreenStateReceiverの初期化と登録

        val screenfilter = IntentFilter().apply {   // スクリーン用のインテントフィルター設定
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, screenfilter)  // スクリーンのオンオフのBroadcastReceiverの登録

        readerManager = ReaderManager.InitInstance(this) // ReaderManagerの初期化
        filter = IntentFilter().apply {  // インテントフィルタの初期化（ハードウェアスキャンをサポート）
            addAction(GeneralString.Intent_PASS_TO_APP) // ハードウェアスキャン用
        }
        registerReceiver(scanDataReceiver, filter)  // BroadcastReceiverの登録（スキャンデータのブロードキャスト受信準備）

        lifecycleScope.launch { // データベースの初期化
            db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "app_database"
            ).fallbackToDestructiveMigration().build()

            dao = db.syukkaDAO()  //データベースオブジェクトの取得
        }
    }

    private val scanDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == GeneralString.Intent_PASS_TO_APP) {
                val receivedData = intent.getStringExtra(GeneralString.BcReaderData) ?: return

                Log.d("ScanData", "Received raw data: $receivedData")

                if (receivedData != null) {


                    // 改行文字を削除
                    val cleanedData = receivedData!!.replace("\n", "").replace(":", "")
                    val dataParts = cleanedData.split(",")

                    // データ挿入用のリスト
                    val itemsToInsert = mutableListOf<SyukkaItem>()

                    // 7の倍数のデータだけ処理
                    val validDataParts = (dataParts.size / 8) * 8

                    if (validDataParts >= 8) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            for (i in 0 until validDataParts step 8) {
                                val kenpinNo = dataParts[i]
                                val kenpinpage = dataParts[i + 1]


                                // 現在の日時を取得
                                val currentDate = Date()
// 日時を指定の形式でフォーマット
                                val dateFormat =
                                    SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
                                val formattedDate = dateFormat.format(currentDate)

                                val item = SyukkaItem(
                                    id = 0,
                                    kenpinNo = kenpinNo,
                                    kenpinpage = kenpinpage,
                                    itemCD = dataParts[i + 2],
                                    itemName = dataParts[i + 3],
                                    case_q = dataParts[i + 4].toInt(),
                                    bara = dataParts[i + 5].toInt(),
                                    JAN = dataParts[i + 6],
                                    ITF = dataParts[i + 7],
                                    casezumi = 0,
                                    barazumi = 0,
                                    kenpinTime = "",
                                    QRTime = formattedDate
                                )
                                itemsToInsert.add(item)
                            }

                            try {
                                // データベースにインサート
                                dao.insert(itemsToInsert)

                                val allItem = dao.getSyukkaItemAll()
                                Log.d("Database", "Current items in database: $allItem")


                                withContext(Dispatchers.Main) {
                                    val nextIntent =
                                        Intent(this@Syukka_B02, Syukka_B03::class.java)
                                    startActivity(nextIntent)
                                    finish()
                                }

                            } catch (e: Exception) {
                                e.printStackTrace()
                                Log.e("DatabaseError", "Error during insertion", e)
                                withContext(Dispatchers.Main) {
                                    CustomDialog.Builder(this@Syukka_B02)
                                        .setTitle("エラー")
                                        .setMessage("不正なQRコードです。")
                                        .setPositiveButton("OK")
                                        .setNegativeButton("")
                                        .build()
                                        .show(supportFragmentManager, CustomDialog::class.simpleName)
                                    true
                                }
                            }
                        }
                    } else {
                        CustomDialog.Builder(this@Syukka_B02)
                            .setTitle("エラー")
                            .setMessage("スキャンデータが読み込めませんでした。")
                            .setPositiveButton("OK")
                            .setNegativeButton("")
                            .build()
                            .show(supportFragmentManager, CustomDialog::class.simpleName)
                        true
                    }
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_F4 -> { // F4キーが押されたときメインメニューに戻る処理
                val intent = Intent(this, Main_Menu::class.java)
                startActivity(intent)
                true
            }
            KeyEvent.KEYCODE_BACK -> {
                // バックキーが押されたときの処理
                true
            }
            KeyEvent.KEYCODE_ENTER -> {
                // エンターキーが押されたときの処理
                true
            }


            else -> super.onKeyDown(keyCode, event)
        }
    }

    // Activity破棄される時に呼び出されるライフサイクルメソッド
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(scanDataReceiver)  // BroadcastReceiverの解除
        readerManager?.Release() // ReaderManagerの解放
        Log.d("ScanData", "Received raw data: ${intent.getStringExtra(GeneralString.BcReaderData)}")
        unregisterReceiver(screenReceiver)

    }

    // ダイアログを表示するメソッド
    fun showWorkingDialog() {
        CustomDialog.Builder(this)
            .setTitle("作業中")
            .setMessage("作業中です")
            .setPositiveButton("OK")
            .setNegativeButton("")
            .build()
            .show(supportFragmentManager, CustomDialog::class.simpleName)
        true
    }
    override fun onPause() {
        super.onPause()
        val sharedPreferences = getSharedPreferences("AppState", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("lastActivity", this::class.java.simpleName)
        editor.apply()
    }


}